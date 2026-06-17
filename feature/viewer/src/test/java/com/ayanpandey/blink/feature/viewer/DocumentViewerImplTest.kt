package com.ayanpandey.blink.feature.viewer

import com.ayanpandey.blink.core.common.error.AppError
import com.ayanpandey.blink.core.common.error.AppErrorException
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentState
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.model.DocumentMetadata
import com.ayanpandey.blink.domain.repository.FileResolver
import com.ayanpandey.blink.domain.contract.DocumentFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileDescriptor

class DocumentViewerImplTest {

    private val fileResolver = mockk<FileResolver>()
    private val documentFactory = mockk<DocumentFactory>()
    private val logger = mockk<BlinkLogger>(relaxed = true)
    private val viewer = DocumentViewerImpl(fileResolver, documentFactory, logger)

    private val testUri = "content://path/to/doc.pdf"
    private val testDoc = Document(
        id = "1",
        uri = testUri,
        displayName = "doc.pdf",
        mimeType = "application/pdf",
        extension = "pdf",
        size = 1024L,
        lastModified = 123456L,
        documentType = DocumentType.PDF
    )
    private val testMetadata = DocumentMetadata(
        uri = testUri,
        fileName = "doc.pdf",
        fileSize = 1024L,
        mimeType = "application/pdf",
        lastModified = 123456L,
        extension = "pdf"
    )

    @Test
    fun loadDocument_success() = runTest {
        coEvery { fileResolver.resolveMetadata(testUri) } returns Result.success(testMetadata)
        every {
            documentFactory.createDocument(
                fileDescriptor = any(),
                uriString = testUri,
                displayName = "doc.pdf",
                mimeType = "application/pdf",
                size = 1024L,
                lastModified = 123456L
            )
        } returns Result.success(testDoc)

        val result = viewer.loadDocument(testUri)

        assertTrue(result.isSuccess)
        assertEquals(testDoc, result.getOrThrow())
        assertEquals(DocumentState.Ready(testDoc), viewer.state.value)
        assertEquals(testDoc, viewer.getMetadata())
    }

    @Test
    fun loadDocument_metadataFailure() = runTest {
        val error = AppError.FileError.FileNotFound
        coEvery { fileResolver.resolveMetadata(testUri) } returns Result.failure(AppErrorException(error))

        val result = viewer.loadDocument(testUri)

        assertTrue(result.isFailure)
        assertEquals(DocumentState.Error(error), viewer.state.value)
    }

    @Test
    fun loadDocument_factoryFailure() = runTest {
        coEvery { fileResolver.resolveMetadata(testUri) } returns Result.success(testMetadata)
        val error = AppError.DocumentError.UnsupportedDocument
        every {
            documentFactory.createDocument(
                fileDescriptor = any(),
                uriString = testUri,
                displayName = "doc.pdf",
                mimeType = "application/pdf",
                size = 1024L,
                lastModified = 123456L
            )
        } returns Result.failure(AppErrorException(error))

        val result = viewer.loadDocument(testUri)

        assertTrue(result.isFailure)
        assertEquals(DocumentState.Error(error), viewer.state.value)
    }

    @Test
    fun closeDocument_resetsToIdle() = runTest {
        coEvery { fileResolver.resolveMetadata(testUri) } returns Result.success(testMetadata)
        every {
            documentFactory.createDocument(
                fileDescriptor = any(),
                uriString = testUri,
                displayName = "doc.pdf",
                mimeType = "application/pdf",
                size = 1024L,
                lastModified = 123456L
            )
        } returns Result.success(testDoc)

        viewer.loadDocument(testUri)
        assertEquals(DocumentState.Ready(testDoc), viewer.state.value)

        viewer.closeDocument()
        assertEquals(DocumentState.Idle, viewer.state.value)
        assertTrue(viewer.getMetadata() == null)
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(DocumentState.Idle, viewer.state.value)
    }
}
