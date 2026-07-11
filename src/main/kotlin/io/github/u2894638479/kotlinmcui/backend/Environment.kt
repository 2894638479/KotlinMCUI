package io.github.u2894638479.kotlinmcui.backend

enum class Environment {
    /**
     * 表示环境中存在CLIENT特定代码，可确保[io.github.u2894638479.kotlinmcui.entry.DslEntryClient.initializeClient]加载并执行时不因缺少类而崩溃
     */
    CLIENT,

    /**
     * 表示环境中存在SERVER特定代码，可确保[io.github.u2894638479.kotlinmcui.entry.DslEntryServer.initializeServer]加载并执行时不因缺少类而崩溃
     */
    SERVER,

    /**
     * 表示环境中只可确保[io.github.u2894638479.kotlinmcui.entry.DslEntryCommon.initialize]加载并执行时不因缺少类而崩溃
     */
    COMMON
}