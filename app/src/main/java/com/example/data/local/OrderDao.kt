package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CourierSyncStatus
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.OrderType
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders WHERE isTrashed = 0 ORDER BY id DESC")
    fun getAllActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isTrashed = 0 AND fulfillmentStatus = :status ORDER BY id DESC")
    fun getOrdersByStatus(status: FulfillmentStatus): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isTrashed = 0 AND orderType = :type ORDER BY id DESC")
    fun getOrdersByType(type: OrderType): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isTrashed = 0 AND priority = 'URGENT' ORDER BY id DESC")
    fun getUrgentOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isTrashed = 0 AND isScheduled = 1 ORDER BY id DESC")
    fun getScheduledOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isTrashed = 0 AND courierSyncStatus = 'PENDING_SYNC' ORDER BY id DESC")
    fun getPendingCourierSyncOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isTrashed = 0 AND courierSyncStatus = 'PENDING_SYNC'")
    suspend fun getPendingCourierSyncOrdersList(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun getTrashedOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    fun getOrderById(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderByIdDirect(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE customerPhone = :phone AND isTrashed = 0")
    suspend fun getOrdersByPhone(phone: String): List<OrderEntity>

    @Query("""
        SELECT * FROM orders 
        WHERE isTrashed = 0 AND (
            invoiceNo LIKE '%' || :query || '%' OR 
            customerName LIKE '%' || :query || '%' OR 
            customerPhone LIKE '%' || :query || '%' OR 
            district LIKE '%' || :query || '%' OR 
            thana LIKE '%' || :query || '%' OR 
            courierTrackingId LIKE '%' || :query || '%'
        )
        ORDER BY id DESC
    """)
    fun searchOrders(query: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Update
    suspend fun updateOrders(orders: List<OrderEntity>)

    @Transaction
    suspend fun updateOrdersTransaction(orders: List<OrderEntity>) {
        updateOrders(orders)
    }

    @Query("UPDATE orders SET fulfillmentStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFulfillmentStatus(id: Long, status: FulfillmentStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE orders 
        SET courierTrackingId = :trackingId, 
            courierSyncStatus = :syncStatus, 
            fulfillmentStatus = :status,
            updatedAt = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateCourierTracking(
        id: Long,
        trackingId: String,
        syncStatus: CourierSyncStatus,
        status: FulfillmentStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE orders SET isTrashed = :isTrashed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTrashed(id: Long, isTrashed: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM orders WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM orders WHERE isTrashed = 0")
    fun getTotalOrdersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCountDirect(): Int

    @Query("SELECT id FROM orders")
    suspend fun getAllExistingOrderIds(): List<Long>

    @Query("SELECT * FROM orders ORDER BY id ASC")
    suspend fun getAllOrdersDirect(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrdersIgnore(orders: List<OrderEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM orders WHERE isTrashed = 0 AND fulfillmentStatus = :status")
    fun getCountByStatus(status: FulfillmentStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE isTrashed = 0 AND courierSyncStatus = 'PENDING_SYNC'")
    fun getPendingSyncCount(): Flow<Int>
}
