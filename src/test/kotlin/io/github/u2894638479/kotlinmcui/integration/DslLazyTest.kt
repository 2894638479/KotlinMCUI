package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.scroll.Scroller
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslLazyTest {

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
    fun `LazyColumn builds without crash`() {
        val screen = buildScreen {
            LazyColumn(Modifier.size(200.px, 300.px)) {
                for (i in 1..10) Spacer(Modifier.size(200.px, 30.px), id = i)
            }
        }
        assertTrue(screen.children.isNotEmpty())
    }

    @Test
    fun `LazyRow builds without crash`() {
        val screen = buildScreen {
            LazyRow(Modifier.size(300.px, 50.px)) {
                for (i in 1..10) Spacer(Modifier.size(40.px, 50.px), id = i)
            }
        }
        assertTrue(screen.children.isNotEmpty())
    }

    @Test
    fun `LazyColumn with many items builds without crash`() {
        val screen = buildScreen {
            LazyColumn(Modifier.size(200.px, 100.px)) {
                for (i in 1..100) Spacer(Modifier.size(200.px, 30.px), id = i)
            }
        }
        val lazy = screen.children[0]
        assertEquals(100, lazy.children.size, "all children should exist in the tree")
    }

    @Test
    fun `LazyRow with many items builds without crash`() {
        val screen = buildScreen {
            LazyRow(Modifier.size(100.px, 50.px)) {
                for (i in 1..100) Spacer(Modifier.size(40.px, 50.px), id = i)
            }
        }
        val lazy = screen.children[0]
        assertEquals(100, lazy.children.size, "all children should exist in the tree")
    }

    @Test
    fun `nested LazyColumn in LazyColumn builds`() {
        val screen = buildScreen {
            LazyColumn(Modifier.size(200.px, 200.px)) {
                for (i in 1..10) {
                    LazyColumn(Modifier.size(200.px, 60.px)) {
                        for (j in 1..5) Spacer(Modifier.size(200.px, 30.px), id = "$i-$j")
                    }
                }
            }
        }
        assertTrue(screen.children.isNotEmpty())
    }

    @Test
    fun `LazyColumn with conditional children`() {
        var toggle = true
        val screen = buildScreen {
            LazyColumn(Modifier.size(200.px, 100.px)) {
                if (toggle) Spacer(Modifier.size(200.px, 30.px), id = "present")
                else Spacer(Modifier.size(200.px, 30.px), id = "absent")
            }
        }
        assertEquals(1, screen.children[0].children.size)
    }

    @Test
    fun `LazyRow with scissor contains rendering`() {
        val screen = buildScreen {
            LazyRow(Modifier.size(200.px, 50.px)) {
                for (i in 1..10) Spacer(Modifier.size(100.px, 50.px), id = i)
            }
        }
        assertTrue(screen.children.isNotEmpty())
    }

    @Test
    fun `LazyColumn with ScrollBar builds`() {
        val screen = buildScreen {
            Row {
                val scrollerProp = local { Scroller.empty }
                LazyColumn(Modifier.size(200.px, 100.px), scrollerProp) {
                    for (i in 1..20) Spacer(Modifier.size(200.px, 30.px), id = i)
                }
                ScrollBarVertical(Modifier.width(10.px), scrollerProp) {}
            }
        }
        assertTrue(screen.children.isNotEmpty())
    }
}
