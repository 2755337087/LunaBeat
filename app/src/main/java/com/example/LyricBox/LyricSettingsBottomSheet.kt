package com.example.LyricBox

import android.content.res.Configuration
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt

private enum class LyricSettingsPage {
    MAIN,
    LYRIC_DISPLAY_MODE,
    LYRIC_DISPLAY_POSITION,
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
    showTranslation: Boolean,
    showTransliteration: Boolean,
    supportsLyricBlur: Boolean,
    supportsDynamicCoverBackground: Boolean,
    lyricBlurEnabled: Boolean,
    lyricGlowEnabled: Boolean,
    dynamicCoverBackgroundEnabled: Boolean,
    lyriconStatusBarEnabled: Boolean,
    keepScreenOnEnabled: Boolean,
    autoHidePlaybackControlsEnabled: Boolean,
    lyricDisplayMode: Int,
    lyricDisplayPosition: Int,
    fontSize: Float,
    fontWeight: Int,
    animationType: Int,
    wordLiftDistanceDp: Float,
    fontOptions: List<LyricCustomFontOption>,
    selectedFontId: String,
    onShowTranslationChange: (Boolean) -> Unit,
    onShowTransliterationChange: (Boolean) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onLyricGlowEnabledChange: (Boolean) -> Unit,
    onDynamicCoverBackgroundEnabledChange: (Boolean) -> Unit,
    onLyriconStatusBarEnabledChange: (Boolean) -> Unit,
    onKeepScreenOnEnabledChange: (Boolean) -> Unit,
    onAutoHidePlaybackControlsEnabledChange: (Boolean) -> Unit,
    onLyricDisplayModeChange: (Int) -> Unit,
    onLyricDisplayPositionChange: (Int) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontWeightChange: (Int) -> Unit,
    onAnimationTypeChange: (Int) -> Unit,
    onWordLiftDistanceDpChange: (Float) -> Unit,
    onOpenCustomFontPicker: () -> Unit,
    onSelectFont: (String) -> Unit,
    onDeleteFont: (String) -> Unit,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    val isPortraitMode = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val blockSheetDragFromList = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(x = 0f, y = available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(x = 0f, y = available.y)
        }
    }
    var page by remember { mutableStateOf(LyricSettingsPage.MAIN) }
    var tempLyricDisplayPosition by remember(lyricDisplayPosition) { mutableFloatStateOf(lyricDisplayPosition.toFloat()) }
    var tempLyricDisplayMode by remember(lyricDisplayMode) { mutableIntStateOf(lyricDisplayMode) }
    var tempFontSize by remember(fontSize) { mutableFloatStateOf(fontSize) }
    var tempFontWeight by remember(fontWeight) { mutableFloatStateOf(fontWeight.toFloat()) }
    var tempAnimationType by remember(animationType) { mutableIntStateOf(animationType) }
    var tempWordLiftDistanceDp by remember(wordLiftDistanceDp) { mutableFloatStateOf(wordLiftDistanceDp) }
    val previewTypeface = remember(selectedFontId, fontOptions) {
        LyricCustomFontStore.resolveTypefaceById(context, selectedFontId)
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
                .nestedScroll(blockSheetDragFromList)
                .animateContentSize(animationSpec = tween(durationMillis = 260))
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                when (page) {
                LyricSettingsPage.MAIN -> {
                    Text(
                        text = "歌词设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    LyricSettingsSectionTitle(
                        title = "歌词显示",
                        contentColor = contentColor
                    )
                    LyricSettingsSwitchRow(
                        title = "显示翻译",
                        checked = showTranslation,
                        onCheckedChange = onShowTranslationChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsSwitchRow(
                        title = "显示注音",
                        checked = showTransliteration,
                        onCheckedChange = onShowTransliterationChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsActionRow(
                        title = "歌词显示模式",
                        contentColor = contentColor,
                        onClick = {
                            tempLyricDisplayMode = lyricDisplayMode
                            page = LyricSettingsPage.LYRIC_DISPLAY_MODE
                        }
                    )
                    LyricSettingsActionRow(
                        title = "歌词显示位置",
                        contentColor = contentColor,
                        onClick = {
                            tempLyricDisplayPosition = lyricDisplayPosition.toFloat()
                            page = LyricSettingsPage.LYRIC_DISPLAY_POSITION
                        }
                    )
                    LyricSettingsActionRow(
                        title = "字体大小",
                        contentColor = contentColor,
                        onClick = {
                            tempFontSize = fontSize
                            page = LyricSettingsPage.FONT_SIZE
                        }
                    )
                    LyricSettingsActionRow(
                        title = "字体粗细",
                        contentColor = contentColor,
                        onClick = {
                            tempFontWeight = fontWeight.toFloat()
                            page = LyricSettingsPage.FONT_WEIGHT
                        }
                    )
                    LyricSettingsActionRow(
                        title = "自定义字体",
                        contentColor = contentColor,
                        onClick = { page = LyricSettingsPage.CUSTOM_FONT }
                    )

                    LyricSettingsSectionTitle(
                        title = "动画效果",
                        contentColor = contentColor
                    )
                    LyricSettingsActionRow(
                        title = "间奏动画",
                        contentColor = contentColor,
                        onClick = {
                            tempAnimationType = animationType
                            page = LyricSettingsPage.INTERLUDE_ANIMATION
                        }
                    )
                    LyricSettingsActionRow(
                        title = "上抬动画",
                        contentColor = contentColor,
                        onClick = {
                            tempWordLiftDistanceDp = wordLiftDistanceDp
                            page = LyricSettingsPage.WORD_LIFT_ANIMATION
                        }
                    )
                    LyricSettingsSwitchRow(
                        title = "歌词发光",
                        checked = lyricGlowEnabled,
                        onCheckedChange = onLyricGlowEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    if (supportsDynamicCoverBackground) {
                        LyricSettingsSwitchRow(
                            title = "封面流光背景",
                            checked = dynamicCoverBackgroundEnabled,
                            onCheckedChange = onDynamicCoverBackgroundEnabledChange,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                    if (supportsLyricBlur) {
                        LyricSettingsSwitchRow(
                            title = "歌词模糊",
                            checked = lyricBlurEnabled,
                            onCheckedChange = onLyricBlurEnabledChange,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }

                    LyricSettingsSectionTitle(
                        title = "播放与系统",
                        contentColor = contentColor
                    )
                    LyricSettingsSwitchRow(
                        title = "状态栏歌词（词幕）",
                        checked = lyriconStatusBarEnabled,
                        onCheckedChange = onLyriconStatusBarEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    LyricSettingsSwitchRow(
                        title = "屏幕常亮",
                        checked = keepScreenOnEnabled,
                        onCheckedChange = onKeepScreenOnEnabledChange,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    if (isPortraitMode) {
                        LyricSettingsSwitchRow(
                            title = "自动隐藏播放组件",
                            checked = autoHidePlaybackControlsEnabled,
                            onCheckedChange = onAutoHidePlaybackControlsEnabledChange,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                }

                LyricSettingsPage.LYRIC_DISPLAY_MODE -> {
                    LyricSettingsSubPageTitle(
                        title = "歌词显示模式",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsRadioRow(
                        title = "默认",
                        selected = tempLyricDisplayMode == LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempLyricDisplayMode = LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT
                            onLyricDisplayModeChange(LyricPreviewActivity.LYRIC_DISPLAY_MODE_DEFAULT)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = "强制逐字",
                        selected = tempLyricDisplayMode == LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempLyricDisplayMode = LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD
                            onLyricDisplayModeChange(LyricPreviewActivity.LYRIC_DISPLAY_MODE_FORCE_WORD)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = "强制逐行",
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
                        title = "歌词显示位置",
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
                        text = "当前位置：${getLyricDisplayPositionLabel(snappedPosition)}",
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
                        title = "字体大小",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    Text(
                        text = "当前大小: ${tempFontSize.toInt()}sp",
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
                        title = "字体粗细",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    Text(
                        text = "当前粗细: ${getFontWeightLabelForSheet(tempFontWeight.toInt().coerceIn(100, 900) / 100 * 100)}",
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
                        title = "间奏动画",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsRadioRow(
                        title = "默认 圆点",
                        selected = tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DEFAULT,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DEFAULT
                            onAnimationTypeChange(LyricPreviewActivity.ANIMATION_TYPE_DEFAULT)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = "恐龙",
                        selected = tempAnimationType == LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            tempAnimationType = LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR
                            onAnimationTypeChange(LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR)
                        }
                    )
                    LyricSettingsRadioRow(
                        title = "小狗",
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
                        title = "上抬动画",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    val snappedLiftDistance = tempWordLiftDistanceDp.roundToInt().coerceIn(
                        LyricPreviewActivity.WORD_LIFT_DISTANCE_MIN_DP.toInt(),
                        LyricPreviewActivity.WORD_LIFT_DISTANCE_MAX_DP.toInt()
                    )
                    Text(
                        text = "上抬距离: ${snappedLiftDistance}dp",
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
                        title = "自定义字体",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsActionRow(
                        title = "添加TTF字体",
                        contentColor = contentColor,
                        onClick = onOpenCustomFontPicker
                    )
                    LyricSettingsActionRow(
                        title = "预览字体",
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

                LyricSettingsPage.FONT_PREVIEW -> {
                    LyricSettingsSubPageTitle(
                        title = "预览字体",
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

private fun getLyricDisplayPositionLabel(position: Int): String {
    return when (position) {
        LyricPreviewActivity.LYRIC_DISPLAY_POSITION_DEFAULT -> "默认 上移4档"
        in Int.MIN_VALUE..-1 -> "上移${-position}档"
        0 -> "0档"
        else -> "下移${position}档"
    }
}

private fun getFontWeightLabelForSheet(weight: Int): String {
    val safeWeight = weight.coerceIn(100, 900) / 100 * 100
    return when (safeWeight) {
        100 -> "极细 100"
        200 -> "特细 200"
        300 -> "细 300"
        400 -> "正常 400"
        500 -> "中 500"
        600 -> "半粗 600"
        700 -> "粗 700"
        800 -> "特粗 800"
        900 -> "极粗 900"
        else -> "正常 400"
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
            contentDescription = "返回",
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
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.10f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = contentColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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
                    contentDescription = "删除字体",
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
