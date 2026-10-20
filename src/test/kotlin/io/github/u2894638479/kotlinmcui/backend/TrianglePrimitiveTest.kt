package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.text.DslFont
import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * The triangle-list primitive is part of the renderer contract — a quad cannot express a
 * shape that is not a parallelogram — and [fillPolygon] is the convenience that turns a
 * convex polygon into a fan of it.
 */
class TrianglePrimitiveTest {

    /** A renderer that records the triangle list it was handed; everything else is a stub. */
    private class Recorder : DslRootRenderer<Unit> by Plain {
        var vertices: FloatArray? = null
        var colors: IntArray? = null
        var image: ImageHolder? = null
        var calls = 0

        context(renderParam: Unit)
        override fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder?) {
            calls++
            this.vertices = vertices
            this.colors = colors
            this.image = image
        }
    }

    private fun Recorder.polygon(points: List<Position>, colors: List<Color>) {
        kotlin.context(Unit) { fillPolygon(points, colors) }
    }

    @Test
    fun `a convex polygon becomes a fan around its first vertex`() {
        val recorder = Recorder()

        recorder.polygon(square, squareColors)

        assertEquals(1, recorder.calls, "one call, whatever the vertex count")
        assertContentEquals(
            floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 0f, 10f, 10f, 0f, 10f),
            recorder.vertices!!,
            "two triangles, each starting from the apex",
        )
        assertContentEquals(
            intArrayOf(
                Color.RED.rgbaInt, Color.GREEN.rgbaInt, Color.BLUE.rgbaInt,
                Color.RED.rgbaInt, Color.BLUE.rgbaInt, Color.WHITE.rgbaInt,
            ),
            recorder.colors!!,
            "the colours follow the vertices",
        )
        assertNull(recorder.image, "the convenience form is untextured")
    }

    @Test
    fun `a triangle is a single triangle`() {
        val recorder = Recorder()

        recorder.polygon(square.take(3), squareColors.take(3))

        assertContentEquals(floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f), recorder.vertices!!)
        assertEquals(3, recorder.colors!!.size)
    }

    @Test
    fun `a vertex that is not a plain number draws nothing`() {
        val recorder = Recorder()

        recorder.polygon(
            listOf(Position(0.px, 0.px), Position(Measure.AUTO, 0.px), Position(10.px, 10.px)),
            squareColors.take(3),
        )

        assertEquals(0, recorder.calls, "an unmeasured coordinate must skip the draw, not draw at zero")
    }

    @Test
    fun `the shape of the input is checked`() {
        assertFailsWith<IllegalArgumentException>("two vertices are not a polygon") {
            Recorder().polygon(square.take(2), squareColors.take(2))
        }
        assertFailsWith<IllegalArgumentException>("every vertex needs a colour") {
            Recorder().polygon(square, squareColors.take(3))
        }
    }

    private val square = listOf(
        Position(0.px, 0.px), Position(10.px, 0.px), Position(10.px, 10.px), Position(0.px, 10.px)
    )
    private val squareColors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE)

    /** A renderer that draws nothing; the recording subclass overrides what it needs. */
    private object Plain : DslRootRenderer<Unit> {
        override val guiScale = 1.0
        context(renderParam: Unit) override fun renderQuad(
            rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color, image: ImageHolder?, uv: Rect?
        ) {}
        context(renderParam: Unit) override fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder?) {}
        context(renderParam: Unit, ctx: DslScaleContext) override fun renderButton(rect: Rect, highlighted: Boolean, active: Boolean, color: Color) {}
        context(renderParam: Unit, ctx: DslScaleContext) override fun renderSlot(rect: Rect) {}
        context(renderParam: Unit, ctx: DslScaleContext) override fun renderContainer(rect: Rect) {}
        context(renderParam: Unit, ctx: DslScaleContext) override fun renderTooltip(rect: Rect) {}
        context(renderParam: Unit, ctx: DslScaleContext) override fun renderItem(rect: Rect, item: String, count: Int, damage: Double?, enchanted: Boolean) {}
        context(ctx: DslScaleContext, renderParam: Unit) override fun renderDefaultBackground(rect: Rect) {}
        context(renderParam: Unit) override fun withScissor(rect: Rect, block: () -> Unit) = block()
        context(renderParam: Unit) override fun withTransform(transform: Transform, block: () -> Unit) = block()
        override fun getFont(name: String?): DslFont<Unit> = error("not used")
    }
}
