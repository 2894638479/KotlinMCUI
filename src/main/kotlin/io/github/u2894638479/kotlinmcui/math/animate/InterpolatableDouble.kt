package io.github.u2894638479.kotlinmcui.math.animate

@JvmInline
value class InterpolatableDouble(private val value: Double): Interpolatable<InterpolatableDouble> {
    override fun plus(other: InterpolatableDouble) = InterpolatableDouble(value + other.value)
    override fun minus(other: InterpolatableDouble) = InterpolatableDouble(value - other.value)
    override fun times(other: Double) = InterpolatableDouble(value * other)
    override fun div(other: Double) = InterpolatableDouble(value / other)
    fun toDouble() = value
}

fun Double.toInterpolatable() = InterpolatableDouble(this)