package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jack_settings")

class JackPreferences(private val context: Context) {

    companion object {
        val KEY_IS_JACK_ON = booleanPreferencesKey("is_jack_on")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_LOCALE = stringPreferencesKey("bengali_locale")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val isJackOn: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_JACK_ON] ?: true }
    val speechRate: Flow<Float> = context.dataStore.data.map { it[KEY_SPEECH_RATE] ?: 1.0f }
    val speechPitch: Flow<Float> = context.dataStore.data.map { it[KEY_SPEECH_PITCH] ?: 1.0f }
    val bengaliLocale: Flow<String> = context.dataStore.data.map { it[KEY_LOCALE] ?: "bn-BD" }
    val autoSpeak: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_SPEAK] ?: true }
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WAKE_WORD_ENABLED] ?: false }
    val customApiKey: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_API_KEY] ?: "" }
    val selectedModel: Flow<String> = context.dataStore.data.map { it[KEY_SELECTED_MODEL] ?: "gemini-3.5-flash" }
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_THEME] ?: true }

    suspend fun setJackOn(enabled: Boolean) {
        context.dataStore.edit { it[KEY_IS_JACK_ON] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[KEY_SPEECH_PITCH] = pitch }
    }

    suspend fun setBengaliLocale(locale: String) {
        context.dataStore.edit { it[KEY_LOCALE] = locale }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SPEAK] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { it[KEY_CUSTOM_API_KEY] = key.trim() }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { it[KEY_SELECTED_MODEL] = model }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = dark }
    }
}
