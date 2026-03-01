package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.prop.mapView

class DslChild(private var component: DslComponent) {
    fun currentComponent() = component
    fun change(component: DslComponent) = apply { this.component = component }
    inline fun change(action: (DslComponent)-> DslComponent) = change(action(currentComponent()))

    class List private constructor(private val mutList: MutableList<DslChild>):kotlin.collections.List<DslComponent> by mutList.mapView({it.component}){
        constructor():this(mutableListOf())
        companion object {
            val empty = List(object : AbstractMutableList<DslChild>() {
                override fun set(index: Int, element: DslChild): DslChild { error("empty DslChild.List") }
                override fun removeAt(index: Int): DslChild { error("empty DslChild.List") }
                override fun add(index: Int, element: DslChild) {}
                override val size get() = 0
                override fun get(index: Int): DslChild { error("empty DslChild.List") }
            })
        }

        fun collect(child: DslComponent) = DslChild(child).also { mutList += it }

        fun remove(slot: DslChild) {
            if(!mutList.remove(slot)) error("DslChild.List.remove: cannot find this element")
        }

        fun clear() = mutList.clear()

        context(idCtx: DslIdContext)
        fun buildThis(ctx: DslContext, function: DslFunction) = context(ctx.change(identity = idCtx.identity, children = this),function)

        fun <R : Comparable<R>> sortBy(selector: (DslComponent) -> R?) =
            mutList.sortBy { selector(it.component) }
    }
}