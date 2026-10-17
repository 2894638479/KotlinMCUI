package io.github.u2894638479.kotlinmcui.backend

// Both providers are deliberately not public. A platform installs itself through
// DslRuntime, which is what turns "install once while the mod initialises, publish the
// backend when the platform is ready" into a property of the framework rather than a
// convention every backend has to remember.
//
// `internal` is a Kotlin-source-level restriction only: the JVM sees these as public
// members with unmangled names, so a hand-written Java mod can still reach them. The
// boundary this file draws is about what the framework promises, not about bytecode.
internal var dslBackendProvider: (() -> DslBackend<*, *>)? = null
internal var dslMetadataProvider: (() -> DslBackendMetadata)? = null

/** The current backend instance. Throws until the platform has published one. */
val dslBackend get() = dslBackendProvider?.invoke() ?: throw dslNoBackendError()

private fun dslNoBackendError() = IllegalStateException(
    if (DslRuntime.installedButNotReady)
        "The backend is installed, but the platform is not ready to build it yet. Mod initialisation must not use the DSL; do it once screens can be created."
    else
        "DSL failed to find a backend. You should install a mod to provide it, or you are trying to use DSL before initialization."
)
