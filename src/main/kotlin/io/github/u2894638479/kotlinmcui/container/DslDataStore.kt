package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope.TimedProperty
import io.github.u2894638479.kotlinmcui.test.DebugOverlay
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

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

    var pauseGame = true

    var debug = false

    var focused: DslId? = null
        set(value) {
            field = value
            dslScreen.run { globalFocusChanged(value) }
        }
    var hovered: DslId? = null
        set(value) {
            field = value
            dslScreen.run { globalHoverChanged(value) }
        }
    var keyboardNarration: String? = null
        set(value) {
            if(value != field) {
                field = value
                value?.let { backend.narrate(it) }
            }
        }
    var mouseNarration: String? = null
        set(value) {
            if(value != field) {
                field = value
                value?.let { backend.narrate(it) }
            }
        }
    val mouse get() = backend.mouse

    val dslScreen = DslScreen(this,dslFunction)

    val debugOverlay: DebugOverlay field = object: DebugOverlay {
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
        override val focusedId get() = focused
        override val hoveredId get() = hovered
        override val focusedDepth: Int get() {
            return 0
        }
        override val hoveredDepth: Int get() {
            return 0
        }
        override val mouse get() = backend.mouse
    }
}