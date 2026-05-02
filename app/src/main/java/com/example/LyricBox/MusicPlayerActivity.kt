package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayerActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MusicPlayerActivity::class.java))
            if (context is Activity) {
                context.overridePendingTransition(
                    R.anim.player_slide_up_in,
                    R.anim.player_hold
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            歌词转换Theme {
                MusicPlayerScreen(
                    onClose = { finish() }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(
            R.anim.player_hold,
            R.anim.player_slide_down_out
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MusicPlayerScreen(
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = rememberMusicPlaybackController()
    var hasAppeared by remember { mutableStateOf(false) }
    var coverThemeColor by remember { mutableStateOf<Color?>(null) }

    val lyricPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val usedSharedPlayback = result.data?.getBooleanExtra(LyricPreviewActivity.EXTRA_SHARED_PLAYBACK_USED, false) == true
        val returnedPos = result.data?.getLongExtra(LyricPreviewActivity.EXTRA_RETURN_POSITION, -1L) ?: -1L
        if (!usedSharedPlayback && returnedPos >= 0L) {
            controller.seekTo(returnedPos)
        }
    }

    LaunchedEffect(Unit) { hasAppeared = true }

    val rawCoverBitmap = remember(controller.currentArtworkData) {
        val artwork = controller.currentArtworkData
        if (artwork != null) BitmapFactory.decodeByteArray(artwork, 0, artwork.size) else null
    }
    var displayCoverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var displayThemeColor by remember { mutableStateOf<Color?>(null) }
    val isDarkTheme = colorLuminance(MaterialTheme.colorScheme.background) < 0.5f

    LaunchedEffect(rawCoverBitmap, isDarkTheme) {
        if (rawCoverBitmap == null) return@LaunchedEffect
        val pendingColor = withContext(Dispatchers.IO) {
            extractMutedCoverColor(rawCoverBitmap, preferDark = isDarkTheme)
        }
        // 封面与主题色同时切换，避免“先糊图后原图”的突兀感。
        displayCoverBitmap = rawCoverBitmap
        displayThemeColor = pendingColor
    }

    LaunchedEffect(displayCoverBitmap, isDarkTheme) {
        coverThemeColor = withContext(Dispatchers.IO) {
            displayCoverBitmap?.let { extractMutedCoverColor(it, preferDark = isDarkTheme) }
        }
    }

    val targetBackgroundColorRaw = displayThemeColor ?: coverThemeColor ?: MaterialTheme.colorScheme.surface
    val targetBackgroundColor = normalizeCoverThemeBackground(targetBackgroundColorRaw, isDarkTheme)
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 420),
        label = "playerBgColor"
    )
    val onBackgroundColor = if (colorLuminance(backgroundColor) > 0.52f) Color(0xFF111111) else Color.White
    val panelColor = blendColorForUi(backgroundColor, if (colorLuminance(backgroundColor) > 0.52f) Color.White else Color.Black, 0.20f)
    val panelOnColor = if (colorLuminance(panelColor) > 0.52f) Color(0xFF111111) else Color.White
    val accentColor = if (panelOnColor == Color.White) {
        blendColorForUi(backgroundColor, Color.White, 0.42f)
    } else {
        blendColorForUi(backgroundColor, Color.Black, 0.42f)
    }
    val onAccentColor = if (colorLuminance(accentColor) > 0.52f) Color(0xFF101010) else Color.White
    val controlAccentBase = displayThemeColor ?: coverThemeColor ?: accentColor
    val controlAccentColor = if (isDarkTheme) {
        blendColorForUi(controlAccentBase, Color.White, 0.7f)
    } else {
        blendColorForUi(controlAccentBase, Color.Black, 0.7f)
    }
    val onControlAccentColor = if (colorLuminance(controlAccentColor) > 0.52f) Color(0xFF101010) else Color.White
    val oppositeControlColor = if (isDarkTheme) {
        blendColorForUi(controlAccentBase, Color.Black, 0.7f)
    } else {
        blendColorForUi(controlAccentBase, Color.White, 0.7f)
    }
    val onOppositeControlColor = if (colorLuminance(oppositeControlColor) > 0.52f) Color(0xFF101010) else Color.White
    val gradientTop = if (isDarkTheme) {
        blendColorForUi(backgroundColor, Color.Black, 0.18f)
    } else {
        blendColorForUi(backgroundColor, Color.White, 0.10f)
    }
    val gradientBottom = if (isDarkTheme) {
        blendColorForUi(backgroundColor, Color.Black, 0.48f)
    } else {
        blendColorForUi(backgroundColor, MaterialTheme.colorScheme.surface, 0.55f)
    }

    val position = controller.positionMs.coerceAtLeast(0L)
    val duration = controller.durationMs.coerceAtLeast(0L)
    val coverScale by animateFloatAsState(
        targetValue = if (controller.playWhenReadyRequested) 1f else 0.95f,
        animationSpec = tween(durationMillis = 320),
        label = "coverPlayPauseScale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom)
                )
            )
    ) {
        AnimatedVisibility(
            visible = hasAppeared,
            enter = fadeIn(animationSpec = tween(260)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = "收起播放器",
                            tint = onBackgroundColor
                        )
                    }
                    Text(
                        text = "正在播放",
                        fontSize = 14.sp,
                        color = onBackgroundColor.copy(alpha = 0.88f)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayCoverBitmap != null) {
                        Crossfade(
                            targetState = displayCoverBitmap,
                            animationSpec = tween(durationMillis = 360),
                            label = "coverCrossfade"
                        ) { cover ->
                            if (cover != null) {
                                Image(
                                    bitmap = cover.asImageBitmap(),
                                    contentDescription = "封面",
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .widthIn(max = 520.dp)
                                        .aspectRatio(1f)
                                        .graphicsLayer(
                                            scaleX = coverScale,
                                            scaleY = coverScale
                                        )
                                        .clip(RoundedCornerShape(26.dp))
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .widthIn(max = 520.dp)
                                .aspectRatio(1f)
                                .graphicsLayer(
                                    scaleX = coverScale,
                                    scaleY = coverScale
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_media_play),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(108.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(panelColor)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = controller.currentTitle.ifBlank { "未选择歌曲" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = panelOnColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = controller.currentArtist.ifBlank { "未知艺术家" },
                        fontSize = 15.sp,
                        color = panelOnColor.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val minSeekStartMs = 240L
                    val safeDuration = duration.coerceAtLeast(0L)
                    val seekStart = if (safeDuration > minSeekStartMs) minSeekStartMs else 0L
                    val seekSpan = (safeDuration - seekStart).coerceAtLeast(0L)
                    val clampedCurrentTime = position.coerceIn(0L, safeDuration)
                    val seekSliderProgress = if (seekSpan > 0L) {
                        ((clampedCurrentTime - seekStart).toFloat() / seekSpan.toFloat())
                            .takeIf { it.isFinite() }
                            ?.coerceIn(0f, 1f)
                            ?: 0f
                    } else {
                        0f
                    }

                    val progressColor = controlAccentColor
                    val progressTrackColor = controlAccentColor.copy(alpha = 0.24f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(vertical = 6.dp)
                    ) {
                        val displayProgress = if (safeDuration > 0L) {
                            (clampedCurrentTime.toFloat() / safeDuration.toFloat())
                                .takeIf { it.isFinite() }
                                ?.coerceIn(0f, 1f)
                                ?: 0f
                        } else {
                            0f
                        }
                        LinearProgressIndicator(
                            progress = { displayProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = progressColor,
                            trackColor = progressTrackColor
                        )

                        Slider(
                            value = seekSliderProgress,
                            onValueChange = { newProgress ->
                                val safeProgress = newProgress
                                    .takeIf { it.isFinite() }
                                    ?.coerceIn(0f, 1f)
                                    ?: 0f
                                val newPosition = if (seekSpan > 0L) {
                                    seekStart + (safeProgress * seekSpan.toFloat()).toLong()
                                } else {
                                    seekStart
                                }
                                controller.seekTo(newPosition.coerceIn(0L, safeDuration))
                            },
                            modifier = Modifier.fillMaxSize(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Transparent,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatPlaybackTime(position),
                            color = panelOnColor.copy(alpha = 0.74f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatPlaybackTime(safeDuration),
                            color = panelOnColor.copy(alpha = 0.74f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val toggleColors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = oppositeControlColor,
                        contentColor = onOppositeControlColor,
                        checkedContainerColor = oppositeControlColor,
                        checkedContentColor = onOppositeControlColor
                    )
        ButtonGroup(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
                        ToggleButton(
                            checked = false,
                            onCheckedChange = { controller.skipToPrevious() },
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            colors = toggleColors
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "上一首"
                            )
                        }
                        ToggleButton(
                            checked = controller.isPlaying,
                            onCheckedChange = { controller.togglePlayPause() },
                            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxSize(),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = controlAccentColor,
                                contentColor = onControlAccentColor,
                                checkedContainerColor = controlAccentColor,
                                checkedContentColor = onControlAccentColor
                            )
                        ) {
                            Icon(
                                imageVector = if (controller.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (controller.isPlaying) "暂停" else "播放"
                            )
                        }
                        ToggleButton(
                            checked = false,
                            onCheckedChange = { controller.skipToNext() },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            colors = toggleColors
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "下一首"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .clickable {
                                val currentPath = controller.currentAudioPath
                                if (currentPath.isNullOrBlank()) return@clickable

                                val previewIntent = LyricPreviewActivity.createIntent(
                                    context = context,
                                    audioPath = currentPath,
                                    lyricLines = emptyList(),
                                    title = controller.currentTitle.ifBlank { "歌词预览" },
                                    initialPosition = position,
                                    creators = emptyList(),
                                    sourceAudioPath = currentPath,
                                    useSharedPlayback = true
                                )
                                lyricPreviewLauncher.launch(previewIntent)
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "歌词",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
