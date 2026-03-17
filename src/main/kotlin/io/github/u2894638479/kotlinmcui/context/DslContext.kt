package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.DslDataStore
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.scope.DslChild

@DslContextMarker
open class DslContext(
    override val identity: DslId,
    override val dataStore: DslDataStore,
    override val children: DslChild.List,
    val scaleContext: DslScaleContext,
    val frameContext: DslFrameContext
): DslScaleContext by scaleContext, DslDataStoreContext, DslIdContext, DslChildrenContext, DslFrameContext by frameContext {
    fun change(
        identity: DslId = this.identity,
        dataStore: DslDataStore = this.dataStore,
        children: DslChild.List = this.children,
        scaleContext: DslScaleContext = this.scaleContext,
        frameContext: DslFrameContext = this.frameContext
    ) = DslContext(identity,dataStore,children,scaleContext,frameContext)
}