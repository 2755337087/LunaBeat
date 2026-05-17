package com.example.LyricBox

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
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
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.LyricBox.utils.AudioMetadataReader
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

private const val EXTRA_AUDIO_PATH = "audio_path"
private const val EXTRA_COVER_CACHE_PATH = "cover_cache_path"
private const val EXTRA_MEDIA_STORE_ID = "media_store_id"
private const val PLAYBACK_STATE_PREFS = "music_playback_state"
private const val KEY_LAST_AUDIO_PATH = "last_audio_path"
private const val KEY_LAST_TITLE = "last_title"
private const val KEY_LAST_ARTIST = "last_artist"
private const val KEY_LAST_ALBUM = "last_album"
private const val KEY_LAST_COVER_CACHE_PATH = "last_cover_cache_path"
private const val KEY_PLAYBACK_MODE = "playback_mode"
@Volatile
private var cachedDirectAlacDecodeSupport: Boolean? = null

enum class PlaybackMode {
    SEQUENTIAL,
    SHUFFLE,
    SINGLE_REPEAT
}

class MusicPlaybackController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val transcodeQueueExecutor = Executors.newSingleThreadExecutor()
    private val queuePersistExecutor = Executors.newSingleThreadExecutor()
    private val playbackStatePrefs by lazy {
        context.getSharedPreferences(PLAYBACK_STATE_PREFS, Context.MODE_PRIVATE)
    }
    @Volatile
    private var pendingQueueEntries: List<LocalPlaylistEntry>? = null
    @Volatile
    private var queuePersistScheduled: Boolean = false

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
    var currentMediaStoreId by mutableStateOf(-1L)
        private set
    var currentPlaybackUriPath by mutableStateOf<String?>(null)
        private set
    var currentCoverCachePath by mutableStateOf<String?>(null)
        private set
    var currentArtworkData by mutableStateOf<ByteArray?>(null)
        private set
    var currentTitle by mutableStateOf("")
        private set
    var currentArtist by mutableStateOf("")
        private set
    var currentAlbum by mutableStateOf("")
        private set
    var currentIndex by mutableStateOf(-1)
        private set
    var mediaCount by mutableStateOf(0)
        private set
    var queueAudioPaths by mutableStateOf<List<String>>(emptyList())
        private set
    var playbackMode by mutableStateOf(PlaybackMode.SEQUENTIAL)
        private set
    var hasNextTrack by mutableStateOf(false)
        private set
    var nextTrackAudioPath by mutableStateOf<String?>(null)
        private set
    var nextTrackTitle by mutableStateOf("")
        private set
    private var temporaryCompanionMediaIndex: Int = -1
    private var temporaryCompanionSourcePath: String? = null
    private var lastKnownDurationMs: Long = 0L

    val hasCurrentItem: Boolean
        get() = currentAudioPath != null

    init {
        restoreSnapshotState()
        restorePlaybackModeState()
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
                applyPlaybackModeToPlayer(readyController, playbackMode)
                syncFromPlayer(readyController)
                isReady = true
                restoreQueueIfNeededAsync(readyController)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        transcodeQueueExecutor.shutdownNow()
        queuePersistExecutor.shutdownNow()
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
        val queueSnapshot = queue.distinctBy { it.path }
        val startIndex = queueSnapshot.indexOfFirst { it.path == startPath }.coerceAtLeast(0)
        transcodeQueueExecutor.execute {
            val playablePathBySource = queueSnapshot.associate { audio ->
                audio.path to resolvePlayablePathForPlayback(context, audio.path)
            }
            val mediaItems = queueSnapshot.map { audio ->
                audio.toPlayableMediaItem(
                    context = context,
                    preferOriginalCover = audio.path == startPath,
                    playbackPathOverride = playablePathBySource[audio.path]
                )
            }
            ContextCompat.getMainExecutor(context).execute {
                val latestPlayer = controller ?: return@execute
                latestPlayer.setMediaItems(mediaItems, startIndex, 0L)
                latestPlayer.prepare()
                latestPlayer.playWhenReady = true
                latestPlayer.play()
            }
        }
        persistQueueEntriesAsync(
            queueSnapshot.map {
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
        val manualNextToken = UUID.randomUUID().toString()
        transcodeQueueExecutor.execute {
            val playbackPath = resolvePlayablePathForPlayback(context, audio.path)
            val mediaItem = audio.toPlayableMediaItem(
                context = context,
                preferOriginalCover = false,
                playbackPathOverride = playbackPath,
                manualNextToken = manualNextToken
            )
            ContextCompat.getMainExecutor(context).execute {
                val latestPlayer = controller ?: return@execute
                val removalIndexes = (0 until latestPlayer.mediaItemCount).filter { idx ->
                    latestPlayer.getMediaItemAt(idx).resolveSourcePath() == audio.path
                }
                var safeInsertIndex = insertIndex.coerceIn(0, latestPlayer.mediaItemCount)
                removalIndexes.sortedDescending().forEach { removeIndex ->
                    if (removeIndex == latestPlayer.currentMediaItemIndex) {
                        return@execute
                    }
                    if (removeIndex < safeInsertIndex) {
                        safeInsertIndex--
                    }
                    latestPlayer.removeMediaItem(removeIndex)
                }
                latestPlayer.addMediaItem(safeInsertIndex, mediaItem)
                ManualNextPriorityStore.enqueue(context, manualNextToken)
                if (latestPlayer.playbackState == Player.STATE_IDLE) {
                    latestPlayer.prepare()
                }
                persistCurrentQueue(latestPlayer)
            }
        }
    }

    fun skipToNext() {
        val player = controller ?: return
        val current = player.currentMediaItemIndex
        if (current < 0) return
        val count = player.mediaItemCount
        if (count <= 0) return
        val manualNextIndex = resolveManualNextIndex(context, player, consume = true)
        if (manualNextIndex in 0 until count) {
            playQueueItemWithPreTranscode(manualNextIndex, forcePlay = true)
            return
        }
        if (playbackMode == PlaybackMode.SINGLE_REPEAT) {
            val nextIndex = if (current + 1 in 0 until count) current + 1 else 0
            playQueueItemWithPreTranscode(nextIndex, forcePlay = true)
            return
        }
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex in 0 until count) {
            playQueueItemWithPreTranscode(nextIndex, forcePlay = true)
        } else {
            playQueueItemWithPreTranscode(0, forcePlay = true)
        }
    }

    fun skipToPrevious() {
        val player = controller ?: return
        val current = player.currentMediaItemIndex
        if (playbackMode == PlaybackMode.SINGLE_REPEAT && current > 0 && player.currentPosition <= 5_000L) {
            playQueueItemWithPreTranscode(current - 1, forcePlay = true)
            return
        }
        if (player.currentPosition > 5_000L) {
            player.seekTo(0L)
        } else {
            val previousIndex = player.previousMediaItemIndex
            if (previousIndex in 0 until player.mediaItemCount) {
                playQueueItemWithPreTranscode(previousIndex, forcePlay = true)
            } else if (player.mediaItemCount > 0) {
                playQueueItemWithPreTranscode(player.mediaItemCount - 1, forcePlay = true)
            }
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position.coerceAtLeast(0L))
    }

    suspend fun switchCurrentAudioKeepingMetadata(
        expectedSourcePath: String,
        targetAudioPath: String,
        crossfadeDurationMs: Long = 360L
    ): Boolean {
        val player = controller ?: return false
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex !in 0 until player.mediaItemCount) return false

        val currentItem = player.getMediaItemAt(currentIndex)
        val currentSourcePath = currentItem.resolveSourcePath()
        if (currentSourcePath.isNullOrBlank() || currentSourcePath != expectedSourcePath) {
            return false
        }

        val targetFile = File(targetAudioPath)
        if (!targetFile.exists() || !targetFile.isFile) return false

        val targetPlaybackPath = resolvePlayablePathForPlayback(context, targetFile.absolutePath)
        val resolvedTargetFile = File(targetPlaybackPath)
        if (!resolvedTargetFile.exists() || !resolvedTargetFile.isFile) return false
        val targetPlayableUri = resolvePlayableAudioUri(
            context = context,
            sourcePath = targetAudioPath,
            playbackPath = resolvedTargetFile.absolutePath
        )

        val currentUriPath = currentItem.localConfiguration?.uri?.path
        if (currentItem.localConfiguration?.uri == targetPlayableUri || currentUriPath == resolvedTargetFile.absolutePath) return true

        val wasPlaying = player.isPlaying || player.playWhenReady
        val resumePosition = player.currentPosition.coerceAtLeast(0L)
        val baselineVolume = player.volume.takeIf { it > 0f } ?: 1f
        val halfDuration = (crossfadeDurationMs / 2L).coerceAtLeast(90L)

        animatePlayerVolume(player, baselineVolume, 0f, halfDuration)

        val updatedItem = currentItem.buildUpon()
            .setUri(targetPlayableUri)
            .build()
        player.replaceMediaItem(currentIndex, updatedItem)
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.seekTo(currentIndex, resumePosition)
        player.playWhenReady = wasPlaying
        if (wasPlaying) {
            player.play()
        }

        player.volume = 0f
        animatePlayerVolume(player, 0f, baselineVolume, halfDuration)
        val enablingCompanion = File(targetAudioPath).absolutePath != File(expectedSourcePath).absolutePath
        if (enablingCompanion) {
            temporaryCompanionMediaIndex = currentIndex
            temporaryCompanionSourcePath = expectedSourcePath
        } else {
            temporaryCompanionMediaIndex = -1
            temporaryCompanionSourcePath = null
        }
        syncFromPlayer(player)
        return true
    }

    fun playAtQueueIndex(index: Int) {
        playQueueItemWithPreTranscode(index, forcePlay = true)
    }

    private fun playQueueItemWithPreTranscode(index: Int, forcePlay: Boolean) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return

        val sourceItem = player.getMediaItemAt(index)
        val sourcePath = sourceItem.resolveSourcePath()
        val shouldPlay = forcePlay || player.isPlaying || player.playWhenReady

        if (sourcePath.isNullOrBlank()) {
            player.seekToDefaultPosition(index)
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.playWhenReady = shouldPlay
            if (shouldPlay) {
                player.play()
            }
            return
        }

        transcodeQueueExecutor.execute {
            val playbackPath = resolvePlayablePathForPlayback(context, sourcePath)
            ContextCompat.getMainExecutor(context).execute {
                val latestPlayer = controller ?: return@execute
                if (index !in 0 until latestPlayer.mediaItemCount) return@execute
                val latestItem = latestPlayer.getMediaItemAt(index)
                if (latestItem.resolveSourcePath() != sourcePath) return@execute

                if (playbackPath.isNotBlank() && File(playbackPath).exists()) {
                    val mediaStoreId = latestItem.mediaMetadata.extras?.getLong(EXTRA_MEDIA_STORE_ID, -1L) ?: -1L
                    val updatedUri = resolvePlayableAudioUri(
                        context = context,
                        sourcePath = sourcePath,
                        playbackPath = playbackPath,
                        mediaStoreId = mediaStoreId
                    )
                    val currentUriPath = latestItem.localConfiguration?.uri?.path
                    if (latestItem.localConfiguration?.uri != updatedUri && currentUriPath != playbackPath) {
                        val updatedItem = latestItem.buildUpon()
                            .setUri(updatedUri)
                            .build()
                        latestPlayer.replaceMediaItem(index, updatedItem)
                    }
                }

                latestPlayer.seekToDefaultPosition(index)
                if (latestPlayer.playbackState == Player.STATE_IDLE) {
                    latestPlayer.prepare()
                }
                latestPlayer.playWhenReady = shouldPlay
                if (shouldPlay) {
                    latestPlayer.play()
                }
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex !in 0 until player.mediaItemCount || toIndex !in 0 until player.mediaItemCount) return
        if (fromIndex == toIndex) return
        player.moveMediaItem(fromIndex, toIndex)
        persistCurrentQueue(player)
        syncFromPlayer(player)
    }

    fun removeQueueItemByPath(path: String) {
        val targetPath = path.trim()
        if (targetPath.isEmpty()) return
        val player = controller ?: return
        val index = (0 until player.mediaItemCount).firstOrNull { idx ->
            val mediaItem = player.getMediaItemAt(idx)
            val itemPath = mediaItem.mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
                ?: mediaItem.mediaId.takeIf { it.isNotBlank() }
            itemPath == targetPath
        } ?: return
        removeQueueItemAt(index)
    }

    fun removeQueueItemAt(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        val wasPlaying = player.isPlaying || player.playWhenReady
        player.removeMediaItem(index)
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
        persistCurrentQueue(player)
        syncFromPlayer(player)
    }

    fun updatePlaybackMode(mode: PlaybackMode) {
        playbackMode = mode
        persistPlaybackModeState(mode)
        controller?.let { player ->
            applyPlaybackModeToPlayer(player, mode)
            syncFromPlayer(player)
        }
    }

    fun cyclePlaybackMode() {
        val nextMode = when (playbackMode) {
            PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.SINGLE_REPEAT
            PlaybackMode.SINGLE_REPEAT -> PlaybackMode.SEQUENTIAL
        }
        updatePlaybackMode(nextMode)
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
            currentAlbum = ""
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

    fun refreshMetadataForPath(path: String) {
        val targetPath = path.trim()
        if (targetPath.isEmpty()) return
        ContextCompat.getMainExecutor(context).execute {
            val player = controller ?: return@execute
            if (player.mediaItemCount <= 0) return@execute

            val currentIndexSnapshot = player.currentMediaItemIndex
            val resumePosition = player.currentPosition.coerceAtLeast(0L)
            val resumePlayWhenReady = player.playWhenReady
            val wasPlaying = player.isPlaying || resumePlayWhenReady
            val targetIndexes = mutableListOf<Pair<Int, Boolean>>()

            for (idx in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(idx)
                val itemPath = item.resolveSourcePath()
                if (itemPath == targetPath) {
                    targetIndexes += idx to (idx == currentIndexSnapshot)
                }
            }
            if (targetIndexes.isEmpty()) return@execute

            transcodeQueueExecutor.execute {
                val replacements = targetIndexes.mapNotNull { (idx, preferOriginalCover) ->
                    buildMediaItemForRenamedPath(
                        path = targetPath,
                        preferOriginalCover = preferOriginalCover
                    )?.let { updatedItem -> idx to updatedItem }
                }
                if (replacements.isEmpty()) return@execute

                ContextCompat.getMainExecutor(context).execute {
                    val latestPlayer = controller ?: return@execute
                    if (latestPlayer.mediaItemCount <= 0) return@execute

                    replacements.forEach { (idx, updatedItem) ->
                        if (idx in 0 until latestPlayer.mediaItemCount) {
                            val latestItemPath = latestPlayer.getMediaItemAt(idx).resolveSourcePath()
                            if (latestItemPath == targetPath) {
                                latestPlayer.replaceMediaItem(idx, updatedItem)
                            }
                        }
                    }

                    val currentIdx = latestPlayer.currentMediaItemIndex
                    if (currentIdx >= 0 && currentIdx == currentIndexSnapshot) {
                        latestPlayer.seekTo(currentIdx, resumePosition)
                    }
                    latestPlayer.playWhenReady = resumePlayWhenReady
                    if (wasPlaying) {
                        latestPlayer.play()
                    }
                    persistCurrentQueue(latestPlayer)
                    syncFromPlayer(latestPlayer)
                }
            }
        }
    }

    fun refreshProgress() {
        val player = controller ?: return
        positionMs = player.currentPosition.coerceAtLeast(0L)
        val updatedDuration = player.duration.takeIf { it > 0L }
        if (updatedDuration != null) {
            lastKnownDurationMs = updatedDuration
            durationMs = updatedDuration
        } else if (durationMs <= 0L && lastKnownDurationMs > 0L) {
            durationMs = lastKnownDurationMs
        }
    }

    private fun syncFromPlayer(player: Player) {
        if (removeDuplicateQueueItems(player)) {
            persistCurrentQueue(player)
            return
        }
        restoreTemporaryCompanionIfNeeded(player)
        isPlaying = player.isPlaying
        playWhenReadyRequested = player.playWhenReady
        playbackState = player.playbackState
        positionMs = player.currentPosition.coerceAtLeast(0L)
        val updatedDuration = player.duration.takeIf { it > 0L }
        if (updatedDuration != null) {
            lastKnownDurationMs = updatedDuration
            durationMs = updatedDuration
        } else if (durationMs <= 0L && lastKnownDurationMs > 0L) {
            durationMs = lastKnownDurationMs
        }
        currentIndex = player.currentMediaItemIndex
        mediaCount = player.mediaItemCount
        hasNextTrack = currentIndex >= 0 && player.mediaItemCount > 0
        queueAudioPaths = (0 until player.mediaItemCount).mapNotNull { idx ->
            val mediaItem = player.getMediaItemAt(idx)
            mediaItem.mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
                ?: mediaItem.mediaId.takeIf { it.isNotBlank() }
        }
        playbackMode = when {
            player.repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.SINGLE_REPEAT
            player.shuffleModeEnabled -> PlaybackMode.SHUFFLE
            else -> PlaybackMode.SEQUENTIAL
        }
        val manualNextIndex = resolveManualNextIndex(context, player, consume = false)
        val resolvedNextIndex = when {
            currentIndex < 0 || mediaCount <= 0 -> -1
            manualNextIndex in 0 until mediaCount -> manualNextIndex
            playbackMode == PlaybackMode.SINGLE_REPEAT -> {
                if (currentIndex + 1 in 0 until mediaCount) currentIndex + 1 else 0
            }
            else -> {
                val candidate = player.nextMediaItemIndex
                if (candidate in 0 until mediaCount) candidate else 0
            }
        }
        if (resolvedNextIndex in 0 until mediaCount) {
            val nextItem = player.getMediaItemAt(resolvedNextIndex)
            val nextExtras = nextItem.mediaMetadata.extras
            nextTrackAudioPath = nextExtras?.getString(EXTRA_AUDIO_PATH)
                ?: nextItem.mediaId.takeIf { it.isNotBlank() }
            nextTrackTitle = nextItem.mediaMetadata.title?.toString()
                ?: File(nextTrackAudioPath ?: "").nameWithoutExtension
        } else {
            nextTrackAudioPath = null
            nextTrackTitle = ""
        }
        persistPlaybackModeState(playbackMode)
        persistCurrentQueue(player)

        val item = player.currentMediaItem
        currentMediaId = item?.mediaId
        currentPlaybackUriPath = item?.localConfiguration?.uri?.path
        val extras = item?.mediaMetadata?.extras
        currentAudioPath = extras?.getString(EXTRA_AUDIO_PATH)
            ?: item?.mediaId?.takeIf { it.isNotBlank() }
        currentMediaStoreId = if (extras?.containsKey(EXTRA_MEDIA_STORE_ID) == true) {
            extras.getLong(EXTRA_MEDIA_STORE_ID, -1L)
        } else {
            -1L
        }
        currentCoverCachePath = extras?.getString(EXTRA_COVER_CACHE_PATH)
        currentArtworkData = item?.mediaMetadata?.artworkData
        currentTitle = item?.mediaMetadata?.title?.toString()
            ?: File(currentAudioPath ?: "").nameWithoutExtension
        currentArtist = item?.mediaMetadata?.artist?.toString() ?: "未知艺术家"
        currentAlbum = item?.mediaMetadata?.albumTitle?.toString().orEmpty()
        if (currentAudioPath.isNullOrBlank()) {
            clearSnapshotState()
        } else {
            persistSnapshotState()
        }
    }

    private fun removeDuplicateQueueItems(player: Player): Boolean {
        val count = player.mediaItemCount
        if (count <= 1) return false
        val currentIndex = player.currentMediaItemIndex
        val currentPath = if (currentIndex in 0 until count) {
            player.getMediaItemAt(currentIndex).resolveSourcePath()
        } else {
            null
        }
        val keepIndexByPath = linkedMapOf<String, Int>()
        for (index in 0 until count) {
            val path = player.getMediaItemAt(index).resolveSourcePath() ?: continue
            val existed = keepIndexByPath[path]
            if (existed == null) {
                keepIndexByPath[path] = index
            } else if (path == currentPath && index == currentIndex) {
                keepIndexByPath[path] = index
            }
        }
        val removeIndexes = mutableListOf<Int>()
        for (index in 0 until count) {
            val path = player.getMediaItemAt(index).resolveSourcePath() ?: continue
            val keepIndex = keepIndexByPath[path] ?: continue
            if (index != keepIndex) {
                removeIndexes += index
            }
        }
        if (removeIndexes.isEmpty()) return false
        removeIndexes.sortedDescending().forEach { player.removeMediaItem(it) }
        return true
    }

    private fun restoreTemporaryCompanionIfNeeded(player: Player) {
        val sourcePath = temporaryCompanionSourcePath ?: return
        val index = temporaryCompanionMediaIndex
        if (index !in 0 until player.mediaItemCount) {
            temporaryCompanionMediaIndex = -1
            temporaryCompanionSourcePath = null
            return
        }
        if (player.currentMediaItemIndex == index) {
            return
        }

        val item = player.getMediaItemAt(index)
        if (item.resolveSourcePath() != sourcePath) {
            temporaryCompanionMediaIndex = -1
            temporaryCompanionSourcePath = null
            return
        }

        val originalPlaybackPath = resolvePlayablePathForPlayback(context, sourcePath)
        val originalFile = File(originalPlaybackPath)
        if (!originalFile.exists() || !originalFile.isFile) {
            temporaryCompanionMediaIndex = -1
            temporaryCompanionSourcePath = null
            return
        }
        val originalPlayableUri = resolvePlayableAudioUri(
            context = context,
            sourcePath = sourcePath,
            playbackPath = originalPlaybackPath
        )

        val currentUriPath = item.localConfiguration?.uri?.path
        if (item.localConfiguration?.uri != originalPlayableUri && currentUriPath != originalFile.absolutePath) {
            val restoredItem = item.buildUpon()
                .setUri(originalPlayableUri)
                .build()
            player.replaceMediaItem(index, restoredItem)
        }
        temporaryCompanionMediaIndex = -1
        temporaryCompanionSourcePath = null
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
        }.distinctBy { it.path }
        persistQueueEntriesAsync(entries)
    }

    private fun restoreQueueIfNeededAsync(player: MediaController) {
        if (player.mediaItemCount > 0) return
        val targetPath = playbackStatePrefs.getString(KEY_LAST_AUDIO_PATH, null)
        transcodeQueueExecutor.execute {
            val savedPaths = LocalPlaylistStore.loadPlaybackQueuePaths(context)
            val uniqueSavedPaths = savedPaths.distinct()
            if (uniqueSavedPaths.isEmpty()) return@execute
            val targetIndex = uniqueSavedPaths.indexOfFirst { it == targetPath }.let { if (it >= 0) it else 0 }
            val restoredItems = uniqueSavedPaths.mapNotNull { path ->
                val file = File(path)
                if (!file.exists()) return@mapNotNull null
                val metadata = runCatching { AudioMetadataReader.readMetadata(context, path) }.getOrNull()
                val inferredCoverCachePath = resolveExistingCoverCachePath(path)
                val resolvedMediaStoreId = resolveMediaStoreIdByPath(context, path)
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
                    mediaStoreId = resolvedMediaStoreId
                )
                val shouldPreferOriginalCover = path == uniqueSavedPaths.getOrNull(targetIndex)
                audio.toPlayableMediaItem(context, preferOriginalCover = shouldPreferOriginalCover)
            }
            if (restoredItems.isEmpty()) return@execute
            val safeTargetIndex = targetIndex.coerceIn(0, restoredItems.lastIndex)

            ContextCompat.getMainExecutor(context).execute {
                val latestPlayer = controller ?: return@execute
                if (latestPlayer != player) return@execute
                if (latestPlayer.mediaItemCount > 0) return@execute
                try {
                    latestPlayer.setMediaItems(restoredItems, safeTargetIndex, 0L)
                    latestPlayer.prepare()
                } catch (e: Exception) {
                    Log.e("MusicPlaybackController", "Failed to restore queue", e)
                }
            }
        }
    }

    private fun persistSnapshotState() {
        val path = currentAudioPath
        if (path.isNullOrBlank()) return
        playbackStatePrefs.edit()
            .putString(KEY_LAST_AUDIO_PATH, path)
            .putString(KEY_LAST_TITLE, currentTitle)
            .putString(KEY_LAST_ARTIST, currentArtist)
            .putString(KEY_LAST_ALBUM, currentAlbum)
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
        currentAlbum = playbackStatePrefs.getString(KEY_LAST_ALBUM, null)
            ?.takeIf { it.isNotBlank() }
            ?: ""
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
        val resolvedMediaStoreId = resolveMediaStoreIdByPath(context, path)
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
            mediaStoreId = resolvedMediaStoreId
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
        persistQueueEntriesAsync(entries)
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
        persistQueueEntriesAsync(entries)
    }

    private fun persistQueueEntriesAsync(entries: List<LocalPlaylistEntry>) {
        pendingQueueEntries = entries
        if (queuePersistScheduled) return
        queuePersistScheduled = true
        queuePersistExecutor.execute {
            try {
                while (true) {
                    val snapshot = pendingQueueEntries ?: break
                    pendingQueueEntries = null
                    LocalPlaylistStore.savePlaybackQueue(context, snapshot)
                    if (pendingQueueEntries == null) break
                }
            } catch (e: Exception) {
                Log.e("MusicPlaybackController", "persistQueueEntriesAsync failed", e)
            } finally {
                queuePersistScheduled = false
                if (pendingQueueEntries != null) {
                    persistQueueEntriesAsync(pendingQueueEntries.orEmpty())
                }
            }
        }
    }

    private fun clearSnapshotState() {
        playbackStatePrefs.edit()
            .remove(KEY_LAST_AUDIO_PATH)
            .remove(KEY_LAST_TITLE)
            .remove(KEY_LAST_ARTIST)
            .remove(KEY_LAST_ALBUM)
            .remove(KEY_LAST_COVER_CACHE_PATH)
            .apply()
    }

    private fun restorePlaybackModeState() {
        playbackMode = when (playbackStatePrefs.getString(KEY_PLAYBACK_MODE, null)) {
            PlaybackMode.SHUFFLE.name -> PlaybackMode.SHUFFLE
            PlaybackMode.SINGLE_REPEAT.name -> PlaybackMode.SINGLE_REPEAT
            else -> PlaybackMode.SEQUENTIAL
        }
    }

    private fun persistPlaybackModeState(mode: PlaybackMode) {
        playbackStatePrefs.edit()
            .putString(KEY_PLAYBACK_MODE, mode.name)
            .apply()
    }

    private fun applyPlaybackModeToPlayer(player: Player, mode: PlaybackMode) {
        when (mode) {
            PlaybackMode.SEQUENTIAL -> {
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_ALL
            }
            PlaybackMode.SHUFFLE -> {
                player.shuffleModeEnabled = true
                player.repeatMode = Player.REPEAT_MODE_ALL
            }
            PlaybackMode.SINGLE_REPEAT -> {
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    private suspend fun animatePlayerVolume(
        player: Player,
        from: Float,
        to: Float,
        durationMs: Long
    ) {
        val clampedFrom = from.coerceIn(0f, 1f)
        val clampedTo = to.coerceIn(0f, 1f)
        if (durationMs <= 0L) {
            player.volume = clampedTo
            return
        }

        val steps = 12
        val stepDelay = (durationMs / steps).coerceAtLeast(12L)
        for (step in 0..steps) {
            val fraction = step / steps.toFloat()
            player.volume = clampedFrom + (clampedTo - clampedFrom) * fraction
            delay(stepDelay)
        }
        player.volume = clampedTo
    }
}

private fun AudioFile.toPlayableMediaItem(
    context: Context,
    preferOriginalCover: Boolean,
    playbackPathOverride: String? = null,
    manualNextToken: String? = null
): MediaItem {
    val playbackPath = playbackPathOverride
        ?.takeIf { it.isNotBlank() && File(it).exists() }
        ?: path
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(displayTitle)
        .setArtist(displayArtist)
        .setAlbumTitle(displayAlbum)
        .setExtras(Bundle().apply {
            putString(EXTRA_AUDIO_PATH, path)
            putString(EXTRA_COVER_CACHE_PATH, coverCachePath)
            putLong(EXTRA_MEDIA_STORE_ID, mediaStoreId)
            putString(EXTRA_MANUAL_NEXT_TOKEN, manualNextToken)
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
    val playableUri = resolvePlayableAudioUri(
        context = context,
        sourcePath = path,
        playbackPath = playbackPath,
        mediaStoreId = mediaStoreId
    )

    return MediaItem.Builder()
        .setMediaId(path)
        .setUri(playableUri)
        .setMediaMetadata(metadata)
        .build()
}

private fun resolvePlayableAudioUri(
    context: Context,
    sourcePath: String,
    playbackPath: String,
    mediaStoreId: Long = -1L
): Uri {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
        val mediaUri = resolveMediaStoreAudioUri(context, sourcePath, mediaStoreId)
        if (mediaUri != null) return mediaUri
    }
    val playbackFile = File(playbackPath)
    if (playbackFile.exists()) {
        return Uri.fromFile(playbackFile)
    }
    return Uri.fromFile(File(sourcePath))
}

private fun resolveMediaStoreAudioUri(
    context: Context,
    sourcePath: String,
    mediaStoreId: Long = -1L
): Uri? {
    return try {
        if (mediaStoreId > 0L) {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
        }
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

private fun MediaItem.resolveSourcePath(): String? {
    return mediaMetadata.extras?.getString(EXTRA_AUDIO_PATH)
        ?: mediaId.takeIf { it.isNotBlank() }
}

private fun resolvePlayablePathForPlayback(context: Context, sourcePath: String): String {
    val normalizedPath = sourcePath.trim()
    if (normalizedPath.isEmpty()) return sourcePath
    if (!normalizedPath.lowercase().endsWith(".m4a")) return sourcePath
    if (supportsDirectAlacDecode()) {
        return sourcePath
    }
    val isDetectedAlac = PlaybackAlacTranscodeManager.isAlacEncodedM4a(normalizedPath)
    if (!isDetectedAlac) return sourcePath
    Log.d("MusicPlaybackController", "ALAC direct decode unavailable, fallback transcode: $normalizedPath")
    return PlaybackAlacTranscodeManager.ensureTranscodedPath(
        context = context.applicationContext,
        sourcePath = normalizedPath,
        forceForM4aFailure = true
    ) ?: sourcePath
}

private fun resolveMediaStoreIdByPath(context: Context, sourcePath: String): Long {
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
            if (!cursor.moveToFirst()) return@use -1L
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        } ?: -1L
    } catch (_: Exception) {
        -1L
    }
}

private fun supportsDirectAlacDecode(): Boolean {
    cachedDirectAlacDecodeSupport?.let { return it }
    val detected = runCatching {
        FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_ALAC)
    }.getOrDefault(false)
    cachedDirectAlacDecodeSupport = detected
    return detected
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

    LaunchedEffect(controller) {
        AudioMetadataUpdateBus.updates.collect { path ->
            controller.refreshMetadataForPath(path)
        }
    }

    return controller
}
