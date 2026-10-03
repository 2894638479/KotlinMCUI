package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.dsl.ui.LateBox
import io.github.u2894638479.kotlinmcui.dsl.ui.Overlay
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslContainerTest {

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
    fun `LateBox does not repeat its content when laid out twice`() {
        val screen = buildScreen {
            LateBox(Modifier.size(100.px, 100.px)) {
                Box { TextFlatten { "late".emit() } }
            }
        }
        val lateBox = screen.children[0]
        val declared = lateBox.children.size
        assertTrue(declared > 0, "LateBox should have built its content")

        lateBox.layoutVertical()
        assertEquals(declared, lateBox.children.size, "a second layout pass must not re-run the content")
    }

    @Test
    fun `overlay identity is derived from its parent`() {
        val screen = buildScreen {
            Overlay(Modifier, OverlayAlign.ToPoint(Position(0.px, 0.px))) { TextFlatten { "o".emit() } }
        }
        val overlay = screen.children.single()
        assertNotNull(
            overlay.identity.prevNode,
            "an overlay must extend its parent's identity instead of using a root-level one"
        )
    }
}
