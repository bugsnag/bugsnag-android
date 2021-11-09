package com.bugsnag.android

import android.util.JsonReader
import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.Journalable
import java.io.IOException

/**
 * Information about the current user of your application.
 */
class User internal constructor(
    data: Map<String, String?> = mutableMapOf()
) : JsonStream.Streamable, Journalable {

    private val map: Map<String, String?> = data.withDefault { null }

    /**
     * @return the user ID, by default a UUID generated on installation
     */
    val id: String? by map

    /**
     * @return the user's email, if available
     */
    val email: String? by map

    /**
     * @return the user's name, if available
     */
    val name: String? by map

    internal constructor(id: String?, email: String?, name: String?) : this(
        mutableMapOf(
            JournalKeys.keyId to id,
            JournalKeys.keyEmail to email,
            JournalKeys.keyName to name
        )
    )

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) = writer.value(toJournalSection())

    override fun toJournalSection(): Map<String, Any?> = map

    internal companion object : JsonReadable<User> {

        override fun fromReader(reader: JsonReader): User {
            var user: User
            with(reader) {
                beginObject()
                val map = mutableMapOf<String, String?>()
                while (hasNext()) {
                    map[nextName()] = nextString()
                }
                user = User(map)
                endObject()
            }
            return user
        }
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
