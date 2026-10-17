package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.get
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Contract tests for [OverlayAlign.AutoToRect] and [OverlayAlign.AutoToPoint].
 *
 * Both classes promise one thing: whatever the anchor does, the overlay ends up
 * **inside** the keep-in rect. That contract is asserted directly and no exact
 * coordinate is pinned, so these tests stay valid while the clamps themselves are
 * being reworked.
 *
 * Publishes the fake platform through `TestRuntime`, like the other integration classes,
 * and puts the previous value back afterwards, so neither this class nor any other one
 * has to run first.
 */
class OverlayAlignTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslPlatform)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() { TestRuntime.restore(previousBackendProvider) }

    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("test", function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    /** A 30x30 spacer standing in for the overlay's own measured size. */
    private fun tip(): DslComponent = buildScreen {
        Spacer(Modifier.size(30.px, 30.px), id = "tip")
    }.children[0]

    private fun assertInside(tip: DslComponent, axis: Axis, keepin: Rect, what: String) {
        val keepinLow = keepin[axis].low
        val keepinHigh = keepin[axis].high
        val low = when (axis) {
            Axis.Horizontal -> tip.rect.left
            Axis.Vertical -> tip.rect.top
        }
        val high = when (axis) {
            Axis.Horizontal -> tip.rect.right
            Axis.Vertical -> tip.rect.bottom
        }
        assertTrue(
            low >= keepinLow && high <= keepinHigh,
            "$what: the overlay size must fit inside the keep-in rect, " +
                "but ${low.raw}..${high.raw} escapes ${keepinLow.raw}..${keepinHigh.raw}"
        )
    }

    private fun autoToRect(anchor: Rect, keepin: Rect, axis: Axis): DslComponent =
        tip().also { OverlayAlign.AutoToRect(anchor, keepin).layoutAxis(it, Modifier, axis) }

    private fun autoToPoint(point: Position, keepin: Rect, axis: Axis): DslComponent =
        tip().also { OverlayAlign.AutoToPoint(point, keepin).layoutAxis(it, Modifier, axis) }

    // ── AutoToRect: one case per overflowing edge ────────────────────────

    private val keepin = Rect(100.px, 100.px, 300.px, 300.px)

    @Test
    fun `AutoToRect overflowing the right edge is pulled back inside`() {
        // rect.left + size (280 + 30) > keepin.right (300)
        val anchor = Rect(280.px, 150.px, 300.px, 180.px)
        assertInside(
            autoToRect(anchor, keepin, Axis.Horizontal), Axis.Horizontal, keepin,
            "AutoToRect right overflow"
        )
    }

    @Test
    fun `AutoToRect overflowing the bottom edge is flipped inside`() {
        // rect.bottom + size (320 + 30) > keepin.bottom (300)
        val anchor = Rect(150.px, 285.px, 180.px, 320.px)
        assertInside(
            autoToRect(anchor, keepin, Axis.Vertical), Axis.Vertical, keepin,
            "AutoToRect bottom overflow"
        )
    }

    /**
     * The anchor starts left of the keep-in rect. The implementation only tests the
     * *high* edge (`rect.left + size > keepin.right`), so nothing pulls the overlay
     * back over the low edge and it lands off-screen.
     */
    @Test
    fun `AutoToRect overflowing the left edge stays inside`() {
        val anchor = Rect(80.px, 150.px, 110.px, 180.px)
        assertInside(
            autoToRect(anchor, keepin, Axis.Horizontal), Axis.Horizontal, keepin,
            "AutoToRect left overflow"
        )
    }

    /**
     * The anchor touches the top edge while overflowing the bottom one, so the
     * "flip above" fallback (`rect.top - size`) leaves the overlay above `keepin.top`.
     */
    @Test
    fun `AutoToRect overflowing the top edge stays inside`() {
        val anchor = Rect(150.px, 90.px, 180.px, 285.px)
        assertInside(
            autoToRect(anchor, keepin, Axis.Vertical), Axis.Vertical, keepin,
            "AutoToRect top overflow"
        )
    }

    @Test
    fun `AutoToRect does not clamp an anchor that already fits`() {
        val anchor = Rect(120.px, 120.px, 150.px, 150.px)
        // Neither branch fires, so the default placement is kept: the overlay is
        // aligned to the anchor's left edge horizontally and hangs below it vertically.
        val horizontal = autoToRect(anchor, keepin, Axis.Horizontal)
        assertEquals(anchor.left, horizontal.rect.left)
        val vertical = autoToRect(anchor, keepin, Axis.Vertical)
        assertEquals(anchor.bottom, vertical.rect.top)
    }

    // ── AutoToPoint: the tip must not fall off the keep-in rect ───────────

    @Test
    fun `AutoToPoint at all four corners of the keep-in rect stays inside`() {
        val corners = listOf(
            Position(keepin.left, keepin.top),
            Position(keepin.right, keepin.top),
            Position(keepin.left, keepin.bottom),
            Position(keepin.right, keepin.bottom),
        )
        corners.forEach { point ->
            assertInside(
                autoToPoint(point, keepin, Axis.Horizontal), Axis.Horizontal, keepin,
                "AutoToPoint $point horizontal"
            )
            assertInside(
                autoToPoint(point, keepin, Axis.Vertical), Axis.Vertical, keepin,
                "AutoToPoint $point vertical"
            )
        }
    }

    /**
     * A keep-in rect narrower than twice the tip forces the `else` fallback — neither
     * side of the point fits the tip. The fallback must still land inside.
     */
    @Test
    fun `AutoToPoint falls back to the high edge and stays inside`() {
        val narrow = Rect(100.px, 100.px, 140.px, 140.px)
        val point = Position(120.px, 120.px)
        assertInside(
            autoToPoint(point, narrow, Axis.Horizontal), Axis.Horizontal, narrow,
            "AutoToPoint narrow horizontal"
        )
        assertInside(
            autoToPoint(point, narrow, Axis.Vertical), Axis.Vertical, narrow,
            "AutoToPoint narrow vertical"
        )
    }
}
