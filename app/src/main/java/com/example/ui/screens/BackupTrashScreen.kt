package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogProductEntity
import com.example.data.model.OrderEntity
import com.example.util.ExportHelper
import com.example.util.MessagingHelper
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupTrashScreen(
    allOrders: List<OrderEntity>,
    catalogProducts: List<CatalogProductEntity>,
    trashedOrders: List<OrderEntity>,
    language: AppLanguage,
    onNavigateBack: () -> Unit,
    onRestoreBackupStream: (InputStream) -> Unit,
    onRestoreOrder: (Long) -> Unit,
    onDeletePermanently: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn = language == AppLanguage.BANGLA
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
                        Toast.makeText(context, if (isBn) "ZIP ব্যাকআপ ত্রুটি: ${error.message}" else "ZIP Backup error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(context, if (isBn) "ZIP ব্যাকআপ ত্রুটি: ${e.message}" else "ZIP Backup error: ${e.message}", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, if (isBn) "JSON ব্যাকআপ ত্রুটি: ${error.message}" else "JSON Backup error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(context, if (isBn) "JSON সেভ ত্রুটি: ${e.message}" else "JSON Save error: ${e.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(context, "রিস্টোর ত্রুটি: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ব্যাকআপ ও ট্র্যাশ ম্যানেজার" else "Backup & Trash Manager",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📦 ১-ফাইল সম্পূর্ণ ZIP ব্যাকআপ (ছবি + ডাটাবেজ)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "অ্যাপ আনইনস্টল করলেও ক্যাটালগের সব ড্রেসের ছবি এবং সম্পূর্ণ ডাটাবেজ নিরাপদে রাখতে এই ZIP ফাইলটি ডাউনলোড বা সংরক্ষণ করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { createZipLauncher.launch("fashion_aura_backup.zip") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ZIP ব্যাকআপ ডাউনলোড করুন", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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
                            Text("📥 ZIP বা JSON ফাইল থেকে রিস্টোর করুন")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                createJsonLauncher.launch("fashion_aura_backup.json")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBn) "JSON ফাইল সেভ করুন (.json)" else "Save JSON Backup File (.json)")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                ExportHelper.exportFullBackupToJson(allOrders, catalogProducts).fold(
                                    onSuccess = { json ->
                                        MessagingHelper.copyToClipboard(context, "Full Backup JSON", json)
                                        Toast.makeText(context, if (isBn) "JSON ব্যাকআপ ক্লিপবোর্ডে কপি হয়েছে!" else "JSON backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, if (isBn) "JSON ব্যাকআপ ত্রুটি: ${error.message}" else "JSON Backup error: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBn) "JSON ব্যাকআপ ক্লিপবোর্ডে কপি করুন" else "Copy JSON to Clipboard")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val csv = ExportHelper.exportOrdersToCsv(allOrders)
                                MessagingHelper.copyToClipboard(context, "Orders CSV", csv)
                                Toast.makeText(context, "CSV ডাটা কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("১-ট্যাপ CSV এক্সপোর্ট (Excel)")
                        }
                    }
                }
            }

            // TRASH MANAGEMENT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            if (trashedOrders.isNotEmpty()) {
                                TextButton(onClick = { showEmptyTrashConfirmDialog = true }) {
                                    Text(if (isBn) "সম্পূর্ণ খালি করুন" else "Empty Bin", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }

                        if (trashedOrders.isEmpty()) {
                            Text(
                                text = if (isBn) "রিসাইকেল বিন খালি। কোনো ডিলিট করা অর্ডার নেই।" else "Recycle Bin is empty.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            trashedOrders.forEach { order ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(order.invoiceNo, fontWeight = FontWeight.Bold)
                                        Text("${order.customerName} - ${order.customerPhone}", fontSize = 12.sp)
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
                }
            }
        }
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

    // Empty Trash Confirmation Dialog
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
                        "আপনি কি নিশ্চিতভাবে ট্র্যাশের সমস্ত (${trashedOrders.size}টি) ড্রাফট মুছে ফেলতে চান?\n\nডাটা সুরক্ষার স্বার্থে মুছে ফেলার ঠিক পূর্বে এই ড্রাফটগুলোর একটি সেফটি স্ন্যাপশট অভ্যন্তরীণ স্টোরেজে স্বয়ংক্রিয়ভাবে সংরক্ষণ ও যাচাই করা হবে।"
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
