package com.example.LyricBox

import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.lonx.audiotag.rw.AudioTagReader
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.compose.*
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.modifier.springPlacement
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.AudioMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

private const val AUTO_SCROLL_LOG_TAG = "LyricPreviewScroll"
private const val CREATOR_VS_LYRIC_LOG_TAG = "LyricPreviewCreatorSync"
private const val ENABLE_LYRIC_AUTOSCROLL_DEBUG_LOG = false
private const val ENABLE_INTERLUDE_SCROLL_DIAGNOSTIC_LOG = true
private const val DEBUG_KEEP_INTERLUDE_PLACEHOLDER_AFTER_END = false
private const val DEBUG_INTERLUDE_PLACEHOLDER_HOLD_MS = 420L
private const val ENABLE_CREATOR_SYNC_TRACE = false
private const val PLAYED_LINE_BLUR_RADIUS = 2f
private const val UPCOMING_LINE_MAX_BLUR_RADIUS = 15f
private const val UPCOMING_LINE_BLUR_STEP = 4f
private const val COMPANION_AUDIO_LOG_TAG = "LyricPreviewCompanion"

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

private fun resolveForceWordLineEnd(
    line: NewPreviewLyricLine,
    nextLine: NewPreviewLyricLine?,
    songDuration: Long
): Long {
    val fallback = when {
        line.end > 0L -> line.end
        nextLine != null && nextLine.begin > 0L -> nextLine.begin
        songDuration > 0L -> songDuration
        else -> line.begin + 1L
    }
    return fallback.coerceAtLeast(line.begin + 1L).coerceAtLeast(1L)
}

private fun isAsciiWordChar(c: Char): Boolean {
    return (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9')
}

private fun isLiftStaggerLetterChar(c: Char): Boolean {
    return when (Character.UnicodeScript.of(c.code)) {
        Character.UnicodeScript.LATIN,
        Character.UnicodeScript.CYRILLIC -> Character.isLetter(c)
        else -> false
    }
}

private fun isLiftStaggerPunctuationChar(c: Char): Boolean {
    return c == '\'' ||
        c == '’' ||
        c == '‘' ||
        c == '-' ||
        c == '‐' ||
        c == '‑' ||
        c == '–' ||
        c == '—' ||
        c == ',' ||
        c == '.' ||
        c == '!' ||
        c == '?' ||
        c == ':' ||
        c == ';' ||
        c == '"' ||
        c == '“' ||
        c == '”' ||
        c == '(' ||
        c == ')' ||
        c == '[' ||
        c == ']'
}

private fun isSingleLiftStaggerLetterText(text: String): Boolean {
    return text.length == 1 && isLiftStaggerLetterChar(text[0])
}

private fun isSingleLiftStaggerParticipantText(text: String): Boolean {
    return text.length == 1 && (isLiftStaggerLetterChar(text[0]) || isLiftStaggerPunctuationChar(text[0]))
}

private fun isForeignGlowText(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.length > 1 &&
        trimmed.any { isLiftStaggerLetterChar(it) } &&
        trimmed.all { isLiftStaggerLetterChar(it) || isLiftStaggerPunctuationChar(it) }
}
//英文字母上抬开始时间及长持续时间间隔阈值
private const val LONG_ASCII_LETTER_LIFT_AVG_DURATION_MS = 70L
private const val ASCII_LETTER_LIFT_STAGGER_MS = 60L
private const val MIN_CJK_LIFT_AVG_DURATION_MS = 100L
private const val MAX_CJK_LIFT_AVG_DURATION_MS = 400L
private const val CJK_LIFT_STAGGER_MS = 100L

private fun isHanChar(c: Char): Boolean {
    return Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN
}

private fun isCjkGlowChar(c: Char): Boolean {
    return when (Character.UnicodeScript.of(c.code)) {
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.HANGUL,
        Character.UnicodeScript.BOPOMOFO -> true
        else -> false
    }
}

private fun isSingleCjkGlowText(text: String): Boolean {
    return text.length == 1 && isCjkGlowChar(text[0])
}

private fun tokenizeForceWordText(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val tokens = mutableListOf<String>()
    val latinBuffer = StringBuilder()
    fun flushLatin() {
        if (latinBuffer.isNotEmpty()) {
            tokens.add(latinBuffer.toString())
            latinBuffer.clear()
        }
    }

    text.forEach { rawChar ->
        val ch = when (rawChar) {
            '\u00A0', '\u2009' -> ' '
            else -> rawChar
        }
        when {
            ch.isWhitespace() -> {
                flushLatin()
                if (tokens.lastOrNull() != " ") {
                    tokens.add(" ")
                }
            }
            isAsciiWordChar(ch) -> {
                latinBuffer.append(ch)
            }
            (ch == '\'' || ch == '’' || ch == '-') && latinBuffer.isNotEmpty() -> {
                latinBuffer.append(ch)
            }
            isHanChar(ch) -> {
                flushLatin()
                tokens.add(ch.toString())
            }
            else -> {
                flushLatin()
                if (tokens.isNotEmpty() && tokens.last() != " ") {
                    tokens[tokens.lastIndex] = tokens.last() + ch
                } else {
                    tokens.add(ch.toString())
                }
            }
        }
    }
    flushLatin()
    return tokens
}

private fun buildForceWordWords(
    line: NewPreviewLyricLine,
    nextLine: NewPreviewLyricLine?,
    songDuration: Long
): List<NewPreviewLyricWord> {
    if (line.words.isEmpty()) return emptyList()

    val forceLineEnd = resolveForceWordLineEnd(line, nextLine, songDuration)
    val normalizedLineBegin = line.begin.coerceAtLeast(0L)
    val normalizedWords = line.words.map { word ->
        val normalizedBegin = if (word.begin > 0L) word.begin else normalizedLineBegin
        val normalizedEnd = if (word.end > 0L) {
            word.end
        } else {
            forceLineEnd.coerceAtLeast(normalizedBegin + 1L)
        }
        word.copy(
            begin = normalizedBegin,
            end = normalizedEnd,
            duration = (normalizedEnd - normalizedBegin).coerceAtLeast(1L)
        )
    }

    if (!line.isLineByLineLyric()) {
        return normalizedWords
    }

    val fullText = normalizedWords.joinToString(separator = "") { it.text }
    val tokens = tokenizeForceWordText(fullText)
    if (tokens.isEmpty()) {
        return normalizedWords
    }

    val visibleTokenIndices = tokens.indices.filter { tokens[it].isNotBlank() }
    if (visibleTokenIndices.isEmpty()) {
        return normalizedWords
    }

    val lineStart = normalizedWords.firstOrNull()?.begin?.coerceAtLeast(0L) ?: normalizedLineBegin
    val lineEnd = maxOf(forceLineEnd, lineStart + visibleTokenIndices.size.toLong())
    val visibleCount = visibleTokenIndices.size
    val boundsByTokenIndex = mutableMapOf<Int, Pair<Long, Long>>()
    visibleTokenIndices.forEachIndexed { order, tokenIndex ->
        var begin = lineStart + ((lineEnd - lineStart) * order) / visibleCount
        var end = lineStart + ((lineEnd - lineStart) * (order + 1)) / visibleCount
        if (end <= begin) end = begin + 1L
        boundsByTokenIndex[tokenIndex] = begin to end
    }

    var cursor = lineStart
    return tokens.mapIndexed { idx, token ->
        val visibleBounds = boundsByTokenIndex[idx]
        val (begin, end) = if (visibleBounds != null) {
            cursor = visibleBounds.second
            visibleBounds
        } else {
            val safeBegin = cursor.coerceAtMost(lineEnd - 1L)
            val safeEnd = (safeBegin + 1L).coerceAtMost(lineEnd)
            safeBegin to if (safeEnd <= safeBegin) safeBegin + 1L else safeEnd
        }
        NewPreviewLyricWord(
            text = token,
            begin = begin,
            end = end,
            duration = (end - begin).coerceAtLeast(1L)
        )
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

private fun applyLiftStartOverridesForRange(
    overrides: MutableMap<Int, Long>,
    words: List<NewPreviewWordLayout>,
    start: Int,
    endExclusive: Int,
    averageDivisor: Int
) {
    if (endExclusive <= start || averageDivisor <= 0) return
    val runBegin = words[start].word.begin
    val runEnd = words.subList(start, endExclusive).maxOf { it.word.end }
    val averageDuration = ((runEnd - runBegin).coerceAtLeast(0L)) / averageDivisor
    if (averageDuration >= LONG_ASCII_LETTER_LIFT_AVG_DURATION_MS) {
        for (runIndex in start until endExclusive) {
            overrides[runIndex] = runBegin + (runIndex - start) * ASCII_LETTER_LIFT_STAGGER_MS
        }
    }
}

private fun applyCjkLiftStartOverridesForRange(
    overrides: MutableMap<Int, Long>,
    words: List<NewPreviewWordLayout>,
    start: Int,
    endExclusive: Int,
    averageDivisor: Int
) {
    if (endExclusive <= start || averageDivisor <= 0) return
    val runBegin = words[start].word.begin
    val runEnd = words.subList(start, endExclusive).maxOf { it.word.end }
    val averageDuration = ((runEnd - runBegin).coerceAtLeast(0L)) / averageDivisor
    if (averageDuration !in MIN_CJK_LIFT_AVG_DURATION_MS..MAX_CJK_LIFT_AVG_DURATION_MS) return

    var previousCjkBegin: Long? = null
    for (runIndex in start until endExclusive) {
        val word = words[runIndex].word
        if (!isSingleCjkGlowText(word.text)) continue
        overrides[runIndex] = previousCjkBegin?.plus(CJK_LIFT_STAGGER_MS) ?: word.begin
        previousCjkBegin = word.begin
    }
}

private fun buildLongLyricLiftStartOverrides(
    words: List<NewPreviewWordLayout>
): Map<Int, Long> {
    val overrides = mutableMapOf<Int, Long>()
    var index = 0
    while (index < words.size) {
        val text = words[index].word.text

        if (isSingleLiftStaggerLetterText(text)) {
            val runStart = index
            var runEndExclusive = index + 1
            while (
                runEndExclusive < words.size &&
                isSingleLiftStaggerParticipantText(words[runEndExclusive].word.text)
            ) {
                runEndExclusive++
            }

            val letterCount = words
                .subList(runStart, runEndExclusive)
                .count { isSingleLiftStaggerLetterText(it.word.text) }
            if (letterCount >= 2) {
                applyLiftStartOverridesForRange(
                    overrides = overrides,
                    words = words,
                    start = runStart,
                    endExclusive = runEndExclusive,
                    averageDivisor = runEndExclusive - runStart
                )
            }

            index = runEndExclusive
            continue
        }

        if (isSingleCjkGlowText(text)) {
            val runStart = index
            var runEndExclusive = index + 1
            while (
                runEndExclusive < words.size &&
                words[runEndExclusive].word.text != " "
            ) {
                runEndExclusive++
            }

            val cjkCount = words
                .subList(runStart, runEndExclusive)
                .count { isSingleCjkGlowText(it.word.text) }
            if (cjkCount >= 2) {
                applyCjkLiftStartOverridesForRange(
                    overrides = overrides,
                    words = words,
                    start = runStart,
                    endExclusive = runEndExclusive,
                    averageDivisor = runEndExclusive - runStart
                )
            }

            index = runEndExclusive
            continue
        }

        index++
    }
    return overrides
}

private enum class LyricGlowLevel {
    VERY_SLIGHT,
    SLIGHT,
    NORMAL,
    HIGH
}

private data class LyricGlowState(
    val level: LyricGlowLevel,
    val alpha: Float,
    val playbackScale: Float = 1f,
    val textScale: Float = 1f
)

private const val LYRIC_GLOW_FADE_DURATION_MS = 200L
private const val LYRIC_GLOW_LEAD_IN_MS = 200L
private const val LYRIC_GLOW_FADE_OUT_DELAY_MS = 300L
private const val LYRIC_GLOW_FADE_OUT_DURATION_MS = 400L
private const val LYRIC_GLOW_PRE_PLAY_ALPHA = 0.20f
private const val LYRIC_GLOW_MAX_ALPHA = 1.00f
private const val LYRIC_GLOW_FOREIGN_HOLD_DELTA = 0.02f
private val LYRIC_GLOW_FOREIGN_GROW_EASING = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val LYRIC_GLOW_FOREIGN_SHRINK_EASING = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
//CJK字符发光阈值
private fun resolveCjkGlowLevel(durationMs: Long): LyricGlowLevel? {
    return when {
        durationMs >= 1800L -> LyricGlowLevel.VERY_SLIGHT
        durationMs >= 1200L -> LyricGlowLevel.VERY_SLIGHT
        else -> null
    }
}

private fun resolveForeignWordGlowLevel(averageDurationMs: Long): LyricGlowLevel? {
    return when {
        averageDurationMs > 550L -> LyricGlowLevel.NORMAL
        averageDurationMs > 400L -> LyricGlowLevel.SLIGHT
        averageDurationMs > 240L -> LyricGlowLevel.VERY_SLIGHT
        else -> null
    }
}

private fun resolveForeignWordGlowLevel(durationMs: Long, letterCount: Int): LyricGlowLevel? {
    val safeCount = letterCount.coerceAtLeast(1)
    val divisor = if (safeCount <= 2) 3 else safeCount
    return resolveForeignWordGlowLevel(durationMs / divisor)
}

private fun computeLyricGlowAlpha(
    currentTime: Long,
    glowStart: Long,
    contentBegin: Long,
    contentEnd: Long
): Float {
    val fadeOutStart = contentEnd + LYRIC_GLOW_FADE_OUT_DELAY_MS
    val glowEnd = fadeOutStart + LYRIC_GLOW_FADE_OUT_DURATION_MS
    if (currentTime < glowStart || currentTime > glowEnd) return 0f
    val fadeDuration = LYRIC_GLOW_FADE_OUT_DURATION_MS.coerceAtLeast(1L)
    val leadInDuration = (contentBegin - glowStart).coerceAtLeast(1L)
    val leadInProgress = ((currentTime - glowStart).toFloat() / leadInDuration.toFloat()).coerceIn(0f, 1f)
    val fadeInAlpha = LYRIC_GLOW_PRE_PLAY_ALPHA +
        (LYRIC_GLOW_MAX_ALPHA - LYRIC_GLOW_PRE_PLAY_ALPHA) * leadInProgress
    return when {
        currentTime < contentBegin -> fadeInAlpha
        currentTime < fadeOutStart -> LYRIC_GLOW_MAX_ALPHA
        else -> {
            val fadeProgress = ((currentTime - fadeOutStart).toFloat() / fadeDuration.toFloat())
                .coerceIn(0f, 1f)
            val easedFade = 1f - FastOutLinearInEasing.transform(fadeProgress)
            LYRIC_GLOW_MAX_ALPHA * easedFade.coerceIn(0f, 1f)
        }
    }
}

private fun computeLyricGlowPlaybackScale(currentTime: Long, itemBegin: Long): Float {
    if (currentTime < itemBegin) return 0.34f
    val progress = ((currentTime - itemBegin).toFloat() / LYRIC_GLOW_FADE_DURATION_MS.toFloat())
        .coerceIn(0f, 1f)
    return 0.34f + (1f - 0.34f) * progress
}

private fun getLyricGlowTargetScale(level: LyricGlowLevel): Float {
    return when (level) {
        LyricGlowLevel.VERY_SLIGHT -> 1.03f
        LyricGlowLevel.SLIGHT -> 1.05f
        LyricGlowLevel.NORMAL,
        LyricGlowLevel.HIGH -> 1.07f
    }
}

private fun getLyricGlowScaleAnimDurationMs(level: LyricGlowLevel): Long {
    return when (level) {
        LyricGlowLevel.VERY_SLIGHT -> 480L
        LyricGlowLevel.SLIGHT -> 800L
        LyricGlowLevel.NORMAL,
        LyricGlowLevel.HIGH -> 1100L
    }
}

private fun getLyricGlowScaleStaggerMs(level: LyricGlowLevel): Long {
    return when (level) {
        LyricGlowLevel.VERY_SLIGHT -> 125L
        LyricGlowLevel.SLIGHT -> 200L
        LyricGlowLevel.NORMAL,
        LyricGlowLevel.HIGH -> 275L
    }
}

private fun easedScaleLerp(
    start: Float,
    end: Float,
    progress: Float,
    easing: Easing = FastOutSlowInEasing
): Float {
    val eased = easing.transform(progress.coerceIn(0f, 1f))
    return start + (end - start) * eased
}

private fun computeLyricGlowScale(
    currentTime: Long,
    begin: Long,
    end: Long,
    level: LyricGlowLevel
): Float {
    if (currentTime < begin || currentTime >= end) return 1f
    val targetScale = getLyricGlowTargetScale(level)
    val animDuration = getLyricGlowScaleAnimDurationMs(level)
    val duration = (end - begin).coerceAtLeast(1L)
    val inDuration = animDuration
        .coerceAtMost(duration / 2L)
        .coerceAtLeast(1L)
    val outDuration = animDuration
        .coerceAtMost(duration - inDuration)
        .coerceAtLeast(1L)
    val growEnd = begin + inDuration
    val shrinkStart = (end - outDuration).coerceAtLeast(growEnd)
    return when {
        currentTime < growEnd -> {
            val progress = (currentTime - begin).toFloat() / inDuration.toFloat()
            easedScaleLerp(1f, targetScale, progress)
        }
        currentTime >= shrinkStart -> {
            val progress = (currentTime - shrinkStart).toFloat() / (end - shrinkStart).coerceAtLeast(1L).toFloat()
            easedScaleLerp(targetScale, 1f, progress)
        }
        else -> targetScale
    }
}

private fun computeForeignMultiLetterGlowScale(
    currentTime: Long,
    scaleBegin: Long,
    letterEnd: Long,
    wordEnd: Long,
    level: LyricGlowLevel
): Float {
    val targetScale = getLyricGlowTargetScale(level)
    val animDuration = getLyricGlowScaleAnimDurationMs(level)
    val holdScale = (targetScale - LYRIC_GLOW_FOREIGN_HOLD_DELTA).coerceAtLeast(1f)
    if (currentTime < scaleBegin) return 1f

    val growDuration = animDuration.coerceAtLeast(1L)
    val growEnd = scaleBegin + growDuration
    if (currentTime < growEnd) {
        val progress = (currentTime - scaleBegin).toFloat() / growDuration.toFloat()
        return easedScaleLerp(1f, targetScale, progress, LYRIC_GLOW_FOREIGN_GROW_EASING)
    }

    val hasIntermediateHold = letterEnd < wordEnd
    if (hasIntermediateHold) {
        val settleStart = letterEnd
        if (currentTime < settleStart) return targetScale
        val settleDuration = animDuration.coerceAtLeast(1L)
        val settleEnd = settleStart + settleDuration
        if (currentTime < settleEnd) {
            val progress = (currentTime - settleStart).toFloat() / settleDuration.toFloat()
            return easedScaleLerp(targetScale, holdScale, progress, LYRIC_GLOW_FOREIGN_SHRINK_EASING)
        }
        if (currentTime < wordEnd) return holdScale
    } else if (currentTime < wordEnd) {
        return targetScale
    }

    val finalDuration = animDuration.coerceAtLeast(1L)
    val finalEnd = wordEnd + finalDuration
    if (currentTime < finalEnd) {
        val startScale = if (hasIntermediateHold) holdScale else targetScale
        val progress = (currentTime - wordEnd).toFloat() / finalDuration.toFloat()
        return easedScaleLerp(startScale, 1f, progress, LYRIC_GLOW_FOREIGN_SHRINK_EASING)
    }
    return 1f
}

private fun buildLyricGlowStates(
    words: List<NewPreviewWordLayout>,
    currentTime: Long
): Map<Int, LyricGlowState> {
    val glowStates = mutableMapOf<Int, LyricGlowState>()
    var index = 0
    while (index < words.size) {
        val word = words[index].word
        val text = word.text

        if (text == " ") {
            index++
            continue
        }

        if (isSingleCjkGlowText(text)) {
            val glowStart = word.begin - LYRIC_GLOW_LEAD_IN_MS
            val glowAlpha = computeLyricGlowAlpha(currentTime, glowStart, word.begin, word.end)
            val glowLevel = resolveCjkGlowLevel(word.duration)
            if (glowLevel != null && glowAlpha > 0f) {
                glowStates[index] = LyricGlowState(
                    level = glowLevel,
                    alpha = glowAlpha,
                    playbackScale = computeLyricGlowPlaybackScale(currentTime, word.begin),
                    textScale = computeLyricGlowScale(currentTime, word.begin, word.end, glowLevel)
                )
            }
            index++
            continue
        }

        if (isForeignGlowText(text)) {
            val letterCount = text.count { isLiftStaggerLetterChar(it) }.coerceAtLeast(1)
            val duration = word.duration.coerceAtLeast(0L)
            val glowLevel = resolveForeignWordGlowLevel(duration, letterCount)
            val glowStart = word.begin - LYRIC_GLOW_LEAD_IN_MS
            val glowAlpha = computeLyricGlowAlpha(currentTime, glowStart, word.begin, word.end)
            if (glowLevel != null) {
                val textScale = if (letterCount > 1) {
                    computeForeignMultiLetterGlowScale(
                        currentTime = currentTime,
                        scaleBegin = word.begin,
                        letterEnd = word.end,
                        wordEnd = word.end,
                        level = glowLevel
                    )
                } else {
                    computeLyricGlowScale(currentTime, word.begin, word.end, glowLevel)
                }
                if (glowAlpha > 0f || textScale > 1.0001f) {
                    glowStates[index] = LyricGlowState(
                        level = glowLevel,
                        alpha = glowAlpha,
                        playbackScale = computeLyricGlowPlaybackScale(currentTime, word.begin),
                        textScale = textScale
                    )
                }
            }
            index++
            continue
        }

        if (!isSingleLiftStaggerParticipantText(text)) {
            index++
            continue
        }

        val runStart = index
        var runEndExclusive = index + 1
        while (
            runEndExclusive < words.size &&
            words[runEndExclusive].word.text != " " &&
            !isSingleCjkGlowText(words[runEndExclusive].word.text) &&
            isSingleLiftStaggerParticipantText(words[runEndExclusive].word.text)
        ) {
            runEndExclusive++
        }

        val run = words.subList(runStart, runEndExclusive)
        val letterCount = run.count { isSingleLiftStaggerLetterText(it.word.text) }
        if (letterCount == 0) {
            index = runEndExclusive
            continue
        }
        val runBegin = run.minOf { it.word.begin }
        val runEnd = run.maxOf { it.word.end }
        val duration = (runEnd - runBegin).coerceAtLeast(0L)
        val glowLevel = resolveForeignWordGlowLevel(duration, letterCount)
        if (glowLevel != null) {
            val glowStart = runBegin - LYRIC_GLOW_LEAD_IN_MS
            val glowAlpha = computeLyricGlowAlpha(currentTime, glowStart, runBegin, runEnd)
            val staggerMs = getLyricGlowScaleStaggerMs(glowLevel)
            val runFirstLetterBegin = run.firstOrNull { isSingleLiftStaggerLetterText(it.word.text) }?.word?.begin
                ?: runBegin
            var letterOrderCounter = 0
            for (runIndex in runStart until runEndExclusive) {
                val runWord = words[runIndex].word
                val isLetterToken = isSingleLiftStaggerLetterText(runWord.text)
                val letterOrder = if (isLetterToken) {
                    val order = letterOrderCounter
                    letterOrderCounter++
                    order
                } else {
                    (letterOrderCounter - 1).coerceAtLeast(0)
                }
                val textScale = if (letterCount > 1) {
                    val scaleBegin = runFirstLetterBegin + letterOrder * staggerMs
                    computeForeignMultiLetterGlowScale(
                        currentTime = currentTime,
                        scaleBegin = scaleBegin,
                        letterEnd = runWord.end,
                        wordEnd = runEnd,
                        level = glowLevel
                    )
                } else {
                    computeLyricGlowScale(currentTime, runWord.begin, runWord.end, glowLevel)
                }
                if (glowAlpha > 0f || textScale > 1.0001f) {
                    glowStates[runIndex] = LyricGlowState(
                        level = glowLevel,
                        alpha = glowAlpha,
                        playbackScale = computeLyricGlowPlaybackScale(currentTime, runWord.begin),
                        textScale = textScale
                    )
                }
            }
        }

        index = runEndExclusive
    }
    return glowStates
}

// ==================== 歌词上抬动画状态 ====================

class WordLiftAnimator {
    private val completedWords = mutableSetOf<String>() // 记录已完成上抬的歌词
    private val wordLiftProgress = mutableMapOf<String, Float>() // 每个歌词的上抬进度
    
    fun getLiftOffset(
        word: NewPreviewLyricWord,
        currentTime: Long,
        density: Density,
        liftDistanceDp: Float,
        liftBeginMs: Long = word.begin,
        liftEndMs: Long = word.end + 300L
    ): Float {
        val wordKey = "${word.begin}_${word.text}"
        val adjustedBegin = liftBeginMs - 200L // 提前200ms开始上抬动画
        val liftDuration = (liftEndMs - adjustedBegin).coerceAtLeast(1L)
        val maxLiftOffset = with(density) {
            liftDistanceDp
                .coerceIn(
                    LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP,
                    LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP
                )
                .dp
                .toPx()
        }
        
        // 如果播放进度回退到未播放状态（包括等于开始时间的情况），重置该歌词的上抬状态
        if (currentTime <= adjustedBegin) {
            completedWords.remove(wordKey)
            wordLiftProgress.remove(wordKey)
            return 0f
        }
        
        // 如果已经完成上抬，保持最大上抬位置
        if (completedWords.contains(wordKey)) {
            return maxLiftOffset
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
        
        return maxLiftOffset * easedProgress
    }
    
    fun reset() {
        completedWords.clear()
        wordLiftProgress.clear()
    }
}

// ==================== Activity ====================

class LyricPreviewActivity : ComponentActivity() {
    private var mediaPlayer: ExoPlayer? = null
    private var companionPlayer: ExoPlayer? = null
    private var sharedPlaybackController: MusicPlaybackController? = null
    private var useSharedPlayback: Boolean = false
    private var currentPlaybackPosition: Long = 0L
    private var playbackCompleted by mutableStateOf(false)
    private var companionModeEnabled by mutableStateOf(false)
    private var previewAudioDuration by mutableLongStateOf(0L)
    private var previewLastKnownDuration by mutableLongStateOf(0L)
    private var previewConvertedAudioPath: String? = null
    private var isFallbackTranscoding by mutableStateOf(false)
    private var forceLyricDisplayOffToken by mutableIntStateOf(0)
    
    companion object {
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val EXTRA_SOURCE_AUDIO_PATH = "source_audio_path"
        const val EXTRA_MEDIA_STORE_ID = "media_store_id"
        const val EXTRA_LYRIC_LINES = "lyric_lines"
        const val EXTRA_TITLE = "title"
        const val EXTRA_INITIAL_POSITION = "initial_position"
        const val EXTRA_RETURN_POSITION = "return_position"
        const val EXTRA_CREATORS = "creators"
        const val EXTRA_USE_SHARED_PLAYBACK = "use_shared_playback"
        const val EXTRA_SHARED_PLAYBACK_USED = "shared_playback_used"
        const val EXTRA_PREVIEW_ENTRY_SOURCE = "preview_entry_source"
        const val EXTRA_FORCE_LYRIC_DISPLAY_OFF = "force_lyric_display_off"
        const val PREVIEW_ENTRY_SOURCE_DEFAULT = 0
        const val PREVIEW_ENTRY_SOURCE_TIMING = 1
        const val PREFS_NAME = "LyricPreviewSettings"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_SHOW_TRANSLATION = "show_translation"
        const val KEY_INTERLUDE_ANIMATION_TYPE = "interlude_animation_type"
        const val KEY_WORD_LIFT_DISTANCE_DP = "word_lift_distance_dp"
        const val KEY_FONT_WEIGHT = "font_weight"
        const val KEY_SHOW_TRANSLITERATION = "show_transliteration"
        const val KEY_LYRIC_BLUR = "lyric_blur"
        const val KEY_LYRIC_GLOW = "lyric_glow"
        const val KEY_DYNAMIC_COVER_BACKGROUND = "dynamic_cover_background"
        const val KEY_LYRICON_STATUS_BAR = "lyricon_status_bar"
        const val KEY_SCREEN_KEEP_ON = "screen_keep_on"
        const val KEY_AUTO_HIDE_PLAYBACK_CONTROLS = "auto_hide_playback_controls"
        const val KEY_LYRIC_DISPLAY_POSITION = "lyric_display_position"
        const val KEY_LYRIC_DISPLAY_MODE = "lyric_display_mode"
        const val DEFAULT_FONT_SIZE = 32f
        const val DEFAULT_SHOW_TRANSLATION = true
        const val DEFAULT_WORD_LIFT_DISTANCE_DP = 2f
        const val DEFAULT_FONT_WEIGHT = 800 // ExtraBold
        const val DEFAULT_SHOW_TRANSLITERATION = true
        const val DEFAULT_LYRIC_BLUR = true
        const val DEFAULT_LYRIC_GLOW = true
        const val DEFAULT_DYNAMIC_COVER_BACKGROUND = false
        const val DEFAULT_LYRICON_STATUS_BAR = false
        const val DEFAULT_SCREEN_KEEP_ON = true
        const val DEFAULT_AUTO_HIDE_PLAYBACK_CONTROLS = false
        const val LYRIC_DISPLAY_MODE_DEFAULT = 0
        const val LYRIC_DISPLAY_MODE_FORCE_WORD = 1
        const val LYRIC_DISPLAY_MODE_FORCE_LINE = 2
        const val DEFAULT_LYRIC_DISPLAY_MODE = LYRIC_DISPLAY_MODE_DEFAULT
        const val LYRIC_DISPLAY_POSITION_MIN = -4
        const val LYRIC_DISPLAY_POSITION_DEFAULT = -4
        const val LYRIC_DISPLAY_POSITION_MAX = 15
        const val LYRIC_DISPLAY_STEP_DP = 10
        const val DEFAULT_LYRIC_DISPLAY_POSITION = LYRIC_DISPLAY_POSITION_DEFAULT
        const val ANIMATION_TYPE_DEFAULT = 0 // circle
        const val ANIMATION_TYPE_DINOSAUR = 1 // dinosaur
        const val ANIMATION_TYPE_DOGE = 2 // doge
        const val WORD_LIFT_DISTANCE_MIN_DP = 0f
        const val WORD_LIFT_DISTANCE_MAX_DP = 5f
        private const val STATE_PLAYBACK_POSITION = "state_playback_position"
        private const val STATE_IS_PLAYING = "state_is_playing"
        private const val DEFAULT_COMPANION_FOLDER_NAME = "sing"
        private const val DEFAULT_COMPANION_EXACT_DIR = "/storage/emulated/0/Music/.sing"
        private const val CROSSFADE_DURATION_MS = 420L
        
        fun createIntent(
            context: Context,
            audioPath: String,
            lyricLines: List<NewPreviewLyricLine>,
            title: String = "歌词预览",
            initialPosition: Long = 0L,
            creators: List<String> = emptyList(),
            sourceAudioPath: String = "",
            mediaStoreId: Long = -1L,
            useSharedPlayback: Boolean = false,
            previewEntrySource: Int = PREVIEW_ENTRY_SOURCE_DEFAULT
        ): Intent {
            return Intent(context, LyricPreviewActivity::class.java).apply {
                putExtra(EXTRA_AUDIO_PATH, audioPath)
                putExtra(EXTRA_SOURCE_AUDIO_PATH, sourceAudioPath)
                putExtra(EXTRA_MEDIA_STORE_ID, mediaStoreId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_INITIAL_POSITION, initialPosition)
                putExtra(EXTRA_CREATORS, creators.toTypedArray())
                putExtra(EXTRA_USE_SHARED_PLAYBACK, useSharedPlayback)
                putExtra(EXTRA_PREVIEW_ENTRY_SOURCE, previewEntrySource)
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
            mediaStoreId: Long = -1L,
            useSharedPlayback: Boolean = false,
            previewEntrySource: Int = PREVIEW_ENTRY_SOURCE_DEFAULT,
            useBottomSlideTransition: Boolean = false
        ) {
            val intent = createIntent(
                context = context,
                audioPath = audioPath,
                lyricLines = lyricLines,
                title = title,
                initialPosition = initialPosition,
                creators = creators,
                sourceAudioPath = sourceAudioPath,
                mediaStoreId = mediaStoreId,
                useSharedPlayback = useSharedPlayback,
                previewEntrySource = previewEntrySource
            )
            context.startActivity(intent)
            if (useBottomSlideTransition && context is android.app.Activity) {
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
        
        val audioPath = intent.getStringExtra(EXTRA_AUDIO_PATH) ?: ""
        val sourceAudioPath = intent.getStringExtra(EXTRA_SOURCE_AUDIO_PATH) ?: audioPath
        val sourceMediaStoreId = intent.getLongExtra(EXTRA_MEDIA_STORE_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "歌词预览"
        val intentInitialPosition = intent.getLongExtra(EXTRA_INITIAL_POSITION, 0L)
        useSharedPlayback = intent.getBooleanExtra(EXTRA_USE_SHARED_PLAYBACK, false)
        val previewEntrySource = intent.getIntExtra(EXTRA_PREVIEW_ENTRY_SOURCE, PREVIEW_ENTRY_SOURCE_DEFAULT)
        if (intent.getBooleanExtra(EXTRA_FORCE_LYRIC_DISPLAY_OFF, false)) {
            forceLyricDisplayOffToken += 1
        }
        val isTimingPreviewEntry = previewEntrySource == PREVIEW_ENTRY_SOURCE_TIMING
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
        val initialSharedSnapshot = sharedPlaybackController?.takeIf { useSharedPlayback && it.isReady }?.also {
            it.refreshProgress()
        }
        val initialSharedPath = initialSharedSnapshot?.currentAudioPath.orEmpty()
        val initialSharedMediaStoreId = initialSharedSnapshot?.currentMediaStoreId ?: -1L
        val initialSharedTitle = initialSharedSnapshot?.currentTitle.orEmpty()

        // Shared 播放模式下：
        // - 首次进入（savedInstanceState == null）优先使用 intent 数据，保证首屏封面/背景即时可见；
        // - 重建恢复时优先使用共享播放快照，避免先闪回旧 intent 歌曲封面/背景。
        val preferSharedBootstrap = useSharedPlayback && savedInstanceState != null
        val previewAudioPathState = mutableStateOf(
            if (preferSharedBootstrap) initialSharedPath else audioPath
        )
        val previewSourceAudioPathState = mutableStateOf(
            if (preferSharedBootstrap) initialSharedPath else sourceAudioPath
        )
        val previewMediaStoreIdState = mutableLongStateOf(
            if (preferSharedBootstrap) initialSharedMediaStoreId else sourceMediaStoreId
        )
        val previewTitleState = mutableStateOf(
            if (preferSharedBootstrap) {
                initialSharedTitle.ifBlank { "歌词预览" }
            } else {
                title
            }
        )
        val previewCreatorsState = mutableStateOf(creators)
        val previewLyricLinesState = mutableStateOf(
            if (preferSharedBootstrap) emptyList() else reorganizedLines
        )
        val previewLyricsLoadingState = mutableStateOf(preferSharedBootstrap)
        val companionAudioPathState = mutableStateOf<String?>(null)
        Log.d(
            COMPANION_AUDIO_LOG_TAG,
            "onCreate source=${previewSourceAudioPathState.value} companion=${companionAudioPathState.value ?: "null"}"
        )
        val companionSwitchingState = mutableStateOf(false)

        lifecycleScope.launch(Dispatchers.IO) {
            val resolvedCompanion = resolveCompanionAudioPath(previewSourceAudioPathState.value)
            withContext(Dispatchers.Main) {
                companionAudioPathState.value = resolvedCompanion
                Log.d(
                    COMPANION_AUDIO_LOG_TAG,
                    "initial companion resolved=${resolvedCompanion ?: "null"} source=${previewSourceAudioPathState.value}"
                )
            }
        }
        
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
                            val resolvedNowMediaStoreId = controller.currentMediaStoreId
                            previewLyricsLoadingState.value = true
                            previewLyricLinesState.value = emptyList()
                            val payload = withContext(Dispatchers.IO) {
                                buildPlayerLyricPreviewPayload(
                                    context = this@LyricPreviewActivity,
                                    audioPath = resolvedNowPath,
                                    mediaStoreId = resolvedNowMediaStoreId
                                )
                            }
                            val rebuiltLines = reorganizeLyricsWithBackground(payload?.lines ?: emptyList())
                            previewAudioPathState.value = resolvedNowPath
                            previewSourceAudioPathState.value = resolvedNowPath
                            previewMediaStoreIdState.longValue = resolvedNowMediaStoreId
                            previewTitleState.value = controller.currentTitle.ifBlank {
                                File(resolvedNowPath).nameWithoutExtension
                            }
                            previewCreatorsState.value = payload?.creators ?: emptyList()
                            previewLyricLinesState.value = rebuiltLines
                            previewLyricsLoadingState.value = false
                            companionModeEnabled = false
                            val resolvedCompanion = withContext(Dispatchers.IO) {
                                resolveCompanionAudioPath(resolvedNowPath)
                            }
                            companionAudioPathState.value = resolvedCompanion
                            Log.d(
                                COMPANION_AUDIO_LOG_TAG,
                                "sharedTrackChanged source=$resolvedNowPath companion=${companionAudioPathState.value ?: "null"}"
                            )
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
                val useTrackSkipControls = useSharedPlayback && !isTimingPreviewEntry
                val resolvedCompanionSelected = if (companionSwitchingState.value) {
                    companionModeEnabled
                } else {
                    resolveCompanionSelectedState(companionAudioPathState.value)
                }
                LyricPreviewScreen(
                    title = previewTitleState.value,
                    audioPath = previewAudioPathState.value,
                    sourceAudioPath = previewSourceAudioPathState.value,
                    sourceMediaStoreId = previewMediaStoreIdState.longValue,
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
                                mediaPlayer?.play()
                                playbackCompleted = false
                            } else {
                                mediaPlayer?.pause()
                            }
                        }
                    },
                    onSkipPreviousTrack = if (useTrackSkipControls) {
                        { sharedPlaybackController?.skipToPrevious() }
                    } else {
                        null
                    },
                    onSkipNextTrack = if (useTrackSkipControls) {
                        { sharedPlaybackController?.skipToNext() }
                    } else {
                        null
                    },
                    canSkipNextTrack = if (useTrackSkipControls) {
                        val controller = sharedPlaybackController
                        controller != null &&
                            controller.isReady &&
                            controller.currentIndex >= 0 &&
                            controller.mediaCount > 0
                    } else {
                        true
                    },
                    onSeekTo = { position -> 
                        if (useSharedPlayback) {
                            sharedPlaybackController?.seekTo(position)
                        } else {
                            mediaPlayer?.seekTo(position)
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
                    companionAvailable = companionAudioPathState.value != null,
                    companionSelected = resolvedCompanionSelected,
                    companionBusy = companionSwitchingState.value,
                    playbackController = if (useTrackSkipControls) sharedPlaybackController else null,
                    lyricDisplayResetToken = forceLyricDisplayOffToken,
                    onCompanionToggle = { enabled ->
                        Log.d(
                            COMPANION_AUDIO_LOG_TAG,
                            "toggle requested enabled=$enabled selected=$companionModeEnabled busy=${companionSwitchingState.value} source=${previewSourceAudioPathState.value} companion=${companionAudioPathState.value ?: "null"}"
                        )
                        if (!companionSwitchingState.value) {
                            val sourcePathForToggle = previewSourceAudioPathState.value
                            val companionPath = companionAudioPathState.value
                            val targetPath = if (enabled) companionPath else sourcePathForToggle
                            if (!targetPath.isNullOrBlank()) {
                                companionSwitchingState.value = true
                                lifecycleScope.launch {
                                    try {
                                        val switchSucceeded = if (useSharedPlayback) {
                                            sharedPlaybackController?.switchCurrentAudioKeepingMetadata(
                                                expectedSourcePath = sourcePathForToggle,
                                                targetAudioPath = targetPath,
                                                crossfadeDurationMs = CROSSFADE_DURATION_MS
                                            ) == true
                                        } else {
                                            switchLocalPreviewAudioKeepingProgress(
                                                targetPath = targetPath,
                                                crossfadeDurationMs = CROSSFADE_DURATION_MS
                                            )
                                        }
                                        if (switchSucceeded) {
                                            companionModeEnabled = enabled
                                        }
                                        Log.d(
                                            COMPANION_AUDIO_LOG_TAG,
                                            "toggle result enabled=$enabled success=$switchSucceeded target=$targetPath"
                                        )
                                    } finally {
                                        companionSwitchingState.value = false
                                    }
                                }
                            } else {
                                Log.d(
                                    COMPANION_AUDIO_LOG_TAG,
                                    "toggle skipped: targetPath empty enabled=$enabled source=$sourcePathForToggle companion=${companionPath ?: "null"}"
                                )
                            }
                        }
                    },
                    enableSongInfoSheet = !isTimingPreviewEntry,
                    playbackCompleted = playbackCompleted,
                    onPlaybackCompletedHandled = { playbackCompleted = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        useSharedPlayback = intent.getBooleanExtra(EXTRA_USE_SHARED_PLAYBACK, useSharedPlayback)
        if (intent.getBooleanExtra(EXTRA_FORCE_LYRIC_DISPLAY_OFF, false)) {
            forceLyricDisplayOffToken += 1
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

    override fun finish() {
        super.finish()
        overridePendingTransition(
            R.anim.player_hold,
            R.anim.player_slide_down_out
        )
    }
    
    private fun loadAudio(path: String, initialPosition: Long = 0L, autoPlay: Boolean = false) {
        val targetFile = File(path)
        if (!targetFile.exists()) {
            Log.w("LyricPreview", "Audio file does not exist: $path")
            previewAudioDuration = 0L
            return
        }
        companionModeEnabled = false

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
            companionPlayer?.release()
            companionPlayer = null
            val renderersFactory = DefaultRenderersFactory(this).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            }
            mediaPlayer = ExoPlayer.Builder(this, renderersFactory).build().apply {
                attachPreviewPlayerListener(this, fallbackPath = path, allowFallback = allowFallback)
                val playableUri = resolvePlayablePreviewUri(path)
                setMediaItem(MediaItem.fromUri(playableUri))
                prepare()
                if (initialPosition > 0L) {
                    seekTo(initialPosition)
                }
                if (autoPlay) {
                    playWhenReady = true
                    play()
                }
            }
            previewAudioDuration = mediaPlayer?.duration
                ?.takeIf { it != C.TIME_UNSET && it > 0L }
                ?: 0L
            if (previewAudioDuration > 0L) {
                previewLastKnownDuration = previewAudioDuration
            }
            return true
        } catch (e: Exception) {
            Log.e("LyricPreview", "Failed to load audio: $path", e)
            mediaPlayer?.release()
            mediaPlayer = null
            previewAudioDuration = 0L
        }
        return false
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
            Log.d("LyricPreview", "Fallback already running, skip new request. reason=$reason")
            return
        }
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            Log.e("LyricPreview", "Input file missing for fallback: $inputPath")
            return
        }

        isFallbackTranscoding = true
        previewAudioDuration = 0L
        mediaPlayer?.release()
        mediaPlayer = null
        companionPlayer?.release()
        companionPlayer = null
        previewConvertedAudioPath = null
        cleanupPreviewConvertCacheFiles()
        Log.w("LyricPreview", "Transcode fallback disabled. reason=$reason input=$inputPath")
        val loaded = tryLoadAudioDirect(
            path = inputFile.absolutePath,
            initialPosition = initialPosition,
            autoPlay = autoPlay,
            allowFallback = false
        )
        if (!loaded) {
            Log.e("LyricPreview", "Direct decode retry failed after fallback trigger: $inputPath")
        }
        isFallbackTranscoding = false
    }

    private fun cleanupPreviewConvertCache() {
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

    private fun resolveCompanionAudioPath(sourcePath: String): String? {
        val sourceFile = File(sourcePath)
        val exactFileName = sourceFile.name.takeIf { it.isNotBlank() } ?: return null
        val sourceBaseName = sourceFile.nameWithoutExtension.takeIf { it.isNotBlank() } ?: return null
        val candidateDirs = linkedSetOf<File>()
        candidateDirs += File(DEFAULT_COMPANION_EXACT_DIR)
        val sourceParent = sourceFile.parentFile
        if (sourceParent != null) {
            candidateDirs += File(sourceParent, DEFAULT_COMPANION_FOLDER_NAME)
            candidateDirs += File(sourceParent, ".$DEFAULT_COMPANION_FOLDER_NAME")
            sourceParent.parentFile?.let { parent ->
                candidateDirs += File(parent, "music${File.separator}$DEFAULT_COMPANION_FOLDER_NAME")
                candidateDirs += File(parent, "Music${File.separator}$DEFAULT_COMPANION_FOLDER_NAME")
                candidateDirs += File(parent, "music${File.separator}.$DEFAULT_COMPANION_FOLDER_NAME")
                candidateDirs += File(parent, "Music${File.separator}.$DEFAULT_COMPANION_FOLDER_NAME")
            }
        }
        Log.d(
            COMPANION_AUDIO_LOG_TAG,
            "resolve source=$sourcePath exact=$exactFileName base=$sourceBaseName dirs=${candidateDirs.size}"
        )
        candidateDirs.forEach { dir ->
            Log.d(
                COMPANION_AUDIO_LOG_TAG,
                "dir=${dir.absolutePath} exists=${dir.exists()} isDirectory=${dir.isDirectory}"
            )
        }
        val resolvedFile = candidateDirs.firstNotNullOfOrNull { dir ->
            findCompanionInDir(
                dir = dir,
                exactFileName = exactFileName,
                sourceBaseName = sourceBaseName
            )
        }
        val resolved = resolvedFile?.absolutePath
        Log.d(
            COMPANION_AUDIO_LOG_TAG,
            "resolved=${resolved ?: "null"} source=$sourcePath"
        )
        return resolved
    }

    private fun findCompanionInDir(
        dir: File,
        exactFileName: String,
        sourceBaseName: String
    ): File? {
        if (!dir.exists() || !dir.isDirectory) return null

        val exactCandidate = File(dir, exactFileName)
        if (exactCandidate.exists() && exactCandidate.isFile) {
            Log.d(
                COMPANION_AUDIO_LOG_TAG,
                "match exact=${exactCandidate.absolutePath}"
            )
            return exactCandidate
        }

        val files = dir.listFiles() ?: return null
        val baseMatched = files.firstOrNull { file ->
            file.isFile && isCompanionNameMatch(file.name, sourceBaseName)
        }
        if (baseMatched != null) {
            Log.d(
                COMPANION_AUDIO_LOG_TAG,
                "match base=${baseMatched.absolutePath}"
            )
        }
        return baseMatched
    }

    private fun isCompanionNameMatch(fileName: String?, sourceBaseName: String): Boolean {
        if (fileName.isNullOrBlank() || sourceBaseName.isBlank()) return false
        val candidate = fileName.trim()
        val lowerCandidate = candidate.lowercase()
        val lowerBase = sourceBaseName.lowercase()
        if (lowerCandidate == lowerBase) return true
        if (lowerCandidate.startsWith("$lowerBase.")) return true
        val withoutLast = candidate.substringBeforeLast('.', candidate)
        if (withoutLast.equals(sourceBaseName, ignoreCase = true)) return true
        val withoutTwo = withoutLast.substringBeforeLast('.', withoutLast)
        return withoutTwo.equals(sourceBaseName, ignoreCase = true)
    }

    private fun resolveCompanionSelectedState(companionPath: String?): Boolean {
        if (companionPath.isNullOrBlank()) return false
        val currentPlaybackPath = if (useSharedPlayback) {
            sharedPlaybackController?.currentPlaybackUriPath
        } else {
            mediaPlayer?.currentMediaItem?.localConfiguration?.uri?.path
        }
        return isSameAudioPath(currentPlaybackPath, companionPath)
    }

    private fun isSameAudioPath(pathA: String?, pathB: String?): Boolean {
        if (pathA.isNullOrBlank() || pathB.isNullOrBlank()) return false
        val normalizedA = runCatching { File(pathA).absolutePath }.getOrElse { pathA }
        val normalizedB = runCatching { File(pathB).absolutePath }.getOrElse { pathB }
        return normalizedA == normalizedB
    }

    private suspend fun switchLocalPreviewAudioKeepingProgress(
        targetPath: String,
        crossfadeDurationMs: Long
    ): Boolean {
        return try {
            val targetFile = File(targetPath)
            if (!targetFile.exists() || !targetFile.isFile) return false

            val primaryPlayer = mediaPlayer ?: return false
            val currentUriPath = primaryPlayer.currentMediaItem?.localConfiguration?.uri?.path
            if (currentUriPath == targetFile.absolutePath) return true

            val resumePosition = primaryPlayer.currentPosition.coerceAtLeast(0L)
            val keepPlaying = primaryPlayer.isPlaying || primaryPlayer.playWhenReady
            val baselineVolume = primaryPlayer.volume.takeIf { it > 0f } ?: 1f
            val halfDuration = (crossfadeDurationMs / 2L).coerceAtLeast(90L)

            companionPlayer?.release()
            companionPlayer = ExoPlayer.Builder(
                this,
                DefaultRenderersFactory(this).apply {
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                }
            ).build().apply {
                attachPreviewPlayerListener(this, fallbackPath = targetPath, allowFallback = false)
                val playableUri = resolvePlayablePreviewUri(targetPath)
                setMediaItem(MediaItem.fromUri(playableUri))
                prepare()
                if (resumePosition > 0L) {
                    seekTo(resumePosition)
                }
                volume = 0f
                playWhenReady = keepPlaying
                if (keepPlaying) {
                    play()
                }
            }

            val targetPlayer = companionPlayer ?: return false

            var waitRounds = 0
            while (
                waitRounds < 12 &&
                (targetPlayer.playbackState == Player.STATE_IDLE || targetPlayer.playbackState == Player.STATE_BUFFERING)
            ) {
                delay(35L)
                waitRounds += 1
            }

            animateDualPlayerVolume(primaryPlayer, targetPlayer, baselineVolume, halfDuration)

            val oldPlayer = mediaPlayer
            mediaPlayer = targetPlayer
            companionPlayer = null
            oldPlayer?.release()
            mediaPlayer?.volume = baselineVolume
            playbackCompleted = false
            previewAudioDuration = mediaPlayer?.duration
                ?.takeIf { it != C.TIME_UNSET && it > 0L }
                ?: previewAudioDuration
            if (previewAudioDuration > 0L) {
                previewLastKnownDuration = previewAudioDuration
            } else if (previewLastKnownDuration > 0L) {
                previewAudioDuration = previewLastKnownDuration
            }
            currentPlaybackPosition = mediaPlayer?.currentPosition?.coerceAtLeast(0L) ?: currentPlaybackPosition
            true
        } catch (e: Exception) {
            Log.e("LyricPreview", "Failed to switch preview companion audio: $targetPath", e)
            companionPlayer?.release()
            companionPlayer = null
            false
        }
    }

    private fun attachPreviewPlayerListener(
        player: ExoPlayer,
        fallbackPath: String,
        allowFallback: Boolean
    ) {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(
                    "LyricPreview",
                    "ExoPlayer error: code=${error.errorCodeName} message=${error.message} path=$fallbackPath"
                )
                if (allowFallback) {
                    val resumePosition = player.currentPosition.coerceAtLeast(0L)
                    startFallbackTranscode(fallbackPath, resumePosition, autoPlay = true, reason = "exoplayer_error")
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    playbackCompleted = true
                } else if (state == Player.STATE_READY) {
                    val updatedDuration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                    if (updatedDuration != null) {
                        previewAudioDuration = updatedDuration
                        previewLastKnownDuration = updatedDuration
                    } else if (previewAudioDuration <= 0L && previewLastKnownDuration > 0L) {
                        previewAudioDuration = previewLastKnownDuration
                    }
                }
            }
        })
    }

    private fun resolvePlayablePreviewUri(path: String): android.net.Uri {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val mediaStoreUri = resolveMediaStoreAudioUri(this, path)
            if (mediaStoreUri != null) return mediaStoreUri
        }
        return android.net.Uri.fromFile(File(path))
    }

    private suspend fun animateDualPlayerVolume(
        fadeOutPlayer: Player,
        fadeInPlayer: Player,
        targetVolume: Float,
        durationMs: Long
    ) {
        val clampedTarget = targetVolume.coerceIn(0f, 1f)
        if (durationMs <= 0L) {
            fadeOutPlayer.volume = 0f
            fadeInPlayer.volume = clampedTarget
            return
        }
        val steps = 12
        val stepDelay = (durationMs / steps).coerceAtLeast(12L)
        for (step in 0..steps) {
            val fraction = step / steps.toFloat()
            fadeOutPlayer.volume = clampedTarget * (1f - fraction)
            fadeInPlayer.volume = clampedTarget * fraction
            delay(stepDelay)
        }
        fadeOutPlayer.volume = 0f
        fadeInPlayer.volume = clampedTarget
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
        companionPlayer?.release()
        companionPlayer = null
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

private fun isInterludeLineVisibleAtTime(line: NewPreviewLyricLine, timeMs: Long): Boolean {
    if (!line.isInterlude) return false
    val effectiveBegin = line.begin + 200L
    val effectiveEnd = line.end - 500L
    return timeMs >= effectiveBegin && timeMs < effectiveEnd
}

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
    return FontWeight(weight.coerceIn(100, 900))
}

private fun applyAndroidFontWeight(
    paint: android.graphics.Paint,
    fontWeight: Int,
    baseTypeface: android.graphics.Typeface? = null
) {
    val safeWeight = fontWeight.coerceIn(100, 900)
    val sourceTypeface = baseTypeface ?: android.graphics.Typeface.DEFAULT
    paint.typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        android.graphics.Typeface.create(sourceTypeface, safeWeight, false)
    } else {
        when {
            safeWeight >= 600 -> android.graphics.Typeface.create(sourceTypeface, android.graphics.Typeface.BOLD)
            else -> android.graphics.Typeface.create(sourceTypeface, android.graphics.Typeface.NORMAL)
        }
    }
    paint.isFakeBoldText = false
    paint.style = android.graphics.Paint.Style.FILL
}

private fun computeLyricLineBlurRadius(
    lyricLines: List<NewPreviewLyricLine>,
    line: NewPreviewLyricLine,
    nextLine: NewPreviewLyricLine?,
    lineIndex: Int,
    currentPlayingIndex: Int,
    currentTime: Long,
    isCurrentLineVisible: Boolean,
    isBlurEnabled: Boolean
): Float {
    if (!isBlurEnabled) return 0f
    if (line.isInterlude) return 0f
    if (lineIndex == currentPlayingIndex) return 0f
    if (!isCurrentLineVisible) return 0f

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

    val distanceFromCurrent = if (currentPlayingIndex >= 0 && lineIndex > currentPlayingIndex) {
        var distance = 0
        for (idx in (currentPlayingIndex + 1)..lineIndex) {
            val candidate = lyricLines.getOrNull(idx) ?: continue
            val candidateVisible = if (candidate.isBackground) {
                shouldShowBackgroundLine(lyricLines, idx, currentTime)
            } else {
                true
            }
            if (!candidateVisible) continue
            distance++
        }
        distance.coerceAtLeast(1)
    } else {
        1
    }

    val targetBlur = PLAYED_LINE_BLUR_RADIUS + (distanceFromCurrent - 1) * UPCOMING_LINE_BLUR_STEP
    return targetBlur.coerceIn(PLAYED_LINE_BLUR_RADIUS, UPCOMING_LINE_MAX_BLUR_RADIUS)
}

@Composable
private fun rememberLyricLineBlurModifier(blurRadius: Float): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return Modifier
    }

    val safeBlur = blurRadius.coerceIn(0f, UPCOMING_LINE_MAX_BLUR_RADIUS)
    val animatedBlur by animateFloatAsState(
        targetValue = safeBlur,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "lyricLineBlurTransition"
    )
    if (animatedBlur <= 0.01f) return Modifier

    val blurEffect = remember(animatedBlur) {
        android.graphics.RenderEffect
            .createBlurEffect(animatedBlur, animatedBlur, android.graphics.Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }

    return Modifier.graphicsLayer {
        // 与外层 springPlacement 解耦，避免位移动画过程中的模糊采样异常
        compositingStrategy = CompositingStrategy.Offscreen
        clip = false
        renderEffect = blurEffect
    }
}

private fun Modifier.lyricChromeEdgeFade(
    enabled: Boolean,
    topHiddenHeight: Dp = 0.dp,
    topFadeHeight: Dp,
    bottomHiddenHeight: Dp = 0.dp,
    bottomFadeHeight: Dp
): Modifier {
    if (!enabled) return this
    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            if (size.height <= 0f) return@drawWithContent
            val topHiddenStop = (topHiddenHeight.toPx() / size.height).coerceIn(0f, 0.48f)
            val topVisibleStop = ((topHiddenHeight + topFadeHeight).toPx() / size.height)
                .coerceIn((topHiddenStop + 0.01f).coerceAtMost(0.49f), 0.58f)
            val bottomTransparentStop = ((size.height - bottomHiddenHeight.toPx()) / size.height)
                .coerceIn(topVisibleStop, 1f)
            val bottomVisibleStopMax = (bottomTransparentStop - 0.01f).coerceAtLeast(topVisibleStop)
            val bottomVisibleStop = (
                (size.height - bottomHiddenHeight.toPx() - bottomFadeHeight.toPx()) / size.height
            ).coerceIn(topVisibleStop, bottomVisibleStopMax)
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        topHiddenStop to Color.Transparent,
                        topVisibleStop to Color.Black,
                        bottomVisibleStop to Color.Black,
                        bottomTransparentStop to Color.Transparent,
                        1f to Color.Transparent
                    )
                ),
                blendMode = BlendMode.DstIn
            )
        }
}

private fun Modifier.lyricBottomOpacityFade(
    enabled: Boolean,
    bottomAlpha: Float = 0.45f,
    fullyOpaqueFromRatio: Float = 0.5f
): Modifier {
    if (!enabled) return this
    val safeBottomAlpha = bottomAlpha.coerceIn(0f, 1f)
    val safeOpaqueStop = fullyOpaqueFromRatio.coerceIn(0f, 1f)
    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            if (size.height <= 0f) return@drawWithContent
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black,
                        safeOpaqueStop to Color.Black,
                        1f to Color.Black.copy(alpha = safeBottomAlpha)
                    )
                ),
                blendMode = BlendMode.DstIn
            )
        }
}

private fun createLyricFlowBackgroundBitmap(source: Bitmap): Bitmap {
    val maxSide = 64
    val sourceMaxSide = max(source.width, source.height).coerceAtLeast(1)
    if (sourceMaxSide <= maxSide) return source
    val scale = maxSide.toFloat() / sourceMaxSide.toFloat()
    val targetWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
}

private fun DrawScope.drawDistortedCoverMesh(
    bitmap: Bitmap,
    phase: Float,
    amplitudePx: Float,
    overscanPx: Float,
    alpha: Float,
    meshWidth: Int = 18,
    meshHeight: Int = 18
) {
    if (size.width <= 0f || size.height <= 0f || bitmap.width <= 0 || bitmap.height <= 0) return

    val vertexCount = (meshWidth + 1) * (meshHeight + 1)
    val verts = FloatArray(vertexCount * 2)
    val left = -overscanPx
    val top = -overscanPx
    val drawWidth = size.width + overscanPx * 2f
    val drawHeight = size.height + overscanPx * 2f
    val twoPi = (PI * 2.0).toFloat()
    var offset = 0

    for (row in 0..meshHeight) {
        val normalizedY = row.toFloat() / meshHeight.toFloat()
        for (column in 0..meshWidth) {
            val normalizedX = column.toFloat() / meshWidth.toFloat()
            val baseX = left + drawWidth * normalizedX
            val baseY = top + drawHeight * normalizedY
            val centerX = normalizedX - 0.5f
            val centerY = normalizedY - 0.5f
            val radius = kotlin.math.sqrt(centerX * centerX + centerY * centerY)
            val twist = sin(phase + radius * 7.2f) * amplitudePx * 0.86f
            val waveX = sin(normalizedY * twoPi * 1.55f + phase) * amplitudePx +
                cos((normalizedX + normalizedY) * twoPi * 0.95f - phase * 0.72f) * amplitudePx * 0.55f
            val waveY = cos(normalizedX * twoPi * 1.35f - phase * 1.08f) * amplitudePx * 0.82f +
                sin((normalizedX - normalizedY) * twoPi * 1.05f + phase * 0.92f) * amplitudePx * 0.48f
            verts[offset++] = baseX + waveX - centerY * twist
            verts[offset++] = baseY + waveY + centerX * twist
        }
    }

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            this.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        }
        canvas.nativeCanvas.drawBitmapMesh(
            bitmap,
            meshWidth,
            meshHeight,
            verts,
            0,
            null,
            0,
            paint
        )
    }
}

@Composable
private fun DynamicLyricCoverBackground(
    coverBitmap: Bitmap?,
    backgroundColor: Color,
    isDarkTheme: Boolean,
    enabled: Boolean
) {
    if (!enabled || coverBitmap == null) return

    var lowResCover by remember(coverBitmap) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(coverBitmap) {
        lowResCover = withContext(Dispatchers.Default) {
            createLyricFlowBackgroundBitmap(coverBitmap)
        }
    }

    val flowBitmap = lowResCover ?: coverBitmap
    val density = LocalDensity.current
    var elapsedNanos by remember(flowBitmap) { mutableLongStateOf(0L) }
    LaunchedEffect(flowBitmap) {
        val startNanos = withFrameNanos { it }
        while (true) {
            elapsedNanos = withFrameNanos { it - startNanos }
        }
    }
    val elapsedMs = elapsedNanos / 1_000_000f
    val primaryPhase = elapsedMs / 36000f * (PI * 2.0).toFloat()
    val secondaryPhase = elapsedMs / 52000f * (PI * 2.0).toFloat()
    val primaryAmplitudePx = with(density) { 122.dp.toPx() }
    val secondaryAmplitudePx = with(density) { 86.dp.toPx() }
    val overscanPx = with(density) { 260.dp.toPx() }
    val blurRadius = 100.dp
    val scrimColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.60f)
    } else {
        Color.White.copy(alpha = 0.50f)
    }
    val depthOverlay = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0.14f),
                backgroundColor.copy(alpha = 0.22f),
                Color.Black.copy(alpha = 0.28f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                backgroundColor.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.30f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) {
            drawDistortedCoverMesh(
                bitmap = flowBitmap,
                phase = primaryPhase,
                amplitudePx = primaryAmplitudePx,
                overscanPx = overscanPx,
                alpha = 0.76f,
                meshWidth = 20,
                meshHeight = 20
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) {
            drawDistortedCoverMesh(
                bitmap = flowBitmap,
                phase = secondaryPhase + 1.7f,
                amplitudePx = secondaryAmplitudePx,
                overscanPx = overscanPx * 0.9f,
                alpha = 0.36f,
                meshWidth = 16,
                meshHeight = 16
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .background(depthOverlay)
        )
    }
}

// ==================== 预览界面 ====================

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun LyricPreviewScreen(
    title: String,
    audioPath: String,
    sourceAudioPath: String,
    sourceMediaStoreId: Long = -1L,
    lyricLines: List<NewPreviewLyricLine>,
    isLyricLoading: Boolean = false,
    applyInitialSeek: Boolean = true,
    creators: List<String> = emptyList(),
    audioDuration: Long,
    initialPosition: Long = 0L,
    initialIsPlaying: Boolean = false,
    onBack: () -> Unit,
    onPlayPause: (Boolean) -> Unit,
    onSkipPreviousTrack: (() -> Unit)? = null,
    onSkipNextTrack: (() -> Unit)? = null,
    canSkipNextTrack: Boolean = true,
    onSeekTo: (Long) -> Unit,
    getCurrentPosition: () -> Long,
    getIsPlayingState: () -> Boolean,
    getAudioDuration: () -> Long,
    companionAvailable: Boolean = false,
    companionSelected: Boolean = false,
    companionBusy: Boolean = false,
    playbackController: MusicPlaybackController? = null,
    lyricDisplayResetToken: Int = 0,
    onCompanionToggle: (Boolean) -> Unit = {},
    enableSongInfoSheet: Boolean = true,
    playbackCompleted: Boolean = false,
    onPlaybackCompletedHandled: () -> Unit = {},
    showChrome: Boolean = true,
    enableBackHandler: Boolean = true
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE) }
    val appPrefs = remember { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
    val supportsLyricBlur = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    val supportsDynamicCoverBackground = supportsLyricBlur
    
    var isPlaying by remember(initialIsPlaying) { mutableStateOf(initialIsPlaying) }
    var currentTime by remember { mutableStateOf(initialPosition) }
    var dynamicDuration by remember(audioDuration) { mutableLongStateOf(audioDuration.coerceAtLeast(0L)) }
    var showTranslation by remember { 
        mutableStateOf(prefs.getBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLATION, LyricPreviewActivity.DEFAULT_SHOW_TRANSLATION)) 
    }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat(LyricPreviewActivity.KEY_FONT_SIZE, LyricPreviewActivity.DEFAULT_FONT_SIZE)) }
    var animationType by remember { mutableIntStateOf(prefs.getInt(LyricPreviewActivity.KEY_INTERLUDE_ANIMATION_TYPE, LyricPreviewActivity.ANIMATION_TYPE_DEFAULT)) }
    var wordLiftDistanceDp by remember {
        mutableFloatStateOf(
            prefs.getFloat(
                LyricPreviewActivity.KEY_WORD_LIFT_DISTANCE_DP,
                LyricPreviewActivity.DEFAULT_WORD_LIFT_DISTANCE_DP
            ).coerceIn(
                LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP,
                LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP
            )
        )
    }
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
    var lyricGlowEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                LyricPreviewActivity.KEY_LYRIC_GLOW,
                LyricPreviewActivity.DEFAULT_LYRIC_GLOW
            )
        )
    }
    var dynamicCoverBackgroundEnabled by remember {
        mutableStateOf(
            supportsDynamicCoverBackground &&
                prefs.getBoolean(
                    LyricPreviewActivity.KEY_DYNAMIC_COVER_BACKGROUND,
                    LyricPreviewActivity.DEFAULT_DYNAMIC_COVER_BACKGROUND
                )
        )
    }
    var lyriconStatusBarEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                LyricPreviewActivity.KEY_LYRICON_STATUS_BAR,
                LyricPreviewActivity.DEFAULT_LYRICON_STATUS_BAR
            )
        )
    }
    var keepScreenOnEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                LyricPreviewActivity.KEY_SCREEN_KEEP_ON,
                LyricPreviewActivity.DEFAULT_SCREEN_KEEP_ON
            )
        )
    }
    var autoHidePlaybackControlsEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                LyricPreviewActivity.KEY_AUTO_HIDE_PLAYBACK_CONTROLS,
                LyricPreviewActivity.DEFAULT_AUTO_HIDE_PLAYBACK_CONTROLS
            )
        )
    }
    var lyricDisplayPosition by remember {
        mutableIntStateOf(
            prefs.getInt(
                LyricPreviewActivity.KEY_LYRIC_DISPLAY_POSITION,
                LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_POSITION
            ).let { raw ->
                if (raw in LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN..LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX) {
                    raw
                } else {
                    LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_POSITION
                }
            }
        )
    }
    var lyricDisplayMode by remember {
        mutableIntStateOf(
            prefs.getInt(
                LyricPreviewActivity.KEY_LYRIC_DISPLAY_MODE,
                LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_MODE
            ).let { raw ->
                when (raw) {
                    LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT,
                    LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD,
                    LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_LINE -> raw
                    else -> LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_MODE
                }
            }
        )
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var playbackControlsMenuExpanded by remember { mutableStateOf(false) }
    var showLyricSettingsSheet by remember { mutableStateOf(false) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var showArtistSelectionSheet by remember { mutableStateOf(false) }
    var pendingArtistSheetAlbum by remember { mutableStateOf("") }
    var pendingArtistSheetArtists by remember { mutableStateOf(listOf<String>()) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var pendingResetLyricDisplayOnResume by remember { mutableStateOf(false) }
    var customFontOptions by remember { mutableStateOf(LyricCustomFontStore.loadOptions(context)) }
    var selectedCustomFontId by remember { mutableStateOf(LyricCustomFontStore.getSelectedFontId(context)) }
    var lyricFontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var lyricTypeface by remember { mutableStateOf<Typeface?>(null) }
    var isFontSwitchReloading by remember { mutableStateOf(false) }
    var hasFontInitialized by remember { mutableStateOf(false) }
    var metadata by remember {
        mutableStateOf(
            PreviewAudioMetadata(
                title = title,
                artist = "未知艺术家",
                album = "",
                comment = "",
                coverBitmap = null
            )
        )
    }
    var coverThemeColor by remember { mutableStateOf<Color?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(keepScreenOnEnabled, isPlaying) {
        val activity = context as? ComponentActivity
        val shouldKeepScreenOn = keepScreenOnEnabled && isPlaying
        if (activity != null) {
            if (shouldKeepScreenOn) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            if (activity != null && shouldKeepScreenOn) {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    
    // 获取深浅模式设置
    val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val settingsDarkMode = com.example.LyricBox.ui.theme.getDarkModeFromSettings(context)
    val isDarkTheme = settingsDarkMode ?: systemDarkTheme
    val screenConfig = LocalConfiguration.current
    val screenMetrics = context.resources.displayMetrics
    val screenShortEdgePx = min(screenMetrics.widthPixels, screenMetrics.heightPixels)
    val screenLongEdgePx = max(screenMetrics.widthPixels, screenMetrics.heightPixels)
    val screenMinDp = min(screenConfig.screenWidthDp, screenConfig.screenHeightDp)
    val isWatchFeatureDevice = context.packageManager.hasSystemFeature("android.hardware.type.watch")
    val isWatchUiMode = (
        context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_TYPE_MASK
        ) == android.content.res.Configuration.UI_MODE_TYPE_WATCH
    val isRoundScreen = context.resources.configuration.isScreenRound
    val isLargeScreenDevice = screenConfig.smallestScreenWidthDp >= 600 || screenConfig.screenWidthDp >= 900
    val isWatchLikeSmallScreen = (
        isWatchFeatureDevice ||
            isWatchUiMode ||
            isRoundScreen ||
            screenMinDp <= 260 ||
            (screenShortEdgePx <= 420 && screenLongEdgePx <= 520)
        )
    val isLandscape = screenConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val useSidePanelLayout = showChrome && isLandscape && !isWatchLikeSmallScreen
    val shouldHideSystemBarsForLandscapePhone = useSidePanelLayout && !isLargeScreenDevice
    val useMiniHeader = showChrome && isWatchLikeSmallScreen
    val usePortraitPlaybackLayout = showChrome && !isLandscape && !isWatchLikeSmallScreen
    var portraitLyricDisplaySelected by remember { mutableStateOf(false) }
    var portraitLyricLayoutSelected by remember { mutableStateOf(false) }
    var portraitLyricLayerVisible by remember { mutableStateOf(false) }
    var landscapeLyricDisplaySelected by remember { mutableStateOf(false) }
    val lyricDisplayLayoutMode = when {
        usePortraitPlaybackLayout -> 0
        useSidePanelLayout -> 1
        else -> 2
    }
    var lyricDisplaySelectionMemory by remember { mutableStateOf(false) }
    var previousLyricDisplayLayoutMode by remember { mutableIntStateOf(lyricDisplayLayoutMode) }
    LaunchedEffect(lyricDisplayLayoutMode) {
        if (lyricDisplayLayoutMode == previousLyricDisplayLayoutMode) return@LaunchedEffect
        val rememberedSelection = when (previousLyricDisplayLayoutMode) {
            0 -> portraitLyricDisplaySelected
            1 -> landscapeLyricDisplaySelected
            else -> lyricDisplaySelectionMemory
        }
        lyricDisplaySelectionMemory = rememberedSelection
        when (lyricDisplayLayoutMode) {
            0 -> portraitLyricDisplaySelected = rememberedSelection
            1 -> landscapeLyricDisplaySelected = rememberedSelection
        }
        previousLyricDisplayLayoutMode = lyricDisplayLayoutMode
    }
    LaunchedEffect(
        usePortraitPlaybackLayout,
        useSidePanelLayout,
        portraitLyricDisplaySelected,
        landscapeLyricDisplaySelected
    ) {
        lyricDisplaySelectionMemory = when {
            usePortraitPlaybackLayout -> portraitLyricDisplaySelected
            useSidePanelLayout -> landscapeLyricDisplaySelected
            else -> lyricDisplaySelectionMemory
        }
    }
    val lyricDisplaySelected = when {
        usePortraitPlaybackLayout -> portraitLyricDisplaySelected
        useSidePanelLayout -> landscapeLyricDisplaySelected
        else -> true
    }
    val lyricLayoutSelected = when {
        usePortraitPlaybackLayout -> portraitLyricLayoutSelected
        useSidePanelLayout -> landscapeLyricDisplaySelected
        else -> true
    }
    val lyricLayerVisible = when {
        usePortraitPlaybackLayout -> portraitLyricLayerVisible
        useSidePanelLayout -> landscapeLyricDisplaySelected
        else -> true
    }
    val portraitControlsCanAutoHide =
        usePortraitPlaybackLayout &&
            portraitLyricDisplaySelected &&
            autoHidePlaybackControlsEnabled
    val isPortraitCoverMode = usePortraitPlaybackLayout && !portraitLyricLayoutSelected
    var portraitControlsVisible by remember { mutableStateOf(true) }
    var portraitCompanionReadyVisible by remember { mutableStateOf(false) }
    var portraitControlsAutoHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var portraitControlsPanelHeightPx by remember { mutableIntStateOf(0) }
    var landscapeTemporaryControlsVisible by remember { mutableStateOf(false) }
    var landscapeTemporaryControlsJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val registerPortraitControlsInteraction: () -> Unit = {
        if (usePortraitPlaybackLayout) {
            portraitControlsAutoHideJob?.cancel()
            portraitControlsAutoHideJob = null
            portraitControlsVisible = true
        }
    }
    DisposableEffect(shouldHideSystemBarsForLandscapePhone) {
        val activity = context as? ComponentActivity
        val controller = activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
        }
        if (shouldHideSystemBarsForLandscapePhone) {
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (shouldHideSystemBarsForLandscapePhone) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val triggerLandscapeTemporaryControls: () -> Unit = {
        if (useSidePanelLayout && landscapeLyricDisplaySelected) {
            landscapeTemporaryControlsVisible = true
            landscapeTemporaryControlsJob?.cancel()
            landscapeTemporaryControlsJob = scope.launch {
                delay(3000L)
                landscapeTemporaryControlsVisible = false
                landscapeTemporaryControlsJob = null
            }
        }
    }

    LaunchedEffect(usePortraitPlaybackLayout) {
        if (!usePortraitPlaybackLayout) {
            portraitLyricLayoutSelected = false
            portraitLyricLayerVisible = true
            portraitControlsVisible = true
            portraitCompanionReadyVisible = false
            portraitControlsAutoHideJob?.cancel()
            portraitControlsAutoHideJob = null
        } else {
            portraitLyricLayoutSelected = portraitLyricDisplaySelected
            portraitLyricLayerVisible = portraitLyricDisplaySelected
            portraitControlsVisible = true
            portraitControlsAutoHideJob?.cancel()
            portraitControlsAutoHideJob = null
        }
    }
    LaunchedEffect(usePortraitPlaybackLayout, portraitLyricDisplaySelected) {
        if (!usePortraitPlaybackLayout) return@LaunchedEffect
        if (portraitLyricDisplaySelected) {
            // 进入歌词显示时，让歌词淡入和封面共享元素过渡同时启动。
            portraitLyricLayoutSelected = true
            portraitLyricLayerVisible = true
        } else {
            // 退回大封面时，让歌词淡出和封面共享元素过渡同时启动。
            portraitLyricLayerVisible = false
            portraitLyricLayoutSelected = false
        }
        portraitControlsVisible = true
        portraitControlsAutoHideJob?.cancel()
        portraitControlsAutoHideJob = null
    }
    LaunchedEffect(usePortraitPlaybackLayout, portraitLyricDisplaySelected, portraitLyricLayoutSelected) {
        if (!usePortraitPlaybackLayout || !portraitLyricDisplaySelected || !portraitLyricLayoutSelected) {
            portraitCompanionReadyVisible = false
            return@LaunchedEffect
        }
        portraitCompanionReadyVisible = false
        delay(740L)
        if (usePortraitPlaybackLayout && portraitLyricDisplaySelected && portraitLyricLayoutSelected) {
            portraitCompanionReadyVisible = true
        }
    }
    LaunchedEffect(useSidePanelLayout, landscapeLyricDisplaySelected) {
        if (useSidePanelLayout && landscapeLyricDisplaySelected) return@LaunchedEffect
        landscapeTemporaryControlsVisible = false
        landscapeTemporaryControlsJob?.cancel()
        landscapeTemporaryControlsJob = null
        if (useSidePanelLayout) {
            menuExpanded = false
        }
        playbackControlsMenuExpanded = false
    }
    val handleBackRequest: () -> Unit = {
        if (usePortraitPlaybackLayout && portraitLyricDisplaySelected) {
            portraitLyricDisplaySelected = false
            portraitControlsVisible = true
            portraitControlsAutoHideJob?.cancel()
            portraitControlsAutoHideJob = null
        } else {
            onBack()
        }
    }
    LaunchedEffect(lyricDisplayResetToken) {
        if (lyricDisplayResetToken <= 0) return@LaunchedEffect
        portraitLyricDisplaySelected = false
        landscapeLyricDisplaySelected = false
        portraitLyricLayoutSelected = false
        portraitLyricLayerVisible = false
        portraitControlsVisible = true
        portraitControlsAutoHideJob?.cancel()
        portraitControlsAutoHideJob = null
        landscapeTemporaryControlsVisible = false
        landscapeTemporaryControlsJob?.cancel()
        landscapeTemporaryControlsJob = null
    }
    // 使用 BackHandler 拦截系统返回事件
    androidx.activity.compose.BackHandler(enabled = enableBackHandler) {
        handleBackRequest()
    }
    val portraitNextTrackTitle = playbackController?.nextTrackTitle.orEmpty()
    val portraitPlaybackMode = playbackController?.playbackMode
    var showPortraitNextTrackHint by remember(portraitNextTrackTitle) { mutableStateOf(false) }
    var portraitTitleSwitchToken by remember(portraitNextTrackTitle, portraitPlaybackMode) { mutableIntStateOf(0) }
    val shouldCyclePlaybackTopHint = isPortraitCoverMode || useSidePanelLayout
    LaunchedEffect(portraitNextTrackTitle, portraitPlaybackMode) {
        showPortraitNextTrackHint = false
    }
    LaunchedEffect(portraitNextTrackTitle, portraitPlaybackMode, portraitTitleSwitchToken, shouldCyclePlaybackTopHint) {
        if (!shouldCyclePlaybackTopHint || portraitNextTrackTitle.isBlank()) return@LaunchedEffect
        while (true) {
            delay(5000)
            showPortraitNextTrackHint = !showPortraitNextTrackHint
        }
    }
    val playbackComment = metadata.comment.trim()
    val shouldShowPlaybackComment = playbackComment.isNotBlank() &&
        !playbackComment.contains("163 key", ignoreCase = true) &&
        !playbackComment.contains("163key", ignoreCase = true)
    val nowPlayingTopText = if (shouldShowPlaybackComment) {
        "正在播放：$playbackComment"
    } else {
        "正在播放"
    }
    val portraitTopTitleText = if (showPortraitNextTrackHint && portraitNextTrackTitle.isNotBlank()) {
        "下一首：$portraitNextTrackTitle"
    } else {
        nowPlayingTopText
    }
    val landscapeOuterHorizontalPadding = 16.dp
    val landscapePaneSpacing = 12.dp
    val landscapeRightPaneHorizontalPadding = 16.dp
    val sidePanelWidth = (
        (screenConfig.screenWidthDp.dp - (landscapeOuterHorizontalPadding * 2) - landscapePaneSpacing)
            .coerceAtLeast(0.dp)
    ) / 2f
    val landscapeSafeAreaInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical
    )
    val openMusicLibrarySearch: (String) -> Unit = { query ->
        val normalizedQuery = query.trim()
        if (normalizedQuery.isNotBlank()) {
            val intent = Intent(context, MusicLibraryActivity::class.java).apply {
                putExtra(MusicLibraryActivity.EXTRA_INITIAL_SEARCH_QUERY, normalizedQuery)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }
    val showArtistInfoSheet: () -> Unit = {
        val artists = extractLyricPreviewArtistsForSheet(metadata.title, metadata.artist)
        if (artists.isNotEmpty()) {
            pendingArtistSheetAlbum = metadata.album
            pendingArtistSheetArtists = artists
            showArtistSelectionSheet = true
        }
    }

    val customFontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val importResult = withContext(Dispatchers.IO) {
                LyricCustomFontStore.importFont(context, uri)
            }
            importResult.onSuccess { option ->
                customFontOptions = LyricCustomFontStore.loadOptions(context)
                selectedCustomFontId = option.id
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "字体导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(selectedCustomFontId, customFontOptions.size) {
        val withLoading = hasFontInitialized
        if (withLoading) {
            isFontSwitchReloading = true
        }
        val resolved = withContext(Dispatchers.IO) {
            LyricCustomFontStore.resolveSelectedFontFamily(context) to
                LyricCustomFontStore.resolveSelectedTypeface(context)
        }
        lyricFontFamily = resolved.first
        lyricTypeface = resolved.second
        hasFontInitialized = true
        if (withLoading) {
            delay(260L)
            isFontSwitchReloading = false
        }
    }
    
    // 加载音频元数据
    LaunchedEffect(sourceAudioPath, title, sourceMediaStoreId) {
        scope.launch {
            metadata = loadAudioMetadata(
                context = context,
                audioPath = sourceAudioPath,
                fallbackTitle = title,
                mediaStoreId = sourceMediaStoreId
            )
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
    val density = LocalDensity.current
    val lyricDisplayStepPx = with(density) { LyricPreviewActivity.LYRIC_DISPLAY_STEP_DP.dp.roundToPx() }
    val lyricTargetScrollOffset = (-100 - lyricDisplayPosition * lyricDisplayStepPx).coerceIn(-520, 160)
    
    // 时间导航器 - 修复结束时间为0的情况
    val lineNavigator = remember(processedLyricLines) {
        TimingNavigator(processedLyricLines)
    }
    
    // 用户滑动检测
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var autoScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lyricDisplayPreviewJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
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
    var isManualLyricJumping by remember { mutableStateOf(false) }
    var manualLyricJumpGuardJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var manualTapAnchorIndex by remember { mutableIntStateOf(-1) }
    var manualTapAnchorBeginMs by remember { mutableLongStateOf(0L) }
    var manualTapNextLineBeginMs by remember { mutableLongStateOf(Long.MAX_VALUE) }
    var manualTapSeekSettled by remember { mutableStateOf(false) }
    var manualTapSuppressUntilMs by remember { mutableLongStateOf(0L) }
    var hideInterludeAnimationDuringManualScroll by remember { mutableStateOf(false) }
    var previousObservedTime by remember { mutableLongStateOf(initialPosition) }
    var hidePlayedLinesDuringInitialBuild by remember(processedLyricLines, initialPosition) {
        mutableStateOf(initialPosition > 0L && processedLyricLines.isNotEmpty())
    }
    var initialBuildTargetIndex by remember(processedLyricLines, initialPosition) { mutableIntStateOf(-1) }
    var lyricsContentReady by remember(processedLyricLines, isLyricLoading) {
        mutableStateOf(processedLyricLines.isEmpty() && !isLyricLoading)
    }
    var previousVisibleInterludeIndices by remember(processedLyricLines) {
        mutableStateOf(emptySet<Int>())
    }
    var appWentBackground by remember { mutableStateOf(false) }
    var resumeRebuildRequest by remember { mutableIntStateOf(0) }
    var lyricSettingsReloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val maxCloseNextConsecutiveSkips = 2
    val autoScrollMinIntervalMs = 700L
    val lifecycleOwner = LocalLifecycleOwner.current
    fun logAutoScroll(message: String) {
        if (ENABLE_LYRIC_AUTOSCROLL_DEBUG_LOG) {
            Log.d(AUTO_SCROLL_LOG_TAG, message)
        }
    }
    fun logInterludeDiagnostic(message: String) {
        if (ENABLE_INTERLUDE_SCROLL_DIAGNOSTIC_LOG) {
            Log.d(AUTO_SCROLL_LOG_TAG, "[InterludeDiag] $message")
        }
    }

    fun schedulePortraitControlsAutoHideFromAutoScroll() {
        if (!portraitControlsCanAutoHide || !lyricLayerVisible) return
        portraitControlsAutoHideJob?.cancel()
        portraitControlsAutoHideJob = coroutineScope.launch {
            delay(3000L)
            if (portraitControlsCanAutoHide && lyricLayerVisible && !isUserScrolling) {
                portraitControlsVisible = false
            }
            portraitControlsAutoHideJob = null
        }
    }

    LaunchedEffect(usePortraitPlaybackLayout, portraitControlsCanAutoHide, lyricLayerVisible) {
        if (!usePortraitPlaybackLayout || !portraitControlsCanAutoHide || !lyricLayerVisible) {
            portraitControlsAutoHideJob?.cancel()
            portraitControlsAutoHideJob = null
            if (usePortraitPlaybackLayout) {
                portraitControlsVisible = true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    appWentBackground = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (pendingResetLyricDisplayOnResume) {
                        pendingResetLyricDisplayOnResume = false
                        portraitLyricDisplaySelected = false
                        landscapeLyricDisplaySelected = false
                        portraitLyricLayoutSelected = false
                        portraitLyricLayerVisible = false
                        portraitControlsVisible = true
                        portraitControlsAutoHideJob?.cancel()
                        portraitControlsAutoHideJob = null
                    }
                    if (appWentBackground) {
                        appWentBackground = false
                        resumeRebuildRequest += 1
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    appWentBackground = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val landscapeLeadingPlaceholderCount = if (useSidePanelLayout) 1 else 0
    fun toLyricListIndex(lyricLineIndex: Int): Int = lyricLineIndex + landscapeLeadingPlaceholderCount

    fun requestAutoScroll(targetIndex: Int, forceAnimate: Boolean = false, source: String = "auto") {
        if (targetIndex < 0 || targetIndex >= processedLyricLines.size) return
        val targetLine = processedLyricLines[targetIndex]
        if (lyricBlurPreferenceEnabled && !isLyricBlurEnabled) {
            isLyricBlurEnabled = true
            logAutoScroll("restore lyric blur at auto-scroll target=$targetIndex")
        }
        logAutoScroll(
            "request source=$source target=$targetIndex currentVisible=${lazyListState.firstVisibleItemIndex}" +
                " running=$autoScrollRunningTarget pending=$pendingAutoScrollIndex activeJob=${autoScrollJob?.isActive == true}"
        )
        if (targetLine.isInterlude) {
            logInterludeDiagnostic(
                "requestAutoScroll source=$source target=$targetIndex interlude begin=${targetLine.begin} end=${targetLine.end} " +
                    "time=$currentTime firstVisible=${lazyListState.firstVisibleItemIndex} offset=${lazyListState.firstVisibleItemScrollOffset}"
            )
        }

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
                var forceAnimateForCurrent = forceAnimate
                while (nextTarget >= 0) {
                    val now = SystemClock.elapsedRealtime()
                    val isRapidAutoScroll = (now - lastAutoScrollWallTime) < autoScrollMinIntervalMs
                    val useInstantScroll = !forceAnimateForCurrent && isRapidAutoScroll
                    logAutoScroll(
                        "execute source=$source target=$nextTarget mode=${if (useInstantScroll) "instant" else "animate"} " +
                            "deltaSinceLast=${now - lastAutoScrollWallTime}ms forceAnimate=$forceAnimateForCurrent"
                    )
                    if (useInstantScroll) {
                        lazyListState.scrollToItem(
                            index = toLyricListIndex(nextTarget),
                            scrollOffset = lyricTargetScrollOffset
                        )
                    } else {
                        lazyListState.animateScrollToItem(
                            index = toLyricListIndex(nextTarget),
                            scrollOffset = lyricTargetScrollOffset
                        )
                    }
                    lastAutoScrollWallTime = now
                    logAutoScroll("complete target=$nextTarget visible=${lazyListState.firstVisibleItemIndex}")
                    val executedLine = processedLyricLines.getOrNull(nextTarget)
                    if (executedLine?.isInterlude == true) {
                        logInterludeDiagnostic(
                            "executeComplete source=$source target=$nextTarget interlude begin=${executedLine.begin} end=${executedLine.end} " +
                                "time=$currentTime firstVisible=${lazyListState.firstVisibleItemIndex} offset=${lazyListState.firstVisibleItemScrollOffset}"
                        )
                    }
                    schedulePortraitControlsAutoHideFromAutoScroll()

                    val pending = pendingAutoScrollIndex
                    if (pending >= 0 && pending != nextTarget) {
                        pendingAutoScrollIndex = -1
                        autoScrollRunningTarget = pending
                        nextTarget = pending
                        forceAnimateForCurrent = false
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
            } finally {
                hideInterludeAnimationDuringManualScroll = false
            }
        }
    }

    suspend fun rebuildLyricPresentation(anchorPositionRaw: Long, allowSeek: Boolean) {
        if (isLyricLoading) {
            lyricsContentReady = false
            hidePlayedLinesDuringInitialBuild = false
            initialBuildTargetIndex = -1
            return
        }

        if (processedLyricLines.isEmpty()) {
            initialBuildTargetIndex = -1
            hidePlayedLinesDuringInitialBuild = false
            lyricsContentReady = true
            return
        }

        isUserScrolling = false
        scrollJob?.cancel()
        autoScrollJob?.cancel()
        pendingAutoScrollIndex = -1
        autoScrollRunningTarget = -1

        lyricsContentReady = false
        val anchorPosition = anchorPositionRaw.coerceAtLeast(0L)

        if (allowSeek && anchorPosition > 0L) {
            onSeekTo(anchorPosition)
        }

        currentTime = anchorPosition
        hidePlayedLinesDuringInitialBuild = anchorPosition > 0L
        val targetIndex = lineNavigator.findTargetIndex(anchorPosition)
        initialBuildTargetIndex = targetIndex
        if (targetIndex >= 0) {
            lazyListState.scrollToItem(index = toLyricListIndex(targetIndex), scrollOffset = lyricTargetScrollOffset)
        }
        // 重建阶段只负责把列表定位到锚点，不应把“已自动滚动索引”锁定到锚点，
        // 否则后续早于该锚点的行会被误判为回退目标而被忽略。
        lastAutoScrolledIndex = -1
        lastSkippedScrollIndex = -1
        closeNextSkipStreak = 0

        withFrameNanos { }
        withFrameNanos { }
        delay(180L)
        hidePlayedLinesDuringInitialBuild = false
        lyricsContentReady = true
    }

    val initialSeekAnchorKey = if (applyInitialSeek) initialPosition.coerceAtLeast(0L) else 0L

    // 初始化歌词列表定位：共享播放模式只跟随当前进度，不做强制 seek，避免回退与中断。
    LaunchedEffect(processedLyricLines, initialSeekAnchorKey, applyInitialSeek, isLyricLoading) {
        val anchorPosition = if (applyInitialSeek) {
            initialSeekAnchorKey
        } else {
            getCurrentPosition().coerceAtLeast(0L)
        }
        rebuildLyricPresentation(anchorPositionRaw = anchorPosition, allowSeek = applyInitialSeek)
    }

    // 应用从后台恢复时，触发一次重建并渐变显示歌词。
    LaunchedEffect(resumeRebuildRequest, isLyricLoading, processedLyricLines) {
        if (resumeRebuildRequest <= 0) return@LaunchedEffect
        if (isLyricLoading) return@LaunchedEffect
        val anchorPosition = getCurrentPosition().coerceAtLeast(0L)
        rebuildLyricPresentation(anchorPositionRaw = anchorPosition, allowSeek = false)
    }

    // 监听用户滑动交互
    LaunchedEffect(lazyListState.interactionSource) {
        lazyListState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    registerPortraitControlsInteraction()
                    isUserScrolling = true
                    scrollJob?.cancel()
                    autoScrollJob?.cancel()
                    pendingAutoScrollIndex = -1
                    autoScrollRunningTarget = -1
                    logAutoScroll("user touch started; cancel auto-scroll queue")
                }
                is DragInteraction.Start -> {
                    registerPortraitControlsInteraction()
                    isUserScrolling = true
                    if (lyricBlurPreferenceEnabled && isLyricBlurEnabled) {
                        isLyricBlurEnabled = false
                        logAutoScroll("disable lyric blur while user dragging list")
                    }
                    scrollJob?.cancel()
                    autoScrollJob?.cancel()
                    pendingAutoScrollIndex = -1
                    autoScrollRunningTarget = -1
                    logAutoScroll("user scroll started; cancel auto-scroll queue")
                }
                is PressInteraction.Release -> {
                    registerPortraitControlsInteraction()
                    scrollJob = coroutineScope.launch {
                        delay(3000)
                        isUserScrolling = false
                    }
                }
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
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
        if (isExternalBackwardSeek && !isManualLyricJumping) {
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
                        lazyListState.scrollToItem(index = toLyricListIndex(targetIndex), scrollOffset = lyricTargetScrollOffset)
                    }
                }
            }
            logAutoScroll("detected external backward seek from=$previousObservedTime to=$currentTime, reset auto-scroll state")
        } else if (isExternalBackwardSeek) {
            logAutoScroll("ignore external-backward-reset because manual lyric tap seek is active")
        }

        if (processedLyricLines.isNotEmpty()) {
            currentLineIndex = lineNavigator.findTargetIndex(currentTime)
        }
        previousObservedTime = currentTime
    }

    LaunchedEffect(lazyListState, processedLyricLines, creators, currentLineIndex, isUserScrolling, seekResetCounter) {
        if (!ENABLE_CREATOR_SYNC_TRACE) return@LaunchedEffect
        if (creators.isEmpty() || processedLyricLines.isEmpty()) return@LaunchedEffect
        var lastSnapshot = ""
        while (true) {
            val lastLyricIndex = processedLyricLines.lastIndex
            val creatorItemKey = "creator-info-line"
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val lyricItemInfo = visibleItems.firstOrNull { it.index == lastLyricIndex }
            val creatorItemInfo = visibleItems.firstOrNull { it.key == creatorItemKey }
            val lyricOffset = lyricItemInfo?.offset ?: Int.MIN_VALUE
            val creatorOffset = creatorItemInfo?.offset ?: Int.MIN_VALUE
            val offsetDelta = if (lyricItemInfo != null && creatorItemInfo != null) {
                creatorOffset - lyricOffset
            } else {
                Int.MIN_VALUE
            }
            val lyricDistance = if (currentLineIndex >= 0) kotlin.math.abs(lastLyricIndex - currentLineIndex) else 0
            val creatorDistance = lyricDistance
            val lyricStiffness = (120f - (lyricDistance * 20f)).coerceAtLeast(20f)
            val creatorStiffness = (120f - (creatorDistance * 20f)).coerceAtLeast(20f)

            val snapshot = buildString {
                append("currentLine=").append(currentLineIndex)
                append(" firstVisible=").append(lazyListState.firstVisibleItemIndex)
                append(" firstOffset=").append(lazyListState.firstVisibleItemScrollOffset)
                append(" userScrolling=").append(isUserScrolling)
                append(" reset=").append(seekResetCounter)
                append(" | lyric(index=").append(lyricItemInfo?.index ?: -1)
                append(",offset=").append(lyricOffset)
                append(",size=").append(lyricItemInfo?.size ?: Int.MIN_VALUE)
                append(")")
                append(" creator(index=").append(creatorItemInfo?.index ?: -1)
                append(",offset=").append(creatorOffset)
                append(",size=").append(creatorItemInfo?.size ?: Int.MIN_VALUE)
                append(")")
                append(" delta=").append(offsetDelta)
                append(" stiffness(lyric=").append(lyricStiffness)
                append(",creator=").append(creatorStiffness).append(")")
            }
            if (snapshot != lastSnapshot) {
                lastSnapshot = snapshot
                Log.d(CREATOR_VS_LYRIC_LOG_TAG, snapshot)
            }
            delay(80L)
        }
    }

    fun applyLyricSettingWithReload(applyChange: () -> Unit) {
        lyricSettingsReloadJob?.cancel()
        lyricSettingsReloadJob = coroutineScope.launch {
            lyricsContentReady = false
            withFrameNanos { }
            applyChange()
            val anchorPosition = getCurrentPosition().coerceAtLeast(0L)
            rebuildLyricPresentation(anchorPositionRaw = anchorPosition, allowSeek = false)
        }
    }

    // 保存字体大小设置
    fun saveFontSize(size: Float) {
        applyLyricSettingWithReload {
            fontSize = size
            prefs.edit().putFloat(LyricPreviewActivity.KEY_FONT_SIZE, size).apply()
        }
    }
    
    // 保存翻译显示设置
    fun saveShowTranslation(show: Boolean) {
        applyLyricSettingWithReload {
            showTranslation = show
            prefs.edit().putBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLATION, show).apply()
        }
    }
    
    // 保存字体粗细设置
    fun saveFontWeight(weight: Int) {
        fontWeight = weight
        prefs.edit().putInt(LyricPreviewActivity.KEY_FONT_WEIGHT, weight).apply()
    }

    fun saveWordLiftDistanceDp(distanceDp: Float) {
        val normalized = distanceDp.coerceIn(
            LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP,
            LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP
        )
        wordLiftDistanceDp = normalized
        prefs.edit().putFloat(LyricPreviewActivity.KEY_WORD_LIFT_DISTANCE_DP, normalized).apply()
    }
    
    // 保存注音显示设置
    fun saveShowTransliteration(show: Boolean) {
        applyLyricSettingWithReload {
            showTransliteration = show
            prefs.edit().putBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLITERATION, show).apply()
        }
    }

    fun saveLyricBlurEnabled(enabled: Boolean) {
        lyricBlurPreferenceEnabled = enabled
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_LYRIC_BLUR, enabled).apply()
        isLyricBlurEnabled = enabled
    }

    fun saveLyricGlowEnabled(enabled: Boolean) {
        lyricGlowEnabled = enabled
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_LYRIC_GLOW, enabled).apply()
    }

    fun saveDynamicCoverBackgroundEnabled(enabled: Boolean) {
        val normalized = enabled && supportsDynamicCoverBackground
        dynamicCoverBackgroundEnabled = normalized
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_DYNAMIC_COVER_BACKGROUND, normalized).apply()
    }

    fun saveLyriconStatusBarEnabled(enabled: Boolean) {
        lyriconStatusBarEnabled = enabled
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_LYRICON_STATUS_BAR, enabled).apply()
    }

    fun saveKeepScreenOnEnabled(enabled: Boolean) {
        keepScreenOnEnabled = enabled
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_SCREEN_KEEP_ON, enabled).apply()
    }

    fun saveAutoHidePlaybackControlsEnabled(enabled: Boolean) {
        autoHidePlaybackControlsEnabled = enabled
        prefs.edit().putBoolean(LyricPreviewActivity.KEY_AUTO_HIDE_PLAYBACK_CONTROLS, enabled).apply()
    }

    fun saveLyricDisplayPosition(position: Int) {
        val normalized = position.coerceIn(
            LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN,
            LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX
        )
        lyricDisplayPosition = normalized
        prefs.edit().putInt(LyricPreviewActivity.KEY_LYRIC_DISPLAY_POSITION, normalized).apply()
        if (processedLyricLines.isEmpty() || isLyricLoading || !lyricsContentReady) return
        val anchorIndex = when {
            manualTapAnchorIndex >= 0 -> manualTapAnchorIndex
            currentLineIndex >= 0 -> currentLineIndex
            else -> lineNavigator.findTargetIndex(currentTime.coerceAtLeast(0L))
        }
        if (anchorIndex < 0) return
        val previewOffset = (-100 - normalized * lyricDisplayStepPx).coerceIn(-520, 160)
        lyricDisplayPreviewJob?.cancel()
        lyricDisplayPreviewJob = coroutineScope.launch {
            autoScrollJob?.cancel()
            pendingAutoScrollIndex = -1
            autoScrollRunningTarget = -1
            lazyListState.scrollToItem(
                index = toLyricListIndex(anchorIndex),
                scrollOffset = previewOffset
            )
        }
    }

    fun saveLyricDisplayMode(mode: Int) {
        val normalized = when (mode) {
            LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT,
            LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD,
            LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_LINE -> mode
            else -> LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_MODE
        }
        applyLyricSettingWithReload {
            lyricDisplayMode = normalized
            prefs.edit().putInt(LyricPreviewActivity.KEY_LYRIC_DISPLAY_MODE, normalized).apply()
        }
    }
    
    // 更新当前时间
    val playbackTickerDelayMs = if (showChrome) 16L else 48L
    LaunchedEffect(playbackTickerDelayMs) {
        while (true) {
            currentTime = getCurrentPosition()
            isPlaying = getIsPlayingState()
            dynamicDuration = getAudioDuration().coerceAtLeast(0L)
            delay(playbackTickerDelayMs)
        }
    }

    val autoScrollLeadMs = 400L
    val autoScrollConflictWindowMs = 1000L
    val densityForInterludeCompensation = LocalDensity.current
    val interludePlaceholderCompensationPx = with(densityForInterludeCompensation) {
        ((fontSize.sp.toDp() * 2) + 8.dp).toPx()
    }

    LaunchedEffect(currentTime, lyricsContentReady, processedLyricLines, fontSize, isUserScrolling) {
        if (!lyricsContentReady || processedLyricLines.isEmpty()) {
            previousVisibleInterludeIndices = emptySet()
            return@LaunchedEffect
        }
        val currentVisibleInterlude = buildSet {
            processedLyricLines.forEachIndexed { index, line ->
                if (!line.isInterlude) return@forEachIndexed
                val baseVisible = if (line.begin <= 0L) {
                    currentTime >= 0L && currentTime < line.end
                } else {
                    currentTime >= line.begin && currentTime < line.end
                }
                val debugHoldVisible =
                    DEBUG_KEEP_INTERLUDE_PLACEHOLDER_AFTER_END &&
                        currentTime >= line.end &&
                        currentTime < (line.end + DEBUG_INTERLUDE_PLACEHOLDER_HOLD_MS)
                if (baseVisible || debugHoldVisible) {
                    add(index)
                }
            }
        }
        val endedInterludeIndices = previousVisibleInterludeIndices - currentVisibleInterlude
        if (endedInterludeIndices.isNotEmpty()) {
            val firstVisibleIndex = lazyListState.firstVisibleItemIndex - landscapeLeadingPlaceholderCount
            val endedBeforeOrAtViewport = endedInterludeIndices.count { it <= firstVisibleIndex }
            if (!isUserScrolling && endedBeforeOrAtViewport > 0 && interludePlaceholderCompensationPx > 0f) {
                // 间奏占位收起会把后续歌词整体“向上顶”，这里反向补偿把视图拉回去
                val compensation = -interludePlaceholderCompensationPx * endedBeforeOrAtViewport
                val beforeIndex = lazyListState.firstVisibleItemIndex
                val beforeOffset = lazyListState.firstVisibleItemScrollOffset
                lazyListState.scrollBy(compensation)
                val afterIndex = lazyListState.firstVisibleItemIndex
                val afterOffset = lazyListState.firstVisibleItemScrollOffset
                logInterludeDiagnostic(
                    "applyCollapseCompensation ended=${endedInterludeIndices.sorted()} " +
                        "firstVisible=$firstVisibleIndex count=$endedBeforeOrAtViewport px=$compensation " +
                        "before=($beforeIndex,$beforeOffset) after=($afterIndex,$afterOffset)"
                )
            } else {
                logInterludeDiagnostic(
                    "skipCollapseCompensation ended=${endedInterludeIndices.sorted()} " +
                        "firstVisible=$firstVisibleIndex userScrolling=$isUserScrolling count=$endedBeforeOrAtViewport"
                )
            }
        }
        previousVisibleInterludeIndices = currentVisibleInterlude
    }

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

    fun findNextAutoScrollEligibleLineBegin(anchorIndex: Int): Long {
        if (anchorIndex < 0 || anchorIndex >= processedLyricLines.lastIndex) return Long.MAX_VALUE
        for (index in (anchorIndex + 1) until processedLyricLines.size) {
            val line = processedLyricLines[index]
            if (line.isInterlude) continue
            if (line.isBackground && line.backgroundPlacement >= 0) continue
            return line.begin
        }
        return Long.MAX_VALUE
    }

    fun getMainLineOriginalBegin(line: NewPreviewLyricLine): Long {
        return line.words.firstOrNull()?.begin ?: line.begin
    }

    fun getMainLineOriginalEnd(line: NewPreviewLyricLine, fallbackNext: NewPreviewLyricLine?): Long {
        val lastWordEnd = line.words.lastOrNull()?.end ?: 0L
        if (lastWordEnd > 0L) return lastWordEnd
        return getEffectiveEndTime(line, fallbackNext)
    }

    // 自动滚动到当前播放行（屏幕1/4位置）
    LaunchedEffect(
        currentTime,
        isUserScrolling,
        isManualLyricJumping,
        lyricsContentReady,
        manualTapAnchorIndex,
        manualTapAnchorBeginMs,
        manualTapNextLineBeginMs,
        manualTapSeekSettled,
        manualTapSuppressUntilMs
    ) {
        if (!isUserScrolling && !isManualLyricJumping && lyricsContentReady && processedLyricLines.isNotEmpty()) {
            val nowWallTime = SystemClock.elapsedRealtime()
            if (manualTapAnchorIndex >= 0) {
                val seekDelta = currentTime - manualTapAnchorBeginMs
                if (!manualTapSeekSettled && seekDelta in -220L..2200L) {
                    manualTapSeekSettled = true
                    logAutoScroll("manual tap seek settled anchor=$manualTapAnchorIndex at=$currentTime delta=${seekDelta}ms")
                }
                val hasNextLine = manualTapNextLineBeginMs != Long.MAX_VALUE
                val nextStarted = manualTapSeekSettled && hasNextLine && currentTime >= manualTapNextLineBeginMs
                if (!nextStarted && nowWallTime < manualTapSuppressUntilMs) {
                    logAutoScroll(
                        "manual tap hold index=$manualTapAnchorIndex wait=${manualTapSuppressUntilMs - nowWallTime}ms " +
                            "settled=$manualTapSeekSettled current=$currentTime nextBegin=$manualTapNextLineBeginMs"
                    )
                    return@LaunchedEffect
                }
                if (!nextStarted && nowWallTime >= manualTapSuppressUntilMs) {
                    // 2 秒内下一句未开始，允许当前句触发一次自动滚动
                    if (lastAutoScrolledIndex == manualTapAnchorIndex) {
                        lastAutoScrolledIndex = -1
                    }
                    logAutoScroll("manual tap timeout -> allow current line auto-scroll index=$manualTapAnchorIndex")
                    manualTapAnchorIndex = -1
                    manualTapAnchorBeginMs = 0L
                    manualTapNextLineBeginMs = Long.MAX_VALUE
                    manualTapSeekSettled = false
                    manualTapSuppressUntilMs = 0L
                } else if (nextStarted) {
                    logAutoScroll("manual tap released by next line start anchor=$manualTapAnchorIndex current=$currentTime")
                    manualTapAnchorIndex = -1
                    manualTapAnchorBeginMs = 0L
                    manualTapNextLineBeginMs = Long.MAX_VALUE
                    manualTapSeekSettled = false
                    manualTapSuppressUntilMs = 0L
                }
            }

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
                
                // 情况一：间奏行在“开始时”触发一次；背景歌词仅上方背景可触发，下方背景不触发
                if (targetLine.isInterlude) {
                    shouldScroll = currentTime >= targetLine.begin
                } else if (targetLine.isBackground) {
                    val isUpperBackground = targetLine.backgroundPlacement < 0
                    if (!isUpperBackground) {
                        shouldScroll = false
                    }
                }
                
                // 情况二：上一句主句歌词结束时间减去当前行开始时间差大于1.55秒 → 不触发
                // 注意：若主句存在背景歌词，仍按主句原始时间（words）判断，不按背景延长后的时间判断
                var hasLargeTimeDiff = false
                if (shouldScroll && !targetLine.isInterlude && currentLineIndex > 0) {
                    // 找到上一句主句歌词（跳过背景歌词）
                    var prevMainLineIndex = currentLineIndex - 1
                    while (prevMainLineIndex >= 0 && (processedLyricLines[prevMainLineIndex].isBackground || processedLyricLines[prevMainLineIndex].isInterlude)) {
                        prevMainLineIndex--
                    }
                    
                    if (prevMainLineIndex >= 0) {
                        val previousLine = processedLyricLines[prevMainLineIndex]
                        val prevNextLine = if (prevMainLineIndex < processedLyricLines.size - 1) processedLyricLines[prevMainLineIndex + 1] else null
                        val prevEndTime = getMainLineOriginalEnd(previousLine, prevNextLine)
                        val currentStartTime = getMainLineOriginalBegin(targetLine)
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
                if (shouldScroll && !targetLine.isInterlude && shouldSkipAutoScrollDueToCloseNextTrigger(currentLineIndex, currentTime)) {
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
                    requestAutoScroll(currentLineIndex, source = "auto-main")
                } else {
                    if (targetLine.isInterlude) {
                        // 间奏行不参与补滚动，否则会在间奏淡出后额外触发一次上滚
                        lastSkippedScrollIndex = -1
                        closeNextSkipStreak = 0
                        logAutoScroll("drop current=$currentLineIndex because interlude line should never enqueue补滚动")
                    } else if (skipBecauseCloseNextTrigger) {
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
                val skippedLine = processedLyricLines.getOrNull(skippedLineIndex)
                if (skippedLine?.isInterlude == true) {
                    // 防御：历史状态里残留的间奏索引直接丢弃
                    lastSkippedScrollIndex = -1
                    logAutoScroll("drop stale interlude skipped index=$skippedLineIndex")
                    return@LaunchedEffect
                }
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
                            requestAutoScroll(skippedLineIndex, source = "auto-recovery")
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
    val useDynamicCoverChrome = supportsDynamicCoverBackground && dynamicCoverBackgroundEnabled && metadata.coverBitmap != null
    val chromeContainerColor = if (useDynamicCoverChrome) Color.Transparent else backgroundColor
    val edgeFadeTopBrush = Brush.verticalGradient(
        colors = if (useDynamicCoverChrome) {
            listOf(Color.Transparent, Color.Transparent)
        } else {
            listOf(backgroundColor, Color.Transparent)
        }
    )
    val edgeFadeBottomBrush = Brush.verticalGradient(
        colors = if (useDynamicCoverChrome) {
            listOf(Color.Transparent, Color.Transparent)
        } else {
            listOf(Color.Transparent, backgroundColor)
        }
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        DynamicLyricCoverBackground(
            coverBitmap = metadata.coverBitmap,
            backgroundColor = backgroundColor,
            isDarkTheme = isDarkTheme,
            enabled = useDynamicCoverChrome
        )
        
        // 歌词区域放在顶层
        AnimatedVisibility(
            visible = lyricLayerVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(durationMillis = 260)),
            exit = fadeOut(animationSpec = tween(durationMillis = 220))
        ) {
            androidx.compose.ui.layout.LookaheadScope {
                Box(modifier = Modifier
                .fillMaxSize()
                .then(
                    if (useSidePanelLayout) {
                        Modifier.windowInsetsPadding(landscapeSafeAreaInsets)
                    } else {
                        Modifier
                    }
                )
                .padding(
                    start = if (useSidePanelLayout) {
                        landscapeOuterHorizontalPadding +
                            sidePanelWidth +
                            landscapePaneSpacing +
                            landscapeRightPaneHorizontalPadding
                    } else {
                        16.dp
                    },
                    end = if (useSidePanelLayout) {
                        landscapeRightPaneHorizontalPadding
                    } else {
                        16.dp
                    }
                )
            ) {
                val showLoadingOverlay = isLyricLoading || !lyricsContentReady || isFontSwitchReloading
                val lyricContentAlpha by animateFloatAsState(
                    targetValue = if (showLoadingOverlay) 0f else 1f,
                    animationSpec = if (showLoadingOverlay) snap() else tween(durationMillis = 320),
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
                    val extraTopPaddingForDisplayPosition = (
                        (lyricDisplayPosition - LyricPreviewActivity.LYRIC_DISPLAY_POSITION_DEFAULT)
                            .coerceAtLeast(0) * LyricPreviewActivity.LYRIC_DISPLAY_STEP_DP
                    ).dp
                    val topPadding = if (showChrome && !useSidePanelLayout && !useMiniHeader) {
                        screenHeight * 0.30f + extraTopPaddingForDisplayPosition
                    } else if (useMiniHeader) {
                        36.dp
                    } else {
                        48.dp
                    }
                    val bottomPadding = if (showChrome && !useSidePanelLayout && !useMiniHeader) {
                        screenHeight * 0.6f
                    } else if (useMiniHeader) {
                        84.dp
                    } else {
                        96.dp
                    }
                    val landscapeTopPlaceholder = if (useSidePanelLayout) {
                        screenHeight * 0.12f
                    } else {
                        0.dp
                    }
                    val landscapeBottomPlaceholder = if (useSidePanelLayout) {
                        screenHeight * 0.36f
                    } else {
                        0.dp
                    }
                    val hasAnyDuetLine = remember(processedLyricLines) {
                        processedLyricLines.any { !it.isInterlude && it.isDuet }
                    }
                    val lyricTopChromeHidden = when {
                        useSidePanelLayout -> 0.dp
                        useMiniHeader -> 52.dp
                        else -> 150.dp
                    }
                    val lyricTopChromeFade = when {
                        useSidePanelLayout -> 86.dp
                        useMiniHeader -> 58.dp
                        else -> 86.dp
                    }
                    val lyricBottomChromeHidden = when {
                        useSidePanelLayout -> 0.dp
                        useMiniHeader -> 150.dp
                        usePortraitPlaybackLayout && !portraitControlsVisible -> 0.dp
                        else -> 190.dp
                    }
                    val lyricBottomChromeFade = when {
                        useSidePanelLayout -> 150.dp
                        useMiniHeader -> 72.dp
                        usePortraitPlaybackLayout && !portraitControlsVisible -> 72.dp
                        else -> 86.dp
                    }
                    
                    // 使用较小的 keepAlive 区域来确保弹簧动画工作，同时不会占用过多空间
                    val keepAlivePadding = 100.dp
                    val density = LocalDensity.current
                    var lastLyricRowHeightPx by remember { mutableIntStateOf(0) }
                    
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
                                .lyricChromeEdgeFade(
                                    enabled = showChrome,
                                    topHiddenHeight = lyricTopChromeHidden,
                                    topFadeHeight = lyricTopChromeFade,
                                    bottomHiddenHeight = lyricBottomChromeHidden,
                                    bottomFadeHeight = lyricBottomChromeFade
                                )
                                .lyricBottomOpacityFade(
                                    enabled = usePortraitPlaybackLayout && portraitLyricDisplaySelected,
                                    bottomAlpha = 0.45f,
                                    fullyOpaqueFromRatio = 0.5f
                                )
                                .then(extendedViewportModifier), // 延长视口高度，让更多歌词行保持活跃
                            contentPadding = PaddingValues(
                                top = topPadding + keepAlivePadding,
                                bottom = bottomPadding + keepAlivePadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (useSidePanelLayout) {
                                item(key = "landscape_top_placeholder") {
                                    Spacer(modifier = Modifier.height(landscapeTopPlaceholder))
                                }
                            }
                            itemsIndexed(processedLyricLines) { index, line ->
                            val nextLine = if (index < processedLyricLines.size - 1) processedLyricLines[index + 1] else null
                            // 判断背景歌词是否应该显示（用于渲染和模糊距离计算）
                            val shouldShowBackground = if (line.isBackground) {
                                shouldShowBackgroundLine(processedLyricLines, index, currentTime)
                            } else {
                                true
                            }
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
                                isCurrentLineVisible = shouldShowBackground,
                                isBlurEnabled = lyricBlurPreferenceEnabled && isLyricBlurEnabled
                            )
                            
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

                            val lyricLineContainerModifier = if (index == processedLyricLines.lastIndex) {
                                Modifier
                                    .alpha(lineAlpha)
                                    .onGloballyPositioned { coordinates ->
                                        val h = coordinates.size.height
                                        if (h > 0 && h != lastLyricRowHeightPx) {
                                            lastLyricRowHeightPx = h
                                        }
                                    }
                            } else {
                                Modifier.alpha(lineAlpha)
                            }
                            Box(modifier = lyricLineContainerModifier) {
                                LyricLineView(
                                    line = line,
                                    nextLine = nextLine,
                                    lineIndex = index,
                                    currentPlayingIndex = currentLineIndex,
                                    currentTime = currentTime,
                                    showTranslation = showTranslation,
                                    isDarkTheme = isDarkTheme,
                                    fontSize = fontSize.sp,
                                    songDuration = dynamicDuration,
                                    fontWeight = fontWeight, // 新增
                                    showTransliteration = showTransliteration, // 新增
                                    fontFamily = lyricFontFamily,
                                    customTypeface = lyricTypeface,
                                    lyricDisplayMode = lyricDisplayMode,
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
                                    limitWidthForDuetLayout = hasAnyDuetLine,
                                    isPlaying = isPlaying, // 新增
                                    animationType = animationType, // 新增
                                    wordLiftDistanceDp = wordLiftDistanceDp,
                                    lyricGlowEnabled = lyricGlowEnabled,
                                    blurRadius = lyricLineBlurRadius,
                                    hideInterludeAnimation = hideInterludeAnimationDuringManualScroll,
                                    onClick = {
                                        val tapTargetTime = line.begin
                                        if (!isPlaying) {
                                            isPlaying = true
                                            onPlayPause(true)
                                        }
                                        manualTapAnchorIndex = -1
                                        manualTapAnchorBeginMs = 0L
                                        manualTapNextLineBeginMs = Long.MAX_VALUE
                                        manualTapSeekSettled = false
                                        manualTapSuppressUntilMs = 0L
                                        isManualLyricJumping = true
                                        manualLyricJumpGuardJob?.cancel()
                                        manualLyricJumpGuardJob = coroutineScope.launch {
                                            // 轻微保护，避免 seek 回调与当前帧竞争导致一次错误回退判断
                                            delay(260L)
                                            isManualLyricJumping = false
                                        }
                                        isUserScrolling = false
                                        scrollJob?.cancel()
                                        // 先seek稳定间奏占位可见性，再滚动，避免目标定位偏高
                                        onSeekTo(tapTargetTime)
                                        currentTime = tapTargetTime
                                        hideInterludeAnimationDuringManualScroll = processedLyricLines.any { interlude ->
                                            isInterludeLineVisibleAtTime(interlude, currentTime)
                                        }
                                        // 点击歌词后触发自动滚动，并将当前行滚动到目标位置
                                        lastAutoScrolledIndex = index
                                        lastSkippedScrollIndex = -1
                                        closeNextSkipStreak = 0
                                        coroutineScope.launch {
                                            withFrameNanos { }
                                            requestAutoScroll(index, forceAnimate = true, source = "manual-tap")
                                        }
                                    }
                                )
                            }
                        }
                            if (creators.isNotEmpty()) {
                                val creatorRenderIndex = processedLyricLines.size
                                val lastLyricIndex = processedLyricLines.lastIndex
                                val creatorDistance = if (currentLineIndex >= 0) {
                                    kotlin.math.abs(lastLyricIndex - currentLineIndex)
                                } else {
                                    0
                                }
                                val creatorStiffness = (120f - (creatorDistance * 20f)).coerceAtLeast(20f)
                                item(key = "creator-info-line") {
                                    val creatorFontSize = maxOf(18f, fontSize - 8f).sp
                                    val creatorVirtualLine = NewPreviewLyricLine(
                                        words = listOf(
                                            NewPreviewLyricWord(
                                                text = buildString {
                                                    append("创作者：")
                                                    append(creators.joinToString("、"))
                                                },
                                                begin = Long.MAX_VALUE / 4,
                                                end = Long.MAX_VALUE / 4 + 1
                                            )
                                        ),
                                        translation = "",
                                        isDuet = false,
                                        isBackground = false,
                                        isInterlude = false,
                                        backgroundPlacement = 0
                                    )
                                    val creatorMinHeightDp = if (lastLyricRowHeightPx > 0) {
                                        with(density) { lastLyricRowHeightPx.toDp() }
                                    } else {
                                        0.dp
                                    }
                                    Box(
                                        modifier = if (creatorMinHeightDp > 0.dp) {
                                            Modifier.heightIn(min = creatorMinHeightDp)
                                        } else {
                                            Modifier
                                        }
                                    ) {
                                        LyricLineView(
                                            line = creatorVirtualLine,
                                            nextLine = null,
                                            lineIndex = creatorRenderIndex,
                                            currentPlayingIndex = currentLineIndex,
                                            currentTime = currentTime,
                                            showTranslation = false,
                                            isDarkTheme = isDarkTheme,
                                            fontSize = creatorFontSize,
                                            songDuration = dynamicDuration,
                                            fontWeight = fontWeight,
                                            showTransliteration = false,
                                            fontFamily = lyricFontFamily,
                                            customTypeface = lyricTypeface,
                                            lyricDisplayMode = LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT,
                                            lookaheadScope = this@LookaheadScope,
                                            itemKey = "creator-info-line",
                                            isManualScrolling = isUserScrolling,
                                            stiffness = creatorStiffness,
                                            forceReset = seekResetCounter,
                                            shouldShowBackgroundLine = true,
                                            isAboveMain = null,
                                            backgroundColor = backgroundColor,
                                            themeAccentColor = accentColor,
                                            effectiveIsDuet = false,
                                            nextLineIsDuet = false,
                                            limitWidthForDuetLayout = false,
                                            isPlaying = isPlaying,
                                            animationType = animationType,
                                            blurRadius = 0f,
                                            lyricGlowEnabled = lyricGlowEnabled,
                                            clickableEnabled = false,
                                            overrideInactiveColor = creatorLyricColor,
                                            onClick = {}
                                        )
                                    }
                                }
                            }
                            if (useSidePanelLayout) {
                                item(key = "landscape_bottom_placeholder") {
                                    Spacer(modifier = Modifier.height(landscapeBottomPlaceholder))
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
                        LoadingIndicator(
                            modifier = Modifier.size(56.dp),
                            color = accentColor
                        )
                    }
                }
            }
        }
        }
        
        if (showChrome) {
            val lyricSettingsMenuContent: @Composable (MenuAnchorPosition?) -> Unit = { menuButtonPosition ->
                CustomDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = listOf(
                        MenuItem(
                            title = "歌词设置",
                            onClick = {
                                menuExpanded = false
                                showLyricSettingsSheet = true
                            }
                        )
                    ),
                    anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f),
                    containerColor = menuSurfaceColor,
                    contentColor = menuContentColor,
                    pressColor = menuPressedColor,
                    borderColor = menuBorderColor
                )
            }
            val playbackControlsMenuContent: @Composable (MenuAnchorPosition?) -> Unit = { menuButtonPosition ->
                val items = buildList {
                    if (enableSongInfoSheet) {
                        add(
                            MenuItem(
                                title = "歌曲信息",
                                onClick = {
                                    playbackControlsMenuExpanded = false
                                    showSongInfoSheet = true
                                }
                            )
                        )
                    }
                    add(
                        MenuItem(
                            title = "歌词设置",
                            onClick = {
                                playbackControlsMenuExpanded = false
                                showLyricSettingsSheet = true
                            }
                        )
                    )
                }
                CustomDropdownMenu(
                    expanded = playbackControlsMenuExpanded,
                    onDismissRequest = { playbackControlsMenuExpanded = false },
                    items = items,
                    anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f),
                    containerColor = menuSurfaceColor,
                    contentColor = menuContentColor,
                    pressColor = menuPressedColor,
                    borderColor = menuBorderColor
                )
            }

            val playbackControlsPanel: @Composable (SharedTransitionScope?, Modifier, Boolean, Boolean) -> Unit = {
                    sharedUiTransitionScope,
                    panelModifier,
                    fillPanelHeight,
                    showTrackInfoInPanel ->
                Column(
                    modifier = panelModifier
                        .fillMaxWidth()
                        .padding(horizontal = if (useSidePanelLayout) 0.dp else 18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            registerPortraitControlsInteraction()
                        }
                ) {
                    PlaybackControls(
                        currentTime = currentTime,
                        duration = dynamicDuration,
                        isPlaying = isPlaying,
                        isDarkTheme = isDarkTheme,
                        seekTimeMs = seekTimeMs,
                        seekTimeSeconds = seekTimeSeconds,
                        onPlayPauseClick = {
                            registerPortraitControlsInteraction()
                            onPlayPause(!isPlaying)
                        },
                        onSkipPreviousClick = onSkipPreviousTrack,
                        onSkipNextClick = onSkipNextTrack,
                        isSkipNextEnabled = if (onSkipNextTrack == null) true else canSkipNextTrack,
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
                                    lazyListState.scrollToItem(index = toLyricListIndex(targetIndex), scrollOffset = lyricTargetScrollOffset)
                                }
                            }
                        },
                        vibrantColor = coverThemeColor ?: accentColor,
                        backgroundColor = backgroundColor,
                        containerColor = chromeContainerColor,
                        trackTitle = metadata.title,
                        trackArtist = metadata.artist,
                        showTrackInfo = showTrackInfoInPanel,
                        onTrackInfoClick = if (showTrackInfoInPanel) {
                            { showArtistInfoSheet() }
                        } else {
                            null
                        },
                        onTrackInfoMenuClick = if (showTrackInfoInPanel) {
                            {
                                if (useSidePanelLayout && !landscapeLyricDisplaySelected) {
                                    if (enableSongInfoSheet) {
                                        showSongInfoSheet = true
                                    }
                                } else {
                                    playbackControlsMenuExpanded = true
                                }
                            }
                        } else {
                            null
                        },
                        trackInfoMenuContent = playbackControlsMenuContent,
                        playbackMode = playbackController?.playbackMode,
                        onPlaybackModeClick = playbackController?.let { controller ->
                            { controller.cyclePlaybackMode() }
                        },
                        onLyricDisplayClick = {
                            registerPortraitControlsInteraction()
                            if (usePortraitPlaybackLayout) {
                                portraitLyricDisplaySelected = !portraitLyricDisplaySelected
                            } else if (useSidePanelLayout) {
                                landscapeLyricDisplaySelected = !landscapeLyricDisplaySelected
                            } else {
                                showLyricSettingsSheet = true
                            }
                        },
                        lyricDisplaySelected = lyricDisplaySelected,
                        onShowPlaylistClick = playbackController?.let {
                            { showPlaylistSheet = true }
                        },
                        stabilizeTrackInfoSlot = false,
                        sharedTransitionScope = sharedUiTransitionScope,
                        enableSharedTrackInfoTransition = false,
                        enableSharedMenuTransition = false,
                        panelCornerRadius = if (useDynamicCoverChrome) 24.dp else 0.dp,
                        modifier = if (fillPanelHeight) {
                            Modifier.fillMaxHeight()
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                }
            }

            if (useSidePanelLayout) {
                val density = LocalDensity.current
                val usePersistentLeftControlsForLargeScreen =
                    landscapeLyricDisplaySelected && isLargeScreenDevice
                val leftPanelShowsControls =
                    landscapeLyricDisplaySelected &&
                        !usePersistentLeftControlsForLargeScreen &&
                        landscapeTemporaryControlsVisible
                val largeLeftPanelShift by animateDpAsState(
                    targetValue = if (isLargeScreenDevice && !landscapeLyricDisplaySelected) {
                        (sidePanelWidth + landscapePaneSpacing) / 2f
                    } else {
                        0.dp
                    },
                    animationSpec = tween(durationMillis = 360),
                    label = "landscapeLargeLeftPanelShift"
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(landscapeSafeAreaInsets)
                        .padding(horizontal = landscapeOuterHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(landscapePaneSpacing)
                ) {
                    Column(
                        modifier = Modifier
                            .width(sidePanelWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                if (isLargeScreenDevice) {
                                    translationX = with(density) { largeLeftPanelShift.toPx() }
                                }
                            }
                            .background(chromeContainerColor)
                    ) {
                        if (isLargeScreenDevice) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LyricPreviewPlaybackTopLabel(
                                    titleText = portraitTopTitleText,
                                    backgroundColor = backgroundColor,
                                    onBackClick = handleBackRequest,
                                    hasNextTrack = portraitNextTrackTitle.isNotBlank(),
                                    onTitleClick = {
                                        if (portraitNextTrackTitle.isNotBlank()) {
                                            showPortraitNextTrackHint = !showPortraitNextTrackHint
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (isLargeScreenDevice) {
                            val coverVerticalShift by animateDpAsState(
                                targetValue = if (landscapeLyricDisplaySelected) (-8).dp else 0.dp,
                                animationSpec = tween(durationMillis = 320),
                                label = "landscapeLargeCoverShift"
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .animateContentSize(animationSpec = tween(durationMillis = 320))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LyricPreviewSideCover(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                translationY = with(density) { coverVerticalShift.toPx() }
                                            },
                                        coverBitmap = metadata.coverBitmap,
                                        accentColor = accentColor,
                                        backgroundColor = backgroundColor,
                                        isPlaying = isPlaying,
                                        coverWidthFraction = 0.98f,
                                        coverHeightFraction = 0.96f,
                                        coverMaxSizeOverride = 560.dp
                                    )
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    playbackControlsPanel(
                                        null,
                                        Modifier.fillMaxWidth(),
                                        false,
                                        true
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                AnimatedContent(
                                    targetState = leftPanelShowsControls,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(durationMillis = 240)) togetherWith
                                            fadeOut(animationSpec = tween(durationMillis = 200))
                                    },
                                    label = "landscapeCoverControlsSwap",
                                    modifier = Modifier.fillMaxSize()
                                ) { showControls ->
                                    if (showControls) {
                                        playbackControlsPanel(
                                            null,
                                            Modifier.fillMaxSize(),
                                            true,
                                            true
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (landscapeLyricDisplaySelected) {
                                                        Modifier.pointerInput(Unit) {
                                                            detectTapGestures(
                                                                onTap = { triggerLandscapeTemporaryControls() }
                                                            )
                                                        }
                                                    } else {
                                                        Modifier
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LyricPreviewSideCover(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(top = 13.dp),//封面与正在播放文字的间距
                                                coverBitmap = metadata.coverBitmap,
                                                accentColor = accentColor,
                                                backgroundColor = backgroundColor,
                                                isPlaying = isPlaying,
                                                coverWidthFraction = 0.82f,
                                                coverHeightFraction = 0.78f,
                                                coverMaxSizeOverride = 560.dp
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                ) {
                                LyricPreviewPlaybackTopLabel(
                                    titleText = portraitTopTitleText,
                                    backgroundColor = backgroundColor,
                                    onBackClick = handleBackRequest,
                                    hasNextTrack = portraitNextTrackTitle.isNotBlank(),
                                    onTitleClick = {
                                        if (portraitNextTrackTitle.isNotBlank()) {
                                            showPortraitNextTrackHint = !showPortraitNextTrackHint
                                        }
                                    }
                                )
                            }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .width(sidePanelWidth)
                            .fillMaxHeight()
                            .background(
                                if (isLargeScreenDevice || landscapeLyricDisplaySelected) {
                                    Color.Transparent
                                } else {
                                    chromeContainerColor
                                }
                            )
                    ) {
                        if (isLargeScreenDevice) {
                            Spacer(modifier = Modifier.fillMaxSize())
                        } else {
                            AnimatedContent(
                                targetState = landscapeLyricDisplaySelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(durationMillis = 240)) togetherWith
                                        fadeOut(animationSpec = tween(durationMillis = 200))
                                },
                                label = "landscapeRightPaneSwitch",
                                modifier = Modifier.fillMaxSize()
                            ) { showLyrics ->
                                if (showLyrics) {
                                    Spacer(modifier = Modifier.fillMaxSize())
                                } else {
                                    playbackControlsPanel(
                                        null,
                                        Modifier.fillMaxSize(),
                                        true,
                                        true
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (useMiniHeader) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LyricPreviewMiniHeader(
                        title = metadata.title,
                        onHeaderClick = {
                            if (enableSongInfoSheet) {
                                showSongInfoSheet = true
                            }
                        },
                        onBackClick = handleBackRequest,
                        onMenuClick = { menuExpanded = true },
                        menuContent = lyricSettingsMenuContent,
                        backgroundColor = backgroundColor,
                        containerColor = chromeContainerColor
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    playbackControlsPanel(null, Modifier, false, false)
                }
            } else {
                // Headbar 和播放控制放在上层，遮挡额外歌词区域
                SharedTransitionLayout {
                    val portraitSharedTransitionScope = this
                    val portraitTopChromeHeight = if (lyricLayoutSelected) {
                        150.dp
                    } else {
                        WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding() + 56.dp
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(portraitTopChromeHeight)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isPortraitCoverMode,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 160))
                                ) {
                                    LyricPreviewPlaybackTopLabel(
                                        titleText = portraitTopTitleText,
                                        backgroundColor = backgroundColor,
                                        onBackClick = handleBackRequest,
                                        hasNextTrack = portraitNextTrackTitle.isNotBlank(),
                                        onTitleClick = {
                                            if (portraitNextTrackTitle.isNotBlank()) {
                                                showPortraitNextTrackHint = !showPortraitNextTrackHint
                                                portraitTitleSwitchToken += 1
                                            }
                                        }
                                    )
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = lyricLayoutSelected,
                                enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 180))
                            ) {
                                // 顶部渐变透明效果
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = { })
                                        }
                                        .background(brush = edgeFadeTopBrush)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isPortraitCoverMode,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 160)),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val centerCoverVisibilityScope = this
                                    LyricPreviewSideCover(
                                        modifier = Modifier.fillMaxSize(),
                                        coverBitmap = metadata.coverBitmap,
                                        accentColor = accentColor,
                                        backgroundColor = backgroundColor,
                                        isPlaying = isPlaying,
                                        sharedTransitionScope = portraitSharedTransitionScope,
                                        sharedCoverAnimatedVisibilityScope = centerCoverVisibilityScope
                                    )
                                }
                            }

                            // 底部渐变透明效果
                            androidx.compose.animation.AnimatedVisibility(
                                visible = lyricLayoutSelected && portraitControlsVisible && portraitCompanionReadyVisible,
                                enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 180))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = { })
                                        }
                                        .background(brush = edgeFadeBottomBrush),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (companionAvailable && lyricDisplaySelected) {
                                        CompanionToggleButton(
                                            checked = companionSelected,
                                            enabled = !companionBusy,
                                            accentColor = coverThemeColor ?: accentColor,
                                            backgroundColor = backgroundColor,
                                            onToggle = { onCompanionToggle(!companionSelected) },
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = portraitControlsVisible,
                                enter = fadeIn(animationSpec = tween(durationMillis = 240)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 220))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { size ->
                                            if (size.height > 0) {
                                                portraitControlsPanelHeightPx = size.height
                                            }
                                        }
                                ) {
                                    playbackControlsPanel(
                                        portraitSharedTransitionScope,
                                        Modifier,
                                        false,
                                        isPortraitCoverMode
                                    )
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = lyricLayoutSelected,
                            enter = fadeIn(animationSpec = tween(durationMillis = 240)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 160)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            val headerCoverVisibilityScope = this
                            LyricPreviewHeader(
                                title = metadata.title,
                                artist = metadata.artist,
                                coverBitmap = metadata.coverBitmap,
                                onBackClick = handleBackRequest,
                                onHeaderClick = {
                                    if (enableSongInfoSheet) {
                                        showSongInfoSheet = true
                                    }
                                },
                                onMenuClick = { menuExpanded = true },
                                menuContent = lyricSettingsMenuContent,
                                mutedColor = accentColor,
                                isDarkTheme = isDarkTheme,
                                backgroundColor = backgroundColor,
                                containerColor = chromeContainerColor,
                                sharedTransitionScope = portraitSharedTransitionScope,
                                sharedCoverAnimatedVisibilityScope = headerCoverVisibilityScope,
                                sharedHeaderAnimatedVisibilityScope = headerCoverVisibilityScope,
                                enableSharedTrackInfoTransition = false,
                                enableSharedMenuTransition = false
                            )
                        }

                        if (portraitControlsCanAutoHide && !portraitControlsVisible) {
                            val portraitControlsWakeAreaHeight = with(density) {
                                if (portraitControlsPanelHeightPx > 0) {
                                    portraitControlsPanelHeightPx.toDp()
                                } else {
                                    228.dp
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(portraitControlsWakeAreaHeight)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                registerPortraitControlsInteraction()
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        }

        if (useSidePanelLayout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(landscapeSafeAreaInsets)
                    .padding(
                        start = landscapeOuterHorizontalPadding +
                            sidePanelWidth +
                            landscapePaneSpacing +
                            landscapeRightPaneHorizontalPadding,
                        end = landscapeRightPaneHorizontalPadding
                    )
            ) {
                if (landscapeLyricDisplaySelected && companionAvailable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 20.dp)
                            .navigationBarsPadding()
                    ) {
                        CompanionToggleButton(
                            checked = companionSelected,
                            enabled = !companionBusy,
                            accentColor = coverThemeColor ?: accentColor,
                            backgroundColor = backgroundColor,
                            onToggle = { onCompanionToggle(!companionSelected) }
                        )
                    }
                }
            }
        }

        val activePlaybackController = playbackController
        val playbackQueuePaths = activePlaybackController?.queueAudioPaths ?: emptyList()
        val cachedQueueEntries = remember(playbackQueuePaths) {
            LocalPlaylistStore.loadPlaybackQueueEntries(context)
        }
        val cachedQueueEntryByPath = remember(cachedQueueEntries) {
            cachedQueueEntries
                .groupBy { it.path }
                .mapValues { (_, entries) -> entries.first() }
        }
        val playbackQueueAudios = remember(
            playbackQueuePaths,
            activePlaybackController?.currentAudioPath,
            activePlaybackController?.currentTitle,
            activePlaybackController?.currentArtist,
            activePlaybackController?.currentAlbum,
            activePlaybackController?.currentCoverCachePath,
            activePlaybackController?.durationMs,
            metadata.title,
            metadata.artist,
            cachedQueueEntryByPath
        ) {
            playbackQueuePaths.map { path ->
                buildLyricPreviewQueueAudioFile(
                    context = context,
                    path = path,
                    currentAudioPath = activePlaybackController?.currentAudioPath,
                    currentTitle = activePlaybackController?.currentTitle.orEmpty().ifBlank { metadata.title },
                    currentArtist = activePlaybackController?.currentArtist.orEmpty().ifBlank { metadata.artist },
                    currentAlbum = activePlaybackController?.currentAlbum.orEmpty(),
                    currentCoverCachePath = activePlaybackController?.currentCoverCachePath,
                    currentDuration = activePlaybackController?.durationMs ?: 0L,
                    cachedEntry = cachedQueueEntryByPath[path]
                )
            }
        }

        val previewSongInfoAudio = remember(audioPath, metadata.title, metadata.artist, audioDuration) {
            val file = File(audioPath)
            AudioFile(
                path = audioPath,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                duration = audioDuration.coerceAtLeast(0L),
                fileSize = if (file.exists()) file.length() else 0L,
                lastModified = if (file.exists()) file.lastModified() else 0L
            )
        }
        val previewSongInfoIsFavorite = remember(showSongInfoSheet, previewSongInfoAudio.path) {
            previewSongInfoAudio.path in LocalPlaylistStore.loadFavoritePaths(context)
        }

        if (showLyricSettingsSheet) {
            LyricSettingsBottomSheet(
                onDismissRequest = { showLyricSettingsSheet = false },
                showTranslation = showTranslation,
                showTransliteration = showTransliteration,
                supportsLyricBlur = supportsLyricBlur,
                supportsDynamicCoverBackground = supportsDynamicCoverBackground,
                lyricBlurEnabled = lyricBlurPreferenceEnabled,
                lyricGlowEnabled = lyricGlowEnabled,
                dynamicCoverBackgroundEnabled = dynamicCoverBackgroundEnabled,
                lyriconStatusBarEnabled = lyriconStatusBarEnabled,
                keepScreenOnEnabled = keepScreenOnEnabled,
                autoHidePlaybackControlsEnabled = autoHidePlaybackControlsEnabled,
                lyricDisplayMode = lyricDisplayMode,
                lyricDisplayPosition = lyricDisplayPosition,
                fontSize = fontSize,
                fontWeight = fontWeight,
                animationType = animationType,
                wordLiftDistanceDp = wordLiftDistanceDp,
                fontOptions = customFontOptions,
                selectedFontId = selectedCustomFontId,
                onShowTranslationChange = { saveShowTranslation(it) },
                onShowTransliterationChange = { saveShowTransliteration(it) },
                onLyricBlurEnabledChange = { saveLyricBlurEnabled(it) },
                onLyricGlowEnabledChange = { saveLyricGlowEnabled(it) },
                onDynamicCoverBackgroundEnabledChange = { saveDynamicCoverBackgroundEnabled(it) },
                onLyriconStatusBarEnabledChange = { saveLyriconStatusBarEnabled(it) },
                onKeepScreenOnEnabledChange = { saveKeepScreenOnEnabled(it) },
                onAutoHidePlaybackControlsEnabledChange = { saveAutoHidePlaybackControlsEnabled(it) },
                onLyricDisplayModeChange = { saveLyricDisplayMode(it) },
                onLyricDisplayPositionChange = { saveLyricDisplayPosition(it) },
                onFontSizeChange = { saveFontSize(it) },
                onFontWeightChange = { saveFontWeight(it) },
                onAnimationTypeChange = {
                    animationType = it
                    prefs.edit().putInt(LyricPreviewActivity.KEY_INTERLUDE_ANIMATION_TYPE, it).apply()
                },
                onWordLiftDistanceDpChange = { saveWordLiftDistanceDp(it) },
                onOpenCustomFontPicker = {
                    customFontPickerLauncher.launch(arrayOf("*/*"))
                },
                onSelectFont = { fontId ->
                    if (selectedCustomFontId != fontId) {
                        LyricCustomFontStore.setSelectedFontId(context, fontId)
                        selectedCustomFontId = fontId
                    }
                },
                onDeleteFont = { fontId ->
                    val deleted = LyricCustomFontStore.deleteFont(context, fontId)
                    if (deleted) {
                        customFontOptions = LyricCustomFontStore.loadOptions(context)
                        selectedCustomFontId = LyricCustomFontStore.getSelectedFontId(context)
                    }
                },
                containerColor = dialogContainerColor,
                contentColor = dialogContentColor,
                accentColor = dialogAccentColor
            )
        }

        if (showPlaylistSheet && activePlaybackController != null) {
            NowPlayingPlaylistBottomSheet(
                queue = playbackQueueAudios,
                currentAudioPath = activePlaybackController.currentAudioPath,
                canReorder = true,
                onDismiss = { showPlaylistSheet = false },
                onMoveItem = { fromIndex, toIndex ->
                    activePlaybackController.moveQueueItem(fromIndex, toIndex)
                },
                onPlayAtIndex = { index ->
                    activePlaybackController.playAtQueueIndex(index)
                },
                onRemoveByPath = { path ->
                    activePlaybackController.removeQueueItemByPath(path)
                }
            )
        }

        if (enableSongInfoSheet && showSongInfoSheet) {
            SongInfoBottomSheet(
                audio = previewSongInfoAudio,
                isFavorite = previewSongInfoIsFavorite,
                renameSuccessSignal = 0L,
                onDismiss = { showSongInfoSheet = false },
                onEditLyricsFromPreview = {
                    pendingResetLyricDisplayOnResume = true
                },
                onEditMetadataFromSheet = { audioToEdit ->
                    val activity = context as? android.app.Activity ?: return@SongInfoBottomSheet
                    pendingResetLyricDisplayOnResume = true
                    val editIntent = Intent(activity, SongMetadataEditActivity::class.java).apply {
                        putExtra(SongMetadataEditActivity.EXTRA_AUDIO_PATH, audioToEdit.path)
                        putExtra(SongMetadataEditActivity.EXTRA_MEDIA_STORE_ID, audioToEdit.mediaStoreId)
                    }
                    activity.startActivity(editIntent)
                }
            )
        }

        if (showArtistSelectionSheet) {
            ArtistSelectionBottomSheet(
                albumName = pendingArtistSheetAlbum,
                artists = pendingArtistSheetArtists,
                onDismiss = { showArtistSelectionSheet = false },
                onSelectAlbum = { albumName ->
                    showArtistSelectionSheet = false
                    openMusicLibrarySearch("#专辑：$albumName")
                },
                onSelectArtist = { artist ->
                    showArtistSelectionSheet = false
                    openMusicLibrarySearch(artist)
                }
            )
        }
    }
}

private fun extractLyricPreviewArtistsForSheet(title: String, artist: String): List<String> {
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

// 获取字体粗细标签
fun getFontWeightLabel(weight: Int): String {
    val safeWeight = weight.coerceIn(100, 900) / 100 * 100
    return when (safeWeight) {
        100 -> "极细(100)"
        200 -> "特细(200)"
        300 -> "细(300)"
        400 -> "正常(400)"
        500 -> "中(500)"
        600 -> "半粗(600)"
        700 -> "粗(700)"
        800 -> "特粗(800)"
        900 -> "极粗(900)"
        else -> "正常(400)"
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
    nextLineIsDuet: Boolean = false, // 新增：下一句是否是对唱歌词
    isDarkTheme: Boolean = false, // 新增：是否是深色模式
    fontSize: TextUnit = 32.sp, // 新增：字号大小
    isPlaying: Boolean = false, // 新增：歌曲是否正在播放
    animationType: Int = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT, // 新增：动画类型
    lyricColor: Color = if (isDarkTheme) Color.White else Color.Black,
    forceHidden: Boolean = false
) {
    val isVisible = !forceHidden && isInterludeLineVisibleAtTime(interludeLine, currentTime)
    
    val alignment = remember(nextLineIsDuet) {
        if (nextLineIsDuet) Alignment.CenterEnd else Alignment.CenterStart
    }
    val useLightAnimation = lyricColor.luminance() >= 0.5f
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(350)),
        exit = fadeOut(animationSpec = tween(350))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            } else if (animationType == LyricPreviewActivity.ANIMATION_TYPE_DOGE) {
                // 小狗动画
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = alignment
                ) {
                    DogeAnimation(
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

@Composable
fun DogeAnimation(
    isPlaying: Boolean,
    useLightAnimation: Boolean,
    alignment: Alignment
) {
    val animationRes = if (useLightAnimation) R.raw.anim_doge_white else R.raw.anim_doge_black
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))

    var wasPlaying by remember { mutableStateOf(isPlaying) }
    var previousProgress by remember { mutableStateOf(0f) }
    var shouldContinueUntilCycleComplete by remember { mutableStateOf(false) }
    var effectiveIsPlaying by remember { mutableStateOf(isPlaying) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying && wasPlaying) {
            shouldContinueUntilCycleComplete = true
            effectiveIsPlaying = true
        } else if (isPlaying) {
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

    LaunchedEffect(progress) {
        if (shouldContinueUntilCycleComplete && previousProgress > 0.9f && progress < 0.1f) {
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
    songDuration: Long = 0L,
    fontWeight: Int = 400, // 新增：字体粗细
    showTransliteration: Boolean = true, // 新增：是否显示注音
    fontFamily: FontFamily? = null,
    customTypeface: Typeface? = null,
    lyricDisplayMode: Int = LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT,
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
    limitWidthForDuetLayout: Boolean = false,
    isPlaying: Boolean = false, // 新增：歌曲是否正在播放
    animationType: Int = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT, // 新增：动画类型
    wordLiftDistanceDp: Float = LyricPreviewActivity.DEFAULT_WORD_LIFT_DISTANCE_DP,
    lyricGlowEnabled: Boolean = LyricPreviewActivity.DEFAULT_LYRIC_GLOW,
    blurRadius: Float = 0f,
    hideInterludeAnimation: Boolean = false,
    clickableEnabled: Boolean = true,
    overrideInactiveColor: Color? = null,
    onClick: () -> Unit = {}
) {
    // 间奏内容单独在滚动层底部渲染；列表中仅保留不可点击空白占位行
    if (line.isInterlude) {
        val fontSizeDp = with(LocalDensity.current) { fontSize.toDp() }
        val interludeHeight = fontSizeDp * 2
        val interludeLyricColor = backgroundColor?.let { bg ->
            getHighContrastBlackOrWhite(bg)
        } ?: if (isDarkTheme) Color.White else Color.Black
        val baseVisualVisible = if (line.begin <= 0L) {
            currentTime >= 0L && currentTime < line.end
        } else {
            currentTime >= line.begin && currentTime < line.end
        }
        val debugHoldVisualActive =
            DEBUG_KEEP_INTERLUDE_PLACEHOLDER_AFTER_END &&
                currentTime >= line.end &&
                currentTime < (line.end + DEBUG_INTERLUDE_PLACEHOLDER_HOLD_MS)
        val shouldShowInterludeVisual = baseVisualVisible || debugHoldVisualActive
        var lastInterludeVisualVisible by remember(line.begin, line.end) {
            mutableStateOf<Boolean?>(null)
        }
        LaunchedEffect(shouldShowInterludeVisual, currentTime) {
            if (!ENABLE_INTERLUDE_SCROLL_DIAGNOSTIC_LOG) return@LaunchedEffect
            val previous = lastInterludeVisualVisible
            if (previous == null || previous != shouldShowInterludeVisual) {
                Log.d(
                    AUTO_SCROLL_LOG_TAG,
                    "[InterludeDiag] visualVisibility lineBegin=${line.begin} lineEnd=${line.end} " +
                        "time=$currentTime visible=$shouldShowInterludeVisual baseVisible=$baseVisualVisible " +
                        "debugHoldActive=$debugHoldVisualActive"
                )
                lastInterludeVisualVisible = shouldShowInterludeVisual
            }
        }
        AnimatedVisibility(
            visible = shouldShowInterludeVisual,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(interludeHeight)
                    .springPlacement(lookaheadScope, itemKey, isManualScrolling, stiffness, forceReset)
            ) {
                InterludeLineView(
                    interludeLine = line,
                    currentTime = currentTime,
                    nextLineIsDuet = nextLineIsDuet,
                    isDarkTheme = isDarkTheme,
                    fontSize = fontSize,
                    isPlaying = isPlaying,
                    animationType = animationType,
                    lyricColor = interludeLyricColor,
                    forceHidden = hideInterludeAnimation
                )
            }
        }
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
    val defaultInactiveColor = ensureReadableColor(
        candidate = blendColors(activeColor, resolvedBackground, 0.42f),
        background = resolvedBackground,
        fallback = blendColors(baseForeground, resolvedBackground, 0.45f),
        //minContrast = 2.9f
        minContrast = 3.6f
    )
    val inactiveColor = overrideInactiveColor ?: defaultInactiveColor
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
    
    val isLineByLine = when (lyricDisplayMode) {
        LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD -> false
        LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_LINE -> true
        else -> line.isLineByLineLyric()
    }
    val normalizedWords = remember(line.words, nextLine?.begin, songDuration, lyricDisplayMode) {
        if (lyricDisplayMode != LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD) {
            line.words
        } else {
            buildForceWordWords(line, nextLine, songDuration)
        }
    }
    val effectiveEnd = if (isLineByLine) {
        getEffectiveEndTime(line, nextLine)
    } else if (lyricDisplayMode == LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD) {
        resolveForceWordLineEnd(line, nextLine, songDuration)
    } else {
        line.end
    }
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
    val rowContentAlignment = if (effectiveIsDuet) Alignment.CenterEnd else Alignment.CenterStart
    val lineWidthModifier = if (limitWidthForDuetLayout) {
        Modifier.fillMaxWidth(0.85f)
    } else {
        Modifier.fillMaxWidth()
    }

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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = rowContentAlignment
                ) {
                    Column(
                        modifier = lineWidthModifier
                            .alpha(lineAlpha * 0.8f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Transparent)
                            .then(lineBlurModifier)
                            .clickable(enabled = clickableEnabled, onClick = onClick)
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
                                showTransliteration = showTransliteration, // 新增
                                fontFamily = fontFamily
                            )
                        } else {
                            // 逐字歌词渲染 - 支持自动换行
                            LyricWordsCanvasWithWrap(
                                words = normalizedWords,
                                currentTime = currentTime,
                                activeColor = activeColor,
                                inactiveColor = inactiveColor,
                                fontSize = lineFontSize,
                                isDuet = effectiveIsDuet,
                                fontWeight = fontWeight, // 新增
                                showTransliteration = showTransliteration, // 新增
                                fontFamily = fontFamily,
                                customTypeface = customTypeface,
                                wordLiftDistanceDp = wordLiftDistanceDp,
                                lyricGlowEnabled = lyricGlowEnabled
                            )
                        }

                        // 翻译
                        if (showTranslation && line.translation.isNotEmpty()) {
                            Text(
                                text = line.translation,
                                color = translationColor,
                                fontSize = (lineFontSize.value * 0.6).sp,
                                fontWeight = fontWeightValue,
                                fontFamily = fontFamily,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                textAlign = if (effectiveIsDuet) TextAlign.End else TextAlign.Start
                            )
                        }
                    }
                }
            }
        } else {
            // 普通歌词（非背景）直接显示
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = rowContentAlignment
            ) {
                Column(
                    modifier = lineWidthModifier
                        .alpha(lineAlpha)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .then(lineBlurModifier)
                        .clickable(enabled = clickableEnabled, onClick = onClick)
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
                            showTransliteration = showTransliteration, // 新增
                            fontFamily = fontFamily
                        )
                    } else {
                        // 逐字歌词渲染 - 支持自动换行
                        LyricWordsCanvasWithWrap(
                            words = normalizedWords,
                            currentTime = currentTime,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            fontSize = lineFontSize,
                            isDuet = effectiveIsDuet,
                            fontWeight = fontWeight, // 新增
                            showTransliteration = showTransliteration, // 新增
                            fontFamily = fontFamily,
                            customTypeface = customTypeface,
                            wordLiftDistanceDp = wordLiftDistanceDp,
                            lyricGlowEnabled = lyricGlowEnabled
                        )
                    }
                    
                    // 翻译
                    if (showTranslation && line.translation.isNotEmpty()) {
                        Text(
                            text = line.translation,
                            color = translationColor,
                            fontSize = (lineFontSize.value * 0.6).sp,
                            fontWeight = fontWeightValue,
                            fontFamily = fontFamily,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            textAlign = if (effectiveIsDuet) TextAlign.End else TextAlign.Start
                        )
                    }
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
    showTransliteration: Boolean = true, // 新增：是否显示注音
    fontFamily: FontFamily? = null
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
    val textMeasurer = rememberTextMeasurer()
    val mainTextStyle = remember(fontSize, fontWeightValue, fontFamily) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeightValue,
            fontFamily = fontFamily
        )
    }
    // 检查是否有注音
    val hasTransliteration = showTransliteration && line.words.any { 
        it.transliteration.isNotEmpty() || it.charTransliterations.isNotEmpty() 
    }
    
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val wrappedMainText = remember(fullText, maxWidthPx, mainTextStyle) {
            formatLineByLineTextWithSpaceWrapRules(
                text = fullText,
                textMeasurer = textMeasurer,
                style = mainTextStyle,
                maxWidthPx = maxWidthPx
            )
        }
        if (hasTransliteration) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                    fontFamily = fontFamily,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isDuet) TextAlign.End else TextAlign.Start
                )
                // 第二行：显示歌词
                Text(
                    text = wrappedMainText,
                    color = displayColor,
                    fontSize = fontSize,
                    fontWeight = fontWeightValue,
                    fontFamily = fontFamily,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isDuet) TextAlign.End else TextAlign.Start
                )
            }
        } else {
            // 没有注音，只显示歌词
            Text(
                text = wrappedMainText,
                color = displayColor,
                fontSize = fontSize,
                fontWeight = fontWeightValue,
                fontFamily = fontFamily,
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (isDuet) TextAlign.End else TextAlign.Start
            )
        }
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
    showTransliteration: Boolean = true, // 新增：是否显示注音
    fontFamily: FontFamily? = null,
    customTypeface: Typeface? = null,
    wordLiftDistanceDp: Float = LyricPreviewActivity.DEFAULT_WORD_LIFT_DISTANCE_DP,
    lyricGlowEnabled: Boolean = LyricPreviewActivity.DEFAULT_LYRIC_GLOW
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val composeFontWeight = mapComposeFontWeight(fontWeight)
    val fontSizePx = with(density) { fontSize.toPx() }
    val configuration = LocalConfiguration.current
    val fallbackWrapWidthPx = with(density) { (configuration.screenWidthDp.dp - 56.dp).toPx() }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val wrapWidthPx = if (containerWidthPx > 1f) containerWidthPx else fallbackWrapWidthPx
    
    // 创建上抬动画器，使用remember保存状态
    val liftAnimator = remember { WordLiftAnimator() }
    
    // 计算所有单词布局并自动换行
    val lineLayouts = remember(words, fontSize, fontWeight, showTransliteration, fontFamily, wrapWidthPx) {
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
                
                // 否则检查是否是连续拉丁/西里尔字母及其英文模式标点
                val isLetterParticipant = isSingleLiftStaggerParticipantText(word.text)
                val prevWasLetterParticipant = currentGroup.isNotEmpty() && {
                    val lastWord = currentGroup.last().second
                    isSingleLiftStaggerParticipantText(lastWord.text)
                }()
                
                if (isLetterParticipant && prevWasLetterParticipant) {
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
                    style = TextStyle(
                        fontSize = fontSize,
                        fontWeight = composeFontWeight,
                        fontFamily = fontFamily
                    )
                )
                val groupWidth = result.size.width.toFloat()
                val spaceWidth = if (processedText.endsWith(" ")) with(density) { 1.dp.toPx() } else 0f
                
                // 检查是否需要换行
                // 两种情况需要换行：
                // 1. 当前是空格且在允许换行的位置
                // 2. 加上当前内容超过屏幕宽度
                val isBreakableSpace = group.size == 1 && group.first().second.text == " " && breakableSpaceIndices.contains(group.first().first)
                val shouldWrapByWidth = currentX + groupWidth > wrapWidthPx && currentLine.isNotEmpty()
                val isLeadingQuestionMarkGroup = !isBreakableSpace && group.size == 1 && (
                    group.first().second.text == "?" || group.first().second.text == "？"
                )

                // 标点保护：问号不能单独落到新行句首，和前一个字符/单词一起换行。
                if (shouldWrapByWidth && isLeadingQuestionMarkGroup && currentLine.isNotEmpty()) {
                    var moveStartIndex = currentLine.lastIndex
                    while (moveStartIndex > 0 && currentLine[moveStartIndex - 1].word.text != " ") {
                        moveStartIndex--
                    }
                    val movedLayoutsRaw = currentLine.subList(moveStartIndex, currentLine.size).toList()
                    repeat(currentLine.size - moveStartIndex) {
                        currentLine.removeAt(currentLine.lastIndex)
                    }
                    if (currentLine.isNotEmpty()) {
                        layouts.add(currentLine.toList())
                    }
                    currentLine = mutableListOf()
                    var movedX = 0f
                    movedLayoutsRaw.forEach { layout ->
                        val rebased = layout.copy(
                            startPosition = movedX,
                            endPosition = movedX + layout.textWidth
                        )
                        currentLine.add(rebased)
                        movedX = rebased.endPosition + rebased.spaceWidth
                    }
                    currentX = movedX
                    isNewLine = false
                }
                
                if ((currentX + groupWidth > wrapWidthPx && currentLine.isNotEmpty()) || isBreakableSpace) {
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
                                    style = TextStyle(
                                        fontSize = fontSize,
                                        fontWeight = composeFontWeight,
                                        fontFamily = fontFamily
                                    )
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
                        style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = composeFontWeight,
                            fontFamily = fontFamily
                        )
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
                            style = TextStyle(
                                fontSize = transFontSize,
                                fontWeight = composeFontWeight,
                                fontFamily = fontFamily
                            )
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
                val prevIsEnglish = if (index > 0) {
                    val prevWord = words[index - 1]
                    prevWord.text.length == 1 && isEnglishLikeCharForSpaceWrap(prevWord.text[0])
                } else false
                
                val nextIsEnglish = if (index < words.size - 1) {
                    val nextWord = words[index + 1]
                    nextWord.text.length == 1 && isEnglishLikeCharForSpaceWrap(nextWord.text[0])
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
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidthPx = it.width.toFloat() },
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
            val liftStartOverrides = buildLongLyricLiftStartOverrides(effectiveLineWords)
            val glowStates = if (lyricGlowEnabled) {
                buildLyricGlowStates(effectiveLineWords, currentTime)
            } else {
                emptyMap()
            }
            
            Box(
                modifier = Modifier
                    .width(with(density) { lineWidth.toDp() })
                    .height(with(density) { lineHeight.toDp() })
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    effectiveLineWords.forEachIndexed { wordIndex, layout ->
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
                        val liftOffset = liftAnimator.getLiftOffset(
                            word = word,
                            currentTime = currentTime,
                            density = density,
                            liftDistanceDp = wordLiftDistanceDp,
                            liftBeginMs = liftStartOverrides[wordIndex] ?: word.begin,
                            liftEndMs = word.end + 300L
                        )
                        
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
                            fontWeight = fontWeight, // 新增
                            customTypeface = customTypeface,
                            glowState = glowStates[wordIndex]
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
    fontWeight: Int = 400, // 新增：字体粗细
    customTypeface: Typeface? = null,
    glowState: LyricGlowState? = null
) {
    val word = layout.word
    
    // 根据 font weight 选择 typeface 和粗细效果
    val paint = android.graphics.Paint().apply {
        this.textSize = fontSizePx
        this.isAntiAlias = true
        applyAndroidFontWeight(this, fontWeight, customTypeface)
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
    val lyricTextScale = glowState?.textScale?.coerceAtLeast(1f) ?: 1f
    val textPivotX = textX + (layout.textWidth / 2f)
    val textPivotY = mainBaseLineY + fontMetrics.descent

    fun drawMainText(targetPaint: android.graphics.Paint) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        if (lyricTextScale > 1.0001f) {
            nativeCanvas.save()
            nativeCanvas.scale(lyricTextScale, lyricTextScale, textPivotX, textPivotY)
            nativeCanvas.drawText(word.text, textX, mainBaseLineY, targetPaint)
            nativeCanvas.restore()
        } else {
            nativeCanvas.drawText(word.text, textX, mainBaseLineY, targetPaint)
        }
    }

    if (glowState != null && word.text != " ") {
        val (innerRadiusDp, outerRadiusDp, levelAlphaScale) = when (glowState.level) {
            LyricGlowLevel.VERY_SLIGHT -> Triple(2f, 4.5f, 0.42f)
            LyricGlowLevel.SLIGHT -> Triple(3f, 7f, 1f)
            LyricGlowLevel.NORMAL -> Triple(5f, 11f, 1f)
            LyricGlowLevel.HIGH -> Triple(7f, 16f, 1f)
        }
        val glowAlpha = (glowState.alpha * glowState.playbackScale * levelAlphaScale)
            .coerceIn(0f, LYRIC_GLOW_MAX_ALPHA)
        val glowTintColor = blendColors(baseInactiveColor, activeColor, progress.coerceIn(0f, 1f))
        val outerGlowColor = glowTintColor.copy(alpha = 0.28f * glowAlpha)
        val innerGlowColor = glowTintColor.copy(alpha = 0.56f * glowAlpha)
        val bodyGlowColor = glowTintColor.copy(alpha = 0.06f * glowAlpha)

        fun drawGlowLayer(radiusDp: Float, color: Color) {
            val layerPaint = android.graphics.Paint(paint).apply {
                shader = null
                this.color = bodyGlowColor.toArgb()
                setShadowLayer(
                    with(density) { radiusDp.dp.toPx() },
                    0f,
                    0f,
                    color.toArgb()
                )
            }
            drawMainText(layerPaint)
        }

        drawGlowLayer(outerRadiusDp, outerGlowColor)
        drawGlowLayer(innerRadiusDp, innerGlowColor)
    }
    
    // 1. 绘制背景层（灰色）
    drawMainText(paint.apply { color = baseInactiveColor.toArgb() })
    
    // 2. 绘制高亮层
    if (progress > 0f) {
        if (progress >= 1f) {
            // 已完成歌词
            paint.color = activeColor.toArgb()
            paint.shader = null
            drawMainText(paint)
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
            drawMainText(paint)
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
            applyAndroidFontWeight(this, fontWeight, customTypeface)
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun PlaybackControls(
    currentTime: Long,
    duration: Long,
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    seekTimeMs: Long,
    seekTimeSeconds: Float,
    onPlayPauseClick: () -> Unit,
    onSkipPreviousClick: (() -> Unit)? = null,
    onSkipNextClick: (() -> Unit)? = null,
    isSkipNextEnabled: Boolean = true,
    onSeek: (Long) -> Unit,
    vibrantColor: Color? = null,
    backgroundColor: Color? = null,
    containerColor: Color? = null,
    trackTitle: String = "",
    trackArtist: String = "",
    showTrackInfo: Boolean = false,
    onTrackInfoClick: (() -> Unit)? = null,
    onTrackInfoMenuClick: (() -> Unit)? = null,
    trackInfoMenuContent: (@Composable (MenuAnchorPosition?) -> Unit)? = null,
    playbackMode: PlaybackMode? = null,
    onPlaybackModeClick: (() -> Unit)? = null,
    onLyricDisplayClick: (() -> Unit)? = null,
    lyricDisplaySelected: Boolean = false,
    onShowPlaylistClick: (() -> Unit)? = null,
    stabilizeTrackInfoSlot: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    enableSharedTrackInfoTransition: Boolean = false,
    sharedTrackInfoKey: Any = "lyricPreviewTrackInfo",
    enableSharedMenuTransition: Boolean = false,
    sharedMenuIconKey: Any = "lyricPreviewMenuIcon",
    panelCornerRadius: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val safeDuration = duration.coerceAtLeast(0L)
    val seekStart = 0L
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
    val controlContainer = containerColor ?: controlBackground
    val controlAccentBase = vibrantColor ?: MaterialTheme.colorScheme.primary
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
    val progressColor = controlAccentColor
    val progressTrackColor = controlAccentColor.copy(alpha = 0.24f)
    val panelTextColor = getHighContrastBlackOrWhite(controlBackground)
    val density = LocalDensity.current
    var trackInfoMenuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(panelCornerRadius))
            .background(controlContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        @Composable
        fun TrackInfoRow(trackInfoVisibilityScope: AnimatedVisibilityScope) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = showTrackInfo && onTrackInfoClick != null,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onTrackInfoClick?.invoke()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (enableSharedTrackInfoTransition) {
                                Modifier.lyricPreviewSharedElement(
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = trackInfoVisibilityScope,
                                    sharedElementKey = sharedTrackInfoKey
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = trackTitle.ifBlank { "未选择歌曲" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = panelTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trackArtist.ifBlank { "未知艺术家" },
                        fontSize = 15.sp,
                        color = panelTextColor.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onTrackInfoClick != null || onTrackInfoMenuClick != null) {
                    IconButton(
                        onClick = { onTrackInfoMenuClick?.invoke() },
                        enabled = showTrackInfo,
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInRoot()
                                trackInfoMenuButtonPosition = MenuAnchorPosition(
                                    x = with(density) { bounds.center.x.toDp().value },
                                    y = with(density) { bounds.center.y.toDp().value }
                                )
                            }
                            .then(
                                if (enableSharedMenuTransition) {
                                    Modifier.lyricPreviewSharedElement(
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = trackInfoVisibilityScope,
                                        sharedElementKey = sharedMenuIconKey
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = panelTextColor
                        )
                    }
                    trackInfoMenuContent?.invoke(trackInfoMenuButtonPosition)
                }
            }
        }

        if (stabilizeTrackInfoSlot) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTrackInfo,
                    enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TrackInfoRow(this)
                }
            }
        } else {
            androidx.compose.animation.AnimatedVisibility(
                visible = showTrackInfo,
                enter = fadeIn(animationSpec = tween(durationMillis = 220)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                TrackInfoRow(this)
            }
        }

        // 进度条（上行）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(vertical = 6.dp)
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

        // 控制按钮（下行）
        val sideButtonColors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = oppositeControlColor,
            contentColor = onOppositeControlColor,
            checkedContainerColor = oppositeControlColor,
            checkedContentColor = onOppositeControlColor
        )
        val middleButtonColors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = controlAccentColor,
            contentColor = onControlAccentColor,
            checkedContainerColor = controlAccentColor,
            checkedContentColor = onControlAccentColor
        )
        ButtonGroup(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            ToggleButton(
                checked = false,
                onCheckedChange = {
                    if (onSkipPreviousClick != null) {
                        onSkipPreviousClick()
                    } else {
                        val target = (clampedCurrentTime - seekTimeMs).coerceAtLeast(0L)
                        onSeek(target)
                    }
                },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                colors = sideButtonColors
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = if (onSkipPreviousClick != null) "上一首" else "后退${seekTimeSeconds}秒"
                )
            }
            ToggleButton(
                checked = isPlaying,
                onCheckedChange = { onPlayPauseClick() },
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxSize(),
                colors = middleButtonColors
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                    ),
                    contentDescription = if (isPlaying) "暂停" else "播放"
                )
            }
            ToggleButton(
                checked = false,
                onCheckedChange = {
                    if (!isSkipNextEnabled) return@ToggleButton
                    if (onSkipNextClick != null) {
                        onSkipNextClick()
                    } else {
                        val target = (clampedCurrentTime + seekTimeMs).coerceAtMost(safeDuration)
                        onSeek(target)
                    }
                },
                enabled = isSkipNextEnabled,
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                colors = sideButtonColors
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = if (onSkipNextClick != null) "下一首" else "前进${seekTimeSeconds}秒"
                )
            }
        }

        val hasBottomActions = onPlaybackModeClick != null || onLyricDisplayClick != null || onShowPlaylistClick != null
        if (hasBottomActions) {
            val actionIconSize = 20.dp
            val actionButtonRadius = 16.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modeIcon = when (playbackMode) {
                    PlaybackMode.SHUFFLE -> Icons.Rounded.Shuffle
                    PlaybackMode.SINGLE_REPEAT -> Icons.Rounded.RepeatOne
                    else -> Icons.Rounded.Repeat
                }
                LyricPreviewBottomActionButton(
                    modifier = Modifier.weight(1f),
                    icon = modeIcon,
                    tint = controlAccentColor,
                    enabled = onPlaybackModeClick != null,
                    onClick = { onPlaybackModeClick?.invoke() },
                    contentDescription = "播放模式",
                    iconSize = actionIconSize,
                    cornerRadius = actionButtonRadius
                )
                LyricPreviewBottomActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Lyrics,
                    tint = controlAccentColor,
                    enabled = onLyricDisplayClick != null,
                    selected = lyricDisplaySelected,
                    onClick = { onLyricDisplayClick?.invoke() },
                    contentDescription = "歌词显示",
                    iconSize = actionIconSize,
                    cornerRadius = actionButtonRadius
                )
                LyricPreviewBottomActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    tint = controlAccentColor,
                    enabled = onShowPlaylistClick != null,
                    onClick = { onShowPlaylistClick?.invoke() },
                    contentDescription = "播放列表",
                    iconSize = actionIconSize,
                    cornerRadius = actionButtonRadius
                )
            }
        }
    }
}

@Composable
private fun LyricPreviewBottomActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    enabled: Boolean,
    selected: Boolean = false,
    onClick: () -> Unit,
    contentDescription: String,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 16.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                tint.copy(
                    alpha = when {
                        !enabled -> 0.08f
                        selected -> 0.32f
                        else -> 0.18f
                    }
                )
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = tint.copy(alpha = if (selected) 0.44f else 0f),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.38f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CompanionToggleButton(
    checked: Boolean,
    enabled: Boolean,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val backgroundIsLight = colorLuminance(backgroundColor) > 0.5f
    val uncheckedColorTarget = if (backgroundIsLight) {
        blendColorForUi(accentColor, Color.White, 0.60f).copy(alpha = 0.72f)
    } else {
        blendColorForUi(accentColor, Color.White, 0.76f).copy(alpha = 0.64f)
    }
    val checkedColorTarget = if (backgroundIsLight) {
        blendColorForUi(accentColor, Color.Black, 0.80f).copy(alpha = 0.94f)
    } else {
        blendColorForUi(accentColor, Color.Black, 0.86f).copy(alpha = 0.92f)
    }
    val borderColorTarget = if (checked) {
        if (backgroundIsLight) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.28f)
    } else {
        if (backgroundIsLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.14f)
    }
    val containerColor by animateColorAsState(
        targetValue = if (checked) checkedColorTarget else uncheckedColorTarget,
        animationSpec = tween(durationMillis = 260),
        label = "companionContainerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = borderColorTarget,
        animationSpec = tween(durationMillis = 260),
        label = "companionBorderColor"
    )
    val iconColor = getHighContrastBlackOrWhite(containerColor)
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onToggle() }
            .padding(9.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sing),
            contentDescription = "伴奏",
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

fun formatPreviewTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun isEnglishLikeCharForSpaceWrap(c: Char): Boolean {
    return (c in 'a'..'z') || (c in 'A'..'Z') || c == '\'' || c == '"'
}

private fun formatLineByLineTextWithSpaceWrapRules(
    text: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int
): String {
    if (text.isEmpty()) return text
    if (maxWidthPx <= 1) return text
    val chars = text.toCharArray()
    val candidateIndices = mutableListOf<Int>()
    for (i in chars.indices) {
        if (chars[i] != ' ') continue
        val prev = chars.getOrNull(i - 1)
        val next = chars.getOrNull(i + 1)
        val prevIsEnglishLike = prev?.let(::isEnglishLikeCharForSpaceWrap) == true
        val nextIsEnglishLike = next?.let(::isEnglishLikeCharForSpaceWrap) == true
        if (!(prevIsEnglishLike && nextIsEnglishLike)) {
            candidateIndices.add(i)
        }
    }
    if (candidateIndices.isEmpty()) return text

    fun measureLayout(value: String) = textMeasurer.measure(
        text = value,
        style = style,
        constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxWidthPx)
    )

    fun lineCountOf(value: String): Int {
        return measureLayout(value).lineCount
    }

    val originalLineCount = lineCountOf(text)
    var bestText = text
    var bestBalance = Double.MAX_VALUE

    fun evaluateCandidate(candidate: String) {
        val layout = measureLayout(candidate)
        if (layout.lineCount > originalLineCount) return
        val widths = (0 until layout.lineCount).map { lineIndex ->
            (layout.getLineRight(lineIndex) - layout.getLineLeft(lineIndex)).toDouble()
        }
        if (widths.isEmpty()) return
        val avg = widths.average()
        val balance = widths.sumOf { (it - avg) * (it - avg) }
        if (balance < bestBalance) {
            bestBalance = balance
            bestText = candidate
        }
    }

    fun applySubsetBreaks(subset: Set<Int>): String {
        if (subset.isEmpty()) return text
        val copied = chars.copyOf()
        subset.forEach { idx ->
            if (idx in copied.indices && copied[idx] == ' ') {
                // 对逐行歌词采用显式换行，避免系统在CJK字符中间断行导致断点偏移。
                copied[idx] = '\n'
            }
        }
        return String(copied)
    }

    evaluateCandidate(text)

    if (candidateIndices.size <= 5) {
        val totalMask = 1 shl candidateIndices.size
        for (mask in 1 until totalMask) {
            val subset = mutableSetOf<Int>()
            for (bit in candidateIndices.indices) {
                if ((mask and (1 shl bit)) != 0) {
                    subset.add(candidateIndices[bit])
                }
            }
            evaluateCandidate(applySubsetBreaks(subset))
        }
    } else {
        evaluateCandidate(applySubsetBreaks(setOf(candidateIndices.first())))
        evaluateCandidate(applySubsetBreaks(setOf(candidateIndices.last())))
        evaluateCandidate(applySubsetBreaks(candidateIndices.takeLast(2).toSet()))
        evaluateCandidate(applySubsetBreaks(setOf(candidateIndices[candidateIndices.size / 2])))
        val everyOther = mutableSetOf<Int>()
        for (i in candidateIndices.indices step 2) {
            everyOther.add(candidateIndices[i])
        }
        evaluateCandidate(applySubsetBreaks(everyOther))
    }

    return bestText
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

@Composable
fun LyricPreviewCompactHeader(
    title: String,
    artist: String,
    backgroundColor: Color,
    containerColor: Color = backgroundColor,
    onBackClick: () -> Unit = {},
    onHeaderClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    menuContent: @Composable (menuButtonPosition: MenuAnchorPosition?) -> Unit = {}
) {
    val textColor = getHighContrastBlackOrWhite(backgroundColor)
    val titleSize = 20.sp
    val artistSize = 16.sp
    val artistColor = ensureReadableColor(
        candidate = textColor.copy(alpha = 0.74f),
        background = backgroundColor,
        fallback = textColor,
        minContrast = 3.2f
    )
    val iconColor = getHighContrastBlackOrWhite(backgroundColor)
    var menuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                contentDescription = "返回",
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onHeaderClick()
                },
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = artist,
                fontSize = artistSize,
                color = artistColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(40.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    menuButtonPosition = MenuAnchorPosition(
                        x = with(density) { bounds.center.x.toDp().value },
                        y = with(density) { bounds.center.y.toDp().value }
                    )
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                contentDescription = "菜单",
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        menuContent(menuButtonPosition)
    }
}

@Composable
fun LyricPreviewMiniHeader(
    title: String,
    backgroundColor: Color,
    containerColor: Color = backgroundColor,
    onHeaderClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    menuContent: @Composable (menuButtonPosition: MenuAnchorPosition?) -> Unit = {}
) {
    val textColor = getHighContrastBlackOrWhite(backgroundColor)
    val iconColor = getHighContrastBlackOrWhite(backgroundColor)
    var menuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(52.dp)
            .background(containerColor)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                contentDescription = "返回",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onHeaderClick()
                }
                .padding(horizontal = 6.dp)
        )
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(36.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    menuButtonPosition = MenuAnchorPosition(
                        x = with(density) { bounds.center.x.toDp().value },
                        y = with(density) { bounds.center.y.toDp().value }
                    )
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                contentDescription = "菜单",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        menuContent(menuButtonPosition)
    }
}

@Composable
private fun LyricPreviewPlaybackTopLabel(
    titleText: String,
    backgroundColor: Color,
    onBackClick: () -> Unit,
    hasNextTrack: Boolean,
    onTitleClick: () -> Unit
) {
    val foregroundColor = getHighContrastBlackOrWhite(backgroundColor)
    val headerSideSize = 40.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.size(headerSideSize),
            onClick = onBackClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.down),
                contentDescription = "返回",
                tint = foregroundColor.copy(alpha = 0.88f),
                modifier = Modifier.size(18.dp)
            )
        }
        Crossfade(
            targetState = titleText,
            animationSpec = tween(durationMillis = 260),
            modifier = Modifier
                .weight(1f)
                .clickable(
                    enabled = hasNextTrack,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onTitleClick()
                },
            label = "previewPlaybackTopTitle"
        ) { text ->
            Text(
                text = text,
                fontSize = 14.sp,
                color = foregroundColor.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.width(headerSideSize))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.lyricPreviewSharedElement(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    sharedElementKey: Any
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return this
    val sharedContentState = with(sharedTransitionScope) {
        rememberSharedContentState(key = sharedElementKey)
    }
    return with(sharedTransitionScope) {
        this@lyricPreviewSharedElement.sharedElement(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.lyricPreviewSharedCoverElement(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    sharedCoverKey: Any
): Modifier = lyricPreviewSharedElement(
    sharedTransitionScope = sharedTransitionScope,
    animatedVisibilityScope = animatedVisibilityScope,
    sharedElementKey = sharedCoverKey
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LyricPreviewSideCover(
    modifier: Modifier,
    coverBitmap: Bitmap?,
    accentColor: Color,
    backgroundColor: Color,
    isPlaying: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    sharedCoverAnimatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedCoverKey: Any = "lyricPreviewCover",
    coverWidthFraction: Float = 0.94f,
    coverHeightFraction: Float = 0.90f,
    coverMaxSizeOverride: Dp? = null
) {
    val placeholderColor = blendColors(backgroundColor, accentColor, 0.22f)
    val screenConfig = LocalConfiguration.current
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = tween(durationMillis = 280),
        label = "previewCoverScale"
    )
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val coverAbsoluteMax = coverMaxSizeOverride ?: when {
            screenConfig.screenWidthDp >= 1800 -> 500.dp
            screenConfig.screenWidthDp >= 1200 -> 460.dp
            else -> 400.dp
        }
        val coverMaxWidth = minOf(
            maxWidth * coverWidthFraction.coerceIn(0.5f, 1f),
            coverAbsoluteMax
        )
        val coverMaxHeight = minOf(
            maxHeight * coverHeightFraction.coerceIn(0.5f, 1f),
            coverAbsoluteMax
        )
        val coverAspectRatio = remember(coverBitmap) {
            if (coverBitmap != null && coverBitmap.height > 0) {
                (coverBitmap.width.toFloat() / coverBitmap.height.toFloat())
                    .takeIf { it.isFinite() && it > 0f }
            } else {
                null
            }
        } ?: 1f
        val coverWidth: Dp
        val coverHeight: Dp
        if (coverAspectRatio >= 1f) {
            val candidateWidth = coverMaxHeight * coverAspectRatio
            coverWidth = minOf(coverMaxWidth, candidateWidth)
            coverHeight = (coverWidth / coverAspectRatio).coerceAtMost(coverMaxHeight)
        } else {
            val candidateHeight = coverMaxWidth / coverAspectRatio
            coverHeight = minOf(coverMaxHeight, candidateHeight)
            coverWidth = (coverHeight * coverAspectRatio).coerceAtMost(coverMaxWidth)
        }

        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap.asImageBitmap(),
                contentDescription = "封面",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(coverWidth)
                    .height(coverHeight)
                    .lyricPreviewSharedCoverElement(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = sharedCoverAnimatedVisibilityScope,
                        sharedCoverKey = sharedCoverKey
                    )
                    .graphicsLayer(
                        scaleX = coverScale,
                        scaleY = coverScale
                    )
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .width(coverWidth.coerceAtLeast(120.dp))
                    .height(coverHeight.coerceAtLeast(120.dp))
                    .lyricPreviewSharedCoverElement(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = sharedCoverAnimatedVisibilityScope,
                        sharedCoverKey = sharedCoverKey
                    )
                    .graphicsLayer(
                        scaleX = coverScale,
                        scaleY = coverScale
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(placeholderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.img),
                    contentDescription = "封面占位",
                    tint = getHighContrastBlackOrWhite(placeholderColor),
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LyricPreviewHeader(
    title: String,
    artist: String,
    coverBitmap: Bitmap?,
    onBackClick: () -> Unit = {},
    onHeaderClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    menuContent: @Composable (menuButtonPosition: MenuAnchorPosition?) -> Unit = {},
    mutedColor: Color? = null,
    isDarkTheme: Boolean = false,
    backgroundColor: Color? = null,
    containerColor: Color? = backgroundColor,
    sharedTransitionScope: SharedTransitionScope? = null,
    sharedCoverAnimatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedCoverKey: Any = "lyricPreviewCover",
    sharedHeaderAnimatedVisibilityScope: AnimatedVisibilityScope? = sharedCoverAnimatedVisibilityScope,
    enableSharedTrackInfoTransition: Boolean = false,
    sharedTrackInfoKey: Any = "lyricPreviewTrackInfo",
    enableSharedMenuTransition: Boolean = false,
    sharedMenuIconKey: Any = "lyricPreviewMenuIcon"
) {
    val headerBackground = backgroundColor ?: MaterialTheme.colorScheme.surface
    val headerContainer = containerColor ?: headerBackground
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
    val headerHeight = 150.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(headerContainer)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
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

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onHeaderClick()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val coverAspectRatio = remember(coverBitmap) {
                    coverBitmap?.let { bitmap ->
                        if (bitmap.height > 0) {
                            (bitmap.width.toFloat() / bitmap.height.toFloat())
                                .takeIf { it.isFinite() && it > 0f }
                        } else {
                            null
                        }
                    } ?: 1f
                }
                val coverBoxMax = 75.dp
                val coverWidth = if (coverAspectRatio >= 1f) {
                    coverBoxMax
                } else {
                    (coverBoxMax * coverAspectRatio).coerceAtLeast(42.dp)
                }
                val coverHeight = if (coverAspectRatio >= 1f) {
                    (coverBoxMax / coverAspectRatio).coerceAtLeast(42.dp)
                } else {
                    coverBoxMax
                }

                // 封面
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = "封面",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(coverWidth)
                            .height(coverHeight)
                            .lyricPreviewSharedCoverElement(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = sharedCoverAnimatedVisibilityScope,
                                sharedCoverKey = sharedCoverKey
                            )
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(75.dp)
                            .lyricPreviewSharedCoverElement(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = sharedCoverAnimatedVisibilityScope,
                                sharedCoverKey = sharedCoverKey
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.img),
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
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (enableSharedTrackInfoTransition) {
                                Modifier.lyricPreviewSharedElement(
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = sharedHeaderAnimatedVisibilityScope,
                                    sharedElementKey = sharedTrackInfoKey
                                )
                            } else {
                                Modifier
                            }
                        ),
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
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 菜单按钮
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .then(
                        if (enableSharedMenuTransition) {
                            Modifier.lyricPreviewSharedElement(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = sharedHeaderAnimatedVisibilityScope,
                                sharedElementKey = sharedMenuIconKey
                            )
                        } else {
                            Modifier
                        }
                    )
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
    val album: String,
    val comment: String,
    val coverBitmap: Bitmap?
)

private fun buildLyricPreviewQueueAudioFile(
    context: Context,
    path: String,
    currentAudioPath: String?,
    currentTitle: String,
    currentArtist: String,
    currentAlbum: String,
    currentCoverCachePath: String?,
    currentDuration: Long,
    cachedEntry: LocalPlaylistEntry?
): AudioFile {
    val file = File(path)
    val isCurrent = path == currentAudioPath
    val title = if (isCurrent) {
        currentTitle
    } else {
        cachedEntry?.title.orEmpty()
    }.ifBlank { file.nameWithoutExtension }
    val artist = if (isCurrent) currentArtist else cachedEntry?.artist.orEmpty()
    val coverCachePath = if (isCurrent) {
        currentCoverCachePath ?: resolveLyricPreviewCoverCachePath(context, path)
    } else {
        resolveLyricPreviewCoverCachePath(context, path)
    }
    return AudioFile(
        path = path,
        title = title,
        artist = artist,
        album = if (isCurrent) currentAlbum else "",
        duration = if (isCurrent) currentDuration.coerceAtLeast(0L) else 0L,
        fileSize = if (file.exists()) file.length() else 0L,
        lastModified = if (file.exists()) file.lastModified() else 0L,
        addedTime = if (file.exists()) file.lastModified() else System.currentTimeMillis(),
        coverCachePath = coverCachePath
    )
}

private fun resolveLyricPreviewCoverCachePath(context: Context, audioPath: String): String? {
    if (audioPath.isBlank()) return null
    val cacheFile = File(File(context.cacheDir, "covers"), "${audioPath.hashCode()}.jpg")
    return cacheFile.absolutePath.takeIf { cacheFile.exists() }
}

private fun resolveMediaStoreAudioUri(
    context: Context,
    filePath: String,
    mediaStoreId: Long = -1L
): android.net.Uri? {
    return try {
        if (mediaStoreId > 0L) {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
        }
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val args = arrayOf(filePath)
        context.contentResolver.query(
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

private fun readExternalTtmlWithFallback(
    context: Context,
    audioPath: String
): String? {
    val ttmlFile = runCatching {
        val audioFile = File(audioPath)
        val parent = audioFile.parentFile ?: return@runCatching null
        File(parent, "${audioFile.nameWithoutExtension}.ttml")
    }.getOrNull() ?: return null

    val directRead = runCatching {
        ttmlFile.readText().takeIf { it.isNotBlank() }
    }.getOrNull()
    if (!directRead.isNullOrBlank()) return directRead

    return runCatching {
        val baseUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val args = arrayOf(ttmlFile.absolutePath)
        context.contentResolver.query(baseUri, projection, selection, args, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@runCatching null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            val contentUri = ContentUris.withAppendedId(baseUri, id)
            context.contentResolver.openInputStream(contentUri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readText().takeIf { it.isNotBlank() }
            }
        }
    }.getOrNull()
}

suspend fun loadAudioMetadata(
    context: Context,
    audioPath: String,
    fallbackTitle: String,
    mediaStoreId: Long = -1L
): PreviewAudioMetadata {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("LyricPreview", "Loading metadata from: $audioPath")
            val metadata = AudioMetadataReader.readMetadata(
                context = context,
                filePath = audioPath,
                mediaStoreId = mediaStoreId,
                includeCover = true
            )
            val title = metadata.title.takeIf { it.isNotBlank() } ?: fallbackTitle
            val artist = metadata.artist.takeIf { it.isNotBlank() } ?: "未知艺术家"
            val album = metadata.album
            val comment = metadata.comment
            val coverBitmap = metadata.cover?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }

            PreviewAudioMetadata(title, artist, album, comment, coverBitmap)
        } catch (e: Exception) {
            Log.e("LyricPreview", "Failed to load metadata", e)
            PreviewAudioMetadata(fallbackTitle, "未知艺术家", "", "", null)
        }
    }
}
