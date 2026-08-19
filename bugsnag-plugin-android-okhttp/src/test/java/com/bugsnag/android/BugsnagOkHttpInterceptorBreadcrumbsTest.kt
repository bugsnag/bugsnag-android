package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttp
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
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException

/**
 * Tests for breadcrumb functionality using the BugsnagOkHttp interceptor instead of EventListener.
 */
@RunWith(MockitoJUnitRunner::class)
class BugsnagOkHttpInterceptorBreadcrumbsTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        `when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a simple GET request with a 200 response using interceptor.
     */
    @Test
    fun getRequest200WithInterceptor() {
        val request = Request.Builder()
        val expectedResponseBody = "hello, world!"
        val mockResponse = MockResponse().setBody(expectedResponseBody)
        val url = makeInterceptorBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(200, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(expectedResponseBody.length.toLong(), get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Performs a GET request to a non-existent resource (404) using interceptor.
     */
    @Test
    fun getRequest404WithInterceptor() {
        val request = Request.Builder()
        val expectedResponseBody = "Resource not found"
        val mockResponse = MockResponse().setResponseCode(404).setBody(expectedResponseBody)
        val path = "/a/9?lang=en&darkMode=true&count=5"
        val url = makeInterceptorBreadcrumbRequest(client, request, mockResponse, path)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call error"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(404, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(expectedResponseBody.length.toLong(), get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertEquals(
                mapOf(
                    "lang" to "en",
                    "darkMode" to "true",
                    "count" to "5"
                ),
                get("urlParams")
            )
            assertEquals(url.substringBefore("?"), get("url"))
        }
    }

    /**
     * Performs a GET request that triggers an internal server error using interceptor.
     */
    @Test
    fun getRequest500WithInterceptor() {
        val request = Request.Builder()
        val expectedResponseBody = "Internal server error."
        val mockResponse = MockResponse().setBody(expectedResponseBody).setResponseCode(500)
        val url = makeInterceptorBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call failed"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(500, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(expectedResponseBody.length.toLong(), get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Verifies a long endpoint + URL params are not truncated when using interceptor.
     */
    @Test
    fun longEndpointNotTruncatedWithInterceptor() {
        val request = Request.Builder()
        val mockResponse = MockResponse()
        val path = "/test/endpoints/fifty-nine/spaghetti/custom_resource/foo/123409/amp/shoot/" +
            "the-biggest-bar.aspx?lang=en&highlighted=Hello%20World" +
            "&something_very_very_very_very_very_long=something_very_very_very_very_very_big"
        val url = makeInterceptorBreadcrumbRequest(client, request, mockResponse, path)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals(url.substringBefore("?"), get("url"))
            assertEquals(
                mapOf(
                    "lang" to "en",
                    "highlighted" to "Hello World",
                    "something_very_very_very_very_very_long" to "something_very_very_very_very_very_big"
                ),
                get("urlParams")
            )
        }
    }

    /**
     * Performs a simple HEAD request with a 429 response using interceptor.
     */
    @Test
    fun headRequest429WithInterceptor() {
        val request = Request.Builder().head()
        val mockResponse = MockResponse().setResponseCode(429).setBody("Rate limited")
        val url = makeInterceptorBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call error"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("HEAD", get("method"))
            assertEquals(429, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Tests that network errors (IOException) generate error breadcrumbs using interceptor.
     */
    @Test
    fun networkErrorWithInterceptor() {
        val server = MockWebServer()
        server.start()
        val baseUrl = server.url("/test")

        // Close server immediately to simulate network error
        server.shutdown()

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()
        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder().url(baseUrl).build()
        val call = okHttpClient.newCall(request)
        try {
            call.execute().close()
        } catch (ignored: IOException) {
            // Expected for this test
        }

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call failed"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(0L, get("requestContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(baseUrl.toString(), get("url"))
            // No status code for network errors
            assertNull(get("status"))
        }
    }

    /**
     * Tests that breadcrumbs are not logged when disabled using interceptor.
     */
    @Test
    fun breadcrumbsDisabledWithInterceptor() {
        val request = Request.Builder()
        val mockResponse = MockResponse().setBody("hello, world!")

        // Create interceptor with breadcrumbs disabled
        makeInterceptorBreadcrumbRequest(
            client,
            request,
            mockResponse,
            breadcrumbsEnabled = false
        )

        // Verify no breadcrumb was logged
        verify(client, times(0)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
    }

    /**
     * Tests that breadcrumbs are not logged when REQUEST breadcrumb type is disabled.
     */
    @Test
    fun requestBreadcrumbTypeDisabledWithInterceptor() {
        val cfg = Configuration("api-key").apply { enabledBreadcrumbTypes = emptySet() }
        `when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig(cfg))

        val request = Request.Builder()
        val mockResponse = MockResponse().setBody("hello, world!")
        makeInterceptorBreadcrumbRequest(client, request, mockResponse)

        // Verify no breadcrumb was logged when REQUEST type is disabled
        verify(client, times(0)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
    }

    /**
     * Tests custom error codes configured on the interceptor.
     */
    @Test
    fun customErrorCodesWithInterceptor() {
        val request = Request.Builder()
        val mockResponse = MockResponse().setResponseCode(418).setBody("I'm a teapot")

        // Configure 418 as an error code
        val url = makeInterceptorBreadcrumbRequest(
            client,
            request,
            mockResponse,
            configureErrorCodes = { it.addHttpErrorCode(418) }
        )

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call error"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(418, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Tests that default error codes (4xx, 5xx) work with interceptor.
     */
    @Test
    fun defaultErrorCodesWithInterceptor() {
        // Test with 404 (should be treated as error by default based on errorCodes BitSet)
        val request = Request.Builder()
        val mockResponse = MockResponse().setResponseCode(404).setBody("Not found")

        // Configure default error codes
        val url = makeInterceptorBreadcrumbRequest(
            client,
            request,
            mockResponse,
            configureErrorCodes = { it.addHttpErrorCodes(400, 599) }
        )

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call error"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(404, get("status"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Helper function to make HTTP requests with the BugsnagOkHttp interceptor.
     */
    private fun makeInterceptorBreadcrumbRequest(
        client: Client,
        request: Request.Builder,
        response: MockResponse? = null,
        path: String = "/test",
        breadcrumbsEnabled: Boolean = true,
        configureErrorCodes: ((BugsnagOkHttp) -> BugsnagOkHttp)? = null
    ): String {
        val server = MockWebServer().apply {
            response?.let(this::enqueue)
            start()
        }
        val baseUrl = server.url(path)

        var bugsnagOkHttp = BugsnagOkHttp()
        if (breadcrumbsEnabled) {
            bugsnagOkHttp = bugsnagOkHttp.logBreadcrumbs()
        }

        configureErrorCodes?.let {
            bugsnagOkHttp = it(bugsnagOkHttp)
        }

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val req = request.url(baseUrl).build()
        val call = okHttpClient.newCall(req)
        try {
            call.execute().close()
        } catch (ignored: IOException) {
            // Expected for some tests
        }
        server.shutdown()
        return baseUrl.toString()
    }
}
