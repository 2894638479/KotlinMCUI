package io.github.u2894638479.kotlinmcui.prop

import kotlin.reflect.KProperty


fun interface StableROProperty<out T> {
    fun getValue():T
}

val <T> StableROProperty<T>.value @JvmName("extGetValue") get() = getValue()

operator fun <T> StableROProperty<T>.getValue(thisRef:Any?,property: KProperty<*>) = getValue()


interface StableRWProperty<T>: StableROProperty<T> {
    fun setValue(value:T)
}

var <T> StableRWProperty<T>.value @JvmName("extGetValue") get() = getValue()
    @JvmName("extSetValue") set(value) = setValue(value)

operator fun <T> StableRWProperty<T>.setValue(thisRef:Any?,property: KProperty<*>,value:T) = setValue(value)



fun <T> StableRWProperty<T>.remapGet(listener:(T)->T) = object : StableRWProperty<T> by this {
    override fun getValue() = listener(this@remapGet.getValue())
}
fun <T> StableROProperty<T>.remapGet(listener:(T)->T) = StableROProperty { listener(this@remapGet.getValue()) }

fun <T> StableRWProperty<T>.remapSet(listener:(T)->T) = object : StableRWProperty<T> by this {
    override fun setValue(value: T) = this@remapSet.setValue(listener(value))
}

fun <T> StableRWProperty<T>.onGet(listener:(T)->Unit) = object : StableRWProperty<T> by this {
    override fun getValue() = this@onGet.getValue().also(listener)
}
fun <T> StableROProperty<T>.onGet(listener:(T)->Unit) = StableROProperty { this@onGet.getValue().also(listener) }

fun <T> StableRWProperty<T>.onSet(listener:(T)->Unit) = object : StableRWProperty<T> by this {
    override fun setValue(value: T) = this@onSet.setValue(value.also(listener))
}

fun <T,K> StableRWProperty<T>.remap(get:(T)->K,set:(K)->T) = object : StableRWProperty<K> {
    override fun getValue() = get(this@remap.getValue())
    override fun setValue(value: K) = this@remap.setValue(set(value))
}
fun <T,K> StableROProperty<T>.remap(get:(T)->K) = StableROProperty { get(this@remap.getValue()) }


