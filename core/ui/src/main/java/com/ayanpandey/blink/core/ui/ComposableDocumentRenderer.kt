package com.ayanpandey.blink.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayanpandey.blink.domain.contract.DocumentRenderer
import com.ayanpandey.blink.domain.model.Document

interface ComposableDocumentRenderer : DocumentRenderer {
    @Composable
    fun Render(
        document: Document,
        modifier: Modifier
    )

    @Composable
    fun Render(
        document: Document,
        initialPosition: Int,
        onPositionChanged: (Int) -> Unit,
        modifier: Modifier
    ) {
        Render(document, modifier)
    }
}
