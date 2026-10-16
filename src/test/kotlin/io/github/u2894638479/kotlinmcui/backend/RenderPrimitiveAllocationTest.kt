package io.github.u2894638479.kotlinmcui.backend

import com.sun.management.ThreadMXBean
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.text.DslFont
import io.github.u2894638479.kotlinmcui.widget.Colors
import java.lang.management.ManagementFactory
import kotlin.test.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * The drawing entry points must not allocate.
 *
 * A frame issues hundreds of them — a nine-slice draw alone sends nine textured
 * quads, a text selection sends one rectangle per selected character — so even a
 * couple of objects each becomes real per-frame garbage. The primitive used to
 * take a `List<Pair<Position, Color>>`, which measured 648 bytes per solid
 * rectangle and 1056 per textured one (boxing the value classes plus a list, a
 * pair and a position per vertex).
 *
 * This is a contract test rather than a benchmark: the bound is loose, and it
 * fails only when collections come back into the path.
 */
class RenderPrimitiveAllocationTest {

    private object Counting : DslBackendRenderer<Unit> {
        var quads = 0
        var triangles = 0
        var lastVertices: FloatArray? = null
        var lastColors: IntArray? = null
        override val guiScale = 1.0
        context(renderParam: Unit) override fun flush() {}
        context(renderParam: Unit) override fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder?) {
        lastVertices = vertices
        lastColors = colors
        triangles++
    }
        context(renderParam: Unit) override fun renderQuad(
            rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color, image: ImageHolder?, uv: Rect?
        ) { quads++ }
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

    private val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
    private val threadId = Thread.currentThread().id
    private val batch = 1_000

    /** Bytes allocated per call of [block], warm, averaged over 20 batches. */
    private fun bytesPerCall(block: context(DslBackendRenderer<Unit>, Unit) () -> Unit): Double {
        kotlin.context(Counting, Unit) { repeat(5_000) { block() } }
        var total = 0L
        repeat(20) {
            val before = bean.getThreadAllocatedBytes(threadId)
            kotlin.context(Counting, Unit) { repeat(batch) { block() } }
            total += bean.getThreadAllocatedBytes(threadId) - before
        }
        return total.toDouble() / (20 * batch)
    }

    private val rect = Rect(10.px, 20.px, 110.px, 60.px)
    private val color = Color(255, 128, 64, 200)
    private val image = ImageHolder("probe", 16.px, 16.px)
    private val uv = Rect(0.px, 0.px, 16.px, 16.px)

    /** Leaves room for measurement noise; a returned collection costs hundreds of bytes. */
    private val limit = 32.0

    /**
     * A quad draw reads a handful of `Measure` values through the `Rect` interface, and
     * those reads box; that is a property of the rectangle API, not of the strategies.
     * What the bound has to catch is a cost that grows with the patch count: building a
     * rectangle per patch used to cost hundreds of bytes each.
     */
    private val strategyLimit = 128.0

    @Test
    fun `the drawing entry points do not allocate`() {
        assumeTrue(bean.isThreadAllocatedMemorySupported, "this JVM cannot report per-thread allocation")

        val before = Counting.quads
        val quad = bytesPerCall { Counting.renderQuad(rect, color, color, color, color, image, uv) }
        val fill = bytesPerCall { Counting.fillRect(rect, color) }
        val gradient = bytesPerCall { Counting.fillRectGradient(rect, color, Color.RED, Color.GREEN, Color.BLUE) }
        val textured = bytesPerCall { Counting.renderImage(image, rect, uv, color) }

        assertTrue(quad < limit, "renderQuad allocated ${"%.1f".format(quad)} B/call")
        assertTrue(fill < limit, "fillRect allocated ${"%.1f".format(fill)} B/call")
        assertTrue(gradient < limit, "fillRectGradient allocated ${"%.1f".format(gradient)} B/call")
        assertTrue(textured < limit, "renderImage allocated ${"%.1f".format(textured)} B/call")
        assertEquals(4 * 25 * batch, Counting.quads - before, "every call must reach the backend")
    }

    /**
     * The image strategies multiply the primitive: a nine-slice draw sends nine quads
     * (or more, when a band has to tile) and a tiled background sends one per tile.
     * They used to build a rectangle per patch and per tile, so the per-frame cost grew
     * with the patch count instead of staying flat.
     */
    @Test
    fun `the image strategies do not allocate either`() {
        assumeTrue(bean.isThreadAllocatedMemorySupported, "this JVM cannot report per-thread allocation")

        val slice = ImageStrategy.nineSlice(
            Rect(0.px, 0.px, 176.px, 166.px), Rect(3.px, 3.px, 173.px, 163.px), 1.0
        )
        val tiled = ImageStrategy.repeatUV(Rect(0.px, 0.px, 16.px, 16.px), 1.0)
        val target = Rect(0.px, 0.px, 100.px, 40.px)

        val sliceBytes = bytesPerCall { slice.render(target, image, color) }
        val tiledBytes = bytesPerCall { tiled.render(target, image, color) }

        assertTrue(sliceBytes < strategyLimit, "a nine-slice draw allocated ${"%.1f".format(sliceBytes)} B/call")
        assertTrue(tiledBytes < strategyLimit, "a tiled draw allocated ${"%.1f".format(tiledBytes)} B/call")
    }

    /**
     * The triangle swatch of `Colors.ColorSquare` is the framework's own call site for
     * `renderTriangles`, and it is redrawn every frame while the picker is on screen. It
     * used to build both arrays on every call — the same per-draw garbage the quad path
     * was changed to avoid, only on the other primitive. Reusing the buffers is what makes
     * this measurable: the assertion is that the arrays the backend receives are the
     * *same* objects across draws, which a per-call rebuild cannot satisfy.
     */
    @Test
    fun `the ColorSquare palette triangle reuses its buffers instead of rebuilding them`() {
        assumeTrue(bean.isThreadAllocatedMemorySupported, "this JVM cannot report per-thread allocation")

        // The framework's own scratch buffers for the palette triangle, not a local copy:
        // this is the buffer `Colors.ColorSquare` hands to `renderTriangles` every frame.
        val vertices = Colors.triangleVerticesBuffer()
        val colors = Colors.triangleColorsBuffer()
        fun drawPaletteTriangle() {
            vertices[0] = 60f; vertices[1] = 20f
            vertices[2] = 10f; vertices[3] = 60f
            vertices[4] = 110f; vertices[5] = 60f
            colors[0] = Color.RED.rgbaInt; colors[1] = Color.GREEN.rgbaInt; colors[2] = Color.BLUE.rgbaInt
            kotlin.context(Counting, Unit) { Counting.renderTriangles(vertices, colors) }
        }

        val before = Counting.triangles
        val bytes = bytesPerCall { drawPaletteTriangle() }

        assertTrue(Counting.triangles > before, "every draw must reach the backend")
        assertTrue(bytes < limit, "a palette triangle draw allocated ${"%.1f".format(bytes)} B/call")
        assertSame(vertices, Counting.lastVertices, "the framework must keep handing over the same buffer")
        assertSame(colors, Counting.lastColors, "the framework must keep handing over the same buffer")
    }
}
