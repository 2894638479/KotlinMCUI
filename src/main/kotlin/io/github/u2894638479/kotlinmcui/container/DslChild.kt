package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.prop.mapView

class DslChild(private var component: DslComponent) {
    fun currentComponent() = component
    fun change(component: DslComponent) = apply { this.component = component }
    inline fun change(action: (DslComponent)-> DslComponent) = change(action(currentComponent()))

    interface List:kotlin.collections.List<DslComponent> {
        fun collect(child: DslComponent): DslChild
        fun collectAll(list: kotlin.collections.List<DslComponent>)
        fun remove(child: DslChild)
        companion object {
            val empty: List get() = ListImpl.empty
        }
    }

    companion object {
        fun List(): List = ListImpl()
        fun List.reverseRead(): List = object: List,kotlin.collections.List<DslComponent> by asReversed() {
            override fun collect(child: DslComponent) = this@reverseRead.collect(child)
            override fun collectAll(list: kotlin.collections.List<DslComponent>) {
               this@reverseRead.collectAll(list)
            }
            override fun remove(child: DslChild) = this@reverseRead.remove(child)
        }

        context(idCtx: DslIdContext, scaleCtx: DslScaleContext)
        inline fun List.buildThis(ctx:DslContext, function: DslFunction) = context(ctx) {
            ctx.withIdentity(idCtx.identity) {
                ctx.withScale(scaleCtx.scale) {
                    ctx.withChildren(this) {
                        function()
                    }
                }
            }
        }
    }

    private class ListImpl private constructor(private val mutList: MutableList<DslChild>):List,kotlin.collections.List<DslComponent> by mutList.mapView({it.component}){
        constructor():this(mutableListOf())
        companion object {
            val empty = ListImpl(object : AbstractMutableList<DslChild>() {
                override fun set(index: Int, element: DslChild): DslChild { error("empty DslChild.List") }
                override fun removeAt(index: Int): DslChild { error("empty DslChild.List") }
                override fun add(index: Int, element: DslChild) {}
                override val size get() = 0
                override fun get(index: Int): DslChild { error("empty DslChild.List") }
            })
        }

        override fun collect(child: DslComponent) = DslChild(child).also { mutList += it }

        override fun collectAll(list: kotlin.collections.List<DslComponent>) {
            mutList.addAll(list.map { DslChild(it) })
        }

        override fun remove(slot: DslChild) {
            if(!mutList.remove(slot)) error("DslChild.List.remove: cannot find this element")
        }
    }
}