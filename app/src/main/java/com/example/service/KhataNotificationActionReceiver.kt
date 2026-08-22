package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import com.example.data.KhataCloudManager
import com.example.data.KhataMemoEntity
import com.example.data.WayStockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that captures Direct Reply input from the Android Status Bar Notification.
 * When the user types a memo in the notification input box and taps Send,
 * this receiver immediately stores the memo into Room SQLite & Firebase Firestore.
 */
class KhataNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_DIRECT_REPLY_KHATA_MEMO) {
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            val memoText = remoteInput?.getCharSequence(KEY_QUICK_MEMO_INPUT)?.toString()?.trim()

            if (!memoText.isNullOrBlank()) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = WayStockRepository(context)
                        val cloudManager = KhataCloudManager(context, repository)

                        // 1. Insert into local SQLite database
                        val memoId = repository.insertKhataMemo(memoText)

                        // 2. Sync to Firebase Firestore cloud database
                        val memoEntity = KhataMemoEntity(
                            id = memoId,
                            note = memoText,
                            timestamp = System.currentTimeMillis()
                        )
                        cloudManager.uploadMemoToCloud(memoEntity)

                        // 3. Update the notification to show immediate success feedback
                        updateNotificationFeedback(context, memoText)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving memo from notification: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun updateNotificationFeedback(context: Context, savedText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        KhataStickyNotificationService.createChannel(context)

        // Open App to Khata Tab PendingIntent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = KhataStickyNotificationService.ACTION_OPEN_KHATA_TAB
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            203,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Setup Direct Reply Action for next entry
        val remoteInput = RemoteInput.Builder(KEY_QUICK_MEMO_INPUT)
            .setLabel("Type note / transaction...")
            .build()

        val replyIntent = Intent(context, KhataNotificationActionReceiver::class.java).apply {
            action = ACTION_DIRECT_REPLY_KHATA_MEMO
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPendingIntent = PendingIntent.getBroadcast(context, 101, replyIntent, flags)

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "✍️ Type Note & Send",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val stopIntent = Intent(context, KhataStickyNotificationService::class.java).apply {
            action = KhataStickyNotificationService.ACTION_STOP_STICKY_NOTIFICATION
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            204,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Rebuild the sticky notification with the saved acknowledgment
        val updatedNotification = NotificationCompat.Builder(context, KhataStickyNotificationService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✅ Saved to Khata: $savedText")
            .setContentText("Tap 'Type Note & Send' to type another memo or tap to open KhataBook")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .addAction(replyAction)
            .addAction(android.R.drawable.ic_menu_agenda, "📒 Open Khata", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "❌ Turn Off", stopPendingIntent)
            .build()

        notificationManager.notify(KhataStickyNotificationService.NOTIFICATION_ID, updatedNotification)
    }

    companion object {
        const val ACTION_DIRECT_REPLY_KHATA_MEMO = "com.example.ACTION_DIRECT_REPLY_KHATA_MEMO"
        const val KEY_QUICK_MEMO_INPUT = "key_quick_memo_input"
        private const val TAG = "KhataNotificationRcvr"
    }
}
