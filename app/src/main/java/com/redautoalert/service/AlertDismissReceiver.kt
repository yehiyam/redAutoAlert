package com.redautoalert.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.redautoalert.util.DebugLog

/**
 * Handles mark-as-read and reply actions from Android Auto messaging notifications.
 *
 * Android Auto requires messaging-style notifications to provide BOTH a working
 * SEMANTIC_ACTION_MARK_AS_READ and SEMANTIC_ACTION_REPLY PendingIntent. Without
 * both actions the car head-unit silently refuses to display the notification.
 *
 * Since this is a safety-alert app (not a messaging app), the reply action simply
 * dismisses the notification — there is nothing meaningful to "reply" to.
 */
class AlertDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.redautoalert.ACTION_MARK_READ"
        const val ACTION_REPLY = "com.redautoalert.ACTION_REPLY"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
        private const val TAG = "AlertDismissReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_MARK_READ -> {
                Log.d(TAG, "Alert marked as read from Android Auto (id=$notificationId)")
                DebugLog.log("Alert marked as read from Android Auto (id=$notificationId)")
            }
            ACTION_REPLY -> {
                val reply = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_TEXT_REPLY)
                Log.d(TAG, "Reply received from Android Auto (id=$notificationId): $reply")
                DebugLog.log("Reply received from Android Auto (id=$notificationId)")
            }
            else -> return
        }

        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId)
        }
    }
}
