package com.ayanpandey.blink.feature.ppt

import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PptRendererTest {

    private val fileResolver: FileResolver = mockk()
    private val logger: BlinkLogger = mockk(relaxed = true)

    @Test
    fun `PptxRenderer supports PPTX type`() {
        val renderer = PptxRenderer(fileResolver, logger)
        assertEquals(DocumentType.PPTX, renderer.supportedType)
    }

    @Test
    fun `PptRenderer supports PPT type`() {
        val renderer = PptRenderer(fileResolver, logger)
        assertEquals(DocumentType.PPT, renderer.supportedType)
    }

    @Test
    fun `PptxRenderer has correct name`() {
        val renderer = PptxRenderer(fileResolver, logger)
        assertEquals("Apache POI PPTX Renderer", renderer.name)
    }

    @Test
    fun `PptRenderer has correct name`() {
        val renderer = PptRenderer(fileResolver, logger)
        assertEquals("Apache POI PPT Renderer", renderer.name)
    }
}
