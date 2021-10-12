package com.bugsnag.android

internal class ErrorInternal @JvmOverloads internal constructor(
    var errorClass: String,
    var errorMessage: String?,
    stacktrace: Stacktrace,
    var type: ErrorType = ErrorType.ANDROID
) : JsonStream.Streamable {

    val stacktrace: List<Stackframe> = stacktrace.trace

    @Suppress("UNCHECKED_CAST")
    internal constructor(json: Map<String, Any?>) : this(
        errorClass = json["errorClass"] as String,
        errorMessage = json["message"] as? String,
        type = (json["type"] as? String)?.let { ErrorType.fromDescriptor(it) } ?: ErrorType.ANDROID,
        stacktrace = Stacktrace(
            (json["stacktrace"] as? List<Map<String, Any?>>)?.map { Stackframe(it) }
                ?: emptyList()
        )
    )

    internal companion object {
        fun createError(exc: Throwable, projectPackages: Collection<String>, logger: Logger): MutableList<Error> {
            return exc.safeUnrollCauses()
                .mapTo(mutableListOf()) { currentEx ->
                    // Somehow it's possible for stackTrace to be null in rare cases
                    val stacktrace = currentEx.stackTrace ?: arrayOf<StackTraceElement>()
                    val trace = Stacktrace(stacktrace, projectPackages, logger)
                    val errorInternal =
                        ErrorInternal(currentEx.javaClass.name, currentEx.localizedMessage, trace)

                    return@mapTo Error(errorInternal, logger)
                }
        }
    }

    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("errorClass").value(errorClass)
        writer.name("message").value(errorMessage)
        writer.name("type").value(type.desc)
        writer.name("stacktrace").value(stacktrace)
        writer.endObject()
    }
}
