package io.github.u2894638479.kotlinmcui.math.animate

interface Interpolatable<T> {
    operator fun plus(other:T):T
    operator fun minus(other:T):T
    operator fun times(other: Double):T
    operator fun div(other: Double):T
}