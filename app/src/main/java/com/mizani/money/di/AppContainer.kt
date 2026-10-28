package com.mizani.money.di

import android.app.Application
import com.mizani.money.data.local.DatabaseKeyManager
import com.mizani.money.data.local.MizaniDatabase
import com.mizani.money.data.repo.FinanceRepository
import com.mizani.money.data.repo.ExchangeRateRepository
import com.mizani.money.data.repo.SettingsRepository
import com.mizani.money.domain.export.ExportManager
import com.mizani.money.reminder.DailyReminderScheduler
import com.mizani.money.reminder.CommitmentReminderScheduler
import kotlinx.coroutines.Deferred

/**
 * Explicit, constructor-injected dependency container. Keeping DI manual for V1 removes
 * annotation-processing risk and makes the wiring auditable.
 */
class AppContainer(
    private val application: Application,
    private val sqlCipherReady: Deferred<Unit>,
) {

    suspend fun awaitSqlCipherReady() = sqlCipherReady.await()

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }

    val database: MizaniDatabase by lazy {
        MizaniDatabase.build(application, DatabaseKeyManager.loadOrCreate(application))
    }

    val financeRepository: FinanceRepository by lazy { FinanceRepository(database) }

    val exchangeRateRepository: ExchangeRateRepository by lazy { ExchangeRateRepository(application) }

    val exportManager: ExportManager by lazy { ExportManager(application, financeRepository) }

    val dailyReminderScheduler: DailyReminderScheduler by lazy { DailyReminderScheduler(application) }

    val commitmentReminderScheduler: CommitmentReminderScheduler by lazy { CommitmentReminderScheduler(application) }
}
