package com.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timer.services.TimerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_AFTER_REBOOT
            }
            context.startService(serviceIntent)
        }
    }
}
