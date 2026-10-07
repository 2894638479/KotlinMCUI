package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
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

/** A page declared as an extension function: its receiver becomes an argument nobody can pass. */
context(ctx: DslContext)
internal fun String.ExtensionPreviewPage(): DslChild = Spacer(id = "extension")

/** A page that suspends, which adds a continuation parameter the preview cannot supply. */
context(ctx: DslContext)
internal suspend fun SuspendPreviewPage(): DslChild = Spacer(id = "suspend")

/** A page that takes a second context parameter, which the preview cannot supply either. */
context(ctx: DslContext, scale: DslScaleContext)
internal fun TwoContextPreviewPage(): DslChild = Spacer(id = "two-context")

/** An inner class needs its outer instance, so its member function is out of reach. */
internal class OuterPreviewPage {
    inner class InnerPreviewPage {
        context(ctx: DslContext)
        fun page(): DslChild = Spacer(id = "inner")
    }
}
