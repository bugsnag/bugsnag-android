package com.bugsnag.android

import com.bugsnag.android.http.HttpInstrumentedRequest
import com.bugsnag.android.http.HttpInstrumentedResponse
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import com.bugsnag.android.Response as BugsnagResponse

internal fun Event.setHttpInfo(
    instrumentedRequest: HttpInstrumentedRequest<Request>,
    instrumentedResponse: HttpInstrumentedResponse<Request, Response>?
) {
    val url = instrumentedRequest.reportedUrl ?: return
    setRequest(
        instrumentedRequest.request.method,
        instrumentedResponse?.response?.protocol.toVersionString(),
        url
    )

    request?.apply {
        bodyLength = instrumentedRequest.request.body?.contentLength() ?: 0L
        body = instrumentedRequest.reportedRequestBody
        instrumentedRequest.request.headers.forEach { (name, value) ->
            addHeader(name, value)
        }
    }

    val okResp = instrumentedResponse?.response
    if (okResp != null) {
        response = BugsnagResponse(okResp.code).apply {
            bodyLength = okResp.body?.contentLength() ?: 0
            body = instrumentedResponse.reportedResponseBody
            okResp.headers.forEach { (name, value) ->
                addHeader(name, value)
            }
        }
    }
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
