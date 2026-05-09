package com.example.LyricBox

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.LyricBox.ui.components.MenuItem

enum class AppPageDestination(val title: String) {
    LYRIC_TIMING("歌词打轴"),
    MUSIC_LIBRARY("音乐库"),
    SETTINGS("设置"),
    ABOUT("关于")
}

fun buildPageSwitchMenuItems(
    context: Context,
    currentPage: AppPageDestination,
    includeCurrentPage: Boolean = true
): List<MenuItem> {
    return AppPageDestination.values()
        .filter { destination -> includeCurrentPage || destination != currentPage }
        .map { destination ->
        val title = if (destination == currentPage) "${destination.title}（当前）" else destination.title
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
