package io.github.u2894638479.kotlinmcui.container

/**
 * Returns a lazy view of this list with each element transformed by [map].
 *
 * Unlike [kotlin.collections.map], this does not allocate a new list — the
 * transformation is applied on each [get] call. Useful for avoiding allocation
 * when the mapped view is consumed immediately.
 */
internal fun <T,K> List<T>.mapView(map:(T)->K): List<K> = object: AbstractList<K>() {
    private val origList = this@mapView
    override val size get() = origList.size
    override fun get(index: Int) = map(origList[index])
}