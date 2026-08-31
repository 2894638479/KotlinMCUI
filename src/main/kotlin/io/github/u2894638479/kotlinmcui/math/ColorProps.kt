package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.prop.StableRO
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.value

sealed interface ColorProp<T> {
    fun get(color: Color): T
    fun set(color: Color, value: T): Color
}

object R: ColorProp<UByte> {
    override fun get(color: Color) = color.r
    override fun set(color: Color, value: UByte) = color.change(r = value)
}

object G: ColorProp<UByte> {
    override fun get(color: Color) = color.g
    override fun set(color: Color, value: UByte) = color.change(g = value)
}

object B: ColorProp<UByte> {
    override fun get(color: Color) = color.b
    override fun set(color: Color, value: UByte) = color.change(b = value)
}

object A: ColorProp<UByte> {
    override fun get(color: Color) = color.a
    override fun set(color: Color, value: UByte) = color.change(a = value)
}

// ── Int channels ─────────────────────────────────────────────────────────────

object RInt: ColorProp<Int> {
    override fun get(color: Color) = color.rInt
    override fun set(color: Color, value: Int) = color.change(r = value)
}
object GInt: ColorProp<Int> {
    override fun get(color: Color) = color.gInt
    override fun set(color: Color, value: Int) = color.change(g = value)
}
object BInt: ColorProp<Int> {
    override fun get(color: Color) = color.bInt
    override fun set(color: Color, value: Int) = color.change(b = value)
}
object AInt: ColorProp<Int> {
    override fun get(color: Color) = color.aInt
    override fun set(color: Color, value: Int) = color.change(a = value)
}

// ── Float channels ───────────────────────────────────────────────────────────

object RFloat: ColorProp<Float> {
    override fun get(color: Color) = color.rFloat
    override fun set(color: Color, value: Float) = color.change(r = value)
}
object GFloat: ColorProp<Float> {
    override fun get(color: Color) = color.gFloat
    override fun set(color: Color, value: Float) = color.change(g = value)
}
object BFloat: ColorProp<Float> {
    override fun get(color: Color) = color.bFloat
    override fun set(color: Color, value: Float) = color.change(b = value)
}
object AFloat: ColorProp<Float> {
    override fun get(color: Color) = color.aFloat
    override fun set(color: Color, value: Float) = color.change(a = value)
}

// ── Double channels ──────────────────────────────────────────────────────────

object RDouble: ColorProp<Double> {
    override fun get(color: Color) = color.rDouble
    override fun set(color: Color, value: Double) = color.change(r = value)
}
object GDouble: ColorProp<Double> {
    override fun get(color: Color) = color.gDouble
    override fun set(color: Color, value: Double) = color.change(g = value)
}
object BDouble: ColorProp<Double> {
    override fun get(color: Color) = color.bDouble
    override fun set(color: Color, value: Double) = color.change(b = value)
}
object ADouble: ColorProp<Double> {
    override fun get(color: Color) = color.aDouble
    override fun set(color: Color, value: Double) = color.change(a = value)
}

// ── HSV channels ─────────────────────────────────────────────────────────────

object H: ColorProp<Float> {
    override fun get(color: Color) = color.h
    override fun set(color: Color, value: Float) = color.changeHSV(h = value)
}
object S: ColorProp<Float> {
    override fun get(color: Color) = color.s
    override fun set(color: Color, value: Float) = color.changeHSV(s = value)
}
object V: ColorProp<Float> {
    override fun get(color: Color) = color.v
    override fun set(color: Color, value: Float) = color.changeHSV(v = value)
}

object HDouble: ColorProp<Double> {
    override fun get(color: Color) = color.hDouble
    override fun set(color: Color, value: Double) = color.changeHSV(h = value)
}
object SDouble: ColorProp<Double> {
    override fun get(color: Color) = color.sDouble
    override fun set(color: Color, value: Double) = color.changeHSV(s = value)
}
object VDouble: ColorProp<Double> {
    override fun get(color: Color) = color.vDouble
    override fun set(color: Color, value: Double) = color.changeHSV(v = value)
}

// ── Packed representations ───────────────────────────────────────────────────

object RGBAInt: ColorProp<Int> {
    override fun get(color: Color) = color.rgbaInt
    override fun set(color: Color, value: Int) = Color.ofRGBA(value)
}
object ARGBInt: ColorProp<Int> {
    override fun get(color: Color) = color.argbInt
    override fun set(color: Color, value: Int) = Color.ofARGB(value)
}
object ABGRInt: ColorProp<Int> {
    override fun get(color: Color) = color.abgrInt
    override fun set(color: Color, value: Int) = Color.ofABGR(value)
}

// ── Operators ────────────────────────────────────────────────────────────────

operator fun <T> StableRO<Color>.get(prop: ColorProp<T>) = prop.get(value)
operator fun <T> Color.get(prop: ColorProp<T>) = prop.get(this)
operator fun <T> StableRW<Color>.set(prop: ColorProp<T>, value: T) {
    this.value = prop.set(this.value, value)
}

// ── Keyboard stepping ─────────────────────────────────────────────────────────

/*
 * Keyboard stepping is computed exclusively from the RGBA8888 uint8 channels —
 * no floating point anywhere — and always lands on the next closest value that
 * is actually different. S = (max-min)/max and V = max depend only on the max
 * and min bytes, so:
 *
 *  R / G / B / A  the byte itself steps by ±1 (clamped at 0/255).
 *  V  (max byte)  every byte at the maximum steps by ±1 together, so a gray
 *                 stays gray and the hue is left intact.
 *  S  (min byte)  every byte at the minimum steps by ∓1 (up lowers the min,
 *                 down raises it), so V stays fixed; an achromatic colour
 *                 breaks its tie on the red byte to get the smallest
 *                 saturation above 0.
 *  H  the middle byte moves ±1 while both max and min stay put (tied max/min
 *     bytes rotate at sector boundaries), so S and V are preserved exactly;
 *     the neighbour with the closest different hue in the stepped direction
 *     around the circular hue wheel is chosen — stepping wraps past 360°/0°.
 *     Achromatic colours have no hue and stay put.
 */

/** Move one keyboard step up along this channel. */
fun ColorProp<Double>.stepUp(color: Color): Color = step(color, +1)

/** Move one keyboard step down along this channel. */
fun ColorProp<Double>.stepDown(color: Color): Color = step(color, -1)

private fun ColorProp<Double>.step(color: Color, dir: Int): Color = when (this) {
    RDouble -> color.change(r = color.rInt + dir)
    GDouble -> color.change(g = color.gInt + dir)
    BDouble -> color.change(b = color.bInt + dir)
    ADouble -> color.change(a = color.aInt + dir)
    VDouble -> color.stepValue(dir)
    SDouble -> color.stepSaturation(dir)
    HDouble -> color.stepHue(dir)
    else -> color
}

/** Value = max byte: move every channel sitting at the maximum by [dir]. */
private fun Color.stepValue(dir: Int): Color {
    val max = maxOf(rInt, gInt, bInt)
    return change(
        r = if (rInt == max) rInt + dir else rInt,
        g = if (gInt == max) gInt + dir else gInt,
        b = if (bInt == max) bInt + dir else bInt,
    )
}

/**
 * Saturation = (max - min) / max: move every channel sitting at the minimum by
 * -[dir], keeping the maximum (value) fixed.
 */
private fun Color.stepSaturation(dir: Int): Color {
    val max = maxOf(rInt, gInt, bInt)
    val min = minOf(rInt, gInt, bInt)
    if (max == 0) return this                                  // black: no saturation
    if (min == max) return if (dir > 0) change(r = min - 1) else this  // gray: s == 0 already
    val newMin = min - dir                                     // up lowers min, down raises it
    return change(
        r = if (rInt == min) newMin else rInt,
        g = if (gInt == min) newMin else gInt,
        b = if (bInt == min) newMin else bInt,
    )
}

/**
 * Odd-of-six hue of an RGB888 colour, as an exact integer numerator over the
 * implied denominator (max - min): hue = 60° × n / (max - min) ∈ [0, 6),
 * so the full circle is 6·(max - min). Achromatic colours have no hue → 0.
 */
private fun hueNumerator(r: Int, g: Int, b: Int): Long {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = (max - min).toLong()
    if (delta == 0L) return 0L
    return when (max) {
        r -> (g - b).toLong().let { if (it >= 0) it else 6 * delta + it }
        g -> 2 * delta + (b - r).toLong()
        else -> 4 * delta + (r - g).toLong()
    }
}

/**
 * Hue: rotate while keeping S and V exactly fixed. Both depend only on the
 * max/min bytes, so only single-byte moves that leave them untouched are
 * candidates. Such moves share the same hue denominator (max - min), so the
 * closest different hue in the stepped direction is a plain integer
 * comparison of hue numerators; the wheel wraps at 360°. Achromatic colours
 * have no hue and stay put.
 */
private fun Color.stepHue(dir: Int): Color {
    val max = maxOf(rInt, gInt, bInt)
    val min = minOf(rInt, gInt, bInt)
    val delta = max - min
    if (delta == 0) return this                               // gray: hue is degenerate
    val clamp = 6L * delta
    val start = hueNumerator(rInt, gInt, bInt)
    var best: IntArray? = null
    var bestDist = 0L
    for (i in 0..2) for (d in -1..1 step 2) {
        val next = intArrayOf(rInt, gInt, bInt)
        next[i] += d
        if (next[i] !in 0..255 || next.max() != max || next.min() != min) continue
        val cand = hueNumerator(next[0], next[1], next[2])
        var dist = (cand - start + clamp) % clamp             // hue distance forward
        if (dist == 0L) continue                              // same hue: not different
        if (dir < 0) dist = clamp - dist                      // distance backward
        if (best == null || dist < bestDist) { best = next; bestDist = dist }
    }
    return best?.let { Color(it[0], it[1], it[2], aInt) } ?: this
}
