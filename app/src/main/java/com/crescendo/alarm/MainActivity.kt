package com.crescendo.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import com.crescendo.alarm.ui.theme.CrescendoAlarmTheme
import com.crescendo.alarm.ui.navigation.AppNavigation
import com.crescendo.alarm.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels()

    private val alarmStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.crescendo.alarm.ALARM_STARTED" -> viewModel.setRinging(true)
                "com.crescendo.alarm.ALARM_DISMISSED" -> viewModel.setRinging(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter().apply {
            addAction("com.crescendo.alarm.ALARM_STARTED")
            addAction("com.crescendo.alarm.ALARM_DISMISSED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(alarmStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(alarmStateReceiver, filter)
        }
        
        // Check if service is already running (e.g. app reopened during alarm)
        // A simpler way is to just let the service broadcast again if needed, 
        // but for now this is fine.

        // ...

        // ✅ Request exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        // ✅ Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                101
            )
        }

        setContent {
            val sleepSchedule by viewModel.sleepSchedule.collectAsState()
            val fontFamily = when(sleepSchedule.fontFamily) {
                "Serif" -> FontFamily.Serif
                "SansSerif" -> FontFamily.SansSerif
                "Monospace" -> FontFamily.Monospace
                "Cursive" -> FontFamily.Cursive
                else -> FontFamily.Default
            }

            CrescendoAlarmTheme(
                fontFamily = fontFamily,
                fontSizeMultiplier = sleepSchedule.fontSizeMultiplier
            ) {
                AppNavigation(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(alarmStateReceiver)
    }
}
