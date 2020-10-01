package com.bugsnag.android

import java.util.Date

private const val HEADER_API_PAYLOAD_VERSION = "Bugsnag-Payload-Version"
private const val HEADER_BUGSNAG_SENT_AT = "Bugsnag-Sent-At"
internal const val HEADER_API_KEY = "Bugsnag-Api-Key"
internal const val HEADER_INTERNAL_ERROR = "Bugsnag-Internal-Error"

/**
 * Supplies the headers which must be used in any request sent to the Error Reporting API.
 *
 * @return the HTTP headers
 */
internal fun errorApiHeaders(apiKey: String) = mapOf(
    HEADER_API_PAYLOAD_VERSION to "4.0",
    HEADER_API_KEY to apiKey,
    HEADER_BUGSNAG_SENT_AT to DateUtils.toIso8601(Date())
)

/**
 * Supplies the headers which must be used in any request sent to the Session Tracking API.
 *
 * @return the HTTP headers
 */
internal fun sessionApiHeaders(apiKey: String) = mapOf(
    HEADER_API_PAYLOAD_VERSION to "1.0",
    HEADER_API_KEY to apiKey,
    HEADER_BUGSNAG_SENT_AT to DateUtils.toIso8601(Date())
)
