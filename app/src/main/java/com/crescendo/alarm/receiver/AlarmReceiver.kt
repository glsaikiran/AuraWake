package com.crescendo.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.crescendo.alarm.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id",          intent.getIntExtra("alarm_id", -1))
            putExtra("alarm_label",       intent.getStringExtra("alarm_label") ?: "Alarm")
            putExtra("crescendo_enabled", intent.getBooleanExtra("crescendo_enabled", true))
            val rampSec = intent.getIntExtra("ramp_seconds", 0)
            putExtra("ramp_seconds", rampSec)
            putExtra("sound_name",        intent.getStringExtra("sound_name") ?: "Helix")
            putExtra("sounds_json",       intent.getStringExtra("sounds_json") ?: "[]")
            putExtra("vibration",         intent.getBooleanExtra("vibration", false))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
