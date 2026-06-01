package com.example.LyricBox

import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.app.RecoverableSecurityException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuItem
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.nativeCanvas
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.PiracyChecker
import com.example.LyricBox.utils.PiracyCheckResult
import com.example.LyricBox.utils.LyricBatchEditUtils
import com.example.LyricBox.utils.LyricSaveEmbedUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

private const val PREVIEW_LYRICS_REFRESH_LOG_TAG = "PreviewLyricsRefresh"

private fun sanitizeTimingLyricLogText(value: String?, maxLength: Int = 80): String {
    val compact = value
        ?.replace("\r", " ")
        ?.replace("\n", " ")
        ?.replace("\t", " ")
        ?.trim()
        .orEmpty()
    return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
}

private fun summarizeTimingLyricsContentForLog(lyricsContent: String?): String {
    return if (lyricsContent == null) {
        "content=null"
    } else {
        "len=${lyricsContent.length} hash=${lyricsContent.hashCode()} first=\"${sanitizeTimingLyricLogText(lyricsContent)}\""
    }
}

private fun summarizeTimingLyricLinesForLog(lines: List<LyricLine>): String {
    val firstText = lines.firstOrNull()
        ?.timeUnits
        ?.joinToString(separator = "") { it.text }
        .orEmpty()
    return "count=${lines.size} hash=${lines.hashCode()} first=\"${sanitizeTimingLyricLogText(firstText)}\""
}

class LyricTimingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(newBase))
    }

    companion object {
        const val EXTRA_MEDIA_STORE_ID = "media_store_id"
        const val EXTRA_INITIAL_PLAYBACK_POSITION_MS = "initial_playback_position_ms"
        private const val KEY_SOURCE_AUDIO_PATH = "lyric_timing.source_audio_path"
        private const val KEY_SOURCE_TITLE = "lyric_timing.source_title"
        private const val KEY_SOURCE_ARTIST = "lyric_timing.source_artist"
        private const val KEY_IMPORTED_LYRICS_CONTENT = "lyric_timing.imported_lyrics_content"
        private const val KEY_IMPORTED_LYRICS_FORMAT = "lyric_timing.imported_lyrics_format"
        private const val KEY_CONVERTED_AUDIO_PATH = "lyric_timing.converted_audio_path"
        private const val KEY_HAS_LYRICS = "lyric_timing.has_lyrics"
        private const val KEY_PLAYBACK_POSITION = "lyric_timing.playback_position"
        private const val KEY_PENDING_CREATORS = "lyric_timing.pending_creators"
        private const val KEY_LYRIC_LINES = "lyric_timing.lyric_lines"
        private const val KEY_SELECTED_LINE_INDEX = "lyric_timing.selected_line_index"
        private const val KEY_SELECTED_WORD_INDEX = "lyric_timing.selected_word_index"
    }
    
    private var mediaPlayer: ExoPlayer? = null
    private val dsdTimingPlayer by lazy {
        DsdAudioTrackPlayer { state ->
            runOnUiThread {
                syncDsdTimingState(state)
            }
        }
    }
    private var showConfirmDialog by mutableStateOf(false)
    private var hasLyrics by mutableStateOf(false)
    private var showConvertDialog by mutableStateOf(false)
    private var convertProgress by mutableIntStateOf(0)
    private var convertMessage by mutableStateOf("")
    private var isConverting by mutableStateOf(false)
    private var convertedAudioPath by mutableStateOf("")
    private var audioImportCount by mutableIntStateOf(0)
    private var sourceAudioPath by mutableStateOf("")
    private var sourceMediaStoreId by mutableLongStateOf(-1L)
    private var sourceTitle by mutableStateOf("")
    private var sourceArtist by mutableStateOf("")
    private var importedLyricsContent by mutableStateOf("")
    private var importedLyricsFormat by mutableStateOf(0)
    private var playbackCompleted by mutableStateOf(false)
    private var isDsdTimingAudio by mutableStateOf(false)
    private var dsdTimingDurationMs by mutableLongStateOf(0L)
    private var pendingLyricsCreators by mutableStateOf<List<String>>(emptyList())
    private var pendingRestoreSeekMs: Long? = null
    private var lastKnownPlaybackPositionMs: Long = 0L
    private var currentLyricLinesSnapshot: List<LyricLine> = emptyList()
    private var restoredLyricLines: List<LyricLine> = emptyList()
    private var restoredSelectedLineIndex: Int = 0
    private var restoredSelectedWordIndex: Int = 0
    private var lastSelectedLineIndexSnapshot: Int = 0
    private var lastSelectedWordIndexSnapshot: Int = 0
    
    private val audioPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let {uri ->
            val fileName = getFileName(uri)
            Log.d("LyricTiming", "Selected audio: $uri, fileName: $fileName")
            loadAudio(uri)
        }
    }
    
    private fun getFileName(uri: android.net.Uri): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
    
    private fun copyUriToTempFile(uri: android.net.Uri, fileName: String): java.io.File? {
        return try {
            val tempFile = java.io.File(cacheDir, "temp_$fileName")
            contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("LyricTiming", "Failed to copy file", e)
            null
        }
    }
    
    private fun startConversion(uri: android.net.Uri, fileName: String) {
        // 兼容旧入口：已改为直解播放，不再执行转码。
        isConverting = false
        showConvertDialog = false
        convertProgress = 0
        convertMessage = "当前版本已改为直解播放，无需转码"
        convertedAudioPath = ""
        val inputFile = copyUriToTempFile(uri, fileName)
        if (inputFile == null) {
            convertMessage = "文件复制失败"
            return
        }
        loadAudioFromPath(inputFile.absolutePath)
    }
    
    private fun startConversionFromPath(path: String, fileName: String) {
        // 兼容旧入口：已改为直解播放，不再执行转码。
        isConverting = false
        showConvertDialog = false
        convertProgress = 0
        convertMessage = "当前版本已改为直解播放，无需转码"
        convertedAudioPath = ""
        val inputFile = java.io.File(path)
        if (!inputFile.exists()) {
            convertMessage = "文件不存在"
            return
        }
        loadAudioFromPath(inputFile.absolutePath)
    }

    private fun startManualConversion() {
        if (sourceAudioPath.isEmpty()) {
            return
        }
        val inputFile = java.io.File(sourceAudioPath)
        if (!inputFile.exists()) {
            return
        }
        showConvertDialog = false
        convertProgress = 0
        convertMessage = "当前版本已改为直解播放，无需转码"
        isConverting = false
        startConversionFromPath(sourceAudioPath, inputFile.name)
    }
    
    private fun loadAudio(uri: android.net.Uri) {
        val fileName = getFileName(uri)
        val cachedFile = copyUriToTempFile(uri, fileName)
        if (cachedFile != null) {
            loadAudioFromPath(cachedFile.absolutePath)
            return
        }

        val player = ensureTimingPlayer()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        playbackCompleted = false
        audioImportCount++
        applyPendingRestoreSeek()
    }

    private fun applyPendingRestoreSeek() {
        val seekPosition = pendingRestoreSeekMs ?: return
        if (seekPosition > 0L) {
            seekTimingAudioTo(seekPosition)
            lastKnownPlaybackPositionMs = seekPosition
        }
        pendingRestoreSeekMs = null
    }
    
    private fun loadAudioFromPath(path: String, updateSourcePath: Boolean = true) {
        val targetFile = java.io.File(path)
        val mediaStoreUri = resolveMediaStoreAudioUri(path, sourceMediaStoreId)
        if (!targetFile.exists() && mediaStoreUri == null) {
            Log.w("LyricTiming", "Audio file does not exist: $path")
            return
        }
        if (path.isDsfAudioPath()) {
            mediaPlayer?.pause()
            isDsdTimingAudio = true
            dsdTimingDurationMs = 0L
            if (updateSourcePath) {
                sourceAudioPath = path
            }
            dsdTimingPlayer.play(path, startPositionMs = pendingRestoreSeekMs ?: 0L, startPaused = true)
            playbackCompleted = false
            audioImportCount++
            applyPendingRestoreSeek()
            return
        }
        if (isDsdTimingAudio || dsdTimingPlayer.isActive) {
            dsdTimingPlayer.stop()
        }
        isDsdTimingAudio = false
        dsdTimingDurationMs = 0L
        val player = ensureTimingPlayer()
        val playbackUri = mediaStoreUri ?: Uri.fromFile(targetFile)
        player.setMediaItem(MediaItem.fromUri(playbackUri))
        player.prepare()
        playbackCompleted = false
        if (updateSourcePath) {
            sourceAudioPath = path
        }
        audioImportCount++
        applyPendingRestoreSeek()
    }
    
    private fun cleanConvertCache() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") || file.name.endsWith(".wav")) {
                    if (file.delete()) {
                        Log.d("LyricTiming", "Deleted cache file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LyricTiming", "Failed to clean cache", e)
        }
    }
    
    private fun getAudioDuration(): Long {
        if (isDsdTimingAudio) {
            return dsdTimingDurationMs.takeIf { it > 0L } ?: 0L
        }
        val player = mediaPlayer ?: return 0L
        val duration = runCatching { player.duration }
            .onFailure { throwable ->
                if (throwable is IllegalStateException) {
                    Log.w("LyricTiming", "getAudioDuration called before player is ready", throwable)
                } else {
                    Log.e("LyricTiming", "Failed to read audio duration", throwable)
                }
            }
            .getOrNull()
        return duration
            ?.takeIf { it != C.TIME_UNSET && it > 0L }
            ?: 0L
    }
    
    private fun setPlaybackSpeed(speed: Float) {
        if (isDsdTimingAudio) {
            Log.d("LyricTiming", "DSF timing playback does not support speed changes yet: $speed")
            return
        }
        mediaPlayer?.setPlaybackParameters(PlaybackParameters(speed))
    }

    private fun playPauseTimingAudio(play: Boolean) {
        if (isDsdTimingAudio) {
            if (play) {
                if (dsdTimingPlayer.isActive) {
                    dsdTimingPlayer.resume()
                } else if (sourceAudioPath.isNotBlank()) {
                    dsdTimingPlayer.play(
                        sourceAudioPath,
                        startPositionMs = lastKnownPlaybackPositionMs.coerceAtLeast(0L),
                        startPaused = false
                    )
                }
                playbackCompleted = false
            } else {
                dsdTimingPlayer.pause()
            }
            return
        }
        if (play) {
            mediaPlayer?.play()
            playbackCompleted = false
        } else {
            mediaPlayer?.pause()
        }
    }

    private fun reloadCurrentTimingAudio() {
        val currentAudioPath = when {
            sourceAudioPath.isNotBlank() -> sourceAudioPath
            convertedAudioPath.isNotBlank() -> convertedAudioPath
            else -> return
        }
        val resumePosition = getTimingAudioPosition().coerceAtLeast(0L)
        playPauseTimingAudio(false)
        pendingRestoreSeekMs = resumePosition
        loadAudioFromPath(
            path = currentAudioPath,
            updateSourcePath = sourceAudioPath.isBlank()
        )
    }

    private fun seekTimingAudioTo(timeMs: Long) {
        if (isDsdTimingAudio) {
            if (dsdTimingPlayer.isActive) {
                dsdTimingPlayer.seekTo(timeMs)
            }
        } else {
            mediaPlayer?.seekTo(timeMs)
        }
        lastKnownPlaybackPositionMs = timeMs
    }

    private fun getTimingAudioPosition(): Long {
        val position = if (isDsdTimingAudio) {
            dsdTimingPlayer.currentPositionMs()
        } else {
            mediaPlayer?.currentPosition?.toLong() ?: lastKnownPlaybackPositionMs
        }
        lastKnownPlaybackPositionMs = position
        return position
    }

    private fun syncDsdTimingState(state: DsdPlaybackState) {
        if (!isDsdTimingAudio || state.path != sourceAudioPath) return
        if (state.durationMs > 0L) {
            dsdTimingDurationMs = state.durationMs
        }
        lastKnownPlaybackPositionMs = state.positionMs.coerceAtLeast(0L)
        if (state.hasEnded) {
            playbackCompleted = true
        }
        if (state.errorMessage != null) {
            Log.e("LyricTiming", "DSF timing playback failed: ${state.errorMessage}")
        }
    }

    private fun resolveMediaStoreAudioUri(filePath: String, mediaStoreId: Long = -1L): Uri? {
        return try {
            if (mediaStoreId > 0L) {
                return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
            }
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DATA} = ?"
            val args = arrayOf(filePath)
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readExternalTtmlWithFallback(audioPath: String): String? {
        val ttmlFile = runCatching {
            val audioFile = File(audioPath)
            val parent = audioFile.parentFile ?: return@runCatching null
            File(parent, "${audioFile.nameWithoutExtension}.ttml")
        }.getOrNull() ?: return null

        val directRead = runCatching {
            ttmlFile.readText().takeIf { it.isNotBlank() }
        }.getOrNull()
        if (!directRead.isNullOrBlank()) return directRead

        val baseUri = MediaStore.Files.getContentUri("external")
        val contentUri = resolveMediaStoreFileUriForTtml(
            context = this,
            ttmlAbsolutePath = ttmlFile.absolutePath,
            displayName = ttmlFile.name,
            relativePath = buildRelativePathForMediaStore(ttmlFile.absolutePath)
        ) ?: return null

        return runCatching {
            contentResolver.openInputStream(contentUri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readText().takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun readLyricsForTiming(audioPath: String, mediaStoreId: Long = -1L): String? {
        val externalTtml = readExternalTtmlWithFallback(audioPath)?.takeIf { it.isNotBlank() }
        if (!externalTtml.isNullOrBlank()) return externalTtml
        return runCatching {
            com.example.LyricBox.utils.AudioMetadataReader
                .readLyrics(this, audioPath, mediaStoreId)
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun ensureTimingPlayer(): ExoPlayer {
        mediaPlayer?.let { return it }
        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }
        val player = ExoPlayer.Builder(this, renderersFactory).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("LyricTiming", "ExoPlayer error: code=${error.errorCodeName} message=${error.message}")
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        Log.d("LyricTiming", "Audio playback completed")
                        playbackCompleted = true
                    }
                }
            })
        }
        mediaPlayer = player
        return player
    }
    
    private val lyricPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let {uri ->
            // 处理歌词文件
            Log.d("LyricTiming", "Selected lyric: $uri")
            // 读取歌词文件内容
            contentResolver.openInputStream(uri)?.use {inputStream ->
                val content = inputStream.bufferedReader().readText()
                // 解析歌词，按行分割
                val lines = content.lines().filter { it.isNotBlank() }
                // 这里可以通过ViewModel或其他方式更新UI
                Log.d("LyricTiming", "Imported lyrics: $lines")
            }
        }
    }
    
    private var showVerbatimLyricsOverwriteDialog by mutableStateOf(false)
    private var verbatimLyricsLines by mutableStateOf<List<LyricLine>>(emptyList())
    private var pendingVerbatimLyricsContent by mutableStateOf("")
    private var piracyCheckResult by mutableStateOf<PiracyCheckResult?>(null)
    private var showPiracyWarning by mutableStateOf(false)
    
    private val verbatimLyricsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val lyricsContent = result.data?.getStringExtra("lyricsContent") ?: ""
            val lyricsFormat = result.data?.getStringExtra("lyricsFormat") ?: "LRC逐行/逐字歌词"
            if (lyricsContent.isNotEmpty()) {
                val resolvedFormat = resolveLyricsFormat(lyricsFormat, lyricsContent)
                if (resolvedFormat == 3) {
                    // 导入 TTML 歌词
                    // 先解析创作者信息
                    val parsedSongwriters = LyricParsingUtils.parseSongwritersFromTtml(lyricsContent)
                    pendingLyricsCreators = parsedSongwriters
                    
                    if (hasLyrics) {
                        pendingVerbatimLyricsContent = lyricsContent
                        importedLyricsFormat = 3 // 标记为 TTML 格式
                        showVerbatimLyricsOverwriteDialog = true
                    } else {
                        importTtmlLyrics(lyricsContent)
                    }
                } else {
                    // 导入普通逐字歌词
                    pendingLyricsCreators = emptyList()
                    if (hasLyrics) {
                        pendingVerbatimLyricsContent = lyricsContent
                        importedLyricsFormat = 1 // 标记为逐字 LRC 格式
                        showVerbatimLyricsOverwriteDialog = true
                    } else {
                        importVerbatimLyrics(lyricsContent)
                    }
                }
            }
        }
    }
    
    private fun importVerbatimLyrics(content: String) {
        val parseResult = LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, content)
        val parsedLyrics = parseResult.lyrics
        val parsedLines = parseResult.lyricLines
        if (parsedLyrics.isNotEmpty()) {
            verbatimLyricsLines = parsedLines
            hasLyrics = true
            pendingVerbatimLyricsContent = ""
        }
    }
    
    private fun importTtmlLyrics(content: String) {
        val parsedLines = LyricParsingUtils.parseByType(LyricParseType.TTML, content).lyricLines
        if (parsedLines.isNotEmpty()) {
            verbatimLyricsLines = parsedLines
            hasLyrics = true
            pendingVerbatimLyricsContent = ""
        }
    }
    
    private var splLrcImportResult: Pair<List<String>, List<List<Pair<String, String>>>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 进入打轴页时统一暂停系统媒体播放，避免双播放源并行。
        lifecycleScope.launch {
            val playbackController = MusicPlaybackController(applicationContext).also { it.connect() }
            repeat(80) {
                if (playbackController.isReady) {
                    playbackController.pause()
                    playbackController.release()
                    return@launch
                }
                delay(50L)
            }
            playbackController.release()
        }
        
        val checkResult = PiracyChecker.checkAll(this)
        if (checkResult.isPirated) {
            piracyCheckResult = checkResult
            showPiracyWarning = true
        }
        
        var intentAudioPath = intent.getStringExtra("audioPath") ?: ""
        val intentSourceTitle = intent.getStringExtra("sourceTitle") ?: ""
        val intentSourceArtist = intent.getStringExtra("sourceArtist") ?: ""
        val intentLyricsContent = intent.getStringExtra("lyricsContent") ?: ""
        val intentMediaStoreId = intent.getLongExtra(EXTRA_MEDIA_STORE_ID, -1L)
        val intentInitialPlaybackPositionMs = intent.getLongExtra(EXTRA_INITIAL_PLAYBACK_POSITION_MS, -1L)
        val intentLyricsFormat = resolveLyricsFormat(
            intent.getStringExtra("lyricsFormat").orEmpty(),
            intentLyricsContent
        )
        
        // 如果没有直接传入路径，尝试从外部 Intent 解析
        if (intentAudioPath.isBlank()) {
            val externalPath = handleExternalIntent(intent)
            if (externalPath != null) {
                intentAudioPath = externalPath
            }
        }

        if (savedInstanceState != null) {
            sourceAudioPath = savedInstanceState.getString(KEY_SOURCE_AUDIO_PATH, intentAudioPath)
            sourceTitle = savedInstanceState.getString(KEY_SOURCE_TITLE, intentSourceTitle)
            sourceArtist = savedInstanceState.getString(KEY_SOURCE_ARTIST, intentSourceArtist)
            sourceMediaStoreId = intentMediaStoreId
            importedLyricsContent = savedInstanceState.getString(KEY_IMPORTED_LYRICS_CONTENT, intentLyricsContent)
            importedLyricsFormat = savedInstanceState.getInt(KEY_IMPORTED_LYRICS_FORMAT, intentLyricsFormat)
            convertedAudioPath = savedInstanceState.getString(KEY_CONVERTED_AUDIO_PATH, "")
            hasLyrics = savedInstanceState.getBoolean(KEY_HAS_LYRICS, false)
            pendingLyricsCreators = savedInstanceState.getStringArrayList(KEY_PENDING_CREATORS)?.toList() ?: emptyList()
            lastKnownPlaybackPositionMs = savedInstanceState.getLong(KEY_PLAYBACK_POSITION, 0L)
            pendingRestoreSeekMs = lastKnownPlaybackPositionMs.takeIf { it > 0L }
            @Suppress("DEPRECATION")
            val savedLines = savedInstanceState.getSerializable(KEY_LYRIC_LINES)
            restoredLyricLines = if (savedLines is ArrayList<*>) {
                savedLines.filterIsInstance<LyricLine>()
            } else {
                emptyList()
            }
            restoredSelectedLineIndex = savedInstanceState.getInt(KEY_SELECTED_LINE_INDEX, 0)
            restoredSelectedWordIndex = savedInstanceState.getInt(KEY_SELECTED_WORD_INDEX, 0)
            if (restoredLyricLines.isNotEmpty()) {
                hasLyrics = true
            }
        } else {
            sourceAudioPath = intentAudioPath
            sourceTitle = intentSourceTitle
            sourceArtist = intentSourceArtist
            sourceMediaStoreId = intentMediaStoreId
            importedLyricsContent = intentLyricsContent
            importedLyricsFormat = intentLyricsFormat
            pendingRestoreSeekMs = intentInitialPlaybackPositionMs.takeIf { it >= 0L }
            lastKnownPlaybackPositionMs = pendingRestoreSeekMs ?: 0L
            restoredLyricLines = emptyList()
            restoredSelectedLineIndex = 0
            restoredSelectedWordIndex = 0
        }
        val launchSelectionPositionMs = if (savedInstanceState == null) {
            intentInitialPlaybackPositionMs.takeIf { it >= 0L } ?: -1L
        } else {
            -1L
        }
        
        // 如果是TTML歌词，解析创作者信息
        if (importedLyricsFormat == 3 && importedLyricsContent.isNotEmpty()) {
            val parsedSongwriters = LyricParsingUtils.parseSongwritersFromTtml(importedLyricsContent)
            pendingLyricsCreators = parsedSongwriters
        }
        
        val startupAudioPath = when {
            sourceAudioPath.isNotEmpty() -> sourceAudioPath
            convertedAudioPath.isNotEmpty() -> convertedAudioPath
            else -> ""
        }
        if (startupAudioPath.isNotEmpty()) {
            val file = java.io.File(startupAudioPath)
            if (file.exists()) {
                loadAudioFromPath(startupAudioPath)
            } else {
                val mediaStoreUri = resolveMediaStoreAudioUri(startupAudioPath, sourceMediaStoreId)
                if (mediaStoreUri != null) {
                    loadAudioFromPath(startupAudioPath)
                } else {
                    pendingRestoreSeekMs = null
                    lastKnownPlaybackPositionMs = 0L
                }
            }
        } else {
            pendingRestoreSeekMs = null
            lastKnownPlaybackPositionMs = 0L
        }

        if (startupAudioPath.isNotEmpty() && importedLyricsContent.isBlank()) {
            importedLyricsContent = readLyricsForTiming(startupAudioPath, sourceMediaStoreId).orEmpty()
            if (importedLyricsContent.isNotBlank()) {
                importedLyricsFormat = detectLyricsFormat(importedLyricsContent).coerceIn(0, 3)
            }
        }
        
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasLyrics) {
                    showConfirmDialog = true
                } else {
                    finishWithLyricsResult()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    LyricTimingScreen(
                        onBack = { lyricLines -> finishWithLyricsResult(lyricLines) },
                        onImportAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        onPlayPause = { play ->
                            playPauseTimingAudio(play)
                        },
                        onReloadCurrentAudio = {
                            reloadCurrentTimingAudio()
                        },
                        onSeekTo = { timeMs ->
                            seekTimingAudioTo(timeMs)
                        },
                        onSetPlaybackSpeed = { speed -> setPlaybackSpeed(speed) },
                        getCurrentPosition = {
                            getTimingAudioPosition()
                        },
                        getAudioDuration = { getAudioDuration() },
                        showConfirmDialog = showConfirmDialog,
                        onConfirmDialogChange = { showConfirmDialog = it },
                        hasLyrics = hasLyrics,
                        onHasLyricsChange = { hasLyrics = it },
                        showConvertDialog = showConvertDialog,
                        onConvertDialogChange = { showConvertDialog = it },
                        convertProgress = convertProgress,
                        convertMessage = convertMessage,
                        audioImportCount = audioImportCount,
                        sourceAudioPath = sourceAudioPath,
                        sourceMediaStoreId = sourceMediaStoreId,
                        sourceTitle = sourceTitle,
                        sourceArtist = sourceArtist,
                        importedLyricsContent = importedLyricsContent,
                        importedLyricsFormat = importedLyricsFormat,
                        convertedAudioPath = convertedAudioPath,
                        playbackCompleted = playbackCompleted,
                        onPlaybackCompletedHandled = { playbackCompleted = false },
                        showVerbatimLyricsOverwriteDialog = showVerbatimLyricsOverwriteDialog,
                        onShowVerbatimLyricsOverwriteDialogChange = { showVerbatimLyricsOverwriteDialog = it },
                        onImportVerbatimLyrics = { importVerbatimLyrics(it) },
                        onImportTtmlLyrics = { importTtmlLyrics(it) },
                        onOpenVerbatimLyrics = { keyword ->
                            val intent = Intent(this, VerbatimLyricsActivity::class.java).apply {
                                putExtra("autoSearchKeyword", keyword)
                                putExtra("audioDuration", getAudioDuration())
                            }
                            verbatimLyricsLauncher.launch(intent)
                        },
                        verbatimLyricsLines = verbatimLyricsLines,
                        onVerbatimLyricsLinesChange = { verbatimLyricsLines = it },
                        pendingVerbatimLyricsContent = pendingVerbatimLyricsContent,
                        onPendingVerbatimLyricsContentChange = { pendingVerbatimLyricsContent = it },
                        pendingLyricsCreators = pendingLyricsCreators,
                        onPendingLyricsCreatorsChange = { pendingLyricsCreators = it },
                        onStartManualConversion = { startManualConversion() },
                        initialPlaybackPositionMs = pendingRestoreSeekMs ?: lastKnownPlaybackPositionMs,
                        launchSelectionPositionMs = launchSelectionPositionMs,
                        initialLyricLines = restoredLyricLines,
                        initialSelectedLineIndex = restoredSelectedLineIndex,
                        initialSelectedWordIndex = restoredSelectedWordIndex,
                        onLyricLinesSnapshot = { currentLyricLinesSnapshot = it },
                        onSelectionSnapshot = { lineIndex, wordIndex ->
                            lastSelectedLineIndexSnapshot = lineIndex
                            lastSelectedWordIndexSnapshot = wordIndex
                        }
                    )

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

    private fun buildReturnLyricsContent(lyricLinesOverride: List<LyricLine>? = null): String? {
        val lyricLines = lyricLinesOverride ?: currentLyricLinesSnapshot
        if (lyricLines.isEmpty()) {
            return importedLyricsContent.takeIf { it.isNotBlank() }
        }
        return when (importedLyricsFormat) {
            3 -> buildTtmlContent(lyricLines, pendingLyricsCreators)
            2 -> toEnhancedLrc(lyricLines, showDuet = true)
            else -> buildSavedLyricFromLines(lyricLines)
        }.trimEnd()
    }

    private fun buildReturnLyricsFormat(): String {
        return when (importedLyricsFormat) {
            3 -> "TTML歌词"
            2 -> "增强LRC/ELRC歌词"
            else -> "LRC逐行/逐字歌词"
        }
    }

    private fun resolveLyricsFormat(formatLabel: String, lyricsContent: String): Int {
        val normalized = formatLabel.trim().lowercase()
        if (normalized.contains("ttml")) return 3
        if (normalized.contains("elrc") || normalized.contains("enhanced")) return 2
        if (normalized.contains("拡張")) return 2
        if (normalized.contains("lrc")) return 1
        if (normalized.contains("plain") || normalized.contains("text")) return 0
        if (normalized.contains("纯文本") || normalized.contains("プレーンテキスト")) return 0
        if (normalized.contains("增强")) return 2
        if (normalized.contains("ttml歌词") || normalized.contains("ttml歌詞")) return 3
        if (normalized.contains("lrc歌词") || normalized.contains("lrc歌詞") || normalized.contains("逐行") || normalized.contains("逐字")) return 1
        return detectLyricsFormat(lyricsContent).coerceIn(0, 3)
    }

    private fun finishWithLyricsResult(lyricLinesOverride: List<LyricLine>? = null) {
        if (lyricLinesOverride != null) {
            currentLyricLinesSnapshot = lyricLinesOverride
        }
        val linesForResult = lyricLinesOverride ?: currentLyricLinesSnapshot
        val returnLyricsContent = buildReturnLyricsContent(lyricLinesOverride)
        Log.d(
            PREVIEW_LYRICS_REFRESH_LOG_TAG,
            "timingFinish override=${lyricLinesOverride != null} source=$sourceAudioPath mediaStoreId=$sourceMediaStoreId format=${buildReturnLyricsFormat()} lines=${summarizeTimingLyricLinesForLog(linesForResult)} content=${summarizeTimingLyricsContentForLog(returnLyricsContent)}"
        )
        val resultIntent = Intent().apply {
            returnLyricsContent?.let { putExtra("lyricsContent", it) }
            putExtra("lyricsFormat", buildReturnLyricsFormat())
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dsdTimingPlayer.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        if (isFinishing) {
            lifecycleScope.launch(Dispatchers.IO) {
                cleanConvertCache()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SOURCE_AUDIO_PATH, sourceAudioPath)
        outState.putString(KEY_SOURCE_TITLE, sourceTitle)
        outState.putString(KEY_SOURCE_ARTIST, sourceArtist)
        outState.putString(KEY_IMPORTED_LYRICS_CONTENT, importedLyricsContent)
        outState.putInt(KEY_IMPORTED_LYRICS_FORMAT, importedLyricsFormat)
        outState.putString(KEY_CONVERTED_AUDIO_PATH, convertedAudioPath)
        outState.putBoolean(KEY_HAS_LYRICS, hasLyrics)
        outState.putStringArrayList(KEY_PENDING_CREATORS, ArrayList(pendingLyricsCreators))
        val playbackPosition = getTimingAudioPosition()
        outState.putLong(KEY_PLAYBACK_POSITION, playbackPosition)
        outState.putInt(KEY_SELECTED_LINE_INDEX, lastSelectedLineIndexSnapshot)
        outState.putInt(KEY_SELECTED_WORD_INDEX, lastSelectedWordIndexSnapshot)
        if (currentLyricLinesSnapshot.isNotEmpty()) {
            outState.putSerializable(KEY_LYRIC_LINES, ArrayList(currentLyricLinesSnapshot))
        }
    }
    
    private fun handleExternalIntent(intent: Intent): String? {
        Log.d("LyricTiming", "handleExternalIntent start: action=${intent.action}, type=${intent.type}, data=${intent.data}")
        
        // 处理 ACTION_SEND
        if (intent.action == Intent.ACTION_SEND) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null) {
                Log.d("LyricTiming", "ACTION_SEND uri=$uri")
                val resolved = getRealPathFromUri(uri)
                Log.d("LyricTiming", "ACTION_SEND resolved=$resolved")
                return resolved
            }
        }
        
        // 检查 Intent 的 data（用于 ACTION_VIEW 和 ACTION_EDIT）
        val uri = intent.data ?: return null
        Log.d("LyricTiming", "fallback intent.data uri=$uri")
        
        // 尝试从 Uri 获取真实路径
        val resolved = getRealPathFromUri(uri)
        Log.d("LyricTiming", "intent.data resolved=$resolved")
        return resolved
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        Log.d("LyricTiming", "getRealPathFromUri start: $uri")
        // file:// 直接取本地路径
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val filePath = uri.path
            if (isExistingFilePath(filePath)) {
                Log.d("LyricTiming", "resolved by file scheme path: $filePath")
                return filePath
            }
        }

        // 直接 path（部分机型 ACTION_VIEW 会给出）
        uri.path?.let { rawPath ->
            val decodedPath = Uri.decode(rawPath)
            if (isExistingFilePath(decodedPath)) {
                Log.d("LyricTiming", "resolved by decoded raw path: $decodedPath")
                return decodedPath
            }
        }

        if (!uri.scheme.equals("content", ignoreCase = true)) {
            Log.d("LyricTiming", "unsupported scheme for resolution: ${uri.scheme}")
            return null
        }

        resolvePathFromFileProviderUri(uri)?.let { providerDerivedPath ->
            if (isExistingFilePath(providerDerivedPath)) {
                Log.d("LyricTiming", "resolved by FileProvider derived path: $providerDerivedPath")
                return providerDerivedPath
            }
        }

        // 优先处理 DocumentUri（更接近真实路径）
        resolvePathFromDocumentUri(uri)?.let { documentPath ->
            if (isExistingFilePath(documentPath)) {
                Log.d("LyricTiming", "resolved by DocumentUri: $documentPath")
                return documentPath
            }
        }

        // 兜底查询
        getPathFromDataColumn(uri)?.let { dataPath ->
            if (isExistingFilePath(dataPath)) {
                Log.d("LyricTiming", "resolved by _data column: $dataPath")
                return dataPath
            }
        }
        getPathFromMediaStore(uri)?.let { mediaPath ->
            if (isExistingFilePath(mediaPath)) {
                Log.d("LyricTiming", "resolved by MediaStore lookup: $mediaPath")
                return mediaPath
            }
        }
        getPathFromDocumentsProvider(uri)?.let { providerPath ->
            if (isExistingFilePath(providerPath)) {
                Log.d("LyricTiming", "resolved by legacy DocumentsProvider: $providerPath")
                return providerPath
            }
        }

        // 解析不到真实路径时，最后兜底复制到缓存，保证可继续处理
        Log.w("LyricTiming", "无法获取外部音频真实路径，回退到缓存文件: $uri")
        val fallbackPath = copyContentUriToCache(uri)
        Log.d("LyricTiming", "fallback cache copy path=$fallbackPath")
        return fallbackPath
    }

    private fun isExistingFilePath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching { File(path).exists() && File(path).isFile }.getOrDefault(false)
    }

    private fun resolvePathFromFileProviderUri(uri: Uri): String? {
        val authority = uri.authority ?: return null
        if (!authority.contains("fileprovider", ignoreCase = true)) {
            return null
        }
        val lowerAuthority = authority.lowercase(Locale.ROOT)
        val fileProviderSuffix = ".fileprovider"
        val suffixIndex = lowerAuthority.indexOf(fileProviderSuffix)
        val senderPackage = if (suffixIndex > 0) {
            authority.substring(0, suffixIndex)
        } else {
            authority
        }.takeIf { it.isNotBlank() } ?: return null
        val segments = uri.pathSegments
        if (segments.isEmpty()) return null
        val rootName = segments.first().lowercase(Locale.ROOT)
        val relativePath = segments.drop(1).joinToString("/")
        if (relativePath.isBlank()) return null

        val externalRoot = android.os.Environment.getExternalStorageDirectory().absolutePath
        val candidates = when (rootName) {
            "external_files", "external-files", "externalfiles", "external_file", "external-file" -> listOf(
                "$externalRoot/$relativePath",
                "$externalRoot/Android/data/$senderPackage/files/$relativePath"
            )
            "external_cache", "external-cache", "externalcache" -> listOf(
                "$externalRoot/Android/data/$senderPackage/cache/$relativePath"
            )
            "external_media", "external-media", "externalmedia" -> listOf(
                "$externalRoot/Android/media/$senderPackage/$relativePath"
            )
            "external_path", "external-path", "external", "sdcard", "root" -> listOf(
                "$externalRoot/$relativePath"
            )
            "files", "internal_files", "internal-files" -> listOf(
                "/data/user/0/$senderPackage/files/$relativePath"
            )
            "cache", "internal_cache", "internal-cache" -> listOf(
                "/data/user/0/$senderPackage/cache/$relativePath"
            )
            else -> emptyList()
        }
        if (candidates.isEmpty()) {
            return null
        }
        candidates.forEach { candidate ->
            val exists = isExistingFilePath(candidate)
            if (exists) {
                return candidate
            }
        }
        return null
    }

    private fun resolvePathFromDocumentUri(uri: Uri): String? {
        return try {
            if (!android.provider.DocumentsContract.isDocumentUri(this, uri)) {
                return null
            }
            val docId = android.provider.DocumentsContract.getDocumentId(uri)
            when (uri.authority) {
                "com.android.externalstorage.documents" -> {
                    val split = docId.split(":", limit = 2)
                    if (split.size < 2) return null
                    val volume = split[0]
                    val relativePath = split[1]
                    val candidate = if (volume.equals("primary", ignoreCase = true)) {
                        "${android.os.Environment.getExternalStorageDirectory()}/$relativePath"
                    } else {
                        "/storage/$volume/$relativePath"
                    }
                    candidate
                }
                "com.android.providers.downloads.documents" -> {
                    when {
                        docId.startsWith("raw:") -> docId.removePrefix("raw:")
                        docId.startsWith("/storage/") -> docId
                        docId.toLongOrNull() != null -> {
                            val id = docId.toLong()
                            val publicUri = android.content.ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                id
                            )
                            getPathFromDataColumn(publicUri) ?: run {
                                val myUri = android.content.ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/my_downloads"),
                                    id
                                )
                                getPathFromDataColumn(myUri)
                            }
                        }
                        else -> null
                    }
                }
                "com.android.providers.media.documents" -> {
                    val split = docId.split(":")
                    if (split.size < 2) return null
                    val type = split[0]
                    val id = split[1]
                    val contentUri = when (type) {
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    } ?: return null
                    val selection = "${MediaStore.MediaColumns._ID}=?"
                    val selectionArgs = arrayOf(id)
                    queryDataColumn(contentUri, selection, selectionArgs)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("LyricTiming", "Error resolving document uri path: $uri", e)
            null
        }
    }

    private fun queryDataColumn(
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): String? {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (columnIndex >= 0) {
                        cursor.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun copyContentUriToCache(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = getFileName(uri).replace('/', '_').replace('\\', '_')
                val tempFile = File(cacheDir, fileName)
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                tempFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("LyricTiming", "Error copying file from content URI", e)
            null
        }
    }

    private fun getPathFromDataColumn(uri: Uri): String? {
        return queryDataColumn(uri)
    }

    private fun getPathFromMediaStore(uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    if (columnIndex >= 0) {
                        cursor.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getPathFromDocumentsProvider(uri: Uri): String? {
        return getPathFromDataColumn(uri)
    }
}

private fun String.isDsfAudioPath(): Boolean {
    return substringAfterLast('.', "").lowercase(Locale.ROOT) == "dsf"
}

data class LyricTimeUnit(
    val text: String,
    val startTime: String,
    val endTime: String,
    val transliteration: String = "",
    val charTransliterations: Map<Int, String> = emptyMap()
) : java.io.Serializable

enum class LyricAgentType {
    LEFT,       
    RIGHT,      
    BACKGROUND  
}

data class LyricLine(
    val timeUnits: List<LyricTimeUnit>,
    val translation: String = "",
    val agentType: LyricAgentType = LyricAgentType.LEFT,
    val lineKey: String = ""
) : java.io.Serializable

private fun isBlankLyricLine(lyricLine: LyricLine): Boolean {
    return lyricLine.timeUnits.isEmpty() || lyricLine.timeUnits.all { it.text.isBlank() }
}

private fun lyricUnitCountForTimingCheck(lyricLine: LyricLine): Int {
    return lyricLine.timeUnits.count { it.text.isNotBlank() }
}

private fun isSingleUnitLyricLineForTimingCheck(lyricLine: LyricLine): Boolean {
    return !isBlankLyricLine(lyricLine) && lyricUnitCountForTimingCheck(lyricLine) == 1
}

private fun isLineByLineTimingLine(
    lyricLines: List<LyricLine>,
    lineIndex: Int
): Boolean {
    if (lineIndex !in lyricLines.indices) return false
    val currentLine = lyricLines[lineIndex]
    if (!isSingleUnitLyricLineForTimingCheck(currentLine)) return false

    val prevLine = lyricLines.getOrNull(lineIndex - 1)
    return if (prevLine != null && !isBlankLyricLine(prevLine)) {
        isSingleUnitLyricLineForTimingCheck(prevLine)
    } else {
        val nextLine = lyricLines.getOrNull(lineIndex + 1)
        nextLine != null && isSingleUnitLyricLineForTimingCheck(nextLine)
    }
}

private fun findNextSelectableTimingUnit(
    lyricLines: List<LyricLine>,
    lineIndex: Int,
    unitIndex: Int
): Pair<Int, Int>? {
    var targetLineIndex = lineIndex
    var targetUnitIndex = unitIndex + 1

    while (targetLineIndex < lyricLines.size) {
        val timeUnits = lyricLines[targetLineIndex].timeUnits
        while (targetUnitIndex < timeUnits.size) {
            if (timeUnits[targetUnitIndex].text.isNotBlank()) {
                return targetLineIndex to targetUnitIndex
            }
            targetUnitIndex++
        }
        targetLineIndex++
        targetUnitIndex = 0
    }

    return null
}

private fun findPreviousSelectableTimingUnit(
    lyricLines: List<LyricLine>,
    lineIndex: Int,
    unitIndex: Int
): Pair<Int, Int>? {
    var targetLineIndex = lineIndex
    var targetUnitIndex = unitIndex - 1

    while (targetLineIndex >= 0) {
        val timeUnits = lyricLines[targetLineIndex].timeUnits
        while (targetUnitIndex >= 0 && targetUnitIndex < timeUnits.size) {
            if (timeUnits[targetUnitIndex].text.isNotBlank()) {
                return targetLineIndex to targetUnitIndex
            }
            targetUnitIndex--
        }
        targetLineIndex--
        targetUnitIndex = if (targetLineIndex >= 0) lyricLines[targetLineIndex].timeUnits.lastIndex else -1
    }

    return null
}

private fun findFirstSelectableTimingUnit(lyricLines: List<LyricLine>): Pair<Int, Int>? {
    lyricLines.forEachIndexed { lineIndex, lyricLine ->
        lyricLine.timeUnits.forEachIndexed { unitIndex, timeUnit ->
            if (timeUnit.text.isNotBlank()) {
                return lineIndex to unitIndex
            }
        }
    }
    return null
}

private fun findLastSelectableTimingUnit(lyricLines: List<LyricLine>): Pair<Int, Int>? {
    for (lineIndex in lyricLines.indices.reversed()) {
        val timeUnits = lyricLines[lineIndex].timeUnits
        for (unitIndex in timeUnits.indices.reversed()) {
            if (timeUnits[unitIndex].text.isNotBlank()) {
                return lineIndex to unitIndex
            }
        }
    }
    return null
}

private data class CjkTransliterationUnit(
    val startIndex: Int,
    val indices: List<Int>,
    val text: String
)

private val JapaneseSmallKanaChars = setOf(
    'ゃ', 'ゅ', 'ょ', 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ゎ', 'っ',
    'ャ', 'ュ', 'ョ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ヮ', 'ッ'
)

private val JapaneseTrailingAttachableChars = setOf(
    'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ゃ', 'ゅ', 'ょ', 'ゎ', 'ゕ', 'ゖ',
    'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ヮ', 'ヵ', 'ヶ',
    'ー'
)

private fun clearAllLyricLineTransliterations(lines: List<LyricLine>): List<LyricLine> {
    return lines.map { line ->
        val clearedUnits = line.timeUnits.map { unit ->
            unit.copy(
                transliteration = "",
                charTransliterations = emptyMap()
            )
        }
        line.copy(timeUnits = clearedUnits)
    }
}

@Composable
private fun ImportTransliterationSheetHeader(
    onImportKanaRomaClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    onClosePageClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(MenuAnchorPosition(0f, 0f)) }
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.lyric_timing_import_transliteration),
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                val centerX = bounds.center.x
                val centerY = bounds.center.y
                menuAnchor = MenuAnchorPosition(
                    x = with(density) { centerX.toDp().value },
                    y = with(density) { centerY.toDp().value }
                )
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.more),
                contentDescription = stringResource(R.string.lyric_timing_transliteration_menu)
            )
        }
    }

    CustomDropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        items = listOf(
            MenuItem(
                title = stringResource(R.string.lyric_timing_import_kana_romaji),
                onClick = {
                    showMenu = false
                    onImportKanaRomaClick()
                }
            ),
            MenuItem(
                title = stringResource(R.string.lyric_timing_delete_all_transliteration),
                onClick = {
                    showMenu = false
                    onDeleteAllClick()
                }
            ),
            MenuItem(
                title = stringResource(R.string.lyric_timing_close_page),
                onClick = {
                    showMenu = false
                    onClosePageClick()
                }
            )
        ),
        anchorPosition = menuAnchor
    )
}

@Composable
private fun ImportTranslationSheetContent(
    lyricLines: List<LyricLine>,
    translationInput: String,
    onTranslationInputChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    scrollBlocker: NestedScrollConnection
) {
    val lyricScrollState = rememberLazyListState()
    val translationScrollState = rememberScrollState()
    val translationLineCount = if (translationInput.isEmpty()) 1 else translationInput.lines().size
    val density = LocalDensity.current
    val lineHeightPx = with(density) { 20.dp.toPx() }

    LaunchedEffect(lyricScrollState.firstVisibleItemScrollOffset, lyricScrollState.firstVisibleItemIndex) {
        val targetOffset = (lyricScrollState.firstVisibleItemIndex * lineHeightPx + lyricScrollState.firstVisibleItemScrollOffset).toInt()
        if (translationScrollState.value != targetOffset) {
            translationScrollState.scrollTo(targetOffset)
        }
    }

    LaunchedEffect(translationScrollState.value) {
        val targetIndex = (translationScrollState.value / lineHeightPx).toInt()
        val targetOffset = (translationScrollState.value % lineHeightPx).toInt()
        if (lyricScrollState.firstVisibleItemIndex != targetIndex || lyricScrollState.firstVisibleItemScrollOffset != targetOffset) {
            lyricScrollState.scrollToItem(targetIndex, targetOffset)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.lyric_timing_import_translation_by_text),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(scrollBlocker)
        ) {
            LazyColumn(
                state = lyricScrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 4.dp)
                    .nestedScroll(scrollBlocker),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(lyricLines) { index, line ->
                    val lineText = line.timeUnits.joinToString("") { it.text }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = lineText,
                            fontSize = 14.sp,
                            maxLines = 1,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .verticalScroll(translationScrollState)
                        .nestedScroll(scrollBlocker)
                        .padding(start = 4.dp, top = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    repeat(maxOf(translationLineCount, lyricLines.size)) { index ->
                        Text(
                            text = "${index + 1}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .height(20.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val translationHorizontalScrollState = rememberScrollState()
                    BasicTextField(
                        value = translationInput,
                        onValueChange = onTranslationInputChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(translationScrollState)
                            .horizontalScroll(translationHorizontalScrollState)
                            .nestedScroll(scrollBlocker),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp)
                            ) {
                                if (translationInput.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.lyric_timing_translation_input_placeholder),
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onConfirmClick) {
                Text(stringResource(R.string.common_confirm))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportTranslationBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    translationInput: String,
    onTranslationInputChange: (String) -> Unit,
    onApplyTranslations: (List<String>) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet) return

    val scope = rememberCoroutineScope()
    val scrollBlocker = rememberBottomSheetListScrollBlocker()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val closeSheet = {
        hideSheetAndDismiss(scope, sheetState) {
            onDismissSheet()
        }
    }

    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        sheetMaxWidth = Dp.Unspecified,
        onDismissRequest = { closeSheet() },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        ImportTranslationSheetContent(
            lyricLines = lyricLines,
            translationInput = translationInput,
            onTranslationInputChange = onTranslationInputChange,
            scrollBlocker = scrollBlocker,
            onConfirmClick = {
                onApplyTranslations(translationInput.lines())
                closeSheet()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportTransliterationBottomSheet(
    showSheet: Boolean,
    transliterationInput: String,
    onTransliterationInputChange: (String) -> Unit,
    lyricLines: List<LyricLine>,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onResultStateChange: (Boolean, String) -> Unit,
    onShowResultDialog: () -> Unit,
    onImportKanaRomaClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val closeSheet = {
        hideSheetAndDismiss(scope, sheetState) {
            onDismissSheet()
        }
    }

    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        sheetMaxWidth = Dp.Unspecified,
        onDismissRequest = {
            closeSheet()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            ImportTransliterationSheetHeader(
                onImportKanaRomaClick = onImportKanaRomaClick,
                onDeleteAllClick = onDeleteAllClick,
                onClosePageClick = { closeSheet() }
            )
            ThemedTextField(
                value = transliterationInput,
                onValueChange = onTransliterationInputChange,
                placeholder = stringResource(R.string.lyric_timing_transliteration_input_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                singleLine = false,
                maxLines = 10
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            onTransliterationInputChange(clip.getItemAt(0).text.toString())
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_paste))
                }
                OutlinedButton(
                    onClick = { onTransliterationInputChange("") }
                ) {
                    Text(stringResource(R.string.common_clear))
                }
                Button(
                    onClick = {
                        val (transliterationMap, parseError) = parseTransliterationInput(transliterationInput)
                        if (parseError) {
                            onResultStateChange(false, context.getString(R.string.lyric_timing_parse_failed_check_format))
                            onShowResultDialog()
                            closeSheet()
                        } else if (transliterationMap.isEmpty()) {
                            onResultStateChange(false, context.getString(R.string.lyric_timing_no_transliteration_input))
                            onShowResultDialog()
                            closeSheet()
                        } else {
                            val oldLines = lyricLines.toList()
                            val (updatedLyricLines, matchCount) = applyTransliterationMapToLyrics(
                                lyricLines = lyricLines,
                                transliterationMap = transliterationMap
                            )
                            onLyricLinesChange(updatedLyricLines)

                            undoRedoManager.pushAction(
                                UndoAction(
                                    actionType = UndoActionType.MULTI_CHANGE,
                                    lineIndex = 0,
                                    oldValue = oldLines,
                                    newValue = updatedLyricLines
                                )
                            )
                            updateUndoRedoState()

                            if (matchCount == 0) {
                                onResultStateChange(false, context.getString(R.string.lyric_timing_no_matching_lyrics))
                            } else {
                                onResultStateChange(true, context.getString(R.string.lyric_timing_transliteration_success_count, matchCount))
                            }
                            onShowResultDialog()
                            closeSheet()
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteMultipleLinesBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    selectedLineIndices: Set<Int>,
    onSelectedLineIndicesChange: (Set<Int>) -> Unit,
    originalSelectedLineIndices: Set<Int>,
    selectedLineIndex: Int,
    onSelectedLineIndexChange: (Int) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet) return

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBlocker = rememberBottomSheetListScrollBlocker()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val closeSheet = {
        hideSheetAndDismiss(scope, sheetState) {
            if (selectedLineIndices != originalSelectedLineIndices) {
                onSelectedLineIndicesChange(originalSelectedLineIndices)
            }
            onDismissSheet()
        }
    }

    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        sheetMaxWidth = Dp.Unspecified,
        onDismissRequest = {
            closeSheet()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BatchEditSheetMenuHeader(
                title = stringResource(R.string.lyric_timing_delete_multiple_lines),
                onSelectAll = { onSelectedLineIndicesChange(lyricLines.indices.toSet()) },
                onInvertSelect = {
                    onSelectedLineIndicesChange(lyricLines.indices.toSet() - selectedLineIndices)
                },
                onClosePage = { closeSheet() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lyric_timing_selected_lines_count, selectedLineIndices.size),
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(stringResource(R.string.lyric_timing_select_lines_to_delete), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(scrollBlocker)
            ) {
                itemsIndexed(lyricLines) { lineIndex, line ->
                    val lineText = line.timeUnits.joinToString("") { it.text }
                    val isSelected = lineIndex in selectedLineIndices

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                onSelectedLineIndicesChange(
                                    if (isSelected) {
                                        selectedLineIndices - lineIndex
                                    } else {
                                        selectedLineIndices + lineIndex
                                    }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${lineIndex + 1}",
                            modifier = Modifier.padding(end = 8.dp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lineText,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        if (selectedLineIndices.isNotEmpty() && selectedLineIndices.size < lyricLines.size) {
                            val actions = mutableListOf<UndoAction>()
                            val sortedIndices = selectedLineIndices.sortedDescending()
                            sortedIndices.forEach { index ->
                                if (index < lyricLines.size) {
                                    actions.add(
                                        UndoAction(
                                            actionType = UndoActionType.LINE_DELETE,
                                            lineIndex = index,
                                            unitIndex = -1,
                                            oldValue = lyricLines[index],
                                            newValue = null
                                        )
                                    )
                                }
                            }
                            undoRedoManager.pushBatchAction(BatchUndoAction(actions, context.getString(R.string.lyric_timing_delete_multiple_lines)))
                            updateUndoRedoState()
                            val newLines = lyricLines.toMutableList()
                            sortedIndices.forEach { index ->
                                newLines.removeAt(index)
                            }
                            onLyricLinesChange(newLines)
                            if (selectedLineIndex >= newLines.size) {
                                onSelectedLineIndexChange(newLines.size - 1)
                            }
                        }
                        closeSheet()
                    },
                    enabled = selectedLineIndices.isNotEmpty() && selectedLineIndices.size < lyricLines.size
                ) {
                    Text(stringResource(R.string.lyric_timing_confirm_delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveLineBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    menuLineIndex: Int,
    moveLineTargetIndex: Int,
    onMoveLineTargetIndexChange: (Int) -> Unit,
    originalMoveLineTargetIndex: Int,
    moveLinePosition: Int,
    onMoveLinePositionChange: (Int) -> Unit,
    originalMoveLinePosition: Int,
    showCancelConfirm: Boolean,
    onShowCancelConfirmChange: (Boolean) -> Unit,
    pendingDismiss: Boolean,
    onPendingDismissChange: (Boolean) -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onSelectedLineIndexChange: (Int) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet && !showCancelConfirm) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollBlocker = rememberBottomSheetListScrollBlocker()

    val hasChanges = moveLineTargetIndex != originalMoveLineTargetIndex || moveLinePosition != originalMoveLinePosition
    val requestDismiss = {
        if (hasChanges) {
            onPendingDismissChange(true)
            onShowCancelConfirmChange(true)
        } else {
            onDismissSheet()
        }
    }

    if (showSheet && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
        val currentLineText = lyricLines[menuLineIndex].timeUnits.joinToString("") { it.text }
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = { requestDismiss() },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_move_line),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(R.string.lyric_timing_current_line_with_content, menuLineIndex + 1, currentLineText),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(stringResource(R.string.lyric_timing_select_target_line_to_move), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBlocker)
                ) {
                    items(lyricLines.size) { lineIndex ->
                        val line = lyricLines[lineIndex]
                        val lineText = line.timeUnits.joinToString("") { it.text }
                        val isCurrentLine = lineIndex == menuLineIndex
                        val isSelected = lineIndex == moveLineTargetIndex

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isCurrentLine -> Color(0x40FFA500)
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable(enabled = !isCurrentLine) {
                                    onMoveLineTargetIndexChange(lineIndex)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lineIndex + 1}",
                                modifier = Modifier.padding(end = 8.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isCurrentLine -> Color.White
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = lineText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isCurrentLine -> Color.White
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (isCurrentLine) {
                                Text(
                                    text = stringResource(R.string.lyric_timing_current_line_tag),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.lyric_timing_position_label), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                CustomRadioButtonGroup(
                    options = listOf(stringResource(R.string.lyric_timing_above), stringResource(R.string.lyric_timing_below)),
                    selectedIndex = moveLinePosition,
                    onSelect = onMoveLinePositionChange
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(
                        onClick = {
                            if (moveLineTargetIndex >= 0 && moveLineTargetIndex != menuLineIndex) {
                                val newLines = lyricLines.toMutableList()
                                val lineToMove = newLines[menuLineIndex]
                                newLines.removeAt(menuLineIndex)
                                val insertIndex = if (moveLineTargetIndex > menuLineIndex) {
                                    if (moveLinePosition == 0) moveLineTargetIndex - 1 else moveLineTargetIndex
                                } else {
                                    if (moveLinePosition == 0) moveLineTargetIndex else moveLineTargetIndex + 1
                                }
                                val finalIndex = insertIndex.coerceIn(0, newLines.size)
                                newLines.add(finalIndex, lineToMove)
                                onLyricLinesChange(newLines)
                                onSelectedLineIndexChange(finalIndex.coerceIn(0, newLines.size - 1))
                            }
                            onDismissSheet()
                        },
                        enabled = moveLineTargetIndex >= 0 && moveLineTargetIndex != menuLineIndex
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowCancelConfirmChange(false)
                onPendingDismissChange(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
            text = { Text(stringResource(R.string.lyric_timing_discard_move_changes_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onDismissSheet()
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    LaunchedEffect(showCancelConfirm, pendingDismiss, showSheet) {
        if (!showCancelConfirm && !pendingDismiss && showSheet) {
            scope.launch { sheetState.show() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitToMultipleLinesBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    menuLineIndex: Int,
    splitText: String,
    onSplitTextChange: (String) -> Unit,
    originalSplitText: String,
    showCancelConfirm: Boolean,
    onShowCancelConfirmChange: (Boolean) -> Unit,
    pendingDismiss: Boolean,
    onPendingDismissChange: (Boolean) -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onSelectedLineIndexChange: (Int) -> Unit,
    onSelectedWordIndexChange: (Int) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet && !showCancelConfirm) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var splitError by remember { mutableStateOf("") }

    val hasChanges = splitText != originalSplitText
    val requestDismiss = {
        if (hasChanges) {
            onPendingDismissChange(true)
            onShowCancelConfirmChange(true)
        } else {
            onDismissSheet()
            splitError = ""
        }
    }

    if (showSheet && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
        val currentLine = lyricLines[menuLineIndex]
        val splitNoNewlineError = stringResource(R.string.lyric_timing_split_no_newline_error)
        val splitNeedTwoLinesError = stringResource(R.string.lyric_timing_split_need_two_lines_error)
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = { requestDismiss() },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_split_to_multiple_lines),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(stringResource(R.string.lyric_timing_current_lyrics_label), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ThemedTextField(
                    value = splitText,
                    onValueChange = onSplitTextChange,
                    placeholder = stringResource(R.string.lyric_timing_lyrics_content_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    singleLine = false,
                    minLines = 5,
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.lyric_timing_split_multiline_hint),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (splitError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = splitError,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(
                        onClick = {
                            if (!splitText.contains("\n")) {
                                splitError = splitNoNewlineError
                                return@Button
                            }

                            val lines = splitText.split("\n").filter { it.isNotBlank() }
                            if (lines.size < 2) {
                                splitError = splitNeedTwoLinesError
                                return@Button
                            }

                            val newLines = lyricLines.toMutableList()
                            val currentTimeUnits = currentLine.timeUnits
                            val inheritedTranslation = currentLine.translation
                            val inheritedAgentType = currentLine.agentType
                            val newLyricLines = mutableListOf<LyricLine>()

                            if (currentTimeUnits.size > 1) {
                                var currentTextIndex = 0
                                lines.forEach { lineText ->
                                    val lineTimeUnits = mutableListOf<LyricTimeUnit>()
                                    var remainingText = lineText

                                    while (remainingText.isNotEmpty() && currentTextIndex < currentTimeUnits.size) {
                                        val unit = currentTimeUnits[currentTextIndex]
                                        if (remainingText.startsWith(unit.text)) {
                                            lineTimeUnits.add(unit)
                                            remainingText = remainingText.substring(unit.text.length)
                                            currentTextIndex++
                                        } else if (unit.text.startsWith(remainingText)) {
                                            val matchedPart = unit.text.substring(0, remainingText.length)
                                            val remainingPart = unit.text.substring(remainingText.length)
                                            lineTimeUnits.add(LyricTimeUnit(matchedPart, unit.startTime, unit.endTime))
                                            currentTimeUnits.toMutableList()[currentTextIndex] =
                                                LyricTimeUnit(remainingPart, unit.startTime, unit.endTime)
                                            remainingText = ""
                                        } else {
                                            lineTimeUnits.add(unit)
                                            remainingText = remainingText.substring(unit.text.length.coerceAtMost(remainingText.length))
                                            currentTextIndex++
                                        }
                                    }

                                    if (lineTimeUnits.isNotEmpty()) {
                                        newLyricLines.add(
                                            LyricLine(
                                                lineTimeUnits,
                                                inheritedTranslation,
                                                inheritedAgentType,
                                                ""
                                            )
                                        )
                                    }
                                }

                                while (currentTextIndex < currentTimeUnits.size) {
                                    val lastLine = newLyricLines.lastOrNull()
                                    if (lastLine != null) {
                                        val updatedUnits = lastLine.timeUnits.toMutableList()
                                        updatedUnits.add(currentTimeUnits[currentTextIndex])
                                        newLyricLines[newLyricLines.size - 1] = lastLine.copy(timeUnits = updatedUnits)
                                    }
                                    currentTextIndex++
                                }
                            } else {
                                lines.forEach { lineText ->
                                    newLyricLines.add(
                                        LyricLine(
                                            listOf(LyricTimeUnit(lineText, "00:00.000", "00:00.000")),
                                            inheritedTranslation,
                                            inheritedAgentType,
                                            ""
                                        )
                                    )
                                }
                            }

                            if (newLyricLines.isNotEmpty()) {
                                newLines.removeAt(menuLineIndex)
                                newLines.addAll(menuLineIndex, newLyricLines)
                                onLyricLinesChange(newLines)
                                onSelectedLineIndexChange(menuLineIndex)
                                onSelectedWordIndexChange(0)
                            }

                            onDismissSheet()
                            splitError = ""
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowCancelConfirmChange(false)
                onPendingDismissChange(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
            text = { Text(stringResource(R.string.lyric_timing_discard_lyrics_changes_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onDismissSheet()
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    LaunchedEffect(showCancelConfirm, pendingDismiss, showSheet) {
        if (!showCancelConfirm && !pendingDismiss && showSheet) {
            scope.launch { sheetState.show() }
        }
    }
}

private fun segmentUnitWithTransliteration(unit: LyricTimeUnit): List<LyricTimeUnit> {
    val segmentedWords = smartSegmentLyric(unit.text)
    if (segmentedWords.size <= 1) {
        return listOf(unit)
    }

    val startTimeMs = parseTimeToMs(unit.startTime)
    val endTimeMs = parseTimeToMs(unit.endTime)
    val totalDuration = endTimeMs - startTimeMs

    var charIndex = 0
    val wordCharRanges = segmentedWords.map { word ->
        val start = charIndex
        charIndex += word.length
        start until charIndex
    }

    fun createNewUnit(word: String, charRange: IntRange, startMs: Long, endMs: Long): LyricTimeUnit {
        val newCharTransliterations = mutableMapOf<Int, String>()
        charRange.forEachIndexed { newIdx, oldIdx ->
            unit.charTransliterations[oldIdx]?.let { translit ->
                newCharTransliterations[newIdx] = translit
            }
        }
        val newTransliteration = if (word.length == 1 && newCharTransliterations.isNotEmpty()) {
            newCharTransliterations[0] ?: ""
        } else {
            ""
        }
        return LyricTimeUnit(
            text = word,
            startTime = formatTime(startMs),
            endTime = formatTime(endMs),
            transliteration = newTransliteration,
            charTransliterations = newCharTransliterations.toMap()
        )
    }

    if (totalDuration > 0) {
        val unitCount = segmentedWords.size
        val unitDuration = totalDuration / unitCount
        return segmentedWords.mapIndexed { wordIndex, word ->
            val unitStartMs = startTimeMs + unitDuration * wordIndex
            val unitEndMs = if (wordIndex == unitCount - 1) {
                endTimeMs
            } else {
                startTimeMs + unitDuration * (wordIndex + 1)
            }
            createNewUnit(word, wordCharRanges[wordIndex], unitStartMs, unitEndMs)
        }
    }

    return segmentedWords.mapIndexed { wordIndex, word ->
        val unitStartMs = if (wordIndex == 0) startTimeMs else 0L
        val unitEndMs = if (wordIndex == segmentedWords.size - 1) endTimeMs else 0L
        createNewUnit(word, wordCharRanges[wordIndex], unitStartMs, unitEndMs)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchSegmentBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    selectedLines: Set<Int>,
    onSelectedLinesChange: (Set<Int>) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet || lyricLines.isEmpty()) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollBlocker = rememberBottomSheetListScrollBlocker()
    val context = LocalContext.current

    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        sheetMaxWidth = Dp.Unspecified,
        onDismissRequest = {
            onDismissSheet()
            onSelectedLinesChange(emptySet())
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BatchEditSheetMenuHeader(
                title = stringResource(R.string.lyric_timing_batch_segment),
                onSelectAll = { onSelectedLinesChange(lyricLines.indices.toSet()) },
                onInvertSelect = {
                    onSelectedLinesChange(lyricLines.indices.toSet() - selectedLines)
                },
                onClosePage = {
                    hideSheetAndDismiss(scope, sheetState) {
                        onDismissSheet()
                        onSelectedLinesChange(emptySet())
                    }
                }
            )
            Text(
                text = stringResource(R.string.lyric_timing_select_lines_to_segment),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(scrollBlocker)
            ) {
                items(lyricLines.size) { index ->
                    val line = lyricLines[index]
                    val lineText = line.timeUnits.joinToString("") { it.text }
                    val isSelected = selectedLines.contains(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                onSelectedLinesChange(
                                    if (isSelected) selectedLines - index else selectedLines + index
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(end = 8.dp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lineText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Button(
                    onClick = {
                        if (selectedLines.isNotEmpty()) {
                            val actions = mutableListOf<UndoAction>()
                            selectedLines.forEach { lineIdx ->
                                if (lineIdx < lyricLines.size) {
                                    val line = lyricLines[lineIdx]
                                    val newTimeUnits = line.timeUnits.flatMap { unit ->
                                        segmentUnitWithTransliteration(unit)
                                    }
                                    actions.add(
                                        UndoAction(
                                            actionType = UndoActionType.BATCH_SEGMENT,
                                            lineIndex = lineIdx,
                                            unitIndex = -1,
                                            oldValue = line.timeUnits,
                                            newValue = newTimeUnits
                                        )
                                    )
                                }
                            }
                            undoRedoManager.pushBatchAction(BatchUndoAction(actions, context.getString(R.string.lyric_timing_batch_segment)))
                            updateUndoRedoState()
                            val newLyricLines = lyricLines.mapIndexed { index, line ->
                                if (selectedLines.contains(index)) {
                                    val newTimeUnits = line.timeUnits.flatMap { unit ->
                                        segmentUnitWithTransliteration(unit)
                                    }
                                    line.copy(timeUnits = newTimeUnits)
                                } else {
                                    line
                                }
                            }
                            onLyricLinesChange(newLyricLines)
                        }
                        onDismissSheet()
                        onSelectedLinesChange(emptySet())
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MergeUnitsBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    selectedLines: Set<Int>,
    onSelectedLinesChange: (Set<Int>) -> Unit,
    mergeThreshold: Long,
    onMergeThresholdChange: (Long) -> Unit,
    showThresholdMenu: Boolean,
    onShowThresholdMenuChange: (Boolean) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet || lyricLines.isEmpty()) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollBlocker = rememberBottomSheetListScrollBlocker()
    val context = LocalContext.current
    var mergeThresholdMenuAnchor by remember { mutableStateOf(MenuAnchorPosition(0f, 0f)) }
    val density = LocalDensity.current

    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        sheetMaxWidth = Dp.Unspecified,
        onDismissRequest = {
            onDismissSheet()
            onSelectedLinesChange(emptySet())
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BatchEditSheetMenuHeader(
                title = stringResource(R.string.lyric_timing_batch_merge_units),
                onSelectAll = { onSelectedLinesChange(lyricLines.indices.toSet()) },
                onInvertSelect = {
                    onSelectedLinesChange(lyricLines.indices.toSet() - selectedLines)
                },
                onClosePage = {
                    hideSheetAndDismiss(scope, sheetState) {
                        onDismissSheet()
                        onSelectedLinesChange(emptySet())
                    }
                }
            )
            Text(
                text = stringResource(R.string.lyric_timing_select_lines_to_merge_units),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_merge_threshold_prefix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    TextButton(
                        onClick = { onShowThresholdMenuChange(true) },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            mergeThresholdMenuAnchor = MenuAnchorPosition(
                                x = with(density) { bounds.center.x.toDp().value },
                                y = with(density) { bounds.center.y.toDp().value }
                            )
                        }
                    ) {
                        Text(stringResource(R.string.lyric_timing_milliseconds_value, mergeThreshold))
                    }
                    CustomDropdownMenu(
                        expanded = showThresholdMenu,
                        onDismissRequest = { onShowThresholdMenuChange(false) },
                        items = listOf(10L, 30L, 50L, 70L, 100L).map { threshold ->
                            MenuItem(
                                title = stringResource(R.string.lyric_timing_milliseconds_value, threshold),
                                onClick = {
                                    onMergeThresholdChange(threshold)
                                    onShowThresholdMenuChange(false)
                                }
                            )
                        },
                        anchorPosition = mergeThresholdMenuAnchor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(scrollBlocker)
            ) {
                items(lyricLines.size) { index ->
                    val line = lyricLines[index]
                    val lineText = line.timeUnits.joinToString("") { it.text }
                    val isSelected = selectedLines.contains(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                onSelectedLinesChange(
                                    if (isSelected) selectedLines - index else selectedLines + index
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(end = 8.dp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lineText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Button(
                    onClick = {
                        if (selectedLines.isNotEmpty()) {
                            val actions = mutableListOf<UndoAction>()
                            selectedLines.forEach { lineIdx ->
                                if (lineIdx < lyricLines.size) {
                                    val line = lyricLines[lineIdx]
                                    actions.add(
                                        UndoAction(
                                            actionType = UndoActionType.BATCH_MERGE_UNITS,
                                            lineIndex = lineIdx,
                                            unitIndex = -1,
                                            oldValue = line.timeUnits,
                                            newValue = mergeCloseTimeUnits(line.timeUnits, mergeThreshold)
                                        )
                                    )
                                }
                            }
                            undoRedoManager.pushBatchAction(BatchUndoAction(actions, context.getString(R.string.lyric_timing_batch_merge_units)))
                            updateUndoRedoState()
                            val newLyricLines = lyricLines.mapIndexed { index, line ->
                                if (selectedLines.contains(index)) {
                                    line.copy(timeUnits = mergeCloseTimeUnits(line.timeUnits, mergeThreshold))
                                } else {
                                    line
                                }
                            }
                            onLyricLinesChange(newLyricLines)
                        }
                        onDismissSheet()
                        onSelectedLinesChange(emptySet())
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimestampShiftBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    selectedLines: Set<Int>,
    onSelectedLinesChange: (Set<Int>) -> Unit,
    shiftValue: String,
    onShiftValueChange: (String) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet || lyricLines.isEmpty()) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollBlocker = rememberBottomSheetListScrollBlocker()
    val context = LocalContext.current

    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        sheetMaxWidth = Dp.Unspecified,
        onDismissRequest = {
            onDismissSheet()
            onSelectedLinesChange(emptySet())
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BatchEditSheetMenuHeader(
                title = stringResource(R.string.lyric_timing_batch_shift_timestamp),
                onSelectAll = { onSelectedLinesChange(lyricLines.indices.toSet()) },
                onInvertSelect = {
                    onSelectedLinesChange(lyricLines.indices.toSet() - selectedLines)
                },
                onClosePage = {
                    hideSheetAndDismiss(scope, sheetState) {
                        onDismissSheet()
                        onSelectedLinesChange(emptySet())
                    }
                }
            )
            Text(
                text = stringResource(R.string.lyric_timing_select_lines_to_shift),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(scrollBlocker)
            ) {
                items(lyricLines.size) { index ->
                    val line = lyricLines[index]
                    val lineText = line.timeUnits.joinToString("") { it.text }
                    val isSelected = selectedLines.contains(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                onSelectedLinesChange(
                                    if (isSelected) selectedLines - index else selectedLines + index
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(end = 8.dp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lineText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val currentValue = shiftValue.toIntOrNull() ?: 0
                        if (currentValue >= 50) {
                            onShiftValueChange((currentValue - 50).toString())
                        }
                    },
                    modifier = Modifier.weight(0.2f)
                ) {
                    Text("-")
                }
                ThemedTextField(
                    value = shiftValue,
                    onValueChange = { input ->
                        onShiftValueChange(filterDigits(input))
                    },
                    placeholder = stringResource(R.string.lyric_timing_shift_value_placeholder),
                    modifier = Modifier.weight(0.6f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val currentValue = shiftValue.toIntOrNull() ?: 0
                        onShiftValueChange((currentValue + 50).toString())
                    },
                    modifier = Modifier.weight(0.2f)
                ) {
                    Text("+")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lyric_timing_unit_milliseconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (selectedLines.isNotEmpty()) {
                                val shiftMs = shiftValue.toLongOrNull() ?: 0L
                                val actions = mutableListOf<UndoAction>()
                                selectedLines.forEach { lineIdx ->
                                    if (lineIdx < lyricLines.size) {
                                        lyricLines[lineIdx].timeUnits.forEachIndexed { unitIdx, unit ->
                                            actions.add(
                                                UndoAction(
                                                    actionType = UndoActionType.TIME_CHANGE,
                                                    lineIndex = lineIdx,
                                                    unitIndex = unitIdx,
                                                    oldValue = unit,
                                                    newValue = unit.copy(
                                                        startTime = LyricBatchEditUtils.adjustTime(unit.startTime, -shiftMs),
                                                        endTime = LyricBatchEditUtils.adjustTime(unit.endTime, -shiftMs)
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                                undoRedoManager.pushBatchAction(BatchUndoAction(actions, context.getString(R.string.lyric_timing_batch_shift_timestamp)))
                                updateUndoRedoState()
                                onLyricLinesChange(LyricBatchEditUtils.shiftTimestamps(lyricLines, selectedLines, -shiftMs))
                            }
                            onDismissSheet()
                            onSelectedLinesChange(emptySet())
                        }
                    ) {
                        Text(stringResource(R.string.lyric_timing_shift_earlier))
                    }
                    Button(
                        onClick = {
                            if (selectedLines.isNotEmpty()) {
                                val shiftMs = shiftValue.toLongOrNull() ?: 0L
                                val actions = mutableListOf<UndoAction>()
                                selectedLines.forEach { lineIdx ->
                                    if (lineIdx < lyricLines.size) {
                                        lyricLines[lineIdx].timeUnits.forEachIndexed { unitIdx, unit ->
                                            actions.add(
                                                UndoAction(
                                                    actionType = UndoActionType.TIME_CHANGE,
                                                    lineIndex = lineIdx,
                                                    unitIndex = unitIdx,
                                                    oldValue = unit,
                                                    newValue = unit.copy(
                                                        startTime = LyricBatchEditUtils.adjustTime(unit.startTime, shiftMs),
                                                        endTime = LyricBatchEditUtils.adjustTime(unit.endTime, shiftMs)
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                                undoRedoManager.pushBatchAction(BatchUndoAction(actions, context.getString(R.string.lyric_timing_batch_shift_timestamp)))
                                updateUndoRedoState()
                                onLyricLinesChange(LyricBatchEditUtils.shiftTimestamps(lyricLines, selectedLines, shiftMs))
                            }
                            onDismissSheet()
                            onSelectedLinesChange(emptySet())
                        }
                    ) {
                        Text(stringResource(R.string.lyric_timing_shift_later))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvertToSimplifiedBottomSheet(
    showSheet: Boolean,
    lyricLines: List<LyricLine>,
    selectedLines: Set<Int>,
    onSelectedLinesChange: (Set<Int>) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    if (!showSheet || lyricLines.isEmpty()) return

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollBlocker = rememberBottomSheetListScrollBlocker()
    val context = LocalContext.current

    androidx.compose.material3.ModalBottomSheet(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        sheetMaxWidth = Dp.Unspecified,
        onDismissRequest = {
            onDismissSheet()
            onSelectedLinesChange(emptySet())
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BatchEditSheetMenuHeader(
                title = stringResource(R.string.lyric_timing_batch_convert_simplified),
                onSelectAll = { onSelectedLinesChange(lyricLines.indices.toSet()) },
                onInvertSelect = {
                    onSelectedLinesChange(lyricLines.indices.toSet() - selectedLines)
                },
                onClosePage = {
                    hideSheetAndDismiss(scope, sheetState) {
                        onDismissSheet()
                        onSelectedLinesChange(emptySet())
                    }
                }
            )
            Text(
                text = stringResource(R.string.lyric_timing_select_lines_to_convert_simplified),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(scrollBlocker)
            ) {
                items(lyricLines.size) { index ->
                    val line = lyricLines[index]
                    val lineText = line.timeUnits.joinToString("") { it.text }
                    val isSelected = selectedLines.contains(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                onSelectedLinesChange(
                                    if (isSelected) selectedLines - index else selectedLines + index
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(end = 8.dp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lineText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Button(
                    onClick = {
                        if (selectedLines.isNotEmpty()) {
                            val actions = mutableListOf<UndoAction>()
                            selectedLines.forEach { lineIdx ->
                                if (lineIdx < lyricLines.size) {
                                    lyricLines[lineIdx].timeUnits.forEachIndexed { unitIdx, unit ->
                                        val simplifiedText = LyricBatchEditUtils.toSimplifiedText(unit.text)
                                        if (simplifiedText != unit.text) {
                                            actions.add(
                                                UndoAction(
                                                    actionType = UndoActionType.BATCH_CONVERT_TO_SIMPLIFIED,
                                                    lineIndex = lineIdx,
                                                    unitIndex = unitIdx,
                                                    oldValue = unit,
                                                    newValue = unit.copy(text = simplifiedText)
                                                )
                                            )
                                        }
                                    }
                                    val oldTranslation = lyricLines[lineIdx].translation
                                    val newTranslation = LyricBatchEditUtils.toSimplifiedText(oldTranslation)
                                    if (newTranslation != oldTranslation) {
                                        actions.add(
                                            UndoAction(
                                                actionType = UndoActionType.BATCH_CONVERT_TO_SIMPLIFIED,
                                                lineIndex = lineIdx,
                                                unitIndex = -1,
                                                oldValue = oldTranslation,
                                                newValue = newTranslation
                                            )
                                        )
                                    }
                                }
                            }
                            if (actions.isNotEmpty()) {
                                undoRedoManager.pushBatchAction(BatchUndoAction(actions, context.getString(R.string.lyric_timing_batch_convert_simplified)))
                                updateUndoRedoState()
                            }
                            onLyricLinesChange(LyricBatchEditUtils.convertToSimplified(lyricLines, selectedLines))
                        }
                        onDismissSheet()
                        onSelectedLinesChange(emptySet())
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportLyricsBottomSheets(
    showLyricInputDialog: Boolean,
    onShowLyricInputDialogChange: (Boolean) -> Unit,
    lyricInput: String,
    onLyricInputChange: (String) -> Unit,
    useSpaceSplit: Boolean,
    onUseSpaceSplitChange: (Boolean) -> Unit,
    showImportExampleDialog: Boolean,
    onShowImportExampleDialogChange: (Boolean) -> Unit,
    showCancelLyricInputConfirm: Boolean,
    onShowCancelLyricInputConfirmChange: (Boolean) -> Unit,
    pendingLyricInputDismiss: Boolean,
    onPendingLyricInputDismissChange: (Boolean) -> Unit,
    showSPLLrcInputDialog: Boolean,
    onShowSPLLrcInputDialogChange: (Boolean) -> Unit,
    splLrcInput: String,
    onSplLrcInputChange: (String) -> Unit,
    showCancelSpllrcInputConfirm: Boolean,
    onShowCancelSpllrcInputConfirmChange: (Boolean) -> Unit,
    pendingSpllrcInputDismiss: Boolean,
    onPendingSpllrcInputDismissChange: (Boolean) -> Unit,
    showElrcInputDialog: Boolean,
    onShowElrcInputDialogChange: (Boolean) -> Unit,
    elrcInput: String,
    onElrcInputChange: (String) -> Unit,
    showCancelElrcInputConfirm: Boolean,
    onShowCancelElrcInputConfirmChange: (Boolean) -> Unit,
    pendingElrcInputDismiss: Boolean,
    onPendingElrcInputDismissChange: (Boolean) -> Unit,
    showTtmlInputDialog: Boolean,
    onShowTtmlInputDialogChange: (Boolean) -> Unit,
    ttmlInput: String,
    onTtmlInputChange: (String) -> Unit,
    showCancelTtmlInputConfirm: Boolean,
    onShowCancelTtmlInputConfirmChange: (Boolean) -> Unit,
    pendingTtmlInputDismiss: Boolean,
    onPendingTtmlInputDismissChange: (Boolean) -> Unit,
    onApplyLyricInput: (String, Boolean) -> Unit,
    onApplySplInput: (String) -> Unit,
    onApplyElrcInput: (String) -> Unit,
    onApplyTtmlInput: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val lyricInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showLyricInputDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (lyricInput.isNotEmpty()) {
                    onPendingLyricInputDismissChange(true)
                    onShowCancelLyricInputConfirmChange(true)
                } else {
                    onShowLyricInputDialogChange(false)
                }
            },
            sheetState = lyricInputSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                var lyricInputMenuExpanded by remember { mutableStateOf(false) }
                var lyricInputMenuAnchor by remember { mutableStateOf<MenuAnchorPosition?>(null) }
                val density = LocalDensity.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_import_lyrics),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { lyricInputMenuExpanded = true },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            lyricInputMenuAnchor = MenuAnchorPosition(
                                x = with(density) { bounds.center.x.toDp().value },
                                y = with(density) { bounds.center.y.toDp().value }
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.more),
                            contentDescription = stringResource(R.string.common_more),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                CustomDropdownMenu(
                    expanded = lyricInputMenuExpanded,
                    onDismissRequest = { lyricInputMenuExpanded = false },
                    items = listOf(
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_import_example),
                            onClick = {
                                lyricInputMenuExpanded = false
                                onShowImportExampleDialogChange(true)
                            }
                        ),
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_close_page),
                            onClick = {
                                lyricInputMenuExpanded = false
                                if (lyricInput.isNotEmpty()) {
                                    onPendingLyricInputDismissChange(true)
                                    onShowCancelLyricInputConfirmChange(true)
                                } else {
                                    onShowLyricInputDialogChange(false)
                                }
                            }
                        )
                    ),
                    anchorPosition = lyricInputMenuAnchor ?: MenuAnchorPosition(0f, 0f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                ThemedTextField(
                    value = lyricInput,
                    onValueChange = onLyricInputChange,
                    placeholder = stringResource(R.string.lyric_timing_import_plaintext_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 360.dp),
                    singleLine = false,
                    maxLines = Int.MAX_VALUE,
                    minLines = 8
                )
                Spacer(modifier = Modifier.height(8.dp))
                CustomCheckbox(
                    checked = useSpaceSplit,
                    onCheckedChange = onUseSpaceSplitChange,
                    label = stringResource(R.string.lyric_timing_use_space_split_advanced)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text(
                        text = if (useSpaceSplit) stringResource(R.string.lyric_timing_space_split_hint_enabled)
                        else stringResource(R.string.lyric_timing_space_split_hint_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                onLyricInputChange(clip.getItemAt(0).text.toString())
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_paste))
                    }
                    OutlinedButton(onClick = { onLyricInputChange("") }) {
                        Text(stringResource(R.string.common_clear))
                    }
                    Button(
                        onClick = {
                            onApplyLyricInput(lyricInput, useSpaceSplit)
                            onShowLyricInputDialogChange(false)
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelLyricInputConfirm) {
        AlertDialog(
            onDismissRequest = { onShowCancelLyricInputConfirmChange(false) },
            title = { Text(stringResource(R.string.lyric_timing_confirm_cancel_title)) },
            text = { Text(stringResource(R.string.lyric_timing_confirm_cancel_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelLyricInputConfirmChange(false)
                        coroutineScope.launch {
                            lyricInputSheetState.show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelLyricInputConfirmChange(false)
                        onShowLyricInputDialogChange(false)
                        onLyricInputChange("")
                        onPendingLyricInputDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    ImportExampleDialog(
        showDialog = showImportExampleDialog,
        onDismiss = { onShowImportExampleDialogChange(false) }
    )

    val splLrcInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showSPLLrcInputDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (splLrcInput.isNotEmpty()) {
                    onPendingSpllrcInputDismissChange(true)
                    onShowCancelSpllrcInputConfirmChange(true)
                } else {
                    onShowSPLLrcInputDialogChange(false)
                }
            },
            sheetState = splLrcInputSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_import_lrc_line_word),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ThemedTextField(
                    value = splLrcInput,
                    onValueChange = onSplLrcInputChange,
                    placeholder = stringResource(R.string.lyric_timing_import_lrc_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    singleLine = false,
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                onSplLrcInputChange(clip.getItemAt(0).text.toString())
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_paste))
                    }
                    OutlinedButton(onClick = { onSplLrcInputChange("") }) {
                        Text(stringResource(R.string.common_clear))
                    }
                    Button(
                        onClick = {
                            onApplySplInput(splLrcInput)
                            onShowSPLLrcInputDialogChange(false)
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelSpllrcInputConfirm) {
        AlertDialog(
            onDismissRequest = { onShowCancelSpllrcInputConfirmChange(false) },
            title = { Text(stringResource(R.string.lyric_timing_confirm_cancel_title)) },
            text = { Text(stringResource(R.string.lyric_timing_confirm_cancel_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelSpllrcInputConfirmChange(false)
                        coroutineScope.launch {
                            splLrcInputSheetState.show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelSpllrcInputConfirmChange(false)
                        onShowSPLLrcInputDialogChange(false)
                        onSplLrcInputChange("")
                        onPendingSpllrcInputDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    val elrcInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showElrcInputDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (elrcInput.isNotEmpty()) {
                    onPendingElrcInputDismissChange(true)
                    onShowCancelElrcInputConfirmChange(true)
                } else {
                    onShowElrcInputDialogChange(false)
                }
            },
            sheetState = elrcInputSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_import_elrc_word),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ThemedTextField(
                    value = elrcInput,
                    onValueChange = onElrcInputChange,
                    placeholder = stringResource(R.string.lyric_timing_import_elrc_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    singleLine = false,
                    maxLines = 15
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.lyric_timing_import_elrc_hint),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                onElrcInputChange(clip.getItemAt(0).text.toString())
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_paste))
                    }
                    OutlinedButton(onClick = { onElrcInputChange("") }) {
                        Text(stringResource(R.string.common_clear))
                    }
                    Button(
                        onClick = {
                            onApplyElrcInput(elrcInput)
                            onShowElrcInputDialogChange(false)
                            onElrcInputChange("")
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelElrcInputConfirm) {
        AlertDialog(
            onDismissRequest = { onShowCancelElrcInputConfirmChange(false) },
            title = { Text(stringResource(R.string.lyric_timing_confirm_cancel_title)) },
            text = { Text(stringResource(R.string.lyric_timing_confirm_cancel_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelElrcInputConfirmChange(false)
                        coroutineScope.launch {
                            elrcInputSheetState.show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelElrcInputConfirmChange(false)
                        onShowElrcInputDialogChange(false)
                        onElrcInputChange("")
                        onPendingElrcInputDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    val ttmlInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showTtmlInputDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (ttmlInput.isNotEmpty()) {
                    onPendingTtmlInputDismissChange(true)
                    onShowCancelTtmlInputConfirmChange(true)
                } else {
                    onShowTtmlInputDialogChange(false)
                }
            },
            sheetState = ttmlInputSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_import_ttml),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ThemedTextField(
                    value = ttmlInput,
                    onValueChange = onTtmlInputChange,
                    placeholder = stringResource(R.string.lyric_timing_import_ttml_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    singleLine = false,
                    maxLines = 15
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.lyric_timing_import_ttml_hint),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                onTtmlInputChange(clip.getItemAt(0).text.toString())
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_paste))
                    }
                    OutlinedButton(onClick = { onTtmlInputChange("") }) {
                        Text(stringResource(R.string.common_clear))
                    }
                    Button(
                        onClick = {
                            onApplyTtmlInput(ttmlInput)
                            onShowTtmlInputDialogChange(false)
                            onTtmlInputChange("")
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelTtmlInputConfirm) {
        AlertDialog(
            onDismissRequest = { onShowCancelTtmlInputConfirmChange(false) },
            title = { Text(stringResource(R.string.lyric_timing_confirm_cancel_title)) },
            text = { Text(stringResource(R.string.lyric_timing_confirm_cancel_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelTtmlInputConfirmChange(false)
                        coroutineScope.launch {
                            ttmlInputSheetState.show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelTtmlInputConfirmChange(false)
                        onShowTtmlInputDialogChange(false)
                        onTtmlInputChange("")
                        onPendingTtmlInputDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetTimestampBottomSheet(
    showSheet: Boolean,
    menuLineIndex: Int,
    onMenuLineIndexChange: (Int) -> Unit,
    menuUnitIndex: Int,
    onMenuUnitIndexChange: (Int) -> Unit,
    lyricLines: List<LyricLine>,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onSelectedLineIndexChange: (Int) -> Unit,
    onSelectedWordIndexChange: (Int) -> Unit,
    tempStartTime: String,
    onTempStartTimeChange: (String) -> Unit,
    tempEndTime: String,
    onTempEndTimeChange: (String) -> Unit,
    originalTempStartTime: String,
    onOriginalTempStartTimeChange: (String) -> Unit,
    originalTempEndTime: String,
    onOriginalTempEndTimeChange: (String) -> Unit,
    showCancelConfirm: Boolean,
    onShowCancelConfirmChange: (Boolean) -> Unit,
    pendingDismiss: Boolean,
    onPendingDismissChange: (Boolean) -> Unit,
    showTimeEditor: Boolean,
    onShowTimeEditorChange: (Boolean) -> Unit,
    editingStartTime: Boolean,
    onEditingStartTimeChange: (Boolean) -> Unit,
    showSwitchUnitConfirm: Boolean,
    onShowSwitchUnitConfirmChange: (Boolean) -> Unit,
    targetUnitInfo: Pair<Int, Int>?,
    onTargetUnitInfoChange: ((Pair<Int, Int>?) -> Unit),
    onSeekTo: (Long) -> Unit,
    onPlayPause: (Boolean) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onDismissSheet: () -> Unit
) {
    val setTimestampSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val canShowSheet = showSheet &&
        menuLineIndex >= 0 && menuLineIndex < lyricLines.size &&
        menuUnitIndex >= 0 && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size

    if (canShowSheet) {
        val currentUnit = lyricLines[menuLineIndex].timeUnits[menuUnitIndex]
        var editingTimeInSheet by remember { mutableStateOf("") }
        var shiftValue by remember { mutableStateOf("50") }
        var isPlaying by remember { mutableStateOf(false) }
        var saveButtonText by remember { mutableStateOf("保存") }
        var linkToPrevEndTime by remember { mutableStateOf(false) }
        var linkToNextStartTime by remember { mutableStateOf(false) }

        val prevUnitInfo: Pair<Int, Int>? = findPreviousSelectableTimingUnit(lyricLines, menuLineIndex, menuUnitIndex)
        val nextUnitInfo: Pair<Int, Int>? = findNextSelectableTimingUnit(lyricLines, menuLineIndex, menuUnitIndex)

        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (tempStartTime != originalTempStartTime || tempEndTime != originalTempEndTime) {
                    onPendingDismissChange(true)
                    onShowCancelConfirmChange(true)
                } else {
                    onDismissSheet()
                    onShowTimeEditorChange(false)
                }
            },
            sheetState = setTimestampSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_set_timestamp),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (!showTimeEditor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    val hasUnsavedChanges = tempStartTime != originalTempStartTime || tempEndTime != originalTempEndTime
                                    if (hasUnsavedChanges) {
                                        onTargetUnitInfoChange(prevUnitInfo)
                                        onShowSwitchUnitConfirmChange(true)
                                    } else {
                                        prevUnitInfo?.let { (lineIdx, unitIdx) ->
                                            onMenuLineIndexChange(lineIdx)
                                            onMenuUnitIndexChange(unitIdx)
                                            onSelectedLineIndexChange(lineIdx)
                                            onSelectedWordIndexChange(unitIdx)
                                            val unit = lyricLines[lineIdx].timeUnits[unitIdx]
                                            onTempStartTimeChange(unit.startTime)
                                            onTempEndTimeChange(unit.endTime)
                                            onOriginalTempStartTimeChange(unit.startTime)
                                            onOriginalTempEndTimeChange(unit.endTime)
                                            linkToPrevEndTime = false
                                            linkToNextStartTime = false
                                        }
                                    }
                                },
                                enabled = !isPlaying && prevUnitInfo != null
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.prel),
                                    contentDescription = stringResource(R.string.common_previous),
                                    tint = if (isPlaying || prevUnitInfo == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            TextButton(
                                onClick = {
                                    isPlaying = true
                                    val startMs = parseTimeToMs(tempStartTime)
                                    val endMs = parseTimeToMs(tempEndTime)
                                    onSeekTo(startMs)
                                    onPlayPause(true)
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        onPlayPause(false)
                                        isPlaying = false
                                    }, endMs - startMs)
                                },
                                enabled = !isPlaying
                            ) {
                                Text(
                                    text = "▶",
                                    color = if (isPlaying) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(
                                onClick = {
                                    val hasUnsavedChanges = tempStartTime != originalTempStartTime || tempEndTime != originalTempEndTime
                                    if (hasUnsavedChanges) {
                                        onTargetUnitInfoChange(nextUnitInfo)
                                        onShowSwitchUnitConfirmChange(true)
                                    } else {
                                        nextUnitInfo?.let { (lineIdx, unitIdx) ->
                                            onMenuLineIndexChange(lineIdx)
                                            onMenuUnitIndexChange(unitIdx)
                                            onSelectedLineIndexChange(lineIdx)
                                            onSelectedWordIndexChange(unitIdx)
                                            val unit = lyricLines[lineIdx].timeUnits[unitIdx]
                                            onTempStartTimeChange(unit.startTime)
                                            onTempEndTimeChange(unit.endTime)
                                            onOriginalTempStartTimeChange(unit.startTime)
                                            onOriginalTempEndTimeChange(unit.endTime)
                                            linkToPrevEndTime = false
                                            linkToNextStartTime = false
                                        }
                                    }
                                },
                                enabled = !isPlaying && nextUnitInfo != null
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.nextl),
                                    contentDescription = stringResource(R.string.common_next),
                                    tint = if (isPlaying || nextUnitInfo == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (!showTimeEditor) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                onEditingStartTimeChange(true)
                                editingTimeInSheet = tempStartTime
                                onShowTimeEditorChange(true)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.lyric_timing_start_time_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.3f)
                        )
                        Text(
                            text = tempStartTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.lyric_timing_lyrics_text_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.3f)
                        )
                        Text(
                            text = currentUnit.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                onEditingStartTimeChange(false)
                                editingTimeInSheet = tempEndTime
                                onShowTimeEditorChange(true)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.lyric_timing_end_time_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.3f)
                        )
                        Text(
                            text = tempEndTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    if (prevUnitInfo != null) {
                        CustomCheckbox(
                            checked = linkToPrevEndTime,
                            onCheckedChange = { linkToPrevEndTime = it },
                            label = stringResource(R.string.lyric_timing_apply_start_to_prev_end)
                        )
                    }
                    if (prevUnitInfo != null && nextUnitInfo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (nextUnitInfo != null) {
                        CustomCheckbox(
                            checked = linkToNextStartTime,
                            onCheckedChange = { linkToNextStartTime = it },
                            label = stringResource(R.string.lyric_timing_apply_end_to_next_start)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = {
                                val updatedLines = lyricLines.toMutableList()
                                val currentTimeUnits = updatedLines[menuLineIndex].timeUnits.toMutableList()
                                val oldUnit = currentTimeUnits[menuUnitIndex]
                                val newUnit = currentUnit.copy(
                                    startTime = tempStartTime,
                                    endTime = tempEndTime
                                )
                                undoRedoManager.pushAction(
                                    UndoAction(
                                        actionType = UndoActionType.TIME_CHANGE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = menuUnitIndex,
                                        oldValue = oldUnit,
                                        newValue = newUnit
                                    )
                                )
                                currentTimeUnits[menuUnitIndex] = newUnit
                                updatedLines[menuLineIndex] = updatedLines[menuLineIndex].copy(timeUnits = currentTimeUnits)

                                if (linkToPrevEndTime && prevUnitInfo != null) {
                                    val (prevLineIdx, prevUnitIdx) = prevUnitInfo
                                    val prevTimeUnits = updatedLines[prevLineIdx].timeUnits.toMutableList()
                                    val prevOldUnit = prevTimeUnits[prevUnitIdx]
                                    val prevNewUnit = prevTimeUnits[prevUnitIdx].copy(endTime = tempStartTime)
                                    undoRedoManager.pushAction(
                                        UndoAction(
                                            actionType = UndoActionType.TIME_CHANGE,
                                            lineIndex = prevLineIdx,
                                            unitIndex = prevUnitIdx,
                                            oldValue = prevOldUnit,
                                            newValue = prevNewUnit
                                        )
                                    )
                                    prevTimeUnits[prevUnitIdx] = prevNewUnit
                                    updatedLines[prevLineIdx] = updatedLines[prevLineIdx].copy(timeUnits = prevTimeUnits)
                                }

                                if (linkToNextStartTime && nextUnitInfo != null) {
                                    val (nextLineIdx, nextUnitIdx) = nextUnitInfo
                                    val nextTimeUnits = updatedLines[nextLineIdx].timeUnits.toMutableList()
                                    val nextOldUnit = nextTimeUnits[nextUnitIdx]
                                    val nextNewUnit = nextTimeUnits[nextUnitIdx].copy(startTime = tempEndTime)
                                    undoRedoManager.pushAction(
                                        UndoAction(
                                            actionType = UndoActionType.TIME_CHANGE,
                                            lineIndex = nextLineIdx,
                                            unitIndex = nextUnitIdx,
                                            oldValue = nextOldUnit,
                                            newValue = nextNewUnit
                                        )
                                    )
                                    nextTimeUnits[nextUnitIdx] = nextNewUnit
                                    updatedLines[nextLineIdx] = updatedLines[nextLineIdx].copy(timeUnits = nextTimeUnits)
                                }

                                updateUndoRedoState()
                                onLyricLinesChange(updatedLines)
                                onOriginalTempStartTimeChange(tempStartTime)
                                onOriginalTempEndTimeChange(tempEndTime)
                                linkToPrevEndTime = false
                                linkToNextStartTime = false
                                saveButtonText = "已保存"
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    saveButtonText = "保存"
                                }, 1000)
                            },
                            modifier = Modifier.widthIn(min = 80.dp)
                        ) {
                            Box(
                                modifier = Modifier.widthIn(min = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(saveButtonText)
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (editingStartTime) "编辑开始时间" else "编辑结束时间",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = editingTimeInSheet,
                        onValueChange = { editingTimeInSheet = it },
                        placeholder = stringResource(R.string.lyric_timing_time_format_placeholder),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val currentValue = shiftValue.toIntOrNull() ?: 0
                                if (currentValue >= 50) {
                                    shiftValue = (currentValue - 50).toString()
                                }
                            },
                            modifier = Modifier.weight(0.2f)
                        ) {
                            Text("-")
                        }
                        ThemedTextField(
                            value = shiftValue,
                            onValueChange = { shiftValue = filterDigits(it) },
                            placeholder = stringResource(R.string.lyric_timing_shift_value_placeholder),
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val currentValue = shiftValue.toIntOrNull() ?: 0
                                shiftValue = (currentValue + 50).toString()
                            },
                            modifier = Modifier.weight(0.2f)
                        ) {
                            Text("+")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lyric_timing_unit_milliseconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val shiftMs = shiftValue.toLongOrNull() ?: 0L
                                editingTimeInSheet = adjustTime(editingTimeInSheet, -shiftMs)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.lyric_timing_shift_earlier)) }
                        Button(
                            onClick = {
                                val shiftMs = shiftValue.toLongOrNull() ?: 0L
                                editingTimeInSheet = adjustTime(editingTimeInSheet, shiftMs)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.lyric_timing_shift_later)) }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { onShowTimeEditorChange(false) }) {
                            Text(stringResource(R.string.common_back))
                        }
                        Button(
                            onClick = {
                                if (editingStartTime) {
                                    onTempStartTimeChange(editingTimeInSheet)
                                } else {
                                    onTempEndTimeChange(editingTimeInSheet)
                                }
                                onShowTimeEditorChange(false)
                            }
                        ) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowCancelConfirmChange(false)
                onPendingDismissChange(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
            text = { Text("您已修改了时间，确定要放弃修改吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onDismissSheet()
                        onPendingDismissChange(false)
                        onShowTimeEditorChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    if (showSwitchUnitConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowSwitchUnitConfirmChange(false)
                onTargetUnitInfoChange(null)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.lyric_timing_unsaved_data_title)) },
            text = { Text(stringResource(R.string.lyric_timing_unsaved_data_switch_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowSwitchUnitConfirmChange(false)
                        onTargetUnitInfoChange(null)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowSwitchUnitConfirmChange(false)
                        targetUnitInfo?.let { (lineIdx, unitIdx) ->
                            onMenuLineIndexChange(lineIdx)
                            onMenuUnitIndexChange(unitIdx)
                            onSelectedLineIndexChange(lineIdx)
                            onSelectedWordIndexChange(unitIdx)
                            val unit = lyricLines[lineIdx].timeUnits[unitIdx]
                            onTempStartTimeChange(unit.startTime)
                            onTempEndTimeChange(unit.endTime)
                            onOriginalTempStartTimeChange(unit.startTime)
                            onOriginalTempEndTimeChange(unit.endTime)
                        }
                        onTargetUnitInfoChange(null)
                    }
                ) {
                    Text("切换")
                }
            }
        )
    }

    LaunchedEffect(showCancelConfirm, pendingDismiss, showSheet) {
        if (!showCancelConfirm && !pendingDismiss && showSheet) {
            scope.launch { setTimestampSheetState.show() }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SplitLyricBottomSheet(
    showSheet: Boolean,
    menuLineIndex: Int,
    menuUnitIndex: Int,
    lyricLines: List<LyricLine>,
    splitLyricText: String,
    onSplitLyricTextChange: (String) -> Unit,
    originalSplitLyricText: String,
    showCancelConfirm: Boolean,
    onShowCancelConfirmChange: (Boolean) -> Unit,
    pendingDismiss: Boolean,
    onPendingDismissChange: (Boolean) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    val splitLyricSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showSheet) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (splitLyricText != originalSplitLyricText) {
                    onPendingDismissChange(true)
                    onShowCancelConfirmChange(true)
                } else {
                    onDismissSheet()
                }
            },
            sheetState = splitLyricSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_split_lyrics),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(stringResource(R.string.lyric_timing_split_lyrics_by_space), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ThemedTextField(
                    value = splitLyricText,
                    onValueChange = { onSplitLyricTextChange(it) },
                    placeholder = stringResource(R.string.lyric_timing_lyrics_content_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.lyric_timing_split_lyrics_hint), fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = {
                            val segmentedWords = smartSegmentLyric(splitLyricText)
                            onSplitLyricTextChange(segmentedWords.joinToString(" "))
                        }
                    ) {
                        Text(stringResource(R.string.lyric_timing_batch_segment), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (menuLineIndex < lyricLines.size && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
                                val currentUnit = lyricLines[menuLineIndex].timeUnits[menuUnitIndex]
                                val startMs = parseTimeToMs(currentUnit.startTime)
                                val endMs = parseTimeToMs(currentUnit.endTime)
                                val totalDuration = endMs - startMs

                                val normalizedText = splitLyricText
                                    .replace('\u00A0', ' ')
                                    .replace('\u2009', ' ')
                                val segments = mutableListOf<String>()
                                var currentSegment = ""
                                var index = 0

                                while (index < normalizedText.length) {
                                    val char = normalizedText[index]
                                    if (char == ' ') {
                                        if (currentSegment.isNotEmpty()) {
                                            segments.add(currentSegment)
                                            currentSegment = ""
                                        }
                                        index++
                                        var extraSpaces = ""
                                        while (index < normalizedText.length && normalizedText[index] == ' ') {
                                            extraSpaces += ' '
                                            index++
                                        }
                                        if (index < normalizedText.length) {
                                            currentSegment = extraSpaces
                                        }
                                    } else {
                                        currentSegment += char
                                        index++
                                    }
                                }
                                if (currentSegment.isNotEmpty()) {
                                    segments.add(currentSegment)
                                }

                                if (segments.isNotEmpty()) {
                                    val newTimeUnits = mutableListOf<LyricTimeUnit>()
                                    val segmentWeights = segments.map { segment ->
                                        smartSegmentLyric(segment).size
                                    }
                                    val totalWeight = segmentWeights.sum()
                                    var sourceCharCursor = 0
                                    var accumulatedMs = startMs
                                    val sourceText = currentUnit.text
                                        .replace('\u00A0', ' ')
                                        .replace('\u2009', ' ')

                                    segments.forEachIndexed { segmentIndex, segment ->
                                        val weight = segmentWeights[segmentIndex]
                                        val unitDuration = if (totalWeight > 0) {
                                            (totalDuration * weight) / totalWeight
                                        } else {
                                            totalDuration / segments.size
                                        }
                                        val unitStartMs = accumulatedMs
                                        val unitEndMs = if (segmentIndex == segments.size - 1) endMs else accumulatedMs + unitDuration
                                        accumulatedMs = unitEndMs

                                        // 按原始单元文本做顺序匹配，避免前导空格/分隔空格变化导致注音索引错位。
                                        val sourceIndices = mutableListOf<Int>()
                                        var searchCursor = sourceCharCursor
                                        segment.forEachIndexed { _, ch ->
                                            while (searchCursor < sourceText.length && sourceText[searchCursor] != ch) {
                                                searchCursor++
                                            }
                                            if (searchCursor < sourceText.length) {
                                                sourceIndices.add(searchCursor)
                                                searchCursor++
                                            } else {
                                                sourceIndices.add(-1)
                                            }
                                        }
                                        sourceCharCursor = searchCursor

                                        val mappedCharTransliterations = mutableMapOf<Int, String>()
                                        sourceIndices.forEachIndexed { newIdx, oldIdx ->
                                            if (oldIdx >= 0) {
                                                currentUnit.charTransliterations[oldIdx]?.let { translit ->
                                                    mappedCharTransliterations[newIdx] = translit
                                                }
                                            }
                                        }

                                        val mappedTransliteration = when {
                                            mappedCharTransliterations.isNotEmpty() && segment.length == 1 -> mappedCharTransliterations[0] ?: ""
                                            segments.size == 1 -> currentUnit.transliteration
                                            else -> ""
                                        }

                                        newTimeUnits.add(
                                            LyricTimeUnit(
                                                text = segment,
                                                startTime = formatTime(unitStartMs),
                                                endTime = formatTime(unitEndMs),
                                                transliteration = mappedTransliteration,
                                                charTransliterations = mappedCharTransliterations.toMap()
                                            )
                                        )
                                    }

                                    val newLines = lyricLines.toMutableList()
                                    val currentLine = newLines[menuLineIndex]
                                    val oldTimeUnits = currentLine.timeUnits
                                    val updatedTimeUnits = currentLine.timeUnits.toMutableList()
                                    updatedTimeUnits.removeAt(menuUnitIndex)
                                    updatedTimeUnits.addAll(menuUnitIndex, newTimeUnits)
                                    newLines[menuLineIndex] = currentLine.copy(timeUnits = updatedTimeUnits)

                                    undoRedoManager.pushAction(
                                        UndoAction(
                                            actionType = UndoActionType.UNIT_SPLIT,
                                            lineIndex = menuLineIndex,
                                            unitIndex = menuUnitIndex,
                                            oldValue = oldTimeUnits,
                                            newValue = updatedTimeUnits.toList()
                                        )
                                    )
                                    updateUndoRedoState()
                                    onLyricLinesChange(newLines)
                                }
                            }
                            onDismissSheet()
                        },
                        enabled = splitLyricText.isNotBlank()
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowCancelConfirmChange(false)
                onPendingDismissChange(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
            text = { Text("您已修改了内容，确定要放弃修改吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onDismissSheet()
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    LaunchedEffect(showCancelConfirm, pendingDismiss, showSheet) {
        if (!showCancelConfirm && !pendingDismiss && showSheet) {
            splitLyricSheetState.show()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddTranslationBottomSheet(
    showSheet: Boolean,
    menuLineIndex: Int,
    lyricLines: List<LyricLine>,
    addTranslationText: String,
    onAddTranslationTextChange: (String) -> Unit,
    originalAddTranslationText: String,
    showCancelConfirm: Boolean,
    onShowCancelConfirmChange: (Boolean) -> Unit,
    pendingDismiss: Boolean,
    onPendingDismissChange: (Boolean) -> Unit,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    val addTranslationSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCopiedButton by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showSheet && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
        val currentLine = lyricLines[menuLineIndex]
        val lineText = currentLine.timeUnits.joinToString("") { it.text }
        var translationRows by remember(showSheet, menuLineIndex, originalAddTranslationText) {
            mutableStateOf(splitTranslationLines(addTranslationText).ifEmpty { listOf("") })
        }

        fun normalizedOriginalTranslation(): String {
            return joinTranslationLines(splitTranslationLines(originalAddTranslationText))
        }

        fun normalizedCurrentTranslation(): String {
            return joinTranslationLines(translationRows)
        }

        fun updateRows(newRows: List<String>) {
            translationRows = newRows
            onAddTranslationTextChange(joinTranslationLines(newRows))
        }

        val hasChanges = normalizedCurrentTranslation() != normalizedOriginalTranslation()

        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (hasChanges) {
                    onPendingDismissChange(true)
                    onShowCancelConfirmChange(true)
                } else {
                    onDismissSheet()
                }
            },
            sheetState = addTranslationSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_add_translation),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("歌词：", fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(lineText)
                            showCopiedButton = true
                            scope.launch {
                                delay(1500)
                                showCopiedButton = false
                            }
                        }
                    ) {
                        Text(if (showCopiedButton) "已复制" else "复制")
                    }
                }
                Text(lineText, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = stringResource(R.string.lyric_timing_translation_line),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                translationRows.forEachIndexed { rowIndex, rowText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedTextField(
                            value = rowText,
                            onValueChange = { newValue ->
                                val newRows = translationRows.toMutableList()
                                newRows[rowIndex] = newValue
                                updateRows(newRows)
                            },
                            placeholder = stringResource(R.string.lyric_timing_translation_input_placeholder_simple),
                            modifier = Modifier.weight(1f)
                        )
                        if (translationRows.size > 1) {
                            IconButton(
                                onClick = {
                                    val newRows = translationRows.toMutableList()
                                    newRows.removeAt(rowIndex)
                                    updateRows(if (newRows.isEmpty()) listOf("") else newRows)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.lyric_timing_delete_translation_line)
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { updateRows(translationRows + "") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("新增翻译行")
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(
                        onClick = {
                            if (menuLineIndex < lyricLines.size) {
                                val newLines = lyricLines.toMutableList()
                                val line = newLines[menuLineIndex]
                                val oldTranslation = line.translation
                                val newTranslation = joinTranslationLines(translationRows)
                                undoRedoManager.pushAction(
                                    UndoAction(
                                        actionType = UndoActionType.TRANSLATION_CHANGE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = -1,
                                        oldValue = oldTranslation,
                                        newValue = newTranslation
                                    )
                                )
                                updateUndoRedoState()
                                newLines[menuLineIndex] = line.copy(translation = newTranslation)
                                onLyricLinesChange(newLines)
                                onAddTranslationTextChange(newTranslation)
                            }
                            onDismissSheet()
                        },
                        enabled = hasChanges
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowCancelConfirmChange(false)
                onPendingDismissChange(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
            text = { Text("您已输入了翻译内容，确定要放弃修改吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onDismissSheet()
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    LaunchedEffect(showCancelConfirm, pendingDismiss, showSheet) {
        if (!showCancelConfirm && !pendingDismiss && showSheet) {
            addTranslationSheetState.show()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MergeLyricBottomSheet(
    showSheet: Boolean,
    menuLineIndex: Int,
    lyricLines: List<LyricLine>,
    mergeLyricPreview: List<LyricTimeUnit>,
    onMergeLyricPreviewChange: (List<LyricTimeUnit>) -> Unit,
    mergeSelectedUnits: Set<Int>,
    onMergeSelectedUnitsChange: (Set<Int>) -> Unit,
    mergeLyricHistory: List<List<LyricTimeUnit>>,
    onMergeLyricHistoryChange: (List<List<LyricTimeUnit>>) -> Unit,
    showCancelConfirm: Boolean,
    onShowCancelConfirmChange: (Boolean) -> Unit,
    pendingDismiss: Boolean,
    onPendingDismissChange: (Boolean) -> Unit,
    originalMergeLyricPreview: List<LyricTimeUnit>,
    originalMergeSelectedUnits: Set<Int>,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    val mergeLyricSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasChanges = mergeLyricPreview != originalMergeLyricPreview || mergeSelectedUnits != originalMergeSelectedUnits
    val hapticFeedback = LocalHapticFeedback.current

    fun toggleUnitSelection(unitIndex: Int, select: Boolean? = null) {
        val isSelected = mergeSelectedUnits.contains(unitIndex)
        val targetSelected = select ?: !isSelected
        if (targetSelected == isSelected) return

        val newSelection = mergeSelectedUnits.toMutableSet()
        if (targetSelected) {
            newSelection.add(unitIndex)
        } else {
            newSelection.remove(unitIndex)
        }
        onMergeSelectedUnitsChange(newSelection)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun resetState() {
        onMergeLyricPreviewChange(emptyList())
        onMergeLyricHistoryChange(emptyList())
        onMergeSelectedUnitsChange(emptySet())
    }

    fun mergeLyricGroup(units: List<LyricTimeUnit>, group: List<Int>): LyricTimeUnit {
        val unitsToMerge = group.map { units[it] }
        val mergedText = unitsToMerge.joinToString("") { it.text }

        val mergedCharTransliterations = mutableMapOf<Int, String>()
        var charOffset = 0
        unitsToMerge.forEach { unit ->
            unit.charTransliterations.forEach { (idx, translit) ->
                mergedCharTransliterations[charOffset + idx] = translit
            }
            charOffset += unit.text.length
        }

        val transliterationsToJoin = unitsToMerge.mapNotNull {
            it.transliteration.takeIf { trans -> trans.isNotEmpty() }
        }
        val mergedTransliteration = if (transliterationsToJoin.isNotEmpty()) {
            val hasAnyCjk = unitsToMerge.any { hasCjkChar(it.text) }
            if (hasAnyCjk) {
                transliterationsToJoin.joinToString("")
            } else {
                transliterationsToJoin.joinToString(" ")
            }
        } else {
            ""
        }

        return LyricTimeUnit(
            text = mergedText,
            startTime = unitsToMerge.first().startTime,
            endTime = unitsToMerge.last().endTime,
            transliteration = mergedTransliteration,
            charTransliterations = mergedCharTransliterations.toMap()
        )
    }

    fun buildMergedUnits(units: List<LyricTimeUnit>, sortedIndices: List<Int>): List<LyricTimeUnit> {
        if (sortedIndices.size < 2) return units
        val groups = mutableListOf<MutableList<Int>>()
        var currentGroup = mutableListOf<Int>()
        for (index in sortedIndices) {
            if (currentGroup.isEmpty() || index == currentGroup.last() + 1) {
                currentGroup.add(index)
            } else {
                if (currentGroup.size >= 2) groups.add(currentGroup)
                currentGroup = mutableListOf(index)
            }
        }
        if (currentGroup.size >= 2) groups.add(currentGroup)
        if (groups.isEmpty()) return units

        val newTimeUnits = mutableListOf<LyricTimeUnit>()
        var cursor = 0
        while (cursor < units.size) {
            val inGroup = groups.find { group -> cursor in group }
            if (inGroup != null) {
                newTimeUnits.add(mergeLyricGroup(units, inGroup))
                cursor = inGroup.last() + 1
            } else {
                newTimeUnits.add(units[cursor])
                cursor++
            }
        }
        return newTimeUnits
    }

    if (showSheet && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
        val currentLine = lyricLines[menuLineIndex]
        val displayTimeUnits = if (mergeLyricPreview.isNotEmpty()) mergeLyricPreview else currentLine.timeUnits

        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (hasChanges) {
                    onPendingDismissChange(true)
                    onShowCancelConfirmChange(true)
                } else {
                    onDismissSheet()
                    resetState()
                }
            },
            sheetState = mergeLyricSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_merge_lyrics),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text("选择要合并的相邻歌词单元（支持多组）：", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayTimeUnits.forEachIndexed { unitIndex, timeUnit ->
                        val isSelected = mergeSelectedUnits.contains(unitIndex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    toggleUnitSelection(unitIndex)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeUnit.text,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sortedIndices = mergeSelectedUnits.sorted()
                            if (sortedIndices.size >= 2) {
                                val merged = buildMergedUnits(displayTimeUnits, sortedIndices)
                                if (merged != displayTimeUnits) {
                                    onMergeLyricHistoryChange(mergeLyricHistory + listOf(displayTimeUnits))
                                    onMergeLyricPreviewChange(merged)
                                    onMergeSelectedUnitsChange(emptySet())
                                }
                            }
                        },
                        enabled = mergeSelectedUnits.size >= 2
                    ) {
                        Text("合并")
                    }
                    OutlinedButton(
                        onClick = {
                            if (mergeLyricHistory.isNotEmpty()) {
                                val lastState = mergeLyricHistory.last()
                                onMergeLyricPreviewChange(lastState)
                                onMergeLyricHistoryChange(mergeLyricHistory.dropLast(1))
                                onMergeSelectedUnitsChange(emptySet())
                            }
                        },
                        enabled = mergeLyricHistory.isNotEmpty()
                    ) {
                        Text("撤销")
                    }
                    Button(
                        onClick = {
                            val timeUnitsToApply = if (mergeLyricPreview.isNotEmpty()) {
                                mergeLyricPreview
                            } else {
                                buildMergedUnits(currentLine.timeUnits, mergeSelectedUnits.sorted())
                            }

                            if (mergeLyricPreview.isNotEmpty() || mergeSelectedUnits.size >= 2) {
                                undoRedoManager.pushAction(
                                    UndoAction(
                                        actionType = UndoActionType.UNIT_MERGE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = -1,
                                        oldValue = currentLine.timeUnits,
                                        newValue = timeUnitsToApply
                                    )
                                )
                                updateUndoRedoState()
                                val newLines = lyricLines.toMutableList()
                                newLines[menuLineIndex] = currentLine.copy(timeUnits = timeUnitsToApply)
                                onLyricLinesChange(newLines)
                            }
                            onDismissSheet()
                            resetState()
                        },
                        enabled = mergeLyricPreview.isNotEmpty() || mergeSelectedUnits.size >= 2
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowCancelConfirmChange(false)
                onPendingDismissChange(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
            text = { Text("您已进行了合并操作，确定要放弃修改吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onDismissSheet()
                        onPendingDismissChange(false)
                        resetState()
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    LaunchedEffect(showCancelConfirm, pendingDismiss, showSheet) {
        if (!showCancelConfirm && !pendingDismiss && showSheet) {
            mergeLyricSheetState.show()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MergeLinesBottomSheet(
    showSheet: Boolean,
    menuLineIndex: Int,
    lyricLines: List<LyricLine>,
    mergeLinesPreview: List<LyricLine>,
    onMergeLinesPreviewChange: (List<LyricLine>) -> Unit,
    mergeLinesSelected: Set<Int>,
    onMergeLinesSelectedChange: (Set<Int>) -> Unit,
    mergeLinesPreviewSelected: Set<Int>,
    onMergeLinesPreviewSelectedChange: (Set<Int>) -> Unit,
    mergeLinesHistory: List<List<LyricLine>>,
    onMergeLinesHistoryChange: (List<List<LyricLine>>) -> Unit,
    mergeLinesAddSpace: Boolean,
    onMergeLinesAddSpaceChange: (Boolean) -> Unit,
    showCancelConfirm: Boolean,
    onShowCancelConfirmChange: (Boolean) -> Unit,
    pendingDismiss: Boolean,
    onPendingDismissChange: (Boolean) -> Unit,
    originalMergeLinesPreview: List<LyricLine>,
    originalMergeLinesSelected: Set<Int>,
    undoRedoManager: UndoRedoManager,
    updateUndoRedoState: () -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onDismissSheet: () -> Unit
) {
    val mergeLinesSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mergeLinesScrollState = rememberLazyListState()
    val scrollBlocker = rememberBottomSheetListScrollBlocker()
    var mergeLinesError by remember { mutableStateOf("") }

    val hasChanges = mergeLinesPreview != originalMergeLinesPreview || mergeLinesSelected != originalMergeLinesSelected
    val displayLines = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreview else lyricLines
    val displaySelected = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreviewSelected else mergeLinesSelected
    fun resetState() {
        onMergeLinesPreviewChange(emptyList())
        onMergeLinesPreviewSelectedChange(emptySet())
        onMergeLinesHistoryChange(emptyList())
        mergeLinesError = ""
    }

    if (showSheet && menuLineIndex >= 0) {
        LaunchedEffect(showSheet, menuLineIndex) {
            if (showSheet && menuLineIndex >= 0) {
                delay(100)
                val visibleItems = mergeLinesScrollState.layoutInfo.visibleItemsInfo.size
                val targetItem = menuLineIndex - visibleItems / 2
                if (targetItem > 0) {
                    mergeLinesScrollState.scrollToItem(targetItem.coerceAtLeast(0))
                }
            }
        }

        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = {
                if (hasChanges) {
                    onPendingDismissChange(true)
                    onShowCancelConfirmChange(true)
                } else {
                    onDismissSheet()
                    resetState()
                }
            },
            sheetState = mergeLinesSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_merge_lines),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text("选择要合并的连续行：", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = mergeLinesScrollState,
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBlocker)
                ) {
                    items(displayLines.size) { lineIndex ->
                        val line = displayLines[lineIndex]
                        val lineText = line.timeUnits.joinToString("") { it.text }
                        val isSelected = displaySelected.contains(lineIndex)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    val newSelection = displaySelected.toMutableSet()
                                    if (isSelected) {
                                        newSelection.remove(lineIndex)
                                    } else {
                                        newSelection.add(lineIndex)
                                    }
                                    if (mergeLinesPreview.isNotEmpty()) {
                                        onMergeLinesPreviewSelectedChange(newSelection)
                                    } else {
                                        onMergeLinesSelectedChange(newSelection)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lineIndex + 1}",
                                modifier = Modifier.padding(end = 8.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = lineText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                CustomCheckbox(
                    checked = mergeLinesAddSpace,
                    onCheckedChange = { onMergeLinesAddSpaceChange(it) },
                    label = stringResource(R.string.lyric_timing_add_space_between_lines)
                )

                if (mergeLinesError.isNotEmpty()) {
                    Text(
                        text = mergeLinesError,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = {
                            val currentSelected = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreviewSelected else mergeLinesSelected
                            val currentLines = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreview else lyricLines
                            val sortedIndices = currentSelected.sorted()

                            var isConsecutive = true
                            if (sortedIndices.size >= 2) {
                                for (index in 0 until sortedIndices.size - 1) {
                                    if (sortedIndices[index + 1] - sortedIndices[index] != 1) {
                                        isConsecutive = false
                                        break
                                    }
                                }
                            }

                            if (sortedIndices.size < 2) {
                                mergeLinesError = "请至少选择两行"
                            } else if (!isConsecutive) {
                                mergeLinesError = "请选择连续的歌词行"
                            } else {
                                onMergeLinesHistoryChange(mergeLinesHistory + listOf(currentLines))

                                val newLines = currentLines.toMutableList()
                                val firstIndex = sortedIndices.first()
                                val firstLine = newLines[firstIndex]
                                val mergedTimeUnits = firstLine.timeUnits.toMutableList()

                                sortedIndices.drop(1).forEach { idx ->
                                    val line = newLines[idx]
                                    if (mergeLinesAddSpace && line.timeUnits.isNotEmpty()) {
                                        val firstUnit = line.timeUnits.first()
                                        mergedTimeUnits.add(
                                            LyricTimeUnit(
                                                " " + firstUnit.text,
                                                firstUnit.startTime,
                                                firstUnit.endTime
                                            )
                                        )
                                        mergedTimeUnits.addAll(line.timeUnits.drop(1))
                                    } else {
                                        mergedTimeUnits.addAll(line.timeUnits)
                                    }
                                }

                                newLines[firstIndex] = firstLine.copy(timeUnits = mergedTimeUnits)
                                sortedIndices.drop(1).sortedDescending().forEach { idx ->
                                    newLines.removeAt(idx)
                                }

                                onMergeLinesPreviewChange(newLines)
                                onMergeLinesPreviewSelectedChange(emptySet())
                                mergeLinesError = ""
                            }
                        }
                    ) {
                        Text("合并")
                    }
                    OutlinedButton(
                        onClick = {
                            if (mergeLinesHistory.isNotEmpty()) {
                                val lastState = mergeLinesHistory.last()
                                onMergeLinesPreviewChange(lastState)
                                onMergeLinesHistoryChange(mergeLinesHistory.dropLast(1))
                                onMergeLinesPreviewSelectedChange(emptySet())
                                mergeLinesError = ""
                            }
                        },
                        enabled = mergeLinesHistory.isNotEmpty()
                    ) {
                        Text("撤销")
                    }
                    Button(
                        onClick = {
                            if (mergeLinesPreview.isNotEmpty()) {
                                val actions = mutableListOf<UndoAction>()
                                val oldLines = lyricLines.toList()
                                actions.add(
                                    UndoAction(
                                        actionType = UndoActionType.LINE_MERGE,
                                        lineIndex = -1,
                                        unitIndex = -1,
                                        oldValue = oldLines,
                                        newValue = mergeLinesPreview
                                    )
                                )
                                undoRedoManager.pushBatchAction(BatchUndoAction(actions, "合并行"))
                                updateUndoRedoState()
                                onLyricLinesChange(mergeLinesPreview)
                            } else {
                                val sortedIndices = mergeLinesSelected.sorted()
                                var isConsecutive = true
                                if (sortedIndices.size >= 2) {
                                    for (index in 0 until sortedIndices.size - 1) {
                                        if (sortedIndices[index + 1] - sortedIndices[index] != 1) {
                                            isConsecutive = false
                                            break
                                        }
                                    }
                                }

                                if (sortedIndices.size < 2) {
                                    mergeLinesError = "请至少选择两行"
                                    return@Button
                                }
                                if (!isConsecutive) {
                                    mergeLinesError = "请选择连续的歌词行"
                                    return@Button
                                }

                                val actions = mutableListOf<UndoAction>()
                                val oldLines = lyricLines.toList()
                                val newLines = lyricLines.toMutableList()
                                val firstIndex = sortedIndices.first()
                                val firstLine = newLines[firstIndex]
                                val mergedTimeUnits = firstLine.timeUnits.toMutableList()

                                sortedIndices.drop(1).forEach { idx ->
                                    val line = newLines[idx]
                                    if (mergeLinesAddSpace && line.timeUnits.isNotEmpty()) {
                                        val firstUnit = line.timeUnits.first()
                                        mergedTimeUnits.add(
                                            LyricTimeUnit(
                                                " " + firstUnit.text,
                                                firstUnit.startTime,
                                                firstUnit.endTime
                                            )
                                        )
                                        mergedTimeUnits.addAll(line.timeUnits.drop(1))
                                    } else {
                                        mergedTimeUnits.addAll(line.timeUnits)
                                    }
                                }

                                newLines[firstIndex] = firstLine.copy(timeUnits = mergedTimeUnits)
                                sortedIndices.drop(1).sortedDescending().forEach { idx ->
                                    newLines.removeAt(idx)
                                }

                                actions.add(
                                    UndoAction(
                                        actionType = UndoActionType.LINE_MERGE,
                                        lineIndex = -1,
                                        unitIndex = -1,
                                        oldValue = oldLines,
                                        newValue = newLines.toList()
                                    )
                                )
                                undoRedoManager.pushBatchAction(BatchUndoAction(actions, "合并行"))
                                updateUndoRedoState()
                                onLyricLinesChange(newLines)
                            }

                            onDismissSheet()
                            resetState()
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                onShowCancelConfirmChange(false)
                onPendingDismissChange(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
            text = { Text("您已进行了合并操作，确定要放弃修改吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onPendingDismissChange(false)
                    }
                ) {
                    Text(stringResource(R.string.common_continue_editing))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onShowCancelConfirmChange(false)
                        onDismissSheet()
                        onPendingDismissChange(false)
                        resetState()
                    }
                ) {
                    Text(stringResource(R.string.common_discard_changes))
                }
            }
        )
    }

    LaunchedEffect(showCancelConfirm, pendingDismiss, showSheet) {
        if (!showCancelConfirm && !pendingDismiss && showSheet) {
            mergeLinesSheetState.show()
        }
    }
}

@Composable
private fun DeleteAllTransliterationConfirmDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除所有注音") },
        text = { Text("确定删除当前打轴界面所有歌词行的注音内容吗？此操作可通过撤销恢复。") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.lyric_timing_confirm_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun RecognizeRomajiRubyDialog(
    showDialog: Boolean,
    translationLineOptionCount: Int,
    selectedTranslationLineIndex: Int,
    onSelectedTranslationLineIndexChange: (Int) -> Unit,
    onRomajiToKanaClick: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("识别注音") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_transliteration_recognition_description),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (translationLineOptionCount > 1) {
                    Text(
                        text = stringResource(R.string.lyric_timing_select_romaji_translation_row),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    for (index in 0 until translationLineOptionCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectedTranslationLineIndexChange(index) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == selectedTranslationLineIndex,
                                onClick = { onSelectedTranslationLineIndexChange(index) }
                            )
                            Text(
                                text = stringResource(R.string.lyric_timing_translation_row_index, index + 1),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.lyric_timing_only_one_translation_row_hint),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("开始识别")
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onRomajiToKanaClick) {
                    Text("罗马音转假名（仅日语）")
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }
    )
}

private data class RomajiRubyRecognitionResult(
    val updatedLines: List<LyricLine>,
    val successCount: Int,
    val failureCount: Int,
    val errorMessage: String? = null
)

private data class KanaRomanMapping(
    val romanByKana: Map<String, String>,
    val maxKanaLength: Int
)

private data class LyricCharRef(
    val unitIndex: Int,
    val charIndex: Int,
    val char: Char
)

private data class RomajiToken(
    val raw: String,
    val normalized: String
)

private data class RomanTokenRange(
    val start: Int,
    val endInclusive: Int
)

private data class KanaAnchor(
    val chars: List<LyricCharRef>,
    val kanaText: String,
    val romajiCandidates: Set<String>
)

private data class NonKanaSegment(
    val chars: List<LyricCharRef>
)

private enum class RecognitionItemType {
    NON_KANA,
    KANA_ANCHOR
}

private data class RecognitionLineItem(
    val type: RecognitionItemType,
    val nonKanaSegment: NonKanaSegment? = null,
    val kanaAnchor: KanaAnchor? = null
)

private enum class LineRomajiRubyStatus {
    SUCCESS,
    FAILED,
    SKIPPED
}

private data class LineRomajiRubyRecognitionResult(
    val status: LineRomajiRubyStatus,
    val updatedLine: LyricLine
)

private data class RomajiToKanaConvertResult(
    val updatedLines: List<LyricLine>,
    val successCount: Int,
    val failureCount: Int,
    val errorMessage: String? = null
)

private fun normalizeKanaChar(char: Char): Char {
    return if (char in '\u30A1'..'\u30F6') {
        (char.code - 0x60).toChar()
    } else {
        char
    }
}

private fun normalizeKanaText(text: String): String {
    return text.map { normalizeKanaChar(it) }.joinToString("")
}

private fun isKanaChar(char: Char): Boolean {
    val normalized = normalizeKanaChar(char)
    return normalized in '\u3040'..'\u309F' || char == 'ー'
}

private fun isTrailingKanaAttachableChar(char: Char): Boolean {
    return JapaneseTrailingAttachableChars.contains(char)
}

private fun isRubyAssignableCharacter(char: Char): Boolean {
    val codePoint = char.code
    return (codePoint in 0x3400..0x4DBF) ||
            (codePoint in 0x4E00..0x9FFF) ||
            (codePoint in 0xF900..0xFAFF) ||
            (codePoint in 0xAC00..0xD7AF) ||
            (codePoint in 0x1100..0x11FF) ||
            (codePoint in 0x3130..0x318F) ||
            (codePoint in 0xA960..0xA97F) ||
            (codePoint in 0xD7B0..0xD7FF) ||
            char == '々' ||
            char == '〆'
}

private fun normalizeRomanToken(token: String): String {
    return token
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z]"), "")
}

private fun tokenizeRomajiLine(text: String): List<RomajiToken> {
    return text
        .replace('\u3000', ' ')
        .trim()
        .split(Regex("\\s+"))
        .mapNotNull { part ->
            val raw = part.trim()
            if (raw.isEmpty()) return@mapNotNull null
            val normalized = normalizeRomanToken(raw)
            if (normalized.isEmpty() && raw != "っ") return@mapNotNull null
            RomajiToken(raw = raw, normalized = normalized)
        }
}

private fun loadKanaRomanMapping(context: Context): KanaRomanMapping? {
    return try {
        val romanByKana = linkedMapOf<String, String>()
        var maxKanaLength = 1
        context.assets.open("JapRoman.txt")
            .bufferedReader(Charsets.UTF_8)
            .useLines { sequence ->
                sequence.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@forEach
                    val parts = trimmed.split("：", ":", limit = 2)
                    if (parts.size != 2) return@forEach
                    val kana = normalizeKanaText(parts[0].trim())
                    val romaji = normalizeRomanToken(parts[1].trim())
                    if (kana.isEmpty() || romaji.isEmpty()) return@forEach
                    romanByKana[kana] = romaji
                    if (kana.length > maxKanaLength) {
                        maxKanaLength = kana.length
                    }
                }
            }
        if (romanByKana.isEmpty()) {
            null
        } else {
            KanaRomanMapping(romanByKana = romanByKana, maxKanaLength = maxKanaLength)
        }
    } catch (_: Exception) {
        null
    }
}

private fun kanaRomanOptions(
    kana: String,
    mapping: KanaRomanMapping
): Set<String> {
    val normalized = normalizeKanaText(kana)
    val direct = mapping.romanByKana[normalized]
    if (normalized == "っ") {
        return linkedSetOf(
            "t", "k", "s", "p", "c", "f", "h", "m", "r", "g", "d", "b", "j", "z", "q", "v", "w", "y",
            "ch", "sh", "ts",
            "xtu", "ltu", "xtsu", "ltsu"
        )
    }
    if (normalized == "ー") {
        return emptySet()
    }
    if (direct.isNullOrEmpty()) return emptySet()
    return when (normalized) {
        "は" -> linkedSetOf(direct, "wa")
        "へ" -> linkedSetOf(direct, "e")
        "を" -> linkedSetOf(direct, "o")
        else -> linkedSetOf(direct)
    }
}

private fun hasKanaMappingCandidate(
    kana: String,
    mapping: KanaRomanMapping
): Boolean {
    val normalized = normalizeKanaText(kana)
    if (normalized == "っ") return true
    return mapping.romanByKana.containsKey(normalized)
}

private fun buildRecognitionLineItems(
    lyricLine: LyricLine,
    mapping: KanaRomanMapping
): List<RecognitionLineItem> {
    val items = mutableListOf<RecognitionLineItem>()
    val pendingNonKana = mutableListOf<LyricCharRef>()
    data class LineCharToken(
        val ref: LyricCharRef,
        val char: Char
    )

    val lineTokens = mutableListOf<LineCharToken>()
    lyricLine.timeUnits.forEachIndexed { unitIndex, unit ->
        unit.text.forEachIndexed { charIndex, char ->
            lineTokens.add(
                LineCharToken(
                    ref = LyricCharRef(
                        unitIndex = unitIndex,
                        charIndex = charIndex,
                        char = char
                    ),
                    char = char
                )
            )
        }
    }

    fun flushNonKana() {
        if (pendingNonKana.isNotEmpty()) {
            items.add(
                RecognitionLineItem(
                    type = RecognitionItemType.NON_KANA,
                    nonKanaSegment = NonKanaSegment(pendingNonKana.toList())
                )
            )
            pendingNonKana.clear()
        }
    }

    fun leadingConsonantCandidate(romaji: String): String? {
        if (romaji.isBlank()) return null
        val vowels = setOf('a', 'e', 'i', 'o', 'u')
        val normalized = romaji.lowercase(Locale.ROOT)
        val firstVowelIndex = normalized.indexOfFirst { it in vowels }
        val prefix = when {
            firstVowelIndex < 0 -> normalized
            firstVowelIndex == 0 -> ""
            firstVowelIndex == 1 -> normalized.substring(0, 1)
            else -> normalized.substring(0, minOf(2, firstVowelIndex))
        }
        return prefix.takeIf { it.isNotBlank() }
    }

    fun peekNextKanaCandidates(startIndex: Int): Set<String> {
        var index = startIndex
        while (index < lineTokens.size && lineTokens[index].char.isWhitespace()) {
            index++
        }
        if (index >= lineTokens.size) return emptySet()
        if (!isKanaChar(lineTokens[index].char)) return emptySet()
        val longest = minOf(3, lineTokens.size - index)
        for (len in longest downTo 1) {
            val candidateTokens = lineTokens.subList(index, index + len)
            if (candidateTokens.any { it.char.isWhitespace() || !isKanaChar(it.char) }) continue
            val candidateKana = candidateTokens.joinToString("") { it.char.toString() }
            if (hasKanaMappingCandidate(candidateKana, mapping)) {
                return kanaRomanOptions(candidateKana, mapping)
            }
        }
        return emptySet()
    }

    var tokenIndex = 0
    while (tokenIndex < lineTokens.size) {
        val currentToken = lineTokens[tokenIndex]
        val currentChar = currentToken.char

        if (currentChar.isWhitespace()) {
            flushNonKana()
            tokenIndex++
            continue
        }
        if (!isKanaChar(currentChar)) {
            pendingNonKana.add(currentToken.ref)
            tokenIndex++
            continue
        }

        val hasAdjacentPrevious = tokenIndex > 0 && !lineTokens[tokenIndex - 1].char.isWhitespace()
        if (isTrailingKanaAttachableChar(currentChar) && pendingNonKana.isEmpty() && hasAdjacentPrevious) {
            val previous = items.lastOrNull()
            val previousAnchor = previous?.kanaAnchor
            if (previous?.type == RecognitionItemType.KANA_ANCHOR && previousAnchor != null) {
                items[items.lastIndex] = previous.copy(
                    kanaAnchor = previousAnchor.copy(
                        chars = previousAnchor.chars + currentToken.ref,
                        kanaText = previousAnchor.kanaText + currentChar
                    )
                )
                tokenIndex++
                continue
            }
        }

        flushNonKana()

        var matchedLength = 0
        var matchedKana = ""
        val longest = minOf(3, lineTokens.size - tokenIndex)
        for (len in longest downTo 1) {
            val candidateTokens = lineTokens.subList(tokenIndex, tokenIndex + len)
            if (candidateTokens.any { it.char.isWhitespace() || !isKanaChar(it.char) }) continue
            val candidateKana = candidateTokens.joinToString("") { it.char.toString() }
            if (hasKanaMappingCandidate(candidateKana, mapping)) {
                matchedLength = len
                matchedKana = candidateKana
                break
            }
        }
        if (matchedLength <= 0) {
            matchedLength = 1
            matchedKana = currentChar.toString()
        }

        val refs = lineTokens
            .subList(tokenIndex, tokenIndex + matchedLength)
            .map { it.ref }
        val baseCandidates = kanaRomanOptions(matchedKana, mapping)
        val normalizedMatched = normalizeKanaText(matchedKana)
        val mergedCandidates = if (normalizedMatched == "っ") {
            val nextCandidates = peekNextKanaCandidates(tokenIndex + matchedLength)
            val fromNext = nextCandidates.mapNotNull { candidate ->
                leadingConsonantCandidate(candidate)
            }
            linkedSetOf<String>().apply {
                addAll(baseCandidates)
                addAll(fromNext)
            }
        } else {
            baseCandidates
        }
        items.add(
            RecognitionLineItem(
                type = RecognitionItemType.KANA_ANCHOR,
                kanaAnchor = KanaAnchor(
                    chars = refs,
                    kanaText = matchedKana,
                    romajiCandidates = mergedCandidates
                )
            )
        )
        tokenIndex += matchedLength
    }
    flushNonKana()
    return items
}

private fun joinNormalizedTokens(
    tokens: List<RomajiToken>,
    start: Int,
    endInclusive: Int
): String {
    val builder = StringBuilder()
    for (i in start..endInclusive) {
        builder.append(tokens[i].normalized)
    }
    return builder.toString()
}

private fun findAnchorTokenRanges(
    anchors: List<KanaAnchor>,
    tokens: List<RomajiToken>
): List<RomanTokenRange>? {
    val failedState = mutableSetOf<Pair<Int, Int>>()

    fun search(anchorIndex: Int, tokenCursor: Int): List<RomanTokenRange>? {
        if (anchorIndex >= anchors.size) {
            return emptyList()
        }
        val state = anchorIndex to tokenCursor
        if (state in failedState) return null

        val candidates = anchors[anchorIndex].romajiCandidates
        if (candidates.isEmpty()) {
            failedState.add(state)
            return null
        }
        if (tokenCursor >= tokens.size) {
            failedState.add(state)
            return null
        }

        for (start in tokenCursor until tokens.size) {
            val builder = StringBuilder()
            for (end in start until tokens.size) {
                builder.append(tokens[end].normalized)
                val joined = builder.toString()
                val exact = joined in candidates
                val hasPrefix = candidates.any { it.startsWith(joined) }
                if (exact) {
                    val rest = search(anchorIndex + 1, end + 1)
                    if (rest != null) {
                        return listOf(RomanTokenRange(start = start, endInclusive = end)) + rest
                    }
                }
                if (!hasPrefix) {
                    break
                }
            }
        }

        failedState.add(state)
        return null
    }

    return search(anchorIndex = 0, tokenCursor = 0)
}

private fun mergeStandaloneNTokens(
    romajiTokens: List<RomajiToken>
): List<String> {
    if (romajiTokens.isEmpty()) return emptyList()
    val merged = mutableListOf<String>()
    romajiTokens.forEach { token ->
        if (token.normalized == "n" && merged.isNotEmpty()) {
            val lastIndex = merged.lastIndex
            merged[lastIndex] = merged[lastIndex] + " " + token.raw
        } else {
            merged.add(token.raw)
        }
    }
    return merged
}

private fun assignRubyUnitsEvenly(
    charTargets: List<LyricCharRef>,
    rubyUnits: List<String>,
    assignments: MutableMap<Pair<Int, Int>, String>
) {
    if (charTargets.isEmpty() || rubyUnits.isEmpty()) return
    val totalChars = charTargets.size
    val totalUnits = rubyUnits.size
    val base = totalUnits / totalChars
    val remainder = totalUnits % totalChars
    var unitCursor = 0

    charTargets.forEachIndexed { index, target ->
        val unitCount = base + if (index < remainder) 1 else 0
        if (unitCount <= 0) return@forEachIndexed
        val end = (unitCursor + unitCount).coerceAtMost(totalUnits)
        if (end <= unitCursor) return@forEachIndexed
        val ruby = rubyUnits.subList(unitCursor, end).joinToString(" ").trim()
        unitCursor = end
        if (ruby.isNotEmpty()) {
            assignments[target.unitIndex to target.charIndex] = ruby
        }
    }
}

private fun distributeRomajiToNonKanaSegments(
    segments: List<NonKanaSegment>,
    romajiTokens: List<RomajiToken>,
    assignments: MutableMap<Pair<Int, Int>, String>
) {
    if (segments.isEmpty() || romajiTokens.isEmpty()) return
    val targets = segments
        .flatMap { it.chars }
        .filter { isRubyAssignableCharacter(it.char) }
    if (targets.isEmpty()) return
    val rubyUnits = mergeStandaloneNTokens(romajiTokens)
    assignRubyUnitsEvenly(
        charTargets = targets,
        rubyUnits = rubyUnits,
        assignments = assignments
    )
}

private fun rubyForKanaAnchor(
    anchor: KanaAnchor,
    matchedTokens: List<RomajiToken>
): String {
    val normalizedKana = normalizeKanaText(anchor.kanaText)
    val raw = matchedTokens.joinToString(" ") { it.raw }.trim()
    val normalized = matchedTokens.joinToString("") { it.normalized }
    if (normalizedKana == "っ") {
        return when (normalized) {
            "t" -> "t"
            "xtu", "ltu", "xtsu", "ltsu" -> normalized
            else -> if (normalized.isNotEmpty()) normalized else raw
        }
    }
    return raw
}

private fun applyRomajiAssignmentsToLine(
    lyricLine: LyricLine,
    assignments: Map<Pair<Int, Int>, String>,
    clearPositions: Set<Pair<Int, Int>>
): LyricLine {
    val updatedUnits = lyricLine.timeUnits.mapIndexed { unitIndex, unit ->
        val newCharMap = unit.charTransliterations.toMutableMap()
        clearPositions.forEach { position ->
            if (position.first == unitIndex) {
                newCharMap.remove(position.second)
            }
        }
        assignments.forEach { (position, ruby) ->
            if (position.first == unitIndex && ruby.isNotBlank()) {
                newCharMap[position.second] = ruby
            }
        }
        unit.copy(charTransliterations = newCharMap)
    }
    return lyricLine.copy(timeUnits = updatedUnits)
}

private fun recognizeRomajiRubyForLine(
    lyricLine: LyricLine,
    selectedTranslationLineIndex: Int,
    mapping: KanaRomanMapping
): LineRomajiRubyRecognitionResult {
    if (isBlankLyricLine(lyricLine)) {
        return LineRomajiRubyRecognitionResult(
            status = LineRomajiRubyStatus.SKIPPED,
            updatedLine = lyricLine
        )
    }

    val translationLines = splitTranslationLines(lyricLine.translation)
    if (selectedTranslationLineIndex !in translationLines.indices) {
        return LineRomajiRubyRecognitionResult(
            status = LineRomajiRubyStatus.FAILED,
            updatedLine = lyricLine
        )
    }

    val romajiTokens = tokenizeRomajiLine(translationLines[selectedTranslationLineIndex])
    if (romajiTokens.isEmpty()) {
        return LineRomajiRubyRecognitionResult(
            status = LineRomajiRubyStatus.FAILED,
            updatedLine = lyricLine
        )
    }

    val lineItems = buildRecognitionLineItems(lyricLine, mapping)
    val anchors = lineItems.mapNotNull { it.kanaAnchor }
    if (anchors.isEmpty()) {
        val assignments = linkedMapOf<Pair<Int, Int>, String>()
        val nonKanaSegments = lineItems.mapNotNull { it.nonKanaSegment }
        distributeRomajiToNonKanaSegments(
            segments = nonKanaSegments,
            romajiTokens = romajiTokens,
            assignments = assignments
        )
        if (assignments.isEmpty()) {
            return LineRomajiRubyRecognitionResult(
                status = LineRomajiRubyStatus.SKIPPED,
                updatedLine = lyricLine
            )
        }
        val updatedLine = applyRomajiAssignmentsToLine(
            lyricLine = lyricLine,
            assignments = assignments,
            clearPositions = emptySet()
        )
        val remainingTranslations = translationLines.toMutableList().apply {
            removeAt(selectedTranslationLineIndex)
        }
        return LineRomajiRubyRecognitionResult(
            status = LineRomajiRubyStatus.SUCCESS,
            updatedLine = updatedLine.copy(
                translation = joinTranslationLines(remainingTranslations)
            )
        )
    }
    if (anchors.any { it.romajiCandidates.isEmpty() }) {
        return LineRomajiRubyRecognitionResult(
            status = LineRomajiRubyStatus.FAILED,
            updatedLine = lyricLine
        )
    }

    val ranges = findAnchorTokenRanges(anchors, romajiTokens)
        ?: return LineRomajiRubyRecognitionResult(
            status = LineRomajiRubyStatus.FAILED,
            updatedLine = lyricLine
        )

    val assignments = linkedMapOf<Pair<Int, Int>, String>()
    val clearPositions = mutableSetOf<Pair<Int, Int>>()
    var tokenCursor = 0
    var anchorIndex = 0
    val pendingNonKana = mutableListOf<NonKanaSegment>()

    lineItems.forEach { item ->
        when (item.type) {
            RecognitionItemType.NON_KANA -> {
                item.nonKanaSegment?.let { pendingNonKana.add(it) }
            }
            RecognitionItemType.KANA_ANCHOR -> {
                val anchor = item.kanaAnchor ?: return@forEach
                val range = ranges.getOrNull(anchorIndex) ?: return@forEach

                if (range.start > tokenCursor) {
                    val betweenTokens = romajiTokens.subList(tokenCursor, range.start)
                    distributeRomajiToNonKanaSegments(
                        segments = pendingNonKana,
                        romajiTokens = betweenTokens,
                        assignments = assignments
                    )
                }
                pendingNonKana.clear()

                anchor.chars.forEach { charRef ->
                    clearPositions.add(charRef.unitIndex to charRef.charIndex)
                }
                val matchedTokens = romajiTokens.subList(range.start, range.endInclusive + 1)
                val ruby = rubyForKanaAnchor(anchor, matchedTokens)
                val primaryChar = anchor.chars.firstOrNull()
                if (primaryChar != null && ruby.isNotBlank()) {
                    assignments[primaryChar.unitIndex to primaryChar.charIndex] = ruby
                }

                tokenCursor = range.endInclusive + 1
                anchorIndex++
            }
        }
    }

    if (tokenCursor < romajiTokens.size) {
        val tailTokens = romajiTokens.subList(tokenCursor, romajiTokens.size)
        distributeRomajiToNonKanaSegments(
            segments = pendingNonKana,
            romajiTokens = tailTokens,
            assignments = assignments
        )
    }

    val updatedLine = applyRomajiAssignmentsToLine(
        lyricLine = lyricLine,
        assignments = assignments,
        clearPositions = clearPositions
    )
    val remainingTranslations = translationLines.toMutableList().apply {
        removeAt(selectedTranslationLineIndex)
    }
    return LineRomajiRubyRecognitionResult(
        status = LineRomajiRubyStatus.SUCCESS,
        updatedLine = updatedLine.copy(
            translation = joinTranslationLines(remainingTranslations)
        )
    )
}

private fun recognizeRomajiRubyForLyrics(
    context: Context,
    lyricLines: List<LyricLine>,
    selectedTranslationLineIndex: Int
): RomajiRubyRecognitionResult {
    val hasAnyTranslation = lyricLines.any { splitTranslationLines(it.translation).isNotEmpty() }
    if (!hasAnyTranslation) {
        return RomajiRubyRecognitionResult(
            updatedLines = lyricLines,
            successCount = 0,
            failureCount = 0,
            errorMessage = "当前歌词没有翻译行，无法识别。"
        )
    }

    val mapping = loadKanaRomanMapping(context)
        ?: return RomajiRubyRecognitionResult(
            updatedLines = lyricLines,
            successCount = 0,
            failureCount = 0,
            errorMessage = "注音映射文件加载失败。"
        )

    var successCount = 0
    var failureCount = 0
    val updatedLines = lyricLines.map { line ->
        val lineResult = recognizeRomajiRubyForLine(
            lyricLine = line,
            selectedTranslationLineIndex = selectedTranslationLineIndex,
            mapping = mapping
        )
        when (lineResult.status) {
            LineRomajiRubyStatus.SUCCESS -> {
                successCount++
                lineResult.updatedLine
            }
            LineRomajiRubyStatus.FAILED -> {
                failureCount++
                line
            }
            LineRomajiRubyStatus.SKIPPED -> line
        }
    }

    return RomajiRubyRecognitionResult(
        updatedLines = updatedLines,
        successCount = successCount,
        failureCount = failureCount,
        errorMessage = null
    )
}

private fun buildRomanToKanaMap(mapping: KanaRomanMapping): Map<String, String> {
    val romanToKana = linkedMapOf<String, String>()
    mapping.romanByKana.forEach { (kana, romaji) ->
        val normalizedRomaji = normalizeRomanToken(romaji)
        if (normalizedRomaji.isEmpty()) return@forEach
        val hiraganaKana = normalizeKanaText(kana)
        if (hiraganaKana.isEmpty()) return@forEach
        val existing = romanToKana[normalizedRomaji]
        if (existing == null || existing.length > hiraganaKana.length) {
            romanToKana[normalizedRomaji] = hiraganaKana
        }
    }
    romanToKana["n"] = "ん"
    return romanToKana
}

private fun convertRomajiRubyTextToKana(
    rubyText: String,
    romanToKana: Map<String, String>
): String? {
    val tokens = rubyText
        .replace('\u3000', ' ')
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return null

    val kanaBuilder = StringBuilder()
    for (token in tokens) {
        val normalized = normalizeRomanToken(token)
        if (token == "っ" || token == "ッ") {
            kanaBuilder.append('っ')
            continue
        }
        if (token.startsWith("'") && normalized.length == 1) {
            kanaBuilder.append('っ')
            continue
        }
        if (normalized in setOf("xtu", "ltu", "xtsu", "ltsu")) {
            kanaBuilder.append('っ')
            continue
        }
        if (normalized.isEmpty()) {
            return null
        }
        val kana = romanToKana[normalized] ?: return null
        kanaBuilder.append(kana)
    }
    return kanaBuilder.toString().ifBlank { null }
}

private fun convertRomajiRubyToKanaForLyrics(
    context: Context,
    lyricLines: List<LyricLine>
): RomajiToKanaConvertResult {
    val mapping = loadKanaRomanMapping(context)
        ?: return RomajiToKanaConvertResult(
            updatedLines = lyricLines,
            successCount = 0,
            failureCount = 0,
            errorMessage = "注音映射文件加载失败。"
        )
    val romanToKana = buildRomanToKanaMap(mapping)

    var successCount = 0
    var failureCount = 0
    val updatedLines = lyricLines.map { line ->
        val updatedUnits = line.timeUnits.map { unit ->
            val charMap = unit.charTransliterations.toMutableMap()
            charMap.keys.toList().forEach { charIndex ->
                val currentRuby = charMap[charIndex].orEmpty().trim()
                if (currentRuby.isBlank()) {
                    charMap.remove(charIndex)
                    return@forEach
                }
                val char = unit.text.getOrNull(charIndex) ?: run {
                    charMap.remove(charIndex)
                    return@forEach
                }
                if (isKanaChar(char)) {
                    charMap.remove(charIndex)
                    return@forEach
                }
                val converted = convertRomajiRubyTextToKana(
                    rubyText = currentRuby,
                    romanToKana = romanToKana
                )
                if (converted != null) {
                    charMap[charIndex] = converted
                    successCount++
                } else {
                    failureCount++
                }
            }
            unit.copy(charTransliterations = charMap)
        }
        line.copy(timeUnits = updatedUnits)
    }

    return RomajiToKanaConvertResult(
        updatedLines = updatedLines,
        successCount = successCount,
        failureCount = failureCount,
        errorMessage = null
    )
}

private fun isJapaneseSmallKana(char: Char): Boolean {
    return JapaneseSmallKanaChars.contains(char)
}

private fun extractCjkTransliterationUnits(text: String): List<CjkTransliterationUnit> {
    data class MutableUnit(
        val startIndex: Int,
        val indices: MutableList<Int>,
        val text: StringBuilder
    )

    val units = mutableListOf<MutableUnit>()
    text.forEachIndexed { index, char ->
        if (!isCJKCharacter(char)) return@forEachIndexed

        val lastUnit = units.lastOrNull()
        val canAttachToPrevious = isJapaneseSmallKana(char) &&
                lastUnit != null &&
                index == lastUnit.indices.last() + 1

        if (canAttachToPrevious) {
            lastUnit.indices.add(index)
            lastUnit.text.append(char)
        } else {
            units.add(
                MutableUnit(
                    startIndex = index,
                    indices = mutableListOf(index),
                    text = StringBuilder(char.toString())
                )
            )
        }
    }

    return units.map { unit ->
        CjkTransliterationUnit(
            startIndex = unit.startIndex,
            indices = unit.indices.toList(),
            text = unit.text.toString()
        )
    }
}

private fun findTransliterationForUnit(
    indices: List<Int>,
    charTransliterations: Map<Int, String>
): String {
    indices.forEach { idx ->
        val trans = charTransliterations[idx]
        if (!trans.isNullOrBlank()) {
            return trans
        }
    }
    return ""
}

private fun parseTransliterationInput(content: String): Pair<Map<String, String>, Boolean> {
    val transliterationMap = linkedMapOf<String, String>()
    val lines = content.lines().filter { it.isNotBlank() }
    var parseError = false

    for (line in lines) {
        val parts = line.split("：", ":", limit = 2)
        if (parts.size == 2) {
            val key = parts[0].trim()
            val value = parts[1].trim()
            if (key.isNotEmpty()) {
                transliterationMap[key] = value
            }
        } else {
            parseError = true
            break
        }
    }

    return transliterationMap to parseError
}

private fun applyTransliterationMapToLyrics(
    lyricLines: List<LyricLine>,
    transliterationMap: Map<String, String>
): Pair<List<LyricLine>, Int> {
    if (transliterationMap.isEmpty()) return lyricLines to 0

    val sortedKeys = transliterationMap.keys
        .filter { it.isNotBlank() }
        .sortedByDescending { it.length }

    var matchCount = 0
    val updatedLines = lyricLines.map { line ->
        val newUnits = line.timeUnits.map { unit ->
            val text = unit.text
            val newCharTransliterations = unit.charTransliterations.toMutableMap()
            var index = 0

            while (index < text.length) {
                val matchedKey = sortedKeys.firstOrNull { key ->
                    index + key.length <= text.length &&
                            text.regionMatches(index, key, 0, key.length, ignoreCase = false)
                }

                if (matchedKey != null) {
                    val targetChar = text[index]
                    val transliteration = transliterationMap[matchedKey].orEmpty()
                    if (isCJKCharacter(targetChar) && transliteration.isNotBlank()) {
                        newCharTransliterations[index] = transliteration
                        for (removeIdx in (index + 1) until (index + matchedKey.length)) {
                            newCharTransliterations.remove(removeIdx)
                        }
                        matchCount++
                    }
                    index += matchedKey.length
                } else {
                    index++
                }
            }

            unit.copy(charTransliterations = newCharTransliterations)
        }
        line.copy(timeUnits = newUnits)
    }

    return updatedLines to matchCount
}

@Composable
fun LyricLineItem(
    lineIndex: Int,
    lyricLine: LyricLine,
    selectedLineIndex: Int,
    selectedWordIndex: Int,
    currentTime: Long,
    isDarkTheme: Boolean,
    isFollowMode: Boolean,
    timestampMinWidth: Dp,
    lyricLines: List<LyricLine>,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onSelectedLineIndexChange: (Int) -> Unit,
    onSelectedWordIndexChange: (Int) -> Unit,
    onCurrentTimeChange: (Long) -> Unit,
    onIsFollowModeChange: (Boolean) -> Unit,
    onShowEditControlPanelChange: (Boolean) -> Unit,
    onMenuLineIndexChange: (Int) -> Unit,
    onMenuUnitIndexChange: (Int) -> Unit,
    onTranslationMenuLineIndexChange: (Int) -> Unit,
    onEditTranslationRequested: (Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayPause: (Boolean) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onUpdateJobCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${lineIndex + 1}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            val agentTypeOptions = listOf(stringResource(R.string.lyric_timing_agent_default), stringResource(R.string.lyric_timing_agent_duet), stringResource(R.string.lyric_timing_agent_background))
            val selectedAgentIndex = when (lyricLine.agentType) {
                LyricAgentType.LEFT -> 0
                LyricAgentType.RIGHT -> 1
                LyricAgentType.BACKGROUND -> 2
            }
            CustomRadioButtonGroup(
                options = agentTypeOptions,
                selectedIndex = selectedAgentIndex,
                onSelect = { index ->
                    if (lineIndex < lyricLines.size) {
                        val newLines = lyricLines.toMutableList()
                        val currentLine = newLines[lineIndex]
                        val newAgentType = when (index) {
                            0 -> LyricAgentType.LEFT
                            1 -> LyricAgentType.RIGHT
                            2 -> LyricAgentType.BACKGROUND
                            else -> LyricAgentType.LEFT
                        }
                        newLines[lineIndex] = currentLine.copy(agentType = newAgentType)
                        onLyricLinesChange(newLines)
                    }
                }
            )
        }
        
        val lineBgColor = when (lyricLine.agentType) {
            LyricAgentType.LEFT -> Color.Transparent
            LyricAgentType.RIGHT -> Color(0x100000FF)
            LyricAgentType.BACKGROUND -> Color(0x10FFA500)
        }
        
        // 判断当前行是否有注音
        val lineHasTransliteration = lyricLine.timeUnits.any { 
            it.transliteration.isNotEmpty() || it.charTransliterations.isNotEmpty() 
        }
        val isBlankLine = isBlankLyricLine(lyricLine)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(lineBgColor, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                if (isBlankLine) {
                    val isSelected = lineIndex == selectedLineIndex && selectedWordIndex == 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isFollowMode) {
                                        onIsFollowModeChange(false)
                                    }
                                    onSelectedLineIndexChange(lineIndex)
                                    onSelectedWordIndexChange(0)
                                },
                                onLongClick = {
                                    if (isFollowMode) {
                                        onIsFollowModeChange(false)
                                    }
                                    onMenuLineIndexChange(lineIndex)
                                    onMenuUnitIndexChange(0)
                                    onTranslationMenuLineIndexChange(lineIndex)
                                    onSelectedLineIndexChange(lineIndex)
                                    onSelectedWordIndexChange(0)
                                    onShowEditControlPanelChange(true)
                                    // 自动暂停歌曲
                                    onIsPlayingChange(false)
                                    onPlayPause(false)
                                    onUpdateJobCancel()
                                }
                            )
                            .padding(4.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .height(56.dp)
                            .widthIn(min = 72.dp)
                    )
                } else {
                    val isLineByLineLyric = isLineByLineTimingLine(lyricLines, lineIndex)
                    lyricLine.timeUnits.forEachIndexed { unitIndex, timeUnit ->
                    if (timeUnit.text.isNotBlank()) {
                        val isSelected = lineIndex == selectedLineIndex && unitIndex == selectedWordIndex
                        val startMs = parseTimeToMs(timeUnit.startTime)
                        val endMs = parseTimeToMs(timeUnit.endTime)
                        val shouldWarnZeroTimestamp = if (isLineByLineLyric) {
                            startMs == 0L
                        } else {
                            startMs == 0L || endMs == 0L
                        }
                        val isPlayingHighlight = startMs <= currentTime && currentTime < endMs
                        val unitBorderColor = when {
                            shouldWarnZeroTimestamp -> if (isDarkTheme) Color(0xFFFF8A80) else Color(0xFFD32F2F)
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> Color.Transparent
                        }
                        val unitBackgroundColor = when {
                            shouldWarnZeroTimestamp -> if (isDarkTheme) Color(0x33D32F2F) else Color(0x1FD32F2F)
                            isPlayingHighlight -> if (isDarkTheme) Color(0x406200EE) else Color(0x206200EE)
                            else -> Color.Transparent
                        }
                        
                        // 获取要显示的注音
                        val displayTransliteration = if (timeUnit.charTransliterations.isNotEmpty()) {
                            // 如果有单字符注音，组合起来显示
                            timeUnit.text.mapIndexed { idx, _ ->
                                timeUnit.charTransliterations[idx] ?: ""
                            }.joinToString("")
                        } else {
                            timeUnit.transliteration
                        }
                        
                        var lastClickTime by remember { mutableStateOf(0L) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (isFollowMode) {
                                            onIsFollowModeChange(false)
                                        }
                                        val clickTime = System.currentTimeMillis()
                                        if (clickTime - lastClickTime < 300) {
                                            onSelectedLineIndexChange(lineIndex)
                                            onSelectedWordIndexChange(unitIndex)
                                            val timeParts = timeUnit.startTime.split(":", ".")
                                            if (timeParts.size == 3) {
                                                val minutes = timeParts[0].toLong()
                                                val seconds = timeParts[1].toLong()
                                                val milliseconds = timeParts[2].toLong()
                                                val totalMs = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                                                onCurrentTimeChange(totalMs)
                                                onSeekTo(totalMs)
                                            }
                                        } else {
                                            onSelectedLineIndexChange(lineIndex)
                                            onSelectedWordIndexChange(unitIndex)
                                        }
                                        lastClickTime = clickTime
                                    },
                                    onLongClick = {
                                        if (isFollowMode) {
                                            onIsFollowModeChange(false)
                                        }
                                        onMenuLineIndexChange(lineIndex)
                                        onMenuUnitIndexChange(unitIndex)
                                        onTranslationMenuLineIndexChange(lineIndex)
                                        onSelectedLineIndexChange(lineIndex)
                                        onSelectedWordIndexChange(unitIndex)
                                        onShowEditControlPanelChange(true)
                                        // 自动暂停歌曲
                                        onIsPlayingChange(false)
                                        onPlayPause(false)
                                        onUpdateJobCancel()
                                    }
                                )
                                .padding(4.dp)
                                .border(
                                    width = 2.dp,
                                    color = unitBorderColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(
                                    unitBackgroundColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = timeUnit.startTime,
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32),
                                    modifier = Modifier.widthIn(min = timestampMinWidth)
                                )
                                
                                // 注音显示区域 - 根据字符位置精确显示
                                if (lineHasTransliteration) {
                                    if (timeUnit.charTransliterations.isNotEmpty()) {
                                        // 有单字符注音，逐个字符显示对应的注音
                                        Row(
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            timeUnit.text.forEachIndexed { charIndex, char ->
                                                val charTransliteration = timeUnit.charTransliterations[charIndex] ?: ""
                                                // 只有CJK字符才显示注音占位
                                                val isCjk = isCJKCharacter(char)
                                                if (isCjk) {
                                                    val hasTrans = charTransliteration.isNotEmpty()
                                                    // 根据注音长度调整宽度，确保长注音完整显示
                                                    val baseWidth = if (hasTrans) {
                                                        // 进一步增加最小宽度和字符宽度
                                                        maxOf(32.dp, (charTransliteration.length * 10).dp)
                                                    } else {
                                                        16.dp
                                                    }
                                                    
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.width(baseWidth)
                                                    ) {
                                                        Text(
                                                            text = charTransliteration,
                                                            fontSize = 10.sp,
                                                            color = Color.Gray,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    // 只在注音较长且下一个字符也有注音时才添加额外间距
                                                    if (hasTrans && charIndex < timeUnit.text.length - 1) {
                                                        val nextTrans = timeUnit.charTransliterations[charIndex + 1] ?: ""
                                                        if (charTransliteration.length >= 2 && nextTrans.isNotEmpty()) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                    }
                                                } else {
                                                    // 非CJK字符只保留占位
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        // 只有整体注音，居中显示
                                        Text(
                                            text = displayTransliteration,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                // 歌词文本显示区域 - 与注音对齐
                                if (lineHasTransliteration && timeUnit.charTransliterations.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        timeUnit.text.forEachIndexed { charIndex, char ->
                                            val charTransliteration = timeUnit.charTransliterations[charIndex] ?: ""
                                            val isCjk = isCJKCharacter(char)
                                            if (isCjk) {
                                                val hasTrans = charTransliteration.isNotEmpty()
                                                // 歌词宽度与对应注音的宽度保持一致
                                                val baseWidth = if (hasTrans) {
                                                    maxOf(32.dp, (charTransliteration.length * 10).dp)
                                                } else {
                                                    16.dp
                                                }
                                                
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.width(baseWidth)
                                                ) {
                                                    Text(
                                                        text = char.toString(),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                                // 同样添加额外的间距，与注音保持一致
                                                if (hasTrans && charIndex < timeUnit.text.length - 1) {
                                                    val nextTrans = timeUnit.charTransliterations[charIndex + 1] ?: ""
                                                    if (charTransliteration.length >= 2 && nextTrans.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                }
                                            } else {
                                                // 非CJK字符保持固定宽度
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.width(16.dp)
                                                ) {
                                                    Text(
                                                        text = char.toString(),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 没有注音时，正常显示整个歌词文本
                                    Text(
                                        text = timeUnit.text,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                Text(
                                    text = timeUnit.endTime,
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFEF5350) else Color(0xFFC62828),
                                    modifier = Modifier.widthIn(min = timestampMinWidth)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
        
        val translationLines = splitTranslationLines(lyricLine.translation)
        if (translationLines.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            onTranslationMenuLineIndexChange(lineIndex)
                            onMenuLineIndexChange(lineIndex)
                            onMenuUnitIndexChange(0)
                            onSelectedLineIndexChange(lineIndex)
                            onSelectedWordIndexChange(0)
                            onShowEditControlPanelChange(false)
                            // 自动暂停歌曲
                            onIsPlayingChange(false)
                            onPlayPause(false)
                            onUpdateJobCancel()
                            onEditTranslationRequested(lineIndex)
                        }
                    )
            ) {
                translationLines.forEach { translationLine ->
                    Text(
                        text = translationLine,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun CreatorsArea(
    creators: List<String>,
    onCreatorsChange: (List<String>) -> Unit,
    onEditCreator: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 72.dp, top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.lyric_timing_creators_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (creators.isEmpty()) {
            Text(
                text = stringResource(R.string.lyric_timing_creators_empty),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            creators.forEachIndexed { index, creator ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = creator.ifEmpty { stringResource(R.string.lyric_timing_creator_unnamed) },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 编辑按钮
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = { onEditCreator(index) })
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.pencil),
                            contentDescription = stringResource(R.string.common_edit),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // 删除按钮
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = {
                                onCreatorsChange(creators.toMutableList().apply {
                                    removeAt(index)
                                })
                            })
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.delete),
                            contentDescription = stringResource(R.string.common_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        Button(
            onClick = {
                onEditCreator(creators.size)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.lyric_timing_creator_add))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCreatorSheet(
    creatorIndex: Int,
    creatorName: String,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputName by remember { mutableStateOf(creatorName) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (creatorIndex >= 0) stringResource(R.string.lyric_timing_creator_edit) else stringResource(R.string.lyric_timing_creator_add_dialog),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = stringResource(R.string.lyric_timing_creator_name_label),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ThemedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                placeholder = stringResource(R.string.lyric_timing_creator_name_placeholder),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        onSave(creatorIndex, inputName.trim())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}

@Composable
fun TimingControlArea(
    hasLyrics: Boolean,
    isPreviewMode: Boolean,
    isFollowMode: Boolean,
    hasAudio: Boolean,
    selectedLineIndex: Int,
    selectedWordIndex: Int,
    lyricLines: List<LyricLine>,
    currentTime: Long,
    audioDuration: Long,
    seekTimeMs: Long,
    seekTimeSeconds: Double,
    isPlaying: Boolean,
    onIsFollowModeChange: (Boolean) -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onSelectedLineIndexChange: (Int) -> Unit,
    onSelectedWordIndexChange: (Int) -> Unit,
    onCurrentTimeChange: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayPause: (Boolean) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onUpdateJobCancel: () -> Unit,
    onShowEditControlPanelChange: (Boolean) -> Unit,
    onMenuLineIndexChange: (Int) -> Unit,
    onMenuUnitIndexChange: (Int) -> Unit,
    undoRedoManager: UndoRedoManager,
    onUpdateUndoRedoState: () -> Unit,
    coroutineScope: CoroutineScope,
    onPerformVibration: () -> Unit
) {
    if (hasLyrics && !isPreviewMode) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = if (hasAudio) 16.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 快退按钮
            Button(
                onClick = {
                    val newTime = maxOf(0L, currentTime - seekTimeMs)
                    onCurrentTimeChange(newTime)
                    onSeekTo(newTime)
                },
                modifier = Modifier.weight(0.8f),
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rewind),
                    contentDescription = stringResource(R.string.lyric_timing_seek_backward_seconds, seekTimeSeconds.toInt()),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }

            if (isFollowMode) {
                // 跟随模式下显示退出按钮
                Button(
                    onClick = { onIsFollowModeChange(false) },
                    modifier = Modifier.weight(3.4f)
                ) {
                    Text(text = stringResource(R.string.lyric_timing_exit_follow_mode))
                }
            } else {
                // 正常模式显示起始、连续、结束按钮
                Button(
                    onClick = {
                        onPerformVibration()
                        onMenuLineIndexChange(selectedLineIndex)
                        onMenuUnitIndexChange(selectedWordIndex)
                        onShowEditControlPanelChange(true)
                        // 自动暂停歌曲
                        if (isPlaying) {
                            onIsPlayingChange(false)
                            onPlayPause(false)
                            onUpdateJobCancel()
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(text = stringResource(R.string.lyric_timing_set_start), maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = {
                        onPerformVibration()
                        // 设置结束时间并跳转到下一个字
                        if (selectedLineIndex < lyricLines.size && selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size) {
                            val newLines = lyricLines.toMutableList()
                            val currentLine = newLines[selectedLineIndex]
                            val newTimeUnits = currentLine.timeUnits.toMutableList()
                            val currentTimeStr = formatTime(currentTime)

                            // 设置当前字的结束时间
                            val oldUnit = newTimeUnits[selectedWordIndex]
                            val newUnit = LyricTimeUnit(oldUnit.text, oldUnit.startTime, currentTimeStr)
                            undoRedoManager.pushAction(UndoAction(
                                actionType = UndoActionType.TIME_CHANGE,
                                lineIndex = selectedLineIndex,
                                unitIndex = selectedWordIndex,
                                oldValue = oldUnit,
                                newValue = newUnit
                            ))
                            onUpdateUndoRedoState()
                            newTimeUnits[selectedWordIndex] = newUnit
                            newLines[selectedLineIndex] = currentLine.copy(timeUnits = newTimeUnits)
                            onLyricLinesChange(newLines)

                            // 跳转到下一个有内容的字，跳过空白行组件
                            findNextSelectableTimingUnit(lyricLines, selectedLineIndex, selectedWordIndex)?.let { (lineIndex, unitIndex) ->
                                onSelectedLineIndexChange(lineIndex)
                                onSelectedWordIndexChange(unitIndex)
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(text = stringResource(R.string.lyric_timing_set_continuous), maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = {
                        onPerformVibration()
                        // 设置结束时间并跳转到下一个字
                        if (selectedLineIndex < lyricLines.size && selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size) {
                            val newLines = lyricLines.toMutableList()
                            val currentLine = newLines[selectedLineIndex]
                            val newTimeUnits = currentLine.timeUnits.toMutableList()
                            val currentTimeStr = formatTime(currentTime)

                            // 设置当前字的结束时间
                            val oldUnit = newTimeUnits[selectedWordIndex]
                            val newUnit = LyricTimeUnit(oldUnit.text, oldUnit.startTime, currentTimeStr)
                            undoRedoManager.pushAction(UndoAction(
                                actionType = UndoActionType.TIME_CHANGE,
                                lineIndex = selectedLineIndex,
                                unitIndex = selectedWordIndex,
                                oldValue = oldUnit,
                                newValue = newUnit
                            ))
                            onUpdateUndoRedoState()
                            newTimeUnits[selectedWordIndex] = newUnit
                            newLines[selectedLineIndex] = currentLine.copy(timeUnits = newTimeUnits)
                            onLyricLinesChange(newLines)

                            // 跳转到下一个有内容的字，跳过空白行组件
                            findNextSelectableTimingUnit(lyricLines, selectedLineIndex, selectedWordIndex)?.let { (lineIndex, unitIndex) ->
                                onSelectedLineIndexChange(lineIndex)
                                onSelectedWordIndexChange(unitIndex)
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(text = stringResource(R.string.lyric_timing_set_end), maxLines = 1, softWrap = false)
                }
            }

            // 快进按钮
            Button(
                onClick = {
                    val newTime = minOf(audioDuration, currentTime + seekTimeMs)
                    onCurrentTimeChange(newTime)
                    onSeekTo(newTime)
                },
                modifier = Modifier.weight(0.8f),
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.fastforward),
                    contentDescription = stringResource(R.string.lyric_timing_seek_forward_seconds, seekTimeSeconds.toInt()),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun LyricDisplayArea(
    lyricLines: List<LyricLine>,
    selectedLineIndex: Int,
    selectedWordIndex: Int,
    currentTime: Long,
    isDarkTheme: Boolean,
    isFollowMode: Boolean,
    isPlaying: Boolean,
    hasLyrics: Boolean,
    timestampMinWidth: Dp,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope,
    creators: List<String>,
    onCreatorsChange: (List<String>) -> Unit,
    onLyricLinesChange: (List<LyricLine>) -> Unit,
    onSelectedLineIndexChange: (Int) -> Unit,
    onSelectedWordIndexChange: (Int) -> Unit,
    onCurrentTimeChange: (Long) -> Unit,
    onIsFollowModeChange: (Boolean) -> Unit,
    onShowEditControlPanelChange: (Boolean) -> Unit,
    onMenuLineIndexChange: (Int) -> Unit,
    onMenuUnitIndexChange: (Int) -> Unit,
    onTranslationMenuLineIndexChange: (Int) -> Unit,
    onEditTranslationRequested: (Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayPause: (Boolean) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onUpdateJobCancel: () -> Unit,
    onIsPreviewModeChange: (Boolean) -> Unit,
    isPreviewMode: Boolean,
    modifier: Modifier = Modifier
) {
    var showEditCreatorSheet by remember { mutableStateOf(false) }
    var editingCreatorIndex by remember { mutableStateOf(-1) }
    var lyricScrollbarDragging by remember { mutableStateOf(false) }
    var lyricScrollbarProgress by remember { mutableStateOf(0f) }
    var targetScrollbarAlpha by remember { mutableStateOf(1f) }
    val animatedScrollbarAlpha by animateFloatAsState(
        targetValue = targetScrollbarAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "lyricScrollbarAlpha"
    )
    var scrollbarHideTimerJob by remember { mutableStateOf<Job?>(null) }

    fun resetScrollbarHideTimer() {
        scrollbarHideTimerJob?.cancel()
        targetScrollbarAlpha = 1f
        scrollbarHideTimerJob = coroutineScope.launch {
            delay(1000)
            targetScrollbarAlpha = 0.2f
        }
    }

    LaunchedEffect(Unit) {
        resetScrollbarHideTimer()
    }
    LaunchedEffect(
        lazyListState.firstVisibleItemIndex,
        lazyListState.firstVisibleItemScrollOffset,
        lyricLines.size,
        hasLyrics
    ) {
        resetScrollbarHideTimer()
        if (!lyricScrollbarDragging) {
            val totalItemsForScrollbar = lazyListState.layoutInfo.totalItemsCount
            val visibleItemsCount = lazyListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val maxScrollIndex = (totalItemsForScrollbar - visibleItemsCount).coerceAtLeast(0)
            val firstVisibleItemSize = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull()
                ?.size
                ?.coerceAtLeast(1)
                ?: 1
            val rawPosition = lazyListState.firstVisibleItemIndex.toFloat() +
                lazyListState.firstVisibleItemScrollOffset.toFloat() / firstVisibleItemSize.toFloat()
            lyricScrollbarProgress = if (maxScrollIndex > 0) {
                when {
                    !lazyListState.canScrollBackward -> 0f
                    !lazyListState.canScrollForward -> 1f
                    else -> rawPosition / maxScrollIndex.toFloat()
                }
            } else {
                0f
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(lyricLines) { lineIndex, lyricLine ->
                LyricLineItem(
                    lineIndex = lineIndex,
                    lyricLine = lyricLine,
                    selectedLineIndex = selectedLineIndex,
                    selectedWordIndex = selectedWordIndex,
                    currentTime = currentTime,
                    isDarkTheme = isDarkTheme,
                    isFollowMode = isFollowMode,
                    timestampMinWidth = timestampMinWidth,
                    lyricLines = lyricLines,
                    onLyricLinesChange = onLyricLinesChange,
                    onSelectedLineIndexChange = onSelectedLineIndexChange,
                    onSelectedWordIndexChange = onSelectedWordIndexChange,
                    onCurrentTimeChange = onCurrentTimeChange,
                    onIsFollowModeChange = onIsFollowModeChange,
                    onShowEditControlPanelChange = onShowEditControlPanelChange,
                    onMenuLineIndexChange = onMenuLineIndexChange,
                    onMenuUnitIndexChange = onMenuUnitIndexChange,
                    onTranslationMenuLineIndexChange = onTranslationMenuLineIndexChange,
                    onEditTranslationRequested = onEditTranslationRequested,
                    onSeekTo = onSeekTo,
                    onPlayPause = onPlayPause,
                    onIsPlayingChange = onIsPlayingChange,
                    onUpdateJobCancel = onUpdateJobCancel
                )
            }
            
            if (hasLyrics) {
                item {
                    CreatorsArea(
                        creators = creators,
                        onCreatorsChange = onCreatorsChange,
                        onEditCreator = { index ->
                            editingCreatorIndex = index
                            showEditCreatorSheet = true
                        }
                    )
                }
            }
        }

        val totalItemsForScrollbar = lazyListState.layoutInfo.totalItemsCount
        if (totalItemsForScrollbar > 0) {
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height
            if (viewportHeight > 0) {
                val density = LocalDensity.current
                val visibleItemsCount = lazyListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                val hasScrollableContent = totalItemsForScrollbar > visibleItemsCount ||
                    lazyListState.canScrollBackward ||
                    lazyListState.canScrollForward
                val scrollbarHeightPx = (viewportHeight - 16f).coerceAtLeast(1f)
                val thumbHeightPx = if (hasScrollableContent) {
                    (scrollbarHeightPx * 0.18f).coerceIn(80f, scrollbarHeightPx)
                } else {
                    scrollbarHeightPx
                }
                val maxScrollIndex = (totalItemsForScrollbar - visibleItemsCount).coerceAtLeast(0)
                val scrollRangePx = (scrollbarHeightPx - thumbHeightPx).coerceAtLeast(1f)
                val thumbOffsetY = lyricScrollbarProgress.coerceIn(0f, 1f) * scrollRangePx

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
                        .width(24.dp)
                        .height(with(density) { scrollbarHeightPx.toDp() })
                        .graphicsLayer {
                            alpha = animatedScrollbarAlpha
                        }
                        .pointerInput(totalItemsForScrollbar, viewportHeight) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    resetScrollbarHideTimer()
                                    lyricScrollbarDragging = true
                                    lyricScrollbarProgress =
                                        ((offset.y - thumbHeightPx / 2f) / scrollRangePx).coerceIn(0f, 1f)
                                    val targetIndex = (lyricScrollbarProgress * maxScrollIndex).toInt()
                                        .coerceIn(0, maxScrollIndex)
                                    coroutineScope.launch { lazyListState.scrollToItem(targetIndex) }
                                },
                                onDragEnd = {
                                    lyricScrollbarDragging = false
                                    resetScrollbarHideTimer()
                                },
                                onDragCancel = {
                                    lyricScrollbarDragging = false
                                    resetScrollbarHideTimer()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                resetScrollbarHideTimer()
                                lyricScrollbarProgress = if (hasScrollableContent) {
                                    ((change.position.y - thumbHeightPx / 2f) / scrollRangePx)
                                        .coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                val targetIndex =
                                    (lyricScrollbarProgress * maxScrollIndex).toInt()
                                        .coerceIn(0, maxScrollIndex)
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(targetIndex)
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = with(density) { thumbOffsetY.toDp() })
                            .width(24.dp)
                            .height(with(density) { thumbHeightPx.toDp() })
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(8.dp)
                                .height(with(density) { thumbHeightPx.toDp() })
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (lyricScrollbarDragging) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    }
                                )
                        )
                    }
                }
            }
        }
        
        val isLineVisible = lazyListState.layoutInfo.visibleItemsInfo.any { it.index == selectedLineIndex }
        val showFab = hasLyrics && selectedLineIndex >= 0 && selectedLineIndex < lyricLines.size && !isLineVisible
        val fabScale by animateFloatAsState(
            targetValue = if (showFab) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "fabScale"
        )
        
        // 计算当前播放高亮行是否在屏幕中可见
        var playingLineIndex by remember { mutableStateOf(-1) }
        if (lyricLines.isNotEmpty()) {
            // 首先检查是否有正在播放的歌词单元
            for (lineIndex in lyricLines.indices) {
                val line = lyricLines[lineIndex]
                for (unit in line.timeUnits) {
                    val startMs = parseTimeToMs(unit.startTime)
                    val endMs = parseTimeToMs(unit.endTime)
                    if (startMs <= currentTime && currentTime < endMs) {
                        playingLineIndex = lineIndex
                        break
                    }
                }
                if (playingLineIndex >= 0) break
            }
            
            // 如果没有正在播放的歌词单元，检查当前时间是否在某一行歌词的整体时间范围内
            if (playingLineIndex == -1) {
                for (lineIndex in lyricLines.indices) {
                    val line = lyricLines[lineIndex]
                    if (line.timeUnits.isNotEmpty()) {
                        // 获取该行的整体时间范围
                        val firstUnitStartMs = parseTimeToMs(line.timeUnits.first().startTime)
                        val lastUnitEndMs = parseTimeToMs(line.timeUnits.last().endTime)
                        
                        // 检查当前时间是否在该行的整体时间范围内
                        if (firstUnitStartMs <= currentTime && currentTime < lastUnitEndMs) {
                            playingLineIndex = lineIndex
                            break
                        }
                    }
                }
            }
        }
        val isPlayingLineVisible = playingLineIndex >= 0 && 
            lazyListState.layoutInfo.visibleItemsInfo.any { it.index == playingLineIndex }
        val showFollowFab = hasLyrics && playingLineIndex >= 0 && !isPlayingLineVisible && !isFollowMode
        val followFabScale by animateFloatAsState(
            targetValue = if (showFollowFab) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "followFabScale"
        )
        
        // 右下角按钮区域
        if (hasLyrics) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                // 定位按钮
                FloatingActionButton(

                    onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(selectedLineIndex, scrollOffset = 0)
                        }
                    },

                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                            alpha = fabScale
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_my_location_24),
                        contentDescription = stringResource(R.string.lyric_timing_locate_selected_lyric),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        // 预览模式覆盖层
        if (isPreviewMode) {
            LyricPreviewOverlay(
                lyricLines = lyricLines,
                currentTime = currentTime,
                isPlaying = isPlaying,
                onBack = { onIsPreviewModeChange(false) },
                onPlayPause = { 
                    onIsPlayingChange(!isPlaying)
                    onPlayPause(!isPlaying)
                },
                onSeek = { position ->
                    onSeekTo(position)
                },
                onLineClick = { lineIndex ->
                    if (lineIndex < lyricLines.size) {
                        val line = lyricLines[lineIndex]
                        val timeStr = line.timeUnits.firstOrNull()?.startTime ?: "00:00.000"
                        val positionMs = parseTimeToMs(timeStr)
                        onSeekTo(positionMs)
                    }
                }
            )
        }
        
        // 编辑创作者的 ModalBottomSheet
        if (showEditCreatorSheet) {
            val creatorName = if (editingCreatorIndex >= 0 && editingCreatorIndex < creators.size) {
                creators[editingCreatorIndex]
            } else {
                ""
            }
            EditCreatorSheet(
                creatorIndex = editingCreatorIndex,
                creatorName = creatorName,
                onDismiss = {
                    showEditCreatorSheet = false
                    editingCreatorIndex = -1
                },
                onSave = { index, name ->
                    if (index >= 0 && index < creators.size) {
                        // 编辑现有创作者
                        onCreatorsChange(creators.toMutableList().apply {
                            this[index] = name
                        })
                    } else {
                        // 添加新创作者
                        onCreatorsChange(creators + name)
                    }
                }
            )
        }
    }
}

@Composable
fun ImportButtons(
    hasLyrics: Boolean,
    hasAudio: Boolean,
    showImportLyricMenu: Boolean,
    sourceTitle: String,
    sourceArtist: String,
    onShowImportLyricMenuChange: (Boolean) -> Unit,
    onShowLyricInputDialogChange: () -> Unit,
    onShowSPLLrcInputDialogChange: () -> Unit,
    onShowElrcInputDialogChange: () -> Unit,
    onShowTtmlInputDialogChange: () -> Unit,
    onOpenVerbatimLyrics: (String) -> Unit,
    onImportAudio: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!hasLyrics) {
            var importLyricButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
            val density = LocalDensity.current
            Box {
                Button(
                    onClick = { onShowImportLyricMenuChange(true) },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            val centerX = bounds.center.x
                            val centerY = bounds.center.y
                            importLyricButtonPosition = MenuAnchorPosition(
                                x = with(density) { centerX.toDp().value },
                                y = with(density) { centerY.toDp().value }
                            )
                        }
                ) {
                    Text(text = stringResource(R.string.lyric_timing_import_lyrics))
                }
                CustomDropdownMenu(
                    expanded = showImportLyricMenu,
                    onDismissRequest = { onShowImportLyricMenuChange(false) },
                    items = listOf(
                        MenuItem(title = stringResource(R.string.lyric_timing_menu_import_plain_text), onClick = {
                            onShowImportLyricMenuChange(false); onShowLyricInputDialogChange() }),
                        MenuItem(title = stringResource(R.string.lyric_timing_menu_import_lrc), onClick = {
                            onShowImportLyricMenuChange(false); onShowSPLLrcInputDialogChange() }),
                        MenuItem(title = stringResource(R.string.lyric_timing_menu_import_elrc), onClick = {
                            onShowImportLyricMenuChange(false); onShowElrcInputDialogChange() }),
                        MenuItem(title = stringResource(R.string.lyric_timing_menu_import_ttml), onClick = {
                            onShowImportLyricMenuChange(false); onShowTtmlInputDialogChange() }),
                        MenuItem(title = stringResource(R.string.lyric_timing_menu_fetch_verbatim), onClick = {
                            onShowImportLyricMenuChange(false)
                            val keyword = if (sourceTitle.isNotEmpty() && sourceArtist.isNotEmpty()) "$sourceTitle $sourceArtist" else sourceTitle
                            onOpenVerbatimLyrics(keyword)
                        })
                    ),
                    anchorPosition = importLyricButtonPosition ?: MenuAnchorPosition(0f, 0f)
                )
            }
        }
        
        if (!hasAudio) {
            // 未导入音频：显示导入音频按钮
            Button(
                onClick = { onImportAudio() },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(text = stringResource(R.string.lyric_timing_menu_import_audio))
            }
        }
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    currentTime: Long,
    audioDuration: Long,
    hasAudio: Boolean,
    hasLyrics: Boolean,
    onPlayPause: (Boolean) -> Unit,
    onGetCurrentPosition: () -> Long,
    onGetAudioDuration: () -> Long,
    onUpdateJobChange: (Job?) -> Unit,
    onCurrentTimeChange: (Long) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onSeekTo: (Long) -> Unit,
    onShowTimeInputDialogChange: (Boolean) -> Unit,
    onWasPlayingBeforeTimeInputChange: (Boolean) -> Unit,
    onInputTimeMinutesChange: (String) -> Unit,
    onInputTimeSecondsChange: (String) -> Unit,
    onInputTimeMillisecondsChange: (String) -> Unit,
    coroutineScope: CoroutineScope,
    context: Context
) {
    var updateJob: Job? by remember { mutableStateOf(null) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 播放暂停按钮
        Button(
            onClick = {
                if (isPlaying) {
                    onPlayPause(false)
                    updateJob?.cancel()
                    onUpdateJobChange(null)
                } else {
                    onPlayPause(true)
                    val newDuration = onGetAudioDuration()
                    updateJob = coroutineScope.launch {
                        while (true) {
                            delay(100)
                            onCurrentTimeChange(onGetCurrentPosition())
                        }
                    }
                    onUpdateJobChange(updateJob)
                }
                onIsPlayingChange(!isPlaying)
            },
            contentPadding = PaddingValues(8.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                ),
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        // 播放进度条
        Slider(
            value = currentTime.toFloat(),
            onValueChange = { value ->
                val newTime = value.toLong()
                onCurrentTimeChange(newTime)
                onSeekTo(newTime)
            },
            valueRange = 0f..audioDuration.toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        )
        Text(
            text = formatTime(currentTime),
            modifier = Modifier
                .widthIn(min = 80.dp)
                .clickable {
                    if (hasAudio) {
                        onWasPlayingBeforeTimeInputChange(isPlaying)
                        if (isPlaying) {
                            onIsPlayingChange(false)
                            onPlayPause(false)
                        }
                        val currentMinutes = (currentTime / 60000) % 60
                        val currentSeconds = (currentTime / 1000) % 60
                        val currentMs = currentTime % 1000
                        onInputTimeMinutesChange(currentMinutes.toString())
                        onInputTimeSecondsChange(currentSeconds.toString())
                        onInputTimeMillisecondsChange(currentMs.toString())
                        onShowTimeInputDialogChange(true)
                    }
                },
            fontSize = 14.sp
        )
    }
}

@Composable
fun BottomControlArea(
    hasLyrics: Boolean,
    hasAudio: Boolean,
    showImportLyricMenu: Boolean,
    isPlaying: Boolean,
    currentTime: Long,
    audioDuration: Long,
    sourceTitle: String,
    sourceArtist: String,
    onShowImportLyricMenuChange: (Boolean) -> Unit,
    onShowLyricInputDialogChange: () -> Unit,
    onShowSPLLrcInputDialogChange: () -> Unit,
    onShowElrcInputDialogChange: () -> Unit,
    onShowTtmlInputDialogChange: () -> Unit,
    onOpenVerbatimLyrics: (String) -> Unit,
    onImportAudio: () -> Unit,
    onPlayPause: (Boolean) -> Unit,
    onGetCurrentPosition: () -> Long,
    onGetAudioDuration: () -> Long,
    onUpdateJobChange: (Job?) -> Unit,
    onCurrentTimeChange: (Long) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onSeekTo: (Long) -> Unit,
    onShowTimeInputDialogChange: (Boolean) -> Unit,
    onWasPlayingBeforeTimeInputChange: (Boolean) -> Unit,
    onInputTimeMinutesChange: (String) -> Unit,
    onInputTimeSecondsChange: (String) -> Unit,
    onInputTimeMillisecondsChange: (String) -> Unit,
    coroutineScope: CoroutineScope,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 导入按钮区域
        if (!hasLyrics || !hasAudio) {
            ImportButtons(
                hasLyrics = hasLyrics,
                hasAudio = hasAudio,
                showImportLyricMenu = showImportLyricMenu,
                sourceTitle = sourceTitle,
                sourceArtist = sourceArtist,
                onShowImportLyricMenuChange = onShowImportLyricMenuChange,
                onShowLyricInputDialogChange = onShowLyricInputDialogChange,
                onShowSPLLrcInputDialogChange = onShowSPLLrcInputDialogChange,
                onShowElrcInputDialogChange = onShowElrcInputDialogChange,
                onShowTtmlInputDialogChange = onShowTtmlInputDialogChange,
                onOpenVerbatimLyrics = onOpenVerbatimLyrics,
                onImportAudio = onImportAudio
            )
        }
        
        // 播放控制区域（已导入音频）
        if (hasAudio) {
            PlaybackControls(
                isPlaying = isPlaying,
                currentTime = currentTime,
                audioDuration = audioDuration,
                hasAudio = hasAudio,
                hasLyrics = hasLyrics,
                onPlayPause = onPlayPause,
                onGetCurrentPosition = onGetCurrentPosition,
                onGetAudioDuration = onGetAudioDuration,
                onUpdateJobChange = onUpdateJobChange,
                onCurrentTimeChange = onCurrentTimeChange,
                onIsPlayingChange = onIsPlayingChange,
                onSeekTo = onSeekTo,
                onShowTimeInputDialogChange = onShowTimeInputDialogChange,
                onWasPlayingBeforeTimeInputChange = onWasPlayingBeforeTimeInputChange,
                onInputTimeMinutesChange = onInputTimeMinutesChange,
                onInputTimeSecondsChange = onInputTimeSecondsChange,
                onInputTimeMillisecondsChange = onInputTimeMillisecondsChange,
                coroutineScope = coroutineScope,
                context = context
            )
        }
    }
}

@Composable
fun CreatorsSection(
    creators: List<String>,
    onCreatorsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.lyric_timing_creators_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        creators.forEachIndexed { index, creator ->
            OutlinedTextField(
                value = creator,
                onValueChange = { newValue ->
                    onCreatorsChange(creators.toMutableList().apply {
                        if (index < size) {
                            this[index] = newValue
                        }
                    })
                },
                label = { Text(stringResource(R.string.lyric_timing_creator_label, index + 1)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        Button(
            onClick = {
                onCreatorsChange(creators + "")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.lyric_timing_creator_add))
        }
    }
}

enum class UndoActionType {
    TIME_CHANGE,
    TEXT_CHANGE,
    UNIT_ADD,
    UNIT_DELETE,
    LINE_ADD,
    LINE_DELETE,
    LINE_MERGE,
    UNIT_MERGE,
    UNIT_SPLIT,
    TRANSLATION_CHANGE,
    BATCH_TIME_SHIFT,
    BATCH_DELETE_LINES,
    BATCH_MERGE_UNITS,
    BATCH_SEGMENT,
    BATCH_CONVERT_TO_SIMPLIFIED,
    BATCH_FORMAT_TIMELINE,
    MULTI_CHANGE
}

data class UndoAction(
    val actionType: UndoActionType,
    val lineIndex: Int,
    val unitIndex: Int = -1,
    val oldValue: Any?,
    val newValue: Any?,
    val timestamp: Long = System.currentTimeMillis()
)

data class BatchUndoAction(
    val actions: List<UndoAction>,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class UndoRedoManager(private val maxSize: Int = 20) {
    private val undoStack = mutableListOf<Any>()
    private val redoStack = mutableListOf<Any>()
    
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    fun pushAction(action: UndoAction) {
        undoStack.add(action)
        if (undoStack.size > maxSize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }
    
    fun pushBatchAction(batchAction: BatchUndoAction) {
        undoStack.add(batchAction)
        if (undoStack.size > maxSize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }
    
    fun undo(): Any? {
        return if (undoStack.isNotEmpty()) {
            val action = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(action)
            action
        } else null
    }
    
    fun redo(): Any? {
        return if (redoStack.isNotEmpty()) {
            val action = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(action)
            action
        } else null
    }
    
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonHeadBar(
    title: String,
    showBack: Boolean = true,
    showMenu: Boolean = false,
    leadingIconResId: Int = R.drawable.baseline_arrow_back_24,
    onBackClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    leadingMenuContent: @Composable (backButtonPosition: MenuAnchorPosition?) -> Unit = {},
    menuContent: @Composable (menuButtonPosition: MenuAnchorPosition?) -> Unit = {}
) {
    val iconColor = MaterialTheme.colorScheme.onPrimaryContainer
    var backButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    var menuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    val density = LocalDensity.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .lyricHorizontalSafeDrawingPadding()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showBack) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            val centerX = bounds.center.x
                            val centerY = bounds.center.y
                            backButtonPosition = MenuAnchorPosition(
                                x = with(density) { centerX.toDp().value },
                                y = with(density) { centerY.toDp().value }
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = leadingIconResId),
                            contentDescription = stringResource(R.string.common_back),
                            tint = iconColor
                        )
                    }
                }
                leadingMenuContent(backButtonPosition)
            }
            
            androidx.compose.animation.AnimatedContent(
                targetState = title,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 200))
                },
                modifier = Modifier.weight(2f),
                label = "titleAnimation"
            ) { targetTitle ->
                val interactionSource = remember { MutableInteractionSource() }
                var useCompactTitle by remember(targetTitle) { mutableStateOf(false) }
                val titleTextStyle = if (useCompactTitle) {
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    MaterialTheme.typography.titleLarge
                }
                Text(
                    text = targetTitle,
                    style = titleTextStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onTitleClick != null) {
                                Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onTitleClick() }
                            } else {
                                Modifier
                            }
                        ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layoutResult ->
                        if (!useCompactTitle && (layoutResult.lineCount > 1 || layoutResult.hasVisualOverflow)) {
                            useCompactTitle = true
                        }
                    }
                )
            }
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (showMenu) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            val centerX = bounds.center.x
                            val centerY = bounds.center.y
                            menuButtonPosition = MenuAnchorPosition(
                                x = with(density) { centerX.toDp().value },
                                y = with(density) { centerY.toDp().value }
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_more_vert_24),
                            contentDescription = stringResource(R.string.common_menu),
                            tint = iconColor
                        )
                    }
                }
                
                menuContent(menuButtonPosition)
            }
        }
    }
}

@Composable
private fun rememberBottomSheetListScrollBlocker(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(x = 0f, y = available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(x = 0f, y = available.y)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun hideSheetAndDismiss(
    scope: CoroutineScope,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit
) {
    scope.launch {
        sheetState.hide()
        onDismiss()
    }
}

@Composable
private fun BatchEditSheetMenuHeader(
    title: String,
    onSelectAll: () -> Unit,
    onInvertSelect: () -> Unit,
    onClosePage: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    val density = LocalDensity.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                menuAnchor = MenuAnchorPosition(
                    x = with(density) { bounds.center.x.toDp().value },
                    y = with(density) { bounds.center.y.toDp().value }
                )
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.more),
                contentDescription = stringResource(R.string.common_more),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    CustomDropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        items = listOf(
            MenuItem(title = stringResource(R.string.common_select_all), onClick = onSelectAll),
            MenuItem(title = stringResource(R.string.common_invert_select), onClick = onInvertSelect),
            MenuItem(title = stringResource(R.string.lyric_timing_close_page), onClick = onClosePage)
        ),
        anchorPosition = menuAnchor ?: MenuAnchorPosition(0f, 0f),
        menuWidth = 200f
    )
}

@Composable
fun TimingControlBarSwitch(
    showEditPanel: Boolean,
    normalControlBar: @Composable () -> Unit,
    editControlPanel: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = showEditPanel,
        transitionSpec = {
            if (targetState) {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height } + fadeOut()
                )
            } else {
                (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> height } + fadeOut()
                )
            }.using(
                SizeTransform()
            )
        },
        label = "TimingControlBarSwitch",
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) { showEdit ->
        if (showEdit) editControlPanel() else normalControlBar()
    }
}

@Composable
fun EditControlPanel(
    menuLineIndex: Int,
    menuUnitIndex: Int,
    lyricLines: List<LyricLine>,
    onDismiss: () -> Unit,
    onEditLyric: () -> Unit,
    onAddLyric: () -> Unit,
    onSplitLyric: () -> Unit,
    onMergeLyric: () -> Unit,
    onSetTimestamp: () -> Unit,
    onDeleteLyric: () -> Unit,
    onAddLine: () -> Unit,
    onMergeLine: () -> Unit,
    onSplitLine: () -> Unit,
    onMoveLine: () -> Unit,
    onAddTranslation: () -> Unit,
    onEditTranslation: () -> Unit,
    onDeleteLine: () -> Unit,
    showAddTranslation: Boolean,
    showEditTranslation: Boolean,
    isBlankLineMenu: Boolean = false
) {
    val scrollState1 = rememberScrollState()
    val scrollState2 = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
            .animateContentSize()
    ) {
        // 修改歌词区域
        Text(
            text = stringResource(R.string.lyric_timing_edit_lyrics_section),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState1),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isBlankLineMenu) {
                Button(
                    onClick = onEditLyric,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.common_edit), fontSize = 14.sp)
                }
            }
            Button(
                onClick = onAddLyric,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(stringResource(R.string.common_add), fontSize = 14.sp)
            }
            if (!isBlankLineMenu) {
                Button(
                    onClick = onSplitLyric,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.common_split), fontSize = 14.sp)
                }
                Button(
                    onClick = onMergeLyric,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.common_merge), fontSize = 14.sp)
                }
                Button(
                    onClick = onSetTimestamp,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.lyric_timing_set_time), fontSize = 14.sp)
                }
                Button(
                    onClick = onDeleteLyric,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.common_delete), fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 修改行区域
        Text(
            text = stringResource(R.string.lyric_timing_edit_lines_section),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState2),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddLine,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(stringResource(R.string.common_add), fontSize = 14.sp)
            }
            if (!isBlankLineMenu) {
                Button(
                    onClick = onSplitLine,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.common_split), fontSize = 14.sp)
                }
                Button(
                    onClick = onMergeLine,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.common_merge), fontSize = 14.sp)
                }
                Button(
                    onClick = onMoveLine,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.common_move), fontSize = 14.sp)
                }
            }
            if (!isBlankLineMenu && showAddTranslation) {
                Button(
                    onClick = onAddTranslation,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.lyric_timing_add_translation), fontSize = 14.sp)
                }
            }
            if (!isBlankLineMenu && showEditTranslation) {
                Button(
                    onClick = onEditTranslation,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.lyric_timing_edit_translation), fontSize = 14.sp)
                }
            }
            Button(
                onClick = onDeleteLine,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.common_delete), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun CustomRadioButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        if (isSelected) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (checked) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedLrcSaveDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    lyricLines: List<LyricLine>,
    showDuet: Boolean,
    onShowDuetChange: (Boolean) -> Unit,
    onCopied: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enhancedLrcContent = remember(lyricLines, showDuet) {
        LyricSaveEmbedUtils.buildEnhancedLrc(lyricLines, showDuet)
    }
    if (showDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_save_as_enhanced_lrc),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = enhancedLrcContent,
                        fontSize = 12.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                CustomCheckbox(
                    checked = showDuet,
                    onCheckedChange = onShowDuetChange,
                    label = stringResource(R.string.lyric_timing_show_duet_tags)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(enhancedLrcContent)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text(stringResource(R.string.common_copy))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbedEnhancedLrcDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    lyricLines: List<LyricLine>,
    showDuet: Boolean,
    onShowDuetChange: (Boolean) -> Unit,
    onCopied: () -> Unit,
    displayTitle: String,
    sourceAudioPath: String,
    sourceMediaStoreId: Long,
    onEmbedResult: (Boolean, String, Boolean, IntentSender?) -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enhancedLrcContent = remember(lyricLines, showDuet) {
        LyricSaveEmbedUtils.buildEnhancedLrc(lyricLines, showDuet)
    }
    val scrollState = rememberScrollState()
    if (showDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_embed_enhanced_lrc),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(R.string.lyric_timing_confirm_embed_to_target, displayTitle),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = enhancedLrcContent,
                        fontSize = 10.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                CustomCheckbox(
                    checked = showDuet,
                    onCheckedChange = onShowDuetChange,
                    label = stringResource(R.string.lyric_timing_show_duet_tags)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(enhancedLrcContent)
                            onCopied()
                        }
                    ) {
                        Text(stringResource(R.string.common_copy))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            if (sourceAudioPath.isEmpty()) {
                                onEmbedResult(false, context.getString(R.string.lyric_timing_audio_path_empty_embed_failed), false, null)
                                return@Button
                            }
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                Log.d("LyricTiming", "Embedding Enhanced LRC lyrics to: $sourceAudioPath")
                                val result = LyricSaveEmbedUtils.embedLyrics(
                                    context = context,
                                    sourceAudioPath = sourceAudioPath,
                                    lyricsContent = enhancedLrcContent,
                                    mediaStoreId = sourceMediaStoreId
                                )
                                Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                withContext(Dispatchers.Main) {
                                    onEmbedResult(
                                        result.success,
                                        if (result.success) context.getString(R.string.lyric_timing_embed_success) else result.errorMessage,
                                        result.needPermission,
                                        result.recoverableIntentSender
                                    )
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.lyric_timing_confirm_embed))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineLyricSaveDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    lyricLines: List<LyricLine>,
    showLineEndTime: Boolean,
    onShowLineEndTimeChange: (Boolean) -> Unit,
    onCopied: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lineLyricContent = remember(lyricLines, showLineEndTime) {
        LyricSaveEmbedUtils.buildLineLrc(lyricLines, showLineEndTime)
    }
    
    if (showDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_save_as_lrc_line),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = lineLyricContent,
                        fontSize = 12.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                CustomCheckbox(
                    checked = showLineEndTime,
                    onCheckedChange = onShowLineEndTimeChange,
                    label = stringResource(R.string.lyric_timing_show_line_end_timestamp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(lineLyricContent)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text(stringResource(R.string.common_copy))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtmlSaveDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    ttmlContent: String,
    onCopied: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_save_as_ttml),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = ttmlContent,
                        fontSize = 10.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(ttmlContent)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text(stringResource(R.string.common_copy))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLyricSaveDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    savedLyric: String,
    onCopied: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showDialog) {
        androidx.compose.material3.ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetMaxWidth = Dp.Unspecified,
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_save_as_lrc_word),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = savedLyric,
                        fontSize = 10.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(savedLyric)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text(stringResource(R.string.common_copy))
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleAlertDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String? = null
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(confirmButtonText ?: stringResource(R.string.common_confirm))
                }
            }
        )
    }
}

@Composable
fun ConfirmDialog(
    showDialog: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    title: String,
    text: String
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
fun UndoRedoFloatingButton(
    hasLyrics: Boolean,
    isPreviewMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hasLyrics && !isPreviewMode) {
        Row(
            modifier = modifier
                .statusBarsPadding()
                .padding(top = 72.dp, end = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.undo),
                    contentDescription = stringResource(R.string.lyric_timing_undo),
                    tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.redo),
                    contentDescription = stringResource(R.string.lyric_timing_redo),
                    tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ImportExampleDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.lyric_timing_import_example)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_basic),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_basic_desc),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_case1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "欢迎_使用__Lyric_Box",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_case1_result),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_case2),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "欢迎使用_LunaBeat",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_case2_result),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_case3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hello_World=你好世界\nHow_are_you=你好吗",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_case3_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.common_hint_colon),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lyric_timing_import_example_tip_segment),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.common_got_it))
                }
            }
        )
    }
}

private fun notifyTimingSourceLyricsUpdated(sourceAudioPath: String) {
    AudioMetadataUpdateBus.notifyPathUpdated(sourceAudioPath)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@androidx.compose.runtime.NonRestartableComposable
@androidx.compose.runtime.NonSkippableComposable
fun LyricTimingScreen(
    onBack: (List<LyricLine>) -> Unit,
    onImportAudio: () -> Unit,
    onPlayPause: (Boolean) -> Unit,
    onReloadCurrentAudio: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    getCurrentPosition: () -> Long,
    getAudioDuration: () -> Long,
    showConfirmDialog: Boolean,
    onConfirmDialogChange: (Boolean) -> Unit,
    hasLyrics: Boolean,
    onHasLyricsChange: (Boolean) -> Unit,
    showConvertDialog: Boolean = false,
    onConvertDialogChange: (Boolean) -> Unit = {},
    convertProgress: Int = 0,
    convertMessage: String = "",
    audioImportCount: Int = 0,
    sourceAudioPath: String = "",
    sourceMediaStoreId: Long = -1L,
    sourceTitle: String = "",
    sourceArtist: String = "",
    importedLyricsContent: String = "",
    importedLyricsFormat: Int = 0,
    convertedAudioPath: String = "",
    playbackCompleted: Boolean = false,
    onPlaybackCompletedHandled: () -> Unit = {},
    showVerbatimLyricsOverwriteDialog: Boolean = false,
    onShowVerbatimLyricsOverwriteDialogChange: (Boolean) -> Unit = {},
    onImportVerbatimLyrics: (String) -> Unit = {},
    onImportTtmlLyrics: (String) -> Unit = {},
    onOpenVerbatimLyrics: (String) -> Unit = {},
    verbatimLyricsLines: List<LyricLine> = emptyList(),
    onVerbatimLyricsLinesChange: (List<LyricLine>) -> Unit = {},
    pendingVerbatimLyricsContent: String = "",
    onPendingVerbatimLyricsContentChange: (String) -> Unit = {},
    pendingLyricsCreators: List<String> = emptyList(),
    onPendingLyricsCreatorsChange: (List<String>) -> Unit = {},
    onStartManualConversion: () -> Unit = {},
    initialPlaybackPositionMs: Long = 0L,
    launchSelectionPositionMs: Long = -1L,
    initialLyricLines: List<LyricLine> = emptyList(),
    onLyricLinesSnapshot: (List<LyricLine>) -> Unit = {},
    initialSelectedLineIndex: Int = 0,
    initialSelectedWordIndex: Int = 0,
    onSelectionSnapshot: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var lyrics by remember { mutableStateOf(listOf<String>()) }
    var lyricInput by remember { mutableStateOf("") }
    var useSpaceSplit by remember { mutableStateOf(false) }
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(initialPlaybackPositionMs) }
    var selectedWordIndex by rememberSaveable { mutableStateOf(initialSelectedWordIndex) }
    var selectedLineIndex by rememberSaveable { mutableStateOf(initialSelectedLineIndex) }
    var audioDuration by remember { mutableStateOf(0L) }
    var showLyricDialog by remember { mutableStateOf(false) }
    var savedLyricContent by remember { mutableStateOf("") }
    var showImportExampleDialog by remember { mutableStateOf(false) }
    var showLineLyricDialog by remember { mutableStateOf(false) }
    var savedLineLyricContent by remember { mutableStateOf("") }
    var showLineEndTime by remember { mutableStateOf(false) }
    var showNoLyricsDialog by remember { mutableStateOf(false) }
    var showNoEmptyLinesDialog by remember { mutableStateOf(false) }
    var showImportLyricMenu by remember { mutableStateOf(false) }
    var hasAudio by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    LaunchedEffect(playbackCompleted) {
        if (playbackCompleted) {
            isPlaying = false
            onPlaybackCompletedHandled()
        }
    }
    
    val prefs = remember { context.getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE) }
    val seekTimeSeconds by remember { mutableFloatStateOf(prefs.getFloat("seekTimeSeconds", 2f)) }
    val seekTimeMs = (seekTimeSeconds * 1000).toLong()
    val vibrationIntensity by remember { mutableStateOf(prefs.getString("vibrationIntensity", "weak")) }
    
    // 震动辅助函数
    fun performVibration() {
        when (vibrationIntensity) {
            "off" -> {}
            "weak" -> {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, 64))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(15)
                }
            }
            "medium" -> {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, 128))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(30)
                }
            }
            "strong" -> {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }
    }
    
    var showTitlePathDialog by remember { mutableStateOf(false) }
    var displayTitle by remember { mutableStateOf("歌词打轴") }
    var isFromMusicLibrary by remember { mutableStateOf(false) }
    var showEmbedLrcWordDialog by remember { mutableStateOf(false) }
    var showEmbedLrcLineDialog by remember { mutableStateOf(false) }
    var showEmbedTtmlDialog by remember { mutableStateOf(false) }
    var showSaveTtmlFileDialog by remember { mutableStateOf(false) }
    var showEmbedResultDialog by remember { mutableStateOf(false) }
    var embedResultMessage by remember { mutableStateOf("") }
    var embedResultSuccess by remember { mutableStateOf(false) }
    var pendingRecoverableEmbedIntentSender by remember { mutableStateOf<IntentSender?>(null) }
    var pendingRecoverableEmbedLyricsContent by remember { mutableStateOf<String?>(null) }
    var showFormatTimelineConfirmDialog by remember { mutableStateOf(false) }
    var showFormatTimelineResultDialog by remember { mutableStateOf(false) }
    var formatTimelineResultMessage by remember { mutableStateOf("") }
    var formatTimelineResultSuccess by remember { mutableStateOf(false) }
    var needStoragePermission by remember { mutableStateOf(false) }
    var showNoAudioDialog by remember { mutableStateOf(false) }
    var showEnhancedLrcSaveDialog by remember { mutableStateOf(false) }
    var showDuetInEnhancedLrc by remember { mutableStateOf(prefs.getBoolean("showDuetInEnhancedLrc", true)) }
    var savedEnhancedLrcContent by remember { mutableStateOf("") }
    var showEmbedEnhancedLrcDialog by remember { mutableStateOf(false) }
    var showTimeInputDialog by remember { mutableStateOf(false) }
    var inputTimeMinutes by remember { mutableStateOf("") }
    var inputTimeSeconds by remember { mutableStateOf("") }
    var inputTimeMilliseconds by remember { mutableStateOf("") }
    var wasPlayingBeforeTimeInput by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var isFollowMode by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // 处理从音乐库导入
    LaunchedEffect(sourceAudioPath, importedLyricsContent) {
        if (sourceAudioPath.isNotEmpty() && sourceTitle.isNotEmpty()) {
            displayTitle = sourceTitle
            isFromMusicLibrary = true
        }
    }
    
    // 更新音频状态 - 定期检查音频时长
    LaunchedEffect(Unit) {
        while (true) {
            val duration = getAudioDuration()
            if (duration > 0L && audioDuration != duration) {
                audioDuration = duration
                hasAudio = true
            }
            delay(500) // 每500ms检查一次
        }
    }
    
    var hasHandledFirstAudioImport by remember { mutableStateOf(false) }
    // 音频重新导入时重置播放状态
    LaunchedEffect(audioImportCount) {
        if (audioImportCount > 0) {
            isPlaying = false
            val shouldKeepRestoredProgress = !hasHandledFirstAudioImport && initialPlaybackPositionMs > 0L
            if (!shouldKeepRestoredProgress) {
                currentTime = 0L
            }
            hasHandledFirstAudioImport = true
        }
    }
    
    // 编辑功能状态
    var menuLineIndex by remember { mutableStateOf(-1) }
    var menuUnitIndex by remember { mutableStateOf(-1) }
    var translationMenuLineIndex by remember { mutableStateOf(-1) }
    // 底部编辑控制区域显示状态
    var showEditControlPanel by remember { mutableStateOf(false) }
    
    // 编辑对话框状态
    var showEditUnitDialog by remember { mutableStateOf(false) }
    var showAddLyricDialog by remember { mutableStateOf(false) }
    var showAddLineDialog by remember { mutableStateOf(false) }
    var showMergeLinesDialog by remember { mutableStateOf(false) }
    var showSplitLyricDialog by remember { mutableStateOf(false) }
    var showMergeLyricDialog by remember { mutableStateOf(false) }
    var showDeleteUnitConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteLineConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteMultipleLinesDialog by remember { mutableStateOf(false) }
    var deleteMultipleLinesSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var originalDeleteMultipleLinesSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showAddTranslationDialog by remember { mutableStateOf(false) }
    var showMoveLineDialog by remember { mutableStateOf(false) }
    var showSplitToMultipleLinesDialog by remember { mutableStateOf(false) }
    var showBatchSegmentDialog by remember { mutableStateOf(false) }
    var batchSegmentSelectedLines by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showMergeUnitsDialog by remember { mutableStateOf(false) }
    var mergeUnitsSelectedLines by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var mergeUnitsThreshold by remember { mutableStateOf(50L) }
    var showMergeUnitsThresholdMenu by remember { mutableStateOf(false) }
    var showTimestampShiftDialog by remember { mutableStateOf(false) }
    var timestampShiftSelectedLines by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var timestampShiftValue by remember { mutableStateOf("100") }
    var showSetTimestampDialog by remember { mutableStateOf(false) }
    var showEditTimeDialog by remember { mutableStateOf(false) }
    var editingStartTime by remember { mutableStateOf(true) }
    var tempStartTime by remember { mutableStateOf("") }
    var tempEndTime by remember { mutableStateOf("") }
    var editingTimeValue by remember { mutableStateOf("") }
    var tempEditingTimeValue by remember { mutableStateOf("") }
    var showEditTimeCancelConfirm by remember { mutableStateOf(false) }
    var originalTempStartTime by remember { mutableStateOf("") }
    var originalTempEndTime by remember { mutableStateOf("") }
    var showSetTimestampCancelConfirm by remember { mutableStateOf(false) }
    var pendingSetTimestampDismiss by remember { mutableStateOf(false) }
    var showTimeEditorInSheet by remember { mutableStateOf(false) }
    var showConvertToSimplifiedDialog by remember { mutableStateOf(false) }
    var convertToSimplifiedSelectedLines by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // 设置时间戳对话框的切换变量
    var showSwitchUnitConfirm by remember { mutableStateOf(false) }
    var targetUnitInfo: Pair<Int, Int>? by remember { mutableStateOf(null) }
    // 编辑歌词对话框的切换变量（新增，避免冲突）
    var showEditUnitSwitchConfirm by remember { mutableStateOf(false) }
    var editUnitSwitchDelta: Int by remember { mutableStateOf(0) }
    var showDeleteEmptyLinesDialog by remember { mutableStateOf(false) }
    var showImportTransliterationDialog by remember { mutableStateOf(false) }
    var transliterationInput by remember { mutableStateOf("") }
    var showTransliterationResultDialog by remember { mutableStateOf(false) }
    var transliterationResultSuccess by remember { mutableStateOf(false) }
    var transliterationResultMessage by remember { mutableStateOf("") }
    var showDeleteAllTransliterationConfirmDialog by remember { mutableStateOf(false) }
    var showRecognizeRomajiRubyDialog by remember { mutableStateOf(false) }
    var romajiTranslationLineIndex by remember { mutableIntStateOf(0) }
    var romajiTranslationLineOptionCount by remember { mutableIntStateOf(1) }
    
    // 编辑输入状态
    var editUnitText by remember { mutableStateOf("") }
    var originalEditUnitText by remember { mutableStateOf("") }
    var editUnitTransliteration by remember { mutableStateOf("") }
    var originalEditUnitTransliteration by remember { mutableStateOf("") }
    var editUnitCharTransliterations by remember { mutableStateOf<MutableMap<Int, String>>(mutableMapOf()) }
    var originalEditUnitCharTransliterations by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var previousText by remember { mutableStateOf("") }
    var showSavedIndicator by remember { mutableStateOf(false) }
    var showEditUnitCancelConfirm by remember { mutableStateOf(false) }
    var addLyricText by remember { mutableStateOf("") }
    var originalAddLyricText by remember { mutableStateOf("") }
    var showAddLyricCancelConfirm by remember { mutableStateOf(false) }
    var addLyricPosition by remember { mutableStateOf(0) } // 0=之前, 1=之后
    var addLineText by remember { mutableStateOf("") }
    var originalAddLineText by remember { mutableStateOf("") }
    var showAddLineCancelConfirm by remember { mutableStateOf(false) }
    var addLinePosition by remember { mutableStateOf(0) } // 0=之前, 1=之后
    var splitLyricText by remember { mutableStateOf("") }
    var originalSplitLyricText by remember { mutableStateOf("") }
    var showSplitLyricCancelConfirm by remember { mutableStateOf(false) }
    var pendingSplitLyricDismiss by remember { mutableStateOf(false) }
    var mergeSelectedUnits by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var mergeLyricPreview by remember { mutableStateOf<List<LyricTimeUnit>>(emptyList()) }
    var mergeLyricHistory by remember { mutableStateOf<List<List<LyricTimeUnit>>>(emptyList()) }
    var addTranslationText by remember { mutableStateOf("") }
    var originalAddTranslationText by remember { mutableStateOf("") }
    var showAddTranslationCancelConfirm by remember { mutableStateOf(false) }
    var mergeLinesSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var mergeLinesAddSpace by remember { mutableStateOf(false) }
    var mergeLinesPreview by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var mergeLinesPreviewSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var mergeLinesHistory by remember { mutableStateOf<List<List<LyricLine>>>(emptyList()) }
    var showMergeLinesCancelConfirm by remember { mutableStateOf(false) }
    var originalMergeLinesSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var originalMergeLinesPreview by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var pendingMergeLinesDismiss by remember { mutableStateOf(false) }
    var showMergeLyricCancelConfirm by remember { mutableStateOf(false) }
    var originalMergeSelectedUnits by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var originalMergeLyricPreview by remember { mutableStateOf<List<LyricTimeUnit>>(emptyList()) }
    var pendingMergeLyricDismiss by remember { mutableStateOf(false) }
    var moveLineTargetIndex by remember { mutableStateOf(-1) }
    var originalMoveLineTargetIndex by remember { mutableStateOf(-1) }
    var showMoveLineCancelConfirm by remember { mutableStateOf(false) }
    var moveLinePosition by remember { mutableStateOf(0) } // 0=上方, 1=下方
    var originalMoveLinePosition by remember { mutableStateOf(0) }
    var splitToMultipleLinesText by remember { mutableStateOf("") }
    var originalSplitToMultipleLinesText by remember { mutableStateOf("") }
    var showSplitToMultipleLinesCancelConfirm by remember { mutableStateOf(false) }
    var pendingAddTranslationDismiss by remember { mutableStateOf(false) }
    var pendingMoveLineDismiss by remember { mutableStateOf(false) }
    var pendingSplitToMultipleLinesDismiss by remember { mutableStateOf(false) }
    
    // LazyColumn滚动状态
    val lazyListState = rememberLazyListState()
    
    // 测量时间戳文本宽度，防止布局抖动
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val timestampMinWidth = remember(textMeasurer, density) {
        val baselineText = "00:00.000"
        val textSize = 12.sp
        val style = androidx.compose.ui.text.TextStyle(fontSize = textSize)
        val measuredWidth = textMeasurer.measure(baselineText, style).size.width
        with(density) { measuredWidth.toDp() }
    }
    
    val defaultLyricLines = remember(lyrics) {
        lyrics.map { line ->
            val words = line.split(" ")
            val result = mutableListOf<LyricTimeUnit>()
            var pendingSpaces = ""
            
            words.forEach { word ->
                if (word.isEmpty()) {
                    pendingSpaces += " "
                } else {
                    result.add(LyricTimeUnit(pendingSpaces + word, "00:00.000", "00:00.000"))
                    pendingSpaces = ""
                }
            }
            
            LyricLine(result, "")
        }
    }
    var lyricLines: List<LyricLine> by remember {
        mutableStateOf(
            if (initialLyricLines.isNotEmpty()) initialLyricLines else defaultLyricLines
        )
    }
    var hasAppliedLaunchSelection by rememberSaveable { mutableStateOf(false) }

    fun resolveLineTimeRangeMs(line: LyricLine): Pair<Long, Long>? {
        if (line.timeUnits.isEmpty()) return null
        val starts = line.timeUnits.mapNotNull { unit ->
            parseTimeToMs(unit.startTime).takeIf { it > 0L }
        }
        val ends = line.timeUnits.mapNotNull { unit ->
            parseTimeToMs(unit.endTime).takeIf { it > 0L }
        }
        val startMs = starts.minOrNull() ?: ends.minOrNull() ?: return null
        val endMs = ends.maxOrNull() ?: starts.maxOrNull() ?: startMs
        return startMs to maxOf(endMs, startMs)
    }

    fun findLineIndexForLaunchPosition(lines: List<LyricLine>, targetMs: Long): Int {
        var fallbackIndex = -1
        lines.forEachIndexed { index, line ->
            val range = resolveLineTimeRangeMs(line) ?: return@forEachIndexed
            val (startMs, endMs) = range
            if (targetMs in startMs..endMs) {
                return index
            }
            if (targetMs >= startMs) {
                fallbackIndex = index
            }
        }
        return fallbackIndex.takeIf { it >= 0 } ?: 0
    }
    
    // 创作者列表
        var creators: List<String> by remember { mutableStateOf(emptyList()) }
    
    // 更新歌词状态 - 基于 lyricLines 判断
    SideEffect {
        if (lyricLines.isNotEmpty() != hasLyrics) {
            onHasLyricsChange(lyricLines.isNotEmpty())
        }
    }
    LaunchedEffect(launchSelectionPositionMs, lyricLines, hasAppliedLaunchSelection) {
        if (hasAppliedLaunchSelection) return@LaunchedEffect
        val targetMs = launchSelectionPositionMs.takeIf { it >= 0L } ?: return@LaunchedEffect
        if (lyricLines.isEmpty()) return@LaunchedEffect
        val targetLineIndex = findLineIndexForLaunchPosition(lyricLines, targetMs)
        val safeTargetLineIndex = targetLineIndex.coerceIn(0, lyricLines.size - 1)
        selectedLineIndex = safeTargetLineIndex
        selectedWordIndex = 0
        currentTime = targetMs
        runCatching {
            lazyListState.animateScrollToItem(
                index = safeTargetLineIndex,
                scrollOffset = 0
            )
        }
        hasAppliedLaunchSelection = true
    }
    LaunchedEffect(lyricLines) {
        onLyricLinesSnapshot(lyricLines)
    }
    LaunchedEffect(lyricLines, selectedLineIndex, selectedWordIndex) {
        if (lyricLines.isEmpty()) {
            onSelectionSnapshot(0, 0)
            return@LaunchedEffect
        }
        val safeLineIndex = selectedLineIndex.coerceIn(0, lyricLines.size - 1)
        val safeWordIndex = lyricLines[safeLineIndex].timeUnits.let { units ->
            if (units.isEmpty()) 0 else selectedWordIndex.coerceIn(0, units.size - 1)
        }
        if (safeLineIndex != selectedLineIndex) {
            selectedLineIndex = safeLineIndex
        }
        if (safeWordIndex != selectedWordIndex) {
            selectedWordIndex = safeWordIndex
        }
        onSelectionSnapshot(safeLineIndex, safeWordIndex)
    }
    
    val undoRedoManager = remember { UndoRedoManager(maxSize = 20) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    
    fun updateUndoRedoState() {
        canUndo = undoRedoManager.canUndo()
        canRedo = undoRedoManager.canRedo()
    }
    
    fun applyUndoAction(action: UndoAction, currentLines: MutableList<LyricLine>) {
        when (action.actionType) {
            UndoActionType.TIME_CHANGE, UndoActionType.TEXT_CHANGE -> {
                if (action.lineIndex < currentLines.size && action.unitIndex < currentLines[action.lineIndex].timeUnits.size) {
                    val oldUnit = action.oldValue as? LyricTimeUnit
                    if (oldUnit != null) {
                        val line = currentLines[action.lineIndex]
                        val newTimeUnits = line.timeUnits.toMutableList()
                        newTimeUnits[action.unitIndex] = oldUnit
                        currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                    }
                }
            }
            UndoActionType.TRANSLATION_CHANGE -> {
                if (action.lineIndex < currentLines.size) {
                    val oldTranslation = action.oldValue as? String ?: ""
                    currentLines[action.lineIndex] = currentLines[action.lineIndex].copy(translation = oldTranslation)
                }
            }
            UndoActionType.UNIT_DELETE -> {
                val deletedUnit = action.oldValue as? LyricTimeUnit
                if (deletedUnit != null && action.lineIndex < currentLines.size) {
                    val line = currentLines[action.lineIndex]
                    val newTimeUnits = line.timeUnits.toMutableList()
                    newTimeUnits.add(action.unitIndex, deletedUnit)
                    currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                }
            }
            UndoActionType.UNIT_ADD -> {
                if (action.lineIndex < currentLines.size && action.unitIndex < currentLines[action.lineIndex].timeUnits.size) {
                    val line = currentLines[action.lineIndex]
                    val newTimeUnits = line.timeUnits.toMutableList()
                    newTimeUnits.removeAt(action.unitIndex)
                    currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                }
            }
            UndoActionType.LINE_DELETE -> {
                val deletedLine = action.oldValue as? LyricLine
                if (deletedLine != null) {
                    currentLines.add(action.lineIndex, deletedLine)
                }
            }
            UndoActionType.LINE_ADD -> {
                if (action.lineIndex < currentLines.size) {
                    currentLines.removeAt(action.lineIndex)
                }
            }
            UndoActionType.BATCH_DELETE_LINES -> {
                val deletedLines = action.oldValue as? List<*>
                if (deletedLines != null) {
                    val sortedIndices = (action.newValue as? List<Int>)?.sortedDescending() ?: emptyList()
                    sortedIndices.forEach { idx ->
                        if (idx < currentLines.size) {
                            currentLines.removeAt(idx)
                        }
                    }
                }
            }
            UndoActionType.BATCH_SEGMENT, UndoActionType.BATCH_MERGE_UNITS, UndoActionType.UNIT_SPLIT -> {
                if (action.lineIndex < currentLines.size) {
                    val oldTimeUnits = action.oldValue as? List<*>
                    if (oldTimeUnits != null) {
                        val line = currentLines[action.lineIndex]
                        currentLines[action.lineIndex] = line.copy(timeUnits = oldTimeUnits.filterIsInstance<LyricTimeUnit>())
                    }
                }
            }
            UndoActionType.BATCH_CONVERT_TO_SIMPLIFIED -> {
                if (action.unitIndex == -1) {
                    if (action.lineIndex < currentLines.size) {
                        val oldTranslation = action.oldValue as? String ?: ""
                        currentLines[action.lineIndex] = currentLines[action.lineIndex].copy(translation = oldTranslation)
                    }
                } else {
                    if (action.lineIndex < currentLines.size && action.unitIndex < currentLines[action.lineIndex].timeUnits.size) {
                        val oldUnit = action.oldValue as? LyricTimeUnit
                        if (oldUnit != null) {
                            val line = currentLines[action.lineIndex]
                            val newTimeUnits = line.timeUnits.toMutableList()
                            newTimeUnits[action.unitIndex] = oldUnit
                            currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                        }
                    }
                }
            }
            UndoActionType.LINE_MERGE -> {
                val oldLines = action.oldValue as? List<*>
                if (oldLines != null) {
                    currentLines.clear()
                    currentLines.addAll(oldLines.filterIsInstance<LyricLine>())
                }
            }
            UndoActionType.BATCH_FORMAT_TIMELINE -> {
                val oldLines = action.oldValue as? List<*>
                if (oldLines != null) {
                    currentLines.clear()
                    currentLines.addAll(oldLines.filterIsInstance<LyricLine>())
                }
            }
            UndoActionType.MULTI_CHANGE -> {
                val oldLines = action.oldValue as? List<*>
                if (oldLines != null) {
                    currentLines.clear()
                    currentLines.addAll(oldLines.filterIsInstance<LyricLine>())
                }
            }
            else -> {}
        }
    }
    
    fun applyRedoAction(action: UndoAction, currentLines: MutableList<LyricLine>) {
        when (action.actionType) {
            UndoActionType.TIME_CHANGE, UndoActionType.TEXT_CHANGE -> {
                if (action.lineIndex < currentLines.size && action.unitIndex < currentLines[action.lineIndex].timeUnits.size) {
                    val newUnit = action.newValue as? LyricTimeUnit
                    if (newUnit != null) {
                        val line = currentLines[action.lineIndex]
                        val newTimeUnits = line.timeUnits.toMutableList()
                        newTimeUnits[action.unitIndex] = newUnit
                        currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                    }
                }
            }
            UndoActionType.TRANSLATION_CHANGE -> {
                if (action.lineIndex < currentLines.size) {
                    val newTranslation = action.newValue as? String ?: ""
                    currentLines[action.lineIndex] = currentLines[action.lineIndex].copy(translation = newTranslation)
                }
            }
            UndoActionType.UNIT_DELETE -> {
                if (action.lineIndex < currentLines.size && action.unitIndex < currentLines[action.lineIndex].timeUnits.size) {
                    val line = currentLines[action.lineIndex]
                    val newTimeUnits = line.timeUnits.toMutableList()
                    newTimeUnits.removeAt(action.unitIndex)
                    currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                }
            }
            UndoActionType.UNIT_ADD -> {
                val addedUnit = action.newValue as? LyricTimeUnit
                if (addedUnit != null && action.lineIndex < currentLines.size) {
                    val line = currentLines[action.lineIndex]
                    val newTimeUnits = line.timeUnits.toMutableList()
                    newTimeUnits.add(action.unitIndex, addedUnit)
                    currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                }
            }
            UndoActionType.LINE_DELETE -> {
                if (action.lineIndex < currentLines.size) {
                    currentLines.removeAt(action.lineIndex)
                }
            }
            UndoActionType.LINE_ADD -> {
                val addedLine = action.newValue as? LyricLine
                if (addedLine != null) {
                    currentLines.add(action.lineIndex, addedLine)
                }
            }
            UndoActionType.BATCH_DELETE_LINES -> {
                val deletedIndices = action.newValue as? List<Int>
                if (deletedIndices != null) {
                    val sortedIndices = deletedIndices.sortedDescending()
                    sortedIndices.forEach { idx ->
                        if (idx < currentLines.size) {
                            currentLines.removeAt(idx)
                        }
                    }
                }
            }
            UndoActionType.BATCH_SEGMENT, UndoActionType.BATCH_MERGE_UNITS, UndoActionType.UNIT_SPLIT -> {
                if (action.lineIndex < currentLines.size) {
                    val newTimeUnits = action.newValue as? List<*>
                    if (newTimeUnits != null) {
                        val line = currentLines[action.lineIndex]
                        currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits.filterIsInstance<LyricTimeUnit>())
                    }
                }
            }
            UndoActionType.BATCH_CONVERT_TO_SIMPLIFIED -> {
                if (action.unitIndex == -1) {
                    if (action.lineIndex < currentLines.size) {
                        val newTranslation = action.newValue as? String ?: ""
                        currentLines[action.lineIndex] = currentLines[action.lineIndex].copy(translation = newTranslation)
                    }
                } else {
                    if (action.lineIndex < currentLines.size && action.unitIndex < currentLines[action.lineIndex].timeUnits.size) {
                        val newUnit = action.newValue as? LyricTimeUnit
                        if (newUnit != null) {
                            val line = currentLines[action.lineIndex]
                            val newTimeUnits = line.timeUnits.toMutableList()
                            newTimeUnits[action.unitIndex] = newUnit
                            currentLines[action.lineIndex] = line.copy(timeUnits = newTimeUnits)
                        }
                    }
                }
            }
            UndoActionType.LINE_MERGE -> {
                val newLines = action.newValue as? List<*>
                if (newLines != null) {
                    currentLines.clear()
                    currentLines.addAll(newLines.filterIsInstance<LyricLine>())
                }
            }
            UndoActionType.BATCH_FORMAT_TIMELINE -> {
                val newLines = action.newValue as? List<*>
                if (newLines != null) {
                    currentLines.clear()
                    currentLines.addAll(newLines.filterIsInstance<LyricLine>())
                }
            }
            UndoActionType.MULTI_CHANGE -> {
                val newLines = action.newValue as? List<*>
                if (newLines != null) {
                    currentLines.clear()
                    currentLines.addAll(newLines.filterIsInstance<LyricLine>())
                }
            }
            else -> {}
        }
    }
    
    fun performUndo(): List<LyricLine> {
        val action = undoRedoManager.undo()
        var result = lyricLines
        if (action != null) {
            val currentLines = lyricLines.toMutableList()
            when (action) {
                is UndoAction -> applyUndoAction(action, currentLines)
                is BatchUndoAction -> {
                    action.actions.reversed().forEach { subAction ->
                        applyUndoAction(subAction, currentLines)
                    }
                }
            }
            result = currentLines
            updateUndoRedoState()
        }
        return result
    }
    
    fun performRedo(): List<LyricLine> {
        val action = undoRedoManager.redo()
        var result = lyricLines
        if (action != null) {
            val currentLines = lyricLines.toMutableList()
            when (action) {
                is UndoAction -> applyRedoAction(action, currentLines)
                is BatchUndoAction -> {
                    action.actions.forEach { subAction ->
                        applyRedoAction(subAction, currentLines)
                    }
                }
            }
            result = currentLines
            updateUndoRedoState()
        }
        return result
    }
    
    // 辅助函数：检测是否存在空行
    fun hasEmptyLines(lines: List<LyricLine>): Boolean {
        return LyricBatchEditUtils.hasEmptyLines(lines)
    }
    
    // 辅助函数：删除所有空行
    fun removeEmptyLines(lines: List<LyricLine>): List<LyricLine> {
        return LyricBatchEditUtils.removeEmptyLines(lines)
    }
    
    // 处理导入的歌词内容
    LaunchedEffect(importedLyricsContent) {
        if (importedLyricsContent.isNotEmpty() && lyricLines.isEmpty()) {
            var parsedLinesResult: List<LyricLine> = emptyList()
            when (importedLyricsFormat) {
                0 -> {
                    val lines = importedLyricsContent.split("\n").filter { it.isNotBlank() }
                    parsedLinesResult = lines.map { line ->
                        LyricLine(
                            timeUnits = listOf(LyricTimeUnit(text = line, startTime = "00:00.000", endTime = "00:00.000")),
                            translation = ""
                        )
                    }
                }
                1 -> {
                    val result = LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, importedLyricsContent)
                    parsedLinesResult = result.lyricLines
                }
                2 -> {
                    val result = LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, importedLyricsContent)
                    parsedLinesResult = result.lyricLines
                }
                3 -> {
                    val result = LyricParsingUtils.parseByType(LyricParseType.TTML, importedLyricsContent)
                    if (result.lyricLines.isNotEmpty()) {
                        parsedLinesResult = result.lyricLines
                    }
                    // 解析创作者信息
                    Log.d("LyricTiming", "开始解析创作者信息，importedLyricsContent长度=${importedLyricsContent.length}")
                    val parsedSongwriters = LyricParsingUtils.parseSongwritersFromTtml(importedLyricsContent)
                    Log.d("LyricTiming", "解析到的创作者数量: ${parsedSongwriters.size}, 内容: $parsedSongwriters")
                    if (parsedSongwriters.isNotEmpty()) {
                        creators = parsedSongwriters
                        Log.d("LyricTiming", "已更新creators变量: $creators")
                    }
                }
            }
            
            lyricLines = parsedLinesResult
            onHasLyricsChange(true)
            
        }
    }
    
    // 处理从逐字歌词页面导入的歌词
    LaunchedEffect(verbatimLyricsLines) {
        if (verbatimLyricsLines.isNotEmpty()) {
            lyricLines = verbatimLyricsLines
            selectedLineIndex = 0
            selectedWordIndex = 0
            onVerbatimLyricsLinesChange(emptyList())
            
            // 如果有导入的创作者信息，使用它
            if (pendingLyricsCreators.isNotEmpty()) {
                creators = pendingLyricsCreators
            }
            
        }
    }
    
    // 实时更新播放时间和音频时长
    val coroutineScope = rememberCoroutineScope()
    var updateJob by remember { mutableStateOf<Job?>(null) }
    
    // 自动滚动节流：记录上次滚动时间
    var lastScrollTime by remember { mutableStateOf(0L) }
    
    // 自动滚动到当前编辑的歌词行
    LaunchedEffect(selectedLineIndex, selectedWordIndex) {
        if (selectedLineIndex >= 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScrollTime >= 500) {
                lastScrollTime = currentTime
                delay(100)
                lazyListState.animateScrollToItem(selectedLineIndex)
            }
        }
    }
    
    // 跟随模式：根据音频播放进度自动滚动歌词
    LaunchedEffect(isFollowMode, currentTime, lyricLines.size) {
        if (isFollowMode && lyricLines.isNotEmpty()) {
            var targetLineIndex = -1
            
            for (lineIndex in lyricLines.indices) {
                val line = lyricLines[lineIndex]
                var lineStartMs = Long.MAX_VALUE
                var lineEndMs = 0L
                
                for (unit in line.timeUnits) {
                    val startMs = parseTimeToMs(unit.startTime)
                    val endMs = parseTimeToMs(unit.endTime)
                    if (startMs > 0 || endMs > 0) {
                        if (startMs > 0 && startMs < lineStartMs) {
                            lineStartMs = startMs
                        }
                        if (endMs > lineEndMs) {
                            lineEndMs = endMs
                        }
                    }
                }
                
                if (lineStartMs != Long.MAX_VALUE && lineEndMs > 0) {
                    if (currentTime >= lineStartMs && currentTime < lineEndMs) {
                        targetLineIndex = lineIndex
                        break
                    }
                }
            }
            
            if (targetLineIndex >= 0) {
                lazyListState.animateScrollToItem(targetLineIndex, scrollOffset = 0)
            }
        }
    }
    
    // 预览Activity启动器
    val previewLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val returnPosition = result.data?.getLongExtra(LyricPreviewActivity.EXTRA_RETURN_POSITION, 0L) ?: 0L
            if (returnPosition > 0) {
                // 跳转到返回的进度位置
                onSeekTo(returnPosition)
                currentTime = returnPosition
                
                // 找到对应时间的歌词单元并选中
                var foundLineIndex = 0
                var foundWordIndex = 0
                for (lineIndex in lyricLines.indices) {
                    val line = lyricLines[lineIndex]
                    for (wordIndex in line.timeUnits.indices) {
                        val unit = line.timeUnits[wordIndex]
                        val beginMs = parseTimeToMs(unit.startTime)
                        val endMs = parseTimeToMs(unit.endTime)
                        if (returnPosition in beginMs..endMs) {
                            foundLineIndex = lineIndex
                            foundWordIndex = wordIndex
                            break
                        }
                    }
                }
                selectedLineIndex = foundLineIndex
                selectedWordIndex = foundWordIndex
            }
        }
    }

    val recoverableEmbedPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingRecoverableEmbedIntentSender = null
            val pendingContent = pendingRecoverableEmbedLyricsContent
            pendingRecoverableEmbedLyricsContent = null
            showEmbedResultDialog = false
            if (!pendingContent.isNullOrBlank() && sourceAudioPath.isNotBlank()) {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    val retryResult = LyricSaveEmbedUtils.embedLyrics(
                        context = context,
                        sourceAudioPath = sourceAudioPath,
                        lyricsContent = pendingContent,
                        mediaStoreId = sourceMediaStoreId
                    )
                    withContext(Dispatchers.Main) {
                        embedResultSuccess = retryResult.success
                        if (retryResult.success) {
                            notifyTimingSourceLyricsUpdated(sourceAudioPath)
                        }
                        embedResultMessage = if (retryResult.success) {
                            "歌词已成功嵌入到音频文件"
                        } else {
                            retryResult.errorMessage
                        }
                        needStoragePermission = retryResult.needPermission
                        pendingRecoverableEmbedIntentSender = retryResult.recoverableIntentSender
                        showEmbedResultDialog = true
                    }
                }
            }
        }
    }

    fun launchRecoverableEmbedPermissionIfNeeded() {
        val sender = pendingRecoverableEmbedIntentSender ?: return
        recoverableEmbedPermissionLauncher.launch(
            IntentSenderRequest.Builder(sender).build()
        )
    }

    // 处理歌词导入
    val handleLyricsImported = { importedLyrics: List<String> ->
        lyrics = importedLyrics.map { line ->
            // 按=分割，只按第一个=分割
            val parts = line.split("=", limit = 2)
            parts[0]
        }
        
        lyricLines = importedLyrics.mapIndexed { index, line ->
            val parts = line.split("=", limit = 2)
            val originalLyric = parts[0]
            val translation = if (parts.size > 1) parts[1] else ""
            
            val words = originalLyric.split(" ")
            val result = mutableListOf<LyricTimeUnit>()
            var pendingSpaces = ""
            
            words.forEach { word ->
                if (word.isEmpty()) {
                    pendingSpaces += " "
                } else {
                    result.add(LyricTimeUnit(pendingSpaces + word, "00:00.000", "00:00.000"))
                    pendingSpaces = ""
                }
            }
            
            LyricLine(result, translation)
        }
        
        selectedLineIndex = 0
        selectedWordIndex = 0
    }
    
    // 构建保存的歌词内容
    val buildSavedLyric = { ->
        LyricSaveEmbedUtils.buildWordLrc(lyricLines)
    }

    // 菜单状态
    var menuExpanded by remember { mutableStateOf(false) }
    var showLyricInputDialog by remember { mutableStateOf(false) }
    var showSPLLrcInputDialog by remember { mutableStateOf(false) }
    var splLrcInput by remember { mutableStateOf("") }
    var showElrcInputDialog by remember { mutableStateOf(false) }
    var elrcInput by remember { mutableStateOf("") }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var tempSpeed by remember { mutableStateOf(1f) }
    var showTtmlInputDialog by remember { mutableStateOf(false) }
    var ttmlInput by remember { mutableStateOf("") }
    var showTtmlSaveDialog by remember { mutableStateOf(false) }
    var savedTtmlContent by remember { mutableStateOf("") }
    var showImportTranslationDialog by remember { mutableStateOf(false) }
    var translationInput by remember { mutableStateOf("") }
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    var showSaveFailDialog by remember { mutableStateOf(false) }
    var ttmlSaveErrorMessage by remember { mutableStateOf("歌词文件保存失败，请检查存储权限或重试") }
    var ttmlSaveSuccessMessage by remember { mutableStateOf("歌词文件已成功保存") }
    var pendingTtmlRecoverableIntentSender by remember { mutableStateOf<IntentSender?>(null) }
    var pendingTtmlContentForRetry by remember { mutableStateOf<String?>(null) }

    val recoverableTtmlSavePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val retryContent = pendingTtmlContentForRetry
            pendingTtmlRecoverableIntentSender = null
            pendingTtmlContentForRetry = null
            if (!retryContent.isNullOrBlank() && sourceAudioPath.isNotBlank()) {
                coroutineScope.launch {
                    val retryResult = withContext(Dispatchers.IO) {
                        saveTtmlToFileResult(
                            audioPath = sourceAudioPath,
                            ttmlContent = retryContent,
                            context = context,
                            sourceMediaStoreId = sourceMediaStoreId
                        )
                    }
                    if (retryResult.success) {
                        if (!retryResult.redirectedToFallbackDir) {
                            notifyTimingSourceLyricsUpdated(sourceAudioPath)
                        }
                        ttmlSaveSuccessMessage = if (retryResult.redirectedToFallbackDir) {
                            "原目录写入失败，已改为保存到：${retryResult.savedPath}"
                        } else {
                            "歌词文件已成功保存：${retryResult.savedPath.ifBlank { sourceAudioPath }}"
                        }
                        showSaveSuccessDialog = true
                    } else {
                        ttmlSaveErrorMessage = retryResult.errorMessage.ifBlank { "歌词文件保存失败，请检查存储权限或重试" }
                        needStoragePermission = retryResult.needPermission
                        pendingTtmlRecoverableIntentSender = retryResult.recoverableIntentSender
                        pendingTtmlContentForRetry = retryContent
                        showSaveFailDialog = true
                    }
                }
            }
        }
    }
    var showCopiedDialog by remember { mutableStateOf(false) }
    var showDeleteEmptyLinesSuccessDialog by remember { mutableStateOf(false) }
    var deletedEmptyLinesCount by remember { mutableIntStateOf(0) }
    
    // 导入歌词取消确认状态
    var showCancelLyricInputConfirm by remember { mutableStateOf(false) }
    var showCancelSpllrcInputConfirm by remember { mutableStateOf(false) }
    var showCancelElrcInputConfirm by remember { mutableStateOf(false) }
    var showCancelTtmlInputConfirm by remember { mutableStateOf(false) }
    var pendingLyricInputDismiss by remember { mutableStateOf(false) }
    var pendingSpllrcInputDismiss by remember { mutableStateOf(false) }
    var pendingElrcInputDismiss by remember { mutableStateOf(false) }
    var pendingTtmlInputDismiss by remember { mutableStateOf(false) }

    @Composable
    fun ScreenContent() {
    val noTranslationCannotRecognizeMessage = stringResource(R.string.lyric_timing_no_translation_cannot_recognize)
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // Headbar
        CommonHeadBar(
            title = displayTitle,
            showBack = true,
            showMenu = !isPreviewMode,
            onBackClick = { onBack(lyricLines) },
            onMenuClick = { menuExpanded = true },
            onTitleClick = if (isFromMusicLibrary) {
                { showTitlePathDialog = true }
            } else null,
            menuContent = { menuButtonPosition ->
                if (isPreviewMode) return@CommonHeadBar
                val menuItems = if (isFromMusicLibrary) {
                    listOf(
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_import_lyrics),
                            subItems = listOf(
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_plain_text), onClick = { showLyricInputDialog = true }),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_lrc), onClick = { showSPLLrcInputDialog = true }),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_elrc), onClick = { showElrcInputDialog = true }),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_ttml), onClick = { showTtmlInputDialog = true }),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_import_translation_by_text),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            translationInput = lyricLines.map { it.translation }.joinToString("\n")
                                            showImportTranslationDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_import_transliteration_by_text),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            transliterationInput = ""
                                            showImportTransliterationDialog = true
                                        }
                                    }
                                ),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_fetch_verbatim), onClick = {
                                    val keyword = if (sourceTitle.isNotEmpty() && sourceArtist.isNotEmpty()) "$sourceTitle $sourceArtist" else sourceTitle
                                    onOpenVerbatimLyrics(keyword)
                                })
                            )
                        ),
                        MenuItem(title = stringResource(R.string.lyric_timing_menu_import_audio), onClick = { onImportAudio() }),
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_menu_batch_actions),
                            subItems = listOf(
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_segment),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            batchSegmentSelectedLines = emptySet()
                                            showBatchSegmentDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_merge_units),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            mergeUnitsSelectedLines = emptySet()
                                            showMergeUnitsDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_shift_timestamp),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            timestampShiftSelectedLines = emptySet()
                                            timestampShiftValue = "100"
                                            showTimestampShiftDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_convert_simplified),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            convertToSimplifiedSelectedLines = emptySet()
                                            showConvertToSimplifiedDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_recognize_transliteration),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            val maxTranslationLines = lyricLines.maxOfOrNull {
                                                splitTranslationLines(it.translation).size
                                            } ?: 0
                                            if (maxTranslationLines <= 0) {
                                                transliterationResultSuccess = false
                                                transliterationResultMessage = noTranslationCannotRecognizeMessage
                                                showTransliterationResultDialog = true
                                            } else {
                                                romajiTranslationLineOptionCount = maxTranslationLines
                                                romajiTranslationLineIndex = romajiTranslationLineIndex
                                                    .coerceIn(0, maxTranslationLines - 1)
                                                showRecognizeRomajiRubyDialog = true
                                            }
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_delete_empty_lines),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            val emptyLinesIndices = LyricBatchEditUtils.emptyLineIndices(lyricLines)
                                            if (emptyLinesIndices.isEmpty()) {
                                                showNoEmptyLinesDialog = true
                                            } else {
                                                deleteMultipleLinesSelected = emptyLinesIndices.toSet()
                                                showDeleteMultipleLinesDialog = true
                                            }
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_format_timeline),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showFormatTimelineConfirmDialog = true
                                        }
                                    }
                                )
                            )
                        ),
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_embed_lyrics),
                            subItems = listOf(
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_embed_lrc_word),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedLrcWordDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_embed_lrc_line),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedLrcLineDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_embed_enhanced_lrc),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedEnhancedLrcDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_embed_ttml),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedTtmlDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_save_ttml_to_same_dir),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showSaveTtmlFileDialog = true
                                        }
                                    }
                                )
                            )
                        ),
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_preview_lyrics),
                            onClick = {
                                if (lyricLines.isEmpty()) {
                                    showNoLyricsDialog = true
                                } else if (!hasAudio) {
                                    showNoAudioDialog = true
                                } else {
                                    if (isPlaying) {
                                        onPlayPause(false)
                                        isPlaying = false
                                    }
                                    val currentPos = getCurrentPosition()
                                    val intent = createLyricPreviewIntent(
                                        context = context,
                                        lyricLines = lyricLines,
                                        sourceAudioPath = sourceAudioPath,
                                        sourceMediaStoreId = sourceMediaStoreId,
                                        convertedAudioPath = convertedAudioPath,
                                        displayTitle = displayTitle,
                                        currentPos = currentPos,
                                        pendingLyricsCreators = pendingLyricsCreators
                                    )
                                    previewLauncher.launch(intent)
                                }
                            }
                        )
                    )
                } else {
                    listOf(
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_import_lyrics),
                            subItems = listOf(
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_plain_text), onClick = { showLyricInputDialog = true }),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_lrc), onClick = { showSPLLrcInputDialog = true }),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_elrc), onClick = { showElrcInputDialog = true }),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_import_ttml), onClick = { showTtmlInputDialog = true }),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_import_translation_by_text),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            translationInput = lyricLines.map { it.translation }.joinToString("\n")
                                            showImportTranslationDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_import_transliteration_by_text),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            transliterationInput = ""
                                            showImportTransliterationDialog = true
                                        }
                                    }
                                ),
                                MenuItem(title = stringResource(R.string.lyric_timing_menu_fetch_verbatim), onClick = {
                                    val keyword = if (sourceTitle.isNotEmpty() && sourceArtist.isNotEmpty()) "$sourceTitle $sourceArtist" else sourceTitle
                                    onOpenVerbatimLyrics(keyword)
                                })
                            )
                        ),
                        MenuItem(title = stringResource(R.string.lyric_timing_menu_import_audio), onClick = { onImportAudio() }),
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_menu_batch_actions),
                            subItems = listOf(
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_segment),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            batchSegmentSelectedLines = emptySet()
                                            showBatchSegmentDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_merge_units),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            mergeUnitsSelectedLines = emptySet()
                                            showMergeUnitsDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_shift_timestamp),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            timestampShiftSelectedLines = emptySet()
                                            timestampShiftValue = "100"
                                            showTimestampShiftDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_batch_convert_simplified),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            convertToSimplifiedSelectedLines = emptySet()
                                            showConvertToSimplifiedDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_recognize_transliteration),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            val maxTranslationLines = lyricLines.maxOfOrNull {
                                                splitTranslationLines(it.translation).size
                                            } ?: 0
                                            if (maxTranslationLines <= 0) {
                                                transliterationResultSuccess = false
                                                transliterationResultMessage = noTranslationCannotRecognizeMessage
                                                showTransliterationResultDialog = true
                                            } else {
                                                romajiTranslationLineOptionCount = maxTranslationLines
                                                romajiTranslationLineIndex = romajiTranslationLineIndex
                                                    .coerceIn(0, maxTranslationLines - 1)
                                                showRecognizeRomajiRubyDialog = true
                                            }
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_delete_empty_lines),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            val emptyLinesIndices = LyricBatchEditUtils.emptyLineIndices(lyricLines)
                                            if (emptyLinesIndices.isEmpty()) {
                                                showNoEmptyLinesDialog = true
                                            } else {
                                                deleteMultipleLinesSelected = emptyLinesIndices.toSet()
                                                showDeleteMultipleLinesDialog = true
                                            }
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_format_timeline),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showFormatTimelineConfirmDialog = true
                                        }
                                    }
                                )
                            )
                        ),
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_save_lyrics),
                            subItems = listOf(
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_save_as_lrc_word),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            val savedLyric = buildSavedLyric()
                                            showLyricDialog = true
                                            savedLyricContent = savedLyric
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_save_as_lrc_line),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showLineEndTime = false
                                            showLineLyricDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_save_as_enhanced_lrc),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEnhancedLrcSaveDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = stringResource(R.string.lyric_timing_save_as_ttml),
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            val ttmlContent = LyricSaveEmbedUtils.buildTtml(lyricLines, creators)
                                            savedTtmlContent = ttmlContent
                                            showTtmlSaveDialog = true
                                        }
                                    }
                                )
                            )
                        ),
                        MenuItem(
                            title = stringResource(R.string.lyric_timing_preview_lyrics),
                            onClick = {
                                if (lyricLines.isEmpty()) {
                                    showNoLyricsDialog = true
                                } else if (!hasAudio) {
                                    showNoAudioDialog = true
                                } else {
                                    if (isPlaying) {
                                        onPlayPause(false)
                                        isPlaying = false
                                    }
                                    val currentPos = getCurrentPosition()
                                    val intent = createLyricPreviewIntent(
                                        context = context,
                                        lyricLines = lyricLines,
                                        sourceAudioPath = sourceAudioPath,
                                        sourceMediaStoreId = sourceMediaStoreId,
                                        convertedAudioPath = convertedAudioPath,
                                        displayTitle = displayTitle,
                                        currentPos = currentPos,
                                        pendingLyricsCreators = pendingLyricsCreators
                                    )
                                    previewLauncher.launch(intent)
                                }
                            }
                        )
                    )
                }
                
                CustomDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = menuItems,
                    anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f)
                )
            }
        )
        
        ImportLyricsBottomSheets(
            showLyricInputDialog = showLyricInputDialog,
            onShowLyricInputDialogChange = { showLyricInputDialog = it },
            lyricInput = lyricInput,
            onLyricInputChange = { lyricInput = it },
            useSpaceSplit = useSpaceSplit,
            onUseSpaceSplitChange = { useSpaceSplit = it },
            showImportExampleDialog = showImportExampleDialog,
            onShowImportExampleDialogChange = { showImportExampleDialog = it },
            showCancelLyricInputConfirm = showCancelLyricInputConfirm,
            onShowCancelLyricInputConfirmChange = { showCancelLyricInputConfirm = it },
            pendingLyricInputDismiss = pendingLyricInputDismiss,
            onPendingLyricInputDismissChange = { pendingLyricInputDismiss = it },
            showSPLLrcInputDialog = showSPLLrcInputDialog,
            onShowSPLLrcInputDialogChange = { showSPLLrcInputDialog = it },
            splLrcInput = splLrcInput,
            onSplLrcInputChange = { splLrcInput = it },
            showCancelSpllrcInputConfirm = showCancelSpllrcInputConfirm,
            onShowCancelSpllrcInputConfirmChange = { showCancelSpllrcInputConfirm = it },
            pendingSpllrcInputDismiss = pendingSpllrcInputDismiss,
            onPendingSpllrcInputDismissChange = { pendingSpllrcInputDismiss = it },
            showElrcInputDialog = showElrcInputDialog,
            onShowElrcInputDialogChange = { showElrcInputDialog = it },
            elrcInput = elrcInput,
            onElrcInputChange = { elrcInput = it },
            showCancelElrcInputConfirm = showCancelElrcInputConfirm,
            onShowCancelElrcInputConfirmChange = { showCancelElrcInputConfirm = it },
            pendingElrcInputDismiss = pendingElrcInputDismiss,
            onPendingElrcInputDismissChange = { pendingElrcInputDismiss = it },
            showTtmlInputDialog = showTtmlInputDialog,
            onShowTtmlInputDialogChange = { showTtmlInputDialog = it },
            ttmlInput = ttmlInput,
            onTtmlInputChange = { ttmlInput = it },
            showCancelTtmlInputConfirm = showCancelTtmlInputConfirm,
            onShowCancelTtmlInputConfirmChange = { showCancelTtmlInputConfirm = it },
            pendingTtmlInputDismiss = pendingTtmlInputDismiss,
            onPendingTtmlInputDismissChange = { pendingTtmlInputDismiss = it },
            onApplyLyricInput = { input, useSpace ->
                val lines = input.lines().filter { it.isNotBlank() }
                lyrics = lines.map { line ->
                    val parts = line.split("=", limit = 2)
                    parts[0]
                }
                val parsedLyricLines = lines.map { line ->
                    val parts = line.split("=", limit = 2)
                    val originalLyric = parts[0]
                        .replace('\u00A0', ' ')
                        .replace('\u2009', ' ')
                    val translation = if (parts.size > 1) parts[1] else ""
                    val words = if (useSpace) {
                        val splitResult = originalLyric.split(" ")
                        val result = mutableListOf<String>()
                        var pendingSpaces = ""
                        splitResult.forEach { word ->
                            if (word.isEmpty()) {
                                pendingSpaces += " "
                            } else {
                                result.add(pendingSpaces + word)
                                pendingSpaces = ""
                            }
                        }
                        result
                    } else {
                        listOf(originalLyric)
                    }
                    LyricLine(
                        words.map { LyricTimeUnit(it, "00:00.000", "00:00.000") },
                        translation
                    )
                }
                lyricLines = parsedLyricLines
                selectedLineIndex = 0
                selectedWordIndex = 0
            },
            onApplySplInput = { input ->
                val parseResult = LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, input)
                val parsedLyrics = parseResult.lyrics
                val parsedLines = parseResult.lyricLines
                lyrics = parsedLyrics
                lyricLines = parsedLines
                selectedLineIndex = 0
                selectedWordIndex = 0
            },
            onApplyElrcInput = { input ->
                val parseResult = LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, input)
                val parsedLyrics = parseResult.lyrics
                val parsedLines = parseResult.lyricLines
                if (parsedLines.isNotEmpty()) {
                    lyrics = parsedLyrics
                    lyricLines = parsedLines
                    selectedLineIndex = 0
                    selectedWordIndex = 0
                }
            },
            onApplyTtmlInput = { input ->
                val parseResult = LyricParsingUtils.parseByType(LyricParseType.TTML, input)
                val parsedLines = parseResult.lyricLines
                if (parsedLines.isNotEmpty()) {
                    lyricLines = parsedLines
                    lyrics = parseResult.lyrics
                    selectedLineIndex = 0
                    selectedWordIndex = 0
                    val parsedSongwriters = LyricParsingUtils.parseSongwritersFromTtml(input)
                    if (parsedSongwriters.isNotEmpty()) {
                        creators = parsedSongwriters
                    }
                }
            }
        )
        
        val context = LocalContext.current

        ImportTranslationBottomSheet(
            showSheet = showImportTranslationDialog,
            lyricLines = lyricLines,
            translationInput = translationInput,
            onTranslationInputChange = { translationInput = it },
            onApplyTranslations = { translationLinesList ->
                lyricLines = lyricLines.mapIndexed { index, line ->
                    line.copy(translation = translationLinesList.getOrNull(index) ?: "")
                }
            },
            onDismissSheet = {
                showImportTranslationDialog = false
                translationInput = ""
            }
        )

        ImportTransliterationBottomSheet(
            showSheet = showImportTransliterationDialog,
            transliterationInput = transliterationInput,
            onTransliterationInputChange = { transliterationInput = it },
            lyricLines = lyricLines,
            onLyricLinesChange = { lyricLines = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onResultStateChange = { success, message ->
                transliterationResultSuccess = success
                transliterationResultMessage = message
            },
            onShowResultDialog = { showTransliterationResultDialog = true },
            onImportKanaRomaClick = {
                try {
                    val importedText = context.assets.open("JapRoman.txt")
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    if (importedText.isBlank()) {
                        transliterationResultSuccess = false
                        transliterationResultMessage = "导入失败：JapRoman.txt 内容为空"
                    } else {
                        transliterationInput = importedText
                        transliterationResultSuccess = true
                        transliterationResultMessage = "已从 JapRoman.txt 导入文本"
                    }
                } catch (e: Exception) {
                    transliterationResultSuccess = false
                    transliterationResultMessage = "导入失败：${e.message ?: "无法读取文件"}"
                }
                showTransliterationResultDialog = true
            },
            onDeleteAllClick = { showDeleteAllTransliterationConfirmDialog = true },
            onDismissSheet = { showImportTransliterationDialog = false }
        )

        DeleteMultipleLinesBottomSheet(
            showSheet = showDeleteMultipleLinesDialog,
            lyricLines = lyricLines,
            selectedLineIndices = deleteMultipleLinesSelected,
            onSelectedLineIndicesChange = { deleteMultipleLinesSelected = it },
            originalSelectedLineIndices = originalDeleteMultipleLinesSelected,
            selectedLineIndex = selectedLineIndex,
            onSelectedLineIndexChange = { selectedLineIndex = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showDeleteMultipleLinesDialog = false }
        )

        DeleteAllTransliterationConfirmDialog(
            showDialog = showDeleteAllTransliterationConfirmDialog,
            onDismiss = { showDeleteAllTransliterationConfirmDialog = false },
            onConfirm = {
                showDeleteAllTransliterationConfirmDialog = false
                val oldLines = lyricLines.toList()
                val clearedLines = clearAllLyricLineTransliterations(lyricLines)
                if (oldLines != clearedLines) {
                    lyricLines = clearedLines
                    undoRedoManager.pushAction(
                        UndoAction(
                            actionType = UndoActionType.MULTI_CHANGE,
                            lineIndex = 0,
                            oldValue = oldLines,
                            newValue = clearedLines
                        )
                    )
                    updateUndoRedoState()
                }
            }
        )

        RecognizeRomajiRubyDialog(
            showDialog = showRecognizeRomajiRubyDialog,
            translationLineOptionCount = romajiTranslationLineOptionCount,
            selectedTranslationLineIndex = romajiTranslationLineIndex,
            onSelectedTranslationLineIndexChange = { romajiTranslationLineIndex = it },
            onRomajiToKanaClick = {
                showRecognizeRomajiRubyDialog = false
                val oldLines = lyricLines.toList()
                val result = convertRomajiRubyToKanaForLyrics(
                    context = context,
                    lyricLines = lyricLines
                )
                if (result.errorMessage != null) {
                    transliterationResultSuccess = false
                    transliterationResultMessage = result.errorMessage
                    showTransliterationResultDialog = true
                    return@RecognizeRomajiRubyDialog
                }
                if (oldLines != result.updatedLines) {
                    lyricLines = result.updatedLines
                    undoRedoManager.pushAction(
                        UndoAction(
                            actionType = UndoActionType.MULTI_CHANGE,
                            lineIndex = 0,
                            oldValue = oldLines,
                            newValue = result.updatedLines
                        )
                    )
                    updateUndoRedoState()
                }
                transliterationResultSuccess = result.failureCount == 0
                transliterationResultMessage =
                    "罗马音注音转假名完成：成功 ${result.successCount} 个，失败 ${result.failureCount} 个。"
                showTransliterationResultDialog = true
            },
            onDismiss = { showRecognizeRomajiRubyDialog = false },
            onConfirm = {
                showRecognizeRomajiRubyDialog = false
                val oldLines = lyricLines.toList()
                val result = recognizeRomajiRubyForLyrics(
                    context = context,
                    lyricLines = lyricLines,
                    selectedTranslationLineIndex = romajiTranslationLineIndex
                )
                if (result.errorMessage != null) {
                    transliterationResultSuccess = false
                    transliterationResultMessage = result.errorMessage
                    showTransliterationResultDialog = true
                    return@RecognizeRomajiRubyDialog
                }
                if (oldLines != result.updatedLines) {
                    lyricLines = result.updatedLines
                    undoRedoManager.pushAction(
                        UndoAction(
                            actionType = UndoActionType.MULTI_CHANGE,
                            lineIndex = 0,
                            oldValue = oldLines,
                            newValue = result.updatedLines
                        )
                    )
                    updateUndoRedoState()
                }
                transliterationResultSuccess = result.successCount > 0
                transliterationResultMessage =
                    "注音识别完成（日韩罗马音）：成功 ${result.successCount} 行，失败 ${result.failureCount} 行。请保存为 TTML 格式以保留逐字注音。"
                showTransliterationResultDialog = true
            }
        )
        
        // 注音导入结果对话框
        SimpleAlertDialog(
            showDialog = showTransliterationResultDialog,
            onDismiss = { showTransliterationResultDialog = false },
            title = if (transliterationResultSuccess) "成功" else "提示",
            text = transliterationResultMessage
        )
        
        TtmlSaveDialog(
            showDialog = showTtmlSaveDialog,
            onDismiss = { showTtmlSaveDialog = false },
            ttmlContent = savedTtmlContent,
            onCopied = { showCopiedDialog = true }
        )
        
        EnhancedLrcSaveDialog(
            showDialog = showEnhancedLrcSaveDialog,
            onDismiss = { showEnhancedLrcSaveDialog = false },
            lyricLines = lyricLines,
            showDuet = showDuetInEnhancedLrc,
            onShowDuetChange = {
                showDuetInEnhancedLrc = it
                prefs.edit().putBoolean("showDuetInEnhancedLrc", it).apply()
            },
            onCopied = { showCopiedDialog = true }
        )
        
        EmbedEnhancedLrcDialog(
            showDialog = showEmbedEnhancedLrcDialog,
            onDismiss = { showEmbedEnhancedLrcDialog = false },
            lyricLines = lyricLines,
            showDuet = showDuetInEnhancedLrc,
            onShowDuetChange = {
                showDuetInEnhancedLrc = it
                prefs.edit().putBoolean("showDuetInEnhancedLrc", it).apply()
            },
            onCopied = { showCopiedDialog = true },
            displayTitle = displayTitle,
            sourceAudioPath = sourceAudioPath,
            sourceMediaStoreId = sourceMediaStoreId,
            onEmbedResult = { success, message, needPermission, recoverableSender ->
                if (success) {
                    notifyTimingSourceLyricsUpdated(sourceAudioPath)
                }
                embedResultSuccess = success
                embedResultMessage = message
                needStoragePermission = needPermission
                pendingRecoverableEmbedIntentSender = recoverableSender
                pendingRecoverableEmbedLyricsContent = if (success) null else LyricSaveEmbedUtils.buildEnhancedLrc(lyricLines, showDuetInEnhancedLrc)
                showEmbedResultDialog = true
            }
        )
        
        LineLyricSaveDialog(
            showDialog = showLineLyricDialog,
            onDismiss = { showLineLyricDialog = false },
            lyricLines = lyricLines,
            showLineEndTime = showLineEndTime,
            onShowLineEndTimeChange = { showLineEndTime = it },
            onCopied = { showCopiedDialog = true }
        )
        
        // 未导入歌词提示对话框
        SimpleAlertDialog(
            showDialog = showNoLyricsDialog,
            onDismiss = { showNoLyricsDialog = false },
            title = stringResource(R.string.common_hint),
            text = stringResource(R.string.lyric_timing_please_import_lyrics_first),
        )
        
        // 无空行提示对话框
        SimpleAlertDialog(
            showDialog = showNoEmptyLinesDialog,
            onDismiss = { showNoEmptyLinesDialog = false },
            title = stringResource(R.string.common_hint),
            text = stringResource(R.string.lyric_timing_no_empty_lines),
        )
        
        // 未导入音频提示对话框
        SimpleAlertDialog(
            showDialog = showNoAudioDialog,
            onDismiss = { showNoAudioDialog = false },
            title = stringResource(R.string.common_hint),
            text = stringResource(R.string.lyric_timing_please_import_audio_first),
        )
        
        // 返回确认对话框
        ConfirmDialog(
            showDialog = showConfirmDialog,
            onConfirm = {
                onConfirmDialogChange(false)
                onBack(lyricLines)
            },
            onCancel = { onConfirmDialogChange(false) },
            title = stringResource(R.string.lyric_timing_confirm_back_title),
            text = stringResource(R.string.lyric_timing_confirm_back_message),
        )
        
        // 逐字歌词覆盖确认对话框
        if (showVerbatimLyricsOverwriteDialog) {
            val isTtmlFormat = importedLyricsFormat == 3
            ConfirmDialog(
                showDialog = showVerbatimLyricsOverwriteDialog,
                onConfirm = {
                    onShowVerbatimLyricsOverwriteDialogChange(false)
                    if (isTtmlFormat) {
                        // 直接在这里设置创作者信息，然后导入歌词
                        if (pendingLyricsCreators.isNotEmpty()) {
                            creators = pendingLyricsCreators
                        }
                        onImportTtmlLyrics(pendingVerbatimLyricsContent)
                    } else {
                        onImportVerbatimLyrics(pendingVerbatimLyricsContent)
                    }
                    onPendingVerbatimLyricsContentChange("")
                },
                onCancel = {
                    onShowVerbatimLyricsOverwriteDialogChange(false)
                    onPendingVerbatimLyricsContentChange("")
                },
                title = stringResource(R.string.lyric_timing_confirm_overwrite_title),
                text = stringResource(R.string.lyric_timing_confirm_overwrite_message),
            )
        }
        
        // 标题路径对话框
        SimpleAlertDialog(
            showDialog = showTitlePathDialog,
            onDismiss = { showTitlePathDialog = false },
            title = stringResource(R.string.lyric_timing_file_path_title),
            text = sourceAudioPath
        )
        
        // 嵌入LRC逐字歌词对话框 - ModalBottomSheet
        val embedLrcWordSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showEmbedLrcWordDialog) {
            val lrcContent = remember(lyricLines) {
                LyricSaveEmbedUtils.buildWordLrc(lyricLines)
            }
            val scrollState = rememberScrollState()
            
            androidx.compose.material3.ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetMaxWidth = Dp.Unspecified,
                onDismissRequest = { showEmbedLrcWordDialog = false },
                sheetState = embedLrcWordSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_embed_lrc_word),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.lyric_timing_confirm_embed_to_target, displayTitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = lrcContent,
                            fontSize = 11.sp,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setText(lrcContent)
                                showCopiedDialog = true
                            }
                        ) {
                            Text(stringResource(R.string.common_copy))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showEmbedLrcWordDialog = false
                                if (sourceAudioPath.isEmpty()) {
                                    embedResultSuccess = false
                                    embedResultMessage = context.getString(R.string.lyric_timing_audio_path_empty_embed_failed)
                                    showEmbedResultDialog = true
                                    return@Button
                                }
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                    Log.d("LyricTiming", "Embedding lyrics to: $sourceAudioPath")
                                    val result = LyricSaveEmbedUtils.embedLyrics(
                                        context = context,
                                        sourceAudioPath = sourceAudioPath,
                                        lyricsContent = lrcContent,
                                        mediaStoreId = sourceMediaStoreId
                                    )
                                    Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                    withContext(Dispatchers.Main) {
                                        embedResultSuccess = result.success
                                        if (result.success) {
                                            notifyTimingSourceLyricsUpdated(sourceAudioPath)
                                        }
                                        embedResultMessage = if (result.success) context.getString(R.string.lyric_timing_embed_success) else result.errorMessage
                                        needStoragePermission = result.needPermission
                                        pendingRecoverableEmbedIntentSender = result.recoverableIntentSender
                                        pendingRecoverableEmbedLyricsContent = if (result.success) null else lrcContent
                                        showEmbedResultDialog = true
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.lyric_timing_confirm_embed))
                        }
                    }
                }
            }
        }
        
        // 嵌入LRC逐行歌词对话框 - ModalBottomSheet
        val embedLrcLineSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showEmbedLrcLineDialog) {
            val lineLrcContent = remember(lyricLines, showLineEndTime) {
                LyricSaveEmbedUtils.buildLineLrc(lyricLines, showLineEndTime)
            }
            val scrollState = rememberScrollState()
            
            androidx.compose.material3.ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetMaxWidth = Dp.Unspecified,
                onDismissRequest = { showEmbedLrcLineDialog = false },
                sheetState = embedLrcLineSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_embed_lrc_line),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.lyric_timing_confirm_embed_to_target, displayTitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = lineLrcContent,
                            fontSize = 11.sp,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomCheckbox(
                        checked = showLineEndTime,
                        onCheckedChange = { showLineEndTime = it },
                        label = stringResource(R.string.lyric_timing_show_line_end_timestamp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setText(lineLrcContent)
                                showCopiedDialog = true
                            }
                        ) {
                            Text(stringResource(R.string.common_copy))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showEmbedLrcLineDialog = false
                                if (sourceAudioPath.isEmpty()) {
                                    embedResultSuccess = false
                                    embedResultMessage = context.getString(R.string.lyric_timing_audio_path_empty_embed_failed)
                                    showEmbedResultDialog = true
                                    return@Button
                                }
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                    Log.d("LyricTiming", "Embedding line lyrics to: $sourceAudioPath")
                                    val result = LyricSaveEmbedUtils.embedLyrics(
                                        context = context,
                                        sourceAudioPath = sourceAudioPath,
                                        lyricsContent = lineLrcContent,
                                        mediaStoreId = sourceMediaStoreId
                                    )
                                    Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                    withContext(Dispatchers.Main) {
                                        embedResultSuccess = result.success
                                        if (result.success) {
                                            notifyTimingSourceLyricsUpdated(sourceAudioPath)
                                        }
                                        embedResultMessage = if (result.success) context.getString(R.string.lyric_timing_embed_success) else result.errorMessage
                                        needStoragePermission = result.needPermission
                                        pendingRecoverableEmbedIntentSender = result.recoverableIntentSender
                                        pendingRecoverableEmbedLyricsContent = if (result.success) null else lineLrcContent
                                        showEmbedResultDialog = true
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.lyric_timing_confirm_embed))
                        }
                    }
                }
            }
        }
        
        // 嵌入TTML歌词对话框 - ModalBottomSheet
        val embedTtmlSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showEmbedTtmlDialog) {
            val ttmlContent = remember(lyricLines, creators) {
                LyricSaveEmbedUtils.buildTtml(lyricLines, creators)
            }
            val scrollState = rememberScrollState()
            
            androidx.compose.material3.ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetMaxWidth = Dp.Unspecified,
                onDismissRequest = { showEmbedTtmlDialog = false },
                sheetState = embedTtmlSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_embed_ttml),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.lyric_timing_confirm_embed_to_target, displayTitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = ttmlContent,
                            fontSize = 10.sp,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setText(ttmlContent)
                                showCopiedDialog = true
                            }
                        ) {
                            Text(stringResource(R.string.common_copy))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showEmbedTtmlDialog = false
                                if (sourceAudioPath.isEmpty()) {
                                    embedResultSuccess = false
                                    embedResultMessage = context.getString(R.string.lyric_timing_audio_path_empty_embed_failed)
                                    showEmbedResultDialog = true
                                    return@Button
                                }
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                    Log.d("LyricTiming", "Embedding TTML lyrics to: $sourceAudioPath")
                                    val result = LyricSaveEmbedUtils.embedLyrics(
                                        context = context,
                                        sourceAudioPath = sourceAudioPath,
                                        lyricsContent = ttmlContent,
                                        mediaStoreId = sourceMediaStoreId
                                    )
                                    Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                    withContext(Dispatchers.Main) {
                                        embedResultSuccess = result.success
                                        if (result.success) {
                                            notifyTimingSourceLyricsUpdated(sourceAudioPath)
                                        }
                                        embedResultMessage = if (result.success) context.getString(R.string.lyric_timing_embed_success) else result.errorMessage
                                        needStoragePermission = result.needPermission
                                        pendingRecoverableEmbedIntentSender = result.recoverableIntentSender
                                        pendingRecoverableEmbedLyricsContent = if (result.success) null else ttmlContent
                                        showEmbedResultDialog = true
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.lyric_timing_confirm_embed))
                        }
                    }
                }
            }
        }
        
        // 保存TTML到同目录对话框 - ModalBottomSheet
        val saveTtmlFileSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showSaveTtmlFileDialog) {
            val ttmlContent = remember(lyricLines, creators) {
                LyricSaveEmbedUtils.buildTtml(lyricLines, creators)
            }
            val scrollState = rememberScrollState()
            val audioFile = java.io.File(sourceAudioPath)
            val ttmlFileName = audioFile.nameWithoutExtension + ".ttml"
            
            androidx.compose.material3.ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetMaxWidth = Dp.Unspecified,
                onDismissRequest = { showSaveTtmlFileDialog = false },
                sheetState = saveTtmlFileSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_save_ttml_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.lyric_timing_confirm_save_ttml_to_dir,
                            ttmlFileName,
                            audioFile.parent?.substringAfterLast("/") ?: ""
                        ),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = ttmlContent,
                            fontSize = 10.sp,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                showSaveTtmlFileDialog = false
                                coroutineScope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        saveTtmlToFileResult(
                                            audioPath = sourceAudioPath,
                                            ttmlContent = ttmlContent,
                                            context = context,
                                            sourceMediaStoreId = sourceMediaStoreId
                                        )
                                    }
                                    if (result.success) {
                                        if (!result.redirectedToFallbackDir) {
                                            notifyTimingSourceLyricsUpdated(sourceAudioPath)
                                        }
                                        ttmlSaveSuccessMessage = if (result.redirectedToFallbackDir) {
                                            context.getString(R.string.lyric_timing_save_redirected_to, result.savedPath)
                                        } else {
                                            context.getString(R.string.lyric_timing_save_file_success_with_path, result.savedPath.ifBlank { sourceAudioPath })
                                        }
                                        showSaveSuccessDialog = true
                                    } else {
                                        ttmlSaveErrorMessage = result.errorMessage.ifBlank { context.getString(R.string.lyric_timing_save_file_failed_retry) }
                                        needStoragePermission = result.needPermission
                                        pendingTtmlRecoverableIntentSender = result.recoverableIntentSender
                                        pendingTtmlContentForRetry = ttmlContent
                                        showSaveFailDialog = true
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.lyric_timing_confirm_save))
                        }
                    }
                }
            }
        }
        
        // 时间输入对话框 - ModalBottomSheet
        val timeInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showTimeInputDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    showTimeInputDialog = false
                    inputTimeMinutes = ""
                    inputTimeSeconds = ""
                    inputTimeMilliseconds = ""
                },
                sheetState = timeInputSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_jump_to_time),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.lyric_timing_current_time_with_value, formatTime(currentTime)),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedTextField(
                            value = inputTimeMinutes,
                            onValueChange = { 
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    inputTimeMinutes = it
                                }
                            },
                            placeholder = stringResource(R.string.lyric_timing_time_minute_placeholder),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        ThemedTextField(
                            value = inputTimeSeconds,
                            onValueChange = { 
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    inputTimeSeconds = it
                                }
                            },
                            placeholder = stringResource(R.string.lyric_timing_time_second_placeholder),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(
                            text = ".",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        ThemedTextField(
                            value = inputTimeMilliseconds,
                            onValueChange = { 
                                if (it.length <= 3 && it.all { c -> c.isDigit() }) {
                                    inputTimeMilliseconds = it
                                }
                            },
                            placeholder = stringResource(R.string.lyric_timing_time_millisecond_placeholder),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 新增三个轮廓按钮
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 进入播放跟随模式按钮（在跟随模式下隐藏）
                        if (!isFollowMode) {
                            OutlinedButton(
                                onClick = {
                                    isFollowMode = true
                                    showTimeInputDialog = false
                                    inputTimeMinutes = ""
                                    inputTimeSeconds = ""
                                    inputTimeMilliseconds = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.lyric_timing_enter_follow_mode))
                            }
                        }
                        
                        // 跳转到歌曲开头按钮
                        OutlinedButton(
                            onClick = {
                                findFirstSelectableTimingUnit(lyricLines)?.let { (lineIndex, unitIndex) ->
                                    selectedLineIndex = lineIndex
                                    selectedWordIndex = unitIndex
                                    val firstUnit = lyricLines[lineIndex].timeUnits[unitIndex]
                                    val timeMs = parseTimeToMs(firstUnit.startTime)
                                    currentTime = timeMs
                                    onSeekTo(timeMs)
                                }
                                showTimeInputDialog = false
                                inputTimeMinutes = ""
                                inputTimeSeconds = ""
                                inputTimeMilliseconds = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.lyric_timing_jump_song_start))
                        }
                        
                        // 跳转到歌曲结尾按钮
                        OutlinedButton(
                            onClick = {
                                findLastSelectableTimingUnit(lyricLines)?.let { (lineIndex, unitIndex) ->
                                    selectedLineIndex = lineIndex
                                    selectedWordIndex = unitIndex
                                    val lastUnit = lyricLines[lineIndex].timeUnits[unitIndex]
                                    val timeMs = parseTimeToMs(lastUnit.startTime)
                                    currentTime = timeMs
                                    onSeekTo(timeMs)
                                }
                                showTimeInputDialog = false
                                inputTimeMinutes = ""
                                inputTimeSeconds = ""
                                inputTimeMilliseconds = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.lyric_timing_jump_song_end))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = {
                                val minutes = inputTimeMinutes.toLongOrNull() ?: 0L
                                val seconds = inputTimeSeconds.toLongOrNull() ?: 0L
                                val ms = inputTimeMilliseconds.toLongOrNull() ?: 0L
                                val newTime = minutes * 60 * 1000 + seconds * 1000 + ms
                                
                                if (newTime in 0..audioDuration) {
                                    currentTime = newTime
                                    onSeekTo(newTime)
                                    
                                    // 查找并选择对应的歌词单元
                                    if (lyricLines.isNotEmpty()) {
                                        var foundLineIndex = 0
                                        var foundWordIndex = 0
                                        for ((lineIndex, line) in lyricLines.withIndex()) {
                                            for ((wordIndex, unit) in line.timeUnits.withIndex()) {
                                                val beginMs = parseTimeToMs(unit.startTime)
                                                val endMs = parseTimeToMs(unit.endTime)
                                                if (newTime in beginMs..endMs) {
                                                    foundLineIndex = lineIndex
                                                    foundWordIndex = wordIndex
                                                    break
                                                }
                                            }
                                        }
                                        selectedLineIndex = foundLineIndex
                                        selectedWordIndex = foundWordIndex
                                    }
                                    
                                    showTimeInputDialog = false
                                    inputTimeMinutes = ""
                                    inputTimeSeconds = ""
                                    inputTimeMilliseconds = ""
                                    
                                    // 开始播放
                                    isPlaying = true
                                    onPlayPause(true)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.lyric_timing_confirm_and_play))
                        }
                    }
                }
            }
        }
        
        // M4A解码对话框
        if (showConvertDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.lyric_timing_audio_transcoding_title)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.lyric_timing_audio_transcoding_running),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { convertProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = convertMessage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {}
            )
        }
        
        // 嵌入结果显示对话框 - AlertDialog
        if (showEmbedResultDialog) {
            AlertDialog(
                onDismissRequest = { showEmbedResultDialog = false },
                title = { Text(text = if (embedResultSuccess) "嵌入成功" else "嵌入失败") },
                text = {
                    Column {
                        Text(
                            text = embedResultMessage,
                            fontSize = 14.sp,
                            color = if (embedResultSuccess) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        if (!embedResultSuccess && !needStoragePermission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.lyric_timing_embed_failure_reasons),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    if (pendingRecoverableEmbedIntentSender != null) {
                        Button(onClick = {
                            launchRecoverableEmbedPermissionIfNeeded()
                        }) {
                            Text(stringResource(R.string.lyric_timing_grant_write_permission))
                        }
                    } else if (needStoragePermission) {
                        Button(onClick = {
                            showEmbedResultDialog = false
                            com.example.LyricBox.utils.AudioMetadataReader.requestStoragePermission(context)
                        }) {
                            Text("设置权限")
                        }
                    } else {
                        Button(onClick = { showEmbedResultDialog = false }) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEmbedResultDialog = false
                        pendingRecoverableEmbedIntentSender = null
                        pendingRecoverableEmbedLyricsContent = null
                    }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
        
        // 格式化时间轴确认对话框
        if (showFormatTimelineConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showFormatTimelineConfirmDialog = false },
                title = { Text(stringResource(R.string.lyric_timing_confirm_format_timeline_title)) },
                text = { Text(stringResource(R.string.lyric_timing_confirm_format_timeline_message)) },
                confirmButton = {
                    Button(onClick = {
                        showFormatTimelineConfirmDialog = false
                        val oldLines = lyricLines.toList()
                        val newLines = LyricBatchEditUtils.formatTimeline(lyricLines)
                        lyricLines = newLines
                        undoRedoManager.pushAction(
                            UndoAction(
                                actionType = UndoActionType.BATCH_FORMAT_TIMELINE,
                                lineIndex = 0,
                                oldValue = oldLines,
                                newValue = newLines
                            )
                        )
                        updateUndoRedoState()
                        formatTimelineResultSuccess = true
                        formatTimelineResultMessage = "时间轴格式化成功！共处理 ${newLines.size} 行歌词。"
                        showFormatTimelineResultDialog = true
                    }) {
                        Text(stringResource(R.string.common_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFormatTimelineConfirmDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
        
        // 格式化时间轴结果对话框
        if (showFormatTimelineResultDialog) {
            AlertDialog(
                onDismissRequest = { showFormatTimelineResultDialog = false },
                title = { Text(text = if (formatTimelineResultSuccess) "格式化成功" else "格式化失败") },
                text = {
                    Text(
                        text = formatTimelineResultMessage,
                        fontSize = 14.sp,
                        color = if (formatTimelineResultSuccess) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = { showFormatTimelineResultDialog = false }) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            )
        }
        
        // 歌词显示区域
        LyricDisplayArea(
            lyricLines = lyricLines,
            selectedLineIndex = selectedLineIndex,
            selectedWordIndex = selectedWordIndex,
            currentTime = currentTime,
            isDarkTheme = isDarkTheme,
            isFollowMode = isFollowMode,
            isPlaying = isPlaying,
            hasLyrics = hasLyrics,
            timestampMinWidth = timestampMinWidth,
            lazyListState = lazyListState,
            coroutineScope = coroutineScope,
            creators = creators,
            onCreatorsChange = { newCreators ->
                creators = newCreators
                // 同步更新 pendingLyricsCreators，确保传递到预览界面
                onPendingLyricsCreatorsChange(newCreators)
            },
            onLyricLinesChange = { newLines -> lyricLines = newLines },
            onSelectedLineIndexChange = { idx -> selectedLineIndex = idx },
            onSelectedWordIndexChange = { idx -> selectedWordIndex = idx },
            onCurrentTimeChange = { time -> currentTime = time },
            onIsFollowModeChange = { follow -> isFollowMode = follow },
            onShowEditControlPanelChange = { show -> showEditControlPanel = show },
            onMenuLineIndexChange = { idx -> menuLineIndex = idx },
            onMenuUnitIndexChange = { idx -> menuUnitIndex = idx },
            onTranslationMenuLineIndexChange = { idx -> translationMenuLineIndex = idx },
            onEditTranslationRequested = { lineIndex ->
                if (lineIndex in lyricLines.indices) {
                    showEditControlPanel = false
                    menuLineIndex = lineIndex
                    menuUnitIndex = 0
                    translationMenuLineIndex = lineIndex
                    selectedLineIndex = lineIndex
                    selectedWordIndex = 0
                    addTranslationText = lyricLines[lineIndex].translation
                    originalAddTranslationText = addTranslationText
                    showAddTranslationCancelConfirm = false
                    pendingAddTranslationDismiss = false
                    showAddTranslationDialog = true
                }
            },
            onSeekTo = onSeekTo,
            onPlayPause = onPlayPause,
            onIsPlayingChange = { playing -> isPlaying = playing },
            onUpdateJobCancel = { updateJob?.cancel() },
            onIsPreviewModeChange = { preview -> isPreviewMode = preview },
            isPreviewMode = isPreviewMode,
            modifier = Modifier.weight(1f)
        )
        
        // 底部控制区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 导入按钮区域
            if (!hasLyrics || !hasAudio) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasLyrics) {
                        var importLyricButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
                        val density = LocalDensity.current
                        Box {
                            Button(
                                onClick = { showImportLyricMenu = true },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .onGloballyPositioned { coordinates ->
                                        val bounds = coordinates.boundsInRoot()
                                        val centerX = bounds.center.x
                                        val centerY = bounds.center.y
                                        importLyricButtonPosition = MenuAnchorPosition(
                                            x = with(density) { centerX.toDp().value },
                                            y = with(density) { centerY.toDp().value }
                                        )
                                    }
                            ) {
                                Text(text = stringResource(R.string.lyric_timing_import_lyrics))
                            }
                            CustomDropdownMenu(
                                expanded = showImportLyricMenu,
                                onDismissRequest = { showImportLyricMenu = false },
                                items = listOf(
                                    MenuItem(title = stringResource(R.string.lyric_timing_menu_import_plain_text), onClick = { showLyricInputDialog = true }),
                                    MenuItem(title = stringResource(R.string.lyric_timing_menu_import_lrc), onClick = { showSPLLrcInputDialog = true }),
                                    MenuItem(title = stringResource(R.string.lyric_timing_menu_import_elrc), onClick = { showElrcInputDialog = true }),
                                    MenuItem(title = stringResource(R.string.lyric_timing_menu_import_ttml), onClick = { showTtmlInputDialog = true }),
                                    MenuItem(title = stringResource(R.string.lyric_timing_menu_fetch_verbatim), onClick = {
                                        showImportLyricMenu = false
                                        val keyword = if (sourceTitle.isNotEmpty() && sourceArtist.isNotEmpty()) "$sourceTitle $sourceArtist" else sourceTitle
                                        onOpenVerbatimLyrics(keyword)
                                    })
                                ),
                                anchorPosition = importLyricButtonPosition ?: MenuAnchorPosition(0f, 0f)
                            )
                        }
                    }
                    
                    if (!hasAudio) {
                        // 未导入音频：显示导入音频按钮
                        Button(
                            onClick = { onImportAudio() },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Text(text = stringResource(R.string.lyric_timing_menu_import_audio))
                        }
                    }
                }
            }
            
            // 播放控制区域（已导入音频）
            if (hasAudio) {
                val sideControlButtonWidth = 72.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    fun togglePlayback() {
                        if (isPlaying) {
                            onPlayPause(false)
                            updateJob?.cancel()
                        } else {
                            onPlayPause(true)
                            audioDuration = getAudioDuration()
                            updateJob = coroutineScope.launch {
                                while (true) {
                                    delay(100)
                                    currentTime = getCurrentPosition()
                                }
                            }
                        }
                        isPlaying = !isPlaying
                    }

                    // 播放暂停按钮
                    Box(
                        modifier = Modifier
                            .width(sideControlButtonWidth)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .combinedClickable(
                                onClick = { togglePlayback() },
                                onLongClickLabel = "重新载入当前音频",
                                onLongClick = {
                                    onPlayPause(false)
                                    updateJob?.cancel()
                                    updateJob = null
                                    isPlaying = false
                                    onReloadCurrentAudio()
                                    currentTime = getCurrentPosition()
                                    audioDuration = getAudioDuration()
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                            ),
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // 播放进度条
                    Slider(
                        value = currentTime.toFloat(),
                        onValueChange = { value ->
                            val newTime = value.toLong()
                            currentTime = newTime
                            onSeekTo(newTime)
                        },
                        valueRange = 0f..audioDuration.toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    )
                    Text(
                        text = formatTime(currentTime),
                        modifier = Modifier
                            .widthIn(min = 80.dp)
                            .clickable {
                                if (hasAudio) {
                                    wasPlayingBeforeTimeInput = isPlaying
                                    if (isPlaying) {
                                        isPlaying = false
                                        onPlayPause(false)
                                    }
                                    val currentMinutes = (currentTime / 60000) % 60
                                    val currentSeconds = (currentTime / 1000) % 60
                                    val currentMs = currentTime % 1000
                                    inputTimeMinutes = String.format("%02d", currentMinutes)
                                    inputTimeSeconds = String.format("%02d", currentSeconds)
                                    inputTimeMilliseconds = String.format("%03d", currentMs)
                                    showTimeInputDialog = true
                                }
                            },
                        textAlign = TextAlign.End
                    )
                    // 倍速按钮
                    Button(
                        onClick = {
                            tempSpeed = playbackSpeed
                            showSpeedDialog = true
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(sideControlButtonWidth)
                    ) {
                        Text(text = "${String.format("%.2f", playbackSpeed).trimEnd('0').trimEnd('.')}X")
                    }
                }
            }

            // 打轴控制区域（已导入歌词）
            if (hasLyrics && !isPreviewMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = if (hasAudio) 16.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 快退按钮
                    Button(
                        onClick = {
                            val newTime = maxOf(0L, currentTime - seekTimeMs)
                            currentTime = newTime
                            onSeekTo(newTime)
                        },
                        modifier = Modifier.weight(0.8f),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rewind),
                            contentDescription = stringResource(R.string.lyric_timing_seek_backward_seconds, seekTimeSeconds.toInt()),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (isFollowMode) {
                        // 跟随模式下显示退出按钮
                        Button(
                            onClick = { isFollowMode = false },
                            modifier = Modifier.weight(3.4f)
                        ) {
                            Text(text = stringResource(R.string.lyric_timing_exit_follow_mode))
                        }
                    } else {
                        // 正常模式显示起始、连续、结束按钮
                        Button(
                            onClick = {
                                performVibration()
                                // 设置开始时间
                                if (selectedLineIndex < lyricLines.size && selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size) {
                                    val newLines = lyricLines.toMutableList()
                                    val currentLine = newLines[selectedLineIndex]
                                    val newTimeUnits = currentLine.timeUnits.toMutableList()
                                    val currentTimeStr = formatTime(currentTime)
                                    val oldUnit = newTimeUnits[selectedWordIndex]
                                    val newUnit = LyricTimeUnit(oldUnit.text, currentTimeStr, oldUnit.endTime)
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.TIME_CHANGE,
                                        lineIndex = selectedLineIndex,
                                        unitIndex = selectedWordIndex,
                                        oldValue = oldUnit,
                                        newValue = newUnit
                                    ))
                                    updateUndoRedoState()
                                    newTimeUnits[selectedWordIndex] = newUnit
                                    newLines[selectedLineIndex] = currentLine.copy(timeUnits = newTimeUnits)
                                    lyricLines = newLines
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(text = stringResource(R.string.lyric_timing_set_start), maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = {
                                performVibration()
                                // 设置结束时间并跳转到下一个字
                                if (selectedLineIndex < lyricLines.size && selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size) {
                                    val newLines = lyricLines.toMutableList()
                                    val currentLine = newLines[selectedLineIndex]
                                    val newTimeUnits = currentLine.timeUnits.toMutableList()
                                    val currentTimeStr = formatTime(currentTime)

                                    // 设置当前字的结束时间
                                    val oldUnit = newTimeUnits[selectedWordIndex]
                                    val newUnit = LyricTimeUnit(oldUnit.text, oldUnit.startTime, currentTimeStr)
                                    val actions = mutableListOf<UndoAction>()
                                    actions.add(UndoAction(
                                        actionType = UndoActionType.TIME_CHANGE,
                                        lineIndex = selectedLineIndex,
                                        unitIndex = selectedWordIndex,
                                        oldValue = oldUnit,
                                        newValue = newUnit
                                    ))
                                    newTimeUnits[selectedWordIndex] = newUnit
                                    newLines[selectedLineIndex] = currentLine.copy(timeUnits = newTimeUnits)

                                    // 跳转到下一个有内容的字并设置其开始时间，跳过空白行组件
                                    findNextSelectableTimingUnit(lyricLines, selectedLineIndex, selectedWordIndex)?.let { (nextLineIndex, nextUnitIndex) ->
                                        selectedLineIndex = nextLineIndex
                                        selectedWordIndex = nextUnitIndex
                                        val nextLine = newLines[nextLineIndex]
                                        val nextTimeUnits = nextLine.timeUnits.toMutableList()
                                        val nextOldUnit = nextTimeUnits[nextUnitIndex]
                                        val nextNewUnit = LyricTimeUnit(nextOldUnit.text, currentTimeStr, nextOldUnit.endTime)
                                        actions.add(UndoAction(
                                            actionType = UndoActionType.TIME_CHANGE,
                                            lineIndex = nextLineIndex,
                                            unitIndex = nextUnitIndex,
                                            oldValue = nextOldUnit,
                                            newValue = nextNewUnit
                                        ))
                                        nextTimeUnits[nextUnitIndex] = nextNewUnit
                                        newLines[nextLineIndex] = nextLine.copy(timeUnits = nextTimeUnits)
                                    }

                                    undoRedoManager.pushBatchAction(BatchUndoAction(actions, "连续设置时间戳"))
                                    updateUndoRedoState()
                                    lyricLines = newLines
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(text = stringResource(R.string.lyric_timing_set_continuous), maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = {
                                performVibration()
                                // 设置结束时间并跳转到下一个字
                                if (selectedLineIndex < lyricLines.size && selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size) {
                                    val newLines = lyricLines.toMutableList()
                                    val currentLine = newLines[selectedLineIndex]
                                    val newTimeUnits = currentLine.timeUnits.toMutableList()
                                    val currentTimeStr = formatTime(currentTime)

                                    // 设置当前字的结束时间
                                    val oldUnit = newTimeUnits[selectedWordIndex]
                                    val newUnit = LyricTimeUnit(oldUnit.text, oldUnit.startTime, currentTimeStr)
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.TIME_CHANGE,
                                        lineIndex = selectedLineIndex,
                                        unitIndex = selectedWordIndex,
                                        oldValue = oldUnit,
                                        newValue = newUnit
                                    ))
                                    updateUndoRedoState()
                                    newTimeUnits[selectedWordIndex] = newUnit
                                    newLines[selectedLineIndex] = currentLine.copy(timeUnits = newTimeUnits)
                                    lyricLines = newLines

                                    // 跳转到下一个有内容的字，跳过空白行组件
                                    findNextSelectableTimingUnit(lyricLines, selectedLineIndex, selectedWordIndex)?.let { (nextLineIndex, nextUnitIndex) ->
                                        selectedLineIndex = nextLineIndex
                                        selectedWordIndex = nextUnitIndex
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(text = stringResource(R.string.lyric_timing_set_end), maxLines = 1, softWrap = false)
                        }
                    }

                    // 快进按钮
                    Button(
                        onClick = {
                            val newTime = minOf(audioDuration, currentTime + seekTimeMs)
                            currentTime = newTime
                            onSeekTo(newTime)
                        },
                        modifier = Modifier.weight(0.8f),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.fastforward),
                            contentDescription = stringResource(R.string.lyric_timing_seek_forward_seconds, seekTimeSeconds.toInt()),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // 编辑控制面板 - ModalBottomSheet
        val editControlPanelSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showEditControlPanel && menuLineIndex >= 0 && menuUnitIndex >= 0) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showEditControlPanel = false },
                sheetState = editControlPanelSheetState
            ) {
                EditControlPanel(
                    menuLineIndex = menuLineIndex,
                    menuUnitIndex = menuUnitIndex,
                    lyricLines = lyricLines,
                    onDismiss = { showEditControlPanel = false },
                    onEditLyric = {
                        showEditControlPanel = false
                        if (menuLineIndex < lyricLines.size && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
                            val unit = lyricLines[menuLineIndex].timeUnits[menuUnitIndex]
                            editUnitText = unit.text
                            originalEditUnitText = editUnitText
                            editUnitTransliteration = unit.transliteration
                            originalEditUnitTransliteration = editUnitTransliteration
                            editUnitCharTransliterations = unit.charTransliterations.toMutableMap()
                            originalEditUnitCharTransliterations = unit.charTransliterations.toMap()
                            previousText = editUnitText
                            showSavedIndicator = false
                            showEditUnitDialog = true
                        }
                    },
                    onAddLyric = {
                        showEditControlPanel = false
                        addLyricText = ""
                        originalAddLyricText = ""
                        addLyricPosition = 1
                        showAddLyricDialog = true
                    },
                    onSplitLyric = {
                        showEditControlPanel = false
                        if (menuLineIndex < lyricLines.size && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
                            splitLyricText = lyricLines[menuLineIndex].timeUnits[menuUnitIndex].text
                            originalSplitLyricText = splitLyricText
                            showSplitLyricDialog = true
                        }
                    },
                    onMergeLyric = {
                        showEditControlPanel = false
                        mergeSelectedUnits = setOf(menuUnitIndex)
                        originalMergeSelectedUnits = setOf(menuUnitIndex)
                        mergeLyricPreview = emptyList()
                        originalMergeLyricPreview = emptyList()
                        mergeLyricHistory = emptyList()
                        showMergeLyricDialog = true
                    },
                    onSetTimestamp = {
                        showEditControlPanel = false
                        if (menuLineIndex < lyricLines.size && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
                            val unit = lyricLines[menuLineIndex].timeUnits[menuUnitIndex]
                            tempStartTime = unit.startTime
                            tempEndTime = unit.endTime
                            originalTempStartTime = unit.startTime
                            originalTempEndTime = unit.endTime
                            showTimeEditorInSheet = false
                            showSetTimestampDialog = true
                        }
                    },
                    onDeleteLyric = {
                        showEditControlPanel = false
                        showDeleteUnitConfirmDialog = true
                    },
                    onAddLine = {
                        showEditControlPanel = false
                        addLineText = ""
                        originalAddLineText = ""
                        addLinePosition = 1
                        showAddLineDialog = true
                    },
                    onMergeLine = {
                        showEditControlPanel = false
                        mergeLinesSelected = setOf(menuLineIndex)
                        originalMergeLinesSelected = setOf(menuLineIndex)
                        mergeLinesAddSpace = false
                        mergeLinesPreview = emptyList()
                        originalMergeLinesPreview = emptyList()
                        mergeLinesHistory = emptyList()
                        showMergeLinesDialog = true
                    },
                    onSplitLine = {
                        showEditControlPanel = false
                        if (menuLineIndex < lyricLines.size) {
                            splitToMultipleLinesText = lyricLines[menuLineIndex].timeUnits.joinToString("") { it.text }
                            originalSplitToMultipleLinesText = splitToMultipleLinesText
                            showSplitToMultipleLinesDialog = true
                        }
                    },
                    onMoveLine = {
                        showEditControlPanel = false
                        moveLineTargetIndex = -1
                        originalMoveLineTargetIndex = -1
                        moveLinePosition = 0
                        originalMoveLinePosition = 0
                        showMoveLineDialog = true
                    },
                    onAddTranslation = {
                        showEditControlPanel = false
                        translationMenuLineIndex = menuLineIndex
                        addTranslationText = ""
                        originalAddTranslationText = ""
                        showAddTranslationCancelConfirm = false
                        pendingAddTranslationDismiss = false
                        showAddTranslationDialog = true
                    },
                    onEditTranslation = {
                        showEditControlPanel = false
                        val targetLineIndex = when {
                            menuLineIndex >= 0 && menuLineIndex < lyricLines.size -> menuLineIndex
                            translationMenuLineIndex >= 0 && translationMenuLineIndex < lyricLines.size -> translationMenuLineIndex
                            else -> -1
                        }
                        if (targetLineIndex >= 0) {
                            menuLineIndex = targetLineIndex
                            translationMenuLineIndex = targetLineIndex
                            addTranslationText = lyricLines[targetLineIndex].translation
                            originalAddTranslationText = addTranslationText
                            showAddTranslationCancelConfirm = false
                            pendingAddTranslationDismiss = false
                            showAddTranslationDialog = true
                        }
                    },
                    onDeleteLine = {
                        showEditControlPanel = false
                        showDeleteLineConfirmDialog = true
                    },
                    showAddTranslation = menuLineIndex < lyricLines.size && lyricLines[menuLineIndex].translation.isEmpty(),
                    showEditTranslation = menuLineIndex < lyricLines.size && lyricLines[menuLineIndex].translation.isNotEmpty(),
                    isBlankLineMenu = menuLineIndex in lyricLines.indices && isBlankLyricLine(lyricLines[menuLineIndex])
                )
            }
        }

        // 倍速设置对话框 - ModalBottomSheet
        val speedSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showSpeedDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showSpeedDialog = false },
                sheetState = speedSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_playback_speed_settings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.lyric_timing_current_speed_with_value, String.format("%.2f", tempSpeed).trimEnd('0').trimEnd('.')),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Slider(
                        value = tempSpeed,
                        onValueChange = { tempSpeed = it },
                        valueRange = 0.2f..3f,
                        steps = 27
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.2X", fontSize = 12.sp)
                        Text("1.6X", fontSize = 12.sp)
                        Text("3X", fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = { showSpeedDialog = false }
                        ) {
                            Text(stringResource(R.string.common_cancel))
                        }
                        Button(
                            onClick = {
                                playbackSpeed = tempSpeed
                                onSetPlaybackSpeed(tempSpeed)
                                showSpeedDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    }
                }
            }
        }
        
        WordLyricSaveDialog(
            showDialog = showLyricDialog,
            onDismiss = { showLyricDialog = false },
            savedLyric = savedLyricContent,
            onCopied = { showCopiedDialog = true }
        )
        
        SimpleAlertDialog(
            showDialog = showSaveSuccessDialog,
            onDismiss = { showSaveSuccessDialog = false },
            title = stringResource(R.string.lyric_timing_save_success_title),
            text = ttmlSaveSuccessMessage
        )
        
        SimpleAlertDialog(
            showDialog = showSaveFailDialog,
            onDismiss = {
                showSaveFailDialog = false
                pendingTtmlRecoverableIntentSender = null
                pendingTtmlContentForRetry = null
            },
            title = stringResource(R.string.lyric_timing_save_failed_title),
            text = ttmlSaveErrorMessage
        )

        if (showSaveFailDialog && (pendingTtmlRecoverableIntentSender != null || needStoragePermission)) {
            val actionLabel = if (pendingTtmlRecoverableIntentSender != null) "授予写入权限" else "设置权限"
            AlertDialog(
                onDismissRequest = {
                    showSaveFailDialog = false
                    pendingTtmlRecoverableIntentSender = null
                    pendingTtmlContentForRetry = null
                },
                title = { Text("保存失败") },
                text = { Text(ttmlSaveErrorMessage) },
                confirmButton = {
                    Button(onClick = {
                        if (pendingTtmlRecoverableIntentSender != null) {
                            val sender = pendingTtmlRecoverableIntentSender
                            if (sender != null) {
                                recoverableTtmlSavePermissionLauncher.launch(
                                    IntentSenderRequest.Builder(sender).build()
                                )
                            }
                        } else if (needStoragePermission) {
                            showSaveFailDialog = false
                            com.example.LyricBox.utils.AudioMetadataReader.requestStoragePermission(context)
                        }
                    }) {
                        Text(actionLabel)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSaveFailDialog = false
                        pendingTtmlRecoverableIntentSender = null
                        pendingTtmlContentForRetry = null
                    }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
        
        SimpleAlertDialog(
            showDialog = showCopiedDialog,
            onDismiss = { showCopiedDialog = false },
            title = stringResource(R.string.lyric_timing_copied_title),
            text = stringResource(R.string.lyric_timing_copied_message),
        )
        

        
        // 编辑歌词单元对话框 - ModalBottomSheet
        val editUnitSheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        var pendingDismiss by remember { mutableStateOf(false) }
        
        // 检查是否有未保存的修改
        fun hasUnsavedChanges(): Boolean {
            return editUnitText != originalEditUnitText ||
                    editUnitTransliteration != originalEditUnitTransliteration ||
                    editUnitCharTransliterations != originalEditUnitCharTransliterations
        }
        
        // 切换到上一个/下一个单元（支持跨行）
        fun switchUnit(delta: Int) {
            var newLineIndex = menuLineIndex
            var newUnitIndex = menuUnitIndex + delta
            
            // 寻找合适的单元
            while (newLineIndex >= 0 && newLineIndex < lyricLines.size) {
                val line = lyricLines[newLineIndex]
                if (newUnitIndex >= 0 && newUnitIndex < line.timeUnits.size) {
                    // 找到了有效的单元
                    val unit = line.timeUnits[newUnitIndex]
                    editUnitText = unit.text
                    originalEditUnitText = editUnitText
                    editUnitTransliteration = unit.transliteration
                    originalEditUnitTransliteration = editUnitTransliteration
                    editUnitCharTransliterations = unit.charTransliterations.toMutableMap()
                    originalEditUnitCharTransliterations = unit.charTransliterations.toMap()
                    previousText = editUnitText
                    showSavedIndicator = false
                    menuLineIndex = newLineIndex
                    menuUnitIndex = newUnitIndex
                    return
                }
                
                // 跨行查找
                if (delta > 0) {
                    // 下一个：检查下一行
                    newLineIndex++
                    newUnitIndex = 0
                } else {
                    // 上一个：检查上一行
                    newLineIndex--
                    if (newLineIndex >= 0) {
                        newUnitIndex = lyricLines[newLineIndex].timeUnits.size - 1
                    }
                }
            }
        }
        
        if (showEditUnitDialog) {
            androidx.compose.material3.ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetMaxWidth = Dp.Unspecified,
                onDismissRequest = {
                    if (hasUnsavedChanges()) {
                        pendingDismiss = true
                        showEditUnitCancelConfirm = true
                    } else {
                        showEditUnitDialog = false
                    }
                },
                sheetState = editUnitSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .animateContentSize(animationSpec = tween(300))
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_edit_lyrics),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 歌词文本编辑
                    ThemedTextField(
                        value = editUnitText,
                        onValueChange = { newText ->
                            // 如果文本改变，需要调整 charTransliterations 的索引
                            if (newText != previousText) {
                                val oldText = previousText
                                previousText = newText
                                
                                // 尝试智能调整注音索引
                                val newCharTransliterations = mutableMapOf<Int, String>()
                                
                                // 获取旧/新文本中的可注音字符单元（小假名与前一字符绑定）
                                val oldCjkUnits = extractCjkTransliterationUnits(oldText)
                                val newCjkUnits = extractCjkTransliterationUnits(newText)
                                
                                // 如果可注音单元数量相同，按顺序迁移注音
                                if (oldCjkUnits.size == newCjkUnits.size && oldCjkUnits.isNotEmpty()) {
                                    for (i in oldCjkUnits.indices) {
                                        val oldUnit = oldCjkUnits[i]
                                        val newUnit = newCjkUnits[i]
                                        val trans = findTransliterationForUnit(
                                            indices = oldUnit.indices,
                                            charTransliterations = editUnitCharTransliterations
                                        )
                                        if (trans.isNotBlank()) {
                                            newCharTransliterations[newUnit.startIndex] = trans
                                        }
                                    }
                                }
                                // 否则清空注音（因为可注音单元变化太大，无法确定映射关系）
                                
                                editUnitCharTransliterations = newCharTransliterations
                            }
                            editUnitText = newText
                        },
                        placeholder = stringResource(R.string.lyric_timing_lyrics_content_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 编辑注音
                    Text(
                        text = stringResource(R.string.lyric_timing_edit_transliteration),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 检查是否需要显示整体注音编辑
                    val cjkUnits = extractCjkTransliterationUnits(editUnitText)
                    val cjkCount = cjkUnits.size
                    val hasUnassignedTransliteration = editUnitTransliteration.isNotEmpty() && 
                            editUnitCharTransliterations.isEmpty()
                    
                    // 单字符注音编辑（如果有CJK字符）
                    if (cjkCount > 0) {
                        Text(
                            text = stringResource(R.string.lyric_timing_single_char_transliteration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cjkUnits.forEach { unit ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = unit.text,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    // 使用key确保正确的重组
                                    key(unit.startIndex) {
                                        val currentValue = findTransliterationForUnit(
                                            indices = unit.indices,
                                            charTransliterations = editUnitCharTransliterations
                                        )
                                        ThemedTextField(
                                            value = currentValue,
                                            onValueChange = { newValue ->
                                                val newMap = editUnitCharTransliterations.toMutableMap()
                                                unit.indices.forEach { idx -> newMap.remove(idx) }
                                                if (newValue.isNotBlank()) {
                                                    newMap[unit.startIndex] = newValue
                                                }
                                                editUnitCharTransliterations = newMap
                                            },
                                            placeholder = stringResource(R.string.lyric_timing_transliteration_placeholder),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // 只在有未分配的整体注音时才显示整体注音编辑
                    if (hasUnassignedTransliteration || (editUnitTransliteration.isNotEmpty() && cjkCount == 0)) {
                        Text(
                            text = stringResource(R.string.lyric_timing_whole_transliteration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ThemedTextField(
                            value = editUnitTransliteration,
                            onValueChange = { editUnitTransliteration = it },
                            placeholder = stringResource(R.string.lyric_timing_whole_transliteration_placeholder),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 上一个/下一个按钮
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 检查是否有前一个单元（支持跨行）
                            val hasPrev = (menuLineIndex > 0) || 
                                    (menuLineIndex < lyricLines.size && menuUnitIndex > 0)
                            OutlinedButton(
                                onClick = {
                                    // 直接切换，先检查是否有未保存修改
                                    val canSwitch = !hasUnsavedChanges()
                                    if (canSwitch) {
                                        switchUnit(-1)
                                    } else {
                                        // 显示确认对话框
                                        editUnitSwitchDelta = -1
                                        showEditUnitSwitchConfirm = true
                                    }
                                },
                                enabled = hasPrev
                            ) {
                                Text("上一个")
                            }
                            
                            // 检查是否有后一个单元（支持跨行）
                            val hasNext = (menuLineIndex < lyricLines.size - 1) || 
                                    (menuLineIndex < lyricLines.size && 
                                     menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size - 1)
                            OutlinedButton(
                                onClick = {
                                    // 直接切换，先检查是否有未保存修改
                                    val canSwitch = !hasUnsavedChanges()
                                    if (canSwitch) {
                                        switchUnit(1)
                                    } else {
                                        // 显示确认对话框
                                        editUnitSwitchDelta = 1
                                        showEditUnitSwitchConfirm = true
                                    }
                                },
                                enabled = hasNext
                            ) {
                                Text("下一个")
                            }
                        }
                        
                        // 关闭/保存按钮
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            // 保存按钮
                            Button(
                                onClick = {
                                    if (menuLineIndex < lyricLines.size && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
                                        val newLines = lyricLines.toMutableList()
                                        val currentLine = newLines[menuLineIndex]
                                        val newTimeUnits = currentLine.timeUnits.toMutableList()
                                        val oldUnit = newTimeUnits[menuUnitIndex]
                                        val newUnit = LyricTimeUnit(
                                            editUnitText,
                                            oldUnit.startTime,
                                            oldUnit.endTime,
                                            editUnitTransliteration,
                                            editUnitCharTransliterations.toMap()
                                        )
                                        undoRedoManager.pushAction(UndoAction(
                                            actionType = UndoActionType.TEXT_CHANGE,
                                            lineIndex = menuLineIndex,
                                            unitIndex = menuUnitIndex,
                                            oldValue = oldUnit,
                                            newValue = newUnit
                                        ))
                                        updateUndoRedoState()
                                        newTimeUnits[menuUnitIndex] = newUnit
                                        newLines[menuLineIndex] = currentLine.copy(timeUnits = newTimeUnits)
                                        lyricLines = newLines
                                        
                                        // 更新原始值
                                        originalEditUnitText = editUnitText
                                        originalEditUnitTransliteration = editUnitTransliteration
                                        originalEditUnitCharTransliterations = editUnitCharTransliterations.toMap()
                                        
                                        // 显示"已保存"提示
                                        showSavedIndicator = true
                                        kotlinx.coroutines.GlobalScope.launch {
                                            delay(1000)
                                            showSavedIndicator = false
                                        }
                                    }
                                }
                            ) {
                                if (showSavedIndicator) {
                                    Text("已保存")
                                } else {
                                    Text("保存")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 放弃修改确认对话框
        if (showEditUnitCancelConfirm) {
            AlertDialog(
                onDismissRequest = { 
                    showEditUnitCancelConfirm = false
                    pendingDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
                text = { Text(stringResource(R.string.lyric_timing_discard_lyrics_changes_confirm)) },
                confirmButton = {
                    Button(
                        onClick = { 
                            showEditUnitCancelConfirm = false
                            pendingDismiss = false
                        }
                    ) {
                        Text(stringResource(R.string.common_continue_editing))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditUnitCancelConfirm = false
                            showEditUnitDialog = false
                            pendingDismiss = false
                        }
                    ) {
                        Text(stringResource(R.string.common_discard_changes))
                    }
                }
            )
        }
        
        // 编辑歌词切换单元确认对话框
        if (showEditUnitSwitchConfirm) {
            AlertDialog(
                onDismissRequest = { 
                    showEditUnitSwitchConfirm = false
                    editUnitSwitchDelta = 0
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("未保存") },
                text = { Text("当前修改未保存，是否切换？") },
                confirmButton = {
                    Button(
                        onClick = { 
                            showEditUnitSwitchConfirm = false
                            editUnitSwitchDelta = 0
                            // 继续编辑，不切换
                        }
                    ) {
                        Text(stringResource(R.string.common_continue_editing))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditUnitSwitchConfirm = false
                            // 直接调用 switchUnit
                            val delta = editUnitSwitchDelta
                            editUnitSwitchDelta = 0
                            if (delta != 0) {
                                switchUnit(delta)
                            }
                        }
                    ) {
                        Text("切换")
                    }
                }
            )
        }
        
        // 处理继续编辑后重新显示 ModalBottomSheet
        LaunchedEffect(showEditUnitCancelConfirm, pendingDismiss) {
            if (!showEditUnitCancelConfirm && !pendingDismiss && showEditUnitDialog) {
                // 用户点击了继续编辑，需要重新显示 sheet
                editUnitSheetState.show()
            }
        }
        
        // 新增歌词对话框 - ModalBottomSheet
        val addLyricSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var pendingAddLyricDismiss by remember { mutableStateOf(false) }
        if (showAddLyricDialog) {
            androidx.compose.material3.ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetMaxWidth = Dp.Unspecified,
                onDismissRequest = {
                    if (addLyricText != originalAddLyricText || addLyricPosition != 1) {
                        pendingAddLyricDismiss = true
                        showAddLyricCancelConfirm = true
                    } else {
                        showAddLyricDialog = false
                    }
                },
                sheetState = addLyricSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_add_lyrics),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = addLyricText,
                        onValueChange = { addLyricText = it },
                        placeholder = stringResource(R.string.lyric_timing_lyrics_required_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("添加位置：", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomRadioButtonGroup(
                        options = listOf("之前", "之后"),
                        selectedIndex = addLyricPosition,
                        onSelect = { addLyricPosition = it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = {
                                if (addLyricText.isNotBlank() && menuLineIndex < lyricLines.size) {
                                    val newLines = lyricLines.toMutableList()
                                    val currentLine = newLines[menuLineIndex]
                                    val targetLineIsBlank = isBlankLyricLine(currentLine)
                                    val newTimeUnits = if (targetLineIsBlank) {
                                        mutableListOf<LyricTimeUnit>()
                                    } else {
                                        currentLine.timeUnits.toMutableList()
                                    }
                                    val insertIndex = if (targetLineIsBlank) {
                                        0
                                    } else {
                                        (if (addLyricPosition == 0) menuUnitIndex else menuUnitIndex + 1)
                                            .coerceIn(0, newTimeUnits.size)
                                    }
                                    val addedUnit = LyricTimeUnit(addLyricText, "00:00.000", "00:00.000")
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.UNIT_ADD,
                                        lineIndex = menuLineIndex,
                                        unitIndex = insertIndex,
                                        oldValue = null,
                                        newValue = addedUnit
                                    ))
                                    updateUndoRedoState()
                                    newTimeUnits.add(insertIndex, addedUnit)
                                    newLines[menuLineIndex] = currentLine.copy(timeUnits = newTimeUnits)
                                    lyricLines = newLines
                                    selectedLineIndex = menuLineIndex
                                    selectedWordIndex = insertIndex
                                }
                                showAddLyricDialog = false
                            },
                            enabled = addLyricText.isNotBlank()
                        ) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    }
                }
            }
        }
        if (showAddLyricCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showAddLyricCancelConfirm = false
                    pendingAddLyricDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
                text = { Text("您已修改了内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showAddLyricCancelConfirm = false
                            pendingAddLyricDismiss = false
                        }
                    ) {
                        Text(stringResource(R.string.common_continue_editing))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddLyricCancelConfirm = false
                            showAddLyricDialog = false
                            pendingAddLyricDismiss = false
                        }
                    ) {
                        Text(stringResource(R.string.common_discard_changes))
                    }
                }
            )
        }
        LaunchedEffect(showAddLyricCancelConfirm, pendingAddLyricDismiss) {
            if (!showAddLyricCancelConfirm && !pendingAddLyricDismiss && showAddLyricDialog) {
                addLyricSheetState.show()
            }
        }
        
        // 新增行对话框 - ModalBottomSheet
        val addLineSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var pendingAddLineDismiss by remember { mutableStateOf(false) }
        if (showAddLineDialog) {
            androidx.compose.material3.ModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetMaxWidth = Dp.Unspecified,
                onDismissRequest = {
                    if (addLineText != originalAddLineText || addLinePosition != 1) {
                        pendingAddLineDismiss = true
                        showAddLineCancelConfirm = true
                    } else {
                        showAddLineDialog = false
                    }
                },
                sheetState = addLineSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = 220
                            )
                        )
                ) {
                    Text(
                        text = stringResource(R.string.lyric_timing_add_line),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = addLineText,
                        onValueChange = { addLineText = it },
                        placeholder = stringResource(R.string.lyric_timing_lyrics_content_placeholder),
                        singleLine = false,
                        minLines = 1,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("添加位置：", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomRadioButtonGroup(
                        options = listOf("之前", "之后"),
                        selectedIndex = addLinePosition,
                        onSelect = { addLinePosition = it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = {
                                val parsedLines = if (addLineText.isNotBlank()) {
                                    runCatching {
                                        when (detectLyricsFormat(addLineText)) {
                                            1 -> LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, addLineText).lyricLines
                                            2 -> LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, addLineText).lyricLines
                                            3 -> LyricParsingUtils.parseByType(LyricParseType.TTML, addLineText).lyricLines
                                            else -> emptyList()
                                        }
                                    }.getOrElse { emptyList() }
                                } else {
                                    emptyList()
                                }

                                val linesToInsert = if (parsedLines.isNotEmpty()) {
                                    parsedLines
                                } else if (addLineText.isBlank()) {
                                    listOf(LyricLine(emptyList(), ""))
                                } else {
                                    listOf(
                                        LyricLine(
                                            listOf(LyricTimeUnit(addLineText, "00:00.000", "00:00.000")),
                                            ""
                                        )
                                    )
                                }

                                val newLines = lyricLines.toMutableList()
                                val rawInsertIndex = if (addLinePosition == 0) menuLineIndex else menuLineIndex + 1
                                val insertIndex = rawInsertIndex.coerceIn(0, newLines.size)

                                if (linesToInsert.size == 1) {
                                    undoRedoManager.pushAction(
                                        UndoAction(
                                            actionType = UndoActionType.LINE_ADD,
                                            lineIndex = insertIndex,
                                            unitIndex = -1,
                                            oldValue = null,
                                            newValue = linesToInsert.first()
                                        )
                                    )
                                } else {
                                    val actions = linesToInsert.mapIndexed { index, line ->
                                        UndoAction(
                                            actionType = UndoActionType.LINE_ADD,
                                            lineIndex = insertIndex + index,
                                            unitIndex = -1,
                                            oldValue = null,
                                            newValue = line
                                        )
                                    }
                                    undoRedoManager.pushBatchAction(
                                        BatchUndoAction(actions, "新增${linesToInsert.size}行")
                                    )
                                }
                                updateUndoRedoState()
                                newLines.addAll(insertIndex, linesToInsert)
                                lyricLines = newLines
                                selectedLineIndex = insertIndex
                                selectedWordIndex = 0
                                showAddLineDialog = false
                            },
                            enabled = true
                        ) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    }
                }
            }
        }
        if (showAddLineCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showAddLineCancelConfirm = false
                    pendingAddLineDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text(stringResource(R.string.common_confirm_discard_changes_title)) },
                text = { Text("您已修改了内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showAddLineCancelConfirm = false
                            pendingAddLineDismiss = false
                        }
                    ) {
                        Text(stringResource(R.string.common_continue_editing))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddLineCancelConfirm = false
                            showAddLineDialog = false
                            pendingAddLineDismiss = false
                        }
                    ) {
                        Text(stringResource(R.string.common_discard_changes))
                    }
                }
            )
        }
        LaunchedEffect(showAddLineCancelConfirm, pendingAddLineDismiss) {
            if (!showAddLineCancelConfirm && !pendingAddLineDismiss && showAddLineDialog) {
                addLineSheetState.show()
            }
        }
        
        SplitLyricBottomSheet(
            showSheet = showSplitLyricDialog,
            menuLineIndex = menuLineIndex,
            menuUnitIndex = menuUnitIndex,
            lyricLines = lyricLines,
            splitLyricText = splitLyricText,
            onSplitLyricTextChange = { splitLyricText = it },
            originalSplitLyricText = originalSplitLyricText,
            showCancelConfirm = showSplitLyricCancelConfirm,
            onShowCancelConfirmChange = { showSplitLyricCancelConfirm = it },
            pendingDismiss = pendingSplitLyricDismiss,
            onPendingDismissChange = { pendingSplitLyricDismiss = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showSplitLyricDialog = false }
        )
        
        MergeLyricBottomSheet(
            showSheet = showMergeLyricDialog,
            menuLineIndex = menuLineIndex,
            lyricLines = lyricLines,
            mergeLyricPreview = mergeLyricPreview,
            onMergeLyricPreviewChange = { mergeLyricPreview = it },
            mergeSelectedUnits = mergeSelectedUnits,
            onMergeSelectedUnitsChange = { mergeSelectedUnits = it },
            mergeLyricHistory = mergeLyricHistory,
            onMergeLyricHistoryChange = { mergeLyricHistory = it },
            showCancelConfirm = showMergeLyricCancelConfirm,
            onShowCancelConfirmChange = { showMergeLyricCancelConfirm = it },
            pendingDismiss = pendingMergeLyricDismiss,
            onPendingDismissChange = { pendingMergeLyricDismiss = it },
            originalMergeLyricPreview = originalMergeLyricPreview,
            originalMergeSelectedUnits = originalMergeSelectedUnits,
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showMergeLyricDialog = false }
        )
        
        // 删除当前歌词确认对话框
        if (showDeleteUnitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteUnitConfirmDialog = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除当前歌词单元吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (menuLineIndex < lyricLines.size && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
                                val newLines = lyricLines.toMutableList()
                                val currentLine = newLines[menuLineIndex]
                                val newTimeUnits = currentLine.timeUnits.toMutableList()
                                val deletedUnit = newTimeUnits[menuUnitIndex]
                                
                                // 检查删除后该行是否为空
                                val isLastUnit = newTimeUnits.size == 1
                                if (isLastUnit) {
                                    // 删除整个行
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.LINE_DELETE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = -1,
                                        oldValue = currentLine,
                                        newValue = null
                                    ))
                                    updateUndoRedoState()
                                    newLines.removeAt(menuLineIndex)
                                    // 更新选中的行索引
                                    if (selectedLineIndex >= newLines.size && newLines.isNotEmpty()) {
                                        selectedLineIndex = newLines.size - 1
                                    }
                                    if (selectedLineIndex >= lyricLines.size - 1 && newLines.isNotEmpty()) {
                                        selectedLineIndex = newLines.size - 1
                                    }
                                } else {
                                    // 只删除单元
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.UNIT_DELETE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = menuUnitIndex,
                                        oldValue = deletedUnit,
                                        newValue = null
                                    ))
                                    updateUndoRedoState()
                                    newTimeUnits.removeAt(menuUnitIndex)
                                    newLines[menuLineIndex] = currentLine.copy(timeUnits = newTimeUnits)
                                }
                                
                                lyricLines = newLines
                            }
                            showDeleteUnitConfirmDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteUnitConfirmDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
        
        // 删除当前行确认对话框
        if (showDeleteLineConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteLineConfirmDialog = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除当前整行歌词吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (menuLineIndex < lyricLines.size && lyricLines.size > 1) {
                                val newLines = lyricLines.toMutableList()
                                val deletedLine = newLines[menuLineIndex]
                                undoRedoManager.pushAction(UndoAction(
                                    actionType = UndoActionType.LINE_DELETE,
                                    lineIndex = menuLineIndex,
                                    unitIndex = -1,
                                    oldValue = deletedLine,
                                    newValue = null
                                ))
                                updateUndoRedoState()
                                newLines.removeAt(menuLineIndex)
                                lyricLines = newLines
                                if (selectedLineIndex >= lyricLines.size) {
                                    selectedLineIndex = lyricLines.size - 1
                                }
                            }
                            showDeleteLineConfirmDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDeleteLineConfirmDialog = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                        TextButton(
                            onClick = {
                                showDeleteLineConfirmDialog = false
                                deleteMultipleLinesSelected = setOf(menuLineIndex)
                                originalDeleteMultipleLinesSelected = setOf(menuLineIndex)
                                showDeleteMultipleLinesDialog = true
                            }
                        ) {
                            Text("删除多行")
                        }
                    }
                }
            )
        }
        
        // 删除空行确认对话框
        if (showDeleteEmptyLinesDialog) {
            AlertDialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("检测到空行") },
                text = { Text("检测到歌词存在空行，点击确认删除所有空行。") },
                confirmButton = {
                    Button(
                        onClick = {
                            lyricLines = removeEmptyLines(lyricLines)
                            showDeleteEmptyLinesDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.lyric_timing_confirm_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteEmptyLinesDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
        
        // 删除空行成功提示对话框
        if (showDeleteEmptyLinesSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("删除成功") },
                text = { Text("删除了${deletedEmptyLinesCount}行空行") },
                confirmButton = {
                    Button(onClick = { showDeleteEmptyLinesSuccessDialog = false }) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            )
        }
        
        MergeLinesBottomSheet(
            showSheet = showMergeLinesDialog,
            menuLineIndex = menuLineIndex,
            lyricLines = lyricLines,
            mergeLinesPreview = mergeLinesPreview,
            onMergeLinesPreviewChange = { mergeLinesPreview = it },
            mergeLinesSelected = mergeLinesSelected,
            onMergeLinesSelectedChange = { mergeLinesSelected = it },
            mergeLinesPreviewSelected = mergeLinesPreviewSelected,
            onMergeLinesPreviewSelectedChange = { mergeLinesPreviewSelected = it },
            mergeLinesHistory = mergeLinesHistory,
            onMergeLinesHistoryChange = { mergeLinesHistory = it },
            mergeLinesAddSpace = mergeLinesAddSpace,
            onMergeLinesAddSpaceChange = { mergeLinesAddSpace = it },
            showCancelConfirm = showMergeLinesCancelConfirm,
            onShowCancelConfirmChange = { showMergeLinesCancelConfirm = it },
            pendingDismiss = pendingMergeLinesDismiss,
            onPendingDismissChange = { pendingMergeLinesDismiss = it },
            originalMergeLinesPreview = originalMergeLinesPreview,
            originalMergeLinesSelected = originalMergeLinesSelected,
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showMergeLinesDialog = false }
        )
        
        AddTranslationBottomSheet(
            showSheet = showAddTranslationDialog,
            menuLineIndex = menuLineIndex,
            lyricLines = lyricLines,
            addTranslationText = addTranslationText,
            onAddTranslationTextChange = { addTranslationText = it },
            originalAddTranslationText = originalAddTranslationText,
            showCancelConfirm = showAddTranslationCancelConfirm,
            onShowCancelConfirmChange = { showAddTranslationCancelConfirm = it },
            pendingDismiss = pendingAddTranslationDismiss,
            onPendingDismissChange = { pendingAddTranslationDismiss = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showAddTranslationDialog = false }
        )
        
        MoveLineBottomSheet(
            showSheet = showMoveLineDialog,
            lyricLines = lyricLines,
            menuLineIndex = menuLineIndex,
            moveLineTargetIndex = moveLineTargetIndex,
            onMoveLineTargetIndexChange = { moveLineTargetIndex = it },
            originalMoveLineTargetIndex = originalMoveLineTargetIndex,
            moveLinePosition = moveLinePosition,
            onMoveLinePositionChange = { moveLinePosition = it },
            originalMoveLinePosition = originalMoveLinePosition,
            showCancelConfirm = showMoveLineCancelConfirm,
            onShowCancelConfirmChange = { showMoveLineCancelConfirm = it },
            pendingDismiss = pendingMoveLineDismiss,
            onPendingDismissChange = { pendingMoveLineDismiss = it },
            onLyricLinesChange = { lyricLines = it },
            onSelectedLineIndexChange = { selectedLineIndex = it },
            onDismissSheet = { showMoveLineDialog = false }
        )

        SplitToMultipleLinesBottomSheet(
            showSheet = showSplitToMultipleLinesDialog,
            lyricLines = lyricLines,
            menuLineIndex = menuLineIndex,
            splitText = splitToMultipleLinesText,
            onSplitTextChange = { splitToMultipleLinesText = it },
            originalSplitText = originalSplitToMultipleLinesText,
            showCancelConfirm = showSplitToMultipleLinesCancelConfirm,
            onShowCancelConfirmChange = { showSplitToMultipleLinesCancelConfirm = it },
            pendingDismiss = pendingSplitToMultipleLinesDismiss,
            onPendingDismissChange = { pendingSplitToMultipleLinesDismiss = it },
            onLyricLinesChange = { lyricLines = it },
            onSelectedLineIndexChange = { selectedLineIndex = it },
            onSelectedWordIndexChange = { selectedWordIndex = it },
            onDismissSheet = { showSplitToMultipleLinesDialog = false }
        )
        
        BatchSegmentBottomSheet(
            showSheet = showBatchSegmentDialog,
            lyricLines = lyricLines,
            selectedLines = batchSegmentSelectedLines,
            onSelectedLinesChange = { batchSegmentSelectedLines = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showBatchSegmentDialog = false }
        )

        MergeUnitsBottomSheet(
            showSheet = showMergeUnitsDialog,
            lyricLines = lyricLines,
            selectedLines = mergeUnitsSelectedLines,
            onSelectedLinesChange = { mergeUnitsSelectedLines = it },
            mergeThreshold = mergeUnitsThreshold,
            onMergeThresholdChange = { mergeUnitsThreshold = it },
            showThresholdMenu = showMergeUnitsThresholdMenu,
            onShowThresholdMenuChange = { showMergeUnitsThresholdMenu = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showMergeUnitsDialog = false }
        )

        TimestampShiftBottomSheet(
            showSheet = showTimestampShiftDialog,
            lyricLines = lyricLines,
            selectedLines = timestampShiftSelectedLines,
            onSelectedLinesChange = { timestampShiftSelectedLines = it },
            shiftValue = timestampShiftValue,
            onShiftValueChange = { timestampShiftValue = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showTimestampShiftDialog = false }
        )

        ConvertToSimplifiedBottomSheet(
            showSheet = showConvertToSimplifiedDialog,
            lyricLines = lyricLines,
            selectedLines = convertToSimplifiedSelectedLines,
            onSelectedLinesChange = { convertToSimplifiedSelectedLines = it },
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onLyricLinesChange = { lyricLines = it },
            onDismissSheet = { showConvertToSimplifiedDialog = false }
        )

        SetTimestampBottomSheet(
            showSheet = showSetTimestampDialog,
            menuLineIndex = menuLineIndex,
            onMenuLineIndexChange = { menuLineIndex = it },
            menuUnitIndex = menuUnitIndex,
            onMenuUnitIndexChange = { menuUnitIndex = it },
            lyricLines = lyricLines,
            onLyricLinesChange = { lyricLines = it },
            onSelectedLineIndexChange = { selectedLineIndex = it },
            onSelectedWordIndexChange = { selectedWordIndex = it },
            tempStartTime = tempStartTime,
            onTempStartTimeChange = { tempStartTime = it },
            tempEndTime = tempEndTime,
            onTempEndTimeChange = { tempEndTime = it },
            originalTempStartTime = originalTempStartTime,
            onOriginalTempStartTimeChange = { originalTempStartTime = it },
            originalTempEndTime = originalTempEndTime,
            onOriginalTempEndTimeChange = { originalTempEndTime = it },
            showCancelConfirm = showSetTimestampCancelConfirm,
            onShowCancelConfirmChange = { showSetTimestampCancelConfirm = it },
            pendingDismiss = pendingSetTimestampDismiss,
            onPendingDismissChange = { pendingSetTimestampDismiss = it },
            showTimeEditor = showTimeEditorInSheet,
            onShowTimeEditorChange = { showTimeEditorInSheet = it },
            editingStartTime = editingStartTime,
            onEditingStartTimeChange = { editingStartTime = it },
            showSwitchUnitConfirm = showSwitchUnitConfirm,
            onShowSwitchUnitConfirmChange = { showSwitchUnitConfirm = it },
            targetUnitInfo = targetUnitInfo,
            onTargetUnitInfoChange = { targetUnitInfo = it },
            onSeekTo = onSeekTo,
            onPlayPause = onPlayPause,
            undoRedoManager = undoRedoManager,
            updateUndoRedoState = { updateUndoRedoState() },
            onDismissSheet = { showSetTimestampDialog = false }
        )

        }

        // 撤销/重做悬浮窗口 - 显示在右上角（HeadBar下方）
        UndoRedoFloatingButton(
            hasLyrics = hasLyrics,
            isPreviewMode = isPreviewMode,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = {
                if (canUndo) {
                    lyricLines = performUndo()
                }
            },
            onRedo = {
                if (canRedo) {
                    lyricLines = performRedo()
                }
            },
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
    }
    ScreenContent()
}

fun isCJKCharacter(char: Char): Boolean {
    val codePoint = char.code
    return (codePoint in 0x4E00..0x9FFF) ||
           (codePoint in 0x3400..0x4DBF) ||
           (codePoint in 0x20000..0x2A6DF) ||
           (codePoint in 0x2A700..0x2B73F) ||
           (codePoint in 0x2B740..0x2B81F) ||
           (codePoint in 0x2B820..0x2CEAF) ||
           (codePoint in 0x2CEB0..0x2EBEF) ||
           (codePoint in 0x3000..0x303F) ||
           (codePoint in 0x3040..0x309F) ||
           (codePoint in 0x30A0..0x30FF) ||
           (codePoint in 0x31F0..0x31FF) ||
           (codePoint in 0xAC00..0xD7AF) ||
           (codePoint in 0x30..0x39) // 数字0-9也作为CJK字符
}

private fun isAttachablePunctuation(char: Char): Boolean {
    // 一键分词时：标点不单独成词，优先附着前一个词；句首无前词时附着后一个词。
    // 注意：不把英文单引号(')作为此类标点，避免打断 don't 等英文缩写。
    val type = Character.getType(char)
    return type == Character.START_PUNCTUATION.toInt() ||
        type == Character.END_PUNCTUATION.toInt() ||
        type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
        type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
        type == Character.OTHER_PUNCTUATION.toInt() ||
        type == Character.DASH_PUNCTUATION.toInt()
}

fun smartSegmentLyric(text: String): List<String> {
    val normalizedText = text
        .replace('\u00A0', ' ')
        .replace('\u2009', ' ')
    val result = mutableListOf<String>()
    var pendingSpaces = ""
    var pendingPrefixPunctuation = ""
    var i = 0
    
    while (i < normalizedText.length) {
        val char = normalizedText[i]
        
        if (char == ' ') {
            pendingSpaces += char
            i++
            continue
        }

        if (char != '\'' && isAttachablePunctuation(char)) {
            if (result.isNotEmpty()) {
                val lastIndex = result.lastIndex
                result[lastIndex] = result[lastIndex] + char
            } else {
                // 句首标点：缓存，附着到后一个词/字。
                pendingPrefixPunctuation += char
            }
            i++
            continue
        }
        
        if (isCJKCharacter(char)) {
            val shouldAttachToPrevious = isJapaneseSmallKana(char) &&
                    pendingSpaces.isEmpty() &&
                    pendingPrefixPunctuation.isEmpty() &&
                    result.isNotEmpty() &&
                    result.last().lastOrNull()?.let { isCJKCharacter(it) } == true

            if (shouldAttachToPrevious) {
                val lastIndex = result.lastIndex
                result[lastIndex] = result[lastIndex] + char
                i++
                continue
            }

            result.add(pendingSpaces + pendingPrefixPunctuation + char)
            pendingSpaces = ""
            pendingPrefixPunctuation = ""
            i++
        } else {
            val wordStart = i
            while (
                i < normalizedText.length &&
                normalizedText[i] != ' ' &&
                !isCJKCharacter(normalizedText[i]) &&
                (normalizedText[i] == '\'' || !isAttachablePunctuation(normalizedText[i]))
            ) {
                i++
            }
            val word = normalizedText.substring(wordStart, i)
            result.add(pendingSpaces + pendingPrefixPunctuation + word)
            pendingSpaces = ""
            pendingPrefixPunctuation = ""
        }
    }

    if (pendingPrefixPunctuation.isNotEmpty()) {
        if (result.isNotEmpty()) {
            val lastIndex = result.lastIndex
            result[lastIndex] = result[lastIndex] + pendingPrefixPunctuation
        } else {
            result.add(pendingSpaces + pendingPrefixPunctuation)
        }
    }
    
    return result
}

fun formatTime(timeMs: Long): String {
    return LyricBatchEditUtils.formatTime(timeMs)
}

fun parseTimeToMs(timeStr: String): Long {
    return LyricBatchEditUtils.parseTimeToMs(timeStr)
}

fun adjustTime(timeStr: String, shiftMs: Long): String {
    return LyricBatchEditUtils.adjustTime(timeStr, shiftMs)
}

private fun buildPreviewLyricLines(lyricLines: List<LyricLine>): List<com.example.LyricBox.NewPreviewLyricLine> {
    return lyricLines.map { line ->
        val expandedWords = if (line.timeUnits.size == 1) {
            val unit = line.timeUnits.first()
            val beginMs = parseTimeToMs(unit.startTime)
            val endMs = parseTimeToMs(unit.endTime)
            listOf(
                com.example.LyricBox.NewPreviewLyricWord(
                    text = unit.text,
                    begin = beginMs,
                    end = endMs,
                    transliteration = unit.transliteration,
                    charTransliterations = unit.charTransliterations
                )
            )
        } else {
            line.timeUnits.flatMap { unit ->
                val beginMs = parseTimeToMs(unit.startTime)
                val endMs = parseTimeToMs(unit.endTime)
                val duration = endMs - beginMs
                val text = unit.text
                if (text.isEmpty()) {
                    emptyList()
                } else if (text.length == 1) {
                    listOf(
                        com.example.LyricBox.NewPreviewLyricWord(
                            text = text,
                            begin = beginMs,
                            end = endMs,
                            transliteration = if (unit.charTransliterations.isNotEmpty()) {
                                unit.charTransliterations[0] ?: ""
                            } else {
                                ""
                            },
                            charTransliterations = emptyMap()
                        )
                    )
                } else {
                    val nonSpaceCount = text.count { it != ' ' }
                    if (nonSpaceCount == 0) {
                        text.map { char ->
                            com.example.LyricBox.NewPreviewLyricWord(
                                text = char.toString(),
                                begin = beginMs,
                                end = beginMs,
                                transliteration = "",
                                charTransliterations = emptyMap()
                            )
                        }
                    } else {
                        val charDuration = duration / nonSpaceCount
                        var currentTime = beginMs
                        var textIndex = 0
                        text.map { char ->
                            if (char == ' ') {
                                textIndex++
                                com.example.LyricBox.NewPreviewLyricWord(
                                    text = char.toString(),
                                    begin = currentTime,
                                    end = currentTime,
                                    transliteration = "",
                                    charTransliterations = emptyMap()
                                )
                            } else {
                                val charBegin = currentTime
                                val charEnd = if (currentTime + charDuration >= endMs) endMs else currentTime + charDuration
                                currentTime = charEnd
                                val transliteration = if (unit.charTransliterations.isNotEmpty()) {
                                    unit.charTransliterations[textIndex] ?: ""
                                } else {
                                    ""
                                }
                                textIndex++
                                com.example.LyricBox.NewPreviewLyricWord(
                                    text = char.toString(),
                                    begin = charBegin,
                                    end = charEnd,
                                    transliteration = transliteration,
                                    charTransliterations = emptyMap()
                                )
                            }
                        }
                    }
                }
            }
        }

        com.example.LyricBox.NewPreviewLyricLine(
            words = expandedWords,
            translation = line.translation,
            isDuet = line.agentType == LyricAgentType.RIGHT,
            isBackground = line.agentType == LyricAgentType.BACKGROUND
        )
    }
}

private fun createLyricPreviewIntent(
    context: Context,
    lyricLines: List<LyricLine>,
    sourceAudioPath: String,
    sourceMediaStoreId: Long,
    convertedAudioPath: String,
    displayTitle: String,
    currentPos: Long,
    pendingLyricsCreators: List<String>
): Intent {
    val audioPath = if (sourceAudioPath.isNotEmpty()) sourceAudioPath else convertedAudioPath
    val previewSourceAudioPath = if (sourceAudioPath.isNotEmpty()) sourceAudioPath else audioPath
    val previewLyricLines = buildPreviewLyricLines(lyricLines)
    val wordsList = previewLyricLines.flatMap { it.words }

    return Intent(context, LyricPreviewActivity::class.java).apply {
        putExtra(LyricPreviewActivity.EXTRA_AUDIO_PATH, audioPath)
        putExtra(LyricPreviewActivity.EXTRA_SOURCE_AUDIO_PATH, previewSourceAudioPath)
        putExtra(LyricPreviewActivity.EXTRA_MEDIA_STORE_ID, sourceMediaStoreId)
        putExtra(LyricPreviewActivity.EXTRA_TITLE, displayTitle)
        putExtra(LyricPreviewActivity.EXTRA_INITIAL_POSITION, currentPos)
        putExtra(LyricPreviewActivity.EXTRA_CREATORS, pendingLyricsCreators.toTypedArray())
        putExtra(
            LyricPreviewActivity.EXTRA_PREVIEW_ENTRY_SOURCE,
            LyricPreviewActivity.PREVIEW_ENTRY_SOURCE_TIMING
        )
        putExtra("line_count", previewLyricLines.size)
        putExtra("words_per_line", previewLyricLines.map { it.words.size }.toIntArray())
        putExtra("begins", wordsList.map { it.begin }.toLongArray())
        putExtra("ends", wordsList.map { it.end }.toLongArray())
        putExtra("texts", wordsList.map { it.text }.toTypedArray())
        putExtra("transliterations", wordsList.map { it.transliteration }.toTypedArray())
        val charTransliterationStrings = wordsList.map { word ->
            word.charTransliterations.entries.joinToString(";") { "${it.key}=${it.value}" }
        }.toTypedArray()
        putExtra("char_transliterations", charTransliterationStrings)
        putExtra("translations", previewLyricLines.map { it.translation }.toTypedArray())
        putExtra("is_duets", previewLyricLines.map { it.isDuet }.toBooleanArray())
        putExtra("is_backgrounds", previewLyricLines.map { it.isBackground }.toBooleanArray())
    }
}

fun formatLyricTimeline(lyricLines: List<LyricLine>): List<LyricLine> {
    return LyricBatchEditUtils.formatTimeline(lyricLines)
}

// 检测字符是否为CJK字符
fun isCjkChar(c: Char): Boolean {
    return (c.code in 0x4E00..0x9FFF) || 
           (c.code in 0x3400..0x4DBF) || 
           (c.code in 0x20000..0x2A6DF) ||
           (c.code in 0x2A700..0x2B73F) ||
           (c.code in 0x2B740..0x2B81F) ||
           (c.code in 0x2B820..0x2CEAF) ||
           (c.code in 0xF900..0xFAFF) ||
           (c.code in 0x2F800..0x2FA1F) ||
           (c.code in 0x3040..0x30FF) || // 日语平假名和片假名
           (c.code in 0x31F0..0x31FF) ||
           (c.code in 0xAC00..0xD7AF)  // 韩语Hangul
}

// 检测文本是否包含CJK字符
fun hasCjkChar(text: String): Boolean {
    return text.any { isCjkChar(it) }
}

fun mergeCloseTimeUnits(timeUnits: List<LyricTimeUnit>, thresholdMs: Long): List<LyricTimeUnit> {
    if (timeUnits.isEmpty()) return timeUnits
    
    val result = mutableListOf<LyricTimeUnit>()
    var i = 0
    
    while (i < timeUnits.size) {
        val baseUnit = timeUnits[i]
        val baseDuration = parseTimeToMs(baseUnit.endTime) - parseTimeToMs(baseUnit.startTime)
        
        var mergeEndIndex = i
        for (j in (i + 1) until timeUnits.size) {
            val nextUnit = timeUnits[j]
            val nextDuration = parseTimeToMs(nextUnit.endTime) - parseTimeToMs(nextUnit.startTime)
            
            if (kotlin.math.abs(nextDuration - baseDuration) <= thresholdMs) {
                mergeEndIndex = j
            } else {
                break
            }
        }
        
        if (mergeEndIndex > i) {
            val unitsToMerge = timeUnits.subList(i, mergeEndIndex + 1)
            val mergedText = unitsToMerge.joinToString("") { it.text }
            
            // 合并charTransliterations
            val mergedCharTransliterations = mutableMapOf<Int, String>()
            var charOffset = 0
            unitsToMerge.forEach { unit ->
                unit.charTransliterations.forEach { (idx, translit) ->
                    mergedCharTransliterations[charOffset + idx] = translit
                }
                charOffset += unit.text.length
            }
            
            // 合并transliteration，对于非CJK字符加空格
            val transliterationsToJoin = unitsToMerge.mapNotNull { 
                if (it.transliteration.isNotEmpty()) it.transliteration else null
            }
            val mergedTransliteration = if (transliterationsToJoin.isNotEmpty()) {
                val hasAnyCjk = unitsToMerge.any { hasCjkChar(it.text) }
                if (hasAnyCjk) {
                    transliterationsToJoin.joinToString("")
                } else {
                    transliterationsToJoin.joinToString(" ")
                }
            } else {
                ""
            }
            
            val mergedUnit = LyricTimeUnit(
                text = mergedText,
                startTime = timeUnits[i].startTime,
                endTime = timeUnits[mergeEndIndex].endTime,
                transliteration = mergedTransliteration,
                charTransliterations = mergedCharTransliterations.toMap()
            )
            result.add(mergedUnit)
        } else {
            result.add(baseUnit)
        }
        
        i = mergeEndIndex + 1
    }
    
    return result
}

fun filterDigits(input: String): String {
    return input.filter { it.isDigit() }
}

fun parseSPLLrcLyrics(content: String): Pair<List<String>, List<LyricLine>> {
    val lines = content.lines().filter { it.isNotBlank() }
    val lyrics = mutableListOf<String>()
    val lyricLines = mutableListOf<LyricLine>()
    
    // 辅助函数：将时间戳转换为标准的三位毫秒格式
    fun normalizeTimeTag(timeTag: String): String {
        val parts = timeTag.split(":", ".")
        if (parts.size == 3) {
            val m = parts[0]
            val s = parts[1]
            val msStr = parts[2]
            val ms = if (msStr.length == 2) msStr + "0" else msStr
            return "$m:$s.$ms"
        }
        return timeTag
    }
    
    // 先解析所有行
    val parsedLines = lines.map { line ->
        val lineLyrics = StringBuilder()
        val lineTimes = mutableListOf<LyricTimeUnit>()
        var firstTimeTag = ""
        
        val timeTagPattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
        val matches = timeTagPattern.findAll(line)
        val matchList = matches.toList()
        
        if (matchList.isEmpty()) {
            lineLyrics.append(line)
            lineTimes.add(LyricTimeUnit(line, "00:00.000", "00:00.000"))
        } else {
            firstTimeTag = normalizeTimeTag(matchList[0].value.substring(1, matchList[0].value.length - 1))
            
            if (matchList.size == 1 && matchList[0].range.first == 0) {
                val timeTag = matchList[0].value
                val startTime = normalizeTimeTag(timeTag.substring(1, timeTag.length - 1))
                val textAfterTag = line.substring(matchList[0].range.last + 1)
                
                if (textAfterTag.isNotEmpty()) {
                    lineLyrics.append(textAfterTag)
                    lineTimes.add(LyricTimeUnit(textAfterTag, startTime, "00:00.000"))
                }
            } else if (matchList.size == 1 && matchList[0].range.first > 0) {
                val textBeforeTag = line.substring(0, matchList[0].range.first)
                val timeTag = matchList[0].value
                val startTime = normalizeTimeTag(timeTag.substring(1, timeTag.length - 1))
                val textAfterTag = line.substring(matchList[0].range.last + 1)
                
                if (textBeforeTag.isNotEmpty()) {
                    lineLyrics.append(textBeforeTag)
                    lineTimes.add(LyricTimeUnit(textBeforeTag, "00:00.000", "00:00.000"))
                }
                
                if (textAfterTag.isNotEmpty()) {
                    lineLyrics.append(textAfterTag)
                    lineTimes.add(LyricTimeUnit(textAfterTag, startTime, "00:00.000"))
                }
            } else {
                var pendingSpaces = ""
                
                matchList.forEachIndexed { index, match ->
                    val timeTag = match.value
                    val startTime = normalizeTimeTag(timeTag.substring(1, timeTag.length - 1))
                    
                    val endPos = match.range.last + 1
                    val nextMatch = if (index < matchList.size - 1) matchList[index + 1] else null
                    val nextStartPos = nextMatch?.range?.first ?: line.length
                    
                    val textAfterTag = line.substring(endPos, nextStartPos)
                    
                    if (textAfterTag.isNotEmpty()) {
                        if (textAfterTag.all { it == ' ' }) {
                            pendingSpaces += textAfterTag
                        } else {
                            val fullText = pendingSpaces + textAfterTag
                            lineLyrics.append(fullText)
                            val endTime = if (nextMatch != null) {
                                normalizeTimeTag(nextMatch.value.substring(1, nextMatch.value.length - 1))
                            } else {
                                "00:00.000"
                            }
                            lineTimes.add(LyricTimeUnit(fullText, startTime, endTime))
                            pendingSpaces = ""
                        }
                    }
                }
            }
        }
        
        Triple(lineLyrics.toString(), lineTimes, firstTimeTag)
    }
    
    // 处理翻译匹配
    var i = 0
    while (i < parsedLines.size) {
        val (lineLyric, lineTimes, firstTimeTag) = parsedLines[i]
        
        val translationLines = mutableListOf<String>()
        var nextIndex = i + 1
        while (nextIndex < parsedLines.size) {
            val nextLine = parsedLines[nextIndex]
            val nextFirstTimeTag = nextLine.third
            val canUseAsTranslation = firstTimeTag.isNotEmpty() &&
                firstTimeTag == nextFirstTimeTag &&
                nextLine.second.size == 1
            if (!canUseAsTranslation) break
            translationLines.add(nextLine.first)
            nextIndex++
        }
        val translation = joinTranslationLines(translationLines)
        i = nextIndex - 1
        
        lyrics.add(lineLyric)
        lyricLines.add(LyricLine(lineTimes, translation, LyricAgentType.LEFT, ""))
        i++
    }
    
    return Pair(lyrics, lyricLines)
}

fun parseElrcLyrics(content: String): Pair<List<String>, List<LyricLine>> {
    val lines = content.lines().filter { it.isNotBlank() }
    val lyrics = mutableListOf<String>()
    val lyricLines = mutableListOf<LyricLine>()
    
    // 支持 [mm:ss.sss] 格式（三位毫秒）
    val timeTagPattern = Regex("<(\\d{2}):(\\d{2})\\.(\\d{3})>")
    val lineTimePattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{3})\\]")
    val agentPattern = Regex("^(v1|v2):\\s*")
    val bgPattern = Regex("^\\[bg:\\s*(.*?)\\]\\s*$", RegexOption.DOT_MATCHES_ALL)
    
    data class ParsedLine(
        val lineTime: String,
        val agentType: LyricAgentType,
        val timeUnits: List<LyricTimeUnit>,
        val lineText: String,
        val isTranslation: Boolean
    )
    
    val parsedLines = mutableListOf<ParsedLine>()
    
    for (line in lines) {
        var agentType = LyricAgentType.LEFT
        var processedLine = line
        
        // 检查是否是背景歌词
        val bgMatch = bgPattern.find(processedLine)
        if (bgMatch != null) {
            val bgContent = bgMatch.groupValues[1]
            processedLine = bgContent
            agentType = LyricAgentType.BACKGROUND
        }
        
        val lineTimeMatch = lineTimePattern.find(processedLine)
        val lineTime = if (lineTimeMatch != null) {
            lineTimeMatch.groupValues.let {
                String.format("%02d:%02d.%03d",
                    it[1].toIntOrNull() ?: 0,
                    it[2].toIntOrNull() ?: 0,
                    it[3].toIntOrNull() ?: 0
                )
            }
        } else {
            ""
        }
        processedLine = processedLine.replace(lineTimePattern, "")
        
        // 只有非背景歌词才检查 v1/v2
        if (agentType != LyricAgentType.BACKGROUND) {
            val agentMatch = agentPattern.find(processedLine)
            if (agentMatch != null) {
                val agent = agentMatch.groupValues[1]
                agentType = if (agent == "v1") LyricAgentType.LEFT else LyricAgentType.RIGHT
                processedLine = processedLine.removeRange(agentMatch.range)
            }
        }
        
        val timeMatches = timeTagPattern.findAll(processedLine).toList()
        
        if (timeMatches.isEmpty()) {
            val text = processedLine.trim()
            if (text.isNotEmpty()) {
                parsedLines.add(ParsedLine(
                    lineTime = lineTime,
                    agentType = agentType,
                    timeUnits = listOf(LyricTimeUnit(text, "00:00.000", "00:00.000")),
                    lineText = text,
                    isTranslation = true
                ))
            }
            continue
        }
        
        val timeUnits = mutableListOf<LyricTimeUnit>()
        val lineText = StringBuilder()
        
        for (i in timeMatches.indices) {
            val currentMatch = timeMatches[i]
            val startTime = currentMatch.groupValues.let {
                String.format("%02d:%02d.%03d",
                    it[1].toIntOrNull() ?: 0,
                    it[2].toIntOrNull() ?: 0,
                    it[3].toIntOrNull() ?: 0
                )
            }
            
            val textStart = currentMatch.range.last + 1
            val textEnd = if (i + 1 < timeMatches.size) timeMatches[i + 1].range.first else processedLine.length
            val text = processedLine.substring(textStart, textEnd)
            
            val endTime = if (i + 1 < timeMatches.size) {
                timeMatches[i + 1].groupValues.let {
                    String.format("%02d:%02d.%03d",
                        it[1].toIntOrNull() ?: 0,
                        it[2].toIntOrNull() ?: 0,
                        it[3].toIntOrNull() ?: 0
                    )
                }
            } else {
                "00:00.000"
            }
            
            if (text.isNotEmpty()) {
                lineText.append(text)
                timeUnits.add(LyricTimeUnit(text, startTime, endTime))
            }
        }
        
        if (timeUnits.isNotEmpty()) {
            parsedLines.add(ParsedLine(
                lineTime = lineTime,
                agentType = agentType,
                timeUnits = timeUnits,
                lineText = lineText.toString(),
                isTranslation = false
            ))
        }
    }
    
    var i = 0
    while (i < parsedLines.size) {
        val currentLine = parsedLines[i]
        
        if (currentLine.isTranslation) {
            i++
            continue
        }
        
        val translationLines = mutableListOf<String>()
        var nextIndex = i + 1
        while (nextIndex < parsedLines.size) {
            val nextLine = parsedLines[nextIndex]
            val canUseAsTranslation = nextLine.isTranslation &&
                currentLine.lineTime.isNotEmpty() &&
                currentLine.lineTime == nextLine.lineTime
            if (!canUseAsTranslation) break
            translationLines.add(nextLine.lineText)
            nextIndex++
        }
        val translation = joinTranslationLines(translationLines)
        i = nextIndex - 1
        
        lyrics.add(currentLine.lineText)
        lyricLines.add(LyricLine(currentLine.timeUnits, translation, currentLine.agentType, ""))
        i++
    }
    
    return Pair(lyrics, lyricLines)
}

/**
 * 导出增强LRC歌词
 * @param lyricLines 歌词行列表
 * @param showDuet 是否显示对唱标识（v1/v2）
 * @return 导出的歌词内容
 */
private fun isBlankLyricLineForExport(lyricLine: LyricLine): Boolean {
    return isBlankLyricLine(lyricLine)
}

private fun firstExportStartTime(lyricLine: LyricLine, fallback: String): String {
    return lyricLine.timeUnits.firstOrNull { it.text.isNotBlank() }?.startTime
        ?: lyricLine.timeUnits.firstOrNull()?.startTime
        ?: fallback
}

private fun splitTranslationLines(value: String): List<String> {
    return value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun joinTranslationLines(lines: List<String>): String {
    return lines
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}

private fun mergeTranslationValues(vararg values: String?): String {
    val mergedLines = mutableListOf<String>()
    values.forEach { value ->
        splitTranslationLines(value.orEmpty()).forEach { line ->
            if (line !in mergedLines) {
                mergedLines.add(line)
            }
        }
    }
    return joinTranslationLines(mergedLines)
}

fun toEnhancedLrc(lyricLines: List<LyricLine>, showDuet: Boolean): String {
    val sb = StringBuilder()
    var previousLineStartTime = "00:00.000"
    
    for (lyricLine in lyricLines) {
        if (isBlankLyricLineForExport(lyricLine)) {
            sb.append("[$previousLineStartTime]\n")
            continue
        }

        val timeUnits = lyricLine.timeUnits
        val lineTime = firstExportStartTime(lyricLine, previousLineStartTime)
        previousLineStartTime = lineTime
        
        if (lyricLine.agentType == LyricAgentType.BACKGROUND) {
            if (showDuet) {
                sb.append("[bg: ")
            } else {
                sb.append("[$lineTime]")
            }
            for (unit in timeUnits) {
                sb.append("<${unit.startTime}>")
                sb.append(unit.text)
            }
            sb.append("<${timeUnits.last().endTime}>")
            if (showDuet) {
                sb.append("]")
            }
            sb.append("\n")
        } else {
            sb.append("[$lineTime]")
            if (showDuet) {
                when (lyricLine.agentType) {
                    LyricAgentType.LEFT -> sb.append("v1: ")
                    LyricAgentType.RIGHT -> sb.append("v2: ")
                    else -> {}
                }
            }
            for (unit in timeUnits) {
                sb.append("<${unit.startTime}>")
                sb.append(unit.text)
            }
            sb.append("<${timeUnits.last().endTime}>")
            sb.append("\n")
        }
        
        val translationLines = splitTranslationLines(lyricLine.translation)
        if (translationLines.isNotEmpty()) {
            translationLines.forEach { translationLine ->
                sb.append("[$lineTime]$translationLine\n")
            }
        }
    }
    
    return sb.toString()
}

fun parseTtmlTime(timeStr: String): String {
    val timeStr = timeStr.trim()
    if (timeStr.contains(":")) {
        val parts = if (timeStr.contains(".")) {
            timeStr.split(":", ".")
        } else {
            timeStr.split(":")
        }
        if (parts.size >= 2) {
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts[1].split(".").first().toDoubleOrNull()?.toInt() ?: 0
            val ms = if (parts.size >= 3) {
                (parts[2].toDoubleOrNull() ?: 0.0).toInt()
            } else if (parts[1].contains(".")) {
                val secParts = parts[1].split(".")
                if (secParts.size > 1) {
                    ((secParts[1].toDoubleOrNull() ?: 0.0) * 10).toInt()
                } else 0
            } else 0
            return String.format("%02d:%02d.%03d", minutes, seconds, ms)
        }
    } else {
        val totalSeconds = timeStr.toDoubleOrNull() ?: 0.0
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        val ms = ((totalSeconds * 1000) % 1000).toInt()
        return String.format("%02d:%02d.%03d", minutes, seconds, ms)
    }
    return "00:00.000"
}

fun parseSongwritersFromTtml(content: String): List<String> {
    val songwriters = mutableListOf<String>()
    
    Log.d("LyricTiming", "parseSongwritersFromTtml: 开始解析，内容长度=${content.length}")
    
    // 检查内容中是否包含 songwriters 关键字
    val containsSongwriters = content.contains("songwriters", ignoreCase = false)
    val containsSongwriter = content.contains("songwriter", ignoreCase = false)
    Log.d("LyricTiming", "内容中是否包含 'songwriters': $containsSongwriters")
    Log.d("LyricTiming", "内容中是否包含 'songwriter': $containsSongwriter")
    
    // 如果包含，打印相关部分的内容
    if (containsSongwriters || containsSongwriter) {
        val songwritersIndex = content.indexOf("songwriters")
        if (songwritersIndex >= 0) {
            val startIndex = maxOf(0, songwritersIndex - 50)
            val endIndex = minOf(content.length, songwritersIndex + 200)
            Log.d("LyricTiming", "songwriters 附近的内容: ${content.substring(startIndex, endIndex)}")
        }
    }
    
    // 直接在整个内容中查找 songwriters 标签
    val songwritersPattern = Regex("<songwriters>(.*?)</songwriters>", RegexOption.DOT_MATCHES_ALL)
    val songwritersMatch = songwritersPattern.find(content)
    
    Log.d("LyricTiming", "SongwritersMatch: ${songwritersMatch != null}")
    
    if (songwritersMatch != null) {
        val songwritersContent = songwritersMatch.groupValues[1]
        Log.d("LyricTiming", "SongwritersContent: $songwritersContent")
        
        // 找所有songwriter标签
        val songwriterPattern = Regex("<songwriter>(.*?)</songwriter>", RegexOption.DOT_MATCHES_ALL)
        val songwriterMatches = songwriterPattern.findAll(songwritersContent)
        
        for (match in songwriterMatches) {
            val creator = match.groupValues[1].trim()
            if (creator.isNotEmpty()) {
                songwriters.add(creator)
                Log.d("LyricTiming", "找到创作者: $creator")
            }
        }
    }
    
    Log.d("LyricTiming", "最终找到的创作者数量: ${songwriters.size}, 内容: $songwriters")
    
    return songwriters
}

fun parseTtmlLyrics(content: String): List<LyricLine> {
    val mainTranslations = mutableMapOf<String, String>()
    val bgTranslations = mutableMapOf<String, String>()
    val transliterations = mutableMapOf<String, List<Triple<String, String, String>>>() // key: lineKey, value: list of (begin, end, text)
    val bgTransliterations = mutableMapOf<String, List<Triple<String, String, String>>>() // key: lineKey, value: list of (begin, end, text) for background
    
    // 先在原始内容中解析翻译标签和注音标签，避免预处理破坏结构
    val translationPattern = Regex("<translation([^>]*)>(.*?)</translation>", RegexOption.DOT_MATCHES_ALL)
    val translationMatch = translationPattern.find(content)
    
    // 解析注音标签
    val transliterationPattern = Regex("<transliteration([^>]*)>(.*?)</transliteration>", RegexOption.DOT_MATCHES_ALL)
    val transliterationMatch = transliterationPattern.find(content)
    if (transliterationMatch != null) {
        val transliterationContent = transliterationMatch.groupValues[2]
        val textPattern = Regex("<text for=\"([^\"]+)\">(.*?)</text>", RegexOption.DOT_MATCHES_ALL)
        val textMatches = textPattern.findAll(transliterationContent).toList()
        
        for (textMatch in textMatches) {
            val key = textMatch.groupValues[1]
            var fullText = textMatch.groupValues[2]
            
            // 分离主注音和背景注音
            val bgStartPattern = Regex("""<span[^>]*ttm:role="x-bg"[^>]*>""")
            val bgStartMatch = bgStartPattern.find(fullText)
            
            if (bgStartMatch != null) {
                // 有背景注音，需要分离出来
                val bgStartPos = bgStartMatch.range.first
                val afterBgStart = fullText.substring(bgStartMatch.range.last + 1)
                
                val closeSpanPattern = Regex("</span>")
                var depth = 1
                var searchPos = 0
                var bgTransText = ""
                
                while (searchPos < afterBgStart.length && depth > 0) {
                    val remaining = afterBgStart.substring(searchPos)
                    val nextOpen = Regex("<span[^>]*>").find(remaining)
                    val nextClose = closeSpanPattern.find(remaining)
                    
                    val openRelPos = nextOpen?.range?.first ?: Int.MAX_VALUE
                    val closeRelPos = nextClose?.range?.first ?: Int.MAX_VALUE
                    
                    if (closeRelPos < openRelPos) {
                        depth--
                        if (depth == 0) {
                            bgTransText = afterBgStart.substring(0, searchPos + closeRelPos)
                            fullText = fullText.substring(0, bgStartPos) + 
                                       afterBgStart.substring(searchPos + nextClose!!.range.last + 1)
                        } else {
                            searchPos += nextClose!!.range.last + 1
                        }
                    } else if (openRelPos < closeRelPos) {
                        depth++
                        searchPos += nextOpen!!.range.last + 1
                    } else {
                        break
                    }
                }
                
                // 解析背景注音
                val bgSpanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
                val bgSpanMatches = bgSpanPattern.findAll(bgTransText).toList()
                
                val bgSpans = mutableListOf<Triple<String, String, String>>()
                for (spanMatch in bgSpanMatches) {
                    val spanTag = spanMatch.groups[0]!!.value.substringBefore(">")
                    var spanContent = spanMatch.groups[1]!!.value
                    
                    val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                    val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                    
                    val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                    val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                    
                    // 去掉括号
                    spanContent = spanContent.removePrefix("(").removeSuffix(")")
                    
                    bgSpans.add(Triple(beginTime, endTime, spanContent))
                }
                
                if (bgSpans.isNotEmpty()) {
                    bgTransliterations[key] = bgSpans
                }
            }
            
            // 解析主注音
            val spanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
            val spanMatches = spanPattern.findAll(fullText).toList()
            
            val spans = mutableListOf<Triple<String, String, String>>()
            for (spanMatch in spanMatches) {
                val spanTag = spanMatch.groups[0]!!.value.substringBefore(">")
                val spanContent = spanMatch.groups[1]!!.value
                
                val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                
                val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                
                spans.add(Triple(beginTime, endTime, spanContent))
            }
            
            if (spans.isNotEmpty()) {
                transliterations[key] = spans
            }
        }
    }
    if (translationMatch != null) {
        val translationAttrs = translationMatch.groupValues[1]
        val translationContent = translationMatch.groupValues[2]
        val isReplacementType = translationAttrs.contains("type=\"replacement\"")
        
        val textPattern = Regex("<text for=\"([^\"]+)\">(.*?)</text>", RegexOption.DOT_MATCHES_ALL)
        val textMatches = textPattern.findAll(translationContent).toList()
        
        for (textMatch in textMatches) {
            val key = textMatch.groupValues[1]
            val fullText = textMatch.groupValues[2]
            
            // 检测是否包含带时间戳的 span 标签（有 begin 属性）
            val hasTimestampSpans = fullText.contains(Regex("""<span[^>]*begin="[^"]+""""))
            
            if (isReplacementType || hasTimestampSpans) {
                // type="replacement" 格式或包含时间戳 span：直接移除所有 XML 标签，保留纯文本和空格
                val plainText = fullText.replace(Regex("<[^>]+>"), "").trim()
                mainTranslations[key] = plainText
            } else {
                // 普通格式
                // 提取主翻译（去掉span标签及其内容）
                val bgSpanPattern = Regex("<span[^>]*ttm:role=\"x-bg\"[^>]*>.*?</span>", RegexOption.DOT_MATCHES_ALL)
                val mainText = bgSpanPattern.replace(fullText, "").trim()
                mainTranslations[key] = mainText
                
                // 提取背景翻译
                val bgSpanContentPattern = Regex("<span[^>]*ttm:role=\"x-bg\"[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
                val bgSpanMatch = bgSpanContentPattern.find(fullText)
                if (bgSpanMatch != null) {
                    val bgText = bgSpanMatch.groupValues[1].trim().removePrefix("(").removeSuffix(")")
                    bgTranslations[key] = bgText
                }
            }
        }
    }
    
    // 处理歌词内容时才进行预处理
    var processedContent = content
        .replace("\n", "")
        .replace("\r", "")
        .replace("  ", "")
    
    val lyricLines = mutableListOf<LyricLine>()
    
    val pPattern = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
    val pMatches = pPattern.findAll(processedContent).toList()
    
    val uniqueAgents = mutableSetOf<String>()
    for (pMatch in pMatches) {
        val pTag = pMatch.groups[0]!!.value
        val agentMatch = Regex("""ttm:agent="([^"]+)"""").find(pTag)
        if (agentMatch != null) {
            uniqueAgents.add(agentMatch.groupValues[1])
        }
    }
    
    val agentList = uniqueAgents.sorted()
    
    for (pMatch in pMatches) {
        val pTag = pMatch.groups[0]!!.value
        var pContent = pMatch.groups[1]!!.value
        
        var agentType = LyricAgentType.LEFT
        
        val agentMatch = Regex("""ttm:agent="([^"]+)"""").find(pTag)
        if (agentMatch != null) {
            val agent = agentMatch.groupValues[1]
            if (agentList.size == 2) {
                if (agent == agentList[1]) {
                    agentType = LyricAgentType.RIGHT
                } else {
                    agentType = LyricAgentType.LEFT
                }
            } else {
                if (agent == "v2") {
                    agentType = LyricAgentType.RIGHT
                } else {
                    agentType = LyricAgentType.LEFT
                }
            }
        }
        
        var lineKey = ""
        val keyMatch = Regex("""itunes:key="([^"]+)"""").find(pTag)
        if (keyMatch != null) {
            lineKey = keyMatch.groupValues[1]
        }
        
        val beginMatchP = Regex("""begin="([^"]+)"""").find(pTag)
        val endMatchP = Regex("""end="([^"]+)"""").find(pTag)
        val beginTimeP = if (beginMatchP != null) parseTtmlTime(beginMatchP.groupValues[1]) else "00:00.000"
        val endTimeP = if (endMatchP != null) parseTtmlTime(endMatchP.groupValues[1]) else "00:00.000"
        
        var mainTranslation = ""
        var bgTranslation = ""
        val mainTimeUnits = mutableListOf<LyricTimeUnit>()
        val bgTimeUnits = mutableListOf<LyricTimeUnit>()
        
        val bgMarker = "___BG_CONTENT_MARKER___"
        var bgContent = ""
        
        val bgStartPattern = Regex("""<span[^>]*ttm:role="x-bg"[^>]*>""")
        val bgStartMatch = bgStartPattern.find(pContent)
        
        if (bgStartMatch != null) {
            val bgStartPos = bgStartMatch.range.first
            val afterBgStart = pContent.substring(bgStartMatch.range.last + 1)
            
            val closeSpanPattern = Regex("</span>")
            var depth = 1
            var searchPos = 0
            
            while (searchPos < afterBgStart.length && depth > 0) {
                val remaining = afterBgStart.substring(searchPos)
                val nextOpen = Regex("<span[^>]*>").find(remaining)
                val nextClose = closeSpanPattern.find(remaining)
                
                val openRelPos = nextOpen?.range?.first ?: Int.MAX_VALUE
                val closeRelPos = nextClose?.range?.first ?: Int.MAX_VALUE
                
                if (closeRelPos < openRelPos) {
                    depth--
                    if (depth == 0) {
                        bgContent = afterBgStart.substring(0, searchPos + closeRelPos)
                        pContent = pContent.substring(0, bgStartPos) + bgMarker + 
                                   afterBgStart.substring(searchPos + nextClose!!.range.last + 1)
                    } else {
                        searchPos += nextClose!!.range.last + 1
                    }
                } else if (openRelPos < closeRelPos) {
                    depth++
                    searchPos += nextOpen!!.range.last + 1
                } else {
                    break
                }
            }
        }
        
        val mainTranslationMatch = Regex("""<span[^>]*ttm:role="x-translation"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
            .find(pContent)
        val mainRomanMatch = Regex("""<span[^>]*ttm:role="x-roman"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
            .find(pContent)
        mainTranslation = mergeTranslationValues(
            mainTranslationMatch?.groupValues?.get(1)?.trim(),
            mainRomanMatch?.groupValues?.get(1)?.trim()
        )
        
        val mainContentWithoutTrans = pContent
            .replace(Regex("""<span[^>]*ttm:role="x-translation"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""<span[^>]*ttm:role="x-roman"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(bgMarker, "")
        
        val mainSpanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
        val mainSpanMatches = mainSpanPattern.findAll(mainContentWithoutTrans).toList()
        
        val spanFullPattern = Regex("<span[^>]*>.*?</span>", RegexOption.DOT_MATCHES_ALL)
        val spanFullMatches = spanFullPattern.findAll(mainContentWithoutTrans).toList()
        
        if (mainSpanMatches.isNotEmpty()) {
            var pendingSpaces = ""
            
            for ((idx, spanMatch) in mainSpanMatches.withIndex()) {
                val spanFullTag = spanFullMatches[idx].groups[0]!!.value
                val spanTag = spanFullTag.substringBefore(">")
                val spanContent = spanMatch.groups[1]!!.value
                
                val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                
                val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                
                if (spanContent.isNotEmpty()) {
                    val text = pendingSpaces + spanContent
                    mainTimeUnits.add(LyricTimeUnit(text, beginTime, endTime))
                    pendingSpaces = ""
                }
                
                if (idx < spanFullMatches.size - 1) {
                    val currentEnd = spanFullMatches[idx].range.last + 1
                    val nextStart = spanFullMatches[idx + 1].range.first
                    if (nextStart > currentEnd) {
                        val betweenText = mainContentWithoutTrans.substring(currentEnd, nextStart)
                        if (betweenText.all { it == ' ' }) {
                            pendingSpaces = betweenText
                        }
                    }
                }
            }
        } else {
            val plainText = mainContentWithoutTrans.trim()
            if (plainText.isNotEmpty()) {
                mainTimeUnits.add(LyricTimeUnit(plainText, beginTimeP, endTimeP))
            }
        }
        
        // 为当前行的歌词单元分配注音
        val lineTransliterations = transliterations[lineKey]
        if (lineTransliterations != null) {
            val updatedUnits = mutableListOf<LyricTimeUnit>()
            for (unit in mainTimeUnits) {
                val unitBeginMs = parseTimeToMs(unit.startTime)
                val unitEndMs = parseTimeToMs(unit.endTime)
                
                // 查找时间匹配的注音
                var matchedTransliteration = ""
                val charTransliterations = mutableMapOf<Int, String>()
                val matchingTransliterations = mutableListOf<Triple<String, String, String>>()
                
                // 收集所有与这个单元时间匹配的注音
                for (transInfo in lineTransliterations) {
                    val (transBegin, transEnd, transText) = transInfo
                    val transBeginMs = parseTimeToMs(transBegin)
                    val transEndMs = parseTimeToMs(transEnd)
                    
                    // 检查时间重叠
                    if (transBeginMs < unitEndMs && transEndMs > unitBeginMs) {
                        matchingTransliterations.add(transInfo)
                    }
                }
                
                // 统计CJK字符数量
                val cjkIndices = unit.text.mapIndexedNotNull { idx, char -> 
                    if (isCJKCharacter(char)) idx else null 
                }
                
                // 尝试将注音分配到单字符
                if (cjkIndices.isNotEmpty() && matchingTransliterations.isNotEmpty()) {
                    // 检查注音数量是否与CJK字符数量匹配
                    // 或者我们可以按字符位置分配
                    var transIndex = 0
                    for (cjkIdx in cjkIndices) {
                        if (transIndex < matchingTransliterations.size) {
                            val (_, _, transText) = matchingTransliterations[transIndex]
                            if (transText.isNotEmpty()) {
                                charTransliterations[cjkIdx] = transText
                            }
                            transIndex++
                        } else {
                            break
                        }
                    }
                    
                    // 如果没有成功分配到单字符，使用整体注音
                    if (charTransliterations.isEmpty() && matchingTransliterations.isNotEmpty()) {
                        matchedTransliteration = matchingTransliterations[0].third
                    }
                } else if (matchingTransliterations.isNotEmpty()) {
                    // 没有CJK字符，使用整体注音
                    matchedTransliteration = matchingTransliterations[0].third
                }
                
                updatedUnits.add(unit.copy(transliteration = matchedTransliteration, charTransliterations = charTransliterations))
            }
            mainTimeUnits.clear()
            mainTimeUnits.addAll(updatedUnits)
        }
        
        if (bgContent.isNotEmpty()) {
            val bgTranslationMatch = Regex("""<span[^>]*ttm:role="x-translation"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
                .find(bgContent)
            val bgRomanMatch = Regex("""<span[^>]*ttm:role="x-roman"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
                .find(bgContent)
            bgTranslation = mergeTranslationValues(
                bgTranslationMatch?.groupValues?.get(1)?.trim(),
                bgRomanMatch?.groupValues?.get(1)?.trim()
            )
            
            val bgContentWithoutTrans = bgContent
                .replace(Regex("""<span[^>]*ttm:role="x-translation"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("""<span[^>]*ttm:role="x-roman"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
            
            val bgSpanMatches = mainSpanPattern.findAll(bgContentWithoutTrans).toList()
            val bgSpanFullMatches = spanFullPattern.findAll(bgContentWithoutTrans).toList()
            
            if (bgSpanMatches.isNotEmpty()) {
                var pendingSpaces = ""
                
                for ((idx, spanMatch) in bgSpanMatches.withIndex()) {
                    val spanFullTag = bgSpanFullMatches[idx].groups[0]!!.value
                    val spanTag = spanFullTag.substringBefore(">")
                    var spanContent = spanMatch.groups[1]!!.value
                    
                    val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                    val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                    
                    val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                    val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                    
                    spanContent = spanContent.removePrefix("(").removeSuffix(")")
                    
                    if (spanContent.isNotEmpty()) {
                        val text = pendingSpaces + spanContent
                        bgTimeUnits.add(LyricTimeUnit(text, beginTime, endTime))
                        pendingSpaces = ""
                    }
                    
                    if (idx < bgSpanFullMatches.size - 1) {
                        val currentEnd = bgSpanFullMatches[idx].range.last + 1
                        val nextStart = bgSpanFullMatches[idx + 1].range.first
                        if (nextStart > currentEnd) {
                            val betweenText = bgContentWithoutTrans.substring(currentEnd, nextStart)
                            if (betweenText.all { it == ' ' }) {
                                pendingSpaces = betweenText
                            }
                        }
                    }
                }
            }
            
            // 为当前行的背景歌词单元分配注音
            val lineBgTransliterations = bgTransliterations[lineKey]
            if (lineBgTransliterations != null) {
                val updatedBgUnits = mutableListOf<LyricTimeUnit>()
                for (unit in bgTimeUnits) {
                    val unitBeginMs = parseTimeToMs(unit.startTime)
                    val unitEndMs = parseTimeToMs(unit.endTime)
                    
                    var matchedTransliteration = ""
                    val charTransliterations = mutableMapOf<Int, String>()
                    val matchingTransliterations = mutableListOf<Triple<String, String, String>>()
                    
                    for (transInfo in lineBgTransliterations) {
                        val (transBegin, transEnd, transText) = transInfo
                        val transBeginMs = parseTimeToMs(transBegin)
                        val transEndMs = parseTimeToMs(transEnd)
                        
                        if (transBeginMs < unitEndMs && transEndMs > unitBeginMs) {
                            matchingTransliterations.add(transInfo)
                        }
                    }
                    
                    val cjkIndices = unit.text.mapIndexedNotNull { idx, char ->
                        if (isCJKCharacter(char)) idx else null
                    }
                    
                    if (cjkIndices.isNotEmpty() && matchingTransliterations.isNotEmpty()) {
                        var transIndex = 0
                        for (cjkIdx in cjkIndices) {
                            if (transIndex < matchingTransliterations.size) {
                                val (_, _, transText) = matchingTransliterations[transIndex]
                                if (transText.isNotEmpty()) {
                                    charTransliterations[cjkIdx] = transText
                                }
                                transIndex++
                            } else {
                                break
                            }
                        }
                        
                        if (charTransliterations.isEmpty() && matchingTransliterations.isNotEmpty()) {
                            matchedTransliteration = matchingTransliterations[0].third
                        }
                    } else if (matchingTransliterations.isNotEmpty()) {
                        matchedTransliteration = matchingTransliterations[0].third
                    }
                    
                    updatedBgUnits.add(unit.copy(transliteration = matchedTransliteration, charTransliterations = charTransliterations))
                }
                bgTimeUnits.clear()
                bgTimeUnits.addAll(updatedBgUnits)
            }
        }
        
        if (mainTimeUnits.isNotEmpty()) {
            // 优先使用从顶部翻译标签解析出的翻译
            val finalMainTranslation = mergeTranslationValues(mainTranslations[lineKey], mainTranslation)
            lyricLines.add(LyricLine(
                mainTimeUnits,
                finalMainTranslation,
                agentType,
                lineKey
            ))
        }
        
        if (bgTimeUnits.isNotEmpty()) {
            // 优先使用从顶部翻译标签解析出的背景翻译
            val finalBgTranslation = mergeTranslationValues(bgTranslations[lineKey], bgTranslation)
            lyricLines.add(LyricLine(
                bgTimeUnits,
                finalBgTranslation,
                LyricAgentType.BACKGROUND,
                lineKey
            ))
        }
    }
    
    return lyricLines
}

fun parseTimeToSeconds(timeStr: String): Double {
    val parts = timeStr.split(":", ".")
    return if (parts.size == 3) {
        val minutes = parts[0].toDoubleOrNull() ?: 0.0
        val seconds = parts[1].toDoubleOrNull() ?: 0.0
        val ms = parts[2].toDoubleOrNull() ?: 0.0
        minutes * 60 + seconds + ms / 1000
    } else if (parts.size == 2) {
        val minutes = parts[0].toDoubleOrNull() ?: 0.0
        val seconds = parts[1].toDoubleOrNull() ?: 0.0
        minutes * 60 + seconds
    } else {
        0.0
    }
}

fun parseSpanContent(content: String): List<LyricTimeUnit> {
    val timeUnits = mutableListOf<LyricTimeUnit>()
    val spanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
    val spanMatches = spanPattern.findAll(content).toList()
    
    for (spanMatch in spanMatches) {
        val spanTag = spanMatch.groups[0]!!.value.substringBefore(">")
        val spanContent = spanMatch.groups[1]!!.value
        
        val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
        val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
        
        val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
        val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
        
        if (spanContent.isNotEmpty()) {
            timeUnits.add(LyricTimeUnit(spanContent, beginTime, endTime))
        }
    }
    
    if (timeUnits.isEmpty()) {
        val text = content.replace(Regex("<[^>]+>"), "").trim()
        if (text.isNotEmpty()) {
            timeUnits.add(LyricTimeUnit(text, "00:00.000", "00:00.000"))
        }
    }
    
    return timeUnits
}

fun formatTtmlTime(timeStr: String): String {
    val parts = timeStr.split(":", ".")
    return if (parts.size == 3) {
        val minutes = parts[0].toIntOrNull() ?: 0
        val seconds = parts[1].toIntOrNull() ?: 0
        val ms = parts[2].toIntOrNull() ?: 0
        String.format("%d:%02d.%03d", minutes, seconds, ms)
    } else {
        timeStr
    }
}

fun buildTtmlContent(lyricLines: List<LyricLine>, creators: List<String> = emptyList()): String {
    val sb = StringBuilder()
    val transliterationsSb = StringBuilder()
    
    val hasV2Agent = lyricLines.any { !isBlankLyricLineForExport(it) && it.agentType == LyricAgentType.RIGHT }
    
    sb.appendLine("<?xml version='1.0' encoding='utf-8'?>")
    sb.appendLine("<tt xmlns=\"http://www.w3.org/ns/ttml\" xmlns:ttm=\"http://www.w3.org/ns/ttml#metadata\" xmlns:tts=\"http://www.w3.org/ns/ttml#styling\" xmlns:amll=\"http://www.example.com/ns/amll\" xmlns:itunes=\"http://music.apple.com/lyric-ttml-internal\" itunes:timing=\"Word\">")
    sb.appendLine("  <head>")
    sb.appendLine("    <metadata>")
    sb.appendLine("      <ttm:agent type=\"person\" xml:id=\"v1\"/>")
    if (hasV2Agent) {
        sb.appendLine("      <ttm:agent type=\"person\" xml:id=\"v2\"/>")
    }
    sb.appendLine("      <iTunesMetadata xmlns=\"http://music.apple.com/lyric-ttml-internal\">")
    
    // 添加创作者信息
    val validCreators = creators.filter { it.isNotBlank() }
    if (validCreators.isNotEmpty()) {
        sb.appendLine("        <songwriters>")
        for (creator in validCreators) {
            val escapedCreator = creator
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            sb.appendLine("          <songwriter>$escapedCreator</songwriter>")
        }
        sb.appendLine("        </songwriters>")
    }
    
    // 先收集注音信息
    var lineCounter = 1
    var i = 0
    while (i < lyricLines.size) {
        val lyricLine = lyricLines[i]
        
        if (lyricLine.agentType == LyricAgentType.BACKGROUND && lyricLine.timeUnits.isNotEmpty()) {
            i++
            continue
        }
        
        if (isBlankLyricLineForExport(lyricLine)) {
            i++
            continue
        }
        
        val lineKey = "L$lineCounter"
        
        // 检查是否有背景歌词
        var bgLyricLine: LyricLine? = null
        if (i + 1 < lyricLines.size &&
            lyricLines[i + 1].agentType == LyricAgentType.BACKGROUND &&
            !isBlankLyricLineForExport(lyricLines[i + 1])
        ) {
            bgLyricLine = lyricLines[i + 1]
        }
        
        // 收集当前行的主注音和背景注音
        var hasMainTransliteration = false
        var hasBgTransliteration = false
        val mainTransSb = StringBuilder()
        val bgTransSb = StringBuilder()
        
        // 收集主注音
        if (lyricLine.agentType != LyricAgentType.BACKGROUND) {
            hasMainTransliteration = lyricLine.timeUnits.any { it.transliteration.isNotEmpty() || it.charTransliterations.isNotEmpty() }
            if (hasMainTransliteration) {
                for (timeUnit in lyricLine.timeUnits) {
                    val begin = formatTtmlTime(timeUnit.startTime)
                    val end = formatTtmlTime(timeUnit.endTime)
                    
                    var transliterationText = timeUnit.transliteration
                    
                    if (timeUnit.charTransliterations.isNotEmpty()) {
                        val charCount = timeUnit.text.length
                        if (charCount > 0) {
                            val unitBeginMs = parseTimeToMs(timeUnit.startTime)
                            val unitEndMs = parseTimeToMs(timeUnit.endTime)
                            val unitDuration = unitEndMs - unitBeginMs
                            val charDuration = unitDuration / charCount
                            
                            var charIndex = 0
                            while (charIndex < charCount) {
                                val charTrans = timeUnit.charTransliterations[charIndex]
                                if (charTrans != null && charTrans.isNotEmpty()) {
                                    val charBegin = unitBeginMs + charIndex * charDuration
                                    val charEnd = if (charIndex == charCount - 1) unitEndMs else charBegin + charDuration
                                    val charBeginStr = formatTime(charBegin)
                                    val charEndStr = formatTime(charEnd)
                                    val escapedCharTrans = charTrans
                                        .replace("&", "&amp;")
                                        .replace("<", "&lt;")
                                        .replace(">", "&gt;")
                                    mainTransSb.append("<span begin=\"$charBeginStr\" end=\"$charEndStr\">$escapedCharTrans</span>")
                                }
                                charIndex++
                            }
                        }
                    } else if (transliterationText.isNotEmpty()) {
                        val escapedTrans = transliterationText
                            .replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                        mainTransSb.append("<span begin=\"$begin\" end=\"$end\">$escapedTrans</span>")
                    }
                }
            }
        }
        
        // 收集背景注音
        if (bgLyricLine != null && bgLyricLine.timeUnits.isNotEmpty()) {
            hasBgTransliteration = bgLyricLine.timeUnits.any { it.transliteration.isNotEmpty() || it.charTransliterations.isNotEmpty() }
            if (hasBgTransliteration) {
                // 首先收集所有有注音的span信息
                val allTransliterationSpans = mutableListOf<Triple<String, String, String>>() // (begin, end, text)
                
                for (timeUnit in bgLyricLine.timeUnits) {
                    val begin = formatTtmlTime(timeUnit.startTime)
                    val end = formatTtmlTime(timeUnit.endTime)
                    
                    var transliterationText = timeUnit.transliteration
                    
                    if (timeUnit.charTransliterations.isNotEmpty()) {
                        val charCount = timeUnit.text.length
                        if (charCount > 0) {
                            val unitBeginMs = parseTimeToMs(timeUnit.startTime)
                            val unitEndMs = parseTimeToMs(timeUnit.endTime)
                            val unitDuration = unitEndMs - unitBeginMs
                            val charDuration = unitDuration / charCount
                            
                            var charIndex = 0
                            while (charIndex < charCount) {
                                val charTrans = timeUnit.charTransliterations[charIndex]
                                if (charTrans != null && charTrans.isNotEmpty()) {
                                    val charBegin = unitBeginMs + charIndex * charDuration
                                    val charEnd = if (charIndex == charCount - 1) unitEndMs else charBegin + charDuration
                                    val charBeginStr = formatTime(charBegin)
                                    val charEndStr = formatTime(charEnd)
                                    
                                    allTransliterationSpans.add(Triple(charBeginStr, charEndStr, charTrans))
                                }
                                charIndex++
                            }
                        }
                    } else if (transliterationText.isNotEmpty()) {
                        allTransliterationSpans.add(Triple(begin, end, transliterationText))
                    }
                }
                
                // 现在处理收集到的span，添加括号
                if (allTransliterationSpans.isNotEmpty()) {
                    for ((spanIndex, spanInfo) in allTransliterationSpans.withIndex()) {
                        val (spanBegin, spanEnd, spanText) = spanInfo
                        
                        val transText = if (spanIndex == 0 && allTransliterationSpans.size == 1) {
                            // 只有一个span，直接包裹括号
                            "($spanText)"
                        } else if (spanIndex == 0) {
                            // 第一个span，添加左括号
                            "($spanText"
                        } else if (spanIndex == allTransliterationSpans.size - 1) {
                            // 最后一个span，添加右括号
                            "$spanText)"
                        } else {
                            // 中间的span，保持原样
                            spanText
                        }
                        
                        val escapedTrans = transText
                            .replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                        
                        bgTransSb.append("<span begin=\"$spanBegin\" end=\"$spanEnd\">$escapedTrans</span>")
                    }
                }
            }
        }
        
        // 如果有主注音或背景注音，都添加到transliterations
        if (hasMainTransliteration || hasBgTransliteration) {
            transliterationsSb.append("        <text for=\"$lineKey\">")
            if (hasMainTransliteration) {
                transliterationsSb.append(mainTransSb)
            }
            if (hasBgTransliteration) {
                transliterationsSb.append("<span ttm:role=\"x-bg\">")
                transliterationsSb.append(bgTransSb)
                transliterationsSb.append("</span>")
            }
            transliterationsSb.appendLine("</text>")
        }
        
        lineCounter++
        i++
    }
    
    // 添加注音到iTunesMetadata
    if (transliterationsSb.isNotEmpty()) {
        sb.appendLine("        <transliterations>")
        sb.appendLine("          <transliteration>")
        sb.append(transliterationsSb)
        sb.appendLine("          </transliteration>")
        sb.appendLine("        </transliterations>")
    }
    
    sb.appendLine("      </iTunesMetadata>")
    sb.appendLine("    </metadata>")
    sb.appendLine("  </head>")
    sb.appendLine("  <body>")
    sb.appendLine("    <div>")
    
    // 现在添加歌词内容
    lineCounter = 1
    i = 0
    while (i < lyricLines.size) {
        val lyricLine = lyricLines[i]
        
        if (lyricLine.agentType == LyricAgentType.BACKGROUND && lyricLine.timeUnits.isNotEmpty()) {
            i++
            continue
        }
        
        if (isBlankLyricLineForExport(lyricLine)) {
            i++
            continue
        }
        
        val lineKey = "L$lineCounter"
        
        val agentAttr = when (lyricLine.agentType) {
            LyricAgentType.LEFT -> "ttm:agent=\"v1\""
            LyricAgentType.RIGHT -> "ttm:agent=\"v2\""
            LyricAgentType.BACKGROUND -> "ttm:agent=\"v1\""
        }
        
        var firstBegin = formatTtmlTime(lyricLine.timeUnits.first().startTime)
        var lastEnd = formatTtmlTime(lyricLine.timeUnits.last().endTime)
        
        var nextLineIsBackground = false
        if (i + 1 < lyricLines.size) {
            val nextLine = lyricLines[i + 1]
            if (nextLine.agentType == LyricAgentType.BACKGROUND && !isBlankLyricLineForExport(nextLine)) {
                nextLineIsBackground = true
                
                val bgFirstBegin = formatTtmlTime(nextLine.timeUnits.first().startTime)
                val bgLastEnd = formatTtmlTime(nextLine.timeUnits.last().endTime)
                
                val mainBeginMs = parseTimeToMs(lyricLine.timeUnits.first().startTime)
                val bgBeginMs = parseTimeToMs(nextLine.timeUnits.first().startTime)
                if (bgBeginMs < mainBeginMs) {
                    firstBegin = bgFirstBegin
                }
                
                val mainEndMs = parseTimeToMs(lyricLine.timeUnits.last().endTime)
                val bgEndMs = parseTimeToMs(nextLine.timeUnits.last().endTime)
                if (bgEndMs > mainEndMs) {
                    lastEnd = bgLastEnd
                }
            }
        }
        
        sb.append("      <p begin=\"$firstBegin\" end=\"$lastEnd\" itunes:key=\"$lineKey\" $agentAttr>")
        
        if (lyricLine.timeUnits.size == 1) {
            val timeUnit = lyricLine.timeUnits.first()
            val escapedText = timeUnit.text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            sb.append(escapedText)
        } else {
            for (timeUnit in lyricLine.timeUnits) {
                val begin = formatTtmlTime(timeUnit.startTime)
                val end = formatTtmlTime(timeUnit.endTime)
                val escapedText = timeUnit.text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                sb.append("<span begin=\"$begin\" end=\"$end\">$escapedText</span>")
            }
        }
        
        if (nextLineIsBackground) {
            val nextLine = lyricLines[i + 1]
            val nextUnits = nextLine.timeUnits
            if (nextUnits.isNotEmpty()) {
                val bgFirstBegin = formatTtmlTime(nextUnits.first().startTime)
                val bgLastEnd = formatTtmlTime(nextUnits.last().endTime)
                sb.append("<span ttm:role=\"x-bg\" begin=\"$bgFirstBegin\" end=\"$bgLastEnd\">")
                
                val escapedFirst = nextUnits.first().text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                sb.append("<span begin=\"$bgFirstBegin\" end=\"${formatTtmlTime(nextUnits.first().endTime)}\">($escapedFirst</span>")
                
                for (j in 1 until nextUnits.size) {
                    val unit = nextUnits[j]
                    val begin = formatTtmlTime(unit.startTime)
                    val end = formatTtmlTime(unit.endTime)
                    val escapedText = unit.text
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    if (j == nextUnits.size - 1) {
                        sb.append("<span begin=\"$begin\" end=\"$end\">$escapedText)</span>")
                    } else {
                        sb.append("<span begin=\"$begin\" end=\"$end\">$escapedText</span>")
                    }
                }
                
                val bgTranslationLines = splitTranslationLines(nextLine.translation)
                if (bgTranslationLines.isNotEmpty()) {
                    val escapedTranslation = bgTranslationLines[0]
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    sb.append("<span ttm:role=\"x-translation\" xml:lang=\"zh-CN\">$escapedTranslation</span>")
                    if (bgTranslationLines.size > 1) {
                        val escapedRoman = bgTranslationLines[1]
                            .replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                        sb.append("<span ttm:role=\"x-roman\">$escapedRoman</span>")
                    }
                }
                
                sb.append("</span>")
            }
            i++
        }
        
        val mainTranslationLines = splitTranslationLines(lyricLine.translation)
        if (mainTranslationLines.isNotEmpty()) {
            val escapedTranslation = mainTranslationLines[0]
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            sb.append("<span ttm:role=\"x-translation\" xml:lang=\"zh-CN\">$escapedTranslation</span>")
            if (mainTranslationLines.size > 1) {
                val escapedRoman = mainTranslationLines[1]
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                sb.append("<span ttm:role=\"x-roman\">$escapedRoman</span>")
            }
        }
        
        sb.appendLine("</p>")
        
        lineCounter++
        i++
    }
    
    sb.appendLine("    </div>")
    sb.appendLine("  </body>")
    sb.append("</tt>")
    
    return sb.toString()
}

fun saveTtmlToFile(audioPath: String, ttmlContent: String): Boolean {
    return saveTtmlToFileResult(audioPath, ttmlContent).success
}

data class TtmlSaveResult(
    val success: Boolean,
    val needPermission: Boolean = false,
    val recoverableIntentSender: IntentSender? = null,
    val errorMessage: String = "",
    val savedPath: String = "",
    val redirectedToFallbackDir: Boolean = false
)

fun saveTtmlToFileResult(
    audioPath: String,
    ttmlContent: String,
    context: Context? = null,
    sourceMediaStoreId: Long = -1L
): TtmlSaveResult {
    return try {
        if (audioPath.isEmpty()) {
            Log.e("LyricTiming", "Audio path is empty")
            return TtmlSaveResult(success = false, errorMessage = "音频路径为空")
        }

        val audioFile = File(audioPath)
        val parentDir = audioFile.parentFile

        if (parentDir == null || !parentDir.exists()) {
            Log.e("LyricTiming", "Parent directory does not exist: ${audioFile.parent}")
            return TtmlSaveResult(success = false, errorMessage = "目标目录不存在")
        }

        val ttmlFile = File(parentDir, audioFile.nameWithoutExtension + ".ttml")
        Log.d("LyricTiming", "Saving TTML to: ${ttmlFile.absolutePath}")

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            if (context == null) {
                return TtmlSaveResult(success = false, errorMessage = "缺少上下文，无法在安卓10保存TTML")
            }
            val contentUri = resolveOrCreateMediaStoreFileUriForTtml(context, ttmlFile)
            if (contentUri != null) {
                context.contentResolver.openFileDescriptor(contentUri, "rwt")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { fos ->
                        fos.write(ttmlContent.toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                } ?: return TtmlSaveResult(success = false, errorMessage = "无法打开TTML文件")
                return TtmlSaveResult(success = true, savedPath = ttmlFile.absolutePath)
            } else {
                Log.w("LyricTiming", "MediaStore could not resolve/create TTML uri, fallback to direct file write on Android 10")
                val fallbackWrite = runCatching {
                    ttmlFile.writeText(ttmlContent, Charsets.UTF_8)
                }
                if (fallbackWrite.isSuccess) {
                    return TtmlSaveResult(success = true, savedPath = ttmlFile.absolutePath)
                }
                val publicFallback = saveTtmlToPublicDownloadFallback(
                    context = context,
                    fileName = ttmlFile.name,
                    ttmlContent = ttmlContent
                )
                if (publicFallback.success) {
                    return TtmlSaveResult(
                        success = true,
                        savedPath = publicFallback.displayPath,
                        redirectedToFallbackDir = true
                    )
                }
                val error = fallbackWrite.exceptionOrNull()
                val needLegacyStoragePermission =
                    !com.example.LyricBox.utils.AudioMetadataReader.hasStoragePermission(context)
                return TtmlSaveResult(
                    success = false,
                    needPermission = needLegacyStoragePermission,
                    errorMessage = if (needLegacyStoragePermission) {
                        "无法定位或创建TTML文件，且缺少存储权限"
                    } else {
                        val publicError = publicFallback.errorMessage
                        "无法定位或创建TTML文件；同目录写入失败: ${error?.message.orEmpty()}；下载目录写入失败: $publicError"
                    }
                )
            }
        }

        ttmlFile.writeText(ttmlContent)
        TtmlSaveResult(success = true, savedPath = ttmlFile.absolutePath)
    } catch (e: RecoverableSecurityException) {
        TtmlSaveResult(
            success = false,
            recoverableIntentSender = e.userAction.actionIntent.intentSender,
            errorMessage = "需要授权写入此文件"
        )
    } catch (e: SecurityException) {
        val needAllFilesPermission = context != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !com.example.LyricBox.utils.AudioMetadataReader.hasStoragePermission(context)
        TtmlSaveResult(
            success = false,
            needPermission = needAllFilesPermission,
            errorMessage = if (needAllFilesPermission) {
                "需要存储权限才能保存TTML"
            } else {
                e.message.orEmpty()
            }
        )
    } catch (e: Exception) {
        Log.e("LyricTiming", "Failed to save TTML file", e)
        TtmlSaveResult(success = false, errorMessage = e.message.orEmpty())
    }
}

private fun resolveOrCreateMediaStoreFileUriForTtml(
    context: Context,
    ttmlFile: File
): Uri? {
    val relativePath = buildRelativePathForMediaStore(ttmlFile.absolutePath)
    val existingUri = resolveMediaStoreFileUriForTtml(
        context = context,
        ttmlAbsolutePath = ttmlFile.absolutePath,
        displayName = ttmlFile.name,
        relativePath = relativePath
    )
    if (existingUri != null) return existingUri

    // On some Android 10 ROMs, Files table only allows Download/Documents for insert.
    // For Music/ sidecar TTML, skip insert and let caller fallback to direct write.
    if (!isFilesCollectionInsertPathAllowed(relativePath)) {
        Log.i(
            "LyricTiming",
            "Skip MediaStore insert for TTML due to restricted primary directory: path=${ttmlFile.absolutePath} relative=$relativePath"
        )
        return null
    }

    val collectionUri = mediaStoreFilesCollectionUri()
    val inserted = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, ttmlFile.name)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/ttml+xml")
            if (!relativePath.isNullOrBlank()) {
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                put(MediaStore.Files.FileColumns.DATA, ttmlFile.absolutePath)
            }
        }
        context.contentResolver.insert(collectionUri, values)
    }.onFailure { e ->
        Log.e("LyricTiming", "Insert TTML to MediaStore failed: path=${ttmlFile.absolutePath} relative=$relativePath", e)
    }.getOrNull()

    if (inserted == null) {
        Log.w("LyricTiming", "Insert TTML to MediaStore returned null: path=${ttmlFile.absolutePath} relative=$relativePath")
    }
    return inserted
}

@Suppress("DEPRECATION")
private fun resolveMediaStoreFileUriForTtml(
    context: Context,
    ttmlAbsolutePath: String,
    displayName: String? = null,
    relativePath: String? = null
): Uri? {
    return runCatching {
        val baseUri = mediaStoreFilesCollectionUri()
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val args = arrayOf(ttmlAbsolutePath)
        context.contentResolver.query(baseUri, projection, selection, args, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            ContentUris.withAppendedId(baseUri, id)
        }
    }.getOrNull() ?: runCatching {
        if (displayName.isNullOrBlank() || relativePath.isNullOrBlank()) return@runCatching null
        val baseUri = mediaStoreFilesCollectionUri()
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection =
            "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val args = arrayOf(relativePath, displayName)
        context.contentResolver.query(baseUri, projection, selection, args, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@runCatching null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            ContentUris.withAppendedId(baseUri, id)
        }
    }.getOrNull()
}

private fun mediaStoreFilesCollectionUri(): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Files.getContentUri("external")
    }
}

private data class PublicTtmlSaveResult(
    val success: Boolean,
    val displayPath: String = "",
    val errorMessage: String = ""
)

private fun saveTtmlToPublicDownloadFallback(
    context: Context,
    fileName: String,
    ttmlContent: String
): PublicTtmlSaveResult {
    val relativeDir = "Documents/LunaBeatLyrics/"
    val displayPath = "/storage/emulated/0/$relativeDir$fileName"
    return runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/ttml+xml")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativeDir)
        }
        val uri = context.contentResolver.insert(mediaStoreFilesCollectionUri(), values)
            ?: return PublicTtmlSaveResult(success = false, errorMessage = "insert返回空")
        context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { fos ->
                fos.write(ttmlContent.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
        } ?: return PublicTtmlSaveResult(success = false, errorMessage = "无法打开下载目录目标文件")
        PublicTtmlSaveResult(success = true, displayPath = displayPath)
    }.getOrElse { e ->
        Log.e("LyricTiming", "Save TTML to public download fallback failed: $displayPath", e)
        PublicTtmlSaveResult(success = false, errorMessage = e.message.orEmpty())
    }
}

private fun isFilesCollectionInsertPathAllowed(relativePath: String?): Boolean {
    val primaryDir = relativePath
        ?.trim()
        ?.trim('/')
        ?.substringBefore('/')
        ?.lowercase()
        ?: return true
    return primaryDir == "download" || primaryDir == "documents"
}

@Suppress("DEPRECATION")
private fun buildRelativePathForMediaStore(absolutePath: String): String? {
    val externalRoot = Environment.getExternalStorageDirectory().absolutePath
        .replace('\\', '/')
        .trimEnd('/')
    val normalizedPath = absolutePath.replace('\\', '/')
    if (!normalizedPath.startsWith(externalRoot, ignoreCase = true)) return null
    val relativeFull = normalizedPath.removePrefix(externalRoot).trimStart('/')
    val dir = relativeFull.substringBeforeLast('/', "")
    return if (dir.isBlank()) null else "$dir/"
}

fun buildSavedLyricFromLines(lyricLines: List<LyricLine>): String {
    val sb = StringBuilder()
    var previousLineStartTime = "00:00.000"
    
    lyricLines.forEachIndexed { lineIndex, lyricLine ->
        val timeUnits = lyricLine.timeUnits
        if (isBlankLyricLineForExport(lyricLine)) {
            sb.append("[$previousLineStartTime]\n")
            return@forEachIndexed
        }

        previousLineStartTime = firstExportStartTime(lyricLine, previousLineStartTime)
        var lastEndTime = ""
        
        timeUnits.forEach { timeUnit ->
            if (timeUnit.text.isNotBlank()) {
                if (timeUnit.startTime != lastEndTime) {
                    sb.append("[${timeUnit.startTime}]")
                }
                sb.append(timeUnit.text)
                sb.append("[${timeUnit.endTime}]")
                lastEndTime = timeUnit.endTime
            }
        }
        
        sb.append("\n")
        
        val translationLines = splitTranslationLines(lyricLine.translation)
        if (translationLines.isNotEmpty()) {
            val firstStartTime = if (timeUnits.isNotEmpty()) timeUnits[0].startTime else "00:00.000"
            translationLines.forEach { translationLine ->
                sb.append("[$firstStartTime]$translationLine\n")
            }
        }
    }
    
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricPreviewOverlay(
    lyricLines: List<LyricLine>,
    currentTime: Long,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onLineClick: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    var showTranslation by remember { mutableStateOf(true) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE) }
    val savedFontSize = remember { prefs.getFloat("preview_font_size", 24f) }
    var fontSize by remember { mutableStateOf(savedFontSize.sp) }
    
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    var lastAutoScrollLine by remember { mutableStateOf(-1) }
    
    val previewLines = remember(lyricLines) {
        lyricLines.map { line ->
            val expandedUnits = expandTimeUnits(line.timeUnits)
            val startTimeMs = expandedUnits.firstOrNull()?.startTimeMs ?: 0L
            val endTimeMs = expandedUnits.lastOrNull()?.endTimeMs ?: 0L
            PreviewLyricLineData(
                timeUnits = expandedUnits,
                translation = line.translation,
                isDuet = line.agentType == LyricAgentType.RIGHT,
                isBackground = line.agentType == LyricAgentType.BACKGROUND,
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs
            )
        }
    }
    
    LaunchedEffect(currentTime, previewLines.size) {
        if (previewLines.isEmpty()) return@LaunchedEffect
        
        val currentLineIndex = previewLines.indexOfFirst { line ->
            currentTime >= line.startTimeMs && currentTime < line.endTimeMs
        }
        
        if (currentLineIndex >= 0 && currentLineIndex != lastAutoScrollLine) {
            lastAutoScrollLine = currentLineIndex
            coroutineScope.launch {
                lazyListState.animateScrollToItem(
                    index = currentLineIndex,
                    scrollOffset = 0
                )
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDarkTheme) Color(0xFF0D0D0D) else Color(0xFFF5F5F5)
            )
    ) {
        if (previewLines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyric_timing_no_lyrics_text),
                    color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = 200.dp,
                    bottom = 300.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(previewLines) { index, line ->
                    PreviewLyricLineView(
                        line = line,
                        currentTime = currentTime,
                        showTranslation = showTranslation,
                        isDarkTheme = isDarkTheme,
                        fontSize = fontSize,
                        onClick = { onLineClick(index) }
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { showFontSizeDialog = true },
                modifier = Modifier
                    .background(
                        if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = stringResource(R.string.lyric_timing_font_size),
                    tint = if (isDarkTheme) Color.White else Color.Black
                )
            }
            IconButton(
                onClick = { showTranslation = !showTranslation },
                modifier = Modifier
                    .background(
                        if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = if (showTranslation) "隐藏翻译" else "显示翻译",
                    tint = if (isDarkTheme) Color.White else Color.Black
                )
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(
                        if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.lyric_timing_exit_preview),
                    tint = if (isDarkTheme) Color.White else Color.Black
                )
            }
        }
        
        if (showFontSizeDialog) {
            AlertDialog(
                onDismissRequest = { showFontSizeDialog = false },
                title = { Text("字体大小") },
                text = {
                    Column {
                        Text("当前: ${fontSize.value.toInt()}sp")
                        Slider(
                            value = fontSize.value,
                            onValueChange = { fontSize = it.sp },
                            valueRange = 18f..50f,
                            steps = 31
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showFontSizeDialog = false
                        prefs.edit().putFloat("preview_font_size", fontSize.value).apply()
                    }) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            )
        }
    }
}

fun expandTimeUnits(timeUnits: List<LyricTimeUnit>): List<PreviewLyricWord> {
    val result = mutableListOf<PreviewLyricWord>()
    
    timeUnits.forEach { unit ->
        val startTimeMs = parseTimeToMs(unit.startTime)
        val endTimeMs = parseTimeToMs(unit.endTime)
        val duration = endTimeMs - startTimeMs
        val text = unit.text
        
        if (text.isEmpty()) {
            result.add(PreviewLyricWord(text, startTimeMs, endTimeMs))
            return@forEach
        }
        
        val isCJK = text.any { isCJKCharacter(it) }
        
        if (isCJK) {
            val nonSpaceChars = text.filter { !it.isWhitespace() }
            if (nonSpaceChars.length <= 1) {
                result.add(PreviewLyricWord(text, startTimeMs, endTimeMs))
            } else {
                var charIndex = 0
                text.forEach { char ->
                    if (char.isWhitespace()) {
                        result.add(PreviewLyricWord(char.toString(), startTimeMs, endTimeMs))
                    } else {
                        val charDuration = duration / nonSpaceChars.length
                        val charStart = startTimeMs + charIndex * charDuration
                        val charEnd = if (charIndex == nonSpaceChars.length - 1) endTimeMs else charStart + charDuration
                        result.add(PreviewLyricWord(char.toString(), charStart, charEnd))
                        charIndex++
                    }
                }
            }
        } else {
            val nonSpaceChars = text.filter { !it.isWhitespace() }
            if (nonSpaceChars.length <= 1) {
                result.add(PreviewLyricWord(text, startTimeMs, endTimeMs))
            } else {
                var charIndex = 0
                text.forEach { char ->
                    if (char.isWhitespace()) {
                        result.add(PreviewLyricWord(char.toString(), startTimeMs, endTimeMs))
                    } else {
                        val charDuration = duration / nonSpaceChars.length
                        val charStart = startTimeMs + charIndex * charDuration
                        val charEnd = if (charIndex == nonSpaceChars.length - 1) endTimeMs else charStart + charDuration
                        result.add(PreviewLyricWord(char.toString(), charStart, charEnd))
                        charIndex++
                    }
                }
            }
        }
    }
    
    return result
}

data class PreviewLyricLineData(
    val timeUnits: List<PreviewLyricWord>,
    val translation: String = "",
    val isDuet: Boolean = false,
    val isBackground: Boolean = false,
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 0
)

data class PreviewLyricWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

@Composable
fun PreviewLyricLineView(
    line: PreviewLyricLineData,
    currentTime: Long,
    showTranslation: Boolean,
    isDarkTheme: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    val isLineActive = currentTime >= line.startTimeMs && currentTime < line.endTimeMs
    val isLinePassed = currentTime >= line.endTimeMs
    
    val lineAlpha by animateFloatAsState(
        targetValue = when {
            isLineActive -> 1f
            isLinePassed -> 0.5f
            else -> 0.7f
        },
        animationSpec = tween(300),
        label = "lineAlpha"
    )
    
    val textColor = when {
        line.isBackground -> if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1976D2)
        line.isDuet -> if (isDarkTheme) Color(0xFF81C784) else Color(0xFF388E3C)
        else -> if (isDarkTheme) Color.White else Color.Black
    }
    
    val backgroundColor = when {
        line.isBackground -> if (isDarkTheme) Color(0xFF1A237E).copy(alpha = 0.3f) else Color(0xFFE3F2FD).copy(alpha = 0.5f)
        line.isDuet -> if (isDarkTheme) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFE8F5E9).copy(alpha = 0.5f)
        isLineActive -> if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0)
        else -> Color.Transparent
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(lineAlpha)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = if (line.isDuet) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (line.isDuet) Arrangement.End else Arrangement.Start
        ) {
            if (line.isBackground) {
                Text(
                    text = "♪ ",
                    color = textColor,
                    fontSize = fontSize * 0.7f,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (line.isDuet) {
                Text(
                    text = stringResource(R.string.lyric_timing_duet_prefix),
                    color = textColor,
                    fontSize = fontSize * 0.6f,
                    fontWeight = FontWeight.Light
                )
            }
            
            PreviewLyricWordsView(
                words = line.timeUnits,
                currentTime = currentTime,
                textColor = textColor,
                isDarkTheme = isDarkTheme,
                fontSize = fontSize
            )
        }
        
        val translationLines = splitTranslationLines(line.translation)
        if (showTranslation && translationLines.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                translationLines.forEach { translationLine ->
                    Text(
                        text = translationLine,
                        color = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF666666),
                        fontSize = fontSize * 0.7f
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewLyricWordsView(
    words: List<PreviewLyricWord>,
    currentTime: Long,
    textColor: Color,
    isDarkTheme: Boolean,
    fontSize: TextUnit
) {
    val activeColor = textColor
    val inactiveColor = if (isDarkTheme) Color(0xFF666666) else Color(0xFF999666)
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val letterSpacingPx = with(density) { 0.5.sp.toPx() }
    val maxLiftPx = with(density) { 3.dp.toPx() }
    
    val textMeasurer = rememberTextMeasurer()
    
    // 预计算所有布局，避免重复测量
    val wordLayouts = remember(words, fontSize) {
        words.map { word ->
            val result = textMeasurer.measure(
                text = word.text,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp
                )
            )
            WordLayout(
                text = word.text,
                width = result.size.width.toFloat(),
                height = result.size.height.toFloat()
            )
        }
    }
    
    // 计算每个字的状态和进度
    // 上抬动画：从字开始时间到字结束时间+500ms
    // 颜色渐变：只在字播放期间（开始到结束）
    val wordStates = remember(words, currentTime) {
        words.mapIndexed { index, word ->
            val isPassed = currentTime >= word.endTimeMs
            val isActive = currentTime >= word.startTimeMs && currentTime < word.endTimeMs
            val isFuture = currentTime < word.startTimeMs
            
            // 计算上抬动画进度 - 基于实际时间，不直接跳到1f
            // 动画时间：startTimeMs 到 endTimeMs + 500
            val liftDuration = (word.endTimeMs - word.startTimeMs) + 500
            val liftProgress = if (currentTime < word.startTimeMs) {
                0f // 未开始
            } else {
                // 已开始，根据实际时间计算进度
                val elapsed = currentTime - word.startTimeMs
                (elapsed.toFloat() / liftDuration).coerceIn(0f, 1f)
            }
            
            // 计算颜色渐变进度 - 只在播放期间
            val colorProgress = if (currentTime < word.startTimeMs) {
                0f // 未播放
            } else if (currentTime >= word.endTimeMs) {
                1f // 已播放完
            } else {
                // 正在播放
                ((currentTime - word.startTimeMs).toFloat() / (word.endTimeMs - word.startTimeMs)).coerceIn(0f, 1f)
            }
            
            WordState(
                word = word,
                layout = wordLayouts[index],
                isActive = isActive,
                isPassed = isPassed,
                liftProgress = liftProgress,
                colorProgress = colorProgress
            )
        }
    }
    
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        wordStates.forEach { state ->
            val word = state.word
            val layout = state.layout
            
            // 使用 animateFloatAsState 实现平滑动画
            val animatedLiftProgress by animateFloatAsState(
                targetValue = state.liftProgress,
                animationSpec = tween(durationMillis = 50, easing = LinearEasing),
                label = "lift_${word.startTimeMs}"
            )
            
            val liftY = easeOutCubic(animatedLiftProgress) * maxLiftPx
            val offsetY = with(density) { (-liftY).toDp() }
            
            key(word.startTimeMs, word.text) {
                if (state.isActive) {
                    // 正在播放的字：使用 Canvas 绘制渐变效果
                    val animatedColorProgress by animateFloatAsState(
                        targetValue = state.colorProgress,
                        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
                        label = "color_${word.startTimeMs}"
                    )
                    
                    Box(
                        modifier = Modifier
                            .offset(y = offsetY)
                            .width(with(density) { layout.width.toDp() })
                            .height(with(density) { layout.height.toDp() })
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val featherWidth = layout.width * 0.15f
                            val playedEnd = layout.width * animatedColorProgress
                            
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    this.textSize = fontSizePx
                                    this.letterSpacing = letterSpacingPx / fontSizePx
                                    this.typeface = android.graphics.Typeface.DEFAULT
                                    // 获取字体度量，计算正确的基线位置
                                    val fontMetrics = this.fontMetrics
                                    val textHeight = fontMetrics.descent - fontMetrics.ascent
                                    // 垂直居中：使用 (height + textHeight) / 2 - descent
                                    this.textAlign = android.graphics.Paint.Align.LEFT
                                }
                                
                                val fontMetrics = paint.fontMetrics
                                val textHeight = fontMetrics.descent - fontMetrics.ascent
                                val textX = 0f
                                // 调整基线位置，使文字垂直居中
                                val textY = (layout.height + textHeight) / 2f - fontMetrics.descent
                                
                                // 1. 先绘制整行灰色（待播放部分）
                                canvas.nativeCanvas.drawText(
                                    word.text,
                                    textX,
                                    textY,
                                    paint.apply { 
                                        color = inactiveColor.toArgb()
                                        shader = null
                                    }
                                )
                                
                                // 2. 绘制已播放部分（白色）+ 羽化渐变区域
                                if (playedEnd > 0) {
                                    canvas.save()
                                    
                                    // 创建从左到右的渐变：白色 -> 白灰色 -> 灰色
                                    val shader = android.graphics.LinearGradient(
                                        0f,
                                        0f,
                                        playedEnd,
                                        0f,
                                        intArrayOf(
                                            activeColor.toArgb(),
                                            activeColor.toArgb(),
                                            inactiveColor.toArgb()
                                        ),
                                        floatArrayOf(0f, (playedEnd - featherWidth) / playedEnd, 1f),
                                        android.graphics.Shader.TileMode.CLAMP
                                    )
                                    
                                    paint.shader = shader
                                    canvas.nativeCanvas.drawText(word.text, textX, textY, paint)
                                    
                                    canvas.restore()
                                }
                            }
                        }
                    }
                } else {
                    // 未播放或已播放的字：使用普通 Text
                    Text(
                        text = word.text,
                        color = if (state.isPassed) activeColor else inactiveColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.offset(y = offsetY)
                    )
                }
            }
        }
    }
}

data class WordState(
    val word: PreviewLyricWord,
    val layout: WordLayout,
    val isActive: Boolean,
    val isPassed: Boolean,
    val liftProgress: Float,
    val colorProgress: Float
)

// 缓动函数：easeOutCubic
// 参考 RBBAnimation 的缓动函数
fun easeOutCubic(t: Float): Float {
    return 1 - (1 - t).pow(3)
}

data class WordLayout(
    val text: String,
    val width: Float,
    val height: Float
)

@Composable
fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        }
    )
}
