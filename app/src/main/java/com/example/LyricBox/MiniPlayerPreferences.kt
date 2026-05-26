package com.example.LyricBox

import android.content.Context
import android.os.Build

const val MUSIC_LIBRARY_SETTINGS_PREFS_NAME = "MusicLibrarySettings"

const val KEY_MINI_PLAYER_MANUALLY_HIDDEN = "mini_player_manually_hidden"
const val KEY_MINI_PLAYER_BACKGROUND_MODE = "mini_player_background_mode"
const val MINI_PLAYER_BACKGROUND_MODE_DEFAULT = 0
const val MINI_PLAYER_BACKGROUND_MODE_COVER_COLOR = 1
const val MINI_PLAYER_BACKGROUND_MODE_COVER_BLUR = 2
const val MINI_PLAYER_BACKGROUND_MODE_REALTIME_BLUR = 3
const val DEFAULT_MINI_PLAYER_BACKGROUND_MODE = MINI_PLAYER_BACKGROUND_MODE_DEFAULT

const val MINI_PLAYER_BACKGROUND_STYLE_SOLID = 0
const val MINI_PLAYER_BACKGROUND_STYLE_COVER_COLOR = 1
const val MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR = 2
const val MINI_PLAYER_BACKGROUND_STYLE_REALTIME_BLUR = 3

const val KEY_MINI_PLAYER_LANDSCAPE_ALIGNMENT = "mini_player_landscape_alignment"
const val MINI_PLAYER_LANDSCAPE_ALIGNMENT_END = 0
const val MINI_PLAYER_LANDSCAPE_ALIGNMENT_CENTER = 1
const val MINI_PLAYER_LANDSCAPE_ALIGNMENT_START = 2
const val DEFAULT_MINI_PLAYER_LANDSCAPE_ALIGNMENT = MINI_PLAYER_LANDSCAPE_ALIGNMENT_END

fun normalizeMiniPlayerBackgroundMode(mode: Int): Int {
    return when (mode) {
        MINI_PLAYER_BACKGROUND_MODE_DEFAULT,
        MINI_PLAYER_BACKGROUND_MODE_COVER_COLOR,
        MINI_PLAYER_BACKGROUND_MODE_COVER_BLUR -> mode
        MINI_PLAYER_BACKGROUND_MODE_REALTIME_BLUR -> MINI_PLAYER_BACKGROUND_MODE_REALTIME_BLUR
        else -> DEFAULT_MINI_PLAYER_BACKGROUND_MODE
    }
}

fun normalizeMiniPlayerLandscapeAlignment(alignment: Int): Int {
    return when (alignment) {
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_END,
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_CENTER,
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_START -> alignment
        else -> DEFAULT_MINI_PLAYER_LANDSCAPE_ALIGNMENT
    }
}

fun getMiniPlayerBackgroundModeLabel(mode: Int): String {
    return when (normalizeMiniPlayerBackgroundMode(mode)) {
        MINI_PLAYER_BACKGROUND_MODE_COVER_COLOR -> "封面取色"
        MINI_PLAYER_BACKGROUND_MODE_COVER_BLUR -> "封面模糊"
        MINI_PLAYER_BACKGROUND_MODE_REALTIME_BLUR -> "液态玻璃"
        else -> "默认"
    }
}

fun getMiniPlayerLandscapeAlignmentLabel(alignment: Int): String {
    return when (normalizeMiniPlayerLandscapeAlignment(alignment)) {
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_CENTER -> "居中"
        MINI_PLAYER_LANDSCAPE_ALIGNMENT_START -> "居左"
        else -> "居右"
    }
}

fun normalizeLyricPageBackgroundModeForMiniPlayer(
    mode: Int,
    supportsDynamicCoverBackground: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
): Int {
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

fun getSavedMiniPlayerBackgroundMode(context: Context): Int {
    val prefs = context.getSharedPreferences(MUSIC_LIBRARY_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    return normalizeMiniPlayerBackgroundMode(
        prefs.getInt(KEY_MINI_PLAYER_BACKGROUND_MODE, DEFAULT_MINI_PLAYER_BACKGROUND_MODE)
    )
}

fun getSavedMiniPlayerLandscapeAlignment(context: Context): Int {
    val prefs = context.getSharedPreferences(MUSIC_LIBRARY_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    return normalizeMiniPlayerLandscapeAlignment(
        prefs.getInt(KEY_MINI_PLAYER_LANDSCAPE_ALIGNMENT, DEFAULT_MINI_PLAYER_LANDSCAPE_ALIGNMENT)
    )
}

fun getSavedLyricPageBackgroundModeForMiniPlayer(context: Context): Int {
    val prefs = context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE)
    return if (prefs.contains(LyricPreviewActivity.KEY_PAGE_BACKGROUND_MODE)) {
        normalizeLyricPageBackgroundModeForMiniPlayer(
            prefs.getInt(
                LyricPreviewActivity.KEY_PAGE_BACKGROUND_MODE,
                LyricPreviewActivity.DEFAULT_PAGE_BACKGROUND_MODE
            )
        )
    } else {
        val legacyDynamicEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            prefs.getBoolean(
                LyricPreviewActivity.KEY_DYNAMIC_COVER_BACKGROUND,
                LyricPreviewActivity.DEFAULT_DYNAMIC_COVER_BACKGROUND
            )
        if (legacyDynamicEnabled) {
            LyricPreviewActivity.PAGE_BACKGROUND_DYNAMIC_FLOW
        } else {
            LyricPreviewActivity.DEFAULT_PAGE_BACKGROUND_MODE
        }
    }
}

fun resolveMiniPlayerBackgroundStyle(
    backgroundMode: Int,
    pageBackgroundMode: Int
): Int {
    return when (normalizeMiniPlayerBackgroundMode(backgroundMode)) {
        MINI_PLAYER_BACKGROUND_MODE_COVER_COLOR -> MINI_PLAYER_BACKGROUND_STYLE_COVER_COLOR
        MINI_PLAYER_BACKGROUND_MODE_COVER_BLUR -> MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR
        MINI_PLAYER_BACKGROUND_MODE_REALTIME_BLUR -> MINI_PLAYER_BACKGROUND_STYLE_REALTIME_BLUR
        else -> when (normalizeLyricPageBackgroundModeForMiniPlayer(pageBackgroundMode)) {
            LyricPreviewActivity.PAGE_BACKGROUND_SOLID -> MINI_PLAYER_BACKGROUND_STYLE_COVER_COLOR
            else -> MINI_PLAYER_BACKGROUND_STYLE_COVER_BLUR
        }
    }
}
