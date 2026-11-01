package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.OrderEntity
import com.example.util.BanglaFormatter
import com.example.util.MessagingHelper
import com.example.util.PrintHelper

enum class ShippingPrintTab {
    THERMAL_LABELS,
    RIDER_MANIFEST
}

@Composable
fun BulkShippingLabelsDialog(
    selectedOrders: List<OrderEntity>,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onMarkAsShipped: ((List<OrderEntity>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ShippingPrintTab.THERMAL_LABELS) }

    val totalCountStr = BanglaFormatter.formatNumber(selectedOrders.size, useBanglaDigits)
    val totalCodAmount = selectedOrders.sumOf { it.codAmount }
    val formattedTotalCod = BanglaFormatter.formatCurrency(totalCodAmount, useBanglaDigits)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "প্রিন্ট ও রাইডার হ্যান্ডওভার শিট",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "$totalCountStr টি পার্সেল • মোট COD: $formattedTotalCod",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2 Tabs: থার্মাল লেবেল ও রাইডার ম্যানিফেস্ট
                TabRow(
                    selectedTabIndex = if (selectedTab == ShippingPrintTab.THERMAL_LABELS) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == ShippingPrintTab.THERMAL_LABELS,
                        onClick = { selectedTab = ShippingPrintTab.THERMAL_LABELS },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏷️ থার্মাল লেবেল", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTab == ShippingPrintTab.THERMAL_LABELS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = totalCountStr,
                                        fontSize = 10.sp,
                                        color = if (selectedTab == ShippingPrintTab.THERMAL_LABELS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == ShippingPrintTab.RIDER_MANIFEST,
                        onClick = { selectedTab = ShippingPrintTab.RIDER_MANIFEST },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📄 রাইডার ম্যানিফেস্ট", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB 1: THERMAL LABELS PREVIEW
                if (selectedTab == ShippingPrintTab.THERMAL_LABELS) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(selectedOrders) { index, order ->
                            SingleThermalLabelPreviewCard(
                                index = index + 1,
                                order = order,
                                useBanglaDigits = useBanglaDigits
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab 1 Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Copy All Slips
                        OutlinedButton(
                            onClick = {
                                val text = PrintHelper.generateBulkSlipsText(selectedOrders, useBanglaDigits)
                                MessagingHelper.copyToClipboard(context, "Fashion Aura Slips", text)
                                Toast.makeText(context, "সবগুলো স্লিপ কপি হয়েছে 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("স্লিপ কপি", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 2. Share
                        OutlinedButton(
                            onClick = {
                                val text = PrintHelper.generateBulkSlipsText(selectedOrders, useBanglaDigits)
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Slips"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 3. Print / PDF
                        Button(
                            onClick = {
                                PrintHelper.printShippingLabels(context, selectedOrders, useBanglaDigits)
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রিন্ট / PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // TAB 2: RIDER PICKUP MANIFEST
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Summary Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFDF2F8),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF472B6)),
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
                                    Text("📦 মোট পার্সেল", fontSize = 11.sp, color = Color(0xFF831843))
                                    Text("$totalCountStr টি", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF831843))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💰 মোট COD", fontSize = 11.sp, color = Color(0xFF831843))
                                    Text(formattedTotalCod, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🚚 কুরিয়ার", fontSize = 11.sp, color = Color(0xFF831843))
                                    Text("Steadfast", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manifest Table Preview
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(selectedOrders) { index, order ->
                                val sl = BanglaFormatter.formatNumber(index + 1, useBanglaDigits)
                                val codStr = BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(sl, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${order.invoiceNo} • ${order.customerName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = codStr,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF047857)
                                                )
                                            }
                                            Text(
                                                text = "CID: ${if (order.courierTrackingId.isNotBlank()) order.courierTrackingId else "এন্ট্রি বাকি"} | 📞 ${order.customerPhone}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "📍 ${order.customerAddress}, ${order.district}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Signature Box Preview
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("✍️ রাইডারের নাম ও স্বাক্ষর: _________________", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("✍️ শপ স্বাক্ষর: _________________", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab 2 Action Buttons: Print Manifest, WhatsApp Share, and "Mark as Shipped"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Print Manifest
                            Button(
                                onClick = {
                                    PrintHelper.printRiderManifest(context, selectedOrders, useBanglaDigits)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ম্যানিফেস্ট প্রিন্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. WhatsApp Share
                            Button(
                                onClick = {
                                    val text = PrintHelper.generateRiderManifestText(selectedOrders, useBanglaDigits)
                                    try {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            setPackage("com.whatsapp")
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }, "ম্যানিফেস্ট শেয়ার করুন")
                                        context.startActivity(chooser)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("হোয়াটসঅ্যাপ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 3. Mark All as Shipped
                            Button(
                                onClick = {
                                    onMarkAsShipped?.invoke(selectedOrders)
                                    Toast.makeText(context, "${selectedOrders.size}টি অর্ডার Shipped মার্ক করা হয়েছে 🚚", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D174D)),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shipped মার্ক", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleThermalLabelPreviewCard(
    index: Int,
    order: OrderEntity,
    useBanglaDigits: Boolean
) {
    val codStr = BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)
    val indexStr = BanglaFormatter.formatNumber(index, useBanglaDigits)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Label Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = indexStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FASHION AURA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = order.invoiceNo,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tracking & Barcode Visual
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "COURIER CONSIGNMENT / TRACKING",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (order.courierTrackingId.isNotBlank()) order.courierTrackingId else "STEADFAST PENDING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Barcode Visual Lines
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(34) { i ->
                            val width = if (i % 3 == 0) 3.dp else if (i % 2 == 0) 1.5.dp else 1.dp
                            Box(
                                modifier = Modifier
                                    .width(width)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recipient Section
            Text(
                text = "ডেলিভারি প্রাপক (Customer):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = order.customerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "📞 ${order.customerPhone}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "📍 ${order.customerAddress}, ${order.district}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Items breakdown
            if (order.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "প্যাকেজ ড্রেস তালিকা:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        order.items.forEach { item ->
                            Text(
                                text = "• ${item.name} (সাইজ: ${item.size.ifBlank { "Free" }}${if (item.color.isNotBlank()) ", কালার: " + item.color else ""}) x ${item.quantity}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cash On Delivery (COD) Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFDF2F8),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE935C1))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ক্যাশ অন ডেলিভারি (COD):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF831843)
                    )
                    Text(
                        text = codStr,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF9D174D)
                    )
                }
            }
        }
    }
}
