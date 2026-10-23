package com.mizani.money

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File

/** Shared cleanup for instrumented tests. Only test-owned app state is touched. */
object TestState {
    fun reset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("mizani.db")
        context.getSharedPreferences("mizani_database_key", Context.MODE_PRIVATE)
            .edit().clear().commit()
        File(context.filesDir, "datastore/mizani_settings.preferences_pb").delete()
    }
}
