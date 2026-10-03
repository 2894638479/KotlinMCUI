package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.math.Color

/**
 * The current theme colour of a UI block, active during DSL construction.
 *
 * Components resolve their appearance colour at build time by mixing a base
 * into the theme: `Color.WHITE.mixed` is the theme colour itself, while a
 * custom base (e.g. `Color(180,180,180).mixed` for a splitter) blends that
 * base into the theme. The colour is settled once during construction —
 * nothing is stored for the render pass.
 *
 * Scope in with [io.github.u2894638479.kotlinmcui.dsl.withColor]; do the
 * blending yourself at the call site, e.g. `withColor(Color.RED.mixed) { }`
 * or `withColor(Color.WHITE.mixed.change(a = 128)) { }` for a translucent
 * block.
 */
interface DslColorContext {
    val color: Color
    companion object {
        operator fun invoke(color: Color) = object : DslColorContext {
            override val color = color
        }
    }
}

/** [this] multiplied into the current theme colour (channel-wise). */
context(ctx: DslColorContext)
val Color.mixed get() = ctx.color.mixedWith(this)

/** The current theme colour with the alpha channel replaced by [this] alpha. */
context(ctx: DslColorContext)
val Int.mixedAlpha get() = ctx.color.change(a = this)

/** [this] (0..1) applied as a uniform alpha over the current theme colour. */
context(ctx: DslColorContext)
val Double.mixedAlpha get() = ctx.color.change(a = (this * 255).toInt().coerceIn(0, 255))