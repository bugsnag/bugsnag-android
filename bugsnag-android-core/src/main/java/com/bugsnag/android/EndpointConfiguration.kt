package com.bugsnag.android

private const val NOTIFY_ENDPOINT = "https://notify.bugsnag.com"
private const val SESSIONS_ENDPOINT = "https://sessions.bugsnag.com"
private const val CONFIGURATION_ENDPOINT = "https://config.bugsnag.com/error-config"

/**
 * Set the endpoints to send data to. By default we'll send error reports to
 * https://notify.bugsnag.com, and sessions to https://sessions.bugsnag.com, but you can
 * override this if you are using Bugsnag Enterprise to point to your own Bugsnag endpoints.
 */
class EndpointConfiguration(

    /**
     * Configures the endpoint to which events should be sent
     */
    val notify: String = NOTIFY_ENDPOINT,

    /**
     * Configures the endpoint to which sessions should be sent
     */
    val sessions: String = SESSIONS_ENDPOINT,

    /**
     * Configures the endpoint to retrieve configuration from
     */
    val configuration: String? = CONFIGURATION_ENDPOINT
) {

    constructor(
        notify: String = NOTIFY_ENDPOINT,
        sessions: String = SESSIONS_ENDPOINT
    ) : this(notify, sessions, null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EndpointConfiguration

        if (notify != other.notify) return false
        if (sessions != other.sessions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = notify.hashCode()
        result = 31 * result + sessions.hashCode()
        return result
    }
}
