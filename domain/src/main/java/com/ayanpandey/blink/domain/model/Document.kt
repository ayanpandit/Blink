package com.ayanpandey.blink.domain.model

data class Document(
    val id: String,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val extension: String,
    val size: Long,
    val lastModified: Long,
    val documentType: DocumentType
)
