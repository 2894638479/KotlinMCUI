package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.center
import io.github.u2894638479.kotlinmcui.math.rect.ifEmpty

/**
 * Focus traversal navigation for a component tree.
 *
 * Each view defines a different navigation model:
 * - [viewHorizontal] — 2D grid for left/right arrow key navigation.
 * - [viewVertical] — 2D grid for up/down arrow key navigation.
 * - [viewSequential] — flat list for Tab / Shift+Tab navigation.
 */
interface DslComponentNavigator {
    val viewHorizontal: List<List<DslComponent>>
    val viewVertical: List<List<DslComponent>>
    val viewSequential: List<DslComponent>
}

/**
 * Find the next focusable component in a 2D navigation view.
 *
 * Traverses the component tree depth-first using [view]. When [reverse] is true,
 * traversal order is reversed.
 *
 * @param focused The identity of the currently focused component, or `null`.
 * @param reverse If `true`, traverse in reverse direction.
 * @param view Extracts the 2D navigation view from a [DslComponentNavigator].
 * @return The next focusable component, or `null` if none is found.
 */
fun DslComponent.nextFocusable(focused: DslId?, reverse:Boolean = false, view:(DslComponentNavigator) -> List<List<DslComponent>>): DslComponent? {
    var center: Position? = null
    fun <T> List<T>.rev() = if (reverse) asReversed() else this
    fun DslComponent.iterate(): DslComponent? {
        for (list in view(this).rev()) {
            val list = center?.let { center ->
                list.sortedBy { it.rect.ifEmpty { return@sortedBy Double.MAX_VALUE.px }.center.distanceTo(center) }
            } ?: list.rev()
            for(it in list) {
                if (center == null && it.identity == focused) {
                    center = it.rect.center
                    break
                }
                it.iterate()?.let { return it }
                if(center != null && it.focusable) return it
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

/**
 * Find the next focusable component in a flat sequential navigation view.
 *
 * Flattens the tree using [view] and returns the first focusable component
 * after the currently focused one. Wraps around by returning `null` when
 * nothing is found (caller should then reset to the first focusable).
 *
 * @param focused The identity of the currently focused component, or `null`.
 * @param view Extracts the flat list view from a [DslComponentNavigator].
 * @return The next focusable component, or `null`.
 */
fun DslComponent.nextFocusable(focused: DslId?, view:(DslComponentNavigator) -> List<DslComponent>): DslComponent? {
    fun DslComponent.flattenView(): List<DslComponent> = buildList {
        fun flatten(component: DslComponent) {
            for (child in view(component)) {
                flatten(child)
            }
            add(component)
        }
        flatten(this@nextFocusable)
    }
    val flatten = flattenView()
    val index = flatten.indexOfFirst { it.identity == focused }
    return flatten.drop(index + 1).firstOrNull { it.focusable }
}
