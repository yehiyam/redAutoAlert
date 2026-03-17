package com.redautoalert.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.redautoalert.util.DebugLog

/**
 * Handles the mark-as-read action dispatched when the driver dismisses a Red Alert
 * notification from the Android Auto head unit.
 *
 * Android Auto requires messaging-style notifications to provide a working
 * SEMANTIC_ACTION_MARK_AS_READ PendingIntent; without a registered receiver the
 * action silently fails and Auto may decline to show the notification at all.
 */
class AlertDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.redautoalert.ACTION_MARK_READ"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "AlertDismissReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_READ) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        Log.d(TAG, "Alert marked as read from Android Auto (id=$notificationId)")
        DebugLog.log("Alert marked as read from Android Auto (id=$notificationId)")

        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId)
        }
    }
}
