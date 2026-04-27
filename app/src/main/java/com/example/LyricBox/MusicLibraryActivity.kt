package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.collection.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.lyrics.LyricsService
import com.example.LyricBox.lyrics.models.SongInfo
import com.example.LyricBox.lyrics.models.Source
import com.example.LyricBox.lyrics.models.VerbatimLyricsResult
import com.example.LyricBox.lyrics.parser.VerbatimLrcConverter
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.PiracyChecker
import com.example.LyricBox.utils.PiracyCheckResult
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
        get() = if (artist.isNotEmpty()) artist else "未知艺术家"
    
    val displayAlbum: String
        get() = if (album.isNotEmpty()) album else "未知专辑"
    
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

enum class SortType(val displayName: String) {
    FILE_NAME("文件名称"),
    MODIFY_TIME("修改时间"),
    ADD_TIME("新增时间"),
    YEAR("年份")
}

enum class SortOrder(val displayName: String) {
    ASC("正序"),
    DESC("反序")
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

class MusicLibraryActivity : ComponentActivity() {
    
    private var piracyCheckResult by mutableStateOf<PiracyCheckResult?>(null)
    private var showPiracyWarning by mutableStateOf(false)
    private var externalAudioFile by mutableStateOf<AudioFile?>(null)
    private var editingMetadataPath by mutableStateOf<String?>(null)
    private var _refreshMetadataPath by mutableStateOf<String?>(null)
    
    companion object {
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val REQUEST_CODE_EDIT_METADATA = 200
        
        private val coverCache = LruCache<String, Bitmap>(50).apply {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            val cacheSize = maxMemory / 8
            resize(cacheSize)
        }
        
        fun getCoverFromCache(cachePath: String): Bitmap? = coverCache.get(cachePath)
        fun putCoverToCache(cachePath: String, bitmap: Bitmap) {
            coverCache.put(cachePath, bitmap)
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
        enableEdgeToEdge()
        
        val checkResult = PiracyChecker.checkAll(this)
        if (checkResult.isPirated) {
            piracyCheckResult = checkResult
            showPiracyWarning = true
        }
        
        // 检查是否从第三方应用调用
        val externalPath = handleExternalIntent(intent)
        
        val prefs = getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE)
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0)
                ) { paddingValues ->
                    if (externalAudioFile != null) {
                        ExternalAudioScreen(
                            audio = externalAudioFile!!,
                            onBack = { finish() },
                            onEditLyrics = { lyricsContent, format ->
                                val intent = Intent(this, LyricTimingActivity::class.java).apply {
                                    putExtra("audioPath", externalAudioFile!!.path)
                                    putExtra("lyricsContent", lyricsContent)
                                    putExtra("sourceTitle", externalAudioFile!!.displayTitle)
                                    putExtra("sourceArtist", externalAudioFile!!.artist)
                                    putExtra("lyricsFormat", format)
                                }
                                startActivity(intent)
                            },
                            onEditMetadata = { path ->
                                val intent = Intent(this, SongMetadataEditActivity::class.java).apply {
                                    putExtra(SongMetadataEditActivity.EXTRA_AUDIO_PATH, path)
                                }
                                startActivity(intent)
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    } else {
                        MusicLibraryScreen(
                            onBack = { finish() },
                            onOpenSettings = {
                                startActivityForResult(Intent(this, MusicLibrarySettingsActivity::class.java), 100)
                            },
                            onEditMetadata = { path ->
                                editingMetadataPath = path
                                val intent = Intent(this, SongMetadataEditActivity::class.java).apply {
                                    putExtra(SongMetadataEditActivity.EXTRA_AUDIO_PATH, path)
                                }
                                startActivityForResult(intent, REQUEST_CODE_EDIT_METADATA)
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
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
            val externalPath = handleExternalIntent(it)
            if (externalPath != null) {
                loadExternalAudioFile(externalPath)
            }
        }
    }
    
    private fun handleExternalIntent(intent: Intent): String? {
        // 检查是否有直接传入的路径
        val directPath = intent.getStringExtra(EXTRA_AUDIO_PATH)
        if (directPath != null && File(directPath).exists()) {
            return directPath
        }
        
        // 处理 ACTION_SEND
        if (intent.action == Intent.ACTION_SEND) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null) {
                return getRealPathFromUri(uri)
            }
        }
        
        // 处理 ACTION_SEND_MULTIPLE（只取第一个）
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            if (uris != null && uris.isNotEmpty()) {
                return getRealPathFromUri(uris[0])
            }
        }
        
        // 检查 Intent 的 data（用于 ACTION_VIEW 和 ACTION_EDIT）
        val uri = intent.data ?: return null
        
        // 尝试从 Uri 获取真实路径
        return getRealPathFromUri(uri)
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        // 首先尝试直接使用 uri.path
        uri.path?.let { path ->
            val file = File(path)
            if (file.exists()) {
                return path
            }
        }

        // 对于 content:// 类型的 Uri，尝试获取真实路径
        if (uri.scheme == "content") {
            // 尝试使用不同的方法获取真实路径
            var path: String? = null

            // 方法 1: 尝试从 _data 列获取
            path = getPathFromDataColumn(uri)
            if (path != null && File(path).exists()) {
                return path
            }

            // 方法 2: 尝试从 MediaStore 获取
            path = getPathFromMediaStore(uri)
            if (path != null && File(path).exists()) {
                return path
            }

            // 方法 3: 尝试从 DocumentsProvider 获取
            path = getPathFromDocumentsProvider(uri)
            if (path != null && File(path).exists()) {
                return path
            }

            // 如果以上方法都失败，才回退到复制到缓存的方法
            // 但这时候我们需要给用户一个提示或者警告
            Log.w("MusicLibrary", "无法获取原始文件路径，将使用缓存文件")
            return try {
                val inputStream = contentResolver.openInputStream(uri) ?: return null
                val fileName = getFileName(uri) ?: "temp_audio_${System.currentTimeMillis()}"
                val tempFile = File(cacheDir, fileName)

                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()

                tempFile.absolutePath
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error copying file from content URI", e)
                null
            }
        }

        return null
    }

    private fun getPathFromDataColumn(uri: Uri): String? {
        return try {
            val projection = arrayOf("_data")
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow("_data")
                    it.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
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
            val selectionArgs = arrayOf(uri.lastPathSegment ?: return null)
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
                    it.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("MusicLibrary", "Error getting path from MediaStore", e)
            null
        }
    }

    private fun getPathFromDocumentsProvider(uri: Uri): String? {
        return try {
            // 检查是否是 DocumentsProvider URI
            if ("com.android.externalstorage.documents" == uri.authority) {
                val docId = android.provider.DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return android.os.Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            }
            null
        } catch (e: Exception) {
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
                val file = File(path)
                if (!file.exists()) return@Thread
                
                val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(path)
                
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
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error loading external audio file", e)
            }
        }.start()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            recreate()
        } else if (requestCode == REQUEST_CODE_EDIT_METADATA) {
            if (editingMetadataPath != null) {
                _refreshMetadataPath = editingMetadataPath
                editingMetadataPath = null
            }
        }
    }
    
    fun getRefreshMetadataPath(): String? {
        val path = _refreshMetadataPath
        _refreshMetadataPath = null
        return path
    }
}

@Composable
fun MusicLibraryScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditMetadata: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val songClickAction = remember { prefs.getString("songClickAction", "editLyrics") } ?: "editLyrics"
    val scope = rememberCoroutineScope()
    
    val allAudioFiles = remember { mutableStateListOf<AudioFile>() }
    val displayAudioFiles = remember { mutableStateListOf<AudioFile>() }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0 to 0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedAudio by remember { mutableStateOf<AudioFile?>(null) }
    var showAudioOptionsDialog by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var scanSummary by remember { mutableStateOf(ScanSummary()) }
    var isFromCache by remember { mutableStateOf(false) }
    var showScanComplete by remember { mutableStateOf(false) }
    var isLoadingCache by remember { mutableStateOf(true) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedPaths = remember { mutableStateSetOf<String>() }
    val lastSelectedIndices = remember { mutableStateListOf<Int>() }
    
    BackHandler(enabled = isMultiSelectMode) {
        isMultiSelectMode = false
        selectedPaths.clear()
    }
    var showBatchMenu by remember { mutableStateOf(false) }
    var showBatchMatchConfig by remember { mutableStateOf(false) }
    var batchMatchResult by remember { mutableStateOf<BatchMatchResult?>(null) }
    var showBatchMatchResult by remember { mutableStateOf(false) }
    var isBatchMatching by remember { mutableStateOf(false) }
    var isCancelled by remember { mutableStateOf(false) }
    var batchMatchProgress by remember { mutableStateOf(0 to 0) }
    var showRangeSelectDialog by remember { mutableStateOf(false) }
    
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
    
    LaunchedEffect(Unit) {
        val pathToRefresh = activity.getRefreshMetadataPath()
        if (pathToRefresh != null) {
            refreshAudioFileMetadata(context, pathToRefresh, allAudioFiles)
            val filtered = if (searchQuery.isEmpty()) {
                allAudioFiles.toList()
            } else {
                allAudioFiles.filter { 
                    it.displayTitle.contains(searchQuery, ignoreCase = true) || 
                    it.displayArtist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
                }
            }
            displayAudioFiles.clear()
            displayAudioFiles.addAll(filtered)
            sortAudioFiles(displayAudioFiles, sortType.value, sortOrder.value)
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pathToRefresh = activity.getRefreshMetadataPath()
                if (pathToRefresh != null) {
                    scope.launch {
                        refreshAudioFileMetadata(context, pathToRefresh, allAudioFiles)
                        val filtered = if (searchQuery.isEmpty()) {
                            allAudioFiles.toList()
                        } else {
                            allAudioFiles.filter { 
                                it.displayTitle.contains(searchQuery, ignoreCase = true) || 
                                it.displayArtist.contains(searchQuery, ignoreCase = true) ||
                                it.album.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        displayAudioFiles.clear()
                        displayAudioFiles.addAll(filtered)
                        sortAudioFiles(displayAudioFiles, sortType.value, sortOrder.value)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    fun updateDisplayFiles() {
        val filtered = if (searchQuery.isEmpty()) {
            allAudioFiles.toList()
        } else {
            allAudioFiles.filter { 
                it.displayTitle.contains(searchQuery, ignoreCase = true) || 
                it.displayArtist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }
        displayAudioFiles.clear()
        displayAudioFiles.addAll(filtered)
        sortAudioFiles(displayAudioFiles, sortType.value, sortOrder.value)
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
    
    LaunchedEffect(Unit) {
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
        
        // 检查目录是否有变化（新增或减少）
        val foldersChanged = currentFolders != lastScannedFolders
        val shouldScan = if (useNativeMediaLibrary) {
            true
        } else {
            foldersChanged || autoScanEnabled
        }

        // 原生媒体库模式默认自动增量同步；目录模式按原逻辑触发扫描
        if (shouldScan) {
            scanJob = launch {
                isScanning = true
                scanAudioFiles(context, prefs, allAudioFiles,
                    onProgress = { current, total ->
                        scanProgress = current to total
                    },
                    onComplete = { summary ->
                        isScanning = false
                        scanSummary = summary
                        if (isFromCache) {
                            showScanComplete = true
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                showScanComplete = false
                            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(durationMillis = 500))
        ) {
            var lastClickTime by remember { mutableStateOf(0L) }
            var showDoubleTapHint by remember { mutableStateOf(false) }
            var previousFirstVisibleIndex by remember { mutableStateOf(0) }
            var hasShownHint by remember { mutableStateOf(false) }
            
            val listState = if (displayAudioFiles.isNotEmpty() || isScanning) {
                rememberLazyListState()
            } else {
                rememberLazyListState()
            }
            
            LaunchedEffect(showDoubleTapHint) {
                if (showDoubleTapHint) {
                    delay(2000)
                    showDoubleTapHint = false
                }
            }
            
            LaunchedEffect(listState.firstVisibleItemIndex) {
                val currentIndex = listState.firstVisibleItemIndex
                if (!hasShownHint && currentIndex >= 14 && currentIndex > previousFirstVisibleIndex) {
                    hasShownHint = true
                    showDoubleTapHint = true
                }
                previousFirstVisibleIndex = currentIndex
            }
            
            val headbarTitle = when {
                isMultiSelectMode -> "已选 ${selectedPaths.size}"
                showDoubleTapHint -> "双击返回顶部"
                else -> "音乐库"
            }
            
            CommonHeadBar(
                title = headbarTitle,
                showBack = true,
                showMenu = !isMultiSelectMode,
                onBackClick = onBack,
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
                    CustomDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        items = listOf(
                            MenuItem(title = "多选模式", onClick = { 
                                menuExpanded = false
                                isMultiSelectMode = true 
                            }),
                            MenuItem(title = "目录设置", onClick = { onOpenSettings() }),
                            MenuItem(
                                title = "刷新",
                                onClick = {
                                    isScanning = true
                                    scanJob?.cancel()
                                    scanJob = scope.launch {
                                        scanAudioFiles(context, prefs, allAudioFiles,
                                            onProgress = { current, total ->
                                                scanProgress = current to total
                                            },
                                            onComplete = { summary ->
                                                isScanning = false
                                                scanSummary = summary
                                                showScanComplete = true
                                                scope.launch {
                                                    kotlinx.coroutines.delay(3000)
                                                    showScanComplete = false
                                                }
                                                updateDisplayFiles()
                                            }
                                        )
                                    }
                                }
                            )
                        ),
                        anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f)
                    )
                }
            )
            
            MusicLibraryBarSwitch(
                isMultiSelectMode = isMultiSelectMode,
                multiSelectActionBar = {
                    MultiSelectActionBar(
                        onSelectAll = { selectAll() },
                        onInvertSelection = { invertSelection() },
                        onClearSelection = { clearSelection() },
                        onRangeSelect = { showRangeSelectDialog = true },
                        onExit = { exitMultiSelectMode() }
                    )
                },
                searchBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            modifier = Modifier.fillMaxWidth(),
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
                                        contentDescription = "搜索",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "搜索歌曲、艺术家或专辑...",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
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
                                                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                                contentDescription = "清除",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            )
            
            if (isLoadingCache) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(10) { index ->
                        androidx.compose.runtime.key(index) {
                            AudioFileItemPlaceholder()
                        }
                    }
                }
            } else if (displayAudioFiles.isEmpty() && !isScanning) {
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
                            text = if (searchQuery.isNotEmpty()) "未找到匹配的歌曲" else "未找到音频文件",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "请检查设置中的目录配置",
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
                
                LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                    resetHideTimer()
                    if (!isDragging) {
                        val totalItems = displayAudioFiles.size
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        val itemHeight = 80
                        val maxScrollIndex = (totalItems - (viewportHeight / itemHeight)).coerceAtLeast(0)
                        if (maxScrollIndex > 0) {
                            dragProgress = listState.firstVisibleItemIndex.toFloat() / maxScrollIndex.toFloat()
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
                    val bottomPadding = navigationBarsPadding.calculateBottomPadding() + 8.dp
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
                        items(displayAudioFiles, key = { it.path }) { audio ->
                            val index = displayAudioFiles.indexOf(audio) + 1
                            val isSelected = selectedPaths.contains(audio.path)
                            AudioFileItem(
                                audio = audio,
                                isInMultiSelectMode = isMultiSelectMode,
                                isSelected = isSelected,
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
                                        if (songClickAction == "editMetadata") {
                                            onEditMetadata(audio.path)
                                        } else {
                                            selectedAudio = audio
                                            showAudioOptionsDialog = true
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
                                }
                            )
                        }
                    }
                    
                    val totalItems = displayAudioFiles.size
                    if (totalItems > 0) {
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        val itemHeight = 80
                        val totalContentHeight = totalItems * itemHeight
                        val thumbHeightRatio = viewportHeight.toFloat() / totalContentHeight.coerceAtLeast(viewportHeight)
                        val thumbHeightPx = (viewportHeight * thumbHeightRatio).coerceAtLeast(80f)
                        
                        val maxScrollIndex = (totalItems - (viewportHeight / itemHeight)).coerceAtLeast(0)
                        
                        val density = LocalDensity.current
                        val scrollbarHeightPx = viewportHeight - 16 - with(density) { 48.dp.toPx() }.toInt()
                        val thumbOffsetY = dragProgress.coerceIn(0f, 1f) * (scrollbarHeightPx - thumbHeightPx)
                        
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
                                                val delta = dragAmount.y / (scrollbarHeightPx - thumbHeightPx)
                                                dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                                                val targetIndex = (dragProgress * maxScrollIndex).toInt()
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
        
        val fabHeight = 56.dp
        val fabOffset by animateDpAsState(
            targetValue = if ((isScanning || showScanComplete) && isMultiSelectMode) fabHeight + 8.dp else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "fabOffset"
        )
        
        AnimatedVisibility(
            visible = isScanning || showScanComplete,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
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
                    targetState = isScanning,
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
                                        text = "正在扫描音频文件...",
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
                                text = "扫描完成，共发现 ${scanSummary.totalCount} 首歌曲\n新增 ${scanSummary.addedCount} 首，删除 ${scanSummary.removedCount} 首，元数据更新 ${scanSummary.updatedCount} 首",
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
    
    if (showAudioOptionsDialog && selectedAudio != null) {
        val audioPath = selectedAudio!!.path
        val audioTitle = selectedAudio!!.displayTitle
        val audioArtist = selectedAudio!!.artist
        AudioOptionsDialog(
            audio = selectedAudio!!,
            onDismiss = {
                showAudioOptionsDialog = false
                selectedAudio = null
            },
            onEditLyrics = { lyricsContent, format ->
                showAudioOptionsDialog = false
                val intent = Intent(context, LyricTimingActivity::class.java).apply {
                    putExtra("audioPath", audioPath)
                    putExtra("lyricsContent", lyricsContent)
                    putExtra("sourceTitle", audioTitle)
                    putExtra("sourceArtist", audioArtist)
                    putExtra("lyricsFormat", format)
                }
                context.startActivity(intent)
            },
            onEditMetadata = { path ->
                onEditMetadata(path)
            }
        )
    }
    
    if (showRangeSelectDialog) {
        RangeSelectDialog(
            maxIndex = displayAudioFiles.size,
            lastSelectedIndices = lastSelectedIndices,
            onConfirm = { start, end ->
                selectRange(start, end)
                showRangeSelectDialog = false
            },
            onDismiss = { showRangeSelectDialog = false }
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
                    refreshAudioFileMetadata(context, item.path, allAudioFiles)
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
                                val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(newFile.absolutePath)
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
    sequenceNumber: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onSelectionChange: ((Boolean) -> Unit)? = null
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
    
    LaunchedEffect(audio.coverCachePath) {
        coverBitmap = null
        audio.coverCachePath?.let { cachePath ->
            val cached = MusicLibraryActivity.getCoverFromCache(cachePath)
            if (cached != null) {
                coverBitmap = cached
            } else {
                delay(100)
                withContext(Dispatchers.IO) {
                    try {
                        val cacheFile = File(cachePath)
                        if (cacheFile.exists()) {
                            val bytes = cacheFile.readBytes()
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                            val sampleSize = MusicLibraryActivity.calculateInSampleSize(options, targetSizePx, targetSizePx)
                            options.inJustDecodeBounds = false
                            options.inSampleSize = sampleSize
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                            bitmap?.let {
                                MusicLibraryActivity.putCoverToCache(cachePath, it)
                                coverBitmap = it
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicLibrary", "Error loading cover bitmap", e)
                    }
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
            .background(
                if (isInMultiSelectMode && isSelected) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
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
                contentDescription = "专辑封面",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = "音频",
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOptionsDialog(
    audio: AudioFile,
    onDismiss: () -> Unit,
    onEditLyrics: (String?, String) -> Unit,
    onEditMetadata: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var embeddedLyrics by remember { mutableStateOf<String?>(null) }
    var externalLyrics by remember { mutableStateOf<String?>(null) }
    var externalLyricsPath by remember { mutableStateOf<String?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(true) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedFormat by remember { mutableStateOf(0) }
    var showLyricsPreview by remember { mutableStateOf(false) }
    var previewLyricsContent by remember { mutableStateOf("") }
    var previewLyricsTitle by remember { mutableStateOf("") }
    var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val hasEmbedded by remember { derivedStateOf { embeddedLyrics != null } }
    val hasExternal by remember { derivedStateOf { externalLyrics != null } }
    val hasLyrics by remember { derivedStateOf { hasEmbedded || hasExternal } }
    
    val formats = listOf(
        "纯文本歌词",
        "LRC逐行/逐字歌词",
        "增强LRC/ELRC歌词",
        "TTML歌词"
    )
    
    fun detectLyricsFormat(lyrics: String): Int {
        val trimmed = lyrics.trim()
        
        if (trimmed.contains("<tt xmlns")) {
            return 3
        }
        
        val hasAngleBrackets = trimmed.contains("<\\d{2}:\\d{2}\\.\\d{2,3}>".toRegex())
        val lines = trimmed.lines().filter { it.isNotBlank() }
        var lineWithMultipleTimestamps = 0
        
        for (line in lines.take(20)) {
            val timestamps = "\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]".toRegex().findAll(line).count()
            if (timestamps > 1) {
                lineWithMultipleTimestamps++
            }
        }
        
        if (hasAngleBrackets) {
            return 2
        }
        
        if (lineWithMultipleTimestamps > 0) {
            return 1
        }
        
        val hasTimestamps = trimmed.contains("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]".toRegex())
        if (hasTimestamps) {
            return 1
        }
        
        return 0
    }
    
    val detectedFormat by remember { derivedStateOf { 
        embeddedLyrics?.let { detectLyricsFormat(it) } ?: 0
    } }
    
    val recommendedFormatIndex by remember { derivedStateOf { detectedFormat } }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(Unit) {
        sheetState.show()
    }
    
    LaunchedEffect(audio.path) {
        isLoadingLyrics = true
        
        // 加载封面
        audio.coverCachePath?.let { cachePath ->
            withContext(Dispatchers.IO) {
                try {
                    val cacheFile = File(cachePath)
                    if (cacheFile.exists()) {
                        val bytes = cacheFile.readBytes()
                        coverBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } catch (e: Exception) {
                    Log.e("MusicLibrary", "Error loading cover bitmap", e)
                }
            }
        }
        
        embeddedLyrics = withContext(Dispatchers.IO) {
            extractEmbeddedLyrics(audio.path)
        }
        
        val ttmlFile = java.io.File(audio.path).let { audioFile ->
            java.io.File(audioFile.parent, audioFile.nameWithoutExtension + ".ttml")
        }
        
        if (ttmlFile.exists()) {
            externalLyrics = withContext(Dispatchers.IO) {
                try {
                    ttmlFile.readText()
                } catch (e: Exception) {
                    Log.e("MusicLibrary", "Error reading external TTML", e)
                    null
                }
            }
            externalLyricsPath = ttmlFile.absolutePath
        }
        
        if (embeddedLyrics != null && externalLyrics == null) {
            selectedSource = "embedded"
            selectedFormat = detectLyricsFormat(embeddedLyrics!!)
        }
        
        isLoadingLyrics = false
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
                .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = "专辑封面",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_play),
                        contentDescription = "音频",
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
                        text = "艺术家: ${audio.displayArtist}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (audio.album.isNotEmpty()) {
                        Text(
                            text = "专辑: ${audio.displayAlbum}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "时长: ${formatAudioDuration(audio.duration)}  |  大小: ${formatFileSize(audio.fileSize)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "目录: ${File(audio.path).parent?.substringAfterLast("/") ?: "未知"}",
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
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "正在检测歌词...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (hasLyrics) {
                    Text(
                        text = "检测到歌词，请选择来源：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (hasEmbedded) {
                        LyricsSourceCard(
                            title = "嵌入歌词",
                            subtitle = "点击查看歌词内容",
                            selected = selectedSource == "embedded",
                            onClick = { 
                                selectedSource = "embedded"
                                selectedFormat = recommendedFormatIndex
                            },
                            onPreview = {
                                previewLyricsContent = embeddedLyrics ?: ""
                                previewLyricsTitle = "嵌入歌词预览"
                                showLyricsPreview = true
                            }
                        )
                    }
                    
                    if (hasExternal) {
                        if (hasEmbedded) Spacer(modifier = Modifier.height(8.dp))
                        LyricsSourceCard(
                            title = "外部TTML文件",
                            subtitle = externalLyricsPath ?: "",
                            selected = selectedSource == "external",
                            onClick = { selectedSource = "external" },
                            onPreview = {
                                previewLyricsContent = externalLyrics ?: ""
                                previewLyricsTitle = "外部TTML预览"
                                showLyricsPreview = true
                            }
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = selectedSource == "embedded",
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
                                text = "请选择歌词格式：",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            formats.forEachIndexed { index, format ->
                                val isRecommended = index == recommendedFormatIndex
                                val displayText = if (isRecommended) "$format（推荐）" else format
                                
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
                                text = "未检测到歌词",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击下方按钮开始创建歌词",
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
                            onEditLyrics(externalLyrics, "TTML歌词")
                        }
                        selectedSource == "embedded" && embeddedLyrics != null -> {
                            onEditLyrics(embeddedLyrics, formats[selectedFormat])
                        }
                        hasExternal && !hasEmbedded -> {
                            onEditLyrics(externalLyrics, "TTML歌词")
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
                        isLoadingLyrics -> "正在加载..."
                        hasLyrics && selectedSource == null -> "请先选择歌词来源"
                        else -> "开始编辑歌词"
                    },
                    fontSize = 16.sp
                )
            }
            
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
                    text = "编辑歌曲元数据",
                    fontSize = 16.sp
                )
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
                Text("查看")
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
                Text("关闭")
            }
        }
    )
}

private fun loadCachedAudioFiles(context: Context, audioFiles: MutableList<AudioFile>): Boolean {
    val prefs = context.getSharedPreferences("MusicLibraryCache", Context.MODE_PRIVATE)
    val cacheJson = prefs.getString("audioFilesCache", null) ?: return false
    
    try {
        val jsonArray = JSONArray(cacheJson)
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            audioFiles.add(AudioFile.fromJson(json))
        }
        return audioFiles.isNotEmpty()
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error loading cache", e)
        return false
    }
}

private fun saveCachedAudioFiles(context: Context, audioFiles: List<AudioFile>) {
    val prefs = context.getSharedPreferences("MusicLibraryCache", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    audioFiles.forEach { audio ->
        jsonArray.put(audio.toJson())
    }
    prefs.edit().putString("audioFilesCache", jsonArray.toString()).apply()
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

    val sorted = when (sortType) {
        SortType.FILE_NAME -> {
            if (sortOrder == SortOrder.ASC) {
                audioFiles.sortedBy { it.displayTitle.lowercase() }
            } else {
                audioFiles.sortedByDescending { it.displayTitle.lowercase() }
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
    val audioExtensions = setOf("mp3", "flac", "ogg", "m4a", "wav", "aac", "wma", "ape")

    val allFiles = mutableListOf<File>()

    fun isExcluded(file: File): Boolean {
        return excludeFolders.any { excludeFolder ->
            file.absolutePath.startsWith(excludeFolder + File.separator) ||
                file.absolutePath == excludeFolder
        }
    }

    withContext(Dispatchers.IO) {
        var addedCount = 0
        var removedCount = 0
        var updatedCount = 0

        for (folder in folders) {
            val dir = File(folder)
            if (dir.exists() && dir.isDirectory) {
                scanDirectory(dir, audioExtensions, allFiles, ::isExcluded)
            }
        }

        val validPaths = allFiles.map { it.absolutePath }.toSet()
        withContext(Dispatchers.Main) {
            val removedFiles = audioFiles.filter { it.path !in validPaths }
            removedCount = removedFiles.size
            audioFiles.removeAll(removedFiles)
        }

        val total = allFiles.size
        allFiles.forEachIndexed { index, file ->
            withContext(Dispatchers.Main) {
                onProgress(index + 1, total)
            }

            try {
                val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(file.absolutePath)
                val duration = metadata.duration
                if (!excludeShortAudio || duration >= 60000) {
                    val fileSize = file.length()
                    val fileLastModified = file.lastModified()
                    var coverCachePath: String? = null
                    if (metadata.cover != null) {
                        coverCachePath = saveCoverToCache(context, file.absolutePath, metadata.cover)
                    }

                    withContext(Dispatchers.Main) {
                        val existingIndex = audioFiles.indexOfFirst { it.path == file.absolutePath }
                        if (existingIndex >= 0) {
                            val existing = audioFiles[existingIndex]
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
                            audioFiles[existingIndex] = existing.copy(
                                title = metadata.title,
                                artist = metadata.artist,
                                album = metadata.album,
                                duration = duration,
                                fileSize = fileSize,
                                lastModified = fileLastModified,
                                coverCachePath = coverCachePath ?: existing.coverCachePath,
                                year = metadata.year,
                                mediaStoreId = -1L
                            )
                            if (changed) {
                                updatedCount++
                            }
                        } else {
                            val audioFile = AudioFile(
                                path = file.absolutePath,
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
                            audioFiles.add(audioFile)
                            addedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error reading file: ${file.absolutePath}", e)
            }
        }

        val validCoverPaths = withContext(Dispatchers.Main) {
            audioFiles.mapNotNull { it.coverCachePath }.toSet()
        }
        clearOldCoverCache(context, validCoverPaths)

        withContext(Dispatchers.Main) {
            saveCachedAudioFiles(context, audioFiles)
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

        val validPaths = mediaEntries.map { it.path }.toSet()
        withContext(Dispatchers.Main) {
            val removedFiles = audioFiles.filter { it.path !in validPaths }
            removedCount += removedFiles.size
            audioFiles.removeAll(removedFiles)
        }

        val existingSnapshot = withContext(Dispatchers.Main) {
            audioFiles.associateBy { it.path }
        }
        val entriesToProcess = mediaEntries.filter { entry ->
            isNativeMediaEntryChanged(existingSnapshot[entry.path], entry)
        }

        withContext(Dispatchers.Main) {
            onProgress(0, entriesToProcess.size)
        }

        entriesToProcess.forEachIndexed { index, entry ->
            withContext(Dispatchers.Main) {
                onProgress(index + 1, entriesToProcess.size)
            }

            try {
                val file = File(entry.path)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        val beforeSize = audioFiles.size
                        audioFiles.removeAll { it.path == entry.path }
                        removedCount += (beforeSize - audioFiles.size)
                    }
                    return@forEachIndexed
                }

                val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(entry.path)
                val duration = if (metadata.duration > 0) metadata.duration else entry.duration
                if (excludeShortAudio && duration < 60000L) {
                    withContext(Dispatchers.Main) {
                        val beforeSize = audioFiles.size
                        audioFiles.removeAll { it.path == entry.path }
                        removedCount += (beforeSize - audioFiles.size)
                    }
                    return@forEachIndexed
                }

                var coverCachePath: String? = null
                if (metadata.cover != null) {
                    coverCachePath = saveCoverToCache(context, entry.path, metadata.cover)
                }

                withContext(Dispatchers.Main) {
                    val existingIndex = audioFiles.indexOfFirst { it.path == entry.path }
                    if (existingIndex >= 0) {
                        val existing = audioFiles[existingIndex]
                        val targetFileSize = if (entry.fileSize > 0) entry.fileSize else file.length()
                        val targetLastModified = if (entry.lastModified > 0) entry.lastModified else file.lastModified()
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
                        audioFiles[existingIndex] = existing.copy(
                            title = metadata.title,
                            artist = metadata.artist,
                            album = metadata.album,
                            duration = duration,
                            fileSize = targetFileSize,
                            lastModified = targetLastModified,
                            coverCachePath = coverCachePath ?: existing.coverCachePath,
                            year = metadata.year,
                            mediaStoreId = entry.mediaStoreId
                        )
                        if (changed) {
                            updatedCount++
                        }
                    } else {
                        audioFiles.add(
                            AudioFile(
                                path = entry.path,
                                title = metadata.title,
                                artist = metadata.artist,
                                album = metadata.album,
                                duration = duration,
                                fileSize = if (entry.fileSize > 0) entry.fileSize else file.length(),
                                lastModified = if (entry.lastModified > 0) entry.lastModified else file.lastModified(),
                                addedTime = System.currentTimeMillis(),
                                coverCachePath = coverCachePath,
                                year = metadata.year,
                                mediaStoreId = entry.mediaStoreId
                            )
                        )
                        addedCount++
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicLibrary", "Error syncing native media entry: ${entry.path}", e)
            }
        }

        val validCoverPaths = withContext(Dispatchers.Main) {
            audioFiles.mapNotNull { it.coverCachePath }.toSet()
        }
        clearOldCoverCache(context, validCoverPaths)

        withContext(Dispatchers.Main) {
            saveCachedAudioFiles(context, audioFiles)
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

private fun extractEmbeddedLyrics(path: String): String? {
    return com.example.LyricBox.utils.AudioMetadataReader.readLyrics(path)
}

private fun getCoverCacheDir(context: Context): File {
    val cacheDir = File(context.cacheDir, "covers")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    return cacheDir
}

private fun saveCoverToCache(context: Context, audioFilePath: String, coverData: ByteArray): String? {
    return try {
        val cacheDir = getCoverCacheDir(context)
        val fileName = audioFilePath.hashCode().toString() + ".jpg"
        val cacheFile = File(cacheDir, fileName)
        
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(coverData, 0, coverData.size, options)
        
        val targetSize = 100
        var scaleFactor = 1
        while (options.outWidth / scaleFactor / 2 >= targetSize && 
               options.outHeight / scaleFactor / 2 >= targetSize) {
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
            Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
        } else {
            null
        }
        
        if (resizedBitmap != null) {
            val outputStream = java.io.FileOutputStream(cacheFile)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            resizedBitmap.recycle()
            scaledBitmap?.recycle()
            cacheFile.absolutePath
        } else {
            cacheFile.writeBytes(coverData)
            cacheFile.absolutePath
        }
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error saving cover to cache", e)
        null
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
    } catch (e: Exception) {
        Log.e("MusicLibrary", "Error clearing old cover cache", e)
    }
}

@Composable
fun ExternalAudioScreen(
    audio: AudioFile,
    onBack: () -> Unit,
    onEditLyrics: (String?, String) -> Unit,
    onEditMetadata: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAudioOptionsDialog by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 当从编辑页面返回时，重新弹出对话框
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
            title = "音频文件",
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
                    text = "点击屏幕继续操作",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
    
    if (showAudioOptionsDialog) {
        val audioPath = audio.path
        val audioTitle = audio.displayTitle
        val audioArtist = audio.artist
        AudioOptionsDialog(
            audio = audio,
            onDismiss = {
                showAudioOptionsDialog = false
            },
            onEditLyrics = { lyricsContent, format ->
                showAudioOptionsDialog = false
                onEditLyrics(lyricsContent, format)
            },
            onEditMetadata = { path ->
                showAudioOptionsDialog = false
                onEditMetadata(path)
            }
        )
    }
}

@Composable
private fun MultiSelectActionBar(
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onClearSelection: () -> Unit,
    onRangeSelect: () -> Unit,
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
            Text("全选", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .clickable(onClick = onInvertSelection)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("反选", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .clickable(onClick = onClearSelection)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("清空", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .clickable(onClick = onRangeSelect)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("区间", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                .clickable(onClick = onExit)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("关闭", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
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
                text = "按序号区间选择",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前列表共 $maxIndex 项",
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
                        text = "起始",
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
                                        text = "请输入起始序号",
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
                    text = "至",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "结束",
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
                                        text = "请输入结束序号",
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
                    Text("取消")
                }
                Button(
                    onClick = {
                        val start = startText.toIntOrNull() ?: 1
                        val end = endText.toIntOrNull() ?: maxIndex
                        onConfirm(start, end)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确定")
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
                    contentDescription = "批量操作",
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
                        text = "批量操作",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BatchOperationMenuItem(
                        title = "批量匹配标签",
                        onClick = {
                            onShowSheetChange(false)
                            onBatchMatch()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatchOperationMenuItem(
                        title = "批量匹配歌词",
                        onClick = {
                            onShowSheetChange(false)
                            onBatchLyricMatch()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatchOperationMenuItem(
                        title = "批量重命名",
                        onClick = {
                            onShowSheetChange(false)
                            onBatchRename()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatchOperationMenuItem(
                        title = "批量编辑",
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
private fun BatchMatchConfigSheet(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onStartMatch: (BatchMatchConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    
    LaunchedEffect(fields, selectedSources, threadCount) {
        saveConfig(fields, selectedSources, threadCount)
    }
    
    LaunchedEffect(Unit) { sheetState.show() }
    
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
                .navigationBarsPadding()
        ) {
            Text(
                text = "批量匹配标签配置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "已选择 $selectedCount 个音频文件",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "匹配字段",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "补充：仅填充空字段 | 覆盖：替换所有字段",
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
                        Text("全选", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = { fields = fields.map { it.copy(enabled = !it.enabled) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    ) {
                        Text("反选", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
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
                        Text("全补充", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = { fields = fields.map { it.copy(mode = FieldMatchMode.OVERWRITE) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    ) {
                        Text("全覆盖", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
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
                                        text = field.label,
                                        fontSize = 14.sp,
                                        color = if (isSelected) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                    val modeLabel = if (field.mode == FieldMatchMode.SUPPLEMENT) "补充" else "覆盖"
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
                text = "匹配音源",
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
                text = "并发线程数：$threadCount",
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
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
                    Text("开始匹配")
                }
            }
        }
    }
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
            Text("匹配中...")
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
                Text("取消匹配", color = MaterialTheme.colorScheme.error)
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
            Text("匹配歌词中...")
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
                Text("取消匹配", color = MaterialTheme.colorScheme.error)
            }
        }
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
                    text = "匹配歌词结果",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "成功 ${result.totalSuccess} / 失败 ${result.totalFailed}",
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
                Text("关闭", fontSize = 15.sp)
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
        MatchStatus.SUCCESS -> "成功"
        MatchStatus.FAILED -> "失败"
        MatchStatus.SKIPPED -> "跳过"
        else -> "处理中"
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
                MatchStatus.SUCCESS -> "成功"
                MatchStatus.FAILED -> "失败"
                MatchStatus.SKIPPED -> "跳过"
                else -> "处理中"
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
                        text = "来自 ${batchMatchSourceShortName(item.matchSource!!)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.matchStatus == MatchStatus.SUCCESS && item.similarityScore > 0) {
                    Text(
                        text = String.format("匹配度 %.0f%%", item.similarityScore * 100),
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
                    text = "新歌词：",
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
                    text = "原有歌词：",
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
                    text = "无歌词",
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
                Text("关闭", fontSize = 15.sp)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedSources by remember { mutableStateOf(listOf(Source.QM)) }
    var matchMode by remember { mutableStateOf(LyricMatchMode.SUPPLEMENT) }
    var lyricType by remember { mutableStateOf(LyricType.VERBATIM) }
    var threadCount by remember { mutableStateOf(3) }
    var filterMetadata by remember { mutableStateOf(false) }
    var includeTranslation by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { sheetState.show() }
    
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
                .navigationBarsPadding()
        ) {
            Text(
                text = "批量匹配歌词",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "已选择 $selectedCount 个音频文件",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "歌词匹配音源",
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
                text = "歌词类型",
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
                        text = "逐字歌词",
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
                        text = "逐行歌词",
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
                text = "匹配模式",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "补充：仅填充空歌词 | 覆盖：替换所有歌词",
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
                        text = "补充",
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
                        text = "覆盖",
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
                text = "其他设置",
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
                        text = "仅保留歌词",
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
                        text = "包含翻译",
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
                text = "并发线程数：$threadCount",
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
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
                    Text("开始匹配")
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
                    text = "匹配结果",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "成功 ${result.totalSuccess} / 失败 ${result.totalFailed}",
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
                Text("关闭", fontSize = 15.sp)
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
        MatchStatus.SUCCESS -> "成功"
        MatchStatus.FAILED -> "失败"
        MatchStatus.SKIPPED -> "跳过"
        else -> "处理中"
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
                text = "尺寸: ${bitmap.width} × ${bitmap.height}",
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
                Text("关闭", fontSize = 15.sp)
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
                MatchStatus.SUCCESS -> "匹配成功"
                MatchStatus.FAILED -> "匹配失败"
                MatchStatus.SKIPPED -> "已跳过"
                else -> "未知"
            }
            val statusColor = when (item.matchStatus) {
                MatchStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                MatchStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = "状态: $statusText",
                fontSize = 13.sp,
                color = statusColor
            )
            
            if (item.error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "错误: ${item.error}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "字段变更详情",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (displayData.isEmpty() && !hasCover) {
                Text(
                    text = "无变更字段",
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
                                    text = "封面",
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
                                        text = "原",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    if (hasOriginalCover) {
                                        androidx.compose.foundation.Image(
                                            bitmap = item.originalCoverBitmap!!.asImageBitmap(),
                                            contentDescription = "原封面",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    showCoverFull = item.originalCoverBitmap
                                                    coverTitle = "原封面"
                                                }
                                        )
                                    } else {
                                        Text(
                                            text = "(无)",
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
                                        text = "新",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    androidx.compose.foundation.Image(
                                        bitmap = item.coverBitmap!!.asImageBitmap(),
                                        contentDescription = "封面",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                showCoverFull = item.coverBitmap
                                                coverTitle = "新封面"
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
                                    contentDescription = "撤销",
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
                            "title" -> "标题"
                            "artist" -> "艺术家"
                            "album" -> "专辑"
                            "year" -> "年份"
                            "trackNumber" -> "音轨号"
                            "discNumber" -> "碟号"
                            "genre" -> "风格"
                            "albumArtist" -> "专辑艺术家"
                            "composer" -> "作曲"
                            "lyricist" -> "作词"
                            "comment" -> "注释"
                            "copyrightInfo" -> "版权信息"
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
                                        text = "原",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = oldValue.ifEmpty { "(空)" },
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
                                        text = "新",
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
                                    contentDescription = "撤销",
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
                Text("关闭", fontSize = 15.sp)
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
                        item.error = "无有效搜索关键词"
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
                            item.error = "相似度不足 (${String.format("%.0f%%", bestCombinedSim * 100)} < 80%)"
                        }
                    } else {
                        item.matchStatus = MatchStatus.FAILED
                        item.error = "未找到匹配结果"
                    }
                } catch (e: Exception) {
                    item.matchStatus = MatchStatus.FAILED
                    item.error = e.message ?: "未知错误"
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
            
            val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(file.absolutePath)
            
            var coverCachePath: String? = null
            if (metadata.cover != null) {
                coverCachePath = saveCoverToCache(context, file.absolutePath, metadata.cover)
            }
            
            withContext(Dispatchers.Main) {
                val existingIndex = audioFiles.indexOfFirst { it.path == path }
                if (existingIndex >= 0) {
                    val existing = audioFiles[existingIndex]
                    val newAudioFile = existing.copy(
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.album,
                        duration = metadata.duration,
                        fileSize = file.length(),
                        lastModified = file.lastModified(),
                        coverCachePath = coverCachePath ?: existing.coverCachePath
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
        "歌曲标题" to "歌曲标题",
        "艺术家" to "艺术家",
        "专辑" to "专辑",
        "碟号" to "碟号",
        "音轨号" to "音轨号",
        "年份" to "年份",
        "专辑艺术家" to "专辑艺术家"
    )
    
    val presetTemplates = listOf(
        "[歌曲标题] - [艺术家]",
        "[艺术家] - [歌曲标题]"
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
                text = "批量重命名配置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "已选择 $selectedCount 个音频文件",
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
                    text = "文件名模板",
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
                        text = "清空",
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
                                text = "请输入文件名，例如‘[歌曲标题] - [艺术家]’",
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
                text = "快捷模板",
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
                presetTemplates.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            .clickable {
                                templateValue = androidx.compose.ui.text.input.TextFieldValue(
                                    text = preset,
                                    selection = androidx.compose.ui.text.TextRange(preset.length)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(preset, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "快捷标签",
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
                text = "艺术家分隔符",
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
                        text = if (isCustomSelected) "自定义: $artistSeparator" else "自定义",
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
                    text = "同时重命名TTML文件",
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
                    Text("取消")
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
                    Text("预览")
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
                text = "自定义艺术家分隔符",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "不能包含文件名非法字符：\\ / : * ? \" < > |",
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
                                text = "请输入分隔符，例如“／”",
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
                    Text("取消")
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
                    Text("确定")
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
                text = "重命名预览",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "共 ${previewItems.size} 个文件",
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
                    Text("取消")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认重命名")
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
                text = "原名称: ",
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
                text = "新名称: ",
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
                    text = "TTML原名称: ",
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
                    text = "TTML新名称: ",
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
            Text("重命名中...")
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
                    text = "重命名完成",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "成功 ${result.successCount} / 失败 ${result.failedCount}",
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
                    text = "失败项目",
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
                Text("关闭", fontSize = 15.sp)
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
                text = "原: ${item.oldName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "新: ${item.newName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "成功",
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
            val metadata = com.example.LyricBox.utils.AudioMetadataReader.readMetadata(audioFile.path)
            
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
                if (file.exists() && newFile != file) {
                    val renamed = file.renameTo(newFile)
                    if (renamed) {
                        successItems.add(item)
                    } else {
                        errors[item.oldName] = "重命名失败"
                    }
                } else if (file.exists() && newFile == file) {
                    successItems.add(item)
                }
                
                if (config.renameTtml && item.oldTtmlName != null && item.newTtmlName != null) {
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
        
        val lineStartTime = formatLrcTime(line.start)
        sb.append("[$lineStartTime]")
        
        for (word in line.words) {
            sb.append(word.text)
        }
        
        sb.append("\n")
        
        if (includeTranslation && translationData.isNotEmpty() && index < translationData.size) {
            val translationLine = translationData[index]
            val translationText = translationLine.words.joinToString("") { word -> word.text }
            sb.append("[$lineStartTime]$translationText\n")
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
                    
                    item.originalLyrics = extractEmbeddedLyrics(item.audioFile.path)
                    
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
                        item.error = "无有效搜索关键词"
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
                            item.error = "已有歌词，补充模式下跳过"
                        }
                    } else {
                        item.matchStatus = MatchStatus.FAILED
                        item.error = if (bestCombinedSim < 0.9f) 
                            "匹配度不足 (${String.format("%.0f%%", bestCombinedSim * 100)} < 90%)" 
                        else 
                            "未找到有效歌词"
                    }
                } catch (e: Exception) {
                    item.matchStatus = MatchStatus.FAILED
                    item.error = e.message ?: "未知错误"
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
