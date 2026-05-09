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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    var coverThemeColor by remember { mutableStateOf<Color?>(null) }
    var showArtistSheet by remember { mutableStateOf(false) }
    var pendingArtists by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var selectedSongInfoAudio by remember { mutableStateOf<AudioFile?>(null) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    val lyricPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val usedSharedPlayback = result.data?.getBooleanExtra(LyricPreviewActivity.EXTRA_SHARED_PLAYBACK_USED, false) == true
        val returnedPos = result.data?.getLongExtra(LyricPreviewActivity.EXTRA_RETURN_POSITION, -1L) ?: -1L
        if (!usedSharedPlayback && returnedPos >= 0L) {
            controller.seekTo(returnedPos)
        }
    }

    val cachedQueueEntries = remember(controller.queueAudioPaths) {
        LocalPlaylistStore.loadPlaybackQueueEntries(context)
    }
    val cachedEntryByPath = remember(cachedQueueEntries) {
        cachedQueueEntries
            .groupBy { it.path }
            .mapValues { (_, entries) -> entries.first() }
    }

    val currentAudio = remember(
        controller.currentAudioPath,
        controller.currentTitle,
        controller.currentArtist,
        controller.currentCoverCachePath,
        controller.durationMs
    ) {
        controller.currentAudioPath?.let { path ->
            buildAudioFileForPlayer(
                path = path,
                titleHint = controller.currentTitle,
                artistHint = controller.currentArtist,
                coverCachePath = controller.currentCoverCachePath
                    ?: resolveCoverCachePathForAudio(context, path),
                durationHint = controller.durationMs
            )
        }
    }

    val playbackQueueAudios = remember(
        controller.queueAudioPaths,
        cachedEntryByPath,
        controller.currentAudioPath,
        controller.currentTitle,
        controller.currentArtist,
        controller.currentCoverCachePath,
        controller.durationMs
    ) {
        controller.queueAudioPaths.map { path ->
            if (path == controller.currentAudioPath && currentAudio != null) {
                currentAudio
            } else {
                val cachedEntry = cachedEntryByPath[path]
                buildAudioFileForPlayer(
                    path = path,
                    titleHint = cachedEntry?.title.orEmpty(),
                    artistHint = cachedEntry?.artist.orEmpty(),
                    coverCachePath = resolveCoverCachePathForAudio(context, path)
                )
            }
        }
    }

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
    val navigationBottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp
    val openLyricPreview: () -> Unit = {
        val currentPath = controller.currentAudioPath
        if (!currentPath.isNullOrBlank()) {
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom)
                )
            )
    ) {
        MusicPlayerPrimaryPane(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = navigationBottomPadding),
            onClose = onClose,
            controller = controller,
            currentAudio = currentAudio,
            displayCoverBitmap = displayCoverBitmap,
            coverScale = coverScale,
            position = position,
            duration = duration,
            onBackgroundColor = onBackgroundColor,
            panelColor = panelColor,
            panelOnColor = panelOnColor,
            accentColor = accentColor,
            controlAccentColor = controlAccentColor,
            onControlAccentColor = onControlAccentColor,
            oppositeControlColor = oppositeControlColor,
            onOppositeControlColor = onOppositeControlColor,
            playbackMode = controller.playbackMode,
            nextTrackTitle = controller.nextTrackTitle,
            showLyricPreviewButton = true,
            onLyricPreviewClick = openLyricPreview,
            onShowPlaylist = { showPlaylistSheet = true },
            onSongInfoClick = {
                selectedSongInfoAudio = currentAudio
                showSongInfoSheet = currentAudio != null
            },
            onArtistsClick = { artists ->
                if (artists.isNotEmpty()) {
                    pendingArtists = artists
                    showArtistSheet = true
                }
            }
        )
    }

    if (showPlaylistSheet) {
        NowPlayingPlaylistBottomSheet(
            queue = playbackQueueAudios,
            currentAudioPath = controller.currentAudioPath,
            onDismiss = { showPlaylistSheet = false },
            onMoveItem = { fromIndex, toIndex ->
                controller.moveQueueItem(fromIndex, toIndex)
            },
            onPlayAtIndex = { index ->
                controller.playAtQueueIndex(index)
            },
            onRemoveAtIndex = { index ->
                controller.removeQueueItemAt(index)
            }
        )
    }

    if (showSongInfoSheet && selectedSongInfoAudio != null) {
        val infoAudio = selectedSongInfoAudio!!
        SongInfoBottomSheet(
            audio = infoAudio,
            isFavorite = infoAudio.path in LocalPlaylistStore.loadFavoritePaths(context),
            renameSuccessSignal = 0L,
            onDismiss = {
                showSongInfoSheet = false
                selectedSongInfoAudio = null
            },
            onPlayNext = {
                controller.insertNext(infoAudio)
            },
            onViewArtists = { artists ->
                if (artists.isNotEmpty()) {
                    pendingArtists = artists
                    showArtistSheet = true
                }
            }
        )
    }

    if (showArtistSheet) {
        ArtistSelectionBottomSheet(
            artists = pendingArtists,
            onDismiss = { showArtistSheet = false },
            onSelectArtist = { artist ->
                showArtistSheet = false
                val intent = Intent(context, MusicLibraryActivity::class.java).apply {
                    putExtra(MusicLibraryActivity.EXTRA_INITIAL_SEARCH_QUERY, artist)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MusicPlayerPrimaryPane(
    modifier: Modifier,
    onClose: () -> Unit,
    controller: MusicPlaybackController,
    currentAudio: AudioFile?,
    displayCoverBitmap: android.graphics.Bitmap?,
    coverScale: Float,
    position: Long,
    duration: Long,
    onBackgroundColor: Color,
    panelColor: Color,
    panelOnColor: Color,
    accentColor: Color,
    controlAccentColor: Color,
    onControlAccentColor: Color,
    oppositeControlColor: Color,
    onOppositeControlColor: Color,
    playbackMode: PlaybackMode,
    nextTrackTitle: String,
    showLyricPreviewButton: Boolean,
    onLyricPreviewClick: () -> Unit,
    onShowPlaylist: () -> Unit,
    onSongInfoClick: () -> Unit,
    onArtistsClick: (List<String>) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var showNextTrackHint by remember(nextTrackTitle) { mutableStateOf(false) }
    LaunchedEffect(nextTrackTitle, playbackMode) {
        showNextTrackHint = false
        if (nextTrackTitle.isBlank()) return@LaunchedEffect
        while (true) {
            delay(5000)
            showNextTrackHint = true
            delay(5000)
            showNextTrackHint = false
        }
    }
    val topTitleText = if (showNextTrackHint && nextTrackTitle.isNotBlank()) {
        "下一首：$nextTrackTitle"
    } else {
        "正在播放"
    }

    if (isLandscape) {
        BoxWithConstraints(modifier = modifier) {
            val landscapePanelHeight = (maxHeight * 0.92f)
                .coerceAtMost(520.dp)
                .coerceAtLeast(220.dp)
            val coverHostHeight = (landscapePanelHeight * 0.92f)
                .coerceAtMost(460.dp)
                .coerceAtLeast(180.dp)

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusicPlayerCoverView(
                    modifier = Modifier
                        .weight(0.95f)
                        .height(coverHostHeight),
                    displayCoverBitmap = displayCoverBitmap,
                    coverScale = coverScale,
                    contentScaleFactor = 1f,
                    applyPreviewLandscapeBounds = true
                )
                MusicPlayerControlPanel(
                    modifier = Modifier
                        .weight(1.05f)
                        .height(coverHostHeight),
                    isLandscape = true,
                    controller = controller,
                    currentAudio = currentAudio,
                    position = position,
                    duration = duration,
                    panelColor = panelColor,
                    panelOnColor = panelOnColor,
                    accentColor = accentColor,
                    controlAccentColor = controlAccentColor,
                    onControlAccentColor = onControlAccentColor,
                    oppositeControlColor = oppositeControlColor,
                    onOppositeControlColor = onOppositeControlColor,
                    showLyricPreviewButton = showLyricPreviewButton,
                    onLyricPreviewClick = onLyricPreviewClick,
                    onShowPlaylist = onShowPlaylist,
                    onSongInfoClick = onSongInfoClick,
                    onArtistsClick = onArtistsClick
                )
            }
        }
    } else {
        Column(modifier = modifier) {
            val headerSideSize = 40.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.size(headerSideSize),
                    onClick = onClose
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.down),
                        contentDescription = "收起播放器",
                        tint = onBackgroundColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Crossfade(
                    targetState = topTitleText,
                    animationSpec = tween(durationMillis = 260),
                    modifier = Modifier.weight(1f),
                    label = "playerTopTitle"
                ) { text ->
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        color = onBackgroundColor.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(headerSideSize))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                MusicPlayerCoverView(
                    modifier = Modifier.fillMaxSize(),
                    displayCoverBitmap = displayCoverBitmap,
                    coverScale = coverScale
                )
            }

            MusicPlayerControlPanel(
                modifier = Modifier.fillMaxWidth(),
                isLandscape = false,
                controller = controller,
                currentAudio = currentAudio,
                position = position,
                duration = duration,
                panelColor = panelColor,
                panelOnColor = panelOnColor,
                accentColor = accentColor,
                controlAccentColor = controlAccentColor,
                onControlAccentColor = onControlAccentColor,
                oppositeControlColor = oppositeControlColor,
                onOppositeControlColor = onOppositeControlColor,
                showLyricPreviewButton = showLyricPreviewButton,
                onLyricPreviewClick = onLyricPreviewClick,
                onShowPlaylist = onShowPlaylist,
                onSongInfoClick = onSongInfoClick,
                onArtistsClick = onArtistsClick
            )
        }
    }
}

@Composable
private fun MusicPlayerCoverView(
    modifier: Modifier,
    displayCoverBitmap: android.graphics.Bitmap?,
    coverScale: Float,
    contentScaleFactor: Float = 0.92f,
    applyPreviewLandscapeBounds: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val screenConfig = LocalConfiguration.current
        val safeScaleFactor = contentScaleFactor.coerceIn(0.6f, 1f)
        val targetWidth = maxWidth * safeScaleFactor
        val targetHeight = maxHeight * safeScaleFactor
        val coverAspectRatio = remember(displayCoverBitmap) {
            displayCoverBitmap?.let { bitmap ->
                if (bitmap.height > 0) {
                    (bitmap.width.toFloat() / bitmap.height.toFloat())
                        .takeIf { it.isFinite() && it > 0f }
                } else {
                    null
                }
            } ?: 1f
        }
        val previewLikeAbsoluteMax = when {
            screenConfig.screenWidthDp >= 1800 -> 500.dp
            screenConfig.screenWidthDp >= 1200 -> 460.dp
            else -> 400.dp
        }
        val maxCoverWidth = if (applyPreviewLandscapeBounds) {
            minOf(targetWidth * 0.94f, previewLikeAbsoluteMax).coerceAtLeast(120.dp)
        } else {
            targetWidth.coerceAtMost(520.dp).coerceAtLeast(120.dp)
        }
        val maxCoverHeight = if (applyPreviewLandscapeBounds) {
            minOf(targetHeight * 0.90f, previewLikeAbsoluteMax).coerceAtLeast(120.dp)
        } else {
            targetHeight.coerceAtMost(520.dp).coerceAtLeast(120.dp)
        }
        val coverWidth: androidx.compose.ui.unit.Dp
        val coverHeight: androidx.compose.ui.unit.Dp
        if (coverAspectRatio >= 1f) {
            val candidateWidth = maxCoverHeight * coverAspectRatio
            coverWidth = minOf(maxCoverWidth, candidateWidth)
            coverHeight = (coverWidth / coverAspectRatio).coerceAtMost(maxCoverHeight)
        } else {
            val candidateHeight = maxCoverWidth / coverAspectRatio
            coverHeight = minOf(maxCoverHeight, candidateHeight)
            coverWidth = (coverHeight * coverAspectRatio).coerceAtMost(maxCoverWidth)
        }

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
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .width(coverWidth)
                            .height(coverHeight)
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
                    .width(coverWidth)
                    .height(coverHeight)
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
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun MusicPlayerControlPanel(
    modifier: Modifier,
    isLandscape: Boolean,
    controller: MusicPlaybackController,
    currentAudio: AudioFile?,
    position: Long,
    duration: Long,
    panelColor: Color,
    panelOnColor: Color,
    accentColor: Color,
    controlAccentColor: Color,
    onControlAccentColor: Color,
    oppositeControlColor: Color,
    onOppositeControlColor: Color,
    showLyricPreviewButton: Boolean,
    onLyricPreviewClick: () -> Unit,
    onShowPlaylist: () -> Unit,
    onSongInfoClick: () -> Unit,
    onArtistsClick: (List<String>) -> Unit
) {
    val controlScale = if (isLandscape) 1.12f else 1f
    val panelPaddingH = if (isLandscape) 20.dp else 16.dp
    val panelPaddingV = if (isLandscape) 18.dp else 14.dp
    val titleSize = (24f * controlScale).sp
    val artistSize = (15f * controlScale).sp
    val timeSize = (12f * controlScale).sp
    val progressHeight = if (isLandscape) 36.dp else 32.dp
    val transportHeight = if (isLandscape) 56.dp else 48.dp
    val actionIconSize = if (isLandscape) 22.dp else 20.dp
    val actionButtonRadius = if (isLandscape) 18.dp else 16.dp
    val actionButtonPaddingH = if (isLandscape) 20.dp else 18.dp
    val actionButtonPaddingV = if (isLandscape) 11.dp else 10.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(panelColor)
            .padding(horizontal = panelPaddingH, vertical = panelPaddingV),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val title = controller.currentTitle.ifBlank { currentAudio?.displayTitle.orEmpty() }
                        val artist = controller.currentArtist.ifBlank { currentAudio?.displayArtist.orEmpty() }
                        val artists = extractAllArtistsForPlayer(title, artist)
                        if (artists.isNotEmpty()) {
                            onArtistsClick(artists)
                        }
                    }
            ) {
                Text(
                    text = controller.currentTitle.ifBlank { "未选择歌曲" },
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = panelOnColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = controller.currentArtist.ifBlank { "未知艺术家" },
                    fontSize = artistSize,
                    color = panelOnColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onSongInfoClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多",
                    tint = panelOnColor
                )
            }
        }

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
                .height(progressHeight)
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
                fontSize = timeSize
            )
            Text(
                text = formatPlaybackTime(safeDuration),
                color = panelOnColor.copy(alpha = 0.74f),
                fontSize = timeSize
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
                .height(transportHeight),
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
                enabled = controller.hasNextTrack,
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                colors = toggleColors
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "下一首",
                    tint = if (controller.hasNextTrack) {
                        onOppositeControlColor
                    } else {
                        onOppositeControlColor.copy(alpha = 0.42f)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val modeIcon = when (controller.playbackMode) {
            PlaybackMode.SEQUENTIAL -> Icons.Rounded.Repeat
            PlaybackMode.SHUFFLE -> Icons.Rounded.Shuffle
            PlaybackMode.SINGLE_REPEAT -> Icons.Rounded.RepeatOne
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerBottomActionButton(
                modifier = Modifier.weight(1f),
                icon = modeIcon,
                tint = accentColor,
                onClick = { controller.cyclePlaybackMode() },
                contentDescription = "播放模式",
                iconSize = actionIconSize,
                cornerRadius = actionButtonRadius,
                horizontalPadding = actionButtonPaddingH,
                verticalPadding = actionButtonPaddingV
            )
            if (showLyricPreviewButton) {
                PlayerBottomActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Lyrics,
                    tint = accentColor,
                    onClick = onLyricPreviewClick,
                    contentDescription = "歌词预览",
                    iconSize = actionIconSize,
                    cornerRadius = actionButtonRadius,
                    horizontalPadding = actionButtonPaddingH,
                    verticalPadding = actionButtonPaddingV
                )
            }
            PlayerBottomActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                tint = accentColor,
                onClick = onShowPlaylist,
                contentDescription = "播放列表",
                iconSize = actionIconSize,
                cornerRadius = actionButtonRadius,
                horizontalPadding = actionButtonPaddingH,
                verticalPadding = actionButtonPaddingV
            )
        }
    }
}

@Composable
private fun PlayerBottomActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 18.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 10.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun extractAllArtistsForPlayer(title: String, artist: String): List<String> {
    fun splitArtists(raw: String): List<String> {
        return raw
            .replace("／", "/")
            .replace("；", ";")
            .replace("，", ",")
            .split("/", "&", ";", ",", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    val base = splitArtists(artist)
    val featPattern = Regex("""(?i)(?:feat\.?|ft\.?|featuring|with)\s*([^\]\)\(（\[]+)""")
    val titleArtists = featPattern.findAll(title)
        .flatMap { match -> splitArtists(match.groupValues.getOrElse(1) { "" }).asSequence() }
        .toList()

    return (base + titleArtists)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
}

private fun buildAudioFileForPlayer(
    path: String,
    titleHint: String = "",
    artistHint: String = "",
    coverCachePath: String? = null,
    durationHint: Long = 0L
): AudioFile {
    val file = File(path)
    val title = titleHint.ifBlank { file.nameWithoutExtension }
    return AudioFile(
        path = path,
        title = title,
        artist = artistHint,
        album = "",
        duration = durationHint.coerceAtLeast(0L),
        fileSize = if (file.exists()) file.length() else 0L,
        lastModified = if (file.exists()) file.lastModified() else 0L,
        addedTime = if (file.exists()) file.lastModified() else System.currentTimeMillis(),
        coverCachePath = coverCachePath,
        year = "",
        mediaStoreId = -1L
    )
}

private fun resolveCoverCachePathForAudio(context: Context, audioPath: String): String? {
    if (audioPath.isBlank()) return null
    val cacheFile = File(File(context.cacheDir, "covers"), "${audioPath.hashCode()}.jpg")
    return cacheFile.absolutePath.takeIf { cacheFile.exists() }
}

private fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
