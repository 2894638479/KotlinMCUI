package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader

/**
 * Global backend provider.
 *
 * Set [dslBackendProvider] before any DSL screens are created (typically during
 * mod initialization). Once set, the backend is accessed via [dslBackend].
 */
var dslBackendProvider: (() -> DslBackend<*, *>)? = null

/** The current backend instance. Throws if no provider has been registered. */
val dslBackend get() = dslBackendProvider?.invoke() ?: throw dslNoBackendError()

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
