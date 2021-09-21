package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
import java.nio.file.Files

@RunWith(MockitoJUnitRunner::class)
class CachedRequestIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a GET request that is served from the local cache
     */
    @Test
    fun cachedGetRequest200() {
        val body = "Hello, world!"
        val response = MockResponse()
            .setBody(body)
            .addHeader("Cache-Control", "public")
            .addHeader("Cache-Control", "max-age=3600")
        val server = MockWebServer().apply {
            enqueue(response)
            start()
        }
        val url = server.url("/test")
        val request = Request.Builder()
            .url(url)
            .addHeader("Cache-Control", "max-stale=86400")
            .build()
        val plugin = BugsnagOkHttpPlugin().apply { load(client) }

        // create a client with a cache
        val cache = Cache(Files.createTempDirectory("cache").toFile(), 1024 * 1024)
        val okHttpClient = OkHttpClient.Builder()
            .eventListener(plugin)
            .cache(cache)
            .build()

        // make first request which hits network
        verifyFirstRequest(okHttpClient, request, url)

        // make second request which hits cache
        verifySecondRequest(okHttpClient, request, url)
        server.shutdown()
    }

    private fun verifyFirstRequest(
        okHttpClient: OkHttpClient,
        request: Request,
        url: HttpUrl
    ) {
        val call = okHttpClient.newCall(request)
        call.execute().use { response ->
            assertEquals(200, response.code)
            assertNull(response.cacheResponse)
            assertNotNull(response.networkResponse)
        }

        // verify breadcrumb received for first request
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
            assertEquals(url.toString(), get("url"))
        }
    }

    private fun verifySecondRequest(
        okHttpClient: OkHttpClient,
        request: Request,
        url: HttpUrl
    ) {
        val forceCacheRequest = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
        val call = okHttpClient.newCall(forceCacheRequest)
        call.execute().use { response ->
            assertEquals(200, response.code)
            assertNotNull(response.cacheResponse)
            assertNull(response.networkResponse)
        }

        // verify breadcrumb received for first request
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
            assertEquals(url.toString(), get("url"))
        }
    }
}
