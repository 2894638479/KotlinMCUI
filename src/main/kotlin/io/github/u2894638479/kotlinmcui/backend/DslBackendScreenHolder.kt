package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslScreen

/**
 * Holds a created screen, bridging the platform-specific screen with the
 * framework's abstract [DslScreen].
 *
 * @param SC The platform-specific screen type.
 */
interface DslBackendScreenHolder<SC> {
    /** Display this screen to the user. */
    fun show()

    /** The platform-specific screen object. */
    val screen: SC

    /** The framework-abstracted DSL screen. */
    val dslScreen: DslScreen
}
