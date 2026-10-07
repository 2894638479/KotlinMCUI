package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer

/**
 * The declaration shapes a preview page can have, one per class, so that a test can pin down which
 * reference reaches each of them and which are out of reach.
 */

/** A page that is a member of an object, which is the shape an entry has. */
internal object ObjectPreviewPage {
    context(ctx: DslContext)
    fun page(): DslChild = Spacer(id = "object")
}

/** A page that is a member of a class a no-argument constructor can build. */
internal class ConstructedPreviewPage {
    context(ctx: DslContext)
    fun page(): DslChild = Spacer(id = "constructed")
}

/** A page whose class needs arguments, so its member function cannot be reached. */
internal class UnbuildablePreviewPage(private val seed: Int) {
    context(ctx: DslContext)
    fun page(): DslChild = Spacer(id = "unbuildable-$seed")
}

/** A page a companion declares with `@JvmStatic`, so it lands on the class itself. */
internal class StaticCompanionPreviewPage {
    companion object {
        @JvmStatic
        context(ctx: DslContext)
        fun page(): DslChild = Spacer(id = "static-companion")
    }
}

/** A page a companion declares without `@JvmStatic`, so it lives on the companion class. */
internal class PlainCompanionPreviewPage {
    companion object {
        context(ctx: DslContext)
        fun page(): DslChild = Spacer(id = "plain-companion")
    }
}

/** A page that is a member of an abstract class, which cannot be instantiated. */
internal abstract class AbstractPreviewPage {
    context(ctx: DslContext)
    fun page(): DslChild = Spacer(id = "abstract")
}

/** A page whose only extra parameter is a vararg, which the preview refuses. */
internal object VarargMemberPreviewPage {
    context(ctx: DslContext)
    fun page(vararg names: String): DslChild = Spacer(id = "vararg")
}
