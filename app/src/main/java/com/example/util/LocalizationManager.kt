package com.example.util

import java.util.Locale

enum class AppLanguage(val code: String, val titleAr: String, val titleEn: String) {
    SYSTEM("system", "لغة النظام (تلقائي)", "System Default (Auto)"),
    ARABIC("ar", "العربية", "Arabic"),
    ENGLISH("en", "English", "English")
}

object LocalizationManager {

    fun getEffectiveLanguage(preference: String): String {
        return when (preference) {
            "ar" -> "ar"
            "en" -> "en"
            else -> {
                val systemLang = Locale.getDefault().language
                if (systemLang.equals("ar", ignoreCase = true)) "ar" else "en"
            }
        }
    }

    fun isRtl(preference: String): Boolean {
        return getEffectiveLanguage(preference) == "ar"
    }

    // Helper translation accessor
    fun str(lang: String, ar: String, en: String): String {
        return if (getEffectiveLanguage(lang) == "ar") ar else en
    }
}
