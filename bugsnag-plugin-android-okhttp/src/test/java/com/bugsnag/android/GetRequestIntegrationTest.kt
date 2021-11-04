package com.bugsnag.android

import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
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
class GetRequestIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Performs a simple GET request with a 200 response.
     */
    @Test
    fun getRequest200() {
        val request = Request.Builder()
        val mockResponse = MockResponse().setBody("hello, world!")
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

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
            assertEquals(url, get("url"))
        }
    }

    /**
     * Performs a GET request to a non-existent resource (404), that includes some URL params.
     */
    @Test
    fun getRequest404() {
        val request = Request.Builder()
        val mockResponse = MockResponse().setResponseCode(404).setBody("Resource not found")
        val path = "/a/9?lang=en&darkMode=true&count=5"
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse, path)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call failed"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(404, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
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
     * Performs a GET request that triggers an internal server error.
     */
    @Test
    fun getRequest500() {
        val request = Request.Builder()
        val mockResponse = MockResponse().setBody("Internal server error.").setResponseCode(500)
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call failed"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(500, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Verifies a long endpoint + URL params are not truncated
     */
    @Test
    fun longEndpointNotTruncated() {
        val request = Request.Builder()
        val mockResponse = MockResponse()
        val path = "/test/endpoints/fifty-nine/spaghetti/custom_resource/foo/123409/amp/shoot/" +
            "the-biggest-bar.aspx?lang=en&highlighted=Hello%20World" +
            "&something_very_very_very_very_very_long=something_very_very_very_very_very_big"
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse, path)

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
     * Performs a simple HEAD request with a 429 response.
     */
    @Test
    fun headRequest429() {
        val request = Request.Builder().head()
        val mockResponse = MockResponse().setResponseCode(429).setBody("Chill out!")
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call failed"),
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
}
