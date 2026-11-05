package com.ali.agent.system

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AliVoiceManager(
    private val context: Context,
    private val onFinalSpeechResult: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _livePartialTranscript = MutableStateFlow("")
    val livePartialTranscript: StateFlow<String> = _livePartialTranscript.asStateFlow()

    private val _voiceOutputEnabled = MutableStateFlow(true)
    val voiceOutputEnabled: StateFlow<Boolean> = _voiceOutputEnabled.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            // Select the highest-quality local neural voice available on the Android device
            try {
                val voices = tts?.voices
                val preferredVoice = voices?.firstOrNull { v ->
                    v.locale.language == "en" &&
                            !v.isNetworkConnectionRequired &&
                            (v.name.contains("natural", ignoreCase = true) ||
                                    v.name.contains("local", ignoreCase = true) ||
                                    v.quality >= android.speech.tts.Voice.QUALITY_HIGH)
                }
                if (preferredVoice != null) {
                    tts?.voice = preferredVoice
                }
            } catch (_: Exception) {
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            isTtsReady = true
        }
    }

    fun toggleVoiceOutput(): Boolean {
        val next = !_voiceOutputEnabled.value
        _voiceOutputEnabled.value = next
        if (!next) {
            stopSpeaking()
        }
        return next
    }

    /**
     * Speaks ALI's response with human-like prosody (adaptive pitch & cadence based on emotional state).
     */
    fun speakHumanLike(rawText: String, emotion: String = "Warm & Curious") {
        if (!_voiceOutputEnabled.value || !isTtsReady) return

        val cleanSpeechText = rawText
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`(.*?)`"), "$1")
            .replace("•", ",")
            .replace("-•", "")
            .replace(Regex("\\n+"), ". ")
            .trim()

        // Adjust vocal cadence & warmth to match emotional state
        when {
            emotion.contains("Empathetic", ignoreCase = true) ||
                    emotion.contains("Calm", ignoreCase = true) -> {
                tts?.setPitch(0.95f)
                tts?.setSpeechRate(0.94f)
            }
            emotion.contains("Excited", ignoreCase = true) ||
                    emotion.contains("Playful", ignoreCase = true) -> {
                tts?.setPitch(1.06f)
                tts?.setSpeechRate(1.04f)
            }
            emotion.contains("Analytical", ignoreCase = true) ||
                    emotion.contains("Focused", ignoreCase = true) -> {
                tts?.setPitch(0.98f)
                tts?.setSpeechRate(0.99f)
            }
            else -> {
                // Warm natural human baseline
                tts?.setPitch(1.01f)
                tts?.setSpeechRate(0.98f)
            }
        }

        tts?.speak(
            cleanSpeechText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "ali_utt_${System.currentTimeMillis()}"
        )
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening(preferOffline: Boolean = true) {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _livePartialTranscript.value = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _livePartialTranscript.value = ""
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _livePartialTranscript.value = ""
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()?.trim().orEmpty()
                        if (spoken.isNotBlank()) {
                            onFinalSpeechResult(spoken)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (partial.isNotBlank()) {
                            _livePartialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        _livePartialTranscript.value = ""
    }

    fun release() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
