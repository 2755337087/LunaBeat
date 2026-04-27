package com.example.LyricBox.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.LyricBox.R

data class MenuItem(
    val title: String,
    val subItems: List<MenuItem>? = null,
    val onClick: (() -> Unit)? = null
)

data class MenuAnchorPosition(
    val x: Float,
    val y: Float
)

private val EmphasizedDecelerate = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val LinearEasing = androidx.compose.animation.core.LinearEasing

private object MotionConstants {
    const val DURATION_ENTER = 300
    const val DURATION_ENTER_SHORT = 200
    const val DURATION_EXIT = 250
    const val DURATION_EXIT_SHORT = 150
}

@Composable
fun CustomDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    anchorPosition: MenuAnchorPosition = MenuAnchorPosition(0f, 0f),
    menuWidth: Float = 200f
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded
    var pendingCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(pendingCallback) {
        if (pendingCallback != null) {
            pendingCallback?.invoke()
            pendingCallback = null
        }
    }

    if (expandedState.currentState || expandedState.targetState || !expandedState.isIdle) {
        var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
        var menuBounds by remember { mutableStateOf(IntRect.Zero) }
        val transformOrigin = remember(anchorBounds, menuBounds) {
            if (menuBounds.center.y >= anchorBounds.center.y) {
                if (menuBounds.center.x >= anchorBounds.center.x) {
                    TransformOrigin(0f, 0f)
                } else {
                    TransformOrigin(1f, 0f)
                }
            } else {
                if (menuBounds.center.x >= anchorBounds.center.x) {
                    TransformOrigin(0f, 1f)
                } else {
                    TransformOrigin(1f, 1f)
                }
            }
        }
        
        val density = LocalDensity.current
        val popupPositionProvider = remember(density, anchorPosition) {
            SimpleDropdownMenuPositionProvider(
                density = density,
                anchorXDp = anchorPosition.x,
                anchorYDp = anchorPosition.y,
                onPositionCalculated = { aBounds, mBounds ->
                    anchorBounds = aBounds
                    menuBounds = mBounds
                }
            )
        }

        Popup(
            onDismissRequest = {
                onDismissRequest()
            },
            popupPositionProvider = popupPositionProvider,
            properties = PopupProperties(focusable = true)
        ) {
            DropdownMenuContent(
                expandedState = expandedState,
                scrollState = rememberScrollState(),
                transformOrigin = transformOrigin,
                items = items,
                onDismissRequest = onDismissRequest,
                onItemClick = { callback ->
                    pendingCallback = callback
                    onDismissRequest()
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DropdownMenuContent(
    expandedState: MutableTransitionState<Boolean>,
    scrollState: ScrollState,
    transformOrigin: TransformOrigin,
    items: List<MenuItem>,
    onDismissRequest: () -> Unit,
    onItemClick: ((() -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentItems by remember { mutableStateOf(items) }
    var previousItems by remember { mutableStateOf<List<MenuItem>?>(null) }
    var parentTitle by remember { mutableStateOf<String?>(null) }
    var isNavigatingBack by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visibleState = expandedState,
        label = "Dropdown menu animation",
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = MotionConstants.DURATION_ENTER,
                easing = EmphasizedDecelerate
            ),
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = MotionConstants.DURATION_ENTER,
                easing = EmphasizedDecelerate
            ),
            initialScale = 0.8f,
            transformOrigin = if (LocalLayoutDirection.current == LayoutDirection.Ltr)
                TransformOrigin(1f, 0f)
            else
                TransformOrigin(0f, 0f),
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = MotionConstants.DURATION_EXIT_SHORT,
                easing = LinearEasing,
            ),
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = MotionConstants.DURATION_EXIT_SHORT,
                easing = LinearEasing,
            ),
            targetScale = 0.8f,
            transformOrigin = transformOrigin,
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
        ) {
            AnimatedContent(
                targetState = Pair(currentItems, parentTitle),
                transitionSpec = {
                    if (isNavigatingBack) {
                        ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                animationSpec = tween(
                                    durationMillis = MotionConstants.DURATION_ENTER_SHORT,
                                    easing = EmphasizedDecelerate
                                ),
                                initialOffsetX = { -it }
                            ),
                            initialContentExit = slideOutHorizontally(
                                animationSpec = tween(
                                    durationMillis = MotionConstants.DURATION_EXIT_SHORT,
                                    easing = EmphasizedAccelerate
                                ),
                                targetOffsetX = { it }
                            ),
                        )
                    } else {
                        ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                animationSpec = tween(
                                    durationMillis = MotionConstants.DURATION_ENTER_SHORT,
                                    easing = EmphasizedDecelerate
                                ),
                                initialOffsetX = { it }
                            ),
                            initialContentExit = slideOutHorizontally(
                                animationSpec = tween(
                                    durationMillis = MotionConstants.DURATION_EXIT_SHORT,
                                    easing = EmphasizedAccelerate
                                ),
                                targetOffsetX = { -it }
                            ),
                        )
                    }
                },
                label = "SubMenu animation"
            ) { (itemsState, titleState) ->
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .width(IntrinsicSize.Max)
                        .verticalScroll(scrollState)
                ) {
                    titleState?.let { title ->
                        MenuHeader(
                            title = title,
                            onBack = {
                                previousItems?.let { prev ->
                                    isNavigatingBack = true
                                    currentItems = prev
                                    parentTitle = null
                                    previousItems = null
                                }
                            }
                        )
                    }
                    
                    itemsState.forEach { item ->
                        MenuItemRow(
                            item = item,
                            hasParent = titleState != null,
                            onClick = {
                                if (item.subItems != null) {
                                    isNavigatingBack = false
                                    previousItems = currentItems
                                    currentItems = item.subItems
                                    parentTitle = item.title
                                } else {
                                    onItemClick?.invoke(item.onClick ?: {})
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor = if (isPressed) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onBack() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.back),
            contentDescription = "返回",
            modifier = Modifier
                .size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MenuItemRow(
    item: MenuItem,
    hasParent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor = if (isPressed) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f, fill = false)
        )
        
        if (item.subItems != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.next),
                contentDescription = "展开",
                modifier = Modifier
                    .size(14.dp)
            )
        }
    }
}

private data class SimpleDropdownMenuPositionProvider(
    val density: Density,
    val anchorXDp: Float,
    val anchorYDp: Float,
    val onPositionCalculated: (anchorBounds: IntRect, menuBounds: IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val anchorXPx = with(density) { anchorXDp.dp.toPx() }
        val anchorYPx = with(density) { anchorYDp.dp.toPx() }
        
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            (anchorXPx.toInt() - popupContentSize.width).coerceAtLeast(0)
        } else {
            anchorXPx.toInt().coerceAtMost(windowSize.width - popupContentSize.width)
        }
        
        val y = anchorYPx.toInt().coerceAtMost(windowSize.height - popupContentSize.height)
        
        val menuOffset = IntOffset(x, y)
        onPositionCalculated(anchorBounds, IntRect(offset = menuOffset, size = popupContentSize))
        return menuOffset
    }
}
