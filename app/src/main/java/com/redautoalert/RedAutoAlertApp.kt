package com.redautoalert

import android.app.Application
import android.util.Log
import com.redautoalert.processor.AlertForwarder
import com.redautoalert.service.AlertEventBus
import com.redautoalert.util.CarConnectionTracker

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

    lateinit var carConnectionTracker: CarConnectionTracker
        private set

    override fun onCreate() {
        super.onCreate()

        carConnectionTracker = CarConnectionTracker(this)
        carConnectionTracker.start()

        alertForwarder = AlertForwarder(this, carConnectionTracker)

        AlertEventBus.registerProcessor(alertForwarder)

        Log.i(TAG, "Processors registered")
    }
}
