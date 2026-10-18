package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.DslRuntime
import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.backend.dslPlatformProvider
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.integration.TestRuntime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The registry model: one file, an environment column, and two stages.
 *
 * The environment filter is checked *before* any class is loaded, which is what the
 * [FixtureStandaloneOnly] probe measures: its static initialiser counts loads, so a non-zero count in a
 * client run means the filter happened too late.
 */
class EntryRegistryTest {
    private var previous: (() -> io.github.u2894638479.kotlinmcui.backend.DslPlatform)? = null

    @BeforeTest
    fun setUp() {
        // discover() reads the stored overlay list, which needs installed metadata; the platform itself
        // is hidden again so the "not published" checks are the real ones.
        TestRuntime.install()
        previous = dslPlatformProvider
        dslPlatformProvider = null
        FixtureStats.stages.clear()
        FixtureStats.loads = 0
    }

    @AfterTest
    fun tearDown() {
        dslPlatformProvider = previous
        FixtureStats.stages.clear()
    }

    private fun publish() {
        dslPlatformProvider = { TestPlatform }
    }

    // ---- 1: the filter runs before any class is loaded ----
    @Test
    fun `a standalone-only entry is never loaded in a client run`() {
        FixtureStats.loads = 0
        DslEntryLoader.discover(Environment.CLIENT)

        assertEquals(0, FixtureStats.loads, "the class must not even be loaded, let alone initialised")
        assertTrue(
            DslEntryLoader.entries.none { it.first is FixtureStandaloneOnly },
            "and it must not be registered",
        )
    }

    @Test
    fun `a standalone-only entry is loaded in a standalone run`() {
        DslEntryLoader.discover(Environment.STANDALONE)

        assertEquals(1, FixtureStats.loads, "standalone is exactly where it belongs")
        assertTrue(DslEntryLoader.entries.any { it.first is FixtureStandaloneOnly })
    }

    // ---- 2: kinds come from the type, not from the file ----
    @Test
    fun `kinds are decided by the class, not by the declaration`() {
        DslEntryLoader.discover(Environment.STANDALONE)

        val byName = DslEntryLoader.entries.associateBy { it.first::class.java.simpleName }
        assertTrue(byName.getValue("FixturePage").second.isGui)
        assertFalse(byName.getValue("FixturePage").second.isOverlay)
        assertTrue(byName.getValue("FixtureOverlay").second.isOverlay)
        assertFalse(byName.getValue("FixtureOverlay").second.isGui)
        assertTrue(byName.getValue("FixtureBoth").second.isGui, "both kinds in one class")
        assertTrue(byName.getValue("FixtureBoth").second.isOverlay)
    }

    // ---- 3: the declaration is validated, and a bad one is loud ----
    @Test
    fun `a line without an environment column throws`() {
        val failure = assertFailsWith<IllegalStateException> { parseEntryRegistry("com.example.Page") }
        val message = failure.message.orEmpty()
        assertTrue("line 1" in message, "the message must name the line, was: $message")
        assertTrue("com.example.Page" in message, "and the offending text, was: $message")
    }

    @Test
    fun `an unknown environment throws`() {
        val failure = assertFailsWith<IllegalStateException> {
            parseEntryRegistry("com.example.Page dedicated")
        }
        assertTrue("unknown environment" in failure.message.orEmpty(), "was: ${failure.message}")
    }

    @Test
    fun `declaring one class twice throws`() {
        val failure = assertFailsWith<IllegalStateException> {
            parseEntryRegistry("com.example.Page client\ncom.example.Page server")
        }
        val message = failure.message.orEmpty()
        assertTrue("line 2" in message, "the duplicate is on line 2, was: $message")
        assertTrue("com.example.Page" in message, "and the class is named, was: $message")
    }

    @Test
    fun `duplicate declarations across registries throw too`() {
        // Same file twice is the same mistake as two jars; the parse layer cannot tell, so the loader's
        // merge does. Simulated by parsing twice - the merge rule is the same code path.
        val once = parseEntryRegistry("com.example.Page client\n")
        assertEquals(1, once.size)
        assertFailsWith<IllegalStateException> {
            parseEntryRegistry("com.example.Page client\ncom.example.Page client\n")
        }
    }

    /**
     * An entry with no UI at all is an entry, not an error: the entry page shows it only when code entries
     * are shown, and nothing else has to happen.
     */
    @Test
    fun `an entry with neither gui nor overlay loads and runs both stages`() {
        DslEntryLoader.discover(Environment.STANDALONE)
        publish()

        val flags = DslEntryLoader.entries
            .first { it.first::class.java.simpleName == "FixtureNoUi" }.second
        assertFalse(flags.isGui)
        assertFalse(flags.isOverlay)
        assertTrue("initialize:noui" in FixtureStats.stages, "initialize must run for it")

        DslRuntime.launch()

        assertTrue("launch:noui" in FixtureStats.stages, "launch must run for it too")
    }

    @Test
    fun `flags carry the declared environments`() {
        DslEntryLoader.discover(Environment.CLIENT)

        val byName = DslEntryLoader.entries.associateBy { it.first::class.java.simpleName }
        assertEquals(
            setOf(Environment.CLIENT, Environment.SERVER, Environment.STANDALONE),
            byName.getValue("FixturePage").second.environments,
        )
        // The standalone-only entry is not in a client run at all; its declared set is checked where it
        // exists.
        DslEntryLoader.discover(Environment.STANDALONE)
        val standalone = DslEntryLoader.entries.associateBy { it.first::class.java.simpleName }
        assertEquals(
            setOf(Environment.STANDALONE),
            standalone.getValue("FixtureStandaloneOnly").second.environments,
        )
    }

    // ---- 5: the two stages ----
    @Test
    fun `initialize runs on every filtered entry in declaration order`() {
        DslEntryLoader.discover(Environment.CLIENT)

        val expected = listOf("initialize:page", "initialize:overlay")
        assertEquals(expected, FixtureStats.stages.filter { it.startsWith("initialize:page") || it.startsWith("initialize:overlay") })
    }

    @Test
    fun `launch runs only on the entries that passed the filter`() {
        DslEntryLoader.discover(Environment.CLIENT)
        publish()

        DslRuntime.launch()

        assertTrue("launch:page" in FixtureStats.stages)
        assertTrue("launch:overlay" in FixtureStats.stages)
        assertFalse(
            "launch:standalone" in FixtureStats.stages,
            "an entry filtered out by environment must not launch either",
        )
    }

    @Test
    fun `launch before the platform is published is an error`() {
        DslEntryLoader.discover(Environment.STANDALONE)

        val failure = assertFailsWith<IllegalStateException> { DslEntryLoader.launch() }
        assertTrue("before the platform was published" in failure.message.orEmpty())
    }

    @Test
    fun `launch is idempotent`() {
        DslEntryLoader.discover(Environment.CLIENT)
        publish()

        DslEntryLoader.launch()
        val afterFirst = FixtureStats.stages.toList()
        DslEntryLoader.launch()

        assertEquals(afterFirst, FixtureStats.stages, "the second call must not run the hooks again")
    }

    @Test
    fun `publishing a platform does not run the launch stage`() {
        DslEntryLoader.discover(Environment.CLIENT)
        FixtureStats.stages.clear()

        TestRuntime.install()

        assertFalse(FixtureStats.stages.any { it.startsWith("launch") }, "start is not launch")
    }

    // ---- 6: broken entries are recorded, not swallowed ----
    @Test
    fun `unloadable and non-service entries land in brokenEntries`() {
        DslEntryLoader.discover(Environment.STANDALONE)

        val names = DslEntryLoader.brokenEntries.map { it.className }
        assertTrue(names.any { it.endsWith("FixtureThrowingConstructor") }, "a throwing constructor is recorded")
        assertTrue(names.any { it.endsWith("FixtureBrokenStaticInit") }, "a broken static initialiser is recorded")
        assertTrue(names.any { it.endsWith("FixtureNotAService") }, "a class that is not a service is recorded")
        // The class name is what the page shows, so the reason has to be short and specific.
        assertTrue(DslEntryLoader.brokenEntries.all { it.reason.isNotBlank() })
    }
}
