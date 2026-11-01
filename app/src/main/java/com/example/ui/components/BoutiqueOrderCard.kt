package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import com.example.data.model.CourierSyncStatus
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderType
import com.example.data.model.PaymentStatus
import com.example.data.model.Priority
import com.example.ui.theme.AppStrings
import com.example.util.BanglaFormatter
import com.example.util.MessagingHelper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import com.example.ui.theme.auraTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoutiqueOrderCard(
    order: OrderEntity,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onCardClick: () -> Unit,
    onMarkAsShipped: () -> Unit,
    onStockArrived: () -> Unit,
    onBookCourier: () -> Unit,
    onEditOrder: () -> Unit,
    onDeleteOrder: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark
    var showMenu by remember { mutableStateOf(false) }
    var selectedZoomPhoto by remember { mutableStateOf<String?>(null) }
    var showAllItemsDialog by remember { mutableStateOf(false) }

    val firstItem = remember(order.id, order.updatedAt) { order.items.firstOrNull() }
    val totalAmountFormatted = remember(order.id, order.updatedAt, useBanglaDigits) {
        BanglaFormatter.formatCurrency(order.totalBill, useBanglaDigits)
    }
    val codAmountFormatted = remember(order.id, order.updatedAt, useBanglaDigits) {
        BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)
    }
    val dateFormatted = remember(order.id, order.updatedAt, useBanglaDigits) {
        BanglaFormatter.formatDate(order.createdAt, useBanglaDigits)
    }

    if (showAllItemsDialog) {
        OrderItemsGalleryDialog(
            order = order,
            onDismiss = { showAllItemsDialog = false }
        )
    }

    if (selectedZoomPhoto != null) {
        FullScreenPhotoDialog(
            photoUri = selectedZoomPhoto,
            title = firstItem?.name ?: order.invoiceNo,
            onDismiss = { selectedZoomPhoto = null }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect?.invoke()
                    } else {
                        onCardClick()
                    }
                },
                onLongClick = {
                    if (onLongClick != null) {
                        onLongClick.invoke()
                    } else {
                        onToggleSelect?.invoke()
                    }
                }
            ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, if (isDark) Color(0xFFFFFFFF) else Color(0xFF9D174D))
        } else {
            BorderStroke(1.dp, if (isDark) tokens.borderMain else Color(0xFFE5E7EB))
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                if (isDark) tokens.surfaceSecondary else Color(0xFFFDF2F8)
            } else {
                if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else if (isSelected) 3.dp else 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // TOP MAIN ROW: Photo + Customer Info + Status/Tracking + Total Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 1. LEFT: Product Thumbnail with multi-item badge
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (order.items.size > 1) {
                                showAllItemsDialog = true
                            } else if (firstItem?.photoUri != null) {
                                selectedZoomPhoto = firstItem.photoUri
                            }
                        }
                ) {
                    if (firstItem?.photoUri != null) {
                        AsyncImage(
                            model = firstItem.photoUri,
                            contentDescription = firstItem.name,
                            modifier = Modifier.size(66.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(66.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (order.items.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp)
                                .clickable { showAllItemsDialog = true },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "+${order.items.size - 1}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. MIDDLE: Invoice, Date, Customer Name, Phone, Address
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.invoiceNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) tokens.textMuted else Color(0xFF6B7280),
                        fontSize = 10.5.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.5.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                MessagingHelper.dialCustomer(context, order.customerPhone)
                            }
                            .padding(vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = if (isDark) tokens.textSecondary else Color(0xFF4B5563),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = order.customerPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) tokens.textSecondary else Color(0xFF374151),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = if (isDark) tokens.textMuted else Color(0xFF6B7280),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${order.customerAddress}, ${order.district}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) tokens.textSecondary else Color(0xFF4B5563),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 3. RIGHT: Total Box & 3-dot Menu
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect?.invoke() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = if (isDark) Color.White else Color(0xFF9D174D),
                                checkmarkColor = if (isDark) Color.Black else Color.White,
                                uncheckedColor = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                            DropdownMenuItem(
                                text = { Text("কল করুন") },
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                onClick = {
                                    showMenu = false
                                    MessagingHelper.dialCustomer(context, order.customerPhone)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("WhatsApp মেসেজ") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showMenu = false
                                    val msg = MessagingHelper.generateCustomerConfirmationMessage(order, useBanglaDigits)
                                    MessagingHelper.openWhatsApp(context, order.customerPhone, msg)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("অর্ডার এডিট") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showMenu = false
                                    onEditOrder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ট্র্যাশে পাঠান") },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteOrder()
                                }
                            )
                        }
                    }
                }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Soft Total Box with separated COD badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDark) tokens.surfaceSecondary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .border(
                                1.dp,
                                if (isDark) tokens.borderMain else Color(0xFFE5E7EB),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = AppStrings.total(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) tokens.textMuted else Color(0xFF6B7280),
                                fontSize = 9.5.sp
                            )
                            Text(
                                text = totalAmountFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (order.codAmount <= 0.0) {
                                    if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7)
                                } else {
                                    if (isDark) Color(0xFF3B1E08) else Color(0xFFFEF3C7)
                                }
                            ) {
                                Text(
                                    text = "${AppStrings.cod(language)} $codAmountFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (order.codAmount <= 0.0) {
                                        if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                    } else {
                                        if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // STATUS & COURIER ROW: Pills, Consignment ID, Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Pill
                StatusPill(status = order.fulfillmentStatus, language = language)

                // Courier & Consignment ID Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Courier",
                        tint = if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = order.courierService.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (order.courierTrackingId.isNotBlank()) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
                            border = if (isDark) BorderStroke(1.dp, Color(0xFF166534)) else null,
                            modifier = Modifier.clickable {
                                MessagingHelper.copyToClipboard(context, "Consignment ID", order.courierTrackingId)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = order.courierTrackingId,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        // 1-TAP WHATSAPP TRACKING MESSAGE BUTTON
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF25D366),
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    val message = MessagingHelper.generateCustomerConfirmationMessage(order, useBanglaDigits)
                                    MessagingHelper.openWhatsApp(context, order.customerPhone, message)
                                }
                                .testTag("whatsapp_button_${order.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    } else if (order.courierSyncStatus == CourierSyncStatus.PENDING_SYNC) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF450A0A) else Color(0xFFFFE4E6),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF7F1D1D) else Color(0xFFFDA4AF)),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFBE123C),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "এন্ট্রি বাকি" else "Pending sync",
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFBE123C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // TAGS ROW: OrderType, Priority, Payment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Order Type Tag
                val isExchange = order.orderType == OrderType.EXCHANGE
                val isScheduledOrder = order.orderType == OrderType.SCHEDULED || order.isScheduled || order.requestedDeliveryDate.isNotBlank()
                val tagBg = if (isExchange) {
                    if (isDark) Color(0xFF2D1D02) else Color(0xFFFEF3C7)
                } else if (isScheduledOrder) {
                    if (isDark) Color(0xFF1E1B4B) else Color(0xFFEDE9FE)
                } else {
                    if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                }
                val tagFg = if (isExchange) {
                    if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                } else if (isScheduledOrder) {
                    if (isDark) Color(0xFFA5B4FC) else Color(0xFF6B46C1)
                } else {
                    if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSecondaryContainer
                }
                val tagLabel = if (isScheduledOrder) {
                    if (order.requestedDeliveryDate.isNotBlank()) "${AppStrings.scheduled(language)} (${order.requestedDeliveryDate})" else AppStrings.scheduled(language)
                } else if (language == AppLanguage.BANGLA) order.orderType.displayNameBn else order.orderType.displayNameEn

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagBg
                ) {
                    Text(
                        text = tagLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isExchange || isScheduledOrder) FontWeight.Bold else FontWeight.Medium,
                        color = tagFg,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Priority Tag (if urgent)
                if (order.priority == Priority.URGENT) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF450A0A) else Color(0xFFFFE4E6)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) order.priority.displayNameBn else order.priority.displayNameEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFBE123C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Payment Status Tag
                val paymentBg = when (order.paymentStatus) {
                    PaymentStatus.PAID -> if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7)
                    PaymentStatus.PARTIAL -> if (isDark) Color(0xFF3B1E08) else Color(0xFFFEF3C7)
                    PaymentStatus.UNPAID -> if (isDark) Color(0xFF450A0A) else Color(0xFFFFE4E6)
                }
                val paymentFg = when (order.paymentStatus) {
                    PaymentStatus.PAID -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                    PaymentStatus.PARTIAL -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                    PaymentStatus.UNPAID -> if (isDark) Color(0xFFF87171) else Color(0xFFBE123C)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = paymentBg
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) order.paymentStatus.displayNameBn else order.paymentStatus.displayNameEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = paymentFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                thickness = 0.8.dp
            )
            Spacer(modifier = Modifier.height(7.dp))

            // BOTTOM PACKING ROW: Thumbnail(s) + Dress Name + Size/Qty + 1-Tap Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item details
                if (order.items.size > 1) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        order.items.forEachIndexed { idx, itm ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAllItemsDialog = true }
                            ) {
                                if (!itm.photoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = itm.photoUri,
                                        contentDescription = itm.name,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(5.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itm.name.ifBlank { "আইটেম #${idx + 1}" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 1.dp)
                                    ) {
                                        if (itm.size.isNotBlank()) {
                                            Text(
                                                text = "সাইজ: ${itm.size}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (itm.color.isNotBlank()) {
                                            Text(
                                                text = "• ${itm.color}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color(0xFFA5B4FC) else MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = "• x${itm.quantity}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (firstItem?.photoUri != null) {
                            AsyncImage(
                                model = firstItem.photoUri,
                                contentDescription = firstItem.name,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = firstItem?.name ?: "ফ্যাশন ড্রেস",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "${AppStrings.size(language)} ${firstItem?.size ?: "40"}${if (!firstItem?.color.isNullOrBlank()) " | ${firstItem?.color}" else ""} | ${AppStrings.qty(language)} ${firstItem?.quantity ?: 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) tokens.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Action Button
                when {
                    order.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP -> {
                        Button(
                            onClick = onMarkAsShipped,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary,
                                contentColor = if (isDark) tokens.onPrimaryAction else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ship_button_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppStrings.markAsShipped(language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    order.fulfillmentStatus == FulfillmentStatus.PRE_ORDER_PENDING -> {
                        Button(
                            onClick = onStockArrived,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.secondary,
                                contentColor = if (isDark) tokens.onPrimaryAction else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("stock_arrived_button_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppStrings.stockArrivedReady(language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    order.courierTrackingId.isBlank() -> {
                        Button(
                            onClick = onBookCourier,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.secondary,
                                contentColor = if (isDark) tokens.onPrimaryAction else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("book_courier_button_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppStrings.bookCourier(language),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {
                        // Already shipped or other status
                        Text(
                            text = if (language == AppLanguage.BANGLA) order.fulfillmentStatus.displayNameBn else order.fulfillmentStatus.displayNameEn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: FulfillmentStatus, language: AppLanguage) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark

    val (bgColor, fgColor, icon) = when (status) {
        FulfillmentStatus.READY_TO_SHIP -> if (isDark) Triple(Color(0xFF052E16), Color(0xFF4ADE80), Icons.Default.Check) else Triple(Color(0xFF15803D), Color.White, Icons.Default.Check)
        FulfillmentStatus.SHIPPED -> if (isDark) Triple(Color(0xFF0B1E3B), Color(0xFF60A5FA), Icons.Default.Check) else Triple(Color(0xFF1D4ED8), Color.White, Icons.Default.Check)
        FulfillmentStatus.PRE_ORDER_PENDING -> if (isDark) Triple(Color(0xFF1E1B4B), Color(0xFFA5B4FC), Icons.Default.Inventory2) else Triple(Color(0xFF6B46C1), Color.White, Icons.Default.Inventory2)
        FulfillmentStatus.PROCESSING -> if (isDark) Triple(Color(0xFF2E1065), Color(0xFFC084FC), Icons.Default.AutoAwesome) else Triple(Color(0xFF7C3AED), Color.White, Icons.Default.AutoAwesome)
        FulfillmentStatus.NEW -> if (isDark) Triple(Color(0xFF2D1D02), Color(0xFFFBBF24), null) else Triple(Color(0xFFEA580C), Color.White, null)
        FulfillmentStatus.DELIVERED -> if (isDark) Triple(Color(0xFF064E3B), Color(0xFF34D399), Icons.Default.Check) else Triple(Color(0xFF047857), Color.White, Icons.Default.Check)
        FulfillmentStatus.CANCELLED -> if (isDark) Triple(Color(0xFF450A0A), Color(0xFFF87171), null) else Triple(Color(0xFFB91C1C), Color.White, null)
        FulfillmentStatus.RETURNED -> if (isDark) Triple(Color(0xFF4C0519), Color(0xFFFB7185), null) else Triple(Color(0xFFBE123C), Color.White, null)
    }

    val text = if (language == AppLanguage.BANGLA) status.displayNameBn else status.displayNameEn

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = if (isDark) BorderStroke(1.dp, fgColor.copy(alpha = 0.4f)) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = text,
                color = fgColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
fun OrderItemsGalleryDialog(
    order: OrderEntity,
    onDismiss: () -> Unit
) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface,
            border = if (isDark) BorderStroke(1.dp, tokens.borderMain) else null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${order.invoiceNo} - প্রোডাক্ট গ্যালারি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "মোট আইটেম: ${order.items.size}টি",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(order.items) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) tokens.surfaceSecondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (!item.photoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.photoUri,
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Text(
                                    text = item.name.ifBlank { "ফ্যাশন ড্রেস" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.size.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "সাইজ: ${item.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (item.color.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "কালার: ${item.color}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "পরিমাণ: ${item.quantity}টি | মূল্য: ৳${item.unitPrice.toInt()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
