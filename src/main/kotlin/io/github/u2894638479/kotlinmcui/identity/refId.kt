package io.github.u2894638479.kotlinmcui.identity

private class RefWrapper<T>(val ref: T) {
    override fun equals(other: Any?) = this === other || (other is RefWrapper<*> && ref === other.ref)
    override fun hashCode() = System.identityHashCode(ref)
}

val <T> T.refId: Any get() = RefWrapper(this)