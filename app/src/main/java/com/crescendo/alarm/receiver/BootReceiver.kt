// ── Boot receiver (reschedule alarms after reboot) ─────────────────────
package com.crescendo.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crescendo.alarm.data.AlarmDatabase
import com.crescendo.alarm.scheduler.AlarmScheduler
import com.crescendo.alarm.scheduler.SleepScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val db = AlarmDatabase.getInstance(context)
            
            // 1. Reschedule regular alarms
            val alarmScheduler = AlarmScheduler(context)
            db.alarmDao().getAllAlarms().collect { alarms ->
                alarms.filter { it.enabled }.forEach { alarmScheduler.schedule(it) }
                
                // 2. Reschedule sleep schedule
                val sleepSchedule = db.sleepScheduleDao().getScheduleNow()
                if (sleepSchedule != null && sleepSchedule.enabled) {
                    val sleepScheduler = SleepScheduler(context)
                    sleepScheduler.scheduleWindDown(sleepSchedule)
                    sleepScheduler.scheduleBedtimeReminder(sleepSchedule)
                    sleepScheduler.scheduleWakeWindow(sleepSchedule)
                }

                scope.cancel()
            }
        }
    }
}