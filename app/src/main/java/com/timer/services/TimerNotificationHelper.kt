package com.timer.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.timer.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface TimerNotification {
    fun showOnResumeServiceNotification(context: Context, restoredSystemTime: Long, currentSystemTime: Long)
    fun showOnMinuteStartNotification(context: Context, remainingMinutes: Long)
}

object TimerNotificationHelper: TimerNotification {
    private const val PUSH_CHANNEL_ID = "push_channel"
    private const val PUSH_CHANNEL_NAME = "Timer Push Notifications"

    private const val SERVICE_CHANNEL_ID = "timer_service"
    private const val SERVICE_CHANNEL_NAME = "Timer Service"

    private const val SERVICE_NOTIFICATION_ID = 1
    private const val PUSH_NOTIFICATION_ID = 2

    private val alarmIconId = R.drawable.alarm

    private var isPushNotificationChannelInitialized = false
    private val pushNotificationChannel: NotificationChannel by lazy {
        isPushNotificationChannelInitialized = true
        NotificationChannel(
            PUSH_CHANNEL_ID, PUSH_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        )
    }

    private val serviceNotificationChannel: NotificationChannel by lazy {
        NotificationChannel(
            SERVICE_CHANNEL_ID, SERVICE_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        )
    }

    private fun createServiceNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(serviceNotificationChannel)
    }

    private fun createPushNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(pushNotificationChannel)
    }

    override fun showOnResumeServiceNotification(
        context: Context,
        restoredSystemTime: Long,
        currentSystemTime: Long
    ) {
        val restoredUserTime = Instant.ofEpochMilli(restoredSystemTime)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()

        val currentUserTime = Instant.ofEpochMilli(currentSystemTime)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val restoredUserTimeViewFormat = restoredUserTime.format(formatter)
        val currentUserTimeViewFormat = currentUserTime.format(formatter)

        if (!isPushNotificationChannelInitialized) {
            createPushNotificationChannel(context)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText("Время остановки: $restoredUserTimeViewFormat\n" +
                    "Время продолжения: $currentUserTimeViewFormat")

        val notification = NotificationCompat.Builder(context, pushNotificationChannel.id)
            .setContentTitle("Таймер")
            .setStyle(bigTextStyle)
            .setSmallIcon(alarmIconId)
            .setSmallIcon(alarmIconId)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(PUSH_NOTIFICATION_ID, notification)
    }

    override fun showOnMinuteStartNotification(
        context: Context,
        remainingMinutes: Long
    ) {
        if (!isPushNotificationChannelInitialized) {
            createPushNotificationChannel(context)
        }

        val notification = NotificationCompat.Builder(context, pushNotificationChannel.id)
            .setContentTitle("Таймер")
            .setContentText("Осталось $remainingMinutes минут до завершения")
            .setSmallIcon(alarmIconId)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(PUSH_NOTIFICATION_ID, notification)
    }

}