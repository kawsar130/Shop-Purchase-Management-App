package com.shopkeeper.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.ShopInfo
import com.shopkeeper.data.model.StaffMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val COLLECTION_SHOPS = "shops"
        private const val SUBCOLLECTION_PRODUCTS = "products"
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
                val ownerSnapshot = firestore.collection(COLLECTION_SHOPS)
                    .whereEqualTo("ownerUid", userUid)
                    .get()
                    .await()

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
                val collaboratorSnapshot = firestore.collection(COLLECTION_SHOPS)
                    .whereArrayContains("collaborators", normalizedEmail)
                    .get()
                    .await()

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

            shopDoc.set(shopData).await()

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
            val snapshot = firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .collection(SUBCOLLECTION_PRODUCTS)
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                val code = doc.getString("code") ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                val category = doc.getString("category") ?: "General"
                val purchasePrice = doc.getDouble("purchasePrice") ?: 0.0
                val salesPrice = doc.getDouble("salesPrice") ?: 0.0
                val quantity = (doc.getLong("quantity") ?: 0L).toInt()
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
        if (shopId.isEmpty()) return@withContext Result.failure(Exception("Shop ID is empty"))
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
                "updatedAt" to System.currentTimeMillis().toString()
            )

            productRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a product from the shop's Firestore subcollection.
     */
    suspend fun deleteProduct(shopId: String, productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (shopId.isEmpty() || productId.isEmpty()) return@withContext Result.failure(Exception("Invalid ID"))
        try {
            firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .collection(SUBCOLLECTION_PRODUCTS)
                .document(productId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
                        val purchasePrice = doc.getDouble("purchasePrice") ?: 0.0
                        val salesPrice = doc.getDouble("salesPrice") ?: 0.0
                        val quantity = (doc.getLong("quantity") ?: 0L).toInt()
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
            firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .update("currency", currency.trim(), "updatedAt", System.currentTimeMillis())
                .await()
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
            firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .update("collaborators", FieldValue.arrayUnion(normalized))
                .await()
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
            firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .update("collaborators", FieldValue.arrayRemove(normalized))
                .await()
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
            val snapshot = firestore.collection(COLLECTION_SHOPS)
                .document(shopId)
                .get()
                .await()

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
}
