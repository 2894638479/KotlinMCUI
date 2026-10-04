package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position

/**
 * Optional capability: submit a triangle list.
 *
 * [DslBackendRenderer] draws quads, and a quad cannot express an arbitrary triangle: its
 * four corners are the corners of a rectangle, so an affine mapping of them is always a
 * parallelogram, and no finite projective map turns one into a triangle either. Shapes a
 * quad cannot express — a fan for a polygon, a slanted outline, a chart — therefore need
 * this second entry point.
 *
 * Nothing in the framework calls it; it exists for components that paint something else.
 * Such a component asks the backend it was given for the capability:
 * ```
 * val triangles = backend.triangles() ?: return
 * ```
 * A backend that does not implement this interface stays valid, and a component that
 * needs it simply draws nothing there instead of failing.
 */
interface DslBackendTriangles<RP> {
    /**
     * Submit a triangle list; every three vertices form one triangle.
     *
     * @param vertices two floats `(x, y)` per vertex, or four `(x, y, u, v)` when [image]
     *   is given. Coordinates are in the same space as every other drawing call (the
     *   pixel space the framework measures in); UVs are image pixels from the top-left.
     *   The array is read during the call and may be overwritten afterwards, so a caller
     *   that draws every frame can keep reusing one buffer.
     * @param colors one packed RGBA colour per vertex, in the same order as [vertices].
     * @param image the texture for the UV-carrying form, or `null` for colour only.
     */
    context(renderParam: RP)
    fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder? = null)
}

/**
 * The triangle-list capability of this backend, or `null` when it has none.
 *
 * The cast is unavoidable — the render parameter is erased — so it lives here rather than
 * at every call site; components use this and draw nothing when the answer is `null`.
 */
@Suppress("UNCHECKED_CAST")
fun <RP> DslBackendRenderer<RP>.triangles(): DslBackendTriangles<RP>? = this as? DslBackendTriangles<RP>

/**
 * Fill a convex polygon as a triangle fan, one colour per vertex.
 *
 * Convenience for [DslBackendTriangles.renderTriangles]: it rebuilds its arrays on every
 * call, which is fine for the shapes that need it — the framework itself never calls
 * this. A concave polygon, or one that needs texture coordinates, submits its own
 * triangles through [DslBackendTriangles.renderTriangles].
 *
 * A vertex whose coordinates are not plain numbers (an unmeasured value) makes the whole
 * call draw nothing, like the other drawing entry points.
 */
context(renderParam: RP)
fun <RP> DslBackendTriangles<RP>.fillPolygon(points: List<Position>, colors: List<Color>) {
    require(points.size >= 3) { "a polygon needs at least three vertices, got ${points.size}" }
    require(colors.size == points.size) {
        "one colour per vertex, got ${colors.size} for ${points.size} vertices"
    }
    val vertices = FloatArray((points.size - 2) * 6)
    val packed = IntArray((points.size - 2) * 3)
    var v = 0
    var c = 0
    for (i in 1 until points.size - 1) {
        // The fan's apex is the first vertex, so every triangle is (0, i, i + 1).
        val apexX = points[0].x.pixelsOrElse<Float> { return }
        val apexY = points[0].y.pixelsOrElse<Float> { return }
        val x0 = points[i].x.pixelsOrElse<Float> { return }
        val y0 = points[i].y.pixelsOrElse<Float> { return }
        val x1 = points[i + 1].x.pixelsOrElse<Float> { return }
        val y1 = points[i + 1].y.pixelsOrElse<Float> { return }
        vertices[v++] = apexX; vertices[v++] = apexY
        vertices[v++] = x0; vertices[v++] = y0
        vertices[v++] = x1; vertices[v++] = y1
        packed[c++] = colors[0].rgbaInt
        packed[c++] = colors[i].rgbaInt
        packed[c++] = colors[i + 1].rgbaInt
    }
    renderTriangles(vertices, packed)
}
