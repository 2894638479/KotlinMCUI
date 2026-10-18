package io.github.u2894638479.kotlinmcui.entry

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

/*
 * Two stages, and only two.
 *
 * initialize*() runs while the mod is still loading: metadata is readable, the game client is not, so
 * this stage may read facts and register things - it must not open a window, start a service or build a
 * root. launch*() runs once the platform is published and can create screens: opening, starting and
 * building belong here.
 *
 * If a third stage ever looks necessary, argue first whether it really needs its own moment, or whether
 * it is one of these two in disguise.
 */

/**
 * Runs on the client side only. Suitable for rendering-related initialisation.
 */
interface DslEntryClient: DslEntryService {
    fun initializeClient()

    /** Platform published, screens can be built: open the client-side part. Default: nothing to do. */
    fun launchClient() {}
}

/**
 * Runs on the server side only. Suitable for server-specific logic.
 * Most mods will not need this interface.
 */
interface DslEntryServer: DslEntryService {
    fun initializeServer()

    /** Platform published: start the server-side part. Default: nothing to do. */
    fun launchServer() {}
}

/**
 * Runs on both client and server - and in [io.github.u2894638479.kotlinmcui.backend.Environment.NONE],
 * which is the "no game side" environment. Behaves similarly to an `init {}` block at initialisation,
 * and to a "start" hook at launch.
 */
interface DslEntryCommon: DslEntryService {
    fun initialize()

    /** Platform published, screens can be built: open the common part. Default: nothing to do. */
    fun launch() {}
}