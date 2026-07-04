package io.github.u2894638479.kotlinmcui

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope.TimedProperty
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

    fun frame(action:() -> Unit) = try {
        inFrame = true
        frameBeginNano = System.nanoTime()
        frameIndex++
        action()
    } finally {
        localLifeScope.clearOutdated()
        inFrame = false
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
    var mouse = Position(-1.px,-1.px)
    var tooltipVisible = true

    val dslScreen = DslScreen(this,dslFunction)
}