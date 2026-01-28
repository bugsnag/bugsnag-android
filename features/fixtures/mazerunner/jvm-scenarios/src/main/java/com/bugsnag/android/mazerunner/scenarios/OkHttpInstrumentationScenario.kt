package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.net.Uri
import android.os.StrictMode
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.mazerunner.log
import com.bugsnag.android.okhttp.BugsnagOkHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.regex.Pattern
import kotlin.concurrent.thread

private const val MAX_CAPTURE_BYTES = 32L
private val JSON = "application/json".toMediaType()

open class OkHttpInstrumentationScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        // revert StrictMode to its defaults, the version of okhttp we test with trips up
        // StrictMode: android.os.strictmode.NonSdkApiUsedViolation:
        //      Lcom/android/org/conscrypt/OpenSSLSocketImpl;->setUseSessionTickets(Z)V
        // on some devices, and we can't reasonably fix this in the scenario
        StrictMode.enableDefaults()
    }

    protected open val instrumentation by lazy {
        BugsnagOkHttp()
            .maxRequestBodyCapture(MAX_CAPTURE_BYTES)
            .maxResponseBodyCapture(MAX_CAPTURE_BYTES)
            .logBreadcrumbs()
            .addHttpErrorCodes(400, 599)
            .addResponseCallback { response ->
                response.errorCallback = OnErrorCallback { event ->
                    event.addMetadata("OkHttpInstrumentationScenario", "onErrorCallback", true)
                    true
                }
            }
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(instrumentation.createInterceptor())
            .build()
    }

    private fun reflectionUrl(status: Int): Uri {
        return Uri.parse(config.endpoints.notify)
            .buildUpon()
            .path("/reflect")
            .appendQueryParameter("status", status.toString())
            .appendQueryParameter("password", "secret")
            .build()
    }

    init {
        config.redactedKeys = setOf(
            "Cookie".toPattern(Pattern.LITERAL or Pattern.CASE_INSENSITIVE),
            "Authorization".toPattern(Pattern.LITERAL or Pattern.CASE_INSENSITIVE),
            ".*password.*".toPattern(Pattern.CASE_INSENSITIVE)
        )
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
            // we expect the output URL to not have a query string, so we help the scenario feature
            // out by removing it in the reflection payload (allowing an "equals" match)
            payload.put("url", reflectionUrl.buildUpon().clearQuery().build())
            payload.put("status", status)

            val body = payload.toString().toRequestBody(JSON)

            val requestBuilder = Request.Builder()
                .url(reflectionUrl.toString())
                .header("Authorization", "Bearer OpenSesame")
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
