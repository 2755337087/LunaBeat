package com.example.LyricBox

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio

data class AppUCropThemeColors(
    val toolbarColor: Int,
    val toolbarWidgetColor: Int,
    val activeControlsWidgetColor: Int,
    val rootViewBackgroundColor: Int
)

@Composable
internal fun rememberAppUCropThemeColors(): AppUCropThemeColors {
    val colorScheme = MaterialTheme.colorScheme
    return remember(
        colorScheme.primary,
        colorScheme.onPrimary,
        colorScheme.background
    ) {
        AppUCropThemeColors(
            toolbarColor = colorScheme.primary.toArgb(),
            toolbarWidgetColor = colorScheme.onPrimary.toArgb(),
            activeControlsWidgetColor = colorScheme.primary.toArgb(),
            rootViewBackgroundColor = colorScheme.background.toArgb()
        )
    }
}

internal fun createAppImageCropOptions(
    title: String,
    compressionFormat: Bitmap.CompressFormat,
    themeColors: AppUCropThemeColors,
    compressionQuality: Int = 100
): UCrop.Options {
    return UCrop.Options().apply {
        setToolbarTitle(title)
        setCompressionFormat(compressionFormat)
        setCompressionQuality(compressionQuality)
        setFreeStyleCropEnabled(true)
        setToolbarColor(themeColors.toolbarColor)
        setToolbarWidgetColor(themeColors.toolbarWidgetColor)
        setActiveControlsWidgetColor(themeColors.activeControlsWidgetColor)
        setRootViewBackgroundColor(themeColors.rootViewBackgroundColor)
        setAspectRatioOptions(
            1,
            AspectRatio("原图", 0f, 0f),
            AspectRatio("1:1", 1f, 1f),
            AspectRatio("4:3", 4f, 3f),
            AspectRatio("3:4", 3f, 4f),
            AspectRatio("16:9", 16f, 9f),
            AspectRatio("9:16", 9f, 16f)
        )
    }
}
