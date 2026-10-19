package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.integration.TestRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

/**
 * The triangle scratch buffers belong to a root, not to the process: two roots can render on two
 * threads at once, so the test pins ownership rather than betting on timing.
 */
class RootTriangleBufferTest {

    @Test
    fun `each root owns its triangle scratch buffers`() {
        val first = TestRoot.newRoot()
        val second = TestRoot.newRoot()

        assertNotSame(first.triangleVertices, second.triangleVertices, "vertices must not be shared between roots")
        assertNotSame(first.triangleColors, second.triangleColors, "colours must not be shared between roots")
        assertEquals(6, first.triangleVertices.size)
        assertEquals(3, first.triangleColors.size)
    }
}
