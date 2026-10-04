package io.github.u2894638479.kotlinmcui.backend

import com.sun.management.ThreadMXBean
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.text.DslFont
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
        override val guiScale = 1.0
        context(renderParam: Unit) override fun flush() {}
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

    @Test
    fun `the drawing entry points do not allocate`() {
        assumeTrue(bean.isThreadAllocatedMemorySupported, "this JVM cannot report per-thread allocation")

        // 32 B/call leaves room for measurement noise; a returned collection costs hundreds.
        val limit = 32.0
        val quad = bytesPerCall { Counting.renderQuad(rect, color, color, color, color, image, uv) }
        val fill = bytesPerCall { Counting.fillRect(rect, color) }
        val gradient = bytesPerCall { Counting.fillRectGradient(rect, color, Color.RED, Color.GREEN, Color.BLUE) }
        val textured = bytesPerCall { Counting.renderImage(image, rect, uv, color) }

        assertTrue(quad < limit, "renderQuad allocated ${"%.1f".format(quad)} B/call")
        assertTrue(fill < limit, "fillRect allocated ${"%.1f".format(fill)} B/call")
        assertTrue(gradient < limit, "fillRectGradient allocated ${"%.1f".format(gradient)} B/call")
        assertTrue(textured < limit, "renderImage allocated ${"%.1f".format(textured)} B/call")
        assertEquals(4 * 25 * batch, Counting.quads, "every call must reach the backend")
    }
}
