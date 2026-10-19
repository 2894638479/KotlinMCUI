package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.dsl.identity
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import io.github.u2894638479.kotlinmcui.math.animate.InterpolatableDouble
import io.github.u2894638479.kotlinmcui.math.animate.Interpolator
import io.github.u2894638479.kotlinmcui.math.animate.interpolate
import io.github.u2894638479.kotlinmcui.math.animate.toInterpolatable
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PropertyLifeScope internal constructor(
    internal val dataStore: DslDataStore,
    internal val isStatic: Boolean = false,
    private val map: MutableMap<DslId, TimedProperty<*>> = Object2ObjectOpenHashMap<DslId, TimedProperty<*>>(),
    /**
     * session: keyed by the bare key, bounded by the root instead of the process, so its entries
     * launch in the root's own session scope.
     */
    internal val isSession: Boolean = false,
    /**
     * Whether every entry owns a scope of its own instead of running in this life scope's one.
     *
     * This is decided by how entries die, not by the life scope's name. `local` is the only one whose
     * entries are cleared **one by one** (the frame's end clears the entries that were not read), so
     * there an entry's coroutines must end with that entry: the entry owns its own scope and cancels
     * it. The other life scopes are released **as a whole**, so their entries run in one scope held by
     * the life cycle (for `session`, by the root), never by an entry.
     */
    internal val scopePerEntry: Boolean = false,
) {
    internal val size get() = map.size

    private val declaredSites = Object2ObjectOpenHashMap<Any, Any>()

    /**
     * The one scope a together-released life cycle's entries launch in, `null` until one is needed.
     *
     * This field is why such an entry never holds a scope itself: the scope belongs to the life cycle
     * and [clear] cancels it. `session` does not use it - its lifetime already has an owner, the root,
     * whose session scope `root.close()` cancels.
     */
    private var sharedScope: CoroutineScope? = null

    /**
     * Keys this scope saw declared by **two different declaration sites** in the same screen.
     *
     * A site repeating its own key is normal (a page declares it every frame); two sites reusing one
     * key is the mistake this records. The value type is not part of the key either, so the two sites
     * end up sharing one slot and the second reads the first one's value.
     */
    internal val reusedKeys = mutableListOf<ReusedKey>()

    /** One recorded reuse: the key and the two declaration sites that claimed it. */
    internal data class ReusedKey(val key: DslId, val firstSite: Any, val laterSite: Any)

    internal open class TimedProperty<T>(private var _value:T, var frameStamp: ULong = 0uL) : StableRW<T> {
        /**
         * The scope this entry's initialiser started its coroutines in, or `null` when it owns none.
         *
         * **Invariant: a non-null scope means this entry owns it, so [onClear] cancels it.** Only an
         * entry of a per-entry life cycle (`local`, see [PropertyLifeScope.scopePerEntry]) owns one,
         * because only there is an entry cleared on its own and must take its coroutines with it. An
         * entry of a together-released life cycle launches in the scope that life cycle holds (which
         * its whole release cancels), so this stays `null` and [onClear] is a no-op.
         */
        internal var scope: CoroutineScope? = null

        override fun getValue() = _value
        override fun setValue(value: T) { this._value = value }

        /** Release this entry: cancel the scope it owns, if any. A shared scope is not its to cancel. */
        open fun onClear() { scope?.cancel(); scope = null }
    }

    /**
     * Storage key of one declaration site.
     *
     * The declaring lambda's class is part of the key in the per-screen scopes, so two
     * different call sites that pass the same explicit [key] — possibly with different
     * value types — get separate slots instead of silently sharing (and corrupting) one.
     * Static keys stay the bare key: that scope is process global and is meant to be
     * reachable across mods by key alone.
     */
    context(_: DslIdContext)
    private fun getKey(key: Any?, site: Any) = when {
        // The bare key is what makes a static slot reachable across mods - and a session slot
        // reachable from another screen on the same root.
        isStatic || isSession -> DslId(key ?: site)
        // Without an explicit key the declaring site alone already is the key, and this
        // is the per-frame path: do not grow the identity chain for nothing.
        key == null -> identity + site
        else -> identity + key + site
    }

    /**
     * Record a key claimed by more than one declaration site in this screen.
     *
     * Static scopes are skipped on purpose: their key *is* the bare id (that is how they stay reachable
     * across mods), so "two sites, one key" is their documented usage, not a mistake. With no explicit
     * key the site itself is the slot, so there is nothing to compare.
     */
    private fun noteDeclaration(userKey: Any?, site: Any) {
        if(isStatic || isSession || userKey == null) return
        // Keyed by the raw key: no DslId is built on the normal path.
        val first = declaredSites[userKey]
        if(first == null) {
            declaredSites[userKey] = site
            return
        }
        if(first === site) return
        // Only the recorded path builds the DslId: it is the rare one.
        val key = DslId(userKey)
        if(reusedKeys.none { it.key == key && it.laterSite === site }) {
            reusedKeys += ReusedKey(key, first, site)
            dslLogger.warn(
                "the property key '$key' is declared by two different sites in one screen. The key of " +
                    "`local`/`stable` is only a sub-key of its declaration site and is never shared across " +
                    "sites; to share by name use `session(\"…\")` (per root) or `static(\"…\")` (per process).",
            )
        }
    }

    /**
     * This erased cast is safe **only where the declaration site is part of the key** - the per-screen
     * scopes. A static key is the bare id (that is what makes it reachable across mods), so two
     * declaration sites that use the same id with different value types share one slot, and reading it
     * as the wrong type throws ClassCastException. The contract is therefore per scope:
     * **inside one process, the same id must always carry the same type**.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> TimedProperty<*>.stable(): StableRW<T> = this as StableRW<T>

    private inner class Animator<T: Interpolatable<T>> (
        beginValue: T,
        private val durationNano: Long,
        private val interpolator: Interpolator,
    ): StableRW<T> {
        var beginValue = beginValue
            private set
        private var beginTimeNano = 0L
        var targetValue = beginValue
            private set

        private var lastResult = beginValue
        private var lastResultNano = 0L

        fun setWithTime(value:T,timeNano: Long) {
            if(targetValue == value) return
            // Read the current (possibly interpolated) value first, then drop the
            // frame memo: a later read in the same frame must see the new segment,
            // not the result that was cached before the target changed.
            beginValue = getValue()
            targetValue = value
            beginTimeNano = timeNano
            lastResultNano = Long.MIN_VALUE
        }

        override fun getValue():T {
            val frameTimeNano = dataStore.frameBeginNano
            if(frameTimeNano == lastResultNano) return lastResult
            return when {
                // A zero-length animation must land on its target immediately;
                // interpolating would divide by zero and yield NaN.
                durationNano <= 0L -> targetValue
                frameTimeNano >= beginTimeNano + durationNano -> targetValue
                frameTimeNano < beginTimeNano -> beginValue
                else -> interpolator.interpolate(beginValue,targetValue,(frameTimeNano - beginTimeNano)/durationNano.toDouble())
            }.also {
                lastResult = it
                lastResultNano = frameTimeNano
            }
        }
        override fun setValue(value: T) = setWithTime(value, dataStore.frameBeginNano)
    }

    private class CachedProperty<K,V>(val key: K,value:V,frameStamp: ULong = 0uL): TimedProperty<V>(value,frameStamp)
    /**
     * Give this entry the current frame stamp, which is what keeps it alive for the frame.
     *
     * The refresh is a primitive of its own because not every access path inserts: cached keeps its
     * entry when the cache key is unchanged. Every path that inserts goes through [entry], and every path
     * that only touches an existing entry goes through this - there is no third way to write the stamp.
     */
    private fun <T : TimedProperty<*>> T.refreshed(): T {
        frameStamp = dataStore.frameIndex
        return this
    }

    /**
     * The one place an entry is inserted; it also stamps it, so a brand-new entry is alive immediately.
     *
     * Four copies of "create it, then stamp it" is what let [dispose] lose its stamp: its entry looked
     * stale on the very next frame and a live screen had its cleanup run.
     */
    private fun <T : TimedProperty<*>> entry(identity: DslId, create: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        val stored = (map[identity] ?: create().also { map[identity] = it }) as T
        return stored.refreshed()
    }

    /**
     * The scope one entry of this life cycle should launch in.
     *
     * Which one follows the entries' death mode, never the life scope's name: entries cleared one by
     * one (`local`) get a fresh scope each, which the entry owns ([TimedProperty.scope]); entries
     * released as a whole launch in the one scope their life cycle holds - the root's own for
     * `session`, whose lifetime is exactly the root's, and a lazily created one for `stable`/`static`,
     * which [clear] cancels (`static` is never cleared, so its scope simply lives for the process).
     */
    private fun scopeForEntry(dispatcher: CoroutineDispatcher): CoroutineScope =
        if(scopePerEntry) CoroutineScope(SupervisorJob() + dispatcher)
        else if(isSession) dataStore.root.sessionScope
        else sharedScope ?: CoroutineScope(SupervisorJob() + dispatcher).also { sharedScope = it }

    /**
     * Give the entry the scope it owns, so [TimedProperty.onClear] cancels it.
     *
     * Only a per-entry life cycle lets an entry own a scope; an entry of a together-released one runs
     * in the life cycle's scope, which is not the entry's to cancel, so its `scope` stays `null`.
     */
    private fun <T : TimedProperty<*>> T.owning(owned: CoroutineScope): T = apply {
        if(scopePerEntry) scope = owned
    }

    internal fun clearOutdated() {
        val frameIndex = dataStore.frameIndex
        map.values.removeIf {
            val outdated = it.frameStamp != frameIndex
            // Only per-entry entries own a scope, so this cancels exactly the stale entry's coroutines.
            if(outdated) it.onClear()
            outdated
        }
    }

    /** Drop every entry, running [TimedProperty.onClear] on each, then cancel the scope they shared. */
    internal fun clear() {
        map.values.forEach { it.onClear() }
        map.clear()
        // Of the entries above only a per-entry life cycle owns a scope; the ones released together
        // launch in this one (or, for `session`, in the root's), so this cancel ends their coroutines.
        sharedScope?.cancel()
        sharedScope = null
    }

    /**
     * The stand-alone scope entry of this declaration site (`local.coroutineScope`, `stable.coroutineScope`).
     *
     * It is an entry like any other, so it follows the same death mode as the life cycle's properties:
     * a per-entry life cycle (`local`) gives it a scope it owns and cancels when the entry is cleared;
     * a together-released one hands out the scope the life cycle holds (for `session`, the root's own),
     * so the entry only carries the frame stamp and owns no scope. Kept as a separate accessor because
     * a component that needs a scope outside an initialiser has nowhere else to put it.
     */
    private fun coroutineScope(
        identity:DslId,
        dispatcher: CoroutineDispatcher = dataStore.root.rootUtils.sessionDispatcher
    ): CoroutineScope {
        val property = entry(identity) {
            val scope = scopeForEntry(dispatcher)
            TimedProperty(scope).owning(scope)
        }
        return property.value
    }

    /**
     * Returns a [CoroutineScope] for launching coroutines, dispatched on the render thread by default.
     *
     * If the scope is not accessed every frame, it may be disposed depending on the lifecycle type.
     *
     * Like a property, the scope **owns its coroutines**, and when it ends follows the life cycle's
     * death mode: a scope in `local` dies with the frame that stopped reading it (it is the only life
     * cycle whose entries are cleared one by one); a scope in `stable`/`session`/`static` is the one
     * scope that life cycle holds, cancelled when the whole life cycle is released - with the screen,
     * with the root, or never for `static`.
     * For a coroutine that must survive that, take a bigger scope (the root's) instead of this one.
     *
     * ### Correct usage 1: use the built-in scope
     * ```
     * Button{}.clickable {
     *     launch { /* ... */ }
     * }
     * ```
     * The scope provided to [clickable] is automatically managed.
     *
     * ### Correct usage 2: keep a reference accessed every frame
     * ```
     * val coroutineScope = local.coroutineScope
     * Button{}.clickable {
     *     coroutineScope.launch { /* ... */ }
     * }
     * ```
     *
     * ### Incorrect: accessing only in event handler
     * ```
     * Button{}.clickable {
     *     local.coroutineScope.launch { /* destroyed next frame! */ }
     * }
     * ```
     */
    context(ctx: DslIdContext)
    val coroutineScope get() = coroutineScope(identity, dataStore.root.rootUtils.sessionDispatcher)

    /**
     * Returns a reactive property with automatic lifecycle management.
     *
     * The declaration site is part of the storage key, so two call sites may pass the same [key] and
     * still get separate slots. Only the `static` scope keys by the bare key - that is what makes it
     * reachable across mods, and it is also why there the same key must always carry the same type.
     *
     * **The coroutines an initialiser starts follow this property's death mode.** A `local` entry is
     * cleared on its own, so it owns the scope its initialiser ran in and clearing it cancels its
     * coroutines; a `stable` / `session` / `static` entry dies with the whole life cycle, which holds
     * the scope - the entry itself owns none - so the life cycle's release cancels them. A coroutine
     * that must outlive the property needs a bigger scope explicitly (the root's, the process's) - the
     * ownership of a coroutine is the author's choice, never a silent default.
     *
     * @param key Sub-key inside this declaration site.
     * @param init The initial value. Only called when the property does not yet exist.
     *
     * When the property's lifecycle ends and it is accessed again, [init] is
     * called again to recreate the property.
     */
    context(ctx: DslIdContext)
    fun <T> property(key:Any? = null, init:context(DslExecuteContext) CoroutineScope.() -> T): StableRW<T> {
        val identity = getKey(key, init::class)
        return entry(identity) {
            // Only a brand-new entry can carry new information about key reuse: the diagnostic is a
            // creation-time check, never a per-frame one.
            noteDeclaration(key, init::class)
            val scope = scopeForEntry(dataStore.root.rootUtils.sessionDispatcher)
            TimedProperty(context(DslExecuteContext(dataStore)) { scope.init() }).owning(scope)
        }.stable()
    }

    /**
     * Syntactic sugar for [property]. Can be used for both declaration and initialisation.
     * @see property
     *
     * ### Usage
     * ```
     * var a by static { 1 }
     * var b by local { "alice" }
     *
     * static {
     *     initMyGui()
     *     launch { loadMyData() }
     * }
     *
     * local { startEnterAnimation() }
     * ```
     */
    context(ctx: DslIdContext)
    operator fun <T> invoke(key:Any? = null, init:context(DslExecuteContext) CoroutineScope.() -> T) = property(key,init)

    /**
     * Cache a value with an expensive computation (e.g. [String.format]).
     * For lightweight calculations, caching is not recommended.
     *
     * A changed [cacheKey] replaces the entry. The replaced entry is cleared on its own through
     * [TimedProperty.onClear]: in a per-entry life cycle that cancels its scope; in a together-released
     * one the entry owns no scope, so its coroutines end with the life cycle rather than with the entry -
     * cancelling them would cancel the siblings that share the scope.
     *
     * @param cacheKey The cache key — when this changes, the value is recomputed.
     * @param id Storage location ID, appended to the current [identity]. Uses `init::class` if `null`.
     * @param init Called on first access and whenever [cacheKey] changes, to compute the cached value.
     */
    context(ctx: DslIdContext)
    fun <K,V> cached(cacheKey: K, key: Any? = null, init:context(DslExecuteContext) CoroutineScope.(K)->V) : StableRW<V> {
        val identity = getKey(key, init::class)
        val previous = map[identity]
        if(previous is CachedProperty<*,*> && previous.key == cacheKey) {
            @Suppress("UNCHECKED_CAST")
            return (previous as CachedProperty<K, V>).refreshed().stable()
        }
        // A changed cache key replaces the entry, so let `entry` build a new one.
        if(previous != null) map.remove(identity)
        // Same rules as `property`: the entry runs in the life cycle's scope, owning it only where
        // entries die one by one; reuse is diagnosed only on creation.
        val fresh = entry(identity) {
            noteDeclaration(key, init::class)
            val scope = scopeForEntry(dataStore.root.rootUtils.sessionDispatcher)
            CachedProperty(cacheKey, context(DslExecuteContext(dataStore)) { scope.init(cacheKey) })
                .owning(scope)
        }
        previous?.onClear()
        return fresh.stable()
    }

    /**
     * An animatable property with the same lifecycle as [property].
     * When assigned, the value gradually interpolates to the target instead of
     * changing immediately. Animation starts from the moment of assignment.
     *
     * @param duration Animation duration.
     * @param interpolator The easing function.
     */
    context(ctx: DslIdContext)
    fun <T : Interpolatable<T>> animatable(
        duration: Duration = 0.5.seconds,
        interpolator: Interpolator = Interpolator.default,
        key:Any? = null,
        init:context(DslExecuteContext)() -> T
    ) : StableRW<T> {
        val id = key ?: init::class
        val animator by property(id) {
            Animator(init(), duration.inWholeNanoseconds, interpolator)
        }
        return animator
    }

    @JvmName("animatableD")
    context(ctx: DslIdContext)
    fun animatable(
        duration: Duration = 0.5.seconds,
        interpolator: Interpolator = Interpolator.default,
        key:Any? = null,
        value:context(DslExecuteContext)() -> Double
    ) = animatable<InterpolatableDouble>(duration, interpolator, key ?: value::class) {
        InterpolatableDouble(value())
    }.remap({it.toDouble()},{InterpolatableDouble(it)})

    /**
     * Automatically animates to follow the [value] expression each frame.
     * Cannot be assigned manually. Animation starts from the beginning of the frame.
     *
     * @param duration Animation duration.
     * @param interpolator The easing function.
     * @param value Evaluated every frame to determine the animation target.
     *
     * ### Example
     * ```
     * val height by autoAnimate {
     *     if (unfold) 60.scaled else 20.scaled
     * }
     * ```
     */
    context(ctx: DslIdContext)
    fun <T : Interpolatable<T>> autoAnimate(
        init: T? = null,
        duration: Duration = 0.5.seconds,
        interpolator: Interpolator = Interpolator.default,
        key:Any? = null,
        value:context(DslExecuteContext)() -> T
    ) : StableRO<T> {
        val id = key ?: value::class
        val value = context(DslExecuteContext(dataStore), value)
        val animator by property(id) {
            Animator(init ?: value, duration.inWholeNanoseconds, interpolator)
        }
        return animator.also { it.value = value }
    }

    @JvmName("autoAnimateD")
    context(ctx: DslIdContext)
    fun autoAnimate(
        init: Double? = null,
        duration: Duration = 0.5.seconds,
        interpolator: Interpolator = Interpolator.default,
        key:Any? = null,
        value:context(DslExecuteContext)() -> Double
    ) = autoAnimate(init?.toInterpolatable(), duration, interpolator, key ?: value::class) {
        value().toInterpolatable()
    }.remap { it.toDouble() }

    /**
     * Register an [action] to be executed when this property is cleared from the lifecycle.
     * Useful for cleanup (e.g. cancelling coroutines, releasing resources).
     */
    context(ctx: DslIdContext)
    fun dispose(key:Any? = null, action: () -> Unit) {
        val identity = getKey(key, action::class)
        entry(identity) {
            object : TimedProperty<Unit>(Unit) {
                override fun onClear() { action() }
            }
        }
    }
}