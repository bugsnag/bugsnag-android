package com.bugsnag.android.okhttp

import com.bugsnag.android.Event
import com.bugsnag.android.http.HttpInstrumentedRequest
import com.bugsnag.android.http.HttpInstrumentedResponse
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import kotlin.math.max
import com.bugsnag.android.Request as BugsnagRequest
import com.bugsnag.android.Response as BugsnagResponse

internal fun Event.setHttpInfo(
    instrumentedRequest: HttpInstrumentedRequest<Request>,
    instrumentedResponse: HttpInstrumentedResponse<Request, Response>?
) {
    val url = instrumentedRequest.reportedUrl ?: return
    request = BugsnagRequest(
        instrumentedResponse?.response?.protocol.toVersionString(),
        instrumentedRequest.request.method,
        url
    )

    request?.apply {
        val okReq = instrumentedRequest.request
        bodyLength = bodyLengthOf(okReq)
        body = instrumentedRequest.reportedRequestBody
        okReq.headers.forEach { (name, value) ->
            addHeader(name, value)
        }

        val queryParams = okReq.url.queryParameterNames
        queryParams.forEach { queryKey ->
            addQueryParameter(queryKey, okReq.url.queryParameter(queryKey))
        }
    }

    val okResp = instrumentedResponse?.response
    if (okResp != null) {
        response = BugsnagResponse(okResp.code).apply {
            bodyLength = bodyLengthOf(okResp)
            body = instrumentedResponse.reportedResponseBody
            okResp.headers.forEach { (name, value) ->
                addHeader(name, value)
            }
        }
    }
}

private fun bodyLengthOf(request: Request): Long {
    val requestBody = request.body ?: return 0
    return max(requestBody.contentLength(), 0)
}

private fun bodyLengthOf(response: Response): Long {
    val requestBody = response.body ?: return 0
    return max(requestBody.contentLength(), 0)
}

@Suppress("DEPRECATION")
private fun Protocol?.toVersionString(): String? = when (this) {
    Protocol.HTTP_1_1 -> "HTTP/1.1"
    Protocol.HTTP_1_0 -> "HTTP/1.0"
    Protocol.H2_PRIOR_KNOWLEDGE, Protocol.HTTP_2 -> "HTTP/2.0"
    Protocol.SPDY_3 -> "SPDY/3.1"
    Protocol.QUIC -> "QUIC"
    null -> null
}
