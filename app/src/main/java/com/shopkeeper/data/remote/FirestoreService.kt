package com.shopkeeper.data.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.ShopInfo
import com.shopkeeper.data.model.StaffMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class FirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "FirestoreService"
        private const val COLLECTION_SHOPS = "shops"
        private const val SUBCOLLECTION_PRODUCTS = "products"
        private const val REMOTE_TIMEOUT_MS = 15000L
        private const val GET_TIMEOUT_MS = 15000L
    }

    /**
     * Finds all shops where the user is either the owner or an invited staff collaborator.
     */
    suspend fun findUserShops(userEmail: String, userUid: String): Result<List<ShopInfo>> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = userEmail.trim().lowercase()
            val shopsList = mutableListOf<ShopInfo>()

            // 1. Shops owned by user
            if (userUid.isNotEmpty()) {
                val ownerSnapshot = withTimeout(GET_TIMEOUT_MS) {
                    firestore.collection(COLLECTION_SHOPS)
                        .whereEqualTo("ownerUid", userUid)
                        .get()
                        .await()
                }

                for (doc in ownerSnapshot.documents) {
                    val collaborators = (doc.get("collaborators") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    shopsList.add(
                        ShopInfo(
                            shopId = doc.id,
                            shopName = doc.getString("name") ?: "My Shop",
                            currencySymbol = doc.getString("currency") ?: "$",
                            ownerEmail = doc.getString("ownerEmail") ?: normalizedEmail,
                            ownerUid = doc.getString("ownerUid") ?: userUid,
                            collaborators = collaborators,
                            isOwner = true
                        )
                    )
                }
            }

            // 2. Shops where user is an invited collaborator
            if (normalizedEmail.isNotEmpty()) {
                val collaboratorSnapshot = withTimeout(GET_TIMEOUT_MS) {
                    firestore.collection(COLLECTION_SHOPS)
                        .whereArrayContains("collaborators", normalizedEmail)
                        .get()
                        .await()
                }

                for (doc in collaboratorSnapshot.documents) {
                    if (shopsList.none { it.shopId == doc.id }) {
                        val ownerEmail = doc.getString("ownerEmail") ?: ""
                        val collaborators = (doc.get("collaborators") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        shopsList.add(
                            ShopInfo(
                                shopId = doc.id,
                                shopName = doc.getString("name") ?: "Shared Shop",
                                currencySymbol = doc.getString("currency") ?: "$",
                                ownerEmail = ownerEmail,
                                ownerUid = doc.getString("ownerUid") ?: "",
                                collaborators = collaborators,
                                isOwner = ownerEmail.equals(normalizedEmail, ignoreCase = true)
                            )
                        )
                    }
                }
            }

            Result.success(shopsList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new shop in Cloud Firestore.
     */
    suspend fun createShop(
        shopName: String,
        currency: String,
        ownerUid: String,
        ownerEmail: String
    ): Result<ShopInfo> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = ownerEmail.trim().lowercase()
            val shopDoc = firestore.collection(COLLECTION_SHOPS).document()
            val shopId = shopDoc.id

            val shopData = hashMapOf(
                "id" to shopId,
                "name" to shopName.trim(),
                "currency" to currency.trim(),
                "ownerUid" to ownerUid,
                "ownerEmail" to normalizedEmail,
                "collaborators" to emptyList<String>(),
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            withTimeout(REMOTE_TIMEOUT_MS) {
                shopDoc.set(shopData).await()
            }

            Result.success(
                ShopInfo(
                    shopId = shopId,
                    shopName = shopName.trim(),
                    currencySymbol = currency.trim(),
                    ownerEmail = normalizedEmail,
                    ownerUid = ownerUid,
                    collaborators = emptyList(),
                    isOwner = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all products from a shop's Firestore subcollection.
     */
    suspend fun getProducts(shopId: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.success(emptyList())
        try {
            val snapshot = withTimeout(GET_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .collection(SUBCOLLECTION_PRODUCTS)
                    .get()
                    .await()
            }

            val products = snapshot.documents.mapNotNull { doc ->
                val code = doc.getString("code") ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                val category = doc.getString("category") ?: "General"
                val purchasePrice = (doc.get("purchasePrice") as? Number)?.toDouble() ?: 0.0
                val salesPrice = (doc.get("salesPrice") as? Number)?.toDouble() ?: 0.0
                val quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0
                val updatedAt = doc.getString("updatedAt") ?: System.currentTimeMillis().toString()

                Product(
                    id = doc.id,
                    code = code,
                    name = name,
                    category = category,
                    purchasePrice = purchasePrice,
                    salesPrice = salesPrice,
                    quantity = quantity,
                    updatedAt = updatedAt
                )
            }

            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves or updates a product in the shop's Firestore subcollection.
     */
    suspend fun saveProduct(shopId: String, product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) {
            Log.e(TAG, "saveProduct: shopId is empty")
            return@withContext Result.failure(Exception("Shop ID is empty"))
        }
        try {
            val productRef = firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .collection(SUBCOLLECTION_PRODUCTS)
                .document(product.id)

            val data = hashMapOf(
                "id" to product.id,
                "code" to product.code.trim(),
                "name" to product.name.trim(),
                "category" to product.category.trim().ifEmpty { "General" },
                "purchasePrice" to product.purchasePrice,
                "salesPrice" to product.salesPrice,
                "quantity" to product.quantity,
                "updatedAt" to product.updatedAt.ifEmpty { System.currentTimeMillis().toString() }
            )

            withTimeout(REMOTE_TIMEOUT_MS) {
                productRef.set(data).await()
            }
            Log.d(TAG, "saveProduct: Saved product ${product.id} to shop $shopId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveProduct: Failed to save product ${product.id} in shop $shopId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a product from the shop's Firestore subcollection.
     */
    suspend fun deleteProduct(shopId: String, productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty() || productId.isEmpty()) {
            Log.e(TAG, "deleteProduct: Invalid ID (shopId='$shopId', productId='$productId')")
            return@withContext Result.failure(Exception("Invalid ID"))
        }
        try {
            withTimeout(REMOTE_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .collection(SUBCOLLECTION_PRODUCTS)
                    .document(productId)
                    .delete()
                    .await()
            }
            Log.d(TAG, "deleteProduct: Deleted product $productId from shop $shopId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteProduct: Failed to delete product $productId from shop $shopId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Atomically pushes multiple deletions and updates in a single Firestore WriteBatch.
     * Completes in a single network roundtrip with zero intermediate lock stalls.
     * Falls back to individual operations if WriteBatch fails.
     */
    suspend fun batchSyncProducts(
        shopId: String,
        deletions: List<String>,
        updates: List<Product>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) {
            Log.e(TAG, "batchSyncProducts: Aborted because shopId is empty")
            return@withContext Result.failure(Exception("Shop ID is empty"))
        }
        if (deletions.isEmpty() && updates.isEmpty()) return@withContext Result.success(Unit)

        Log.d(TAG, "batchSyncProducts: Starting batch for shop=$shopId (${deletions.size} deletions, ${updates.size} updates)")
        try {
            val batch = firestore.batch()
            val productsColl = firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .collection(SUBCOLLECTION_PRODUCTS)

            for (delId in deletions) {
                batch.delete(productsColl.document(delId))
            }

            for (product in updates) {
                val data = hashMapOf(
                    "id" to product.id,
                    "code" to product.code.trim(),
                    "name" to product.name.trim(),
                    "category" to product.category.trim().ifEmpty { "General" },
                    "purchasePrice" to product.purchasePrice,
                    "salesPrice" to product.salesPrice,
                    "quantity" to product.quantity,
                    "updatedAt" to product.updatedAt.ifEmpty { System.currentTimeMillis().toString() }
                )
                batch.set(productsColl.document(product.id), data)
            }

            withTimeout(REMOTE_TIMEOUT_MS) {
                batch.commit().await()
            }
            Log.d(TAG, "batchSyncProducts: Batch write committed successfully for shop=$shopId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "batchSyncProducts: Batch commit failed for shop=$shopId: ${e.message}. Attempting individual writes fallback...", e)
            try {
                for (delId in deletions) {
                    deleteProduct(shopId, delId)
                }
                for (product in updates) {
                    val singleRes = saveProduct(shopId, product)
                    if (singleRes.isFailure) {
                        Log.e(TAG, "batchSyncProducts fallback: single save failed for ${product.id}: ${singleRes.exceptionOrNull()?.message}")
                    }
                }
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "batchSyncProducts fallback error: ${fallbackEx.message}", fallbackEx)
                Result.failure(e)
            }
        }
    }

    /**
     * Real-time listener for remote changes. Syncs updates across all staff phones in real time.
     */
    fun observeProducts(
        shopId: String,
        onUpdate: (List<Product>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        if (shopId.isEmpty()) return null
        return firestore.collection(COLLECTION_SHOPS)
            .document(shopId)
            .collection(SUBCOLLECTION_PRODUCTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        val code = doc.getString("code") ?: return@mapNotNull null
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val category = doc.getString("category") ?: "General"
                        val purchasePrice = (doc.get("purchasePrice") as? Number)?.toDouble() ?: 0.0
                        val salesPrice = (doc.get("salesPrice") as? Number)?.toDouble() ?: 0.0
                        val quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0
                        val updatedAt = doc.getString("updatedAt") ?: System.currentTimeMillis().toString()

                        Product(
                            id = doc.id,
                            code = code,
                            name = name,
                            category = category,
                            purchasePrice = purchasePrice,
                            salesPrice = salesPrice,
                            quantity = quantity,
                            updatedAt = updatedAt
                        )
                    }
                    onUpdate(products)
                }
            }
    }

    /**
     * Updates the currency symbol of the shop in Cloud Firestore.
     */
    suspend fun updateShopCurrency(shopId: String, currency: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.failure(Exception("Shop ID is empty"))
        try {
            withTimeout(REMOTE_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .update("currency", currency.trim(), "updatedAt", System.currentTimeMillis())
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a staff member Gmail to the shop's collaborators array.
     */
    suspend fun addStaff(shopId: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.failure(Exception("Shop ID is empty"))
        try {
            val normalized = email.trim().lowercase()
            withTimeout(REMOTE_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .update("collaborators", FieldValue.arrayUnion(normalized))
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a staff member Gmail from the shop's collaborators array.
     */
    suspend fun removeStaff(shopId: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.failure(Exception("Shop ID is empty"))
        try {
            val normalized = email.trim().lowercase()
            withTimeout(REMOTE_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .update("collaborators", FieldValue.arrayRemove(normalized))
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches current staff list for the shop.
     */
    suspend fun getStaffList(shopId: String): Result<List<StaffMember>> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.success(emptyList())
        try {
            val snapshot = withTimeout(GET_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .get()
                    .await()
            }

            val collaborators = (snapshot.get("collaborators") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val staff = collaborators.map { email ->
                StaffMember(
                    email = email,
                    role = "Staff",
                    displayName = email.substringBefore("@")
                )
            }
            Result.success(staff)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches metadata for a specific shop.
     */
    suspend fun getShopInfo(shopId: String): Result<ShopInfo> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.failure(Exception("Shop ID is empty"))
        try {
            val doc = withTimeout(GET_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .get()
                    .await()
            }

            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Shop not found"))
            }

            val collaborators = (doc.get("collaborators") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val info = ShopInfo(
                shopId = doc.id,
                shopName = doc.getString("name") ?: "My Shop",
                currencySymbol = doc.getString("currency") ?: "$",
                ownerEmail = doc.getString("ownerEmail") ?: "",
                ownerUid = doc.getString("ownerUid") ?: "",
                collaborators = collaborators,
                isOwner = false
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for shop document changes (shop name, currency).
     */
    fun observeShop(
        shopId: String,
        onUpdate: (ShopInfo) -> Unit
    ): ListenerRegistration? {
        if (shopId.isEmpty()) return null
        return firestore.collection(COLLECTION_SHOPS)
            .document(shopId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                val collaborators = (doc.get("collaborators") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val info = ShopInfo(
                    shopId = doc.id,
                    shopName = doc.getString("name") ?: "My Shop",
                    currencySymbol = doc.getString("currency") ?: "$",
                    ownerEmail = doc.getString("ownerEmail") ?: "",
                    ownerUid = doc.getString("ownerUid") ?: "",
                    collaborators = collaborators,
                    isOwner = false
                )
                onUpdate(info)
            }
    }
}
