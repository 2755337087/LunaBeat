package com.example.LyricBox.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

fun getDarkModeFromSettings(context: Context): Boolean? {
    val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
    val darkModeType = try {
        DarkModeType.valueOf(prefs.getString("darkModeType", DarkModeType.FOLLOW_SYSTEM.name) ?: DarkModeType.FOLLOW_SYSTEM.name)
    } catch (e: Exception) { DarkModeType.FOLLOW_SYSTEM }
    
    return when (darkModeType) {
        DarkModeType.FOLLOW_SYSTEM -> null
        DarkModeType.LIGHT -> false
        DarkModeType.DARK -> true
    }
}

enum class DarkModeType {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun 歌词转换Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsDarkMode = getDarkModeFromSettings(context)
    val effectiveDarkTheme = settingsDarkMode ?: darkTheme
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色为透明
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // 设置状态栏图标颜色（深色模式用浅色图标，浅色模式用深色图标）
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
