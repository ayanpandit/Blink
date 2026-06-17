package com.ayanpandey.blink.core.file.factory

import com.ayanpandey.blink.core.common.error.AppError
import com.ayanpandey.blink.core.common.error.AppErrorException
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.DocumentType
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileDescriptor

class DocumentFactoryImplTest {

    private val logger = mockk<BlinkLogger>(relaxed = true)
    private val factory = DocumentFactoryImpl(logger)

    @Test
    fun createDocument_pdf_success() {
        val result = factory.createDocument(
            fileDescriptor = FileDescriptor(),
            uriString = "content://path/to/doc.pdf",
            displayName = "doc.pdf",
            mimeType = "application/pdf",
            size = 1024L,
            lastModified = 123456789L
        )

        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("content://path/to/doc.pdf", doc.uri)
        assertEquals("doc.pdf", doc.displayName)
        assertEquals("application/pdf", doc.mimeType)
        assertEquals("pdf", doc.extension)
        assertEquals(1024L, doc.size)
        assertEquals(123456789L, doc.lastModified)
        assertEquals(DocumentType.PDF, doc.documentType)
    }

    @Test
    fun createDocument_docx_byMimeType_success() {
        // Test case where display name does not have extension, but mimetype is docx
        val result = factory.createDocument(
            fileDescriptor = FileDescriptor(),
            uriString = "content://path/to/doc",
            displayName = "doc",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            size = 2048L,
            lastModified = 0L
        )

        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals(DocumentType.DOCX, doc.documentType)
        assertEquals("", doc.extension)
    }

    @Test
    fun createDocument_unsupportedFormat_failure() {
        val result = factory.createDocument(
            fileDescriptor = FileDescriptor(),
            uriString = "content://path/to/image.png",
            displayName = "image.png",
            mimeType = "image/png",
            size = 512L,
            lastModified = 0L
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppErrorException)
        assertEquals(AppError.DocumentError.UnsupportedDocument, (exception as AppErrorException).error)
    }
}
