package com.mizani.money.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mizani.money.MizaniApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class CommitmentReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MizaniApplication ?: return Result.failure()
        return try {
            val commitmentId = inputData.getString(CommitmentReminderScheduler.INPUT_COMMITMENT_ID)
                ?: return Result.failure()
            val dueEpochDay = inputData.getLong(CommitmentReminderScheduler.INPUT_DUE_EPOCH_DAY, Long.MIN_VALUE)
            val daysBefore = inputData.getInt(CommitmentReminderScheduler.INPUT_DAYS_BEFORE, -1)
            if (dueEpochDay == Long.MIN_VALUE || daysBefore !in 1..30) return Result.failure()

            val container = app.container
            container.awaitSqlCipherReady()
            val snapshot = container.financeRepository.snapshot.first()
            val progress = snapshot.commitmentProgress().firstOrNull { it.commitment.id == commitmentId }
                ?: return Result.success()
            val reminderDate = LocalDate.ofEpochDay(dueEpochDay).minusDays(daysBefore.toLong())
            val today = LocalDate.now()
            val dueDate = LocalDate.ofEpochDay(dueEpochDay)
            val current = progress.commitment
            if (current.archived || current.dueEpochDay != dueEpochDay ||
                current.reminderDaysBefore != daysBefore || progress.remainingMinor <= 0L ||
                today.isAfter(dueDate)
            ) return Result.success()
            if (today.isBefore(reminderDate)) return Result.retry()

            DailyReminderNotifications.postCommitment(applicationContext, commitmentId)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w(TAG, "Commitment reminder check failed; WorkManager will retry")
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "CommitmentReminder"
    }
}
