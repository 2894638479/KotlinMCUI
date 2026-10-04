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
 * prove something about a class no other case mentions. The initialisation counters move
 * on every pass, so they are compared as deltas — an absolute count would make this class
 * depend on the classes that happen to run before it. And discovery always reads the GUI
 * service file, so a load counter can only ever say "the first pass that named this class
 * was an earlier one, not this one"; the case that needs the positive statement asserts
 * on the registered entry instead.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class EntryEnvironmentTest {

    @BeforeEach
    fun setUp() {
        TestRuntime.install()
    }

    @Test
    @Order(1)
    fun `a common platform loads neither the client nor the server entry`() {
        val silentClientLoads = FixtureLoads.silentClient
        val serverRuns = FixtureServer.runs

        DslEntryLoader.discover(Environment.COMMON)

        assertEquals(silentClientLoads, FixtureLoads.silentClient, "a client entry must not be loaded on a common platform")
        assertEquals(serverRuns, FixtureServer.runs, "a server entry must not be initialised on a common platform")
    }

    @Test
    @Order(2)
    fun `a server platform loads the server entry and never touches the client one`() {
        val serverRuns = FixtureServer.runs
        val silentClientLoads = FixtureLoads.silentClient

        DslEntryLoader.discover(Environment.SERVER)

        assertEquals(serverRuns + 1, FixtureServer.runs, "the server initialisation must run")
        // Nothing below may mention FixtureSilentClient: reading it would load it.
        assertEquals(silentClientLoads, FixtureLoads.silentClient, "a client entry must not even be loaded on a server")
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

        // The GUI service file names FixtureMixed, so the entry — not a class-load
        // counter, which a JVM only moves once, on whichever pass ran first — is what
        // proves a server registers its screen and does not initialise it as a client.
        val mixed = DslEntryLoader.entries.first { it.first === FixtureMixed }.second
        assertTrue(mixed.isGui, "the GUI service file names it, so a server registers its screen")
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
        val silentClientRuns = FixtureSilentClient.runs
        val mixedClientRuns = FixtureMixed.clientRuns

        DslEntryLoader.discover(Environment.CLIENT)

        val silentClient = DslEntryLoader.entries.first { it.first === FixtureSilentClient }.second
        assertTrue(silentClient.isClient, "the client entry is registered once a client is running")
        assertEquals(silentClientRuns + 1, FixtureSilentClient.runs, "and its initialisation runs")
        assertEquals(clientRuns + 1, FixtureClient.runs)
        assertEquals(mixedClientRuns + 1, FixtureMixed.clientRuns, "the mixed entry initialises on a client")
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
