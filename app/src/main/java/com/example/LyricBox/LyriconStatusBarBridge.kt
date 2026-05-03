package com.example.LyricBox

import android.content.Context
import android.util.Log
import io.github.proify.lyricon.lyric.model.LyricWord
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import io.github.proify.lyricon.provider.ProviderLogo
import kotlin.math.max

private const val LYRICON_LOG_TAG = "LyriconBridge"

class LyriconStatusBarBridge(context: Context) {
    private val appContext = context.applicationContext
    private var provider: LyriconProvider? = null
    private var started = false

    private var cachedSong: Song? = null
    private var cachedPositionMs: Long = 0L
    private var cachedPlaying: Boolean = false
    private var cachedShowTranslation: Boolean = true
    private var cachedShowRoma: Boolean = true

    fun start() {
        if (started) return
        runCatching {
            val logo = runCatching {
                ProviderLogo.fromDrawable(appContext, R.drawable.lyricon_status_icon).copy(colorful = false)
            }.getOrNull()
            val created = LyriconFactory.createProvider(
                context = appContext,
                providerPackageName = appContext.packageName,
                playerPackageName = appContext.packageName,
                logo = logo
            )
            created.autoSync = true
            created.register()
            provider = created
            started = true
            pushAllState()
        }.onFailure {
            Log.e(LYRICON_LOG_TAG, "start failed", it)
            provider = null
            started = false
        }
    }

    fun stop(clearRemoteState: Boolean) {
        val current = provider
        if (current != null) {
            runCatching {
                if (clearRemoteState) {
                    current.player.setPlaybackState(false)
                    current.player.setSong(null)
                }
            }
            runCatching { current.unregister() }
            runCatching { current.destroy() }
        }
        provider = null
        started = false
    }

    fun release() {
        stop(clearRemoteState = true)
    }

    fun updateSong(song: Song?) {
        cachedSong = song
        provider?.player?.setSong(song)
    }

    fun updateDisplayFlags(showTranslation: Boolean, showRoma: Boolean) {
        cachedShowTranslation = showTranslation
        cachedShowRoma = showRoma
        provider?.player?.setDisplayTranslation(showTranslation)
        provider?.player?.setDisplayRoma(showRoma)
    }

    fun updatePlayback(positionMs: Long, isPlaying: Boolean, isSeek: Boolean) {
        val safePosition = positionMs.coerceAtLeast(0L)
        cachedPositionMs = safePosition
        cachedPlaying = isPlaying
        val player = provider?.player ?: return
        if (isSeek) {
            player.seekTo(safePosition)
        } else {
            player.setPosition(safePosition)
        }
        player.setPlaybackState(isPlaying)
    }

    private fun pushAllState() {
        val player = provider?.player ?: return
        player.setDisplayTranslation(cachedShowTranslation)
        player.setDisplayRoma(cachedShowRoma)
        player.setSong(cachedSong)
        player.setPosition(cachedPositionMs)
        player.setPlaybackState(cachedPlaying)
    }
}

fun buildLyriconSong(
    songId: String,
    title: String,
    artist: String,
    duration: Long,
    lyricLines: List<NewPreviewLyricLine>
): Song {
    val richLines = lyricLines
        .filterNot { it.isInterlude }
        .mapNotNull { line ->
            val text = line.words.joinToString(separator = "") { it.text }.ifBlank { null }
            val parsedWords = line.words.mapNotNull { word ->
                if (word.text.isBlank()) return@mapNotNull null
                val begin = max(0L, word.begin)
                val end = if (word.end > begin) word.end else begin + 1
                LyricWord(
                    begin = begin,
                    end = end,
                    text = word.text
                )
            }.takeIf { it.isNotEmpty() }
            // 单歌词单元按逐行歌词传递，避免 Lyricon 进入逐字模式。
            val words = parsedWords?.takeIf { it.size > 1 }

            val begin = max(0L, line.begin)
            val fallbackEnd = parsedWords?.lastOrNull()?.end ?: (begin + 1)
            val end = if (line.end > begin) line.end else fallbackEnd
            if (text.isNullOrBlank() && line.translation.isBlank()) {
                return@mapNotNull null
            }

            val roma = line.words
                .mapNotNull { it.transliteration.takeIf { trans -> trans.isNotBlank() } }
                .joinToString(" ")
                .ifBlank { null }

            RichLyricLine(
                begin = begin,
                end = end,
                text = text ?: line.translation,
                words = words,
                translation = line.translation.ifBlank { null },
                roma = roma,
                isAlignedRight = line.isDuet
            )
        }
        .sortedBy { it.begin }

    return Song(
        id = songId,
        name = title,
        artist = artist,
        duration = duration.coerceAtLeast(0L),
        lyrics = richLines
    )
}
