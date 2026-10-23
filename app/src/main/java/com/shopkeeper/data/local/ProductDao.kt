package com.shopkeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE isDeletedLocally = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE isDeletedLocally = 0 
        AND (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE isDeletedLocally = 0 
        AND category = :category 
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT DISTINCT category FROM products 
        WHERE isDeletedLocally = 0 AND category != '' 
        ORDER BY category COLLATE NOCASE ASC
    """)
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE code = :code COLLATE NOCASE LIMIT 1")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Query("SELECT * FROM products WHERE isPendingSync = 1 OR isDeletedLocally = 1")
    suspend fun getPendingSyncProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET isDeletedLocally = 1, isPendingSync = 1 WHERE id = :id")
    suspend fun markDeletedLocally(id: String)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM products WHERE isPendingSync = 0")
    suspend fun clearSyncedProducts()

    @Query("SELECT COUNT(*) FROM products WHERE isDeletedLocally = 0")
    suspend fun getProductCount(): Int
}
