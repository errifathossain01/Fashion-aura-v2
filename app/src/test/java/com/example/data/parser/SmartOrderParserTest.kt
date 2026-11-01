package com.example.data.parser

import com.example.data.model.DeliveryTier
import com.example.data.model.OrderType
import org.junit.Assert.*
import org.junit.Test

class SmartOrderParserTest {

    @Test
    fun `regression test - standard labeled format with bangla numerals and equation`() {
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
    fun `numeral substitution - bangla vs english digits parity`() {
        val banglaInput = """
            নাম: সুমাইয়া
            ফোন: ০১৭৯৮৭৬৫৪৩২
            ঠিকানা: ধানমন্ডি ঢাকা
            সাইজ: ৩৮
            দাম: ১৪০০ টাকা
            ডেলিভারি: ৮০ টাকা
        """.trimIndent()

        val englishInput = """
            নাম: সুমাইয়া
            ফোন: 01798765432
            ঠিকানা: ধানমন্ডি ঢাকা
            সাইজ: 38
            দাম: 1400 টাকা
            ডেলিভারি: 80 টাকা
        """.trimIndent()

        val draftBangla = SmartOrderParser.parse(banglaInput)
        val draftEnglish = SmartOrderParser.parse(englishInput)

        assertEquals("01798765432", draftBangla.customerPhone)
        assertEquals(draftEnglish.customerPhone, draftBangla.customerPhone)
        assertEquals("38", draftBangla.size)
        assertEquals(draftEnglish.size, draftBangla.size)
        assertEquals(1400.0, draftBangla.unitPrice, 0.01)
        assertEquals(draftEnglish.unitPrice, draftBangla.unitPrice, 0.01)
        assertEquals(80.0, draftBangla.deliveryCharge, 0.01)
        assertEquals(1480.0, draftBangla.totalBill, 0.01)
    }

    @Test
    fun `phone number formatting - hyphens, spaces, and international prefix`() {
        val input1 = "নাম: তানিয়া\nমোবাইল: +8801711-223344\nঠিকানা: মিরপুর, ঢাকা"
        val draft1 = SmartOrderParser.parse(input1)
        assertEquals("01711223344", draft1.customerPhone)

        val input2 = "নাম: তানিয়া\nফোন: 01812 345 678\nঠিকানা: মিরপুর, ঢাকা"
        val draft2 = SmartOrderParser.parse(input2)
        assertEquals("01812345678", draft2.customerPhone)

        val input3 = "নাম: তানিয়া\nফোন: 01911.22.33.44\nঠিকানা: মিরপুর, ঢাকা"
        val draft3 = SmartOrderParser.parse(input3)
        assertEquals("01911223344", draft3.customerPhone)
    }

    @Test
    fun `banglish and alternative label aliases`() {
        val input = """
            Nam: Sabrina Islam
            Phn: 01612345678
            Thikana: House 12, Road 5, Block B, Uttara, Dhaka
            Sz: 40
            Dam: 1600
            Adv: 200
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("Sabrina Islam", draft.customerName)
        assertEquals("01612345678", draft.customerPhone)
        assertTrue(draft.customerAddress.contains("Uttara"))
        assertEquals("Dhaka", draft.district)
        assertEquals("40", draft.size)
        assertEquals(1600.0, draft.unitPrice, 0.01)
        assertEquals(200.0, draft.advanceAmount, 0.01)
        assertEquals(80.0, draft.deliveryCharge, 0.01) // Inferred Dhaka inside
        assertEquals(1680.0, draft.totalBill, 0.01)
        assertEquals(1480.0, draft.codDue, 0.01)
    }

    @Test
    fun `body size vs length conflict resolution`() {
        val input = """
            নাম: ফারহানা
            ফোন: 01755112233
            ঠিকানা: চকরিয়া, Cox's Bazar
            বডি: 42
            লং: 46
            কালার: পিস্তা
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("42", draft.size)
        assertTrue(draft.specialInstructions.contains("46"))
        assertNotNull(draft.alternativeCandidates["size"])
        assertEquals("পিস্তা", draft.color)
        assertEquals("Cox's Bazar", draft.district)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
    }

    @Test
    fun `free size recognition`() {
        val input = """
            নাম: ফারজানা
            ফোন: 01711001122
            ঠিকানা: কুমিল্লা
            সাইজ: ফ্রি
            কালার: কালো
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)
        assertEquals("Free", draft.size)
    }

    @Test
    fun `equation with advance payment resolution`() {
        // 1400 dress + 150 delivery - 200 advance = 1350 due
        val input = """
            নাম: মরিয়ম
            ফোন: 01822334455
            ঠিকানা: সিলেট সদর
            ১৪০০+১৫০-২০০=১৩৫০ টাকা
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(1400.0, draft.unitPrice, 0.01)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(200.0, draft.advanceAmount, 0.01)
        assertEquals(1550.0, draft.totalBill, 0.01)
        assertEquals(1350.0, draft.codDue, 0.01)
    }

    @Test
    fun `multiple fields on single line`() {
        val input = "নাম: আফরোজা, ফোন: 01722334455, সাইজ: 38, ঠিকানা: সাভার, কালার: লাল"
        val draft = SmartOrderParser.parse(input)

        assertEquals("আফরোজা", draft.customerName)
        assertEquals("01722334455", draft.customerPhone)
        assertEquals("38", draft.size)
        assertTrue(draft.customerAddress.contains("সাভার"))
        assertEquals("লাল", draft.color)
        assertEquals(DeliveryTier.DHAKA_SUB, draft.deliveryTier)
        assertEquals(120.0, draft.deliveryCharge, 0.01)
    }

    @Test
    fun `multiple phones extracted with secondary as candidate`() {
        val input = """
            নাম: জেরিন
            ফোন: 01711223344, 01899887766
            ঠিকানা: বগুড়া
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("01711223344", draft.customerPhone)
        val candidates = draft.alternativeCandidates["phone"]
        assertNotNull(candidates)
        assertTrue(candidates!!.contains("01899887766"))
        assertTrue(draft.confidenceWarnings.any { it.contains("একাধিক ফোন") })
    }

    @Test
    fun `pre-order detection`() {
        val input = """
            নাম: নাদিয়া
            ফোন: 01733445566
            ঠিকানা: রাজশাহী
            প্রি-অর্ডার ড্রেস
            সাইজ: 40
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)
        assertEquals(OrderType.PRE_ORDER, draft.orderType)
    }

    @Test
    fun `exchange order detection`() {
        val input = """
            নাম: তাসনিম
            ফোন: 01744556677
            ঠিকানা: খুলনা
            Exchange order: সাইজ পরিবর্তন
            সাইজ: 44
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)
        assertTrue(draft.specialInstructions.contains("Exchange order"))
    }

    @Test
    fun `total bill mismatch records warning without crashing`() {
        val input = """
            নাম: রুমানা
            ফোন: 01788990011
            ঠিকানা: ঢাকা
            দাম: 1200
            ডেলিভারি: 80
            মোট বিল: 1500
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)
        assertEquals(1500.0, draft.totalBill, 0.01)
        assertTrue(draft.confidenceWarnings.any { it.contains("মোট বিলের হিসাবে অমিল") })
    }

    @Test
    fun `unknown or blank input does not fabricate fake phone or size`() {
        val input = "খুব সুন্দর ড্রেস পছন্দ হয়েছে ডেলিভারি কবে পাব?"
        val draft = SmartOrderParser.parse(input)

        assertEquals("", draft.customerPhone)
        assertEquals("", draft.size)
        assertTrue(draft.confidenceWarnings.any { it.contains("ফোন নম্বর পাওয়া যায়নি") })
    }

    @Test
    fun `normalization idempotence invariant`() {
        val text = "নাম: শায়লা\nটাকা: ১৪৫০\nফোন: ০১৮১১২২৩৩৪৪"
        val norm1 = SmartOrderParser.normalizeDigits(text)
        val norm2 = SmartOrderParser.normalizeDigits(norm1)
        assertEquals(norm1, norm2)
    }

    @Test
    fun `multi-product equation solver - 1300+1300+1300+150=4050 with 3 items`() {
        val input = """
            নাম: সাদিয়া
            ফোন: 01711223344
            ঠিকানা: মিরপুর ১০, ঢাকা
            সাইজ: 36, 38, 42
            বিল: 1300+1300+1300+150=4050
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("01711223344", draft.customerPhone)
        assertEquals(3, draft.items.size)
        assertEquals(1300.0, draft.items[0].unitPrice, 0.01)
        assertEquals(1300.0, draft.items[1].unitPrice, 0.01)
        assertEquals(1300.0, draft.items[2].unitPrice, 0.01)
        assertEquals("36", draft.items[0].size)
        assertEquals("38", draft.items[1].size)
        assertEquals("42", draft.items[2].size)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(3900.0, draft.subtotal, 0.01)
        assertEquals(4050.0, draft.totalBill, 0.01)
    }

    @Test
    fun `multi-product equation solver - 1400+1350+150=2900 with varying prices`() {
        val input = """
            নাম: রোকসানা
            মোবাইল: 01812345678
            ঠিকানা: চট্টগ্রাম
            হিসাব: 1400+1350+150=2900
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals(2, draft.items.size)
        assertEquals(1400.0, draft.items[0].unitPrice, 0.01)
        assertEquals(1350.0, draft.items[1].unitPrice, 0.01)
        assertEquals(150.0, draft.deliveryCharge, 0.01)
        assertEquals(2750.0, draft.subtotal, 0.01)
        assertEquals(2900.0, draft.totalBill, 0.01)
    }

    @Test
    fun `set and piece extraction - 36(3set)`() {
        val input = """
            নাম: জেসিকা
            ফোন: 01912345678
            ঠিকানা: ধানমন্ডি
            সাইজ: 36(3set)
            দাম: 3900
            ডেলিভারি: 80
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)

        assertEquals("36", draft.size)
        assertEquals(3, draft.items.size)
        draft.items.forEach {
            assertEquals("36", it.size)
        }
    }

    @Test
    fun `reverse size detection in bangla and english`() {
        val inputBangla = "নাম: নুশরাত\nফোন: 01700112233\nঠিকানা: সিলেট\n৪২ সাইজ ড্রেস দাম ১৩০০ ডেলিভারি ১৫০"
        val draftBangla = SmartOrderParser.parse(inputBangla)
        assertEquals("42", draftBangla.size)

        val inputEnglish = "নাম: নুশরাত\nফোন: 01700112233\nঠিকানা: সিলেট\n42 size dress price 1300 delivery 150"
        val draftEnglish = SmartOrderParser.parse(inputEnglish)
        assertEquals("42", draftEnglish.size)
    }

    @Test
    fun `thana upazila extraction - standalone line and embedded in address`() {
        val input1 = """
            Name: Nila
            Phone: 01712345678
            ঠিকানা: গ্রাম: ভাটরা, থানা: শাহরাস্তি, চাঁদপুর
            সাইজ: 40
            দাম: 1500
        """.trimIndent()

        val draft1 = SmartOrderParser.parse(input1)
        assertEquals("Nila", draft1.customerName)
        assertEquals("শাহরাস্তি", draft1.thana)
        assertEquals("শাহরাস্তি", draft1.thanaUpazila)

        val input2 = """
            নাম: নিলা
            ফোন: 01812345678
            উপজেলা: শাহরাস্তি
            ঠিকানা: ভাটরা
            জেলা: চাঁদপুর
        """.trimIndent()

        val draft2 = SmartOrderParser.parse(input2)
        assertEquals("নিলা", draft2.customerName)
        assertEquals("শাহরাস্তি", draft2.thana)
        assertEquals("শাহরাস্তি", draft2.thanaUpazila)
    }

    @Test
    fun `prefix stripping - Name and Address labels are cleanly stripped`() {
        val input = """
            Name: Nila Rahman
            Phone: 01712345678
            Address: Baneshwar, Puthia, Rajshahi
            ড্রেস: কটন সালোয়ার কামিজ
        """.trimIndent()

        val draft = SmartOrderParser.parse(input)
        assertEquals("Nila Rahman", draft.customerName)
        assertFalse(draft.customerName.startsWith("Name:", ignoreCase = true))
        assertFalse(draft.customerAddress.startsWith("Address:", ignoreCase = true))
        assertTrue(draft.customerAddress.contains("Baneshwar"))

        val inputBanglaWithEmoji = """
            👤 নাম: নিলা
            📱 ফোন: 01812345678
            🛍️ এড্রেস: বনেশ্বর, পুঠিয়া, রাজশাহী
        """.trimIndent()

        val draftBn = SmartOrderParser.parse(inputBanglaWithEmoji)
        assertEquals("নিলা", draftBn.customerName)
        assertFalse(draftBn.customerName.startsWith("নাম:"))
        assertFalse(draftBn.customerAddress.startsWith("এড্রেস:"))
        assertFalse(draftBn.customerAddress.startsWith("ঠিকানা:"))
        assertTrue(draftBn.customerAddress.contains("বনেশ্বর"))
    }
}
