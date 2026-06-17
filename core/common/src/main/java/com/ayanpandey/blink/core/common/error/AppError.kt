package com.ayanpandey.blink.core.common.error

sealed interface AppError {
    sealed interface FileError : AppError {
        data object FileNotFound : FileError

        data class PermissionDenied(
            val causeMessage: String? = null,
            val stackTrace: String? = null
        ) : FileError

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

    sealed interface DocumentError : AppError {
        data object UnsupportedDocument : DocumentError
        data object CorruptedDocument : DocumentError
        data object DocumentNotFound : DocumentError
        data class DocumentPermissionDenied(val causeMessage: String? = null, val stackTrace: String? = null) : DocumentError
        data class DocumentParsing(val detail: String? = null) : DocumentError
    }

    data class UnknownError(val throwable: Throwable) : AppError
}

class AppErrorException(val error: AppError) : Exception(error.toString())
