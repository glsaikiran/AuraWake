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
