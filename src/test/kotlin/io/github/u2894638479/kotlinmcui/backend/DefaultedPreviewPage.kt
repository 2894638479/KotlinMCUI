package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer

/** What [DefaultedPreviewPage] was called with, so a test can see the preview's defaults arrive. */
object DefaultedPreviewCalls {
    var count: Int? = null
    var name: String? = null
}

/**
 * A page whose extra parameters all have defaults and whose return type is not `Unit`: the two
 * shapes the preview accepts beyond a bare `ctx`-only function.
 */
context(ctx: DslContext)
fun DefaultedPreviewPage(count: Int = 7,name: String = "default"): DslChild {
    DefaultedPreviewCalls.count = count
    DefaultedPreviewCalls.name = name
    return Spacer(id = "defaulted")
}
