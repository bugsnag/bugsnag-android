package com.bugsnag.android

import java.io.IOException

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (email != other.email) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

}
