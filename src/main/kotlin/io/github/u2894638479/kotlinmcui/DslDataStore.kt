package io.github.u2894638479.kotlinmcui

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.identity.DslProperty
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.animate.Animator
import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import io.github.u2894638479.kotlinmcui.math.animate.Interpolator
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.prop.LocalROProperty
import io.github.u2894638479.kotlinmcui.prop.LocalRWProperty
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlin.time.Duration

class DslDataStore(val backend: DslBackend<*, *>, val onClose:()-> Unit, dslFunction: DslFunction): DslScaleContext {
    override val scale get() = backend.guiScale
    private val extraData = object: Object2ObjectOpenHashMap<DslProperty<*>, Any?>(){
        val empty = object{}
        inline fun getOrEmpty(key: DslProperty<*>, ifEmpty:()->Any?):Any?{
            val result = getOrDefault(key,empty)
            if(result === empty) return ifEmpty()
            return result
        }
        fun getOrPutInner(key: DslProperty<*>, default: Any?):Any? {
            defRetValue = empty
            val result = putIfAbsent(key,default)
            defRetValue = null
            if(result === empty) return default
            return result
        }
    }
    var frameIndex = ULong.MAX_VALUE
        private set
    var frameTimeNano = 0L
        private set

    internal fun newFrame() {
        frameIndex++
        frameTimeNano = System.nanoTime()
    }


    fun <T> remember(identity: DslId, defaultValue:T) = object : LocalRWProperty<T> {
        override val identity = identity
        override fun getValue(property: DslProperty<*>) = extraData.getOrPutInner(property,defaultValue) as T
        override fun setValue(property: DslProperty<*>, value: T) { extraData[property] = value }
    }

    fun <T> remember(identity: DslId, defaultValue:()->T) = object : LocalRWProperty<T> {
        override val identity = identity
        override fun getValue(property: DslProperty<*>) = extraData.getOrEmpty(property) {
            defaultValue().also { extraData[property] = it }
        } as T
        override fun setValue(property: DslProperty<*>, value: T) { extraData[property] = value}
    }

    fun <T : Interpolatable<T>> animatable(identity: DslId, beginValue: T, duration: Duration, interpolator: Interpolator) = object :
        LocalRWProperty<T> {
        fun animator(property: DslProperty<*>) = extraData.getOrPut(property) {
            Animator(
                beginValue,
                duration.inWholeNanoseconds,
                interpolator
            )
        } as Animator<T>
        override val identity = identity
        override fun getValue(property: DslProperty<*>) = animator(property).value
        override fun setValue(property: DslProperty<*>, value: T) { animator(property).value = value }
    }

    fun <T : Interpolatable<T>> autoAnimate(identity: DslId, value: T, duration: Duration, interpolator: Interpolator) = object:
        LocalROProperty<T> {
        override val identity = identity
        override fun getValue(property: DslProperty<*>):T {
            val animator = extraData.getOrPut(property) {
                Animator(
                    value,
                    duration.inWholeNanoseconds,
                    interpolator
                )
            } as Animator<T>
            if(animator.targetValue != value) animator.value = value
            return animator.value
        }
    }

    fun <K,V> cached(identity: DslId, key: K, value:(K)->V) = object: LocalROProperty<V> {
        override val identity = identity
        override fun getValue(property: DslProperty<*>): V {
            val map = extraData.getOrPut(property) { Object2ObjectOpenHashMap<K,V>() } as Object2ObjectOpenHashMap<K,V>
            return map.getOrPut(key) { value(key) }
        }
    }

    var focused: DslId? = null
        set(value) {
            field = value
            dslScreen.run { focusChanged(value) }
        }
    var tooltip: DslComponent? = null
    var hovered: DslId? = null
        set(value) {
            field = value
            dslScreen.run { hoverChanged(value) }
        }
    var narration: String? = null
        set(value) {
            if(value != field) {
                field = value
                //TODO
            }
        }
    var mouse = Position(-1.px,-1.px)

    val dslScreen = DslScreen(this,dslFunction)
}