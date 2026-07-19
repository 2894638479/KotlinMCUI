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
 * An entry that appears in the GUI list. Used for registering screens.
 */
interface DslEntryGui: DslEntryService {
    context(ctx: DslContext)
    fun content()
}

/**
 * Runs on the client side only. Suitable for rendering-related initialisation.
 */
interface DslEntryClient: DslEntryService {
    fun initializeClient()
}

/**
 * Runs on the server side only. Suitable for server-specific logic.
 * Most mods will not need this interface.
 */
interface DslEntryServer: DslEntryService {
    fun initializeServer()
}

/**
 * Runs on both client and server. Suitable for most logic initialisation.
 * Behaves similarly to an `init {}` block.
 */
interface DslEntryCommon: DslEntryService {
    fun initialize()
}