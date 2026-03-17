package io.github.u2894638479.kotlinmcui.context

interface DslFrameContext {
    val frameIndex: ULong
    val frameBeginNano: Long
    companion object {
        operator fun invoke(frameIndex: ULong,frameBeginNano: Long) = object: DslFrameContext {
            override val frameIndex = frameIndex
            override val frameBeginNano = frameBeginNano
        }
        fun DslFrameContext.copy() = DslFrameContext(frameIndex,frameBeginNano)
    }
}