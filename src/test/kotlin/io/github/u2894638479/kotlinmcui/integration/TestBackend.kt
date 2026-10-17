package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.*
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.integration.TestRoot
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
 * Provides the fake platform's pieces so that [DslScreen.build]
 * and the complete layout/render pipeline can be exercised in tests.
 */
internal object TestBackend : DslRootRenderer<Unit> by TestRenderer, DslRootScreenFactory<Unit> by TestScreenFactory {

    /** Run a single frame of the DSL lifecycle for the given [screen]. */
    fun renderFrame(screen: DslScreen, mousePos: Position = Position(0.px, 0.px)) {
        TestInput._mouse = mousePos
        TestRoot.rect.apply {
            left = 0.px; top = 0.px
            right = 800.px; bottom = 600.px
        }
        val child = TestRoot.children.collect(screen)
        val backend = this
        val renderParam: Unit = Unit
        // Unmount even when the frame throws: a leftover child would be rendered by
        // every following test in the same JVM.
        try {
            kotlin.context(backend, renderParam, mousePos) {
                TestRoot.render<Unit>()
            }
        } finally {
            TestRoot.children.remove(child)
        }
    }

    /** Put the pointer at [position] for the next frame, without mounting anything. */
    fun mouseAt(position: Position) { TestInput._mouse = position }

    /** Screenshot names the preview session asked for, in order. */
    val captures get() = TestUtils.captures

    /** The exit code the preview session asked for, or null while the game has not been stopped. */
    val quitCode get() = TestUtils.quitCode

    /** Makes the next capture fail, so the "no screenshot was written" path can be exercised. */
    var failCapture
        get() = TestUtils.failCapture
        set(value) { TestUtils.failCapture = value }

    /** The title of the last screen a case opened, which is how a test tells an error page apart. */
    val lastScreenTitle get() = TestScreenFactory.lastTitle

    /** Forget what an earlier case asked the preview session to do, and unmount what it opened. */
    fun resetPreview() {
        TestUtils.captures.clear()
        TestUtils.quitCode = null
        TestUtils.failCapture = false
        TestScreenFactory.lastTitle = null
        TestScreenFactory.unmount()
    }
}

// ── Renderer ─────────────────────────────────────────────────────────────────

/** One recorded [DslRootRenderer.renderTriangles] call, for tests that assert geometry. */
internal class Triangles(val vertices: FloatArray, val colors: IntArray, val image: ImageHolder?)

/** Every `renderTriangles` call the fake backend received, in order. */
internal val recordedTriangles = mutableListOf<Triangles>()

internal object TestRenderer : DslRootRenderer<Unit> {
    override val guiScale = 1.0
    context(renderParam: Unit) override fun flush() {}
    context(renderParam: Unit) override fun renderQuad(
        rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color, image: ImageHolder?, uv: Rect?
    ) {}
    context(renderParam: Unit) override fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder?) {
        recordedTriangles += Triangles(vertices.copyOf(), colors.copyOf(), image)
    }
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

internal object TestScreenFactory : DslRootScreenFactory<Unit> {
    private var mounted: DslChild? = null

    /** The title the last screen was created with; a test clears it through [TestBackend.resetPreview]. */
    var lastTitle: String? = null

    override fun create(title: String, dslFunction: DslFunction): DslScreenHolder<Unit> {
        lastTitle = title
        return object : DslScreenHolder<Unit> {
            private val store = DslDataStore(TestPlatform, TestRoot.root, title, {}, dslFunction)
            // A real backend mounts in its own screen's lifecycle (the Minecraft Screen's added and
            // removed), which a headless fake does not have - showing is the only event it can hang
            // mounting on. A page that is never mounted is never built, and so never gets to fail.
            override fun show() { mount(dslScreen) }
            override val screen: Unit = Unit
            // The data store has already built its screen; reusing it avoids a second one.
            override val dslScreen = store.dslScreen
        }
    }

    private fun mount(screen: DslScreen) {
        unmount()
        mounted = TestRoot.children.collect(screen)
    }

    /** Unmount whatever a case opened: a leftover child would be rendered by every later case. */
    fun unmount() {
        mounted?.let { TestRoot.children.remove(it) }
        mounted = null
    }
}

// ── Utils ────────────────────────────────────────────────────────────────────

internal object TestUtils : DslPlatformUtils, DslRootUtils {
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
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    // The split gives the same object two hats: process-level helpers and per-output helpers.
    override val sessionDispatcher: CoroutineDispatcher get() = mainDispatcher

    // The fake renders on one thread, so "the session thread" is whoever calls in.
    override val isOnSessionThread: Boolean get() = true

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

internal object TestMetadata : DslPlatformMetadata {
    // Configuration written during tests belongs under build/, not in the checkout.
    override val configDir = Paths.get("build", "test-config")
    override val gameDir = Paths.get(".")
    override val gameVersion = "test"
    override val gameLoader = "test"
    override val environment = Environment.COMMON
}

// ── Input ────────────────────────────────────────────────────────────────────

internal object TestInput : DslRootInput {
    var _mouse = Position(0.px, 0.px)
    override val mouse: Position get() = _mouse
    override fun isKeyDown(key: Key) = false
    override fun isMouseDown(mouse: Int) = false
    override fun setPreeditArea(rect: Rect) {}
}

/**
 * The fixture's platform role: the process-level half of the old aggregate backend.
 *
 * The same `TestUtils` object serves both this and [TestRootProvider] - the "one object, two
 * interfaces" shape the real backends will use after the split.
 */
internal object TestPlatform : DslPlatform {
    override val metadata: DslPlatformMetadata get() = TestMetadata
    override val utils: DslPlatformUtils get() = TestUtils
    override val dispatcher: CoroutineDispatcher get() = TestUtils.mainDispatcher
}
