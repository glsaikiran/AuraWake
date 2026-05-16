// ── Boot receiver (reschedule alarms after reboot) ─────────────────────
package com.crescendo.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crescendo.alarm.data.AlarmDatabase
import com.crescendo.alarm.scheduler.AlarmScheduler
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
            val dao = AlarmDatabase.getInstance(context).alarmDao()
            val scheduler = AlarmScheduler(context)
            // Collect once
            dao.getAllAlarms().collect { alarms ->
                alarms.filter { it.enabled }.forEach { scheduler.schedule(it) }
                scope.cancel()
            }
        }
    }
}