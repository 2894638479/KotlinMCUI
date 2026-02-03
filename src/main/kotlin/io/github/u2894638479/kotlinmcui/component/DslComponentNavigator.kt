package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.identity.DslId

interface DslComponentNavigator {
    val viewHorizontal: List<List<DslComponent>>
    val viewVertical: List<List<DslComponent>>
    val viewSequential: List<DslComponent>
}

fun DslComponent.nextFocusableList(focused: DslId?, reverse:Boolean = false, view:(DslComponentNavigator) -> List<List<DslComponent>>): DslComponent? {
    var hit = false
    fun <T> List<T>.rev() = if (reverse) asReversed() else this
    fun DslComponent.iterate(): DslComponent? {
        for (list in view(this).rev()) {
            for(it in list.rev()) {
                if (it.identity == focused) {
                    hit = true
                    break
                }
                it.iterate()?.let { return it }
                if(hit && it.focusable) return it
            }
        }
        return null
    }
    iterate()?.let { return it }
    fun DslComponent.firstFocusable(): DslComponent? {
        for (list in view(this).rev()) {
            for(it in list.rev()) {
                it.firstFocusable()?.let { return it }
                if(it.focusable) return it
            }
        }
        return null
    }
    return firstFocusable()
}

fun DslComponent.nextFocusable(focused: DslId?, view:(DslComponentNavigator) -> List<DslComponent>): DslComponent? {
    fun DslComponent.flattenView():List<DslComponent> = listOf(this) + view(this).flatMap { it.flattenView() }
    val flatten = flattenView()
    val index = flatten.indexOfFirst { it.identity == focused }
    return flatten.drop(index + 1).firstOrNull { it.run { focusable } }
}