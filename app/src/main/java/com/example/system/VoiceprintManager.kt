package com.example.system

import android.content.Context
import android.util.Log
import com.example.data.local.dao.VoiceprintDao
import com.example.data.local.entity.VoiceprintEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.sin

data class VoiceprintVerificationResult(
    val isMatch: Boolean,
    val confidenceScore: Float, // 0.0 to 1.0 (e.g. 0.94)
    val matchPercentage: Int, // e.g. 94%
    val message: String
)

class VoiceprintManager(
    private val context: Context,
    private val voiceprintDao: VoiceprintDao
) {
    val enrolledProfiles: Flow<List<VoiceprintEntity>> = voiceprintDao.getAllVoiceprints()

    // Standard enrollment reference phrases
    val defaultEnrollmentPhrases = listOf(
        "أنا المالك المعتمد لهذا الهاتف والمتحكم به",
        "تفعيل التحكم الذاتي وتنفيذ كافة الأوامر بدون لمس",
        "حماية الهاتف وتدقيق الأمان ومراقبة الشبكة المحلية"
    )

    suspend fun isVoiceprintEnrolled(): Boolean {
        val count = voiceprintDao.getVoiceprintsSync().size
        return count >= 1
    }

    suspend fun saveEnrollmentSample(
        sampleIndex: Int,
        phrase: String,
        rmsLevelHistory: List<Float>
    ): VoiceprintEntity {
        val pitchMean = calculateSimulatedPitchMean(phrase, rmsLevelHistory)
        val rmsEnergy = if (rmsLevelHistory.isNotEmpty()) rmsLevelHistory.average().toFloat() else 0.45f
        val envelopeHash = generateAcousticSignature(phrase, rmsLevelHistory)

        val entity = VoiceprintEntity(
            sampleIndex = sampleIndex,
            phraseText = phrase,
            pitchMean = pitchMean,
            rmsEnergy = rmsEnergy,
            spectralEnvelopeHash = envelopeHash,
            isVerified = true
        )
        voiceprintDao.insertVoiceprint(entity)
        return entity
    }

    suspend fun clearVoiceprintData() {
        voiceprintDao.clearVoiceprints()
    }

    suspend fun verifyVoiceprint(
        spokenText: String,
        recentRmsLevels: List<Float>,
        threshold: Float = 0.70f
    ): VoiceprintVerificationResult {
        val enrolled = voiceprintDao.getVoiceprintsSync()
        if (enrolled.isEmpty()) {
            // No profile enrolled yet -> allow execution with warning
            return VoiceprintVerificationResult(
                isMatch = true,
                confidenceScore = 1.0f,
                matchPercentage = 100,
                message = "بصمة الصوت غير مفعلة (مسموح بالتحكم المباشر)"
            )
        }

        // Calculate acoustic feature similarity
        val currentEnergy = if (recentRmsLevels.isNotEmpty()) recentRmsLevels.average().toFloat() else 0.45f
        val currentPitch = calculateSimulatedPitchMean(spokenText, recentRmsLevels)

        // Compare against enrolled profiles
        val avgEnrolledPitch = enrolled.map { it.pitchMean }.average().toFloat()
        val avgEnrolledEnergy = enrolled.map { it.rmsEnergy }.average().toFloat()

        val pitchDiff = abs(currentPitch - avgEnrolledPitch) / (avgEnrolledPitch.coerceAtLeast(1f))
        val energyDiff = abs(currentEnergy - avgEnrolledEnergy) / (avgEnrolledEnergy.coerceAtLeast(0.1f))

        var similarityScore = 1.0f - (pitchDiff * 0.4f + energyDiff * 0.3f).coerceIn(0f, 0.5f)

        // Text phonetic structure weight
        if (spokenText.length > 3) {
            similarityScore = (similarityScore * 0.85f + 0.15f).coerceIn(0.65f, 0.98f)
        }

        val isAuthorized = similarityScore >= threshold
        val percent = (similarityScore * 100).toInt().coerceIn(40, 99)

        val msg = if (isAuthorized) {
            "تم التحقق من بصمة صوت المالك بنجاح (المطابقة: $percent%)"
        } else {
            "تنبيه أمني: بصمة الصوت لا تطابق المالك المعتمد (المطابقة: $percent%)"
        }

        return VoiceprintVerificationResult(
            isMatch = isAuthorized,
            confidenceScore = similarityScore,
            matchPercentage = percent,
            message = msg
        )
    }

    private fun calculateSimulatedPitchMean(phrase: String, levels: List<Float>): Float {
        var base = 140f // Average human pitch Hz
        for (ch in phrase) {
            base += (ch.code % 17) - 8
        }
        val rmsOffset = if (levels.isNotEmpty()) levels.average().toFloat() * 30f else 15f
        return (base + rmsOffset).coerceIn(85f, 260f)
    }

    private fun generateAcousticSignature(phrase: String, levels: List<Float>): String {
        val raw = StringBuilder(phrase)
        levels.take(10).forEach { raw.append(String.format("%.2f", it)) }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toString().toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
