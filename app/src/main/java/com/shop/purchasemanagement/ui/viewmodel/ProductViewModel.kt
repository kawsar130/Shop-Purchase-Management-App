package com.shop.purchasemanagement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shop.purchasemanagement.data.model.Product
import com.shop.purchasemanagement.data.model.SyncStatus
import com.shop.purchasemanagement.data.repository.ProductRepository
import com.shop.purchasemanagement.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryStats(
    val totalCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalPurchaseValue: Double = 0.0,
    val totalSalesValue: Double = 0.0
)

class ProductViewModel(
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

    val categories: StateFlow<List<String>> = repository.allCategories
        .combine(_selectedCategory) { cats, _ ->
            listOf("All") + cats.filter { it.isNotBlank() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val filteredProducts: StateFlow<List<Product>> = combine(
        repository.allProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, category ->
        var list = products

        // 1. Filter by category
        if (category != "All" && category.isNotBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        // 2. Filter by search query (instant match on Name or Product Code)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(trimmedQuery, ignoreCase = true) ||
                it.code.contains(trimmedQuery, ignoreCase = true)
            }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<InventoryStats> = repository.allProducts.combine(_selectedCategory) { products, _ ->
        val count = products.size
        val lowStock = products.count { it.isLowStock }
        val purchaseVal = products.sumOf { it.totalPurchaseValue }
        val salesVal = products.sumOf { it.totalSalesValue }
        InventoryStats(count, lowStock, purchaseVal, salesVal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryStats())

    init {
        // Auto-sync in background on launch if URL is present
        viewModelScope.launch {
            if (userSession.getWebAppUrl().isNotEmpty()) {
                repository.syncFromRemote()
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
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
        purchasePrice: Double,
        salesPrice: Double,
        quantity: Int,
        onSuccess: () -> Unit
    ) {
        val trimmedCode = code.trim()
        val trimmedName = name.trim()

        if (trimmedCode.isEmpty()) {
            emitToast("Product Code / ID cannot be empty.")
            return
        }
        if (trimmedName.isEmpty()) {
            emitToast("Product Name cannot be empty.")
            return
        }
        if (salesPrice < 0 || purchasePrice < 0) {
            emitToast("Prices cannot be negative.")
            return
        }

        viewModelScope.launch {
            val product = Product(
                id = id ?: "",
                code = trimmedCode,
                name = trimmedName,
                category = category.trim().ifEmpty { "General" },
                purchasePrice = purchasePrice,
                salesPrice = salesPrice,
                quantity = quantity
            )

            val result = if (id.isNullOrEmpty()) {
                repository.addProduct(product)
            } else {
                repository.updateProduct(product)
            }

            if (result.isSuccess) {
                emitToast(if (id.isNullOrEmpty()) "Product added successfully!" else "Product updated!")
                onSuccess()
            } else {
                emitToast(result.exceptionOrNull()?.message ?: "Error saving product.")
            }
        }
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

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }
}

class ProductViewModelFactory(
    private val repository: ProductRepository,
    private val userSession: UserSessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository, userSession) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
