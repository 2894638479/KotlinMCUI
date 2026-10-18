package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder

/** Shared identity for the loader fixtures; only the loading behaviour is under test. */
internal object FixtureIdentity : DslEntryService {
    override val name get() = "fixture"
    override val id get() = "fixture"
    override val icon get() = ImageHolder.empty
}

/**
 * What the fixtures recorded: which stage ran on which class, in order.
 *
 * Counters live outside the fixtures on purpose - reading an assertion must not itself load a fixture.
 */
internal object FixtureStats {
    val stages = mutableListOf<String>()
    var loads = 0
}

/**
 * A class that is declared only for standalone, so a client/server run must never load it.
 * [FixtureStats.loads] is bumped from its static initialiser: a count above zero proves the class was
 * loaded when it should not have been.
 */
internal class FixtureStandaloneOnly : DslEntryGui, DslEntryService by FixtureIdentity {
    init { FixtureStats.loads++ }

    override fun initialize() { FixtureStats.stages += "initialize:standalone" }
    override fun launch() { FixtureStats.stages += "launch:standalone" }

    context(ctx: DslContext)
    override fun gui() {}
}

internal object FixturePage : DslEntryGui, DslEntryService by FixtureIdentity {
    override fun initialize() { FixtureStats.stages += "initialize:page" }
    override fun launch() { FixtureStats.stages += "launch:page" }

    context(ctx: DslContext)
    override fun gui() {}
}

internal object FixtureOverlay : DslEntryOverlay, DslEntryService by FixtureIdentity {
    override fun initialize() { FixtureStats.stages += "initialize:overlay" }
    override fun launch() { FixtureStats.stages += "launch:overlay" }

    context(ctx: DslContext)
    override fun overlay() {}
}

/** Both kinds in one class: the page entry shows it as gui and overlay. */
internal object FixtureBoth : DslEntryGui, DslEntryOverlay, DslEntryService by FixtureIdentity {
    override val id get() = "fixture-both"
    context(ctx: DslContext)
    override fun gui() {}

    context(ctx: DslContext)
    override fun overlay() {}
}

/** A public constructor that throws: the entry must be skipped, never substituted. */
internal class FixtureThrowingConstructor : DslEntryGui, DslEntryService by FixtureIdentity {
    init { error("constructor failure") }

    context(ctx: DslContext)
    override fun gui() {}
}

/** A Kotlin object whose static initialiser fails with an [Error] rather than an exception. */
internal object FixtureBrokenStaticInit : DslEntryGui, DslEntryService by FixtureIdentity {
    init { error("static initialiser failure") }

    context(ctx: DslContext)
    override fun gui() {}
}

/** Not a service at all: listed in the registry, must land in rokenEntries. */
internal class FixtureNotAService

internal object FixtureGuiClass : DslEntryGui, DslEntryService by FixtureIdentity {
    context(ctx: DslContext)
    override fun gui() {}
}

internal object FixtureOverlayTwo : DslEntryOverlay {
    override val name get() = "fixture2"
    override val id get() = "fixture2"
    override val icon get() = ImageHolder.empty

    context(ctx: DslContext)
    override fun overlay() {}
}

/** No gui and no overlay: an entry with stages only. It must load and run like any other. */
internal object FixtureNoUi : DslEntryService by FixtureIdentity {
    override val id get() = "fixture-noui"

    override fun initialize() { FixtureStats.stages += "initialize:noui" }
    override fun launch() { FixtureStats.stages += "launch:noui" }
}

/** The same overlay id as FixtureOverlay: declared later, so it must lose and be reported. */
internal object FixtureOverlayDup : DslEntryOverlay, DslEntryService by FixtureIdentity {
    context(ctx: DslContext)
    override fun overlay() {}
}
