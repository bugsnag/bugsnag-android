package com.bugsnag.android

import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.Date

private const val HEADER_API_PAYLOAD_VERSION = "Bugsnag-Payload-Version"
private const val HEADER_BUGSNAG_SENT_AT = "Bugsnag-Sent-At"
private const val HEADER_BUGSNAG_INTEGRITY = "Bugsnag-Integrity"
internal const val HEADER_API_KEY = "Bugsnag-Api-Key"
internal const val HEADER_INTERNAL_ERROR = "Bugsnag-Internal-Error"

/**
 * Supplies the headers which must be used in any request sent to the Error Reporting API.
 *
 * @return the HTTP headers
 */
internal fun errorApiHeaders(payload: EventPayload): Map<String, String?> {
    return mapOf(
        HEADER_API_PAYLOAD_VERSION to "4.0",
        HEADER_API_KEY to payload.apiKey,
        HEADER_BUGSNAG_SENT_AT to DateUtils.toIso8601(Date()),
        HEADER_BUGSNAG_INTEGRITY to computeSha1Digest(payload)
    )
}

/**
 * Supplies the headers which must be used in any request sent to the Session Tracking API.
 *
 * @return the HTTP headers
 */
internal fun sessionApiHeaders(apiKey: String, payload: Session): Map<String, String?> = mapOf(
    HEADER_API_PAYLOAD_VERSION to "1.0",
    HEADER_API_KEY to apiKey,
    HEADER_BUGSNAG_SENT_AT to DateUtils.toIso8601(Date()),
    HEADER_BUGSNAG_INTEGRITY to computeSha1Digest(payload)
)

internal fun computeSha1Digest(payload: JsonStream.Streamable): String? {
    runCatching {
        val shaDigest = MessageDigest.getInstance("SHA-1")
        val builder = StringBuilder("sha1 ")

        // Pipe the object through a no-op output stream
        DigestOutputStream(NullOutputStream(), shaDigest).use { stream ->
            stream.bufferedWriter().use { writer ->
                payload.toStream(JsonStream(writer))
            }
            shaDigest.digest().forEach { byte ->
                builder.append(String.format("%02x", byte))
            }
        }
        return builder.toString()
    }.getOrElse { return null }
}

internal class NullOutputStream : OutputStream() {
    override fun write(b: Int) = Unit
}
