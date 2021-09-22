package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ClonedRequestTest {

    @Mock
    lateinit var client: Client

    /**
     * Verifies that cloning a [okhttp3.Call] and submitting it while the original is in-flight
     * results in the request being tracked separately.
     */
    @Test
    fun testBreadcrumbCaptured() {
        val request = Request.Builder()
        val server = MockWebServer().apply { start() }
        val baseUrl = server.url("/test")
        val plugin = BugsnagOkHttpPlugin().apply { load(client) }
        val okHttpClient = OkHttpClient.Builder().build()

        // create a call and clone it
        val req = request.url(baseUrl).build()
        val call = okHttpClient.newCall(req)
        val clone = call.clone()
        plugin.callStart(call)
        plugin.callStart(clone)
        assertEquals(2, plugin.requestMap.size)
    }
}
