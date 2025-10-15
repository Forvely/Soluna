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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_setAlarm).setOnClickListener {
            scheduleAlarmIn5s()
        }
        findViewById<Button>(R.id.btn_Ublock).setOnClickListener {
            startActivity(Intent().apply {
                action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            })
            val channelId = AlarmReceiver.CHANNEL_ID
            startActivity(Intent().apply {
                action = android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
            })
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