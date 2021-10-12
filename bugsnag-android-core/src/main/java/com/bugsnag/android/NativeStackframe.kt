package com.bugsnag.android

import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.Journalable
import java.io.IOException

/**
 * Represents a single native stackframe
 */
class NativeStackframe internal constructor(

    /**
     * The name of the method that was being executed
     */
    var method: String?,

    /**
     * The location of the source file
     */
    var file: String?,

    /**
     * The line number within the source file this stackframe refers to
     */
    var lineNumber: Number?,

    /**
     * The address of the instruction where the event occurred.
     */
    var frameAddress: Long?,

    /**
     * The address of the function where the event occurred.
     */
    var symbolAddress: Long?,

    /**
     * The address of the library where the event occurred.
     */
    var loadAddress: Long?
) : JsonStream.Streamable, Journalable {

    constructor(json: Map<String, Any?>) : this(
        json[JournalKeys.keyMethod] as? String,
        json[JournalKeys.keyFile] as? String,
        json[JournalKeys.keyLineNumber] as? Number,
        json[JournalKeys.keyFrameAddress] as? Long,
        json[JournalKeys.keySymbolAddress] as? Long,
        json[JournalKeys.keyLoadAddress] as? Long
    )
    /**
     * The type of the error
     */
    var type: ErrorType? = ErrorType.C

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) = writer.value(toJournalSection())

    override fun toJournalSection(): Map<String, Any?> = mapOf<String, Any?>(
        JournalKeys.keyMethod to method,
        JournalKeys.keyFile to file,
        JournalKeys.keyLineNumber to lineNumber,
        JournalKeys.keyFrameAddress to frameAddress,
        JournalKeys.keySymbolAddress to symbolAddress,
        JournalKeys.keyLoadAddress to loadAddress,
        JournalKeys.keyType to type?.desc
    ).filterValues { it != null }
}
