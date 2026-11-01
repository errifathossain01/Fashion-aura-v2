package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogProductEntity
import com.example.data.model.OrderEntity
import com.example.data.model.UserPreferences
import com.example.util.ExportHelper
import com.example.util.MessagingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    trashedOrders: List<OrderEntity>,
    pendingSyncCount: Int,
    balanceState: String?,
    isCheckingBalance: Boolean,
    onSaveSteadfastCredentials: (String, String) -> Unit,
    onCheckBalance: (String, String) -> Unit,
    onBulkSyncPending: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onDigitChange: (Boolean) -> Unit,
    onRestoreOrder: (Long) -> Unit,
    onDeletePermanently: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    onRestoreJson: (String) -> Unit = {},
    onRestoreBackupStream: (java.io.InputStream) -> Unit = {},
    allOrders: List<OrderEntity>,
    catalogProducts: List<CatalogProductEntity> = emptyList(),
    onNavigateToLoginPreview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isBn = userPreferences.appLanguage == AppLanguage.BANGLA
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf("") }

    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            try {
                val outStream = context.contentResolver.openOutputStream(uri)
                val zipResult = ExportHelper.exportFullZipBackup(context, outStream, allOrders, catalogProducts)
                zipResult.fold(
                    onSuccess = { summary ->
                        exportSuccessMessage = if (isBn) {
                            "ZIP ব্যাকআপ সফলভাবে সম্পন্ন হয়েছে!\n\n• মোট অর্ডার: ${summary.totalOrders}টি\n• ক্যাটালগ ড্রেস: ${summary.totalCatalogProducts}টি\n• সংরক্ষিত ছবি: ${summary.archivedPhotosCount}টি"
                        } else {
                            "ZIP Backup completed successfully!\n\n• Total Orders: ${summary.totalOrders}\n• Catalog Dresses: ${summary.totalCatalogProducts}\n• Photos Archived: ${summary.archivedPhotosCount}"
                        }
                        showExportSuccessDialog = true
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            if (isBn) "ZIP ব্যাকআপ তৈরিতে ত্রুটি: ${error.message}" else "Error creating ZIP backup: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isBn) "ZIP ব্যাকআপ তৈরিতে ত্রুটি: ${e.message}" else "Error creating ZIP backup: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val outStream = context.contentResolver.openOutputStream(uri)
                val jsonResult = ExportHelper.exportFullBackupToJsonStream(outStream, allOrders, catalogProducts)
                jsonResult.fold(
                    onSuccess = {
                        exportSuccessMessage = if (isBn) {
                            "JSON ব্যাকআপ সফলভাবে সেভ হয়েছে!\n\n• মোট অর্ডার: ${allOrders.size}টি\n• ক্যাটালগ ড্রেস: ${catalogProducts.size}টি"
                        } else {
                            "JSON Backup saved successfully!\n\n• Total Orders: ${allOrders.size}\n• Catalog Dresses: ${catalogProducts.size}"
                        }
                        showExportSuccessDialog = true
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            if (isBn) "JSON ব্যাকআপ তৈরিতে ত্রুটি: ${error.message}" else "Error creating JSON backup: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isBn) "JSON সেভ করার সময় ত্রুটি: ${e.message}" else "Error saving JSON: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    onRestoreBackupStream(stream)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isBn) "ফাইল পড়তে সমস্যা হয়েছে: ${e.message}" else "Error reading file: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    var apiKeyInput by remember(userPreferences.steadfastApiKey) { mutableStateOf(userPreferences.steadfastApiKey) }
    var secretKeyInput by remember(userPreferences.steadfastSecretKey) { mutableStateOf(userPreferences.steadfastSecretKey) }
    var showTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBn) "সেটিংস ও কুরিয়ার কনফিগারেশন" else "Settings & Courier Config",
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
            // 1. STEADFAST COURIER INTEGRATION CARD
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
                                text = "Steadfast Courier API Integration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = if (isBn) {
                                "অর্ডার স্বয়ংক্রিয়ভাবে কুরিয়ারে বুকিং ও Consignment ID জেনারেট করতে আপনার Steadfast মার্চেন্ট ক্রেডেনশিয়াল দিন।"
                            } else {
                                "Enter your Steadfast Merchant credentials to automatically book orders and generate Consignment IDs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Steadfast Api-Key") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = secretKeyInput,
                            onValueChange = { secretKeyInput = it },
                            label = { Text("Steadfast Secret-Key") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSaveSteadfastCredentials(apiKeyInput.trim(), secretKeyInput.trim())
                                    Toast.makeText(
                                        context,
                                        if (isBn) "API কী সফলভাবে সেভ হয়েছে!" else "API Keys saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "কী সেভ করুন" else "Save Keys")
                            }

                            OutlinedButton(
                                onClick = { onCheckBalance(apiKeyInput.trim(), secretKeyInput.trim()) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCheckingBalance) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBn) "ব্যালেন্স টেস্ট" else "Test Balance")
                                }
                            }
                        }

                        if (balanceState != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = balanceState,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Bulk Sync Pending Queue
                        if (pendingSyncCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isBn) "পেন্ডিং কুরিয়ার সিঙ্ক কিউ" else "Pending Courier Sync Queue",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBn) "$pendingSyncCount টি অর্ডার অফলাইনে পেন্ডিং রয়েছে" else "$pendingSyncCount orders pending sync",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = onBulkSyncPending,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBn) "১-ট্যাপ বাল্ক সিঙ্ক" else "1-Tap Bulk Sync")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // TEMPORARY AUTHENTICATION UI PREVIEW (STAGE B TESTING)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "অথেনটিকেশন (UI প্রিভিউ টেস্ট)" else "Authentication (UI Preview)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = if (isBn) {
                                "লগইন স্ক্রিনের ডিজাইন, ইমেইল ভেরিফিকেশন ও পাসওয়ার্ড রিসেট ডায়ালগ টেস্ট করুন। এটি বিদ্যমান ড্যাশবোর্ড বা একাউন্ট পরিবর্তন করে না।"
                            } else {
                                "Preview and test the Login Screen UI, verification pending card, and password reset dialog. This does not alter existing Dashboard access."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onNavigateToLoginPreview,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preview Login Screen (UI Test)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. APP PREFERENCES CARD (Dark Mode, Language)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBn) "অ্যাপ ও ভাষা প্রিফারেন্স" else "App & Language Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dark Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(if (isBn) "ডার্ক মোড" else "Dark Mode", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isBn) "চোখের স্বাচ্ছন্দ্যের জন্য ডার্ক থিম ব্যবহার করুন" else "Use dark theme for low-light comfort",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = userPreferences.isDarkMode == true,
                                onCheckedChange = { onToggleDarkMode() }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // App Language Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(if (isBn) "ভাষা" else "App Language", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isBn) "বাংলা সক্রিয় রয়েছে" else "English is active",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (userPreferences.appLanguage == AppLanguage.BANGLA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onLanguageChange(AppLanguage.BANGLA) }
                                ) {
                                    Text(
                                        text = "বাংলা",
                                        fontWeight = FontWeight.Bold,
                                        color = if (userPreferences.appLanguage == AppLanguage.BANGLA) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (userPreferences.appLanguage == AppLanguage.ENGLISH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onLanguageChange(AppLanguage.ENGLISH) }
                                ) {
                                    Text(
                                        text = "English",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (userPreferences.appLanguage == AppLanguage.ENGLISH) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. BACKUP, EXPORT & TRASH CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBn) "ডাটা ব্যাকআপ ও রিসাইকেল বিন" else "Data Backup & Recycle Bin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. ZIP Backup (Photos + Database)
                        Button(
                            onClick = {
                                createZipLauncher.launch("fashion_aura_backup.zip")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "ছবি + ডাটাবেজ এক ফাইলে ZIP ব্যাকআপ নিন" else "Backup ZIP (Photos + Database)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Restore (ZIP or JSON)
                        OutlinedButton(
                            onClick = {
                                restoreFileLauncher.launch(arrayOf(
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/octet-stream",
                                    "application/json",
                                    "text/*",
                                    "*/*"
                                ))
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "ZIP বা JSON ব্যাকআপ রিস্টোর করুন" else "Restore Backup (ZIP or JSON)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. JSON Backup File
                        OutlinedButton(
                            onClick = {
                                createJsonLauncher.launch("fashion_aura_backup.json")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "JSON ফাইল সেভ করুন (.json)" else "Save JSON Backup File (.json)"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. JSON Clipboard Copy
                        OutlinedButton(
                            onClick = {
                                ExportHelper.exportFullBackupToJson(allOrders, catalogProducts).fold(
                                    onSuccess = { json ->
                                        MessagingHelper.copyToClipboard(context, "Fashion Aura Full Backup", json)
                                        Toast.makeText(
                                            context,
                                            if (isBn) "সম্পূর্ণ ডাটাবেজ ও ক্যাটালগ JSON ব্যাকআপ কপি হয়েছে!" else "Full database & catalog JSON copied to clipboard!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            context,
                                            if (isBn) "JSON ব্যাকআপ তৈরিতে ত্রুটি: ${error.message}" else "Error generating JSON backup: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "সম্পূর্ণ ডাটাবেজ ও ক্যাটালগ JSON কপি করুন" else "Copy Full Database & Catalog JSON"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. CSV Export
                        OutlinedButton(
                            onClick = {
                                val csv = ExportHelper.exportOrdersToCsv(allOrders)
                                MessagingHelper.copyToClipboard(context, "Orders CSV", csv)
                                Toast.makeText(
                                    context,
                                    if (isBn) "CSV ডাটা ক্লিপবোর্ডে কপি হয়েছে!" else "CSV data copied to clipboard!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "১-ট্যাপ CSV এক্সপোর্ট (Excel/Sheets)" else "1-Tap CSV Export (Excel/Sheets)"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Trash / Recycle Bin
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "রিসাইকেল বিন (${trashedOrders.size}টি ড্রাফট)" else "Recycle Bin (${trashedOrders.size} drafts)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { showTrashDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isBn) "বিন দেখুন" else "View Bin",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = {
                Text(
                    if (isBn) "রিসাইকেল বিন (${trashedOrders.size}টি ট্র্যাশড অর্ডার)" else "Recycle Bin (${trashedOrders.size} trashed orders)"
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    if (trashedOrders.isEmpty()) {
                        item {
                            Text(
                                text = if (isBn) "রিসাইকেল বিন সম্পূর্ণ খালি।" else "Recycle Bin is completely empty.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(trashedOrders) { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(order.invoiceNo, fontWeight = FontWeight.Bold)
                                    Text(order.customerName, fontSize = 12.sp)
                                }
                                IconButton(onClick = { onRestoreOrder(order.id) }) {
                                    Icon(Icons.Default.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeletePermanently(order.id) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                if (trashedOrders.isNotEmpty()) {
                    TextButton(onClick = {
                        showEmptyTrashConfirmDialog = true
                    }) {
                        Text(
                            text = if (isBn) "বিন সম্পূর্ণ খালি করুন" else "Empty Recycle Bin",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashDialog = false }) {
                    Text(if (isBn) "বন্ধ করুন" else "Close")
                }
            }
        )
    }

    // Export Confirmation Dialog
    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            title = {
                Text(
                    text = if (isBn) "ব্যাকআপ সম্পন্ন হয়েছে" else "Backup Completed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = exportSuccessMessage, fontSize = 13.sp)
            },
            confirmButton = {
                Button(onClick = { showExportSuccessDialog = false }) {
                    Text(if (isBn) "ঠিক আছে" else "OK")
                }
            }
        )
    }

    // Empty Trash Safety Confirmation Dialog
    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            title = {
                Text(
                    text = if (isBn) "রিসাইকেল বিন সম্পূর্ণ খালি করুন" else "Empty Recycle Bin",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (isBn) {
                        "আপনি কি নিশ্চিতভাবে ট্র্যাশের সমস্ত (${trashedOrders.size}টি) ড্রাফট মুছে ফেলতে চান?\n\nডাটা সুরক্ষার স্বার্থে মুছে ফেলার পূর্বে এই ড্রাফটগুলোর একটি সেফটি স্ন্যাপশট অভ্যন্তরীণ মেমরিতে স্বয়ংক্রিয়ভাবে সংরক্ষণ ও যাচাই করা হবে।"
                    } else {
                        "Are you sure you want to permanently delete all ${trashedOrders.size} drafts?\n\nA safety snapshot of these orders will be verified and saved before deletion."
                    },
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyTrashConfirmDialog = false
                        showTrashDialog = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "হ্যাঁ, সম্পূর্ণ খালি করুন" else "Confirm Empty", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEmptyTrashConfirmDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
