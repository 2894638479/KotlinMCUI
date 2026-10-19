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
 * The platform-level helpers: process-wide questions and effects.
 *
 * Only what the framework itself relies on is stated here. How a backend caches images, whether a load
 * is synchronous, what it draws for an unresolvable resource - all of that is its own business, and the
 * signature is the only requirement.
 *
 * The per-output half lives in [DslRootUtils].
 */
interface DslPlatformUtils {
    /** `null` when there is no translation; callers then fall back to the key. */
    fun translate(key: String, vararg args: Any?): String?

    /** Whether the game world is loaded behind the UI. */
    val isInWorld: Boolean

    /** The image stored in [file]; [ImageHolder.empty] means nothing to draw. */
    fun localImage(file: File): ImageHolder

    /** The image at [url]; [ImageHolder.empty] means nothing to draw. */
    fun remoteImage(url: String): ImageHolder {
        warnRemoteUnsupportedOnce()
        return ImageHolder.empty
    }

    /** Drops the cached copy of [url]: the next [remoteImage] reads it again. */
    fun reloadRemoteImage(url: String) {
        warnRemoteUnsupportedOnce()
    }

    /** Drops the cached copy of [file]: the next [localImage] reads it again. */
    fun reloadLocalImage(file: File)

    /** Leaves the process. */
    fun quit(exitCode: Int)
}
