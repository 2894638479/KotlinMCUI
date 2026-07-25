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
