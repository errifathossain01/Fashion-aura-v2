package com.example.data.parser

import com.example.data.model.DeliveryTier
import com.example.data.model.OrderType

/**
 * Deterministic, extensible, offline-first parsing engine pipeline.
 * Supports multi-product and multi-size extraction, generalized equation parsing,
 * set/piece quantity resolution, and reverse-format size detection.
 */
object ParsingPipeline {

    fun execute(rawText: String): ParsedOrderDraft {
        if (rawText.isBlank()) return ParsedOrderDraft()

        val warnings = mutableListOf<String>()
        val candidates = mutableMapOf<String, MutableList<String>>()
        val confidences = mutableMapOf<String, FieldConfidence>()

        // -------------------------------------------------------------
        // STAGE 1: Unicode, Numeral Normalization & Cleaning
        // -------------------------------------------------------------
        val normalized = normalizeDigits(rawText)
        val cleanedLines = normalized.lines()
            .map { cleanNoiseAndBullets(it) }
            .filter { it.isNotBlank() }

        // -------------------------------------------------------------
        // STAGE 2: Header, Greeting & Irrelevant Metadata Filtering
        // -------------------------------------------------------------
        val contentLines = cleanedLines.filterNot { isIrrelevantGreetingOrFooter(it) }

        // -------------------------------------------------------------
        // STAGE 3: Phone-number Candidate Extraction
        // -------------------------------------------------------------
        val phoneMatches = ParserDictionaries.PHONE_REGEX.findAll(normalized).toList()
        var primaryPhone = ""
        val extraPhones = mutableListOf<String>()

        for (match in phoneMatches) {
            val rawDigits = match.groupValues[1].replace(Regex("""[\s\-\.–—]"""), "")
            if (rawDigits.length == 10 && rawDigits.startsWith("1")) {
                val formatted = "0$rawDigits"
                if (primaryPhone.isEmpty()) {
                    primaryPhone = formatted
                } else if (formatted != primaryPhone && !extraPhones.contains(formatted)) {
                    extraPhones.add(formatted)
                }
            }
        }

        if (primaryPhone.isNotEmpty()) {
            confidences["customerPhone"] = FieldConfidence.EXPLICIT
            if (extraPhones.isNotEmpty()) {
                candidates["phone"] = extraPhones
                warnings.add("একাধিক ফোন নম্বর পাওয়া গেছে (প্রধান: $primaryPhone, অতিরিক্ত: ${extraPhones.joinToString()})")
            }
        } else {
            confidences["customerPhone"] = FieldConfidence.MISSING
            warnings.add("গ্রাহকের ১১ ডিজিট ফোন নম্বর পাওয়া যায়নি")
        }

        // -------------------------------------------------------------
        // STAGE 4: Explicit Field-Label Extraction
        // -------------------------------------------------------------
        var explicitName = ""
        var explicitAddress = ""
        var explicitThana = ""
        var explicitDress = ""
        var explicitBodySize = ""
        var explicitLength = ""
        var explicitColor = ""
        var explicitQty: Int? = null
        var explicitAdvance: Double? = null
        var explicitDelivery: Double? = null
        var explicitPrice: Double? = null
        val allExplicitPrices = mutableListOf<Double>()
        var explicitTotal: Double? = null
        var explicitDue: Double? = null
        var explicitNote = ""

        for (line in contentLines) {
            if (explicitName.isEmpty()) {
                ParserDictionaries.NAME_LABEL_REGEX.find(line)?.let {
                    explicitName = sanitizeFieldValue(it.groupValues[1])
                }
            }
            if (explicitAddress.isEmpty()) {
                ParserDictionaries.ADDRESS_LABEL_REGEX.find(line)?.let {
                    explicitAddress = sanitizeFieldValue(it.groupValues[1])
                }
            }
            if (explicitThana.isEmpty()) {
                ParserDictionaries.THANA_LABEL_REGEX.find(line)?.let {
                    explicitThana = sanitizeFieldValue(it.groupValues[1])
                }
            }
            if (explicitDress.isEmpty()) {
                ParserDictionaries.DRESS_LABEL_REGEX.find(line)?.let {
                    explicitDress = sanitizeFieldValue(it.groupValues[1])
                }
            }
            if (explicitBodySize.isEmpty()) {
                ParserDictionaries.BODY_SIZE_LABEL_REGEX.find(line)?.let {
                    explicitBodySize = it.groupValues[1].trim()
                }
            }
            if (explicitLength.isEmpty()) {
                ParserDictionaries.LENGTH_LABEL_REGEX.find(line)?.let {
                    explicitLength = it.groupValues[1].trim()
                }
            }
            if (explicitColor.isEmpty()) {
                ParserDictionaries.COLOR_LABEL_REGEX.find(line)?.let {
                    explicitColor = sanitizeFieldValue(it.groupValues[1])
                }
            }
            if (explicitQty == null) {
                ParserDictionaries.QUANTITY_LABEL_REGEX.find(line)?.let {
                    explicitQty = it.groupValues[1].toIntOrNull()
                }
            }
            if (explicitAdvance == null) {
                ParserDictionaries.ADVANCE_LABEL_REGEX.find(line)?.let {
                    explicitAdvance = it.groupValues[1].toDoubleOrNull()
                }
                if (explicitAdvance == null) {
                    ParserDictionaries.DELIVERY_ADVANCE_REGEX.find(line)?.let {
                        val rawNum = it.groupValues[1].ifEmpty { it.groupValues[2] }
                        val adv = rawNum.toDoubleOrNull()
                        explicitAdvance = adv
                        if (explicitDelivery == null) {
                            explicitDelivery = adv
                        }
                    }
                }
            }
            if (explicitDelivery == null) {
                ParserDictionaries.DELIVERY_LABEL_REGEX.find(line)?.let {
                    explicitDelivery = it.groupValues[1].toDoubleOrNull()
                }
                if (explicitDelivery == null) {
                    ParserDictionaries.DELIVERY_ADVANCE_REGEX.find(line)?.let {
                        val rawNum = it.groupValues[1].ifEmpty { it.groupValues[2] }
                        explicitDelivery = rawNum.toDoubleOrNull()
                    }
                }
            }
            if (!line.contains("=")) {
                ParserDictionaries.PRICE_LABEL_REGEX.find(line)?.let {
                    val p = it.groupValues[1].toDoubleOrNull()
                    if (p != null && !allExplicitPrices.contains(p)) {
                        allExplicitPrices.add(p)
                    }
                    if (explicitPrice == null) {
                        explicitPrice = p
                    }
                }
            }
            if (explicitTotal == null) {
                if (!line.contains("=") && !line.contains("+")) {
                    ParserDictionaries.TOTAL_BILL_LABEL_REGEX.find(line)?.let {
                        explicitTotal = it.groupValues[1].toDoubleOrNull()
                    }
                }
            }
            if (explicitDue == null) {
                if (!line.contains("=") && !line.contains("+")) {
                    ParserDictionaries.DUE_LABEL_REGEX.find(line)?.let {
                        explicitDue = it.groupValues[1].toDoubleOrNull()
                    }
                }
            }
            if (explicitNote.isEmpty()) {
                ParserDictionaries.NOTE_LABEL_REGEX.find(line)?.let {
                    explicitNote = sanitizeFieldValue(it.groupValues[1])
                }
            }
        }

        // -------------------------------------------------------------
        // STAGE 5: Multi-Size & Set/Quantity Resolution
        // -------------------------------------------------------------
        // 1. Set or piece detection: e.g. "36(3set)", "38(2pcs)", "36 (3 set)"
        val setSizeMatch = Regex("""(?i)\b(3[4-9]|4[0-8]|50|Free|ফ্রি|XS|S|M|L|XL|XXL|2XL|3XL|4XL)\s*\(\s*(\d+)\s*(?:set|sets|pcs|pieces|পিস|টি|জোড়া)?\s*\)""").find(normalized)
        var setExtractedSize = ""
        var setExtractedQty: Int? = null
        if (setSizeMatch != null) {
            setExtractedSize = if (setSizeMatch.groupValues[1].equals("ফ্রি", ignoreCase = true)) "Free" else setSizeMatch.groupValues[1]
            setExtractedQty = setSizeMatch.groupValues[2].toIntOrNull()
        }

        // 2. Token quantity: e.g. "3set", "3 set", "3 pcs", "৩ পিস", "3টি"
        val qtyTokenMatch = Regex("""(?i)\b(\d+)\s*(?:set|sets|pcs|pieces|পিস|টি|জোড়া)(?:(?=[^\w\u0980-\u09FF])|$)""").find(normalized)
        val tokenQty = qtyTokenMatch?.groupValues?.get(1)?.toIntOrNull()

        // 3. Reverse format sizes: e.g. "৪২ সাইজ", "42 size", "38 size", "Free size", "৩৬ সাইজ"
        val reverseSizeMatches = Regex("""(?i)\b(3[4-9]|4[0-8]|50|Free|ফ্রি|XS|S|M|L|XL|XXL|2XL|3XL|4XL)\s*(?:সাইজ|size|sz)\b""").findAll(normalized).map {
            val s = it.groupValues[1]
            if (s.equals("ফ্রি", ignoreCase = true)) "Free" else s
        }.toList()

        // 4. Multiple comma/slash/space separated sizes after label: e.g. "সাইজ: ৩৬, ৩৮, ৪২" or "size: 36, 38, 42"
        val labeledMultiSizeMatches = mutableListOf<String>()
        val multiSizeSection = Regex("""(?i)(?:সাইজ|size|sz|বডি)\s*[:=\-–—~]?\s*([^\n\r;|]+)""").find(normalized)
        if (multiSizeSection != null) {
            val rawSection = multiSizeSection.groupValues[1]
            val tokens = Regex("""(?i)\b(3[4-9]|4[0-8]|50|Free|ফ্রি|XS|S|M|L|XL|XXL|2XL|3XL|4XL)\b""").findAll(rawSection).map {
                val s = it.groupValues[1]
                if (s.equals("ফ্রি", ignoreCase = true)) "Free" else s
            }.toList()
            if (tokens.isNotEmpty()) {
                labeledMultiSizeMatches.addAll(tokens)
            }
        }

        // 5. Aggregate all detected sizes preserving appearance order
        val allExtractedSizes = mutableListOf<String>()
        if (setExtractedSize.isNotBlank() && !allExtractedSizes.contains(setExtractedSize)) {
            allExtractedSizes.add(setExtractedSize)
        }
        for (sz in labeledMultiSizeMatches) {
            if (!allExtractedSizes.contains(sz)) allExtractedSizes.add(sz)
        }
        for (sz in reverseSizeMatches) {
            if (!allExtractedSizes.contains(sz)) allExtractedSizes.add(sz)
        }
        if (explicitBodySize.isNotBlank()) {
            val s = if (explicitBodySize.equals("ফ্রি", ignoreCase = true)) "Free" else explicitBodySize
            if (!allExtractedSizes.contains(s)) allExtractedSizes.add(s)
        }

        // Fallback search for standalone body sizes (avoiding phone and address numbers)
        val standaloneSizeMatches = Regex("""(?i)(?:^|[\s,•*|])(34|36|38|40|42|44|46|48|50|Free|ফ্রি|XS|S|M|L|XL|XXL|2XL|3XL|4XL)(?:(?=[^\w])|$)""").findAll(normalized)
        for (m in standaloneSizeMatches) {
            val cand = if (m.groupValues[1].equals("ফ্রি", ignoreCase = true)) "Free" else m.groupValues[1]
            if (!primaryPhone.contains(cand) && cand != "50" && !allExtractedSizes.contains(cand)) {
                val idx = m.range.first
                val prefix = normalized.substring((idx - 10).coerceAtLeast(0), idx).lowercase()
                val postfixEnd = (m.range.last + 12).coerceAtMost(normalized.length)
                val postfix = normalized.substring(m.range.last + 1, postfixEnd).lowercase()
                val isAddressNumber = prefix.contains("road") || prefix.contains("house") || prefix.contains("রোড") ||
                        prefix.contains("বাড়ি") || prefix.contains("বাসা") || prefix.contains("sector") || prefix.contains("সেক্টর") ||
                        prefix.contains("holding") || prefix.contains("হোল্ডিং") || prefix.contains("নং") || prefix.contains("no") ||
                        prefix.contains("#") || prefix.contains("lane") || prefix.contains("লেন") || prefix.contains("flat") ||
                        prefix.contains("ফ্ল্যাট") || prefix.contains("ব্লক") || prefix.contains("block") ||
                        prefix.contains("h-") || prefix.contains("h#") || prefix.contains("r-") || prefix.contains("rd-") ||
                        prefix.contains("ward") || prefix.contains("ওয়ার্ড") || prefix.contains("গলি") ||
                        postfix.contains("road") || postfix.contains("রোড") || postfix.contains("নং") ||
                        postfix.contains("no") || postfix.contains("নম্বর") || postfix.contains("lane") || postfix.contains("লেন")
                if (!isAddressNumber) {
                    allExtractedSizes.add(cand)
                }
            }
        }

        var finalSize = allExtractedSizes.firstOrNull() ?: ""
        if (finalSize.isNotBlank()) {
            confidences["size"] = if (explicitBodySize.isNotBlank() || setExtractedSize.isNotBlank() || reverseSizeMatches.isNotEmpty()) {
                FieldConfidence.EXPLICIT
            } else {
                FieldConfidence.INFERRED
            }
        } else {
            confidences["size"] = FieldConfidence.MISSING
        }

        val finalQuantity = setExtractedQty ?: tokenQty ?: explicitQty ?: 1

        // -------------------------------------------------------------
        // STAGE 6: Name & Address Separation & District Identification
        // -------------------------------------------------------------
        var detectedDistrict = "Dhaka"
        var districtConfidence = FieldConfidence.INFERRED

        for ((engDist, bngDist) in ParserDictionaries.BANGLADESH_DISTRICTS) {
            val engRegex = Regex("""(?i)\b$engDist\b""")
            if (rawText.contains(bngDist) || normalized.contains(bngDist) ||
                engRegex.containsMatchIn(rawText) || engRegex.containsMatchIn(normalized)) {
                detectedDistrict = engDist
                districtConfidence = FieldConfidence.EXPLICIT
                break
            }
        }

        var finalAddress = cleanPrefixLabels(explicitAddress)
        if (finalAddress.isNotBlank()) {
            confidences["customerAddress"] = FieldConfidence.EXPLICIT
        } else {
            val addressCandidates = contentLines.filter { line ->
                isLikelyAddressLine(line) && !ParserDictionaries.PHONE_REGEX.containsMatchIn(line)
            }
            if (addressCandidates.isNotEmpty()) {
                finalAddress = addressCandidates.joinToString(", ") { cleanPrefixLabels(it) }
                confidences["customerAddress"] = FieldConfidence.INFERRED
            } else {
                val secondaryCandidate = contentLines.find { line ->
                    line != explicitName &&
                    !ParserDictionaries.PHONE_REGEX.containsMatchIn(line) &&
                    line.length >= 8 &&
                    !isPriceOrEquationLine(line) &&
                    !isSizeOrColorOrMetaLine(line)
                }
                if (secondaryCandidate != null) {
                    finalAddress = cleanPrefixLabels(secondaryCandidate)
                    confidences["customerAddress"] = FieldConfidence.INFERRED
                } else if (districtConfidence == FieldConfidence.EXPLICIT) {
                    finalAddress = detectedDistrict
                    confidences["customerAddress"] = FieldConfidence.INFERRED
                } else {
                    finalAddress = "ঢাকা"
                    confidences["customerAddress"] = FieldConfidence.MISSING
                    warnings.add("গ্রাহকের ঠিকানা খুঁজে পাওয়া যায়নি")
                }
            }
        }
        finalAddress = cleanPrefixLabels(finalAddress)

        // Thana / Upazila Extraction & Fallback
        var finalThana = cleanPrefixLabels(explicitThana)
        if (finalThana.isEmpty()) {
            ParserDictionaries.THANA_LABEL_REGEX.find(normalized)?.let {
                finalThana = cleanPrefixLabels(sanitizeFieldValue(it.groupValues[1]))
            }
        }
        if (finalThana.isEmpty() && finalAddress.isNotBlank()) {
            ParserDictionaries.THANA_LABEL_REGEX.find(finalAddress)?.let {
                finalThana = cleanPrefixLabels(sanitizeFieldValue(it.groupValues[1]))
            }
        }
        if (finalThana.isEmpty()) {
            ParserDictionaries.EMBEDDED_THANA_POSTFIX_REGEX.find(normalized)?.let {
                finalThana = cleanPrefixLabels(it.groupValues[1])
            }
        }
        if (finalThana.isEmpty()) {
            ParserDictionaries.EMBEDDED_THANA_PREFIX_REGEX.find(normalized)?.let {
                finalThana = cleanPrefixLabels(it.groupValues[1])
            }
        }
        if (finalThana.isEmpty()) {
            for (upazila in ParserDictionaries.KNOWN_UPAZILAS_THANAS) {
                val upazilaRegex = Regex("""(?i)\b$upazila\b""")
                if (upazilaRegex.containsMatchIn(finalAddress) || upazilaRegex.containsMatchIn(normalized)) {
                    finalThana = upazila
                    break
                }
            }
        }
        if (finalThana.isNotBlank()) {
            confidences["thana"] = FieldConfidence.EXPLICIT
        }

        if (districtConfidence == FieldConfidence.EXPLICIT) {
            confidences["district"] = FieldConfidence.EXPLICIT
        } else {
            confidences["district"] = FieldConfidence.INFERRED
            warnings.add("জেলা স্পষ্ট নয় ('ঢাকা' ডিফল্ট ধরা হয়েছে)")
        }

        var finalName = cleanPrefixLabels(explicitName)
        if (finalName.isNotBlank()) {
            finalName = cleanPrefixLabels(finalName)
            confidences["customerName"] = FieldConfidence.EXPLICIT
        } else {
            val nameCandidate = contentLines.find { line ->
                val cleaned = cleanPrefixLabels(line)
                !ParserDictionaries.PHONE_REGEX.containsMatchIn(line) &&
                !isLikelyAddressLine(line) &&
                !isPriceOrEquationLine(line) &&
                !isDressKeyword(cleaned) &&
                !isSizeOrColorOrMetaLine(line) &&
                cleaned.length in 2..40
            }
            if (nameCandidate != null) {
                finalName = cleanPrefixLabels(nameCandidate)
                confidences["customerName"] = FieldConfidence.INFERRED
            } else {
                finalName = "সম্মানিত গ্রাহক"
                confidences["customerName"] = FieldConfidence.MISSING
                warnings.add("গ্রাহকের নাম স্পষ্ট নয় ('সম্মানিত গ্রাহক' ড্রাফট করা হয়েছে)")
            }
        }
        finalName = cleanPrefixLabels(finalName)

        // -------------------------------------------------------------
        // STAGE 7: Product, Financial Model & Multi-Item Equation Solver
        // -------------------------------------------------------------
        var unitPrice = explicitPrice ?: 1500.0
        var isPriceExplicit = explicitPrice != null
        var deliveryCharge = explicitDelivery ?: 80.0
        var isDeliveryExplicit = explicitDelivery != null
        var advanceAmount = explicitAdvance ?: 0.0
        var totalBill = explicitTotal ?: (unitPrice * finalQuantity + deliveryCharge)
        val extractedItemPrices = mutableListOf<Double>()

        // Multi-Product Line Item Parsing (e.g. "১. কটন কুর্তি - সাইজ ৩৮ - দাম ১২০০")
        val itemLineRegex = Regex("""(?i)^\s*(?:[1-9১-৯][\.\)\-\:\s]|(?:product|item|ড্রেস|আইটেম)\s*[1-9১-৯]?\s*[:=\-–—~])\s*(.+)""")
        val numberedItemCandidates = contentLines.mapNotNull { line ->
            val match = itemLineRegex.find(line)
            if (match != null) match.groupValues[1].trim() else null
        }

        val multiLineItems = mutableListOf<ParsedOrderItem>()
        if (numberedItemCandidates.size >= 2) {
            for (itemText in numberedItemCandidates) {
                var itemName = "ফ্যাশন ড্রেস"
                for (dress in ParserDictionaries.DRESS_KEYWORDS) {
                    if (itemText.contains(dress, ignoreCase = true)) {
                        itemName = dress
                        break
                    }
                }
                if (itemName == "ফ্যাশন ড্রেস") {
                    val firstSegment = itemText.split("-", ",", ":").firstOrNull()?.trim()
                    if (!firstSegment.isNullOrBlank() && firstSegment.length in 3..30) {
                        itemName = cleanPrefixLabels(firstSegment)
                    }
                }

                var itemSize = ""
                ParserDictionaries.BODY_SIZE_LABEL_REGEX.find(itemText)?.let {
                    itemSize = it.groupValues[1].trim()
                }
                if (itemSize.isEmpty()) {
                    val standalone = Regex("""(?i)\b(3[4-9]|4[0-8]|50|Free|ফ্রি|XS|S|M|L|XL|XXL|2XL|3XL|4XL)\b""").find(itemText)
                    if (standalone != null) itemSize = standalone.groupValues[1]
                }
                if (itemSize.equals("ফ্রি", ignoreCase = true)) itemSize = "Free"

                var itemColor = ""
                for (c in ParserDictionaries.COLORS) {
                    if (itemText.contains(c, ignoreCase = true)) {
                        itemColor = c
                        break
                    }
                }

                var itemPrice = 1500.0
                var itemPriceExplicit = false
                val priceMatch = ParserDictionaries.PRICE_LABEL_REGEX.find(itemText)
                if (priceMatch != null) {
                    itemPrice = priceMatch.groupValues[1].toDoubleOrNull() ?: 1500.0
                    itemPriceExplicit = true
                } else {
                    val priceNum = Regex("""\b(5[0-9]{2}|[1-9][0-9]{3,4})\b""").find(itemText)
                    if (priceNum != null) {
                        itemPrice = priceNum.groupValues[1].toDoubleOrNull() ?: 1500.0
                        itemPriceExplicit = true
                    }
                }
                if (!itemPriceExplicit) {
                    warnings.add("আইটেম '$itemName' এর মূল্য মেসেজে উল্লেখ নেই (ডিফল্ট ৳${itemPrice.toInt()} ধরা হয়েছে)")
                }

                var itemQty = 1
                val itemQtyMatch = Regex("""(?i)\b([1-9][0-9]?)\s*(?:set|sets|pcs|pieces|পিস|টি|জোড়া)(?:(?=[^\w\u0980-\u09FF])|$)""").find(itemText)
                if (itemQtyMatch != null) {
                    itemQty = itemQtyMatch.groupValues[1].toIntOrNull() ?: 1
                } else {
                    ParserDictionaries.QUANTITY_LABEL_REGEX.find(itemText)?.let {
                        itemQty = it.groupValues[1].toIntOrNull() ?: 1
                    }
                }

                multiLineItems.add(
                    ParsedOrderItem(
                        dressName = itemName,
                        size = itemSize,
                        color = itemColor,
                        quantity = itemQty,
                        unitPrice = itemPrice
                    )
                )
            }
        }

        if (multiLineItems.size >= 2) {
            extractedItemPrices.clear()
            extractedItemPrices.addAll(multiLineItems.map { it.unitPrice })
            unitPrice = multiLineItems.first().unitPrice
            isPriceExplicit = multiLineItems.all { it.unitPrice != 1500.0 } || contentLines.any { it.contains("দাম") || it.contains("price") }
        }

        // Generalized Equation Solver: matches e.g. "1300+1300+1300+150=4050" or "1400+1350+150=2900" or "1400+150-200=1350"
        val generalizedEquationRegex = Regex("""((?:\d+\s*\+\s*)+\d+)(?:\s*-\s*(\d+))?\s*=\s*(\d+)""")
        val genMatch = generalizedEquationRegex.find(normalized)

        if (genMatch != null) {
            val sumPart = genMatch.groupValues[1]
            val subPart = genMatch.groupValues[2].toDoubleOrNull()
            val totalOrDue = genMatch.groupValues[3].toDoubleOrNull()

            val parts = sumPart.split("+").mapNotNull { it.trim().toDoubleOrNull() }
            if (parts.size >= 2) {
                val lastNum = parts.last()
                val isTypicalDelivery = lastNum in listOf(60.0, 70.0, 80.0, 90.0, 100.0, 120.0, 130.0, 140.0, 150.0, 160.0, 180.0, 200.0) ||
                        (lastNum <= 250.0 && lastNum < parts.first() * 0.5)

                if (isTypicalDelivery) {
                    deliveryCharge = lastNum
                    isDeliveryExplicit = true
                    extractedItemPrices.addAll(parts.dropLast(1))
                } else if (parts.all { it >= 300.0 }) {
                    // e.g. 1300+1300+1300=3900 (all are dress prices, delivery not in sum)
                    extractedItemPrices.addAll(parts)
                } else {
                    deliveryCharge = lastNum
                    isDeliveryExplicit = true
                    extractedItemPrices.addAll(parts.dropLast(1))
                }

                if (extractedItemPrices.isNotEmpty()) {
                    unitPrice = extractedItemPrices.first()
                    isPriceExplicit = true
                }

                if (subPart != null && subPart > 0) {
                    advanceAmount = subPart
                }

                val calculatedTotal = extractedItemPrices.sum() + deliveryCharge
                totalBill = if (subPart != null && subPart > 0) {
                    calculatedTotal
                } else {
                    totalOrDue ?: calculatedTotal
                }
            } else if (parts.size == 1) {
                unitPrice = parts[0]
                isPriceExplicit = true
                if (subPart != null && subPart > 0) advanceAmount = subPart
            }
        } else {
            // Check legacy 2-term equation if generalized didn't catch
            val equationMatch = ParserDictionaries.EQUATION_FULL_REGEX.find(normalized)
            if (equationMatch != null) {
                val eqPrice = equationMatch.groupValues[1].toDoubleOrNull()
                val eqDelivery = equationMatch.groupValues[2].toDoubleOrNull()
                val eqAdvance = equationMatch.groupValues[3].toDoubleOrNull()
                val eqTotalOrDue = equationMatch.groupValues[4].toDoubleOrNull()

                if (eqPrice != null && eqDelivery != null) {
                    unitPrice = eqPrice
                    deliveryCharge = eqDelivery
                    isPriceExplicit = true
                    isDeliveryExplicit = true
                    extractedItemPrices.add(eqPrice)

                    if (eqAdvance != null && eqAdvance > 0) {
                        advanceAmount = eqAdvance
                        totalBill = eqPrice + eqDelivery
                    } else {
                        totalBill = eqTotalOrDue ?: (eqPrice + eqDelivery)
                    }
                }
            } else {
                if (!isDeliveryExplicit) {
                    deliveryCharge = when {
                        detectedDistrict.equals("Dhaka", ignoreCase = true) -> {
                            val isSuburban = ParserDictionaries.DHAKA_SUBURBAN_KEYWORDS.any {
                                finalAddress.contains(it, ignoreCase = true) || rawText.contains(it, ignoreCase = true)
                            }
                            if (isSuburban) 120.0 else 80.0
                        }
                        else -> 150.0
                    }
                }

                val calculatedSubtotal = if (multiLineItems.size >= 2) {
                    multiLineItems.sumOf { it.unitPrice * it.quantity }
                } else {
                    unitPrice * finalQuantity
                }
                val expectedTotal = calculatedSubtotal + deliveryCharge

                if (explicitDue != null && explicitTotal == null) {
                    val totalFromDueAndAdvance = advanceAmount + explicitDue!!
                    if (Math.abs(totalFromDueAndAdvance - expectedTotal) <= 1.0) {
                        totalBill = totalFromDueAndAdvance
                    } else {
                        totalBill = expectedTotal
                        if (isPriceExplicit) {
                            warnings.add("মোট বিলের হিসাবে অমিল: ড্রেস (৳${calculatedSubtotal.toInt()}) + ডেলিভারি (৳${deliveryCharge.toInt()}) = ৳${expectedTotal.toInt()}, কিন্তু অগ্রিম + বাকি = ৳${totalFromDueAndAdvance.toInt()}")
                        }
                    }
                } else if (explicitTotal != null) {
                    totalBill = explicitTotal!!
                    if (Math.abs(expectedTotal - totalBill) > 1.0) {
                        warnings.add("মোট বিলের হিসাবে অমিল: ড্রেস (৳${calculatedSubtotal.toInt()}) + ডেলিভারি (৳${deliveryCharge.toInt()}) = ৳${expectedTotal.toInt()}, কিন্তু মেসেজে মোট ৳${totalBill.toInt()} উল্লেখ আছে")
                    }
                } else {
                    totalBill = expectedTotal
                }
            }
        }

        val deliveryTier = when (deliveryCharge.toInt()) {
            80 -> DeliveryTier.DHAKA_INSIDE
            120 -> DeliveryTier.DHAKA_SUB
            150 -> DeliveryTier.OUTSIDE_DHAKA
            else -> if (deliveryCharge >= 140) DeliveryTier.OUTSIDE_DHAKA else DeliveryTier.DHAKA_INSIDE
        }

        // -------------------------------------------------------------
        // STAGE 8: Order-type Detection
        // -------------------------------------------------------------
        val orderType = if (rawText.contains("pre-order", ignoreCase = true) ||
            rawText.contains("preorder", ignoreCase = true) ||
            rawText.contains("অগ্রিম অর্ডার") ||
            rawText.contains("প্রি-অর্ডার") ||
            rawText.contains("প্রি অর্ডার")
        ) {
            OrderType.PRE_ORDER
        } else {
            OrderType.REGULAR
        }

        // -------------------------------------------------------------
        // STAGE 9: Color & Dress Name & Special Note Extraction
        // -------------------------------------------------------------
        var finalDress = explicitDress
        if (finalDress.isBlank()) {
            for (dress in ParserDictionaries.DRESS_KEYWORDS) {
                if (rawText.contains(dress, ignoreCase = true) || normalized.contains(dress, ignoreCase = true)) {
                    finalDress = dress
                    break
                }
            }
            if (finalDress.isBlank()) {
                finalDress = "ফ্যাশন ড্রেস"
            }
        }

        // Multi-color extraction
        val allExtractedColors = mutableListOf<String>()
        if (explicitColor.isNotBlank()) {
            allExtractedColors.add(explicitColor)
        }
        for (c in ParserDictionaries.COLORS) {
            if (rawText.contains(c, ignoreCase = true) && !allExtractedColors.contains(c)) {
                allExtractedColors.add(c)
            }
        }
        val finalColor = allExtractedColors.firstOrNull() ?: ""

        // Special instructions / notes
        val notesList = mutableListOf<String>()
        if (explicitNote.isNotBlank()) {
            notesList.add(explicitNote)
        }
        if (rawText.contains("Exchange order", ignoreCase = true) || rawText.contains("এক্সচেঞ্জ", ignoreCase = true)) {
            notesList.add("Exchange order: এক্সচেঞ্জ পার্সেল")
        }
        if (explicitLength.isNotBlank()) {
            notesList.add("দৈর্ঘ্য/লং: $explicitLength")
        }
        val finalSpecialInstructions = notesList.joinToString(" | ")

        // -------------------------------------------------------------
        // STAGE 9.5: Multi-Item Construction
        // -------------------------------------------------------------
        val numItems = when {
            multiLineItems.size >= 2 -> multiLineItems.size
            extractedItemPrices.size > 1 -> extractedItemPrices.size
            finalQuantity > 1 -> finalQuantity
            allExtractedSizes.size > 1 -> allExtractedSizes.size
            else -> 1
        }

        val parsedItems = mutableListOf<ParsedOrderItem>()
        if (multiLineItems.size >= 2) {
            parsedItems.addAll(multiLineItems)
            finalDress = multiLineItems.first().dressName
            finalSize = multiLineItems.first().size
            unitPrice = multiLineItems.first().unitPrice
        } else {
            for (i in 0 until numItems) {
                val itemPrice = when {
                    extractedItemPrices.size > i -> extractedItemPrices[i]
                    extractedItemPrices.isNotEmpty() -> extractedItemPrices.last()
                    else -> unitPrice
                }
                val itemSize = when {
                    allExtractedSizes.size > i -> allExtractedSizes[i]
                    allExtractedSizes.isNotEmpty() -> allExtractedSizes.first()
                    else -> finalSize
                }
                val itemColor = when {
                    allExtractedColors.size > i -> allExtractedColors[i]
                    allExtractedColors.isNotEmpty() -> allExtractedColors.first()
                    else -> finalColor
                }
                val itemQty = if (numItems == 1) finalQuantity else 1

                parsedItems.add(
                    ParsedOrderItem(
                        dressName = finalDress,
                        size = itemSize,
                        color = itemColor,
                        quantity = itemQty,
                        unitPrice = itemPrice
                    )
                )
            }
        }

        // Alternative candidates for size (e.g. length, other extracted sizes)
        val altSizes = mutableListOf<String>()
        if (explicitLength.isNotBlank()) altSizes.add(explicitLength)
        if (allExtractedSizes.size > 1) altSizes.addAll(allExtractedSizes.drop(1))
        if (altSizes.isNotEmpty()) {
            candidates["size"] = altSizes
        }

        // Keep unitPrice and finalSize synced to primary item
        val effectiveSubtotal = parsedItems.sumOf { it.unitPrice * it.quantity }
        if (explicitTotal != null) {
            totalBill = explicitTotal!!
            if (Math.abs((effectiveSubtotal + deliveryCharge) - totalBill) > 1.0) {
                if (!warnings.any { it.contains("মোট বিলের হিসাবে অমিল") }) {
                    warnings.add("মোট বিলের হিসাবে অমিল: ড্রেস (৳${effectiveSubtotal.toInt()}) + ডেলিভারি (৳${deliveryCharge.toInt()}) = ৳${(effectiveSubtotal + deliveryCharge).toInt()}, কিন্তু মেসেজে মোট ৳${totalBill.toInt()} উল্লেখ আছে")
                }
            }
        } else if (genMatch == null && (explicitDue == null || Math.abs(advanceAmount + explicitDue!! - (effectiveSubtotal + deliveryCharge)) > 1.0)) {
            totalBill = effectiveSubtotal + deliveryCharge
        }

        if (allExplicitPrices.size > 1 && multiLineItems.isEmpty()) {
            candidates["price"] = allExplicitPrices.map { it.toInt().toString() }.toMutableList()
            warnings.add("মেসেজে একাধিক ভিন্ন মূল্য পাওয়া গেছে: ${allExplicitPrices.map { "৳" + it.toInt() }.joinToString(", ")}")
        }

        if (explicitDue != null && Math.abs((totalBill - advanceAmount) - explicitDue!!) > 1.0) {
            candidates["codDue"] = mutableListOf(explicitDue!!.toInt().toString())
        }

        val mentionsAdvanceWithoutAmount = explicitAdvance == null &&
                Regex("""(?i)\b(?:advance|adv|bkash|bikash|অগ্রিম|বিকাশ)\b""").containsMatchIn(rawText) &&
                !Regex("""(?i)\b(?:advance|adv|bkash|bikash|অগ্রিম|বিকাশ)\s*[:=\-–—~]?\s*[0-9]+""").containsMatchIn(normalized)
        if (mentionsAdvanceWithoutAmount) {
            warnings.add("অগ্রিম বা বিকাশের উল্লেখ আছে কিন্তু নির্দিষ্ট টাকার পরিমাণ পাওয়া যায়নি")
        }

        if (!isPriceExplicit) {
            confidences["unitPrice"] = FieldConfidence.MISSING
            warnings.add("পণ্যের মূল্য স্পষ্ট নয় (ডিফল্ট ৳${unitPrice.toInt()} ড্রাফট করা হয়েছে)")
        } else {
            confidences["unitPrice"] = FieldConfidence.EXPLICIT
        }

        // -------------------------------------------------------------
        // STAGE 10: Cross-field Validation & Result Construction
        // -------------------------------------------------------------
        return ParsedOrderDraft(
            customerName = finalName,
            customerPhone = primaryPhone,
            customerAddress = finalAddress,
            district = detectedDistrict,
            thana = finalThana,
            items = parsedItems,
            dressName = finalDress,
            size = finalSize,
            color = finalColor,
            quantity = finalQuantity,
            unitPrice = unitPrice,
            advanceAmount = advanceAmount,
            deliveryTier = deliveryTier,
            deliveryCharge = deliveryCharge,
            totalBill = totalBill,
            orderType = orderType,
            specialInstructions = finalSpecialInstructions,
            rawText = rawText,
            photoUri = null,
            isPriceExplicitInMessage = isPriceExplicit,
            isSizeExplicitInMessage = finalSize.isNotBlank(),
            isColorExplicitInMessage = finalColor.isNotBlank(),
            isDeliveryExplicitInMessage = isDeliveryExplicit,
            isAdvanceExplicitInMessage = advanceAmount > 0,
            confidenceWarnings = warnings,
            alternativeCandidates = candidates,
            fieldConfidences = confidences
        )
    }

    // Helper functions
    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(ParserDictionaries.BANGLA_TO_ENG_DIGITS[ch] ?: ch)
        }
        return sb.toString()
    }

    private fun cleanNoiseAndBullets(line: String): String {
        return line
            .replace(Regex("""^[\s•\*\-\+👉📍📞📱👗💰📦👤🏠📏🎨💳🚚🧾💵📝🛍️~|]+"""), "")
            .trim()
    }

    private fun sanitizeFieldValue(value: String): String {
        return cleanPrefixLabels(
            value
                .replace(Regex("""^[:=\-–—~,\s]+"""), "")
                .replace(Regex("""[:=\-–—~,\s]+$"""), "")
                .trim()
        )
    }

    fun cleanPrefixLabels(text: String): String {
        var result = text.trim()
        // Strip common leading emojis and bullet symbols:
        result = result.replace(Regex("""^[\s•\*\-\+👉📍📞📱👗💰📦👤🏠📏🎨💳🚚🧾💵📝🛍️~|]+"""), "").trim()
        // Strip name, address, phone, thana prefixes (Bangla, English, Banglish)
        result = result.replace(Regex("""(?i)^(?:নাম|গ্রাহকের\s*নাম|কাস্টমারের\s*নাম|customer\s*name|cust\s*name|name|customer|cust|apnar\s*nam|nam)\s*[:=\-–—~]?\s*"""), "")
        result = result.replace(Regex("""(?i)^(?:ঠিকানা|কাস্টমার\s*ঠিকানা|এড্রেস|address|add|thikana|location|লোকেশন|delivery\s*address)\s*[:=\-–—~]?\s*"""), "")
        result = result.replace(Regex("""(?i)^(?:ফোন|মোবাইল|phone|mobile|cell|contact|phn|ph)\s*[:=\-–—~]?\s*"""), "")
        result = result.replace(Regex("""(?i)^(?:থানা|উপজেলা|thana|upazila|p\.?s\.?)\s*[:=\-–—~]?\s*"""), "")
        result = result.replace(Regex("""^[0-9]+[\.\)\-\:\s]+"""), "")
        result = result.replace(Regex("""^[:=\-–—~,\s]+"""), "")
        // Handle repeated emojis or nested prefixes (e.g., "🛍️ এড্রেস: " or "👤 Name: ")
        result = result.replace(Regex("""^[\s•\*\-\+👉📍📞📱👗💰📦👤🏠📏🎨💳🚚🧾💵📝🛍️~|]+"""), "").trim()
        result = result.replace(Regex("""(?i)^(?:ঠিকানা|এড্রেস|address|add|name|নাম|থানা|উপজেলা)\s*[:=\-–—~]?\s*"""), "").trim()
        result = result.replace(Regex("""^[:=\-–—~,\s]+"""), "").trim()
        return result
    }

    private fun isLikelyAddressLine(line: String): Boolean {
        return ParserDictionaries.ADDRESS_STRUCTURAL_KEYWORDS.any { line.contains(it, ignoreCase = true) } ||
               ParserDictionaries.BANGLADESH_DISTRICTS.keys.any { line.contains(it, ignoreCase = true) } ||
               ParserDictionaries.BANGLADESH_DISTRICTS.values.any { line.contains(it) }
    }

    private fun isPriceOrEquationLine(line: String): Boolean {
        return line.contains("=") || line.contains("+") ||
               line.contains("টাকা", ignoreCase = true) || line.contains("tk", ignoreCase = true) ||
               line.contains("দাম", ignoreCase = true) || line.contains("price", ignoreCase = true)
    }

    private fun isDressKeyword(line: String): Boolean {
        return ParserDictionaries.DRESS_KEYWORDS.any { line.contains(it, ignoreCase = true) }
    }

    private fun isSizeOrColorOrMetaLine(line: String): Boolean {
        val cleaned = cleanPrefixLabels(line).trim()
        val isSize = ParserDictionaries.BODY_SIZE_LABEL_REGEX.containsMatchIn(line) ||
                ParserDictionaries.LENGTH_LABEL_REGEX.containsMatchIn(line) ||
                Regex("""(?i)\b(size|সাইজ|বডি|sz)\b""").containsMatchIn(line) ||
                Regex("""(?i)^\s*(?:3[4-9]|4[0-8]|50|Free|ফ্রি|XS|S|M|L|XL|XXL|2XL|3XL|4XL)\s*$""").matches(cleaned)
        val isColor = ParserDictionaries.COLOR_LABEL_REGEX.containsMatchIn(line) ||
                ParserDictionaries.COLORS.any { it.equals(cleaned, ignoreCase = true) }
        val isQty = ParserDictionaries.QUANTITY_LABEL_REGEX.containsMatchIn(line)
        val isAdvanceOrDue = ParserDictionaries.ADVANCE_LABEL_REGEX.containsMatchIn(line) ||
                ParserDictionaries.DELIVERY_ADVANCE_REGEX.containsMatchIn(line) ||
                ParserDictionaries.DUE_LABEL_REGEX.containsMatchIn(line)
        return isSize || isColor || isQty || isAdvanceOrDue
    }

    private fun isIrrelevantGreetingOrFooter(line: String): Boolean {
        val lower = line.lowercase().trim()
        val greetings = listOf(
            "আসসালামু আলাইকুম", "সালাম", "hello", "hi", "hey", "good morning", "good evening",
            "order details", "order summary", "new order", "confirmation", "fashion aura",
            "inbox check", "check inbox", "ধন্যবাদ"
        )
        return greetings.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }
    }
}
