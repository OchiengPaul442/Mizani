package com.mizani.money.di

import android.app.Application
import com.mizani.money.data.local.DatabaseKeyManager
import com.mizani.money.data.local.MizaniDatabase
import com.mizani.money.data.repo.FinanceRepository
import com.mizani.money.data.repo.SettingsRepository
import com.mizani.money.domain.export.ExportManager

/**
 * Explicit, constructor-injected dependency container. Keeping DI manual for V1 removes
 * annotation-processing risk and makes the wiring auditable.
 */
class AppContainer(private val application: Application) {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }

    val database: MizaniDatabase by lazy {
        MizaniDatabase.build(application, DatabaseKeyManager.loadOrCreate(application))
    }

    val financeRepository: FinanceRepository by lazy { FinanceRepository(database) }

    val exportManager: ExportManager by lazy { ExportManager(application, financeRepository) }
}
