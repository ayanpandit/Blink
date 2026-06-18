package com.ayanpandey.blink.feature.viewer

import com.ayanpandey.blink.domain.contract.DocumentRenderer
import com.ayanpandey.blink.domain.contract.DocumentViewer
import com.ayanpandey.blink.domain.model.DocumentType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val documentViewer = mockk<DocumentViewer>(relaxed = true)
    
    private val mockPdfRenderer = mockk<DocumentRenderer> {
        every { supportedType } returns DocumentType.PDF
        every { name } returns "PDFium Renderer"
    }
    
    private val mockWordRenderer = mockk<DocumentRenderer> {
        every { supportedType } returns DocumentType.DOCX
        every { name } returns "Word Renderer"
    }

    private val renderers = listOf(mockPdfRenderer, mockWordRenderer)
    private lateinit var viewModel: ViewerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ViewerViewModel(documentViewer, renderers)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetRenderer_success() {
        val resolved = viewModel.getRenderer(DocumentType.PDF)
        assertEquals(mockPdfRenderer, resolved)
    }

    @Test
    fun testGetRenderer_unsupported() {
        val resolved = viewModel.getRenderer(DocumentType.XLSX)
        assertNull(resolved)
    }

    @Test
    fun testGetAssignedRenderer_found() {
        val resolvedName = viewModel.getAssignedRenderer(DocumentType.DOCX)
        assertEquals("Word Renderer", resolvedName)
    }

    @Test
    fun testGetAssignedRenderer_notFound() {
        val resolvedName = viewModel.getAssignedRenderer(DocumentType.XLSX)
        assertEquals("No Renderer Assigned", resolvedName)
    }

    @Test
    fun testLoad_callsViewer() {
        viewModel.load("content://test.pdf")
        coVerify { documentViewer.loadDocument("content://test.pdf") }
    }
}
