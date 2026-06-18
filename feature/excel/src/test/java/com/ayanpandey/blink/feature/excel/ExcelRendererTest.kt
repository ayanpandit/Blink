package com.ayanpandey.blink.feature.excel

import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ExcelRendererTest {

    private val fileResolver: FileResolver = mockk()
    private val logger: BlinkLogger = mockk(relaxed = true)

    @Test
    fun `ExcelRenderer supports XLSX type`() {
        val renderer = ExcelRenderer(fileResolver, logger)
        assertEquals(DocumentType.XLSX, renderer.supportedType)
    }

    @Test
    fun `XlsRenderer supports XLS type`() {
        val renderer = XlsRenderer(fileResolver, logger)
        assertEquals(DocumentType.XLS, renderer.supportedType)
    }

    @Test
    fun `ExcelRenderer has correct name`() {
        val renderer = ExcelRenderer(fileResolver, logger)
        assertEquals("Apache POI Excel Renderer", renderer.name)
    }

    @Test
    fun `XlsRenderer has correct name`() {
        val renderer = XlsRenderer(fileResolver, logger)
        assertEquals("Apache POI XLS Renderer", renderer.name)
    }
}
