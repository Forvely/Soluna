package com.example.iwillworkv6

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

class FullscreenAlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContentView(R.layout.activity_fullscreen_alarm)

        // make hardware volume buttons control the alarm stream while this activity is visible
        volumeControlStream = AudioManager.STREAM_ALARM

        findViewById<Button>(R.id.btn_Dismiss).setOnClickListener {
            // stop foreground service playing the alarm and finish activity
            stopService(Intent(this, AlarmPlayerService::class.java))
            finish()
        }
        findViewById<Button>(R.id.btn_Snooze).setOnClickListener {
            // stop foreground service playing the alarm and finish activity, then schedule a new alarm in _ minutes/seconds
            stopService(Intent(this, AlarmPlayerService::class.java))
            finish()
            scheduleAlarmIn(5, TimeUnit.SECONDS)
        }
    }
}
