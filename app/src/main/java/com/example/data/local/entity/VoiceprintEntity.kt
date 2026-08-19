package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voiceprint_profiles")
data class VoiceprintEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val enrolledAt: Long = System.currentTimeMillis(),
    val sampleIndex: Int,
    val phraseText: String,
    val pitchMean: Float,
    val rmsEnergy: Float,
    val spectralEnvelopeHash: String,
    val isVerified: Boolean = true
)
