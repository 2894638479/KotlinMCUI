package io.github.u2894638479.kotlinmcui.dsl

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext

/**
 * Convenience accessors for common DSL context properties.
 *
 * These extension properties reduce boilerplate by making [DslDataStore],
 * lifecycle scopes, and the backend directly accessible from any DSL context.
 */

context(ctx: DslDataStoreContext)
val dataStore get() = ctx.dataStore

context(ctx: DslIdContext)
val identity get() = ctx.identity

context(ctx: DslDataStoreContext)
val backend get() = ctx.dataStore.backend

context(ctx: DslDataStoreContext)
val screenTitle get() = dataStore.title

/** Stable property scope — persists across frames while accessed each frame. */
context(ctx: DslDataStoreContext)
val stable get() = dataStore.stableLifeScope

/** Static property scope — persists for the entire process lifetime. */
context(ctx: DslDataStoreContext)
val static get() = dataStore.staticLifeScope

/** Local property scope — cleared every frame if not accessed. */
context(ctx: DslDataStoreContext)
val local get() = dataStore.localLifeScope

context(ctx: DslDataStoreContext)
val executeContext: DslExecuteContext get() = DslExecuteContext(ctx.dataStore)