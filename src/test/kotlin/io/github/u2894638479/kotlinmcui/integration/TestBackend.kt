package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.*
import io.github.u2894638479.kotlinmcui.container.DslChild
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
import java.nio.file.Paths

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
        // Unmount even when the frame throws: a leftover child would be rendered by
        // every following test in the same JVM.
        try {
            kotlin.context(backend, renderParam, mousePos) {
                TopComponent.render<Unit>()
            }
        } finally {
            TopComponent.children.remove(child)
        }
    }

    /** Screenshot names the preview session asked for, in order. */
    val captures get() = TestUtils.captures

    /** The exit code the preview session asked for, or null while the game has not been stopped. */
    val quitCode get() = TestUtils.quitCode

    /** Makes the next capture fail, so the "no screenshot was written" path can be exercised. */
    var failCapture
        get() = TestUtils.failCapture
        set(value) { TestUtils.failCapture = value }

    /** Forget what an earlier case asked the preview session to do, and unmount what it opened. */
    fun resetPreview() {
        TestUtils.captures.clear()
        TestUtils.quitCode = null
        TestUtils.failCapture = false
        TestScreenFactory.unmount()
    }
}

// ── Renderer ─────────────────────────────────────────────────────────────────

private object TestRenderer : DslBackendRenderer<Unit> {
    override val guiScale = 1.0
    context(renderParam: Unit) override fun flush() {}
    context(renderParam: Unit) override fun renderQuad(
        rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color, image: ImageHolder?, uv: Rect?
    ) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderButton(rect: Rect, highlighted: Boolean, active: Boolean, color: Color) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderSlot(rect: Rect) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderContainer(rect: Rect) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderTooltip(rect: Rect) {}
    context(renderParam: Unit, ctx: DslScaleContext) override fun renderItem(rect: Rect, item: String, count: Int, damage: Double?, enchanted: Boolean) {}
    context(ctx: DslScaleContext, renderParam: Unit) override fun renderDefaultBackground(rect: Rect) {}
    context(renderParam: Unit) override fun withScissor(rect: Rect, block: () -> Unit) { block() }
    context(renderParam: Unit) override fun withTransform(transform: Transform, block: () -> Unit) { block() }

    private val glyph = object : DslGlyph {
        override val normalAdvance = 9.px
        override val boldOffset = 0.px
        override val shadowOffset = 1.px
    }

    // A real backend must hand out the same instance for the same name: text lines
    // compare font identity between layout and render.
    private val font = object : DslFont<Unit> {
        override val lineHeight = 9.px
        override fun glyph(code: Int) = glyph
        context(renderParam: Unit) override fun renderChar(char: DslRenderableChar, x: Measure, y: Measure, effectLeft: Measure, effectRight: Measure) {}
    }
    override fun getFont(name: String?) = font
}

// ── Screen factory ───────────────────────────────────────────────────────────

private object TestScreenFactory : DslBackendScreenFactory<Unit> {
    private var mounted: DslChild? = null

    override fun create(title: String, dslFunction: DslFunction) = object : DslBackendScreenHolder<Unit> {
        private val store = DslDataStore(TestBackend, title, {}, dslFunction)
        // Showing a screen mounts it, the way a real platform does when the screen is added: a page
        // that is never mounted is never built, and so never gets to fail.
        override fun show() { mount(dslScreen) }
        override val screen: Unit = Unit
        // The data store has already built its screen; reusing it avoids a second one.
        override val dslScreen = store.dslScreen
    }

    private fun mount(screen: DslScreen) {
        unmount()
        mounted = TopComponent.children.collect(screen)
    }

    /** Unmount whatever a case opened: a leftover child would be rendered by every later case. */
    fun unmount() {
        mounted?.let { TopComponent.children.remove(it) }
        mounted = null
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
    // UI state is confined to the thread that builds it, so "the main thread"
    // dispatcher here must run continuations on the caller rather than a pool.
    override val mainDispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    /** Capture requests, recorded so a test can assert what the preview session asked for. */
    val captures = mutableListOf<String>()

    /** The exit code the preview session asked for; a test cannot let the game really stop. */
    var quitCode: Int? = null

    /** Makes the next capture fail, so the "no screenshot was written" path can be exercised. */
    var failCapture = false

    override suspend fun capture(fileName: String): File? {
        captures += fileName
        if(failCapture) return null
        val directory = File("build","test-captures").apply { mkdirs() }
        return File(directory,fileName).apply { writeBytes(byteArrayOf(1,2,3)) }
    }

    override fun quit(exitCode: Int) {
        quitCode = exitCode
    }
}

// ── Metadata ─────────────────────────────────────────────────────────────────

internal object TestMetadata : DslBackendMetadata {
    // Configuration written during tests belongs under build/, not in the checkout.
    override val configDir = Paths.get("build", "test-config")
    override val gameDir = Paths.get(".")
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
