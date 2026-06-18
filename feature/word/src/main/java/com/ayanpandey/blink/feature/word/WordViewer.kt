package com.ayanpandey.blink.feature.word

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFDocument

private const val TAG = "WordViewer"

// ----- Data model -----

sealed interface WordBlock {
    data class Paragraph(val text: AnnotatedString, val style: ParagraphStyle) : WordBlock
    data object Divider : WordBlock
}

enum class ParagraphStyle { HEADING1, HEADING2, HEADING3, BODY, CAPTION }

// ----- State -----

private sealed interface WordViewState {
    data object Loading : WordViewState
    data class Ready(val blocks: List<WordBlock>) : WordViewState
    data class Error(val message: String) : WordViewState
}

// ----- Composable -----

@Composable
fun WordViewer(
    document: Document,
    fileResolver: FileResolver,
    logger: BlinkLogger,
    modifier: Modifier = Modifier,
) {
    var viewState: WordViewState by remember { mutableStateOf(WordViewState.Loading) }

    LaunchedEffect(document.uri) {
        viewState = WordViewState.Loading
        viewState = withContext(Dispatchers.IO) {
            try {
                val inputStream = fileResolver.openInputStream(document.uri).getOrThrow()
                val blocks = when (document.documentType) {
                    DocumentType.DOCX -> parseDocx(inputStream)
                    DocumentType.DOC -> parseDoc(inputStream)
                    else -> parseDocx(inputStream)
                }
                logger.d(TAG, "Parsed ${blocks.size} blocks from ${document.displayName}")
                WordViewState.Ready(blocks)
            } catch (e: Exception) {
                logger.e(TAG, "Failed to parse document: ${e.message}", e)
                WordViewState.Error(e.message ?: "Unknown parsing error")
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = viewState) {
            is WordViewState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is WordViewState.Ready -> {
                WordContent(blocks = state.blocks, modifier = Modifier.fillMaxSize())
            }

            is WordViewState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "⚠ Failed to render document",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WordContent(blocks: List<WordBlock>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(blocks, key = { block -> blocks.indexOf(block) }) { block ->
            when (block) {
                is WordBlock.Paragraph -> {
                    val textStyle = when (block.style) {
                        ParagraphStyle.HEADING1 -> MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold, fontSize = 26.sp
                        )
                        ParagraphStyle.HEADING2 -> MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold, fontSize = 22.sp
                        )
                        ParagraphStyle.HEADING3 -> MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold, fontSize = 18.sp
                        )
                        ParagraphStyle.CAPTION -> MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ParagraphStyle.BODY -> MaterialTheme.typography.bodyMedium
                    }
                    val topPadding = when (block.style) {
                        ParagraphStyle.HEADING1 -> 16.dp
                        ParagraphStyle.HEADING2 -> 12.dp
                        ParagraphStyle.HEADING3 -> 8.dp
                        else -> 2.dp
                    }
                    if (block.text.text.isNotBlank()) {
                        Text(
                            text = block.text,
                            style = textStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = topPadding),
                        )
                    }
                }

                is WordBlock.Divider -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// ----- Parsers -----

private fun parseDocx(inputStream: java.io.InputStream): List<WordBlock> {
    val blocks = mutableListOf<WordBlock>()
    XWPFDocument(inputStream).use { doc ->
        for (paragraph in doc.paragraphs) {
            val styleId = paragraph.style?.lowercase() ?: ""
            val paragraphStyle = when {
                styleId.contains("heading1") || styleId == "title" -> ParagraphStyle.HEADING1
                styleId.contains("heading2") || styleId == "subtitle" -> ParagraphStyle.HEADING2
                styleId.contains("heading3") -> ParagraphStyle.HEADING3
                styleId.contains("caption") -> ParagraphStyle.CAPTION
                else -> ParagraphStyle.BODY
            }

            val annotated = buildAnnotatedString {
                for (run in paragraph.runs) {
                    val start = length
                    append(run.text() ?: "")
                    val end = length
                    if (run.isBold) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    if (run.isItalic) addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    if (run.isStrikeThrough) addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                    @Suppress("DEPRECATION")
                    if (run.underline) addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
            }
            blocks.add(WordBlock.Paragraph(annotated, paragraphStyle))
        }
    }
    return blocks
}

private fun parseDoc(inputStream: java.io.InputStream): List<WordBlock> {
    val blocks = mutableListOf<WordBlock>()
    HWPFDocument(inputStream).use { doc ->
        val range = doc.range
        for (i in 0 until range.numParagraphs()) {
            val para = range.getParagraph(i)
            val text = para.text().trimEnd('\r', '\u0000')
            val styleIndex = para.styleIndex
            val paragraphStyle = when {
                styleIndex == 1 -> ParagraphStyle.HEADING1
                styleIndex == 2 -> ParagraphStyle.HEADING2
                styleIndex == 3 -> ParagraphStyle.HEADING3
                styleIndex == 4 -> ParagraphStyle.HEADING3
                else -> ParagraphStyle.BODY
            }
            val annotated = buildAnnotatedString { append(text) }
            blocks.add(WordBlock.Paragraph(annotated, paragraphStyle))
        }
    }
    return blocks
}
