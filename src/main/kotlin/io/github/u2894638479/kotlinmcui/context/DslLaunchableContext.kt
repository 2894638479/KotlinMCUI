package io.github.u2894638479.kotlinmcui.context

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

@DslContextMarker
interface DslLaunchableContext: CoroutineScope {
    fun destroy()
    companion object {
        operator fun invoke(_dispatcher: CoroutineDispatcher) = object : DslLaunchableContext {
            private var dispatcher: CoroutineDispatcher? = _dispatcher
            private var _value: CoroutineContext? = null
            override val coroutineContext get() = _value ?: synchronized(this) {
                val dispatcher = dispatcher ?: error("context is already destroyed")
                _value ?: CoroutineScope(dispatcher).coroutineContext.also { _value = it }
            }
            override fun destroy() = synchronized(this) {
                if(dispatcher == null) error("context is already destroyed")
                _value?.cancel(CancellationException("context destroyed"))
                dispatcher = null
                _value = null
            }
        }
    }
}