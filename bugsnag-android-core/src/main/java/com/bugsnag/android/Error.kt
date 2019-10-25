package com.bugsnag.android

class Error(
    var errorClass: String,
    var errorMessage: String?,
    var stacktrace: List<Stackframe>,
    var type: String = "android"
): JsonStream.Streamable {

    companion object {
        fun createError(exc: Throwable, projectPackages: Collection<String>): List<Error> {
            val errors = mutableListOf<Error>()

            var currentEx: Throwable? = exc
            while (currentEx != null) {
                val trace = Stacktrace(exc.stackTrace, projectPackages)
                errors.add(Error(exc.javaClass.name, exc.localizedMessage, trace.trace))
                currentEx = currentEx.cause
            }
            return errors
        }
    }

    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("errorClass").value(errorClass)
        writer.name("message").value(errorMessage)
        writer.name("type").value(type)
        writer.name("stacktrace").value(stacktrace)
        writer.endObject()
    }
}
