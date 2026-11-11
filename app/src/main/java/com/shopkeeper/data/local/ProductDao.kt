package com.shopkeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND isDeletedLocally = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT DISTINCT category FROM products 
        WHERE isDeleted = 0 AND isDeletedLocally = 0 AND category != '' 
        ORDER BY category COLLATE NOCASE ASC
    """)
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND isDeletedLocally = 0 AND code = :code COLLATE NOCASE LIMIT 1")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Query("""
        SELECT * FROM products 
        WHERE isDeleted = 0 AND isDeletedLocally = 0
        AND (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE isDeleted = 0 AND isDeletedLocally = 0
        AND (name LIKE '%' || :token1 || '%' OR code LIKE '%' || :token1 || '%' OR category LIKE '%' || :token1 || '%')
        AND (name LIKE '%' || :token2 || '%' OR code LIKE '%' || :token2 || '%' OR category LIKE '%' || :token2 || '%')
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun searchProductsMultiToken(token1: String, token2: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("SELECT id FROM products WHERE isPendingSync = 1 AND isDeleted = 0 AND isDeletedLocally = 0")
    suspend fun getPendingSyncIds(): List<String>

    @Query("SELECT * FROM products WHERE isPendingSync = 1 AND isDeleted = 0 AND isDeletedLocally = 0")
    suspend fun getPendingSyncProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isDeletedLocally = 1 OR isDeleted = 1")
    suspend fun getPendingDeletedProducts(): List<ProductEntity>

    @Query("SELECT id FROM products WHERE isDeletedLocally = 1 OR isDeleted = 1")
    suspend fun getLocallyDeletedIds(): List<String>

    @Query("UPDATE products SET isDeleted = 1, isDeletedLocally = 1, isPendingSync = 1, deletedAt = :deletedAt, code = '__del__' || id || '__' || code WHERE id = :id AND (isDeletedLocally = 0 OR isDeleted = 0)")
    suspend fun markDeletedLocally(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM products WHERE isPendingSync = 0")
    suspend fun clearSyncedProducts()

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Transaction
    suspend fun syncRemoteProducts(products: List<ProductEntity>) {
        val deletedIds = getLocallyDeletedIds().toSet()
        val pendingSyncIds = getPendingSyncIds().toSet()
        val nonConflictingRemote = products.filter { it.id !in deletedIds && it.id !in pendingSyncIds }
        clearSyncedProducts()
        insertAll(nonConflictingRemote)
    }

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0 AND isDeletedLocally = 0")
    suspend fun getProductCount(): Int

    @Query("""
        UPDATE products 
        SET category = :newCategory, isPendingSync = 1, updatedAt = :updatedAt 
        WHERE category = :oldCategory COLLATE NOCASE AND isDeleted = 0 AND isDeletedLocally = 0
    """)
    suspend fun reassignCategory(oldCategory: String, newCategory: String, updatedAt: String): Int

    @Query("""
        SELECT COUNT(*) FROM products 
        WHERE category = :category COLLATE NOCASE AND isDeleted = 0 AND isDeletedLocally = 0
    """)
    suspend fun getProductCountForCategory(category: String): Int
}
