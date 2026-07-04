package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.DslDataStore
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslLaunchableContext
import io.github.u2894638479.kotlinmcui.functions.identity
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import io.github.u2894638479.kotlinmcui.math.animate.InterpolatableDouble
import io.github.u2894638479.kotlinmcui.math.animate.Interpolator
import io.github.u2894638479.kotlinmcui.math.animate.interpolate
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PropertyLifeScope internal constructor(
    val dataStore: DslDataStore,
    val isStatic: Boolean = false,
    private val map: Object2ObjectOpenHashMap<DslId, TimedProperty<*>> = Object2ObjectOpenHashMap<DslId, TimedProperty<*>>()
) {
    open class TimedProperty<T> internal constructor(protected var _value:T, var frameStamp: ULong) : StableRW<T> {
        override fun getValue() = _value
        override fun setValue(value: T) { this._value = value }
        open fun onClear() {}
    }

    context(_: DslIdContext)
    private fun getId(id:Any) = if(isStatic) DslId(id) else identity + id

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
            beginValue = getValue()
            targetValue = value
            beginTimeNano = timeNano
        }

        override fun getValue():T {
            val frameTimeNano = dataStore.frameBeginNano
            if(frameTimeNano == lastResultNano) return lastResult
            return when {
                frameTimeNano > beginTimeNano + durationNano -> targetValue
                frameTimeNano < beginTimeNano -> beginValue
                else -> interpolator.interpolate(beginValue,targetValue,(frameTimeNano - beginTimeNano)/durationNano.toDouble())
            }.also {
                lastResult = it
                lastResultNano = frameTimeNano
            }
        }
        override fun setValue(value: T) = setWithTime(value, dataStore.frameBeginNano)
    }

    private class CachedProperty<K,V>(value:V,frameStamp: ULong,val key:K): TimedProperty<V>(value,frameStamp)
    fun clearOutdated() {
        val frameIndex = dataStore.frameIndex
        map.values.removeIf {
            val bl = it.frameStamp != frameIndex
            bl.apply { if(bl) it.onClear() }
        }
    }

    private fun launchableContext(
        identity:DslId,
        dispatcher: CoroutineDispatcher = dataStore.backend.mainDispatcher
    ): DslLaunchableContext {
        val identity = identity + dispatcher
        return map.getOrPut(identity) {
            object : TimedProperty<DslLaunchableContext>
                (DslLaunchableContext(dispatcher),dataStore.frameIndex) {
                override fun onClear() { _value.destroy() }
            }
        }.also { it.frameStamp = dataStore.frameIndex }.getValue() as DslLaunchableContext
    }

    /**
     * @return 一个[DslLaunchableContext]对象，可用于获取[CoroutineScope]以启动协程。
     *
     * 使用此属性不会立即创建[CoroutineScope]，而是在第一次启动协程时创建。
     *
     * [CoroutineScope]创建后每次访问此属性会刷新变量的帧戳。如果没有每帧都访问此属性，在[io.github.u2894638479.kotlinmcui.functions.local]中会被销毁。
     *
     * ### 用法示例
     * ```
     * Button{}.clickable {
     *     // ✅ 使用自带的scope，自动管理
     *     coroutineScope.launch {
     *         // ...
     *     }
     * }
     * ```
     * ### 用法示例2
     * ```
     * // ✅ 每帧都会被调用，保证存活
     * val ctx = local.launchableContext
     * Button{}.clickable {
     *     ctx.launch {
     *         // ...
     *     }
     * }
     * ```
     * ### 错误示例
     * ```
     * Button{}.clickable {
     *     // ❌ 只有点击时调用，下一帧就被销毁
     *     local.launchableContext.launch {
     *         // ...
     *     }
     * }
     */
    context(ctx: DslIdContext)
    val launchableContext get() = launchableContext(identity, dataStore.backend.mainDispatcher)

    /**
     * 返回一个属性。
     * @param init 属性的初始值。只会在属性不存在且调用了此方法时调用。
     *
     * 注意变量生命周期结束后再次调用会触发[init]
     */
    context(ctx: DslIdContext)
    fun <T> property(id:Any? = null, init:context(DslExecuteContext) CoroutineScope.() -> T): StableRW<T> {
        val identity = getId(id ?: init::class)
        val launchable = launchableContext(identity)
        return map.getOrPut(identity) {
            TimedProperty(context(DslExecuteContext(dataStore)) { launchable.init() },dataStore.frameIndex)
        }.also { it.frameStamp = dataStore.frameIndex } as StableRW<T>
    }

    /**
     * [PropertyLifeScope.property]的语法糖。可用于声明变量也可用于初始化。
     * @see [PropertyLifeScope.property]
     * ### 用法示例
     * ```
     * var a by static { 1 }
     * var b by local { "alice" }
     *
     * static {
     *     initMyGui()
     *     coroutineScope.launch {
     *         loadMyData()
     *     }
     * }
     *
     * local {
     *     startEnterAnimation()
     * }
     * ```
     */
    context(ctx: DslIdContext)
    operator fun <T> invoke(id:Any? = null, init:context(DslExecuteContext) CoroutineScope.() -> T) = property(id,init)

    /**
     * 缓存一个计算耗时较长的值。如[String.format]。
     *
     * 少量的计算不建议使用此函数进行缓存。
     *
     * @param key 缓存的标识。
     * @param id 存储位置的ID。会延伸在[identity]之后。如果为`null`则使用`init::class`
     * @param init 在第一次调用时和[key]发生变化时调用，用来计算缓存值。
     */
    context(ctx: DslIdContext)
    fun <K,V> cached(key: K,id: Any? = null, init:context(DslExecuteContext) CoroutineScope.(K)->V) : StableRW<V> {
        val identity = getId(id ?: init::class)
        val launchableCtx = launchableContext(identity)
        val property = map[identity]?.let { it as CachedProperty<K,V> }
        if(property != null && property.key == key) return property
        return CachedProperty(context(DslExecuteContext(dataStore)){ launchableCtx.init(key) },dataStore.frameIndex,key).also {
            property?.onClear()
            map[identity] = it
        }
    }

    /**
     * 生命周期同[property]，被赋值时并不会立刻变为目标值，而是渐变到目标值。动画起始时间为被赋值的时刻。
     *
     * @param duration 动画持续时长
     * @param interpolator 插值器
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
     * 跟随value的值自动产生动画。不能手动赋值。动画的起始时间点为帧起始时刻。
     *
     * @param duration 动画持续时长
     * @param interpolator 插值器
     * @param value 每帧都会调用
     * ```
     * val height by autoAnimate {
     *     if(unfold) 60.scaled else 20.scaled
     * }
     * ```
     */
    context(ctx: DslIdContext)
    fun <T : Interpolatable<T>> autoAnimate(
        duration: Duration = 0.5.seconds,
        interpolator: Interpolator = Interpolator.default,
        id:Any? = null,
        value:context(DslExecuteContext)() -> T
    ) : StableRO<T> {
        val id = id ?: value::class
        val value = context(DslExecuteContext(dataStore), value)
        val animator by property(id) {
            Animator(value, duration.inWholeNanoseconds, interpolator)
        }
        return animator.also { it.value = value }
    }

    @JvmName("autoAnimateD")
    context(ctx: DslIdContext)
    fun autoAnimate(
        duration: Duration = 0.5.seconds,
        interpolator: Interpolator = Interpolator.default,
        id:Any? = null,
        value:context(DslExecuteContext)() -> Double
    ) = autoAnimate<InterpolatableDouble>(duration, interpolator, id ?: value::class) {
        InterpolatableDouble(value())
    }.remap { it.toDouble() }

    context(ctx: DslIdContext)
    fun dispose(id:Any? = null, action: context(DslExecuteContext)() -> Unit) {

    }
}