package com.example.LyricBox

import android.app.PendingIntent
import android.content.SharedPreferences
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.LyricBox.utils.AudioMetadataReader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executors

class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val transcodeExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var isTranscodingCurrentItem = false
    @Volatile
    private var pendingPlayAfterTranscode = false
    @Volatile
    private var isUpdatingArtwork = false
    private var lastFailedAlacPath: String? = null
    private var lastFailedAlacAtMs: Long = 0L
    private var lastArtworkUpdatedSourcePath: String? = null
    private val lyricExecutor = Executors.newSingleThreadExecutor()
    private val lyricPreviewPrefs by lazy {
        getSharedPreferences(LyricPreviewActivity.PREFS_NAME, MODE_PRIVATE)
    }
    private val lyriconBridge by lazy { LyriconStatusBarBridge(this) }
    private var lyriconEnabled = false
    private var lyriconSongSourcePath: String? = null
    private var lyriconBuildSerial: Long = 0L
    private val lyriconProgressRunnable = object : Runnable {
        override fun run() {
            if (!lyriconEnabled) return
            pushLyriconPlayback(isSeek = false)
            mainHandler.postDelayed(this, 300L)
        }
    }
    private val lyricPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            LyricPreviewActivity.KEY_LYRICON_STATUS_BAR -> syncLyriconEnabledFromPrefs()
            LyricPreviewActivity.KEY_SHOW_TRANSLATION,
            LyricPreviewActivity.KEY_SHOW_TRANSLITERATION -> {
                if (lyriconEnabled) {
                    pushLyriconDisplayFlags()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this).build().apply {
            setSmallIcon(R.drawable.splash_icon)
        }
        setMediaNotificationProvider(notificationProvider)

        val sessionActivityIntent = Intent(this, MusicPlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        player = ExoPlayer.Builder(this).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    maybeEnsureCurrentArtworkMetadata(this@apply)
                    maybeHandleCurrentAlacPlayback(this@apply, reason = "transition_$reason")
                    if (lyriconEnabled) {
                        pushLyriconSongForCurrentItem(forceReloadLyrics = true)
                        pushLyriconPlayback(isSeek = true)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    maybeHandleCurrentAlacPlayback(this@apply, reason = "player_error_${error.errorCode}")
                    if (lyriconEnabled) {
                        pushLyriconPlayback(isSeek = false)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (lyriconEnabled) {
                        pushLyriconPlayback(isSeek = false)
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (lyriconEnabled) {
                        pushLyriconPlayback(isSeek = true)
                    }
                }
            })
        }

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionPendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    val resolved = mediaItems.map { it.ensurePlayable() }.toMutableList()
                    return Futures.immediateFuture(resolved)
                }
            })
            .build()

        lyricPreviewPrefs.registerOnSharedPreferenceChangeListener(lyricPrefsListener)
        syncLyriconEnabledFromPrefs()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(lyriconProgressRunnable)
        lyricPreviewPrefs.unregisterOnSharedPreferenceChangeListener(lyricPrefsListener)
        lyriconBridge.release()
        lyricExecutor.shutdownNow()
        transcodeExecutor.shutdownNow()
        mediaSession?.run {
            player.release()
            release()
        }
        PlaybackAlacTranscodeManager.cleanupCacheFiles(this, keepPath = null)
        player = null
        mediaSession = null
        super.onDestroy()
    }

    private fun syncLyriconEnabledFromPrefs() {
        val enabled = lyricPreviewPrefs.getBoolean(
            LyricPreviewActivity.KEY_LYRICON_STATUS_BAR,
            LyricPreviewActivity.DEFAULT_LYRICON_STATUS_BAR
        )
        if (enabled == lyriconEnabled) return
        lyriconEnabled = enabled

        if (enabled) {
            lyriconBridge.start()
            pushLyriconDisplayFlags()
            pushLyriconSongForCurrentItem(forceReloadLyrics = true)
            pushLyriconPlayback(isSeek = true)
            mainHandler.removeCallbacks(lyriconProgressRunnable)
            mainHandler.post(lyriconProgressRunnable)
        } else {
            mainHandler.removeCallbacks(lyriconProgressRunnable)
            lyriconSongSourcePath = null
            lyriconBridge.stop(clearRemoteState = true)
        }
    }

    private fun pushLyriconDisplayFlags() {
        lyriconBridge.updateDisplayFlags(
            showTranslation = lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_SHOW_TRANSLATION,
                LyricPreviewActivity.DEFAULT_SHOW_TRANSLATION
            ),
            showRoma = lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_SHOW_TRANSLITERATION,
                LyricPreviewActivity.DEFAULT_SHOW_TRANSLITERATION
            )
        )
    }

    private fun pushLyriconPlayback(isSeek: Boolean) {
        val currentPlayer = player ?: return
        val position = currentPlayer.currentPosition.coerceAtLeast(0L)
        lyriconBridge.updatePlayback(
            positionMs = position,
            isPlaying = currentPlayer.isPlaying || currentPlayer.playWhenReady,
            isSeek = isSeek
        )
    }

    private fun pushLyriconSongForCurrentItem(forceReloadLyrics: Boolean) {
        val currentPlayer = player ?: return
        val currentItem = currentPlayer.currentMediaItem
        if (currentItem == null) {
            lyriconSongSourcePath = null
            lyriconBridge.updateSong(null)
            return
        }
        val sourcePath = currentItem.resolveOriginalAudioPath() ?: return
        if (!forceReloadLyrics && lyriconSongSourcePath == sourcePath) return
        lyriconSongSourcePath = sourcePath
        val localSerial = ++lyriconBuildSerial
        val mediaTitle = currentItem.mediaMetadata.title?.toString()
        val mediaArtist = currentItem.mediaMetadata.artist?.toString()
        val fileName = runCatching { File(sourcePath).nameWithoutExtension }.getOrDefault("未知歌曲")
        val title = mediaTitle?.takeIf { it.isNotBlank() } ?: fileName
        val artist = mediaArtist?.takeIf { it.isNotBlank() } ?: "未知艺术家"
        val duration = currentPlayer.duration
            .takeIf { it > 0L && it != C.TIME_UNSET }
            ?: 0L

        lyricExecutor.execute {
            val rawLyrics = loadLyriconRawLyrics(sourcePath)
            val previewLines = buildLyricPreviewLinesFromRawLyrics(rawLyrics, duration)
            val song = buildLyriconSong(
                songId = sourcePath,
                title = title,
                artist = artist,
                duration = duration,
                lyricLines = previewLines
            )
            mainHandler.post {
                if (!lyriconEnabled) return@post
                if (localSerial != lyriconBuildSerial) return@post
                val latestPath = player?.currentMediaItem?.resolveOriginalAudioPath()
                if (latestPath != sourcePath) return@post
                lyriconBridge.updateSong(song)
                pushLyriconDisplayFlags()
                pushLyriconPlayback(isSeek = true)
            }
        }
    }

    private fun loadLyriconRawLyrics(sourcePath: String): String {
        val sidecarTtml = runCatching {
            val audioFile = File(sourcePath)
            val parent = audioFile.parentFile ?: return@runCatching null
            File(parent, "${audioFile.nameWithoutExtension}.ttml")
        }.getOrNull()

        if (sidecarTtml != null && sidecarTtml.exists() && sidecarTtml.isFile) {
            val ttmlText = runCatching { sidecarTtml.readText(Charsets.UTF_8) }
                .getOrDefault("")
                .trim()
            if (ttmlText.isNotEmpty()) {
                Log.d("Lyricon", "lyric source=ttml_sidecar path=${sidecarTtml.absolutePath}")
                return ttmlText
            }
        }

        val embedded = AudioMetadataReader.readLyrics(this, sourcePath).orEmpty().trim()
        if (embedded.isNotEmpty()) {
            Log.d("Lyricon", "lyric source=embedded path=$sourcePath")
        } else {
            Log.d("Lyricon", "lyric source=none path=$sourcePath")
        }
        return embedded
    }

    private fun buildLyricPreviewLinesFromRawLyrics(
        rawLyrics: String,
        durationMs: Long
    ): List<NewPreviewLyricLine> {
        val text = rawLyrics.trim()
        if (text.isEmpty()) return emptyList()
        val format = detectLyricsFormat(text)
        return if (format == 0) {
            buildFallbackPlainLyricLines(text, durationMs)
        } else {
            val parseType = when (format) {
                1 -> LyricParseType.SPL_LRC
                2 -> LyricParseType.ENHANCED_LRC
                3 -> LyricParseType.TTML
                else -> LyricParseType.SPL_LRC
            }
            val parsed = runCatching { LyricParsingUtils.parseByType(parseType, text) }.getOrNull()
            val parsedLines = parsed?.lyricLines.orEmpty()
            if (parsedLines.isEmpty()) {
                buildFallbackPlainLyricLines(text, durationMs)
            } else {
                parsedLines.mapIndexedNotNull { index, line ->
                    val words = line.timeUnits.mapIndexedNotNull { wordIndex, unit ->
                        val wordText = unit.text
                        if (wordText.isEmpty()) return@mapIndexedNotNull null
                        val begin = parseTimestampToMs(unit.startTime).coerceAtLeast(0L)
                        val defaultEnd = begin + 1L
                        val nextBegin = line.timeUnits.getOrNull(wordIndex + 1)?.let {
                            parseTimestampToMs(it.startTime).coerceAtLeast(0L)
                        } ?: defaultEnd
                        val parsedEnd = parseTimestampToMs(unit.endTime).coerceAtLeast(0L)
                        val end = when {
                            parsedEnd > begin -> parsedEnd
                            nextBegin > begin -> nextBegin
                            else -> defaultEnd
                        }
                        NewPreviewLyricWord(
                            text = wordText,
                            begin = begin,
                            end = end,
                            transliteration = unit.transliteration,
                            charTransliterations = unit.charTransliterations
                        )
                    }
                    if (words.isEmpty()) return@mapIndexedNotNull null
                    val lineBegin = words.first().begin
                    val lineEnd = words.last().end.coerceAtLeast(lineBegin + 1L)
                    NewPreviewLyricLine(
                        words = words,
                        begin = lineBegin,
                        end = lineEnd,
                        translation = line.translation,
                        isDuet = line.agentType == LyricAgentType.RIGHT,
                        isBackground = line.agentType == LyricAgentType.BACKGROUND,
                        isInterlude = false,
                        backgroundPlacement = if (line.agentType == LyricAgentType.BACKGROUND) 1 else 0
                    )
                }
            }
        }
    }

    private fun buildFallbackPlainLyricLines(
        plainText: String,
        durationMs: Long
    ): List<NewPreviewLyricLine> {
        val lines = plainText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val spacing = (durationMs / lines.size).coerceAtLeast(1200L)
        return lines.mapIndexed { index, lineText ->
            val begin = index * spacing
            val end = begin + spacing
            NewPreviewLyricLine(
                words = listOf(
                    NewPreviewLyricWord(
                        text = lineText,
                        begin = begin,
                        end = end
                    )
                ),
                begin = begin,
                end = end,
                translation = "",
                isDuet = false,
                isBackground = false,
                isInterlude = false,
                backgroundPlacement = 0
            )
        }
    }

    private fun parseTimestampToMs(raw: String): Long {
        val value = raw.trim()
        if (value.isEmpty()) return 0L
        val parts = value.split(":", ".")
        return when (parts.size) {
            3 -> {
                val minute = parts[0].toLongOrNull() ?: 0L
                val second = parts[1].toLongOrNull() ?: 0L
                val millisPart = parts[2]
                val millis = when (millisPart.length) {
                    1 -> (millisPart.toLongOrNull() ?: 0L) * 100L
                    2 -> (millisPart.toLongOrNull() ?: 0L) * 10L
                    else -> millisPart.take(3).toLongOrNull() ?: 0L
                }
                minute * 60_000L + second * 1_000L + millis
            }
            2 -> {
                val minute = parts[0].toLongOrNull() ?: 0L
                val second = parts[1].toLongOrNull() ?: 0L
                minute * 60_000L + second * 1_000L
            }
            else -> 0L
        }
    }

    private fun maybeHandleCurrentAlacPlayback(player: ExoPlayer, reason: String) {
        val index = player.currentMediaItemIndex
        if (index < 0) return

        val currentItem = player.currentMediaItem ?: return
        val sourcePath = currentItem.resolveOriginalAudioPath() ?: return

        if (!PlaybackAlacTranscodeManager.isAlacEncodedM4a(sourcePath)) {
            lastFailedAlacPath = null
            pendingPlayAfterTranscode = false
            PlaybackAlacTranscodeManager.cleanupCacheFiles(this, keepPath = null)
            return
        }

        val currentUriPath = currentItem.localConfiguration?.uri?.path
        if (PlaybackAlacTranscodeManager.isPlaybackCachePath(this, currentUriPath)) {
            if (!currentUriPath.isNullOrBlank() && !File(currentUriPath).exists()) {
                // 缓存路径已失效，需要重新触发转码而不是直接返回。
                Log.d("MusicPlaybackService", "Playback cache path missing, retranscode source=$sourcePath")
            } else {
                PlaybackAlacTranscodeManager.cleanupCacheFiles(this, keepPath = currentUriPath)
                return
            }
        }

        if (isTranscodingCurrentItem) {
            return
        }
        if (lastFailedAlacPath == sourcePath) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastFailedAlacAtMs < 2500L) {
                return
            }
        }

        val resumePosition = player.currentPosition.coerceAtLeast(0L)
        val requestedPlayAtStart = player.playWhenReady || player.isPlaying
        if (requestedPlayAtStart) {
            pendingPlayAfterTranscode = true
        }
        isTranscodingCurrentItem = true

        transcodeExecutor.execute {
            val transcodedPath = PlaybackAlacTranscodeManager.ensureTranscodedPath(this, sourcePath)
            mainHandler.post {
                isTranscodingCurrentItem = false
                if (transcodedPath.isNullOrBlank()) {
                    lastFailedAlacPath = sourcePath
                    lastFailedAlacAtMs = SystemClock.elapsedRealtime()
                    pendingPlayAfterTranscode = false
                    return@post
                }

                val latestItem = player.currentMediaItem ?: return@post
                val latestSourcePath = latestItem.resolveOriginalAudioPath() ?: return@post
                if (latestSourcePath != sourcePath) {
                    pendingPlayAfterTranscode = false
                    return@post
                }

                lastFailedAlacPath = null
                lastFailedAlacAtMs = 0L
                val updatedItem = latestItem.buildUpon()
                    .setUri(Uri.fromFile(File(transcodedPath)))
                    .build()

                player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
                player.prepare()
                player.seekTo(player.currentMediaItemIndex, resumePosition)
                val shouldPlayNow = pendingPlayAfterTranscode || player.playWhenReady || player.isPlaying
                player.playWhenReady = shouldPlayNow
                if (shouldPlayNow) {
                    player.play()
                }
                pendingPlayAfterTranscode = false
                maybeEnsureCurrentArtworkMetadata(player, force = true)
                PlaybackAlacTranscodeManager.cleanupCacheFiles(this, keepPath = transcodedPath)
            }
        }
    }

    private fun maybeEnsureCurrentArtworkMetadata(
        player: ExoPlayer,
        force: Boolean = false
    ) {
        val currentItem = player.currentMediaItem ?: return
        val sourcePath = currentItem.resolveOriginalAudioPath() ?: return
        val currentArtwork = currentItem.mediaMetadata.artworkData

        if (!force && currentArtwork != null && isHighResolutionCover(currentArtwork)) {
            lastArtworkUpdatedSourcePath = sourcePath
            return
        }

        if (!force && isUpdatingArtwork) return
        if (!force && lastArtworkUpdatedSourcePath == sourcePath && currentArtwork != null) return

        val targetIndex = player.currentMediaItemIndex
        if (targetIndex < 0) return
        val resumePosition = player.currentPosition.coerceAtLeast(0L)
        val shouldPlay = player.playWhenReady
        isUpdatingArtwork = true

        transcodeExecutor.execute {
            val coverData = runCatching {
                AudioMetadataReader.readMetadata(this, sourcePath).cover
            }.getOrNull()

            mainHandler.post {
                isUpdatingArtwork = false
                if (coverData == null || coverData.isEmpty()) return@post

                val latestItem = player.currentMediaItem ?: return@post
                val latestSourcePath = latestItem.resolveOriginalAudioPath() ?: return@post
                if (latestSourcePath != sourcePath) return@post

                val updatedMetadata = latestItem.mediaMetadata.buildUpon()
                    .setArtworkData(coverData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .apply {
                        latestItem.mediaMetadata.extras
                            ?.getString(EXTRA_COVER_CACHE_PATH)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { setArtworkUri(Uri.fromFile(File(it))) }
                    }
                    .build()

                val updatedItem = latestItem.buildUpon()
                    .setMediaMetadata(updatedMetadata)
                    .build()

                val latestIndex = player.currentMediaItemIndex
                if (latestIndex < 0) return@post

                player.replaceMediaItem(latestIndex, updatedItem)
                player.seekTo(latestIndex, resumePosition)
                player.playWhenReady = shouldPlay
                if (shouldPlay) player.play()
                lastArtworkUpdatedSourcePath = sourcePath
            }
        }
    }
}

private fun MediaItem.ensurePlayable(): MediaItem {
    if (localConfiguration != null) return this

    val resolvedUri = mediaId.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { Uri.fromFile(File(path)) }.getOrNull()
        }
        ?: return this

    return buildUpon().setUri(resolvedUri).build()
}

private const val EXTRA_AUDIO_PATH = "audio_path"
private const val EXTRA_COVER_CACHE_PATH = "cover_cache_path"

private fun MediaItem.resolveOriginalAudioPath(): String? {
    val pathFromExtras = mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
    if (!pathFromExtras.isNullOrBlank()) return pathFromExtras
    return mediaId.takeIf { it.isNotBlank() }
}

private fun isHighResolutionCover(bytes: ByteArray): Boolean {
    return runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        maxOf(options.outWidth, options.outHeight) >= 320
    }.getOrDefault(false)
}
