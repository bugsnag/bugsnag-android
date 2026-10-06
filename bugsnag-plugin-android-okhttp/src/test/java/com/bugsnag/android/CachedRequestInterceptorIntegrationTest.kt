package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttp
import okhttp3.Cache
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
import kotlin.io.path.createTempDirectory

@RunWith(MockitoJUnitRunner::class)
class CachedRequestInterceptorIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a GET request with caching enabled using BugsnagOkHttp interceptor.
     * A breadcrumb is collected for successful cached requests.
     */
    @Test
    fun getRequestCachedSuccess() {
        val server = MockWebServer().apply {
            enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .addHeader("Cache-Control", "max-age=300")
                    .setBody("hello, world!")
            )
        }
        val baseUrl = server.url("/test")
        val cacheDir = createTempDirectory().toFile()
        val cache = Cache(cacheDir, 1024 * 1024)

        val bugsnagOkHttp = BugsnagOkHttp().logBreadcrumbs()
        val interceptor = bugsnagOkHttp.createInterceptor(client)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .cache(cache)
            .build()

        val req = Request.Builder().url(baseUrl).build()

        // First request - should hit server and cache response
        val execute1 = okHttpClient.newCall(req).execute()
        execute1.close()

        // Second request - should be served from cache
        val execute2 = okHttpClient.newCall(req).execute()
        execute2.close()

        server.shutdown()
        cacheDir.deleteRecursively()

        verify(client, times(2)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )

        val capturedValues = mapCaptor.allValues
        with(capturedValues[0]) {
            assertEquals("GET", get("method"))
            assertEquals(200, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertTrue(get("responseContentLength") is Long)
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(server.url("/test").toString(), get("url"))
        }

        with(capturedValues[1]) {
            assertEquals("GET", get("method"))
            assertEquals(200, get("status"))
            assertEquals(server.url("/test").toString(), get("url"))
        }
    }
}
