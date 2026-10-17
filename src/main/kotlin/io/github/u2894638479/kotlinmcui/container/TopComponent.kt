package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.input.PreeditInfo
import io.github.u2894638479.kotlinmcui.math.Position

/**
 * The singleton root of the entire component tree.
 *
 * The view state ([focused], [hovered], [preedit], the narration, the frame clock and
 * [renderFailure]) lives in the main root's [DslRootState], and the viewport in its
 * [DslRootGeometry]; both are only projected here, so those slots are no longer process-wide.
 *
 * The tree itself is walked by the root's component ([DslRoot.component]); this object forwards to
 * it, which is a stepping stone rather than a shape worth keeping.
 *
 * ### Backend convention
 * The backend must, each frame, set [rect] to the full screen dimensions
 * and call [DslComponent.render] on this component. The render method copies
 * the rect to all overlay children before dispatching rendering.
 *
 * [modifier], [identity], and [scale] are not meaningful at the root level
 * and will throw if accessed.
 */
object TopComponent: DslScope {
    private val root: DslRoot = DslRoot(
        state = DslRootState(
            onFocusChanged = { newFocus, source -> globalFocusChanged(newFocus, source) },
            onHoverChanged = { globalHoverChanged(it) },
            onNarrate = { dslBackend.narrate(it) }
        ),
        geometry = DslRootGeometry()
    )

    private val component: DslScope get() = root.component

    override val rect get() = component.rect
    override val modifier get() = component.modifier
    override var instance: DslComponent
        get() = component.instance
        set(value) { component.instance = value }
    override val identity get() = component.identity
    override val scale get() = component.scale
    override val children get() = component.children

    override fun globalFocusChanged(newFocus: DslId?, source: FocusSource) =
        component.globalFocusChanged(newFocus, source)

    override fun globalHoverChanged(newHover: DslId?) = component.globalHoverChanged(newHover)

    /**
     * The state of the main root, which this object projects as [focused], [hovered], [preedit],
     * [keyboardNarration], [mouseNarration], [frameIndex] and [renderFailure].
     */
    internal val rootState get() = root.state

    /** The identity of the main root's focused component, or `null`. */
    val focused: DslId? get() = rootState.focused

    /**
     * Move the focus to the component with this [DslId], as if the user had done it; `null`
     * clears the focus.
     *
     * This is the low-level entry point the mouse and keyboard paths use too, so it does not
     * filter by [DslComponentMetadata.focusable]: a target that cannot take focus is still
     * recorded, it just keeps reporting itself unfocused. The change is broadcast to the whole
     * tree ([DslComponentGlobalEvent.globalFocusChanged], children before parents), and the
     * identity is compared structurally, so one captured on an earlier frame still matches the
     * component rebuilt this frame.
     */
    fun setFocus(newFocus: DslId?, source: FocusSource = FocusSource.CODE) {
        rootState.setFocus(newFocus, source)
    }
    var hovered: DslId?
        get() = rootState.hovered
        internal set(value) { rootState.setHovered(value) }
    var keyboardNarration: String?
        get() = rootState.keyboardNarration
        internal set(value) { rootState.noteKeyboardNarration(value) }
    var mouseNarration: String?
        get() = rootState.mouseNarration
        internal set(value) { rootState.noteMouseNarration(value) }
    val mouse get() = dslBackend.mouse

    /** The current IME preedit state. Updated by [globalPreedit]. */
    var preedit: PreeditInfo
        get() = rootState.preedit
        internal set(value) { rootState.setPreedit(value) }

    /** Whether the currently focused component accepts IME input (has a non-null [DslComponentMetadata.preeditArea]). */
    val preeditUsing get() = testHit { it.takeIf { it !== this && it.identity == focused } }?.preeditArea != null

    context(eventModifier: KeyModifiers, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton) = component.mouseDown(mouseButton)

    context(eventModifier: KeyModifiers)
    override fun keyDown(key: Key, scanCode: Int) = component.keyDown(key, scanCode)

    /**
     * How many frames the platform has rendered. It stays zero until the game is past its loading
     * overlay, which makes it both "the platform can show a screen now" and the frame clock a
     * preview session waits on.
     */
    internal val frameIndex: Long get() = rootState.frameIndex

    /** The last exception a page threw while rendering, kept so a preview session can report it. */
    internal var renderFailure: Throwable?
        get() = rootState.renderFailure
        set(value) { rootState.noteRenderFailure(value) }

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() = component.render<RP>()

    override fun globalPreedit(info: PreeditInfo) = component.globalPreedit(info)
}
