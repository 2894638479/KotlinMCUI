package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatformUtils
import io.github.u2894638479.kotlinmcui.component.*
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.widget.Presets
import kotlin.test.*

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslTreeTest {

    private var previousBackendProvider: (() -> DslPlatformUtils)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() {
        TestRuntime.restore(previousBackendProvider)
    }

    /** Build a screen from a DSL function, run one frame, return the screen. */
    private fun buildScreen(title: String = "test", function: DslFunction): DslScreen {
        val screen = TestBackend.create(title, function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    @Test
    fun `empty Box has no children`() {
        val screen = buildScreen {
            Box { /* empty */ }
        }
        assertEquals(1, screen.children.size)
        assertTrue(screen.children[0].children.isEmpty())
    }

    @Test
    fun `Box contains one child`() {
        val screen = buildScreen {
            Box {
                Spacer(Modifier.size(10.px, 10.px), id = "spacer")
            }
        }
        assertEquals(1, screen.children[0].children.size)
    }

    @Test
    fun `Row contains three children`() {
        val screen = buildScreen {
            Row {
                Spacer(Modifier.size(10.px, 10.px), id = "a")
                Spacer(Modifier.size(10.px, 10.px), id = "b")
                Spacer(Modifier.size(10.px, 10.px), id = "c")
            }
        }
        assertEquals(3, screen.children[0].children.size)
    }

    @Test
    fun `Column contains nested Box and Row`() {
        val screen = buildScreen {
            Column {
                Box {
                    Spacer(Modifier.size(5.px, 5.px), id = "inner")
                }
                Row {
                    Spacer(Modifier.size(10.px, 10.px), id = "row-child")
                }
            }
        }
        val column = screen.children[0]
        assertEquals(2, column.children.size)
        assertEquals(1, column.children[0].children.size)
        assertEquals(1, column.children[1].children.size)
    }

    @Test
    fun `deeply nested tree has correct structure`() {
        val screen = buildScreen {
            Box {
                Box {
                    Box {
                        Box {
                            Spacer(Modifier.size(1.px, 1.px), id = "deep")
                        }
                    }
                }
            }
        }
        var component: DslComponent = screen.children[0]
        for (depth in 1..4) {
            assertEquals(1, component.children.size, "depth $depth should have 1 child")
            component = component.children[0]
        }
        assertEquals(0, component.children.size)
    }

    @Test
    fun `conditional component present when true`() {
        val screen = buildScreen {
            Box {
                if (true) Spacer(Modifier.size(10.px, 10.px), id = "present")
            }
        }
        assertEquals(1, screen.children[0].children.size)
    }

    @Test
    fun `conditional component absent when false`() {
        val screen = buildScreen {
            Box {
                if (false) Spacer(Modifier.size(10.px, 10.px), id = "absent")
            }
        }
        assertEquals(0, screen.children[0].children.size)
    }

    @Test
    fun `forEach generates correct number of children`() {
        val screen = buildScreen {
            Column {
                for (i in 1..5) {
                    Spacer(Modifier.size(10.px, 10.px), id = i)
                }
            }
        }
        assertEquals(5, screen.children[0].children.size)
    }

    @Test
    fun `decorator wraps component via change`() {
        val screen = buildScreen {
            Box {
                Presets.Button("click") {}.clickable { /* no-op */ }
            }
        }
        assertEquals(1, screen.children.size)
    }

    @Test
    fun `overlay creates floating component`() {
        val screen = buildScreen {
            Box {
                Overlay(Modifier, OverlayAlign.FixRect(Rect(0.px, 0.px, 100.px, 100.px))) {
                    Presets.Text { "overlay" }
                }
            }
        }
        assertTrue(screen.children.isNotEmpty())
    }

    @Test
    fun `children have distinct identities in nested scopes`() {
        val screen = buildScreen {
            Box {
                Spacer(Modifier.size(10.px, 10.px), id = "a")
                Box {
                    Spacer(Modifier.size(10.px, 10.px), id = "b")
                }
            }
        }
        val box = screen.children[0]
        val spacer1 = box.children[0]
        val innerBox = box.children[1]
        val spacer2 = innerBox.children[0]
        assertNotEquals(spacer1.identity, spacer2.identity)
    }

    @Test
    fun `withId scopes identity correctly`() {
        val screen = buildScreen {
            Box {
                withId("scope1") {
                    Spacer(Modifier.size(10.px, 10.px), id = "s")
                }
                withId("scope2") {
                    Spacer(Modifier.size(10.px, 10.px), id = "s")
                }
            }
        }
        val box = screen.children[0]
        assertEquals(2, box.children.size)
        assertNotEquals(box.children[0].identity, box.children[1].identity)
    }
}
