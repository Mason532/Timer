package com.timer.services

import android.content.Context

interface SystemTimerTimeStorage {
    fun saveSystemTimerTime(context: Context)
    fun restoreSystemTimerTime(context: Context): Long
    fun removeSystemTimerTime(context: Context)
}

interface RemainingTimerTimeStorage {
    fun saveRemainingTimerTime(context: Context, time: Long)
    fun restoreRemainingTimerTime(context: Context): Long
    fun removeRemainingTimerTime(context: Context)
}

interface TimerStoppedFlagStorage {
    fun saveTimerStoppedFlag(context: Context, flag: Boolean)
    fun restoreTimerStoppedFlag(context: Context): Boolean
    fun removeTimerStoppedFlag(context: Context)
}

interface TimerStorageTimer: SystemTimerTimeStorage, RemainingTimerTimeStorage, TimerStoppedFlagStorage

object TimerSharedPrefHelper: TimerStorageTimer {
    private const val PREF_NAME = "timer_prefs"
    private const val KEY_SYSTEM_TIME = "system_time"
    private const val KEY_REMAINING_TIME = "remaining_time"
    private const val KEY_TIMER_STOOPED_FLAG = "timer_stopped_flag"

    override fun saveSystemTimerTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_SYSTEM_TIME, System.currentTimeMillis())
            .apply()
    }

    override fun restoreSystemTimerTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_SYSTEM_TIME, 0)
    }

    override fun removeSystemTimerTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SYSTEM_TIME).apply()
    }

    override fun saveRemainingTimerTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_REMAINING_TIME, time)
            .apply()
    }

    override fun restoreRemainingTimerTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_REMAINING_TIME, 0)
    }

    override fun removeRemainingTimerTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_REMAINING_TIME).apply()
    }

    override fun saveTimerStoppedFlag(context: Context, flag: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_TIMER_STOOPED_FLAG, flag)
            .apply()
    }

    override fun restoreTimerStoppedFlag(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TIMER_STOOPED_FLAG, false)
    }

    override fun removeTimerStoppedFlag(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TIMER_STOOPED_FLAG).apply()
    }
}
