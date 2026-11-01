package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.theme.auraTokens
import androidx.compose.ui.graphics.Color
import com.example.data.model.OrderEntity

@Composable
fun ConsignmentIdDialog(
    order: OrderEntity,
    onDismiss: () -> Unit,
    onSaveConsignmentId: (String) -> Unit,
    onBookSteadfastApi: () -> Unit
) {
    val tokens = MaterialTheme.auraTokens
    val isDark = tokens.isDark
    var consignmentId by remember { mutableStateOf(order.courierTrackingId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) tokens.surfaceElevated else MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "কুরিয়ার ট্র্যাকিং আইডি (${order.invoiceNo})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Consignment ID (যেমন: 312731669 বা SF-955058) লিখলে অর্ডারটি স্বয়ংক্রিয়ভাবে 'রেডি টু শিপ' স্ট্যাটাসে চলে যাবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = consignmentId,
                    onValueChange = { consignmentId = it },
                    label = { Text("Consignment ID / ট্র্যাকিং কোড") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        focusedBorderColor = if (isDark) tokens.borderActive else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDark) tokens.borderMain else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = if (isDark) tokens.textPrimary else MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (isDark) tokens.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onBookSteadfastApi()
                    },
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "⚡ Steadfast API দিয়ে অটো বুকিং করুন",
                        color = if (isDark) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConsignmentId(consignmentId.trim())
                    onDismiss()
                },
                enabled = consignmentId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) tokens.primaryAction else MaterialTheme.colorScheme.primary,
                    contentColor = if (isDark) tokens.onPrimaryAction else Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("আইডি সেভ করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = tokens.textSecondary)
            }
        }
    )
}
