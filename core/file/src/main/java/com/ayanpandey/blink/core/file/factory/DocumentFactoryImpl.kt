package com.ayanpandey.blink.core.file.factory

import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.contract.DocumentFactory
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.core.common.error.AppError
import com.ayanpandey.blink.core.common.error.AppErrorException
import java.io.FileDescriptor
import java.util.Locale

class DocumentFactoryImpl(private val logger: BlinkLogger) : DocumentFactory {
    override fun createDocument(
        fileDescriptor: FileDescriptor,
        uriString: String,
        displayName: String,
        mimeType: String,
        size: Long,
        lastModified: Long
    ): Result<Document> {
        logger.d(TAG, "Creating document for URI: $uriString | Mime: $mimeType")
        
        val extension = extractExtension(displayName)
        val docType = detectDocumentType(mimeType, extension)
        
        if (docType == DocumentType.UNKNOWN) {
            logger.w(TAG, "Unsupported file format for document: $displayName")
            return Result.failure(AppErrorException(AppError.DocumentError.UnsupportedDocument))
        }

        val document = Document(
            id = uriString.hashCode().toString(),
            uri = uriString,
            displayName = displayName,
            mimeType = mimeType,
            extension = extension,
            size = size,
            lastModified = lastModified,
            documentType = docType
        )
        
        logger.i(TAG, "Successfully created document model: $document")
        return Result.success(document)
    }

    private fun extractExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex != -1 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    private fun detectDocumentType(mimeType: String, extension: String): DocumentType {
        return when (extension) {
            "pdf" -> DocumentType.PDF
            "doc" -> DocumentType.DOC
            "docx" -> DocumentType.DOCX
            "xls" -> DocumentType.XLS
            "xlsx" -> DocumentType.XLSX
            "ppt" -> DocumentType.PPT
            "pptx" -> DocumentType.PPTX
            "txt" -> DocumentType.TXT
            "csv" -> DocumentType.CSV
            else -> when (mimeType) {
                "application/pdf" -> DocumentType.PDF
                "application/msword" -> DocumentType.DOC
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DocumentType.DOCX
                "application/vnd.ms-excel" -> DocumentType.XLS
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> DocumentType.XLSX
                "application/vnd.ms-powerpoint" -> DocumentType.PPT
                "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> DocumentType.PPTX
                "text/plain" -> DocumentType.TXT
                "text/csv", "application/csv" -> DocumentType.CSV
                else -> DocumentType.UNKNOWN
            }
        }
    }

    companion object {
        private const val TAG = "DocumentFactoryImpl"
    }
}
