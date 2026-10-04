package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.integration.TestRuntime
import kotlin.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * Which entries exist for which environment, and — the point of the load counters —
 * which classes an environment must never even load.
 *
 * Three things shape the cases below. The client fixture has to stay untouched until
 * the client case runs, which is why "the client class was not loaded" is asserted on
 * [FixtureSilentClient]: reading a fixture's own fields loads it, so a counter can only
 * prove something about a class no other case mentions. The initialisation counters are
 * compared as deltas, because discovery may be repeated. And `DslRuntimeTest` runs
 * before this class with a COMMON platform already installed.
 */
@Order(2)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class EntryEnvironmentTest {

    @BeforeEach
    fun setUp() {
        TestRuntime.install()
    }

    @Test
    @Order(1)
    fun `a common platform loads neither the client nor the server entry`() {
        DslEntryLoader.discover(Environment.COMMON)

        assertEquals(0, FixtureLoads.silentClient, "a client entry must not be loaded on a common platform")
        assertEquals(0, FixtureLoads.server, "a server entry must not be loaded on a common platform")
    }

    @Test
    @Order(2)
    fun `a server platform loads the server entry and never touches the client one`() {
        val serverRuns = FixtureServer.runs

        DslEntryLoader.discover(Environment.SERVER)

        assertEquals(serverRuns + 1, FixtureServer.runs, "the server initialisation must run")
        // Nothing below may mention FixtureSilentClient: reading it would load it.
        assertEquals(0, FixtureLoads.silentClient, "a client entry must not even be loaded on a server")
    }

    @Test
    @Order(3)
    fun `screens are registered in every environment, but registering is not initialising`() {
        DslEntryLoader.discover(Environment.SERVER)

        val gui = DslEntryLoader.entries.first { it.first === FixtureGuiObject }.second
        assertTrue(gui.isGui, "a GUI entry is registered on a server too")
        assertFalse(gui.isClient, "registering a screen is not running a client initialisation")
        assertFalse(gui.isServer)
    }

    @Test
    @Order(4)
    fun `a mixed entry is loaded for its screen while its client initialisation stays unrun`() {
        val clientRuns = FixtureMixed.clientRuns

        DslEntryLoader.discover(Environment.SERVER)

        assertEquals(1, FixtureLoads.mixed, "the GUI service file names it, so it is loaded")
        assertEquals(clientRuns, FixtureMixed.clientRuns, "its initializeClient must not run on a server")
    }

    @Test
    @Order(5)
    fun `an entry listed under an interface it does not implement is not registered as one`() {
        DslEntryLoader.discover(Environment.SERVER)

        val listed = DslEntryLoader.entries.first { it.first::class == FixtureGuiClass::class }.second
        assertFalse(listed.isServer, "FixtureGuiClass is listed in the server file but is not a DslEntryServer")
    }

    @Test
    @Order(6)
    fun `a client platform loads and initialises the client entries, and only those`() {
        val serverRuns = FixtureServer.runs
        val clientRuns = FixtureClient.runs

        DslEntryLoader.discover(Environment.CLIENT)

        assertEquals(1, FixtureLoads.silentClient, "the client entry is loaded once a client is running")
        assertEquals(1, FixtureSilentClient.runs, "and its initialisation runs")
        assertEquals(clientRuns + 1, FixtureClient.runs)
        assertEquals(1, FixtureMixed.clientRuns, "the mixed entry initialises on a client")
        assertEquals(serverRuns, FixtureServer.runs, "a client platform must not run the server initialisation")
    }

    @Test
    @Order(7)
    fun `a wrong listing is skipped without aborting the rest of the pass`() {
        val clientRuns = FixtureSilentClient.runs

        DslEntryLoader.discover(Environment.CLIENT)

        // FixtureCommon is listed first in the client file but is not a DslEntryClient,
        // and it is already in the instance cache from the common pass.
        val common = DslEntryLoader.entries.first { it.first === FixtureCommon }.second
        assertFalse(common.isClient, "FixtureCommon is listed in the client file but is not a DslEntryClient")
        assertEquals(clientRuns + 1, FixtureSilentClient.runs, "the entries listed after it are still loaded")
    }
}
