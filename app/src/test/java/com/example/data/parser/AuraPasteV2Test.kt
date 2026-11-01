package com.example.data.parser

import com.example.data.model.DeliveryTier
import com.example.data.model.OrderType
import org.junit.Assert.*
import org.junit.Test

/**
 * Aura Paste V2 comprehensive regression & edge-case test suite.
 * All test cases use synthetic fixtures and verify deterministic parsing.
 */
class AuraPasteV2Test {

    @Test
    fun `test 1 - unlabelled Bengali order`() {
        val input = """
            নুসরাত জাহান
            ০১৭০৯৮৭৬৫৪৩
            বাড়ি ১২, রোড ৫, ব্লক সি, বনশ্রী, ঢাকা
            কটন সালোয়ার কামিজ
            সাইজ ৩৮
            দাম ১৪০০ টাকা
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("নুসরাত জাহান", draft.customerName)
        assertEquals("01709876543", draft.customerPhone)
        assertTrue(draft.customerAddress.contains("বনশ্রী"))
        assertTrue(draft.customerAddress.contains("12") || draft.customerAddress.contains("১২"))
        assertEquals("Dhaka", draft.district)
        assertEquals("38", draft.size)
        assertEquals(1400.0, draft.unitPrice, 0.01)
        assertEquals("কটন সালোয়ার কামিজ", draft.dressName)
        assertEquals(DeliveryTier.DHAKA_INSIDE, draft.deliveryTier)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(1480.0, draft.totalBill, 0.01)
    }

    @Test
    fun `test 2 - unlabelled English order`() {
        val input = """
            Sadia Sultana
            01812345678
            House 24, Road 7, Block D, Mirpur 10, Dhaka
            Three Piece
            Size 40
            Price 1600
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("Sadia Sultana", draft.customerName)
        assertEquals("01812345678", draft.customerPhone)
        assertTrue(draft.customerAddress.contains("House 24") || draft.customerAddress.contains("Mirpur 10"))
        assertEquals("Dhaka", draft.district)
        assertEquals("40", draft.size)
        assertEquals(1600.0, draft.unitPrice, 0.01)
        assertEquals("Three Piece", draft.dressName)
        assertEquals(DeliveryTier.DHAKA_INSIDE, draft.deliveryTier)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(1680.0, draft.totalBill, 0.01)
    }

    @Test
    fun `test 3 - Banglish order with advance and baki cod`() {
        val input = """
            nam: Tanzila Akter
            phn: 01912345678
            thikana: Ward 3, Puthia, Rajshahi
            size: 42
            dam: 1500 tk
            adv 200 baki cod
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("Tanzila Akter", draft.customerName)
        assertEquals("01912345678", draft.customerPhone)
        assertEquals("Rajshahi", draft.district)
        assertTrue(draft.thana.contains("Puthia", ignoreCase = true) || draft.customerAddress.contains("Puthia", ignoreCase = true))
        assertEquals("42", draft.size)
        assertEquals(1500.0, draft.unitPrice, 0.01)
        assertEquals(200.0, draft.advanceAmount, 0.01)
        assertEquals(DeliveryTier.OUTSIDE_DHAKA, draft.deliveryTier)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(1650.0, draft.totalBill, 0.01)
        assertEquals(1450.0, draft.codDue, 0.01)
    }

    @Test
    fun `test 4 - mixed-script Bengali and English numerals`() {
        val input = """
            নাম: ফারিয়া ইসলাম
            ফোন: 01711২3৪৫৬৭
            ঠিকানা: ধানমন্ডি ৪/এ, ঢাকা
            সাইজ: ৩৮
            দাম: 1500 টাকা
            ডেলিভারি: ৮০ টাকা
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("ফারিয়া ইসলাম", draft.customerName)
        assertEquals("01711234567", draft.customerPhone)
        assertEquals("Dhaka", draft.district)
        assertEquals("38", draft.size)
        assertEquals(1500.0, draft.unitPrice, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(1580.0, draft.totalBill, 0.01)
    }

    @Test
    fun `test 5 - house number 42 alongside dress size 42`() {
        val input = """
            নাম: আফরিন জাহান
            ফোন: 01700112233
            ঠিকানা: বাসা ৪২, রোড ৭, উত্তরা, ঢাকা
            ড্রেস: কুর্তি
            সাইজ: 42
            দাম: 1200
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("আফরিন জাহান", draft.customerName)
        assertEquals("01700112233", draft.customerPhone)
        assertTrue(draft.customerAddress.contains("42") || draft.customerAddress.contains("৪২"))
        assertEquals("42", draft.size)
        assertEquals(1200.0, draft.unitPrice, 0.01)
        assertEquals(FieldConfidence.EXPLICIT, draft.fieldConfidences["size"])
    }

    @Test
    fun `test 6 - multiple phone numbers with secondary candidate`() {
        val input = """
            নাম: তানহা
            ফোন: 01700112233, 01800112233
            ঠিকানা: মালিবাগ, ঢাকা
            সাইজ: 36
            দাম: 1300
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("01700112233", draft.customerPhone)
        val candidates = draft.alternativeCandidates["phone"]
        assertNotNull(candidates)
        assertTrue(candidates!!.contains("01800112233"))
        assertTrue(draft.confidenceWarnings.any { it.contains("একাধিক ফোন") })
    }

    @Test
    fun `test 7 - missing or ambiguous district and thana flags warning`() {
        val input = """
            নাম: সুবর্ণা
            ফোন: 01700001122
            ঠিকানা: নয়াবাজার ৩ নম্বর গলি
            সাইজ: 38
            দাম: 1200
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("সুবর্ণা", draft.customerName)
        assertEquals("01700001122", draft.customerPhone)
        assertTrue(draft.confidenceWarnings.any { it.contains("জেলা") || it.contains("ঠিকানা") } ||
                draft.fieldConfidences["customerAddress"] != FieldConfidence.EXPLICIT)
    }

    @Test
    fun `test 8 - advance payment followed by remaining COD`() {
        val input = """
            নাম: মারিয়া
            ফোন: 01712340000
            ঠিকানা: চকবাজার, কুমিল্লা
            দাম: ১৫০০
            ডেলিভারি: ১৫০
            advance 500 bkash korlam
            baki taka 1150 delivery man k dibo
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("মারিয়া", draft.customerName)
        assertEquals("01712340000", draft.customerPhone)
        assertTrue(draft.district.equals("Comilla", ignoreCase = true) || draft.district.equals("Cumilla", ignoreCase = true))
        assertEquals(1500.0, draft.unitPrice, 0.01)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(500.0, draft.advanceAmount, 0.01)
        assertEquals(1650.0, draft.totalBill, 0.01)
        assertEquals(1150.0, draft.codDue, 0.01)
    }

    @Test
    fun `test 9 - delivery charge plus advance payment`() {
        val input = """
            নাম: রেশমা
            ফোন: 01811223344
            ঠিকানা: লালমাটিয়া, ঢাকা
            দাম: 1400
            ডেলিভারি চার্জ 80 টাকা অগ্রিম দেওয়া হয়েছে
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("রেশমা", draft.customerName)
        assertEquals("01811223344", draft.customerPhone)
        assertEquals(1400.0, draft.unitPrice, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(80.0, draft.advanceAmount, 0.01)
        assertEquals(1480.0, draft.totalBill, 0.01)
        assertEquals(1400.0, draft.codDue, 0.01)
    }

    @Test
    fun `test 10 - conflicting financial values flags review warning without overwriting explicit total`() {
        val input = """
            নাম: ফারহানা
            ফোন: 01799887766
            ঠিকানা: মিরপুর ২, ঢাকা
            দাম: 1500
            ডেলিভারি: 80
            টোটাল বিল: 2000
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(2000.0, draft.totalBill, 0.01)
        assertTrue(draft.confidenceWarnings.any { it.contains("হিসাবে অমিল") })
    }

    @Test
    fun `test 11 - multiple product lines with separate sizes and prices`() {
        val input = """
            নাম: কামরুন নাহার
            ফোন: 01611223344
            ঠিকানা: গুলশান ১, ঢাকা
            ১. কটন কুর্তি - সাইজ ৩৮ - দাম ১২০০
            ২. জর্জেট গাউন - সাইজ ৪০ - দাম ১৮০০
            ডেলিভারি: ৮০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("কামরুন নাহার", draft.customerName)
        assertEquals("01611223344", draft.customerPhone)
        assertEquals(2, draft.items.size)
        assertEquals("38", draft.items[0].size)
        assertEquals(1200.0, draft.items[0].unitPrice, 0.01)
        assertEquals("40", draft.items[1].size)
        assertEquals(1800.0, draft.items[1].unitPrice, 0.01)
        assertEquals(3000.0, draft.subtotal, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(3080.0, draft.totalBill, 0.01)
    }

    @Test
    fun `test 12 - missing name phone and price flags missing warnings`() {
        val input = """
            ঠিকানা: ধানমন্ডি, ঢাকা
            সাইজ: ৩৮
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("", draft.customerPhone)
        assertEquals(FieldConfidence.MISSING, draft.fieldConfidences["customerPhone"])
        assertEquals(FieldConfidence.MISSING, draft.fieldConfidences["customerName"])
        assertFalse(draft.isPriceExplicitInMessage)
        assertTrue(draft.confidenceWarnings.any { it.contains("ফোন নম্বর") })
    }

    @Test
    fun `test 13 - empty and whitespace only input`() {
        val draftEmpty = SmartOrderParser.parse("")
        assertEquals("", draftEmpty.customerName)
        assertEquals("", draftEmpty.customerPhone)
        assertEquals(0, draftEmpty.items.size)

        val draftWhitespace = SmartOrderParser.parse("   \n\t  \r\n")
        assertEquals("", draftWhitespace.customerName)
        assertEquals("", draftWhitespace.customerPhone)
        assertEquals(0, draftWhitespace.items.size)
    }

    @Test
    fun `test 14 - existing labeled format regression case`() {
        val input = """
            নাম: Rony akter
            মোবাইল: 01316701932
            ঠিকানা: Shantigonj Upazila Health Complex, Sunamganj
            ড্রেস: কটন সালোয়ার কামিজ
            সাইজ: 42
            কালার: মেরুন
            টোটাল বিল: ১৩৫০+১৫০=১৫০০ টাকা
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("Rony akter", draft.customerName)
        assertEquals("01316701932", draft.customerPhone)
        assertTrue(draft.customerAddress.contains("Shantigonj"))
        assertEquals("Sunamganj", draft.district)
        assertEquals("কটন সালোয়ার কামিজ", draft.dressName)
        assertEquals("42", draft.size)
        assertEquals("মেরুন", draft.color)
        assertEquals(1350.0, draft.unitPrice, 0.01)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(1500.0, draft.totalBill, 0.01)
        assertEquals(DeliveryTier.OUTSIDE_DHAKA, draft.deliveryTier)
        assertEquals(OrderType.REGULAR, draft.orderType)
        assertTrue(draft.isPriceExplicitInMessage)
        assertTrue(draft.isSizeExplicitInMessage)
    }

    @Test
    fun `test 15 - compatibility with existing single item fields`() {
        val input = """
            নাম: মেহজাবিন
            ফোন: 01755667788
            ঠিকানা: আগ্রাবাদ, চট্টগ্রাম
            ড্রেস: সিল্ক কুর্তি
            সাইজ: 38
            কালার: পিস্তা
            দাম: 1400
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        // Verify top-level backward compatibility fields
        assertEquals("মেহজাবিন", draft.customerName)
        assertEquals("01755667788", draft.customerPhone)
        assertEquals("সিল্ক কুর্তি", draft.dressName)
        assertEquals("38", draft.size)
        assertEquals("পিস্তা", draft.color)
        assertEquals(1400.0, draft.unitPrice, 0.01)
        assertEquals(1, draft.quantity)

        // Verify items list matches single-item fields
        assertEquals(1, draft.items.size)
        assertEquals(draft.dressName, draft.items[0].dressName)
        assertEquals(draft.size, draft.items[0].size)
        assertEquals(draft.color, draft.items[0].color)
        assertEquals(draft.unitPrice, draft.items[0].unitPrice, 0.01)
    }
}
