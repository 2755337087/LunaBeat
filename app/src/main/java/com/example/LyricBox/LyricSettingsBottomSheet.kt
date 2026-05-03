package com.example.LyricBox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class LyricSettingsPage {
    MAIN,
    FONT_SIZE,
    FONT_WEIGHT,
    INTERLUDE_ANIMATION
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
    fontSize: Float,
    fontWeight: Int,
    animationType: Int,
    onShowTranslationChange: (Boolean) -> Unit,
    onShowTransliterationChange: (Boolean) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onLyriconStatusBarEnabledChange: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontWeightChange: (Int) -> Unit,
    onAnimationTypeChange: (Int) -> Unit,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var page by remember { mutableStateOf(LyricSettingsPage.MAIN) }
    var tempFontSize by remember(fontSize) { mutableFloatStateOf(fontSize) }
    var tempFontWeight by remember(fontWeight) { mutableFloatStateOf(fontWeight.toFloat()) }
    var tempAnimationType by remember(animationType) { mutableIntStateOf(animationType) }

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
                    LyricSettingsActionRow(
                        title = "字体大小 (${fontSize.toInt()}sp)",
                        contentColor = contentColor,
                        onClick = {
                            tempFontSize = fontSize
                            page = LyricSettingsPage.FONT_SIZE
                        }
                    )
                    LyricSettingsActionRow(
                        title = "字体粗细 (${getFontWeightLabel(fontWeight)})",
                        contentColor = contentColor,
                        onClick = {
                            tempFontWeight = fontWeight.toFloat()
                            page = LyricSettingsPage.FONT_WEIGHT
                        }
                    )
                    LyricSettingsActionRow(
                        title = if (animationType == LyricPreviewActivity.ANIMATION_TYPE_DINOSAUR) {
                            "间奏动画：恐龙"
                        } else {
                            "间奏动画：默认"
                        },
                        contentColor = contentColor,
                        onClick = {
                            tempAnimationType = animationType
                            page = LyricSettingsPage.INTERLUDE_ANIMATION
                        }
                    )
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
                        valueRange = 18f..40f,
                        steps = 21,
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
                        Text("29sp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("40sp", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                    }
                }

                LyricSettingsPage.FONT_WEIGHT -> {
                    LyricSettingsSubPageTitle(
                        title = "字体粗细",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    Text(
                        text = "当前粗细: ${getFontWeightLabel(tempFontWeight.toInt() / 100 * 100)}",
                        color = contentColor
                    )
                    Slider(
                        value = tempFontWeight,
                        onValueChange = { tempFontWeight = it },
                        valueRange = 300f..700f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.25f)
                        ),
                        onValueChangeFinished = {
                            val snappedWeight = tempFontWeight.toInt().coerceIn(300, 700) / 100 * 100
                            tempFontWeight = snappedWeight.toFloat()
                            onFontWeightChange(snappedWeight)
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("细", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("正常", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("中", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("半粗", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                        Text("粗", fontSize = 12.sp, color = contentColor.copy(alpha = 0.82f))
                    }
                }

                LyricSettingsPage.INTERLUDE_ANIMATION -> {
                    LyricSettingsSubPageTitle(
                        title = "间奏动画",
                        contentColor = contentColor,
                        onBack = { page = LyricSettingsPage.MAIN }
                    )
                    LyricSettingsRadioRow(
                        title = "默认（圆点）",
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
                }
            }
        }
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
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.10f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
