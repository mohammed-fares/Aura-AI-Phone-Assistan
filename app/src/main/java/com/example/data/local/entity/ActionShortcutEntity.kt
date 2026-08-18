package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActionType {
    CALL_CONTACT,
    SEND_MESSAGE,
    TOGGLE_SILENT_MODE,
    DEVICE_DIAGNOSTIC,
    BATTERY_OPTIMIZATION,
    NETWORK_AUDIT,
    VOICE_NOTE,
    REMOTE_LOCK_ALERT,
    AI_SUMMARIZE_ACTIVITY
}

@Entity(tableName = "action_shortcuts")
data class ActionShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerVoicePhrase: String,
    val actionType: ActionType,
    val payload: String = "",
    val dialect: String = "العربية (تلقائي)",
    val executionCount: Int = 0,
    val lastExecutedAt: Long = 0L,
    val isGestureEnabled: Boolean = true
)
