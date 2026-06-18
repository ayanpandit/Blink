package com.ayanpandey.blink.feature.word

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.core.ui.ComposableDocumentRenderer
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver

class WordRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    // Primary type — DOCX. DOC handled via separate renderer instance.
    override val supportedType: DocumentType = DocumentType.DOCX
    override val name: String = "Apache POI Word Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        WordViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}

class DocRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.DOC
    override val name: String = "Apache POI DOC Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        WordViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}
