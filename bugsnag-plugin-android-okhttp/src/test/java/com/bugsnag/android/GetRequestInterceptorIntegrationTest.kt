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
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class GetRequestInterceptorIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a GET request using BugsnagOkHttp interceptor.
     * A breadcrumb is collected for successful requests.
     */
    @Test
    fun getRequestSuccess() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("hello, world!"))
        }
        val baseUrl = server.url("/test")
        val bugsnagOkHttp = BugsnagOkHttp().logBreadcrumbs()
        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val req = Request.Builder().url(baseUrl).build()
        val execute = okHttpClient.newCall(req).execute()
        execute.close()
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
            assertEquals(13L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(server.url("/test").toString(), get("url"))
        }
    }

    /**
     * Performs a GET request with query parameters using BugsnagOkHttp interceptor.
     * A breadcrumb is collected with url params populated.
     */
    @Test
    fun getRequestWithParamsSuccess() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("hello, world!"))
        }
        val baseUrl = server.url("/test?id=123456&user=test")
        val bugsnagOkHttp = BugsnagOkHttp().logBreadcrumbs()
        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val req = Request.Builder().url(baseUrl).build()
        val execute = okHttpClient.newCall(req).execute()
        execute.close()
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
            assertEquals(13L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            @Suppress("UNCHECKED_CAST")
            val params = get("urlParams") as Map<String, Any>
            assertEquals("test", params["user"])
            assertEquals("123456", params["id"])
            assertEquals(server.url("/test").toString(), get("url"))
        }
    }
}
