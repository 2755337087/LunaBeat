package com.example.LyricBox

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private const val LEGACY_LAYOUT_SYSTEM_BAR_FLAGS =
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

private const val LEGACY_HIDE_SYSTEM_BAR_FLAGS =
    LEGACY_LAYOUT_SYSTEM_BAR_FLAGS or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

private const val LEGACY_SHOW_SYSTEM_BAR_CLEAR_FLAGS =
    View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

fun Activity.applyLyricEdgeToEdgeWindowCompat() {
    window.applyLyricEdgeToEdgeWindowCompat()
}

fun Window.applyLyricEdgeToEdgeWindowCompat() {
    WindowCompat.setDecorFitsSystemWindows(this, false)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val targetCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val layoutParams = attributes
        if (layoutParams.layoutInDisplayCutoutMode != targetCutoutMode) {
            layoutParams.layoutInDisplayCutoutMode = targetCutoutMode
            attributes = layoutParams
        }
    }

    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isStatusBarContrastEnforced = false
        isNavigationBarContrastEnforced = false
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility =
            decorView.systemUiVisibility or LEGACY_LAYOUT_SYSTEM_BAR_FLAGS
    }
}

fun Window.hideLyricSystemBarsCompat() {
    applyLyricEdgeToEdgeWindowCompat()
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility =
            decorView.systemUiVisibility or LEGACY_HIDE_SYSTEM_BAR_FLAGS
    }
}

fun Window.showLyricSystemBarsCompat() {
    applyLyricEdgeToEdgeWindowCompat()
    WindowCompat.getInsetsController(this, decorView)
        .show(WindowInsetsCompat.Type.systemBars())
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility =
            (decorView.systemUiVisibility and LEGACY_SHOW_SYSTEM_BAR_CLEAR_FLAGS.inv()) or
                LEGACY_LAYOUT_SYSTEM_BAR_FLAGS
    }
}
