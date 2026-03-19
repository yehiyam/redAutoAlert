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
        private const val KEY_PHONE_NOTIFICATION_ENABLED = "phone_notification_enabled"
        private const val KEY_INCLUDE_FILTER = "include_filter"
        private const val KEY_EXCLUDE_FILTER = "exclude_filter"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()

    var isPhoneNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_PHONE_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PHONE_NOTIFICATION_ENABLED, value).apply()

    var includeFilter: String
        get() = prefs.getString(KEY_INCLUDE_FILTER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_INCLUDE_FILTER, value).apply()

    var excludeFilter: String
        get() = prefs.getString(KEY_EXCLUDE_FILTER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EXCLUDE_FILTER, value).apply()
}
