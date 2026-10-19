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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * How many scopes a life cycle hands out is decided by how its entries die, not by its name.
 *
 * Entries released together (`stable`) share one scope; entries cleared one by one (`local`) each own
 * one. The last case here is the key one: clearing one local entry must cancel only its own coroutine,
 * which is exactly what would break if `local` shared a scope.
 */
class PropertyScopeSharingTest {

    private fun screenOn(root: DslRoot, tag: String, body: DslFunction): DslScreen =
        DslDataStore(TestPlatform, root, tag, {}, body).dslScreen

    private fun render(screen: DslScreen) =
        kotlin.context(TestBackend, Unit, Position(0.px, 0.px)) { screen.render<Unit>() }

    private fun StableRW<*>.entry(): PropertyLifeScope.TimedProperty<*> =
        this as PropertyLifeScope.TimedProperty<*>

    @Test
    fun `two stable properties of one screen share the same scope`() {
        val root = TestRoot.newRoot()
        var first: CoroutineScope? = null
        var second: CoroutineScope? = null

        val screen = screenOn(root, "stable") {
            first = stable("first") { 1 }.entry().scope
            second = stable("second") { 2 }.entry().scope
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        assertNotNull(first)
        assertSame(first, second, "stable entries die together, so they share one scope")
    }

    @Test
    fun `two local properties get their own scopes`() {
        val root = TestRoot.newRoot()
        var first: CoroutineScope? = null
        var second: CoroutineScope? = null

        val screen = screenOn(root, "local") {
            first = local("first") { 1 }.entry().scope
            second = local("second") { 2 }.entry().scope
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        assertNotNull(first)
        assertNotNull(second)
        assertNotSame(first, second, "a local entry is cleared on its own, so it owns its scope")
    }

    @Test
    fun `closing the screen cancels the stable scope the entries share`() {
        val root = TestRoot.newRoot()
        var scope: CoroutineScope? = null
        var firstEntry: PropertyLifeScope.TimedProperty<*>? = null
        var secondEntry: PropertyLifeScope.TimedProperty<*>? = null
        var first: Job? = null
        var second: Job? = null

        val screen = screenOn(root, "close") {
            val a = stable("a") { launch { awaitCancellation() } }
            val b = stable("b") { launch { awaitCancellation() } }
            val entryA = a.entry()
            val entryB = b.entry()
            firstEntry = entryA
            secondEntry = entryB
            scope = entryA.scope
            first = a.value
            second = b.value
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        assertNotNull(scope)
        val runningFirst = assertNotNull(first)
        val runningSecond = assertNotNull(second)
        assertTrue(runningFirst.isActive, "the coroutines run while the screen is open")
        assertTrue(runningSecond.isActive)

        screen.close()

        assertFalse(runningFirst.isActive, "releasing the screen cancels the scope its entries shared")
        assertFalse(runningSecond.isActive, "the second clear only repeats the same idempotent cancel")
        assertNull(firstEntry?.scope)
        assertNull(secondEntry?.scope)
    }

    @Test
    fun `clearing an entry drops its scope reference`() {
        val root = TestRoot.newRoot()
        var declared = true
        var entry: PropertyLifeScope.TimedProperty<*>? = null

        val screen = screenOn(root, "release") {
            if(declared) entry = local("only") { 1 }.entry()
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        val cleared = assertNotNull(entry)
        assertNotNull(cleared.scope, "an alive entry has a scope")

        declared = false
        render(screen) // frame end clears the entry that was not read

        assertNull(cleared.scope, "onClear releases the reference, so nothing can launch into a dead scope")
    }

    @Test
    fun `clearing one local entry cancels only its own coroutine`() {
        val root = TestRoot.newRoot()
        var keep: Job? = null
        var drop: Job? = null
        var dropping = true

        val screen = screenOn(root, "local") {
            keep = local("keep") { launch { awaitCancellation() } }.value
            if(dropping) drop = local("drop") { launch { awaitCancellation() } }.value
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        val kept = assertNotNull(keep)
        val dropped = assertNotNull(drop)
        assertTrue(kept.isActive)
        assertTrue(dropped.isActive)

        dropping = false
        render(screen) // the stale entry alone is cleared at the end of the frame

        assertFalse(dropped.isActive, "the cleared entry's own scope is cancelled")
        assertTrue(kept.isActive, "a sibling local entry keeps its own scope: local scopes are per entry")
    }

    @Test
    fun `retiring one cached entry does not cancel the scope its siblings share`() {
        val root = TestRoot.newRoot()
        var cacheKey = 1
        var job: Job? = null

        val screen = screenOn(root, "cached") {
            job = stable.cached(cacheKey) { launch { awaitCancellation() } }.value
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        val running = assertNotNull(job)
        assertTrue(running.isActive)

        cacheKey = 2
        render(screen) // the old entry is retired alone; its siblings still use the shared scope

        assertTrue(running.isActive, "a single entry's retirement must not cancel the scope siblings share")
    }
}
