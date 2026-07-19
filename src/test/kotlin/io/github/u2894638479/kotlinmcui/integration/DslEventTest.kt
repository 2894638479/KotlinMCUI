package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.component.*
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.utils.Simple
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
}
