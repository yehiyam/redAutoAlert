package com.redautoalert.util

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

/**
 * Tracks whether Android Auto is currently connected using the CarConnection API.
 *
 * Call [start] once on the main thread (e.g. from Application.onCreate) to begin observing.
 * After that, [isConnected] always reflects the latest known connection state.
 *
 * The observer is tied to the application process lifetime. Call [stop] to detach it explicitly
 * if the tracker is no longer needed before process termination.
 */
class CarConnectionTracker(context: Context) {

    /** True when an Android Auto (projection or native) session is active. */
    @Volatile
    var isConnected: Boolean = false
        private set

    private val carConnection = CarConnection(context.applicationContext)

    /**
     * Raw connection type LiveData; use [isConnectedLiveData] for a boolean convenience stream.
     */
    val type: LiveData<Int> get() = carConnection.type

    /**
     * LiveData that emits `true` when Android Auto is connected, `false` otherwise.
     *
     * Built with [Transformations.map], so it only actively observes [type] while it has
     * active observers — no manual lifecycle management required.
     */
    val isConnectedLiveData: LiveData<Boolean> =
        Transformations.map(carConnection.type) { it != CarConnection.CONNECTION_TYPE_NOT_CONNECTED }

    private val observer = Observer<Int> { connectionType ->
        val connected = connectionType != CarConnection.CONNECTION_TYPE_NOT_CONNECTED
        if (connected != isConnected) {
            isConnected = connected
            DebugLog.log("Android Auto ${if (connected) "connected" else "disconnected"} (type=$connectionType)")
        }
    }

    /**
     * Begins observing the Android Auto connection state.
     * Must be called on the main thread.
     *
     * The observer is intentionally tied to the application process lifetime:
     * `Application.onTerminate()` is never invoked on production devices, so the process
     * being killed is the natural cleanup boundary. Call [stop] in tests or if the tracker
     * is discarded before process termination.
     */
    fun start() {
        carConnection.type.observeForever(observer)
    }

    /** Removes the [observeForever] observer. Safe to call even if [start] was never called. */
    fun stop() {
        carConnection.type.removeObserver(observer)
    }
}
