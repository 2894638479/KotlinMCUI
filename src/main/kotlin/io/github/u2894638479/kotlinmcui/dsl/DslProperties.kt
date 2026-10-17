package io.github.u2894638479.kotlinmcui.dsl

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext

/**
 * Convenience accessors for common DSL context properties.
 *
 * These extension properties reduce boilerplate by making [DslDataStore],
 * lifecycle scopes, and the root provider directly accessible from any DSL context.
 */

context(ctx: DslDataStoreContext)
val dataStore get() = ctx.dataStore

context(ctx: DslIdContext)
val identity get() = ctx.identity

/**
 * The provider of the root this page belongs to.
 *
 * It used to be the aggregate backend; after the split it is the root's provider, so the answer is
 * per root rather than process-wide. Reach the pieces through it:
 *
 * - `renderer` - **may be `null`** for a root that draws nothing. A page that needs drawing should
 *   fail, or degrade visibly, at the point where it asks: `provider.renderer?.getFont(...)` coming
 *   back as `null` is the framework saying so.
 * - `input` (pointer and preedit), `rootUtils` (clipboard, narration, sounds), `screenFactory`.
 *
 * Breaking change of this round: what was `backend.xxx` is now `provider.<slice>.xxx`.
 */
context(ctx: DslDataStoreContext)
val provider get() = ctx.dataStore.provider

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
