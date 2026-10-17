package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.math.Position

/**
 * What a root needs from the backend that made it: drawing, input, screens and per-output helpers.
 *
 * Early form of the draft's `DslRootProvider<RP, SC>`. A provider is registered once and may be shared
 * by several roots, so everything here must be "one answer, many roots" - state that belongs to a root
 * lives on the root, not here.
 *
 * `screenFactory` keeps its current name [DslBackendScreenFactory]: the draft renames it
 * `DslRootScreenFactory`, and that rename rides with the rest of the breaking changes so the backends
 * are touched once.
 */
interface DslRootProvider<RP, SC> {
    /** `null` when this provider's roots can draw nothing - which says nothing about the process. */
    val renderer: DslBackendRenderer<RP>?

    val input: DslBackendInput

    /** `null` when this provider's roots are evaluation-only; creating a screen on such a root fails. */
    val screenFactory: DslBackendScreenFactory<SC>?

    val rootUtils: DslRootUtils

    /**
     * Run one frame of [root] with [renderParam].
     *
     * A provider without a renderer still has a well-defined frame: it draws nothing. Writing the
     * root's [DslRoot.rect] is the provider's business too - it is the one that knows the output size -
     * and it must happen before the render pass, which copies that rect onto every mounted screen.
     */
    fun frame(root: DslRoot, renderParam: RP)
}

/**
 * The transitional provider: the published aggregate backend, seen as a [DslRootProvider].
 *
 * Nothing is copied - the renderer and the input are the backend object itself, so identity checks
 * hold and a backend keeps working untouched. It exists only until the backends implement
 * [DslRootProvider] themselves; then this file goes away with the aggregate type.
 */
internal fun <RP, SC> rootProviderOf(backend: DslBackend<RP, SC>): DslRootProvider<RP, SC> =
    object : DslRootProvider<RP, SC> {
        override val renderer: DslBackendRenderer<RP>? get() = backend
        override val input: DslBackendInput get() = backend
        override val screenFactory: DslBackendScreenFactory<SC>? get() = backend
        override val rootUtils: DslRootUtils get() = rootUtilsOf(backend)

        override fun frame(root: DslRoot, renderParam: RP) {
            val renderer = renderer ?: return
            kotlin.context(renderer, renderParam, backendMouse()) { root.component.render() }
        }
    }

private fun DslRootProvider<*, *>.backendMouse(): Position = input.mouse

private fun rootUtilsOf(backend: DslBackendUtils): DslRootUtils = object : DslRootUtils {
    override var clipboard: String
        get() = backend.clipboard
        set(value) { backend.clipboard = value }

    override suspend fun capture(fileName: String) = backend.capture(fileName)
    override fun narrate(string: String) = backend.narrate(string)
    override fun playButtonSound() = backend.playButtonSound()
    override fun openUri(uri: String) = backend.openUri(uri)
    override val sessionDispatcher get() = backend.mainDispatcher
}
