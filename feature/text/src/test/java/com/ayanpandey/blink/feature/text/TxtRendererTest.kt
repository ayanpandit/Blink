package com.ayanpandey.blink.feature.text

import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TxtRendererTest {

    private val fileResolver: FileResolver = mockk()
    private val logger: BlinkLogger = mockk(relaxed = true)

    @Test
    fun `TxtRenderer supports TXT type`() {
        val renderer = TxtRenderer(fileResolver, logger)
        assertEquals(DocumentType.TXT, renderer.supportedType)
    }

    @Test
    fun `CsvRenderer supports CSV type`() {
        val renderer = CsvRenderer(fileResolver, logger)
        assertEquals(DocumentType.CSV, renderer.supportedType)
    }

    @Test
    fun `TxtRenderer has correct name`() {
        val renderer = TxtRenderer(fileResolver, logger)
        assertEquals("Blink Text Renderer", renderer.name)
    }

    @Test
    fun `CsvRenderer has correct name`() {
        val renderer = CsvRenderer(fileResolver, logger)
        assertEquals("Blink CSV Renderer", renderer.name)
    }
}
