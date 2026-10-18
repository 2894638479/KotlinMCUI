package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.backend.dslPlatformProvider
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.integration.TestRuntime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The overlay list: what is enabled, in what order, and that the order survives a restart.
 *
 * These three functions (`setOverlayEnabled`, `bringOverlayToTop`, and the stored id list behind them)
 * predate the entry-model merge and were not changed by it - but a test that keeps them honest is what
 * makes a future change to the page (or to the loader) fail loudly instead of silently reordering a
 * user's overlays.
 */
class OverlayOrderTest {
    private var previous: (() -> io.github.u2894638479.kotlinmcui.backend.DslPlatform)? = null

    @BeforeTest
    fun setUp() {
        TestRuntime.install()
        previous = dslPlatformProvider
        dslPlatformProvider = null
        DslEntryLoader.discover(Environment.STANDALONE)
        // Start from a known stored list.
        writeOverlayIds(emptyList())
        DslEntryLoader.discover(Environment.STANDALONE)
    }

    @AfterTest
    fun tearDown() {
        writeOverlayIds(emptyList())
        dslPlatformProvider = previous
    }

    private fun overlay(className: String): DslEntryOverlay =
        DslEntryLoader.entries.first { it.first::class.java.simpleName == className }.first as DslEntryOverlay

    @Test
    fun `enabling and disabling an overlay changes the enabled list`() {
        val first = overlay("FixtureOverlay")
        assertTrue(DslEntryLoader.enabledOverlays.isEmpty(), "nothing is enabled in a fresh config")

        DslEntryLoader.setOverlayEnabled(first, true)
        assertEquals(listOf(first), DslEntryLoader.enabledOverlays)

        DslEntryLoader.setOverlayEnabled(first, false)
        assertTrue(DslEntryLoader.enabledOverlays.isEmpty())
    }

    @Test
    fun `bringing an overlay to the top moves it and stores the new order`() {
        val first = overlay("FixtureOverlay")
        val second = overlay("FixtureOverlayTwo")
        DslEntryLoader.setOverlayEnabled(first, true)
        DslEntryLoader.setOverlayEnabled(second, true)
        assertEquals(listOf(first, second), DslEntryLoader.enabledOverlays, "the enable order is the initial order")

        DslEntryLoader.bringOverlayToTop(second)

        assertEquals(listOf(second, first), DslEntryLoader.enabledOverlays, "the stack order changed")
        assertEquals(
            listOf(second.id, first.id),
            readOverlayIds(),
            "and what was written must be the same order, not just the in-memory list",
        )

        // A restart reads the stored list back: same order, without the in-memory state.
        DslEntryLoader.discover(Environment.STANDALONE)
        assertEquals(
            listOf(second, first),
            DslEntryLoader.enabledOverlays,
            "the stored order must survive a reload",
        )
    }
    /**
     * Two mods may pick the same overlay id. Declaring first wins, and the later one is reported rather than
     * silently taking the id: a silent overwrite would enable an implementation the author did not declare.
     */
    @Test
    fun `a duplicate overlay id keeps the first and reports the second`() {
        val first = overlay("FixtureOverlay")
        val duplicate = DslEntryLoader.brokenEntries
            .firstOrNull { it.className.endsWith("FixtureOverlayDup") }
        assertTrue(duplicate != null, "the later declaration must be reported")
        assertTrue(
            duplicate!!.reason.contains("FixtureOverlay"),
            "and the reason must name the winner, was: " + '$' + "{duplicate.reason}" + "",
        )
        DslEntryLoader.setOverlayEnabled(first, true)
        assertEquals(listOf(first), DslEntryLoader.enabledOverlays, "only the winner can be enabled")
    }
}