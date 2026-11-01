package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogProductEntity
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderType
import com.example.data.parser.FieldConfidence
import com.example.data.parser.ParsedOrderDraft
import com.example.data.parser.ParsedOrderItem
import com.example.data.parser.SmartOrderParser
import com.example.util.BanglaFormatter


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartPasteScreen(
    allOrders: List<OrderEntity> = emptyList(),
    catalogProducts: List<CatalogProductEntity> = emptyList(),
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onNavigateBack: () -> Unit,
    onConfirmDraft: (ParsedOrderDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("smart_paste_prefs", Context.MODE_PRIVATE) }
    var isOfflineBannerDismissed by remember {
        mutableStateOf(prefs.getBoolean("offline_banner_dismissed", false))
    }

    var inputText by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(SmartOrderParser.parse(inputText)) }
    var selectedCatalogDress by remember { mutableStateOf<CatalogProductEntity?>(null) }
    var userManuallySelectedDress by remember { mutableStateOf(false) }
    var targetItemIndex by remember { mutableStateOf(0) }

    // 48-Hour Duplicate Order Guard
    val cleanPhone = remember(draft.customerPhone) {
        SmartOrderParser.normalizeDigits(draft.customerPhone).trim()
    }
    val fortyEightHoursAgo = remember { System.currentTimeMillis() - 48 * 60 * 60 * 1000L }
    val duplicateActiveOrder = remember(cleanPhone, allOrders) {
        if (cleanPhone.length == 11) {
            allOrders.firstOrNull { order ->
                val p = SmartOrderParser.normalizeDigits(order.customerPhone).trim()
                p == cleanPhone &&
                order.createdAt >= fortyEightHoursAgo &&
                (order.fulfillmentStatus == FulfillmentStatus.NEW ||
                 order.fulfillmentStatus == FulfillmentStatus.PROCESSING ||
                 order.fulfillmentStatus == FulfillmentStatus.READY_TO_SHIP)
            }
        } else null
    }

    // Multiple products matching parsed price (smart sorted: most recently ordered first)
    val samePriceCatalogProducts = remember(draft.unitPrice, catalogProducts, allOrders) {
        if (draft.unitPrice > 0) {
            val matched = catalogProducts.filter { it.basePrice == draft.unitPrice }
            if (matched.size > 1) {
                matched.sortedByDescending { dress ->
                    allOrders.filter { o ->
                        o.items.any { it.name.equals(dress.name, ignoreCase = true) }
                    }.maxOfOrNull { it.createdAt } ?: 0L
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    LaunchedEffect(inputText) {
        val parsed = SmartOrderParser.parse(inputText)
        if (!userManuallySelectedDress) {
            val nameMatch = if (parsed.dressName.isNotBlank() && parsed.dressName != "কটন সালোয়ার কামিজ") {
                catalogProducts.filter { cat ->
                    cat.name.contains(parsed.dressName, ignoreCase = true) ||
                    parsed.dressName.contains(cat.name, ignoreCase = true)
                }
            } else emptyList()

            val priceMatches = if (parsed.unitPrice > 0) {
                catalogProducts.filter { it.basePrice == parsed.unitPrice }
            } else emptyList()

            if (nameMatch.size == 1) {
                val cat = nameMatch.first()
                selectedCatalogDress = cat
                val finalColor = if (parsed.isColorExplicitInMessage && cat.availableColors.any { it.equals(parsed.color, ignoreCase = true) }) {
                    parsed.color
                } else (cat.availableColors.firstOrNull() ?: parsed.color)
                val finalSize = if (parsed.isSizeExplicitInMessage) parsed.size else (cat.availableSizes.firstOrNull() ?: parsed.size)
                val updatedItems = parsed.items.mapIndexed { idx, itm ->
                    if (idx == 0 || itm.dressName == "কটন সালোয়ার কামিজ" || itm.dressName.isBlank()) {
                        itm.copy(
                            dressName = cat.name,
                            photoUri = cat.photoUri,
                            color = if (itm.color.isNotBlank()) itm.color else finalColor,
                            size = if (itm.size.isNotBlank()) itm.size else finalSize
                        )
                    } else itm
                }
                draft = parsed.copy(
                    dressName = cat.name,
                    photoUri = cat.photoUri,
                    color = finalColor,
                    size = finalSize,
                    items = updatedItems
                )
            } else if (priceMatches.size == 1) {
                val cat = priceMatches.first()
                selectedCatalogDress = cat
                val finalColor = if (parsed.isColorExplicitInMessage && cat.availableColors.any { it.equals(parsed.color, ignoreCase = true) }) {
                    parsed.color
                } else (cat.availableColors.firstOrNull() ?: parsed.color)
                val finalSize = if (parsed.isSizeExplicitInMessage) parsed.size else (cat.availableSizes.firstOrNull() ?: parsed.size)
                val updatedItems = parsed.items.mapIndexed { idx, itm ->
                    if (idx == 0 || itm.dressName == "কটন সালোয়ার কামিজ" || itm.dressName.isBlank()) {
                        itm.copy(
                            dressName = cat.name,
                            photoUri = cat.photoUri,
                            color = if (itm.color.isNotBlank()) itm.color else finalColor,
                            size = if (itm.size.isNotBlank()) itm.size else finalSize
                        )
                    } else itm
                }
                draft = parsed.copy(
                    dressName = cat.name,
                    photoUri = cat.photoUri,
                    color = finalColor,
                    size = finalSize,
                    items = updatedItems
                )
            } else {
                selectedCatalogDress = null
                draft = parsed
            }
        } else if (selectedCatalogDress != null) {
            val cat = selectedCatalogDress!!
            val finalPrice = if (parsed.isPriceExplicitInMessage) parsed.unitPrice else cat.basePrice
            val finalSize = if (parsed.isSizeExplicitInMessage) parsed.size else (cat.availableSizes.firstOrNull() ?: parsed.size)
            val finalColor = if (parsed.isColorExplicitInMessage) parsed.color else (cat.availableColors.firstOrNull() ?: parsed.color)
            val updatedItems = parsed.items.mapIndexed { idx, itm ->
                if (idx == 0 || itm.dressName == "কটন সালোয়ার কামিজ" || itm.dressName.isBlank()) {
                    itm.copy(
                        dressName = cat.name,
                        photoUri = cat.photoUri,
                        unitPrice = if (parsed.isPriceExplicitInMessage) itm.unitPrice else cat.basePrice,
                        color = if (itm.color.isNotBlank()) itm.color else finalColor,
                        size = if (itm.size.isNotBlank()) itm.size else finalSize
                    )
                } else itm
            }
            val sub = updatedItems.sumOf { it.unitPrice * it.quantity }
            draft = parsed.copy(
                dressName = cat.name,
                photoUri = cat.photoUri,
                unitPrice = finalPrice,
                size = finalSize,
                color = finalColor,
                items = updatedItems,
                totalBill = (sub + parsed.deliveryCharge - parsed.advanceAmount).coerceAtLeast(0.0)
            )
        } else {
            draft = parsed
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "স্মার্ট পেস্ট পার্সার (অফলাইন)" else "Smart Paste Parser (Offline)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Dismissible Offline Info Banner
            if (!isOfflineBannerDismissed) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA)
                                "১০০% অফলাইন: কোনো ইন্টারনেট ছাড়াই ফেসবুক বা হোয়াটসঅ্যাপের অগোছালো মেসেজ থেকে নাম, ১১ ডিজিট ফোন, ঠিকানা, সাইজ এবং টাকা অটোমেটিক শনাক্ত হয়।"
                            else
                                "100% Offline: Automatically extracts customer name, 11-digit phone, address, size and delivery charge with zero latency.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                isOfflineBannerDismissed = true
                                prefs.edit().putBoolean("offline_banner_dismissed", true).apply()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Action: Paste from Clipboard Button
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text?.toString() ?: ""
                        if (text.isNotBlank()) {
                            inputText = text
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (language == AppLanguage.BANGLA) "ক্লিপবোর্ড থেকে পেস্ট করুন" else "Paste from Clipboard")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-line Input Field (starts empty with short 1-line placeholder)
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text(if (language == AppLanguage.BANGLA) "চ্যাট বা অর্ডার টেক্সট পেস্ট করুন" else "Paste Raw Chat Text Here") },
                placeholder = {
                    Text(if (language == AppLanguage.BANGLA) "কাস্টমারের মেসেজ এখানে পেস্ট করুন..." else "Paste customer message here...")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(14.dp)
            )

            // 48-HOUR DUPLICATE WARNING BANNER (Zero emoji in text)
            if (duplicateActiveOrder != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BANGLA)
                                    "সতর্কতা: এই নম্বরে ইতিমধ্যে একটি সক্রিয় অর্ডার (#${duplicateActiveOrder.invoiceNo}) রয়েছে!"
                                else
                                    "Warning: An active order (#${duplicateActiveOrder.invoiceNo}) already exists for this number!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = if (language == AppLanguage.BANGLA)
                                    "গত ৪৮ ঘণ্টার মধ্যে (${BanglaFormatter.formatDate(duplicateActiveOrder.createdAt, useBanglaDigits)}) অর্ডারটি প্লেস হয়েছে। ডুপ্লিকেট অর্ডার কি না যাচাই করুন।"
                                else
                                    "Placed within the last 48 hours (${BanglaFormatter.formatDate(duplicateActiveOrder.createdAt, useBanglaDigits)}). Verify if duplicate.",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // PARSER CONFIDENCE & AMBIGUITY WARNINGS
            if (draft.confidenceWarnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "পার্সার সতর্কতা ও স্পষ্টীকরণ:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        draft.confidenceWarnings.forEach { warn ->
                            Text(
                                text = "• $warn",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }

                        // Alternative Candidates Quick Resolvers (e.g. alternate phone or size)
                        if (draft.alternativeCandidates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            draft.alternativeCandidates.forEach { (fieldKey, options) ->
                                Text(
                                    text = "বিকল্প $fieldKey নির্বাচন করুন:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    options.forEach { opt ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    if (fieldKey == "phone") {
                                                        draft = draft.copy(customerPhone = opt)
                                                    } else if (fieldKey == "size") {
                                                        val cleanSz = opt.replace(Regex("""^[^\d]+"""), "").trim()
                                                        draft = draft.copy(size = cleanSz)
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = opt,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF92400E),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

            // QUICK CATALOG PICKER (Requirement 2: Intelligent Matching & Multiple Price Matches Handler)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val activeTargetIndex = targetItemIndex.coerceIn(0, (draft.items.size - 1).coerceAtLeast(0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ক্যাটালগ থেকে ড্রেস ও কালার সিলেক্ট করুন" else "Select Dress & Color from Catalog",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Multi-Item Target Tabs (Requirement 2)
                    if (draft.items.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "কোন আইটেমের জন্য ড্রেস বসাবেন:" else "Select target item to assign dress:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            itemsIndexed(draft.items) { idx, itm ->
                                val isTarget = idx == activeTargetIndex
                                val priceStr = if (itm.unitPrice > 0) BanglaFormatter.formatCurrency(itm.unitPrice, useBanglaDigits) else ""
                                val label = if (language == AppLanguage.BANGLA) {
                                    "আইটেম ${idx + 1}" + (if (priceStr.isNotBlank()) " ($priceStr)" else "")
                                } else {
                                    "Item ${idx + 1}" + (if (priceStr.isNotBlank()) " ($priceStr)" else "")
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isTarget) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { targetItemIndex = idx }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        if (!itm.photoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = itm.photoUri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isTarget) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Warning & filter banner if multiple products share the exact parsed unit price
                    val hasSamePriceConflict = samePriceCatalogProducts.size > 1 && selectedCatalogDress == null
                    if (hasSamePriceConflict) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BANGLA)
                                        "৳${draft.unitPrice.toInt()} দামের ${samePriceCatalogProducts.size}টি ড্রেস পাওয়া গেছে — ছবি দেখে সিলেক্ট করুন:"
                                    else
                                        "Found ${samePriceCatalogProducts.size} dresses priced at ৳${draft.unitPrice.toInt()} — tap to select:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (language == AppLanguage.BANGLA)
                                "ড্রেস সিলেক্ট করলে ক্যাটালগের সংরক্ষিত ফটো ও নাম বসে যাবে (সাইজ ও মূল্য অপরিবর্তিত থাকবে)"
                            else
                                "Selecting a dress applies the saved photo and name (size and price remain unchanged)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                    }

                    val displayDresses = if (hasSamePriceConflict) {
                        samePriceCatalogProducts
                    } else {
                        catalogProducts
                    }

                    if (displayDresses.isEmpty()) {
                        Text(
                            text = if (language == AppLanguage.BANGLA)
                                "ক্যাটালগে কোনো ড্রেস নেই। 'প্রোডাক্টস' ট্যাব থেকে ড্রেস যোগ করুন।"
                            else
                                "No dresses in catalog. Add dresses from the 'Products' tab.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(displayDresses, key = { it.id }) { cat ->
                                val currentTargetItem = draft.items.getOrNull(activeTargetIndex)
                                val isSelected = (selectedCatalogDress?.id == cat.id) ||
                                        (currentTargetItem != null && currentTargetItem.dressName.equals(cat.name, ignoreCase = true))
                                val isConflictItem = hasSamePriceConflict
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isConflictItem) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else if (isConflictItem) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)) else null,
                                    modifier = Modifier
                                        .width(if (isConflictItem) 155.dp else 140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            userManuallySelectedDress = true
                                            selectedCatalogDress = cat

                                            // Determine target item to assign
                                            val assignedIdx = if (draft.items.size > 1) {
                                                val currItem = draft.items.getOrNull(activeTargetIndex)
                                                val matchingPriceIdx = draft.items.indices.firstOrNull { i ->
                                                    i != activeTargetIndex && draft.items[i].unitPrice == cat.basePrice &&
                                                    (draft.items[i].photoUri.isNullOrBlank() || draft.items[i].dressName == "নতুন ড্রেস" || draft.items[i].dressName == "কটন সালোয়ার কামিজ")
                                                }
                                                if (currItem != null && currItem.unitPrice == cat.basePrice) {
                                                    activeTargetIndex
                                                } else if (matchingPriceIdx != null && (currItem?.photoUri != null && !currItem.photoUri.isNullOrBlank())) {
                                                    targetItemIndex = matchingPriceIdx
                                                    matchingPriceIdx
                                                } else {
                                                    activeTargetIndex
                                                }
                                            } else {
                                                0
                                            }

                                            val targetItm = draft.items.getOrNull(assignedIdx)
                                            val finalPrice = if (targetItm != null && targetItm.unitPrice > 0) targetItm.unitPrice else cat.basePrice
                                            val finalSize = if (targetItm != null && targetItm.size.isNotBlank() && targetItm.size != "Free") targetItm.size else (cat.availableSizes.firstOrNull() ?: "Free")
                                            val finalColor = if (targetItm != null && targetItm.color.isNotBlank()) targetItm.color else (cat.availableColors.firstOrNull() ?: "")

                                            val updatedItems = if (draft.items.isNotEmpty()) {
                                                draft.items.mapIndexed { idx, itm ->
                                                    if (idx == assignedIdx) {
                                                        itm.copy(
                                                            dressName = cat.name,
                                                            photoUri = cat.photoUri,
                                                            unitPrice = finalPrice,
                                                            size = finalSize,
                                                            color = finalColor
                                                        )
                                                    } else itm
                                                }
                                            } else {
                                                listOf(ParsedOrderItem(dressName = cat.name, photoUri = cat.photoUri, unitPrice = finalPrice, size = finalSize, color = finalColor))
                                            }

                                            val totalSub = updatedItems.sumOf { it.unitPrice * it.quantity }
                                            val newTotal = (totalSub + draft.deliveryCharge - draft.advanceAmount).coerceAtLeast(0.0)
                                            draft = draft.copy(
                                                items = updatedItems,
                                                dressName = if (assignedIdx == 0) cat.name else draft.dressName,
                                                photoUri = if (assignedIdx == 0) cat.photoUri else draft.photoUri,
                                                unitPrice = if (assignedIdx == 0) finalPrice else draft.unitPrice,
                                                size = if (assignedIdx == 0) finalSize else draft.size,
                                                color = if (assignedIdx == 0) finalColor else draft.color,
                                                totalBill = newTotal
                                            )

                                            // Auto-advance target tab to next unassigned item
                                            if (draft.items.size > 1) {
                                                val nextUnassigned = updatedItems.indices.firstOrNull { i ->
                                                    i > assignedIdx && (updatedItems[i].photoUri.isNullOrBlank() || updatedItems[i].dressName == "নতুন ড্রেস" || updatedItems[i].dressName == "কটন সালোয়ার কামিজ")
                                                } ?: updatedItems.indices.firstOrNull { i ->
                                                    i != assignedIdx && (updatedItems[i].photoUri.isNullOrBlank() || updatedItems[i].dressName == "নতুন ড্রেস" || updatedItems[i].dressName == "কটন সালোয়ার কামিজ")
                                                }
                                                if (nextUnassigned != null) {
                                                    targetItemIndex = nextUnassigned
                                                }
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(if (isConflictItem) 85.dp else 75.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            if (cat.photoUri.isNotBlank()) {
                                                AsyncImage(
                                                    model = cat.photoUri,
                                                    contentDescription = cat.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Inventory2,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(28.dp).align(Alignment.Center)
                                                )
                                            }

                                            if (isSelected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(4.dp).align(Alignment.TopEnd)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp).padding(2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = BanglaFormatter.formatCurrency(cat.basePrice, useBanglaDigits),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Variation color chips for the active target item
                    val activeDress = selectedCatalogDress ?: catalogProducts.firstOrNull {
                        it.name.equals(draft.items.getOrNull(activeTargetIndex)?.dressName, ignoreCase = true)
                    }

                    activeDress?.let { cat ->
                        val targetItem = draft.items.getOrNull(activeTargetIndex)
                        if (cat.availableColors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == AppLanguage.BANGLA) "কালার ভেরিয়েশন নির্বাচন করুন:" else "Select Color Variation:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                cat.availableColors.forEach { c ->
                                    val isColorSelected = targetItem?.color.equals(c, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isColorSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val variantPhoto = cat.variantPhotos[c]
                                                val photoToUse = if (!variantPhoto.isNullOrBlank()) variantPhoto else cat.photoUri
                                                val newItems = draft.items.mapIndexed { i, itm ->
                                                    if (i == activeTargetIndex) itm.copy(color = c, photoUri = photoToUse) else itm
                                                }
                                                draft = draft.copy(
                                                    color = if (activeTargetIndex == 0) c else draft.color,
                                                    photoUri = if (activeTargetIndex == 0) photoToUse else draft.photoUri,
                                                    items = newItems
                                                )
                                            }
                                    ) {
                                        Text(
                                            text = c,
                                            fontSize = 11.sp,
                                            fontWeight = if (isColorSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isColorSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Size variation chips for active target item
                        if (cat.availableSizes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val isSizeExplicit = targetItem?.size.isNullOrBlank().not() && targetItem?.size != "Free"
                            Text(
                                text = if (language == AppLanguage.BANGLA) {
                                    if (isSizeExplicit) "সাইজ (${targetItem?.size}) নির্বাচিত (পরিবর্তন করুন):" else "সাইজ নির্বাচন করুন:"
                                } else {
                                    if (isSizeExplicit) "Size (${targetItem?.size}) selected (tap to change):" else "Select Size:"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                cat.availableSizes.forEach { sz ->
                                    val isSizeSelected = targetItem?.size.equals(sz, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSizeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val newItems = draft.items.mapIndexed { i, itm ->
                                                    if (i == activeTargetIndex) itm.copy(size = sz) else itm
                                                }
                                                draft = draft.copy(
                                                    size = if (activeTargetIndex == 0) sz else draft.size,
                                                    items = newItems
                                                )
                                            }
                                    ) {
                                        Text(
                                            text = sz,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSizeSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSizeSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time Extracted Data Preview Card
            Text(
                text = if (language == AppLanguage.BANGLA) "শনাক্তকৃত তথ্য (লাইভ প্রিভিউ)" else "Extracted Data (Live Preview)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    ExtractedItemRow(
                        label = if (language == AppLanguage.BANGLA) "গ্রাহকের নাম" else "Name",
                        value = draft.customerName,
                        isValid = draft.customerName.isNotBlank()
                    )
                    ExtractedItemRow(
                        label = if (language == AppLanguage.BANGLA) "১১ ডিজিট মোবাইল" else "Phone (11 Digits)",
                        value = draft.customerPhone,
                        isValid = draft.customerPhone.length == 11
                    )
                    val isDistrictConfirmed = draft.fieldConfidences["district"] == FieldConfidence.EXPLICIT
                    ExtractedItemRow(
                        label = if (language == AppLanguage.BANGLA) "ঠিকানা ও জেলা" else "Address & District",
                        value = if (isDistrictConfirmed) {
                            "${draft.customerAddress} (${draft.district})"
                        } else {
                            "${draft.customerAddress} (${draft.district} - ${if (language == AppLanguage.BANGLA) "অনিশ্চিত জেলা" else "Unconfirmed"})"
                        },
                        isValid = draft.customerAddress.isNotBlank() && isDistrictConfirmed
                    )
                    if (draft.thana.isNotBlank()) {
                        ExtractedItemRow(
                            label = if (language == AppLanguage.BANGLA) "থানা / উপজেলা" else "Thana / Upazila",
                            value = draft.thana,
                            isValid = true
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Multi-Item Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ড্রেস ও আইটেমসমূহ (${draft.items.size}টি)" else "Ordered Dresses (${draft.items.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = {
                                val newItem = ParsedOrderItem(
                                    dressName = if (language == AppLanguage.BANGLA) "নতুন ড্রেস" else "New Dress",
                                    unitPrice = 0.0,
                                    quantity = 1,
                                    size = "Free",
                                    color = ""
                                )
                                val newItems = draft.items + newItem
                                val newSub = newItems.sumOf { it.unitPrice * it.quantity }
                                val newTotal = (newSub + draft.deliveryCharge - draft.advanceAmount).coerceAtLeast(0.0)
                                targetItemIndex = newItems.lastIndex
                                draft = draft.copy(
                                    items = newItems,
                                    totalBill = newTotal
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.BANGLA) "আইটেম যোগ করুন" else "Add Item",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Multi-Item Cards
                    val activeTargetIndex = targetItemIndex.coerceIn(0, (draft.items.size - 1).coerceAtLeast(0))
                    draft.items.forEachIndexed { index, item ->
                        val isItemTargeted = index == activeTargetIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isItemTargeted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isItemTargeted) 1.5.dp else 1.dp,
                                if (isItemTargeted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Item Header (Index, Subtotal, Catalog Button & Delete)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isItemTargeted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == AppLanguage.BANGLA)
                                                "আইটেম ${index + 1}: ${BanglaFormatter.formatCurrency(item.unitPrice * item.quantity, useBanglaDigits)}"
                                            else
                                                "Item ${index + 1}: ${BanglaFormatter.formatCurrency(item.unitPrice * item.quantity, useBanglaDigits)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (item.quantity > 1) {
                                            Text(
                                                text = if (language == AppLanguage.BANGLA) " (${item.quantity} পিস)" else " (${item.quantity} pcs)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { targetItemIndex = index },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = if (isItemTargeted) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (language == AppLanguage.BANGLA) "ক্যাটালগ দিন" else "Select from Catalog",
                                                fontSize = 10.sp,
                                                fontWeight = if (isItemTargeted) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        if (draft.items.size > 1) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    val newItems = draft.items.toMutableList().apply { removeAt(index) }
                                                    val newSub = newItems.sumOf { it.unitPrice * it.quantity }
                                                    val newTotal = (newSub + draft.deliveryCharge - draft.advanceAmount).coerceAtLeast(0.0)
                                                    draft = draft.copy(
                                                        items = newItems,
                                                        dressName = newItems.firstOrNull()?.dressName ?: "",
                                                        unitPrice = newItems.firstOrNull()?.unitPrice ?: 0.0,
                                                        size = newItems.firstOrNull()?.size ?: "",
                                                        color = newItems.firstOrNull()?.color ?: "",
                                                        photoUri = newItems.firstOrNull()?.photoUri,
                                                        totalBill = newTotal
                                                    )
                                                    if (targetItemIndex >= newItems.size) {
                                                        targetItemIndex = (newItems.size - 1).coerceAtLeast(0)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove Item",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Item Dress Name & Thumbnail
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!item.photoUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.photoUri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    OutlinedTextField(
                                        value = item.dressName,
                                        onValueChange = { newName ->
                                            val newItems = draft.items.mapIndexed { i, itm ->
                                                if (i == index) itm.copy(dressName = newName) else itm
                                            }
                                            draft = draft.copy(
                                                items = newItems,
                                                dressName = if (index == 0) newName else draft.dressName
                                            )
                                        },
                                        label = { Text(if (language == AppLanguage.BANGLA) "ড্রেসের নাম" else "Dress Name", fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Size row & Quick chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) "সাইজ: " else "Size: ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.size.ifBlank { if (language == AppLanguage.BANGLA) "উল্লেখ নেই (খালি)" else "Not specified" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.size.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 3.dp, bottom = 4.dp)
                                ) {
                                    listOf("36", "38", "40", "42", "44", "46", "Free").forEach { sz ->
                                        val isSzSelected = item.size.equals(sz, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSzSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSzSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val newItems = draft.items.mapIndexed { i, itm ->
                                                        if (i == index) itm.copy(size = sz) else itm
                                                    }
                                                    draft = draft.copy(
                                                        items = newItems,
                                                        size = if (index == 0) sz else draft.size
                                                    )
                                                }
                                        ) {
                                            Text(
                                                text = sz,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSzSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSzSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Color, Price, and Quantity fields
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.color,
                                        onValueChange = { newCol ->
                                            val newItems = draft.items.mapIndexed { i, itm ->
                                                if (i == index) itm.copy(color = newCol) else itm
                                            }
                                            draft = draft.copy(
                                                items = newItems,
                                                color = if (index == 0) newCol else draft.color
                                            )
                                        },
                                        label = { Text(if (language == AppLanguage.BANGLA) "কালার" else "Color", fontSize = 10.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = if (item.unitPrice > 0) item.unitPrice.toInt().toString() else "",
                                        onValueChange = { newP ->
                                            val pVal = newP.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                            val newItems = draft.items.mapIndexed { i, itm ->
                                                if (i == index) itm.copy(unitPrice = pVal) else itm
                                            }
                                            val newSub = newItems.sumOf { it.unitPrice * it.quantity }
                                            val newTotal = (newSub + draft.deliveryCharge - draft.advanceAmount).coerceAtLeast(0.0)
                                            draft = draft.copy(
                                                items = newItems,
                                                unitPrice = if (index == 0) pVal else draft.unitPrice,
                                                totalBill = newTotal
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (!draft.isPriceExplicitInMessage) {
                                                    if (language == AppLanguage.BANGLA) "দাম (আনুমানিক ৳)" else "Price (Est. ৳)"
                                                } else {
                                                    if (language == AppLanguage.BANGLA) "দাম (৳)" else "Price (৳)"
                                                },
                                                fontSize = 10.sp
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = item.quantity.toString(),
                                        onValueChange = { qtyStr ->
                                            val q = qtyStr.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                            val newItems = draft.items.mapIndexed { i, itm ->
                                                if (i == index) itm.copy(quantity = q) else itm
                                            }
                                            val newSub = newItems.sumOf { it.unitPrice * it.quantity }
                                            val newTotal = (newSub + draft.deliveryCharge - draft.advanceAmount).coerceAtLeast(0.0)
                                            draft = draft.copy(
                                                items = newItems,
                                                totalBill = newTotal
                                            )
                                        },
                                        label = { Text(if (language == AppLanguage.BANGLA) "পিস" else "Qty", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(60.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Accurate Financial Totals
                    val itemsSubtotal = draft.items.sumOf { it.unitPrice * it.quantity }
                    ExtractedItemRow(
                        label = if (draft.isPriceExplicitInMessage) {
                            if (language == AppLanguage.BANGLA) "ড্রেসের মোট মূল্য (${draft.items.size}টি আইটেম)" else "Items Subtotal (${draft.items.size} items)"
                        } else {
                            if (language == AppLanguage.BANGLA) "ড্রেসের মোট মূল্য (${draft.items.size}টি আইটেম - আনুমানিক)" else "Items Subtotal (${draft.items.size} items - Est.)"
                        },
                        value = BanglaFormatter.formatCurrency(itemsSubtotal, useBanglaDigits),
                        isValid = draft.isPriceExplicitInMessage,
                        emptyText = if (language == AppLanguage.BANGLA) "খালি" else "Empty"
                    )
                    ExtractedItemRow(
                        label = if (language == AppLanguage.BANGLA) "ডেলিভারি চার্জ" else "Delivery Charge",
                        value = BanglaFormatter.formatCurrency(draft.deliveryCharge, useBanglaDigits),
                        isValid = true,
                        emptyText = if (language == AppLanguage.BANGLA) "খালি" else "Empty"
                    )
                    ExtractedItemRow(
                        label = if (language == AppLanguage.BANGLA) "সর্বমোট বিল" else "Total Bill",
                        value = BanglaFormatter.formatCurrency(itemsSubtotal + draft.deliveryCharge, useBanglaDigits),
                        isValid = true,
                        emptyText = if (language == AppLanguage.BANGLA) "খালি" else "Empty"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // MANUAL ADVANCE PAYMENT INPUT (Requirement 1: Short placeholder & English support)
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ম্যানুয়াল অগ্রিম টাকা:" else "Manual Advance:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    var advanceInputText by remember(draft.advanceAmount) {
                        mutableStateOf(if (draft.advanceAmount > 0) draft.advanceAmount.toInt().toString() else "")
                    }

                    OutlinedTextField(
                        value = advanceInputText,
                        onValueChange = { input ->
                            advanceInputText = input
                            val adv = input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                            val fullBill = itemsSubtotal + draft.deliveryCharge
                            draft = draft.copy(
                                advanceAmount = adv,
                                totalBill = (fullBill - adv).coerceAtLeast(0.0)
                            )
                        },
                        label = { Text(if (language == AppLanguage.BANGLA) "অগ্রিম টাকা" else "Advance Amount") },
                        placeholder = { Text(if (language == AppLanguage.BANGLA) "এমাউন্ট লিখুন" else "Enter amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 6.dp)
                    )

                    // Quick Advance Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        listOf(0, 100, 200, 500, 1000).forEach { chipVal ->
                            val isChipSelected = draft.advanceAmount.toInt() == chipVal
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChipSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        advanceInputText = if (chipVal == 0) "" else chipVal.toString()
                                        val fullBill = itemsSubtotal + draft.deliveryCharge
                                        draft = draft.copy(
                                            advanceAmount = chipVal.toDouble(),
                                            totalBill = (fullBill - chipVal.toDouble()).coerceAtLeast(0.0)
                                        )
                                    }
                            ) {
                                Text(
                                    text = "৳$chipVal",
                                    fontSize = 11.sp,
                                    fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChipSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Live COD Due Box
                    val codDue = (itemsSubtotal + draft.deliveryCharge - draft.advanceAmount).coerceAtLeast(0.0)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "ক্যাশ অন ডেলিভারি (বাকি COD)" else "Cash on Delivery (COD Due)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "কাস্টমার ডেলিভারির সময় রাইডারকে প্রদান করবেন" else "Customer will pay the rider upon delivery",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = BanglaFormatter.formatCurrency(codDue, useBanglaDigits),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (draft.specialInstructions.isNotBlank()) {
                        ExtractedItemRow(
                            label = if (language == AppLanguage.BANGLA) "বিশেষ নোট" else "Special Note",
                            value = draft.specialInstructions,
                            isValid = true,
                            emptyText = if (language == AppLanguage.BANGLA) "খালি" else "Empty"
                        )
                    }
                    ExtractedItemRow(
                        label = if (language == AppLanguage.BANGLA) "অর্ডার টাইপ" else "Order Type",
                        value = if (draft.orderType == OrderType.PRE_ORDER) {
                            if (language == AppLanguage.BANGLA) "প্রি-অর্ডার" else "Pre-Order"
                        } else {
                            if (language == AppLanguage.BANGLA) "রেগুলার" else "Regular"
                        },
                        isValid = true,
                        emptyText = if (language == AppLanguage.BANGLA) "খালি" else "Empty"
                    )
                }
            }


            Spacer(modifier = Modifier.height(20.dp))

            // Confirm & Create Order Button
            Button(
                onClick = { onConfirmDraft(draft) },
                enabled = draft.customerPhone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.BANGLA) "এই তথ্য দিয়ে অর্ডার বানান ➔" else "Create Order with this info ➔",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ExtractedItemRow(
    label: String,
    value: String,
    isValid: Boolean,
    emptyText: String = "Empty"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.42f, fill = false)
        ) {
            Icon(
                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isValid) Color(0xFF15803D) else Color(0xFFE11D48),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value.ifBlank { emptyText },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.58f, fill = false),
            color = if (value.isBlank()) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
        )
    }
}
