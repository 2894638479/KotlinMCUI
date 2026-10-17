package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.dsl.DslFunction

/**
 * Factory for creating platform-specific screens from DSL functions.
 *
 * This is a [fun interface], so it can be implemented with a lambda.
 *
 * @param SC The platform-specific screen type.
 */
fun interface DslRootScreenFactory<SC> {
    /**
     * Create a new screen.
     * @param title The screen title (shown in the game's screen list or title bar).
     * @param dslFunction The DSL function that defines the screen's UI content.
     * @return A holder wrapping both the platform screen and the framework's [io.github.u2894638479.kotlinmcui.container.DslScreen].
     */
    fun create(title: String, dslFunction: DslFunction): DslBackendScreenHolder<SC>
}
