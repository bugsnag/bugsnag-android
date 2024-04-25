package com.bugsnag.android

import java.util.UUID

internal data class TraceCorrelation(val traceId: UUID, val spanId: Long) : JsonStream.Streamable {
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
            .name("traceid").value(traceId.toHexString())
            .name("spanid").value(spanId.toHexString())
        writer.endObject()
    }

    private fun UUID.toHexString(): String {
        return "%016x%016x".format(mostSignificantBits, leastSignificantBits)
    }

    private fun Long.toHexString(): String {
        return "%016x".format(this)
    }
}
