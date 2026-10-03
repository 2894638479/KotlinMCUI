package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.image.ImageHolder

/** Shared identity for the loader fixtures; only the loading behaviour is under test. */
internal object FixtureIdentity : DslEntryService {
    override val name get() = "fixture"
    override val id get() = "fixture"
    override val icon get() = ImageHolder.empty
}

internal object FixtureCommon : DslEntryCommon, DslEntryService by FixtureIdentity {
    var runs = 0
    override fun initialize() { runs++ }
}

internal object FixtureClient : DslEntryClient, DslEntryService by FixtureIdentity {
    var runs = 0
    override fun initializeClient() { runs++ }
}

internal object FixtureGuiObject : DslEntryGui, DslEntryService by FixtureIdentity {
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
