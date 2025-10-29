package com.example.mobilebugsgame

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
class MobileBugsGameApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MobileBugsGameApplication)
            modules(appModule)
        }
    }
}