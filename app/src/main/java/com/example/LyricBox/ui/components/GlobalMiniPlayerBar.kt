package com.example.LyricBox.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.MusicPlaybackController
import com.example.LyricBox.buildCoverThemeCacheKey
import com.example.LyricBox.colorLuminance
import com.example.LyricBox.CoverThemeColorPair
import com.example.LyricBox.normalizeCoverThemeBackground
import com.example.LyricBox.resolveCachedCoverThemePair
import com.example.LyricBox.utils.AudioMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun GlobalMiniPlayerBar(
    controller: MusicPlaybackController,
    onExpand: () -> Unit,
    onExpandTouchDown: (() -> Unit)? = null,
    expandProgress: Float = 0f,
    onExpandDragDelta: ((Float) -> Unit)? = null,
    onExpandDragEnd: (() -> Unit)? = null,
    onExpandDragCancel: (() -> Unit)? = null,
    sharedCoverId: String = "music_cover",
    coverAlpha: Float = 1f,
    onBarBoundsChanged: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    onCoverBoundsChanged: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    onCoverBitmapChanged: ((Bitmap?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var coverBitmapKey by remember { mutableStateOf<String?>(null) }
    var coverThemePair by remember { mutableStateOf<CoverThemeColorPair?>(null) }
    var displayedCoverThemePair by remember { mutableStateOf<CoverThemeColorPair?>(null) }
    var coverLoadRequestVersion by remember { mutableStateOf(0) }
    var themeResolveRequestVersion by remember { mutableStateOf(0) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val isDarkTheme = colorLuminance(MaterialTheme.colorScheme.background) < 0.5f
    val coverThemeCacheKey = remember(
        controller.currentMediaStoreId,
        controller.currentAudioPath
    ) {
        buildCoverThemeCacheKey(
            coverCachePath = null,
            mediaId = controller.currentMediaStoreId
                .takeIf { it > 0L }
                ?.toString(),
            audioPath = controller.currentAudioPath
        )
    }

    LaunchedEffect(
        controller.currentMediaId,
        controller.currentAudioPath,
        controller.currentMediaStoreId,
        controller.currentArtworkData,
        controller.currentCoverCachePath
    ) {
        val requestVersion = coverLoadRequestVersion + 1
        coverLoadRequestVersion = requestVersion
        val requestKey = coverThemeCacheKey
        val requestAudioPath = controller.currentAudioPath
        val requestMediaStoreId = controller.currentMediaStoreId
        val requestArtworkData = controller.currentArtworkData
        val requestCoverCachePath = controller.currentCoverCachePath
        coverBitmapKey = null
        val loadedCover = withContext(Dispatchers.IO) {
            loadMiniPlayerCoverBitmap(
                context = context,
                audioPath = requestAudioPath,
                mediaStoreId = requestMediaStoreId,
                artworkData = requestArtworkData,
                coverCachePath = requestCoverCachePath
            )
        }
        if (coverLoadRequestVersion != requestVersion) return@LaunchedEffect
        coverBitmap = loadedCover
        coverBitmapKey = requestKey
    }
    LaunchedEffect(coverBitmap) {
        onCoverBitmapChanged?.invoke(coverBitmap)
    }
    LaunchedEffect(coverThemeCacheKey, coverBitmap) {
        if (coverBitmapKey != coverThemeCacheKey || coverBitmap == null) {
            return@LaunchedEffect
        }
        val requestVersion = themeResolveRequestVersion + 1
        themeResolveRequestVersion = requestVersion
        val resolvedPair = withContext(Dispatchers.IO) {
            resolveCachedCoverThemePair(
                context = context,
                cacheKey = coverThemeCacheKey,
                cover = coverBitmap
            )
        }
        if (themeResolveRequestVersion != requestVersion) return@LaunchedEffect
        coverThemePair = resolvedPair
        if (resolvedPair != null) {
            displayedCoverThemePair = resolvedPair
        } else if (displayedCoverThemePair == null) {
            displayedCoverThemePair = null
        }
    }
    val coverThemeColor = if (isDarkTheme) displayedCoverThemePair?.darkColor else displayedCoverThemePair?.lightColor

    val rawColor = coverThemeColor ?: MaterialTheme.colorScheme.background
    val targetPanelColor = normalizeCoverThemeBackground(rawColor, isDarkTheme)
    val panelColor by animateColorAsState(
        targetValue = targetPanelColor,
        animationSpec = tween(durationMillis = 360),
        label = "globalMiniPlayerPanelColor"
    )
    val onPanelColor = if (colorLuminance(panelColor) > 0.52f) Color(0xFF151515) else Color.White
    val normalizedExpandProgress = expandProgress.coerceIn(0f, 1f)
    val coverScale = 1f + (1.9f - 1f) * normalizedExpandProgress
    val coverCornerRadius = lerp(12.dp, 4.dp, normalizedExpandProgress)
    val miniContentAlpha = (1f - normalizedExpandProgress).coerceIn(0f, 1f)
    val normalizedCoverAlpha = coverAlpha.coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(panelColor)
            .onGloballyPositioned { coordinates ->
                onBarBoundsChanged?.invoke(coordinates.boundsInRoot())
            }
            .pointerInput(controller.currentMediaId) {
                detectTapGestures(
                    onPress = {
                        onExpandTouchDown?.invoke()
                        tryAwaitRelease()
                    }
                )
            }
            .pointerInput(controller.currentMediaId) {
                detectDragGestures(
                    onDragEnd = {
                        val handledByParent = onExpandDragEnd != null
                        if (handledByParent) {
                            onExpandDragEnd?.invoke()
                        } else if (dragOffsetY < -56f) {
                            onExpand()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        onExpandDragCancel?.invoke()
                        dragOffsetY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffsetY += dragAmount.y
                    onExpandDragDelta?.invoke(dragAmount.y)
                }
            }
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap!!.asImageBitmap(),
                contentDescription = "当前歌曲封面",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(coverCornerRadius))
                    .testTag(sharedCoverId)
                    .onGloballyPositioned { coordinates ->
                        onCoverBoundsChanged?.invoke(coordinates.boundsInRoot())
                    }
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                        alpha = normalizedCoverAlpha
                    }
            )
        } else {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = null,
                tint = onPanelColor,
                modifier = Modifier
                    .size(44.dp)
                    .testTag(sharedCoverId)
                    .onGloballyPositioned { coordinates ->
                        onCoverBoundsChanged?.invoke(coordinates.boundsInRoot())
                    }
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                        alpha = normalizedCoverAlpha
                    }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(alpha = miniContentAlpha),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = controller.currentTitle.ifBlank { "未选择歌曲" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = onPanelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = controller.currentArtist.ifBlank { "未知艺术家" },
                fontSize = 12.sp,
                color = onPanelColor.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { controller.skipToPrevious() },
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer(alpha = miniContentAlpha)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "上一首",
                tint = onPanelColor,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(
            onClick = { controller.togglePlayPause() },
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer(alpha = miniContentAlpha)
        ) {
            Icon(
                imageVector = if (controller.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (controller.isPlaying) "暂停" else "播放",
                tint = onPanelColor,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(
            onClick = { controller.skipToNext() },
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer(alpha = miniContentAlpha)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "下一首",
                tint = onPanelColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun decodeCoverBitmapFromCache(cachePath: String?, reqWidth: Int, reqHeight: Int): Bitmap? {
    if (cachePath.isNullOrBlank()) return null
    val cacheFile = File(cachePath)
    if (!cacheFile.exists()) return null
    val bytes = cacheFile.readBytes()
    if (bytes.isEmpty()) return null

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null

    options.inJustDecodeBounds = false
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun decodeCoverBitmapFromBytes(bytes: ByteArray?, reqWidth: Int, reqHeight: Int): Bitmap? {
    val rawBytes = bytes ?: return null
    if (rawBytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    options.inJustDecodeBounds = false
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
}

private const val MINI_PLAYER_COVER_REQUEST_SIZE_PX = 1024

private fun loadMiniPlayerCoverBitmap(
    context: Context,
    audioPath: String?,
    mediaStoreId: Long,
    artworkData: ByteArray?,
    coverCachePath: String?
): Bitmap? {
    val sourceCoverBytes = runCatching {
        val sourcePath = audioPath?.takeIf { it.isNotBlank() } ?: return@runCatching null
        AudioMetadataReader.readMetadata(
            context = context,
            filePath = sourcePath,
            mediaStoreId = mediaStoreId,
            includeCover = true
        ).cover
    }.getOrNull()

    val fromSourceCover = decodeCoverBitmapFromBytes(
        sourceCoverBytes,
        reqWidth = MINI_PLAYER_COVER_REQUEST_SIZE_PX,
        reqHeight = MINI_PLAYER_COVER_REQUEST_SIZE_PX
    )
    if (fromSourceCover != null) return fromSourceCover

    val fromCache = decodeCoverBitmapFromCache(
        coverCachePath,
        reqWidth = MINI_PLAYER_COVER_REQUEST_SIZE_PX,
        reqHeight = MINI_PLAYER_COVER_REQUEST_SIZE_PX
    )
    if (fromCache != null) return fromCache

    return decodeCoverBitmapFromBytes(
        artworkData,
        reqWidth = MINI_PLAYER_COVER_REQUEST_SIZE_PX,
        reqHeight = MINI_PLAYER_COVER_REQUEST_SIZE_PX
    )
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}
