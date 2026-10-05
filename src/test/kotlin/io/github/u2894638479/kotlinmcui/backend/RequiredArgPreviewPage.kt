package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer

/**
 * A page-shaped function whose extra parameter has no default: the preview has to refuse it, which
 * it can only tell from the missing `name$default` method.
 */
context(ctx: DslContext)
fun RequiredArgPreviewPage(required: Int): DslChild = Spacer(id = required)
