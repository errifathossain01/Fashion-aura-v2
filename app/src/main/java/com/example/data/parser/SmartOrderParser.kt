package com.example.data.parser

/**
 * Backward-compatible entry-point for Fashion Aura Smart Order Parsing.
 * Delegates to the deterministic, extensible, offline-first [ParsingPipeline].
 */
object SmartOrderParser {

    fun normalizeDigits(input: String): String {
        return ParsingPipeline.normalizeDigits(input)
    }

    fun parse(rawText: String): ParsedOrderDraft {
        return ParsingPipeline.execute(rawText)
    }
}
