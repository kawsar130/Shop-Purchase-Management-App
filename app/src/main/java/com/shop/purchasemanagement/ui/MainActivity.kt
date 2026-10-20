package com.shop.purchasemanagement.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shop.purchasemanagement.ShopApplication
import com.shop.purchasemanagement.ui.screens.AuthScreen
import com.shop.purchasemanagement.ui.screens.CategoryScreen
import com.shop.purchasemanagement.ui.screens.ProductListScreen
import com.shop.purchasemanagement.ui.screens.SettingsScreen
import com.shop.purchasemanagement.ui.screens.ShopSetupScreen
import com.shop.purchasemanagement.ui.screens.StaffManagementScreen
import com.shop.purchasemanagement.ui.theme.BluePrimary
import com.shop.purchasemanagement.ui.theme.PureWhite
import com.shop.purchasemanagement.ui.theme.ShopPurchaseManagementTheme
import com.shop.purchasemanagement.ui.viewmodel.AuthState
import com.shop.purchasemanagement.ui.viewmodel.AuthViewModel
import com.shop.purchasemanagement.ui.viewmodel.AuthViewModelFactory
import com.shop.purchasemanagement.ui.viewmodel.ProductViewModel
import com.shop.purchasemanagement.ui.viewmodel.ProductViewModelFactory
import com.shop.purchasemanagement.ui.viewmodel.SettingsViewModel
import com.shop.purchasemanagement.ui.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ShopApplication
        val userSession = app.userSessionRepository
        val productRepo = app.productRepository
        val driveSheetsService = app.driveSheetsService

        val authViewModel: AuthViewModel by viewModels {
            AuthViewModelFactory(applicationContext, userSession, driveSheetsService, productRepo)
        }

        val productViewModel: ProductViewModel by viewModels {
            ProductViewModelFactory(productRepo, userSession)
        }

        val settingsViewModel: SettingsViewModel by viewModels {
            SettingsViewModelFactory(userSession, productRepo)
        }

        setContent {
            ShopPurchaseManagementTheme {
                MainAppNavHost(
                    authViewModel = authViewModel,
                    productViewModel = productViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavHost(
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    val startDestination = when (authState) {
        is AuthState.Authenticated -> "products"
        is AuthState.NeedsShopSetup, is AuthState.SelectExistingShop -> "setup"
        else -> "auth"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Google Sign-In Screen
        composable("auth") {
            AuthScreen(
                authViewModel = authViewModel,
                onNeedsSetup = {
                    navController.navigate("setup") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onAuthenticated = {
                    navController.navigate("products") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        // 2. Automated Shop & Spreadsheet Setup Screen
        composable("setup") {
            ShopSetupScreen(
                authViewModel = authViewModel,
                onSetupCompleted = {
                    navController.navigate("products") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        // 3. Main Product List & Instant Price Checker Screen
        composable("products") {
            ProductListScreen(
                viewModel = productViewModel,
                onNavigateToCategories = { navController.navigate("categories") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        // 4. Category Grouping Screen
        composable("categories") {
            val products by productViewModel.filteredProducts.collectAsState()
            val currency by productViewModel.currencySymbol.collectAsState()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Product Categories", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = PureWhite,
                            titleContentColor = BluePrimary,
                            navigationIconContentColor = BluePrimary
                        )
                    )
                }
            ) { padding ->
                CategoryScreen(
                    products = products,
                    currencySymbol = currency,
                    onCategoryClick = { selectedCat ->
                        productViewModel.onCategorySelected(selectedCat)
                        navController.popBackStack()
                    },
                    onProductClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        // 5. Settings Screen
        composable("settings") {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Shop Settings", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = PureWhite,
                            titleContentColor = BluePrimary,
                            navigationIconContentColor = BluePrimary
                        )
                    )
                }
            ) { padding ->
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToStaff = { navController.navigate("staff") },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        // 6. Multi-User Staff Management Screen
        composable("staff") {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Staff & Multi-User Access", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = PureWhite,
                            titleContentColor = BluePrimary,
                            navigationIconContentColor = BluePrimary
                        )
                    )
                }
            ) { padding ->
                StaffManagementScreen(
                    viewModel = settingsViewModel,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
