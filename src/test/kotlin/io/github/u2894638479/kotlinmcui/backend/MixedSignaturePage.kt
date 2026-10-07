package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer

/**
 * A page shaped function whose parameters are half defaulted: the shape the preview has to refuse,
 * because there is no value it could pass for `required`.
 */
context(ctx: DslContext)
fun MixedSignaturePage(required: Int,optional: String = "d"): DslChild = Spacer(id = required)
