package com.redautoalert.model

/**
 * Interface for alert consumers. Implementing classes process alerts in their own way
 * (e.g., notification forwarding, TTS, future car UI screen).
 * Designed for extensibility — Phase 2 will add CarScreenAlertProcessor.
 */
interface AlertProcessor {
    /** Called when a new alert is received */
    fun onAlert(event: AlertEvent)

    /** Called when an alert is cleared/dismissed */
    fun onAlertCleared(alertId: String) {}

    /** Whether this processor is currently enabled */
    fun isEnabled(): Boolean
}
