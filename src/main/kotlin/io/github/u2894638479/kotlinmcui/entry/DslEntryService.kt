package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.DslBackendScreenHolder
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder

interface DslEntryService {
    val name: String
    val id: String
    val icon: ImageHolder
}

interface DslEntryOverlay: DslEntryService {
    context(ctx: DslContext)
    fun overlay()
}

/**
 * 仅用于注册gui
 */
interface DslEntryGui: DslEntryService {
    context(ctx: DslContext)
    fun content()
}

/**
 * 仅在客户端执行。适用于渲染相关初始化。
 */
interface DslEntryClient: DslEntryService {
    fun initializeClient()
}

/**
 * 仅在服务端执行。适用于服务端特定逻辑。一般不会用到此接口。
 */
interface DslEntryServer: DslEntryService {
    fun initializeServer()
}

/**
 * 在客户端和服务端都执行。适用于大多数逻辑初始化。效果接近于`init{}`块
 */
interface DslEntryCommon: DslEntryService {
    fun initialize()
}