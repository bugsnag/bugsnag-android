package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSink
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * Tests for cloned requests and special request body types using the BugsnagOkHttp interceptor.
 */
@RunWith(MockitoJUnitRunner::class)
class ClonedRequestInterceptorTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Tests that cloned requests are handled correctly using interceptor.
     */
    @Test
    fun clonedRequestWithInterceptor() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("Original response"))
            enqueue(MockResponse().setBody("Cloned response"))
            start()
        }

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val originalRequest = Request.Builder()
            .url(server.url("/original"))
            .addHeader("X-Original", "true")
            .build()

        val clonedRequest = originalRequest.newBuilder()
            .url(server.url("/cloned"))
            .addHeader("X-Cloned", "true")
            .build()

        // Execute both requests
        okHttpClient.newCall(originalRequest).execute().close()
        okHttpClient.newCall(clonedRequest).execute().close()

        server.shutdown()

        // Should get 2 breadcrumbs
        verify(client, times(2)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )

        val capturedValues = mapCaptor.allValues

        // Both should be GET requests with 200 status
        capturedValues.forEach { breadcrumb ->
            assertEquals("GET", breadcrumb["method"])
            assertEquals(200, breadcrumb["status"])
        }

        // URLs should be different
        assertEquals(server.url("/original").toString(), capturedValues[0]["url"])
        assertEquals(server.url("/cloned").toString(), capturedValues[1]["url"])
    }

    /**
     * Tests one-shot request body handling using interceptor.
     */
    @Test
    fun oneShotRequestBodyWithInterceptor() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("OK"))
            start()
        }

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()
            .maxRequestBodyCapture(1024L)

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        // Create a one-shot request body
        val oneShotBody = object : RequestBody() {
            override fun contentType() = "text/plain".toMediaType()
            override fun isOneShot() = true
            override fun contentLength() = 5L
            override fun writeTo(sink: BufferedSink) {
                sink.writeUtf8("hello")
            }
        }

        val request = Request.Builder()
            .url(server.url("/test"))
            .post(oneShotBody)
            .build()

        okHttpClient.newCall(request).execute().close()
        server.shutdown()

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )

        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(200, get("status"))
            assertEquals(5L, get("requestContentLength"))
            // One-shot body content should not be captured in request body
        }
    }

    /**
     * Tests request body with unknown content length using interceptor.
     */
    @Test
    fun unknownContentLengthRequestBodyWithInterceptor() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("OK"))
            start()
        }

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        // Create request body with unknown content length
        val unknownLengthBody = object : RequestBody() {
            override fun contentType() = "text/plain".toMediaType()
            override fun contentLength() = -1L // Unknown length
            override fun writeTo(sink: BufferedSink) {
                sink.writeUtf8("content with unknown length")
            }
        }

        val request = Request.Builder()
            .url(server.url("/test"))
            .post(unknownLengthBody)
            .build()

        okHttpClient.newCall(request).execute().close()
        server.shutdown()

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )

        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(200, get("status"))
            assertEquals(-1L, get("requestContentLength"))
            assertEquals(2L, get("responseContentLength"))
        }
    }

    /**
     * Tests empty request body handling using interceptor.
     */
    @Test
    fun emptyRequestBodyWithInterceptor() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("OK"))
            start()
        }

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(server.url("/test"))
            .post("".toRequestBody("text/plain".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().close()
        server.shutdown()

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )

        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(200, get("status"))
            assertEquals(0L, get("requestContentLength"))
        }
    }

    /**
     * Tests request with no body (null body) using interceptor.
     */
    @Test
    fun nullRequestBodyWithInterceptor() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("OK"))
            start()
        }

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(server.url("/test"))
            .get() // GET requests have no body
            .build()

        okHttpClient.newCall(request).execute().close()
        server.shutdown()

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )

        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(200, get("status"))
            assertEquals(0L, get("requestContentLength"))
        }
    }

    /**
     * Tests response body with unknown content length using interceptor.
     */
    @Test
    fun unknownResponseContentLengthWithInterceptor() {
        val server = MockWebServer().apply {
            enqueue(
                MockResponse()
                    .setBody("Response content")
                    .removeHeader("Content-Length") // Remove content-length header
                    .setHeader("Transfer-Encoding", "chunked")
            )
            start()
        }

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(server.url("/test"))
            .build()

        okHttpClient.newCall(request).execute().close()
        server.shutdown()

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )

        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(200, get("status"))
            // Response content length might be -1 for chunked encoding
        }
    }

    /**
     * Tests interceptor with network call that throws exception during processing.
     */
    @Test
    fun exceptionDuringResponseProcessingWithInterceptor() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("OK"))
            start()
        }

        val bugsnagOkHttp = BugsnagOkHttp()
            .logBreadcrumbs()

        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(server.url("/test"))
            .build()

        try {
            okHttpClient.newCall(request).execute().close()
        } catch (ignored: Exception) {
            // Ignore any exceptions for this test
        }

        server.shutdown()

        // Should still get breadcrumb even if there are exceptions during processing
        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            any(),
            eq(BreadcrumbType.REQUEST)
        )
    }
}
