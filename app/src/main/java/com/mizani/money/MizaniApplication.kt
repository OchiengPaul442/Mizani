package com.mizani.money

import android.app.Application
import com.mizani.money.di.AppContainer

class MizaniApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // SQLCipher's native library must be loaded before the encrypted database is opened.
        System.loadLibrary("sqlcipher")
        container = AppContainer(this)
    }
}
