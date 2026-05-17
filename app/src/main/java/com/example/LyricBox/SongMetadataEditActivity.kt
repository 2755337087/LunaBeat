package com.example.LyricBox

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.provider.DocumentsContract
import android.util.Log
import android.view.ViewGroup
import java.util.Locale
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import android.os.ParcelFileDescriptor
import com.lonx.audiotag.TagLib
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.LyricBox.R
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.LyricBatchEditUtils
import com.example.LyricBox.utils.LyricExportFormat
import com.example.LyricBox.utils.LyricSaveEmbedUtils
import com.lonx.audiotag.model.AudioPicture
import com.lonx.audiotag.model.AudioTagData
import com.lonx.audiotag.rw.AudioTagReader
import com.lonx.audiotag.rw.AudioTagWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream

private const val TAG = "SongMetadataEdit"
private const val REQUEST_CODE_SEARCH_METADATA = 1001
private const val REQUEST_CODE_LYRIC_TIMING = 1002
private const val REQUEST_CODE_UCROP = 1004
private val GENRE_SPLIT_REGEX = Regex("""[;；:：、&]+""")
private const val ACCOMPANIMENT_DIR_PATH = "/storage/emulated/0/Music/.sing"
private const val ACCOMPANIMENT_PREFS = "AccompanimentStoragePrefs"
private const val ACCOMPANIMENT_TREE_URI_KEY = "music_sing_tree_uri"

private fun readAudioDurationMillis(audioPath: String): Long {
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever().apply {
            setDataSource(audioPath)
        }
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        Log.e(TAG, "Error reading audio duration for $audioPath", e)
        0L
    } finally {
        runCatching { retriever?.release() }
    }
}

data class BatchEditFieldValues(
    val titles: Set<String> = emptySet(),
    val artists: Set<String> = emptySet(),
    val albums: Set<String> = emptySet(),
    val years: Set<String> = emptySet(),
    val trackNumbers: Set<String> = emptySet(),
    val discNumbers: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val albumArtists: Set<String> = emptySet(),
    val composers: Set<String> = emptySet(),
    val lyricists: Set<String> = emptySet(),
    val comments: Set<String> = emptySet(),
    val copyrightInfos: Set<String> = emptySet(),
    val lyricsValues: Set<String> = emptySet(),
    val customFieldValues: Map<String, Set<String>> = emptyMap()
)

data class OriginalData(
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val trackNumber: String,
    val discNumber: String,
    val genre: String,
    val albumArtist: String,
    val composer: String,
    val lyricist: String,
    val comment: String,
    val copyrightInfo: String,
    val lyrics: String,
    val coverBitmap: Bitmap?,
    val customFields: Map<String, String> = emptyMap()
)

data class RedoHistory(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val genre: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val lyricist: String? = null,
    val comment: String? = null,
    val copyrightInfo: String? = null,
    val lyrics: String? = null,
    val coverBitmap: Bitmap? = null,
    val customFields: Map<String, String> = emptyMap()
)

data class ModifiedField(
    val title: Boolean = false,
    val artist: Boolean = false,
    val album: Boolean = false,
    val year: Boolean = false,
    val trackNumber: Boolean = false,
    val discNumber: Boolean = false,
    val genre: Boolean = false,
    val albumArtist: Boolean = false,
    val composer: Boolean = false,
    val lyricist: Boolean = false,
    val comment: Boolean = false,
    val copyrightInfo: Boolean = false,
    val lyrics: Boolean = false,
    val cover: Boolean = false,
    val canRedoTitle: Boolean = false,
    val canRedoArtist: Boolean = false,
    val canRedoAlbum: Boolean = false,
    val canRedoYear: Boolean = false,
    val canRedoTrackNumber: Boolean = false,
    val canRedoDiscNumber: Boolean = false,
    val canRedoGenre: Boolean = false,
    val canRedoAlbumArtist: Boolean = false,
    val canRedoComposer: Boolean = false,
    val canRedoLyricist: Boolean = false,
    val canRedoComment: Boolean = false,
    val canRedoCopyrightInfo: Boolean = false,
    val canRedoLyrics: Boolean = false,
    val canRedoCover: Boolean = false,
    val customFields: Map<String, Boolean> = emptyMap(),
    val canRedoCustomFields: Map<String, Boolean> = emptyMap()
)

private val modifiedFieldSaver: Saver<ModifiedField, Any> = mapSaver(
    save = { value ->
        mapOf(
            "title" to value.title,
            "artist" to value.artist,
            "album" to value.album,
            "year" to value.year,
            "trackNumber" to value.trackNumber,
            "discNumber" to value.discNumber,
            "genre" to value.genre,
            "albumArtist" to value.albumArtist,
            "composer" to value.composer,
            "lyricist" to value.lyricist,
            "comment" to value.comment,
            "copyrightInfo" to value.copyrightInfo,
            "lyrics" to value.lyrics,
            "cover" to value.cover,
            "canRedoTitle" to value.canRedoTitle,
            "canRedoArtist" to value.canRedoArtist,
            "canRedoAlbum" to value.canRedoAlbum,
            "canRedoYear" to value.canRedoYear,
            "canRedoTrackNumber" to value.canRedoTrackNumber,
            "canRedoDiscNumber" to value.canRedoDiscNumber,
            "canRedoGenre" to value.canRedoGenre,
            "canRedoAlbumArtist" to value.canRedoAlbumArtist,
            "canRedoComposer" to value.canRedoComposer,
            "canRedoLyricist" to value.canRedoLyricist,
            "canRedoComment" to value.canRedoComment,
            "canRedoCopyrightInfo" to value.canRedoCopyrightInfo,
            "canRedoLyrics" to value.canRedoLyrics,
            "canRedoCover" to value.canRedoCover,
            "customFields" to HashMap(value.customFields),
            "canRedoCustomFields" to HashMap(value.canRedoCustomFields)
        )
    },
    restore = { restored ->
        @Suppress("UNCHECKED_CAST")
        val customFields = (restored["customFields"] as? Map<*, *>)?.entries
            ?.mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                key to (entry.value as? Boolean ?: false)
            }?.toMap().orEmpty()
        @Suppress("UNCHECKED_CAST")
        val canRedoCustomFields = (restored["canRedoCustomFields"] as? Map<*, *>)?.entries
            ?.mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                key to (entry.value as? Boolean ?: false)
            }?.toMap().orEmpty()
        ModifiedField(
            title = restored["title"] as? Boolean ?: false,
            artist = restored["artist"] as? Boolean ?: false,
            album = restored["album"] as? Boolean ?: false,
            year = restored["year"] as? Boolean ?: false,
            trackNumber = restored["trackNumber"] as? Boolean ?: false,
            discNumber = restored["discNumber"] as? Boolean ?: false,
            genre = restored["genre"] as? Boolean ?: false,
            albumArtist = restored["albumArtist"] as? Boolean ?: false,
            composer = restored["composer"] as? Boolean ?: false,
            lyricist = restored["lyricist"] as? Boolean ?: false,
            comment = restored["comment"] as? Boolean ?: false,
            copyrightInfo = restored["copyrightInfo"] as? Boolean ?: false,
            lyrics = restored["lyrics"] as? Boolean ?: false,
            cover = restored["cover"] as? Boolean ?: false,
            canRedoTitle = restored["canRedoTitle"] as? Boolean ?: false,
            canRedoArtist = restored["canRedoArtist"] as? Boolean ?: false,
            canRedoAlbum = restored["canRedoAlbum"] as? Boolean ?: false,
            canRedoYear = restored["canRedoYear"] as? Boolean ?: false,
            canRedoTrackNumber = restored["canRedoTrackNumber"] as? Boolean ?: false,
            canRedoDiscNumber = restored["canRedoDiscNumber"] as? Boolean ?: false,
            canRedoGenre = restored["canRedoGenre"] as? Boolean ?: false,
            canRedoAlbumArtist = restored["canRedoAlbumArtist"] as? Boolean ?: false,
            canRedoComposer = restored["canRedoComposer"] as? Boolean ?: false,
            canRedoLyricist = restored["canRedoLyricist"] as? Boolean ?: false,
            canRedoComment = restored["canRedoComment"] as? Boolean ?: false,
            canRedoCopyrightInfo = restored["canRedoCopyrightInfo"] as? Boolean ?: false,
            canRedoLyrics = restored["canRedoLyrics"] as? Boolean ?: false,
            canRedoCover = restored["canRedoCover"] as? Boolean ?: false,
            customFields = customFields,
            canRedoCustomFields = canRedoCustomFields
        )
    }
)

private val stringStateMapSaver: Saver<SnapshotStateMap<String, String>, Any> = mapSaver(
    save = { value ->
        value.toMap()
    },
    restore = { restored ->
        mutableStateMapOf<String, String>().apply {
            restored.forEach { (key, value) ->
                if (value is String) {
                    this[key] = value
                }
            }
        }
    }
)

class SongMetadataEditActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val EXTRA_MEDIA_STORE_ID = "media_store_id"
        const val EXTRA_IS_BATCH_EDIT = "is_batch_edit"
        const val EXTRA_SELECTED_PATHS = "selected_paths"
        const val REQUEST_CODE_LYRIC_TIMING = 1002
        const val REQUEST_CODE_VERBATIM_LYRICS = 1003
        const val REQUEST_CODE_UCROP = 1004
    }
    
    private var audioPath by mutableStateOf<String?>(null)
    private var mediaStoreId by mutableStateOf(-1L)
    private var selectedPaths by mutableStateOf<ArrayList<String>?>(null)
    private var isBatchEdit by mutableStateOf(false)
    private var searchResultData by mutableStateOf<android.content.Intent?>(null)
    private var hasUnsavedChangesState by mutableStateOf(false)
    private var showExitConfirmDialog by mutableStateOf(false)
    private var importedLyrics by mutableStateOf<String?>(null)
    private var lyricTimingReturnNonce by mutableStateOf(0L)
    private var croppedBitmap by mutableStateOf<Bitmap?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 优先使用直接传入的路径
        audioPath = intent.getStringExtra(EXTRA_AUDIO_PATH)
        mediaStoreId = intent.getLongExtra(EXTRA_MEDIA_STORE_ID, -1L)
        isBatchEdit = intent.getBooleanExtra(EXTRA_IS_BATCH_EDIT, false)
        selectedPaths = intent.getStringArrayListExtra(EXTRA_SELECTED_PATHS)
        
        // 如果没有直接传入路径，尝试从外部 Intent 解析
        if (audioPath == null) {
            val externalPath = handleExternalIntent(intent)
            if (externalPath != null) {
                audioPath = externalPath
            }
        }
        
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChangesState) {
                    showExitConfirmDialog = true
                } else {
                    isEnabled = false
                    finish()
                }
            }
        })
        
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    SongMetadataEditScreen(
                        audioPath = audioPath,
                        mediaStoreId = mediaStoreId,
                        isBatchEdit = isBatchEdit,
                        selectedPaths = selectedPaths,
                        onCheckUnsavedChanges = { hasUnsavedChanges ->
                            hasUnsavedChangesState = hasUnsavedChanges
                            if (hasUnsavedChanges) {
                                showExitConfirmDialog = true
                            } else {
                                finish()
                            }
                        },
                        onUnsavedChangesChanged = { hasUnsavedChanges ->
                            hasUnsavedChangesState = hasUnsavedChanges
                        },
                        onDataSaved = {
                            hasUnsavedChangesState = false
                            setResult(
                                RESULT_OK,
                                Intent().apply {
                                    putExtra(EXTRA_AUDIO_PATH, audioPath)
                                    putExtra(EXTRA_MEDIA_STORE_ID, mediaStoreId)
                                }
                            )
                            AudioMetadataUpdateBus.notifyPathUpdated(audioPath)
                        },
                        onSearchMetadata = { keyword, coverOnly ->
                            val intent = Intent(this, AudioMetadataSearchActivity::class.java).apply {
                                putExtra("autoSearchKeyword", keyword)
                                putExtra("coverOnly", coverOnly)
                            }
                            startActivityForResult(intent, REQUEST_CODE_SEARCH_METADATA)
                        },
                        onOpenLyricTiming = { lyricsContent, lyricsFormat ->
                            val intent = Intent(this, LyricTimingActivity::class.java).apply {
                                putExtra("audioPath", audioPath)
                                putExtra("lyricsContent", lyricsContent)
                                putExtra("sourceTitle", audioPath?.let { java.io.File(it).nameWithoutExtension } ?: "")
                                putExtra("sourceArtist", "")
                                putExtra("lyricsFormat", lyricsFormat)
                                putExtra(LyricTimingActivity.EXTRA_MEDIA_STORE_ID, mediaStoreId)
                            }
                            startActivityForResult(intent, REQUEST_CODE_LYRIC_TIMING)
                        },
                        onOpenVerbatimLyricsSearch = { keyword ->
                            val intent = Intent(this, VerbatimLyricsActivity::class.java).apply {
                                putExtra("autoSearchKeyword", keyword)
                            }
                            startActivityForResult(intent, REQUEST_CODE_VERBATIM_LYRICS)
                        },
                        onStartCrop = { uri -> startCrop(uri) },
                        croppedBitmap = croppedBitmap,
                        onCroppedBitmapConsumed = { croppedBitmap = null },
                        onExit = {
                            finish()
                        },
                        searchResult = searchResultData,
                        onSearchResultConsumed = { searchResultData = null },
                        importedLyrics = importedLyrics,
                        onImportedLyricsConsumed = { importedLyrics = null },
                        lyricTimingReturnNonce = lyricTimingReturnNonce,
                        modifier = Modifier.padding(paddingValues)
                    )
                    
                    if (showExitConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitConfirmDialog = false },
                            title = { Text("提示") },
                            text = { Text("存在未保存的数据，是否放弃修改？") },
                            confirmButton = {
                                Button(onClick = { 
                                    showExitConfirmDialog = false
                                }) {
                                    Text("继续编辑")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { 
                                    showExitConfirmDialog = false
                                    finish()
                                }) {
                                    Text("放弃修改")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SEARCH_METADATA && resultCode == RESULT_OK) {
            searchResultData = data
        } else if (requestCode == REQUEST_CODE_LYRIC_TIMING) {
            lyricTimingReturnNonce = System.currentTimeMillis()
        } else if (requestCode == REQUEST_CODE_VERBATIM_LYRICS && resultCode == RESULT_OK) {
            val lyricsContent = data?.getStringExtra("lyricsContent")
            if (lyricsContent != null) {
                importedLyrics = lyricsContent
            }
        } else if (requestCode == REQUEST_CODE_UCROP && resultCode == RESULT_OK && data != null) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(data)
            if (resultUri != null) {
                val bitmap = loadBitmapFromUriSync(this, resultUri)
                if (bitmap != null) {
                    croppedBitmap = bitmap
                }
            }
        } else if (requestCode == REQUEST_CODE_UCROP && resultCode == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
            data?.let { 
                val cropError = com.yalantis.ucrop.UCrop.getError(it)
                Log.e(TAG, "Crop error", cropError)
            }
        }
    }
    
    fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_cover.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))
        
        val options = com.yalantis.ucrop.UCrop.Options()
        options.setToolbarTitle("裁剪封面")
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        options.setCompressionQuality(100)
        options.withAspectRatio(1f, 1f)
        options.setFreeStyleCropEnabled(true)
        
        // 使用主题色美化裁剪界面
        val primaryColor = android.graphics.Color.parseColor("#6650a4")
        options.setToolbarColor(primaryColor)
        options.setToolbarWidgetColor(android.graphics.Color.WHITE)
        options.setActiveControlsWidgetColor(primaryColor)
        
        com.yalantis.ucrop.UCrop.of(uri, destinationUri)
            .withOptions(options)
            .start(this, REQUEST_CODE_UCROP)
    }
    
    private fun loadBitmapFromUriSync(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from uri", e)
            null
        }
    }
    
    private fun handleExternalIntent(intent: Intent): String? {
        Log.d(TAG, "handleExternalIntent start: action=${intent.action}, type=${intent.type}, data=${intent.data}")
        
        // 处理 ACTION_SEND
        if (intent.action == Intent.ACTION_SEND) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null) {
                Log.d(TAG, "ACTION_SEND uri=$uri")
                val resolved = getRealPathFromUri(uri)
                Log.d(TAG, "ACTION_SEND resolved=$resolved")
                return resolved
            }
        }
        
        // 检查 Intent 的 data（用于 ACTION_VIEW 和 ACTION_EDIT）
        val uri = intent.data ?: return null
        Log.d(TAG, "fallback intent.data uri=$uri")
        
        // 尝试从 Uri 获取真实路径
        val resolved = getRealPathFromUri(uri)
        Log.d(TAG, "intent.data resolved=$resolved")
        return resolved
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        Log.d(TAG, "getRealPathFromUri start: $uri")
        // file:// 直接取本地路径
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val filePath = uri.path
            if (isExistingFilePath(filePath)) {
                Log.d(TAG, "resolved by file scheme path: $filePath")
                return filePath
            }
        }

        // 直接 path（部分机型 ACTION_VIEW 会给出）
        uri.path?.let { rawPath ->
            val decodedPath = Uri.decode(rawPath)
            if (isExistingFilePath(decodedPath)) {
                Log.d(TAG, "resolved by decoded raw path: $decodedPath")
                return decodedPath
            }
        }

        if (!uri.scheme.equals("content", ignoreCase = true)) {
            Log.d(TAG, "unsupported scheme for resolution: ${uri.scheme}")
            return null
        }

        resolvePathFromFileProviderUri(uri)?.let { providerDerivedPath ->
            if (isExistingFilePath(providerDerivedPath)) {
                Log.d(TAG, "resolved by FileProvider derived path: $providerDerivedPath")
                return providerDerivedPath
            }
        }

        // 优先处理 DocumentUri（更接近真实路径）
        resolvePathFromDocumentUri(uri)?.let { documentPath ->
            if (isExistingFilePath(documentPath)) {
                Log.d(TAG, "resolved by DocumentUri: $documentPath")
                return documentPath
            }
        }

        // 兜底查询
        getPathFromDataColumn(uri)?.let { dataPath ->
            if (isExistingFilePath(dataPath)) {
                Log.d(TAG, "resolved by _data column: $dataPath")
                return dataPath
            }
        }
        getPathFromMediaStore(uri)?.let { mediaPath ->
            if (isExistingFilePath(mediaPath)) {
                Log.d(TAG, "resolved by MediaStore lookup: $mediaPath")
                return mediaPath
            }
        }
        getPathFromDocumentsProvider(uri)?.let { providerPath ->
            if (isExistingFilePath(providerPath)) {
                Log.d(TAG, "resolved by legacy DocumentsProvider: $providerPath")
                return providerPath
            }
        }

        // 解析不到真实路径时，最后兜底复制到缓存，保证可继续处理
        Log.w(TAG, "无法获取外部音频真实路径，回退到缓存文件: $uri")
        val fallbackPath = copyContentUriToCache(uri)
        Log.d(TAG, "fallback cache copy path=$fallbackPath")
        return fallbackPath
    }

    private fun isExistingFilePath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching { File(path).exists() && File(path).isFile }.getOrDefault(false)
    }

    private fun resolvePathFromFileProviderUri(uri: Uri): String? {
        val authority = uri.authority ?: return null
        if (!authority.contains("fileprovider", ignoreCase = true)) {
            return null
        }
        val lowerAuthority = authority.lowercase(Locale.ROOT)
        val fileProviderSuffix = ".fileprovider"
        val suffixIndex = lowerAuthority.indexOf(fileProviderSuffix)
        val senderPackage = if (suffixIndex > 0) {
            authority.substring(0, suffixIndex)
        } else {
            authority
        }.takeIf { it.isNotBlank() } ?: return null
        val segments = uri.pathSegments
        if (segments.isEmpty()) return null
        val rootName = segments.first().lowercase(Locale.ROOT)
        val relativePath = segments.drop(1).joinToString("/")
        if (relativePath.isBlank()) return null

        val externalRoot = android.os.Environment.getExternalStorageDirectory().absolutePath
        val candidates = when (rootName) {
            "external_files", "external-files", "externalfiles", "external_file", "external-file" -> listOf(
                "$externalRoot/$relativePath",
                "$externalRoot/Android/data/$senderPackage/files/$relativePath"
            )
            "external_cache", "external-cache", "externalcache" -> listOf(
                "$externalRoot/Android/data/$senderPackage/cache/$relativePath"
            )
            "external_media", "external-media", "externalmedia" -> listOf(
                "$externalRoot/Android/media/$senderPackage/$relativePath"
            )
            "external_path", "external-path", "external", "sdcard", "root" -> listOf(
                "$externalRoot/$relativePath"
            )
            "files", "internal_files", "internal-files" -> listOf(
                "/data/user/0/$senderPackage/files/$relativePath"
            )
            "cache", "internal_cache", "internal-cache" -> listOf(
                "/data/user/0/$senderPackage/cache/$relativePath"
            )
            else -> emptyList()
        }
        if (candidates.isEmpty()) {
            return null
        }
        candidates.forEach { candidate ->
            val exists = isExistingFilePath(candidate)
            if (exists) {
                return candidate
            }
        }
        return null
    }

    private fun resolvePathFromDocumentUri(uri: Uri): String? {
        return try {
            if (!android.provider.DocumentsContract.isDocumentUri(this, uri)) {
                return null
            }
            val docId = android.provider.DocumentsContract.getDocumentId(uri)
            when (uri.authority) {
                "com.android.externalstorage.documents" -> {
                    val split = docId.split(":", limit = 2)
                    if (split.size < 2) return null
                    val volume = split[0]
                    val relativePath = split[1]
                    val candidate = if (volume.equals("primary", ignoreCase = true)) {
                        "${android.os.Environment.getExternalStorageDirectory()}/$relativePath"
                    } else {
                        "/storage/$volume/$relativePath"
                    }
                    candidate
                }
                "com.android.providers.downloads.documents" -> {
                    when {
                        docId.startsWith("raw:") -> docId.removePrefix("raw:")
                        docId.startsWith("/storage/") -> docId
                        docId.toLongOrNull() != null -> {
                            val id = docId.toLong()
                            val publicUri = android.content.ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                id
                            )
                            getPathFromDataColumn(publicUri) ?: run {
                                val myUri = android.content.ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/my_downloads"),
                                    id
                                )
                                getPathFromDataColumn(myUri)
                            }
                        }
                        else -> null
                    }
                }
                "com.android.providers.media.documents" -> {
                    val split = docId.split(":")
                    if (split.size < 2) return null
                    val type = split[0]
                    val id = split[1]
                    val contentUri = when (type) {
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    } ?: return null
                    val selection = "${MediaStore.MediaColumns._ID}=?"
                    val selectionArgs = arrayOf(id)
                    queryDataColumn(contentUri, selection, selectionArgs)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving document uri path: $uri", e)
            null
        }
    }

    private fun queryDataColumn(
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): String? {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (columnIndex >= 0) {
                        cursor.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun copyContentUriToCache(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = getFileName(uri)?.replace('/', '_')?.replace('\\', '_')
                    ?: "temp_audio_${System.currentTimeMillis()}"
                val tempFile = File(cacheDir, fileName)
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                tempFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file from content URI", e)
            null
        }
    }

    private fun getPathFromDataColumn(uri: Uri): String? {
        return queryDataColumn(uri)
    }

    private fun getPathFromMediaStore(uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    if (columnIndex >= 0) {
                        cursor.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getPathFromDocumentsProvider(uri: Uri): String? {
        return getPathFromDataColumn(uri)
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMetadataEditScreen(
    audioPath: String?,
    mediaStoreId: Long,
    isBatchEdit: Boolean,
    selectedPaths: ArrayList<String>?,
    onCheckUnsavedChanges: (Boolean) -> Unit,
    onUnsavedChangesChanged: (Boolean) -> Unit,
    onDataSaved: () -> Unit,
    onSearchMetadata: (String, Boolean) -> Unit,
    onOpenLyricTiming: (String?, String) -> Unit,
    onOpenVerbatimLyricsSearch: (String) -> Unit,
    onStartCrop: (Uri) -> Unit,
    croppedBitmap: Bitmap?,
    onCroppedBitmapConsumed: () -> Unit,
    onExit: () -> Unit,
    searchResult: Intent?,
    onSearchResultConsumed: () -> Unit,
    importedLyrics: String?,
    onImportedLyricsConsumed: () -> Unit,
    lyricTimingReturnNonce: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isCopyingAccompaniment by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showAccompanimentCopiedDialog by remember { mutableStateOf(false) }
    var showAccompanimentDirectoryAuthDialog by remember { mutableStateOf(false) }
    var showAccompanimentDirectoryAuthResultDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showCoverPreview by remember { mutableStateOf(false) }
    var previewType by remember { mutableStateOf("image") }
    var showRemoveImageConfirm by remember { mutableStateOf(false) }
    var showRemoveVideoConfirm by remember { mutableStateOf(false) }
    var showRemoveAccompanimentConfirm by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showBatchProgressDialog by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf(0 to 0) }
    var isBatchCancelled by remember { mutableStateOf(false) }
    var showBatchCompleteDialog by remember { mutableStateOf(false) }
    
    var showLyricsEditConfirmDialog by remember { mutableStateOf(false) }
    var showLyricsSelectionSheet by remember { mutableStateOf(false) }
    var showLyricsMoreMenu by remember { mutableStateOf(false) }
    var lyricsMoreMenuAnchor by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    var showShiftTimestampDialog by remember { mutableStateOf(false) }
    var shiftTimestampInput by remember { mutableStateOf("0") }
    var showCoverSelectionSheet by remember { mutableStateOf(false) }
    var showMusicLibraryCoverSheet by remember { mutableStateOf(false) }
    var musicLibraryAudioFiles by remember { mutableStateOf<List<Any>?>(null) }
    var musicLibrarySearchQuery by remember { mutableStateOf("") }
    
    var tagData by remember { mutableStateOf<AudioTagData?>(null) }
    var originalData by remember { mutableStateOf<OriginalData?>(null) }
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalCoverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var coverChanged by remember { mutableStateOf(false) }
    var coverRemoved by remember { mutableStateOf(false) }
    var videoCoverPath by remember { mutableStateOf<String?>(null) }
    var hasVideoCover by remember { mutableStateOf(false) }
    
    var modifiedField by rememberSaveable(stateSaver = modifiedFieldSaver) { mutableStateOf(ModifiedField()) }
    var redoHistory by remember { mutableStateOf(RedoHistory()) }
    var batchEditFieldValues by remember { mutableStateOf(BatchEditFieldValues()) }
    var showFieldSelectionSheet by remember { mutableStateOf(false) }
    var currentSelectionField by remember { mutableStateOf("") }
    
    // Crop related - handle cropped bitmap
    LaunchedEffect(croppedBitmap) {
        croppedBitmap?.let { bitmap ->
            coverBitmap = bitmap
            coverChanged = true
            coverRemoved = false
            modifiedField = modifiedField.copy(cover = true, canRedoCover = false)
            onCroppedBitmapConsumed()
        }
    }
    
    fun saveBitmapToTempUri(bitmap: Bitmap): Uri? {
        return try {
            val tempFile = File(context.cacheDir, "temp_cover.jpg")
            val fos = java.io.FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to temp file", e)
            null
        }
    }
    
    val KEEP = "[KEEP]"
    var hasInitializedEditableFields by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(false) }
    var title by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var artist by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var album by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var year by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var trackNumber by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var discNumber by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var genre by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var albumArtist by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var composer by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var lyricist by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var comment by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var copyright by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var lyrics by rememberSaveable(audioPath, isBatchEdit) { mutableStateOf(if (isBatchEdit) KEEP else "") }
    var accompanimentPath by remember(audioPath) { mutableStateOf<String?>(null) }
    var copiedAccompanimentPath by remember { mutableStateOf<String?>(null) }
    var accompanimentSourceUri by remember { mutableStateOf<Uri?>(null) }
    var accompanimentDirectoryAuthResultMessage by remember { mutableStateOf("") }
    
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val autoDetectEmbeddedLyricsType = remember { prefs.getBoolean("autoDetectEmbeddedLyricsType", false) }
    val cachedLyricsDialogAudio = remember(audioPath) {
        audioPath?.let { currentPath ->
            loadMusicLibraryCache(context).firstOrNull { it.path == currentPath }
        }
    }
    val resolvedAudioDuration by produceState(
        initialValue = cachedLyricsDialogAudio?.duration?.takeIf { it > 0L } ?: 0L,
        key1 = audioPath,
        key2 = cachedLyricsDialogAudio?.duration
    ) {
        val cachedDuration = cachedLyricsDialogAudio?.duration?.takeIf { it > 0L } ?: 0L
        if (cachedDuration > 0L) {
            value = cachedDuration
        } else {
            val currentPath = audioPath
            value = if (!currentPath.isNullOrBlank()) {
                withContext(Dispatchers.IO) { readAudioDurationMillis(currentPath) }
            } else {
                0L
            }
        }
    }
    val lyricsDialogAudio = remember(
        audioPath,
        title,
        artist,
        album,
        year,
        tagData?.title,
        tagData?.artist,
        tagData?.album,
        tagData?.date,
        cachedLyricsDialogAudio,
        resolvedAudioDuration
    ) {
        audioPath?.let { currentPath ->
            val file = File(currentPath)
            fun metadataValue(currentValue: String, loadedValue: String?, cachedValue: String): String {
                return if (currentValue != KEEP) {
                    currentValue
                } else {
                    loadedValue?.takeIf { it.isNotBlank() } ?: cachedValue
                }
            }

            AudioFile(
                path = currentPath,
                title = metadataValue(title, tagData?.title, cachedLyricsDialogAudio?.title.orEmpty()),
                artist = metadataValue(artist, tagData?.artist, cachedLyricsDialogAudio?.artist.orEmpty()),
                album = metadataValue(album, tagData?.album, cachedLyricsDialogAudio?.album.orEmpty()),
                duration = resolvedAudioDuration,
                fileSize = cachedLyricsDialogAudio?.fileSize?.takeIf { it > 0L } ?: file.length(),
                lastModified = cachedLyricsDialogAudio?.lastModified?.takeIf { it > 0L } ?: file.lastModified(),
                addedTime = cachedLyricsDialogAudio?.addedTime ?: System.currentTimeMillis(),
                coverCachePath = cachedLyricsDialogAudio?.coverCachePath?.takeIf { it.isNotBlank() },
                year = metadataValue(year, tagData?.date, cachedLyricsDialogAudio?.year.orEmpty())
            )
        }
    }
    val fieldConfig = remember { MetadataFieldConfigStore.load(prefs) }
    val visibleFieldKeys = remember { fieldConfig.visibleFieldKeys }
    val enabledFields = remember { fieldConfig.visibleCustomFieldNames }
    
    // 自定义字段状态
    val customFieldValues = rememberSaveable(audioPath, isBatchEdit, saver = stringStateMapSaver) { mutableStateMapOf<String, String>() }
    val originalCustomFieldValues = rememberSaveable(audioPath, isBatchEdit, saver = stringStateMapSaver) { mutableStateMapOf<String, String>() }
    
    var audioFileName by remember { mutableStateOf("") }

    val retrySaveActionRef = remember { mutableStateOf<(() -> Unit)?>(null) }
    val recoverableWritePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            retrySaveActionRef.value?.invoke()
        }
    }

    LaunchedEffect(audioPath) {
        accompanimentPath = withContext(Dispatchers.IO) {
            getAccompanimentPathForAudio(audioPath)
        }
    }
    
    fun hasUnsavedChanges(): Boolean {
        if (isBatchEdit) {
            val hasCustomChanges = enabledFields.any { field ->
                customFieldValues[field] != KEEP
            }
            return (title != KEEP) || (artist != KEEP) || (album != KEEP) ||
                   (year != KEEP) || (trackNumber != KEEP) || (discNumber != KEEP) ||
                   (genre != KEEP) || (albumArtist != KEEP) || (composer != KEEP) ||
                   (lyricist != KEEP) || (comment != KEEP) || (copyright != KEEP) ||
                   (lyrics != KEEP) || modifiedField.cover || hasCustomChanges
        }
        val hasCustomChanges = enabledFields.any { field ->
            modifiedField.customFields[field] == true
        }
        return modifiedField.title || modifiedField.artist || modifiedField.album || 
               modifiedField.year || modifiedField.trackNumber || modifiedField.discNumber || 
               modifiedField.genre || modifiedField.albumArtist || modifiedField.composer || 
               modifiedField.lyricist || modifiedField.comment || modifiedField.copyrightInfo || 
               modifiedField.lyrics || modifiedField.cover || hasCustomChanges
    }

    fun updateLyricsWithModifiedState(newLyrics: String) {
        lyrics = newLyrics
        modifiedField = if (isBatchEdit) {
            modifiedField.copy(lyrics = newLyrics != KEEP, canRedoLyrics = false)
        } else {
            modifiedField.copy(lyrics = newLyrics != originalData?.lyrics, canRedoLyrics = false)
        }
    }

    fun buildLyricsSearchKeyword(): String {
        return if (title.isNotEmpty() && artist.isNotEmpty() && title != KEEP && artist != KEEP) {
            "$title $artist"
        } else if (title.isNotEmpty() && title != KEEP) {
            title
        } else if (artist.isNotEmpty() && artist != KEEP) {
            artist
        } else {
            ""
        }
    }

    fun buildMetadataSearchKeyword(): String {
        val normalizedTitle = title.takeIf { it.isNotBlank() && it != KEEP }
        val normalizedArtist = artist.takeIf { it.isNotBlank() && it != KEEP }
        if (normalizedTitle != null && normalizedArtist != null) {
            return "$normalizedTitle $normalizedArtist"
        }
        return audioFileName.takeIf { it.isNotBlank() }
            ?: audioPath?.let { File(it).nameWithoutExtension }
            .orEmpty()
    }

    fun openLyricTimingByPreference() {
        val hasExternalTtmlLyrics = audioPath?.let { currentPath ->
            runCatching {
                val audioFile = File(currentPath)
                val ttmlFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.ttml")
                ttmlFile.exists() && ttmlFile.length() > 0L
            }.getOrDefault(false)
        } ?: false

        if (autoDetectEmbeddedLyricsType && !hasExternalTtmlLyrics) {
            val lyricsContent = lyrics.takeIf { it.isNotBlank() && it != KEEP }
            val detectedFormat = detectLyricsFormat(lyricsContent ?: "")
            val formatLabel = when (detectedFormat) {
                3 -> "TTML歌词"
                2 -> "增强LRC/ELRC歌词"
                1 -> "LRC逐行/逐字歌词"
                else -> "纯文本歌词"
            }
            onOpenLyricTiming(lyricsContent, formatLabel)
        } else {
            showLyricsSelectionSheet = true
        }
    }

    fun parseLyricsForTools(): Pair<Int, List<LyricLine>>? {
        if (lyrics.isBlank()) {
            errorMessage = "歌词为空，无法执行此操作"
            showErrorDialog = true
            return null
        }
        val detectedFormat = detectLyricsFormat(lyrics)
        if (detectedFormat == 0) {
            errorMessage = "当前为纯文本歌词，无法执行此时间轴相关操作"
            showErrorDialog = true
            return null
        }
        return try {
            val parsedLines = when (detectedFormat) {
                1 -> LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, lyrics).lyricLines
                2 -> LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, lyrics).lyricLines
                3 -> LyricParsingUtils.parseByType(LyricParseType.TTML, lyrics).lyricLines
                else -> emptyList()
            }
            if (parsedLines.isEmpty()) {
                errorMessage = "歌词解析失败，无法执行此操作"
                showErrorDialog = true
                null
            } else {
                detectedFormat to parsedLines
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse lyrics for tools", e)
            errorMessage = "歌词解析失败：${e.message ?: "未知错误"}"
            showErrorDialog = true
            null
        }
    }

    fun sourceLyricsExportFormat(): LyricExportFormat {
        val hasMultipleTimestampsInLine = lyrics.lines().any { line ->
            Regex("""\[\d{2}:\d{2}\.\d{2,3}\]""").findAll(line).count() > 1
        }
        return if (hasMultipleTimestampsInLine) {
            LyricExportFormat.LRC_WORD
        } else {
            LyricExportFormat.LRC_LINE
        }
    }

    fun saveLyricsInFormat(
        lyricLines: List<LyricLine>,
        targetFormat: LyricExportFormat
    ): String {
        return LyricSaveEmbedUtils.buildLyricsByFormat(
            format = targetFormat,
            lyricLines = lyricLines,
            showLineEndTime = false,
            showDuet = true,
            creators = emptyList()
        )
    }

    fun trimTrailingBlankLines(text: String): String {
        return text.trimEnd('\r', '\n')
    }

    fun convertLyricsFormat(targetFormat: LyricExportFormat) {
        val parsed = parseLyricsForTools() ?: return
        val converted = saveLyricsInFormat(parsed.second, targetFormat)
        updateLyricsWithModifiedState(converted)
    }

    fun shiftLyricsTimestamps(shiftMs: Long) {
        val parsed = parseLyricsForTools() ?: return
        val shifted = LyricBatchEditUtils.shiftTimestamps(
            parsed.second,
            parsed.second.indices.toSet(),
            shiftMs
        )
        val sourceFormat = if (parsed.first == 1) sourceLyricsExportFormat() else when (parsed.first) {
            2 -> LyricExportFormat.ENHANCED_LRC
            3 -> LyricExportFormat.TTML
            else -> LyricExportFormat.LRC_WORD
        }
        updateLyricsWithModifiedState(saveLyricsInFormat(shifted, sourceFormat))
    }

    fun convertLyricsToSimplified() {
        val detectedFormat = detectLyricsFormat(lyrics)
        if (detectedFormat == 0) {
            updateLyricsWithModifiedState(LyricBatchEditUtils.toSimplifiedText(lyrics))
            return
        }
        val parsed = parseLyricsForTools() ?: return
        val converted = LyricBatchEditUtils.convertToSimplified(
            parsed.second,
            parsed.second.indices.toSet()
        )
        val sourceFormat = if (parsed.first == 1) sourceLyricsExportFormat() else when (parsed.first) {
            2 -> LyricExportFormat.ENHANCED_LRC
            3 -> LyricExportFormat.TTML
            else -> LyricExportFormat.LRC_WORD
        }
        updateLyricsWithModifiedState(saveLyricsInFormat(converted, sourceFormat))
    }

    fun deleteLyricsEmptyLines() {
        val detectedFormat = detectLyricsFormat(lyrics)
        if (detectedFormat == 0) {
            val cleaned = lyrics.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && it != "//" }
                .joinToString("\n")
            updateLyricsWithModifiedState(cleaned)
            return
        }
        val parsed = parseLyricsForTools() ?: return
        val cleaned = LyricBatchEditUtils.removeEmptyLines(parsed.second)
        val sourceFormat = if (parsed.first == 1) sourceLyricsExportFormat() else when (parsed.first) {
            2 -> LyricExportFormat.ENHANCED_LRC
            3 -> LyricExportFormat.TTML
            else -> LyricExportFormat.LRC_WORD
        }
        updateLyricsWithModifiedState(trimTrailingBlankLines(saveLyricsInFormat(cleaned, sourceFormat)))
    }

    fun formatLyricsTimeline() {
        val parsed = parseLyricsForTools() ?: return
        val formatted = LyricBatchEditUtils.formatTimeline(parsed.second)
        val sourceFormat = if (parsed.first == 1) sourceLyricsExportFormat() else when (parsed.first) {
            2 -> LyricExportFormat.ENHANCED_LRC
            3 -> LyricExportFormat.TTML
            else -> LyricExportFormat.LRC_WORD
        }
        updateLyricsWithModifiedState(trimTrailingBlankLines(saveLyricsInFormat(formatted, sourceFormat)))
    }
    
    fun saveAllData() {
        if (isBatchEdit && selectedPaths != null) {
            // 批量编辑模式
            scope.launch {
                isBatchCancelled = false
                showBatchProgressDialog = true
                batchProgress = 0 to selectedPaths!!.size
                
                var successCount = 0
                var needPermission = false
                var recoverableIntentSender: IntentSender? = null
                
                for ((index, filePath) in selectedPaths!!.withIndex()) {
                    if (isBatchCancelled) {
                        break
                    }
                    
                    // 获取该文件的原始封面数据用于处理封面移除
                    var oldCoverData: ByteArray? = null
                    try {
                        val file = File(filePath)
                        if (file.exists()) {
                            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                            val data = AudioTagReader.read(pfd, true)
                            pfd.close()
                            oldCoverData = data.pictures.firstOrNull()?.data
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading old cover data", e)
                    }
                    
                    // 构建自定义字段和保持标志
                    val customFieldsMap = mutableMapOf<String, String>()
                    val customFieldKeepMap = mutableMapOf<String, Boolean>()
                    enabledFields.forEach { field ->
                        customFieldsMap[field] = if (customFieldValues[field] != KEEP) customFieldValues[field] ?: "" else ""
                        customFieldKeepMap[field] = customFieldValues[field] == KEEP
                    }
                    
                    val result = saveMetadata(
                        context = context,
                        filePath = filePath,
                        mediaStoreId = -1L,
                        title = if (title != KEEP) title else "",
                        artist = if (artist != KEEP) artist else "",
                        album = if (album != KEEP) album else "",
                        year = if (year != KEEP) year else "",
                        trackNumber = if (trackNumber != KEEP) trackNumber else "",
                        discNumber = if (discNumber != KEEP) discNumber else "",
                        genre = if (genre != KEEP) genre else "",
                        albumArtist = if (albumArtist != KEEP) albumArtist else "",
                        composer = if (composer != KEEP) composer else "",
                        lyricist = if (lyricist != KEEP) lyricist else "",
                        comment = if (comment != KEEP) comment else "",
                        copyright = if (copyright != KEEP) copyright else "",
                        lyrics = if (lyrics != KEEP) lyrics else "",
                        coverBitmap = if (modifiedField.cover && coverChanged) coverBitmap else null,
                        coverRemoved = if (modifiedField.cover) coverRemoved else false,
                        oldCoverData = oldCoverData,
                        keepFields = KEEP,
                        titleKeep = title == KEEP,
                        artistKeep = artist == KEEP,
                        albumKeep = album == KEEP,
                        yearKeep = year == KEEP,
                        trackNumberKeep = trackNumber == KEEP,
                        discNumberKeep = discNumber == KEEP,
                        genreKeep = genre == KEEP,
                        albumArtistKeep = albumArtist == KEEP,
                        composerKeep = composer == KEEP,
                        lyricistKeep = lyricist == KEEP,
                        commentKeep = comment == KEEP,
                        copyrightKeep = copyright == KEEP,
                        lyricsKeep = lyrics == KEEP,
                        coverKeep = !modifiedField.cover,
                        customFields = customFieldsMap,
                        customFieldKeep = customFieldKeepMap
                    )
                    
                    if (result.success) {
                        successCount++
                    } else if (result.needPermission) {
                        needPermission = true
                    } else if (result.recoverableIntentSender != null) {
                        recoverableIntentSender = result.recoverableIntentSender
                        break
                    }
                    
                    batchProgress = (index + 1) to selectedPaths!!.size
                }
                
                showBatchProgressDialog = false
                
                if (needPermission) {
                    showPermissionDialog = true
                } else if (recoverableIntentSender != null) {
                    recoverableWritePermissionLauncher.launch(
                        IntentSenderRequest.Builder(recoverableIntentSender!!).build()
                    )
                } else if (!isBatchCancelled) {
                    showBatchCompleteDialog = true
                    onDataSaved()
                }
            }
        } else {
            // 普通编辑模式
            scope.launch {
                isSaving = true
                // 构建自定义字段和保持标志
                val customFieldsMap = mutableMapOf<String, String>()
                val customFieldKeepMap = mutableMapOf<String, Boolean>()
                enabledFields.forEach { field ->
                    customFieldsMap[field] = customFieldValues[field] ?: ""
                    customFieldKeepMap[field] = false // 普通编辑模式总是更新
                }
                
                val result = saveMetadata(
                    context = context,
                    filePath = audioPath ?: "",
                    mediaStoreId = mediaStoreId,
                    title = title,
                    artist = artist,
                    album = album,
                    year = year,
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    genre = genre,
                    albumArtist = albumArtist,
                    composer = composer,
                    lyricist = lyricist,
                    comment = comment,
                    copyright = copyright,
                    lyrics = lyrics,
                    coverBitmap = if (coverChanged) coverBitmap else null,
                    coverRemoved = coverRemoved,
                    oldCoverData = tagData?.pictures?.firstOrNull()?.data,
                    coverKeep = !modifiedField.cover,
                    customFields = customFieldsMap,
                    customFieldKeep = customFieldKeepMap
                )
                
                isSaving = false
                if (result.success) {
                    val albumChanged = modifiedField.album
                    var renameError: String? = null
                    if (albumChanged && hasVideoCover && videoCoverPath != null) {
                        if (album.isNotEmpty() && containsIllegalFileNameChars(album)) {
                            val illegalChars = getIllegalChars(album)
                            renameError = "存在非法字符\"$illegalChars\"，无法重命名。"
                        } else {
                            withContext(Dispatchers.IO) {
                                try {
                                    val oldVideoFile = File(videoCoverPath!!)
                                    if (oldVideoFile.exists()) {
                                        val parentDir = oldVideoFile.parentFile
                                        val newVideoFile = if (album.isNotEmpty()) {
                                            File(parentDir, "$album.mp4")
                                        } else {
                                            val baseName = File(audioPath!!).nameWithoutExtension
                                            File(parentDir, "$baseName.mp4")
                                        }
                                        if (oldVideoFile.absolutePath != newVideoFile.absolutePath) {
                                            val renameSuccess = oldVideoFile.renameTo(newVideoFile)
                                            if (renameSuccess) {
                                                videoCoverPath = newVideoFile.absolutePath
                                            } else {
                                                renameError = "重命名失败"
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error renaming video cover", e)
                                    renameError = e.message
                                }
                            }
                        }
                    }
                    
                    if (renameError != null) {
                        errorMessage = renameError ?: "重命名失败"
                        showErrorDialog = true
                    } else {
                        showSuccessDialog = true
                        onDataSaved()
                        modifiedField = ModifiedField()
                        // 更新自定义字段的原始值
                        customFieldValues.forEach { (field, value) ->
                            originalCustomFieldValues[field] = value
                        }
                        
                        originalData = OriginalData(
                            title = title,
                            artist = artist,
                            album = album,
                            year = year,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            genre = genre,
                            albumArtist = albumArtist,
                            composer = composer,
                            lyricist = lyricist,
                            comment = comment,
                            copyrightInfo = copyright,
                            lyrics = lyrics,
                            coverBitmap = coverBitmap,
                            customFields = customFieldValues.toMap()
                        )
                        originalCoverBitmap = coverBitmap
                    }
                } else if (result.needPermission) {
                    showPermissionDialog = true
                } else if (result.recoverableIntentSender != null) {
                    recoverableWritePermissionLauncher.launch(
                        IntentSenderRequest.Builder(result.recoverableIntentSender!!).build()
                    )
                }
            }
        }
    }

    retrySaveActionRef.value = {
        scope.launch {
            saveAllData()
        }
    }
    
    fun undoField(field: String) {
        originalData?.let { orig ->
            when (field) {
                "title" -> {
                    redoHistory = redoHistory.copy(title = title)
                    title = orig.title
                    modifiedField = modifiedField.copy(title = false, canRedoTitle = true)
                }
                "artist" -> {
                    redoHistory = redoHistory.copy(artist = artist)
                    artist = orig.artist
                    modifiedField = modifiedField.copy(artist = false, canRedoArtist = true)
                }
                "album" -> {
                    redoHistory = redoHistory.copy(album = album)
                    album = orig.album
                    modifiedField = modifiedField.copy(album = false, canRedoAlbum = true)
                }
                "year" -> {
                    redoHistory = redoHistory.copy(year = year)
                    year = orig.year
                    modifiedField = modifiedField.copy(year = false, canRedoYear = true)
                }
                "trackNumber" -> {
                    redoHistory = redoHistory.copy(trackNumber = trackNumber)
                    trackNumber = orig.trackNumber
                    modifiedField = modifiedField.copy(trackNumber = false, canRedoTrackNumber = true)
                }
                "discNumber" -> {
                    redoHistory = redoHistory.copy(discNumber = discNumber)
                    discNumber = orig.discNumber
                    modifiedField = modifiedField.copy(discNumber = false, canRedoDiscNumber = true)
                }
                "genre" -> {
                    redoHistory = redoHistory.copy(genre = genre)
                    genre = orig.genre
                    modifiedField = modifiedField.copy(genre = false, canRedoGenre = true)
                }
                "albumArtist" -> {
                    redoHistory = redoHistory.copy(albumArtist = albumArtist)
                    albumArtist = orig.albumArtist
                    modifiedField = modifiedField.copy(albumArtist = false, canRedoAlbumArtist = true)
                }
                "composer" -> {
                    redoHistory = redoHistory.copy(composer = composer)
                    composer = orig.composer
                    modifiedField = modifiedField.copy(composer = false, canRedoComposer = true)
                }
                "lyricist" -> {
                    redoHistory = redoHistory.copy(lyricist = lyricist)
                    lyricist = orig.lyricist
                    modifiedField = modifiedField.copy(lyricist = false, canRedoLyricist = true)
                }
                "comment" -> {
                    redoHistory = redoHistory.copy(comment = comment)
                    comment = orig.comment
                    modifiedField = modifiedField.copy(comment = false, canRedoComment = true)
                }
                "copyrightInfo" -> {
                    redoHistory = redoHistory.copy(copyrightInfo = copyright)
                    copyright = orig.copyrightInfo
                    modifiedField = modifiedField.copy(copyrightInfo = false, canRedoCopyrightInfo = true)
                }
                "lyrics" -> {
                    redoHistory = redoHistory.copy(lyrics = lyrics)
                    lyrics = orig.lyrics
                    modifiedField = modifiedField.copy(lyrics = false, canRedoLyrics = true)
                }
                "cover" -> {
                    redoHistory = redoHistory.copy(coverBitmap = coverBitmap)
                    coverBitmap = orig.coverBitmap
                    originalCoverBitmap = orig.coverBitmap
                    coverChanged = false
                    coverRemoved = false
                    modifiedField = modifiedField.copy(cover = false, canRedoCover = true)
                }
            }
        }
    }
    
    fun redoField(field: String) {
        when (field) {
            "title" -> {
                redoHistory.title?.let { value ->
                    title = value
                    modifiedField = modifiedField.copy(title = true, canRedoTitle = false)
                    redoHistory = redoHistory.copy(title = null)
                }
            }
            "artist" -> {
                redoHistory.artist?.let { value ->
                    artist = value
                    modifiedField = modifiedField.copy(artist = true, canRedoArtist = false)
                    redoHistory = redoHistory.copy(artist = null)
                }
            }
            "album" -> {
                redoHistory.album?.let { value ->
                    album = value
                    modifiedField = modifiedField.copy(album = true, canRedoAlbum = false)
                    redoHistory = redoHistory.copy(album = null)
                }
            }
            "year" -> {
                redoHistory.year?.let { value ->
                    year = value
                    modifiedField = modifiedField.copy(year = true, canRedoYear = false)
                    redoHistory = redoHistory.copy(year = null)
                }
            }
            "trackNumber" -> {
                redoHistory.trackNumber?.let { value ->
                    trackNumber = value
                    modifiedField = modifiedField.copy(trackNumber = true, canRedoTrackNumber = false)
                    redoHistory = redoHistory.copy(trackNumber = null)
                }
            }
            "discNumber" -> {
                redoHistory.discNumber?.let { value ->
                    discNumber = value
                    modifiedField = modifiedField.copy(discNumber = true, canRedoDiscNumber = false)
                    redoHistory = redoHistory.copy(discNumber = null)
                }
            }
            "genre" -> {
                redoHistory.genre?.let { value ->
                    genre = value
                    modifiedField = modifiedField.copy(genre = true, canRedoGenre = false)
                    redoHistory = redoHistory.copy(genre = null)
                }
            }
            "albumArtist" -> {
                redoHistory.albumArtist?.let { value ->
                    albumArtist = value
                    modifiedField = modifiedField.copy(albumArtist = true, canRedoAlbumArtist = false)
                    redoHistory = redoHistory.copy(albumArtist = null)
                }
            }
            "composer" -> {
                redoHistory.composer?.let { value ->
                    composer = value
                    modifiedField = modifiedField.copy(composer = true, canRedoComposer = false)
                    redoHistory = redoHistory.copy(composer = null)
                }
            }
            "lyricist" -> {
                redoHistory.lyricist?.let { value ->
                    lyricist = value
                    modifiedField = modifiedField.copy(lyricist = true, canRedoLyricist = false)
                    redoHistory = redoHistory.copy(lyricist = null)
                }
            }
            "comment" -> {
                redoHistory.comment?.let { value ->
                    comment = value
                    modifiedField = modifiedField.copy(comment = true, canRedoComment = false)
                    redoHistory = redoHistory.copy(comment = null)
                }
            }
            "copyrightInfo" -> {
                redoHistory.copyrightInfo?.let { value ->
                    copyright = value
                    modifiedField = modifiedField.copy(copyrightInfo = true, canRedoCopyrightInfo = false)
                    redoHistory = redoHistory.copy(copyrightInfo = null)
                }
            }
            "lyrics" -> {
                redoHistory.lyrics?.let { value ->
                    lyrics = value
                    modifiedField = modifiedField.copy(lyrics = true, canRedoLyrics = false)
                    redoHistory = redoHistory.copy(lyrics = null)
                }
            }
            "cover" -> {
                redoHistory.coverBitmap?.let { bitmap ->
                    coverBitmap = bitmap
                    originalCoverBitmap = bitmap
                    coverChanged = true
                    coverRemoved = false
                    modifiedField = modifiedField.copy(cover = true, canRedoCover = false)
                    redoHistory = redoHistory.copy(coverBitmap = null)
                }
            }
        }
    }
    
    LaunchedEffect(searchResult) {
        searchResult?.let { intent ->
            val selectedFields = intent.getStringArrayListExtra("selectedFields")?.toSet() ?: emptySet()
            
            if ("title" in selectedFields) {
                intent.getStringExtra("title")?.let { 
                    if (it.isNotEmpty() && it != originalData?.title) {
                        title = it
                        modifiedField = modifiedField.copy(title = true, canRedoTitle = false)
                    }
                }
            }
            if ("artist" in selectedFields) {
                intent.getStringExtra("artist")?.let { 
                    if (it.isNotEmpty() && it != originalData?.artist) {
                        artist = it
                        modifiedField = modifiedField.copy(artist = true, canRedoArtist = false)
                    }
                }
            }
            if ("album" in selectedFields) {
                intent.getStringExtra("album")?.let { 
                    if (it.isNotEmpty() && it != originalData?.album) {
                        album = it
                        modifiedField = modifiedField.copy(album = true, canRedoAlbum = false)
                    }
                }
            }
            if ("year" in selectedFields) {
                intent.getStringExtra("year")?.let { 
                    if (it.isNotEmpty() && it != originalData?.year) {
                        year = it
                        modifiedField = modifiedField.copy(year = true, canRedoYear = false)
                    }
                }
            }
            if ("trackNumber" in selectedFields) {
                intent.getStringExtra("trackNumber")?.let { 
                    if (it.isNotEmpty() && it != originalData?.trackNumber) {
                        trackNumber = it
                        modifiedField = modifiedField.copy(trackNumber = true, canRedoTrackNumber = false)
                    }
                }
            }
            if ("discNumber" in selectedFields) {
                intent.getStringExtra("discNumber")?.let { 
                    if (it.isNotEmpty() && it != originalData?.discNumber) {
                        discNumber = it
                        modifiedField = modifiedField.copy(discNumber = true, canRedoDiscNumber = false)
                    }
                }
            }
            if ("genre" in selectedFields) {
                intent.getStringExtra("genre")?.let { 
                    if (it.isNotEmpty() && it != originalData?.genre) {
                        genre = it
                        modifiedField = modifiedField.copy(genre = true, canRedoGenre = false)
                    }
                }
            }
            if ("albumArtist" in selectedFields) {
                intent.getStringExtra("albumArtist")?.let { 
                    if (it.isNotEmpty() && it != originalData?.albumArtist) {
                        albumArtist = it
                        modifiedField = modifiedField.copy(albumArtist = true, canRedoAlbumArtist = false)
                    }
                }
            }
            if ("composer" in selectedFields) {
                intent.getStringExtra("composer")?.let { 
                    if (it.isNotEmpty() && it != originalData?.composer) {
                        composer = it
                        modifiedField = modifiedField.copy(composer = true, canRedoComposer = false)
                    }
                }
            }
            if ("lyricist" in selectedFields) {
                intent.getStringExtra("lyricist")?.let { 
                    if (it.isNotEmpty() && it != originalData?.lyricist) {
                        lyricist = it
                        modifiedField = modifiedField.copy(lyricist = true, canRedoLyricist = false)
                    }
                }
            }
            if ("comment" in selectedFields) {
                intent.getStringExtra("comment")?.let { 
                    if (it.isNotEmpty() && it != originalData?.comment) {
                        comment = it
                        modifiedField = modifiedField.copy(comment = true, canRedoComment = false)
                    }
                }
            }
            if ("copyright" in selectedFields) {
                intent.getStringExtra("copyright")?.let { 
                    if (it.isNotEmpty() && it != originalData?.copyrightInfo) {
                        copyright = it
                        modifiedField = modifiedField.copy(copyrightInfo = true, canRedoCopyrightInfo = false)
                    }
                }
            }
            if ("cover" in selectedFields) {
                AudioMetadataSearchActivity.getAndClearTempCoverBitmap()?.let { bitmap ->
                    if (bitmap != originalCoverBitmap) {
                        coverBitmap = bitmap
                        coverChanged = true
                        coverRemoved = false
                        modifiedField = modifiedField.copy(cover = true, canRedoCover = false)
                    }
                }
            }
            onSearchResultConsumed()
        }
    }
    
    LaunchedEffect(importedLyrics) {
        if (importedLyrics != null) {
            lyrics = importedLyrics
            modifiedField = modifiedField.copy(lyrics = true, canRedoLyrics = false)
            onImportedLyricsConsumed()
        }
    }

    LaunchedEffect(lyricTimingReturnNonce) {
        if (lyricTimingReturnNonce == 0L || audioPath.isNullOrBlank() || isBatchEdit) return@LaunchedEffect
        val refreshed = withContext(Dispatchers.IO) {
            com.example.LyricBox.utils.AudioMetadataReader.readLyrics(context, audioPath!!, mediaStoreId)
        } ?: ""
        lyrics = refreshed
        originalData = originalData?.copy(lyrics = refreshed)
        modifiedField = modifiedField.copy(lyrics = false, canRedoLyrics = false)
        redoHistory = redoHistory.copy(lyrics = null)
    }
    
    LaunchedEffect(modifiedField) {
        onUnsavedChangesChanged(hasUnsavedChanges())
    }
    
    LaunchedEffect(Unit) {
        if (isBatchEdit && selectedPaths != null) {
            // 批量编辑模式，加载所有选中歌曲的元数据
            scope.launch {
                val titles = mutableSetOf<String>()
                val artists = mutableSetOf<String>()
                val albums = mutableSetOf<String>()
                val years = mutableSetOf<String>()
                val trackNumbers = mutableSetOf<String>()
                val discNumbers = mutableSetOf<String>()
                val genres = mutableSetOf<String>()
                val albumArtists = mutableSetOf<String>()
                val composers = mutableSetOf<String>()
                val lyricists = mutableSetOf<String>()
                val comments = mutableSetOf<String>()
                val copyrightInfos = mutableSetOf<String>()
                val lyricsValues = mutableSetOf<String>()
                val customFieldValuesMap = mutableMapOf<String, MutableSet<String>>()
                
                // 初始化自定义字段值集合
                enabledFields.forEach { field ->
                    customFieldValuesMap[field] = mutableSetOf()
                }
                
                for (path in selectedPaths!!) {
                    try {
                        val file = File(path)
                        if (file.exists()) {
                            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                            val data = AudioTagReader.read(pfd, true)
                            pfd.close()
                            var normalizedGenre: String? = data.genre?.takeIf { it.isNotEmpty() }
                            
                            data.title?.takeIf { it.isNotEmpty() }?.let { titles.add(it) }
                            data.artist?.takeIf { it.isNotEmpty() }?.let { artists.add(it) }
                            data.album?.takeIf { it.isNotEmpty() }?.let { albums.add(it) }
                            data.date?.takeIf { it.isNotEmpty() }?.let { years.add(it) }
                            data.trackNumber?.takeIf { it.isNotEmpty() }?.let { trackNumbers.add(it) }
                            data.discNumber?.toString()?.takeIf { it.isNotEmpty() }?.let { discNumbers.add(it) }
                            data.albumArtist?.takeIf { it.isNotEmpty() }?.let { albumArtists.add(it) }
                            data.composer?.takeIf { it.isNotEmpty() }?.let { composers.add(it) }
                            data.lyricist?.takeIf { it.isNotEmpty() }?.let { lyricists.add(it) }
                            data.comment?.takeIf { it.isNotEmpty() }?.let { comments.add(it) }
                            
                            // 读取版权信息和自定义字段
                            try {
                                val tagPfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                val nativeFd = tagPfd.dup().detachFd()
                                val metadata = TagLib.getMetadata(nativeFd, false)
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

                                    fun joinedOf(vararg keys: String): String? {
                                        for (key in keys) {
                                            val arr = props[key]
                                            if (!arr.isNullOrEmpty()) {
                                                val values = arr
                                                    .flatMap { item -> item.split(GENRE_SPLIT_REGEX) }
                                                    .map { it.trim() }
                                                    .filter { it.isNotEmpty() }
                                                    .distinct()
                                                if (values.isNotEmpty()) return values.joinToString("/")
                                            }
                                        }
                                        return null
                                    }
                                    
                                    firstOf("COPYRIGHT", "COPYRIGHTS", "COPYRIGHTINFO")?.let { copyrightInfos.add(it) }
                                    normalizedGenre = joinedOf("GENRE") ?: normalizedGenre
                                    
                                    // 读取自定义字段
                                    enabledFields.forEach { field ->
                                        firstOf(field.uppercase())?.takeIf { it.isNotEmpty() }?.let { value ->
                                            customFieldValuesMap[field]?.add(value)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error reading copyright with TagLib for $path", e)
                            }
                            normalizedGenre?.let { genres.add(it) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading metadata for $path", e)
                    }
                }
                
                // 构建自定义字段值Map
                val customFieldsForBatch = mutableMapOf<String, Set<String>>()
                customFieldValuesMap.forEach { (field, values) ->
                    customFieldsForBatch[field] = values.toSet()
                }
                
                batchEditFieldValues = BatchEditFieldValues(
                    titles = titles,
                    artists = artists,
                    albums = albums,
                    years = years,
                    trackNumbers = trackNumbers,
                    discNumbers = discNumbers,
                    genres = genres,
                    albumArtists = albumArtists,
                    composers = composers,
                    lyricists = lyricists,
                    comments = comments,
                    copyrightInfos = copyrightInfos,
                    lyricsValues = lyricsValues,
                    customFieldValues = customFieldsForBatch
                )
                
                isLoading = false
                onUnsavedChangesChanged(false)
            }
        } else if (audioPath != null) {
            // 普通编辑模式
            loadMetadata(
                context = context,
                filePath = audioPath,
                mediaStoreId = mediaStoreId,
                onLoaded = { data, cover, copyrightFromTagLib, lyricsFromTagLib, genreFromTagLib, customFieldsFromTagLib ->
                    tagData = data
                    coverBitmap = cover
                    originalCoverBitmap = cover
                    val resolvedGenre = genreFromTagLib ?: data.genre ?: ""
                    if (!hasInitializedEditableFields) {
                        title = data.title ?: ""
                        artist = data.artist ?: ""
                        album = data.album ?: ""
                        year = data.date ?: ""
                        trackNumber = data.trackNumber ?: ""
                        discNumber = data.discNumber?.toString() ?: ""
                        genre = resolvedGenre
                        albumArtist = data.albumArtist ?: ""
                        composer = data.composer ?: ""
                        lyricist = data.lyricist ?: ""
                        comment = data.comment ?: ""
                        copyright = copyrightFromTagLib ?: ""
                        lyrics = lyricsFromTagLib ?: ""
                        customFieldValues.clear()
                        customFieldValues.putAll(customFieldsFromTagLib)
                        hasInitializedEditableFields = true
                    }

                    // 原始值始终更新，用于计算修改状态和撤销逻辑
                    originalCustomFieldValues.clear()
                    originalCustomFieldValues.putAll(customFieldsFromTagLib)

                    originalData = OriginalData(
                        title = data.title ?: "",
                        artist = data.artist ?: "",
                        album = data.album ?: "",
                        year = data.date ?: "",
                        trackNumber = data.trackNumber ?: "",
                        discNumber = data.discNumber?.toString() ?: "",
                        genre = resolvedGenre,
                        albumArtist = data.albumArtist ?: "",
                        composer = data.composer ?: "",
                        lyricist = data.lyricist ?: "",
                        comment = data.comment ?: "",
                        copyrightInfo = copyrightFromTagLib ?: "",
                        lyrics = lyricsFromTagLib ?: "",
                        coverBitmap = cover,
                        customFields = customFieldsFromTagLib
                    )
                    scope.launch {
                        videoCoverPath = getVideoCoverPath(context, audioPath, data.album)
                        hasVideoCover = videoCoverPath != null
                    }
                    audioFileName = File(audioPath).nameWithoutExtension
                    isLoading = false
                    onUnsavedChangesChanged(false)
                },
                onError = {
                    // 权限或文件访问失败时，避免界面一直停留在“正在加载”
                    isLoading = false
                }
            )
        } else {
            isLoading = false
        }
    }
    
    fun removeImageCover() {
        coverBitmap = null
        coverChanged = true
        coverRemoved = true
        modifiedField = modifiedField.copy(cover = true, canRedoCover = false)
    }
    
    fun removeVideoCover() {
        videoCoverPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
                videoCoverPath = null
                hasVideoCover = false
                showSuccessDialog = true
            }
        }
    }
    
    fun saveImageToAlbum() {
        coverBitmap?.let { bitmap ->
            scope.launch {
                val success = saveImageToGallery(context, bitmap, audioFileName)
                if (success) {
                    showSuccessDialog = true
                }
            }
        }
    }

    fun removeAccompaniment() {
        scope.launch {
            val result = removeAccompanimentFile(context, accompanimentPath)
            when {
                result.success -> {
                    accompanimentPath = null
                    showSuccessDialog = true
                }
                result.needPermission -> {
                    showPermissionDialog = true
                }
                else -> {
                    errorMessage = result.errorMessage ?: "移除伴奏失败"
                    showErrorDialog = true
                }
            }
        }
    }
    
    val pickImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bitmap = loadBitmapFromUri(context, it)
                bitmap?.let { bmp ->
                    coverBitmap = bmp
                    coverChanged = true
                    coverRemoved = false
                    modifiedField = modifiedField.copy(cover = true, canRedoCover = false)
                }
            }
        }
    }
    
    val pickVideoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (audioPath != null) {
                scope.launch {
                    val result = saveVideoCover(context, audioPath!!, it, album)
                    if (result.success) {
                        videoCoverPath = getVideoCoverPath(context, audioPath, album)
                        hasVideoCover = true
                        showSuccessDialog = true
                    } else if (result.errorMessage != null) {
                        errorMessage = result.errorMessage
                        showErrorDialog = true
                    }
                }
            }
        }
    }

    val pickAccompanimentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isCopyingAccompaniment = true
                try {
                    val cachedTreeUri = getAccompanimentTreeUri(context)
                    val result = copyAccompanimentToDefaultPath(context, audioPath, it, cachedTreeUri)
                    when {
                        result.success -> {
                            accompanimentPath = result.path
                            copiedAccompanimentPath = result.path
                            accompanimentSourceUri = it
                            showAccompanimentCopiedDialog = true
                        }
                        result.needPermission -> {
                            showPermissionDialog = true
                        }
                        else -> {
                            errorMessage = result.errorMessage ?: "添加伴奏失败"
                            showErrorDialog = true
                        }
                    }
                } finally {
                    isCopyingAccompaniment = false
                }
            }
        }
    }

    val accompanimentDirectoryPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) {
            accompanimentDirectoryAuthResultMessage = "授权失败：未授予 /Music/ 目录权限。"
            showAccompanimentDirectoryAuthResultDialog = true
            return@rememberLauncherForActivityResult
        }
        if (!isMusicDirectoryTreeUri(context, treeUri)) {
            accompanimentDirectoryAuthResultMessage = "授权失败：请选择 /Music/ 目录。"
            showAccompanimentDirectoryAuthResultDialog = true
            return@rememberLauncherForActivityResult
        }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }.isSuccess
        if (!persisted) {
            accompanimentDirectoryAuthResultMessage = "授权失败：保存 /Music/ 目录权限失败。"
            showAccompanimentDirectoryAuthResultDialog = true
            return@rememberLauncherForActivityResult
        }
        saveAccompanimentTreeUri(context, treeUri)
        accompanimentDirectoryAuthResultMessage = "授权成功，请重新点击“添加伴奏”。"
        showAccompanimentDirectoryAuthResultDialog = true
    }

    val requestAccompanimentDirectoryPermissionAndPickAudio: () -> Unit = {
        // 只在 Android 10 上检查授权，其他版本直接选择文件
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
            pickAccompanimentLauncher.launch("audio/*")
        } else {
            val cachedTreeUri = getAccompanimentTreeUri(context)
            val isAuthorized = cachedTreeUri != null &&
                hasPersistedTreeReadWritePermission(context, cachedTreeUri) &&
                isMusicDirectoryTreeUri(context, cachedTreeUri)
            if (isAuthorized) {
                pickAccompanimentLauncher.launch("audio/*")
            } else {
                showAccompanimentDirectoryAuthDialog = true
            }
        }
    }
    
    val buttonCount = remember(coverBitmap, hasVideoCover) {
        var count = 2
        if (coverBitmap != null) count++
        if (hasVideoCover) count++
        count
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        CommonHeadBar(
            title = if (isBatchEdit) "批量编辑（已选择${selectedPaths?.size ?: 0}）" else "编辑歌曲元数据",
            showBack = true,
            showMenu = true,
            onBackClick = {
                onCheckUnsavedChanges(hasUnsavedChanges())
            },
            onMenuClick = { menuExpanded = true },
            menuContent = { menuButtonPosition ->
                com.example.LyricBox.ui.components.CustomDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = if (isBatchEdit) {
                        listOf(
                            com.example.LyricBox.ui.components.MenuItem(
                                title = "重置        ",
                                onClick = {
                                    title = KEEP
                                    artist = KEEP
                                    album = KEEP
                                    year = KEEP
                                    trackNumber = KEEP
                                    discNumber = KEEP
                                    genre = KEEP
                                    albumArtist = KEEP
                                    composer = KEEP
                                    lyricist = KEEP
                                    comment = KEEP
                                    copyright = KEEP
                                    // 重置自定义字段
                                    enabledFields.forEach { field ->
                                        customFieldValues[field] = KEEP
                                    }
                                    coverBitmap = null
                                    coverChanged = false
                                    coverRemoved = false
                                    modifiedField = ModifiedField()
                                }
                            ),
                            com.example.LyricBox.ui.components.MenuItem(
                                title = "保存",
                                onClick = { saveAllData() }
                            )
                        )
                    } else {
                        listOf(
                            com.example.LyricBox.ui.components.MenuItem(
                                title = "搜索元数据",
                                onClick = { 
                                    onSearchMetadata(buildMetadataSearchKeyword(), false)
                                }
                            ),
                            com.example.LyricBox.ui.components.MenuItem(
                                title = "保存",
                                onClick = { saveAllData() }
                            )
                        )
                    },
                    anchorPosition = menuButtonPosition ?: com.example.LyricBox.ui.components.MenuAnchorPosition(0f, 0f)
                )
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在加载...", fontSize = 14.sp)
                }
            }
        } else {
            val hasUnsaved = hasUnsavedChanges()
            val saveFabScale by animateFloatAsState(
                targetValue = if (hasUnsaved) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "saveFabScale"
            )
            
            val configuration = LocalConfiguration.current
            val screenHeightDp = configuration.screenHeightDp.dp
            val keyboardBottomPadding by animateDpAsState(
                targetValue = screenHeightDp / 2,
                animationSpec = tween(durationMillis = 300),
                label = "keyboardBottomPadding"
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                val lazyListState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = keyboardBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(28.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (modifiedField.cover) "封面（已修改）" else "封面",
                                    fontSize = 14.sp,
                                    color = if (modifiedField.cover) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (modifiedField.canRedoCover) {
                                        Box(
                                            modifier = Modifier.size(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Image(
                                                painter = painterResource(id = R.drawable.redo),
                                                contentDescription = "重做",
                                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { redoField("cover") }
                                            )
                                        }
                                    }
                                    if (modifiedField.cover) {
                                        Box(
                                            modifier = Modifier.size(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Image(
                                                painter = painterResource(id = R.drawable.undo),
                                                contentDescription = "撤销",
                                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { undoField("cover") }
                                            )
                                        }
                                    }
                                    if (!modifiedField.cover && !modifiedField.canRedoCover) {
                                        Spacer(modifier = Modifier.size(48.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        color = if (isBatchEdit) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else if (modifiedField.cover) {
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) 
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        }
                                    )
                                    .clickable {
                                        if (hasVideoCover && coverBitmap != null) {
                                            previewType = "image"
                                            showCoverPreview = true
                                        } else if (coverBitmap != null) {
                                            previewType = "image"
                                            showCoverPreview = true
                                        } else if (hasVideoCover) {
                                            previewType = "video"
                                            showCoverPreview = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isBatchEdit && !modifiedField.cover) {
                                    // 批量编辑模式，封面保持为KEEP时显示文本
                                    Text(
                                        text = KEEP,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else if (coverBitmap != null) {
                                    Image(
                                        bitmap = coverBitmap!!.asImageBitmap(),
                                        contentDescription = "封面",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.img),
                                        contentDescription = "添加封面",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (hasVideoCover) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    previewType = "video"
                                                    showCoverPreview = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = android.R.drawable.ic_media_play),
                                                contentDescription = "有视频封面",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                onSearchMetadata(buildMetadataSearchKeyword(), true)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.searchbold),
                                            contentDescription = "搜索封面",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            coverBitmap?.let { bitmap ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${bitmap.width} × ${bitmap.height}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCoverSelectionSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(min = 200.dp)
                            ) {
                                Text("选择图片封面", maxLines = 1)
                            }
                            
                            if (!isBatchEdit) {
                                OutlinedButton(
                                    onClick = { pickVideoLauncher.launch("video/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(min = 200.dp)
                                ) {
                                    Text("选择视频封面", maxLines = 1)
                                }
                            }
                            
                            if (coverBitmap != null) {
                                OutlinedButton(
                                    onClick = { showRemoveImageConfirm = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(min = 200.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("移除图片封面", maxLines = 1)
                                }
                            }
                            
                            if (!isBatchEdit && hasVideoCover) {
                                OutlinedButton(
                                    onClick = { showRemoveVideoConfirm = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(min = 200.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("移除视频封面", maxLines = 1)
                                }
                            }
                        }
                    }
                }
                
                val renderedFieldKeys = if (isBatchEdit) {
                    visibleFieldKeys.filterNot { it == "accompaniment" }
                } else {
                    visibleFieldKeys
                }

                itemsIndexed(
                    items = renderedFieldKeys,
                    key = { _, fieldKey -> fieldKey }
                ) { index, fieldKey ->
                    when (fieldKey) {
                        "title" -> ModifiableMetadataField(
                            label = "标题",
                            value = title,
                            onValueChange = {
                                title = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(title = it != KEEP, canRedoTitle = false)
                                } else {
                                    modifiedField = modifiedField.copy(title = it != originalData?.title, canRedoTitle = false)
                                }
                            },
                            isModified = modifiedField.title,
                            canRedo = modifiedField.canRedoTitle,
                            onUndo = { undoField("title") },
                            onRedo = { redoField("title") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "title"
                                showFieldSelectionSheet = true
                            }
                        )

                        "artist" -> ModifiableMetadataField(
                            label = "艺术家",
                            value = artist,
                            onValueChange = {
                                artist = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(artist = it != KEEP, canRedoArtist = false)
                                } else {
                                    modifiedField = modifiedField.copy(artist = it != originalData?.artist, canRedoArtist = false)
                                }
                            },
                            isModified = modifiedField.artist,
                            canRedo = modifiedField.canRedoArtist,
                            onUndo = { undoField("artist") },
                            onRedo = { redoField("artist") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "artist"
                                showFieldSelectionSheet = true
                            }
                        )

                        "album" -> ModifiableMetadataField(
                            label = "专辑",
                            value = album,
                            onValueChange = {
                                album = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(album = it != KEEP, canRedoAlbum = false)
                                } else {
                                    modifiedField = modifiedField.copy(album = it != originalData?.album, canRedoAlbum = false)
                                }
                            },
                            isModified = modifiedField.album,
                            canRedo = modifiedField.canRedoAlbum,
                            onUndo = { undoField("album") },
                            onRedo = { redoField("album") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "album"
                                showFieldSelectionSheet = true
                            }
                        )

                        "year" -> ModifiableMetadataField(
                            label = "年份",
                            value = year,
                            onValueChange = {
                                year = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(year = it != KEEP, canRedoYear = false)
                                } else {
                                    modifiedField = modifiedField.copy(year = it != originalData?.year, canRedoYear = false)
                                }
                            },
                            isModified = modifiedField.year,
                            canRedo = modifiedField.canRedoYear,
                            onUndo = { undoField("year") },
                            onRedo = { redoField("year") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "year"
                                showFieldSelectionSheet = true
                            }
                        )

                        "trackDisc" -> Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModifiableMetadataField(
                                label = "音轨号",
                                value = trackNumber,
                                onValueChange = {
                                    trackNumber = it
                                    if (isBatchEdit) {
                                        modifiedField = modifiedField.copy(trackNumber = it != KEEP, canRedoTrackNumber = false)
                                    } else {
                                        modifiedField = modifiedField.copy(trackNumber = it != originalData?.trackNumber, canRedoTrackNumber = false)
                                    }
                                },
                                isModified = modifiedField.trackNumber,
                                canRedo = modifiedField.canRedoTrackNumber,
                                onUndo = { undoField("trackNumber") },
                                onRedo = { redoField("trackNumber") },
                                onFocused = {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                isBatchEdit = isBatchEdit,
                                onShowDropdown = {
                                    currentSelectionField = "trackNumber"
                                    showFieldSelectionSheet = true
                                }
                            )
                            ModifiableMetadataField(
                                label = "碟号",
                                value = discNumber,
                                onValueChange = {
                                    discNumber = it
                                    if (isBatchEdit) {
                                        modifiedField = modifiedField.copy(discNumber = it != KEEP, canRedoDiscNumber = false)
                                    } else {
                                        modifiedField = modifiedField.copy(discNumber = it != originalData?.discNumber, canRedoDiscNumber = false)
                                    }
                                },
                                isModified = modifiedField.discNumber,
                                canRedo = modifiedField.canRedoDiscNumber,
                                onUndo = { undoField("discNumber") },
                                onRedo = { redoField("discNumber") },
                                onFocused = {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                isBatchEdit = isBatchEdit,
                                onShowDropdown = {
                                    currentSelectionField = "discNumber"
                                    showFieldSelectionSheet = true
                                }
                            )
                        }

                        "genre" -> ModifiableMetadataField(
                            label = "风格",
                            value = genre,
                            onValueChange = {
                                genre = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(genre = it != KEEP, canRedoGenre = false)
                                } else {
                                    modifiedField = modifiedField.copy(genre = it != originalData?.genre, canRedoGenre = false)
                                }
                            },
                            isModified = modifiedField.genre,
                            canRedo = modifiedField.canRedoGenre,
                            onUndo = { undoField("genre") },
                            onRedo = { redoField("genre") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "genre"
                                showFieldSelectionSheet = true
                            }
                        )

                        "albumArtist" -> ModifiableMetadataField(
                            label = "专辑艺术家",
                            value = albumArtist,
                            onValueChange = {
                                albumArtist = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(albumArtist = it != KEEP, canRedoAlbumArtist = false)
                                } else {
                                    modifiedField = modifiedField.copy(albumArtist = it != originalData?.albumArtist, canRedoAlbumArtist = false)
                                }
                            },
                            isModified = modifiedField.albumArtist,
                            canRedo = modifiedField.canRedoAlbumArtist,
                            onUndo = { undoField("albumArtist") },
                            onRedo = { redoField("albumArtist") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "albumArtist"
                                showFieldSelectionSheet = true
                            }
                        )

                        "composer" -> ModifiableMetadataField(
                            label = "作曲",
                            value = composer,
                            onValueChange = {
                                composer = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(composer = it != KEEP, canRedoComposer = false)
                                } else {
                                    modifiedField = modifiedField.copy(composer = it != originalData?.composer, canRedoComposer = false)
                                }
                            },
                            isModified = modifiedField.composer,
                            canRedo = modifiedField.canRedoComposer,
                            onUndo = { undoField("composer") },
                            onRedo = { redoField("composer") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "composer"
                                showFieldSelectionSheet = true
                            }
                        )

                        "lyricist" -> ModifiableMetadataField(
                            label = "作词",
                            value = lyricist,
                            onValueChange = {
                                lyricist = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(lyricist = it != KEEP, canRedoLyricist = false)
                                } else {
                                    modifiedField = modifiedField.copy(lyricist = it != originalData?.lyricist, canRedoLyricist = false)
                                }
                            },
                            isModified = modifiedField.lyricist,
                            canRedo = modifiedField.canRedoLyricist,
                            onUndo = { undoField("lyricist") },
                            onRedo = { redoField("lyricist") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "lyricist"
                                showFieldSelectionSheet = true
                            }
                        )

                        "comment" -> ModifiableMetadataField(
                            label = "注释",
                            value = comment,
                            onValueChange = {
                                comment = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(comment = it != KEEP, canRedoComment = false)
                                } else {
                                    modifiedField = modifiedField.copy(comment = it != originalData?.comment, canRedoComment = false)
                                }
                            },
                            isModified = modifiedField.comment,
                            canRedo = modifiedField.canRedoComment,
                            onUndo = { undoField("comment") },
                            onRedo = { redoField("comment") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 4,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "comment"
                                showFieldSelectionSheet = true
                            }
                        )

                        "copyrightInfo" -> ModifiableMetadataField(
                            label = "版权信息",
                            value = copyright,
                            onValueChange = {
                                copyright = it
                                if (isBatchEdit) {
                                    modifiedField = modifiedField.copy(copyrightInfo = it != KEEP, canRedoCopyrightInfo = false)
                                } else {
                                    modifiedField = modifiedField.copy(copyrightInfo = it != originalData?.copyrightInfo, canRedoCopyrightInfo = false)
                                }
                            },
                            isModified = modifiedField.copyrightInfo,
                            canRedo = modifiedField.canRedoCopyrightInfo,
                            onUndo = { undoField("copyrightInfo") },
                            onRedo = { redoField("copyrightInfo") },
                            onFocused = {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                }
                            },
                            minLines = 1,
                            maxLines = 10,
                            isBatchEdit = isBatchEdit,
                            onShowDropdown = {
                                currentSelectionField = "copyrightInfo"
                                showFieldSelectionSheet = true
                            }
                        )

                        "accompaniment" -> AccompanimentMetadataField(
                            path = accompanimentPath,
                            onAddClick = { requestAccompanimentDirectoryPermissionAndPickAudio() },
                            onRemoveClick = { showRemoveAccompanimentConfirm = true }
                        )

                        "lyrics" -> Column {
                            ModifiableMetadataField(
                                label = "歌词",
                                value = lyrics,
                                onValueChange = {
                                    updateLyricsWithModifiedState(it)
                                },
                                isModified = modifiedField.lyrics,
                                canRedo = modifiedField.canRedoLyrics,
                                onUndo = { undoField("lyrics") },
                                onRedo = { redoField("lyrics") },
                                onFocused = {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                    }
                                },
                                minLines = 1,
                                maxLines = 10,
                                isBatchEdit = isBatchEdit,
                                showDropdown = false,
                                showMoreButton = !isBatchEdit,
                                onMoreClick = { showLyricsMoreMenu = true },
                                onMoreButtonPositioned = { anchor -> lyricsMoreMenuAnchor = anchor },
                                onShowDropdown = {
                                    currentSelectionField = "lyrics"
                                    showFieldSelectionSheet = true
                                }
                            )
                            if (!isBatchEdit) {
                                CustomDropdownMenu(
                                    expanded = showLyricsMoreMenu,
                                    onDismissRequest = { showLyricsMoreMenu = false },
                                    items = listOf(
                                        MenuItem(
                                            title = "去打轴界面编辑",
                                            onClick = {
                                                if (hasUnsavedChanges()) {
                                                    showLyricsEditConfirmDialog = true
                                                } else {
                                                    openLyricTimingByPreference()
                                                }
                                            }
                                        ),
                                        MenuItem(
                                            title = "获取歌词",
                                            onClick = { onOpenVerbatimLyricsSearch(buildLyricsSearchKeyword()) }
                                        ),
                                        MenuItem(
                                            title = "平移时间戳",
                                            onClick = { showShiftTimestampDialog = true }
                                        ),
                                        MenuItem(
                                            title = "转换为简体",
                                            onClick = { convertLyricsToSimplified() }
                                        ),
                                        MenuItem(
                                            title = "删除空行",
                                            onClick = { deleteLyricsEmptyLines() }
                                        ),
                                        MenuItem(
                                            title = "格式化时间戳",
                                            onClick = { formatLyricsTimeline() }
                                        ),
                                        MenuItem(
                                            title = "转换歌词格式",
                                            subItems = listOf(
                                                MenuItem(title = "LRC逐字", onClick = { convertLyricsFormat(LyricExportFormat.LRC_WORD) }),
                                                MenuItem(title = "LRC逐行", onClick = { convertLyricsFormat(LyricExportFormat.LRC_LINE) }),
                                                MenuItem(title = "ELRC", onClick = { convertLyricsFormat(LyricExportFormat.ENHANCED_LRC) }),
                                                MenuItem(title = "TTML", onClick = { convertLyricsFormat(LyricExportFormat.TTML) })
                                            )
                                        )
                                    ),
                                    anchorPosition = lyricsMoreMenuAnchor ?: MenuAnchorPosition(0f, 0f)
                                )
                            }
                        }

                        else -> {
                            if (isCustomMetadataFieldKey(fieldKey)) {
                                val field = customMetadataFieldNameFromKey(fieldKey)
                                if (!customFieldValues.containsKey(field)) {
                                    customFieldValues[field] = if (isBatchEdit) KEEP else ""
                                }
                                ModifiableMetadataField(
                                    label = field,
                                    value = customFieldValues[field] ?: (if (isBatchEdit) KEEP else ""),
                                    onValueChange = {
                                        customFieldValues[field] = it
                                        if (isBatchEdit) {
                                            modifiedField = if (it == KEEP) {
                                                modifiedField.copy(
                                                    customFields = modifiedField.customFields - field,
                                                    canRedoCustomFields = modifiedField.canRedoCustomFields - field
                                                )
                                            } else {
                                                modifiedField.copy(
                                                    customFields = modifiedField.customFields + (field to true),
                                                    canRedoCustomFields = modifiedField.canRedoCustomFields - field
                                                )
                                            }
                                        } else {
                                            modifiedField = if (it != originalCustomFieldValues[field]) {
                                                modifiedField.copy(
                                                    customFields = modifiedField.customFields + (field to true),
                                                    canRedoCustomFields = modifiedField.canRedoCustomFields - field
                                                )
                                            } else {
                                                modifiedField.copy(
                                                    customFields = modifiedField.customFields - field,
                                                    canRedoCustomFields = modifiedField.canRedoCustomFields - field
                                                )
                                            }
                                        }
                                    },
                                    isModified = modifiedField.customFields[field] == true,
                                    canRedo = modifiedField.canRedoCustomFields[field] == true,
                                    onUndo = {
                                        originalCustomFieldValues[field]?.let { origValue ->
                                            redoHistory = redoHistory.copy(
                                                customFields = redoHistory.customFields + (field to (customFieldValues[field] ?: ""))
                                            )
                                            customFieldValues[field] = origValue
                                            modifiedField = modifiedField.copy(
                                                customFields = modifiedField.customFields - field,
                                                canRedoCustomFields = modifiedField.canRedoCustomFields + (field to true)
                                            )
                                        }
                                    },
                                    onRedo = {
                                        redoHistory.customFields[field]?.let { value ->
                                            customFieldValues[field] = value
                                            modifiedField = modifiedField.copy(
                                                customFields = modifiedField.customFields + (field to true),
                                                canRedoCustomFields = modifiedField.canRedoCustomFields - field
                                            )
                                            redoHistory = redoHistory.copy(
                                                customFields = redoHistory.customFields - field
                                            )
                                        }
                                    },
                                    onFocused = {
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(index + 1, scrollOffset = -200)
                                        }
                                    },
                                    minLines = 1,
                                    maxLines = 4,
                                    isBatchEdit = isBatchEdit,
                                    showDropdown = true,
                                    onShowDropdown = {
                                        currentSelectionField = field
                                        showFieldSelectionSheet = true
                                    }
                                )
                            }
                        }
                    }
                }

            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (!isBatchEdit) {
                    FloatingActionButton(
                        onClick = { 
                            onSearchMetadata(buildMetadataSearchKeyword(), false)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.searchbold),
                            contentDescription = "搜索元数据",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                val shouldShowSave = hasUnsavedChanges()
                
                if (saveFabScale > 0f) {
                    val fabOffset by animateDpAsState(
                        targetValue = if (isBatchEdit) 0.dp else 64.dp,
                        animationSpec = tween(durationMillis = 200),
                        label = "fabOffset"
                    )
                    FloatingActionButton(
                        onClick = { saveAllData() },
                        modifier = Modifier
                            .offset(y = -fabOffset)
                            .graphicsLayer {
                                scaleX = saveFabScale
                                scaleY = saveFabScale
                                alpha = saveFabScale
                            },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.save),
                            contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            }
        }
    }
    
    if (showCoverPreview) {
        HalfScreenPreviewDialog(
            previewType = previewType,
            coverBitmap = coverBitmap,
            videoCoverPath = videoCoverPath,
            onDismiss = { showCoverPreview = false },
            onSwitchType = { newType ->
                previewType = newType
            },
            hasImageCover = coverBitmap != null,
            hasVideoCover = hasVideoCover,
            onSaveImage = { saveImageToAlbum() },
            onCropClick = {
                showCoverPreview = false
                coverBitmap?.let { bitmap ->
                    saveBitmapToTempUri(bitmap)?.let { uri ->
                        onStartCrop(uri)
                    }
                }
            }
        )
    }
    
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("提示") },
            text = { Text("操作成功") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSuccessDialog = false
                    onExit()
                }) {
                    Text("退出")
                }
            }
        )
    }

    if (showAccompanimentCopiedDialog) {
        AlertDialog(
            onDismissRequest = { showAccompanimentCopiedDialog = false },
            title = { Text("伴奏添加成功") },
            text = {
                Text(
                    buildString {
                        append("伴奏已经复制到：")
                        append(copiedAccompanimentPath ?: ACCOMPANIMENT_DIR_PATH)
                        append("\n是否删除原文件？")
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccompanimentCopiedDialog = false
                        accompanimentSourceUri = null
                    }
                ) {
                    Text("保留")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val sourceUri = accompanimentSourceUri
                        showAccompanimentCopiedDialog = false
                        accompanimentSourceUri = null
                        scope.launch {
                            val result = deleteOriginalAccompanimentSource(context, sourceUri)
                            when {
                                result.success -> Unit
                                result.needPermission -> {
                                    showPermissionDialog = true
                                }
                                else -> {
                                    errorMessage = result.errorMessage ?: "删除原文件失败"
                                    showErrorDialog = true
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            }
        )
    }
    
    if (showLyricsEditConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsEditConfirmDialog = false },
            title = { Text("提示") },
            text = { Text("当前页面存在未保存数据，继续前往会丢失未保存数据，是否继续？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLyricsEditConfirmDialog = false
                        openLyricTimingByPreference()
                    }
                ) {
                    Text("继续前往")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLyricsEditConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showShiftTimestampDialog) {
        val shiftInputFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { showShiftTimestampDialog = false },
            title = { Text("平移时间戳") },
            text = {
                Column {
                    Text(
                        text = "请输入平移毫秒数，正数后移，负数前移。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { shiftInputFocusRequester.requestFocus() }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val current = shiftTimestampInput.trim().toLongOrNull() ?: 0L
                                    shiftTimestampInput = (current - 100L).toString()
                                }
                            ) { Text("-") }
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = shiftTimestampInput,
                                onValueChange = { shiftTimestampInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(shiftInputFocusRequester),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                ),
                                decorationBox = { innerTextField ->
                                    if (shiftTimestampInput.isBlank()) {
                                        Text(
                                            text = "请输入毫秒数",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    val current = shiftTimestampInput.trim().toLongOrNull() ?: 0L
                                    shiftTimestampInput = (current + 100L).toString()
                                }
                            ) { Text("+") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val shiftMs = shiftTimestampInput.trim().toLongOrNull()
                        if (shiftMs == null) {
                            errorMessage = "请输入有效的毫秒数"
                            showErrorDialog = true
                        } else {
                            shiftLyricsTimestamps(shiftMs)
                            showShiftTimestampDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftTimestampDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showRemoveImageConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveImageConfirm = false },
            title = { Text("确认移除") },
            text = { Text("确定要移除图片封面吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveImageConfirm = false
                        removeImageCover()
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveImageConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showRemoveVideoConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveVideoConfirm = false },
            title = { Text("确认移除") },
            text = { Text("确定要移除视频封面吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveVideoConfirm = false
                        removeVideoCover()
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveVideoConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showRemoveAccompanimentConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveAccompanimentConfirm = false },
            title = { Text("确认移除") },
            text = { Text("确定要移除伴奏文件吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveAccompanimentConfirm = false
                        removeAccompaniment()
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAccompanimentConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAccompanimentDirectoryAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAccompanimentDirectoryAuthDialog = false },
            title = { Text("授权提示") },
            text = { Text("请授权 /Music/ 目录访问权限。授权完成后请重新点击“添加伴奏”。") },
            confirmButton = {
                Button(
                    onClick = {
                        showAccompanimentDirectoryAuthDialog = false
                        accompanimentDirectoryPickerLauncher.launch(null)
                    }
                ) {
                    Text("去授权")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccompanimentDirectoryAuthDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAccompanimentDirectoryAuthResultDialog) {
        AlertDialog(
            onDismissRequest = { showAccompanimentDirectoryAuthResultDialog = false },
            title = { Text("授权结果") },
            text = { Text(accompanimentDirectoryAuthResultMessage) },
            confirmButton = {
                Button(onClick = { showAccompanimentDirectoryAuthResultDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    if (isCopyingAccompaniment) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("复制中")
                }
            }
        }
    }
    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要权限") },
            text = { Text("需要\"所有文件访问\"权限才能保存文件。请点击\"设置权限\"按钮授权。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.addCategory("android.intent.category.DEFAULT")
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                context.startActivity(intent)
                            }
                        }
                    }
                ) {
                    Text("设置权限")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("错误") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    if (showBatchProgressDialog) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "正在批量修改...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { 
                            if (batchProgress.second > 0) 
                                batchProgress.first.toFloat() / batchProgress.second.toFloat() 
                            else 0f 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${batchProgress.first}/${batchProgress.second}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { isBatchCancelled = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("取消操作")
                    }
                }
            }
        }
    }
    
    if (showBatchCompleteDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("提示") },
            text = { Text("批量修改成功") },
            confirmButton = {
                Button(onClick = { showBatchCompleteDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showBatchCompleteDialog = false
                    onExit()
                }) {
                    Text("退出")
                }
            }
        )
    }
    
    if (showCoverSelectionSheet) {
        CoverSelectionSheet(
            onDismiss = { showCoverSelectionSheet = false },
            onFromSystemFile = {
                showCoverSelectionSheet = false
                pickImageLauncher.launch("image/*")
            },
            onFromMusicLibrary = {
                showCoverSelectionSheet = false
                showMusicLibraryCoverSheet = true
            }
        )
    }
    
    if (showMusicLibraryCoverSheet) {
        MusicLibraryCoverSelectionSheet(
            onDismiss = { showMusicLibraryCoverSheet = false },
            onSelectSong = { filePath ->
                scope.launch {
                    val coverData = loadAudioCover(context, filePath)
                    if (coverData != null) {
                        val bitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
                        coverBitmap = bitmap
                        coverChanged = true
                        coverRemoved = false
                        modifiedField = modifiedField.copy(cover = true, canRedoCover = false)
                    }
                    showMusicLibraryCoverSheet = false
                }
            }
        )
    }
    
    if (showLyricsSelectionSheet && lyricsDialogAudio != null) {
        AudioOptionsDialog(
            audio = lyricsDialogAudio,
            autoDetectEmbeddedLyricsType = autoDetectEmbeddedLyricsType,
            initialCoverBitmap = coverBitmap,
            onDismiss = { showLyricsSelectionSheet = false },
            onEditLyrics = { lyricsContent, lyricsFormat ->
                showLyricsSelectionSheet = false
                onOpenLyricTiming(lyricsContent, lyricsFormat)
            },
            showEditMetadataButton = false
        )
    }
    
    if (showFieldSelectionSheet) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        val fieldValues = when (currentSelectionField) {
            "title" -> listOf(KEEP) + batchEditFieldValues.titles.toList()
            "artist" -> listOf(KEEP) + batchEditFieldValues.artists.toList()
            "album" -> listOf(KEEP) + batchEditFieldValues.albums.toList()
            "year" -> listOf(KEEP) + batchEditFieldValues.years.toList()
            "trackNumber" -> listOf(KEEP) + batchEditFieldValues.trackNumbers.toList()
            "discNumber" -> listOf(KEEP) + batchEditFieldValues.discNumbers.toList()
            "genre" -> listOf(KEEP) + batchEditFieldValues.genres.toList()
            "albumArtist" -> listOf(KEEP) + batchEditFieldValues.albumArtists.toList()
            "composer" -> listOf(KEEP) + batchEditFieldValues.composers.toList()
            "lyricist" -> listOf(KEEP) + batchEditFieldValues.lyricists.toList()
            "comment" -> listOf(KEEP) + batchEditFieldValues.comments.toList()
            "copyrightInfo" -> listOf(KEEP) + batchEditFieldValues.copyrightInfos.toList()
            "lyrics" -> listOf(KEEP) + batchEditFieldValues.lyricsValues.toList()
            else -> {
                // 自定义字段
                listOf(KEEP) + (batchEditFieldValues.customFieldValues[currentSelectionField]?.toList() ?: emptyList())
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { showFieldSelectionSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "选择${currentSelectionField}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                fieldValues.forEach { value ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                when (currentSelectionField) {
                                    "title" -> {
                                        title = value
                                        modifiedField = modifiedField.copy(title = value != KEEP, canRedoTitle = false)
                                    }
                                    "artist" -> {
                                        artist = value
                                        modifiedField = modifiedField.copy(artist = value != KEEP, canRedoArtist = false)
                                    }
                                    "album" -> {
                                        album = value
                                        modifiedField = modifiedField.copy(album = value != KEEP, canRedoAlbum = false)
                                    }
                                    "year" -> {
                                        year = value
                                        modifiedField = modifiedField.copy(year = value != KEEP, canRedoYear = false)
                                    }
                                    "trackNumber" -> {
                                        trackNumber = value
                                        modifiedField = modifiedField.copy(trackNumber = value != KEEP, canRedoTrackNumber = false)
                                    }
                                    "discNumber" -> {
                                        discNumber = value
                                        modifiedField = modifiedField.copy(discNumber = value != KEEP, canRedoDiscNumber = false)
                                    }
                                    "genre" -> {
                                        genre = value
                                        modifiedField = modifiedField.copy(genre = value != KEEP, canRedoGenre = false)
                                    }
                                    "albumArtist" -> {
                                        albumArtist = value
                                        modifiedField = modifiedField.copy(albumArtist = value != KEEP, canRedoAlbumArtist = false)
                                    }
                                    "composer" -> {
                                        composer = value
                                        modifiedField = modifiedField.copy(composer = value != KEEP, canRedoComposer = false)
                                    }
                                    "lyricist" -> {
                                        lyricist = value
                                        modifiedField = modifiedField.copy(lyricist = value != KEEP, canRedoLyricist = false)
                                    }
                                    "comment" -> {
                                        comment = value
                                        modifiedField = modifiedField.copy(comment = value != KEEP, canRedoComment = false)
                                    }
                                    "copyrightInfo" -> {
                                        copyright = value
                                        modifiedField = modifiedField.copy(copyrightInfo = value != KEEP, canRedoCopyrightInfo = false)
                                    }
                                    "lyrics" -> {
                                        lyrics = value
                                        modifiedField = modifiedField.copy(lyrics = value != KEEP, canRedoLyrics = false)
                                    }
                                    else -> {
                                        // 自定义字段
                                        customFieldValues[currentSelectionField] = value
                                        modifiedField = modifiedField.copy(
                                            customFields = if (value != KEEP) {
                                                modifiedField.customFields + (currentSelectionField to true)
                                            } else {
                                                modifiedField.customFields - currentSelectionField
                                            },
                                            canRedoCustomFields = modifiedField.canRedoCustomFields - currentSelectionField
                                        )
                                    }
                                }
                                showFieldSelectionSheet = false
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = value,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AccompanimentMetadataField(
    path: String?,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val fieldTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "伴奏",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = fieldContainerColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!path.isNullOrBlank()) {
                    Text(
                        text = path,
                        fontSize = 16.sp,
                        color = fieldTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "添加伴奏",
                            color = fieldTextColor
                        )
                    }
                    OutlinedButton(
                        onClick = onRemoveClick,
                        enabled = !path.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("移除伴奏")
                    }
                }
            }
        }
    }
}

@Composable
fun ModifiableMetadataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isModified: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFocused: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    isBatchEdit: Boolean = false,
    showDropdown: Boolean = true,
    showMoreButton: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
    onMoreButtonPositioned: ((MenuAnchorPosition) -> Unit)? = null,
    onShowDropdown: (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val lineLimitPrefKey = remember(label) { "metadata_field_unlimited_lines_$label" }
    var unlimitedLines by rememberSaveable(label) {
        mutableStateOf(prefs.getBoolean(lineLimitPrefKey, false))
    }
    val fieldContainerColor = if (isModified) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }
    val fieldTextColor = if (isModified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val fieldPlaceholderColor = fieldTextColor.copy(alpha = 0.72f)
    val effectiveMaxLines = if (unlimitedLines) Int.MAX_VALUE else maxLines
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isModified) "$label（已修改）" else label,
                fontSize = 14.sp,
                color = if (isModified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    unlimitedLines = !unlimitedLines
                    prefs.edit().putBoolean(lineLimitPrefKey, unlimitedLines).apply()
                }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBatchEdit && showDropdown) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.down),
                            contentDescription = "选择",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onShowDropdown?.invoke() }
                        )
                    }
                } else if (isBatchEdit && !showDropdown) {
                    Spacer(modifier = Modifier.size(48.dp))
                } else {
                    if (canRedo) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.redo),
                                contentDescription = "重做",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onRedo)
                            )
                        }
                    }
                    if (isModified) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.undo),
                                contentDescription = "撤销",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onUndo)
                            )
                        }
                    }
                    if (!isModified && !canRedo && !showMoreButton) {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    if (showMoreButton) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .onGloballyPositioned { coordinates ->
                                        val bounds = coordinates.boundsInRoot()
                                        val centerX = bounds.center.x
                                        val centerY = bounds.center.y
                                        onMoreButtonPositioned?.invoke(
                                            MenuAnchorPosition(
                                                x = with(density) { centerX.toDp().value },
                                                y = with(density) { centerY.toDp().value }
                                            )
                                        )
                                    }
                                    .clickable { onMoreClick?.invoke() }
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = fieldContainerColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocused?.invoke()
                    }
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    minLines = minLines,
                    maxLines = effectiveMaxLines,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = fieldTextColor,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = "请输入$label",
                                color = fieldPlaceholderColor,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (value.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                onValueChange("")
                                focusRequester.requestFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "清空",
                            tint = fieldTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

suspend fun getVideoCoverPath(context: Context, audioPath: String?, albumName: String? = null): String? {
    return withContext(Dispatchers.IO) {
        audioPath?.let { path ->
            val file = File(path)
            val parentDir = file.parentFile
            
            var videoCoverFile: File? = null
            
            if (albumName?.isNotEmpty() == true) {
                videoCoverFile = File(parentDir, "$albumName.mp4")
                if (videoCoverFile.exists()) {
                    return@withContext videoCoverFile.absolutePath
                }
            }
            
            val baseName = file.nameWithoutExtension
            videoCoverFile = File(parentDir, "$baseName.mp4")
            if (videoCoverFile.exists()) videoCoverFile.absolutePath else null
        }
    }
}

private fun resolveMediaStoreAudioUri(
    context: Context,
    filePath: String,
    mediaStoreId: Long = -1L
): Uri? {
    return try {
        if (mediaStoreId > 0L) {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
        }
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val args = arrayOf(filePath)
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

private suspend fun readAudioTagDataWithFallback(
    context: Context,
    filePath: String,
    mediaStoreId: Long = -1L
): AudioTagData {
    val file = File(filePath)
    val primary = runCatching {
        if (!file.exists()) throw FileNotFoundException("Audio file does not exist: $filePath")
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            AudioTagReader.read(pfd, true)
        } finally {
            pfd.close()
        }
    }.getOrNull()
    if (primary != null) return primary

    val uri = resolveMediaStoreAudioUri(context, filePath, mediaStoreId)
        ?: throw FileNotFoundException("Cannot resolve MediaStore uri for: $filePath")
    val uriPfd = context.contentResolver.openFileDescriptor(uri, "r")
        ?: throw FileNotFoundException("Cannot open audio from uri: $uri")
    return try {
        AudioTagReader.read(uriPfd, true)
    } finally {
        uriPfd.close()
    }
}

private fun readTagLibPropertyMapWithFallback(
    context: Context,
    filePath: String,
    mediaStoreId: Long = -1L
): Map<String, Array<String>>? {
    val file = File(filePath)
    val fromFile = runCatching {
        if (!file.exists()) throw FileNotFoundException("Audio file does not exist: $filePath")
        val tagPfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val nativeFd = tagPfd.dup().detachFd()
            TagLib.getMetadata(nativeFd, false)?.propertyMap
        } finally {
            tagPfd.close()
        }
    }.getOrNull()
    if (fromFile != null) return fromFile

    val uri = resolveMediaStoreAudioUri(context, filePath, mediaStoreId) ?: return null
    val uriPfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
    return try {
        val nativeFd = uriPfd.dup().detachFd()
        TagLib.getMetadata(nativeFd, false)?.propertyMap
    } finally {
        uriPfd.close()
    }
}

suspend fun loadMetadata(
    context: Context,
    filePath: String?,
    mediaStoreId: Long = -1L,
    onLoaded: (AudioTagData, Bitmap?, String?, String?, String?, Map<String, String>) -> Unit,
    onError: (Throwable) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            if (filePath == null) {
                withContext(Dispatchers.Main) {
                    onError(IllegalArgumentException("filePath is null"))
                }
                return@withContext
            }
            
            val data = readAudioTagDataWithFallback(
                context = context,
                filePath = filePath,
                mediaStoreId = mediaStoreId
            )
            
            var coverBitmap = data.pictures.firstOrNull()?.let { pic ->
                BitmapFactory.decodeByteArray(pic.data, 0, pic.data.size)
            }
            
            var copyrightValue: String? = null
            var lyricsValue: String? = null
            var genreValue: String? = null
            val customFieldsMap = mutableMapOf<String, String>()
            
            // 从SharedPreferences获取启用的字段
            val prefs = context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE)
            val enabledFields = prefs.getStringSet("enabledMetadataFields", emptySet())?.toList() ?: emptyList()
            
            try {
                val props = readTagLibPropertyMapWithFallback(
                    context = context,
                    filePath = filePath,
                    mediaStoreId = mediaStoreId
                )
                
                if (props != null) {
                    
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

                    fun joinedOf(vararg keys: String): String? {
                        for (key in keys) {
                            val arr = props[key]
                            if (!arr.isNullOrEmpty()) {
                                val values = arr
                                    .flatMap { item -> item.split(GENRE_SPLIT_REGEX) }
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .distinct()
                                if (values.isNotEmpty()) return values.joinToString("/")
                            }
                        }
                        return null
                    }
                    
                    copyrightValue = firstOf("COPYRIGHT", "COPYRIGHTS", "COPYRIGHTINFO")
                    lyricsValue = firstOf("LYRICS", "UNSYNCED LYRICS", "UNSYNCEDLYRICS", "USLT", "LYRIC", "LYRICSENG")
                    genreValue = joinedOf("GENRE")

                    // 读取自定义字段
                    enabledFields.forEach { field ->
                        val value = firstOf(field.uppercase()) ?: ""
                        customFieldsMap[field] = value
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading metadata with TagLib", e)
            }
            
            withContext(Dispatchers.Main) {
                onLoaded(data, coverBitmap, copyrightValue, lyricsValue, genreValue, customFieldsMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading metadata", e)
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }
}

suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from uri", e)
            null
        }
    }
}

suspend fun saveVideoCover(
    context: Context,
    audioPath: String,
    videoUri: Uri,
    albumName: String
): VideoSaveResult {
    return withContext(Dispatchers.IO) {
        try {
            if (!hasStoragePermission(context)) {
                return@withContext VideoSaveResult(false)
            }
            
            if (albumName.isNotEmpty() && containsIllegalFileNameChars(albumName)) {
                val illegalChars = getIllegalChars(albumName)
                return@withContext VideoSaveResult(false, "存在非法字符\"$illegalChars\"，无法重命名。")
            }
            
            val audioFile = File(audioPath)
            val parentDir = audioFile.parentFile
            val videoCoverFile = if (albumName.isNotEmpty()) {
                File(parentDir, "$albumName.mp4")
            } else {
                val baseName = audioFile.nameWithoutExtension
                File(parentDir, "$baseName.mp4")
            }
            
            context.contentResolver.openInputStream(videoUri)?.use { input ->
                java.io.FileOutputStream(videoCoverFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            VideoSaveResult(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving video cover", e)
            VideoSaveResult(false, e.message)
        }
    }
}

suspend fun saveImageToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "LunaBeat")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                var outputStream: OutputStream? = null
                try {
                    outputStream = context.contentResolver.openOutputStream(it)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.flush()
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            context.contentResolver.update(it, contentValues, null, null)
                        }
                        
                        return@withContext true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving image to gallery", e)
                } finally {
                    outputStream?.close()
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to gallery", e)
            false
        }
    }
}

fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

fun containsIllegalFileNameChars(name: String): Boolean {
    val illegalChars = listOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    return name.any { it in illegalChars }
}

fun getIllegalChars(name: String): String {
    val illegalChars = listOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    return name.filter { it in illegalChars }.toSet().joinToString("")
}

data class SaveResult(
    val success: Boolean,
    val needPermission: Boolean = false,
    val recoverableIntentSender: IntentSender? = null
)

data class VideoSaveResult(
    val success: Boolean,
    val errorMessage: String? = null
)

data class AccompanimentOperationResult(
    val success: Boolean,
    val needPermission: Boolean = false,
    val needPermissionForTree: Boolean = false,
    val path: String? = null,
    val errorMessage: String? = null
)

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else {
                    null
                }
            }
    }.getOrNull()
}

private fun resolveUriExtension(context: Context, uri: Uri): String {
    val displayName = queryDisplayName(context, uri).orEmpty()
    val fromDisplayName = displayName.substringAfterLast('.', "").trim()
    if (fromDisplayName.isNotEmpty()) return fromDisplayName

    val mimeType = context.contentResolver.getType(uri).orEmpty()
    val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
    if (fromMime.isNotEmpty()) return fromMime

    val fromPathSegment = uri.lastPathSegment.orEmpty().substringAfterLast('.', "").trim()
    return fromPathSegment
}

private fun isCompanionNameMatch(fileName: String?, sourceBaseName: String): Boolean {
    if (fileName.isNullOrBlank() || sourceBaseName.isBlank()) return false
    val candidate = fileName.trim()
    val lowerCandidate = candidate.lowercase()
    val lowerBase = sourceBaseName.lowercase()
    if (lowerCandidate == lowerBase) return true
    if (lowerCandidate.startsWith("$lowerBase.")) return true
    val withoutLast = candidate.substringBeforeLast('.', candidate)
    if (withoutLast.equals(sourceBaseName, ignoreCase = true)) return true
    val withoutTwo = withoutLast.substringBeforeLast('.', withoutLast)
    return withoutTwo.equals(sourceBaseName, ignoreCase = true)
}

fun findAccompanimentFileForAudio(audioPath: String?): File? {
    if (audioPath.isNullOrBlank()) return null
    val sourceAudio = File(audioPath)
    if (!sourceAudio.exists()) return null
    val dir = File(ACCOMPANIMENT_DIR_PATH)
    if (!dir.exists() || !dir.isDirectory) return null
    val baseName = sourceAudio.nameWithoutExtension
    return dir.listFiles()
        ?.filter { it.isFile }
        ?.sortedBy { it.name }
        ?.firstOrNull { isCompanionNameMatch(it.name, baseName) }
}

fun getAccompanimentPathForAudio(audioPath: String?): String? {
    return findAccompanimentFileForAudio(audioPath)?.absolutePath
}

private fun getAccompanimentTreeUri(context: Context): Uri? {
    val prefs = context.getSharedPreferences(ACCOMPANIMENT_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(ACCOMPANIMENT_TREE_URI_KEY, null) ?: return null
    return runCatching { Uri.parse(raw) }.getOrNull()
}

private fun saveAccompanimentTreeUri(context: Context, uri: Uri?) {
    val prefs = context.getSharedPreferences(ACCOMPANIMENT_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putString(ACCOMPANIMENT_TREE_URI_KEY, uri?.toString()).apply()
}

private fun hasPersistedTreeReadWritePermission(context: Context, treeUri: Uri): Boolean {
    return context.contentResolver.persistedUriPermissions.any { permission ->
        permission.uri == treeUri && permission.isReadPermission && permission.isWritePermission
    }
}

private fun findOrCreateChildDirectory(parent: DocumentFile, name: String): DocumentFile? {
    return parent.findFile(name)?.takeIf { it.isDirectory } ?: parent.createDirectory(name)
}

private fun resolveMusicDirectoryFromTreeRoot(treeRoot: DocumentFile): DocumentFile? {
    val rootName = treeRoot.name?.trim()?.lowercase().orEmpty()
    if (rootName == "music") return treeRoot
    return treeRoot.findFile("Music")?.takeIf { it.isDirectory }
        ?: treeRoot.findFile("music")?.takeIf { it.isDirectory }
}

private fun isMusicDirectoryTreeUri(context: Context, treeUri: Uri): Boolean {
    val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
    val treeDocIdNormalized = treeDocId?.lowercase().orEmpty()
    if (treeDocIdNormalized == "primary:music" || treeDocIdNormalized.endsWith(":music")) {
        return true
    }

    val treeRoot = DocumentFile.fromTreeUri(context, treeUri) ?: return false
    val treeName = treeRoot.name?.trim()?.lowercase().orEmpty()
    if (treeName == "music") return true

    val musicChild = treeRoot.findFile("Music")?.takeIf { it.isDirectory }
        ?: treeRoot.findFile("music")?.takeIf { it.isDirectory }
    return musicChild != null
}

suspend fun copyAccompanimentToDefaultPath(
    context: Context,
    audioPath: String?,
    sourceUri: Uri,
    preferredTreeUri: Uri? = null
): AccompanimentOperationResult = withContext(Dispatchers.IO) {
    try {
        if (!hasStoragePermission(context)) {
            return@withContext AccompanimentOperationResult(success = false, needPermission = true)
        }
        if (audioPath.isNullOrBlank()) {
            return@withContext AccompanimentOperationResult(success = false, errorMessage = "未找到当前歌曲路径")
        }

        val sourceAudio = File(audioPath)
        if (!sourceAudio.exists()) {
            return@withContext AccompanimentOperationResult(success = false, errorMessage = "当前歌曲文件不存在")
        }

        val baseName = sourceAudio.nameWithoutExtension
        val extension = resolveUriExtension(context, sourceUri)
        val targetName = if (extension.isNotBlank()) "$baseName.$extension" else baseName
        val targetDir = File(ACCOMPANIMENT_DIR_PATH)
        val directWriteResult = runCatching {
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                return@runCatching null
            }
            if (!targetDir.isDirectory) return@runCatching null
            val targetFile = File(targetDir, targetName)
            targetDir.listFiles()
                ?.filter { it.isFile && it.nameWithoutExtension.equals(baseName, ignoreCase = true) && it.absolutePath != targetFile.absolutePath }
                ?.forEach { oldFile -> runCatching { oldFile.delete() } }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return@runCatching null
            targetFile.absolutePath
        }.getOrNull()
        if (!directWriteResult.isNullOrBlank()) {
            return@withContext AccompanimentOperationResult(success = true, path = directWriteResult)
        }

        // 只有 Android 10 才尝试 SAF 授权方式，其他版本直接失败
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
            return@withContext AccompanimentOperationResult(
                success = false,
                errorMessage = "无法创建或写入伴奏文件"
            )
        }

        val treeUri = preferredTreeUri ?: getAccompanimentTreeUri(context)
        if (treeUri == null) {
            return@withContext AccompanimentOperationResult(
                success = false,
                needPermissionForTree = true,
                errorMessage = "无法创建伴奏目录，请授权 Music 目录访问权限"
            )
        }

        val treeRoot = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext AccompanimentOperationResult(
                success = false,
                needPermissionForTree = true,
                errorMessage = "伴奏目录权限已失效，请重新授权"
            )
        val musicDir = resolveMusicDirectoryFromTreeRoot(treeRoot)
            ?: return@withContext AccompanimentOperationResult(
                success = false,
                needPermissionForTree = true,
                errorMessage = "无法访问 Music 目录，请重新授权"
            )
        val singDir = findOrCreateChildDirectory(musicDir, ".sing")
            ?: return@withContext AccompanimentOperationResult(
                success = false,
                needPermissionForTree = true,
                errorMessage = "无法访问 .sing 目录，请重新授权"
            )

        singDir.listFiles()
            .orEmpty()
            .filter { file ->
                file.isFile && isCompanionNameMatch(file.name, baseName)
            }
            .forEach { oldFile -> runCatching { oldFile.delete() } }

        val mimeType = if (extension.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "audio/*"
        } else {
            "audio/*"
        }
        val targetDoc = singDir.createFile(mimeType, baseName)
            ?: return@withContext AccompanimentOperationResult(
                success = false,
                errorMessage = "在授权目录创建伴奏文件失败"
            )

        val wrote = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(targetDoc.uri, "w")?.use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
        }.isSuccess

        if (!wrote) {
            return@withContext AccompanimentOperationResult(
                success = false,
                errorMessage = "写入伴奏文件失败"
            )
        }

        val resolvedPath = findAccompanimentFileForAudio(audioPath)?.absolutePath
        val resolvedName = targetDoc.name ?: targetName
        AccompanimentOperationResult(
            success = true,
            path = resolvedPath ?: ACCOMPANIMENT_DIR_PATH + File.separator + resolvedName
        )
    } catch (e: SecurityException) {
        Log.e(TAG, "No permission to save accompaniment file", e)
        // 只有 Android 10 才返回需要授权的错误
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            AccompanimentOperationResult(
                success = false,
                needPermissionForTree = true,
                errorMessage = "没有目录访问权限，请重新授权"
            )
        } else {
            AccompanimentOperationResult(
                success = false,
                errorMessage = "没有目录访问权限"
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error copying accompaniment file", e)
        AccompanimentOperationResult(
            success = false,
            errorMessage = e.message ?: "添加伴奏失败"
        )
    }
}


suspend fun removeAccompanimentFile(
    context: Context,
    accompanimentPath: String?
): AccompanimentOperationResult = withContext(Dispatchers.IO) {
    try {
        if (!hasStoragePermission(context)) {
            return@withContext AccompanimentOperationResult(success = false, needPermission = true)
        }
        if (accompanimentPath.isNullOrBlank()) {
            return@withContext AccompanimentOperationResult(success = false, errorMessage = "当前没有可移除的伴奏文件")
        }
        val file = File(accompanimentPath)
        if (!file.exists()) {
            return@withContext AccompanimentOperationResult(success = true)
        }
        if (!file.delete()) {
            return@withContext AccompanimentOperationResult(success = false, errorMessage = "删除伴奏文件失败")
        }
        AccompanimentOperationResult(success = true)
    } catch (e: Exception) {
        Log.e(TAG, "Error removing accompaniment file", e)
        AccompanimentOperationResult(success = false, errorMessage = e.message ?: "移除伴奏失败")
    }
}

suspend fun deleteOriginalAccompanimentSource(
    context: Context,
    sourceUri: Uri?
): AccompanimentOperationResult = withContext(Dispatchers.IO) {
    try {
        if (sourceUri == null) {
            return@withContext AccompanimentOperationResult(success = true)
        }

        if (sourceUri.scheme.equals("content", ignoreCase = true)) {
            val deletedCount = context.contentResolver.delete(sourceUri, null, null)
            return@withContext if (deletedCount > 0) {
                AccompanimentOperationResult(success = true)
            } else {
                AccompanimentOperationResult(success = false, errorMessage = "删除原文件失败")
            }
        }

        if (sourceUri.scheme.equals("file", ignoreCase = true) || sourceUri.scheme.isNullOrBlank()) {
            val filePath = sourceUri.path
            if (filePath.isNullOrBlank()) {
                return@withContext AccompanimentOperationResult(success = false, errorMessage = "原文件路径无效")
            }
            val sourceFile = File(filePath)
            return@withContext if (!sourceFile.exists() || sourceFile.delete()) {
                AccompanimentOperationResult(success = true)
            } else {
                AccompanimentOperationResult(success = false, errorMessage = "删除原文件失败")
            }
        }

        AccompanimentOperationResult(success = false, errorMessage = "不支持删除该来源的文件")
    } catch (e: SecurityException) {
        Log.e(TAG, "No permission to delete original accompaniment source", e)
        AccompanimentOperationResult(success = false, needPermission = true, errorMessage = "没有权限删除原文件")
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting original accompaniment source", e)
        AccompanimentOperationResult(success = false, errorMessage = e.message ?: "删除原文件失败")
    }
}

// 音乐库音频文件数据类
data class MusicLibraryAudioFile(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val fileSize: Long,
    val lastModified: Long = 0,
    val addedTime: Long = System.currentTimeMillis(),
    val coverCachePath: String? = null,
    val year: String = ""
) {
    val displayTitle: String
        get() = if (title.isNotEmpty()) title else File(path).nameWithoutExtension
    
    val displayArtist: String
        get() = if (artist.isNotEmpty()) artist else "未知艺术家"
    
    val displayAlbum: String
        get() = if (album.isNotEmpty()) album else "未知专辑"
}

// 从音频文件中读取封面数据
suspend fun loadAudioCover(context: Context, filePath: String): ByteArray? {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val tagData = AudioTagReader.read(pfd, true)
            pfd.close()
            
            tagData.pictures.firstOrNull()?.data
        } catch (e: Exception) {
            Log.e(TAG, "Error loading audio cover", e)
            null
        }
    }
}

// 加载音乐库缓存
fun loadMusicLibraryCache(context: Context): List<MusicLibraryAudioFile> {
    return try {
        val prefs = context.getSharedPreferences("MusicLibraryCache", Context.MODE_PRIVATE)
        val cacheJson = prefs.getString("audioFilesCache", null) ?: return emptyList()
        
        val jsonArray = org.json.JSONArray(cacheJson)
        val files = mutableListOf<MusicLibraryAudioFile>()
        
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            files.add(
                MusicLibraryAudioFile(
                    path = json.getString("path"),
                    title = json.optString("title", ""),
                    artist = json.optString("artist", ""),
                    album = json.optString("album", ""),
                    duration = json.optLong("duration", 0),
                    fileSize = json.optLong("fileSize", 0),
                    lastModified = json.optLong("lastModified", 0),
                    addedTime = json.optLong("addedTime", System.currentTimeMillis()),
                    coverCachePath = if (json.has("coverCachePath")) json.optString("coverCachePath") else null,
                    year = json.optString("year", "")
                )
            )
        }
        
        files
    } catch (e: Exception) {
        Log.e(TAG, "Error loading music library cache", e)
        emptyList()
    }
}

suspend fun loadCoverSelectionCachedAudioFiles(context: Context): List<MusicLibraryAudioFile> {
    return withContext(Dispatchers.IO) {
        val roomCached = musicLibraryCacheStore(context).loadAllPaged()
        if (roomCached.isNotEmpty()) {
            return@withContext roomCached.map {
                MusicLibraryAudioFile(
                    path = it.path,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    duration = it.duration,
                    fileSize = it.fileSize,
                    lastModified = it.lastModified,
                    addedTime = it.addedTime,
                    coverCachePath = it.coverCachePath,
                    year = it.year
                )
            }
        }
        // 兜底兼容历史缓存
        return@withContext loadMusicLibraryCache(context)
    }
}

// 从缓存加载封面位图
fun loadCoverBitmapFromCache(coverCachePath: String): Bitmap? {
    return try {
        val file = File(coverCachePath)
        if (file.exists()) {
            BitmapFactory.decodeFile(coverCachePath)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading cover bitmap from cache", e)
        null
    }
}

// 封面选择主菜单 - 独立组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverSelectionSheet(
    onDismiss: () -> Unit,
    onFromSystemFile: () -> Unit,
    onFromMusicLibrary: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            Text(
                text = "选择封面来源",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            OutlinedButton(
                onClick = onFromSystemFile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("从系统文件中选择", fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onFromMusicLibrary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("从歌曲封面中选择", fontSize = 16.sp)
            }
        }
    }
}

// 音乐库封面选择 - 独立组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryCoverSelectionSheet(
    onDismiss: () -> Unit,
    onSelectSong: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val contentNoPassNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return Velocity(0f, available.y)
            }
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var audioFiles by remember { mutableStateOf<List<MusicLibraryAudioFile>?>(null) }
    val lazyListState = rememberLazyListState()

    fun dismissWithAnimation() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    
    // 加载音乐库缓存
    LaunchedEffect(Unit) {
        audioFiles = loadCoverSelectionCachedAudioFiles(context)
    }
    
    // 过滤搜索结果
    val filteredFiles = remember(audioFiles, searchQuery) {
        if (searchQuery.isEmpty()) {
            audioFiles
        } else {
            audioFiles?.filter {
                it.displayTitle.contains(searchQuery, ignoreCase = true) ||
                it.displayArtist.contains(searchQuery, ignoreCase = true) ||
                it.displayAlbum.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = { dismissWithAnimation() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择歌曲封面",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { dismissWithAnimation() }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 搜索栏 - 参考音乐库风格
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
                    onValueChange = { searchQuery = it },
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
                                    onClick = { searchQuery = "" },
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (audioFiles == null) {
                // 加载中
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(56.dp))
                }
            } else if (filteredFiles.isNullOrEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.img),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "未找到匹配的歌曲" else "音乐库中没有歌曲",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 歌曲列表
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .nestedScroll(contentNoPassNestedScroll),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        count = filteredFiles.size,
                        key = { filteredFiles[it].path }
                    ) { index ->
                        val audioFile = filteredFiles[index]
                        MusicLibraryCoverItem(
                            audioFile = audioFile,
                            onClick = { onSelectSong(audioFile.path) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// 音乐库封面项 - 独立组件
@Composable
fun MusicLibraryCoverItem(
    audioFile: MusicLibraryAudioFile,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverBitmap by produceState<Bitmap?>(initialValue = null, audioFile.path, audioFile.coverCachePath) {
        value = withContext(Dispatchers.IO) {
            val fromCache = audioFile.coverCachePath
                ?.takeIf { it.isNotBlank() }
                ?.let { loadCoverBitmapFromCache(it) }
            if (fromCache != null) {
                fromCache
            } else {
                val coverBytes = loadAudioCover(context, audioFile.path)
                coverBytes?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audioFile.displayTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${audioFile.displayArtist} - ${audioFile.displayAlbum}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

suspend fun saveMetadata(
    context: Context,
    filePath: String,
    mediaStoreId: Long = -1L,
    title: String,
    artist: String,
    album: String,
    year: String,
    trackNumber: String,
    discNumber: String,
    genre: String,
    albumArtist: String,
    composer: String,
    lyricist: String,
    comment: String,
    copyright: String,
    lyrics: String,
    coverBitmap: Bitmap?,
    coverRemoved: Boolean,
    oldCoverData: ByteArray?,
    keepFields: String = "[KEEP]",
    titleKeep: Boolean = false,
    artistKeep: Boolean = false,
    albumKeep: Boolean = false,
    yearKeep: Boolean = false,
    trackNumberKeep: Boolean = false,
    discNumberKeep: Boolean = false,
    genreKeep: Boolean = false,
    albumArtistKeep: Boolean = false,
    composerKeep: Boolean = false,
    lyricistKeep: Boolean = false,
    commentKeep: Boolean = false,
    copyrightKeep: Boolean = false,
    lyricsKeep: Boolean = false,
    coverKeep: Boolean = true,
    customFields: Map<String, String> = emptyMap(),
    customFieldKeep: Map<String, Boolean> = emptyMap()
): SaveResult {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext SaveResult(false)
            }
            
            if (!hasStoragePermission(context)) {
                return@withContext SaveResult(false, needPermission = true)
            }
            
            val updates = mutableMapOf<String, String>()
            if (!titleKeep) updates["TITLE"] = title
            if (!artistKeep) updates["ARTIST"] = artist
            if (!albumKeep) updates["ALBUM"] = album
            if (!yearKeep) updates["DATE"] = year
            if (!trackNumberKeep) updates["TRACKNUMBER"] = trackNumber
            if (!discNumberKeep) updates["DISCNUMBER"] = discNumber
            if (!genreKeep) updates["GENRE"] = genre
            if (!albumArtistKeep) updates["ALBUMARTIST"] = albumArtist
            if (!composerKeep) updates["COMPOSER"] = composer
            if (!lyricistKeep) updates["LYRICIST"] = lyricist
            if (!commentKeep) updates["COMMENT"] = comment
            if (!copyrightKeep) updates["COPYRIGHT"] = copyright
            if (!lyricsKeep) updates["LYRICS"] = lyrics
            
            // 添加自定义字段
            customFields.forEach { (field, value) ->
                if (customFieldKeep[field] != true) {
                    updates[field.uppercase()] = value
                }
            }
            
            var success = true
            fun openWritablePfdOrResult(): Pair<ParcelFileDescriptor?, SaveResult?> {
                return try {
                    Pair(openAudioReadWritePfd(context, filePath, mediaStoreId), null)
                } catch (e: RecoverableSecurityException) {
                    Pair(
                        null,
                        SaveResult(
                            success = false,
                            recoverableIntentSender = e.userAction.actionIntent.intentSender
                        )
                    )
                } catch (e: SecurityException) {
                    val needAllFilesPermission =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasStoragePermission(context)
                    Pair(
                        null,
                        SaveResult(
                            success = false,
                            needPermission = needAllFilesPermission
                        )
                    )
                }
            }

            // 只有有字段更新时才写标签
            if (updates.isNotEmpty()) {
                val (pfd, openResult) = openWritablePfdOrResult()
                if (openResult != null) return@withContext openResult
                if (pfd == null) return@withContext SaveResult(false)
                try {
                    success = AudioTagWriter.writeTags(pfd, updates, true)
                } finally {
                    pfd.close()
                }
            }
            
            // 只有封面修改时才处理封面
            if (!coverKeep) {
                if (coverBitmap != null) {
                    val byteArray = java.io.ByteArrayOutputStream()
                    coverBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArray)
                    val picData = byteArray.toByteArray()
                    
                    val newPic = AudioPicture(
                        data = picData,
                        mimeType = "image/jpeg",
                        pictureType = "Front Cover"
                    )
                    
                    val (picPfd, openResult) = openWritablePfdOrResult()
                    if (openResult != null) return@withContext openResult
                    if (picPfd == null) return@withContext SaveResult(false)
                    try {
                        AudioTagWriter.writePictures(picPfd, listOf(newPic))
                    } finally {
                        picPfd.close()
                    }
                } else if (coverRemoved && oldCoverData != null) {
                    val (picPfd, openResult) = openWritablePfdOrResult()
                    if (openResult != null) return@withContext openResult
                    if (picPfd == null) return@withContext SaveResult(false)
                    try {
                        AudioTagWriter.writePictures(picPfd, emptyList())
                    } finally {
                        picPfd.close()
                    }
                }
            }
            
            SaveResult(success)
        } catch (e: RecoverableSecurityException) {
            SaveResult(
                success = false,
                recoverableIntentSender = e.userAction.actionIntent.intentSender
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving metadata", e)
            SaveResult(false)
        }
    }
}

private fun openAudioReadWritePfd(
    context: Context,
    filePath: String,
    mediaStoreId: Long = -1L
): ParcelFileDescriptor? {
    val direct = try {
        val file = File(filePath)
        if (!file.exists()) {
            null
        } else {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        }
    } catch (e: RecoverableSecurityException) {
        throw e
    } catch (_: Exception) {
        null
    }
    if (direct != null) return direct

    val uri = resolveMediaStoreAudioUri(context, filePath, mediaStoreId) ?: return null
    return context.contentResolver.openFileDescriptor(uri, "rw")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalfScreenPreviewDialog(
    previewType: String,
    coverBitmap: Bitmap?,
    videoCoverPath: String?,
    onDismiss: () -> Unit,
    onSwitchType: (String) -> Unit,
    hasImageCover: Boolean,
    hasVideoCover: Boolean,
    onSaveImage: () -> Unit,
    onCropClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val playerState = remember {
        mutableStateOf<ExoPlayer?>(null)
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> playerState.value?.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (previewType == "video" && videoCoverPath != null) {
                        playerState.value?.play()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> playerState.value?.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerState.value?.release()
        }
    }
    
    LaunchedEffect(previewType, videoCoverPath) {
        if (previewType == "video" && videoCoverPath != null) {
            Log.d(TAG, "准备播放视频: $videoCoverPath")
            val videoFile = File(videoCoverPath)
            if (videoFile.exists()) {
                Log.d(TAG, "视频文件存在，大小: ${videoFile.length()} bytes")
                
                val existingPlayer = playerState.value
                if (existingPlayer != null) {
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
                    existingPlayer.setMediaItem(mediaItem)
                    existingPlayer.prepare()
                    existingPlayer.playWhenReady = true
                    Log.d(TAG, "使用现有播放器，已重置媒体项")
                } else {
                    val player = ExoPlayer.Builder(context).build().apply {
                        repeatMode = ExoPlayer.REPEAT_MODE_ALL
                        val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                    playerState.value = player
                    Log.d(TAG, "创建新播放器，开始播放")
                }
            } else {
                Log.e(TAG, "视频文件不存在: $videoCoverPath")
            }
        } else {
            playerState.value?.pause()
            playerState.value?.clearMediaItems()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (previewType == "image") "图片封面" else "视频封面",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (previewType == "image" && coverBitmap != null) {
                        IconButton(onClick = onCropClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.cutpic),
                                contentDescription = "裁剪图片",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onSaveImage) {
                            Icon(
                                painter = painterResource(id = R.drawable.save),
                                contentDescription = "保存到相册",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            if (hasImageCover && hasVideoCover) {
                TabRow(
                    selectedTabIndex = if (previewType == "image") 0 else 1,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = previewType == "image",
                        onClick = { if (hasImageCover) onSwitchType("image") }
                    ) {
                        Text("图片封面", modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Tab(
                        selected = previewType == "video",
                        onClick = { if (hasVideoCover) onSwitchType("video") }
                    ) {
                        Text("视频封面", modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                if (previewType == "image" && coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = "封面预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (previewType == "video" && videoCoverPath != null) {
                    val videoFile = File(videoCoverPath)
                    val currentPlayer = playerState.value
                    if (videoFile.exists() && currentPlayer != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = true
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { playerView ->
                                playerView.player = currentPlayer
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!videoFile.exists()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                contentDescription = "视频文件不存在",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "视频文件不存在",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSelectionSheet(
    audioPath: String,
    onDismiss: () -> Unit,
    onStartEdit: (String?, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var embeddedLyrics by remember { mutableStateOf<String?>(null) }
    var externalLyrics by remember { mutableStateOf<String?>(null) }
    var externalLyricsPath by remember { mutableStateOf<String?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(true) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedFormat by remember { mutableStateOf(0) }
    var showLyricsPreview by remember { mutableStateOf(false) }
    var previewLyricsContent by remember { mutableStateOf("") }
    var previewLyricsTitle by remember { mutableStateOf("") }
    
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
    
    val isStartButtonEnabled by remember { derivedStateOf { 
        !hasLyrics || selectedSource != null
    } }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(Unit) {
        sheetState.show()
    }
    
    LaunchedEffect(audioPath) {
        isLoadingLyrics = true
        
        embeddedLyrics = withContext(Dispatchers.IO) {
            com.example.LyricBox.utils.AudioMetadataReader.readLyrics(audioPath)
        }
        
        val ttmlFile = java.io.File(audioPath).let { audioFile ->
            java.io.File(audioFile.parent, audioFile.nameWithoutExtension + ".ttml")
        }
        
        if (ttmlFile.exists()) {
            externalLyrics = withContext(Dispatchers.IO) {
                try {
                    ttmlFile.readText()
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading external TTML", e)
                    null
                }
            }
            externalLyricsPath = ttmlFile.absolutePath
        }
        
        // 只有当只有一个来源时才自动选择
        if (embeddedLyrics != null && externalLyrics == null) {
            selectedSource = "embedded"
            selectedFormat = detectLyricsFormat(embeddedLyrics!!)
        } else if (embeddedLyrics == null && externalLyrics != null) {
            selectedSource = "external"
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
            if (isLoadingLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator(modifier = Modifier.size(48.dp))
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
                        LyricsSourceCardEdit(
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
                        LyricsSourceCardEdit(
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
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        when {
                            selectedSource == "external" && externalLyrics != null -> {
                                onStartEdit(externalLyrics, "TTML歌词")
                            }
                            selectedSource == "embedded" && embeddedLyrics != null -> {
                                onStartEdit(embeddedLyrics, formats[selectedFormat])
                            }
                            hasExternal && !hasEmbedded -> {
                                onStartEdit(externalLyrics, "TTML歌词")
                            }
                            !hasLyrics -> {
                                onStartEdit(null, "")
                            }
                        }
                    },
                    enabled = isStartButtonEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("开始编辑歌词")
                }
            }
        }
    }
    
    if (showLyricsPreview) {
        Dialog(
            onDismissRequest = { showLyricsPreview = false },
            properties = DialogProperties()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(min = 300.dp, max = 500.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previewLyricsTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showLyricsPreview = false }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = "关闭"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = previewLyricsContent,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showLyricsPreview = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsSourceCardEdit(
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
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onPreview) {
                Text("查看")
            }
        }
    }
}
