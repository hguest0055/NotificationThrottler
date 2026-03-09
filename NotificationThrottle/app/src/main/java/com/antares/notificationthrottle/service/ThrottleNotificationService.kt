package com.antares.notificationthrottle.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.antares.notificationthrottle.R
import com.antares.notificationthrottle.data.AppRuleRepository
import com.antares.notificationthrottle.data.SuppressedLogRepository
import com.antares.notificationthrottle.model.SuppressedNotification
import java.util.concurrent.ConcurrentHashMap

class ThrottleNotificationService : NotificationListenerService() {

    // packageName -> list of timestamps in current window
    private val notificationTimestamps = ConcurrentHashMap<String, MutableList<Long>>()

    private lateinit var repository: AppRuleRepository

    override fun onCreate() {
        super.onCreate()
        repository = AppRuleRepository.getInstance(applicationContext)
        createForegroundChannel()
        Log.d(TAG, "ThrottleNotificationService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        // Never suppress our own notifications or system UI
        if (pkg == applicationContext.packageName || pkg == "android") return

        val rule = repository.getRuleForPackage(pkg) ?: return
        if (!rule.enabled) return

        val now = System.currentTimeMillis()
        val windowMs = rule.timeWindowMs

        val timestamps = notificationTimestamps.getOrPut(pkg) {
            java.util.Collections.synchronizedList(mutableListOf())
        }

        synchronized(timestamps) {
            // Remove timestamps outside the current window
            timestamps.removeAll { now - it > windowMs }

            if (timestamps.size >= rule.maxNotifications) {
                // Suppress!
                cancelNotification(sbn.key)
                logSuppressed(sbn, pkg, rule.maxNotifications, rule.timeWindowMinutes)
                Log.d(TAG, "Suppressed notification from $pkg (${timestamps.size}/${rule.maxNotifications} in ${rule.timeWindowMinutes}min window)")
            } else {
                timestamps.add(now)
                Log.d(TAG, "Allowed notification from $pkg (${timestamps.size}/${rule.maxNotifications})")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No-op needed but good practice to override
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        notificationTimestamps.clear()
    }

    private fun logSuppressed(
        sbn: StatusBarNotification,
        pkg: String,
        maxAllowed: Int,
        windowMinutes: Int
    ) {
        val extras = sbn.notification?.extras
        val title = extras?.getString(Notification.EXTRA_TITLE)
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }

        SuppressedLogRepository.add(
            SuppressedNotification(
                packageName = pkg,
                appName = appName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis(),
                reason = "Exceeded $maxAllowed in ${windowMinutes}min"
            )
        )
    }

    private fun createForegroundChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Throttle Service",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Notification Throttle is active"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ThrottleService"
        const val CHANNEL_ID = "throttle_service_channel"
    }
}
