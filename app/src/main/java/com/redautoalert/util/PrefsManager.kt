package com.redautoalert.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages app preferences using SharedPreferences.
 */
class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "red_auto_alert_prefs"
        private const val KEY_FORWARDING_ENABLED = "forwarding_enabled"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_LANGUAGE = "tts_language"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()

    var isTtsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var ttsLanguage: String
        get() = prefs.getString(KEY_TTS_LANGUAGE, "he") ?: "he"
        set(value) = prefs.edit().putString(KEY_TTS_LANGUAGE, value).apply()
}
