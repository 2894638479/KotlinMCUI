package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.DslRuntime
import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.backend.dslPlatformProvider
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.integration.TestRuntime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The second stage of the entry points: `launch*` runs when the platform is published, common first.
 *
 * The platform is published by `DslRuntime.start`; these cases set the provider directly and put it back,
 * so they do not have to run the whole install sequence.
 */
class EntryLaunchTest {
    private var previous: (() -> io.github.u2894638479.kotlinmcui.backend.DslPlatform)? = null

    @BeforeTest
    fun setUp() {
        // discover() reads the stored overlay list, which needs installed metadata; the platform itself
        // is hidden again so the "not published" check is the real one.
        TestRuntime.install()
        previous = dslPlatformProvider
        dslPlatformProvider = null
        FixtureLaunchStats.calls.clear()
    }

    @AfterTest
    fun tearDown() {
        dslPlatformProvider = previous
        FixtureLaunchStats.calls.clear()
    }

    private fun publishPlatform() {
        dslPlatformProvider = { TestPlatform }
    }

    /**
     * Publishing a platform is not the same fact as "the game is ready": this is why `launch` is an
     * explicit call and not a side effect of `start`.
     */
    @Test
    fun `start does not run the launch hooks`() {
        DslEntryLoader.discover(Environment.CLIENT)
        FixtureLaunchStats.calls.clear()

        DslRuntime.start { TestPlatform }

        assertEquals(emptyList(), FixtureLaunchStats.calls, "publishing a platform must not launch the entries")

        DslRuntime.launch()

        assertEquals(listOf("common", "client"), FixtureLaunchStats.calls, "the explicit call is what launches them")
    }

    @Test
    fun `launch before the platform is published is an error`() {
        DslEntryLoader.discover(Environment.NONE)

        val failure = assertFailsWith<IllegalStateException> { DslEntryLoader.launch() }

        assertTrue(
            "before the platform was published" in failure.message.orEmpty(),
            "the failure must name the missing step, was: ${failure.message}",
        )
    }

    @Test
    fun `the none environment launches only the common entries`() {
        DslEntryLoader.discover(Environment.NONE)
        publishPlatform()

        DslEntryLoader.launch()

        assertEquals(listOf("common"), FixtureLaunchStats.calls)
    }

    @Test
    fun `the client environment launches common before the client`() {
        DslEntryLoader.discover(Environment.CLIENT)
        publishPlatform()

        DslEntryLoader.launch()

        assertEquals(listOf("common", "client"), FixtureLaunchStats.calls)
    }

    @Test
    fun `the server environment launches common before the server`() {
        DslEntryLoader.discover(Environment.SERVER)
        publishPlatform()

        DslEntryLoader.launch()

        assertEquals(listOf("common", "server"), FixtureLaunchStats.calls)
    }

    @Test
    fun `launch is idempotent`() {
        DslEntryLoader.discover(Environment.CLIENT)
        publishPlatform()

        DslEntryLoader.launch()
        val afterFirst = FixtureLaunchStats.calls.toList()
        DslEntryLoader.launch()

        assertEquals(afterFirst, FixtureLaunchStats.calls, "the second call must not run the hooks again")
    }
}
