package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.content.res.Configuration
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

private enum class LyricSettingsPage {
    MAIN,
    STATUS_BAR_LYRIC,
    DESKTOP_LYRIC,
    PAGE_BACKGROUND,
    LYRIC_DISPLAY_MODE,
    LYRIC_DISPLAY_POSITION,
    LYRIC_PLAYBACK_STATE,
    FONT_SIZE,
    FONT_WEIGHT,
    INTERLUDE_ANIMATION,
    WORD_LIFT_ANIMATION,
    CUSTOM_FONT,
    FONT_PREVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingsBottomSheet(
    onDismissRequest: () -> Unit,
    onDesktopLyricSettingsVisibilityChange: (Boolean) -> Unit,
    showTranslation: Boolean,
    showTransliteration: Boolean,
    supportsLyricBlur: Boolean,
    supportsDynamicCoverBackground: Boolean,
    lyricBlurEnabled: Boolean,
    lyricGlowEnabled: Boolean,
    pageBackgroundMode: Int,
    lyriconStatusBarEnabled: Boolean,
    carBluetoothLyricEnabled: Boolean,
    flymeStatusBarLyricEnabled: Boolean,
    flymeStatusBarLyricHideNotificationEnabled: Boolean,
    keepScreenOnEnabled: Boolean,
    autoHidePlaybackControlsEnabled: Boolean,
    lyricDisplayMode: Int,
    lyricDisplayPosition: Int,
    fontSize: Float,
    fontWeight: Int,
    animationType: Int,
    wordLiftDistanceDp: Float,
    latinWordLiftAsWholeEnabled: Boolean,
    playedLyricAlpha: Float,
    upcomingLyricContrast: Float,
    fontOptions: List<LyricCustomFontOption>,
    selectedFontId: String,
    onShowTranslationChange: (Boolean) -> Unit,
    onShowTransliterationChange: (Boolean) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onLyricGlowEnabledChange: (Boolean) -> Unit,
    onPageBackgroundModeChange: (Int) -> Unit,
    onLyriconStatusBarEnabledChange: (Boolean) -> Unit,
    onCarBluetoothLyricEnabledChange: (Boolean) -> Unit,
    onFlymeStatusBarLyricEnabledChange: (Boolean) -> Unit,
    onFlymeStatusBarLyricHideNotificationEnabledChange: (Boolean) -> Unit,
    onKeepScreenOnEnabledChange: (Boolean) -> Unit,
    onAutoHidePlaybackControlsEnabledChange: (Boolean) -> Unit,
    onLyricDisplayModeChange: (Int) -> Unit,
    onLyricDisplayPositionChange: (Int) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontWeightChange: (Int) -> Unit,
    onAnimationTypeChange: (Int) -> Unit,
    onWordLiftDistanceDpChange: (Float) -> Unit,
    onLatinWordLiftAsWholeEnabledChange: (Boolean) -> Unit,
    onPlayedLyricAlphaChange: (Float) -> Unit,
    onUpcomingLyricContrastChange: (Float) -> Unit,
    onOpenCustomFontPicker: () -> Unit,
    onSelectFont: (String) -> Unit,
    onDeleteFont: (String) -> Unit,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPortraitMode = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val prefs = remember {
        context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val loadedDesktopSettings = remember { DesktopLyricsSettingsStore.load(prefs) }
    var page by remember { mutableStateOf(LyricSettingsPage.MAIN) }
    var tempLyricDisplayPosition by remember(lyricDisplayPosition) { mutableFloatStateOf(lyricDisplayPosition.toFloat()) }
    var tempLyricDisplayMode by remember(lyricDisplayMode) { mutableIntStateOf(lyricDisplayMode) }
    var tempFontSize by remember(fontSize) { mutableFloatStateOf(fontSize) }
    var tempFontWeight by remember(fontWeight) { mutableFloatStateOf(fontWeight.toFloat()) }
    var tempAnimationType by remember(animationType) { mutableIntStateOf(animationType) }
    var tempWordLiftDistanceDp by remember(wordLiftDistanceDp) { mutableFloatStateOf(wordLiftDistanceDp) }
    var tempPlayedLyricAlpha by remember(playedLyricAlpha) { mutableFloatStateOf(playedLyricAlpha.coerceIn(0f, 1f)) }
    var tempUpcomingLyricContrast by remember(upcomingLyricContrast) { mutableFloatStateOf(upcomingLyricContrast.coerceIn(0f, 1f)) }
    var desktopLyricSettings by remember { mutableStateOf(loadedDesktopSettings) }
    var pendingOverlayPermissionEnable by remember { mutableStateOf(false) }
    val previewTypeface = remember(selectedFontId, fontOptions) {
        LyricCustomFontStore.resolveTypefaceById(context, selectedFontId)
    }
    fun saveDesktopSettings(settings: DesktopLyricsSettings) {
        desktopLyricSettings = settings
        DesktopLyricsSettingsStore.save(context, settings)
    }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (canDrawDesktopLyricOverlay(context)) {
            saveDesktopSettings(desktopLyricSettings.copy(enabled = true))
            pendingOverlayPermissionEnable = false
        }
    }
    DisposableEffect(lifecycleOwner, pendingOverlayPermissionEnable) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingOverlayPermissionEnable) {
                if (canDrawDesktopLyricOverlay(context)) {
                    saveDesktopSettings(desktopLyricSettings.copy(enabled = true))
                    pendingOverlayPermissionEnable = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    SideEffect {
        onDesktopLyricSettingsVisibilityChange(page == LyricSettingsPage.DESKTOP_LYRIC)
    }
    DisposableEffect(Unit) {
        onDispose {
            onDesktopLyricSettingsVisibilityChange(false)
        }
    }
    LaunchedEffect(page) { listState.scrollToItem(0) }

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = contentColor,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = page.name) {
                when (page) {
                LyricSettingsPage.MAIN -> {
                    Text(
                        text = stringResource(R.string.lyric_settings_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    LyricSettingsSectionTitle(
                        title = stringResource(R.string.lyric_settings_section_display),
                        contentColor = contentColor
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_show_translation),
                        checked = showTranslation,
                        onCheckedChange = onShowTranslationChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_show_transliteration),
                        checked = showTransliteration,
                        onCheckedChange = onShowTransliterationChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_display_mode),
                        contentColor = contentColor,
                        onClick = {
                            tempLyricDisplayMode = lyricDisplayMode
                            page = LyricSettingsPage.LYRIC_DISPLAY_MODE
                        }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_display_position),
                        contentColor = contentColor,
                        onClick = {
                            tempLyricDisplayPosition = lyricDisplayPosition.toFloat()
                            page = LyricSettingsPage.LYRIC_DISPLAY_POSITION
                        }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_playback_state),
                        contentColor = contentColor,
                        onClick = {
                            tempPlayedLyricAlpha = playedLyricAlpha.coerceIn(0f, 1f)
                            tempUpcomingLyricContrast = upcomingLyricContrast.coerceIn(0f, 1f)
                            page = LyricSettingsPage.LYRIC_PLAYBACK_STATE
                        }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_font_size),
                        contentColor = contentColor,
                        onClick = {
                            tempFontSize = fontSize
                            page = LyricSettingsPage.FONT_SIZE
                        }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_font_weight),
                        contentColor = contentColor,
                        onClick = {
                            tempFontWeight = fontWeight.toFloat()
                            page = LyricSettingsPage.FONT_WEIGHT
                        }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_custom_font),
                        contentColor = contentColor,
                        onClick = { page = LyricSettingsPage.CUSTOM_FONT }
                    )

                    LyricSettingsSectionTitle(
                        title = stringResource(R.string.lyric_settings_section_animation),
                        contentColor = contentColor
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_page_background),
                        contentColor = contentColor,
                        onClick = { page = LyricSettingsPage.PAGE_BACKGROUND }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_interlude_animation),
                        contentColor = contentColor,
                        onClick = {
                            tempAnimationType = animationType
                            page = LyricSettingsPage.INTERLUDE_ANIMATION
                        }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_word_lift_animation),
                        contentColor = contentColor,
                        onClick = {
                            tempWordLiftDistanceDp = wordLiftDistanceDp
                            page = LyricSettingsPage.WORD_LIFT_ANIMATION
                        }
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_lyric_glow),
                        checked = lyricGlowEnabled,
                        onCheckedChange = onLyricGlowEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    if (supportsLyricBlur) {
                        LyricSettingsSwitchRow(
                            title = stringResource(R.string.lyric_settings_lyric_blur),
                            checked = lyricBlurEnabled,
                            onCheckedChange = onLyricBlurEnabledChange,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }

                    LyricSettingsSectionTitle(
                        title = stringResource(R.string.lyric_settings_section_system),
                        contentColor = contentColor
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_status_bar_lyric),
                        contentColor = contentColor,
                        onClick = { page = LyricSettingsPage.STATUS_BAR_LYRIC }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_desktop_lyric),
                        contentColor = contentColor,
                        onClick = { page = LyricSettingsPage.DESKTOP_LYRIC }
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_keep_screen_on),
                        checked = keepScreenOnEnabled,
                        onCheckedChange = onKeepScreenOnEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    if (isPortraitMode) {
                        LyricSettingsSwitchRow(
                            title = stringResource(R.string.lyric_settings_auto_hide_playback_controls),
                            checked = autoHidePlaybackControlsEnabled,
                            onCheckedChange = onAutoHidePlaybackControlsEnabledChange,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                }

                LyricSettingsPage.STATUS_BAR_LYRIC -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_status_bar_lyric),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_status_bar_lyricon),
                        checked = lyriconStatusBarEnabled,
                        onCheckedChange = onLyriconStatusBarEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_car_bluetooth_lyric),
                        checked = carBluetoothLyricEnabled,
                        onCheckedChange = onCarBluetoothLyricEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }

                LyricSettingsPage.DESKTOP_LYRIC -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_desktop_lyric),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_desktop_lyric_master_switch),
                        checked = desktopLyricSettings.enabled,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                pendingOverlayPermissionEnable = false
                                saveDesktopSettings(desktopLyricSettings.copy(enabled = false))
                                return@LyricSettingsSwitchRow
                            }
                            if (!canDrawDesktopLyricOverlay(context)) {
                                pendingOverlayPermissionEnable = true
                                Toast.makeText(context, context.getString(R.string.lyric_settings_overlay_permission_required), Toast.LENGTH_SHORT).show()
                                overlayPermissionLauncher.launch(desktopLyricOverlaySettingsIntent(context))
                            } else {
                                pendingOverlayPermissionEnable = false
                                saveDesktopSettings(desktopLyricSettings.copy(enabled = true))
                            }
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_show_in_app),
                        checked = desktopLyricSettings.showInApp,
                        onCheckedChange = {
                            saveDesktopSettings(desktopLyricSettings.copy(showInApp = it))
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    val widthPercent = desktopLyricSettings.widthPercent.coerceIn(50, 100)
                    Text(
                        text = stringResource(R.string.lyric_settings_desktop_width, widthPercent),
                        color = contentColor
                    )
                    Slider(
                        value = widthPercent.toFloat(),
                        onValueChange = {
                            saveDesktopSettings(
                                desktopLyricSettings.copy(
                                    widthPercent = it.roundToInt().coerceIn(50, 100)
                                )
                            )
                        },
                        valueRange = 50f..100f,
                        steps = 49,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    val desktopFontSize = desktopLyricSettings.fontSizeSp.coerceIn(8f, 30f)
                    Text(
                        text = stringResource(R.string.lyric_settings_desktop_font_size, desktopFontSize.roundToInt()),
                        color = contentColor
                    )
                    Slider(
                        value = desktopFontSize,
                        onValueChange = {
                            saveDesktopSettings(
                                desktopLyricSettings.copy(
                                    fontSizeSp = it.coerceIn(8f, 30f)
                                )
                            )
                        },
                        valueRange = 8f..30f,
                        steps = 21,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    val desktopFontWeight = desktopLyricSettings.fontWeight.coerceIn(400, 900)
                    Text(
                        text = stringResource(R.string.lyric_settings_desktop_font_weight, desktopFontWeight),
                        color = contentColor
                    )
                    Slider(
                        value = desktopFontWeight.toFloat(),
                        onValueChange = {
                            val snapped = (it.roundToInt() / 50 * 50).coerceIn(400, 900)
                            saveDesktopSettings(desktopLyricSettings.copy(fontWeight = snapped))
                        },
                        valueRange = 400f..900f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_font_stroke),
                        checked = desktopLyricSettings.strokeEnabled,
                        onCheckedChange = {
                            saveDesktopSettings(desktopLyricSettings.copy(strokeEnabled = it))
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_show_translation),
                        checked = desktopLyricSettings.showTranslation,
                        onCheckedChange = {
                            saveDesktopSettings(desktopLyricSettings.copy(showTranslation = it))
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_use_custom_font),
                        checked = desktopLyricSettings.useCustomFont,
                        onCheckedChange = {
                            saveDesktopSettings(desktopLyricSettings.copy(useCustomFont = it))
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    Text(
                        text = stringResource(R.string.lyric_settings_alignment),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_align_left),
                        selected = desktopLyricSettings.align == LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_LEFT,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            saveDesktopSettings(
                                desktopLyricSettings.copy(align = LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_LEFT)
                            )
                        }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_align_center),
                        selected = desktopLyricSettings.align == LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_CENTER,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            saveDesktopSettings(
                                desktopLyricSettings.copy(align = LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_CENTER)
                            )
                        }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_align_right),
                        selected = desktopLyricSettings.align == LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_RIGHT,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            saveDesktopSettings(
                                desktopLyricSettings.copy(align = LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_RIGHT)
                            )
                        }
                    )
                    val xPercent = desktopLyricSettings.xPercent.coerceIn(0, 100)
                    Text(
                        text = stringResource(R.string.lyric_settings_display_x_axis, xPercent),
                        color = contentColor
                    )
                    Slider(
                        value = xPercent.toFloat(),
                        onValueChange = {
                            saveDesktopSettings(
                                desktopLyricSettings.copy(xPercent = it.roundToInt().coerceIn(0, 100))
                            )
                        },
                        valueRange = 0f..100f,
                        steps = 99,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    val yPercent = desktopLyricSettings.yPercent.coerceIn(0, 100)
                    Text(
                        text = stringResource(R.string.lyric_settings_display_y_axis, yPercent),
                        color = contentColor
                    )
                    Slider(
                        value = yPercent.toFloat(),
                        onValueChange = {
                            saveDesktopSettings(
                                desktopLyricSettings.copy(yPercent = it.roundToInt().coerceIn(0, 100))
                            )
                        },
                        valueRange = 0f..100f,
                        steps = 99,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    Text(
                        text = stringResource(R.string.lyric_settings_lyric_color),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DesktopLyricsSettingsStore.colorPresets.forEach { (key, color) ->
                            val selected = desktopLyricSettings.colorKey == key
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 38.dp else 34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) accentColor else contentColor.copy(alpha = 0.38f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        saveDesktopSettings(desktopLyricSettings.copy(colorKey = key))
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LyricSettingsPage.LYRIC_DISPLAY_MODE -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_display_mode),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_default),
                        selected = tempLyricDisplayMode == LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempLyricDisplayMode = LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT
                            onLyricDisplayModeChange(LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_force_word),
                        selected = tempLyricDisplayMode == LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempLyricDisplayMode = LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD
                            onLyricDisplayModeChange(LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_force_line),
                        selected = tempLyricDisplayMode == LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_LINE,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempLyricDisplayMode = LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_LINE
                            onLyricDisplayModeChange(LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_LINE)
                        }
                    )
                }

                LyricSettingsPage.LYRIC_DISPLAY_POSITION -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_display_position),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    val labelColor = if (containerColor.luminance() > 0.5f) {
                        Color.Black
                    } else {
                        Color.White
                    }
                    val minPosition = LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN.toFloat()
                    val maxPosition = LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX.toFloat()
                    val snappedPosition = tempLyricDisplayPosition.roundToInt()
                        .coerceIn(
                            LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN,
                            LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX
                        )
                    Text(
                        text = stringResource(R.string.lyric_settings_current_position, getLyricDisplayPositionLabel(snappedPosition)),
                        color = labelColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp)
                    )
                    Slider(
                        value = tempLyricDisplayPosition,
                        onValueChange = {
                            val snapped = it.roundToInt().coerceIn(
                                LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN,
                                LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX
                            )
                            tempLyricDisplayPosition = snapped.toFloat()
                            onLyricDisplayPositionChange(snapped)
                        },
                        valueRange = minPosition..maxPosition,
                        steps = (LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX - LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN - 1)
                            .coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            getLyricDisplayPositionLabel(LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MIN),
                            fontSize = 12.sp,
                            color = labelColor.copy(alpha = 0.92f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            getLyricDisplayPositionLabel(LyricPreviewActivity.LYRIC_DISPLAY_POSITION_DEFAULT),
                            fontSize = 12.sp,
                            color = labelColor.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            getLyricDisplayPositionLabel(LyricPreviewActivity.LYRIC_DISPLAY_POSITION_MAX),
                            fontSize = 12.sp,
                            color = labelColor.copy(alpha = 0.92f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LyricSettingsPage.FONT_SIZE -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_font_size),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    Text(
                        text = stringResource(R.string.lyric_settings_current_font_size, tempFontSize.toInt()),
                        color = contentColor
                    )
                    Slider(
                        value = tempFontSize,
                        onValueChange = { tempFontSize = it },
                        valueRange = 18f..50f,
                        steps = 31,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        ),
                        onValueChangeFinished = {
                            onFontSizeChange(tempFontSize)
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("18sp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("34sp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("50sp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                    }
                }

                LyricSettingsPage.FONT_WEIGHT -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_font_weight),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    Text(
                        text = stringResource(R.string.lyric_settings_current_font_weight, getFontWeightLabelForSheet(tempFontWeight.toInt().coerceIn(100, 900) / 100 * 100)),
                        color = contentColor
                    )
                    Slider(
                        value = tempFontWeight,
                        onValueChange = { tempFontWeight = it },
                        valueRange = 100f..900f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        ),
                        onValueChangeFinished = {
                            val snappedWeight = tempFontWeight.toInt().coerceIn(100, 900) / 100 * 100
                            tempFontWeight = snappedWeight.toFloat()
                            onFontWeightChange(snappedWeight)
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("100", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("500", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("900", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                    }
                }

                LyricSettingsPage.INTERLUDE_ANIMATION -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_interlude_animation),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_interlude_default_dot),
                        selected = tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DEFAULT,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT
                            onAnimationTypeChange(LyricPreviewActivity.ANIMATION_TYPE_DEFAULT)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_interlude_dinosaur),
                        selected = tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR
                            onAnimationTypeChange(LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_interlude_doge),
                        selected = tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DOGE,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DOGE
                            onAnimationTypeChange(LyricPreviewActivity.ANIMATION_TYPE_DOGE)
                        }
                    )
                }

                LyricSettingsPage.WORD_LIFT_ANIMATION -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_word_lift_animation),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsSwitchRow(
                        title = stringResource(R.string.lyric_settings_latin_lift_whole),
                        checked = latinWordLiftAsWholeEnabled,
                        onCheckedChange = onLatinWordLiftAsWholeEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    val snappedLiftDistance = tempWordLiftDistanceDp.roundToInt().coerceIn(
                        LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP.toInt(),
                        LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP.toInt()
                    )
                    Text(
                        text = stringResource(R.string.lyric_settings_lift_distance, snappedLiftDistance),
                        color = contentColor
                    )
                    Slider(
                        value = tempWordLiftDistanceDp,
                        onValueChange = {
                            val snapped = it.roundToInt().coerceIn(
                                LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP.toInt(),
                                LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP.toInt()
                            )
                            tempWordLiftDistanceDp = snapped.toFloat()
                            onWordLiftDistanceDpChange(snapped.toFloat())
                        },
                        valueRange = LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP..LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0dp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("2dp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("5dp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                    }
                }

                LyricSettingsPage.CUSTOM_FONT -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_custom_font),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_add_ttf_font),
                        contentColor = contentColor,
                        onClick = onOpenCustomFontPicker
                    )
                    LyricSettingsActionRow(
                        title = stringResource(R.string.lyric_settings_preview_font),
                        contentColor = contentColor,
                        onClick = { page = LyricSettingsPage.FONT_PREVIEW }
                    )
                    fontOptions.forEach { option ->
                        val optionTypeface = remember(option.id) {
                            LyricCustomFontStore.resolveTypefaceById(context, option.id)
                        }
                        LyricSettingsFontRow(
                            option = option,
                            selected = selectedFontId == option.id,
                            typeface = optionTypeface,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onSelect = { onSelectFont(option.id) },
                            onDelete = {
                                if (!option.isDefault) {
                                    onDeleteFont(option.id)
                                }
                            }
                        )
                    }
                }

                LyricSettingsPage.LYRIC_PLAYBACK_STATE -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_playback_state),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    val playedAlphaPercent = (tempPlayedLyricAlpha * 100f).roundToInt()
                    Text(
                        text = stringResource(R.string.lyric_settings_played_alpha, playedAlphaPercent),
                        color = contentColor
                    )
                    Slider(
                        value = tempPlayedLyricAlpha,
                        onValueChange = {
                            tempPlayedLyricAlpha = it.coerceIn(0f, 1f)
                            onPlayedLyricAlphaChange(tempPlayedLyricAlpha)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.lyric_settings_hidden), fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text(stringResource(R.string.lyric_settings_default), fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("100%", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                    }
                    val contrastPercent = (tempUpcomingLyricContrast * 100f).roundToInt()
                    Text(
                        text = stringResource(R.string.lyric_settings_upcoming_contrast, contrastPercent),
                        color = contentColor
                    )
                    Slider(
                        value = tempUpcomingLyricContrast,
                        onValueChange = {
                            tempUpcomingLyricContrast = it.coerceIn(0f, 1f)
                            onUpcomingLyricContrastChange(tempUpcomingLyricContrast)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.lyric_settings_low), fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text(stringResource(R.string.lyric_settings_default), fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text(stringResource(R.string.lyric_settings_high), fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                    }
                    Text(
                        text = stringResource(R.string.lyric_settings_played_hidden_hint),
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }

                LyricSettingsPage.PAGE_BACKGROUND -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_page_background),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_bg_solid),
                        selected = pageBackgroundMode == LyricPreviewActivity.PAGE_BACKGROUND_SOLID,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { onPageBackgroundModeChange(LyricPreviewActivity.PAGE_BACKGROUND_SOLID) }
                    )
                    LyricSettingsRadioRow(
                        title = stringResource(R.string.lyric_settings_bg_static_blur),
                        selected = pageBackgroundMode == LyricPreviewActivity.PAGE_BACKGROUND_STATIC_BLUR,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { onPageBackgroundModeChange(LyricPreviewActivity.PAGE_BACKGROUND_STATIC_BLUR) }
                    )
                    if (supportsDynamicCoverBackground) {
                        LyricSettingsRadioRow(
                            title = stringResource(R.string.lyric_settings_bg_dynamic),
                            selected = pageBackgroundMode == LyricPreviewActivity.PAGE_BACKGROUND_DYNAMIC_FLOW,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onClick = { onPageBackgroundModeChange(LyricPreviewActivity.PAGE_BACKGROUND_DYNAMIC_FLOW) }
                        )
                    } else {
                        LyricSettingsActionRow(
                            title = stringResource(R.string.lyric_settings_bg_dynamic_android12),
                            contentColor = contentColor.copy(alpha = 0.62f),
                            onClick = {}
                        )
                    }
                }

                LyricSettingsPage.FONT_PREVIEW -> {
                    LyricSettingsSubPageTitle(
                        title = stringResource(R.string.lyric_settings_preview_font),
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.CUSTOM_FONT }
                    )
                    FontPreviewTextBlock(
                        contentColor = contentColor,
                        typeface = previewTypeface
                    )
                }
            }
            }
        }
    }
}

private fun canDrawDesktopLyricOverlay(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
}

private fun desktopLyricOverlaySettingsIntent(context: Context): Intent {
    return Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
}

@Composable
private fun getLyricDisplayPositionLabel(position: Int): String {
    return when (position) {
        LyricPreviewActivity.LYRIC_DISPLAY_POSITION_DEFAULT -> stringResource(R.string.lyric_settings_position_default)
        in Int.MIN_VALUE..-1 -> stringResource(R.string.lyric_settings_position_up, -position)
        0 -> stringResource(R.string.lyric_settings_position_zero)
        else -> stringResource(R.string.lyric_settings_position_down, position)
    }
}

@Composable
private fun getFontWeightLabelForSheet(weight: Int): String {
    val safeWeight = weight.coerceIn(100, 900) / 100 * 100
    return when (safeWeight) {
        100 -> stringResource(R.string.lyric_settings_weight_100)
        200 -> stringResource(R.string.lyric_settings_weight_200)
        300 -> stringResource(R.string.lyric_settings_weight_300)
        400 -> stringResource(R.string.lyric_settings_weight_400)
        500 -> stringResource(R.string.lyric_settings_weight_500)
        600 -> stringResource(R.string.lyric_settings_weight_600)
        700 -> stringResource(R.string.lyric_settings_weight_700)
        800 -> stringResource(R.string.lyric_settings_weight_800)
        900 -> stringResource(R.string.lyric_settings_weight_900)
        else -> stringResource(R.string.lyric_settings_weight_400)
    }
}

@Composable
private fun LyricSettingsSectionTitle(
    title: String,
    contentColor: Color
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = contentColor.copy(alpha = 0.72f),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun LyricSettingsSubPageTitle(
    title: String,
    contentColor: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.lyric_settings_cd_back),
            tint = contentColor,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onBack)
        )
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun LyricSettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = if (enabled) 0.10f else 0.06f))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.52f),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = contentColor.copy(alpha = 0.90f),
                uncheckedTrackColor = contentColor.copy(alpha = 0.30f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun LyricSettingsActionRow(
    title: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = contentColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun LyricSettingsRadioRow(
    title: String,
    selected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accentColor.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = accentColor,
                unselectedColor = contentColor.copy(alpha = 0.65f)
            )
        )
        Text(text = title, color = contentColor)
    }
}

@Composable
private fun LyricSettingsFontRow(
    option: LyricCustomFontOption,
    selected: Boolean,
    typeface: Typeface?,
    contentColor: Color,
    accentColor: Color,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accentColor.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = accentColor,
                unselectedColor = contentColor.copy(alpha = 0.65f)
            )
        )
        Text(
            text = option.displayName,
            color = contentColor,
            fontFamily = typeface?.let { FontFamily(it) },
            modifier = Modifier.weight(1f)
        )
        if (!option.isDefault) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.lyric_settings_cd_delete_font),
                    tint = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun FontPreviewTextBlock(
    contentColor: Color,
    typeface: Typeface?
) {
    val previewFamily = typeface?.let { FontFamily(it) }
    val previewText = """
简体中文：
欢迎使用 LunaBeat，这是一段用于实时预览字体效果的示例文本。
繁體中文：
歡迎使用 LunaBeat，這是一段用於即時預覽字體效果的示例文字。
English：
Welcome to LunaBeat. This is a sample text used to preview font rendering in real time.
日本語：
LunaBeatへようこそ。これはフォントの表示をリアルタイムで確認するためのサンプルテキストです。
한국어：
LunaBeat에 오신 것을 환영합니다. 이 문장은 글꼴 표시 효과를 실시간으로 미리보기 위한 예제 텍스트입니다.
Tiếng Việt（越南语）：
Chào mừng bạn đến với LunaBeat, đây là văn bản mẫu dùng để xem trước hiệu ứng hiển thị phông chữ theo thời gian thực.
ไทย（泰语）：
ยินดีต้อนรับสู่ LunaBeat นี่คือตัวอย่างข้อความสำหรับแสดงตัวอย่างเอฟเฟกต์การแสดงผลของฟอนต์แบบเรียลไทม์
Русский（俄语）：
Добро пожаловать в LunaBeat. Это пример текста для предварительного просмотра отображения шрифта в реальном времени.
""".trimIndent()
    Text(
        text = previewText,
        color = contentColor,
        fontFamily = previewFamily,
        lineHeight = 23.sp,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}
