package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechEvent {
    object Ready : SpeechEvent()
    data class RmsChanged(val normalizedRms: Float) : SpeechEvent()
    data class Result(val text: String) : SpeechEvent()
    data class PartialResult(val text: String) : SpeechEvent()
    data class Error(val messageBn: String) : SpeechEvent()
    object EndOfSpeech : SpeechEvent()
}

class JackSpeechRecognizer(private val context: Context) {

    private val tag = "JackSpeechRecognizer"
    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechEvents = MutableStateFlow<SpeechEvent?>(null)
    val speechEvents: StateFlow<SpeechEvent?> = _speechEvents.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(localeTag: String = "bn-BD") {
        stopListening()

        if (!isRecognitionAvailable()) {
            _speechEvents.value = SpeechEvent.Error("এই ডিভাইসে ভয়েস রিকগনিশন সার্ভিস উপলব্ধ নেই।")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _speechEvents.value = SpeechEvent.Ready
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB to 10dB into 0.0 to 1.0 range
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioLevel.value = normalized
                        _speechEvents.value = SpeechEvent.RmsChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _audioLevel.value = 0f
                        _speechEvents.value = SpeechEvent.EndOfSpeech
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioLevel.value = 0f
                        val errorBn = mapErrorCodeToBengali(error)
                        Log.w(tag, "SpeechRecognizer error: $error ($errorBn)")
                        _speechEvents.value = SpeechEvent.Error(errorBn)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            _speechEvents.value = SpeechEvent.Result(text)
                        } else {
                            _speechEvents.value = SpeechEvent.Error("কোনো কথা শোনা যায়নি।")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            _speechEvents.value = SpeechEvent.PartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error starting speech recognition", e)
            _isListening.value = false
            _speechEvents.value = SpeechEvent.Error("ভয়েস ইনপুট চালু করতে সমস্যা হয়েছে: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            _isListening.value = false
            _audioLevel.value = 0f
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping speech recognition", e)
        }
    }

    private fun mapErrorCodeToBengali(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিংয়ে ত্রুটি হয়েছে।"
            SpeechRecognizer.ERROR_CLIENT -> "ক্লায়েন্ট সম্পর্কিত ত্রুটি হয়েছে।"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন ব্যবহারের অনুমতি নেই।"
            SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক সংযোগে সমস্যা হয়েছে।"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্কের সময়সীমা শেষ হয়েছে।"
            SpeechRecognizer.ERROR_NO_MATCH -> "স্পষ্ট কোনো কথা শোনা যায়নি। আবার বলুন।"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস সার্ভিস ব্যস্ত রয়েছে।"
            SpeechRecognizer.ERROR_SERVER -> "সার্ভার থেকে ত্রুটি হয়েছে।"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি।"
            else -> "ভয়েস সনাক্তকরণে সমস্যা হয়েছে।"
        }
    }
}
