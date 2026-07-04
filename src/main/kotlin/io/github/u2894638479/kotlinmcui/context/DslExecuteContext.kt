package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.DslDataStore
import io.github.u2894638479.kotlinmcui.functions.dataStore

@DslContextMarker
interface DslExecuteContext : DslDataStoreContext {
    companion object {
        operator fun invoke(dataStore: DslDataStore) = object: DslExecuteContext {
            override val dataStore = dataStore
        }
        context(ctx: DslDataStoreContext)
        operator fun invoke() = invoke(ctx.dataStore)
    }
}

context(ctx: DslExecuteContext)
fun closeScreen() = dataStore.onClose()
