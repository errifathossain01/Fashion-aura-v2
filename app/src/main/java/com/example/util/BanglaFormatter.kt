package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BanglaFormatter {

    private val ENG_TO_BANGLA_DIGITS = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(ENG_TO_BANGLA_DIGITS[ch] ?: ch)
        }
        return sb.toString()
    }

    fun formatNumber(number: Number, useBanglaDigits: Boolean = true): String {
        val str = if (number is Double || number is Float) {
            val d = number.toDouble()
            if (d == d.toLong().toDouble()) d.toLong().toString() else DecimalFormat("#,##0.##").format(d)
        } else {
            DecimalFormat("#,###").format(number.toLong())
        }
        return if (useBanglaDigits) toBanglaDigits(str) else str
    }

    fun formatCurrency(amount: Double, useBanglaDigits: Boolean = true): String {
        val formattedNum = if (amount == amount.toLong().toDouble()) {
            DecimalFormat("#,###").format(amount.toLong())
        } else {
            DecimalFormat("#,##0.00").format(amount)
        }
        val numStr = if (useBanglaDigits) toBanglaDigits(formattedNum) else formattedNum
        return "৳$numStr"
    }

    fun formatDate(timestamp: Long, useBanglaDigits: Boolean = false): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
        val formatted = sdf.format(Date(timestamp))
        return if (useBanglaDigits) toBanglaDigits(formatted) else formatted
    }

    fun formatShortDate(timestamp: Long, useBanglaDigits: Boolean = false): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val formatted = sdf.format(Date(timestamp))
        return if (useBanglaDigits) toBanglaDigits(formatted) else formatted
    }
}
