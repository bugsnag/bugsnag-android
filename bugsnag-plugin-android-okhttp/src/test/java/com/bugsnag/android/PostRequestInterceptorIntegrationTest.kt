package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
class PostRequestInterceptorIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a POST request using BugsnagOkHttp interceptor.
     * A breadcrumb is collected for successful requests.
     */
    @Test
    fun postRequestSuccess() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("hello, world!"))
        }
        val baseUrl = server.url("/test")
        val bugsnagOkHttp = BugsnagOkHttp().logBreadcrumbs()
        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val requestBody = "test-data".toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(baseUrl).post(requestBody).build()
        val execute = okHttpClient.newCall(req).execute()
        execute.close()
        server.shutdown()

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(200, get("status"))
            assertEquals(9L, get("requestContentLength"))
            assertEquals(13L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(server.url("/test").toString(), get("url"))
        }
    }
}
