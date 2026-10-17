package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.container.DslScope
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.dsl.ui.Scope
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/** The custom component recipe of `docs/advanced/custom.md`, built on [Spacer]. */
context(ctx: DslContext)
private fun ClickableSpacer(modifier: Modifier = Modifier, id: Any, onClick: () -> Unit) =
    Spacer(modifier, id).change {
        object : DslComponent by it {
            override val focusable get() = true

            context(eventModifier: KeyModifiers, mouse: Position)
            override fun mouseDown(mouseButton: MouseButton): Boolean {
                if (mouseButton != MouseButton.LEFT) return it.mouseDown(mouseButton)
                onClick()
                return true
            }
        }
    }

/** Where the counted clicks are read back from. */
private class Counter { var value = 0 }

/** The same recipe, with the state kept in the component's own property scope. */
context(ctx: DslContext)
private fun CountingSpacer(modifier: Modifier = Modifier, id: Any, counter: Counter) =
    Spacer(modifier, id).change {
        object : DslComponent by it {
            var clicks by local { 0 }

            context(eventModifier: KeyModifiers, mouse: Position)
            override fun mouseDown(mouseButton: MouseButton): Boolean {
                if (mouseButton != MouseButton.LEFT) return it.mouseDown(mouseButton)
                clicks += 1
                counter.value = clicks
                return true
            }
        }
    }

/** The custom container recipe of `docs/advanced/custom.md`, built on [Scope]. */
context(ctx: DslContext)
private fun WrappingColumn(
    modifier: Modifier = Modifier,
    id: Any? = null,
    alignerHorizontal: Aligner = Aligner.simplePlace,
    function: DslFunction
) = Scope(modifier, id, alignerHorizontal, Aligner.simplePlace, function).change {
    object : DslScope by it as DslScope {
        override val contentMinWidth by lazy {
            max(instance.childrenMaxWidth, super.contentMinWidth)
        }
    }
}

class DslCustomComponentTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslPlatform)? = null

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
    fun `a Spacer based component replaces the spacer it wraps`() {
        var clicked = false
        val screen = buildScreen {
            ClickableSpacer(Modifier.size(20.px, 20.px), "custom") { clicked = true }
        }
        val child = screen.children.single()
        assertTrue(child.focusable, "the decorator must be the component living in the tree")

        val inside = Position(child.rect.left + 1.px, child.rect.top + 1.px)
        context(KeyModifiers(), inside) {
            assertTrue(child.mouseDown(MouseButton.LEFT), "the override must handle the event")
        }
        assertTrue(clicked)

        context(KeyModifiers(), inside) {
            assertFalse(child.mouseDown(MouseButton.RIGHT), "the delegate must still see other buttons")
        }
    }

    @Test
    fun `a component can keep state of its own`() {
        val counter = Counter()
        val screen = buildScreen {
            CountingSpacer(Modifier.size(20.px, 20.px), "counting", counter)
        }
        val inside = Position(screen.children.single().rect.left + 1.px, screen.children.single().rect.top + 1.px)
        context(KeyModifiers(), inside) {
            assertTrue(screen.children.single().mouseDown(MouseButton.LEFT))
        }
        assertEquals(1, counter.value)

        // The component object is rebuilt every frame; its state must not be.
        TestBackend.renderFrame(screen)
        context(KeyModifiers(), inside) {
            assertTrue(screen.children.single().mouseDown(MouseButton.LEFT))
        }
        assertEquals(2, counter.value, "state declared inside the component must survive a frame")
    }

    @Test
    fun `a Scope based container uses the aligners it was given`() {        val far = buildScreen {
            Scope(Modifier.size(100.px, 100.px), "far", Aligner.close(Align.HIGH), Aligner.close(Align.HIGH)) {
                Box(Modifier.size(10.px, 10.px)) {}
            }
        }
        val farScope = far.children.single()
        val farChild = farScope.children.single()
        assertEquals(farScope.rect.right, farChild.rect.right, "the horizontal aligner must place the child at the high edge")
        assertEquals(farScope.rect.bottom, farChild.rect.bottom, "the vertical aligner must place the child at the high edge")

        val near = buildScreen {
            Scope(Modifier.size(100.px, 100.px), "near", Aligner.close(Align.LOW), Aligner.close(Align.LOW)) {
                Box(Modifier.size(10.px, 10.px)) {}
            }
        }
        val nearScope = near.children.single()
        val nearChild = nearScope.children.single()
        assertEquals(nearScope.rect.left, nearChild.rect.left, "the horizontal aligner must place the child at the low edge")
        assertEquals(nearScope.rect.top, nearChild.rect.top, "the vertical aligner must place the child at the low edge")
    }

    @Test
    fun `a Scope based container can override its own content size`() {
        val screen = buildScreen {
            Scope(Modifier.size(100.px, 100.px), "outer", Aligner.close(Align.LOW), Aligner.close(Align.LOW)) {
                WrappingColumn {
                    Box(Modifier.size(30.px, 20.px)) {}
                }
            }
        }
        val column = screen.children.single().children.single()
        assertEquals(30.px, column.contentMinWidth, "the override must report the wrapped content width")
        assertEquals(
            30.px, column.rect.right - column.rect.left,
            "the overridden minimum must size the container inside a close-aligned parent"
        )
    }
}
