package com.ayanpandey.blink.domain.di

import com.ayanpandey.blink.core.common.di.AppContainer
import com.ayanpandey.blink.domain.repository.FileResolver

import com.ayanpandey.blink.domain.repository.DocumentRepository

interface DomainContainer : AppContainer {
    val fileResolver: FileResolver
    val documentViewer: com.ayanpandey.blink.domain.contract.DocumentViewer
    val renderers: List<com.ayanpandey.blink.domain.contract.DocumentRenderer>
    val documentRepository: DocumentRepository
}

