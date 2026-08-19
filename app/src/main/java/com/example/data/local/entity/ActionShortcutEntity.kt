package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActionType {
    CALL_CONTACT,          // Make phone call / dial
    SEND_MESSAGE,          // Send SMS / text message
    SEND_EMAIL,            // Compose and send email
    OPEN_APP,              // Launch any installed app
    OPEN_CAMERA,           // Open system camera
    OPEN_BROWSER,          // Open web browser / URL
    SET_ALARM,             // Open Clock / Set Alarm / Timer
    WEB_SEARCH,            // Perform web / Google search
    TOGGLE_SILENT_MODE,    // Mute / Unmute / DND
    TOGGLE_FLASHLIGHT,     // Turn on/off camera flashlight
    OPEN_SETTINGS,         // Main phone settings
    OPEN_WIFI_SETTINGS,    // Wi-Fi network settings
    OPEN_BLUETOOTH_SETTINGS,// Bluetooth settings
    OPEN_DISPLAY_SETTINGS, // Display & Brightness settings
    OPEN_BATTERY_SETTINGS, // Battery & Power settings
    OPEN_SECURITY_SETTINGS,// Security & Biometrics settings
    SYSTEM_SECURITY_SCAN,  // Deep vulnerability scan
    LOCAL_NETWORK_SCAN,    // LAN nodes & shared devices audit
    DEVICE_DIAGNOSTIC,     // Hardware & telemetry diagnostic
    BATTERY_OPTIMIZATION,  // Low-power background optimization
    NETWORK_AUDIT,         // Traffic & port analysis
    VOICE_NOTE,            // Save quick audio/text note
    REMOTE_LOCK_ALERT,     // Lock alert notice
    AI_SUMMARIZE_ACTIVITY  // AI summary of phone activity
}

@Entity(tableName = "action_shortcuts")
data class ActionShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerVoicePhrase: String,
    val actionType: ActionType,
    val payload: String = "",
    val dialect: String = "العربية / English",
    val executionCount: Int = 0,
    val lastExecutedAt: Long = 0L,
    val isGestureEnabled: Boolean = true
)
