package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppLanguage
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.Priority
import com.example.ui.theme.AppStrings
import com.example.util.BanglaFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationRemindersDialog(
    orders: List<OrderEntity>,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onOrderClick: (OrderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val scheduledOrPreOrders = remember(orders) {
        orders.filter {
            it.requestedDeliveryDate.isNotBlank() ||
            it.isScheduled ||
            it.fulfillmentStatus == FulfillmentStatus.PRE_ORDER_PENDING
        }
    }

    val pendingCourierOrders = remember(orders) {
        orders.filter {
            (it.courierTrackingId.isBlank() || it.courierTrackingId.equals("PENDING", ignoreCase = true)) &&
            (it.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP || it.fulfillmentStatus == FulfillmentStatus.NEW)
        }
    }

    val urgentOrders = remember(orders) {
        orders.filter {
            it.priority == Priority.URGENT &&
            it.fulfillmentStatus != FulfillmentStatus.DELIVERED &&
            it.fulfillmentStatus != FulfillmentStatus.CANCELLED
        }
    }

    val isBn = language == AppLanguage.BANGLA

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBn) "রিমাইন্ডার ও নোটিফিকেশন" else "Reminders & Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBn) "আজকের ডেলিভারি ও পেন্ডিং কুরিয়ার" else "Today's deliveries & pending courier",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Status Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderStatBadge(
                        title = if (isBn) "শিডিউল/প্রি-অর্ডার" else "Scheduled",
                        count = scheduledOrPreOrders.size,
                        color = Color(0xFF6B46C1),
                        useBanglaDigits = useBanglaDigits,
                        modifier = Modifier.weight(1f)
                    )
                    ReminderStatBadge(
                        title = if (isBn) "কুরিয়ার বাকি" else "Pending Sync",
                        count = pendingCourierOrders.size,
                        color = Color(0xFFC2410C),
                        useBanglaDigits = useBanglaDigits,
                        modifier = Modifier.weight(1f)
                    )
                    ReminderStatBadge(
                        title = if (isBn) "জরুরি অর্ডার" else "Urgent",
                        count = urgentOrders.size,
                        color = Color(0xFFDC2626),
                        useBanglaDigits = useBanglaDigits,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                // Order Action List
                val displayList = remember(scheduledOrPreOrders, pendingCourierOrders, urgentOrders) {
                    (urgentOrders + pendingCourierOrders + scheduledOrPreOrders).distinctBy { it.id }.take(10)
                }

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBn) "এই মুহূর্তে কোনো জরুরি রিমাইন্ডার নেই!" else "No urgent reminders right now!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList, key = { it.id }) { order ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onOrderClick(order)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = order.invoiceNo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (order.priority == Priority.URGENT) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFEE2E2)
                                                ) {
                                                    Text(
                                                        text = if (isBn) "জরুরি" else "Urgent",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFDC2626),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${order.customerName} • ${order.district}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isBn) order.fulfillmentStatus.displayNameBn else order.fulfillmentStatus.displayNameEn,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dismiss Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = if (isBn) "বন্ধ করুন" else "Close",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderStatBadge(
    title: String,
    count: Int,
    color: Color,
    useBanglaDigits: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = BanglaFormatter.formatNumber(count, useBanglaDigits),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                maxLines = 1
            )
        }
    }
}
