package com.mizani.money.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mizani.money.MizaniApplication
import com.mizani.money.core.time.DailyReminderSchedule
import com.mizani.money.domain.notifications.HabitNotificationEngine
import com.mizani.money.domain.notifications.HabitNotificationPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZonedDateTime

class DailyTransactionReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MizaniApplication ?: return Result.failure()
        return try {
            val container = app.container
            val settingsRepository = container.settingsRepository
            val settings = settingsRepository.settings.first()
            if (!settings.dailyReminderEnabled && !settings.habitNotificationsEnabled) return Result.success()

            val scheduleVersion = inputData.getLong(
                DailyReminderScheduler.INPUT_SCHEDULE_VERSION,
                -1L,
            )
            val reminderMinute = inputData.getInt(
                DailyReminderScheduler.INPUT_REMINDER_MINUTE_OF_DAY,
                -1,
            )
            val targetEpochDay = inputData.getLong(
                DailyReminderScheduler.INPUT_TARGET_EPOCH_DAY,
                -1L,
            )
            if (scheduleVersion != settings.dailyReminderScheduleVersion ||
                reminderMinute != settings.dailyReminderMinuteOfDay ||
                targetEpochDay < 0L
            ) {
                return Result.success()
            }

            val now = ZonedDateTime.now()
            val today = now.toLocalDate()
            // WorkManager may run a delayed one-shot after its target date (for example after
            // Doze or battery restrictions). Do not silently drop the reminder in that case;
            // wait only when it runs early, then check the current day as usual.
            if (today.isBefore(LocalDate.ofEpochDay(targetEpochDay))) {
                return Result.retry()
            }

            val scheduledFor = DailyReminderSchedule.occurrenceOn(
                targetEpochDay,
                settings.dailyReminderMinuteOfDay,
                now.zone,
            )
            // Compare the full instant rather than only hour/minute so a worker that runs a few
            // milliseconds early does not fall into the 15-minute retry backoff window.
            if (now.isBefore(scheduledFor)) return Result.retry()

            var habitNotificationPosted = false
            val needsDatabase = settings.habitNotificationsEnabled ||
                (settings.dailyReminderEnabled && settings.lastDailyReminderEpochDay != today.toEpochDay())
            if (needsDatabase) {
                container.awaitSqlCipherReady()
                val snapshot = container.financeRepository.snapshot.first()
                val hasTrackedMoneyToday = if (settings.dailyReminderEnabled) {
                    container.database.transactionDao().hasIncomeExpenseOrRefundOn(today.toEpochDay())
                } else {
                    false
                }

                // Recheck opt-in after the encrypted read so a concurrent settings change cannot
                // leave a stale worker posting after the user disabled the category. One daily
                // work item may deliver one habit check-in OR the regular daily check-in, avoiding
                // a burst of two reminders for the same local run.
                val latestSettings = settingsRepository.settings.first()
                val scheduleIsStillCurrent = latestSettings.dailyReminderScheduleVersion == scheduleVersion &&
                    latestSettings.dailyReminderMinuteOfDay == reminderMinute
                if (scheduleIsStillCurrent && latestSettings.habitNotificationsEnabled &&
                    latestSettings.lastHabitNotificationEpochDay != today.toEpochDay() &&
                    DailyReminderNotifications.canPost(applicationContext)
                ) {
                    val preferences = HabitNotificationPreferences(
                        enabled = latestSettings.habitNotificationsEnabled,
                        spendingEnabled = latestSettings.habitSpendingNotificationsEnabled,
                        incomeEnabled = latestSettings.habitIncomeNotificationsEnabled,
                        investmentEnabled = latestSettings.habitInvestmentNotificationsEnabled,
                        debtEnabled = latestSettings.habitDebtNotificationsEnabled,
                        goalEnabled = latestSettings.habitGoalNotificationsEnabled,
                    )
                    val kind = HabitNotificationEngine.choose(
                        snapshot = snapshot,
                        today = today,
                        preferences = preferences,
                        lastShownEpochDay = latestSettings.lastHabitNotificationEpochDay,
                    )
                    if (kind != null && DailyReminderNotifications.postHabit(applicationContext, kind)) {
                        settingsRepository.markHabitNotificationShown(today.toEpochDay())
                        habitNotificationPosted = true
                    }
                }

                if (!habitNotificationPosted && scheduleIsStillCurrent &&
                    latestSettings.dailyReminderEnabled &&
                    latestSettings.lastDailyReminderEpochDay != today.toEpochDay() &&
                    !hasTrackedMoneyToday && DailyReminderNotifications.canPost(applicationContext) &&
                    DailyReminderNotifications.post(applicationContext)
                ) {
                    settingsRepository.markDailyReminderShown(today.toEpochDay())
                }
            }
            scheduleNextIfCurrent(settingsRepository, scheduleVersion)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w(TAG, "Daily reminder check failed; WorkManager will retry")
            Result.retry()
        }
    }

    private suspend fun scheduleNextIfCurrent(
        settingsRepository: com.mizani.money.data.repo.SettingsRepository,
        scheduleVersion: Long,
    ) {
        val latestSettings = settingsRepository.settings.first()
        if ((latestSettings.dailyReminderEnabled || latestSettings.habitNotificationsEnabled) &&
            latestSettings.dailyReminderScheduleVersion == scheduleVersion
        ) {
            val app = applicationContext as MizaniApplication
            app.container.dailyReminderScheduler.scheduleNext(latestSettings, scheduleVersion)
        }
    }

    private companion object {
        const val TAG = "DailyReminder"
    }
}
