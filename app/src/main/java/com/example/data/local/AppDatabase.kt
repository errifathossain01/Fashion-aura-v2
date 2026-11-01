package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [OrderEntity::class, CatalogProductEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(OrderTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fashion_aura.db"
                )
                    .addCallback(SeedDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    if (database.catalogDao().getProductCount() == 0) {
                        seedCatalogProducts(database.catalogDao())
                    }
                    // CRITICAL DATA PROTECTION:
                    // 1. Never seed demo orders into a production database.
                    // 2. Restrict demo orders strictly to debug/test builds.
                    // 3. Only seed if the orders table is completely empty.
                    if (com.example.BuildConfig.DEBUG && database.orderDao().getOrderCountDirect() == 0) {
                        seedSyntheticDemoOrders(database.orderDao())
                    }
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch {
                    if (database.catalogDao().getProductCount() == 0) {
                        seedCatalogProducts(database.catalogDao())
                    }
                }
            }
        }

        private suspend fun seedCatalogProducts(catalogDao: CatalogDao) {
            val sampleCatalog = listOf(
                CatalogProductEntity(
                    id = "cat_p1",
                    name = "কটন সালোয়ার কামিজ (Cotton Salwar Kameez)",
                    category = "Salwar Kameez",
                    basePrice = 1350.0,
                    availableSizes = listOf("36", "38", "40", "42", "44", "Free"),
                    availableColors = listOf("মেরুন", "লাল", "কালো", "পেস্তা"),
                    variantPhotos = emptyMap(),
                    stockStatus = "IN_STOCK",
                    photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_red"
                ),
                CatalogProductEntity(
                    id = "cat_p2",
                    name = "লং গাউন ড্রেস (Lavender Long Gown)",
                    category = "Gown",
                    basePrice = 1670.0,
                    availableSizes = listOf("38", "40", "42", "Free"),
                    availableColors = listOf("ল্যাভেন্ডার", "বেগুনি", "নীল"),
                    variantPhotos = emptyMap(),
                    stockStatus = "PRE_ORDER",
                    photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_purple"
                ),
                CatalogProductEntity(
                    id = "cat_p3",
                    name = "জর্জেট গাউন (Mustard Georgette Gown)",
                    category = "Gown",
                    basePrice = 2270.0,
                    availableSizes = listOf("36", "38", "40", "42"),
                    availableColors = listOf("হলুদ", "মাস্টার্ড", "সবুজ"),
                    variantPhotos = emptyMap(),
                    stockStatus = "IN_STOCK",
                    photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_yellow"
                )
            )
            catalogDao.insertProducts(sampleCatalog)
        }

        /**
         * Synthetic fixtures for debug/test development only.
         * Contains zero real customer names, phone numbers, addresses, or courier consignment tracking IDs.
         * Uses auto-generated primary key (id = 0L) to prevent overwriting existing rows.
         */
        private suspend fun seedSyntheticDemoOrders(orderDao: OrderDao) {
            val now = System.currentTimeMillis()
            val syntheticOrders = listOf(
                OrderEntity(
                    id = 0L,
                    invoiceNo = "INV-DEMO-001",
                    customerName = "Synthetic Test Customer Alpha",
                    customerPhone = "01700000001",
                    customerAddress = "Road 1, Block A, Synthetic Ward, Dhaka",
                    district = "Dhaka",
                    thana = "Mirpur",
                    orderType = OrderType.REGULAR,
                    priority = Priority.NORMAL,
                    paymentStatus = PaymentStatus.UNPAID,
                    fulfillmentStatus = FulfillmentStatus.READY_TO_SHIP,
                    items = listOf(
                        OrderItem(
                            name = "কটন সালোয়ার কামিজ",
                            size = "42",
                            color = "মেরুন / Red",
                            quantity = 1,
                            unitPrice = 1350.0,
                            photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_red"
                        ),
                        OrderItem(
                            name = "দোপাট্টা ম্যাচিং",
                            size = "Free",
                            color = "মেরুন",
                            quantity = 1,
                            unitPrice = 0.0
                        )
                    ),
                    deliveryTier = DeliveryTier.DHAKA_INSIDE,
                    deliveryCharge = 80.0,
                    advanceAmount = 0.0,
                    discountAmount = 0.0,
                    courierService = CourierService.STEADFAST,
                    courierTrackingId = "DEMO-TRK-001",
                    courierSyncStatus = CourierSyncStatus.SYNCED,
                    riderNote = "🛵 টেস্ট নোট: পার্সেল চেক করে ডেলিভারি দিন।",
                    createdAt = now - (1000 * 60 * 60 * 2),
                    updatedAt = now - (1000 * 60 * 60 * 2)
                ),
                OrderEntity(
                    id = 0L,
                    invoiceNo = "INV-DEMO-002",
                    customerName = "Synthetic Test Customer Beta",
                    customerPhone = "01700000002",
                    customerAddress = "Road 2, Synthetic Ward, Chittagong",
                    district = "Chittagong",
                    thana = "Kotwali",
                    orderType = OrderType.PRE_ORDER,
                    priority = Priority.NORMAL,
                    paymentStatus = PaymentStatus.PARTIAL,
                    fulfillmentStatus = FulfillmentStatus.PRE_ORDER_PENDING,
                    items = listOf(
                        OrderItem(
                            name = "লং গাউন ড্রেস",
                            size = "40",
                            color = "ল্যাভেন্ডার / Lavender",
                            quantity = 1,
                            unitPrice = 1670.0,
                            photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_purple"
                        )
                    ),
                    deliveryTier = DeliveryTier.OUTSIDE_DHAKA,
                    deliveryCharge = 150.0,
                    advanceAmount = 0.0,
                    discountAmount = 0.0,
                    courierService = CourierService.STEADFAST,
                    courierTrackingId = "",
                    courierSyncStatus = CourierSyncStatus.NONE,
                    riderNote = "🛵 টেস্ট প্রি-অর্ডার পার্সেল।",
                    createdAt = now - (1000 * 60 * 60 * 4),
                    updatedAt = now - (1000 * 60 * 60 * 4)
                ),
                OrderEntity(
                    id = 0L,
                    invoiceNo = "INV-DEMO-003",
                    customerName = "Synthetic Test Customer Gamma",
                    customerPhone = "01700000003",
                    customerAddress = "Road 3, Synthetic Ward, Sylhet",
                    district = "Sylhet",
                    thana = "Sylhet Sadar",
                    orderType = OrderType.PRE_ORDER,
                    priority = Priority.URGENT,
                    paymentStatus = PaymentStatus.PAID,
                    fulfillmentStatus = FulfillmentStatus.SHIPPED,
                    items = listOf(
                        OrderItem(
                            name = "জর্জেট গাউন",
                            size = "38",
                            color = "হলুদ / Mustard Yellow",
                            quantity = 1,
                            unitPrice = 2270.0,
                            photoUri = "android.resource://com.aistudio.fashionaura.app/drawable/sample_dress_yellow"
                        )
                    ),
                    deliveryTier = DeliveryTier.OUTSIDE_DHAKA,
                    deliveryCharge = 150.0,
                    advanceAmount = 2420.0,
                    discountAmount = 0.0,
                    courierService = CourierService.PATHAO,
                    courierTrackingId = "DEMO-TRK-003",
                    courierSyncStatus = CourierSyncStatus.SYNCED,
                    riderNote = "🛵 টেস্ট পেইড পার্সেল।",
                    createdAt = now - (1000 * 60 * 60 * 6),
                    updatedAt = now - (1000 * 60 * 60 * 6)
                )
            )
            orderDao.insertOrders(syntheticOrders)
        }
    }
}
