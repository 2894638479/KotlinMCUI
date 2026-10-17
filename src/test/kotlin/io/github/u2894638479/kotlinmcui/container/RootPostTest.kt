package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslRootUtils
import java.io.File
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `DslRootUtils.post`: the door from a foreign thread to the root's thread.
 *
 * The fake's own utils answer "yes, you are on the session thread" everywhere, so these cases build their
 * own utils around a real single-thread executor: one thread whose name identifies it.
 */
class RootPostTest {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "session-thread") }
    private val onSessionThread = ThreadLocal.withInitial { false }
    private val dispatcher = executor.asCoroutineDispatcher()

    /**
     * Utils that only call the executor's thread "the session thread".
     *
     * Written out in full instead of delegating to the fake: delegation would also copy the interface's
     * default post, which is exactly the implementation under test here.
     */
    private val utils: DslRootUtils = object : DslRootUtils {
        override var clipboard = ""
        override suspend fun capture(fileName: String): File? = null
        override fun narrate(string: String) {}
        override fun playButtonSound() {}
        override fun openUri(uri: String) {}
        override val sessionDispatcher get() = dispatcher
        override val isOnSessionThread get() = onSessionThread.get()
    }

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
            utils.post { executed = true }
            ranBeforeReturn = executed
            onSessionThread.set(false)
        }.get()

        assertTrue(ranBeforeReturn, "on the session thread the block must run before post returns")
    }

    @Test
    fun `post is dispatched when the caller is on another thread`() {
        val done = CountDownLatch(1)
        var ranOn: String? = null

        utils.post {
            ranOn = Thread.currentThread().name
            done.countDown()
        }

        assertTrue(done.await(5, TimeUnit.SECONDS), "the block must reach the session thread")
        // A coroutine's thread name carries its own suffix, so compare the part that identifies the thread.
        assertEquals("session-thread", ranOn?.substringBefore(" @"), "and it must be that root's thread")
    }
}
