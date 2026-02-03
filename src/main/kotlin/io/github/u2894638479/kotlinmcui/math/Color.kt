package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable


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

    fun change(r: UByte = this.r,g: UByte = this.g,b:UByte = this.b,a:UByte = this.a) = Color(r,g,b,a)
    fun change(r: Int = this.rInt,g: Int = this.gInt,b:Int = this.bInt,a:Int = this.aInt) = Color(r,g,b,a)
    fun change(r: Float = this.rFloat,g: Float = this.gFloat,b:Float = this.bFloat,a:Float = this.aFloat) = Color(r,g,b,a)
    fun change(r: Double = this.rDouble,g: Double = this.gDouble,b:Double = this.bDouble,a:Double = this.aDouble) = Color(r,g,b,a)

    override fun plus(other: Color) = Color(rInt+other.rInt,gInt+other.gInt,bInt+other.bInt,aInt+other.aInt)
    override fun minus(other: Color) = Color(rInt-other.rInt,gInt-other.gInt,bInt-other.bInt,aInt-other.aInt)
    override fun times(other: Double) = Color(rDouble*other,gDouble*other,bDouble*other,aDouble*other)
    override fun div(other: Double) = Color(rDouble/other,gDouble/other,bDouble/other,aDouble/other)


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

        fun ofRGBA(value:Int) = Color(value)
        fun ofARGB(value: Int) = Color(value.uByte(16),value.uByte(8),value.uByte(0),value.uByte(24))
        fun ofABGR(value: Int) = Color(value.uByte(0),value.uByte(8),value.uByte(16),value.uByte(24))

        val WHITE = Color(255u,255u,255u)
        val BLACK = Color(0u,0u,0u)
        val RED = Color(255u,0u,0u)
        val GREEN = Color(0u,255u,0u)
        val BLUE = Color(0u,0u,255u)
        val TRANSPARENT_WHITE = WHITE.change(a = 0)
        val TRANSPARENT_BLACK = BLACK.change(a = 0)
    }
}