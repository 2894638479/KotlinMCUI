package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.align.Alignment
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.text.DslFont
import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * Pins the geometry the image strategies emit, quad by quad.
 *
 * These strategies had no test at all, so the rectangle handling inside them could
 * change a border or a crop without anything failing. Each entry below is one
 * `renderQuad` call, in the order it was made.
 */
class ImageStrategyTest {

    private data class Quad(
        val rect: List<Double>,
        val uv: List<Double>?,
        val colors: List<Int>,
    ) {
        override fun toString() = "rect=${rect.map { it.toInt() }} uv=${uv?.map { it.toInt() }}"
    }

    private object Recording : DslRootRenderer<Unit> {
        val quads = mutableListOf<Quad>()
        override val guiScale = 1.0
        context(renderParam: Unit) override fun flush() {}
        context(renderParam: Unit) override fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder?) {}
        context(renderParam: Unit) override fun renderQuad(
            rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color, image: ImageHolder?, uv: Rect?
        ) {
            quads += Quad(
                listOf(rect.left.raw, rect.top.raw, rect.right.raw, rect.bottom.raw),
                uv?.let { listOf(it.left.raw, it.top.raw, it.right.raw, it.bottom.raw) },
                listOf(lt.rgbaInt, rt.rgbaInt, lb.rgbaInt, rb.rgbaInt),
            )
        }
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

    private val image = ImageHolder("probe", 16.px, 16.px)

    private fun record(strategy: ImageStrategy, target: Rect, img: ImageHolder = image): List<Quad> {
        Recording.quads.clear()
        kotlin.context(Recording, Unit) { strategy.render(target, img, Color.WHITE) }
        return Recording.quads.toList()
    }

    private fun assertQuads(name: String, expected: List<String>, quads: List<Quad>) {
        assertEquals(expected, quads.map { it.toString() }, name)
    }

    private fun nineSlice() = ImageStrategy.nineSlice(
        Rect(0.px, 0.px, 176.px, 166.px), Rect(3.px, 3.px, 173.px, 163.px), 1.0
    )

    @Test
    fun `a single-quad strategy samples the image it is told to`() {
        assertQuads("stretch", listOf("rect=[0, 0, 32, 16] uv=[0, 0, 16, 16]"), record(ImageStrategy.stretch, Rect(0.px, 0.px, 32.px, 16.px)))
        // Cropping keeps the middle band of the source and fills the target.
        assertQuads("clip wide", listOf("rect=[0, 0, 64, 16] uv=[0, 6, 16, 10]"), record(ImageStrategy.clip, Rect(0.px, 0.px, 64.px, 16.px)))
        assertQuads("clip tall", listOf("rect=[0, 0, 16, 64] uv=[6, 0, 10, 16]"), record(ImageStrategy.clip, Rect(0.px, 0.px, 16.px, 64.px)))
        // Repeat deliberately emits negative UV so the texture wraps.
        assertQuads("repeat", listOf("rect=[0, 0, 40, 20] uv=[-20, -10, 20, 10]"), record(ImageStrategy.repeat(Alignment(), 1.0), Rect(0.px, 0.px, 40.px, 20.px)))
    }

    @Test
    fun `tiling crops the last row and column`() {
        val quads = record(ImageStrategy.repeatUV(Rect(0.px, 0.px, 16.px, 16.px), 1.0), Rect(0.px, 0.px, 50.px, 40.px))
        assertEquals(12, quads.size, "three full columns, one partial; two full rows, one partial")
        assertQuads(
            "repeatUV", listOf(
                "rect=[0, 0, 16, 16] uv=[0, 0, 16, 16]",
                "rect=[0, 16, 16, 32] uv=[0, 0, 16, 16]",
                "rect=[0, 32, 16, 40] uv=[0, 0, 16, 8]",
                "rect=[16, 0, 32, 16] uv=[0, 0, 16, 16]",
                "rect=[16, 16, 32, 32] uv=[0, 0, 16, 16]",
                "rect=[16, 32, 32, 40] uv=[0, 0, 16, 8]",
                "rect=[32, 0, 48, 16] uv=[0, 0, 16, 16]",
                "rect=[32, 16, 48, 32] uv=[0, 0, 16, 16]",
                "rect=[32, 32, 48, 40] uv=[0, 0, 16, 8]",
                "rect=[48, 0, 50, 16] uv=[0, 0, 2, 16]",
                "rect=[48, 16, 50, 32] uv=[0, 0, 2, 16]",
                "rect=[48, 32, 50, 40] uv=[0, 0, 2, 8]",
            ), quads
        )
    }

    @Test
    fun `letterboxing fills the target before drawing the image`() {
        val quads = record(ImageStrategy.fitIn(Alignment()), Rect(0.px, 0.px, 40.px, 40.px))
        assertEquals(2, quads.size)
        assertEquals(null, quads[0].uv, "the fill quad is not textured")
        assertEquals(Color.TRANSPARENT_WHITE.rgbaInt, quads[0].colors[0], "the fill uses the strategy's own colour")
        assertQuads("fitIn", listOf("rect=[0, 0, 40, 40] uv=[0, 0, 16, 16]"), listOf(quads[1]))
    }

    @Test
    fun `an unscaled nine-slice samples exactly the region each patch covers`() {
        // Target size == source size, so every patch is its own region one to one.
        val quads = record(nineSlice(), Rect(0.px, 0.px, 176.px, 166.px))
        assertEquals(9, quads.size)
        quads.forEach { assertEquals(it.rect, it.uv, "patch $it must sample the region it covers") }
    }

    @Test
    fun `a nine-slice smaller than its corners keeps the corners and drops the empty middle`() {
        // 3 px of border on each side leaves 4 px of middle horizontally and none
        // vertically, so the middle row produces no quad at all.
        val quads = record(
            ImageStrategy.nineSlice(Rect(0.px, 0.px, 200.px, 20.px), Rect(3.px, 3.px, 197.px, 17.px), 1.0),
            Rect(0.px, 0.px, 10.px, 6.px)
        )
        assertQuads(
            "nine-slice smaller than its corners", listOf(
                "rect=[0, 0, 3, 3] uv=[0, 0, 3, 3]",
                "rect=[0, 3, 3, 6] uv=[0, 17, 3, 20]",
                "rect=[3, 0, 7, 3] uv=[3, 0, 7, 3]",
                "rect=[3, 3, 7, 6] uv=[3, 17, 7, 20]",
                "rect=[7, 0, 10, 3] uv=[197, 0, 200, 3]",
                "rect=[7, 3, 10, 6] uv=[197, 17, 200, 20]",
            ), quads
        )
    }

    @Test
    fun `a stretched nine-slice clamps its scale and stretches only the middle`() {
        val quads = record(nineSlice(), Rect(0.px, 0.px, 100.px, 40.px))
        assertEquals(9, quads.size, "corners keep their size, so the middle stays one quad per band")
        assertQuads(
            "nine-slice stretched", listOf(
                "rect=[0, 0, 3, 3] uv=[0, 0, 3, 3]",
                "rect=[0, 3, 3, 37] uv=[0, 3, 3, 37]",
                "rect=[0, 37, 3, 40] uv=[0, 163, 3, 166]",
            ), quads.take(3)
        )
        assertEquals("rect=[3, 3, 97, 37] uv=[3, 3, 97, 37]", quads[4].toString(), "the centre patch spans the middle")
    }
}
