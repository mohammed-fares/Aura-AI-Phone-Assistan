package com.example.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.util.LocalizationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

class VoiceSpeechEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val engineScope = CoroutineScope(Dispatchers.IO + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsAudioLevel = MutableStateFlow(0f)
    val rmsAudioLevel: StateFlow<Float> = _rmsAudioLevel.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _isUsingFallbackAcousticEngine = MutableStateFlow(false)
    val isUsingFallbackAcousticEngine: StateFlow<Boolean> = _isUsingFallbackAcousticEngine.asStateFlow()

    // Sound control
    private var isAllSoundsMuted = false
    private var isMuteMicBleepsAndSystemSounds = true

    // Persistent listening state
    private var keepMicContinuouslyOpen = true
    private var isContinuousListeningMode = false
    private var currentLanguage = "system"
    private var onResultCallback: ((String, List<Float>) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    // Health & Watchdog Tracking
    private var lastRecognizerEventTimestamp = System.currentTimeMillis()
    private var watchdogJob: Job? = null
    private var isRecognizerBusyOrStarting = false
    private var consecutiveErrorCount = 0
    private var restartPending = false

    // Recent RMS buffer for biometric voiceprint analysis
    val recentRmsBuffer = mutableListOf<Float>()

    init {
        initTts()
        startWatchdogEngine()
    }

    fun setMuteAllSounds(muted: Boolean) {
        isAllSoundsMuted = muted
        if (muted) {
            stopSpeaking()
        }
    }

    fun setMuteMicBleepsAndSystemSounds(muted: Boolean) {
        isMuteMicBleepsAndSystemSounds = muted
    }

    fun isMuted(): Boolean = isAllSoundsMuted

    fun setKeepMicContinuouslyOpen(keepOpen: Boolean) {
        keepMicContinuouslyOpen = keepOpen
    }

    fun setLanguage(lang: String) {
        currentLanguage = lang
        val effective = LocalizationManager.getEffectiveLanguage(lang)
        if (isTtsReady) {
            try {
                val locale = if (effective == "ar") Locale("ar") else Locale.ENGLISH
                textToSpeech?.setLanguage(locale)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    val effective = LocalizationManager.getEffectiveLanguage(currentLanguage)
                    val targetLocale = if (effective == "ar") Locale("ar") else Locale.ENGLISH
                    val result = textToSpeech?.setLanguage(targetLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.getDefault())
                    }
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            if (!isAllSoundsMuted) {
                                _isSpeaking.value = true
                            }
                        }
                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceSpeechEngine", "TTS Init failed", e)
        }
    }

    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
        if (isAllSoundsMuted || !isTtsReady || text.isBlank()) {
            _isSpeaking.value = false
            return
        }
        try {
            textToSpeech?.setPitch(pitch)
            textToSpeech?.setSpeechRate(speed)
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aura_speech_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("VoiceSpeechEngine", "Error in speak()", e)
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // ignore
        }
        _isSpeaking.value = false
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start continuous, robust listening that automatically reconnects and receives speech.
     */
    fun startContinuousListening(
        language: String = currentLanguage,
        onResult: (String, List<Float>) -> Unit,
        onError: (String) -> Unit
    ) {
        currentLanguage = language
        isContinuousListeningMode = true
        onResultCallback = onResult
        onErrorCallback = onError
        consecutiveErrorCount = 0
        lastRecognizerEventTimestamp = System.currentTimeMillis()

        if (!hasRecordAudioPermission()) {
            _isListening.value = false
            onError.invoke("يرجى منح إذن الميكروفون للبدء في الاستماع للأوامر الصوتية.")
            return
        }

        _isListening.value = true

        mainHandler.post {
            createAndStartRecognizer()
        }
    }

    private fun createAndStartRecognizer() {
        if (!isContinuousListeningMode) return
        if (!hasRecordAudioPermission()) {
            _isListening.value = false
            return
        }

        try {
            if (speechRecognizer == null) {
                val available = SpeechRecognizer.isRecognitionAvailable(context)
                if (!available) {
                    Log.w("VoiceSpeechEngine", "Speech recognition service not standard on device, using fallback")
                    _isUsingFallbackAcousticEngine.value = true
                }
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(AuraRecognitionListener())
                }
            }

            val effective = LocalizationManager.getEffectiveLanguage(currentLanguage)
            val langTag = if (effective == "ar") "ar-SA" else "en-US"

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            isRecognizerBusyOrStarting = true
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("VoiceSpeechEngine", "Error in createAndStartRecognizer", e)
            scheduleSafeRestart(delayMs = 800)
        }
    }

    private inner class AuraRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            isRecognizerBusyOrStarting = false
            consecutiveErrorCount = 0
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
            _rmsAudioLevel.value = normalized
            synchronized(recentRmsBuffer) {
                if (recentRmsBuffer.size > 60) recentRmsBuffer.removeAt(0)
                recentRmsBuffer.add(normalized)
            }
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        }

        override fun onEndOfSpeech() {
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        }

        override fun onError(error: Int) {
            lastRecognizerEventTimestamp = System.currentTimeMillis()
            isRecognizerBusyOrStarting = false
            consecutiveErrorCount++

            if (!isContinuousListeningMode) return

            Log.d("VoiceSpeechEngine", "Recognition error code: $error, count: $consecutiveErrorCount")

            val restartDelay = when {
                consecutiveErrorCount > 5 -> 2500L
                consecutiveErrorCount > 2 -> 1200L
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH -> 400L
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 700L
                error == SpeechRecognizer.ERROR_CLIENT -> 800L
                else -> 600L
            }

            scheduleSafeRestart(delayMs = restartDelay)
        }

        override fun onResults(results: Bundle?) {
            lastRecognizerEventTimestamp = System.currentTimeMillis()
            isRecognizerBusyOrStarting = false
            consecutiveErrorCount = 0

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _lastRecognizedText.value = text
                val copyBuffer = synchronized(recentRmsBuffer) { recentRmsBuffer.toList() }
                onResultCallback?.invoke(text, copyBuffer)
            }

            if (isContinuousListeningMode) {
                scheduleSafeRestart(delayMs = 350L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            lastRecognizerEventTimestamp = System.currentTimeMillis()
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { _lastRecognizedText.value = it }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        }
    }

    private fun scheduleSafeRestart(delayMs: Long) {
        if (!isContinuousListeningMode || restartPending) return
        restartPending = true

        mainHandler.postDelayed({
            restartPending = false
            if (!isContinuousListeningMode) return@postDelayed
            try {
                if (!hasRecordAudioPermission()) {
                    _isListening.value = false
                    return@postDelayed
                }

                // If recognizer had repeated errors, clean recreate it
                if (consecutiveErrorCount >= 3 || speechRecognizer == null) {
                    try {
                        speechRecognizer?.cancel()
                        speechRecognizer?.destroy()
                    } catch (e: Exception) {
                        // ignore
                    }
                    speechRecognizer = null
                }

                createAndStartRecognizer()
            } catch (e: Exception) {
                Log.e("VoiceSpeechEngine", "Restart failed", e)
            }
        }, delayMs)
    }

    /**
     * Active Watchdog Coroutine:
     * Checks recognizer state every 5 seconds. If the recognizer stalled or stopped
     * unexpectedly, gently recovers it without hanging the UI thread.
     */
    private fun startWatchdogEngine() {
        watchdogJob?.cancel()
        watchdogJob = engineScope.launch {
            while (isActive) {
                delay(5000)
                if (isContinuousListeningMode && hasRecordAudioPermission()) {
                    val now = System.currentTimeMillis()
                    // If no recognizer lifecycle event occurred for > 10 seconds, softly kickstart
                    if (now - lastRecognizerEventTimestamp > 10000 && !restartPending) {
                        Log.d("VoiceSpeechEngine", "Watchdog triggered: Soft self-healing of microphone stream")
                        mainHandler.post {
                            scheduleSafeRestart(delayMs = 200L)
                        }
                    }
                }
            }
        }
    }

    fun stopListening() {
        isContinuousListeningMode = false
        _isListening.value = false
        restartPending = false

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        watchdogJob?.cancel()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // ignore
            }
        }
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
