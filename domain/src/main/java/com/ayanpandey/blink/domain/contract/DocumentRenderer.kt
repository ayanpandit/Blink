package com.ayanpandey.blink.domain.contract

import com.ayanpandey.blink.domain.model.DocumentType

interface DocumentRenderer {
    val supportedType: DocumentType
    val name: String
}
