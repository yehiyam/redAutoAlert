package com.redautoalert.service

import com.redautoalert.model.AlertEvent
import com.redautoalert.model.AlertProcessor
import com.redautoalert.util.DebugLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Central event bus for distributing AlertEvents to all registered processors.
 * Uses Kotlin SharedFlow for reactive event distribution.
 * Phase 2 processors (Car App screen) can subscribe here without changing existing code.
 */
object AlertEventBus {

    private val _alerts = MutableSharedFlow<AlertEvent>(replay = 1, extraBufferCapacity = 10)
    val alerts: SharedFlow<AlertEvent> = _alerts.asSharedFlow()

    private val processors = mutableListOf<AlertProcessor>()

    fun registerProcessor(processor: AlertProcessor) {
        synchronized(processors) {
            if (processor !in processors) {
                processors.add(processor)
            }
        }
    }

    fun unregisterProcessor(processor: AlertProcessor) {
        synchronized(processors) {
            processors.remove(processor)
        }
    }

    suspend fun emit(event: AlertEvent) {
        _alerts.emit(event)
        synchronized(processors) {
            processors.filter { it.isEnabled() }.forEach { processor ->
                try {
                    processor.onAlert(event)
                } catch (e: Exception) {
                    android.util.Log.e("AlertEventBus", "Processor error: ${e.message}", e)
                }
            }
        }
    }

    fun emitBlocking(event: AlertEvent) {
        _alerts.tryEmit(event)
        DebugLog.log("Alert: ${event.title} — ${event.text}")
        synchronized(processors) {
            val enabled = processors.filter { it.isEnabled() }
            DebugLog.log("Dispatching to ${enabled.size}/${processors.size} processors")
            enabled.forEach { processor ->
                try {
                    processor.onAlert(event)
                    DebugLog.log("✓ ${processor.javaClass.simpleName}")
                } catch (e: Exception) {
                    DebugLog.log("✗ ${processor.javaClass.simpleName}: ${e.message}")
                    android.util.Log.e("AlertEventBus", "Processor error: ${e.message}", e)
                }
            }
        }
    }

    fun clearAlert(alertId: String) {
        synchronized(processors) {
            processors.filter { it.isEnabled() }.forEach { processor ->
                try {
                    processor.onAlertCleared(alertId)
                } catch (e: Exception) {
                    android.util.Log.e("AlertEventBus", "Processor clear error: ${e.message}", e)
                }
            }
        }
    }
}
