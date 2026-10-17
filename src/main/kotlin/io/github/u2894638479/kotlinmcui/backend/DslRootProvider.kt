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
 * `screenFactory` keeps its current name [DslRootScreenFactory]: the draft renames it
 * `DslRootScreenFactory`, and that rename rides with the rest of the breaking changes so the backends
 * are touched once.
 */
interface DslRootProvider<RP, SC> {
    /** `null` when this provider's roots can draw nothing - which says nothing about the process. */
    val renderer: DslBackendRenderer<RP>?

    val input: DslBackendInput

    /** `null` when this provider's roots are evaluation-only; creating a screen on such a root fails. */
    val screenFactory: DslRootScreenFactory<SC>?

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
