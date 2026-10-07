package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer

/** A page shaped function whose extra parameter is a vararg: nothing to call it with. */
context(ctx: DslContext)
fun VarargPreviewPage(vararg names: String): DslChild = Spacer(id = names.size)
