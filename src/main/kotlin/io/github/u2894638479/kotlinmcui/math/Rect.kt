package io.github.u2894638479.kotlinmcui.math

class Rect(
    var left: Measure = 0.px,
    var top: Measure = 0.px,
    var right: Measure = 0.px,
    var bottom: Measure = 0.px,
) {

    inline var width get() = right - left
        set(value) { right = left + value }
    inline var height get() = bottom - top
        set(value) { bottom = top + value }

    val isEmpty get() = width <= 0.px || height <= 0.px
    val xRange get() = left..right
    val yRange get() = top..bottom

    fun copyFrom(other: Rect) {
        left = other.left
        top = other.top
        right = other.right
        bottom = other.bottom
    }

    override fun equals(other: Any?) = other is Rect
            && left == other.left
            && top == other.top
            && right == other.right
            && bottom == other.bottom

    fun contains(x: Measure, y: Measure) = x >= left && x <= right && y >= top && y <= bottom
    operator fun contains(pos: Position) = contains(pos.x,pos.y)

    operator fun contains(other: Rect) = other.left >= left && other.right <= right && other.top >= top && other.bottom <= bottom
    fun overlap(other: Rect): Boolean {
        if(right < other.left || left > other.right) return false
        if(bottom < other.top || top > other.bottom) return false
        return true
    }

    fun expand(value: Measure) = Rect(left - value,top - value,right + value,bottom + value)
    fun contract(value: Measure) = expand(-value)

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        return result
    }
}