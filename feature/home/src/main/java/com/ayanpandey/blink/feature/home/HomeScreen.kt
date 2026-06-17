package com.ayanpandey.blink.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToPdf: (String) -> Unit,
    onNavigateToWord: (String) -> Unit,
    onNavigateToExcel: (String) -> Unit,
    onNavigateToPpt: (String) -> Unit,
    onNavigateToText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Blink Document Viewer") })
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Home Screen (Placeholder)", modifier = Modifier.padding(bottom = 16.dp))

            Button(onClick = onNavigateToScanner, modifier = Modifier.fillMaxWidth()) {
                Text("Scan Document")
            }
            Button(onClick = { onNavigateToPdf("sample.pdf") }, modifier = Modifier.fillMaxWidth()) {
                Text("Open PDF (Placeholder)")
            }
            Button(onClick = { onNavigateToWord("sample.docx") }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Word (Placeholder)")
            }
            Button(onClick = { onNavigateToExcel("sample.xlsx") }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Excel (Placeholder)")
            }
            Button(onClick = { onNavigateToPpt("sample.pptx") }, modifier = Modifier.fillMaxWidth()) {
                Text("Open PowerPoint (Placeholder)")
            }
            Button(onClick = { onNavigateToText("sample.txt") }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Plain Text (Placeholder)")
            }
        }
    }
}
