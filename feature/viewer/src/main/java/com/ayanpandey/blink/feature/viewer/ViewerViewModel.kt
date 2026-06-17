package com.ayanpandey.blink.feature.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayanpandey.blink.domain.contract.DocumentViewer
import com.ayanpandey.blink.domain.contract.DocumentRenderer
import com.ayanpandey.blink.domain.model.DocumentState
import com.ayanpandey.blink.domain.model.DocumentType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ViewerViewModel(
    private val documentViewer: DocumentViewer,
    private val renderers: List<DocumentRenderer>
) : ViewModel() {
    val state: StateFlow<DocumentState> = documentViewer.state

    fun load(uriString: String) {
        viewModelScope.launch {
            documentViewer.loadDocument(uriString)
        }
    }

    fun getAssignedRenderer(docType: DocumentType): String {
        val renderer = renderers.find { it.supportedType == docType }
        return renderer?.name ?: "No Renderer Assigned"
    }

    override fun onCleared() {
        super.onCleared()
        documentViewer.closeDocument()
    }
}
