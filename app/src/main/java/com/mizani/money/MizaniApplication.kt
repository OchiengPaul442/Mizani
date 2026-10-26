package com.mizani.money

import android.app.Application
import com.mizani.money.di.AppContainer
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MizaniApplication : Application() {

    private val sqlCipherReady = CompletableDeferred<Unit>()
    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, sqlCipherReady)
        // Native library loading can be slow on entry-level devices; database initialization waits
        // for this signal on IO, while the Activity can draw its splash immediately.
        initializationScope.launch {
            try {
                System.loadLibrary("sqlcipher")
                sqlCipherReady.complete(Unit)
            } catch (error: Exception) {
                sqlCipherReady.completeExceptionally(error)
            } catch (error: LinkageError) {
                sqlCipherReady.completeExceptionally(error)
            }
        }
        initializationScope.launch {
            try {
                val settings = container.settingsRepository.settings.first()
                container.dailyReminderScheduler.ensureScheduled(settings)
            } catch (_: Exception) {
                // The next app launch repairs the schedule; never log local finance data.
                Log.w("MizaniApplication", "Could not restore the daily reminder schedule")
            }
        }
    }
}
