package io.github.u2894638479.kotlinmcui.image

import java.io.File

/**
 * Where an [ImageHolder]'s pixels come from.
 *
 * The three sources are three types rather than three prefixes of one string, so they cannot be
 * mistaken for one another: a resource location can never be read as a local file. Backends answer a
 * `when` over this instead of parsing ids.
 *
 * The strings inside are **data**, not assertions: a location or url can come from a config file, a text
 * field or a server, so a wrong one must not throw.
 */
sealed interface ImageSource {
    /**
     * An image from a resource pack, written `namespace:path`.
     *
     * Not validated here: the string may be typed by a user. Built by code, `imageResource` keeps the old
     * habit of showing the missing sprite rather than throwing.
     */
    data class Resource(val location: String) : ImageSource

    /** A file on this machine. Its size is known only after it has been read. */
    data class LocalFile(val file: File) : ImageSource

    /** An image behind an http(s) url. Its size is known only after it has been downloaded. */
    data class Remote(val url: String) : ImageSource
}
