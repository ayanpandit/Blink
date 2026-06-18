package com.ayanpandey.blink.feature.ppt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.model.DocumentType
import com.ayanpandey.blink.domain.repository.FileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.xslf.usermodel.XMLSlideShow

private const val TAG = "PptViewer"
private const val PPT_RENDER_WIDTH = 1024
private const val MAX_CACHED_SLIDES = 4

// ----- Data model -----

data class SlideInfo(val index: Int, val aspectRatio: Float)

private sealed interface PptViewState {
    data object Loading : PptViewState
    data class Ready(val slides: List<SlideInfo>, val slideCount: Int) : PptViewState
    data class Error(val error: com.ayanpandey.blink.core.common.error.AppError.DocumentError) : PptViewState
}

// ----- Composable -----

@Composable
fun PptViewer(
    document: Document,
    fileResolver: FileResolver,
    logger: BlinkLogger,
    modifier: Modifier = Modifier,
) {
    var viewState: PptViewState by remember { mutableStateOf(PptViewState.Loading) }
    val bitmapCache = remember { LruCache<Int, Bitmap>(MAX_CACHED_SLIDES) }
    val scope = rememberCoroutineScope()

    // Loader function exposed to page items
    val loadBitmap: suspend (Int) -> Bitmap? = remember(document.uri) {
        { pageIndex ->
            bitmapCache.get(pageIndex) ?: withContext(Dispatchers.IO) {
                try {
                    val inputStreamResult = fileResolver.openInputStream(document.uri)
                    if (inputStreamResult.isFailure) {
                        logger.e(TAG, "Failed to resolve stream for slide rendering: ${inputStreamResult.exceptionOrNull()?.message}")
                        null
                    } else {
                        val inputStream = inputStreamResult.getOrThrow()
                        val bitmap = when (document.documentType) {
                            DocumentType.PPTX -> renderPptxSlide(inputStream, pageIndex, PPT_RENDER_WIDTH)
                            DocumentType.PPT -> renderPptSlide(inputStream, pageIndex, PPT_RENDER_WIDTH)
                            else -> renderPptxSlide(inputStream, pageIndex, PPT_RENDER_WIDTH)
                        }
                        bitmap?.also { bitmapCache.put(pageIndex, it) }
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to render slide $pageIndex: ${e.message}", e)
                    null
                }
            }
        }
    }

    LaunchedEffect(document.uri) {
        viewState = PptViewState.Loading
        logger.d(TAG, "State transition: LOADING for ${document.displayName}")
        viewState = withContext(Dispatchers.IO) {
            try {
                val inputStreamResult = fileResolver.openInputStream(document.uri)
                if (inputStreamResult.isFailure) {
                    val ex = inputStreamResult.exceptionOrNull()!!
                    val error = when (ex) {
                        is SecurityException -> com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied(ex.message, ex.stackTraceToString())
                        is com.ayanpandey.blink.core.common.error.AppErrorException -> ex.error as? com.ayanpandey.blink.core.common.error.AppError.DocumentError ?: com.ayanpandey.blink.core.common.error.AppError.DocumentError.PowerPointParsingError(ex.message)
                        else -> com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied(ex.message, ex.stackTraceToString())
                    }
                    logger.e(TAG, "State transition: ERROR | uri=${document.uri} | error=$error", ex)
                    PptViewState.Error(error)
                } else {
                    val inputStream = inputStreamResult.getOrThrow()
                    val slides = when (document.documentType) {
                        DocumentType.PPTX -> getPptxSlideInfos(inputStream, PPT_RENDER_WIDTH)
                        DocumentType.PPT -> getPptSlideInfos(inputStream, PPT_RENDER_WIDTH)
                        else -> getPptxSlideInfos(inputStream, PPT_RENDER_WIDTH)
                    }
                    logger.d(TAG, "State transition: READY | Found ${slides.size} slides in ${document.displayName}")
                    PptViewState.Ready(slides, slides.size)
                }
            } catch (e: Exception) {
                logger.e(TAG, "State transition: ERROR | Failed to open presentation: ${e.message}", e)
                val error = com.ayanpandey.blink.core.common.error.AppError.DocumentError.PowerPointParsingError(e.message, e.stackTraceToString())
                PptViewState.Error(error)
            }
        }
    }

    DisposableEffect(document.uri) {
        onDispose { bitmapCache.evictAll() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = viewState) {
            is PptViewState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is PptViewState.Ready -> {
                var scale by remember { mutableFloatStateOf(1f) }
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 3.0f)
                            }
                        },
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.slides.size) { index ->
                        val slide = state.slides[index]
                        SlideItem(
                            slideIndex = index,
                            aspectRatio = slide.aspectRatio,
                            totalSlides = state.slideCount,
                            loadBitmap = { scope.launch { loadBitmap(index) }; bitmapCache.get(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                        LaunchedEffect(index) {
                            loadBitmap(index)
                        }
                    }
                }

                // Slide counter overlay
                val visibleIndex = listState.firstVisibleItemIndex + 1
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        "Slide $visibleIndex / ${state.slideCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            is PptViewState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "⚠ Failed to render presentation",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (val err = state.error) {
                            is com.ayanpandey.blink.core.common.error.AppError.DocumentError.DocumentPermissionDenied -> "Permission Denied: ${err.causeMessage ?: "No permission"}"
                            is com.ayanpandey.blink.core.common.error.AppError.DocumentError.PowerPointParsingError -> "Parsing Error: ${err.detail ?: err.causeMessage ?: "Malformed file"}"
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
private fun SlideItem(
    slideIndex: Int,
    aspectRatio: Float,
    totalSlides: Int,
    loadBitmap: () -> Bitmap?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(slideIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(slideIndex) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { loadBitmap() }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Slide ${slideIndex + 1} of $totalSlides",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

// ----- PPTX Renderers -----

private fun getPptxSlideInfos(inputStream: java.io.InputStream, renderWidth: Int): List<SlideInfo> {
    return XMLSlideShow(inputStream).use { show ->
        val pageSize = show.pageSize
        val aspectRatio = if (pageSize.height > 0) pageSize.width.toFloat() / pageSize.height.toFloat() else 16f / 9f
        show.slides.mapIndexed { index, _ ->
            SlideInfo(index = index, aspectRatio = aspectRatio)
        }
    }
}

private fun renderPptxSlide(inputStream: java.io.InputStream, slideIndex: Int, renderWidth: Int): Bitmap? {
    return XMLSlideShow(inputStream).use { show ->
        val slides = show.slides
        if (slideIndex >= slides.size) return@use null
        val slide = slides[slideIndex]
        val pageSize = show.pageSize
        val aspectRatio = if (pageSize.height > 0) pageSize.width.toFloat() / pageSize.height.toFloat() else 16f / 9f
        val renderHeight = (renderWidth / aspectRatio).toInt()

        val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val scale = renderWidth.toFloat() / pageSize.width.toFloat()
        canvas.scale(scale, scale)
        slide.draw(com.android.internal.awt.AndroidGraphics2D(canvas))
        bitmap
    }
}

// ----- PPT (Legacy) Renderers -----

private fun getPptSlideInfos(inputStream: java.io.InputStream, renderWidth: Int): List<SlideInfo> {
    return HSLFSlideShow(inputStream).use { show ->
        val pageSize = show.pageSize
        val aspectRatio = if (pageSize.height > 0) pageSize.width.toFloat() / pageSize.height.toFloat() else 4f / 3f
        show.slides.mapIndexed { index, _ ->
            SlideInfo(index = index, aspectRatio = aspectRatio)
        }
    }
}

private fun renderPptSlide(inputStream: java.io.InputStream, slideIndex: Int, renderWidth: Int): Bitmap? {
    return HSLFSlideShow(inputStream).use { show ->
        val slides = show.slides
        if (slideIndex >= slides.size) return@use null
        val slide = slides[slideIndex]
        val pageSize = show.pageSize
        val aspectRatio = if (pageSize.height > 0) pageSize.width.toFloat() / pageSize.height.toFloat() else 4f / 3f
        val renderHeight = (renderWidth / aspectRatio).toInt()

        val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val scale = renderWidth.toFloat() / pageSize.width.toFloat()
        canvas.scale(scale, scale)
        slide.draw(com.android.internal.awt.AndroidGraphics2D(canvas))
        bitmap
    }
}
