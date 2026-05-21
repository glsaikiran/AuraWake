package com.crescendo.alarm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.crescendo.alarm.R
import org.json.JSONArray

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID  = "crescendo_alarm_channel"
        const val NOTIF_ID    = 1001
        const val ACTION_STOP = "com.crescendo.alarm.STOP_ALARM"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler    = Handler(Looper.getMainLooper())
    private var curVol     = 0f
    private val maxVol     = 1f
    // Step every 10 seconds for smooth ramp across minutes
    private val stepMs     = 10_000L
    private var totalSteps = 1

    private data class WakeSound(val uri: String, val name: String, val durationMin: Int)
    private var soundsList = mutableListOf<WakeSound>()
    private var currentSoundIndex = -1

    override fun onBind(intent: Intent?) = null
    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopAlarm(); return START_NOT_STICKY }

        val label      = intent?.getStringExtra("alarm_label")              ?: "Alarm"
        val crescendo  = intent?.getBooleanExtra("crescendo_enabled", true) ?: true
        val rampSec    = intent?.getIntExtra("ramp_seconds", 0)             ?: 0
        val rampMin    = rampSec / 60
        val vibOn      = intent?.getBooleanExtra("vibration", false)        ?: false
        val soundName  = intent?.getStringExtra("sound_name")               ?: "Default"
        val soundsJson = intent?.getStringExtra("sounds_json")             ?: "[]"

        parseSounds(soundsJson, soundName)

        val notification = buildNotif(label)
        startForeground(NOTIF_ID, notification)
        
        // Explicitly try to launch the activity so it appears immediately if the phone is in use
        val alarmIntent = Intent(this, com.crescendo.alarm.AlarmActivity::class.java).apply {
            putExtra("alarm_label", label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            startActivity(alarmIntent)
        } catch (e: Exception) {
            // Fallback: the fullScreenIntent in the notification will handle it if the screen is locked
        }

        sendBroadcast(Intent("com.crescendo.alarm.ALARM_STARTED").setPackage(packageName))
        playAlarmSequence(crescendo, rampMin, vibOn)
        return START_STICKY
    }

    private fun parseSounds(json: String, defaultSound: String) {
        soundsList.clear()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                soundsList.add(WakeSound(
                    uri = obj.getString("uri"),
                    name = obj.optString("name", "Sound"),
                    durationMin = obj.optInt("duration", 5)
                ))
            }
        } catch (e: Exception) {}
        if (soundsList.isEmpty()) {
            soundsList.add(WakeSound(defaultSound, defaultSound, 60))
        }
    }

    private fun soundUri(path: String): Uri {
        return if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri.parse(path)
        } else when (path) {
            "Helix"        -> Uri.parse("android.resource://${packageName}/raw/helix")
            "Cosmic Rise"  -> Uri.parse("android.resource://${packageName}/raw/cosmic_rise")
            "Morning Mist" -> Uri.parse("android.resource://${packageName}/raw/morning_mist")
            "Wind Chime"   -> Uri.parse("android.resource://${packageName}/raw/wind_chime")
            else           -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }

    private fun playAlarmSequence(crescendo: Boolean, rampMin: Int, vibOn: Boolean) {
        if (vibOn) startVibration()
        
        if (crescendo && rampMin > 0) {
            totalSteps = (rampMin * 60 * 1000L / stepMs).toInt().coerceAtLeast(1)
            curVol = 0f
            ramp()
        } else {
            curVol = maxVol
        }

        playNextSound()
    }

    private fun playNextSound() {
        currentSoundIndex++
        if (currentSoundIndex >= soundsList.size) {
            currentSoundIndex = soundsList.size - 1
        }

        val sound = soundsList[currentSoundIndex]
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try { setDataSource(applicationContext, soundUri(sound.uri)) }
            catch (e: Exception) {
                setDataSource(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            }
            @Suppress("DEPRECATION")
            setAudioStreamType(AudioManager.STREAM_ALARM)
            isLooping = true
            prepare()
            setVolume(curVol, curVol)
            start()
        }

        if (currentSoundIndex < soundsList.size - 1) {
            handler.postDelayed({
                playNextSound()
            }, sound.durationMin * 60 * 1000L)
        }
    }

    private fun ramp() {
        val step = maxVol / totalSteps
        handler.postDelayed(object : Runnable {
            override fun run() {
                curVol = (curVol + step).coerceAtMost(maxVol)
                mediaPlayer?.setVolume(curVol, curVol)
                if (curVol < maxVol) handler.postDelayed(this, stepMs)
            }
        }, stepMs)
    }


    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val pat = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator?.vibrate(VibrationEffect.createWaveform(pat, 0))
        else @Suppress("DEPRECATION") vibrator?.vibrate(pat, 0)
    }

    private fun stopAlarm() {
        if (mediaPlayer == null && vibrator == null) return
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        
        // Trigger briefing broadcast
        sendBroadcast(Intent("com.crescendo.alarm.ALARM_DISMISSED").setPackage(packageName))

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() { stopAlarm(); super.onDestroy() }

    private fun buildNotif(label: String): Notification {
        val stopPi = PendingIntent.getService(this, 0,
            Intent(this, AlarmService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        val alarmIntent = Intent(this, com.crescendo.alarm.AlarmActivity::class.java).apply {
            putExtra("alarm_label", label)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val openPi = PendingIntent.getActivity(this, 0, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(label)
            .setContentText("Crescendo alarm ringing — swipe to stop")
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_alarm, "⏹ Stop", stopPi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openPi, true)
            .setOngoing(true).build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Crescendo Alarms", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alarm notifications"
                setSound(null, null) // We play our own sound via MediaPlayer
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
