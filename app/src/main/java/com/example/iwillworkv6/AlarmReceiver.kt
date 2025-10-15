package com.example.iwillworkv6

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmReceiver: BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIF_ID = 1001
        // use your actual package here (matches manifest package)
        const val WAKELOCK_TAG = "com.example.iwillworkv6:alarmWl"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        android.util.Log.d("ALARM_DEBUG","AlarmReceiver.onReceive() called")

        // Acquire short wakelock to ensure device stays awake while we post notification / start service
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wl.acquire(10_000L) // 10 seconds; adjust as necessary

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createChannelIfNeeded(nm)

            // Full-screen intent: opens your full-screen activity
            val activityIntent = Intent(context, FullscreenAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPending = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification that will trigger the full-screen UI
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)               // ensure this drawable exists
                .setContentTitle("Alarm")
                .setContentText("Wake up")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(alarmSound)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPending, true)    // full-screen intent
                .build()

            nm.notify(NOTIF_ID, notif)

            // Start a foreground service to play audio reliably (recommended for long audio)
            val svc = Intent(context, AlarmPlayerService::class.java).apply {
                putExtra("notificationId", NOTIF_ID)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }

        } finally {
            // Release wakelock if still held (safety)
            if (wl.isHeld) wl.release()
        }
    }

    private fun createChannelIfNeeded(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarm notifications"
                    setShowBadge(false)
                    setBypassDnd(true) // user must grant notification policy access for DND bypass to work
                }
                nm.createNotificationChannel(ch)
            }
        }
    }
}
