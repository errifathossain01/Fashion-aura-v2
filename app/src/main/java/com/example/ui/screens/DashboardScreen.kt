package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogProductEntity
import com.example.data.model.CourierSyncStatus
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderType
import com.example.data.model.PaymentStatus
import com.example.data.model.UserPreferences
import com.example.ui.components.BoutiqueOrderCard
import com.example.ui.components.BulkCatalogAssignDialog
import com.example.ui.components.BulkPickListDialog
import com.example.ui.components.BulkShippingLabelsDialog
import com.example.ui.components.ConsignmentIdDialog
import com.example.ui.components.FashionAuraLogo
import com.example.ui.components.MoreBulkActionsDialog
import com.example.ui.components.NotificationRemindersDialog
import com.example.ui.theme.AppStrings
import com.example.ui.theme.auraTokens
import com.example.ui.viewmodel.BulkCourierSyncState
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.OrderFilterTab
import com.example.util.BanglaFormatter
import java.util.Calendar

@Composable
fun DashboardScreen(
    orders: List<OrderEntity>,
    allOrders: List<OrderEntity> = orders,
    catalogProducts: List<CatalogProductEntity> = emptyList(),
    metrics: DashboardMetrics,
    selectedTab: OrderFilterTab,
    searchQuery: String,
    selectedProductFilter: String? = null,
    selectedSizeFilter: String? = null,
    selectedColorFilter: String? = null,
    isPendingCourierFilterActive: Boolean = false,
    userPreferences: UserPreferences,
    onTabSelected: (OrderFilterTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onProductFilterChange: (String?) -> Unit = {},
    onSizeFilterChange: (String?) -> Unit = {},
    onColorFilterChange: (String?) -> Unit = {},
    onTogglePendingCourierFilter: () -> Unit = {},
    onClearFilters: () -> Unit = {},
    onMarkAsShipped: (OrderEntity) -> Unit,
    onStockArrived: (OrderEntity) -> Unit,
    onBookCourier: (OrderEntity) -> Unit,
    onCardClick: (OrderEntity) -> Unit,
    onEditOrder: (OrderEntity) -> Unit,
    onDeleteOrder: (OrderEntity) -> Unit,
    onBulkStatusChange: ((Set<Long>, FulfillmentStatus) -> Unit)? = null,
    onBulkAssignDress: ((orderIds: Set<Long>, product: CatalogProductEntity, keepExistingSizes: Boolean, overrideSize: String?, overrideColor: String?) -> Unit)? = null,
    onBulkRetrySync: ((List<OrderEntity>) -> Unit)? = null,
    onBulkDelete: ((Set<Long>) -> Unit)? = null,
    onBulkConvertToRegular: ((Set<Long>) -> Unit)? = null,
    onBulkUpdateDeliveryDate: ((Set<Long>, String) -> Unit)? = null,
    onBulkUpdateStatusWithPayment: ((Set<Long>, FulfillmentStatus, PaymentStatus?) -> Unit)? = null,
    bulkCourierSyncState: BulkCourierSyncState = BulkCourierSyncState(),
    onStartBulkCourierSync: ((List<OrderEntity>) -> Unit)? = null,
    onStopBulkCourierSync: (() -> Unit)? = null,
    onDismissBulkCourierSync: (() -> Unit)? = null,
    onNavigateToAddOrder: () -> Unit,
    onNavigateToSmartPaste: () -> Unit,
    onNavigateToOrders: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onToggleDarkMode: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onDigitChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = userPreferences.appLanguage
    val useBanglaDigits = userPreferences.useBanglaDigits
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    // Multi-Select Action Mode State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedOrderIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog States
    var showPickListDialog by remember { mutableStateOf(false) }
    var showAssignDressDialog by remember { mutableStateOf(false) }
    var showBulkPrintDialog by remember { mutableStateOf(false) }
    var showMoreActionsDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Local fallback filters in case callbacks are used as state
    var localProductFilter by remember { mutableStateOf<String?>(null) }
    var localSizeFilter by remember { mutableStateOf<String?>(null) }
    var localColorFilter by remember { mutableStateOf<String?>(null) }
    var localPendingCourier by remember { mutableStateOf(false) }
    var isFilterExpanded by remember { mutableStateOf(false) }

    val activeProduct = selectedProductFilter ?: localProductFilter
    val activeSize = selectedSizeFilter ?: localSizeFilter
    val activeColor = selectedColorFilter ?: localColorFilter
    val activePendingCourier = isPendingCourierFilterActive || localPendingCourier

    fun handleProductSelect(prod: String?) {
        localProductFilter = prod
        onProductFilterChange(prod)
    }

    fun handleSizeSelect(sz: String?) {
        localSizeFilter = sz
        onSizeFilterChange(sz)
    }

    fun handleColorSelect(col: String?) {
        localColorFilter = col
        onColorFilterChange(col)
    }

    fun handleTogglePendingCourier() {
        localPendingCourier = !localPendingCourier
        onTogglePendingCourierFilter()
    }

    fun handleClearFilters() {
        localProductFilter = null
        localSizeFilter = null
        localColorFilter = null
        localPendingCourier = false
        onClearFilters()
    }

    // Available dresses for the dress chip row
    val availableDresses = remember(catalogProducts, allOrders) {
        val map = mutableMapOf<String, DressChipData>()
        catalogProducts.forEach { cat ->
            val count = allOrders.count { order ->
                order.items.any { it.name.trim().equals(cat.name.trim(), ignoreCase = true) }
            }
            map[cat.name.trim().lowercase()] = DressChipData(
                name = cat.name.trim(),
                photoUri = cat.photoUri,
                orderCount = count
            )
        }
        allOrders.flatMap { it.items }.forEach { item ->
            val key = item.name.trim().lowercase()
            if (key.isNotBlank() && !map.containsKey(key)) {
                val count = allOrders.count { order ->
                    order.items.any { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
                }
                map[key] = DressChipData(
                    name = item.name.trim(),
                    photoUri = item.photoUri,
                    orderCount = count
                )
            }
        }
        map.values.toList().sortedByDescending { it.orderCount }
    }

    // Available colors
    val availableColors = remember(allOrders, catalogProducts) {
        val colors = mutableSetOf<String>()
        catalogProducts.forEach { colors.addAll(it.availableColors) }
        allOrders.flatMap { it.items }.forEach { item ->
            if (item.color.isNotBlank()) colors.add(item.color.trim())
        }
        colors.filter { it.isNotBlank() }.toList()
    }

    // Pending courier count
    val pendingCourierCount = remember(allOrders) {
        allOrders.count {
            it.courierTrackingId.isBlank() || it.courierTrackingId.equals("PENDING", ignoreCase = true)
        }
    }

    // Filtered orders displayed on screen
    val displayedOrders = remember(orders, activeProduct, activeSize, activeColor, activePendingCourier) {
        orders.filter { order ->
            val matchesProduct = if (activeProduct.isNullOrBlank() || activeProduct == "সব ড্রেস") {
                true
            } else {
                order.items.any { it.name.trim().equals(activeProduct.trim(), ignoreCase = true) }
            }

            val matchesSize = if (activeSize.isNullOrBlank() || activeSize == "সব সাইজ") {
                true
            } else {
                order.items.any { it.size.trim().equals(activeSize.trim(), ignoreCase = true) }
            }

            val matchesColor = if (activeColor.isNullOrBlank()) {
                true
            } else {
                order.items.any { it.color.trim().equals(activeColor.trim(), ignoreCase = true) }
            }

            val matchesCourier = if (!activePendingCourier) {
                true
            } else {
                order.courierTrackingId.isBlank() || order.courierTrackingId.equals("PENDING", ignoreCase = true)
            }

            matchesProduct && matchesSize && matchesColor && matchesCourier
        }
    }

    val selectedOrders = remember(selectedOrderIds, allOrders) {
        allOrders.filter { selectedOrderIds.contains(it.id) }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedOrderIds = emptySet()
    }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = AppStrings.greeting(language, currentHour)
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isSelectionMode && selectedOrderIds.isNotEmpty()) 150.dp else 90.dp)
        ) {
        // 1. BRANDED TOP BAR
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo + Brand Title + Greeting Subtitle
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FashionAuraLogo(size = 42.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Fashion ",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Aura",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Light,
                                    fontStyle = FontStyle.Italic,
                                    color = if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.secondary,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            Text(
                                text = "$greetingText • ${AppStrings.orderManagement(language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Actions: Day/Night, Lang Dropdown, Bell
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Day/Night switch
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (userPreferences.isDarkMode == true) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Language Switcher Dropdown (dismiss state first)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tokens.surfaceSecondary,
                                border = BorderStroke(1.dp, tokens.borderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showLanguageMenu = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "বাং" else "EN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.textPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("বাংলা (Bangla)") },
                                    onClick = {
                                        showLanguageMenu = false
                                        onLanguageChange(AppLanguage.BANGLA)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("English (ইংরেজি)") },
                                    onClick = {
                                        showLanguageMenu = false
                                        onLanguageChange(AppLanguage.ENGLISH)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { showNotificationDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = tokens.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = AppStrings.searchPlaceholder(language),
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF9E9E9E) else Color(0xFF6B7280)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Color(0xFF9E9E9E) else Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) tokens.borderActive else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = tokens.borderMain,
                        focusedContainerColor = tokens.inputSurface,
                        unfocusedContainerColor = tokens.inputSurface,
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        cursorColor = tokens.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }

        // 2. QUICK ACCESS SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Section Header with "See All"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "দ্রুত অ্যাক্সেস" else "Quick Access",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigateToOrders() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "সব অর্ডার দেখুন" else "See All Orders",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Hero Card: Clean Smart Paste
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToSmartPaste() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.secondary
                    ),
                    border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) tokens.surfaceSecondary else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = if (isDark) tokens.textPrimary else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = AppStrings.smartPaste(language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) tokens.textPrimary else Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "মেসেজ পেস্ট করে ১-ক্লিকে অর্ডার" else "Paste message to create order",
                                    fontSize = 11.sp,
                                    color = if (isDark) tokens.textMuted else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) tokens.buttonPrimaryBg else Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "পেস্ট করুন" else "Paste",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) tokens.buttonPrimaryText else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. 2x2 METRICS SUMMARY CARDS
        item {
            // Defensive KPI calculation ensuring accurate data display even during initial flow emission
            val effectiveTotalOrders = if (metrics.totalOrdersCount > 0) metrics.totalOrdersCount else allOrders.size
            val effectiveTodaySales = if (metrics.todaySales > 0.0) {
                metrics.todaySales
            } else {
                val now = System.currentTimeMillis()
                val dayStart = now - (now % 86400000L)
                allOrders.filter { it.createdAt >= dayStart }.sumOf { it.totalBill }
            }
            val effectivePendingDelivery = if (metrics.pendingDeliveryCount > 0) {
                metrics.pendingDeliveryCount
            } else {
                allOrders.count {
                    it.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP ||
                    it.fulfillmentStatus == FulfillmentStatus.PROCESSING ||
                    it.fulfillmentStatus == FulfillmentStatus.SHIPPED
                }
            }
            val effectivePendingCod = if (metrics.pendingCodAmount > 0.0) {
                metrics.pendingCodAmount
            } else {
                allOrders.filter {
                    it.fulfillmentStatus != FulfillmentStatus.DELIVERED &&
                    it.fulfillmentStatus != FulfillmentStatus.CANCELLED &&
                    it.fulfillmentStatus != FulfillmentStatus.RETURNED
                }.sumOf { it.codAmount }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (language == AppLanguage.BANGLA) "মোট অর্ডার" else "Total Orders",
                        value = BanglaFormatter.formatNumber(effectiveTotalOrders, useBanglaDigits),
                        icon = Icons.Default.Inventory2,
                        iconTint = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                        iconBgColor = if (isDark) tokens.surfaceSecondary else null,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (language == AppLanguage.BANGLA) "আজকের বিক্রি" else "Today's Sales",
                        value = BanglaFormatter.formatCurrency(effectiveTodaySales, useBanglaDigits),
                        icon = Icons.Default.TrendingUp,
                        iconTint = if (isDark) tokens.successForeground else Color(0xFF15803D),
                        iconBgColor = if (isDark) tokens.successContainer else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (language == AppLanguage.BANGLA) "পেন্ডিং ডেলিভারি" else "Pending Delivery",
                        value = BanglaFormatter.formatNumber(effectivePendingDelivery, useBanglaDigits),
                        icon = Icons.Default.LocalShipping,
                        iconTint = if (isDark) tokens.infoForeground else Color(0xFF1D4ED8),
                        iconBgColor = if (isDark) tokens.infoContainer else null,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (language == AppLanguage.BANGLA) "পেন্ডিং COD" else "Pending COD",
                        value = BanglaFormatter.formatCurrency(effectivePendingCod, useBanglaDigits),
                        icon = Icons.Default.Payments,
                        iconTint = if (isDark) tokens.warningForeground else Color(0xFFC2410C),
                        iconBgColor = if (isDark) tokens.warningContainer else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. STATUS FILTER CHIPS ROW (Positioned directly above Recent Orders header)
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(OrderFilterTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    val tabCount = when (tab) {
                        OrderFilterTab.ALL -> allOrders.size
                        OrderFilterTab.PRE_ORDER -> allOrders.count {
                            it.orderType == OrderType.PRE_ORDER ||
                            it.orderType.name.equals("PRE_ORDER", ignoreCase = true) ||
                            it.fulfillmentStatus == com.example.data.model.FulfillmentStatus.PRE_ORDER_PENDING ||
                            it.fulfillmentStatus.name.equals("PRE_ORDER_PENDING", ignoreCase = true)
                        }
                        OrderFilterTab.REGULAR -> allOrders.count {
                            it.orderType == OrderType.REGULAR ||
                            it.orderType.name.equals("REGULAR", ignoreCase = true)
                        }
                        OrderFilterTab.NEW -> allOrders.count {
                            it.fulfillmentStatus == com.example.data.model.FulfillmentStatus.NEW ||
                            it.fulfillmentStatus.name.equals("NEW", ignoreCase = true)
                        }
                        OrderFilterTab.READY_TO_SHIP -> allOrders.count {
                            it.fulfillmentStatus == com.example.data.model.FulfillmentStatus.READY_TO_SHIP ||
                            it.fulfillmentStatus.name.equals("READY_TO_SHIP", ignoreCase = true)
                        }
                        OrderFilterTab.SHIPPED -> allOrders.count {
                            it.fulfillmentStatus == com.example.data.model.FulfillmentStatus.SHIPPED ||
                            it.fulfillmentStatus.name.equals("SHIPPED", ignoreCase = true)
                        }
                        OrderFilterTab.URGENT -> allOrders.count {
                            it.priority == com.example.data.model.Priority.URGENT ||
                            it.priority.name.equals("URGENT", ignoreCase = true)
                        }
                        OrderFilterTab.SCHEDULED -> allOrders.count {
                            it.isScheduled ||
                            it.orderType == OrderType.SCHEDULED ||
                            it.orderType.name.equals("SCHEDULED", ignoreCase = true) ||
                            it.requestedDeliveryDate.isNotBlank()
                        }
                        OrderFilterTab.PENDING_SYNC -> allOrders.count {
                            it.courierSyncStatus == com.example.data.model.CourierSyncStatus.PENDING_SYNC ||
                            it.courierSyncStatus.name.equals("PENDING_SYNC", ignoreCase = true)
                        }
                    }

                    val countStr = BanglaFormatter.formatNumber(tabCount, useBanglaDigits)
                    val label = if (language == AppLanguage.BANGLA) tab.titleBn else tab.titleEn

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) {
                            if (isSelected) tokens.buttonPrimaryBg else tokens.surface
                        } else {
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        },
                        border = if (isSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onTabSelected(tab) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDark) {
                                    if (isSelected) tokens.buttonPrimaryText else tokens.textSecondary
                                } else {
                                    if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) {
                                    if (isSelected) tokens.buttonPrimaryText else tokens.surfaceSecondary
                                } else {
                                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = countStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) {
                                        if (isSelected) tokens.buttonPrimaryBg else tokens.textMuted
                                    } else {
                                        if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. ORDERS LIST HEADER OR SELECTION CONTROL BAR
        item {
            if (isSelectionMode) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) tokens.surfaceElevated else Color(0xFF1E1B4B),
                    border = BorderStroke(1.2.dp, if (isDark) tokens.borderMain else Color(0xFF6366F1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${BanglaFormatter.formatNumber(selectedOrderIds.size, useBanglaDigits)} ${if (language == AppLanguage.BANGLA) "টি অর্ডার নির্বাচিত" else "orders selected"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) tokens.textPrimary else Color.White
                            )
                            val selectedCodSum = selectedOrders.sumOf { it.codAmount }
                            Text(
                                text = "${if (language == AppLanguage.BANGLA) "মোট COD: " else "Total COD: "}${BanglaFormatter.formatCurrency(selectedCodSum, useBanglaDigits)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) tokens.infoForeground else Color(0xFF818CF8)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val allSelected = displayedOrders.isNotEmpty() && selectedOrderIds.size >= displayedOrders.size
                            TextButton(
                                onClick = {
                                    selectedOrderIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        displayedOrders.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (allSelected) (if (language == AppLanguage.BANGLA) "সব আনমার্ক" else "Deselect All") else "${if (language == AppLanguage.BANGLA) "সব মার্ক" else "Select All"} (${BanglaFormatter.formatNumber(displayedOrders.size, useBanglaDigits)})",
                                    color = if (isDark) tokens.infoForeground else Color(0xFFA5B4FC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedOrderIds = emptySet()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = if (isDark) tokens.textPrimary else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = AppStrings.recentOrders(language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.ordersCount(language, BanglaFormatter.formatNumber(displayedOrders.size, useBanglaDigits)),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textMuted
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Compact "Filter" Button
                        val hasActiveFilters = !activeProduct.isNullOrBlank() || !activeSize.isNullOrBlank() || !activeColor.isNullOrBlank() || activePendingCourier
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasActiveFilters || isFilterExpanded) {
                                if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                if (isDark) tokens.surface else MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.2.dp,
                                if (hasActiveFilters || isFilterExpanded) {
                                    if (isDark) tokens.borderActive else MaterialTheme.colorScheme.secondary
                                } else {
                                    if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isFilterExpanded = !isFilterExpanded }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (hasActiveFilters || isFilterExpanded) {
                                        if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        tokens.textSecondary
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = AppStrings.filter(language),
                                    fontSize = 12.sp,
                                    fontWeight = if (hasActiveFilters || isFilterExpanded) FontWeight.Bold else FontWeight.Medium,
                                    color = if (hasActiveFilters || isFilterExpanded) {
                                        if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        tokens.textSecondary
                                    }
                                )
                                if (hasActiveFilters) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDark) tokens.buttonPrimaryBg else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(7.dp)
                                    ) {}
                                }
                            }
                        }

                        // Single Prominent "Select" Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) tokens.surfaceSecondary else Color(0xFF9D174D).copy(alpha = 0.12f),
                            border = BorderStroke(1.2.dp, if (isDark) tokens.borderMain else Color(0xFF9D174D)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isSelectionMode = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDark) tokens.textPrimary else Color(0xFF9D174D),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = AppStrings.select(language),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) tokens.textPrimary else Color(0xFF9D174D)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. EXPANDABLE PRODUCT, SIZE & COURIER FILTER PANEL
        if (isFilterExpanded) {
            item {
                val hasActiveFilters = !activeProduct.isNullOrBlank() || !activeSize.isNullOrBlank() || !activeColor.isNullOrBlank() || activePendingCourier
                Surface(
                    color = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Filter Bar Header: Active Badge, Clear Filters
                        if (hasActiveFilters) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = AppStrings.active(language),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = AppStrings.clearAll(language),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.errorForeground,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { handleClearFilters() }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Catalog dress selector
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                val isAllSelected = activeProduct.isNullOrBlank() || activeProduct == "সব ড্রেস" || activeProduct == "All Dresses"
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isAllSelected) (if (isDark) tokens.buttonPrimaryBg else MaterialTheme.colorScheme.primary) else (if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant),
                                    border = if (isAllSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderSubtle else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { handleProductSelect(null) }
                                ) {
                                    Text(
                                        text = "${AppStrings.allDresses(language)} (${BanglaFormatter.formatNumber(allOrders.size, useBanglaDigits)})",
                                        fontSize = 11.sp,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) (if (isDark) tokens.buttonPrimaryText else Color.White) else (if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            items(availableDresses, key = { it.name }) { dress ->
                                val isSelected = activeProduct.equals(dress.name, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) (if (isDark) tokens.buttonPrimaryBg else MaterialTheme.colorScheme.primary) else (if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant),
                                    border = if (isSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderSubtle else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (isSelected) handleProductSelect(null) else handleProductSelect(dress.name)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        if (!dress.photoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = dress.photoUri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = if (isSelected) (if (isDark) tokens.buttonPrimaryText else Color.White) else (if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "${dress.name} (${BanglaFormatter.formatNumber(dress.orderCount, useBanglaDigits)})",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) (if (isDark) tokens.buttonPrimaryText else Color.White) else (if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Courier & Size sub-filter chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            // Courier status chip
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) {
                                        if (activePendingCourier) tokens.warningContainer else tokens.surfaceSecondary
                                    } else {
                                        if (activePendingCourier) Color(0xFFF59E0B) else Color(0xFFFEF3C7)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) {
                                            if (activePendingCourier) tokens.warningBorder else tokens.borderSubtle
                                        } else {
                                            if (activePendingCourier) Color(0xFFB45309) else Color(0xFFF59E0B)
                                        }
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { handleTogglePendingCourier() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = if (isDark) tokens.warningForeground else (if (activePendingCourier) Color.White else Color(0xFF92400E)),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${AppStrings.pendingCourier(language)} (${BanglaFormatter.formatNumber(pendingCourierCount, useBanglaDigits)})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) tokens.warningForeground else (if (activePendingCourier) Color.White else Color(0xFF92400E))
                                        )
                                        if (activePendingCourier) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Check, null, tint = if (isDark) tokens.warningForeground else Color.White, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }

                            // Size sub-filter chips
                            val sizeChips = listOf(AppStrings.allSizes(language), "36", "38", "40", "42", "44", "46", "Free")
                            items(sizeChips) { sz ->
                                val isAllSz = sz == AppStrings.allSizes(language)
                                val isSzSelected = if (isAllSz) activeSize.isNullOrBlank() || activeSize == "সব সাইজ" || activeSize == "All Sizes" else activeSize.equals(sz, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSzSelected) (if (isDark) tokens.buttonPrimaryBg else MaterialTheme.colorScheme.secondary) else (if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                    border = if (isSzSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderSubtle else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (isAllSz) {
                                                handleSizeSelect(null)
                                            } else {
                                                if (isSzSelected) handleSizeSelect(null) else handleSizeSelect(sz)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (isAllSz) sz else "${AppStrings.size(language)} ${if (useBanglaDigits && sz.toIntOrNull() != null) BanglaFormatter.toBanglaDigits(sz) else sz}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSzSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSzSelected) (if (isDark) tokens.buttonPrimaryText else Color.White) else (if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Color sub-filters
                            if (availableColors.isNotEmpty()) {
                                items(availableColors) { col ->
                                    val isColSelected = activeColor.equals(col, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isColSelected) (if (isDark) tokens.buttonPrimaryBg else Color(0xFF6B21A8)) else (if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                        border = if (isColSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderSubtle else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                if (isColSelected) handleColorSelect(null) else handleColorSelect(col)
                                            }
                                    ) {
                                        Text(
                                            text = "${if (language == AppLanguage.BANGLA) "কালার:" else "Color:"} $col",
                                            fontSize = 11.sp,
                                            fontWeight = if (isColSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isColSelected) (if (isDark) tokens.buttonPrimaryText else Color.White) else (if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. ORDER CARDS (with Checkbox & Selection State)
        if (displayedOrders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "কোনো অর্ডার পাওয়া যায়নি" else "No orders found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!activeProduct.isNullOrBlank() || !activeSize.isNullOrBlank() || activePendingCourier) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { handleClearFilters() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ফিল্টার রিসেট করুন")
                            }
                        }
                    }
                }
            }
        } else {
            items(displayedOrders, key = { it.id }) { order ->
                val isSelected = selectedOrderIds.contains(order.id)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .animateItem()
                ) {
                    BoutiqueOrderCard(
                        order = order,
                        language = language,
                        useBanglaDigits = useBanglaDigits,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onToggleSelect = {
                            selectedOrderIds = if (isSelected) {
                                selectedOrderIds - order.id
                            } else {
                                selectedOrderIds + order.id
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedOrderIds = setOf(order.id)
                            }
                        },
                        onCardClick = {
                            if (isSelectionMode) {
                                selectedOrderIds = if (isSelected) {
                                    selectedOrderIds - order.id
                                } else {
                                    selectedOrderIds + order.id
                                }
                            } else {
                                onCardClick(order)
                            }
                        },
                        onMarkAsShipped = { onMarkAsShipped(order) },
                        onStockArrived = { onStockArrived(order) },
                        onBookCourier = { onBookCourier(order) },
                        onEditOrder = { onEditOrder(order) },
                        onDeleteOrder = { onDeleteOrder(order) }
                    )
                }
            }
        }
    }

    // =========================================================================
    // FLOATING BULK ACTION DOCK (Visible when items are selected)
    // =========================================================================
    AnimatedVisibility(
        visible = isSelectionMode && selectedOrderIds.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) tokens.surfaceElevated else Color(0xFF1E1B4B),
            border = BorderStroke(1.5.dp, if (isDark) tokens.borderMain else Color(0xFF818CF8)),
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 🚀 স্টেডফাস্ট এন্ট্রি
                // 1. Steadfast Entry
                DockActionButton(
                    icon = Icons.Default.LocalShipping,
                    label = if (language == AppLanguage.BANGLA) "স্টেডফাস্ট এন্ট্রি" else "Steadfast Entry",
                    tint = Color(0xFF38BDF8),
                    onClick = {
                        onStartBulkCourierSync?.invoke(selectedOrders)
                    }
                )

                // 2. Pick-List
                DockActionButton(
                    icon = Icons.Default.Inventory2,
                    label = if (language == AppLanguage.BANGLA) "পিক-লিস্ট" else "Pick-List",
                    tint = Color(0xFFFBBF24),
                    onClick = {
                        showPickListDialog = true
                    }
                )

                // 3. Assign Dress
                DockActionButton(
                    icon = Icons.Default.CheckCircle,
                    label = if (language == AppLanguage.BANGLA) "ড্রেস বসান" else "Assign Dress",
                    tint = Color(0xFFF472B6),
                    onClick = {
                        showAssignDressDialog = true
                    }
                )

                // 4. Print / Sheet
                DockActionButton(
                    icon = Icons.Default.Print,
                    label = if (language == AppLanguage.BANGLA) "প্রিন্ট / শিট" else "Print / Slip",
                    tint = Color(0xFF34D399),
                    onClick = {
                        showBulkPrintDialog = true
                    }
                )

                // 5. More
                DockActionButton(
                    icon = Icons.Default.MoreVert,
                    label = if (language == AppLanguage.BANGLA) "আরও" else "More",
                    tint = Color.White,
                    onClick = {
                        showMoreActionsDialog = true
                    }
                )
            }
        }
    }
}

    // =========================================================================
    // DIALOGS: PICK-LIST, ASSIGN DRESS, BULK LABELS, MORE ACTIONS, DELETE CONFIRM, COURIER SYNC
    // =========================================================================

    // 1. Pick-List Packaging Summary Dialog (Godown Pick-List)
    if (showPickListDialog && selectedOrders.isNotEmpty()) {
        BulkPickListDialog(
            selectedOrders = selectedOrders,
            useBanglaDigits = useBanglaDigits,
            onDismiss = { showPickListDialog = false }
        )
    }

    // 2. Assign Dress Dialog (Smart Non-Destructive Merge)
    if (showAssignDressDialog && selectedOrders.isNotEmpty()) {
        BulkCatalogAssignDialog(
            selectedOrdersCount = selectedOrders.size,
            catalogProducts = catalogProducts,
            useBanglaDigits = useBanglaDigits,
            onAssign = { product, keepExistingSizes, overrideSize, overrideColor ->
                onBulkAssignDress?.invoke(selectedOrderIds, product, keepExistingSizes, overrideSize, overrideColor)
                isSelectionMode = false
                selectedOrderIds = emptySet()
            },
            onDismiss = { showAssignDressDialog = false }
        )
    }

    // 3. Bulk Shipping Labels Preview & Rider Manifest Dialog
    if (showBulkPrintDialog && selectedOrders.isNotEmpty()) {
        BulkShippingLabelsDialog(
            selectedOrders = selectedOrders,
            language = language,
            useBanglaDigits = useBanglaDigits,
            onMarkAsShipped = { shippedList ->
                val ids = shippedList.map { it.id }.toSet()
                if (onBulkStatusChange != null) {
                    onBulkStatusChange(ids, FulfillmentStatus.SHIPPED)
                }
                isSelectionMode = false
                selectedOrderIds = emptySet()
            },
            onDismiss = { showBulkPrintDialog = false }
        )
    }

    // 4. More Bulk Actions Dialog (Milestone 4)
    if (showMoreActionsDialog && selectedOrders.isNotEmpty()) {
        MoreBulkActionsDialog(
            selectedOrders = selectedOrders,
            useBanglaDigits = useBanglaDigits,
            onConvertToRegular = { ids ->
                onBulkConvertToRegular?.invoke(ids)
                isSelectionMode = false
                selectedOrderIds = emptySet()
            },
            onUpdateDeliveryDate = { ids, dateStr ->
                onBulkUpdateDeliveryDate?.invoke(ids, dateStr)
                isSelectionMode = false
                selectedOrderIds = emptySet()
            },
            onUpdateStatus = { ids, status, payment ->
                if (onBulkUpdateStatusWithPayment != null) {
                    onBulkUpdateStatusWithPayment(ids, status, payment)
                } else {
                    onBulkStatusChange?.invoke(ids, status)
                }
                isSelectionMode = false
                selectedOrderIds = emptySet()
            },
            onMoveToTrash = { ids ->
                onBulkDelete?.invoke(ids)
                isSelectionMode = false
                selectedOrderIds = emptySet()
            },
            onDismiss = { showMoreActionsDialog = false }
        )
    }

    // 5. Bulk Delete Confirmation Dialog
    if (showBulkDeleteConfirmDialog && selectedOrderIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.BANGLA) "অর্ডারগুলো ট্র্যাশে পাঠাবেন?" else "Move Orders to Trash?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.BANGLA)
                        "${BanglaFormatter.formatNumber(selectedOrderIds.size, useBanglaDigits)} টি নির্বাচিত অর্ডার ট্র্যাশে পাঠানো হবে। পরে সেটিংস বা ট্র্যাশ স্ক্রিন থেকে পুনরুদ্ধার করা যাবে।"
                    else
                        "${selectedOrderIds.size} selected orders will be moved to trash. You can restore them later."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteConfirmDialog = false
                        onBulkDelete?.invoke(selectedOrderIds)
                        isSelectionMode = false
                        selectedOrderIds = emptySet()
                    }
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ট্র্যাশে পাঠান" else "Move to Trash",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmDialog = false }) {
                    Text(text = if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // 6. Bulk Courier Sync Progress & Report Dialog
    if (bulkCourierSyncState.isActive) {
        BulkCourierSyncProgressDialog(
            syncState = bulkCourierSyncState,
            useBanglaDigits = useBanglaDigits,
            onStop = { onStopBulkCourierSync?.invoke() },
            onDismiss = { onDismissBulkCourierSync?.invoke() }
        )
    }

    // 7. Notification Reminders & Alerts Dialog
    if (showNotificationDialog) {
        NotificationRemindersDialog(
            orders = allOrders,
            language = language,
            useBanglaDigits = useBanglaDigits,
            onOrderClick = { order -> onCardClick(order) },
            onDismiss = { showNotificationDialog = false }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    iconBgColor: Color? = null
) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isDark) tokens.borderMain else Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bg = iconBgColor ?: if (isDark) tokens.surfaceSecondary else iconTint.copy(alpha = 0.12f)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.textPrimary
                )
            }
        }
    }
}

@Composable
private fun DockActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
