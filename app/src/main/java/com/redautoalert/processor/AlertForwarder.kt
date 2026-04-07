package com.redautoalert.processor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.redautoalert.R
import com.redautoalert.model.AlertEvent
import com.redautoalert.model.AlertProcessor
import com.redautoalert.service.AlertDismissReceiver
import com.redautoalert.ui.SettingsActivity
import com.redautoalert.util.PrefsManager

/**
 * Forwards alerts as MessagingStyle notifications so Android Auto displays them.
 * Uses CarAppExtender for car-optimized notification handling.
 */
class AlertForwarder(private val context: Context) : AlertProcessor {

    companion object {
        const val CHANNEL_ID = "red_auto_alert_channel"
        const val CHANNEL_NAME = "Red Alert Notifications"
        private const val SILENT_CHANNEL_ID = "red_auto_alert_channel_silent"
        private const val SILENT_CHANNEL_NAME = "Red Alert (Android Auto Only)"
        private const val NOTIFICATION_GROUP = "red_alert_group"
        private var notificationCounter = 100
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = PrefsManager(context)
    private val handler = Handler(Looper.getMainLooper())

    private val alertPerson = Person.Builder()
        .setName("🚨 Red Alert")
        .setImportant(true)
        .build()

    init {
        createNotificationChannel()
        createSilentNotificationChannel()
    }

    override fun onAlert(event: AlertEvent) {
        val isPhoneNotificationEnabled = prefs.isPhoneNotificationEnabled
        val notificationId = notificationCounter++
        val notification = buildMessagingNotification(event, notificationId)
        notificationManager.notify(notificationId, notification)

        if (!isPhoneNotificationEnabled) {
            // Remove from phone after a delay long enough for Android Auto to have received
            // and rendered the notification.  Cancelling too quickly (e.g. 2 s) races with
            // the car head-unit's notification pipeline and the alert never appears on screen.
            handler.postDelayed({ notificationManager.cancel(notificationId) }, 5_000)
        }
    }

    override fun onAlertCleared(alertId: String) {
        // Auto-dismiss is handled by notification timeout
    }

    override fun isEnabled(): Boolean = prefs.isForwardingEnabled

    private fun buildMessagingNotification(
        event: AlertEvent,
        notificationId: Int
    ): android.app.Notification {
        val alertText = formatAlertText(event)

        val message = NotificationCompat.MessagingStyle.Message(
            alertText,
            event.timestamp,
            alertPerson
        )

        val messagingStyle = NotificationCompat.MessagingStyle(alertPerson)
            .setConversationTitle(event.title)
            .addMessage(message)

        // Intent to open settings when notification is tapped
        val contentIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, SettingsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reply action (required by Android Auto — without it notifications are silently ignored)
        val replyIntent = PendingIntent.getBroadcast(
            context, notificationId + 20000,
            Intent(AlertDismissReceiver.ACTION_REPLY)
                .setPackage(context.packageName)
                .putExtra(AlertDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val remoteInput = RemoteInput.Builder(AlertDismissReceiver.KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_alert, "Reply", replyIntent
        ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .addRemoteInput(remoteInput)
            .build()

        // Mark-as-read action (required by Android Auto for messaging notifications)
        val markReadIntent = PendingIntent.getBroadcast(
            context, notificationId + 10000,
            Intent(AlertDismissReceiver.ACTION_MARK_READ)
                .setPackage(context.packageName)
                .putExtra(AlertDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_alert, "OK", markReadIntent
        ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        val showOnPhone = prefs.isPhoneNotificationEnabled

        return NotificationCompat.Builder(context, if (showOnPhone) CHANNEL_ID else SILENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alert)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setTimeoutAfter(5 * 60 * 1000) // Auto-dismiss after 5 minutes
            .addInvisibleAction(replyAction)
            .addInvisibleAction(markReadAction)
            .extend(
                CarAppExtender.Builder()
                    .setImportance(NotificationManager.IMPORTANCE_HIGH)
                    .build()
            )
            .build()
    }

    private fun formatAlertText(event: AlertEvent): String {
        return extractAreas(event.text) ?: event.text
    }

    /**
     * Early-warning alerts ("התרעה מקדימה") follow this pattern:
     *   "בעקבות זיהוי שיגורים, בדקות הקרובות צפויות להתקבל התרעות באזורים X, Y, Z. לרשימת הישובים המלאה"
     *
     * Android Auto's notification preview only shows the first ~60 characters,
     * so the actual area names are truncated. This method extracts the area list
     * and places it at the start so drivers see the relevant info immediately.
     */
    internal fun extractAreas(text: String): String? {
        val marker = "באזורים"
        val markerIdx = text.indexOf(marker)
        if (markerIdx < 0) return null

        val afterMarker = text.substring(markerIdx + marker.length).trimStart()

        // Strip the trailing "לרשימת הישובים המלאה" suffix if present
        val suffix = "לרשימת הישובים המלאה"
        val suffixIdx = afterMarker.indexOf(suffix)
        val areas = if (suffixIdx >= 0) {
            afterMarker.substring(0, suffixIdx).trimEnd('.', ' ', ',')
        } else {
            afterMarker.trimEnd('.', ' ', ',')
        }

        return areas.ifBlank { null }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Red Alert safety notifications forwarded to Android Auto"
            enableVibration(true)
            setShowBadge(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createSilentNotificationChannel() {
        val channel = NotificationChannel(
            SILENT_CHANNEL_ID,
            SILENT_CHANNEL_NAME,
            // Must stay IMPORTANCE_HIGH — Android Auto ignores lower-importance channels.
            // Phone-side noise is suppressed via setSound/enableVibration below,
            // and the notification is cancelled shortly after posting (see onAlert).
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Silent carrier for Android Auto-only Red Alert notifications"
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
            setBypassDnd(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
        }
        notificationManager.createNotificationChannel(channel)
    }
}
