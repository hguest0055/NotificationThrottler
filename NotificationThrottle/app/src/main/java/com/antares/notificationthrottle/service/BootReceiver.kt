package com.antares.notificationthrottle.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Ensures the app stays in the notification listener list after reboot.
 * The system handles re-binding the NotificationListenerService automatically,
 * but this receiver can be used to trigger any re-initialization if needed.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed - notification throttle service will auto-rebind")
        }
    }
}
