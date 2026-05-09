package com.example.LyricBox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.io.File

private data class QueueUiItem(
    val id: Long,
    val audio: AudioFile
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingPlaylistBottomSheet(
    queue: List<AudioFile>,
    currentAudioPath: String?,
    onDismiss: () -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onPlayAtIndex: (Int) -> Unit,
    onRemoveAtIndex: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollBlocker = rememberSheetScrollBlocker()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var idSeed by remember { mutableLongStateOf(1L) }
    var draggingItemId by remember { mutableLongStateOf(-1L) }
    var dragTranslationY by remember { mutableFloatStateOf(0f) }
    var hasAutoLocatedCurrent by remember { mutableStateOf(false) }
    var localQueue by remember {
        mutableStateOf(queue.map { QueueUiItem(id = idSeed++, audio = it) })
    }

    LaunchedEffect(queue, draggingItemId) {
        if (draggingItemId < 0L) {
            localQueue = reconcileQueueUiItems(
                current = localQueue,
                incoming = queue,
                nextId = { idSeed++ }
            )
        }
    }

    LaunchedEffect(localQueue, currentAudioPath) {
        if (hasAutoLocatedCurrent) return@LaunchedEffect
        val index = localQueue.indexOfFirst { it.audio.path == currentAudioPath }
        if (index >= 0) {
            listState.requestScrollToItem(index)
            hasAutoLocatedCurrent = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = draggingItemId < 0L,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(horizontal = 14.dp)
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "播放列表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val index = localQueue.indexOfFirst { it.audio.path == currentAudioPath }
                        if (index >= 0) {
                            listState.requestScrollToItem(index)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = "定位当前播放"
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBlocker)
                        .padding(end = 18.dp)
                ) {
                    itemsIndexed(
                        items = localQueue,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val audio = item.audio
                        val isCurrent = audio.path == currentAudioPath
                        val isDragging = item.id == draggingItemId
                        val rowModifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragTranslationY
                                }
                            }
                            .zIndex(if (isDragging) 1f else 0f)

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart && draggingItemId < 0L) {
                                    val removeIndex = localQueue.indexOfFirst { it.id == item.id }
                                    if (removeIndex >= 0) {
                                        localQueue = localQueue.toMutableList().apply { removeAt(removeIndex) }
                                        onRemoveAtIndex(removeIndex)
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = draggingItemId < 0L,
                            backgroundContent = {
                                if (draggingItemId < 0L) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = "删除",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            QueueRow(
                                modifier = rowModifier,
                                audio = audio,
                                isCurrent = isCurrent,
                                isDragging = isDragging,
                                onPlay = { onPlayAtIndex(index) },
                                onDrag = { changeY ->
                                    if (draggingItemId < 0L) return@QueueRow
                                    dragTranslationY += changeY

                                    val draggingInfo = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == draggingItemId }
                                    if (draggingInfo != null) {
                                        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
                                        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()
                                        val centerY = draggingInfo.offset + draggingInfo.size / 2f + dragTranslationY
                                        val edgeThreshold = draggingInfo.size * 0.9f
                                        val scrollDelta = when {
                                            centerY < viewportStart + edgeThreshold -> {
                                                -((viewportStart + edgeThreshold - centerY) / 7f).coerceIn(6f, 34f)
                                            }
                                            centerY > viewportEnd - edgeThreshold -> {
                                                ((centerY - (viewportEnd - edgeThreshold)) / 7f).coerceIn(6f, 34f)
                                            }
                                            else -> 0f
                                        }
                                        if (scrollDelta != 0f) {
                                            val consumed = listState.dispatchRawDelta(scrollDelta)
                                            dragTranslationY += consumed
                                        }
                                    }

                                    while (true) {
                                        val from = localQueue.indexOfFirst { it.id == draggingItemId }
                                        if (from < 0) break
                                        val currentInfo = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == draggingItemId }
                                        val itemHeight = currentInfo?.size?.toFloat() ?: 92f
                                        val swapThreshold = itemHeight * 0.5f
                                        when {
                                            dragTranslationY >= swapThreshold && from < localQueue.lastIndex -> {
                                                val to = from + 1
                                                localQueue = localQueue.toMutableList().apply {
                                                    add(to, removeAt(from))
                                                }
                                                onMoveItem(from, to)
                                                dragTranslationY -= itemHeight
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            dragTranslationY <= -swapThreshold && from > 0 -> {
                                                val to = from - 1
                                                localQueue = localQueue.toMutableList().apply {
                                                    add(to, removeAt(from))
                                                }
                                                onMoveItem(from, to)
                                                dragTranslationY += itemHeight
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            else -> break
                                        }
                                    }
                                },
                                onDragStart = {
                                    draggingItemId = item.id
                                    dragTranslationY = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    draggingItemId = -1L
                                    dragTranslationY = 0f
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
                    }
                }

                QueueFastScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(end = 2.dp, top = 6.dp, bottom = 6.dp),
                    totalCount = localQueue.size,
                    listState = listState,
                    onRequestScroll = { targetIndex ->
                        listState.requestScrollToItem(
                            targetIndex.coerceIn(0, (localQueue.size - 1).coerceAtLeast(0))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberSheetScrollBlocker(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(x = 0f, y = available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(x = 0f, y = available.y)
        }
    }
}

@Composable
private fun QueueRow(
    modifier: Modifier,
    audio: AudioFile,
    isCurrent: Boolean,
    isDragging: Boolean,
    onPlay: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    ListItem(
        modifier = modifier.clickable(onClick = onPlay),
        colors = ListItemDefaults.colors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        headlineContent = {
            Text(
                text = audio.displayTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = audio.displayArtist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            QueueSongLeadingCover(audio = audio, isCurrent = isCurrent)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.pointerInput(audio.path) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                }
            )
        }
    )
}

@Composable
private fun QueueSongLeadingCover(
    audio: AudioFile,
    isCurrent: Boolean
) {
    var cover by remember(audio.coverCachePath, audio.path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(audio.coverCachePath, audio.path) {
        cover = withContext(Dispatchers.IO) {
            decodeQueueCoverBitmap(audio.coverCachePath, audio.path)
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (cover != null) {
            Image(
                bitmap = cover!!.asImageBitmap(),
                contentDescription = "封面",
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "正在播放",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QueueFastScrollbar(
    modifier: Modifier = Modifier,
    totalCount: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRequestScroll: (Int) -> Unit
) {
    if (totalCount <= 1) return
    val density = LocalDensity.current
    val visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val maxStartIndex = (totalCount - visibleCount).coerceAtLeast(1).toFloat()
    val averageItemSize = listState.layoutInfo.visibleItemsInfo
        .map { it.size }
        .average()
        .takeIf { it > 1.0 } ?: 1.0
    val fractionalIndex = listState.firstVisibleItemIndex +
        (listState.firstVisibleItemScrollOffset / averageItemSize).toFloat()
    val progress = (fractionalIndex / maxStartIndex).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(14.dp)
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val minThumbPx = with(density) { 36.dp.toPx() }
        val thumbHeightPx = (trackHeightPx * (visibleCount.toFloat() / totalCount.toFloat()))
            .coerceIn(minThumbPx, trackHeightPx)
        val maxThumbOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
        val thumbOffsetPx = progress * maxThumbOffsetPx

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .pointerInput(totalCount, visibleCount) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val available = (size.height - thumbHeightPx).coerceAtLeast(1f)
                            val fraction = ((start.y - thumbHeightPx / 2f) / available).coerceIn(0f, 1f)
                            val target = (fraction * (totalCount - visibleCount).coerceAtLeast(0)).roundToInt()
                            onRequestScroll(target)
                        }
                    ) { change, _ ->
                        val available = (size.height - thumbHeightPx).coerceAtLeast(1f)
                        val fraction = ((change.position.y - thumbHeightPx / 2f) / available).coerceIn(0f, 1f)
                        val target = (fraction * (totalCount - visibleCount).coerceAtLeast(0)).roundToInt()
                        onRequestScroll(target)
                        change.consume()
                    }
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = thumbOffsetPx }
                .size(width = 10.dp, height = with(density) { thumbHeightPx.toDp() })
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
        )
    }
}

private fun reconcileQueueUiItems(
    current: List<QueueUiItem>,
    incoming: List<AudioFile>,
    nextId: () -> Long
): List<QueueUiItem> {
    if (incoming.isEmpty()) return emptyList()
    val buckets = linkedMapOf<String, ArrayDeque<QueueUiItem>>()
    current.forEach { item ->
        buckets.getOrPut(item.audio.path) { ArrayDeque() }.addLast(item)
    }
    return incoming.map { audio ->
        val reused = buckets[audio.path]?.removeFirstOrNull()
        if (reused != null) {
            reused.copy(audio = audio)
        } else {
            QueueUiItem(id = nextId(), audio = audio)
        }
    }
}

private fun decodeQueueCoverBitmap(cachePath: String?, audioPath: String?): Bitmap? {
    val fromCache = if (!cachePath.isNullOrBlank()) {
        runCatching {
            val file = File(cachePath)
            if (!file.exists()) return@runCatching null
            val bytes = file.readBytes()
            decodeQueueCoverBitmapFromBytes(bytes)
        }.getOrNull()
    } else {
        null
    }
    if (fromCache != null) return fromCache

    if (audioPath.isNullOrBlank()) return null
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(audioPath)
        val coverBytes = retriever.embeddedPicture
        retriever.release()
        decodeQueueCoverBitmapFromBytes(coverBytes)
    }.getOrNull()
}

private fun decodeQueueCoverBitmapFromBytes(bytes: ByteArray?): Bitmap? {
    if (bytes == null || bytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    options.inJustDecodeBounds = false
    options.inSampleSize = calculateCoverSampleSize(options, 96, 96)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun calculateCoverSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val width = options.outWidth
    val height = options.outHeight
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}
