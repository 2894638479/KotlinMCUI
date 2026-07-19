package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.prop.mapView

/**
 * A mutable wrapper around a [DslComponent] in the child list.
 *
 * Supports the decorator pattern via [change] — wrapping a component's behaviour
 * without changing its identity. The [Group] interface is the managed collection
 * of children used throughout the tree.
 */
class DslChild(private var component: DslComponent) {
    fun currentComponent() = component
    fun change(component: DslComponent) = apply { this.component = component }
    inline fun change(action: (DslComponent)-> DslComponent) = change(action(currentComponent()))

    interface Group: kotlin.collections.List<DslComponent> {
        fun collect(child: DslComponent): DslChild
        fun collectAll(list: kotlin.collections.List<DslComponent>)
        fun remove(child: DslChild)
        companion object {
            val empty: Group get() = GroupImpl.empty
        }
    }

    companion object {
        fun Group(): Group = GroupImpl()
        fun Group.reverseRead(): Group = object : Group, kotlin.collections.List<DslComponent> by asReversed() {
            override fun collect(child: DslComponent) = this@reverseRead.collect(child)
            override fun collectAll(list: kotlin.collections.List<DslComponent>) {
               this@reverseRead.collectAll(list)
            }
            override fun remove(child: DslChild) = this@reverseRead.remove(child)
        }

        context(idCtx: DslIdContext, scaleCtx: DslScaleContext)
        inline fun Group.buildThis(ctx:DslContext, function: DslFunction) = context(ctx) {
            ctx.withIdentity(idCtx.identity) {
                ctx.withScale(scaleCtx.scale) {
                    ctx.withChildren(this) {
                        function()
                    }
                }
            }
        }
    }

    private class GroupImpl private constructor(private val mutList: MutableList<DslChild>): Group, kotlin.collections.List<DslComponent> by mutList.mapView({it.component}){
        constructor():this(mutableListOf())
        companion object {
            val empty = GroupImpl(object : AbstractMutableList<DslChild>() {
                override fun set(index: Int, element: DslChild): DslChild { error("empty DslChild.Group") }
                override fun removeAt(index: Int): DslChild { error("empty DslChild.Group") }
                override fun add(index: Int, element: DslChild) {}
                override val size get() = 0
                override fun get(index: Int): DslChild { error("empty DslChild.Group") }
            })
        }

        override fun collect(child: DslComponent) = DslChild(child).also { mutList += it }

        override fun collectAll(list: kotlin.collections.List<DslComponent>) {
            mutList.addAll(list.map { DslChild(it) })
        }

        override fun remove(child: DslChild) {
            if(!mutList.remove(child)) error("DslChild.Group.remove: cannot find this element")
        }
    }
}