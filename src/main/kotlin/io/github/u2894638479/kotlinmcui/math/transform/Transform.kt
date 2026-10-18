package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 3×3 homogeneous transformation matrix using [Float] precision.
 *
 * Supports translation, scale, and rotation via companion factory methods.
 * Transformations compose via `*` and can be inverted. The [empty] identity
 * matrix is the default — use [isEmpty] to detect it for optimisation.
 *
 * Multiply-add is a plain `a * b + c`: `Math.fma` would need Java 9, and the single rounding it saves
 * is far below what a 2D UI coordinate can observe.
 */
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

        // Math.fma would be Java 9+, and this library has to load on Java 8. The single
        // rounding it saves is far below anything a 2D UI coordinate can observe, so the
        // plain expression is used instead of a reflective call.
        private fun fma(a: Float,b: Float,c: Float): Float = a * b + c
        private fun fma(a: Double,b: Double,c: Double): Double = a * b + c

        fun translate(x: Measure,y: Measure) = if(x == 0.px && y == 0.px) empty else Transform(
            1f,0f,x.raw.toFloat(),
            0f,1f,y.raw.toFloat(),
            0f,0f,1f
        )

        fun translate(pos: Position) = translate(pos.x,pos.y)

        fun scale(centerX: Measure,centerY: Measure,x: Double,y: Double) =
            if(x == 1.0 && y == 1.0) empty else Transform(
            x.toFloat(),0f,fma(centerX.raw,-x,centerX.raw).toFloat(),
            0f,y.toFloat(),fma(centerY.raw,-y,centerY.raw).toFloat(),
            0f,0f,1f
        )

        fun scale(center: Position,x: Double,y: Double) = scale(center.x,center.y,x,y)

        fun rotate(centerX: Measure,centerY: Measure,rad: Double): Transform {
            if(rad == 0.0) return empty
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

        /** Shear (skew) around a center point.
         * @param shx Horizontal shear factor: x' = x + shx*(y - cy).
         * @param shy Vertical shear factor: y' = shy*(x - cx) + y. */
        fun shear(centerX: Measure, centerY: Measure, shx: Double, shy: Double) =
            if(shx == 0.0 && shy == 0.0) empty else Transform(
            1f, shx.toFloat(), (-shx * centerY.raw).toFloat(),
            shy.toFloat(), 1f, (-shy * centerX.raw).toFloat(),
            0f, 0f, 1f
        )

        fun shear(center: Position, shx: Double, shy: Double) = shear(center.x, center.y, shx, shy)

        /** Perspective projection via the third matrix row (origin-centric).
         * Use [perspective]\(center, px, py\) for a centered version. */
        fun perspective(px: Double, py: Double) = if(px == 0.0 && py == 0.0) empty else Transform(
            1f, 0f, 0f,
            0f, 1f, 0f,
            px.toFloat(), py.toFloat(), 1f
        )

        /** Perspective projection around a center point.
         * Translates center to origin, applies projection, translates back. */
        fun perspective(centerX: Measure, centerY: Measure, px: Double, py: Double): Transform {
            if(px == 0.0 && py == 0.0) return empty
            val cx = centerX.raw; val cy = centerY.raw
            val dot = px * cx + py * cy
            return Transform(
                (1f + px * cx).toFloat(), (py * cx).toFloat(), (-cx * dot).toFloat(),
                (px * cy).toFloat(), (1f + py * cy).toFloat(), (-cy * dot).toFloat(),
                px.toFloat(), py.toFloat(), (1f - dot).toFloat()
            )
        }

        fun perspective(center: Position, px: Double, py: Double) = perspective(center.x, center.y, px, py)
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
        // A singular matrix (e.g. scale(0, 0)) has no inverse. Returning the
        // identity keeps hit testing finite instead of spreading NaN everywhere.
        if(det == 0f || !det.isFinite()) return empty
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