package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.Call
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyMap
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

@RunWith(MockitoJUnitRunner::class)
class BugsnagOkHttpPluginTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var call: Call

    @Before
    fun setup() {
        `when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Zero breadcrumbs are left when a call fails and the plugin is unloaded
     */
    @Test
    fun unloadedClientCallFailed() {
        BugsnagOkHttpPlugin().apply {
            load(client)
            callStart(call)
            unload()
            callFailed(call, IOException())
        }
        verify(client, times(0)).leaveBreadcrumb(
            anyString(),
            anyMap(),
            any()
        )
    }

    /**
     * Zero breadcrumbs are left when a call ends and the plugin is unloaded
     */
    @Test
    fun unloadedClientCallEnd() {
        BugsnagOkHttpPlugin().apply {
            load(client)
            callStart(call)
            unload()
            callEnd(call)
        }
        verify(client, times(0)).leaveBreadcrumb(
            anyString(),
            anyMap(),
            any()
        )
    }

    /**
     * A breadcrumb is left when a call fails and the plugin is unloaded
     */
    @Test
    fun loadedClientCallFailed() {
        val request = Request.Builder().url("https://example.com?debug=true").build()
        `when`(call.request()).thenReturn(request)

        val duration = AtomicLong()
        val plugin = BugsnagOkHttpPlugin { duration.incrementAndGet() }.apply {
            load(client)
            callStart(call)
            callFailed(call, IOException())
        }
        val map = mapOf(
            "method" to "GET",
            "url" to "https://example.com/",
            "urlParams" to mapOf(
                "debug" to "true"
            ),
            "duration" to 1L,
            "requestContentLength" to 0L
        )
        verify(client, times(1)).leaveBreadcrumb(
            "OkHttp call error",
            map,
            BreadcrumbType.REQUEST
        )
        assertNull(plugin.requestMap[call])
    }

    /**
     * A breadcrumb is left when a call ends and the plugin is unloaded
     */
    @Test
    fun loadedClientCallEnd() {
        val request = Request.Builder().url("https://example.com?debug=true").build()
        `when`(call.request()).thenReturn(request)

        val duration = AtomicLong()
        val plugin = BugsnagOkHttpPlugin { duration.incrementAndGet() }.apply {
            load(client)
            callStart(call)
            callEnd(call)
        }
        val map = mapOf(
            "method" to "GET",
            "url" to "https://example.com/",
            "urlParams" to mapOf(
                "debug" to "true"
            ),
            "duration" to 1L,
            "requestContentLength" to 0L
        )
        verify(client, times(1)).leaveBreadcrumb(
            "OkHttp call error",
            map,
            BreadcrumbType.REQUEST
        )
        assertNull(plugin.requestMap[call])
    }

    /**
     * No breadcrumbs are left when a call is cancelled
     */
    @Test
    fun callCancelled() {
        val request = Request.Builder().url("https://example.com?debug=true").build()
        `when`(call.request()).thenReturn(request)
        val plugin = BugsnagOkHttpPlugin { 1 }
        plugin.apply {
            load(client)
            assertNull(requestMap[call])

            callStart(call)
            assertNotNull(requestMap[call])
            assertEquals(1L, requireNotNull(requestMap[call]).startTime)

            canceled(call)
            assertNull(requestMap[call])
        }
        val map = mapOf(
            "method" to "GET",
            "url" to "https://example.com/",
            "urlParams" to mapOf(
                "debug" to "true"
            ),
            "duration" to 0L,
            "requestContentLength" to 0L
        )
        verify(client, times(1)).leaveBreadcrumb(
            "OkHttp call error",
            map,
            BreadcrumbType.REQUEST
        )
    }

    /**
     * The request body size is captured
     */
    @Test
    fun requestContentLength() {
        val plugin = BugsnagOkHttpPlugin()
        plugin.apply {
            load(client)
            callStart(call)
            val info = requireNotNull(requestMap[call])
            assertEquals(0, info.requestBodyCount)
            requestBodyEnd(call, 2340)
            assertEquals(2340, info.requestBodyCount)
        }
    }

    /**
     * The response body size is captured
     */
    @Test
    fun responseContentLength() {
        val plugin = BugsnagOkHttpPlugin()
        plugin.apply {
            load(client)
            callStart(call)
            val info = requireNotNull(requestMap[call])
            assertEquals(0, info.responseBodyCount)
            responseBodyEnd(call, 5092)
            assertEquals(5092, info.responseBodyCount)
        }
    }

    /**
     * The response status is captured
     */
    @Test
    fun responseStatus() {
        val plugin = BugsnagOkHttpPlugin()
        val request = Request.Builder().url("https://example.com").build()
        val response = Response.Builder()
            .message("Test")
            .protocol(Protocol.HTTP_2)
            .request(request).code(200)
            .build()

        plugin.apply {
            load(client)
            callStart(call)
            val info = requireNotNull(requestMap[call])
            assertEquals(0, info.status)
            responseHeadersEnd(call, response)
            assertEquals(200, info.status)
        }
    }

    /**
     * A breadcrumb is not left when the enabledBreadcrumbTypes does not include
     * [BreadcrumbType.REQUEST]
     */
    @Test
    fun disabledBreadrumbType() {
        val cfg = Configuration("api-key").apply { enabledBreadcrumbTypes = emptySet() }
        `when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig(cfg))

        BugsnagOkHttpPlugin().apply {
            load(client)
            callStart(call)
            callEnd(call)
        }
        verify(client, times(0)).leaveBreadcrumb(
            anyString(),
            anyMap(),
            any()
        )
    }
}
