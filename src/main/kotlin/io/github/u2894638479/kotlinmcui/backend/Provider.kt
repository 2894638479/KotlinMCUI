package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.image.ImageHolder
import kotlinx.coroutines.CoroutineDispatcher
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
 * `DslRuntime`'s lock, but the readers ([dslMetadata], [currentPlatform]) take no lock: without a
 * happens-before edge from the write to the read, a reader may legally keep seeing `null` forever.
 */
@Volatile
internal var dslMetadataProvider: (() -> DslPlatformMetadata)? = null

/**
 * The published platform. [DslRuntime.start] is what fills this in.
 *
 * **`@Volatile` is part of the publication contract, do not remove it.** Written under
 * `DslRuntime`'s lock, read without one by [currentPlatform]; and the three backends publish from
 * different threads (web main, window EDT, the game thread), so this is not a hypothetical.
 */
@Volatile
internal var dslPlatformProvider: (() -> DslPlatform)? = null

/** The platform in force: the published one, or the placeholder below. */
internal fun currentPlatform(): DslPlatform = dslPlatformProvider?.invoke() ?: UnpublishedPlatform

/**
 * The placeholder that answers while no backend has published a platform.
 *
 * Every query that has a benign answer gets it, so entries can be initialised before any backend is
 * ready: translation falls back to the key (`null`), the world is not loaded (`false`), there is no image
 * yet (`empty`). Only [DslPlatform.dispatcher] cannot be faked - dispatching onto the session thread
 * needs a thread a backend owns - so it is the single member that still fails before [DslRuntime.start].
 */
internal object UnpublishedPlatform : DslPlatform {
    override val metadata: DslPlatformMetadata
        get() = dslMetadataProvider?.invoke()
            ?: throw IllegalStateException("DslRuntime.install has not run, so there is no metadata yet")

    override val utils: DslPlatformUtils = UnpublishedUtils

    override val dispatcher: CoroutineDispatcher
        get() = throw IllegalStateException(
            "no platform is published yet, so there is no session thread to dispatch onto. Entry " +
                "initialisation runs before a backend publishes one with DslRuntime.start, and this is " +
                "the one thing it cannot do there.",
        )
}

private object UnpublishedUtils : DslPlatformUtils {
    override fun translate(key: String, vararg args: Any?): String? = null
    override val isInWorld: Boolean = false
    override fun localImage(file: File): ImageHolder = ImageHolder.empty
    override fun remoteImage(url: String): ImageHolder = ImageHolder.empty
    override fun reloadLocalImage(file: File) {}
    override fun reloadRemoteImage(url: String) {}
    override fun quit(exitCode: Int): Nothing = exitProcess(exitCode)
}
