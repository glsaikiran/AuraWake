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
import com.crescendo.alarm.MainActivity
import com.crescendo.alarm.R
import org.json.JSONArray

/**
 * Wake Window Service
 * ──────────────────
 * Starts at the beginning of the wake window (e.g. 5:30 for a 30-min window before 6:00).
 * Volume begins at ~2% and ramps up using a quadratic ease-in curve.
 * Every 10 seconds: v = targetVolume × (elapsed / total)²
 * At wake time (full duration elapsed) volume reaches targetVolume.
 *
 * Bug fixes vs previous version:
 *  - MediaPlayer now starts immediately (was missing .start() call in some paths)
 *  - soundUri() fallback now correctly catches missing raw files
 *  - First volume step fires at t=0 so sound is audible right away (whisper level)
 *  - Stop action now works correctly and cancels handler callbacks
 */
class WakeWindowService : Service() {

    companion object {
        const val CHANNEL_ID  = "wake_window_channel"
        const val NOTIF_ID    = 1002
        const val ACTION_STOP = "com.crescendo.alarm.STOP_WAKE_WINDOW"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private var totalMs      = 0L
    private var startMs      = 0L
    private var targetVol    = 0.8f
    private var hapticsOn    = false
    private var windowMin    = 30
    
    private data class WakeSound(val uri: String, val name: String, val durationMin: Int)
    private var soundsList = mutableListOf<WakeSound>()
    private var currentSoundIndex = -1
    private var lastVolume = 0.02f

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        windowMin   = intent?.getIntExtra("wake_window_minutes", 30) ?: 30
        targetVol   = ((intent?.getIntExtra("wake_volume", 80) ?: 80) / 100f).coerceIn(0.1f, 1f)
        hapticsOn   = intent?.getBooleanExtra("wake_haptics", true)  ?: true
        val sound   = intent?.getStringExtra("wake_sound") ?: "Helix"
        val soundsJson = intent?.getStringExtra("wake_sounds_json") ?: "[]"

        parseSounds(soundsJson, sound)

        totalMs  = windowMin * 60 * 1000L
        startMs  = System.currentTimeMillis()

        startForeground(NOTIF_ID, buildNotif())
        
        // Launch the visual atmospheric screen
        val alarmIntent = Intent(this, com.crescendo.alarm.AlarmActivity::class.java).apply {
            putExtra("alarm_label", "Wake Up Window")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            startActivity(alarmIntent)
        } catch (e: Exception) {}

        sendBroadcast(Intent("com.crescendo.alarm.ALARM_STARTED").setPackage(packageName))
        playNextSound()

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (soundsList.isEmpty()) {
            soundsList.add(WakeSound(defaultSound, defaultSound, windowMin))
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

    private fun playNextSound() {
        currentSoundIndex++
        if (currentSoundIndex >= soundsList.size) {
            currentSoundIndex = soundsList.size - 1
            // Keep playing the last sound if window isn't over? 
            // Actually, we'll just let it loop.
        }

        val sound = soundsList[currentSoundIndex]
        
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, soundUri(sound.uri))
            } catch (e: Exception) {
                setDataSource(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            }

            @Suppress("DEPRECATION")
            setAudioStreamType(AudioManager.STREAM_ALARM)
            isLooping = true
            prepare()
            setVolume(lastVolume, lastVolume)
            start()
        }

        if (hapticsOn && currentSoundIndex == 0) startVibration()
        
        if (currentSoundIndex == 0) {
            scheduleVolumeRamp()
        }
        
        // Schedule next sound if any
        if (currentSoundIndex < soundsList.size - 1) {
            handler.postDelayed({
                playNextSound()
            }, sound.durationMin * 60 * 1000L)
        }
    }

    private fun scheduleVolumeRamp() {
        val runnable = object : Runnable {
            override fun run() {
                val elapsed  = System.currentTimeMillis() - startMs
                val progress = if (totalMs > 0) (elapsed.toFloat() / totalMs).coerceIn(0f, 1f) else 1f
                val vol = (targetVol * progress * progress).coerceAtLeast(0.02f)
                lastVolume = vol
                mediaPlayer?.setVolume(vol, vol)

                if (progress < 1f) {
                    handler.postDelayed(this, 10_000L)
                } else {
                    mediaPlayer?.setVolume(targetVol, targetVol)
                }
            }
        }
        handler.post(runnable)
    }


    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Gentle pulse: 200ms buzz, 29.8s quiet → repeats every 30s
        val pattern = longArrayOf(0, 200, 29_800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        else @Suppress("DEPRECATION") vibrator?.vibrate(pattern, 0)
    }

    private fun stopEverything() {
        if (mediaPlayer == null && vibrator == null) return
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply {
            try { if (isPlaying) stop() } catch (_: Exception) {}
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null

        // Trigger briefing broadcast
        sendBroadcast(Intent("com.crescendo.alarm.ALARM_DISMISSED").setPackage(packageName))
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    private fun buildNotif(): Notification {
        val stopIntent = Intent(this, WakeWindowService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        val alarmIntent = Intent(this, com.crescendo.alarm.AlarmActivity::class.java).apply {
            putExtra("alarm_label", "Wake Up Window")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(this, 0, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("🌅 Wake Up Window")
            .setContentText("Gradually waking you up over ${windowMin} minutes")
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_alarm, "⏹ Stop", stopPi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openPi, true)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "Wake Window", NotificationManager.IMPORTANCE_HIGH
                ).apply { setSound(null, null) })
        }
    }
}
