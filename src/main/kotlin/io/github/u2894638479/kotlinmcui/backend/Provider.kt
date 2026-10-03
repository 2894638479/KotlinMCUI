package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader

/**
 * Global backend provider.
 *
 * Set [dslBackendProvider] before any DSL screens are created (typically during
 * mod initialization). Once set, the backend is accessed via [dslBackend].
 */
var dslBackendProvider: (() -> DslBackend<*, *>)? = null

/**
 * Global metadata provider.
 *
 * Install this during mod initialisation, **before** the platform is ready to build
 * a backend. Everything in [DslBackendMetadata] is readable from the first moment,
 * while building the backend itself may need a platform that does not exist yet
 * (a window, a narrator, loaded resources). The provider must be side-effect free.
 *
 * Callers that only need metadata read [dslMetadata], so they never force a backend
 * to be constructed.
 */
var dslMetadataProvider: (() -> DslBackendMetadata)? = null

/** The current backend instance. Throws if no provider has been registered. */
val dslBackend get() = dslBackendProvider?.invoke() ?: throw dslNoBackendError()

/**
 * Metadata of the current platform.
 *
 * Falls back to the backend's own metadata for platforms that did not install a
 * separate provider, in which case reading metadata does build the backend.
 */
val dslMetadata: DslBackendMetadata
    get() = dslMetadataProvider?.invoke() ?: dslBackend

private fun dslNoBackendError() = IllegalStateException(
    "DSL failed to find a backend. You should install a mod to provide it, or you are trying to use DSL before initialization."
)

/**
 * Marks a backend implementation as internal/unstable.
 *
 * Users may install third-party backends at runtime, so depending on a specific
 * implementation may cause crashes. Use [dslBackend] instead for general access.
 */
@RequiresOptIn(
    message = "This backend is not stable; users may install other backends at runtime, causing a crash. Use dslBackend instead.",
    level = RequiresOptIn.Level.WARNING
)
annotation class InternalBackend
