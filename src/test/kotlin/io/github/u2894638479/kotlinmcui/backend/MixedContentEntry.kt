package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.entry.DslEntryGui
import io.github.u2894638479.kotlinmcui.image.ImageHolder

/** Which of [MixedContentEntry]'s two `content` overloads the preview asked for. */
object MixedContentCalls {
    var recorded: String? = null
}

/**
 * An entry that also declares a parameterised `content`: the interface's own parameterless
 * `content()` is the page, whatever order reflection happens to report the two in.
 */
object MixedContentEntry: DslEntryGui {
    override val name = "Mixed content"
    override val id = "kotlinmcui.mixedcontent"
    override val icon = ImageHolder.empty

    context(ctx: DslContext)
    override fun gui() {
        MixedContentCalls.recorded = "no-arg"
        Spacer(id = "no-arg")
    }

    context(ctx: DslContext)
    fun content(count: Int = 5): DslChild {
        MixedContentCalls.recorded = "count=$count"
        return Spacer(id = count)
    }
}
