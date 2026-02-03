package io.github.u2894638479.kotlinmcui.math.animate

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Animator<T: Interpolatable<T>> (
    beginValue: T,
    private val durationNano: Long,
    private val interpolator: Interpolator
): ReadWriteProperty<Any?, T> {
    var beginValue = beginValue
        private set
    private var beginTimeNano = 0L
    var targetValue = beginValue
        private set
    var value:T
        get() {
            val currentTimeNano = System.nanoTime()
            return when {
                currentTimeNano > beginTimeNano + durationNano -> targetValue
                currentTimeNano < beginTimeNano -> beginValue
                else -> interpolator.interpolate(beginValue,targetValue,(currentTimeNano - beginTimeNano)/durationNano.toDouble())
            }
        }
        set(value) {
            if(targetValue == value) return
            beginValue = this.value
            targetValue = value
            beginTimeNano = System.nanoTime()
        }
    override fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}