package com.bugsnag.android.okhttp

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Client
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NetworkBreadcrumbIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    /**
     * Performs a simple GET request with a successful response.
     */
    @Test
    fun simpleGetRequest() {
        // prepare the request + response
        val request = Request.Builder()
        val mockResponse = MockResponse().setBody("hello, world!")

        // make the request and verify what was sent
        makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("GET", get("method"))
            assertEquals(200, get("status"))
            assertEquals(-1L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertEquals(emptyMap<String, Any>(), get("urlParams"))

            val url = get("url") as String
            assertTrue(url.startsWith("http://localhost:"))
            assertTrue(url.endsWith("/test"))
        }
    }

    // TODO add more integration tests
    // TODO add tests for requests with no request body/response body (confirm metadata behaviour)
    // TODO test unclosed OkHttp response body
    // TODO test redirected requests
}
