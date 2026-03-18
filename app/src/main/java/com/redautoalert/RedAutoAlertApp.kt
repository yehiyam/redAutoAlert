package com.redautoalert

import android.app.Application
import android.util.Log
import com.redautoalert.processor.AlertForwarder
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

    override fun onCreate() {
        super.onCreate()

        alertForwarder = AlertForwarder(this)

        AlertEventBus.registerProcessor(alertForwarder)

        Log.i(TAG, "Processors registered")
    }
}
