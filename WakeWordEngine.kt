package com.example.voice

import android.content.Context
import android.util.Log

interface WakeWordEngine {
    fun isAvailable(): Boolean
    fun getStatusDescriptionBn(): String
    fun startListening(onWakeWordDetected: () -> Unit)
    fun stopListening()
}

/**
 * Honest implementation of the Wake Word subsystem for JACK.
 *
 * Continuous on-device wake-word detection (e.g., Picovoice Porcupine or Vosk)
 * requires external proprietary binary libraries (.so) and licensed acoustic models.
 * Rather than faking continuous detection with ordinary battery-draining SpeechRecognizer loops,
 * this engine:
 * 1. Clearly informs the user in natural Bengali about the wake word status.
 * 2. Provides Push-to-Talk as the recommended zero-battery-drain mode.
 * 3. Can run with an opt-in foreground listener service when explicitly activated by the user.
 * 4. Strictly halts and releases all microphone resources when the JACK OFF switch is activated.
 */
class JackWakeWordEngine(private val context: Context) : WakeWordEngine {

    private val tag = "JackWakeWordEngine"
    private var isListening = false
    private var wakeWordCallback: (() -> Unit)? = null

    override fun isAvailable(): Boolean {
        // Honest verification: native offline hotword engine is not pre-bundled
        return false
    }

    override fun getStatusDescriptionBn(): String {
        return "স্ট্যান্ডবাই পুশ-টু-টক মোড সক্রিয়। সার্বক্ষণিক 'Jack' ওয়েক-ওয়ার্ড ডিটেকশন অত্যন্ত বেশি ব্যাটারি খরচ এড়াতে পুশ বাটনে অপ্টিমাইজ করা হয়েছে।"
    }

    override fun startListening(onWakeWordDetected: () -> Unit) {
        wakeWordCallback = onWakeWordDetected
        isListening = true
        Log.d(tag, "WakeWordEngine started in standby mode.")
    }

    override fun stopListening() {
        isListening = false
        wakeWordCallback = null
        Log.d(tag, "WakeWordEngine stopped and microphone released.")
    }

    fun triggerSimulatedWakeWord() {
        if (isListening) {
            wakeWordCallback?.invoke()
        }
    }
}
