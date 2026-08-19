package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_config")
data class AssistantConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val assistantName: String = "AURA",
    val userDisplayName: String = "User",
    val appLanguage: String = "system", // "system", "ar", "en"
    val preferredDialect: String = "العربية / English (Auto)",
    val autoListeningSensitivity: Float = 0.85f,
    val ttsPitch: Float = 1.0f,
    val ttsSpeed: Float = 1.0f,
    val voiceFeedbackEnabled: Boolean = false, // User requested silent execution by default
    val autoContinuousListening: Boolean = true, // Hands-free auto listening on app launch
    val biometricVoiceprintEnabled: Boolean = true, // User voiceprint biometric security
    val voiceprintEnrolled: Boolean = false,
    val voiceprintSignature: String = "",
    val voiceprintConfidenceThreshold: Float = 0.75f,
    val lowResourceMode: Boolean = true, // Conserve phone resources & battery
    val localNetworkMonitoringEnabled: Boolean = true, // Monitor LAN sharing & activities
    val securityThreatScanAutoAlerts: Boolean = true,
    val backgroundTelemetryEnabled: Boolean = true,
    val backgroundServiceEnabled: Boolean = true,
    val shakeGestureActionEnabled: Boolean = true,
    val speechEngineMode: String = "auto", // "auto", "system", "embedded"
    val lastAuditTimestamp: Long = System.currentTimeMillis()
)
