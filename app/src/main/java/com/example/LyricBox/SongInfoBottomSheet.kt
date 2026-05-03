package com.example.LyricBox

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.LyricBox.utils.AudioMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SongInfoState(
    val title: String,
    val artist: String,
    val album: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoBottomSheet(
    audio: AudioFile,
    isFavorite: Boolean,
    renameSuccessSignal: Long,
    onDismiss: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onViewAlbum: ((String) -> Unit)? = null,
    onViewArtists: ((List<String>) -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onShareFile: (() -> Unit)? = null,
    onRenameFile: (() -> Unit)? = null,
    onDeleteFile: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fallbackPlaybackController = if (onPlayNext == null) rememberMusicPlaybackController() else null
    var infoState by remember {
        mutableStateOf(
            SongInfoState(
                title = audio.displayTitle,
                artist = audio.displayArtist,
                album = audio.displayAlbum
            )
        )
    }
    var hasQueuedAsNext by remember(audio.path) { mutableStateOf(false) }
    var favoriteState by remember(audio.path, isFavorite) { mutableStateOf(isFavorite) }
    var showRenameSuccess by remember(audio.path) { mutableStateOf(false) }
    var showArtistSelectionSheet by remember(audio.path) { mutableStateOf(false) }
    var pendingArtists by remember(audio.path) { mutableStateOf<List<String>>(emptyList()) }
    var showRenameDialog by remember(audio.path) { mutableStateOf(false) }
    var renameInputValue by remember(audio.path) { mutableStateOf(File(audio.path).nameWithoutExtension) }
    val renameFileExtension = remember(audio.path) {
        File(audio.path).extension
            .takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            ?: ""
    }
    var showDeleteConfirmDialog by remember(audio.path) { mutableStateOf(false) }
    var renameIndicatorJob by remember(audio.path) { mutableStateOf<Job?>(null) }
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) { sheetState.show() }

    LaunchedEffect(audio.path, audio.coverCachePath, audio.mediaStoreId) {
        val meta = withContext(Dispatchers.IO) {
            runCatching { AudioMetadataReader.readMetadata(context, audio.path, audio.mediaStoreId) }
                .getOrNull()
        }
        if (meta != null) {
            infoState = SongInfoState(
                title = meta.title.ifBlank { audio.displayTitle },
                artist = meta.artist.ifBlank { audio.displayArtist },
                album = meta.album.ifBlank { audio.displayAlbum }
            )
            coverBitmap = withContext(Dispatchers.IO) {
                meta.cover?.let { data ->
                    runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull()
                } ?: loadCoverFromCache(audio.coverCachePath)
            }
        } else {
            coverBitmap = withContext(Dispatchers.IO) { loadCoverFromCache(audio.coverCachePath) }
        }
    }

    LaunchedEffect(renameSuccessSignal) {
        if (renameSuccessSignal <= 0L) return@LaunchedEffect
        showRenameSuccess = true
        delay(1000)
        showRenameSuccess = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "歌曲信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = "封面",
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(74.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = infoState.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${infoState.artist} - ${infoState.album}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SongInfoActionItem(
                title = if (hasQueuedAsNext) "已加入下一首" else "下一首播放",
                onClick = {
                    if (!hasQueuedAsNext) {
                        if (onPlayNext != null) {
                            onPlayNext()
                            hasQueuedAsNext = true
                        } else if (fallbackPlaybackController != null && fallbackPlaybackController.isReady) {
                            fallbackPlaybackController.insertNext(audio)
                            hasQueuedAsNext = true
                        } else {
                            Toast.makeText(context, "播放控制器未就绪，稍后再试", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            SongInfoActionItem(title = "查看专辑", onClick = {
                if (infoState.album.isNotBlank()) {
                    if (onViewAlbum != null) {
                        onViewAlbum(infoState.album)
                    } else {
                        launchMusicLibrarySearch(context, "#专辑：${infoState.album}")
                    }
                }
            })
            SongInfoActionItem(title = "查看艺术家", onClick = {
                val artists = extractAllArtistsForSheet(
                    title = infoState.title,
                    artist = infoState.artist
                )
                if (artists.isEmpty()) {
                } else if (onViewArtists != null) {
                    onViewArtists(artists)
                } else {
                    pendingArtists = artists
                    showArtistSelectionSheet = true
                }
            })
            SongInfoActionItem(
                title = if (favoriteState) "取消收藏" else "收藏音乐",
                onClick = {
                    if (onToggleFavorite != null) {
                        onToggleFavorite()
                        favoriteState = !favoriteState
                    } else {
                        favoriteState = toggleFavoriteForAudio(context, audio)
                    }
                }
            )
            SongInfoActionItem(
                title = "分享文件",
                onClick = {
                    if (onShareFile != null) {
                        onShareFile()
                    } else {
                        shareAudioFile(context, audio)
                    }
                }
            )
            SongInfoActionItem(
                title = if (showRenameSuccess) "重命名成功" else "重命名文件",
                onClick = {
                    if (onRenameFile != null) {
                        onRenameFile()
                    } else {
                        renameInputValue = File(audio.path).nameWithoutExtension
                        showRenameDialog = true
                    }
                },
                textColor = if (showRenameSuccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            SongInfoActionItem(
                title = "删除文件",
                onClick = {
                    if (onDeleteFile != null) {
                        onDeleteFile()
                    } else {
                        showDeleteConfirmDialog = true
                    }
                },
                textColor = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showArtistSelectionSheet) {
        ArtistSelectionBottomSheet(
            artists = pendingArtists,
            onDismiss = { showArtistSelectionSheet = false },
            onSelectArtist = { artist ->
                showArtistSelectionSheet = false
                launchMusicLibrarySearch(context, artist)
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名文件") },
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
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp
                        ),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (renameInputValue.isEmpty()) {
                                    Text(
                                        text = "输入文件名",
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
                        val result = renameAudioFile(
                            context = context,
                            audio = audio,
                            inputName = renameInputValue,
                            extension = renameFileExtension,
                            playbackController = fallbackPlaybackController
                        )
                        if (result) {
                            showRenameDialog = false
                            showRenameSuccess = true
                            renameIndicatorJob?.cancel()
                            renameIndicatorJob = scope.launch {
                                delay(1000)
                                showRenameSuccess = false
                            }
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("删除文件") },
            text = { Text("确认删除该音频文件吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = File(audio.path)
                        val deleted = runCatching { file.exists() && file.delete() }.getOrDefault(false)
                        if (deleted) {
                            runCatching {
                                val parent = file.parentFile
                                if (parent != null) {
                                    val ttml = File(parent, "${file.nameWithoutExtension}.ttml")
                                    if (ttml.exists()) ttml.delete()
                                }
                            }
                            removeFavoritePath(context, audio.path)
                            // 预览页场景：删除后从播放队列移除并优先切换到下一首。
                            fallbackPlaybackController?.removeAudioPathAndAdvance(audio.path)
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SongInfoActionItem(
    title: String,
    onClick: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun loadCoverFromCache(cachePath: String?): Bitmap? {
    if (cachePath.isNullOrBlank()) return null
    return try {
        val file = File(cachePath)
        if (!file.exists()) return null
        BitmapFactory.decodeFile(file.absolutePath)
    } catch (e: Exception) {
        Log.e("SongInfoBottomSheet", "Error reading cover cache", e)
        null
    }
}

private fun extractAllArtistsForSheet(title: String, artist: String): List<String> {
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

private fun buildShareableAudioUri(audio: AudioFile): Uri {
    return if (audio.mediaStoreId > 0L) {
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audio.mediaStoreId)
    } else {
        Uri.EMPTY
    }
}

private fun shareAudioFile(context: Context, audio: AudioFile) {
    runCatching {
        val shareUri = if (audio.mediaStoreId > 0L) {
            buildShareableAudioUri(audio)
        } else {
            val file = File(audio.path)
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享音频"))
    }.onFailure {
        Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
    }
}

private fun launchMusicLibrarySearch(context: Context, query: String) {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return
    val intent = Intent(context, MusicLibraryActivity::class.java).apply {
        putExtra(MusicLibraryActivity.EXTRA_INITIAL_SEARCH_QUERY, normalizedQuery)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(intent)
}

private fun toggleFavoriteForAudio(context: Context, audio: AudioFile): Boolean {
    val favorites = LocalPlaylistStore.loadFavoritePaths(context).toMutableSet()
    val shouldFavorite = !favorites.contains(audio.path)
    if (shouldFavorite) {
        favorites.add(audio.path)
    } else {
        favorites.remove(audio.path)
    }
    saveFavoritePaths(context, favorites)
    return shouldFavorite
}

private fun removeFavoritePath(context: Context, path: String) {
    val favorites = LocalPlaylistStore.loadFavoritePaths(context).toMutableSet()
    if (favorites.remove(path)) {
        saveFavoritePaths(context, favorites)
    }
}

private fun saveFavoritePaths(context: Context, paths: Set<String>) {
    val entries = paths.map { path ->
        val file = File(path)
        LocalPlaylistEntry(
            path = path,
            title = file.nameWithoutExtension,
            artist = "",
            durationSeconds = -1L
        )
    }
    LocalPlaylistStore.saveFavorites(context, entries)
}

private fun renameAudioFile(
    context: Context,
    audio: AudioFile,
    inputName: String,
    extension: String,
    playbackController: MusicPlaybackController? = null
): Boolean {
    val newName = inputName.trim()
    if (newName.isEmpty()) {
        Toast.makeText(context, "文件名不能为空", Toast.LENGTH_SHORT).show()
        return false
    }
    val sourceFile = File(audio.path)
    if (!sourceFile.exists()) {
        Toast.makeText(context, "源文件不存在", Toast.LENGTH_SHORT).show()
        return false
    }
    val targetFile = File(sourceFile.parentFile, "$newName$extension")
    if (targetFile.absolutePath == sourceFile.absolutePath) {
        Toast.makeText(context, "文件名未变化", Toast.LENGTH_SHORT).show()
        return false
    }
    if (targetFile.exists()) {
        Toast.makeText(context, "目标文件已存在", Toast.LENGTH_SHORT).show()
        return false
    }
    val renamed = runCatching { sourceFile.renameTo(targetFile) }.getOrDefault(false)
    if (!renamed) {
        Toast.makeText(context, "重命名失败", Toast.LENGTH_SHORT).show()
        return false
    }

    runCatching {
        val oldTtml = File(sourceFile.parentFile, "${sourceFile.nameWithoutExtension}.ttml")
        if (oldTtml.exists()) {
            val newTtml = File(targetFile.parentFile, "${targetFile.nameWithoutExtension}.ttml")
            oldTtml.renameTo(newTtml)
        }
    }

    val favorites = LocalPlaylistStore.loadFavoritePaths(context).toMutableSet()
    if (favorites.remove(sourceFile.absolutePath)) {
        favorites.add(targetFile.absolutePath)
        saveFavoritePaths(context, favorites)
    }
    playbackController?.handleAudioRenamed(
        oldPath = sourceFile.absolutePath,
        newPath = targetFile.absolutePath
    )
    return true
}
