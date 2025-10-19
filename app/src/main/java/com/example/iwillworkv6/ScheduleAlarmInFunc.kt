package com.example.iwillworkv6

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.concurrent.TimeUnit

fun Context.scheduleAlarmIn(
    value: Long,
    unit: TimeUnit = TimeUnit.SECONDS,
    requestCode: Int = 1001,
    receiverClass: Class<*> = AlarmReceiver::class.java
) {
    val delayMs = try {
        unit.toMillis(value)
    } catch (e: Exception) {
        // fallback if something weird is passed
        unit.toMillis(0)
    }

    val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // On Android S+ check the exact-alarm permission
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

    // Build the PendingIntent for the receiver
    val intent = Intent(this, receiverClass)
    val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val pi = PendingIntent.getBroadcast(this, requestCode, intent, pendingFlags)

    val triggerAt = System.currentTimeMillis().plus(delayMs)

    // Set the alarm (use exact and allow while idle on M+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    } else {
        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    // Persist next alarm time (optional, used by your BootReceiver)
    getSharedPreferences("alarms", Context.MODE_PRIVATE)
        .edit()
        .putLong("next_alarm_ms", triggerAt)
        .apply()
}
