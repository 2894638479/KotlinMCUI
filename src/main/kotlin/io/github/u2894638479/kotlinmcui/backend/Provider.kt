package io.github.u2894638479.kotlinmcui.backend

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
 * `DslRuntime`'s lock, but the readers ([dslMetadata], [publishedPlatform]) take no lock: without a
 * happens-before edge from the write to the read, a reader may legally keep seeing `null` forever.
 */
@Volatile
internal var dslMetadataProvider: (() -> DslPlatformMetadata)? = null

/**
 * The published platform. `platformReady` is what fills this in.
 *
 * **`@Volatile` is part of the publication contract, do not remove it.** Written under
 * `DslRuntime`'s lock, read without one by [publishedPlatform]; and the three backends publish from
 * different threads (web main, window EDT, the game thread), so this is not a hypothetical.
 */
@Volatile
internal var dslPlatformProvider: (() -> DslPlatform)? = null

/** The published platform, or a loud failure when nothing published one yet. */
internal fun publishedPlatform(): DslPlatform = dslPlatformProvider?.invoke() ?: throw IllegalStateException(
    if (DslRuntime.installedButNotReady)
        "The platform is installed, but not ready to publish yet. Mod initialisation must not use the DSL; do it once screens can be created."
    else
        "DSL failed to find a platform. You should install a mod to provide it, or you are trying to use DSL before initialization."
)
