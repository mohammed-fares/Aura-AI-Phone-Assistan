package com.example.system

import java.util.Locale

data class WakeWordCheckResult(
    val isWakeWordDetected: Boolean,
    val matchedWakeWord: String? = null,
    val extractedCommand: String = "",
    val isStandAloneCall: Boolean = false
)

class WakeWordManager {

    companion object {
        val DEFAULT_WAKE_WORDS_AR = listOf(
            "يا اورا", "اورا", "يا أورا", "أورا", "يا مساعد", "يا مساعدي", "مساعدي", "يا ذكاء", "يا رفيق"
        )
        val DEFAULT_WAKE_WORDS_EN = listOf(
            "hey aura", "aura", "ok aura", "hi aura", "assistant", "hey assistant", "ok assistant"
        )
    }

    /**
     * Checks if the spoken text contains the assistant wake word.
     * Extracts the remaining command payload cleanly.
     */
    fun checkWakeWord(
        rawText: String,
        configuredAssistantName: String = "AURA",
        customWakeWord: String = "أورا"
    ): WakeWordCheckResult {
        if (rawText.isBlank()) {
            return WakeWordCheckResult(isWakeWordDetected = false)
        }

        val normalizedInput = normalizeArabicAndEnglish(rawText)

        // Build list of target trigger phrases
        val targetTriggers = mutableListOf<String>()
        
        // Custom configured words
        val cleanCustom = normalizeArabicAndEnglish(customWakeWord)
        if (cleanCustom.isNotBlank()) {
            targetTriggers.add(cleanCustom)
            targetTriggers.add("يا $cleanCustom")
            targetTriggers.add("hey $cleanCustom")
            targetTriggers.add("ok $cleanCustom")
        }

        val cleanName = normalizeArabicAndEnglish(configuredAssistantName)
        if (cleanName.isNotBlank()) {
            targetTriggers.add(cleanName)
            targetTriggers.add("يا $cleanName")
            targetTriggers.add("hey $cleanName")
            targetTriggers.add("ok $cleanName")
        }

        // Add standard defaults
        targetTriggers.addAll(DEFAULT_WAKE_WORDS_AR.map { normalizeArabicAndEnglish(it) })
        targetTriggers.addAll(DEFAULT_WAKE_WORDS_EN.map { normalizeArabicAndEnglish(it) })

        // Check longest match first to avoid partial prefix conflict
        val sortedTriggers = targetTriggers.distinct().sortedByDescending { it.length }

        for (trigger in sortedTriggers) {
            if (trigger.isBlank()) continue

            // 1. Exact match (Stand-alone call: e.g. user just said "أورا" or "يا أورا")
            if (normalizedInput == trigger) {
                return WakeWordCheckResult(
                    isWakeWordDetected = true,
                    matchedWakeWord = trigger,
                    extractedCommand = "",
                    isStandAloneCall = true
                )
            }

            // 2. Starts with trigger (e.g. "يا أورا اتصل بمحمد")
            if (normalizedInput.startsWith("$trigger ") || normalizedInput.startsWith("$trigger,")) {
                // Find actual cut point in original rawText
                val cutLength = trigger.length
                val remaining = if (normalizedInput.length > cutLength) {
                    rawText.substring(cutLength.coerceAtMost(rawText.length)).trim(' ', ',', '،', ':', '.')
                } else ""

                return WakeWordCheckResult(
                    isWakeWordDetected = true,
                    matchedWakeWord = trigger,
                    extractedCommand = remaining.ifBlank { "" },
                    isStandAloneCall = remaining.isBlank()
                )
            }

            // 3. Contains trigger inside (e.g. "لو سمحت يا أورا افتح الواتساب")
            val index = normalizedInput.indexOf(trigger)
            if (index != -1) {
                val before = normalizedInput.substring(0, index).trim()
                val after = rawText.substring((index + trigger.length).coerceAtMost(rawText.length)).trim(' ', ',', '،', ':', '.')

                val combinedCommand = if (after.isNotBlank()) after else before
                return WakeWordCheckResult(
                    isWakeWordDetected = true,
                    matchedWakeWord = trigger,
                    extractedCommand = combinedCommand,
                    isStandAloneCall = combinedCommand.isBlank()
                )
            }
        }

        return WakeWordCheckResult(
            isWakeWordDetected = false,
            extractedCommand = rawText
        )
    }

    /**
     * Normalizes text for robust fuzzy comparison:
     * - Standardizes Arabic letters (أ, إ, آ -> ا), (ة -> ه), (ى -> ي)
     * - Removes diacritics / tashkeel
     * - Lowercases English letters
     * - Trims redundant punctuation and spaces
     */
    fun normalizeArabicAndEnglish(text: String): String {
        var clean = text.lowercase(Locale.ROOT).trim()

        // Remove Arabic diacritics
        clean = clean.replace(Regex("[\u064B-\u065F\u0670]"), "")

        // Normalize Alifs
        clean = clean.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ء', '')
            .replace('ئ', 'ي')
            .replace('ؤ', 'و')
            .replace('ة', 'ه')
            .replace('ى', 'ي')

        // Clean extra spaces and punctuation
        clean = clean.replace(Regex("[,،.:!?؛\n\r\t]+"), " ")
        clean = clean.replace(Regex("\\s+"), " ").trim()

        return clean
    }
}
