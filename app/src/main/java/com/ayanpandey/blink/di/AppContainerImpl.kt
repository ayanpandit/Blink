package com.ayanpandey.blink.di

import android.content.Context
import com.ayanpandey.blink.BuildConfig
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.core.common.logging.DebugLogger
import com.ayanpandey.blink.core.common.logging.ReleaseLogger
import com.ayanpandey.blink.core.file.factory.DocumentFactoryImpl
import com.ayanpandey.blink.core.file.resolver.FileResolverImpl
import com.ayanpandey.blink.domain.contract.DocumentFactory
import com.ayanpandey.blink.domain.contract.DocumentViewer
import com.ayanpandey.blink.domain.di.DomainContainer
import com.ayanpandey.blink.domain.repository.FileResolver
import com.ayanpandey.blink.feature.viewer.DocumentViewerImpl

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

    val documentFactory: DocumentFactory by lazy {
        DocumentFactoryImpl(logger)
    }

    override val documentViewer: DocumentViewer by lazy {
        DocumentViewerImpl(fileResolver, documentFactory, logger)
    }
}

