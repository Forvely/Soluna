package com.example.iwillworkv6

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.app.NotificationManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var seekBar: SeekBar
    private lateinit var tvSleepValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seekBar = findViewById(R.id.seekBar)
        tvSleepValue = findViewById(R.id.tvSleepValue)

        // 2) helper to update the TextView
        fun updateDisplay(progress: Int) {
            val totalMinutes = progress * 10
            val hours   = totalMinutes / 60
            val minutes = totalMinutes % 60
            tvSleepValue.text = "${hours} h ${minutes} m"
        }

        // 3) initialize
        updateDisplay(seekBar.progress)

        // 4) snap to steps of 1 (each = 10 min) and update text as you drag
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, prog: Int, fromUser: Boolean) {
                // prog is already an int 0–72, each step = 10 min
                updateDisplay(prog)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        findViewById<Button>(R.id.btn_setAlarm).setOnClickListener {
            val stepsPicked = seekBar.progress                // 0..N, each step = 10 minutes
            if (stepsPicked == 0) {
                scheduleAlarmIn5s()                           // quick test path
                return@setOnClickListener
            }

            val totalMinutes = stepsPicked * 10 + 1           // as you requested (+1 minute buffer)
            val wakeTime = Calendar.getInstance().apply {
                add(Calendar.MINUTE, totalMinutes)
            }

            // permission check for exact alarms on Android S+
            val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmMgr.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                return@setOnClickListener
            }

            // schedule the alarm (reuses requestCode 1001 you already use)
            val alarmIntent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 1001, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime.timeInMillis, pendingIntent)
            } else {
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, wakeTime.timeInMillis, pendingIntent)
            }

            // persist for BootReceiver (optional but consistent)
            getSharedPreferences("alarms", Context.MODE_PRIVATE)
                .edit().putLong("next_alarm_ms", wakeTime.timeInMillis).apply()

            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            Toast.makeText(this, "Alarm set for ${fmt.format(wakeTime.time)}", Toast.LENGTH_LONG).show()
        }
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val alreadyOpened = prefs.getBoolean("opened_notification_settings", false)

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (!alreadyOpened) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                })
                prefs.edit().putBoolean("opened_notification_settings", true).apply()
            }
        } else {
            // Notifications enabled for app; ensure the alarm channel exists and is high importance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = getSystemService(NotificationManager::class.java)
                val ch = nm.getNotificationChannel(com.example.iwillworkv6.AlarmReceiver.CHANNEL_ID)
                if (ch == null || ch.importance < NotificationManager.IMPORTANCE_HIGH) {
                    if (!alreadyOpened) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, com.example.iwillworkv6.AlarmReceiver.CHANNEL_ID)
                        })
                        prefs.edit().putBoolean("opened_notification_settings", true).apply()
                    }
                }
            }
        }
    }

    private fun scheduleAlarmIn5s() {
        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmMgr.canScheduleExactAlarms()) {
                // Send user to the exact alarm permission settings (they must grant)
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                return
            }
        }

        val i = Intent(this, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 1001, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + 5000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }

        // Persist a flag so BootReceiver can reschedule if you expand to persistent alarms
        getSharedPreferences("alarms", Context.MODE_PRIVATE)
            .edit().putLong("next_alarm_ms", triggerAt).apply()
    }
}