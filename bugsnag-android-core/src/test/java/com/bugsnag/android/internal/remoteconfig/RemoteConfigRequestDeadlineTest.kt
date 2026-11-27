package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.Logger
import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The deadline must bound how long [RemoteConfigRequest.requestConfig] blocks even when the
 * connection is stuck somewhere [HttpURLConnection.disconnect] cannot interrupt, such as a DNS
 * lookup (observed on a device: the deadline fired at 15s but the request only failed at 45s).
 */
class RemoteConfigRequestDeadlineTest {

    private val notifier = Notifier("Test Notifier", "1.0.0", "https://test.com")
    private val warnings: MutableList<Pair<String, Throwable?>> = Collections.synchronizedList(mutableListOf())
    private val logger = object : Logger {
        override fun w(msg: String) {
            warnings.add(msg to null)
        }

        override fun w(msg: String, throwable: Throwable) {
            warnings.add(msg to throwable)
        }
    }

    /** Released after each test so blocked workers never leak into the next test. */
    private val release = CountDownLatch(1)

    @After
    fun releaseBlockedWorkers() {
        release.countDown()
        val deadline = System.currentTimeMillis() + 5_000
        while (RemoteConfigRequest.requestExecutor.activeCount > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
    }

    @Test
    fun requestThatNeverCompletesIsAbandonedAtTheDeadline() {
        val connection = hangingConnection()

        val startedAt = System.nanoTime()
        val result = requestConfig(connection, deadlineMs = 200)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

        assertNull(result)
        assertTrue("requestConfig blocked for ${elapsedMs}ms", elapsedMs < 2_000)
        verify(connection, atLeastOnce()).disconnect()
        assertTrue(warnings.any { it.first == "Remote config request exceeded 200ms; aborting" })
        val failure = warnings.single { it.first.startsWith("Remote config request failed") }.second
        assertTrue(failure is RemoteConfigTimeoutException)
    }

    @Test
    fun requestFailsFastWhileEarlierRequestsAreStillBlocked() {
        // Two abandoned requests occupy every worker thread
        repeat(RemoteConfigRequest.MAX_REQUEST_THREADS) {
            assertNull(requestConfig(hangingConnection(), deadlineMs = 50))
        }

        val connection = hangingConnection(stubbed = false)
        val startedAt = System.nanoTime()
        val result = requestConfig(connection, deadlineMs = 5_000)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

        assertNull(result)
        assertTrue("requestConfig blocked for ${elapsedMs}ms", elapsedMs < 1_000)
        verify(connection).disconnect()
        val failure = warnings.last { it.first.startsWith("Remote config request failed") }.second
        assertTrue(failure is RemoteConfigTimeoutException)
        assertTrue(failure!!.message!!.contains("still blocked"))
    }

    @Test
    fun failureInsideTheExchangeIsReportedWithItsOriginalCause() {
        val connection = mock(HttpURLConnection::class.java)
        `when`(connection.responseCode).thenThrow(UnknownHostException("config.bugsnag.com"))

        assertNull(requestConfig(connection, deadlineMs = 5_000))

        val (message, failure) = warnings.single { it.first.startsWith("Remote config request failed") }
        assertTrue(failure is UnknownHostException)
        // the cause must be readable from the message alone (Android drops this exception's stack trace)
        assertTrue(message, message.contains("(java.net.UnknownHostException: config.bugsnag.com)"))
        verify(connection).disconnect()
    }

    @Test
    fun interruptWhileWaitingDoesNotDropTheResponse() {
        // Seen on a device: the app crashed while a fetch was in flight, the SDK's executor was
        // interrupted, and the 200 that arrived 50 ms later was dropped and a cooldown started.
        val responding = CountDownLatch(1)
        val connection = mock(HttpURLConnection::class.java)
        `when`(connection.responseCode).thenAnswer {
            responding.countDown()
            Thread.sleep(300)
            HttpURLConnection.HTTP_OK
        }
        `when`(connection.getHeaderField("ETag")).thenReturn("\"etag-2\"")
        `when`(connection.getHeaderField("Cache-Control")).thenReturn("max-age=3600")
        `when`(connection.inputStream).thenReturn(ByteArrayInputStream("{}".toByteArray()))

        var result: RemoteConfig? = null
        var interruptFlagAfter = false
        val caller = Thread {
            result = requestConfig(connection, deadlineMs = 5_000)
            interruptFlagAfter = Thread.currentThread().isInterrupted
        }
        caller.start()
        assertTrue(responding.await(5, TimeUnit.SECONDS))
        caller.interrupt()
        caller.join(10_000)

        assertNotNull("the response was dropped after an interrupt", result)
        assertEquals("\"etag-2\"", result!!.configurationTag)
        assertTrue("the interrupt flag must be restored for the caller", interruptFlagAfter)
        assertTrue(warnings.none { it.first.startsWith("Remote config request failed") })
    }

    @Test
    fun failureWithoutAnyResponseIsFlagged() {
        val connection = mock(HttpURLConnection::class.java)
        `when`(connection.responseCode).thenThrow(UnknownHostException("config.bugsnag.com"))
        val request = request(deadlineMs = 5_000)

        assertNull(requestConfig(request, connection))
        assertTrue("a DNS failure never reached the service", request.failedWithoutResponse)
    }

    @Test
    fun httpErrorIsNotFlaggedAsFailingWithoutResponse() {
        val connection = mock(HttpURLConnection::class.java)
        `when`(connection.responseCode).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR)
        val request = request(deadlineMs = 5_000)

        assertNull(requestConfig(request, connection))
        assertFalse("an HTTP 500 means the service was reached", request.failedWithoutResponse)
    }

    @Test
    fun responseWithinTheDeadlineIsReturned() {
        val connection = mock(HttpURLConnection::class.java)
        `when`(connection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)
        `when`(connection.getHeaderField("ETag")).thenReturn("\"etag-1\"")
        `when`(connection.getHeaderField("Cache-Control")).thenReturn("max-age=3600")
        `when`(connection.inputStream).thenReturn(ByteArrayInputStream("{}".toByteArray()))

        val result = requestConfig(connection, deadlineMs = 5_000)

        assertNotNull(result)
        assertEquals("\"etag-1\"", result!!.configurationTag)
        assertTrue(result.discardRules.isEmpty())
        verify(connection).disconnect()
    }

    /** A connection whose response blocks like a stalled DNS lookup: disconnect does not unblock it. */
    private fun hangingConnection(stubbed: Boolean = true): HttpURLConnection {
        val connection = mock(HttpURLConnection::class.java)
        if (stubbed) {
            `when`(connection.responseCode).thenAnswer {
                // a blocked DNS lookup ignores interrupts, so keep waiting through Future.cancel(true)
                val giveUpAt = System.currentTimeMillis() + 30_000
                while (release.count > 0 && System.currentTimeMillis() < giveUpAt) {
                    try {
                        release.await(50, TimeUnit.MILLISECONDS)
                    } catch (ignored: InterruptedException) {
                    }
                }
                HttpURLConnection.HTTP_OK
            }
        }
        return connection
    }

    /** Runs [RemoteConfigRequest.requestConfig] with URL construction mocked to return [connection]. */
    private fun requestConfig(connection: HttpURLConnection, deadlineMs: Long): RemoteConfig? =
        requestConfig(request(deadlineMs), connection)

    private fun request(deadlineMs: Long) = RemoteConfigRequest(
        "https://config.bugsnag.com",
        "test-api-key-12345678901234567890123456789012",
        notifier,
        "1.0.0",
        100,
        "production",
        "com.example.app",
        null,
        logger,
        requestDeadlineMs = deadlineMs
    )

    /** Runs [request] with URL construction mocked to return [connection]. */
    private fun requestConfig(request: RemoteConfigRequest, connection: HttpURLConnection): RemoteConfig? =
        mockConstruction(URL::class.java) { url, _ ->
            `when`(url.openConnection()).thenReturn(connection)
        }.use { request.requestConfig() }
}
