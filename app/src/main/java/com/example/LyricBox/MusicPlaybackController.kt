package com.example.LyricBox

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.LyricBox.utils.AudioMetadataReader
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import java.io.File

private const val EXTRA_AUDIO_PATH = "audio_path"
private const val EXTRA_COVER_CACHE_PATH = "cover_cache_path"

class MusicPlaybackController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    var isReady by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var playWhenReadyRequested by mutableStateOf(false)
        private set
    var playbackState by mutableStateOf(Player.STATE_IDLE)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var currentMediaId by mutableStateOf<String?>(null)
        private set
    var currentAudioPath by mutableStateOf<String?>(null)
        private set
    var currentCoverCachePath by mutableStateOf<String?>(null)
        private set
    var currentArtworkData by mutableStateOf<ByteArray?>(null)
        private set
    var currentTitle by mutableStateOf("")
        private set
    var currentArtist by mutableStateOf("")
        private set
    var currentIndex by mutableStateOf(-1)
        private set
    var mediaCount by mutableStateOf(0)
        private set

    val hasCurrentItem: Boolean
        get() = currentAudioPath != null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
        }
    }

    fun connect() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { readyController ->
                controller = readyController
                readyController.addListener(listener)
                syncFromPlayer(readyController)
                isReady = true
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        isReady = false
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun playQueue(queue: List<AudioFile>, startPath: String) {
        val player = controller ?: return
        if (queue.isEmpty()) return
        val startIndex = queue.indexOfFirst { it.path == startPath }.coerceAtLeast(0)
        val mediaItems = queue.map { audio ->
            audio.toPlayableMediaItem(
                context = context,
                preferOriginalCover = audio.path == startPath
            )
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        player.play()
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        val player = controller ?: return
        if (player.currentPosition > 5_000L) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position.coerceAtLeast(0L))
    }

    fun refreshProgress() {
        val player = controller ?: return
        positionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it > 0L } ?: 0L
    }

    private fun syncFromPlayer(player: Player) {
        isPlaying = player.isPlaying
        playWhenReadyRequested = player.playWhenReady
        playbackState = player.playbackState
        positionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it > 0L } ?: 0L
        currentIndex = player.currentMediaItemIndex
        mediaCount = player.mediaItemCount

        val item = player.currentMediaItem
        currentMediaId = item?.mediaId
        val extras = item?.mediaMetadata?.extras
        currentAudioPath = extras?.getString(EXTRA_AUDIO_PATH)
            ?: item?.mediaId?.takeIf { it.isNotBlank() }
        currentCoverCachePath = extras?.getString(EXTRA_COVER_CACHE_PATH)
        currentArtworkData = item?.mediaMetadata?.artworkData
        currentTitle = item?.mediaMetadata?.title?.toString()
            ?: File(currentAudioPath ?: "").nameWithoutExtension
        currentArtist = item?.mediaMetadata?.artist?.toString() ?: "未知艺术家"
    }
}

private fun AudioFile.toPlayableMediaItem(
    context: Context,
    preferOriginalCover: Boolean
): MediaItem {
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(displayTitle)
        .setArtist(displayArtist)
        .setAlbumTitle(displayAlbum)
        .setExtras(Bundle().apply {
            putString(EXTRA_AUDIO_PATH, path)
            putString(EXTRA_COVER_CACHE_PATH, coverCachePath)
        })

    if (preferOriginalCover) {
        coverCachePath
            ?.takeIf { File(it).exists() }
            ?.let { metadataBuilder.setArtworkUri(Uri.fromFile(File(it))) }
    }

    resolvePlaybackArtworkData(context, preferOriginalCover)?.let { artwork ->
        metadataBuilder.setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
    }

    val metadata = metadataBuilder.build()

    return MediaItem.Builder()
        .setMediaId(path)
        .setUri(Uri.fromFile(File(path)))
        .setMediaMetadata(metadata)
        .build()
}

private fun AudioFile.resolvePlaybackArtworkData(
    context: Context,
    preferOriginalCover: Boolean
): ByteArray? {
    if (!preferOriginalCover) return null

    val cachedBytes = coverCachePath
        ?.takeIf { File(it).exists() }
        ?.let { runCatching { File(it).readBytes() }.getOrNull() }

    if (cachedBytes != null && isHighResolutionCover(cachedBytes)) {
        return cachedBytes
    }

    val originalCover = runCatching {
        AudioMetadataReader.readMetadata(context, path, mediaStoreId).cover
    }.getOrNull()
    return originalCover ?: cachedBytes
}

private fun isHighResolutionCover(bytes: ByteArray): Boolean {
    return runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        maxOf(options.outWidth, options.outHeight) >= 320
    }.getOrDefault(false)
}

@Composable
fun rememberMusicPlaybackController(): MusicPlaybackController {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { MusicPlaybackController(context.applicationContext) }

    DisposableEffect(controller) {
        controller.connect()
        onDispose { controller.release() }
    }

    LaunchedEffect(controller.isReady, controller.isPlaying, controller.playbackState) {
        if (!controller.isReady) return@LaunchedEffect
        while (controller.isReady && (controller.isPlaying || controller.playbackState == Player.STATE_BUFFERING)) {
            delay(500L)
            controller.refreshProgress()
        }
    }

    return controller
}
