package com.timer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.timer.R
import java.util.concurrent.TimeUnit

class TimerAlarmService : Service() {
    companion object {
        const val ACTION_START_TIMER_FINISHED_ALARM  = "ACTION_START_ALARM"
        const val ACTION_STOP_TIMER_FINISHED_ALARM = "ACTION_STOP_ALARM"
        const val INTENT_EXTRA_KEY_TIME_AGO_FINISHED = "Time"

        private const val TIMER_FINISHED_ALARM_CHANNEL_ID = "alarm_channel"
        private const val TIMER_FINISHED_ALARM_CHANNEL_NAME = "Timer alarm"
        private const val TIMER_FINISHED_ALARM_NOTIFICATION_ID = 2
    }

    private var alarmRingtone: Ringtone? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START_TIMER_FINISHED_ALARM -> {
                val timeAgoFinished = intent.getLongExtra(INTENT_EXTRA_KEY_TIME_AGO_FINISHED, 0L)
                playTimerFinishedAlarmSound()
                startForeground(
                    TIMER_FINISHED_ALARM_NOTIFICATION_ID,
                    createTimerFinishedAlarmNotification(timeAgoFinished)
                )
            }
            ACTION_STOP_TIMER_FINISHED_ALARM -> {
                stopTimerFinishedAlarmSound()
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        return START_NOT_STICKY
    }

    private fun playTimerFinishedAlarmSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        alarmRingtone = RingtoneManager.getRingtone(this, uri)
        alarmRingtone?.play()
    }

    private fun stopTimerFinishedAlarmSound() = alarmRingtone?.takeIf { it.isPlaying }?.stop()

    override fun onDestroy() {
        stopTimerFinishedAlarmSound()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private val timerAlarmNotificationChannel: NotificationChannel by lazy {
        NotificationChannel(
            TIMER_FINISHED_ALARM_CHANNEL_ID,
            TIMER_FINISHED_ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            getSystemService(NotificationManager::class.java).createNotificationChannel(this)
        }
    }

    private fun createTimerFinishedAlarmNotification(timeAgoFinished: Long = 0L): Notification {
        val stopIntent = Intent(this, TimerAlarmService::class.java).apply {
            action = ACTION_STOP_TIMER_FINISHED_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message: String?
        if (timeAgoFinished != 0L) {
            val timeFormated = formatDuration(timeAgoFinished)
            message = "Таймер завершился: $timeFormated назад!"
        } else message = "Таймер завершился!"


        return NotificationCompat.Builder(this, timerAlarmNotificationChannel.id)
            .setContentTitle("Таймер")
            .setContentText(message)
            .setSmallIcon(R.drawable.alarm)
            .addAction(R.drawable.close, "Остановить", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun formatDuration(millis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val days = TimeUnit.MILLISECONDS.toDays(millis) % 365
        val years = TimeUnit.MILLISECONDS.toDays(millis) / 365

        val parts = mutableListOf<String>()

        if (years > 0) parts.add("$years ${if (years == 1L) "год" else "года"}")
        if (days > 0) parts.add("$days ${if (days == 1L) "день" else "дней"}")
        if (hours > 0) parts.add("$hours ${if (hours == 1L) "час" else "часов"}")
        if (minutes > 0) parts.add("$minutes ${if (minutes == 1L) "минута" else "минут"}")
        if (seconds > 0 || parts.isEmpty()) parts.add("$seconds ${if (seconds == 1L) "секунда"
        else "секунд"}")

        return parts.joinToString(" ")
    }
}