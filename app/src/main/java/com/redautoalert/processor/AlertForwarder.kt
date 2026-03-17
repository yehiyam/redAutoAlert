package com.redautoalert.processor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.redautoalert.R
import com.redautoalert.model.AlertEvent
import com.redautoalert.model.AlertProcessor
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

    private val alertPerson = Person.Builder()
        .setName("🚨 Red Alert")
        .setImportant(true)
        .build()

    init {
        createNotificationChannel()
        createSilentNotificationChannel()
    }

    override fun onAlert(event: AlertEvent) {
        val notificationId = notificationCounter++
        val notification = buildMessagingNotification(event, notificationId)
        notificationManager.notify(notificationId, notification)
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

        // Mark-as-read action (required by Android Auto for messaging notifications)
        val markReadIntent = PendingIntent.getBroadcast(
            context, notificationId + 10000,
            Intent("com.redautoalert.ACTION_MARK_READ").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_alert, "OK", markReadIntent
        ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        return NotificationCompat.Builder(context, if (prefs.isPhoneNotificationEnabled) CHANNEL_ID else SILENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alert)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setTimeoutAfter(5 * 60 * 1000) // Auto-dismiss after 5 minutes
            .addAction(markReadAction)
            .extend(
                CarAppExtender.Builder()
                    .setImportance(NotificationManager.IMPORTANCE_HIGH)
                    .build()
            )
            .build()
    }

    private fun formatAlertText(event: AlertEvent): String {
        val typeEmoji = when (event.alertType) {
            AlertEvent.AlertType.ROCKET -> "🚀"
            AlertEvent.AlertType.DRONE -> "✈️"
            AlertEvent.AlertType.EARTHQUAKE -> "🌍"
            AlertEvent.AlertType.TSUNAMI -> "🌊"
            AlertEvent.AlertType.HAZARDOUS_MATERIALS -> "☢️"
            AlertEvent.AlertType.TERRORIST_INFILTRATION -> "⚠️"
            AlertEvent.AlertType.UNKNOWN -> "🚨"
        }

        return if (event.cities.isNotEmpty()) {
            "$typeEmoji ${event.cities.joinToString(", ")}"
        } else {
            "$typeEmoji ${event.text}"
        }
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
            // Must be at least IMPORTANCE_HIGH so Android Auto still shows the alert.
            // Phone-side noise is suppressed via setSound/enableVibration below.
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
