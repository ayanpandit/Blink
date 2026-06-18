package com.ayanpandey.blink.feature.word

import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class WordRendererTest {

    private val fileResolver: FileResolver = mockk()
    private val logger: BlinkLogger = mockk(relaxed = true)

    @Test
    fun `WordRenderer supports DOCX type`() {
        val renderer = WordRenderer(fileResolver, logger)
        assertEquals(DocumentType.DOCX, renderer.supportedType)
    }

    @Test
    fun `DocRenderer supports DOC type`() {
        val renderer = DocRenderer(fileResolver, logger)
        assertEquals(DocumentType.DOC, renderer.supportedType)
    }

    @Test
    fun `WordRenderer has correct name`() {
        val renderer = WordRenderer(fileResolver, logger)
        assertEquals("Apache POI Word Renderer", renderer.name)
    }

    @Test
    fun `DocRenderer has correct name`() {
        val renderer = DocRenderer(fileResolver, logger)
        assertEquals("Apache POI DOC Renderer", renderer.name)
    }
}
