package com.example.LyricBox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlin.math.abs
import kotlin.math.min

private const val PALETTE_TARGET_SIZE = 300
private const val PALETTE_EMPTY_COLOR_SENTINEL = Int.MIN_VALUE
private const val COVER_THEME_PREFS = "CoverThemeColorCache"
private const val COVER_THEME_LIGHT_PREFIX = "cover_theme_light_"
private const val COVER_THEME_DARK_PREFIX = "cover_theme_dark_"
private const val COVER_THEME_ALGO_VERSION = "v5_population_min_20_keep_gray_white"
private const val MIN_THEME_POPULATION_RATIO = 0.20f
private const val MIN_THEME_LIGHTNESS = 0f
private const val MAX_THEME_LIGHTNESS = 1f
private const val TARGET_THEME_SATURATION_FLOOR = 0f
private const val TARGET_THEME_SATURATION_CEILING = 0.82f
private const val LIGHT_THEME_LIGHTNESS_MIN = 0.48f
private const val LIGHT_THEME_LIGHTNESS_MAX = 0.78f
private const val DARK_THEME_LIGHTNESS_MIN = 0.20f
private const val DARK_THEME_LIGHTNESS_MAX = 0.44f
private val paletteColorCache = object : LruCache<Int, CoverThemeColorPair>(128) {}
private val localThemePairCache = object : LruCache<String, CoverThemeColorPair>(256) {}

data class CoverThemeColorPair(
    val lightColor: Color?,
    val darkColor: Color?
)

fun buildCoverThemeCacheKey(
    coverCachePath: String?,
    mediaId: String?,
    audioPath: String? = null
): String {
    return when {
        !coverCachePath.isNullOrBlank() -> "coverPath:$coverCachePath"
        !mediaId.isNullOrBlank() -> "mediaId:$mediaId"
        !audioPath.isNullOrBlank() -> "audioPath:$audioPath"
        else -> "default"
    }
}

fun resolveCachedCoverThemePair(
    context: Context,
    cacheKey: String,
    cover: Bitmap?
): CoverThemeColorPair? {
    val resolvedKey = cacheKey.ifBlank {
        val bitmap = cover ?: return null
        "bitmap:${buildPaletteCacheKey(bitmap)}"
    }
    val versionedKey = "$COVER_THEME_ALGO_VERSION|$resolvedKey"
    synchronized(localThemePairCache) {
        localThemePairCache.get(versionedKey)?.let { return it }
    }
    readCoverThemePairCache(context, versionedKey)?.let { cached ->
        synchronized(localThemePairCache) {
            localThemePairCache.put(versionedKey, cached)
        }
        return cached
    }
    val bitmap = cover ?: return null
    val computed = extractCoverThemePair(bitmap)
    writeCoverThemePairCache(context, versionedKey, computed)
    synchronized(localThemePairCache) {
        localThemePairCache.put(versionedKey, computed)
    }
    return computed
}

fun extractMutedCoverColor(cover: Bitmap, preferDark: Boolean): Color? {
    val pair = extractCoverThemePair(cover)
    return if (preferDark) pair.darkColor else pair.lightColor
}

private fun extractCoverThemePair(cover: Bitmap): CoverThemeColorPair {
    val cacheKey = buildPaletteCacheKey(cover)
    synchronized(paletteColorCache) {
        paletteColorCache.get(cacheKey)?.let { return it }
    }

    val sampleBitmap = buildPaletteSampleBitmap(cover)
    val palette = Palette.from(sampleBitmap)
        .maximumColorCount(36)
        .clearFilters()
        .generate()
    val allSwatches = palette.swatches.sortedByDescending { it.population }
    val totalPopulation = allSwatches.sumOf { it.population }.coerceAtLeast(1)
    val bestSwatch = pickDominantThemeSwatch(
        swatches = allSwatches,
        totalPopulation = totalPopulation
    )
    val themeColorInt = bestSwatch?.rgb ?: palette.dominantSwatch?.rgb
    val lightColor = themeColorInt?.let { adjustThemeColor(it, preferDark = false) }
    val darkColor = themeColorInt?.let { adjustThemeColor(it, preferDark = true) }

    val resolvedPair = CoverThemeColorPair(
        lightColor = lightColor,
        darkColor = darkColor
    )
    synchronized(paletteColorCache) {
        paletteColorCache.put(cacheKey, resolvedPair)
    }
    return resolvedPair
}

private fun buildPaletteSampleBitmap(cover: Bitmap): Bitmap {
    if (cover.width <= 0 || cover.height <= 0) {
        return Bitmap.createBitmap(PALETTE_TARGET_SIZE, PALETTE_TARGET_SIZE, Bitmap.Config.ARGB_8888)
    }

    val minSide = min(cover.width, cover.height).coerceAtLeast(1)
    val squareLeft = ((cover.width - minSide) / 2).coerceAtLeast(0)
    val squareTop = ((cover.height - minSide) / 2).coerceAtLeast(0)
    val squareRight = squareLeft + minSide
    val squareBottom = squareTop + minSide
    val centerInset = (minSide * 0.10f).toInt().coerceAtLeast(0)
    val candidateRect = Rect(
        squareLeft + centerInset,
        squareTop + centerInset,
        squareRight - centerInset,
        squareBottom - centerInset
    )
    val srcRect = if (candidateRect.width() > 1 && candidateRect.height() > 1) {
        candidateRect
    } else {
        Rect(squareLeft, squareTop, squareRight, squareBottom)
    }
    val dstRect = Rect(0, 0, PALETTE_TARGET_SIZE, PALETTE_TARGET_SIZE)
    val output = Bitmap.createBitmap(PALETTE_TARGET_SIZE, PALETTE_TARGET_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    canvas.drawBitmap(cover, srcRect, dstRect, paint)
    return output
}

private fun buildPaletteCacheKey(cover: Bitmap): Int {
    var key = 17
    key = key * 31 + System.identityHashCode(cover)
    key = key * 31 + cover.generationId
    key = key * 31 + cover.width
    key = key * 31 + cover.height
    return key
}

private fun pickDominantThemeSwatch(
    swatches: List<Palette.Swatch>,
    totalPopulation: Int
): Palette.Swatch? {
    if (swatches.isEmpty()) return null

    val eligibleSwatches = swatches.filter { swatch ->
        val ratio = swatch.population.toFloat() / totalPopulation
        ratio >= MIN_THEME_POPULATION_RATIO && shouldKeepPaletteColor(swatch.rgb, swatch.hsl)
    }
    if (eligibleSwatches.isNotEmpty()) {
        return eligibleSwatches.maxByOrNull { scorePopulationFirstSwatch(it, totalPopulation) }
    }
    return null
}

private fun scorePopulationFirstSwatch(
    swatch: Palette.Swatch,
    totalPopulation: Int
): Float {
    val hsl = swatch.hsl
    val populationRatio = (swatch.population.toFloat() / totalPopulation.coerceAtLeast(1)).coerceIn(0f, 1f)
    val saturation = hsl.getOrElse(1) { 0f }.coerceIn(0f, 1f)
    val lightness = hsl.getOrElse(2) { 0f }.coerceIn(0f, 1f)
    val brightnessBalance = (1f - abs(lightness - 0.5f) / 0.5f).coerceIn(0f, 1f)

    return populationRatio * 0.7f +
        saturation * 0.2f +
        brightnessBalance * 0.1f
}

private fun adjustThemeColor(colorInt: Int, preferDark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(colorInt, hsl)

    hsl[1] = hsl[1].coerceIn(TARGET_THEME_SATURATION_FLOOR, TARGET_THEME_SATURATION_CEILING)

    val lightnessRange = if (preferDark) {
        DARK_THEME_LIGHTNESS_MIN..DARK_THEME_LIGHTNESS_MAX
    } else {
        LIGHT_THEME_LIGHTNESS_MIN..LIGHT_THEME_LIGHTNESS_MAX
    }
    hsl[2] = hsl[2].coerceIn(lightnessRange.start, lightnessRange.endInclusive)

    return Color(ColorUtils.HSLToColor(hsl))
}

private fun shouldKeepPaletteColor(@Suppress("UNUSED_PARAMETER") rgb: Int, hsl: FloatArray): Boolean {
    if (hsl.size < 3) return false
    val lightness = hsl[2]
    if (lightness <= MIN_THEME_LIGHTNESS || lightness >= MAX_THEME_LIGHTNESS) return false
    return true
}

private fun readCoverThemePairCache(context: Context, key: String): CoverThemeColorPair? {
    val prefs = context.getSharedPreferences(COVER_THEME_PREFS, Context.MODE_PRIVATE)
    val lightKey = COVER_THEME_LIGHT_PREFIX + key
    val darkKey = COVER_THEME_DARK_PREFIX + key
    if (!prefs.contains(lightKey) || !prefs.contains(darkKey)) return null
    val lightArgb = prefs.getInt(lightKey, PALETTE_EMPTY_COLOR_SENTINEL)
    val darkArgb = prefs.getInt(darkKey, PALETTE_EMPTY_COLOR_SENTINEL)
    return CoverThemeColorPair(
        lightColor = lightArgb.takeUnless { it == PALETTE_EMPTY_COLOR_SENTINEL }?.let { Color(it) },
        darkColor = darkArgb.takeUnless { it == PALETTE_EMPTY_COLOR_SENTINEL }?.let { Color(it) }
    )
}

private fun writeCoverThemePairCache(context: Context, key: String, pair: CoverThemeColorPair) {
    val prefs = context.getSharedPreferences(COVER_THEME_PREFS, Context.MODE_PRIVATE)
    prefs.edit()
        .putInt(
            COVER_THEME_LIGHT_PREFIX + key,
            pair.lightColor?.toArgb() ?: PALETTE_EMPTY_COLOR_SENTINEL
        )
        .putInt(
            COVER_THEME_DARK_PREFIX + key,
            pair.darkColor?.toArgb() ?: PALETTE_EMPTY_COLOR_SENTINEL
        )
        .apply()
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
