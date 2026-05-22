package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.GlobalMiniPlayerBar
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class ArtistsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ARTIST_NAME = "artist_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialArtistName = intent.getStringExtra(EXTRA_ARTIST_NAME).orEmpty()

        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0)
                ) { paddingValues ->
                    if (initialArtistName.isBlank()) {
                        ArtistsScreen(
                            onBack = { finish() },
                            modifier = Modifier.padding(paddingValues)
                        )
                    } else {
                        ArtistDetailScreen(
                            artistName = initialArtistName,
                            onBack = { finish() },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}

private enum class ArtistSortType(val displayName: String) {
    NAME("艺术家名称"),
    SONG_COUNT("歌曲总数"),
    ALBUM_COUNT("专辑总数")
}

internal enum class ArtistDetailTab {
    SONGS, ALBUMS
}

private val artistDetailPagerTabs = listOf(ArtistDetailTab.SONGS, ArtistDetailTab.ALBUMS)

private fun ArtistDetailTab.toPagerPage(): Int {
    return artistDetailPagerTabs.indexOf(this).takeIf { it >= 0 } ?: 0
}

private fun artistDetailTabForPage(page: Int): ArtistDetailTab {
    return artistDetailPagerTabs.getOrElse(page) { ArtistDetailTab.SONGS }
}

private data class ArtistInfo(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val firstAudio: AudioFile?
)

private data class ArtistAccumulator(
    val name: String,
    val songPaths: MutableSet<String> = linkedSetOf(),
    val albumKeys: MutableSet<String> = linkedSetOf(),
    var firstAudio: AudioFile? = null
)

private data class ArtistDetailInfo(
    val name: String,
    val songs: List<AudioFile>,
    val albums: List<ArtistAlbumInfo>
) {
    val songCount: Int
        get() = songs.size
    val albumCount: Int
        get() = albums.size
    val firstAudio: AudioFile?
        get() = songs.firstOrNull()
}

private data class ArtistAlbumInfo(
    val name: String,
    val songs: List<AudioFile>,
    val coverAudio: AudioFile?
)

private data class AlbumDetailInfo(
    val name: String,
    val songs: List<AlbumSongInfo>,
    val albumArtist: String,
    val releaseDateInfo: String,
    val copyrightInfo: String
) {
    val songCount: Int
        get() = songs.size
    val totalDurationMs: Long
        get() = songs.sumOf { it.audio.duration }
    val coverAudio: AudioFile?
        get() = songs.firstOrNull()?.audio
}

private data class AlbumSongInfo(
    val audio: AudioFile,
    val trackNumber: String,
    val discNumber: String,
    val artistName: String,
    val albumArtistName: String,
    val releaseDateRaw: String,
    val displayTrackNumber: String
)

private data class ArtistAvatarSaveResult(
    val success: Boolean,
    val savedPath: String? = null,
    val errorMessage: String? = null
)

private data class SourceMediaDeleteResult(
    val success: Boolean,
    val errorMessage: String? = null
)

private val artistAvatarBitmapCache = android.util.LruCache<String, Bitmap>(24)
private val artistDetailInfoMemoryCache = mutableMapOf<String, ArtistDetailInfo>()
private val edgeTranslucentHeight = 10.dp

@Composable
private fun EdgeTranslucent(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    height: Dp = edgeTranslucentHeight
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color,
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
internal fun EmbeddedArtistsScreen(
    audioFiles: List<AudioFile>,
    isLoading: Boolean,
    miniPlayerExtraBottomPadding: Dp,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    var artists by remember { mutableStateOf<List<ArtistInfo>>(emptyList()) }
    var isBuildingArtists by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortType by rememberSaveable {
        mutableStateOf(
            runCatching {
                ArtistSortType.valueOf(
                    prefs.getString("artistSortType", ArtistSortType.NAME.name)
                        ?: ArtistSortType.NAME.name
                )
            }.getOrDefault(ArtistSortType.NAME)
        )
    }
    val audioFingerprint = remember(audioFiles) {
        audioFiles.fold(audioFiles.size.toLong()) { acc, audio ->
            acc * 31L + audio.path.hashCode().toLong() + audio.lastModified
        }
    }
    val artistSplitWhitelist = remember { ArtistSplitWhitelistStore.load(context) }
    val artistSplitWhitelistFingerprint = remember(artistSplitWhitelist) {
        ArtistSplitWhitelistStore.fingerprint(artistSplitWhitelist)
    }

    val visibleArtists by remember {
        derivedStateOf {
            val trimmed = searchQuery.trim()
            val filtered = if (trimmed.isEmpty()) {
                artists
            } else {
                artists.filter { it.name.contains(trimmed, ignoreCase = true) }
            }
            sortArtistInfos(filtered, sortType)
        }
    }

    fun persistSortType(newSortType: ArtistSortType) {
        sortType = newSortType
        prefs.edit().putString("artistSortType", newSortType.name).apply()
    }

    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
    }

    LaunchedEffect(audioFingerprint, artistSplitWhitelistFingerprint) {
        isBuildingArtists = true
        artists = withContext(Dispatchers.Default) {
            buildArtistInfos(audioFiles, artistSplitWhitelist)
        }
        isBuildingArtists = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CommonHeadBar(
            title = "艺术家",
            showBack = true,
            showMenu = true,
            onBackClick = onBack,
            onMenuClick = { menuExpanded = true },
            menuContent = { menuButtonPosition ->
                CustomDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = listOf(
                        MenuItem(
                            title = "排序方式",
                            subItems = ArtistSortType.values().map { item ->
                                MenuItem(
                                    title = item.displayName,
                                    onClick = { persistSortType(item) }
                                )
                            }
                        )
                    ),
                    anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f)
                )
            }
        )

        ArtistSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClear = { searchQuery = "" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            focusRequester = searchFocusRequester
        )

        Box(modifier = Modifier.fillMaxSize()) {
            val listTopPadding = 8.dp
            when {
                isLoading || isBuildingArtists -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                visibleArtists.isEmpty() -> {
                    EmptyArtistsState(
                        text = if (searchQuery.isBlank()) "暂无艺术家" else "未找到匹配的艺术家",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = listTopPadding,
                            end = 16.dp,
                            bottom = navigationBarsPadding.calculateBottomPadding() + 24.dp + miniPlayerExtraBottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = visibleArtists,
                            key = { it.name.lowercase(Locale.ROOT) }
                        ) { artist ->
                            ArtistListItem(
                                artist = artist,
                                onClick = { onArtistClick(artist.name) }
                            )
                        }
                    }
                }
            }
            EdgeTranslucent(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
internal fun EmbeddedArtistDetailScreen(
    audioFiles: List<AudioFile>,
    artistName: String,
    miniPlayerExtraBottomPadding: Dp,
    onBack: () -> Unit,
    onSongClick: (List<AudioFile>, AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit,
    selectedTab: ArtistDetailTab,
    onTabSelected: (ArtistDetailTab) -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var detailInfo by remember { mutableStateOf<ArtistDetailInfo?>(null) }
    var avatarRefreshVersion by remember(artistName) { mutableStateOf(0) }
    var headBarCollapsed by remember(artistName) { mutableStateOf(false) }
    val animatedHeadBarProgress by animateFloatAsState(
        targetValue = if (headBarCollapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "artistHeadBarCollapsedProgress"
    )
    val headBarBackgroundColor = lerp(
        Color.Transparent,
        MaterialTheme.colorScheme.primaryContainer,
        animatedHeadBarProgress
    )
    val headBarContentColor = lerp(
        MaterialTheme.colorScheme.onBackground,
        MaterialTheme.colorScheme.onPrimaryContainer,
        animatedHeadBarProgress
    )
    val artistImageController = rememberArtistImagePickerController(
        artistName = artistName,
        onSaved = { avatarRefreshVersion += 1 }
    )
    val audioFingerprint = remember(audioFiles) {
        audioFiles.fold(audioFiles.size.toLong()) { acc, audio ->
            acc * 31L + audio.path.hashCode().toLong() + audio.lastModified
        }
    }
    val artistSplitWhitelist = remember { ArtistSplitWhitelistStore.load(context) }
    val artistSplitWhitelistFingerprint = remember(artistSplitWhitelist) {
        ArtistSplitWhitelistStore.fingerprint(artistSplitWhitelist)
    }
    val detailCacheKey = remember(artistName, audioFingerprint, artistSplitWhitelistFingerprint) {
        buildArtistDetailCacheKey(artistName, audioFingerprint, artistSplitWhitelistFingerprint)
    }

    LaunchedEffect(artistName, audioFingerprint, artistSplitWhitelistFingerprint) {
        artistDetailInfoMemoryCache[detailCacheKey]?.let { cached ->
            detailInfo = cached
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        val loadedDetail = withContext(Dispatchers.Default) {
            buildArtistDetailInfo(
                audioFiles = audioFiles,
                artistName = artistName,
                artistSplitWhitelist = artistSplitWhitelist
            )
        }
        detailInfo = loadedDetail
        loadedDetail?.let { artistDetailInfoMemoryCache[detailCacheKey] = it }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            detailInfo == null -> {
                EmptyArtistsState(
                    text = "未找到艺术家",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                val detail = detailInfo!!
                ArtistDetailContent(
                    detail = detail,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    miniPlayerExtraBottomPadding = miniPlayerExtraBottomPadding,
                    avatarRefreshVersion = avatarRefreshVersion,
                    onSongClick = { audio -> onSongClick(detail.songs, audio) },
                    onSongMoreClick = onSongMoreClick,
                    onAlbumClick = { album -> onAlbumClick(album.name) },
                    onHeadBarCollapsedChange = { headBarCollapsed = it }
                )
            }
        }

        TransparentArtistHeadBar(
            onBack = onBack,
            contentColor = headBarContentColor,
            backgroundColor = headBarBackgroundColor,
            title = detailInfo?.name ?: artistName,
            titleAlpha = animatedHeadBarProgress,
            menuItems = listOf(
                MenuItem(
                    title = "修改艺术家图片",
                    onClick = { artistImageController.launchPicker() }
                )
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )
    }

    artistImageController.renderDialogs()
}

@Composable
internal fun EmbeddedAlbumDetailScreen(
    audioFiles: List<AudioFile>,
    albumName: String,
    miniPlayerExtraBottomPadding: Dp,
    onBack: () -> Unit,
    onSongClick: (List<AudioFile>, AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var detailInfo by remember { mutableStateOf<AlbumDetailInfo?>(null) }
    var coverRefreshVersion by remember(albumName) { mutableStateOf(0) }
    var headBarContentColor by remember { mutableStateOf(Color.White) }
    var headBarThemeColor by remember(albumName) { mutableStateOf(Color.Transparent) }
    var headBarCollapsed by remember(albumName) { mutableStateOf(false) }
    val animatedHeadBarProgress by animateFloatAsState(
        targetValue = if (headBarCollapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "albumHeadBarCollapsedProgress"
    )
    val headBarBackgroundColor = lerp(
        Color.Transparent,
        headBarThemeColor,
        animatedHeadBarProgress
    )
    val audioFingerprint = remember(audioFiles) {
        audioFiles.fold(audioFiles.size.toLong()) { acc, audio ->
            acc * 31L + audio.path.hashCode().toLong() + audio.lastModified
        }
    }
    val albumVideoController = rememberAlbumVideoPickerController(
        albumName = albumName,
        audioPath = detailInfo?.coverAudio?.path,
        onSaved = { coverRefreshVersion += 1 }
    )

    LaunchedEffect(albumName, audioFingerprint) {
        isLoading = true
        detailInfo = withContext(Dispatchers.IO) {
            buildAlbumDetailInfo(
                context = context,
                audioFiles = audioFiles,
                albumName = albumName
            )
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            detailInfo == null -> {
                EmptyArtistsState(
                    text = "未找到专辑",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                val detail = detailInfo!!
                AlbumDetailContent(
                    detail = detail,
                    miniPlayerExtraBottomPadding = miniPlayerExtraBottomPadding,
                    coverRefreshVersion = coverRefreshVersion,
                    onHeadBarContentColorChange = { color -> headBarContentColor = color },
                    onHeadBarBackgroundColorChange = { color -> headBarThemeColor = color },
                    onHeadBarCollapsedChange = { collapsed -> headBarCollapsed = collapsed },
                    onSongClick = { audio -> onSongClick(detail.songs.map { it.audio }, audio) },
                    onSongMoreClick = onSongMoreClick
                )
            }
        }

        TransparentArtistHeadBar(
            onBack = onBack,
            contentColor = headBarContentColor,
            backgroundColor = headBarBackgroundColor,
            title = detailInfo?.name ?: albumName,
            titleAlpha = animatedHeadBarProgress,
            menuItems = listOf(
                MenuItem(
                    title = "添加视频封面",
                    onClick = { albumVideoController.launchPicker() }
                )
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )
    }

    albumVideoController.renderDialogs()
}

@Composable
private fun ArtistsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val playbackController = rememberMusicPlaybackController()
    val showMiniPlayer = playbackController.hasCurrentItem
    val miniPlayerExtraBottomPadding = if (showMiniPlayer) 84.dp else 0.dp
    val artists = remember { mutableStateListOf<ArtistInfo>() }
    var isLoading by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val artistSplitWhitelist = remember { ArtistSplitWhitelistStore.load(context) }
    val artistSplitWhitelistFingerprint = remember(artistSplitWhitelist) {
        ArtistSplitWhitelistStore.fingerprint(artistSplitWhitelist)
    }
    var sortType by rememberSaveable {
        mutableStateOf(
            runCatching {
                ArtistSortType.valueOf(
                    prefs.getString("artistSortType", ArtistSortType.NAME.name)
                        ?: ArtistSortType.NAME.name
                )
            }.getOrDefault(ArtistSortType.NAME)
        )
    }

    val visibleArtists by remember {
        derivedStateOf {
            val trimmed = searchQuery.trim()
            val filtered = if (trimmed.isEmpty()) {
                artists.toList()
            } else {
                artists.filter { it.name.contains(trimmed, ignoreCase = true) }
            }
            sortArtistInfos(filtered, sortType)
        }
    }

    fun persistSortType(newSortType: ArtistSortType) {
        sortType = newSortType
        prefs.edit().putString("artistSortType", newSortType.name).apply()
    }

    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
    }

    LaunchedEffect(artistSplitWhitelistFingerprint) {
        isLoading = true
        val loadedArtists = withContext(Dispatchers.IO) {
            buildArtistInfos(musicLibraryCacheStore(context).loadAllPaged(), artistSplitWhitelist)
        }
        artists.clear()
        artists.addAll(loadedArtists)
        isLoading = false
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommonHeadBar(
                title = "艺术家",
                showBack = true,
                showMenu = true,
                onBackClick = onBack,
                onMenuClick = { menuExpanded = true },
                menuContent = { menuButtonPosition ->
                    CustomDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        items = listOf(
                            MenuItem(
                                title = "排序方式",
                                subItems = ArtistSortType.values().map { item ->
                                    MenuItem(
                                        title = item.displayName,
                                        onClick = { persistSortType(item) }
                                    )
                                }
                            )
                        ),
                        anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f)
                    )
                }
            )

            ArtistSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                focusRequester = searchFocusRequester
            )

            Box(modifier = Modifier.fillMaxSize()) {
                val listTopPadding = 8.dp
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    visibleArtists.isEmpty() -> {
                        EmptyArtistsState(
                            text = if (searchQuery.isBlank()) "暂无艺术家" else "未找到匹配的艺术家",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = listTopPadding,
                                end = 16.dp,
                                bottom = navigationBarsPadding.calculateBottomPadding() + 24.dp + miniPlayerExtraBottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = visibleArtists,
                                key = { it.name.lowercase(Locale.ROOT) }
                            ) { artist ->
                                ArtistListItem(
                                    artist = artist,
                                    onClick = {
                                        val intent = Intent(context, ArtistsActivity::class.java).apply {
                                            putExtra(ArtistsActivity.EXTRA_ARTIST_NAME, artist.name)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
                EdgeTranslucent(modifier = Modifier.align(Alignment.TopCenter))
            }
        }

        ArtistsMiniPlayerOverlay(
            controller = playbackController,
            visible = showMiniPlayer,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ArtistsMiniPlayerOverlay(
    controller: MusicPlaybackController,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .navigationBarsPadding()
            .padding(
                start = 12.dp,
                end = 12.dp,
                bottom = 8.dp
            ),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        GlobalMiniPlayerBar(
            controller = controller,
            onExpand = { MusicPlayerActivity.start(context) }
        )
    }
}

@Composable
private fun ArtistDetailScreen(
    artistName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackController = rememberMusicPlaybackController()
    val showMiniPlayer = playbackController.hasCurrentItem
    val miniPlayerExtraBottomPadding = if (showMiniPlayer) 84.dp else 0.dp
    var isLoading by remember { mutableStateOf(true) }
    var detailInfo by remember { mutableStateOf<ArtistDetailInfo?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(ArtistDetailTab.SONGS) }
    var selectedSongInfoAudio by remember { mutableStateOf<AudioFile?>(null) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var favoritePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val artistSplitWhitelist = remember { ArtistSplitWhitelistStore.load(context) }
    val artistSplitWhitelistFingerprint = remember(artistSplitWhitelist) {
        ArtistSplitWhitelistStore.fingerprint(artistSplitWhitelist)
    }
    var avatarRefreshVersion by remember(artistName) { mutableStateOf(0) }
    var headBarCollapsed by remember(artistName) { mutableStateOf(false) }
    val animatedHeadBarProgress by animateFloatAsState(
        targetValue = if (headBarCollapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "artistHeadBarCollapsedProgress"
    )
    val headBarBackgroundColor = lerp(
        Color.Transparent,
        MaterialTheme.colorScheme.primaryContainer,
        animatedHeadBarProgress
    )
    val headBarContentColor = lerp(
        MaterialTheme.colorScheme.onBackground,
        MaterialTheme.colorScheme.onPrimaryContainer,
        animatedHeadBarProgress
    )
    val artistImageController = rememberArtistImagePickerController(
        artistName = artistName,
        onSaved = { avatarRefreshVersion += 1 }
    )

    LaunchedEffect(artistName, artistSplitWhitelistFingerprint) {
        isLoading = true
        detailInfo = withContext(Dispatchers.IO) {
            buildArtistDetailInfo(
                audioFiles = musicLibraryCacheStore(context).loadAllPaged(),
                artistName = artistName,
                artistSplitWhitelist = artistSplitWhitelist
            )
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        favoritePaths = withContext(Dispatchers.IO) {
            LocalPlaylistStore.loadFavoritePaths(context)
        }
    }

    fun toggleFavorite(audio: AudioFile) {
        val updated = favoritePaths.toMutableSet()
        if (audio.path in updated) {
            updated.remove(audio.path)
        } else {
            updated.add(audio.path)
        }
        favoritePaths = updated
        saveArtistFavoritePaths(context, updated)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            detailInfo == null -> {
                EmptyArtistsState(
                    text = "未找到艺术家",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                ArtistDetailContent(
                    detail = detailInfo!!,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    miniPlayerExtraBottomPadding = miniPlayerExtraBottomPadding,
                    avatarRefreshVersion = avatarRefreshVersion,
                    onSongClick = { audio ->
                        detailInfo?.songs?.let { songs ->
                            playbackController.playQueue(songs, audio.path)
                        }
                    },
                    onSongMoreClick = { audio ->
                        selectedSongInfoAudio = audio
                        showSongInfoSheet = true
                    },
                    onHeadBarCollapsedChange = { headBarCollapsed = it }
                )
            }
        }

        TransparentArtistHeadBar(
            onBack = onBack,
            contentColor = headBarContentColor,
            backgroundColor = headBarBackgroundColor,
            title = detailInfo?.name ?: artistName,
            titleAlpha = animatedHeadBarProgress,
            menuItems = listOf(
                MenuItem(
                    title = "修改艺术家图片",
                    onClick = { artistImageController.launchPicker() }
                )
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )

        ArtistsMiniPlayerOverlay(
            controller = playbackController,
            visible = showMiniPlayer,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showSongInfoSheet && selectedSongInfoAudio != null) {
        val infoAudio = selectedSongInfoAudio!!
        SongInfoBottomSheet(
            audio = infoAudio,
            isFavorite = infoAudio.path in favoritePaths,
            isSleepTimerRunning = playbackController.sleepTimerState.isActive,
            renameSuccessSignal = 0L,
            onDismiss = {
                showSongInfoSheet = false
                selectedSongInfoAudio = null
            },
            onPlayNext = {
                playbackController.insertNext(infoAudio)
            },
            onToggleFavorite = {
                toggleFavorite(infoAudio)
            },
            onOpenSleepTimer = {
                showSongInfoSheet = false
                selectedSongInfoAudio = null
                showSleepTimerSheet = true
            }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            isRunning = playbackController.sleepTimerState.isActive,
            remainingMs = playbackController.sleepTimerState.remainingMs,
            onDismiss = { showSleepTimerSheet = false },
            onStartTimer = { minutes, finishCurrentSong ->
                playbackController.startSleepTimer(minutes = minutes, finishCurrentSong = finishCurrentSong)
            },
            onCancelTimer = {
                playbackController.cancelSleepTimer()
            }
        )
    }

    artistImageController.renderDialogs()
}

@Composable
private fun ArtistDetailContent(
    detail: ArtistDetailInfo,
    selectedTab: ArtistDetailTab,
    onTabSelected: (ArtistDetailTab) -> Unit,
    miniPlayerExtraBottomPadding: androidx.compose.ui.unit.Dp,
    avatarRefreshVersion: Int,
    onSongClick: (AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit,
    onAlbumClick: ((ArtistAlbumInfo) -> Unit)? = null,
    onHeadBarCollapsedChange: (Boolean) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = navigationBarsPadding.calculateBottomPadding() + 24.dp + miniPlayerExtraBottomPadding
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = selectedTab.toPagerPage()
    ) {
        artistDetailPagerTabs.size
    }

    LaunchedEffect(selectedTab) {
        val targetPage = selectedTab.toPagerPage()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val tab = artistDetailTabForPage(pagerState.currentPage)
        if (tab != selectedTab) {
            onTabSelected(tab)
        }
    }

    LaunchedEffect(configuration.orientation) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onHeadBarCollapsedChange(false)
        }
    }

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ArtistDetailLandscapePanel(
                detail = detail,
                avatarRefreshVersion = avatarRefreshVersion,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                ArtistDetailTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val pageTab = artistDetailTabForPage(page)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 10.dp,
                            bottom = bottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        artistDetailListItems(
                            detail = detail,
                            selectedTab = pageTab,
                            onSongClick = onSongClick,
                            onSongMoreClick = onSongMoreClick,
                            onAlbumClick = onAlbumClick
                        )
                    }
                }
            }
        }
    } else {
        ArtistDetailPortraitContent(
            detail = detail,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            pagerState = pagerState,
            bottomPadding = bottomPadding,
            avatarRefreshVersion = avatarRefreshVersion,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onAlbumClick = onAlbumClick,
            onHeadBarCollapsedChange = onHeadBarCollapsedChange
        )
    }
}

@Composable
private fun ArtistDetailPortraitContent(
    detail: ArtistDetailInfo,
    selectedTab: ArtistDetailTab,
    onTabSelected: (ArtistDetailTab) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
    bottomPadding: Dp,
    avatarRefreshVersion: Int,
    onSongClick: (AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit,
    onAlbumClick: ((ArtistAlbumInfo) -> Unit)? = null,
    onHeadBarCollapsedChange: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val heroHeight = 360.dp
    val tabHeight = 56.dp
    val headBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 50.dp
    val heroHeightPx = with(density) { heroHeight.toPx() }
    val tabHeightPx = with(density) { tabHeight.toPx() }
    val headBarHeightPx = with(density) { headBarHeight.toPx() }
    val maxCollapsePx = (heroHeightPx - headBarHeightPx).coerceAtLeast(1f)
    var headerOffsetPx by remember(detail.name) { mutableStateOf(0f) }
    val headBarCollapsed by remember(maxCollapsePx) {
        derivedStateOf {
            -headerOffsetPx >= (maxCollapsePx - 1f).coerceAtLeast(0f)
        }
    }
    val pagerTopPadding = with(density) {
        (heroHeightPx + tabHeightPx + headerOffsetPx)
            .coerceAtLeast(headBarHeightPx + tabHeightPx)
            .toDp()
    }
    val nestedScrollConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta >= 0f || headerOffsetPx <= -maxCollapsePx) return Offset.Zero
                val oldOffset = headerOffsetPx
                headerOffsetPx = (headerOffsetPx + delta).coerceIn(-maxCollapsePx, 0f)
                return Offset(x = 0f, y = headerOffsetPx - oldOffset)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta <= 0f || headerOffsetPx >= 0f) return Offset.Zero
                val oldOffset = headerOffsetPx
                headerOffsetPx = (headerOffsetPx + delta).coerceIn(-maxCollapsePx, 0f)
                return Offset(x = 0f, y = headerOffsetPx - oldOffset)
            }
        }
    }

    LaunchedEffect(maxCollapsePx) {
        headerOffsetPx = headerOffsetPx.coerceIn(-maxCollapsePx, 0f)
    }

    LaunchedEffect(headBarCollapsed) {
        onHeadBarCollapsedChange(headBarCollapsed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = pagerTopPadding)
        ) { page ->
            val pageTab = artistDetailTabForPage(page)
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 6.dp,
                    bottom = bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                artistDetailListItems(
                    detail = detail,
                    selectedTab = pageTab,
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick,
                    onAlbumClick = onAlbumClick,
                    itemModifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = headerOffsetPx
                }
        ) {
            ArtistDetailHero(
                detail = detail,
                avatarRefreshVersion = avatarRefreshVersion
            )
            ArtistDetailTabRow(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            EdgeTranslucent()
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.artistDetailListItems(
    detail: ArtistDetailInfo,
    selectedTab: ArtistDetailTab,
    onSongClick: (AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit,
    onAlbumClick: ((ArtistAlbumInfo) -> Unit)? = null,
    itemModifier: Modifier = Modifier
) {
    when (selectedTab) {
        ArtistDetailTab.SONGS -> {
            items(
                items = detail.songs,
                key = { it.path }
            ) { audio ->
                ArtistDetailSongItem(
                    audio = audio,
                    onClick = { onSongClick(audio) },
                    onMoreClick = { onSongMoreClick(audio) },
                    modifier = itemModifier
                )
            }
        }
        ArtistDetailTab.ALBUMS -> {
            items(
                items = detail.albums,
                key = { it.name.lowercase(Locale.ROOT) }
            ) { album ->
                ArtistDetailAlbumItem(
                    album = album,
                    onClick = onAlbumClick?.let { click -> { click(album) } },
                    modifier = itemModifier
                )
            }
        }
    }
}

@Composable
private fun AlbumDetailContent(
    detail: AlbumDetailInfo,
    miniPlayerExtraBottomPadding: Dp,
    coverRefreshVersion: Int,
    onHeadBarContentColorChange: (Color) -> Unit = {},
    onHeadBarBackgroundColorChange: (Color) -> Unit = {},
    onHeadBarCollapsedChange: (Boolean) -> Unit = {},
    onSongClick: (AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = navigationBarsPadding.calculateBottomPadding() + 24.dp + miniPlayerExtraBottomPadding
    val coverAudio = detail.coverAudio
    val targetSizePx = with(density) { 900.dp.toPx().toInt().coerceAtLeast(480) }
    val artistSplitWhitelist = remember { ArtistSplitWhitelistStore.load(context) }
    var coverBitmap by remember(detail.name, coverAudio?.path, coverAudio?.coverCachePath) {
        mutableStateOf<Bitmap?>(null)
    }
    var coverThemePair by remember { mutableStateOf<CoverThemeColorPair?>(null) }
    val coverThemeCacheKey = remember(coverAudio?.coverCachePath, coverAudio?.mediaStoreId, coverAudio?.path) {
        buildCoverThemeCacheKey(
            coverCachePath = coverAudio?.coverCachePath,
            mediaId = coverAudio
                ?.mediaStoreId
                ?.takeIf { it > 0L }
                ?.toString(),
            audioPath = coverAudio?.path
        )
    }
    val isDarkTheme = colorLuminance(MaterialTheme.colorScheme.background) < 0.5f

    LaunchedEffect(coverAudio?.path, coverAudio?.coverCachePath, coverAudio?.lastModified) {
        coverBitmap = if (coverAudio == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                loadAudioCoverBitmap(context, coverAudio, targetSizePx, preferEmbeddedCover = true)
            }
        }
    }

    LaunchedEffect(coverThemeCacheKey, coverBitmap) {
        val bitmap = coverBitmap
        coverThemePair = if (bitmap == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                resolveCachedCoverThemePair(
                    context = context,
                    cacheKey = coverThemeCacheKey,
                    cover = bitmap
                )
            }
        }
    }

    val coverThemeColor = if (isDarkTheme) {
        coverThemePair?.darkColor
    } else {
        coverThemePair?.lightColor
    }
    val targetBackgroundColor = normalizeCoverThemeBackground(
        coverThemeColor ?: MaterialTheme.colorScheme.background,
        isDarkTheme
    )
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 360),
        label = "albumDetailBackgroundColor"
    )
    val backgroundIsLight = colorLuminance(backgroundColor) > 0.52f
    val onBackgroundColor = if (backgroundIsLight) Color(0xFF111111) else Color.White
    val listContainerColor = if (backgroundIsLight) {
        blendColorForUi(backgroundColor, Color.White, 0.34f)
    } else {
        blendColorForUi(backgroundColor, Color.Black, 0.24f)
    }
    val onListContainerColor = if (colorLuminance(listContainerColor) > 0.52f) Color(0xFF111111) else Color.White
    val accentBase = coverThemeColor ?: MaterialTheme.colorScheme.primary
    val listAccentColor = if (backgroundIsLight) {
        blendColorForUi(accentBase, Color.Black, 0.28f)
    } else {
        blendColorForUi(accentBase, Color.White, 0.52f)
    }

    LaunchedEffect(onBackgroundColor) {
        onHeadBarContentColorChange(onBackgroundColor)
    }
    LaunchedEffect(backgroundColor) {
        onHeadBarBackgroundColorChange(backgroundColor)
    }
    LaunchedEffect(configuration.orientation) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onHeadBarCollapsedChange(false)
        }
    }

    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AlbumDetailLandscapePanel(
                        detail = detail,
                        coverBitmap = coverBitmap,
                        backgroundColor = backgroundColor,
                        contentColor = onBackgroundColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            bottom = bottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        albumDetailSongItems(
                            detail = detail,
                            artistSplitWhitelist = artistSplitWhitelist,
                            onSongClick = onSongClick,
                            onSongMoreClick = onSongMoreClick,
                            containerColor = listContainerColor,
                            contentColor = onListContainerColor,
                            accentColor = listAccentColor
                        )
                        item {
                            AlbumDetailFooter(
                                detail = detail,
                                contentColor = onBackgroundColor,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } else {
                val headBarHeightPx = with(density) {
                    (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 50.dp).toPx()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        AlbumDetailHero(
                            detail = detail,
                            coverBitmap = coverBitmap,
                            backgroundColor = backgroundColor,
                            contentColor = onBackgroundColor,
                            coverRefreshVersion = coverRefreshVersion,
                            headBarHeightPx = headBarHeightPx,
                            onTitleCollapsedChange = onHeadBarCollapsedChange
                        )
                    }
                    albumDetailSongItems(
                        detail = detail,
                        artistSplitWhitelist = artistSplitWhitelist,
                        onSongClick = onSongClick,
                        onSongMoreClick = onSongMoreClick,
                        containerColor = listContainerColor,
                        contentColor = onListContainerColor,
                        accentColor = listAccentColor,
                        itemModifier = Modifier.padding(horizontal = 16.dp)
                    )
                    item {
                        AlbumDetailFooter(
                            detail = detail,
                            contentColor = onBackgroundColor,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.albumDetailSongItems(
    detail: AlbumDetailInfo,
    artistSplitWhitelist: Collection<String>,
    onSongClick: (AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    itemModifier: Modifier = Modifier
) {
    items(
        items = detail.songs,
        key = { it.audio.path }
    ) { songInfo ->
        AlbumDetailSongItem(
            songInfo = songInfo,
            onClick = { onSongClick(songInfo.audio) },
            onMoreClick = { onSongMoreClick(songInfo.audio) },
            containerColor = containerColor,
            contentColor = contentColor,
            accentColor = accentColor,
            showArtist = shouldShowAlbumSongArtist(detail.albumArtist, songInfo.artistName, artistSplitWhitelist),
            modifier = itemModifier
        )
    }
}

private fun Modifier.albumCoverBottomOpacityFade(
    bottomFadeHeightPx: Float,
    contentAlpha: Float = 1f
): Modifier = this
    .graphicsLayer {
        alpha = contentAlpha.coerceIn(0f, 1f)
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        if (size.height <= 0f) return@drawWithContent
        val fadeStart = (size.height - bottomFadeHeightPx).coerceAtLeast(0f)
        val opaqueStop = (fadeStart / size.height).coerceIn(0f, 1f)
        val stop1 = (opaqueStop + (1f - opaqueStop) * 0.25f).coerceIn(0f, 1f)
        val stop2 = (opaqueStop + (1f - opaqueStop) * 0.50f).coerceIn(0f, 1f)
        val stop3 = (opaqueStop + (1f - opaqueStop) * 0.75f).coerceIn(0f, 1f)
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.White,
                    opaqueStop to Color.White,
                    stop1 to Color.White.copy(alpha = 0.75f),
                    stop2 to Color.White.copy(alpha = 0.50f),
                    stop3 to Color.White.copy(alpha = 0.25f),
                    1f to Color.Transparent
                )
            ),
            blendMode = BlendMode.DstIn
        )
    }

@Composable
private fun AlbumDetailHero(
    detail: AlbumDetailInfo,
    coverBitmap: Bitmap?,
    backgroundColor: Color,
    contentColor: Color,
    coverRefreshVersion: Int,
    headBarHeightPx: Float,
    onTitleCollapsedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var videoCoverPath by remember(detail.name, detail.coverAudio?.path, coverRefreshVersion) { mutableStateOf<String?>(null) }
    var videoCoverResolved by remember(detail.name, detail.coverAudio?.path, coverRefreshVersion) { mutableStateOf(false) }
    val useVideoCover = videoCoverResolved && !videoCoverPath.isNullOrBlank()
    val bottomFadeHeightPx = remember(density, useVideoCover) {
        with(density) {
            if (useVideoCover) 240.dp.toPx() else 180.dp.toPx()
        }
    }
    val heroHeightModifier = if (!videoCoverResolved || useVideoCover) {
        Modifier.height(456.dp)
    } else {
        Modifier.aspectRatio(1f)
    }
    val overlayGradientHeight = if (!videoCoverResolved || useVideoCover) 220.dp else 184.dp
    val contentBottomPadding = if (!videoCoverResolved || useVideoCover) 30.dp else 26.dp
    val titleCollapseThresholdPx = with(density) { 8.dp.toPx() }

    LaunchedEffect(detail.name, detail.coverAudio?.path, coverRefreshVersion) {
        videoCoverResolved = false
        videoCoverPath = withContext(Dispatchers.IO) {
            getVideoCoverPath(
                context = context,
                audioPath = detail.coverAudio?.path,
                albumName = detail.name
            )
        }
        videoCoverResolved = true
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(heroHeightModifier)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            useVideoCover -> {
                AlbumVideoCover(
                    videoPath = videoCoverPath.orEmpty(),
                    bottomFadeHeightPx = bottomFadeHeightPx,
                    modifier = Modifier.fillMaxSize()
                )
            }
            coverBitmap != null -> {
                Image(
                    bitmap = coverBitmap!!.asImageBitmap(),
                    contentDescription = "专辑封面",
                    modifier = Modifier
                        .fillMaxSize()
                        .albumCoverBottomOpacityFade(bottomFadeHeightPx),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                    contentDescription = "专辑封面",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(overlayGradientHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.32f),
                            backgroundColor.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 24.dp, top = 26.dp, end = 24.dp, bottom = contentBottomPadding)
        ) {
            Text(
                text = detail.name,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val titleTop = coordinates.boundsInWindow().top
                    onTitleCollapsedChange(titleTop <= headBarHeightPx + titleCollapseThresholdPx)
                },
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (detail.albumArtist.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detail.albumArtist,
                    fontSize = 16.sp,
                    color = contentColor.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${detail.songCount} 首歌曲",
                fontSize = 15.sp,
                color = contentColor.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumVideoCover(
    videoPath: String,
    bottomFadeHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasRenderedFirstFrame by remember(videoPath) { mutableStateOf(false) }
    val renderedFrameAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "albumVideoFirstFrameFadeIn"
    )
    val player = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            setAudioAttributes(AudioAttributes.DEFAULT, false)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_PAUSE -> player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(videoPath) {
        hasRenderedFirstFrame = false
        val file = File(videoPath)
        if (!file.exists()) {
            player.clearMediaItems()
            return@LaunchedEffect
        }
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.playWhenReady = true
        player.play()
    }

    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx)
                .inflate(R.layout.view_lyric_preview_video_cover_player, null, false) as PlayerView).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.player = player
            }
        },
        update = { playerView ->
            playerView.player = player
        },
        modifier = modifier
            .fillMaxSize()
            .albumCoverBottomOpacityFade(
                bottomFadeHeightPx = bottomFadeHeightPx,
                contentAlpha = renderedFrameAlpha
            )
    )
}

@Composable
private fun AlbumDetailLandscapePanel(
    detail: AlbumDetailInfo,
    coverBitmap: Bitmap?,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val bitmap = coverBitmap
        val coverRatio = remember(bitmap) {
            if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.62f, 1.85f)
            } else {
                1f
            }
        }
        val maxCoverWidth = maxWidth * 0.82f
        val maxCoverHeight = maxHeight * 0.54f
        val desiredWidth = maxCoverHeight * coverRatio
        val coverWidth = if (desiredWidth > maxCoverWidth) maxCoverWidth else desiredWidth
        val coverHeight = coverWidth / coverRatio
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = 0.0001f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(coverWidth)
                        .height(coverHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "专辑封面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                            contentDescription = "专辑封面",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = detail.name,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (detail.albumArtist.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detail.albumArtist,
                        fontSize = 15.sp,
                        color = contentColor.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${detail.songCount} 首歌曲 · ${formatAlbumTotalDurationMinutes(detail.totalDurationMs)}",
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AlbumDetailSongItem(
    songInfo: AlbumSongInfo,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    showArtist: Boolean,
    modifier: Modifier = Modifier
) {
    val audio = songInfo.audio
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = songInfo.displayTrackNumber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(34.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.displayTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showArtist) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = songInfo.artistName,
                    fontSize = 13.sp,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                contentDescription = "更多",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AlbumDetailFooter(
    detail: AlbumDetailInfo,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${detail.songCount} 首歌曲 · ${formatAlbumTotalDurationMinutes(detail.totalDurationMs)}",
            fontSize = 12.sp,
            color = contentColor.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (detail.releaseDateInfo.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail.releaseDateInfo,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = contentColor.copy(alpha = 0.66f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (detail.copyrightInfo.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail.copyrightInfo,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = contentColor.copy(alpha = 0.62f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistDetailHero(
    detail: ArtistDetailInfo,
    avatarRefreshVersion: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val heroHeight = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 360.dp else 240.dp
    val targetSizePx = with(density) { heroHeight.toPx().toInt().coerceAtLeast(240) }
    val avatarCacheKey = remember(detail.name, detail.firstAudio?.path, detail.firstAudio?.coverCachePath, avatarRefreshVersion) {
        buildArtistAvatarCacheKey(detail.name, detail.firstAudio, avatarRefreshVersion)
    }
    var coverBitmap by remember(avatarCacheKey) {
        mutableStateOf(getCachedArtistAvatarBitmap(avatarCacheKey))
    }
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(avatarCacheKey) {
        if (coverBitmap != null) return@LaunchedEffect
        val loadedBitmap = withContext(Dispatchers.IO) {
            loadArtistAvatarBitmap(
                context = context,
                artist = ArtistInfo(
                    name = detail.name,
                    songCount = detail.songCount,
                    albumCount = detail.albumCount,
                    firstAudio = detail.firstAudio
                ),
                targetSizePx = targetSizePx,
                preferEmbeddedCover = true
            )
        }
        coverBitmap = loadedBitmap
        loadedBitmap?.let { putCachedArtistAvatarBitmap(avatarCacheKey, it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = coverBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "艺术家头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = detail.name.trim().firstOrNull()?.toString() ?: "艺",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(178.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.82f),
                            backgroundColor
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Text(
                text = detail.name,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${detail.songCount} 首歌曲 · ${detail.albumCount} 张专辑",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistDetailLandscapePanel(
    detail: ArtistDetailInfo,
    avatarRefreshVersion: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetSizePx = with(density) { 360.dp.toPx().toInt().coerceAtLeast(240) }
    val avatarCacheKey = remember(detail.name, detail.firstAudio?.path, detail.firstAudio?.coverCachePath, avatarRefreshVersion) {
        buildArtistAvatarCacheKey(detail.name, detail.firstAudio, avatarRefreshVersion)
    }
    var coverBitmap by remember(avatarCacheKey) {
        mutableStateOf(getCachedArtistAvatarBitmap(avatarCacheKey))
    }
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(avatarCacheKey) {
        if (coverBitmap != null) return@LaunchedEffect
        val loadedBitmap = withContext(Dispatchers.IO) {
            loadArtistAvatarBitmap(
                context = context,
                artist = ArtistInfo(
                    name = detail.name,
                    songCount = detail.songCount,
                    albumCount = detail.albumCount,
                    firstAudio = detail.firstAudio
                ),
                targetSizePx = targetSizePx,
                preferEmbeddedCover = true
            )
        }
        coverBitmap = loadedBitmap
        loadedBitmap?.let { putCachedArtistAvatarBitmap(avatarCacheKey, it) }
    }

    BoxWithConstraints(modifier = modifier) {
        val bitmap = coverBitmap
        val coverRatio = remember(bitmap) {
            if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.62f, 1.85f)
            } else {
                1f
            }
        }
        val maxCoverWidth = maxWidth * 0.82f
        val maxCoverHeight = maxHeight * 0.54f
        val desiredWidth = maxCoverHeight * coverRatio
        val coverWidth = if (desiredWidth > maxCoverWidth) maxCoverWidth else desiredWidth
        val coverHeight = coverWidth / coverRatio
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = 0.0001f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(coverWidth)
                        .height(coverHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "艺术家头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = detail.name.trim().firstOrNull()?.toString() ?: "艺",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = detail.name,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${detail.songCount} 首歌曲 · ${detail.albumCount} 张专辑",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TransparentArtistHeadBar(
    onBack: () -> Unit,
    contentColor: Color,
    backgroundColor: Color = Color.Transparent,
    title: String = "",
    titleAlpha: Float = 0f,
    menuItems: List<MenuItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var menuExpanded by remember { mutableStateOf(false) }
    var menuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .height(50.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                contentDescription = "返回",
                tint = contentColor
            )
        }
        if (title.isNotBlank()) {
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 64.dp)
                    .graphicsLayer {
                        alpha = titleAlpha.coerceIn(0f, 1f)
                    },
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (menuItems.isNotEmpty()) {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        menuButtonPosition = MenuAnchorPosition(
                            x = with(density) { bounds.right.toDp().value },
                            y = with(density) { bounds.bottom.toDp().value }
                        )
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_more_vert_24),
                    contentDescription = "更多",
                    tint = contentColor
                )
            }
            CustomDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = menuItems,
                anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f)
            )
        }
    }
}

private class ArtistImagePickerController(
    val launchPicker: () -> Unit,
    val renderDialogs: @Composable () -> Unit
)

private class AlbumVideoPickerController(
    val launchPicker: () -> Unit,
    val renderDialogs: @Composable () -> Unit
)

private data class MediaSaveDialogState(
    val title: String,
    val message: String,
    val sourceUri: Uri? = null,
    val deleteButtonText: String = "",
    val deleteWarningText: String = ""
)

@Composable
private fun rememberArtistImagePickerController(
    artistName: String,
    onSaved: () -> Unit
): ArtistImagePickerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialogState by remember { mutableStateOf<MediaSaveDialogState?>(null) }
    var pendingSourceImageUri by remember { mutableStateOf<Uri?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        when {
            result.resultCode == android.app.Activity.RESULT_OK && data != null -> {
                val outputUri = com.yalantis.ucrop.UCrop.getOutput(data)
                if (outputUri == null) {
                    pendingSourceImageUri = null
                    dialogState = MediaSaveDialogState(
                        title = "保存失败",
                        message = "未读取到裁剪后的图片。"
                    )
                    return@rememberLauncherForActivityResult
                }
                scope.launch {
                    val saveResult = saveArtistAvatarFromCroppedUri(
                        context = context,
                        artistName = artistName,
                        croppedUri = outputUri
                    )
                    if (saveResult.success) {
                        val sourceUri = pendingSourceImageUri
                        pendingSourceImageUri = null
                        onSaved()
                        dialogState = MediaSaveDialogState(
                            title = "艺术家图片已保存",
                            message = "已保存到 ${saveResult.savedPath.orEmpty()}",
                            sourceUri = sourceUri,
                            deleteButtonText = "删除刚刚选择的源图片",
                            deleteWarningText = "红色按钮会尝试删除你刚刚选择的源图片，不会删除已保存到 Music/.artists/ 的艺术家图片。"
                        )
                    } else {
                        pendingSourceImageUri = null
                        dialogState = MediaSaveDialogState(
                            title = "保存失败",
                            message = saveResult.errorMessage ?: "无法保存艺术家图片。"
                        )
                    }
                }
            }
            result.resultCode == com.yalantis.ucrop.UCrop.RESULT_ERROR && data != null -> {
                pendingSourceImageUri = null
                val error = com.yalantis.ucrop.UCrop.getError(data)
                dialogState = MediaSaveDialogState(
                    title = "裁剪失败",
                    message = error?.message ?: "图片裁剪时发生错误。"
                )
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingSourceImageUri = uri
        persistPickedSourceUriPermission(context, uri)
        val destinationUri = Uri.fromFile(
            File(context.cacheDir, "artist_avatar_${System.currentTimeMillis()}.png")
        )
        val options = com.yalantis.ucrop.UCrop.Options().apply {
            setToolbarTitle("裁剪艺术家图片")
            setCompressionFormat(Bitmap.CompressFormat.PNG)
            setCompressionQuality(100)
            setFreeStyleCropEnabled(true)
            val primaryColor = android.graphics.Color.parseColor("#6650a4")
            setToolbarColor(primaryColor)
            setToolbarWidgetColor(android.graphics.Color.WHITE)
            setActiveControlsWidgetColor(primaryColor)
        }
        val cropIntent = com.yalantis.ucrop.UCrop.of(uri, destinationUri)
            .withOptions(options)
            .getIntent(context)
        cropLauncher.launch(cropIntent)
    }

    return ArtistImagePickerController(
        launchPicker = { imagePickerLauncher.launch(arrayOf("image/*")) },
        renderDialogs = {
            MediaSaveResultDialog(
                state = dialogState,
                onDismiss = { dialogState = null },
                onDeleteSource = { sourceUri ->
                    scope.launch {
                        val deleteResult = deleteSelectedSourceMedia(context, sourceUri)
                        dialogState = MediaSaveDialogState(
                            title = if (deleteResult.success) "源图片已删除" else "源图片删除失败",
                            message = if (deleteResult.success) {
                                "已删除刚刚选择的源图片。已保存的艺术家图片不会被删除。"
                            } else {
                                deleteResult.errorMessage ?: "无法删除刚刚选择的源图片。"
                            }
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun rememberAlbumVideoPickerController(
    albumName: String,
    audioPath: String?,
    onSaved: () -> Unit
): AlbumVideoPickerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialogState by remember { mutableStateOf<MediaSaveDialogState?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        persistPickedSourceUriPermission(context, uri)
        scope.launch {
            val safeAudioPath = audioPath?.trim().orEmpty()
            if (safeAudioPath.isBlank()) {
                dialogState = MediaSaveDialogState(
                    title = "保存失败",
                    message = "无法确定专辑中的音频文件。"
                )
                return@launch
            }
            val safeAlbumName = albumName.trim()
            val result = saveVideoCover(
                context = context,
                audioPath = safeAudioPath,
                videoUri = uri,
                albumName = safeAlbumName
            )
            if (result.success) {
                val savedPath = getVideoCoverPath(
                    context = context,
                    audioPath = safeAudioPath,
                    albumName = safeAlbumName
                )
                onSaved()
                dialogState = MediaSaveDialogState(
                    title = "视频封面已保存",
                    message = "已保存到 ${savedPath.orEmpty()}",
                    sourceUri = uri,
                    deleteButtonText = "删除刚刚选择的源视频",
                    deleteWarningText = "红色按钮会尝试删除你刚刚选择的源视频，不会删除已保存到 Music/.album_video/ 的视频封面。"
                )
            } else {
                dialogState = MediaSaveDialogState(
                    title = "保存失败",
                    message = result.errorMessage ?: "缺少存储权限或无法保存视频封面。"
                )
            }
        }
    }

    return AlbumVideoPickerController(
        launchPicker = { videoPickerLauncher.launch(arrayOf("video/*")) },
        renderDialogs = {
            MediaSaveResultDialog(
                state = dialogState,
                onDismiss = { dialogState = null },
                onDeleteSource = { sourceUri ->
                    scope.launch {
                        val deleteResult = deleteSelectedSourceMedia(context, sourceUri)
                        dialogState = MediaSaveDialogState(
                            title = if (deleteResult.success) "源视频已删除" else "源视频删除失败",
                            message = if (deleteResult.success) {
                                "已删除刚刚选择的源视频。已保存的视频封面不会被删除。"
                            } else {
                                deleteResult.errorMessage ?: "无法删除刚刚选择的源视频。"
                            }
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun MediaSaveResultDialog(
    state: MediaSaveDialogState?,
    onDismiss: () -> Unit,
    onDeleteSource: (Uri) -> Unit
) {
    val dialogState = state ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogState.title) },
        text = {
            Column {
                Text(dialogState.message)
                if (dialogState.sourceUri != null && dialogState.deleteWarningText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = dialogState.deleteWarningText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        },
        dismissButton = dialogState.sourceUri?.let { sourceUri ->
            {
                TextButton(onClick = { onDeleteSource(sourceUri) }) {
                    Text(
                        text = dialogState.deleteButtonText,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun ArtistDetailTabRow(
    selectedTab: ArtistDetailTab,
    onTabSelected: (ArtistDetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.toPagerPage(),
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = {},
        divider = {}
    ) {
        artistDetailPagerTabs.forEach { tab ->
            val selected = selectedTab == tab
            val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            val textColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Tab(
                selected = selected,
                onClick = { onTabSelected(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 1.2.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (tab) {
                                ArtistDetailTab.SONGS -> "歌曲"
                                ArtistDetailTab.ALBUMS -> "专辑"
                            },
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ArtistDetailSongItem(
    audio: AudioFile,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AudioCoverImage(
            audio = audio,
            contentDescription = "歌曲封面",
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.displayTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = audio.displayArtist,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = audio.displayAlbum,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ArtistDetailAlbumItem(
    album: ArtistAlbumInfo,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        album.coverAudio?.let { coverAudio ->
            AudioCoverImage(
                audio = coverAudio,
                contentDescription = "专辑封面",
                modifier = Modifier.size(64.dp)
            )
        } ?: Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                contentDescription = "专辑",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${album.songs.size} 首歌曲",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = album.songs.joinToString("、") { it.displayTitle },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AudioCoverImage(
    audio: AudioFile,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetSizePx = with(density) { 64.dp.toPx().toInt().coerceAtLeast(64) }
    var coverBitmap by remember(audio.path, audio.coverCachePath, audio.lastModified) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(audio.path, audio.coverCachePath, audio.lastModified) {
        coverBitmap = withContext(Dispatchers.IO) {
            loadAudioCoverBitmap(context, audio, targetSizePx)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = coverBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ArtistSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(Modifier.focusRequester(focusRequester)),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索艺术家",
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ArtistListItem(
    artist: ArtistInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistAvatar(
            artist = artist,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = artist.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${artist.songCount} 首歌曲 · ${artist.albumCount} 张专辑",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistAvatar(
    artist: ArtistInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetSizePx = with(density) { 56.dp.toPx().toInt().coerceAtLeast(56) }
    var avatarBitmap by remember(artist.name, artist.firstAudio?.path, artist.firstAudio?.coverCachePath) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(artist.name, artist.firstAudio?.path, artist.firstAudio?.coverCachePath) {
        avatarBitmap = withContext(Dispatchers.IO) {
            loadArtistAvatarBitmap(context, artist, targetSizePx)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = avatarBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "艺术家头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = artist.name.trim().firstOrNull()?.toString() ?: "艺",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyArtistsState(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildArtistInfos(
    audioFiles: List<AudioFile>,
    artistSplitWhitelist: Collection<String>
): List<ArtistInfo> {
    val titleComparator = audioTitleComparator()
    val accumulators = linkedMapOf<String, ArtistAccumulator>()

    audioFiles.sortedWith(titleComparator).forEach { audio ->
        val artistNames = artistNamesForAudio(audio, artistSplitWhitelist)
        artistNames.forEach { artistName ->
            val key = artistName.lowercase(Locale.ROOT)
            val accumulator = accumulators.getOrPut(key) {
                ArtistAccumulator(name = artistName, firstAudio = audio)
            }
            accumulator.songPaths.add(audio.path)
            accumulator.albumKeys.add(audio.displayAlbum.lowercase(Locale.ROOT))
            if (accumulator.firstAudio == null) {
                accumulator.firstAudio = audio
            }
        }
    }

    return accumulators.values.map { accumulator ->
        ArtistInfo(
            name = accumulator.name,
            songCount = accumulator.songPaths.size,
            albumCount = accumulator.albumKeys.size,
            firstAudio = accumulator.firstAudio
        )
    }
}

private fun buildArtistDetailInfo(
    audioFiles: List<AudioFile>,
    artistName: String,
    artistSplitWhitelist: Collection<String>
): ArtistDetailInfo? {
    val normalizedName = artistName.trim()
    if (normalizedName.isEmpty()) return null

    val titleComparator = audioTitleComparator()
    val songs = audioFiles
        .filter { audio -> audioMatchesArtist(audio, normalizedName, artistSplitWhitelist) }
        .sortedWith(titleComparator)
    if (songs.isEmpty()) return null

    val albums = songs
        .groupBy { it.displayAlbum }
        .map { (albumName, albumSongs) ->
            val sortedSongs = albumSongs.sortedWith(titleComparator)
            ArtistAlbumInfo(
                name = albumName,
                songs = sortedSongs,
                coverAudio = sortedSongs.firstOrNull()
            )
        }
        .sortedWith(
            compareBy<ArtistAlbumInfo>(
                { buildFileNameSortBucket(it.name) },
                { buildFileNameSortKey(it.name) },
                { it.name.lowercase(Locale.ROOT) }
            )
        )

    return ArtistDetailInfo(
        name = songs
            .flatMap { artistNamesForAudio(it, artistSplitWhitelist) }
            .firstOrNull { it.equals(normalizedName, ignoreCase = true) }
            ?: normalizedName,
        songs = songs,
        albums = albums
    )
}

private fun buildAlbumDetailInfo(
    context: Context,
    audioFiles: List<AudioFile>,
    albumName: String
): AlbumDetailInfo? {
    val normalizedName = albumName.trim()
    if (normalizedName.isEmpty()) return null

    val titleComparator = audioTitleComparator()
    val albumSongs = audioFiles
        .filter { it.displayAlbum.equals(normalizedName, ignoreCase = true) }
    if (albumSongs.isEmpty()) return null

    val songInfos = albumSongs.map { audio ->
        val metadata = runCatching {
            com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
                context = context,
                filePath = audio.path,
                mediaStoreId = audio.mediaStoreId,
                includeCover = false
            )
        }.getOrNull()
        val resolvedArtistName = metadata
            ?.artist
            .orEmpty()
            .ifBlank { audio.artist }
            .ifBlank { audio.displayArtist }
        AlbumSongInfo(
            audio = audio,
            trackNumber = metadata?.trackNumber.orEmpty(),
            discNumber = metadata?.discNumber.orEmpty(),
            artistName = resolvedArtistName,
            albumArtistName = metadata?.albumArtist.orEmpty(),
            releaseDateRaw = metadata?.year.orEmpty(),
            displayTrackNumber = ""
        )
    }
    val sortedSongs = songInfos
        .sortedWith(
            compareBy<AlbumSongInfo>(
                { parseAlbumDiscSortValue(it.discNumber) },
                { parseAlbumTrackSortValue(it.trackNumber) },
                { buildFileNameSortBucket(it.audio.displayTitle) },
                { buildFileNameSortKey(it.audio.displayTitle) },
                { it.audio.displayTitle.lowercase(Locale.ROOT) }
            ).then(titleComparatorByAlbumSong())
        )
        .mapIndexed { index, songInfo ->
            songInfo.copy(
                displayTrackNumber = normalizedTrackNumberLabel(songInfo.trackNumber)
                    .ifBlank { (index + 1).toString() }
            )
        }
    val albumArtistName = sortedSongs
        .firstOrNull()
        ?.albumArtistName
        .orEmpty()
        .ifBlank { sortedSongs.firstOrNull()?.artistName.orEmpty() }

    return AlbumDetailInfo(
        name = sortedSongs
            .firstOrNull()
            ?.audio
            ?.displayAlbum
            ?: normalizedName,
        songs = sortedSongs,
        albumArtist = albumArtistName,
        releaseDateInfo = formatAlbumReleaseDateInfo(sortedSongs.firstOrNull()?.releaseDateRaw.orEmpty()),
        copyrightInfo = sortedSongs.firstOrNull()?.audio?.let { readAudioCopyrightInfo(it) }.orEmpty()
    )
}

private fun audioTitleComparator(): Comparator<AudioFile> {
    return compareBy(
        { buildFileNameSortBucket(it.displayTitle) },
        { buildFileNameSortKey(it.displayTitle) },
        { it.displayTitle.lowercase(Locale.ROOT) }
    )
}

private fun titleComparatorByAlbumSong(): Comparator<AlbumSongInfo> {
    return compareBy(
        { buildFileNameSortBucket(it.audio.displayTitle) },
        { buildFileNameSortKey(it.audio.displayTitle) },
        { it.audio.displayTitle.lowercase(Locale.ROOT) }
    )
}

private fun artistNamesForAudio(
    audio: AudioFile,
    artistSplitWhitelist: Collection<String>
): List<String> {
    return splitArtistNames(audio.artist, artistSplitWhitelist)
        .ifEmpty { listOf(audio.displayArtist) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.ROOT) }
}

private fun audioMatchesArtist(
    audio: AudioFile,
    artistName: String,
    artistSplitWhitelist: Collection<String>
): Boolean {
    return artistNamesForAudio(audio, artistSplitWhitelist).any { it.equals(artistName, ignoreCase = true) }
}

private fun parseAlbumTrackSortValue(trackRaw: String): Int {
    if (trackRaw.isBlank()) return Int.MAX_VALUE
    val firstPart = trackRaw.substringBefore("/").trim()
    return firstPart.toIntOrNull()
        ?: Regex("""\d+""").find(firstPart)?.value?.toIntOrNull()
        ?: Int.MAX_VALUE
}

private fun parseAlbumDiscSortValue(discRaw: String): Int {
    if (discRaw.isBlank()) return 0
    val firstPart = discRaw.substringBefore("/").trim()
    return firstPart.toIntOrNull()
        ?: Regex("""\d+""").find(firstPart)?.value?.toIntOrNull()
        ?: 0
}

private fun normalizedTrackNumberLabel(trackRaw: String): String {
    val label = trackRaw.substringBefore("/").trim()
    return label.takeIf { it.isNotEmpty() }
        ?: Regex("""\d+""").find(trackRaw)?.value.orEmpty()
}

private fun shouldShowAlbumSongArtist(
    albumArtist: String,
    songArtist: String,
    artistSplitWhitelist: Collection<String>
): Boolean {
    val normalizedSongArtist = songArtist.trim()
    if (normalizedSongArtist.isEmpty()) return false
    val albumArtists = splitArtistNames(albumArtist, artistSplitWhitelist)
        .ifEmpty { albumArtist.trim().takeIf { it.isNotEmpty() }?.let { listOf(it) } ?: emptyList() }
    val songArtists = splitArtistNames(normalizedSongArtist, artistSplitWhitelist)
        .ifEmpty { listOf(normalizedSongArtist) }
    return !(albumArtists.size == 1 &&
        songArtists.size == 1 &&
        albumArtists.first().equals(songArtists.first(), ignoreCase = true))
}

private fun formatAlbumTotalDurationMinutes(durationMs: Long): String {
    if (durationMs <= 0L) return "0 分钟"
    val minutes = ((durationMs + 59_999L) / 60_000L).coerceAtLeast(1L)
    return "$minutes 分钟"
}

private fun formatAlbumReleaseDateInfo(rawDate: String): String {
    val raw = rawDate.trim()
    if (raw.isEmpty()) return ""
    val compactDate = Regex("""^(\d{4})(\d{2})(\d{2})$""").matchEntire(raw)
    if (compactDate != null) {
        val year = compactDate.groupValues[1]
        val month = compactDate.groupValues[2].toIntOrNull()
        val day = compactDate.groupValues[3].toIntOrNull()
        if (month in 1..12 && day in 1..31) {
            return "${year}年${month}月${day}日"
        }
    }
    val normalized = raw
        .substringBefore('T')
        .substringBefore(' ')
        .replace('/', '-')
        .replace('.', '-')
        .replace("年", "-")
        .replace("月", "-")
        .replace("日", "")
    val dateMatch = Regex("""^(\d{4})(?:-(\d{1,2})(?:-(\d{1,2}))?)?""").find(normalized)
        ?: return raw
    val year = dateMatch.groupValues[1]
    val month = dateMatch.groupValues.getOrNull(2).orEmpty().toIntOrNull()
    val day = dateMatch.groupValues.getOrNull(3).orEmpty().toIntOrNull()
    return when {
        month in 1..12 && day in 1..31 -> "${year}年${month}月${day}日"
        month in 1..12 -> "${year}年${month}月"
        else -> "${year}年"
    }
}

private fun readAudioCopyrightInfo(audio: AudioFile): String {
    return runCatching {
        val pfd = android.os.ParcelFileDescriptor.open(
            File(audio.path),
            android.os.ParcelFileDescriptor.MODE_READ_ONLY
        )
        val nativeFd = pfd.dup().detachFd()
        val metadata = com.lonx.audiotag.TagLib.getMetadata(nativeFd, false)
        pfd.close()
        val props = metadata?.propertyMap ?: return@runCatching ""

        fun firstOf(vararg keys: String): String {
            keys.forEach { key ->
                val value = props[key]
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (value.isNotEmpty()) return value
            }
            return ""
        }

        firstOf("COPYRIGHT", "COPYRIGHTS", "COPYRIGHTINFO")
    }.onFailure {
        Log.e("ArtistsActivity", "Error reading copyright for ${audio.path}", it)
    }.getOrDefault("")
}

private fun saveArtistFavoritePaths(context: Context, paths: Set<String>) {
    val entries = paths.map { path ->
        LocalPlaylistEntry(
            path = path,
            title = File(path).nameWithoutExtension,
            artist = "",
            durationSeconds = -1L
        )
    }
    LocalPlaylistStore.saveFavorites(context, entries)
}

private fun sortArtistInfos(
    artists: List<ArtistInfo>,
    sortType: ArtistSortType
): List<ArtistInfo> {
    val nameComparator = compareBy<ArtistInfo>(
        { buildFileNameSortBucket(it.name) },
        { buildFileNameSortKey(it.name) },
        { it.name.lowercase(Locale.ROOT) }
    )
    return when (sortType) {
        ArtistSortType.NAME -> artists.sortedWith(nameComparator)
        ArtistSortType.SONG_COUNT -> artists.sortedWith(
            compareByDescending<ArtistInfo> { it.songCount }.then(nameComparator)
        )
        ArtistSortType.ALBUM_COUNT -> artists.sortedWith(
            compareByDescending<ArtistInfo> { it.albumCount }.then(nameComparator)
        )
    }
}

private fun splitArtistNames(
    raw: String,
    artistSplitWhitelist: Collection<String>
): List<String> {
    return ArtistNameSplitter.split(raw, artistSplitWhitelist)
}

private fun buildArtistDetailCacheKey(
    artistName: String,
    audioFingerprint: Long,
    artistSplitWhitelistFingerprint: String
): String {
    return "${artistName.trim().lowercase(Locale.ROOT)}#$audioFingerprint#$artistSplitWhitelistFingerprint"
}

private fun buildArtistAvatarCacheKey(
    artistName: String,
    firstAudio: AudioFile?,
    refreshVersion: Int
): String {
    val artistAvatarFile = findArtistAvatarFile(artistName)
    return buildString {
        append(artistName.trim().lowercase(Locale.ROOT))
        append('#')
        append(artistAvatarFile?.absolutePath.orEmpty())
        append('#')
        append(artistAvatarFile?.lastModified() ?: 0L)
        append('#')
        append(artistAvatarFile?.length() ?: 0L)
        append('#')
        append(firstAudio?.path.orEmpty())
        append('#')
        append(firstAudio?.coverCachePath.orEmpty())
        append('#')
        append(firstAudio?.lastModified ?: 0L)
        append('#')
        append(refreshVersion)
    }
}

private fun getCachedArtistAvatarBitmap(cacheKey: String): Bitmap? {
    return artistAvatarBitmapCache.get(cacheKey)
}

private fun putCachedArtistAvatarBitmap(cacheKey: String, bitmap: Bitmap) {
    artistAvatarBitmapCache.put(cacheKey, bitmap)
}

private fun clearArtistAvatarBitmapCache() {
    artistAvatarBitmapCache.evictAll()
}

private fun resolveArtistAvatarDirectory(): File {
    return File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        ".artists"
    )
}

private suspend fun saveArtistAvatarFromCroppedUri(
    context: Context,
    artistName: String,
    croppedUri: Uri
): ArtistAvatarSaveResult {
    return withContext(Dispatchers.IO) {
        try {
            val safeArtistName = artistName.trim()
            if (safeArtistName.isBlank()) {
                return@withContext ArtistAvatarSaveResult(false, errorMessage = "艺术家名称为空。")
            }
            if (!hasStoragePermission(context)) {
                return@withContext ArtistAvatarSaveResult(false, errorMessage = "缺少存储权限。")
            }
            if (containsIllegalFileNameChars(safeArtistName)) {
                val illegalChars = getIllegalChars(safeArtistName)
                return@withContext ArtistAvatarSaveResult(false, errorMessage = "存在非法字符\"$illegalChars\"，无法保存。")
            }

            val artistDir = resolveArtistAvatarDirectory()
            if (!artistDir.exists() && !artistDir.mkdirs()) {
                return@withContext ArtistAvatarSaveResult(
                    false,
                    errorMessage = "无法创建艺术家图片目录：${artistDir.absolutePath}"
                )
            }

            val destinationFile = File(artistDir, "$safeArtistName.png")
            context.contentResolver.openInputStream(croppedUri)?.use { input ->
                java.io.FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext ArtistAvatarSaveResult(false, errorMessage = "无法读取裁剪后的图片。")

            clearArtistAvatarBitmapCache()
            ArtistAvatarSaveResult(
                success = true,
                savedPath = destinationFile.absolutePath
            )
        } catch (e: Exception) {
            Log.e("ArtistsActivity", "Error saving artist avatar for $artistName", e)
            ArtistAvatarSaveResult(false, errorMessage = e.message)
        }
    }
}

private fun persistPickedSourceUriPermission(context: Context, sourceUri: Uri) {
    if (!sourceUri.scheme.equals("content", ignoreCase = true)) return
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            sourceUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }.recoverCatching {
        context.contentResolver.takePersistableUriPermission(
            sourceUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

private suspend fun deleteSelectedSourceMedia(
    context: Context,
    sourceUri: Uri
): SourceMediaDeleteResult = withContext(Dispatchers.IO) {
    try {
        if (sourceUri.scheme.equals("content", ignoreCase = true)) {
            val documentDeleted = runCatching {
                DocumentFile.fromSingleUri(context, sourceUri)?.delete() == true
            }.getOrDefault(false)
            if (documentDeleted) {
                return@withContext SourceMediaDeleteResult(success = true)
            }

            val deletedCount = context.contentResolver.delete(sourceUri, null, null)
            return@withContext if (deletedCount > 0) {
                SourceMediaDeleteResult(success = true)
            } else {
                SourceMediaDeleteResult(
                    success = false,
                    errorMessage = "无法删除刚刚选择的源文件，文件提供方未授权删除或不支持删除。"
                )
            }
        }

        if (sourceUri.scheme.equals("file", ignoreCase = true) || sourceUri.scheme.isNullOrBlank()) {
            val filePath = sourceUri.path
            if (filePath.isNullOrBlank()) {
                return@withContext SourceMediaDeleteResult(
                    success = false,
                    errorMessage = "刚刚选择的源文件路径无效。"
                )
            }
            val sourceFile = File(filePath)
            return@withContext if (!sourceFile.exists() || sourceFile.delete()) {
                SourceMediaDeleteResult(success = true)
            } else {
                SourceMediaDeleteResult(
                    success = false,
                    errorMessage = "无法删除刚刚选择的源文件。"
                )
            }
        }

        SourceMediaDeleteResult(
            success = false,
            errorMessage = "不支持删除该来源的文件。"
        )
    } catch (e: SecurityException) {
        Log.e("ArtistsActivity", "No permission to delete selected source media", e)
        SourceMediaDeleteResult(
            success = false,
            errorMessage = "没有权限删除刚刚选择的源文件。"
        )
    } catch (e: Exception) {
        Log.e("ArtistsActivity", "Error deleting selected source media", e)
        SourceMediaDeleteResult(
            success = false,
            errorMessage = e.message ?: "删除刚刚选择的源文件失败。"
        )
    }
}

private val artistAvatarExtensions = setOf("jpg", "jpeg", "png", "webp")

private fun loadArtistAvatarBitmap(
    context: Context,
    artist: ArtistInfo,
    targetSizePx: Int,
    preferEmbeddedCover: Boolean = false
): Bitmap? {
    findArtistAvatarFile(artist.name)?.let { avatarFile ->
        decodeBitmapFile(avatarFile, targetSizePx, targetSizePx)?.let { return it }
    }

    val firstAudio = artist.firstAudio ?: return null
    return loadAudioCoverBitmap(
        context = context,
        audio = firstAudio,
        targetSizePx = targetSizePx,
        preferEmbeddedCover = preferEmbeddedCover,
        cacheDecoded = !preferEmbeddedCover
    )
}

private fun loadAudioCoverBitmap(
    context: Context,
    audio: AudioFile,
    targetSizePx: Int,
    preferEmbeddedCover: Boolean = false,
    cacheDecoded: Boolean = true
): Bitmap? {
    if (preferEmbeddedCover) {
        readEmbeddedAudioCoverBitmap(context, audio, targetSizePx)?.let { return it }
    }

    val cachePath = audio.coverCachePath
    if (!cachePath.isNullOrBlank() && !MusicLibraryActivity.isNoCoverMarked(cachePath)) {
        if (cacheDecoded) {
            MusicLibraryActivity.getCoverFromCache(cachePath)?.let { return it }
        }
        decodeBitmapFile(File(cachePath), targetSizePx, targetSizePx)?.let { bitmap ->
            if (cacheDecoded) {
                MusicLibraryActivity.putCoverToCache(cachePath, bitmap)
            }
            return bitmap
        }
    }

    return readEmbeddedAudioCoverBitmap(context, audio, targetSizePx)?.also { bitmap ->
        if (cacheDecoded && !cachePath.isNullOrBlank()) {
            MusicLibraryActivity.putCoverToCache(cachePath, bitmap)
        }
    }
}

private fun readEmbeddedAudioCoverBitmap(
    context: Context,
    audio: AudioFile,
    targetSizePx: Int
): Bitmap? {
    return runCatching {
        val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
            context = context,
            filePath = audio.path,
            mediaStoreId = audio.mediaStoreId,
            includeCover = true
        )
        val coverData = metadata.cover ?: return null
        decodeBitmapBytes(coverData, targetSizePx, targetSizePx)
    }.onFailure {
        Log.e("ArtistsActivity", "Error loading audio cover for ${audio.path}", it)
    }.getOrNull()
}

private fun findArtistAvatarFile(artistName: String): File? {
    val targetName = artistName.trim()
    if (targetName.isEmpty()) return null
    val artistDir = resolveArtistAvatarDirectory()
    val files = artistDir.listFiles() ?: return null
    return files.firstOrNull { file ->
        file.isFile &&
            file.extension.lowercase(Locale.ROOT) in artistAvatarExtensions &&
            file.nameWithoutExtension == targetName
    } ?: files.firstOrNull { file ->
        file.isFile &&
            file.extension.lowercase(Locale.ROOT) in artistAvatarExtensions &&
            file.nameWithoutExtension.equals(targetName, ignoreCase = true)
    }
}

private fun decodeBitmapFile(
    file: File,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    if (!file.exists() || !file.isFile) return null
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        options.inJustDecodeBounds = false
        options.inSampleSize = MusicLibraryActivity.calculateInSampleSize(options, reqWidth, reqHeight)
        BitmapFactory.decodeFile(file.absolutePath, options)
    } catch (e: Exception) {
        Log.e("ArtistsActivity", "Error decoding bitmap file: ${file.absolutePath}", e)
        null
    }
}

private fun decodeBitmapBytes(
    bytes: ByteArray,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    if (bytes.isEmpty()) return null
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        options.inJustDecodeBounds = false
        options.inSampleSize = MusicLibraryActivity.calculateInSampleSize(options, reqWidth, reqHeight)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (e: Exception) {
        Log.e("ArtistsActivity", "Error decoding bitmap bytes", e)
        null
    }
}
