package com.ayanpandey.blink.feature.viewer

import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.contract.DocumentViewer
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentState
import com.ayanpandey.blink.domain.repository.FileResolver
import com.ayanpandey.blink.domain.contract.DocumentFactory
import com.ayanpandey.blink.core.common.error.AppErrorException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileDescriptor

class DocumentViewerImpl(
    private val fileResolver: FileResolver,
    private val documentFactory: DocumentFactory,
    private val logger: BlinkLogger
) : DocumentViewer {
    private val _state = MutableStateFlow<DocumentState>(DocumentState.Idle)
    override val state: StateFlow<DocumentState> = _state.asStateFlow()
    private var currentDocument: Document? = null

    @Suppress("TooGenericExceptionCaught")
    override suspend fun loadDocument(uriString: String): Result<Document> {
        logger.d(TAG, "loadDocument: START | uriString=$uriString")
        _state.value = DocumentState.Loading

        // Step 1: Resolve metadata
        logger.d(TAG, "loadDocument: Step 1 - Resolving metadata for $uriString")
        val metadataResult = fileResolver.resolveMetadata(uriString)
        if (metadataResult.isFailure) {
            val exception = metadataResult.exceptionOrNull()!!
            val error = (exception as? AppErrorException)?.error
                ?: com.ayanpandey.blink.core.common.error.AppError.UnknownError(exception)
            logger.e(TAG, "loadDocument: Step 1 FAILED | error=$error | exception=${exception.message}")
            _state.value = DocumentState.Error(error)
            return Result.failure(exception)
        }
        val metadata = metadataResult.getOrThrow()
        logger.i(TAG, "loadDocument: Step 1 SUCCESS | fileName=${metadata.fileName} " +
            "| mimeType=${metadata.mimeType} | size=${metadata.fileSize} " +
            "| extension=${metadata.extension} | metadataUri=${metadata.uri}")

        // Step 2: Create Document model via factory (no InputStream/FileDescriptor needed)
        logger.d(TAG, "loadDocument: Step 2 - Creating Document via DocumentFactory")
        return try {
            val factoryResult = documentFactory.createDocument(
                fileDescriptor = FileDescriptor(),
                uriString = uriString,
                displayName = metadata.fileName,
                mimeType = metadata.mimeType,
                size = metadata.fileSize,
                lastModified = metadata.lastModified
            )

            if (factoryResult.isSuccess) {
                val doc = factoryResult.getOrThrow()
                currentDocument = doc
                _state.value = DocumentState.Ready(doc)
                logger.i(TAG, "loadDocument: Step 2 SUCCESS | doc=${doc.displayName} " +
                    "| type=${doc.documentType} | docUri=${doc.uri}")
                Result.success(doc)
            } else {
                val exception = factoryResult.exceptionOrNull()!!
                val error = (exception as? AppErrorException)?.error
                    ?: com.ayanpandey.blink.core.common.error.AppError.UnknownError(exception)
                logger.e(TAG, "loadDocument: Step 2 FAILED (factory) | error=$error " +
                    "| exception=${exception.message}")
                _state.value = DocumentState.Error(error)
                Result.failure(exception)
            }
        } catch (e: Exception) {
            logger.e(TAG, "loadDocument: Step 2 EXCEPTION | type=${e.javaClass.name} " +
                "| message=${e.message} | stackTrace=${e.stackTraceToString()}")
            val error = com.ayanpandey.blink.core.common.error.AppError.UnknownError(e)
            _state.value = DocumentState.Error(error)
            Result.failure(e)
        }
    }

    override fun closeDocument() {
        logger.d(TAG, "closeDocument: Closing ${currentDocument?.displayName}")
        currentDocument = null
        _state.value = DocumentState.Idle
    }

    override fun reloadDocument() {
        currentDocument?.uri ?: return
        closeDocument()
    }

    override fun getMetadata(): Document? = currentDocument

    companion object {
        private const val TAG = "DocumentViewerImpl"
    }
}

