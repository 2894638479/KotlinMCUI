package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

/**
 * A reference to an image resource identified by [id].
 *
 * ### Sizing
 * [width] and [height] should be the image's own dimensions in pixels.
 * If the actual texture size is unknown (e.g. resource packs may use 16×16,
 * 64×64, or higher), it is sufficient to provide the correct **aspect ratio**
 * — the framework scales images similarly to Minecraft's item/block textures.
 *
 * [empty] is a sentinel representing no image.
 */
data class ImageHolder(
    /** The image resource identifier (e.g. a namespace-qualified path). */
    val id: String,
    /** The image width in pixels, or a proportional value for aspect ratio. */
    val width: Measure,
    /** The image height in pixels, or a proportional value for aspect ratio. */
    val height: Measure
) {
    val isEmpty get() = this === empty
    companion object {
        /** A sentinel representing no image. */
        val empty = ImageHolder("",0.px,0.px)
    }
}