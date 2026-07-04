package io.github.u2894638479.kotlinmcui.functions

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext

context(ctx: DslDataStoreContext)
val dataStore get() = ctx.dataStore

context(ctx: DslIdContext)
val identity get() = ctx.identity

context(ctx: DslDataStoreContext)
val backend get() = ctx.dataStore.backend

context(ctx: DslDataStoreContext)
val screenTitle get() = dataStore.title

context(ctx: DslDataStoreContext)
val stable get() = dataStore.stableLifeScope

context(ctx: DslDataStoreContext)
val static get() = dataStore.staticLifeScope

context(ctx: DslDataStoreContext)
val local get() = dataStore.localLifeScope

context(ctx: DslDataStoreContext)
val executeContext: DslExecuteContext get() = DslExecuteContext(ctx.dataStore)