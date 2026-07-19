package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.utils.Simple
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslLayoutTest {

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
    fun `Row lays out children in left-to-right order`() {
        val screen = buildScreen {
            Row(Modifier.size(100.px, 20.px)) {
                Spacer(Modifier.size(30.px, 20.px), id = "a")
                Spacer(Modifier.size(40.px, 20.px), id = "b")
            }
        }
        val row = screen.children[0]
        val a = row.children[0]
        val b = row.children[1]

        assertTrue(a.rect.left < b.rect.left, "child a should be left of child b")
        assertEquals(a.rect.right, b.rect.left, "b should start where a ends")
        assertTrue(a.rect.width > 0.px && b.rect.width > 0.px, "children should have nonzero width")
    }

    @Test
    fun `Column lays out children in top-to-bottom order`() {
        val screen = buildScreen {
            Column(Modifier.size(50.px, 100.px)) {
                Spacer(Modifier.size(50.px, 30.px), id = "a")
                Spacer(Modifier.size(50.px, 40.px), id = "b")
            }
        }
        val column = screen.children[0]
        val a = column.children[0]
        val b = column.children[1]

        assertTrue(a.rect.top < b.rect.top, "child a should be above child b")
        assertEquals(a.rect.bottom, b.rect.top, "b should start where a ends")
    }

    @Test
    fun `weighted items distribute space proportionally`() {
        val screen = buildScreen {
            Row(Modifier.size(100.px, 20.px)) {
                Spacer(Modifier.weight(1.0).size(Measure.AUTO, 20.px), id = "a")
                Spacer(Modifier.weight(2.0).size(Measure.AUTO, 20.px), id = "b")
            }
        }
        val row = screen.children[0]
        val a = row.children[0]
        val b = row.children[1]

        // Both items have nonzero width
        assertTrue(a.rect.width > 0.px, "a should have width > 0")
        assertTrue(b.rect.width > 0.px, "b should have width > 0")
        // b has weight 2, a has weight 1 — b should be wider
        assertTrue(b.rect.width > a.rect.width, "weight-2 item should be wider than weight-1")
    }

    @Test
    fun `Box contains its child`() {
        val screen = buildScreen {
            Box {
                Spacer(Modifier.size(50.px, 30.px), id = "child")
            }
        }
        val box = screen.children[0]
        val child = box.children[0]

        assertTrue(child.rect.width > 0.px)
        assertTrue(child.rect.height > 0.px)
    }

    @Test
    fun `Button has nonzero size`() {
        val screen = buildScreen {
            Button { Simple.Text { "hello" } }
        }
        val button = screen.children[0]
        assertTrue(button.rect.width > 0.px)
        assertTrue(button.rect.height > 0.px)
    }

    @Test
    fun `nested layout produces non-overlapping children`() {
        val screen = buildScreen {
            Column(Modifier.size(200.px, 200.px)) {
                Row(Modifier.size(200.px, 50.px)) {
                    Spacer(Modifier.size(100.px, 50.px), id = "top-left")
                    Spacer(Modifier.size(100.px, 50.px), id = "top-right")
                }
                Row(Modifier.size(200.px, 50.px)) {
                    Spacer(Modifier.size(200.px, 50.px), id = "bottom")
                }
            }
        }
        val column = screen.children[0]
        val topRow = column.children[0]
        val bottomRow = column.children[1]

        // top row is above bottom row
        assertTrue(topRow.rect.top < bottomRow.rect.top)
        // top row has 2 children side by side
        assertEquals(2, topRow.children.size)
        assertTrue(topRow.children[0].rect.left < topRow.children[1].rect.left)
    }
}
