package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The IME preview box must cost nothing on a frame without composition.
 *
 * The observable is "was the focused component's preedit area read at all": the box is the only render
 * path that reads it, so zero reads means the early return happened before any tree walk. Counting
 * `testHit` instead would be muddied by the hover/narration probes that legitimately run every frame.
 */
class ImeBoxEarlyExitTest {

    @Test
    fun `a frame without preedit never looks at the focused component`() {
        val root = TestRoot.newRoot()
        var preeditReads = 0
        var targeted: DslChild? = null

        val body: DslFunction = {
            Column {
                val holder = Spacer(Modifier.size(10.px, 10.px), "target")
                targeted = holder.change {
                    object : DslComponent by it {
                        override val preeditArea: Rect?
                            get() {
                                preeditReads++
                                return it.preeditArea
                            }
                    }
                }
            }
        }
        val screen = DslDataStore(TestPlatform, root, "ime", {}, body).dslScreen
        root.children.collect(screen)
        render(screen)

        val component = targeted?.currentComponent() ?: error("the page must have built its component")
        root.setFocus(component.identity)
        preeditReads = 0

        render(screen) // no preedit was ever set: the box must not even search for the component

        assertEquals(0, preeditReads, "the IME box must return on an empty preedit before touching the tree")
    }

    private fun render(screen: DslScreen) =
        kotlin.context(TestBackend, Unit, Position(0.px, 0.px)) { screen.render<Unit>() }
}
