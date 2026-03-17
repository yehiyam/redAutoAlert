package com.redautoalert.model

/**
 * Represents a parsed alert from the Red Alert app.
 * Designed as the shared data model consumed by all AlertProcessor implementations.
 */
data class AlertEvent(
    val id: String,
    val title: String,
    val text: String,
    val cities: List<String>,
    val alertType: AlertType,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcePackage: String = ""
) {
    enum class AlertType {
        ROCKET,
        DRONE,
        EARTHQUAKE,
        TSUNAMI,
        HAZARDOUS_MATERIALS,
        TERRORIST_INFILTRATION,
        UNKNOWN;

        companion object {
            fun fromText(text: String): AlertType {
                val lower = text.lowercase()
                return when {
                    "רקט" in lower || "טיל" in lower || "rocket" in lower || "missile" in lower -> ROCKET
                    "כלי טיס" in lower || "drone" in lower || "uav" in lower -> DRONE
                    "רעידת אדמה" in lower || "earthquake" in lower -> EARTHQUAKE
                    "צונמי" in lower || "tsunami" in lower -> TSUNAMI
                    "חומרים מסוכנים" in lower || "hazardous" in lower -> HAZARDOUS_MATERIALS
                    "חדירה" in lower || "infiltration" in lower -> TERRORIST_INFILTRATION
                    else -> UNKNOWN
                }
            }
        }
    }
}
