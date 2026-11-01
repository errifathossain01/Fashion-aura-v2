package com.example.data.parser

import com.example.data.model.DeliveryTier
import com.example.data.model.OrderType
import org.junit.Assert.*
import org.junit.Test

/**
 * Aura Paste V2 Stage D Adversarial & Rigorous Stress Testing Suite.
 *
 * Verifies edge-case robustness across:
 * 1. Financial reconciliations, conflicting COD/totals, unquantified advance/cod words
 * 2. Multi-item quantity, missing item prices, aggregate vs item sum conflicts
 * 3. Address numbers (house, road, sector, ward, flat) vs dress size collision resistance
 * 4. District inference, embedded thana extraction, missing district diagnostics
 * 5. Messy conversational noise, emojis, bullets, multiple phones, pre-order detection
 */
class AuraPasteAdversarialTest {

    // =========================================================================
    // SECTION 1: FINANCIAL EDGE CASES
    // =========================================================================

    @Test
    fun `financial - product price plus delivery plus advance plus matching COD`() {
        val input = """
            নাম: সুমাইয়া খানম
            ফোন: 01712345678
            ঠিকানা: আগ্রাবাদ, চট্টগ্রাম
            দাম: ১৪০০
            ডেলিভারি: ১৫০
            অগ্রিম: ৩০০
            বাকি: ১২৫০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("সুমাইয়া খানম", draft.customerName)
        assertEquals("01712345678", draft.customerPhone)
        assertEquals("Chittagong", draft.district)
        assertEquals(1400.0, draft.unitPrice, 0.01)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(300.0, draft.advanceAmount, 0.01)
        assertEquals(1550.0, draft.totalBill, 0.01)
        assertEquals(1250.0, draft.codDue, 0.01)
        // No conflict warning should be emitted since 1400 + 150 = 1550 = 300 + 1250
        assertFalse(draft.confidenceWarnings.any { it.contains("হিসাবে অমিল") })
    }

    @Test
    fun `financial - stated COD amount conflicts with calculated remainder`() {
        val input = """
            নাম: সাদিয়া
            ফোন: 01811223344
            ঠিকানা: মিরপুর ১০, ঢাকা
            দাম: ১২০০
            ডেলিভারি: ৮০
            অগ্রিম: ২০০
            বাকি: ৮০০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        // Calculated: 1200 + 80 = 1280 total, 1280 - 200 = 1080 remaining
        assertEquals(1280.0, draft.totalBill, 0.01)
        assertEquals(200.0, draft.advanceAmount, 0.01)
        assertEquals(1080.0, draft.codDue, 0.01)
        // Stated remainder 800 does not match calculated 1080 -> warning emitted & candidate retained
        assertTrue(draft.confidenceWarnings.any { it.contains("হিসাবে অমিল") })
        val codCandidates = draft.alternativeCandidates["codDue"]
        assertNotNull(codCandidates)
        assertTrue(codCandidates!!.contains("800"))
    }

    @Test
    fun `financial - delivery charge described as paid in advance - word order 1`() {
        val input = """
            নাম: ফারজানা
            ফোন: 01911223344
            ঠিকানা: লালবাগ, ঢাকা
            দাম: 1500
            ডেলিভারি চার্জ ৮০ টাকা অগ্রিম দেওয়া হয়েছে
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(1500.0, draft.unitPrice, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(80.0, draft.advanceAmount, 0.01)
        assertEquals(1580.0, draft.totalBill, 0.01)
        assertEquals(1500.0, draft.codDue, 0.01)
        assertTrue(draft.isAdvanceExplicitInMessage)
        assertTrue(draft.isDeliveryExplicitInMessage)
    }

    @Test
    fun `financial - delivery charge described as paid in advance - word order 2`() {
        val input = """
            নাম: রুমানা
            ফোন: 01611223344
            ঠিকানা: জিইসি মোড়, চট্টগ্রাম
            দাম: 1600
            delivery advance 150 tk
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(1600.0, draft.unitPrice, 0.01)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(150.0, draft.advanceAmount, 0.01)
        assertEquals(1750.0, draft.totalBill, 0.01)
        assertEquals(1600.0, draft.codDue, 0.01)
    }

    @Test
    fun `financial - advance mentioned without explicit total or price`() {
        val input = """
            নাম: বিলকিস
            ফোন: 01799887766
            ঠিকানা: উত্তরা ৪ নম্বর সেক্টর, ঢাকা
            সাইজ: ৩৮
            adv 300 tk
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(300.0, draft.advanceAmount, 0.01)
        assertFalse(draft.isPriceExplicitInMessage)
        assertEquals(FieldConfidence.MISSING, draft.fieldConfidences["unitPrice"])
        assertTrue(draft.confidenceWarnings.any { it.contains("মূল্য স্পষ্ট নয়") })
        // Defaults to 1500 price + 80 delivery = 1580 total - 300 advance = 1280 due
        assertEquals(1500.0, draft.unitPrice, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(1580.0, draft.totalBill, 0.01)
        assertEquals(1280.0, draft.codDue, 0.01)
    }

    @Test
    fun `financial - banglish phrases mentioning advance or baki without numeric digits`() {
        val input = """
            nam: Sharmin
            phone: 01711002233
            thikana: Dhanmondi, Dhaka
            size: 38
            dam: 1400
            adv korechi baki cod e dibo
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(1400.0, draft.unitPrice, 0.01)
        assertEquals(0.0, draft.advanceAmount, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(1480.0, draft.totalBill, 0.01)
        assertEquals(1480.0, draft.codDue, 0.01)
        // Must emit warning indicating advance was mentioned without specific numeric amount
        assertTrue(draft.confidenceWarnings.any { it.contains("অগ্রিম") && it.contains("পরিমাণ") })
    }

    @Test
    fun `financial - multiple contradictory price labels outside multi-item lines`() {
        val input = """
            নাম: নুসরাত
            ফোন: 01722334455
            ঠিকানা: মিরপুর ১, ঢাকা
            দাম: ১২০০
            দাম: ১৪০০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        // Multiple distinct price labels must trigger diagnostic warning and candidate retention
        assertTrue(draft.confidenceWarnings.any { it.contains("একাধিক ভিন্ন মূল্য") })
        val priceCandidates = draft.alternativeCandidates["price"]
        assertNotNull(priceCandidates)
        assertTrue(priceCandidates!!.contains("1200"))
        assertTrue(priceCandidates.contains("1400"))
    }

    // =========================================================================
    // SECTION 2: MULTI-ITEM ORDERS & BOUNDARIES
    // =========================================================================

    @Test
    fun `multi-item - separate lines with sizes, prices, and quantities`() {
        val input = """
            নাম: আফরোজা পারভীন
            ফোন: 01511223344
            ঠিকানা: বনানী, ঢাকা
            ১. কটন সালোয়ার কামিজ - সাইজ ৩৮ - দাম ১২০০ - ২ পিস
            ২. সিল্ক কুর্তি - সাইজ ৪০ - দাম ১৫০০ - ১ পিস
            ডেলিভারি: ৮০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(2, draft.items.size)
        // Item 1: 1200 * 2 = 2400
        assertEquals("কটন সালোয়ার কামিজ", draft.items[0].dressName)
        assertEquals("38", draft.items[0].size)
        assertEquals(1200.0, draft.items[0].unitPrice, 0.01)
        assertEquals(2, draft.items[0].quantity)

        // Item 2: 1500 * 1 = 1500
        assertEquals("সিল্ক কুর্তি", draft.items[1].dressName)
        assertEquals("40", draft.items[1].size)
        assertEquals(1500.0, draft.items[1].unitPrice, 0.01)
        assertEquals(1, draft.items[1].quantity)

        // Subtotal = 2400 + 1500 = 3900 + 80 delivery = 3980
        assertEquals(3900.0, draft.subtotal, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(3980.0, draft.totalBill, 0.01)
    }

    @Test
    fun `multi-item - one item missing price triggers diagnostic warning`() {
        val input = """
            নাম: তাহমিনা
            ফোন: 01733445566
            ঠিকানা: ধানমন্ডি, ঢাকা
            ১. কটন কুর্তি - সাইজ ৩৮ - দাম ১২০০
            ২. জর্জেট গাউন - সাইজ ৪০
            ডেলিভারি: ৮০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(2, draft.items.size)
        assertEquals(1200.0, draft.items[0].unitPrice, 0.01)
        // Item 2 has no explicit price -> default 1500 assigned + warning emitted
        assertEquals(1500.0, draft.items[1].unitPrice, 0.01)
        assertTrue(draft.confidenceWarnings.any { it.contains("জর্জেট গাউন") && it.contains("মূল্য মেসেজে উল্লেখ নেই") })
    }

    @Test
    fun `multi-item - aggregate total bill conflicts with individual item sum`() {
        val input = """
            নাম: জাহানারা
            ফোন: 01744556677
            ঠিকানা: গুলশান, ঢাকা
            ১. কুর্তি - সাইজ ৩৮ - দাম ১০০০
            ২. গাউন - সাইজ ৪০ - দাম ১৫০০
            ডেলিভারি: ৮০
            টোটাল বিল: ৩২০০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        // Items sum: 1000 + 1500 + 80 = 2580, but message specifies 3200
        assertEquals(3200.0, draft.totalBill, 0.01)
        assertTrue(draft.confidenceWarnings.any { it.contains("মোট বিলের হিসাবে অমিল") })
    }

    // =========================================================================
    // SECTION 3: ADDRESS, DISTRICT, AND THANA ROBUSTNESS
    // =========================================================================

    @Test
    fun `address - complex address numbers do not leak as dress sizes`() {
        val input = """
            নাম: ফারহিন
            ফোন: 01755667788
            ঠিকানা: House 12, Road 4, Sector 7, Block B, Flat 3A, Ward 5, Uttara, Dhaka
            দাম: 1400
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("Dhaka", draft.district)
        // No dress size was specified in the message; address numbers (12, 4, 7, 3, 5) must not be picked up as size
        assertEquals("", draft.size)
        assertEquals(FieldConfidence.MISSING, draft.fieldConfidences["size"])
    }

    @Test
    fun `address - house number and dress size with identical numerals`() {
        val input = """
            নাম: রোকসানা
            ফোন: 01766778899
            ঠিকানা: বাসা ৩৮, রোড ৪, উত্তরা, ঢাকা
            ড্রেস: কুর্তি
            সাইজ: ৩৮
            দাম: ১২০০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertTrue(draft.customerAddress.contains("38") || draft.customerAddress.contains("৩৮"))
        assertEquals("38", draft.size)
        assertEquals(FieldConfidence.EXPLICIT, draft.fieldConfidences["size"])
    }

    @Test
    fun `district - no district in text flags inferred default and warning`() {
        val input = """
            নাম: পারভীন
            ফোন: 01777889900
            ঠিকানা: নয়াবাজার ৩ নম্বর গলি
            সাইজ: ৪০
            দাম: ১৩০০
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("Dhaka", draft.district)
        assertEquals(FieldConfidence.INFERRED, draft.fieldConfidences["district"])
        assertTrue(draft.confidenceWarnings.any { it.contains("জেলা স্পষ্ট নয়") })
    }

    @Test
    fun `thana - embedded thana in address is cleanly extracted`() {
        val input = """
            নাম: কানিজ ফাতেমা
            ফোন: 01788990011
            ঠিকানা: মিরপুর ১০, কাফরুল থানা, ঢাকা
            সাইজ: 40
            দাম: 1500
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertTrue(draft.thana.equals("কাফরুল") || draft.thana.equals("Kafrul", ignoreCase = true))
        assertEquals(FieldConfidence.EXPLICIT, draft.fieldConfidences["thana"])
    }

    // =========================================================================
    // SECTION 4: CONVERSATIONAL NOISE, EMOJIS, AND ADVANCED SCRIPTS
    // =========================================================================

    @Test
    fun `conversational - conversational noise with emojis and pleasantries`() {
        val input = """
            Apu kemon achen? Amar ei dress ta lagbe 🔥👗
            Name: Tasnim Rahman
            Phone: 01711223344
            Address: House 10, Road 2, Dhanmondi, Dhaka
            Dress: কটন সালোয়ার কামিজ
            Size: 38
            Price: 1400
            Delivery: 80
            Advance 200 disi bkash e
            R baki taka cod e dibo. Tnx apu! ❤️
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("Tasnim Rahman", draft.customerName)
        assertEquals("01711223344", draft.customerPhone)
        assertTrue(draft.customerAddress.contains("Dhanmondi"))
        assertEquals("Dhaka", draft.district)
        assertEquals("38", draft.size)
        assertEquals("কটন সালোয়ার কামিজ", draft.dressName)
        assertEquals(1400.0, draft.unitPrice, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01)
        assertEquals(200.0, draft.advanceAmount, 0.01)
        assertEquals(1480.0, draft.totalBill, 0.01)
        assertEquals(1280.0, draft.codDue, 0.01)
    }

    @Test
    fun `order type - pre-order detection in bangla and english`() {
        val input = """
            নাম: নাদিয়া
            ফোন: 01799001122
            ঠিকানা: বনশ্রী, ঢাকা
            সাইজ: ৩৬
            দাম: ১৫০০
            প্রি-অর্ডার কনফার্ম করবেন
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(OrderType.PRE_ORDER, draft.orderType)
    }

    @Test
    fun `notes - special instructions and exchange order detection`() {
        val input = """
            নাম: শায়লা
            ফোন: 01899001122
            ঠিকানা: মতিঝিল, ঢাকা
            সাইজ: 40
            দাম: 1300
            Exchange order আগের ড্রেসটা ফেরত নেওয়া লাগবে
            লং: 44
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertTrue(draft.specialInstructions.contains("Exchange order"))
        assertTrue(draft.specialInstructions.contains("44"))
    }
}
