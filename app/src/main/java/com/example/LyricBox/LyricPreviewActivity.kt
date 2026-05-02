package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import com.lonx.audiotag.rw.AudioTagReader
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.airbnb.lottie.compose.*
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.modifier.springPlacement
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.AudioConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private const val AUTO_SCROLL_LOG_TAG = "LyricPreviewScroll"
private const val PLAYED_LINE_BLUR_RADIUS = 3f
private const val UPCOMING_LINE_MAX_BLUR_RADIUS = 15f
private const val UPCOMING_LINE_BLUR_STEP = 4f

// ==================== 数据模型 ====================

data class NewPreviewLyricWord(
    val text: String,
    val begin: Long,      // 开始时间（毫秒）
    val end: Long,        // 结束时间（毫秒）
    val duration: Long = end - begin,
    val transliteration: String = "",
    val charTransliterations: Map<Int, String> = emptyMap()
)

data class NewPreviewLyricLine(
    val words: List<NewPreviewLyricWord>,
    val begin: Long = words.firstOrNull()?.begin ?: 0L,
    val end: Long = words.lastOrNull()?.end ?: 0L,
    val translation: String = "",
    val isDuet: Boolean = false,
    val isBackground: Boolean = false,
    val isInterlude: Boolean = false, // 新增：是否是间奏行
    val backgroundPlacement: Int = 0 // -1: 主句上方背景歌词, 1: 主句下方背景歌词, 0: 非背景/未知
)

// 检测是否为逐行歌词
fun NewPreviewLyricLine.isLineByLineLyric(): Boolean {
    // 特征4：当前歌词行只存在1个歌词单元（word）
    if (words.size == 1) {
        return true
    }
    
    // 检查其他特征
    for (word in words) {
        // 特征1：歌词无结束时间（end == 0）
        // 特征2：歌词行结束时间为0
        // 特征3：歌词中包含为0的开始或者结束时间
        if (word.end == 0L || word.begin == 0L) {
            return true
        }
    }
    
    return false
}

// 获取逐行歌词的有效结束时间
fun getEffectiveEndTime(line: NewPreviewLyricLine, nextLine: NewPreviewLyricLine?): Long {
    return if (line.end > 0L) {
        line.end
    } else {
        nextLine?.begin ?: Long.MAX_VALUE
    }
}

// ==================== 时间导航器（TimingNavigator）====================

class TimingNavigator(private val lyricLines: List<NewPreviewLyricLine>) {
    var lastMatchedIndex: Int = -1
    var lastQueryPosition: Long = -1L

    fun findTargetIndex(position: Long): Int {
        if (lyricLines.isEmpty()) return -1
        
        // 找到最后一个满足 position >= line.begin 的可滚动目标行：
        // 主句歌词 + 上方背景歌词（下方背景歌词不作为目标）
        fun isEligibleTarget(line: NewPreviewLyricLine): Boolean {
            return !line.isBackground || line.backgroundPlacement < 0
        }

        var result = -1
        var latestBegin = -1L
        
        for (i in lyricLines.indices) {
            val line = lyricLines[i]
            
            if (!isEligibleTarget(line)) continue
            
            // 检查当前时间是否大于等于该行的开始时间
            if (position >= line.begin) {
                // 找到开始时间最大的行（最后一个开始的行）
                if (line.begin > latestBegin) {
                    latestBegin = line.begin
                    result = i
                }
            }
        }
        
        // 如果没有找到任何行，返回第一个可滚动目标行
        if (result == -1) {
            for (i in lyricLines.indices) {
                if (isEligibleTarget(lyricLines[i])) {
                    result = i
                    break
                }
            }
        }
        
        lastMatchedIndex = result
        lastQueryPosition = position
        return result
    }
}

interface ILyricTiming {
    val begin: Long
    val end: Long
}

// ==================== 进度动画器 ====================

class ProgressAnimator {
    private var startWidth: Float = 0f
    private var targetWidth: Float = 0f
    private var durationNano: Long = 0L
    private var startTimeNano: Long = 0L
    
    var currentWidth: Float = 0f
        private set
    var isAnimating: Boolean = false
        private set

    fun start(target: Float, durationMs: Long) {
        startWidth = currentWidth
        targetWidth = target
        durationNano = max(1L, durationMs) * 1_000_000L
        startTimeNano = System.nanoTime()
        isAnimating = true
    }

    fun update(now: Long): Boolean {
        if (!isAnimating) return false
        
        val elapsed = (now - startTimeNano).coerceAtLeast(0L)
        if (elapsed >= durationNano) {
            currentWidth = targetWidth
            isAnimating = false
            return true
        }
        
        val progress = elapsed.toFloat() / durationNano
        currentWidth = startWidth + (targetWidth - startWidth) * progress
        return true
    }

    fun reset() {
        currentWidth = 0f
        isAnimating = false
    }
}

// ==================== 单词布局模型 ====================

data class NewPreviewWordLayout(
    val word: NewPreviewLyricWord,
    val textWidth: Float,
    val spaceWidth: Float,  // 空格宽度
    val startPosition: Float,
    val endPosition: Float,
    val charWidths: FloatArray,
    val charStartPositions: FloatArray
)

// ==================== 歌词上抬动画状态 ====================

class WordLiftAnimator {
    private val completedWords = mutableSetOf<String>() // 记录已完成上抬的歌词
    private val wordLiftProgress = mutableMapOf<String, Float>() // 每个歌词的上抬进度
    
    fun getLiftOffset(word: NewPreviewLyricWord, currentTime: Long, density: Density): Float {
        val wordKey = "${word.begin}_${word.text}"
        val liftDuration = (word.duration + 500L).coerceAtLeast(1L) // 歌词持续时间 + 500ms
        val adjustedBegin = word.begin - 200L // 提前200ms开始上抬动画
        
        // 如果播放进度回退到未播放状态（包括等于开始时间的情况），重置该歌词的上抬状态
        if (currentTime <= adjustedBegin) {
            completedWords.remove(wordKey)
            wordLiftProgress.remove(wordKey)
            return 0f
        }
        
        // 如果已经完成上抬，保持最大上抬位置
        if (completedWords.contains(wordKey)) {
            return with(density) { 2.dp.toPx() }
        }
        
        // 计算上抬进度
        val elapsed = currentTime - adjustedBegin
        val progress = (elapsed.toFloat() / liftDuration.toFloat())
            .takeIf { it.isFinite() }
            ?.coerceIn(0f, 1f)
            ?: 0f
        
        // 使用缓动函数使动画更自然
        val easedProgress = androidx.compose.animation.core.FastOutSlowInEasing.transform(progress)
        
        // 保存进度
        wordLiftProgress[wordKey] = easedProgress
        
        // 如果动画完成，标记为已完成
        if (progress >= 1f) {
            completedWords.add(wordKey)
        }
        
        return with(density) { (2.dp.toPx() * easedProgress) }
    }
    
    fun reset() {
        completedWords.clear()
        wordLiftProgress.clear()
    }
}

// ==================== Activity ====================

class LyricPreviewActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var sharedPlaybackController: MusicPlaybackController? = null
    private var useSharedPlayback: Boolean = false
    private var currentPlaybackPosition: Long = 0L
    private var playbackCompleted by mutableStateOf(false)
    private var previewAudioDuration by mutableLongStateOf(0L)
    private var previewConvertedAudioPath: String? = null
    private var isFallbackTranscoding by mutableStateOf(false)
    
    companion object {
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val EXTRA_SOURCE_AUDIO_PATH = "source_audio_path"
        const val EXTRA_LYRIC_LINES = "lyric_lines"
        const val EXTRA_TITLE = "title"
        const val EXTRA_INITIAL_POSITION = "initial_position"
        const val EXTRA_RETURN_POSITION = "return_position"
        const val EXTRA_CREATORS = "creators"
        const val EXTRA_USE_SHARED_PLAYBACK = "use_shared_playback"
        const val EXTRA_SHARED_PLAYBACK_USED = "shared_playback_used"
        const val PREFS_NAME = "LyricPreviewSettings"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_SHOW_TRANSLATION = "show_translation"
        const val KEY_INTERLUDE_ANIMATION_TYPE = "interlude_animation_type"
        const val KEY_FONT_WEIGHT = "font_weight"
        const val KEY_SHOW_TRANSLITERATION = "show_transliteration"
        const val KEY_LYRIC_BLUR = "lyric_blur"
        const val DEFAULT_FONT_SIZE = 32f
        const val DEFAULT_SHOW_TRANSLATION = true
        const val DEFAULT_FONT_WEIGHT = 400 // Normal
        const val DEFAULT_SHOW_TRANSLITERATION = true
        const val DEFAULT_LYRIC_BLUR = true
        const val ANIMATION_TYPE_DEFAULT = 0 // circle
        const val ANIMATION_TYPE_DINOSAUR = 1 // dinosaur
        private const val STATE_PLAYBACK_POSITION = "state_playback_position"
        private const val STATE_IS_PLAYING = "state_is_playing"
        
        fun createIntent(
            context: Context,
            audioPath: String,
            lyricLines: List<NewPreviewLyricLine>,
            title: String = "歌词预览",
            initialPosition: Long = 0L,
            creators: List<String> = emptyList(),
            sourceAudioPath: String = "",
            useSharedPlayback: Boolean = false
        ): Intent {
            return Intent(context, LyricPreviewActivity::class.java).apply {
                putExtra(EXTRA_AUDIO_PATH, audioPath)
                putExtra(EXTRA_SOURCE_AUDIO_PATH, sourceAudioPath)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_INITIAL_POSITION, initialPosition)
                putExtra(EXTRA_CREATORS, creators.toTypedArray())
                putExtra(EXTRA_USE_SHARED_PLAYBACK, useSharedPlayback)
                // 传递行数
                putExtra("line_count", lyricLines.size)
                // 每行的单词数
                val wordsPerLine = lyricLines.map { it.words.size }.toIntArray()
                putExtra("words_per_line", wordsPerLine)
                // 传递歌词数据
                val wordsList = lyricLines.flatMap { it.words }
                val begins = wordsList.map { it.begin }.toLongArray()
                val ends = wordsList.map { it.end }.toLongArray()
                val texts = wordsList.map { it.text }.toTypedArray()
                val transliterations = wordsList.map { it.transliteration }.toTypedArray()
                // 序列化 charTransliterations
                val charTransliterationStrings = wordsList.map { word ->
                    word.charTransliterations.entries.joinToString(";") { "${it.key}=${it.value}" }
                }.toTypedArray()
                putExtra("begins", begins)
                putExtra("ends", ends)
                putExtra("texts", texts)
                putExtra("transliterations", transliterations)
                putExtra("char_transliterations", charTransliterationStrings)
                // 传递每行的翻译和对唱信息
                val translations = lyricLines.map { it.translation }.toTypedArray()
                val isDuets = lyricLines.map { it.isDuet }.toBooleanArray()
                val isBackgrounds = lyricLines.map { it.isBackground }.toBooleanArray()
                putExtra("translations", translations)
                putExtra("is_duets", isDuets)
                putExtra("is_backgrounds", isBackgrounds)
            }
        }

        fun start(
            context: Context,
            audioPath: String,
            lyricLines: List<NewPreviewLyricLine>,
            title: String = "歌词预览",
            initialPosition: Long = 0L,
            creators: List<String> = emptyList(),
            sourceAudioPath: String = "",
            useSharedPlayback: Boolean = false
        ) {
            val intent = createIntent(
                context = context,
                audioPath = audioPath,
                lyricLines = lyricLines,
                title = title,
                initialPosition = initialPosition,
                creators = creators,
                sourceAudioPath = sourceAudioPath,
                useSharedPlayback = useSharedPlayback
            )
            context.startActivity(intent)
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
        
        val audioPath = intent.getStringExtra(EXTRA_AUDIO_PATH) ?: ""
        val sourceAudioPath = intent.getStringExtra(EXTRA_SOURCE_AUDIO_PATH) ?: audioPath
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "歌词预览"
        val intentInitialPosition = intent.getLongExtra(EXTRA_INITIAL_POSITION, 0L)
        useSharedPlayback = intent.getBooleanExtra(EXTRA_USE_SHARED_PLAYBACK, false)
        val restoredPosition = savedInstanceState?.getLong(STATE_PLAYBACK_POSITION, intentInitialPosition) ?: intentInitialPosition
        val restorePlaying = savedInstanceState?.getBoolean(STATE_IS_PLAYING, false) ?: false
        val shouldAutoPlayOnLoad = if (savedInstanceState == null) true else restorePlaying
        currentPlaybackPosition = restoredPosition
        val creators = intent.getStringArrayExtra(EXTRA_CREATORS)?.toList() ?: emptyList()

        if (useSharedPlayback) {
            sharedPlaybackController = MusicPlaybackController(applicationContext).apply { connect() }
            if (savedInstanceState != null && restoredPosition > 0L) {
                lifecycleScope.launch {
                    repeat(80) {
                        val controller = sharedPlaybackController
                        if (controller != null && controller.isReady) {
                            controller.seekTo(restoredPosition)
                            return@launch
                        }
                        delay(50L)
                    }
                }
            }
        }
        
        // 恢复歌词数据
        val lineCount = intent.getIntExtra("line_count", 0)
        val wordsPerLine = intent.getIntArrayExtra("words_per_line") ?: intArrayOf()
        val begins = intent.getLongArrayExtra("begins") ?: longArrayOf()
        val ends = intent.getLongArrayExtra("ends") ?: longArrayOf()
        val texts = intent.getStringArrayExtra("texts") ?: arrayOf()
        val transliterations = intent.getStringArrayExtra("transliterations") ?: arrayOf()
        val charTransliterationStrings = intent.getStringArrayExtra("char_transliterations") ?: arrayOf()
        val translations = intent.getStringArrayExtra("translations") ?: arrayOf()
        val isDuets = intent.getBooleanArrayExtra("is_duets") ?: booleanArrayOf()
        val isBackgrounds = intent.getBooleanArrayExtra("is_backgrounds") ?: booleanArrayOf()
        
        // 反序列化 charTransliterations 辅助函数
        fun parseCharTransliteration(str: String): Map<Int, String> {
            if (str.isEmpty()) return emptyMap()
            return str.split(";")
                .filter { it.isNotEmpty() }
                .mapNotNull { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        parts[0].toIntOrNull()?.let { key ->
                            key to parts[1]
                        }
                    } else null
                }
                .toMap()
        }
        
        // 按原始行结构重建歌词
        val lines = mutableListOf<NewPreviewLyricLine>()
        var wordIndex = 0
        
        for (lineIndex in 0 until lineCount) {
            val wordCount = wordsPerLine.getOrElse(lineIndex) { 0 }
            val lineWords = mutableListOf<NewPreviewLyricWord>()
            
            repeat(wordCount) {
                if (wordIndex < begins.size) {
                    lineWords.add(NewPreviewLyricWord(
                        text = texts.getOrElse(wordIndex) { "" },
                        begin = begins[wordIndex],
                        end = ends[wordIndex],
                        transliteration = transliterations.getOrElse(wordIndex) { "" },
                        charTransliterations = parseCharTransliteration(charTransliterationStrings.getOrElse(wordIndex) { "" })
                    ))
                    wordIndex++
                }
            }
            
            if (lineWords.isNotEmpty()) {
                lines.add(NewPreviewLyricLine(
                    words = lineWords,
                    translation = translations.getOrElse(lineIndex) { "" },
                    isDuet = isDuets.getOrElse(lineIndex) { false },
                    isBackground = isBackgrounds.getOrElse(lineIndex) { false }
                ))
            }
        }
        
        // 重新组织歌词，在主句上下插入背景歌词
        val reorganizedLines = reorganizeLyricsWithBackground(lines)
        val previewAudioPathState = mutableStateOf(audioPath)
        val previewSourceAudioPathState = mutableStateOf(sourceAudioPath)
        val previewTitleState = mutableStateOf(title)
        val previewCreatorsState = mutableStateOf(creators)
        val previewLyricLinesState = mutableStateOf(reorganizedLines)
        val previewLyricsLoadingState = mutableStateOf(useSharedPlayback && reorganizedLines.isEmpty())
        
        if (!useSharedPlayback && audioPath.isNotEmpty()) {
            loadAudio(audioPath, restoredPosition, shouldAutoPlayOnLoad)
        }

        if (useSharedPlayback) {
            lifecycleScope.launch {
                var lastResolvedPath: String? = if (previewLyricLinesState.value.isNotEmpty()) {
                    previewAudioPathState.value
                } else {
                    null
                }
                while (true) {
                    val controller = sharedPlaybackController ?: break
                    if (controller.isReady) {
                        controller.refreshProgress()
                        val nowPath = controller.currentAudioPath
                        val shouldRebuildLyrics = !nowPath.isNullOrBlank() && nowPath != lastResolvedPath
                        if (shouldRebuildLyrics) {
                            val resolvedNowPath = nowPath ?: ""
                            previewLyricsLoadingState.value = true
                            val payload = withContext(Dispatchers.IO) {
                                buildPlayerLyricPreviewPayload(resolvedNowPath)
                            }
                            val rebuiltLines = reorganizeLyricsWithBackground(payload?.lines ?: emptyList())
                            previewAudioPathState.value = resolvedNowPath
                            previewSourceAudioPathState.value = resolvedNowPath
                            previewTitleState.value = controller.currentTitle.ifBlank {
                                File(resolvedNowPath).nameWithoutExtension
                            }
                            previewCreatorsState.value = payload?.creators ?: emptyList()
                            previewLyricLinesState.value = rebuiltLines
                            previewLyricsLoadingState.value = false
                            currentPlaybackPosition = controller.positionMs
                            playbackCompleted = false
                            lastResolvedPath = resolvedNowPath
                        }
                    }
                    delay(180L)
                }
            }
        }
        
        setContent {
            歌词转换Theme {
                LyricPreviewScreen(
                    title = previewTitleState.value,
                    audioPath = previewAudioPathState.value,
                    sourceAudioPath = previewSourceAudioPathState.value,
                    lyricLines = previewLyricLinesState.value,
                    isLyricLoading = previewLyricsLoadingState.value,
                    applyInitialSeek = !useSharedPlayback,
                    creators = previewCreatorsState.value,
                    audioDuration = previewAudioDuration,
                    initialPosition = restoredPosition,
                    initialIsPlaying = if (useSharedPlayback) {
                        sharedPlaybackController?.isPlaying == true
                    } else {
                        shouldAutoPlayOnLoad
                    },
                    onBack = { 
                        // 返回时传递当前播放进度
                        returnWithPosition()
                    },
                    onPlayPause = { playing ->
                        if (useSharedPlayback) {
                            val controller = sharedPlaybackController
                            if (playing) {
                                controller?.play()
                            } else {
                                controller?.pause()
                            }
                        } else {
                            if (playing) {
                                mediaPlayer?.start()
                                playbackCompleted = false
                            } else {
                                mediaPlayer?.pause()
                            }
                        }
                    },
                    onSeekTo = { position -> 
                        if (useSharedPlayback) {
                            sharedPlaybackController?.seekTo(position)
                        } else {
                            mediaPlayer?.seekTo(position.toInt())
                        }
                        currentPlaybackPosition = position
                    },
                    getCurrentPosition = { 
                        val pos = if (useSharedPlayback) {
                            val controller = sharedPlaybackController
                            if (controller != null && controller.isReady) {
                                controller.refreshProgress()
                                controller.positionMs
                            } else {
                                currentPlaybackPosition
                            }
                        } else {
                            mediaPlayer?.currentPosition?.toLong() ?: 0L
                        }
                        currentPlaybackPosition = pos
                        pos
                    },
                    getIsPlayingState = {
                        if (useSharedPlayback) {
                            sharedPlaybackController?.isPlaying == true
                        } else {
                            mediaPlayer?.isPlaying == true
                        }
                    },
                    getAudioDuration = {
                        if (useSharedPlayback) {
                            val controller = sharedPlaybackController
                            if (controller != null && controller.isReady) {
                                controller.refreshProgress()
                                controller.durationMs.coerceAtLeast(0L)
                            } else {
                                previewAudioDuration.coerceAtLeast(0L)
                            }
                        } else {
                            previewAudioDuration.coerceAtLeast(0L)
                        }
                    },
                    playbackCompleted = playbackCompleted,
                    onPlaybackCompletedHandled = { playbackCompleted = false }
                )
            }
        }
    }
    
    private fun returnWithPosition() {
        // 返回时传递当前播放进度
        val returnIntent = Intent().apply {
            putExtra(EXTRA_RETURN_POSITION, currentPlaybackPosition)
            putExtra(EXTRA_SHARED_PLAYBACK_USED, useSharedPlayback)
        }
        setResult(RESULT_OK, returnIntent)
        if (!useSharedPlayback) {
            cleanupPreviewConvertCache()
        }
        finish()
    }
    
    private fun loadAudio(path: String, initialPosition: Long = 0L, autoPlay: Boolean = false) {
        val targetFile = File(path)
        if (!targetFile.exists()) {
            Log.w("LyricPreview", "Audio file does not exist: $path")
            previewAudioDuration = 0L
            return
        }

        val shouldForceTranscode = isAlacEncodedM4a(path)
        if (shouldForceTranscode) {
            startFallbackTranscode(path, initialPosition, autoPlay, "detected_alac_m4a")
            return
        }

        val loaded = tryLoadAudioDirect(path, initialPosition, autoPlay, allowFallback = true)
        if (!loaded) {
            startFallbackTranscode(path, initialPosition, autoPlay, "direct_load_failed")
        }
    }

    private fun tryLoadAudioDirect(
        path: String,
        initialPosition: Long,
        autoPlay: Boolean,
        allowFallback: Boolean
    ): Boolean {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener { _, what, extra ->
                    Log.e("LyricPreview", "MediaPlayer error: what=$what extra=$extra path=$path")
                    if (allowFallback) {
                        val resumePosition = mediaPlayer?.currentPosition?.toLong() ?: currentPlaybackPosition
                        startFallbackTranscode(path, resumePosition, autoPlay = true, reason = "mediaplayer_error")
                    }
                    true
                }
                setDataSource(path)
                prepare()
                if (initialPosition > 0) {
                    seekTo(initialPosition.toInt())
                }
                if (autoPlay) {
                    start()
                }
                setOnCompletionListener {
                    playbackCompleted = true
                }
            }
            previewAudioDuration = mediaPlayer?.duration?.toLong() ?: 0L
            return true
        } catch (e: Exception) {
            Log.e("LyricPreview", "Failed to load audio: $path", e)
            mediaPlayer?.release()
            mediaPlayer = null
            previewAudioDuration = 0L
        }
        return false
    }

    private fun isAlacEncodedM4a(path: String): Boolean {
        val file = File(path)
        if (!file.exists() || !file.name.lowercase().endsWith(".m4a")) {
            return false
        }
        val extractorDetected = runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(path)
                for (index in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(index)
                    val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                    if (mime.contains("alac", ignoreCase = true)) {
                        return@runCatching true
                    }
                }
                false
            } finally {
                extractor.release()
            }
        }.getOrDefault(false)
        if (extractorDetected) return true

        return try {
            FileInputStream(file).use { input ->
                val maxProbeBytes = min(1024 * 1024, file.length().toInt().coerceAtLeast(0))
                if (maxProbeBytes <= 0) return false
                val buffer = ByteArray(maxProbeBytes)
                val readCount = input.read(buffer)
                if (readCount <= 0) return false
                String(buffer, 0, readCount, Charsets.ISO_8859_1).contains("alac", ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.w("LyricPreview", "ALAC probe failed for: $path", e)
            false
        }
    }

    private fun getPreviewConvertCacheDir(): File {
        val dir = File(cacheDir, "preview_audio_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun startFallbackTranscode(
        inputPath: String,
        initialPosition: Long,
        autoPlay: Boolean,
        reason: String
    ) {
        if (isFallbackTranscoding) {
            Log.d("LyricPreview", "Transcoding already running, skip new request. reason=$reason")
            return
        }
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            Log.e("LyricPreview", "Input file missing for transcode: $inputPath")
            return
        }

        isFallbackTranscoding = true
        previewAudioDuration = 0L
        mediaPlayer?.release()
        mediaPlayer = null
        cleanupPreviewConvertCacheFiles()

        val outputFile = File(
            getPreviewConvertCacheDir(),
            "preview_transcoded_${System.currentTimeMillis()}_${inputFile.nameWithoutExtension}.wav"
        )
        previewConvertedAudioPath = outputFile.absolutePath

        Log.d("LyricPreview", "Start fallback transcode. reason=$reason input=$inputPath output=${outputFile.absolutePath}")

        AudioConverter.decodeToWav(
            inputPath = inputFile.absolutePath,
            outputPath = outputFile.absolutePath,
            callback = object : AudioConverter.ConvertCallback {
                override fun onProgress(progress: Int, time: Long) {
                    // 预览页无需进度UI，保留回调以便后续扩展
                }

                override fun onComplete(success: Boolean, message: String) {
                    runOnUiThread {
                        isFallbackTranscoding = false
                        if (!success) {
                            Log.e("LyricPreview", "Fallback transcode failed: $message")
                            if (outputFile.exists()) {
                                outputFile.delete()
                            }
                            previewConvertedAudioPath = null
                            return@runOnUiThread
                        }

                        if (!outputFile.exists()) {
                            Log.e("LyricPreview", "Fallback transcode success but output missing")
                            previewConvertedAudioPath = null
                            return@runOnUiThread
                        }

                        val loaded = tryLoadAudioDirect(
                            path = outputFile.absolutePath,
                            initialPosition = initialPosition,
                            autoPlay = autoPlay,
                            allowFallback = false
                        )
                        cleanupPreviewConvertCacheFiles(excludePath = outputFile.absolutePath)
                        if (!loaded) {
                            Log.e("LyricPreview", "Failed to play transcoded output: ${outputFile.absolutePath}")
                        }
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        isFallbackTranscoding = false
                        Log.e("LyricPreview", "Fallback transcode error: $error")
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }
                        previewConvertedAudioPath = null
                    }
                }
            }
        )
    }

    private fun cleanupPreviewConvertCache() {
        if (isFallbackTranscoding) {
            try {
                AudioConverter.cancelCurrentTask()
            } catch (e: Exception) {
                Log.w("LyricPreview", "Failed to cancel converter task", e)
            }
        }
        isFallbackTranscoding = false
        cleanupPreviewConvertCacheFiles()
        previewConvertedAudioPath = null
    }

    private fun cleanupPreviewConvertCacheFiles(excludePath: String? = null) {
        val dir = getPreviewConvertCacheDir()
        dir.listFiles()?.forEach { file ->
            if (excludePath != null && file.absolutePath == excludePath) return@forEach
            if (file.name.startsWith("preview_transcoded_") || file.name.startsWith("temp_")) {
                if (!file.delete()) {
                    Log.w("LyricPreview", "Failed to delete preview cache file: ${file.absolutePath}")
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentPos = if (useSharedPlayback) {
            val controller = sharedPlaybackController
            if (controller != null && controller.isReady) {
                controller.refreshProgress()
                controller.positionMs
            } else {
                currentPlaybackPosition
            }
        } else {
            mediaPlayer?.currentPosition?.toLong() ?: currentPlaybackPosition
        }
        outState.putLong(STATE_PLAYBACK_POSITION, currentPos)
        outState.putBoolean(
            STATE_IS_PLAYING,
            if (useSharedPlayback) sharedPlaybackController?.isPlaying == true else mediaPlayer?.isPlaying == true
        )
        currentPlaybackPosition = currentPos
    }
    
    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        sharedPlaybackController?.release()
        sharedPlaybackController = null
        if (isFinishing && !useSharedPlayback) {
            cleanupPreviewConvertCache()
        }
        super.onDestroy()
    }
}

// ==================== 检测和插入间奏行 ====================

data class EnhancedInterludeLine(
    val line: NewPreviewLyricLine,
    val nextLineIsDuet: Boolean // 记录下一句是否是对唱歌词
)

fun detectAndInsertInterludeLines(lyricLines: List<NewPreviewLyricLine>): List<NewPreviewLyricLine> {
    val result = mutableListOf<NewPreviewLyricLine>()
    
    // 先处理开头的间奏
    val firstMainLine = lyricLines.firstOrNull { !it.isBackground }
    if (firstMainLine != null && firstMainLine.begin >= 6000) {
        // 第一句开始时间距离0超过6秒，插入开头间奏（第一行例外）
        val openingInterlude = NewPreviewLyricLine(
            words = emptyList(),
            begin = 0,
            end = firstMainLine.begin,
            isInterlude = true
        )
        result.add(openingInterlude)
    }
    
    var i = 0
    while (i < lyricLines.size) {
        val currentLine = lyricLines[i]
        
        // 先添加当前行
        result.add(currentLine)
        
        // 如果是背景歌词，继续处理下一行
        if (currentLine.isBackground) {
            i++
            continue
        }
        
        // 查找下一个非背景歌词
        var nextMainLineIndex = i + 1
        while (nextMainLineIndex < lyricLines.size && lyricLines[nextMainLineIndex].isBackground) {
            nextMainLineIndex++
        }
        
        // 先把所有背景歌词都添加到结果中
        for (j in i + 1 until nextMainLineIndex) {
            result.add(lyricLines[j])
        }
        
        // 如果找到了下一个非背景歌词
        if (nextMainLineIndex < lyricLines.size) {
            val nextMainLine = lyricLines[nextMainLineIndex]
            val currentEnd = currentLine.end
            val nextBegin = nextMainLine.begin
            
            // 计算间隙时长
            val gapDuration = nextBegin - currentEnd
            
            // 只要上一行结束时间为0，就不在两句之间插入间奏行
            // （开头间奏由前面的 openingInterlude 逻辑单独处理）
            val shouldAddInterlude = if (currentEnd == 0L) {
                false
            } else {
                // 否则，如果间隙超过6秒，在背景歌词后面插入间奏行
                gapDuration >= 6000
            }
            
            if (shouldAddInterlude) {
                val interludeLine = NewPreviewLyricLine(
                    words = emptyList(),
                    begin = currentEnd,
                    end = nextBegin,
                    isInterlude = true
                )
                result.add(interludeLine)
            }
        }
        
        // 跳到下一个主歌词
        i = nextMainLineIndex
    }
    
    return result
}

// 辅助函数：获取处理后歌词列表中每个间奏行的下一句是否是对唱歌词
fun getNextLineIsDuet(lyricLines: List<NewPreviewLyricLine>, currentIndex: Int): Boolean {
    for (i in currentIndex + 1 until lyricLines.size) {
        if (!lyricLines[i].isBackground && !lyricLines[i].isInterlude) {
            return lyricLines[i].isDuet
        }
    }
    return false // 默认返回false
}

private fun pickBestPaletteSwatch(
    swatches: List<androidx.palette.graphics.Palette.Swatch>,
    score: (swatch: androidx.palette.graphics.Palette.Swatch, maxPopulation: Int) -> Float
): androidx.palette.graphics.Palette.Swatch? {
    if (swatches.isEmpty()) return null
    val maxPopulation = (swatches.maxOfOrNull { it.population } ?: 1).coerceAtLeast(1)
    return swatches.maxByOrNull { swatch -> score(swatch, maxPopulation) }
}

private fun blendColors(start: Color, end: Color, fraction: Float): Color {
    return lerp(start, end, fraction.coerceIn(0f, 1f))
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = max(foreground.luminance(), background.luminance())
    val darker = min(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun ensureReadableColor(
    candidate: Color,
    background: Color,
    fallback: Color,
    minContrast: Float
): Color {
    if (contrastRatio(candidate, background) >= minContrast) return candidate
    for (step in 1..8) {
        val mixed = blendColors(candidate, fallback, step / 8f)
        if (contrastRatio(mixed, background) >= minContrast) {
            return mixed
        }
    }
    return fallback
}

private fun getHighContrastBlackOrWhite(background: Color): Color {
    val blackContrast = contrastRatio(Color.Black, background)
    val whiteContrast = contrastRatio(Color.White, background)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

private fun mapComposeFontWeight(weight: Int): FontWeight {
    return when (weight) {
        300 -> FontWeight.Light
        400 -> FontWeight.Normal
        500 -> FontWeight.Medium
        600 -> FontWeight.SemiBold
        700 -> FontWeight.Bold
        else -> FontWeight.Normal
    }
}

private fun applyAndroidFontWeight(
    paint: android.graphics.Paint,
    fontWeight: Int
) {
    when (fontWeight) {
        300 -> {
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.isFakeBoldText = false
            paint.style = android.graphics.Paint.Style.FILL
        }
        400 -> {
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.isFakeBoldText = false
            paint.style = android.graphics.Paint.Style.FILL
        }
        500 -> {
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.isFakeBoldText = true
            paint.style = android.graphics.Paint.Style.FILL
        }
        600 -> {
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.isFakeBoldText = true
            paint.style = android.graphics.Paint.Style.FILL
        }
        700 -> {
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.isFakeBoldText = true
            paint.style = android.graphics.Paint.Style.FILL
        }
        else -> {
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.isFakeBoldText = false
            paint.style = android.graphics.Paint.Style.FILL
        }
    }
}

private fun computeLyricLineBlurRadius(
    lyricLines: List<NewPreviewLyricLine>,
    line: NewPreviewLyricLine,
    nextLine: NewPreviewLyricLine?,
    lineIndex: Int,
    currentPlayingIndex: Int,
    currentTime: Long,
    isBlurEnabled: Boolean
): Float {
    if (!isBlurEnabled) return 0f
    if (line.isInterlude) return 0f
    if (lineIndex == currentPlayingIndex) return 0f

    val effectiveEnd = getEffectiveEndTime(line, nextLine)
    val isLineActive = currentTime >= line.begin && currentTime < effectiveEnd
    if (isLineActive) return 0f

    // 主句播放期间，其下方背景歌词也视为“当前行”，不做模糊
    if (line.isBackground && line.backgroundPlacement > 0) {
        val (mainLine, _, mainLineIndex) = findBackgroundAssociatedInfo(lyricLines, lineIndex)
        if (mainLine != null && mainLineIndex != null) {
            val mainNextLine = lyricLines.getOrNull(mainLineIndex + 1)
            val mainEffectiveEnd = getEffectiveEndTime(mainLine, mainNextLine)
            val isMainActive = currentTime >= mainLine.begin && currentTime < mainEffectiveEnd
            if (isMainActive) return 0f
        }
    }

    val isLinePlayed = currentTime >= effectiveEnd
    if (isLinePlayed) return PLAYED_LINE_BLUR_RADIUS

    val distanceFromCurrent = if (currentPlayingIndex >= 0) {
        (lineIndex - currentPlayingIndex).coerceAtLeast(1)
    } else {
        1
    }

    val targetBlur = PLAYED_LINE_BLUR_RADIUS + (distanceFromCurrent - 1) * UPCOMING_LINE_BLUR_STEP
    return targetBlur.coerceIn(PLAYED_LINE_BLUR_RADIUS, UPCOMING_LINE_MAX_BLUR_RADIUS)
}

@Composable
private fun rememberLyricLineBlurModifier(blurRadius: Float): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || blurRadius <= 0f) {
        return Modifier
    }

    val safeBlur = blurRadius.coerceIn(0f, UPCOMING_LINE_MAX_BLUR_RADIUS)
    val blurEffect = remember(safeBlur) {
        android.graphics.RenderEffect
            .createBlurEffect(safeBlur, safeBlur, android.graphics.Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }

    return Modifier.graphicsLayer {
        // 与外层 springPlacement 解耦，避免位移动画过程中的模糊采样异常
        compositingStrategy = CompositingStrategy.Offscreen
        clip = false
        renderEffect = blurEffect
    }
}

// ==================== 预览界面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricPreviewScreen(
    title: String,
    audioPath: String,
    sourceAudioPath: String,
    lyricLines: List<NewPreviewLyricLine>,
    isLyricLoading: Boolean = false,
    applyInitialSeek: Boolean = true,
    creators: List<String> = emptyList(),
    audioDuration: Long,
    initialPosition: Long = 0L,
    initialIsPlaying: Boolean = false,
    onBack: () -> Unit,
    onPlayPause: (Boolean) -> Unit,
    onSeekTo: (Long) -> Unit,
    getCurrentPosition: () -> Long,
    getIsPlayingState: () -> Boolean,
    getAudioDuration: () -> Long,
    playbackCompleted: Boolean = false,
    onPlaybackCompletedHandled: () -> Unit = {}
) {
    // 使用 BackHandler 拦截系统返回事件
    androidx.activity.compose.BackHandler(enabled = true) {
        onBack()
    }
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE) }
    val appPrefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    val supportsLyricBlur = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    
    var isPlaying by remember(initialIsPlaying) { mutableStateOf(initialIsPlaying) }
    var currentTime by remember { mutableStateOf(initialPosition) }
    var dynamicDuration by remember(audioDuration) { mutableLongStateOf(audioDuration.coerceAtLeast(0L)) }
    var showTranslation by remember { 
        mutableStateOf(prefs.getBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLATION, LyricPreviewActivity.DEFAULT_SHOW_TRANSLATION)) 
    }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat(LyricPreviewActivity.KEY_FONT_SIZE, LyricPreviewActivity.DEFAULT_FONT_SIZE)) }
    var animationType by remember { mutableIntStateOf(prefs.getInt(LyricPreviewActivity.KEY_INTERLUDE_ANIMATION_TYPE, LyricPreviewActivity.ANIMATION_TYPE_DEFAULT)) }
    var fontWeight by remember { mutableIntStateOf(prefs.getInt(LyricPreviewActivity.KEY_FONT_WEIGHT, LyricPreviewActivity.DEFAULT_FONT_WEIGHT)) }
    var showTransliteration by remember { 
        mutableStateOf(prefs.getBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLITERATION, LyricPreviewActivity.DEFAULT_SHOW_TRANSLITERATION)) 
    }
    var lyricBlurPreferenceEnabled by remember {
        mutableStateOf(
            if (supportsLyricBlur) {
                prefs.getBoolean(LyricPreviewActivity.KEY_LYRIC_BLUR, LyricPreviewActivity.DEFAULT_LYRIC_BLUR)
            } else {
                false
            }
        )
    }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showFontWeightDialog by remember { mutableStateOf(false) }
    var showAnimationTypeDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var metadata by remember { mutableStateOf(PreviewAudioMetadata(title, "未知艺术家", null)) }
    var coverThemeColor by remember { mutableStateOf<Color?>(null) }
    val scope = rememberCoroutineScope()
    
    // 获取深浅模式设置
    val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val settingsDarkMode = com.example.LyricBox.ui.theme.getDarkModeFromSettings(context)
    val isDarkTheme = settingsDarkMode ?: systemDarkTheme
    
    // 加载音频元数据
    LaunchedEffect(sourceAudioPath, title) {
        scope.launch {
            metadata = loadAudioMetadata(context, sourceAudioPath, title)
        }
    }
    
    // 提取封面颜色
    LaunchedEffect(metadata.coverBitmap, isDarkTheme) {
        val cover = metadata.coverBitmap
        if (cover == null) {
            coverThemeColor = null
            return@LaunchedEffect
        }
        coverThemeColor = withContext(Dispatchers.IO) {
            extractMutedCoverColor(cover, preferDark = isDarkTheme)
        }
    }
    
    LaunchedEffect(playbackCompleted) {
        if (playbackCompleted) {
            isPlaying = false
            onPlaybackCompletedHandled()
        }
    }
    
    // 读取打轴界面的快进快退设置
    val seekTimeSeconds by remember { mutableFloatStateOf(appPrefs.getFloat("seekTimeSeconds", 2f)) }
    val seekTimeMs = (seekTimeSeconds * 1000).toLong()
    
    // 处理歌词：检测并插入间奏行
    val processedLyricLines = remember(lyricLines) {
        detectAndInsertInterludeLines(lyricLines)
    }
    
    // 懒列表状态
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 时间导航器 - 修复结束时间为0的情况
    val lineNavigator = remember(processedLyricLines) {
        TimingNavigator(processedLyricLines)
    }
    
    // 用户滑动检测
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var autoScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var pendingAutoScrollIndex by remember { mutableIntStateOf(-1) }
    var autoScrollRunningTarget by remember { mutableIntStateOf(-1) }
    var lastAutoScrollWallTime by remember { mutableLongStateOf(0L) }
    var lastAutoScrolledIndex by remember { mutableIntStateOf(-1) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var isLyricBlurEnabled by remember { mutableStateOf(lyricBlurPreferenceEnabled) }
    // seek 重置触发器 - 使用计数器确保每次 seek 都能被唯一识别
    var seekResetCounter by remember { mutableStateOf(0L) }
    // 记录最后一次尝试滚动但被跳过的行索引
    var lastSkippedScrollIndex by remember { mutableIntStateOf(-1) }
    var closeNextSkipStreak by remember { mutableIntStateOf(0) }
    var previousObservedTime by remember { mutableLongStateOf(initialPosition) }
    var hidePlayedLinesDuringInitialBuild by remember(processedLyricLines, initialPosition) {
        mutableStateOf(initialPosition > 0L && processedLyricLines.isNotEmpty())
    }
    var initialBuildTargetIndex by remember(processedLyricLines, initialPosition) { mutableIntStateOf(-1) }
    var lyricsContentReady by remember(processedLyricLines, isLyricLoading) {
        mutableStateOf(processedLyricLines.isEmpty() && !isLyricLoading)
    }
    val maxCloseNextConsecutiveSkips = 2
    val autoScrollMinIntervalMs = 700L
    fun logAutoScroll(message: String) {
        Log.d(AUTO_SCROLL_LOG_TAG, message)
    }

    fun requestAutoScroll(targetIndex: Int) {
        if (targetIndex < 0 || targetIndex >= processedLyricLines.size) return
        if (lyricBlurPreferenceEnabled && !isLyricBlurEnabled) {
            isLyricBlurEnabled = true
            logAutoScroll("restore lyric blur at auto-scroll target=$targetIndex")
        }
        logAutoScroll(
            "request target=$targetIndex currentVisible=${lazyListState.firstVisibleItemIndex}" +
                " running=$autoScrollRunningTarget pending=$pendingAutoScrollIndex activeJob=${autoScrollJob?.isActive == true}"
        )

        if (targetIndex == autoScrollRunningTarget || targetIndex == pendingAutoScrollIndex) {
            logAutoScroll("ignore duplicated target=$targetIndex")
            return
        }
        if (autoScrollJob?.isActive == true) {
            pendingAutoScrollIndex = targetIndex
            logAutoScroll("queue target=$targetIndex (job is active)")
            return
        }

        autoScrollRunningTarget = targetIndex
        autoScrollJob = coroutineScope.launch {
            try {
                var nextTarget = targetIndex
                while (nextTarget >= 0) {
                    val now = SystemClock.elapsedRealtime()
                    val isRapidAutoScroll = (now - lastAutoScrollWallTime) < autoScrollMinIntervalMs
                    logAutoScroll(
                        "execute target=$nextTarget mode=${if (isRapidAutoScroll) "instant" else "animate"} " +
                            "deltaSinceLast=${now - lastAutoScrollWallTime}ms"
                    )
                    if (isRapidAutoScroll) {
                        lazyListState.scrollToItem(
                            index = nextTarget,
                            scrollOffset = -100
                        )
                    } else {
                        lazyListState.animateScrollToItem(
                            index = nextTarget,
                            scrollOffset = -100
                        )
                    }
                    lastAutoScrollWallTime = now
                    logAutoScroll("complete target=$nextTarget visible=${lazyListState.firstVisibleItemIndex}")

                    val pending = pendingAutoScrollIndex
                    if (pending >= 0 && pending != nextTarget) {
                        pendingAutoScrollIndex = -1
                        autoScrollRunningTarget = pending
                        nextTarget = pending
                        logAutoScroll("drain pending target=$pending")
                    } else {
                        pendingAutoScrollIndex = -1
                        autoScrollRunningTarget = -1
                        nextTarget = -1
                    }
                }
            } catch (e: CancellationException) {
                logAutoScroll("cancelled running=$autoScrollRunningTarget pending=$pendingAutoScrollIndex")
                throw e
            }
        }
    }

    // 初始化歌词列表定位：共享播放模式只跟随当前进度，不做强制 seek，避免回退与中断。
    LaunchedEffect(processedLyricLines, initialPosition, applyInitialSeek, isLyricLoading) {
        if (isLyricLoading) {
            lyricsContentReady = false
            hidePlayedLinesDuringInitialBuild = false
            initialBuildTargetIndex = -1
            return@LaunchedEffect
        }

        if (processedLyricLines.isEmpty()) {
            initialBuildTargetIndex = -1
            hidePlayedLinesDuringInitialBuild = false
            lyricsContentReady = true
            return@LaunchedEffect
        }

        lyricsContentReady = false
        val anchorPosition = if (applyInitialSeek) {
            initialPosition.coerceAtLeast(0L)
        } else {
            getCurrentPosition().coerceAtLeast(0L)
        }

        if (applyInitialSeek && anchorPosition > 0L) {
            onSeekTo(anchorPosition)
        }

        currentTime = anchorPosition
        hidePlayedLinesDuringInitialBuild = anchorPosition > 0L
        val targetIndex = lineNavigator.findTargetIndex(anchorPosition)
        initialBuildTargetIndex = targetIndex
        if (targetIndex >= 0) {
            lazyListState.scrollToItem(index = targetIndex, scrollOffset = -100)
            lastAutoScrolledIndex = targetIndex
        }

        withFrameNanos { }
        withFrameNanos { }
        delay(120L)
        hidePlayedLinesDuringInitialBuild = false
        lyricsContentReady = true
    }

    // 监听用户滑动交互
    LaunchedEffect(lazyListState.interactionSource) {
        lazyListState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press, is DragInteraction.Start -> {
                    isUserScrolling = true
                    if (interaction is DragInteraction.Start && lyricBlurPreferenceEnabled && isLyricBlurEnabled) {
                        isLyricBlurEnabled = false
                        logAutoScroll("disable lyric blur while user dragging list")
                    }
                    scrollJob?.cancel()
                    autoScrollJob?.cancel()
                    pendingAutoScrollIndex = -1
                    autoScrollRunningTarget = -1
                    logAutoScroll("user scroll started; cancel auto-scroll queue")
                }
                is PressInteraction.Release, is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    scrollJob = coroutineScope.launch {
                        delay(3000)
                        isUserScrolling = false
                    }
                }
            }
        }
    }

    // 更新当前播放行索引
    LaunchedEffect(currentTime, processedLyricLines) {
        val isExternalBackwardSeek = currentTime + 800L < previousObservedTime
        if (isExternalBackwardSeek) {
            isUserScrolling = false
            scrollJob?.cancel()
            autoScrollJob?.cancel()
            pendingAutoScrollIndex = -1
            autoScrollRunningTarget = -1
            lastAutoScrolledIndex = -1
            lastSkippedScrollIndex = -1
            closeNextSkipStreak = 0
            seekResetCounter += 1
            if (processedLyricLines.isNotEmpty()) {
                val targetIndex = lineNavigator.findTargetIndex(currentTime)
                if (targetIndex >= 0) {
                    coroutineScope.launch {
                        lazyListState.scrollToItem(index = targetIndex, scrollOffset = -100)
                    }
                }
            }
            logAutoScroll("detected external backward seek from=$previousObservedTime to=$currentTime, reset auto-scroll state")
        }

        if (processedLyricLines.isNotEmpty()) {
            currentLineIndex = lineNavigator.findTargetIndex(currentTime)
        }
        previousObservedTime = currentTime
    }
    
    // 保存字体大小设置
    fun saveFontSize(size: Float) {
        fontSize = size
        prefs.edit().putFloat(LyricPreviewActivity.KEY_FONT_SIZE, size).apply()
    }
    
    // 保存翻译显示设置
    fun saveShowTranslation(show: Boolean) {
        showTranslation = show
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLATION, show).apply()
    }
    
    // 保存字体粗细设置
    fun saveFontWeight(weight: Int) {
        fontWeight = weight
        prefs.edit().putInt(LyricPreviewActivity.KEY_FONT_WEIGHT, weight).apply()
    }
    
    // 保存注音显示设置
    fun saveShowTransliteration(show: Boolean) {
        showTransliteration = show
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLITERATION, show).apply()
    }

    fun saveLyricBlurEnabled(enabled: Boolean) {
        lyricBlurPreferenceEnabled = enabled
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_LYRIC_BLUR, enabled).apply()
        isLyricBlurEnabled = enabled
    }
    
    // 更新当前时间
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentPosition()
            isPlaying = getIsPlayingState()
            dynamicDuration = getAudioDuration().coerceAtLeast(0L)
            delay(16)
        }
    }
    
    val autoScrollLeadMs = 400L
    val autoScrollConflictWindowMs = 1000L

    fun shouldSkipAutoScrollDueToCloseNextTrigger(currentMainIndex: Int, referenceTimeMs: Long): Boolean {
        val currentLine = processedLyricLines.getOrNull(currentMainIndex) ?: return false
        // 上方背景歌词要按本行触发滚动，不受“下一次触发过近”规则抑制
        if (currentLine.isBackground && currentLine.backgroundPlacement < 0) {
            logAutoScroll("keep upper-background scroll index=$currentMainIndex (ignore close-next rule)")
            return false
        }

        var nextMainLineIndex = currentMainIndex + 1
        while (nextMainLineIndex < processedLyricLines.size &&
            (processedLyricLines[nextMainLineIndex].isBackground || processedLyricLines[nextMainLineIndex].isInterlude)
        ) {
            nextMainLineIndex++
        }
        if (nextMainLineIndex >= processedLyricLines.size) return false

        val nextMainLine = processedLyricLines[nextMainLineIndex]
        val nextTriggerTime = nextMainLine.begin - autoScrollLeadMs
        val triggerInterval = nextTriggerTime - referenceTimeMs
        if (triggerInterval < autoScrollConflictWindowMs) {
            logAutoScroll(
                "skip due close next: current=$currentMainIndex next=$nextMainLineIndex " +
                    "interval=${triggerInterval}ms ref=$referenceTimeMs nextTrigger=$nextTriggerTime"
            )
        }
        return triggerInterval < autoScrollConflictWindowMs
    }

    // 自动滚动到当前播放行（屏幕1/4位置）
    LaunchedEffect(currentTime, isUserScrolling) {
        if (!isUserScrolling && processedLyricLines.isNotEmpty()) {
            // 提前400ms计算目标行，这样可以更早地切换到下一行歌词行
            val adjustedTime = currentTime + autoScrollLeadMs
            val candidateLineIndex = lineNavigator.findTargetIndex(adjustedTime)
            val currentLineIndex = if (
                candidateLineIndex >= 0 &&
                lastAutoScrolledIndex >= 0 &&
                candidateLineIndex < lastAutoScrolledIndex
            ) {
                logAutoScroll(
                    "ignore backward target candidate=$candidateLineIndex last=$lastAutoScrolledIndex at=$currentTime"
                )
                if (lastSkippedScrollIndex >= 0 && lastSkippedScrollIndex < lastAutoScrolledIndex) {
                    logAutoScroll(
                        "drop stale skipped index=$lastSkippedScrollIndex because lastAutoScrolledIndex=$lastAutoScrolledIndex"
                    )
                    lastSkippedScrollIndex = -1
                }
                lastAutoScrolledIndex
            } else {
                candidateLineIndex
            }
            
            if (currentLineIndex >= 0 && currentLineIndex != lastAutoScrolledIndex) {
                val targetLine = processedLyricLines[currentLineIndex]
                
                // 检查是否应该触发自动滚动（只有两种情况不触发）
                var shouldScroll = true
                var skipBecauseCloseNextTrigger = false
                
                // 情况一：间奏行不触发；背景歌词仅上方背景可触发，下方背景不触发
                if (targetLine.isInterlude) {
                    shouldScroll = false
                } else if (targetLine.isBackground) {
                    val isUpperBackground = targetLine.backgroundPlacement < 0
                    if (!isUpperBackground) {
                        shouldScroll = false
                    }
                }
                
                // 情况二：上一句主句歌词结束时间减去当前行开始时间差大于1.55秒 → 不触发
                var hasLargeTimeDiff = false
                if (shouldScroll && currentLineIndex > 0) {
                    // 找到上一句主句歌词（跳过背景歌词）
                    var prevMainLineIndex = currentLineIndex - 1
                    while (prevMainLineIndex >= 0 && (processedLyricLines[prevMainLineIndex].isBackground || processedLyricLines[prevMainLineIndex].isInterlude)) {
                        prevMainLineIndex--
                    }
                    
                    if (prevMainLineIndex >= 0) {
                        val previousLine = processedLyricLines[prevMainLineIndex]
                        val prevNextLine = if (prevMainLineIndex < processedLyricLines.size - 1) processedLyricLines[prevMainLineIndex + 1] else null
                        val prevEndTime = getEffectiveEndTime(previousLine, prevNextLine)
                        val currentStartTime = targetLine.begin
                        val timeDiff = prevEndTime - currentStartTime
                        if (timeDiff > 1550L) { // 1.55秒 = 1550毫秒
                            shouldScroll = false
                            hasLargeTimeDiff = true
                            logAutoScroll(
                                "skip auto-scroll current=$currentLineIndex reason=largeTimeDiff " +
                                    "prevEnd=$prevEndTime currentBegin=$currentStartTime diff=${timeDiff}ms"
                            )
                        }
                    }
                }

                // 情况三：如果下一次自动滚动触发间隔 < 1000ms，跳过当前行，仅让下一次触发生效
                if (shouldScroll && shouldSkipAutoScrollDueToCloseNextTrigger(currentLineIndex, currentTime)) {
                    if (closeNextSkipStreak >= maxCloseNextConsecutiveSkips) {
                        logAutoScroll("force scroll current=$currentLineIndex after close-next skip streak=$closeNextSkipStreak")
                        closeNextSkipStreak = 0
                    } else {
                        shouldScroll = false
                        skipBecauseCloseNextTrigger = true
                        closeNextSkipStreak += 1
                    }
                }
                
                // 如果应该滚动，直接滚动
                if (shouldScroll) {
                    closeNextSkipStreak = 0
                    lastAutoScrolledIndex = currentLineIndex
                    lastSkippedScrollIndex = -1
                    requestAutoScroll(currentLineIndex)
                } else {
                    if (skipBecauseCloseNextTrigger) {
                        // 间隔过短时，明确丢弃当前次触发，避免补滚动把它拉回来
                        lastSkippedScrollIndex = -1
                        logAutoScroll("drop current=$currentLineIndex due close-next window")
                    } else {
                        // 其余情况保留补滚动机制
                        closeNextSkipStreak = 0
                        lastSkippedScrollIndex = currentLineIndex
                        if (hasLargeTimeDiff) {
                            logAutoScroll("mark skipped for补滚动 index=$currentLineIndex")
                        }
                    }
                }
            }
            
            // 检查是否有跳过的行需要补滚动
            if (lastSkippedScrollIndex >= 0 && lastSkippedScrollIndex != lastAutoScrolledIndex) {
                val skippedLineIndex = lastSkippedScrollIndex
                if (lastAutoScrolledIndex >= 0 && skippedLineIndex < lastAutoScrolledIndex) {
                    logAutoScroll(
                        "drop backward 补滚动 skipped=$skippedLineIndex last=$lastAutoScrolledIndex"
                    )
                    lastSkippedScrollIndex = -1
                    return@LaunchedEffect
                }
                if (skippedLineIndex > 0) {
                    // 找到上一句主句歌词（跳过背景歌词）
                    var prevMainLineIndex = skippedLineIndex - 1
                    while (prevMainLineIndex >= 0 && (processedLyricLines[prevMainLineIndex].isBackground || processedLyricLines[prevMainLineIndex].isInterlude)) {
                        prevMainLineIndex--
                    }
                    
                    if (prevMainLineIndex >= 0) {
                        val previousLine = processedLyricLines[prevMainLineIndex]
                        val prevNextLine = if (prevMainLineIndex < processedLyricLines.size - 1) processedLyricLines[prevMainLineIndex + 1] else null
                        val prevEndTime = getEffectiveEndTime(previousLine, prevNextLine)
                        
                        // 检查上一句是否播放结束了
                        if (currentTime >= prevEndTime) {
                            // 若距离下一次自动滚动触发太近，补滚动失效（避免短时间内二次滚动）
                            if (shouldSkipAutoScrollDueToCloseNextTrigger(skippedLineIndex, currentTime)) {
                                if (closeNextSkipStreak >= maxCloseNextConsecutiveSkips) {
                                    logAutoScroll("force 补滚动 index=$skippedLineIndex after close-next skip streak=$closeNextSkipStreak")
                                    closeNextSkipStreak = 0
                                } else {
                                    closeNextSkipStreak += 1
                                    lastSkippedScrollIndex = -1
                                    logAutoScroll("drop 补滚动 index=$skippedLineIndex due close-next window")
                                    return@LaunchedEffect
                                }
                            }
                            // 上一句播放结束了，强制滚动到当前跳过的行
                            closeNextSkipStreak = 0
                            lastAutoScrolledIndex = skippedLineIndex
                            lastSkippedScrollIndex = -1
                            logAutoScroll("trigger 补滚动 index=$skippedLineIndex at=$currentTime")
                            requestAutoScroll(skippedLineIndex)
                        }
                    }
                }
            }
        }
    }
    
    val rawBackgroundColor = coverThemeColor ?: MaterialTheme.colorScheme.background
    val backgroundColor = normalizeCoverThemeBackground(rawBackgroundColor, isDarkTheme)
    val accentColor = if (isDarkTheme) {
        blendColorForUi(backgroundColor, Color.White, 0.42f)
    } else {
        blendColorForUi(backgroundColor, Color.Black, 0.42f)
    }
    val baseForegroundColor = getHighContrastBlackOrWhite(backgroundColor)
    val menuSurfaceColor = ensureReadableColor(
        candidate = blendColors(backgroundColor, accentColor, 0.20f),
        background = backgroundColor,
        fallback = blendColors(backgroundColor, baseForegroundColor, 0.18f),
        minContrast = 1.25f
    )
    val menuContentColor = ensureReadableColor(
        candidate = blendColors(accentColor, baseForegroundColor, 0.72f),
        background = menuSurfaceColor,
        fallback = baseForegroundColor,
        minContrast = 4.0f
    )
    val menuPressedColor = ensureReadableColor(
        candidate = blendColors(accentColor, menuSurfaceColor, 0.40f),
        background = menuSurfaceColor,
        fallback = menuContentColor.copy(alpha = 0.18f),
        minContrast = 1.35f
    )
    val menuBorderColor = ensureReadableColor(
        candidate = blendColors(menuContentColor, menuSurfaceColor, 0.35f),
        background = menuSurfaceColor,
        fallback = menuContentColor.copy(alpha = 0.22f),
        minContrast = 1.1f
    )
    val dialogContainerColor = ensureReadableColor(
        candidate = blendColors(backgroundColor, accentColor, 0.24f),
        background = backgroundColor,
        fallback = blendColors(backgroundColor, baseForegroundColor, 0.20f),
        minContrast = 1.2f
    )
    val dialogContentColor = ensureReadableColor(
        candidate = blendColors(accentColor, baseForegroundColor, 0.78f),
        background = dialogContainerColor,
        fallback = baseForegroundColor,
        minContrast = 4.4f
    )
    val dialogAccentColor = ensureReadableColor(
        candidate = accentColor,
        background = dialogContainerColor,
        fallback = dialogContentColor,
        minContrast = 3.0f
    )
    val creatorLyricColor = ensureReadableColor(
        candidate = blendColors(accentColor, backgroundColor, 0.42f),
        background = backgroundColor,
        fallback = blendColors(baseForegroundColor, backgroundColor, 0.45f),
        minContrast = 2.8f
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        
        // 歌词区域放在顶层
        androidx.compose.ui.layout.LookaheadScope {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
            ) {
                val showLoadingOverlay = isLyricLoading || !lyricsContentReady
                val lyricContentAlpha by animateFloatAsState(
                    targetValue = if (showLoadingOverlay) 0f else 1f,
                    animationSpec = tween(durationMillis = 320),
                    label = "lyricContentRevealAlpha"
                )

                if (processedLyricLines.isEmpty()) {
                    if (!showLoadingOverlay) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无歌词", color = if (isDarkTheme) Color.Gray else Color.DarkGray)
                        }
                    }
                } else {
                    // 计算屏幕高度的1/4作为顶部padding
                    val configuration = LocalConfiguration.current
                    val screenHeight = configuration.screenHeightDp.dp
                    val topPadding = screenHeight * 0.25f // 屏幕高度的1/4
                    val bottomPadding = screenHeight * 0.6f // 底部留出足够空间
                    
                    // 使用较小的 keepAlive 区域来确保弹簧动画工作，同时不会占用过多空间
                    val keepAlivePadding = 100.dp
                    val density = LocalDensity.current
                    
                    // 创建一个自定义 Modifier 来延长视口高度
                    val extendedViewportModifier = object : LayoutModifier {
                        override fun MeasureScope.measure(
                            measurable: Measurable,
                            constraints: Constraints
                        ): MeasureResult {
                            val extraHeightPx = with(density) { (keepAlivePadding * 2).toPx().roundToInt() }
                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxHeight = if (constraints.maxHeight != Constraints.Infinity) {
                                        constraints.maxHeight + extraHeightPx
                                    } else {
                                        Constraints.Infinity
                                    }
                                )
                            )
                            val keepAlivePaddingPx = with(density) { keepAlivePadding.toPx().roundToInt() }
                            return layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(0, -keepAlivePaddingPx)
                            }
                        }
                    }
                    
                    Box(modifier = Modifier.alpha(lyricContentAlpha)) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds() // 裁剪超出边界的内容
                                .then(extendedViewportModifier), // 延长视口高度，让更多歌词行保持活跃
                            contentPadding = PaddingValues(
                                top = topPadding + keepAlivePadding,
                                bottom = bottomPadding + keepAlivePadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(processedLyricLines) { index, line ->
                            val nextLine = if (index < processedLyricLines.size - 1) processedLyricLines[index + 1] else null
                            // 计算动态 stiffness - 距离当前行越近，stiffness 越大，动画越快
                            val distance = if (currentLineIndex >= 0) kotlin.math.abs(index - currentLineIndex) else 0
                            val dynamicStiffness = (120f - (distance * 20f)).coerceAtLeast(20f)
                            val lyricLineBlurRadius = computeLyricLineBlurRadius(
                                lyricLines = processedLyricLines,
                                line = line,
                                nextLine = nextLine,
                                lineIndex = index,
                                currentPlayingIndex = currentLineIndex,
                                currentTime = currentTime,
                                isBlurEnabled = lyricBlurPreferenceEnabled && isLyricBlurEnabled
                            )
                            
                            // 判断背景歌词是否应该显示
                            val shouldShowBackground = if (line.isBackground) {
                                shouldShowBackgroundLine(processedLyricLines, index, currentTime)
                            } else {
                                true
                            }
                            
                            // 查找关联的主歌词和位置信息
                            val (mainLine, isAboveMain, _) = if (line.isBackground) {
                                findBackgroundAssociatedInfo(processedLyricLines, index)
                            } else {
                                Triple(null, null, null)
                            }
                            // 如果是背景歌词，跟随主句的对唱状态
                            val effectiveIsDuet = if (line.isBackground) {
                                mainLine?.isDuet ?: false
                            } else {
                                line.isDuet
                            }
                            
                            val shouldHideInitially = hidePlayedLinesDuringInitialBuild &&
                                initialBuildTargetIndex >= 0 &&
                                index < initialBuildTargetIndex
                            val lineAlpha by animateFloatAsState(
                                targetValue = if (shouldHideInitially) 0f else 1f,
                                animationSpec = tween(durationMillis = 300),
                                label = "initialPlayedLineReveal"
                            )

                            Box(modifier = Modifier.alpha(lineAlpha)) {
                                LyricLineView(
                                    line = line,
                                    nextLine = nextLine,
                                    lineIndex = index,
                                    currentPlayingIndex = currentLineIndex,
                                    currentTime = currentTime,
                                    showTranslation = showTranslation,
                                    isDarkTheme = isDarkTheme,
                                    fontSize = fontSize.sp,
                                    fontWeight = fontWeight, // 新增
                                    showTransliteration = showTransliteration, // 新增
                                    lookaheadScope = this@LookaheadScope,
                                    itemKey = "${line.begin}-${line.end}-$index",
                                    isManualScrolling = isUserScrolling,
                                    stiffness = dynamicStiffness,
                                    forceReset = seekResetCounter,
                                    shouldShowBackgroundLine = shouldShowBackground,
                                    isAboveMain = isAboveMain,
                                    backgroundColor = backgroundColor,
                                    themeAccentColor = accentColor,
                                    effectiveIsDuet = effectiveIsDuet,
                                    nextLineIsDuet = getNextLineIsDuet(processedLyricLines, index), // 新增
                                    isPlaying = isPlaying, // 新增
                                    animationType = animationType, // 新增
                                    blurRadius = lyricLineBlurRadius,
                                    onClick = {
                                        // 点击歌词行跳转到该行开始播放
                                        onSeekTo(line.begin)
                                        currentTime = line.begin
                                        lastAutoScrolledIndex = index // 更新最后滚动索引
                                        // 重置跳过的索引状态
                                        lastSkippedScrollIndex = -1
                                        if (!isPlaying) {
                                            isPlaying = true
                                            onPlayPause(true)
                                        }
                                        
                                        // 先确保用户滚动标志关闭，延迟一小段时间再滚动
                                        isUserScrolling = false
                                        scrollJob?.cancel()
                                        
                                        // 延迟滚动，给弹簧动画准备时间
                                        coroutineScope.launch {
                                            delay(50) // 短暂延迟确保动画准备好
                                            lazyListState.animateScrollToItem(
                                                index = index,
                                                scrollOffset = -100
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        
                            // 创作者信息显示区域，跟随歌词一起滚动
                            if (creators.isNotEmpty()) {
                                val creatorFontSize = maxOf(18f, fontSize - 10f).sp
                                val creatorItemKey = "creators-section-${seekResetCounter}"
                                val distance = if (currentLineIndex >= 0) kotlin.math.abs(processedLyricLines.size - currentLineIndex) else 0
                                val dynamicStiffness = (120f - (distance * 20f)).coerceAtLeast(20f)
                                
                                item(key = creatorItemKey) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .springPlacement(this@LookaheadScope, creatorItemKey, isUserScrolling, dynamicStiffness, seekResetCounter)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 16.dp),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(
                                                text = buildAnnotatedString {
                                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append("创作者：")
                                                    }
                                                    append(creators.joinToString("、"))
                                                },
                                                fontSize = creatorFontSize,
                                                color = creatorLyricColor,
                                                modifier = Modifier.alpha(0.8f),
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showLoadingOverlay,
                    enter = fadeIn(animationSpec = tween(durationMillis = 160)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 280))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = accentColor)
                            Text(
                                text = "歌词构建中...",
                                color = if (isDarkTheme) Color.LightGray else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
        
        // Headbar 和播放控制放在上层，遮挡额外歌词区域
        Column(modifier = Modifier.fillMaxSize()) {
            LyricPreviewHeader(
                title = metadata.title,
                artist = metadata.artist,
                coverBitmap = metadata.coverBitmap,
                onBackClick = onBack,
                onMenuClick = { menuExpanded = true },
                menuContent = { menuButtonPosition ->
                    CustomDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        items = buildList {
                            add(
                                MenuItem(
                                    title = if (showTranslation) "关闭翻译" else "开启翻译",
                                    onClick = { saveShowTranslation(!showTranslation) }
                                )
                            )
                            add(
                                MenuItem(
                                    title = if (showTransliteration) "关闭注音" else "开启注音",
                                    onClick = { saveShowTransliteration(!showTransliteration) }
                                )
                            )
                            if (supportsLyricBlur) {
                                add(
                                    MenuItem(
                                        title = if (lyricBlurPreferenceEnabled) "关闭歌词模糊" else "开启歌词模糊",
                                        onClick = { saveLyricBlurEnabled(!lyricBlurPreferenceEnabled) }
                                    )
                                )
                            }
                            add(
                                MenuItem(
                                    title = "字体大小 (${fontSize.toInt()}sp)",
                                    onClick = { showFontSizeDialog = true }
                                )
                            )
                            add(
                                MenuItem(
                                    title = "字体粗细 (${getFontWeightLabel(fontWeight)})",
                                    onClick = { showFontWeightDialog = true }
                                )
                            )
                            add(
                                MenuItem(
                                    title = if (animationType == LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR) "间奏动画：恐龙" else "间奏动画：默认",
                                    onClick = { showAnimationTypeDialog = true }
                                )
                            )
                        },
                        anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f),
                        containerColor = menuSurfaceColor,
                        contentColor = menuContentColor,
                        pressColor = menuPressedColor,
                        borderColor = menuBorderColor
                    )
                },
                mutedColor = accentColor,
                isDarkTheme = isDarkTheme,
                backgroundColor = backgroundColor
            )
            
            // 顶部渐变透明效果
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor,
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 底部渐变透明效果
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor
                            )
                        )
                    )
            )
            
            PlaybackControls(
                currentTime = currentTime,
                duration = dynamicDuration,
                isPlaying = isPlaying,
                isDarkTheme = isDarkTheme,
                seekTimeMs = seekTimeMs,
                seekTimeSeconds = seekTimeSeconds,
                onPlayPauseClick = { 
                    isPlaying = !isPlaying
                    onPlayPause(isPlaying)
                },
                onSeek = { position ->
                    onSeekTo(position)
                    currentTime = position
                    // 拖动进度条后重置用户滚动状态，立即滚动到对应位置
                    isUserScrolling = false
                    scrollJob?.cancel()
                    // 只有在非0秒处才触发歌词动画重置
                    if (position > 0) {
                        seekResetCounter += 1
                    }
                    // 强制重新计算自动滚动索引
                    lastAutoScrolledIndex = -1
                    // 重置跳过的索引状态
                    lastSkippedScrollIndex = -1
                    // 立即滚动到当前歌词位置，不使用动画
                    coroutineScope.launch {
                        val targetIndex = lineNavigator.findTargetIndex(position)
                        if (targetIndex >= 0) {
                            lazyListState.scrollToItem(index = targetIndex, scrollOffset = -100)
                        }
                    }
                },
                vibrantColor = accentColor,
                backgroundColor = backgroundColor
            )
        }
        
        // 字体大小设置对话框
        if (showFontSizeDialog) {
            var tempFontSize by remember { mutableFloatStateOf(fontSize) }
            AlertDialog(
                onDismissRequest = { showFontSizeDialog = false },
                containerColor = dialogContainerColor,
                titleContentColor = dialogContentColor,
                textContentColor = dialogContentColor,
                title = { Text("字体大小设置") },
                text = {
                    Column {
                        Text(
                            text = "当前大小: ${tempFontSize.toInt()}sp",
                            color = dialogContentColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Slider(
                            value = tempFontSize,
                            onValueChange = { tempFontSize = it },
                            valueRange = 18f..40f,
                            steps = 21,
                            colors = SliderDefaults.colors(
                                thumbColor = dialogAccentColor,
                                activeTrackColor = dialogAccentColor,
                                inactiveTrackColor = dialogContentColor.copy(alpha = 0.25f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("18sp", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                            Text("29sp", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                            Text("40sp", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogAccentColor),
                        onClick = {
                        saveFontSize(tempFontSize)
                        showFontSizeDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogContentColor.copy(alpha = 0.88f)),
                        onClick = { showFontSizeDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 字体粗细设置对话框
        if (showFontWeightDialog) {
            var tempFontWeight by remember { mutableFloatStateOf(fontWeight.toFloat()) }
            AlertDialog(
                onDismissRequest = { showFontWeightDialog = false },
                containerColor = dialogContainerColor,
                titleContentColor = dialogContentColor,
                textContentColor = dialogContentColor,
                title = { Text("字体粗细设置") },
                text = {
                    Column {
                        Text(
                            text = "当前粗细: ${getFontWeightLabel(tempFontWeight.toInt())}",
                            color = dialogContentColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Slider(
                            value = tempFontWeight,
                            onValueChange = { tempFontWeight = it },
                            valueRange = 300f..700f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = dialogAccentColor,
                                activeTrackColor = dialogAccentColor,
                                inactiveTrackColor = dialogContentColor.copy(alpha = 0.25f)
                            ),
                            onValueChangeFinished = {
                                // 将值调整为100的倍数
                                tempFontWeight = (tempFontWeight.toInt() / 100 * 100).toFloat()
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("细", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                            Text("正常", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                            Text("中", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                            Text("半粗", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                            Text("粗", fontSize = 12.sp, color = dialogContentColor.copy(alpha = 0.82f))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogAccentColor),
                        onClick = {
                        saveFontWeight(tempFontWeight.toInt() / 100 * 100)
                        showFontWeightDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogContentColor.copy(alpha = 0.88f)),
                        onClick = { showFontWeightDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        if (showAnimationTypeDialog) {
            var tempAnimationType by remember { mutableIntStateOf(animationType) }
            AlertDialog(
                onDismissRequest = { showAnimationTypeDialog = false },
                containerColor = dialogContainerColor,
                titleContentColor = dialogContentColor,
                textContentColor = dialogContentColor,
                title = { Text("间奏动画设置") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DEFAULT) {
                                        dialogAccentColor.copy(alpha = 0.16f)
                                    } else Color.Transparent
                                )
                                .clickable { tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DEFAULT,
                                onClick = { tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = dialogAccentColor,
                                    unselectedColor = dialogContentColor.copy(alpha = 0.65f)
                                )
                            )
                            Text("默认（圆点）", color = dialogContentColor)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR) {
                                        dialogAccentColor.copy(alpha = 0.16f)
                                    } else Color.Transparent
                                )
                                .clickable { tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR,
                                onClick = { tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = dialogAccentColor,
                                    unselectedColor = dialogContentColor.copy(alpha = 0.65f)
                                )
                            )
                            Text("恐龙", color = dialogContentColor)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogAccentColor),
                        onClick = {
                            animationType = tempAnimationType
                            prefs.edit().putInt(LyricPreviewActivity.KEY_INTERLUDE_ANIMATION_TYPE, tempAnimationType).apply()
                            showAnimationTypeDialog = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = dialogContentColor.copy(alpha = 0.88f)),
                        onClick = { showAnimationTypeDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// 获取字体粗细标签
fun getFontWeightLabel(weight: Int): String {
    return when (weight) {
        300 -> "细"
        400 -> "正常"
        500 -> "中"
        600 -> "半粗"
        700 -> "粗"
        else -> "正常"
    }
}

// ==================== 辅助函数：歌词关联 ====================

/**
 * 增强的歌词数据，包含上下背景歌词和调整后的播放时间
 */
data class EnhancedLyricLine(
    val line: NewPreviewLyricLine,
    val backgroundAbove: NewPreviewLyricLine? = null,
    val backgroundBelow: NewPreviewLyricLine? = null,
    // 调整后的播放时间：最早的开始，最晚的结束
    val adjustedBegin: Long = line.begin,
    val adjustedEnd: Long = line.end
)

/**
 * 重组歌词：将开始时间早于主句的背景歌词放上方，晚的放下方
 */
fun reorganizeLyricsWithBackground(lyricLines: List<NewPreviewLyricLine>): List<NewPreviewLyricLine> {
    val result = mutableListOf<NewPreviewLyricLine>()
    val processedIndices = mutableSetOf<Int>()
    
    for (i in lyricLines.indices) {
        if (processedIndices.contains(i)) continue
        
        val currentLine = lyricLines[i]
        
        if (!currentLine.isBackground) {
            // 这是主歌词行，收集背景歌词
            val backgroundLines = mutableListOf<NewPreviewLyricLine>()
            var j = i + 1
            while (j < lyricLines.size && lyricLines[j].isBackground) {
                backgroundLines.add(lyricLines[j])
                processedIndices.add(j)
                j++
            }
            
            // 计算调整后的时间
            var earliestBegin = currentLine.begin
            var latestEnd = currentLine.end
            
            // 分离上下背景歌词
            val backgroundAbove = mutableListOf<NewPreviewLyricLine>()
            val backgroundBelow = mutableListOf<NewPreviewLyricLine>()
            
            for (bg in backgroundLines) {
                if (bg.begin < currentLine.begin) {
                    backgroundAbove.add(bg)
                } else {
                    backgroundBelow.add(bg)
                }
                
                if (bg.begin < earliestBegin) earliestBegin = bg.begin
                if (bg.end > latestEnd) latestEnd = bg.end
            }
            
            // 先添加上方背景歌词，结束时间延长到主句结束
            backgroundAbove.forEach { bg ->
                val adjustedBg = bg.copy(
                    end = latestEnd,
                    backgroundPlacement = -1
                )
                result.add(adjustedBg)
            }
            
            // 添加主句
            val adjustedMainLine = currentLine.copy(
                begin = earliestBegin,
                end = latestEnd
            )
            result.add(adjustedMainLine)
            processedIndices.add(i)
            
            // 添加下方背景歌词，结束时间延长到主句结束
            backgroundBelow.forEach { bg ->
                val adjustedBg = bg.copy(
                    end = latestEnd,
                    backgroundPlacement = 1
                )
                result.add(adjustedBg)
            }
        } else {
            // 不是主歌词行，直接添加
            result.add(currentLine)
            processedIndices.add(i)
        }
    }
    
    return result
}

/**
 * 查找某个背景歌词属于哪个主句，以及位置（上方还是下方）
 */
fun findBackgroundAssociatedInfo(
    lyricLines: List<NewPreviewLyricLine>,
    backgroundIndex: Int
): Triple<NewPreviewLyricLine?, Boolean, Int?> { // 主句, 是否在上方, 主句索引
    val backgroundLine = lyricLines.getOrNull(backgroundIndex) ?: return Triple(null, false, null)

    fun findPrevMain(): Pair<NewPreviewLyricLine, Int>? {
        for (i in backgroundIndex - 1 downTo 0) {
            if (!lyricLines[i].isBackground) {
                return lyricLines[i] to i
            }
        }
        return null
    }

    fun findNextMain(): Pair<NewPreviewLyricLine, Int>? {
        for (i in backgroundIndex + 1 until lyricLines.size) {
            if (!lyricLines[i].isBackground) {
                return lyricLines[i] to i
            }
        }
        return null
    }

    val prevMain = findPrevMain()
    val nextMain = findNextMain()

    // 优先使用重组阶段记录的上下方信息，避免误关联到上一句主句
    if (backgroundLine.backgroundPlacement < 0) {
        if (nextMain != null) return Triple(nextMain.first, true, nextMain.second)
        if (prevMain != null) return Triple(prevMain.first, false, prevMain.second)
        return Triple(null, false, null)
    }
    if (backgroundLine.backgroundPlacement > 0) {
        if (prevMain != null) return Triple(prevMain.first, false, prevMain.second)
        if (nextMain != null) return Triple(nextMain.first, true, nextMain.second)
        return Triple(null, false, null)
    }

    // 兼容未知场景：按最近主句回退
    if (prevMain == null && nextMain == null) return Triple(null, false, null)
    if (prevMain == null && nextMain != null) return Triple(nextMain.first, true, nextMain.second)
    if (nextMain == null && prevMain != null) return Triple(prevMain.first, false, prevMain.second)

    val prev = prevMain!!
    val next = nextMain!!
    val prevDistance = backgroundIndex - prev.second
    val nextDistance = next.second - backgroundIndex
    return if (nextDistance < prevDistance) {
        Triple(next.first, true, next.second)
    } else {
        Triple(prev.first, false, prev.second)
    }
}

/**
 * 查找与给定主歌词关联的背景歌词索引
 */
fun findAssociatedBackgroundLines(
    lyricLines: List<NewPreviewLyricLine>,
    mainLineIndex: Int
): List<Int> {
    val result = mutableListOf<Int>()
    // 通常背景歌词紧跟在主歌词之后
    var i = mainLineIndex + 1
    while (i < lyricLines.size && lyricLines[i].isBackground) {
        result.add(i)
        i++
    }
    return result
}

/**
 * 判断某个背景歌词是否应该显示
 * - 上方背景歌词：自己开始时间前400毫秒显示，播放后不自动隐藏
 * - 下方背景歌词：和主句一同显示，播放后不自动隐藏
 */
fun shouldShowBackgroundLine(
    lyricLines: List<NewPreviewLyricLine>,
    backgroundLineIndex: Int,
    currentTime: Long
): Boolean {
    val backgroundLine = lyricLines[backgroundLineIndex]
    val (mainLine, isAbove, _) = findBackgroundAssociatedInfo(lyricLines, backgroundLineIndex)
    
    if (mainLine == null) return false

    val isUpperBackground = backgroundLine.backgroundPlacement < 0 || (backgroundLine.backgroundPlacement == 0 && isAbove == true)

    return if (isUpperBackground) {
        // 上方背景歌词：自己开始前400毫秒显示，播放后不自动隐藏
        currentTime >= (backgroundLine.begin - 400L)
    } else {
        // 下方背景歌词：和主句一同显示，播放后不自动隐藏
        currentTime >= mainLine.begin
    }
}

// ==================== 间奏行视图 ====================

@Composable
fun InterludeLineView(
    interludeLine: NewPreviewLyricLine,
    currentTime: Long,
    lookaheadScope: androidx.compose.ui.layout.LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    stiffness: Float,
    forceReset: Long,
    nextLineIsDuet: Boolean = false, // 新增：下一句是否是对唱歌词
    isDarkTheme: Boolean = false, // 新增：是否是深色模式
    fontSize: TextUnit = 32.sp, // 新增：字号大小
    isPlaying: Boolean = false, // 新增：歌曲是否正在播放
    animationType: Int = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT, // 新增：动画类型
    lyricColor: Color = if (isDarkTheme) Color.White else Color.Black,
    onClick: () -> Unit = {}
) {
    // 判断是否是开头间奏行（begin == 0）
    val isOpeningInterlude = interludeLine.begin == 0L
    
    // 显示逻辑：所有间奏行都在第500毫秒后开始显示
    val effectiveBeginTime = interludeLine.begin + 200L
    // 提前500毫秒隐藏
    val isVisible = currentTime >= effectiveBeginTime && currentTime < interludeLine.end - 500
    
    val alignment = remember(nextLineIsDuet) {
        if (nextLineIsDuet) Alignment.CenterEnd else Alignment.CenterStart
    }
    val useLightAnimation = lyricColor.luminance() >= 0.5f
    
    // 计算高度为字号的5倍
    val fontSizeDp = with(LocalDensity.current) { fontSize.toDp() }
    val interludeHeight = fontSizeDp * 2
    
    // 显示/隐藏动画 - 优化版本
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(350)),
        exit = fadeOut(animationSpec = tween(350))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(interludeHeight) 
                .springPlacement(lookaheadScope, itemKey, isManualScrolling, stiffness, forceReset)
        ) {
            if (animationType == LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR) {
                // 恐龙动画
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = alignment
                ) {
                    DinosaurAnimation(
                        isPlaying = isVisible && isPlaying,
                        useLightAnimation = useLightAnimation,
                        alignment = alignment
                    )
                }
            } else {
                // 默认动画 (circle)
                val timeRemaining = interludeLine.end - currentTime
                val firstVisible = timeRemaining > 1000
                val secondVisible = timeRemaining > 2500
                val thirdVisible = timeRemaining > 4000
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth(),
                        horizontalArrangement = if (nextLineIsDuet) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 第一张的透明度动画
                        val firstAlpha by animateFloatAsState(
                            targetValue = if (firstVisible) 1f else 0.3f,
                            animationSpec = tween(800),
                            label = "firstAlpha"
                        )
                        // 第二张的透明度动画
                        val secondAlpha by animateFloatAsState(
                            targetValue = if (secondVisible) 1f else 0.3f,
                            animationSpec = tween(800),
                            label = "secondAlpha"
                        )
                        // 第三张的透明度动画
                        val thirdAlpha by animateFloatAsState(
                            targetValue = if (thirdVisible) 1f else 0.3f,
                            animationSpec = tween(800),
                            label = "thirdAlpha"
                        )
                        
                        if (nextLineIsDuet) {
                            // 对唱模式，从右向左
                            // 第三张：2500毫秒前100%，之后30%
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth()
                                    .alpha(thirdAlpha)
                            ) {
                                CircleAnimation(isVisible && isPlaying, useLightAnimation)
                            }
                            // 第二张：1500毫秒前100%，之后30%
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth()
                                    .alpha(secondAlpha)
                            ) {
                                CircleAnimation(isVisible && isPlaying, useLightAnimation)
                            }
                            // 第一张：500毫秒前100%，之后30%
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth()
                                    .alpha(firstAlpha)
                            ) {
                                CircleAnimation(isVisible && isPlaying, useLightAnimation)
                            }
                        } else {
                            // 主句模式，从左向右
                            // 第一张：500毫秒前100%，之后30%
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth()
                                    .alpha(firstAlpha)
                            ) {
                                CircleAnimation(isVisible && isPlaying, useLightAnimation)
                            }
                            // 第二张：1500毫秒前100%，之后30%
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth()
                                    .alpha(secondAlpha)
                            ) {
                                CircleAnimation(isVisible && isPlaying, useLightAnimation)
                            }
                            // 第三张：2500毫秒前100%，之后30%
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth()
                                    .alpha(thirdAlpha)
                            ) {
                                CircleAnimation(isVisible && isPlaying, useLightAnimation)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircleAnimation(isPlaying: Boolean, useLightAnimation: Boolean) {
    val animationRes = if (useLightAnimation) R.raw.anim_circle else R.raw.anim_circle_blank
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    
    // 记录上一次的播放状态和进度
    var wasPlaying by remember { mutableStateOf(isPlaying) }
    var previousProgress by remember { mutableStateOf(0f) }
    var shouldContinueUntilCycleComplete by remember { mutableStateOf(false) }
    var effectiveIsPlaying by remember { mutableStateOf(isPlaying) }
    
    // 更新播放状态
    LaunchedEffect(isPlaying) {
        if (!isPlaying && wasPlaying) {
            // 从播放变为暂停，需要等待当前循环结束
            shouldContinueUntilCycleComplete = true
            effectiveIsPlaying = true
        } else if (isPlaying) {
            // 开始或继续播放
            shouldContinueUntilCycleComplete = false
            effectiveIsPlaying = true
        }
        wasPlaying = isPlaying
    }
    
    val progress by animateLottieCompositionAsState(
        composition,
        isPlaying = effectiveIsPlaying,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )
    
    // 监听进度变化，检测循环是否完成
    LaunchedEffect(progress) {
        if (shouldContinueUntilCycleComplete && previousProgress > 0.9f && progress < 0.1f) {
            // 检测到一次循环完成（进度从接近1跳回到接近0）
            shouldContinueUntilCycleComplete = false
            effectiveIsPlaying = false
        }
        previousProgress = progress
    }
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(140f / 500f),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun DinosaurAnimation(
    isPlaying: Boolean,
    useLightAnimation: Boolean,
    alignment: Alignment
) {
    val animationRes = if (useLightAnimation) R.raw.anim_dinosaur_white else R.raw.anim_dinosaur_black
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    
    // 记录上一次的播放状态和进度
    var wasPlaying by remember { mutableStateOf(isPlaying) }
    var previousProgress by remember { mutableStateOf(0f) }
    var shouldContinueUntilCycleComplete by remember { mutableStateOf(false) }
    var effectiveIsPlaying by remember { mutableStateOf(isPlaying) }
    
    // 更新播放状态
    LaunchedEffect(isPlaying) {
        if (!isPlaying && wasPlaying) {
            // 从播放变为暂停，需要等待当前循环结束
            shouldContinueUntilCycleComplete = true
            effectiveIsPlaying = true
        } else if (isPlaying) {
            // 开始或继续播放
            shouldContinueUntilCycleComplete = false
            effectiveIsPlaying = true
        }
        wasPlaying = isPlaying
    }
    
    val progress by animateLottieCompositionAsState(
        composition,
        isPlaying = effectiveIsPlaying,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )
    
    // 监听进度变化，检测循环是否完成
    LaunchedEffect(progress) {
        if (shouldContinueUntilCycleComplete && previousProgress > 0.9f && progress < 0.1f) {
            // 检测到一次循环完成（进度从接近1跳回到接近0）
            shouldContinueUntilCycleComplete = false
            effectiveIsPlaying = false
        }
        previousProgress = progress
    }
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f),
        contentScale = ContentScale.Fit
    )
}

// ==================== 歌词行视图 ====================

@Composable
fun LyricLineView(
    line: NewPreviewLyricLine,
    nextLine: NewPreviewLyricLine?,
    lineIndex: Int,
    currentPlayingIndex: Int,
    currentTime: Long,
    showTranslation: Boolean,
    isDarkTheme: Boolean,
    fontSize: TextUnit = 32.sp,
    fontWeight: Int = 400, // 新增：字体粗细
    showTransliteration: Boolean = true, // 新增：是否显示注音
    lookaheadScope: androidx.compose.ui.layout.LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    stiffness: Float,
    forceReset: Long,
    shouldShowBackgroundLine: Boolean = false,
    isAboveMain: Boolean? = null, // 是否在主句上方
    backgroundColor: Color? = null,
    themeAccentColor: Color? = null,
    effectiveIsDuet: Boolean = false,
    nextLineIsDuet: Boolean = false, // 新增：下一句是否是对唱歌词（用于间奏行）
    isPlaying: Boolean = false, // 新增：歌曲是否正在播放
    animationType: Int = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT, // 新增：动画类型
    blurRadius: Float = 0f,
    onClick: () -> Unit = {}
) {
    val interludeLyricColor = backgroundColor?.let { bg ->
        getHighContrastBlackOrWhite(bg)
    } ?: if (isDarkTheme) Color.White else Color.Black

    // 如果是间奏行，使用特殊视图
    if (line.isInterlude) {
        InterludeLineView(
            interludeLine = line,
            currentTime = currentTime,
            lookaheadScope = lookaheadScope,
            itemKey = itemKey,
            isManualScrolling = isManualScrolling,
            stiffness = stiffness,
            forceReset = forceReset,
            nextLineIsDuet = nextLineIsDuet,
            isDarkTheme = isDarkTheme,
            fontSize = fontSize,
            isPlaying = isPlaying,
            animationType = animationType,
            lyricColor = interludeLyricColor,
            onClick = onClick
        )
        return
    }
    
    // 根据背景色或主题确定高对比度文字颜色
    val effectiveIsDarkTheme = backgroundColor?.let { bg ->
        val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
        luminance < 0.5f
    } ?: isDarkTheme
    
    val resolvedBackground = backgroundColor ?: if (effectiveIsDarkTheme) Color(0xFF101214) else Color(0xFFF6F7F8)
    val baseForeground = getHighContrastBlackOrWhite(resolvedBackground)
    val accent = themeAccentColor ?: MaterialTheme.colorScheme.primary
    val fontWeightValue = mapComposeFontWeight(fontWeight)

    // 通过颜色区分状态：未播放更浅，正在播放更强调，已播放介于两者之间
    val activeColor = ensureReadableColor(
        candidate = blendColors(accent, baseForeground, 0.52f),
        background = resolvedBackground,
        fallback = baseForeground,
        minContrast = 4.8f
    )
    val playedColor = ensureReadableColor(
        candidate = blendColors(activeColor, resolvedBackground, 0.18f),
        background = resolvedBackground,
        fallback = baseForeground,
        minContrast = 4.1f
    )
    val inactiveColor = ensureReadableColor(
        candidate = blendColors(activeColor, resolvedBackground, 0.42f),
        background = resolvedBackground,
        fallback = blendColors(baseForeground, resolvedBackground, 0.45f),
        minContrast = 2.9f
    )
    val translationActiveColor = ensureReadableColor(
        candidate = blendColors(accent, baseForeground, 0.78f),
        background = resolvedBackground,
        fallback = baseForeground.copy(alpha = 0.90f),
        minContrast = 3.6f
    )
    val translationInactiveColor = ensureReadableColor(
        candidate = blendColors(translationActiveColor, resolvedBackground, 0.44f),
        background = resolvedBackground,
        fallback = blendColors(baseForeground, resolvedBackground, 0.52f),
        minContrast = 2.5f
    )
    val translationPlayedColor = ensureReadableColor(
        candidate = blendColors(translationActiveColor, resolvedBackground, 0.50f),
        background = resolvedBackground,
        fallback = translationInactiveColor,
        minContrast = 2.4f
    )
    val transliterationInactiveColor = ensureReadableColor(
        candidate = blendColors(inactiveColor, translationInactiveColor, 0.45f),
        background = resolvedBackground,
        fallback = inactiveColor,
        minContrast = 2.6f
    )
    val transliterationActiveColor = ensureReadableColor(
        candidate = blendColors(activeColor, translationActiveColor, 0.32f),
        background = resolvedBackground,
        fallback = activeColor,
        minContrast = 4.0f
    )
    val transliterationPlayedColor = ensureReadableColor(
        candidate = blendColors(transliterationActiveColor, resolvedBackground, 0.16f),
        background = resolvedBackground,
        fallback = playedColor,
        minContrast = 3.4f
    )
    
    val isLineByLine = line.isLineByLineLyric()
    val effectiveEnd = if (isLineByLine) getEffectiveEndTime(line, nextLine) else line.end
    val isLineActive = currentTime >= line.begin && currentTime < effectiveEnd
    val isLinePassed = currentTime >= effectiveEnd
    
    // 背景歌词的显示逻辑
    val isBackgroundVisible = if (line.isBackground) shouldShowBackgroundLine else true
    
    // 背景歌词行的间距和字体调整
    val verticalPadding = if (line.isBackground) 4.dp else 8.dp
    val lineFontSize = if (line.isBackground) {
        (fontSize.value - 8).sp
    } else {
        fontSize
    }
    
    val lineAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "lineAlpha"
    )
    val translationTargetColor = when {
        lineIndex == currentPlayingIndex -> translationActiveColor
        isLineActive -> translationActiveColor
        isLinePassed -> translationPlayedColor
        else -> translationInactiveColor
    }
    val translationColor by androidx.compose.animation.animateColorAsState(
        targetValue = translationTargetColor,
        animationSpec = tween(260),
        label = "translationColor"
    )
    val lineBlurModifier = rememberLyricLineBlurModifier(blurRadius)
    
    // 把 springPlacement 放在外面，确保占位始终存在
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .springPlacement(lookaheadScope, itemKey, isManualScrolling, stiffness, forceReset)
    ) {
        if (line.isBackground) {
            // 背景歌词的滑动和淡入淡出动画 - 使用 AnimatedVisibility 但高度也动画化
            AnimatedVisibility(
                visible = isBackgroundVisible,
                enter = if (isAboveMain == true) {
                    // 在主句上方的背景：从主句方向（下方）向上滑入显示
                    fadeIn(animationSpec = tween(300)) + slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400)
                    )
                } else {
                    // 在主句下方的背景：从主句向下滑出显示
                    fadeIn(animationSpec = tween(300)) + slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(400)
                    )
                },
                exit = if (isAboveMain == true) {
                    // 在主句上方的背景：向主句方向（向下）滑出关闭
                    fadeOut(animationSpec = tween(300)) + slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(400)
                    )
                } else {
                    // 在主句下方的背景：向主句方向（向上）滑回关闭
                    fadeOut(animationSpec = tween(300)) + slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(400)
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(lineAlpha * 0.8f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .then(lineBlurModifier)
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = verticalPadding),
                    horizontalAlignment = if (effectiveIsDuet) Alignment.End else Alignment.Start
                ) {
                    if (isLineByLine) {
                        // 逐行歌词渲染
                        LyricLineByLineView(
                            line = line,
                            nextLine = nextLine,
                            currentTime = currentTime,
                            activeColor = activeColor,
                            playedColor = playedColor,
                            inactiveColor = inactiveColor,
                            transliterationActiveColor = transliterationActiveColor,
                            transliterationPlayedColor = transliterationPlayedColor,
                            transliterationInactiveColor = transliterationInactiveColor,
                            fontSize = lineFontSize,
                            isDuet = effectiveIsDuet,
                            fontWeight = fontWeight, // 新增
                            showTransliteration = showTransliteration // 新增
                        )
                    } else {
                        // 逐字歌词渲染 - 支持自动换行
                        LyricWordsCanvasWithWrap(
                            words = line.words,
                            currentTime = currentTime,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            fontSize = lineFontSize,
                            isDuet = effectiveIsDuet,
                            fontWeight = fontWeight, // 新增
                            showTransliteration = showTransliteration // 新增
                        )
                    }
                    
                    // 翻译
                    if (showTranslation && line.translation.isNotEmpty()) {
                        Text(
                            text = line.translation,
                            color = translationColor,
                            fontSize = (lineFontSize.value * 0.6).sp,
                            fontWeight = fontWeightValue,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = if (effectiveIsDuet) TextAlign.End else TextAlign.Start
                        )
                    }
                }
            }
        } else {
            // 普通歌词（非背景）直接显示
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(lineAlpha)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent)
                    .then(lineBlurModifier)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = verticalPadding),
                horizontalAlignment = if (effectiveIsDuet) Alignment.End else Alignment.Start
            ) {
                if (isLineByLine) {
                    // 逐行歌词渲染
                    LyricLineByLineView(
                        line = line,
                        nextLine = nextLine,
                        currentTime = currentTime,
                        activeColor = activeColor,
                        playedColor = playedColor,
                        inactiveColor = inactiveColor,
                        transliterationActiveColor = transliterationActiveColor,
                        transliterationPlayedColor = transliterationPlayedColor,
                        transliterationInactiveColor = transliterationInactiveColor,
                        fontSize = lineFontSize,
                        isDuet = effectiveIsDuet,
                        fontWeight = fontWeight, // 新增
                        showTransliteration = showTransliteration // 新增
                    )
                } else {
                    // 逐字歌词渲染 - 支持自动换行
                    LyricWordsCanvasWithWrap(
                        words = line.words,
                        currentTime = currentTime,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                        fontSize = lineFontSize,
                        isDuet = effectiveIsDuet,
                        fontWeight = fontWeight, // 新增
                        showTransliteration = showTransliteration // 新增
                    )
                }
                
                // 翻译
                if (showTranslation && line.translation.isNotEmpty()) {
                    Text(
                        text = line.translation,
                        color = translationColor,
                        fontSize = (lineFontSize.value * 0.6).sp,
                        fontWeight = fontWeightValue,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = if (effectiveIsDuet) TextAlign.End else TextAlign.Start
                    )
                }
            }
        }
    }
}

// ==================== 逐行歌词渲染组件 ====================

@Composable
fun LyricLineByLineView(
    line: NewPreviewLyricLine,
    nextLine: NewPreviewLyricLine?,
    currentTime: Long,
    activeColor: Color,
    playedColor: Color = activeColor,
    inactiveColor: Color,
    transliterationActiveColor: Color = activeColor,
    transliterationPlayedColor: Color = transliterationActiveColor,
    transliterationInactiveColor: Color = inactiveColor,
    fontSize: TextUnit = 24.sp,
    isDuet: Boolean = false,
    fontWeight: Int = 400, // 新增：字体粗细
    showTransliteration: Boolean = true // 新增：是否显示注音
) {
    val fullText = line.words.joinToString("") { it.text }
    val effectiveEnd = getEffectiveEndTime(line, nextLine)
    val isLineActive = currentTime >= line.begin && currentTime < effectiveEnd
    val isLinePassed = currentTime >= effectiveEnd
    
    val displayTargetColor = when {
        isLineActive -> activeColor
        isLinePassed -> playedColor
        else -> inactiveColor
    }
    val transliterationTargetColor = when {
        isLineActive -> transliterationActiveColor
        isLinePassed -> transliterationPlayedColor
        else -> transliterationInactiveColor
    }
    val displayColor by androidx.compose.animation.animateColorAsState(
        targetValue = displayTargetColor,
        animationSpec = tween(260),
        label = "lineByLineMainColor"
    )
    val transliterationColor by androidx.compose.animation.animateColorAsState(
        targetValue = transliterationTargetColor,
        animationSpec = tween(260),
        label = "lineByLineTransColor"
    )
    
    val fontWeightValue = mapComposeFontWeight(fontWeight)
    
    // 检查是否有注音
    val hasTransliteration = showTransliteration && line.words.any { 
        it.transliteration.isNotEmpty() || it.charTransliterations.isNotEmpty() 
    }
    
    if (hasTransliteration) {
        Column(
            horizontalAlignment = if (isDuet) Alignment.End else Alignment.Start
        ) {
            // 第一行：显示注音，仅在注音本身为非CJK字符（如拉丁拼音）时才在后面加空格
            val transliterationText = line.words.joinToString("") { word ->
                word.text.mapIndexed { idx, char ->
                    val trans = if (word.charTransliterations.isNotEmpty()) {
                        word.charTransliterations[idx] ?: ""
                    } else {
                        if (char == ' ') "" else word.transliteration
                    }
                    // 检查注音文本是否需要加空格（看注音本身的字符，不是歌词字符）
                    if (trans.isNotEmpty() && isTransliterationNeedsSpace(trans)) {
                        trans + " "
                    } else {
                        trans
                    }
                }.joinToString("")
            }
            Text(
                text = transliterationText,
                color = transliterationColor,
                fontSize = fontSize * 0.6f,
                fontWeight = fontWeightValue,
                textAlign = if (isDuet) TextAlign.End else TextAlign.Start
            )
            // 第二行：显示歌词
            Text(
                text = fullText,
                color = displayColor,
                fontSize = fontSize,
                fontWeight = fontWeightValue,
                textAlign = if (isDuet) TextAlign.End else TextAlign.Start
            )
        }
    } else {
        // 没有注音，只显示歌词
        Text(
            text = fullText,
            color = displayColor,
            fontSize = fontSize,
            fontWeight = fontWeightValue,
            textAlign = if (isDuet) TextAlign.End else TextAlign.Start
        )
    }
}

// ==================== 逐字Canvas渲染（支持自动换行）====================

@Composable
fun LyricWordsCanvasWithWrap(
    words: List<NewPreviewLyricWord>,
    currentTime: Long,
    activeColor: Color,
    inactiveColor: Color,
    fontSize: TextUnit = 24.sp,
    isDuet: Boolean = false,
    fontWeight: Int = 400, // 新增：字体粗细
    showTransliteration: Boolean = true // 新增：是否显示注音
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val fontSizePx = with(density) { fontSize.toPx() }
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { (configuration.screenWidthDp.dp - 56.dp).toPx() } // 减去左右padding
    
    // 创建上抬动画器，使用remember保存状态
    val liftAnimator = remember { WordLiftAnimator() }
    
    // 计算所有单词布局并自动换行
    val lineLayouts = remember(words, fontSize, fontWeight, showTransliteration) {
        // 辅助函数：根据给定的单词列表和允许换行的空格位置来计算布局
        fun calculateLayouts(
            inputWords: List<NewPreviewLyricWord>,
            breakableSpaceIndices: Set<Int>,
            showTransliteration: Boolean
        ): List<List<NewPreviewWordLayout>> {
            val layouts = mutableListOf<List<NewPreviewWordLayout>>()
            var currentLine = mutableListOf<NewPreviewWordLayout>()
            var currentX = 0f
            var groupIndex = 0
            var isNewLine = true
            var wordIdx = 0
            
            // 分组：
            // 1. 连续的英文字母组成一组
            // 2. 带 "'" 或 "," 的整个单词单元（前后连续的字）组成一组
            // 3. 空格单独一组
            val wordGroups = mutableListOf<List<Pair<Int, NewPreviewLyricWord>>>()
            var currentGroup = mutableListOf<Pair<Int, NewPreviewLyricWord>>()
            var i = 0
            
            while (i < inputWords.size) {
                val word = inputWords[i]
                
                // 检查是否是空格
                if (word.text == " ") {
                    // 先保存之前的组
                    if (currentGroup.isNotEmpty()) {
                        wordGroups.add(currentGroup.toList())
                        currentGroup = mutableListOf()
                    }
                    // 空格单独作为一个组
                    wordGroups.add(listOf(i to word))
                    i++
                    continue
                }
                
                // 检查是否有 "'" 或 ","，如果有，整个连续的字组成一个单元
                var hasSpecialChar = word.text == "'" || word.text == ","
                // 检查周围的字
                var j = i
                // 向后查找连续的字，直到遇到空格
                while (j < inputWords.size && inputWords[j].text != " ") {
                    if (inputWords[j].text == "'" || inputWords[j].text == ",") {
                        hasSpecialChar = true
                    }
                    j++
                }
                
                if (hasSpecialChar) {
                    // 先保存之前的组
                    if (currentGroup.isNotEmpty()) {
                        wordGroups.add(currentGroup.toList())
                        currentGroup = mutableListOf()
                    }
                    // 把从 i 到 j-1 的字都加入一个组
                    for (k in i until j) {
                        currentGroup.add(k to inputWords[k])
                    }
                    wordGroups.add(currentGroup.toList())
                    currentGroup = mutableListOf()
                    i = j
                    continue
                }
                
                // 否则检查是否是连续英文字母
                val isEnglishChar = word.text.length == 1 && word.text[0] in 'a'..'z' || word.text[0] in 'A'..'Z'
                val prevWasEnglish = currentGroup.isNotEmpty() && {
                    val lastWord = currentGroup.last().second
                    lastWord.text.length == 1 && lastWord.text[0] in 'a'..'z' || lastWord.text[0] in 'A'..'Z'
                }()
                
                if (isEnglishChar && prevWasEnglish) {
                    currentGroup.add(i to word)
                } else {
                    if (currentGroup.isNotEmpty()) {
                        wordGroups.add(currentGroup.toList())
                        currentGroup = mutableListOf()
                    }
                    currentGroup.add(i to word)
                }
                
                i++
            }
            
            if (currentGroup.isNotEmpty()) {
                wordGroups.add(currentGroup.toList())
            }
            
            // 计算布局
            while (groupIndex < wordGroups.size) {
                val group = wordGroups[groupIndex]
                val groupText = group.joinToString("") { it.second.text }
                val processedText = if (isNewLine) groupText.trimStart() else groupText
                
                if (processedText.isEmpty()) {
                    groupIndex++
                    continue
                }
                
                val result = textMeasurer.measure(
                    text = processedText,
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal)
                )
                val groupWidth = result.size.width.toFloat()
                val spaceWidth = if (processedText.endsWith(" ")) with(density) { 1.dp.toPx() } else 0f
                
                // 检查是否需要换行
                // 两种情况需要换行：
                // 1. 当前是空格且在允许换行的位置
                // 2. 加上当前内容超过屏幕宽度
                val isBreakableSpace = group.size == 1 && group.first().second.text == " " && breakableSpaceIndices.contains(group.first().first)
                
                if ((currentX + groupWidth > screenWidthPx && currentLine.isNotEmpty()) || isBreakableSpace) {
                    // 换行前，检查并移除当前行末尾的空格
                    if (currentLine.isNotEmpty()) {
                        val lastLayout = currentLine.last()
                        if (lastLayout.word.text.endsWith(" ")) {
                            val trimmedWord = lastLayout.word.text.trimEnd()
                            if (trimmedWord.isEmpty()) {
                                currentLine.removeAt(currentLine.lastIndex)
                            } else {
                                val trimmedResult = textMeasurer.measure(
                                    text = trimmedWord,
                                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal)
                                )
                                val trimmedWidth = trimmedResult.size.width.toFloat()
                                val trimmedWordObj = NewPreviewLyricWord(
                                    text = trimmedWord,
                                    begin = lastLayout.word.begin,
                                    end = lastLayout.word.end
                                )
                                val newLayout = NewPreviewWordLayout(
                                    word = trimmedWordObj,
                                    textWidth = trimmedWidth,
                                    spaceWidth = 0f,
                                    startPosition = lastLayout.startPosition,
                                    endPosition = lastLayout.startPosition + trimmedWidth,
                                    charWidths = FloatArray(trimmedWord.length),
                                    charStartPositions = FloatArray(trimmedWord.length)
                                )
                                currentLine[currentLine.lastIndex] = newLayout
                            }
                        }
                    }
                    
                    // 保存当前行并换行
                    if (currentLine.isNotEmpty()) {
                        layouts.add(currentLine.toList())
                    }
                    currentLine = mutableListOf()
                    currentX = 0f
                    isNewLine = true
                    
                    // 如果是因为空格换行，跳过这个空格
                    if (isBreakableSpace) {
                        groupIndex++
                        continue
                    }
                    // 否则重新处理当前组
                    continue
                }
                
                // 为组内的每个字符创建布局
                var charX = currentX
                group.forEach { (originalIndex, word) ->
                    val charText = word.text
                    val charResult = textMeasurer.measure(
                        text = charText,
                        style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal)
                    )
                    val charWidth = charResult.size.width.toFloat()
                    
                    // 计算注音宽度（如果注音是拉丁拼音就计算注音+空格的宽度，否则只计算注音宽度）
                    var finalCharWidth = charWidth
                    if (showTransliteration && word.transliteration.isNotEmpty()) {
                        val needsSpace = isTransliterationNeedsSpace(word.transliteration)
                        val transFontSize = (fontSize.value / 2).sp
                        val textToMeasure = if (needsSpace) {
                            word.transliteration + " "
                        } else {
                            word.transliteration
                        }
                        val transResult = textMeasurer.measure(
                            text = textToMeasure,
                            style = TextStyle(fontSize = transFontSize, fontWeight = FontWeight.Normal)
                        )
                        val transWidth = transResult.size.width.toFloat()
                        if (transWidth > charWidth) {
                            finalCharWidth = transWidth
                        }
                    }
                    
                    val layout = NewPreviewWordLayout(
                        word = word,
                        textWidth = finalCharWidth,
                        spaceWidth = 0f,
                        startPosition = charX,
                        endPosition = charX + finalCharWidth,
                        charWidths = FloatArray(charText.length),
                        charStartPositions = FloatArray(charText.length)
                    )
                    currentLine.add(layout)
                    charX += finalCharWidth
                }
                
                currentX = charX + spaceWidth
                groupIndex++
                isNewLine = false
            }
            
            if (currentLine.isNotEmpty()) {
                layouts.add(currentLine.toList())
            }
            
            return layouts
        }
        
        // 收集所有空格位置，排除位于英文字母之间的空格
        val spaceIndices = mutableListOf<Int>()
        words.forEachIndexed { index, word ->
            if (word.text == " ") {
                // 检查前一个和后一个字符是否都是英文字母或英文引号
                fun isEnglishLikeChar(c: Char): Boolean {
                    return c in 'a'..'z' || c in 'A'..'Z' || c == '\'' || c == '"'
                }
                
                val prevIsEnglish = if (index > 0) {
                    val prevWord = words[index - 1]
                    prevWord.text.length == 1 && isEnglishLikeChar(prevWord.text[0])
                } else false
                
                val nextIsEnglish = if (index < words.size - 1) {
                    val nextWord = words[index + 1]
                    nextWord.text.length == 1 && isEnglishLikeChar(nextWord.text[0])
                } else false
                
                // 如果不是位于英文字母之间的空格，才加入可换行的空格列表
                if (!(prevIsEnglish && nextIsEnglish)) {
                    spaceIndices.add(index)
                }
            }
        }
        
        if (spaceIndices.isEmpty()) {
            // 没有空格，直接计算原始自动换行的布局
            return@remember calculateLayouts(words, emptySet(), showTransliteration)
        }
        
        // 步骤1：先计算原始自动换行的布局，得到原始行数
        val originalLayouts = calculateLayouts(words, emptySet(), showTransliteration)
        val originalLineCount = originalLayouts.size
        
        var bestLayouts = originalLayouts
        
        // 辅助函数：计算布局的平衡度（每行长度差异越小越好）
        fun calculateBalance(layouts: List<List<NewPreviewWordLayout>>): Double {
            if (layouts.isEmpty()) return 0.0
            val lineWidths = layouts.map { line ->
                line.sumOf { (it.textWidth + it.spaceWidth).toDouble() }
            }
            val avg = lineWidths.average()
            return lineWidths.sumOf { (it - avg) * (it - avg) }
        }
        
        var bestBalance = calculateBalance(originalLayouts)
        
        // 步骤2：尝试所有可能的空格组合（对于小数量空格）
        if (spaceIndices.size <= 5) {
            // 对于较少的空格，尝试所有组合
            val allSubsets = mutableListOf<List<Int>>()
            fun generateSubsets(start: Int, current: List<Int>) {
                if (current.isNotEmpty()) {
                    allSubsets.add(current)
                }
                for (i in start until spaceIndices.size) {
                    generateSubsets(i + 1, current + spaceIndices[i])
                }
            }
            generateSubsets(0, emptyList())
            
            // 尝试所有子集，找到最佳平衡
            for (subset in allSubsets) {
                val breakable = subset.toSet()
                val testLayouts = calculateLayouts(words, breakable, showTransliteration)
                
                if (testLayouts.size <= originalLineCount) {
                    val balance = calculateBalance(testLayouts)
                    if (balance < bestBalance) {
                        bestBalance = balance
                        bestLayouts = testLayouts
                    }
                }
            }
        } else {
            // 对于较多空格，用启发式策略
            // 策略1：尝试只保留第一个空格换行
            if (spaceIndices.isNotEmpty()) {
                val breakable = setOf(spaceIndices.first())
                val testLayouts = calculateLayouts(words, breakable, showTransliteration)
                if (testLayouts.size <= originalLineCount) {
                    val balance = calculateBalance(testLayouts)
                    if (balance < bestBalance) {
                        bestBalance = balance
                        bestLayouts = testLayouts
                    }
                }
            }
            
            // 策略2：尝试只保留最后一个空格换行
            if (spaceIndices.isNotEmpty()) {
                val breakable = setOf(spaceIndices.last())
                val testLayouts = calculateLayouts(words, breakable, showTransliteration)
                if (testLayouts.size <= originalLineCount) {
                    val balance = calculateBalance(testLayouts)
                    if (balance < bestBalance) {
                        bestBalance = balance
                        bestLayouts = testLayouts
                    }
                }
            }
            
            // 策略3：尝试保留最后两个空格换行
            if (spaceIndices.size >= 2) {
                val breakable = spaceIndices.takeLast(2).toSet()
                val testLayouts = calculateLayouts(words, breakable, showTransliteration)
                if (testLayouts.size <= originalLineCount) {
                    val balance = calculateBalance(testLayouts)
                    if (balance < bestBalance) {
                        bestBalance = balance
                        bestLayouts = testLayouts
                    }
                }
            }
            
            // 策略4：尝试只在中间的某个空格换行
            if (spaceIndices.size >= 2) {
                val mid = spaceIndices.size / 2
                val breakable = setOf(spaceIndices[mid])
                val testLayouts = calculateLayouts(words, breakable, showTransliteration)
                if (testLayouts.size <= originalLineCount) {
                    val balance = calculateBalance(testLayouts)
                    if (balance < bestBalance) {
                        bestBalance = balance
                        bestLayouts = testLayouts
                    }
                }
            }
            
            // 策略5：尝试每隔一个空格换行（平衡的分割）
            val everyOther = mutableListOf<Int>()
            for (i in spaceIndices.indices step 2) {
                everyOther.add(spaceIndices[i])
            }
            if (everyOther.isNotEmpty()) {
                val breakable = everyOther.toSet()
                val testLayouts = calculateLayouts(words, breakable, showTransliteration)
                if (testLayouts.size <= originalLineCount) {
                    val balance = calculateBalance(testLayouts)
                    if (balance < bestBalance) {
                        bestBalance = balance
                        bestLayouts = testLayouts
                    }
                }
            }
        }
        
        // 最终如果所有策略都不行，返回原始布局
        bestLayouts
    }
    
    // 检查是否有注音
    val hasTransliteration = showTransliteration && words.any { 
        it.transliteration.isNotEmpty() || it.charTransliterations.isNotEmpty() 
    }
    
    // 调整行高，确保注音不被遮挡
    // 基础行高 + 注音空间（默认歌词字号的1/4）
    val baseLineHeight = with(density) { (fontSize.value * 1.5).dp.toPx() }
    val extraTransSpace = with(density) { (fontSize.value / 4).dp.toPx() }
    val lineHeight = if (hasTransliteration) {
        baseLineHeight + extraTransSpace
    } else {
        baseLineHeight
    }
    val totalHeight = lineLayouts.size * lineHeight
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isDuet) Alignment.End else Alignment.Start
    ) {
        lineLayouts.forEach { lineWords ->
            // 找到第一个非空格字符的索引
            val firstNonSpaceIndex = lineWords.indexOfFirst { !it.word.text.startsWith(" ") }
            val effectiveLineWords = if (firstNonSpaceIndex > 0) {
                // 跳过前导空格，并重新计算位置（从0开始）
                val skippedWidth = lineWords.take(firstNonSpaceIndex).sumOf { it.textWidth.toDouble() }.toFloat()
                lineWords.drop(firstNonSpaceIndex).map { layout ->
                    layout.copy(
                        startPosition = layout.startPosition - skippedWidth,
                        endPosition = layout.endPosition - skippedWidth
                    )
                }
            } else {
                lineWords
            }
            
            val lineWidth = effectiveLineWords.lastOrNull()?.endPosition ?: 0f
            
            Box(
                modifier = Modifier
                    .width(with(density) { lineWidth.toDp() })
                    .height(with(density) { lineHeight.toDp() })
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    effectiveLineWords.forEach { layout ->
                        val word = layout.word
                        val isPassed = currentTime >= word.end
                        val isActive = currentTime >= word.begin && currentTime < word.end
                        val isFuture = currentTime < word.begin
                        
                        // 计算进度
                        val safeWordDuration = word.duration.coerceAtLeast(1L)
                        val progress = when {
                            isFuture -> 0f
                            isPassed -> 1f
                            else -> ((currentTime - word.begin).toFloat() / safeWordDuration.toFloat())
                                .takeIf { it.isFinite() }
                                ?.coerceIn(0f, 1f)
                                ?: 0f
                        }
                        
                        // 计算上抬偏移（每个字独立计算，不互相影响）
                        val liftOffset = liftAnimator.getLiftOffset(word, currentTime, density)
                        
                        // 绘制文字和注音
                        drawWordWithTransliteration(
                            layout = layout,
                            progress = progress,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            liftOffset = liftOffset,
                            fontSizePx = fontSizePx,
                            lineHeight = lineHeight,
                            hasTransliteration = hasTransliteration,
                            density = density,
                            fontSize = fontSize,
                            fontWeight = fontWeight // 新增
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawWordWithTransliteration(
    layout: NewPreviewWordLayout,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    liftOffset: Float,
    fontSizePx: Float,
    lineHeight: Float,
    hasTransliteration: Boolean,
    density: Density,
    fontSize: TextUnit,
    fontWeight: Int = 400 // 新增：字体粗细
) {
    val word = layout.word
    
    // 根据 font weight 选择 typeface 和粗细效果
    val paint = android.graphics.Paint().apply {
        this.textSize = fontSizePx
        this.isAntiAlias = true
        applyAndroidFontWeight(this, fontWeight)
    }
    
    val textX = layout.startPosition
    
    // 计算主歌词和注音的基线位置
    val fontMetrics = paint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent
    
    // 有注音时的布局调整
    val contentHeight = if (word.transliteration.isNotEmpty() && word.text != " ") {
        // 有注音时，需要计算总高度
        textHeight + (with(density) { (fontSize.value / 2).sp.toPx() }) * 1.2f
    } else {
        textHeight
    }
    
    // 计算主歌词基线位置（保持歌词正常对齐）
    // 有注音时，歌词向下偏移一点，给注音留出空间
    val mainBaseLineY = (lineHeight + textHeight) / 2f - fontMetrics.descent - liftOffset + 
        (if (hasTransliteration) (with(density) { (fontSize.value / 6).sp.toPx() }) else 0f)
    
    // 绘制主歌词
    val baseInactiveColor = inactiveColor
    
    // 1. 绘制背景层（灰色）
    drawContext.canvas.nativeCanvas.drawText(
        word.text,
        textX,
        mainBaseLineY,
        paint.apply { color = baseInactiveColor.toArgb() }
    )
    
    // 2. 绘制高亮层
    if (progress > 0f) {
        if (progress >= 1f) {
            // 已完成歌词
            paint.color = activeColor.toArgb()
            paint.shader = null
            drawContext.canvas.nativeCanvas.drawText(word.text, textX, mainBaseLineY, paint)
        } else {
            val totalWidth = layout.textWidth + layout.spaceWidth
            val highlightWidth = totalWidth * progress
            
            val shader = createGradientShader(
                startX = textX,
                endX = textX + highlightWidth,
                activeColor = activeColor,
                inactiveColor = baseInactiveColor,
                progress = progress,
                hasSpace = layout.spaceWidth > 0
            )
            
            paint.shader = shader
            drawContext.canvas.nativeCanvas.drawText(word.text, textX, mainBaseLineY, paint)
        }
    }
    
    // 只在非空格字符时绘制注音，并且需要检查是否要显示注音
    val transliteration = if (word.charTransliterations.isNotEmpty() && word.text.isNotEmpty()) {
        // 优先使用单字符注音
        word.charTransliterations[0] ?: ""
    } else {
        word.transliteration
    }
    if (word.text != " " && transliteration.isNotEmpty() && hasTransliteration) {
        // 判断注音文本是否需要加空格（看注音本身，不是歌词字符）
        val needsSpace = isTransliterationNeedsSpace(transliteration)
        val finalTransliteration = if (needsSpace) {
            transliteration + " "
        } else {
            transliteration
        }
        
        // 绘制注音（位于字符正上方
        val transliterationPaint = android.graphics.Paint().apply {
            // 字号为歌词的一半
            this.textSize = with(density) { (fontSize.value / 2).sp.toPx() }
            this.isAntiAlias = true
            this.color = inactiveColor.toArgb()
            applyAndroidFontWeight(this, fontWeight)
        }
        
        val transFontMetrics = transliterationPaint.fontMetrics
        val transTextWidth = transliterationPaint.measureText(finalTransliteration)
        val charWidth = layout.textWidth
        
        // 计算注音的垂直位置（字符正上方，增加距离）
        val transTextHeight = transFontMetrics.descent - transFontMetrics.ascent
        // 注音上抬距离按字号比例缩小（注音字号是歌词的一半）
        val transLiftOffset = liftOffset * 0.5f
        val transBaseLineY = mainBaseLineY - textHeight * 0.28f - transTextHeight - transLiftOffset
        
        // 计算注音的水平位置
        val finalTransX = if (transTextWidth > charWidth) {
            // 注音更长，左对齐
            textX
        } else {
            // 注音更短，居中显示
            textX + (charWidth - transTextWidth) / 2f
        }
        
        // 1. 绘制注音背景层（使用inactiveColor）
        drawContext.canvas.nativeCanvas.drawText(
            finalTransliteration,
            finalTransX,
            transBaseLineY,
            transliterationPaint.apply { color = baseInactiveColor.toArgb() }
        )
        
        // 2. 绘制注音高亮层（随歌词进度一起高亮）
        if (progress > 0f) {
            if (progress >= 1f) {
                // 已完成：使用高亮颜色
                transliterationPaint.color = activeColor.toArgb()
                transliterationPaint.shader = null
                drawContext.canvas.nativeCanvas.drawText(
                    finalTransliteration,
                    finalTransX,
                    transBaseLineY,
                    transliterationPaint
                )
            } else {
                // 正在播放：使用渐变效果
                val totalWidth = transTextWidth
                val highlightWidth = totalWidth * progress
                
                val shader = createGradientShader(
                    startX = finalTransX,
                    endX = finalTransX + highlightWidth,
                    activeColor = activeColor,
                    inactiveColor = baseInactiveColor,
                    progress = progress,
                    hasSpace = false
                )
                
                transliterationPaint.shader = shader
                drawContext.canvas.nativeCanvas.drawText(
                    finalTransliteration,
                    finalTransX,
                    transBaseLineY,
                    transliterationPaint
                )
            }
        }
    }
}

private fun createGradientShader(
    startX: Float,
    endX: Float,
    activeColor: Color,
    inactiveColor: Color,
    progress: Float,
    hasSpace: Boolean = false
): Shader {
    // 如果有空格，调整渐变范围使过渡更自然
    val adjustedEndX = if (hasSpace) endX else endX
    val featherWidth = (adjustedEndX - startX) * 0.35f
    val gradientStart = (adjustedEndX - featherWidth).coerceAtLeast(startX)
    
    // 如果有空格，使用更平滑的渐变过渡
    val colors = if (hasSpace && progress < 1f) {
        // 在空格区域添加过渡色，使效果更自然
        intArrayOf(
            activeColor.toArgb(),
            activeColor.toArgb(),
            inactiveColor.copy(alpha = 0.5f).toArgb(),
            inactiveColor.toArgb()
        )
    } else {
        intArrayOf(
            activeColor.toArgb(),
            activeColor.toArgb(),
            inactiveColor.toArgb()
        )
    }
    
    val positions = if (hasSpace && progress < 1f) {
        floatArrayOf(0f, (gradientStart - startX) / (adjustedEndX - startX), 0.9f, 1f)
    } else {
        floatArrayOf(0f, (gradientStart - startX) / (adjustedEndX - startX), 1f)
    }
    
    return android.graphics.LinearGradient(
        startX,
        0f,
        adjustedEndX,
        0f,
        colors,
        positions,
        android.graphics.Shader.TileMode.CLAMP
    )
}

// ==================== 播放控制 ====================

@Composable
fun PlaybackControls(
    currentTime: Long,
    duration: Long,
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    seekTimeMs: Long,
    seekTimeSeconds: Float,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    vibrantColor: Color? = null,
    backgroundColor: Color? = null
) {
    val minSeekStartMs = 240L
    val safeDuration = duration.coerceAtLeast(0L)
    val seekStart = if (safeDuration > minSeekStartMs) minSeekStartMs else 0L
    val seekSpan = (safeDuration - seekStart).coerceAtLeast(0L)
    val clampedCurrentTime = currentTime.coerceIn(0L, safeDuration)
    val sliderProgress = if (seekSpan > 0L) {
        ((clampedCurrentTime - seekStart).toFloat() / seekSpan.toFloat())
            .takeIf { it.isFinite() }
            ?.coerceIn(0f, 1f)
            ?: 0f
    } else {
        0f
    }
    
    val controlBackground = backgroundColor ?: MaterialTheme.colorScheme.surface
    val controlOnBackground = getHighContrastBlackOrWhite(controlBackground)
    val themePrimaryColor = vibrantColor ?: MaterialTheme.colorScheme.primary
    val buttonColor = ensureReadableColor(
        candidate = blendColors(themePrimaryColor, controlOnBackground, 0.16f),
        background = controlBackground,
        fallback = blendColors(controlBackground, controlOnBackground, 0.22f),
        minContrast = 2.2f
    )
    val onPrimaryColor = getHighContrastBlackOrWhite(buttonColor)
    val outlineColor = ensureReadableColor(
        candidate = blendColors(controlBackground, controlOnBackground, 0.18f),
        background = controlBackground,
        fallback = controlOnBackground.copy(alpha = 0.24f),
        minContrast = 1.2f
    )
    val progressColor = ensureReadableColor(
        candidate = blendColors(themePrimaryColor, onPrimaryColor, 0.12f),
        background = controlBackground,
        fallback = controlOnBackground,
        minContrast = 2.8f
    )
    val progressTrackColor = ensureReadableColor(
        candidate = blendColors(controlBackground, controlOnBackground, 0.32f),
        background = controlBackground,
        fallback = controlOnBackground.copy(alpha = 0.30f),
        minContrast = 1.3f
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(controlBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 播放/暂停按钮
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    buttonColor,
                    CircleShape
                )
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                ),
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = onPrimaryColor,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // 进度条，外圈有高对比度的颜色
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .background(
                    color = outlineColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(6.dp)
        ) {
            // 计算显示用的进度，用于LinearProgressIndicator
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
            
            // 可拖动的进度条
            Slider(
                value = sliderProgress,
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
                    onSeek(newPosition.coerceIn(0L, safeDuration))
                },
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

fun formatPreviewTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun isPreviewCJKCharacter(char: Char): Boolean {
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

// 判断注音文本是否为非CJK注音（如拉丁拼音），需要在后面加空格
private fun isTransliterationNeedsSpace(transliteration: String): Boolean {
    if (transliteration.isEmpty()) return false
    // 检查是否包含非CJK字符（如拉丁字母）
    // 如果注音中有拉丁字母或其他非CJK字符，就认为需要加空格
    var hasNonCJK = false
    for (char in transliteration) {
        if (!isPreviewCJKCharacter(char)) {
            hasNonCJK = true
            break
        }
    }
    return hasNonCJK
}

// ==================== 新的Header组件 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricPreviewHeader(
    title: String,
    artist: String,
    coverBitmap: Bitmap?,
    onBackClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    menuContent: @Composable (menuButtonPosition: MenuAnchorPosition?) -> Unit = {},
    mutedColor: Color? = null,
    isDarkTheme: Boolean = false,
    backgroundColor: Color? = null
) {
    val headerBackground = backgroundColor ?: MaterialTheme.colorScheme.surface
    val textColor = getHighContrastBlackOrWhite(headerBackground)
    val artistColor = ensureReadableColor(
        candidate = textColor.copy(alpha = 0.76f),
        background = headerBackground,
        fallback = textColor,
        minContrast = 3.4f
    )
    val iconColor = getHighContrastBlackOrWhite(headerBackground)
    var menuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(headerBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                    contentDescription = "返回",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 封面
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap.asImageBitmap(),
                    contentDescription = "封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(75.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "音乐",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 标题和艺术家
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = artist,
                    fontSize = 16.sp,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 菜单按钮
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .onGloballyPositioned { coordinates ->
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
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 菜单内容
            menuContent(menuButtonPosition)
        }
    }
}

// ==================== 加载音频元数据的辅助函数 ====================

data class PreviewAudioMetadata(
    val title: String,
    val artist: String,
    val coverBitmap: Bitmap?
)

suspend fun loadAudioMetadata(context: Context, audioPath: String, fallbackTitle: String): PreviewAudioMetadata {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("LyricPreview", "Loading metadata from: $audioPath")
            val file = java.io.File(audioPath)
            if (!file.exists()) {
                Log.d("LyricPreview", "File not found")
                return@withContext PreviewAudioMetadata(fallbackTitle, "未知艺术家", null)
            }

            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val tagData = AudioTagReader.read(pfd, true)
            pfd.close()

            val title = tagData.title?.takeIf { it.isNotEmpty() } ?: fallbackTitle
            val artist = tagData.artist?.takeIf { it.isNotEmpty() } ?: "未知艺术家"
            Log.d("LyricPreview", "Title: $title, Artist: $artist")
            Log.d("LyricPreview", "Number of pictures: ${tagData.pictures.size}")
            val coverData = tagData.pictures.firstOrNull()?.data
            Log.d("LyricPreview", "Cover data size: ${coverData?.size ?: 0}")
            val coverBitmap = coverData?.let {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                Log.d("LyricPreview", "Decoded bitmap: $bitmap")
                bitmap
            }

            PreviewAudioMetadata(title, artist, coverBitmap)
        } catch (e: Exception) {
            Log.e("LyricPreview", "Failed to load metadata", e)
            PreviewAudioMetadata(fallbackTitle, "未知艺术家", null)
        }
    }
}
