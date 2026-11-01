package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogProductEntity
import com.example.util.BanglaFormatter
import com.example.util.PhotoStorageManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductsScreen(
    catalogProducts: List<CatalogProductEntity>,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onSaveProduct: (CatalogProductEntity) -> Unit,
    onDeleteProduct: (CatalogProductEntity) -> Unit,
    onProductSelect: (CatalogProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<CatalogProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<CatalogProductEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ক্যাটালগ" else "Catalog",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    Button(
                        onClick = {
                            editingProduct = null
                            showAddEditDialog = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ড্রেস যোগ করুন" else "Add Dress",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
        ) {
            if (catalogProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (language == AppLanguage.BANGLA) "ক্যাটালগে কোনো ড্রেস নেই" else "No dresses in catalog",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                editingProduct = null
                                showAddEditDialog = true
                            }) {
                                Text(if (language == AppLanguage.BANGLA) "+ নতুন ড্রেস যুক্ত করুন" else "+ Add New Dress")
                            }
                        }
                    }
                }
            } else {
                items(catalogProducts, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (product.photoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = product.photoUri,
                                            contentDescription = product.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .size(36.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = product.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = BanglaFormatter.formatCurrency(product.basePrice, useBanglaDigits),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingProduct = product
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Dress",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            productToDelete = product
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Dress",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sizes & Stock
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) "সাইজ:" else "Size:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    product.availableSizes.forEach { sz ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = sz,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                val isInStock = product.stockStatus.contains("IN_STOCK") || product.stockStatus.contains("In Stock")
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isInStock) Color(0xFFDCFCE7) else Color(0xFFEDE9FE)
                                ) {
                                    Text(
                                        text = if (isInStock) (if (language == AppLanguage.BANGLA) "ইন স্টক" else "In Stock") else (if (language == AppLanguage.BANGLA) "প্রি-অর্ডার" else "Pre-Order"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isInStock) Color(0xFF15803D) else Color(0xFF6B46C1),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (product.availableColors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) "কালার:" else "Color:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = product.availableColors.joinToString(", "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onProductSelect(product) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AddShoppingCart, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.BANGLA) "এই ড্রেস দিয়ে নতুন অর্ডার নিন" else "Create Order with this Dress")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        ProductAddEditDialog(
            product = editingProduct,
            language = language,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedProduct ->
                onSaveProduct(updatedProduct)
                showAddEditDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(if (language == AppLanguage.BANGLA) "ড্রেস মুছে ফেলুন" else "Delete Dress") },
            text = { Text(if (language == AppLanguage.BANGLA) "'${productToDelete?.name}' কি ক্যাটালগ থেকে মুছে ফেলতে চান?" else "Are you sure you want to remove '${productToDelete?.name}' from the catalog?") },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = productToDelete
                        if (toRemove != null) {
                            onDeleteProduct(toRemove)
                            PhotoStorageManager.deleteCatalogPhotoIfLocal(context, toRemove.photoUri)
                        }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == AppLanguage.BANGLA) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text(if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductAddEditDialog(
    product: CatalogProductEntity?,
    language: AppLanguage = AppLanguage.BANGLA,
    onDismiss: () -> Unit,
    onSave: (CatalogProductEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Salwar Kameez") }
    var priceText by remember { mutableStateOf(if (product != null) product.basePrice.toInt().toString() else "1350") }
    var sizesText by remember { mutableStateOf(product?.availableSizes?.joinToString(", ") ?: "36, 38, 40, 42, 44, Free") }
    var colorsText by remember { mutableStateOf(product?.availableColors?.joinToString(", ") ?: (if (language == AppLanguage.BANGLA) "মেরুন, লাল, কালো, পেস্তা" else "Maroon, Red, Black, Green")) }
    var stockStatus by remember { mutableStateOf(product?.stockStatus ?: "IN_STOCK") }
    var photoUri by remember { mutableStateOf(product?.photoUri ?: "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_red") }
    var variantPhotos by remember { mutableStateOf(product?.variantPhotos?.toMutableMap() ?: mutableMapOf<String, String>()) }
    var colorForVariantPhoto by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val saved = PhotoStorageManager.saveCatalogPhoto(context, uri)
                if (saved != null) {
                    photoUri = saved
                }
            }
        }
    }

    val variantPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetColor = colorForVariantPhoto
        if (uri != null && targetColor != null) {
            scope.launch {
                val saved = PhotoStorageManager.saveCatalogPhoto(context, uri)
                if (saved != null) {
                    val updated = variantPhotos.toMutableMap()
                    updated[targetColor] = saved
                    variantPhotos = updated
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == AppLanguage.BANGLA) {
                    if (product == null) "নতুন ড্রেস যুক্ত করুন" else "ড্রেস এডিট করুন"
                } else {
                    if (product == null) "Add New Dress" else "Edit Dress"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Photo selection preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (photoUri.isNotBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == AppLanguage.BANGLA) "গ্যালারি থেকে ফটো দিন" else "Choose from Gallery", fontSize = 12.sp)
                        }

                        // Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            PresetDressButton(
                                name = "Red",
                                res = "sample_dress_red",
                                onSelect = { photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_red" }
                            )
                            PresetDressButton(
                                name = "Purple",
                                res = "sample_dress_purple",
                                onSelect = { photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_purple" }
                            )
                            PresetDressButton(
                                name = "Yellow",
                                res = "sample_dress_yellow",
                                onSelect = { photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_yellow" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == AppLanguage.BANGLA) "ড্রেসের নাম" else "Dress Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(if (language == AppLanguage.BANGLA) "ক্যাটাগরি (Gown, Kurti, Salwar Kameez, etc.)" else "Category (Gown, Kurti, Salwar Kameez, etc.)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (language == AppLanguage.BANGLA) "মূল্য (টাকা ৳)" else "Price (BDT ৳)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sizesText,
                    onValueChange = { sizesText = it },
                    label = { Text(if (language == AppLanguage.BANGLA) "সাইজ সমূহ (কমা দিয়ে যেমন: 36, 38, 40, 42, 44, Free)" else "Sizes (comma-separated, e.g. 36, 38, 40, Free)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = colorsText,
                    onValueChange = { colorsText = it },
                    label = { Text(if (language == AppLanguage.BANGLA) "কালার সমূহ (কমা দিয়ে যেমন: লাল, কালো, পেস্তা)" else "Colors (comma-separated, e.g. Red, Black, Green)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Multi-Color Variant Photos Section
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (language == AppLanguage.BANGLA) "কালার অনুযায়ী নির্দিষ্ট ছবি (Variant Photos):" else "Variant Photos by Color:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                val currentColors = colorsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (currentColors.isEmpty()) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "উপরে কালার লিখলে এখানে প্রতিটি কালারের আলাদা ছবি দেওয়ার অপশন আসবে।" else "Add colors above to upload variant photos per color.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        currentColors.forEach { c ->
                            val vPhoto = variantPhotos[c]
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable {
                                                    colorForVariantPhoto = c
                                                    variantPhotoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!vPhoto.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = vPhoto,
                                                    contentDescription = c,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(text = c, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                text = if (!vPhoto.isNullOrBlank()) {
                                                    if (language == AppLanguage.BANGLA) "নির্দিষ্ট ছবি যুক্ত আছে" else "Custom photo added"
                                                } else {
                                                    if (language == AppLanguage.BANGLA) "ডিফল্ট ছবি ব্যবহৃত হবে" else "Default photo will be used"
                                                },
                                                fontSize = 10.sp,
                                                color = if (!vPhoto.isNullOrBlank()) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            colorForVariantPhoto = c
                                            variantPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (!vPhoto.isNullOrBlank()) {
                                                if (language == AppLanguage.BANGLA) "পরিবর্তন" else "Change"
                                            } else {
                                                if (language == AppLanguage.BANGLA) "ছবি দিন" else "Add Photo"
                                            },
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(if (language == AppLanguage.BANGLA) "স্টক স্ট্যাটাস:" else "Stock Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { stockStatus = "IN_STOCK" }
                            .padding(end = 12.dp)
                    ) {
                        RadioButton(
                            selected = stockStatus == "IN_STOCK",
                            onClick = { stockStatus = "IN_STOCK" }
                        )
                        Text(if (language == AppLanguage.BANGLA) "ইন স্টক" else "In Stock", fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { stockStatus = "PRE_ORDER" }
                    ) {
                        RadioButton(
                            selected = stockStatus == "PRE_ORDER",
                            onClick = { stockStatus = "PRE_ORDER" }
                        )
                        Text(if (language == AppLanguage.BANGLA) "প্রি-অর্ডার" else "Pre-Order", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val sizes = sizesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val colors = colorsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val item = CatalogProductEntity(
                        id = product?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        category = category.trim(),
                        basePrice = price,
                        availableSizes = if (sizes.isEmpty()) listOf("Free") else sizes,
                        availableColors = colors,
                        variantPhotos = variantPhotos.toMap(),
                        stockStatus = stockStatus,
                        photoUri = photoUri
                    )
                    onSave(item)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (language == AppLanguage.BANGLA) "সংরক্ষণ করুন" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel")
            }
        }
    )
}

@Composable
private fun PresetDressButton(
    name: String,
    res: String,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable { onSelect() }
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
