package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.ui.theme.DarkModeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val APP_SETTINGS_PREFS_NAME = "AppSettings"
private const val PREF_KEY_AM_REGION = "amRegion"
private const val DEFAULT_AM_REGION = "HK_SC"
private const val PREF_KEY_AM_TOKEN_SOURCE = "amTokenSource"
private const val PREF_KEY_AM_USER_TOKEN = "amUserToken"
private const val PREF_KEY_AM_CLOUDFLARE_URL = "amCloudflareUrl"
private const val PREF_KEY_AM_COUNTRY = "amCountry"
private const val PREF_KEY_AM_URL = "amUrl"
private const val PREF_KEY_AM_URL_NAME = "amUrlName"
private const val PREF_KEY_AM_URL_NAME_CONTRIBUTOR = "amUrlNameContributor"
private const val PREF_KEY_NOTICE_CONTRIBUTOR = "noticeContributor"
private const val DEFAULT_AM_TOKEN_SOURCE = "cloudflare"
private const val DEFAULT_AM_URL_NAME = "软件内置"
private const val DEFAULT_AM_URL_NAME_CONTRIBUTOR = "贡献配置"

data class AMTokenConfig(
    val tokenSource: String = DEFAULT_AM_TOKEN_SOURCE,
    val userToken: String = "",
    val cloudflareUrl: String = "",
    val country: String = "",
    val defaultUrlName: String = DEFAULT_AM_URL_NAME,
    val contributorUrlName: String = DEFAULT_AM_URL_NAME_CONTRIBUTOR,
    val noticeContributor: String = ""
)

private val AM_REGION_OPTIONS = listOf(
    "HK_SC" to "HK - 香港（转简体）",
    "HK" to "HK - 香港",
    "TW_SC" to "TW - 台湾（转简体）",
    "TW" to "TW - 台湾",
    "CN" to "CN - 中国",
    "JP" to "JP - 日本",
    "KR" to "KR - 韩国",
    "US" to "US - 美国"
)

fun getSavedAMDefaultRegion(context: Context): String {
    return context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREF_KEY_AM_REGION, DEFAULT_AM_REGION) ?: DEFAULT_AM_REGION
}

fun updateAMDefaultRegion(context: Context, region: String) {
    context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_KEY_AM_REGION, region)
        .apply()
}

fun getSavedAMTokenConfig(context: Context): AMTokenConfig {
    val prefs = context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    val fallbackCloudflareUrl = prefs.getString(PREF_KEY_AM_URL, "") ?: ""
    return AMTokenConfig(
        tokenSource = prefs.getString(PREF_KEY_AM_TOKEN_SOURCE, DEFAULT_AM_TOKEN_SOURCE) ?: DEFAULT_AM_TOKEN_SOURCE,
        userToken = prefs.getString(PREF_KEY_AM_USER_TOKEN, "") ?: "",
        cloudflareUrl = prefs.getString(PREF_KEY_AM_CLOUDFLARE_URL, fallbackCloudflareUrl) ?: fallbackCloudflareUrl,
        country = prefs.getString(PREF_KEY_AM_COUNTRY, "") ?: "",
        defaultUrlName = prefs.getString(PREF_KEY_AM_URL_NAME, DEFAULT_AM_URL_NAME) ?: DEFAULT_AM_URL_NAME,
        contributorUrlName = prefs.getString(PREF_KEY_AM_URL_NAME_CONTRIBUTOR, DEFAULT_AM_URL_NAME_CONTRIBUTOR) ?: DEFAULT_AM_URL_NAME_CONTRIBUTOR,
        noticeContributor = prefs.getString(PREF_KEY_NOTICE_CONTRIBUTOR, "") ?: ""
    )
}

fun updateAMTokenConfig(context: Context, config: AMTokenConfig) {
    context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_KEY_AM_TOKEN_SOURCE, config.tokenSource)
        .putString(PREF_KEY_AM_USER_TOKEN, config.userToken)
        .putString(PREF_KEY_AM_CLOUDFLARE_URL, config.cloudflareUrl)
        .putString(PREF_KEY_AM_COUNTRY, config.country)
        .apply()
}

fun getAMTokenSourceDisplayName(
    tokenSource: String,
    defaultUrlName: String,
    contributorUrlName: String
): String {
    return when (tokenSource) {
        "cloudflare" -> defaultUrlName
        "contributor" -> contributorUrlName
        else -> "自行填写"
    }
}

fun getAMRegionDisplayName(region: String): String {
    return AM_REGION_OPTIONS.firstOrNull { it.first == region }?.second
        ?: AM_REGION_OPTIONS.first().second
}

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
                ) { paddingValues ->
                    SettingsScreen(
                        onBack = { finish() },
                        onNavigateToMusicLibrarySettings = {
                            startActivity(Intent(this, MusicLibrarySettingsActivity::class.java))
                        },
                        onNavigateToCustomMetadataFieldsSettings = {
                            startActivity(Intent(this, CustomMetadataFieldsSettingsActivity::class.java))
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToMusicLibrarySettings: () -> Unit,
    onNavigateToCustomMetadataFieldsSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val musicLibraryPrefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val lyricPreviewPrefs = remember {
        context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val supportsLyricBlur = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    
    val savedDarkModeType = remember {
        try {
            DarkModeType.valueOf(prefs.getString("darkModeType", DarkModeType.FOLLOW_SYSTEM.name) ?: DarkModeType.FOLLOW_SYSTEM.name)
        } catch (e: Exception) { DarkModeType.FOLLOW_SYSTEM }
    }
    
    val savedSeekTimeSeconds = remember { prefs.getFloat("seekTimeSeconds", 2f) }
    val savedVibrationIntensity = remember { prefs.getString("vibrationIntensity", "weak") }
    
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showSeekTimeDialog by remember { mutableStateOf(false) }
    var showVibrationIntensityDialog by remember { mutableStateOf(false) }
    
    val savedCoverSize = remember { prefs.getInt("amCoverSize", 3000) }
    val savedQmCoverSize = remember { prefs.getInt("qmCoverSize", 1200) }
    val savedNeCoverSize = remember { prefs.getInt("neCoverSize", 1000) }
    val savedAMRegion = remember { getSavedAMDefaultRegion(context) }
    val savedAMTokenConfig = remember { getSavedAMTokenConfig(context) }
    val savedAMTokenSource = savedAMTokenConfig.tokenSource
    val savedAMUserToken = savedAMTokenConfig.userToken
    val savedAMUrlName = savedAMTokenConfig.defaultUrlName
    val savedAMUrlNameContributor = savedAMTokenConfig.contributorUrlName
    val savedNoticeContributor = savedAMTokenConfig.noticeContributor
    val savedAMCloudflareUrl = savedAMTokenConfig.cloudflareUrl
    val savedAMCountry = savedAMTokenConfig.country
    
    val savedSongClickAction = remember { musicLibraryPrefs.getString("songClickAction", "editLyrics") } ?: "editLyrics"
    var tempSongClickAction by remember { mutableStateOf(savedSongClickAction) }
    val currentSongClickAction = remember { mutableStateOf(savedSongClickAction) }
    val savedAutoDetectEmbeddedLyricsType = remember {
        musicLibraryPrefs.getBoolean("autoDetectEmbeddedLyricsType", false)
    }
    var tempAutoDetectEmbeddedLyricsType by remember { mutableStateOf(savedAutoDetectEmbeddedLyricsType) }
    val currentAutoDetectEmbeddedLyricsType = remember { mutableStateOf(savedAutoDetectEmbeddedLyricsType) }
    
    var showSongClickActionDialog by remember { mutableStateOf(false) }
    
    var tempDarkModeType by remember { mutableStateOf(savedDarkModeType) }
    var tempSeekTimeSeconds by remember { mutableFloatStateOf(savedSeekTimeSeconds) }
    var tempCoverSize by remember { mutableStateOf(savedCoverSize) }
    var tempQmCoverSize by remember { mutableStateOf(savedQmCoverSize) }
    var tempNeCoverSize by remember { mutableStateOf(savedNeCoverSize) }
    var tempAMRegion by remember { mutableStateOf(savedAMRegion) }
    var tempAMTokenSource by remember { mutableStateOf(savedAMTokenSource) }
    var tempAMUserToken by remember { mutableStateOf(savedAMUserToken) }
    var tempAMCloudflareUrl by remember { mutableStateOf(savedAMCloudflareUrl) }
    var tempAMCountry by remember { mutableStateOf(savedAMCountry) }
    
    val savedArtistSeparator = remember { prefs.getString("artistSeparator", "/") } ?: "/"
    var tempArtistSeparator by remember { mutableStateOf(savedArtistSeparator) }
    var tempArtistSeparatorCustom by remember { mutableStateOf(savedArtistSeparator) }
    var tempArtistSeparatorMode by remember { mutableStateOf(if (listOf("/", "、", "；", "，", "&").contains(savedArtistSeparator)) "preset" else "custom") }
    
    val currentDarkModeType = remember { mutableStateOf(savedDarkModeType) }
    val currentSeekTimeSeconds = remember { mutableFloatStateOf(savedSeekTimeSeconds) }
    val currentCoverSize = remember { mutableStateOf(savedCoverSize) }
    val currentQmCoverSize = remember { mutableStateOf(savedQmCoverSize) }
    val currentNeCoverSize = remember { mutableStateOf(savedNeCoverSize) }
    val currentAMRegion = remember { mutableStateOf(savedAMRegion) }
    val currentAMTokenSource = remember { mutableStateOf(savedAMTokenSource) }
    val currentAMUserToken = remember { mutableStateOf(savedAMUserToken) }
    val currentAMCloudflareUrl = remember { mutableStateOf(savedAMCloudflareUrl) }
    val currentAMCountry = remember { mutableStateOf(savedAMCountry) }
    val currentArtistSeparator = remember { mutableStateOf(savedArtistSeparator) }
    var tempVibrationIntensity by remember { mutableStateOf(savedVibrationIntensity ?: "weak") }
    val currentVibrationIntensity = remember { mutableStateOf(savedVibrationIntensity ?: "weak") }
    
    var showCoverSizeDialog by remember { mutableStateOf(false) }
    var showQmCoverSizeDialog by remember { mutableStateOf(false) }
    var showNeCoverSizeDialog by remember { mutableStateOf(false) }
    var showAMRegionDialog by remember { mutableStateOf(false) }
    var showAMTokenDialog by remember { mutableStateOf(false) }
    var showArtistSeparatorDialog by remember { mutableStateOf(false) }
    
    var autoScan by remember { mutableStateOf(musicLibraryPrefs.getBoolean("autoScan", false)) }
    var showLyricSettingsSheet by remember { mutableStateOf(false) }
    var lyricShowTranslation by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_SHOW_TRANSLATION,
                LyricPreviewActivity.DEFAULT_SHOW_TRANSLATION
            )
        )
    }
    var lyricShowTransliteration by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_SHOW_TRANSLITERATION,
                LyricPreviewActivity.DEFAULT_SHOW_TRANSLITERATION
            )
        )
    }
    var lyricBlurEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_LYRIC_BLUR,
                LyricPreviewActivity.DEFAULT_LYRIC_BLUR
            )
        )
    }
    var lyriconStatusBarEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_LYRICON_STATUS_BAR,
                LyricPreviewActivity.DEFAULT_LYRICON_STATUS_BAR
            )
        )
    }
    var lyricFontSize by remember {
        mutableFloatStateOf(
            lyricPreviewPrefs.getFloat(
                LyricPreviewActivity.KEY_FONT_SIZE,
                LyricPreviewActivity.DEFAULT_FONT_SIZE
            )
        )
    }
    var lyricFontWeight by remember {
        mutableStateOf(
            lyricPreviewPrefs.getInt(
                LyricPreviewActivity.KEY_FONT_WEIGHT,
                LyricPreviewActivity.DEFAULT_FONT_WEIGHT
            )
        )
    }
    var lyricAnimationType by remember {
        mutableStateOf(
            lyricPreviewPrefs.getInt(
                LyricPreviewActivity.KEY_INTERLUDE_ANIMATION_TYPE,
                LyricPreviewActivity.ANIMATION_TYPE_DEFAULT
            )
        )
    }
    var lyricFontOptions by remember { mutableStateOf(LyricCustomFontStore.loadOptions(context)) }
    var lyricSelectedFontId by remember { mutableStateOf(LyricCustomFontStore.getSelectedFontId(context)) }
    val scope = rememberCoroutineScope()
    val lyricFontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                LyricCustomFontStore.importFont(context, uri)
            }
            result.onSuccess { option ->
                lyricFontOptions = LyricCustomFontStore.loadOptions(context)
                lyricSelectedFontId = option.id
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "字体导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "设置",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Text(
                    text = "软件设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = "深色模式",
                    summary = currentDarkModeType.value.displayName,
                    onClick = {
                        tempDarkModeType = currentDarkModeType.value
                        showDarkModeDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Text(
                    text = "打轴设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = "快进/快退时间",
                    summary = "${currentSeekTimeSeconds.value.toInt()} 秒",
                    onClick = {
                        tempSeekTimeSeconds = currentSeekTimeSeconds.value
                        showSeekTimeDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItem(
                    title = "震动反馈",
                    summary = when (currentVibrationIntensity.value) {
                        "off" -> "关闭"
                        "weak" -> "弱"
                        "medium" -> "中等"
                        "strong" -> "强"
                        else -> "弱"
                    },
                    onClick = {
                        tempVibrationIntensity = currentVibrationIntensity.value
                        showVibrationIntensityDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Text(
                    text = "音乐库设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = "目录设置",
                    summary = "设置目录、排序方式",
                    onClick = onNavigateToMusicLibrarySettings
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItem(
                    title = "自定义元数据字段",
                    summary = "设置要显示的自定义元数据字段",
                    onClick = onNavigateToCustomMetadataFieldsSettings
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItemWithSwitch(
                    title = "进入音乐库时自动扫描",
                    summary = "开启后进入音乐库会自动扫描新文件",
                    checked = autoScan,
                    onCheckedChange = { newValue ->
                        autoScan = newValue
                        musicLibraryPrefs.edit().putBoolean("autoScan", newValue).apply()
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItem(
                    title = "点击歌曲默认操作",
                    summary = when (currentSongClickAction.value) {
                        "editLyrics" -> "编辑歌词"
                        "editMetadata" -> "编辑元数据"
                        "playMusic" -> "播放音乐"
                        else -> "编辑歌词"
                    },
                    onClick = {
                        tempSongClickAction = currentSongClickAction.value
                        tempAutoDetectEmbeddedLyricsType = currentAutoDetectEmbeddedLyricsType.value
                        showSongClickActionDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItem(
                    title = "歌词设置",
                    summary = "翻译、注音、字体、间奏动画",
                    onClick = { showLyricSettingsSheet = true }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Text(
                    text = "音频元数据下载设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = "QM 元数据封面尺寸",
                    summary = "${currentQmCoverSize.value}x${currentQmCoverSize.value}",
                    onClick = {
                        tempQmCoverSize = currentQmCoverSize.value
                        showQmCoverSizeDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItem(
                    title = "NE 元数据封面尺寸",
                    summary = "${currentNeCoverSize.value}x${currentNeCoverSize.value}",
                    onClick = {
                        tempNeCoverSize = currentNeCoverSize.value
                        showNeCoverSizeDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItem(
                    title = "AM 元数据封面尺寸",
                    summary = "${currentCoverSize.value}x${currentCoverSize.value}",
                    onClick = {
                        tempCoverSize = currentCoverSize.value
                        showCoverSizeDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItem(
                    title = "AM 默认地区",
                    summary = getAMRegionDisplayName(currentAMRegion.value),
                    onClick = {
                        tempAMRegion = currentAMRegion.value
                        showAMRegionDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItem(
                    title = "艺术家分隔符",
                    summary = if (currentArtistSeparator.value.isNotEmpty()) "\"${currentArtistSeparator.value}\"" else "\"/\"",
                    onClick = {
                        tempArtistSeparator = currentArtistSeparator.value
                        tempArtistSeparatorCustom = currentArtistSeparator.value
                        tempArtistSeparatorMode = if (listOf("/", "、", "；", "，", "&").contains(currentArtistSeparator.value)) "preset" else "custom"
                        showArtistSeparatorDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Text(
                    text = "歌词下载设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = "APPLE_MUSIC_MEDIA_USER_TOKEN",
                    summary = getAMTokenSourceDisplayName(
                        tokenSource = currentAMTokenSource.value,
                        defaultUrlName = savedAMUrlName,
                        contributorUrlName = savedAMUrlNameContributor
                    ),
                    onClick = {
                        tempAMTokenSource = currentAMTokenSource.value
                        tempAMUserToken = currentAMUserToken.value
                        tempAMCloudflareUrl = currentAMCloudflareUrl.value
                        tempAMCountry = currentAMCountry.value
                        showAMTokenDialog = true
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    if (showDarkModeDialog) {
        DarkModeDialog(
            currentValue = tempDarkModeType,
            onValueChange = { tempDarkModeType = it },
            onDismiss = { showDarkModeDialog = false },
            onConfirm = {
                currentDarkModeType.value = tempDarkModeType
                prefs.edit().putString("darkModeType", tempDarkModeType.name).commit()
                showDarkModeDialog = false
                restartApp(context)
            }
        )
    }
    
    if (showSeekTimeDialog) {
        SeekTimeDialog(
            currentValue = tempSeekTimeSeconds,
            onValueChange = { tempSeekTimeSeconds = it },
            onDismiss = { showSeekTimeDialog = false },
            onConfirm = {
                currentSeekTimeSeconds.value = tempSeekTimeSeconds
                prefs.edit().putFloat("seekTimeSeconds", tempSeekTimeSeconds).apply()
                showSeekTimeDialog = false
            }
        )
    }
    
    if (showQmCoverSizeDialog) {
        QmCoverSizeDialog(
            currentValue = tempQmCoverSize,
            onValueChange = { tempQmCoverSize = it },
            onDismiss = { showQmCoverSizeDialog = false },
            onConfirm = {
                currentQmCoverSize.value = tempQmCoverSize
                prefs.edit().putInt("qmCoverSize", tempQmCoverSize).apply()
                showQmCoverSizeDialog = false
            }
        )
    }
    
    if (showNeCoverSizeDialog) {
        NeCoverSizeDialog(
            currentValue = tempNeCoverSize,
            onValueChange = { tempNeCoverSize = it },
            onDismiss = { showNeCoverSizeDialog = false },
            onConfirm = {
                currentNeCoverSize.value = tempNeCoverSize
                prefs.edit().putInt("neCoverSize", tempNeCoverSize).apply()
                showNeCoverSizeDialog = false
            }
        )
    }
    
    if (showCoverSizeDialog) {
        CoverSizeDialog(
            currentValue = tempCoverSize,
            onValueChange = { tempCoverSize = it },
            onDismiss = { showCoverSizeDialog = false },
            onConfirm = {
                currentCoverSize.value = tempCoverSize
                prefs.edit().putInt("amCoverSize", tempCoverSize).apply()
                showCoverSizeDialog = false
            }
        )
    }
    
    if (showAMRegionDialog) {
        AMRegionDialog(
            currentValue = tempAMRegion,
            onValueChange = { tempAMRegion = it },
            onDismiss = { showAMRegionDialog = false },
            onConfirm = {
                currentAMRegion.value = tempAMRegion
                updateAMDefaultRegion(context, tempAMRegion)
                showAMRegionDialog = false
            }
        )
    }
    
    if (showAMTokenDialog) {
        AMTokenDialog(
            currentSource = tempAMTokenSource,
            onSourceChange = { tempAMTokenSource = it },
            currentUserToken = tempAMUserToken,
            onUserTokenChange = { tempAMUserToken = it },
            currentCloudflareUrl = tempAMCloudflareUrl,
            onCloudflareUrlChange = { tempAMCloudflareUrl = it },
            currentCountry = tempAMCountry,
            onCountryChange = { tempAMCountry = it },
            defaultUrlName = savedAMUrlName,
            contributorUrlName = savedAMUrlNameContributor,
            noticeContributor = savedNoticeContributor,
            onDismiss = { showAMTokenDialog = false },
            onConfirm = {
                currentAMTokenSource.value = tempAMTokenSource
                currentAMUserToken.value = tempAMUserToken
                currentAMCloudflareUrl.value = tempAMCloudflareUrl
                currentAMCountry.value = tempAMCountry
                updateAMTokenConfig(
                    context = context,
                    config = AMTokenConfig(
                        tokenSource = tempAMTokenSource,
                        userToken = tempAMUserToken,
                        cloudflareUrl = tempAMCloudflareUrl,
                        country = tempAMCountry,
                        defaultUrlName = savedAMUrlName,
                        contributorUrlName = savedAMUrlNameContributor,
                        noticeContributor = savedNoticeContributor
                    )
                )
                showAMTokenDialog = false
            }
        )
    }
    
    if (showSongClickActionDialog) {
        SongClickActionDialog(
            currentValue = tempSongClickAction,
            onValueChange = { tempSongClickAction = it },
            autoDetectEmbeddedLyricsType = tempAutoDetectEmbeddedLyricsType,
            onAutoDetectEmbeddedLyricsTypeChange = { tempAutoDetectEmbeddedLyricsType = it },
            onDismiss = { showSongClickActionDialog = false },
            onConfirm = {
                currentSongClickAction.value = tempSongClickAction
                val editor = musicLibraryPrefs.edit()
                editor.putString("songClickAction", tempSongClickAction)
                currentAutoDetectEmbeddedLyricsType.value = tempAutoDetectEmbeddedLyricsType
                editor.putBoolean("autoDetectEmbeddedLyricsType", tempAutoDetectEmbeddedLyricsType)
                editor.apply()
                showSongClickActionDialog = false
            }
        )
    }
    
    if (showArtistSeparatorDialog) {
        ArtistSeparatorDialog(
            currentValue = tempArtistSeparator,
            onValueChange = { tempArtistSeparator = it },
            currentCustomValue = tempArtistSeparatorCustom,
            onCustomValueChange = { tempArtistSeparatorCustom = it },
            currentMode = tempArtistSeparatorMode,
            onModeChange = { tempArtistSeparatorMode = it },
            onDismiss = { showArtistSeparatorDialog = false },
            onConfirm = {
                val separatorToSave = if (tempArtistSeparatorMode == "custom") tempArtistSeparatorCustom else tempArtistSeparator
                currentArtistSeparator.value = separatorToSave
                prefs.edit().putString("artistSeparator", separatorToSave).apply()
                showArtistSeparatorDialog = false
            }
        )
    }
    
    if (showVibrationIntensityDialog) {
        VibrationIntensityDialog(
            currentValue = tempVibrationIntensity,
            onValueChange = { tempVibrationIntensity = it },
            onDismiss = { showVibrationIntensityDialog = false },
            onConfirm = {
                currentVibrationIntensity.value = tempVibrationIntensity
                prefs.edit().putString("vibrationIntensity", tempVibrationIntensity).apply()
                showVibrationIntensityDialog = false
            }
        )
    }

    if (showLyricSettingsSheet) {
        LyricSettingsBottomSheet(
            onDismissRequest = { showLyricSettingsSheet = false },
            showTranslation = lyricShowTranslation,
            showTransliteration = lyricShowTransliteration,
            supportsLyricBlur = supportsLyricBlur,
            lyricBlurEnabled = lyricBlurEnabled,
            lyriconStatusBarEnabled = lyriconStatusBarEnabled,
            fontSize = lyricFontSize,
            fontWeight = lyricFontWeight,
            animationType = lyricAnimationType,
            fontOptions = lyricFontOptions,
            selectedFontId = lyricSelectedFontId,
            onShowTranslationChange = {
                lyricShowTranslation = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLATION, it)
                    .apply()
            },
            onShowTransliterationChange = {
                lyricShowTransliteration = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_SHOW_TRANSLITERATION, it)
                    .apply()
            },
            onLyricBlurEnabledChange = {
                lyricBlurEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_LYRIC_BLUR, it)
                    .apply()
            },
            onLyriconStatusBarEnabledChange = {
                lyriconStatusBarEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_LYRICON_STATUS_BAR, it)
                    .apply()
            },
            onFontSizeChange = {
                lyricFontSize = it
                lyricPreviewPrefs.edit()
                    .putFloat(LyricPreviewActivity.KEY_FONT_SIZE, it)
                    .apply()
            },
            onFontWeightChange = {
                lyricFontWeight = it
                lyricPreviewPrefs.edit()
                    .putInt(LyricPreviewActivity.KEY_FONT_WEIGHT, it)
                    .apply()
            },
            onAnimationTypeChange = {
                lyricAnimationType = it
                lyricPreviewPrefs.edit()
                    .putInt(LyricPreviewActivity.KEY_INTERLUDE_ANIMATION_TYPE, it)
                    .apply()
            },
            onOpenCustomFontPicker = {
                lyricFontPickerLauncher.launch(arrayOf("*/*"))
            },
            onSelectFont = { fontId ->
                LyricCustomFontStore.setSelectedFontId(context, fontId)
                lyricSelectedFontId = fontId
            },
            onDeleteFont = { fontId ->
                val deleted = LyricCustomFontStore.deleteFont(context, fontId)
                if (deleted) {
                    lyricFontOptions = LyricCustomFontStore.loadOptions(context)
                    lyricSelectedFontId = LyricCustomFontStore.getSelectedFontId(context)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            accentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SettingsItemWithSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DarkModeDialog(
    currentValue: DarkModeType,
    onValueChange: (DarkModeType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("深色模式") },
        text = {
            Column {
                DarkModeType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(type) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == type,
                            onClick = { onValueChange(type) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type.displayName,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SeekTimeDialog(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快进/快退时间") },
        text = {
            Column {
                Text(
                    text = "当前设置: ${currentValue.toInt()} 秒",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = currentValue,
                    onValueChange = onValueChange,
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1秒", fontSize = 12.sp)
                    Text("5秒", fontSize = 12.sp)
                    Text("10秒", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun QmCoverSizeDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val coverSizes = listOf(500, 800, 1200)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QM 元数据封面尺寸") },
        text = {
            Column {
                coverSizes.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(size) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == size,
                            onClick = { onValueChange(size) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (size == 1200) "1200×1200" else if (size == 800) "800×800" else "${size}x${size}",
                            fontSize = 16.sp
                        )
                        if (size == 1200) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun NeCoverSizeDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val coverSizes = listOf(500, 1000, 3000)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NE 元数据封面尺寸") },
        text = {
            Column {
                coverSizes.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(size) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == size,
                            onClick = { onValueChange(size) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (size == 1000) "1000×1000" else if (size == 3000) "3000×3000" else "${size}x${size}",
                            fontSize = 16.sp
                        )
                        if (size == 1000) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CoverSizeDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val coverSizes = listOf(500, 1000, 3000)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AM 元数据封面尺寸") },
        text = {
            Column {
                coverSizes.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(size) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == size,
                            onClick = { onValueChange(size) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (size == 3000) "3000×3000" else "${size}x${size}",
                            fontSize = 16.sp
                        )
                        if (size == 3000) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AMRegionDialog(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val regions = AM_REGION_OPTIONS
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AM 默认地区") },
        text = {
            Column {
                regions.forEach { (key, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == key,
                            onClick = { onValueChange(key) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                        if (key == "HK_SC") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AMTokenDialog(
    currentSource: String,
    onSourceChange: (String) -> Unit,
    currentUserToken: String,
    onUserTokenChange: (String) -> Unit,
    currentCloudflareUrl: String,
    onCloudflareUrlChange: (String) -> Unit,
    currentCountry: String,
    onCountryChange: (String) -> Unit,
    defaultUrlName: String,
    contributorUrlName: String,
    noticeContributor: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("APPLE_MUSIC_MEDIA_USER_TOKEN") },
        text = {
            Column {
                val tokenSources = listOf(
                    "cloudflare" to defaultUrlName,
                    "contributor" to contributorUrlName,
                    "custom" to "自行填写"
                )
                
                tokenSources.forEach { (key, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSourceChange(key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSource == key,
                            onClick = { onSourceChange(key) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                        if (key == "cloudflare") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (currentSource == "custom") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = currentUserToken,
                            onValueChange = onUserTokenChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (currentUserToken.isEmpty()) {
                                    Text(
                                        text = "输入你的 Media-User-Token",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = currentCountry,
                            onValueChange = onCountryChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (currentCountry.isEmpty()) {
                                    Text(
                                        text = "国家代号 (例如: cn, us, jp)",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    
                    if (noticeContributor.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = noticeContributor,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    if (noticeContributor.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = noticeContributor,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SongClickActionDialog(
    currentValue: String,
    onValueChange: (String) -> Unit,
    autoDetectEmbeddedLyricsType: Boolean,
    onAutoDetectEmbeddedLyricsTypeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("点击歌曲默认操作") },
        text = {
            Column {
                val actions = listOf(
                    "editLyrics" to "编辑歌词",
                    "editMetadata" to "编辑元数据",
                    "playMusic" to "播放音乐"
                )
                
                actions.forEach { (key, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == key,
                            onClick = { onValueChange(key) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                        if (key == "editLyrics") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onAutoDetectEmbeddedLyricsTypeChange(!autoDetectEmbeddedLyricsType) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "自动判断歌词类型",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "对于不标准的歌词，可能会判断出错",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoDetectEmbeddedLyricsType,
                        onCheckedChange = onAutoDetectEmbeddedLyricsTypeChange
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ArtistSeparatorDialog(
    currentValue: String,
    onValueChange: (String) -> Unit,
    currentCustomValue: String,
    onCustomValueChange: (String) -> Unit,
    currentMode: String,
    onModeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val presetSeparators = listOf("/", "、", "；", "，", "&")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("艺术家分隔符") },
        text = {
            Column {
                Text(
                    text = "预设选项",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                presetSeparators.forEach { sep ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onModeChange("preset")
                                onValueChange(sep)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == "preset" && currentValue == sep,
                            onClick = {
                                onModeChange("preset")
                                onValueChange(sep)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\"$sep\"",
                            fontSize = 16.sp
                        )
                        if (sep == "/") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeChange("custom") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == "custom",
                        onClick = { onModeChange("custom") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "自定义",
                        fontSize = 16.sp
                    )
                }
                
                if (currentMode == "custom") {
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
                            value = currentCustomValue,
                            onValueChange = onCustomValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (currentCustomValue.isEmpty()) {
                                    Text(
                                        text = "输入自定义分隔符",
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
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun VibrationIntensityDialog(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val intensities = listOf(
        "off" to "关闭",
        "weak" to "弱",
        "medium" to "中等",
        "strong" to "强"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("震动反馈强度") },
        text = {
            Column {
                intensities.forEach { (key, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == key,
                            onClick = { onValueChange(key) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                        if (key == "weak") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(默认)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}
