package com.redautoalert.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.redautoalert.RedAutoAlertApp
import com.redautoalert.model.AlertEvent
import com.redautoalert.processor.AlertForwarder
import com.redautoalert.processor.TtsAlertAnnouncer
import com.redautoalert.util.PrefsManager

/**
 * Listens for notifications from the Red Alert app and forwards them
 * through the AlertEventBus to all registered processors.
 */
class AlertNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "AlertListener"

        // Red Alert app packages to monitor
        private val MONITORED_PACKAGES = setOf(
            "com.redalert.tzevaadom",  // Tzofar Red Alert
            "com.red.alert",           // RedAlert alternate
        )

        // Track recent alerts to avoid duplicates
        private val recentAlertIds = LinkedHashSet<String>()
        private const val MAX_RECENT = 50
    }

    private lateinit var prefs: PrefsManager

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)

        // Ensure processors are registered. Application.onCreate() should have done this,
        // but if the system restarted us in an unusual way, register as fallback.
        if (application !is RedAutoAlertApp) {
            Log.w(TAG, "Application not initialized, registering processors as fallback")
            AlertEventBus.registerProcessor(AlertForwarder(this))
            AlertEventBus.registerProcessor(TtsAlertAnnouncer(this))
        }

        Log.i(TAG, "AlertNotificationListener started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (sbn.packageName !in MONITORED_PACKAGES) return
        if (!prefs.isForwardingEnabled) return

        Log.d(TAG, "Red Alert notification from: ${sbn.packageName}")

        val event = parseAlertEvent(sbn) ?: return

        // Deduplicate
        val dedupeKey = "${event.title}|${event.text}|${event.timestamp / 5000}"
        if (dedupeKey in recentAlertIds) {
            Log.d(TAG, "Duplicate alert, skipping: $dedupeKey")
            return
        }
        recentAlertIds.add(dedupeKey)
        if (recentAlertIds.size > MAX_RECENT) {
            recentAlertIds.iterator().let { it.next(); it.remove() }
        }

        Log.i(TAG, "Forwarding alert: ${event.title} - ${event.text}")
        AlertEventBus.emitBlocking(event)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName !in MONITORED_PACKAGES) return

        AlertEventBus.clearAlert(sbn.key)
    }

    private fun parseAlertEvent(sbn: StatusBarNotification): AlertEvent? {
        val extras = sbn.notification.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getString(Notification.EXTRA_TITLE)
            ?: ""

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getString(Notification.EXTRA_TEXT)
            ?: ""

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // Use the longest text available for full alert info
        val fullText = listOf(text, bigText, subText)
            .maxByOrNull { it.length }
            ?.takeIf { it.isNotBlank() } ?: return null

        // Parse cities from alert text (typically comma-separated Hebrew city names)
        val cities = parseCities(fullText)

        // Determine alert type
        val combinedText = "$title $fullText"
        val alertType = AlertEvent.AlertType.fromText(combinedText)

        return AlertEvent(
            id = sbn.key,
            title = title.ifBlank { "🚨 Red Alert" },
            text = fullText,
            cities = cities,
            alertType = alertType,
            timestamp = sbn.postTime,
            sourcePackage = sbn.packageName
        )
    }

    private fun parseCities(text: String): List<String> {
        // Red Alert typically formats as "City1, City2, City3" or "Alert in: City1, City2"
        val cleanText = text
            .replace("צבע אדום", "")
            .replace("Red Alert", "")
            .replace("התרעה", "")
            .replace("in:", "")
            .replace("ב:", "")
            .trim()

        return cleanText.split(",", "،", "、")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
