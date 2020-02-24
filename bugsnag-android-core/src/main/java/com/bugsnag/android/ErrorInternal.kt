package com.bugsnag.android

internal class ErrorInternal @JvmOverloads internal constructor(
    var errorClass: String,
    var errorMessage: String?,
    val stacktrace: List<Stackframe>,
    var type: ErrorType = ErrorType.ANDROID
): JsonStream.Streamable {

    internal companion object {
        fun createError(exc: Throwable, projectPackages: Collection<String>, logger: Logger): MutableList<Error> {
            val errors = mutableListOf<ErrorInternal>()

            var currentEx: Throwable? = exc
            while (currentEx != null) {
                val trace = Stacktrace(currentEx.stackTrace, projectPackages, logger)
                errors.add(ErrorInternal(currentEx.javaClass.name, currentEx.localizedMessage, trace.trace))
                currentEx = currentEx.cause
            }
            return errors.map { Error(it, logger) }.toMutableList()
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
