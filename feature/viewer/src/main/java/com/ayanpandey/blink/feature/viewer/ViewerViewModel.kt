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
    private val renderers: List<DocumentRenderer>,
    private val logger: com.ayanpandey.blink.core.common.logging.BlinkLogger,
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

    fun getRenderer(docType: DocumentType): DocumentRenderer? {
        val renderer = renderers.find { it.supportedType == docType }
        logger.d("ViewerViewModel", "Renderer Selection: resolved '${renderer?.name}' for document type '$docType'")
        return renderer
    }


    override fun onCleared() {
        super.onCleared()
        documentViewer.closeDocument()
    }
}
