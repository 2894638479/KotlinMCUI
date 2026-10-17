package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslRuntime

/**
 * The process's main root, built on first use.
 *
 * It is created through [DslRuntime.createOwner], the only path that may set `isOwner`, and it draws
 * with the provider an entry point registered: a root without a provider has nothing to draw with, so
 * asking for one before [DslRuntime.registerProvider] fails loudly instead of building a half-root.
 *
 * Built lazily so that creating it - which creates the overlay screen through the provider - still
 * happens on the first touch of the framework, which is where it has always happened.
 */
internal val mainRoot: DslRoot by lazy {
    val provider = DslRuntime.registeredProviders.firstOrNull()
        ?: error(
            "no root provider was registered: DslRuntime.registerProvider must run before the owner " +
                "root is created, otherwise the main root has nothing to draw with"
        )
    DslRuntime.createOwner(provider)
}
