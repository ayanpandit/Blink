package com.ayanpandey.blink

import android.app.Application
import com.ayanpandey.blink.core.common.di.AppContainer
import com.ayanpandey.blink.di.AppContainerImpl

class BlinkApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl()
        container.logger.i(TAG, "BlinkApplication initialized successfully.")
    }

    companion object {
        private const val TAG = "BlinkApplication"
    }
}
