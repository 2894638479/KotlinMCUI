package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position

/**
 * Move the focus to whatever this scope's own tree has under [mouse], the way a mouse press does.
 *
 * "Who takes the focus on a press" is not root-specific: any root can run the same lookup against its
 * own tree, which is why it lives here rather than inside one implementation. The press records
 * [FocusSource.MOUSE] where the keyboard path records [FocusSource.KEYBOARD], so an observer can tell
 * a click from a TAB.
 *
 * [setFocus] is the root's own writer: the main root passes its state's, another root passes its own.
 * The lookup deliberately filters on nothing else, so a component that cannot take focus is still
 * recorded - it just keeps reporting itself unfocused.
 */
context(mouse: Position)
fun DslScope.acquireFocusFromInput(setFocus: (DslId?, FocusSource) -> Unit) {
    setFocus(instance.testHit(mouse) { it.takeIf { it.focusable } }?.identity, FocusSource.MOUSE)
}
