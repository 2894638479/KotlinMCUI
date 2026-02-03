package io.github.u2894638479.kotlinmcui.prop

class ContextLazy<C,T>(initializer:context(C) ()->T) {
    private var initializer:(context(C)()->T)? = initializer
    private var t:T? = null
    context(c:C)
    val value get() = t ?: initializer!!().also {
        t = it
        initializer = null
    }
}