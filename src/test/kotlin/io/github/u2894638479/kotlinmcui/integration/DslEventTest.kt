package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.component.*
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.decorator.scissor
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.utils.Simple
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslEventTest {

    @BeforeEach
    fun setUp() { io.github.u2894638479.kotlinmcui.backend.dslBackendProvider = { TestBackend } }

    @AfterEach
    fun tearDown() { io.github.u2894638479.kotlinmcui.backend.dslBackendProvider = null }

    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("test", function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    @Test
    fun `clickable component consumes mouseDown`() {
        val screen = buildScreen {
            Simple.Button("click") {}.clickable { /* no-op */ }
        }
        val button = screen.children[0]
        val pos = Position(button.rect.left + 1.px, button.rect.top + 1.px)
        val modifiers = KeyModifiers()

        context(modifiers, pos) {
            val consumed = button.mouseDown(MouseButton.LEFT)
            assertTrue(consumed, "clickable button should consume mouseDown event")
        }
    }

    @Test
    fun `click outside component does not trigger`() {
        var clicked = false
        val screen = buildScreen {
            Simple.Button("click") {}.clickable { clicked = true }
        }
        val button = screen.children[0]

        // Click far outside the button
        val outside = Position(9999.px, 9999.px)
        val modifiers = KeyModifiers()
        context(modifiers, outside) {
            button.mouseDown(MouseButton.LEFT)
        }
        assertFalse(clicked, "click outside button should not trigger handler")
    }

    @Test
    fun `hit test finds component at position`() {
        val screen = buildScreen {
            Box {
                Spacer(Modifier.size(50.px, 50.px), id = "target")
            }
        }
        val spacer = screen.children[0].children[0]
        val inside = Position(spacer.rect.left + 1.px, spacer.rect.top + 1.px)

        val hit = screen.testHit(inside) { it.takeIf { it.identity == spacer.identity } }
        assertNotNull(hit, "should hit the spacer when mouse is inside it")
    }

    @Test
    fun `hit test returns null for position outside`() {
        val screen = buildScreen {
            Box {
                Spacer(Modifier.size(10.px, 10.px), id = "target")
            }
        }
        val spacer = screen.children[0].children[0]
        val outside = Position(9999.px, 9999.px)

        val hit = screen.testHit(outside) { it.takeIf { it.identity == spacer.identity } }
        assertNull(hit, "should not hit spacer from outside position")
    }

    @Test
    fun `tab navigation finds next focusable component`() {
        val screen = buildScreen {
            Row {
                Simple.Button("a") {}.clickable {}
                Simple.Button("b") {}.clickable {}
            }
        }
        val row = screen.children[0]
        val a = row.children[0]
        val b = row.children[1]

        assertTrue(a.focusable)
        assertTrue(b.focusable)

        val next = screen.nextFocusable(a.identity) { it.viewSequential }
        assertEquals(b.identity, next?.identity)
    }

    @Test
    fun `scroll event passes through when unhandled`() {
        val screen = buildScreen {
            Simple.Button("scroll") {}.clickable {}
        }
        val button = screen.children[0]
        val center = Position(
            (button.rect.left + button.rect.right) / 2,
            (button.rect.top + button.rect.bottom) / 2
        )

        context(center) {
            val unconsumed = button.mouseScrollVertical(10.0)
            assertEquals(10.0, unconsumed, "unhandled scroll should pass through")
        }
    }

    @Test
    fun `clickable only reacts to the primary button`() {
        var clicked = false
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(50.px, 50.px), id = "click")
            spacer.clickable { clicked = true }
        }
        val button = screen.children[0]
        val pos = Position(button.rect.left + 1.px, button.rect.top + 1.px)

        context(KeyModifiers(), pos) {
            assertFalse(button.mouseDown(MouseButton.RIGHT), "right click should not be consumed")
        }
        assertFalse(clicked, "right click must not run the click handler")

        context(KeyModifiers(), pos) {
            assertTrue(button.mouseDown(MouseButton.LEFT))
        }
        assertTrue(clicked, "left click should run the click handler")
    }

    @Test
    fun `scissor blocks mouse events outside its rect`() {
        // The child answers `true` to everything, so only the decorator's own rect
        // check can stop these events: a child that declined them by default would
        // hide a scissor that never gated the event at all.
        var down = false
        var up = false
        var moved = false
        var dropped = false
        val screen = buildScreen {
            Box(Modifier.size(50.px, 50.px)) {
                Spacer(Modifier.size(20.px, 20.px), id = "inside").change {
                    object : DslComponent by it {
                        context(eventModifier: KeyModifiers, mouse: Position)
                        override fun mouseDown(mouseButton: MouseButton): Boolean { down = true; return true }
                        context(eventModifier: KeyModifiers, mouse: Position)
                        override fun mouseUp(mouseButton: MouseButton): Boolean { up = true; return true }
                        context(mouse: Position)
                        override fun mouseMove() { moved = true }
                        context(mouse: Position)
                        override fun dropFiles(files: Array<Path>): Boolean { dropped = true; return true }
                    }
                }
            }.scissor()
        }
        val box = screen.children[0]
        val outside = Position(9999.px, 9999.px)

        context(KeyModifiers(), outside) {
            assertFalse(box.mouseDown(MouseButton.LEFT), "mouseDown outside a scissor must not pass through")
            assertFalse(box.mouseUp(MouseButton.LEFT), "mouseUp outside a scissor must not pass through")
            assertFalse(box.dropFiles(emptyArray()), "dropFiles outside a scissor must not pass through")
        }
        context(outside) { box.mouseMove() }
        assertFalse(down, "the child must not see a mouseDown outside the clip rect")
        assertFalse(up, "the child must not see a mouseUp outside the clip rect")
        assertFalse(moved, "the child must not see a mouseMove outside the clip rect")
        assertFalse(dropped, "the child must not see a file drop outside the clip rect")

        val inside = Position(box.rect.left + 1.px, box.rect.top + 1.px)
        context(KeyModifiers(), inside) {
            assertTrue(box.mouseDown(MouseButton.LEFT))
            assertTrue(box.mouseUp(MouseButton.LEFT))
        }
        context(inside) { box.mouseMove() }
        assertTrue(down, "events inside the clip rect must reach the child")
        assertTrue(up)
        assertTrue(moved)
    }
}
