package com.example.LyricBox.ui.modifier

import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

@OptIn(ExperimentalAnimatableApi::class)
class SpringPlacementModifierNode(
    var lookaheadScope: LookaheadScope,
    var itemKey: Any,
    var isManualScrolling: Boolean,
    var stiffness: Float,
    var forceReset: Long
) : ApproachLayoutModifierNode, Modifier.Node() {
    private var offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)

    private var isFirstFrame = true
    private var lastForceReset = forceReset

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean = false

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        val target = with(lookaheadScope) {
            lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates).round()
        }
        -offsetAnimation.updateTarget(target, coroutineScope, if (isFirstFrame || isManualScrolling) snap() else spring(dampingRatio = 0.95f, stiffness = stiffness))
        return !offsetAnimation.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val coordinates = coordinates
            if (coordinates != null) {
                val target = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates).round()
                }

                val animatedOffset = offsetAnimation.updateTarget(
                    target,
                    coroutineScope,
                    if (isFirstFrame || isManualScrolling) snap() else spring(dampingRatio = 0.95f, stiffness = stiffness)
                )

                isFirstFrame = false

                val placementOffset = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
                }

                val delta = animatedOffset - placementOffset
                placeable.place(delta.x, delta.y)
            } else {
                placeable.place(0, 0)
            }
        }
    }

    fun updateState(newScope: LookaheadScope, newKey: Any, newIsManualScrolling: Boolean, newStiffness: Float, newForceReset: Long) {
        lookaheadScope = newScope
        isManualScrolling = newIsManualScrolling
        stiffness = newStiffness
        if (itemKey != newKey) {
            itemKey = newKey
            offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)
            isFirstFrame = true
        }
        if (lastForceReset != newForceReset) {
            lastForceReset = newForceReset
            offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)
            isFirstFrame = true
        }
    }
}

data class SpringPlacementNodeElement(
    val lookaheadScope: LookaheadScope,
    val itemKey: Any,
    val isManualScrolling: Boolean,
    val stiffness: Float,
    val forceReset: Long
) : ModifierNodeElement<SpringPlacementModifierNode>() {
    override fun update(node: SpringPlacementModifierNode) {
        node.updateState(lookaheadScope, itemKey, isManualScrolling, stiffness, forceReset)
    }
    override fun create(): SpringPlacementModifierNode =
        SpringPlacementModifierNode(lookaheadScope, itemKey, isManualScrolling, stiffness, forceReset)
}

fun Modifier.springPlacement(
    lookaheadScope: LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    stiffness: Float,
    forceReset: Long
) = this.then(SpringPlacementNodeElement(lookaheadScope, itemKey, isManualScrolling, stiffness, forceReset))
