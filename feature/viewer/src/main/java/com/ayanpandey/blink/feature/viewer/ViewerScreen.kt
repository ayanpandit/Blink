package com.ayanpandey.blink.feature.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    uriString: String,
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(uriString) {
        viewModel.load(uriString)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Viewer") },
                navigationIcon = {
                    Button(onClick = onBackClick, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is DocumentState.Idle, is DocumentState.Loading -> {
                    CircularProgressIndicator()
                }
                is DocumentState.Ready -> {
                    DocumentInfo(
                        document = currentState.document,
                        rendererName = viewModel.getAssignedRenderer(currentState.document.documentType)
                    )
                }
                is DocumentState.Error -> {
                    Text(
                        text = "Failed to load document: ${currentState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentInfo(document: Document, rendererName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Document Info (Architecture Validation)", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Name: ${document.displayName}")
        Text("Type: ${document.documentType.name}")
        Text("Mime: ${document.mimeType}")
        Text("Size: ${document.size} Bytes")
        Text("URI: ${document.uri}")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Renderer Assigned: $rendererName", color = MaterialTheme.colorScheme.primary)
    }
}
