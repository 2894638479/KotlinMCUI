package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.identity.DslProperty
import kotlin.reflect.KProperty

interface LocalROProperty<out T> {
    val identity: DslId
    fun getValue(property: DslProperty<*>):T
}

operator fun <T> LocalROProperty<T>.getValue(thisRef:Any?, property: KProperty<*>) = getValue(
    DslProperty(
        property,
        identity
    )
)

interface LocalRWProperty<T> : LocalROProperty<T> {
    fun setValue(property: DslProperty<*>, value:T)
}

operator fun <T> LocalRWProperty<T>.setValue(thisRef: Any?,property: KProperty<*>,value:T) = setValue(
    DslProperty(
        property,
        identity
    ),value)


private fun <T> DslProperty<T>.typed(value:Any) = DslProperty(kProp, identity + value::class)
private fun <T> DslProperty<T>.typed2(value:Any, value2:Any) =
    DslProperty(kProp, identity + (value::class to value2::class))

fun <T> LocalRWProperty<T>.remapGet(listener:(T)->T) = object : LocalRWProperty<T> by this {
    override fun getValue(property: DslProperty<*>) = listener(this@remapGet.getValue(property.typed(listener)))
}
fun <T> LocalROProperty<T>.remapGet(listener:(T)->T) = object: LocalROProperty<T> {
    override val identity = this@remapGet.identity
    override fun getValue(property: DslProperty<*>) = listener(this@remapGet.getValue(property.typed(listener)))
}

fun <T> LocalRWProperty<T>.remapSet(listener:(T)->T) = object : LocalRWProperty<T> by this {
    override fun setValue(property: DslProperty<*>, value: T) = this@remapSet.setValue(property.typed(listener),listener(value))
}

fun <T> LocalRWProperty<T>.onGet(listener:(T)->Unit) = object : LocalRWProperty<T> by this {
    override fun getValue(property: DslProperty<*>) = this@onGet.getValue(property.typed(listener)).also(listener)
}
fun <T> LocalROProperty<T>.onGet(listener:(T)->Unit) = object: LocalROProperty<T> {
    override val identity = this@onGet.identity
    override fun getValue(property: DslProperty<*>) = this@onGet.getValue(property.typed(listener)).also(listener)
}

fun <T> LocalRWProperty<T>.onSet(listener:(T)->Unit) = object : LocalRWProperty<T> by this {
    override fun setValue(property: DslProperty<*>, value: T) = this@onSet.setValue(property.typed(listener),value.also(listener))
}

fun <T,K> LocalRWProperty<T>.remap(get:(T)->K,set:(K)->T) = object : LocalRWProperty<K> {
    override val identity = this@remap.identity
    override fun getValue(property: DslProperty<*>) = get(this@remap.getValue(property.typed2(get,set)))
    override fun setValue(property: DslProperty<*>, value: K) = this@remap.setValue(property.typed2(get,set),set(value))
}
fun <T,K> LocalROProperty<T>.remap(get:(T)->K) = object: LocalROProperty<K> {
    override val identity = this@remap.identity
    override fun getValue(property: DslProperty<*>) = get(this@remap.getValue(property.typed(get)))
}