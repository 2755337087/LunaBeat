package com.example.LyricBox

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
private const val PLAYBACK_STATE_PREFS = "music_playback_state"
private const val KEY_LAST_AUDIO_PATH = "last_audio_path"
private const val KEY_LAST_TITLE = "last_title"
private const val KEY_LAST_ARTIST = "last_artist"
private const val KEY_LAST_COVER_CACHE_PATH = "last_cover_cache_path"

class MusicPlaybackController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val playbackStatePrefs by lazy {
        context.getSharedPreferences(PLAYBACK_STATE_PREFS, Context.MODE_PRIVATE)
    }

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
    var queueAudioPaths by mutableStateOf<List<String>>(emptyList())
        private set

    val hasCurrentItem: Boolean
        get() = currentAudioPath != null

    init {
        restoreSnapshotState()
    }

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
                restoreQueueIfNeeded(readyController)
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
        LocalPlaylistStore.savePlaybackQueue(
            context,
            queue.map {
                LocalPlaylistEntry(
                    path = it.path,
                    title = it.displayTitle,
                    artist = it.displayArtist,
                    durationSeconds = (it.duration / 1000L).coerceAtLeast(-1L)
                )
            }
        )
    }

    fun insertNext(audio: AudioFile) {
        val player = controller ?: return
        val insertIndex = if (player.currentMediaItemIndex >= 0) {
            (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        } else {
            player.mediaItemCount
        }
        val mediaItem = audio.toPlayableMediaItem(
            context = context,
            preferOriginalCover = false
        )
        player.addMediaItem(insertIndex, mediaItem)
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        persistCurrentQueue(player)
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

    fun removeAudioPathAndAdvance(path: String) {
        val targetPath = path.trim()
        if (targetPath.isEmpty()) return

        val player = controller
        if (player != null && player.mediaItemCount > 0) {
            val matchedIndexes = (0 until player.mediaItemCount).filter { idx ->
                val mediaItem = player.getMediaItemAt(idx)
                val itemPath = mediaItem.mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
                    ?: mediaItem.mediaId.takeIf { it.isNotBlank() }
                itemPath == targetPath
            }

            if (matchedIndexes.isNotEmpty()) {
                val currentIdx = player.currentMediaItemIndex
                val currentPath = if (currentIdx in 0 until player.mediaItemCount) {
                    val currentItem = player.getMediaItemAt(currentIdx)
                    currentItem.mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
                        ?: currentItem.mediaId.takeIf { it.isNotBlank() }
                } else {
                    null
                }
                val removingCurrent = currentPath == targetPath
                val wasPlaying = player.isPlaying || player.playWhenReady

                // 删除当前曲目时，优先切到下一首（如果存在）。
                if (removingCurrent && currentIdx >= 0 && currentIdx < player.mediaItemCount - 1) {
                    player.seekTo(currentIdx + 1, 0L)
                }

                matchedIndexes.sortedDescending().forEach { removeIdx ->
                    player.removeMediaItem(removeIdx)
                }

                if (player.mediaItemCount == 0) {
                    player.stop()
                } else {
                    if (player.playbackState == Player.STATE_IDLE) {
                        player.prepare()
                    }
                    player.playWhenReady = wasPlaying
                    if (wasPlaying) {
                        player.play()
                    }
                }

                syncFromPlayer(player)
            }
        }

        queueAudioPaths = queueAudioPaths.filter { it != targetPath }
        removePathFromPersistedQueue(targetPath)

        if (currentAudioPath == targetPath && (controller == null || mediaCount == 0)) {
            currentAudioPath = null
            currentMediaId = null
            currentCoverCachePath = null
            currentArtworkData = null
            currentTitle = ""
            currentArtist = ""
            currentIndex = -1
            mediaCount = 0
            clearSnapshotState()
        }
    }

    fun handleAudioRenamed(oldPath: String, newPath: String) {
        if (oldPath.isBlank() || newPath.isBlank() || oldPath == newPath) return
        val player = controller
        if (player != null && player.mediaItemCount > 0) {
            val currentIdx = player.currentMediaItemIndex
            val resumePosition = player.currentPosition.coerceAtLeast(0L)
            val resumePlayWhenReady = player.playWhenReady
            var replacedAny = false
            var replacedCurrent = false

            for (idx in 0 until player.mediaItemCount) {
                val mediaItem = player.getMediaItemAt(idx)
                val itemPath = mediaItem.mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
                    ?: mediaItem.mediaId.takeIf { it.isNotBlank() }
                if (itemPath == oldPath) {
                    val updatedItem = buildMediaItemForRenamedPath(newPath, preferOriginalCover = idx == currentIdx)
                    if (updatedItem != null) {
                        player.replaceMediaItem(idx, updatedItem)
                        replacedAny = true
                        if (idx == currentIdx) replacedCurrent = true
                    }
                }
            }

            if (replacedAny) {
                if (replacedCurrent && currentIdx >= 0) {
                    player.seekTo(currentIdx, resumePosition)
                }
                player.playWhenReady = resumePlayWhenReady
                syncFromPlayer(player)
                return
            }
        }

        migratePersistedQueuePath(oldPath = oldPath, newPath = newPath)
        if (currentAudioPath == oldPath) {
            currentAudioPath = newPath
            currentMediaId = newPath
            currentTitle = File(newPath).nameWithoutExtension
            persistSnapshotState()
        }
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
        queueAudioPaths = (0 until player.mediaItemCount).mapNotNull { idx ->
            val mediaItem = player.getMediaItemAt(idx)
            mediaItem.mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
                ?: mediaItem.mediaId.takeIf { it.isNotBlank() }
        }
        persistCurrentQueue(player)

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
        if (currentAudioPath.isNullOrBlank()) {
            clearSnapshotState()
        } else {
            persistSnapshotState()
        }
    }

    private fun persistCurrentQueue(player: Player) {
        val entries = (0 until player.mediaItemCount).mapNotNull { idx ->
            val mediaItem = player.getMediaItemAt(idx)
            val path = mediaItem.mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
                ?: mediaItem.mediaId.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            LocalPlaylistEntry(
                path = path,
                title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                durationSeconds = -1L
            )
        }
        LocalPlaylistStore.savePlaybackQueue(context, entries)
    }

    private fun restoreQueueIfNeeded(player: MediaController) {
        if (player.mediaItemCount > 0) return
        val savedPaths = LocalPlaylistStore.loadPlaybackQueuePaths(context)
        if (savedPaths.isEmpty()) return
        val targetPath = playbackStatePrefs.getString(KEY_LAST_AUDIO_PATH, null)
        val targetIndex = savedPaths.indexOfFirst { it == targetPath }.let { if (it >= 0) it else 0 }
        val restoredItems = savedPaths.mapNotNull { path ->
            val file = File(path)
            if (!file.exists()) return@mapNotNull null
            val metadata = runCatching { AudioMetadataReader.readMetadata(context, path) }.getOrNull()
            val inferredCoverCachePath = resolveExistingCoverCachePath(path)
            val audio = AudioFile(
                path = path,
                title = metadata?.title.orEmpty(),
                artist = metadata?.artist.orEmpty(),
                album = metadata?.album.orEmpty(),
                duration = metadata?.duration ?: 0L,
                fileSize = file.length(),
                lastModified = file.lastModified(),
                addedTime = file.lastModified(),
                coverCachePath = inferredCoverCachePath,
                year = metadata?.year.orEmpty(),
                mediaStoreId = -1L
            )
            val shouldPreferOriginalCover = path == savedPaths.getOrNull(targetIndex)
            audio.toPlayableMediaItem(context, preferOriginalCover = shouldPreferOriginalCover)
        }
        if (restoredItems.isEmpty()) return
        val safeTargetIndex = targetIndex.coerceIn(0, restoredItems.lastIndex)
        try {
            player.setMediaItems(restoredItems, safeTargetIndex, 0L)
            player.prepare()
        } catch (e: Exception) {
            Log.e("MusicPlaybackController", "Failed to restore queue", e)
        }
    }

    private fun persistSnapshotState() {
        val path = currentAudioPath
        if (path.isNullOrBlank()) return
        playbackStatePrefs.edit()
            .putString(KEY_LAST_AUDIO_PATH, path)
            .putString(KEY_LAST_TITLE, currentTitle)
            .putString(KEY_LAST_ARTIST, currentArtist)
            .putString(KEY_LAST_COVER_CACHE_PATH, currentCoverCachePath)
            .apply()
    }

    private fun restoreSnapshotState() {
        val savedPath = playbackStatePrefs.getString(KEY_LAST_AUDIO_PATH, null)?.takeIf { it.isNotBlank() } ?: return
        currentAudioPath = savedPath
        currentMediaId = savedPath
        currentTitle = playbackStatePrefs.getString(KEY_LAST_TITLE, null)
            ?.takeIf { it.isNotBlank() }
            ?: File(savedPath).nameWithoutExtension
        currentArtist = playbackStatePrefs.getString(KEY_LAST_ARTIST, null)
            ?.takeIf { it.isNotBlank() }
            ?: "未知艺术家"
        currentCoverCachePath = playbackStatePrefs.getString(KEY_LAST_COVER_CACHE_PATH, null)
            ?.takeIf { !it.isNullOrBlank() && File(it).exists() }
            ?: resolveExistingCoverCachePath(savedPath)
    }

    private fun resolveExistingCoverCachePath(audioPath: String): String? {
        val cacheFile = File(File(context.cacheDir, "covers"), "${audioPath.hashCode()}.jpg")
        return cacheFile.absolutePath.takeIf { cacheFile.exists() }
    }

    private fun buildMediaItemForRenamedPath(path: String, preferOriginalCover: Boolean): MediaItem? {
        val file = File(path)
        if (!file.exists()) return null
        val metadata = runCatching { AudioMetadataReader.readMetadata(context, path) }.getOrNull()
        val audio = AudioFile(
            path = path,
            title = metadata?.title.orEmpty(),
            artist = metadata?.artist.orEmpty(),
            album = metadata?.album.orEmpty(),
            duration = metadata?.duration ?: 0L,
            fileSize = file.length(),
            lastModified = file.lastModified(),
            addedTime = file.lastModified(),
            coverCachePath = resolveExistingCoverCachePath(path),
            year = metadata?.year.orEmpty(),
            mediaStoreId = -1L
        )
        return audio.toPlayableMediaItem(context, preferOriginalCover = preferOriginalCover)
    }

    private fun migratePersistedQueuePath(oldPath: String, newPath: String) {
        val savedPaths = LocalPlaylistStore.loadPlaybackQueuePaths(context)
        if (savedPaths.isEmpty()) return
        var changed = false
        val replacedPaths = savedPaths.map { path ->
            if (path == oldPath) {
                changed = true
                newPath
            } else {
                path
            }
        }
        if (!changed) return

        val entries = replacedPaths.map { path ->
            val file = File(path)
            val metadata = runCatching { AudioMetadataReader.readMetadata(context, path) }.getOrNull()
            LocalPlaylistEntry(
                path = path,
                title = metadata?.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension,
                artist = metadata?.artist.orEmpty(),
                durationSeconds = ((metadata?.duration ?: 0L) / 1000L).coerceAtLeast(-1L)
            )
        }
        LocalPlaylistStore.savePlaybackQueue(context, entries)
    }

    private fun removePathFromPersistedQueue(path: String) {
        val savedPaths = LocalPlaylistStore.loadPlaybackQueuePaths(context)
        if (savedPaths.isEmpty()) return
        var changed = false
        val remainedPaths = savedPaths.filter { saved ->
            val keep = saved != path
            if (!keep) changed = true
            keep
        }
        if (!changed) return
        val entries = remainedPaths.map { remainedPath ->
            val file = File(remainedPath)
            val metadata = runCatching { AudioMetadataReader.readMetadata(context, remainedPath) }.getOrNull()
            LocalPlaylistEntry(
                path = remainedPath,
                title = metadata?.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension,
                artist = metadata?.artist.orEmpty(),
                durationSeconds = ((metadata?.duration ?: 0L) / 1000L).coerceAtLeast(-1L)
            )
        }
        LocalPlaylistStore.savePlaybackQueue(context, entries)
    }

    private fun clearSnapshotState() {
        playbackStatePrefs.edit()
            .remove(KEY_LAST_AUDIO_PATH)
            .remove(KEY_LAST_TITLE)
            .remove(KEY_LAST_ARTIST)
            .remove(KEY_LAST_COVER_CACHE_PATH)
            .apply()
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
