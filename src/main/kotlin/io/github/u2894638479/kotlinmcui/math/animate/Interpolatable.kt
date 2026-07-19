package io.github.u2894638479.kotlinmcui.math.animate

/**
 * A type that supports linear interpolation between two values.
 *
 * Implement the four arithmetic operators and use with [Interpolator.interpolate]
 * to animate between values.
 */
interface Interpolatable<T> {
    operator fun plus(other: T): T
    operator fun minus(other: T): T
    operator fun times(other: Double): T
    operator fun div(other: Double): T
}