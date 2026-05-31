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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.ui.theme.DarkModeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
private const val PREF_SONG_CLICK_ACTION_CONFIRMED = "songClickActionConfirmed"
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

private val LocalSettingsLayoutProfile = compositionLocalOf { AppLayoutProfile.PHONE }

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
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(newBase))
    }

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
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val musicLibraryPrefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    val lyricPreviewPrefs = remember {
        context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val supportsLyricBlur = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    val supportsDynamicCoverBackground = supportsLyricBlur
    fun normalizePageBackgroundMode(mode: Int): Int {
        val normalized = when (mode) {
            LyricPreviewActivity.PAGE_BACKGROUND_SOLID,
            LyricPreviewActivity.PAGE_BACKGROUND_STATIC_BLUR,
            LyricPreviewActivity.PAGE_BACKGROUND_DYNAMIC_FLOW -> mode
            else -> LyricPreviewActivity.DEFAULT_PAGE_BACKGROUND_MODE
        }
        return if (!supportsDynamicCoverBackground && normalized == LyricPreviewActivity.PAGE_BACKGROUND_DYNAMIC_FLOW) {
            LyricPreviewActivity.DEFAULT_PAGE_BACKGROUND_MODE
        } else {
            normalized
        }
    }
    
    val savedDarkModeType = remember {
        try {
            DarkModeType.valueOf(prefs.getString("darkModeType", DarkModeType.FOLLOW_SYSTEM.name) ?: DarkModeType.FOLLOW_SYSTEM.name)
        } catch (e: Exception) { DarkModeType.FOLLOW_SYSTEM }
    }
    val savedLanguageTag = remember {
        prefs.getString(APP_LANGUAGE_TAG_KEY, APP_LANGUAGE_SYSTEM) ?: APP_LANGUAGE_SYSTEM
    }
    
    val savedSeekTimeSeconds = remember { prefs.getFloat("seekTimeSeconds", 2f) }
    val savedVibrationIntensity = remember { prefs.getString("vibrationIntensity", "weak") }
    val savedLayoutModePreference = remember { getSavedAppLayoutModePreference(context) }
    val configuration = LocalConfiguration.current
    
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showSeekTimeDialog by remember { mutableStateOf(false) }
    var showVibrationIntensityDialog by remember { mutableStateOf(false) }
    var showLayoutModeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
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
    
    val savedSongClickAction = remember { musicLibraryPrefs.getString("songClickAction", "") } ?: ""
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
    var tempLayoutModePreference by remember { mutableStateOf(savedLayoutModePreference) }
    var tempLanguageTag by remember { mutableStateOf(savedLanguageTag) }
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
    val currentLayoutModePreference = remember { mutableStateOf(savedLayoutModePreference) }
    val currentLanguageTag = remember { mutableStateOf(savedLanguageTag) }
    val effectiveLayoutProfile = remember(
        currentLayoutModePreference.value,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.smallestScreenWidthDp,
        configuration.orientation
    ) {
        resolveAppLayoutProfile(context, currentLayoutModePreference.value)
    }
    val settingsListHorizontalPadding = when (effectiveLayoutProfile) {
        AppLayoutProfile.WATCH -> 12.dp
        AppLayoutProfile.PHONE -> 16.dp
        AppLayoutProfile.TABLET -> 24.dp
    }
    fun languageTagDisplayName(tag: String): String = when (tag) {
        APP_LANGUAGE_SYSTEM -> context.getString(R.string.settings_language_system)
        "zh-CN" -> context.getString(R.string.settings_language_zh_cn)
        "en" -> context.getString(R.string.settings_language_en)
        "ja" -> context.getString(R.string.settings_language_ja)
        else -> context.getString(R.string.settings_language_system)
    }
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
    var tempMiniPlayerBackgroundMode by remember {
        mutableStateOf(getSavedMiniPlayerBackgroundMode(context))
    }
    val currentMiniPlayerBackgroundMode = remember {
        mutableStateOf(getSavedMiniPlayerBackgroundMode(context))
    }
    var tempMiniPlayerLandscapeAlignment by remember {
        mutableStateOf(getSavedMiniPlayerLandscapeAlignment(context))
    }
    val currentMiniPlayerLandscapeAlignment = remember {
        mutableStateOf(getSavedMiniPlayerLandscapeAlignment(context))
    }
    
    var showCoverSizeDialog by remember { mutableStateOf(false) }
    var showQmCoverSizeDialog by remember { mutableStateOf(false) }
    var showNeCoverSizeDialog by remember { mutableStateOf(false) }
    var showAMRegionDialog by remember { mutableStateOf(false) }
    var showAMTokenDialog by remember { mutableStateOf(false) }
    var showArtistSeparatorDialog by remember { mutableStateOf(false) }
    var showMiniPlayerBackgroundDialog by remember { mutableStateOf(false) }
    var showMiniPlayerLandscapeAlignmentDialog by remember { mutableStateOf(false) }
    var showPlaybackControlStyleDialog by remember { mutableStateOf(false) }
    
    var autoScan by remember { mutableStateOf(musicLibraryPrefs.getBoolean("autoScan", false)) }
    var metadataSaveAutoClose by remember {
        mutableStateOf(musicLibraryPrefs.getBoolean(PREF_KEY_METADATA_SAVE_AUTO_CLOSE, false))
    }
    var showLyricSettingsSheet by remember { mutableStateOf(false) }
    var showArtistSplitWhitelistSheet by remember { mutableStateOf(false) }
    var artistSplitWhitelist by remember { mutableStateOf(ArtistSplitWhitelistStore.load(context)) }
    var showFeaturingKeywordSheet by remember { mutableStateOf(false) }
    var featuringKeywords by remember { mutableStateOf(FeaturingArtistKeywordStore.load(context)) }
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
    var lyricGlowEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_LYRIC_GLOW,
                LyricPreviewActivity.DEFAULT_LYRIC_GLOW
            )
        )
    }
    var pageBackgroundMode by remember {
        mutableStateOf(
            if (lyricPreviewPrefs.contains(LyricPreviewActivity.KEY_PAGE_BACKGROUND_MODE)) {
                normalizePageBackgroundMode(
                    lyricPreviewPrefs.getInt(
                        LyricPreviewActivity.KEY_PAGE_BACKGROUND_MODE,
                        LyricPreviewActivity.DEFAULT_PAGE_BACKGROUND_MODE
                    )
                )
            } else {
                val legacyDynamicEnabled = supportsDynamicCoverBackground &&
                    lyricPreviewPrefs.getBoolean(
                        LyricPreviewActivity.KEY_DYNAMIC_COVER_BACKGROUND,
                        LyricPreviewActivity.DEFAULT_DYNAMIC_COVER_BACKGROUND
                    )
                if (legacyDynamicEnabled) {
                    LyricPreviewActivity.PAGE_BACKGROUND_DYNAMIC_FLOW
                } else {
                    LyricPreviewActivity.DEFAULT_PAGE_BACKGROUND_MODE
                }
            }
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
    var carBluetoothLyricEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_CAR_BLUETOOTH_LYRIC,
                LyricPreviewActivity.DEFAULT_CAR_BLUETOOTH_LYRIC
            )
        )
    }
    var flymeStatusBarLyricEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC,
                LyricPreviewActivity.DEFAULT_FLYME_STATUS_BAR_LYRIC
            )
        )
    }
    var flymeStatusBarLyricHideNotificationEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION,
                LyricPreviewActivity.DEFAULT_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION
            )
        )
    }
    LaunchedEffect(Unit) {
        if (flymeStatusBarLyricEnabled || flymeStatusBarLyricHideNotificationEnabled) {
            flymeStatusBarLyricEnabled = false
            flymeStatusBarLyricHideNotificationEnabled = false
            lyricPreviewPrefs.edit()
                .putBoolean(LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC, false)
                .putBoolean(LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION, false)
                .apply()
        }
    }
    var lyricKeepScreenOnEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_SCREEN_KEEP_ON,
                LyricPreviewActivity.DEFAULT_SCREEN_KEEP_ON
            )
        )
    }
    var lyricAutoHidePlaybackControlsEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_AUTO_HIDE_PLAYBACK_CONTROLS,
                LyricPreviewActivity.DEFAULT_AUTO_HIDE_PLAYBACK_CONTROLS
            )
        )
    }
    val savedPlaybackControlStyle = remember {
        when (
            lyricPreviewPrefs.getInt(
                LyricPreviewActivity.KEY_PLAYBACK_CONTROL_STYLE,
                LyricPreviewActivity.DEFAULT_PLAYBACK_CONTROL_STYLE
            )
        ) {
            LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_2 -> LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_2
            else -> LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_1
        }
    }
    var tempPlaybackControlStyle by remember { mutableStateOf(savedPlaybackControlStyle) }
    val currentPlaybackControlStyle = remember { mutableStateOf(savedPlaybackControlStyle) }
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
    var lyricWordLiftDistanceDp by remember {
        mutableFloatStateOf(
            lyricPreviewPrefs.getFloat(
                LyricPreviewActivity.KEY_WORD_LIFT_DISTANCE_DP,
                LyricPreviewActivity.DEFAULT_WORD_LIFT_DISTANCE_DP
            ).coerceIn(
                LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP,
                LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP
            )
        )
    }
    var lyricLatinWordLiftAsWholeEnabled by remember {
        mutableStateOf(
            lyricPreviewPrefs.getBoolean(
                LyricPreviewActivity.KEY_LATIN_WORD_LIFT_AS_WHOLE,
                LyricPreviewActivity.DEFAULT_LATIN_WORD_LIFT_AS_WHOLE
            )
        )
    }
    var lyricPlayedLyricAlpha by remember {
        mutableFloatStateOf(
            lyricPreviewPrefs.getFloat(
                LyricPreviewActivity.KEY_PLAYED_LYRIC_ALPHA,
                LyricPreviewActivity.DEFAULT_PLAYED_LYRIC_ALPHA
            ).coerceIn(0f, 1f)
        )
    }
    var lyricUpcomingLyricContrast by remember {
        mutableFloatStateOf(
            lyricPreviewPrefs.getFloat(
                LyricPreviewActivity.KEY_UPCOMING_LYRIC_CONTRAST,
                LyricPreviewActivity.DEFAULT_UPCOMING_LYRIC_CONTRAST
            ).coerceIn(0f, 1f)
        )
    }
    var lyricDisplayPosition by remember {
        mutableStateOf(
            lyricPreviewPrefs.getInt(
                LyricPreviewActivity.KEY_LYRIC_DISPLAY_POSITION,
                LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_POSITION
            ).let { raw ->
                if (raw in LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN..LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX) {
                    raw
                } else {
                    LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_POSITION
                }
            }
        )
    }
    var lyricDisplayMode by remember {
        mutableStateOf(
            lyricPreviewPrefs.getInt(
                LyricPreviewActivity.KEY_LYRIC_DISPLAY_MODE,
                LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_MODE
            ).let { raw ->
                when (raw) {
                    LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT,
                    LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD,
                    LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_LINE -> raw
                    else -> LyricPreviewActivity.DEFAULT_LYRIC_DISPLAY_MODE
                }
            }
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
                Toast.makeText(context, error.message ?: context.getString(R.string.settings_font_import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    CompositionLocalProvider(LocalSettingsLayoutProfile provides effectiveLayoutProfile) {
        Column(modifier = modifier.fillMaxSize()) {
            CommonHeadBar(
                title = stringResource(R.string.settings_page_title),
                showBack = true,
                showMenu = false,
                onBackClick = onBack
            )
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = settingsListHorizontalPadding)
            ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
                item { SettingsSectionTitle(stringResource(R.string.settings_section_software)) }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_dark_mode),
                    summary = currentDarkModeType.value.displayName,
                    onClick = {
                        tempDarkModeType = currentDarkModeType.value
                        showDarkModeDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_layout_mode),
                    summary = getAppLayoutModeSummary(context, currentLayoutModePreference.value),
                    onClick = {
                        tempLayoutModePreference = currentLayoutModePreference.value
                        showLayoutModeDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_language_title),
                    summary = languageTagDisplayName(currentLanguageTag.value),
                    onClick = {
                        tempLanguageTag = currentLanguageTag.value
                        showLanguageDialog = true
                    }
                )
            }
             
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
                item { SettingsSectionTitle(stringResource(R.string.settings_section_timing)) }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_seek_time),
                    summary = stringResource(R.string.settings_seconds_value, currentSeekTimeSeconds.value.toInt()),
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
                    title = stringResource(R.string.settings_vibration_feedback),
                    summary = when (currentVibrationIntensity.value) {
                        "off" -> stringResource(R.string.settings_vibration_off)
                        "weak" -> stringResource(R.string.settings_vibration_weak)
                        "medium" -> stringResource(R.string.settings_vibration_medium)
                        "strong" -> stringResource(R.string.settings_vibration_strong)
                        else -> stringResource(R.string.settings_vibration_weak)
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

                item { SettingsSectionTitle(stringResource(R.string.settings_section_home)) }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_playback_bar_bg),
                    summary = getMiniPlayerBackgroundModeLabel(currentMiniPlayerBackgroundMode.value),
                    onClick = {
                        tempMiniPlayerBackgroundMode = currentMiniPlayerBackgroundMode.value
                        showMiniPlayerBackgroundDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_playback_control_style),
                    summary = getPlaybackControlStyleLabel(context, currentPlaybackControlStyle.value),
                    onClick = {
                        tempPlaybackControlStyle = currentPlaybackControlStyle.value
                        showPlaybackControlStyleDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_landscape_playbar_position),
                    summary = getMiniPlayerLandscapeAlignmentLabel(currentMiniPlayerLandscapeAlignment.value),
                    onClick = {
                        tempMiniPlayerLandscapeAlignment = currentMiniPlayerLandscapeAlignment.value
                        showMiniPlayerLandscapeAlignmentDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
                item { SettingsSectionTitle(stringResource(R.string.settings_section_library)) }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.ml_music_library_directory_settings),
                    summary = stringResource(R.string.settings_library_directory_summary),
                    onClick = onNavigateToMusicLibrarySettings
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item {
                SettingsItemWithSwitch(
                    title = stringResource(R.string.settings_auto_scan_library),
                    summary = stringResource(R.string.settings_auto_scan_library_summary),
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
                    title = stringResource(R.string.ml_music_library_select_default_action),
                    summary = when (currentSongClickAction.value) {
                        "editLyrics" -> stringResource(R.string.settings_song_action_edit_lyrics)
                        "editMetadata" -> stringResource(R.string.settings_song_action_edit_metadata)
                        "playMusic" -> stringResource(R.string.settings_song_action_play_music)
                        else -> stringResource(R.string.common_not_set)
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
                    title = stringResource(R.string.lyric_settings_title),
                    summary = stringResource(R.string.settings_lyric_settings_summary),
                    onClick = { showLyricSettingsSheet = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_artist_split_whitelist),
                    summary = if (artistSplitWhitelist.isEmpty()) {
                        stringResource(R.string.settings_not_added)
                    } else {
                        stringResource(R.string.settings_artist_count_added, artistSplitWhitelist.size)
                    },
                    onClick = { showArtistSplitWhitelistSheet = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                val keywordSummary = if (featuringKeywords.isEmpty()) {
                    stringResource(R.string.settings_not_enabled)
                } else {
                    featuringKeywords.joinToString("|")
                }
                SettingsItem(
                    title = stringResource(R.string.settings_featuring_recognition),
                    summary = keywordSummary,
                    onClick = { showFeaturingKeywordSheet = true }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

                item { SettingsSectionTitle(stringResource(R.string.settings_section_metadata_edit)) }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_custom_metadata_fields),
                    summary = stringResource(R.string.settings_custom_metadata_fields_summary),
                    onClick = onNavigateToCustomMetadataFieldsSettings
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItemWithSwitch(
                    title = stringResource(R.string.settings_auto_close_after_save),
                    summary = stringResource(R.string.settings_auto_close_after_save_summary),
                    checked = metadataSaveAutoClose,
                    onCheckedChange = { newValue ->
                        metadataSaveAutoClose = newValue
                        musicLibraryPrefs.edit()
                            .putBoolean(PREF_KEY_METADATA_SAVE_AUTO_CLOSE, newValue)
                            .apply()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

                item { SettingsSectionTitle(stringResource(R.string.settings_section_metadata_download)) }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_qm_cover_size),
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
                    title = stringResource(R.string.settings_ne_cover_size),
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
                    title = stringResource(R.string.settings_am_cover_size),
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
                    title = stringResource(R.string.settings_am_default_region),
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
                    title = stringResource(R.string.ml_music_library_artist_separator),
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
            
                item { SettingsSectionTitle(stringResource(R.string.settings_section_lyrics_download)) }
            
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
                Spacer(modifier = Modifier.height(16.dp + bottomContentPadding))
            }
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

    if (showLayoutModeDialog) {
        LayoutModeDialog(
            currentValue = tempLayoutModePreference,
            onValueChange = { tempLayoutModePreference = it },
            onDismiss = { showLayoutModeDialog = false },
            onConfirm = {
                currentLayoutModePreference.value = tempLayoutModePreference
                saveAppLayoutModePreference(context, tempLayoutModePreference)
                showLayoutModeDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentValue = tempLanguageTag,
            onValueChange = { tempLanguageTag = it },
            onDismiss = { showLanguageDialog = false },
            onConfirm = {
                currentLanguageTag.value = tempLanguageTag
                AppLanguage.saveLanguageTag(context, tempLanguageTag)
                showLanguageDialog = false
                restartAppKeepingPlayback(context)
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
                editor.putBoolean(PREF_SONG_CLICK_ACTION_CONFIRMED, true)
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

    if (showMiniPlayerBackgroundDialog) {
        MiniPlayerBackgroundDialog(
            currentValue = tempMiniPlayerBackgroundMode,
            onValueChange = { tempMiniPlayerBackgroundMode = it },
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onConfirm = {
                val normalized = normalizeMiniPlayerBackgroundMode(tempMiniPlayerBackgroundMode)
                currentMiniPlayerBackgroundMode.value = normalized
                musicLibraryPrefs.edit()
                    .putInt(KEY_MINI_PLAYER_BACKGROUND_MODE, normalized)
                    .apply()
                showMiniPlayerBackgroundDialog = false
            }
        )
    }

    if (showMiniPlayerLandscapeAlignmentDialog) {
        MiniPlayerLandscapeAlignmentDialog(
            currentValue = tempMiniPlayerLandscapeAlignment,
            onValueChange = { tempMiniPlayerLandscapeAlignment = it },
            onDismiss = { showMiniPlayerLandscapeAlignmentDialog = false },
            onConfirm = {
                val normalized = normalizeMiniPlayerLandscapeAlignment(tempMiniPlayerLandscapeAlignment)
                currentMiniPlayerLandscapeAlignment.value = normalized
                musicLibraryPrefs.edit()
                    .putInt(KEY_MINI_PLAYER_LANDSCAPE_ALIGNMENT, normalized)
                    .apply()
                showMiniPlayerLandscapeAlignmentDialog = false
            }
        )
    }

    if (showPlaybackControlStyleDialog) {
        PlaybackControlStyleDialog(
            currentValue = tempPlaybackControlStyle,
            onValueChange = { tempPlaybackControlStyle = it },
            onDismiss = { showPlaybackControlStyleDialog = false },
            onConfirm = {
                val normalized = when (tempPlaybackControlStyle) {
                    LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_2 -> LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_2
                    else -> LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_1
                }
                currentPlaybackControlStyle.value = normalized
                lyricPreviewPrefs.edit()
                    .putInt(LyricPreviewActivity.KEY_PLAYBACK_CONTROL_STYLE, normalized)
                    .apply()
                showPlaybackControlStyleDialog = false
            }
        )
    }

    if (showLyricSettingsSheet) {
        LyricSettingsBottomSheet(
            onDismissRequest = { showLyricSettingsSheet = false },
            onDesktopLyricSettingsVisibilityChange = { isDesktopPageVisible ->
                lyricPreviewPrefs.edit()
                    .putBoolean(
                        LyricPreviewActivity.KEY_DESKTOP_LYRIC_SETTINGS_SHEET_OPEN,
                        isDesktopPageVisible
                    )
                    .apply()
            },
            showTranslation = lyricShowTranslation,
            showTransliteration = lyricShowTransliteration,
            supportsLyricBlur = supportsLyricBlur,
            supportsDynamicCoverBackground = supportsDynamicCoverBackground,
            lyricBlurEnabled = lyricBlurEnabled,
            lyricGlowEnabled = lyricGlowEnabled,
            pageBackgroundMode = pageBackgroundMode,
            lyriconStatusBarEnabled = lyriconStatusBarEnabled,
            carBluetoothLyricEnabled = carBluetoothLyricEnabled,
            flymeStatusBarLyricEnabled = flymeStatusBarLyricEnabled,
            flymeStatusBarLyricHideNotificationEnabled = flymeStatusBarLyricHideNotificationEnabled,
            keepScreenOnEnabled = lyricKeepScreenOnEnabled,
            autoHidePlaybackControlsEnabled = lyricAutoHidePlaybackControlsEnabled,
            lyricDisplayMode = lyricDisplayMode,
            lyricDisplayPosition = lyricDisplayPosition,
            fontSize = lyricFontSize,
            fontWeight = lyricFontWeight,
            animationType = lyricAnimationType,
            wordLiftDistanceDp = lyricWordLiftDistanceDp,
            latinWordLiftAsWholeEnabled = lyricLatinWordLiftAsWholeEnabled,
            playedLyricAlpha = lyricPlayedLyricAlpha,
            upcomingLyricContrast = lyricUpcomingLyricContrast,
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
            onLyricGlowEnabledChange = {
                lyricGlowEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_LYRIC_GLOW, it)
                    .apply()
            },
            onPageBackgroundModeChange = {
                val normalized = normalizePageBackgroundMode(it)
                pageBackgroundMode = normalized
                lyricPreviewPrefs.edit()
                    .putInt(LyricPreviewActivity.KEY_PAGE_BACKGROUND_MODE, normalized)
                    .putBoolean(
                        LyricPreviewActivity.KEY_DYNAMIC_COVER_BACKGROUND,
                        normalized == LyricPreviewActivity.PAGE_BACKGROUND_DYNAMIC_FLOW
                    )
                    .apply()
            },
            onLyriconStatusBarEnabledChange = {
                lyriconStatusBarEnabled = it
                if (it) {
                    flymeStatusBarLyricEnabled = false
                }
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_LYRICON_STATUS_BAR, it)
                    .putBoolean(
                        LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC,
                        if (it) false else flymeStatusBarLyricEnabled
                    )
                    .apply()
            },
            onCarBluetoothLyricEnabledChange = {
                carBluetoothLyricEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_CAR_BLUETOOTH_LYRIC, it)
                    .apply()
            },
            onFlymeStatusBarLyricEnabledChange = {
                flymeStatusBarLyricEnabled = it
                if (it) {
                    lyriconStatusBarEnabled = false
                }
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC, it)
                    .putBoolean(
                        LyricPreviewActivity.KEY_LYRICON_STATUS_BAR,
                        if (it) false else lyriconStatusBarEnabled
                    )
                    .apply()
            },
            onFlymeStatusBarLyricHideNotificationEnabledChange = {
                flymeStatusBarLyricHideNotificationEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_FLYME_STATUS_BAR_LYRIC_HIDE_NOTIFICATION, it)
                    .apply()
            },
            onKeepScreenOnEnabledChange = {
                lyricKeepScreenOnEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_SCREEN_KEEP_ON, it)
                    .apply()
            },
            onAutoHidePlaybackControlsEnabledChange = {
                lyricAutoHidePlaybackControlsEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_AUTO_HIDE_PLAYBACK_CONTROLS, it)
                    .apply()
            },
            onLyricDisplayModeChange = {
                lyricDisplayMode = it
                lyricPreviewPrefs.edit()
                    .putInt(LyricPreviewActivity.KEY_LYRIC_DISPLAY_MODE, it)
                    .apply()
            },
            onLyricDisplayPositionChange = {
                lyricDisplayPosition = it
                lyricPreviewPrefs.edit()
                    .putInt(LyricPreviewActivity.KEY_LYRIC_DISPLAY_POSITION, it)
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
            onWordLiftDistanceDpChange = {
                val normalized = it.coerceIn(
                    LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP,
                    LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP
                )
                lyricWordLiftDistanceDp = normalized
                lyricPreviewPrefs.edit()
                    .putFloat(LyricPreviewActivity.KEY_WORD_LIFT_DISTANCE_DP, normalized)
                    .apply()
            },
            onLatinWordLiftAsWholeEnabledChange = {
                lyricLatinWordLiftAsWholeEnabled = it
                lyricPreviewPrefs.edit()
                    .putBoolean(LyricPreviewActivity.KEY_LATIN_WORD_LIFT_AS_WHOLE, it)
                    .apply()
            },
            onPlayedLyricAlphaChange = {
                val normalized = it.coerceIn(0f, 1f)
                lyricPlayedLyricAlpha = normalized
                lyricPreviewPrefs.edit()
                    .putFloat(LyricPreviewActivity.KEY_PLAYED_LYRIC_ALPHA, normalized)
                    .apply()
            },
            onUpcomingLyricContrastChange = {
                val normalized = it.coerceIn(0f, 1f)
                lyricUpcomingLyricContrast = normalized
                lyricPreviewPrefs.edit()
                    .putFloat(LyricPreviewActivity.KEY_UPCOMING_LYRIC_CONTRAST, normalized)
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

    if (showArtistSplitWhitelistSheet) {
        ArtistSplitWhitelistBottomSheet(
            whitelist = artistSplitWhitelist,
            onDismiss = { showArtistSplitWhitelistSheet = false },
            onWhitelistChange = { updatedWhitelist ->
                val normalized = ArtistSplitWhitelistStore.normalize(updatedWhitelist)
                artistSplitWhitelist = normalized
                ArtistSplitWhitelistStore.save(context, normalized)
            }
        )
    }

    if (showFeaturingKeywordSheet) {
        FeaturingArtistKeywordBottomSheet(
            keywords = featuringKeywords,
            onDismiss = { showFeaturingKeywordSheet = false },
            onKeywordsChange = { updatedKeywords ->
                val normalized = FeaturingArtistKeywordStore.normalize(updatedKeywords)
                featuringKeywords = normalized
                FeaturingArtistKeywordStore.save(context, normalized)
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    val profile = LocalSettingsLayoutProfile.current
    val titleSize = when (profile) {
        AppLayoutProfile.WATCH -> 16.sp
        AppLayoutProfile.PHONE -> 18.sp
        AppLayoutProfile.TABLET -> 20.sp
    }
    Text(
        text = title,
        fontSize = titleSize,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun SettingsItem(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = LocalSettingsLayoutProfile.current
    val horizontalPadding = when (profile) {
        AppLayoutProfile.WATCH -> 12.dp
        AppLayoutProfile.PHONE -> 16.dp
        AppLayoutProfile.TABLET -> 20.dp
    }
    val verticalPadding = when (profile) {
        AppLayoutProfile.WATCH -> 12.dp
        AppLayoutProfile.PHONE -> 16.dp
        AppLayoutProfile.TABLET -> 18.dp
    }
    val titleSize = when (profile) {
        AppLayoutProfile.WATCH -> 15.sp
        AppLayoutProfile.PHONE -> 16.sp
        AppLayoutProfile.TABLET -> 17.sp
    }
    val summarySize = when (profile) {
        AppLayoutProfile.WATCH -> 12.sp
        AppLayoutProfile.PHONE -> 14.sp
        AppLayoutProfile.TABLET -> 15.sp
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = titleSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                fontSize = summarySize,
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
    val profile = LocalSettingsLayoutProfile.current
    val horizontalPadding = when (profile) {
        AppLayoutProfile.WATCH -> 12.dp
        AppLayoutProfile.PHONE -> 16.dp
        AppLayoutProfile.TABLET -> 20.dp
    }
    val verticalPadding = when (profile) {
        AppLayoutProfile.WATCH -> 12.dp
        AppLayoutProfile.PHONE -> 16.dp
        AppLayoutProfile.TABLET -> 18.dp
    }
    val titleSize = when (profile) {
        AppLayoutProfile.WATCH -> 15.sp
        AppLayoutProfile.PHONE -> 16.sp
        AppLayoutProfile.TABLET -> 17.sp
    }
    val summarySize = when (profile) {
        AppLayoutProfile.WATCH -> 12.sp
        AppLayoutProfile.PHONE -> 14.sp
        AppLayoutProfile.TABLET -> 15.sp
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = titleSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                fontSize = summarySize,
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
        title = { Text(stringResource(R.string.settings_dark_mode)) },
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun LayoutModeDialog(
    currentValue: AppLayoutModePreference,
    onValueChange: (AppLayoutModePreference) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val autoProfile = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.smallestScreenWidthDp,
        configuration.orientation
    ) {
        detectAutoAppLayoutProfile(context)
    }
    val options = listOf(
        AppLayoutModePreference.AUTO,
        AppLayoutModePreference.WATCH,
        AppLayoutModePreference.PHONE,
        AppLayoutModePreference.TABLET
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_layout_mode)) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == option,
                            onClick = { onValueChange(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (option == AppLayoutModePreference.AUTO) {
                                stringResource(R.string.settings_layout_mode_current_profile, option.displayName, autoProfile.displayName)
                            } else {
                                option.displayName
                            },
                            fontSize = 16.sp
                        )
                        if (option == AppLayoutModePreference.AUTO) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSplitWhitelistBottomSheet(
    whitelist: List<String>,
    onDismiss: () -> Unit,
    onWhitelistChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_artist_split_whitelist),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_add_artist_label)) },
                placeholder = { Text(stringResource(R.string.settings_add_artist_placeholder)) },
                minLines = 3,
                maxLines = 6
            )

            if (errorText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { inputText = "" },
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotBlank()
                ) {
                    Text(stringResource(R.string.ml_music_library_clear_selection))
                }
                Button(
                    onClick = {
                        val newArtists = ArtistSplitWhitelistStore.parseInput(inputText)
                        if (newArtists.isEmpty()) {
                            errorText = context.getString(R.string.settings_add_artist_empty_error)
                            return@Button
                        }

                        val merged = ArtistSplitWhitelistStore.normalize(whitelist + newArtists)
                        if (merged.size == whitelist.size) {
                            errorText = context.getString(R.string.settings_add_artist_no_new_error)
                            return@Button
                        }

                        onWhitelistChange(merged)
                        inputText = ""
                        errorText = ""
                        Toast.makeText(context, context.getString(R.string.settings_add_artist_success, merged.size - whitelist.size), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_add))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.settings_current_whitelist),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (whitelist.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_artist),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                whitelist.forEach { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = artist,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                onWhitelistChange(
                                    whitelist.filterNot {
                                        it.equals(artist, ignoreCase = true)
                                    }
                                )
                            }
                        ) {
                            Text(stringResource(R.string.common_delete))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturingArtistKeywordBottomSheet(
    keywords: List<String>,
    onDismiss: () -> Unit,
    onKeywordsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    val normalizedKeywords = remember(keywords) { FeaturingArtistKeywordStore.normalize(keywords) }
    val defaultKeywords = FeaturingArtistKeywordStore.defaultKeywords
    val customKeywords = remember(normalizedKeywords) {
        FeaturingArtistKeywordStore.customKeywords(normalizedKeywords)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_featuring_recognition),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_featuring_summary),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.settings_default_keywords),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            defaultKeywords.forEach { keyword ->
                val checked = normalizedKeywords.any { it.equals(keyword, ignoreCase = true) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val updated = normalizedKeywords.toMutableList()
                            if (checked) {
                                updated.removeAll { it.equals(keyword, ignoreCase = true) }
                            } else {
                                updated.add(keyword)
                            }
                            onKeywordsChange(FeaturingArtistKeywordStore.normalize(updated))
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            val updated = normalizedKeywords.toMutableList()
                            if (isChecked) {
                                updated.add(keyword)
                            } else {
                                updated.removeAll { it.equals(keyword, ignoreCase = true) }
                            }
                            onKeywordsChange(FeaturingArtistKeywordStore.normalize(updated))
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = keyword,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_custom_keywords),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_add_keyword_label)) },
                placeholder = { Text(stringResource(R.string.settings_add_keyword_placeholder)) },
                minLines = 2,
                maxLines = 4
            )

            if (errorText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { inputText = "" },
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotBlank()
                ) {
                    Text(stringResource(R.string.ml_music_library_clear_selection))
                }
                Button(
                    onClick = {
                        val newKeywords = FeaturingArtistKeywordStore.parseInput(inputText)
                        if (newKeywords.isEmpty()) {
                            errorText = context.getString(R.string.settings_add_keyword_empty_error)
                            return@Button
                        }
                        val merged = FeaturingArtistKeywordStore.normalize(normalizedKeywords + newKeywords)
                        if (merged.size == normalizedKeywords.size) {
                            errorText = context.getString(R.string.settings_add_keyword_no_new_error)
                            return@Button
                        }
                        onKeywordsChange(merged)
                        inputText = ""
                        errorText = ""
                        Toast.makeText(context, context.getString(R.string.settings_add_keyword_success, merged.size - normalizedKeywords.size), Toast.LENGTH_SHORT)
                            .show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_add))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (customKeywords.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_custom_keyword),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                customKeywords.forEach { keyword ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = keyword,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                onKeywordsChange(
                                    normalizedKeywords.filterNot {
                                        it.equals(keyword, ignoreCase = true)
                                    }
                                )
                            }
                        ) {
                            Text(stringResource(R.string.common_delete))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
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
        title = { Text(stringResource(R.string.settings_seek_time)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_current_seek_time, currentValue.toInt()),
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
                    Text(stringResource(R.string.settings_one_second), fontSize = 12.sp)
                    Text(stringResource(R.string.settings_five_seconds), fontSize = 12.sp)
                    Text(stringResource(R.string.settings_ten_seconds), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.settings_qm_cover_size)) },
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
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.settings_ne_cover_size)) },
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
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.settings_am_cover_size)) },
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
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.settings_am_default_region)) },
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
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
    showDismissButton: Boolean = true,
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
                    "custom" to stringResource(R.string.settings_custom_fill_in)
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
                                text = stringResource(R.string.settings_default_bracket),
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
                                        text = stringResource(R.string.settings_am_user_token_placeholder),
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
                                        text = stringResource(R.string.settings_country_code_placeholder),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = if (showDismissButton) {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        } else null
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
        title = { Text(stringResource(R.string.ml_music_library_select_default_action)) },
        text = {
            Column {
                val actions = listOf(
                    "editLyrics" to stringResource(R.string.settings_song_action_edit_lyrics),
                    "editMetadata" to stringResource(R.string.settings_song_action_edit_metadata),
                    "playMusic" to stringResource(R.string.settings_song_action_play_music)
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
                            text = stringResource(R.string.settings_auto_detect_lyrics_type),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_auto_detect_lyrics_type_summary),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun LanguageDialog(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val languageOptions = listOf(
        APP_LANGUAGE_SYSTEM to context.getString(R.string.settings_language_system),
        "zh-CN" to context.getString(R.string.settings_language_zh_cn),
        "en" to context.getString(R.string.settings_language_en),
        "ja" to context.getString(R.string.settings_language_ja)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_title)) },
        text = {
            Column {
                languageOptions.forEach { (tag, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(tag) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == tag,
                            onClick = { onValueChange(tag) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        title = { Text(stringResource(R.string.ml_music_library_artist_separator)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_preset_options),
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
                                text = stringResource(R.string.settings_default_bracket),
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
                        text = stringResource(R.string.common_custom),
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
                                        text = stringResource(R.string.settings_custom_separator_placeholder),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
        "off" to stringResource(R.string.settings_vibration_off),
        "weak" to stringResource(R.string.settings_vibration_weak),
        "medium" to stringResource(R.string.settings_vibration_medium),
        "strong" to stringResource(R.string.settings_vibration_strong)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_vibration_intensity)) },
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
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun MiniPlayerBackgroundDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val options = buildList {
        add(MINI_PLAYER_BACKGROUND_MODE_DEFAULT to stringResource(R.string.lyric_settings_default))
        add(MINI_PLAYER_BACKGROUND_MODE_COVER_COLOR to stringResource(R.string.settings_bg_cover_color))
        add(MINI_PLAYER_BACKGROUND_MODE_COVER_BLUR to stringResource(R.string.settings_bg_cover_blur))
        add(MINI_PLAYER_BACKGROUND_MODE_REALTIME_BLUR to stringResource(R.string.settings_bg_liquid_glass))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_playback_bar_bg)) },
        text = {
            Column {
                options.forEach { (value, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = normalizeMiniPlayerBackgroundMode(currentValue) == value,
                            onClick = { onValueChange(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                        if (value == DEFAULT_MINI_PLAYER_BACKGROUND_MODE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun MiniPlayerLandscapeAlignmentDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val options = listOf(
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_END to stringResource(R.string.settings_align_end),
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_CENTER to stringResource(R.string.settings_align_center),
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_START to stringResource(R.string.settings_align_start)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_landscape_playbar_position)) },
        text = {
            Column {
                options.forEach { (value, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = normalizeMiniPlayerLandscapeAlignment(currentValue) == value,
                            onClick = { onValueChange(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                        if (value == DEFAULT_MINI_PLAYER_LANDSCAPE_ALIGNMENT) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

private fun getPlaybackControlStyleLabel(context: Context, value: Int): String {
    return when (value) {
        LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_2 -> context.getString(R.string.settings_playback_style_2)
        else -> context.getString(R.string.settings_playback_style_1)
    }
}

@Composable
fun PlaybackControlStyleDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val options = listOf(
        LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_1 to stringResource(R.string.settings_playback_style_1),
        LyricPreviewActivity.PLAYBACK_CONTROL_STYLE_2 to stringResource(R.string.settings_playback_style_2)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_playback_control_style)) },
        text = {
            Column {
                options.forEach { (value, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == value,
                            onClick = { onValueChange(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 16.sp
                        )
                        if (value == LyricPreviewActivity.DEFAULT_PLAYBACK_CONTROL_STYLE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_default_bracket),
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
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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

private fun restartAppKeepingPlayback(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
}
