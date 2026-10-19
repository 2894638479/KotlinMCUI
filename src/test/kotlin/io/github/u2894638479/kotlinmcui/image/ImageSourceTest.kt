package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The source strings are data, not assertions.
 *
 * An image location can be typed into a text field, read from a config file or sent by a server. A wrong
 * one must degrade - the backend draws its placeholder, or answers empty - and never throw while a frame
 * is being built.
 */
class ImageSourceTest {
    @Test
    fun `a malformed resource location is carried, not rejected`() {
        val holder = ImageHolder(ImageSource.Resource("not-a-location"), 16.px, 16.px)

        assertEquals("not-a-location", (holder.source as ImageSource.Resource).location)
    }

    @Test
    fun `a url that is not http is carried, not rejected`() {
        val source = ImageSource.Remote("ftp://example.invalid/icon.png")

        assertEquals("ftp://example.invalid/icon.png", source.url)
    }

    @Test
    fun `an empty holder has no source`() {
        assertNull(ImageHolder.empty.source)
        assertTrue(ImageHolder.empty.isEmpty)
    }

    @Test
    fun `a platform without http answers empty instead of throwing`() {
        // The test platform does not override remoteImage, so this runs the interface default. A page
        // that happens to show a remote picture must still render on a backend that cannot fetch it.
        assertTrue(TestPlatform.utils.remoteImage("https://example.invalid/icon.png").isEmpty)
        TestPlatform.utils.reloadRemoteImage("https://example.invalid/icon.png")
    }
}
