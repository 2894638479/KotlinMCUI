package io.github.u2894638479.kotlinmcui.prop

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0


fun interface StableRO<out T> {
    fun getValue():T
}

val <T> StableRO<T>.value @JvmName("extGetValue") get() = getValue()

operator fun <T> StableRO<T>.getValue(thisRef:Any?, property: KProperty<*>) = getValue()


interface StableRW<T>: StableRO<T> {
    fun setValue(value:T)
}

var <T> StableRW<T>.value @JvmName("extGetValue") get() = getValue()
    @JvmName("extSetValue") set(value) = setValue(value)

operator fun <T> StableRW<T>.setValue(thisRef:Any?, property: KProperty<*>, value:T) = setValue(value)



fun <T> StableRW<T>.remapGet(listener:(T)->T) = object : StableRW<T> by this {
    override fun getValue() = listener(this@remapGet.getValue())
}
fun <T> StableRO<T>.remapGet(listener:(T)->T) = StableRO { listener(this@remapGet.getValue()) }

fun <T> StableRW<T>.remapSet(listener:(T)->T) = object : StableRW<T> by this {
    override fun setValue(value: T) = this@remapSet.setValue(listener(value))
}

fun <T> StableRW<T>.onGet(listener:(T)->Unit) = object : StableRW<T> by this {
    override fun getValue() = this@onGet.getValue().also(listener)
}
fun <T> StableRO<T>.onGet(listener:(T)->Unit) = StableRO { this@onGet.getValue().also(listener) }

fun <T> StableRW<T>.onSet(listener:(T)->Unit) = object : StableRW<T> by this {
    override fun setValue(value: T) = this@onSet.setValue(value.also(listener))
}

fun <T,K> StableRW<T>.remap(get:(T)->K, set:(K)->T) = object : StableRW<K> {
    override fun getValue() = get(this@remap.getValue())
    override fun setValue(value: K) = this@remap.setValue(set(value))
}
fun <T,K> StableRO<T>.remap(get:(T)->K) = StableRO { get(this@remap.getValue()) }

val <V> KMutableProperty0<V>.property get() = object : StableRW<V> {
    override fun getValue() = get()
    override fun setValue(value: V) = set(value)
}

val <V> KProperty0<V>.property get() = StableRO { get() }
