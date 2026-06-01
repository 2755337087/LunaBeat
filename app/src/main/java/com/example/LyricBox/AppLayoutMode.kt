package com.example.LyricBox

import android.content.Context
import android.content.res.Configuration
import kotlin.math.max
import kotlin.math.min

private const val APP_LAYOUT_PREFS_NAME = "AppSettings"
private const val PREF_KEY_LAYOUT_MODE = "layoutMode"

enum class AppLayoutModePreference(val value: String) {
    AUTO("auto"),
    WATCH("watch"),
    PHONE("phone"),
    TABLET("tablet")
}

enum class AppLayoutProfile {
    WATCH,
    PHONE,
    TABLET
}

fun getSavedAppLayoutModePreference(context: Context): AppLayoutModePreference {
    val savedValue = context.getSharedPreferences(APP_LAYOUT_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREF_KEY_LAYOUT_MODE, AppLayoutModePreference.AUTO.value)
        ?: AppLayoutModePreference.AUTO.value
    return AppLayoutModePreference.entries.firstOrNull { it.value == savedValue }
        ?: AppLayoutModePreference.AUTO
}

fun saveAppLayoutModePreference(context: Context, mode: AppLayoutModePreference) {
    context.getSharedPreferences(APP_LAYOUT_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_KEY_LAYOUT_MODE, mode.value)
        .apply()
}

fun detectAutoAppLayoutProfile(context: Context): AppLayoutProfile {
    val config = context.resources.configuration
    val metrics = context.resources.displayMetrics
    val screenShortEdgePx = min(metrics.widthPixels, metrics.heightPixels)
    val screenLongEdgePx = max(metrics.widthPixels, metrics.heightPixels)
    val screenMinDp = min(config.screenWidthDp, config.screenHeightDp)
    val isWatchFeatureDevice = context.packageManager.hasSystemFeature("android.hardware.type.watch")
    val isWatchUiMode = (
        config.uiMode and Configuration.UI_MODE_TYPE_MASK
        ) == Configuration.UI_MODE_TYPE_WATCH
    val isWatchLikeSmallScreen = (
        isWatchFeatureDevice ||
            isWatchUiMode ||
            config.isScreenRound ||
            screenMinDp <= 260 ||
            (screenShortEdgePx <= 420 && screenLongEdgePx <= 520)
        )
    if (isWatchLikeSmallScreen) {
        return AppLayoutProfile.WATCH
    }
    val isLargeScreenDevice = config.smallestScreenWidthDp >= 600 || config.screenWidthDp >= 900
    return if (isLargeScreenDevice) {
        AppLayoutProfile.TABLET
    } else {
        AppLayoutProfile.PHONE
    }
}

fun resolveAppLayoutProfile(
    context: Context,
    modePreference: AppLayoutModePreference = getSavedAppLayoutModePreference(context)
): AppLayoutProfile {
    return when (modePreference) {
        AppLayoutModePreference.AUTO -> detectAutoAppLayoutProfile(context)
        AppLayoutModePreference.WATCH -> AppLayoutProfile.WATCH
        AppLayoutModePreference.PHONE -> AppLayoutProfile.PHONE
        AppLayoutModePreference.TABLET -> AppLayoutProfile.TABLET
    }
}

fun getAppLayoutModeSummary(context: Context, modePreference: AppLayoutModePreference): String {
    return if (modePreference == AppLayoutModePreference.AUTO) {
        context.getString(
            R.string.settings_layout_mode_current_profile,
            modePreference.getDisplayName(context),
            detectAutoAppLayoutProfile(context).getDisplayName(context)
        )
    } else {
        modePreference.getDisplayName(context)
    }
}

fun AppLayoutModePreference.getDisplayName(context: Context): String = when (this) {
    AppLayoutModePreference.AUTO -> context.getString(R.string.settings_layout_mode_auto)
    AppLayoutModePreference.WATCH -> context.getString(R.string.settings_layout_mode_watch)
    AppLayoutModePreference.PHONE -> context.getString(R.string.settings_layout_mode_phone)
    AppLayoutModePreference.TABLET -> context.getString(R.string.settings_layout_mode_tablet)
}

fun AppLayoutProfile.getDisplayName(context: Context): String = when (this) {
    AppLayoutProfile.WATCH -> context.getString(R.string.settings_layout_mode_watch)
    AppLayoutProfile.PHONE -> context.getString(R.string.settings_layout_mode_phone)
    AppLayoutProfile.TABLET -> context.getString(R.string.settings_layout_mode_tablet)
}
