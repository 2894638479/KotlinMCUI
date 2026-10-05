package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext

/** A page that cannot be built at all, for the preview session's failure path. */
context(ctx: DslContext)
fun ThrowingPreviewPage(): DslChild = error("this page cannot be built")
