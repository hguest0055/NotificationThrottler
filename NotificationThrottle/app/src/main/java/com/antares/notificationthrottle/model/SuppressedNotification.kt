package com.antares.notificationthrottle.model

data class SuppressedNotification(
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val reason: String
)
