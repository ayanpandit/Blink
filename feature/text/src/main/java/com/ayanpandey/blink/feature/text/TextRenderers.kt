package com.ayanpandey.blink.feature.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.core.ui.ComposableDocumentRenderer
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver

class TxtRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.TXT
    override val name: String = "Blink Text Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        TxtViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}

class CsvRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.CSV
    override val name: String = "Blink CSV Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        CsvViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}
