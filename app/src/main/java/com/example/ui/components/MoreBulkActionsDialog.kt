package com.example.ui.components

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.PaymentStatus
import com.example.util.BanglaFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MoreBulkActionsDialog(
    selectedOrders: List<OrderEntity>,
    useBanglaDigits: Boolean,
    onConvertToRegular: (Set<Long>) -> Unit,
    onUpdateDeliveryDate: (Set<Long>, String) -> Unit,
    onUpdateStatus: (Set<Long>, FulfillmentStatus, PaymentStatus?) -> Unit,
    onMoveToTrash: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedIds = remember(selectedOrders) { selectedOrders.map { it.id }.toSet() }
    val countStr = BanglaFormatter.formatNumber(selectedOrders.size, useBanglaDigits)

    var showTrashConfirmDialog by remember { mutableStateOf(false) }
    var showStatusUpdateSubDialog by remember { mutableStateOf(false) }

    // CSV Generator for Steadfast Web Upload
    fun generateSteadfastCsv(): String {
        return buildString {
            appendLine("Invoice,Name,Address,Phone,Amount,Note")
            selectedOrders.forEach { order ->
                val inv = "\"${order.invoiceNo.replace("\"", "\"\"")}\""
                val name = "\"${order.customerName.replace("\"", "\"\"")}\""
                val addr = "\"${("${order.customerAddress}, ${order.district}").replace("\"", "\"\"")}\""
                val phone = "\"${order.customerPhone.replace("\"", "\"\"")}\""
                val amount = order.codAmount.toInt()
                val itemsNote = order.items.joinToString("; ") { "${it.name} (${it.size}) x ${it.quantity}" }
                val note = "\"${itemsNote.replace("\"", "\"\"")}\""
                appendLine("$inv,$name,$addr,$phone,$amount,$note")
            }
        }
    }

    // Phone Numbers Extractor & Formatter
    fun extractCleanPhoneNumbers(): List<String> {
        return selectedOrders.mapNotNull { order ->
            val digits = order.customerPhone.filter { it.isDigit() }
            val clean = if (digits.startsWith("880") && digits.length == 13) {
                digits.removePrefix("88")
            } else {
                digits
            }
            if (clean.length == 11 && clean.startsWith("01")) clean else null
        }.distinct()
    }

    // Android Date Picker for Delivery Date
    fun openDatePicker() {
        val cal = Calendar.getInstance()
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val targetCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(targetCal.time)
                onUpdateDeliveryDate(selectedIds, formattedDate)
                onDismiss()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
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
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⚡", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "আরও বাল্ক অ্যাকশন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$countStr টি অর্ডারে একসাথে কার্যকর হবে",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                // 6 Distinct Actions
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 🔄 Pre-Order থেকে Regular-এ রূপান্তর
                    item {
                        MoreActionItemRow(
                            icon = Icons.Default.Refresh,
                            iconTint = Color(0xFF38BDF8),
                            title = "Pre-Order থেকে Regular-এ রূপান্তর",
                            subtitle = "স্টক চলে আসলে অর্ডারগুলোকে রেগুলার ও রেডি টু শিপে আপডেট করুন",
                            onClick = {
                                onConvertToRegular(selectedIds)
                                onDismiss()
                            }
                        )
                    }

                    // 2. 📅 বাল্ক শিডিউল তারিখ পরিবর্তন
                    item {
                        MoreActionItemRow(
                            icon = Icons.Default.CalendarMonth,
                            iconTint = Color(0xFFF59E0B),
                            title = "বাল্ক ডেলিভারি তারিখ নির্ধারণ",
                            subtitle = "ক্যালেন্ডার ওপেন করে সব অর্ডারের কাঙ্ক্ষিত ডেলিভারি তারিখ একসাথে সেট করুন",
                            onClick = {
                                openDatePicker()
                            }
                        )
                    }

                    // 3. ✅ বাল্ক স্ট্যাটাস আপডেট
                    item {
                        MoreActionItemRow(
                            icon = Icons.Default.CheckCircle,
                            iconTint = Color(0xFF10B981),
                            title = "বাল্ক স্ট্যাটাস আপডেট",
                            subtitle = "Ready to Ship, Shipped, বা Delivered & Paid মার্ক করুন",
                            onClick = {
                                showStatusUpdateSubDialog = true
                            }
                        )
                    }

                    // 4. 📊 Steadfast Bulk CSV কপি / এক্সপোর্ট
                    item {
                        MoreActionItemRow(
                            icon = Icons.Default.TableChart,
                            iconTint = Color(0xFF6366F1),
                            title = "Steadfast CSV কপি ও এক্সপোর্ট",
                            subtitle = "ওয়েব প্যানেলে আপলোডের জন্য স্ট্যান্ডার্ড CSV ফরম্যাটে কপি বা ফাইল শেয়ার করুন",
                            onClick = {
                                val csvText = generateSteadfastCsv()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SteadfastCSV", csvText))

                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, csvText)
                                    type = "text/csv"
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, "Steadfast CSV শেয়ার করুন"))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "CSV ক্লিপবোর্ডে কপি হয়েছে 📊", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        )
                    }

                    // 5. 📱 সব মোবাইল নম্বর কপি
                    item {
                        MoreActionItemRow(
                            icon = Icons.Default.Phone,
                            iconTint = Color(0xFFEC4899),
                            title = "সব মোবাইল নম্বর কপি (SMS পাঠাতে)",
                            subtitle = "বাল্ক এসএমএস দিতে ইউনিক ১১-ডিজিট নম্বরগুলো একসাথে কপি করুন",
                            onClick = {
                                val cleanPhones = extractCleanPhoneNumbers()
                                if (cleanPhones.isNotEmpty()) {
                                    val phonesText = cleanPhones.joinToString(", ")
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("CustomerPhones", phonesText))
                                    Toast.makeText(context, "${cleanPhones.size}টি ফোন নম্বর কপি হয়েছে 📱", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "কোনো বৈধ মোবাইল নম্বর পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        )
                    }

                    // 6. 🗑️ বাল্ক ট্র্যাশ
                    item {
                        MoreActionItemRow(
                            icon = Icons.Default.Delete,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = "ট্র্যাশে পাঠান (Move to Trash)",
                            subtitle = "নির্বাচিত অর্ডারগুলোকে একসাথে ট্র্যাশে সরিয়ে ফেলুন",
                            isDestructive = true,
                            onClick = {
                                showTrashConfirmDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("বন্ধ করুন")
                }
            }
        }
    }

    // Sub-dialog for Status Selection
    if (showStatusUpdateSubDialog) {
        AlertDialog(
            onDismissRequest = { showStatusUpdateSubDialog = false },
            title = {
                Text("বাল্ক স্ট্যাটাস নির্বাচন করুন", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusOptionButton(
                        label = "✅ Ready to Ship (রেডি টু শিপ)",
                        color = Color(0xFF0284C7)
                    ) {
                        onUpdateStatus(selectedIds, FulfillmentStatus.READY_TO_SHIP, null)
                        showStatusUpdateSubDialog = false
                        onDismiss()
                    }

                    StatusOptionButton(
                        label = "🚚 Shipped (কুরিয়ারে পাঠানো হয়েছে)",
                        color = Color(0xFF7C3AED)
                    ) {
                        onUpdateStatus(selectedIds, FulfillmentStatus.SHIPPED, null)
                        showStatusUpdateSubDialog = false
                        onDismiss()
                    }

                    StatusOptionButton(
                        label = "💰 Delivered & Paid (ডেলিভার্ড ও পরিশোধিত)",
                        color = Color(0xFF059669)
                    ) {
                        onUpdateStatus(selectedIds, FulfillmentStatus.DELIVERED, PaymentStatus.PAID)
                        showStatusUpdateSubDialog = false
                        onDismiss()
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusUpdateSubDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Trash Confirmation Dialog
    if (showTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showTrashConfirmDialog = false },
            title = {
                Text("অর্ডারগুলো ট্র্যাশে পাঠাবেন?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("$countStr টি নির্বাচিত অর্ডার ট্র্যাশে পাঠানো হবে। পরে সেটিংস বা ট্র্যাশ স্ক্রিন থেকে পুনরুদ্ধার করা যাবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTrashConfirmDialog = false
                        onMoveToTrash(selectedIds)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ট্র্যাশে পাঠান", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun MoreActionItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusOptionButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
