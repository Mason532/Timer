package com.timer.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.timer.R
import com.timer.services.TimerAlarmService.Companion.ACTION_START_TIMER_FINISHED_ALARM
import com.timer.services.TimerAlarmService.Companion.INTENT_EXTRA_KEY_TIME_AGO_FINISHED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TimerService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESET = "ACTION_RESET"
        const val ACTION_AFTER_REBOOT = "ACTION_AFTER_REBOOT"
        const val ACTION_RESUME = "ACTION_RESUME"

        const val INTENT_EXTRA_KEY_TIME_IN_MINUTES = "EXTRA_TIME_IN_MINUTES"

        private val _remainingTime = MutableStateFlow(0L)
        val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

        private const val SERVICE_CHANNEL_ID = "timer_service"
        private const val SERVICE_CHANNEL_NAME = "Timer Service"

        private const val SERVICE_NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (flags and START_FLAG_REDELIVERY != 0) {
                    onAfterReboot()
                } else {
                    val timerTime = intent.getIntExtra(INTENT_EXTRA_KEY_TIME_IN_MINUTES, 0) * 60 * 1000L
                    setTimer(
                        totalMillis = timerTime,
                        onTimerFinish = {
                            startTimerFinishedAlarmService()
                            endTimerForegroundService()
                        }
                    )
                    startTimerForegroundService()
                }
            }

            ACTION_RESUME -> {
                setTimer(
                    onTimerFinish = {
                        startTimerFinishedAlarmService()
                        endTimerForegroundService()
                    }
                )
                startTimerForegroundService()
            }

            ACTION_STOP -> {
                stopTimerForegroundService()
            }

            ACTION_RESET -> {
                resetTimerForeground()
            }

            ACTION_AFTER_REBOOT -> {
                onAfterReboot()
            }
        }
        return  START_REDELIVER_INTENT
    }

    private fun startTimerForegroundService() {
        val foregroundServiceRequiredNotification = NotificationCompat.Builder(
    this, serviceNotificationChannel.id
        )
            .setContentTitle("Таймер")
            .setContentText("Приложение Таймер работает в фоне")
            .setSmallIcon(R.drawable.alarm_on)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(
            SERVICE_NOTIFICATION_ID,
            foregroundServiceRequiredNotification
        )
    }

    private fun stopTimerForegroundService() {
        timerJob?.cancel()
        remainingTimeCollectorJob?.cancel()
        TimerSharedPrefHelper.saveTimerStoppedFlag(context = this, flag = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun resetTimerForeground() {
        timerJob?.cancel()
        remainingTimeCollectorJob?.cancel()
        endTimerForegroundService()
    }

    private fun endTimerForegroundService() {
        TimerSharedPrefHelper.removeRemainingTimerTime(context = this)
        TimerSharedPrefHelper.removeSystemTimerTime(context = this)
        TimerSharedPrefHelper.removeTimerStoppedFlag(context = this)
        _remainingTime.value = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onAfterReboot() {
        val restoredRemainingTime = TimerSharedPrefHelper.restoreRemainingTimerTime(
            context = this
        )

        val restoredIsLastTimerStoppedFlag = TimerSharedPrefHelper.restoreTimerStoppedFlag(
            context = this
        )

        if (!restoredIsLastTimerStoppedFlag && restoredRemainingTime > 0) {
            val restoredSystemTime = TimerSharedPrefHelper.restoreSystemTimerTime(context = this)
            val resumedRemainingTime = calculateResumedRemainingTime(
                savedSystemTime = restoredSystemTime,
                savedRemainingTime = restoredRemainingTime
            )

            if (resumedRemainingTime > 0) {
                startTimerForegroundService()

                setTimer(
                    totalMillis = resumedRemainingTime,
                    onTimerFinish = {
                        startTimerFinishedAlarmService()
                        endTimerForegroundService()
                    }
                )

                TimerNotificationHelper.showOnResumeServiceNotification(
                    context = this,
                    restoredSystemTime = restoredSystemTime,
                    currentSystemTime = System.currentTimeMillis()
                )

            } else {
                startTimerFinishedAlarmService(timeAgoFinished = -resumedRemainingTime)
                endTimerForegroundService()
            }

        } else {
            endTimerForegroundService()
        }
    }

    private var timerJob: Job? = null
    private var remainingTimeCollectorJob: Job? = null

    private fun setUpRemainingTimeStateFlowCollector() {
        remainingTimeCollectorJob = CoroutineScope(Dispatchers.IO).launch {
            remainingTime.collectLatest {
                TimerSharedPrefHelper.saveSystemTimerTime(context = this@TimerService)
                TimerSharedPrefHelper.saveRemainingTimerTime(
                    context = this@TimerService, time = it
                )
            }
        }
    }

    private fun setTimer(totalMillis: Long = _remainingTime.value, onTimerFinish: () -> Unit) {
        _remainingTime.value = totalMillis

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            setUpRemainingTimeStateFlowCollector()
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value -= 1000

                val remainingMillis = _remainingTime.value
                val remainingSeconds = remainingMillis / 1000

                if (remainingSeconds % 60 == 0L) {
                    val remainingMinutes = remainingMillis / 60000
                    if (remainingMinutes > 0) {
                        TimerNotificationHelper.showOnMinuteStartNotification(
                            context = this@TimerService,
                            remainingMinutes = remainingMinutes
                        )
                    }
                }
            }
            timerJob?.cancel()
            remainingTimeCollectorJob?.cancel()
            onTimerFinish()
        }
    }

    private fun startTimerFinishedAlarmService(timeAgoFinished: Long = 0L) {
        val intent = Intent(this, TimerAlarmService::class.java).apply {
            action = ACTION_START_TIMER_FINISHED_ALARM
            if (timeAgoFinished > 0)
                putExtra(INTENT_EXTRA_KEY_TIME_AGO_FINISHED, timeAgoFinished)
        }
        this.startService(intent)
    }

    private val serviceNotificationChannel: NotificationChannel by lazy {
        NotificationChannel(
            SERVICE_CHANNEL_ID, SERVICE_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            getSystemService(NotificationManager::class.java).createNotificationChannel(this)
        }
    }

    private fun calculateResumedRemainingTime(
        savedSystemTime: Long,
        savedRemainingTime: Long
    ) : Long {
        val systemTimeDiff = System.currentTimeMillis() - savedSystemTime
        return savedRemainingTime - systemTimeDiff
    }

   private fun onBeforeActiveServiceStopped() {
       val remainingTime = _remainingTime.value
       if (remainingTime > 0) {
           TimerSharedPrefHelper.saveSystemTimerTime(context = this)
           TimerSharedPrefHelper.saveRemainingTimerTime(context = this, time = remainingTime)
       } else {
           endTimerForegroundService()
       }
   }

   override fun onTaskRemoved(rootIntent: Intent?) {
       super.onTaskRemoved(rootIntent)
       Log.d("____serviceRemoved", "+")
       onBeforeActiveServiceStopped()
   }


   override fun onDestroy() {
       super.onDestroy()
       Log.d("____serviceDestroyed", "+")
       onBeforeActiveServiceStopped()
   }

   override fun onBind(intent: Intent?): IBinder? {
       TODO("Not yet implemented")
   }

}
