package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.PaymentStatus
import com.example.util.BanglaFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    orders: List<OrderEntity>,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    modifier: Modifier = Modifier
) {
    val totalOrders = orders.size
    val totalRevenue = orders.sumOf { it.totalBill }
    val totalCollected = orders.sumOf { it.advanceAmount } + orders.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.totalBill - it.advanceAmount }
    val pendingCod = orders.filter { it.paymentStatus != PaymentStatus.PAID && it.fulfillmentStatus != FulfillmentStatus.DELIVERED && it.fulfillmentStatus != FulfillmentStatus.CANCELLED }.sumOf { it.codAmount }

    val readyToShipCount = orders.count { it.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP }
    val shippedCount = orders.count { it.fulfillmentStatus == FulfillmentStatus.SHIPPED }
    val preOrderCount = orders.count { it.fulfillmentStatus == FulfillmentStatus.PRE_ORDER_PENDING }

    // District grouping
    val districtMap = orders.groupBy { it.district.ifBlank { if (language == AppLanguage.BANGLA) "অন্যান্য" else "Others" } }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "বিজনেস রিপোর্ট ও অ্যানালিটিক্স" else "Business Reports & Analytics",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 90.dp)
        ) {
            // Revenue summary cards
            item {
                Text(
                    text = if (language == AppLanguage.BANGLA) "আর্থিক পারফরম্যান্স" else "Financial Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsCard(
                        title = if (language == AppLanguage.BANGLA) "মোট বিক্রয়" else "Total Revenue",
                        value = BanglaFormatter.formatCurrency(totalRevenue, useBanglaDigits),
                        icon = Icons.Default.TrendingUp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        title = if (language == AppLanguage.BANGLA) "পেন্ডিং COD কালেকশন" else "Pending COD",
                        value = BanglaFormatter.formatCurrency(pendingCod, useBanglaDigits),
                        icon = Icons.Default.Payments,
                        color = Color(0xFFC2410C),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Shipment Funnel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.BANGLA) "শিপমেন্ট ও কুরিয়ার ট্র্যাকিং অবস্থা" else "Shipment & Courier Status",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val parcelUnit = if (language == AppLanguage.BANGLA) "টি পার্সেল" else "Parcels"
                        StatRow(
                            label = if (language == AppLanguage.BANGLA) "রেডি টু শিপ" else "Ready to Ship",
                            value = "${BanglaFormatter.formatNumber(readyToShipCount, useBanglaDigits)} $parcelUnit",
                            color = Color(0xFF15803D)
                        )
                        StatRow(
                            label = if (language == AppLanguage.BANGLA) "শিপড (কুরিয়ারে ডেলিভারি চলমান)" else "Shipped (In Transit)",
                            value = "${BanglaFormatter.formatNumber(shippedCount, useBanglaDigits)} $parcelUnit",
                            color = Color(0xFF1D4ED8)
                        )
                        StatRow(
                            label = if (language == AppLanguage.BANGLA) "প্রি-অর্ডার অপেক্ষমাণ" else "Pre-Order Pending",
                            value = "${BanglaFormatter.formatNumber(preOrderCount, useBanglaDigits)} $parcelUnit",
                            color = Color(0xFF6B46C1)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Top Districts
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.BANGLA) "জেলা অনুযায়ী ডেলিভারি বণ্টন" else "District-wise Delivery",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val orderUnit = if (language == AppLanguage.BANGLA) "টি অর্ডার" else "Orders"
                        districtMap.take(6).forEach { (district, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(district, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${BanglaFormatter.formatNumber(count, useBanglaDigits)} $orderUnit",
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

@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
