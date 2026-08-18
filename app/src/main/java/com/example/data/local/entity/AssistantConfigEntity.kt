package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_config")
data class AssistantConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val assistantName: String = "نور", // Customizable AI companion name (e.g. Aura, Noor, Sanad)
    val userDisplayName: String = "المستخدم",
    val preferredDialect: String = "العربية (لهجات متعددة)",
    val autoListeningSensitivity: Float = 0.8f,
    val ttsPitch: Float = 1.0f,
    val ttsSpeed: Float = 1.0f,
    val voiceFeedbackEnabled: Boolean = true,
    val lowResourceMode: Boolean = true, // Conserve phone resources & battery
    val backgroundTelemetryEnabled: Boolean = true,
    val shakeGestureActionEnabled: Boolean = true,
    val simulatedRemoteConnected: Boolean = true,
    val lastAuditTimestamp: Long = System.currentTimeMillis()
)
