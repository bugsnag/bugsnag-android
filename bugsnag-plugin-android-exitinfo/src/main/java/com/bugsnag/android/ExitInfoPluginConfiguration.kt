package com.bugsnag.android

class ExitInfoPluginConfiguration(
    /**
     * Whether to add the list of open file descriptors to correlated reports
     */
    var listOpenFds: Boolean = true,

    /**
     * Whether to report stored logcat messages metadata
     */
    var includeLogcat: Boolean = false,

    /**
     * Turn off event correlation based on the
     * [processStateSummary](ActivityManager.setProcessStateSummary) field. This can set to `true`
     * to stop `BugsnagExitInfoPlugin` overwriting the field if it is being used by the app.
     */
    var disableProcessStateSummaryOverride: Boolean = false
) {
    constructor() : this(true, false, false)

    internal fun copy() =
        ExitInfoPluginConfiguration(listOpenFds, includeLogcat, disableProcessStateSummaryOverride)

    override fun equals(other: Any?): Boolean {
        return other is ExitInfoPluginConfiguration &&
            listOpenFds == other.listOpenFds &&
            includeLogcat == other.includeLogcat &&
            disableProcessStateSummaryOverride == other.disableProcessStateSummaryOverride
    }

    override fun hashCode(): Int {
        var result = listOpenFds.hashCode()
        result = 31 * result + includeLogcat.hashCode()
        result = 31 * result + disableProcessStateSummaryOverride.hashCode()
        return result
    }
}
