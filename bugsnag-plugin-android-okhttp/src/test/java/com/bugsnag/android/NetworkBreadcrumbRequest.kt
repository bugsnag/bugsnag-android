package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.mockito.Mockito
import java.io.IOException

/**
 * Makes a HTTP request to a MockWebServer that returns a mocked response. A
 * BugsnagOkHttpPlugin is loaded which means that any completed network request should
 * correspond to a breadcrumb being left on the Client.
 */
internal fun makeNetworkBreadcrumbRequest(
    client: Client,
    request: Request.Builder,
    response: MockResponse? = null,
    path: String = "/test",
    action: (OkHttpClient.Builder) -> Unit = {}
): String {
    val server = MockWebServer().apply {
        response?.let(this::enqueue)
        start()
    }
    val baseUrl = server.url(path)
    val plugin = BugsnagOkHttpPlugin().apply {
        load(client)
    }
    val builder = OkHttpClient.Builder().eventListener(plugin)
    action(builder)
    val okHttpClient = builder.build()

    // assert that leaveBreadcrumb has not been called
    Mockito.verify(client, Mockito.times(0))
        .leaveBreadcrumb(Mockito.anyString(), Mockito.anyMap(), Mockito.any())

    val req = request.url(baseUrl).build()
    val call = okHttpClient.newCall(req)
    try {
        call.execute().close()
    } catch (ignored: IOException) {
    }
    server.shutdown()
    return baseUrl.toString()
}
