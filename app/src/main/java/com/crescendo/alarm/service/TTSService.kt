package com.crescendo.alarm.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.crescendo.alarm.R
import java.util.Locale

class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "tts_channel"
        const val NOTIF_ID   = 1003
        const val ACTION_STOP_TTS = "com.crescendo.alarm.STOP_TTS"
    }

    private var tts: TextToSpeech? = null
    private var message = "It's Bedtime. Time to sleep."
    private var title   = "AuraWake"
    private var pitch   = 1.0f
    private var rate    = 0.9f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_TTS) {
            stopSelf()
            return START_NOT_STICKY
        }
        message = intent?.getStringExtra("message") ?: message
        title   = intent?.getStringExtra("title") ?: "AuraWake"
        pitch   = intent?.getFloatExtra("pitch", 1.0f) ?: 1.0f
        rate    = intent?.getFloatExtra("rate", 0.9f) ?: 0.9f
        
        // Update notification if already running
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotif())

        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // We don't force a language here anymore. 
            // This allows the TTS engine to use your preferred voice (English India) 
            // selected in the Android System Settings.
            tts?.setSpeechRate(rate)
            tts?.setPitch(pitch)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?)  {}
                override fun onDone(id: String?)   { stopSelf() }
                override fun onError(id: String?)  { stopSelf() }
            })
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "bedtime")
        } else stopSelf()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun buildNotif(): Notification {
        val stopPi = PendingIntent.getService(this, 0,
            Intent(this, TTSService::class.java).apply { action = ACTION_STOP_TTS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .addAction(R.drawable.ic_alarm, "Stop", stopPi)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "Bedtime Voice", NotificationManager.IMPORTANCE_LOW))
    }
}
