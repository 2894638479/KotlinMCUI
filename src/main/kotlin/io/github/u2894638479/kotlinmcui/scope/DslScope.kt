package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.px

interface DslScope : DslComponent {
    val children: DslChild.List

    val alignerHorizontal: Aligner
    val alignerVertical: Aligner
    context(instance: DslComponent)
    override fun layoutHorizontal() {
        val rect = instance.rect
        alignerHorizontal.align(rect.left,rect.right,children.map { it.run { alignableHorizontal } })
        children.forEach { it.run { layoutHorizontal() } }
    }

    context(instance: DslComponent)
    override fun layoutVertical() {
        val rect = instance.rect
        alignerVertical.align(rect.top,rect.bottom,children.map { it.run { alignableVertical } })
        children.forEach { it.run { layoutVertical() } }
    }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) =
        children.asReversed().forEach { it.run { render(mouse) } }

    context(instance: DslComponent)
    override fun keyDown(key: Int, scanCode: Int, eventModifier: EventModifier) =
        children.firstOrNull { it.run { keyDown(key, scanCode, eventModifier) } } != null

    context(instance: DslComponent)
    override fun keyUp(key: Int, scanCode: Int, eventModifier: EventModifier) =
        children.firstOrNull { it.run { keyUp(key, scanCode, eventModifier) } } != null

    context(instance: DslComponent)
    override fun mouseDown(mouse: Position, mouseButton: MouseButton) =
        children.firstOrNull { it.run { mouseDown(mouse, mouseButton) } } != null

    context(instance: DslComponent)
    override fun mouseUp(mouse: Position, mouseButton: MouseButton) =
        children.firstOrNull { it.run { mouseUp(mouse, mouseButton) } } != null

    context(instance: DslComponent)
    override fun mouseMove(mouse: Position) =
        children.forEach { it.run { mouseMove(mouse) } }

    context(instance: DslComponent)
    override fun mouseScroll(mouse: Position, amount: Double): Double {
        var remain = amount
        children.forEach {
            if(remain == 0.0) return 0.0
            remain = it.run { mouseScroll(mouse, remain) }
        }
        return remain
    }

    context(instance: DslComponent)
    override fun charTyped(c: Char, eventModifier: EventModifier) =
        children.firstOrNull { it.run { charTyped(c, eventModifier) } } != null

    context(instance: DslComponent)
    override fun <T> testHit(mouse: Position, get: context(DslComponent) (DslComponent) -> T?): T? =
        children.firstNotNullOfOrNull { it.run { testHit(mouse, get) } } ?: super.testHit(mouse, get)

    context(instance: DslComponent)
    override fun <T> testHit(get: context(DslComponent) (DslComponent) -> T?): T? =
        children.firstNotNullOfOrNull { it.run { testHit(get) } } ?: super.testHit(get)

    context(instance: DslComponent)
    override fun focusChanged(newFocus: DslId?) = children.forEach { it.run { focusChanged(newFocus) } }

    override val viewHorizontal: List<List<DslComponent>> get() = children.groupBy { it.rect.run { left + right } }.toSortedMap().values.toList()
    override val viewVertical: List<List<DslComponent>> get() = children.groupBy { it.rect.run { top + bottom } }.toSortedMap().values.toList()
    override val viewSequential: List<DslComponent> get() = children
}

val DslScope.childrenSumWidth get() = children.sumOf { it.run { outerMinWidth.pixelsOrElse { 0.0 } } }.px
val DslScope.childrenSumHeight get() = children.sumOf { it.run { outerMinHeight.pixelsOrElse { 0.0 } } }.px

val DslScope.childrenMaxWidth get() = children.maxOfOrNull { it.run { outerMinWidth } } ?: 0.px
val DslScope.childrenMaxHeight get() = children.maxOfOrNull { it.run { outerMinHeight } } ?: 0.px