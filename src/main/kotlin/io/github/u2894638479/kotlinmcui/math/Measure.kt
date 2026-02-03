package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.dslLogger
import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import kotlin.math.*

@JvmInline
value class Measure private constructor(val raw: Double):Comparable<Measure>, Interpolatable<Measure> {
    companion object {
        val AUTO = Measure(Double.fromBits(0x7FF1000000000000L))
        fun ofRaw(raw: Double) = Measure(raw)
        fun max(a: Measure,b: Measure) = Measure(max(a.raw,b.raw))
        fun min(a: Measure,b: Measure) = Measure(min(a.raw,b.raw))
    }
    inline val bits get() = this@Measure.raw.toRawBits()
    inline val isAuto get() = bits == AUTO.bits
    inline val isQNaN get() = (bits and 0x7FF8000000000000L) == 0x7FF8000000000000L
    inline val isFinite get() = this@Measure.raw.isFinite()
    inline val isNumber get() = !this@Measure.raw.isNaN()
    inline val absoluteValue get() = raw.absoluteValue.px

    inline fun ifNan(default:()-> Measure) = if(isNumber) this else default()

    inline fun <reified T: Number?> pixelsOrElse(defaultValue:()->T):T {
        if(!isNumber) return defaultValue()
        return when(T::class) {
            Double::class -> this@Measure.raw
            Float::class -> this@Measure.raw.toFloat()
            Int::class -> this@Measure.raw.toInt()
            else -> error("unsupported type: ${T::class}")
        } as T
    }

    inline fun <reified T: Number> pixelsOrWarn(ret:()-> Nothing):T {
        return pixelsOrElse {
            dslLogger.warn(Exception("warning: using NaN value of Measure").stackTraceToString())
            ret()
        }
    }

    inline fun <reified T: Number> pixels() = pixelsOrElse { null }

    fun requireNonNaN() = ifNan { error("required a number measure but has NaN value") }


    override operator fun plus(other: Measure) = Measure(this@Measure.raw + other.raw)
    override operator fun minus(other: Measure) = Measure(this@Measure.raw - other.raw)

    override operator fun times(other: Double) = Measure(this@Measure.raw * other)
    operator fun times(other: Float) = Measure(this@Measure.raw * other.toDouble())
    operator fun times(other: Int) = Measure(this@Measure.raw * other.toDouble())

    operator fun div(other: Measure) = this@Measure.raw / other.raw
    override operator fun div(other: Double) = Measure(this@Measure.raw / other)
    operator fun div(other: Float) = Measure(this@Measure.raw / other.toDouble())
    operator fun div(other: Int) = Measure(this@Measure.raw / other.toDouble())

    operator fun unaryMinus() = Measure(-this@Measure.raw)
    override operator fun compareTo(other: Measure) = this@Measure.raw.compareTo(other.raw)
}

val Double.px get() = Measure.ofRaw(this)
val Float.px get() = Measure.ofRaw(toDouble())
val Int.px get() = Measure.ofRaw(toDouble())
