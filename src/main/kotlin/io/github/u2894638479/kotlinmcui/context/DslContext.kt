package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.DslDataStore
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.scope.DslChild

@DslContextMarker
open class DslContext(
    override val dataStore: DslDataStore,
): DslScaleContext, DslDataStoreContext, DslIdContext, DslChildrenContext {
    @PublishedApi
    internal val thread = Thread.currentThread()
    @PublishedApi
    internal fun <T> T.checkThread() = apply {
        check(Thread.currentThread() === thread) {
            "current thread ${Thread.currentThread()}, should be $thread"
        }
    }
    @PublishedApi
    internal var _identity: DslId? = null
    @PublishedApi
    internal var _children: DslChild.List? = null
    @PublishedApi
    internal var _scale: Double = Double.NaN
    @PublishedApi
    internal var _overlays: DslChild.List? = null
    @PublishedApi
    internal inline fun <T,R> withField(get:() -> T, set:(T) -> Unit,value: T,block:() -> R): R {
        checkThread()
        val orig = get()
        try {
            set(value)
            return block()
        } finally {
            set(orig)
        }
    }
    override val identity get() = _identity.checkThread() ?: error("undefined identity")
    override val children get() = _children.checkThread() ?: error("undefined children")
    override val scale get() = _scale.checkThread().also { require(!_scale.isNaN()) { "undefined scale" } }
    val overlays get() = _overlays.checkThread() ?: error("undefined overlays")
    inline fun <R> withIdentity(identity:DslId, block: () -> R) = withField({_identity},{_identity = it},identity,block)
    inline fun <R> withChildren(children:DslChild.List, block: () -> R) = withField({_children},{_children = it},children,block)
    inline fun <R> withScale(scale:Double, block: () -> R) = require(!scale.isNaN()).run { withField({_scale},{_scale = it},scale,block) }
    inline fun <R> withOverlays(overlays:DslChild.List, block: () -> R) = withField({_overlays},{_overlays = it},overlays,block)
}