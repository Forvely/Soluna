package com.example.iwillworkv6

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import android.media.RingtoneManager
import android.util.Log

class AlarmPlayerService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notifId = intent?.getIntExtra("notificationId", 1001) ?: 1001

        // content intent opens the full screen activity
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

        // determine ringtone URI: use extra "ringtoneUri" (string) if provided, otherwise system alarm
        val ringtoneUri: Uri = intent?.getStringExtra("ringtoneUri")?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // setup MediaPlayer
        if (player == null) {
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(this@AlarmPlayerService, ringtoneUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e("AlarmPlayerService", "media player fail", e)
                // fallback: try default alarm ringtone again
                try {
                    val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    player = MediaPlayer.create(this, fallback)
                    player?.isLooping = true
                    player?.start()
                } catch (_: Exception) { /* ignore */ }
            }
        }

        // setup vibrator and start repeating vibration pattern
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 1000, 500) // wait 0ms, vibrate 1000ms, pause 500ms, repeat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = repeat from index 0
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            player?.stop()
            player?.release()
            player = null
        } catch (_: Exception) { }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                vibrator?.cancel()
            }
        } catch (_: Exception) { }

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(AlarmReceiver.NOTIF_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
