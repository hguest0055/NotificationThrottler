package com.antares.notificationthrottle.model

enum class TimeWindow(val minutes: Int, val label: String) {
    ONE_MIN(1, "1 minute"),
    FIVE_MIN(5, "5 minutes"),
    TEN_MIN(10, "10 minutes"),
    FIFTEEN_MIN(15, "15 minutes"),
    THIRTY_MIN(30, "30 minutes"),
    FORTY_FIVE_MIN(45, "45 minutes"),
    ONE_HOUR(60, "1 hour");

    companion object {
        fun fromMinutes(minutes: Int): TimeWindow {
            return values().find { it.minutes == minutes } ?: TEN_MIN
        }

        fun labels(): Array<String> = values().map { it.label }.toTypedArray()
        fun minuteValues(): IntArray = values().map { it.minutes }.toIntArray()
    }
}
