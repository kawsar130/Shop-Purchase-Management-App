package com.shop.purchasemanagement.data.repository

import com.shop.purchasemanagement.data.local.ProductDao
import com.shop.purchasemanagement.data.local.ProductEntity
import com.shop.purchasemanagement.data.model.Product
import com.shop.purchasemanagement.data.model.StaffMember
import com.shop.purchasemanagement.data.model.SyncStatus
import com.shop.purchasemanagement.data.remote.GoogleDriveSheetsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepository(
    private val productDao: ProductDao,
    val driveSheetsService: GoogleDriveSheetsService,
    private val userSession: UserSessionRepository
) {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Continuous reactive stream of all active products, ordered by name.
     * Always loads instantly (<50ms) from local SQLite/Room database.
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
     * Add a new product (Optimistic local-first + Google Sheets sync).
     */
    suspend fun addProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        val spreadsheetId = userSession.getSpreadsheetId()

        // 1. Check local code uniqueness
        if (isCodeTaken(product.code)) {
            return@withContext Result.failure(Exception("Product Code '${product.code}' is already used."))
        }

        val id = if (product.id.isNotEmpty()) product.id else "PROD_" + System.currentTimeMillis()
        val newProduct = product.copy(
            id = id,
            updatedAt = System.currentTimeMillis().toString()
        )

        // 2. Save locally immediately
        val entity = ProductEntity.fromDomain(newProduct, isPendingSync = true)
        productDao.insertProduct(entity)

        // 3. Sync to Google Sheets
        if (spreadsheetId.isNotEmpty()) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val syncResult = driveSheetsService.appendProduct(spreadsheetId, newProduct)
                if (syncResult.isSuccess) {
                    productDao.insertProduct(entity.copy(isPendingSync = false))
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Synced with Google Sheet")
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
        val spreadsheetId = userSession.getSpreadsheetId()

        // Check if new code conflicts with another product
        if (isCodeTaken(product.code, excludeId = product.id)) {
            return@withContext Result.failure(Exception("Product Code '${product.code}' is already used by another product."))
        }

        val updated = product.copy(updatedAt = System.currentTimeMillis().toString())
        val entity = ProductEntity.fromDomain(updated, isPendingSync = true)
        productDao.updateProduct(entity)

        // Sync to Google Sheets
        if (spreadsheetId.isNotEmpty()) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val syncResult = driveSheetsService.updateProduct(spreadsheetId, updated)
                if (syncResult.isSuccess) {
                    productDao.insertProduct(entity.copy(isPendingSync = false))
                    userSession.updateLastSyncTime()
                    _syncStatus.value = SyncStatus.Success("Synced with Google Sheet")
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
        val spreadsheetId = userSession.getSpreadsheetId()

        // Immediately delete locally
        productDao.deletePermanently(product.id)

        // Sync deletion to Google Sheets
        if (spreadsheetId.isNotEmpty()) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                val syncResult = driveSheetsService.deleteProduct(spreadsheetId, product.id, product.code)
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
     * Full refresh/sync from Google Sheets.
     */
    suspend fun syncFromRemote(): Result<Int> = withContext(Dispatchers.IO) {
        val spreadsheetId = userSession.getSpreadsheetId()
        if (spreadsheetId.isEmpty()) {
            _syncStatus.value = SyncStatus.Error("No Shop Spreadsheet linked.")
            return@withContext Result.failure(Exception("No Spreadsheet ID configured"))
        }

        try {
            _syncStatus.value = SyncStatus.Syncing

            val result = driveSheetsService.getProducts(spreadsheetId)
            if (result.isSuccess) {
                val remoteProducts = result.getOrNull() ?: emptyList()
                val entities = remoteProducts.map { ProductEntity.fromDomain(it, isPendingSync = false) }

                productDao.clearSyncedProducts()
                productDao.insertAll(entities)

                userSession.updateLastSyncTime()
                _syncStatus.value = SyncStatus.Success("Successfully synced ${remoteProducts.size} products")
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
        val spreadsheetId = userSession.getSpreadsheetId()
        driveSheetsService.getStaffMembers(spreadsheetId)
    }

    suspend fun addStaffMember(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val spreadsheetId = userSession.getSpreadsheetId()
        driveSheetsService.addStaffMember(spreadsheetId, email)
    }

    suspend fun removeStaffMember(permissionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val spreadsheetId = userSession.getSpreadsheetId()
        driveSheetsService.removeStaffMember(spreadsheetId, permissionId)
    }
}
