package com.ayanpandey.blink.domain.contract

import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentState
import kotlinx.coroutines.flow.StateFlow

interface DocumentViewer {
    val state: StateFlow<DocumentState>
    suspend fun loadDocument(uriString: String): Result<Document>
    fun closeDocument()
    fun reloadDocument()
    fun getMetadata(): Document?
}
