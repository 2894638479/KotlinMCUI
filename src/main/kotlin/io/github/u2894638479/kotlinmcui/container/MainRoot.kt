package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.backend.rootProviderOf

/**
 * The process's main root, built on first use.
 *
 * It is the one the runtime hands out as the owner root. Built lazily so that creating it - which
 * creates the overlay screen through the backend - still happens on the first touch of the framework,
 * which is where it has always happened.
 */
internal val mainRoot: DslRoot by lazy {
    DslRoot(
        state = DslRootState(
            onFocusChanged = { newFocus, source -> mainComponent().globalFocusChanged(newFocus, source) },
            onHoverChanged = { newHover -> mainComponent().globalHoverChanged(newHover) },
            onNarrate = { mainRoot.provider.rootUtils.narrate(it) }
        ),
        geometry = DslRootGeometry(),
        // The one place that hands the published platform to a root. Everything downstream reads it
        // from the root, which is what lets the rest of the framework stop asking the global.
        provider = rootProviderOf(dslBackend)
    )
}

/**
 * The main root's component, resolved through [mainRoot] on every call so the state callbacks never
 * capture a root that is still being initialised.
 */
private fun mainComponent() = mainRoot.component
