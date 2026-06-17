package com.ayanpandey.blink.domain.di

import com.ayanpandey.blink.core.common.di.AppContainer
import com.ayanpandey.blink.domain.repository.FileResolver

interface DomainContainer : AppContainer {
    val fileResolver: FileResolver
}
