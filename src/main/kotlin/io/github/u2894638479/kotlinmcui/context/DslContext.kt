package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.DslDataStore
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.scope.DslChild

@DslContextMarker
class DslContext(
    override val identity: DslId,
    override val dataStore: DslDataStore,
    override val children: DslChild.List,
    val scaleScope: DslScaleContext
): DslScaleContext by scaleScope, DslDataStoreContext, DslIdContext, DslChildrenContext {
    fun change(
        dslIdentity: DslId = this.identity,
        dslDataStore: DslDataStore = this.dataStore,
        dslChildren: DslChild.List = this.children,
        scaleScope: DslScaleContext = this.scaleScope
    ) = DslContext(dslIdentity,dslDataStore,dslChildren,scaleScope)
}