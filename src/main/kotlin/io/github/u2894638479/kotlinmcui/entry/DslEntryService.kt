package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder

/**
 * One entry point declared by a mod: its identity, and the two stages it can run in.
 *
 * Two stages, and only two. [initialize] runs while the mod is still loading: metadata is readable, the
 * game client is not, so it may read facts and register things - it must not open a window, start a
 * service or build a root. [launch] runs once the platform is published and can create screens: opening,
 * starting and building belong there.
 *
 * If a third stage ever looks necessary, argue first whether it really needs its own moment, or whether it
 * is one of these two in disguise.
 *
 * An entry is one class; what it *is* - a gui page, an overlay - is decided by which of the kind
 * interfaces below it implements, not by which file listed it.
 */
interface DslEntryService {
    val name: String
    val id: String
    val icon: ImageHolder

    /** Load time: metadata only. Default: nothing to do. */
    fun initialize() {}

    /** Platform published, screens can be built. Default: nothing to do. */
    fun launch() {}
}

/** An entry that appears in the GUI list. */
interface DslEntryGui : DslEntryService {
    context(ctx: DslContext)
    fun gui()
}

/** An entry that draws an overlay layer. */
interface DslEntryOverlay : DslEntryService {
    context(ctx: DslContext)
    fun overlay()
}
