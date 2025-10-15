package com.example.iwillworkv6

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmPlayerService : Service() {
    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("ALARM_DEBUG","AlarmPlayerService.onStartCommand()")
        val notifId = intent?.getIntExtra("notificationId", 1001) ?: 1001

        val pi = PendingIntent.getActivity(
            this, 3001,
            Intent(this, FullscreenAlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID)
            .setContentTitle("Alarm running")
            .setContentText("Tap to open alarm")
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        startForeground(notifId, notification)
        // ... player setup ...
        return START_STICKY
    }


    override fun onDestroy() {
        player?.stop()
        player?.release()
        player = null
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(AlarmReceiver.NOTIF_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
