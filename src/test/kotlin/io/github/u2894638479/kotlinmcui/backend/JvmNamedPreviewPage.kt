@file:JvmName("JvmNamedFacade")

package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer

/**
 * A file whose facade class the annotation renames, so the class a bare reference would look for
 * (`JvmNamedPreviewPageKt`) does not exist and only the explicit form reaches the page.
 */
context(ctx: DslContext)
internal fun JvmNamedPreviewPage(): DslChild = Spacer(id = "jvm-named")
