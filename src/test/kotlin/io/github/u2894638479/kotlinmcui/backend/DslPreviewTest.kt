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
    fun `a top-level function resolves through its file class`() {
        val page = DslPreview.pageFor("io.github.u2894638479.kotlinmcui.test.TestColorPage")
        assertEquals("io.github.u2894638479.kotlinmcui.test.TestColorPageKt#TestColorPage",page.describe())
    }

    @Test
    fun `an entry resolves through its content`() {
        val page = DslPreview.pageFor("io.github.u2894638479.kotlinmcui.test.TestEntry\$TestPage")
        assertEquals("io.github.u2894638479.kotlinmcui.test.TestEntry\$TestPage#content",page.describe())
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
        assertTrue(message.contains("NoSuchPage#content"),message)
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
