package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogProductEntity
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderType
import com.example.data.model.PaymentStatus
import com.example.ui.components.BoutiqueOrderCard
import com.example.ui.components.BulkCatalogAssignDialog
import com.example.ui.components.BulkPickListDialog
import com.example.ui.components.BulkShippingLabelsDialog
import com.example.ui.components.MoreBulkActionsDialog
import com.example.ui.theme.AppStrings
import com.example.ui.theme.auraTokens
import com.example.ui.viewmodel.BulkCourierSyncState
import com.example.ui.viewmodel.BulkSyncItemStatus
import com.example.ui.viewmodel.OrderFilterTab
import com.example.util.BanglaFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DressChipData(
    val name: String,
    val photoUri: String?,
    val orderCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orders: List<OrderEntity>,
    allOrders: List<OrderEntity> = orders,
    allOrdersCount: Int = allOrders.size,
    catalogProducts: List<CatalogProductEntity> = emptyList(),
    selectedTab: OrderFilterTab,
    searchQuery: String,
    selectedProductFilter: String? = null,
    selectedSizeFilter: String? = null,
    selectedColorFilter: String? = null,
    isPendingCourierFilterActive: Boolean = false,
    language: AppLanguage,
    useBanglaDigits: Boolean,
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
    modifier: Modifier = Modifier
) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark

    var groupByDress by remember { mutableStateOf(false) }

    // Multi-Select Action Mode State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedOrderIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog States
    var showBulkPrintDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoreActionsDialog by remember { mutableStateOf(false) }
    var showPickListDialog by remember { mutableStateOf(false) }
    var showAssignDressDialog by remember { mutableStateOf(false) }

    // Local fallback filters in case callbacks are used as state
    var isFilterExpanded by remember { mutableStateOf(false) }
    var localProductFilter by remember { mutableStateOf<String?>(null) }
    var localSizeFilter by remember { mutableStateOf<String?>(null) }
    var localColorFilter by remember { mutableStateOf<String?>(null) }
    var localPendingCourier by remember { mutableStateOf(false) }

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

    // Intercept back press when selection mode is active
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedOrderIds = emptySet()
    }

    // Dynamic filtering for displayed orders
    val displayedOrders = remember(
        orders,
        allOrders,
        selectedTab,
        groupByDress,
        activeProduct,
        activeSize,
        activeColor,
        activePendingCourier
    ) {
        var list = orders

        if (!activeProduct.isNullOrBlank() && activeProduct != "সব ড্রেস") {
            list = list.filter { order ->
                order.items.any { item -> item.name.equals(activeProduct, ignoreCase = true) }
            }
        }

        if (!activeSize.isNullOrBlank() && activeSize != "সব সাইজ") {
            list = list.filter { order ->
                order.items.any { item -> item.size.equals(activeSize, ignoreCase = true) }
            }
        }

        if (!activeColor.isNullOrBlank() && activeColor != "সব কালার") {
            list = list.filter { order ->
                order.items.any { item -> item.color.equals(activeColor, ignoreCase = true) }
            }
        }

        if (activePendingCourier) {
            list = list.filter { order ->
                order.courierTrackingId.isBlank() || order.courierTrackingId.equals("PENDING", ignoreCase = true)
            }
        }

        if (selectedTab == OrderFilterTab.READY_TO_SHIP && groupByDress) {
            list.sortedWith(
                compareBy<OrderEntity> { it.items.firstOrNull()?.name ?: "" }
                    .thenBy { it.items.firstOrNull()?.size ?: "" }
                    .thenBy { it.items.firstOrNull()?.color ?: "" }
            )
        } else {
            list
        }
    }

    val selectedOrders = remember(selectedOrderIds, allOrders) {
        allOrders.filter { selectedOrderIds.contains(it.id) }
    }

    val totalSelectedCod = remember(selectedOrders) {
        selectedOrders.sumOf { it.codAmount }
    }

    // Distinct list of dresses from catalog and all orders
    val availableDresses = remember(catalogProducts, allOrders) {
        val dressMap = linkedMapOf<String, DressChipData>()

        catalogProducts.forEach { cat ->
            val count = allOrders.count { order ->
                order.items.any { it.name.equals(cat.name, ignoreCase = true) }
            }
            dressMap[cat.name.lowercase().trim()] = DressChipData(cat.name, cat.photoUri.ifBlank { null }, count)
        }

        allOrders.flatMap { it.items }.forEach { item ->
            val key = item.name.lowercase().trim()
            if (key.isNotBlank() && !dressMap.containsKey(key)) {
                val count = allOrders.count { order ->
                    order.items.any { it.name.equals(item.name, ignoreCase = true) }
                }
                dressMap[key] = DressChipData(item.name, item.photoUri, count)
            }
        }

        dressMap.values.sortedByDescending { it.orderCount }
    }

    // Distinct colors from orders
    val availableColors = remember(allOrders, activeProduct) {
        val relevant = if (!activeProduct.isNullOrBlank() && activeProduct != "সব ড্রেস") {
            allOrders.filter { o -> o.items.any { it.name.equals(activeProduct, ignoreCase = true) } }
        } else {
            allOrders
        }
        relevant.flatMap { it.items }
            .map { it.color.trim() }
            .filter { it.isNotBlank() && it != "Standard" && it != "স্বাভাবিক / উল্লেখ নেই" }
            .distinct()
            .take(12)
    }

    val pendingCourierCount = remember(allOrders) {
        allOrders.count { it.courierTrackingId.isBlank() || it.courierTrackingId.equals("PENDING", ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Top Selection Control Bar (Requirement 2)
                Surface(
                    color = if (isDark) tokens.surfaceElevated else Color(0xFF2A0F3D), // Brand deep plum in light, elevated surface in dark
                    border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedOrderIds = emptySet()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit Selection",
                                    tint = if (isDark) tokens.textPrimary else Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "${BanglaFormatter.formatNumber(selectedOrderIds.size, useBanglaDigits)} ${if (language == AppLanguage.BANGLA) "টি মার্কড" else "selected"}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (isDark) tokens.textPrimary else Color.White
                                )
                                Text(
                                    text = "${if (language == AppLanguage.BANGLA) "মোট COD: " else "Total COD: "}${BanglaFormatter.formatCurrency(totalSelectedCod, useBanglaDigits)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFFBBF24) else Color(0xFFFFB4D6)
                                )
                            }
                        }

                        // Select All / Deselect All Button
                        val allSelected = displayedOrders.isNotEmpty() && selectedOrderIds.size == displayedOrders.size
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (allSelected) {
                                if (isDark) tokens.surfaceSecondary else Color(0xFF831843)
                            } else {
                                if (isDark) tokens.surfaceMain else Color.White.copy(alpha = 0.15f)
                            },
                            border = BorderStroke(1.dp, if (allSelected) {
                                if (isDark) Color(0xFFFFFFFF) else Color(0xFFF472B6)
                            } else {
                                if (isDark) tokens.borderMain else Color.White.copy(alpha = 0.35f)
                            }),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedOrderIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        displayedOrders.map { it.id }.toSet()
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.Clear else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDark) tokens.textPrimary else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (allSelected) (if (language == AppLanguage.BANGLA) "সব আনমার্ক" else "Deselect All") else "${if (language == AppLanguage.BANGLA) "সব মার্ক" else "Select All"} (${BanglaFormatter.formatNumber(displayedOrders.size, useBanglaDigits)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) tokens.textPrimary else Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "অর্ডার তালিকা ও শিপমেন্ট" else "Orders & Shipments",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(if (isDark) tokens.backgroundMain else MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar (hidden in selection mode)
                if (!isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
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
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) tokens.borderActive else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                unfocusedContainerColor = tokens.inputSurface,
                                focusedContainerColor = tokens.inputSurface,
                                focusedTextColor = tokens.textPrimary,
                                unfocusedTextColor = tokens.textPrimary,
                                cursorColor = tokens.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    }

                    // Horizontal Status Filter Tabs (All, Pre-Order, Regular, New, Ready to Ship, etc.)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface)
                            .padding(bottom = 6.dp),
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
                                    it.fulfillmentStatus == FulfillmentStatus.PRE_ORDER_PENDING ||
                                    it.fulfillmentStatus.name.equals("PRE_ORDER_PENDING", ignoreCase = true)
                                }
                                OrderFilterTab.REGULAR -> allOrders.count {
                                    it.orderType == OrderType.REGULAR ||
                                    it.orderType.name.equals("REGULAR", ignoreCase = true)
                                }
                                OrderFilterTab.NEW -> allOrders.count {
                                    it.fulfillmentStatus == FulfillmentStatus.NEW ||
                                    it.fulfillmentStatus.name.equals("NEW", ignoreCase = true)
                                }
                                OrderFilterTab.READY_TO_SHIP -> allOrders.count {
                                    it.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP ||
                                    it.fulfillmentStatus.name.equals("READY_TO_SHIP", ignoreCase = true)
                                }
                                OrderFilterTab.SHIPPED -> allOrders.count {
                                    it.fulfillmentStatus == FulfillmentStatus.SHIPPED ||
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
                                color = if (isSelected) {
                                    if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                border = if (isSelected) {
                                    BorderStroke(1.5.dp, if (isDark) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.secondary)
                                } else {
                                    BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onTabSelected(tab) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF333333) else MaterialTheme.colorScheme.secondary
                                        } else {
                                            if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = countStr,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) {
                                                if (isDark) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.onSecondary
                                            } else {
                                                if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1-Tap Group by Dress Toggle Chip (for READY_TO_SHIP)
                    if (selectedTab == OrderFilterTab.READY_TO_SHIP) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (groupByDress) {
                                    if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = if (groupByDress) {
                                    BorderStroke(1.5.dp, if (isDark) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { groupByDress = !groupByDress }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppStrings.groupByDress(language),
                                        fontSize = 11.sp,
                                        fontWeight = if (groupByDress) FontWeight.Bold else FontWeight.Medium,
                                        color = if (groupByDress) {
                                            if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                                        } else {
                                            if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (groupByDress) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val hasActiveFilters = !activeProduct.isNullOrBlank() || !activeSize.isNullOrBlank() || !activeColor.isNullOrBlank() || activePendingCourier

                    // Compact Action Bar: Order Count (Left) + Filter & Select Buttons (Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.ordersCount(language, BanglaFormatter.formatNumber(displayedOrders.size, useBanglaDigits)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textMuted
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Compact "Filter" Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (hasActiveFilters || isFilterExpanded) {
                                    if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface
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

                            // Compact "Select" Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.2.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isSelectionMode = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Select",
                                        tint = tokens.textPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = AppStrings.select(language),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Expandable Filter Panel: Collapsed/Hidden by Default, Expands Only on Filter Button Tap
                    AnimatedVisibility(
                        visible = isFilterExpanded,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            color = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (hasActiveFilters) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = AppStrings.active(language),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.Black else Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = AppStrings.clearAll(language),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) tokens.errorForeground else MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { handleClearFilters() }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Catalog dresses horizontal scrolling chips
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    item {
                                        val isAllSelected = activeProduct.isNullOrBlank() || activeProduct == "সব ড্রেস" || activeProduct == "All Dresses"
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isAllSelected) {
                                                if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            border = if (isAllSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { handleProductSelect(null) }
                                        ) {
                                            Text(
                                                text = "${AppStrings.allDresses(language)} (${BanglaFormatter.formatNumber(allOrders.size, useBanglaDigits)})",
                                                fontSize = 11.sp,
                                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isAllSelected) {
                                                    if (isDark) Color.Black else Color.White
                                                } else {
                                                    if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    items(availableDresses, key = { it.name }) { dress ->
                                        val isSelected = activeProduct.equals(dress.name, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) {
                                                if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            border = if (isSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
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
                                                            .size(16.dp)
                                                            .clip(RoundedCornerShape(3.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Inventory2,
                                                        contentDescription = null,
                                                        tint = if (isSelected) {
                                                            if (isDark) Color.Black else Color.White
                                                        } else {
                                                            if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.primary
                                                        },
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = "${dress.name} (${BanglaFormatter.formatNumber(dress.orderCount, useBanglaDigits)})",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) {
                                                        if (isDark) Color.Black else Color.White
                                                    } else {
                                                        if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Size, Courier & Color sub-filter row
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    // Clean Pending Courier Chip (NO emoji!)
                                    item {
                                        val bg = if (activePendingCourier) {
                                            if (isDark) Color(0xFF78350F) else Color(0xFFF59E0B)
                                        } else {
                                            if (isDark) Color(0xFF2A1B05) else Color(0xFFFEF3C7)
                                        }
                                        val border = if (activePendingCourier) {
                                            if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                        } else {
                                            if (isDark) Color(0xFF78350F) else Color(0xFFF59E0B)
                                        }
                                        val fg = if (activePendingCourier) {
                                            if (isDark) Color(0xFFFFFFFF) else Color.White
                                        } else {
                                            if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = bg,
                                            border = BorderStroke(1.dp, border),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { handleTogglePendingCourier() }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${AppStrings.pendingCourier(language)} (${BanglaFormatter.formatNumber(pendingCourierCount, useBanglaDigits)})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = fg
                                                )
                                                if (activePendingCourier) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Default.Check, null, tint = fg, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Size sub-filter chips
                                    val sizeChips = listOf("all", "36", "38", "40", "42", "44", "46", "Free")
                                    items(sizeChips) { sz ->
                                        val isSzSelected = if (sz == "all") {
                                            activeSize.isNullOrBlank() || activeSize == "সব সাইজ" || activeSize == "All Sizes"
                                        } else {
                                            activeSize.equals(sz, ignoreCase = true)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSzSelected) {
                                                if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.secondary
                                            } else {
                                                if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            },
                                            border = if (isSzSelected) null else BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (sz == "all") {
                                                        handleSizeSelect(null)
                                                    } else {
                                                        if (isSzSelected) handleSizeSelect(null) else handleSizeSelect(sz)
                                                    }
                                                }
                                        ) {
                                            val labelText = if (sz == "all") {
                                                AppStrings.allSizes(language)
                                            } else {
                                                val formattedSz = if (language == AppLanguage.BANGLA && sz.all { it.isDigit() }) BanglaFormatter.toBanglaDigits(sz) else sz
                                                "${AppStrings.size(language)}: $formattedSz"
                                            }
                                            Text(
                                                text = labelText,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSzSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSzSelected) {
                                                    if (isDark) Color.Black else Color.White
                                                } else {
                                                    if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Color sub-filters
                                    if (availableColors.isNotEmpty()) {
                                        items(availableColors) { col ->
                                            val isColSelected = activeColor.equals(col, ignoreCase = true)
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isColSelected) {
                                                    if (isDark) tokens.surfaceSecondary else Color(0xFF6B21A8)
                                                } else {
                                                    if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                },
                                                border = BorderStroke(1.dp, if (isColSelected) {
                                                    if (isDark) Color(0xFFFFFFFF) else Color(0xFF6B21A8)
                                                } else {
                                                    if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                }),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        if (isColSelected) handleColorSelect(null) else handleColorSelect(col)
                                                    }
                                            ) {
                                                Text(
                                                    text = "${if (language == AppLanguage.BANGLA) "কালার: " else "Color: "}$col",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isColSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isColSelected) {
                                                        if (isDark) Color(0xFFFFFFFF) else Color.White
                                                    } else {
                                                        if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurface
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Multi-select Mode Helper Sub-banner
                    Surface(
                        color = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "কার্ডে ট্যাপ করে সিলেক্ট করুন অথবা নিচে বাল্ক অ্যাকশন নিন" else "Tap cards to select or use bulk actions below",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${BanglaFormatter.formatNumber(displayedOrders.size, useBanglaDigits)} ${if (language == AppLanguage.BANGLA) "টির মধ্যে" else "orders"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // =========================================================================
                // ORDER CARDS LIST (with Checkbox & Selection State)
                // =========================================================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (isSelectionMode && selectedOrderIds.isNotEmpty()) 130.dp else 90.dp
                    )
                ) {
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
                                        tint = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) "এই ফিল্টারে কোনো অর্ডার নেই" else "No orders in this filter",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!activeProduct.isNullOrBlank() || !activeSize.isNullOrBlank() || activePendingCourier) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { handleClearFilters() },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline)
                                        ) {
                                            Text("ফিল্টার রিসেট করুন", color = tokens.textPrimary)
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
                                    .padding(vertical = 5.dp)
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
                                        } else {
                                            selectedOrderIds = if (isSelected) {
                                                selectedOrderIds - order.id
                                            } else {
                                                selectedOrderIds + order.id
                                            }
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
            }

            // =========================================================================
            // FLOATING BULK ACTION DOCK (Requirement 2 - 5 Buttons)
            // =========================================================================
            AnimatedVisibility(
                visible = isSelectionMode && selectedOrderIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) tokens.surfaceElevated else Color(0xFF1E0A2D), // Deep brand plum in light, elevated surface in dark
                    border = BorderStroke(1.5.dp, if (isDark) tokens.borderMain else Color(0xFFE935C1).copy(alpha = 0.7f)),
                    shadowElevation = if (isDark) 0.dp else 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 1. 🚀 স্টেডফাস্ট এন্ট্রি
                        DockActionButton(
                            icon = Icons.Default.LocalShipping,
                            label = "স্টেডফাস্ট এন্ট্রি",
                            badgeText = "🚀",
                            tint = Color(0xFF38BDF8),
                            textColor = if (isDark) tokens.textPrimary else Color.White,
                            onClick = {
                                if (onStartBulkCourierSync != null) {
                                    onStartBulkCourierSync(selectedOrders)
                                } else {
                                    onBulkRetrySync?.invoke(selectedOrders)
                                }
                                isSelectionMode = false
                                selectedOrderIds = emptySet()
                            }
                        )

                        // 2. 📋 পিক-লিস্ট
                        DockActionButton(
                            icon = Icons.Default.Inventory2,
                            label = "পিক-লিস্ট",
                            badgeText = "📋",
                            tint = Color(0xFFFBBF24),
                            textColor = if (isDark) tokens.textPrimary else Color.White,
                            onClick = {
                                showPickListDialog = true
                            }
                        )

                        // 3. 👗 ড্রেস বসান
                        DockActionButton(
                            icon = Icons.Default.CheckCircle,
                            label = "ড্রেস বসান",
                            badgeText = "👗",
                            tint = Color(0xFFF472B6),
                            textColor = if (isDark) tokens.textPrimary else Color.White,
                            onClick = {
                                showAssignDressDialog = true
                            }
                        )

                        // 4. 🖨️ প্রিন্ট / শিট
                        DockActionButton(
                            icon = Icons.Default.Print,
                            label = "প্রিন্ট / শিট",
                            badgeText = "🖨️",
                            tint = Color(0xFF34D399),
                            textColor = if (isDark) tokens.textPrimary else Color.White,
                            onClick = {
                                showBulkPrintDialog = true
                            }
                        )

                        // 5. ⚡ আরও
                        DockActionButton(
                            icon = Icons.Default.MoreVert,
                            label = "আরও",
                            badgeText = "⚡",
                            tint = if (isDark) tokens.textPrimary else Color.White,
                            textColor = if (isDark) tokens.textPrimary else Color.White,
                            onClick = {
                                showMoreActionsDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS: PICK-LIST, ASSIGN DRESS, BULK LABELS, MORE ACTIONS, DELETE CONFIRM
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

    // 4. Bulk Delete Confirmation Dialog
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

    // 5. Bulk Courier Sync Progress & Report Dialog
    if (bulkCourierSyncState.isActive) {
        BulkCourierSyncProgressDialog(
            syncState = bulkCourierSyncState,
            useBanglaDigits = useBanglaDigits,
            onStop = { onStopBulkCourierSync?.invoke() },
            onDismiss = { onDismissBulkCourierSync?.invoke() }
        )
    }
}

@Composable
private fun DockActionButton(
    icon: ImageVector,
    label: String,
    badgeText: String,
    tint: Color,
    textColor: Color = Color.White,
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$badgeText $label",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun BulkCourierSyncProgressDialog(
    syncState: BulkCourierSyncState,
    useBanglaDigits: Boolean,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!syncState.isActive) return

    val total = syncState.totalOrders
    val processed = syncState.processedCount
    val progress = if (total > 0) processed.toFloat() / total.toFloat() else 0f
    val skippedAndFailed = syncState.alreadySyncedCount + syncState.preflightSkippedCount + syncState.apiFailedCount

    val totalStr = BanglaFormatter.formatNumber(total, useBanglaDigits)
    val processedStr = BanglaFormatter.formatNumber(processed, useBanglaDigits)
    val successStr = BanglaFormatter.formatNumber(syncState.successCount, useBanglaDigits)
    val skippedFailedStr = BanglaFormatter.formatNumber(skippedAndFailed, useBanglaDigits)
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark

    Dialog(onDismissRequest = {
        if (!syncState.isRunning) onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface
            ),
            border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (syncState.isRunning) "🚀" else "📊",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = if (syncState.isRunning) "স্টেডফাস্ট বাল্ক এন্ট্রি" else "এন্ট্রি সম্পন্ন রিপোর্ট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                    }

                    if (!syncState.isRunning) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = tokens.textMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error state if credentials missing
                if (!syncState.errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) tokens.errorContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = tokens.errorForeground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncState.errorMessage,
                                fontSize = 12.sp,
                                color = tokens.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDark) tokens.onPrimaryAction else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("ঠিক আছে", fontWeight = FontWeight.Bold)
                    }
                    return@Column
                }

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status text: "স্টেডফাস্টে এন্ট্রি হচ্ছে: ১২৫ / ২৫০ (✅ সফল: ১২৩ | ⚠️ স্কিপড/ব্যর্থ: ২)"
                Text(
                    text = if (syncState.isRunning) {
                        "স্টেডফাস্টে এন্ট্রি হচ্ছে: $processedStr / $totalStr (✅ সফল: $successStr | ⚠️ স্কিপড/ব্যর্থ: $skippedFailedStr)"
                    } else {
                        "মোট প্রসেসড: $processedStr / $totalStr (✅ সফল: $successStr)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Currently processing box (when running)
                if (syncState.isRunning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = if (isDark) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "বর্তমান প্রসেসিং অর্ডার:",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${syncState.currentCustomerName} • ${syncState.currentInvoice}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stop Button
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) tokens.errorContainer else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (isDark) tokens.onErrorContainer else MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⏸️ থামান (Stop)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Summary cards when finished
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFD1FAE5),
                            border = if (isDark) BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.6f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "✅ সফল",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                )
                                Text(
                                    successStr,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.5f) else Color(0xFFE0E7FF),
                            border = if (isDark) BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "⏩ পূর্বেই ছিল",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3)
                                )
                                val preStr = BanglaFormatter.formatNumber(syncState.alreadySyncedCount, useBanglaDigits)
                                Text(
                                    preStr,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF3730A3)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF450A0A).copy(alpha = 0.5f) else Color(0xFFFEE2E2),
                            border = if (isDark) BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "⚠️ স্কিপ/ব্যর্থ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                )
                                val failStr = BanglaFormatter.formatNumber(syncState.preflightSkippedCount + syncState.apiFailedCount, useBanglaDigits)
                                Text(
                                    failStr,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    // Detailed breakdown of skipped or failed orders if any
                    val nonSuccessReports = syncState.reports.filter { it.status != BulkSyncItemStatus.SUCCESS }
                    if (nonSuccessReports.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "স্কিপ ও ব্যর্থতার কারণসমূহ (${nonSuccessReports.size}টি):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            items(nonSuccessReports) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (item.status) {
                                                BulkSyncItemStatus.ALREADY_SYNCED -> "⏩"
                                                BulkSyncItemStatus.INVALID_INFO -> "⚠️"
                                                BulkSyncItemStatus.API_FAILED -> "❌"
                                                else -> "•"
                                            },
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${item.invoice} • ${item.customerName}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tokens.textPrimary
                                            )
                                            Text(
                                                text = item.reason,
                                                fontSize = 10.sp,
                                                color = tokens.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDark) tokens.onPrimaryAction else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("ঠিক আছে (Dismiss)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
