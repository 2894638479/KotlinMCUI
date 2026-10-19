package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import java.io.File

/**
 * The platform-level helpers: questions about the process, not about one output.
 *
 * The process-level helpers one platform offers: translation, exit, image loading, world state. The signatures are the ones the old aggregate utils already
 * has, so a backend object satisfies it as-is - nothing new to implement. The per-output half lives
 * in [DslRootUtils].
 */
interface DslPlatformUtils {
    fun translate(key: String, vararg args: Any?): String?

    /** Whether the game world is loaded behind the UI. `false` on a headless platform is a real answer. */
    val isInWorld: Boolean

    /**
     * The image stored in [file], or [ImageHolder.empty] while there is nothing to draw yet.
     *
     * It answers **immediately**: a backend that cannot produce the holder this instant returns `empty`
     * and has the real one ready on a later call. Never store the result - a cached `empty` turns a
     * first-frame miss into a permanent one; ask again when the image is needed.
     */
    fun localImage(file: File): ImageHolder

    /**
     * Forgets whatever is cached for [file] and reads it again.
     *
     * It returns nothing on purpose: this is a command, not a query. A return value would let a render
     * function write `Image(..., reloadLocalImage(file))` and read the file on every frame. Read through
     * [localImage] as usual; the reloaded holder lands in that same cache.
     */
    fun reloadLocalImage(file: File)

    /** Leave the process. Root-local teardown is [DslRootUtils]'s business, not this. */
    fun quit(exitCode: Int)
}
