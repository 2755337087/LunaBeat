package com.example.LyricBox

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlin.math.abs

fun extractMutedCoverColor(cover: Bitmap, preferDark: Boolean): Color? {
    val source = if (cover.width >= 4 && cover.height >= 4) {
        Bitmap.createBitmap(
            cover,
            cover.width / 4,
            cover.height / 4,
            (cover.width / 2).coerceAtLeast(1),
            (cover.height / 2).coerceAtLeast(1)
        )
    } else {
        cover
    }
    val small = Bitmap.createScaledBitmap(source, 120, 120, true)
    val palette = Palette.from(small)
        .maximumColorCount(24)
        .clearFilters()
        .generate()

    val swatches = palette.swatches
    val mutedFallback = pickBestPaletteSwatch(swatches) { swatch, maxPopulation ->
        val hsl = swatch.hsl
        val saturation = hsl[1]
        val lightness = hsl[2]
        val populationScore = swatch.population.toFloat() / maxPopulation
        val satTargetScore = 1f - abs(saturation - 0.28f)
        val lightnessScore = 1f - abs(lightness - 0.52f)
        satTargetScore * 1.2f + lightnessScore * 0.45f + populationScore * 0.35f
    }

    val chosen = if (preferDark) {
        palette.darkMutedSwatch
            ?: palette.mutedSwatch
            ?: mutedFallback
            ?: palette.dominantSwatch
    } else {
        palette.lightMutedSwatch
            ?: palette.mutedSwatch
            ?: mutedFallback
            ?: palette.dominantSwatch
    }

    return chosen?.rgb?.let { Color(it) }
}

private fun pickBestPaletteSwatch(
    swatches: List<Palette.Swatch>,
    score: (swatch: Palette.Swatch, maxPopulation: Int) -> Float
): Palette.Swatch? {
    if (swatches.isEmpty()) return null
    val maxPopulation = swatches.maxOf { it.population }.coerceAtLeast(1)
    var best: Palette.Swatch? = null
    var bestScore = Float.NEGATIVE_INFINITY
    for (swatch in swatches) {
        val swatchScore = score(swatch, maxPopulation)
        if (swatchScore > bestScore) {
            bestScore = swatchScore
            best = swatch
        }
    }
    return best
}

fun blendColorForUi(start: Color, end: Color, fraction: Float): Color {
    val value = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * value,
        green = start.green + (end.green - start.green) * value,
        blue = start.blue + (end.blue - start.blue) * value,
        alpha = start.alpha + (end.alpha - start.alpha) * value
    )
}

fun colorLuminance(color: Color): Float {
    return 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
}

fun normalizeCoverThemeBackground(source: Color, isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        normalizeDarkThemeColor(source)
    } else {
        normalizeLightThemeColor(source)
    }
}

private fun normalizeLightThemeColor(source: Color): Color {
    val minLuminance = 0.62f
    if (colorLuminance(source) >= minLuminance) return source

    var adjusted = source
    repeat(6) {
        adjusted = blendColorForUi(adjusted, Color.White, 0.28f)
        if (colorLuminance(adjusted) >= minLuminance) return adjusted
    }
    return adjusted
}

private fun normalizeDarkThemeColor(source: Color): Color {
    val maxLuminance = 0.36f
    if (colorLuminance(source) <= maxLuminance) return source

    var adjusted = source
    repeat(6) {
        adjusted = blendColorForUi(adjusted, Color.Black, 0.30f)
        if (colorLuminance(adjusted) <= maxLuminance) return adjusted
    }
    return adjusted
}
