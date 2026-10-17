package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A property and the coroutines its initialiser starts share one lifetime.
 *
 * The scope belongs to the property entry (it is cancelled from `onClear`), so what keeps a coroutine
 * alive is the entry being read - and a coroutine that must outlive the property needs a bigger scope,
 * chosen by the author.
 */
class PropertyScopeCancellationTest {

    private fun screenOn(root: DslRoot, tag: String, body: DslFunction): DslScreen =
        DslDataStore(TestPlatform, root, tag, {}, body).dslScreen

    private fun render(screen: DslScreen) =
        kotlin.context(TestBackend, Unit, Position(0.px, 0.px)) { screen.render<Unit>() }

    @Test
    fun `a local property cancels the coroutine it started when it stops being declared`() {
        val root = TestRoot.newRoot()
        var declared = true
        var job: Job? = null

        val screen = screenOn(root, "local") {
            if(declared) { val slot = local { launch { awaitCancellation() } }; job = slot.value }
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        assertNotNull(job, "the initialiser must have run")
        assertTrue(job.isActive, "the coroutine runs while the property is declared")

        declared = false
        render(screen) // frame ends -> the entry is cleared

        assertFalse(job.isActive, "the property's scope is cancelled with it")
    }

    @Test
    fun `a stable property keeps one scope across frames`() {
        val root = TestRoot.newRoot()
        var job: Job? = null

        val screen = screenOn(root, "stable") {
            val slot = stable { launch { awaitCancellation() } }; job = slot.value
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)
        val first = job
        render(screen)
        val second = job

        assertNotNull(first)
        assertSame(first, second, "an initialiser runs once; re-declaring the property must not restart it")
        assertTrue(first.isActive, "and the coroutine is not cancelled on a frame that still declares it")
    }

    @Test
    fun `the stand-alone local coroutine scope is cancelled when its entry is not read`() {
        val root = TestRoot.newRoot()
        var declared = true
        var job: Job? = null

        val screen = screenOn(root, "accessor") {
            if(declared) job = local.coroutineScope.launch { awaitCancellation() }
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        assertNotNull(job)
        assertTrue(job.isActive)

        declared = false
        render(screen)

        assertFalse(job.isActive, "an unread scope entry is cleared, and clearing cancels it")
    }
}
