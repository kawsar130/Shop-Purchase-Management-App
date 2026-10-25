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

    @Query("SELECT * FROM products WHERE isDeletedLocally = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT DISTINCT category FROM products 
        WHERE isDeletedLocally = 0 AND category != '' 
        ORDER BY category COLLATE NOCASE ASC
    """)
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE isDeletedLocally = 0 AND code = :code COLLATE NOCASE LIMIT 1")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE isPendingSync = 1 AND isDeletedLocally = 0")
    suspend fun getPendingSyncProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isDeletedLocally = 1")
    suspend fun getPendingDeletedProducts(): List<ProductEntity>

    @Query("SELECT id FROM products WHERE isDeletedLocally = 1")
    suspend fun getLocallyDeletedIds(): List<String>

    @Query("UPDATE products SET isDeletedLocally = 1, isPendingSync = 1 WHERE id = :id")
    suspend fun markDeletedLocally(id: String)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM products WHERE isPendingSync = 0")
    suspend fun clearSyncedProducts()

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Transaction
    suspend fun syncRemoteProducts(products: List<ProductEntity>) {
        val deletedIds = getLocallyDeletedIds().toSet()
        val nonDeletedRemote = products.filter { it.id !in deletedIds }
        clearSyncedProducts()
        insertAll(nonDeletedRemote)
    }

    @Query("SELECT COUNT(*) FROM products WHERE isDeletedLocally = 0")
    suspend fun getProductCount(): Int
}
