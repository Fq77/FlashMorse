package com.flashmorse.app

import android.app.Application
import com.flashmorse.app.di.AppContainer

class FlashMorseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
