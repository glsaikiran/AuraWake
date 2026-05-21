package com.crescendo.alarm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Alarm ──────────────────────────────────────────────────────────────────

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String = "Alarm",
    val hour: Int = 7,
    val minute: Int = 0,
    val days: String = "false,true,true,true,true,true,false",
    val enabled: Boolean = true,
    val crescendoEnabled: Boolean = true,
    val rampSeconds: Int = 20,
    val soundName: String = "Helix",
    val soundsJson: String = "[]",
    val vibration: Boolean = false
) {
    fun daysAsList(): List<Boolean> = days.split(",").map { it.trim() == "true" }
    fun formattedTime(): String {
        val period = if (hour >= 12) "PM" else "AM"
        val h = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        return "%d:%02d %s".format(h, minute, period)
    }
}

// ── SleepSchedule ──────────────────────────────────────────────────────────

@Entity(tableName = "sleep_schedule")
data class SleepSchedule(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val enabled: Boolean = true,
    // Bedtime
    val bedHour: Int = 22,
    val bedMinute: Int = 30,
    // Wake time
    val wakeHour: Int = 6,
    val wakeMinute: Int = 30,
    // Wind Down: 0=off, else minutes before bed
    val windDownMinutes: Int = 30,
    // Bedtime reminder voice
    val bedtimeReminderEnabled: Boolean = true,
    val bedtimeSnoozeMinutes: Int = 10,   // 5,10,15,20,30
    // Wake window: 0=off, else minutes before wake
    val wakeWindowMinutes: Int = 30,       // 0,15,30,45,60
    val wakeSound: String = "Helix",
    val wakeSoundsJson: String = "[]",
    val wakeHaptics: Boolean = true,
    val wakeVolume: Int = 80,               // 10-100
    val ttsPitch: Float = 1.0f,
    val ttsRate: Float = 0.9f,
    val fontFamily: String = "Default",
    val fontSizeMultiplier: Float = 1.0f
) {
    /** Total sleep in minutes */
    fun sleepMinutes(): Int {
        var diff = (wakeHour * 60 + wakeMinute) - (bedHour * 60 + bedMinute)
        if (diff < 0) diff += 24 * 60
        return diff
    }

    fun sleepDurationLabel(): String {
        val t = sleepMinutes()
        val h = t / 60; val m = t % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }

    /** true when sleep < 8 hours */
    fun isSleepShort(): Boolean = sleepMinutes() < 480

    fun fmt(h: Int, m: Int): String {
        val period = if (h >= 12) "PM" else "AM"
        val hh = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        return "%d:%02d %s".format(hh, m, period)
    }

    fun formattedBedtime()  = fmt(bedHour, bedMinute)
    fun formattedWakeTime() = fmt(wakeHour, wakeMinute)

    fun windDownStartTime(): String {
        if (windDownMinutes == 0) return ""
        var h = bedHour; var m = bedMinute - windDownMinutes
        while (m < 0) { m += 60; h-- }; if (h < 0) h += 24
        return fmt(h, m)
    }

    fun wakeWindowStartTime(): String {
        if (wakeWindowMinutes == 0) return ""
        var h = wakeHour; var m = wakeMinute - wakeWindowMinutes
        while (m < 0) { m += 60; h-- }; if (h < 0) h += 24
        return fmt(h, m)
    }

    /** Mid-point labels for the wake window timeline */
    fun wakeWindowMidLabel(quarterIndex: Int): String {
        // quarterIndex 0..3 maps to 0%, 33%, 66%, 100% of wakeWindowMinutes
        val fractions = listOf(0, wakeWindowMinutes / 3, wakeWindowMinutes * 2 / 3, wakeWindowMinutes)
        val offsetFromWake = wakeWindowMinutes - fractions[quarterIndex]
        var h = wakeHour; var m = wakeMinute - offsetFromWake
        while (m < 0) { m += 60; h-- }; if (h < 0) h += 24
        return fmt(h, m)
    }
}

data class WakeSoundItem(val uri: String, val name: String, val duration: Int)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String, // format "yyyy-MM-dd"
    val time: String, // format "HH:mm"
    val completed: Boolean = false
)

// ── DAOs ───────────────────────────────────────────────────────────────────

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Int, enabled: Boolean)
}

@Dao
interface SleepScheduleDao {
    @Query("SELECT * FROM sleep_schedule WHERE id = 1")
    fun getSchedule(): Flow<SleepSchedule?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSchedule(schedule: SleepSchedule)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY time ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}

// ── Database ───────────────────────────────────────────────────────────────

@Database(entities = [Alarm::class, SleepSchedule::class, Task::class], version = 8, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun sleepScheduleDao(): SleepScheduleDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AlarmDatabase? = null
        fun getInstance(context: android.content.Context): AlarmDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java, "crescendo_alarm_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
