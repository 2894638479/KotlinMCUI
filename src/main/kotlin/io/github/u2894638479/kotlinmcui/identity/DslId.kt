package io.github.u2894638479.kotlinmcui.identity

class DslId(private val obj: Any?,val prevNode: DslId? = null) : Iterable<DslId> {
    private var init = false
    private var hash = 0
        get() {
            if(!init) {
                field = prevNode.hashCode() * 31 + obj.hashCode()
                init = true
            }
            return field
        }
    override fun equals(other: Any?) = other is DslId && obj == other.obj && prevNode == other.prevNode
    override fun hashCode() = hash
    operator fun plus(other: Any?) = DslId(other,this)
    override fun toString() = "DslId{size=${size}, objs=${map { it.obj }.joinToString()}}"
    val size:Int get() = prevNode?.size?.plus(1) ?: 1
    val topElement get() = obj
    override fun iterator() = object : Iterator<DslId> {
        var node: DslId? = this@DslId
        override fun hasNext() = node != null
        override fun next() = node!!.also { node = node!!.prevNode }
    }
}

private class RefWrapper<T>(val ref: T) {
    override fun equals(other: Any?) = this === other || (other is RefWrapper<*> && ref === other.ref)
    override fun hashCode() = System.identityHashCode(ref)
}

val <T> T.refId: Any get() = RefWrapper(this)