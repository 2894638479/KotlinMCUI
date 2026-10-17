package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope.TimedProperty
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

/**
 * Central data store for a screen instance.
 *
 * Holds the backend reference, frame timing, and three [PropertyLifeScope]
 * instances with different lifetimes:
 * - [localLifeScope]: cleared every frame (properties not accessed this frame are disposed)
 * - [stableLifeScope]: survives while the screen lives, even across frames in which
 *   the declaring UI code did not run; released by [dispose] when the screen closes
 * - [staticLifeScope]: persists for the lifetime of the process and is never released
 *
 * Global UI state (focus, hover, narration, IME) is reached through
 * [TopComponent] rather than held here, since it belongs to the main root and
 * is shared by every screen mounted on it.
 *
 * Also provides a [debugOverlay] with performance metrics referencing
 * [TopComponent] for current focus/hover state.
 */
class DslDataStore(
    val backend: DslBackend<*, *>,
    val title:String,
    val onClose:()-> Unit,
    dslFunction: DslFunction
): DslScaleContext {
    override val scale get() = backend.guiScale

    var frameBeginNano = 0L
        private set

    var frameIndex = ULong.MAX_VALUE
        private set

    var inFrame = false
        private set

    /**
     * The focus and hover state of the tree this screen belongs to, or `null` while nothing has
     * claimed it.
     *
     * This is the link a component follows to find its own tree. Mounting does not wire it yet, so it
     * stays `null` and components fall back to the main root's state; once it is set, every component
     * this screen built answers from here.
     */
    internal var rootState: DslRootState? = null

    companion object {
        private val staticPropertyMap = Object2ObjectOpenHashMap<DslId, TimedProperty<*>>()
    }

    val stableLifeScope = PropertyLifeScope(this)
    val localLifeScope = PropertyLifeScope(this)
    val staticLifeScope = PropertyLifeScope(this,true,staticPropertyMap)

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
        override val guiScale get() = backend.guiScale
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
        override val focusedId get() = TopComponent.focused
        override val hoveredId get() = TopComponent.hovered
        override val focusedDepth: Int get() {
            return 0
        }
        override val hoveredDepth: Int get() {
            return 0
        }
        override val mouse get() = backend.mouse
    }
}