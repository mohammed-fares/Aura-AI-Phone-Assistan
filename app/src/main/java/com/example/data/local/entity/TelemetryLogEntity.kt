package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TelemetryType {
    VOICE_COMMAND,
    TOUCH_GESTURE,
    NETWORK_TRAFFIC,
    BATTERY_POWER,
    COMMUNICATION,
    SYSTEM_PERFORMANCE,
    SECURITY_AUDIT,
    AI_INFERENCE
}

enum class TelemetrySeverity {
    INFO,
    OPTIMAL,
    WARNING,
    CRITICAL
}

@Entity(tableName = "telemetry_logs")
data class TelemetryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: TelemetryType,
    val title: String,
    val description: String,
    val severity: TelemetrySeverity = TelemetrySeverity.INFO,
    val aiAudited: Boolean = false,
    val aiAnnotation: String? = null
)
