package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.*
import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.text.DslFont
import io.github.u2894638479.kotlinmcui.text.DslGlyph
import io.github.u2894638479.kotlinmcui.text.DslRenderableChar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Path

/**
 * Minimal mock backend for integration testing.
 *
 * All rendering methods are no-ops. Translation returns the key itself.
 * Provides a full [DslBackend] implementation so that [DslScreen.build]
 * and the complete layout/render pipeline can be exercised in tests.
 */
internal object TestBackend : DslBackend<Unit, Unit>,
    DslBackendRenderer<Unit> by TestRenderer,
    DslBackendScreenFactory<Unit> by TestScreenFactory,
    DslBackendUtils by TestUtils,
    DslBackendMetadata by TestMetadata,
    DslBackendInput by TestInput
{
    /** Run a single frame of the DSL lifecycle for the given [screen]. */
    fun renderFrame(screen: DslScreen, mousePos: Position = Position(0.px, 0.px)) {
        TestInput._mouse = mousePos
        TopComponent.rect.apply {
            left = 0.px; top = 0.px
            right = 800.px; bottom = 600.px
        }
        val child = TopComponent.children.collect(screen)
        val backend = this
        val renderParam: Unit = Unit
        kotlin.context(backend, renderParam, mousePos) {
            TopComponent.render<Unit>()
        }
        TopComponent.children.remove(child)
    }
}

// ── Renderer ─────────────────────────────────────────────────────────────────

private object TestRenderer : DslBackendRenderer<Unit> {
    override val guiScale = 1.0
    context(renderParam: Unit) override fun flush() {}
    context(renderParam: Unit) override fun fillRect(rect: Rect, color: Color) {}
    context(renderParam: Unit) override fun fillRectGradient(rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderButton(rect: Rect, highlighted: Boolean, active: Boolean, color: Color) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderSlot(rect: Rect) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderContainer(rect: Rect) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderTooltip(rect: Rect) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderItem(rect: Rect, item: String, count: Int, damage: Double?, enchanted: Boolean) {}
    context(renderParam: Unit) override fun renderImage(image: ImageHolder, rect: Rect, uv: Rect, color: Color) {}
    context(ctx: DslScaleContext, renderParam: Unit) override fun renderDefaultBackground(rect: Rect) {}
    context(renderParam: Unit) override fun withScissor(rect: Rect, block: () -> Unit) { block() }
    context(renderParam: Unit) override fun withTransform(transform: Transform, block: () -> Unit) { block() }
    override fun getFont(name: String?) = object : DslFont<Unit> {
        override val lineHeight = 9.px
        override fun glyph(code: Int) = object : DslGlyph {
            override val normalAdvance = 9.px
            override val boldOffset = 0.px
            override val shadowOffset = 1.px
        }
        context(renderParam: Unit) override fun renderChar(char: DslRenderableChar, x: Measure, y: Measure, effectLeft: Measure, effectRight: Measure) {}
    }
}

// ── Screen factory ───────────────────────────────────────────────────────────

private object TestScreenFactory : DslBackendScreenFactory<Unit> {
    override fun create(title: String, dslFunction: DslFunction) = object : DslBackendScreenHolder<Unit> {
        override fun show() {}
        override val screen: Unit = Unit
        override val dslScreen = DslScreen(
            DslDataStore(TestBackend, title, {}, dslFunction),
            dslFunction
        )
    }
}

// ── Utils ────────────────────────────────────────────────────────────────────

private object TestUtils : DslBackendUtils {
    override var clipboard = ""
    override fun narrate(string: String) {}
    override fun translate(key: String, vararg args: Any?) = key
    override val isInWorld = true
    override fun loadLocalImage(file: File) = ImageHolder.empty
    override fun forceLoadLocalImage(file: File) = ImageHolder.empty
    override fun playButtonSound() {}
    override fun openUri(uri: String) {}
    override val mainDispatcher: CoroutineDispatcher = Dispatchers.Default
}

// ── Metadata ─────────────────────────────────────────────────────────────────

private object TestMetadata : DslBackendMetadata {
    override val configDir = Path.of(".")
    override val gameDir = Path.of(".")
    override val gameVersion = "test"
    override val gameLoader = "test"
    override val environment = Environment.COMMON
}

// ── Input ────────────────────────────────────────────────────────────────────

private object TestInput : DslBackendInput {
    var _mouse = Position(0.px, 0.px)
    override val mouse: Position get() = _mouse
    override fun isKeyDown(key: Key) = false
    override fun isMouseDown(mouse: Int) = false
    override fun setPreeditArea(rect: Rect) {}
}
