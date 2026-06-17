package com.ayanpandey.blink.core.file.resolver

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.ayanpandey.blink.core.common.error.AppError
import com.ayanpandey.blink.core.common.error.AppErrorException
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InputStream

class FileResolverImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private val logger = mockk<BlinkLogger>(relaxed = true)
    private val uri = mockk<Uri>()

    private lateinit var fileResolver: FileResolverImpl

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockkStatic(android.os.Process::class)
        every { Uri.parse(any()) } returns uri
        every { uri.scheme } returns "content"
        every { uri.authority } returns "com.example.provider"
        every { context.contentResolver } returns contentResolver
        every {
            context.checkUriPermission(any(), any(), any(), any())
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        every {
            contentResolver.takePersistableUriPermission(any(), any())
        } returns Unit
        every { android.os.Process.myPid() } returns 1
        every { android.os.Process.myUid() } returns 1
        fileResolver = FileResolverImpl(context, logger)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun resolveMetadata_contentScheme_success() =
        runBlocking {
            // Arrange
            val uriString = "content://com.android.providers.downloads.documents/document/1"
            every { uri.scheme } returns "content"
            every { contentResolver.getType(uri) } returns "application/pdf"

            val cursor = mockk<Cursor>(relaxed = true)
            every { contentResolver.query(uri, null, null, null, null) } returns cursor
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndex("_display_name") } returns 0
            every { cursor.getString(0) } returns "test_document.pdf"
            every { cursor.getColumnIndex("_size") } returns 1
            every { cursor.getLong(1) } returns 1024L
            every { cursor.getColumnIndex("last_modified") } returns -1

            // Act
            val result = fileResolver.resolveMetadata(uriString)

            // Assert
            assertTrue(result.isSuccess)
            val metadata = result.getOrNull()
            assertEquals(uriString, metadata?.uri)
            assertEquals("test_document.pdf", metadata?.fileName)
            assertEquals("application/pdf", metadata?.mimeType)
            assertEquals(1024L, metadata?.fileSize)
            assertEquals(0L, metadata?.lastModified)
            assertEquals("pdf", metadata?.extension)
        }

    @Test
    fun resolveMetadata_contentScheme_unsupportedFormat() =
        runBlocking {
            // Arrange
            val uriString = "content://com.android.providers.downloads.documents/document/1"
            every { uri.scheme } returns "content"
            every { contentResolver.getType(uri) } returns "image/png"

            val cursor = mockk<Cursor>(relaxed = true)
            every { contentResolver.query(uri, null, null, null, null) } returns cursor
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndex("_display_name") } returns 0
            every { cursor.getString(0) } returns "image.png"

            // Act
            val result = fileResolver.resolveMetadata(uriString)

            // Assert
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull() as? AppErrorException
            assertEquals(AppError.FileError.UnsupportedType, exception?.error)
        }

    @Test
    fun resolveMetadata_invalidScheme() =
        runBlocking {
            // Arrange
            val uriString = "http://example.com/file.pdf"
            every { uri.scheme } returns "http"

            // Act
            val result = fileResolver.resolveMetadata(uriString)

            // Assert
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull() as? AppErrorException
            assertEquals(AppError.FileError.InvalidUri, exception?.error)
        }

    @Test
    fun resolveMetadata_fileScheme_success() =
        runBlocking {
            // Arrange
            val tempFile = tempFolder.newFile("sample_sheet.xlsx")
            val uriString = "file://${tempFile.absolutePath}"
            every { uri.scheme } returns "file"
            every { uri.path } returns tempFile.absolutePath
            every { contentResolver.getType(uri) } returns null

            // Act
            val result = fileResolver.resolveMetadata(uriString)

            // Assert
            assertTrue(result.isSuccess)
            val metadata = result.getOrNull()
            assertEquals(uriString, metadata?.uri)
            assertEquals("sample_sheet.xlsx", metadata?.fileName)
            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", metadata?.mimeType)
            assertEquals("xlsx", metadata?.extension)
        }

    @Test
    fun resolveMetadata_fileScheme_fileNotFound() =
        runBlocking {
            // Arrange
            val uriString = "file:///non/existent/path/doc.docx"
            every { uri.scheme } returns "file"
            every { uri.path } returns "/non/existent/path/doc.docx"
            every { contentResolver.getType(uri) } returns null

            // Act
            val result = fileResolver.resolveMetadata(uriString)

            // Assert
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull() as? AppErrorException
            assertEquals(AppError.FileError.FileNotFound, exception?.error)
        }

    @Test
    fun resolveMetadata_securityException() =
        runBlocking {
            // Arrange
            val uriString = "content://restricted/file.pdf"
            every { uri.scheme } returns "content"
            every { contentResolver.getType(uri) } throws SecurityException("Permission denied")

            // Act
            val result = fileResolver.resolveMetadata(uriString)

            // Assert
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull() as? AppErrorException
            assertEquals(AppError.FileError.PermissionDenied, exception?.error)
        }

    @Test
    fun openInputStream_success() =
        runBlocking {
            // Arrange
            val uriString = "content://com.android.providers.downloads.documents/document/1"
            val mockInputStream = mockk<InputStream>()
            every { contentResolver.openInputStream(uri) } returns mockInputStream

            // Act
            val result = fileResolver.openInputStream(uriString)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(mockInputStream, result.getOrNull())
        }

    @Test
    fun openInputStream_securityException() =
        runBlocking {
            // Arrange
            val uriString = "content://restricted/file.pdf"
            every { contentResolver.openInputStream(uri) } throws SecurityException("Access denied")

            // Act
            val result = fileResolver.openInputStream(uriString)

            // Assert
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull() as? AppErrorException
            assertEquals(AppError.FileError.PermissionDenied, exception?.error)
        }
}
