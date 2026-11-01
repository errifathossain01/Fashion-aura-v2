package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.CourierService
import com.example.data.model.DeliveryTier
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItem
import com.example.data.model.OrderType
import com.example.data.model.PaymentStatus
import com.example.data.model.Priority
import com.example.data.parser.SmartOrderParser
import com.example.util.BanglaFormatter
import com.example.util.PhotoStorageManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateMapOf
import com.example.data.model.CatalogProductEntity
import com.example.data.parser.FieldConfidence
import com.example.data.parser.ParsedOrderDraft
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditOrderScreen(
    initialOrder: OrderEntity? = null,
    parsedDraft: ParsedOrderDraft? = null,
    totalOrdersCount: Int,
    catalogProducts: List<CatalogProductEntity> = emptyList(),
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onNavigateBack: () -> Unit,
    onCheckCustomerHistory: suspend (String) -> List<OrderEntity>,
    onSaveOrder: (OrderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isEditMode = initialOrder != null && initialOrder.id > 0L
    var invoiceNo by remember { mutableStateOf(if (isEditMode) initialOrder!!.invoiceNo else "INV-${totalOrdersCount + 1506}") }
    var customerName by remember { mutableStateOf(initialOrder?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(initialOrder?.customerPhone ?: "") }
    var customerAddress by remember { mutableStateOf(initialOrder?.customerAddress ?: "") }
    var district by remember { mutableStateOf(initialOrder?.district ?: "Dhaka") }
    var thana by remember { mutableStateOf(initialOrder?.thana ?: "") }

    val isDistrictFromDraftUnconfirmed = parsedDraft != null &&
        parsedDraft.fieldConfidences["district"] != FieldConfidence.EXPLICIT
    var isDistrictConfirmedByUser by remember {
        mutableStateOf(!isDistrictFromDraftUnconfirmed)
    }

    val isPriceFromDraftEstimated = parsedDraft != null && !parsedDraft.isPriceExplicitInMessage
    var isPriceReviewedByUser by remember {
        mutableStateOf(!isPriceFromDraftEstimated)
    }

    var orderType by remember { mutableStateOf(initialOrder?.orderType ?: OrderType.REGULAR) }
    var priority by remember { mutableStateOf(initialOrder?.priority ?: Priority.NORMAL) }
    var paymentStatus by remember { mutableStateOf(initialOrder?.paymentStatus ?: PaymentStatus.UNPAID) }
    var fulfillmentStatus by remember {
        mutableStateOf(
            initialOrder?.fulfillmentStatus ?: if (orderType == OrderType.PRE_ORDER) FulfillmentStatus.PRE_ORDER_PENDING else FulfillmentStatus.NEW
        )
    }

    var deliveryTier by remember { mutableStateOf(initialOrder?.deliveryTier ?: DeliveryTier.DHAKA_INSIDE) }
    var deliveryCharge by remember { mutableDoubleStateOf(initialOrder?.deliveryCharge ?: 80.0) }
    var advanceAmount by remember { mutableDoubleStateOf(initialOrder?.advanceAmount ?: 0.0) }
    var discountAmount by remember { mutableDoubleStateOf(initialOrder?.discountAmount ?: 0.0) }
    var courierTrackingId by remember { mutableStateOf(initialOrder?.courierTrackingId ?: "") }
    var courierService by remember { mutableStateOf(initialOrder?.courierService ?: CourierService.STEADFAST) }
    val formatRiderNote: (Double) -> String = { fee ->
        val amountFormatted = if (language == AppLanguage.BANGLA) {
            val digits = if (useBanglaDigits) BanglaFormatter.toBanglaDigits(fee.toInt().toString()) else fee.toInt().toString()
            "$digits টাকা"
        } else {
            "${fee.toInt()} Tk"
        }
        if (language == AppLanguage.BANGLA) {
            "পার্সেল খুলে দেখে পছন্দ না হলে বা না নিলে ডেলিভারি চার্জ $amountFormatted রাইডারকে প্রদান করতে হবে।"
        } else {
            "If parcel is opened and not accepted, delivery charge of $amountFormatted must be paid to the rider."
        }
    }

    var riderNote by remember {
        mutableStateOf(
            if (!initialOrder?.riderNote.isNullOrBlank() &&
                !initialOrder!!.riderNote.contains("ডেলিভারি চার্জ") &&
                !initialOrder.riderNote.contains("delivery charge") &&
                !initialOrder.riderNote.contains("পার্সেল খুলে") &&
                !initialOrder.riderNote.contains("🛵")
            ) {
                initialOrder.riderNote
            } else {
                formatRiderNote(initialOrder?.deliveryCharge ?: deliveryCharge)
            }
        )
    }

    LaunchedEffect(deliveryCharge, language, useBanglaDigits) {
        val isStandardNote = riderNote.isBlank() ||
                riderNote.contains("পার্সেল খুলে") ||
                riderNote.contains("ডেলিভারি চার্জ") ||
                riderNote.contains("delivery charge") ||
                riderNote.contains("rider") ||
                riderNote.contains("🛵")
        if (isStandardNote) {
            riderNote = formatRiderNote(deliveryCharge)
        }
    }

    var isScheduled by remember { mutableStateOf(initialOrder?.isScheduled ?: (initialOrder?.orderType == OrderType.SCHEDULED)) }
    var requestedDeliveryDate by remember { mutableStateOf(initialOrder?.requestedDeliveryDate ?: "") }

    // Multi-dress items list
    val items = remember {
        mutableStateListOf<OrderItem>().apply {
            if (initialOrder?.items?.isNotEmpty() == true) {
                addAll(initialOrder.items)
            } else {
                add(OrderItem(name = "কটন সালোয়ার কামিজ", size = "42", color = "", quantity = 1, unitPrice = 1500.0))
            }
        }
    }

    // Repeat customer history detector & 48h duplicate guard
    var customerHistoryCount by remember { mutableStateOf(0) }
    var customerHasReturnedOrders by remember { mutableStateOf(false) }
    var duplicateActiveOrder by remember { mutableStateOf<OrderEntity?>(null) }

    LaunchedEffect(customerPhone) {
        val clean = SmartOrderParser.normalizeDigits(customerPhone).trim()
        if (clean.length >= 11) {
            val history = onCheckCustomerHistory(clean)
            customerHistoryCount = history.size
            customerHasReturnedOrders = history.any { it.fulfillmentStatus == FulfillmentStatus.RETURNED }
            val fortyEightHoursAgo = System.currentTimeMillis() - 48 * 60 * 60 * 1000L
            duplicateActiveOrder = history.firstOrNull { o ->
                o.id != (initialOrder?.id ?: 0L) &&
                o.createdAt >= fortyEightHoursAgo &&
                (o.fulfillmentStatus == FulfillmentStatus.NEW ||
                 o.fulfillmentStatus == FulfillmentStatus.PROCESSING ||
                 o.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP)
            }
        } else {
            customerHistoryCount = 0
            customerHasReturnedOrders = false
            duplicateActiveOrder = null
        }
    }


    // Photo picker launcher for items
    var itemIndexForPhoto by remember { mutableStateOf(-1) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && itemIndexForPhoto in 0 until items.size) {
            scope.launch {
                val savedUri = PhotoStorageManager.savePhotoToInternalStorage(context, uri)
                if (savedUri != null) {
                    val current = items[itemIndexForPhoto]
                    items[itemIndexForPhoto] = current.copy(photoUri = savedUri)
                }
            }
        }
    }

    // Catalog bottom sheet state
    var showCatalogBottomSheet by remember { mutableStateOf(false) }
    var selectedItemIndexForCatalog by remember { mutableStateOf(-1) }
    var catalogSearchQuery by remember { mutableStateOf("") }
    val selectedCatalogForItems = remember { mutableStateMapOf<Int, CatalogProductEntity>() }

    // Subtotal and COD
    val subtotal = items.sumOf { it.unitPrice * it.quantity }
    val totalBill = (subtotal + deliveryCharge - discountAmount).coerceAtLeast(0.0)
    val codAmount = if (paymentStatus == PaymentStatus.PAID) 0.0 else (totalBill - advanceAmount).coerceAtLeast(0.0)

    val isPhoneValid = customerPhone.length == 11 && (customerPhone.startsWith("013") ||
            customerPhone.startsWith("014") || customerPhone.startsWith("015") ||
            customerPhone.startsWith("016") || customerPhone.startsWith("017") ||
            customerPhone.startsWith("018") || customerPhone.startsWith("019"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) {
                            if (language == AppLanguage.ENGLISH) "Edit Order (${initialOrder?.invoiceNo})" else "অর্ডার এডিট (${initialOrder?.invoiceNo})"
                        } else {
                            if (language == AppLanguage.ENGLISH) "New Order Entry" else "নতুন অর্ডার এন্ট্রি"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Repeat customer alert badge
            if (customerHistoryCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (customerHasReturnedOrders) Color(0xFFFFE4E6) else Color(0xFFDCFCE7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (customerHasReturnedOrders) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (customerHasReturnedOrders) Color(0xFFBE123C) else Color(0xFF15803D),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (customerHasReturnedOrders) {
                                    if (language == AppLanguage.ENGLISH) "Return Risk Alert (Previous return record exists)" else "রিটার্ন রিস্ক অ্যালার্ট (পূর্ববর্তী রিটার্ন রেকর্ড রয়েছে)"
                                } else {
                                    if (language == AppLanguage.ENGLISH) "Repeat Customer ($customerHistoryCount previous orders)" else "রিপিট কাস্টমার (${customerHistoryCount}টি পূর্ববর্তী অর্ডার)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (customerHasReturnedOrders) Color(0xFFBE123C) else Color(0xFF15803D)
                            )
                            Text(
                                text = if (language == AppLanguage.ENGLISH) {
                                    "Found $customerHistoryCount previous order(s) for this phone number."
                                } else {
                                    "এই মোবাইল নম্বরে ইতিপূর্বে ${customerHistoryCount} টি অর্ডার পাওয়া গেছে।"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 48-HOUR DUPLICATE ACTIVE ORDER WARNING BANNER (Requirement 3)
            if (duplicateActiveOrder != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.ENGLISH) {
                                    "Warning: Active order (#${duplicateActiveOrder?.invoiceNo}) already exists for this number!"
                                } else {
                                    "সতর্কতা: এই নম্বরে ইতিমধ্যে একটি সক্রিয় অর্ডার (#${duplicateActiveOrder?.invoiceNo}) রয়েছে!"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = if (language == AppLanguage.ENGLISH) {
                                    "Active order placed within last 48 hours (${BanglaFormatter.formatDate(duplicateActiveOrder?.createdAt ?: 0L, useBanglaDigits)}). Verify if duplicate."
                                } else {
                                    "গত ৪৮ ঘণ্টার মধ্যে (${BanglaFormatter.formatDate(duplicateActiveOrder?.createdAt ?: 0L, useBanglaDigits)}) অর্ডারটি সক্রিয় রয়েছে। ডুপ্লিকেট অর্ডার কি না যাচাই করুন।"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // SECTION 1: CUSTOMER DETAILS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "Customer Details & Delivery Address" else "গ্রাহকের তথ্য ও ডেলিভারি ঠিকানা",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(if (language == AppLanguage.ENGLISH) "Customer Name" else "গ্রাহকের নাম") },
                        placeholder = { Text(if (language == AppLanguage.ENGLISH) "Enter name" else "নাম লিখুন") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = {
                            customerPhone = SmartOrderParser.normalizeDigits(it).filter { ch -> ch.isDigit() }
                        },
                        label = { Text(if (language == AppLanguage.ENGLISH) "Mobile Number (11 digits)" else "মোবাইল নম্বর (১১ ডিজিট)") },
                        placeholder = { Text("017XXXXXXXX") },
                        supportingText = {
                            if (customerPhone.isNotEmpty() && !isPhoneValid) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) "Enter valid 11-digit Bangladesh phone number (013/014/015/016/017/018/019...)" else "১১ ডিজিট সঠিক বাংলাদেশি নম্বর দিন (যেমন: 013/017/018/019...)",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = customerPhone.isNotEmpty() && !isPhoneValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text(if (language == AppLanguage.ENGLISH) "Full Address (Road, House, Area)" else "পূর্ণ ঠিকানা (রোড, বাসা, এলাকা)") },
                        placeholder = { Text(if (language == AppLanguage.ENGLISH) "Enter delivery address" else "ডেলিভারি ঠিকানা লিখুন") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = district,
                            onValueChange = {
                                district = it
                                isDistrictConfirmedByUser = true
                            },
                            label = { Text(if (language == AppLanguage.ENGLISH) "District" else "জেলা") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = thana,
                            onValueChange = { thana = it },
                            label = { Text(if (language == AppLanguage.ENGLISH) "Thana / Upazila" else "থানা / উপজেলা") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (!isDistrictConfirmedByUser) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.ENGLISH)
                                            "District unconfirmed in message (defaulted to $district). Please confirm or edit."
                                        else
                                            "মেসেজে জেলা ছিল না (ডিফল্ট $district)। দয়া করে যাচাই বা সংশোধন করুন।",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { isDistrictConfirmedByUser = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.ENGLISH) "Confirm" else "ঠিক আছে",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: 5-DIMENSION BOUTIQUE FILTERS & STATUS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "Order Type & Priority" else "অর্ডার টাইপ ও প্রায়োরিটি",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Order Type Chips
                    Text(if (language == AppLanguage.ENGLISH) "Order Type:" else "অর্ডার ধরণ:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        OrderType.values().forEach { type ->
                            val selected = orderType == type
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        orderType = type
                                        if (type == OrderType.PRE_ORDER) {
                                            fulfillmentStatus = FulfillmentStatus.PRE_ORDER_PENDING
                                        } else if (type == OrderType.EXCHANGE) {
                                            val exchangePrefix = if (language == AppLanguage.ENGLISH) "[Exchange Parcel: Collect old product and deliver new parcel]" else "[এক্সচেঞ্জ পার্সেল: পুরাতন প্রোডাক্ট সংগ্রহ করে নতুন পার্সেল দিন]"
                                            if (!riderNote.contains("Exchange") && !riderNote.contains("এক্সচেঞ্জ")) {
                                                riderNote = "$exchangePrefix $riderNote".trim()
                                            }
                                        } else if (type == OrderType.SCHEDULED) {
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val picked = String.format("%04d-%02d-%02d", y, m + 1, d)
                                                    requestedDeliveryDate = picked
                                                    isScheduled = true
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                    }
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) type.displayNameEn else type.displayNameBn,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (orderType == OrderType.SCHEDULED || isScheduled || requestedDeliveryDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val picked = String.format("%04d-%02d-%02d", y, m + 1, d)
                                            requestedDeliveryDate = picked
                                            isScheduled = true
                                            orderType = OrderType.SCHEDULED
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (requestedDeliveryDate.isNotBlank()) {
                                        if (language == AppLanguage.ENGLISH) "Scheduled Delivery Date: $requestedDeliveryDate (Tap to change)" else "সিডিউল ডেলিভারি তারিখ: $requestedDeliveryDate (পরিবর্তন করতে ট্যাপ করুন)"
                                    } else {
                                        if (language == AppLanguage.ENGLISH) "Select Delivery Date (Tap here)" else "তারিখ সিলেক্ট করুন (ট্যাপ করুন)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Priority Chips
                    Text(if (language == AppLanguage.ENGLISH) "Priority:" else "প্রায়োরিটি:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Priority.values().forEach { p ->
                            val selected = priority == p
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { priority = p }
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) p.displayNameEn else p.displayNameBn,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color(0xFFBE123C) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Payment Status Chips
                    Text(if (language == AppLanguage.ENGLISH) "Payment Status:" else "পেমেন্ট স্ট্যাটাস:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        PaymentStatus.values().forEach { ps ->
                            val selected = paymentStatus == ps
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { paymentStatus = ps }
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) ps.displayNameEn else ps.displayNameBn,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 3: ITEMS (Dresses, Sizes, Photo Picker)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "Dresses & Sizes (${items.size} items)" else "ড্রেস ও সাইজ (${items.size}টি আইটেম)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = {
                                items.add(OrderItem(name = if (language == AppLanguage.ENGLISH) "New Dress" else "নতুন ড্রেস", size = "40", quantity = 1, unitPrice = 1200.0))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == AppLanguage.ENGLISH) "Add Dress" else "ড্রেস যোগ করুন")
                        }
                    }

                    items.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Dress Photo / Picker
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                itemIndexForPhoto = index
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.photoUri != null) {
                                            AsyncImage(
                                                model = item.photoUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                                contentDescription = "Pick Photo",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = item.name,
                                            onValueChange = { items[index] = item.copy(name = it) },
                                            label = { Text(if (language == AppLanguage.ENGLISH) "Dress Name" else "ড্রেসের নাম") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (items.size > 1) {
                                        IconButton(onClick = { items.removeAt(index) }) {
                                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                // Prominent Catalog Picker Button (Cleaned up, no emoji)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedItemIndexForCatalog = index
                                            catalogSearchQuery = ""
                                            showCatalogBottomSheet = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == AppLanguage.ENGLISH) "Select from Catalog" else "ক্যাটালগ থেকে নিন",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (item.photoUri != null) {
                                        TextButton(onClick = { items[index] = item.copy(photoUri = null) }) {
                                            Text(if (language == AppLanguage.ENGLISH) "Remove Photo" else "ছবি মুছুন", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Quick Size Selection Chips
                                val catSizes = selectedCatalogForItems[index]?.availableSizes ?: emptyList()
                                val defaultSizes = listOf("34", "36", "38", "40", "42", "44", "46", "48", "Free", "XS", "S", "M", "L", "XL", "XXL", "3XL")
                                val displaySizes = (catSizes + defaultSizes).distinct()

                                Text(
                                    text = if (catSizes.isNotEmpty()) {
                                        if (language == AppLanguage.ENGLISH) "Select Catalog Size:" else "ক্যাটালগ সাইজ সিলেক্ট করুন:"
                                    } else {
                                        if (language == AppLanguage.ENGLISH) "Select Size:" else "সাইজ সিলেক্ট করুন:"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (catSizes.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (catSizes.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                    displaySizes.forEach { sz ->
                                        val isSzSelected = item.size == sz
                                        val isCatSize = catSizes.contains(sz)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSzSelected) MaterialTheme.colorScheme.primary else if (isCatSize) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                            border = if (isCatSize && !isSzSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { items[index] = item.copy(size = sz) }
                                        ) {
                                            Text(
                                                text = sz,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSzSelected || isCatSize) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSzSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Color input and quick suggestions
                                OutlinedTextField(
                                    value = item.color,
                                    onValueChange = { items[index] = item.copy(color = it) },
                                    label = { Text(if (language == AppLanguage.ENGLISH) "Color" else "কালার") },
                                    placeholder = { Text(if (language == AppLanguage.ENGLISH) "Enter dress color" else "ড্রেসের কালার লিখুন") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val catColors = selectedCatalogForItems[index]?.availableColors ?: emptyList()
                                val defaultColors = listOf("pista", "মেরুন", "কালো", "লাল", "হলুদ", "নীল", "ল্যাভেন্ডার", "বেগুনি", "সাদা")
                                val displayColors = (catColors + defaultColors).distinct()

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    displayColors.forEach { qc ->
                                        val isColorSelected = item.color.equals(qc, ignoreCase = true)
                                        val isCatColor = catColors.contains(qc)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isColorSelected) MaterialTheme.colorScheme.primaryContainer else if (isCatColor) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    val cat = selectedCatalogForItems[index]
                                                    val variantPhoto = cat?.variantPhotos?.get(qc)
                                                    val photoToUse = if (!variantPhoto.isNullOrBlank()) variantPhoto else (item.photoUri ?: cat?.photoUri)
                                                    items[index] = item.copy(color = qc, photoUri = photoToUse)
                                                }
                                        ) {
                                            Text(
                                                text = qc,
                                                fontSize = 10.sp,
                                                fontWeight = if (isCatColor || isColorSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isColorSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AutoSelectNumberField(
                                        value = item.quantity.toString(),
                                        onValueChange = {
                                            val q = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 1
                                            items[index] = item.copy(quantity = q)
                                        },
                                        label = if (language == AppLanguage.ENGLISH) "Quantity (Qty)" else "পরিমাণ (Qty)",
                                        modifier = Modifier.weight(1f)
                                    )

                                    AutoSelectNumberField(
                                        value = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString(),
                                        onValueChange = {
                                            val pr = it.filter { ch -> ch.isDigit() || ch == '.' }.toDoubleOrNull() ?: 0.0
                                            items[index] = item.copy(unitPrice = pr)
                                            isPriceReviewedByUser = true
                                        },
                                        label = if (!isPriceReviewedByUser) {
                                            if (language == AppLanguage.ENGLISH) "Price (Est. ৳ - Review)" else "মূল্য (আনুমানিক ৳ - যাচাই)"
                                        } else {
                                            if (language == AppLanguage.ENGLISH) "Price (৳)" else "মূল্য (৳)"
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (!isPriceReviewedByUser) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFFBEB),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD97706),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (language == AppLanguage.ENGLISH)
                                                        "Price was not in message (defaulted to ৳${item.unitPrice.toInt()}). Please review."
                                                    else
                                                        "মেসেজে মূল্য ছিল না (ডিফল্ট ৳${item.unitPrice.toInt()})। যাচাই বা কনফার্ম করুন।",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            OutlinedButton(
                                                onClick = { isPriceReviewedByUser = true },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (language == AppLanguage.ENGLISH) "Confirm Price" else "মূল্য কনফার্ম",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 4: DELIVERY TIER & CHARGES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "Delivery Charge & Advance Payment" else "ডেলিভারি চার্জ ও অগ্রিম টাকা",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Delivery Tier Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeliveryTier.values().forEach { tier ->
                            val isTierSelected = deliveryTier == tier
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isTierSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        deliveryTier = tier
                                        deliveryCharge = tier.defaultFee
                                        riderNote = formatRiderNote(tier.defaultFee)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.ENGLISH) tier.displayNameEn else tier.displayNameBn,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTierSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = BanglaFormatter.formatCurrency(tier.defaultFee, useBanglaDigits),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isTierSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Advance Payment Manual Input & Quick Chips (Cleaned up placeholders)
                    Text(if (language == AppLanguage.ENGLISH) "Advance Payment (৳):" else "অগ্রিম টাকা (৳):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    var manualAdvanceInput by remember(advanceAmount) {
                        mutableStateOf(if (advanceAmount > 0.0) advanceAmount.toInt().toString() else "")
                    }

                    AutoSelectNumberField(
                        value = manualAdvanceInput,
                        onValueChange = { input ->
                            manualAdvanceInput = input
                            val parsedAdv = input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                            advanceAmount = parsedAdv
                        },
                        label = if (language == AppLanguage.ENGLISH) "Advance Amount (৳)" else "অগ্রিম এমাউন্ট (৳)",
                        placeholder = if (language == AppLanguage.ENGLISH) "Enter amount" else "এমাউন্ট লিখুন",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 6.dp)
                    )

                    val advanceChips = listOf(0.0, 100.0, 200.0, 500.0, 1000.0)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        advanceChips.forEach { adv ->
                            val isAdvSelected = advanceAmount == adv
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAdvSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        advanceAmount = adv
                                        manualAdvanceInput = if (adv == 0.0) "" else adv.toInt().toString()
                                    }
                            ) {
                                Text(
                                    text = BanglaFormatter.formatCurrency(adv, useBanglaDigits),
                                    fontSize = 12.sp,
                                    fontWeight = if (isAdvSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAdvSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = courierTrackingId,
                        onValueChange = { courierTrackingId = it },
                        label = { Text(if (language == AppLanguage.ENGLISH) "Consignment ID" else "কনসাইনমেন্ট আইডি") },
                        placeholder = { Text("Consignment ID") },
                        supportingText = { Text(if (language == AppLanguage.ENGLISH) "Auto-sets status to Ready to Ship" else "আইডি লিখলে স্বয়ংক্রিয়ভাবে রেডি টু শিপ হয়ে যাবে") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = riderNote,
                        onValueChange = { riderNote = it },
                        label = { Text(if (language == AppLanguage.ENGLISH) "Rider Return Note" else "রাইডার রিটার্ন নোট") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BILL BREAKDOWN SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "Bill Breakdown & Summary" else "হিসাব ও বিল সারাংশ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (language == AppLanguage.ENGLISH) "Items Subtotal:" else "ড্রেস সাবটোটাল:")
                        Text(BanglaFormatter.formatCurrency(subtotal, useBanglaDigits), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (language == AppLanguage.ENGLISH) "Delivery Charge:" else "ডেলিভারি চার্জ:")
                        Text(BanglaFormatter.formatCurrency(deliveryCharge, useBanglaDigits), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (language == AppLanguage.ENGLISH) "Advance Paid:" else "অগ্রিম পরিশোধিত:")
                        Text("- ${BanglaFormatter.formatCurrency(advanceAmount, useBanglaDigits)}", color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (language == AppLanguage.ENGLISH) "Total Bill:" else "মোট বিল:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(BanglaFormatter.formatCurrency(totalBill, useBanglaDigits), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (language == AppLanguage.ENGLISH) "Cash on Delivery (COD):" else "ক্যাশ অন ডেলিভারি (COD):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(BanglaFormatter.formatCurrency(codAmount, useBanglaDigits), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SAVE BUTTON
            Button(
                onClick = {
                    val status = if (courierTrackingId.isNotBlank()) {
                        FulfillmentStatus.READY_TO_SHIP
                    } else if (orderType == OrderType.PRE_ORDER) {
                        FulfillmentStatus.PRE_ORDER_PENDING
                    } else {
                        fulfillmentStatus
                    }

                    val updatedOrder = OrderEntity(
                        id = initialOrder?.id ?: 0L,
                        invoiceNo = invoiceNo,
                        customerName = customerName.ifBlank { if (language == AppLanguage.ENGLISH) "Valued Customer" else "সম্মানিত গ্রাহক" },
                        customerPhone = customerPhone,
                        customerAddress = customerAddress,
                        district = district,
                        thana = thana,
                        orderType = orderType,
                        priority = priority,
                        paymentStatus = paymentStatus,
                        fulfillmentStatus = status,
                        items = items.toList(),
                        deliveryTier = deliveryTier,
                        deliveryCharge = deliveryCharge,
                        advanceAmount = advanceAmount,
                        discountAmount = discountAmount,
                        isScheduled = isScheduled || orderType == OrderType.SCHEDULED,
                        requestedDeliveryDate = requestedDeliveryDate,
                        courierService = courierService,
                        courierTrackingId = courierTrackingId,
                        riderNote = if (orderType == OrderType.EXCHANGE && !riderNote.contains("Exchange") && !riderNote.contains("এক্সচেঞ্জ")) {
                            val exchangePrefix = if (language == AppLanguage.ENGLISH) "[Exchange Parcel: Collect old product and deliver new parcel]" else "[এক্সচেঞ্জ পার্সেল: পুরাতন প্রোডাক্ট সংগ্রহ করে নতুন পার্সেল দিন]"
                            "$exchangePrefix $riderNote".trim()
                        } else riderNote,
                        createdAt = initialOrder?.createdAt ?: System.currentTimeMillis()
                    )
                    onSaveOrder(updatedOrder)
                },
                enabled = isPhoneValid && isDistrictConfirmedByUser && isPriceReviewedByUser,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) {
                        if (language == AppLanguage.ENGLISH) "Update Order" else "অর্ডার আপডেট করুন"
                    } else {
                        if (language == AppLanguage.ENGLISH) "Confirm Order" else "অর্ডার কনফার্ম করুন"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isDistrictConfirmedByUser) {
                Text(
                    text = if (language == AppLanguage.ENGLISH)
                        "Please confirm or edit the district before saving."
                    else
                        "অর্ডার সেভ করার পূর্বে জেলা কনফার্ম বা সংশোধন করুন।",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            } else if (!isPriceReviewedByUser) {
                Text(
                    text = if (language == AppLanguage.ENGLISH)
                        "Please review item price before saving."
                    else
                        "অর্ডার সেভ করার পূর্বে পণ্যের মূল্য যাচাই বা কনফার্ম করুন।",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }
    }

    if (showCatalogBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCatalogBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.ENGLISH) "Select Dress from Catalog" else "ক্যাটালগ থেকে ড্রেস নির্বাচন করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = catalogSearchQuery,
                    onValueChange = { catalogSearchQuery = it },
                    placeholder = { Text(if (language == AppLanguage.ENGLISH) "Search dress name or category..." else "ড্রেসের নাম বা ক্যাটাগরি খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filteredCatalog = catalogProducts.filter {
                    it.name.contains(catalogSearchQuery, ignoreCase = true) ||
                    it.category.contains(catalogSearchQuery, ignoreCase = true)
                }

                if (filteredCatalog.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "No dress found" else "কোনো ড্রেস পাওয়া যায়নি",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCatalog, key = { it.id }) { cat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val idx = selectedItemIndexForCatalog
                                        if (idx in 0 until items.size) {
                                            val cur = items[idx]
                                            selectedCatalogForItems[idx] = cat
                                            items[idx] = cur.copy(
                                                name = cat.name,
                                                photoUri = cat.photoUri,
                                                unitPrice = if (cur.unitPrice <= 0.0 || cur.name == "নতুন ড্রেস" || cur.name == "কটন সালোয়ার কামিজ") cat.basePrice else cur.unitPrice,
                                                size = if (cur.size.isNotBlank()) cur.size else (cat.availableSizes.firstOrNull() ?: ""),
                                                color = if (cur.color.isNotBlank()) cur.color else (cat.availableColors.firstOrNull() ?: "")
                                            )
                                        }
                                        showCatalogBottomSheet = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        if (cat.photoUri.isNotBlank()) {
                                            AsyncImage(
                                                model = cat.photoUri,
                                                contentDescription = cat.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(24.dp).align(Alignment.Center)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = cat.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = BanglaFormatter.formatCurrency(cat.basePrice, useBanglaDigits),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (cat.stockStatus.contains("IN_STOCK") || cat.stockStatus.contains("In Stock")) Color(0xFFDCFCE7) else Color(0xFFEDE9FE)
                                    ) {
                                        Text(
                                            text = if (cat.stockStatus.contains("IN_STOCK") || cat.stockStatus.contains("In Stock")) {
                                                if (language == AppLanguage.ENGLISH) "In Stock" else "ইন স্টক"
                                            } else {
                                                if (language == AppLanguage.ENGLISH) "Pre-Order" else "প্রি-অর্ডার"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cat.stockStatus.contains("IN_STOCK") || cat.stockStatus.contains("In Stock")) Color(0xFF15803D) else Color(0xFF6B46C1),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutoSelectNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Number
) {
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    var isFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            onValueChange(newValue.text)
        },
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) { { Text(placeholder) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused && !isFocused) {
                // When focused/tapped, auto-select all characters!
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(0, textFieldValue.text.length)
                )
            }
            isFocused = focusState.isFocused
        }
    )
}
