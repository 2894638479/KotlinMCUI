package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * An RGBA color packed into a single [Int].
 *
 * Layout: R at bits 24-31, G at 16-23, B at 8-15, A at 0-7.
 *
 * Supports construction from [UByte], [Int], [Float], [Double], and HSV.
 * Provides channel access at multiple numeric precisions and immutable
 * modification via [change] / [changeHSV].
 */
@Serializable(with = Color.Serializer::class)
@JvmInline
value class Color(val rgbaInt:Int): Interpolatable<Color> {
    constructor(r: UByte, g: UByte, b: UByte, a: UByte = 255u):this(combineUBytes(r,g,b,a))
    constructor(r: Int,g: Int,b: Int,a: Int = 255):this(r.convert,g.convert,b.convert,a.convert)
    constructor(r: Float,g: Float,b: Float,a: Float = 1f):this(r.convert,g.convert,b.convert,a.convert)
    constructor(r: Double,g: Double,b: Double,a: Double = 1.0):this(r.convert,g.convert,b.convert,a.convert)

    inline val rDouble get() = r.toDouble() / 255.0
    inline val gDouble get() = g.toDouble() / 255.0
    inline val bDouble get() = b.toDouble() / 255.0
    inline val aDouble get() = a.toDouble() / 255.0

    inline val rFloat get() = r.toFloat() / 255f
    inline val gFloat get() = g.toFloat() / 255f
    inline val bFloat get() = b.toFloat() / 255f
    inline val aFloat get() = a.toFloat() / 255f

    inline val rInt get() = r.toInt()
    inline val gInt get() = g.toInt()
    inline val bInt get() = b.toInt()
    inline val aInt get() = a.toInt()

    val argbInt get() = combineUBytes(a,r,g,b)
    val abgrInt get() = combineUBytes(a,b,g,r)


    val r get() = rgbaInt.uByte(24)
    val g get() = rgbaInt.uByte(16)
    val b get() = rgbaInt.uByte(8)
    val a get() = rgbaInt.uByte(0)

    val hDouble get() = hFloat.toDouble()
    val sDouble: Double get() {
        val rd = rDouble
        val gd = gDouble
        val bd = bDouble
        val max = max(rd,max(gd,bd))
        if(max == 0.0) return 0.0
        val min = min(rd,min(gd,bd))
        return (max - min) / max
    }
    val vDouble get() = max(rDouble,max(gDouble,bDouble))

    val hFloat: Float get() {
        val rf = rFloat
        val gf = gFloat
        val bf = bFloat

        val max = maxOf(rf, maxOf(gf, bf))
        val min = minOf(rf, minOf(gf, bf))
        val delta = max - min

        var h = when {
            delta == 0f -> 0f
            max == rf -> 60f * (((gf - bf) / delta) % 6f)
            max == gf -> 60f * (((bf - rf) / delta) + 2f)
            else -> 60f * (((rf - gf) / delta) + 4f)
        }
        if (h < 0) h += 360f
        return h / 360
    }
    val sFloat get() = sDouble.toFloat()
    val vFloat get() = vDouble.toFloat()

    val h get() = hFloat
    val s get() = sFloat
    val v get() = vFloat


    fun change(r: UByte = this.r,g: UByte = this.g,b:UByte = this.b,a:UByte = this.a) = Color(r,g,b,a)
    fun change(r: Int = this.rInt,g: Int = this.gInt,b:Int = this.bInt,a:Int = this.aInt) = Color(r,g,b,a)
    fun change(r: Float = this.rFloat,g: Float = this.gFloat,b:Float = this.bFloat,a:Float = this.aFloat) = Color(r,g,b,a)
    fun change(r: Double = this.rDouble,g: Double = this.gDouble,b:Double = this.bDouble,a:Double = this.aDouble) = Color(r,g,b,a)
    fun changeHSV(h: Float = this.hFloat,s: Float = this.sFloat,v: Float = this.vFloat,a: Float = this.aFloat) = ofHSV(h,s,v,a)
    fun changeHSV(h: Double = this.hDouble,s: Double = this.sDouble,v: Double = this.vDouble,a: Double = this.aDouble) = ofHSV(h,s,v,a)

    override fun plus(other: Color) = Color(rInt+other.rInt,gInt+other.gInt,bInt+other.bInt,aInt+other.aInt)
    override fun minus(other: Color) = Color(rInt-other.rInt,gInt-other.gInt,bInt-other.bInt,aInt-other.aInt)
    override fun times(other: Double) = Color(rDouble*other,gDouble*other,bDouble*other,aDouble*other)
    override fun div(other: Double) = Color(rDouble/other,gDouble/other,bDouble/other,aDouble/other)

    /**
     * Mix [this] colour with [other] by integer channel-wise multiplication:
     * each byte is `(a * b) / 255`, so `WHITE.mixedWith(c) == c`.
     */
    fun mixedWith(other: Color) = Color(rInt*other.rInt/255,gInt*other.gInt/255,bInt*other.bInt/255,aInt*other.aInt/255)

    /**
     * Channel-wise screen (lighten): `255 - (255-a)(255-b)/255`, applied to
     * alpha too, so opacity unions where [mixedWith] intersects it.
     */
    fun screenWith(other: Color) = Color(
        255-(255-rInt)*(255-other.rInt)/255, 255-(255-gInt)*(255-other.gInt)/255,
        255-(255-bInt)*(255-other.bInt)/255, 255-(255-aInt)*(255-other.aInt)/255,
    )

    /** Channel-wise average: `(a + b) / 2`. */
    fun averageWith(other: Color) = Color((rInt+other.rInt)/2,(gInt+other.gInt)/2,(bInt+other.bInt)/2,(aInt+other.aInt)/2)

    /**
     * Alpha-composite [this] (source) over [other] (destination), channel-wise
     * and un-premultiplied. Two fully transparent colours stay transparent.
     */
    fun over(other: Color): Color {
        val srcA = aInt
        val dstA = other.aInt
        val outA = srcA + dstA * (255 - srcA) / 255
        if (outA == 0) return TRANSPARENT_BLACK
        fun channel(src: Int, dst: Int) = (src * srcA + dst * dstA * (255 - srcA) / 255) / outA
        return Color(channel(rInt,other.rInt), channel(gInt,other.gInt), channel(bInt,other.bInt), outA)
    }


    val hexStringARGB get() = argbInt.toUInt().toString(16).padStart(8, '0')

    override fun toString() = "Color r:$r, g:$g, b:$b, a:$a"

    companion object {
        private fun combineUBytes(b1: UByte,b2: UByte,b3: UByte,b4: UByte):Int {
            return (b1.toInt() shl 24) or (b2.toInt() shl 16) or (b3.toInt() shl 8) or b4.toInt()
        }
        private fun Int.uByte(offset:Int) = ((this shr offset) and 0xFF).toUByte()
        private inline val Int.convert get() = coerceIn(0,255).toUByte()
        private inline val Float.convert get() = (this * 255).toInt().convert
        private inline val Double.convert get() = (this * 255).toInt().convert

        fun ofRGBA(value: Int) = Color(value)
        fun ofARGB(value: Int) = Color(value.uByte(16),value.uByte(8),value.uByte(0),value.uByte(24))
        fun ofABGR(value: Int) = Color(value.uByte(0),value.uByte(8),value.uByte(16),value.uByte(24))
        fun ofHSV(h: Double,s: Double,v: Double,a: Double = 1.0) = ofHSV(h.toFloat(),s.toFloat(),v.toFloat(),a.toFloat())
        fun ofHSV(h: Float,s: Float,v: Float,a: Float = 1f): Color {
            val h = h.mod(1f) * 360f
            val s = s.coerceIn(0f,1f)
            val v = v.coerceIn(0f,1f)
            val c = v * s
            val x = c * (1f - abs((h / 60f) % 2f - 1f))
            val m = v - c

            val (rf, gf, bf) = when {
                h < 60 -> Triple(c, x, 0f)
                h < 120 -> Triple(x, c, 0f)
                h < 180 -> Triple(0f, c, x)
                h < 240 -> Triple(0f, x, c)
                h < 300 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return Color(rf + m,gf + m,bf + m,a)
        }

        val WHITE = Color(255u,255u,255u)
        val BLACK = Color(0u,0u,0u)
        val RED = Color(255u,0u,0u)
        val GREEN = Color(0u,255u,0u)
        val BLUE = Color(0u,0u,255u)
        val TRANSPARENT_WHITE = WHITE.change(a = 0)
        val TRANSPARENT_BLACK = BLACK.change(a = 0)
    }

    object Serializer : KSerializer<Color> {
        override val descriptor = PrimitiveSerialDescriptor("color", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: Color) = encoder.encodeInt(value.rgbaInt)
        override fun deserialize(decoder: Decoder) = ofRGBA(decoder.decodeInt())
    }
}