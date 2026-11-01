package com.shopkeeper.data.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
                val purchasePriceMinor = when (val raw = doc.get("purchasePriceMinor")) {
                    is Number -> raw.toLong()
                    else -> {
                        val legacyDouble = (doc.get("purchasePrice") as? Number)?.toDouble() ?: 0.0
                        kotlin.math.round(legacyDouble * 100.0).toLong()
                    }
                }
                val salesPriceMinor = when (val raw = doc.get("salesPriceMinor")) {
                    is Number -> raw.toLong()
                    else -> {
                        val legacyDouble = (doc.get("salesPrice") as? Number)?.toDouble() ?: 0.0
                        kotlin.math.round(legacyDouble * 100.0).toLong()
                    }
                }
                val quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0
                val updatedAt = doc.getString("updatedAt") ?: System.currentTimeMillis().toString()

                Product(
                    id = doc.id,
                    code = code,
                    name = name,
                    category = category,
                    purchasePriceMinor = purchasePriceMinor,
                    salesPriceMinor = salesPriceMinor,
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
                "purchasePriceMinor" to product.purchasePriceMinor,
                "salesPriceMinor" to product.salesPriceMinor,
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

    private sealed class BatchOp {
        data class Delete(val id: String) : BatchOp()
        data class SetProduct(val product: Product) : BatchOp()
    }

    /**
     * Atomically pushes multiple deletions and updates in chunks of at most 400 operations
     * per Firestore WriteBatch (safely below Firestore's 500-op limit).
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

        val totalOps = deletions.size + updates.size
        Log.d(TAG, "batchSyncProducts: Starting batch for shop=$shopId ($totalOps operations: ${deletions.size} deletions, ${updates.size} updates)")

        val operations: List<BatchOp> = deletions.map { BatchOp.Delete(it) } + updates.map { BatchOp.SetProduct(it) }
        val productsColl = firestore.collection(COLLECTION_SHOPS)
            .document(shopId)
            .collection(SUBCOLLECTION_PRODUCTS)

        // Chunk operations into batches of at most 400 to stay safely below Firestore's 500-op limit
        val chunks = operations.chunked(400)
        var failedCount = 0
        var lastError: Throwable? = null

        for ((index, chunk) in chunks.withIndex()) {
            try {
                val batch = firestore.batch()
                for (op in chunk) {
                    when (op) {
                        is BatchOp.Delete -> batch.delete(productsColl.document(op.id))
                        is BatchOp.SetProduct -> {
                            val product = op.product
                            val data = hashMapOf(
                                "id" to product.id,
                                "code" to product.code.trim(),
                                "name" to product.name.trim(),
                                "category" to product.category.trim().ifEmpty { "General" },
                                "purchasePriceMinor" to product.purchasePriceMinor,
                                "salesPriceMinor" to product.salesPriceMinor,
                                "purchasePrice" to product.purchasePrice,
                                "salesPrice" to product.salesPrice,
                                "quantity" to product.quantity,
                                "updatedAt" to product.updatedAt.ifEmpty { System.currentTimeMillis().toString() }
                            )
                            batch.set(productsColl.document(product.id), data)
                        }
                    }
                }

                withTimeout(REMOTE_TIMEOUT_MS) {
                    batch.commit().await()
                }
                Log.d(TAG, "batchSyncProducts: Chunk ${index + 1}/${chunks.size} (${chunk.size} ops) committed successfully")
            } catch (e: Exception) {
                // Fail fast on security / authentication rejections — retrying single operations is guaranteed to fail
                if (e is FirebaseFirestoreException &&
                    (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                     e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED)
                ) {
                    Log.e(TAG, "batchSyncProducts: Aborting sync - Firebase security rule / auth rejection: ${e.message}", e)
                    return@withContext Result.failure(e)
                }

                Log.w(TAG, "batchSyncProducts: Chunk ${index + 1}/${chunks.size} commit failed (${e.message}). Attempting verified individual writes fallback...", e)
                lastError = e

                for (op in chunk) {
                    val opResult = when (op) {
                        is BatchOp.Delete -> deleteProduct(shopId, op.id)
                        is BatchOp.SetProduct -> saveProduct(shopId, op.product)
                    }
                    if (opResult.isFailure) {
                        failedCount++
                        lastError = opResult.exceptionOrNull()
                    }
                }
            }
        }

        if (failedCount > 0) {
            Log.e(TAG, "batchSyncProducts: $failedCount of $totalOps operations failed")
            Result.failure(lastError ?: Exception("Failed to sync $failedCount operation(s)"))
        } else {
            Log.d(TAG, "batchSyncProducts: All $totalOps operations committed successfully")
            Result.success(Unit)
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
                        val purchasePriceMinor = when (val raw = doc.get("purchasePriceMinor")) {
                            is Number -> raw.toLong()
                            else -> {
                                val legacyDouble = (doc.get("purchasePrice") as? Number)?.toDouble() ?: 0.0
                                kotlin.math.round(legacyDouble * 100.0).toLong()
                            }
                        }
                        val salesPriceMinor = when (val raw = doc.get("salesPriceMinor")) {
                            is Number -> raw.toLong()
                            else -> {
                                val legacyDouble = (doc.get("salesPrice") as? Number)?.toDouble() ?: 0.0
                                kotlin.math.round(legacyDouble * 100.0).toLong()
                            }
                        }
                        val quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0
                        val updatedAt = doc.getString("updatedAt") ?: System.currentTimeMillis().toString()

                        Product(
                            id = doc.id,
                            code = code,
                            name = name,
                            category = category,
                            purchasePriceMinor = purchasePriceMinor,
                            salesPriceMinor = salesPriceMinor,
                            quantity = quantity,
                            updatedAt = updatedAt
                        )
                    }
                    onUpdate(products)
                }
            }
    }

    /**
     * Updates the name of the shop in Cloud Firestore.
     */
    suspend fun updateShopName(shopId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.failure(Exception("Shop ID is empty"))
        try {
            withTimeout(REMOTE_TIMEOUT_MS) {
                firestore.collection(COLLECTION_SHOPS)
                    .document(shopId)
                    .update("name", name.trim(), "updatedAt", System.currentTimeMillis())
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
     * Permanently deletes a shop and all products in its subcollection.
     * Batches product deletions in chunks of <= 400 operations to satisfy Firestore constraints.
     */
    suspend fun deleteShop(shopId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty()) return@withContext Result.failure(Exception("Shop ID is empty"))
        try {
            val shopRef = firestore.collection(COLLECTION_SHOPS).document(shopId)

            // 1. Fetch and delete all products in the subcollection
            val productsSnapshot = withTimeout(REMOTE_TIMEOUT_MS) {
                shopRef.collection(SUBCOLLECTION_PRODUCTS).get().await()
            }

            productsSnapshot.documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                for (doc in chunk) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }

            // 2. Delete the shop document itself
            withTimeout(REMOTE_TIMEOUT_MS) {
                shopRef.delete().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting shop $shopId: ${e.message}", e)
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
