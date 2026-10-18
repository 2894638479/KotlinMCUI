package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.dsl.dataStore

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

/**
 * Close this screen the way the backend does it: release its `stable`/`local` entries - running their
 * cleanups and cancelling the coroutine scopes they own - then hand control back.
 *
 * There is one closing path, not two: a DSL close that only told the backend would leave the screen's
 * properties alive and its coroutines running.
 */
context(ctx: DslExecuteContext)
fun closeScreen() = dataStore.dslScreen.close()
