package com.shopkeeper.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shopkeeper.data.model.Product

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val purchasePrice: Double,
    val salesPrice: Double,
    val quantity: Int,
    val updatedAt: String,
    val isPendingSync: Boolean = false,
    val isDeletedLocally: Boolean = false
) {
    fun toDomain(): Product = Product(
        id = id,
        code = code,
        name = name,
        category = category,
        purchasePrice = purchasePrice,
        salesPrice = salesPrice,
        quantity = quantity,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(product: Product, isPendingSync: Boolean = false): ProductEntity = ProductEntity(
            id = product.id.ifEmpty { "PROD_" + System.currentTimeMillis() },
            code = product.code.trim(),
            name = product.name.trim(),
            category = product.category.trim().ifEmpty { "General" },
            purchasePrice = product.purchasePrice,
            salesPrice = product.salesPrice,
            quantity = product.quantity,
            updatedAt = product.updatedAt.ifEmpty { System.currentTimeMillis().toString() },
            isPendingSync = isPendingSync,
            isDeletedLocally = false
        )
    }
}
