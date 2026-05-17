package com.example.LyricBox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private enum class LyricSettingsPage {
    MAIN,
    LYRIC_DISPLAY_MODE,
    LYRIC_DISPLAY_POSITION,
    FONT_SIZE,
    FONT_WEIGHT,
    LYRIC_ANIMATION,
    CUSTOM_FONT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingsBottomSheet(
    onDismissRequest: () -> Unit,
    showTranslation: Boolean,
    showTransliteration: Boolean,
    supportsLyricBlur: Boolean,
    lyricBlurEnabled: Boolean,
    lyriconStatusBarEnabled: Boolean,
    keepScreenOnEnabled: Boolean,
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
    onLyriconStatusBarEnabledChange: (Boolean) -> Unit,
    onKeepScreenOnEnabledChange: (Boolean) -> Unit,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var page by remember { mutableStateOf(LyricSettingsPage.MAIN) }
    var tempLyricDisplayPosition by remember(lyricDisplayPosition) { mutableFloatStateOf(lyricDisplayPosition.toFloat()) }
    var tempLyricDisplayMode by remember(lyricDisplayMode) { mutableIntStateOf(lyricDisplayMode) }
    var tempFontSize by remember(fontSize) { mutableFloatStateOf(fontSize) }
    var tempFontWeight by remember(fontWeight) { mutableFloatStateOf(fontWeight.toFloat()) }
    var tempAnimationType by remember(animationType) { mutableIntStateOf(animationType) }
    var tempWordLiftDistanceDp by remember(wordLiftDistanceDp) { mutableFloatStateOf(wordLiftDistanceDp) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 260))
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (page) {
                LyricSettingsPage.MAIN -> {
                    Text(
                        text = "歌词设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
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
                    if (supportsLyricBlur) {
                        LyricSettingsSwitchRow(
                            title = "歌词模糊",
                            checked = lyricBlurEnabled,
                            onCheckedChange = onLyricBlurEnabledChange,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                    LyricSettingsSwitchRow(
                        title = "状态栏歌词（Lyricon）",
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
                        title = "歌词动画",
                        contentColor = contentColor,
                        onClick = {
                            tempAnimationType = animationType
                            tempWordLiftDistanceDp = wordLiftDistanceDp
                            page = LyricSettingsPage.LYRIC_ANIMATION
                        }
                    )
                    LyricSettingsActionRow(
                        title = "自定义字体",
                        contentColor = contentColor,
                        onClick = { page = LyricSettingsPage.CUSTOM_FONT }
                    )
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

                LyricSettingsPage.LYRIC_ANIMATION -> {
                    LyricSettingsSubPageTitle(
                        title = "歌词动画",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    Text(
                        text = "间奏动画",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        modifier = Modifier.padding(top = 4.dp)
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
                    Text(
                        text = "上抬动画",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        modifier = Modifier.padding(top = 12.dp)
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
                    fontOptions.forEach { option ->
                        LyricSettingsFontRow(
                            option = option,
                            selected = selectedFontId == option.id,
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
            imageVector = Icons.Default.ArrowBack,
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
    contentColor: Color,
    accentColor: Color,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
