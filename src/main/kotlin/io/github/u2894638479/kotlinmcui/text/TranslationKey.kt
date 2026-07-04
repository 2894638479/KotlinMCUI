package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.dslBackend
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


data class KeyInfo(
    val name: String,
    val parents: List<String>
) {
    val path = parents.joinToString(".") + "." + name
}

interface TranslationKey {
    fun getInfo() : KeyInfo
}

interface ParentKey: TranslationKey {
    fun spawn(name: String) = KeyInfo(name,getInfo().let { it.parents + it.name })
}

interface TranslatableKey: TranslationKey {
    fun getPath() = getInfo().path
    operator fun invoke(vararg args: Any?) = dslBackend.translate(getPath(),*args) ?: getPath()
}

abstract class BaseKey : TranslationKey {
    private var `kotlinmcui$info`: KeyInfo? = null
    override fun getInfo() = `kotlinmcui$info` ?: error("TranslationKey not initialized")
    fun init(info: KeyInfo) {
        if(`kotlinmcui$info` != null) error("TranslationKey init twice")
        `kotlinmcui$info` = info
    }
}

abstract class BaseParentKey() : ParentKey, BaseKey()
abstract class BaseTranslatableKey() : TranslatableKey, BaseKey()
abstract class BaseTranslatableParentKey() : ParentKey, TranslatableKey, BaseKey()

val top: ParentKey = object: ParentKey, BaseKey() {
    override fun spawn(name: String) = KeyInfo(name,emptyList())
}


class KeyProperty<T: BaseKey>(val key: T,val string: String? = null): ReadOnlyProperty<Any?, T> {
    var initialized = false
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!initialized) {
            initialized = true
            val parent = (thisRef as? ParentKey) ?: top
            key.init(parent.spawn(string ?: property.name))
        }
        return key
    }
    infix fun <T: BaseKey> type(type: T) = KeyProperty(type,string)
}


class SimpleKey : BaseTranslatableKey()
val name = KeyProperty(SimpleKey())
fun string(string: String) = KeyProperty(SimpleKey(),string)
fun <K: BaseKey> type(key:K) = KeyProperty(key)




val kotlinmcui by type(KotlinMCUIKey())

class KotlinMCUIKey : BaseParentKey() {
    val testpage by string("testpage")
    val testentry by name type SimpleKey()
    val test_translate by name
    val layout by name
    val text by name
    val image by name
    val scroll by name
    val slider by name
    val translation by name
    val id by name
    val container by name
    val mousetip by name
    val screen by name
    val metadata by name
    val rotation by name
    val animation by name
    val narration by name type NarrationKey()

    class NarrationKey : BaseParentKey() {
        val button by name
        val row by name
        val column by name
        val scrollablecolumn by name
        val slider by name
        val scroller by name
        val text by name
        val editabletext by name
    }
}