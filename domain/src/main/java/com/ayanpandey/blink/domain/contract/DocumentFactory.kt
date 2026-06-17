package com.ayanpandey.blink.domain.contract

import com.ayanpandey.blink.domain.model.Document
import java.io.FileDescriptor

interface DocumentFactory {
    fun createDocument(
        fileDescriptor: FileDescriptor,
        uriString: String,
        displayName: String,
        mimeType: String,
        size: Long,
        lastModified: Long
    ): Result<Document>
}
