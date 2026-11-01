package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.ui.components.ConsignmentIdDialog
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.ParcelMarkerSlipCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.auraTokens
import com.example.util.BanglaFormatter
import com.example.util.MessagingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: OrderEntity,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onNavigateBack: () -> Unit,
    onEditOrder: () -> Unit,
    onMarkAsShipped: () -> Unit,
    onStockArrived: () -> Unit,
    onSaveConsignmentId: (String) -> Unit,
    onBookSteadfastApi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark
    val context = LocalContext.current
    var showConsignmentDialog by remember { mutableStateOf(false) }
    var selectedZoomPhoto by remember { mutableStateOf<String?>(null) }

    // Packing checklist state
    var checkedDress by remember { mutableStateOf(true) }
    var checkedSize by remember { mutableStateOf(true) }
    var checkedPolyMail by remember { mutableStateOf(false) }
    var checkedCodSlip by remember { mutableStateOf(false) }

    if (selectedZoomPhoto != null) {
        FullScreenPhotoDialog(
            photoUri = selectedZoomPhoto,
            title = order.items.firstOrNull()?.name ?: order.invoiceNo,
            onDismiss = { selectedZoomPhoto = null }
        )
    }

    if (showConsignmentDialog) {
        ConsignmentIdDialog(
            order = order,
            onDismiss = { showConsignmentDialog = false },
            onSaveConsignmentId = { id ->
                onSaveConsignmentId(id)
                showConsignmentDialog = false
            },
            onBookSteadfastApi = {
                onBookSteadfastApi()
                showConsignmentDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "অর্ডার ডিটেইলস ও প্যাকেজিং",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = order.invoiceNo,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = tokens.textSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditOrder) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = tokens.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(if (isDark) tokens.backgroundMain else MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Status & Action Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(status = order.fulfillmentStatus, language = language)

                        if (order.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP) {
                            Button(
                                onClick = onMarkAsShipped,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isDark) tokens.onPrimaryAction else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🚚 শিপ করুন", fontWeight = FontWeight.Bold)
                            }
                        } else if (order.fulfillmentStatus == FulfillmentStatus.PRE_ORDER_PENDING) {
                            Button(
                                onClick = onStockArrived,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.secondary,
                                    contentColor = if (isDark) tokens.onPrimaryAction else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("✨ স্টক এসেছে", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1-Tap Customer Messaging Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { MessagingHelper.dialCustomer(context, order.customerPhone) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কল", color = tokens.textPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                val msg = MessagingHelper.generateCustomerConfirmationMessage(order, useBanglaDigits)
                                MessagingHelper.openWhatsApp(context, order.customerPhone, msg)
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", color = tokens.textPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                val msg = MessagingHelper.generateCustomerConfirmationMessage(order, useBanglaDigits)
                                MessagingHelper.sendSms(context, order.customerPhone, msg)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                Icons.Default.Message,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", color = tokens.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PARCEL MARKER SLIP CARD (For handwriting on poly mailer bags)
            ParcelMarkerSlipCard(
                order = order,
                useBanglaDigits = useBanglaDigits
            )

            Spacer(modifier = Modifier.height(14.dp))

            // COURIER & CONSIGNMENT MANAGEMENT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "কুরিয়ার ও কনসাইনমেন্ট আইডি",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "কুরিয়ার: ${order.courierService.displayName}",
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = if (order.courierTrackingId.isNotBlank()) "Consignment ID: ${order.courierTrackingId}" else "কোনো আইডি যুক্ত করা হয়নি",
                                color = if (order.courierTrackingId.isNotBlank()) (if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)) else tokens.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { showConsignmentDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary,
                                contentColor = if (isDark) tokens.onPrimaryAction else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (order.courierTrackingId.isBlank()) "+ ID লিখুন" else "এডিট ID",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (order.courierSyncStatus == com.example.data.model.CourierSyncStatus.PENDING_SYNC) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF450A0A) else Color(0xFFFFE4E6),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFDA4AF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFBE123C),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⚠️ কুরিয়ার API থেকে আইডি পাওয়া যায়নি (পেন্ডিং সিঙ্ক)",
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFBE123C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PACKAGING CHECKLIST CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "প্যাকিং ভেরিফিকেশন চেকলিস্ট (পার্সেল রেডি করার ধাপ)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ChecklistItem(title = "ড্রেসের কোয়ালিটি ও ত্রুটি চেক করা হয়েছে", checked = checkedDress, onCheckedChange = { checkedDress = it })
                    ChecklistItem(title = "সাইজ লেবেল (${order.items.firstOrNull()?.size ?: "40"}) সঠিক আছে কিনা মিলিয়ে দেখা হয়েছে", checked = checkedSize, onCheckedChange = { checkedSize = it })
                    ChecklistItem(title = "প্যাকেটের গায়ে মার্কার দিয়ে Consignment ID ও Invoice লেখা হয়েছে", checked = checkedPolyMail, onCheckedChange = { checkedPolyMail = it })
                    ChecklistItem(title = "সঠিক COD অ্যামাউন্ট (${BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)}) নিশ্চিত করা হয়েছে", checked = checkedCodSlip, onCheckedChange = { checkedCodSlip = it })
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DRESS ITEMS LIST WITH FULL SCREEN ZOOM
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "অর্ডারের ড্রেস তালিকা (${order.items.size}টি আইটেম)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (item.photoUri != null) selectedZoomPhoto = item.photoUri
                                    }
                            ) {
                                if (item.photoUri != null) {
                                    AsyncImage(
                                        model = item.photoUri,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = "সাইজ: ${item.size} • কালার: ${item.color.ifBlank { "ডিফল্ট" }}",
                                    fontSize = 13.sp,
                                    color = tokens.textSecondary
                                )
                                Text(
                                    text = "পরিমাণ: ${item.quantity}টি • একক মূল্য: ${BanglaFormatter.formatCurrency(item.unitPrice, useBanglaDigits)}",
                                    fontSize = 12.sp,
                                    color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider(color = if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // FINANCIAL SUMMARY CARD (Subtotal, Delivery Fee, Discount, Total Bill, Advance, COD Due)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বিল ও পেমেন্ট বিবরণী",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                        )
                        val paymentBg = when (order.paymentStatus) {
                            com.example.data.model.PaymentStatus.PAID -> if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7)
                            com.example.data.model.PaymentStatus.PARTIAL -> if (isDark) Color(0xFF2D1D02) else Color(0xFFFEF3C7)
                            com.example.data.model.PaymentStatus.UNPAID -> if (isDark) Color(0xFF450A0A) else Color(0xFFFFE4E6)
                        }
                        val paymentFg = when (order.paymentStatus) {
                            com.example.data.model.PaymentStatus.PAID -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            com.example.data.model.PaymentStatus.PARTIAL -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                            com.example.data.model.PaymentStatus.UNPAID -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFBE123C)
                        }
                        val paymentBorder = when (order.paymentStatus) {
                            com.example.data.model.PaymentStatus.PAID -> if (isDark) Color(0xFF14532D) else Color.Transparent
                            com.example.data.model.PaymentStatus.PARTIAL -> if (isDark) Color(0xFF78350F) else Color.Transparent
                            com.example.data.model.PaymentStatus.UNPAID -> if (isDark) Color(0xFF991B1B) else Color.Transparent
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = paymentBg,
                            border = if (isDark) BorderStroke(1.dp, paymentBorder) else null
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) order.paymentStatus.displayNameBn else order.paymentStatus.displayNameEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = paymentFg,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ড্রেস সাবটোটাল (${order.items.sumOf { it.quantity }}টি):", color = tokens.textSecondary, fontSize = 13.sp)
                        Text(BanglaFormatter.formatCurrency(order.subtotal, useBanglaDigits), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = tokens.textPrimary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ডেলিভারি চার্জ (${order.deliveryTier.displayNameBn}):", color = tokens.textSecondary, fontSize = 13.sp)
                        Text("+ ${BanglaFormatter.formatCurrency(order.deliveryCharge, useBanglaDigits)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = tokens.textPrimary)
                    }

                    if (order.discountAmount > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ডিসকাউন্ট:", color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontSize = 13.sp)
                            Text("- ${BanglaFormatter.formatCurrency(order.discountAmount, useBanglaDigits)}", color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("সর্বমোট বিল (Total Bill):", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = tokens.textPrimary)
                        Text(BanglaFormatter.formatCurrency(order.totalBill, useBanglaDigits), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary)
                    }

                    if (order.advanceAmount > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("অগ্রিম পরিশোধ (Advance Paid):", color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("- ${BanglaFormatter.formatCurrency(order.advanceAmount, useBanglaDigits)}", color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF2D1D02) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = if (isDark) BorderStroke(1.dp, Color(0xFF78350F)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ক্যাশ অন ডেলিভারি (COD Due):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isDark) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                checkmarkColor = if (isDark) Color.Black else Color.White,
                uncheckedColor = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textPrimary
        )
    }
}
