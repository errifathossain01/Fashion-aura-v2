package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.viewmodel.RestorePreviewState
import com.example.util.BanglaFormatter
import com.example.util.RestoreConflictStrategy

@Composable
fun SafeRestoreConfirmationDialog(
    previewState: RestorePreviewState,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmRestore: (RestoreConflictStrategy) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val meta = previewState.metadata
    var selectedStrategy by remember {
        mutableStateOf(RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS)
    }

    val dateFormatted = if (meta.exportedAt > 0) {
        BanglaFormatter.formatDate(meta.exportedAt, isBn)
    } else {
        if (isBn) "অজ্ঞাত তারিখ" else "Unknown Date"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBn) "ব্যাকআপ মেটাডাটা ও রিস্টোর নিশ্চিতকরণ" else "Backup Metadata & Restore Confirmation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Safety guarantee card
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) {
                                "ডাটা সুরক্ষা গ্যারান্টি: রিস্টোর শুরু করার আগে বর্তমান সমস্ত অর্ডারের একটি স্বয়ংক্রিয় সেফটি স্ন্যাপশট তৈরি ও যাচাই করা হবে।"
                            } else {
                                "Data Safety Guarantee: An automatic safety snapshot of current orders will be verified before applying any changes."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // File metadata details
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isBn) "ফাইলের তথ্য:" else "Backup File Information:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider()

                        MetadataRow(
                            label = if (isBn) "ব্যাকআপ তৈরির সময়:" else "Exported Date:",
                            value = dateFormatted
                        )
                        MetadataRow(
                            label = if (isBn) "মোট অর্ডার সংখ্যা:" else "Total Orders:",
                            value = "${meta.totalOrders} ${if (isBn) "টি" else ""}"
                        )
                        MetadataRow(
                            label = if (isBn) "ক্যাটালগ ড্রেস:" else "Catalog Dresses:",
                            value = "${meta.totalCatalogProducts} ${if (isBn) "টি" else ""}"
                        )
                        if (meta.photosInZipCount > 0) {
                            MetadataRow(
                                label = if (isBn) "সংরক্ষিত ছবি:" else "Archived Photos:",
                                value = "${meta.photosInZipCount} ${if (isBn) "টি" else ""}"
                            )
                        }
                        MetadataRow(
                            label = if (isBn) "ডাটাবেজ ফরম্যাট:" else "Format / Version:",
                            value = "${meta.format} (v${meta.version})"
                        )
                        MetadataRow(
                            label = if (isBn) "বর্তমান লোকাল অর্ডার:" else "Current Local Orders:",
                            value = "${previewState.currentDbOrdersCount} ${if (isBn) "টি" else ""}"
                        )
                    }
                }

                // Conflict Status Alert
                if (meta.conflictingOrderIdsCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) {
                                        "আইডি কনফ্লিক্ট শনাক্ত হয়েছে (${meta.conflictingOrderIdsCount}টি অর্ডার)"
                                    } else {
                                        "ID Conflicts Detected (${meta.conflictingOrderIdsCount} orders)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBn) {
                                    "ব্যাকআপের কিছু অর্ডারের আইডি আপনার ডিভাইসের চলমান অর্ডারের সাথে মিলে গেছে। আপনার পছন্দ অনুযায়ী সিদ্ধান্ত নিন:"
                                } else {
                                    "Some backup orders share IDs with orders on this device. Choose how to handle conflicting records:"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "কোনো আইডি কনফ্লিক্ট নেই। বর্তমান অর্ডার নিরাপদে অক্ষত থাকবে।" else "Zero ID conflicts. Existing orders will be completely preserved.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Conflict Strategies Selection
                Text(
                    text = if (isBn) "কনফ্লিক্ট হ্যান্ডলিং পছন্দ নির্বাচন করুন:" else "Select Conflict Handling Strategy:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Strategy 1: PRESERVE & RENUMBER (Recommended Default)
                StrategyOptionCard(
                    title = if (isBn) "১. বর্তমান অর্ডার বজায় রাখুন (প্রস্তাবিত)" else "1. Preserve Existing (Recommended)",
                    description = if (isBn) {
                        "বর্তমান কোনো ডাটা মুছে ফেলা বা পরিবর্তন করা হবে না। কনফ্লিক্ট হওয়া আগত ব্যাকআপ অর্ডারগুলোকে নতুন আইডি দিয়ে সংরক্ষণ করা হবে।"
                    } else {
                        "Never overwrite existing records. Incoming conflicting orders are assigned brand-new auto-generated IDs."
                    },
                    isSelected = selectedStrategy == RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS,
                    badgeText = if (isBn) "শতভাগ নিরাপদ" else "100% Safe",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    onClick = { selectedStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS }
                )

                // Strategy 2: SKIP CONFLICTS
                StrategyOptionCard(
                    title = if (isBn) "২. ডুপ্লিকেট বাদ দিন (Skip)" else "2. Skip Duplicate IDs",
                    description = if (isBn) {
                        "বর্তমান ডাটাবেজের আইডি অপরিবর্তিত থাকবে। যেসব অর্ডারের আইডি পূর্বে থেকেই আছে সেগুলো এড়িয়ে চলা হবে।"
                    } else {
                        "Preserve local database. Skip any incoming backup order whose ID already exists locally."
                    },
                    isSelected = selectedStrategy == RestoreConflictStrategy.SKIP_CONFLICTS,
                    badgeText = if (isBn) "নিরাপদ" else "Safe",
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    onClick = { selectedStrategy = RestoreConflictStrategy.SKIP_CONFLICTS }
                )

                // Strategy 3: OVERWRITE (Requires deliberate choice)
                StrategyOptionCard(
                    title = if (isBn) "৩. বিদ্যমান অর্ডার প্রতিস্থাপন করুন (Overwrite)" else "3. Overwrite Existing Orders",
                    description = if (isBn) {
                        "সতর্কতা: একই আইডির বিদ্যমান লোকাল অর্ডার মুছে নতুনটি বসবে। তবে রিস্টোরের ঠিক পূর্বে স্বয়ংক্রিয় সেফটি স্ন্যাপশট সেভ থাকবে।"
                    } else {
                        "Caution: Existing records with matching IDs will be replaced. A safety snapshot will be verified first."
                    },
                    isSelected = selectedStrategy == RestoreConflictStrategy.OVERWRITE_EXISTING,
                    badgeText = if (isBn) "সাবধানতা প্রয়োজন" else "Use Caution",
                    badgeColor = MaterialTheme.colorScheme.error,
                    onClick = { selectedStrategy = RestoreConflictStrategy.OVERWRITE_EXISTING }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRestore(selectedStrategy) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedStrategy == RestoreConflictStrategy.OVERWRITE_EXISTING) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isBn) "নিরাপদে রিস্টোর সম্পন্ন করুন" else "Confirm & Restore Safely",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isBn) "বাতিল করুন" else "Cancel")
            }
        }
    )
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StrategyOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
