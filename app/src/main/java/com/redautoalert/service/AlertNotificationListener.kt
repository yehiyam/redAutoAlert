package com.redautoalert.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.redautoalert.RedAutoAlertApp
import com.redautoalert.model.AlertEvent
import com.redautoalert.processor.AlertForwarder
import com.redautoalert.util.DebugLog
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

    // Cached parsed filter patterns — invalidated when the stored filter string changes
    private var lastIncludeFilter: String? = null
    private var cachedIncludePatterns: List<String> = emptyList()
    private var lastExcludeFilter: String? = null
    private var cachedExcludePatterns: List<String> = emptyList()

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)

        // Ensure processors are registered. Application.onCreate() should have done this,
        // but if the system restarted us in an unusual way, register as fallback.
        if (application !is RedAutoAlertApp) {
            Log.w(TAG, "Application not initialized, registering processors as fallback")
            AlertEventBus.registerProcessor(AlertForwarder(this))
        }

        Log.i(TAG, "AlertNotificationListener started")
        DebugLog.log("NotificationListener started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (sbn.packageName !in MONITORED_PACKAGES) return
        if (!prefs.isForwardingEnabled) {
            DebugLog.log("Ignored (forwarding disabled): ${sbn.packageName}")
            return
        }

        Log.d(TAG, "Red Alert notification from: ${sbn.packageName}")
        DebugLog.log("Notification from: ${sbn.packageName}")

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

        // Apply include/exclude text filters
        if (!matchesFilters(event)) {
            Log.d(TAG, "Alert filtered by text rules: ${event.title} - ${event.text}")
            DebugLog.log("Alert filtered (text rules): ${event.text}")
            return
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

    /**
     * Returns true if the alert passes the include/exclude text filters stored in preferences.
     *
     * - Include filter: if non-empty, at least one pattern must be present in the alert text.
     * - Exclude filter: if non-empty, none of the patterns may be present in the alert text.
     * Both comparisons are case-insensitive; patterns are comma-separated.
     * Parsed patterns are cached and only re-parsed when the stored filter string changes.
     */
    private fun matchesFilters(event: AlertEvent): Boolean {
        val searchText = "${event.title} ${event.text}".lowercase()

        val includePatterns = getIncludePatterns()
        if (includePatterns.isNotEmpty() && includePatterns.none { it in searchText }) return false

        val excludePatterns = getExcludePatterns()
        if (excludePatterns.any { it in searchText }) return false

        return true
    }

    private fun getIncludePatterns(): List<String> {
        val filter = prefs.includeFilter.trim()
        if (filter != lastIncludeFilter) {
            lastIncludeFilter = filter
            cachedIncludePatterns = if (filter.isEmpty()) emptyList()
            else filter.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        }
        return cachedIncludePatterns
    }

    private fun getExcludePatterns(): List<String> {
        val filter = prefs.excludeFilter.trim()
        if (filter != lastExcludeFilter) {
            lastExcludeFilter = filter
            cachedExcludePatterns = if (filter.isEmpty()) emptyList()
            else filter.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        }
        return cachedExcludePatterns
    }
}
