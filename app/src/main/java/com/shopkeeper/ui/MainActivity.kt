package com.shopkeeper.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shopkeeper.R
import com.shopkeeper.ui.navigation.Screen
import com.shopkeeper.ui.screens.AuthScreen
import com.shopkeeper.ui.screens.CategoryScreen
import com.shopkeeper.ui.screens.ProductListScreen
import com.shopkeeper.ui.screens.SettingsScreen
import com.shopkeeper.ui.screens.ShopSetupScreen
import com.shopkeeper.ui.screens.StaffManagementScreen
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.ShopKeeperTheme
import com.shopkeeper.ui.viewmodel.AuthState
import com.shopkeeper.ui.viewmodel.AuthViewModel
import com.shopkeeper.ui.viewmodel.ProductViewModel
import com.shopkeeper.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val productViewModel: ProductViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ShopKeeperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavHost(
                        authViewModel = authViewModel,
                        productViewModel = productViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
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
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val productUiState by productViewModel.uiState.collectAsStateWithLifecycle()

    val isInitializing = authState is AuthState.CheckingLogin ||
        (authState is AuthState.Authenticated && productUiState.isLoading)

    if (isInitializing) {
        ShopSplashScreen()
    } else {
        val startDestination = when (authState) {
            is AuthState.Authenticated -> Screen.Products.route
            is AuthState.NeedsShopSetup, is AuthState.SelectExistingShop -> Screen.Setup.route
            else -> Screen.Auth.route
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Google Sign-In Screen
            composable(Screen.Auth.route) {
                AuthScreen(
                    authViewModel = authViewModel,
                    onNeedsSetup = {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    onAuthenticated = {
                        navController.navigate(Screen.Products.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Automated Shop Setup Screen
            composable(Screen.Setup.route) {
                ShopSetupScreen(
                    authViewModel = authViewModel,
                    onSetupCompleted = {
                        navController.navigate(Screen.Products.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    },
                    onSignedOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // 3. Main Product List & Instant Price Checker Screen
            composable(Screen.Products.route) {
                ProductListScreen(
                    viewModel = productViewModel,
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // 4. Category Grouping Screen
            composable(Screen.Categories.route) {
                val products by productViewModel.unfilteredProducts.collectAsStateWithLifecycle()
                val categories by productViewModel.categories.collectAsStateWithLifecycle()
                val currency by productViewModel.currencySymbol.collectAsStateWithLifecycle()

                Scaffold(
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.categories_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                ) { padding ->
                    CategoryScreen(
                        products = products,
                        categories = categories,
                        currencySymbol = currency,
                        onCategoryClick = { selectedCat ->
                            productViewModel.onCategorySelected(selectedCat)
                            navController.popBackStack()
                        },
                        onProductClick = { product ->
                            productViewModel.onCategorySelected(product.category)
                            productViewModel.onSearchQueryChanged(product.code)
                            navController.popBackStack()
                        },
                        onAddCategory = { newCat ->
                            productViewModel.addCategory(newCat)
                        },
                        onRenameCategory = { oldName, newName ->
                            productViewModel.renameCategory(oldName, newName)
                        },
                        onDeleteCategory = { categoryToDelete, reassignTo ->
                            productViewModel.deleteCategory(categoryToDelete, reassignTo)
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            // 5. Settings Screen
            composable(Screen.Settings.route) {
                val settingsSnackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = { ShopSnackbarHost(settingsSnackbarHostState) },
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.settings_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                ) { padding ->
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        snackbarHostState = settingsSnackbarHostState,
                        onNavigateToStaff = { navController.navigate(Screen.Staff.route) },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onAccountDeleted = {
                            authViewModel.logout()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            // 6. Multi-User Staff Management Screen
            composable(Screen.Staff.route) {
                val staffSnackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = { ShopSnackbarHost(staffSnackbarHostState) },
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.staff_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                ) { padding ->
                    StaffManagementScreen(
                        viewModel = settingsViewModel,
                        snackbarHostState = staffSnackbarHostState,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun ShopSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState) { data ->
        val msg = data.visuals.message
        val isError = msg.contains("fail", ignoreCase = true) ||
            msg.contains("error", ignoreCase = true) ||
            msg.contains("cannot", ignoreCase = true) ||
            msg.contains("unable", ignoreCase = true) ||
            msg.contains("already used", ignoreCase = true) ||
            msg.contains("not found", ignoreCase = true)

        val icon = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle
        val iconTint = if (isError) RedAlert else GreenSuccess

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (isError) "Error" else "Success",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ShopSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BluePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = stringResource(R.string.app_name),
                    tint = PureWhite,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.auth_tagline),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
