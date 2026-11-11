package com.shopkeeper.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.shopkeeper.data.export.CsvExporter
import com.shopkeeper.data.local.ProductDao
import com.shopkeeper.data.local.ProductEntity
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.StaffMember
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.data.remote.FirestoreService
import com.shopkeeper.data.sync.SyncScheduler
import com.shopkeeper.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID

class ProductRepository(
    private val productDao: ProductDao,
    val firestoreService: FirestoreService,
    private val userSession: UserSessionRepository,
    val networkMonitor: NetworkMonitor,
    private val syncScheduler: SyncScheduler? = null,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val TAG = "ProductRepository"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var realtimeProductsListener: ListenerRegistration? = null
    private var realtimeShopListener: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        repositoryScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                val shopId = userSession.getShopId()
                val pending = getPendingCount()
                if (isOnline) {
                    if (shopId.isNotEmpty()) {
                        // Allow network DHCP IP lease and TLS gRPC socket to stabilize
                        delay(1500)
                        val currentPending = getPendingCount()
                        if (currentPending > 0) {
                            _syncStatus.value = SyncStatus.PendingSync(currentPending)
                            pushPendingChanges(shopId)
                        } else {
                            _syncStatus.value = SyncStatus.Success("Synced with Cloud Database")
                        }
                        startRealtimeSync(shopId)
                    } else {
                        _syncStatus.value = SyncStatus.Idle
                    }
                } else {
                    // Instantly reflect offline status without infinite spinner
                    _syncStatus.value = SyncStatus.Offline(pending)
                }
            }
        }
    }

    private fun isDemoShop(shopId: String): Boolean =
        shopId.isEmpty() || shopId.startsWith("DEMO", ignoreCase = true)

    /**
     * Verifies that FirebaseAuth has a valid active session so that Cloud Firestore security rules
     * (request.auth != null) allow mutations.
     */
    private suspend fun ensureFirebaseAuthValid(): Boolean = withContext(Dispatchers.IO) {
        firebaseAuth.currentUser != null
    }

    /**
     * Seeds initial sample products into local SQLite database if empty (useful for demo/offline test)
     */
    suspend fun seedDemoProductsIfEmpty() = withContext(Dispatchers.IO) {
        if (productDao.getProductCount() == 0) {
            val sampleProducts = listOf(
                Product(id = "DEMO_1", code = "101", name = "Miniket Rice 5kg", category = "Groceries", purchasePriceMinor = 36000L, salesPriceMinor = 42000L, quantity = 25),
                Product(id = "DEMO_2", code = "102", name = "Soybean Cooking Oil 1L", category = "Groceries", purchasePriceMinor = 17500L, salesPriceMinor = 19500L, quantity = 40),
                Product(id = "DEMO_3", code = "103", name = "LED Bulb 15W", category = "Electronics", purchasePriceMinor = 12000L, salesPriceMinor = 16000L, quantity = 50),
                Product(id = "DEMO_4", code = "104", name = "Heavy Duty Extension Cord 3M", category = "Electronics", purchasePriceMinor = 28000L, salesPriceMinor = 35000L, quantity = 15),
                Product(id = "DEMO_5", code = "105", name = "Cotton Bath Towel", category = "Clothing", purchasePriceMinor = 25000L, salesPriceMinor = 32000L, quantity = 20)
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

    suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        productDao.getPendingSyncProducts().size + productDao.getPendingDeletedProducts().size
    }

    /**
     * Add a new product (Optimistic local-first + Async Cloud Firestore sync).
     * Local Room DB write finishes in <10ms and returns Result.success immediately,
     * ensuring dialogs close with zero latency both online and offline.
     */
    suspend fun addProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()

        // 1. Check local code uniqueness
        if (isCodeTaken(product.code)) {
            return@withContext Result.failure(Exception("Product Code '${product.code}' is already used."))
        }

        val id = if (product.id.isNotEmpty()) product.id else "PROD_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(6)
        val newProduct = product.copy(
            id = id,
            updatedAt = System.currentTimeMillis().toString()
        )

        // 2. Save locally immediately (<10ms)
        val entity = ProductEntity.fromDomain(newProduct, isPendingSync = true)
        productDao.insertProduct(entity)

        // 3. Asynchronously sync to Cloud Firestore in background without blocking UI
        if (shopId.isNotEmpty()) {
            repositoryScope.launch {
                pushPendingChanges(shopId)
            }
            syncScheduler?.scheduleSync()
        }

        Result.success(newProduct)
    }

    /**
     * Update an existing product.
     * Local Room DB write finishes in <10ms and returns Result.success immediately.
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

        // Asynchronously sync to Cloud Firestore in background
        if (shopId.isNotEmpty()) {
            repositoryScope.launch {
                pushPendingChanges(shopId)
            }
            syncScheduler?.scheduleSync()
        }

        Result.success(updated)
    }

    /**
     * Delete a product.
     * Soft-deletes locally immediately (<10ms) and returns Result.success immediately.
     */
    suspend fun deleteProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()

        // Soft-delete locally immediately so UI removes it without resurrection risk
        productDao.markDeletedLocally(product.id)

        // Asynchronously sync deletion to Cloud Firestore in background
        if (shopId.isNotEmpty()) {
            repositoryScope.launch {
                pushPendingChanges(shopId)
            }
            syncScheduler?.scheduleSync()
        } else {
            productDao.deletePermanently(product.id)
        }

        Result.success(Unit)
    }

    /**
     * Atomically reassigns all active products from one category to another in Room,
     * marks them as pending sync, and schedules background sync to Cloud Firestore.
     */
    suspend fun reassignCategory(oldCategory: String, newCategory: String): Result<Int> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        val updatedAt = System.currentTimeMillis().toString()
        val affectedCount = productDao.reassignCategory(oldCategory.trim(), newCategory.trim(), updatedAt)

        if (shopId.isNotEmpty() && affectedCount > 0) {
            repositoryScope.launch {
                pushPendingChanges(shopId)
            }
            syncScheduler?.scheduleSync()
        }

        Result.success(affectedCount)
    }

    suspend fun getProductCountForCategory(category: String): Int = withContext(Dispatchers.IO) {
        productDao.getProductCountForCategory(category.trim())
    }

    /**
     * Pushes any local additions, modifications, or deletions made while offline to Cloud Firestore.
     * Protected by syncMutex and timeouts, using atomic batch operations to guarantee rapid completion.
     */
    suspend fun pushPendingChanges(shopId: String): Boolean = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) {
            Log.w(TAG, "pushPendingChanges: shopId is empty")
            return@withContext false
        }
        if (isDemoShop(shopId)) {
            Log.d(TAG, "pushPendingChanges: Demo shop $shopId is local-only, skipping cloud push")
            _syncStatus.value = SyncStatus.Idle
            return@withContext true
        }
        if (!networkMonitor.isCurrentlyOnline()) {
            _syncStatus.value = SyncStatus.Offline(getPendingCount())
            return@withContext false
        }

        try {
            withTimeout(20000L) {
                syncMutex.withLock {
                    if (!networkMonitor.isCurrentlyOnline()) {
                        _syncStatus.value = SyncStatus.Offline(getPendingCount())
                        return@withLock false
                    }

                    val pendingDeletions = productDao.getPendingDeletedProducts()
                    val pendingUpdates = productDao.getPendingSyncProducts()
                    if (pendingDeletions.isEmpty() && pendingUpdates.isEmpty()) {
                        _syncStatus.value = SyncStatus.Success("Synced with Cloud Database")
                        return@withLock true
                    }

                    _syncStatus.value = SyncStatus.Syncing

                    // Ensure auth session is valid before writing to Cloud Firestore
                    val authValid = ensureFirebaseAuthValid()
                    if (!authValid) {
                        Log.e(TAG, "pushPendingChanges: Aborted - Firebase Auth session is invalid")
                        val remaining = getPendingCount()
                        _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) {
                            SyncStatus.PendingSync(remaining)
                        } else {
                            SyncStatus.Offline(remaining)
                        }
                        return@withLock false
                    }

                    // Atomic single-batch push for all pending changes
                    val delIds = pendingDeletions.map { it.id }
                    val updateDomains = pendingUpdates.map { it.toDomain() }
                    Log.d(TAG, "pushPendingChanges: Pushing ${delIds.size} deletions, ${updateDomains.size} updates to shop $shopId")
                    val res = firestoreService.batchSyncProducts(shopId, delIds, updateDomains)

                    if (res.isSuccess) {
                        for (del in pendingDeletions) {
                            productDao.deletePermanently(del.id)
                        }
                        for (item in pendingUpdates) {
                            productDao.insertProduct(item.copy(isPendingSync = false))
                        }
                        userSession.updateLastSyncTime()
                        Log.d(TAG, "pushPendingChanges: Successfully synced all pending changes to Firestore")
                        _syncStatus.value = SyncStatus.Success("Synced with Cloud Database")
                        true
                    } else {
                        val remaining = getPendingCount()
                        val errMsg = res.exceptionOrNull()?.message ?: "Sync error"
                        Log.e(TAG, "pushPendingChanges: Batch push failed: $errMsg", res.exceptionOrNull())
                        _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) {
                            SyncStatus.PendingSync(remaining)
                        } else {
                            SyncStatus.Offline(remaining)
                        }
                        false
                    }
                }
            }
        } catch (e: Exception) {
            val remaining = getPendingCount()
            Log.e(TAG, "pushPendingChanges: Exception during push: ${e.message}", e)
            _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) {
                SyncStatus.PendingSync(remaining)
            } else {
                SyncStatus.Offline(remaining)
            }
            false
        } finally {
            // Safety invariant: Ensure syncStatus never gets stuck on Syncing!
            if (_syncStatus.value is SyncStatus.Syncing) {
                val remaining = getPendingCount()
                _syncStatus.value = when {
                    !networkMonitor.isCurrentlyOnline() -> SyncStatus.Offline(remaining)
                    remaining > 0 -> SyncStatus.PendingSync(remaining)
                    else -> SyncStatus.Idle
                }
            }
        }
    }

    /**
     * Starts real-time listening so updates made by staff on other devices reflect automatically.
     */
    fun startRealtimeSync(shopId: String) {
        if (isDemoShop(shopId) || !networkMonitor.isCurrentlyOnline()) return
        realtimeProductsListener?.remove()
        realtimeShopListener?.remove()

        realtimeProductsListener = firestoreService.observeProducts(
            shopId = shopId,
            onUpdate = { remoteProducts ->
                repositoryScope.launch {
                    val entities = remoteProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) }
                    productDao.syncRemoteProducts(entities)
                    userSession.updateLastSyncTime()
                    val pending = getPendingCount()
                    if (pending == 0) {
                        _syncStatus.value = SyncStatus.Success("Cloud sync active")
                    } else {
                        _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) {
                            SyncStatus.PendingSync(pending)
                        } else {
                            SyncStatus.Offline(pending)
                        }
                    }
                }
            },
            onError = { error ->
                repositoryScope.launch {
                    Log.e(TAG, "observeProducts: Real-time listener error: ${error.message}", error)
                    val pending = getPendingCount()
                    _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) {
                        SyncStatus.Error(error.message ?: "Cloud sync error")
                    } else {
                        SyncStatus.Offline(pending)
                    }
                }
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
     * Full refresh/sync from Cloud Firestore with overall timeout and invariant guarantees.
     */
    suspend fun syncFromRemote(): Result<Int> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        if (shopId.isEmpty()) {
            _syncStatus.value = SyncStatus.Error("No Shop Database linked.")
            return@withContext Result.failure(Exception("No Shop ID configured"))
        }

        if (isDemoShop(shopId)) {
            Log.d(TAG, "syncFromRemote: Demo shop $shopId is local-only, skipping cloud sync")
            _syncStatus.value = SyncStatus.Idle
            return@withContext Result.success(0)
        }

        if (!networkMonitor.isCurrentlyOnline()) {
            val pending = getPendingCount()
            _syncStatus.value = SyncStatus.Offline(pending)
            return@withContext Result.failure(Exception("Device is offline. Showing local data."))
        }

        _syncStatus.value = SyncStatus.Syncing

        try {
            // Overall operation timeout: Guarantee completion within 25 seconds
            withTimeout(25000L) {
                // 1. Refresh shop metadata (name, currency) in case updated remotely
                val shopInfoResult = firestoreService.getShopInfo(shopId)
                if (shopInfoResult.isSuccess) {
                    val shopInfo = shopInfoResult.getOrNull()
                    if (shopInfo != null) {
                        userSession.updateShopMetadata(shopInfo.shopName, shopInfo.currencySymbol)
                    }
                }

                // 2. Push pending local mutations first so they reach cloud before fetching
                val pushSuccess = pushPendingChanges(shopId)
                val remainingAfterPush = getPendingCount()
                if (!pushSuccess && remainingAfterPush > 0) {
                    Log.e(TAG, "syncFromRemote: pushPendingChanges failed with $remainingAfterPush items pending. Aborting remote sync.")
                    val status = if (networkMonitor.isCurrentlyOnline()) SyncStatus.PendingSync(remainingAfterPush) else SyncStatus.Offline(remainingAfterPush)
                    _syncStatus.value = status
                    return@withTimeout Result.failure(Exception("Could not upload $remainingAfterPush local change(s) to cloud database."))
                }

                // 3. Fetch remote products
                val result = firestoreService.getProducts(shopId)
                if (result.isSuccess) {
                    val remoteProducts = result.getOrNull() ?: emptyList()
                    val entities = remoteProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) }

                    productDao.syncRemoteProducts(entities)

                    userSession.updateLastSyncTime()
                    val remaining = getPendingCount()
                    if (remaining == 0) {
                        _syncStatus.value = SyncStatus.Success("Successfully synced ${remoteProducts.size} products")
                    } else {
                        _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) SyncStatus.PendingSync(remaining) else SyncStatus.Offline(remaining)
                    }

                    // Ensure real-time listener is attached
                    startRealtimeSync(shopId)

                    Result.success(remoteProducts.size)
                } else {
                    val remaining = getPendingCount()
                    _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) {
                        SyncStatus.Error(result.exceptionOrNull()?.message ?: "Failed to fetch remote products")
                    } else {
                        SyncStatus.Offline(remaining)
                    }
                    val errMsg = result.exceptionOrNull()?.message ?: "Sync error"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            val remaining = getPendingCount()
            Log.e(TAG, "syncFromRemote: Overall sync exception: ${e.message}", e)
            _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) {
                if (remaining > 0) SyncStatus.PendingSync(remaining) else SyncStatus.Error(e.message ?: "Sync error")
            } else {
                SyncStatus.Offline(remaining)
            }
            Result.failure(e)
        } finally {
            // Safety invariant: Ensure syncStatus never gets stuck on Syncing!
            if (_syncStatus.value is SyncStatus.Syncing) {
                val remaining = getPendingCount()
                _syncStatus.value = when {
                    !networkMonitor.isCurrentlyOnline() -> SyncStatus.Offline(remaining)
                    remaining > 0 -> SyncStatus.PendingSync(remaining)
                    else -> SyncStatus.Idle
                }
            }
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
        if (!isDemoShop(shopId) && isOwner && networkMonitor.isCurrentlyOnline()) {
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
        if (isDemoShop(shopId)) return@withContext Result.success(emptyList())
        firestoreService.getStaffList(shopId)
    }

    suspend fun addStaffMember(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        if (isDemoShop(shopId)) {
            return@withContext Result.failure(Exception("Staff management requires signing in with Google."))
        }
        firestoreService.addStaff(shopId, email)
    }

    suspend fun removeStaffMember(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val shopId = userSession.getShopId()
        if (isDemoShop(shopId)) {
            return@withContext Result.failure(Exception("Staff management requires signing in with Google."))
        }
        firestoreService.removeStaff(shopId, email)
    }

    /**
     * Exports entire product inventory to Excel / CSV file without UI framework references.
     */
    suspend fun exportInventoryToCsv(cacheDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val currentProducts = allProducts.first()
            if (currentProducts.isEmpty()) {
                return@withContext Result.failure(Exception("No products to export. Add products first."))
            }
            val shopName = userSession.shopNameFlow.first()
            val currency = userSession.currencySymbolFlow.first()
            CsvExporter.generateCsvFile(cacheDir, shopName, currency, currentProducts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Backward-compatible overload combining generation and Activity launch.
     */
    suspend fun exportInventoryToCsv(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val genResult = exportInventoryToCsv(context.cacheDir)
        if (genResult.isSuccess) {
            val file = genResult.getOrThrow()
            val shopName = userSession.shopNameFlow.first()
            val chooser = CsvExporter.createShareIntent(context, file, shopName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } else {
            Result.failure(genResult.exceptionOrNull() ?: Exception("Export failed"))
        }
    }
}
