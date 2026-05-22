package com.example.LyricBox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.LyricBox.lyrics.LyricsService
import com.example.LyricBox.lyrics.models.SongInfo
import com.example.LyricBox.lyrics.models.Source
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class AudioMetadataSearchActivity : ComponentActivity() {
    companion object {
        private var tempCoverBitmap: android.graphics.Bitmap? = null
        
        fun getAndClearTempCoverBitmap(): android.graphics.Bitmap? {
            val bitmap = tempCoverBitmap
            tempCoverBitmap = null
            return bitmap
        }
    }
    
    private val cachedCoverFiles = mutableSetOf<java.io.File>()
    private val cacheClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        clearAllCoverCaches()
        
        val autoSearchKeyword = intent.getStringExtra("autoSearchKeyword") ?: ""
        val coverOnly = intent.getBooleanExtra("coverOnly", false)
        
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars
                ) { _ ->
                    AudioMetadataSearchScreen(
                        onBack = { 
                            tempCoverBitmap = null
                            finish() 
                        },
                        onImport = { metadata, selectedFields ->
                            tempCoverBitmap = if ("cover" in selectedFields) metadata.coverBitmap else null
                            val resultIntent = android.content.Intent().apply {
                                putExtra("selectedFields", ArrayList(selectedFields))
                                if ("title" in selectedFields) putExtra("title", metadata.title)
                                if ("artist" in selectedFields) putExtra("artist", metadata.artist)
                                if ("album" in selectedFields) putExtra("album", metadata.album)
                                if ("year" in selectedFields) putExtra("year", metadata.year)
                                if ("trackNumber" in selectedFields) putExtra("trackNumber", metadata.trackNumber)
                                if ("discNumber" in selectedFields) putExtra("discNumber", metadata.discNumber)
                                if ("genre" in selectedFields) putExtra("genre", metadata.genre)
                                if ("albumArtist" in selectedFields) putExtra("albumArtist", metadata.albumArtist)
                                if ("composer" in selectedFields) putExtra("composer", metadata.composer)
                                if ("lyricist" in selectedFields) putExtra("lyricist", metadata.lyricist)
                                if ("comment" in selectedFields) putExtra("comment", metadata.comment)
                                if ("copyright" in selectedFields) putExtra("copyright", metadata.copyright)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        },
                        autoSearchKeyword = autoSearchKeyword,
                        coverOnly = coverOnly,
                        onCacheCovers = { songs ->
                            songs.forEach { song ->
                                getLowResCoverUrl(song)?.let { url ->
                                    cacheCover(this, url)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        clearAllCoverCaches()
    }
    
    private fun clearAllCoverCaches() {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = File(cacheDir, "cover_cache")
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.forEach { file ->
                        try {
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                cachedCoverFiles.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun cacheCover(context: Context, coverUrl: String) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "cover_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                
                val fileName = coverUrl.hashCode().toString() + ".jpg"
                val cacheFile = File(cacheDir, fileName)
                
                if (cacheFile.exists()) {
                    return@launch
                }
                
                val request = Request.Builder().url(coverUrl).build()
                cacheClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.bytes()?.let { bytes ->
                            cacheFile.writeBytes(bytes)
                            cachedCoverFiles.add(cacheFile)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val genre: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val lyricist: String? = null,
    val comment: String? = null,
    val copyright: String? = null,
    val coverBitmap: Bitmap? = null
)

data class MetadataSearchResult(
    val songInfo: SongInfo,
    val metadata: AudioMetadata? = null,
    val coverBitmap: Bitmap? = null,
    val isLoading: Boolean = false
)

data class MetadataSourceGroup(
    val source: Source,
    val sourceName: String,
    val songs: List<MetadataSearchResult>,
    var isExpanded: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMetadataSearchScreen(
    onBack: () -> Unit,
    onImport: (AudioMetadata, Set<String>) -> Unit,
    autoSearchKeyword: String = "",
    coverOnly: Boolean = false,
    onCacheCovers: (List<SongInfo>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lyricsService = remember { LyricsService() }
    val prefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    val client = remember { OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    }
    
    val tabTitles = listOf("推荐结果", "QM", "NE", "AM")
    val tabSources = listOf(null, Source.QM, Source.NE, Source.ITUNES)
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    var searchInput by remember { mutableStateOf(autoSearchKeyword) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var sourceGroups by remember { mutableStateOf<List<MetadataSourceGroup>>(emptyList()) }
    var hasAutoSearched by remember { mutableStateOf(false) }
    
    var selectedSong by remember { mutableStateOf<MetadataSearchResult?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var showHeadbarMenu by remember { mutableStateOf(false) }
    var showAMRegionDialog by remember { mutableStateOf(false) }

    val savedAMRegion = remember { getSavedAMDefaultRegion(context) }
    var currentAMRegion by remember { mutableStateOf(savedAMRegion) }
    var tempAMRegion by remember { mutableStateOf(savedAMRegion) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { 4 }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }
    
    fun getItunesCoverSize(): Int {
        return prefs.getInt("amCoverSize", 3000)
    }
    
    fun getQmCoverSize(): Int {
        return prefs.getInt("qmCoverSize", 1200)
    }
    
    fun getNeCoverSize(): Int {
        return prefs.getInt("neCoverSize", 1000)
    }
    
    fun getItunesCountry(): String {
        return when (currentAMRegion) {
            "HK_SC" -> "HK"
            "HK" -> "HK"
            "TW_SC" -> "TW"
            "TW" -> "TW"
            "CN" -> "CN"
            "JP" -> "JP"
            "KR" -> "KR"
            "US" -> "US"
            else -> "HK"
        }
    }
    
    fun shouldConvertToSimplified(): Boolean {
        return currentAMRegion == "HK_SC" || currentAMRegion == "TW_SC"
    }
    
    fun getArtistSeparator(): String {
        return prefs.getString("artistSeparator", "/") ?: "/"
    }
    
    fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1.isEmpty() || s2.isEmpty()) return 0f
        val norm1 = s1.lowercase().trim()
        val norm2 = s2.lowercase().trim()
        if (norm1 == norm2) return 1f
        
        var matches = 0
        val words1 = norm1.split("\\s+".toRegex())
        val words2 = norm2.split("\\s+".toRegex())
        
        for (word in words1) {
            if (words2.any { it.contains(word) || word.contains(it) }) matches++
        }
        
        return matches.toFloat() / maxOf(words1.size, words2.size).coerceAtLeast(1)
    }
    
    fun removeBrackets(s: String): String {
        var result = s
        val bracketPatterns = listOf(
            "\\([^)]*\\)",
            "\\[[^]]*\\]",
            "\\{[^}]*\\}",
            "【[^】]*】",
            "（[^）]*）"
        )
        for (pattern in bracketPatterns) {
            result = result.replace(pattern.toRegex(), "")
        }
        return result.trim()
    }
    
    fun normalizeArtist(artist: String): String {
        var result = artist.lowercase().trim()
        result = result.replace("tia ray", "")
        result = result.replace("-", "")
        result = result.replace("/", " ")
        result = result.replace("\\s+".toRegex(), " ")
        return result.trim()
    }
    
    fun calculateMaxSimilarity(orig: String, candidate: String): Float {
        val sim1 = calculateSimilarity(orig, candidate)
        val origNoBrackets = removeBrackets(orig)
        val candidateNoBrackets = removeBrackets(candidate)
        val sim2 = calculateSimilarity(origNoBrackets, candidateNoBrackets)
        return maxOf(sim1, sim2)
    }
    
    fun calculateArtistSimilarity(orig: String, candidate: String): Float {
        val origNorm = normalizeArtist(orig)
        val candidateNorm = normalizeArtist(candidate)
        
        if (origNorm.isEmpty() || candidateNorm.isEmpty()) return 1f
        if (origNorm == candidateNorm) return 1f
        
        val sim1 = calculateSimilarity(orig, candidate)
        val sim2 = calculateSimilarity(origNorm, candidateNorm)
        
        val origArtists = orig.split("/").flatMap { it.split("\\s+".toRegex()) }.filter { it.isNotEmpty() }
        val candidateArtists = candidate.split("/").flatMap { it.split("\\s+".toRegex()) }.filter { it.isNotEmpty() }
        
        var matchCount = 0
        for (oa in origArtists) {
            for (ca in candidateArtists) {
                if (calculateMaxSimilarity(oa, ca) >= 0.8f) {
                    matchCount++
                    break
                }
            }
        }
        
        val artistMatchScore = if (origArtists.isNotEmpty()) {
            matchCount.toFloat() / origArtists.size
        } else 0f
        
        return maxOf(sim1, sim2, artistMatchScore)
    }
    
    fun calculateMatchScore(song: SongInfo, keyword: String): Float {
        val candidateTitle = (song.title ?: "").lowercase().trim()
        val candidateArtist = song.artist.joinToString("/").lowercase().trim()
        val origTitle = keyword.lowercase().trim()
        
        val titleSim = calculateMaxSimilarity(origTitle, candidateTitle)
        val artistSim = if (origTitle.isNotEmpty()) calculateArtistSimilarity(origTitle, candidateArtist) else 1f
        
        val titleArtistReversedSim = if (origTitle.isNotEmpty()) {
            val sim1 = calculateMaxSimilarity(origTitle, candidateArtist)
            val sim2 = calculateArtistSimilarity(origTitle, candidateTitle)
            maxOf(sim1, sim2)
        } else 0f
        
        val combinedSim = maxOf(
            (titleSim * 0.6f + artistSim * 0.4f),
            (titleArtistReversedSim * 0.5f + titleSim * 0.3f + artistSim * 0.2f)
        )
        
        return combinedSim
    }
    
    fun getRecommendedResults(keyword: String): List<MetadataSearchResult> {
        val allResults = mutableListOf<Pair<MetadataSearchResult, Float>>()
        
        sourceGroups.forEach { group ->
            group.songs.take(2).forEach { song ->
                val score = calculateMatchScore(song.songInfo, keyword)
                allResults.add(song to score)
            }
        }
        
        return allResults
            .filter { it.second >= 0.5f }
            .sortedByDescending { it.second }
            .map { it.first }
    }
    
    fun performSearch(keyword: String, onSearchResultsLoaded: ((List<SongInfo>) -> Unit)? = null) {
        if (keyword.isBlank()) return
        
        searchError = null
        isSearching = true
        sourceGroups = emptyList()
        
        val coverSize = getItunesCoverSize()
        val qmCoverSize = getQmCoverSize()
        val neCoverSize = getNeCoverSize()
        val country = getItunesCountry()
        val convertToSimplified = shouldConvertToSimplified()
        
        val allSongs = mutableListOf<SongInfo>()
        var qmDone = false
        var neDone = false
        var itunesDone = false
        var hasResults = false
        
        fun checkIfAllDone() {
            if (qmDone && neDone && itunesDone) {
                isSearching = false
                if (!hasResults) {
                    searchError = "未找到结果"
                } else {
                    onSearchResultsLoaded?.invoke(allSongs)
                }
            }
        }
        
        // 搜索 QQ 音乐
        scope.launch {
            try {
                Log.d("AudioMetadataSearch", "开始搜索 QQ 音乐, 封面尺寸: $qmCoverSize")
                val qmSongs = lyricsService.searchFromSource(
                    keyword, 
                    Source.QM,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                if (qmSongs.isNotEmpty()) {
                    allSongs.addAll(qmSongs)
                    hasResults = true
                    sourceGroups = sourceGroups + MetadataSourceGroup(
                        source = Source.QM,
                        sourceName = getMetadataSourceShortName(Source.QM),
                        songs = qmSongs.map { MetadataSearchResult(it, null, null, false) },
                        isExpanded = true
                    )
                }
            } catch (e: Exception) {
                Log.e("AudioMetadataSearch", "QQ 音乐搜索失败", e)
            } finally {
                qmDone = true
                checkIfAllDone()
            }
        }
        
        // 搜索网易云音乐
        scope.launch {
            try {
                Log.d("AudioMetadataSearch", "开始搜索网易云音乐, 封面尺寸: $neCoverSize")
                val neSongs = lyricsService.searchFromSource(
                    keyword, 
                    Source.NE,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                if (neSongs.isNotEmpty()) {
                    allSongs.addAll(neSongs)
                    hasResults = true
                    sourceGroups = sourceGroups + MetadataSourceGroup(
                        source = Source.NE,
                        sourceName = getMetadataSourceShortName(Source.NE),
                        songs = neSongs.map { MetadataSearchResult(it, null, null, false) },
                        isExpanded = true
                    )
                }
            } catch (e: Exception) {
                Log.e("AudioMetadataSearch", "网易云音乐搜索失败", e)
            } finally {
                neDone = true
                checkIfAllDone()
            }
        }
        
        // 搜索 iTunes
        scope.launch {
            try {
                Log.d("AudioMetadataSearch", "开始搜索 iTunes")
                val itunesSongs = lyricsService.searchFromSource(
                    keyword, 
                    Source.ITUNES,
                    itunesCountry = country,
                    itunesConvertToSimplified = convertToSimplified,
                    itunesCoverSize = coverSize,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                if (itunesSongs.isNotEmpty()) {
                    allSongs.addAll(itunesSongs)
                    hasResults = true
                    sourceGroups = sourceGroups + MetadataSourceGroup(
                        source = Source.ITUNES,
                        sourceName = getMetadataSourceShortName(Source.ITUNES),
                        songs = itunesSongs.map { MetadataSearchResult(it, null, null, false) },
                        isExpanded = true
                    )
                }
            } catch (e: Exception) {
                Log.e("AudioMetadataSearch", "iTunes 搜索失败", e)
            } finally {
                itunesDone = true
                checkIfAllDone()
            }
        }
    }
    
    fun findAndLoadSong(result: MetadataSearchResult, groupSource: Source) {
        selectedSong = result
        showLoading = true
        showBottomSheet = true
        
        if (result.metadata == null && !result.isLoading) {
            scope.launch {
                sourceGroups = sourceGroups.map { g ->
                    if (g.source == groupSource) {
                        g.copy(songs = g.songs.map {
                            if (it.songInfo == result.songInfo) it.copy(isLoading = true) else it
                        })
                    } else g
                }
                
                val coverSize = getItunesCoverSize()
                val qmCoverSize = getQmCoverSize()
                val neCoverSize = getNeCoverSize()
                val country = getItunesCountry()
                val convertToSimplified = shouldConvertToSimplified()
                val detailedSongInfo = lyricsService.getMusicDetail(
                    result.songInfo, 
                    itunesCountry = country, 
                    itunesConvertToSimplified = convertToSimplified, 
                    itunesCoverSize = coverSize,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                val songToUse = detailedSongInfo ?: result.songInfo
                
                val coverUrl = songToUse.coverUrl ?: getCoverUrl(songToUse)
                val coverBitmap = coverUrl?.let { loadBitmapFromUrl(client, it) }
                
                val metadata = AudioMetadata(
                    title = songToUse.title,
                    artist = songToUse.artist.joinToString(getArtistSeparator()),
                    album = songToUse.album,
                    year = songToUse.year,
                    trackNumber = songToUse.trackNumber,
                    discNumber = songToUse.discNumber,
                    genre = songToUse.genre,
                    albumArtist = songToUse.albumArtist,
                    composer = songToUse.composer,
                    lyricist = songToUse.lyricist,
                    comment = songToUse.comment,
                    copyright = songToUse.copyright,
                    coverBitmap = coverBitmap
                )
                
                sourceGroups = sourceGroups.map { g ->
                    if (g.source == groupSource) {
                        g.copy(songs = g.songs.map {
                            if (it.songInfo == result.songInfo) 
                                it.copy(metadata = metadata, coverBitmap = coverBitmap, isLoading = false) 
                            else it
                        })
                    } else g
                }
                
                selectedSong = selectedSong?.copy(metadata = metadata, coverBitmap = coverBitmap, isLoading = false)
                showLoading = false
            }
        } else {
            showLoading = false
        }
    }
    
    LaunchedEffect(autoSearchKeyword) {
        if (autoSearchKeyword.isNotEmpty() && !hasAutoSearched) {
            hasAutoSearched = true
            performSearch(autoSearchKeyword) { allSongs ->
                onCacheCovers(allSongs)
            }
        }
    }
    
    if (coverOnly) {
        CoverOnlySearchScreen(
            onBack = onBack,
            onImport = { metadata ->
                onImport(metadata, setOf("cover"))
            },
            autoSearchKeyword = autoSearchKeyword,
            onCacheCovers = onCacheCovers,
            modifier = modifier
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            CommonHeadBar(
                title = "搜索音频元数据",
                showBack = true,
                showMenu = true,
                onBackClick = onBack,
                onMenuClick = { showHeadbarMenu = true },
                menuContent = { menuButtonPosition ->
                    CustomDropdownMenu(
                        expanded = showHeadbarMenu,
                        onDismissRequest = { showHeadbarMenu = false },
                        items = listOf(
                            MenuItem(
                                title = "AM默认地区",
                                onClick = {
                                    tempAMRegion = currentAMRegion
                                    showAMRegionDialog = true
                                }
                            )
                        ),
                        anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f)
                    )
                }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSearching,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            ),
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "搜索",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchInput.isEmpty()) {
                                            Text(
                                                text = "歌曲名 歌手 / 歌曲名 / 歌手",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (searchInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchInput = "" },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "清除",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            performSearch(searchInput) { allSongs ->
                                onCacheCovers(allSongs)
                            }
                        },
                        enabled = !isSearching
                    ) {
                        if (isSearching) {
                            LoadingIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("搜索")
                        }
                    }
                }
                
                searchError?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
                
                if (sourceGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TabRow(
                        selectedTabIndex = selectedTabIndex
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = {
                                    scope.launch {
                                        pagerState.scrollToPage(index)
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                    
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) { page ->
                        val currentSource = tabSources[page]
                        
                        if (currentSource == null) {
                            val recommendedResults = getRecommendedResults(searchInput)
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(recommendedResults) { result ->
                                    val sourceGroup = sourceGroups.find { it.source == result.songInfo.source }
                                    MetadataResultItem(
                                        result = result,
                                        showSourceLabel = true,
                                        onClick = {
                                            findAndLoadSong(result, result.songInfo.source)
                                        }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.navigationBarsPadding())
                                }
                            }
                        } else {
                            val group = sourceGroups.find { it.source == currentSource }
                            if (group != null) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    items(group.songs) { result ->
                                        MetadataResultItem(
                                            result = result,
                                            showSourceLabel = false,
                                            onClick = {
                                                findAndLoadSong(result, currentSource)
                                            }
                                        )
                                    }
                                    item {
                                        Spacer(modifier = Modifier.navigationBarsPadding())
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "该来源暂无结果",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isSearching) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在搜索中...",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                } else if (sourceGroups.isEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "输入格式：歌曲名 歌手 / 歌曲名 / 歌手",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "示例：成都 赵雷",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        if (showBottomSheet && selectedSong != null) {
            MetadataBottomSheet(
                songResult = selectedSong!!,
                isLoading = showLoading,
                sheetState = sheetState,
                onDismiss = { showBottomSheet = false },
                onImport = { selectedFields ->
                    selectedSong?.metadata?.let { metadata ->
                        onImport(metadata, selectedFields)
                    }
                }
            )
        }

        if (showAMRegionDialog) {
            AMRegionDialog(
                currentValue = tempAMRegion,
                onValueChange = { tempAMRegion = it },
                onDismiss = { showAMRegionDialog = false },
                onConfirm = {
                    currentAMRegion = tempAMRegion
                    updateAMDefaultRegion(context, tempAMRegion)
                    showAMRegionDialog = false
                }
            )
        }
    }
}

@Composable
fun MetadataSourceGroupCard(
    group: MetadataSourceGroup,
    onToggleExpand: () -> Unit,
    onSongClick: (MetadataSearchResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.sourceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "(${group.songs.size}首)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = if (group.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (group.isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    group.songs.forEach { result ->
                        MetadataResultItem(
                            result = result,
                            onClick = { onSongClick(result) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataResultItem(
    result: MetadataSearchResult,
    showSourceLabel: Boolean = false,
    onClick: () -> Unit
) {
    val song = result.songInfo
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    val client = remember { OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    }
    var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoadingCover by remember { mutableStateOf(false) }
    var hasTriedLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(song) {
        if (hasTriedLoading) {
            return@LaunchedEffect
        }
        
        val coverUrl = getLowResCoverUrl(song)
        if (coverUrl != null) {
            hasTriedLoading = true
            isLoadingCover = true
            scope.launch {
                coverBitmap = loadBitmapFromUrl(client, coverUrl, context)
                isLoadingCover = false
            }
        } else {
            hasTriedLoading = true
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap!!.asImageBitmap(),
                            contentDescription = "封面",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isLoadingCover) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                            contentDescription = "音乐",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                coverBitmap?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${it.width} × ${it.height}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "未知歌曲",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = song.artist.joinToString(prefs.getString("artistSeparator", "/") ?: "/"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                song.album?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "专辑: $it",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val extraInfo = mutableListOf<String>()
                song.year?.let { extraInfo.add("年份: $it") }
                song.discNumber?.let { extraInfo.add("碟: $it") }
                song.trackNumber?.let { extraInfo.add("曲: $it") }
                
                if (extraInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = extraInfo.joinToString(" | "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val sourceLabel = getMetadataSourceShortName(song.source)
                if (showSourceLabel || true) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = sourceLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                song.duration?.let { duration ->
                    Text(
                        text = formatMetadataDuration(duration),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (result.isLoading) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataBottomSheet(
    songResult: MetadataSearchResult,
    isLoading: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onImport: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    val metadata = songResult.metadata
    val fieldNames = listOf("cover", "title", "artist", "album", "year", "trackNumber", "discNumber", "genre", "albumArtist", "composer", "lyricist", "comment", "copyright")
    
    fun loadSavedFields(): Set<String> {
        val savedString = prefs.getString("selectedMetadataFields", null)
        return if (savedString != null) {
            savedString.split(",").toSet()
        } else {
            fieldNames.toSet()
        }
    }
    
    fun saveFields(fields: Set<String>) {
        prefs.edit().putString("selectedMetadataFields", fields.joinToString(",")).apply()
    }
    
    var selectedFields by remember { mutableStateOf(loadSavedFields()) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songResult.songInfo.title ?: "未知歌曲",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = songResult.songInfo.artist.joinToString(prefs.getString("artistSeparator", "/") ?: "/"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                TextButton(onClick = {
                    selectedFields = if (selectedFields.size == fieldNames.size) {
                        emptySet()
                    } else {
                        fieldNames.toSet()
                    }
                    saveFields(selectedFields)
                }) {
                    Text(if (selectedFields.size == fieldNames.size) "取消全选" else "全选")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在加载元数据...")
                    }
                }
            } else if (metadata != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        if (metadata.coverBitmap != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = metadata.coverBitmap.asImageBitmap(),
                                        contentDescription = "封面",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${metadata.coverBitmap.width} × ${metadata.coverBitmap.height}",
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Checkbox(
                                    checked = "cover" in selectedFields,
                                    onCheckedChange = { checked ->
                                        selectedFields = if (checked) {
                                            selectedFields + "cover"
                                        } else {
                                            selectedFields - "cover"
                                        }
                                        saveFields(selectedFields)
                                    }
                                )
                            }
                        }
                    }
                    
                    item {
                        MetadataDisplayField("标题", metadata.title, "title" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "title"
                            } else {
                                selectedFields - "title"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("艺术家", metadata.artist, "artist" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "artist"
                            } else {
                                selectedFields - "artist"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("专辑", metadata.album, "album" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "album"
                            } else {
                                selectedFields - "album"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("年份", metadata.year, "year" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "year"
                            } else {
                                selectedFields - "year"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("音轨号", metadata.trackNumber, "trackNumber" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "trackNumber"
                            } else {
                                selectedFields - "trackNumber"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("碟号", metadata.discNumber, "discNumber" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "discNumber"
                            } else {
                                selectedFields - "discNumber"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("风格", metadata.genre, "genre" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "genre"
                            } else {
                                selectedFields - "genre"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("专辑艺术家", metadata.albumArtist, "albumArtist" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "albumArtist"
                            } else {
                                selectedFields - "albumArtist"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("作曲", metadata.composer, "composer" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "composer"
                            } else {
                                selectedFields - "composer"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("作词", metadata.lyricist, "lyricist" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "lyricist"
                            } else {
                                selectedFields - "lyricist"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("注释", metadata.comment, "comment" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "comment"
                            } else {
                                selectedFields - "comment"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        MetadataDisplayField("版权信息", metadata.copyright, "copyright" in selectedFields) { checked ->
                            selectedFields = if (checked) {
                                selectedFields + "copyright"
                            } else {
                                selectedFields - "copyright"
                            }
                            saveFields(selectedFields)
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无元数据")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                
                Button(
                    onClick = { onImport(selectedFields) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && metadata != null && selectedFields.isNotEmpty()
                ) {
                    Text("导入")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MetadataDisplayField(label: String, value: String?, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = value ?: "无",
                    fontSize = 14.sp,
                    color = if (value != null) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

fun getCoverUrl(songInfo: SongInfo): String? {
    return when (songInfo.source) {
        Source.QM -> "https://y.gtimg.cn/music/photo_new/T002R300x300M000${songInfo.mid}.jpg"
        Source.KG -> null
        Source.NE -> "https://p3.music.126.net/${songInfo.id}?param=300y300"
        Source.ITUNES -> songInfo.coverUrl
    }
}

fun getLowResCoverUrl(songInfo: SongInfo): String? {
    return songInfo.coverUrl ?: when (songInfo.source) {
        Source.QM -> null // QQ 音乐搜索结果中已包含 coverUrl
        Source.KG -> null
        Source.NE -> "https://p3.music.126.net/${songInfo.id}?param=90y90"
        Source.ITUNES -> songInfo.coverUrl?.replace("100x100", "60x60")?.replace("500x500", "60x60")
    }
}

suspend fun loadBitmapFromUrl(client: OkHttpClient, url: String, context: Context? = null): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            context?.let { ctx ->
                val cachedBitmap = loadBitmapFromCache(ctx, url)
                if (cachedBitmap != null) {
                    return@withContext cachedBitmap
                }
            }
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

suspend fun loadBitmapFromCache(context: Context, coverUrl: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "cover_cache")
            if (!cacheDir.exists()) {
                return@withContext null
            }
            
            val fileName = coverUrl.hashCode().toString() + ".jpg"
            val cacheFile = File(cacheDir, fileName)
            
            if (cacheFile.exists()) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

fun getMetadataSourceShortName(source: Source): String {
    return when (source) {
        Source.QM -> "QM"
        Source.KG -> "KG"
        Source.NE -> "NE"
        Source.ITUNES -> "AM"
    }
}

fun formatMetadataDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverOnlySearchScreen(
    onBack: () -> Unit,
    onImport: (AudioMetadata) -> Unit,
    autoSearchKeyword: String = "",
    onCacheCovers: (List<SongInfo>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lyricsService = remember { LyricsService() }
    val prefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    val client = remember { OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    }
    
    var searchInput by remember { mutableStateOf(autoSearchKeyword) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var allResults by remember { mutableStateOf<List<MetadataSearchResult>>(emptyList()) }
    var hasAutoSearched by remember { mutableStateOf(false) }
    
    var selectedCover by remember { mutableStateOf<MetadataSearchResult?>(null) }
    var showCoverPreview by remember { mutableStateOf(false) }
    var isLoadingCover by remember { mutableStateOf(false) }
    
    fun getItunesCoverSize(): Int {
        return prefs.getInt("amCoverSize", 3000)
    }
    
    fun getQmCoverSize(): Int {
        return prefs.getInt("qmCoverSize", 1200)
    }
    
    fun getNeCoverSize(): Int {
        return prefs.getInt("neCoverSize", 1000)
    }
    
    fun getItunesCountry(): String {
        return when (prefs.getString("amRegion", "HK_SC") ?: "HK_SC") {
            "HK_SC" -> "HK"
            "HK" -> "HK"
            "TW_SC" -> "TW"
            "TW" -> "TW"
            "CN" -> "CN"
            "JP" -> "JP"
            "KR" -> "KR"
            "US" -> "US"
            else -> "HK"
        }
    }
    
    fun shouldConvertToSimplified(): Boolean {
        val region = prefs.getString("amRegion", "HK_SC") ?: "HK_SC"
        return region == "HK_SC" || region == "TW_SC"
    }
    
    fun performSearch(keyword: String) {
        if (keyword.isBlank()) return
        
        searchError = null
        isSearching = true
        allResults = emptyList()
        
        val coverSize = getItunesCoverSize()
        val qmCoverSize = getQmCoverSize()
        val neCoverSize = getNeCoverSize()
        val country = getItunesCountry()
        val convertToSimplified = shouldConvertToSimplified()
        
        val allSongs = mutableListOf<SongInfo>()
        var qmDone = false
        var neDone = false
        var itunesDone = false
        var hasResults = false
        
        fun checkIfAllDone() {
            if (qmDone && neDone && itunesDone) {
                isSearching = false
                if (!hasResults) {
                    searchError = "未找到结果"
                } else {
                    onCacheCovers(allSongs)
                }
            }
        }
        
        scope.launch {
            try {
                val qmSongs = lyricsService.searchFromSource(
                    keyword, 
                    Source.QM,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                if (qmSongs.isNotEmpty()) {
                    allSongs.addAll(qmSongs)
                    hasResults = true
                    allResults = allResults + qmSongs.map { MetadataSearchResult(it, null, null, false) }
                }
            } catch (e: Exception) {
                Log.e("CoverSearch", "QQ 音乐搜索失败", e)
            } finally {
                qmDone = true
                checkIfAllDone()
            }
        }
        
        scope.launch {
            try {
                val neSongs = lyricsService.searchFromSource(
                    keyword, 
                    Source.NE,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                if (neSongs.isNotEmpty()) {
                    allSongs.addAll(neSongs)
                    hasResults = true
                    allResults = allResults + neSongs.map { MetadataSearchResult(it, null, null, false) }
                }
            } catch (e: Exception) {
                Log.e("CoverSearch", "网易云音乐搜索失败", e)
            } finally {
                neDone = true
                checkIfAllDone()
            }
        }
        
        scope.launch {
            try {
                val itunesSongs = lyricsService.searchFromSource(
                    keyword, 
                    Source.ITUNES,
                    itunesCountry = country,
                    itunesConvertToSimplified = convertToSimplified,
                    itunesCoverSize = coverSize,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                if (itunesSongs.isNotEmpty()) {
                    allSongs.addAll(itunesSongs)
                    hasResults = true
                    allResults = allResults + itunesSongs.map { MetadataSearchResult(it, null, null, false) }
                }
            } catch (e: Exception) {
                Log.e("CoverSearch", "iTunes 搜索失败", e)
            } finally {
                itunesDone = true
                checkIfAllDone()
            }
        }
    }
    
    fun loadAndSelectCover(result: MetadataSearchResult) {
        selectedCover = result
        isLoadingCover = true
        showCoverPreview = true
        
        if (result.metadata == null && !result.isLoading) {
            scope.launch {
                val coverSize = getItunesCoverSize()
                val qmCoverSize = getQmCoverSize()
                val neCoverSize = getNeCoverSize()
                val country = getItunesCountry()
                val convertToSimplified = shouldConvertToSimplified()
                val detailedSongInfo = lyricsService.getMusicDetail(
                    result.songInfo, 
                    itunesCountry = country, 
                    itunesConvertToSimplified = convertToSimplified, 
                    itunesCoverSize = coverSize,
                    qmCoverSize = qmCoverSize,
                    neCoverSize = neCoverSize
                )
                val songToUse = detailedSongInfo ?: result.songInfo
                
                val coverUrl = songToUse.coverUrl ?: getCoverUrl(songToUse)
                val coverBitmap = coverUrl?.let { loadBitmapFromUrl(client, it) }
                
                val metadata = AudioMetadata(
                    coverBitmap = coverBitmap
                )
                
                allResults = allResults.map { r ->
                    if (r.songInfo == result.songInfo) 
                        r.copy(metadata = metadata, coverBitmap = coverBitmap, isLoading = false) 
                    else r
                }
                
                selectedCover = selectedCover?.copy(metadata = metadata, coverBitmap = coverBitmap, isLoading = false)
                isLoadingCover = false
            }
        } else {
            isLoadingCover = false
        }
    }
    
    LaunchedEffect(autoSearchKeyword) {
        if (autoSearchKeyword.isNotEmpty() && !hasAutoSearched) {
            hasAutoSearched = true
            performSearch(autoSearchKeyword)
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "搜索封面",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSearching,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp
                        ),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "搜索",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchInput.isEmpty()) {
                                        Text(
                                            text = "歌曲名 歌手",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                                if (searchInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchInput = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "清除",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = { performSearch(searchInput) },
                    enabled = !isSearching
                ) {
                    if (isSearching) {
                        LoadingIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("搜索")
                    }
                }
            }
            
            searchError?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            
            if (allResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = allResults.size,
                        key = { index -> allResults[index].songInfo.hashCode() }
                    ) { index ->
                        val result = allResults[index]
                        CoverGridItem(
                            result = result,
                            onClick = { loadAndSelectCover(result) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
            
            if (isSearching) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在搜索中...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            } else if (allResults.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "输入格式：歌曲名 歌手",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "示例：成都 赵雷",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
    
    if (showCoverPreview && selectedCover != null) {
        CoverPreviewDialog(
            songResult = selectedCover!!,
            isLoading = isLoadingCover,
            onDismiss = { showCoverPreview = false },
            onImport = {
                selectedCover?.metadata?.let { metadata ->
                    onImport(metadata)
                }
            }
        )
    }
}

@Composable
fun CoverGridItem(
    result: MetadataSearchResult,
    onClick: () -> Unit
) {
    val song = result.songInfo
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val client = remember { OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    }
    var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoadingCover by remember { mutableStateOf(false) }
    var hasTriedLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(song) {
        if (hasTriedLoading) {
            return@LaunchedEffect
        }
        
        val coverUrl = getLowResCoverUrl(song)
        if (coverUrl != null) {
            hasTriedLoading = true
            isLoadingCover = true
            scope.launch {
                coverBitmap = loadBitmapFromUrl(client, coverUrl, context)
                isLoadingCover = false
            }
        } else {
            hasTriedLoading = true
        }
    }
    
    val sourceLabel = getMetadataSourceShortName(song.source)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isLoadingCover) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                        contentDescription = "音乐",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = sourceLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            coverBitmap?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${it.width}×${it.height}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPreviewDialog(
    songResult: MetadataSearchResult,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    
    fun saveCoverToGallery() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val song = songResult.songInfo
                    val coverBitmap = songResult.coverBitmap ?: return@withContext
                    
                    // 创建目录
                    val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    val lyricBoxDir = java.io.File(picturesDir, "LyricBox")
                    if (!lyricBoxDir.exists()) {
                        lyricBoxDir.mkdirs()
                    }
                    
                    // 确定文件名
                    val title = song.title?.ifEmpty { "unknown" } ?: "unknown"
                    val artist = song.artist?.ifEmpty { "unknown" } ?: "unknown"
                    val baseFileName = "$title - $artist"
                    
                    // 确定文件扩展名
                    val extension = "jpg"
                    
                    // 确保文件名唯一
                    var fileName = "$baseFileName.$extension"
                    var file = java.io.File(lyricBoxDir, fileName)
                    var counter = 1
                    while (file.exists()) {
                        fileName = "$baseFileName ($counter).$extension"
                        file = java.io.File(lyricBoxDir, fileName)
                        counter++
                    }
                    
                    // 保存图片
                    java.io.FileOutputStream(file).use { out ->
                        coverBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        out.flush()
                    }
                    
                    // 通知媒体库
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )
                    
                    showSaveSuccessDialog = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(48.dp))
                
                Text(
                    text = "封面预览",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = { saveCoverToGallery() },
                    enabled = !isLoading && songResult.coverBitmap != null
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.save),
                        contentDescription = "保存",
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (!isLoading && songResult.coverBitmap != null) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (songResult.coverBitmap != null) {
                    Image(
                        bitmap = songResult.coverBitmap!!.asImageBitmap(),
                        contentDescription = "封面",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${songResult.coverBitmap!!.width} × ${songResult.coverBitmap!!.height}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && songResult.coverBitmap != null
            ) {
                Text("应用此封面")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    if (showSaveSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSaveSuccessDialog = false },
            title = { Text("保存成功") },
            text = { Text("封面已成功保存到相册/LyricBox目录") },
            confirmButton = {
                Button(onClick = { showSaveSuccessDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}
