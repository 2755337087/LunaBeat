package com.example.LyricBox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.lyrics.LyricsService
import com.example.LyricBox.lyrics.api.AppleMusicApi
import com.example.LyricBox.lyrics.models.LyricsLine
import com.example.LyricBox.lyrics.models.SongInfo
import com.example.LyricBox.lyrics.models.Source
import com.example.LyricBox.lyrics.models.VerbatimLyricsResult
import com.example.LyricBox.lyrics.parser.VerbatimLrcConverter
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.PiracyChecker
import com.example.LyricBox.utils.PiracyCheckResult
import kotlinx.coroutines.launch

class VerbatimLyricsActivity : ComponentActivity() {
    
    private var piracyCheckResult by mutableStateOf<PiracyCheckResult?>(null)
    private var showPiracyWarning by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val checkResult = PiracyChecker.checkAll(this)
        if (checkResult.isPirated) {
            piracyCheckResult = checkResult
            showPiracyWarning = true
        }
        
        val autoSearchKeyword = intent.getStringExtra("autoSearchKeyword") ?: ""
        val audioDuration = intent.getLongExtra("audioDuration", 0L)
        
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars
                ) { _ ->
                    VerbatimLyricsScreen(
                        onBack = { finish() },
                        onImport = { lyricsContent, lyricsFormat ->
                            val resultIntent = android.content.Intent().apply {
                                putExtra("lyricsContent", lyricsContent)
                                putExtra("lyricsFormat", lyricsFormat)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        },
                        autoSearchKeyword = autoSearchKeyword,
                        audioDuration = audioDuration
                    )
                }
                
                if (showPiracyWarning && piracyCheckResult != null) {
                    com.example.LyricBox.ui.components.PiracyWarningDialog(
                        onExit = {
                            finish()
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    )
                }
            }
        }
    }
}

data class SongSearchResult(
    val songInfo: SongInfo,
    val lyricsResult: VerbatimLyricsResult?,
    val isLoading: Boolean = false,
    val similarityScore: Float = 0f
)

data class SourceGroup(
    val source: Source,
    val sourceName: String,
    val songs: List<SongSearchResult>,
    var isExpanded: Boolean = true
)

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

fun calculateCombinedSimilarity(searchKeyword: String, candidate: SongInfo): Float {
    val parts = searchKeyword.split("\\s+".toRegex())
    var title = ""
    var artist = ""
    
    if (parts.size >= 2) {
        title = parts.dropLast(1).joinToString(" ")
        artist = parts.last()
    } else {
        title = searchKeyword
    }
    
    val candidateTitle = (candidate.title ?: "").lowercase().trim()
    val candidateArtist = candidate.artist.joinToString("/").lowercase().trim()
    val origTitle = title.lowercase().trim()
    val origArtist = artist.lowercase().trim()
    
    val titleSim = calculateMaxSimilarity(origTitle, candidateTitle)
    val artistSim = if (origArtist.isNotEmpty()) calculateArtistSimilarity(origArtist, candidateArtist) else 1f
    
    val titleArtistReversedSim = if (origArtist.isNotEmpty()) {
        val sim1 = calculateMaxSimilarity(origTitle, candidateArtist)
        val sim2 = calculateArtistSimilarity(origArtist, candidateTitle)
        (sim1 * 0.7f + sim2 * 0.3f)
    } else 0f
    
    return maxOf(
        (titleSim * 0.7f + artistSim * 0.3f),
        titleArtistReversedSim
    )
}

private val japaneseKanaRegex = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u31F0-\\u31FF\\uFF66-\\uFF9F]")

private fun containsJapaneseKana(text: String): Boolean {
    return japaneseKanaRegex.containsMatchIn(text)
}

private fun hasJapaneseKanaInLyrics(lyricsResult: VerbatimLyricsResult?): Boolean {
    if (lyricsResult == null) return false
    if (containsJapaneseKana(lyricsResult.rawTtml.orEmpty())) return true

    val lyrics = lyricsResult.lyrics ?: return false
    val allLines = lyrics.orig + lyrics.ts
    return allLines.any { line ->
        val lineText = line.words.joinToString("") { it.text }
        containsJapaneseKana(lineText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbatimLyricsScreen(
    onBack: () -> Unit,
    onImport: (String, String) -> Unit,
    autoSearchKeyword: String = "",
    audioDuration: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lyricsService = remember { LyricsService() }
    val appleMusicApi = remember { AppleMusicApi() }
    val prefs = remember { context.getSharedPreferences("VerbatimLyricsSettings", Context.MODE_PRIVATE) }
    
    val tabTitles = listOf("推荐结果", "QM", "KG", "NE", "AM")
    val tabSources = listOf(null, Source.QM, Source.KG, Source.NE, Source.ITUNES)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { 5 }
    
    var searchInput by remember { mutableStateOf(autoSearchKeyword) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var sourceGroups by remember { mutableStateOf<List<SourceGroup>>(emptyList()) }
    var hasAutoSearched by remember { mutableStateOf(false) }
    
    var selectedSong by remember { mutableStateOf<SongSearchResult?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showTranslation by remember { mutableStateOf(prefs.getBoolean("showTranslation", false)) }
    var filterMetadata by remember { mutableStateOf(prefs.getBoolean("filterMetadata", true)) }
    var keepTranslation by remember { mutableStateOf(prefs.getBoolean("amKeepTranslation", true)) }
    var convertToSimplified by remember { mutableStateOf(prefs.getBoolean("amConvertToSimplified", true)) }
    var showLineByLine by remember { mutableStateOf(prefs.getBoolean("showLineByLine", false)) }
    var showLineEndTimestamp by remember { mutableStateOf(prefs.getBoolean("showLineEndTimestamp", true)) }
    var bottomSheetRefreshKey by remember { mutableIntStateOf(0) }
    
    var showInputDialog by remember { mutableStateOf(false) }
    var showCopiedDialog by remember { mutableStateOf(false) }
    var showNoLyricsDialog by remember { mutableStateOf(false) }
    var showHeadbarMenu by remember { mutableStateOf(false) }
    var showAMTokenDialog by remember { mutableStateOf(false) }

    val savedAMTokenConfig = remember { getSavedAMTokenConfig(context) }
    var currentAMTokenConfig by remember { mutableStateOf(savedAMTokenConfig) }
    var tempAMTokenConfig by remember { mutableStateOf(savedAMTokenConfig) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }

    fun applyConvertToSimplifiedOption(source: Source, lyricsResult: VerbatimLyricsResult?) {
        convertToSimplified = when {
            hasJapaneseKanaInLyrics(lyricsResult) -> false
            source == Source.ITUNES -> prefs.getBoolean("amConvertToSimplified", true)
            else -> true
        }
    }
    
    fun performSearch(keyword: String) {
        if (keyword.isBlank()) {
            showInputDialog = true
            return
        }
        
        searchError = null
        isSearching = true
        sourceGroups = emptyList()
        
        scope.launch {
            try {
                val songs = lyricsService.searchAllSourcesForLyrics(keyword)
                
                val groups = mutableListOf<SourceGroup>()
                songs.forEach { (source, songList) ->
                    if (songList.isNotEmpty()) {
                        val songsWithSimilarity = songList.map { song ->
                            SongSearchResult(
                                songInfo = song,
                                lyricsResult = null,
                                isLoading = false,
                                similarityScore = calculateCombinedSimilarity(keyword, song)
                            )
                        }
                        groups.add(SourceGroup(
                            source = source,
                            sourceName = getSourceShortName(source),
                            songs = songsWithSimilarity,
                            isExpanded = false
                        ))
                    }
                }
                
                // 添加 Apple Music 搜索结果
                val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val tokenSource = prefs.getString("amTokenSource", "cloudflare") ?: "cloudflare"
                val amUrlCountry = prefs.getString("amUrlCountry", "cn") ?: "cn"
                val amUrlCountryContributor = prefs.getString("amUrlCountryContributor", "tr") ?: "tr"
                
                val storefront = when (tokenSource) {
                    "contributor" -> amUrlCountryContributor
                    "custom" -> prefs.getString("amCountry", "cn") ?: "cn"
                    else -> amUrlCountry
                }
                val amSongs = appleMusicApi.search(keyword, storefront)
                if (amSongs.isNotEmpty()) {
                    val amSongsWithSimilarity = amSongs.map { song ->
                        SongSearchResult(
                            songInfo = song,
                            lyricsResult = null,
                            isLoading = false,
                            similarityScore = calculateCombinedSimilarity(keyword, song)
                        )
                    }
                    groups.add(SourceGroup(
                        source = Source.ITUNES,
                        sourceName = "AM",
                        songs = amSongsWithSimilarity,
                        isExpanded = false
                    ))
                }
                
                if (groups.isEmpty()) {
                    searchError = "未找到歌词"
                } else {
                    sourceGroups = groups
                }
            } catch (e: Exception) {
                searchError = when {
                    e.message?.contains("network", ignoreCase = true) == true -> "请求失败，请检查网络"
                    else -> "搜索失败：${e.message}"
                }
            } finally {
                isSearching = false
            }
        }
    }
    
    fun getRecommendResults(): List<SongSearchResult> {
        val allResults = mutableListOf<SongSearchResult>()
        
        sourceGroups.forEach { group ->
            group.songs.take(2).forEach { song ->
                allResults.add(song)
            }
        }
        
        Log.d("VerbatimLyrics", "原始推荐结果数: ${allResults.size}")
        Log.d("VerbatimLyrics", "当前音频时长: $audioDuration ms")
        
        val filtered = allResults
            .filter { 
                val passSimilarity = it.similarityScore >= 0.5f
                val passDuration = if (audioDuration > 0 && it.songInfo.duration != null) {
                    val durationDiff = Math.abs(it.songInfo.duration!! - audioDuration)
                    Log.d("VerbatimLyrics", "歌曲: ${it.songInfo.title}, 时长: ${it.songInfo.duration} ms, 差值: $durationDiff ms")
                    durationDiff <= 2000L
                } else {
                    true
                }
                passSimilarity && passDuration
            }
            .sortedByDescending { it.similarityScore }
        
        Log.d("VerbatimLyrics", "筛选后推荐结果数: ${filtered.size}")
        return filtered
    }
    
    LaunchedEffect(autoSearchKeyword) {
        if (autoSearchKeyword.isNotEmpty() && !hasAutoSearched) {
            hasAutoSearched = true
            performSearch(autoSearchKeyword)
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "获取逐字歌词",
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
                            title = "AM歌词配置",
                            onClick = {
                                tempAMTokenConfig = currentAMTokenConfig
                                showAMTokenDialog = true
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
                    onClick = { performSearch(searchInput) },
                    enabled = !isSearching
                ) {
                    if (isSearching) {
                        LoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("搜索")
                    }
                }
            }
            
            if (audioDuration > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "音频时长",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前音频时长: ${formatDuration(audioDuration)}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            searchError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            
            if (sourceGroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) { page ->
                    val currentSource = tabSources[page]
                    
                    if (currentSource == null) {
                        val recommendedResults = getRecommendResults()
                        if (recommendedResults.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无符合条件的推荐结果",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(recommendedResults) { result ->
                                    SongResultItemWithSource(
                                        result = result,
                                        onClick = {
                                            if (result.lyricsResult == null && !result.isLoading) {
                                                // 对于非 Apple Music 源，先加载歌词再显示
                                                scope.launch {
                                                    // 1. 更新 sourceGroups 为 isLoading=true
                                                    val updatedGroups = sourceGroups.map { group ->
                                                        group.copy(
                                                            songs = group.songs.map { song ->
                                                                if (song.songInfo == result.songInfo) {
                                                                    song.copy(isLoading = true)
                                                                } else song
                                                            }
                                                        )
                                                    }
                                                    sourceGroups = updatedGroups
                                                    
                                                    // 2. 获取歌词
                                                    val lyricsResult = if (result.songInfo.source == Source.ITUNES) {
                                                        val (lyrics, rawTtml, error) = appleMusicApi.getLyrics(result.songInfo, context)
                                                        if (lyrics != null) {
                                                            VerbatimLyricsResult(
                                                                source = result.songInfo.source,
                                                                sourceName = "AM",
                                                                lyrics = lyrics,
                                                                rawTtml = rawTtml
                                                            )
                                                        } else {
                                                            VerbatimLyricsResult(
                                                                source = result.songInfo.source,
                                                                sourceName = "AM",
                                                                lyrics = null,
                                                                error = error ?: "获取歌词失败"
                                                            )
                                                        }
                                                    } else {
                                                        lyricsService.getLyricsFromSource(result.songInfo)
                                                    }
                                                    
                                                    // 3. 更新 sourceGroups
                                                    val finalUpdatedGroups = sourceGroups.map { group ->
                                                        group.copy(
                                                            songs = group.songs.map { song ->
                                                                if (song.songInfo == result.songInfo) {
                                                                    song.copy(lyricsResult = lyricsResult, isLoading = false)
                                                                } else song
                                                            }
                                                        )
                                                    }
                                                    sourceGroups = finalUpdatedGroups
                                                    
                                                    // 4. 现在显示 bottomSheet
                                                    applyConvertToSimplifiedOption(result.songInfo.source, lyricsResult)
                                                    selectedSong = result.copy(lyricsResult = lyricsResult, isLoading = false)
                                                    bottomSheetRefreshKey++
                                                    showBottomSheet = true
                                                }
                                            } else {
                                                applyConvertToSimplifiedOption(result.songInfo.source, result.lyricsResult)
                                                selectedSong = result
                                                bottomSheetRefreshKey++
                                                showBottomSheet = true
                                            }
                                        }
                                    )
                                }
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
                                    SongResultItem(
                                        result = result,
                                        onClick = {
                                            if (result.lyricsResult == null && !result.isLoading) {
                                                // 先加载歌词再显示
                                                scope.launch {
                                                    // 1. 更新 sourceGroups 为 isLoading=true
                                                    sourceGroups = sourceGroups.map { g ->
                                                        if (g.source == group.source) {
                                                            g.copy(songs = g.songs.map {
                                                                if (it.songInfo == result.songInfo) it.copy(isLoading = true) else it
                                                            })
                                                        } else g
                                                    }
                                                    
                                                    // 2. 获取歌词
                                                    val lyricsResult = if (result.songInfo.source == Source.ITUNES) {
                                                        val (lyrics, rawTtml, error) = appleMusicApi.getLyrics(result.songInfo, context)
                                                        if (lyrics != null) {
                                                            VerbatimLyricsResult(
                                                                source = result.songInfo.source,
                                                                sourceName = "AM",
                                                                lyrics = lyrics,
                                                                rawTtml = rawTtml
                                                            )
                                                        } else {
                                                            VerbatimLyricsResult(
                                                                source = result.songInfo.source,
                                                                sourceName = "AM",
                                                                lyrics = null,
                                                                error = error ?: "获取歌词失败"
                                                            )
                                                        }
                                                    } else {
                                                        lyricsService.getLyricsFromSource(result.songInfo)
                                                    }
                                                    
                                                    // 3. 更新 sourceGroups
                                                    sourceGroups = sourceGroups.map { g ->
                                                        if (g.source == group.source) {
                                                            g.copy(songs = g.songs.map {
                                                                if (it.songInfo == result.songInfo) 
                                                                    it.copy(lyricsResult = lyricsResult, isLoading = false) 
                                                                else it
                                                            })
                                                        } else g
                                                    }
                                                    
                                                    // 4. 现在显示 bottomSheet
                                                    applyConvertToSimplifiedOption(result.songInfo.source, lyricsResult)
                                                    selectedSong = result.copy(lyricsResult = lyricsResult, isLoading = false)
                                                    bottomSheetRefreshKey++
                                                    showBottomSheet = true
                                                }
                                            } else {
                                                applyConvertToSimplifiedOption(result.songInfo.source, result.lyricsResult)
                                                selectedSong = result
                                                bottomSheetRefreshKey++
                                                showBottomSheet = true
                                            }
                                        }
                                    )
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
                    LoadingIndicator(modifier = Modifier.size(32.dp))
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
        key(bottomSheetRefreshKey) {
            LyricsBottomSheet(
                songResult = selectedSong!!,
                showTranslation = showTranslation,
                onTranslationChange = { 
                    showTranslation = it
                    prefs.edit().putBoolean("showTranslation", it).apply()
                },
                filterMetadata = filterMetadata,
                onFilterMetadataChange = { 
                    filterMetadata = it
                    prefs.edit().putBoolean("filterMetadata", it).apply()
                },
                sheetState = sheetState,
                onDismiss = { showBottomSheet = false },
                keepTranslation = keepTranslation,
                onKeepTranslationChange = {
                    keepTranslation = it
                    prefs.edit().putBoolean("amKeepTranslation", it).apply()
                },
                convertToSimplified = convertToSimplified,
                onConvertToSimplifiedChange = {
                    convertToSimplified = it
                    prefs.edit().putBoolean("amConvertToSimplified", it).apply()
                },
                showLineByLine = showLineByLine,
                onShowLineByLineChange = { 
                    showLineByLine = it
                    prefs.edit().putBoolean("showLineByLine", it).apply()
                },
                showLineEndTimestamp = showLineEndTimestamp,
                onShowLineEndTimestampChange = {
                    showLineEndTimestamp = it
                    prefs.edit().putBoolean("showLineEndTimestamp", it).apply()
                },
                onCopy = {
                    val isAppleMusic = selectedSong?.lyricsResult?.source == Source.ITUNES
                    val rawTtml = selectedSong?.lyricsResult?.rawTtml
                    val lyricsText = if (isAppleMusic && !rawTtml.isNullOrEmpty()) {
                        // 处理 TTML 文本
                        var ttml = rawTtml
                        // 处理保留翻译
                        if (!keepTranslation) {
                            ttml = ttml.replace(Regex("""<[\w:]*translation\b[^>]*>.*?</[\w:]*translation>""", RegexOption.DOT_MATCHES_ALL), "")
                            ttml = ttml.replace(Regex("""<[\w:]*translations\b[^>]*>.*?</[\w:]*translations>""", RegexOption.DOT_MATCHES_ALL), "")
                        }
                        // 处理繁转简
                        if (convertToSimplified) {
                            ttml = com.example.LyricBox.utils.ChineseConverter.toSimplified(ttml)
                        }
                        ttml
                    } else {
                        // 处理其他文本
                        var text = getLyricsText(selectedSong!!, showTranslation, filterMetadata, keepTranslation, convertToSimplified, rawTtml, showLineByLine, showLineEndTimestamp)
                        if (convertToSimplified) {
                            text = com.example.LyricBox.utils.ChineseConverter.toSimplified(text)
                        }
                        text
                    }
                    if (lyricsText.isNotEmpty()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("歌词", lyricsText)
                        clipboard.setPrimaryClip(clip)
                        showCopiedDialog = true
                    } else {
                        showNoLyricsDialog = true
                    }
                },
                onImport = {
                    val isAppleMusic = selectedSong?.lyricsResult?.source == Source.ITUNES
                    val rawTtml = selectedSong?.lyricsResult?.rawTtml
                    val lyricsFormat = if (isAppleMusic && !rawTtml.isNullOrEmpty()) {
                        "TTML歌词"
                    } else {
                        "LRC逐行/逐字歌词"
                    }
                    val lyricsText = if (isAppleMusic && !rawTtml.isNullOrEmpty()) {
                        // 处理 TTML 文本
                        var ttml = rawTtml
                        // 处理保留翻译
                        if (!keepTranslation) {
                            ttml = ttml.replace(Regex("""<[\w:]*translation\b[^>]*>.*?</[\w:]*translation>""", RegexOption.DOT_MATCHES_ALL), "")
                            ttml = ttml.replace(Regex("""<[\w:]*translations\b[^>]*>.*?</[\w:]*translations>""", RegexOption.DOT_MATCHES_ALL), "")
                        }
                        // 处理繁转简
                        if (convertToSimplified) {
                            ttml = com.example.LyricBox.utils.ChineseConverter.toSimplified(ttml)
                        }
                        ttml
                    } else {
                        // 处理其他文本
                        var text = getLyricsText(selectedSong!!, showTranslation, filterMetadata, keepTranslation, convertToSimplified, rawTtml, showLineByLine, showLineEndTimestamp)
                        if (convertToSimplified) {
                            text = com.example.LyricBox.utils.ChineseConverter.toSimplified(text)
                        }
                        text
                    }
                    if (lyricsText.isNotEmpty()) {
                        onImport(lyricsText, lyricsFormat)
                    } else {
                        showNoLyricsDialog = true
                    }
                }
            )
        }
    }

    if (showAMTokenDialog) {
        AMTokenDialog(
            currentSource = tempAMTokenConfig.tokenSource,
            onSourceChange = { tempAMTokenConfig = tempAMTokenConfig.copy(tokenSource = it) },
            currentUserToken = tempAMTokenConfig.userToken,
            onUserTokenChange = { tempAMTokenConfig = tempAMTokenConfig.copy(userToken = it) },
            currentCloudflareUrl = tempAMTokenConfig.cloudflareUrl,
            onCloudflareUrlChange = { tempAMTokenConfig = tempAMTokenConfig.copy(cloudflareUrl = it) },
            currentCountry = tempAMTokenConfig.country,
            onCountryChange = { tempAMTokenConfig = tempAMTokenConfig.copy(country = it) },
            defaultUrlName = savedAMTokenConfig.defaultUrlName,
            contributorUrlName = savedAMTokenConfig.contributorUrlName,
            noticeContributor = savedAMTokenConfig.noticeContributor,
            onDismiss = { showAMTokenDialog = false },
            onConfirm = {
                currentAMTokenConfig = tempAMTokenConfig
                updateAMTokenConfig(context, tempAMTokenConfig)
                showAMTokenDialog = false
            }
        )
    }
    
    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("提示") },
            text = { Text("请输入搜索内容") },
            confirmButton = {
                Button(onClick = { showInputDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    if (showCopiedDialog) {
        AlertDialog(
            onDismissRequest = { showCopiedDialog = false },
            title = { Text("提示") },
            text = { Text("已复制到剪贴板") },
            confirmButton = {
                Button(onClick = { showCopiedDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    if (showNoLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showNoLyricsDialog = false },
            title = { Text("提示") },
            text = { Text("暂无歌词") },
            confirmButton = {
                Button(onClick = { showNoLyricsDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun SongResultItemWithSource(
    result: SongSearchResult,
    onClick: () -> Unit
) {
    val song = result.songInfo
    
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
                    text = song.artist.joinToString("/"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                song.album?.let { album ->
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = album,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = getSourceShortName(song.source),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                song.duration?.let { duration ->
                    Text(
                        text = formatDuration(duration),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (result.isLoading) {
                Spacer(modifier = Modifier.width(8.dp))
                LoadingIndicator(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SongResultItem(
    result: SongSearchResult,
    onClick: () -> Unit
) {
    val song = result.songInfo
    
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "未知歌曲",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = song.artist.joinToString("/"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    song.duration?.let { duration ->
                        Text(
                            text = formatDuration(duration),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                song.album?.let { album ->
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = album,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (result.isLoading) {
                LoadingIndicator(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    songResult: SongSearchResult,
    showTranslation: Boolean,
    onTranslationChange: (Boolean) -> Unit,
    filterMetadata: Boolean,
    onFilterMetadataChange: (Boolean) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onImport: () -> Unit,
    keepTranslation: Boolean = false,
    onKeepTranslationChange: (Boolean) -> Unit = {},
    convertToSimplified: Boolean = true,
    onConvertToSimplifiedChange: (Boolean) -> Unit = {},
    showLineByLine: Boolean = false,
    onShowLineByLineChange: (Boolean) -> Unit = {},
    showLineEndTimestamp: Boolean = true,
    onShowLineEndTimestampChange: (Boolean) -> Unit = {}
) {
    val lyricsResult = songResult.lyricsResult
    val lyrics = lyricsResult?.lyrics
    val isAppleMusic = lyricsResult?.source == Source.ITUNES
    val rawTtml = lyricsResult?.rawTtml
    val hasTranslation = lyrics?.ts?.isNotEmpty() == true
    val hasTtmlTranslation = rawTtml?.let { 
        it.contains("<translation") || it.contains("<itunes:translation")
    } == true
    
    // 根据选项处理歌词文本
    val processedLyricsText = remember(showTranslation, filterMetadata, keepTranslation, convertToSimplified, showLineByLine, showLineEndTimestamp) {
        var text = getLyricsText(songResult, showTranslation, filterMetadata, keepTranslation, convertToSimplified, rawTtml, showLineByLine, showLineEndTimestamp)
        
        // 处理繁转简
        if (convertToSimplified) {
            text = com.example.LyricBox.utils.ChineseConverter.toSimplified(text)
        }
        text
    }
    
    // 处理原始 TTML 文本（用于 Apple Music）
    val processedRawTtml = remember(rawTtml, keepTranslation, convertToSimplified) {
        if (rawTtml.isNullOrEmpty()) {
            rawTtml
        } else {
            var ttml = rawTtml
            // 处理保留翻译
            if (!keepTranslation) {
                // 移除 <translation> 标签及其内容（支持属性和命名空间）
                ttml = ttml.replace(Regex("""<[\w:]*translation\b[^>]*>.*?</[\w:]*translation>""", RegexOption.DOT_MATCHES_ALL), "")
                // 也移除整个 <translations> 标签块
                ttml = ttml.replace(Regex("""<[\w:]*translations\b[^>]*>.*?</[\w:]*translations>""", RegexOption.DOT_MATCHES_ALL), "")
            }
            // 处理繁转简
            if (convertToSimplified) {
                ttml = com.example.LyricBox.utils.ChineseConverter.toSimplified(ttml)
            }
            ttml
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = songResult.songInfo.title ?: "未知歌曲",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = songResult.songInfo.artist.joinToString("/"),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                if (songResult.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LoadingIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "正在获取歌词",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (isAppleMusic && !processedRawTtml.isNullOrEmpty()) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = processedRawTtml,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                } else if (processedLyricsText.isNotEmpty()) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = processedLyricsText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val errorText = if (isAppleMusic && lyricsResult?.error != null) {
                            lyricsResult.error
                        } else if (isAppleMusic) {
                            "加载失败"
                        } else {
                            "暂无歌词"
                        }
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 仅在非加载状态显示按钮
            if (!songResult.isLoading) {
                // 显示按钮
                if (isAppleMusic) {
                    // Apple Music 源的按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasTtmlTranslation) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (keepTranslation)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onKeepTranslationChange(!keepTranslation) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "保留翻译",
                                    fontSize = 14.sp,
                                    fontWeight = if (keepTranslation) FontWeight.Bold else FontWeight.Medium,
                                    color = if (keepTranslation)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (convertToSimplified)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onConvertToSimplifiedChange(!convertToSimplified) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "繁转简",
                                fontSize = 14.sp,
                                fontWeight = if (convertToSimplified) FontWeight.Bold else FontWeight.Medium,
                                color = if (convertToSimplified)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // 非 Apple Music 源的按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (filterMetadata)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onFilterMetadataChange(!filterMetadata) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "仅保留歌词",
                                fontSize = 14.sp,
                                fontWeight = if (filterMetadata) FontWeight.Bold else FontWeight.Medium,
                                color = if (filterMetadata)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (hasTranslation) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (showTranslation)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onTranslationChange(!showTranslation) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "显示翻译",
                                    fontSize = 14.sp,
                                    fontWeight = if (showTranslation) FontWeight.Bold else FontWeight.Medium,
                                    color = if (showTranslation)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (showLineByLine)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onShowLineByLineChange(!showLineByLine) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "逐行歌词",
                                fontSize = 14.sp,
                                fontWeight = if (showLineByLine) FontWeight.Bold else FontWeight.Medium,
                                color = if (showLineByLine)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showLineByLine) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (showLineEndTimestamp)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onShowLineEndTimestampChange(!showLineEndTimestamp) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "显示行结束时间戳",
                                fontSize = 14.sp,
                                fontWeight = if (showLineEndTimestamp) FontWeight.Bold else FontWeight.Medium,
                                color = if (showLineEndTimestamp)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("复制")
                }
                
                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("导入")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun getLyricsText(
    songResult: SongSearchResult, 
    showTranslation: Boolean, 
    filterMetadata: Boolean,
    keepTranslation: Boolean = true,
    convertToSimplified: Boolean = true,
    rawTtml: String? = null,
    showLineByLine: Boolean = false,
    showLineEndTimestamp: Boolean = true
): String {
    val lyricsResult = songResult.lyricsResult ?: return ""
    val isAppleMusic = lyricsResult.source == Source.ITUNES
    
    // 如果是 Apple Music，只返回纯文字（不带时间戳）
    if (isAppleMusic) {
        val lyrics = lyricsResult.lyrics ?: return ""
        val lines = mutableListOf<String>()
        for (line in lyrics.orig) {
            val lineText = line.words.joinToString("") { it.text }
            if (lineText.isNotEmpty()) {
                lines.add(lineText)
            }
        }
        return lines.joinToString("\n")
    }
    
    // 其他来源正常处理
    val lyrics = lyricsResult.lyrics ?: return ""
    
    // 过滤翻译歌词中仅包含"//"的行
    val filteredTs = if (showTranslation && lyrics.ts.isNotEmpty()) {
        lyrics.ts.filter { line ->
            val lineText = line.words.joinToString("") { it.text }.trim()
            lineText.isNotEmpty() && lineText != "//" && !lineText.matches(Regex("^/+$"))
        }
    } else {
        null
    }
    
    var text = if (showLineByLine) {
        // 逐行歌词格式
        val sb = StringBuilder()
        for (line in lyrics.orig) {
            if (line.start == null) continue
            val lineStartTime = formatTime(line.start)
            val lineEndTime = line.end?.let { formatTime(it) }
            val lineText = line.words.joinToString("") { it.text }
            if (lineText.isNotEmpty()) {
                sb.append("[$lineStartTime]")
                sb.append(lineText)
                if (showLineEndTimestamp && lineEndTime != null) {
                    sb.append("[$lineEndTime]")
                }
                sb.append("\n")
            }
            
            // 添加翻译
            filteredTs?.let { ts ->
                val tsLine = findMatchingTranslationLine(line.start, ts)
                if (tsLine != null && tsLine.words.isNotEmpty()) {
                    val translationText = tsLine.words.joinToString("") { it.text }.trim()
                    if (translationText.isNotEmpty() && translationText != "//" && !translationText.matches(Regex("^/+$"))) {
                        sb.append("[$lineStartTime]")
                        sb.append(translationText)
                        if (showLineEndTimestamp && lineEndTime != null) {
                            sb.append("[$lineEndTime]")
                        }
                        sb.append("\n")
                    }
                }
            }
        }
        sb.toString()
    } else {
        // 逐字歌词格式
        VerbatimLrcConverter.toVerbatimLrc(
            lyrics.orig,
            filteredTs
        )
    }
    
    if (filterMetadata) {
        val timestampPattern = Regex("""\[\d{1,2}:\d{2}[.:]?\d*\]""")
        val lines = text.lines()
        val startIndex = if (lines.isNotEmpty() && lines[0].contains("-")) 1 else 0
        text = lines.subList(startIndex, lines.size)
            .filter { line ->
                val content = timestampPattern.replace(line, "").trim()
                content.isNotEmpty() &&
                    !content.contains(":") &&
                    !content.contains("：") &&
                    !content.contains("歌词翻译由")
            }
            .joinToString("\n")
    }
    
    return text
}

private fun findMatchingTranslationLine(origStart: Long, translation: List<LyricsLine>): LyricsLine? {
    val exactMatch = translation.find { it.start == origStart }
    if (exactMatch != null) return exactMatch
    
    val tolerance = 1000L
    val nearbyMatch = translation.filter { it.start != null && kotlin.math.abs(it.start - origStart) <= tolerance }
        .minByOrNull { kotlin.math.abs(it.start!! - origStart) }
    return nearbyMatch
}

fun getSourceShortName(source: Source): String {
    return when (source) {
        Source.QM -> "QM"
        Source.KG -> "KG"
        Source.NE -> "NE"
        Source.ITUNES -> "AM"
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
