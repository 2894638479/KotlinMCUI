package io.github.u2894638479.kotlinmcui.backend

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * The per-output helpers: things one root does for its own session.
 *
 * The per-root helpers one output offers: clipboard, narration, capture, sounds, links, the session
 * dispatcher, and the bridge to that root's thread. The process-level half lives in [DslPlatformUtils].
 *
 * **Writing `local` / `stable` slots is only allowed on this root's rendering thread.** Those stores are
 * plain maps, not concurrent containers: a write from anywhere else is a data race, not a feature. From
 * another thread (HTTP, IO, a platform event) call [post] instead - it hands the block to the root thread.
 * Declarative properties (`local { }`, `stable { }`, `cached`, `animatable`) are built while the tree
 * renders, so their coroutines are already on that thread and need no `post`.
 */
interface DslRootUtils {
    var clipboard: String

    /** Capture this output; a `null` result means it is not supported. */
    suspend fun capture(fileName: String): File?

    fun narrate(string: String)
    fun playButtonSound()
    fun openUri(uri: String)

    /** The serial dispatcher of this session; session-scoped property scopes bind to it. */
    val sessionDispatcher: CoroutineDispatcher

    /** Whether the caller is on this root's rendering thread. */
    val isOnSessionThread: Boolean
}

/**
 * Run [block] on this root's rendering thread, inline when the caller is already on it.
 *
 * Inline is the only thing this adds over `CoroutineScope(sessionDispatcher).launch`: an
 * `ExecutorCoroutineDispatcher` always says a dispatch is needed, so a launch from the session thread
 * would still run after the current work rather than inside it, and an exception would go to the scope's
 * handler instead of back to the caller. Posting across threads is not ordered by anything beyond the
 * dispatcher's own queue - do not use it to sequence work against the frame loop.
 *
 * This is the entry for non-suspending callbacks; code that needs to suspend uses [sessionDispatcher]
 * with `withContext` or a scope.
 */
fun DslRootUtils.post(block: () -> Unit) {
    if(isOnSessionThread) block()
    else CoroutineScope(sessionDispatcher).launch { block() }
}
