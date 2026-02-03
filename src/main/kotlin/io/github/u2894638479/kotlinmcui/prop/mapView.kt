package io.github.u2894638479.kotlinmcui.prop

fun <T,K> List<T>.mapView(map:(T)->K): List<K> = object: AbstractList<K>() {
    private val origList = this@mapView
    override val size get() = origList.size
    override fun get(index: Int) = map(origList[index])
}