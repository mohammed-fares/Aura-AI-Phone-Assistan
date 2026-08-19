package com.example.system

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceSpeechEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsAudioLevel = MutableStateFlow(0f)
    val rmsAudioLevel: StateFlow<Float> = _rmsAudioLevel.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    // Recent RMS buffer for biometric voiceprint analysis
    val recentRmsBuffer = mutableListOf<Float>()

    private var isContinuousListeningMode = false
    private var onResultCallback: ((String, List<Float>) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val arabic = Locale("ar")
                val result = textToSpeech?.setLanguage(arabic)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.getDefault())
                }
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        // Resume continuous listening if active
                        if (isContinuousListeningMode) {
                            mainHandler.postDelayed({ restartListeningInternal() }, 400)
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        if (isContinuousListeningMode) {
                            mainHandler.postDelayed({ restartListeningInternal() }, 400)
                        }
                    }
                })
            }
        }
    }

    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
        if (!isTtsReady || text.isBlank()) return
        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(speed)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aura_utterance_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun startContinuousListening(
        onResult: (String, List<Float>) -> Unit,
        onError: (String) -> Unit
    ) {
        isContinuousListeningMode = true
        onResultCallback = onResult
        onErrorCallback = onError
        restartListeningInternal()
    }

    private fun restartListeningInternal() {
        if (!isContinuousListeningMode) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorCallback?.invoke("محرك التعرف على الصوت غير متوفر في هذا النظام.")
            return
        }

        stopListeningQuietly()
        synchronized(recentRmsBuffer) {
            recentRmsBuffer.clear()
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }
                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }
                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
                    _rmsAudioLevel.value = normalized
                    synchronized(recentRmsBuffer) {
                        if (recentRmsBuffer.size > 50) recentRmsBuffer.removeAt(0)
                        recentRmsBuffer.add(normalized)
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _rmsAudioLevel.value = 0f
                }
                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsAudioLevel.value = 0f
                    // In continuous mode, re-arm automatically after brief pause
                    if (isContinuousListeningMode && !_isSpeaking.value) {
                        mainHandler.postDelayed({
                            restartListeningInternal()
                        }, 800)
                    }
                }
                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsAudioLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _lastRecognizedText.value = text
                        val copyBuffer = synchronized(recentRmsBuffer) { recentRmsBuffer.toList() }
                        onResultCallback?.invoke(text, copyBuffer)
                    }
                    // Re-arm listening loop
                    if (isContinuousListeningMode && !_isSpeaking.value) {
                        mainHandler.postDelayed({
                            restartListeningInternal()
                        }, 600)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { _lastRecognizedText.value = it }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("VoiceSpeechEngine", "startListening error", e)
            _isListening.value = false
            if (isContinuousListeningMode) {
                mainHandler.postDelayed({ restartListeningInternal() }, 1500)
            }
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        startContinuousListening(
            onResult = { text, _ -> onResult(text) },
            onError = onError
        )
    }

    private fun stopListeningQuietly() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        speechRecognizer = null
        _isListening.value = false
        _rmsAudioLevel.value = 0f
    }

    fun stopListening() {
        isContinuousListeningMode = false
        stopListeningQuietly()
    }

    fun destroy() {
        isContinuousListeningMode = false
        stopListeningQuietly()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
