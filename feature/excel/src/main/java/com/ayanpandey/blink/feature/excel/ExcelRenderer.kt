package com.ayanpandey.blink.feature.excel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.core.ui.ComposableDocumentRenderer
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver

class ExcelRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.XLSX
    override val name: String = "Apache POI Excel Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        ExcelViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}

class XlsRenderer(
    private val fileResolver: FileResolver,
    private val logger: BlinkLogger,
) : ComposableDocumentRenderer {

    override val supportedType: DocumentType = DocumentType.XLS
    override val name: String = "Apache POI XLS Renderer"

    @Composable
    override fun Render(document: Document, modifier: Modifier) {
        ExcelViewer(
            document = document,
            fileResolver = fileResolver,
            logger = logger,
            modifier = modifier,
        )
    }
}
