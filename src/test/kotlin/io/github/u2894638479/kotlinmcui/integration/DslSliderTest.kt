package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatformUtils
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.dsl.ui.Slider
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.prop.StableRW
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Behavioural tests for [Slider].
 *
 * The slider had no tests at all, so removing the `coerceIn(0.0..1.0)` from
 * `setProgress` — which lets a drag write a progress outside 0..1 straight into the
 * caller's property — went unnoticed by the whole suite.
 *
 * Publishes the fake platform through `TestRuntime`, like the other integration classes,
 * and puts the previous value back afterwards, so neither this class nor any other one
 * has to run first.
 */
class DslSliderTest {

    private var previousBackendProvider: (() -> DslPlatformUtils)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() { TestRuntime.restore(previousBackendProvider) }

    /** A hand-rolled [StableRW] so the test owns the value the slider writes into. */
    private class DoubleProp(var v: Double) : StableRW<Double> {
        override fun getValue() = v
        override fun setValue(value: Double) { v = value }
    }

    private fun slider(
        axis: Axis,
        step: Double = 0.25,
        initial: Double = 0.5,
    ): Pair<DslScreen, DoubleProp> {
        val prop = DoubleProp(initial)
        val screen = TestBackend.create("test") {
            Slider(Modifier.size(100.px, 20.px), axis, prop, step, id = "slider") {}
        }.dslScreen
        TestBackend.renderFrame(screen)
        return screen to prop
    }

    private fun press(screen: DslScreen, at: Position): Boolean {
        val bar = screen.children[0]
        return context(KeyModifiers(), at) { bar.mouseDown(MouseButton.LEFT) }
    }

    private fun move(screen: DslScreen, at: Position) {
        val bar = screen.children[0]
        context(at) { bar.mouseMove() }
    }

    // ── Drag ────────────────────────────────────────────────────────────

    @Test
    fun `dragging a horizontal slider clamps progress at both ends`() {
        val (screen, prop) = slider(Axis.Horizontal)
        val bar = screen.children[0]
        val r = bar.rect

        assertTrue(press(screen, Position(r.left + 1.px, r.top + 1.px)),
            "a press inside the track must be consumed")

        move(screen, Position(r.right + 200.px, r.top + 1.px))
        assertEquals(1.0, prop.v, "dragging past the right end must clamp progress to 1.0")

        move(screen, Position(r.left - 200.px, r.top + 1.px))
        assertEquals(0.0, prop.v, "dragging past the left end must clamp progress to 0.0")
    }

    @Test
    fun `a horizontal drag follows the mouse fraction of the usable track`() {
        val (screen, prop) = slider(Axis.Horizontal, initial = 0.0)
        val bar = screen.children[0]
        val r = bar.rect
        val button = 8.px

        // The knob centre sits half a button in from each end of the track.
        press(screen, Position(r.left + button / 2, r.top + 1.px))
        move(screen, Position(r.left + button / 2 + (r.right - r.left - button) / 2, r.top + 1.px))
        assertEquals(0.5, prop.v, "the middle of the usable track is progress 0.5")
    }

    @Test
    fun `dragging a vertical slider clamps progress at both ends`() {
        val (screen, prop) = slider(Axis.Vertical)
        val bar = screen.children[0]
        val r = bar.rect

        assertTrue(press(screen, Position(r.left + 1.px, r.top + 1.px)))

        move(screen, Position(r.left + 1.px, r.bottom + 200.px))
        assertEquals(1.0, prop.v, "dragging past the bottom must clamp progress to 1.0")

        move(screen, Position(r.left + 1.px, r.top - 200.px))
        assertEquals(0.0, prop.v, "dragging past the top must clamp progress to 0.0")
    }

    @Test
    fun `a drag stops changing progress after the button is released`() {
        val (screen, prop) = slider(Axis.Horizontal)
        val bar = screen.children[0]
        val r = bar.rect

        press(screen, Position(r.left + 1.px, r.top + 1.px))
        move(screen, Position(r.left - 200.px, r.top + 1.px))
        assertEquals(0.0, prop.v)

        context(KeyModifiers(), Position(r.left + 1.px, r.top + 1.px)) { bar.mouseUp(MouseButton.LEFT) }
        move(screen, Position(r.right + 200.px, r.top + 1.px))
        assertEquals(0.0, prop.v, "a released slider must ignore further mouse moves")
    }

    @Test
    fun `a press outside the track is not consumed`() {
        val (screen, _) = slider(Axis.Horizontal)
        val bar = screen.children[0]
        assertFalse(
            press(screen, Position(bar.rect.right + 500.px, bar.rect.bottom + 500.px)),
            "a press outside the slider must fall through to the sibling components"
        )
    }

    // ── Keyboard ────────────────────────────────────────────────────────

    @Test
    fun `keyboard steps a horizontal slider only on its own axis and only once key focused`() {
        val (screen, prop) = slider(Axis.Horizontal, step = 0.25, initial = 0.5)
        val bar = screen.children[0]
        TestRoot.setFocus(bar.identity, FocusSource.KEYBOARD)

        context(KeyModifiers()) {
            assertFalse(bar.keyDown(Key.LEFT, 0), "arrows are inert until ENTER arms the slider")
        }
        assertEquals(0.5, prop.v)

        context(KeyModifiers()) { assertTrue(bar.keyDown(Key.ENTER, 0), "ENTER toggles key focus") }

        context(KeyModifiers()) { assertTrue(bar.keyDown(Key.RIGHT, 0)) }
        assertEquals(0.75, prop.v)
        context(KeyModifiers()) { assertTrue(bar.keyDown(Key.LEFT, 0)) }
        assertEquals(0.5, prop.v)

        // The perpendicular axis belongs to the surrounding layout, not to the slider.
        context(KeyModifiers()) { assertFalse(bar.keyDown(Key.UP, 0)) }
        context(KeyModifiers()) { assertFalse(bar.keyDown(Key.DOWN, 0)) }
        assertEquals(0.5, prop.v)

        context(KeyModifiers()) { assertTrue(bar.keyDown(Key.HOME, 0)) }
        assertEquals(0.0, prop.v)
        context(KeyModifiers()) { assertTrue(bar.keyDown(Key.END, 0)) }
        assertEquals(1.0, prop.v)

        context(KeyModifiers()) { bar.keyDown(Key.RIGHT, 0) }
        assertEquals(1.0, prop.v, "stepping past the end must clamp")
    }

    @Test
    fun `keyboard steps a vertical slider on the vertical axis`() {
        val (screen, prop) = slider(Axis.Vertical, step = 0.25, initial = 0.5)
        val bar = screen.children[0]
        TestRoot.setFocus(bar.identity, FocusSource.KEYBOARD)

        context(KeyModifiers()) { assertFalse(bar.keyDown(Key.DOWN, 0), "not armed yet") }
        context(KeyModifiers()) { bar.keyDown(Key.ENTER, 0) }

        context(KeyModifiers()) { assertTrue(bar.keyDown(Key.DOWN, 0)) }
        assertEquals(0.75, prop.v)
        context(KeyModifiers()) { assertTrue(bar.keyDown(Key.UP, 0)) }
        assertEquals(0.5, prop.v)
        context(KeyModifiers()) { assertFalse(bar.keyDown(Key.LEFT, 0)) }
        assertEquals(0.5, prop.v)
    }

    @Test
    fun `keyboard does nothing while the slider is not focused`() {
        val (screen, prop) = slider(Axis.Horizontal, step = 0.25, initial = 0.5)
        val bar = screen.children[0]

        context(KeyModifiers()) {
            assertFalse(bar.keyDown(Key.ENTER, 0), "an unfocused slider must not handle keys")
        }
        context(KeyModifiers()) { assertFalse(bar.keyDown(Key.HOME, 0)) }
        assertEquals(0.5, prop.v)
    }
}
