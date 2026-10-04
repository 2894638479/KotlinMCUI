package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
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
import io.github.u2894638479.kotlinmcui.widget.Simple
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslDecoratorTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = dslBackendProvider
        dslBackendProvider = { TestBackend }
    }

    @AfterEach
    fun tearDown() { dslBackendProvider = previousBackendProvider }

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

    @Test
    fun `tooltipBackground applies the requested padding`() {
        val screen = buildScreen {
            val spacer = Spacer(Modifier.size(50.px, 50.px), id = "tooltip")
            spacer.tooltipBackground(10.px)
        }
        val child = screen.children[0]
        assertEquals(10.px, child.modifier.paddingLeft)
        assertEquals(10.px, child.modifier.paddingTop)
        assertEquals(10.px, child.modifier.paddingRight)
        assertEquals(10.px, child.modifier.paddingBottom)
    }

    @Test
    fun `transform converts the render mouse into the subtree space`() {
        var observed: Position? = null
        val screen = buildScreen {
            val box = Box(Modifier.size(50.px, 50.px)) {}.change { object : DslComponent by it {
                context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
                override fun <RP> render() { observed = mouse }
            } }
            box.move(30.px, 0.px)
        }
        val seen = assertNotNull(observed)
        // The frame renders with the mouse at the origin, so a 30px translation
        // must show up as -30px inside the transformed subtree.
        assertEquals(-30.px, seen.x)
        assertEquals(0.px, seen.y)
    }

    @Test
    fun `transform converts the mouse for scroll and drop events too`() {
        // Scroll and file drops carry the mouse as well; a subtree that only
        // converted press/move/release would see the parent's coordinates here and
        // disagree with the rects it laid out.
        var scrolled: Position? = null
        var dropped: Position? = null
        val screen = buildScreen {
            val box = Box(Modifier.size(50.px, 50.px)) {}.change { object : DslComponent by it {
                context(mouse: Position)
                override fun mouseScrollVertical(amount: Double): Double { scrolled = mouse; return amount }
                context(mouse: Position)
                override fun dropFiles(files: Array<Path>): Boolean { dropped = mouse; return true }
            } }
            box.move(10.px, 20.px)
        }
        val target = Position(30.px, 50.px)
        context(target) { screen.children[0].mouseScrollVertical(1.0) }
        context(target) { screen.children[0].dropFiles(emptyArray()) }

        assertEquals(20.px, assertNotNull(scrolled).x)
        assertEquals(30.px, assertNotNull(scrolled).y)
        assertEquals(20.px, assertNotNull(dropped).x)
        assertEquals(30.px, assertNotNull(dropped).y)
    }
}
