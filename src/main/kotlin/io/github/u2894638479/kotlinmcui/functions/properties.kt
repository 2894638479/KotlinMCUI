package io.github.u2894638479.kotlinmcui.functions

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext

context(ctx: DslDataStoreContext)
val dataStore get() = ctx.dataStore

context(ctx: DslIdContext)
val identity get() = ctx.identity

context(ctx: DslDataStoreContext)
val ctxBackend get() = ctx.dataStore.backend

context(_: DslDataStoreContext,_: DslIdContext)
val inFocused get() = dataStore.focused?.contains(identity) ?: false

context(_: DslDataStoreContext,_: DslIdContext)
val containFocused get() = dataStore.focused?.let { it in identity } ?: false