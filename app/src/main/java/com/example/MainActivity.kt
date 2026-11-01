package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogProductEntity
import com.example.data.model.OrderItem
import com.example.data.model.OrderEntity
import com.example.data.parser.ParsedOrderDraft
import com.example.data.remote.SteadfastApiService
import com.example.data.repository.AuthRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CourierRepository
import com.example.data.repository.OrderRepository
import com.example.ui.screens.CreateEditOrderScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrdersListScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartPasteScreen
import com.example.ui.theme.AppStrings
import com.example.ui.theme.FashionAuraTheme
import com.example.ui.theme.auraTokens
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.OrderViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.example.data.repository.OwnerVerificationResult
import androidx.compose.material.icons.filled.Lock

/**
 * Application-wide root authentication state for Stage C-C.
 * Evaluates local Firebase Auth state and Owner custom claims to gate OMS screens.
 */
sealed interface RootAuthState {
    object Initializing : RootAuthState
    object Unauthenticated : RootAuthState
    data class Authenticated(val user: FirebaseUser, val isOffline: Boolean = false) : RootAuthState
    data class EmailVerificationPending(val user: FirebaseUser) : RootAuthState
    data class AccessDenied(val email: String) : RootAuthState
    data class Error(val message: String) : RootAuthState
}

sealed class Screen {
    object Dashboard : Screen()
    object OrdersList : Screen()
    object Products : Screen()
    object Reports : Screen()
    object Settings : Screen()
    object SmartPaste : Screen()
    object LoginPreview : Screen()
    data class CreateOrder(val draft: ParsedOrderDraft? = null, val initialItem: OrderItem? = null) : Screen()
    data class EditOrder(val order: OrderEntity) : Screen()
    data class OrderDetail(val order: OrderEntity) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val orderRepository = OrderRepository(database.orderDao(), database)
        val catalogRepository = CatalogRepository(database.catalogDao())
        val apiService = SteadfastApiService.create()
        val courierRepository = CourierRepository(apiService, database.orderDao())
        val preferencesManager = PreferencesManager(applicationContext)
        val authRepository = AuthRepository()

        setContent {
            val orderViewModel: OrderViewModel = viewModel(
                factory = OrderViewModel.Factory(orderRepository, courierRepository, catalogRepository)
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(preferencesManager, courierRepository)
            )
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(authRepository)
            )

            // Stage C-C: Observe local Firebase Auth state and verify Owner custom claims
            var retryKey by remember { mutableStateOf(0) }
            val rootAuthState by produceState<RootAuthState>(initialValue = RootAuthState.Initializing, retryKey) {
                try {
                    authRepository.currentAuthUserFlow.collect { user ->
                        if (user == null) {
                            value = RootAuthState.Unauthenticated
                        } else if (!user.isEmailVerified) {
                            value = RootAuthState.EmailVerificationPending(user)
                        } else {
                            when (val verification = authRepository.validateOwnerClaims(user)) {
                                is OwnerVerificationResult.Authorized -> {
                                    value = RootAuthState.Authenticated(user, isOffline = verification.isOffline)
                                }
                                is OwnerVerificationResult.AccessDenied -> {
                                    value = RootAuthState.AccessDenied(verification.email)
                                }
                                is OwnerVerificationResult.EmailNotVerified -> {
                                    value = RootAuthState.EmailVerificationPending(user)
                                }
                                is OwnerVerificationResult.Unauthenticated -> {
                                    value = RootAuthState.Unauthenticated
                                }
                                is OwnerVerificationResult.Error -> {
                                    value = RootAuthState.Error(verification.message)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    value = RootAuthState.Error(e.message ?: "Authentication state observation error")
                }
            }

            val userPreferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
            val isDark = when (userPreferences.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            FashionAuraTheme(darkTheme = isDark) {
                // ROOT AUTHENTICATION GATE:
                // Prevents Dashboard, customer data, and OMS screens from composing
                // until local Firebase authentication state and Owner custom claims are evaluated.
                when (val authState = rootAuthState) {
                    is RootAuthState.Initializing -> {
                        InitializingScreen()
                    }
                    is RootAuthState.Unauthenticated,
                    is RootAuthState.EmailVerificationPending -> {
                        LoginScreen(
                            authViewModel = authViewModel,
                            language = userPreferences.appLanguage,
                            onLoginSuccess = {
                                // Once authenticated, currentAuthUserFlow emits the verified user,
                                // which automatically evaluates claims and transitions rootAuthState.
                            },
                            onNavigateBack = null // Root gate has no exit button
                        )
                    }
                    is RootAuthState.AccessDenied -> {
                        AccessDeniedScreen(
                            email = authState.email,
                            language = userPreferences.appLanguage,
                            onSwitchAccount = {
                                authRepository.signOut()
                                retryKey++
                            }
                        )
                    }
                    is RootAuthState.Authenticated -> {
                        FashionAuraApp(
                            orderViewModel = orderViewModel,
                            settingsViewModel = settingsViewModel,
                            authViewModel = authViewModel
                        )
                    }
                    is RootAuthState.Error -> {
                        AuthGateErrorScreen(
                            message = authState.message,
                            language = userPreferences.appLanguage,
                            onRetry = { retryKey++ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FashionAuraApp(
    orderViewModel: OrderViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(AuthRepository()))
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userPreferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isCurrentlyDark = userPreferences.isDarkMode ?: isSystemDark
    val filteredOrders by orderViewModel.filteredOrders.collectAsStateWithLifecycle()
    val allOrders by orderViewModel.allOrders.collectAsStateWithLifecycle()
    val trashedOrders by orderViewModel.trashedOrders.collectAsStateWithLifecycle()
    val catalogProducts by orderViewModel.catalogProducts.collectAsStateWithLifecycle()
    val metrics by orderViewModel.metrics.collectAsStateWithLifecycle()
    val selectedTab by orderViewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by orderViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedProductFilter by orderViewModel.selectedProductFilter.collectAsStateWithLifecycle()
    val selectedSizeFilter by orderViewModel.selectedSizeFilter.collectAsStateWithLifecycle()
    val selectedColorFilter by orderViewModel.selectedColorFilter.collectAsStateWithLifecycle()
    val isPendingCourierFilterActive by orderViewModel.isPendingCourierFilterActive.collectAsStateWithLifecycle()
    val bulkCourierSyncState by orderViewModel.bulkCourierSyncState.collectAsStateWithLifecycle()
    val balanceState by settingsViewModel.balanceState.collectAsStateWithLifecycle()
    val isCheckingBalance by settingsViewModel.isCheckingBalance.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    // Listen to snackbar messages from OrderViewModel
    LaunchedEffect(Unit) {
        orderViewModel.snackbarMessage.collectLatest { message ->
            val hasUndo = orderViewModel.undoAction.value != null
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (hasUndo) {
                    if (userPreferences.appLanguage == AppLanguage.BANGLA) "পূর্বাবস্থায় ফিরুন" else "Undo"
                } else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                orderViewModel.undoLastAction()
            }
        }
    }

    // Handle Android system back gesture
    if (currentScreen !is Screen.Dashboard) {
        BackHandler {
            currentScreen = if (currentScreen is Screen.LoginPreview) Screen.Settings else Screen.Dashboard
        }
    }

    val isTopLevelScreen = currentScreen is Screen.Dashboard ||
            currentScreen is Screen.OrdersList ||
            currentScreen is Screen.Products ||
            currentScreen is Screen.Reports ||
            currentScreen is Screen.Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isTopLevelScreen) {
                FashionAuraBottomNavigation(
                    currentScreen = currentScreen,
                    language = userPreferences.appLanguage,
                    onNavigate = { screen -> currentScreen = screen },
                    onAddOrderClick = { currentScreen = Screen.CreateOrder() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        orders = filteredOrders,
                        allOrders = allOrders,
                        catalogProducts = catalogProducts,
                        metrics = metrics,
                        selectedTab = selectedTab,
                        searchQuery = searchQuery,
                        selectedProductFilter = selectedProductFilter,
                        selectedSizeFilter = selectedSizeFilter,
                        selectedColorFilter = selectedColorFilter,
                        isPendingCourierFilterActive = isPendingCourierFilterActive,
                        userPreferences = userPreferences,
                        onTabSelected = { tab -> orderViewModel.setFilterTab(tab) },
                        onSearchQueryChange = { q -> orderViewModel.setSearchQuery(q) },
                        onProductFilterChange = { p -> orderViewModel.setProductFilter(p) },
                        onSizeFilterChange = { s -> orderViewModel.setSizeFilter(s) },
                        onColorFilterChange = { c -> orderViewModel.setColorFilter(c) },
                        onTogglePendingCourierFilter = { orderViewModel.togglePendingCourierFilter() },
                        onClearFilters = { orderViewModel.clearProductAndSizeFilters() },
                        onMarkAsShipped = { order -> orderViewModel.markAsShipped(order) },
                        onStockArrived = { order -> orderViewModel.markAsProcessing(order) },
                        onBookCourier = { order ->
                            orderViewModel.bookCourierDirect(
                                order,
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        },
                        onCardClick = { order -> currentScreen = Screen.OrderDetail(order) },
                        onEditOrder = { order -> currentScreen = Screen.EditOrder(order) },
                        onDeleteOrder = { order -> orderViewModel.moveToTrash(order) },
                        onBulkStatusChange = { ids, status -> orderViewModel.bulkUpdateStatus(ids, status) },
                        onBulkRetrySync = { selectedList ->
                            orderViewModel.bulkRetryCourierSync(
                                selectedList,
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        },
                        onBulkDelete = { ids -> orderViewModel.bulkMoveToTrash(ids) },
                        onBulkConvertToRegular = { ids -> orderViewModel.bulkConvertToRegular(ids) },
                        onBulkUpdateDeliveryDate = { ids, date -> orderViewModel.bulkUpdateDeliveryDate(ids, date) },
                        onBulkUpdateStatusWithPayment = { ids, status, payment -> orderViewModel.bulkUpdateStatusWithPayment(ids, status, payment) },
                        onBulkAssignDress = { ids, product, keepExisting, overrideSize, overrideColor ->
                            orderViewModel.bulkAssignCatalogProduct(ids, product, keepExisting, overrideSize, overrideColor)
                        },
                        bulkCourierSyncState = bulkCourierSyncState,
                        onStartBulkCourierSync = { selectedList ->
                            orderViewModel.startBulkCourierSync(
                                selectedList,
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        },
                        onStopBulkCourierSync = { orderViewModel.stopBulkCourierSync() },
                        onDismissBulkCourierSync = { orderViewModel.dismissBulkSyncDialog() },
                        onNavigateToAddOrder = { currentScreen = Screen.CreateOrder() },
                        onNavigateToSmartPaste = { currentScreen = Screen.SmartPaste },
                        onNavigateToOrders = { currentScreen = Screen.OrdersList },
                        onNavigateToProducts = { currentScreen = Screen.Products },
                        onNavigateToReports = { currentScreen = Screen.Reports },
                        onToggleDarkMode = {
                            settingsViewModel.setDarkMode(!isCurrentlyDark)
                        },
                        onLanguageChange = { lang -> settingsViewModel.setAppLanguage(lang) },
                        onDigitChange = { useBangla -> settingsViewModel.setUseBanglaDigits(useBangla) }
                    )
                }

                is Screen.OrdersList -> {
                    OrdersListScreen(
                        orders = filteredOrders,
                        allOrders = allOrders,
                        allOrdersCount = allOrders.size,
                        selectedTab = selectedTab,
                        searchQuery = searchQuery,
                        language = userPreferences.appLanguage,
                        useBanglaDigits = userPreferences.useBanglaDigits,
                        catalogProducts = catalogProducts,
                        selectedProductFilter = selectedProductFilter,
                        selectedSizeFilter = selectedSizeFilter,
                        selectedColorFilter = selectedColorFilter,
                        isPendingCourierFilterActive = isPendingCourierFilterActive,
                        onProductFilterChange = { prod -> orderViewModel.setProductFilter(prod) },
                        onSizeFilterChange = { sz -> orderViewModel.setSizeFilter(sz) },
                        onColorFilterChange = { col -> orderViewModel.setColorFilter(col) },
                        onTogglePendingCourierFilter = { orderViewModel.togglePendingCourierFilter() },
                        onClearFilters = { orderViewModel.clearProductAndSizeFilters() },
                        onTabSelected = { tab -> orderViewModel.setFilterTab(tab) },
                        onSearchQueryChange = { q -> orderViewModel.setSearchQuery(q) },
                        onMarkAsShipped = { order -> orderViewModel.markAsShipped(order) },
                        onStockArrived = { order -> orderViewModel.markAsProcessing(order) },
                        onBookCourier = { order ->
                            orderViewModel.bookCourierDirect(
                                order,
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        },
                        onCardClick = { order -> currentScreen = Screen.OrderDetail(order) },
                        onEditOrder = { order -> currentScreen = Screen.EditOrder(order) },
                        onDeleteOrder = { order -> orderViewModel.moveToTrash(order) },
                        onBulkStatusChange = { ids, status -> orderViewModel.bulkUpdateStatus(ids, status) },
                        onBulkRetrySync = { selectedList ->
                            orderViewModel.bulkRetryCourierSync(
                                selectedList,
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        },
                        onBulkDelete = { ids -> orderViewModel.bulkMoveToTrash(ids) },
                        onBulkConvertToRegular = { ids -> orderViewModel.bulkConvertToRegular(ids) },
                        onBulkUpdateDeliveryDate = { ids, date -> orderViewModel.bulkUpdateDeliveryDate(ids, date) },
                        onBulkUpdateStatusWithPayment = { ids, status, payment -> orderViewModel.bulkUpdateStatusWithPayment(ids, status, payment) },
                        onBulkAssignDress = { ids, product, keepExisting, overrideSize, overrideColor ->
                            orderViewModel.bulkAssignCatalogProduct(ids, product, keepExisting, overrideSize, overrideColor)
                        },
                        bulkCourierSyncState = bulkCourierSyncState,
                        onStartBulkCourierSync = { selectedList ->
                            orderViewModel.startBulkCourierSync(
                                selectedList,
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        },
                        onStopBulkCourierSync = { orderViewModel.stopBulkCourierSync() },
                        onDismissBulkCourierSync = { orderViewModel.dismissBulkSyncDialog() }
                    )
                }

                is Screen.Products -> {
                    ProductsScreen(
                        catalogProducts = catalogProducts,
                        language = userPreferences.appLanguage,
                        useBanglaDigits = userPreferences.useBanglaDigits,
                        onSaveProduct = { product -> orderViewModel.saveCatalogProduct(product) },
                        onDeleteProduct = { product -> orderViewModel.deleteCatalogProduct(product.id, product.name) },
                        onProductSelect = { product ->
                            val item = OrderItem(
                                name = product.name,
                                size = product.availableSizes.firstOrNull() ?: "40",
                                color = product.availableColors.firstOrNull() ?: "",
                                quantity = 1,
                                unitPrice = product.basePrice,
                                photoUri = product.photoUri
                            )
                            currentScreen = Screen.CreateOrder(initialItem = item)
                        }
                    )
                }

                is Screen.Reports -> {
                    ReportsScreen(
                        orders = allOrders,
                        language = userPreferences.appLanguage,
                        useBanglaDigits = userPreferences.useBanglaDigits
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        userPreferences = userPreferences,
                        trashedOrders = trashedOrders,
                        pendingSyncCount = allOrders.count { it.courierSyncStatus == com.example.data.model.CourierSyncStatus.PENDING_SYNC },
                        balanceState = balanceState,
                        isCheckingBalance = isCheckingBalance,
                        onSaveSteadfastCredentials = { k, s -> settingsViewModel.saveSteadfastCredentials(k, s) },
                        onCheckBalance = { k, s -> settingsViewModel.checkSteadfastBalance(k, s) },
                        onBulkSyncPending = {
                            orderViewModel.syncAllPendingOrders(
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        },
                        onToggleDarkMode = {
                            settingsViewModel.setDarkMode(!isCurrentlyDark)
                        },
                        onLanguageChange = { lang -> settingsViewModel.setAppLanguage(lang) },
                        onDigitChange = { useBangla -> settingsViewModel.setUseBanglaDigits(useBangla) },
                        onRestoreOrder = { id -> orderViewModel.restoreFromTrash(id) },
                        onDeletePermanently = { id -> orderViewModel.deletePermanently(id) },
                        onEmptyTrash = { orderViewModel.emptyTrashSafe(context) },
                        onRestoreJson = { json -> orderViewModel.restoreOrdersFromJson(json) },
                        onRestoreBackupStream = { stream -> orderViewModel.restoreFromBackupStream(context, stream) },
                        allOrders = allOrders,
                        catalogProducts = catalogProducts,
                        onNavigateToLoginPreview = { currentScreen = Screen.LoginPreview }
                    )
                }

                is Screen.LoginPreview -> {
                    BackHandler {
                        currentScreen = Screen.Settings
                    }
                    LoginScreen(
                        authViewModel = authViewModel,
                        language = userPreferences.appLanguage,
                        onLoginSuccess = {
                            currentScreen = Screen.Settings
                        },
                        onNavigateBack = {
                            currentScreen = Screen.Settings
                        }
                    )
                }

                is Screen.SmartPaste -> {
                    SmartPasteScreen(
                        allOrders = allOrders,
                        catalogProducts = catalogProducts,
                        language = userPreferences.appLanguage,
                        useBanglaDigits = userPreferences.useBanglaDigits,
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        onConfirmDraft = { draft ->
                            currentScreen = Screen.CreateOrder(draft = draft)
                        }
                    )
                }

                is Screen.CreateOrder -> {
                    val initialOrder = if (screen.draft != null) {
                        OrderEntity(
                            invoiceNo = "INV-${allOrders.size + 1506}",
                            customerName = screen.draft.customerName,
                            customerPhone = screen.draft.customerPhone,
                            customerAddress = screen.draft.customerAddress,
                            district = screen.draft.district,
                            thana = screen.draft.thana,
                            orderType = screen.draft.orderType,
                            items = screen.draft.toOrderItems(),
                            deliveryTier = screen.draft.deliveryTier,
                            deliveryCharge = screen.draft.deliveryCharge,
                            advanceAmount = screen.draft.advanceAmount
                        )
                    } else if (screen.initialItem != null) {
                        OrderEntity(
                            invoiceNo = "INV-${allOrders.size + 1506}",
                            customerName = "",
                            customerPhone = "",
                            customerAddress = "",
                            items = listOf(screen.initialItem)
                        )
                    } else {
                        null
                    }

                    CreateEditOrderScreen(
                        initialOrder = initialOrder,
                        parsedDraft = screen.draft,
                        totalOrdersCount = allOrders.size,
                        catalogProducts = catalogProducts,
                        language = userPreferences.appLanguage,
                        useBanglaDigits = userPreferences.useBanglaDigits,
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        onCheckCustomerHistory = { phone -> orderViewModel.getCustomerHistory(phone) },
                        onSaveOrder = { newOrder ->
                            orderViewModel.createOrder(newOrder) {
                                currentScreen = Screen.Dashboard
                            }
                        }
                    )
                }

                is Screen.EditOrder -> {
                    CreateEditOrderScreen(
                        initialOrder = screen.order,
                        totalOrdersCount = allOrders.size,
                        catalogProducts = catalogProducts,
                        language = userPreferences.appLanguage,
                        useBanglaDigits = userPreferences.useBanglaDigits,
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        onCheckCustomerHistory = { phone -> orderViewModel.getCustomerHistory(phone) },
                        onSaveOrder = { updatedOrder ->
                            orderViewModel.updateOrder(updatedOrder) {
                                currentScreen = Screen.Dashboard
                            }
                        }
                    )
                }

                is Screen.OrderDetail -> {
                    OrderDetailScreen(
                        order = screen.order,
                        language = userPreferences.appLanguage,
                        useBanglaDigits = userPreferences.useBanglaDigits,
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        onEditOrder = { currentScreen = Screen.EditOrder(screen.order) },
                        onMarkAsShipped = {
                            orderViewModel.markAsShipped(screen.order)
                            currentScreen = Screen.Dashboard
                        },
                        onStockArrived = {
                            orderViewModel.markAsProcessing(screen.order)
                            currentScreen = Screen.Dashboard
                        },
                        onSaveConsignmentId = { cid ->
                            orderViewModel.saveConsignmentId(screen.order.id, cid)
                        },
                        onBookSteadfastApi = {
                            orderViewModel.bookSteadfastApi(
                                screen.order,
                                userPreferences.steadfastApiKey,
                                userPreferences.steadfastSecretKey
                            )
                        }
                    )
                }
            }
        }

        val restorePreview by orderViewModel.restorePreviewState.collectAsStateWithLifecycle()
        if (restorePreview != null) {
            com.example.ui.components.SafeRestoreConfirmationDialog(
                previewState = restorePreview!!,
                language = userPreferences.appLanguage,
                onDismiss = { orderViewModel.dismissRestorePreview() },
                onConfirmRestore = { strategy ->
                    orderViewModel.executeSafeRestore(context, strategy)
                }
            )
        }
    }
}

@Composable
fun FashionAuraBottomNavigation(
    currentScreen: Screen,
    language: AppLanguage,
    onNavigate: (Screen) -> Unit,
    onAddOrderClick: () -> Unit
) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = if (isDark) tokens.background else MaterialTheme.colorScheme.surface,
            border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, tokens.borderSubtle) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                containerColor = if (isDark) tokens.background else MaterialTheme.colorScheme.surface,
                tonalElevation = if (isDark) 0.dp else 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDark) Color.White else MaterialTheme.colorScheme.secondary,
                    selectedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.secondary,
                    indicatorColor = if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                )

                NavigationBarItem(
                    selected = currentScreen is Screen.Dashboard,
                    onClick = { onNavigate(Screen.Dashboard) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Orders") },
                    label = {
                        Text(
                            AppStrings.navOrders(language),
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen is Screen.Dashboard) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = currentScreen is Screen.Products,
                    onClick = { onNavigate(Screen.Products) },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Catalog") },
                    label = {
                        Text(
                            AppStrings.navProducts(language),
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen is Screen.Products) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors
                )

                // Center Spacer for elevated Add Order FAB
                NavigationBarItem(
                    selected = false,
                    onClick = onAddOrderClick,
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    label = { Text(AppStrings.navAddOrder(language), fontSize = 11.sp) },
                    enabled = true
                )

                NavigationBarItem(
                    selected = currentScreen is Screen.Reports,
                    onClick = { onNavigate(Screen.Reports) },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Reports") },
                    label = {
                        Text(
                            AppStrings.navReports(language),
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen is Screen.Reports) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = currentScreen is Screen.Settings,
                    onClick = { onNavigate(Screen.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = {
                        Text(
                            AppStrings.navSettings(language),
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen is Screen.Settings) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors
                )
            }
        }

        // Center Floating Action Button (+ Add Order)
        FloatingActionButton(
            onClick = onAddOrderClick,
            shape = CircleShape,
            containerColor = if (isDark) tokens.buttonPrimaryBg else MaterialTheme.colorScheme.secondary,
            contentColor = if (isDark) tokens.buttonPrimaryText else Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(54.dp)
                .testTag("center_add_order_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Order",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Lightweight branded initializing screen.
 * Prevents Dashboard or customer order data from flashing before local auth state resolves.
 */
@Composable
private fun InitializingScreen() {
    val tokens = MaterialTheme.auraTokens
    val bg = if (tokens.isDark) Color(0xFF101010) else Color(0xFF4A0E18)
    val accent = if (tokens.isDark) Color(0xFFFFFFFF) else Color(0xFFFFD9DF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = if (tokens.isDark) Color(0xFF202020) else Color(0xFF3B000F),
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "FA",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fashion Aura",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ORDER MANAGEMENT SYSTEM",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent.copy(alpha = 0.85f),
                letterSpacing = 2.5.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = accent,
                strokeWidth = 2.5.dp
            )
        }
    }
}

/**
 * Non-OMS error surface displayed if authentication observation fails.
 * Never falls back to Dashboard or customer data.
 */
@Composable
private fun AuthGateErrorScreen(
    message: String,
    language: AppLanguage,
    onRetry: () -> Unit
) {
    val tokens = MaterialTheme.auraTokens
    val isBn = language == AppLanguage.BANGLA

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = tokens.errorForeground,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isBn) "অথেনটিকেশন ত্রুটি" else "Authentication Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.buttonPrimaryBg,
                    contentColor = tokens.buttonPrimaryText
                )
            ) {
                Text(if (isBn) "পুনরায় চেষ্টা করুন" else "Retry")
            }
        }
    }
}

/**
 * Dedicated Access Denied surface for authenticated users whose custom claims do not match Owner permissions.
 * Prevents unauthorized users from composing or accessing any OMS screens or local customer data.
 */
@Composable
private fun AccessDeniedScreen(
    email: String,
    language: AppLanguage,
    onSwitchAccount: () -> Unit
) {
    val tokens = MaterialTheme.auraTokens

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .padding(24.dp)
            .testTag("access_denied_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = tokens.surfaceSecondary,
                border = BorderStroke(1.5.dp, tokens.errorForeground.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Denied",
                        tint = tokens.errorForeground,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = AppStrings.accessDeniedTitle(language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = AppStrings.accessDeniedDesc(language, email),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onSwitchAccount,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.buttonPrimaryBg,
                    contentColor = tokens.buttonPrimaryText
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
                    .testTag("switch_account_button")
            ) {
                Text(
                    text = AppStrings.switchAccount(language),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


