package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.LazyColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.Row
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollBarVertical
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.scroll.Scroller
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Behavioural tests for [ScrollBarVertical]'s drag handling.
 *
 * The only existing "test" asserted `screen.children.isNotEmpty()`, so multiplying
 * the drag delta by `0.0` — a scroll bar that can never scroll — kept the whole
 * suite green.
 *
 * Publishes the fake platform through `TestRuntime`, like the other integration classes,
 * and puts the previous value back afterwards, so neither this class nor any other one
 * has to run first.
 */
class DslScrollBarTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() { TestRuntime.restore(previousBackendProvider) }

    /**
     * The bar sizes its three spacers from the scroller's *previous* layout, so the
     * first frame allocates zero height to the knob. A second frame gives it a real
     * size and makes the knob hittable.
     */
    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("test", function).dslScreen
        TestBackend.renderFrame(screen)
        TestBackend.renderFrame(screen)
        return screen
    }

    private class Fixture(val screen: DslScreen, val scroller: Scroller) {
        /** The Row's second child is the scroll bar; its only child is the track. */
        val bar = screen.children[0].children[1].children[0]
        val rect = bar.rect
        val content = scroller.spaceBefore() + scroller.high.raw - scroller.low.raw + scroller.spaceAfter()
    }

    private fun fixture(): Fixture {
        lateinit var scrollerProp: StableRW<Scroller>
        val screen = buildScreen {
            Row(Modifier.size(300.px, 100.px)) {
                scrollerProp = local { Scroller.empty }
                LazyColumn(Modifier.size(200.px, 100.px), scrollerProp) {
                    for (i in 1..20) Spacer(Modifier.size(200.px, 30.px), id = i)
                }
                ScrollBarVertical(Modifier.width(10.px), scrollerProp) {}
            }
        }
        return Fixture(screen, scrollerProp.value)
    }

    @Test
    fun `dragging the knob scrolls by the dragged fraction of the content`() {
        val f = fixture()
        val viewport = f.scroller.high.raw - f.scroller.low.raw
        assertTrue(f.content > viewport, "the list must overflow for the bar to have anything to drag")
        assertEquals(0.0, f.scroller.rawScroll)

        context(KeyModifiers(), Position(f.rect.left + 1.px, f.rect.top + 1.px)) {
            assertTrue(f.bar.mouseDown(MouseButton.LEFT), "a press on the knob must be consumed")
        }
        // Half a track height must scroll half of the whole content.
        context(Position(f.rect.left + 1.px, f.rect.top + 1.px + f.rect.height / 2)) {
            f.bar.mouseMove()
        }

        assertEquals(
            0.5 * f.content, f.scroller.rawScroll, 1.0,
            "dragging half the track must scroll half of ${f.content}px"
        )
    }

    @Test
    fun `a mouse move without a press on the knob does not scroll`() {
        val f = fixture()
        context(Position(f.rect.left + 1.px, f.rect.top + f.rect.height / 2)) {
            f.bar.mouseMove()
        }
        assertEquals(0.0, f.scroller.rawScroll, "the bar must not scroll before the knob was grabbed")
    }

    @Test
    fun `a drag stops scrolling after the button is released`() {
        val f = fixture()
        val start = Position(f.rect.left + 1.px, f.rect.top + 1.px)

        context(KeyModifiers(), start) { f.bar.mouseDown(MouseButton.LEFT) }
        context(KeyModifiers(), start) { f.bar.mouseUp(MouseButton.LEFT) }
        context(Position(f.rect.left + 1.px, f.rect.top + 1.px + f.rect.height / 2)) {
            f.bar.mouseMove()
        }
        assertEquals(0.0, f.scroller.rawScroll, "a released bar must ignore further mouse moves")
    }
}
