package com.ayanpandey.blink.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayanpandey.blink.core.common.error.AppError
import com.ayanpandey.blink.core.common.error.AppErrorException
import com.ayanpandey.blink.domain.model.DocumentMetadata
import com.ayanpandey.blink.domain.repository.FileResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MetadataUiState {
    data object Loading : MetadataUiState

    data class Success(val metadata: DocumentMetadata) : MetadataUiState

    data class Error(val error: AppError) : MetadataUiState
}

class MetadataViewModel(
    private val fileResolver: FileResolver,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MetadataUiState>(MetadataUiState.Loading)
    val uiState: StateFlow<MetadataUiState> = _uiState.asStateFlow()

    fun loadMetadata(uriString: String) {
        viewModelScope.launch {
            _uiState.value = MetadataUiState.Loading
            fileResolver.resolveMetadata(uriString)
                .onSuccess { metadata ->
                    _uiState.value = MetadataUiState.Success(metadata)
                }
                .onFailure { throwable ->
                    val appError =
                        if (throwable is AppErrorException) {
                            throwable.error
                        } else {
                            AppError.UnknownError(throwable)
                        }
                    _uiState.value = MetadataUiState.Error(appError)
                }
        }
    }
}
