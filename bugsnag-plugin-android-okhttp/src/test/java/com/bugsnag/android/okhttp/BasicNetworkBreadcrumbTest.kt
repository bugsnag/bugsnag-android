package com.bugsnag.android.okhttp

import com.bugsnag.android.Client
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BasicNetworkBreadcrumbTest {

    @Mock
    lateinit var client: Client

    @Test
    fun testBreadcrumbCaptured() {
        // prepare the request + response
        val request = Request.Builder()
        val mockResponse = MockResponse().setBody("hello, world!")

        // make the request and verify what was sent
        makeNetworkBreadcrumbRequest(client, request, mockResponse)

        // TODO verify the breadcrumb once implementation complete
        // verify(client, times(1)).leaveBreadcrumb(anyString(), anyMap(), any())
    }
}
