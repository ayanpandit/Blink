package com.ayanpandey.blink.domain.model

data class DocumentMetadata(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val lastModified: Long,
    val extension: String,
)
