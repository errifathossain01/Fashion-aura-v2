package com.example.data.parser

import com.example.data.model.DeliveryTier
import com.example.data.model.OrderItem
import com.example.data.model.OrderType

/**
 * Indicates confidence level for an extracted or inferred field.
 */
enum class FieldConfidence {
    EXPLICIT,      // Derived from explicit label or unmistakable pattern
    INFERRED,      // Inferred with supporting evidence (e.g. district-based delivery fee)
    AMBIGUOUS,     // Multiple competing candidates found; user resolution required
    MISSING        // Field not present in input
}

/**
 * Represents an individual dress item inside a multi-item parsed order.
 */
data class ParsedOrderItem(
    val dressName: String = "ফ্যাশন ড্রেস",
    val size: String = "",
    val color: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 1500.0,
    val photoUri: String? = null
) {
    fun toOrderItem(): OrderItem {
        return OrderItem(
            name = dressName,
            size = size,
            color = color,
            quantity = quantity,
            unitPrice = unitPrice,
            photoUri = photoUri
        )
    }
}

/**
 * Immutable draft holding parsed order information, validation status,
 * confidence warnings, and alternative candidates for disambiguation.
 */
data class ParsedOrderDraft(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val district: String = "Dhaka",
    val thana: String = "",
    val items: List<ParsedOrderItem> = emptyList(),
    // Backward-compatible single-item fields
    val dressName: String = "ফ্যাশন ড্রেস",
    val size: String = "", // Kept empty when not specified
    val color: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 1500.0,
    val advanceAmount: Double = 0.0,
    val deliveryTier: DeliveryTier = DeliveryTier.DHAKA_INSIDE,
    val deliveryCharge: Double = 80.0,
    val totalBill: Double = 1580.0,
    val orderType: OrderType = OrderType.REGULAR,
    val specialInstructions: String = "",
    val rawText: String = "",
    val photoUri: String? = null,
    val isPriceExplicitInMessage: Boolean = false,
    val isSizeExplicitInMessage: Boolean = false,
    val isColorExplicitInMessage: Boolean = false,
    val isDeliveryExplicitInMessage: Boolean = false,
    val isAdvanceExplicitInMessage: Boolean = false,
    val confidenceWarnings: List<String> = emptyList(),
    val alternativeCandidates: Map<String, List<String>> = emptyMap(),
    val fieldConfidences: Map<String, FieldConfidence> = emptyMap()
) {
    val thanaUpazila: String
        get() = thana

    val codDue: Double
        get() = (totalBill - advanceAmount).coerceAtLeast(0.0)

    val subtotal: Double
        get() = if (items.isNotEmpty()) items.sumOf { it.unitPrice * it.quantity } else (unitPrice * quantity)

    fun toOrderItem(): OrderItem {
        return items.firstOrNull()?.toOrderItem() ?: OrderItem(
            name = dressName,
            size = size,
            color = color,
            quantity = quantity,
            unitPrice = unitPrice,
            photoUri = photoUri
        )
    }

    fun toOrderItems(): List<OrderItem> {
        return if (items.isNotEmpty()) {
            items.map { it.toOrderItem() }
        } else {
            listOf(toOrderItem())
        }
    }
}

