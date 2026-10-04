package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.stable
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import kotlin.test.*
import kotlin.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslPropertyTest {

    @BeforeEach
    fun setUp() { io.github.u2894638479.kotlinmcui.backend.dslBackendProvider = { TestBackend } }

    @AfterEach
    fun tearDown() { io.github.u2894638479.kotlinmcui.backend.dslBackendProvider = null }

    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("test", function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    @Test
    fun `a zero-length animation lands on its target in the same frame`() {
        var observed: Double? = null
        buildScreen {
            var value by local.animatable(Duration.ZERO) { 0.0 }
            value = 5.0
            observed = value
        }
        assertEquals(5.0, observed, "a zero-length animation must not interpolate")
    }

    @Test
    fun `closing a screen runs cleanup registered on the stable scope`() {
        var disposed = false
        val screen = buildScreen {
            stable.dispose { disposed = true }
        }
        assertFalse(disposed, "cleanup must not run while the screen is alive")
        screen.close()
        assertTrue(disposed, "closing the screen should run stable cleanup")
    }

    @Test
    fun `the same explicit id at two call sites keeps two values apart`() {
        var first: Any? = null
        var second: Any? = null
        buildScreen {
            val a = local("shared") { 1 }
            val b = local("shared") { "text" }
            first = a.value
            second = b.value
        }
        assertEquals(1, first, "the integer declaration must keep its own slot")
        assertEquals("text", second, "the string declaration must keep its own slot")
    }

    @Test
    fun `a cached property does not take over a plain property slot`() {
        var plainValue: Any? = null
        var cachedValue: Any? = null
        buildScreen {
            val plain = local("slot") { 7 }
            val doubled = local.cached(1, "slot") { it * 2 }
            plainValue = plain.value
            cachedValue = doubled.value
        }
        assertEquals(7, plainValue, "the plain property must survive a cached declaration on the same id")
        assertEquals(2, cachedValue)
    }

    @Test
    fun `dispose still registers when a property uses the same id`() {
        var disposed = false
        val screen = buildScreen {
            local("shared") { 1 }
            local.dispose("shared") { disposed = true }
        }
        assertFalse(disposed, "cleanup must not run while the screen is alive")
        screen.close()
        assertTrue(disposed, "cleanup of the other declaration site must still run")
    }
}
