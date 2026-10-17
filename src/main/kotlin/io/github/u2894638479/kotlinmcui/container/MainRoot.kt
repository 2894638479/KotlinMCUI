package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.dslBackend

/**
 * The process's main root, built on first use.
 *
 * It is the root [TopComponent] stands for and the one the runtime hands out as the owner root. Built
 * lazily so that creating it - which creates the overlay screen through the backend - still happens on
 * the first touch of the framework, exactly where the object used to build it.
 */
internal val mainRoot: DslRoot by lazy {
    DslRoot(
        state = DslRootState(
            onFocusChanged = { newFocus, source -> mainComponent().globalFocusChanged(newFocus, source) },
            onHoverChanged = { newHover -> mainComponent().globalHoverChanged(newHover) },
            onNarrate = { dslBackend.narrate(it) }
        ),
        geometry = DslRootGeometry()
    )
}

/**
 * The main root's component, resolved through [mainRoot] on every call so the state callbacks never
 * capture a root that is still being initialised.
 */
private fun mainComponent() = mainRoot.component
