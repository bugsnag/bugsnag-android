package com.bugsnag.android.okhttp

import android.os.SystemClock
import android.util.Base64
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Client
import com.bugsnag.android.ErrorCaptureOptions
import com.bugsnag.android.ErrorOptions
import com.bugsnag.android.Logger
import com.bugsnag.android.http.HttpInstrumentedRequest
import com.bugsnag.android.http.HttpInstrumentedResponse
import com.bugsnag.android.http.HttpRequestCallback
import com.bugsnag.android.http.HttpResponseCallback
import com.bugsnag.android.log
import com.bugsnag.android.shouldDiscardNetworkBreadcrumb
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.util.BitSet

internal class BugsnagOkHttpInterceptor(
    private val errorCodes: BitSet,
    private var maxRequestBodyCapture: Long,
    private var maxResponseBodyCapture: Long,
    private var logRequestBreadcrumbs: Boolean,
    private val requestCallbacks: List<HttpRequestCallback<Request>>,
    private val responseCallbacks: List<HttpResponseCallback<Request, Response>>,
    private val clientSource: () -> Client?,
    private val timeProvider: () -> Long = { SystemClock.elapsedRealtime() }
) : Interceptor {
    private val templateException by lazy {
        RuntimeException("HTTP Error Placeholder")
    }

    private val httpErrorOptions = ErrorOptions(
        ErrorCaptureOptions(stacktrace = false)
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val client = clientSource() ?: return chain.proceed(request)
        val logger = client.log

        val instrumentedRequest = OkHttpInstrumentedRequest(request, maxRequestBodyCapture)
        runRequestCallbacks(logger, instrumentedRequest)

        val startTimeMs = timeProvider()
        val response = runCatching { chain.proceed(request) }
        val endTimeMs = timeProvider()
        try {
            val instrumentedResponse = OkHttpInstrumentedResponse(
                request = request,
                response = response.getOrNull(),
                errorCodes = errorCodes,
                maxResponseBodyCapture = maxResponseBodyCapture,
                reportBreadcrumb = logRequestBreadcrumbs
            )

            runResponseCallbacks(logger, instrumentedResponse)

            actionConfiguredInstrumentation(
                client,
                instrumentedRequest,
                instrumentedResponse,
                endTimeMs - startTimeMs
            )
        } catch (_: Exception) {
        }

        return response.getOrThrow()
    }

    private fun runRequestCallbacks(logger: Logger, req: OkHttpInstrumentedRequest) {
        if (requestCallbacks.isEmpty()) {
            return
        }

        requestCallbacks.forEach { callback ->
            try {
                callback.onHttpRequest(req)
            } catch (ex: Exception) {
                logger.w("HttpRequestCallback threw an exception", ex)
            }
        }
    }

    private fun runResponseCallbacks(logger: Logger, resp: OkHttpInstrumentedResponse) {
        if (responseCallbacks.isEmpty()) {
            return
        }

        responseCallbacks.forEach { callback ->
            try {
                callback.onHttpResponse(resp)
            } catch (ex: Exception) {
                logger.w("HttpResponseCallback threw an exception", ex)
            }
        }
    }

    private fun actionConfiguredInstrumentation(
        client: Client,
        req: OkHttpInstrumentedRequest,
        resp: OkHttpInstrumentedResponse,
        durationMs: Long
    ) {
        if (resp.isBreadcrumbReported && !client.shouldDiscardNetworkBreadcrumb()) {
            val okHttpResponse = resp.response
            val statusCode = okHttpResponse?.code ?: 0
            val isResponseError = statusCode in 400..499 || errorCodes[statusCode]
            val message = when {
                isResponseError -> "OkHttp call error"
                statusCode in 100..399 -> "OkHttp call succeeded"
                else -> "OkHttp call failed"
            }

            client.leaveBreadcrumb(
                message,
                collateMetadata(req, resp, durationMs),
                BreadcrumbType.REQUEST
            )
        }

        if (resp.isErrorReported) {
            client.notify(templateException, httpErrorOptions) { event ->
                val okHttpRequest = resp.request
                val okHttpResponse = resp.response

                val domain = okHttpRequest.url.host

                event.errors.clear()
                event.addError("HTTPError", "${okHttpResponse?.code}: ${okHttpRequest.url}")
                event.context = "${okHttpRequest.method} $domain"
                event.setHttpInfo(req, resp)
                true
            }
        }
    }

    private fun collateMetadata(
        req: OkHttpInstrumentedRequest,
        resp: OkHttpInstrumentedResponse,
        duration: Long
    ): Map<String, Any> {
        val data = LinkedHashMap<String, Any>()
        data["method"] = req.request.method
        req.reportedUrl?.let { data["url"] = it }

        val queryParams = buildQueryParams(req.request)
        if (queryParams.isNotEmpty()) {
            data["urlParams"] = queryParams
        }

        data["requestContentLength"] = req.request.body?.contentLength() ?: 0L

        data["duration"] = duration

        resp.response?.code?.let { data["status"] = it }
        data["responseContentLength"] = resp.response?.body?.contentLength() ?: 0L

        return data
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
}

private class OkHttpInstrumentedRequest(
    private val request: Request,
    private val maxRequestBodyCapture: Long,
) : HttpInstrumentedRequest<Request> {

    private var reportedUrl: String? = sanitizeUrl(request.url)

    private var reportedRequestBody: String? = null
    private var isRequestBodySet: Boolean = false

    override fun getRequest(): Request = request

    override fun getReportedUrl(): String? = reportedUrl

    override fun setReportedUrl(reportedUrl: String?) {
        this.reportedUrl = reportedUrl
    }

    override fun getReportedRequestBody(): String? {
        if (isRequestBodySet) {
            return reportedRequestBody
        } else {
            reportedRequestBody = extractRequestBody()
            isRequestBodySet = true
            return reportedRequestBody
        }
    }

    private fun extractRequestBody(): String? {
        val body = request.body ?: return null

        if (maxRequestBodyCapture <= 0) {
            return null
        }

        // Don't read one-shot or duplex bodies as they can only be consumed once
        // and reading them here would break the actual HTTP request
        if (body.isOneShot() || body.isDuplex()) {
            return null
        }

        return try {
            val buffer = Buffer()
            body.writeTo(buffer)

            // Limit the capture to maxRequestBodyCapture bytes
            val bytesToRead = minOf(buffer.size, maxRequestBodyCapture)
            buffer.readUtf8(bytesToRead)
        } catch (_: Exception) {
            // If we can't read the body (e.g., it's not text or already consumed), return null
            null
        }
    }

    /**
     * Sanitizes the URL by removing query params.
     */
    private fun sanitizeUrl(url: HttpUrl): String {
        val builder = url.newBuilder()

        url.queryParameterNames.forEach { name ->
            builder.removeAllQueryParameters(name)
        }
        return builder.build().toString()
    }

    override fun setReportedRequestBody(requestBody: String?) {
        reportedRequestBody = requestBody
        isRequestBodySet = true
    }
}

private class OkHttpInstrumentedResponse(
    private val request: Request,
    private val response: Response?,
    errorCodes: BitSet,
    private val maxResponseBodyCapture: Long,
    private var reportBreadcrumb: Boolean,
) : HttpInstrumentedResponse<Request, Response> {
    private var reportedResponseBody: String? = null
    private var isResponseBodySet = false

    private var isErrorReported = response != null && errorCodes[response.code]

    override fun getRequest(): Request = request
    override fun getResponse(): Response? = response

    override fun setBreadcrumbReported(isBreadcrumbReported: Boolean) {
        reportBreadcrumb = isBreadcrumbReported
    }

    override fun isBreadcrumbReported(): Boolean {
        return reportBreadcrumb
    }

    override fun setErrorReported(isErrorReported: Boolean) {
        this.isErrorReported = isErrorReported
    }

    override fun isErrorReported(): Boolean {
        return isErrorReported
    }

    override fun getReportedResponseBody(): String? {
        if (isResponseBodySet) {
            return reportedResponseBody
        } else {
            reportedResponseBody = extractResponseBody()
            isResponseBodySet = true
            return reportedResponseBody
        }
    }

    override fun setReportedResponseBody(responseBody: String?) {
        reportedResponseBody = responseBody
        isResponseBodySet = true
    }

    private fun extractResponseBody(): String? {
        if (maxResponseBodyCapture <= 0) {
            return null
        }

        val body = response?.peekBody(maxResponseBodyCapture) ?: return null

        try {
            // Use peekBody to read without consuming the actual response body
            // This creates a copy of the body bytes that can be read safely
            val peekedBody = body.source().peek()

            // Request the data we want to read
            peekedBody.request(maxResponseBodyCapture)

            // Read up to maxResponseBodyCapture bytes
            val bytesToRead = minOf(peekedBody.buffer.size, maxResponseBodyCapture)
            if (bytesToRead <= 0) {
                return null
            }

            val contentType = body.contentType()
            if (contentType != null) {
                if (contentType.subtype == "json" ||
                    contentType.type == "text" ||
                    contentType.charset(null) != null
                ) {
                    val charset = contentType.charset(null)
                    return if (charset != null) {
                        peekedBody.readString(bytesToRead, charset)
                    } else {
                        peekedBody.readUtf8(bytesToRead)
                    }
                }
            }

            return Base64.encodeToString(peekedBody.readByteArray(bytesToRead), Base64.NO_WRAP)
        } catch (_: Exception) {
            // If we can't read the body (e.g., it's not text or an error occurred), return null
        }

        return null
    }
}
