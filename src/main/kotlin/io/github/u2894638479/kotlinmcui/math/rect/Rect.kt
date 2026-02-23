package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px


interface Rect {
    val left: Measure
    val top: Measure
    val right: Measure
    val bottom: Measure
    companion object {
        val empty = object : Rect {
            override val left get() = 0.px
            override val top get() = 0.px
            override val right get() = 0.px
            override val bottom get() = 0.px
        }
    }
}

fun Rect(
    left: Measure = 0.px,
    top: Measure = 0.px,
    right: Measure = 0.px,
    bottom: Measure = 0.px,
):Rect = RectImpl(left,top,right,bottom)

fun Rect(horizontal: Bound,vertical: Bound) = object: Rect {
    override val left by horizontal::low
    override val top by vertical::low
    override val right by horizontal::high
    override val bottom by vertical::high
}

inline val Rect.width get() = right - left
inline val Rect.height get() = bottom - top
inline val Rect.isEmpty get() = width <= 0.px || height <= 0.px
inline fun Rect.ifEmpty(action: ()-> Unit) = apply { if (isEmpty) action() }

fun Rect.bound(axis: Axis) = when(axis) {
    Axis.Horizontal -> object : Bound {
        override val low get() = left
        override val high get() = right
    }
    Axis.Vertical -> object : Bound {
        override val low get() = top
        override val high get() = bottom
    }
}

fun Rect.contains(x: Measure, y: Measure) = x >= left && x <= right && y >= top && y <= bottom
operator fun Rect.contains(pos: Position) = contains(pos.x,pos.y)
operator fun Rect.contains(other: Rect) =
    other.left >= left && other.right <= right && other.top >= top && other.bottom <= bottom

operator fun Rect.times(time: Double) = object: Rect {
    override val left get() = this@times.left * time
    override val top get() = this@times.top * time
    override val right get() = this@times.right * time
    override val bottom get() = this@times.bottom * time
}

operator fun Rect.div(div: Double) = times(1/div)
fun Rect.expand(expand: Measure) = object: Rect {
    override val left get() = this@expand.left - expand
    override val top get() = this@expand.top - expand
    override val right get() = this@expand.right + expand
    override val bottom get() = this@expand.bottom + expand
}

fun Rect.overlap(other: Rect): Boolean {
    if(right < other.left || left > other.right) return false
    if(bottom < other.top || top > other.bottom) return false
    return true
}


data class DoubleRect(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    inline val width get() = right - left
    inline val height get() = bottom - top
    inline val isEmpty get() = width <= 0 || height <= 0
    inline fun ifEmpty(action: ()-> Unit) = apply { if (isEmpty) action() }
    companion object {
        val empty = DoubleRect(0.0,0.0,0.0,0.0)
    }
}

data class FloatRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    inline val width get() = right - left
    inline val height get() = bottom - top
    inline val isEmpty get() = width <= 0 || height <= 0
    inline fun ifEmpty(action: ()-> Unit) = apply { if (isEmpty) action() }
    companion object {
        val empty = FloatRect(0f,0f,0f,0f)
    }
}

data class IntRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    inline val width get() = right - left
    inline val height get() = bottom - top
    inline val isEmpty get() = width <= 0 || height <= 0
    inline fun ifEmpty(action: ()-> Unit) = apply { if (isEmpty) action() }
    companion object {
        val empty = IntRect(0,0,0,0)
    }
}

fun Rect.toDouble(): DoubleRect = DoubleRect(
    left.pixelsOrElse { return DoubleRect.empty },
    top.pixelsOrElse { return DoubleRect.empty },
    right.pixelsOrElse { return DoubleRect.empty },
    bottom.pixelsOrElse { return DoubleRect.empty },
)

fun Rect.toFloat(): FloatRect = FloatRect(
    left.pixelsOrElse { return FloatRect.empty },
    top.pixelsOrElse { return FloatRect.empty },
    right.pixelsOrElse { return FloatRect.empty },
    bottom.pixelsOrElse { return FloatRect.empty },
)

fun Rect.toInt(): IntRect = IntRect(
    left.pixelsOrElse { return IntRect.empty },
    top.pixelsOrElse { return IntRect.empty },
    right.pixelsOrElse { return IntRect.empty },
    bottom.pixelsOrElse { return IntRect.empty },
)