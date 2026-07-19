package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader

var dslBackendProvider: (()->DslBackend<*, *>)? = null

val dslBackend get() = dslBackendProvider?.invoke() ?: throw dslNoBackendError()

private fun dslNoBackendError() = IllegalStateException(
    "DSL failed to find a backend. You should install a mod to provide it, or you are trying to use DSL before initialization."
)

@RequiresOptIn(
    message = "This backend is not stable, users may install other backends at runtime, causing a crash. \n use dslBackend instead",
    level = RequiresOptIn.Level.WARNING
)
annotation class InternalBackend