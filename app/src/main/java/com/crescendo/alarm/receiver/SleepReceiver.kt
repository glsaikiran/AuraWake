package com.crescendo.alarm.receiver

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.crescendo.alarm.R
import com.crescendo.alarm.service.TTSService
import com.crescendo.alarm.service.WakeWindowService

class SleepReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WIND_DOWN         = "com.crescendo.alarm.WIND_DOWN"
        const val ACTION_BEDTIME_REMINDER  = "com.crescendo.alarm.BEDTIME_REMINDER"
        const val ACTION_WAKE_WINDOW       = "com.crescendo.alarm.WAKE_WINDOW"
        const val ACTION_STOP_WIND_DOWN    = "com.crescendo.alarm.STOP_WIND_DOWN"
        const val ACTION_STOP_BEDTIME      = "com.crescendo.alarm.STOP_BEDTIME"
        const val CHANNEL_SLEEP            = "sleep_channel"
        const val NOTIF_WIND_DOWN          = 2001
        const val NOTIF_BEDTIME            = 2002
    }

    override fun onReceive(context: Context, intent: Intent) {
        createChannel(context)
        when (intent.action) {
            ACTION_WIND_DOWN       -> handleWindDown(context, intent)
            ACTION_STOP_WIND_DOWN  -> handleStopWindDown(context)
            ACTION_BEDTIME_REMINDER -> handleBedtime(context, intent)
            ACTION_STOP_BEDTIME     -> handleStopBedtime(context)
            ACTION_WAKE_WINDOW     -> handleWakeWindow(context, intent)
        }
    }

    private fun handleWindDown(context: Context, intent: Intent) {
        val mins = intent.getIntExtra("wind_down_minutes", 30)

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val savedNotif = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val savedRing  = audio.getStreamVolume(AudioManager.STREAM_RING)
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audio.setStreamVolume(AudioManager.STREAM_RING, 0, 0)

        val stopIntent = Intent(context, SleepReceiver::class.java).apply {
            action = ACTION_STOP_WIND_DOWN
            putExtra("saved_notif_vol", savedNotif)
            putExtra("saved_ring_vol",  savedRing)
        }
        val stopPi = PendingIntent.getBroadcast(context, 9020, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_WIND_DOWN,
            NotificationCompat.Builder(context, CHANNEL_SLEEP)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("🌫️ Wind Down — $mins min")
                .setContentText("Notifications silenced. Tap Stop to restore.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_alarm, "⏹ Stop Wind Down", stopPi)
                .setOngoing(true)
                .build()
        )
    }

    private fun handleStopWindDown(context: Context) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxNotif = audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        val maxRing  = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotif / 2, 0)
        audio.setStreamVolume(AudioManager.STREAM_RING, maxRing / 2, 0)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_WIND_DOWN)
    }

    private fun handleBedtime(context: Context, intent: Intent) {
        val snooze = intent.getIntExtra("snooze_minutes", 10)

        val snoozeIntent = Intent(context, SleepReceiver::class.java).apply {
            action = ACTION_BEDTIME_REMINDER
            putExtra("snooze_minutes", snooze)
        }
        val snoozePi = PendingIntent.getBroadcast(context, 9010, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(context, SleepReceiver::class.java).apply {
            action = ACTION_STOP_BEDTIME
        }
        val stopPi = PendingIntent.getBroadcast(context, 9011, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + snooze * 60_000L, snoozePi)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_BEDTIME,
            NotificationCompat.Builder(context, CHANNEL_SLEEP)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("🌙 It's Bedtime!")
                .setContentText("Time to sleep. Remind again in ${snooze}m?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_alarm, "Remind in ${snooze}m", snoozePi)
                .addAction(R.drawable.ic_alarm, "Stop", stopPi)
                .setAutoCancel(true)
                .build()
        )

        ContextCompat.startForegroundService(context,
            Intent(context, TTSService::class.java).apply {
                putExtra("message", "It's Bedtime. Time to sleep.")
            })
    }

    private fun handleStopBedtime(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_BEDTIME)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(context, SleepReceiver::class.java).apply {
            action = ACTION_BEDTIME_REMINDER
        }
        val snoozePi = PendingIntent.getBroadcast(context, 9010, snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        
        if (snoozePi != null) {
            am.cancel(snoozePi)
            snoozePi.cancel()
        }
        
        context.stopService(Intent(context, TTSService::class.java))
    }

    private fun handleWakeWindow(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, WakeWindowService::class.java).apply {
            putExtra("wake_window_minutes", intent.getIntExtra("wake_window_minutes", 30))
            putExtra("wake_hour",    intent.getIntExtra("wake_hour", 6))
            putExtra("wake_minute",  intent.getIntExtra("wake_minute", 30))
            putExtra("wake_sound",   intent.getStringExtra("wake_sound") ?: "Helix")
            putExtra("wake_sounds_json", intent.getStringExtra("wake_sounds_json") ?: "[]")
            putExtra("wake_haptics", intent.getBooleanExtra("wake_haptics", true))
            putExtra("wake_volume",  intent.getIntExtra("wake_volume", 80))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(
                    CHANNEL_SLEEP, "Sleep Schedule", NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Bedtime and wind-down notifications" })
        }
    }
}
