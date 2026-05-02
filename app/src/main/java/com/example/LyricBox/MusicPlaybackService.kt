package com.example.LyricBox

import android.app.PendingIntent
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

    override fun onCreate() {
        super.onCreate()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this).build().apply {
            setSmallIcon(R.mipmap.ic_launcher_foreground)
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
                }

                override fun onPlayerError(error: PlaybackException) {
                    maybeHandleCurrentAlacPlayback(this@apply, reason = "player_error_${error.errorCode}")
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
