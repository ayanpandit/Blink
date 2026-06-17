package com.ayanpandey.blink.domain.repository

import com.ayanpandey.blink.domain.model.DocumentMetadata
import java.io.InputStream

interface FileResolver {
    suspend fun resolveMetadata(uriString: String): Result<DocumentMetadata>

    suspend fun openInputStream(uriString: String): Result<InputStream>
}
