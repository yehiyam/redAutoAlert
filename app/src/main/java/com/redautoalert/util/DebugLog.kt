package com.redautoalert.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple in-memory ring buffer for debug log entries displayed in the UI.
 */
object DebugLog {

    data class Entry(val timestamp: Long, val message: String) {
        fun formatted(): String {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            return "[$time] $message"
        }
    }

    private const val MAX_ENTRIES = 50
    private val entries = ArrayDeque<Entry>(MAX_ENTRIES)
    private val listeners = mutableListOf<() -> Unit>()

    fun log(message: String) {
        synchronized(entries) {
            if (entries.size >= MAX_ENTRIES) entries.removeFirst()
            entries.addLast(Entry(System.currentTimeMillis(), message))
        }
        synchronized(listeners) {
            listeners.forEach { it() }
        }
    }

    fun getEntries(): List<Entry> {
        synchronized(entries) {
            return entries.toList()
        }
    }

    fun addListener(listener: () -> Unit) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun removeListener(listener: () -> Unit) {
        synchronized(listeners) { listeners.remove(listener) }
    }
}
