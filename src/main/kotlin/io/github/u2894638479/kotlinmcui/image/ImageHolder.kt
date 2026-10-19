package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

/**
 * A reference to an image: where it comes from, and how big it is.
 *
 * ### Sizing
 * [width] and [height] should be the image's own dimensions in pixels.
 * If the actual texture size is unknown (e.g. resource packs may use 16×16,
 * 64×64, or higher), it is sufficient to provide the correct **aspect ratio**
 * — the framework scales images similarly to Minecraft's item/block textures.
 *
 * A [ImageSource.LocalFile] or [ImageSource.Remote] holder learns its size only after the pixels
 * arrive, so a holder fetched too early is [empty] and worth fetching again.
 *
 * [empty] is a sentinel representing no image.
 */
data class ImageHolder(
    /** Where the pixels come from. */
    val source: ImageSource,
    /** The image width in pixels, or a proportional value for aspect ratio. */
    val width: Measure,
    /** The image height in pixels, or a proportional value for aspect ratio. */
    val height: Measure
) {
    companion object {
        /** A sentinel representing no image. */
        val empty = ImageHolder(ImageSource.Missing,0.px,0.px)
    }
}
