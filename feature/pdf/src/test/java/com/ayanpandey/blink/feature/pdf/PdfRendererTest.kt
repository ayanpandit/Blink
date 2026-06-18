package com.ayanpandey.blink.feature.pdf

import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfRendererTest {

    private val fileResolver = mockk<FileResolver>(relaxed = true)
    private val logger = mockk<BlinkLogger>(relaxed = true)

    @Test
    fun testPdfRendererProperties() {
        val renderer = PdfRenderer(fileResolver, logger)
        assertEquals(DocumentType.PDF, renderer.supportedType)
        assertEquals("PDFium Native Renderer", renderer.name)
    }
}
