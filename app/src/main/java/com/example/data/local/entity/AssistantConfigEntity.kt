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
    val voiceFeedbackEnabled: Boolean = false, // Voice response speech
    val muteAllAppSounds: Boolean = true, // Completely mute all TTS speech, sounds and audio effects
    val muteMicBleepsAndSystemSounds: Boolean = true, // Completely silence mic open/close chimes and phone bleeps
    val autonomousUiInteractions: Boolean = true, // Autonomous direct typing, sending and screen navigation via Accessibility
    val keepMicOpenContinuously: Boolean = true, // Keep microphone continuously open without open/close cycles
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
    val customWakeWord: String = "أورا", // e.g. "أورا", "Aura", "يا مساعد", "يا ذكاء"
    val wakeWordOnlyMode: Boolean = false, // When true, require wake word calling; when false, always-active
    val wakeWordSensitivity: Float = 0.85f,
    val lastAuditTimestamp: Long = System.currentTimeMillis()
)
