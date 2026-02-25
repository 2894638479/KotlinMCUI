package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.math.Measure
import kotlin.reflect.KMutableProperty0

inline fun <V> KMutableProperty0<V?>.lazy(defaultValue:()->V):V {
    get()?.let { return it }
    return defaultValue().also { set(it) }
}

inline fun KMutableProperty0<Measure>.lazy(defaultValue:()->Measure): Measure {
    val value = get()
    if(value.bits != Measure.AUTO.bits) return value
    return defaultValue().also { set(it) }
}