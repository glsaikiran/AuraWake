package com.crescendo.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crescendo.alarm.data.AlarmDatabase
import com.crescendo.alarm.service.TTSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class BriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.crescendo.alarm.ALARM_DISMISSED") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AlarmDatabase.getInstance(context)
                    val sleep = db.sleepScheduleDao().getSchedule().first()
                    val today = LocalDate.now()
                    val now = LocalTime.now()
                    val dateStr = today.toString()
                    val dayOfMonth = today.dayOfMonth
                    val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    
                    val suffix = when {
                        dayOfMonth in 11..13 -> "th"
                        dayOfMonth % 10 == 1 -> "st"
                        dayOfMonth % 10 == 2 -> "nd"
                        dayOfMonth % 10 == 3 -> "rd"
                        else -> "th"
                    }

                    val name = sleep?.userName?.ifBlank { "there" } ?: "there"
                    val timeStr = now.format(DateTimeFormatter.ofPattern("h:mm a"))

                    val todayTasks = db.taskDao().getTasksForDate(dateStr).first()
                    val tasksText = if (todayTasks.isEmpty()) {
                        "You have no tasks scheduled for today."
                    } else {
                        "Your tasks for today are: " + todayTasks.joinToString(", ") { it.title }
                    }

                    val briefing = "Good morning, $name. Today is ${dayOfMonth}${suffix} and ${dayOfWeek}. " +
                                   "It is currently $timeStr. " +
                                   "The weather is currently clear. $tasksText"

                    val ttsIntent = Intent(context, TTSService::class.java).apply {
                        putExtra("message", briefing)
                        putExtra("title", "Morning Briefing")
                        putExtra("pitch", sleep?.ttsPitch ?: 1.0f)
                        putExtra("rate", sleep?.ttsRate ?: 0.9f)
                    }
                    context.startForegroundService(ttsIntent)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
