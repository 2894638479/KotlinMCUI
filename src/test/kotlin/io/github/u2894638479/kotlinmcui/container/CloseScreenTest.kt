package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.context.closeScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.executeContext
import io.github.u2894638479.kotlinmcui.dsl.stable
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Closing a screen from the DSL must be the same operation the backend performs.
 *
 * `closeScreen()` used to call only `onClose`, so the screen's `stable`/`local` entries were never
 * released: their cleanups did not run and - since a property owns the coroutines its initialiser starts -
 * those coroutines kept running. One closing path is what this pins down.
 */
class CloseScreenTest {

    private fun render(screen: DslScreen) =
        kotlin.context(TestBackend, Unit, Position(0.px, 0.px)) { screen.render<Unit>() }

    @Test
    fun `closeScreen from the DSL releases the screen's properties and cancels their coroutines`() {
        val root = TestRoot.newRoot()
        var cleanups = 0
        var backendTold = 0
        var job: Job? = null
        var close: (() -> Unit)? = null

        val body: DslFunction = {
            stable.dispose("cleanup") { cleanups++ }
            stable { job = launch { awaitCancellation() } }
            val ctx = executeContext
            close = { kotlin.context(ctx) { closeScreen() } }
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        val screen = DslDataStore(TestPlatform, root, "close", { backendTold++ }, body).dslScreen
        root.children.collect(screen)
        render(screen)

        assertNotNull(job)
        assertTrue(job.isActive, "the coroutine runs while the screen is open")
        assertEquals(0, cleanups)

        close?.invoke() ?: error("the page must have captured closeScreen")

        assertEquals(1, cleanups, "the stable entry's cleanup must run when the DSL closes the screen")
        assertFalse(job.isActive, "and the coroutine its scope owned must be cancelled")
        assertEquals(1, backendTold, "the backend is told exactly once")
    }
}
