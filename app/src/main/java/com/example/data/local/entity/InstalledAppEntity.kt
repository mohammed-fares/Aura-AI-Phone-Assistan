package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val appNameArabic: String = "",
    val category: String = "GENERAL",
    val capabilities: String = "OPEN,CLOSE", // Comma-separated or descriptor
    val keywords: String = "", // e.g. "واتساب, رسائل, شات, whatsapp"
    val isSystemApp: Boolean = false,
    val launchIntentAvailable: Boolean = true,
    val aiMechanicsDescription: String = "", // How AI interacts with this app
    val lastScannedTimestamp: Long = System.currentTimeMillis()
)
