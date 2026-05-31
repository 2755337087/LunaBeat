package com.example.LyricBox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaybackStatsDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    PlaybackStatsDebugScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun PlaybackStatsDebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val statsManager = remember(context) { playbackStatsManagerOf(context) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var todayDurationMs by remember { mutableStateOf(0L) }
    var recentDaily by remember { mutableStateOf<List<DailyPlaybackStats>>(emptyList()) }
    var topSongs by remember { mutableStateOf<List<SongPlaybackStats>>(emptyList()) }
    var topArtists by remember { mutableStateOf<List<ArtistPlaybackStats>>(emptyList()) }
    var topAlbums by remember { mutableStateOf<List<AlbumPlaybackStats>>(emptyList()) }

    val todayDate = remember { formatStatsDate(System.currentTimeMillis()) }

    suspend fun reload() {
        loading = true
        errorMessage = null
        runCatching {
            todayDurationMs = statsManager.getDayTotalDurationMs(todayDate)
            recentDaily = statsManager.getRecentDailyPlaybackStats(14)
            topSongs = statsManager.getTopSongs(10)
            topArtists = statsManager.getTopArtists(10)
            topAlbums = statsManager.getTopAlbums(10)
        }.onFailure {
            errorMessage = it.message ?: "加载失败"
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "播放统计详情",
            showBack = true,
            showMenu = false,
            onBackClick = onBack,
            onMenuClick = {}
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        reload()
                    }
                },
                enabled = !loading
            ) {
                Text(if (loading) "加载中..." else "刷新统计")
            }

            Spacer(modifier = Modifier.height(12.dp))

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "今日($todayDate)播放时长: ${formatStatsDuration(todayDurationMs)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("最近14天")
            recentDaily.forEach { stat ->
                Text(
                    text = "${stat.date}  ${formatStatsDuration(stat.totalDurationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("歌曲 Top 10")
            topSongs.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. ${item.title} - ${item.artist} | ${formatStatsDuration(item.totalDurationMs)} | ${item.playCount}次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("艺人 Top 10")
            topArtists.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. ${item.artistName} | ${formatStatsDuration(item.totalDurationMs)} | ${item.playCount}次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("专辑 Top 10")
            topAlbums.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. ${item.albumName} - ${item.artistName} | ${formatStatsDuration(item.totalDurationMs)} | ${item.playCount}次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatStatsDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

private fun formatStatsDate(timeMs: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(timeMs))
}
