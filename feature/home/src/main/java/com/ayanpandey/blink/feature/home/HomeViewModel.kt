package com.ayanpandey.blink.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Success(val count: Int) : ScanState
    data class Error(val message: String) : ScanState
}

class HomeViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Document>>(emptyList())
    val searchResults: StateFlow<List<Document>> = _searchResults.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    val recentDocuments: StateFlow<List<Document>> = repository.getRecentDocuments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val scannedDocuments: StateFlow<List<Document>> = repository.getScannedDocuments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadFolders()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        executeSearch()
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
        executeSearch()
    }

    private fun executeSearch() {
        viewModelScope.launch {
            val results = repository.searchDocuments(
                query = _searchQuery.value,
                category = _selectedCategory.value,
                extension = null
            )
            _searchResults.value = results
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _folders.value = repository.getScannedFolders()
        }
    }

    fun addFolder(uri: String) {
        viewModelScope.launch {
            repository.addScannedFolder(uri)
            loadFolders()
            triggerScan()
        }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch {
            repository.removeScannedFolder(uri)
            loadFolders()
            triggerScan()
        }
    }

    fun triggerScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            val foldersList = repository.getScannedFolders()
            val result = repository.scanStorage(foldersList)
            result.onSuccess { count ->
                _scanState.value = ScanState.Success(count)
                executeSearch() // Refresh list if active
            }.onFailure { err ->
                _scanState.value = ScanState.Error(err.message ?: "Unknown scanning error")
            }
        }
    }

    fun resetScanState() {
        _scanState.value = ScanState.Idle
    }
}
