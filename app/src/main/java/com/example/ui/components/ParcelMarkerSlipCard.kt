package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderEntity
import com.example.ui.theme.auraTokens
import com.example.util.BanglaFormatter
import com.example.util.MessagingHelper

@Composable
fun ParcelMarkerSlipCard(
    order: OrderEntity,
    useBanglaDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark
    val context = LocalContext.current
    val codStr = BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isDark) tokens.borderMain else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) tokens.surfaceMain else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "মার্কার স্লিপ (পার্সেল লেখার কার্ড)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        val textToCopy = """
                            Invoice: ${order.invoiceNo}
                            Consignment: ${order.courierTrackingId}
                            Customer: ${order.customerName}
                            Phone: ${order.customerPhone}
                            Address: ${order.customerAddress}, ${order.district}
                            COD: $codStr
                        """.trimIndent()
                        MessagingHelper.copyToClipboard(context, "Parcel Slip", textToCopy)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Slip",
                        tint = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Box for poly mailer writing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Invoice & Consignment ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "INVOICE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = order.invoiceNo,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (order.courierTrackingId.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CONSIGNMENT ID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                )
                                Text(
                                    text = order.courierTrackingId,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                )
                            }
                        } else if (order.courierSyncStatus == com.example.data.model.CourierSyncStatus.PENDING_SYNC) {
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF450A0A) else Color(0xFFFFE4E6),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFDA4AF))
                                ) {
                                    Text(
                                        text = "⚠️ কুরিয়ার API থেকে আইডি পাওয়া যায়নি",
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFBE123C),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Customer Name & Phone
                    Text(
                        text = order.customerName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = order.customerPhone,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Address
                    Text(
                        text = "${order.customerAddress}, ${order.district}",
                        fontSize = 14.sp,
                        color = tokens.textSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // COD Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF2D1D02) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .then(
                                if (isDark) Modifier.border(1.dp, Color(0xFF78350F), RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "কালেকশন (COD):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = codStr,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
