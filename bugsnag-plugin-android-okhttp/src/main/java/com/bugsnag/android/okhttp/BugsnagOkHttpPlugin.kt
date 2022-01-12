package com.bugsnag.android.okhttp

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Client
import com.bugsnag.android.Plugin
import com.bugsnag.android.shouldDiscardNetworkBreadcrumb
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * This plugin captures network requests made by OkHttp as breadcrumbs in Bugsnag.
 * It tracks OkHttp calls by extending [EventListener] and leaves a breadcrumb when a
 * call succeeds or failed.
 *
 * To enable this functionality in Bugsnag call [com.bugsnag.android.Configuration.addPlugin]
 * with an instance of this object before calling [com.bugsnag.android.Bugsnag.start].
 *
 * You *must* close the [Response] body as documented by OkHttp. Failing to do so will leak the
 * OkHttp connection and prevent breadcrumbs from being collected. For further information, see:
 * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/#the-response-body-must-be-closed
 */
class BugsnagOkHttpPlugin @JvmOverloads constructor(
    internal val timeProvider: () -> Long = { System.currentTimeMillis() }
) : Plugin, EventListener() {

    internal val requestMap = ConcurrentHashMap<Call, NetworkRequestMetadata>()
    private var client: Client? = null

    override fun load(client: Client) {
        this.client = client
    }

    override fun unload() {
        this.client = null
    }

    override fun callStart(call: Call) {
        requestMap[call] = NetworkRequestMetadata(timeProvider())
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        requestMap[call]?.requestBodyCount = byteCount
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        requestMap[call]?.responseBodyCount = byteCount
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        requestMap[call]?.status = response.code
    }

    override fun callEnd(call: Call) = captureNetworkBreadcrumb(call)
    override fun callFailed(call: Call, ioe: IOException) = captureNetworkBreadcrumb(call)
    override fun canceled(call: Call) = captureNetworkBreadcrumb(call)

    private fun captureNetworkBreadcrumb(call: Call) {
        client?.apply {
            requestMap.remove(call)?.let { requestInfo ->
                if (shouldDiscardNetworkBreadcrumb()) {
                    return
                }
                val result = getRequestResult(requestInfo)
                leaveBreadcrumb(
                    result.message,
                    collateMetadata(call, requestInfo, result, timeProvider()),
                    BreadcrumbType.REQUEST
                )
            }
        }
    }

    @VisibleForTesting
    internal fun Client.collateMetadata(
        call: Call,
        info: NetworkRequestMetadata,
        result: RequestResult,
        nowMs: Long
    ): Map<String, Any> {
        val request = call.request()

        val data = mutableMapOf<String, Any>(
            "method" to request.method,
            "url" to sanitizeUrl(request),
            "duration" to nowMs - info.startTime,
            "requestContentLength" to info.requestBodyCount
        )

        val queryParams = buildQueryParams(request)
        if (queryParams.isNotEmpty()) {
            data["urlParams"] = queryParams
        }

        // only add response body length + status for requests that did not error
        if (result != RequestResult.ERROR) {
            data["responseContentLength"] = info.responseBodyCount
            data["status"] = info.status
        }
        return data.toMap()
    }

    /**
     * Constructs a map of query parameters, redacting any sensitive values.
     */
    private fun buildQueryParams(request: Request): Map<String, Any?> {
        val url = request.url
        val params = mutableMapOf<String, Any?>()

        url.queryParameterNames.forEach { name ->
            val values = url.queryParameterValues(name)
            when (values.size) {
                1 -> params[name] = values.first()
                else -> params[name] = url.queryParameterValues(name)
            }
        }
        return params
    }

    /**
     * Sanitizes the URL by removing query params.
     */
    private fun sanitizeUrl(request: Request): String {
        val url = request.url
        val builder = url.newBuilder()

        url.queryParameterNames.forEach { name ->
            builder.removeAllQueryParameters(name)
        }
        return builder.build().toString()
    }
}

internal fun getRequestResult(requestInfo: NetworkRequestMetadata) =
    when (requestInfo.status) {
        in 100..399 -> RequestResult.SUCCESS
        in 400..599 -> RequestResult.FAILURE
        else -> RequestResult.ERROR
    }

internal enum class RequestResult(val message: String) {
    SUCCESS("OkHttp call succeeded"),
    FAILURE("OkHttp call failed"),
    ERROR("OkHttp call error");
}

/**
 * Stores stateful information about the in-flight network request, and contains functions
 * that construct breadcrumb metadata from this information.
 */
internal class NetworkRequestMetadata(
    @JvmField
    val startTime: Long
) {

    @JvmField
    @Volatile
    var status: Int = 0

    @JvmField
    @Volatile
    var requestBodyCount: Long = 0

    @JvmField
    @Volatile
    var responseBodyCount: Long = 0
}
