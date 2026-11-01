package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.OrderEntity
import java.net.URLEncoder

object MessagingHelper {

    fun generateCustomerConfirmationMessage(order: OrderEntity, useBanglaDigits: Boolean = true): String {
        val trackingCodeText = if (order.courierTrackingId.isNotBlank()) {
            order.courierTrackingId
        } else {
            "শীঘ্রই পাঠানো হবে"
        }

        val totalBillStr = BanglaFormatter.formatCurrency(order.totalBill, useBanglaDigits)
        val advanceStr = BanglaFormatter.formatCurrency(order.advanceAmount, useBanglaDigits)
        val codStr = BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)
        val deliveryFeeStr = BanglaFormatter.formatCurrency(order.deliveryCharge, useBanglaDigits)

        return """
প্রিয় ${order.customerName} Sir/Mam,
Fashion Aura-তে আপনার অর্ডারটি সফলভাবে কনফার্ম করা হয়েছে। 🌸
📦 ইনভয়েস নং: #${order.invoiceNo}
👗 আইটেম: ${order.itemsSummary}
🚚 কুরিয়ার: ${order.courierService.displayName} Courier
🔖 ট্র্যাকিং কোড: $trackingCodeText
💵 মোট বিল: $totalBillStr (অগ্রিম: $advanceStr | বাকি COD: $codStr)
🛵 ডেলিভারি নির্দেশনা: পার্সেল রিটার্ন করলে ডেলিভারি চার্জ $deliveryFeeStr নিয়ে রিটার্ন করবেন। ধন্যবাদ!
        """.trimIndent()
    }

    fun dialCustomer(context: Context, phone: String) {
        val cleanPhone = phone.replace(Regex("""[^0-9+]"""), "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanPhone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        var cleanPhone = phone.replace(Regex("""[^0-9]"""), "")
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "88$cleanPhone"
        } else if (!cleanPhone.startsWith("880")) {
            cleanPhone = "880$cleanPhone"
        }

        val encodedMsg = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        val url = "https://wa.me/$cleanPhone?text=$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendSms(context: Context, phone: String, message: String) {
        val cleanPhone = phone.replace(Regex("""[^0-9+]"""), "")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanPhone")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
