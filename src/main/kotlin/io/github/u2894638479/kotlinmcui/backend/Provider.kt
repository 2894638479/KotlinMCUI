package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import java.io.File
import kotlin.system.exitProcess

// The providers are deliberately not public. A platform installs itself through DslRuntime,
// which is what turns "install once while the mod initialises, publish the platform when the
// platform is ready" into a property of the framework rather than a convention every backend
// has to remember.
//
// `internal` is a Kotlin-source-level restriction only: the JVM sees these as public
// members with unmangled names, so a hand-written Java mod can still reach them. The
// boundary this file draws is about what the framework promises, not about bytecode.
/**
 * The installed metadata provider, or `null` before `install` ran.
 *
 * **`@Volatile` is part of the publication contract, do not remove it.** The writer sets it inside
 * `DslRuntime`'s lock, but the readers ([dslMetadata]) take no lock: without a happens-before edge
 * from the write to the read, a reader may legally keep seeing `null` forever.
 */
@Volatile
internal var dslMetadataProvider: (() -> DslPlatformMetadata)? = null

/**
 * The published utils provider. [DslRuntime.start] is what fills this in.
 *
 * **`@Volatile` is part of the publication contract, do not remove it.** Written under
 * `DslRuntime`'s lock, read without one by [dslUtils]; and the three backends publish from
 * different threads (web main, window EDT, the game thread), so this is not a hypothetical.
 */
@Volatile
internal var dslUtilsProvider: (() -> DslPlatformUtils)? = null

/**
 * The process-level helpers in force: the published ones, or the placeholder below.
 *
 * The placeholder answers while no backend has published utils, so entries can be initialised
 * before any backend is ready. Every one of its answers is benign; a session thread belongs to a
 * root ([DslRootUtils.sessionDispatcher]), not here.
 */
val dslUtils: DslPlatformUtils
    get() = dslUtilsProvider?.invoke() ?: UnpublishedUtils

/**
 * Metadata of the installed platform.
 *
 * An installed metadata provider answers before utils are published, so mod initialisation can
 * read directories without building anything.
 *
 * @throws IllegalStateException when [DslRuntime.install] has not run.
 */
val dslMetadata: DslPlatformMetadata
    get() = dslMetadataProvider?.invoke()
        ?: error("DslRuntime.install has not run, so there is no metadata yet")

/**
 * The placeholder that answers while no backend has published its utils.
 *
 * Every member is benign: translation falls back to the key (`null`), the world is not loaded
 * (`false`), there is no image yet (`empty`), the reloads do nothing, and quitting leaves the
 * process. Nothing here throws.
 */
internal object UnpublishedUtils : DslPlatformUtils {
    override fun translate(key: String, vararg args: Any?): String? = null
    override val isInWorld: Boolean = false
    override fun localImage(file: File): ImageHolder = ImageHolder.empty
    override fun remoteImage(url: String): ImageHolder = ImageHolder.empty
    override fun reloadLocalImage(file: File) {}
    override fun reloadRemoteImage(url: String) {}
    override fun quit(exitCode: Int): Nothing = exitProcess(exitCode)
}
