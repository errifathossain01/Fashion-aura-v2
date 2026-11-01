package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItem
import com.example.util.BanglaFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PickListGroup(
    val dressName: String,
    val color: String,
    val sizeQuantities: Map<String, Int>,
    val totalGroupQuantity: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkPickListDialog(
    selectedOrders: List<OrderEntity>,
    useBanglaDigits: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Group items by (dressName, color), then breakdown by size
    val groups: List<PickListGroup> = remember(selectedOrders) {
        val map = mutableMapOf<Pair<String, String>, MutableMap<String, Int>>()
        selectedOrders.forEach { order ->
            val items = if (order.items.isEmpty()) {
                listOf(OrderItem(name = "ফ্যাশন ড্রেস", size = "Free", color = "", quantity = 1))
            } else {
                order.items
            }
            items.forEach { item ->
                val name = item.name.ifBlank { "ফ্যাশন ড্রেস" }.trim()
                val color = item.color.trim()
                val size = item.size.ifBlank { "Free Size" }.trim()
                val qty = item.quantity.coerceAtLeast(1)

                val key = Pair(name, color)
                val sizeMap = map.getOrPut(key) { mutableMapOf() }
                sizeMap[size] = (sizeMap[size] ?: 0) + qty
            }
        }
        map.map { (key, sizeMap) ->
            PickListGroup(
                dressName = key.first,
                color = key.second,
                sizeQuantities = sizeMap.toSortedMap(compareBy {
                    it.toIntOrNull() ?: Int.MAX_VALUE
                }),
                totalGroupQuantity = sizeMap.values.sum()
            )
        }.sortedByDescending { it.totalGroupQuantity }
    }

    val totalOrdersCount = selectedOrders.size
    val totalDressesCount = groups.sumOf { it.totalGroupQuantity }
    val totalCodAmount = selectedOrders.sumOf { it.codAmount }

    val formattedTotalOrders = BanglaFormatter.formatNumber(totalOrdersCount, useBanglaDigits)
    val formattedTotalDresses = BanglaFormatter.formatNumber(totalDressesCount, useBanglaDigits)
    val formattedTotalCod = BanglaFormatter.formatCurrency(totalCodAmount, useBanglaDigits)

    // Formatted text for Clipboard & WhatsApp
    fun buildPickListText(): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        return buildString {
            appendLine("📋 FASHION AURA - গোডাউন পিক-লিস্ট (GODOWN PACKING LIST)")
            appendLine("📅 তারিখ: $dateStr")
            appendLine("==================================================")
            appendLine("📦 সর্বমোট অর্ডার: $totalOrdersCount টি | 👗 সর্বমোট ড্রেস: $totalDressesCount পিস")
            appendLine("💰 মোট COD অ্যামাউন্ট: ৳${totalCodAmount.toInt()}")
            appendLine("==================================================")
            appendLine()
            groups.forEachIndexed { index, group ->
                val colorTag = if (group.color.isNotBlank()) " (${group.color})" else ""
                appendLine("${index + 1}. ${group.dressName}$colorTag")
                val sizeLines = group.sizeQuantities.map { (size, qty) ->
                    "সাইজ $size: $qty পিস"
                }.joinToString(" | ")
                appendLine("   • $sizeLines")
                appendLine("   👉 মোট: ${group.totalGroupQuantity} পিস")
                appendLine()
            }
            appendLine("--------------------------------------------------")
            appendLine("✅ গোডাউন ও প্যাকেজিং টিম: প্রতিটি অর্ডারের সাইজ ও কালার চেক করে যত্নসহকারে পার্সেল প্যাকিং সম্পন্ন করুন।")
        }
    }

    // Android PrintManager integration for printing or saving PDF
    fun printPickList() {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        val webView = WebView(context)
        val dateStr = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.ENGLISH).format(Date())

        val html = buildString {
            append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Fashion Aura Pick List</title>")
            append("<style>")
            append("body { font-family: -apple-system, sans-serif; padding: 24px; color: #111; font-size: 13px; }")
            append("h2 { margin: 0 0 4px 0; color: #831843; font-size: 20px; font-weight: bold; }")
            append(".meta { font-size: 11px; color: #666; margin-bottom: 16px; }")
            append(".summary-box { background: #fdf2f8; border: 1.5px solid #f472b6; border-radius: 8px; padding: 12px; margin-bottom: 20px; display: flex; justify-content: space-around; }")
            append(".summary-item { font-size: 13px; font-weight: bold; }")
            append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }")
            append("th { background: #831843; color: white; padding: 8px 10px; text-align: left; font-size: 12px; }")
            append("td { padding: 9px 10px; border-bottom: 1px solid #e5e7eb; vertical-align: top; }")
            append(".size-pill { display: inline-block; background: #f3f4f6; border: 1px solid #d1d5db; border-radius: 4px; padding: 2px 7px; margin: 2px; font-weight: bold; font-size: 11px; }")
            append(".total-badge { font-weight: bold; color: #831843; font-size: 13px; }")
            append(".footer { margin-top: 30px; font-size: 11px; color: #666; border-top: 1px dashed #ccc; padding-top: 8px; }")
            append("</style></head><body>")
            append("<h2>👗 FASHION AURA — গোডাউন পিক-লিস্ট</h2>")
            append("<div class='meta'>তারিখ: $dateStr | প্রিন্ট কপি</div>")
            append("<div class='summary-box'>")
            append("<div class='summary-item'>📦 মোট অর্ডার: $totalOrdersCount টি</div>")
            append("<div class='summary-item'>👗 মোট ড্রেস: $totalDressesCount পিস</div>")
            append("<div class='summary-item'>💰 মোট COD: ৳${totalCodAmount.toInt()}</div>")
            append("</div>")
            append("<table>")
            append("<thead><tr><th style='width: 35px;'>নং</th><th>ড্রেসের নাম ও কালার</th><th>সাইজ ও পরিমাণ ব্রেকডাউন</th><th style='width: 80px; text-align: right;'>মোট পিস</th></tr></thead>")
            append("<tbody>")
            groups.forEachIndexed { i, g ->
                val colorStr = if (g.color.isNotBlank()) " <span style='color: #666;'>(${g.color})</span>" else ""
                val sizePills = g.sizeQuantities.map { (sz, q) ->
                    "<span class='size-pill'>সাইজ $sz: <b>$q</b></span>"
                }.joinToString(" ")
                append("<tr>")
                append("<td>${i + 1}</td>")
                append("<td><b>${g.dressName}</b>$colorStr</td>")
                append("<td>$sizePills</td>")
                append("<td style='text-align: right;' class='total-badge'>${g.totalGroupQuantity} পিস</td>")
                append("</tr>")
            }
            append("</tbody></table>")
            append("<div class='footer'>Fashion Aura Automated Warehouse Management System • প্যাকেজিং যাচাইয়ের জন্য প্রস্তুতকৃত</div>")
            append("</body></html>")
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter("FashionAura_PickList")
                printManager?.print("FashionAura_PickList", printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
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
                                Text("📋", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "গোডাউন পিক-লিস্ট",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "সাইজ ও কালার ভিত্তিক প্যাকেজিং সামারি",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Banner: "সর্বমোট অর্ডার সংখ্যা" এবং "সর্বমোট ড্রেস (পিস) সংখ্যা"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFDF2F8),
                    border = BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦 সর্বমোট অর্ডার", fontSize = 11.sp, color = Color(0xFF831843), fontWeight = FontWeight.Medium)
                            Text("$formattedTotalOrders টি", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF831843))
                        }

                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFFF472B6).copy(alpha = 0.5f)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👗 সর্বমোট ড্রেস (পিস)", fontSize = 11.sp, color = Color(0xFF831843), fontWeight = FontWeight.Medium)
                            Text("$formattedTotalDresses পিস", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9D174D))
                        }

                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFFF472B6).copy(alpha = 0.5f)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰 মোট COD", fontSize = 11.sp, color = Color(0xFF831843), fontWeight = FontWeight.Medium)
                            Text(formattedTotalCod, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "প্রোডাক্ট ও সাইজ ব্রেকডাউন তালিকা:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // List of Dress Groups
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    itemsIndexed(groups) { index, group ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Group Title & Total Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = buildString {
                                                append(group.dressName)
                                                if (group.color.isNotBlank()) append(" (${group.color})")
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF9D174D)
                                    ) {
                                        Text(
                                            text = "মোট: ${BanglaFormatter.formatNumber(group.totalGroupQuantity, useBanglaDigits)} পিস",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Size Breakdown FlowRow
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    group.sizeQuantities.forEach { (size, qty) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "সাইজ $size: ",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${BanglaFormatter.formatNumber(qty, useBanglaDigits)} পিস",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom 3 Action Buttons: 📋 কপি টেক্সট, 💬 হোয়াটসঅ্যাপ শেয়ার, 🖨️ প্রিন্ট / PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 📋 কপি টেক্সট
                    OutlinedButton(
                        onClick = {
                            val pickText = buildPickListText()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("FashionAuraPickList", pickText))
                            Toast.makeText(context, "পিক-লিস্ট ক্লিপবোর্ডে কপি হয়েছে 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কপি", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // 2. 💬 হোয়াটসঅ্যাপ শেয়ার
                    Button(
                        onClick = {
                            val pickText = buildPickListText()
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    setPackage("com.whatsapp")
                                    putExtra(Intent.EXTRA_TEXT, pickText)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, pickText)
                                }, "পিক-লিস্ট শেয়ার করুন")
                                context.startActivity(chooser)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("হোয়াটসঅ্যাপ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // 3. 🖨️ প্রিন্ট / PDF
                    Button(
                        onClick = { printPickList() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("প্রিন্ট / PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
