package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.screens.KhataQuickEntryActivity

/**
 * Persistent Foreground Service that displays a sticky Notification Bar
 * for KhataBook Quick Search & 1-Tap Udhar/Advance Entries from anywhere in Android.
 */
class KhataStickyNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_STICKY_NOTIFICATION) {
            setStickyKhataEnabled(this, false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startKhataForeground()
        return START_STICKY
    }

    private fun startKhataForeground() {
        createChannel(this)

        // Tap on notification body opens the Quick Entry Floating Dialog
        val quickEntryIntent = Intent(this, KhataQuickEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val quickEntryPendingIntent = PendingIntent.getActivity(
            this,
            201,
            quickEntryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Search & Quick Add
        val searchPendingIntent = PendingIntent.getActivity(
            this,
            202,
            quickEntryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Open Full Khata Book in App
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_KHATA_TAB
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            203,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Turn Off Sticky Bar
        val stopIntent = Intent(this, KhataStickyNotificationService::class.java).apply {
            action = ACTION_STOP_STICKY_NOTIFICATION
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            204,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚡ KhataBook Quick Search & Entry")
            .setContentText("Tap to search customer, view wallet balance or add 1-tap entry")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(quickEntryPendingIntent)
            .addAction(android.R.drawable.ic_menu_search, "🔍 Search & Add", searchPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "📒 Open Khata", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "❌ Turn Off", stopPendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Throwable) {}
        }
    }

    companion object {
        const val CHANNEL_ID = "waystock_khata_sticky_channel"
        const val NOTIFICATION_ID = 3030
        const val ACTION_START_STICKY_NOTIFICATION = "com.example.ACTION_START_KHATA_STICKY"
        const val ACTION_STOP_STICKY_NOTIFICATION = "com.example.ACTION_STOP_KHATA_STICKY"
        const val ACTION_OPEN_KHATA_TAB = "com.example.ACTION_OPEN_KHATA_TAB"
        private const val PREFS_NAME = "waystock_khata_prefs"
        private const val KEY_STICKY_ENABLED = "key_sticky_khata_enabled"

        fun isStickyKhataEnabled(context: Context): Boolean {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_STICKY_ENABLED, false)
        }

        fun setStickyKhataEnabled(context: Context, enabled: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_STICKY_ENABLED, enabled).apply()
        }

        fun toggleStickyNotification(context: Context, enable: Boolean) {
            setStickyKhataEnabled(context, enable)
            val intent = Intent(context, KhataStickyNotificationService::class.java).apply {
                action = if (enable) ACTION_START_STICKY_NOTIFICATION else ACTION_STOP_STICKY_NOTIFICATION
            }
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "KhataBook Quick Entry Bar",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Permanent notification bar for instant customer search and khata entry"
                    setShowBadge(false)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
