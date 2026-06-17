package com.ayanpandey.blink.di

import com.ayanpandey.blink.BuildConfig
import com.ayanpandey.blink.core.common.di.AppContainer
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.core.common.logging.DebugLogger
import com.ayanpandey.blink.core.common.logging.ReleaseLogger

class AppContainerImpl : AppContainer {
    override val logger: BlinkLogger by lazy {
        if (BuildConfig.DEBUG) {
            DebugLogger()
        } else {
            ReleaseLogger()
        }
    }
}
