package com.example.util
import android.content.Context
import com.example.data.model.CatalogProductEntity
import com.example.data.model.OrderEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.LinkedHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipBackupSummary(
    val totalOrders: Int,
    val totalCatalogProducts: Int,
    val archivedPhotosCount: Int,
    val totalBytes: Long
)

data class ReferencedPhotoInfo(
    val entryPath: String,
    val file: File,
    val sourceDescription: String
)

@JsonClass(generateAdapter = true)
data class FullDatabaseBackup(
    val format: String = "fashion_aura_full_backup",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val totalOrders: Int = 0,
    val totalCatalogProducts: Int = 0,
    val orders: List<OrderEntity> = emptyList(),
    val catalogProducts: List<CatalogProductEntity> = emptyList()
)

data class RestoredBackupResult(
    val orders: List<OrderEntity>,
    val catalogProducts: List<CatalogProductEntity>
)

data class RestoredZipResult(
    val orders: List<OrderEntity>,
    val catalogProducts: List<CatalogProductEntity>,
    val restoredPhotosCount: Int
)

enum class RestoreConflictStrategy {
    PRESERVE_EXISTING_ASSIGN_NEW_IDS, // Default: renumber incoming conflicting order IDs (id = 0L)
    SKIP_CONFLICTS,                   // Skip any incoming order whose ID already exists
    OVERWRITE_EXISTING                // Overwrite existing orders (after safety snapshot)
}

data class RestoreSummary(
    val totalIncomingOrders: Int,
    val totalIncomingProducts: Int,
    val insertedOrdersCount: Int,
    val renumberedOrdersCount: Int,
    val skippedOrdersCount: Int,
    val overwrittenOrdersCount: Int,
    val restoredPhotosCount: Int,
    val safetySnapshotPath: String?
)

data class BackupMetadata(
    val format: String = "fashion_aura_full_backup",
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val totalOrders: Int = 0,
    val totalCatalogProducts: Int = 0,
    val photosInZipCount: Int = 0,
    val conflictingOrderIdsCount: Int = 0,
    val conflictingOrderIds: List<Long> = emptyList(),
    val isLegacyFormat: Boolean = false
)

data class BackupInspectionResult(
    val metadata: BackupMetadata,
    val orders: List<OrderEntity>,
    val catalogProducts: List<CatalogProductEntity>,
    val payloadBytes: ByteArray
)

object ExportHelper {


    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val orderListType = Types.newParameterizedType(List::class.java, OrderEntity::class.java)
    private val orderListAdapter = moshi.adapter<List<OrderEntity>>(orderListType)

    private val fullBackupAdapter = moshi.adapter(FullDatabaseBackup::class.java)

    fun exportOrdersToCsv(orders: List<OrderEntity>): String {
        val sb = StringBuilder()
        sb.append("Invoice,Date,Customer,Phone,Address,District,OrderType,Status,Courier,TrackingId,TotalBill,Advance,COD\n")
        for (o in orders) {
            val dateStr = BanglaFormatter.formatDate(o.createdAt, false).replace(",", "")
            val cleanAddr = o.customerAddress.replace(",", " ").replace("\n", " ")
            sb.append("\"${o.invoiceNo}\",\"$dateStr\",\"${o.customerName}\",\"${o.customerPhone}\",\"$cleanAddr\",\"${o.district}\",\"${o.orderType.name}\",\"${o.fulfillmentStatus.name}\",\"${o.courierService.name}\",\"${o.courierTrackingId}\",${o.totalBill},${o.advanceAmount},${o.codAmount}\n")
        }
        return sb.toString()
    }

    /**
     * Compact, lightweight JSON backup containing both orders and catalog products.
     * Photo paths are relative references (zero file bloat).
     */
    fun exportFullBackupToJson(
        orders: List<OrderEntity>,
        catalogProducts: List<CatalogProductEntity>,
        adapter: com.squareup.moshi.JsonAdapter<FullDatabaseBackup> = fullBackupAdapter
    ): Result<String> {
        return try {
            val backup = FullDatabaseBackup(
                format = "fashion_aura_full_backup",
                version = 1,
                exportedAt = System.currentTimeMillis(),
                totalOrders = orders.size,
                totalCatalogProducts = catalogProducts.size,
                orders = orders,
                catalogProducts = catalogProducts
            )
            val json = adapter.indent("  ").toJson(backup)
                ?: return Result.failure(IllegalStateException("JSON ব্যাকআপ তৈরি ব্যর্থ হয়েছে (নাল আউটপুট)।"))

            // Verify the generated JSON can be read back and parsed
            val verified = adapter.fromJson(json)
                ?: return Result.failure(IllegalStateException("তৈরিকৃত JSON ব্যাকআপ যাচাইকরণ ব্যর্থ হয়েছে (পার্স করা যায়নি)।"))

            if (verified.format != "fashion_aura_full_backup") {
                return Result.failure(IllegalStateException("JSON ফরম্যাট অমিল: প্রত্যাশিত 'fashion_aura_full_backup', পাওয়া গেছে '${verified.format}'"))
            }
            if (verified.version != 1) {
                return Result.failure(IllegalStateException("JSON ভার্সন অমিল: প্রত্যাশিত ১, পাওয়া গেছে ${verified.version}"))
            }
            if (verified.totalOrders != orders.size || verified.orders.size != orders.size) {
                return Result.failure(IllegalStateException("JSON অর্ডার সংখ্যা অমিল: মূল ${orders.size}টি, ব্যাকআপে পাওয়া গেছে ${verified.orders.size}টি"))
            }
            if (verified.totalCatalogProducts != catalogProducts.size || verified.catalogProducts.size != catalogProducts.size) {
                return Result.failure(IllegalStateException("JSON ক্যাটালগ সংখ্যা অমিল: মূল ${catalogProducts.size}টি, ব্যাকআপে পাওয়া গেছে ${verified.catalogProducts.size}টি"))
            }

            Result.success(json)
        } catch (e: Throwable) {
            Result.failure(IllegalStateException("JSON ব্যাকআপ তৈরি বা যাচাইকরণ ব্যর্থ: ${e.message}", e))
        }
    }

    fun exportFullBackupToJsonStream(
        outputStream: OutputStream?,
        orders: List<OrderEntity>,
        catalogProducts: List<CatalogProductEntity>
    ): Result<Long> {
        if (outputStream == null) {
            return Result.failure(IOException("গন্তব্য ফাইলটি খোলা সম্ভব হয়নি (আউটপুট স্ট্রিম নাল)।"))
        }
        return exportFullBackupToJson(orders, catalogProducts).mapCatching { json ->
            outputStream.use { out ->
                val bytes = json.toByteArray(Charsets.UTF_8)
                out.write(bytes)
                out.flush()
                bytes.size.toLong()
            }
        }
    }

    fun exportOrdersToJson(orders: List<OrderEntity>): String {
        return try {
            orderListAdapter.indent("  ").toJson(orders)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun resolveInternalPhoto(
        context: Context,
        rawUri: String?,
        sourceDescription: String
    ): ReferencedPhotoInfo? {
        if (rawUri == null || rawUri.isBlank()) return null
        val trimmed = rawUri.trim()
        if (trimmed.startsWith("android.resource://") ||
            trimmed.startsWith("http://") ||
            trimmed.startsWith("https://")
        ) {
            return null
        }

        // Parse URI using android.net.Uri for robust normalization
        val parsedUri = try {
            android.net.Uri.parse(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("ত্রুটিপূর্ণ URI ফরম্যাট ($sourceDescription): $trimmed", e)
        }

        val scheme = parsedUri.scheme
        // Non-file schemes (e.g., content:// or custom non-file schemes) are ignored as non-internal
        if (scheme != null && scheme != "file") {
            return null
        }

        // Normalize path: extract decoded path without query or fragment
        val decodedPath = (parsedUri.path ?: trimmed.substringBefore("?").substringBefore("#")).trim()
        if (decodedPath.isBlank()) return null

        // Traversal checks on normalized decoded path
        if (decodedPath.contains("../") || decodedPath.contains("..\\") || decodedPath == ".." || decodedPath.endsWith("/..")) {
            throw SecurityException("অবৈধ পাথ ট্রাভার্সাল শনাক্ত হয়েছে ($sourceDescription): $trimmed")
        }

        val catalogDir = File(context.filesDir, "catalog_photos").canonicalFile
        val orderPhotosDir = File(context.filesDir, "order_photos").canonicalFile

        // Canonical prefix with trailing separator to strictly prevent sibling prefix bypass (e.g. catalog_photos_evil)
        val catalogPrefix = catalogDir.canonicalPath + File.separator
        val orderPhotosPrefix = orderPhotosDir.canonicalPath + File.separator

        // Resolve candidate file: absolute path vs relative path
        val candidateFile = if (File(decodedPath).isAbsolute) {
            File(decodedPath).canonicalFile
        } else {
            File(context.filesDir, decodedPath).canonicalFile
        }

        val candidateCanonicalPath = candidateFile.canonicalPath

        // Verify boundary containment: candidate must be strictly within catalogDir or orderPhotosDir
        val inCatalog = candidateCanonicalPath.startsWith(catalogPrefix)
        val inOrderPhotos = candidateCanonicalPath.startsWith(orderPhotosPrefix)

        if (!inCatalog && !inOrderPhotos) {
            throw SecurityException("অননুমোদিত ডিরেক্টরি অ্যাক্সেস ($sourceDescription): $trimmed")
        }

        // Relative entry path inside the ZIP: preserves relative subpath within allowed directory
        val relativeEntry = if (inCatalog) {
            "catalog_photos/" + candidateCanonicalPath.removePrefix(catalogPrefix)
        } else {
            "order_photos/" + candidateCanonicalPath.removePrefix(orderPhotosPrefix)
        }

        return ReferencedPhotoInfo(
            entryPath = relativeEntry,
            file = candidateFile,
            sourceDescription = sourceDescription
        )
    }

    fun collectAndValidateReferencedPhotos(
        context: Context,
        orders: List<OrderEntity>,
        catalogProducts: List<CatalogProductEntity>
    ): List<ReferencedPhotoInfo> {
        val referenced = mutableListOf<ReferencedPhotoInfo>()

        for (prod in catalogProducts) {
            val prodDesc = "ক্যাটালগ পণ্য: ${prod.name} (ID: ${prod.id})"
            resolveInternalPhoto(context, prod.photoUri, prodDesc)?.let { referenced.add(it) }
            for ((variantName, uri) in prod.variantPhotos) {
                val varDesc = "ক্যাটালগ পণ্য ভ্যারিয়েন্ট: ${prod.name} - $variantName (ID: ${prod.id})"
                resolveInternalPhoto(context, uri, varDesc)?.let { referenced.add(it) }
            }
        }

        for (order in orders) {
            val orderDesc = "অর্ডার ইনভয়েস: ${order.invoiceNo}"
            for (item in order.items) {
                val itemDesc = "$orderDesc, আইটেম: ${item.name}"
                resolveInternalPhoto(context, item.photoUri, itemDesc)?.let { referenced.add(it) }
            }
        }

        for (info in referenced) {
            val file = info.file
            if (!file.exists()) {
                throw FileNotFoundException("অর্ডারে বা ক্যাটালগে ব্যবহৃত ছবি পাওয়া যায়নি: ${file.name} (${info.sourceDescription})")
            }
            if (!file.isFile) {
                throw IOException("ছবির ফাইলটি সাধারণ ফাইল নয়: ${file.name} (${info.sourceDescription})")
            }
            if (file.length() <= 0L) {
                throw IOException("ছবির ফাইলটি খালি (০ বাইট): ${file.name} (${info.sourceDescription})")
            }

            try {
                val readBytes = file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        total += count
                    }
                    total
                }
                if (readBytes != file.length()) {
                    throw IOException("ফাইল সম্পূর্ণ পড়া যায়নি (পঠিত $readBytes বাইট, প্রত্যাশিত ${file.length()} বাইট)")
                }
            } catch (e: Exception) {
                throw IOException("ছবির ফাইল পড়া সম্ভব হয়নি: ${file.name} (${info.sourceDescription}) - ${e.message}", e)
            }
        }

        return referenced
    }

    private fun validateImageHeader(header: ByteArray, length: Int, entryName: String) {
        if (length < 3) {
            throw IOException("ছবিটির হেডার অসম্পূর্ণ (দৈর্ঘ্য $length বাইট): $entryName")
        }
        val isJpeg = length >= 3 &&
                header[0] == 0xFF.toByte() &&
                header[1] == 0xD8.toByte() &&
                header[2] == 0xFF.toByte()

        val isPng = length >= 4 &&
                header[0] == 0x89.toByte() &&
                header[1] == 0x50.toByte() &&
                header[2] == 0x4E.toByte() &&
                header[3] == 0x47.toByte()

        val isWebp = length >= 12 &&
                header[0] == 'R'.code.toByte() &&
                header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() &&
                header[3] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() &&
                header[9] == 'E'.code.toByte() &&
                header[10] == 'B'.code.toByte() &&
                header[11] == 'P'.code.toByte()

        if (!isJpeg && !isPng && !isWebp) {
            throw IOException("অসমর্থিত বা ত্রুটিপূর্ণ ছবি ফরম্যাট: $entryName")
        }
    }

    /**
     * Intelligently imports JSON. Supports:
     * 1. Full database backup format (orders + catalogProducts)
     * 2. Legacy orders-only array format
     */
    fun importBackupFromJson(json: String): RestoredBackupResult {
        val trimmed = json.trim()
        if (trimmed.startsWith("{")) {
            try {
                val full = fullBackupAdapter.fromJson(trimmed)
                if (full != null && (full.orders.isNotEmpty() || full.catalogProducts.isNotEmpty())) {
                    return RestoredBackupResult(
                        orders = full.orders,
                        catalogProducts = full.catalogProducts
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback: try orders list array
        val legacyOrders = try {
            orderListAdapter.fromJson(trimmed) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return RestoredBackupResult(orders = legacyOrders, catalogProducts = emptyList())
    }

    fun importOrdersFromJson(json: String): List<OrderEntity> {
        return importBackupFromJson(json).orders
    }

    /**
     * Staged ZIP backup using cacheDir temporary file.
     * Fully validates JSON contents, counts, and required referenced photos before writing to destination stream.
     */
    fun exportFullZipBackup(
        context: Context,
        outputStream: OutputStream?,
        orders: List<OrderEntity>,
        catalogProducts: List<CatalogProductEntity>
    ): Result<ZipBackupSummary> {
        if (outputStream == null) {
            return Result.failure(IOException("গন্তব্য ফাইলটি খোলা সম্ভব হয়নি (আউটপুট স্ট্রিম নাল)।"))
        }

        var tempZipFile: File? = null
        return try {
            val referencedPhotos = collectAndValidateReferencedPhotos(context, orders, catalogProducts)

            val filesToArchive = LinkedHashMap<String, File>()
            for (ref in referencedPhotos) {
                filesToArchive[ref.entryPath] = ref.file
            }

            val catalogDir = File(context.filesDir, "catalog_photos")
            if (catalogDir.exists() && catalogDir.isDirectory) {
                catalogDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        val entry = "catalog_photos/${file.name}"
                        if (!filesToArchive.containsKey(entry)) {
                            filesToArchive[entry] = file
                        }
                    }
                }
            }
            val orderPhotosDir = File(context.filesDir, "order_photos")
            if (orderPhotosDir.exists() && orderPhotosDir.isDirectory) {
                orderPhotosDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        val entry = "order_photos/${file.name}"
                        if (!filesToArchive.containsKey(entry)) {
                            filesToArchive[entry] = file
                        }
                    }
                }
            }

            val jsonResult = exportFullBackupToJson(orders, catalogProducts)
            val jsonString = jsonResult.getOrThrow()

            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) cacheDir.mkdirs()
            tempZipFile = File.createTempFile("backup_staging_", ".zip", cacheDir)

            FileOutputStream(tempZipFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zipOut ->
                        val dbEntry = ZipEntry("database.json")
                        zipOut.putNextEntry(dbEntry)
                        zipOut.write(jsonString.toByteArray(Charsets.UTF_8))
                        zipOut.closeEntry()

                        for ((entryPath, file) in filesToArchive) {
                            val photoEntry = ZipEntry(entryPath)
                            zipOut.putNextEntry(photoEntry)
                            file.inputStream().use { fileInput ->
                                fileInput.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                        zipOut.finish()
                    }
                }
            }

            val archivedPhotosCount = verifyZipArchive(tempZipFile, orders, catalogProducts, referencedPhotos)

            val stagedSize = tempZipFile.length()
            var totalCopied = 0L
            outputStream.use { out ->
                tempZipFile.inputStream().use { input ->
                    val buffer = ByteArray(16384)
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        out.write(buffer, 0, count)
                        totalCopied += count
                    }
                }
                out.flush()
            }

            if (totalCopied != stagedSize) {
                throw IOException("গন্তব্যে ফাইল সম্পূর্ণ কপি হয়নি (কপি হয়েছে $totalCopied বাইট, প্রত্যাশিত $stagedSize বাইট)")
            }

            Result.success(
                ZipBackupSummary(
                    totalOrders = orders.size,
                    totalCatalogProducts = catalogProducts.size,
                    archivedPhotosCount = archivedPhotosCount,
                    totalBytes = totalCopied
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempZipFile?.let {
                if (it.exists()) it.delete()
            }
        }
    }

    /**
     * Verifies a staged ZIP archive's integrity, format, required entries, and non-empty content.
     * Accessible for direct test verification of duplicate entries, missing files, or corruption.
     */
    fun verifyZipArchive(
        zipFile: File,
        expectedOrders: List<OrderEntity>,
        expectedCatalog: List<CatalogProductEntity>,
        referencedPhotos: List<ReferencedPhotoInfo>
    ): Int {
        var archivedPhotosCount = 0
        ZipFile(zipFile).use { zf ->
            val seenEntries = mutableSetOf<String>()
            var databaseJsonFound = false
            var parsedDatabaseJson: FullDatabaseBackup? = null

            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!seenEntries.add(entry.name)) {
                    throw IllegalStateException("ZIP ফাইলে ডুপ্লিকেট এন্ট্রি শনাক্ত হয়েছে: ${entry.name}")
                }

                if (entry.name == "database.json") {
                    databaseJsonFound = true
                    val baos = ByteArrayOutputStream()
                    zf.getInputStream(entry).use { it.copyTo(baos) }
                    val content = baos.toString(Charsets.UTF_8.name())
                    parsedDatabaseJson = fullBackupAdapter.fromJson(content)
                        ?: throw IllegalStateException("ZIP এর ভেতরের database.json পার্স করা যায়নি")
                } else if (entry.name.startsWith("catalog_photos/") || entry.name.startsWith("order_photos/")) {
                    archivedPhotosCount++
                    val headerBytes = ByteArray(16)
                    var headerBytesRead = 0
                    var totalEntryBytes = 0L
                    zf.getInputStream(entry).use { stream ->
                        val buffer = ByteArray(8192)
                        var count: Int
                        while (stream.read(buffer).also { count = it } != -1) {
                            if (headerBytesRead < 16) {
                                val toCopy = minOf(count, 16 - headerBytesRead)
                                System.arraycopy(buffer, 0, headerBytes, headerBytesRead, toCopy)
                                headerBytesRead += toCopy
                            }
                            totalEntryBytes += count
                        }
                    }
                    if (totalEntryBytes == 0L) {
                        throw IOException("ZIP এন্ট্রি খালি (০ বাইট): ${entry.name}")
                    }
                    validateImageHeader(headerBytes, headerBytesRead, entry.name)
                }
            }

            if (!databaseJsonFound || parsedDatabaseJson == null) {
                throw IllegalStateException("ZIP ফাইলে database.json পাওয়া যায়নি")
            }
            if (parsedDatabaseJson.format != "fashion_aura_full_backup") {
                throw IllegalStateException("ZIP এর ভেতরের ফরম্যাট অমিল: ${parsedDatabaseJson.format}")
            }
            if (parsedDatabaseJson.version != 1) {
                throw IllegalStateException("ZIP এর ভেতরের ভার্সন অমিল: ${parsedDatabaseJson.version}")
            }
            if (parsedDatabaseJson.totalOrders != expectedOrders.size || parsedDatabaseJson.orders.size != expectedOrders.size) {
                throw IllegalStateException("ZIP এর ভেতরে অর্ডারের সংখ্যা অমিল: মূল ${expectedOrders.size}টি, ফাইলে পাওয়া গেছে ${parsedDatabaseJson.orders.size}টি")
            }
            if (parsedDatabaseJson.totalCatalogProducts != expectedCatalog.size || parsedDatabaseJson.catalogProducts.size != expectedCatalog.size) {
                throw IllegalStateException("ZIP এর ভেতরে ক্যাটালগ সংখ্যা অমিল: মূল ${expectedCatalog.size}টি, ফাইলে পাওয়া গেছে ${parsedDatabaseJson.catalogProducts.size}টি")
            }

            for (ref in referencedPhotos) {
                if (!seenEntries.contains(ref.entryPath)) {
                    throw IllegalStateException("প্রয়োজনীয় ছবি ZIP ফাইলে পাওয়া যায়নি: ${ref.entryPath} (${ref.sourceDescription})")
                }
            }
        }
        return archivedPhotosCount
    }

    /**
     * Inspects a backup stream (ZIP or JSON) WITHOUT modifying the database or unpacking files to disk.
     * Validates contents, parses metadata, and identifies conflicting order IDs against existing database IDs.
     */
    fun inspectBackupStream(
        context: Context,
        inputStream: InputStream,
        existingOrderIds: Set<Long>
    ): Result<BackupInspectionResult> {
        return try {
            val bytes = inputStream.readBytes()
            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("ফাইলটি খালি (০ বাইট)। কোনো ডাটা পাওয়া যায়নি।"))
            }

            var photosInZip = 0
            var jsonContent: String? = null
            var isZip = false

            if (bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
            ) {
                isZip = true
                ZipInputStream(bytes.inputStream()).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name == "database.json" || name.endsWith(".json")) {
                            val baos = ByteArrayOutputStream()
                            zipIn.copyTo(baos)
                            jsonContent = baos.toString(Charsets.UTF_8.name())
                        } else if (name.startsWith("catalog_photos/") ||
                            name.startsWith("order_photos/") ||
                            name.endsWith(".jpg", ignoreCase = true) ||
                            name.endsWith(".jpeg", ignoreCase = true) ||
                            name.endsWith(".png", ignoreCase = true)
                        ) {
                            photosInZip++
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            if (jsonContent.isNullOrBlank()) {
                jsonContent = String(bytes, Charsets.UTF_8)
            }

            val trimmed = jsonContent.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("ব্যাকআপ ফাইলে কোনো JSON ডাটা পাওয়া যায়নি।"))
            }

            var backupFormat = "legacy_order_array"
            var backupVersion = 1
            var exportedAt = System.currentTimeMillis()
            var isLegacy = true
            var parsedOrders: List<OrderEntity> = emptyList()
            var parsedCatalog: List<CatalogProductEntity> = emptyList()

            if (trimmed.startsWith("{")) {
                val full = try {
                    fullBackupAdapter.fromJson(trimmed)
                } catch (e: Exception) {
                    null
                }
                if (full != null) {
                    backupFormat = full.format
                    backupVersion = full.version
                    exportedAt = if (full.exportedAt > 0) full.exportedAt else System.currentTimeMillis()
                    parsedOrders = full.orders
                    parsedCatalog = full.catalogProducts
                    isLegacy = false
                }
            }

            if (parsedOrders.isEmpty() && isLegacy) {
                parsedOrders = try {
                    orderListAdapter.fromJson(trimmed) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (parsedOrders.isEmpty() && parsedCatalog.isEmpty()) {
                return Result.failure(IllegalArgumentException("ব্যাকআপ ফাইলে কোনো অর্ডার বা ক্যাটালগ প্রোডাক্ট নেই।"))
            }

            val conflictingIds = parsedOrders.map { it.id }.filter { it > 0L && existingOrderIds.contains(it) }

            val metadata = BackupMetadata(
                format = backupFormat,
                version = backupVersion,
                exportedAt = exportedAt,
                totalOrders = parsedOrders.size,
                totalCatalogProducts = parsedCatalog.size,
                photosInZipCount = photosInZip,
                conflictingOrderIdsCount = conflictingIds.size,
                conflictingOrderIds = conflictingIds,
                isLegacyFormat = isLegacy
            )

            Result.success(
                BackupInspectionResult(
                    metadata = metadata,
                    orders = parsedOrders,
                    catalogProducts = parsedCatalog,
                    payloadBytes = bytes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unpacks ZIP containing database.json and photos (catalog_photos and order_photos),
     * or seamlessly falls back to raw JSON bytes.
     */
    fun unpackZipPayload(
        context: Context,
        bytes: ByteArray
    ): RestoredZipResult {
        var photosRestored = 0
        var jsonContent: String? = null

        val isZip = bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()

        if (isZip) {
            try {
                val catalogDir = File(context.filesDir, "catalog_photos").apply { mkdirs() }
                val orderPhotosDir = File(context.filesDir, "order_photos").apply { mkdirs() }

                ZipInputStream(bytes.inputStream()).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name == "database.json" || name.endsWith(".json")) {
                            val baos = ByteArrayOutputStream()
                            zipIn.copyTo(baos)
                            jsonContent = baos.toString(Charsets.UTF_8.name())
                        } else if (name.startsWith("catalog_photos/") ||
                            name.startsWith("order_photos/") ||
                            name.endsWith(".jpg", ignoreCase = true) ||
                            name.endsWith(".jpeg", ignoreCase = true) ||
                            name.endsWith(".png", ignoreCase = true)
                        ) {
                            val fileName = File(name).name
                            if (fileName.isNotBlank()) {
                                val targetDir = if (name.startsWith("order_photos/")) orderPhotosDir else catalogDir
                                val targetFile = File(targetDir, fileName)
                                targetFile.outputStream().use { out ->
                                    zipIn.copyTo(out)
                                }
                                photosRestored++
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } catch (e: Exception) {
                // Non-fatal unpack issue
            }
        }

        if (jsonContent.isNullOrBlank()) {
            jsonContent = String(bytes, Charsets.UTF_8)
        }

        val parsed = importBackupFromJson(jsonContent ?: "")
        return RestoredZipResult(
            orders = parsed.orders,
            catalogProducts = parsed.catalogProducts,
            restoredPhotosCount = photosRestored
        )
    }

    /**
     * Unpacks ZIP containing database.json and catalog_photos/, or seamlessly falls back to raw JSON.
     */
    fun restoreFromZipOrJsonStream(
        context: Context,
        inputStream: InputStream
    ): RestoredZipResult {
        val bytes = inputStream.readBytes()
        return unpackZipPayload(context, bytes)
    }

    /**
     * Creates and immediately verifies a self-contained safety snapshot file of existing data.
     * If snapshot verification fails (e.g. disk write failure or JSON mismatch), Result.failure is returned.
     */
    fun createSafetySnapshot(
        context: Context,
        orders: List<OrderEntity>,
        catalogProducts: List<CatalogProductEntity>,
        tag: String
    ): Result<File> {
        return try {
            val snapshotDir = File(context.filesDir, "safety_snapshots").apply {
                if (!exists()) mkdirs()
            }
            val timestamp = System.currentTimeMillis()
            val file = File(snapshotDir, "safety_snapshot_${tag}_${timestamp}.json")

            val backup = FullDatabaseBackup(
                format = "fashion_aura_safety_snapshot",
                version = 1,
                exportedAt = timestamp,
                totalOrders = orders.size,
                totalCatalogProducts = catalogProducts.size,
                orders = orders,
                catalogProducts = catalogProducts
            )

            val json = fullBackupAdapter.indent("  ").toJson(backup)
            file.writeText(json, Charsets.UTF_8)

            // Self-verification: confirm file exists, size > 0, and can be read back with matching count
            if (!file.exists() || file.length() == 0L) {
                return Result.failure(IllegalStateException("সেফটি স্ন্যাপশট ফাইল তৈরি ব্যর্থ হয়েছে (০ বাইট)।"))
            }

            val readBack = file.readText(Charsets.UTF_8)
            val parsed = fullBackupAdapter.fromJson(readBack)
            if (parsed == null || parsed.orders.size != orders.size) {
                return Result.failure(IllegalStateException("সেফটি স্ন্যাপশট ডাটা যাচাইকরণে অমিল পাওয়া গেছে।"))
            }

            // Keep rolling history of maximum 5 snapshots
            val allSnapshots = snapshotDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
            if (allSnapshots.size > 5) {
                allSnapshots.drop(5).forEach { it.delete() }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listSafetySnapshots(context: Context): List<File> {
        val snapshotDir = File(context.filesDir, "safety_snapshots")
        if (!snapshotDir.exists()) return emptyList()
        return snapshotDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

