package com.shopkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryStats(
    val totalCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalPurchaseValue: Double = 0.0,
    val totalSalesValue: Double = 0.0
)

data class ProductListUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val stats: InventoryStats = InventoryStats()
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val userSession: UserSessionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus

    val currencySymbol: StateFlow<String> = userSession.currencySymbolFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    val unfilteredProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = combine(
        repository.allProducts,
        userSession.customCategoriesFlow
    ) { products, customCats ->
        val productCats = products.map { it.category.trim() }.filter { it.isNotBlank() }
        val allDistinct = (productCats + customCats)
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        if (allDistinct.isEmpty()) {
            emptyList()
        } else {
            listOf("All") + allDistinct
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ProductListUiState> = combine(
        repository.allProducts,
        _searchQuery,
        _selectedCategory
    ) { allProducts, query, category ->
        val count = allProducts.size
        val lowStock = allProducts.count { it.isLowStock }
        val purchaseVal = allProducts.sumOf { it.totalPurchaseValue }
        val salesVal = allProducts.sumOf { it.totalSalesValue }
        val calculatedStats = InventoryStats(count, lowStock, purchaseVal, salesVal)

        var list = allProducts

        // 1. Filter by category
        if (category != "All" && category.isNotBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        // 2. Multi-Token Search: all tokens must match simultaneously across name, code, or category
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotBlank()) {
            val tokens = trimmedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
            list = list.filter { product ->
                val searchableText = "${product.name} ${product.code} ${product.category}"
                tokens.all { token ->
                    val cleanToken = token.removePrefix("#")
                    searchableText.contains(token, ignoreCase = true) ||
                        (cleanToken.isNotEmpty() && searchableText.contains(cleanToken, ignoreCase = true))
                }
            }
        }

        ProductListUiState(
            isLoading = false,
            products = list,
            stats = calculatedStats
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ProductListUiState(isLoading = true)
    )

    init {
        // Auto-sync in background on launch if shop database is linked
        viewModelScope.launch {
            if (userSession.getShopId().isNotEmpty()) {
                repository.syncFromRemote()
            }
        }

        // Reset filter states when inventory is cleared
        viewModelScope.launch {
            repository.allProducts.collect { products ->
                if (products.isEmpty()) {
                    if (_selectedCategory.value != "All") _selectedCategory.value = "All"
                    if (_searchQuery.value.isNotEmpty()) _searchQuery.value = ""
                }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun addCategory(category: String) {
        viewModelScope.launch {
            userSession.saveCustomCategory(category)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.syncFromRemote()
        }
    }

    fun saveProduct(
        id: String?,
        code: String,
        name: String,
        category: String,
        purchasePriceMinor: Long,
        salesPriceMinor: Long,
        quantity: Int,
        unit: String = "pcs",
        lowStockThreshold: Int = 0,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmedRawCode = code.trim().removePrefix("#")
        val finalCode = if (trimmedRawCode.isEmpty()) {
            val maxExisting = unfilteredProducts.value
                .mapNotNull { it.code.trim().removePrefix("#").toIntOrNull() }
                .maxOrNull() ?: 1000
            val nextNumber = (maxExisting + 1).coerceAtLeast(1001)
            "#$nextNumber"
        } else {
            code.trim()
        }
        val trimmedName = name.trim()

        if (trimmedName.isEmpty()) {
            val err = "Product Name cannot be empty."
            if (onError != null) onError(err) else emitToast(err)
            return
        }
        if (salesPriceMinor < 0L || purchasePriceMinor < 0L) {
            val err = "Prices cannot be negative."
            if (onError != null) onError(err) else emitToast(err)
            return
        }
        if (quantity < 0) {
            val err = "Quantity cannot be negative."
            if (onError != null) onError(err) else emitToast(err)
            return
        }
        // Max 1 billion major units = 100_000_000_000L minor units
        if (salesPriceMinor > 100_000_000_000L || purchasePriceMinor > 100_000_000_000L) {
            val err = "Price cannot exceed 1,000,000,000."
            if (onError != null) onError(err) else emitToast(err)
            return
        }
        if (quantity > 1_000_000) {
            val err = "Quantity cannot exceed 1,000,000."
            if (onError != null) onError(err) else emitToast(err)
            return
        }

        viewModelScope.launch {
            val product = Product(
                id = id ?: "",
                code = finalCode,
                name = trimmedName,
                category = category.trim().ifEmpty { "General" },
                purchasePriceMinor = purchasePriceMinor.coerceAtLeast(0L),
                salesPriceMinor = salesPriceMinor,
                quantity = quantity,
                unit = unit.ifBlank { "pcs" },
                lowStockThreshold = lowStockThreshold.coerceAtLeast(0)
            )

            val result = if (id.isNullOrEmpty()) {
                repository.addProduct(product)
            } else {
                repository.updateProduct(product)
            }

            if (result.isSuccess) {
                userSession.saveCustomCategory(product.category)
                emitToast(if (id.isNullOrEmpty()) "Product added successfully!" else "Product updated!")
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error saving product."
                if (onError != null) {
                    onError(err)
                } else {
                    emitToast(err)
                }
            }
        }
    }

    fun saveProduct(
        id: String?,
        code: String,
        name: String,
        category: String,
        purchasePrice: Double,
        salesPrice: Double,
        quantity: Int,
        unit: String = "pcs",
        lowStockThreshold: Int = 0,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val purchasePriceMinor = kotlin.math.round(purchasePrice * 100.0).toLong()
        val salesPriceMinor = kotlin.math.round(salesPrice * 100.0).toLong()
        saveProduct(
            id = id,
            code = code,
            name = name,
            category = category,
            purchasePriceMinor = purchasePriceMinor,
            salesPriceMinor = salesPriceMinor,
            quantity = quantity,
            unit = unit,
            lowStockThreshold = lowStockThreshold,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val result = repository.deleteProduct(product)
            if (result.isSuccess) {
                emitToast("Deleted '${product.name}'")
            } else {
                emitToast("Failed to delete: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun getSuggestedNextCode(): String {
        val maxExisting = unfilteredProducts.value
            .mapNotNull { it.code.trim().removePrefix("#").toIntOrNull() }
            .maxOrNull() ?: 1000
        return (maxExisting + 1).coerceAtLeast(1001).toString()
    }

    fun emitToastMessage(msg: String) {
        emitToast(msg)
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }
}
