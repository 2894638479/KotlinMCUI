package io.github.u2894638479.kotlinmcui.image

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

/**
 * A reference to an image: where it comes from, and how big it is.
 *
 * [source] is `null` for [empty] - there is no image to draw. A holder is also [empty] while the pixels
 * of a [ImageSource.LocalFile] or [ImageSource.Remote] are still on their way, and nothing in the
 * framework draws differently for "not loaded yet" and "not loadable": both answer "nothing to draw
 * now". A failed load is the backend's business to log, or to replace with a placeholder *resource*
 * before handing the holder back.
 *
 * ### Sizing
 * [width] and [height] should be the image's own dimensions in pixels.
 * If the actual texture size is unknown (e.g. resource packs may use 16×16,
 * 64×64, or higher), it is sufficient to provide the correct **aspect ratio**
 * — the framework scales images similarly to Minecraft's item/block textures.
 *
 * A local or remote holder learns its size only after the pixels arrive, so it is [empty] until then.
 */
data class ImageHolder(
    /** Where the pixels come from; `null` when there is no image. */
    val source: ImageSource?,
    /** The image width in pixels, or a proportional value for aspect ratio. */
    val width: Measure,
    /** The image height in pixels, or a proportional value for aspect ratio. */
    val height: Measure
) {
    /** Nothing to draw: no image at all, or one that has not arrived yet. */
    val isEmpty get() = source == null

    companion object {
        /** A sentinel representing no image. */
        val empty = ImageHolder(null,0.px,0.px)
    }
}
