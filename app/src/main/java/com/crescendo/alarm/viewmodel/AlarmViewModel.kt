package com.crescendo.alarm.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crescendo.alarm.data.*
import com.crescendo.alarm.scheduler.AlarmScheduler
import com.crescendo.alarm.scheduler.SleepScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val db          = AlarmDatabase.getInstance(application)
    private val alarmRepo   = AlarmRepository(db.alarmDao())
    private val sleepRepo   = SleepScheduleRepository(db.sleepScheduleDao())
    private val taskRepo    = TaskRepository(db.taskDao())
    private val scheduler   = AlarmScheduler(application)
    private val sleepSched  = SleepScheduler(application)

    // NEW: Track if alarm is ringing to show Stop button in UI
    private val _isRinging = MutableStateFlow(false)
    val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

    fun setRinging(ringing: Boolean) { _isRinging.value = ringing }

    val alarms: StateFlow<List<Alarm>> = alarmRepo.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepSchedule: StateFlow<SleepSchedule> = sleepRepo.schedule
        .map { it ?: SleepSchedule() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SleepSchedule(id = -1))

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

    fun stopRinging() {
        getApplication<Application>().stopService(
            Intent(getApplication(), com.crescendo.alarm.service.AlarmService::class.java)
        )
        getApplication<Application>().stopService(
            Intent(getApplication(), com.crescendo.alarm.service.WakeWindowService::class.java)
        )
    }

    // ── Sleep Schedule ─────────────────────────────────────────────────────

    fun saveSleepSchedule(s: SleepSchedule) = viewModelScope.launch {
        val wasRinging = _isRinging.value
        sleepRepo.save(s)
        sleepSched.cancelAll()
        
        // If it was ringing and we're saving a new schedule (or disabled it)
        if (wasRinging) {
            stopRinging()
        }

        if (s.enabled) {
            sleepSched.scheduleWindDown(s)
            sleepSched.scheduleBedtimeReminder(s)
            sleepSched.scheduleWakeWindow(s)
        }
    }

    fun toggleSleepSchedule() = viewModelScope.launch {
        val cur     = sleepSchedule.value
        val updated = cur.copy(enabled = !cur.enabled)
        saveSleepSchedule(updated)
    }

    // ── Tasks ─────────────────────────────────────────────────────────────

    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now().toString())
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date -> taskRepo.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDate(date: String) { _selectedDate.value = date }

    fun saveTask(task: Task) = viewModelScope.launch {
        if (task.id == 0) taskRepo.insert(task) else taskRepo.update(task)
    }

    fun toggleTask(task: Task) = viewModelScope.launch {
        taskRepo.update(task.copy(completed = !task.completed))
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        taskRepo.delete(task)
    }

    fun setUserName(name: String) = viewModelScope.launch {
        val cur = sleepSchedule.value
        saveSleepSchedule(cur.copy(userName = name))
    }

    fun setVoiceSettings(pitch: Float, rate: Float) = viewModelScope.launch {
        val cur = sleepSchedule.value
        saveSleepSchedule(cur.copy(ttsPitch = pitch, ttsRate = rate))
    }

    fun setFontFamily(font: String) = viewModelScope.launch {
        val cur = sleepSchedule.value
        saveSleepSchedule(cur.copy(fontFamily = font))
    }

    fun setFontSize(multiplier: Float) = viewModelScope.launch {
        val cur = sleepSchedule.value
        saveSleepSchedule(cur.copy(fontSizeMultiplier = multiplier))
    }

    fun previewVoice() {
        val s = sleepSchedule.value
        val intent = Intent(getApplication(), com.crescendo.alarm.service.TTSService::class.java).apply {
            putExtra("message", "This is a preview of my voice.")
            putExtra("title", "Voice Preview")
            putExtra("pitch", s.ttsPitch)
            putExtra("rate", s.ttsRate)
        }
        getApplication<Application>().startService(intent)
    }

    // ── Briefing ─────────────────────────────────────────────────────────

    fun startMorningBriefing() = viewModelScope.launch {
        val today = java.time.LocalDate.now()
        val now = java.time.LocalTime.now()
        val dateStr = today.toString()
        val dayOfMonth = today.dayOfMonth
        val dayOfWeek = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
        
        val suffix = when {
            dayOfMonth in 11..13 -> "th"
            dayOfMonth % 10 == 1 -> "st"
            dayOfMonth % 10 == 2 -> "nd"
            dayOfMonth % 10 == 3 -> "rd"
            else -> "th"
        }

        val name = sleepSchedule.value.userName.ifBlank { "there" }
        val timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

        val allTasks = taskRepo.getTasksForDate(dateStr).first()
        val pendingTasks = allTasks.filter { !it.completed }
        
        val tasksText = if (allTasks.isEmpty()) {
            "You have no tasks scheduled for today."
        } else if (pendingTasks.isEmpty()) {
            "All your tasks for today are completed. Well done!"
        } else {
            "Your pending tasks for today are: " + pendingTasks.joinToString(", ") { it.title }
        }

        val briefing = "Good morning, $name. Today is ${dayOfMonth}${suffix} and ${dayOfWeek}. " +
                       "It is currently $timeStr. " +
                       "The weather is currently clear. $tasksText"

        val intent = Intent(getApplication(), com.crescendo.alarm.service.TTSService::class.java).apply {
            putExtra("message", briefing)
            putExtra("title", "Morning Briefing")
            val s = sleepSchedule.value
            putExtra("pitch", s.ttsPitch)
            putExtra("rate", s.ttsRate)
        }
        getApplication<Application>().startService(intent)
    }
}
