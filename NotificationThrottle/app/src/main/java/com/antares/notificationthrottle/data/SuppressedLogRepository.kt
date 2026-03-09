package com.antares.notificationthrottle.data

import com.antares.notificationthrottle.model.SuppressedNotification
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory log of suppressed notifications.
 * Kept as a singleton so both the service and UI can access it.
 * Capped at MAX_ENTRIES to avoid unbounded growth.
 */
object SuppressedLogRepository {

    private const val MAX_ENTRIES = 200

    private val _log = CopyOnWriteArrayList<SuppressedNotification>()

    val log: List<SuppressedNotification> get() = _log.toList()

    fun add(entry: SuppressedNotification) {
        _log.add(0, entry) // newest first
        if (_log.size > MAX_ENTRIES) {
            _log.removeAt(_log.lastIndex)
        }
        listeners.forEach { it() }
    }

    fun clear() {
        _log.clear()
        listeners.forEach { it() }
    }

    // Simple observer pattern so UI can react without LiveData dependency in the repo
    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}
