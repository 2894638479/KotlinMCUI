package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.integration.TestRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

/**
 * The triangle scratch buffers belong to a root, not to the process.
 *
 * This is the structural half of the reasons the buffers moved: two roots can render on two threads at
 * the same moment, so one shared pair would let one root's fill land between another root's fill and its
 * `renderTriangles` call. Whether two threads actually interleave badly is not asserted here - a test
 * that bets on timing would be a flaky instrument; the ownership below is the guarantee.
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
