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

    override suspend fun loadDocument(uriString: String): Result<Document> {
        logger.d(TAG, "loadDocument: Starting load for $uriString")
        _state.value = DocumentState.Loading
        
        val metadataResult = fileResolver.resolveMetadata(uriString)
        if (metadataResult.isFailure) {
            val exception = metadataResult.exceptionOrNull()!!
            val error = (exception as? AppErrorException)?.error ?: com.ayanpandey.blink.core.common.error.AppError.UnknownError(exception)
            _state.value = DocumentState.Error(error)
            return Result.failure(exception)
        }
        val metadata = metadataResult.getOrThrow()

        val streamResult = fileResolver.openInputStream(uriString)
        if (streamResult.isFailure) {
            val exception = streamResult.exceptionOrNull()!!
            val error = (exception as? AppErrorException)?.error ?: com.ayanpandey.blink.core.common.error.AppError.UnknownError(exception)
            _state.value = DocumentState.Error(error)
            return Result.failure(exception)
        }
        val inputStream = streamResult.getOrThrow()

        return try {
            val fdField = inputStream.javaClass.getDeclaredField("fd")
            fdField.isAccessible = true
            val fd = fdField.get(inputStream) as java.io.FileDescriptor
            
            val factoryResult = documentFactory.createDocument(
                fileDescriptor = fd,
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
                logger.i(TAG, "loadDocument: Success creating document ${doc.displayName}")
                Result.success(doc)
            } else {
                val exception = factoryResult.exceptionOrNull()!!
                val error = (exception as? AppErrorException)?.error ?: com.ayanpandey.blink.core.common.error.AppError.UnknownError(exception)
                _state.value = DocumentState.Error(error)
                Result.failure(exception)
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get FileDescriptor", e)
            val error = com.ayanpandey.blink.core.common.error.AppError.FileError.CorruptedUri
            _state.value = DocumentState.Error(error)
            Result.failure(AppErrorException(error))
        } finally {
            try {
                inputStream.close()
            } catch (ignored: Exception) {}
        }
    }

    override fun closeDocument() {
        logger.d(TAG, "Closing active document: ${currentDocument?.displayName}")
        currentDocument = null
        _state.value = DocumentState.Idle
    }

    @Suppress("UNUSED_VARIABLE")
    override fun reloadDocument() {
        val uri = currentDocument?.uri ?: return
        closeDocument()
        // Reload will be triggered by the screen calling loadDocument(uri) again
    }

    override fun getMetadata(): Document? = currentDocument

    companion object {
        private const val TAG = "DocumentViewerImpl"
    }
}
