package com.example.LyricBox.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AutoMarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    edgeFadeWidth: Dp = 18.dp,
    gapText: String = "       ",
    startPauseMillis: Long = 2000L,
    speedDpPerSecond: Float = 36f
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val textLayout = remember(text, style) {
        textMeasurer.measure(
            text = AnnotatedString(text),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
    val cycleText = remember(text, gapText) { text + gapText }
    val marqueeText = remember(text, gapText) { text + gapText + text }
    val cycleLayout = remember(cycleText, style) {
        textMeasurer.measure(
            text = AnnotatedString(cycleText),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
    val textWidthPx = textLayout.size.width
    val cycleWidthPx = cycleLayout.size.width
    val edgeFadePx = with(density) { edgeFadeWidth.toPx() }
    val speedPxPerSecond = with(density) { speedDpPerSecond.dp.toPx() }.coerceAtLeast(1f)
    val shouldScroll = containerWidthPx > 0 && textWidthPx > containerWidthPx
    val offset = remember(text) { Animatable(0f) }

    LaunchedEffect(text, shouldScroll, cycleWidthPx, containerWidthPx, speedPxPerSecond) {
        offset.snapTo(0f)
        if (!shouldScroll || cycleWidthPx <= 0) return@LaunchedEffect
        val travel = cycleWidthPx.toFloat()
        val durationMillis = ((travel / speedPxPerSecond) * 1000f)
            .roundToInt()
            .coerceAtLeast(1200)
        delay(startPauseMillis)
        while (true) {
            offset.animateTo(
                targetValue = -travel,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
            )
            offset.snapTo(0f)
            delay(startPauseMillis)
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size -> containerWidthPx = size.width }
            .then(
                if (shouldScroll) {
                    Modifier
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            if (edgeFadePx <= 0f || size.width <= edgeFadePx * 2f) return@drawWithContent
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        (edgeFadePx / size.width).coerceIn(0f, 0.5f) to Color.Black,
                                        (1f - edgeFadePx / size.width).coerceIn(0.5f, 1f) to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(size.width, 0f)
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                } else {
                    Modifier
                }
            )
    ) {
        if (shouldScroll) {
            Layout(
                content = {
                    BasicText(
                        text = marqueeText,
                        style = style,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = Int.MAX_VALUE
                    )
                )
                val layoutWidth = if (constraints.hasBoundedWidth) {
                    constraints.maxWidth
                } else {
                    placeable.width
                }
                layout(
                    width = layoutWidth,
                    height = placeable.height
                ) {
                    placeable.placeRelative(offset.value.roundToInt(), 0)
                }
            }
        } else {
            BasicText(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}
