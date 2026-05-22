package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.GlobalMiniPlayerBar
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.Dispatchers
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

private enum class ArtistDetailTab {
    SONGS, ALBUMS
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

    LaunchedEffect(Unit) {
        isLoading = true
        val loadedArtists = withContext(Dispatchers.IO) {
            buildArtistInfos(musicLibraryCacheStore(context).loadAllPaged())
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
                                top = 10.dp,
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

    LaunchedEffect(artistName) {
        isLoading = true
        detailInfo = withContext(Dispatchers.IO) {
            buildArtistDetailInfo(
                audioFiles = musicLibraryCacheStore(context).loadAllPaged(),
                artistName = artistName
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
                    onSongClick = { audio ->
                        detailInfo?.songs?.let { songs ->
                            playbackController.playQueue(songs, audio.path)
                        }
                    },
                    onSongMoreClick = { audio ->
                        selectedSongInfoAudio = audio
                        showSongInfoSheet = true
                    }
                )
            }
        }

        TransparentArtistHeadBar(
            onBack = onBack,
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
}

@Composable
private fun ArtistDetailContent(
    detail: ArtistDetailInfo,
    selectedTab: ArtistDetailTab,
    onTabSelected: (ArtistDetailTab) -> Unit,
    miniPlayerExtraBottomPadding: androidx.compose.ui.unit.Dp,
    onSongClick: (AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit
) {
    val configuration = LocalConfiguration.current
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val extraScrollSpace = (configuration.screenHeightDp * 0.45f).dp.coerceAtLeast(220.dp)
    val bottomPadding = navigationBarsPadding.calculateBottomPadding() + 24.dp + miniPlayerExtraBottomPadding

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(modifier = Modifier.fillMaxSize()) {
            ArtistDetailLandscapePanel(
                detail = detail,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ArtistDetailTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected
                    )
                }
                artistDetailListItems(
                    detail = detail,
                    selectedTab = selectedTab,
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick
                )
                item {
                    Spacer(modifier = Modifier.height(extraScrollSpace))
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ArtistDetailHero(detail = detail)
            }
            item {
                ArtistDetailTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            artistDetailListItems(
                detail = detail,
                selectedTab = selectedTab,
                onSongClick = onSongClick,
                onSongMoreClick = onSongMoreClick,
                itemModifier = Modifier.padding(horizontal = 16.dp)
            )
            item {
                Spacer(modifier = Modifier.height(extraScrollSpace))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.artistDetailListItems(
    detail: ArtistDetailInfo,
    selectedTab: ArtistDetailTab,
    onSongClick: (AudioFile) -> Unit,
    onSongMoreClick: (AudioFile) -> Unit,
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
                    modifier = itemModifier
                )
            }
        }
    }
}

@Composable
private fun ArtistDetailHero(
    detail: ArtistDetailInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val heroHeight = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 360.dp else 240.dp
    val targetSizePx = with(density) { heroHeight.toPx().toInt().coerceAtLeast(240) }
    var coverBitmap by remember(detail.name, detail.firstAudio?.path, detail.firstAudio?.coverCachePath) {
        mutableStateOf<Bitmap?>(null)
    }
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(detail.name, detail.firstAudio?.path, detail.firstAudio?.coverCachePath) {
        coverBitmap = withContext(Dispatchers.IO) {
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetSizePx = with(density) { 360.dp.toPx().toInt().coerceAtLeast(240) }
    var coverBitmap by remember(detail.name, detail.firstAudio?.path, detail.firstAudio?.coverCachePath) {
        mutableStateOf<Bitmap?>(null)
    }
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(detail.name, detail.firstAudio?.path, detail.firstAudio?.coverCachePath) {
        coverBitmap = withContext(Dispatchers.IO) {
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
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
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
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.68f),
                            backgroundColor
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Text(
                text = detail.name,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${detail.songCount} 首歌曲 · ${detail.albumCount} 张专辑",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransparentArtistHeadBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(50.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                contentDescription = "返回",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ArtistDetailTabRow(
    selectedTab: ArtistDetailTab,
    onTabSelected: (ArtistDetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ArtistDetailTabButton(
            text = "歌曲",
            selected = selectedTab == ArtistDetailTab.SONGS,
            onClick = { onTabSelected(ArtistDetailTab.SONGS) },
            modifier = Modifier.weight(1f)
        )
        ArtistDetailTabButton(
            text = "专辑",
            selected = selectedTab == ArtistDetailTab.ALBUMS,
            onClick = { onTabSelected(ArtistDetailTab.ALBUMS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ArtistDetailTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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

private fun buildArtistInfos(audioFiles: List<AudioFile>): List<ArtistInfo> {
    val titleComparator = audioTitleComparator()
    val accumulators = linkedMapOf<String, ArtistAccumulator>()

    audioFiles.sortedWith(titleComparator).forEach { audio ->
        val artistNames = artistNamesForAudio(audio)
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
    artistName: String
): ArtistDetailInfo? {
    val normalizedName = artistName.trim()
    if (normalizedName.isEmpty()) return null

    val titleComparator = audioTitleComparator()
    val songs = audioFiles
        .filter { audio -> audioMatchesArtist(audio, normalizedName) }
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
            .flatMap { artistNamesForAudio(it) }
            .firstOrNull { it.equals(normalizedName, ignoreCase = true) }
            ?: normalizedName,
        songs = songs,
        albums = albums
    )
}

private fun audioTitleComparator(): Comparator<AudioFile> {
    return compareBy(
        { buildFileNameSortBucket(it.displayTitle) },
        { buildFileNameSortKey(it.displayTitle) },
        { it.displayTitle.lowercase(Locale.ROOT) }
    )
}

private fun artistNamesForAudio(audio: AudioFile): List<String> {
    return splitArtistNames(audio.artist)
        .ifEmpty { listOf(audio.displayArtist) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.ROOT) }
}

private fun audioMatchesArtist(audio: AudioFile, artistName: String): Boolean {
    return artistNamesForAudio(audio).any { it.equals(artistName, ignoreCase = true) }
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

private fun splitArtistNames(raw: String): List<String> {
    return raw
        .replace("／", "/")
        .replace("；", ";")
        .replace("，", ",")
        .split("/", "&", ";", ",", "、")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
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
    val artistDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        ".artists"
    )
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
