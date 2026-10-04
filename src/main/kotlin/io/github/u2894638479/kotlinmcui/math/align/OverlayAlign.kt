package io.github.u2894638479.kotlinmcui.math.align

import io.github.u2894638479.kotlinmcui.component.DslComponentAlign
import io.github.u2894638479.kotlinmcui.component.alignable
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.*
import io.github.u2894638479.kotlinmcui.modifier.Modifier

/**
 * Keep a tip of [size] inside this bound.
 *
 * The placement strategies below prefer one side of their anchor and fall back to the
 * other; this makes sure the fallback still cannot leave the keep-in bound, which is
 * what "stays on screen" promises.
 */
private fun Bound.clamp(position: Measure, size: Measure): Measure {
    val high = max(low, this.high - size)
    return when {
        position < low -> low
        position > high -> high
        else -> position
    }
}

interface OverlayAlign {
    fun layoutAxis(instance: DslComponentAlign, modifier: Modifier, axis: Axis)
    class FixRect(val rect: Rect): OverlayAlign {
        override fun layoutAxis(instance: DslComponentAlign, modifier: Modifier, axis: Axis) {
            instance.alignable(axis) copyFrom rect[axis]
        }
    }
    class InsideRect(val rect: Rect): OverlayAlign {
        override fun layoutAxis(instance: DslComponentAlign, modifier: Modifier, axis: Axis) {
            val alignable = instance.alignable(axis)
            alignable copyFrom rect[axis].alignIn(alignable.align,alignable.autoSizeMin)
        }
    }
    class OutsideRect(val rect: Rect): OverlayAlign {
        override fun layoutAxis(instance: DslComponentAlign, modifier: Modifier, axis: Axis) {
            val alignable = instance.alignable(axis)
            alignable copyFrom rect[axis].alignOut(alignable.align,alignable.autoSizeMin)
        }
    }
    class ToPoint(val point: Position): OverlayAlign {
        override fun layoutAxis(instance: DslComponentAlign, modifier: Modifier, axis: Axis) {
            val alignable = instance.alignable(axis)
            alignable copyFrom Bound(alignable.align, point[axis], alignable.autoSizeMin)
        }
    }
    class AutoToPoint(val point: Position, val keepin: Rect): OverlayAlign {
        override fun layoutAxis(instance: DslComponentAlign, modifier: Modifier, axis: Axis) {
            val alignable = instance.alignable(axis)
            val size = alignable.autoSizeMin
            val keepin = keepin[axis]
            val pos = point[axis]
            val low = when(axis) {
                Axis.Horizontal -> when {
                    keepin.high - pos >= size -> pos
                    keepin.low + size <= pos -> pos - size
                    else -> keepin.high - size
                }
                Axis.Vertical -> when {
                    pos - keepin.low >= size -> pos - size
                    keepin.high - pos >= size -> pos
                    else -> keepin.high - size
                }
            }
            alignable.low = keepin.clamp(low, size)
            alignable.high = alignable.low + size
        }
    }
    class AutoToRect(val rect: Rect, val keepin: Rect): OverlayAlign {
        override fun layoutAxis(instance: DslComponentAlign, modifier: Modifier, axis: Axis) {
            val alignable = instance.alignable(axis)
            val size = alignable.autoSizeMin
            val anchor = rect[axis]
            val keepin = keepin[axis]
            val low = when(axis) {
                Axis.Horizontal -> if(anchor.low + size > keepin.high) keepin.high - size else anchor.low
                Axis.Vertical -> if(anchor.high + size > keepin.high) anchor.low - size else anchor.high
            }
            val clamped = keepin.clamp(low, size)
            alignable.low = clamped
            alignable.high = clamped + size
        }
    }
}

