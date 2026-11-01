package com.example.data.model

enum class AppLanguage(val code: String, val titleBn: String, val titleEn: String) {
    BANGLA("bn", "বাংলা", "Bangla"),
    ENGLISH("en", "English", "English")
}

data class UserPreferences(
    val isDarkMode: Boolean? = null, // null = Follow system
    val appLanguage: AppLanguage = AppLanguage.BANGLA,
    val useBanglaDigits: Boolean = true,
    val steadfastApiKey: String = "",
    val steadfastSecretKey: String = "",
    val autoSyncCourier: Boolean = true
)
