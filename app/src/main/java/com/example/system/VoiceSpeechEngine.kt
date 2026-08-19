package com.example.system

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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
    private var consecutiveRecognizerErrors = 0

    // AudioRecord Continuous Engine (Hardware non-stop stream)
    private var audioRecordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val isAudioRecordRunning = MutableStateFlow(false)

    // Recent RMS buffer for biometric voiceprint analysis
    val recentRmsBuffer = mutableListOf<Float>()

    init {
        initTts()
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
     * Start continuous, never-ending listening that keeps the microphone active
     * without stopping even during prolonged periods of phone inactivity.
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

        startContinuousAudioStream()
    }

    private fun startContinuousAudioStream() {
        if (!isContinuousListeningMode) return

        val isSystemSpeechAvailable = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            false
        }

        if (!isSystemSpeechAvailable) {
            startPersistentAudioRecordEngine()
            return
        }

        val effective = LocalizationManager.getEffectiveLanguage(currentLanguage)
        _isListening.value = true

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                _isListening.value = true
                                _isUsingFallbackAcousticEngine.value = false
                                consecutiveRecognizerErrors = 0
                            }

                            override fun onBeginningOfSpeech() {
                                _isListening.value = true
                            }

                            override fun onRmsChanged(rmsdB: Float) {
                                val normalized = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
                                _rmsAudioLevel.value = normalized
                                synchronized(recentRmsBuffer) {
                                    if (recentRmsBuffer.size > 60) recentRmsBuffer.removeAt(0)
                                    recentRmsBuffer.add(normalized)
                                }
                            }

                            override fun onBufferReceived(buffer: ByteArray?) {}

                            override fun onEndOfSpeech() {
                                if (isContinuousListeningMode) {
                                    _isListening.value = true
                                }
                            }

                            override fun onError(error: Int) {
                                consecutiveRecognizerErrors++
                                if (consecutiveRecognizerErrors >= 3 || error == SpeechRecognizer.ERROR_CLIENT || error == 9) {
                                    startPersistentAudioRecordEngine()
                                    return
                                }
                                if (isContinuousListeningMode) {
                                    _isListening.value = true
                                    restartRecognizerSafely()
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                consecutiveRecognizerErrors = 0
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull() ?: ""
                                if (text.isNotBlank()) {
                                    _lastRecognizedText.value = text
                                    val copyBuffer = synchronized(recentRmsBuffer) { recentRmsBuffer.toList() }
                                    onResultCallback?.invoke(text, copyBuffer)
                                }
                                if (isContinuousListeningMode) {
                                    _isListening.value = true
                                    restartRecognizerSafely()
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                matches?.firstOrNull()?.let { _lastRecognizedText.value = it }
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val langTag = if (effective == "ar") "ar-SA" else "en-US"
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }

                silenceChimesTemporarily()
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceSpeechEngine", "SpeechRecognizer failed, switching to persistent AudioRecord", e)
                startPersistentAudioRecordEngine()
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

    private fun restartRecognizerSafely() {
        if (!isContinuousListeningMode) return
        mainHandler.postDelayed({
            if (!isContinuousListeningMode) return@postDelayed
            try {
                val effective = LocalizationManager.getEffectiveLanguage(currentLanguage)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val langTag = if (effective == "ar") "ar-SA" else "en-US"
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }
                silenceChimesTemporarily()
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                startPersistentAudioRecordEngine()
            }
        }, 50)
    }

    /**
     * Self-contained persistent In-App Audio HAL engine using Android AudioRecord.
     * Keeps the hardware microphone open continuously 24/7 with zero timeouts,
     * zero open/close cycling, and zero system chimes/beeps.
     */
    private fun startPersistentAudioRecordEngine() {
        if (isAudioRecordRunning.value) return

        _isUsingFallbackAcousticEngine.value = true
        _isListening.value = true
        isAudioRecordRunning.value = true

        audioRecordJob?.cancel()
        audioRecordJob = engineScope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    _isListening.value = false
                    isAudioRecordRunning.value = false
                    return@launch
                }

                audioRecord?.startRecording()
                val audioBuffer = ShortArray(bufferSize / 2)
                var voiceFramesDetected = 0
                val energyThreshold = 180.0

                while (isActive && isContinuousListeningMode) {
                    val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readCount > 0) {
                        var sumSquare = 0.0
                        for (i in 0 until readCount) {
                            val sample = audioBuffer[i]
                            sumSquare += (sample * sample)
                        }
                        val rms = sqrt(sumSquare / readCount)
                        val normalized = (rms.toFloat() / 1500f).coerceIn(0f, 1f)
                        _rmsAudioLevel.value = normalized

                        synchronized(recentRmsBuffer) {
                            if (recentRmsBuffer.size > 60) recentRmsBuffer.removeAt(0)
                            recentRmsBuffer.add(normalized)
                        }

                        if (rms > energyThreshold) {
                            voiceFramesDetected++
                        } else {
                            if (voiceFramesDetected > 10) {
                                // Spoken phrase captured
                                val copyBuffer = synchronized(recentRmsBuffer) { recentRmsBuffer.toList() }
                                val effective = LocalizationManager.getEffectiveLanguage(currentLanguage)
                                val fallbackPhrase = if (effective == "ar") "أمر صوتي مباشر" else "Direct Voice Command"
                                onResultCallback?.invoke(fallbackPhrase, copyBuffer)
                            }
                            voiceFramesDetected = 0
                        }
                    }
                    delay(25)
                }
            } catch (e: Exception) {
                Log.e("VoiceSpeechEngine", "Error in persistent AudioRecord loop", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {
                    // ignore
                }
                audioRecord = null
                isAudioRecordRunning.value = false
                _isUsingFallbackAcousticEngine.value = false
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

        audioRecordJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
        isAudioRecordRunning.value = false
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
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
