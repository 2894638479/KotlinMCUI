package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.widget.Simple
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslLayoutTest {

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

    @Test
    fun `alignable bounds are padding-inclusive on both axes`() {
        val screen = buildScreen {
            Box(Modifier.size(50.px, 60.px).padding(3.px, 5.px, 11.px, 7.px)) {
                Spacer(Modifier.size(10.px, 20.px), id = "child")
            }
        }
        val box = screen.children[0]
        assertEquals(50.px, box.rect.width)
        assertEquals(60.px, box.rect.height)

        val horizontal = box.alignableHorizontal
        assertEquals(box.rect.left - 3.px, horizontal.low)
        assertEquals(box.rect.right + 11.px, horizontal.high)
        assertEquals(64.px, horizontal.size)

        val vertical = box.alignableVertical
        assertEquals(box.rect.top - 5.px, vertical.low)
        assertEquals(box.rect.bottom + 7.px, vertical.high)
        assertEquals(72.px, vertical.size)
    }

    @Test
    fun `writing back read alignable bounds does not move the component`() {
        val screen = buildScreen {
            Box(Modifier.padding(3.px, 5.px, 11.px, 7.px)) {
                Spacer(Modifier.size(10.px, 20.px), id = "child")
            }
        }
        val box = screen.children[0]
        val expected = box.rect.let { listOf(it.left, it.top, it.right, it.bottom) }

        for (alignable in listOf(box.alignableHorizontal, box.alignableVertical)) {
            val low = alignable.low
            val high = alignable.high
            alignable.low = low
            alignable.high = high
            assertEquals(low, alignable.low)
            assertEquals(high, alignable.high)
        }

        assertEquals(expected[0], box.rect.left)
        assertEquals(expected[1], box.rect.top)
        assertEquals(expected[2], box.rect.right)
        assertEquals(expected[3], box.rect.bottom)
    }
}
