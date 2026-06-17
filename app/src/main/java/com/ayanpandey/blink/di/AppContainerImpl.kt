package com.ayanpandey.blink.di

import android.content.Context
import com.ayanpandey.blink.BuildConfig
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.core.common.logging.DebugLogger
import com.ayanpandey.blink.core.common.logging.ReleaseLogger
import com.ayanpandey.blink.core.file.resolver.FileResolverImpl
import com.ayanpandey.blink.domain.di.DomainContainer
import com.ayanpandey.blink.domain.repository.FileResolver

class AppContainerImpl(private val context: Context) : DomainContainer {
    override val logger: BlinkLogger by lazy {
        if (BuildConfig.DEBUG) {
            DebugLogger()
        } else {
            ReleaseLogger()
        }
    }

    override val fileResolver: FileResolver by lazy {
        FileResolverImpl(context, logger)
    }
}
