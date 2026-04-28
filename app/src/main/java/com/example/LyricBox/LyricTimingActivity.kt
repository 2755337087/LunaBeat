package com.example.LyricBox

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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
import kotlin.math.pow

class LyricTimingActivity : ComponentActivity() {
    companion object {
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
    
    private var mediaPlayer: MediaPlayer? = null
    private var showConfirmDialog by mutableStateOf(false)
    private var hasLyrics by mutableStateOf(false)
    private var showConvertDialog by mutableStateOf(false)
    private var convertProgress by mutableIntStateOf(0)
    private var convertMessage by mutableStateOf("")
    private var isConverting by mutableStateOf(false)
    private var convertedAudioPath by mutableStateOf("")
    private var audioImportCount by mutableIntStateOf(0)
    private var sourceAudioPath by mutableStateOf("")
    private var sourceTitle by mutableStateOf("")
    private var sourceArtist by mutableStateOf("")
    private var importedLyricsContent by mutableStateOf("")
    private var importedLyricsFormat by mutableStateOf(0)
    private var playbackCompleted by mutableStateOf(false)
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
            
            if (fileName.lowercase().endsWith(".m4a") || fileName.lowercase().endsWith(".alac")) {
                convertProgress = 0
                convertMessage = "准备解码..."
                isConverting = true
                startConversion(uri, fileName)
            } else {
                loadAudio(uri)
            }
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
        isConverting = true
        convertProgress = 0
        convertMessage = "正在复制文件..."
        
        val inputFile = copyUriToTempFile(uri, fileName)
        if (inputFile == null) {
            convertMessage = "文件复制失败"
            isConverting = false
            return
        }

        // 保留原始音频路径（临时文件），用于后续封面/元数据读取
        sourceAudioPath = inputFile.absolutePath
        
        val outputFileName = fileName.substringBeforeLast(".") + ".wav"
        val outputFile = java.io.File(cacheDir, outputFileName)
        convertedAudioPath = outputFile.absolutePath
        
        convertMessage = "正在解码..."
        
        com.example.LyricBox.utils.AudioConverter.decodeToWav(
            inputPath = inputFile.absolutePath,
            outputPath = outputFile.absolutePath,
            callback = object : com.example.LyricBox.utils.AudioConverter.ConvertCallback {
                override fun onProgress(progress: Int, time: Long) {
                    convertProgress = progress
                    convertMessage = "解码中... $progress%"
                }
                
                override fun onComplete(success: Boolean, message: String) {
                    isConverting = false
                    if (success) {
                        convertMessage = "解码完成"
                        convertProgress = 100

                        // 使用转码音频播放，但不覆盖原始音频路径
                        loadAudioFromPath(outputFile.absolutePath, updateSourcePath = false)
                    } else {
                        convertMessage = "解码失败: $message"
                        inputFile.delete()
                        outputFile.delete()
                    }
                }
                
                override fun onError(error: String) {
                    isConverting = false
                    convertMessage = "错误: $error"
                    inputFile.delete()
                    outputFile.delete()
                }
            }
        )
    }
    
    private fun startConversionFromPath(path: String, fileName: String) {
        isConverting = true
        convertProgress = 0
        convertMessage = "正在准备解码..."
        
        val inputFile = java.io.File(path)
        if (!inputFile.exists()) {
            convertMessage = "文件不存在"
            isConverting = false
            return
        }

        // 保留原始音频路径，避免被转码输出覆盖导致封面读取丢失
        sourceAudioPath = inputFile.absolutePath
        
        val outputFileName = fileName.substringBeforeLast(".") + ".wav"
        val outputFile = java.io.File(cacheDir, outputFileName)
        convertedAudioPath = outputFile.absolutePath
        
        convertMessage = "正在解码..."
        
        com.example.LyricBox.utils.AudioConverter.decodeToWav(
            inputPath = inputFile.absolutePath,
            outputPath = outputFile.absolutePath,
            callback = object : com.example.LyricBox.utils.AudioConverter.ConvertCallback {
                override fun onProgress(progress: Int, time: Long) {
                    convertProgress = progress
                    convertMessage = "解码中... $progress%"
                }
                
                override fun onComplete(success: Boolean, message: String) {
                    isConverting = false
                    if (success) {
                        convertMessage = "解码完成"
                        convertProgress = 100

                        // 使用转码音频播放，但不覆盖原始音频路径
                        loadAudioFromPath(outputFile.absolutePath, updateSourcePath = false)
                    } else {
                        convertMessage = "解码失败: $message"
                        outputFile.delete()
                    }
                }
                
                override fun onError(error: String) {
                    isConverting = false
                    convertMessage = "错误: $error"
                    outputFile.delete()
                }
            }
        )
    }

    private fun startManualConversion() {
        if (sourceAudioPath.isEmpty()) {
            return
        }
        val inputFile = java.io.File(sourceAudioPath)
        if (!inputFile.exists()) {
            return
        }
        showConvertDialog = true
        convertProgress = 0
        convertMessage = "准备解码..."
        isConverting = true
        startConversionFromPath(sourceAudioPath, inputFile.name)
    }
    
    private fun loadAudio(uri: android.net.Uri) {
        val fileName = getFileName(uri)
        val cachedFile = copyUriToTempFile(uri, fileName)
        if (cachedFile != null) {
            loadAudioFromPath(cachedFile.absolutePath)
            return
        }

        // 兜底：缓存失败时直接使用 Uri 播放
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer?.setOnCompletionListener {
            Log.d("LyricTiming", "Audio playback completed")
            playbackCompleted = true
        }
        audioImportCount++
        applyPendingRestoreSeek()
    }

    private fun applyPendingRestoreSeek() {
        val seekPosition = pendingRestoreSeekMs ?: return
        if (seekPosition > 0L) {
            mediaPlayer?.seekTo(seekPosition.toInt())
            lastKnownPlaybackPositionMs = seekPosition
        }
        pendingRestoreSeekMs = null
    }
    
    private fun loadAudioFromPath(path: String, updateSourcePath: Boolean = true) {
        val targetFile = java.io.File(path)
        if (!targetFile.exists()) {
            Log.w("LyricTiming", "Audio file does not exist: $path")
            return
        }
        mediaPlayer?.release()
        mediaPlayer = try {
            MediaPlayer().apply {
                setDataSource(path)
                prepare()
            }
        } catch (e: Exception) {
            Log.e("LyricTiming", "Failed to load audio from path: $path", e)
            null
        }
        mediaPlayer?.setOnCompletionListener {
            Log.d("LyricTiming", "Audio playback completed")
            playbackCompleted = true
        }
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
        return mediaPlayer?.duration?.toLong() ?: 0L
    }
    
    private fun setPlaybackSpeed(speed: Float) {
        mediaPlayer?.let { mp ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val wasPlaying = mp.isPlaying
                val params = android.media.PlaybackParams()
                params.setSpeed(speed)
                mp.playbackParams = params
                if (!wasPlaying && mp.isPlaying) {
                    mp.pause()
                }
            }
        }
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
                if (lyricsFormat == "TTML歌词") {
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
        
        val checkResult = PiracyChecker.checkAll(this)
        if (checkResult.isPirated) {
            piracyCheckResult = checkResult
            showPiracyWarning = true
        }
        
        val intentAudioPath = intent.getStringExtra("audioPath") ?: ""
        val intentSourceTitle = intent.getStringExtra("sourceTitle") ?: ""
        val intentSourceArtist = intent.getStringExtra("sourceArtist") ?: ""
        val intentLyricsContent = intent.getStringExtra("lyricsContent") ?: ""
        val intentLyricsFormat = when (intent.getStringExtra("lyricsFormat") ?: "") {
            "纯文本歌词" -> 0
            "LRC歌词", "LRC逐行/逐字歌词" -> 1
            "增强LRC/ELRC歌词" -> 2
            "TTML歌词" -> 3
            else -> 0
        }

        if (savedInstanceState != null) {
            sourceAudioPath = savedInstanceState.getString(KEY_SOURCE_AUDIO_PATH, intentAudioPath)
            sourceTitle = savedInstanceState.getString(KEY_SOURCE_TITLE, intentSourceTitle)
            sourceArtist = savedInstanceState.getString(KEY_SOURCE_ARTIST, intentSourceArtist)
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
            importedLyricsContent = intentLyricsContent
            importedLyricsFormat = intentLyricsFormat
            pendingRestoreSeekMs = null
            restoredLyricLines = emptyList()
            restoredSelectedLineIndex = 0
            restoredSelectedWordIndex = 0
        }
        
        // 如果是TTML歌词，解析创作者信息
        if (importedLyricsFormat == 3 && importedLyricsContent.isNotEmpty()) {
            val parsedSongwriters = LyricParsingUtils.parseSongwritersFromTtml(importedLyricsContent)
            pendingLyricsCreators = parsedSongwriters
        }
        
        val startupAudioPath = when {
            convertedAudioPath.isNotEmpty() -> convertedAudioPath
            sourceAudioPath.isNotEmpty() -> sourceAudioPath
            else -> ""
        }
        if (startupAudioPath.isNotEmpty()) {
            val file = java.io.File(startupAudioPath)
            if (file.exists()) {
                if (startupAudioPath.lowercase().endsWith(".m4a") || startupAudioPath.lowercase().endsWith(".alac")) {
                    convertProgress = 0
                    convertMessage = "准备解码..."
                    isConverting = true
                    startConversionFromPath(startupAudioPath, file.name)
                } else {
                    loadAudioFromPath(startupAudioPath)
                }
            } else {
                pendingRestoreSeekMs = null
                lastKnownPlaybackPositionMs = 0L
            }
        } else {
            pendingRestoreSeekMs = null
            lastKnownPlaybackPositionMs = 0L
        }
        
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasLyrics) {
                    showConfirmDialog = true
                } else {
                    finish()
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
                        onBack = { finish() },
                        onImportAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        onPlayPause = { play ->
                            if (play) {
                                mediaPlayer?.start()
                                playbackCompleted = false
                            } else {
                                mediaPlayer?.pause()
                            }
                        },
                        onSeekTo = { timeMs ->
                            mediaPlayer?.seekTo(timeMs.toInt())
                            lastKnownPlaybackPositionMs = timeMs
                        },
                        onSetPlaybackSpeed = { speed -> setPlaybackSpeed(speed) },
                        getCurrentPosition = {
                            val position = mediaPlayer?.currentPosition?.toLong() ?: lastKnownPlaybackPositionMs
                            lastKnownPlaybackPositionMs = position
                            position
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
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        if (isFinishing) {
            cleanConvertCache()
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
        val playbackPosition = mediaPlayer?.currentPosition?.toLong() ?: lastKnownPlaybackPositionMs
        outState.putLong(KEY_PLAYBACK_POSITION, playbackPosition)
        outState.putInt(KEY_SELECTED_LINE_INDEX, lastSelectedLineIndexSnapshot)
        outState.putInt(KEY_SELECTED_WORD_INDEX, lastSelectedWordIndexSnapshot)
        if (currentLyricLinesSnapshot.isNotEmpty()) {
            outState.putSerializable(KEY_LYRIC_LINES, ArrayList(currentLyricLinesSnapshot))
        }
    }
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
            val agentTypeOptions = listOf("默认", "对唱", "背景")
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
                lyricLine.timeUnits.forEachIndexed { unitIndex, timeUnit ->
                    if (timeUnit.text.isNotBlank()) {
                        val isSelected = lineIndex == selectedLineIndex && unitIndex == selectedWordIndex
                        val startMs = parseTimeToMs(timeUnit.startTime)
                        val endMs = parseTimeToMs(timeUnit.endTime)
                        val isPlayingHighlight = startMs <= currentTime && currentTime < endMs
                        
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
                                .background(
                                    when {
                                        isPlayingHighlight -> if (isDarkTheme) Color(0x406200EE) else Color(0x206200EE)
                                        else -> Color.Transparent
                                    },
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
        
        if (lyricLine.translation.isNotEmpty()) {
            Text(
                text = lyricLine.translation,
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
                            onShowEditControlPanelChange(true)
                            // 自动暂停歌曲
                            onIsPlayingChange(false)
                            onPlayPause(false)
                            onUpdateJobCancel()
                        }
                    ),
                fontSize = 14.sp,
                color = Color.Gray
            )
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
            text = "创作者",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (creators.isEmpty()) {
            Text(
                text = "暂无创作者",
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
                        text = creator.ifEmpty { "未命名创作者" },
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
                            contentDescription = "编辑",
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
                            contentDescription = "删除",
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
            Text(text = "新增创作者")
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
                text = if (creatorIndex >= 0) "编辑创作者" else "添加创作者",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "创作者名称：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ThemedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                placeholder = "请输入创作者名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        onSave(creatorIndex, inputName.trim())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
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
                    contentDescription = "快退${seekTimeSeconds.toInt()}秒",
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
                    Text(text = "退出跟随模式")
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
                    Text(text = "起始", maxLines = 1, softWrap = false)
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

                            // 跳转到下一个字
                            if (selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size - 1) {
                                onSelectedWordIndexChange(selectedWordIndex + 1)
                            } else if (selectedLineIndex < lyricLines.size - 1) {
                                // 向后查找有内容的行
                                var targetLineIndex = selectedLineIndex + 1
                                while (targetLineIndex < lyricLines.size) {
                                    val nextLine = lyricLines[targetLineIndex]
                                    if (nextLine.timeUnits.isNotEmpty()) {
                                        break
                                    }
                                    targetLineIndex++
                                }
                                if (targetLineIndex < lyricLines.size) {
                                    onSelectedLineIndexChange(targetLineIndex)
                                    onSelectedWordIndexChange(0)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(text = "连续", maxLines = 1, softWrap = false)
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

                            // 跳转到下一个字
                            if (selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size - 1) {
                                onSelectedWordIndexChange(selectedWordIndex + 1)
                            } else if (selectedLineIndex < lyricLines.size - 1) {
                                // 向后查找有内容的行
                                var targetLineIndex = selectedLineIndex + 1
                                while (targetLineIndex < lyricLines.size) {
                                    val nextLine = lyricLines[targetLineIndex]
                                    if (nextLine.timeUnits.isNotEmpty()) {
                                        break
                                    }
                                    targetLineIndex++
                                }
                                if (targetLineIndex < lyricLines.size) {
                                    onSelectedLineIndexChange(targetLineIndex)
                                    onSelectedWordIndexChange(0)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(text = "结束", maxLines = 1, softWrap = false)
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
                    contentDescription = "快进${seekTimeSeconds.toInt()}秒",
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
                        contentDescription = "定位到选中歌词",
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
                    Text(text = "导入歌词")
                }
                CustomDropdownMenu(
                    expanded = showImportLyricMenu,
                    onDismissRequest = { onShowImportLyricMenuChange(false) },
                    items = listOf(
                        MenuItem(title = "通过纯文本导入", onClick = {
                            onShowImportLyricMenuChange(false); onShowLyricInputDialogChange() }),
                        MenuItem(title = "通过LRC逐行/逐字歌词导入", onClick = {
                            onShowImportLyricMenuChange(false); onShowSPLLrcInputDialogChange() }),
                        MenuItem(title = "通过增强LRC/ELRC逐字歌词导入", onClick = {
                            onShowImportLyricMenuChange(false); onShowElrcInputDialogChange() }),
                        MenuItem(title = "通过TTML歌词导入", onClick = {
                            onShowImportLyricMenuChange(false); onShowTtmlInputDialogChange() }),
                        MenuItem(title = "获取逐字歌词", onClick = {
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
                Text(text = "导入音频")
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
            text = "创作者",
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
                label = { Text("创作者 ${index + 1}") },
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
            Text(text = "新增创作者")
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
    onBackClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    menuContent: @Composable (menuButtonPosition: MenuAnchorPosition?) -> Unit = {}
) {
    val iconColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showBack) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                            contentDescription = "返回",
                            tint = iconColor
                        )
                    }
                }
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
                Text(
                    text = targetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
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
                    textAlign = TextAlign.Center
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
                            contentDescription = "菜单",
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
    showEditTranslation: Boolean
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
            text = "修改歌词",
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
            Button(
                onClick = onEditLyric,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("编辑", fontSize = 14.sp)
            }
            Button(
                onClick = onAddLyric,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("新增", fontSize = 14.sp)
            }
            Button(
                onClick = onSplitLyric,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("拆分", fontSize = 14.sp)
            }
            Button(
                onClick = onMergeLyric,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("合并", fontSize = 14.sp)
            }
            Button(
                onClick = onSetTimestamp,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("设置时间", fontSize = 14.sp)
            }
            Button(
                onClick = onDeleteLyric,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除", fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 修改行区域
        Text(
            text = "修改行",
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
                Text("新增", fontSize = 14.sp)
            }
            Button(
                onClick = onSplitLine,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("拆分", fontSize = 14.sp)
            }
            Button(
                onClick = onMergeLine,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("合并", fontSize = 14.sp)
            }
            Button(
                onClick = onMoveLine,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("移动", fontSize = 14.sp)
            }
            if (showAddTranslation) {
                Button(
                    onClick = onAddTranslation,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("添加翻译", fontSize = 14.sp)
                }
            }
            if (showEditTranslation) {
                Button(
                    onClick = onEditTranslation,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("修改翻译", fontSize = 14.sp)
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
                Text("删除", fontSize = 14.sp)
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
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "保存为增强LRC歌词",
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
                    label = "显示对唱标识（v1/v2）"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("关闭")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(enhancedLrcContent)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text("复制")
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
    onEmbedResult: (Boolean, String, Boolean) -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enhancedLrcContent = remember(lyricLines, showDuet) {
        LyricSaveEmbedUtils.buildEnhancedLrc(lyricLines, showDuet)
    }
    val scrollState = rememberScrollState()
    if (showDialog) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "嵌入增强LRC歌词",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "确认嵌入到 '$displayTitle' 吗？",
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
                    label = "显示对唱标识（v1/v2）"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(enhancedLrcContent)
                            onCopied()
                        }
                    ) {
                        Text("复制")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            if (sourceAudioPath.isEmpty()) {
                                onEmbedResult(false, "音频路径为空，无法嵌入歌词", false)
                                return@Button
                            }
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                Log.d("LyricTiming", "Embedding Enhanced LRC lyrics to: $sourceAudioPath")
                                val result = LyricSaveEmbedUtils.embedLyrics(context, sourceAudioPath, enhancedLrcContent)
                                Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                withContext(Dispatchers.Main) {
                                    onEmbedResult(result.success, if (result.success) "歌词已成功嵌入到音频文件" else result.errorMessage, result.needPermission)
                                }
                            }
                        }
                    ) {
                        Text("确认嵌入")
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
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "保存为LRC逐行歌词",
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
                    label = "显示行结束时间戳"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("关闭")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(lineLyricContent)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text("复制")
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
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "保存为TTML歌词",
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
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("关闭")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(ttmlContent)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text("复制")
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
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "保存为LRC逐字歌词",
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
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("关闭")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setText(savedLyric)
                            onDismiss()
                            onCopied()
                        }
                    ) {
                        Text("复制")
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
    confirmButtonText: String = "确定"
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(confirmButtonText)
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
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text("取消")
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
                    contentDescription = "撤销",
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
                    contentDescription = "重做",
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
            title = { Text("导入示例") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "基本格式：",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "一行一句歌词，等号(=)后可添加翻译",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "示例1：勾选使用空格分割",
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
                        text = "分割结果：|欢迎|使用|_Lyric|Box|",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "示例2：未勾选使用空格分割",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "欢迎使用_LyricBox",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "分割结果：|欢迎使用_LyricBox|",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "示例3：带翻译",
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
                        text = "等号(=)后为翻译内容",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "提示：",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "导入后可使用菜单中的\"一键分词\"功能进行分词",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("知道了")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LyricTimingScreen(
    onBack: () -> Unit,
    onImportAudio: () -> Unit,
    onPlayPause: (Boolean) -> Unit,
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
    var showFormatTimelineConfirmDialog by remember { mutableStateOf(false) }
    var showFormatTimelineResultDialog by remember { mutableStateOf(false) }
    var formatTimelineResultMessage by remember { mutableStateOf("") }
    var formatTimelineResultSuccess by remember { mutableStateOf(false) }
    var needStoragePermission by remember { mutableStateOf(false) }
    var showNoAudioDialog by remember { mutableStateOf(false) }
    var showEnhancedLrcSaveDialog by remember { mutableStateOf(false) }
        var showDuetInEnhancedLrc by remember { mutableStateOf(true) }
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
    
    // 创作者列表
        var creators: List<String> by remember { mutableStateOf(emptyList()) }
    
    // 更新歌词状态 - 基于 lyricLines 判断
    SideEffect {
        if (lyricLines.isNotEmpty() != hasLyrics) {
            onHasLyricsChange(lyricLines.isNotEmpty())
        }
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
            
            // 检测是否存在空行
            if (hasEmptyLines(parsedLinesResult)) {
                showDeleteEmptyLinesDialog = true
            }
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
            
            // 检测是否存在空行
            if (hasEmptyLines(verbatimLyricsLines)) {
                showDeleteEmptyLinesDialog = true
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
    var showCancelTranslationConfirmDialog by remember { mutableStateOf(false) }
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    var showSaveFailDialog by remember { mutableStateOf(false) }
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // Headbar
        CommonHeadBar(
            title = displayTitle,
            showBack = true,
            showMenu = !isPreviewMode,
            onBackClick = {
                if (hasLyrics) {
                    onConfirmDialogChange(true)
                } else {
                    onBack()
                }
            },
            onMenuClick = { menuExpanded = true },
            onTitleClick = if (isFromMusicLibrary) {
                { showTitlePathDialog = true }
            } else null,
            menuContent = { menuButtonPosition ->
                val menuItems = if (isFromMusicLibrary) {
                    listOf(
                        MenuItem(
                            title = "导入歌词",
                            subItems = listOf(
                                MenuItem(title = "通过纯文本导入", onClick = { showLyricInputDialog = true }),
                                MenuItem(title = "通过LRC逐行/逐字歌词导入", onClick = { showSPLLrcInputDialog = true }),
                                MenuItem(title = "通过增强LRC/ELRC逐字歌词导入", onClick = { showElrcInputDialog = true }),
                                MenuItem(title = "通过TTML歌词导入", onClick = { showTtmlInputDialog = true }),
                                MenuItem(
                                    title = "通过文本导入翻译",
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
                                    title = "通过文本导入注音",
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            transliterationInput = ""
                                            showImportTransliterationDialog = true
                                        }
                                    }
                                ),
                                MenuItem(title = "获取逐字歌词", onClick = {
                                    val keyword = if (sourceTitle.isNotEmpty() && sourceArtist.isNotEmpty()) "$sourceTitle $sourceArtist" else sourceTitle
                                    onOpenVerbatimLyrics(keyword)
                                })
                            )
                        ),
                        MenuItem(title = "导入音频", onClick = { onImportAudio() }),
                        MenuItem(
                            title = "批量操作",
                            subItems = listOf(
                                MenuItem(
                                    title = "一键分词",
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
                                    title = "一键合并歌词",
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
                                    title = "平移时间戳",
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
                                    title = "转换为简体",
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
                                    title = "删除空行",
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
                                    title = "格式化时间轴",
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
                            title = "嵌入歌词",
                            subItems = listOf(
                                MenuItem(
                                    title = "嵌入LRC逐字歌词",
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedLrcWordDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = "嵌入LRC逐行歌词",
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedLrcLineDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = "嵌入增强LRC歌词",
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedEnhancedLrcDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = "嵌入TTML歌词",
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEmbedTtmlDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = "保存TTML歌词到同目录文件夹",
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
                            title = "预览歌词",
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
                                    val audioPath = if (convertedAudioPath.isNotEmpty()) convertedAudioPath else sourceAudioPath
                                    val previewSourceAudioPath = if (sourceAudioPath.isNotEmpty()) sourceAudioPath else audioPath
                                    val currentPos = getCurrentPosition()
                                    val previewLyricLines = lyricLines.map { line ->
                                        val expandedWords = if (line.timeUnits.size == 1) {
                                            // 如果一行只有一个 timeUnit，保留为逐行歌词（不拆分）
                                            val unit = line.timeUnits.first()
                                            val beginMs = parseTimeToMs(unit.startTime)
                                            val endMs = parseTimeToMs(unit.endTime)
                                            listOf(com.example.LyricBox.NewPreviewLyricWord(
                                                text = unit.text,
                                                begin = beginMs,
                                                end = endMs,
                                                transliteration = unit.transliteration,
                                                charTransliterations = unit.charTransliterations
                                            ))
                                        } else {
                                            // 多个 timeUnit，按原来的逻辑拆分成逐字
                                            line.timeUnits.flatMap { unit ->
                                                val beginMs = parseTimeToMs(unit.startTime)
                                                val endMs = parseTimeToMs(unit.endTime)
                                                val duration = endMs - beginMs
                                                val text = unit.text
                                                if (text.isEmpty()) {
                                                    emptyList()
                                                } else if (text.length == 1) {
                                                    listOf(com.example.LyricBox.NewPreviewLyricWord(
                                                        text = text,
                                                        begin = beginMs,
                                                        end = endMs,
                                                        transliteration = if (unit.charTransliterations.isNotEmpty()) {
                                                            unit.charTransliterations[0] ?: ""
                                                        } else {
                                                            ""
                                                        },
                                                        charTransliterations = emptyMap()
                                                    ))
                                                } else {
                                                    val nonSpaceChars = text.filter { it != ' ' }
                                                    val nonSpaceCount = nonSpaceChars.length
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
                                    val intent = android.content.Intent(context, LyricPreviewActivity::class.java).apply {
                                        putExtra(LyricPreviewActivity.EXTRA_AUDIO_PATH, audioPath)
                                        putExtra(LyricPreviewActivity.EXTRA_SOURCE_AUDIO_PATH, previewSourceAudioPath)
                                        putExtra(LyricPreviewActivity.EXTRA_TITLE, displayTitle)
                                        putExtra(LyricPreviewActivity.EXTRA_INITIAL_POSITION, currentPos)
                                        putExtra(LyricPreviewActivity.EXTRA_CREATORS, pendingLyricsCreators.toTypedArray())
                                        putExtra("line_count", previewLyricLines.size)
                                        putExtra("words_per_line", previewLyricLines.map { it.words.size }.toIntArray())
                                        val wordsList = previewLyricLines.flatMap { it.words }
                                        putExtra("begins", wordsList.map { it.begin }.toLongArray())
                                        putExtra("ends", wordsList.map { it.end }.toLongArray())
                                        putExtra("texts", wordsList.map { it.text }.toTypedArray())
                                        // 传递注音数据
                                        val transliterations = wordsList.map { it.transliteration }.toTypedArray()
                                        putExtra("transliterations", transliterations)
                                        // 序列化逐字符注音
                                        val charTransliterationStrings = wordsList.map { word ->
                                            word.charTransliterations.entries.joinToString(";") { "${it.key}=${it.value}" }
                                        }.toTypedArray()
                                        putExtra("char_transliterations", charTransliterationStrings)
                                        putExtra("translations", previewLyricLines.map { it.translation }.toTypedArray())
                                        putExtra("is_duets", previewLyricLines.map { it.isDuet }.toBooleanArray())
                                        putExtra("is_backgrounds", previewLyricLines.map { it.isBackground }.toBooleanArray())
                                    }
                                    previewLauncher.launch(intent)
                                }
                            }
                        )
                    )
                } else {
                    listOf(
                        MenuItem(
                            title = "导入歌词",
                            subItems = listOf(
                                MenuItem(title = "通过纯文本导入", onClick = { showLyricInputDialog = true }),
                                MenuItem(title = "通过LRC逐行/逐字歌词导入", onClick = { showSPLLrcInputDialog = true }),
                                MenuItem(title = "通过增强LRC/ELRC逐字歌词导入", onClick = { showElrcInputDialog = true }),
                                MenuItem(title = "通过TTML歌词导入", onClick = { showTtmlInputDialog = true }),
                                MenuItem(
                                    title = "通过文本导入翻译",
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
                                    title = "通过文本导入注音",
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            transliterationInput = ""
                                            showImportTransliterationDialog = true
                                        }
                                    }
                                ),
                                MenuItem(title = "获取逐字歌词", onClick = {
                                    val keyword = if (sourceTitle.isNotEmpty() && sourceArtist.isNotEmpty()) "$sourceTitle $sourceArtist" else sourceTitle
                                    onOpenVerbatimLyrics(keyword)
                                })
                            )
                        ),
                        MenuItem(title = "导入音频", onClick = { onImportAudio() }),
                        MenuItem(
                            title = "批量操作",
                            subItems = listOf(
                                MenuItem(
                                    title = "一键分词",
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
                                    title = "一键合并歌词",
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
                                    title = "平移时间戳",
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
                                    title = "转换为简体",
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
                                    title = "删除空行",
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
                                    title = "格式化时间轴",
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
                            title = "保存歌词",
                            subItems = listOf(
                                MenuItem(
                                    title = "保存为LRC逐字歌词",
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
                                    title = "保存为LRC逐行歌词",
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
                                    title = "保存为增强LRC歌词",
                                    onClick = {
                                        if (lyricLines.isEmpty()) {
                                            showNoLyricsDialog = true
                                        } else {
                                            showEnhancedLrcSaveDialog = true
                                        }
                                    }
                                ),
                                MenuItem(
                                    title = "保存为TTML歌词",
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
                            title = "预览歌词",
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
                                    val audioPath = if (convertedAudioPath.isNotEmpty()) convertedAudioPath else sourceAudioPath
                                    val previewSourceAudioPath = if (sourceAudioPath.isNotEmpty()) sourceAudioPath else audioPath
                                    val currentPos = getCurrentPosition()
                                    val previewLyricLines = lyricLines.map { line ->
                                        val expandedWords = if (line.timeUnits.size == 1) {
                                            // 如果一行只有一个 timeUnit，保留为逐行歌词（不拆分）
                                            val unit = line.timeUnits.first()
                                            val beginMs = parseTimeToMs(unit.startTime)
                                            val endMs = parseTimeToMs(unit.endTime)
                                            listOf(com.example.LyricBox.NewPreviewLyricWord(
                                                text = unit.text,
                                                begin = beginMs,
                                                end = endMs,
                                                transliteration = unit.transliteration,
                                                charTransliterations = unit.charTransliterations
                                            ))
                                        } else {
                                            // 多个 timeUnit，按原来的逻辑拆分成逐字
                                            line.timeUnits.flatMap { unit ->
                                                val beginMs = parseTimeToMs(unit.startTime)
                                                val endMs = parseTimeToMs(unit.endTime)
                                                val duration = endMs - beginMs
                                                val text = unit.text
                                                if (text.isEmpty()) {
                                                    emptyList()
                                                } else if (text.length == 1) {
                                                    listOf(com.example.LyricBox.NewPreviewLyricWord(
                                                        text = text,
                                                        begin = beginMs,
                                                        end = endMs,
                                                        transliteration = if (unit.charTransliterations.isNotEmpty()) {
                                                            unit.charTransliterations[0] ?: ""
                                                        } else {
                                                            ""
                                                        },
                                                        charTransliterations = emptyMap()
                                                    ))
                                                } else {
                                                    val nonSpaceChars = text.filter { it != ' ' }
                                                    val nonSpaceCount = nonSpaceChars.length
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
                                    val intent = android.content.Intent(context, LyricPreviewActivity::class.java).apply {
                                        putExtra(LyricPreviewActivity.EXTRA_AUDIO_PATH, audioPath)
                                        putExtra(LyricPreviewActivity.EXTRA_SOURCE_AUDIO_PATH, previewSourceAudioPath)
                                        putExtra(LyricPreviewActivity.EXTRA_TITLE, displayTitle)
                                        putExtra(LyricPreviewActivity.EXTRA_INITIAL_POSITION, currentPos)
                                        putExtra(LyricPreviewActivity.EXTRA_CREATORS, pendingLyricsCreators.toTypedArray())
                                        putExtra("line_count", previewLyricLines.size)
                                        putExtra("words_per_line", previewLyricLines.map { it.words.size }.toIntArray())
                                        val wordsList = previewLyricLines.flatMap { it.words }
                                        putExtra("begins", wordsList.map { it.begin }.toLongArray())
                                        putExtra("ends", wordsList.map { it.end }.toLongArray())
                                        putExtra("texts", wordsList.map { it.text }.toTypedArray())
                                        // 传递注音数据
                                        val transliterations = wordsList.map { it.transliteration }.toTypedArray()
                                        putExtra("transliterations", transliterations)
                                        // 序列化逐字符注音
                                        val charTransliterationStrings = wordsList.map { word ->
                                            word.charTransliterations.entries.joinToString(";") { "${it.key}=${it.value}" }
                                        }.toTypedArray()
                                        putExtra("char_transliterations", charTransliterationStrings)
                                        putExtra("translations", previewLyricLines.map { it.translation }.toTypedArray())
                                        putExtra("is_duets", previewLyricLines.map { it.isDuet }.toBooleanArray())
                                        putExtra("is_backgrounds", previewLyricLines.map { it.isBackground }.toBooleanArray())
                                    }
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
        
        // 歌词输入对话框 - ModalBottomSheet
        val lyricInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showLyricInputDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    if (lyricInput.isNotEmpty()) {
                        pendingLyricInputDismiss = true
                        showCancelLyricInputConfirm = true
                    } else {
                        showLyricInputDialog = false
                    }
                },
                sheetState = lyricInputSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "导入歌词",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { showImportExampleDialog = true }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_help),
                                contentDescription = "帮助",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ThemedTextField(
                        value = lyricInput,
                        onValueChange = { lyricInput = it },
                        placeholder = "一行一句歌词\n每行格式：歌词=翻译（翻译可选）",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        singleLine = false,
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomCheckbox(
                        checked = useSpaceSplit,
                        onCheckedChange = { useSpaceSplit = it },
                        label = "使用空格分割歌词(高级)"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(
                            text = if (useSpaceSplit) "按空格分割歌词单元，若需要保留歌词中原有空格，需要两个空格"
                            else "每行作为一个整体歌词单元，可通过菜单中的“一键分词”功能快速分割（推荐）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = { 
                                if (lyricInput.isNotEmpty()) {
                                    pendingLyricInputDismiss = true
                                    showCancelLyricInputConfirm = true
                                } else {
                                    showLyricInputDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    lyricInput = clip.getItemAt(0).text.toString()
                                }
                            }
                        ) {
                            Text("粘贴")
                        }
                        OutlinedButton(
                            onClick = { lyricInput = "" }
                        ) {
                            Text("清空")
                        }
                        Button(
                            onClick = {
                                val lines = lyricInput.lines().filter { it.isNotBlank() }
                                lyrics = lines.map { line ->
                                    val parts = line.split("=", limit = 2)
                                    parts[0]
                                }

                                val parsedLyricLines = lines.map { line ->
                                    val parts = line.split("=", limit = 2)
                                    val originalLyric = parts[0].replace('\u00A0', ' ')
                                    val translation = if (parts.size > 1) parts[1] else ""

                                    val words = if (useSpaceSplit) {
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
                                showLyricInputDialog = false
                                
                                // 检测是否存在空行
                                if (hasEmptyLines(parsedLyricLines)) {
                                    showDeleteEmptyLinesDialog = true
                                }
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // 纯文本导入取消确认对话框
        if (showCancelLyricInputConfirm) {
            AlertDialog(
                onDismissRequest = { 
                    showCancelLyricInputConfirm = false
                },
                title = { Text("确认取消") },
                text = { Text("输入框中有内容，确定要取消吗？") },
                confirmButton = {
                    Button(
                        onClick = { 
                            showCancelLyricInputConfirm = false
                            coroutineScope.launch {
                                lyricInputSheetState.show()
                            }
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCancelLyricInputConfirm = false
                            showLyricInputDialog = false
                            lyricInput = ""
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }
        
        // 导入示例对话框
        ImportExampleDialog(
            showDialog = showImportExampleDialog,
            onDismiss = { showImportExampleDialog = false }
        )
        
        // SPL/LRC歌词输入对话框 - ModalBottomSheet
        val splLrcInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showSPLLrcInputDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    if (splLrcInput.isNotEmpty()) {
                        pendingSpllrcInputDismiss = true
                        showCancelSpllrcInputConfirm = true
                    } else {
                        showSPLLrcInputDialog = false
                    }
                },
                sheetState = splLrcInputSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "导入 LRC 逐行/逐字歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = splLrcInput,
                        onValueChange = { splLrcInput = it },
                        placeholder = "输入LRC 逐行/逐字歌词\n例如：[00:00.000]歌词[00:00.000]",
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
                        TextButton(
                            onClick = { 
                                if (splLrcInput.isNotEmpty()) {
                                    pendingSpllrcInputDismiss = true
                                    showCancelSpllrcInputConfirm = true
                                } else {
                                    showSPLLrcInputDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    splLrcInput = clip.getItemAt(0).text.toString()
                                }
                            }
                        ) {
                            Text("粘贴")
                        }
                        OutlinedButton(
                            onClick = { splLrcInput = "" }
                        ) {
                            Text("清空")
                        }
                        Button(
                            onClick = {
                                val parseResult = LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, splLrcInput)
                                val parsedLyrics = parseResult.lyrics
                                val parsedLines = parseResult.lyricLines
                                lyrics = parsedLyrics
                                lyricLines = parsedLines
                                selectedLineIndex = 0
                                selectedWordIndex = 0
                                showSPLLrcInputDialog = false
                                
                                // 检测是否存在空行
                                if (hasEmptyLines(parsedLines)) {
                                    showDeleteEmptyLinesDialog = true
                                }
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // LRC逐行导入取消确认对话框
        if (showCancelSpllrcInputConfirm) {
            AlertDialog(
                onDismissRequest = { showCancelSpllrcInputConfirm = false },
                title = { Text("确认取消") },
                text = { Text("输入框中有内容，确定要取消吗？") },
                confirmButton = {
                    Button(
                        onClick = { 
                            showCancelSpllrcInputConfirm = false
                            coroutineScope.launch {
                                splLrcInputSheetState.show()
                            }
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCancelSpllrcInputConfirm = false
                            showSPLLrcInputDialog = false
                            splLrcInput = ""
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }
        
        // ELRC歌词输入对话框 - ModalBottomSheet
        val elrcInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showElrcInputDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    if (elrcInput.isNotEmpty()) {
                        pendingElrcInputDismiss = true
                        showCancelElrcInputConfirm = true
                    } else {
                        showElrcInputDialog = false
                    }
                },
                sheetState = elrcInputSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "导入增强LRC/ELRC逐字歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = elrcInput,
                        onValueChange = { elrcInput = it },
                        placeholder = "输入ELRC歌词\n例如：[00:44.360]v2: <00:44.360>内<00:44.840>...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        singleLine = false,
                        maxLines = 15
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：解析<时间>标签，v1为左侧歌词，v2为右侧歌词",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                if (elrcInput.isNotEmpty()) {
                                    pendingElrcInputDismiss = true
                                    showCancelElrcInputConfirm = true
                                } else {
                                    showElrcInputDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    elrcInput = clip.getItemAt(0).text.toString()
                                }
                            }
                        ) {
                            Text("粘贴")
                        }
                        OutlinedButton(
                            onClick = { elrcInput = "" }
                        ) {
                            Text("清空")
                        }
                        Button(
                            onClick = {
                                val parseResult = LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, elrcInput)
                                val parsedLyrics = parseResult.lyrics
                                val parsedLines = parseResult.lyricLines
                                if (parsedLines.isNotEmpty()) {
                                    lyrics = parsedLyrics
                                    lyricLines = parsedLines
                                    selectedLineIndex = 0
                                    selectedWordIndex = 0
                                    
                                    // 检测是否存在空行
                                    if (hasEmptyLines(parsedLines)) {
                                        showDeleteEmptyLinesDialog = true
                                    }
                                }
                                showElrcInputDialog = false
                                elrcInput = ""
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // ELRC导入取消确认对话框
        if (showCancelElrcInputConfirm) {
            AlertDialog(
                onDismissRequest = { showCancelElrcInputConfirm = false },
                title = { Text("确认取消") },
                text = { Text("输入框中有内容，确定要取消吗？") },
                confirmButton = {
                    Button(
                        onClick = { 
                            showCancelElrcInputConfirm = false
                            coroutineScope.launch {
                                elrcInputSheetState.show()
                            }
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCancelElrcInputConfirm = false
                            showElrcInputDialog = false
                            elrcInput = ""
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }
        
        // TTML歌词输入对话框 - ModalBottomSheet
        val ttmlInputSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showTtmlInputDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    if (ttmlInput.isNotEmpty()) {
                        pendingTtmlInputDismiss = true
                        showCancelTtmlInputConfirm = true
                    } else {
                        showTtmlInputDialog = false
                    }
                },
                sheetState = ttmlInputSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "导入TTML歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = ttmlInput,
                        onValueChange = { ttmlInput = it },
                        placeholder = "输入TTML歌词内容\n粘贴TTML格式歌词",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        singleLine = false,
                        maxLines = 15
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：导入时会自动删除换行符和多余空格",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                if (ttmlInput.isNotEmpty()) {
                                    pendingTtmlInputDismiss = true
                                    showCancelTtmlInputConfirm = true
                                } else {
                                    showTtmlInputDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    ttmlInput = clip.getItemAt(0).text.toString()
                                }
                            }
                        ) {
                            Text("粘贴")
                        }
                        OutlinedButton(
                            onClick = { ttmlInput = "" }
                        ) {
                            Text("清空")
                        }
                        Button(
                            onClick = {
                                val parseResult = LyricParsingUtils.parseByType(LyricParseType.TTML, ttmlInput)
                                val parsedLines = parseResult.lyricLines
                                if (parsedLines.isNotEmpty()) {
                                    lyricLines = parsedLines
                                    lyrics = parseResult.lyrics
                                    selectedLineIndex = 0
                                    selectedWordIndex = 0
                                    
                                    // 解析创作者信息
                                    val parsedSongwriters = LyricParsingUtils.parseSongwritersFromTtml(ttmlInput)
                                    if (parsedSongwriters.isNotEmpty()) {
                                        creators = parsedSongwriters
                                    }
                                    
                                    // 检测是否存在空行
                                    if (hasEmptyLines(parsedLines)) {
                                        showDeleteEmptyLinesDialog = true
                                    }
                                }
                                showTtmlInputDialog = false
                                ttmlInput = ""
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // TTML导入取消确认对话框
        if (showCancelTtmlInputConfirm) {
            AlertDialog(
                onDismissRequest = { showCancelTtmlInputConfirm = false },
                title = { Text("确认取消") },
                text = { Text("输入框中有内容，确定要取消吗？") },
                confirmButton = {
                    Button(
                        onClick = { 
                            showCancelTtmlInputConfirm = false
                            coroutineScope.launch {
                                ttmlInputSheetState.show()
                            }
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCancelTtmlInputConfirm = false
                            showTtmlInputDialog = false
                            ttmlInput = ""
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }
        
        // 通过文本仅导入翻译对话框 - ModalBottomSheet
        val importTranslationSheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != androidx.compose.material3.SheetValue.Hidden }
        )
        if (showImportTranslationDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { 
                    if (translationInput.isNotEmpty()) {
                        showCancelTranslationConfirmDialog = true
                    } else {
                        showImportTranslationDialog = false
                    }
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
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
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .statusBarsPadding()
                ) {
                    Text(
                        text = "通过文本导入翻译",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            state = lyricScrollState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 4.dp),
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
                                    onValueChange = { translationInput = it },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(translationScrollState)
                                        .horizontalScroll(translationHorizontalScrollState),
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
                                                    text = "输入翻译，一行一句",
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
                        TextButton(
                            onClick = {
                                if (translationInput.isNotEmpty()) {
                                    showCancelTranslationConfirmDialog = true
                                } else {
                                    showImportTranslationDialog = false
                                    translationInput = ""
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val translationLinesList = translationInput.lines()
                                lyricLines = lyricLines.mapIndexed { index, line ->
                                    line.copy(translation = translationLinesList.getOrNull(index) ?: "")
                                }
                                showImportTranslationDialog = false
                                translationInput = ""
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // 取消导入翻译确认对话框 - AlertDialog
        if (showCancelTranslationConfirmDialog) {
            AlertDialog(
                onDismissRequest = { },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认取消") },
                text = { Text("输入框中有内容，确定要取消吗？") },
                confirmButton = {
                    Button(
                        onClick = { 
                            showCancelTranslationConfirmDialog = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCancelTranslationConfirmDialog = false
                            showImportTranslationDialog = false
                            translationInput = ""
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }
        
        // 导入注音对话框 - ModalBottomSheet
        val importTransliterationSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showImportTransliterationDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showImportTransliterationDialog = false },
                sheetState = importTransliterationSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "导入注音",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = transliterationInput,
                        onValueChange = { transliterationInput = it },
                        placeholder = "输入格式：\n歌词：注音\n注意：请输入单个字符对应的注音，一行一个。\n示例：\n你：ni\n好：hao",
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
                        TextButton(
                            onClick = { showImportTransliterationDialog = false }
                        ) {
                            Text("取消")
                        }
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    transliterationInput = clip.getItemAt(0).text.toString()
                                }
                            }
                        ) {
                            Text("粘贴")
                        }
                        OutlinedButton(
                            onClick = { transliterationInput = "" }
                        ) {
                            Text("清空")
                        }
                        Button(
                            onClick = {
                                // 解析用户输入的注音
                                val transliterationMap = mutableMapOf<String, String>()
                                val lines = transliterationInput.lines().filter { it.isNotBlank() }
                                var parseError = false
                                
                                for (line in lines) {
                                    val parts = line.split("：", ":", limit = 2)
                                    if (parts.size == 2) {
                                        val char = parts[0].trim()
                                        val transliteration = parts[1].trim()
                                        if (char.isNotEmpty()) {
                                            transliterationMap[char] = transliteration
                                        }
                                    } else {
                                        parseError = true
                                        break
                                    }
                                }
                                
                                if (parseError) {
                                    transliterationResultSuccess = false
                                    transliterationResultMessage = "解析失败，请检查格式是否正确"
                                    showTransliterationResultDialog = true
                                    showImportTransliterationDialog = false
                                } else if (transliterationMap.isEmpty()) {
                                    transliterationResultSuccess = false
                                    transliterationResultMessage = "未输入任何注音"
                                    showTransliterationResultDialog = true
                                    showImportTransliterationDialog = false
                                } else {
                                    // 应用注音到歌词
                                    var matchCount = 0
                                    val oldLines = lyricLines.toList()
                                    
                                    lyricLines = lyricLines.map { line ->
                                        val newUnits = line.timeUnits.map { unit ->
                                            val text = unit.text
                                            val newCharTransliterations = unit.charTransliterations.toMutableMap()
                                            
                                            // 逐个字符检查并添加注音
                                            for ((index, char) in text.withIndex()) {
                                                val charStr = char.toString()
                                                // 只对CJK字符、日语假名、韩语谚文和数字注音
                                                if (transliterationMap.containsKey(charStr)) {
                                                    // 检查字符类型
                                                    val isCJK = char in '\u4E00'..'\u9FFF' || char in '\u3400'..'\u4DBF' // CJK统一汉字和扩展A
                                                    val isHiragana = char in '\u3040'..'\u309F' // 平假名
                                                    val isKatakana = char in '\u30A0'..'\u30FF' // 片假名
                                                    val isKatakanaExtended = char in '\u31F0'..'\u31FF' // 片假名扩展
                                                    val isHangul = char in '\uAC00'..'\uD7AF' // 韩语谚文
                                                    val isDigit = char.isDigit()
                                                    if (isCJK || isHiragana || isKatakana || isKatakanaExtended || isHangul || isDigit) {
                                                        newCharTransliterations[index] = transliterationMap[charStr]!!
                                                        matchCount++
                                                    }
                                                }
                                            }
                                            
                                            unit.copy(charTransliterations = newCharTransliterations)
                                        }
                                        line.copy(timeUnits = newUnits)
                                    }
                                    
                                    // 添加到撤销栈
                                    undoRedoManager.pushAction(
                                        UndoAction(
                                            actionType = UndoActionType.MULTI_CHANGE,
                                            lineIndex = 0,
                                            oldValue = oldLines,
                                            newValue = lyricLines
                                        )
                                    )
                                    updateUndoRedoState()
                                    
                                    if (matchCount == 0) {
                                        transliterationResultSuccess = false
                                        transliterationResultMessage = "未找到对应歌词"
                                    } else {
                                        transliterationResultSuccess = true
                                        transliterationResultMessage = "成功对${matchCount}个字符完成注音"
                                    }
                                    
                                    showTransliterationResultDialog = true
                                    showImportTransliterationDialog = false
                                }
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
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
            onShowDuetChange = { showDuetInEnhancedLrc = it },
            onCopied = { showCopiedDialog = true }
        )
        
        EmbedEnhancedLrcDialog(
            showDialog = showEmbedEnhancedLrcDialog,
            onDismiss = { showEmbedEnhancedLrcDialog = false },
            lyricLines = lyricLines,
            showDuet = showDuetInEnhancedLrc,
            onShowDuetChange = { showDuetInEnhancedLrc = it },
            onCopied = { showCopiedDialog = true },
            displayTitle = displayTitle,
            sourceAudioPath = sourceAudioPath,
            onEmbedResult = { success, message, needPermission ->
                embedResultSuccess = success
                embedResultMessage = message
                needStoragePermission = needPermission
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
            title = "提示",
            text = "请先导入歌词"
        )
        
        // 无空行提示对话框
        SimpleAlertDialog(
            showDialog = showNoEmptyLinesDialog,
            onDismiss = { showNoEmptyLinesDialog = false },
            title = "提示",
            text = "暂无空行可删除"
        )
        
        // 未导入音频提示对话框
        SimpleAlertDialog(
            showDialog = showNoAudioDialog,
            onDismiss = { showNoAudioDialog = false },
            title = "提示",
            text = "请先导入音频文件"
        )
        
        // 返回确认对话框
        ConfirmDialog(
            showDialog = showConfirmDialog,
            onConfirm = {
                onConfirmDialogChange(false)
                onBack()
            },
            onCancel = { onConfirmDialogChange(false) },
            title = "确认返回",
            text = "确定要返回吗？当前编辑的歌词可能会丢失。"
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
                title = "确认覆盖",
                text = "当前已存在歌词，是否覆盖？"
            )
        }
        
        // 标题路径对话框
        SimpleAlertDialog(
            showDialog = showTitlePathDialog,
            onDismiss = { showTitlePathDialog = false },
            title = "文件路径",
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
                onDismissRequest = { showEmbedLrcWordDialog = false },
                sheetState = embedLrcWordSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "嵌入LRC逐字歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "确认嵌入到 '$displayTitle' 吗？",
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
                        TextButton(
                            onClick = { showEmbedLrcWordDialog = false }
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setText(lrcContent)
                                showCopiedDialog = true
                            }
                        ) {
                            Text("复制")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showEmbedLrcWordDialog = false
                                if (sourceAudioPath.isEmpty()) {
                                    embedResultSuccess = false
                                    embedResultMessage = "音频路径为空，无法嵌入歌词"
                                    showEmbedResultDialog = true
                                    return@Button
                                }
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                    Log.d("LyricTiming", "Embedding lyrics to: $sourceAudioPath")
                                    val result = LyricSaveEmbedUtils.embedLyrics(context, sourceAudioPath, lrcContent)
                                    Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                    withContext(Dispatchers.Main) {
                                        embedResultSuccess = result.success
                                        embedResultMessage = if (result.success) "歌词已成功嵌入到音频文件" else result.errorMessage
                                        needStoragePermission = result.needPermission
                                        showEmbedResultDialog = true
                                    }
                                }
                            }
                        ) {
                            Text("确认嵌入")
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
                onDismissRequest = { showEmbedLrcLineDialog = false },
                sheetState = embedLrcLineSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "嵌入LRC逐行歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "确认嵌入到 '$displayTitle' 吗？",
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
                        label = "显示行结束时间戳"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showEmbedLrcLineDialog = false }
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setText(lineLrcContent)
                                showCopiedDialog = true
                            }
                        ) {
                            Text("复制")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showEmbedLrcLineDialog = false
                                if (sourceAudioPath.isEmpty()) {
                                    embedResultSuccess = false
                                    embedResultMessage = "音频路径为空，无法嵌入歌词"
                                    showEmbedResultDialog = true
                                    return@Button
                                }
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                    Log.d("LyricTiming", "Embedding line lyrics to: $sourceAudioPath")
                                    val result = LyricSaveEmbedUtils.embedLyrics(context, sourceAudioPath, lineLrcContent)
                                    Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                    withContext(Dispatchers.Main) {
                                        embedResultSuccess = result.success
                                        embedResultMessage = if (result.success) "歌词已成功嵌入到音频文件" else result.errorMessage
                                        needStoragePermission = result.needPermission
                                        showEmbedResultDialog = true
                                    }
                                }
                            }
                        ) {
                            Text("确认嵌入")
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
                onDismissRequest = { showEmbedTtmlDialog = false },
                sheetState = embedTtmlSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "嵌入TTML歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "确认嵌入到 '$displayTitle' 吗？",
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
                        TextButton(
                            onClick = { showEmbedTtmlDialog = false }
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setText(ttmlContent)
                                showCopiedDialog = true
                            }
                        ) {
                            Text("复制")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showEmbedTtmlDialog = false
                                if (sourceAudioPath.isEmpty()) {
                                    embedResultSuccess = false
                                    embedResultMessage = "音频路径为空，无法嵌入歌词"
                                    showEmbedResultDialog = true
                                    return@Button
                                }
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                    Log.d("LyricTiming", "Embedding TTML lyrics to: $sourceAudioPath")
                                    val result = LyricSaveEmbedUtils.embedLyrics(context, sourceAudioPath, ttmlContent)
                                    Log.d("LyricTiming", "Write result: success=${result.success}, error=${result.errorMessage}")
                                    withContext(Dispatchers.Main) {
                                        embedResultSuccess = result.success
                                        embedResultMessage = if (result.success) "歌词已成功嵌入到音频文件" else result.errorMessage
                                        needStoragePermission = result.needPermission
                                        showEmbedResultDialog = true
                                    }
                                }
                            }
                        ) {
                            Text("确认嵌入")
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
                onDismissRequest = { showSaveTtmlFileDialog = false },
                sheetState = saveTtmlFileSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "保存TTML歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "确认保存为 '$ttmlFileName' 到 '${audioFile.parent?.substringAfterLast("/") ?: ""}' 目录吗？",
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
                        TextButton(onClick = { showSaveTtmlFileDialog = false }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val success = LyricSaveEmbedUtils.saveTtmlFile(sourceAudioPath, ttmlContent)
                                showSaveTtmlFileDialog = false
                                if (success) {
                                    showSaveSuccessDialog = true
                                } else {
                                    showSaveFailDialog = true
                                }
                            }
                        ) {
                            Text("确认保存")
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
                        text = "跳转到指定时间",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "当前时间: ${formatTime(currentTime)}",
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
                            placeholder = "分",
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
                            placeholder = "秒",
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
                            placeholder = "毫秒",
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
                                Text("进入播放跟随模式")
                            }
                        }
                        
                        // 跳转到歌曲开头按钮
                        OutlinedButton(
                            onClick = {
                                if (lyricLines.isNotEmpty()) {
                                    selectedLineIndex = 0
                                    selectedWordIndex = 0
                                    val firstUnit = lyricLines[0].timeUnits[0]
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
                            Text("跳转到歌曲开头")
                        }
                        
                        // 跳转到歌曲结尾按钮
                        OutlinedButton(
                            onClick = {
                                if (lyricLines.isNotEmpty()) {
                                    selectedLineIndex = lyricLines.size - 1
                                    selectedWordIndex = 0
                                    val lastLine = lyricLines[lyricLines.size - 1]
                                    if (lastLine.timeUnits.isNotEmpty()) {
                                        val lastUnit = lastLine.timeUnits[0]
                                        val timeMs = parseTimeToMs(lastUnit.startTime)
                                        currentTime = timeMs
                                        onSeekTo(timeMs)
                                    }
                                }
                                showTimeInputDialog = false
                                inputTimeMinutes = ""
                                inputTimeSeconds = ""
                                inputTimeMilliseconds = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("跳转到歌曲结尾")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = { 
                                showTimeInputDialog = false
                                inputTimeMinutes = ""
                                inputTimeSeconds = ""
                                inputTimeMilliseconds = ""
                            }
                        ) {
                            Text("取消")
                        }
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
                            Text("确定并播放")
                        }
                    }
                }
            }
        }
        
        // M4A解码对话框
        if (showConvertDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("音频转码") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "检测到不支持的音频格式，正在转码...",
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
                                text = "可能的原因：\n• 文件格式不支持写入歌词\n• 文件被其他应用占用",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    if (needStoragePermission) {
                        Button(onClick = {
                            showEmbedResultDialog = false
                            com.example.LyricBox.utils.AudioMetadataReader.requestStoragePermission(context)
                        }) {
                            Text("设置权限")
                        }
                    } else {
                        Button(onClick = { showEmbedResultDialog = false }) {
                            Text("确定")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmbedResultDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 格式化时间轴确认对话框
        if (showFormatTimelineConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showFormatTimelineConfirmDialog = false },
                title = { Text("确认格式化时间轴") },
                text = { Text("确定要格式化时间轴吗？这将按时间戳重新排序歌词，并自动合并相同时间戳的翻译。") },
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
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFormatTimelineConfirmDialog = false }) {
                        Text("取消")
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
                        Text("确定")
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
                                Text(text = "导入歌词")
                            }
                            CustomDropdownMenu(
                                expanded = showImportLyricMenu,
                                onDismissRequest = { showImportLyricMenu = false },
                                items = listOf(
                                    MenuItem(title = "通过纯文本导入", onClick = { showLyricInputDialog = true }),
                                    MenuItem(title = "通过LRC逐行/逐字歌词导入", onClick = { showSPLLrcInputDialog = true }),
                                    MenuItem(title = "通过增强LRC/ELRC逐字歌词导入", onClick = { showElrcInputDialog = true }),
                                    MenuItem(title = "通过TTML歌词导入", onClick = { showTtmlInputDialog = true }),
                                    MenuItem(title = "获取逐字歌词", onClick = {
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
                            Text(text = "导入音频")
                        }
                    }
                }
            }
            
            // 播放控制区域（已导入音频）
            if (hasAudio) {
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
                        modifier = Modifier.padding(start = 8.dp)
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
                            contentDescription = "快退${seekTimeSeconds.toInt()}秒",
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
                            Text(text = "退出跟随模式")
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
                            Text(text = "起始", maxLines = 1, softWrap = false)
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

                                    // 跳转到下一个字并设置其开始时间
                                    if (selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size - 1) {
                                        selectedWordIndex++
                                        // 设置下一个字的开始时间
                                        val nextTimeUnits = newLines[selectedLineIndex].timeUnits.toMutableList()
                                        val nextOldUnit = nextTimeUnits[selectedWordIndex]
                                        val nextNewUnit = LyricTimeUnit(nextOldUnit.text, currentTimeStr, nextOldUnit.endTime)
                                        actions.add(UndoAction(
                                            actionType = UndoActionType.TIME_CHANGE,
                                            lineIndex = selectedLineIndex,
                                            unitIndex = selectedWordIndex,
                                            oldValue = nextOldUnit,
                                            newValue = nextNewUnit
                                        ))
                                        nextTimeUnits[selectedWordIndex] = nextNewUnit
                                        newLines[selectedLineIndex] = newLines[selectedLineIndex].copy(timeUnits = nextTimeUnits)
                                    } else if (selectedLineIndex < lyricLines.size - 1) {
                                        // 向后查找有内容的行
                                        var targetLineIndex = selectedLineIndex + 1
                                        while (targetLineIndex < newLines.size) {
                                            val nextLine = newLines[targetLineIndex]
                                            if (nextLine.timeUnits.isNotEmpty()) {
                                                break
                                            }
                                            targetLineIndex++
                                        }
                                        if (targetLineIndex < newLines.size) {
                                            selectedLineIndex = targetLineIndex
                                            selectedWordIndex = 0
                                            // 设置下一行第一个字的开始时间
                                            val nextLine = newLines[selectedLineIndex]
                                            val nextTimeUnits = nextLine.timeUnits.toMutableList()
                                            val nextOldUnit = nextTimeUnits[selectedWordIndex]
                                            val nextNewUnit = LyricTimeUnit(nextOldUnit.text, currentTimeStr, nextOldUnit.endTime)
                                            actions.add(UndoAction(
                                                actionType = UndoActionType.TIME_CHANGE,
                                                lineIndex = selectedLineIndex,
                                                unitIndex = selectedWordIndex,
                                                oldValue = nextOldUnit,
                                                newValue = nextNewUnit
                                            ))
                                            nextTimeUnits[selectedWordIndex] = nextNewUnit
                                            newLines[selectedLineIndex] = nextLine.copy(timeUnits = nextTimeUnits)
                                        }
                                    }

                                    undoRedoManager.pushBatchAction(BatchUndoAction(actions, "连续设置时间戳"))
                                    updateUndoRedoState()
                                    lyricLines = newLines
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(text = "连续", maxLines = 1, softWrap = false)
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

                                    // 跳转到下一个字
                                    if (selectedWordIndex < lyricLines[selectedLineIndex].timeUnits.size - 1) {
                                        selectedWordIndex++
                                    } else if (selectedLineIndex < lyricLines.size - 1) {
                                        // 向后查找有内容的行
                                        var targetLineIndex = selectedLineIndex + 1
                                        while (targetLineIndex < lyricLines.size) {
                                            val nextLine = lyricLines[targetLineIndex]
                                            if (nextLine.timeUnits.isNotEmpty()) {
                                                break
                                            }
                                            targetLineIndex++
                                        }
                                        if (targetLineIndex < lyricLines.size) {
                                            selectedLineIndex = targetLineIndex
                                            selectedWordIndex = 0
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(text = "结束", maxLines = 1, softWrap = false)
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
                            contentDescription = "快进${seekTimeSeconds.toInt()}秒",
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
                        showAddTranslationDialog = true
                    },
                    onEditTranslation = {
                        showEditControlPanel = false
                        val targetLineIndex = when {
                            translationMenuLineIndex >= 0 && translationMenuLineIndex < lyricLines.size -> translationMenuLineIndex
                            menuLineIndex >= 0 && menuLineIndex < lyricLines.size -> menuLineIndex
                            else -> -1
                        }
                        if (targetLineIndex >= 0) {
                            menuLineIndex = targetLineIndex
                            translationMenuLineIndex = targetLineIndex
                            addTranslationText = lyricLines[targetLineIndex].translation
                            originalAddTranslationText = addTranslationText
                            showAddTranslationDialog = true
                        }
                    },
                    onDeleteLine = {
                        showEditControlPanel = false
                        showDeleteLineConfirmDialog = true
                    },
                    showAddTranslation = menuLineIndex < lyricLines.size && lyricLines[menuLineIndex].translation.isEmpty(),
                    showEditTranslation = menuLineIndex < lyricLines.size && lyricLines[menuLineIndex].translation.isNotEmpty()
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
                        text = "播放速度设置",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "当前速度: ${String.format("%.2f", tempSpeed).trimEnd('0').trimEnd('.')}X",
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
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                playbackSpeed = tempSpeed
                                onSetPlaybackSpeed(tempSpeed)
                                showSpeedDialog = false
                            }
                        ) {
                            Text("确定")
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
            title = "保存成功",
            text = "歌词文件已成功保存"
        )
        
        SimpleAlertDialog(
            showDialog = showSaveFailDialog,
            onDismiss = { showSaveFailDialog = false },
            title = "保存失败",
            text = "歌词文件保存失败，请检查存储权限或重试"
        )
        
        SimpleAlertDialog(
            showDialog = showCopiedDialog,
            onDismiss = { showCopiedDialog = false },
            title = "已复制",
            text = "内容已复制到剪贴板"
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
                onDismissRequest = {
                    if (hasUnsavedChanges()) {
                        pendingDismiss = true
                        showEditUnitCancelConfirm = true
                    } else {
                        showEditUnitDialog = false
                    }
                },
                sheetState = editUnitSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .animateContentSize(animationSpec = tween(300))
                ) {
                    Text(
                        text = "编辑歌词",
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
                                
                                // 获取旧文本中的CJK字符索引
                                val oldCjkIndices = oldText.mapIndexedNotNull { idx, char -> 
                                    if (isCJKCharacter(char)) idx else null 
                                }
                                // 获取新文本中的CJK字符索引
                                val newCjkIndices = newText.mapIndexedNotNull { idx, char -> 
                                    if (isCJKCharacter(char)) idx else null 
                                }
                                
                                // 如果CJK字符数量相同，尝试按顺序迁移注音
                                if (oldCjkIndices.size == newCjkIndices.size && oldCjkIndices.size > 0) {
                                    for (i in oldCjkIndices.indices) {
                                        val oldIdx = oldCjkIndices[i]
                                        val newIdx = newCjkIndices[i]
                                        val trans = editUnitCharTransliterations[oldIdx]
                                        if (trans != null) {
                                            newCharTransliterations[newIdx] = trans
                                        }
                                    }
                                }
                                // 否则清空注音（因为字符数量变化太大，无法确定映射关系）
                                
                                editUnitCharTransliterations = newCharTransliterations
                            }
                            editUnitText = newText
                        },
                        placeholder = "歌词内容",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 编辑注音
                    Text(
                        text = "编辑注音",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 检查是否需要显示整体注音编辑
                    val cjkCount = editUnitText.count { isCJKCharacter(it) }
                    val hasUnassignedTransliteration = editUnitTransliteration.isNotEmpty() && 
                            editUnitCharTransliterations.isEmpty()
                    
                    // 单字符注音编辑（如果有CJK字符）
                    if (cjkCount > 0) {
                        Text(
                            text = "单字符注音",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            editUnitText.forEachIndexed { index, char ->
                                if (isCJKCharacter(char)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = char.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(40.dp)
                                        )
                                        // 使用key确保正确的重组
                                        key(index) {
                                            val currentValue = editUnitCharTransliterations[index] ?: ""
                                            ThemedTextField(
                                                value = currentValue,
                                                onValueChange = { newValue ->
                                                    val newMap = editUnitCharTransliterations.toMutableMap()
                                                    if (newValue.isBlank()) {
                                                        newMap.remove(index)
                                                    } else {
                                                        newMap[index] = newValue
                                                    }
                                                    editUnitCharTransliterations = newMap
                                                },
                                                placeholder = "注音",
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // 只在有未分配的整体注音时才显示整体注音编辑
                    if (hasUnassignedTransliteration || (editUnitTransliteration.isNotEmpty() && cjkCount == 0)) {
                        Text(
                            text = "整体注音",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ThemedTextField(
                            value = editUnitTransliteration,
                            onValueChange = { editUnitTransliteration = it },
                            placeholder = "整体注音",
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
                            // 关闭按钮
                            TextButton(
                                onClick = {
                                    if (hasUnsavedChanges()) {
                                        pendingDismiss = true
                                        showEditUnitCancelConfirm = true
                                    } else {
                                        showEditUnitDialog = false
                                    }
                                }
                            ) {
                                Text("关闭")
                            }
                            
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
                title = { Text("确认放弃修改") },
                text = { Text("您已修改了歌词内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = { 
                            showEditUnitCancelConfirm = false
                            pendingDismiss = false
                        }
                    ) {
                        Text("继续编辑")
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
                        Text("放弃修改")
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
                        Text("继续编辑")
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
                onDismissRequest = {
                    if (addLyricText != originalAddLyricText || addLyricPosition != 1) {
                        pendingAddLyricDismiss = true
                        showAddLyricCancelConfirm = true
                    } else {
                        showAddLyricDialog = false
                    }
                },
                sheetState = addLyricSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "新增歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = addLyricText,
                        onValueChange = { addLyricText = it },
                        placeholder = "歌词内容（必填）",
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
                        TextButton(
                            onClick = {
                                if (addLyricText != originalAddLyricText || addLyricPosition != 1) {
                                    pendingAddLyricDismiss = true
                                    showAddLyricCancelConfirm = true
                                } else {
                                    showAddLyricDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                if (addLyricText.isNotBlank() && menuLineIndex < lyricLines.size) {
                                    val newLines = lyricLines.toMutableList()
                                    val currentLine = newLines[menuLineIndex]
                                    val newTimeUnits = currentLine.timeUnits.toMutableList()
                                    val insertIndex = if (addLyricPosition == 0) menuUnitIndex else menuUnitIndex + 1
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
                                }
                                showAddLyricDialog = false
                            },
                            enabled = addLyricText.isNotBlank()
                        ) {
                            Text("确定")
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
                title = { Text("确认放弃修改") },
                text = { Text("您已修改了内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showAddLyricCancelConfirm = false
                            pendingAddLyricDismiss = false
                        }
                    ) {
                        Text("继续编辑")
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
                        Text("放弃修改")
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
                onDismissRequest = {
                    if (addLineText != originalAddLineText || addLinePosition != 1) {
                        pendingAddLineDismiss = true
                        showAddLineCancelConfirm = true
                    } else {
                        showAddLineDialog = false
                    }
                },
                sheetState = addLineSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = 220
                            )
                        )
                ) {
                    Text(
                        text = "新增行",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ThemedTextField(
                        value = addLineText,
                        onValueChange = { addLineText = it },
                        placeholder = "歌词内容（必填）",
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
                        TextButton(
                            onClick = {
                                if (addLineText != originalAddLineText || addLinePosition != 1) {
                                    pendingAddLineDismiss = true
                                    showAddLineCancelConfirm = true
                                } else {
                                    showAddLineDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                if (addLineText.isNotBlank()) {
                                    val parsedLines = runCatching {
                                        when (detectLyricsFormat(addLineText)) {
                                            1 -> LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, addLineText).lyricLines
                                            2 -> LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, addLineText).lyricLines
                                            3 -> LyricParsingUtils.parseByType(LyricParseType.TTML, addLineText).lyricLines
                                            else -> emptyList()
                                        }
                                    }.getOrElse { emptyList() }

                                    val linesToInsert = if (parsedLines.isNotEmpty()) {
                                        parsedLines
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
                                }
                                showAddLineDialog = false
                            },
                            enabled = addLineText.isNotBlank()
                        ) {
                            Text("确定")
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
                title = { Text("确认放弃修改") },
                text = { Text("您已修改了内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showAddLineCancelConfirm = false
                            pendingAddLineDismiss = false
                        }
                    ) {
                        Text("继续编辑")
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
                        Text("放弃修改")
                    }
                }
            )
        }
        LaunchedEffect(showAddLineCancelConfirm, pendingAddLineDismiss) {
            if (!showAddLineCancelConfirm && !pendingAddLineDismiss && showAddLineDialog) {
                addLineSheetState.show()
            }
        }
        
        // 拆分歌词对话框 - ModalBottomSheet
        val splitLyricSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var pendingSplitLyricDismiss by remember { mutableStateOf(false) }
        if (showSplitLyricDialog) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (splitLyricText != originalSplitLyricText) {
                        pendingSplitLyricDismiss = true
                        showSplitLyricCancelConfirm = true
                    } else {
                        showSplitLyricDialog = false
                    }
                },
                sheetState = splitLyricSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "拆分歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text("用空格分隔歌词单元：", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemedTextField(
                        value = splitLyricText,
                        onValueChange = { splitLyricText = it },
                        placeholder = "歌词内容",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("提示：第一个空格为分隔符，连续多余空格保留到下一单元", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                if (splitLyricText != originalSplitLyricText) {
                                    pendingSplitLyricDismiss = true
                                    showSplitLyricCancelConfirm = true
                                } else {
                                    showSplitLyricDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        OutlinedButton(
                            onClick = {
                                val segmentedWords = smartSegmentLyric(splitLyricText)
                                splitLyricText = segmentedWords.joinToString(" ")
                            }
                        ) {
                            Text("一键分词", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (menuLineIndex < lyricLines.size && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
                                    val currentUnit = lyricLines[menuLineIndex].timeUnits[menuUnitIndex]
                                    val startMs = parseTimeToMs(currentUnit.startTime)
                                    val endMs = parseTimeToMs(currentUnit.endTime)
                                    val totalDuration = endMs - startMs
                                    
                                    val normalizedText = splitLyricText.replace('\u00A0', ' ')
                                    val segments = mutableListOf<String>()
                                    var currentSegment = ""
                                    var i = 0
                                    
                                    while (i < normalizedText.length) {
                                        val char = normalizedText[i]
                                        if (char == ' ') {
                                            if (currentSegment.isNotEmpty()) {
                                                segments.add(currentSegment)
                                                currentSegment = ""
                                            }
                                            i++
                                            var extraSpaces = ""
                                            while (i < normalizedText.length && normalizedText[i] == ' ') {
                                                extraSpaces += ' '
                                                i++
                                            }
                                            if (i < normalizedText.length) {
                                                currentSegment = extraSpaces
                                            }
                                        } else {
                                            currentSegment += char
                                            i++
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
                                        
                                        var accumulatedMs = startMs
                                        segments.forEachIndexed { index, segment ->
                                            val weight = segmentWeights[index]
                                            val unitDuration = if (totalWeight > 0) {
                                                (totalDuration * weight) / totalWeight
                                            } else {
                                                totalDuration / segments.size
                                            }
                                            val unitStartMs = accumulatedMs
                                            val unitEndMs = if (index == segments.size - 1) {
                                                endMs
                                            } else {
                                                accumulatedMs + unitDuration
                                            }
                                            accumulatedMs = unitEndMs
                                            
                                            newTimeUnits.add(LyricTimeUnit(
                                                segment,
                                                formatTime(unitStartMs),
                                                formatTime(unitEndMs)
                                            ))
                                        }
                                        
                                        val newLines = lyricLines.toMutableList()
                                        val currentLine = newLines[menuLineIndex]
                                        val oldTimeUnits = currentLine.timeUnits
                                        val updatedTimeUnits = currentLine.timeUnits.toMutableList()
                                        updatedTimeUnits.removeAt(menuUnitIndex)
                                        updatedTimeUnits.addAll(menuUnitIndex, newTimeUnits)
                                        newLines[menuLineIndex] = currentLine.copy(timeUnits = updatedTimeUnits)
                                        
                                        undoRedoManager.pushAction(UndoAction(
                                            actionType = UndoActionType.UNIT_SPLIT,
                                            lineIndex = menuLineIndex,
                                            unitIndex = menuUnitIndex,
                                            oldValue = oldTimeUnits,
                                            newValue = updatedTimeUnits.toList()
                                        ))
                                        updateUndoRedoState()
                                        lyricLines = newLines
                                    }
                                }
                                showSplitLyricDialog = false
                            },
                            enabled = splitLyricText.isNotBlank()
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        if (showSplitLyricCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showSplitLyricCancelConfirm = false
                    pendingSplitLyricDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认放弃修改") },
                text = { Text("您已修改了内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSplitLyricCancelConfirm = false
                            pendingSplitLyricDismiss = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSplitLyricCancelConfirm = false
                            showSplitLyricDialog = false
                            pendingSplitLyricDismiss = false
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }
        LaunchedEffect(showSplitLyricCancelConfirm, pendingSplitLyricDismiss) {
            if (!showSplitLyricCancelConfirm && !pendingSplitLyricDismiss && showSplitLyricDialog) {
                splitLyricSheetState.show()
            }
        }
        
        // 合并歌词对话框 - ModalBottomSheet
        val mergeLyricSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showMergeLyricDialog && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
            val currentLine = lyricLines[menuLineIndex]
            val displayTimeUnits = if (mergeLyricPreview.isNotEmpty()) mergeLyricPreview else currentLine.timeUnits

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (mergeLyricPreview != originalMergeLyricPreview || mergeSelectedUnits != originalMergeSelectedUnits) {
                        pendingMergeLyricDismiss = true
                        showMergeLyricCancelConfirm = true
                    } else {
                        showMergeLyricDialog = false
                        mergeLyricPreview = emptyList()
                        mergeLyricHistory = emptyList()
                    }
                },
                sheetState = mergeLyricSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "合并歌词",
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
                                        val newSelection = mergeSelectedUnits.toMutableSet()
                                        if (isSelected) {
                                            newSelection.remove(unitIndex)
                                        } else {
                                            newSelection.add(unitIndex)
                                        }
                                        mergeSelectedUnits = newSelection
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = timeUnit.text,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                if (mergeLyricPreview != originalMergeLyricPreview || mergeSelectedUnits != originalMergeSelectedUnits) {
                                    pendingMergeLyricDismiss = true
                                    showMergeLyricCancelConfirm = true
                                } else {
                                    showMergeLyricDialog = false
                                    mergeLyricPreview = emptyList()
                                    mergeLyricHistory = emptyList()
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        // 合并按钮 - 轮廓按钮
                        OutlinedButton(
                            onClick = {
                                val sortedIndices = mergeSelectedUnits.sorted()
                                if (sortedIndices.size >= 2) {
                                    val groups = mutableListOf<MutableList<Int>>()
                                    var currentGroup = mutableListOf<Int>()

                                    for (index in sortedIndices) {
                                        if (currentGroup.isEmpty() || index == currentGroup.last() + 1) {
                                            currentGroup.add(index)
                                        } else {
                                            if (currentGroup.size >= 2) {
                                                groups.add(currentGroup)
                                            }
                                            currentGroup = mutableListOf(index)
                                        }
                                    }
                                    if (currentGroup.size >= 2) {
                                        groups.add(currentGroup)
                                    }

                                    if (groups.isNotEmpty()) {
                                        mergeLyricHistory = mergeLyricHistory + listOf(displayTimeUnits)

                                        val newTimeUnits = mutableListOf<LyricTimeUnit>()
                                        var i = 0

                                        while (i < displayTimeUnits.size) {
                                            val inGroup = groups.find { group -> i in group }
                                            if (inGroup != null) {
                                                val mergedText = inGroup.map { displayTimeUnits[it].text }.joinToString("")
                                                val startTime = displayTimeUnits[inGroup.first()].startTime
                                                val endTime = displayTimeUnits[inGroup.last()].endTime
                                                newTimeUnits.add(LyricTimeUnit(mergedText, startTime, endTime))
                                                i = inGroup.last() + 1
                                            } else {
                                                newTimeUnits.add(displayTimeUnits[i])
                                                i++
                                            }
                                        }

                                        mergeLyricPreview = newTimeUnits
                                        mergeSelectedUnits = emptySet()
                                    }
                                }
                            },
                            enabled = mergeSelectedUnits.size >= 2
                        ) {
                            Text("合并")
                        }
                        // 撤销按钮 - 轮廓按钮
                        OutlinedButton(
                            onClick = {
                                if (mergeLyricHistory.isNotEmpty()) {
                                    val lastState = mergeLyricHistory.last()
                                    mergeLyricPreview = lastState
                                    mergeLyricHistory = mergeLyricHistory.dropLast(1)
                                    mergeSelectedUnits = emptySet()
                                }
                            },
                            enabled = mergeLyricHistory.isNotEmpty()
                        ) {
                            Text("撤销")
                        }
                        // 确定按钮 - 高亮按钮
                        Button(
                            onClick = {
                                val timeUnitsToApply = if (mergeLyricPreview.isNotEmpty()) mergeLyricPreview else {
                                    val sortedIndices = mergeSelectedUnits.sorted()
                                    if (sortedIndices.size >= 2) {
                                        val groups = mutableListOf<MutableList<Int>>()
                                        var currentGroup = mutableListOf<Int>()

                                        for (index in sortedIndices) {
                                            if (currentGroup.isEmpty() || index == currentGroup.last() + 1) {
                                                currentGroup.add(index)
                                            } else {
                                                if (currentGroup.size >= 2) {
                                                    groups.add(currentGroup)
                                                }
                                                currentGroup = mutableListOf(index)
                                            }
                                        }
                                        if (currentGroup.size >= 2) {
                                            groups.add(currentGroup)
                                        }

                                        if (groups.isNotEmpty()) {
                                            val timeUnits = currentLine.timeUnits
                                            val newTimeUnits = mutableListOf<LyricTimeUnit>()
                                            var i = 0

                                            while (i < timeUnits.size) {
                                                val inGroup = groups.find { group -> i in group }
                                                if (inGroup != null) {
                                                    val mergedText = inGroup.map { timeUnits[it].text }.joinToString("")
                                                    val startTime = timeUnits[inGroup.first()].startTime
                                                    val endTime = timeUnits[inGroup.last()].endTime
                                                    newTimeUnits.add(LyricTimeUnit(mergedText, startTime, endTime))
                                                    i = inGroup.last() + 1
                                                } else {
                                                    newTimeUnits.add(timeUnits[i])
                                                    i++
                                                }
                                            }
                                            newTimeUnits
                                        } else {
                                            currentLine.timeUnits
                                        }
                                    } else {
                                        currentLine.timeUnits
                                    }
                                }

                                if (mergeLyricPreview.isNotEmpty() || mergeSelectedUnits.size >= 2) {
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.UNIT_MERGE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = -1,
                                        oldValue = currentLine.timeUnits,
                                        newValue = timeUnitsToApply
                                    ))
                                    updateUndoRedoState()
                                    val newLines = lyricLines.toMutableList()
                                    newLines[menuLineIndex] = currentLine.copy(timeUnits = timeUnitsToApply)
                                    lyricLines = newLines
                                }
                                showMergeLyricDialog = false
                                mergeLyricPreview = emptyList()
                                mergeLyricHistory = emptyList()
                            },
                            enabled = mergeLyricPreview.isNotEmpty() || mergeSelectedUnits.size >= 2
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }

        // 合并歌词放弃修改确认对话框
        if (showMergeLyricCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showMergeLyricCancelConfirm = false
                    pendingMergeLyricDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认放弃修改") },
                text = { Text("您已进行了合并操作，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showMergeLyricCancelConfirm = false
                            pendingMergeLyricDismiss = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showMergeLyricCancelConfirm = false
                            showMergeLyricDialog = false
                            pendingMergeLyricDismiss = false
                            mergeLyricPreview = emptyList()
                            mergeLyricHistory = emptyList()
                            mergeSelectedUnits = emptySet()
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }

        // 处理继续编辑后重新显示合并歌词 ModalBottomSheet
        LaunchedEffect(showMergeLyricCancelConfirm, pendingMergeLyricDismiss) {
            if (!showMergeLyricCancelConfirm && !pendingMergeLyricDismiss && showMergeLyricDialog) {
                mergeLyricSheetState.show()
            }
        }
        
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
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteUnitConfirmDialog = false }) {
                        Text("取消")
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
                        Text("确定")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDeleteLineConfirmDialog = false }) {
                            Text("取消")
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
                        Text("确定删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteEmptyLinesDialog = false }) {
                        Text("取消")
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
                        Text("确定")
                    }
                }
            )
        }
        
        // 合并行对话框 - ModalBottomSheet
        val mergeLinesSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showMergeLinesDialog && menuLineIndex >= 0) {
            val mergeLinesScrollState = rememberLazyListState()
            LaunchedEffect(showMergeLinesDialog) {
                if (showMergeLinesDialog && menuLineIndex >= 0) {
                    delay(100)
                    val visibleItems = mergeLinesScrollState.layoutInfo.visibleItemsInfo.size
                    val targetItem = menuLineIndex - visibleItems / 2
                    if (targetItem > 0) {
                        mergeLinesScrollState.scrollToItem(targetItem.coerceAtLeast(0))
                    }
                }
            }

            var mergeLinesError by remember { mutableStateOf("") }

            val displayLines = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreview else lyricLines
            val displaySelected = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreviewSelected else mergeLinesSelected

            // 若歌词行超过10行，则需要修改组件高度到顶部状态栏底部
            val mergeLinesSheetHeight = if (lyricLines.size > 10) 650.dp else 400.dp

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (mergeLinesPreview != originalMergeLinesPreview || mergeLinesSelected != originalMergeLinesSelected) {
                        pendingMergeLinesDismiss = true
                        showMergeLinesCancelConfirm = true
                    } else {
                        showMergeLinesDialog = false
                        mergeLinesPreview = emptyList()
                        mergeLinesPreviewSelected = emptySet()
                        mergeLinesHistory = emptyList()
                        mergeLinesError = ""
                    }
                },
                sheetState = mergeLinesSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(mergeLinesSheetHeight)
                ) {
                    Text(
                        text = "合并行",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text("选择要合并的连续行：", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        state = mergeLinesScrollState,
                        modifier = Modifier.weight(1f)
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
                                            mergeLinesPreviewSelected = newSelection
                                        } else {
                                            mergeLinesSelected = newSelection
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${lineIndex + 1}",
                                    modifier = Modifier.padding(end = 8.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = lineText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    CustomCheckbox(
                        checked = mergeLinesAddSpace,
                        onCheckedChange = { mergeLinesAddSpace = it },
                        label = "行与行之间增加空格"
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
                        TextButton(
                            onClick = {
                                if (mergeLinesPreview != originalMergeLinesPreview || mergeLinesSelected != originalMergeLinesSelected) {
                                    pendingMergeLinesDismiss = true
                                    showMergeLinesCancelConfirm = true
                                } else {
                                    showMergeLinesDialog = false
                                    mergeLinesPreview = emptyList()
                                    mergeLinesPreviewSelected = emptySet()
                                    mergeLinesHistory = emptyList()
                                    mergeLinesError = ""
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        // 合并按钮 - 轮廓按钮
                        OutlinedButton(
                            onClick = {
                                val currentSelected = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreviewSelected else mergeLinesSelected
                                val currentLines = if (mergeLinesPreview.isNotEmpty()) mergeLinesPreview else lyricLines
                                val sortedIndices = currentSelected.sorted()

                                var isConsecutive = true
                                if (sortedIndices.size >= 2) {
                                    for (i in 0 until sortedIndices.size - 1) {
                                        if (sortedIndices[i + 1] - sortedIndices[i] != 1) {
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
                                    mergeLinesHistory = mergeLinesHistory + listOf(currentLines)

                                    val newLines = currentLines.toMutableList()
                                    val firstIndex = sortedIndices.first()
                                    val firstLine = newLines[firstIndex]
                                    val mergedTimeUnits = firstLine.timeUnits.toMutableList()

                                    sortedIndices.drop(1).forEach { idx ->
                                        val line = newLines[idx]
                                        if (mergeLinesAddSpace && line.timeUnits.isNotEmpty()) {
                                            val firstUnit = line.timeUnits.first()
                                            mergedTimeUnits.add(LyricTimeUnit(
                                                " " + firstUnit.text,
                                                firstUnit.startTime,
                                                firstUnit.endTime
                                            ))
                                            mergedTimeUnits.addAll(line.timeUnits.drop(1))
                                        } else {
                                            mergedTimeUnits.addAll(line.timeUnits)
                                        }
                                    }

                                    newLines[firstIndex] = firstLine.copy(timeUnits = mergedTimeUnits)

                                    val indicesToRemove = sortedIndices.drop(1).sortedDescending()
                                    indicesToRemove.forEach { idx ->
                                        newLines.removeAt(idx)
                                    }

                                    mergeLinesPreview = newLines
                                    mergeLinesPreviewSelected = emptySet()
                                    mergeLinesError = ""
                                }
                            }
                        ) {
                            Text("合并")
                        }
                        // 撤销按钮 - 轮廓按钮
                        OutlinedButton(
                            onClick = {
                                if (mergeLinesHistory.isNotEmpty()) {
                                    val lastState = mergeLinesHistory.last()
                                    mergeLinesPreview = lastState
                                    mergeLinesHistory = mergeLinesHistory.dropLast(1)
                                    mergeLinesPreviewSelected = emptySet()
                                    mergeLinesError = ""
                                }
                            },
                            enabled = mergeLinesHistory.isNotEmpty()
                        ) {
                            Text("撤销")
                        }
                        // 确定按钮 - 高亮按钮
                        Button(
                            onClick = {
                                if (mergeLinesPreview.isNotEmpty()) {
                                    val actions = mutableListOf<UndoAction>()
                                    val oldLines = lyricLines.toList()
                                    actions.add(UndoAction(
                                        actionType = UndoActionType.LINE_MERGE,
                                        lineIndex = -1,
                                        unitIndex = -1,
                                        oldValue = oldLines,
                                        newValue = mergeLinesPreview
                                    ))
                                    undoRedoManager.pushBatchAction(BatchUndoAction(actions, "合并行"))
                                    updateUndoRedoState()
                                    lyricLines = mergeLinesPreview
                                } else {
                                    val currentSelected = mergeLinesSelected
                                    val sortedIndices = currentSelected.sorted()

                                    var isConsecutive = true
                                    if (sortedIndices.size >= 2) {
                                        for (i in 0 until sortedIndices.size - 1) {
                                            if (sortedIndices[i + 1] - sortedIndices[i] != 1) {
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
                                                mergedTimeUnits.add(LyricTimeUnit(
                                                    " " + firstUnit.text,
                                                    firstUnit.startTime,
                                                    firstUnit.endTime
                                                ))
                                                mergedTimeUnits.addAll(line.timeUnits.drop(1))
                                            } else {
                                                mergedTimeUnits.addAll(line.timeUnits)
                                            }
                                        }

                                        newLines[firstIndex] = firstLine.copy(timeUnits = mergedTimeUnits)

                                        val indicesToRemove = sortedIndices.drop(1).sortedDescending()
                                        indicesToRemove.forEach { idx ->
                                            newLines.removeAt(idx)
                                        }

                                        actions.add(UndoAction(
                                            actionType = UndoActionType.LINE_MERGE,
                                            lineIndex = -1,
                                            unitIndex = -1,
                                            oldValue = oldLines,
                                            newValue = newLines.toList()
                                        ))
                                        undoRedoManager.pushBatchAction(BatchUndoAction(actions, "合并行"))
                                        updateUndoRedoState()
                                        lyricLines = newLines
                                    }
                                }
                                showMergeLinesDialog = false
                                mergeLinesPreview = emptyList()
                                mergeLinesPreviewSelected = emptySet()
                                mergeLinesHistory = emptyList()
                                mergeLinesError = ""
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }

        // 合并行放弃修改确认对话框
        if (showMergeLinesCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showMergeLinesCancelConfirm = false
                    pendingMergeLinesDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认放弃修改") },
                text = { Text("您已进行了合并操作，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showMergeLinesCancelConfirm = false
                            pendingMergeLinesDismiss = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showMergeLinesCancelConfirm = false
                            showMergeLinesDialog = false
                            pendingMergeLinesDismiss = false
                            mergeLinesPreview = emptyList()
                            mergeLinesPreviewSelected = emptySet()
                            mergeLinesHistory = emptyList()
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }

        // 处理继续编辑后重新显示合并行 ModalBottomSheet
        LaunchedEffect(showMergeLinesCancelConfirm, pendingMergeLinesDismiss) {
            if (!showMergeLinesCancelConfirm && !pendingMergeLinesDismiss && showMergeLinesDialog) {
                mergeLinesSheetState.show()
            }
        }
        
        // 添加翻译对话框 - ModalBottomSheet
        val addTranslationSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showCopiedButton by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        if (showAddTranslationDialog && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
            val currentLine = lyricLines[menuLineIndex]
            val lineText = currentLine.timeUnits.joinToString("") { it.text }

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (addTranslationText != originalAddTranslationText) {
                        pendingAddTranslationDismiss = true
                        showAddTranslationCancelConfirm = true
                    } else {
                        showAddTranslationDialog = false
                    }
                },
                sheetState = addTranslationSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "添加翻译",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("歌词：", fontWeight = FontWeight.Bold)
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setText(lineText)
                                showCopiedButton = true
                                coroutineScope.launch {
                                    delay(1500)
                                    showCopiedButton = false
                                }
                            }
                        ) {
                            Text(if (showCopiedButton) "已复制" else "复制")
                        }
                    }
                    Text(lineText, modifier = Modifier.padding(bottom = 8.dp))
                    ThemedTextField(
                        value = addTranslationText,
                        onValueChange = { addTranslationText = it },
                        placeholder = "请输入翻译内容",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                if (addTranslationText != originalAddTranslationText) {
                                    pendingAddTranslationDismiss = true
                                    showAddTranslationCancelConfirm = true
                                } else {
                                    showAddTranslationDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                if (menuLineIndex < lyricLines.size) {
                                    val newLines = lyricLines.toMutableList()
                                    val currentLine = newLines[menuLineIndex]
                                    val oldTranslation = currentLine.translation
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.TRANSLATION_CHANGE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = -1,
                                        oldValue = oldTranslation,
                                        newValue = addTranslationText
                                    ))
                                    updateUndoRedoState()
                                    newLines[menuLineIndex] = currentLine.copy(translation = addTranslationText)
                                    lyricLines = newLines
                                }
                                showAddTranslationDialog = false
                            },
                            enabled = addTranslationText != originalAddTranslationText
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }

        // 添加翻译放弃修改确认对话框
        if (showAddTranslationCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showAddTranslationCancelConfirm = false
                    pendingAddTranslationDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认放弃修改") },
                text = { Text("您已输入了翻译内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showAddTranslationCancelConfirm = false
                            pendingAddTranslationDismiss = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddTranslationCancelConfirm = false
                            showAddTranslationDialog = false
                            pendingAddTranslationDismiss = false
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }

        // 处理继续编辑后重新显示添加翻译 ModalBottomSheet
        LaunchedEffect(showAddTranslationCancelConfirm, pendingAddTranslationDismiss) {
            if (!showAddTranslationCancelConfirm && !pendingAddTranslationDismiss && showAddTranslationDialog) {
                addTranslationSheetState.show()
            }
        }
        
        // 移动行对话框 - ModalBottomSheet
        val moveLineSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showMoveLineDialog && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
            val currentLineText = lyricLines[menuLineIndex].timeUnits.joinToString("") { it.text }
            // 当歌词行大于10时，使用更高的高度（到顶部状态栏底部）
            val moveLineSheetHeight = if (lyricLines.size > 10) 650.dp else 400.dp

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (moveLineTargetIndex != originalMoveLineTargetIndex || moveLinePosition != originalMoveLinePosition) {
                        pendingMoveLineDismiss = true
                        showMoveLineCancelConfirm = true
                    } else {
                        showMoveLineDialog = false
                    }
                },
                sheetState = moveLineSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(moveLineSheetHeight)
                ) {
                    Text(
                        text = "移动行",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text("当前行：${menuLineIndex + 1} | $currentLineText",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp))
                    Text("请选择需要移动到哪一行：", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f)
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
                                        moveLineTargetIndex = lineIndex
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
                                        text = " (当前行)",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("位置：", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomRadioButtonGroup(
                        options = listOf("上方", "下方"),
                        selectedIndex = moveLinePosition,
                        onSelect = { moveLinePosition = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                if (moveLineTargetIndex != originalMoveLineTargetIndex || moveLinePosition != originalMoveLinePosition) {
                                    pendingMoveLineDismiss = true
                                    showMoveLineCancelConfirm = true
                                } else {
                                    showMoveLineDialog = false
                                }
                            }
                        ) {
                            Text("取消")
                        }
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

                                    newLines.add(insertIndex.coerceIn(0, newLines.size), lineToMove)
                                    lyricLines = newLines

                                    selectedLineIndex = insertIndex.coerceIn(0, lyricLines.size - 1)
                                }
                                showMoveLineDialog = false
                            },
                            enabled = moveLineTargetIndex >= 0 && moveLineTargetIndex != menuLineIndex
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }

        // 移动行放弃修改确认对话框
        if (showMoveLineCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showMoveLineCancelConfirm = false
                    pendingMoveLineDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认放弃修改") },
                text = { Text("您已修改了移动行的选择，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showMoveLineCancelConfirm = false
                            pendingMoveLineDismiss = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showMoveLineCancelConfirm = false
                            showMoveLineDialog = false
                            pendingMoveLineDismiss = false
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }

        // 处理继续编辑后重新显示移动行 ModalBottomSheet
        LaunchedEffect(showMoveLineCancelConfirm, pendingMoveLineDismiss) {
            if (!showMoveLineCancelConfirm && !pendingMoveLineDismiss && showMoveLineDialog) {
                moveLineSheetState.show()
            }
        }
        
        // 删除多行对话框 - ModalBottomSheet
        val deleteMultipleLinesSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showDeleteMultipleLinesDialog) {
            val deleteMultipleLinesSheetHeight = if (lyricLines.size > 10) 650.dp else 400.dp

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (deleteMultipleLinesSelected != originalDeleteMultipleLinesSelected) {
                        deleteMultipleLinesSelected = originalDeleteMultipleLinesSelected
                    }
                    showDeleteMultipleLinesDialog = false
                },
                sheetState = deleteMultipleLinesSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(deleteMultipleLinesSheetHeight)
                ) {
                    Text(
                        text = "删除多行",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "已选择 ${deleteMultipleLinesSelected.size} 行",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("请选择要删除的行：", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(lyricLines.size) { lineIndex ->
                            val line = lyricLines[lineIndex]
                            val lineText = line.timeUnits.joinToString("") { it.text }
                            val isSelected = lineIndex in deleteMultipleLinesSelected

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
                                        deleteMultipleLinesSelected = if (lineIndex in deleteMultipleLinesSelected) {
                                            deleteMultipleLinesSelected - lineIndex
                                        } else {
                                            deleteMultipleLinesSelected + lineIndex
                                        }
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                deleteMultipleLinesSelected = originalDeleteMultipleLinesSelected
                                showDeleteMultipleLinesDialog = false
                            }
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                if (deleteMultipleLinesSelected.isNotEmpty() && deleteMultipleLinesSelected.size < lyricLines.size) {
                                    val actions = mutableListOf<UndoAction>()
                                    val sortedIndices = deleteMultipleLinesSelected.sortedDescending()
                                    sortedIndices.forEach { index ->
                                        if (index < lyricLines.size) {
                                            actions.add(UndoAction(
                                                actionType = UndoActionType.LINE_DELETE,
                                                lineIndex = index,
                                                unitIndex = -1,
                                                oldValue = lyricLines[index],
                                                newValue = null
                                            ))
                                        }
                                    }
                                    undoRedoManager.pushBatchAction(BatchUndoAction(actions, "删除多行"))
                                    updateUndoRedoState()
                                    val newLines = lyricLines.toMutableList()
                                    sortedIndices.forEach { index ->
                                        newLines.removeAt(index)
                                    }
                                    lyricLines = newLines
                                    if (selectedLineIndex >= lyricLines.size) {
                                        selectedLineIndex = lyricLines.size - 1
                                    }
                                }
                                showDeleteMultipleLinesDialog = false
                            },
                            enabled = deleteMultipleLinesSelected.isNotEmpty() && deleteMultipleLinesSelected.size < lyricLines.size
                        ) {
                            Text("确定删除")
                        }
                    }
                }
            }
        }
        
        // 拆分为多行对话框 - ModalBottomSheet
        val splitToMultipleLinesSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showSplitToMultipleLinesDialog && menuLineIndex >= 0 && menuLineIndex < lyricLines.size) {
            var splitError by remember { mutableStateOf("") }
            val currentLine = lyricLines[menuLineIndex]

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (splitToMultipleLinesText != originalSplitToMultipleLinesText) {
                        pendingSplitToMultipleLinesDismiss = true
                        showSplitToMultipleLinesCancelConfirm = true
                    } else {
                        showSplitToMultipleLinesDialog = false
                        splitError = ""
                    }
                },
                sheetState = splitToMultipleLinesSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "拆分为多行",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text("当前歌词：", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemedTextField(
                        value = splitToMultipleLinesText,
                        onValueChange = { splitToMultipleLinesText = it },
                        placeholder = "歌词内容",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        singleLine = false,
                        minLines = 5,
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：请通过输入换行符将歌词拆分为多行。每一行将成为新的歌词行。",
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
                        TextButton(
                            onClick = {
                                if (splitToMultipleLinesText != originalSplitToMultipleLinesText) {
                                    pendingSplitToMultipleLinesDismiss = true
                                    showSplitToMultipleLinesCancelConfirm = true
                                } else {
                                    showSplitToMultipleLinesDialog = false
                                    splitError = ""
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                if (!splitToMultipleLinesText.contains("\n")) {
                                    splitError = "未检测到换行符，无法拆分"
                                    return@Button
                                }

                                val lines = splitToMultipleLinesText.split("\n").filter { it.isNotBlank() }

                                if (lines.size < 2) {
                                    splitError = "拆分后至少需要两行有效歌词"
                                    return@Button
                                }

                                val newLines = lyricLines.toMutableList()
                                val currentTimeUnits = currentLine.timeUnits

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
                                                currentTimeUnits.toMutableList()[currentTextIndex] = LyricTimeUnit(remainingPart, unit.startTime, unit.endTime)
                                                remainingText = ""
                                            } else {
                                                lineTimeUnits.add(unit)
                                                remainingText = remainingText.substring(unit.text.length.coerceAtMost(remainingText.length))
                                                currentTextIndex++
                                            }
                                        }

                                        if (lineTimeUnits.isNotEmpty()) {
                                            newLyricLines.add(LyricLine(lineTimeUnits, "", LyricAgentType.LEFT, ""))
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
                                        newLyricLines.add(LyricLine(
                                            listOf(LyricTimeUnit(lineText, "00:00.000", "00:00.000")),
                                            "",
                                            LyricAgentType.LEFT,
                                            ""
                                        ))
                                    }
                                }

                                if (newLyricLines.isNotEmpty()) {
                                    newLines.removeAt(menuLineIndex)
                                    newLines.addAll(menuLineIndex, newLyricLines)
                                    lyricLines = newLines

                                    selectedLineIndex = menuLineIndex
                                    selectedWordIndex = 0
                                }

                                showSplitToMultipleLinesDialog = false
                                splitError = ""
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }

        // 拆分为多行放弃修改确认对话框
        if (showSplitToMultipleLinesCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showSplitToMultipleLinesCancelConfirm = false
                    pendingSplitToMultipleLinesDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认放弃修改") },
                text = { Text("您已修改了歌词内容，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSplitToMultipleLinesCancelConfirm = false
                            pendingSplitToMultipleLinesDismiss = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSplitToMultipleLinesCancelConfirm = false
                            showSplitToMultipleLinesDialog = false
                            pendingSplitToMultipleLinesDismiss = false
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }

        // 处理继续编辑后重新显示拆分为多行 ModalBottomSheet
        LaunchedEffect(showSplitToMultipleLinesCancelConfirm, pendingSplitToMultipleLinesDismiss) {
            if (!showSplitToMultipleLinesCancelConfirm && !pendingSplitToMultipleLinesDismiss && showSplitToMultipleLinesDialog) {
                splitToMultipleLinesSheetState.show()
            }
        }
        
        // 一键分词对话框 - ModalBottomSheet
        val batchSegmentSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showBatchSegmentDialog && lyricLines.isNotEmpty()) {
            val segmentListHeight = if (lyricLines.size > 10) {
                400.dp
            } else {
                250.dp
            }
            
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    showBatchSegmentDialog = false
                    batchSegmentSelectedLines = emptySet()
                },
                sheetState = batchSegmentSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "一键分词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "选择需要分词的歌词行：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(segmentListHeight)
                    ) {
                        items(lyricLines.size) { index ->
                            val line = lyricLines[index]
                            val lineText = line.timeUnits.joinToString("") { it.text }
                            val isSelected = batchSegmentSelectedLines.contains(index)
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
                                        batchSegmentSelectedLines = if (isSelected) {
                                            batchSegmentSelectedLines - index
                                        } else {
                                            batchSegmentSelectedLines + index
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.padding(end = 8.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = lineText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = { 
                                showBatchSegmentDialog = false
                                batchSegmentSelectedLines = emptySet()
                            }
                        ) {
                            Text("取消")
                        }
                        OutlinedButton(
                            onClick = {
                                batchSegmentSelectedLines = lyricLines.indices.toSet()
                            }
                        ) {
                            Text("全选", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                batchSegmentSelectedLines = lyricLines.indices.toSet() - batchSegmentSelectedLines
                            }
                        ) {
                            Text("反选", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (batchSegmentSelectedLines.isNotEmpty()) {
                                    // 辅助函数：拆分歌词单元并分配注音
                                    fun segmentUnitWithTransliteration(unit: LyricTimeUnit): List<LyricTimeUnit> {
                                        val segmentedWords = smartSegmentLyric(unit.text)
                                        if (segmentedWords.size <= 1) {
                                            return listOf(unit)
                                        }
                                        
                                        val startTimeMs = parseTimeToMs(unit.startTime)
                                        val endTimeMs = parseTimeToMs(unit.endTime)
                                        val totalDuration = endTimeMs - startTimeMs
                                        
                                        // 计算每个字符在原文本中的索引位置，以便正确分配注音
                                        var charIndex = 0
                                        val wordCharRanges = segmentedWords.map { word ->
                                            val start = charIndex
                                            charIndex += word.length
                                            start until charIndex
                                        }
                                        
                                        // 根据wordCharRanges分配charTransliterations
                                        fun createNewUnit(word: String, charRange: IntRange, startMs: Long, endMs: Long): LyricTimeUnit {
                                            val newCharTransliterations = mutableMapOf<Int, String>()
                                            charRange.forEachIndexed { newIdx, oldIdx ->
                                                unit.charTransliterations[oldIdx]?.let { translit ->
                                                    newCharTransliterations[newIdx] = translit
                                                }
                                            }
                                            // 如果整个单元只有一个字符且有transliteration，也传递给新单元
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
                                        } else {
                                            return segmentedWords.mapIndexed { wordIndex, word ->
                                                val unitStartMs = if (wordIndex == 0) startTimeMs else 0L
                                                val unitEndMs = if (wordIndex == segmentedWords.size - 1) endTimeMs else 0L
                                                createNewUnit(word, wordCharRanges[wordIndex], unitStartMs, unitEndMs)
                                            }
                                        }
                                    }
                                    
                                    val actions = mutableListOf<UndoAction>()
                                    batchSegmentSelectedLines.forEach { lineIdx ->
                                        if (lineIdx < lyricLines.size) {
                                            val line = lyricLines[lineIdx]
                                            val newTimeUnits = line.timeUnits.flatMap { unit ->
                                                segmentUnitWithTransliteration(unit)
                                            }
                                            actions.add(UndoAction(
                                                actionType = UndoActionType.BATCH_SEGMENT,
                                                lineIndex = lineIdx,
                                                unitIndex = -1,
                                                oldValue = line.timeUnits,
                                                newValue = newTimeUnits
                                            ))
                                        }
                                    }
                                    undoRedoManager.pushBatchAction(BatchUndoAction(actions, "一键分词"))
                                    updateUndoRedoState()
                                    val newLyricLines = lyricLines.mapIndexed { index, line ->
                                        if (batchSegmentSelectedLines.contains(index)) {
                                            val newTimeUnits = line.timeUnits.flatMap { unit ->
                                                segmentUnitWithTransliteration(unit)
                                            }
                                            line.copy(timeUnits = newTimeUnits)
                                        } else {
                                            line
                                        }
                                    }
                                    lyricLines = newLyricLines
                                }
                                showBatchSegmentDialog = false
                                batchSegmentSelectedLines = emptySet()
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // 一键合并歌词对话框 - ModalBottomSheet
        val mergeUnitsSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showMergeUnitsDialog && lyricLines.isNotEmpty()) {
            val mergeListHeight = if (lyricLines.size > 10) {
                350.dp
            } else {
                200.dp
            }
            
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    showMergeUnitsDialog = false
                    mergeUnitsSelectedLines = emptySet()
                },
                sheetState = mergeUnitsSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "一键合并歌词",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "选择需要合并歌词单元的歌词行：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "合并阈值：±",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        var mergeThresholdMenuAnchor by remember { mutableStateOf(MenuAnchorPosition(0f, 0f)) }
                        val density = LocalDensity.current
                        Box {
                            TextButton(
                                onClick = { showMergeUnitsThresholdMenu = true },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val bounds = coordinates.boundsInRoot()
                                    mergeThresholdMenuAnchor = MenuAnchorPosition(
                                        x = with(density) { bounds.center.x.toDp().value },
                                        y = with(density) { bounds.center.y.toDp().value }
                                    )
                                }
                            ) {
                                Text("$mergeUnitsThreshold 毫秒")
                            }
                            CustomDropdownMenu(
                                expanded = showMergeUnitsThresholdMenu,
                                onDismissRequest = { showMergeUnitsThresholdMenu = false },
                                items = listOf(10L, 30L, 50L, 70L, 100L).map { threshold ->
                                    MenuItem(
                                        title = "$threshold 毫秒",
                                        onClick = {
                                            mergeUnitsThreshold = threshold
                                            showMergeUnitsThresholdMenu = false
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
                            .height(mergeListHeight)
                    ) {
                        items(lyricLines.size) { index ->
                            val line = lyricLines[index]
                            val lineText = line.timeUnits.joinToString("") { it.text }
                            val isSelected = mergeUnitsSelectedLines.contains(index)
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
                                        mergeUnitsSelectedLines = if (isSelected) {
                                            mergeUnitsSelectedLines - index
                                        } else {
                                            mergeUnitsSelectedLines + index
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.padding(end = 8.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = lineText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = { 
                                showMergeUnitsDialog = false
                                mergeUnitsSelectedLines = emptySet()
                            }
                        ) {
                            Text("取消")
                        }
                        OutlinedButton(
                            onClick = {
                                mergeUnitsSelectedLines = lyricLines.indices.toSet()
                            }
                        ) {
                            Text("全选", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                mergeUnitsSelectedLines = lyricLines.indices.toSet() - mergeUnitsSelectedLines
                            }
                        ) {
                            Text("反选", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (mergeUnitsSelectedLines.isNotEmpty()) {
                                    val actions = mutableListOf<UndoAction>()
                                    mergeUnitsSelectedLines.forEach { lineIdx ->
                                        if (lineIdx < lyricLines.size) {
                                            val line = lyricLines[lineIdx]
                                            actions.add(UndoAction(
                                                actionType = UndoActionType.BATCH_MERGE_UNITS,
                                                lineIndex = lineIdx,
                                                unitIndex = -1,
                                                oldValue = line.timeUnits,
                                                newValue = mergeCloseTimeUnits(line.timeUnits, mergeUnitsThreshold)
                                            ))
                                        }
                                    }
                                    undoRedoManager.pushBatchAction(BatchUndoAction(actions, "一键合并歌词"))
                                    updateUndoRedoState()
                                    val newLyricLines = lyricLines.mapIndexed { index, line ->
                                        if (mergeUnitsSelectedLines.contains(index)) {
                                            val mergedTimeUnits = mergeCloseTimeUnits(line.timeUnits, mergeUnitsThreshold)
                                            line.copy(timeUnits = mergedTimeUnits)
                                        } else {
                                            line
                                        }
                                    }
                                    lyricLines = newLyricLines
                                }
                                showMergeUnitsDialog = false
                                mergeUnitsSelectedLines = emptySet()
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // 平移时间戳对话框 - ModalBottomSheet
        val timestampShiftSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showTimestampShiftDialog && lyricLines.isNotEmpty()) {
            val listHeight = if (lyricLines.size > 10) {
                350.dp
            } else {
                200.dp
            }
            
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { 
                    showTimestampShiftDialog = false
                    timestampShiftSelectedLines = emptySet()
                },
                sheetState = timestampShiftSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "平移时间戳",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "选择需要平移时间戳的歌词行：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(listHeight)
                    ) {
                        items(lyricLines.size) { index ->
                            val line = lyricLines[index]
                            val lineText = line.timeUnits.joinToString("") { it.text }
                            val isSelected = timestampShiftSelectedLines.contains(index)
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
                                        timestampShiftSelectedLines = if (isSelected) {
                                            timestampShiftSelectedLines - index
                                        } else {
                                            timestampShiftSelectedLines + index
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.padding(end = 8.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = lineText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
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
                                val currentValue = timestampShiftValue.toIntOrNull() ?: 0
                                if (currentValue >= 50) {
                                    timestampShiftValue = (currentValue - 50).toString()
                                }
                            },
                            modifier = Modifier.weight(0.2f)
                        ) {
                            Text("-")
                        }
                        ThemedTextField(
                            value = timestampShiftValue,
                            onValueChange = { input: String ->
                                timestampShiftValue = filterDigits(input)
                            },
                            placeholder = "偏移值",
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val currentValue = timestampShiftValue.toIntOrNull() ?: 0
                                timestampShiftValue = (currentValue + 50).toString()
                            },
                            modifier = Modifier.weight(0.2f)
                        ) {
                            Text("+")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "单位：毫秒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { 
                                    showTimestampShiftDialog = false
                                    timestampShiftSelectedLines = emptySet()
                                }
                            ) {
                                Text("取消")
                            }
                            OutlinedButton(
                                onClick = {
                                    timestampShiftSelectedLines = lyricLines.indices.toSet()
                                }
                            ) {
                                Text("全选", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    timestampShiftSelectedLines = lyricLines.indices.toSet() - timestampShiftSelectedLines
                                }
                            ) {
                                Text("反选", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (timestampShiftSelectedLines.isNotEmpty()) {
                                        val shiftMs = timestampShiftValue.toLongOrNull() ?: 0L
                                        val actions = mutableListOf<UndoAction>()
                                        timestampShiftSelectedLines.forEach { lineIdx ->
                                            if (lineIdx < lyricLines.size) {
                                                lyricLines[lineIdx].timeUnits.forEachIndexed { unitIdx, unit ->
                                                    actions.add(UndoAction(
                                                        actionType = UndoActionType.TIME_CHANGE,
                                                        lineIndex = lineIdx,
                                                        unitIndex = unitIdx,
                                                        oldValue = unit,
                                                        newValue = unit.copy(
                                                            startTime = LyricBatchEditUtils.adjustTime(unit.startTime, -shiftMs),
                                                            endTime = LyricBatchEditUtils.adjustTime(unit.endTime, -shiftMs)
                                                        )
                                                    ))
                                                }
                                            }
                                        }
                                        undoRedoManager.pushBatchAction(BatchUndoAction(actions, "批量平移时间戳"))
                                        updateUndoRedoState()
                                        val newLyricLines = LyricBatchEditUtils.shiftTimestamps(
                                            lyricLines,
                                            timestampShiftSelectedLines,
                                            -shiftMs
                                        )
                                        lyricLines = newLyricLines
                                    }
                                    showTimestampShiftDialog = false
                                    timestampShiftSelectedLines = emptySet()
                                }
                            ) {
                                Text("提前（-）")
                            }
                            Button(
                                onClick = {
                                    if (timestampShiftSelectedLines.isNotEmpty()) {
                                        val shiftMs = timestampShiftValue.toLongOrNull() ?: 0L
                                        val actions = mutableListOf<UndoAction>()
                                        timestampShiftSelectedLines.forEach { lineIdx ->
                                            if (lineIdx < lyricLines.size) {
                                                lyricLines[lineIdx].timeUnits.forEachIndexed { unitIdx, unit ->
                                                    actions.add(UndoAction(
                                                        actionType = UndoActionType.TIME_CHANGE,
                                                        lineIndex = lineIdx,
                                                        unitIndex = unitIdx,
                                                        oldValue = unit,
                                                        newValue = unit.copy(
                                                            startTime = LyricBatchEditUtils.adjustTime(unit.startTime, shiftMs),
                                                            endTime = LyricBatchEditUtils.adjustTime(unit.endTime, shiftMs)
                                                        )
                                                    ))
                                                }
                                            }
                                        }
                                        undoRedoManager.pushBatchAction(BatchUndoAction(actions, "批量平移时间戳"))
                                        updateUndoRedoState()
                                        val newLyricLines = LyricBatchEditUtils.shiftTimestamps(
                                            lyricLines,
                                            timestampShiftSelectedLines,
                                            shiftMs
                                        )
                                        lyricLines = newLyricLines
                                    }
                                    showTimestampShiftDialog = false
                                    timestampShiftSelectedLines = emptySet()
                                }
                            ) {
                                Text("延后（+）")
                            }
                        }
                    }
                }
            }
        }

        // 转换为简体对话框 - ModalBottomSheet
        val convertToSimplifiedSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showConvertToSimplifiedDialog && lyricLines.isNotEmpty()) {
            val listHeight = if (lyricLines.size > 10) {
                350.dp
            } else {
                200.dp
            }

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    showConvertToSimplifiedDialog = false
                    convertToSimplifiedSelectedLines = emptySet()
                },
                sheetState = convertToSimplifiedSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "转换为简体",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "选择需要转换为简体的歌词行：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(listHeight)
                    ) {
                        items(lyricLines.size) { index ->
                            val line = lyricLines[index]
                            val lineText = line.timeUnits.joinToString("") { it.text }
                            val isSelected = convertToSimplifiedSelectedLines.contains(index)
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
                                        convertToSimplifiedSelectedLines = if (isSelected) {
                                            convertToSimplifiedSelectedLines - index
                                        } else {
                                            convertToSimplifiedSelectedLines + index
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.padding(end = 8.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = lineText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                showConvertToSimplifiedDialog = false
                                convertToSimplifiedSelectedLines = emptySet()
                            }
                        ) {
                            Text("取消")
                        }
                        OutlinedButton(
                            onClick = {
                                convertToSimplifiedSelectedLines = lyricLines.indices.toSet()
                            }
                        ) {
                            Text("全选", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                convertToSimplifiedSelectedLines = lyricLines.indices.toSet() - convertToSimplifiedSelectedLines
                            }
                        ) {
                            Text("反选", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (convertToSimplifiedSelectedLines.isNotEmpty()) {
                                    val actions = mutableListOf<UndoAction>()
                                    convertToSimplifiedSelectedLines.forEach { lineIdx ->
                                        if (lineIdx < lyricLines.size) {
                                            lyricLines[lineIdx].timeUnits.forEachIndexed { unitIdx, unit ->
                                                val simplifiedText = LyricBatchEditUtils.toSimplifiedText(unit.text)
                                                if (simplifiedText != unit.text) {
                                                    actions.add(UndoAction(
                                                        actionType = UndoActionType.BATCH_CONVERT_TO_SIMPLIFIED,
                                                        lineIndex = lineIdx,
                                                        unitIndex = unitIdx,
                                                        oldValue = unit,
                                                        newValue = unit.copy(text = simplifiedText)
                                                    ))
                                                }
                                            }
                                            val oldTranslation = lyricLines[lineIdx].translation
                                            val newTranslation = LyricBatchEditUtils.toSimplifiedText(oldTranslation)
                                            if (newTranslation != oldTranslation) {
                                                actions.add(UndoAction(
                                                    actionType = UndoActionType.BATCH_CONVERT_TO_SIMPLIFIED,
                                                    lineIndex = lineIdx,
                                                    unitIndex = -1,
                                                    oldValue = oldTranslation,
                                                    newValue = newTranslation
                                                ))
                                            }
                                        }
                                    }
                                    if (actions.isNotEmpty()) {
                                        undoRedoManager.pushBatchAction(BatchUndoAction(actions, "转换为简体"))
                                        updateUndoRedoState()
                                    }
                                    val newLyricLines = LyricBatchEditUtils.convertToSimplified(
                                        lyricLines,
                                        convertToSimplifiedSelectedLines
                                    )
                                    lyricLines = newLyricLines
                                }
                                showConvertToSimplifiedDialog = false
                                convertToSimplifiedSelectedLines = emptySet()
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }

        // 设置时间戳对话框 - ModalBottomSheet（整合编辑时间功能）
        val setTimestampSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (showSetTimestampDialog && menuLineIndex >= 0 && menuLineIndex < lyricLines.size && menuUnitIndex >= 0 && menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size) {
            val currentUnit = lyricLines[menuLineIndex].timeUnits[menuUnitIndex]
            
            // 时间编辑器状态
            var editingTimeInSheet by remember { mutableStateOf("") }
            var shiftValue by remember { mutableStateOf("50") }
            var isPlaying by remember { mutableStateOf(false) }
            var saveButtonText by remember { mutableStateOf("保存") }
            
            // 查找上一/下一歌词单元（跨行），跳过空行
            val prevUnitInfo: Pair<Int, Int>? = if (menuUnitIndex > 0) {
                Pair(menuLineIndex, menuUnitIndex - 1)
            } else if (menuLineIndex > 0) {
                // 向前查找有内容的行
                var targetLineIndex = menuLineIndex - 1
                while (targetLineIndex >= 0) {
                    val prevLine = lyricLines[targetLineIndex]
                    if (prevLine.timeUnits.isNotEmpty()) {
                        break
                    }
                    targetLineIndex--
                }
                if (targetLineIndex >= 0) {
                    val prevLine = lyricLines[targetLineIndex]
                    Pair(targetLineIndex, prevLine.timeUnits.size - 1)
                } else null
            } else null
            
            val nextUnitInfo: Pair<Int, Int>? = if (menuUnitIndex < lyricLines[menuLineIndex].timeUnits.size - 1) {
                Pair(menuLineIndex, menuUnitIndex + 1)
            } else if (menuLineIndex < lyricLines.size - 1) {
                // 向后查找有内容的行
                var targetLineIndex = menuLineIndex + 1
                while (targetLineIndex < lyricLines.size) {
                    val nextLine = lyricLines[targetLineIndex]
                    if (nextLine.timeUnits.isNotEmpty()) {
                        break
                    }
                    targetLineIndex++
                }
                if (targetLineIndex < lyricLines.size) {
                    val nextLine = lyricLines[targetLineIndex]
                    Pair(targetLineIndex, 0)
                } else null
            } else null
            
            var linkToPrevEndTime by remember { mutableStateOf(false) }
            var linkToNextStartTime by remember { mutableStateOf(false) }
            
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = {
                    if (tempStartTime != originalTempStartTime || tempEndTime != originalTempEndTime) {
                        pendingSetTimestampDismiss = true
                        showSetTimestampCancelConfirm = true
                    } else {
                        showSetTimestampDialog = false
                        showTimeEditorInSheet = false
                    }
                },
                sheetState = setTimestampSheetState
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
                            text = "设置时间戳",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (!showTimeEditorInSheet) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        val hasUnsavedChanges = tempStartTime != originalTempStartTime || tempEndTime != originalTempEndTime
                                        if (hasUnsavedChanges) {
                                            targetUnitInfo = prevUnitInfo
                                            showSwitchUnitConfirm = true
                                        } else {
                                            prevUnitInfo?.let { (lineIdx, unitIdx) ->
                                                menuLineIndex = lineIdx
                                                menuUnitIndex = unitIdx
                                                selectedLineIndex = lineIdx
                                                selectedWordIndex = unitIdx
                                                val unit = lyricLines[lineIdx].timeUnits[unitIdx]
                                                tempStartTime = unit.startTime
                                                tempEndTime = unit.endTime
                                                originalTempStartTime = unit.startTime
                                                originalTempEndTime = unit.endTime
                                                linkToPrevEndTime = false
                                                linkToNextStartTime = false
                                            }
                                        }
                                    },
                                    enabled = !isPlaying && prevUnitInfo != null
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.prel),
                                        contentDescription = "上一个",
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
                                            targetUnitInfo = nextUnitInfo
                                            showSwitchUnitConfirm = true
                                        } else {
                                            nextUnitInfo?.let { (lineIdx, unitIdx) ->
                                                menuLineIndex = lineIdx
                                                menuUnitIndex = unitIdx
                                                selectedLineIndex = lineIdx
                                                selectedWordIndex = unitIdx
                                                val unit = lyricLines[lineIdx].timeUnits[unitIdx]
                                                tempStartTime = unit.startTime
                                                tempEndTime = unit.endTime
                                                originalTempStartTime = unit.startTime
                                                originalTempEndTime = unit.endTime
                                                linkToPrevEndTime = false
                                                linkToNextStartTime = false
                                            }
                                        }
                                    },
                                    enabled = !isPlaying && nextUnitInfo != null
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.nextl),
                                        contentDescription = "下一个",
                                        tint = if (isPlaying || nextUnitInfo == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!showTimeEditorInSheet) {
                        // 主界面 - 显示开始/结束时间
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    editingStartTime = true
                                    editingTimeInSheet = tempStartTime
                                    showTimeEditorInSheet = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "开始时间：",
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
                                text = "歌词内容：",
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
                                    editingStartTime = false
                                    editingTimeInSheet = tempEndTime
                                    showTimeEditorInSheet = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "结束时间：",
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
                        
                        // 复选框：将当前时间应用到相邻歌词单元
                        if (prevUnitInfo != null) {
                            CustomCheckbox(
                                checked = linkToPrevEndTime,
                                onCheckedChange = { linkToPrevEndTime = it },
                                label = "将开始时间应用到上一单元的结束时间"
                            )
                        }
                        
                        if (prevUnitInfo != null && nextUnitInfo != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        if (nextUnitInfo != null) {
                            CustomCheckbox(
                                checked = linkToNextStartTime,
                                onCheckedChange = { linkToNextStartTime = it },
                                label = "将结束时间应用到下一单元的开始时间"
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            TextButton(
                                onClick = {
                                    if (tempStartTime != originalTempStartTime || tempEndTime != originalTempEndTime) {
                                        pendingSetTimestampDismiss = true
                                        showSetTimestampCancelConfirm = true
                                    } else {
                                        showSetTimestampDialog = false
                                        showTimeEditorInSheet = false
                                    }
                                }
                            ) {
                                Text("关闭")
                            }
                            Button(
                                onClick = {
                                    var updatedLines = lyricLines.toMutableList()
                                    
                                    // 更新当前单元
                                    val currentTimeUnits = updatedLines[menuLineIndex].timeUnits.toMutableList()
                                    val oldUnit = currentTimeUnits[menuUnitIndex]
                                    val newUnit = currentUnit.copy(
                                        startTime = tempStartTime,
                                        endTime = tempEndTime
                                    )
                                    undoRedoManager.pushAction(UndoAction(
                                        actionType = UndoActionType.TIME_CHANGE,
                                        lineIndex = menuLineIndex,
                                        unitIndex = menuUnitIndex,
                                        oldValue = oldUnit,
                                        newValue = newUnit
                                    ))
                                    currentTimeUnits[menuUnitIndex] = newUnit
                                    updatedLines[menuLineIndex] = updatedLines[menuLineIndex].copy(timeUnits = currentTimeUnits)
                                    
                                    // 如果勾选了"将开始时间应用到上一单元的结束时间"
                                    if (linkToPrevEndTime && prevUnitInfo != null) {
                                        val (prevLineIdx, prevUnitIdx) = prevUnitInfo
                                        val prevTimeUnits = updatedLines[prevLineIdx].timeUnits.toMutableList()
                                        val prevOldUnit = prevTimeUnits[prevUnitIdx]
                                        val prevNewUnit = prevTimeUnits[prevUnitIdx].copy(endTime = tempStartTime)
                                        undoRedoManager.pushAction(UndoAction(
                                            actionType = UndoActionType.TIME_CHANGE,
                                            lineIndex = prevLineIdx,
                                            unitIndex = prevUnitIdx,
                                            oldValue = prevOldUnit,
                                            newValue = prevNewUnit
                                        ))
                                        prevTimeUnits[prevUnitIdx] = prevNewUnit
                                        updatedLines[prevLineIdx] = updatedLines[prevLineIdx].copy(timeUnits = prevTimeUnits)
                                    }
                                    
                                    // 如果勾选了"将结束时间应用到下一单元的开始时间"
                                    if (linkToNextStartTime && nextUnitInfo != null) {
                                        val (nextLineIdx, nextUnitIdx) = nextUnitInfo
                                        val nextTimeUnits = updatedLines[nextLineIdx].timeUnits.toMutableList()
                                        val nextOldUnit = nextTimeUnits[nextUnitIdx]
                                        val nextNewUnit = nextTimeUnits[nextUnitIdx].copy(startTime = tempEndTime)
                                        undoRedoManager.pushAction(UndoAction(
                                            actionType = UndoActionType.TIME_CHANGE,
                                            lineIndex = nextLineIdx,
                                            unitIndex = nextUnitIdx,
                                            oldValue = nextOldUnit,
                                            newValue = nextNewUnit
                                        ))
                                        nextTimeUnits[nextUnitIdx] = nextNewUnit
                                        updatedLines[nextLineIdx] = updatedLines[nextLineIdx].copy(timeUnits = nextTimeUnits)
                                    }
                                    
                                    updateUndoRedoState()
                                    lyricLines = updatedLines
                                    // 更新原始时间，避免再次提示未保存
                                    originalTempStartTime = tempStartTime
                                    originalTempEndTime = tempEndTime
                                    linkToPrevEndTime = false
                                    linkToNextStartTime = false
                                    
                                    // 显示"已保存"提示
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
                        // 时间编辑界面
                        Text(
                            text = if (editingStartTime) "编辑开始时间" else "编辑结束时间",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        ThemedTextField(
                            value = editingTimeInSheet,
                            onValueChange = { input: String ->
                                editingTimeInSheet = input
                            },
                            placeholder = "时间 (mm:ss.SSS)",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 调整步进值 - 调换"-"和"+"位置
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "-"按钮现在在左边
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
                                onValueChange = { input: String ->
                                    shiftValue = filterDigits(input)
                                },
                                placeholder = "偏移值",
                                modifier = Modifier.weight(0.6f),
                                singleLine = true
                            )
                            // "+"按钮现在在右边
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
                            text = "单位：毫秒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 提前/延后按钮
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
                            ) {
                                Text("提前（-）")
                            }
                            Button(
                                onClick = {
                                    val shiftMs = shiftValue.toLongOrNull() ?: 0L
                                    editingTimeInSheet = adjustTime(editingTimeInSheet, shiftMs)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("延后（+）")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            TextButton(
                                onClick = {
                                    showTimeEditorInSheet = false
                                }
                            ) {
                                Text("返回")
                            }
                            Button(
                                onClick = {
                                    if (editingStartTime) {
                                        tempStartTime = editingTimeInSheet
                                    } else {
                                        tempEndTime = editingTimeInSheet
                                    }
                                    showTimeEditorInSheet = false
                                }
                            ) {
                                Text("确定")
                            }
                        }
                    }
                }
            }
        }
        
        // 设置时间戳放弃修改确认对话框
        if (showSetTimestampCancelConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showSetTimestampCancelConfirm = false
                    pendingSetTimestampDismiss = false
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("确认放弃修改") },
                text = { Text("您已修改了时间，确定要放弃修改吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSetTimestampCancelConfirm = false
                            pendingSetTimestampDismiss = false
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSetTimestampCancelConfirm = false
                            showSetTimestampDialog = false
                            pendingSetTimestampDismiss = false
                            showTimeEditorInSheet = false
                        }
                    ) {
                        Text("放弃修改")
                    }
                }
            )
        }
        
        // 切换歌词单元确认对话框
        if (showSwitchUnitConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showSwitchUnitConfirm = false
                    targetUnitInfo = null
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = { Text("当前数据未保存") },
                text = { Text("当前数据未保存，是否切换？") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSwitchUnitConfirm = false
                            targetUnitInfo = null
                        }
                    ) {
                        Text("继续编辑")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSwitchUnitConfirm = false
                            targetUnitInfo?.let { (lineIdx, unitIdx) ->
                                menuLineIndex = lineIdx
                                menuUnitIndex = unitIdx
                                selectedLineIndex = lineIdx
                                selectedWordIndex = unitIdx
                                val unit = lyricLines[lineIdx].timeUnits[unitIdx]
                                tempStartTime = unit.startTime
                                tempEndTime = unit.endTime
                                originalTempStartTime = unit.startTime
                                originalTempEndTime = unit.endTime
                            }
                            targetUnitInfo = null
                        }
                    ) {
                        Text("切换")
                    }
                }
            )
        }
        
        // 处理继续编辑后重新显示设置时间戳 ModalBottomSheet
        LaunchedEffect(showSetTimestampCancelConfirm, pendingSetTimestampDismiss) {
            if (!showSetTimestampCancelConfirm && !pendingSetTimestampDismiss && showSetTimestampDialog) {
                setTimestampSheetState.show()
            }
        }
        }

        // 撤销/重做悬浮窗口 - 显示在右上角
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

fun smartSegmentLyric(text: String): List<String> {
    val normalizedText = text.replace('\u00A0', ' ')
    val result = mutableListOf<String>()
    var pendingSpaces = ""
    var i = 0
    
    while (i < normalizedText.length) {
        val char = normalizedText[i]
        
        if (char == ' ') {
            pendingSpaces += char
            i++
            continue
        }
        
        if (isCJKCharacter(char)) {
            result.add(pendingSpaces + char)
            pendingSpaces = ""
            i++
        } else {
            val wordStart = i
            while (i < normalizedText.length && normalizedText[i] != ' ' && !isCJKCharacter(normalizedText[i])) {
                i++
            }
            val word = normalizedText.substring(wordStart, i)
            result.add(pendingSpaces + word)
            pendingSpaces = ""
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
        
        // 检查下一行是否是翻译
        var translation = ""
        if (i + 1 < parsedLines.size) {
            val nextLine = parsedLines[i + 1]
            val nextFirstTimeTag = nextLine.third
            
            // 如果下一行的第一个时间戳与当前行相同，则认为是翻译
            if (firstTimeTag.isNotEmpty() && firstTimeTag == nextFirstTimeTag && nextLine.second.size == 1) {
                translation = nextLine.first
                i++ // 跳过翻译行
            }
        }
        
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
        
        var translation = ""
        if (i + 1 < parsedLines.size) {
            val nextLine = parsedLines[i + 1]
            if (nextLine.isTranslation && 
                currentLine.lineTime.isNotEmpty() && 
                currentLine.lineTime == nextLine.lineTime) {
                translation = nextLine.lineText
                i++
            }
        }
        
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
fun toEnhancedLrc(lyricLines: List<LyricLine>, showDuet: Boolean): String {
    val sb = StringBuilder()
    
    for (lyricLine in lyricLines) {
        if (lyricLine.timeUnits.isEmpty()) continue
        
        val timeUnits = lyricLine.timeUnits
        val firstUnit = timeUnits.first()
        val lineTime = firstUnit.startTime
        
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
        
        if (lyricLine.translation.isNotEmpty()) {
            sb.append("[$lineTime]${lyricLine.translation}\n")
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
        
        val transSpanPattern = Regex("""<span[^>]*ttm:role="x-translation"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        val mainTransMatch = transSpanPattern.find(pContent)
        if (mainTransMatch != null) {
            mainTranslation = mainTransMatch.groupValues[1].trim()
        }
        
        val mainContentWithoutTrans = pContent
            .replace(Regex("""<span[^>]*ttm:role="x-translation"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
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
            val bgTransMatch = transSpanPattern.find(bgContent)
            if (bgTransMatch != null) {
                bgTranslation = bgTransMatch.groupValues[1].trim()
            }
            
            val bgContentWithoutTrans = bgContent
                .replace(Regex("""<span[^>]*ttm:role="x-translation"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
            
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
            val finalMainTranslation = mainTranslations[lineKey] ?: mainTranslation
            lyricLines.add(LyricLine(
                mainTimeUnits,
                finalMainTranslation,
                agentType,
                lineKey
            ))
        }
        
        if (bgTimeUnits.isNotEmpty()) {
            // 优先使用从顶部翻译标签解析出的背景翻译
            val finalBgTranslation = bgTranslations[lineKey] ?: bgTranslation
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
    
    val hasV2Agent = lyricLines.any { it.agentType == LyricAgentType.RIGHT }
    
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
        
        if (lyricLine.timeUnits.isEmpty()) {
            i++
            continue
        }
        
        val lineKey = "L$lineCounter"
        
        // 检查是否有背景歌词
        var bgLyricLine: LyricLine? = null
        if (i + 1 < lyricLines.size && lyricLines[i + 1].agentType == LyricAgentType.BACKGROUND) {
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
        
        if (lyricLine.timeUnits.isEmpty()) {
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
            if (nextLine.agentType == LyricAgentType.BACKGROUND && nextLine.timeUnits.isNotEmpty()) {
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
                
                if (nextLine.translation.isNotEmpty()) {
                    val escapedTrans = nextLine.translation
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    sb.append("<span ttm:role=\"x-translation\" xml:lang=\"zh-CN\">$escapedTrans</span>")
                }
                
                sb.append("</span>")
            }
            i++
        }
        
        if (lyricLine.translation.isNotEmpty()) {
            val escapedTrans = lyricLine.translation
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            sb.append("<span ttm:role=\"x-translation\" xml:lang=\"zh-CN\">$escapedTrans</span>")
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
    return try {
        if (audioPath.isEmpty()) {
            Log.e("LyricTiming", "Audio path is empty")
            return false
        }
        
        val audioFile = java.io.File(audioPath)
        val parentDir = audioFile.parentFile
        
        if (parentDir == null || !parentDir.exists()) {
            Log.e("LyricTiming", "Parent directory does not exist: ${audioFile.parent}")
            return false
        }
        
        val ttmlFile = java.io.File(parentDir, audioFile.nameWithoutExtension + ".ttml")
        Log.d("LyricTiming", "Saving TTML to: ${ttmlFile.absolutePath}")
        ttmlFile.writeText(ttmlContent)
        true
    } catch (e: Exception) {
        Log.e("LyricTiming", "Failed to save TTML file", e)
        false
    }
}

fun buildSavedLyricFromLines(lyricLines: List<LyricLine>): String {
    val sb = StringBuilder()
    
    lyricLines.forEachIndexed { lineIndex, lyricLine ->
        val timeUnits = lyricLine.timeUnits
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
        
        if (lyricLine.translation.isNotEmpty()) {
            val firstStartTime = if (timeUnits.isNotEmpty()) timeUnits[0].startTime else "00:00.000"
            sb.append("[$firstStartTime]${lyricLine.translation}\n")
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
                    text = "暂无歌词",
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
                    contentDescription = "字体大小",
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
                    contentDescription = "退出预览",
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
                            valueRange = 18f..40f,
                            steps = 22
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showFontSizeDialog = false
                        prefs.edit().putFloat("preview_font_size", fontSize.value).apply()
                    }) {
                        Text("确定")
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
                    text = "对唱 ",
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
        
        if (showTranslation && line.translation.isNotEmpty()) {
            Text(
                text = line.translation,
                color = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF666666),
                fontSize = fontSize * 0.7f,
                modifier = Modifier.padding(top = 4.dp)
            )
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
