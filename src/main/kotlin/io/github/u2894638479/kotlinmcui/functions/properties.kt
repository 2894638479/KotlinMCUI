package io.github.u2894638479.kotlinmcui.functions

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext

context(ctx: DslDataStoreContext)
val dataStore get() = ctx.dataStore

context(ctx: DslIdContext)
val identity get() = ctx.identity

context(ctx: DslDataStoreContext)
val ctxBackend get() = ctx.dataStore.backend