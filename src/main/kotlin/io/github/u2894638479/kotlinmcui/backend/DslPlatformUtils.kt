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

    fun loadLocalImage(file: File): ImageHolder
    fun forceLoadLocalImage(file: File): ImageHolder

    /** Leave the process. Root-local teardown is [DslRootUtils]'s business, not this. */
    fun quit(exitCode: Int)
}
