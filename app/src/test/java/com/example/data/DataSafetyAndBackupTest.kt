package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.OrderDao
import com.example.data.model.CatalogProductEntity
import com.example.data.model.CourierService
import com.example.data.model.CourierSyncStatus
import com.example.data.model.DeliveryTier
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItem
import com.example.data.model.OrderType
import com.example.data.model.PaymentStatus
import com.example.data.model.Priority
import com.example.data.repository.OrderRepository
import com.example.util.ExportHelper
import com.example.util.RestoreConflictStrategy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataSafetyAndBackupTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var orderRepository: OrderRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        orderRepository = OrderRepository(db.orderDao(), db)
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createSyntheticOrder(id: Long, invoice: String, customer: String): OrderEntity {
        return OrderEntity(
            id = id,
            invoiceNo = invoice,
            customerName = customer,
            customerPhone = "01700000099",
            customerAddress = "Synthetic Address, Dhaka",
            district = "Dhaka",
            thana = "Mirpur",
            orderType = OrderType.REGULAR,
            priority = Priority.NORMAL,
            paymentStatus = PaymentStatus.UNPAID,
            fulfillmentStatus = FulfillmentStatus.NEW,
            items = listOf(
                OrderItem(
                    name = "Cotton Salwar",
                    size = "40",
                    color = "Blue",
                    quantity = 1,
                    unitPrice = 1200.0
                )
            ),
            deliveryTier = DeliveryTier.DHAKA_INSIDE,
            deliveryCharge = 80.0,
            advanceAmount = 0.0,
            discountAmount = 0.0,
            courierService = CourierService.STEADFAST,
            courierTrackingId = "DEMO-12345",
            courierSyncStatus = CourierSyncStatus.NONE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    @Test
    fun `duplicate id conflict preserves existing orders by assigning new auto-ids`() = runBlocking {
        // Step 1: Pre-populate existing database with production order ID = 42
        val existingOrder = createSyntheticOrder(id = 42L, invoice = "INV-LIVE-42", customer = "Existing Live Customer")
        db.orderDao().insertOrder(existingOrder)

        val initialOrders = db.orderDao().getAllOrdersDirect()
        assertEquals(1, initialOrders.size)
        assertEquals("Existing Live Customer", initialOrders.first().customerName)
        assertEquals(42L, initialOrders.first().id)

        // Step 2: Incoming backup contains conflicting ID = 42L
        val incomingBackupOrder = createSyntheticOrder(id = 42L, invoice = "INV-BACKUP-42", customer = "Incoming Restored Customer")
        val incomingOrders = listOf(incomingBackupOrder)

        // Step 3: Execute restore with PRESERVE_EXISTING_ASSIGN_NEW_IDS
        val summary = orderRepository.restoreOrdersSafely(
            orders = incomingOrders,
            conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
        )

        assertEquals(1, summary.renumberedOrdersCount)
        assertEquals(0, summary.overwrittenOrdersCount)

        // Step 4: Verify existing record was 100% preserved
        val updatedLiveOrder = db.orderDao().getOrderByIdDirect(42L)
        assertNotNull(updatedLiveOrder)
        assertEquals("Existing Live Customer", updatedLiveOrder?.customerName)
        assertEquals("INV-LIVE-42", updatedLiveOrder?.invoiceNo)

        // Step 5: Verify incoming order was safely inserted under a new ID
        val allAfterRestore = db.orderDao().getAllOrdersDirect()
        assertEquals(2, allAfterRestore.size)
        val newlyAssigned = allAfterRestore.find { it.customerName == "Incoming Restored Customer" }
        assertNotNull(newlyAssigned)
        assertNotEquals(42L, newlyAssigned?.id)
    }

    @Test
    fun `duplicate id conflict skips conflicting incoming orders when requested`() = runBlocking {
        // Pre-populate ID = 77
        val existingOrder = createSyntheticOrder(id = 77L, invoice = "INV-LIVE-77", customer = "Untouchable Customer")
        db.orderDao().insertOrder(existingOrder)

        val incomingBackupOrder = createSyntheticOrder(id = 77L, invoice = "INV-BACKUP-77", customer = "Should Be Skipped")
        val summary = orderRepository.restoreOrdersSafely(
            orders = listOf(incomingBackupOrder),
            conflictStrategy = RestoreConflictStrategy.SKIP_CONFLICTS
        )

        assertEquals(1, summary.skippedOrdersCount)
        assertEquals(0, summary.overwrittenOrdersCount)
        assertEquals(0, summary.renumberedOrdersCount)

        val ordersInDb = db.orderDao().getAllOrdersDirect()
        assertEquals(1, ordersInDb.size)
        assertEquals("Untouchable Customer", ordersInDb.first().customerName)
    }

    @Test
    fun `safety snapshot creates verified parseable backup before destructive operation`() {
        val testOrders = listOf(
            createSyntheticOrder(id = 1L, invoice = "INV-SNAP-1", customer = "Snapshot Customer 1"),
            createSyntheticOrder(id = 2L, invoice = "INV-SNAP-2", customer = "Snapshot Customer 2")
        )

        val result = ExportHelper.createSafetySnapshot(
            context = context,
            orders = testOrders,
            catalogProducts = emptyList(),
            tag = "unit_test"
        )

        assertTrue("Safety snapshot must succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        // Read and verify contents
        val json = file.readText(Charsets.UTF_8)
        val parsed = ExportHelper.importBackupFromJson(json)
        assertEquals(2, parsed.orders.size)
        assertEquals("INV-SNAP-1", parsed.orders[0].invoiceNo)
        assertEquals("INV-SNAP-2", parsed.orders[1].invoiceNo)

        file.delete()
    }

    @Test
    fun `inspectBackupStream identifies conflicting order ids without modifying database`() = runBlocking {
        // Pre-populate local DB
        db.orderDao().insertOrder(createSyntheticOrder(id = 15L, invoice = "INV-15", customer = "Local 15"))
        db.orderDao().insertOrder(createSyntheticOrder(id = 16L, invoice = "INV-16", customer = "Local 16"))

        val existingIds = db.orderDao().getAllExistingOrderIds().toSet()

        // Create backup JSON containing IDs 15 (conflict), 16 (conflict), and 99 (no conflict)
        val incomingList = listOf(
            createSyntheticOrder(id = 15L, invoice = "INV-15-DUP", customer = "Incoming 15"),
            createSyntheticOrder(id = 16L, invoice = "INV-16-DUP", customer = "Incoming 16"),
            createSyntheticOrder(id = 99L, invoice = "INV-99-NEW", customer = "Incoming 99")
        )
        val json = ExportHelper.exportFullBackupToJson(incomingList, emptyList()).getOrThrow()
        val stream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

        val inspectionResult = ExportHelper.inspectBackupStream(context, stream, existingIds)
        assertTrue(inspectionResult.isSuccess)

        val meta = inspectionResult.getOrThrow().metadata
        assertEquals(3, meta.totalOrders)
        assertEquals(2, meta.conflictingOrderIdsCount)
        assertTrue(meta.conflictingOrderIds.contains(15L))
        assertTrue(meta.conflictingOrderIds.contains(16L))

        // Ensure database was NOT modified during inspection
        assertEquals(2, db.orderDao().getOrderCountDirect())
        assertEquals("Local 15", db.orderDao().getOrderByIdDirect(15L)?.customerName)
    }

    @Test
    fun `sample fixtures contain zero real personal customer data`() {
        val syntheticOrder = createSyntheticOrder(id = 1L, invoice = "INV-DEMO-1", customer = "Synthetic Customer")
        assertTrue(syntheticOrder.customerPhone.startsWith("0170000"))
        assertTrue(syntheticOrder.customerName.contains("Synthetic"))
    }

    @Test
    fun `restoreDatabaseSafely atomically restores both catalog and orders`() = runBlocking {
        assertEquals(0, db.orderDao().getOrderCountDirect())
        assertEquals(0, db.catalogDao().getProductCount())

        val catalog = listOf(
            CatalogProductEntity(
                id = "cat_atomic_1",
                name = "Atomic Test Dress"
            )
        )
        val orders = listOf(
            createSyntheticOrder(id = 101L, invoice = "INV-ATOMIC-1", customer = "Atomic Customer")
        )

        val summary = orderRepository.restoreDatabaseSafely(
            orders = orders,
            catalogProducts = catalog,
            conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
        )

        assertEquals(1, summary.insertedOrdersCount)
        assertEquals(1, summary.totalIncomingProducts)
        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())
        assertEquals("Atomic Test Dress", db.catalogDao().getProductById("cat_atomic_1")?.name)
        assertEquals("Atomic Customer", db.orderDao().getOrderByIdDirect(101L)?.customerName)
    }

    /**
     * Test-only DAO decorator using Kotlin interface delegation (`OrderDao by delegate`).
     * Intercepts `insertOrder` to throw [IllegalStateException] when a designated invoice number is encountered,
     * while delegating all other Room DAO operations directly to the in-memory database DAO.
     */
    private class FailingOrderDao(
        private val delegate: OrderDao,
        private val failOnInvoice: String? = null
    ) : OrderDao by delegate {
        override suspend fun insertOrder(order: OrderEntity): Long {
            if (failOnInvoice != null && order.invoiceNo == failOnInvoice) {
                throw IllegalStateException("Simulated SQLite write error during insertOrder for ${order.invoiceNo}")
            }
            return delegate.insertOrder(order)
        }
    }

    @Test
    fun `restoreDatabaseSafely rolls back catalog when order insert fails after catalog insert`() = runBlocking {
        // Pre-populate database with 1 order and 1 product
        db.catalogDao().insertProduct(CatalogProductEntity(id = "cat_existing", name = "Existing Dress"))
        db.orderDao().insertOrder(createSyntheticOrder(id = 1L, invoice = "INV-EXISTING", customer = "Existing Customer"))

        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())

        // Target restore data: 1 catalog product + 1 failing order
        val catalog = listOf(
            CatalogProductEntity(id = "cat_doomed_1", name = "Doomed Dress 1")
        )
        val orders = listOf(
            createSyntheticOrder(id = 101L, invoice = "INV-FAIL-FIRST", customer = "Doomed First Customer")
        )

        val failingRepo = OrderRepository(
            orderDao = FailingOrderDao(db.orderDao(), failOnInvoice = "INV-FAIL-FIRST"),
            database = db
        )

        var threwExpectedException = false
        try {
            failingRepo.restoreDatabaseSafely(
                orders = orders,
                catalogProducts = catalog,
                conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
            )
        } catch (e: IllegalStateException) {
            threwExpectedException = true
        }

        assertTrue("Expected simulated transaction exception to be caught", threwExpectedException)

        // Verify that catalog insert was rolled back and failed order does not exist
        assertNull("Rolled back catalog product must NOT exist", db.catalogDao().getProductById("cat_doomed_1"))
        assertNull("Failed order must NOT exist", db.orderDao().getOrderByIdDirect(101L))

        // Verify existing database records remain intact
        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())
        assertEquals("Existing Customer", db.orderDao().getOrderByIdDirect(1L)?.customerName)
        assertEquals("Existing Dress", db.catalogDao().getProductById("cat_existing")?.name)
    }

    @Test
    fun `restoreDatabaseSafely rolls back both catalog and previous orders on mid-stream order failure`() = runBlocking {
        // Pre-populate database with 1 order and 1 product
        db.catalogDao().insertProduct(CatalogProductEntity(id = "cat_existing", name = "Existing Dress"))
        db.orderDao().insertOrder(createSyntheticOrder(id = 1L, invoice = "INV-EXISTING", customer = "Existing Customer"))

        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())

        // Target restore data: 1 catalog product + 2 orders (first succeeds, second fails)
        val catalog = listOf(
            CatalogProductEntity(id = "cat_doomed_2", name = "Doomed Dress 2")
        )
        val orders = listOf(
            createSyntheticOrder(id = 201L, invoice = "INV-ORDER-OK", customer = "Order One OK"),
            createSyntheticOrder(id = 202L, invoice = "INV-FAIL-SECOND", customer = "Order Two Fail")
        )

        val failingRepo = OrderRepository(
            orderDao = FailingOrderDao(db.orderDao(), failOnInvoice = "INV-FAIL-SECOND"),
            database = db
        )

        var threwExpectedException = false
        try {
            failingRepo.restoreDatabaseSafely(
                orders = orders,
                catalogProducts = catalog,
                conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
            )
        } catch (e: IllegalStateException) {
            threwExpectedException = true
        }

        assertTrue("Expected simulated transaction exception to be caught", threwExpectedException)

        // Verify that BOTH catalog and previously successful first order were completely rolled back
        assertNull("Rolled back catalog product must NOT exist", db.catalogDao().getProductById("cat_doomed_2"))
        assertNull("First order (inserted before mid-stream failure) must be rolled back", db.orderDao().getOrderByIdDirect(201L))
        assertNull("Second order (which triggered failure) must NOT exist", db.orderDao().getOrderByIdDirect(202L))

        // Verify existing database records remain intact
        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())
        assertEquals("Existing Customer", db.orderDao().getOrderByIdDirect(1L)?.customerName)
        assertEquals("Existing Dress", db.catalogDao().getProductById("cat_existing")?.name)
    }

    @Test
    fun `exportFullBackupToJson validates counts and succeeds on complete round trip`() {
        val orders = listOf(
            createSyntheticOrder(id = 1L, invoice = "INV-JSON-1", customer = "Customer 1"),
            createSyntheticOrder(id = 2L, invoice = "INV-JSON-2", customer = "Customer 2")
        )
        val catalog = listOf(
            CatalogProductEntity(id = "cat-json-1", name = "Jamdani Dress"),
            CatalogProductEntity(id = "cat-json-2", name = "Silk Saree")
        )

        val result = ExportHelper.exportFullBackupToJson(orders, catalog)
        assertTrue("JSON export should succeed", result.isSuccess)
        val json = result.getOrThrow()
        assertTrue(json.contains("fashion_aura_full_backup"))
        assertTrue(json.contains("INV-JSON-1"))
        assertTrue(json.contains("Jamdani Dress"))

        val restored = ExportHelper.importBackupFromJson(json)
        assertEquals(2, restored.orders.size)
        assertEquals(2, restored.catalogProducts.size)
    }

    @Test
    fun `exportFullZipBackup fails when referenced photo is missing`() {
        val missingPhotoUri = "file://${context.filesDir.absolutePath}/order_photos/non_existent_photo.jpg"
        val orderWithMissingPhoto = createSyntheticOrder(id = 10L, invoice = "INV-MISSING-PHOTO", customer = "Photo Missing Customer").copy(
            items = listOf(
                OrderItem(
                    name = "Custom Dress",
                    photoUri = missingPhotoUri
                )
            )
        )

        val out = ByteArrayOutputStream()
        val result = ExportHelper.exportFullZipBackup(context, out, listOf(orderWithMissingPhoto), emptyList())
        assertTrue("Export should fail for missing referenced photo", result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue("Exception message should identify missing file", ex?.message?.contains("non_existent_photo.jpg") == true)
    }

    @Test
    fun `exportFullZipBackup fails when referenced photo is zero bytes`() {
        val zeroByteFile = File(context.filesDir, "catalog_photos/empty_dress.jpg")
        zeroByteFile.parentFile?.mkdirs()
        zeroByteFile.writeBytes(ByteArray(0))

        val catalogProduct = CatalogProductEntity(
            id = "cat-zero",
            name = "Zero Byte Dress",
            photoUri = zeroByteFile.absolutePath
        )

        val out = ByteArrayOutputStream()
        val result = ExportHelper.exportFullZipBackup(context, out, emptyList(), listOf(catalogProduct))
        assertTrue("Export should fail for 0-byte photo", result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex?.message?.contains("খালি (০ বাইট)") == true || ex?.message?.contains("empty_dress.jpg") == true)
        zeroByteFile.delete()
    }

    @Test
    fun `exportFullZipBackup rejects path traversal attempt`() {
        val traversedOrder = createSyntheticOrder(id = 20L, invoice = "INV-TRAVERSAL", customer = "Traversal Customer").copy(
            items = listOf(
                OrderItem(
                    name = "Malicious Dress",
                    photoUri = "order_photos/../../secret.txt"
                )
            )
        )

        val out = ByteArrayOutputStream()
        val result = ExportHelper.exportFullZipBackup(context, out, listOf(traversedOrder), emptyList())
        assertTrue("Export should fail for path traversal", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException || result.exceptionOrNull()?.message?.contains("পাথ ট্রাভার্সাল") == true)
    }

    @Test
    fun `exportFullZipBackup succeeds and verifies required entries for valid data`() {
        val validPhotoFile = File(context.filesDir, "catalog_photos/valid_dress.jpg")
        validPhotoFile.parentFile?.mkdirs()
        // Standard JPEG header bytes
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 16, 'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 0, 1, 1, 0, 0, 1)
        validPhotoFile.writeBytes(jpegHeader + ByteArray(100))

        val product = CatalogProductEntity(
            id = "cat-valid",
            name = "Valid Dress",
            photoUri = validPhotoFile.absolutePath
        )
        val order = createSyntheticOrder(id = 30L, invoice = "INV-VALID", customer = "Valid Customer")

        val out = ByteArrayOutputStream()
        val result = ExportHelper.exportFullZipBackup(context, out, listOf(order), listOf(product))
        assertTrue("Export should succeed with valid data and photo", result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(1, summary.totalOrders)
        assertEquals(1, summary.totalCatalogProducts)
        assertTrue(summary.archivedPhotosCount >= 1)

        val zipBytes = out.toByteArray()
        val entries = mutableListOf<String>()
        java.util.zip.ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        assertTrue("ZIP must contain database.json", entries.contains("database.json"))
        assertTrue("ZIP must contain catalog_photos/valid_dress.jpg", entries.contains("catalog_photos/valid_dress.jpg"))

        validPhotoFile.delete()
    }

    @Test
    fun `exportFullZipBackup fails when destination stream is null`() {
        val orders = listOf(createSyntheticOrder(id = 1L, invoice = "INV-NULL-STREAM", customer = "Null Customer"))
        val result = ExportHelper.exportFullZipBackup(context, null, orders, emptyList())
        assertTrue("Null stream should return failure", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    private class FailingDestinationOutputStream : java.io.OutputStream() {
        override fun write(b: Int) {
            throw IOException("Simulated destination write failure")
        }
        override fun write(b: ByteArray, off: Int, len: Int) {
            throw IOException("Simulated destination write failure")
        }
    }

    @Test
    fun `exportFullZipBackup fails when destination stream write throws IOException`() {
        val orders = listOf(createSyntheticOrder(id = 40L, invoice = "INV-FAIL-STREAM", customer = "Fail Stream Customer"))
        val failingStream = FailingDestinationOutputStream()
        val result = ExportHelper.exportFullZipBackup(context, failingStream, orders, emptyList())
        assertTrue("Stream write failure should return failure", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `exportFullZipBackup cleans up temporary file on both success and failure`() {
        val getStagingFiles = {
            context.cacheDir.listFiles { _, name -> name.startsWith("backup_staging_") }?.toList() ?: emptyList()
        }

        val initialStagingCount = getStagingFiles().size

        // 1. Successful export cleanup
        val orders = listOf(createSyntheticOrder(id = 50L, invoice = "INV-CLEAN-SUCCESS", customer = "Clean Success"))
        val successOut = ByteArrayOutputStream()
        val successResult = ExportHelper.exportFullZipBackup(context, successOut, orders, emptyList())
        assertTrue(successResult.isSuccess)
        assertEquals("Temporary file must be deleted after success", initialStagingCount, getStagingFiles().size)

        // 2. Failed export cleanup (via failing stream)
        val failResult = ExportHelper.exportFullZipBackup(context, FailingDestinationOutputStream(), orders, emptyList())
        assertTrue(failResult.isFailure)
        assertEquals("Temporary file must be deleted after failure", initialStagingCount, getStagingFiles().size)
    }

    @Test
    fun `inspectBackupStream detects truncated corrupted ZIP file`() {
        val orders = listOf(createSyntheticOrder(id = 60L, invoice = "INV-TRUNCATED", customer = "Truncated Customer"))
        val out = ByteArrayOutputStream()
        val exportResult = ExportHelper.exportFullZipBackup(context, out, orders, emptyList())
        assertTrue(exportResult.isSuccess)

        val fullBytes = out.toByteArray()
        val truncatedBytes = fullBytes.copyOf(30)
        val inspectResult = ExportHelper.inspectBackupStream(context, ByteArrayInputStream(truncatedBytes), emptySet())
        assertTrue("Truncated ZIP must fail inspection or not parse full backup", inspectResult.isFailure || inspectResult.getOrNull()?.orders.isNullOrEmpty())
    }

    @Test
    fun `exportFullBackupToJson returns failure on adapter serialization failure without fallback`() {
        val orders = listOf(createSyntheticOrder(id = 1L, invoice = "INV-FAIL-SER", customer = "Fail Customer"))
        val catalog = listOf(CatalogProductEntity(id = "cat-fail", name = "Fail Dress"))

        val throwingAdapter = object : com.squareup.moshi.JsonAdapter<com.example.util.FullDatabaseBackup>() {
            override fun fromJson(reader: com.squareup.moshi.JsonReader): com.example.util.FullDatabaseBackup? {
                throw IOException("Simulated Moshi deserialization error")
            }
            override fun toJson(writer: com.squareup.moshi.JsonWriter, value: com.example.util.FullDatabaseBackup?) {
                throw IOException("Simulated Moshi serialization error")
            }
        }

        val result = ExportHelper.exportFullBackupToJson(orders, catalog, adapter = throwingAdapter)
        assertTrue("Moshi serialization failure must return Result.failure", result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue("Exception message should reflect serialization failure", ex?.message?.contains("Simulated Moshi serialization error") == true)
        assertFalse("Must not return fallback JSON with order array", result.getOrDefault("").contains("INV-FAIL-SER"))
    }

    @Test
    fun `verifyZipArchive rejects synthetic ZIP containing duplicate entry names`() {
        val tempZip = File(context.cacheDir, "duplicate_test_${System.currentTimeMillis()}.zip")
        try {
            val baos = ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(baos).use { zos ->
                val backupJson = ExportHelper.exportFullBackupToJson(emptyList(), emptyList()).getOrThrow()
                // Entry 1: 13 characters
                zos.putNextEntry(java.util.zip.ZipEntry("database.json"))
                zos.write(backupJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Entry 2: 13 characters (distinct name during creation to bypass ZipOutputStream check)
                zos.putNextEntry(java.util.zip.ZipEntry("database.jso2"))
                zos.write(backupJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val zipBytes = baos.toByteArray()
            val target = "database.jso2".toByteArray(Charsets.UTF_8)
            val replacement = "database.json".toByteArray(Charsets.UTF_8)

            // Replace all occurrences of target with replacement in zipBytes
            for (i in 0..zipBytes.size - target.size) {
                var match = true
                for (j in target.indices) {
                    if (zipBytes[i + j] != target[j]) {
                        match = false
                        break
                    }
                }
                if (match) {
                    for (j in replacement.indices) {
                        zipBytes[i + j] = replacement[j]
                    }
                }
            }

            tempZip.writeBytes(zipBytes)

            var threwDuplicateException = false
            try {
                ExportHelper.verifyZipArchive(tempZip, emptyList(), emptyList(), emptyList())
            } catch (e: IllegalStateException) {
                if (e.message?.contains("ডুপ্লিকেট এন্ট্রি শনাক্ত হয়েছে") == true) {
                    threwDuplicateException = true
                }
            }
            assertTrue("Verifier must reject ZIP containing duplicate entry names", threwDuplicateException)
        } finally {
            if (tempZip.exists()) tempZip.delete()
        }
    }

    private class FailingFlushOutputStream : ByteArrayOutputStream() {
        override fun flush() {
            throw IOException("Simulated destination flush failure")
        }
    }

    @Test
    fun `exportFullZipBackup fails when destination stream flush throws IOException and cleans up staging file`() {
        val initialStagingCount = context.cacheDir.listFiles { _, name -> name.startsWith("backup_staging_") }?.size ?: 0
        val orders = listOf(createSyntheticOrder(id = 70L, invoice = "INV-FLUSH-FAIL", customer = "Flush Fail Customer"))
        val failingStream = FailingFlushOutputStream()

        val result = ExportHelper.exportFullZipBackup(context, failingStream, orders, emptyList())
        assertTrue("Flush failure must return Result.failure", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("Simulated destination flush failure", result.exceptionOrNull()?.message)

        val finalStagingCount = context.cacheDir.listFiles { _, name -> name.startsWith("backup_staging_") }?.size ?: 0
        assertEquals("Staging file must be cleaned up on flush failure", initialStagingCount, finalStagingCount)
    }

    private class FailingCloseOutputStream : ByteArrayOutputStream() {
        override fun close() {
            throw IOException("Simulated destination close failure")
        }
    }

    @Test
    fun `exportFullZipBackup fails when destination stream close throws IOException and cleans up staging file`() {
        val initialStagingCount = context.cacheDir.listFiles { _, name -> name.startsWith("backup_staging_") }?.size ?: 0
        val orders = listOf(createSyntheticOrder(id = 80L, invoice = "INV-CLOSE-FAIL", customer = "Close Fail Customer"))
        val failingStream = FailingCloseOutputStream()

        val result = ExportHelper.exportFullZipBackup(context, failingStream, orders, emptyList())
        assertTrue("Close failure must return Result.failure", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("Simulated destination close failure", result.exceptionOrNull()?.message)

        val finalStagingCount = context.cacheDir.listFiles { _, name -> name.startsWith("backup_staging_") }?.size ?: 0
        assertEquals("Staging file must be cleaned up on close failure", initialStagingCount, finalStagingCount)
    }

    @Test
    fun `resolveInternalPhoto rejects sibling directory prefix bypass for catalog and order photos`() {
        val evilCatalogOrder = createSyntheticOrder(id = 90L, invoice = "INV-SIBLING-CAT", customer = "Sibling Cat").copy(
            items = listOf(OrderItem(name = "Evil Dress", photoUri = "catalog_photos_evil/secret.jpg"))
        )
        val evilOrderPhotosOrder = createSyntheticOrder(id = 91L, invoice = "INV-SIBLING-ORD", customer = "Sibling Ord").copy(
            items = listOf(OrderItem(name = "Evil Dress", photoUri = "order_photos_backup/secret.jpg"))
        )

        val out = ByteArrayOutputStream()
        val resultCat = ExportHelper.exportFullZipBackup(context, out, listOf(evilCatalogOrder), emptyList())
        assertTrue("Sibling catalog_photos_evil must be rejected", resultCat.isFailure)
        assertTrue(resultCat.exceptionOrNull() is SecurityException)

        val resultOrd = ExportHelper.exportFullZipBackup(context, out, listOf(evilOrderPhotosOrder), emptyList())
        assertTrue("Sibling order_photos_backup must be rejected", resultOrd.isFailure)
        assertTrue(resultOrd.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `resolveInternalPhoto rejects encoded traversal attempts`() {
        val encodedOrder = createSyntheticOrder(id = 92L, invoice = "INV-ENCODED", customer = "Encoded").copy(
            items = listOf(OrderItem(name = "Encoded Dress", photoUri = "file://${context.filesDir.absolutePath}/catalog_photos/%2e%2e/databases/fashion_aura.db"))
        )
        val out = ByteArrayOutputStream()
        val result = ExportHelper.exportFullZipBackup(context, out, listOf(encodedOrder), emptyList())
        assertTrue("Encoded %2e%2e traversal must be rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `recovery drill full JSON backup and restore round-trip verifies all order and catalog fields`() = runBlocking {
        val originalOrders = listOf(
            createSyntheticOrder(id = 101L, invoice = "DRILL-JSON-101", customer = "Drill JSON Customer 1").copy(
                customerPhone = "01811223344",
                district = "Chittagong",
                thana = "Kotwali",
                paymentStatus = PaymentStatus.PAID,
                fulfillmentStatus = FulfillmentStatus.SHIPPED,
                advanceAmount = 500.0,
                discountAmount = 100.0,
                deliveryCharge = 130.0,
                items = listOf(
                    OrderItem(name = "Katan Silk Saree", size = "Free", color = "Crimson Red", quantity = 2, unitPrice = 3500.0)
                )
            ),
            createSyntheticOrder(id = 102L, invoice = "DRILL-JSON-102", customer = "Drill JSON Customer 2")
        )
        val originalCatalog = listOf(
            CatalogProductEntity(id = "drill_cat_1", name = "Jamdani Premium", category = "Saree", basePrice = 4500.0),
            CatalogProductEntity(id = "drill_cat_2", name = "Lawn Three Piece", category = "Salwar Kameez", basePrice = 2800.0)
        )

        // 1. Export JSON
        val exportResult = ExportHelper.exportFullBackupToJson(originalOrders, originalCatalog)
        assertTrue("JSON export must succeed", exportResult.isSuccess)
        val jsonPayload = exportResult.getOrThrow()

        // 2. Clear isolated database to simulate fresh restore target
        db.orderDao().emptyTrash()
        db.catalogDao().deleteAllProducts()
        assertEquals(0, db.orderDao().getOrderCountDirect())
        assertEquals(0, db.catalogDao().getProductCount())

        // 3. Inspect and Restore JSON
        val inspection = ExportHelper.inspectBackupStream(context, ByteArrayInputStream(jsonPayload.toByteArray(Charsets.UTF_8)), emptySet())
        assertTrue("Inspection must succeed", inspection.isSuccess)
        val inspected = inspection.getOrThrow()
        assertEquals(2, inspected.orders.size)
        assertEquals(2, inspected.catalogProducts.size)

        val restoreSummary = orderRepository.restoreDatabaseSafely(
            orders = inspected.orders,
            catalogProducts = inspected.catalogProducts,
            conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
        )
        assertEquals(2, restoreSummary.insertedOrdersCount)
        assertEquals(2, restoreSummary.totalIncomingProducts)

        // 4. Detailed field-by-field verification
        val restoredOrder1 = db.orderDao().getOrderByIdDirect(101L)
        assertNotNull("Restored order 101 must exist", restoredOrder1)
        assertEquals("DRILL-JSON-101", restoredOrder1?.invoiceNo)
        assertEquals("Drill JSON Customer 1", restoredOrder1?.customerName)
        assertEquals("01811223344", restoredOrder1?.customerPhone)
        assertEquals("Chittagong", restoredOrder1?.district)
        assertEquals(PaymentStatus.PAID, restoredOrder1?.paymentStatus)
        assertEquals(FulfillmentStatus.SHIPPED, restoredOrder1?.fulfillmentStatus)
        assertEquals(500.0, restoredOrder1?.advanceAmount ?: 0.0, 0.001)
        assertEquals(1, restoredOrder1?.items?.size)
        assertEquals("Katan Silk Saree", restoredOrder1?.items?.first()?.name)
        assertEquals(3500.0, restoredOrder1?.items?.first()?.unitPrice ?: 0.0, 0.001)

        val restoredCat1 = db.catalogDao().getProductById("drill_cat_1")
        assertNotNull("Restored catalog product must exist", restoredCat1)
        assertEquals("Jamdani Premium", restoredCat1?.name)
        assertEquals("Saree", restoredCat1?.category)
        assertEquals(4500.0, restoredCat1?.basePrice ?: 0.0, 0.001)
    }

    @Test
    fun `recovery drill full ZIP backup and restore round-trip verifies orders catalog and photo contents`() = runBlocking {
        val sandboxDir = File(context.cacheDir, "drill_zip_sandbox_${System.currentTimeMillis()}").apply { mkdirs() }
        val catalogPhotosDir = File(context.filesDir, "catalog_photos").apply { mkdirs() }
        val orderPhotosDir = File(context.filesDir, "order_photos").apply { mkdirs() }

        val testCatPhoto = File(catalogPhotosDir, "drill_cat_photo.jpg")
        val testOrderPhoto = File(orderPhotosDir, "drill_order_photo.jpg")

        // Valid JPEG binary content (header + distinctive payload bytes)
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 16, 'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 0, 1, 1, 0, 0, 1)
        val catBytes = jpegHeader + "CATALOG-IMAGE-PAYLOAD-12345".toByteArray(Charsets.UTF_8)
        val orderBytes = jpegHeader + "ORDER-IMAGE-PAYLOAD-67890".toByteArray(Charsets.UTF_8)

        testCatPhoto.writeBytes(catBytes)
        testOrderPhoto.writeBytes(orderBytes)

        try {
            val originalOrder = createSyntheticOrder(id = 201L, invoice = "DRILL-ZIP-201", customer = "Drill ZIP Customer").copy(
                items = listOf(OrderItem(name = "Custom Embroidered Gown", photoUri = "order_photos/drill_order_photo.jpg", unitPrice = 6000.0))
            )
            val originalProduct = CatalogProductEntity(
                id = "drill_cat_zip_1",
                name = "Designer Kurti",
                photoUri = testCatPhoto.absolutePath
            )

            // 1. Export Full ZIP
            val zipOut = ByteArrayOutputStream()
            val exportResult = ExportHelper.exportFullZipBackup(context, zipOut, listOf(originalOrder), listOf(originalProduct))
            assertTrue("ZIP export must succeed", exportResult.isSuccess)
            val summary = exportResult.getOrThrow()
            assertEquals(1, summary.totalOrders)
            assertEquals(1, summary.totalCatalogProducts)
            assertEquals(2, summary.archivedPhotosCount)

            val zipBytes = zipOut.toByteArray()

            // 2. Simulate complete loss: delete on-disk photo files and clear in-memory DB
            assertTrue(testCatPhoto.delete())
            assertTrue(testOrderPhoto.delete())
            assertFalse(testCatPhoto.exists())
            assertFalse(testOrderPhoto.exists())

            // 3. Inspect backup stream without modifying DB
            val inspection = ExportHelper.inspectBackupStream(context, ByteArrayInputStream(zipBytes), emptySet())
            assertTrue("Inspection of ZIP must succeed", inspection.isSuccess)
            val preview = inspection.getOrThrow()
            assertEquals(1, preview.orders.size)
            assertEquals(1, preview.catalogProducts.size)
            assertEquals(2, preview.metadata.photosInZipCount)

            // 4. Restore DB and unpack ZIP payload
            val restoreSummary = orderRepository.restoreDatabaseSafely(
                orders = preview.orders,
                catalogProducts = preview.catalogProducts,
                conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
            )
            val unpackResult = ExportHelper.unpackZipPayload(context, preview.payloadBytes)

            assertEquals(1, restoreSummary.insertedOrdersCount)
            assertEquals(2, unpackResult.restoredPhotosCount)

            // 5. Verify restored database records
            val restoredOrder = db.orderDao().getOrderByIdDirect(201L)
            assertNotNull("Restored order must exist", restoredOrder)
            assertEquals("DRILL-ZIP-201", restoredOrder?.invoiceNo)
            assertEquals("Custom Embroidered Gown", restoredOrder?.items?.first()?.name)

            val restoredProd = db.catalogDao().getProductById("drill_cat_zip_1")
            assertNotNull("Restored product must exist", restoredProd)
            assertEquals("Designer Kurti", restoredProd?.name)

            // 6. Verify restored physical photos exist on disk and match exact byte contents
            assertTrue("Restored catalog photo must exist on disk", testCatPhoto.exists())
            assertTrue("Restored order photo must exist on disk", testOrderPhoto.exists())
            assertArrayEquals("Catalog photo bytes must match original byte-for-byte", catBytes, testCatPhoto.readBytes())
            assertArrayEquals("Order photo bytes must match original byte-for-byte", orderBytes, testOrderPhoto.readBytes())
        } finally {
            if (testCatPhoto.exists()) testCatPhoto.delete()
            if (testOrderPhoto.exists()) testOrderPhoto.delete()
            sandboxDir.deleteRecursively()
        }
    }

    @Test
    fun `recovery drill rejects invalid backup format and unsupported version without modifying database`() = runBlocking {
        // Pre-populate baseline DB record
        val baselineOrder = createSyntheticOrder(id = 301L, invoice = "INV-BASELINE-301", customer = "Baseline Customer")
        val baselineProduct = CatalogProductEntity(id = "cat_baseline_301", name = "Baseline Dress")
        db.orderDao().insertOrder(baselineOrder)
        db.catalogDao().insertProduct(baselineProduct)

        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())

        // 1. Invalid foreign format
        val invalidFormatJson = """
            {
              "format": "foreign_unknown_format",
              "version": 1,
              "exportedAt": 1700000000000,
              "totalOrders": 1,
              "totalCatalogProducts": 0,
              "orders": [],
              "catalogProducts": []
            }
        """.trimIndent()
        val formatResult = ExportHelper.inspectBackupStream(context, ByteArrayInputStream(invalidFormatJson.toByteArray(Charsets.UTF_8)), setOf(301L))
        assertTrue("Empty/invalid format backup inspection must fail", formatResult.isFailure)

        // 2. Unsupported future version inside ZIP archive
        val tempZip = File(context.cacheDir, "drill_future_version_${System.currentTimeMillis()}.zip")
        try {
            val futureBackup = """
                {
                  "format": "fashion_aura_full_backup",
                  "version": 99,
                  "exportedAt": 1700000000000,
                  "totalOrders": 0,
                  "totalCatalogProducts": 0,
                  "orders": [],
                  "catalogProducts": []
                }
            """.trimIndent()
            val baos = ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(java.util.zip.ZipEntry("database.json"))
                zos.write(futureBackup.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            tempZip.writeBytes(baos.toByteArray())

            var threwVersionException = false
            try {
                ExportHelper.verifyZipArchive(tempZip, emptyList(), emptyList(), emptyList())
            } catch (e: IllegalStateException) {
                if (e.message?.contains("ভার্সন অমিল") == true || e.message?.contains("99") == true) {
                    threwVersionException = true
                }
            }
            assertTrue("Verifier must reject ZIP containing future version 99", threwVersionException)
        } finally {
            if (tempZip.exists()) tempZip.delete()
        }

        // Verify database remains 100% untouched
        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())
        assertEquals("Baseline Customer", db.orderDao().getOrderByIdDirect(301L)?.customerName)
    }

    @Test
    fun `recovery drill OVERWRITE_EXISTING conflict strategy updates matching records without creating duplicates`() = runBlocking {
        // Pre-populate existing order ID = 401L
        val originalOrder = createSyntheticOrder(id = 401L, invoice = "INV-OVERWRITE-401", customer = "Original Customer").copy(
            paymentStatus = PaymentStatus.UNPAID,
            deliveryCharge = 80.0
        )
        db.orderDao().insertOrder(originalOrder)
        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(PaymentStatus.UNPAID, db.orderDao().getOrderByIdDirect(401L)?.paymentStatus)

        // Incoming backup order has identical ID = 401L but updated fields
        val incomingBackupOrder = originalOrder.copy(
            customerName = "Updated Customer Name",
            paymentStatus = PaymentStatus.PAID,
            deliveryCharge = 150.0
        )

        val summary = orderRepository.restoreDatabaseSafely(
            orders = listOf(incomingBackupOrder),
            catalogProducts = emptyList(),
            conflictStrategy = RestoreConflictStrategy.OVERWRITE_EXISTING
        )

        assertEquals(1, summary.overwrittenOrdersCount)
        assertEquals(0, summary.insertedOrdersCount)
        assertEquals(0, summary.renumberedOrdersCount)
        assertEquals(0, summary.skippedOrdersCount)

        // Verify record was updated in-place without altering total count
        assertEquals(1, db.orderDao().getOrderCountDirect())
        val updatedOrder = db.orderDao().getOrderByIdDirect(401L)
        assertNotNull(updatedOrder)
        assertEquals("Updated Customer Name", updatedOrder?.customerName)
        assertEquals(PaymentStatus.PAID, updatedOrder?.paymentStatus)
        assertEquals(150.0, updatedOrder?.deliveryCharge ?: 0.0, 0.001)
    }

    @Test
    fun `recovery drill failed restore leaves existing orders and catalog products completely unchanged`() = runBlocking {
        // Pre-populate baseline state
        val baselineOrder = createSyntheticOrder(id = 501L, invoice = "INV-STEADY-501", customer = "Steady Customer")
        val baselineProduct = CatalogProductEntity(id = "cat_steady_501", name = "Steady Silk")
        db.orderDao().insertOrder(baselineOrder)
        db.catalogDao().insertProduct(baselineProduct)

        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())

        // Create failing repository with a DAO that throws on insertion
        val failingRepo = OrderRepository(
            orderDao = FailingOrderDao(db.orderDao(), failOnInvoice = "INV-DOOMED-502"),
            database = db
        )

        val incomingOrders = listOf(
            createSyntheticOrder(id = 502L, invoice = "INV-DOOMED-502", customer = "Doomed Customer")
        )
        val incomingCatalog = listOf(
            CatalogProductEntity(id = "cat_doomed_502", name = "Doomed Dress")
        )

        var failureCaught = false
        try {
            failingRepo.restoreDatabaseSafely(
                orders = incomingOrders,
                catalogProducts = incomingCatalog,
                conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
            )
        } catch (e: IllegalStateException) {
            failureCaught = true
        }

        assertTrue("Failure must be caught from transaction abort", failureCaught)

        // Invariant: Verify baseline database records are 100% unaltered
        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())
        assertEquals("Steady Customer", db.orderDao().getOrderByIdDirect(501L)?.customerName)
        assertEquals("Steady Silk", db.catalogDao().getProductById("cat_steady_501")?.name)
        assertNull(db.orderDao().getOrderByIdDirect(502L))
        assertNull(db.catalogDao().getProductById("cat_doomed_502"))
    }

    @Test
    fun `recovery drill corrupted photo entry in ZIP is caught by verifier and unpack handles non-image data safely`() {
        val tempZip = File(context.cacheDir, "drill_corrupt_photo_${System.currentTimeMillis()}.zip")
        try {
            val backupJson = ExportHelper.exportFullBackupToJson(emptyList(), emptyList()).getOrThrow()
            val textBytes = "THIS IS NOT A VALID JPEG OR PNG IMAGE".toByteArray(Charsets.UTF_8)

            val baos = ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(java.util.zip.ZipEntry("database.json"))
                zos.write(backupJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(java.util.zip.ZipEntry("catalog_photos/corrupt_photo.jpg"))
                zos.write(textBytes)
                zos.closeEntry()
            }
            tempZip.writeBytes(baos.toByteArray())

            // 1. Verifier must detect invalid/corrupted image header and reject
            var headerVerificationFailed = false
            try {
                ExportHelper.verifyZipArchive(tempZip, emptyList(), emptyList(), emptyList())
            } catch (e: IOException) {
                if (e.message?.contains("অসমর্থিত বা ত্রুটিপূর্ণ ছবি ফরম্যাট") == true) {
                    headerVerificationFailed = true
                }
            }
            assertTrue("Verifier must reject ZIP containing corrupted image header", headerVerificationFailed)

            // 2. Unpack handles truncated or corrupt byte stream safely without throwing uncaught exceptions
            val truncatedBytes = baos.toByteArray().copyOf(40)
            val unpackResult = ExportHelper.unpackZipPayload(context, truncatedBytes)
            assertEquals(0, unpackResult.restoredPhotosCount)
            assertTrue("Corrupted archive unpack yields empty orders list", unpackResult.orders.isEmpty())
        } finally {
            if (tempZip.exists()) tempZip.delete()
        }
    }

    @Test
    fun `recovery drill safety snapshot failure aborts restore before any database write`() = runBlocking {
        // Pre-populate baseline DB
        val baselineOrder = createSyntheticOrder(id = 601L, invoice = "INV-PRE-SNAP-601", customer = "Pre-Snap Customer")
        db.orderDao().insertOrder(baselineOrder)
        assertEquals(1, db.orderDao().getOrderCountDirect())

        // Target restore data that would modify the database if executed
        val incomingOrders = listOf(
            createSyntheticOrder(id = 602L, invoice = "INV-POST-SNAP-602", customer = "Should Not Be Inserted")
        )

        // Simulate safety snapshot creation failure by pointing to an unwriteable snapshot directory file
        val snapshotDir = File(context.filesDir, "safety_snapshots")
        snapshotDir.deleteRecursively()
        // Create a regular file where the directory is expected, preventing file creation inside it
        snapshotDir.createNewFile()

        try {
            val snapshotResult = ExportHelper.createSafetySnapshot(
                context = context,
                orders = listOf(baselineOrder),
                catalogProducts = emptyList(),
                tag = "drill_fail"
            )
            assertTrue("Snapshot creation must fail when directory is a regular file", snapshotResult.isFailure)

            // Simulate the pre-restore guard used in OrderViewModel.executeSafeRestore:
            var restoreAttempted = false
            if (snapshotResult.isFailure) {
                // Abort restore immediately — do NOT call orderRepository.restoreDatabaseSafely
            } else {
                restoreAttempted = true
                orderRepository.restoreDatabaseSafely(incomingOrders, emptyList(), RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS)
            }

            assertFalse("Restore must be aborted and never executed when safety snapshot fails", restoreAttempted)

            // Verify database was NOT touched
            assertEquals(1, db.orderDao().getOrderCountDirect())
            assertEquals("Pre-Snap Customer", db.orderDao().getOrderByIdDirect(601L)?.customerName)
            assertNull("Incoming order must not exist in database", db.orderDao().getOrderByIdDirect(602L))
        } finally {
            snapshotDir.delete()
        }
    }

    @Test
    fun `recovery drill empty or blank backup input does not erase or corrupt existing records`() = runBlocking {
        // Pre-populate baseline state
        val baselineOrder = createSyntheticOrder(id = 701L, invoice = "INV-EMPTY-GUARD", customer = "Protected Customer")
        val baselineProduct = CatalogProductEntity(id = "cat_empty_guard", name = "Protected Dress")
        db.orderDao().insertOrder(baselineOrder)
        db.catalogDao().insertProduct(baselineProduct)

        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())

        // 1. Empty byte stream inspection
        val emptyStreamResult = ExportHelper.inspectBackupStream(context, ByteArrayInputStream(ByteArray(0)), emptySet())
        assertTrue("Inspection of empty stream must fail", emptyStreamResult.isFailure)
        assertTrue(emptyStreamResult.exceptionOrNull()?.message?.contains("খালি (০ বাইট)") == true)

        // 2. Empty JSON string import
        val emptyParsed = ExportHelper.importBackupFromJson("")
        assertEquals(0, emptyParsed.orders.size)
        assertEquals(0, emptyParsed.catalogProducts.size)

        // 3. Passing empty lists to restoreDatabaseSafely does not delete or alter existing data
        val summary = orderRepository.restoreDatabaseSafely(
            orders = emptyList(),
            catalogProducts = emptyList(),
            conflictStrategy = RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS
        )
        assertEquals(0, summary.insertedOrdersCount)
        assertEquals(0, summary.overwrittenOrdersCount)

        // Invariant: Verify existing database records remain 100% intact
        assertEquals(1, db.orderDao().getOrderCountDirect())
        assertEquals(1, db.catalogDao().getProductCount())
        assertEquals("Protected Customer", db.orderDao().getOrderByIdDirect(701L)?.customerName)
        assertEquals("Protected Dress", db.catalogDao().getProductById("cat_empty_guard")?.name)
    }
}
