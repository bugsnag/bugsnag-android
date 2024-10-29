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
     * Report [ApplicationExitInfo] ANRs that do not appear to correspond with BugSnag [Event]s
     * as synthesized errors. These will appear on your dashboard without BugSnag data such as
     * breadcrumbs and metadata, but will report crashes that BugSnag is otherwise unable to catch
     * such as background ANRs.
     */
    var reportUnmatchedAnrs: Boolean = true,

    /**
     * Report [ApplicationExitInfo] native crashes that do not appear to correspond with BugSnag [Event]s
     * as synthesized errors. These will appear on your dashboard without BugSnag data such as
     * breadcrumbs and metadata, but will report crashes that BugSnag is otherwise unable to catch.
     */
    var reportUnmatchedNativeCrashes: Boolean = true,

    /**
     * Turn off event correlation based on the
     * [processStateSummary](ActivityManager.setProcessStateSummary) field. This can set to `true`
     * to stop `BugsnagExitInfoPlugin` overwriting the field if it is being used by the app.
     */
    var disableProcessStateSummaryOverride: Boolean = false
) {
    constructor() : this(
        listOpenFds = true,
        includeLogcat = false,
        reportUnmatchedAnrs = true,
        reportUnmatchedNativeCrashes = true,
        disableProcessStateSummaryOverride = false
    )

    internal fun copy() =
        ExitInfoPluginConfiguration(
            listOpenFds,
            includeLogcat,
            disableProcessStateSummaryOverride,
            reportUnmatchedAnrs,
            reportUnmatchedNativeCrashes
        )

    override fun equals(other: Any?): Boolean {
        return other is ExitInfoPluginConfiguration &&
            listOpenFds == other.listOpenFds &&
            includeLogcat == other.includeLogcat &&
            reportUnmatchedAnrs == other.reportUnmatchedAnrs &&
            reportUnmatchedNativeCrashes == other.reportUnmatchedNativeCrashes &&
            disableProcessStateSummaryOverride == other.disableProcessStateSummaryOverride
    }

    override fun hashCode(): Int {
        var result = listOpenFds.hashCode()
        result = 31 * result + includeLogcat.hashCode()
        result = 31 * result + reportUnmatchedAnrs.hashCode()
        result = 31 * result + reportUnmatchedNativeCrashes.hashCode()
        result = 31 * result + disableProcessStateSummaryOverride.hashCode()
        return result
    }
}
