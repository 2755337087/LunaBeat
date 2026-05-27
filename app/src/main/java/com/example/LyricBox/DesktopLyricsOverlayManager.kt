package com.example.LyricBox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

class DesktopLyricsOverlayManager(
    private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val containerView = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        alpha = 0f
        visibility = View.GONE
        setPadding(24, 8, 24, 8)
    }
    private val mainTextView = StrokeTextView(context).apply {
        includeFontPadding = false
        setLineSpacing(0f, 1.08f)
    }
    private val translationTextView = StrokeTextView(context).apply {
        includeFontPadding = false
        setLineSpacing(0f, 1.06f)
    }

    private var attached = false
    private var isVisible = false
    private var currentSettings: DesktopLyricsSettings? = null
    private var lastMainText: String = ""
    private var lastTranslationText: String = ""

    init {
        containerView.addView(
            mainTextView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        containerView.addView(
            translationTextView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    fun applySettings(
        settings: DesktopLyricsSettings,
        selectedFontId: String
    ) {
        currentSettings = settings
        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val widthPx = (screenWidth * (settings.widthPercent.coerceIn(50, 100) / 100f)).roundToInt()
        val xPx = ((screenWidth - widthPx) * (settings.xPercent.coerceIn(0, 100) / 100f)).roundToInt()
        val yPx = (screenHeight * (settings.yPercent.coerceIn(0, 100) / 100f)).roundToInt()
        val colorInt = DesktopLyricsSettingsStore.colorForKey(settings.colorKey).toArgb()

        val typeface = if (settings.useCustomFont) {
            LyricCustomFontStore.resolveTypefaceById(context, selectedFontId) ?: Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT_BOLD
        }
        val fontStyle = if (settings.fontWeight >= 700) Typeface.BOLD else Typeface.NORMAL
        val finalTypeface = Typeface.create(typeface, fontStyle)

        val gravity = when (settings.align) {
            LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_LEFT -> Gravity.START
            LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_RIGHT -> Gravity.END
            else -> Gravity.CENTER_HORIZONTAL
        }
        containerView.gravity = gravity
        mainTextView.gravity = gravity
        translationTextView.gravity = gravity

        mainTextView.setTextColor(colorInt)
        translationTextView.setTextColor(colorInt)
        mainTextView.typeface = finalTypeface
        translationTextView.typeface = finalTypeface
        mainTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp.coerceIn(8f, 30f))
        translationTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            (settings.fontSizeSp * 0.68f).coerceIn(6f, 22f)
        )
        val strokeColor = if (colorInt == Color.BLACK) Color.WHITE else Color.BLACK
        val strokeWidth = if (settings.strokeEnabled) (density * 1.25f) else 0f
        mainTextView.setStroke(strokeWidth, strokeColor)
        translationTextView.setStroke(strokeWidth * 0.9f, strokeColor)

        translationTextView.visibility = if (settings.showTranslation) View.VISIBLE else View.GONE

        val params = layoutParams(
            widthPx = widthPx.coerceAtLeast((160 * density).roundToInt()),
            xPx = xPx,
            yPx = yPx
        )
        if (attached) {
            windowManager.updateViewLayout(containerView, params)
        }
    }

    fun setLyric(main: String, translation: String, animate: Boolean) {
        val settings = currentSettings ?: return
        val normalizedMain = normalizeReadable(main)
        val normalizedTranslation = normalizeReadable(translation)
        if (normalizedMain == lastMainText && normalizedTranslation == lastTranslationText) return

        val availableWidth = (layoutParamsForMeasure()?.width ?: context.resources.displayMetrics.widthPixels)
            .coerceAtLeast(120)
            .minus(containerView.paddingLeft + containerView.paddingRight)
            .coerceAtLeast(120)
        val mainWrapped = formatTextWithHeadProtection(normalizedMain, mainTextView.paint, availableWidth)
        val translationWrapped = formatTextWithHeadProtection(normalizedTranslation, translationTextView.paint, availableWidth)
        val updateText: () -> Unit = {
            mainTextView.text = mainWrapped
            translationTextView.text = translationWrapped
            translationTextView.visibility = if (settings.showTranslation && translationWrapped.isNotBlank()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        if (!isVisible || !animate) {
            updateText()
            containerView.alpha = 1f
        } else {
            containerView.animate()
                .alpha(0f)
                .setDuration(150L)
                .withEndAction {
                    updateText()
                    containerView.animate()
                        .alpha(1f)
                        .setDuration(170L)
                        .start()
                }
                .start()
        }
        lastMainText = normalizedMain
        lastTranslationText = normalizedTranslation
    }

    fun setVisible(visible: Boolean, animated: Boolean) {
        if (visible) {
            ensureAttached()
            if (isVisible) return
            containerView.visibility = View.VISIBLE
            if (animated) {
                containerView.alpha = 0f
                containerView.animate().alpha(1f).setDuration(220L).start()
            } else {
                containerView.alpha = 1f
            }
        } else {
            if (!attached || !isVisible) {
                containerView.visibility = View.GONE
                isVisible = false
                return
            }
            if (animated) {
                containerView.animate()
                    .alpha(0f)
                    .setDuration(220L)
                    .withEndAction {
                        containerView.visibility = View.GONE
                    }
                    .start()
            } else {
                containerView.alpha = 0f
                containerView.visibility = View.GONE
            }
        }
        isVisible = visible
    }

    fun release() {
        isVisible = false
        if (attached) {
            runCatching { windowManager.removeViewImmediate(containerView) }
            attached = false
        }
    }

    private fun ensureAttached() {
        if (attached) return
        windowManager.addView(containerView, layoutParamsForMeasure() ?: layoutParams(480, 0, 0))
        attached = true
    }

    private fun layoutParamsForMeasure(): WindowManager.LayoutParams? {
        val settings = currentSettings ?: return null
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val widthPx = (screenWidth * (settings.widthPercent.coerceIn(50, 100) / 100f)).roundToInt()
        val xPx = ((screenWidth - widthPx) * (settings.xPercent.coerceIn(0, 100) / 100f)).roundToInt()
        val yPx = (screenHeight * (settings.yPercent.coerceIn(0, 100) / 100f)).roundToInt()
        return layoutParams(widthPx.coerceAtLeast(240), xPx, yPx)
    }

    private fun layoutParams(widthPx: Int, xPx: Int, yPx: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = xPx
            y = yPx
        }
    }

    private fun normalizeReadable(value: String): String {
        return value.replace(Regex("\\s+"), " ").trim()
    }

    private fun formatTextWithHeadProtection(text: String, paint: TextPaint, maxWidthPx: Int): String {
        if (text.isBlank() || maxWidthPx <= 20) return text
        val chars = text.toCharArray()
        val candidateBreaks = mutableListOf<Int>()
        for (i in chars.indices) {
            if (chars[i] != ' ') continue
            val prev = chars.getOrNull(i - 1)
            val next = chars.getOrNull(i + 1)
            val shouldKeepWord = prev?.let(::isEnglishLikeCharForSpaceWrap) == true &&
                next?.let(::isEnglishLikeCharForSpaceWrap) == true
            if (!shouldKeepWord) {
                candidateBreaks.add(i)
            }
        }
        if (candidateBreaks.isEmpty()) return text

        var best = text
        var bestLineCount = measureLineCount(text, paint, maxWidthPx)
        var bestVariance = lineWidthVariance(text, paint, maxWidthPx)
        for (index in candidateBreaks) {
            val candidate = chars.copyOf().also { it[index] = '\n' }.concatToString()
            val candidateAdjusted = protectLeadingSymbols(candidate)
            val lineCount = measureLineCount(candidateAdjusted, paint, maxWidthPx)
            val variance = lineWidthVariance(candidateAdjusted, paint, maxWidthPx)
            if (lineCount < bestLineCount || (lineCount == bestLineCount && variance < bestVariance)) {
                best = candidateAdjusted
                bestLineCount = lineCount
                bestVariance = variance
            }
        }
        return best
    }

    private fun lineWidthVariance(text: String, paint: TextPaint, maxWidthPx: Int): Double {
        val layout = createStaticLayout(text, paint, maxWidthPx)
        if (layout.lineCount <= 0) return Double.MAX_VALUE
        val widths = (0 until layout.lineCount).map { idx ->
            (layout.getLineRight(idx) - layout.getLineLeft(idx)).toDouble()
        }
        val avg = widths.average()
        return widths.sumOf { (it - avg) * (it - avg) }
    }

    private fun measureLineCount(text: String, paint: TextPaint, maxWidthPx: Int): Int {
        return createStaticLayout(text, paint, maxWidthPx).lineCount
    }

    private fun createStaticLayout(text: String, paint: TextPaint, maxWidthPx: Int): StaticLayout {
        val width = maxWidthPx.coerceAtLeast(20)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(0f, 1.08f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.08f, 0f, false)
        }
    }

    private fun protectLeadingSymbols(text: String): String {
        val punctuation = setOf('，', '。', '！', '？', '：', '；', '）', ')', '》', '」', '』', '】', ',', '.', '!', '?', ':', ';')
        val smallKana = setOf('ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ゃ', 'ゅ', 'ょ', 'ゎ', 'ゕ', 'ゖ', 'っ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ヮ', 'ヵ', 'ヶ', 'ッ')
        val lines = text.split('\n').toMutableList()
        if (lines.size <= 1) return text
        for (i in 1 until lines.size) {
            val line = lines[i].trimStart()
            if (line.isEmpty()) continue
            val firstChar = line.first()
            if ((firstChar in punctuation || firstChar in smallKana) && lines[i - 1].length > 1) {
                val prev = lines[i - 1]
                val moveChar = prev.last()
                lines[i - 1] = prev.dropLast(1).trimEnd()
                lines[i] = "$moveChar${lines[i]}"
            }
        }
        return lines.joinToString("\n")
    }

    private fun isEnglishLikeCharForSpaceWrap(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z') || c == '\'' || c == '"'
    }
}

private class StrokeTextView(context: Context) : AppCompatTextView(context) {
    private var strokeWidthPx: Float = 0f
    private var strokeColor: Int = Color.BLACK

    fun setStroke(widthPx: Float, color: Int) {
        strokeWidthPx = widthPx.coerceAtLeast(0f)
        strokeColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (strokeWidthPx > 0f) {
            val oldStyle = paint.style
            val oldStrokeWidth = paint.strokeWidth
            val oldColor = currentTextColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidthPx
            setTextColor(strokeColor)
            super.onDraw(canvas)
            paint.style = oldStyle
            paint.strokeWidth = oldStrokeWidth
            setTextColor(oldColor)
        }
        super.onDraw(canvas)
    }
}
