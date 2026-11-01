package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.OrderDao
import com.example.data.model.CatalogProductEntity
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderType
import com.example.util.RestoreConflictStrategy
import com.example.util.RestoreSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class OrderRepository(
    private val orderDao: OrderDao,
    private val database: AppDatabase
) {
    val allActiveOrders: Flow<List<OrderEntity>> = orderDao.getAllActiveOrders()
    val trashedOrders: Flow<List<OrderEntity>> = orderDao.getTrashedOrders()
    val totalOrdersCount: Flow<Int> = orderDao.getTotalOrdersCount()
    val pendingSyncCount: Flow<Int> = orderDao.getPendingSyncCount()

    fun getOrdersByStatus(status: FulfillmentStatus): Flow<List<OrderEntity>> =
        orderDao.getOrdersByStatus(status)

    fun getOrdersByType(type: OrderType): Flow<List<OrderEntity>> =
        orderDao.getOrdersByType(type)

    fun getUrgentOrders(): Flow<List<OrderEntity>> =
        orderDao.getUrgentOrders()

    fun getScheduledOrders(): Flow<List<OrderEntity>> =
        orderDao.getScheduledOrders()

    fun getPendingCourierSyncOrders(): Flow<List<OrderEntity>> =
        orderDao.getPendingCourierSyncOrders()

    fun searchOrders(query: String): Flow<List<OrderEntity>> =
        orderDao.searchOrders(query)

    fun getOrderById(id: Long): Flow<OrderEntity?> =
        orderDao.getOrderById(id)

    suspend fun getOrderByIdDirect(id: Long): OrderEntity? = withContext(Dispatchers.IO) {
        orderDao.getOrderByIdDirect(id)
    }

    suspend fun getOrdersByPhone(phone: String): List<OrderEntity> = withContext(Dispatchers.IO) {
        orderDao.getOrdersByPhone(phone)
    }

    suspend fun insertOrder(order: OrderEntity): Long = withContext(Dispatchers.IO) {
        orderDao.insertOrder(order)
    }

    suspend fun updateOrder(order: OrderEntity) = withContext(Dispatchers.IO) {
        orderDao.updateOrder(order.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateOrders(orders: List<OrderEntity>) = withContext(Dispatchers.IO) {
        orderDao.updateOrdersTransaction(orders.map { it.copy(updatedAt = System.currentTimeMillis()) })
    }

    suspend fun markAsShipped(id: Long) = withContext(Dispatchers.IO) {
        orderDao.updateFulfillmentStatus(id, FulfillmentStatus.SHIPPED)
    }

    suspend fun markAsProcessing(id: Long) = withContext(Dispatchers.IO) {
        orderDao.updateFulfillmentStatus(id, FulfillmentStatus.PROCESSING)
    }

    suspend fun updateFulfillmentStatus(id: Long, status: FulfillmentStatus) = withContext(Dispatchers.IO) {
        orderDao.updateFulfillmentStatus(id, status)
    }

    suspend fun moveToTrash(id: Long) = withContext(Dispatchers.IO) {
        orderDao.setTrashed(id, true)
    }

    suspend fun restoreFromTrash(id: Long) = withContext(Dispatchers.IO) {
        orderDao.setTrashed(id, false)
    }

    suspend fun deletePermanently(id: Long) = withContext(Dispatchers.IO) {
        orderDao.deletePermanently(id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        orderDao.emptyTrash()
    }

    suspend fun getAllExistingOrderIds(): Set<Long> = withContext(Dispatchers.IO) {
        orderDao.getAllExistingOrderIds().toSet()
    }

    suspend fun getAllOrdersDirect(): List<OrderEntity> = withContext(Dispatchers.IO) {
        orderDao.getAllOrdersDirect()
    }

    suspend fun getOrderCountDirect(): Int = withContext(Dispatchers.IO) {
        orderDao.getOrderCountDirect()
    }

    suspend fun restoreOrders(orders: List<OrderEntity>) = withContext(Dispatchers.IO) {
        orderDao.insertOrders(orders)
    }

    /**
     * Executes atomic restoration of both catalog products and orders inside a SINGLE Room transaction.
     * Both catalog and order operations use the injected AppDatabase instance.
     * All existing-ID lookups and conflict decisions occur within the transaction.
     * If any exception occurs during catalog or order insertion, the entire transaction rolls back.
     */
    suspend fun restoreDatabaseSafely(
        orders: List<OrderEntity>,
        catalogProducts: List<CatalogProductEntity>,
        conflictStrategy: RestoreConflictStrategy
    ): RestoreSummary = withContext(Dispatchers.IO) {
        database.withTransaction {
            // 1. Restore Catalog Products inside the transaction
            if (catalogProducts.isNotEmpty()) {
                database.catalogDao().insertProducts(catalogProducts)
            }

            // 2. Perform existing-ID lookup inside the transaction
            val existingIds = orderDao.getAllExistingOrderIds().toSet()
            var inserted = 0
            var renumbered = 0
            var skipped = 0
            var overwritten = 0

            // 3. Process orders according to conflictStrategy inside the transaction
            for (incoming in orders) {
                val hasConflict = incoming.id > 0L && existingIds.contains(incoming.id)
                if (hasConflict) {
                    when (conflictStrategy) {
                        RestoreConflictStrategy.PRESERVE_EXISTING_ASSIGN_NEW_IDS -> {
                            // Assign id = 0L to auto-generate a new ID, completely preserving existing order!
                            orderDao.insertOrder(incoming.copy(id = 0L))
                            renumbered++
                        }
                        RestoreConflictStrategy.SKIP_CONFLICTS -> {
                            // Skip order completely to preserve current record
                            skipped++
                        }
                        RestoreConflictStrategy.OVERWRITE_EXISTING -> {
                            // Overwrite only when explicitly confirmed
                            orderDao.insertOrder(incoming)
                            overwritten++
                        }
                    }
                } else {
                    orderDao.insertOrder(incoming)
                    inserted++
                }
            }

            RestoreSummary(
                totalIncomingOrders = orders.size,
                totalIncomingProducts = catalogProducts.size,
                insertedOrdersCount = inserted,
                renumberedOrdersCount = renumbered,
                skippedOrdersCount = skipped,
                overwrittenOrdersCount = overwritten,
                restoredPhotosCount = 0,
                safetySnapshotPath = null
            )
        }
    }

    suspend fun restoreOrdersSafely(
        orders: List<OrderEntity>,
        conflictStrategy: RestoreConflictStrategy
    ): RestoreSummary = restoreDatabaseSafely(
        orders = orders,
        catalogProducts = emptyList(),
        conflictStrategy = conflictStrategy
    )
}
