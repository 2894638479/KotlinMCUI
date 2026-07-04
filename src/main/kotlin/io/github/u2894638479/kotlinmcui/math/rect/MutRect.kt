package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

interface MutRect: Rect {
    override var left: Measure
    override var top: Measure
    override var right: Measure
    override var bottom: Measure
}

fun MutRect(
    left: Measure = 0.px,
    top: Measure = 0.px,
    right: Measure = 0.px,
    bottom: Measure = 0.px,
): MutRect = RectImpl(left,top,right,bottom)

fun MutRect(horizontal: MutBound,vertical: MutBound) = object:MutRect {
    override var left by horizontal::low
    override var top by vertical::low
    override var right by horizontal::high
    override var bottom by vertical::high
}

operator fun MutRect.get(axis: Axis) = bound(axis)
operator fun MutRect.set(axis: Axis, value: Bound) = this[axis] copyFrom value

fun MutRect.bound(axis: Axis) = when(axis) {
    Axis.Horizontal -> object : MutBound {
        override var low by ::left
        override var high by ::right
    }
    Axis.Vertical -> object : MutBound {
        override var low by ::top
        override var high by ::bottom
    }
}

operator fun MutRect.times(time: Double) = object: Rect {
    override var left get() = this@times.left * time
        set(value) { this@times.left = value / time }
    override var top get() = this@times.top * time
        set(value) { this@times.top = value / time }
    override var right get() = this@times.right * time
        set(value) { this@times.right = value / time }
    override var bottom get() = this@times.bottom * time
        set(value) { this@times.bottom = value / time }
}

operator fun MutRect.div(div: Double) = times(1/div)

fun MutRect.expand(expand: Measure) = object: Rect {
    override var left get() = this@expand.left - expand
        set(value) { this@expand.left = value + expand }
    override var top get() = this@expand.top - expand
        set(value) { this@expand.top = value + expand }
    override var right get() = this@expand.right + expand
        set(value) { this@expand.right = value - expand }
    override var bottom get() = this@expand.bottom + expand
        set(value) { this@expand.bottom = value - expand }
}

infix fun MutRect.copyFrom(other: Rect) {
    left = other.left
    top = other.top
    right = other.right
    bottom = other.bottom
}
