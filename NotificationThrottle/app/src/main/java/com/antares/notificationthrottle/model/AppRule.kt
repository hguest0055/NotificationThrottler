package com.antares.notificationthrottle.model

import com.antares.notificationthrottle.model.TimeWindow

data class AppRule(
    val packageName: String,
    val appName: String,
    val maxNotifications: Int = 2,
    val timeWindowMinutes: Int = TimeWindow.TEN_MIN.minutes,
    val enabled: Boolean = true
) {
    val timeWindowMs: Long get() = timeWindowMinutes * 60 * 1000L
}
