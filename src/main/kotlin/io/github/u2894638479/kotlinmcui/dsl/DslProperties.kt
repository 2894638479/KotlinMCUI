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
 * The root this page is mounted on.
 *
 * It used to be the aggregate backend, then the root's provider; now it is the root itself, so the
 * answer is per root rather than process-wide. Reach the pieces through it:
 *
 * - `renderer` - **may be `null`** for a root that draws nothing. A page that needs drawing should
 *   fail, or degrade visibly, at the point where it asks: `provider.renderer?.getFont(...)` coming
 *   back as `null` is the framework saying so.
 * - `input` (pointer and preedit), `rootUtils` (clipboard, narration, sounds), `screenFactory`.
 *
 * Breaking change of this round: what was `backend.xxx` is now `provider.<slice>.xxx`.
 */
context(ctx: DslDataStoreContext)
val root get() = ctx.dataStore.root

context(ctx: DslDataStoreContext)
val screenTitle get() = dataStore.title

/** Stable property scope — persists across frames while accessed each frame. */
context(ctx: DslDataStoreContext)
val stable get() = dataStore.stableLifeScope

/**
 * Session property scope — bounded by the root, shared by every screen mounted on it.
 *
 * With a key it is a named address inside that root, so one screen can set it and another can read it;
 * without one it is private to the declaration site. Same type contract as `static`: one key, one type
 * per root. `root.close()` runs each slot's cleanup and empties the scope.
 */
context(ctx: DslDataStoreContext)
val session get() = dataStore.sessionLifeScope

/** Static property scope — persists for the entire process lifetime. */
context(ctx: DslDataStoreContext)
val static get() = dataStore.staticLifeScope

/** Local property scope — cleared every frame if not accessed. */
context(ctx: DslDataStoreContext)
val local get() = dataStore.localLifeScope

context(ctx: DslDataStoreContext)
val executeContext: DslExecuteContext get() = DslExecuteContext(ctx.dataStore)
