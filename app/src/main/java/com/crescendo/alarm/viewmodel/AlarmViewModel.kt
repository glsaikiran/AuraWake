package com.crescendo.alarm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crescendo.alarm.data.*
import com.crescendo.alarm.scheduler.AlarmScheduler
import com.crescendo.alarm.scheduler.SleepScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val db          = AlarmDatabase.getInstance(application)
    private val alarmRepo   = AlarmRepository(db.alarmDao())
    private val sleepRepo   = SleepScheduleRepository(db.sleepScheduleDao())
    private val scheduler   = AlarmScheduler(application)
    private val sleepSched  = SleepScheduler(application)

    val alarms: StateFlow<List<Alarm>> = alarmRepo.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepSchedule: StateFlow<SleepSchedule> = sleepRepo.schedule
        .map { it ?: SleepSchedule() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SleepSchedule())

    // ── Alarm ──────────────────────────────────────────────────────────────

    fun toggleAlarm(alarm: Alarm) = viewModelScope.launch {
        val updated = alarm.copy(enabled = !alarm.enabled)
        alarmRepo.update(updated)
        if (updated.enabled) scheduler.schedule(updated) else scheduler.cancel(updated)
    }

    fun saveAlarm(alarm: Alarm) = viewModelScope.launch {
        if (alarm.id == 0) {
            val id = alarmRepo.insert(alarm).toInt()
            val saved = alarm.copy(id = id)
            if (saved.enabled) scheduler.schedule(saved)
        } else {
            alarmRepo.update(alarm)
            scheduler.cancel(alarm)
            if (alarm.enabled) scheduler.schedule(alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) = viewModelScope.launch {
        scheduler.cancel(alarm); alarmRepo.delete(alarm)
    }

    // ── Sleep Schedule ─────────────────────────────────────────────────────

    fun saveSleepSchedule(s: SleepSchedule) = viewModelScope.launch {
        sleepRepo.save(s)
        sleepSched.cancelAll()
        if (s.enabled) {
            sleepSched.scheduleWindDown(s)
            sleepSched.scheduleBedtimeReminder(s)
            sleepSched.scheduleWakeWindow(s)
        }
    }

    fun toggleSleepSchedule() = viewModelScope.launch {
        val cur     = sleepSchedule.value
        val updated = cur.copy(enabled = !cur.enabled)
        sleepRepo.save(updated)
        sleepSched.cancelAll()
        if (updated.enabled) {
            sleepSched.scheduleWindDown(updated)
            sleepSched.scheduleBedtimeReminder(updated)
            sleepSched.scheduleWakeWindow(updated)
        }
    }
}
