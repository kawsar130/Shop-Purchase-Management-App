package com.shopkeeper.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shopkeeper.data.model.Product
import java.util.UUID

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["code"]),
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
    val purchasePriceMinor: Long,
    val salesPriceMinor: Long,
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
        purchasePriceMinor = purchasePriceMinor,
        salesPriceMinor = salesPriceMinor,
        quantity = quantity,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(product: Product, isPendingSync: Boolean = false): ProductEntity {
            val generatedId = "PROD_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(6)
            return ProductEntity(
                id = product.id.ifEmpty { generatedId },
                code = product.code.trim(),
                name = product.name.trim(),
                category = product.category.trim().ifEmpty { "General" },
                purchasePriceMinor = product.purchasePriceMinor,
                salesPriceMinor = product.salesPriceMinor,
                quantity = product.quantity,
                updatedAt = product.updatedAt.ifEmpty { System.currentTimeMillis().toString() },
                isPendingSync = isPendingSync,
                isDeletedLocally = false
            )
        }
    }
}
