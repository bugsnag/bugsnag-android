package com.bugsnag.android.okhttp

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Client
import com.bugsnag.android.Plugin
import com.bugsnag.android.redactMap
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

    override fun canceled(call: Call) {
        requestMap.remove(call)
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

    override fun callEnd(call: Call) {
        requestMap.remove(call)?.let { requestInfo ->
            client?.apply {
                leaveBreadcrumb(
                    "OkHttp call succeeded",
                    collateMetadata(call, requestInfo, timeProvider()),
                    BreadcrumbType.REQUEST
                )
            }
        }
    }

    override fun callFailed(call: Call, ioe: IOException) {
        requestMap.remove(call)?.let { requestInfo ->
            client?.apply {
                leaveBreadcrumb(
                    "OkHttp call failed",
                    collateMetadata(call, requestInfo, timeProvider()),
                    BreadcrumbType.REQUEST
                )
            }
        }
    }

    @VisibleForTesting
    internal fun Client.collateMetadata(
        call: Call,
        info: NetworkRequestMetadata,
        nowMs: Long
    ): Map<String, Any> {
        val request = call.request()

        return mapOf(
            "method" to request.method,
            "url" to sanitizeUrl(request),
            "duration" to nowMs - info.startTime,
            "urlParams" to buildQueryParams(request),
            "requestContentLength" to info.requestBodyCount,
            "responseContentLength" to info.responseBodyCount,
            "status" to info.status
        )
    }

    /**
     * Constructs a map of query parameters, redacting any sensitive values.
     */
    private fun Client.buildQueryParams(request: Request): Map<String, Any?> {
        val url = request.url
        val params = mutableMapOf<String, Any?>()

        url.queryParameterNames.forEach { name ->
            val values = url.queryParameterValues(name)
            when (values.size) {
                1 -> params[name] = values.first()
                else -> params[name] = url.queryParameterValues(name)
            }
        }
        return redactMap(params)
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
    var status: Int = -1

    @JvmField
    @Volatile
    var requestBodyCount: Long = -1

    @JvmField
    @Volatile
    var responseBodyCount: Long = -1
}
