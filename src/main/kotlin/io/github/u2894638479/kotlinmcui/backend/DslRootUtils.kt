package io.github.u2894638479.kotlinmcui.backend

import kotlinx.coroutines.CoroutineDispatcher
import java.io.File

/**
 * The per-output helpers: things one root does for its own session.
 *
 * Early form of the draft's `DslRootUtils`; the signatures are the ones the old aggregate utils already had,
 * so a backend object satisfies it as-is. The process-level half lives in [DslPlatformUtils].
 *
 * `closeOutput` is deliberately absent: nothing needs it before the draft's root lifecycle exists, and
 * adding an empty member now would only be filled in by guessing.
 */
interface DslRootUtils {
    var clipboard: String

    /** Capture this output. The capability is expressed by a `null` result, not by a missing type. */
    suspend fun capture(fileName: String): File?

    fun narrate(string: String)
    fun playButtonSound()
    fun openUri(uri: String)

    /** The serial dispatcher of this session; session-scoped property scopes bind to it. */
    val sessionDispatcher: CoroutineDispatcher
}
