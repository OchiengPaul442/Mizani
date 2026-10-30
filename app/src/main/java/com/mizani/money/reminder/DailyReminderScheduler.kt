package com.mizani.money.reminder

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mizani.money.core.time.DailyReminderSchedule
import com.mizani.money.data.repo.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Keeps at most one local reminder check scheduled for a target date. */
class DailyReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    suspend fun ensureScheduled(settings: AppSettings) = withContext(Dispatchers.IO) {
        if (!hasAnyReminderEnabled(settings)) {
            cancelExisting()
            cancelLegacyPeriodicWork()
            DailyReminderNotifications.cancel(appContext)
            return@withContext
        }
        // Remove the early development periodic request before adopting the wall-clock one-shot.
        cancelLegacyPeriodicWork()
        enqueueNextOccurrence(settings)
    }

    /** Replaces pending work after an explicit settings or local-time change. */
    suspend fun update(settings: AppSettings) = withContext(Dispatchers.IO) {
        cancelExisting()
        cancelLegacyPeriodicWork()
        DailyReminderNotifications.cancel(appContext)
        if (hasAnyReminderEnabled(settings)) enqueueNextOccurrence(settings)
    }

    suspend fun cancel() = withContext(Dispatchers.IO) {
        cancelExisting()
        cancelLegacyPeriodicWork()
        DailyReminderNotifications.cancel(appContext)
    }

    /** Cancels daily/habit work and every app notification during data deletion or restore. */
    suspend fun cancelAll() = withContext(Dispatchers.IO) {
        cancelExisting()
        cancelLegacyPeriodicWork()
        DailyReminderNotifications.cancelAll(appContext)
    }

    internal suspend fun scheduleNext(settings: AppSettings, scheduleVersion: Long) =
        withContext(Dispatchers.IO) {
            if (!hasAnyReminderEnabled(settings) || settings.dailyReminderScheduleVersion != scheduleVersion) {
                return@withContext
            }
            enqueueNextOccurrence(settings)
        }

    private suspend fun enqueueNextOccurrence(settings: AppSettings) {
        val now = ZonedDateTime.now()
        val next = DailyReminderSchedule.nextOccurrence(now, settings.dailyReminderMinuteOfDay)
        val delayMillis = Duration.between(now, next).toMillis().coerceAtLeast(0L)
        val request = reminderRequest(settings, next.toLocalDate().toEpochDay(), delayMillis)
        workManager.enqueueUniqueWork(
            workName(settings.dailyReminderScheduleVersion, next.toLocalDate().toEpochDay()),
            ExistingWorkPolicy.KEEP,
            request,
        ).result.get()
    }

    private fun reminderRequest(
        settings: AppSettings,
        targetEpochDay: Long,
        delayMillis: Long,
    ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<DailyTransactionReminderWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(
            workDataOf(
                INPUT_REMINDER_MINUTE_OF_DAY to settings.dailyReminderMinuteOfDay,
                INPUT_TARGET_EPOCH_DAY to targetEpochDay,
                INPUT_SCHEDULE_VERSION to settings.dailyReminderScheduleVersion,
            ),
        )
        .addTag(WORK_TAG)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
        .build()

    private suspend fun cancelExisting() {
        workManager.cancelAllWorkByTag(WORK_TAG).result.get()
    }

    private suspend fun cancelLegacyPeriodicWork() {
        workManager.cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME).result.get()
    }

    private fun workName(scheduleVersion: Long, epochDay: Long): String =
        "$WORK_NAME_PREFIX-$scheduleVersion-$epochDay"

    private fun hasAnyReminderEnabled(settings: AppSettings): Boolean =
        settings.dailyReminderEnabled || settings.habitNotificationsEnabled

    companion object {
        const val WORK_TAG = "mizani_daily_transaction_reminder"
        private const val WORK_NAME_PREFIX = "mizani_daily_transaction_reminder"
        private const val LEGACY_PERIODIC_WORK_NAME = "mizani_daily_transaction_reminder"
        internal const val INPUT_REMINDER_MINUTE_OF_DAY = "reminder_minute_of_day"
        internal const val INPUT_TARGET_EPOCH_DAY = "reminder_target_epoch_day"
        internal const val INPUT_SCHEDULE_VERSION = "reminder_schedule_version"
        private const val RETRY_BACKOFF_MINUTES = 15L
    }
}
