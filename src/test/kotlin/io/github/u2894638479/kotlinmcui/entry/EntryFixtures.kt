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
 * How often each fixture class was **initialised**, counted from outside the fixture:
 * reading these fields must not itself pull a fixture in, otherwise the counter could
 * never prove that a class stayed untouched.
 */
internal object FixtureLoads {
    var client = 0
    var server = 0
    var mixed = 0
    var gui = 0
    var silentClient = 0
}

internal object FixtureCommon : DslEntryCommon, DslEntryService by FixtureIdentity {
    var runs = 0
    override fun initialize() { runs++ }
}

internal object FixtureClient : DslEntryClient, DslEntryService by FixtureIdentity {
    init { FixtureLoads.client++ }

    var runs = 0
    override fun initializeClient() { runs++ }
}

internal object FixtureServer : DslEntryServer, DslEntryService by FixtureIdentity {
    init { FixtureLoads.server++ }

    var runs = 0
    override fun initializeServer() { runs++ }
}

/**
 * A client entry that no other test touches: asserting "the client class was never
 * loaded" only works while reading the assertion does not itself load the class.
 */
internal object FixtureSilentClient : DslEntryClient, DslEntryService by FixtureIdentity {
    init { FixtureLoads.silentClient++ }

    var runs = 0
    override fun initializeClient() { runs++ }
}

/**
 * The documented trap: an entry that initialises on the client *and* offers a screen.
 * The service files name it under both interfaces, so a server loads it for the screen.
 */
internal object FixtureMixed : DslEntryClient, DslEntryGui, DslEntryService by FixtureIdentity {
    init { FixtureLoads.mixed++ }

    var clientRuns = 0
    override fun initializeClient() { clientRuns++ }

    context(ctx: DslContext)
    override fun content() {}
}

internal object FixtureGuiObject : DslEntryGui, DslEntryService by FixtureIdentity {
    init { FixtureLoads.gui++ }

    context(ctx: DslContext)
    override fun content() {}
}

internal class FixtureGuiClass : DslEntryGui, DslEntryService by FixtureIdentity {
    context(ctx: DslContext)
    override fun content() {}
}

/** A public constructor that fails: the entry must be skipped, never substituted. */
internal class FixtureThrowingConstructor : DslEntryGui, DslEntryService by FixtureIdentity {
    init { error("constructor failure") }

    context(ctx: DslContext)
    override fun content() {}
}

/** A Kotlin object whose static initialiser fails with an [Error] rather than an exception. */
internal object FixtureBrokenStaticInit : DslEntryGui, DslEntryService by FixtureIdentity {
    init { error("static initialiser failure") }

    context(ctx: DslContext)
    override fun content() {}
}

internal object FixtureOverlay : DslEntryOverlay, DslEntryService by FixtureIdentity {
    context(ctx: DslContext)
    override fun overlay() {}
}
