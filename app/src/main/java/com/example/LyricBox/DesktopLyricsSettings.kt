package com.example.LyricBox

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

data class DesktopLyricsSettings(
    val enabled: Boolean,
    val widthPercent: Int,
    val fontSizeSp: Float,
    val fontWeight: Int,
    val strokeEnabled: Boolean,
    val showTranslation: Boolean,
    val useCustomFont: Boolean,
    val align: Int,
    val xPercent: Int,
    val yPercent: Int,
    val colorKey: String
)

object DesktopLyricsSettingsStore {
    val colorPresets: List<Pair<String, Color>> = listOf(
        "white" to Color(0xFFFFFFFF),
        "black" to Color(0xFF111111),
        "theme" to Color(0xFF4A90E2),
        "gray" to Color(0xFFDADADA),
        "yellow" to Color(0xFFFFE066)
    )

    fun load(prefs: SharedPreferences): DesktopLyricsSettings {
        val alignRaw = prefs.getInt(
            LyricPreviewActivity.KEY_DESKTOP_LYRIC_ALIGN,
            LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_ALIGN
        )
        val align = when (alignRaw) {
            LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_LEFT,
            LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_CENTER,
            LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_RIGHT -> alignRaw
            else -> LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_ALIGN
        }
        val colorKey = prefs.getString(
            LyricPreviewActivity.KEY_DESKTOP_LYRIC_COLOR,
            LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_COLOR
        ).orEmpty().ifBlank { LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_COLOR }
        return DesktopLyricsSettings(
            enabled = prefs.getBoolean(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_ENABLED,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_ENABLED
            ),
            widthPercent = prefs.getInt(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_WIDTH_PERCENT,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_WIDTH_PERCENT
            ).coerceIn(50, 100),
            fontSizeSp = prefs.getFloat(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_FONT_SIZE_SP,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_FONT_SIZE_SP
            ).coerceIn(8f, 30f),
            fontWeight = prefs.getInt(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_FONT_WEIGHT,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_FONT_WEIGHT
            ).coerceIn(400, 900),
            strokeEnabled = prefs.getBoolean(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_STROKE_ENABLED,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_STROKE_ENABLED
            ),
            showTranslation = prefs.getBoolean(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_SHOW_TRANSLATION,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_SHOW_TRANSLATION
            ),
            useCustomFont = prefs.getBoolean(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_USE_CUSTOM_FONT,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_USE_CUSTOM_FONT
            ),
            align = align,
            xPercent = prefs.getInt(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_X_PERCENT,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_X_PERCENT
            ).coerceIn(0, 100),
            yPercent = prefs.getInt(
                LyricPreviewActivity.KEY_DESKTOP_LYRIC_Y_PERCENT,
                LyricPreviewActivity.DEFAULT_DESKTOP_LYRIC_Y_PERCENT
            ).coerceIn(0, 100),
            colorKey = colorKey
        )
    }

    fun save(context: Context, settings: DesktopLyricsSettings) {
        val prefs = context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(LyricPreviewActivity.KEY_DESKTOP_LYRIC_ENABLED, settings.enabled)
            .putInt(LyricPreviewActivity.KEY_DESKTOP_LYRIC_WIDTH_PERCENT, settings.widthPercent.coerceIn(50, 100))
            .putFloat(LyricPreviewActivity.KEY_DESKTOP_LYRIC_FONT_SIZE_SP, settings.fontSizeSp.coerceIn(8f, 30f))
            .putInt(LyricPreviewActivity.KEY_DESKTOP_LYRIC_FONT_WEIGHT, settings.fontWeight.coerceIn(400, 900))
            .putBoolean(LyricPreviewActivity.KEY_DESKTOP_LYRIC_STROKE_ENABLED, settings.strokeEnabled)
            .putBoolean(LyricPreviewActivity.KEY_DESKTOP_LYRIC_SHOW_TRANSLATION, settings.showTranslation)
            .putBoolean(LyricPreviewActivity.KEY_DESKTOP_LYRIC_USE_CUSTOM_FONT, settings.useCustomFont)
            .putInt(LyricPreviewActivity.KEY_DESKTOP_LYRIC_ALIGN, settings.align)
            .putInt(LyricPreviewActivity.KEY_DESKTOP_LYRIC_X_PERCENT, settings.xPercent.coerceIn(0, 100))
            .putInt(LyricPreviewActivity.KEY_DESKTOP_LYRIC_Y_PERCENT, settings.yPercent.coerceIn(0, 100))
            .putString(LyricPreviewActivity.KEY_DESKTOP_LYRIC_COLOR, settings.colorKey)
            .apply()
    }

    fun colorForKey(key: String): Color {
        return colorPresets.firstOrNull { it.first == key }?.second ?: colorPresets.first().second
    }
}
