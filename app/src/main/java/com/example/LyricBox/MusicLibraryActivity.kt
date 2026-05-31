package com.example.LyricBox

import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.icu.text.Transliterator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import android.content.SharedPreferences
import androidx.collection.LruCache
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ripple
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.documentfile.provider.DocumentFile
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.AutoMarqueeText
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.components.GlobalMiniPlayerBar
import com.example.LyricBox.lyrics.LyricsService
import com.example.LyricBox.lyrics.models.SongInfo
import com.example.LyricBox.lyrics.models.Source
import com.example.LyricBox.lyrics.models.VerbatimLyricsResult
import com.example.LyricBox.lyrics.parser.VerbatimLrcConverter
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.LyricBox.utils.PiracyChecker
import com.example.LyricBox.utils.PiracyCheckResult
import com.example.LyricBox.utils.LyricExportFormat
import com.example.LyricBox.utils.LyricSaveEmbedUtils
import com.example.LyricBox.utils.SecureStorage
import com.example.LyricBox.utils.UpdateChecker
import com.example.LyricBox.utils.UpdateInfo
import com.example.LyricBox.utils.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AudioFile(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val fileSize: Long,
    val lastModified: Long = 0L,
    val addedTime: Long = System.currentTimeMillis(),
    val coverCachePath: String? = null,
    val year: String = "",
    val mediaStoreId: Long = -1L
) {
    val displayTitle: String
        get() = if (title.isNotEmpty()) title else File(path).nameWithoutExtension
    
    val displayArtist: String
        get() = if (artist.isNotEmpty()) artist else "Unknown Artist"
    
    val displayAlbum: String
        get() = if (album.isNotEmpty()) album else "Unknown Album"
    
    val displayInfo: String
        get() {
            val sizeStr = formatFileSize(fileSize)
            val durationStr = formatAudioDuration(duration)
            return "$sizeStr · $durationStr · ${File(path).parent?.substringAfterLast("/") ?: ""}"
        }
    
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("path", path)
            put("title", title)
            put("artist", artist)
            put("album", album)
            put("duration", duration)
            put("fileSize", fileSize)
            put("lastModified", lastModified)
            put("addedTime", addedTime)
            put("coverCachePath", coverCachePath)
            put("year", year)
            put("mediaStoreId", mediaStoreId)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): AudioFile {
            return AudioFile(
                path = json.getString("path"),
                title = json.optString("title", ""),
                artist = json.optString("artist", ""),
                album = json.optString("album", ""),
                duration = json.optLong("duration", 0),
                fileSize = json.optLong("fileSize", 0),
                lastModified = json.optLong("lastModified", 0),
                addedTime = json.optLong("addedTime", System.currentTimeMillis()),
                coverCachePath = if (json.has("coverCachePath")) {
                    val value = json.optString("coverCachePath")
                    if (value.isNotEmpty()) value else null
                } else null,
                year = json.optString("year", ""),
                mediaStoreId = json.optLong("mediaStoreId", -1L)
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

fun formatAudioDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000) % 60
    return String.format("%d:%02d", minutes, seconds)
}

private const val FAVORITE_SEARCH_TOKEN = "#收藏歌曲"
private const val ALBUM_SEARCH_PREFIX_FULL = "#专辑："
private const val ALBUM_SEARCH_PREFIX_ASCII = "#专辑:"
private const val SEARCH_HISTORY_PREFS_KEY = "music_library_recent_search_history"
private const val SEARCH_HISTORY_LIMIT = 5
private const val EXTERNAL_AUDIO_LOG_TAG = "MusicLibraryExternal"
private const val PREVIEW_LYRICS_REFRESH_LOG_TAG = "PreviewLyricsRefresh"
private const val PREF_SONG_CLICK_ACTION_CONFIRMED = "songClickActionConfirmed"
private const val STARTUP_APP_SETTINGS_PREFS_NAME = "AppSettings"
private const val STARTUP_NOTICE_SNOOZE_DATE_KEY = "noticeSnoozeDate"
private const val FILE_NAME_SORT_KEY_CACHE_SIZE = 4096
private val FILE_NAME_SORT_KEY_SPACE_REGEX = Regex("\\s+")

private fun sanitizeInlinePreviewLyricLogText(value: String?, maxLength: Int = 80): String {
    val compact = value
        ?.replace("\r", " ")
        ?.replace("\n", " ")
        ?.replace("\t", " ")
        ?.trim()
        .orEmpty()
    return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
}

private fun summarizeInlinePreviewLyricsContentForLog(lyricsContent: String?): String {
    return if (lyricsContent == null) {
        "content=null"
    } else {
        "len=${lyricsContent.length} hash=${lyricsContent.hashCode()} first=\"${sanitizeInlinePreviewLyricLogText(lyricsContent)}\""
    }
}

private fun summarizeInlinePreviewLyricLinesForLog(lines: List<NewPreviewLyricLine>): String {
    val firstText = lines.firstOrNull()
        ?.words
        ?.joinToString(separator = "") { it.text }
        .orEmpty()
    return "count=${lines.size} hash=${lines.hashCode()} first=\"${sanitizeInlinePreviewLyricLogText(firstText)}\""
}
private val FILE_NAME_SORT_KEY_CACHE = LruCache<String, String>(FILE_NAME_SORT_KEY_CACHE_SIZE)
private val FILE_NAME_SORT_KEY_LOCK = Any()
private val FILE_NAME_SORT_TRANSLITERATOR: Transliterator? by lazy(LazyThreadSafetyMode.NONE) {
    runCatching { Transliterator.getInstance("Any-Latin; Latin-ASCII") }
        .onFailure { Log.w("MusicLibrarySort", "ICU Transliterator init failed: ${it.message}") }
        .getOrNull()
}

internal fun buildFileNameSortKey(value: String): String {
    val source = value.trim()
    if (source.isEmpty()) return "~"

    synchronized(FILE_NAME_SORT_KEY_LOCK) {
        FILE_NAME_SORT_KEY_CACHE.get(source)?.let { return it }
    }

    val transliterated = runCatching {
        FILE_NAME_SORT_TRANSLITERATOR?.transliterate(source) ?: source
    }.getOrDefault(source)

    val normalized = FILE_NAME_SORT_KEY_SPACE_REGEX
        .replace(transliterated.lowercase(Locale.ROOT), " ")
        .trim()
        .ifEmpty { source.lowercase(Locale.ROOT) }

    synchronized(FILE_NAME_SORT_KEY_LOCK) {
        FILE_NAME_SORT_KEY_CACHE.put(source, normalized)
    }
    return normalized
}

internal fun buildFileNameSortBucket(value: String): String {
    val firstLetter = buildFileNameSortKey(value)
        .firstOrNull { it.isLetter() }
        ?.uppercaseChar()
    return firstLetter?.toString() ?: "{"
}

private fun loadRecentSearchHistory(prefs: SharedPreferences): List<String> {
    return prefs.getString(SEARCH_HISTORY_PREFS_KEY, null)
        ?.split("\n")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() && !it.equals(FAVORITE_SEARCH_TOKEN, ignoreCase = true) }
        ?.distinct()
        ?.take(SEARCH_HISTORY_LIMIT)
        ?: emptyList()
}

private fun saveRecentSearchHistory(prefs: SharedPreferences, history: List<String>) {
    val normalized = history
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.equals(FAVORITE_SEARCH_TOKEN, ignoreCase = true) }
        .distinct()
        .take(SEARCH_HISTORY_LIMIT)
    prefs.edit().putString(SEARCH_HISTORY_PREFS_KEY, normalized.joinToString("\n")).apply()
}

private fun appendRecentSearchHistory(
    prefs: SharedPreferences,
    history: MutableList<String>,
    query: String
) {
    val normalized = query.trim()
    if (normalized.isEmpty()) return
    if (normalized.equals(FAVORITE_SEARCH_TOKEN, ignoreCase = true)) return
    history.removeAll { it.equals(normalized, ignoreCase = true) }
    history.add(0, normalized)
    while (history.size > SEARCH_HISTORY_LIMIT) {
        history.removeAt(history.lastIndex)
    }
    saveRecentSearchHistory(prefs, history)
}

private fun parseTrackNumberValue(trackRaw: String): Int {
    if (trackRaw.isBlank()) return Int.MAX_VALUE
    val firstPart = trackRaw.substringBefore("/").trim()
    return firstPart.toIntOrNull()
        ?: Regex("""\d+""").find(firstPart)?.value?.toIntOrNull()
        ?: Int.MAX_VALUE
}

private val LYRIC_FORMAT_OPTIONS = listOf(
    "纯文本歌词",
    "LRC逐行/逐字歌词",
    "增强LRC/ELRC歌词",
    "TTML歌词"
)

private data class AudioLyricsLoadResult(
    val embeddedLyrics: String?,
    val externalLyrics: String?,
    val externalLyricsPath: String?,
    val detectedEmbeddedFormat: Int
) {
    val hasEmbedded: Boolean
        get() = !embeddedLyrics.isNullOrBlank()
    val hasExternal: Boolean
        get() = !externalLyrics.isNullOrBlank()
    val isEmbeddedOnly: Boolean
        get() = hasEmbedded && !hasExternal
}

private fun Int.toLyricFormatLabel(): String {
    val safeIndex = coerceIn(0, LYRIC_FORMAT_OPTIONS.lastIndex)
    return LYRIC_FORMAT_OPTIONS[safeIndex]
}

internal fun handleMusicLibraryItemLyricsAction(
    scope: CoroutineScope,
    context: Context,
    audio: AudioFile,
    autoDetectEmbeddedLyricsType: Boolean,
    onShowOptions: (AudioFile) -> Unit,
    onStartLyricTimingEditor: (AudioFile, String?, String) -> Unit
) {
    if (!autoDetectEmbeddedLyricsType) {
        onShowOptions(audio)
        return
    }

    scope.launch {
        val lyricsLoadResult = loadAudioLyricsLoadResult(
            context = context,
            audioPath = audio.path,
            mediaStoreId = audio.mediaStoreId
        )
        if (lyricsLoadResult.hasExternal) {
            onShowOptions(audio)
            return@launch
        }
        if (lyricsLoadResult.isEmbeddedOnly && lyricsLoadResult.embeddedLyrics != null) {
            onStartLyricTimingEditor(
                audio,
                lyricsLoadResult.embeddedLyrics,
                lyricsLoadResult.detectedEmbeddedFormat.toLyricFormatLabel()
            )
        } else {
            onShowOptions(audio)
        }
    }
}

private data class PreviewLyricPayload(
    val lines: List<NewPreviewLyricLine>,
    val creators: List<String>
)

private fun buildPlainTextLyricLines(lyricsContent: String): List<LyricLine> {
    return lyricsContent
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            LyricLine(
                timeUnits = listOf(LyricTimeUnit(text = line, startTime = "00:00.000", endTime = "00:00.000")),
                translation = ""
            )
        }
}

private fun trimLeadingSpacesForPreviewWords(words: List<NewPreviewLyricWord>): List<NewPreviewLyricWord> {
    if (words.isEmpty()) return words
    val result = mutableListOf<NewPreviewLyricWord>()
    var foundFirstVisibleWord = false

    for (word in words) {
        if (foundFirstVisibleWord) {
            result.add(word)
            continue
        }

        if (word.text.isEmpty()) continue
        if (word.text.all { it == ' ' }) continue

        if (word.text.startsWith(" ")) {
            val trimmedText = word.text.trimStart()
            if (trimmedText.isEmpty()) continue

            val removedCount = word.text.length - trimmedText.length
            val shiftedCharTransliterations = if (word.charTransliterations.isEmpty()) {
                emptyMap()
            } else {
                word.charTransliterations.entries
                    .mapNotNull { (index, value) ->
                        val newIndex = index - removedCount
                        if (newIndex >= 0) newIndex to value else null
                    }
                    .toMap()
            }

            val resolvedTransliteration = if (trimmedText.length == 1 && shiftedCharTransliterations.isNotEmpty()) {
                shiftedCharTransliterations[0] ?: word.transliteration
            } else {
                word.transliteration
            }

            result.add(
                word.copy(
                    text = trimmedText,
                    transliteration = resolvedTransliteration,
                    charTransliterations = shiftedCharTransliterations
                )
            )
            foundFirstVisibleWord = true
            continue
        }

        result.add(word)
        foundFirstVisibleWord = true
    }

    return result
}

private fun convertToPreviewLyricLines(lines: List<LyricLine>): List<NewPreviewLyricLine> {
    return lines.mapNotNull { line ->
        if (line.timeUnits.isEmpty()) {
            return@mapNotNull null
        }

        val expandedWords = if (line.timeUnits.size == 1) {
            // 和 LyricTimingActivity 的预览逻辑保持一致：仅一个 timeUnit 时保留逐行形态
            val unit = line.timeUnits.first()
            val beginMs = parseTimeToMs(unit.startTime)
            val endMs = parseTimeToMs(unit.endTime)
            // 保留 end=0 的语义（用于预览页抑制间奏行）；仅在其他异常值时做兜底
            val normalizedEndMs = when {
                endMs == 0L -> 0L
                endMs >= beginMs -> endMs
                else -> beginMs
            }
            listOf(
                NewPreviewLyricWord(
                    text = unit.text,
                    begin = beginMs,
                    end = normalizedEndMs,
                    transliteration = unit.transliteration,
                    charTransliterations = unit.charTransliterations
                )
            )
        } else {
            // 多个 timeUnit 时按字符拆分并均分非空格字符时长（与 LyricTimingActivity 一致）
            line.timeUnits.flatMap { unit ->
                val beginMs = parseTimeToMs(unit.startTime)
                val endMs = parseTimeToMs(unit.endTime)
                val safeEndMs = if (endMs >= beginMs) endMs else beginMs
                val duration = safeEndMs - beginMs
                val text = unit.text

                if (text.isEmpty()) {
                    emptyList()
                } else if (text.length == 1) {
                    listOf(
                        NewPreviewLyricWord(
                            text = text,
                            begin = beginMs,
                            end = safeEndMs,
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
                            NewPreviewLyricWord(
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
                                NewPreviewLyricWord(
                                    text = char.toString(),
                                    begin = currentTime,
                                    end = currentTime,
                                    transliteration = "",
                                    charTransliterations = emptyMap()
                                )
                            } else {
                                val charBegin = currentTime
                                val charEnd = if (currentTime + charDuration >= safeEndMs) safeEndMs else currentTime + charDuration
                                currentTime = charEnd
                                val transliteration = if (unit.charTransliterations.isNotEmpty()) {
                                    unit.charTransliterations[textIndex] ?: ""
                                } else {
                                    ""
                                }
                                textIndex++
                                NewPreviewLyricWord(
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

        val words = trimLeadingSpacesForPreviewWords(expandedWords).filter { it.text.isNotEmpty() }

        if (words.isEmpty()) {
            return@mapNotNull null
        }

        NewPreviewLyricLine(
            words = words,
            translation = line.translation,
            isDuet = line.agentType == LyricAgentType.RIGHT,
            isBackground = line.agentType == LyricAgentType.BACKGROUND
        )
    }
}

private fun buildPreviewLyricPayload(
    context: Context,
    audioPath: String,
    mediaStoreId: Long = -1L
): PreviewLyricPayload? {
    val audioFile = File(audioPath)
    val sameNameTtml = audioFile.parentFile?.let { parent ->
        File(parent, "${audioFile.nameWithoutExtension}.ttml")
    }

    val preferredLyrics = run {
        val externalTtmlLyrics = if (sameNameTtml != null) {
            try {
                readExternalTtmlWithFallback(context, sameNameTtml)
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error reading preferred TTML lyric file", e)
                null
            }
        } else {
            null
        }
        externalTtmlLyrics ?: extractEmbeddedLyrics(context, audioPath, mediaStoreId)?.takeIf { it.isNotBlank() }
    } ?: return null

    val detectedFormat = detectLyricsFormat(preferredLyrics)
    Log.d("MusicLibrary", "buildPreviewLyricPayload: detectedFormat=$detectedFormat, path=$audioPath")

    val parseResult = when (detectedFormat) {
        1 -> LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, preferredLyrics)
        2 -> LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, preferredLyrics)
        3 -> LyricParsingUtils.parseByType(LyricParseType.TTML, preferredLyrics)
        else -> LyricParseResult(
            lyrics = preferredLyrics.lines().filter { it.isNotBlank() },
            lyricLines = buildPlainTextLyricLines(preferredLyrics)
        )
    }

    val fallbackLines = buildPlainTextLyricLines(preferredLyrics)
    val parsedLines = if (parseResult.lyricLines.isNotEmpty()) parseResult.lyricLines else fallbackLines
    val previewLines = convertToPreviewLyricLines(parsedLines)
    if (previewLines.isEmpty()) {
        return null
    }

    val creators = if (detectedFormat == 3) {
        LyricParsingUtils.parseSongwritersFromTtml(preferredLyrics)
    } else {
        emptyList()
    }

    return PreviewLyricPayload(lines = previewLines, creators = creators)
}

enum class SortType(@StringRes val displayNameRes: Int) {
    FILE_NAME(R.string.sort_file_name),
    MODIFY_TIME(R.string.sort_modify_time),
    ADD_TIME(R.string.sort_add_time),
    YEAR(R.string.sort_year)
}

enum class SortOrder(@StringRes val displayNameRes: Int) {
    ASC(R.string.sort_order_asc),
    DESC(R.string.sort_order_desc)
}

private enum class MusicLibraryInternalPage {
    SONGS,
    ARTISTS,
    ARTIST_DETAIL,
    ALBUM_DETAIL,
    SETTINGS,
    ABOUT
}

enum class FieldMatchMode {
    SUPPLEMENT, OVERWRITE
}

data class BatchMatchField(
    val key: String,
    val label: String,
    val enabled: Boolean = true,
    val mode: FieldMatchMode = FieldMatchMode.SUPPLEMENT
)

data class BatchMatchConfig(
    val fields: List<BatchMatchField> = listOf(
        BatchMatchField("cover", "封面"),
        BatchMatchField("title", "标题"),
        BatchMatchField("artist", "艺术家"),
        BatchMatchField("album", "专辑"),
        BatchMatchField("year", "年份"),
        BatchMatchField("trackNumber", "音轨号"),
        BatchMatchField("discNumber", "碟号"),
        BatchMatchField("genre", "风格"),
        BatchMatchField("albumArtist", "专辑艺术家"),
        BatchMatchField("composer", "作曲"),
        BatchMatchField("lyricist", "作词"),
        BatchMatchField("comment", "注释"),
        BatchMatchField("copyrightInfo", "版权信息")
    ),
    val sources: List<Source> = listOf(Source.ITUNES, Source.QM, Source.NE),
    val threadCount: Int = 3
)

data class BatchMatchItem(
    val audioFile: AudioFile,
    var originalData: Map<String, String>,
    var originalCoverBitmap: android.graphics.Bitmap? = null,
    var originalCoverData: com.lonx.audiotag.model.AudioPicture? = null,
    var matchedData: Map<String, String> = emptyMap(),
    var matchStatus: MatchStatus = MatchStatus.PENDING,
    var matchSource: Source? = null,
    var coverBitmap: android.graphics.Bitmap? = null,
    var similarityScore: Float = 0f,
    var error: String? = null,
    var hasOriginalCover: Boolean = false
) {
    val displayTitle get() = audioFile.displayTitle
    val displayArtist get() = audioFile.displayArtist
    val path get() = audioFile.path
}

enum class MatchStatus {
    PENDING, MATCHING, SUCCESS, FAILED, SKIPPED
}

data class BatchMatchResult(
    val items: List<BatchMatchItem>,
    val totalMatched: Int,
    val totalSuccess: Int,
    val totalFailed: Int
)

enum class LyricMatchMode {
    SUPPLEMENT, OVERWRITE
}

enum class LyricType {
    VERBATIM, LINE
}

data class BatchLyricMatchConfig(
    val sources: List<Source> = listOf(Source.QM, Source.NE, Source.KG),
    val mode: LyricMatchMode = LyricMatchMode.SUPPLEMENT,
    val lyricType: LyricType = LyricType.VERBATIM,
    val threadCount: Int = 3,
    val filterMetadata: Boolean = false,
    val includeTranslation: Boolean = false
)

data class BatchLyricMatchItem(
    val audioFile: AudioFile,
    var originalLyrics: String? = null,
    var matchedLyrics: String? = null,
    var matchedLyricsLrc: String? = null,
    var isVerbatimLyrics: Boolean = false,
    var lyricType: LyricType = LyricType.VERBATIM,
    var matchStatus: MatchStatus = MatchStatus.PENDING,
    var matchSource: Source? = null,
    var similarityScore: Float = 0f,
    var error: String? = null
) {
    val displayTitle get() = audioFile.displayTitle
    val displayArtist get() = audioFile.displayArtist
    val path get() = audioFile.path
}

data class BatchLyricMatchResult(
    val items: List<BatchLyricMatchItem>,
    val totalMatched: Int,
    val totalSuccess: Int,
    val totalFailed: Int
)

private enum class BatchLyricsTargetFormat(
    @StringRes val labelRes: Int,
    val extension: String,
    val exportFormat: LyricExportFormat
) {
    LRC_WORD(R.string.ml_music_library_lrc_word, ".lrc", LyricExportFormat.LRC_WORD),
    LRC_LINE(R.string.ml_music_library_lrc_line, ".lrc", LyricExportFormat.LRC_LINE),
    ELRC(R.string.ml_music_library_elrc, ".elrc", LyricExportFormat.ENHANCED_LRC),
    TTML(R.string.ml_music_library_ttml, ".ttml", LyricExportFormat.TTML);

    val mimeType: String
        get() = if (this == TTML) "application/ttml+xml" else "text/plain"
}

private data class BatchLyricsExternalExportConfig(
    val preferSameNameTtml: Boolean = true,
    val targetFormat: BatchLyricsTargetFormat = BatchLyricsTargetFormat.LRC_WORD,
    val useCustomDirectory: Boolean = false,
    val customDirectoryUri: Uri? = null
)

private data class BatchLyricsOperationResult(
    val total: Int,
    val successCount: Int,
    val failedMessages: List<String>
) {
    val failedCount: Int
        get() = failedMessages.size
}

data class RenameConfig(
    val template: String = "[歌曲标题] - [艺术家]",
    val renameTtml: Boolean = true,
    val artistSeparator: String = "／"
)

data class RenamePreviewItem(
    val audioFile: AudioFile,
    val oldName: String,
    val newName: String,
    val oldTtmlName: String? = null,
    val newTtmlName: String? = null
)

data class RenameResult(
    val items: List<RenamePreviewItem>,
    val successCount: Int,
    val failedCount: Int,
    val errors: Map<String, String>
)

data class ScanSummary(
    val totalCount: Int = 0,
    val addedCount: Int = 0,
    val removedCount: Int = 0,
    val updatedCount: Int = 0
)

private data class StartupUpdateNoticeState(
    val updateInfo: UpdateInfo? = null,
    val noticeText: String? = null,
    val showNotice: Boolean = false
)

class MusicLibraryActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(newBase))
    }

    
    private var piracyCheckResult by mutableStateOf<PiracyCheckResult?>(null)
    private var showPiracyWarning by mutableStateOf(false)
    private var externalAudioFile by mutableStateOf<AudioFile?>(null)
    private var editingMetadataPath by mutableStateOf<String?>(null)
    private var editingMetadataMediaStoreId by mutableStateOf(-1L)
    private var _refreshMetadataPath by mutableStateOf<String?>(null)
    private var initialSearchQueryState by mutableStateOf("")
    private var initialSearchRequestState by mutableIntStateOf(0)
    private var initialArtistNameState by mutableStateOf("")
    private var initialAlbumNameState by mutableStateOf("")
    private var initialLibraryPageRequestState by mutableIntStateOf(0)
    private var inlinePreviewRequestState by mutableIntStateOf(0)
    
    companion object {
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val EXTRA_INITIAL_SEARCH_QUERY = "initial_search_query"
        const val EXTRA_INITIAL_ARTIST_NAME = "initial_artist_name"
        const val EXTRA_INITIAL_ALBUM_NAME = "initial_album_name"
        const val EXTRA_OPEN_INLINE_PREVIEW = "open_inline_preview"
        const val REQUEST_CODE_EDIT_METADATA = 200
        
        private val coverCache = LruCache<String, Bitmap>(50).apply {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            val cacheSize = maxMemory / 8
            resize(cacheSize)
        }
        private val noCoverCachePaths = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        
        fun getCoverFromCache(cachePath: String): Bitmap? = coverCache.get(cachePath)
        fun putCoverToCache(cachePath: String, bitmap: Bitmap) {
            clearNoCoverMark(cachePath)
            coverCache.put(cachePath, bitmap)
        }
        fun removeCoverFromCache(cachePath: String) {
            coverCache.remove(cachePath)
            clearNoCoverMark(cachePath)
        }
        fun isNoCoverMarked(cachePath: String): Boolean = noCoverCachePaths.contains(cachePath)
        fun markNoCover(cachePath: String) {
            noCoverCachePaths.add(cachePath)
        }
        fun clearNoCoverMark(cachePath: String) {
            noCoverCachePaths.remove(cachePath)
        }
        fun retainNoCoverMarks(validPaths: Set<String>) {
            synchronized(noCoverCachePaths) {
                noCoverCachePaths.retainAll(validPaths)
            }
        }
        
        fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val (height, width) = options.outHeight to options.outWidth
            var inSampleSize = 1
            
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                
                while (halfHeight / inSampleSize >= reqHeight
                    && halfWidth / inSampleSize >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureStorage.initializeIfNeeded(this)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
                    .build()
            )
        }
        enableEdgeToEdge()
        applyLyricEdgeToEdgeWindowCompat()
        
        val checkResult = PiracyChecker.checkAll(this)
        if (checkResult.isPirated) {
            piracyCheckResult = checkResult
            showPiracyWarning = true
        }
        
        // 检查是否从第三方应用调用
        val externalPath = handleExternalIntent(intent)
        
        val prefs = getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE)
        initialSearchQueryState = intent.getStringExtra(EXTRA_INITIAL_SEARCH_QUERY).orEmpty()
        if (initialSearchQueryState.isNotBlank()) {
            initialSearchRequestState += 1
        }
        applyInitialLibraryPageIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_INLINE_PREVIEW, false)) {
            inlinePreviewRequestState += 1
        }
        val hasSetup = prefs.getBoolean("hasSetup", false)
        
        // 如果是第三方调用且没有设置过，直接加载音频文件
        if (externalPath != null) {
            loadExternalAudioFile(externalPath)
        } else if (!hasSetup) {
            val intent = Intent(this, MusicLibrarySettingsActivity::class.java)
            intent.putExtra("isFirstSetup", true)
            startActivity(intent)
            finish()
            return
        }
        
        setContent {
            歌词转换Theme {
                val context = LocalContext.current
                var startupUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                var startupNoticeText by remember { mutableStateOf<String?>(null) }
                var showStartupNoticeDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(800)
                    val startupState = fetchStartupUpdateNoticeState(context)
                    startupUpdateInfo = startupState.updateInfo

                    val canShowNotice = startupState.showNotice &&
                        !startupState.noticeText.isNullOrBlank() &&
                        !isNoticeSnoozedToday(context)
                    if (canShowNotice) {
                        startupNoticeText = startupState.noticeText
                        showStartupNoticeDialog = true
                    }
                }

                LaunchedEffect(startupUpdateInfo) {
                    val info = startupUpdateInfo ?: return@LaunchedEffect
                    val intent = Intent(context, UpdateActivity::class.java).apply {
                        putExtra("updateInfo", info)
                    }
                    context.startActivity(intent)
                    startupUpdateInfo = null
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0)
                ) { paddingValues ->
                    if (externalAudioFile != null) {
                        ExternalAudioScreen(
                            audio = externalAudioFile!!,
                            onBack = { finish() },
                            onEditLyrics = { lyricsContent, format, finishAfterNavigate ->
                                val intent = Intent(this, LyricTimingActivity::class.java).apply {
                                    putExtra("audioPath", externalAudioFile!!.path)
                                    putExtra("lyricsContent", lyricsContent)
                                    putExtra("sourceTitle", externalAudioFile!!.displayTitle)
                                    putExtra("sourceArtist", externalAudioFile!!.artist)
                                    putExtra("lyricsFormat", format)
                                    putExtra(LyricTimingActivity.EXTRA_MEDIA_STORE_ID, externalAudioFile!!.mediaStoreId)
                                }
                                startActivity(intent)
                                if (finishAfterNavigate) {
                                    finish()
                                }
                            },
                            onEditMetadata = { path, finishAfterNavigate ->
                                val intent = Intent(this, SongMetadataEditActivity::class.java).apply {
                                    putExtra(SongMetadataEditActivity.EXTRA_AUDIO_PATH, path)
                                    putExtra(SongMetadataEditActivity.EXTRA_MEDIA_STORE_ID, externalAudioFile!!.mediaStoreId)
                                }
                                startActivity(intent)
                                if (finishAfterNavigate) {
                                    finish()
                                }
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    } else {
                        MusicLibraryScreen(
                            initialSearchQuery = initialSearchQueryState,
                            initialSearchRequestVersion = initialSearchRequestState,
                            initialArtistName = initialArtistNameState,
                            initialAlbumName = initialAlbumNameState,
                            initialLibraryPageRequestVersion = initialLibraryPageRequestState,
                            initialInlinePreviewRequestVersion = inlinePreviewRequestState,
                            onBack = { finish() },
                            onOpenSettings = {
                                startActivityForResult(Intent(this, MusicLibrarySettingsActivity::class.java), 100)
                            },
                            onEditMetadata = { path ->
                                editingMetadataPath = path
                                editingMetadataMediaStoreId =
                                    externalAudioFile?.takeIf { it.path == path }?.mediaStoreId ?: -1L
                                val intent = Intent(this, SongMetadataEditActivity::class.java).apply {
                                    putExtra(SongMetadataEditActivity.EXTRA_AUDIO_PATH, path)
                                    putExtra(SongMetadataEditActivity.EXTRA_MEDIA_STORE_ID, editingMetadataMediaStoreId)
                                }
                                startActivityForResult(intent, REQUEST_CODE_EDIT_METADATA)
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }

                if (showStartupNoticeDialog && !startupNoticeText.isNullOrBlank()) {
                    AlertDialog(
                        onDismissRequest = { showStartupNoticeDialog = false },
                        title = {
                            Text(text = stringResource(R.string.ml_music_library_notice_title))
                        },
                        text = {
                            Text(text = startupNoticeText!!)
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    markNoticeSnoozedToday(context)
                                    showStartupNoticeDialog = false
                                }
                            ) {
                                Text(stringResource(R.string.ml_music_library_notice_snooze_today))
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showStartupNoticeDialog = false
                                }
                            ) {
                                Text(stringResource(R.string.ml_music_library_received))
                            }
                        }
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
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it)
            logExternal("onNewIntent: action=${it.action}, data=${it.data}")
            if (it.hasExtra(EXTRA_INITIAL_SEARCH_QUERY)) {
                val incomingSearch = it.getStringExtra(EXTRA_INITIAL_SEARCH_QUERY).orEmpty()
                initialSearchQueryState = incomingSearch
                initialSearchRequestState += 1
            }
            applyInitialLibraryPageIntent(it)
            if (it.getBooleanExtra(EXTRA_OPEN_INLINE_PREVIEW, false)) {
                inlinePreviewRequestState += 1
            }
            val externalPath = handleExternalIntent(it)
            if (externalPath != null) {
                loadExternalAudioFile(externalPath)
            }
        }
    }

    private fun applyInitialLibraryPageIntent(intent: Intent) {
        if (!intent.hasExtra(EXTRA_INITIAL_ARTIST_NAME) && !intent.hasExtra(EXTRA_INITIAL_ALBUM_NAME)) {
            return
        }
        initialArtistNameState = intent.getStringExtra(EXTRA_INITIAL_ARTIST_NAME).orEmpty()
        initialAlbumNameState = intent.getStringExtra(EXTRA_INITIAL_ALBUM_NAME).orEmpty()
        if (initialArtistNameState.isNotBlank() || initialAlbumNameState.isNotBlank()) {
            initialLibraryPageRequestState += 1
        }
    }
    
    private fun handleExternalIntent(intent: Intent): String? {
        logExternal(
            "handleExternalIntent start: action=${intent.action}, type=${intent.type}, data=${intent.data}, hasExtraPath=${intent.hasExtra(EXTRA_AUDIO_PATH)}"
        )
        // 检查是否有直接传入的路径
        val directPath = intent.getStringExtra(EXTRA_AUDIO_PATH)
        if (directPath != null && File(directPath).exists()) {
            logExternal("hit direct extra path: $directPath")
            return directPath
        }
        if (!directPath.isNullOrBlank()) {
            logExternal("direct extra path not exists: $directPath")
        }
        
        // 处理 ACTION_SEND
        if (intent.action == Intent.ACTION_SEND) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null) {
                logExternal("ACTION_SEND uri=${describeExternalUri(uri)}")
                val resolved = getRealPathFromUri(uri)
                logExternal("ACTION_SEND resolved=$resolved")
                return resolved
            }
            logExternal("ACTION_SEND has no EXTRA_STREAM uri")
        }
        
        // 处理 ACTION_SEND_MULTIPLE（只取第一个）
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            if (uris != null && uris.isNotEmpty()) {
                logExternal("ACTION_SEND_MULTIPLE count=${uris.size}, first=${describeExternalUri(uris[0])}")
                val resolved = getRealPathFromUri(uris[0])
                logExternal("ACTION_SEND_MULTIPLE resolved=$resolved")
                return resolved
            }
            logExternal("ACTION_SEND_MULTIPLE has empty EXTRA_STREAM")
        }
        
        // 检查 Intent 的 data（用于 ACTION_VIEW 和 ACTION_EDIT）
        val uri = intent.data ?: return null
        logExternal("fallback intent.data uri=${describeExternalUri(uri)}")
        
        // 尝试从 Uri 获取真实路径
        val resolved = getRealPathFromUri(uri)
        logExternal("intent.data resolved=$resolved")
        return resolved
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        logExternal("getRealPathFromUri start: ${describeExternalUri(uri)}")
        // file:// 直接取本地路径
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val filePath = uri.path
            if (isExistingFilePath(filePath)) {
                logExternal("resolved by file scheme path: $filePath")
                return filePath
            }
            logExternal("file scheme path not exists: $filePath")
        }

        // 直接 path（部分机型 ACTION_VIEW 会给出）
        uri.path?.let { rawPath ->
            val decodedPath = Uri.decode(rawPath)
            if (isExistingFilePath(decodedPath)) {
                logExternal("resolved by decoded raw path: $decodedPath")
                return decodedPath
            }
            logExternal("decoded raw path not exists: $decodedPath")
        }

        if (!uri.scheme.equals("content", ignoreCase = true)) {
            logExternal("unsupported scheme for resolution: ${uri.scheme}")
            return null
        }

        resolvePathFromFileProviderUri(uri)?.let { providerDerivedPath ->
            if (isExistingFilePath(providerDerivedPath)) {
                logExternal("resolved by FileProvider derived path: $providerDerivedPath")
                return providerDerivedPath
            }
            logExternal("FileProvider derived candidate not exists: $providerDerivedPath")
        }

        // 优先处理 DocumentUri（更接近真实路径）
        resolvePathFromDocumentUri(uri)?.let { documentPath ->
            if (isExistingFilePath(documentPath)) {
                logExternal("resolved by DocumentUri: $documentPath")
                return documentPath
            }
            logExternal("DocumentUri candidate not exists: $documentPath")
        }

        // 兜底查询
        getPathFromDataColumn(uri)?.let { dataPath ->
            if (isExistingFilePath(dataPath)) {
                logExternal("resolved by _data column: $dataPath")
                return dataPath
            }
            logExternal("_data candidate not exists: $dataPath")
        }
        getPathFromMediaStore(uri)?.let { mediaPath ->
            if (isExistingFilePath(mediaPath)) {
                logExternal("resolved by MediaStore lookup: $mediaPath")
                return mediaPath
            }
            logExternal("MediaStore candidate not exists: $mediaPath")
        }
        getPathFromDocumentsProvider(uri)?.let { providerPath ->
            if (isExistingFilePath(providerPath)) {
                logExternal("resolved by legacy DocumentsProvider: $providerPath")
                return providerPath
            }
            logExternal("legacy DocumentsProvider candidate not exists: $providerPath")
        }

        // 解析不到真实路径时，最后兜底复制到缓存，保证可继续处理
        Log.w("MusicLibrary", "无法获取外部音频真实路径，回退到缓存文件: $uri")
        val fallbackPath = copyContentUriToCache(uri)
        logExternal("fallback cache copy path=$fallbackPath")
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
            logExternal(
                "resolvePathFromFileProviderUri unsupported root authority=$authority root=$rootName senderPkg=$senderPackage relative=$relativePath"
            )
            return null
        }
        candidates.forEach { candidate ->
            val exists = isExistingFilePath(candidate)
            logExternal(
                "resolvePathFromFileProviderUri candidate authority=$authority root=$rootName senderPkg=$senderPackage relative=$relativePath candidate=$candidate exists=$exists"
            )
            if (exists) {
                return candidate
            }
        }
        return null
    }

    private fun resolvePathFromDocumentUri(uri: Uri): String? {
        return try {
            if (!android.provider.DocumentsContract.isDocumentUri(this, uri)) {
                logExternal("not a DocumentUri: ${describeExternalUri(uri)}")
                return null
            }
            val docId = android.provider.DocumentsContract.getDocumentId(uri)
            logExternal("DocumentUri authority=${uri.authority}, docId=$docId")
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
                    logExternal("externalstorage candidate=$candidate")
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
                                logExternal("downloads try my_downloads uri=${describeExternalUri(myUri)}")
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
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    } ?: return null
                    val selection = "${MediaStore.MediaColumns._ID}=?"
                    val selectionArgs = arrayOf(id)
                    logExternal("media document type=$type id=$id contentUri=$contentUri")
                    queryDataColumn(contentUri, selection, selectionArgs)
                }
                else -> null
            }
        } catch (e: Exception) {
            logExternal("resolvePathFromDocumentUri exception: ${e.message}")
            Log.e("MusicLibrary", "Error resolving document uri path: $uri", e)
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
            logExternal("queryDataColumn uri=${describeExternalUri(uri)}, selection=$selection, args=${selectionArgs?.joinToString()}")
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (columnIndex >= 0) {
                        val value = cursor.getString(columnIndex)
                        logExternal("queryDataColumn result=$value")
                        value
                    } else null
                } else null
            }
        } catch (e: Exception) {
            logExternal("queryDataColumn exception: ${e.message}")
            null
        }
    }

    private fun copyContentUriToCache(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = getFileName(uri)?.replace('/', '_')?.replace('\\', '_')
                    ?: "temp_audio_${System.currentTimeMillis()}"
                val tempFile = File(cacheDir, fileName)
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                logExternal("copyContentUriToCache wrote=${tempFile.absolutePath}")
                tempFile.absolutePath
            }
        } catch (e: Exception) {
            logExternal("copyContentUriToCache exception: ${e.message}")
            Log.e("MusicLibrary", "Error copying file from content URI", e)
            null
        }
    }

    private fun getPathFromDataColumn(uri: Uri): String? {
        return try {
            val projection = arrayOf("_data")
            logExternal("getPathFromDataColumn uri=${describeExternalUri(uri)}")
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow("_data")
                    val value = it.getString(columnIndex)
                    logExternal("getPathFromDataColumn result=$value")
                    value
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logExternal("getPathFromDataColumn exception: ${e.message}")
            Log.e("MusicLibrary", "Error getting path from _data column", e)
            null
        }
    }

    private fun getPathFromMediaStore(uri: Uri): String? {
        return try {
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media.DATA
            )
            val selection = "${android.provider.MediaStore.Audio.Media._ID} = ?"
            val id = uri.lastPathSegment?.toLongOrNull() ?: return null
            val selectionArgs = arrayOf(id.toString())
            logExternal("getPathFromMediaStore uri=${describeExternalUri(uri)}, id=$id")
            val cursor = contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    val value = it.getString(columnIndex)
                    logExternal("getPathFromMediaStore result=$value")
                    value
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logExternal("getPathFromMediaStore exception: ${e.message}")
            Log.e("MusicLibrary", "Error getting path from MediaStore", e)
            null
        }
    }

    private fun getPathFromDocumentsProvider(uri: Uri): String? {
        return try {
            // 检查是否是 DocumentsProvider URI
            if ("com.android.externalstorage.documents" == uri.authority) {
                val docId = android.provider.DocumentsContract.getDocumentId(uri)
                logExternal("legacy getPathFromDocumentsProvider docId=$docId")
                val split = docId.split(":", limit = 2)
                val type = split[0]
                val relativePath = split.getOrNull(1) ?: return null
                if ("primary".equals(type, ignoreCase = true)) {
                    return android.os.Environment.getExternalStorageDirectory().toString() + "/" + relativePath
                }
                return "/storage/$type/$relativePath"
            }
            null
        } catch (e: Exception) {
            logExternal("getPathFromDocumentsProvider exception: ${e.message}")
            Log.e("MusicLibrary", "Error getting path from DocumentsProvider", e)
            null
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }
    
    private fun loadExternalAudioFile(path: String) {
        Thread {
            try {
                logExternal("loadExternalAudioFile start path=$path")
                val file = File(path)
                if (!file.exists()) {
                    logExternal("loadExternalAudioFile path not exists: $path")
                    return@Thread
                }
                
                val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(this, path)
                
                // 保存封面到缓存
                var coverCachePath: String? = null
                if (metadata.cover != null) {
                    coverCachePath = saveCoverToCache(this, path, metadata.cover)
                }
                
                val audioFile = AudioFile(
                    path = path,
                    title = metadata.title,
                    artist = metadata.artist,
                    album = metadata.album,
                    duration = metadata.duration,
                    fileSize = file.length(),
                    lastModified = file.lastModified(),
                    addedTime = System.currentTimeMillis(),
                    coverCachePath = coverCachePath
                )
                
                runOnUiThread {
                    externalAudioFile = audioFile
                }
                logExternal("loadExternalAudioFile success title=${audioFile.displayTitle}, artist=${audioFile.displayArtist}")
            } catch (e: Exception) {
                logExternal("loadExternalAudioFile exception: ${e.message}")
                Log.e("MusicLibrary", "Error loading external audio file", e)
            }
        }.start()
    }

    private fun describeExternalUri(uri: Uri): String {
        val docId = runCatching {
            if (android.provider.DocumentsContract.isDocumentUri(this, uri)) {
                android.provider.DocumentsContract.getDocumentId(uri)
            } else {
                null
            }
        }.getOrNull()
        return "uri=$uri, scheme=${uri.scheme}, authority=${uri.authority}, path=${uri.path}, lastSegment=${uri.lastPathSegment}, docId=$docId"
    }

    private fun logExternal(message: String) {
        Log.d(EXTERNAL_AUDIO_LOG_TAG, message)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            recreate()
        } else if (requestCode == REQUEST_CODE_EDIT_METADATA) {
            val returnedPath = data?.getStringExtra(SongMetadataEditActivity.EXTRA_AUDIO_PATH)
            _refreshMetadataPath = when {
                !returnedPath.isNullOrBlank() -> returnedPath
                !editingMetadataPath.isNullOrBlank() -> editingMetadataPath
                else -> null
            }
            editingMetadataPath = null
        }
    }
    
    fun getRefreshMetadataPath(): String? {
        val path = _refreshMetadataPath
        _refreshMetadataPath = null
        return path
    }
}

private suspend fun fetchStartupUpdateNoticeState(context: Context): StartupUpdateNoticeState {
    return withContext(Dispatchers.IO) {
        val result = UpdateChecker.checkForUpdate(context)
        val updateInfo = (result as? UpdateResult.UpdateAvailable)?.updateInfo

        val infoForNotice = when (result) {
            is UpdateResult.UpdateAvailable -> result.updateInfo
            is UpdateResult.NoUpdate -> fetchRemoteUpdateInfo()
            else -> null
        }

        if (infoForNotice != null) {
            updateAppSettingsFromUpdateInfo(context, infoForNotice)
        }

        StartupUpdateNoticeState(
            updateInfo = updateInfo,
            noticeText = infoForNotice?.notice,
            showNotice = infoForNotice?.showNotice == true
        )
    }
}

private fun fetchRemoteUpdateInfo(): UpdateInfo? {
    return try {
        val url = java.net.URL(UpdateChecker.UPDATE_URL)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val json = JSONObject(response)
        UpdateInfo(
            versionCode = json.optInt("versionCode", 0),
            versionName = json.optString("versionName", ""),
            changelog = json.optString("changelog", ""),
            updateTime = json.optString("updateTime", ""),
            downloadName1 = json.optString("downloadName1", null).takeIf { it.isNotEmpty() },
            downloadName2 = json.optString("downloadName2", null).takeIf { it.isNotEmpty() },
            downloadUrl1 = json.optString("downloadUrl1", null).takeIf { it.isNotEmpty() },
            downloadUrl2 = json.optString("downloadUrl2", null).takeIf { it.isNotEmpty() },
            notice = json.optString("notice", null).takeIf { it.isNotEmpty() },
            showNotice = json.optBoolean("showNotice", false),
            amUrl = json.optString("AMURL", null).takeIf { it.isNotEmpty() },
            amUrlName = json.optString("AMURLname", null).takeIf { it.isNotEmpty() },
            amUrlCountry = json.optString("AMURLcountry", null).takeIf { it.isNotEmpty() },
            amUrlNameContributor = json.optString("AMURLname_contributor", null).takeIf { it.isNotEmpty() },
            amUrlCountryContributor = json.optString("AMURLcountry_contributor", null).takeIf { it.isNotEmpty() },
            noticeContributor = json.optString("Notice_contributor", null).takeIf { it.isNotEmpty() }
        )
    } catch (_: Exception) {
        null
    }
}

private fun updateAppSettingsFromUpdateInfo(context: Context, info: UpdateInfo) {
    val prefs = context.getSharedPreferences(STARTUP_APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        info.amUrl?.let { putString("amUrl", it) }
        info.amUrlName?.let { putString("amUrlName", it) }
        info.amUrlCountry?.let { putString("amUrlCountry", it) }
        info.amUrlNameContributor?.let { putString("amUrlNameContributor", it) }
        info.amUrlCountryContributor?.let { putString("amUrlCountryContributor", it) }
        info.noticeContributor?.let { putString("noticeContributor", it) }
        apply()
    }
}

private fun isNoticeSnoozedToday(context: Context): Boolean {
    val prefs = context.getSharedPreferences(STARTUP_APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(STARTUP_NOTICE_SNOOZE_DATE_KEY, null) == currentDateToken()
}

private fun markNoticeSnoozedToday(context: Context) {
    val prefs = context.getSharedPreferences(STARTUP_APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(STARTUP_NOTICE_SNOOZE_DATE_KEY, currentDateToken()).apply()
}

private fun currentDateToken(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicLibraryScreen(
    initialSearchQuery: String = "",
    initialSearchRequestVersion: Int = 0,
    initialArtistName: String = "",
    initialAlbumName: String = "",
    initialLibraryPageRequestVersion: Int = 0,
    initialInlinePreviewRequestVersion: Int = 0,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditMetadata: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val lyricPreviewPrefs = remember {
        context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var songClickAction by remember {
        mutableStateOf(prefs.getString("songClickAction", "") ?: "")
    }
    var hasConfirmedSongClickAction by remember {
        mutableStateOf(prefs.getBoolean(PREF_SONG_CLICK_ACTION_CONFIRMED, false))
    }
    var autoDetectEmbeddedLyricsType by remember {
        mutableStateOf(prefs.getBoolean("autoDetectEmbeddedLyricsType", false))
    }
    val scope = rememberCoroutineScope()
    val playbackController = rememberMusicPlaybackController()
    var miniPlayerManuallyHidden by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_MINI_PLAYER_MANUALLY_HIDDEN, false))
    }
    val miniPlayerVisible = playbackController.hasCurrentItem && !miniPlayerManuallyHidden
    val currentPlayingAudioPath = playbackController.currentAudioPath?.takeIf { it.isNotBlank() }
    val canLocatePlayingSong = currentPlayingAudioPath != null
    val miniPlayerHeight = 72.dp
    var miniPlayerExpandProgress by rememberSaveable { mutableStateOf(0f) }
    var miniPlayerSettleJob by remember { mutableStateOf<Job?>(null) }
    var miniPlayerLastDragDeltaY by remember { mutableStateOf(0f) }
    var lastAppliedInlinePreviewRequest by remember { mutableIntStateOf(0) }
    var showInlineLyricPreview by rememberSaveable { mutableStateOf(false) }
    var inlineLyricDisplaySelected by rememberSaveable {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_LYRIC_DISPLAY_SELECTED,
                LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_SELECTED
            )
        )
    }
    var inlineLyricDisplayResetToken by rememberSaveable { mutableStateOf(0) }
    var lyricPreviewLines by remember { mutableStateOf<List<NewPreviewLyricLine>>(emptyList()) }
    var lyricPreviewCreators by remember { mutableStateOf<List<String>>(emptyList()) }
    var lyricPreviewLoading by remember { mutableStateOf(false) }
    var lyricPreviewLoadedPath by remember { mutableStateOf<String?>(null) }
    var inlineMetadataRefreshToken by remember { mutableIntStateOf(0) }
    var inlineCompanionAudioPath by remember { mutableStateOf<String?>(null) }
    var inlineCompanionSwitching by remember { mutableStateOf(false) }
    var miniPlayerCoverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var miniPlayerBackgroundMode by remember { mutableStateOf(getSavedMiniPlayerBackgroundMode(context)) }
    var lyricPageBackgroundModeForMiniPlayer by remember {
        mutableStateOf(getSavedLyricPageBackgroundModeForMiniPlayer(context))
    }
    var miniPlayerLandscapeAlignment by remember {
        mutableStateOf(getSavedMiniPlayerLandscapeAlignment(context))
    }
    var miniBarBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var miniBarBaseTop by remember { mutableStateOf<Float?>(null) }
    val sharedCoverId = "music_cover"
    val miniPlayerBackdrop = rememberLayerBackdrop()
    val internalPageBackdrop = rememberLayerBackdrop()
    val combinedMiniPlayerBackdrop = rememberCombinedBackdrop(miniPlayerBackdrop, internalPageBackdrop)

    fun refreshMiniPlayerAppearancePrefs() {
        miniPlayerBackgroundMode = getSavedMiniPlayerBackgroundMode(context)
        lyricPageBackgroundModeForMiniPlayer = getSavedLyricPageBackgroundModeForMiniPlayer(context)
        miniPlayerLandscapeAlignment = getSavedMiniPlayerLandscapeAlignment(context)
    }

    fun setMiniPlayerHidden(hidden: Boolean) {
        miniPlayerManuallyHidden = hidden
        prefs.edit()
            .putBoolean(KEY_MINI_PLAYER_MANUALLY_HIDDEN, hidden)
            .apply()
        if (hidden) {
            miniPlayerExpandProgress = 0f
            showInlineLyricPreview = false
        }
    }

    fun revealMiniPlayerForUserPlayback() {
        if (miniPlayerManuallyHidden) {
            setMiniPlayerHidden(false)
        }
    }

    suspend fun reloadInlineLyricPreviewFromFile(reason: String, force: Boolean = false) {
        val audioPath = playbackController.currentAudioPath?.takeIf { it.isNotBlank() }
        if (audioPath.isNullOrBlank()) {
            Log.d(PREVIEW_LYRICS_REFRESH_LOG_TAG, "inlineRefreshFromFile skipped reason=$reason blank current path")
            return
        }
        if (!force && lyricPreviewLoadedPath == audioPath) {
            Log.d(PREVIEW_LYRICS_REFRESH_LOG_TAG, "inlineRefreshFromFile skipped reason=$reason alreadyLoaded path=$audioPath")
            return
        }

        lyricPreviewLoading = true
        val resolvedMediaStoreId = playbackController.currentMediaStoreId
        Log.d(
            PREVIEW_LYRICS_REFRESH_LOG_TAG,
            "inlineRefreshFromFile start reason=$reason force=$force path=$audioPath mediaStoreId=$resolvedMediaStoreId"
        )
        val payload = withContext(Dispatchers.IO) {
            buildPlayerLyricPreviewPayload(
                context = context,
                audioPath = audioPath,
                mediaStoreId = resolvedMediaStoreId
            )
        }
        val reorganizedLines = reorganizeLyricsWithBackground(payload?.lines ?: emptyList())
        lyricPreviewLines = reorganizedLines
        lyricPreviewCreators = payload?.creators ?: emptyList()
        lyricPreviewLoadedPath = audioPath
        inlineMetadataRefreshToken += 1
        lyricPreviewLoading = false
        Log.d(
            PREVIEW_LYRICS_REFRESH_LOG_TAG,
            "inlineRefreshFromFile applied reason=$reason token=$inlineMetadataRefreshToken lines=${summarizeInlinePreviewLyricLinesForLog(reorganizedLines)} creators=${lyricPreviewCreators.size}"
        )
    }

    fun applyInlineReturnedLyricsContent(lyricsContent: String) {
        if (lyricsContent.isBlank()) {
            Log.d(PREVIEW_LYRICS_REFRESH_LOG_TAG, "inlineApplyReturned skipped blank content")
            return
        }
        scope.launch {
            lyricPreviewLoading = true
            Log.d(
                PREVIEW_LYRICS_REFRESH_LOG_TAG,
                "inlineApplyReturned start ${summarizeInlinePreviewLyricsContentForLog(lyricsContent)}"
            )
            val payload = withContext(Dispatchers.Default) {
                buildPlayerLyricPreviewPayloadFromContent(lyricsContent)
            }
            val reorganizedLines = reorganizeLyricsWithBackground(payload?.lines ?: emptyList())
            lyricPreviewLines = reorganizedLines
            lyricPreviewCreators = payload?.creators ?: emptyList()
            lyricPreviewLoadedPath = playbackController.currentAudioPath?.takeIf { it.isNotBlank() }
            inlineMetadataRefreshToken += 1
            lyricPreviewLoading = false
            Log.d(
                PREVIEW_LYRICS_REFRESH_LOG_TAG,
                "inlineApplyReturned applied token=$inlineMetadataRefreshToken loadedPath=${lyricPreviewLoadedPath ?: "null"} lines=${summarizeInlinePreviewLyricLinesForLog(reorganizedLines)} creators=${lyricPreviewCreators.size}"
            )
        }
    }
    
    val allAudioFiles = remember { mutableStateListOf<AudioFile>() }
    val displayAudioFiles = remember { mutableStateListOf<AudioFile>() }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0 to 0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pageMenuExpanded by remember { mutableStateOf(false) }
    var showSongClickActionDialog by remember { mutableStateOf(false) }
    var tempSongClickAction by remember { mutableStateOf(songClickAction) }
    var tempAutoDetectEmbeddedLyricsType by remember { mutableStateOf(autoDetectEmbeddedLyricsType) }
    var pendingSongClickAudio by remember { mutableStateOf<AudioFile?>(null) }
    var selectedAudio by remember { mutableStateOf<AudioFile?>(null) }
    var showAudioOptionsDialog by remember { mutableStateOf(false) }
    var selectedSongInfoAudio by remember { mutableStateOf<AudioFile?>(null) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showArtistSelectionSheet by remember { mutableStateOf(false) }
    var pendingAlbumCandidate by remember { mutableStateOf("") }
    var pendingArtistCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputValue by remember { mutableStateOf("") }
    var renameFileExtension by remember { mutableStateOf("") }
    var renameSuccessSignal by remember { mutableStateOf(0L) }
    var bulkFavoriteAddedCount by remember { mutableIntStateOf(0) }
    var showBulkFavoriteResultDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var scanSummary by remember { mutableStateOf(ScanSummary()) }
    var isFromCache by remember { mutableStateOf(false) }
    var showScanComplete by remember { mutableStateOf(false) }
    var isLoadingCache by remember { mutableStateOf(true) }
    var isPreparingInitialCovers by remember { mutableStateOf(false) }
    var isInitialListFadeReady by remember { mutableStateOf(false) }
    var displayUpdateJob by remember { mutableStateOf<Job?>(null) }
    var displayUpdateVersion by remember { mutableIntStateOf(0) }
    var isUpdatingDisplayList by remember { mutableStateOf(false) }
    var hasCompletedInitialDisplayBuild by remember { mutableStateOf(false) }
    val miniPlayerReady = !isLoadingCache && isInitialListFadeReady
    val showMiniPlayer = miniPlayerVisible && miniPlayerReady
    val miniPlayerExtraBottomPadding = if (showMiniPlayer) miniPlayerHeight + 12.dp else 0.dp
    var hasTriggeredInitialReveal by remember { mutableStateOf(false) }
    var showScanProgressPopup by remember { mutableStateOf(false) }
    var scanPopupDelayJob by remember { mutableStateOf<Job?>(null) }
    var pendingLocateAudioPath by remember { mutableStateOf<String?>(null) }
    var locateHighlightPath by remember { mutableStateOf<String?>(null) }
    var locateHighlightActive by remember { mutableStateOf(false) }
    var locateHighlightJob by remember { mutableStateOf<Job?>(null) }
    
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var showSearchHistoryPopup by remember { mutableStateOf(false) }
    var recentSearchHistory by remember {
        mutableStateOf(loadRecentSearchHistory(prefs))
    }
    var searchBoxFocused by remember { mutableStateOf(false) }
    var searchQueryApplied by remember { mutableStateOf(searchQuery.trim()) }
    var lastAppliedInitialSearchRequest by remember { mutableIntStateOf(-1) }
    var lastAppliedInitialLibraryPageRequest by remember { mutableIntStateOf(-1) }
    var isSearching by remember { mutableStateOf(false) }
    var internalPage by rememberSaveable { mutableStateOf(MusicLibraryInternalPage.SONGS) }
    var internalArtistName by rememberSaveable { mutableStateOf("") }
    var internalArtistDetailTab by rememberSaveable { mutableStateOf(ArtistDetailTab.SONGS) }
    var internalAlbumName by rememberSaveable { mutableStateOf("") }
    var internalAlbumReturnPage by rememberSaveable { mutableStateOf(MusicLibraryInternalPage.SONGS) }
    val internalArtistsListState = rememberLazyListState()
    val favoritePaths = remember { mutableStateSetOf<String>() }
    val albumTrackSortCache = remember { mutableMapOf<String, Int>() }
    var trackSortWarmupJob by remember { mutableStateOf<Job?>(null) }
    var trackSortWarmupKey by remember { mutableStateOf("") }
    
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedPaths = remember { mutableStateSetOf<String>() }
    val lastSelectedIndices = remember { mutableStateListOf<Int>() }
    
    var showBatchMenu by remember { mutableStateOf(false) }
    var showBatchMatchConfig by remember { mutableStateOf(false) }
    var batchMatchResult by remember { mutableStateOf<BatchMatchResult?>(null) }
    var showBatchMatchResult by remember { mutableStateOf(false) }
    var isBatchMatching by remember { mutableStateOf(false) }
    var isCancelled by remember { mutableStateOf(false) }
    var batchMatchProgress by remember { mutableStateOf(0 to 0) }
    
    var showBatchRenameConfig by remember { mutableStateOf(false) }
    var showBatchRenamePreview by remember { mutableStateOf(false) }
    var batchRenameConfig by remember { mutableStateOf(RenameConfig()) }
    var batchRenamePreviewItems by remember { mutableStateOf<List<RenamePreviewItem>>(emptyList()) }
    var isRenaming by remember { mutableStateOf(false) }
    var batchRenameProgress by remember { mutableStateOf(0 to 0) }
    var showBatchRenameResult by remember { mutableStateOf(false) }
    var batchRenameResult by remember { mutableStateOf<RenameResult?>(null) }
    
    var showBatchLyricMatchConfig by remember { mutableStateOf(false) }
    var isBatchLyricsMatching by remember { mutableStateOf(false) }
    var isLyricsCancelled by remember { mutableStateOf(false) }
    var batchLyricsMatchProgress by remember { mutableStateOf(0 to 0) }
    var batchLyricMatchResult by remember { mutableStateOf<BatchLyricMatchResult?>(null) }
    var showBatchLyricMatchResult by remember { mutableStateOf(false) }
    
    var showBatchLyricsEditSheet by remember { mutableStateOf(false) }
    var showBatchLyricsConvertSheet by remember { mutableStateOf(false) }
    var showBatchLyricsExportSheet by remember { mutableStateOf(false) }
    var isBatchLyricsConverting by remember { mutableStateOf(false) }
    var batchLyricsConvertProgress by remember { mutableStateOf(0 to 0) }
    var isBatchLyricsExporting by remember { mutableStateOf(false) }
    var batchLyricsExportProgress by remember { mutableStateOf(0 to 0) }
    
    val sortType = remember { 
        mutableStateOf(
            try {
                SortType.valueOf(prefs.getString("sortType", SortType.FILE_NAME.name) ?: SortType.FILE_NAME.name)
            } catch (e: Exception) { SortType.FILE_NAME }
        )
    }
    val sortOrder = remember {
        mutableStateOf(
            try {
                SortOrder.valueOf(prefs.getString("sortOrder", SortOrder.ASC.name) ?: SortOrder.ASC.name)
            } catch (e: Exception) { SortOrder.ASC }
        )
    }
    
    val activity = context as MusicLibraryActivity
    val density = LocalDensity.current
    val coverPreloadSizePx = with(density) { 56.dp.toPx().toInt().coerceAtLeast(56) }

    fun resolveTrackSortValue(audio: AudioFile): Int {
        val cached = albumTrackSortCache[audio.path]
        if (cached != null) return cached
        return Int.MAX_VALUE
    }

    fun warmupTrackSortCacheFor(querySnapshot: String, list: List<AudioFile>) {
        val trimmedQuery = querySnapshot.trim()
        val isAlbumQuery = trimmedQuery.startsWith(ALBUM_SEARCH_PREFIX_FULL) || trimmedQuery.startsWith(ALBUM_SEARCH_PREFIX_ASCII)
        if (!isAlbumQuery) return
        val missing = list.filter { albumTrackSortCache[it.path] == null }
        if (missing.isEmpty()) return

        val newKey = buildString {
            append(querySnapshot.trim())
            append('#')
            append(missing.size)
            append('#')
            append(missing.firstOrNull()?.path.orEmpty())
        }
        if (trackSortWarmupJob?.isActive == true && newKey == trackSortWarmupKey) return

        trackSortWarmupJob?.cancel()
        trackSortWarmupKey = newKey
        trackSortWarmupJob = scope.launch(Dispatchers.IO) {
            val updates = mutableMapOf<String, Int>()
            missing.forEachIndexed { index, audio ->
                val trackValue = runCatching {
                    val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
                        context = context,
                        filePath = audio.path,
                        mediaStoreId = audio.mediaStoreId,
                        includeCover = false
                    )
                    parseTrackNumberValue(metadata.trackNumber)
                }.getOrDefault(Int.MAX_VALUE)
                updates[audio.path] = trackValue
                if ((index + 1) % 20 == 0) {
                    kotlinx.coroutines.yield()
                }
            }

            withContext(Dispatchers.Main) {
                var changed = false
                updates.forEach { (path, value) ->
                    if (albumTrackSortCache[path] != value) {
                        albumTrackSortCache[path] = value
                        changed = true
                    }
                }
                val currentQuery = searchQuery.trim()
                val stillAlbumQuery = currentQuery.startsWith(ALBUM_SEARCH_PREFIX_FULL) || currentQuery.startsWith(ALBUM_SEARCH_PREFIX_ASCII)
                if (changed && stillAlbumQuery && displayAudioFiles.isNotEmpty()) {
                    val resorted = displayAudioFiles
                        .toList()
                        .sortedWith(
                            compareBy<AudioFile> { resolveTrackSortValue(it) }
                                .thenBy { buildFileNameSortKey(it.displayTitle) }
                                .thenBy { it.displayTitle.lowercase(Locale.ROOT) }
                        )
                    displayAudioFiles.clear()
                    displayAudioFiles.addAll(resorted)
                }
            }
        }
    }

    fun isAlbumSearchQuery(query: String): Boolean {
        val trimmed = query.trim()
        return trimmed.startsWith(ALBUM_SEARCH_PREFIX_FULL) || trimmed.startsWith(ALBUM_SEARCH_PREFIX_ASCII)
    }

    fun buildFilteredDisplayList(): List<AudioFile> {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) return allAudioFiles.toList()

        if (trimmed.startsWith(FAVORITE_SEARCH_TOKEN, ignoreCase = true)) {
            val keyword = trimmed.removePrefix(FAVORITE_SEARCH_TOKEN).trim()
            val favoriteList = allAudioFiles.filter { it.path in favoritePaths }
            if (keyword.isBlank()) return favoriteList
            return favoriteList.filter {
                it.displayTitle.contains(keyword, ignoreCase = true) ||
                    it.displayArtist.contains(keyword, ignoreCase = true) ||
                    it.displayAlbum.contains(keyword, ignoreCase = true)
            }
        }

        if (isAlbumSearchQuery(trimmed)) {
            val keyword = trimmed
                .removePrefix(ALBUM_SEARCH_PREFIX_FULL)
                .removePrefix(ALBUM_SEARCH_PREFIX_ASCII)
                .trim()
            if (keyword.isBlank()) return emptyList()
            return allAudioFiles
                .filter { it.displayAlbum.contains(keyword, ignoreCase = true) }
                .sortedWith(
                    compareBy<AudioFile> { resolveTrackSortValue(it) }
                        .thenBy { buildFileNameSortKey(it.displayTitle) }
                        .thenBy { it.displayTitle.lowercase(Locale.ROOT) }
                )
        }

        return allAudioFiles.filter {
            it.displayTitle.contains(trimmed, ignoreCase = true) ||
                it.displayArtist.contains(trimmed, ignoreCase = true) ||
                it.album.contains(trimmed, ignoreCase = true)
        }
    }

    fun persistFavoritesPlaylist() {
        val entries = allAudioFiles
            .filter { it.path in favoritePaths }
            .map { audio ->
                LocalPlaylistEntry(
                    path = audio.path,
                    title = audio.displayTitle,
                    artist = audio.displayArtist,
                    durationSeconds = (audio.duration / 1000L).coerceAtLeast(-1L)
                )
            }
        LocalPlaylistStore.saveFavorites(context, entries)
    }

    fun updateDisplayFiles() {
        val requestVersion = ++displayUpdateVersion
        displayUpdateJob?.cancel()
        isUpdatingDisplayList = true
        displayUpdateJob = scope.launch {
            try {
                val querySnapshot = searchQuery
                val allAudioSnapshot = allAudioFiles.toList()
                val favoriteSnapshot = favoritePaths.toSet()
                val sortTypeSnapshot = sortType.value
                val sortOrderSnapshot = sortOrder.value

                val filtered = withContext(Dispatchers.Default) {
                    val trimmed = querySnapshot.trim()
                    val builtList = when {
                        trimmed.isEmpty() -> allAudioSnapshot
                        trimmed.startsWith(FAVORITE_SEARCH_TOKEN, ignoreCase = true) -> {
                            val keyword = trimmed.removePrefix(FAVORITE_SEARCH_TOKEN).trim()
                            val favoriteList = allAudioSnapshot.filter { it.path in favoriteSnapshot }
                            if (keyword.isBlank()) {
                                favoriteList
                            } else {
                                favoriteList.filter {
                                    it.displayTitle.contains(keyword, ignoreCase = true) ||
                                        it.displayArtist.contains(keyword, ignoreCase = true) ||
                                        it.displayAlbum.contains(keyword, ignoreCase = true)
                                }
                            }
                        }
                        isAlbumSearchQuery(trimmed) -> {
                            val keyword = trimmed
                                .removePrefix(ALBUM_SEARCH_PREFIX_FULL)
                                .removePrefix(ALBUM_SEARCH_PREFIX_ASCII)
                                .trim()
                            if (keyword.isBlank()) {
                                emptyList()
                            } else {
                                allAudioSnapshot
                                    .filter { it.displayAlbum.contains(keyword, ignoreCase = true) }
                                    .sortedWith(
                                        compareBy<AudioFile> { resolveTrackSortValue(it) }
                                            .thenBy { buildFileNameSortKey(it.displayTitle) }
                                            .thenBy { it.displayTitle.lowercase(Locale.ROOT) }
                                    )
                            }
                        }
                        else -> {
                            allAudioSnapshot.filter {
                                it.displayTitle.contains(trimmed, ignoreCase = true) ||
                                    it.displayArtist.contains(trimmed, ignoreCase = true) ||
                                    it.album.contains(trimmed, ignoreCase = true)
                            }
                        }
                    }

                    if (isAlbumSearchQuery(trimmed)) {
                        builtList
                    } else {
                        val sorted = builtList.toMutableList()
                        sortAudioFiles(sorted, sortTypeSnapshot, sortOrderSnapshot)
                        sorted
                    }
                }

                if (requestVersion != displayUpdateVersion) return@launch
                displayAudioFiles.clear()
                if (filtered.isNotEmpty()) {
                    val batchSize = 200
                    var start = 0
                    while (start < filtered.size) {
                        if (requestVersion != displayUpdateVersion) return@launch
                        val end = minOf(start + batchSize, filtered.size)
                        displayAudioFiles.addAll(filtered.subList(start, end))
                        start = end
                        if (start < filtered.size) {
                            kotlinx.coroutines.yield()
                        }
                    }
                }

                if (isAlbumSearchQuery(querySnapshot)) {
                    warmupTrackSortCacheFor(querySnapshot, filtered)
                }
                if (requestVersion == displayUpdateVersion) {
                    hasCompletedInitialDisplayBuild = true
                }
            } finally {
                if (requestVersion == displayUpdateVersion) {
                    isUpdatingDisplayList = false
                }
            }
        }
    }

    val screenConfig = LocalConfiguration.current
    val layoutModePreference = getSavedAppLayoutModePreference(context)
    val effectiveLayoutProfile = remember(
        layoutModePreference,
        screenConfig.screenWidthDp,
        screenConfig.screenHeightDp,
        screenConfig.smallestScreenWidthDp,
        screenConfig.orientation
    ) {
        resolveAppLayoutProfile(context, layoutModePreference)
    }
    val inlinePreviewTransitionOffsetPx = with(density) {
        screenConfig.screenHeightDp.dp.toPx().coerceAtLeast(1f)
    }
    val autoExpandTriggerDistancePx = with(density) {
        (screenConfig.screenHeightDp.dp.toPx() * 0.15f).coerceAtLeast(1f)
    }
    val previewTopDragHandleHeightPx = with(density) { 132.dp.toPx() }
    val previewScreenHeightPx = with(density) { screenConfig.screenHeightDp.dp.toPx().coerceAtLeast(1f) }
    val isInlinePreviewLandscape =
        screenConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isInlinePreviewLargeScreen = effectiveLayoutProfile == AppLayoutProfile.TABLET
    val useWideMiniPlayerDock = isInlinePreviewLandscape || isInlinePreviewLargeScreen
    val miniPlayerBackgroundStyle = resolveMiniPlayerBackgroundStyle(
        backgroundMode = miniPlayerBackgroundMode,
        pageBackgroundMode = lyricPageBackgroundModeForMiniPlayer
    )
    val portraitExpandedDragMaxY = previewScreenHeightPx * 0.72f
    val landscapeLargeScreenDragMaxY = previewScreenHeightPx * 0.72f

    fun resolveMiniPlayerTravelDistancePx(): Float {
        return (miniBarBaseTop ?: miniBarBounds?.top ?: inlinePreviewTransitionOffsetPx)
            .coerceAtLeast(1f)
    }

    fun animateMiniPlayerProgressTo(
        target: Float,
        durationMillis: Int = 280,
        useLinearEasing: Boolean = false,
        deferToNextFrame: Boolean = false
    ) {
        val resolvedTarget = target.coerceIn(0f, 1f)
        val settledEasing = if (useLinearEasing) LinearEasing else FastOutSlowInEasing
        miniPlayerSettleJob?.cancel()
        miniPlayerSettleJob = scope.launch {
            if (deferToNextFrame) {
                withFrameNanos { }
            }
            val startValue = miniPlayerExpandProgress
            animate(
                initialValue = startValue,
                targetValue = resolvedTarget,
                animationSpec = tween(durationMillis = durationMillis, easing = settledEasing)
            ) { value, _ ->
                miniPlayerExpandProgress = value
            }
            if (resolvedTarget <= 0f) {
                showInlineLyricPreview = false
            } else {
                showInlineLyricPreview = true
            }
            miniPlayerSettleJob = null
        }
    }

    fun persistInlineLyricDisplaySelected(selected: Boolean) {
        inlineLyricDisplaySelected = selected
        lyricPreviewPrefs.edit()
            .putBoolean(LyricPreviewActivity.KEY_LYRIC_DISPLAY_SELECTED, selected)
            .commit()
    }

    fun settleMiniPlayerExpandState() {
        val travelDistancePx = resolveMiniPlayerTravelDistancePx()
        val movedDistancePx = miniPlayerExpandProgress * travelDistancePx
        val collapsedDistancePx = (travelDistancePx - movedDistancePx).coerceAtLeast(0f)
        val settleToExpanded = when {
            miniPlayerLastDragDeltaY < -0.5f -> movedDistancePx >= autoExpandTriggerDistancePx
            miniPlayerLastDragDeltaY > 0.5f -> collapsedDistancePx < autoExpandTriggerDistancePx
            else -> miniPlayerExpandProgress >= 0.5f
        }
        miniPlayerLastDragDeltaY = 0f
        if (settleToExpanded) {
            showInlineLyricPreview = true
            animateMiniPlayerProgressTo(1f, durationMillis = 260)
        } else {
            persistInlineLyricDisplaySelected(inlineLyricDisplaySelected)
            animateMiniPlayerProgressTo(0f, durationMillis = 220)
        }
    }

    val refreshInlineLyricDisplaySelectedFromPrefs: () -> Unit = {
        inlineLyricDisplaySelected = lyricPreviewPrefs.getBoolean(
            LyricPreviewActivity.KEY_LYRIC_DISPLAY_SELECTED,
            LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_SELECTED
        )
    }

    fun isInlineCompanionNameMatch(fileName: String?, sourceBaseName: String): Boolean {
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

    fun findInlineCompanionInDir(
        dir: File,
        exactFileName: String,
        sourceBaseName: String
    ): File? {
        if (!dir.exists() || !dir.isDirectory) return null
        val exactCandidate = File(dir, exactFileName)
        if (exactCandidate.exists() && exactCandidate.isFile) return exactCandidate
        val files = dir.listFiles() ?: return null
        return files.firstOrNull { file ->
            file.isFile && isInlineCompanionNameMatch(file.name, sourceBaseName)
        }
    }

    fun resolveInlineCompanionAudioPath(sourcePath: String): String? {
        val sourceFile = File(sourcePath)
        val exactFileName = sourceFile.name.takeIf { it.isNotBlank() } ?: return null
        val sourceBaseName = sourceFile.nameWithoutExtension.takeIf { it.isNotBlank() } ?: return null
        val candidateDirs = linkedSetOf<File>()
        candidateDirs += File("/storage/emulated/0/Music/.sing")
        val sourceParent = sourceFile.parentFile
        if (sourceParent != null) {
            candidateDirs += File(sourceParent, "sing")
            candidateDirs += File(sourceParent, ".sing")
            sourceParent.parentFile?.let { parent ->
                candidateDirs += File(parent, "music${File.separator}sing")
                candidateDirs += File(parent, "Music${File.separator}sing")
                candidateDirs += File(parent, "music${File.separator}.sing")
                candidateDirs += File(parent, "Music${File.separator}.sing")
            }
        }
        return candidateDirs.firstNotNullOfOrNull { dir ->
            findInlineCompanionInDir(
                dir = dir,
                exactFileName = exactFileName,
                sourceBaseName = sourceBaseName
            )
        }?.absolutePath
    }

    fun isSameInlineAudioPath(pathA: String?, pathB: String?): Boolean {
        if (pathA.isNullOrBlank() || pathB.isNullOrBlank()) return false
        val normalizedA = runCatching { File(pathA).absolutePath }.getOrElse { pathA }
        val normalizedB = runCatching { File(pathB).absolutePath }.getOrElse { pathB }
        return normalizedA == normalizedB
    }

    fun openInlineLyricPreview() {
        miniPlayerSettleJob?.cancel()
        refreshInlineLyricDisplaySelectedFromPrefs()
        inlineLyricDisplayResetToken = 0
        showInlineLyricPreview = true
        miniBarBaseTop = miniBarBounds?.top ?: miniBarBaseTop
        if (miniPlayerExpandProgress <= 0.001f) {
            miniPlayerExpandProgress = 0f
        }
        miniPlayerLastDragDeltaY = -1f
        animateMiniPlayerProgressTo(
            target = 1f,
            durationMillis = 300,
            useLinearEasing = false,
            deferToNextFrame = true
        )
    }

    fun closeInlineLyricPreview() {
        persistInlineLyricDisplaySelected(inlineLyricDisplaySelected)
        inlineLyricDisplayResetToken = 0
        animateMiniPlayerProgressTo(0f)
    }

    fun openInternalAlbumDetail(albumName: String) {
        val normalizedAlbumName = albumName.trim()
        if (normalizedAlbumName.isBlank()) return
        internalAlbumReturnPage = internalPage
        internalAlbumName = normalizedAlbumName
        internalPage = MusicLibraryInternalPage.ALBUM_DETAIL
    }

    fun openInternalArtistDetail(artistName: String) {
        val normalizedArtistName = artistName.trim()
        if (normalizedArtistName.isBlank()) return
        internalArtistName = normalizedArtistName
        internalArtistDetailTab = ArtistDetailTab.SONGS
        internalAlbumName = ""
        internalAlbumReturnPage = MusicLibraryInternalPage.SONGS
        internalPage = MusicLibraryInternalPage.ARTIST_DETAIL
    }

    fun closeInternalAlbumDetail() {
        val returnPage = internalAlbumReturnPage
            .takeIf { it != MusicLibraryInternalPage.ALBUM_DETAIL }
            ?: MusicLibraryInternalPage.SONGS
        internalPage = returnPage
        internalAlbumName = ""
        internalAlbumReturnPage = MusicLibraryInternalPage.SONGS
    }

    fun updateMiniPlayerDragProgress(deltaY: Float) {
        miniPlayerSettleJob?.cancel()
        showInlineLyricPreview = true
        miniPlayerLastDragDeltaY = deltaY
        val dragDistancePx = resolveMiniPlayerTravelDistancePx()
        val updated = miniPlayerExpandProgress + (-deltaY / dragDistancePx)
        miniPlayerExpandProgress = updated.coerceIn(0f, 1f)
    }

    LaunchedEffect(
        initialInlinePreviewRequestVersion,
        miniPlayerVisible,
        miniPlayerReady,
        playbackController.currentMediaId
    ) {
        if (initialInlinePreviewRequestVersion <= lastAppliedInlinePreviewRequest) return@LaunchedEffect
        if (!miniPlayerVisible || !miniPlayerReady || playbackController.currentAudioPath.isNullOrBlank()) {
            return@LaunchedEffect
        }
        playbackController.refreshProgress()
        openInlineLyricPreview()
        lastAppliedInlinePreviewRequest = initialInlinePreviewRequestVersion
    }

    BackHandler(
        enabled = miniPlayerExpandProgress > 0f ||
            internalPage != MusicLibraryInternalPage.SONGS ||
            showSearchHistoryPopup ||
            searchQuery.isNotBlank() ||
            isMultiSelectMode
    ) {
        if (miniPlayerExpandProgress > 0f) {
            if (inlineLyricDisplaySelected) {
                persistInlineLyricDisplaySelected(false)
                inlineLyricDisplayResetToken += 1
            } else {
                closeInlineLyricPreview()
            }
            return@BackHandler
        }
        if (internalPage == MusicLibraryInternalPage.ALBUM_DETAIL) {
            closeInternalAlbumDetail()
            return@BackHandler
        }
        if (internalPage == MusicLibraryInternalPage.ARTIST_DETAIL) {
            internalPage = MusicLibraryInternalPage.ARTISTS
            internalArtistName = ""
            return@BackHandler
        }
        if (internalPage == MusicLibraryInternalPage.ARTISTS) {
            internalPage = MusicLibraryInternalPage.SONGS
            return@BackHandler
        }
        if (internalPage == MusicLibraryInternalPage.SETTINGS || internalPage == MusicLibraryInternalPage.ABOUT) {
            internalPage = MusicLibraryInternalPage.SONGS
            return@BackHandler
        }
        if (showSearchHistoryPopup) {
            showSearchHistoryPopup = false
            return@BackHandler
        }
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
            updateDisplayFiles()
            return@BackHandler
        }
        if (isMultiSelectMode) {
            isMultiSelectMode = false
            selectedPaths.clear()
        }
    }

    LaunchedEffect(initialSearchRequestVersion, initialSearchQuery) {
        if (initialSearchRequestVersion == lastAppliedInitialSearchRequest) return@LaunchedEffect
        lastAppliedInitialSearchRequest = initialSearchRequestVersion
        searchQuery = initialSearchQuery
        updateDisplayFiles()
    }

    LaunchedEffect(initialLibraryPageRequestVersion, initialArtistName, initialAlbumName) {
        if (initialLibraryPageRequestVersion == lastAppliedInitialLibraryPageRequest) return@LaunchedEffect
        lastAppliedInitialLibraryPageRequest = initialLibraryPageRequestVersion
        val targetAlbum = initialAlbumName.trim()
        val targetArtist = initialArtistName.trim()
        if (targetAlbum.isNotBlank()) {
            searchQuery = ""
            updateDisplayFiles()
            internalAlbumReturnPage = MusicLibraryInternalPage.SONGS
            internalAlbumName = targetAlbum
            internalArtistName = ""
            internalPage = MusicLibraryInternalPage.ALBUM_DETAIL
        } else if (targetArtist.isNotBlank()) {
            searchQuery = ""
            updateDisplayFiles()
            openInternalArtistDetail(targetArtist)
        }
    }

    LaunchedEffect(searchQuery) {
        val normalized = searchQuery.trim()
        if (normalized.isBlank()) {
            searchQueryApplied = ""
            return@LaunchedEffect
        }
        if (normalized == searchQueryApplied) return@LaunchedEffect
        delay(3000L)
        if (searchQuery.trim() != normalized) return@LaunchedEffect
        val updatedHistory = recentSearchHistory.toMutableList()
        appendRecentSearchHistory(prefs, updatedHistory, normalized)
        recentSearchHistory = updatedHistory
        searchQueryApplied = normalized
    }

    LaunchedEffect(currentPlayingAudioPath, playbackController.currentMediaStoreId) {
        val audioPath = currentPlayingAudioPath
        if (audioPath.isNullOrBlank()) {
            lyricPreviewLines = emptyList()
            lyricPreviewCreators = emptyList()
            lyricPreviewLoading = false
            lyricPreviewLoadedPath = null
            inlineCompanionAudioPath = null
            inlineCompanionSwitching = false
            miniPlayerCoverBitmap = null
            miniPlayerExpandProgress = 0f
            showInlineLyricPreview = false
            inlineLyricDisplayResetToken = 0
            inlineMetadataRefreshToken = 0
            return@LaunchedEffect
        }

        reloadInlineLyricPreviewFromFile(reason = "trackChanged", force = false)
    }

    LaunchedEffect(currentPlayingAudioPath) {
        val audioPath = currentPlayingAudioPath
        if (audioPath.isNullOrBlank()) {
            inlineCompanionAudioPath = null
            inlineCompanionSwitching = false
            return@LaunchedEffect
        }
        inlineCompanionSwitching = false
        inlineCompanionAudioPath = withContext(Dispatchers.IO) {
            resolveInlineCompanionAudioPath(audioPath)
        }
    }

    LaunchedEffect(Unit) {
        val pathToRefresh = activity.getRefreshMetadataPath()
        if (pathToRefresh != null) {
            if (refreshAudioFileMetadata(context, pathToRefresh, allAudioFiles) != null) {
                saveCachedAudioFiles(context, allAudioFiles)
            }
            updateDisplayFiles()
        }
    }

    LaunchedEffect(activity) {
        AudioMetadataUpdateBus.updates.collect { updatedPath ->
            val refreshed = refreshAudioFileMetadata(context, updatedPath, allAudioFiles)
            if (refreshed != null) {
                saveCachedAudioFiles(context, allAudioFiles)
                if (selectedSongInfoAudio?.path == updatedPath) {
                    selectedSongInfoAudio = refreshed
                }
                updateDisplayFiles()
            }
        }
    }

    LaunchedEffect(isLoadingCache, isScanning, displayAudioFiles.size) {
        if (hasTriggeredInitialReveal) return@LaunchedEffect
        if (isLoadingCache) return@LaunchedEffect
        if (displayAudioFiles.isEmpty() && isScanning) return@LaunchedEffect

        hasTriggeredInitialReveal = true
        isInitialListFadeReady = true
        isPreparingInitialCovers = true
        val initialSnapshot = displayAudioFiles.toList()
        try {
            withContext(Dispatchers.IO) {
                initialSnapshot.forEach { audio ->
                    val cachePath = audio.coverCachePath ?: return@forEach
                    val bitmap = runCatching {
                        loadCoverBitmapFromCache(cachePath, coverPreloadSizePx, coverPreloadSizePx)
                            ?: rebuildCoverCacheBitmap(context, audio, cachePath, coverPreloadSizePx)
                    }.getOrNull()
                    bitmap?.let { MusicLibraryActivity.putCoverToCache(cachePath, it) }
                }
            }
        } finally {
            isPreparingInitialCovers = false
            isInitialListFadeReady = true
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(prefs, lyricPreviewPrefs) {
        val musicLibraryPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == KEY_MINI_PLAYER_BACKGROUND_MODE ||
                key == KEY_MINI_PLAYER_LANDSCAPE_ALIGNMENT
            ) {
                refreshMiniPlayerAppearancePrefs()
            }
        }
        val lyricPreviewPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == LyricPreviewActivity.KEY_PAGE_BACKGROUND_MODE ||
                key == LyricPreviewActivity.KEY_DYNAMIC_COVER_BACKGROUND
            ) {
                lyricPageBackgroundModeForMiniPlayer = getSavedLyricPageBackgroundModeForMiniPlayer(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(musicLibraryPrefsListener)
        lyricPreviewPrefs.registerOnSharedPreferenceChangeListener(lyricPreviewPrefsListener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(musicLibraryPrefsListener)
            lyricPreviewPrefs.unregisterOnSharedPreferenceChangeListener(lyricPreviewPrefsListener)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    songClickAction = prefs.getString("songClickAction", "") ?: ""
                    hasConfirmedSongClickAction = prefs.getBoolean(PREF_SONG_CLICK_ACTION_CONFIRMED, false)
                    autoDetectEmbeddedLyricsType = prefs.getBoolean("autoDetectEmbeddedLyricsType", false)
                    refreshMiniPlayerAppearancePrefs()
                    val pathToRefresh = activity.getRefreshMetadataPath()
                    if (pathToRefresh != null) {
                        scope.launch {
                            if (refreshAudioFileMetadata(context, pathToRefresh, allAudioFiles) != null) {
                                saveCachedAudioFiles(context, allAudioFiles)
                            }
                            updateDisplayFiles()
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    playbackController.persistCurrentPlaybackState()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    fun toggleSelection(path: String) {
        if (selectedPaths.contains(path)) {
            selectedPaths.remove(path)
        } else {
            selectedPaths.add(path)
        }
    }
    
    fun selectAll() {
        selectedPaths.clear()
        selectedPaths.addAll(displayAudioFiles.map { it.path })
        lastSelectedIndices.clear()
    }
    
    fun invertSelection() {
        val currentSelected = selectedPaths.toSet()
        selectedPaths.clear()
        lastSelectedIndices.clear()
        displayAudioFiles.forEach { audio ->
            if (!currentSelected.contains(audio.path)) {
                selectedPaths.add(audio.path)
            }
        }
    }
    
    fun clearSelection() {
        selectedPaths.clear()
        lastSelectedIndices.clear()
    }
    
    fun selectRange(startIdx: Int, endIdx: Int) {
        val start = startIdx.coerceIn(1, displayAudioFiles.size)
        val end = endIdx.coerceIn(1, displayAudioFiles.size)
        val rangeStart = minOf(start, end) - 1
        val rangeEnd = maxOf(start, end) - 1
        for (i in rangeStart..rangeEnd) {
            selectedPaths.add(displayAudioFiles[i].path)
        }
        lastSelectedIndices.clear()
    }
    
    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedPaths.clear()
        lastSelectedIndices.clear()
    }

    fun startLyricTimingEditor(audio: AudioFile, lyricsContent: String?, format: String) {
        val intent = Intent(context, LyricTimingActivity::class.java).apply {
            putExtra("audioPath", audio.path)
            putExtra("lyricsContent", lyricsContent)
            putExtra("sourceTitle", audio.displayTitle)
            putExtra("sourceArtist", audio.artist)
            putExtra("lyricsFormat", format)
            putExtra(LyricTimingActivity.EXTRA_MEDIA_STORE_ID, audio.mediaStoreId)
        }
        context.startActivity(intent)
    }

    fun executePrimarySongAction(audio: AudioFile) {
        when (songClickAction) {
            "playMusic" -> {
                revealMiniPlayerForUserPlayback()
                playbackController.playQueue(displayAudioFiles.toList(), audio.path)
            }
            "editMetadata" -> {
                onEditMetadata(audio.path)
            }
            else -> {
                handleMusicLibraryItemLyricsAction(
                    scope = scope,
                    context = context,
                    audio = audio,
                    autoDetectEmbeddedLyricsType = autoDetectEmbeddedLyricsType,
                    onShowOptions = {
                        selectedAudio = it
                        showAudioOptionsDialog = true
                    },
                    onStartLyricTimingEditor = { targetAudio, lyricsContent, format ->
                        startLyricTimingEditor(
                            audio = targetAudio,
                            lyricsContent = lyricsContent,
                            format = format
                        )
                    }
                )
            }
        }
    }

    fun shouldShowScanComplete(summary: ScanSummary): Boolean {
        return summary.addedCount > 0 || summary.removedCount > 0 || summary.updatedCount > 0
    }

    fun startScanPopupDelay() {
        scanPopupDelayJob?.cancel()
        showScanProgressPopup = false
        scanPopupDelayJob = scope.launch {
            delay(3000)
            if (isScanning) {
                showScanProgressPopup = true
            }
        }
    }

    fun hideScanPopupImmediately() {
        scanPopupDelayJob?.cancel()
        scanPopupDelayJob = null
        showScanProgressPopup = false
    }

    fun showScanPopupImmediately() {
        scanPopupDelayJob?.cancel()
        scanPopupDelayJob = null
        showScanProgressPopup = true
    }
    
    LaunchedEffect(Unit) {
        val favoritePathsSnapshot = withContext(Dispatchers.IO) {
            LocalPlaylistStore.loadFavoritePaths(context).filter { path ->
                runCatching { File(path).exists() }.getOrDefault(false)
            }
        }
        favoritePaths.clear()
        favoritePaths.addAll(favoritePathsSnapshot)
        val hasCache = loadCachedAudioFiles(context, allAudioFiles)
        if (hasCache) {
            isFromCache = true
            updateDisplayFiles()
        }
        isLoadingCache = false
        val autoScanEnabled = prefs.getBoolean("autoScan", false)
        val useNativeMediaLibrary = prefs.getBoolean("useNativeMediaLibrary", true)
        val currentFolders = prefs.getStringSet("musicFolders", emptySet()) ?: emptySet()
        val lastScannedFolders = prefs.getStringSet("lastScannedFolders", emptySet()) ?: emptySet()
        val nowMs = System.currentTimeMillis()
        val lastNativeMediaSyncAt = prefs.getLong("lastNativeMediaSyncAt", 0L)
        val nativeAutoSyncIntervalMs = 10 * 60 * 1000L
        
        // 检查目录是否有变化（新增或减少）
        val foldersChanged = currentFolders != lastScannedFolders
        val shouldScan = if (useNativeMediaLibrary) {
            !hasCache ||
                autoScanEnabled ||
                (nowMs - lastNativeMediaSyncAt) >= nativeAutoSyncIntervalMs
        } else {
            !hasCache || foldersChanged || autoScanEnabled
        }

        // 原生媒体库模式默认自动增量同步；目录模式按原逻辑触发扫描
        if (shouldScan) {
            scanJob = launch {
                if (hasCache && useNativeMediaLibrary) {
                    // 先展示缓存列表，避免页面进入瞬间抢占资源导致触控无响应。
                    delay(1200)
                }
                isScanning = true
                showScanComplete = false
                startScanPopupDelay()
                scanAudioFiles(context, prefs, allAudioFiles,
                    onProgress = { current, total ->
                        scanProgress = current to total
                    },
                    onComplete = { summary ->
                        isScanning = false
                        hideScanPopupImmediately()
                        scanSummary = summary
                        if (isFromCache && shouldShowScanComplete(summary)) {
                            showScanComplete = true
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                showScanComplete = false
                            }
                        } else {
                            showScanComplete = false
                        }
                        updateDisplayFiles()
                    }
                )
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .layerBackdrop(miniPlayerBackdrop)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                var lastClickTime by remember { mutableStateOf(0L) }
                var showDoubleTapHint by remember { mutableStateOf(false) }
                var previousFirstVisibleIndex by remember { mutableStateOf(0) }
                var hasShownHint by remember { mutableStateOf(false) }
                
                val listState = rememberLazyListState()
                val musicListColumnCount = remember(
                    effectiveLayoutProfile,
                    screenConfig.orientation,
                    screenConfig.screenWidthDp
                ) {
                    when (effectiveLayoutProfile) {
                        AppLayoutProfile.WATCH -> 1
                        AppLayoutProfile.PHONE -> {
                            if (screenConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                                (screenConfig.screenWidthDp / 360).coerceIn(2, 4)
                            } else {
                                1
                            }
                        }
                        AppLayoutProfile.TABLET -> (screenConfig.screenWidthDp / 360).coerceIn(2, 4)
                    }
                }
                
                LaunchedEffect(showDoubleTapHint) {
                    if (showDoubleTapHint) {
                        delay(2000)
                        showDoubleTapHint = false
                    }
                }
                
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    val currentIndex = listState.firstVisibleItemIndex
                    if (!hasShownHint && currentIndex * musicListColumnCount >= 14 && currentIndex > previousFirstVisibleIndex) {
                        hasShownHint = true
                        showDoubleTapHint = true
                    }
                    previousFirstVisibleIndex = currentIndex
                }

                fun requestLocatePlayingSong(path: String) {
                    val directIndex = displayAudioFiles.indexOfFirst { it.path == path }
                    if (directIndex >= 0) {
                        pendingLocateAudioPath = path
                        return
                    }

                    val existsInLibrary = allAudioFiles.any { it.path == path }
                    if (!existsInLibrary) {
                        Toast.makeText(context, context.getString(R.string.ml_music_library_current_song_not_in_list), Toast.LENGTH_SHORT).show()
                        return
                    }

                    pendingLocateAudioPath = path
                    if (searchQuery.isNotBlank()) {
                        searchQuery = ""
                        updateDisplayFiles()
                    }
                }

                fun triggerLocateHighlight(path: String) {
                    locateHighlightJob?.cancel()
                    locateHighlightJob = scope.launch {
                        locateHighlightPath = path
                        locateHighlightActive = false
                        repeat(2) {
                            locateHighlightActive = true
                            delay(180L)
                            locateHighlightActive = false
                            delay(140L)
                        }
                        locateHighlightPath = null
                    }
                }

                LaunchedEffect(
                    pendingLocateAudioPath,
                    isUpdatingDisplayList,
                    displayAudioFiles.size,
                    musicListColumnCount,
                    listState.firstVisibleItemIndex
                ) {
                    val targetPath = pendingLocateAudioPath ?: return@LaunchedEffect
                    if (isUpdatingDisplayList || displayAudioFiles.isEmpty()) return@LaunchedEffect

                    val targetIndex = displayAudioFiles.indexOfFirst { it.path == targetPath }
                    if (targetIndex < 0) return@LaunchedEffect
                    val targetRowIndex = targetIndex / musicListColumnCount

                    val viewportHeightPx = listState.layoutInfo.viewportSize.height
                    val desiredUpperBandMin = (viewportHeightPx * 0.18f).toInt()
                    val desiredUpperBandMax = (viewportHeightPx * 0.42f).toInt()
                    val estimatedItemHeightPx = with(density) { 84.dp.roundToPx().coerceAtLeast(1) }
                    val leadRowCount = ((viewportHeightPx * 0.30f).toInt() / estimatedItemHeightPx).coerceAtLeast(2)
                    val anchorIndex = (targetRowIndex - leadRowCount).coerceAtLeast(0)

                    val visibleRange = listState.layoutInfo.visibleItemsInfo
                    val currentTargetInfo = visibleRange.firstOrNull { it.index == targetRowIndex }
                    val inDesiredBand = currentTargetInfo?.offset?.let { it in desiredUpperBandMin..desiredUpperBandMax } == true

                    if (!inDesiredBand) {
                        listState.scrollToItem(anchorIndex)
                        listState.animateScrollToItem(anchorIndex)
                    }

                    triggerLocateHighlight(targetPath)
                    pendingLocateAudioPath = null
                }
                
                val headbarTitle = when {
                    isMultiSelectMode -> stringResource(R.string.ml_music_library_selected_count_short, selectedPaths.size)
                    showDoubleTapHint -> stringResource(R.string.ml_music_library_double_tap_back_to_top)
                    else -> "LunaBeat"
                }
                
                CommonHeadBar(
                    title = headbarTitle,
                    showBack = true,
                    leadingIconResId = R.drawable.menu,
                    showMenu = true,
                    onBackClick = { pageMenuExpanded = true },
                    leadingMenuContent = { backButtonPosition ->
                        CustomDropdownMenu(
                            expanded = pageMenuExpanded,
                            onDismissRequest = { pageMenuExpanded = false },
                            items = buildList {
                                val artistMenuItem = MenuItem(
                                    title = stringResource(R.string.ml_music_library_artist_menu_title),
                                    onClick = {
                                        pageMenuExpanded = false
                                        exitMultiSelectMode()
                                        internalPage = MusicLibraryInternalPage.ARTISTS
                                        internalArtistName = ""
                                        internalAlbumName = ""
                                        internalAlbumReturnPage = MusicLibraryInternalPage.SONGS
                                    }
                                )
                                val pageSwitchItems = buildPageSwitchMenuItems(
                                    context = context,
                                    currentPage = AppPageDestination.MUSIC_LIBRARY,
                                    includeCurrentPage = false
                                )
                                pageSwitchItems.forEach { item ->
                                    val internalItem = when (item.title) {
                                        AppPageDestination.SETTINGS.title -> item.copy(
                                            onClick = {
                                                pageMenuExpanded = false
                                                exitMultiSelectMode()
                                                closeInlineLyricPreview()
                                                internalPage = MusicLibraryInternalPage.SETTINGS
                                            }
                                        )
                                        AppPageDestination.ABOUT.title -> item.copy(
                                            onClick = {
                                                pageMenuExpanded = false
                                                exitMultiSelectMode()
                                                closeInlineLyricPreview()
                                                internalPage = MusicLibraryInternalPage.ABOUT
                                            }
                                        )
                                        else -> item
                                    }
                                    add(internalItem)
                                    if (item.title == AppPageDestination.LYRIC_TIMING.title) {
                                        add(artistMenuItem)
                                    }
                                }
                                if (pageSwitchItems.none { it.title == AppPageDestination.LYRIC_TIMING.title }) {
                                    add(0, artistMenuItem)
                                }
                            },
                            anchorPosition = backButtonPosition ?: MenuAnchorPosition(0f, 0f)
                        )
                    },
                    onTitleClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 300) {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                            showDoubleTapHint = false
                        }
                        lastClickTime = currentTime
                    },
                    onMenuClick = { menuExpanded = true },
                    menuContent = { menuButtonPosition ->
                        val menuItems = if (isMultiSelectMode) {
                            listOf(
                                MenuItem(
                                    title = stringResource(R.string.ml_music_library_add_to_favorites),
                                    onClick = {
                                        menuExpanded = false
                                        val selectedSnapshot = selectedPaths.toList()
                                        if (selectedSnapshot.isNotEmpty()) {
                                            val addedCount = selectedSnapshot.count { it !in favoritePaths }
                                            favoritePaths.addAll(selectedSnapshot)
                                            persistFavoritesPlaylist()
                                            updateDisplayFiles()
                                            bulkFavoriteAddedCount = addedCount
                                            showBulkFavoriteResultDialog = true
                                        }
                                    }
                                )
                            )
                        } else {
                            buildList {
                                if (canLocatePlayingSong) {
                                    add(
                                        MenuItem(
                                            title = stringResource(R.string.ml_music_library_locate_current_song),
                                            onClick = {
                                                menuExpanded = false
                                                currentPlayingAudioPath?.let { requestLocatePlayingSong(it) }
                                            }
                                        )
                                    )
                                }
                                add(
                                    MenuItem(title = stringResource(R.string.ml_music_library_multi_select_mode), onClick = {
                                        menuExpanded = false
                                        isMultiSelectMode = true
                                    })
                                )
                                add(MenuItem(title = stringResource(R.string.ml_music_library_directory_settings), onClick = { onOpenSettings() }))
                                add(
                                    MenuItem(
                                        title = stringResource(R.string.ml_music_library_refresh),
                                        onClick = {
                                            isScanning = true
                                            showScanComplete = false
                                            showScanPopupImmediately()
                                            scanJob?.cancel()
                                            scanJob = scope.launch {
                                                scanAudioFiles(context, prefs, allAudioFiles,
                                                    onProgress = { current, total ->
                                                        scanProgress = current to total
                                                    },
                                                onComplete = { summary ->
                                                    isScanning = false
                                                    hideScanPopupImmediately()
                                                    scanSummary = summary
                                                    if (shouldShowScanComplete(summary)) {
                                                        showScanComplete = true
                                                        scope.launch {
                                                            kotlinx.coroutines.delay(3000)
                                                            showScanComplete = false
                                                        }
                                                    } else {
                                                        showScanComplete = false
                                                    }
                                                    updateDisplayFiles()
                                                }
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                    CustomDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        items = menuItems,
                        anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f)
                    )
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .lyricHorizontalSafeDrawingPadding()
            ) {
                MusicLibraryBarSwitch(
                    isMultiSelectMode = isMultiSelectMode,
                    multiSelectActionBar = {
                        val rangeSelectEnabled = lastSelectedIndices.size >= 2
                        MultiSelectActionBar(
                            onSelectAll = { selectAll() },
                            onInvertSelection = { invertSelection() },
                            onClearSelection = { clearSelection() },
                            onRangeSelect = {
                                if (rangeSelectEnabled) {
                                    val sorted = lastSelectedIndices.sorted()
                                    selectRange(sorted[0], sorted[1])
                                }
                            },
                            rangeSelectEnabled = rangeSelectEnabled,
                            onExit = { exitMultiSelectMode() }
                        )
                    },
                    searchBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val historyChipItems = remember(recentSearchHistory) {
                            buildList {
                                add(FAVORITE_SEARCH_TOKEN)
                                addAll(recentSearchHistory.take(SEARCH_HISTORY_LIMIT).filter {
                                    !it.equals(FAVORITE_SEARCH_TOKEN, ignoreCase = true)
                                })
                            }.distinct().take(SEARCH_HISTORY_LIMIT + 1)
                        }
                        val rotation by animateFloatAsState(
                            targetValue = if (showSearchHistoryPopup) 180f else 0f,
                            animationSpec = tween(180),
                            label = "searchHistoryArrowRotation"
                        )
                        var searchBarWidthPx by remember { mutableIntStateOf(0) }
                        var searchBarHeightPx by remember { mutableIntStateOf(0) }
                        val showHistoryPanel = showSearchHistoryPopup && historyChipItems.isNotEmpty()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    searchBarWidthPx = coordinates.size.width
                                    searchBarHeightPx = coordinates.size.height
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    updateDisplayFiles()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .onFocusChanged { focusState ->
                                        searchBoxFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            showSearchHistoryPopup = true
                                            keyboardController?.show()
                                        }
                                    },
                                singleLine = true,
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
                                            painter = painterResource(id = R.drawable.search),
                                            contentDescription = stringResource(R.string.ml_music_library_search),
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.ml_music_library_search_hint),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                                    fontSize = 16.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    showSearchHistoryPopup = true
                                                    searchFocusRequester.requestFocus()
                                                    keyboardController?.show()
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.down),
                                                    contentDescription = stringResource(R.string.ml_music_library_recent_search),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .graphicsLayer {
                                                            rotationZ = rotation
                                                        }
                                                )
                                            }
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = {
                                                        searchQuery = ""
                                                        updateDisplayFiles()
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = stringResource(R.string.ml_music_library_clear),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        if (showHistoryPanel) {
                            val popupOffsetY = searchBarHeightPx + with(density) { 4.dp.roundToPx() }
                            Popup(
                                alignment = Alignment.TopStart,
                                offset = IntOffset(x = 0, y = popupOffsetY),
                                onDismissRequest = { showSearchHistoryPopup = false },
                                properties = PopupProperties(
                                    focusable = false,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(with(density) { searchBarWidthPx.toDp() })
                                        .heightIn(max = 360.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        historyChipItems.forEach { chipText ->
                                            if (chipText == FAVORITE_SEARCH_TOKEN) {
                                                SuggestionChip(
                                                    onClick = {
                                                        searchQuery = chipText
                                                        searchQueryApplied = chipText.trim()
                                                        updateDisplayFiles()
                                                        showSearchHistoryPopup = false
                                                    },
                                                    label = { Text(chipText) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            } else {
                                                SuggestionChip(
                                                    onClick = {
                                                        searchQuery = chipText
                                                        searchQueryApplied = chipText.trim()
                                                        updateDisplayFiles()
                                                        showSearchHistoryPopup = false
                                                        val updatedHistory = recentSearchHistory.toMutableList()
                                                        appendRecentSearchHistory(prefs, updatedHistory, chipText)
                                                        recentSearchHistory = updatedHistory
                                                    },
                                                    label = {
                                                        Row(
                                                            modifier = Modifier.widthIn(max = 240.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = chipText,
                                                                modifier = Modifier.weight(1f, fill = false),
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                lineHeight = 16.sp
                                                            )
                                                            Icon(
                                                                imageVector = Icons.Rounded.Close,
                                                                contentDescription = stringResource(R.string.ml_music_library_delete_history),
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier
                                                                    .size(14.dp)
                                                                    .clickable(
                                                                        indication = null,
                                                                        interactionSource = remember { MutableInteractionSource() }
                                                                    ) {
                                                                        recentSearchHistory = recentSearchHistory
                                                                            .filterNot { it.equals(chipText, ignoreCase = true) }
                                                                        saveRecentSearchHistory(prefs, recentSearchHistory)
                                                                    }
                                                            )
                                                        }
                                                    },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
            
            val shouldShowLibraryLoading =
                isLoadingCache || !isInitialListFadeReady || !hasCompletedInitialDisplayBuild
            val shouldShowScanBootstrapLoading =
                !shouldShowLibraryLoading && isScanning && displayAudioFiles.isEmpty()
            val shouldShowDisplayBuildLoading =
                !shouldShowLibraryLoading && isUpdatingDisplayList && displayAudioFiles.isEmpty()
            val shouldShowMusicListLoading =
                shouldShowLibraryLoading || shouldShowScanBootstrapLoading || shouldShowDisplayBuildLoading

            if (shouldShowMusicListLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(56.dp))
                }
            }

            AnimatedVisibility(
                visible = !shouldShowMusicListLoading,
                enter = fadeIn(animationSpec = tween(durationMillis = 320)),
                exit = fadeOut(animationSpec = tween(durationMillis = 120))
            ) {
                if (displayAudioFiles.isEmpty() && !isScanning && hasCompletedInitialDisplayBuild && !isUpdatingDisplayList) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_folder_open_24),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) stringResource(R.string.ml_music_library_no_matched_songs) else stringResource(R.string.ml_music_library_no_audio_files),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.ml_music_library_check_directory_settings),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else if (displayAudioFiles.isNotEmpty() || isScanning) {
                    var isDragging by remember { mutableStateOf(false) }
                    var dragProgress by remember { mutableStateOf(0f) }
                    var targetScrollbarAlpha by remember { mutableStateOf(1f) }
                    val animatedScrollbarAlpha by animateFloatAsState(
                        targetValue = targetScrollbarAlpha,
                        animationSpec = tween(durationMillis = 300),
                        label = "scrollbarAlpha"
                    )
                    var hideTimerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                    
                    fun resetHideTimer() {
                        hideTimerJob?.cancel()
                        targetScrollbarAlpha = 1f
                        hideTimerJob = scope.launch {
                            delay(1000)
                            targetScrollbarAlpha = 0.2f
                        }
                    }
                    
                    LaunchedEffect(Unit) {
                        resetHideTimer()
                    }
                    
                    LaunchedEffect(
                        listState.firstVisibleItemIndex,
                        listState.firstVisibleItemScrollOffset,
                        displayAudioFiles.size,
                        musicListColumnCount
                    ) {
                        resetHideTimer()
                        if (!isDragging) {
                            val totalItems = (displayAudioFiles.size + musicListColumnCount - 1) / musicListColumnCount
                            val viewportHeight = listState.layoutInfo.viewportSize.height
                            val itemHeight = 80
                            val maxScrollIndex = (totalItems - (viewportHeight / itemHeight)).coerceAtLeast(0)
                            if (maxScrollIndex > 0) {
                                dragProgress = listState.firstVisibleItemIndex.toFloat() / maxScrollIndex.toFloat()
                            } else {
                                dragProgress = 0f
                            }
                        }
                    }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
                        val bottomPadding = navigationBarsPadding.calculateBottomPadding() + 8.dp + miniPlayerExtraBottomPadding
                        val displayAudioRows = displayAudioFiles.chunked(musicListColumnCount)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = bottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                displayAudioRows,
                                key = { rowIndex, row ->
                                    "$rowIndex:${row.joinToString("|") { it.path }}"
                                }
                            ) { rowIndex, rowAudios ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowAudios.forEachIndexed { columnIndex, audio ->
                                        val itemIndex = rowIndex * musicListColumnCount + columnIndex
                                        val index = itemIndex + 1
                                        val isSelected = selectedPaths.contains(audio.path)
                                        AudioFileItem(
                                            audio = audio,
                                            modifier = Modifier.weight(1f),
                                            isInMultiSelectMode = isMultiSelectMode,
                                            isSelected = isSelected,
                                            isLocateHighlightActive = locateHighlightActive && locateHighlightPath == audio.path,
                                            sequenceNumber = if (isMultiSelectMode) index else null,
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    if (selectedPaths.contains(audio.path)) {
                                                        selectedPaths.remove(audio.path)
                                                        lastSelectedIndices.remove(index)
                                                    } else {
                                                        selectedPaths.add(audio.path)
                                                        lastSelectedIndices.add(index)
                                                        if (lastSelectedIndices.size > 2) {
                                                            lastSelectedIndices.removeAt(0)
                                                        }
                                                    }
                                                } else {
                                                    if (!hasConfirmedSongClickAction) {
                                                        pendingSongClickAudio = audio
                                                        tempSongClickAction = ""
                                                        tempAutoDetectEmbeddedLyricsType = autoDetectEmbeddedLyricsType
                                                        showSongClickActionDialog = true
                                                    } else {
                                                        executePrimarySongAction(audio)
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (!isMultiSelectMode) {
                                                    isMultiSelectMode = true
                                                    selectedPaths.add(audio.path)
                                                    lastSelectedIndices.clear()
                                                    lastSelectedIndices.add(index)
                                                }
                                            },
                                            onSelectionChange = { selected ->
                                                if (selected) {
                                                    selectedPaths.add(audio.path)
                                                    lastSelectedIndices.add(index)
                                                    if (lastSelectedIndices.size > 2) {
                                                        lastSelectedIndices.removeAt(0)
                                                    }
                                                } else {
                                                    selectedPaths.remove(audio.path)
                                                    lastSelectedIndices.remove(index)
                                                }
                                            },
                                            onPlayClick = if (isMultiSelectMode) {
                                                null
                                            } else if (songClickAction == "playMusic") {
                                                null
                                            } else {
                                                {
                                                    revealMiniPlayerForUserPlayback()
                                                    playbackController.playQueue(displayAudioFiles.toList(), audio.path)
                                                }
                                            },
                                            onMoreClick = if (isMultiSelectMode) {
                                                null
                                            } else {
                                                {
                                                    selectedSongInfoAudio = audio
                                                    showSongInfoSheet = true
                                                }
                                            }
                                        )
                                    }
                                    repeat(musicListColumnCount - rowAudios.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        
                        val totalItems = displayAudioRows.size
                        if (totalItems > 0) {
                            val viewportHeight = listState.layoutInfo.viewportSize.height
                            val itemHeight = 80
                            val totalContentHeight = totalItems * itemHeight
                            val thumbHeightRatio = viewportHeight.toFloat() / totalContentHeight.coerceAtLeast(viewportHeight)
                            val thumbHeightPx = (viewportHeight * thumbHeightRatio).coerceAtLeast(80f)
                            
                            val maxScrollIndex = (totalItems - (viewportHeight / itemHeight)).coerceAtLeast(0)
                            
                            val density = LocalDensity.current
                            val scrollbarHeightPx = (viewportHeight - 16 - with(density) { 48.dp.toPx() }).coerceAtLeast(1f)
                            val scrollRangePx = (scrollbarHeightPx - thumbHeightPx).coerceAtLeast(1f)
                            val thumbOffsetY = dragProgress.coerceIn(0f, 1f) * scrollRangePx
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp, top = 8.dp, bottom = 48.dp)
                                    .width(24.dp)
                                    .height(with(density) { scrollbarHeightPx.toDp() })
                                    .graphicsLayer {
                                        alpha = animatedScrollbarAlpha
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
                                                if (isDragging) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                }
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(maxScrollIndex, scrollbarHeightPx, thumbHeightPx) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        resetHideTimer()
                                                        isDragging = true
                                                    },
                                                    onDragEnd = {
                                                        isDragging = false
                                                        resetHideTimer()
                                                    },
                                                    onDragCancel = {
                                                        isDragging = false
                                                        resetHideTimer()
                                                    }
                                                ) { change, dragAmount ->
                                                    change.consume()
                                                    resetHideTimer()
                                                    val delta = dragAmount.y / scrollRangePx
                                                    dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                                                    val targetIndex = (dragProgress * maxScrollIndex).toInt()
                                                        .coerceIn(0, maxScrollIndex)
                                                    scope.launch {
                                                        listState.scrollToItem(targetIndex)
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }
                        
                        // EdgeTranslucent 顶部渐变效果
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface,
                                            androidx.compose.ui.graphics.Color.Transparent
                                        )
                                    )
                                )
                        )
                        
                        BatchOperationFAB(
                            isVisible = isMultiSelectMode && selectedPaths.isNotEmpty(),
                            showSheet = showBatchMenu,
                            onShowSheetChange = { showBatchMenu = it },
                            onBatchMatch = { showBatchMatchConfig = true },
                            onBatchLyricMatch = { showBatchLyricMatchConfig = true },
                            onBatchRename = { showBatchRenameConfig = true },
                            onBatchLyricsEdit = { showBatchLyricsEditSheet = true },
                            selectedPaths = selectedPaths,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp)
                                .navigationBarsPadding()
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
        }

        val embeddedArtistAudioFiles = allAudioFiles.toList()
        val embeddedMiniPlayerPadding = if (showMiniPlayer && !isMultiSelectMode) {
            miniPlayerHeight + 12.dp
        } else {
            0.dp
        }
        AnimatedContent(
            targetState = internalPage,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(internalPageBackdrop),
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) togetherWith
                    fadeOut(animationSpec = tween(160))).using(
                    SizeTransform(clip = false) { _, _ -> tween(durationMillis = 0) }
                )
            },
            label = "musicLibraryInternalPage"
        ) { page ->
            when (page) {
                MusicLibraryInternalPage.SONGS -> Box(Modifier.fillMaxSize())
                MusicLibraryInternalPage.ARTISTS -> {
                    EmbeddedArtistsScreen(
                        audioFiles = embeddedArtistAudioFiles,
                        isLoading = isLoadingCache && embeddedArtistAudioFiles.isEmpty(),
                        miniPlayerExtraBottomPadding = embeddedMiniPlayerPadding,
                        artistListState = internalArtistsListState,
                        onBack = {
                            internalPage = MusicLibraryInternalPage.SONGS
                            internalArtistName = ""
                            internalAlbumName = ""
                            internalAlbumReturnPage = MusicLibraryInternalPage.SONGS
                        },
                        onArtistClick = { artistName ->
                            openInternalArtistDetail(artistName)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MusicLibraryInternalPage.ARTIST_DETAIL -> {
                    EmbeddedArtistDetailScreen(
                        audioFiles = embeddedArtistAudioFiles,
                        artistName = internalArtistName,
                        miniPlayerExtraBottomPadding = embeddedMiniPlayerPadding,
                        onBack = {
                            internalPage = MusicLibraryInternalPage.ARTISTS
                            internalAlbumName = ""
                            internalAlbumReturnPage = MusicLibraryInternalPage.SONGS
                        },
                        onSongClick = { queue, audio ->
                            revealMiniPlayerForUserPlayback()
                            playbackController.playQueue(queue, audio.path)
                        },
                        onSongMoreClick = { audio ->
                            selectedSongInfoAudio = audio
                            showSongInfoSheet = true
                        },
                        selectedTab = internalArtistDetailTab,
                        onTabSelected = { tab ->
                            internalArtistDetailTab = tab
                        },
                        onAlbumClick = { albumName ->
                            openInternalAlbumDetail(albumName)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MusicLibraryInternalPage.ALBUM_DETAIL -> {
                    EmbeddedAlbumDetailScreen(
                        audioFiles = embeddedArtistAudioFiles,
                        albumName = internalAlbumName,
                        miniPlayerExtraBottomPadding = embeddedMiniPlayerPadding,
                        onBack = {
                            closeInternalAlbumDetail()
                        },
                        onSongClick = { queue, audio ->
                            revealMiniPlayerForUserPlayback()
                            playbackController.playQueue(queue, audio.path)
                        },
                        onSongMoreClick = { audio ->
                            selectedSongInfoAudio = audio
                            showSongInfoSheet = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MusicLibraryInternalPage.SETTINGS -> {
                    SettingsScreen(
                        onBack = {
                            internalPage = MusicLibraryInternalPage.SONGS
                        },
                        onNavigateToMusicLibrarySettings = onOpenSettings,
                        onNavigateToCustomMetadataFieldsSettings = {
                            context.startActivity(Intent(context, CustomMetadataFieldsSettingsActivity::class.java))
                        },
                        bottomContentPadding = embeddedMiniPlayerPadding,
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutPadding()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
                MusicLibraryInternalPage.ABOUT -> {
                    val isDebugDevice = remember { PiracyChecker.isDebugDevice(context) }
                    AboutScreen(
                        onBack = {
                            internalPage = MusicLibraryInternalPage.SONGS
                        },
                        isDebugDevice = isDebugDevice,
                        bottomContentPadding = embeddedMiniPlayerPadding,
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutPadding()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
        
        val fabHeight = 56.dp
        val showScanPanel = showScanProgressPopup || showScanComplete
        val fabOffset by animateDpAsState(
            targetValue = if (showScanPanel && isMultiSelectMode) fabHeight + 8.dp else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "fabOffset"
        )
        
        AnimatedVisibility(
            visible = showScanPanel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (showMiniPlayer && !isMultiSelectMode) miniPlayerHeight + 16.dp else 8.dp
                )
                .offset(y = -fabOffset),
            enter = fadeIn(animationSpec = tween(400)) + 
                     scaleIn(
                         initialScale = 0.3f,
                         transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center,
                         animationSpec = spring(
                             dampingRatio = Spring.DampingRatioMediumBouncy,
                             stiffness = Spring.StiffnessLow
                         )
                     ) + slideInVertically(
                         initialOffsetY = { it },
                         animationSpec = spring(
                             dampingRatio = Spring.DampingRatioMediumBouncy,
                             stiffness = Spring.StiffnessLow
                         )
                     ),
            exit = fadeOut(animationSpec = tween(200)) + 
                   scaleOut(
                       targetScale = 0.5f,
                       transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center,
                       animationSpec = tween(200)
                   ) + slideOutVertically(
                       targetOffsetY = { it },
                       animationSpec = tween(200)
                   )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showScanProgressPopup && isScanning,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) + slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(200)
                        ) togetherWith fadeOut(animationSpec = tween(200)) + slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(200)
                        )
                    },
                    label = "scanState"
                ) { isScanningState ->
                    if (isScanningState) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(min = 60.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_scanning_audio),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (scanProgress.second > 0) {
                                        Text(
                                            text = "${scanProgress.first}/${scanProgress.second}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    progress = { if (scanProgress.second > 0) scanProgress.first.toFloat() / scanProgress.second else 0f },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(min = 60.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.ml_music_library_scan_summary,
                                    scanSummary.totalCount,
                                    scanSummary.addedCount,
                                    scanSummary.removedCount,
                                    scanSummary.updatedCount
                                ),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        }
        val miniPlayerDockAlignment = if (useWideMiniPlayerDock) {
            when (miniPlayerLandscapeAlignment) {
                MINI_PLAYER_LANDSCAPE_ALIGNMENT_CENTER -> Alignment.BottomCenter
                MINI_PLAYER_LANDSCAPE_ALIGNMENT_START -> Alignment.BottomStart
                else -> Alignment.BottomEnd
            }
        } else {
            Alignment.BottomCenter
        }
        AnimatedVisibility(
            visible = showMiniPlayer && !isMultiSelectMode,
            modifier = Modifier
                .align(miniPlayerDockAlignment)
                .navigationBarsPadding()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 8.dp
                )
                .then(
                    if (useWideMiniPlayerDock) {
                        Modifier.widthIn(max = 420.dp)
                    } else {
                        Modifier
                    }
                ),
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            val miniPlayerTravelDistancePx = resolveMiniPlayerTravelDistancePx()
            val miniPlayerTranslateY = -miniPlayerExpandProgress * miniPlayerTravelDistancePx
            val miniPlayerAlpha = (1f - miniPlayerExpandProgress * 0.9f).coerceIn(0f, 1f)
            GlobalMiniPlayerBar(
                controller = playbackController,
                onExpandTouchDown = {
                    val currentPath = playbackController.currentAudioPath
                    if (currentPath.isNullOrBlank()) return@GlobalMiniPlayerBar
                    refreshInlineLyricDisplaySelectedFromPrefs()
                    miniBarBaseTop = miniBarBounds?.top ?: miniBarBaseTop
                    if (!showInlineLyricPreview) {
                        showInlineLyricPreview = true
                    }
                },
                expandProgress = miniPlayerExpandProgress,
                sharedCoverId = sharedCoverId,
                coverAlpha = 1f,
                backgroundStyle = miniPlayerBackgroundStyle,
                backdrop = combinedMiniPlayerBackdrop,
                onBarBoundsChanged = { bounds ->
                    miniBarBounds = bounds
                    if (miniPlayerExpandProgress <= 0.01f || miniBarBaseTop == null) {
                        miniBarBaseTop = bounds.top
                    }
                },
                onCoverBitmapChanged = { bitmap ->
                    miniPlayerCoverBitmap = bitmap
                },
                onExpandDragDelta = { deltaY ->
                    val currentPath = playbackController.currentAudioPath
                    if (currentPath.isNullOrBlank()) return@GlobalMiniPlayerBar
                    if (deltaY <= 0f || miniPlayerExpandProgress > 0f) {
                        updateMiniPlayerDragProgress(deltaY)
                    }
                },
                onExpandDragEnd = {
                    if (playbackController.currentAudioPath.isNullOrBlank()) {
                        miniPlayerExpandProgress = 0f
                        showInlineLyricPreview = false
                    } else {
                        settleMiniPlayerExpandState()
                    }
                },
                onExpandDragCancel = {
                    if (playbackController.currentAudioPath.isNullOrBlank()) {
                        miniPlayerExpandProgress = 0f
                        showInlineLyricPreview = false
                    } else {
                        settleMiniPlayerExpandState()
                    }
                },
                onHideRequest = {
                    playbackController.persistCurrentPlaybackState()
                    setMiniPlayerHidden(true)
                },
                onExpand = {
                    val currentPath = playbackController.currentAudioPath
                    if (currentPath.isNullOrBlank()) {
                        MusicPlayerActivity.start(context)
                    } else {
                        openInlineLyricPreview()
                    }
                },
                modifier = Modifier
                    .offset { IntOffset(0, miniPlayerTranslateY.toInt()) }
                    .graphicsLayer { alpha = miniPlayerAlpha }
            )
        }

        AnimatedVisibility(
            visible = showInlineLyricPreview || miniPlayerExpandProgress > 0f,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 220))
        ) {
            val miniPlayerTravelDistancePx = resolveMiniPlayerTravelDistancePx()
            val previewTranslateY = miniPlayerTravelDistancePx * (1f - miniPlayerExpandProgress)
            val currentPath = playbackController.currentAudioPath.orEmpty()
            val inlineCompanionSelected = isSameInlineAudioPath(
                playbackController.currentPlaybackUriPath,
                inlineCompanionAudioPath
            )
            val currentTitle = playbackController.currentTitle
                .ifBlank { File(currentPath).nameWithoutExtension }
                .ifBlank { context.getString(R.string.ml_music_library_lyrics_preview_title) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            playbackController.currentMediaId,
                            showInlineLyricPreview,
                            inlineLyricDisplaySelected,
                            screenConfig.orientation,
                            screenConfig.screenWidthDp,
                            screenConfig.screenHeightDp,
                            screenConfig.smallestScreenWidthDp
                        ) {
                            var handleTopDrag = false
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val canStartDrag = when {
                                        isInlinePreviewLandscape -> {
                                            if (isInlinePreviewLargeScreen && !inlineLyricDisplaySelected) {
                                                startOffset.y <= landscapeLargeScreenDragMaxY
                                            } else {
                                                val inLeftPane = startOffset.x <= (size.width / 2f)
                                                val inLyricRightBlankDismissArea =
                                                    inlineLyricDisplaySelected &&
                                                        startOffset.x >= (size.width * 0.86f)
                                                when {
                                                    inLeftPane -> {
                                                        if (isInlinePreviewLargeScreen) {
                                                            startOffset.y <= landscapeLargeScreenDragMaxY
                                                        } else {
                                                            true
                                                        }
                                                    }
                                                    inLyricRightBlankDismissArea -> {
                                                        if (isInlinePreviewLargeScreen) {
                                                            startOffset.y <= landscapeLargeScreenDragMaxY
                                                        } else {
                                                            true
                                                        }
                                                    }
                                                    else -> false
                                                }
                                            }
                                        }
                                        !inlineLyricDisplaySelected -> {
                                            startOffset.y <= portraitExpandedDragMaxY
                                        }
                                        else -> {
                                            startOffset.y <= previewTopDragHandleHeightPx
                                        }
                                    }
                                    handleTopDrag = miniPlayerExpandProgress > 0f && canStartDrag
                                },
                                onDragEnd = {
                                    if (handleTopDrag) {
                                        settleMiniPlayerExpandState()
                                    }
                                    handleTopDrag = false
                                },
                                onDragCancel = {
                                    if (handleTopDrag) {
                                        settleMiniPlayerExpandState()
                                    }
                                    handleTopDrag = false
                                }
                            ) { change, dragAmount ->
                                if (!handleTopDrag) return@detectDragGestures
                                if (dragAmount.y > 0f || miniPlayerExpandProgress < 1f) {
                                    change.consume()
                                    updateMiniPlayerDragProgress(dragAmount.y)
                                }
                            }
                        }
                        .graphicsLayer {
                            translationY = previewTranslateY
                        }
                ) {
                    LyricPreviewScreen(
                        title = currentTitle,
                        audioPath = currentPath,
                        sourceAudioPath = currentPath,
                        sourceMediaStoreId = playbackController.currentMediaStoreId,
                        initialCoverBitmap = miniPlayerCoverBitmap,
                        initialArtistText = playbackController.currentArtist.ifBlank { context.getString(R.string.ml_music_library_unknown_artist) },
                        lyricLines = lyricPreviewLines,
                        isLyricLoading = lyricPreviewLoading,
                        metadataRefreshToken = inlineMetadataRefreshToken,
                        applyInitialSeek = false,
                        creators = lyricPreviewCreators,
                        audioDuration = playbackController.durationMs.coerceAtLeast(0L),
                        initialPosition = playbackController.positionMs.coerceAtLeast(0L),
                        initialIsPlaying = playbackController.isPlaying,
                        onBack = { closeInlineLyricPreview() },
                        onPlayPause = { playing ->
                            if (playing) {
                                playbackController.play()
                            } else {
                                playbackController.pause()
                            }
                        },
                        onSkipPreviousTrack = { playbackController.skipToPrevious() },
                        onSkipNextTrack = { playbackController.skipToNext() },
                        canSkipNextTrack = playbackController.isReady &&
                            playbackController.mediaCount > 0 &&
                            playbackController.currentIndex >= 0,
                        onSeekTo = { position -> playbackController.seekTo(position) },
                        getCurrentPosition = {
                            playbackController.refreshProgress()
                            playbackController.positionMs
                        },
                        getIsPlayingState = { playbackController.isPlaying },
                        getAudioDuration = {
                            playbackController.refreshProgress()
                            playbackController.durationMs.coerceAtLeast(0L)
                        },
                        companionAvailable = !inlineCompanionAudioPath.isNullOrBlank(),
                        companionSelected = inlineCompanionSelected,
                        companionBusy = inlineCompanionSwitching,
                        playbackController = playbackController,
                        onCompanionToggle = { enabled ->
                            if (inlineCompanionSwitching) return@LyricPreviewScreen
                            val sourcePath = playbackController.currentAudioPath?.takeIf { it.isNotBlank() }
                                ?: return@LyricPreviewScreen
                            val companionPath = inlineCompanionAudioPath
                            val targetPath = if (enabled) companionPath else sourcePath
                            if (targetPath.isNullOrBlank()) return@LyricPreviewScreen
                            inlineCompanionSwitching = true
                            scope.launch {
                                runCatching {
                                    playbackController.switchCurrentAudioKeepingMetadata(
                                        expectedSourcePath = sourcePath,
                                        targetAudioPath = targetPath
                                    )
                                }
                                inlineCompanionSwitching = false
                            }
                        },
                        lyricDisplayResetToken = inlineLyricDisplayResetToken,
                        initialLyricDisplaySelected = inlineLyricDisplaySelected,
                        onLyricDisplaySelectedChanged = { selected ->
                            persistInlineLyricDisplaySelected(selected)
                        },
                        showChrome = true,
                        enableBackHandler = false,
                        sharedCoverId = sharedCoverId,
                        showCoverContent = true,
                        onPrimaryCoverBoundsChanged = null,
                        onApplyPreviewLyricsContent = { lyricsContent ->
                            applyInlineReturnedLyricsContent(lyricsContent)
                        },
                        onRefreshPreviewData = {
                            scope.launch {
                                reloadInlineLyricPreviewFromFile(reason = "previewScreen", force = true)
                            }
                        }
                    )
                }
            }
        }
    }
    
    if (showAudioOptionsDialog && selectedAudio != null) {
        val dialogAudio = selectedAudio!!
        AudioOptionsDialog(
            audio = dialogAudio,
            autoDetectEmbeddedLyricsType = autoDetectEmbeddedLyricsType,
            onDismiss = {
                showAudioOptionsDialog = false
                selectedAudio = null
            },
            onEditLyrics = { lyricsContent, format ->
                showAudioOptionsDialog = false
                startLyricTimingEditor(
                    audio = dialogAudio,
                    lyricsContent = lyricsContent,
                    format = format
                )
            },
            onEditMetadata = { path ->
                onEditMetadata(path)
            }
        )
    }

    if (showSongClickActionDialog) {
        SongClickActionDialog(
            currentValue = tempSongClickAction,
            onValueChange = { tempSongClickAction = it },
            autoDetectEmbeddedLyricsType = tempAutoDetectEmbeddedLyricsType,
            onAutoDetectEmbeddedLyricsTypeChange = { tempAutoDetectEmbeddedLyricsType = it },
            onDismiss = {
                showSongClickActionDialog = false
                pendingSongClickAudio = null
            },
            onConfirm = {
                if (tempSongClickAction.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.ml_music_library_select_default_action), Toast.LENGTH_SHORT).show()
                } else {
                    songClickAction = tempSongClickAction
                    hasConfirmedSongClickAction = true
                    autoDetectEmbeddedLyricsType = tempAutoDetectEmbeddedLyricsType
                    prefs.edit()
                        .putString("songClickAction", tempSongClickAction)
                        .putBoolean(PREF_SONG_CLICK_ACTION_CONFIRMED, true)
                        .putBoolean("autoDetectEmbeddedLyricsType", tempAutoDetectEmbeddedLyricsType)
                        .apply()
                    showSongClickActionDialog = false
                    pendingSongClickAudio?.let { executePrimarySongAction(it) }
                    pendingSongClickAudio = null
                }
            }
        )
    }

    if (showSongInfoSheet && selectedSongInfoAudio != null) {
        val infoAudio = selectedSongInfoAudio!!
        SongInfoBottomSheet(
            audio = infoAudio,
            isFavorite = infoAudio.path in favoritePaths,
            isSleepTimerRunning = playbackController.sleepTimerState.isActive,
            renameSuccessSignal = renameSuccessSignal,
            onDismiss = {
                showSongInfoSheet = false
                selectedSongInfoAudio = null
            },
            onPlayNext = {
                playbackController.insertNext(infoAudio)
            },
            onViewAlbum = { albumName ->
                showSongInfoSheet = false
                openInternalAlbumDetail(albumName)
            },
            onViewArtists = { albumName, artists ->
                if (artists.isNotEmpty() || albumName.isNotBlank()) {
                    pendingAlbumCandidate = albumName
                    pendingArtistCandidates = artists
                    showSongInfoSheet = false
                    showArtistSelectionSheet = true
                } else {
                    Toast.makeText(context, context.getString(R.string.ml_music_library_artist_not_read), Toast.LENGTH_SHORT).show()
                }
            },
            onToggleFavorite = {
                if (infoAudio.path in favoritePaths) {
                    favoritePaths.remove(infoAudio.path)
                } else {
                    favoritePaths.add(infoAudio.path)
                }
                persistFavoritesPlaylist()
                updateDisplayFiles()
            },
            onRenameFile = {
                val sourceFile = File(infoAudio.path)
                renameInputValue = sourceFile.nameWithoutExtension
                renameFileExtension = sourceFile.extension
                    .takeIf { it.isNotBlank() }
                    ?.let { ".$it" }
                    ?: ""
                showRenameDialog = true
            },
            onDeleteFile = {
                showDeleteConfirmDialog = true
            },
            onEditMetadataFromSheet = { audioToEdit ->
                onEditMetadata(audioToEdit.path)
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
            waitingForSongEnd = playbackController.sleepTimerState.waitingForSongEnd,
            onDismiss = { showSleepTimerSheet = false },
            onStartTimer = { minutes, finishCurrentSong ->
                playbackController.startSleepTimer(minutes = minutes, finishCurrentSong = finishCurrentSong)
            },
            onCancelTimer = {
                playbackController.cancelSleepTimer()
            }
        )
    }

    if (showArtistSelectionSheet) {
        ArtistSelectionBottomSheet(
            albumName = pendingAlbumCandidate,
            artists = pendingArtistCandidates,
            onDismiss = { showArtistSelectionSheet = false },
            onSelectAlbum = { albumName ->
                openInternalAlbumDetail(albumName)
                showArtistSelectionSheet = false
                showSongInfoSheet = false
            },
            onSelectArtist = { artist ->
                openInternalArtistDetail(artist)
                showArtistSelectionSheet = false
                showSongInfoSheet = false
            }
        )
    }

    if (showBulkFavoriteResultDialog) {
        AlertDialog(
            onDismissRequest = { showBulkFavoriteResultDialog = false },
            title = { Text(stringResource(R.string.ml_music_library_tip_title)) },
            text = { Text(stringResource(R.string.ml_music_library_added_to_favorites_result, bulkFavoriteAddedCount)) },
            confirmButton = {
                TextButton(onClick = { showBulkFavoriteResultDialog = false }) {
                    Text(stringResource(R.string.ml_music_library_confirm))
                }
            }
        )
    }

    if (showRenameDialog && selectedSongInfoAudio != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.ml_music_library_rename_file_title)) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = renameInputValue,
                        onValueChange = { renameInputValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp
                        ),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (renameInputValue.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_input_file_name),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sourceAudio = selectedSongInfoAudio ?: return@TextButton
                        val oldFile = File(sourceAudio.path)
                        val newBaseName = renameInputValue.trim()
                        if (newBaseName.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.ml_music_library_file_name_empty), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val newName = newBaseName + renameFileExtension
                        val parent = oldFile.parentFile
                        if (parent == null) {
                            Toast.makeText(context, context.getString(R.string.ml_music_library_invalid_directory), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val newFile = File(parent, newName)
                        if (newFile.exists()) {
                            Toast.makeText(context, context.getString(R.string.ml_music_library_target_file_exists), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val renamed = runCatching { oldFile.renameTo(newFile) }.getOrDefault(false)
                        if (!renamed) {
                            Toast.makeText(context, context.getString(R.string.ml_music_library_rename_failed), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        runCatching {
                            val oldTtml = File(parent, "${oldFile.nameWithoutExtension}.ttml")
                            if (oldTtml.exists()) {
                                val newTtml = File(parent, "${newFile.nameWithoutExtension}.ttml")
                                oldTtml.renameTo(newTtml)
                            }
                        }

                        val idx = allAudioFiles.indexOfFirst { it.path == sourceAudio.path }
                        if (idx >= 0) {
                            val metadata = runCatching {
                                com.example.LyricBox.utils.AudioMetadataReader.readMetadata(context, newFile.absolutePath)
                            }.getOrNull()
                            allAudioFiles[idx] = sourceAudio.copy(
                                path = newFile.absolutePath,
                                title = metadata?.title ?: sourceAudio.title,
                                artist = metadata?.artist ?: sourceAudio.artist,
                                album = metadata?.album ?: sourceAudio.album,
                                duration = metadata?.duration ?: sourceAudio.duration,
                                fileSize = newFile.length(),
                                lastModified = newFile.lastModified(),
                                year = metadata?.year ?: sourceAudio.year,
                                mediaStoreId = -1L
                            )
                        }
                        if (sourceAudio.path in favoritePaths) {
                            favoritePaths.remove(sourceAudio.path)
                            favoritePaths.add(newFile.absolutePath)
                            persistFavoritesPlaylist()
                        }
                        playbackController.handleAudioRenamed(
                            oldPath = sourceAudio.path,
                            newPath = newFile.absolutePath
                        )
                        selectedPaths.remove(sourceAudio.path)
                        selectedPaths.add(newFile.absolutePath)
                        albumTrackSortCache.remove(sourceAudio.path)
                        saveCachedAudioFiles(context, allAudioFiles)
                        updateDisplayFiles()
                        selectedSongInfoAudio = allAudioFiles.find { it.path == newFile.absolutePath }
                        showRenameDialog = false
                        renameSuccessSignal = System.currentTimeMillis()
                    }
                ) {
                    Text(stringResource(R.string.ml_music_library_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
            }
        )
    }

    if (showDeleteConfirmDialog && selectedSongInfoAudio != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.ml_music_library_delete_file_title)) },
            text = { Text(stringResource(R.string.ml_music_library_delete_file_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = selectedSongInfoAudio ?: return@TextButton
                        val file = File(target.path)
                        val deleted = runCatching { file.delete() }.getOrDefault(false)
                        if (!deleted) {
                            Toast.makeText(context, context.getString(R.string.ml_music_library_delete_failed), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        runCatching {
                            val parent = file.parentFile
                            if (parent != null) {
                                val ttml = File(parent, "${file.nameWithoutExtension}.ttml")
                                if (ttml.exists()) ttml.delete()
                            }
                        }
                        favoritePaths.remove(target.path)
                        persistFavoritesPlaylist()
                        selectedPaths.remove(target.path)
                        albumTrackSortCache.remove(target.path)
                        allAudioFiles.removeAll { it.path == target.path }
                        saveCachedAudioFiles(context, allAudioFiles)
                        updateDisplayFiles()
                        showDeleteConfirmDialog = false
                        showSongInfoSheet = false
                        selectedSongInfoAudio = null
                        Toast.makeText(context, context.getString(R.string.ml_music_library_deleted), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.ml_music_library_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
            }
        )
    }
    
    if (showBatchMatchConfig) {
        BatchMatchConfigSheet(
            selectedCount = selectedPaths.size,
            onDismiss = { showBatchMatchConfig = false },
            onStartMatch = { config ->
                showBatchMatchConfig = false
                isCancelled = false
                isBatchMatching = true
                val selectedFiles = displayAudioFiles.filter { it.path in selectedPaths }
                batchMatchProgress = 0 to selectedFiles.size
                scope.launch {
                    performBatchMatch(context, selectedFiles, config,
                        isCancelled = { isCancelled },
                        onProgress = { current, total ->
                            batchMatchProgress = current to total
                        },
                        onComplete = { result ->
                            isBatchMatching = false
                            if (!isCancelled) {
                                scope.launch {
                                    saveAllBatchMatches(context, result.items, config)
                                    for (item in result.items.filter { it.matchStatus == MatchStatus.SUCCESS && it.matchedData.isNotEmpty() }) {
                                        refreshAudioFileMetadata(context, item.path, allAudioFiles)
                                    }
                                    saveCachedAudioFiles(context, allAudioFiles)
                                    updateDisplayFiles()
                                    batchMatchResult = result
                                    showBatchMatchResult = true
                                }
                            }
                        }
                    )
                }
            }
        )
    }
    
    if (showBatchMatchResult && batchMatchResult != null) {
        BatchMatchResultSheet(
            result = batchMatchResult!!,
            onDismiss = {
                showBatchMatchResult = false
                batchMatchResult = null
            },
            onUndoField = { item, fieldKey -> 
                scope.launch {
                    undoBatchMatchField(context, item, fieldKey)
                    if (refreshAudioFileMetadata(context, item.path, allAudioFiles) != null) {
                        saveCachedAudioFiles(context, allAudioFiles)
                    }
                    updateDisplayFiles()
                }
            }
        )
    }
    
    if (isBatchMatching) {
        BatchMatchProgressDialog(
            current = batchMatchProgress.first,
            total = batchMatchProgress.second,
            onCancel = { 
                isCancelled = true 
                isBatchMatching = false
            }
        )
    }
    
    if (showBatchRenameConfig) {
        BatchRenameConfigSheet(
            selectedCount = selectedPaths.size,
            initialConfig = batchRenameConfig,
            onDismiss = { showBatchRenameConfig = false },
            onStartPreview = { config ->
                batchRenameConfig = config
                showBatchRenameConfig = false
                val selectedFiles = displayAudioFiles.filter { it.path in selectedPaths }
                scope.launch {
                    isRenaming = true
                    val previewItems = generateRenamePreview(context, selectedFiles, config)
                    batchRenamePreviewItems = previewItems
                    isRenaming = false
                    showBatchRenamePreview = true
                }
            }
        )
    }
    
    if (showBatchLyricMatchConfig) {
        BatchMatchLyricsSheet(
            selectedCount = selectedPaths.size,
            onDismiss = { showBatchLyricMatchConfig = false },
            onStartMatch = { config ->
                showBatchLyricMatchConfig = false
                isLyricsCancelled = false
                isBatchLyricsMatching = true
                val selectedFiles = displayAudioFiles.filter { it.path in selectedPaths }
                batchLyricsMatchProgress = 0 to selectedFiles.size
                scope.launch {
                    performBatchLyricsMatch(
                        context, 
                        selectedFiles, 
                        config,
                        isCancelled = { isLyricsCancelled },
                        onProgress = { current, total ->
                            batchLyricsMatchProgress = current to total
                        },
                        onComplete = { result ->
                            if (!isLyricsCancelled) {
                                scope.launch {
                                    saveBatchLyricsMatches(context, result.items)
                                    batchLyricMatchResult = result
                                    isBatchLyricsMatching = false
                                    showBatchLyricMatchResult = true
                                }
                            } else {
                                isBatchLyricsMatching = false
                            }
                        }
                    )
                }
            }
        )
    }
    
    if (isBatchLyricsMatching) {
        BatchLyricMatchProgressDialog(
            current = batchLyricsMatchProgress.first,
            total = batchLyricsMatchProgress.second,
            onCancel = { 
                isLyricsCancelled = true 
                isBatchLyricsMatching = false
            }
        )
    }
    
    if (showBatchLyricMatchResult && batchLyricMatchResult != null) {
        BatchLyricMatchResultSheet(
            result = batchLyricMatchResult!!,
            onDismiss = {
                showBatchLyricMatchResult = false
                batchLyricMatchResult = null
            }
        )
    }
    
    if (showBatchLyricsEditSheet) {
        BatchLyricsEditSheet(
            selectedCount = selectedPaths.size,
            onDismiss = { showBatchLyricsEditSheet = false },
            onOpenBatchConvert = {
                showBatchLyricsEditSheet = false
                showBatchLyricsConvertSheet = true
            },
            onOpenBatchExport = {
                showBatchLyricsEditSheet = false
                showBatchLyricsExportSheet = true
            }
        )
    }
    
    if (showBatchLyricsConvertSheet) {
        BatchLyricsConvertSheet(
            selectedCount = selectedPaths.size,
            onDismiss = { showBatchLyricsConvertSheet = false },
            onStartConvert = { targetFormat ->
                showBatchLyricsConvertSheet = false
                isBatchLyricsConverting = true
                val selectedFiles = displayAudioFiles.filter { it.path in selectedPaths }
                batchLyricsConvertProgress = 0 to selectedFiles.size
                scope.launch {
                    val result = performBatchLyricsFormatConversion(
                        context = context,
                        audioFiles = selectedFiles,
                        targetFormat = targetFormat,
                        onProgress = { current, total ->
                            batchLyricsConvertProgress = current to total
                        }
                    )
                    isBatchLyricsConverting = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.ml_music_library_batch_convert_completed, result.successCount, result.failedCount),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
    
    if (isBatchLyricsConverting) {
        BatchLyricsConvertProgressDialog(
            current = batchLyricsConvertProgress.first,
            total = batchLyricsConvertProgress.second
        )
    }
    
    if (showBatchLyricsExportSheet) {
        BatchLyricsExternalExportSheet(
            selectedCount = selectedPaths.size,
            onDismiss = { showBatchLyricsExportSheet = false },
            onStartExport = { config ->
                showBatchLyricsExportSheet = false
                isBatchLyricsExporting = true
                val selectedFiles = displayAudioFiles.filter { it.path in selectedPaths }
                batchLyricsExportProgress = 0 to selectedFiles.size
                scope.launch {
                    val result = performBatchLyricsExternalExport(
                        context = context,
                        audioFiles = selectedFiles,
                        config = config,
                        onProgress = { current, total ->
                            batchLyricsExportProgress = current to total
                        }
                    )
                    isBatchLyricsExporting = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.ml_music_library_batch_export_completed, result.successCount, result.failedCount),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
    
    if (isBatchLyricsExporting) {
        BatchLyricsExportProgressDialog(
            current = batchLyricsExportProgress.first,
            total = batchLyricsExportProgress.second
        )
    }
    
    if (showBatchRenamePreview) {
        BatchRenamePreviewSheet(
            previewItems = batchRenamePreviewItems,
            config = batchRenameConfig,
            onDismiss = { showBatchRenamePreview = false },
            onConfirm = {
                showBatchRenamePreview = false
                scope.launch {
                    isRenaming = true
                    batchRenameProgress = 0 to batchRenamePreviewItems.size
                    performBatchRename(
                        context, 
                        batchRenamePreviewItems, 
                        batchRenameConfig,
                        onProgress = { current, total ->
                            batchRenameProgress = current to total
                        },
                        onComplete = { result ->
                            isRenaming = false
                            batchRenameResult = result
                            showBatchRenameResult = true
                        }
                    )
                }
            }
        )
    }
    
    if (isRenaming) {
        BatchRenameProgressDialog(
            current = batchRenameProgress.first,
            total = batchRenameProgress.second
        )
    }
    
    if (showBatchRenameResult && batchRenameResult != null) {
        BatchRenameResultSheet(
            result = batchRenameResult!!,
            onDismiss = { 
                showBatchRenameResult = false
                // 刷新文件列表 - 只更新被重命名的文件
                scope.launch {
                    batchRenameResult!!.items.forEach { item ->
                        val oldPath = item.audioFile.path
                        val oldFile = File(oldPath)
                        val newFile = File(oldFile.parent, item.newName)
                        if (newFile.exists()) {
                            // 处理封面缓存
                            val oldAudioFile = allAudioFiles.find { it.path == oldPath }
                            if (oldAudioFile != null && oldAudioFile.coverCachePath != null) {
                                val oldCacheFile = File(oldAudioFile.coverCachePath!!)
                                if (oldCacheFile.exists()) {
                                    // 复制旧封面缓存到新路径
                                    val newCachePath = oldCacheFile.parentFile?.absolutePath + File.separator + newFile.absolutePath.hashCode().toString() + ".jpg"
                                    val newCacheFile = File(newCachePath)
                                    oldCacheFile.copyTo(newCacheFile, overwrite = true)
                                }
                            }
                            
                            // 从列表中移除旧文件
                            val oldIndex = allAudioFiles.indexOfFirst { it.path == oldPath }
                            if (oldIndex >= 0) {
                                allAudioFiles.removeAt(oldIndex)
                            }
                            
                            // 读取新文件的元数据并添加到列表
                            try {
                                val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(context, newFile.absolutePath)
                                var coverCachePath: String? = null
                                if (metadata.cover != null) {
                                    coverCachePath = saveCoverToCache(context, newFile.absolutePath, metadata.cover)
                                } else if (oldAudioFile != null && oldAudioFile.coverCachePath != null) {
                                    // 如果没有新封面，尝试使用旧缓存
                                    val newCachePath = File(oldAudioFile.coverCachePath!!).parentFile?.absolutePath + 
                                        File.separator + newFile.absolutePath.hashCode().toString() + ".jpg"
                                    val newCacheFile = File(newCachePath)
                                    if (newCacheFile.exists()) {
                                        coverCachePath = newCachePath
                                    }
                                }
                                
                                val newAudioFile = AudioFile(
                                    path = newFile.absolutePath,
                                    title = metadata.title,
                                    artist = metadata.artist,
                                    album = metadata.album,
                                    duration = metadata.duration,
                                    fileSize = newFile.length(),
                                    lastModified = newFile.lastModified(),
                                    addedTime = oldAudioFile?.addedTime ?: System.currentTimeMillis(),
                                    coverCachePath = coverCachePath,
                                    year = metadata.year
                                )
                                allAudioFiles.add(newAudioFile)
                            } catch (e: Exception) {
                                Log.e("MusicLibrary", "Error reading renamed file: ${newFile.absolutePath}", e)
                            }
                        }
                    }
                    // 保存更新后的缓存
                    saveCachedAudioFiles(context, allAudioFiles)
                    updateDisplayFiles()
                }
            }
        )
    }
}

@Composable
fun AudioFileItemPlaceholder(
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
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun AudioFileItem(
    audio: AudioFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    isLocateHighlightActive: Boolean = false,
    sequenceNumber: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetSizePx = with(density) { 56.dp.toPx().toInt() }
    
    val sequenceWidth by animateDpAsState(
        targetValue = if (isInMultiSelectMode && sequenceNumber != null) 48.dp else 0.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "sequenceWidth"
    )
    
    val showSequenceText by remember {
        derivedStateOf { 
            sequenceWidth >= 34.dp 
        }
    }
    
    val sequenceAlpha by animateFloatAsState(
        targetValue = if (showSequenceText && isInMultiSelectMode && sequenceNumber != null) 1f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "sequenceAlpha"
    )
    
    val sequenceOffsetX by animateFloatAsState(
        targetValue = if (showSequenceText && isInMultiSelectMode && sequenceNumber != null) -4f else -48f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "sequenceOffset"
    )

    val baseBackgroundColor = if (isInMultiSelectMode && isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val locateHighlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
    val backgroundColor by animateColorAsState(
        targetValue = if (isLocateHighlightActive && !(isInMultiSelectMode && isSelected)) {
            locateHighlightColor
        } else {
            baseBackgroundColor
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "audioItemBackgroundColor"
    )
    
    LaunchedEffect(audio.coverCachePath, audio.lastModified) {
        coverBitmap = null
        audio.coverCachePath?.let { cachePath ->
            if (MusicLibraryActivity.isNoCoverMarked(cachePath)) {
                return@let
            }
            val cached = MusicLibraryActivity.getCoverFromCache(cachePath)
            if (cached != null) {
                coverBitmap = cached
            } else {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val fromCache = loadCoverBitmapFromCache(cachePath, targetSizePx, targetSizePx)
                        when {
                            fromCache != null -> fromCache
                            MusicLibraryActivity.isNoCoverMarked(cachePath) -> null
                            else -> rebuildCoverCacheBitmap(context, audio, cachePath, targetSizePx)
                        }
                    } catch (e: Exception) {
                        Log.e("MusicLibrary", "Error loading cover bitmap", e)
                        null
                    }
                }
                bitmap?.let {
                    MusicLibraryActivity.putCoverToCache(cachePath, it)
                    coverBitmap = it
                }
            }
        }

    }
    
    DisposableEffect(Unit) {
        onDispose {
            coverBitmap = null
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (onLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongClick() },
                            onTap = { onClick() }
                        )
                    }
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(sequenceWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sequenceNumber?.toString() ?: "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = (if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)).copy(alpha = sequenceAlpha),
                modifier = Modifier
                    .graphicsLayer {
                        translationX = sequenceOffsetX.dp.toPx()
                    }
                    .height(20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        }
        
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.ml_music_library_album_cover),
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = stringResource(R.string.ml_music_library_audio),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = audio.displayTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                Text(
                    text = audio.displayArtist,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (audio.album.isNotEmpty()) {
                    Text(
                        text = " · ${audio.displayAlbum}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = audio.displayInfo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isInMultiSelectMode && (onPlayClick != null || onMoreClick != null)) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onPlayClick != null) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.play_style2),
                            contentDescription = stringResource(R.string.ml_music_library_play),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onMoreClick != null) {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_more_vert_24),
                            contentDescription = stringResource(R.string.ml_music_library_more),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicLibraryMiniPlayerBar(
    controller: MusicPlaybackController,
    onExpand: () -> Unit
) {
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var coverBitmapKey by remember { mutableStateOf<String?>(null) }
    var coverThemePair by remember { mutableStateOf<CoverThemeColorPair?>(null) }
    var coverLoadRequestVersion by remember { mutableStateOf(0) }
    var themeResolveRequestVersion by remember { mutableStateOf(0) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val isDarkTheme = colorLuminance(MaterialTheme.colorScheme.background) < 0.5f
    val coverThemeCacheKey = remember(
        controller.currentMediaStoreId,
        controller.currentAudioPath
    ) {
        buildCoverThemeCacheKey(
            coverCachePath = null,
            mediaId = controller.currentMediaStoreId
                .takeIf { it > 0L }
                ?.toString(),
            audioPath = controller.currentAudioPath
        )
    }

    LaunchedEffect(
        controller.currentCoverCachePath,
        controller.currentAudioPath,
        controller.currentMediaStoreId
    ) {
        val requestVersion = coverLoadRequestVersion + 1
        coverLoadRequestVersion = requestVersion
        val requestKey = coverThemeCacheKey
        coverThemePair = null
        coverBitmapKey = null
        val loadedCover = withContext(Dispatchers.IO) {
            controller.currentCoverCachePath
                ?.takeIf { File(it).exists() }
                ?.let { loadCoverBitmapFromCache(it, 112, 112) }
        }
        if (coverLoadRequestVersion != requestVersion) return@LaunchedEffect
        coverBitmap = loadedCover
        coverBitmapKey = requestKey
    }
    LaunchedEffect(coverThemeCacheKey, coverBitmap) {
        if (coverBitmapKey != coverThemeCacheKey || coverBitmap == null) {
            coverThemePair = null
            return@LaunchedEffect
        }
        val requestVersion = themeResolveRequestVersion + 1
        themeResolveRequestVersion = requestVersion
        val resolvedPair = withContext(Dispatchers.IO) {
            resolveCachedCoverThemePair(
                context = context,
                cacheKey = coverThemeCacheKey,
                cover = coverBitmap
            )
        }
        if (themeResolveRequestVersion != requestVersion) return@LaunchedEffect
        coverThemePair = resolvedPair
    }
    val coverPanelColor = if (isDarkTheme) coverThemePair?.darkColor else coverThemePair?.lightColor

    val baseColor = coverPanelColor
        ?.let { blendColorForUi(it, if (isDarkTheme) Color.Black else Color.White, if (isDarkTheme) 0.12f else 0.16f) }
        ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)
    val onBaseColor = if (colorLuminance(baseColor) > 0.52f) Color(0xFF151515) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(baseColor)
            .pointerInput(controller.currentMediaId) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetY < -56f) {
                            onExpand()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffsetY += dragAmount.y
                }
            }
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.ml_music_library_current_song_cover),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            AutoMarqueeText(
                text = controller.currentTitle.ifBlank { stringResource(R.string.ml_music_library_no_song_selected) },
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onBaseColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            AutoMarqueeText(
                text = controller.currentArtist.ifBlank { stringResource(R.string.ml_music_library_unknown_artist) },
                style = TextStyle(
                    fontSize = 12.sp,
                    color = onBaseColor.copy(alpha = 0.75f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        IconButton(
            onClick = { controller.skipToPrevious() },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.ml_music_library_previous_track),
                tint = onBaseColor,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(
            onClick = { controller.togglePlayPause() },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = if (controller.isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = if (controller.isPlaying) stringResource(R.string.ml_music_library_pause) else stringResource(R.string.ml_music_library_play),
                tint = onBaseColor,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = { controller.skipToNext() },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.ml_music_library_next_track),
                tint = onBaseColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOptionsDialog(
    audio: AudioFile,
    autoDetectEmbeddedLyricsType: Boolean = false,
    initialCoverBitmap: android.graphics.Bitmap? = null,
    onDismiss: () -> Unit,
    onEditLyrics: (String?, String) -> Unit,
    onEditMetadata: ((String) -> Unit)? = null,
    showEditMetadataButton: Boolean = onEditMetadata != null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var embeddedLyrics by remember { mutableStateOf<String?>(null) }
    var externalLyrics by remember { mutableStateOf<String?>(null) }
    var externalLyricsPath by remember { mutableStateOf<String?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(true) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedFormat by remember { mutableStateOf(0) }
    var showLyricsPreview by remember { mutableStateOf(false) }
    var previewLyricsContent by remember { mutableStateOf("") }
    var previewLyricsTitle by remember { mutableStateOf("") }
    var coverBitmap by remember(audio.path, initialCoverBitmap) { mutableStateOf(initialCoverBitmap) }
    
    val hasEmbedded by remember { derivedStateOf { embeddedLyrics != null } }
    val hasExternal by remember { derivedStateOf { externalLyrics != null } }
    val hasLyrics by remember { derivedStateOf { hasEmbedded || hasExternal } }
    
    val detectedEmbeddedFormat by remember { derivedStateOf {
        embeddedLyrics?.let { detectLyricsFormat(it) } ?: 0
    } }
    
    val resolvedEmbeddedFormatIndex by remember {
        derivedStateOf {
            if (autoDetectEmbeddedLyricsType) {
                detectedEmbeddedFormat
            } else {
                selectedFormat
            }.coerceIn(0, LYRIC_FORMAT_OPTIONS.lastIndex)
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(Unit) {
        sheetState.show()
    }
    
    LaunchedEffect(audio.path, audio.coverCachePath, initialCoverBitmap) {
        isLoadingLyrics = true
        selectedSource = null
        selectedFormat = 0
        embeddedLyrics = null
        externalLyrics = null
        externalLyricsPath = null
        coverBitmap = initialCoverBitmap

        // 先显示弹窗与加载态，再并行读取封面和歌词
        kotlinx.coroutines.yield()

        val coverDeferred = async { loadAudioOptionsCoverBitmap(audio.coverCachePath) }
        val lyricsLoadResult = loadAudioLyricsLoadResult(
            context = context,
            audioPath = audio.path,
            mediaStoreId = audio.mediaStoreId
        )

        coverDeferred.await()?.let { coverBitmap = it }
        embeddedLyrics = lyricsLoadResult.embeddedLyrics
        externalLyrics = lyricsLoadResult.externalLyrics
        externalLyricsPath = lyricsLoadResult.externalLyricsPath

        if (lyricsLoadResult.isEmbeddedOnly) {
            selectedSource = "embedded"
            selectedFormat = lyricsLoadResult.detectedEmbeddedFormat
        } else if (!lyricsLoadResult.hasEmbedded && lyricsLoadResult.hasExternal) {
            selectedSource = "external"
        }

        isLoadingLyrics = false
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.statusBarsPadding()
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.ml_music_library_album_cover),
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_play),
                        contentDescription = stringResource(R.string.ml_music_library_audio),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = audio.displayTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = stringResource(R.string.ml_music_library_artist_with_value, audio.displayArtist),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (audio.album.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.ml_music_library_album_with_value, audio.displayAlbum),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_duration_size, formatAudioDuration(audio.duration), formatFileSize(audio.fileSize)),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.ml_music_library_directory_with_value, File(audio.path).parent?.substringAfterLast("/") ?: stringResource(R.string.ml_music_library_unknown)),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoadingLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ml_music_library_reading_lyrics),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (hasLyrics) {
                    Text(
                        text = stringResource(R.string.ml_music_library_detected_lyrics_select_source),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (hasEmbedded) {
                        LyricsSourceCard(
                            title = stringResource(R.string.ml_music_library_embedded_lyrics),
                            subtitle = stringResource(R.string.ml_music_library_tap_to_preview_lyrics),
                            selected = selectedSource == "embedded",
                            onClick = { 
                                selectedSource = "embedded"
                                selectedFormat = detectedEmbeddedFormat
                            },
                            onPreview = {
                                previewLyricsContent = embeddedLyrics ?: ""
                                previewLyricsTitle = context.getString(R.string.ml_music_library_embedded_lyrics_preview)
                                showLyricsPreview = true
                            }
                        )
                    }
                    
                    if (hasExternal) {
                        if (hasEmbedded) Spacer(modifier = Modifier.height(8.dp))
                        LyricsSourceCard(
                            title = stringResource(R.string.ml_music_library_external_ttml_file),
                            subtitle = externalLyricsPath ?: "",
                            selected = selectedSource == "external",
                            onClick = { selectedSource = "external" },
                            onPreview = {
                                previewLyricsContent = externalLyrics ?: ""
                                previewLyricsTitle = context.getString(R.string.ml_music_library_external_ttml_preview)
                                showLyricsPreview = true
                            }
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = selectedSource == "embedded" && autoDetectEmbeddedLyricsType,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.ml_music_library_auto_detected_lyrics_format, detectedEmbeddedFormat.toLyricFormatLabel()),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedSource == "embedded" && !autoDetectEmbeddedLyricsType,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { -it / 2 }
                        ),
                        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                            animationSpec = tween(200),
                            targetOffsetY = { -it / 2 }
                        )
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = stringResource(R.string.ml_music_library_select_lyrics_format),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            LYRIC_FORMAT_OPTIONS.forEachIndexed { index, format ->
                                val isRecommended = index == detectedEmbeddedFormat
                                val displayText = if (isRecommended) stringResource(R.string.ml_music_library_format_recommended, format) else format
                                
                                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedFormat == index) 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = if (selectedFormat == index) 
                                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                    else 
                                        null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = { selectedFormat = index })
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = displayText,
                                                fontSize = 16.sp,
                                                fontWeight = if (isRecommended) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_edit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.ml_music_library_no_lyrics_detected),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.ml_music_library_click_below_to_create_lyrics),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = {
                    when {
                        selectedSource == "external" && externalLyrics != null -> {
                            onEditLyrics(externalLyrics, context.getString(R.string.ml_music_library_ttml_lyrics))
                        }
                        selectedSource == "embedded" && embeddedLyrics != null -> {
                            onEditLyrics(embeddedLyrics, resolvedEmbeddedFormatIndex.toLyricFormatLabel())
                        }
                        hasExternal && !hasEmbedded -> {
                            onEditLyrics(externalLyrics, context.getString(R.string.ml_music_library_ttml_lyrics))
                        }
                        !hasLyrics -> {
                            onEditLyrics(null, "")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoadingLyrics && (!hasLyrics || selectedSource != null)
            ) {
                Text(
                    text = when {
                        isLoadingLyrics -> stringResource(R.string.ml_music_library_reading_lyrics)
                        hasLyrics && selectedSource == null -> stringResource(R.string.ml_music_library_select_lyrics_source_first)
                        else -> stringResource(R.string.ml_music_library_start_edit_lyrics)
                    },
                    fontSize = 16.sp
                )
            }
            
            if (showEditMetadataButton && onEditMetadata != null) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditMetadata(audio.path)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_edit_song_metadata),
                        fontSize = 16.sp
                    )
                }
            }
        }

    }
    
    if (showLyricsPreview) {
        LyricsPreviewDialog(
            title = previewLyricsTitle,
            content = previewLyricsContent,
            onDismiss = { showLyricsPreview = false }
        )
    }
}

@Composable
private fun LyricsSourceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (selected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onPreview) {
                Text(stringResource(R.string.ml_music_library_view))
            }
        }
    }
}

@Composable
private fun LyricsPreviewDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    text = content.take(2000) + if (content.length > 2000) "\n..." else "",
                    fontSize = 12.sp,
                    modifier = Modifier.verticalScroll(scrollState)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ml_music_library_close))
            }
        }
    )
}

private suspend fun loadCachedAudioFiles(context: Context, audioFiles: MutableList<AudioFile>): Boolean {
    val roomCachedFiles = withContext(Dispatchers.IO) {
        musicLibraryCacheStore(context).loadAllPaged()
    }
    if (roomCachedFiles.isNotEmpty()) {
        audioFiles.addAll(roomCachedFiles)
        return true
    }

    // 兼容历史版本：首次升级时将 SharedPreferences 缓存迁移到 Room。
    val legacyCachedFiles = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("MusicLibraryCache", Context.MODE_PRIVATE)
        val cacheJson = prefs.getString("audioFilesCache", null) ?: return@withContext emptyList<AudioFile>()

        try {
            val jsonArray = JSONArray(cacheJson)
            buildList(jsonArray.length()) {
                for (i in 0 until jsonArray.length()) {
                    add(AudioFile.fromJson(jsonArray.getJSONObject(i)))
                }
            }
        } catch (e: Exception) {
            Log.e("MusicLibrary", "Error loading cache", e)
            emptyList()
        }
    }
    if (legacyCachedFiles.isEmpty()) return false
    audioFiles.addAll(legacyCachedFiles)
    saveCachedAudioFiles(context, legacyCachedFiles)
    return true
}

private fun saveCachedAudioFiles(context: Context, audioFiles: List<AudioFile>) {
    musicLibraryCacheStore(context).saveAllAsync(audioFiles)
}

private suspend fun saveCachedAudioFilesBlocking(context: Context, audioFiles: List<AudioFile>) {
    withContext(Dispatchers.IO) {
        musicLibraryCacheStore(context).saveAllBlocking(audioFiles)
    }
}

private fun sortAudioFiles(
    audioFiles: MutableList<AudioFile>,
    sortType: SortType,
    sortOrder: SortOrder
) {
    fun parseYearForSort(year: String): String {
        return when {
            year.isEmpty() -> "9999-12-31"
            year.matches(Regex("\\d{4}")) -> "$year-01-01"
            else -> year
        }
    }

    val fileNameComparator = compareBy<AudioFile>(
        { buildFileNameSortBucket(it.displayTitle) },
        { buildFileNameSortKey(it.displayTitle) },
        { it.displayTitle.lowercase(Locale.ROOT) }
    )

    val sorted = when (sortType) {
        SortType.FILE_NAME -> {
            if (sortOrder == SortOrder.ASC) {
                audioFiles.sortedWith(fileNameComparator)
            } else {
                audioFiles.sortedWith(fileNameComparator.reversed())
            }
        }
        SortType.MODIFY_TIME -> {
            if (sortOrder == SortOrder.ASC) {
                audioFiles.sortedBy { it.lastModified }
            } else {
                audioFiles.sortedByDescending { it.lastModified }
            }
        }
        SortType.ADD_TIME -> {
            if (sortOrder == SortOrder.ASC) {
                audioFiles.sortedBy { it.addedTime }
            } else {
                audioFiles.sortedByDescending { it.addedTime }
            }
        }
        SortType.YEAR -> {
            if (sortOrder == SortOrder.ASC) {
                audioFiles.sortedBy { parseYearForSort(it.year) }
            } else {
                audioFiles.sortedByDescending { parseYearForSort(it.year) }
            }
        }
    }
    
    audioFiles.clear()
    audioFiles.addAll(sorted)
}

private data class NativeMediaAudioEntry(
    val mediaStoreId: Long,
    val path: String,
    val duration: Long,
    val fileSize: Long,
    val lastModified: Long
)

private fun normalizeLibraryPath(path: String): String {
    return path.trim().replace('\\', '/').trimEnd('/')
}

private fun isPathInsideFolder(path: String, folder: String): Boolean {
    val normalizedPath = normalizeLibraryPath(path)
    val normalizedFolder = normalizeLibraryPath(folder)
    if (normalizedPath.isEmpty() || normalizedFolder.isEmpty()) return false
    return normalizedPath.equals(normalizedFolder, ignoreCase = true) ||
        normalizedPath.startsWith("$normalizedFolder/", ignoreCase = true)
}

private fun shouldIncludeLibraryPath(
    path: String,
    includeFolders: Set<String>,
    excludeFolders: Set<String>
): Boolean {
    val inIncludeFolders = includeFolders.isEmpty() || includeFolders.any { folder ->
        isPathInsideFolder(path, folder)
    }
    if (!inIncludeFolders) return false
    return excludeFolders.none { folder -> isPathInsideFolder(path, folder) }
}

@Suppress("DEPRECATION")
private fun queryNativeMediaAudioEntries(context: Context): List<NativeMediaAudioEntry> {
    val resolver = context.contentResolver
    val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        android.provider.MediaStore.Audio.Media._ID,
        android.provider.MediaStore.Audio.Media.DATA,
        android.provider.MediaStore.Audio.Media.DURATION,
        android.provider.MediaStore.Audio.Media.SIZE,
        android.provider.MediaStore.Audio.Media.DATE_MODIFIED,
        android.provider.MediaStore.Audio.Media.RELATIVE_PATH,
        android.provider.MediaStore.Audio.Media.DISPLAY_NAME
    )
    val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${android.provider.MediaStore.Audio.Media.DATE_MODIFIED} DESC"
    val results = mutableListOf<NativeMediaAudioEntry>()

    try {
        resolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
            val dataIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
            val durationIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DURATION)
            val sizeIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.SIZE)
            val modifiedIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATE_MODIFIED)
            val relativePathIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.RELATIVE_PATH)
            val displayNameIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idIndex)
                var path = if (dataIndex >= 0) cursor.getString(dataIndex) else null

                if (path.isNullOrBlank()) {
                    val relativePath = if (relativePathIndex >= 0) cursor.getString(relativePathIndex) else null
                    val displayName = if (displayNameIndex >= 0) cursor.getString(displayNameIndex) else null
                    if (!relativePath.isNullOrBlank() && !displayName.isNullOrBlank()) {
                        val fallbackFile = File(android.os.Environment.getExternalStorageDirectory(), relativePath + displayName)
                        if (fallbackFile.exists()) {
                            path = fallbackFile.absolutePath
                        }
                    }
                }

                if (path.isNullOrBlank()) continue

                val duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
                val fileSize = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                val lastModifiedSec = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) else 0L

                results.add(
                    NativeMediaAudioEntry(
                        mediaStoreId = mediaStoreId,
                        path = path,
                        duration = duration,
                        fileSize = fileSize,
                        lastModified = lastModifiedSec * 1000
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error querying native media library", e)
    }

    return results
}

private fun isNativeMediaEntryChanged(existing: AudioFile?, entry: NativeMediaAudioEntry): Boolean {
    if (existing == null) return true
    if (existing.mediaStoreId != entry.mediaStoreId) return true
    if (!existing.coverCachePath.isNullOrBlank() && !File(existing.coverCachePath).exists()) return true
    if (existing.fileSize != entry.fileSize) return true
    if (kotlin.math.abs(existing.duration - entry.duration) > 1000L) return true
    return kotlin.math.abs(existing.lastModified - entry.lastModified) > 1000L
}

private fun hasAudioMetadataChanged(
    existing: AudioFile,
    title: String,
    artist: String,
    album: String,
    duration: Long,
    fileSize: Long,
    lastModified: Long,
    year: String,
    coverCachePath: String?,
    mediaStoreId: Long
): Boolean {
    val finalCoverCachePath = coverCachePath ?: existing.coverCachePath
    return existing.title != title ||
        existing.artist != artist ||
        existing.album != album ||
        existing.duration != duration ||
        existing.fileSize != fileSize ||
        existing.lastModified != lastModified ||
        existing.year != year ||
        existing.coverCachePath != finalCoverCachePath ||
        existing.mediaStoreId != mediaStoreId
}

private data class ScannedAudioSource(
    val path: String,
    val uri: Uri?,
    val fileSize: Long,
    val lastModified: Long
)

private suspend fun scanAudioFiles(
    context: Context,
    prefs: android.content.SharedPreferences,
    audioFiles: MutableList<AudioFile>,
    onProgress: (Int, Int) -> Unit,
    onComplete: (ScanSummary) -> Unit
) {
    val useNativeMediaLibrary = prefs.getBoolean("useNativeMediaLibrary", true)
    if (useNativeMediaLibrary) {
        scanAudioFilesFromNativeMediaStore(context, prefs, audioFiles, onProgress, onComplete)
    } else {
        scanAudioFilesFromFolders(context, prefs, audioFiles, onProgress, onComplete)
    }
}

private suspend fun scanAudioFilesFromFolders(
    context: Context,
    prefs: android.content.SharedPreferences,
    audioFiles: MutableList<AudioFile>,
    onProgress: (Int, Int) -> Unit,
    onComplete: (ScanSummary) -> Unit
) {
    val folders = prefs.getStringSet("musicFolders", emptySet()) ?: emptySet()
    val excludeFolders = prefs.getStringSet("excludeFolders", emptySet()) ?: emptySet()
    val excludeShortAudio = prefs.getBoolean("excludeShortAudio", true)
    val audioExtensions = setOf("mp3", "flac", "ogg", "m4a", "wav", "aac", "wma", "ape", "dsf", "dff", "dsdiff")

    val allSources = mutableListOf<ScannedAudioSource>()

    fun isExcludedPath(path: String): Boolean {
        return excludeFolders.any { excludeFolder ->
            path.startsWith(excludeFolder + File.separator) || path == excludeFolder
        }
    }

    fun isExcluded(file: File): Boolean = isExcludedPath(file.absolutePath)

    withContext(Dispatchers.IO) {
        var addedCount = 0
        var removedCount = 0
        var updatedCount = 0

        for (folder in folders) {
            val dir = File(folder)
            if (dir.exists() && dir.isDirectory) {
                val files = mutableListOf<File>()
                scanDirectory(dir, audioExtensions, files, ::isExcluded)
                files.forEach { file ->
                    allSources.add(
                        ScannedAudioSource(
                            path = file.absolutePath,
                            uri = null,
                            fileSize = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            }

            val treeUri = resolvePersistedFolderTreeUri(context, prefs, folder)
            if (treeUri != null) {
                val root = DocumentFile.fromTreeUri(context, treeUri)
                if (root != null && root.isDirectory) {
                    scanDocumentDirectory(
                        document = root,
                        currentPath = folder,
                        extensions = audioExtensions,
                        fileList = allSources,
                        isExcludedPath = ::isExcludedPath
                    )
                }
            }
        }

        val existingSnapshot = withContext(Dispatchers.Main) {
            audioFiles.associateBy { it.path }
        }
        val distinctSources = allSources
            .groupBy { it.path }
            .map { (_, sources) -> sources.firstOrNull { it.uri != null } ?: sources.first() }
        val mergedByPath = LinkedHashMap<String, AudioFile>(distinctSources.size)
        val progressStep = 100
        var lastProgressReported = -progressStep
        suspend fun reportProgress(current: Int, total: Int) {
            val shouldReport = current == 0 || current >= total || current - lastProgressReported >= progressStep
            if (!shouldReport) return
            lastProgressReported = current
            withContext(Dispatchers.Main) {
                onProgress(current, total)
            }
        }

        val total = distinctSources.size
        reportProgress(0, total)
        distinctSources.forEachIndexed { index, source ->
            val path = source.path
            val existing = existingSnapshot[path]
            try {
                val metadata = if (source.uri != null) {
                    com.example.LyricBox.utils.AudioMetadataReader.readMetadataFromUri(
                        context = context,
                        uri = source.uri,
                        includeCover = false
                    )
                } else {
                    com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
                        context = context,
                        filePath = path,
                        includeCover = false
                    )
                }
                val duration = metadata.duration
                if (!excludeShortAudio || duration >= 60000L) {
                    val fileSize = source.fileSize
                    val fileLastModified = source.lastModified
                    val coverCachePath = resolveCoverCachePath(
                        context = context,
                        audioFilePath = path,
                        cacheDiscriminator = fileLastModified.toString()
                    )
                    if (existing != null) {
                        val changed = hasAudioMetadataChanged(
                            existing = existing,
                            title = metadata.title,
                            artist = metadata.artist,
                            album = metadata.album,
                            duration = duration,
                            fileSize = fileSize,
                            lastModified = fileLastModified,
                            year = metadata.year,
                            coverCachePath = coverCachePath,
                            mediaStoreId = -1L
                        )
                        if (changed) {
                            updatedCount++
                        }
                        mergedByPath[path] = existing.copy(
                            title = metadata.title,
                            artist = metadata.artist,
                            album = metadata.album,
                            duration = duration,
                            fileSize = fileSize,
                            lastModified = fileLastModified,
                            coverCachePath = coverCachePath,
                            year = metadata.year,
                            mediaStoreId = -1L
                        )
                    } else {
                        mergedByPath[path] = AudioFile(
                            path = path,
                            title = metadata.title,
                            artist = metadata.artist,
                            album = metadata.album,
                            duration = duration,
                            fileSize = fileSize,
                            lastModified = fileLastModified,
                            addedTime = System.currentTimeMillis(),
                            coverCachePath = coverCachePath,
                            year = metadata.year,
                            mediaStoreId = -1L
                        )
                        addedCount++
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error reading file: $path", e)
                if (existing != null) {
                    mergedByPath[path] = existing
                }
            } finally {
                reportProgress(index + 1, total)
            }
        }

        removedCount = existingSnapshot.keys.count { it !in mergedByPath.keys }
        val mergedList = mergedByPath.values.toList()
        val validCoverPaths = mergedList.mapNotNull { it.coverCachePath }.toSet()
        clearOldCoverCache(context, validCoverPaths)
        saveCachedAudioFilesBlocking(context, mergedList)

        withContext(Dispatchers.Main) {
            audioFiles.clear()
            if (mergedList.isNotEmpty()) {
                val batchSize = 200
                var start = 0
                while (start < mergedList.size) {
                    val end = minOf(start + batchSize, mergedList.size)
                    audioFiles.addAll(mergedList.subList(start, end))
                    start = end
                    if (start < mergedList.size) {
                        kotlinx.coroutines.yield()
                    }
                }
            }
            prefs.edit().putStringSet("lastScannedFolders", folders).apply()
            onComplete(
                ScanSummary(
                    totalCount = audioFiles.size,
                    addedCount = addedCount,
                    removedCount = removedCount,
                    updatedCount = updatedCount
                )
            )
        }
    }
}

private suspend fun scanAudioFilesFromNativeMediaStore(
    context: Context,
    prefs: android.content.SharedPreferences,
    audioFiles: MutableList<AudioFile>,
    onProgress: (Int, Int) -> Unit,
    onComplete: (ScanSummary) -> Unit
) {
    val includeFolders = prefs.getStringSet("musicFolders", emptySet()) ?: emptySet()
    val excludeFolders = prefs.getStringSet("excludeFolders", emptySet()) ?: emptySet()
    val excludeShortAudio = prefs.getBoolean("excludeShortAudio", true)

    withContext(Dispatchers.IO) {
        var addedCount = 0
        var removedCount = 0
        var updatedCount = 0

        val mediaEntries = queryNativeMediaAudioEntries(context)
            .filter { entry -> shouldIncludeLibraryPath(entry.path, includeFolders, excludeFolders) }
            .filter { entry -> !excludeShortAudio || entry.duration >= 60000L }
            .distinctBy { it.path }
            .toMutableList()

        val mediaPaths = mediaEntries.asSequence().map { it.path }.toHashSet()
        val supplementalEntries = collectSupplementalDsdEntriesFromFolders(
            includeFolders = includeFolders,
            excludeFolders = excludeFolders,
            existingPaths = mediaPaths
        )
        if (supplementalEntries.isNotEmpty()) {
            mediaEntries.addAll(supplementalEntries)
        }

        val existingSnapshot = withContext(Dispatchers.Main) {
            audioFiles.associateBy { it.path }
        }
        val mergedByPath = LinkedHashMap<String, AudioFile>(mediaEntries.size)
        val progressStep = 100
        var lastProgressReported = -progressStep
        suspend fun reportProgress(current: Int, total: Int) {
            val shouldReport = current == 0 || current >= total || current - lastProgressReported >= progressStep
            if (!shouldReport) return
            lastProgressReported = current
            withContext(Dispatchers.Main) {
                onProgress(current, total)
            }
        }

        val total = mediaEntries.size
        reportProgress(0, total)
        mediaEntries.forEachIndexed { index, entry ->
            val existing = existingSnapshot[entry.path]
            if (existing != null && !isNativeMediaEntryChanged(existing, entry)) {
                val resolvedCoverCachePath = if (existing.coverCachePath.isNullOrBlank()) {
                    resolveCoverCachePath(
                        context = context,
                        audioFilePath = entry.path,
                        cacheDiscriminator = entry.lastModified.toString()
                    )
                } else {
                    existing.coverCachePath
                }
                if (resolvedCoverCachePath != existing.coverCachePath) {
                    updatedCount++
                    mergedByPath[entry.path] = existing.copy(coverCachePath = resolvedCoverCachePath)
                } else {
                    mergedByPath[entry.path] = existing
                }
                reportProgress(index + 1, total)
                return@forEachIndexed
            }
            try {
                val file = File(entry.path)
                if (!file.exists()) {
                    return@forEachIndexed
                }

                val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
                    context = context,
                    filePath = entry.path,
                    mediaStoreId = entry.mediaStoreId,
                    includeCover = false
                )
                val duration = if (metadata.duration > 0) metadata.duration else entry.duration
                if (excludeShortAudio && duration < 60000L) {
                    return@forEachIndexed
                }

                val targetFileSize = if (entry.fileSize > 0) entry.fileSize else file.length()
                val targetLastModified = if (entry.lastModified > 0) entry.lastModified else file.lastModified()
                val coverCachePath = resolveCoverCachePath(
                    context = context,
                    audioFilePath = entry.path,
                    cacheDiscriminator = targetLastModified.toString()
                )

                if (existing != null) {
                    val changed = hasAudioMetadataChanged(
                        existing = existing,
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.album,
                        duration = duration,
                        fileSize = targetFileSize,
                        lastModified = targetLastModified,
                        year = metadata.year,
                        coverCachePath = coverCachePath,
                        mediaStoreId = entry.mediaStoreId
                    )
                    if (changed) {
                        updatedCount++
                    }
                    mergedByPath[entry.path] = existing.copy(
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.album,
                        duration = duration,
                        fileSize = targetFileSize,
                        lastModified = targetLastModified,
                        coverCachePath = coverCachePath,
                        year = metadata.year,
                        mediaStoreId = entry.mediaStoreId
                    )
                } else {
                    mergedByPath[entry.path] = AudioFile(
                        path = entry.path,
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.album,
                        duration = duration,
                        fileSize = targetFileSize,
                        lastModified = targetLastModified,
                        addedTime = System.currentTimeMillis(),
                        coverCachePath = coverCachePath,
                        year = metadata.year,
                        mediaStoreId = entry.mediaStoreId
                    )
                    addedCount++
                }
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error syncing native media entry: ${entry.path}", e)
                if (existing != null) {
                    mergedByPath[entry.path] = existing
                }
            } finally {
                reportProgress(index + 1, total)
            }
        }

        removedCount = existingSnapshot.keys.count { it !in mergedByPath.keys }
        val mergedList = mergedByPath.values.toList()
        val validCoverPaths = mergedList.mapNotNull { it.coverCachePath }.toSet()
        clearOldCoverCache(context, validCoverPaths)
        saveCachedAudioFilesBlocking(context, mergedList)

        withContext(Dispatchers.Main) {
            audioFiles.clear()
            if (mergedList.isNotEmpty()) {
                val batchSize = 200
                var start = 0
                while (start < mergedList.size) {
                    val end = minOf(start + batchSize, mergedList.size)
                    audioFiles.addAll(mergedList.subList(start, end))
                    start = end
                    if (start < mergedList.size) {
                        kotlinx.coroutines.yield()
                    }
                }
            }
            prefs.edit().putLong("lastNativeMediaSyncAt", System.currentTimeMillis()).apply()
            onComplete(
                ScanSummary(
                    totalCount = audioFiles.size,
                    addedCount = addedCount,
                    removedCount = removedCount,
                    updatedCount = updatedCount
                )
            )
        }
    }
}

private fun collectSupplementalDsdEntriesFromFolders(
    includeFolders: Set<String>,
    excludeFolders: Set<String>,
    existingPaths: Set<String>
): List<NativeMediaAudioEntry> {
    val dsdExtensions = setOf("dsf", "dff", "dsdiff")
    val files = mutableListOf<File>()
    val scanRoots = if (includeFolders.isNotEmpty()) {
        includeFolders
    } else {
        setOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)
    }

    fun isExcluded(file: File): Boolean {
        val target = file.absolutePath
        return excludeFolders.any { excludeFolder ->
            target.startsWith(excludeFolder + File.separator) || target == excludeFolder
        }
    }

    scanRoots.forEach { folder ->
        val dir = File(folder)
        if (dir.exists() && dir.isDirectory) {
            scanDirectory(dir, dsdExtensions, files, ::isExcluded)
        }
    }

    return files
        .asSequence()
        .map { it.absolutePath }
        .distinct()
        .filter { path -> path !in existingPaths }
        .mapNotNull { path ->
            runCatching {
                val file = File(path)
                if (!file.exists() || !file.isFile) return@runCatching null
                NativeMediaAudioEntry(
                    mediaStoreId = -1L,
                    path = file.absolutePath,
                    duration = 0L,
                    fileSize = file.length(),
                    lastModified = file.lastModified()
                )
            }.getOrNull()
        }
        .toList()
}

private fun scanDirectory(dir: File, extensions: Set<String>, fileList: MutableList<File>, isExcluded: (File) -> Boolean) {
    if (isExcluded(dir)) {
        return
    }
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            scanDirectory(file, extensions, fileList, isExcluded)
        } else {
            if (!isExcluded(file)) {
                val ext = file.extension.lowercase()
                if (ext in extensions) {
                    fileList.add(file)
                }
            }
        }
    }
}

private fun resolvePersistedFolderTreeUri(
    context: Context,
    prefs: android.content.SharedPreferences,
    folder: String
): Uri? {
    val savedUri = prefs.getString("musicFolderTreeUri:$folder", null)
        ?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
    if (savedUri != null && hasPersistedReadPermission(context, savedUri)) {
        return savedUri
    }

    return context.contentResolver.persistedUriPermissions
        .asSequence()
        .filter { it.isReadPermission }
        .map { it.uri }
        .firstOrNull { uri -> convertTreeUriToPath(uri) == folder }
        ?.also { uri ->
            prefs.edit().putString("musicFolderTreeUri:$folder", uri.toString()).apply()
        }
}

private fun hasPersistedReadPermission(context: Context, uri: Uri): Boolean {
    return context.contentResolver.persistedUriPermissions.any { permission ->
        permission.uri == uri && permission.isReadPermission
    }
}

private fun convertTreeUriToPath(uri: Uri): String? {
    val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
    if (!docId.isNullOrBlank()) {
        val split = docId.split(":", limit = 2)
        if (split.isNotEmpty()) {
            val volume = split[0]
            val relativePath = split.getOrNull(1).orEmpty()
            return if (volume.equals("primary", ignoreCase = true)) {
                if (relativePath.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$relativePath"
            } else {
                if (relativePath.isBlank()) "/storage/$volume" else "/storage/$volume/$relativePath"
            }
        }
    }

    val rawPath = uri.path ?: return null
    val treePath = rawPath.substringAfter("/tree/", missingDelimiterValue = "")
    if (treePath.isBlank()) return null
    val volume = treePath.substringBefore(":", missingDelimiterValue = treePath)
    val relativePath = treePath.substringAfter(":", missingDelimiterValue = "")
    return if (volume.equals("primary", ignoreCase = true)) {
        if (relativePath.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$relativePath"
    } else {
        if (relativePath.isBlank()) "/storage/$volume" else "/storage/$volume/$relativePath"
    }
}

private fun scanDocumentDirectory(
    document: DocumentFile,
    currentPath: String,
    extensions: Set<String>,
    fileList: MutableList<ScannedAudioSource>,
    isExcludedPath: (String) -> Boolean
) {
    if (isExcludedPath(currentPath)) {
        return
    }
    document.listFiles().forEach { child ->
        val name = child.name?.takeIf { it.isNotBlank() } ?: return@forEach
        val childPath = currentPath.trimEnd(File.separatorChar) + File.separator + name
        if (child.isDirectory) {
            scanDocumentDirectory(child, childPath, extensions, fileList, isExcludedPath)
        } else if (!isExcludedPath(childPath) && child.extensionLowercase() in extensions) {
            fileList.add(
                ScannedAudioSource(
                    path = childPath,
                    uri = child.uri,
                    fileSize = child.length().coerceAtLeast(0L),
                    lastModified = child.lastModified().coerceAtLeast(0L)
                )
            )
        }
    }
}

private fun DocumentFile.extensionLowercase(): String {
    val name = name ?: return ""
    return name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
}

private fun extractEmbeddedLyrics(context: Context, path: String, mediaStoreId: Long = -1L): String? {
    return com.example.LyricBox.utils.AudioMetadataReader.readLyrics(context, path, mediaStoreId)
}

private fun resolveExternalTtmlFile(audioPath: String): File? {
    val audioFile = File(audioPath)
    val parentDir = audioFile.parentFile ?: return null
    return File(parentDir, "${audioFile.nameWithoutExtension}.ttml")
}

private fun readExternalTtmlWithFallback(context: Context, ttmlFile: File): String? {
    val directRead = runCatching {
        ttmlFile.readText().takeIf { it.isNotBlank() }
    }.getOrNull()
    if (!directRead.isNullOrBlank()) return directRead

    val contentUri = resolveMediaStoreFileUriForTtml(context, ttmlFile) ?: return null
    return runCatching {
        context.contentResolver.openInputStream(contentUri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            reader.readText().takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}

@Suppress("DEPRECATION")
private fun resolveMediaStoreFileUriForTtml(context: Context, ttmlFile: File): Uri? {
    val baseUri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Files.FileColumns._ID)
    val byData = runCatching {
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val args = arrayOf(ttmlFile.absolutePath)
        context.contentResolver.query(baseUri, projection, selection, args, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            ContentUris.withAppendedId(baseUri, id)
        }
    }.getOrNull()
    if (byData != null) return byData

    val relativePath = buildRelativePathForMediaStore(ttmlFile.absolutePath) ?: return null
    return runCatching {
        val selection =
            "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val args = arrayOf(relativePath, ttmlFile.name)
        context.contentResolver.query(baseUri, projection, selection, args, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@runCatching null
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            ContentUris.withAppendedId(baseUri, id)
        }
    }.getOrNull()
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

private suspend fun loadAudioLyricsLoadResult(
    context: Context,
    audioPath: String,
    mediaStoreId: Long = -1L
): AudioLyricsLoadResult =
    withContext(Dispatchers.IO) {
        val embeddedLyrics = extractEmbeddedLyrics(context, audioPath, mediaStoreId)?.takeIf { it.isNotBlank() }
        val ttmlFile = resolveExternalTtmlFile(audioPath)
        val externalLyrics = if (ttmlFile != null) {
            try {
                readExternalTtmlWithFallback(context, ttmlFile)
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error reading external TTML", e)
                null
            }
        } else {
            null
        }
        val detectedEmbeddedFormat = embeddedLyrics?.let { detectLyricsFormat(it) } ?: 0
        AudioLyricsLoadResult(
            embeddedLyrics = embeddedLyrics,
            externalLyrics = externalLyrics,
            externalLyricsPath = ttmlFile?.absolutePath?.takeIf { externalLyrics != null },
            detectedEmbeddedFormat = detectedEmbeddedFormat
        )
    }

private suspend fun loadAudioOptionsCoverBitmap(coverCachePath: String?): Bitmap? =
    withContext(Dispatchers.IO) {
        if (coverCachePath.isNullOrBlank()) return@withContext null
        try {
            loadCoverBitmapFromCache(coverCachePath, Int.MAX_VALUE, Int.MAX_VALUE)
        } catch (e: Exception) {
            Log.e("MusicLibrary", "Error loading cover bitmap", e)
            null
        }
    }

private fun loadCoverBitmapFromCache(
    cachePath: String,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    val cacheFile = File(cachePath)
    if (!cacheFile.exists()) return null
    val bytes = cacheFile.readBytes()
    return decodeCoverBitmap(bytes, reqWidth, reqHeight)
}

private fun decodeCoverBitmap(
    bytes: ByteArray,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    if (bytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null

    options.inJustDecodeBounds = false
    options.inSampleSize = MusicLibraryActivity.calculateInSampleSize(options, reqWidth, reqHeight)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun rebuildCoverCacheBitmap(
    context: Context,
    audio: AudioFile,
    cachePath: String,
    targetSizePx: Int
): Bitmap? {
    val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
        context = context,
        filePath = audio.path,
        mediaStoreId = audio.mediaStoreId
    )
    val coverData = metadata.cover ?: run {
        MusicLibraryActivity.removeCoverFromCache(cachePath)
        MusicLibraryActivity.markNoCover(cachePath)
        return null
    }
    MusicLibraryActivity.clearNoCoverMark(cachePath)
    val cacheFile = File(cachePath)
    return if (writeCoverThumbnailToFile(cacheFile, coverData)) {
        MusicLibraryActivity.removeCoverFromCache(cachePath)
        loadCoverBitmapFromCache(cachePath, targetSizePx, targetSizePx)
    } else {
        decodeCoverBitmap(coverData, targetSizePx, targetSizePx)
    }
}

private fun getCoverCacheDir(context: Context): File {
    val cacheDir = File(context.cacheDir, "covers")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    return cacheDir
}

private fun resolveCoverCachePath(
    context: Context,
    audioFilePath: String,
    cacheDiscriminator: String? = null
): String {
    val cacheDir = getCoverCacheDir(context)
    val baseHash = audioFilePath.hashCode().toString()
    val fileName = if (cacheDiscriminator.isNullOrBlank()) {
        "$baseHash.jpg"
    } else {
        "${baseHash}_${cacheDiscriminator.hashCode()}.jpg"
    }
    return File(cacheDir, fileName).absolutePath
}

private fun saveCoverToCache(
    context: Context,
    audioFilePath: String,
    coverData: ByteArray,
    cacheDiscriminator: String? = null
): String? {
    return try {
        val cacheFile = File(resolveCoverCachePath(context, audioFilePath, cacheDiscriminator))

        if (writeCoverThumbnailToFile(cacheFile, coverData)) {
            MusicLibraryActivity.clearNoCoverMark(cacheFile.absolutePath)
            cacheFile.absolutePath
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error saving cover to cache", e)
        null
    }
}

private fun writeCoverThumbnailToFile(cacheFile: File, coverData: ByteArray): Boolean {
    return try {
        cacheFile.parentFile?.mkdirs()

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(coverData, 0, coverData.size, options)

        val targetSize = 100
        var scaleFactor = 1
        while (options.outWidth / scaleFactor / 2 >= targetSize &&
            options.outHeight / scaleFactor / 2 >= targetSize
        ) {
            scaleFactor *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = scaleFactor
        val scaledBitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size, options)

        val resizedBitmap = if (scaledBitmap != null) {
            val width = scaledBitmap.width
            val height = scaledBitmap.height
            val minSize = minOf(width, height)
            val cropX = (width - minSize) / 2
            val cropY = (height - minSize) / 2
            val squareBitmap = Bitmap.createBitmap(scaledBitmap, cropX, cropY, minSize, minSize)
            val resized = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
            if (squareBitmap != scaledBitmap && squareBitmap != resized) {
                squareBitmap.recycle()
            }
            resized
        } else {
            null
        }

        if (resizedBitmap != null) {
            java.io.FileOutputStream(cacheFile).use { outputStream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
            }
            resizedBitmap.recycle()
            if (scaledBitmap != null && scaledBitmap != resizedBitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        } else {
            cacheFile.writeBytes(coverData)
        }
        true
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error writing cover thumbnail", e)
        false
    }
}

private fun loadCoverFromCache(cachePath: String): ByteArray? {
    return try {
        val cacheFile = File(cachePath)
        if (cacheFile.exists()) {
            cacheFile.readBytes()
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error loading cover from cache", e)
        null
    }
}

private fun clearOldCoverCache(context: Context, validPaths: Set<String>) {
    try {
        val cacheDir = getCoverCacheDir(context)
        cacheDir.listFiles()?.forEach { file ->
            if (!validPaths.contains(file.absolutePath)) {
                file.delete()
            }
        }
        MusicLibraryActivity.retainNoCoverMarks(validPaths)
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error clearing old cover cache", e)
    }
}

@Composable
fun ExternalAudioScreen(
    audio: AudioFile,
    onBack: () -> Unit,
    onEditLyrics: (String?, String, Boolean) -> Unit,
    onEditMetadata: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAudioOptionsDialog by remember { mutableStateOf(false) }
    var hasAppliedDefaultAction by remember(audio.path) { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val songClickAction = remember { prefs.getString("songClickAction", "") } ?: ""
    val hasConfirmedSongClickAction = remember {
        prefs.getBoolean(PREF_SONG_CLICK_ACTION_CONFIRMED, false)
    }
    val autoDetectEmbeddedLyricsType = remember { prefs.getBoolean("autoDetectEmbeddedLyricsType", false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(audio.path, songClickAction, autoDetectEmbeddedLyricsType) {
        if (hasAppliedDefaultAction) return@LaunchedEffect
        hasAppliedDefaultAction = true
        showAudioOptionsDialog = false
        if (!hasConfirmedSongClickAction || songClickAction.isBlank()) {
            showAudioOptionsDialog = true
        } else if (songClickAction == "editMetadata") {
            onEditMetadata(audio.path, true)
        } else {
            handleMusicLibraryItemLyricsAction(
                scope = scope,
                context = context,
                audio = audio,
                autoDetectEmbeddedLyricsType = autoDetectEmbeddedLyricsType,
                onShowOptions = { showAudioOptionsDialog = true },
                onStartLyricTimingEditor = { _, lyricsContent, format ->
                    onEditLyrics(lyricsContent, format, true)
                }
            )
        }
    }
    
    // 当从编辑页面返回时，重新弹出对话框
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasAppliedDefaultAction) {
                showAudioOptionsDialog = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        CommonHeadBar(
            title = stringResource(R.string.ml_music_library_audio_files),
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    showAudioOptionsDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_play),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = audio.displayTitle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = audio.displayArtist,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.ml_music_library_tap_to_continue),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
    
    if (showAudioOptionsDialog) {
        AudioOptionsDialog(
            audio = audio,
            autoDetectEmbeddedLyricsType = autoDetectEmbeddedLyricsType,
            onDismiss = {
                showAudioOptionsDialog = false
            },
            onEditLyrics = { lyricsContent, format ->
                showAudioOptionsDialog = false
                onEditLyrics(lyricsContent, format, false)
            },
            onEditMetadata = { path ->
                showAudioOptionsDialog = false
                onEditMetadata(path, false)
            },
            showEditMetadataButton = false
        )
    }
}

@Composable
private fun MultiSelectActionBar(
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onClearSelection: () -> Unit,
    onRangeSelect: () -> Unit,
    rangeSelectEnabled: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .clickable(onClick = onSelectAll)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.ml_music_library_select_all), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .clickable(onClick = onInvertSelection)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.ml_music_library_invert_selection), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .clickable(onClick = onClearSelection)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.ml_music_library_clear_selection), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (rangeSelectEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                )
                .clickable(enabled = rangeSelectEnabled, onClick = onRangeSelect)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.ml_music_library_range),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (rangeSelectEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                }
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                .clickable(onClick = onExit)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.ml_music_library_close), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SelectActionChip(
    @androidx.annotation.DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 14.dp),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RangeSelectDialog(
    maxIndex: Int,
    lastSelectedIndices: List<Int>,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        if (lastSelectedIndices.size >= 2) {
            val sorted = lastSelectedIndices.sorted()
            startText = sorted[0].toString()
            endText = sorted[1].toString()
        } else if (lastSelectedIndices.size == 1) {
            startText = lastSelectedIndices[0].toString()
            endText = lastSelectedIndices[0].toString()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.ml_music_library_range_select_by_index),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ml_music_library_current_list_count, maxIndex),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ml_music_library_start),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = startText,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { c -> c.isDigit() }) startText = it 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (startText.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_input_start_index),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
                
                Text(
                    text = stringResource(R.string.ml_music_library_to),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ml_music_library_end),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = endText,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { c -> c.isDigit() }) endText = it 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (endText.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_input_end_index),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = {
                        val start = startText.toIntOrNull() ?: 1
                        val end = endText.toIntOrNull() ?: maxIndex
                        onConfirm(start, end)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_confirm))
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchOperationFAB(
    isVisible: Boolean,
    showSheet: Boolean,
    onShowSheetChange: (Boolean) -> Unit,
    onBatchMatch: () -> Unit,
    onBatchLyricMatch: () -> Unit,
    onBatchRename: () -> Unit,
    onBatchLyricsEdit: () -> Unit,
    selectedPaths: Set<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fabScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fabScale"
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    if (fabScale > 0.01f) {
        Box(modifier = modifier) {
            FloatingActionButton(
                onClick = { onShowSheetChange(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                        alpha = fabScale
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pencil),
                    contentDescription = stringResource(R.string.ml_music_library_batch_actions),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { onShowSheetChange(false) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_batch_actions),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BatchOperationMenuItem(
                        title = stringResource(R.string.ml_music_library_batch_match_tags),
                        onClick = {
                            onShowSheetChange(false)
                            onBatchMatch()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatchOperationMenuItem(
                        title = stringResource(R.string.ml_music_library_batch_match_lyrics),
                        onClick = {
                            onShowSheetChange(false)
                            onBatchLyricMatch()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatchOperationMenuItem(
                        title = stringResource(R.string.ml_music_library_batch_rename),
                        onClick = {
                            onShowSheetChange(false)
                            onBatchRename()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatchOperationMenuItem(
                        title = stringResource(R.string.ml_music_library_batch_edit_lyrics),
                        onClick = {
                            onShowSheetChange(false)
                            onBatchLyricsEdit()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatchOperationMenuItem(
                        title = stringResource(R.string.ml_music_library_batch_edit_metadata),
                        onClick = {
                            onShowSheetChange(false)
                            val intent = Intent(context, com.example.LyricBox.SongMetadataEditActivity::class.java).apply {
                                putExtra(com.example.LyricBox.SongMetadataEditActivity.EXTRA_IS_BATCH_EDIT, true)
                                putStringArrayListExtra(com.example.LyricBox.SongMetadataEditActivity.EXTRA_SELECTED_PATHS, ArrayList(selectedPaths))
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchOperationMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchLyricsEditSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onOpenBatchConvert: () -> Unit,
    onOpenBatchExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { sheetState.show() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.ml_music_library_batch_edit_lyrics),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ml_music_library_selected_audio_count, selectedCount),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            BatchOperationMenuItem(
                title = stringResource(R.string.ml_music_library_batch_convert_lyrics_format),
                onClick = onOpenBatchConvert
            )
            Spacer(modifier = Modifier.height(8.dp))
            BatchOperationMenuItem(
                title = stringResource(R.string.ml_music_library_batch_export_external_lyrics),
                onClick = onOpenBatchExport
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchLyricsConvertSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onStartConvert: (BatchLyricsTargetFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val savedFormat = remember {
        runCatching {
            BatchLyricsTargetFormat.valueOf(
                prefs.getString("batchLyricsConvertTargetFormat", BatchLyricsTargetFormat.LRC_WORD.name)
                    ?: BatchLyricsTargetFormat.LRC_WORD.name
            )
        }.getOrDefault(BatchLyricsTargetFormat.LRC_WORD)
    }
    var targetFormat by remember { mutableStateOf(savedFormat) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { sheetState.show() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.ml_music_library_batch_convert_lyrics_format),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ml_music_library_selected_audio_count, selectedCount),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.ml_music_library_target_lyrics_format),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatchLyricsTargetFormat.values().forEach { format ->
                    val selected = targetFormat == format
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                            .clickable { targetFormat = format }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(format.labelRes),
                            fontSize = 15.sp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = {
                        prefs.edit()
                            .putString("batchLyricsConvertTargetFormat", targetFormat.name)
                            .apply()
                        onStartConvert(targetFormat)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_confirm_convert))
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchLyricsExternalExportSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onStartExport: (BatchLyricsExternalExportConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val savedPreferSameNameTtml = remember { prefs.getBoolean("batchLyricsExportPreferSameNameTtml", true) }
    val savedUseCustomDirectory = remember { prefs.getBoolean("batchLyricsExportUseCustomDirectory", false) }
    val savedCustomUri = remember {
        prefs.getString("batchLyricsExportCustomDirectoryUri", null)?.let { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()
        }
    }
    val savedFormat = remember {
        runCatching {
            BatchLyricsTargetFormat.valueOf(
                prefs.getString("batchLyricsExportTargetFormat", BatchLyricsTargetFormat.LRC_WORD.name)
                    ?: BatchLyricsTargetFormat.LRC_WORD.name
            )
        }.getOrDefault(BatchLyricsTargetFormat.LRC_WORD)
    }

    var preferSameNameTtml by remember { mutableStateOf(savedPreferSameNameTtml) }
    var useCustomDirectory by remember { mutableStateOf(savedUseCustomDirectory) }
    var customDirectoryUri by remember { mutableStateOf(savedCustomUri) }
    var targetFormat by remember { mutableStateOf(savedFormat) }
    val customDirectoryName = remember(customDirectoryUri) {
        customDirectoryUri?.let { uri ->
            DocumentFile.fromTreeUri(context, uri)?.name?.takeIf { it.isNotBlank() } ?: uri.toString()
        } ?: context.getString(R.string.ml_music_library_directory_not_selected)
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            customDirectoryUri = uri
            useCustomDirectory = true
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canStartExport = !useCustomDirectory || customDirectoryUri != null

    LaunchedEffect(Unit) { sheetState.show() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.ml_music_library_batch_export_external_lyrics),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ml_music_library_selected_audio_count, selectedCount),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable { preferSameNameTtml = !preferSameNameTtml }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = preferSameNameTtml,
                    onCheckedChange = { preferSameNameTtml = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ml_music_library_prefer_same_name_ttml),
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.ml_music_library_external_lyrics_format),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatchLyricsTargetFormat.values().forEach { format ->
                    val selected = targetFormat == format
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                            .clickable { targetFormat = format }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "${stringResource(format.labelRes)}（${format.extension}）",
                            fontSize = 15.sp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.ml_music_library_save_directory),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (!useCustomDirectory) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                    .clickable { useCustomDirectory = false }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !useCustomDirectory,
                    onClick = { useCustomDirectory = false }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ml_music_library_save_to_audio_directory), fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (useCustomDirectory) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                    .clickable { useCustomDirectory = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = useCustomDirectory,
                        onClick = { useCustomDirectory = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ml_music_library_select_custom_directory), fontSize = 15.sp)
                }
                if (useCustomDirectory) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = customDirectoryName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { directoryPickerLauncher.launch(customDirectoryUri) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ml_music_library_use_system_file_picker))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = {
                        prefs.edit()
                            .putBoolean("batchLyricsExportPreferSameNameTtml", preferSameNameTtml)
                            .putString("batchLyricsExportTargetFormat", targetFormat.name)
                            .putBoolean("batchLyricsExportUseCustomDirectory", useCustomDirectory)
                            .putString("batchLyricsExportCustomDirectoryUri", customDirectoryUri?.toString())
                            .apply()
                        onStartExport(
                            BatchLyricsExternalExportConfig(
                                preferSameNameTtml = preferSameNameTtml,
                                targetFormat = targetFormat,
                                useCustomDirectory = useCustomDirectory,
                                customDirectoryUri = customDirectoryUri
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canStartExport
                ) {
                    Text(stringResource(R.string.ml_music_library_confirm_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchMatchConfigSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onStartMatch: (BatchMatchConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val blockSheetDragFromContent = remember {
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BatchMatchConfig", Context.MODE_PRIVATE) }
    
    fun loadConfig(): Triple<List<BatchMatchField>, List<Source>, Int> {
        val defaultFields = listOf(
            BatchMatchField("cover", "封面", true),
            BatchMatchField("title", "标题", true),
            BatchMatchField("artist", "艺术家", true),
            BatchMatchField("album", "专辑", true),
            BatchMatchField("year", "年份", true),
            BatchMatchField("trackNumber", "音轨号", true),
            BatchMatchField("discNumber", "碟号", true),
            BatchMatchField("genre", "风格", true),
            BatchMatchField("albumArtist", "专辑艺术家", true),
            BatchMatchField("composer", "作曲", true),
            BatchMatchField("lyricist", "作词", true),
            BatchMatchField("comment", "注释", true),
            BatchMatchField("copyrightInfo", "版权信息", true)
        )
        
        val fields = defaultFields.map { field ->
            val enabled = prefs.getBoolean("field_${field.key}_enabled", field.enabled)
            val modeStr = prefs.getString("field_${field.key}_mode", field.mode.name)
            val mode = try {
                FieldMatchMode.valueOf(modeStr ?: field.mode.name)
            } catch (e: Exception) {
                field.mode
            }
            field.copy(enabled = enabled, mode = mode)
        }
        
        val sourcesStr = prefs.getString("selectedSources", null)
        val sources = if (sourcesStr != null) {
            try {
                sourcesStr.split(",").map { Source.valueOf(it) }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        val threadCount = prefs.getInt("threadCount", 3)
        
        return Triple(fields, sources, threadCount)
    }
    
    fun saveConfig(fields: List<BatchMatchField>, sources: List<Source>, threadCount: Int) {
        val editor = prefs.edit()
        fields.forEach { field ->
            editor.putBoolean("field_${field.key}_enabled", field.enabled)
            editor.putString("field_${field.key}_mode", field.mode.name)
        }
        editor.putString("selectedSources", sources.joinToString(","))
        editor.putInt("threadCount", threadCount)
        editor.apply()
    }
    
    val initialConfig = remember { loadConfig() }
    var fields by remember { mutableStateOf(initialConfig.first) }
    var selectedSources by remember { mutableStateOf(initialConfig.second) }
    var threadCount by remember { mutableStateOf(initialConfig.third) }
    val closeSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    
    LaunchedEffect(fields, selectedSources, threadCount) {
        saveConfig(fields, selectedSources, threadCount)
    }
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    ModalBottomSheet(
        modifier = modifier.statusBarsPadding(),
        onDismissRequest = closeSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(blockSheetDragFromContent)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp)
            ) {
            Text(
                text = stringResource(R.string.ml_music_library_batch_match_tags_config),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_selected_audio_count, selectedCount),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_match_fields),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ml_music_library_tag_match_mode_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { fields = fields.map { it.copy(enabled = true) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Text(stringResource(R.string.ml_music_library_select_all), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = { fields = fields.map { it.copy(enabled = !it.enabled) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    ) {
                        Text(stringResource(R.string.ml_music_library_invert_selection), fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { fields = fields.map { it.copy(mode = FieldMatchMode.SUPPLEMENT) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Text(stringResource(R.string.ml_music_library_all_supplement), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = { fields = fields.map { it.copy(mode = FieldMatchMode.OVERWRITE) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    ) {
                        Text(stringResource(R.string.ml_music_library_all_overwrite), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            val chunkedFields = fields.chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                chunkedFields.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEachIndexed { chunkIndex, field ->
                            val index = chunkedFields.indexOf(chunk) * 2 + chunkIndex
                            val isSelected = field.enabled
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .clickable {
                                        fields = fields.toMutableList().apply {
                                            this[index] = field.copy(enabled = !field.enabled)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = batchMatchFieldLabel(field.key),
                                        fontSize = 14.sp,
                                        color = if (isSelected) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                    val modeLabel = if (field.mode == FieldMatchMode.SUPPLEMENT) stringResource(R.string.ml_music_library_mode_supplement) else stringResource(R.string.ml_music_library_mode_overwrite)
                                    val modeColor = if (field.mode == FieldMatchMode.SUPPLEMENT) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(modeColor.copy(alpha = 0.1f))
                                            .clickable {
                                                fields = fields.toMutableList().apply {
                                                    this[index] = field.copy(
                                                        mode = if (field.mode == FieldMatchMode.SUPPLEMENT) 
                                                            FieldMatchMode.OVERWRITE 
                                                        else 
                                                            FieldMatchMode.SUPPLEMENT
                                                    )
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = modeLabel,
                                            fontSize = 11.sp,
                                            color = modeColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        if (chunk.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_match_sources),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val sourceLabels = mapOf(
                Source.ITUNES to "AM",
                Source.QM to "QM",
                Source.NE to "NE"
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sourceLabels.forEach { (source, label) ->
                    val isSelected = source in selectedSources
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable {
                                if (source in selectedSources) {
                                    selectedSources = selectedSources - source
                                } else {
                                    selectedSources = selectedSources + source
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                val idx = selectedSources.indexOf(source)
                                Text(
                                    text = "#${idx + 1}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_thread_count, threadCount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = threadCount.toFloat(),
                onValueChange = { threadCount = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = closeSheet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = {
                        val config = BatchMatchConfig(
                            fields = fields.filter { it.enabled }.ifEmpty { fields.map { it.copy(enabled = true) } },
                            sources = selectedSources.ifEmpty { listOf(Source.ITUNES, Source.QM, Source.NE) },
                            threadCount = threadCount
                        )
                        onStartMatch(config)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedSources.isNotEmpty()
                ) {
                    Text(stringResource(R.string.ml_music_library_start_match))
                }
            }
        }
    }
}

@Composable
private fun batchMatchFieldLabel(key: String): String = when (key) {
    "cover" -> stringResource(R.string.ml_music_library_field_cover)
    "title" -> stringResource(R.string.ml_music_library_field_title)
    "artist" -> stringResource(R.string.ml_music_library_field_artist)
    "album" -> stringResource(R.string.ml_music_library_field_album)
    "year" -> stringResource(R.string.ml_music_library_field_year)
    "trackNumber" -> stringResource(R.string.ml_music_library_field_track_number)
    "discNumber" -> stringResource(R.string.ml_music_library_field_disc_number)
    "genre" -> stringResource(R.string.ml_music_library_field_genre)
    "albumArtist" -> stringResource(R.string.ml_music_library_field_album_artist)
    "composer" -> stringResource(R.string.ml_music_library_field_composer)
    "lyricist" -> stringResource(R.string.ml_music_library_field_lyricist)
    "comment" -> stringResource(R.string.ml_music_library_field_comment)
    "copyrightInfo" -> stringResource(R.string.ml_music_library_field_copyright_info)
    else -> key
}

@Composable
private fun BatchMatchProgressDialog(
    current: Int,
    total: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.ml_music_library_matching))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$current / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.ml_music_library_cancel_match), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun BatchLyricMatchProgressDialog(
    current: Int,
    total: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.ml_music_library_matching_lyrics))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$current / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.ml_music_library_cancel_match), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun BatchLyricsConvertProgressDialog(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.ml_music_library_converting_lyrics))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$current / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun BatchLyricsExportProgressDialog(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.ml_music_library_saving_external_lyrics))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$current / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchLyricMatchResultSheet(
    result: BatchLyricMatchResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedItem by remember { mutableStateOf<BatchLyricMatchItem?>(null) }
    var showLyricViewer by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    if (showLyricViewer && selectedItem != null) {
        LyricViewerSheet(
            item = selectedItem!!,
            onDismiss = { showLyricViewer = false }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ml_music_library_match_lyrics_result),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.ml_music_library_success_failed, result.totalSuccess, result.totalFailed),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            result.items.forEach { item ->
                BatchLyricMatchResultItemRow(
                    item = item,
                    onClick = {
                        selectedItem = item
                        showLyricViewer = true
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ml_music_library_close), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun BatchLyricMatchResultItemRow(
    item: BatchLyricMatchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (item.matchStatus) {
        MatchStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        MatchStatus.FAILED -> MaterialTheme.colorScheme.error
        MatchStatus.SKIPPED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val statusText = when (item.matchStatus) {
        MatchStatus.SUCCESS -> stringResource(R.string.ml_music_library_status_success)
        MatchStatus.FAILED -> stringResource(R.string.ml_music_library_status_failed)
        MatchStatus.SKIPPED -> stringResource(R.string.ml_music_library_status_skipped)
        else -> stringResource(R.string.ml_music_library_status_processing)
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.displayArtist,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            if (item.matchSource != null && item.matchStatus == MatchStatus.SUCCESS) {
                Text(
                    text = batchMatchSourceShortName(item.matchSource!!),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (item.matchStatus == MatchStatus.SUCCESS && item.similarityScore > 0) {
                Text(
                    text = String.format("%.0f%%", item.similarityScore * 100),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricViewerSheet(
    item: BatchLyricMatchItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = item.displayTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.displayArtist,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val statusColor = when (item.matchStatus) {
                MatchStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                MatchStatus.FAILED -> MaterialTheme.colorScheme.error
                MatchStatus.SKIPPED -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            val statusText = when (item.matchStatus) {
                MatchStatus.SUCCESS -> stringResource(R.string.ml_music_library_status_success)
                MatchStatus.FAILED -> stringResource(R.string.ml_music_library_status_failed)
                MatchStatus.SKIPPED -> stringResource(R.string.ml_music_library_status_skipped)
                else -> stringResource(R.string.ml_music_library_status_processing)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
                if (item.matchSource != null && item.matchStatus == MatchStatus.SUCCESS) {
                    Text(
                        text = stringResource(R.string.ml_music_library_from_source, batchMatchSourceShortName(item.matchSource!!)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.matchStatus == MatchStatus.SUCCESS && item.similarityScore > 0) {
                    Text(
                        text = stringResource(R.string.ml_music_library_similarity_percent, item.similarityScore * 100),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (item.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.error!!,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            if (item.matchedLyrics != null) {
                Text(
                    text = stringResource(R.string.ml_music_library_new_lyrics_prefix),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.matchedLyrics!!,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            } else if (!item.originalLyrics.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.ml_music_library_original_lyrics_prefix),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = extractPlainTextFromLrc(item.originalLyrics!!),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            } else {
                Text(
                    text = stringResource(R.string.ml_music_library_no_lyrics),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ml_music_library_close), fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchMatchLyricsSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onStartMatch: (BatchLyricMatchConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val blockSheetDragFromContent = remember {
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
    
    val defaultSources = remember {
        prefs.getString("batchLyricMatchSources", null)
            ?.split(",")
            ?.mapNotNull { raw -> runCatching { Source.valueOf(raw) }.getOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(Source.QM)
    }
    val defaultMode = remember {
        runCatching {
            LyricMatchMode.valueOf(
                prefs.getString("batchLyricMatchMode", LyricMatchMode.SUPPLEMENT.name)
                    ?: LyricMatchMode.SUPPLEMENT.name
            )
        }.getOrDefault(LyricMatchMode.SUPPLEMENT)
    }
    val defaultLyricType = remember {
        runCatching {
            LyricType.valueOf(
                prefs.getString("batchLyricMatchType", LyricType.VERBATIM.name)
                    ?: LyricType.VERBATIM.name
            )
        }.getOrDefault(LyricType.VERBATIM)
    }
    val defaultThreadCount = remember {
        prefs.getInt("batchLyricMatchThreadCount", 3).coerceIn(1, 5)
    }
    val defaultFilterMetadata = remember {
        prefs.getBoolean("batchLyricMatchFilterMetadata", false)
    }
    val defaultIncludeTranslation = remember {
        prefs.getBoolean("batchLyricMatchIncludeTranslation", false)
    }
    
    var selectedSources by remember { mutableStateOf(defaultSources) }
    var matchMode by remember { mutableStateOf(defaultMode) }
    var lyricType by remember { mutableStateOf(defaultLyricType) }
    var threadCount by remember { mutableStateOf(defaultThreadCount) }
    var filterMetadata by remember { mutableStateOf(defaultFilterMetadata) }
    var includeTranslation by remember { mutableStateOf(defaultIncludeTranslation) }
    val closeSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    ModalBottomSheet(
        modifier = modifier.statusBarsPadding(),
        onDismissRequest = closeSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(blockSheetDragFromContent)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp)
            ) {
            Text(
                text = stringResource(R.string.ml_music_library_batch_match_lyrics),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_selected_audio_count, selectedCount),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_lyrics_match_sources),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val sourceLabels = mapOf(
                Source.QM to "QM",
                Source.NE to "NE",
                Source.KG to "KG"
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sourceLabels.forEach { (source, label) ->
                    val isSelected = source in selectedSources
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable {
                                if (source in selectedSources) {
                                    selectedSources = selectedSources - source
                                } else {
                                    selectedSources = selectedSources + source
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                val idx = selectedSources.indexOf(source)
                                Text(
                                    text = "#${idx + 1}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_lyrics_type),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isVerbatim = lyricType == LyricType.VERBATIM
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isVerbatim)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { lyricType = LyricType.VERBATIM }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_verbatim_lyrics),
                        fontSize = 15.sp,
                        color = if (isVerbatim)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isVerbatim) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (!isVerbatim)
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { lyricType = LyricType.LINE }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_line_lyrics),
                        fontSize = 15.sp,
                        color = if (!isVerbatim)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (!isVerbatim) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_match_mode),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ml_music_library_lyric_match_mode_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isSupplement = matchMode == LyricMatchMode.SUPPLEMENT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSupplement)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { matchMode = LyricMatchMode.SUPPLEMENT }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_mode_supplement),
                        fontSize = 15.sp,
                        color = if (isSupplement)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSupplement) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (!isSupplement)
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { matchMode = LyricMatchMode.OVERWRITE }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_mode_overwrite),
                        fontSize = 15.sp,
                        color = if (!isSupplement)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (!isSupplement) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_other_settings),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (filterMetadata)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { filterMetadata = !filterMetadata }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_keep_lyrics_only),
                        fontSize = 15.sp,
                        color = if (filterMetadata)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (filterMetadata) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (includeTranslation)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { includeTranslation = !includeTranslation }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_include_translation),
                        fontSize = 15.sp,
                        color = if (includeTranslation)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (includeTranslation) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_thread_count, threadCount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = threadCount.toFloat(),
                onValueChange = { threadCount = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = closeSheet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = {
                        prefs.edit()
                            .putString(
                                "batchLyricMatchSources",
                                selectedSources.joinToString(",") { it.name }
                            )
                            .putString("batchLyricMatchMode", matchMode.name)
                            .putString("batchLyricMatchType", lyricType.name)
                            .putInt("batchLyricMatchThreadCount", threadCount)
                            .putBoolean("batchLyricMatchFilterMetadata", filterMetadata)
                            .putBoolean("batchLyricMatchIncludeTranslation", includeTranslation)
                            .apply()

                        val config = BatchLyricMatchConfig(
                            sources = selectedSources.ifEmpty { listOf(Source.QM, Source.NE, Source.KG) },
                            mode = matchMode,
                            lyricType = lyricType,
                            threadCount = threadCount,
                            filterMetadata = filterMetadata,
                            includeTranslation = includeTranslation
                        )
                        onStartMatch(config)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedSources.isNotEmpty()
                ) {
                    Text(stringResource(R.string.ml_music_library_start_match))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchMatchResultSheet(
    result: BatchMatchResult,
    onDismiss: () -> Unit,
    onUndoField: (BatchMatchItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedItem by remember { mutableStateOf<BatchMatchItem?>(null) }
    var showItemDetail by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    if (showItemDetail && selectedItem != null) {
        BatchMatchItemDetailSheet(
            item = selectedItem!!,
            onUndoField = { fieldKey ->
                onUndoField(selectedItem!!, fieldKey)
            },
            onDismiss = { showItemDetail = false }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ml_music_library_match_result),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.ml_music_library_success_failed, result.totalSuccess, result.totalFailed),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            result.items.forEach { item ->
                BatchMatchResultItemRow(
                    item = item,
                    onClick = {
                        selectedItem = item
                        showItemDetail = true
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ml_music_library_close), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun BatchMatchResultItemRow(
    item: BatchMatchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (item.matchStatus) {
        MatchStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        MatchStatus.FAILED -> MaterialTheme.colorScheme.error
        MatchStatus.SKIPPED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val statusText = when (item.matchStatus) {
        MatchStatus.SUCCESS -> stringResource(R.string.ml_music_library_status_success)
        MatchStatus.FAILED -> stringResource(R.string.ml_music_library_status_failed)
        MatchStatus.SKIPPED -> stringResource(R.string.ml_music_library_status_skipped)
        else -> stringResource(R.string.ml_music_library_status_processing)
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.displayArtist,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            if (item.matchSource != null && item.matchStatus == MatchStatus.SUCCESS) {
                Text(
                    text = batchMatchSourceShortName(item.matchSource!!),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverViewerSheet(
    bitmap: android.graphics.Bitmap,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.ml_music_library_image_size, bitmap.width, bitmap.height),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ml_music_library_close), fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchMatchItemDetailSheet(
    item: BatchMatchItem,
    onUndoField: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var undoneFields by remember { mutableStateOf(setOf<String>()) }
    var showCoverFull by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var coverTitle by remember { mutableStateOf("") }
    
    val displayData = item.matchedData.filter { it.key !in undoneFields }
    val hasCover = item.coverBitmap != null && "cover" !in undoneFields
    val hasOriginalCover = item.originalCoverBitmap != null
    
    if (showCoverFull != null) {
        CoverViewerSheet(
            bitmap = showCoverFull!!,
            title = coverTitle,
            onDismiss = { showCoverFull = null }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = item.displayTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.displayArtist,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val statusText = when (item.matchStatus) {
                MatchStatus.SUCCESS -> stringResource(R.string.ml_music_library_match_success)
                MatchStatus.FAILED -> stringResource(R.string.ml_music_library_match_failed)
                MatchStatus.SKIPPED -> stringResource(R.string.ml_music_library_status_skipped)
                else -> stringResource(R.string.ml_music_library_unknown)
            }
            val statusColor = when (item.matchStatus) {
                MatchStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                MatchStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = stringResource(R.string.ml_music_library_status_with_value, statusText),
                fontSize = 13.sp,
                color = statusColor
            )
            
            val errorText = item.error
            if (errorText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ml_music_library_error_with_value, errorText),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_field_changes_detail),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (displayData.isEmpty() && !hasCover) {
                Text(
                    text = stringResource(R.string.ml_music_library_no_changed_fields),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                ) {
                    if (hasCover) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.ml_music_library_album_cover),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val originalCoverTitleText = stringResource(R.string.ml_music_library_original_cover)
                                val newCoverTitleText = stringResource(R.string.ml_music_library_new_cover)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_original_short),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    if (hasOriginalCover) {
                                        androidx.compose.foundation.Image(
                                            bitmap = item.originalCoverBitmap!!.asImageBitmap(),
                                            contentDescription = originalCoverTitleText,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    showCoverFull = item.originalCoverBitmap
                                                    coverTitle = originalCoverTitleText
                                                }
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.ml_music_library_none_bracket),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_new_short),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    androidx.compose.foundation.Image(
                                        bitmap = item.coverBitmap!!.asImageBitmap(),
                                        contentDescription = stringResource(R.string.ml_music_library_album_cover),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                showCoverFull = item.coverBitmap
                                                coverTitle = newCoverTitleText
                                            }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = {
                                    undoneFields = undoneFields + "cover"
                                    onUndoField("cover")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.undo),
                                    contentDescription = stringResource(R.string.ml_music_library_revert),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    displayData.forEach { (key, newValue) ->
                        val oldValue = item.originalData[key] ?: ""
                        val fieldLabel = when (key) {
                            "title" -> stringResource(R.string.ml_music_library_field_title)
                            "artist" -> stringResource(R.string.ml_music_library_field_artist)
                            "album" -> stringResource(R.string.ml_music_library_field_album)
                            "year" -> stringResource(R.string.ml_music_library_field_year)
                            "trackNumber" -> stringResource(R.string.ml_music_library_field_track_number)
                            "discNumber" -> stringResource(R.string.ml_music_library_field_disc_number)
                            "genre" -> stringResource(R.string.ml_music_library_field_genre)
                            "albumArtist" -> stringResource(R.string.ml_music_library_field_album_artist)
                            "composer" -> stringResource(R.string.ml_music_library_field_composer)
                            "lyricist" -> stringResource(R.string.ml_music_library_field_lyricist)
                            "comment" -> stringResource(R.string.ml_music_library_field_comment)
                            "copyrightInfo" -> stringResource(R.string.ml_music_library_field_copyright_info)
                            else -> key
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = fieldLabel,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_original_short),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = oldValue.ifEmpty { stringResource(R.string.ml_music_library_empty_bracket) },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.ml_music_library_new_short),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = newValue,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = {
                                    undoneFields = undoneFields + key
                                    onUndoField(key)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.undo),
                                    contentDescription = stringResource(R.string.ml_music_library_revert),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ml_music_library_close), fontSize = 15.sp)
            }
        }
    }
}

private fun batchMatchSourceShortName(source: Source): String {
    return when (source) {
        Source.ITUNES -> "AM"
        Source.QM -> "QM"
        Source.NE -> "NE"
        Source.KG -> "KG"
    }
}

private fun getItunesCountry(context: Context): String {
    val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
    return when (prefs.getString("amRegion", "HK_SC") ?: "HK_SC") {
        "HK_SC" -> "HK"
        "HK" -> "HK"
        "CN" -> "CN"
        "JP" -> "JP"
        "KR" -> "KR"
        "US" -> "US"
        else -> "HK"
    }
}

private fun shouldConvertToSimplified(context: Context): Boolean {
    val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
    return (prefs.getString("amRegion", "HK_SC") ?: "HK_SC") == "HK_SC"
}

private suspend fun performBatchMatch(
    context: Context,
    audioFiles: List<AudioFile>,
    config: BatchMatchConfig,
    isCancelled: () -> Boolean,
    onProgress: (Int, Int) -> Unit,
    onComplete: (BatchMatchResult) -> Unit
) {
    withContext(Dispatchers.IO) {
        val lyricsService = LyricsService()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(2, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build()
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val amCoverSize = prefs.getInt("amCoverSize", 3000)
        val qmCoverSize = prefs.getInt("qmCoverSize", 1200)
        val neCoverSize = prefs.getInt("neCoverSize", 1000)
        val itunesCountry = getItunesCountry(context)
        val itunesConvertToSimplified = shouldConvertToSimplified(context)
        val artistSeparator = prefs.getString("artistSeparator", "/") ?: "/"
        
        fun scaleBitmap(bitmap: android.graphics.Bitmap, maxSize: Int = 800): android.graphics.Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            
            if (width <= maxSize && height <= maxSize) {
                return bitmap
            }
            
            val scaleRatio = maxSize.toFloat() / maxOf(width, height)
            val newWidth = (width * scaleRatio).toInt()
            val newHeight = (height * scaleRatio).toInt()
            
            return android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
        
        suspend fun loadItemMetadata(audio: AudioFile): BatchMatchItem {
            val originalData = mutableMapOf<String, String>()
            originalData["title"] = audio.title
            originalData["artist"] = audio.artist
            originalData["album"] = audio.album
            
            var originalCover: android.graphics.Bitmap? = null
            var originalCoverPicture: com.lonx.audiotag.model.AudioPicture? = null
            
            try {
                val pfd = android.os.ParcelFileDescriptor.open(java.io.File(audio.path), android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val tagData = com.lonx.audiotag.rw.AudioTagReader.read(pfd, true)
                pfd.close()
                originalData["year"] = tagData?.date ?: ""
                originalData["trackNumber"] = tagData?.trackNumber ?: ""
                originalData["discNumber"] = tagData?.discNumber?.toString() ?: ""
                originalData["genre"] = tagData?.genre ?: ""
                originalData["albumArtist"] = tagData?.albumArtist ?: ""
                originalData["composer"] = tagData?.composer ?: ""
                originalData["lyricist"] = tagData?.lyricist ?: ""
                originalData["comment"] = tagData?.comment ?: ""
                
                var copyrightValue: String? = null
                try {
                    val tagPfd = android.os.ParcelFileDescriptor.open(java.io.File(audio.path), android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val nativeFd = tagPfd.dup().detachFd()
                    val metadata = com.lonx.audiotag.TagLib.getMetadata(nativeFd, false)
                    tagPfd.close()
                    
                    if (metadata != null) {
                        val props = metadata.propertyMap
                        
                        fun firstOf(vararg keys: String): String? {
                            for (key in keys) {
                                val arr = props[key]
                                if (!arr.isNullOrEmpty()) {
                                    val value = arr[0].trim()
                                    if (value.isNotEmpty()) return value
                                }
                            }
                            return null
                        }
                        
                        copyrightValue = firstOf("COPYRIGHT", "COPYRIGHTS", "COPYRIGHTINFO")
                    }
                } catch (e: Exception) {
                    Log.e("BatchMatch", "Error reading copyright with TagLib for ${audio.path}", e)
                }
                originalData["copyrightInfo"] = copyrightValue ?: ""
                
                if (tagData?.pictures?.isNotEmpty() == true) {
                    originalCoverPicture = tagData.pictures.first()
                    val picData = originalCoverPicture.data
                    val tempBitmap = android.graphics.BitmapFactory.decodeByteArray(picData, 0, picData.size)
                    if (tempBitmap != null) {
                        originalCover = scaleBitmap(tempBitmap)
                        if (tempBitmap != originalCover) {
                            tempBitmap.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BatchMatch", "Error reading metadata for ${audio.path}", e)
            }
            
            return BatchMatchItem(
                audioFile = audio,
                originalData = originalData,
                originalCoverBitmap = originalCover,
                originalCoverData = originalCoverPicture
            )
        }
        
        val items = audioFiles.map { audio ->
            BatchMatchItem(
                audioFile = audio,
                originalData = mutableMapOf(),
                originalCoverBitmap = null,
                originalCoverData = null
            )
        }
        
        val total = items.size
        var matchedCount = 0
        var successCount = 0
        val progressMutex = Mutex()
        
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
        
        fun parseFileName(fileName: String): Pair<String, String> {
            val name = fileName.trim()
            val hyphenIndex = name.indexOf(" - ")
            if (hyphenIndex > 0) {
                val part1 = name.substring(0, hyphenIndex).trim()
                val part2 = name.substring(hyphenIndex + 3).trim()
                return Pair(part1, part2)
            }
            val dashIndex = name.indexOf("-")
            if (dashIndex > 0) {
                val part1 = name.substring(0, dashIndex).trim()
                val part2 = name.substring(dashIndex + 1).trim()
                return Pair(part1, part2)
            }
            return Pair(name, "")
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
        
        val maxConcurrency = config.threadCount.coerceIn(1, 10)
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrency)
        
        items.map { item ->
            async(Dispatchers.IO) {
                if (isCancelled()) return@async
                semaphore.acquire()
                try {
                    if (isCancelled()) return@async
                    item.matchStatus = MatchStatus.MATCHING
                    
                    val loadedItem = loadItemMetadata(item.audioFile)
                    item.originalData = loadedItem.originalData
                    item.originalCoverBitmap = loadedItem.originalCoverBitmap
                    item.originalCoverData = loadedItem.originalCoverData
                    
                    val fileName = java.io.File(item.audioFile.path).nameWithoutExtension
                    var title: String
                    var artist: String
                    
                    // 保存原始封面状态，因为后面会被清空
                    item.hasOriginalCover = item.originalCoverData != null || item.originalCoverBitmap != null
                    
                    // 当标题或艺术家有一个为空时，使用原文件名进行解析匹配
                    if (item.audioFile.title.isEmpty() || item.audioFile.artist.isEmpty()) {
                        val (parsedTitle, parsedArtist) = parseFileName(fileName)
                        if (parsedArtist.isNotEmpty()) {
                            title = parsedTitle
                            artist = parsedArtist
                        } else {
                            // 如果文件名无法解析出艺术家，则直接使用文件名作为标题
                            title = fileName
                            artist = ""
                        }
                    } else {
                        // 标题和艺术家都有值时，直接使用
                        title = item.audioFile.title
                        artist = item.audioFile.artist
                    }
                    
                    val keyword = buildString {
                        if (title.isNotEmpty()) append(title)
                        if (artist.isNotEmpty()) {
                            if (isNotEmpty()) append(" ")
                            append(artist)
                        }
                    }.ifEmpty { fileName }
                    
                    if (keyword.isBlank()) {
                        item.matchStatus = MatchStatus.SKIPPED
                        item.error = context.getString(R.string.ml_music_library_no_valid_search_keyword)
                        semaphore.release()
                        if (!isCancelled()) {
                            progressMutex.withLock {
                                matchedCount++
                            }
                            withContext(Dispatchers.Main) {
                                onProgress(matchedCount, total)
                            }
                        }
                        return@async
                    }
                    
                    var matchedSong: SongInfo? = null
                    var usedSource: Source? = null
                    var bestCombinedSim = 0f
                    
                    for (source in config.sources) {
                        try {
                            val searchResults = lyricsService.searchFromSource(
                                keyword, 
                                source,
                                itunesCountry = itunesCountry,
                                itunesConvertToSimplified = itunesConvertToSimplified,
                                itunesCoverSize = amCoverSize,
                                qmCoverSize = qmCoverSize,
                                neCoverSize = neCoverSize
                            )
                            if (searchResults.isNotEmpty()) {
                                var bestCandidateForSource: SongInfo? = null
                                var bestCandidateSimForSource = 0f
                                
                                for (candidate in searchResults) {
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
                                    
                                    val combinedSim = maxOf(
                                        (titleSim * 0.7f + artistSim * 0.3f),
                                        titleArtistReversedSim
                                    )
                                    
                                    if (combinedSim > bestCandidateSimForSource) {
                                        bestCandidateSimForSource = combinedSim
                                        bestCandidateForSource = candidate
                                    }
                                }
                                
                                if (bestCandidateForSource != null) {
                                    val detail = lyricsService.getMusicDetail(
                                        bestCandidateForSource, 
                                        itunesCountry = itunesCountry,
                                        itunesConvertToSimplified = itunesConvertToSimplified,
                                        itunesCoverSize = amCoverSize,
                                        qmCoverSize = qmCoverSize,
                                        neCoverSize = neCoverSize
                                    )
                                    if (detail != null) {
                                        if (bestCandidateSimForSource > bestCombinedSim) {
                                            bestCombinedSim = bestCandidateSimForSource
                                            matchedSong = detail
                                            usedSource = source
                                        }
                                        
                                        if (bestCandidateSimForSource >= 0.8f) {
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("BatchMatch", "Search failed for source $source: ${e.message}")
                        }
                    }
                    
                    if (matchedSong != null && usedSource != null) {
                        item.similarityScore = bestCombinedSim
                        
                        if (bestCombinedSim >= 0.8f) {
                            val newData = mutableMapOf<String, String>()
                            
                            config.fields.filter { it.enabled }.forEach { field ->
                                val shouldUpdate = when (field.mode) {
                                    FieldMatchMode.OVERWRITE -> true
                                    FieldMatchMode.SUPPLEMENT -> {
                                        val originalValue = item.originalData[field.key]?.isBlank() != false
                                        originalValue
                                    }
                                }
                                
                                if (shouldUpdate) {
                                    when (field.key) {
                                        "title" -> if (!matchedSong!!.title.isNullOrEmpty()) newData["title"] = matchedSong!!.title ?: ""
                                        "artist" -> if (matchedSong!!.artist.isNotEmpty()) newData["artist"] = matchedSong!!.artist.joinToString(artistSeparator)
                                        "album" -> if (!matchedSong!!.album.isNullOrEmpty()) newData["album"] = matchedSong!!.album ?: ""
                                        "year" -> if (!matchedSong!!.year.isNullOrEmpty()) newData["year"] = matchedSong!!.year ?: ""
                                        "trackNumber" -> if (!matchedSong!!.trackNumber.isNullOrEmpty()) newData["trackNumber"] = matchedSong!!.trackNumber ?: ""
                                        "discNumber" -> if (!matchedSong!!.discNumber.isNullOrEmpty()) newData["discNumber"] = matchedSong!!.discNumber ?: ""
                                        "genre" -> if (!matchedSong!!.genre.isNullOrEmpty()) newData["genre"] = matchedSong!!.genre ?: ""
                                        "albumArtist" -> if (!matchedSong!!.albumArtist.isNullOrEmpty()) newData["albumArtist"] = matchedSong!!.albumArtist ?: ""
                                        "composer" -> if (!matchedSong!!.composer.isNullOrEmpty()) newData["composer"] = matchedSong!!.composer ?: ""
                                        "lyricist" -> if (!matchedSong!!.lyricist.isNullOrEmpty()) newData["lyricist"] = matchedSong!!.lyricist ?: ""
                                        "comment" -> if (!matchedSong!!.comment.isNullOrEmpty()) newData["comment"] = matchedSong!!.comment ?: ""
                                        "copyrightInfo" -> if (!matchedSong!!.copyright.isNullOrEmpty()) newData["copyrightInfo"] = matchedSong!!.copyright ?: ""
                                    }
                                }
                            }
                            
                            item.matchedData = newData
                            item.matchStatus = MatchStatus.SUCCESS
                            item.matchSource = usedSource
                            
                            val coverField = config.fields.find { it.key == "cover" }
                            Log.d("BatchMatch", "封面字段: enabled=${coverField?.enabled}, mode=${coverField?.mode}")
                            if (coverField?.enabled == true) {
                                val shouldUpdateCover = when (coverField.mode) {
                                    FieldMatchMode.OVERWRITE -> true
                                    FieldMatchMode.SUPPLEMENT -> {
                                        Log.d("BatchMatch", "补充模式 - 原始封面: hasOriginalCover=${item.hasOriginalCover}")
                                        !item.hasOriginalCover
                                    }
                                }
                                Log.d("BatchMatch", "是否更新封面: shouldUpdateCover=$shouldUpdateCover")
                                
                                if (shouldUpdateCover) {
                                    try {
                                        val coverUrl = matchedSong!!.coverUrl
                                        Log.d("BatchMatch", "封面 URL: $coverUrl")
                                        if (coverUrl != null && coverUrl.isNotEmpty()) {
                                            val request = Request.Builder().url(coverUrl).build()
                                            client.newCall(request).execute().use { response ->
                                                Log.d("BatchMatch", "封面响应码: ${response.code}")
                                                response.body?.bytes()?.let { bytes ->
                                                    Log.d("BatchMatch", "封面数据大小: ${bytes.size}")
                                                    val tempBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                    if (tempBitmap != null) {
                                                        Log.d("BatchMatch", "封面位图尺寸: ${tempBitmap.width}x${tempBitmap.height}")
                                                        item.coverBitmap = scaleBitmap(tempBitmap)
                                                        if (tempBitmap != item.coverBitmap) {
                                                            tempBitmap.recycle()
                                                        }
                                                        Log.d("BatchMatch", "封面处理完成")
                                                    } else {
                                                        Log.e("BatchMatch", "封面解码失败")
                                                    }
                                                } ?: run {
                                                    Log.e("BatchMatch", "响应体为空")
                                                }
                                            }
                                        } else {
                                            Log.e("BatchMatch", "封面URL为空")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("BatchMatch", "下载封面失败", e)
                                    }
                                }
                            }
                            
                            successCount++
                        } else {
                            item.matchStatus = MatchStatus.FAILED
                            item.error = context.getString(R.string.ml_music_library_similarity_too_low_with_threshold, bestCombinedSim * 100, 80)
                        }
                    } else {
                        item.matchStatus = MatchStatus.FAILED
                        item.error = context.getString(R.string.ml_music_library_no_match_result)
                    }
                } catch (e: Exception) {
                    item.matchStatus = MatchStatus.FAILED
                    item.error = e.message ?: context.getString(R.string.ml_music_library_unknown_error)
                    Log.e("BatchMatch", "Error matching ${item.audioFile.displayTitle}", e)
                } finally {
                    item.originalCoverBitmap = null
                    item.originalCoverData = null
                    
                    semaphore.release()
                    if (!isCancelled()) {
                        progressMutex.withLock {
                            matchedCount++
                        }
                        withContext(Dispatchers.Main) {
                            onProgress(matchedCount, total)
                        }
                    }
                    
                    if (matchedCount % 10 == 0) {
                        System.gc()
                    }
                }
            }
        }.awaitAll()
        
        withContext(Dispatchers.Main) {
            if (!isCancelled()) {
                onComplete(BatchMatchResult(items, matchedCount, successCount, total - successCount))
            }
        }
    }
}

private suspend fun undoBatchMatchField(context: Context, item: BatchMatchItem, fieldKey: String) {
    withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(item.path)
            if (!file.exists()) return@withContext
            
            if (fieldKey == "cover") {
                val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_WRITE)
                if (item.originalCoverData != null) {
                    com.lonx.audiotag.rw.AudioTagWriter.writePictures(pfd, listOf(item.originalCoverData!!))
                } else {
                    com.lonx.audiotag.rw.AudioTagWriter.writePictures(pfd, emptyList())
                }
                pfd.close()
                withContext(Dispatchers.Main) {
                    item.coverBitmap = item.originalCoverBitmap
                }
            } else {
                val updates = mutableMapOf<String, String>()
                val originalValue = item.originalData[fieldKey] ?: ""
                val tagKey = when (fieldKey) {
                    "title" -> "TITLE"
                    "artist" -> "ARTIST"
                    "album" -> "ALBUM"
                    "year" -> "DATE"
                    "trackNumber" -> "TRACKNUMBER"
                    "discNumber" -> "DISCNUMBER"
                    "genre" -> "GENRE"
                    "albumArtist" -> "ALBUMARTIST"
                    "composer" -> "COMPOSER"
                    "lyricist" -> "LYRICIST"
                    "comment" -> "COMMENT"
                    "copyrightInfo" -> "COPYRIGHT"
                    else -> fieldKey.uppercase()
                }
                updates[tagKey] = originalValue
                
                val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_WRITE)
                com.lonx.audiotag.rw.AudioTagWriter.writeTags(pfd, updates, true)
                pfd.close()
            }
            
            withContext(Dispatchers.Main) {
                item.matchedData = item.matchedData - fieldKey
            }
        } catch (e: Exception) {
            Log.e("BatchMatch", "Error undoing field $fieldKey for ${item.path}", e)
        }
    }
}

private suspend fun saveAllBatchMatches(context: Context, items: List<BatchMatchItem>, config: BatchMatchConfig) {
    withContext(Dispatchers.IO) {
        Log.d("BatchMatch", "保存所有匹配结果 - 总项数: ${items.size}")
        // 过滤出匹配成功的项，包含有普通字段或封面字段需要保存的
        val itemsToSave = items.filter { item ->
            item.matchStatus == MatchStatus.SUCCESS && 
            (item.matchedData.isNotEmpty() || 
             (config.fields.find { it.key == "cover" }?.enabled == true && item.coverBitmap != null))
        }
        
        Log.d("BatchMatch", "保存所有匹配结果 - 过滤后项数: ${itemsToSave.size}")
        
        for (item in itemsToSave) {
            try {
                val file = java.io.File(item.path)
                if (!file.exists()) continue
                
                val updates = mutableMapOf<String, String>()
                item.matchedData.forEach { (key, value) ->
                    when (key) {
                        "title" -> if (value.isNotEmpty()) updates["TITLE"] = value
                        "artist" -> if (value.isNotEmpty()) updates["ARTIST"] = value
                        "album" -> if (value.isNotEmpty()) updates["ALBUM"] = value
                        "year" -> if (value.isNotEmpty()) updates["DATE"] = value
                        "trackNumber" -> if (value.isNotEmpty()) updates["TRACKNUMBER"] = value
                        "discNumber" -> if (value.isNotEmpty()) updates["DISCNUMBER"] = value
                        "genre" -> if (value.isNotEmpty()) updates["GENRE"] = value
                        "albumArtist" -> if (value.isNotEmpty()) updates["ALBUMARTIST"] = value
                        "composer" -> if (value.isNotEmpty()) updates["COMPOSER"] = value
                        "lyricist" -> if (value.isNotEmpty()) updates["LYRICIST"] = value
                        "comment" -> if (value.isNotEmpty()) updates["COMMENT"] = value
                        "copyrightInfo" -> if (value.isNotEmpty()) updates["COPYRIGHT"] = value
                    }
                }
                
                if (updates.isNotEmpty()) {
                    val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_WRITE)
                    com.lonx.audiotag.rw.AudioTagWriter.writeTags(pfd, updates, true)
                    pfd.close()
                }
                
                val coverField = config.fields.find { it.key == "cover" }
                Log.d("BatchMatch", "保存时 - 封面字段: enabled=${coverField?.enabled}, item.coverBitmap=${item.coverBitmap != null}")
                if (coverField?.enabled == true && item.coverBitmap != null) {
                    val shouldSaveCover = when (coverField.mode) {
                        FieldMatchMode.OVERWRITE -> true
                        FieldMatchMode.SUPPLEMENT -> {
                            Log.d("BatchMatch", "保存时 - 补充模式 - 原始封面: hasOriginalCover=${item.hasOriginalCover}")
                            !item.hasOriginalCover
                        }
                    }
                    Log.d("BatchMatch", "保存时 - 是否保存封面: shouldSaveCover=$shouldSaveCover")
                    
                    if (shouldSaveCover) {
                        Log.d("BatchMatch", "开始压缩封面，尺寸: ${item.coverBitmap!!.width}x${item.coverBitmap!!.height}")
                        val byteArray = java.io.ByteArrayOutputStream()
                        item.coverBitmap!!.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, byteArray)
                        val picData = byteArray.toByteArray()
                        Log.d("BatchMatch", "封面压缩完成，大小: ${picData.size}")
                        val picPfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_WRITE)
                        com.lonx.audiotag.rw.AudioTagWriter.writePictures(picPfd, listOf(com.lonx.audiotag.model.AudioPicture(data = picData, mimeType = "image/jpeg", pictureType = "Front Cover")))
                        picPfd.close()
                        refreshCoverThemeCachesForAudio(
                            context = context,
                            audioPath = item.path,
                            mediaStoreId = item.audioFile.mediaStoreId,
                            cover = item.coverBitmap
                        )
                        Log.d("BatchMatch", "封面保存完成")
                    }
                }
            } catch (e: Exception) {
                Log.e("BatchMatch", "Error saving ${item.path}", e)
            }
        }
    }
}

private suspend fun refreshAudioFileMetadata(context: Context, path: String, audioFiles: MutableList<AudioFile>): AudioFile? {
    return withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(path)
            if (!file.exists()) return@withContext null
            
            val existing = audioFiles.firstOrNull { it.path == path }
            val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
                context = context,
                filePath = file.absolutePath,
                mediaStoreId = existing?.mediaStoreId ?: -1L
            )
            
            var refreshedCoverCachePath: String? = null
            if (metadata.cover != null) {
                refreshedCoverCachePath = saveCoverToCache(
                    context = context,
                    audioFilePath = file.absolutePath,
                    coverData = metadata.cover,
                    cacheDiscriminator = file.lastModified().toString()
                )
            }
            
            withContext(Dispatchers.Main) {
                val existingIndex = audioFiles.indexOfFirst { it.path == path }
                if (existingIndex >= 0) {
                    val existing = audioFiles[existingIndex]
                    val oldCoverCachePath = existing.coverCachePath
                    val finalCoverCachePath = if (metadata.cover != null) {
                        refreshedCoverCachePath ?: existing.coverCachePath
                    } else {
                        null
                    }

                    oldCoverCachePath?.let { oldPath ->
                        MusicLibraryActivity.removeCoverFromCache(oldPath)
                        if (finalCoverCachePath == null || oldPath != finalCoverCachePath) {
                            try {
                                File(oldPath).delete()
                            } catch (e: Exception) {
                                Log.w("MusicLibrary", "Failed to delete old cover cache: $oldPath", e)
                            }
                        }
                    }

                    if (!finalCoverCachePath.isNullOrBlank() && finalCoverCachePath != oldCoverCachePath) {
                        MusicLibraryActivity.removeCoverFromCache(finalCoverCachePath)
                    }

                    val newAudioFile = existing.copy(
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.album,
                        duration = metadata.duration,
                        fileSize = file.length(),
                        lastModified = file.lastModified(),
                        coverCachePath = finalCoverCachePath
                    )
                    audioFiles[existingIndex] = newAudioFile
                    return@withContext newAudioFile
                }
            }
            null
        } catch (e: Exception) {
            Log.e("MusicLibrary", "Error refreshing file metadata: $path", e)
            null
        }
    }
}

@Composable
private fun MusicLibraryBarSwitch(
    isMultiSelectMode: Boolean,
    multiSelectActionBar: @Composable () -> Unit,
    searchBar: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = isMultiSelectMode,
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
        label = "MusicLibraryBarSwitch",
        modifier = Modifier.fillMaxWidth()
    ) { isMulti ->
        if (isMulti) multiSelectActionBar() else searchBar()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchRenameConfigSheet(
    selectedCount: Int,
    initialConfig: RenameConfig,
    onDismiss: () -> Unit,
    onStartPreview: (RenameConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BatchRenameSettings", Context.MODE_PRIVATE) }
    
    val savedTemplate = remember { prefs.getString("renameTemplate", "") ?: "" }
    val savedSeparator = remember { prefs.getString("artistSeparator", "／") ?: "／" }
    val savedRenameTtml = remember { prefs.getBoolean("renameTtml", true) }
    
    var templateValue by remember { 
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                if (savedTemplate.isNotEmpty()) savedTemplate else ""
            )
        ) 
    }
    var renameTtml by remember { mutableStateOf(savedRenameTtml) }
    var artistSeparator by remember { mutableStateOf(savedSeparator) }
    var showCustomSeparatorSheet by remember { mutableStateOf(false) }
    var customSeparatorInput by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue("")
        )
    }
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    val tags = listOf(
        stringResource(R.string.ml_music_library_field_title) to "歌曲标题",
        stringResource(R.string.ml_music_library_field_artist) to "艺术家",
        stringResource(R.string.ml_music_library_field_album) to "专辑",
        stringResource(R.string.ml_music_library_field_disc_number) to "碟号",
        stringResource(R.string.ml_music_library_field_track_number) to "音轨号",
        stringResource(R.string.ml_music_library_field_year) to "年份",
        stringResource(R.string.ml_music_library_field_album_artist) to "专辑艺术家"
    )

    val presetTemplates = listOf(
        Pair(
            "[${stringResource(R.string.ml_music_library_field_title)}] - [${stringResource(R.string.ml_music_library_field_artist)}]",
            "[歌曲标题] - [艺术家]"
        ),
        Pair(
            "[${stringResource(R.string.ml_music_library_field_artist)}] - [${stringResource(R.string.ml_music_library_field_title)}]",
            "[艺术家] - [歌曲标题]"
        )
    )
    
    val separatorOptions = listOf(
        "／" to "／",
        "&" to "&",
        " " to "[空格]",
        "、" to "、",
        "，" to "，"
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.ml_music_library_batch_rename_config),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_selected_audio_count, selectedCount),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ml_music_library_filename_template),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { 
                        templateValue = androidx.compose.ui.text.input.TextFieldValue("")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ml_music_library_clear),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = templateValue,
                    onValueChange = { templateValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (templateValue.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.ml_music_library_rename_template_hint),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_quick_templates),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val presetScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(presetScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetTemplates.forEach { (displayText, templateValueToken) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            .clickable {
                                templateValue = androidx.compose.ui.text.input.TextFieldValue(
                                    text = templateValueToken,
                                    selection = androidx.compose.ui.text.TextRange(templateValueToken.length)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(displayText, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_quick_tags),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { (display, tag) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable {
                                val newText = templateValue.text + "[$tag]"
                                templateValue = androidx.compose.ui.text.input.TextFieldValue(
                                    text = newText,
                                    selection = androidx.compose.ui.text.TextRange(newText.length)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(display, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_artist_separator),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val separatorScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(separatorScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                separatorOptions.forEach { (sep, display) ->
                    val isSelected = artistSeparator == sep
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                artistSeparator = sep
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = display,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val isCustomSelected = separatorOptions.none { it.first == artistSeparator }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCustomSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            customSeparatorInput = androidx.compose.ui.text.input.TextFieldValue(
                                if (isCustomSelected) artistSeparator else ""
                            )
                            showCustomSeparatorSheet = true
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isCustomSelected) stringResource(R.string.ml_music_library_custom_with_value, artistSeparator) else stringResource(R.string.ml_music_library_custom),
                        fontSize = 14.sp,
                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCustomSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (renameTtml)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { renameTtml = !renameTtml }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ml_music_library_rename_ttml_together),
                    fontSize = 14.sp,
                    fontWeight = if (renameTtml) FontWeight.Bold else FontWeight.Medium,
                    color = if (renameTtml)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val isPreviewEnabled = templateValue.text.isNotEmpty()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = {
                        prefs.edit().apply {
                            putString("renameTemplate", templateValue.text)
                            putString("artistSeparator", artistSeparator)
                            putBoolean("renameTtml", renameTtml)
                            apply()
                        }
                        onStartPreview(RenameConfig(templateValue.text, renameTtml, artistSeparator))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isPreviewEnabled
                ) {
                    Text(stringResource(R.string.ml_music_library_preview))
                }
            }
        }
    }

    if (showCustomSeparatorSheet) {
        CustomArtistSeparatorSheet(
            initialValue = customSeparatorInput,
            onDismiss = { showCustomSeparatorSheet = false },
            onConfirm = { value ->
                artistSeparator = value
                showCustomSeparatorSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomArtistSeparatorSheet(
    initialValue: androidx.compose.ui.text.input.TextFieldValue,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputValue by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = initialValue.text,
                selection = androidx.compose.ui.text.TextRange(initialValue.text.length)
            )
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { sheetState.show() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.ml_music_library_custom_artist_separator),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.ml_music_library_invalid_filename_chars_tip),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 2,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (inputValue.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.ml_music_library_separator_hint),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            if (!errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = {
                        val validateError = validateArtistSeparator(inputValue.text)
                        if (validateError != null) {
                            errorMessage = validateError
                            return@Button
                        }
                        onConfirm(inputValue.text)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchRenamePreviewSheet(
    previewItems: List<RenamePreviewItem>,
    config: RenameConfig,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.ml_music_library_rename_preview),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.ml_music_library_total_files_count, previewItems.size),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(previewItems) { item ->
                    RenamePreviewItemCard(item)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_cancel))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ml_music_library_confirm_rename))
                }
            }
        }
    }
}

@Composable
private fun RenamePreviewItemCard(item: RenamePreviewItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = item.audioFile.displayTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ml_music_library_old_name_prefix),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.oldName,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ml_music_library_new_name_prefix),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.newName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        if (item.oldTtmlName != null && item.newTtmlName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ml_music_library_ttml_old_name_prefix),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.oldTtmlName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ml_music_library_ttml_new_name_prefix),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.newTtmlName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BatchRenameProgressDialog(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.ml_music_library_renaming))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$current / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchRenameResultSheet(
    result: RenameResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    
    LaunchedEffect(Unit) { sheetState.show() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ml_music_library_rename_completed),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.ml_music_library_success_failed, result.successCount, result.failedCount),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (result.items.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(result.items) { item ->
                        BatchRenameResultItemRow(item = item)
                    }
                }
            }
            
            if (result.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ml_music_library_failed_items),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.errors.forEach { (name, error) ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = error,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ml_music_library_close), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun BatchRenameResultItemRow(
    item: RenamePreviewItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.audioFile.displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ml_music_library_old_with_value, item.oldName),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.ml_music_library_new_with_value, item.newName),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = stringResource(R.string.ml_music_library_status_success),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

private suspend fun generateRenamePreview(
    context: Context,
    files: List<AudioFile>,
    config: RenameConfig
): List<RenamePreviewItem> {
    return withContext(Dispatchers.IO) {
        files.map { audioFile ->
            val file = File(audioFile.path)
            val oldName = file.name
            val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(
                context = context,
                filePath = audioFile.path,
                mediaStoreId = audioFile.mediaStoreId
            )
            
            val newNameWithoutExt = replaceTemplateTags(config.template, audioFile, metadata, config.artistSeparator)
            val newName = if (newNameWithoutExt.isNotEmpty()) {
                "$newNameWithoutExt.${file.extension}"
            } else {
                oldName
            }
            
            var oldTtmlName: String? = null
            var newTtmlName: String? = null
            
            if (config.renameTtml) {
                val ttmlFile = File(file.parent, "${file.nameWithoutExtension}.ttml")
                if (ttmlFile.exists()) {
                    oldTtmlName = ttmlFile.name
                    if (newNameWithoutExt.isNotEmpty()) {
                        newTtmlName = "$newNameWithoutExt.ttml"
                    }
                }
            }
            
            RenamePreviewItem(
                audioFile = audioFile,
                oldName = oldName,
                newName = newName,
                oldTtmlName = oldTtmlName,
                newTtmlName = newTtmlName
            )
        }
    }
}

private fun validateArtistSeparator(separator: String): String? {
    if (separator.isEmpty()) {
        return "分隔符不能为空"
    }
    val invalidChars = setOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    if (separator.any { it in invalidChars || it.code < 32 }) {
        return "包含非法字符，不能使用 \\ / : * ? \" < > |"
    }
    return null
}

private fun replaceTemplateTags(
    template: String,
    audioFile: AudioFile,
    metadata: com.example.LyricBox.utils.AudioMetadata,
    artistSeparator: String
): String {
    var result = template
    
    val artistString = processArtists(audioFile.displayArtist, artistSeparator)
    val albumArtistString = processArtists(metadata.albumArtist, artistSeparator)
    
    val tags = mapOf(
        "歌曲标题" to audioFile.displayTitle,
        "艺术家" to artistString,
        "专辑" to audioFile.displayAlbum,
        "碟号" to metadata.discNumber,
        "音轨号" to metadata.trackNumber,
        "年份" to metadata.year,
        "专辑艺术家" to albumArtistString
    )
    
    tags.forEach { (tag, value) ->
        result = result.replace("[$tag]", value)
    }
    
    // 构建无效字符集合；当分隔符包含 "&" 时允许它出现在文件名中
    val invalidChars = mutableSetOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    if (!artistSeparator.contains('&')) {
        invalidChars.add('&')
    }
    return result.filter { it !in invalidChars }.trim()
}

private fun processArtists(artistString: String, separator: String): String {
    if (artistString.isEmpty()) return artistString
    
    var processed = artistString
    
    processed = processed.replace("；", separator)
    processed = processed.replace(";", separator)
    processed = processed.replace("、", separator)
    processed = processed.replace("，", separator)
    processed = processed.replace(", ", separator)
    processed = processed.replace(",", separator)
    processed = processed.replace(" and ", separator)
    processed = processed.replace(" AND ", separator)
    processed = processed.replace("&", separator)
    processed = processed.replace("/", separator)
    processed = processed.replace("／", separator)
    
    return processed
}

private fun renameSingAccompanimentFile(oldAudioFile: File, newAudioFile: File) {
    val oldBaseName = oldAudioFile.nameWithoutExtension
    val newBaseName = newAudioFile.nameWithoutExtension
    if (oldBaseName == newBaseName) return

    val singDir = File("/storage/emulated/0/Music/.sing")
    if (!singDir.exists() || !singDir.isDirectory) return

    singDir.listFiles().orEmpty()
        .filter { it.isFile && it.nameWithoutExtension == oldBaseName }
        .forEach { oldCompanion ->
            val newName = if (oldCompanion.extension.isBlank()) {
                newBaseName
            } else {
                "$newBaseName.${oldCompanion.extension}"
            }
            val newCompanion = File(singDir, newName)
            if (oldCompanion.absolutePath == newCompanion.absolutePath || newCompanion.exists()) return@forEach

            runCatching { oldCompanion.renameTo(newCompanion) }
                .onFailure { error ->
                    Log.w(
                        "BatchRename",
                        "Failed to rename .sing accompaniment: ${oldCompanion.absolutePath} -> ${newCompanion.absolutePath}",
                        error
                    )
                }
        }
}

private suspend fun performBatchRename(
    context: Context,
    previewItems: List<RenamePreviewItem>,
    config: RenameConfig,
    onProgress: (Int, Int) -> Unit,
    onComplete: (RenameResult) -> Unit
) {
    withContext(Dispatchers.IO) {
        val successItems = mutableListOf<RenamePreviewItem>()
        val errors = mutableMapOf<String, String>()
        
        previewItems.forEachIndexed { index, item ->
            val file = File(item.audioFile.path)
            val newFile = File(file.parent, item.newName)
            
            try {
                var audioRenamedOrUnchanged = false
                if (file.exists() && newFile != file) {
                    val renamed = file.renameTo(newFile)
                    if (renamed) {
                        successItems.add(item)
                        audioRenamedOrUnchanged = true
                        renameSingAccompanimentFile(file, newFile)
                    } else {
                        errors[item.oldName] = "重命名失败"
                    }
                } else if (file.exists() && newFile == file) {
                    successItems.add(item)
                    audioRenamedOrUnchanged = true
                }
                
                if (audioRenamedOrUnchanged &&
                    config.renameTtml &&
                    item.oldTtmlName != null &&
                    item.newTtmlName != null
                ) {
                    val ttmlFile = File(file.parent, item.oldTtmlName)
                    val newTtmlFile = File(file.parent, item.newTtmlName)
                    if (ttmlFile.exists() && newTtmlFile != ttmlFile) {
                        ttmlFile.renameTo(newTtmlFile)
                    }
                }
            } catch (e: Exception) {
                Log.e("BatchRename", "Error renaming file: ${file.absolutePath}", e)
                errors[item.oldName] = e.message ?: "未知错误"
            }
            
            withContext(Dispatchers.Main) {
                onProgress(index + 1, previewItems.size)
            }
        }
        
        val result = RenameResult(
            items = successItems,
            successCount = successItems.size,
            failedCount = previewItems.size - successItems.size,
            errors = errors
        )
        
        withContext(Dispatchers.Main) {
            onComplete(result)
        }
    }
}

private fun convertToLrc(
    lyricsData: com.example.LyricBox.lyrics.models.LyricsData, 
    translationData: com.example.LyricBox.lyrics.models.LyricsData,
    lyricType: LyricType,
    includeTranslation: Boolean,
    filterMetadata: Boolean
): String {
    var text = when (lyricType) {
        LyricType.VERBATIM -> VerbatimLrcConverter.toVerbatimLrc(
            lyricsData, 
            if (includeTranslation && translationData.isNotEmpty()) translationData else null
        )
        LyricType.LINE -> convertToLineLrc(lyricsData, translationData, includeTranslation)
    }
    
    if (filterMetadata) {
        val timestampPattern = Regex("""\[\d{1,2}:\d{2}[.:]?\d*\]""")
        val lines = text.lines()
        val startIndex = if (lines.isNotEmpty() && lines[0].contains("-")) 1 else 0
        text = lines.subList(startIndex, lines.size)
            .filter { line ->
                val content = timestampPattern.replace(line, "").trim()
                content.isNotEmpty() && !content.contains(":") && !content.contains("：")
            }
            .joinToString("\n")
    }
    
    return text
}

private fun convertToLineLrc(
    lyricsData: com.example.LyricBox.lyrics.models.LyricsData,
    translationData: com.example.LyricBox.lyrics.models.LyricsData,
    includeTranslation: Boolean
): String {
    val sb = StringBuilder()
    for ((index, line) in lyricsData.withIndex()) {
        if (line.start == null) continue
        if (line.words.isEmpty()) continue
        val lineText = line.words.joinToString("") { word -> word.text }
        if (lineText.trim() == "//") continue
        
        val lineStartTime = formatLrcTime(line.start)
        sb.append("[$lineStartTime]$lineText\n")
        
        if (includeTranslation && translationData.isNotEmpty() && index < translationData.size) {
            val translationLine = translationData[index]
            val translationText = translationLine.words.joinToString("") { word -> word.text }
            if (translationText.trim() != "//") {
                sb.append("[$lineStartTime]$translationText\n")
            }
        }
    }
    return sb.toString()
}

private fun formatLrcTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = ms % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
}

private fun extractPlainTextFromLrc(lrc: String): String {
    return lrc.replace("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]".toRegex(), "")
}

private suspend fun performBatchLyricsMatch(
    context: Context,
    audioFiles: List<AudioFile>,
    config: BatchLyricMatchConfig,
    isCancelled: () -> Boolean,
    onProgress: (Int, Int) -> Unit,
    onComplete: (BatchLyricMatchResult) -> Unit
) {
    withContext(Dispatchers.IO) {
        val lyricsService = LyricsService()
        
        val items = audioFiles.map { audio ->
            BatchLyricMatchItem(
                audioFile = audio,
                originalLyrics = null
            )
        }
        
        val total = items.size
        var matchedCount = 0
        var successCount = 0
        val progressMutex = Mutex()
        
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
        
        fun parseFileName(fileName: String): Pair<String, String> {
            val name = fileName.trim()
            val hyphenIndex = name.indexOf(" - ")
            if (hyphenIndex > 0) {
                val part1 = name.substring(0, hyphenIndex).trim()
                val part2 = name.substring(hyphenIndex + 3).trim()
                return Pair(part1, part2)
            }
            val dashIndex = name.indexOf("-")
            if (dashIndex > 0) {
                val part1 = name.substring(0, dashIndex).trim()
                val part2 = name.substring(dashIndex + 1).trim()
                return Pair(part1, part2)
            }
            return Pair(name, "")
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
        
        val maxConcurrency = config.threadCount.coerceIn(1, 10)
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrency)
        
        items.map { item ->
            async(Dispatchers.IO) {
                if (isCancelled()) return@async
                semaphore.acquire()
                try {
                    if (isCancelled()) return@async
                    item.matchStatus = MatchStatus.MATCHING
                    
                    item.originalLyrics = extractEmbeddedLyrics(
                        context,
                        item.audioFile.path,
                        item.audioFile.mediaStoreId
                    )
                    
                    val fileName = java.io.File(item.audioFile.path).nameWithoutExtension
                    var title: String
                    var artist: String
                    
                    if (item.audioFile.title.isEmpty() || item.audioFile.artist.isEmpty()) {
                        val (parsedTitle, parsedArtist) = parseFileName(fileName)
                        if (parsedArtist.isNotEmpty()) {
                            title = parsedTitle
                            artist = parsedArtist
                        } else {
                            title = fileName
                            artist = ""
                        }
                    } else {
                        title = item.audioFile.title
                        artist = item.audioFile.artist
                    }
                    
                    val keyword = buildString {
                        if (title.isNotEmpty()) append(title)
                        if (artist.isNotEmpty()) {
                            if (isNotEmpty()) append(" ")
                            append(artist)
                        }
                    }.ifEmpty { fileName }
                    
                    if (keyword.isBlank()) {
                        item.matchStatus = MatchStatus.SKIPPED
                        item.error = context.getString(R.string.ml_music_library_no_valid_search_keyword)
                        semaphore.release()
                        if (!isCancelled()) {
                            progressMutex.withLock {
                                matchedCount++
                            }
                            withContext(Dispatchers.Main) {
                                onProgress(matchedCount, total)
                            }
                        }
                        return@async
                    }
                    
                    var bestLyrics: VerbatimLyricsResult? = null
                    var bestCombinedSim = 0f
                    var bestSource: Source? = null
                    
                    for (source in config.sources) {
                        try {
                            val searchResults = lyricsService.searchAllSourcesForLyrics(keyword)[source] ?: emptyList()
                            for (candidate in searchResults) {
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
                                
                                val combinedSim = maxOf(
                                    (titleSim * 0.7f + artistSim * 0.3f),
                                    titleArtistReversedSim
                                )
                                
                                if (combinedSim > bestCombinedSim) {
                                    bestCombinedSim = combinedSim
                                    val lyricsResult = lyricsService.getLyricsFromSource(candidate)
                                    if (lyricsResult.lyrics != null && lyricsResult.lyrics.orig.isNotEmpty()) {
                                        bestLyrics = lyricsResult
                                        bestSource = source
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("BatchLyricsMatch", "Search failed for source $source: ${e.message}")
                        }
                    }
                    
                    if (bestLyrics != null && bestCombinedSim >= 0.9f) {
                        item.similarityScore = bestCombinedSim
                        item.matchStatus = MatchStatus.SUCCESS
                        item.matchSource = bestSource
                        item.lyricType = config.lyricType
                        
                        val originalHasLyrics = !item.originalLyrics.isNullOrEmpty()
                        val shouldUpdate = when (config.mode) {
                            LyricMatchMode.OVERWRITE -> true
                            LyricMatchMode.SUPPLEMENT -> !originalHasLyrics
                        }
                        
                        if (shouldUpdate) {
                            val lyricsData = bestLyrics!!.lyrics?.orig
                            val translationData = bestLyrics!!.lyrics?.ts ?: emptyList()
                            if (lyricsData != null) {
                                // 保存纯文本歌词（用于预览显示）
                                var previewLyrics = lyricsData.joinToString("\n") { line ->
                                    line.words.joinToString("") { word -> word.text }
                                }
                                if (config.includeTranslation && translationData.isNotEmpty()) {
                                    previewLyrics += "\n" + translationData.joinToString("\n") { line ->
                                        line.words.joinToString("") { word -> word.text }
                                    }
                                }
                                if (config.filterMetadata) {
                                    val lines = previewLyrics.lines()
                                    val startIndex = if (lines.isNotEmpty() && lines[0].contains("-")) 1 else 0
                                    previewLyrics = lines.subList(startIndex, lines.size)
                                        .filter { line ->
                                            val content = line.trim()
                                            content.isNotEmpty() && !content.contains(":") && !content.contains("：")
                                        }
                                        .joinToString("\n")
                                }
                                item.matchedLyrics = previewLyrics
                                // 检查是否是逐字歌词（有时间戳）
                                val hasTimestamps = lyricsData.any { line ->
                                    line.words.any { word -> word.start != null }
                                }
                                item.isVerbatimLyrics = hasTimestamps
                                // 保存LRC格式歌词（用于写入文件）
                                item.matchedLyricsLrc = if (hasTimestamps) {
                                    convertToLrc(
                                        lyricsData,
                                        translationData,
                                        config.lyricType,
                                        config.includeTranslation,
                                        config.filterMetadata
                                    )
                                } else {
                                    // 不是逐字歌词，直接使用纯文本
                                    var lyricsText = item.matchedLyrics ?: ""
                                    if (config.includeTranslation && translationData.isNotEmpty()) {
                                        lyricsText += "\n" + translationData.joinToString("\n") { line ->
                                            line.words.joinToString("") { word -> word.text }
                                        }
                                    }
                                    if (config.filterMetadata) {
                                        val lines = lyricsText.lines()
                                        val startIndex = if (lines.isNotEmpty() && lines[0].contains("-")) 1 else 0
                                        lyricsText = lines.subList(startIndex, lines.size)
                                            .filter { line ->
                                                val content = line.trim()
                                                content.isNotEmpty() && !content.contains(":") && !content.contains("：")
                                            }
                                            .joinToString("\n")
                                    }
                                    lyricsText
                                }
                            }
                            successCount++
                        } else {
                            item.matchStatus = MatchStatus.SKIPPED
                            item.error = context.getString(R.string.ml_music_library_existing_lyrics_skipped_in_supplement_mode)
                        }
                    } else {
                        item.matchStatus = MatchStatus.FAILED
                        item.error = if (bestCombinedSim < 0.9f) 
                            context.getString(R.string.ml_music_library_similarity_too_low_with_threshold, bestCombinedSim * 100, 90)
                        else 
                            context.getString(R.string.ml_music_library_no_valid_lyrics_found)
                    }
                } catch (e: Exception) {
                    item.matchStatus = MatchStatus.FAILED
                    item.error = e.message ?: context.getString(R.string.ml_music_library_unknown_error)
                    Log.e("BatchLyricsMatch", "Error matching ${item.audioFile.displayTitle}", e)
                } finally {
                    item.originalLyrics = null
                    
                    semaphore.release()
                    if (!isCancelled()) {
                        progressMutex.withLock {
                            matchedCount++
                        }
                        withContext(Dispatchers.Main) {
                            onProgress(matchedCount, total)
                        }
                    }
                    
                    if (matchedCount % 10 == 0) {
                        System.gc()
                    }
                }
            }
        }.awaitAll()
        
        withContext(Dispatchers.Main) {
            if (!isCancelled()) {
                onComplete(BatchLyricMatchResult(items, matchedCount, successCount, total - successCount))
            }
        }
    }
}

private suspend fun saveBatchLyricsMatches(context: Context, items: List<BatchLyricMatchItem>) {
    withContext(Dispatchers.IO) {
        for (item in items.filter { it.matchStatus == MatchStatus.SUCCESS && it.matchedLyricsLrc != null }) {
            try {
                com.example.LyricBox.utils.AudioMetadataReader.writeLyrics(context, item.path, item.matchedLyricsLrc!!)
            } catch (e: Exception) {
                Log.e("BatchLyricsMatch", "Error saving lyrics for ${item.path}", e)
            }
        }
    }
}

private fun parseLyricsLinesForBatchTargetConversion(lyricsContent: String): List<LyricLine> {
    val trimmed = lyricsContent.trim()
    if (trimmed.isEmpty()) return emptyList()
    return when (detectLyricsFormat(trimmed)) {
        1 -> LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, trimmed).lyricLines
        2 -> LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, trimmed).lyricLines
        3 -> LyricParsingUtils.parseByType(LyricParseType.TTML, trimmed).lyricLines
        else -> buildPlainTextLyricLines(trimmed)
    }
}

private fun removePlaceholderLinesForBatchConversion(lines: List<LyricLine>): List<LyricLine> {
    return lines.mapNotNull { line ->
        val lineText = line.timeUnits.joinToString("") { it.text }.trim()
        if (lineText == "//") {
            null
        } else {
            val cleanedTranslation = if (line.translation.trim() == "//") "" else line.translation
            line.copy(translation = cleanedTranslation)
        }
    }
}

private fun convertLyricsContentToBatchTargetFormat(
    lyricsContent: String,
    targetFormat: BatchLyricsTargetFormat
): String {
    val parsedLines = parseLyricsLinesForBatchTargetConversion(lyricsContent)
    val cleanedLines = removePlaceholderLinesForBatchConversion(parsedLines)
    val creators = if (targetFormat == BatchLyricsTargetFormat.TTML && detectLyricsFormat(lyricsContent) == 3) {
        runCatching { LyricParsingUtils.parseSongwritersFromTtml(lyricsContent) }.getOrDefault(emptyList())
    } else {
        emptyList()
    }
    return LyricSaveEmbedUtils.buildLyricsByFormat(
        format = targetFormat.exportFormat,
        lyricLines = cleanedLines,
        showLineEndTime = false,
        showDuet = true,
        creators = creators
    )
}

private suspend fun resolveLyricsForExternalExport(
    context: Context,
    audioPath: String,
    mediaStoreId: Long = -1L,
    preferSameNameTtml: Boolean
): String? = withContext(Dispatchers.IO) {
    val embeddedLyrics = extractEmbeddedLyrics(context, audioPath, mediaStoreId)?.takeIf { it.isNotBlank() }
    if (!preferSameNameTtml) {
        return@withContext embeddedLyrics
    }
    val ttmlLyrics = resolveExternalTtmlFile(audioPath)?.let { ttmlFile ->
        runCatching { readExternalTtmlWithFallback(context, ttmlFile) }
            .onFailure { Log.e("BatchLyricsExport", "Error reading external TTML: $audioPath", it) }
            .getOrNull()
    }
    ttmlLyrics ?: embeddedLyrics
}

private fun writeLyricsFileToTreeDirectory(
    context: Context,
    directory: DocumentFile,
    fileName: String,
    mimeType: String,
    content: String
): Boolean {
    return try {
        val existing = directory.findFile(fileName)
        if (existing != null && !existing.delete()) {
            return false
        }
        val target = directory.createFile(mimeType, fileName) ?: return false
        context.contentResolver.openOutputStream(target.uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(content)
        } ?: return false
        true
    } catch (e: Exception) {
        Log.e("BatchLyricsExport", "Error writing file to tree: $fileName", e)
        false
    }
}

private suspend fun performBatchLyricsFormatConversion(
    context: Context,
    audioFiles: List<AudioFile>,
    targetFormat: BatchLyricsTargetFormat,
    onProgress: (Int, Int) -> Unit
): BatchLyricsOperationResult = withContext(Dispatchers.IO) {
    val failedMessages = mutableListOf<String>()
    var successCount = 0
    val total = audioFiles.size

    audioFiles.forEachIndexed { index, audio ->
        try {
            val embeddedLyrics = extractEmbeddedLyrics(context, audio.path, audio.mediaStoreId)?.takeIf { it.isNotBlank() }
            if (embeddedLyrics.isNullOrBlank()) {
                failedMessages.add("${audio.displayTitle}: ${context.getString(R.string.ml_music_library_embedded_lyrics_not_read)}")
            } else {
                val convertedLyrics = convertLyricsContentToBatchTargetFormat(embeddedLyrics, targetFormat)
                val result = com.example.LyricBox.utils.AudioMetadataReader.writeLyrics(
                    context,
                    audio.path,
                    convertedLyrics
                )
                if (result.success) {
                    successCount++
                } else {
                    failedMessages.add("${audio.displayTitle}: ${result.errorMessage.ifBlank { context.getString(R.string.ml_music_library_write_failed) }}")
                }
            }
        } catch (e: Exception) {
            Log.e("BatchLyricsConvert", "Error converting lyrics: ${audio.path}", e)
            failedMessages.add("${audio.displayTitle}: ${e.message ?: context.getString(R.string.ml_music_library_unknown_error)}")
        } finally {
            withContext(Dispatchers.Main) {
                onProgress(index + 1, total)
            }
        }
    }

    BatchLyricsOperationResult(
        total = total,
        successCount = successCount,
        failedMessages = failedMessages
    )
}

private suspend fun performBatchLyricsExternalExport(
    context: Context,
    audioFiles: List<AudioFile>,
    config: BatchLyricsExternalExportConfig,
    onProgress: (Int, Int) -> Unit
): BatchLyricsOperationResult = withContext(Dispatchers.IO) {
    val failedMessages = mutableListOf<String>()
    var successCount = 0
    val total = audioFiles.size
    val customDirectory = if (config.useCustomDirectory && config.customDirectoryUri != null) {
        DocumentFile.fromTreeUri(context, config.customDirectoryUri)
    } else {
        null
    }

    audioFiles.forEachIndexed { index, audio ->
        try {
            val sourceLyrics = resolveLyricsForExternalExport(
                context = context,
                audioPath = audio.path,
                mediaStoreId = audio.mediaStoreId,
                preferSameNameTtml = config.preferSameNameTtml
            )
            if (sourceLyrics.isNullOrBlank()) {
                failedMessages.add("${audio.displayTitle}: ${context.getString(R.string.ml_music_library_exportable_lyrics_not_read)}")
                return@forEachIndexed
            }

            val convertedLyrics = convertLyricsContentToBatchTargetFormat(sourceLyrics, config.targetFormat)
            val audioFile = File(audio.path)
            val outputFileName = "${audioFile.nameWithoutExtension}${config.targetFormat.extension}"

            val writeSuccess = if (config.useCustomDirectory) {
                val targetDirectory = customDirectory
                if (targetDirectory == null || !targetDirectory.canWrite()) {
                    false
                } else {
                    writeLyricsFileToTreeDirectory(
                        context = context,
                        directory = targetDirectory,
                        fileName = outputFileName,
                        mimeType = config.targetFormat.mimeType,
                        content = convertedLyrics
                    )
                }
            } else {
                val parentDir = audioFile.parentFile
                if (parentDir == null) {
                    false
                } else {
                    runCatching {
                        File(parentDir, outputFileName).writeText(convertedLyrics, Charsets.UTF_8)
                    }.isSuccess
                }
            }

            if (writeSuccess) {
                successCount++
            } else {
                failedMessages.add("${audio.displayTitle}: ${context.getString(R.string.ml_music_library_external_lyrics_save_failed)}")
            }
        } catch (e: Exception) {
            Log.e("BatchLyricsExport", "Error exporting lyrics: ${audio.path}", e)
            failedMessages.add("${audio.displayTitle}: ${e.message ?: context.getString(R.string.ml_music_library_unknown_error)}")
        } finally {
            withContext(Dispatchers.Main) {
                onProgress(index + 1, total)
            }
        }
    }

    BatchLyricsOperationResult(
        total = total,
        successCount = successCount,
        failedMessages = failedMessages
    )
}
