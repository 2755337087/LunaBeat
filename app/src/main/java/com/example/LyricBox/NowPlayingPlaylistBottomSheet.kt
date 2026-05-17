package com.example.LyricBox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.io.File

private data class QueueUiItem(
    val id: Long,
    val audio: AudioFile
)

private const val QUEUE_REMOVE_SYNC_DELAY_MS = 160L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingPlaylistBottomSheet(
    queue: List<AudioFile>,
    currentAudioPath: String?,
    canReorder: Boolean,
    onDismiss: () -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onPlayAtIndex: (Int) -> Unit,
    onRemoveByPath: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollBlocker = rememberSheetScrollBlocker()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var idSeed by remember { mutableLongStateOf(1L) }
    var draggingItemId by remember { mutableLongStateOf(-1L) }
    var draggingStartIndex by remember { mutableIntStateOf(-1) }
    var dragTranslationY by remember { mutableFloatStateOf(0f) }
    var dragCenterY by remember { mutableFloatStateOf(Float.NaN) }
    var dragItemHeight by remember { mutableFloatStateOf(92f) }
    var hasAutoLocatedCurrent by remember { mutableStateOf(false) }
    var dragSurfaceOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    val dragHandleBounds = remember { mutableStateMapOf<Long, Rect>() }
    var localQueue by remember {
        mutableStateOf(queue.distinctBy { it.path }.map { QueueUiItem(id = idSeed++, audio = it) })
    }

    fun finishDrag() {
        val movedItemId = draggingItemId
        if (movedItemId < 0L) return
        val fromIndex = draggingStartIndex
        val toIndex = localQueue.indexOfFirst { it.id == movedItemId }
        if (canReorder && fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
            onMoveItem(fromIndex, toIndex)
        }
        draggingItemId = -1L
        draggingStartIndex = -1
        dragTranslationY = 0f
        dragCenterY = Float.NaN
    }

    fun removeQueueItem(itemId: Long) {
        if (draggingItemId >= 0L) return
        val removeIndex = localQueue.indexOfFirst { it.id == itemId }
        if (removeIndex >= 0) {
            val removedPath = localQueue[removeIndex].audio.path
            localQueue = localQueue.toMutableList().apply { removeAt(removeIndex) }
            dragHandleBounds.remove(itemId)
            scope.launch {
                // Let placement animation start first, then sync playback queue in background.
                delay(QUEUE_REMOVE_SYNC_DELAY_MS)
                onRemoveByPath(removedPath)
            }
        }
    }

    fun startDragAt(startOffset: Offset) {
        if (!canReorder || draggingItemId >= 0L) return
        val startInRoot = dragSurfaceOffsetInRoot + startOffset
        val visibleKeys = listState.layoutInfo.visibleItemsInfo.map { it.key }.toSet()
        val item = localQueue.firstOrNull {
            it.id in visibleKeys && dragHandleBounds[it.id]?.contains(startInRoot) == true
        } ?: return
        draggingItemId = item.id
        draggingStartIndex = localQueue.indexOfFirst { it.id == item.id }
        dragTranslationY = 0f
        val info = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == item.id }
        dragItemHeight = info?.size?.toFloat() ?: 92f
        dragCenterY = if (info != null) {
            info.offset + info.size / 2f
        } else {
            Float.NaN
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun updateDrag(changeY: Float) {
        if (!canReorder || draggingItemId < 0L) return
        dragTranslationY += changeY
        if (!dragCenterY.isNaN()) {
            dragCenterY += changeY
        }

        val draggingInfo = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == draggingItemId }
        if (draggingInfo != null && dragCenterY.isNaN()) {
            dragItemHeight = draggingInfo.size.toFloat()
            dragCenterY = draggingInfo.offset + draggingInfo.size / 2f + dragTranslationY
        } else if (draggingInfo != null) {
            dragItemHeight = draggingInfo.size.toFloat()
        }
        val activeCenterY = if (draggingInfo != null) {
            draggingInfo.offset + draggingInfo.size / 2f + dragTranslationY
        } else {
            dragCenterY
        }
        if (draggingInfo != null || !dragCenterY.isNaN()) {
            val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
            val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()
            val edgeThreshold = dragItemHeight * 0.9f
            val scrollDelta = when {
                activeCenterY < viewportStart + edgeThreshold -> {
                    -((viewportStart + edgeThreshold - activeCenterY) / 7f).coerceIn(6f, 34f)
                }
                activeCenterY > viewportEnd - edgeThreshold -> {
                    ((activeCenterY - (viewportEnd - edgeThreshold)) / 7f).coerceIn(6f, 34f)
                }
                else -> 0f
            }
            if (scrollDelta != 0f) {
                val consumed = listState.dispatchRawDelta(scrollDelta)
                dragTranslationY += consumed
                if (!dragCenterY.isNaN()) {
                    dragCenterY += consumed
                }
            }
        }

        while (true) {
            val from = localQueue.indexOfFirst { it.id == draggingItemId }
            if (from < 0) break
            val currentInfo = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == draggingItemId }
            val itemHeight = currentInfo?.size?.toFloat() ?: dragItemHeight
            val swapThreshold = itemHeight * 0.5f
            when {
                dragTranslationY >= swapThreshold && from < localQueue.lastIndex -> {
                    val to = from + 1
                    localQueue = localQueue.toMutableList().apply {
                        add(to, removeAt(from))
                    }
                    dragTranslationY -= itemHeight
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                dragTranslationY <= -swapThreshold && from > 0 -> {
                    val to = from - 1
                    localQueue = localQueue.toMutableList().apply {
                        add(to, removeAt(from))
                    }
                    dragTranslationY += itemHeight
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                else -> break
            }
        }
    }

    LaunchedEffect(queue, draggingItemId) {
        if (draggingItemId < 0L) {
            localQueue = reconcileQueueUiItems(
                current = localQueue,
                incoming = queue.distinctBy { it.path },
                nextId = { idSeed++ }
            )
        }
    }

    LaunchedEffect(localQueue) {
        val ids = localQueue.map { it.id }.toSet()
        dragHandleBounds.keys.toList().forEach { id ->
            if (id !in ids) {
                dragHandleBounds.remove(id)
            }
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
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "播放列表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${localQueue.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        dragSurfaceOffsetInRoot = it.localToRoot(Offset.Zero)
                    }
                    .pointerInput(canReorder) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { startDragAt(it) },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        ) { change, dragAmount ->
                            if (draggingItemId >= 0L) {
                                change.consume()
                                updateDrag(dragAmount.y)
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBlocker)
                        .padding(end = 12.dp)
                ) {
                    itemsIndexed(
                        items = localQueue,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val audio = item.audio
                        val isCurrent = audio.path == currentAudioPath
                        val isDragging = item.id == draggingItemId
                        val canDragItem = canReorder && (draggingItemId < 0L || isDragging)
                        var itemModifier = Modifier.fillMaxWidth()
                        if (!isDragging) {
                            itemModifier = itemModifier.animateItem(
                                fadeInSpec = tween(durationMillis = 110),
                                placementSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
                                fadeOutSpec = tween(durationMillis = 90)
                            )
                        }
                        val rowModifier = itemModifier
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = if (isDragging) dragTranslationY.roundToInt() else 0
                                )
                            }
                            .zIndex(if (isDragging) 1f else 0f)

                        Box(modifier = rowModifier) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                QueueRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    itemId = item.id,
                                    audio = audio,
                                    isCurrent = isCurrent,
                                    isDragging = isDragging,
                                    canReorder = canDragItem,
                                    onPlay = { onPlayAtIndex(index) },
                                    onRemove = { removeQueueItem(item.id) },
                                    onHandlePositioned = { id, bounds ->
                                        dragHandleBounds[id] = bounds
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
                            }
                        }
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
    itemId: Long,
    audio: AudioFile,
    isCurrent: Boolean,
    isDragging: Boolean,
    canReorder: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onHandlePositioned: (Long, Rect) -> Unit
) {
    ListItem(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay),
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
            Row(
                modifier = Modifier.offset(x = (-4).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = if (canReorder) "拖动排序" else "当前模式不可拖动排序",
                    tint = if (canReorder) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 2.dp)
                        .onGloballyPositioned { coordinates ->
                            onHandlePositioned(itemId, coordinates.boundsInRoot())
                        }
                )
                QueueSongLeadingCover(audio = audio, isCurrent = isCurrent)
            }
        },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                enabled = !isDragging,
                modifier = Modifier
                    .offset(x = 4.dp)
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "删除歌曲",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
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
    if (totalCount < 15) return
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
