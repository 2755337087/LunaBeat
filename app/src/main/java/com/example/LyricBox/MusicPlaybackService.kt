package com.example.LyricBox

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.example.LyricBox.utils.AudioMetadataReader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executors
import android.view.KeyEvent
import androidx.media3.common.util.UnstableApi
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val transcodeExecutor = Executors.newSingleThreadExecutor()
    private val artworkExecutor = Executors.newSingleThreadExecutor()
    private val notificationDsdPlayer = DsdAudioTrackPlayer { state ->
        mainHandler.post {
            syncNotificationDsdState(state)
        }
    }

    @Volatile
    private var isTranscodingCurrentItem = false
    @Volatile
    private var pendingPlayAfterTranscode = false
    @Volatile
    private var isUpdatingArtwork = false
    private var lastFailedAlacPath: String? = null
    private var lastFailedAlacAtMs: Long = 0L
    private var lastTransitionSourcePath: String? = null
    private var lastTransitionIndex: Int = -1
    private var lastTransitionAtMs: Long = 0L
    private var notificationDsdActive = false
    private var notificationDsdPath: String? = null
    private var notificationDsdIndex: Int = -1
    private var notificationDsdPlaying = false
    private var notificationDsdDurationMs = 0L
    private var suppressNotificationDsdTransition = false
    private var lastRestoreStatePersistRealtimeMs = 0L
    private var lastRestoreStatePersistPath: String? = null
    private var lastRestoreStatePersistPositionMs: Long = -1L
    @Volatile
    private var cachedDirectAlacDecodeSupport: Boolean? = null
    private var lastArtworkUpdatedSourcePath: String? = null
    private val lyricExecutor = Executors.newSingleThreadExecutor()
    private val alacWorkExecutor = Executors.newSingleThreadExecutor()
    private val alacCleanupExecutor = Executors.newSingleThreadExecutor()
    private val lyricPreviewPrefs by lazy {
        getSharedPreferences(LyricPreviewActivity.PREFS_NAME, MODE_PRIVATE)
    }
    private val lyriconBridge by lazy { LyriconStatusBarBridge(this) }
    private val flymeStatusBarLyricBridge by lazy { FlymeStatusBarLyricBridge(this) }
    private var lyriconEnabled = false
    private var flymeStatusBarLyricEnabled = false
    private var flymeStatusBarLyricHideNotificationEnabled = LyricPreviewActivity.DEFAULT_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION
    private var lyriconSongSourcePath: String? = null
    private var flymeSongSourcePath: String? = null
    private var lyriconBuildSerial: Long = 0L
    private val lyriconProgressRunnable = object : Runnable {
        override fun run() {
            if (!hasStatusBarLyricEnabled()) return
            pushStatusBarLyricPlayback(isSeek = false)
            mainHandler.postDelayed(this, 300L)
        }
    }
    private val sleepTimerRunnable = object : Runnable {
        override fun run() {
            evaluateSleepTimer()
            mainHandler.postDelayed(this, 1000L)
        }
    }
    private val lyricPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            LyricPreviewActivity.KEY_LYRICON_STATUS_BAR -> syncLyriconEnabledFromPrefs()
            LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC -> syncFlymeStatusBarLyricEnabledFromPrefs()
            LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION -> syncFlymeHideNotificationFromPrefs()
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

        val sessionActivityIntent = Intent(this, MusicLibraryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }
        val dataSourceFactory = DsdSilenceDataSourceFactory(DefaultDataSource.Factory(this))
        val progressiveSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(progressiveSourceFactory)
            .build()
            .apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    persistPlaybackStateForRestore(player, force = false)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    enforceManualNextPriorityOnAutoTransition(this@apply, reason)
                    lastTransitionSourcePath = mediaItem?.resolveOriginalAudioPath()
                    lastTransitionIndex = this@apply.currentMediaItemIndex
                    lastTransitionAtMs = SystemClock.elapsedRealtime()
                    val transitionSourcePath = mediaItem?.resolveOriginalAudioPath()
                    if (transitionSourcePath?.isDsfAudioPath() == true) {
                        if (suppressNotificationDsdTransition ||
                            (notificationDsdActive && notificationDsdPath == transitionSourcePath)
                        ) {
                            return
                        }
                        startNotificationDsdPlayback(
                            player = this@apply,
                            index = this@apply.currentMediaItemIndex,
                            sourcePath = transitionSourcePath,
                            shouldPlay = this@apply.playWhenReady || this@apply.isPlaying
                        )
                        return
                    } else {
                        if (!suppressNotificationDsdTransition) {
                            stopNotificationDsdPlayback()
                        }
                    }
                    maybeHandleCurrentAlacPlayback(this@apply, reason = "transition_$reason")
                    maybeEnsureCurrentArtworkMetadata(this@apply)
                    if (hasStatusBarLyricEnabled()) {
                        pushStatusBarLyricSongForCurrentItem(forceReloadLyrics = true)
                        pushStatusBarLyricPlayback(isSeek = true)
                    }
                    evaluateSleepTimer()
                }

                override fun onPlayerError(error: PlaybackException) {
                    val currentSourcePath = currentMediaItem?.resolveOriginalAudioPath()
                    if (currentSourcePath?.isDsfAudioPath() == true) {
                        startNotificationDsdPlayback(
                            player = this@apply,
                            index = currentMediaItemIndex,
                            sourcePath = currentSourcePath,
                            shouldPlay = playWhenReady || isPlaying
                        )
                        return
                    }
                    val fallbackSourcePath = lastTransitionSourcePath
                    val preferLastTransition = !fallbackSourcePath.isNullOrBlank() &&
                        fallbackSourcePath != currentSourcePath &&
                        (SystemClock.elapsedRealtime() - lastTransitionAtMs) <= 2500L
                    if (preferLastTransition) {
                        maybeHandleAlacPlaybackForPath(
                            player = this@apply,
                            sourcePath = fallbackSourcePath!!,
                            resumeIndexHint = lastTransitionIndex,
                            reason = "player_error_${error.errorCode}"
                        )
                    } else {
                        maybeHandleCurrentAlacPlayback(this@apply, reason = "player_error_${error.errorCode}")
                    }
                    if (hasStatusBarLyricEnabled()) {
                        pushStatusBarLyricPlayback(isSeek = false)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (hasStatusBarLyricEnabled()) {
                        pushStatusBarLyricPlayback(isSeek = false)
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (hasStatusBarLyricEnabled()) {
                        pushStatusBarLyricPlayback(isSeek = true)
                    }
                    persistPlaybackStateForRestore(this@apply, force = true)
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

                @UnstableApi
                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
                    val servicePlayer = this@MusicPlaybackService.player ?: return false
                    val handled = when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> handleNotificationSkipNext(servicePlayer)
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> handleNotificationSkipPrevious(servicePlayer)
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> handleNotificationPlayPause()
                        KeyEvent.KEYCODE_MEDIA_PLAY -> handleNotificationPlay()
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> handleNotificationPause()
                        else -> false
                    }
                    return handled
                }

                @Deprecated("Handled to keep notification skipping consistent with transcoding.")
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    val servicePlayer = this@MusicPlaybackService.player ?: return SessionResult.RESULT_SUCCESS
                    val handled = when (playerCommand) {
                        Player.COMMAND_PLAY_PAUSE -> handleNotificationPlayPause()
                        Player.COMMAND_SEEK_TO_NEXT -> handleNotificationSkipNext(servicePlayer)
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> handleNotificationSkipNext(servicePlayer)
                        Player.COMMAND_SEEK_TO_PREVIOUS -> handleNotificationSkipPrevious(servicePlayer)
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> handleNotificationSkipPrevious(servicePlayer)
                        else -> false
                    }
                    return if (handled) SessionResult.RESULT_ERROR_NOT_SUPPORTED else SessionResult.RESULT_SUCCESS
                }
            })
            .build()

        normalizeFlymeStatusBarLyricPrefs()
        lyricPreviewPrefs.registerOnSharedPreferenceChangeListener(lyricPrefsListener)
        syncLyriconEnabledFromPrefs()
        syncFlymeStatusBarLyricEnabledFromPrefs()
        syncFlymeHideNotificationFromPrefs()
        mainHandler.post(sleepTimerRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        persistPlaybackStateForRestore(player, force = true)
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.player?.let { persistPlaybackStateForRestore(it, force = true) }
        mainHandler.removeCallbacks(lyriconProgressRunnable)
        mainHandler.removeCallbacks(sleepTimerRunnable)
        lyricPreviewPrefs.unregisterOnSharedPreferenceChangeListener(lyricPrefsListener)
        lyriconBridge.release()
        flymeStatusBarLyricBridge.release()
        mediaSession?.run {
            player.release()
            release()
        }
        notificationDsdPlayer.stop()
        player = null
        mediaSession = null
        // 服务即将销毁，不需要再执行异步清理任务，直接同步清理或跳过
        lyricExecutor.shutdownNow()
        alacWorkExecutor.shutdownNow()
        alacCleanupExecutor.shutdownNow()
        transcodeExecutor.shutdownNow()
        artworkExecutor.shutdownNow()
        PlaybackAlacTranscodeManager.releaseStreamingResources()
        super.onDestroy()
    }

    private fun persistPlaybackStateForRestore(player: Player, force: Boolean) {
        val currentItem = player.currentMediaItem ?: return
        val sourcePath = currentItem.resolveOriginalAudioPath()?.takeIf { it.isNotBlank() } ?: return
        val position = if (notificationDsdActive && notificationDsdPath == sourcePath) {
            notificationDsdPlayer.currentPositionMs()
        } else {
            player.currentPosition
        }.coerceAtLeast(0L)
        val duration = when {
            notificationDsdActive && notificationDsdPath == sourcePath -> notificationDsdDurationMs
            player.duration > 0L && player.duration != C.TIME_UNSET -> player.duration
            else -> 0L
        }.coerceAtLeast(0L)
        val now = SystemClock.elapsedRealtime()
        val samePath = lastRestoreStatePersistPath == sourcePath
        val positionChangedEnough = kotlin.math.abs(position - lastRestoreStatePersistPositionMs) >= 1_000L
        if (!force &&
            samePath &&
            !positionChangedEnough &&
            now - lastRestoreStatePersistRealtimeMs < PLAYBACK_STATE_PERSIST_INTERVAL_MS
        ) {
            return
        }

        val metadata = currentItem.mediaMetadata
        getSharedPreferences(PLAYBACK_STATE_PREFS_NAME, MODE_PRIVATE).edit()
            .putString(KEY_LAST_AUDIO_PATH, sourcePath)
            .putString(KEY_LAST_TITLE, metadata.title?.toString().orEmpty())
            .putString(KEY_LAST_ARTIST, metadata.artist?.toString().orEmpty())
            .putString(KEY_LAST_ALBUM, metadata.albumTitle?.toString().orEmpty())
            .putString(KEY_LAST_COVER_CACHE_PATH, metadata.extras?.getString(EXTRA_COVER_CACHE_PATH))
            .putLong(KEY_LAST_POSITION_MS, position)
            .putLong(KEY_LAST_DURATION_MS, duration)
            .apply()
        lastRestoreStatePersistRealtimeMs = now
        lastRestoreStatePersistPath = sourcePath
        lastRestoreStatePersistPositionMs = position
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
            pushStatusBarLyricSongForCurrentItem(forceReloadLyrics = true)
            pushStatusBarLyricPlayback(isSeek = true)
            mainHandler.removeCallbacks(lyriconProgressRunnable)
            mainHandler.post(lyriconProgressRunnable)
        } else {
            mainHandler.removeCallbacks(lyriconProgressRunnable)
            lyriconSongSourcePath = null
            lyriconBridge.stop(clearRemoteState = true)
        }
    }

    private fun normalizeFlymeStatusBarLyricPrefs() {
        val flyme = lyricPreviewPrefs.getBoolean(
            LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC,
            LyricPreviewActivity.DEFAULT_FLYME_STATUS_BAR_LYRIC
        )
        val hideNotification = lyricPreviewPrefs.getBoolean(
            LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION,
            LyricPreviewActivity.DEFAULT_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION
        )
        if (flyme || hideNotification) {
            lyricPreviewPrefs.edit()
                .putBoolean(LyricPreviewActivity.KEY_LYRICON_STATUS_BAR, false)
                .putBoolean(LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC, false)
                .putBoolean(LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION, false)
                .apply()
        }
    }

    private fun syncFlymeStatusBarLyricEnabledFromPrefs() {
        val enabled = lyricPreviewPrefs.getBoolean(
            LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC,
            LyricPreviewActivity.DEFAULT_FLYME_STATUS_BAR_LYRIC
        )
        if (enabled == flymeStatusBarLyricEnabled) return
        flymeStatusBarLyricEnabled = enabled

        if (enabled) {
            flymeStatusBarLyricBridge.start()
            flymeStatusBarLyricBridge.setHideNotification(flymeStatusBarLyricHideNotificationEnabled)
            pushStatusBarLyricSongForCurrentItem(forceReloadLyrics = true)
            pushStatusBarLyricPlayback(isSeek = true)
            mainHandler.removeCallbacks(lyriconProgressRunnable)
            mainHandler.post(lyriconProgressRunnable)
        } else {
            flymeSongSourcePath = null
            flymeStatusBarLyricBridge.stop(clearRemoteState = true)
            if (!lyriconEnabled) {
                mainHandler.removeCallbacks(lyriconProgressRunnable)
            }
        }
    }

    private fun syncFlymeHideNotificationFromPrefs() {
        val enabled = lyricPreviewPrefs.getBoolean(
            LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION,
            LyricPreviewActivity.DEFAULT_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION
        )
        if (enabled == flymeStatusBarLyricHideNotificationEnabled) return
        flymeStatusBarLyricHideNotificationEnabled = enabled
        flymeStatusBarLyricBridge.setHideNotification(enabled)
        if (hasStatusBarLyricEnabled()) {
            pushStatusBarLyricSongForCurrentItem(forceReloadLyrics = true)
            pushStatusBarLyricPlayback(isSeek = true)
        }
    }

    private fun hasStatusBarLyricEnabled(): Boolean {
        return lyriconEnabled || (flymeStatusBarLyricEnabled && flymeStatusBarLyricBridge.isActive())
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

    private fun pushStatusBarLyricPlayback(isSeek: Boolean) {
        if (!hasStatusBarLyricEnabled()) return
        val currentPlayer = player ?: return
        val position = if (notificationDsdActive) {
            notificationDsdPlayer.currentPositionMs().coerceAtLeast(0L)
        } else {
            currentPlayer.currentPosition.coerceAtLeast(0L)
        }
        val isPlaying = if (notificationDsdActive) {
            notificationDsdPlaying
        } else {
            currentPlayer.isPlaying || currentPlayer.playWhenReady
        }
        if (lyriconEnabled) {
            lyriconBridge.updatePlayback(
                positionMs = position,
                isPlaying = isPlaying,
                isSeek = isSeek
            )
        }
        if (flymeStatusBarLyricEnabled && flymeStatusBarLyricBridge.isActive()) {
            flymeStatusBarLyricBridge.updatePlayback(
                positionMs = position,
                isSeek = isSeek
            )
        }
    }

    private fun pushStatusBarLyricSongForCurrentItem(forceReloadLyrics: Boolean) {
        if (!hasStatusBarLyricEnabled()) return
        val currentPlayer = player ?: return
        val currentItem = currentPlayer.currentMediaItem
        if (currentItem == null) {
            lyriconSongSourcePath = null
            flymeSongSourcePath = null
            if (lyriconEnabled) {
                lyriconBridge.updateSong(null)
            }
            if (flymeStatusBarLyricEnabled && flymeStatusBarLyricBridge.isActive()) {
                flymeStatusBarLyricBridge.updateSong(songId = null, lyricLines = emptyList())
            }
            return
        }
        val sourcePath = currentItem.resolveOriginalAudioPath() ?: return
        if (
            !forceReloadLyrics &&
            lyriconSongSourcePath == sourcePath &&
            flymeSongSourcePath == sourcePath
        ) return
        lyriconSongSourcePath = sourcePath
        flymeSongSourcePath = sourcePath
        val localSerial = ++lyriconBuildSerial
        val mediaTitle = currentItem.mediaMetadata.title?.toString()
        val mediaArtist = currentItem.mediaMetadata.artist?.toString()
        val fileName = runCatching { File(sourcePath).nameWithoutExtension }.getOrDefault("未知歌曲")
        val title = mediaTitle?.takeIf { it.isNotBlank() } ?: fileName
        val artist = mediaArtist?.takeIf { it.isNotBlank() } ?: "未知艺术家"
        val duration = if (notificationDsdActive && notificationDsdPath == sourcePath) {
            notificationDsdDurationMs
        } else {
            currentPlayer.duration
                .takeIf { it > 0L && it != C.TIME_UNSET }
                ?: 0L
        }

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
                if (!hasStatusBarLyricEnabled()) return@post
                if (localSerial != lyriconBuildSerial) return@post
                val latestPath = player?.currentMediaItem?.resolveOriginalAudioPath()
                if (latestPath != sourcePath) return@post
                if (lyriconEnabled) {
                    lyriconBridge.updateSong(song)
                    pushLyriconDisplayFlags()
                }
                if (flymeStatusBarLyricEnabled && flymeStatusBarLyricBridge.isActive()) {
                    flymeStatusBarLyricBridge.updateSong(
                        songId = sourcePath,
                        lyricLines = previewLines
                    )
                }
                pushStatusBarLyricPlayback(isSeek = true)
            }
        }
    }

    private fun evaluateSleepTimer() {
        val currentPlayer = player ?: return
        val snapshot = SleepTimerStore.read(this)
        if (!snapshot.isActive) return

        val now = SystemClock.elapsedRealtime()
        val currentPath = currentPlayer.currentMediaItem?.resolveOriginalAudioPath()

        if (!snapshot.waitingForSongEnd && now >= snapshot.endElapsedRealtimeMs) {
            if (snapshot.finishCurrentSong && !currentPath.isNullOrBlank()) {
                SleepTimerStore.markWaitingForSongEnd(this, currentPath)
                return
            }
            currentPlayer.pause()
            currentPlayer.playWhenReady = false
            SleepTimerStore.clear(this)
            return
        }

        if (snapshot.waitingForSongEnd) {
            val anchorPath = snapshot.anchorAudioPath
            val endedOrSwitched = anchorPath.isNullOrBlank() ||
                currentPath.isNullOrBlank() ||
                currentPath != anchorPath ||
                currentPlayer.playbackState == Player.STATE_ENDED
            if (endedOrSwitched) {
                currentPlayer.pause()
                currentPlayer.playWhenReady = false
                SleepTimerStore.clear(this)
            }
        }
    }

    private fun handleNotificationSkipNext(player: ExoPlayer): Boolean {
        val count = player.mediaItemCount
        if (count <= 0) return false
        val currentIndex = if (notificationDsdActive) notificationDsdIndex else player.currentMediaItemIndex
        if (currentIndex < 0) return false
        val manualNextIndex = resolveManualNextIndex(applicationContext, player, consume = true)
        if (manualNextIndex in 0 until count) {
            playQueueItemWithPreTranscode(player, manualNextIndex, forcePlay = true)
            return true
        }
        if (player.repeatMode == Player.REPEAT_MODE_ONE) {
            val targetIndex = if (currentIndex + 1 in 0 until count) currentIndex + 1 else 0
            playQueueItemWithPreTranscode(player, targetIndex, forcePlay = true)
            return true
        }
        val targetIndex = player.nextMediaItemIndex.let { next ->
            if (next in 0 until count) next else 0
        }
        playQueueItemWithPreTranscode(player, targetIndex, forcePlay = true)
        return true
    }

    private fun enforceManualNextPriorityOnAutoTransition(player: ExoPlayer, reason: Int) {
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
        if (player.mediaItemCount <= 0) return
        val manualNextIndex = resolveManualNextIndex(applicationContext, player, consume = true)
        val currentIndex = player.currentMediaItemIndex
        if (manualNextIndex !in 0 until player.mediaItemCount || manualNextIndex == currentIndex) return
        playQueueItemWithPreTranscode(player, manualNextIndex, forcePlay = true)
    }

    private fun handleNotificationSkipPrevious(player: ExoPlayer): Boolean {
        val count = player.mediaItemCount
        if (count <= 0) return false
        val currentIndex = if (notificationDsdActive) notificationDsdIndex else player.currentMediaItemIndex
        if (currentIndex < 0) return false

        val currentPosition = if (notificationDsdActive) {
            notificationDsdPlayer.currentPositionMs()
        } else {
            player.currentPosition
        }
        if (currentPosition > 5_000L) {
            if (notificationDsdActive) {
                notificationDsdPlayer.seekTo(0L)
            } else {
                player.seekTo(currentIndex, 0L)
            }
            return true
        }
        if (player.repeatMode == Player.REPEAT_MODE_ONE) {
            val targetIndex = if (currentIndex - 1 in 0 until count) currentIndex - 1 else (count - 1)
            playQueueItemWithPreTranscode(player, targetIndex, forcePlay = true)
            return true
        }

        val targetIndex = player.previousMediaItemIndex.let { prev ->
            if (prev in 0 until count) prev else (count - 1)
        }
        playQueueItemWithPreTranscode(player, targetIndex, forcePlay = true)
        return true
    }

    private fun handleNotificationPlayPause(): Boolean {
        if (!notificationDsdActive) return false
        return if (notificationDsdPlaying) {
            handleNotificationPause()
        } else {
            handleNotificationPlay()
        }
    }

    private fun handleNotificationPlay(): Boolean {
        if (!notificationDsdActive) return false
        if (notificationDsdPlayer.isActive) {
            notificationDsdPlayer.resume()
        } else {
            val sourcePath = notificationDsdPath ?: return true
            notificationDsdPlayer.play(sourcePath, startPositionMs = 0L, startPaused = false)
        }
        player?.playWhenReady = true
        player?.play()
        notificationDsdPlaying = true
        if (hasStatusBarLyricEnabled()) pushStatusBarLyricPlayback(isSeek = false)
        return true
    }

    private fun handleNotificationPause(): Boolean {
        if (!notificationDsdActive) return false
        notificationDsdPlayer.pause()
        player?.pause()
        notificationDsdPlaying = false
        if (hasStatusBarLyricEnabled()) pushStatusBarLyricPlayback(isSeek = false)
        return true
    }

    private fun startNotificationDsdPlayback(
        player: ExoPlayer,
        index: Int,
        sourcePath: String,
        shouldPlay: Boolean
    ) {
        if (index !in 0 until player.mediaItemCount) return
        val sameTrack = notificationDsdActive && notificationDsdPath == sourcePath
        val sourceItem = player.getMediaItemAt(index)
        val startPositionMs = if (sameTrack) {
            notificationDsdPlayer.currentPositionMs().coerceAtLeast(0L)
        } else {
            0L
        }
        val durationMs = runCatching { notificationDsdPlayer.probeDurationMs(sourcePath) }
            .getOrDefault(notificationDsdDurationMs.takeIf { it > 0L } ?: 1L)
            .coerceAtLeast(1L)
        val silenceUri = buildDsdSilenceUri(durationMs)
        val silenceItem = sourceItem.buildUpon()
            .setUri(silenceUri)
            .build()
        notificationDsdActive = true
        notificationDsdPath = sourcePath
        notificationDsdIndex = index
        notificationDsdPlaying = shouldPlay
        notificationDsdDurationMs = durationMs
        suppressNotificationDsdTransition = true
        try {
            if (sourceItem.localConfiguration?.uri != silenceUri) {
                player.replaceMediaItem(index, silenceItem)
            }
            player.stop()
            player.seekToDefaultPosition(index)
            player.prepare()
            player.seekTo(index, startPositionMs)
            player.playWhenReady = shouldPlay
            if (shouldPlay) {
                player.play()
            }
        } finally {
            suppressNotificationDsdTransition = false
        }
        if (!sameTrack || !notificationDsdPlayer.isActive) {
            notificationDsdPlayer.play(sourcePath, startPositionMs = startPositionMs, startPaused = !shouldPlay)
        } else if (shouldPlay) {
            notificationDsdPlayer.resume()
        } else {
            notificationDsdPlayer.pause()
        }

        maybeEnsureCurrentArtworkMetadata(player, force = true)
        if (hasStatusBarLyricEnabled()) {
            pushStatusBarLyricSongForCurrentItem(forceReloadLyrics = true)
            pushStatusBarLyricPlayback(isSeek = true)
        }
    }

    private fun stopNotificationDsdPlayback() {
        if (!notificationDsdActive && !notificationDsdPlayer.isActive) return
        notificationDsdPlayer.stop()
        notificationDsdActive = false
        notificationDsdPath = null
        notificationDsdIndex = -1
        notificationDsdPlaying = false
        notificationDsdDurationMs = 0L
    }

    private fun syncNotificationDsdState(state: DsdPlaybackState) {
        if (!notificationDsdActive || state.path != notificationDsdPath) return
        notificationDsdPlaying = state.isPlaying
        if (state.durationMs > 0L) {
            notificationDsdDurationMs = state.durationMs
        }
        if (state.errorMessage != null) {
            Log.e("MusicPlaybackService", "Notification DSF playback failed: ${state.errorMessage}")
            notificationDsdPlaying = false
        }
        if (state.hasEnded) {
            notificationDsdPlaying = false
            player?.let { handleNotificationSkipNext(it) }
            return
        }
        if (hasStatusBarLyricEnabled()) {
            pushStatusBarLyricPlayback(isSeek = false)
        }
    }

    private fun playQueueItemWithPreTranscode(
        player: ExoPlayer,
        index: Int,
        forcePlay: Boolean
    ) {
        if (index !in 0 until player.mediaItemCount) return
        val targetItem = player.getMediaItemAt(index)
        val sourcePath = targetItem.resolveOriginalAudioPath()
        val shouldPlay = forcePlay || player.isPlaying || player.playWhenReady
        if (sourcePath?.isDsfAudioPath() == true) {
            startNotificationDsdPlayback(
                player = player,
                index = index,
                sourcePath = sourcePath,
                shouldPlay = shouldPlay
            )
            return
        }
        stopNotificationDsdPlayback()

        if (sourcePath.isNullOrBlank()) {
            player.seekToDefaultPosition(index)
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.playWhenReady = shouldPlay
            if (shouldPlay) player.play()
            return
        }

        transcodeExecutor.execute {
            val playbackPath = resolvePlayablePathForNotification(sourcePath)
            mainHandler.post {
                if (index !in 0 until player.mediaItemCount) return@post
                val latestItem = player.getMediaItemAt(index)
                val latestSourcePath = latestItem.resolveOriginalAudioPath()
                if (latestSourcePath != sourcePath) return@post

                if (playbackPath.isNotBlank() && File(playbackPath).exists()) {
                    val currentUriPath = latestItem.localConfiguration?.uri?.path
                    if (currentUriPath != playbackPath) {
                        val resolvedUri = resolvePlayableAudioUriForService(
                            context = applicationContext,
                            sourcePath = sourcePath,
                            playbackPath = playbackPath
                        )
                        val updatedItem = latestItem.buildUpon()
                            .setUri(resolvedUri)
                            .build()
                        player.replaceMediaItem(index, updatedItem)
                    }
                }

                player.seekToDefaultPosition(index)
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.playWhenReady = shouldPlay
                if (shouldPlay) {
                    player.play()
                }
            }
        }
    }

    private fun resolvePlayablePathForNotification(sourcePath: String): String {
        val normalizedPath = sourcePath.trim()
        if (normalizedPath.isEmpty()) return sourcePath
        val extension = normalizedPath.substringAfterLast('.', "").lowercase()
        if (extension != "m4a") return sourcePath
        if (supportsDirectAlacDecode()) return sourcePath
        val isDetectedAlac = PlaybackAlacTranscodeManager.isAlacEncodedM4a(normalizedPath)
        if (!isDetectedAlac) return sourcePath
        return PlaybackAlacTranscodeManager.ensureTranscodedPath(
            context = applicationContext,
            sourcePath = normalizedPath,
            forceForM4aFailure = true
        ) ?: sourcePath
    }

    private fun supportsDirectAlacDecode(): Boolean {
        cachedDirectAlacDecodeSupport?.let { return it }
        val detected = runCatching {
            FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_ALAC)
        }.getOrDefault(false)
        cachedDirectAlacDecodeSupport = detected
        return detected
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
        alacWorkExecutor.execute {
            mainHandler.post {
                maybeHandleAlacPlaybackForPath(
                    player = player,
                    sourcePath = sourcePath,
                    resumeIndexHint = index,
                    reason = reason
                )
            }
        }
    }

    private fun maybeHandleAlacPlaybackForPath(
        player: ExoPlayer,
        sourcePath: String,
        resumeIndexHint: Int,
        reason: String
    ) {
        val sourceIndex = findMediaItemIndexBySourcePath(player, sourcePath).let { found ->
            if (found >= 0) found else resumeIndexHint
        }
        if (sourceIndex !in 0 until player.mediaItemCount) return
        val sourceItem = player.getMediaItemAt(sourceIndex)

        val sourceFileNameLower = sourcePath.lowercase()
        val sourceIsM4a = sourceFileNameLower.endsWith(".m4a")
        if (!sourceIsM4a) {
            return
        }
        val directAlacDecodeSupported = supportsDirectAlacDecode()
        val forceTranscodeForM4aFailure = sourceIsM4a && reason.startsWith("player_error")
        // 直解码优先：正常切歌下，m4a 直接交给 FFmpeg/系统解码器；仅在解码报错时再回退转码。
        if (sourceIsM4a && directAlacDecodeSupported && !forceTranscodeForM4aFailure) {
            lastFailedAlacPath = null
            pendingPlayAfterTranscode = false
            return
        }
        val isDetectedAlac = if (sourceIsM4a) {
            PlaybackAlacTranscodeManager.isAlacEncodedM4a(sourcePath)
        } else {
            false
        }
        val shouldTranscode = isDetectedAlac || forceTranscodeForM4aFailure

        if (!shouldTranscode) {
            lastFailedAlacPath = null
            pendingPlayAfterTranscode = false
            return
        }

        val currentUriPath = sourceItem.localConfiguration?.uri?.path
        if (PlaybackAlacTranscodeManager.isPlaybackCachePath(this, currentUriPath)) {
            if (!currentUriPath.isNullOrBlank() && !File(currentUriPath).exists()) {
                // 缓存路径已失效，需要重新触发转码而不是直接返回。
                Log.d("MusicPlaybackService", "Playback cache path missing, retranscode source=$sourcePath")
            } else {
                alacCleanupExecutor.execute {
                    PlaybackAlacTranscodeManager.cleanupCacheFiles(this, keepPath = currentUriPath)
                }
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

        val resumePosition = if (sourceIndex == player.currentMediaItemIndex) {
            player.currentPosition.coerceAtLeast(0L)
        } else {
            0L
        }
        val requestedPlayAtStart = player.playWhenReady || player.isPlaying
        if (requestedPlayAtStart) {
            pendingPlayAfterTranscode = true
        }
        // 当切换到需要转码的曲目时，先冻结播放意图，避免解码失败后被播放器自动跳过到下一首。
        if (reason.startsWith("transition_")) {
            player.pause()
            player.playWhenReady = false
        }
        isTranscodingCurrentItem = true

        transcodeExecutor.execute {
            val transcodedPath = PlaybackAlacTranscodeManager.ensureTranscodedPath(
                context = this,
                sourcePath = sourcePath,
                forceForM4aFailure = forceTranscodeForM4aFailure
            )
            mainHandler.post {
                isTranscodingCurrentItem = false
                if (transcodedPath.isNullOrBlank()) {
                    lastFailedAlacPath = sourcePath
                    lastFailedAlacAtMs = SystemClock.elapsedRealtime()
                    val keepPlaying = pendingPlayAfterTranscode
                    pendingPlayAfterTranscode = false
                    if (keepPlaying) {
                        player.playWhenReady = true
                    }
                    return@post
                }

                lastFailedAlacPath = null
                lastFailedAlacAtMs = 0L
                val latestSourceIndex = findMediaItemIndexBySourcePath(player, sourcePath)
                if (latestSourceIndex !in 0 until player.mediaItemCount) {
                    pendingPlayAfterTranscode = false
                    return@post
                }
                val latestItem = player.getMediaItemAt(latestSourceIndex)
                val resolvedUri = resolvePlayableAudioUriForService(
                    context = applicationContext,
                    sourcePath = sourcePath,
                    playbackPath = transcodedPath
                )
                val updatedItem = latestItem.buildUpon()
                    .setUri(resolvedUri)
                    .build()

                player.replaceMediaItem(latestSourceIndex, updatedItem)
                player.prepare()
                val shouldReturnToRecoveredItem = reason.startsWith("player_error")
                if (shouldReturnToRecoveredItem || latestSourceIndex == player.currentMediaItemIndex) {
                    player.seekTo(latestSourceIndex, resumePosition)
                }
                val shouldPlayNow = pendingPlayAfterTranscode || player.playWhenReady || player.isPlaying
                player.playWhenReady = shouldPlayNow
                if (shouldPlayNow) {
                    player.play()
                }
                pendingPlayAfterTranscode = false
                maybeEnsureCurrentArtworkMetadata(player, force = true)
                alacCleanupExecutor.execute {
                    PlaybackAlacTranscodeManager.cleanupCacheFiles(this, keepPath = transcodedPath)
                }
            }
        }
    }

    private fun findMediaItemIndexBySourcePath(player: Player, sourcePath: String): Int {
        for (index in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(index)
            val itemSourcePath = item.resolveOriginalAudioPath() ?: continue
            if (itemSourcePath == sourcePath) {
                return index
            }
        }
        return -1
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

        artworkExecutor.execute {
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

    val sourcePath = resolveOriginalAudioPath()
        ?: mediaId.takeIf { it.isNotBlank() }
        ?: return this
    val resolvedUri = resolvePlayableAudioUriForService(
        context = null,
        sourcePath = sourcePath,
        playbackPath = sourcePath
    )

    return buildUpon().setUri(resolvedUri).build()
}

private const val EXTRA_AUDIO_PATH = "audio_path"
private const val EXTRA_COVER_CACHE_PATH = "cover_cache_path"
private const val PLAYBACK_STATE_PREFS_NAME = "music_playback_state"
private const val KEY_LAST_AUDIO_PATH = "last_audio_path"
private const val KEY_LAST_TITLE = "last_title"
private const val KEY_LAST_ARTIST = "last_artist"
private const val KEY_LAST_ALBUM = "last_album"
private const val KEY_LAST_COVER_CACHE_PATH = "last_cover_cache_path"
private const val KEY_LAST_POSITION_MS = "last_position_ms"
private const val KEY_LAST_DURATION_MS = "last_duration_ms"
private const val PLAYBACK_STATE_PERSIST_INTERVAL_MS = 2_000L
private const val DSD_SILENCE_SCHEME = "dsd-silence"
private const val DSD_SILENCE_SAMPLE_RATE = 44_100
private const val DSD_SILENCE_CHANNEL_COUNT = 2
private const val DSD_SILENCE_BITS_PER_SAMPLE = 16

private class DsdSilenceDataSourceFactory(
    private val upstreamFactory: DataSource.Factory
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return DsdSilenceDataSource(upstreamFactory.createDataSource())
    }
}

private class DsdSilenceDataSource(
    private val upstream: DataSource
) : BaseDataSource(false) {
    private var silenceUri: Uri? = null
    private var silenceHeader: ByteArray = ByteArray(0)
    private var silenceLength: Long = 0L
    private var readPosition: Long = 0L
    private var openedSilence = false

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        if (uri.scheme != DSD_SILENCE_SCHEME) {
            openedSilence = false
            return upstream.open(dataSpec)
        }

        val durationUs = uri.getQueryParameter("durationUs")?.toLongOrNull()?.coerceAtLeast(1L)
            ?: 1_000_000L
        val frameSize = DSD_SILENCE_CHANNEL_COUNT * (DSD_SILENCE_BITS_PER_SAMPLE / 8)
        val frameCount = ((durationUs * DSD_SILENCE_SAMPLE_RATE) / 1_000_000L).coerceAtLeast(1L)
        val dataSize = (frameCount * frameSize).coerceAtMost(0xFFFF_F000L)
        silenceHeader = buildSilentWavHeader(dataSize)
        silenceLength = silenceHeader.size + dataSize
        if (dataSpec.position > silenceLength) {
            throw IOException("Position out of range: ${dataSpec.position} > $silenceLength")
        }
        readPosition = dataSpec.position
        silenceUri = uri
        openedSilence = true
        transferInitializing(dataSpec)
        transferStarted(dataSpec)
        val available = silenceLength - readPosition
        return if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            available
        } else {
            minOf(available, dataSpec.length)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!openedSilence) {
            return upstream.read(buffer, offset, length)
        }
        if (length == 0) return 0
        val remaining = silenceLength - readPosition
        if (remaining <= 0L) return C.RESULT_END_OF_INPUT
        val bytesToRead = minOf(length.toLong(), remaining).toInt()
        var written = 0
        val headerSize = silenceHeader.size.toLong()
        if (readPosition < headerSize) {
            val headerBytes = minOf(bytesToRead.toLong(), headerSize - readPosition).toInt()
            System.arraycopy(silenceHeader, readPosition.toInt(), buffer, offset, headerBytes)
            readPosition += headerBytes
            written += headerBytes
        }
        val zeroBytes = bytesToRead - written
        if (zeroBytes > 0) {
            buffer.fill(0, offset + written, offset + written + zeroBytes)
            readPosition += zeroBytes
            written += zeroBytes
        }
        bytesTransferred(written)
        return written
    }

    override fun getUri(): Uri? {
        return if (openedSilence) silenceUri else upstream.uri
    }

    override fun close() {
        if (openedSilence) {
            openedSilence = false
            silenceUri = null
            silenceHeader = ByteArray(0)
            silenceLength = 0L
            readPosition = 0L
            transferEnded()
        } else {
            upstream.close()
        }
    }
}

private fun buildDsdSilenceUri(durationMs: Long): Uri {
    val durationUs = durationMs.coerceAtLeast(1L) * 1_000L
    return Uri.Builder()
        .scheme(DSD_SILENCE_SCHEME)
        .authority("audio")
        .appendQueryParameter("durationUs", durationUs.toString())
        .build()
}

private fun buildSilentWavHeader(dataSize: Long): ByteArray {
    val byteRate = DSD_SILENCE_SAMPLE_RATE * DSD_SILENCE_CHANNEL_COUNT * (DSD_SILENCE_BITS_PER_SAMPLE / 8)
    val blockAlign = DSD_SILENCE_CHANNEL_COUNT * (DSD_SILENCE_BITS_PER_SAMPLE / 8)
    val riffSize = (36L + dataSize).coerceAtMost(0xFFFF_FFFFL)
    return ByteBuffer.allocate(44)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put("RIFF".toByteArray(Charsets.US_ASCII))
        .putInt(riffSize.toInt())
        .put("WAVE".toByteArray(Charsets.US_ASCII))
        .put("fmt ".toByteArray(Charsets.US_ASCII))
        .putInt(16)
        .putShort(1)
        .putShort(DSD_SILENCE_CHANNEL_COUNT.toShort())
        .putInt(DSD_SILENCE_SAMPLE_RATE)
        .putInt(byteRate)
        .putShort(blockAlign.toShort())
        .putShort(DSD_SILENCE_BITS_PER_SAMPLE.toShort())
        .put("data".toByteArray(Charsets.US_ASCII))
        .putInt(dataSize.coerceAtMost(0xFFFF_FFFFL).toInt())
        .array()
}

private fun MediaItem.resolveOriginalAudioPath(): String? {
    val pathFromExtras = mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
    if (!pathFromExtras.isNullOrBlank()) return pathFromExtras
    val mediaIdPath = mediaId.takeIf { it.isNotBlank() }
    if (!mediaIdPath.isNullOrBlank()) return mediaIdPath
    return localConfiguration?.uri?.path?.takeIf { it.isNotBlank() }
}

private fun String.isDsfAudioPath(): Boolean {
    return substringAfterLast('.', "").lowercase() == "dsf"
}

private fun resolvePlayableAudioUriForService(
    context: Context?,
    sourcePath: String,
    playbackPath: String
): Uri {
    val playbackFile = File(playbackPath)
    if (playbackPath != sourcePath && playbackFile.exists()) {
        return Uri.fromFile(playbackFile)
    }
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && context != null) {
        val mediaUri = resolveMediaStoreAudioUriForService(context, sourcePath)
        if (mediaUri != null) return mediaUri
    }
    if (playbackFile.exists()) {
        return Uri.fromFile(playbackFile)
    }
    return Uri.fromFile(File(sourcePath))
}

private fun resolveMediaStoreAudioUriForService(
    context: Context,
    sourcePath: String
): Uri? {
    return try {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val args = arrayOf(sourcePath)
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

private fun isHighResolutionCover(bytes: ByteArray): Boolean {
    return runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        maxOf(options.outWidth, options.outHeight) >= 320
    }.getOrDefault(false)
}
