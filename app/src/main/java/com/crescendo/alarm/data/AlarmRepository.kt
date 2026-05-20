package com.crescendo.alarm.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {
    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()
    suspend fun insert(alarm: Alarm): Long = dao.insertAlarm(alarm)
    suspend fun update(alarm: Alarm) = dao.updateAlarm(alarm)
    suspend fun delete(alarm: Alarm) = dao.deleteAlarm(alarm)
    suspend fun setEnabled(id: Int, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun getById(id: Int): Alarm? = dao.getAlarmById(id)
}

class SleepScheduleRepository(private val dao: SleepScheduleDao) {
    val schedule: Flow<SleepSchedule?> = dao.getSchedule()
    suspend fun save(schedule: SleepSchedule) = dao.saveSchedule(schedule)
}

class TaskRepository(private val dao: TaskDao) {
    fun getTasksForDate(date: String): Flow<List<Task>> = dao.getTasksForDate(date)
    suspend fun insert(task: Task) = dao.insertTask(task)
    suspend fun update(task: Task) = dao.updateTask(task)
    suspend fun delete(task: Task) = dao.deleteTask(task)
}
