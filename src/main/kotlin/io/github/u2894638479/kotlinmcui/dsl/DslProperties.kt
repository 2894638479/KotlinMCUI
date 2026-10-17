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
 * - **With a key** it is a named address inside that root: another screen, another component, another
 *   declaration site on the same root reads the same slot. `session("x") { 1 }` here, `session("x")`
 *   there.
 * - **Without a key** it is private to the declaration site.
 *
 * Same type contract as `static`: one key, one type per root; a wrong-type read throws a
 * `ClassCastException`. `root.close()` runs each slot's cleanup and empties the scope.
 *
 * The `local` / `stable` key is only a sub-key of its own declaration site, so a key can never be shared
 * through those two scopes: to share by name use [session] (per root) or [static] (per process).
 */
context(ctx: DslDataStoreContext)
val session get() = dataStore.sessionLifeScope

/**
 * Process-wide property scope — **kept for cross-root sharing and cross-mod contracts, not UI state**.
 *
 * Its keys are bare ids, so any mod can address a slot by name; nothing here is ever released.
 *
 * - **Do not** use it for screen-local UI state - that is [stable] (screen) or [local] (frame).
 * - **Do not** build keys dynamically: every distinct key grows the map for the life of the process.
 * - **Do not** use it as per-connection state - that is [session], which is bounded by a root.
 *
 * **Contract**: one id, one type, for the whole process; a wrong-type read throws a `ClassCastException`.
 */
context(ctx: DslDataStoreContext)
val static get() = dataStore.staticLifeScope

/** Local property scope — cleared every frame if not accessed. */
context(ctx: DslDataStoreContext)
val local get() = dataStore.localLifeScope

context(ctx: DslDataStoreContext)
val executeContext: DslExecuteContext get() = DslExecuteContext(ctx.dataStore)
