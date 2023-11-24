package com.bugsnag.android

import com.bugsnag.android.SessionFilenameInfo.Companion.defaultFilename
import com.bugsnag.android.SessionFilenameInfo.Companion.findTimestampInFilename
import com.bugsnag.android.internal.ImmutableConfig
import java.io.File
import java.util.Calendar
import java.util.Comparator
import java.util.Date

/**
 * Store and flush Sessions which couldn't be sent immediately due to
 * lack of network connectivity.
 */
internal class SessionStore(
    private val config: ImmutableConfig,
    logger: Logger,
    delegate: Delegate?
) : FileStore(
    File(
        config.persistenceDirectory.value, "bugsnag/sessions"
    ),
    config.maxPersistedSessions,
    SESSION_COMPARATOR,
    logger,
    delegate
) {
    fun isTooOld(file: File?): Boolean {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -60)
        return findTimestampInFilename(file!!) < cal.timeInMillis
    }

    fun getCreationDate(file: File?): Date {
        return Date(findTimestampInFilename(file!!))
    }

    companion object {
        val SESSION_COMPARATOR: Comparator<in File?> = Comparator { lhs, rhs ->
            if (lhs == null && rhs == null) {
                return@Comparator 0
            }
            if (lhs == null) {
                return@Comparator 1
            }
            if (rhs == null) {
                return@Comparator -1
            }
            val lhsName = lhs.name
            val rhsName = rhs.name
            lhsName.compareTo(rhsName)
        }
    }

    override fun getFilename(obj: Any?): String {
        val sessionInfo = defaultFilename(obj, config)
        return sessionInfo.encode()
    }
}
