package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.identity.DslId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.*

/**
 * The process-wide `static` storage.
 *
 * It is shared by every root and screen, and after the split different roots can render on different
 * threads - so this is the one map whose type is a correctness statement, not a style choice.
 *
 * Two cases: the structural one (the map is a concurrent container) is the guarantee; the pressure one
 * (two threads writing distinct ids, nothing lost) is evidence at best - a run that passes proves
 * nothing about a race that did not schedule, which is why the first case, not this one, is the thesis.
 */
class StaticPropertyScopeTest {

    @Test
    fun `the process-wide static storage is a concurrent map`() {
        assertIs<ConcurrentHashMap<*, *>>(
            DslDataStore.staticPropertyMap,
            "static slots are shared by every root, so the storage itself must be concurrent",
        )
    }

    /**
     * Pressure, not proof: two threads write [perThread] distinct ids each, [rounds] times over, and
     * nothing may be lost. With the old plain hash map this usually fails; it can also pass while the
     * race is still real, so it is a smoke test on top of the structural case above.
     */
    @Test
    fun `parallel writers of distinct static ids lose nothing`() {
        val threads = 2
        val perThread = 200
        val rounds = 5
        val failures = ConcurrentLinkedQueue<Throwable>()
        val before = DslDataStore.staticIds

        val workers = (0 until threads).map { t ->
            Thread {
                try {
                    repeat(rounds) {
                        repeat(perThread) { i ->
                            DslDataStore.staticPropertyMap[DslId("par-$t-$i")] =
                                PropertyLifeScope.TimedProperty(i)
                        }
                    }
                } catch(e: Throwable) {
                    failures += e
                }
            }
        }
        workers.forEach { it.start() }
        workers.forEach { it.join() }

        assertTrue(failures.isEmpty(), "no writer may throw, was: ${failures.firstOrNull()}")
        val expected = (0 until threads).flatMap { t -> (0 until perThread).map { DslId("par-$t-$it") } }
        val missing = expected.filterNot { it in DslDataStore.staticIds }
        assertTrue(missing.isEmpty(), "every declared id must still be there, missing ${missing.size} of ${expected.size}")

        // Leave the process-wide map as this case found it.
        DslDataStore.staticIds.filterNot { it in before }.forEach { DslDataStore.staticPropertyMap.remove(it) }
    }
}
