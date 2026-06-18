package com.ayanpandey.blink.feature.pdf

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayanpandey.blink.core.ui.ComposableDocumentRenderer
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import com.ayanpandey.blink.core.common.logging.BlinkLogger

class PdfRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.PDF
    override val name: String = "PDFium Native Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        PdfViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier
        )
    }

    @Composable
    override fun Render(
        document: Document,
        initialPosition: Int,
        onPositionChanged: (Int) -> Unit,
        modifier: Modifier
    ) {
        PdfViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            initialPage = if (initialPosition > 0) initialPosition else 1,
            onPageChanged = onPositionChanged,
            modifier = modifier
        )
    }
}
