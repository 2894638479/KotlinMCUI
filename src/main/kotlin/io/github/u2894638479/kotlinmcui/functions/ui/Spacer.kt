package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun Spacer(modifier: Modifier = Modifier, id:Any) =
    collect(DslComponentImpl(newChildId(id), modifier))