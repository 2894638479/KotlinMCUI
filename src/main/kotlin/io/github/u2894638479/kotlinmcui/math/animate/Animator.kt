package io.github.u2894638479.kotlinmcui.math.animate

import io.github.u2894638479.kotlinmcui.context.DslFrameContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Animator<T: Interpolatable<T>> (
    beginValue: T,
    private val durationNano: Long,
    private val interpolator: Interpolator,
    private val frameContext: DslFrameContext
): ReadWriteProperty<Any?, T> {
    var beginValue = beginValue
        private set
    private var beginTimeNano = 0L
    var targetValue = beginValue
        private set

    private var lastResult = beginValue
    private var lastResultNano = 0L

    var value:T
        get() {
            val frameTimeNano = frameContext.frameBeginNano
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
        set(value) = setWithTime(value, System.nanoTime())

    fun setWithTime(value:T,timeNano: Long) {
        if(targetValue == value) return
        beginValue = this.value
        targetValue = value
        beginTimeNano = timeNano
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}