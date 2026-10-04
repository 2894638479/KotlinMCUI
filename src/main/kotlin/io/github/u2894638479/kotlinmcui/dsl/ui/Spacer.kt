package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.modifier.Modifier

/**
 * Create a blank component that only carries a [Modifier].
 *
 * Use it as empty space, or as the base of a custom component:
 * ```
 * Spacer(modifier, id).change { object : DslComponent by it { ... } }
 * ```
 *
 * @param modifier The modifier controlling size, padding, background, etc. Defaults to [Modifier].
 * @param id Unique identifier; the caller supplies one because this function takes no lambda.
 * @return The collected component, for use with [DslChild.change].
 */
context(ctx: DslContext)
fun Spacer(modifier: Modifier = Modifier, id:Any) =
    collect(DslComponentImpl(newChildId(id), modifier,ctx))