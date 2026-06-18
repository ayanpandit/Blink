package com.ayanpandey.blink.feature.text

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.repository.FileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "TxtViewer"
private const val MAX_LINES_PREVIEW = 50_000

private sealed interface TxtViewState {
    data object Loading : TxtViewState
    data class Ready(val lines: List<String>, val truncated: Boolean) : TxtViewState
    data class Error(val error: com.ayanpandey.blink.core.common.error.AppError.DocumentError) : TxtViewState
}

@Composable
fun TxtViewer(
    document: Document,
    fileResolver: FileResolver,
    logger: BlinkLogger,
    modifier: Modifier = Modifier,
) {
    var viewState: TxtViewState by remember { mutableStateOf(TxtViewState.Loading) }

    LaunchedEffect(document.uri) {
        viewState = TxtViewState.Loading
        logger.d(TAG, "State transition: LOADING for ${document.displayName}")
        viewState = withContext(Dispatchers.IO) {
            try {
                val inputStreamResult = fileResolver.openInputStream(document.uri)
                if (inputStreamResult.isFailure) {
                    val ex = inputStreamResult.exceptionOrNull()!!
                    val error = when (ex) {
                        is SecurityException -> com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied(ex.message, ex.stackTraceToString())
                        is com.ayanpandey.blink.core.common.error.AppErrorException -> ex.error as? com.ayanpandey.blink.core.common.error.AppError.DocumentError ?: com.ayanpandey.blink.core.common.error.AppError.DocumentError.TextParsingError(ex.message)
                        else -> com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied(ex.message, ex.stackTraceToString())
                    }
                    logger.e(TAG, "State transition: ERROR | uri=${document.uri} | error=$error", ex)
                    TxtViewState.Error(error)
                } else {
                    val inputStream = inputStreamResult.getOrThrow()
                    val reader = inputStream.bufferedReader(Charsets.UTF_8)
                    val lines = mutableListOf<String>()
                    var truncated = false
                    reader.useLines { seq ->
                        for (line in seq) {
                            if (lines.size >= MAX_LINES_PREVIEW) {
                                truncated = true
                                break
                            }
                            lines.add(line)
                        }
                    }
                    logger.d(TAG, "State transition: READY | Read ${lines.size} lines from ${document.displayName} (truncated=$truncated)")
                    TxtViewState.Ready(lines, truncated)
                }
            } catch (e: Exception) {
                logger.e(TAG, "State transition: ERROR | Failed to read text file: ${e.message}", e)
                val error = com.ayanpandey.blink.core.common.error.AppError.DocumentError.TextParsingError(e.message, e.stackTraceToString())
                TxtViewState.Error(error)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = viewState) {
            is TxtViewState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            is TxtViewState.Ready -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (state.truncated) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "Preview limited to ${MAX_LINES_PREVIEW.toLong() / 1_000}K lines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        itemsIndexed(state.lines) { index, line ->
                            Row(verticalAlignment = Alignment.Top) {
                                // Line number gutter
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.width(40.dp),
                                )
                                Text(
                                    text = line.ifEmpty { " " },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            is TxtViewState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "⚠ Failed to read file",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (val err = state.error) {
                            is com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied -> "Permission Denied: ${err.causeMessage ?: "No permission"}"
                            is com.ayanpandey.blink.core.common.error.AppError.DocumentError.TextParsingError -> "Parsing Error: ${err.detail ?: err.causeMessage ?: "Malformed file"}"
                            else -> err.toString()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
