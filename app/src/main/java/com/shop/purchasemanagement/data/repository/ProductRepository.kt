package com.shop.purchasemanagement.data.repository

import android.content.Context
import com.google.firebase.firestore.ListenerRegistration
import com.shop.purchasemanagement.data.export.CsvExporter
import com.shop.purchasemanagement.data.local.ProductDao
import com.shop.purchasemanagement.data.local.ProductEntity
import com.shop.purchasemanagement.data.model.Product
import com.shop.purchasemanagement.data.model.StaffMember
import com.shop.purchasemanagement.data.model.SyncStatus
import com.shop.purchasemanagement.data.remote.FirestoreService
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
    private var realtimeListener: ListenerRegistration? = null

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
     * Instant search by product name or code.
     */
    fun searchProducts(query: String): Flow<List<Product>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            allProducts
        } else {
            productDao.searchProducts(trimmed).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    /**
     * Filter products by category.
     */
    fun getProductsByCategory(category: String): Flow<List<Product>> {
        return if (category.isEmpty() || category.equals("All", ignoreCase = true)) {
            allProducts
        } else {
            productDao.getProductsByCategory(category).map { list ->
                list.map { it.toDomain() }
            }
        }
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

        // Immediately delete locally
        productDao.deletePermanently(product.id)

        // Sync deletion to Cloud Firestore
        if (shopId.isNotEmpty()) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val syncResult = firestoreService.deleteProduct(shopId, product.id)
                if (syncResult.isSuccess) {
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Product deleted and synced")
                } else {
                    _syncStatus.value = SyncStatus.Offline(0)
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Offline(0)
            }
        }

        Result.success(Unit)
    }

    /**
     * Starts real-time listening so updates made by staff on other devices reflect automatically.
     */
    fun startRealtimeSync(shopId: String) {
        if (shopId.isEmpty()) return
        realtimeListener?.remove()

        realtimeListener = firestoreService.observeProducts(
            shopId = shopId,
            onUpdate = { remoteProducts ->
                repositoryScope.launch {
                    val entities = remoteProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) }
                    productDao.clearSyncedProducts()
                    productDao.insertAll(entities)
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Cloud sync active")
                }
            },
            onError = {
                _syncStatus.value = SyncStatus.Offline(0)
            }
        )
    }

    fun stopRealtimeSync() {
        realtimeListener?.remove()
        realtimeListener = null
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

            val result = firestoreService.getProducts(shopId)
            if (result.isSuccess) {
                val remoteProducts = result.getOrNull() ?: emptyList()
                val entities = remoteProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) }

                productDao.clearSyncedProducts()
                productDao.insertAll(entities)

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
