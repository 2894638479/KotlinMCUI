package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.dsl.identity
import io.github.u2894638479.kotlinmcui.identity.DslId
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
    private val map: Object2ObjectOpenHashMap<DslId, TimedProperty<*>> = Object2ObjectOpenHashMap<DslId, TimedProperty<*>>()
) {
    internal val size get() = map.size
    internal open class TimedProperty<T>(private var _value:T, var frameStamp: ULong = 0uL) : StableRW<T> {
        override fun getValue() = _value
        override fun setValue(value: T) { this._value = value }
        open fun onClear() {}
    }

    /**
     * Storage key of one declaration site.
     *
     * The declaring lambda's class is part of the key in the per-screen scopes, so two
     * different call sites that pass the same explicit [id] — possibly with different
     * value types — get separate slots instead of silently sharing (and corrupting) one.
     * Static keys stay the bare id: that scope is process global and is meant to be
     * reachable across mods by id alone.
     */
    context(_: DslIdContext)
    private fun getKey(id: Any?, site: Any) = when {
        // The bare id is what makes a static slot reachable across mods.
        isStatic -> DslId(id ?: site)
        // Without an explicit id the declaring site alone already is the key, and this
        // is the per-frame path: do not grow the identity chain for nothing.
        id == null -> identity + site
        else -> identity + id + site
    }

    /**
     * The slot's value type is fixed by the declaration site that created it, and that
     * site is part of the storage key, so this erased cast cannot fail at runtime.
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
        dispatcher: CoroutineDispatcher = dataStore.provider.rootUtils.sessionDispatcher
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
    val coroutineScope get() = coroutineScope(identity, dataStore.provider.rootUtils.sessionDispatcher)

    /**
     * Returns a reactive property with automatic lifecycle management.
     *
     * @param init The initial value. Only called when the property does not yet exist.
     *
     * When the property's lifecycle ends and it is accessed again, [init] is
     * called again to recreate the property.
     */
    context(ctx: DslIdContext)
    fun <T> property(id:Any? = null, init:context(DslExecuteContext) CoroutineScope.() -> T): StableRW<T> {
        val identity = getKey(id, init::class)
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
    operator fun <T> invoke(id:Any? = null, init:context(DslExecuteContext) CoroutineScope.() -> T) = property(id,init)

    /**
     * Cache a value with an expensive computation (e.g. [String.format]).
     * For lightweight calculations, caching is not recommended.
     *
     * @param key The cache key — when this changes, the value is recomputed.
     * @param id Storage location ID, appended to the current [identity]. Uses `init::class` if `null`.
     * @param init Called on first access and whenever [key] changes, to compute the cached value.
     */
    context(ctx: DslIdContext)
    fun <K,V> cached(key: K,id: Any? = null, init:context(DslExecuteContext) CoroutineScope.(K)->V) : StableRW<V> {
        val identity = getKey(id, init::class)
        val coroutineScope = coroutineScope(identity)
        val previous = map[identity]
        if(previous is CachedProperty<*,*> && previous.key == key) return previous.stable()
        return CachedProperty(key,context(DslExecuteContext(dataStore)){ coroutineScope.init(key) }).also {
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
        id:Any? = null,
        init:context(DslExecuteContext)() -> T
    ) : StableRW<T> {
        val id = id ?: init::class
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
        id:Any? = null,
        value:context(DslExecuteContext)() -> Double
    ) = animatable<InterpolatableDouble>(duration, interpolator, id ?: value::class) {
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
        id:Any? = null,
        value:context(DslExecuteContext)() -> T
    ) : StableRO<T> {
        val id = id ?: value::class
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
        id:Any? = null,
        value:context(DslExecuteContext)() -> Double
    ) = autoAnimate(init?.toInterpolatable(), duration, interpolator, id ?: value::class) {
        value().toInterpolatable()
    }.remap { it.toDouble() }

    /**
     * Register an [action] to be executed when this property is cleared from the lifecycle.
     * Useful for cleanup (e.g. cancelling coroutines, releasing resources).
     */
    context(ctx: DslIdContext)
    fun dispose(id:Any? = null, action: () -> Unit) {
        val identity = getKey(id, action::class)
        map.getOrPut(identity) {
            object : TimedProperty<Unit>(Unit) {
                override fun onClear() { action() }
            }
        }
    }
}