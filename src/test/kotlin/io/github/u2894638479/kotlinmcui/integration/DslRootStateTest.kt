package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.container.DslRootState
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * The root's view state, and the broadcast that writing it drives.
 *
 * `TopComponent` keeps no focus, hover or preedit of its own any more: it projects the main
 * root's [DslRootState]. The rest of the suite reads that state back (`TopComponent.focused`,
 * `isFocused`), but nothing else observes the *notifications* — `globalFocusChanged` and
 * `globalHoverChanged` have no other listener under test — so a dropped callback would leave
 * every other case green while the tree silently stopped hearing about changes. These cases
 * keep the delegation honest, and the per-instance one is the whole reason the state left the
 * process-wide object.
 */
class DslRootStateTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = dslBackendProvider
        dslBackendProvider = { TestBackend }
    }

    @AfterEach
    fun tearDown() {
        // Both writes broadcast over whatever is mounted, so clear them before the provider
        // goes away - and so the next case does not start out hovering this one's component.
        TopComponent.setFocus(null)
        TopComponent.hovered = null
        dslBackendProvider = previousBackendProvider
    }

    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("root-state", function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    /**
     * A frame unmounts the screen it built, and the broadcast walks [TopComponent.children],
     * so the built tree has to be mounted again for the duration of the write.
     */
    private fun <T> mounted(screen: DslScreen, block: () -> T): T {
        val child = TopComponent.children.collect(screen)
        try {
            return block()
        } finally {
            TopComponent.children.remove(child)
        }
    }

    /**
     * Falsifiable by construction: empty the `onFocusChanged` callback in `TopComponent`
     * (`{ _, _ -> }`) and this case fails, because `seen` stays empty.
     */
    @Test
    fun `setFocus reaches the tree with the source it was given`() {
        val seen = mutableListOf<Pair<DslId?, FocusSource>>()
        val screen = buildScreen {
            Spacer(Modifier.size(10.px, 10.px), "root-state").change {
                object : DslComponent by it {
                    override fun globalFocusChanged(newFocus: DslId?, source: FocusSource) {
                        seen += newFocus to source
                    }
                }
            }
        }
        val target = screen.children.single().identity

        mounted(screen) { TopComponent.setFocus(target, FocusSource.MOUSE) }

        assertEquals(listOf<Pair<DslId?, FocusSource>>(target to FocusSource.MOUSE), seen)
    }

    /**
     * The frame already broadcast its own hover (the render pass rewrites the hover every
     * frame, unconditionally), so the observation is cleared first: asserting on the whole
     * history would compare against that earlier value instead of the write under test.
     *
     * Falsifiable the same way as the focus case, by emptying `onHoverChanged`.
     */
    @Test
    fun `hover reaches the tree`() {
        val seen = mutableListOf<DslId?>()
        val screen = buildScreen {
            Spacer(Modifier.size(10.px, 10.px), "root-state").change {
                object : DslComponent by it {
                    override fun globalHoverChanged(newHover: DslId?) {
                        seen += newHover
                    }
                }
            }
        }
        val target = screen.children.single().identity

        seen.clear()
        mounted(screen) { TopComponent.hovered = target }

        assertEquals(listOf<DslId?>(target), seen)
    }

    /** One instance per root is what makes two roots stop overwriting each other's focus. */
    @Test
    fun `two state instances do not share their slots`() {
        val seen = mutableListOf<DslId?>()
        val a = DslRootState(onFocusChanged = { id, _ -> seen += id }, onHoverChanged = {})
        val b = DslRootState(onFocusChanged = { _, _ -> }, onHoverChanged = { _ -> })
        val id = DslId("only-a")

        a.setFocus(id)

        assertEquals(id, a.focused)
        assertNull(b.focused, "a second root's state must not see the first root's focus")
        assertEquals(listOf<DslId?>(id), seen, "the write must notify its own owner")
    }
}
