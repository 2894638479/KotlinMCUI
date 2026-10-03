package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment
import kotlin.test.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * The framework's own entries are registered from `src/main/resources` as well, so
 * these cases look only at the fixtures declared in `src/test/resources`.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DslEntryLoaderTest {

    // First on purpose: the hooks are meant to run once per JVM, so the counting
    // assertions need the loader untouched by the cases below.
    @Test
    @Order(1)
    fun `initialisation hooks run once however often init is called`() {
        DslEntryLoader.init(Environment.CLIENT)
        assertEquals(1, FixtureCommon.runs)
        assertEquals(1, FixtureClient.runs)

        DslEntryLoader.init(Environment.CLIENT)
        assertEquals(1, FixtureCommon.runs, "a second init must not run the common hook again")
        assertEquals(1, FixtureClient.runs, "a second init must not run the client hook again")
    }

    @Test
    @Order(2)
    fun `entries register whether they are declared as objects or as classes`() {
        DslEntryLoader.init(Environment.CLIENT)
        val registered = DslEntryLoader.entries.map { it.first::class }
        assertTrue(FixtureGuiObject::class in registered, "a Kotlin object entry must register")
        assertTrue(FixtureGuiClass::class in registered, "an entry with a public no-argument constructor must register")
    }

    @Test
    @Order(3)
    fun `flags record how an entry was discovered`() {
        DslEntryLoader.init(Environment.CLIENT)
        val gui = DslEntryLoader.entries.first { it.first === FixtureGuiObject }
        assertTrue(gui.second.isGui)
        assertFalse(gui.second.isOverlay)

        val overlay = DslEntryLoader.entries.first { it.first === FixtureOverlay }
        assertTrue(overlay.second.isOverlay)
        assertFalse(overlay.second.isGui)
    }

    @Test
    @Order(4)
    fun `a broken entry does not stop the ones around it`() {
        DslEntryLoader.init(Environment.CLIENT)
        val registered = DslEntryLoader.entries.map { it.first::class }

        // Both broken fixtures sit between two healthy ones in the service file, and
        // the second one fails with an Error instead of an exception.
        assertTrue(FixtureGuiObject::class in registered)
        assertTrue(FixtureGuiClass::class in registered, "an entry after the broken ones must still register")
        assertFalse(FixtureThrowingConstructor::class in registered, "an entry whose constructor fails is skipped")
        assertFalse(FixtureBrokenStaticInit::class in registered, "an entry whose static initialiser fails is skipped")
    }
}
