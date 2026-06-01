package com.example.LyricBox

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.example.LyricBox.ui.components.MenuItem

enum class AppPageDestination(@StringRes val titleRes: Int) {
    LYRIC_TIMING(R.string.app_page_lyric_timing),
    MUSIC_LIBRARY(R.string.app_page_music_library),
    SETTINGS(R.string.app_page_settings),
    ABOUT(R.string.app_page_about)
}

fun buildPageSwitchMenuItems(
    context: Context,
    currentPage: AppPageDestination,
    includeCurrentPage: Boolean = true
): List<MenuItem> {
    return AppPageDestination.values()
        .filter { destination -> includeCurrentPage || destination != currentPage }
        .map { destination ->
        val baseTitle = context.getString(destination.titleRes)
        val title = if (destination == currentPage) {
            context.getString(R.string.app_page_current_suffix_format, baseTitle)
        } else {
            baseTitle
        }
        MenuItem(
            title = title,
            onClick = {
                navigateToPage(context, currentPage, destination)
            }
        )
    }
}

fun navigateToPage(
    context: Context,
    currentPage: AppPageDestination,
    targetPage: AppPageDestination
) {
    if (currentPage == targetPage) return

    val targetClass = when (targetPage) {
        AppPageDestination.LYRIC_TIMING -> LyricTimingActivity::class.java
        AppPageDestination.MUSIC_LIBRARY -> MusicLibraryActivity::class.java
        AppPageDestination.SETTINGS -> SettingsActivity::class.java
        AppPageDestination.ABOUT -> AboutActivity::class.java
    }

    val intent = Intent(context, targetClass)
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
