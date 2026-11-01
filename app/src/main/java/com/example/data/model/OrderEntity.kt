package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class OrderType(val displayNameEn: String, val displayNameBn: String) {
    REGULAR("Regular", "রেগুলার"),
    PRE_ORDER("Pre-Order", "প্রি-অর্ডার"),
    CUSTOMIZED("Customized", "কাস্টমাইজড"),
    EXCHANGE("Exchange", "এক্সেচেঞ্জ"),
    SCHEDULED("Scheduled", "সিডিউল পার্সেল")
}

enum class Priority(val displayNameEn: String, val displayNameBn: String) {
    NORMAL("Normal", "সাধারণ"),
    URGENT("Urgent", "জরুরি")
}

enum class PaymentStatus(val displayNameEn: String, val displayNameBn: String) {
    UNPAID("Unpaid", "বাকি"),
    PARTIAL("Partial", "আংশিক"),
    PAID("Paid", "পরিশোধিত")
}

enum class FulfillmentStatus(val displayNameEn: String, val displayNameBn: String) {
    PRE_ORDER_PENDING("Pre-Order Pending", "প্রি-অর্ডার অপেক্ষমাণ"),
    NEW("New", "নতুন"),
    PROCESSING("Processing", "প্রসেসিং"),
    READY_TO_SHIP("Ready to Ship", "রেডি টু শিপ"),
    SHIPPED("Shipped", "শিপড"),
    DELIVERED("Delivered", "ডেলিভার্ড"),
    CANCELLED("Cancelled", "বাতিল"),
    RETURNED("Returned", "রিটার্ন")
}

enum class DeliveryTier(val displayNameEn: String, val displayNameBn: String, val defaultFee: Double) {
    DHAKA_INSIDE("Dhaka City", "ঢাকা সিটি", 80.0),
    DHAKA_SUB("Sub-Dhaka", "ঢাকার আশেপাশে", 120.0),
    OUTSIDE_DHAKA("Outside Dhaka", "ঢাকার বাইরে", 150.0)
}

enum class CourierService(val displayName: String) {
    STEADFAST("Steadfast"),
    PATHAO("Pathao")
}

enum class CourierSyncStatus {
    NONE,
    PENDING_SYNC,
    SYNCED
}

@JsonClass(generateAdapter = true)
data class OrderItem(
    val name: String = "",
    val size: String = "",
    val color: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val photoUri: String? = null
)

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["invoiceNo"]),
        Index(value = ["customerPhone"]),
        Index(value = ["fulfillmentStatus"]),
        Index(value = ["orderType"]),
        Index(value = ["isTrashed"])
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val invoiceNo: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val district: String = "Dhaka",
    val thana: String = "",
    val orderType: OrderType = OrderType.REGULAR,
    val priority: Priority = Priority.NORMAL,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val fulfillmentStatus: FulfillmentStatus = FulfillmentStatus.NEW,
    val items: List<OrderItem> = emptyList(),
    val deliveryTier: DeliveryTier = DeliveryTier.DHAKA_INSIDE,
    val deliveryCharge: Double = 80.0,
    val advanceAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val isScheduled: Boolean = false,
    val requestedDeliveryDate: String = "",
    val doNotDispatchBeforeDate: String = "",
    val specialInstructions: String = "",
    val courierService: CourierService = CourierService.STEADFAST,
    val courierTrackingId: String = "",
    val courierSyncStatus: CourierSyncStatus = CourierSyncStatus.NONE,
    val riderNote: String = "পার্সেল খুলে দেখে পছন্দ না হলে বা না নিলে ডেলিভারি চার্জ রাইডারকে প্রদান করতে হবে।",
    val isTrashed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double
        get() = items.sumOf { it.unitPrice * it.quantity }

    val totalBill: Double
        get() = (subtotal + deliveryCharge - discountAmount).coerceAtLeast(0.0)

    val codAmount: Double
        get() = if (paymentStatus == PaymentStatus.PAID) 0.0 else (totalBill - advanceAmount).coerceAtLeast(0.0)

    val itemsSummary: String
        get() = if (items.isEmpty()) "ফ্যাশন ড্রেস" else items.joinToString(", ") {
            if (it.size.isNotBlank()) "${it.name} (${it.size})" else it.name
        }
}
