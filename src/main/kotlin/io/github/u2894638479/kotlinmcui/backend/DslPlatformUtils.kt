package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val warnedAboutRemoteImages = AtomicBoolean(false)

/** Process-wide: warn once. */
internal fun warnRemoteUnsupportedOnce() {
    if (warnedAboutRemoteImages.compareAndSet(false, true)) {
        dslLogger.warn(
            "this backend has no http client: remoteImage() answers empty and remote pictures are not " +
                "drawn. Pages still render; only those images are missing.",
        )
    }
}

/**
 * These run on the frame thread, so none of them may wait for io: read a map, return a cached holder,
 * hand the work to another dispatcher and answer `empty` for now - never read a file, decode an image,
 * open a socket or wait on a lock here.
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

    /** After this call, the next [remoteImage] reflects [url] as it is now. A backend that caches nothing has nothing to do. */
    fun reloadRemoteImage(url: String) {
        warnRemoteUnsupportedOnce()
    }

    /** After this call, the next [localImage] reflects [file] as it is now. A backend that caches nothing has nothing to do. */
    fun reloadLocalImage(file: File)

    /** Leaves the process. */
    fun quit(exitCode: Int)
}
