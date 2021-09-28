package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class ComplexRequestIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a GET request which receives a throttled response exceeding the Call's
     * timeout duration
     */
    @Test
    fun getRequestCallTimeOut() {
        val request = Request.Builder()
        val mockResponse = MockResponse().setBody("This request will fail")

        // simulate Steve's network connection
        mockResponse.setHeadersDelay(1, TimeUnit.SECONDS)
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse) {
            it.callTimeout(250, TimeUnit.MILLISECONDS)
        }

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call error"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(0L, get("requestContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
            assertNull(get("status"))
            assertNull(get("responseContentLength"))
        }
    }

    /**
     * Performs a GET request that times out due to the server not sending any response
     */
    @Test
    fun socketTimeout() {
        val request = Request.Builder()
        val url = makeNetworkBreadcrumbRequest(client, request) {
            it.callTimeout(250, TimeUnit.MILLISECONDS)
        }

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call error"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(0L, get("requestContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
            assertNull(get("status"))
            assertNull(get("responseContentLength"))
        }
    }

    /**
     * Performs a GET request that does not close an empty response body. This is captured
     * as a breadcrumb regardless.
     */
    @Test
    fun getRequestUnclosedBodyEmptyBody() {
        val server = MockWebServer().apply {
            enqueue(MockResponse())
            start()
        }
        val url = server.url("/test")
        val request = Request.Builder().url(url).build()
        val plugin = BugsnagOkHttpPlugin().apply { load(client) }

        // make a request but forget to close the body
        val okHttpClient = OkHttpClient.Builder().eventListener(plugin).build()
        val call = okHttpClient.newCall(request)
        val body = checkNotNull(call.execute().body)
        body.charStream()
        server.shutdown()

        // verify breadcrumb received
        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(200, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url.toString(), get("url"))
        }
    }

    /**
     * Performs a GET request that does not close the response body. This is a resource leak
     * and no breadcrumb will be captured. See
     * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/#the-response-body-must-be-closed
     */
    @Test
    fun getRequestUnclosedBody() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("Plz close me"))
            start()
        }
        val url = server.url("/test")
        val request = Request.Builder().url(url).build()
        val plugin = BugsnagOkHttpPlugin().apply { load(client) }

        // make a request but forget to close the body
        val okHttpClient = OkHttpClient.Builder().eventListener(plugin).build()
        val call = okHttpClient.newCall(request)
        val body = checkNotNull(call.execute().body)
        body.charStream()
        server.shutdown()

        // verify breadcrumb received
        verify(client, times(0)).leaveBreadcrumb(
            anyString(),
            anyMap(),
            any()
        )
    }
}
