package com.shopkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

        // 2. Filter by search query (instant match on Name or Product Code)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(trimmedQuery, ignoreCase = true) ||
                it.code.contains(trimmedQuery, ignoreCase = true)
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

    val filteredProducts: Flow<List<Product>> = uiState.map { it.products }
    val stats: Flow<InventoryStats> = uiState.map { it.stats }
    val isInitialLoading: Flow<Boolean> = uiState.map { it.isLoading }

    init {
        // Auto-sync in background on launch if shop spreadsheet/database is linked
        viewModelScope.launch {
            if (userSession.getSpreadsheetId().isNotEmpty()) {
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
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmedCode = code.trim()
        val trimmedName = name.trim()

        if (trimmedCode.isEmpty()) {
            val err = "Product Code / ID cannot be empty."
            if (onError != null) onError(err) else emitToast(err)
            return
        }
        if (trimmedName.isEmpty()) {
            val err = "Product Name cannot be empty."
            if (onError != null) onError(err) else emitToast(err)
            return
        }
        if (salesPrice < 0 || purchasePrice < 0) {
            val err = "Prices cannot be negative."
            if (onError != null) onError(err) else emitToast(err)
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
                val err = result.exceptionOrNull()?.message ?: "Error saving product."
                if (onError != null) {
                    onError(err)
                } else {
                    emitToast(err)
                }
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
