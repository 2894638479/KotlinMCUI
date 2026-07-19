package io.github.u2894638479.kotlinmcui.math.animate

import kotlin.math.*

/**
 * An easing function that maps linear progress `t` (0..1) to eased progress.
 *
 * Pre-built instances are available in the [companion][Companion] object.
 * Create custom interpolators with a lambda:
 * ```
 * val myEasing = Interpolator { t -> t * t }
 * val result = myEasing.interpolate(start, end, fraction)
 * ```
 */
fun interface Interpolator {
    /** Map linear progress to eased progress. */
    fun progress(t: Double): Double
    companion object {
        val linear = Interpolator { it }
        val sqrt = Interpolator(::sqrt)
        fun power(n:Int) = Interpolator { 1 - (1 - it).pow(n) }
        val square = power(2)
        val cubed = power(3)
        fun exp(range: Double) = Interpolator {
            val min = kotlin.math.exp(-range)
            val scale = 1 / (1 - min)
            (1 - kotlin.math.exp(it * -range)) * scale
        }
        val default = square
    }
}

fun Interpolator.interpolate(begin: Double,end: Double,t: Double): Double{
    val progress = progress(t)
    return begin * (1 - progress) + end * progress
}

fun <T: Interpolatable<T>> Interpolator.interpolate(begin: T, end: T, t: Double) :T {
    val progress = progress(t)
    return begin * (1 - progress) + end * progress
}