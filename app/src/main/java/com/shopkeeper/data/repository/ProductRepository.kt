package com.shopkeeper.data.repository

import android.content.Context
import com.google.firebase.firestore.ListenerRegistration
import com.shopkeeper.data.export.CsvExporter
import com.shopkeeper.data.local.ProductDao
import com.shopkeeper.data.local.ProductEntity
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.StaffMember
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.data.remote.FirestoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductRepository(
    private val productDao: ProductDao,
    val firestoreService: FirestoreService,
    private val userSession: UserSessionRepository
) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeProductsListener: ListenerRegistration? = null
    private var realtimeShopListener: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Seeds initial sample products into local SQLite database if empty (useful for demo/offline test)
     */
    suspend fun seedDemoProductsIfEmpty() = withContext(Dispatchers.IO) {
        if (productDao.getProductCount() == 0) {
            val sampleProducts = listOf(
                Product(id = "DEMO_1", code = "101", name = "Miniket Rice 5kg", category = "Groceries", purchasePrice = 360.0, salesPrice = 420.0, quantity = 25),
                Product(id = "DEMO_2", code = "102", name = "Soybean Cooking Oil 1L", category = "Groceries", purchasePrice = 175.0, salesPrice = 195.0, quantity = 40),
                Product(id = "DEMO_3", code = "103", name = "LED Bulb 15W", category = "Electronics", purchasePrice = 120.0, salesPrice = 160.0, quantity = 50),
                Product(id = "DEMO_4", code = "104", name = "Heavy Duty Extension Cord 3M", category = "Electronics", purchasePrice = 280.0, salesPrice = 350.0, quantity = 15),
                Product(id = "DEMO_5", code = "105", name = "Cotton Bath Towel", category = "Clothing", purchasePrice = 250.0, salesPrice = 320.0, quantity = 20)
            )
            productDao.insertAll(sampleProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) })
        }
    }

    /**
     * Continuous reactive stream of all active products, ordered by name.
     * Always loads instantly (<10ms) from local SQLite/Room database.
     */
    val allProducts: Flow<List<Product>> = productDao.getAllProducts().map { entities ->
        entities.map { it.toDomain() }
    }

    /**
     * Distinct categories.
     */
    val allCategories: Flow<List<String>> = productDao.getAllCategories()

    /**
     * Check if a product code already exists locally.
     */
    suspend fun isCodeTaken(code: String, excludeId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val existing = productDao.getProductByCode(code.trim())
        if (existing != null) {
            return@withContext excludeId == null || existing.id != excludeId
        }
        false
    }

    /**
     * Add a new product (Optimistic local-first + Cloud Firestore sync).
     */
    suspend fun addProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()

        // 1. Check local code uniqueness
        if (isCodeTaken(product.code)) {
            return@withContext Result.failure(Exception("Product Code '${product.code}' is already used."))
        }

        val id = if (product.id.isNotEmpty()) product.id else "PROD_" + System.currentTimeMillis()
        val newProduct = product.copy(
            id = id,
            updatedAt = System.currentTimeMillis().toString()
        )

        // 2. Save locally immediately (<10ms)
        val entity = ProductEntity.fromDomain(newProduct, isPendingSync = true)
        productDao.insertProduct(entity)

        // 3. Sync to Cloud Firestore
        if (shopId.isNotEmpty()) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val syncResult = firestoreService.saveProduct(shopId, newProduct)
                if (syncResult.isSuccess) {
                    productDao.insertProduct(entity.copy(isPendingSync = false))
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Synced with Cloud Database")
                } else {
                    _syncStatus.value = SyncStatus.Offline(1)
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Offline(1)
            }
        }

        Result.success(newProduct)
    }

    /**
     * Update an existing product.
     */
    suspend fun updateProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()

        // Check if new code conflicts with another product
        if (isCodeTaken(product.code, excludeId = product.id)) {
            return@withContext Result.failure(Exception("Product Code '${product.code}' is already used by another product."))
        }

        val updated = product.copy(updatedAt = System.currentTimeMillis().toString())
        val entity = ProductEntity.fromDomain(updated, isPendingSync = true)
        productDao.updateProduct(entity)

        // Sync to Cloud Firestore
        if (shopId.isNotEmpty()) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val syncResult = firestoreService.saveProduct(shopId, updated)
                if (syncResult.isSuccess) {
                    productDao.insertProduct(entity.copy(isPendingSync = false))
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Synced with Cloud Database")
                } else {
                    _syncStatus.value = SyncStatus.Offline(1)
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Offline(1)
            }
        }

        Result.success(updated)
    }

    /**
     * Delete a product.
     */
    suspend fun deleteProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()

        // Soft-delete locally immediately so UI removes it without resurrection risk
        productDao.markDeletedLocally(product.id)

        // Sync deletion to Cloud Firestore
        if (shopId.isNotEmpty()) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val syncResult = firestoreService.deleteProduct(shopId, product.id)
                if (syncResult.isSuccess) {
                    productDao.deletePermanently(product.id)
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Product deleted and synced")
                } else {
                    _syncStatus.value = SyncStatus.Offline(1)
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Offline(1)
            }
        } else {
            productDao.deletePermanently(product.id)
        }

        Result.success(Unit)
    }

    /**
     * Pushes any local additions, modifications, or deletions made while offline to Cloud Firestore.
     */
    private suspend fun pushPendingChanges(shopId: String) {
        if (shopId.isEmpty()) return
        try {
            // 1. Push pending deletions
            val pendingDeletions = productDao.getPendingDeletedProducts()
            for (del in pendingDeletions) {
                val res = firestoreService.deleteProduct(shopId, del.id)
                if (res.isSuccess) {
                    productDao.deletePermanently(del.id)
                }
            }
            // 2. Push pending additions and updates
            val pendingUpdates = productDao.getPendingSyncProducts()
            for (item in pendingUpdates) {
                val res = firestoreService.saveProduct(shopId, item.toDomain())
                if (res.isSuccess) {
                    productDao.insertProduct(item.copy(isPendingSync = false))
                }
            }
        } catch (e: Exception) {
            // Keep pending flags for next sync attempt
        }
    }

    /**
     * Starts real-time listening so updates made by staff on other devices reflect automatically.
     */
    fun startRealtimeSync(shopId: String) {
        if (shopId.isEmpty()) return
        realtimeProductsListener?.remove()
        realtimeShopListener?.remove()

        realtimeProductsListener = firestoreService.observeProducts(
            shopId = shopId,
            onUpdate = { remoteProducts ->
                repositoryScope.launch {
                    pushPendingChanges(shopId)
                    val entities = remoteProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) }
                    productDao.syncRemoteProducts(entities)
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Cloud sync active")
                }
            },
            onError = {
                _syncStatus.value = SyncStatus.Offline(0)
            }
        )

        realtimeShopListener = firestoreService.observeShop(
            shopId = shopId,
            onUpdate = { shopInfo ->
                repositoryScope.launch {
                    userSession.updateShopMetadata(shopInfo.shopName, shopInfo.currencySymbol)
                }
            }
        )
    }

    fun stopRealtimeSync() {
        realtimeProductsListener?.remove()
        realtimeProductsListener = null
        realtimeShopListener?.remove()
        realtimeShopListener = null
    }

    /**
     * Full refresh/sync from Cloud Firestore.
     */
    suspend fun syncFromRemote(): Result<Int> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        if (shopId.isEmpty()) {
            _syncStatus.value = SyncStatus.Error("No Shop Database linked.")
            return@withContext Result.failure(Exception("No Shop ID configured"))
        }

        try {
            _syncStatus.value = SyncStatus.Syncing

            // Push pending offline mutations before pulling down remote collection
            pushPendingChanges(shopId)

            // Also refresh shop metadata (name, currency) in case updated remotely by owner
            val shopInfoResult = firestoreService.getShopInfo(shopId)
            if (shopInfoResult.isSuccess) {
                val shopInfo = shopInfoResult.getOrNull()
                if (shopInfo != null) {
                    userSession.updateShopMetadata(shopInfo.shopName, shopInfo.currencySymbol)
                }
            }

            val result = firestoreService.getProducts(shopId)
            if (result.isSuccess) {
                val remoteProducts = result.getOrNull() ?: emptyList()
                val entities = remoteProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) }

                productDao.syncRemoteProducts(entities)

                userSession.updateLastSyncTime()
                _syncStatus.value = SyncStatus.Success("Successfully synced ${remoteProducts.size} products")

                // Ensure real-time listener is attached
                startRealtimeSync(shopId)

                return@withContext Result.success(remoteProducts.size)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "Sync error"
                _syncStatus.value = SyncStatus.Error(errMsg)
                return@withContext Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Network connection error"
            _syncStatus.value = SyncStatus.Error(msg)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Clears all products from local SQLite database (called on user logout).
     */
    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        productDao.clearAllProducts()
        _syncStatus.value = SyncStatus.Idle
    }

    /**
     * Updates shop currency symbol in DataStore and syncs to Cloud Firestore if owner.
     */
    suspend fun updateShopCurrency(currency: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = currency.trim()
        val shopId = userSession.getShopId()
        val isOwner = userSession.isOwnerFlow.first()
        userSession.updateCurrencySymbol(trimmed)
        if (shopId.isNotEmpty() && isOwner) {
            firestoreService.updateShopCurrency(shopId, trimmed)
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Multi-user staff access management
     */
    suspend fun getStaffList(): Result<List<StaffMember>> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        firestoreService.getStaffList(shopId)
    }

    suspend fun addStaffMember(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        firestoreService.addStaff(shopId, email)
    }

    suspend fun removeStaffMember(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        firestoreService.removeStaff(shopId, email)
    }

    /**
     * Exports entire product inventory to Excel / CSV and opens sharing chooser.
     */
    suspend fun exportInventoryToCsv(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentProducts = allProducts.first()
            if (currentProducts.isEmpty()) {
                return@withContext Result.failure(Exception("No products to export. Add products first."))
            }
            val shopName = userSession.shopNameFlow.first()
            val currency = userSession.currencySymbolFlow.first()
            val result = CsvExporter.exportAndShare(context, shopName, currency, currentProducts)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Export failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
