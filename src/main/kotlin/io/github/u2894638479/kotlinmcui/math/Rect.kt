package io.github.u2894638479.kotlinmcui.math

data class Rect(
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

    inline fun ifEmpty(action: ()-> Unit) = apply { if (isEmpty) action() }

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

    operator fun plusAssign(pos: Position) {
        left += pos.x
        top += pos.y
        right += pos.x
        bottom += pos.y
    }

    operator fun minusAssign(pos: Position) = plusAssign(-pos)

    operator fun timesAssign(times: Double) {
        left *= times
        top *= times
        right *= times
        bottom *= times
    }

    operator fun divAssign(div: Double) {
        left /= div
        top /= div
        right /= div
        bottom /= div
    }

    operator fun plus(pos: Position) = copy().apply { plusAssign(pos) }
    operator fun minus(pos: Position) = copy().apply { minusAssign(pos) }
    operator fun times(times: Double) = copy().apply { timesAssign(times) }
    operator fun div(div: Double) = copy().apply { divAssign(div) }

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

    fun toDouble(): DoubleRect = DoubleRect(
        left.pixelsOrElse { return DoubleRect.empty },
        top.pixelsOrElse { return DoubleRect.empty },
        right.pixelsOrElse { return DoubleRect.empty },
        bottom.pixelsOrElse { return DoubleRect.empty },
    )

    fun toFloat(): FloatRect = FloatRect(
        left.pixelsOrElse { return FloatRect.empty },
        top.pixelsOrElse { return FloatRect.empty },
        right.pixelsOrElse { return FloatRect.empty },
        bottom.pixelsOrElse { return FloatRect.empty },
    )

    fun toInt(): IntRect = IntRect(
        left.pixelsOrElse { return IntRect.empty },
        top.pixelsOrElse { return IntRect.empty },
        right.pixelsOrElse { return IntRect.empty },
        bottom.pixelsOrElse { return IntRect.empty },
    )
}