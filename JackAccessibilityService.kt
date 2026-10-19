package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Optional Accessibility Service for JACK AI.
 * Allows legitimate automation assist when explicitly enabled by user.
 * Strictly adheres to privacy: does not steal keystrokes, passwords, or bypass security prompts.
 */
class JackAccessibilityService : AccessibilityService() {

    private val tag = "JackAccessibilityService"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(tag, "JACK Accessibility Service Connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing for authorized screen reading when explicitly requested
    }

    override fun onInterrupt() {
        Log.w(tag, "JACK Accessibility Service Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "JACK Accessibility Service Destroyed.")
    }
}
