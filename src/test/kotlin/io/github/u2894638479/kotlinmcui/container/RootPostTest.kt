package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslRootUtils
import io.github.u2894638479.kotlinmcui.integration.TestInput
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The door from a foreign thread to a root's thread: [DslRoot.post] and [DslRoot.sessionScope].
 *
 * These cases build their own utils around a real single-thread executor, because the fake's own utils
 * answer "yes, you are on the session thread" everywhere and that is the branch under test. The root
 * is a real one - the scope it owns is what makes a queued callback end with it.
 */
class RootPostTest {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "session-thread") }
    private val onSessionThread = ThreadLocal.withInitial { false }
    private val dispatcher = executor.asCoroutineDispatcher()

    private val utils: DslRootUtils = object : DslRootUtils {
        override var clipboard = ""
        override suspend fun capture(fileName: String): File? = null
        override fun narrate(string: String) {}
        override fun playButtonSound() {}
        override fun openUri(uri: String) {}
        override val sessionDispatcher get() = dispatcher
        override val isOnSessionThread get() = onSessionThread.get()
    }

    private val root = DslRoot(TestInput, renderer = null, screenFactory = null, rootUtils = utils)

    @AfterTest
    fun tearDown() {
        dispatcher.close()
    }

    @Test
    fun `post runs inline when the caller is already on the session thread`() {
        var ranBeforeReturn = false
        var executed = false

        executor.submit {
            onSessionThread.set(true)
            root.post { executed = true }
            ranBeforeReturn = executed
            onSessionThread.set(false)
        }.get()

        assertTrue(ranBeforeReturn, "on the session thread the block must run before post returns")
    }

    @Test
    fun `post is dispatched when the caller is on another thread`() {
        val done = CountDownLatch(1)
        var ranOn: String? = null

        root.post {
            ranOn = Thread.currentThread().name
            done.countDown()
        }

        assertTrue(done.await(5, TimeUnit.SECONDS), "the block must reach the session thread")
        assertTrue(
            ranOn.orEmpty().startsWith("session-thread"),
            "it must run on the session thread, ran on '$ranOn'",
        )
    }

    @Test
    fun `closing the root cancels coroutines started in the session scope`() {
        val job = root.sessionScope.launch { awaitCancellation() }

        root.close()
        runBlocking { job.join() }

        assertFalse(job.isActive, "the session scope belongs to the root, so it dies with it")
    }

    @Test
    fun `a post after close never runs its block`() {
        root.close()

        var executed = false
        root.post { executed = true }

        assertFalse(executed, "a queued callback outliving its root would touch a closed tree")
    }
}
