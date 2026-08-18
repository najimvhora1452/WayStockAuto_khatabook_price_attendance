package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AdminAuthManager
import com.example.data.AttendanceRecordEntity
import com.example.data.WayStockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_ATTENDANCE = "com.example.ACTION_MARK_ATTENDANCE"
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val EXTRA_STATUS = "extra_status"
        const val NOTIFICATION_ID = 2026
        const val CHANNEL_ID = "waystock_attendance_channel"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Attendance Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders to mark Present/Absent directly with quick action buttons"
                    enableLights(true)
                    enableVibration(true)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun showAttendanceNotification(context: Context, customPrompt: String? = null) {
            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val authManager = AdminAuthManager(context)
            val (userName, _) = authManager.getLocalUserProfile()
            val displayName = if (userName.isNotBlank()) userName else "Team Member"

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Present Action Intent
            val presentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                action = ACTION_MARK_ATTENDANCE
                putExtra(EXTRA_STATUS, "Present")
            }
            val presentPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                presentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Absent Action Intent
            val absentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                action = ACTION_MARK_ATTENDANCE
                putExtra(EXTRA_STATUS, "Absent")
            }
            val absentPendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                absentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Half Day Action Intent
            val halfDayIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                action = ACTION_MARK_ATTENDANCE
                putExtra(EXTRA_STATUS, "Half Day")
            }
            val halfDayPendingIntent = PendingIntent.getBroadcast(
                context,
                103,
                halfDayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val body = customPrompt ?: "Good day $displayName! Please punch in or mark your attendance for today."

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⏰ WayStock Daily Attendance")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\nTap below to mark directly without opening the app:"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .addAction(0, "🟢 Present", presentPendingIntent)
                .addAction(0, "🟡 Half Day", halfDayPendingIntent)
                .addAction(0, "🔴 Absent", absentPendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun scheduleDailyReminders(context: Context, enabled: Boolean) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val morningIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
            }
            val morningPending = PendingIntent.getBroadcast(
                context,
                201,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val eveningIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
            }
            val eveningPending = PendingIntent.getBroadcast(
                context,
                202,
                eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!enabled) {
                alarmManager.cancel(morningPending)
                alarmManager.cancel(eveningPending)
                return
            }

            // Morning Reminder (09:30 AM)
            val calMorning = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calMorning.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    morningPending
                )
            } catch (_: Exception) {}

            // Evening Reminder (06:30 PM)
            val calEvening = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calEvening.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    eveningPending
                )
            } catch (_: Exception) {}
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_TRIGGER_REMINDER) {
            showAttendanceNotification(context)
            return
        }

        if (action == ACTION_MARK_ATTENDANCE) {
            val status = intent.getStringExtra(EXTRA_STATUS) ?: "Present"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)

            // Perform Database Insertion in background
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val authManager = AdminAuthManager(context)
                    val (userName, userId) = authManager.getLocalUserProfile()
                    val actualName = if (userName.isNotBlank()) userName else "You"
                    val actualUserId = if (userId.isNotBlank()) userId else "device_user"

                    val db = WayStockDatabase.getDatabase(context)
                    val dao = db.inventoryDao()

                    val allStaff = dao.getAllStaffMembersFlow().first()
                    val matchedStaff = allStaff.find { it.name.equals(actualName, ignoreCase = true) }
                    val staffId = matchedStaff?.id ?: "STAFF_SELF_$actualUserId"

                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                    val existing = dao.getStaffAttendanceForDate(staffId, todayDate)
                    val record = AttendanceRecordEntity(
                        id = "${staffId}_${todayDate}",
                        staffId = staffId,
                        staffName = actualName,
                        date = todayDate,
                        status = status,
                        inTime = existing?.inTime ?: if (status == "Present" || status == "Half Day") timeStr else null,
                        outTime = existing?.outTime,
                        markedBy = "$actualName (Quick Notification)",
                        markedAt = System.currentTimeMillis()
                    )

                    dao.insertOrUpdateAttendance(record)

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "✅ Attendance '$status' marked successfully for $actualName!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
