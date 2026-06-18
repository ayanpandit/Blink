package com.ayanpandey.blink.feature.excel

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory

private const val TAG = "ExcelViewer"
private const val MAX_COLS = 26
private const val CELL_WIDTH = 120
private const val ROW_HEADER_WIDTH = 48
private const val CELL_HEIGHT = 36

// ----- Data model -----

data class SheetData(
    val name: String,
    val rows: List<List<String>>,
    val maxCols: Int,
)

private sealed interface ExcelViewState {
    data object Loading : ExcelViewState
    data class Ready(val sheets: List<SheetData>) : ExcelViewState
    data class Error(val error: com.ayanpandey.blink.core.common.error.AppError.DocumentError) : ExcelViewState
}

// ----- Composable -----

@Composable
fun ExcelViewer(
    document: Document,
    fileResolver: FileResolver,
    logger: BlinkLogger,
    modifier: Modifier = Modifier,
) {
    var viewState: ExcelViewState by remember { mutableStateOf(ExcelViewState.Loading) }
    var selectedSheet by remember { mutableIntStateOf(0) }

    LaunchedEffect(document.uri) {
        viewState = ExcelViewState.Loading
        logger.d(TAG, "State transition: LOADING for ${document.displayName}")
        viewState = withContext(Dispatchers.IO) {
            try {
                val inputStreamResult = fileResolver.openInputStream(document.uri)
                if (inputStreamResult.isFailure) {
                    val ex = inputStreamResult.exceptionOrNull()!!
                    val error = when (ex) {
                        is SecurityException -> com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied(ex.message, ex.stackTraceToString())
                        is com.ayanpandey.blink.core.common.error.AppErrorException -> ex.error as? com.ayanpandey.blink.core.common.error.AppError.DocumentError ?: com.ayanpandey.blink.core.common.error.AppError.DocumentError.ExcelParsingError(ex.message)
                        else -> com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied(ex.message, ex.stackTraceToString())
                    }
                    logger.e(TAG, "State transition: ERROR | uri=${document.uri} | error=$error", ex)
                    ExcelViewState.Error(error)
                } else {
                    val inputStream = inputStreamResult.getOrThrow()
                    val sheets = parseWorkbook(inputStream)
                    logger.d(TAG, "State transition: READY | Parsed ${sheets.size} sheets from ${document.displayName}")
                    ExcelViewState.Ready(sheets)
                }
            } catch (e: Exception) {
                logger.e(TAG, "State transition: ERROR | Failed to parse Excel document: ${e.message}", e)
                val error = com.ayanpandey.blink.core.common.error.AppError.DocumentError.ExcelParsingError(e.message, e.stackTraceToString())
                ExcelViewState.Error(error)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = viewState) {
            is ExcelViewState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ExcelViewState.Ready -> {
                if (state.sheets.isEmpty()) {
                    Text(
                        "Empty workbook",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.sheets.size > 1) {
                            ScrollableTabRow(
                                selectedTabIndex = selectedSheet,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                state.sheets.forEachIndexed { index, sheet ->
                                    Tab(
                                        selected = selectedSheet == index,
                                        onClick = { selectedSheet = index },
                                        text = {
                                            Text(
                                                text = sheet.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        SheetGrid(
                            sheet = state.sheets[selectedSheet],
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                        )
                    }
                }
            }

            is ExcelViewState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "⚠ Failed to render spreadsheet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (val err = state.error) {
                            is com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied -> "Permission Denied: ${err.causeMessage ?: "No permission"}"
                            is com.ayanpandey.blink.core.common.error.AppError.DocumentError.ExcelParsingError -> "Parsing Error: ${err.detail ?: err.causeMessage ?: "Malformed file"}"
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

@Composable
private fun SheetGrid(sheet: SheetData, modifier: Modifier = Modifier) {
    val horizontalScroll = rememberScrollState()
    val colCount = minOf(sheet.maxCols + 1, MAX_COLS)

    Column(modifier = modifier) {
        // Column header row (A, B, C, ...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            // Empty corner
            GridCell(
                text = "",
                isHeader = true,
                modifier = Modifier.width(ROW_HEADER_WIDTH.dp),
            )
            repeat(colCount) { colIndex ->
                GridCell(
                    text = columnLetter(colIndex),
                    isHeader = true,
                    modifier = Modifier.width(CELL_WIDTH.dp),
                )
            }
        }

        // Data rows
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sheet.rows.size) { rowIndex ->
                val row = sheet.rows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll)
                        .background(
                            if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                ) {
                    // Row number
                    GridCell(
                        text = "${rowIndex + 1}",
                        isHeader = true,
                        modifier = Modifier.width(ROW_HEADER_WIDTH.dp),
                    )
                    repeat(colCount) { colIndex ->
                        val cellValue = row.getOrElse(colIndex) { "" }
                        GridCell(
                            text = cellValue,
                            isHeader = false,
                            modifier = Modifier.width(CELL_WIDTH.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    text: String,
    isHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(CELL_HEIGHT.dp)
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
            modifier = Modifier.widthIn(max = (CELL_WIDTH - 16).dp),
        )
    }
}

private fun columnLetter(index: Int): String {
    var n = index + 1
    var result = ""
    while (n > 0) {
        val rem = (n - 1) % 26
        result = ('A' + rem) + result
        n = (n - 1) / 26
    }
    return result
}

// ----- Parser -----

private fun parseWorkbook(inputStream: java.io.InputStream): List<SheetData> {
    val sheets = mutableListOf<SheetData>()
    WorkbookFactory.create(inputStream).use { workbook ->
        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            val rows = mutableListOf<List<String>>()
            var maxCols = 0

            for (rowIndex in 0..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex)
                if (row == null) {
                    rows.add(emptyList())
                    continue
                }
                val cells = mutableListOf<String>()
                val lastCol = row.lastCellNum.toInt()
                if (lastCol > maxCols) maxCols = lastCol

                for (colIndex in 0 until lastCol) {
                    val cell = row.getCell(colIndex)
                    val value = when (cell?.cellType) {
                        CellType.STRING -> cell.stringCellValue ?: ""
                        CellType.NUMERIC -> {
                            val d = cell.numericCellValue
                            if (d == kotlin.math.floor(d)) d.toLong().toString() else d.toString()
                        }
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.FORMULA -> {
                            try { cell.cachedFormulaResultType.name } catch (e: Exception) { "=" }
                        }
                        CellType.BLANK, null -> ""
                        else -> ""
                    }
                    cells.add(value)
                }
                rows.add(cells)
            }

            sheets.add(SheetData(name = sheet.sheetName, rows = rows, maxCols = maxCols))
        }
    }
    return sheets
}
