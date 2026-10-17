package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.context.mixed
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.dsl.ui.LateBox
import io.github.u2894638479.kotlinmcui.dsl.withColor
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslColorScopeTest {

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
    fun `withColor reaches a container body declared inside the scope`() {
        var observed: Color? = null
        buildScreen {
            withColor(Color.RED) {
                Box { observed = Color.WHITE.mixed }
            }
        }
        assertEquals(Color.RED, observed, "the container body must see the colour scope it was declared in")
    }

    @Test
    fun `withColor reaches a LateBox body`() {
        var observed: Color? = null
        buildScreen {
            withColor(Color.GREEN) {
                LateBox(Modifier.size(10.px, 10.px)) { observed = Color.WHITE.mixed }
            }
        }
        assertEquals(Color.GREEN, observed, "LateBox builds its content later and must keep the scope")
    }

    @Test
    fun `the colour scope is restored after a container`() {
        var inside: Color? = null
        var after: Color? = null
        buildScreen {
            withColor(Color.RED) {
                Box { inside = Color.WHITE.mixed }
            }
            after = Color.WHITE.mixed
        }
        assertEquals(Color.RED, inside)
        assertEquals(Color.WHITE, after, "leaving the scope must restore the outer colour")
    }
}
