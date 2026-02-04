package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.dslLogger
import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.*
import kotlin.collections.sumOf

@JvmInline
value class Measure private constructor(val raw: Double):Comparable<Measure>, Interpolatable<Measure> {
    companion object {
        val AUTO = Measure(Double.fromBits(0x7FF1000000000000L))
        val AUTO_MIN = Measure(Double.fromBits(0x7FF2000000000000L))
        fun ofRaw(raw: Double) = Measure(raw)
        fun max(a: Measure,b: Measure) = Measure(max(a.raw,b.raw))
        fun min(a: Measure,b: Measure) = Measure(min(a.raw,b.raw))
    }
    inline val bits get() = raw.toRawBits()
    inline val isAutoMin get() = bits == AUTO_MIN.bits
    inline val isQNaN get() = (bits and 0x7FF8000000000000L) == 0x7FF8000000000000L
    inline val isFinite get() = raw.isFinite()
    inline val isNumber get() = !raw.isNaN()
    inline val absoluteValue get() = raw.absoluteValue.px

    inline fun ifNan(default:()-> Measure) = if(isNumber) this else default()

    inline fun <reified T: Number?> pixelsOrElse(defaultValue:()->T):T {
        if(!isNumber) return defaultValue()
        return when(T::class) {
            Double::class -> raw
            Float::class -> raw.toFloat()
            Int::class -> raw.toInt()
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

    private inline fun ifNum(value:()-> Measure): Measure {
        if(!isNumber) return this
        return value()
    }

    override operator fun plus(other: Measure) = ifNum { other.ifNum { Measure(raw + other.raw) } }
    override operator fun minus(other: Measure) = ifNum { other.ifNum { Measure(raw - other.raw) } }

    override operator fun times(other: Double) = if(isNumber) Measure(raw * other) else this
    operator fun times(other: Float) = times(other.toDouble())
    operator fun times(other: Int) = times(other.toDouble())

    operator fun div(other: Measure) = raw / other.raw
    override operator fun div(other: Double) = if(isNumber) Measure(raw / other) else this
    operator fun div(other: Float) = div(other.toDouble())
    operator fun div(other: Int) = div(other.toDouble())

    operator fun unaryMinus() = ifNum { Measure(-raw) }
    override operator fun compareTo(other: Measure) = raw.compareTo(other.raw)
}

val Double.px get() = Measure.ofRaw(this)
val Float.px get() = Measure.ofRaw(toDouble())
val Int.px get() = Measure.ofRaw(toDouble())

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("sumOfMeasure")
inline fun <T> Iterable<T>.sumOf(selector: (T) -> Measure): Measure = sumOf { selector(it).raw }.px
