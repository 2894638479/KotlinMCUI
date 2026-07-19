package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.component.*
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.decorator.*
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.utils.Simple
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslDecoratorTest {

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
    fun `background decorator exists`() {
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(50.px, 50.px), id = "bg")
            spacer.background(Color.RED)
        }
        assertEquals(1, screen.children.size)
    }

    @Test
    fun `highlightBox sets highlightable`() {
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(50.px, 50.px), id = "hl")
            spacer.highlightBox()
        }
        val child = screen.children[0]
        assertTrue(child.highlightable)
    }

    @Test
    fun `clickable sets focusable and highlightable`() {
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(50.px, 50.px), id = "click")
            spacer.clickable { /* no-op */ }
        }
        val child = screen.children[0]
        assertTrue(child.focusable)
        assertTrue(child.highlightable)
    }

    @Test
    fun `narrate sets narration properties`() {
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(50.px, 50.px), id = "narr")
            spacer.narrate("test narration")
        }
        val child = screen.children[0]
        assertTrue(child.narratable)
        assertEquals("test narration", child.narration)
    }

    @Test
    fun `tooltip sets tooltip function`() {
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(50.px, 50.px), id = "tt")
            spacer.tooltip { Simple.Text { "tip" } }
        }
        val child = screen.children[0]
        assertNotNull(child.tooltip)
    }

    @Test
    fun `padding modifier increases spacing`() {
        val screen = buildScreen {
            Spacer(Modifier.size(50.px, 50.px).padding(10.px), id = "padded")
        }
        val child = screen.children[0]
        assertEquals(10.px, child.modifier.paddingLeft)
        assertEquals(10.px, child.modifier.paddingTop)
        assertEquals(10.px, child.modifier.paddingRight)
        assertEquals(10.px, child.modifier.paddingBottom)
    }

    @Test
    fun `buttonBackground decorator adds narration`() {
        val screen = buildScreen {
            val box = Box { Simple.Text { "btn" } }
            box.buttonBackground()
        }
        val child = screen.children[0]
        assertTrue(child.narratable)
    }

    @Test
    fun `scissor decorator clips to rect`() {
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(100.px, 100.px), id = "scissor")
            spacer.scissor()
        }
        assertEquals(1, screen.children.size)
    }
}
