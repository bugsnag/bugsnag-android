package com.bugsnag.android.okhttp

import com.bugsnag.android.Client
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.mockito.Mockito

/**
 * Makes a HTTP request to a MockWebServer that returns a mocked response. A
 * BugsnagOkHttpPlugin is loaded which means that any completed network request should
 * correspond to a breadcrumb being left on the Client.
 */
internal fun makeNetworkBreadcrumbRequest(
    client: Client,
    request: Request.Builder,
    response: MockResponse,
    path: String = "/test"
) {
    val server = MockWebServer().apply {
        enqueue(response)
        start()
    }
    val baseUrl = server.url(path)
    val plugin = BugsnagOkHttpPlugin().apply {
        load(client)
    }
    val okHttpClient = OkHttpClient.Builder()
        .eventListener(plugin)
        .build()

    // assert that leaveBreadcrumb has not been called
    Mockito.verify(client, Mockito.times(0))
        .leaveBreadcrumb(Mockito.anyString(), Mockito.anyMap(), Mockito.any())

    okHttpClient.newCall(request.url(baseUrl).build()).execute()
    server.shutdown()
}
