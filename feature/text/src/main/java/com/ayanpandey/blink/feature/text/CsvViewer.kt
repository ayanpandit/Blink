package com.ayanpandey.blink.feature.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.repository.FileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CsvViewer"
private const val MAX_ROWS = 10_000
private const val CELL_WIDTH_CSV = 140
private const val ROW_HEADER_WIDTH_CSV = 48
private const val CELL_HEIGHT_CSV = 36

private sealed interface CsvViewState {
    data object Loading : CsvViewState
    data class Ready(val rows: List<List<String>>, val maxCols: Int, val truncated: Boolean) : CsvViewState
    data class Error(val message: String) : CsvViewState
}

@Composable
fun CsvViewer(
    document: Document,
    fileResolver: FileResolver,
    logger: BlinkLogger,
    modifier: Modifier = Modifier,
) {
    var viewState: CsvViewState by remember { mutableStateOf(CsvViewState.Loading) }

    LaunchedEffect(document.uri) {
        viewState = CsvViewState.Loading
        viewState = withContext(Dispatchers.IO) {
            try {
                val inputStream = fileResolver.openInputStream(document.uri).getOrThrow()
                val (rows, truncated) = parseCsv(inputStream)
                val maxCols = rows.maxOfOrNull { it.size } ?: 0
                logger.d(TAG, "Parsed ${rows.size} rows, $maxCols cols from ${document.displayName}")
                CsvViewState.Ready(rows, maxCols, truncated)
            } catch (e: Exception) {
                logger.e(TAG, "Failed to parse CSV: ${e.message}", e)
                CsvViewState.Error(e.message ?: "Unknown error")
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = viewState) {
            is CsvViewState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            is CsvViewState.Ready -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (state.truncated) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "Preview limited to ${MAX_ROWS.toLong() / 1_000}K rows",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }

                    val horizontalScroll = rememberScrollState()

                    // Column header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScroll)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        CsvCell(text = "#", isHeader = true, modifier = Modifier.width(ROW_HEADER_WIDTH_CSV.dp))
                        repeat(state.maxCols) { colIndex ->
                            CsvCell(
                                text = csvColumnLetter(colIndex),
                                isHeader = true,
                                modifier = Modifier.width(CELL_WIDTH_CSV.dp),
                            )
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.rows.size) { rowIndex ->
                            val row = state.rows[rowIndex]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScroll)
                                    .background(
                                        if (rowIndex == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    ),
                            ) {
                                CsvCell(
                                    text = "$rowIndex",
                                    isHeader = true,
                                    modifier = Modifier.width(ROW_HEADER_WIDTH_CSV.dp),
                                )
                                repeat(state.maxCols) { colIndex ->
                                    CsvCell(
                                        text = row.getOrElse(colIndex) { "" },
                                        isHeader = rowIndex == 0,
                                        modifier = Modifier.width(CELL_WIDTH_CSV.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is CsvViewState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "⚠ Failed to parse CSV",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CsvCell(text: String, isHeader: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(CELL_HEIGHT_CSV.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isHeader) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = if (isHeader) {
                MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            } else {
                MaterialTheme.typography.bodySmall
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = (CELL_WIDTH_CSV - 16).dp),
        )
    }
}

private fun csvColumnLetter(index: Int): String {
    var n = index + 1
    var result = ""
    while (n > 0) {
        val rem = (n - 1) % 26
        result = ('A' + rem) + result
        n = (n - 1) / 26
    }
    return result
}

private fun parseCsv(inputStream: java.io.InputStream): Pair<List<List<String>>, Boolean> {
    val rows = mutableListOf<List<String>>()
    var truncated = false
    inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
        var line = reader.readLine()
        while (line != null) {
            if (rows.size >= MAX_ROWS) {
                truncated = true
                break
            }
            rows.add(parseCsvLine(line))
            line = reader.readLine()
        }
    }
    return rows to truncated
}

private fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val currentField = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        when {
            ch == '"' && !inQuotes -> inQuotes = true
            ch == '"' && inQuotes -> {
                // Check for escaped quote
                if (i + 1 < line.length && line[i + 1] == '"') {
                    currentField.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            }
            ch == ',' && !inQuotes -> {
                fields.add(currentField.toString())
                currentField.clear()
            }
            else -> currentField.append(ch)
        }
        i++
    }
    fields.add(currentField.toString())
    return fields
}
