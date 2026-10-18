package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope.TimedProperty
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Central data store for a screen instance.
 *
 * Holds the platform and the root it is mounted on, frame timing, and four [PropertyLifeScope]
 * instances, one per boundary. Their lifetimes only ever grow:
 *
 * - [localLifeScope]: one **frame** - a property not read this frame is disposed at its end
 * - [stableLifeScope]: one **screen** - survives frames in which the declaring code did not run,
 *   released by [dispose] when the screen closes
 * - [sessionLifeScope]: one **root** - shared by every screen mounted there, cleaned and emptied by
 *   `root.close()`; this is the scope for state that one screen sets and another reads
 * - [staticLifeScope]: the **process** - never released
 *
 * Global UI state (focus, hover, narration, IME) belongs to the root this screen is mounted on and is
 * reached through [rootState] rather than held here.
 *
 * Also provides a [debugOverlay] with performance metrics that report this screen's own focus/hover
 * state.
 */
class DslDataStore(
    /** The platform this screen's root runs on: process-level facts and helpers. */
    val platform: DslPlatform,
    /** The root this screen is mounted on: drawing, input, screens and per-output helpers all come from it. */
    val root: DslRoot,
    val title:String,
    val onClose:()-> Unit,
    dslFunction: DslFunction
): DslScaleContext {
    /**
     * The renderer of this screen's root, or `null` when the root draws nothing.
     *
     * Components that cannot exist without drawing (text, images) call [requireRenderer] instead, so
     * a renderer-less root fails where the impossible thing is asked for rather than drawing blank.
     */
    val renderer get() = root.renderer

    /** The renderer, or a loud failure: something that has to draw is attached to a root that cannot. */
    fun requireRenderer(): DslRootRenderer<*> =
        root.renderer ?: error("this screen's root has no renderer: text and images cannot be drawn on it")

    override val scale get() = root.renderer?.guiScale ?: 1.0

    var frameBeginNano = 0L
        private set

    var frameIndex = ULong.MAX_VALUE
        private set

    var inFrame = false
        private set

    /**
     * The state of the tree this screen is mounted on, or `null` while it is not mounted anywhere.
     *
     * This is the link a component follows to find its own tree: `DslRoot.children.collect` stamps it
     * on the screen's store at mounting time, and stamps it again on every later mount, so a screen
     * moved to another root answers from the new one. Removing a screen does not clear it - a hidden
     * screen is not a destroyed one.
     *
     * `null` therefore means "never mounted", and reading focus or hover from a component in that state
     * fails loudly rather than falling back to some other tree's state.
     */
    internal var rootState: DslRootState? = null

    companion object {
        /**
         * The process-wide `static` slots: one map shared by every root and screen.
         *
         * A **concurrent** map, not a locked one: after the split a process can render several roots
         * on different threads, and `static` slots are touched on every frame - a lock would serialise
         * all roots on the hottest path for no gain. Every operation below is a single-key map operation
         * (`get`, `put`, `remove`, `keys`), which is exactly the shape `ConcurrentHashMap` makes safe.
         *
         * Its keys are **bare ids**: unlike the per-screen scopes, the declaration site is not part of
         * the key, so two declaration sites using one id share a slot. See `stable()` on
         * `PropertyLifeScope` for the contract that follows ("same id, same type, per process").
         */
        internal val staticPropertyMap: ConcurrentHashMap<DslId, TimedProperty<*>> = ConcurrentHashMap()

        /**
         * The ids that hold a `static` slot right now, for the debug overlay.
         *
         * Internal on purpose: the overlay is a development tool, and `staticProps` is a count that
         * cannot answer "what is in there". Nothing outside the module needs this list.
         */
        internal val staticIds: Set<DslId> get() = staticPropertyMap.keys.toSet()
    }

    val stableLifeScope = PropertyLifeScope(this)
    val localLifeScope = PropertyLifeScope(this)
    /**
     * The process-wide scope: **kept for cross-root sharing and cross-mod contracts, not for UI state**.
     *
     * Its keys are bare ids, so any mod can address a slot by name - and that is also why nothing here is
     * ever released.
     *
     * - **Do not** put screen-local UI state here; that is [stableLifeScope] (screen) or [localLifeScope]
     *   (frame).
     * - **Do not** build keys dynamically: nothing is released, so every distinct key grows the map for
     *   the life of the process.
     * - **Do not** use it as per-connection state; that is [sessionLifeScope], which is bounded by a root.
     *
     * **Contract**: one id, one type, for the whole process. A static key does not carry the declaration
     * site, so two sites sharing an id share the slot, and reading it as the wrong type throws a
     * `ClassCastException`.
     */
    val staticLifeScope = PropertyLifeScope(this,true,staticPropertyMap)
    /** The root-bounded scope: shared by every screen on this root, released by oot.close(). */
    val sessionLifeScope = PropertyLifeScope(this, false, root.sessionPropertyMap, isSession = true)

    fun frame(action:(() -> Unit) -> Unit) = try {
        inFrame = true
        frameBeginNano = System.nanoTime()
        frameIndex++
        action {
            debugOverlay.dslRenderTimeMs = (System.nanoTime() - frameBeginNano) / 1e6
        }
    } finally {
        localLifeScope.clearOutdated()
        inFrame = false
        debugOverlay.frameRenderTimeMs = (System.nanoTime() - frameBeginNano) / 1e6
        debugOverlay.backendRenderTimeMs = debugOverlay.frameRenderTimeMs - debugOverlay.dslRenderTimeMs
    }

    /**
     * Release this screen's per-screen state, running the cleanup actions of every
     * remaining [stableLifeScope] and [localLifeScope] entry.
     *
     * The static scope is process-wide and is deliberately left alone.
     */
    fun dispose() {
        stableLifeScope.clear()
        localLifeScope.clear()
    }

    var pauseGame = true

    val dslScreen = DslScreen(this,dslFunction)

    val debugOverlay: DebugMetrics field = object: DebugMetrics {
        override val screenTitle get() = title
        override val guiScale get() = root.renderer?.guiScale ?: 1.0
        override val localProps get() = localLifeScope.size
        override val stableProps get() = stableLifeScope.size
        override val staticProps get() = staticLifeScope.size
        private val frameQueue = mutableListOf<Long>()
        private fun addFrame(timeNano: Long) {
            frameQueue.firstOrNull()?.let {
                if(it == timeNano) return
                frameTimeMs = (timeNano - it) / 1e6
                frameRate = 1e9 / (timeNano - it)
            }
            frameQueue.lastOrNull()?.let {
                frameTimeMsSmooth = (timeNano - it) / 1e6 / frameQueue.size
                frameRateSmooth = 1e9 / (timeNano - it) * frameQueue.size
            }
            frameQueue.add(0,timeNano)
            while(timeNano - frameQueue.last() > 1e9) {
                frameQueue.removeAt(frameQueue.lastIndex)
            }
        }
        override var frameRate = 0.0
            get() = field.also { addFrame(frameBeginNano) }
        override var frameTimeMs = 0.0
        override var frameRateSmooth = 0.0
        override var frameTimeMsSmooth = 0.0
        override var frameRenderTimeMs = 0.0
        override var dslRenderTimeMs = 0.0
        override var backendRenderTimeMs = 0.0
        override val components: Int get() {
            fun DslComponent.components(): Int = 1 + children.sumOf { it.components() }
            return dslScreen.components()
        }
        override val conflictId: DslId? = null
        // The overlay describes this screen's own tree. A screen that was never mounted has no root
        // state yet, so these read as "nothing focused / nothing hovered".
        override val focusedId get() = rootState?.focused
        override val hoveredId get() = rootState?.hovered
        override val focusedDepth: Int get() {
            return 0
        }
        override val hoveredDepth: Int get() {
            return 0
        }
        override val mouse get() = root.input.mouse
    }
}