package com.redautoalert.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.redautoalert.service.AlertNotificationListener

/**
 * Handles checking and requesting the Notification Access permission.
 */
object PermissionHelper {

    /**
     * Checks if our NotificationListenerService has been granted notification access.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, AlertNotificationListener::class.java)
        val enabledListeners =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?: return false

        return enabledListeners.contains(componentName.flattenToString())
    }

    /**
     * Opens the system Notification Access settings page.
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * On Android 13+, sideloaded apps need "restricted settings" to be enabled
     * before notification listener access can be granted.
     */
    fun openAppInfoSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Checks if we need to guide the user through Android 13+ restricted settings.
     */
    fun needsRestrictedSettingsGuidance(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Checks if POST_NOTIFICATIONS permission is needed (Android 13+).
     */
    fun needsPostNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
