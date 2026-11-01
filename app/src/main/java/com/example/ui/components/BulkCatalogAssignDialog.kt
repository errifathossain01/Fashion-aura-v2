package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.CatalogProductEntity
import com.example.util.BanglaFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkCatalogAssignDialog(
    selectedOrdersCount: Int,
    catalogProducts: List<CatalogProductEntity>,
    useBanglaDigits: Boolean = true,
    onAssign: (product: CatalogProductEntity, keepExistingSizes: Boolean, overrideSize: String?, overrideColor: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<CatalogProductEntity?>(catalogProducts.firstOrNull()) }
    var keepExistingSizes by remember { mutableStateOf(true) }
    var overrideSize by remember { mutableStateOf<String?>(null) }
    var overrideColor by remember { mutableStateOf<String?>(null) }

    val filteredProducts = remember(catalogProducts, searchQuery) {
        if (searchQuery.isBlank()) {
            catalogProducts
        } else {
            val q = searchQuery.trim().lowercase()
            catalogProducts.filter {
                it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
    }

    val countStr = BanglaFormatter.formatNumber(selectedOrdersCount, useBanglaDigits)

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
                            color = Color(0xFFF472B6).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👗", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "বাল্ক ড্রেস অ্যাসাইন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$countStr টি অর্ডারে ক্যাটালগ ড্রেস বসান",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Catalog Search Field if more than 3 products
                if (catalogProducts.size > 3) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ড্রেস খুঁজুন...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (catalogProducts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ক্যাটালগে কোনো ড্রেস নেই। অনুগ্রহ করে প্রথমে 'প্রোডাক্টস' ট্যাব থেকে ড্রেস যুক্ত করুন।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Text(
                        text = "ক্যাটালগ থেকে ড্রেস নির্বাচন করুন:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Products Selection List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(filteredProducts, key = { it.id }) { prod ->
                            val isSelected = selectedProduct?.id == prod.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFFDF2F8) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF9D174D)) else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedProduct = prod
                                        overrideSize = null
                                        overrideColor = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (prod.photoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = prod.photoUri,
                                            contentDescription = prod.name,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFF831843) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "মূল্য: ৳${prod.basePrice.toInt()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF9D174D),
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                // SMART NON-DESTRUCTIVE MERGE TOGGLE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (keepExistingSizes) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, if (keepExistingSizes) Color(0xFF86EFAC) else Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "কাস্টমারের আগের সাইজ অক্ষত রাখুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (keepExistingSizes) Color(0xFF14532D) else Color(0xFF78350F)
                            )
                            Text(
                                text = if (keepExistingSizes)
                                    "✅ প্রতিটি অর্ডারের গ্রাহকের নিজস্ব সাইজ ও পরিমাণ বজায় থাকবে"
                                else
                                    "⚠️ নিচের সাইজ দিয়ে সব অর্ডারের সাইজ ওভাররাইট হবে",
                                fontSize = 10.sp,
                                color = if (keepExistingSizes) Color(0xFF15803D) else Color(0xFFB45309)
                            )
                        }

                        Switch(
                            checked = keepExistingSizes,
                            onCheckedChange = { keepExistingSizes = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF15803D),
                                checkedTrackColor = Color(0xFFBBF7D0)
                            )
                        )
                    }
                }

                // If keepExistingSizes is OFF: show override size chips
                if (!keepExistingSizes) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "সবগুলো অর্ডারে নির্দিষ্ট সাইজ বসান:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val availableSizeOptions = remember(selectedProduct) {
                        val productSizes = selectedProduct?.availableSizes?.filter { it.isNotBlank() } ?: emptyList()
                        val defaultSizes = listOf("36", "38", "40", "42", "44", "46", "Free")
                        (productSizes + defaultSizes).distinct()
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableSizeOptions.forEach { sz ->
                            val isSzSelected = overrideSize == sz
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSzSelected) Color(0xFF9D174D) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        overrideSize = if (isSzSelected) null else sz
                                    }
                            ) {
                                Text(
                                    text = sz,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSzSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSzSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Optional Color Selector
                val colorOptions = remember(selectedProduct) {
                    val pColors = selectedProduct?.availableColors?.filter { it.isNotBlank() } ?: emptyList()
                    val standardColors = listOf("মেরুন", "কালো", "নেভি ব্লু", "গোলাপি", "অফ-হোয়াইট")
                    (pColors + standardColors).distinct()
                }

                if (colorOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "কালার নির্বাচন করুন (অপশনাল):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        colorOptions.forEach { col ->
                            val isColSelected = overrideColor == col
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isColSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        overrideColor = if (isColSelected) null else col
                                    }
                            ) {
                                Text(
                                    text = col,
                                    fontSize = 11.sp,
                                    fontWeight = if (isColSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isColSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("বাতিল")
                    }

                    Button(
                        onClick = {
                            selectedProduct?.let { prod ->
                                onAssign(prod, keepExistingSizes, overrideSize, overrideColor)
                                onDismiss()
                            }
                        },
                        enabled = selectedProduct != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9D174D),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.8f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Apply to Selected Orders",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
