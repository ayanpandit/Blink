package com.ayanpandey.blink.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "SwallowedException")
@Composable
fun HomeScreen(
    onNavigateToScanner: () -> Unit,
    onFileSelected: (String) -> Unit,
    logger: com.ayanpandey.blink.core.common.logging.BlinkLogger,
    modifier: Modifier = Modifier,
) {
    val supportedMimeTypes =
        arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
        )

    val context = androidx.compose.ui.platform.LocalContext.current
    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            logger.d(TAG, "Picker activity result callback triggered with URI: $uri")
            uri?.let {
                try {
                    logger.d(TAG, "Calling takePersistableUriPermission for $it")
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    logger.i(
                        TAG,
                        "Persistable URI permission taken successfully in picker callback for $it",
                    )
                } catch (e: SecurityException) {
                    logger.w(
                        TAG,
                        "SecurityException while taking persistable URI permission in picker for $it: ${e.message}",
                        e,
                    )
                }
                onFileSelected(it.toString())
            }
        }

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
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Tap → Open → Read",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { pickerLauncher.launch(supportedMimeTypes) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select Document")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToScanner,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan Document")
            }
        }
    }
}

private const val TAG = "HomeScreen"
