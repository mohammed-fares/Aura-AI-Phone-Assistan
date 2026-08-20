package com.example.system

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AutomaticGainControl
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
    private var isAllSoundsMuted = true
    private var isMuteMicBleepsAndSystemSounds = true

    // Persistent non-stop continuous listening (Never Stops while powered on)
    private var keepMicContinuouslyOpen = true
    private var isContinuousListeningMode = false
    private var currentLanguage = "system"
    private var onResultCallback: ((String, List<Float>) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    // Watchdog & Health Tracking
    private var lastRecognizerEventTimestamp = System.currentTimeMillis()
    private var watchdogJob: Job? = null
    private var isRecognizerBusyOrStarting = false

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

    /**
     * Checks if audio is currently playing internally on the phone (video, music, recording).
     * Used to prevent voice commands originating from phone speaker output.
     */
    fun isInternalPlaybackActive(): Boolean {
        return try {
            audioManager?.isMusicActive == true
        } catch (e: Exception) {
            false
        }
    }

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
    }

    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
        if (isAllSoundsMuted || !isTtsReady || text.isBlank()) {
            _isSpeaking.value = false
            return
        }
        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(speed)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aura_speech_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    /**
     * Start continuous, permanent listening that never stops even during prolonged idle periods.
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
        _isListening.value = true
        lastRecognizerEventTimestamp = System.currentTimeMillis()

        mainHandler.post {
            createAndStartRecognizer()
        }
    }

    private fun createAndStartRecognizer() {
        if (!isContinuousListeningMode) return

        try {
            if (speechRecognizer == null) {
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
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra("android.speech.extra.DICTATION_MODE", true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                // Generous silence buffers to avoid cutting off natural speaking pauses
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            }

            silenceChimesTemporarily()
            isRecognizerBusyOrStarting = true
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            lastRecognizerEventTimestamp = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("VoiceSpeechEngine", "Error starting speech recognizer", e)
            recreateRecognizerInstance()
        }
    }

    private inner class AuraRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            isRecognizerBusyOrStarting = false
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

            if (!isContinuousListeningMode) return

            // Normal idle timeouts or no-speech are NOT fatal errors.
            // Instantly loop back and keep listening without annoying notifications or stops.
            when (error) {
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    restartRecognizerImmediately(delayMs = 40)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT -> {
                    // Reset and recreate cleanly
                    recreateRecognizerInstance()
                }
                SpeechRecognizer.ERROR_AUDIO,
                SpeechRecognizer.ERROR_SERVER,
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    restartRecognizerImmediately(delayMs = 250)
                }
                else -> {
                    restartRecognizerImmediately(delayMs = 150)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            lastRecognizerEventTimestamp = System.currentTimeMillis()
            isRecognizerBusyOrStarting = false

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _lastRecognizedText.value = text
                val copyBuffer = synchronized(recentRmsBuffer) { recentRmsBuffer.toList() }
                onResultCallback?.invoke(text, copyBuffer)
            }

            if (isContinuousListeningMode) {
                restartRecognizerImmediately(delayMs = 60)
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

    private fun restartRecognizerImmediately(delayMs: Long) {
        if (!isContinuousListeningMode) return
        mainHandler.postDelayed({
            if (!isContinuousListeningMode) return@postDelayed
            try {
                val effective = LocalizationManager.getEffectiveLanguage(currentLanguage)
                val langTag = if (effective == "ar") "ar-SA" else "en-US"
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                }
                silenceChimesTemporarily()
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                lastRecognizerEventTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
                recreateRecognizerInstance()
            }
        }, delayMs)
    }

    private fun recreateRecognizerInstance() {
        if (!isContinuousListeningMode) return
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // ignore
            }
            speechRecognizer = null
            mainHandler.postDelayed({
                if (isContinuousListeningMode) {
                    createAndStartRecognizer()
                }
            }, 100)
        }
    }

    /**
     * Active Watchdog Coroutine:
     * Checks recognizer state every 4 seconds. If the recognizer stalled or stopped
     * during long periods of silence or OS idle state, it heals and restarts it automatically.
     */
    private fun startWatchdogEngine() {
        watchdogJob?.cancel()
        watchdogJob = engineScope.launch {
            while (isActive) {
                delay(4000)
                if (isContinuousListeningMode) {
                    val now = System.currentTimeMillis()
                    // If no recognizer lifecycle event occurred for > 8 seconds, it may have hung or been put to sleep
                    if (now - lastRecognizerEventTimestamp > 8000) {
                        Log.d("VoiceSpeechEngine", "Watchdog triggered: Self-healing speech recognizer stream")
                        mainHandler.post {
                            recreateRecognizerInstance()
                        }
                    }
                }
            }
        }
    }

    private fun silenceChimesTemporarily() {
        if (isMuteMicBleepsAndSystemSounds || isAllSoundsMuted) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                    audioManager?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun stopListening() {
        isContinuousListeningMode = false
        _isListening.value = false

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

