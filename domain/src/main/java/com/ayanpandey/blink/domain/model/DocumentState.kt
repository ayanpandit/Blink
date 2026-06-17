package com.ayanpandey.blink.domain.model

import com.ayanpandey.blink.core.common.error.AppError

sealed interface DocumentState {
    data object Idle : DocumentState
    data object Loading : DocumentState
    data class Ready(val document: Document) : DocumentState
    data class Error(val error: AppError) : DocumentState
}
