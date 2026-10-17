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
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PropertyLifeScope internal constructor(
    internal val dataStore: DslDataStore,
    internal val isStatic: Boolean = false,
    private val map: MutableMap<DslId, TimedProperty<*>> = Object2ObjectOpenHashMap<DslId, TimedProperty<*>>(),
    /** session: keyed by the bare key, bounded by the root instead of the process. */
    internal val isSession: Boolean = false,
) {
    internal val size get() = map.size

    private val declaredSites = Object2ObjectOpenHashMap<DslId, Any>()

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
        override fun getValue() = _value
        override fun setValue(value: T) { this._value = value }
        open fun onClear() {}
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
    }.also { noteDeclaration(key, site) }

    /**
     * Record a key claimed by more than one declaration site in this screen.
     *
     * Static scopes are skipped on purpose: their key *is* the bare id (that is how they stay reachable
     * across mods), so "two sites, one key" is their documented usage, not a mistake. With no explicit
     * key the site itself is the slot, so there is nothing to compare.
     */
    private fun noteDeclaration(userKey: Any?, site: Any) {
        if(isStatic || isSession || userKey == null) return
        val key = DslId(userKey)
        val first = declaredSites[key]
        if(first == null) {
            declaredSites[key] = site
            return
        }
        if(first === site) return
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
    internal fun clearOutdated() {
        val frameIndex = dataStore.frameIndex
        map.values.removeIf {
            val bl = it.frameStamp != frameIndex
            bl.apply { if(bl) it.onClear() }
        }
    }

    /** Drop every entry, running [TimedProperty.onClear] on each. */
    internal fun clear() {
        map.values.forEach { it.onClear() }
        map.clear()
    }

    private fun coroutineScope(
        identity:DslId,
        dispatcher: CoroutineDispatcher = dataStore.root.rootUtils.sessionDispatcher
    ): CoroutineScope {
        val identity = identity + dispatcher
        val property = map[identity] ?: return object : CoroutineScope {
            var delegate: CoroutineScope? = null
            override val coroutineContext: CoroutineContext
                get() = delegate?.coroutineContext ?: CoroutineScope(dispatcher).let {
                    delegate = it
                    val property = TimedProperty(it,dataStore.frameIndex)
                    map[identity] = property
                    it.coroutineContext
                }
        }
        property.frameStamp = dataStore.frameIndex
        return property.value as CoroutineScope
    }

    /**
     * Returns a [CoroutineScope] for launching coroutines, dispatched on the render thread by default.
     *
     * Accessing this property has no immediate cost — the scope is constructed lazily
     * on first use. If the scope is not accessed every frame, it may be disposed
     * depending on the lifecycle type.
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
     * 这是本声明点内部的子键；同一个 key 在不同声明点是不同槽，不会共享。
     *
     * Returns a reactive property with automatic lifecycle management.
     *
     * The declaration site is part of the storage key, so two call sites may pass the same [key] and
     * still get separate slots. Only the `static` scope keys by the bare key - that is what makes it
     * reachable across mods, and it is also why there the same key must always carry the same type.
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
        val coroutineScope = coroutineScope(identity)
        return map.getOrPut(identity) {
            TimedProperty(context(DslExecuteContext(dataStore)) { coroutineScope.init() })
        }.also { it.frameStamp = dataStore.frameIndex }.stable()
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
     * @param cacheKey The cache key — when this changes, the value is recomputed.
     * @param id Storage location ID, appended to the current [identity]. Uses `init::class` if `null`.
     * @param init Called on first access and whenever [cacheKey] changes, to compute the cached value.
     */
    context(ctx: DslIdContext)
    fun <K,V> cached(cacheKey: K, key: Any? = null, init:context(DslExecuteContext) CoroutineScope.(K)->V) : StableRW<V> {
        val identity = getKey(key, init::class)
        val coroutineScope = coroutineScope(identity)
        val previous = map[identity]
        if(previous is CachedProperty<*,*> && previous.key == cacheKey) return previous.stable()
        return CachedProperty(cacheKey,context(DslExecuteContext(dataStore)){ coroutineScope.init(cacheKey) }).also {
            previous?.onClear()
            map[identity] = it
        }
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
        map.getOrPut(identity) {
            object : TimedProperty<Unit>(Unit) {
                override fun onClear() { action() }
            }
        }
    }
}