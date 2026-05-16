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
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language   = Locale.US
            tts?.setSpeechRate(0.88f)
            tts?.setPitch(0.95f)
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
            .setContentTitle("🌙 Bedtime")
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
