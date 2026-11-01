package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.data.model.CatalogProductEntity
import com.example.data.model.CourierService
import com.example.data.model.CourierSyncStatus
import com.example.data.model.DeliveryTier
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItem
import com.example.data.model.OrderType
import com.example.data.model.PaymentStatus
import com.example.data.model.Priority
import com.example.data.parser.ParsedOrderDraft
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CourierRepository
import com.example.data.repository.OrderRepository
import com.example.util.ExportHelper
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


enum class OrderFilterTab(val titleBn: String, val titleEn: String) {
    ALL("সব", "All"),
    PRE_ORDER("প্রি-অর্ডার", "Pre-Order"),
    REGULAR("রেগুলার", "Regular"),
    NEW("নতুন", "New"),
    READY_TO_SHIP("রেডি টু শিপ", "Ready to Ship"),
    SHIPPED("শিপড", "Shipped"),
    URGENT("জরুরি", "Urgent"),
    SCHEDULED("শিডিউলড", "Scheduled"),
    PENDING_SYNC("কুরিয়ার সিঙ্ক বাকি", "Pending Sync")
}

data class DashboardMetrics(
    val totalOrdersCount: Int = 0,
    val todaySales: Double = 0.0,
    val pendingDeliveryCount: Int = 0,
    val pendingCodAmount: Double = 0.0
)

data class UndoAction(
    val orderId: Long,
    val previousStatus: FulfillmentStatus,
    val description: String
)

enum class BulkSyncItemStatus {
    SUCCESS,
    ALREADY_SYNCED,
    INVALID_INFO,
    API_FAILED
}

data class BulkSyncItemReport(
    val invoice: String,
    val customerName: String,
    val status: BulkSyncItemStatus,
    val reason: String = "",
    val consignmentId: String? = null
)

data class BulkCourierSyncState(
    val isActive: Boolean = false,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val totalOrders: Int = 0,
    val processedCount: Int = 0,
    val currentInvoice: String = "",
    val currentCustomerName: String = "",
    val successCount: Int = 0,
    val alreadySyncedCount: Int = 0,
    val preflightSkippedCount: Int = 0,
    val apiFailedCount: Int = 0,
    val reports: List<BulkSyncItemReport> = emptyList(),
    val errorMessage: String? = null
)

data class RestorePreviewState(
    val metadata: com.example.util.BackupMetadata,
    val orders: List<OrderEntity>,
    val catalogProducts: List<CatalogProductEntity>,
    val payloadBytes: ByteArray,
    val currentDbOrdersCount: Int,
    val currentDbCatalogCount: Int
)

class OrderViewModel(
    private val orderRepository: OrderRepository,
    private val courierRepository: CourierRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _restorePreviewState = MutableStateFlow<RestorePreviewState?>(null)
    val restorePreviewState: StateFlow<RestorePreviewState?> = _restorePreviewState.asStateFlow()

    private val _bulkCourierSyncState = MutableStateFlow(BulkCourierSyncState())
    val bulkCourierSyncState: StateFlow<BulkCourierSyncState> = _bulkCourierSyncState.asStateFlow()

    private var bulkSyncJob: Job? = null

    val catalogProducts: StateFlow<List<CatalogProductEntity>> = catalogRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(OrderFilterTab.ALL)
    val selectedTab: StateFlow<OrderFilterTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProductFilter = MutableStateFlow<String?>(null)
    val selectedProductFilter: StateFlow<String?> = _selectedProductFilter.asStateFlow()

    private val _selectedSizeFilter = MutableStateFlow<String?>(null)
    val selectedSizeFilter: StateFlow<String?> = _selectedSizeFilter.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<String?>(null)
    val selectedColorFilter: StateFlow<String?> = _selectedColorFilter.asStateFlow()

    private val _isPendingCourierFilterActive = MutableStateFlow(false)
    val isPendingCourierFilterActive: StateFlow<Boolean> = _isPendingCourierFilterActive.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _undoAction = MutableStateFlow<UndoAction?>(null)
    val undoAction: StateFlow<UndoAction?> = _undoAction.asStateFlow()

    val allOrders: StateFlow<List<OrderEntity>> = orderRepository.allActiveOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedOrders: StateFlow<List<OrderEntity>> = orderRepository.trashedOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterParams(
        val tab: OrderFilterTab,
        val query: String,
        val product: String?,
        val size: String?,
        val color: String?,
        val pendingCourier: Boolean
    )

    private val filterParams = combine(
        combine(_selectedTab, _searchQuery, _selectedProductFilter) { tab, q, prod -> Triple(tab, q, prod) },
        combine(_selectedSizeFilter, _selectedColorFilter, _isPendingCourierFilterActive) { sz, col, pend -> Triple(sz, col, pend) }
    ) { (tab, q, prod), (sz, col, pend) ->
        FilterParams(tab, q, prod, sz, col, pend)
    }

    // Filtered orders stream
    val filteredOrders: StateFlow<List<OrderEntity>> = combine(
        allOrders,
        filterParams
    ) { orders, params ->
        var list = orders

        if (params.query.isNotBlank()) {
            val q = params.query.trim().lowercase()
            list = list.filter {
                it.invoiceNo.lowercase().contains(q) ||
                it.customerName.lowercase().contains(q) ||
                it.customerPhone.contains(q) ||
                it.district.lowercase().contains(q) ||
                it.courierTrackingId.lowercase().contains(q) ||
                it.items.any { item -> item.name.lowercase().contains(q) }
            }
        }

        list = when (params.tab) {
            OrderFilterTab.ALL -> list
            OrderFilterTab.PRE_ORDER -> list.filter {
                it.orderType == OrderType.PRE_ORDER ||
                it.orderType.name.equals("PRE_ORDER", ignoreCase = true) ||
                it.fulfillmentStatus == FulfillmentStatus.PRE_ORDER_PENDING ||
                it.fulfillmentStatus.name.equals("PRE_ORDER_PENDING", ignoreCase = true)
            }
            OrderFilterTab.REGULAR -> list.filter {
                it.orderType == OrderType.REGULAR ||
                it.orderType.name.equals("REGULAR", ignoreCase = true)
            }
            OrderFilterTab.NEW -> list.filter {
                it.fulfillmentStatus == FulfillmentStatus.NEW ||
                it.fulfillmentStatus.name.equals("NEW", ignoreCase = true)
            }
            OrderFilterTab.READY_TO_SHIP -> list.filter {
                it.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP ||
                it.fulfillmentStatus.name.equals("READY_TO_SHIP", ignoreCase = true)
            }
            OrderFilterTab.SHIPPED -> list.filter {
                it.fulfillmentStatus == FulfillmentStatus.SHIPPED ||
                it.fulfillmentStatus.name.equals("SHIPPED", ignoreCase = true)
            }
            OrderFilterTab.URGENT -> list.filter {
                it.priority == Priority.URGENT ||
                it.priority.name.equals("URGENT", ignoreCase = true)
            }
            OrderFilterTab.SCHEDULED -> list.filter {
                it.isScheduled ||
                it.orderType == OrderType.SCHEDULED ||
                it.orderType.name.equals("SCHEDULED", ignoreCase = true) ||
                it.requestedDeliveryDate.isNotBlank()
            }
            OrderFilterTab.PENDING_SYNC -> list.filter {
                it.courierSyncStatus == CourierSyncStatus.PENDING_SYNC ||
                it.courierSyncStatus.name.equals("PENDING_SYNC", ignoreCase = true)
            }
        }

        // Product Filter
        if (!params.product.isNullOrBlank() && params.product != "সব ড্রেস") {
            list = list.filter { order ->
                order.items.any { item -> item.name.equals(params.product, ignoreCase = true) }
            }
        }

        // Size Filter
        if (!params.size.isNullOrBlank() && params.size != "সব সাইজ") {
            list = list.filter { order ->
                order.items.any { item -> item.size.equals(params.size, ignoreCase = true) }
            }
        }

        // Color Filter
        if (!params.color.isNullOrBlank() && params.color != "সব কালার") {
            list = list.filter { order ->
                order.items.any { item -> item.color.equals(params.color, ignoreCase = true) }
            }
        }

        // Pending Courier Consignment ID Filter
        if (params.pendingCourier) {
            list = list.filter { order ->
                order.courierTrackingId.isBlank() || order.courierTrackingId.equals("PENDING", ignoreCase = true)
            }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2x2 Metric calculations
    val metrics: StateFlow<DashboardMetrics> = allOrders.combine(MutableStateFlow(Unit)) { orders, _ ->
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val todayOrders = orders.filter { (now - it.createdAt) < oneDayMillis }

        val todaySales = todayOrders.sumOf { it.totalBill }
        val pendingDelivery = orders.count {
            it.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP ||
            it.fulfillmentStatus == FulfillmentStatus.PROCESSING ||
            it.fulfillmentStatus == FulfillmentStatus.SHIPPED
        }
        val pendingCod = orders.filter {
            it.fulfillmentStatus != FulfillmentStatus.DELIVERED &&
            it.fulfillmentStatus != FulfillmentStatus.CANCELLED &&
            it.fulfillmentStatus != FulfillmentStatus.RETURNED
        }.sumOf { it.codAmount }

        DashboardMetrics(
            totalOrdersCount = orders.size,
            todaySales = todaySales,
            pendingDeliveryCount = pendingDelivery,
            pendingCodAmount = pendingCod
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    fun setFilterTab(tab: OrderFilterTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProductFilter(productName: String?) {
        _selectedProductFilter.value = if (productName == "সব ড্রেস") null else productName
    }

    fun setSizeFilter(size: String?) {
        _selectedSizeFilter.value = if (size == "সব সাইজ") null else size
    }

    fun setColorFilter(color: String?) {
        _selectedColorFilter.value = if (color == "সব কালার") null else color
    }

    fun togglePendingCourierFilter() {
        _isPendingCourierFilterActive.value = !_isPendingCourierFilterActive.value
    }

    fun clearProductAndSizeFilters() {
        _selectedProductFilter.value = null
        _selectedSizeFilter.value = null
        _selectedColorFilter.value = null
        _isPendingCourierFilterActive.value = false
    }

    // 1-Tap Mark as Shipped (with Zero-Refresh + Undo)
    fun markAsShipped(order: OrderEntity) {
        viewModelScope.launch {
            val previous = order.fulfillmentStatus
            orderRepository.markAsShipped(order.id)
            _undoAction.value = UndoAction(order.id, previous, "${order.invoiceNo} শিপড হিসেবে মার্ক করা হয়েছে")
            _snackbarMessage.emit("${order.invoiceNo} শিপড হিসেবে স্থানান্তরিত হয়েছে 🚚")
        }
    }

    fun markAsProcessing(order: OrderEntity) {
        viewModelScope.launch {
            orderRepository.markAsProcessing(order.id)
            _snackbarMessage.emit("${order.invoiceNo} স্টক এসেছে ➔ প্রসেসিং-এ যুক্ত হয়েছে ✨")
        }
    }

    fun undoLastAction() {
        viewModelScope.launch {
            val action = _undoAction.value ?: return@launch
            orderRepository.updateFulfillmentStatus(action.orderId, action.previousStatus)
            _undoAction.value = null
            _snackbarMessage.emit("অর্ডার পূর্বাবস্থায় ফিরিয়ে আনা হয়েছে")
        }
    }

    fun saveConsignmentId(orderId: Long, consignmentId: String) {
        viewModelScope.launch {
            val result = courierRepository.manualSetConsignmentId(orderId, consignmentId)
            if (result.isSuccess) {
                _snackbarMessage.emit("Consignment ID সেভ হয়েছে। অর্ডার রেডি টু শিপে যুক্ত হয়েছে ✅")
            } else {
                _snackbarMessage.emit("আইডি সেভ করা যায়নি")
            }
        }
    }

    fun bookCourierDirect(order: OrderEntity, apiKey: String, secretKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank() || secretKey.isBlank()) {
                // Do not generate mock/dummy IDs! Keep tracking ID blank & set PENDING_SYNC
                courierRepository.markAsPendingSync(order.id)
                _snackbarMessage.emit("${order.invoiceNo}: ⚠️ কুরিয়ার API থেকে আইডি পাওয়া যায়নি (পেন্ডিং সিঙ্ক)")
                return@launch
            }

            val result = courierRepository.bookSingleOrder(order, apiKey, secretKey)
            if (result.isSuccess) {
                val consignmentId = result.getOrNull()
                _snackbarMessage.emit("${order.invoiceNo}: কুরিয়ার বুকিং সম্পন্ন! Consignment ID: $consignmentId 📦")
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "কুরিয়ার এন্ট্রি ব্যর্থ"
                courierRepository.markAsPendingSync(order.id)
                _snackbarMessage.emit("${order.invoiceNo}: ⚠️ কুরিয়ার API থেকে আইডি পাওয়া যায়নি ($errorMsg)")
            }
        }
    }

    fun saveCatalogProduct(product: CatalogProductEntity) {
        viewModelScope.launch {
            catalogRepository.insertProduct(product)
            _snackbarMessage.emit("${product.name} ক্যাটালগে সংরক্ষিত হয়েছে 👗")
        }
    }

    fun deleteCatalogProduct(id: String, productName: String) {
        viewModelScope.launch {
            catalogRepository.deleteProduct(id)
            _snackbarMessage.emit("$productName ক্যাটালগ থেকে মুছে ফেলা হয়েছে 🗑️")
        }
    }

    fun restoreOrdersFromJson(json: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val backupResult = ExportHelper.importBackupFromJson(json)
                val orders = backupResult.orders
                val catalog = backupResult.catalogProducts
                var restoredOrderCount = 0
                if (orders.isNotEmpty()) {
                    orderRepository.restoreOrders(orders)
                    restoredOrderCount = orders.size
                }
                if (catalog.isNotEmpty()) {
                    catalogRepository.restoreCatalog(catalog)
                }
                if (restoredOrderCount > 0 || catalog.isNotEmpty()) {
                    val msg = buildString {
                        if (restoredOrderCount > 0) append("${restoredOrderCount}টি অর্ডার ")
                        if (catalog.isNotEmpty()) append("${catalog.size}টি ক্যাটালগ ড্রেস ")
                        append("সফলভাবে রিস্টোর হয়েছে! 📥")
                    }
                    _snackbarMessage.emit(msg)
                    onComplete(restoredOrderCount)
                } else {
                    _snackbarMessage.emit("JSON ফাইলে কোনো বৈধ অর্ডার বা ক্যাটালগ ডাটা পাওয়া যায়নি।")
                    onComplete(0)
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("রিস্টোর ব্যর্থ: ${e.localizedMessage}")
                onComplete(0)
            }
        }
    }

    fun dismissRestorePreview() {
        _restorePreviewState.value = null
    }

    /**
     * Inspects the selected backup file without modifying the database.
     * Generates a preview with metadata and conflicting ID detection.
     */
    fun inspectBackupForRestore(context: Context, inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingIds = orderRepository.getAllExistingOrderIds()
                val currentOrders = orderRepository.getAllOrdersDirect()
                val currentCatalog = catalogRepository.getAllProductsDirect()

                val inspection = ExportHelper.inspectBackupStream(context, inputStream, existingIds)
                if (inspection.isSuccess) {
                    val res = inspection.getOrThrow()
                    _restorePreviewState.value = RestorePreviewState(
                        metadata = res.metadata,
                        orders = res.orders,
                        catalogProducts = res.catalogProducts,
                        payloadBytes = res.payloadBytes,
                        currentDbOrdersCount = currentOrders.size,
                        currentDbCatalogCount = currentCatalog.size
                    )
                } else {
                    val err = inspection.exceptionOrNull()?.localizedMessage ?: "অজানা ত্রুটি"
                    _snackbarMessage.emit("ব্যাকআপ ফাইল পড়া সম্ভব হয়নি: $err")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("ব্যাকআপ ফাইল প্রক্রিয়াকরণে ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Executes the restore operation following a user-confirmed strategy.
     * CRITICAL SAFETY: Creates a verified snapshot of the current database before touching any data.
     * If the snapshot fails, the restore is cancelled immediately.
     */
    fun executeSafeRestore(
        context: Context,
        conflictStrategy: com.example.util.RestoreConflictStrategy,
        onComplete: (com.example.util.RestoreSummary) -> Unit = {}
    ) {
        val preview = _restorePreviewState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. SAFETY SNAPSHOT: Capture current state before any modifications
                val currentOrders = orderRepository.getAllOrdersDirect()
                val currentCatalog = catalogRepository.getAllProductsDirect()
                val snapshotResult = ExportHelper.createSafetySnapshot(
                    context = context,
                    orders = currentOrders,
                    catalogProducts = currentCatalog,
                    tag = "pre_restore"
                )

                if (snapshotResult.isFailure) {
                    val reason = snapshotResult.exceptionOrNull()?.message ?: "স্ন্যাপশট ফাইল তৈরি ব্যর্থ"
                    _snackbarMessage.emit("⚠️ ডাটা সুরক্ষা সতর্কতা: সেফটি ব্যাকআপ ব্যর্থ হওয়ায় রিস্টোর বাতিল করা হয়েছে! ($reason)")
                    return@launch
                }

                val snapshotFile = snapshotResult.getOrNull()

                // 2. ATOMIC DATABASE RESTORE: Catalog + Orders inside a single Room transaction
                val restoreSummary = orderRepository.restoreDatabaseSafely(
                    orders = preview.orders,
                    catalogProducts = preview.catalogProducts,
                    conflictStrategy = conflictStrategy
                )

                // 3. Restore Photos from ZIP payload (executed only after database transaction commits)
                val zipResult = ExportHelper.unpackZipPayload(context, preview.payloadBytes)

                val finalSummary = restoreSummary.copy(
                    totalIncomingProducts = preview.catalogProducts.size,
                    restoredPhotosCount = zipResult.restoredPhotosCount,
                    safetySnapshotPath = snapshotFile?.name
                )

                // Dismiss preview dialog
                _restorePreviewState.value = null

                val msg = buildString {
                    append("রিস্টোর সফল! ")
                    if (finalSummary.insertedOrdersCount > 0) append("${finalSummary.insertedOrdersCount}টি নতুন, ")
                    if (finalSummary.renumberedOrdersCount > 0) append("${finalSummary.renumberedOrdersCount}টি নতুন আইডিতে সুরক্ষিত, ")
                    if (finalSummary.skippedOrdersCount > 0) append("${finalSummary.skippedOrdersCount}টি ডুপ্লিকেট বাদ, ")
                    if (finalSummary.overwrittenOrdersCount > 0) append("${finalSummary.overwrittenOrdersCount}টি প্রতিস্থাপিত, ")
                    if (preview.catalogProducts.isNotEmpty()) append("${preview.catalogProducts.size}টি ড্রেস, ")
                    if (finalSummary.restoredPhotosCount > 0) append("${finalSummary.restoredPhotosCount}টি ছবি। ")
                    append("বর্তমান ডাটার সেফটি স্ন্যাপশট সংরক্ষিত রয়েছে ✅")
                }
                _snackbarMessage.emit(msg)
                onComplete(finalSummary)
            } catch (e: Exception) {
                _snackbarMessage.emit("রিস্টোর প্রক্রিয়ায় ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Backward-compatible entrypoint: Now safely routes to metadata inspection and explicit confirmation.
     */
    fun restoreFromBackupStream(context: Context, inputStream: InputStream, onComplete: (Int, Int) -> Unit = { _, _ -> }) {
        inspectBackupForRestore(context, inputStream)
    }

    fun bookSteadfastApi(order: OrderEntity, apiKey: String, secretKey: String) {
        viewModelScope.launch {
            _snackbarMessage.emit("Steadfast API দিয়ে বুকিং করা হচ্ছে...")
            val result = courierRepository.bookSingleOrder(order, apiKey, secretKey)
            if (result.isSuccess) {
                val consignmentId = result.getOrNull()
                _snackbarMessage.emit("সফল! Consignment ID: $consignmentId (রেডি টু শিপ) 📦")
            } else {
                val error = result.exceptionOrNull()?.message ?: "বুকিং ব্যর্থ হয়েছে"
                _snackbarMessage.emit("ব্যর্থ: $error (পেন্ডিং কিউতে রাখা হয়েছে)")
            }
        }
    }

    fun syncAllPendingOrders(apiKey: String, secretKey: String) {
        viewModelScope.launch {
            _snackbarMessage.emit("পেন্ডিং কুরিয়ার বাল্ক সিঙ্ক শুরু হচ্ছে...")
            val result = courierRepository.syncPendingOrders(apiKey, secretKey)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _snackbarMessage.emit("$count টি অর্ডার সফলভাবে কুরিয়ারে বুকিং হয়েছে ✅")
            } else {
                _snackbarMessage.emit("বাল্ক সিঙ্ক ব্যর্থ: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun moveToTrash(order: OrderEntity) {
        viewModelScope.launch {
            orderRepository.moveToTrash(order.id)
            _snackbarMessage.emit("${order.invoiceNo} ট্র্যাশে পাঠানো হয়েছে")
        }
    }

    fun bulkUpdateStatus(orderIds: Set<Long>, newStatus: FulfillmentStatus) {
        viewModelScope.launch {
            if (orderIds.isEmpty()) return@launch
            orderIds.forEach { id ->
                orderRepository.updateFulfillmentStatus(id, newStatus)
            }
            val statusLabel = when (newStatus) {
                FulfillmentStatus.READY_TO_SHIP -> "রেডি টু শিপ"
                FulfillmentStatus.SHIPPED -> "শিপড"
                FulfillmentStatus.PROCESSING -> "প্রসেসিং"
                FulfillmentStatus.DELIVERED -> "ডেলিভার্ড"
                FulfillmentStatus.CANCELLED -> "বাতিল"
                else -> newStatus.name
            }
            _snackbarMessage.emit("${orderIds.size}টি অর্ডার '$statusLabel' স্ট্যাটাসে আপডেট করা হয়েছে ✅")
        }
    }

    fun bulkConvertToRegular(orderIds: Set<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            if (orderIds.isEmpty()) return@launch
            val updated = mutableListOf<OrderEntity>()
            orderIds.forEach { id ->
                val order = orderRepository.getOrderByIdDirect(id)
                if (order != null) {
                    updated.add(order.copy(
                        orderType = OrderType.REGULAR,
                        fulfillmentStatus = FulfillmentStatus.READY_TO_SHIP,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }
            if (updated.isNotEmpty()) {
                orderRepository.updateOrders(updated)
            }
            _snackbarMessage.emit("${updated.size}টি অর্ডার 'রেগুলার' ও 'রেডি টু শিপ'-এ রূপান্তরিত হয়েছে 🔄")
        }
    }

    fun bulkUpdateDeliveryDate(orderIds: Set<Long>, dateStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (orderIds.isEmpty()) return@launch
            val updated = mutableListOf<OrderEntity>()
            orderIds.forEach { id ->
                val order = orderRepository.getOrderByIdDirect(id)
                if (order != null) {
                    updated.add(order.copy(
                        requestedDeliveryDate = dateStr,
                        isScheduled = true,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }
            if (updated.isNotEmpty()) {
                orderRepository.updateOrders(updated)
            }
            _snackbarMessage.emit("${updated.size}টি অর্ডারের ডেলিভারি তারিখ '$dateStr' আপডেট করা হয়েছে 📅")
        }
    }

    fun bulkUpdateStatusWithPayment(orderIds: Set<Long>, newStatus: FulfillmentStatus, newPayment: PaymentStatus? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (orderIds.isEmpty()) return@launch
            if (newPayment != null) {
                val updated = mutableListOf<OrderEntity>()
                orderIds.forEach { id ->
                    val order = orderRepository.getOrderByIdDirect(id)
                    if (order != null) {
                        updated.add(order.copy(
                            fulfillmentStatus = newStatus,
                            paymentStatus = newPayment,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                }
                if (updated.isNotEmpty()) {
                    orderRepository.updateOrders(updated)
                }
                _snackbarMessage.emit("${updated.size}টি অর্ডার 'ডেলিভার্ড ও পেইড' হিসেবে মার্ক করা হয়েছে 💰")
            } else {
                bulkUpdateStatus(orderIds, newStatus)
            }
        }
    }

    fun bulkAssignCatalogProduct(
        orderIds: Set<Long>,
        product: CatalogProductEntity,
        keepExistingSizes: Boolean = true,
        overrideSize: String? = null,
        overrideColor: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (orderIds.isEmpty()) return@launch
            val updatedOrders = mutableListOf<OrderEntity>()
            orderIds.forEach { id ->
                val order = orderRepository.getOrderByIdDirect(id)
                if (order != null) {
                    val updatedItems = if (order.items.isNotEmpty()) {
                        order.items.map { item ->
                            val itemSize = if (keepExistingSizes && item.size.isNotBlank()) {
                                item.size
                            } else if (!overrideSize.isNullOrBlank()) {
                                overrideSize
                            } else if (item.size.isNotBlank()) {
                                item.size
                            } else {
                                product.availableSizes.firstOrNull() ?: ""
                            }
                            val itemColor = if (!overrideColor.isNullOrBlank()) {
                                overrideColor
                            } else {
                                item.color
                            }
                            item.copy(
                                name = product.name,
                                photoUri = product.photoUri,
                                unitPrice = product.basePrice,
                                size = itemSize,
                                color = itemColor
                            )
                        }
                    } else {
                        val itemSize = if (!overrideSize.isNullOrBlank()) {
                            overrideSize
                        } else {
                            product.availableSizes.firstOrNull() ?: ""
                        }
                        listOf(
                            OrderItem(
                                name = product.name,
                                photoUri = product.photoUri,
                                unitPrice = product.basePrice,
                                size = itemSize,
                                color = overrideColor ?: "",
                                quantity = 1
                            )
                        )
                    }
                    updatedOrders.add(order.copy(items = updatedItems, updatedAt = System.currentTimeMillis()))
                }
            }
            if (updatedOrders.isNotEmpty()) {
                orderRepository.updateOrders(updatedOrders)
            }
            _snackbarMessage.emit("${updatedOrders.size}টি অর্ডারে '${product.name}' সফলভাবে অ্যাসাইন করা হয়েছে 👗")
        }
    }

    fun bulkAssignDress(orderIds: Set<Long>, product: CatalogProductEntity) {
        bulkAssignCatalogProduct(
            orderIds = orderIds,
            product = product,
            keepExistingSizes = true,
            overrideSize = null,
            overrideColor = null
        )
    }

    fun startBulkCourierSync(orders: List<OrderEntity>, apiKey: String, secretKey: String) {
        if (orders.isEmpty()) return

        if (apiKey.isBlank() || secretKey.isBlank()) {
            _bulkCourierSyncState.value = BulkCourierSyncState(
                isActive = true,
                isRunning = false,
                isFinished = true,
                totalOrders = orders.size,
                errorMessage = "Steadfast API Key ও Secret Key সেটিংসে দেওয়া নেই। অনুগ্রহ করে সেটিংস পেজে গিয়ে সঠিক কি প্রদান করুন।"
            )
            return
        }

        bulkSyncJob?.cancel()
        bulkSyncJob = viewModelScope.launch(Dispatchers.IO) {
            _bulkCourierSyncState.value = BulkCourierSyncState(
                isActive = true,
                isRunning = true,
                isFinished = false,
                totalOrders = orders.size,
                processedCount = 0
            )

            for (order in orders) {
                if (!isActive || !_bulkCourierSyncState.value.isRunning) {
                    break
                }

                // 1. Idempotency Guard: Check if valid consignment ID already exists
                val existingTrackingId = order.courierTrackingId.trim()
                val isAlreadySynced = existingTrackingId.isNotBlank() &&
                        !existingTrackingId.equals("PENDING", ignoreCase = true) &&
                        !existingTrackingId.equals("failed", ignoreCase = true) &&
                        !existingTrackingId.equals("none", ignoreCase = true)

                if (isAlreadySynced) {
                    _bulkCourierSyncState.update { current ->
                        current.copy(
                            processedCount = current.processedCount + 1,
                            alreadySyncedCount = current.alreadySyncedCount + 1,
                            reports = current.reports + BulkSyncItemReport(
                                invoice = order.invoiceNo,
                                customerName = order.customerName,
                                status = BulkSyncItemStatus.ALREADY_SYNCED,
                                reason = "পূর্বে এন্ট্রি ছিল (ID: $existingTrackingId)",
                                consignmentId = existingTrackingId
                            )
                        )
                    }
                    continue
                }

                // 2. Pre-Flight Validation: Check 11-digit phone number and address
                val digitsOnly = order.customerPhone.filter { it.isDigit() }
                val normalizedPhone = if (digitsOnly.startsWith("880") && digitsOnly.length == 13) {
                    digitsOnly.removePrefix("88")
                } else {
                    digitsOnly
                }
                val isValidPhone = normalizedPhone.length == 11 && normalizedPhone.startsWith("01")
                val isValidAddress = order.customerAddress.trim().isNotBlank()

                if (!isValidPhone || !isValidAddress) {
                    val failureReason = when {
                        !isValidPhone && !isValidAddress -> "১১ ডিজিটের মোবাইল নম্বর এবং ঠিকানা উভয়ই অনুপস্থিত"
                        !isValidPhone -> "ভুল বা অকার্যকর মোবাইল নম্বর (${order.customerPhone})"
                        else -> "কাস্টমার ঠিকানা ফাঁকা"
                    }
                    _bulkCourierSyncState.update { current ->
                        current.copy(
                            processedCount = current.processedCount + 1,
                            preflightSkippedCount = current.preflightSkippedCount + 1,
                            reports = current.reports + BulkSyncItemReport(
                                invoice = order.invoiceNo,
                                customerName = order.customerName,
                                status = BulkSyncItemStatus.INVALID_INFO,
                                reason = failureReason
                            )
                        )
                    }
                    continue
                }

                // 3. Update current processing order
                _bulkCourierSyncState.update { current ->
                    current.copy(
                        currentInvoice = order.invoiceNo,
                        currentCustomerName = order.customerName
                    )
                }

                // 4. Call Courier API with normalized phone (and immediate Room save inside bookSingleOrder)
                val result = courierRepository.bookSingleOrder(
                    order.copy(customerPhone = normalizedPhone),
                    apiKey,
                    secretKey
                )

                if (result.isSuccess) {
                    val consignmentId = result.getOrNull()
                    _bulkCourierSyncState.update { current ->
                        current.copy(
                            processedCount = current.processedCount + 1,
                            successCount = current.successCount + 1,
                            reports = current.reports + BulkSyncItemReport(
                                invoice = order.invoiceNo,
                                customerName = order.customerName,
                                status = BulkSyncItemStatus.SUCCESS,
                                reason = "সফলভাবে এন্ট্রি সম্পন্ন",
                                consignmentId = consignmentId
                            )
                        )
                    }
                } else {
                    val errMsg = result.exceptionOrNull()?.localizedMessage ?: "কুরিয়ার বুকিং ব্যর্থ হয়েছে"
                    _bulkCourierSyncState.update { current ->
                        current.copy(
                            processedCount = current.processedCount + 1,
                            apiFailedCount = current.apiFailedCount + 1,
                            reports = current.reports + BulkSyncItemReport(
                                invoice = order.invoiceNo,
                                customerName = order.customerName,
                                status = BulkSyncItemStatus.API_FAILED,
                                reason = errMsg
                            )
                        )
                    }
                }

                // 5. Sequential Throttled Queue: 150ms delay between API requests
                delay(150)
            }

            _bulkCourierSyncState.update { it.copy(isRunning = false, isFinished = true) }
        }
    }

    fun stopBulkCourierSync() {
        bulkSyncJob?.cancel()
        _bulkCourierSyncState.update { it.copy(isRunning = false, isFinished = true) }
    }

    fun dismissBulkSyncDialog() {
        stopBulkCourierSync()
        _bulkCourierSyncState.value = BulkCourierSyncState()
    }

    fun bulkRetryCourierSync(orders: List<OrderEntity>, apiKey: String, secretKey: String) {
        startBulkCourierSync(orders, apiKey, secretKey)
    }

    fun bulkMoveToTrash(orderIds: Set<Long>) {
        viewModelScope.launch {
            if (orderIds.isEmpty()) return@launch
            orderIds.forEach { id ->
                orderRepository.moveToTrash(id)
            }
            _snackbarMessage.emit("${orderIds.size}টি অর্ডার ট্র্যাশে পাঠানো হয়েছে 🗑️")
        }
    }

    fun restoreFromTrash(orderId: Long) {
        viewModelScope.launch {
            orderRepository.restoreFromTrash(orderId)
            _snackbarMessage.emit("অর্ডার রিস্টোর করা হয়েছে")
        }
    }

    fun deletePermanently(orderId: Long) {
        viewModelScope.launch {
            orderRepository.deletePermanently(orderId)
            _snackbarMessage.emit("অর্ডার স্থায়ীভাবে মুছে ফেলা হয়েছে")
        }
    }

    fun emptyTrashSafe(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTrashed = trashedOrders.value
                if (currentTrashed.isEmpty()) {
                    _snackbarMessage.emit("রিসাইকেল বিন ইতিমধ্যে খালি।")
                    return@launch
                }

                // Pre-purge verified safety snapshot
                val snapshotResult = ExportHelper.createSafetySnapshot(
                    context = context,
                    orders = currentTrashed,
                    catalogProducts = emptyList(),
                    tag = "trash_purge"
                )

                if (snapshotResult.isFailure) {
                    val reason = snapshotResult.exceptionOrNull()?.message ?: "স্ন্যাপশট তৈরি ব্যর্থ"
                    _snackbarMessage.emit("⚠️ ডাটা সুরক্ষা সতর্কতা: সেফটি ব্যাকআপ ব্যর্থ হওয়ায় ট্র্যাশ খালি করা বাতিল করা হয়েছে! ($reason)")
                    return@launch
                }

                orderRepository.emptyTrash()
                _snackbarMessage.emit("রিসাইকেল বিন খালি করা হয়েছে। ${currentTrashed.size}টি ড্রাফটের সেফটি ব্যাকআপ সংরক্ষিত রয়েছে 🗑️")
                onComplete()
            } catch (e: Exception) {
                _snackbarMessage.emit("বিন খালি করতে ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            orderRepository.emptyTrash()
            _snackbarMessage.emit("রিসাইকেল বিন সম্পূর্ণ খালি করা হয়েছে")
        }
    }

    suspend fun getCustomerHistory(phone: String): List<OrderEntity> {
        return orderRepository.getOrdersByPhone(phone)
    }

    fun createOrder(order: OrderEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            orderRepository.insertOrder(order)
            _snackbarMessage.emit("নতুন অর্ডার ${order.invoiceNo} তৈরি হয়েছে 🌸")
            onComplete()
        }
    }

    fun updateOrder(order: OrderEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            orderRepository.updateOrder(order)
            _snackbarMessage.emit("অর্ডার ${order.invoiceNo} আপডেট করা হয়েছে")
            onComplete()
        }
    }

    fun createOrderFromDraft(draft: ParsedOrderDraft, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val nextInvoiceNumber = (allOrders.value.size + 1506)
            val order = OrderEntity(
                invoiceNo = "INV-$nextInvoiceNumber",
                customerName = draft.customerName,
                customerPhone = draft.customerPhone,
                customerAddress = draft.customerAddress,
                district = draft.district,
                thana = draft.thana,
                orderType = draft.orderType,
                fulfillmentStatus = if (draft.orderType == OrderType.PRE_ORDER) FulfillmentStatus.PRE_ORDER_PENDING else FulfillmentStatus.NEW,
                items = draft.toOrderItems(),
                deliveryTier = draft.deliveryTier,
                deliveryCharge = draft.deliveryCharge,
                advanceAmount = draft.advanceAmount,
                discountAmount = 0.0
            )
            val id = orderRepository.insertOrder(order)
            _snackbarMessage.emit("স্মার্ট পেস্ট থেকে অর্ডার ${order.invoiceNo} তৈরি হয়েছে 🌸")
            onComplete(id)
        }
    }

    class Factory(
        private val orderRepository: OrderRepository,
        private val courierRepository: CourierRepository,
        private val catalogRepository: CatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderViewModel(orderRepository, courierRepository, catalogRepository) as T
        }
    }
}
