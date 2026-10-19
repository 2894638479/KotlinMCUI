package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val warnedAboutRemoteImages = AtomicBoolean(false)

/** One warning per process: a backend without remote images should not repeat it every frame. */
internal fun warnRemoteUnsupportedOnce() {
    if (warnedAboutRemoteImages.compareAndSet(false, true)) {
        dslLogger.warn(
            "this backend has no http client: remoteImage() answers empty and remote pictures are not " +
                "drawn. Pages still render; only those images are missing.",
        )
    }
}

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
     * Downloads the image at [url] and caches it, with the same rules as [localImage]: answer
     * immediately, `ImageHolder.empty` until the bytes have arrived, and ask again later.
     *
     * The default answers `empty` and warns once: a backend without an http client should still render
     * pages that happen to include a remote image, so the picture is missing and the log says why. A
     * page is user content, and a backend lacking a capability must not turn it into a crash.
     */
    fun remoteImage(url: String): ImageHolder {
        warnRemoteUnsupportedOnce()
        return ImageHolder.empty
    }

    /**
     * Forgets whatever is cached for [url] and downloads it again.
     *
     * The same command shape as [reloadLocalImage], for the same reason: no return value, and it belongs
     * in an event handler, not in a render body. The default does nothing but warn once.
     */
    fun reloadRemoteImage(url: String) {
        warnRemoteUnsupportedOnce()
    }

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
