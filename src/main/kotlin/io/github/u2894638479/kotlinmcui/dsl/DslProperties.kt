package io.github.u2894638479.kotlinmcui.dsl

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext

/**
 * Convenience accessors for common DSL context properties.
 *
 * These extension properties reduce boilerplate by making [DslDataStore],
 * lifecycle scopes, and the root this page is mounted on directly accessible from any DSL context.
 */

context(ctx: DslDataStoreContext)
val dataStore get() = ctx.dataStore

context(ctx: DslIdContext)
val identity get() = ctx.identity

/**
 * The root this page is mounted on.
 *
 * The answer is per root rather than process-wide. `renderer` may be `null` for a root that draws
 * nothing; what a page does about that is its decision at the point of use.
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
 * Its keys are bare ids, so any mod can address a slot by name and nothing here is ever released;
 * one id, one type, for the whole process.
 *
 * @see io.github.u2894638479.kotlinmcui.container.DslDataStore.staticLifeScope
 */
context(ctx: DslDataStoreContext)
val static get() = dataStore.staticLifeScope

/** Local property scope — cleared every frame if not accessed. */
context(ctx: DslDataStoreContext)
val local get() = dataStore.localLifeScope

context(ctx: DslDataStoreContext)
val executeContext: DslExecuteContext get() = DslExecuteContext(ctx.dataStore)
