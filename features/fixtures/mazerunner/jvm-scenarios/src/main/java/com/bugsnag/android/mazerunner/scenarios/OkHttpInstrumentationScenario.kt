package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.net.Uri
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.log
import com.bugsnag.android.okhttp.BugsnagOkHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

private const val MAX_CAPTURE_BYTES = 32L
private val JSON = "application/json".toMediaType()

class OkHttpInstrumentationScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    private val instrumentation = BugsnagOkHttp()
        .maxRequestBodyCapture(MAX_CAPTURE_BYTES)
        .maxResponseBodyCapture(MAX_CAPTURE_BYTES)
        .logBreadcrumbs()
        .addHttpErrorCodes(400, 599)

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(instrumentation.createInterceptor())
        .build()

    private fun reflectionUrl(status: Int): String {
        return Uri.parse(config.endpoints.notify)
            .buildUpon()
            .path("/reflect")
            .appendQueryParameter("status", status.toString())
            .build()
            .toString()
    }

    private fun requestType(): Pair<String, Int> {
        val type = eventMetadata?.takeIf { it.isNotBlank() } ?: return ("GET" to 200)
        val (method, status) = type.split(' ')
        return method to status.toInt()
    }

    override fun startScenario() {
        super.startScenario()

        // background thread to avoid networking on main
        thread {
            val (method, status) = requestType()
            val reflectionUrl = reflectionUrl(status)

            val payload = JSONObject()
            payload.put("padding", "this is a string, and it goes on and on until it stops...here")
            payload.put("url", reflectionUrl)

            val body = payload.toString().toRequestBody(JSON)

            val requestBuilder = Request.Builder()
                .url(reflectionUrl)
                .method(method, body.takeIf { method != "GET" })

            log("Sending request to $reflectionUrl")

            val call = httpClient.newCall(requestBuilder.build())
            call.execute().use { response ->
                log("Received ${response.code} response code")
                response.body?.use { body ->
                    log("Response Body: '${body.string()}'")
                }
            }
        }
    }
}
