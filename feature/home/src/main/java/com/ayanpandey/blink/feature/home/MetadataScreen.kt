package com.ayanpandey.blink.feature.home

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayanpandey.blink.core.common.error.AppError
import com.ayanpandey.blink.domain.model.DocumentMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataScreen(
    uriString: String,
    viewModel: MetadataViewModel,
    onBackClick: () -> Unit,
    onOpenClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uriString) {
        viewModel.loadMetadata(uriString)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Metadata") },
                navigationIcon = {
                    Button(onClick = onBackClick, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                is MetadataUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is MetadataUiState.Success -> {
                    MetadataContent(
                        metadata = state.metadata,
                        onOpenClick = onOpenClick
                    )
                }
                is MetadataUiState.Error -> {
                    ErrorContent(error = state.error, uriString = uriString, onBackClick = onBackClick)
                }
            }
        }
    }
}

@Composable
private fun MetadataContent(
    metadata: DocumentMetadata,
    onOpenClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MetadataItem(label = "File Name", value = metadata.fileName)
        MetadataItem(label = "File Size", value = formatFileSize(metadata.fileSize))
        MetadataItem(label = "Mime Type", value = metadata.mimeType)
        MetadataItem(label = "Extension", value = metadata.extension.uppercase(Locale.ROOT))
        MetadataItem(label = "Last Modified", value = formatTimestamp(metadata.lastModified))
        MetadataItem(label = "URI", value = metadata.uri)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onOpenClick(metadata.uri) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open in Viewer")
        }
    }
}

@Composable
private fun MetadataItem(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ErrorContent(
    error: AppError,
    uriString: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val parsedUri = try { Uri.parse(uriString) } catch (e: Exception) { null }
    val scheme = parsedUri?.scheme ?: "N/A"
    val authority = parsedUri?.authority ?: "N/A"

    val readPermissionGranted = parsedUri?.let {
        context.checkUriPermission(
            it,
            android.os.Process.myPid(),
            android.os.Process.myUid(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    } ?: false

    val writePermissionGranted = parsedUri?.let {
        context.checkUriPermission(
            it,
            android.os.Process.myPid(),
            android.os.Process.myUid(),
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    } ?: false

    val persistedPermissions = try {
        context.contentResolver.persistedUriPermissions
    } catch (e: Exception) {
        emptyList()
    }

    val (title, description) =
        when (error) {
            is AppError.FileError.PermissionDenied ->
                Pair(
                    "Access Denied",
                    "Blink does not have permission to access this file. Please check system permissions.",
                )
            is AppError.FileError.FileNotFound ->
                Pair(
                    "File Not Found",
                    "The requested document could not be found on the storage.",
                )
            is AppError.FileError.UnsupportedType ->
                Pair(
                    "Format Not Supported",
                    "Blink V1 does not support this file format. " +
                        "Only PDF, Office documents, CSV, and Text files are supported.",
                )
            is AppError.FileError.InvalidUri,
            is AppError.FileError.MissingUri,
            is AppError.FileError.CorruptedUri,
            ->
                Pair(
                    "Invalid Document",
                    "The file identifier is corrupted or invalid.",
                )
            else ->
                Pair(
                    "Unknown Error",
                    "An unexpected error occurred while resolving document metadata.",
                )
        }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Button(onClick = onBackClick) {
            Text("Go Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DEBUG BOX
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DEBUG INFORMATION (Copy & Paste to Agent)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "URI: $uriString",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Scheme: $scheme",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Authority: $authority",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Permission check: READ = $readPermissionGranted, WRITE = $writePermissionGranted",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Persisted Permissions Count: ${persistedPermissions.size}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    persistedPermissions.forEach { perm ->
                        Text(
                            text = "- ${perm.uri} (Read: ${perm.isReadPermission}, Write: ${perm.isWritePermission})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Exception Details:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    when (error) {
                        is AppError.FileError.PermissionDenied -> {
                            Text(
                                text = "Message: ${error.causeMessage ?: "No message"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Stack Trace:\n${error.stackTrace ?: "No stacktrace"}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                        is AppError.UnknownError -> {
                            val stackTrace = android.util.Log.getStackTraceString(error.throwable)
                            Text(
                                text = "Message: ${error.throwable.message ?: "No message"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Stack Trace:\n$stackTrace",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = "No exception details available.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 Bytes"
    val units = arrayOf("Bytes", "KB", "MB", "GB")
    val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = sizeBytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(
        Locale.ROOT,
        "%.2f %s",
        value,
        units[digitGroups],
    )
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
