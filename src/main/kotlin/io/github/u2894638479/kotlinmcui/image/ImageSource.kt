package io.github.u2894638479.kotlinmcui.image

import java.io.File

/**
 * Where an [ImageHolder]'s pixels come from.
 *
 * The four sources are four types rather than four prefixes of one string, so they cannot be mistaken
 * for one another: a resource location can never be read as a local file, and a missing image can never
 * be read as a resource called "missing". Backends answer a `when` over this instead of parsing ids.
 */
sealed interface ImageSource {
    /**
     * An image from a resource pack, written `namespace:path`.
     *
     * Both halves are checked here, when the holder is built: a typo is a programming error and should
     * fail immediately instead of drawing nothing every frame.
     */
    data class Resource(val location: String) : ImageSource {
        init {
            require(RESOURCE_LOCATION.matches(location)) {
                "an image resource is 'namespace:path' (lowercase letters, digits, '_', '-', '.'; '/' in " +
                    "the path), was '$location'"
            }
        }
    }

    /** A file on this machine. Its size is known only after it has been read. */
    data class LocalFile(val file: File) : ImageSource

    /** An image behind an http(s) url. Its size is known only after it has been downloaded. */
    data class Remote(val url: String) : ImageSource {
        init {
            require(url.startsWith("http://") || url.startsWith("https://")) {
                "a remote image url starts with http:// or https://, was '$url'"
            }
        }
    }

    /** Nothing to draw. A sentinel, not a broken address. */
    object Missing : ImageSource
}

private val RESOURCE_LOCATION = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")
