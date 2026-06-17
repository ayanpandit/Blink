package com.ayanpandey.blink.core.common.error

sealed interface AppError {
    sealed interface FileError : AppError {
        data object FileNotFound : FileError

        data object PermissionDenied : FileError

        data object LimitExceeded : FileError

        data object InvalidUri : FileError

        data object MissingUri : FileError

        data object CorruptedUri : FileError

        data object UnsupportedType : FileError
    }

    sealed interface ParsingError : AppError {
        data object InvalidFormat : ParsingError

        data object CorruptedFile : ParsingError

        data class GenericError(val detail: String) : ParsingError
    }

    data class UnknownError(val throwable: Throwable) : AppError
}

class AppErrorException(val error: AppError) : Exception(error.toString())
