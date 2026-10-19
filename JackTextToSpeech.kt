package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class JackTextToSpeech(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val tag = "JackTextToSpeech"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _hasBengaliVoice = MutableStateFlow(false)
    val hasBengaliVoice: StateFlow<Boolean> = _hasBengaliVoice.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true

            // Prefer bn-BD (Bangladesh), fallback to bn-IN (India)
            var locale = Locale.forLanguageTag("bn-BD")
            var result = tts?.isLanguageAvailable(locale)

            if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                locale = Locale.forLanguageTag("bn-IN")
                result = tts?.isLanguageAvailable(locale)
            }

            if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                locale = Locale.forLanguageTag("bn")
                result = tts?.isLanguageAvailable(locale)
            }

            if (result != TextToSpeech.LANG_NOT_SUPPORTED && result != TextToSpeech.LANG_MISSING_DATA) {
                tts?.language = locale
                _hasBengaliVoice.value = true
                Log.d(tag, "Bengali TTS voice initialized with locale: $locale")
            } else {
                _hasBengaliVoice.value = false
                Log.w(tag, "Bengali TTS voice is not available on device. Using system default.")
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

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.e(tag, "TTS Utterance error: $errorCode")
                }
            })

            onInitComplete(true)
        } else {
            isInitialized = false
            onInitComplete(false)
            Log.e(tag, "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
        onDone: () -> Unit = {}
    ) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS is not ready to speak.")
            onDone()
            return
        }

        try {
            stop()
            tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
            tts?.setPitch(speechPitch.coerceIn(0.5f, 2.0f))

            val utteranceId = "jack_utterance_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            // Strip markdown bold or brackets for cleaner speech
            val cleanText = text
                .replace(Regex("""[*_#`~\[\]]"""), "")
                .replace(Regex("""https?://\S+"""), "ওয়েবসাইট লিংক")

            _isSpeaking.value = true
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e(tag, "Error during TTS playback", e)
            _isSpeaking.value = false
            onDone()
        }
    }

    fun stop() {
        try {
            if (_isSpeaking.value) {
                tts?.stop()
                _isSpeaking.value = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }
}
