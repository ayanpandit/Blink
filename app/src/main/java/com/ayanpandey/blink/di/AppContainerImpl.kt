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
import com.ayanpandey.blink.feature.pdf.PdfRenderer
import com.ayanpandey.blink.feature.word.WordRenderer
import com.ayanpandey.blink.feature.word.DocRenderer
import com.ayanpandey.blink.feature.excel.ExcelRenderer
import com.ayanpandey.blink.feature.excel.XlsRenderer
import com.ayanpandey.blink.feature.ppt.PptxRenderer
import com.ayanpandey.blink.feature.ppt.PptRenderer
import com.ayanpandey.blink.feature.text.TxtRenderer
import com.ayanpandey.blink.feature.text.CsvRenderer


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

    override val documentRepository: com.ayanpandey.blink.domain.repository.DocumentRepository by lazy {
        com.ayanpandey.blink.core.file.repository.DocumentRepositoryImpl(context, logger)
    }

    override val renderers: List<com.ayanpandey.blink.domain.contract.DocumentRenderer> by lazy {
        listOf(
            PdfRenderer(fileResolver, logger),
            WordRenderer(fileResolver, logger),
            DocRenderer(fileResolver, logger),
            ExcelRenderer(fileResolver, logger),
            XlsRenderer(fileResolver, logger),
            PptxRenderer(fileResolver, logger),
            PptRenderer(fileResolver, logger),
            TxtRenderer(fileResolver, logger),
            CsvRenderer(fileResolver, logger),
        )
    }
}

