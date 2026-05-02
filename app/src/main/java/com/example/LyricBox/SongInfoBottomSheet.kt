package com.example.LyricBox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.utils.AudioMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    onPlayNext: () -> Unit,
    onViewAlbum: (String) -> Unit,
    onViewArtists: (List<String>) -> Unit,
    onToggleFavorite: () -> Unit,
    onShareFile: () -> Unit,
    onRenameFile: () -> Unit,
    onDeleteFile: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                        onPlayNext()
                        hasQueuedAsNext = true
                    }
                }
            )
            SongInfoActionItem(title = "查看专辑", onClick = {
                if (infoState.album.isNotBlank()) {
                    onViewAlbum(infoState.album)
                }
            })
            SongInfoActionItem(title = "查看艺术家", onClick = {
                val artists = extractAllArtistsForSheet(
                    title = infoState.title,
                    artist = infoState.artist
                )
                onViewArtists(artists)
            })
            SongInfoActionItem(
                title = if (favoriteState) "取消收藏" else "收藏音乐",
                onClick = {
                    onToggleFavorite()
                    favoriteState = !favoriteState
                }
            )
            SongInfoActionItem(title = "分享文件", onClick = onShareFile)
            SongInfoActionItem(
                title = if (showRenameSuccess) "重命名成功" else "重命名文件",
                onClick = onRenameFile,
                textColor = if (showRenameSuccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            SongInfoActionItem(
                title = "删除文件",
                onClick = onDeleteFile,
                textColor = MaterialTheme.colorScheme.error
            )
        }
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
