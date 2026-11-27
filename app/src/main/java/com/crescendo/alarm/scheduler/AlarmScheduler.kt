package com.crescendo.alarm.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.crescendo.alarm.data.Alarm
import com.crescendo.alarm.data.SleepSchedule
import com.crescendo.alarm.receiver.AlarmReceiver
import com.crescendo.alarm.receiver.SleepReceiver
import java.util.Calendar

// ── Alarm Scheduler ────────────────────────────────────────────────────────

class AlarmScheduler(private val ctx: Context) {
    private val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        val days = alarm.daysAsList()
        if (!days.any { it }) {
            set(alarm, nextMs(alarm.hour, alarm.minute, -1), alarm.id)
        } else {
            days.forEachIndexed { i, on ->
                if (on) set(alarm, nextMs(alarm.hour, alarm.minute, i), alarm.id * 10 + i)
            }
        }
    }

    private fun set(alarm: Alarm, ms: Long, rc: Int) {
        val pi = pi(alarm, rc, PendingIntent.FLAG_UPDATE_CURRENT)
        val showIntent = Intent(ctx, com.crescendo.alarm.MainActivity::class.java)
        val showPi = PendingIntent.getActivity(ctx, rc, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(ms, showPi), pi)
    }

    fun cancel(alarm: Alarm) {
        val days = alarm.daysAsList()
        if (!days.any { it }) cancelRc(alarm.id)
        else days.forEachIndexed { i, on -> if (on) cancelRc(alarm.id * 10 + i) }
    }

    private fun cancelRc(rc: Int) {
        val intent = Intent(ctx, AlarmReceiver::class.java)
        val p = PendingIntent.getBroadcast(ctx, rc, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        am.cancel(p); p.cancel()
    }

    private fun pi(alarm: Alarm, rc: Int, flag: Int): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra("alarm_id",          alarm.id)
            putExtra("alarm_label",       alarm.label)
            putExtra("crescendo_enabled", alarm.crescendoEnabled)
            putExtra("ramp_seconds",      alarm.rampSeconds)
            putExtra("sound_name",        alarm.soundName)
            putExtra("sounds_json",       alarm.soundsJson)
            putExtra("vibration",         alarm.vibration)
        }
        return PendingIntent.getBroadcast(ctx, rc, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun nextMs(hour: Int, minute: Int, dayIndex: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val bufferMs = 5000L // 5 second buffer to prevent immediate re-trigger
        if (dayIndex >= 0) {
            cal.set(Calendar.DAY_OF_WEEK, dayIndex + 1)
            if (cal.timeInMillis <= System.currentTimeMillis() + bufferMs) cal.add(Calendar.WEEK_OF_YEAR, 1)
        } else {
            if (cal.timeInMillis <= System.currentTimeMillis() + bufferMs) cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}

// ── Sleep Scheduler ────────────────────────────────────────────────────────

class SleepScheduler(private val ctx: Context) {
    private val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val RC_WIND_DOWN   = 9001
        const val RC_BEDTIME     = 9002
        const val RC_WAKE_WINDOW = 9003
    }

    fun scheduleWindDown(s: SleepSchedule, isReschedule: Boolean = false) {
        if (s.windDownMinutes == 0) return
        val intent = Intent(ctx, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_WIND_DOWN
            putExtra("wind_down_minutes", s.windDownMinutes)
        }
        setExact(RC_WIND_DOWN, intent, bedMs(s, -s.windDownMinutes, isReschedule))
    }

    fun scheduleBedtimeReminder(s: SleepSchedule, isReschedule: Boolean = false) {
        if (!s.bedtimeReminderEnabled) return
        val intent = Intent(ctx, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_BEDTIME_REMINDER
            putExtra("snooze_minutes", s.bedtimeSnoozeMinutes)
        }
        setExact(RC_BEDTIME, intent, bedMs(s, 0, isReschedule))
    }

    fun scheduleWakeWindow(s: SleepSchedule, isReschedule: Boolean = false) {
        val intent = Intent(ctx, SleepReceiver::class.java).apply {
            action = SleepReceiver.ACTION_WAKE_WINDOW
            putExtra("wake_window_minutes", s.wakeWindowMinutes)
            putExtra("wake_hour",   s.wakeHour)
            putExtra("wake_minute", s.wakeMinute)
            putExtra("wake_sound",  s.wakeSound)
            putExtra("wake_sounds_json", s.wakeSoundsJson)
            putExtra("wake_haptics",s.wakeHaptics)
            putExtra("wake_volume", s.wakeVolume)
        }
        setExact(RC_WAKE_WINDOW, intent, wakeMs(s, -s.wakeWindowMinutes, isReschedule))
    }

    fun cancelAll() {
        // Need to cancel all 3 specific actions to match the original PendingIntents
        cancelAction(RC_WIND_DOWN, SleepReceiver.ACTION_WIND_DOWN)
        cancelAction(RC_BEDTIME, SleepReceiver.ACTION_BEDTIME_REMINDER)
        cancelAction(RC_WAKE_WINDOW, SleepReceiver.ACTION_WAKE_WINDOW)
    }

    private fun cancelAction(rc: Int, actionStr: String) {
        val intent = Intent(ctx, SleepReceiver::class.java).apply { action = actionStr }
        val p = PendingIntent.getBroadcast(ctx, rc, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        am.cancel(p)
        p.cancel()
    }

    private fun setExact(rc: Int, intent: Intent, ms: Long) {
        val pi = PendingIntent.getBroadcast(ctx, rc, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            
        if (rc == RC_WAKE_WINDOW) {
            // Use setAlarmClock for the actual wake-up window so it bypasses Doze perfectly
            val showIntent = Intent(ctx, com.crescendo.alarm.MainActivity::class.java)
            val showPi = PendingIntent.getActivity(ctx, rc, showIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(ms, showPi), pi)
        } else {
            // Wind down and bedtime are standard notifications
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        }
    }

    private fun calMs(h: Int, m: Int, offsetMin: Int, isReschedule: Boolean): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val bufferMs = 5000L // 5 second buffer to prevent immediate re-trigger
        
        if (isReschedule) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else {
            // Only add a day if the TARGET event time has already passed
            if (cal.timeInMillis <= System.currentTimeMillis() + bufferMs) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        cal.add(Calendar.MINUTE, offsetMin)
        return cal.timeInMillis
    }

    private fun bedMs(s: SleepSchedule, offsetMin: Int, isReschedule: Boolean = false) = calMs(s.bedHour,  s.bedMinute,  offsetMin, isReschedule)
    private fun wakeMs(s: SleepSchedule, offsetMin: Int, isReschedule: Boolean = false) = calMs(s.wakeHour, s.wakeMinute, offsetMin, isReschedule)
}
