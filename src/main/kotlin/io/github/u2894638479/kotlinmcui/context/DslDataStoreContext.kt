package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.DslDataStore

interface DslDataStoreContext {
    val dataStore: DslDataStore
    companion object {
        operator fun invoke(dataStore: DslDataStore) = object: DslDataStoreContext {
            override val dataStore = dataStore
        }
    }
}