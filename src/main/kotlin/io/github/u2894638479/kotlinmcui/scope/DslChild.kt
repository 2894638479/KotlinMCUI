package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.prop.mapView
import java.util.function.IntFunction

class DslChild(private var component: DslComponent) {
    fun currentComponent() = component
    fun change(component: DslComponent) = apply { this.component = component }
    inline fun change(action: (DslComponent)-> DslComponent) = change(action(currentComponent()))

    class List private constructor(private val mutList: MutableList<DslChild>):kotlin.collections.List<DslComponent> by mutList.mapView({it.component}){
        @Deprecated("")
        override fun <T> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?>? = super.toArray(generator)
        constructor():this(mutableListOf())

        fun collect(child: DslComponent) = DslChild(child).also { mutList += it }

        fun remove(slot: DslChild) {
            if(!mutList.remove(slot)) error("DslChildren.remove: cannot find this element")
        }

        fun clear() = mutList.clear()

        fun <R : Comparable<R>> sortBy(selector: (DslComponent) -> R?) =
            mutList.sortBy { selector(it.component) }
    }
}