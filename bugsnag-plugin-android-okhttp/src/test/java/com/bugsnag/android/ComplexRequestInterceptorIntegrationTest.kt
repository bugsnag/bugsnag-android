package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
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
class ComplexRequestInterceptorIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a complex GET request using BugsnagOkHttp interceptor.
     * A breadcrumb is collected for successful requests.
     */
    @Test
    fun complexGetRequestSuccess() {
        val server = MockWebServer().apply {
            enqueue(
                MockResponse()
                    .setBody("hello, world!")
                    .addHeader("X-Custom-Header", "custom-value")
            )
        }
        val baseUrl = server.url("/complex/test?param1=value1&param2=value2")
        val bugsnagOkHttp = BugsnagOkHttp().logBreadcrumbs()
        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val req = Request.Builder()
            .url(baseUrl)
            .addHeader("User-Agent", "Test-Client/1.0")
            .addHeader("Accept", "application/json")
            .build()
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
            assertEquals("value1", params["param1"])
            assertEquals("value2", params["param2"])
            assertEquals(server.url("/complex/test").toString(), get("url"))
        }
    }
}
