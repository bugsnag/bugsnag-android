package com.bugsnag.android

import java.io.IOException
import java.util.Observable

/**
 * Information about the current user of your application.
 */
class User @JvmOverloads internal constructor(
    /**
     * @return the user ID, by default a UUID generated on installation
     */
    val id: String? = null,

    /**
     * @return the user's email, if available
     */
    val email: String? = null,

    /**
     * @return the user's name, if available
     */
    val name: String? = null
) : JsonStream.Streamable {

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("id").value(id)
        writer.name("email").value(email)
        writer.name("name").value(name)
        writer.endObject()
    }

}
