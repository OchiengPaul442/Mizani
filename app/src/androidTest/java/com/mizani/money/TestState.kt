package com.mizani.money

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** Shared cleanup for instrumented tests. Only test-owned app state is touched. */
object TestState {
    fun reset() {
        val app = ApplicationProvider.getApplicationContext<MizaniApplication>()
        runBlocking {
            app.container.awaitSqlCipherReady()
            app.container.dailyReminderScheduler.cancel()
            app.container.settingsRepository.resetAll()
            withContext(Dispatchers.IO) {
                app.container.database.clearAllTables()
            }
        }
    }
}
