package com.ayanpandey.blink.feature.pdf

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ayanpandey.blink.core.common.error.AppErrorException
import com.ayanpandey.blink.core.common.logging.BlinkLogger
import com.ayanpandey.blink.domain.model.Document
import com.ayanpandey.blink.domain.repository.FileResolver
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PdfViewer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewer(
    document: Document,
    fileResolver: FileResolver,
    logger: BlinkLogger,
    initialPage: Int = 1,
    onPageChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    // Lifecycle variables
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pdfiumCore by remember { mutableStateOf<PdfiumCore?>(null) }
    var pdfDocument by remember { mutableStateOf<PdfDocument?>(null) }

    // State variables
    var loadingState by remember { mutableStateOf<PdfLoadingState>(PdfLoadingState.Loading) }
    var pageCount by remember { mutableStateOf(0) }
    var pageDimensions by remember { mutableStateOf<List<PageSize>>(emptyList()) }

    // Page cache (LRU cache of max 6 rendered page bitmaps to restrict memory size)
    val pageCache = remember {
        object : LruCache<Int, Bitmap>(6) {
            override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
                super.entryRemoved(evicted, key, oldValue, newValue)
                if (evicted) {
                    logger.d(TAG, "Evicting page $key bitmap from LRU cache to reclaim memory")
                    oldValue?.recycle()
                }
            }
        }
    }

    // Load PDF Document
    LaunchedEffect(document.uri) {
        withContext(Dispatchers.IO) {
            logger.i(TAG, "Launched load of PDF Document: ${document.displayName}")
            try {
                val pfdResult = fileResolver.openFileDescriptor(document.uri)
                if (pfdResult.isFailure) {
                    val exception = pfdResult.exceptionOrNull() ?: Exception("Failed to open FileDescriptor")
                    val appError = (exception as? AppErrorException)?.error
                        ?: com.ayanpandey.blink.core.common.error.AppError.UnknownError(exception)
                    logger.e(TAG, "Failed to open FileDescriptor: ${exception.message}")
                    withContext(Dispatchers.Main) {
                        loadingState = PdfLoadingState.Error(appError)
                    }
                    return@withContext
                }

                val openedPfd = pfdResult.getOrThrow()
                pfd = openedPfd

                val core = PdfiumCore(context)
                pdfiumCore = core

                val doc = core.newDocument(openedPfd)
                pdfDocument = doc

                val count = core.getPageCount(doc)
                logger.i(TAG, "PDF document opened successfully. Page count: $count")

                // Pre-cache dimensions of all pages
                val sizes = ArrayList<PageSize>(count)
                for (i in 0 until count) {
                    core.openPage(doc, i)
                    val w = core.getPageWidthPoint(doc, i)
                    val h = core.getPageHeightPoint(doc, i)
                    sizes.add(PageSize(w, h))
                }

                pageDimensions = sizes
                pageCount = count
                withContext(Dispatchers.Main) {
                    loadingState = PdfLoadingState.Ready
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error opening PDF document", e)
                withContext(Dispatchers.Main) {
                    loadingState = PdfLoadingState.Error(
                        com.ayanpandey.blink.core.common.error.AppError.DocumentError.CorruptedDocument
                    )
                }
            }
        }
    }

    // Disposal cleanup
    DisposableEffect(Unit) {
        onDispose {
            logger.i(TAG, "Disposing PdfViewer and closing resources")
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Recycle cached bitmaps
                    pageCache.evictAll()
                    pdfDocument?.let { doc ->
                        pdfiumCore?.closeDocument(doc)
                    }
                    pfd?.close()
                } catch (e: Exception) {
                    logger.e(TAG, "Exception closing PDF JNI resources", e)
                }
            }
        }
    }

    // Render States
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when (val currentLoadingState = loadingState) {
            is PdfLoadingState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading PDF pages...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            is PdfLoadingState.Ready -> {
                if (pageCount > 0 && pdfDocument != null && pdfiumCore != null) {
                    PdfPagesViewport(
                        pdfiumCore = pdfiumCore!!,
                        pdfDocument = pdfDocument!!,
                        pageCount = pageCount,
                        pageDimensions = pageDimensions,
                        cache = pageCache,
                        initialPage = initialPage,
                        onPageChanged = onPageChanged,
                        logger = logger
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("This PDF document is empty.")
                    }
                }
            }
            is PdfLoadingState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Failed to render PDF",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: ${currentLoadingState.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPagesViewport(
    pdfiumCore: PdfiumCore,
    pdfDocument: PdfDocument,
    pageCount: Int,
    pageDimensions: List<PageSize>,
    cache: LruCache<Int, Bitmap>,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    logger: BlinkLogger
) {
    // Zoom and pan transform states
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = (initialPage - 1).coerceAtLeast(0))
    val firstVisiblePage by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex + 1 }
    }

    LaunchedEffect(firstVisiblePage) {
        if (pageCount > 0) {
            onPageChanged(firstVisiblePage)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            val maxOffsetX = (width * (scale - 1) / 2).coerceAtLeast(0f)
                            val maxOffsetY = (height * (scale - 1) / 2).coerceAtLeast(0f)
                            offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(pageDimensions) { index, size ->
                    PdfPageItem(
                        pageIndex = index,
                        pdfiumCore = pdfiumCore,
                        pdfDocument = pdfDocument,
                        pageSize = size,
                        cache = cache,
                        logger = logger
                    )
                }
            }
        }

        // Current page overlay in bottom right corner (never scales)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Row(
                modifier = Modifier
                    .shadow(4.dp, shape = RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page $firstVisiblePage of $pageCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    pdfiumCore: PdfiumCore,
    pdfDocument: PdfDocument,
    pageSize: PageSize,
    cache: LruCache<Int, Bitmap>,
    logger: BlinkLogger
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(cache.get(pageIndex)) }

    // Load bitmap asynchronously when item enters composition scope
    if (pageBitmap == null) {
        LaunchedEffect(pageIndex) {
            withContext(Dispatchers.IO) {
                try {
                    // Standard scaling multiplier (1.5x of PDF points resolves excellent quality)
                    val scaleFactor = 1.5f
                    val targetWidth = (pageSize.width * scaleFactor).toInt()
                    val targetHeight = (pageSize.height * scaleFactor).toInt()

                    logger.d(TAG, "Rendering PDF Page $pageIndex to bitmap ($targetWidth x $targetHeight)")
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

                    synchronized(pdfiumCore) {
                        pdfiumCore.openPage(pdfDocument, pageIndex)
                        pdfiumCore.renderPageBitmap(
                            pdfDocument,
                            bitmap,
                            pageIndex,
                            0,
                            0,
                            targetWidth,
                            targetHeight
                        )
                    }

                    cache.put(pageIndex, bitmap)
                    withContext(Dispatchers.Main) {
                        pageBitmap = bitmap
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Error rendering page $pageIndex to bitmap", e)
                }
            }
        }
    }

    val aspectRatio = pageSize.width.toFloat() / pageSize.height.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .shadow(2.dp, shape = RoundedCornerShape(4.dp))
            .background(Color.White, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = pageBitmap
        if (currentBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

        } else {
            CircularProgressIndicator(
                modifier = Modifier.wrapContentSize(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private data class PageSize(val width: Int, val height: Int)

private sealed interface PdfLoadingState {
    data object Loading : PdfLoadingState
    data object Ready : PdfLoadingState
    data class Error(val error: com.ayanpandey.blink.core.common.error.AppError) : PdfLoadingState
}
