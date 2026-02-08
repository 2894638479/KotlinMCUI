package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.DslDataStore
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.scope.DslChild

@DslContextMarker
class DslTopContext(
    identity: DslId,
    dataStore: DslDataStore,
    children: DslChild.List,
    scaleScope: DslScaleContext,
    val onOnCloseEmit: (context(DslDataStoreContext) DslOnCloseContext.() -> Unit) -> Unit
): DslContext(identity,dataStore,children,scaleScope)

context(ctx: DslTopContext)
fun onClose(block:context(DslDataStoreContext) DslOnCloseContext.() -> Unit) = ctx.onOnCloseEmit(block)