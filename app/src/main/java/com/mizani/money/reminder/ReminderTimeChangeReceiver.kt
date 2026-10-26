package com.mizani.money.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Re-aligns the user's chosen wall-clock time after system clock or timezone changes. */
class ReminderTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = appContext as com.mizani.money.MizaniApplication
                val settingsRepository = app.container.settingsRepository
                val currentSettings = settingsRepository.settings.first()
                if (currentSettings.dailyReminderEnabled) {
                    settingsRepository.bumpDailyReminderScheduleVersion()
                }
                val settings = settingsRepository.settings.first()
                app.container.dailyReminderScheduler.update(settings)
            } catch (_: Exception) {
                Log.w(TAG, "Could not realign the daily reminder schedule")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReminderTimeChange"
        val ACTIONS = setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
        )
    }
}
