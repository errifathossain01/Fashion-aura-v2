package com.example.data.repository

import com.example.data.local.OrderDao
import com.example.data.model.CourierSyncStatus
import com.example.data.model.FulfillmentStatus
import com.example.data.model.OrderEntity
import com.example.data.model.SteadfastCreateOrderRequest
import com.example.data.remote.SteadfastApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CourierRepository(
    private val apiService: SteadfastApiService,
    private val orderDao: OrderDao
) {
    private val syncMutex = Mutex()

    suspend fun getBalance(apiKey: String, secretKey: String): Result<Double> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank() || secretKey.isBlank()) {
                    return@withContext Result.failure(Exception("Steadfast API Key বা Secret Key খালি। অনুগ্রহ করে সেটিংসে কি প্রদান করুন।"))
                }
                val response = apiService.getBalance(apiKey = apiKey, secretKey = secretKey)
                if (response.status == 200 && response.currentBalance != null) {
                    Result.success(response.currentBalance)
                } else {
                    Result.failure(Exception(response.message ?: "ব্যালেন্স লোড করা যায়নি (Status: ${response.status})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun bookSingleOrder(order: OrderEntity, apiKey: String, secretKey: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank() || secretKey.isBlank()) {
                    // Keep tracking ID blank ("") & mark as PENDING_SYNC if offline or keys missing
                    orderDao.updateCourierTracking(
                        id = order.id,
                        trackingId = "",
                        syncStatus = CourierSyncStatus.PENDING_SYNC,
                        status = order.fulfillmentStatus
                    )
                    return@withContext Result.failure(Exception("API Credentials নেই। অর্ডারটি পেন্ডিং সিঙ্ক কিউতে রাখা হয়েছে।"))
                }

                val exchangePrefix = if (order.orderType == com.example.data.model.OrderType.EXCHANGE) "🔄 [এক্সচেঞ্জ পার্সেল: পুরাতন প্রোডাক্ট সংগ্রহ করে নতুন পার্সেল ডেলিভারি করুন] " else ""
                val itemsDescription = if (order.items.isNotEmpty()) {
                    order.items.joinToString("; ") { item ->
                        buildString {
                            append(item.name.ifBlank { "ড্রেস" })
                            val details = mutableListOf<String>()
                            if (item.size.isNotBlank()) details.add("Size: ${item.size}")
                            if (item.color.isNotBlank()) details.add("Color: ${item.color}")
                            if (details.isNotEmpty()) append(" (${details.joinToString(", ")})")
                            if (item.quantity > 1) append(" x ${item.quantity}")
                        }
                    }
                } else ""

                val fullNote = buildString {
                    if (exchangePrefix.isNotBlank()) append(exchangePrefix)
                    if (itemsDescription.isNotBlank()) append("👗 আইটেম: $itemsDescription. ")
                    if (order.riderNote.isNotBlank()) append(order.riderNote)
                }.trim().ifBlank { "Fashion Aura - পার্সেলটি যত্নসহকারে ডেলিভারি করুন।" }

                val request = SteadfastCreateOrderRequest(
                    invoice = order.invoiceNo,
                    recipientName = order.customerName,
                    recipientPhone = order.customerPhone,
                    recipientAddress = "${order.customerAddress}, ${order.district}",
                    codAmount = order.codAmount,
                    note = fullNote
                )

                val response = apiService.createOrder(
                    apiKey = apiKey,
                    secretKey = secretKey,
                    request = request
                )

                // Future-proof: Accept official consignment_id or tracking_code as returned by the API
                // without imposing hardcoded digit lengths (9, 10, or more) or prefix restrictions (317, etc.)
                val officialConsignmentId = response.consignment?.consignmentId?.toString()?.trim()
                    ?: response.consignment?.trackingCode?.trim()

                if (response.status == 200 && !officialConsignmentId.isNullOrBlank()) {
                    // Automatic transition to READY_TO_SHIP with real official ID from server
                    orderDao.updateCourierTracking(
                        id = order.id,
                        trackingId = officialConsignmentId,
                        syncStatus = CourierSyncStatus.SYNCED,
                        status = FulfillmentStatus.READY_TO_SHIP
                    )
                    Result.success(officialConsignmentId)
                } else {
                    val errMsg = response.message ?: "কুরিয়ার API থেকে আইডি পাওয়া যায়নি"
                    orderDao.updateCourierTracking(
                        id = order.id,
                        trackingId = "",
                        syncStatus = CourierSyncStatus.PENDING_SYNC,
                        status = order.fulfillmentStatus
                    )
                    Result.failure(Exception(errMsg))
                }
            } catch (e: Exception) {
                orderDao.updateCourierTracking(
                    id = order.id,
                    trackingId = "",
                    syncStatus = CourierSyncStatus.PENDING_SYNC,
                    status = order.fulfillmentStatus
                )
                Result.failure(e)
            }
        }
    }

    suspend fun manualSetConsignmentId(orderId: Long, consignmentId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanId = consignmentId.trim()
                val syncStatus = if (cleanId.isNotBlank()) CourierSyncStatus.SYNCED else CourierSyncStatus.NONE
                // Automatic transition to READY_TO_SHIP upon saving Consignment ID!
                val status = if (cleanId.isNotBlank()) FulfillmentStatus.READY_TO_SHIP else FulfillmentStatus.PROCESSING

                orderDao.updateCourierTracking(
                    id = orderId,
                    trackingId = cleanId,
                    syncStatus = syncStatus,
                    status = status
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun markAsPendingSync(orderId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                orderDao.updateCourierTracking(
                    id = orderId,
                    trackingId = "",
                    syncStatus = CourierSyncStatus.PENDING_SYNC,
                    status = FulfillmentStatus.PROCESSING
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun syncPendingOrders(apiKey: String, secretKey: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            syncMutex.withLock {
                try {
                    val pendingOrders = orderDao.getPendingCourierSyncOrdersList()
                    if (pendingOrders.isEmpty()) {
                        return@withContext Result.success(0)
                    }

                    var successCount = 0
                    for (order in pendingOrders) {
                        val result = bookSingleOrder(order, apiKey, secretKey)
                        if (result.isSuccess) {
                            successCount++
                        }
                    }
                    Result.success(successCount)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }
}
