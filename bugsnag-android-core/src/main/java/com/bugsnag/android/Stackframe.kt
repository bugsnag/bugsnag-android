package com.bugsnag.android

import java.io.IOException

/**
 * Represents a single stackframe from a [Throwable]
 */
class Stackframe @JvmOverloads internal constructor(

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
     * Whether the package is considered to be in your project for the purposes of grouping and
     * readability on the Bugsnag dashboard. Project package names can be set in
     * [Configuration.projectPackages]
     */
    var inProject: Boolean?,

    /**
     * Lines of the code surrounding the frame, where the lineNumber is the key (React Native only)
     */
    var code: Map<String, String?>? = null,

    /**
     * The column number of the frame (React Native only)
     */
    var columnNumber: Number? = null
) : JsonStream.Streamable {

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("method").value(method)
        writer.name("file").value(file)
        writer.name("lineNumber").value(lineNumber)
        writer.name("inProject").value(inProject)
        writer.name("columnNumber").value(columnNumber)

        code?.let { map: Map<String, String?> ->
            writer.name("code")

            map.forEach {
                writer.beginObject()
                writer.name(it.key)
                writer.value(it.value)
                writer.endObject()
            }
        }
        writer.endObject()
    }
}
