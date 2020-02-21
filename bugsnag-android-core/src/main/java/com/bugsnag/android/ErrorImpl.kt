package com.bugsnag.android

/**
 * An [Error] represents information extracted from a [Throwable].
 */
internal class ErrorImpl @JvmOverloads internal constructor(

    /**
     * The fully-qualified class name of the [Throwable]
     */
    var errorClass: String,

    /**
     * The message string from the [Throwable]
     */
    var errorMessage: String?,

    /**
     * A representation of the stacktrace
     */
    val stacktrace: List<Stackframe>,

    /**
     * The type of error based on the originating platform (intended for internal use only)
     */
    var type: ErrorType = ErrorType.ANDROID
): JsonStream.Streamable {

    internal companion object {
        fun createError(exc: Throwable, projectPackages: Collection<String>, logger: Logger): MutableList<Error> {
            val errors = mutableListOf<ErrorImpl>()

            var currentEx: Throwable? = exc
            while (currentEx != null) {
                val trace = Stacktrace(currentEx.stackTrace, projectPackages, logger)
                errors.add(ErrorImpl(currentEx.javaClass.name, currentEx.localizedMessage, trace.trace))
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
