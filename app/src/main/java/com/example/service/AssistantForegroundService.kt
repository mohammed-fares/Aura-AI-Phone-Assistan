package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.AuraApplication
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.ActionType
import com.example.data.local.entity.TelemetrySeverity
import com.example.data.local.entity.TelemetryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AssistantForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var auraApp: AuraApplication

    companion object {
        const val CHANNEL_ID = "aura_assistant_background_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TOGGLE = "com.example.service.ACTION_TOGGLE"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        private val _lastBackgroundActionStatus = MutableStateFlow<String?>("الخدمة قيد التشغيل في الخلفية")
        val lastBackgroundActionStatus = _lastBackgroundActionStatus.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        auraApp = application as AuraApplication
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                startBackgroundListeningLoop()
            }
            else -> {
                startForegroundWithNotification()
                startBackgroundListeningLoop()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        _isServiceRunning.value = true
        val notification = buildForegroundNotification("المساعد يستمع في الخلفية 🟢 - جاهز لتنفيذ الأوامر")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("المساعد الذكي والتحكم الذاتي (في الخلفية)")
            .setContentText(statusText)
            .setSubText("مراقبة مستمرة بدون لمس الشاشة")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.mipmap.ic_launcher, "إيقاف الخدمة", stopPendingIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildForegroundNotification(statusText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun startBackgroundListeningLoop() {
        val voiceEngine = auraApp.voiceSpeechEngine
        val voiceprintManager = auraApp.voiceprintManager
        val actionEngine = auraApp.actionExecutionEngine
        val wakeWordManager = auraApp.wakeWordManager
        val repository = auraApp.repository

        voiceEngine.startContinuousListening(
            onResult = { recognizedText, rmsHistory ->
                serviceScope.launch {
                    if (recognizedText.isBlank()) return@launch

                    val config = repository.config.firstOrNull() ?: com.example.data.local.entity.AssistantConfigEntity()

                    // Check wake word detection
                    val wakeResult = wakeWordManager.checkWakeWord(
                        rawText = recognizedText,
                        configuredAssistantName = config.assistantName,
                        customWakeWord = config.customWakeWord
                    )

                    // If wake-word-only mode is active and wake word was not mentioned, ignore ambient chatter
                    if (config.wakeWordOnlyMode && !wakeResult.isWakeWordDetected) {
                        return@launch
                    }

                    // Provide discrete haptic feedback when wake word is called
                    if (wakeResult.isWakeWordDetected) {
                        actionEngine.vibratePhone(60L)
                    }

                    // Biometric voiceprint check
                    val verification = if (config.biometricVoiceprintEnabled) {
                        voiceprintManager.verifyVoiceprint(
                            spokenText = recognizedText,
                            recentRmsLevels = rmsHistory,
                            threshold = config.voiceprintConfidenceThreshold
                        )
                    } else {
                        com.example.system.VoiceprintVerificationResult(
                            isMatch = true,
                            confidenceScore = 1.0f,
                            matchPercentage = 100,
                            message = "التحكم المباشر مصرح به"
                        )
                    }

                    if (!verification.isMatch) {
                        actionEngine.vibratePhone(250L)
                        val warn = "⚠️ تم حظر أمر في الخلفية: بصمة الصوت غير مطابقة"
                        _lastBackgroundActionStatus.value = warn
                        updateNotification(warn)
                        repository.logTelemetry(
                            type = TelemetryType.SYSTEM_PERFORMANCE,
                            title = "حظر أمر في الخلفية (صوت غير مصرح)",
                            description = "الأمر: $recognizedText (تطابق ${verification.matchPercentage}%)",
                            severity = TelemetrySeverity.CRITICAL,
                            aiAudited = true
                        )
                        return@launch
                    }

                    // If user called name only ("يا أورا" / "Aura")
                    if (wakeResult.isStandAloneCall) {
                        val ackMsg = if (config.appLanguage == "en") "Yes, I am listening" else "نعم، تفضل أنا أسمعك"
                        _lastBackgroundActionStatus.value = "تمت الاستجابة لنداء: ${wakeResult.matchedWakeWord} 🟢"
                        updateNotification("تم الاستيقاظ لنداء: ${wakeResult.matchedWakeWord} 🟢")
                        if (!config.muteAllAppSounds && config.voiceFeedbackEnabled) {
                            voiceEngine.speak(ackMsg)
                        }
                        return@launch
                    }

                    // Extract executable command (strip wake word if present)
                    val commandToExecute = if (wakeResult.isWakeWordDetected && wakeResult.extractedCommand.isNotBlank()) {
                        wakeResult.extractedCommand
                    } else {
                        recognizedText
                    }

                    // Process command via AI
                    try {
                        val parsed = repository.processVoiceCommand(commandToExecute)
                        val status = "تم تنفيذ في الخلفية: ${parsed.responseSpeechText}"
                        _lastBackgroundActionStatus.value = status
                        updateNotification(status)

                        if (!config.muteAllAppSounds && config.voiceFeedbackEnabled) {
                            voiceEngine.speak(parsed.responseSpeechText)
                        }

                        parsed.actionType?.let { action ->
                            actionEngine.executeAction(action, parsed.actionPayload) { feedback ->
                                serviceScope.launch {
                                    repository.logTelemetry(
                                        type = TelemetryType.TOUCH_GESTURE,
                                        title = "تنفيذ أمر خلفي (${wakeResult.matchedWakeWord ?: "مباشر"}): $action",
                                        description = feedback,
                                        severity = TelemetrySeverity.OPTIMAL,
                                        aiAudited = true
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        _lastBackgroundActionStatus.value = "حدث خطأ في معالجة الأمر في الخلفية: ${e.message}"
                    }
                }
            },
            onError = { errorMsg ->
                // Keep listening silently
            }
        )
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AuraAssistant::BackgroundExecutionWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours max
            }
        } catch (e: Exception) {
            // Ignore wakelock acquisition failure
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "خدمة المساعد في الخلفية (Always-On Agent)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار دائم لتشغيل المساعد والتحكم بالأوامر في الخلفية حتى مع إغلاق الشاشة"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        _isServiceRunning.value = false
        auraApp.voiceSpeechEngine.stopListening()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        releaseWakeLock()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
