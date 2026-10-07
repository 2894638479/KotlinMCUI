package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The preview session's policy, exercised against the fake backend: which references resolve, what
 * a screenshot is called, and which outcomes stop the game with a non-zero code.
 */
class DslPreviewTest {
    private var previous: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previous = dslBackendProvider
        dslBackendProvider = { TestBackend }
        TestBackend.resetPreview()
        TopComponent.renderFailure = null
    }

    @AfterEach
    fun tearDown() {
        dslBackendProvider = previous
        TopComponent.renderFailure = null
    }

    @Test
    fun `the request is one page property and one capture switch`() {
        val page = System.getProperty("kotlinmcui.preview")
        val capture = System.getProperty("kotlinmcui.preview.capture")
        try {
            System.clearProperty("kotlinmcui.preview")
            System.clearProperty("kotlinmcui.preview.capture")
            assertNull(DslPreview.request(),"no property at all means no page")
            System.setProperty("kotlinmcui.preview","a.b.C")
            assertEquals("a.b.C" to false,DslPreview.request(),"opening it is the default")
            System.setProperty("kotlinmcui.preview.capture","true")
            assertEquals("a.b.C" to true,DslPreview.request())
            System.setProperty("kotlinmcui.preview","  ")
            assertNull(DslPreview.request(),"a blank page is not a page")
        } finally {
            if(page == null) System.clearProperty("kotlinmcui.preview") else System.setProperty("kotlinmcui.preview",page)
            if(capture == null) System.clearProperty("kotlinmcui.preview.capture") else System.setProperty("kotlinmcui.preview.capture",capture)
        }
    }

    @Test
    fun `a top-level function resolves through its file class`() {
        val page = DslPreview.pageFor("io.github.u2894638479.kotlinmcui.test.TestColorPage")
        assertEquals("io.github.u2894638479.kotlinmcui.test.TestColorPageKt#TestColorPage",page.describe())
    }

    @Test
    fun `an entry resolves through its content`() {
        val page = DslPreview.pageFor("io.github.u2894638479.kotlinmcui.test.TestEntry\$TestPage#content")
        assertEquals("io.github.u2894638479.kotlinmcui.test.TestEntry\$TestPage#content",page.describe())
    }

    @Test
    fun `a member function of an object is reached through its instance`() {
        val reference = "io.github.u2894638479.kotlinmcui.backend.ObjectPreviewPage#page"
        assertEquals(reference,DslPreview.pageFor(reference).describe())
    }

    @Test
    fun `a member function is reached through a no argument constructor`() {
        val reference = "io.github.u2894638479.kotlinmcui.backend.ConstructedPreviewPage#page"
        assertEquals(reference,DslPreview.pageFor(reference).describe())
    }

    @Test
    fun `a jvm static companion member is reached through the class`() {
        val reference = "io.github.u2894638479.kotlinmcui.backend.StaticCompanionPreviewPage#page"
        assertEquals(reference,DslPreview.pageFor(reference).describe())
    }

    @Test
    fun `a companion member without jvm static lives on the companion class`() {
        val reference = "io.github.u2894638479.kotlinmcui.backend.PlainCompanionPreviewPage\$Companion#page"
        assertEquals(reference,DslPreview.pageFor(reference).describe())
    }

    @Test
    fun `a class that needs arguments is not reachable`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.UnbuildablePreviewPage#page")
        }
        assertNotNull(error.message)
    }

    @Test
    fun `an abstract class is not reachable`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.AbstractPreviewPage#page")
        }
        assertNotNull(error.message)
    }

    @Test
    fun `a vararg member is refused`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.VarargMemberPreviewPage#page")
        }
        val message = assertNotNull(error.message)
        assertTrue(message.contains("parameter 'names' is a vararg"),message)
    }

    @Test
    fun `a bare class name is not a page`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.ObjectPreviewPage")
        }
        val message = assertNotNull(error.message)
        assertTrue(message.contains("ObjectPreviewPageKt#ObjectPreviewPage"),message)
        assertFalse(message.contains("#page"),"a class name no longer guesses which function draws it")
    }

    @Test
    fun `an explicit class and method resolves`() {
        val page = DslPreview.pageFor("io.github.u2894638479.kotlinmcui.test.TestTransformPageKt#TestTransformPage")
        assertEquals("io.github.u2894638479.kotlinmcui.test.TestTransformPageKt#TestTransformPage",page.describe())
    }

    @Test
    fun `an unresolvable reference says what it tried`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.test.NoSuchPage")
        }
        val message = assertNotNull(error.message)
        assertTrue(message.contains("NoSuchPageKt#NoSuchPage"),message)
        assertTrue(message.contains("class not found"),"a candidate whose class is missing says so")
    }

    @Test
    fun `the screenshot name comes from the page`() {
        assertEquals(
            "kotlinmcui-TestColorPage.png",
            DslPreview.fileNameFor("io.github.u2894638479.kotlinmcui.test.TestColorPage"),
        )
        assertEquals(
            "kotlinmcui-TestPage.png",
            DslPreview.fileNameFor("io.github.u2894638479.kotlinmcui.test.TestEntry\$TestPage"),
        )
    }

    @Test
    fun `a session photographs the page and exits zero`() {
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.test.TestColorPage",capture = true)
        }
        assertEquals(0,TestBackend.quitCode,"a page that opened and photographed is a pass")
        assertEquals(listOf("kotlinmcui-TestColorPage.png"),TestBackend.captures)
        assertTrue(File("build/test-captures/kotlinmcui-TestColorPage.png").exists(),"the file is on disk")
    }

    @Test
    fun `a page that throws while it renders exits non zero and still leaves the evidence`() {
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.backend.ThrowingPreviewPage",capture = true)
        }
        assertEquals(1,TestBackend.quitCode,"a page that never finished drawing is not a pass")
        assertEquals(1,TestBackend.captures.size,"the screenshot is what a human needs to see")
    }

    @Test
    fun `a platform that cannot photograph exits non zero`() {
        TestBackend.failCapture = true
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.test.TestColorPage",capture = true)
        }
        assertEquals(1,TestBackend.quitCode)
        assertEquals(1,TestBackend.captures.size,"the attempt is still visible in the record")
    }

    @Test
    fun `a page that threw while rendering exits non zero and still leaves the evidence`() {
        TopComponent.renderFailure = IllegalStateException("broken while rendering")
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.test.TestColorPage",capture = true)
        }
        assertEquals(1,TestBackend.quitCode)
        assertEquals(1,TestBackend.captures.size,"the screenshot is what a human needs to see")
    }

    @Test
    fun `preview mode leaves the game running and takes no screenshot`() {
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.test.TestColorPage",capture = false)
        }
        assertNull(TestBackend.quitCode,"nobody asked the game to stop")
        assertEquals(emptyList(),TestBackend.captures)
    }

    @Test
    fun `a page with defaulted parameters resolves and is called with them`() {
        DefaultedPreviewCalls.count = null
        DefaultedPreviewCalls.name = null
        val page = DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.DefaultedPreviewPage")
        assertEquals(
            "io.github.u2894638479.kotlinmcui.backend.DefaultedPreviewPageKt#DefaultedPreviewPage",
            page.describe(),
        )
        assertTrue(page.usesDefaults,"the preview cannot pass arguments, so it uses the defaults")
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.backend.DefaultedPreviewPage",capture = false)
        }
        assertEquals(7,DefaultedPreviewCalls.count,"the first default arrived")
        assertEquals("default",DefaultedPreviewCalls.name,"the second default arrived")
        assertNull(TestBackend.quitCode,"opening a page without capture never stops the game")
    }

    @Test
    fun `a page shaped function whose extra parameter is required is not a page`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.RequiredArgPreviewPage")
        }
        val message = assertNotNull(error.message)
        assertTrue(message.contains("RequiredArgPreviewPageKt#RequiredArgPreviewPage"),message)
        assertTrue(message.contains("parameter 'required' (kotlin.Int) has no default value"),message)
        assertTrue(message.contains("all have default values"),"the refusal says what a page may look like")
    }

    @Test
    fun `a page that mixes a required parameter with a defaulted one is refused`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.MixedSignaturePage")
        }
        val message = assertNotNull(error.message)
        assertTrue(message.contains("MixedSignaturePageKt#MixedSignaturePage"),message)
        assertTrue(message.contains("parameter 'required' (kotlin.Int) has no default value"),message)
        assertFalse(message.contains("'optional'"),"a parameter that does have a default is not blamed")
    }

    @Test
    fun `a page whose extra parameter is a vararg is refused`() {
        val error = assertFailsWith<IllegalStateException> {
            DslPreview.pageFor("io.github.u2894638479.kotlinmcui.backend.VarargPreviewPage")
        }
        val message = assertNotNull(error.message)
        assertTrue(message.contains("VarargPreviewPageKt#VarargPreviewPage"),message)
        assertTrue(message.contains("parameter 'names' is a vararg"),message)
    }

    @Test
    fun `an entry with a parameterless and a parameterised content uses the parameterless one`() {
        val reference = "io.github.u2894638479.kotlinmcui.backend.MixedContentEntry#content"
        val page = DslPreview.pageFor(reference)
        assertFalse(page.usesDefaults,"the interface's own content() takes no parameters")
        MixedContentCalls.recorded = null
        runWithFrames {
            DslPreview.run(reference,capture = false)
        }
        assertEquals("no-arg",MixedContentCalls.recorded,"the parameterless content is the page")
    }

    @Test
    fun `an unresolvable reference is put on screen as an error page`() {
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.test.NoSuchPage",capture = false)
        }
        assertEquals("Preview error",TestBackend.lastScreenTitle,"the failure takes the page's place")
        assertNull(TestBackend.quitCode,"without capture the reason is read in the game")
        assertNull(TopComponent.renderFailure,"the error page itself draws")
    }

    @Test
    fun `capture mode photographs the error page and still exits non zero`() {
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.test.NoSuchPage",capture = true)
        }
        assertEquals(1,TestBackend.quitCode,"a run that never opened its page is not a pass")
        assertEquals(listOf("kotlinmcui-NoSuchPage.png"),TestBackend.captures,"the error page is the picture")
        assertNull(TopComponent.renderFailure,"the error page itself draws")
    }

    @Test
    fun `a refused parameter is put on screen as an error page too`() {
        runWithFrames {
            DslPreview.run("io.github.u2894638479.kotlinmcui.backend.RequiredArgPreviewPage",capture = false)
        }
        assertEquals("Preview error",TestBackend.lastScreenTitle)
        assertNull(TestBackend.quitCode)
    }

    @Test
    fun `the error text carries the reason the page was refused`() {
        val reference = "io.github.u2894638479.kotlinmcui.backend.RequiredArgPreviewPage"
        val error = assertFailsWith<IllegalStateException> { DslPreview.pageFor(reference) }
        val text = DslPreview.errorText(reference,error)
        assertTrue(text.contains(reference),text)
        assertTrue(text.contains("parameter 'required' (kotlin.Int) has no default value"),text)
    }

    /**
     * Runs [session] while rendering frames, because it waits on the frame clock rather than on a
     * wall clock: without a platform ticking [TopComponent] it would sit there until its timeout.
     */
    private fun runWithFrames(session: suspend () -> Unit) = runBlocking {
        val running = launch { session() }
        try {
            withTimeout(20_000) {
                while(running.isActive) {
                    // A page may throw while rendering; the platform reports it and the session
                    // turns it into a code, so the pump itself must survive it.
                    runCatching {
                        kotlin.context(TestBackend,Unit,Position(0.px,0.px)) { TopComponent.render<Unit>() }
                    }
                    delay(5)
                }
            }
        } finally {
            running.join()
        }
    }
}
