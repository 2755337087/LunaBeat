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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.MusicPlaybackController
import com.example.LyricBox.blendColorForUi
import com.example.LyricBox.colorLuminance
import com.example.LyricBox.extractMutedCoverColor
import com.example.LyricBox.normalizeCoverThemeBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun GlobalMiniPlayerBar(
    controller: MusicPlaybackController,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var coverThemeColor by remember { mutableStateOf<Color?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val isDarkTheme = colorLuminance(MaterialTheme.colorScheme.background) < 0.5f
    val coverThemeCacheKey = remember(controller.currentCoverCachePath, controller.currentMediaId) {
        buildMiniPlayerColorCacheKey(controller.currentCoverCachePath, controller.currentMediaId)
    }

    LaunchedEffect(controller.currentCoverCachePath, controller.currentArtworkData) {
        coverBitmap = withContext(Dispatchers.IO) {
            decodeCoverBitmapFromCache(controller.currentCoverCachePath, reqWidth = 112, reqHeight = 112)
                ?: decodeCoverBitmapFromBytes(controller.currentArtworkData, reqWidth = 112, reqHeight = 112)
        }
    }
    LaunchedEffect(coverThemeCacheKey) {
        coverThemeColor = readMiniPlayerThemeColorCache(context, coverThemeCacheKey)
    }
    LaunchedEffect(coverBitmap, isDarkTheme, coverThemeCacheKey) {
        val computedColor = withContext(Dispatchers.IO) {
            coverBitmap?.let { extractMutedCoverColor(it, preferDark = isDarkTheme) }
        }
        if (computedColor != null) {
            coverThemeColor = computedColor
            writeMiniPlayerThemeColorCache(context, coverThemeCacheKey, computedColor)
        }
    }

    val fallbackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)
    val rawColor = coverThemeColor ?: fallbackColor
    val normalizedColor = normalizeCoverThemeBackground(rawColor, isDarkTheme)
    val targetPanelColor = blendColorForUi(
        normalizedColor,
        if (isDarkTheme) Color.Black else Color.White,
        if (isDarkTheme) 0.12f else 0.12f
    )
    val panelColor by animateColorAsState(
        targetValue = targetPanelColor,
        animationSpec = tween(durationMillis = 360),
        label = "globalMiniPlayerPanelColor"
    )
    val onPanelColor = if (colorLuminance(panelColor) > 0.52f) Color(0xFF151515) else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(panelColor)
            .pointerInput(controller.currentMediaId) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetY < -56f) {
                            onExpand()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffsetY += dragAmount.y
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
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = null,
                tint = onPanelColor,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
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
            modifier = Modifier.size(40.dp)
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
            modifier = Modifier.size(42.dp)
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
            modifier = Modifier.size(40.dp)
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

private const val MINI_PLAYER_THEME_PREFS = "MiniPlayerThemeCache"
private const val MINI_PLAYER_THEME_PREFIX = "cover_theme_"

private fun buildMiniPlayerColorCacheKey(coverCachePath: String?, mediaId: String?): String {
    return when {
        !coverCachePath.isNullOrBlank() -> "coverPath:$coverCachePath"
        !mediaId.isNullOrBlank() -> "mediaId:$mediaId"
        else -> "default"
    }
}

private fun readMiniPlayerThemeColorCache(context: Context, key: String): Color? {
    val prefs = context.getSharedPreferences(MINI_PLAYER_THEME_PREFS, Context.MODE_PRIVATE)
    if (!prefs.contains(MINI_PLAYER_THEME_PREFIX + key)) return null
    val argb = prefs.getInt(MINI_PLAYER_THEME_PREFIX + key, 0)
    return Color(argb)
}

private fun writeMiniPlayerThemeColorCache(context: Context, key: String, color: Color) {
    val prefs = context.getSharedPreferences(MINI_PLAYER_THEME_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putInt(MINI_PLAYER_THEME_PREFIX + key, color.toArgb()).apply()
}
