package com.mizani.money.reminder

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mizani.money.data.repo.LedgerSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Schedules one generic local reminder per unpaid finite commitment. */
class CommitmentReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    suspend fun reconcile(snapshot: LedgerSnapshot, today: LocalDate = LocalDate.now()) = withContext(Dispatchers.IO) {
        val desired = snapshot.commitmentProgress()
            .asSequence()
            .filter { progress ->
                !progress.commitment.archived && progress.remainingMinor > 0L &&
                    progress.commitment.reminderDaysBefore > 0
            }
            .mapNotNull { progress ->
                val commitment = progress.commitment
                val due = LocalDate.ofEpochDay(commitment.dueEpochDay)
                val reminderDate = due.minusDays(commitment.reminderDaysBefore.toLong())
                if (reminderDate.isBefore(today)) null else {
                    commitment.id to ReminderSpec(commitment.dueEpochDay, commitment.reminderDaysBefore)
                }
            }
            .toMap()

        val existing = workManager.getWorkInfosByTag(WORK_TAG).get()
        val desiredNames = desired.mapTo(mutableSetOf()) { (id, spec) -> workName(id, spec) }
        existing.forEach { info ->
            val nameTag = info.tags.firstOrNull { it.startsWith("$WORK_NAME_PREFIX-") }
            if (nameTag !in desiredNames || info.state == WorkInfo.State.CANCELLED) {
                workManager.cancelWorkById(info.id).result.get()
            }
        }

        desired.forEach { (id, spec) ->
            val name = workName(id, spec)
            val alreadyScheduled = existing.any { info ->
                name in info.tags &&
                    info.state != WorkInfo.State.CANCELLED && info.state != WorkInfo.State.FAILED
            }
            if (!alreadyScheduled) enqueue(id, spec)
        }
    }

    private suspend fun enqueue(commitmentId: String, spec: ReminderSpec) {
        val dueDate = LocalDate.ofEpochDay(spec.dueEpochDay)
        val reminderDate = dueDate.minusDays(spec.daysBefore.toLong())
        val now = ZonedDateTime.now()
        val target = reminderDate.atTime(9, 0).atZone(now.zone)
        val delayMillis = Duration.between(now, target).toMillis().coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<CommitmentReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    INPUT_COMMITMENT_ID to commitmentId,
                    INPUT_DUE_EPOCH_DAY to spec.dueEpochDay,
                    INPUT_DAYS_BEFORE to spec.daysBefore,
                ),
            )
            .addTag(WORK_TAG)
            .addTag(workName(commitmentId, spec))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(workName(commitmentId, spec), ExistingWorkPolicy.KEEP, request).result.get()
    }

    private fun workName(commitmentId: String, spec: ReminderSpec): String =
        "$WORK_NAME_PREFIX-$commitmentId-${spec.dueEpochDay}-${spec.daysBefore}"

    private data class ReminderSpec(val dueEpochDay: Long, val daysBefore: Int)

    companion object {
        const val WORK_TAG = "mizani_commitment_reminder"
        private const val WORK_NAME_PREFIX = "mizani_commitment_reminder"
        internal const val INPUT_COMMITMENT_ID = "commitment_id"
        internal const val INPUT_DUE_EPOCH_DAY = "commitment_due_epoch_day"
        internal const val INPUT_DAYS_BEFORE = "commitment_reminder_days_before"
        private const val RETRY_BACKOFF_MINUTES = 15L
    }
}
