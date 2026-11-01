package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SteadfastCreateOrderRequest(
    @Json(name = "invoice") val invoice: String,
    @Json(name = "recipient_name") val recipientName: String,
    @Json(name = "recipient_phone") val recipientPhone: String,
    @Json(name = "recipient_address") val recipientAddress: String,
    @Json(name = "cod_amount") val codAmount: Double,
    @Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class SteadfastConsignmentData(
    @Json(name = "consignment_id") val consignmentId: Long? = null,
    @Json(name = "invoice") val invoice: String? = null,
    @Json(name = "tracking_code") val trackingCode: String? = null,
    @Json(name = "recipient_name") val recipientName: String? = null,
    @Json(name = "recipient_phone") val recipientPhone: String? = null,
    @Json(name = "recipient_address") val recipientAddress: String? = null,
    @Json(name = "cod_amount") val codAmount: Double? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class SteadfastCreateOrderResponse(
    @Json(name = "status") val status: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "consignment") val consignment: SteadfastConsignmentData? = null,
    @Json(name = "errors") val errors: Map<String, List<String>>? = null
)

@JsonClass(generateAdapter = true)
data class SteadfastBalanceResponse(
    @Json(name = "status") val status: Int? = null,
    @Json(name = "current_balance") val currentBalance: Double? = null,
    @Json(name = "message") val message: String? = null
)
