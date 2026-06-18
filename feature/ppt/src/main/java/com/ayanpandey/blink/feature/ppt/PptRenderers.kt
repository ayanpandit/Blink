package com.ayanpandey.blink.feature.ppt

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.core.ui.ComposableDocumentRenderer
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver

class PptxRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.PPTX
    override val name: String = "Apache POI PPTX Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        PptViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}

class PptRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.PPT
    override val name: String = "Apache POI PPT Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        PptViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}
