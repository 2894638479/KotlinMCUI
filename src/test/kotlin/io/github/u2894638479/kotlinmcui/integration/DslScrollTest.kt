package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatformUtils
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.LazyColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.scroll.ScrollConstraint
import io.github.u2894638479.kotlinmcui.scroll.Scroller
import io.github.u2894638479.kotlinmcui.scroll.scrollTo
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslScrollTest {

    private var previousBackendProvider: (() -> DslPlatformUtils)? = null

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

    @Test
    fun `locating an item by identity does not load the list`() {
        lateinit var scrollerProp: StableRW<Scroller>
        val screen = buildScreen {
            scrollerProp = local { Scroller.empty }
            LazyColumn(Modifier.size(200.px, 100.px), scrollerProp, local.animatable { 0.0 }) {
                repeat(100) { i ->
                    Box(Modifier.size(200.px, 20.px), id = i) { TextFlatten { "item $i".emit() } }
                }
            }
        }
        val list = screen.children[0]
        assertEquals(100, list.children.size, "the DSL body always declares every item")

        val built = { list.children.count { it.children.isNotEmpty() } }
        val builtBefore = built()
        assertTrue(builtBefore < 100, "only the visible items should have been built")

        val scroller = scrollerProp.value
        val target = list.children.last().identity
        assertEquals(99, scroller.indexOf(target), "identity lookup should find the last item")
        assertEquals(
            builtBefore, built(),
            "resolving an identity must not force items to load"
        )

        scroller.scrollTo(target, ScrollConstraint.KeepIn(0.px))
        assertTrue(scroller.rawScroll > 0.0, "the list should have scrolled to the last item")
    }

    @Test
    fun `scroll containers that omit the scroller prop do not share one`() {
        val screen = buildScreen {
            Column(Modifier.size(200.px, 200.px)) {
                LazyColumn(Modifier.size(200.px, 100.px)) {
                    repeat(50) { i ->
                        Box(Modifier.size(200.px, 20.px), id = "a$i") { TextFlatten { "a$i".emit() } }
                    }
                }
                LazyColumn(Modifier.size(200.px, 100.px)) {
                    repeat(50) { i ->
                        Box(Modifier.size(200.px, 20.px), id = "b$i") { TextFlatten { "b$i".emit() } }
                    }
                }
            }
        }
        fun list(index: Int): DslComponent = screen.children[0].children[index]
        fun builtIndices(component: DslComponent) =
            component.children.withIndex().filter { it.value.children.isNotEmpty() }.map { it.index }

        assertTrue(builtIndices(list(0)).all { it < 10 }, "only the first items should have been built")
        assertTrue(builtIndices(list(1)).all { it < 10 }, "only the first items should have been built")

        // Bring a far-away item into view the way a keyboard focus change would.
        list(0).globalFocusChanged(list(0).children[30].identity, FocusSource.KEYBOARD)
        TestBackend.renderFrame(screen)

        assertTrue(
            builtIndices(list(0)).any { it >= 20 },
            "the list owning the focused item should have scrolled (built ${builtIndices(list(0))})"
        )
        assertTrue(
            builtIndices(list(1)).all { it < 10 },
            "the sibling list must not have inherited the scroll (built ${builtIndices(list(1))})"
        )
    }

    @Test
    fun `scrollTo MID lands between the two ends`() {
        lateinit var scrollerProp: StableRW<Scroller>
        buildScreen {
            scrollerProp = local { Scroller.empty }
            ScrollableColumn(Modifier.size(200.px, 100.px), scrollerProp, local.animatable { 0.0 }) {
                repeat(10) { i ->
                    Box(Modifier.size(200.px, 20.px)) { TextFlatten { "item $i".emit() } }
                }
            }
        }
        val scroller = scrollerProp.value

        scroller.scrollTo(Align.LOW)
        val low = scroller.rawScroll
        scroller.scrollTo(Align.HIGH)
        val high = scroller.rawScroll
        scroller.scrollTo(Align.MID)
        val mid = scroller.rawScroll

        assertEquals(0.0, low)
        assertTrue(high > low, "HIGH should scroll to the end")
        assertTrue(mid > low && mid < high, "MID should land between the two ends (low=$low mid=$mid high=$high)")
    }
}
