package com.redautoalert

import android.app.Application
import android.util.Log
import com.redautoalert.processor.AlertForwarder
import com.redautoalert.processor.TtsAlertAnnouncer
import com.redautoalert.service.AlertEventBus

/**
 * Application class that initializes alert processors at process startup.
 * This ensures processors are registered regardless of whether the Activity
 * or NotificationListenerService starts first.
 */
class RedAutoAlertApp : Application() {

    companion object {
        private const val TAG = "RedAutoAlertApp"
    }

    lateinit var alertForwarder: AlertForwarder
        private set
    lateinit var ttsAnnouncer: TtsAlertAnnouncer
        private set

    override fun onCreate() {
        super.onCreate()

        alertForwarder = AlertForwarder(this)
        ttsAnnouncer = TtsAlertAnnouncer(this)

        AlertEventBus.registerProcessor(alertForwarder)
        AlertEventBus.registerProcessor(ttsAnnouncer)

        Log.i(TAG, "Processors registered")
    }
}
