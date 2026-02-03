package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.ctxBackend

context(ctx: DslContext)
fun DefaultBackground(id:Any = object{}::class) = ctxBackend.defaultBackground(id)