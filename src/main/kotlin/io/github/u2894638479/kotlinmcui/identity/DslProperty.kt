package io.github.u2894638479.kotlinmcui.identity

import kotlin.reflect.KProperty

class DslProperty<T> (
    val kProp: KProperty<T>,
    val identity: DslId
) {
    override fun equals(other: Any?) = other is DslProperty<T> && kProp == other.kProp && identity == other.identity
    override fun hashCode(): Int {
        var result = identity.hashCode()
        result = 31 * result + kProp.hashCode()
        return result
    }
    val name by kProp::name

    override fun toString() = "DslProperty{kProp=$kProp,name=$name,identity=$identity}"
}