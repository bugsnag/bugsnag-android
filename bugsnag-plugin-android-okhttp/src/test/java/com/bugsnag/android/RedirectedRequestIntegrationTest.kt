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
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RedirectedRequestIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a GET request that follows a redirect. A breadcrumb is collected for the last call
     * in the OkHttp chain.
     */
    @Test
    fun getRequestRedirectSuccess() {
        // create a redirect and then the actual response
        val server = MockWebServer().apply {
            enqueue(
                MockResponse()
                    .setResponseCode(301)
                    .addHeader("Location", url("/foo"))
            )
            enqueue(MockResponse().setBody("hello, world!"))
        }
        val baseUrl = server.url("/test")
        val plugin = BugsnagOkHttpPlugin().apply { load(client) }
        val okHttpClient = OkHttpClient.Builder().eventListener(plugin).build()

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
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(server.url("/test").toString(), get("url"))
        }
    }
}
