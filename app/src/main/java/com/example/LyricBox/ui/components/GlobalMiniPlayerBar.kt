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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.MusicPlaybackController
import com.example.LyricBox.MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR
import com.example.LyricBox.MINI_PLAYER_BACKGROUND_STYLE_COVER_COLOR
import com.example.LyricBox.MINI_PLAYER_BACKGROUND_STYLE_REALTIME_BLUR
import com.example.LyricBox.MINI_PLAYER_BACKGROUND_STYLE_SOLID
import com.example.LyricBox.buildCoverThemeCacheKey
import com.example.LyricBox.colorLuminance
import com.example.LyricBox.CoverThemeColorPair
import com.example.LyricBox.normalizeCoverThemeBackground
import com.example.LyricBox.resolveCachedCoverThemePair
import com.example.LyricBox.utils.AudioMetadataReader
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

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
    backgroundStyle: Int = MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurredPanelBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
    LaunchedEffect(coverBitmap, backgroundStyle) {
        blurredPanelBitmap = if (backgroundStyle == MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR) {
            withContext(Dispatchers.Default) {
                coverBitmap?.let { source ->
                    createMiniPlayerStaticBlurBackgroundBitmap(
                        source = source
                    )
                }
            }
        } else {
            null
        }
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

    val coverBasedPanelColor = run {
        val rawColor = coverThemeColor ?: MaterialTheme.colorScheme.background
        normalizeCoverThemeBackground(rawColor, isDarkTheme)
    }
    val targetPanelColor = when (backgroundStyle) {
        MINI_PLAYER_BACKGROUND_STYLE_SOLID,
        MINI_PLAYER_BACKGROUND_STYLE_COVER_COLOR,
        MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR -> coverBasedPanelColor
        else -> coverBasedPanelColor
    }
    val panelColor by animateColorAsState(
        targetValue = targetPanelColor,
        animationSpec = tween(durationMillis = 360),
        label = "globalMiniPlayerPanelColor"
    )
    val normalizedExpandProgress = expandProgress.coerceIn(0f, 1f)
    val coverScale = 1f + (1.9f - 1f) * normalizedExpandProgress
    val coverCornerRadius = lerp(12.dp, 4.dp, normalizedExpandProgress)
    val miniContentAlpha = (1f - normalizedExpandProgress).coerceIn(0f, 1f)
    val normalizedCoverAlpha = coverAlpha.coerceIn(0f, 1f)
    val blurredBackgroundImage = remember(blurredPanelBitmap) { blurredPanelBitmap?.asImageBitmap() }
    val shouldDrawBlurBackground = backgroundStyle == MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR
    val shouldDrawRealtimeBlurBackground = backgroundStyle == MINI_PLAYER_BACKGROUND_STYLE_REALTIME_BLUR
    val panelShape = RoundedCornerShape(18.dp)
    val onPanelColor = if (shouldDrawRealtimeBlurBackground) {
        if (isDarkTheme) Color.White else Color(0xFF151515)
    } else if (colorLuminance(panelColor) > 0.52f) {
        Color(0xFF151515)
    } else {
        Color.White
    }
    val blurredBackgroundScrim = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.56f)
    } else {
        Color.White.copy(alpha = 0.44f)
    }
    val realtimeBlurMaskColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.44f)
    } else {
        Color.White.copy(alpha = 0.46f)
    }
    val realtimeBlurDepthOverlay = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.05f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.04f)
            )
        )
    }
    val blurredBackgroundDepthOverlay = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0.18f),
                panelColor.copy(alpha = 0.22f),
                Color.Black.copy(alpha = 0.24f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                panelColor.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.26f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .then(
                if (shouldDrawRealtimeBlurBackground && backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { panelShape },
                        effects = {
                            blur(56f.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isDarkTheme) 0.16f else 0.26f)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = if (isDarkTheme) 0.30f else 0.12f)
                            )
                        },
                        onDrawSurface = {
                            drawRect(realtimeBlurMaskColor)
                            drawRect(brush = realtimeBlurDepthOverlay)
                        }
                    )
                } else if (shouldDrawRealtimeBlurBackground) {
                    Modifier.background(realtimeBlurMaskColor, panelShape)
                } else {
                    Modifier.background(panelColor, panelShape)
                }
            )
            .drawWithContent {
                val backgroundImage = blurredBackgroundImage
                if (shouldDrawBlurBackground && backgroundImage != null && size.width > 1f && size.height > 1f) {
                    val dstWidth = size.width.roundToInt().coerceAtLeast(1)
                    val dstHeight = size.height.roundToInt().coerceAtLeast(1)
                    val srcWidth = backgroundImage.width
                    val srcHeight = backgroundImage.height.coerceAtLeast(1)
                    val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()
                    val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
                    val cropWidth: Int
                    val cropHeight: Int
                    if (srcAspect > dstAspect) {
                        cropHeight = srcHeight
                        cropWidth = (cropHeight * dstAspect).roundToInt().coerceIn(1, srcWidth)
                    } else {
                        cropWidth = srcWidth
                        cropHeight = (cropWidth / dstAspect).roundToInt().coerceIn(1, srcHeight)
                    }
                    val cropX = ((srcWidth - cropWidth) / 2).coerceAtLeast(0)
                    val cropY = ((srcHeight - cropHeight) / 2).coerceAtLeast(0)
                    drawImage(
                        image = backgroundImage,
                        srcOffset = IntOffset(cropX, cropY),
                        srcSize = IntSize(cropWidth, cropHeight),
                        dstSize = IntSize(
                            dstWidth,
                            dstHeight
                        ),
                        alpha = 0.86f
                    )
                    drawRect(blurredBackgroundScrim)
                    drawRect(brush = blurredBackgroundDepthOverlay)
                }
                drawContent()
            }
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                AutoMarqueeText(
                    text = controller.currentTitle.ifBlank { "未选择歌曲" },
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onPanelColor
                    ),
                    modifier = Modifier.fillMaxWidth()
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
}

private fun createMiniPlayerStaticBlurBackgroundBitmap(
    source: Bitmap
): Bitmap {
    val minWidth = 40
    val maxWidth = 80
    val desiredWidth = source.width.coerceIn(minWidth, maxWidth)
    val scale = desiredWidth.toFloat() / source.width.coerceAtLeast(1).toFloat()
    val targetWidth = desiredWidth.coerceAtLeast(1)
    val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    return blurMiniPlayerBitmapFast(scaled, radius = 12)
}

private fun blurMiniPlayerBitmapFast(source: Bitmap, radius: Int): Bitmap {
    if (radius <= 0) return source
    val safeRadius = radius.coerceIn(1, 25)
    val width = source.width
    val height = source.height
    if (width <= 1 || height <= 1) return source

    val input = IntArray(width * height)
    val horizontal = IntArray(width * height)
    val output = IntArray(width * height)
    source.getPixels(input, 0, width, 0, 0, width, height)

    val windowSize = safeRadius * 2 + 1
    for (y in 0 until height) {
        val rowOffset = y * width
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -safeRadius..safeRadius) {
            val px = input[rowOffset + i.coerceIn(0, width - 1)]
            sumA += (px ushr 24) and 0xFF
            sumR += (px ushr 16) and 0xFF
            sumG += (px ushr 8) and 0xFF
            sumB += px and 0xFF
        }
        for (x in 0 until width) {
            horizontal[rowOffset + x] =
                ((sumA / windowSize) shl 24) or
                    ((sumR / windowSize) shl 16) or
                    ((sumG / windowSize) shl 8) or
                    (sumB / windowSize)
            val removeX = (x - safeRadius).coerceIn(0, width - 1)
            val addX = (x + safeRadius + 1).coerceIn(0, width - 1)
            val removePx = input[rowOffset + removeX]
            val addPx = input[rowOffset + addX]
            sumA += ((addPx ushr 24) and 0xFF) - ((removePx ushr 24) and 0xFF)
            sumR += ((addPx ushr 16) and 0xFF) - ((removePx ushr 16) and 0xFF)
            sumG += ((addPx ushr 8) and 0xFF) - ((removePx ushr 8) and 0xFF)
            sumB += (addPx and 0xFF) - (removePx and 0xFF)
        }
    }

    for (x in 0 until width) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -safeRadius..safeRadius) {
            val y = i.coerceIn(0, height - 1)
            val px = horizontal[y * width + x]
            sumA += (px ushr 24) and 0xFF
            sumR += (px ushr 16) and 0xFF
            sumG += (px ushr 8) and 0xFF
            sumB += px and 0xFF
        }
        for (y in 0 until height) {
            output[y * width + x] =
                ((sumA / windowSize) shl 24) or
                    ((sumR / windowSize) shl 16) or
                    ((sumG / windowSize) shl 8) or
                    (sumB / windowSize)
            val removeY = (y - safeRadius).coerceIn(0, height - 1)
            val addY = (y + safeRadius + 1).coerceIn(0, height - 1)
            val removePx = horizontal[removeY * width + x]
            val addPx = horizontal[addY * width + x]
            sumA += ((addPx ushr 24) and 0xFF) - ((removePx ushr 24) and 0xFF)
            sumR += ((addPx ushr 16) and 0xFF) - ((removePx ushr 16) and 0xFF)
            sumG += ((addPx ushr 8) and 0xFF) - ((removePx ushr 8) and 0xFF)
            sumB += (addPx and 0xFF) - (removePx and 0xFF)
        }
    }

    val blurred = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    blurred.setPixels(output, 0, width, 0, 0, width, height)
    return blurred
}

private fun decodeCoverBitmapFromCache(cachePath: String?, reqWidth: Int, reqHeight: Int): Bitmap? {
    if (cachePath.isNullOrBlank()) return null
    val cacheFile = File(cachePath)
    if (!cacheFile.exists() || !cacheFile.isFile || cacheFile.length() <= 0L) return null

    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(cacheFile.absolutePath, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null

    options.inJustDecodeBounds = false
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    return BitmapFactory.decodeFile(cacheFile.absolutePath, options)
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
    val fromCache = decodeCoverBitmapFromCache(
        coverCachePath,
        reqWidth = MINI_PLAYER_COVER_REQUEST_SIZE_PX,
        reqHeight = MINI_PLAYER_COVER_REQUEST_SIZE_PX
    )
    if (fromCache != null) return fromCache

    val fromArtworkData = decodeCoverBitmapFromBytes(
        artworkData,
        reqWidth = MINI_PLAYER_COVER_REQUEST_SIZE_PX,
        reqHeight = MINI_PLAYER_COVER_REQUEST_SIZE_PX
    )
    if (fromArtworkData != null) return fromArtworkData

    val sourceCoverBytes = runCatching {
        val sourcePath = audioPath?.takeIf { it.isNotBlank() } ?: return@runCatching null
        AudioMetadataReader.readMetadata(
            context = context,
            filePath = sourcePath,
            mediaStoreId = mediaStoreId,
            includeCover = true
        ).cover
    }.getOrNull()
    return decodeCoverBitmapFromBytes(
        sourceCoverBytes,
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
