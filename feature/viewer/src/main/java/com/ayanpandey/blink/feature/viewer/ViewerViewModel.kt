package com.ayanpandey.blink.feature.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayanpandey.blink.domain.contract.DocumentViewer
import com.ayanpandey.blink.domain.contract.DocumentRenderer
import com.ayanpandey.blink.domain.model.DocumentState
import com.ayanpandey.blink.domain.model.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewerViewModel(
    private val documentViewer: DocumentViewer,
    private val renderers: List<DocumentRenderer>,
    private val repository: com.ayanpandey.blink.domain.repository.DocumentRepository,
    private val logger: com.ayanpandey.blink.core.common.logging.BlinkLogger,
) : ViewModel() {
    val state: StateFlow<DocumentState> = documentViewer.state

    private val _initialPosition = MutableStateFlow(0)
    val initialPosition: StateFlow<Int> = _initialPosition.asStateFlow()

    private var documentUri: String? = null
    private var lastSavedPosition = 0

    fun load(uriString: String) {
        documentUri = uriString
        viewModelScope.launch {
            val pos = repository.getContinueReadingPosition(uriString)
            _initialPosition.value = pos
            lastSavedPosition = pos
            
            val result = documentViewer.loadDocument(uriString)
            result.onSuccess { doc ->
                repository.addRecentDocument(doc)
            }
        }
    }

    fun savePosition(position: Int) {
        val uri = documentUri ?: return
        if (position == lastSavedPosition) return
        lastSavedPosition = position
        viewModelScope.launch {
            repository.saveContinueReadingPosition(uri, position)
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
