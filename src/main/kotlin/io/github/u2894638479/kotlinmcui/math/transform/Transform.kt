package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.math.cos
import kotlin.math.sin

data class Transform(
    val m00: Num,
    val m01: Num,
    val m02: Num,
    val m10: Num,
    val m11: Num,
    val m12: Num,
    val m20: Num,
    val m21: Num,
    val m22: Num,
) {
    private typealias Num = Float
    inline val isEmpty get() = this === empty
    companion object {
        val empty = Transform (
            1f,0f,0f,
            0f,1f,0f,
            0f,0f,1f
        )

        private val useFma = try {
            Math::class.java.getDeclaredMethod("fma", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
            Math::class.java.getDeclaredMethod("fma", Double::class.javaPrimitiveType, Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
            true
        } catch (_: NoSuchMethodException) { false }

        private fun fma(a: Float,b: Float,c: Float): Float = if(useFma) Math.fma(a,b,c) else a * b + c
        private fun fma(a: Double,b: Double,c: Double): Double = if(useFma) Math.fma(a,b,c) else a * b + c

        fun translate(x: Measure,y: Measure) = Transform(
            1f,0f,x.raw.toFloat(),
            0f,1f,y.raw.toFloat(),
            0f,0f,1f
        )

        fun translate(pos: Position) = translate(pos.x,pos.y)

        fun scale(centerX: Measure,centerY: Measure,x: Double,y: Double) = Transform(
            x.toFloat(),0f,fma(centerX.raw,-x,centerX.raw).toFloat(),
            0f,y.toFloat(),fma(centerY.raw,-y,centerY.raw).toFloat(),
            0f,0f,1f
        )

        fun scale(center: Position,x: Double,y: Double) = scale(center.x,center.y,x,y)

        fun rotate(centerX: Measure,centerY: Measure,rad: Double): Transform {
            val cos = cos(rad)
            val sin = sin(rad)
            val cosF = cos.toFloat()
            val sinF = sin.toFloat()
            val x = centerX.raw
            val y = centerY.raw
            return Transform(
                cosF,-sinF,fma(y,sin,fma(x,-cos,x)).toFloat(),
                sinF,cosF,fma(x,-sin,fma(y,-cos,y)).toFloat(),
                0f,0f,1f
            )
        }

        fun rotate(center: Position,rad: Double) = rotate(center.x,center.y,rad)
    }
    private class Vec(val x: Num,val y: Num,val w: Num) {
        constructor(pos: Position):this(pos.x.raw.toFloat(),pos.y.raw.toFloat(),1f)
        fun toPos() = Position((x / w).px,(y / w).px)
    }
    private fun mul(vec: Vec): Vec {
        if(isEmpty) return vec
        val x = fma(vec.x,m00,fma(vec.y,m01,vec.w * m02))
        val y = fma(vec.x,m10,fma(vec.y,m11,vec.w * m12))
        val w = fma(vec.x,m20,fma(vec.y,m21,vec.w * m22))
        return Vec(x,y,w)
    }

    operator fun times(trans: Transform) = if(isEmpty) trans else if(trans.isEmpty) this else Transform(
        fma(m00,trans.m00,fma(m01,trans.m10,m02 * trans.m20)),
        fma(m00,trans.m01,fma(m01,trans.m11,m02 * trans.m21)),
        fma(m00,trans.m02,fma(m01,trans.m12,m02 * trans.m22)),
        fma(m10,trans.m00,fma(m11,trans.m10,m12 * trans.m20)),
        fma(m10,trans.m01,fma(m11,trans.m11,m12 * trans.m21)),
        fma(m10,trans.m02,fma(m11,trans.m12,m12 * trans.m22)),
        fma(m20,trans.m00,fma(m21,trans.m10,m22 * trans.m20)),
        fma(m20,trans.m01,fma(m21,trans.m11,m22 * trans.m21)),
        fma(m20,trans.m02,fma(m21,trans.m12,m22 * trans.m22)),
    )

    fun invert(): Transform {
        if(isEmpty) return this
        val c00 = fma(m11, m22, -(m12 * m21))
        val c01 = fma(m12, m20, -(m10 * m22))
        val c02 = fma(m10, m21, -(m11 * m20))

        val det = fma(m00, c00, fma(m01, c01, m02 * c02))
        val invDet = 1 / det

        return Transform (
            c00 * invDet,
            fma(m02, m21, -(m01 * m22)) * invDet,
            fma(m01, m12, -(m02 * m11)) * invDet,

            c01 * invDet,
            fma(m00, m22, -(m02 * m20)) * invDet,
            fma(m02, m10, -(m00 * m12)) * invDet,

            c02 * invDet,
            fma(m01, m20, -(m00 * m21)) * invDet,
            fma(m00, m11, -(m01 * m10)) * invDet
        )
    }

    operator fun times(pos: Position) = mul(Vec(pos)).toPos()
}