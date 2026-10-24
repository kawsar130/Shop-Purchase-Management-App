package com.shopkeeper.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shopkeeper.ShopApplication
import com.shopkeeper.ui.screens.AuthScreen
import com.shopkeeper.ui.screens.CategoryScreen
import com.shopkeeper.ui.screens.ProductListScreen
import com.shopkeeper.ui.screens.SettingsScreen
import com.shopkeeper.ui.screens.ShopSetupScreen
import com.shopkeeper.ui.screens.StaffManagementScreen
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.ShopKeeperTheme
import com.shopkeeper.ui.theme.Slate100
import com.shopkeeper.ui.theme.Slate500
import com.shopkeeper.ui.theme.Slate900
import com.shopkeeper.ui.viewmodel.AuthState
import com.shopkeeper.ui.viewmodel.AuthViewModel
import com.shopkeeper.ui.viewmodel.AuthViewModelFactory
import com.shopkeeper.ui.viewmodel.ProductViewModel
import com.shopkeeper.ui.viewmodel.ProductViewModelFactory
import com.shopkeeper.ui.viewmodel.SettingsViewModel
import com.shopkeeper.ui.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val app = application as ShopApplication
        val userSession = app.userSessionRepository
        val productRepo = app.productRepository
        val firestoreService = app.firestoreService

        val authViewModel: AuthViewModel by viewModels {
            AuthViewModelFactory(applicationContext, userSession, firestoreService, productRepo)
        }

        val productViewModel: ProductViewModel by viewModels {
            ProductViewModelFactory(productRepo, userSession)
        }

        val settingsViewModel: SettingsViewModel by viewModels {
            SettingsViewModelFactory(userSession, productRepo)
        }

        setContent {
            ShopKeeperTheme {
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
    val productUiState by productViewModel.uiState.collectAsState()

    val isInitializing = authState is AuthState.CheckingLogin ||
        (authState is AuthState.Authenticated && productUiState.isLoading)

    if (isInitializing) {
        ShopSplashScreen()
    } else {
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

        // 2. Automated Shop Setup Screen
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
            val products = productUiState.products
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
}

@Composable
fun ShopSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate100),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ShopKeeper Icon Container
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BluePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = "ShopKeeper",
                    tint = PureWhite,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "ShopKeeper",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Effortless Shop & Inventory",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = BluePrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
