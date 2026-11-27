package com.crescendo.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.crescendo.alarm.data.AlarmDatabase
import com.crescendo.alarm.scheduler.AlarmScheduler
import com.crescendo.alarm.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id",          alarmId)
            putExtra("alarm_label",       intent.getStringExtra("alarm_label") ?: "Alarm")
            putExtra("crescendo_enabled", intent.getBooleanExtra("crescendo_enabled", true))
            val rampSec = intent.getIntExtra("ramp_seconds", 0)
            putExtra("ramp_seconds", rampSec)
            putExtra("sound_name",        intent.getStringExtra("sound_name") ?: "Helix")
            putExtra("sounds_json",       intent.getStringExtra("sounds_json") ?: "[]")
            putExtra("vibration",         intent.getBooleanExtra("vibration", false))
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        if (alarmId != -1) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AlarmDatabase.getInstance(context).alarmDao()
                val alarm = dao.getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.daysAsList().any { it }) {
                        // Reschedule repeating alarm for the next occurrence
                        AlarmScheduler(context).schedule(alarm)
                    } else {
                        // Disable one-time alarm
                        dao.setEnabled(alarmId, false)
                    }
                }
                pendingResult.finish()
            }
        }
    }
}
