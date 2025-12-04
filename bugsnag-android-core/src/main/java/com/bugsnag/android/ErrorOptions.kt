package com.bugsnag.android

/**
 * Options for controlling how handled errors are reported.
 */
class ErrorOptions @JvmOverloads constructor(
    /** Controls which data fields are captured during Event creation. */
    var capture: ErrorCaptureOptions = ErrorCaptureOptions()
)

/**
 * Granular flags for controlling data capture at notify time.
 */
class ErrorCaptureOptions(
    /** Whether to capture breadcrumbs. Defaults to true. */
    var breadcrumbs: Boolean = true,

    /** Whether to capture feature flags. Defaults to true. */
    var featureFlags: Boolean = true,

    /** Whether to capture stacktrace. Defaults to true. */
    var stacktrace: Boolean = true,

    /** Whether to capture thread state. Defaults to true. */
    var threads: Boolean = true,

    /** Whether to capture user information. Defaults to true. */
    var user: Boolean = true,

    /**
     * Controls which custom metadata tabs are included.
     * - null: all metadata tabs captured
     * - empty set: no custom metadata captured
     * - set of names: app, device, and specified tabs captured
     *
     * Note: app and device tabs are always captured.
     */
    var metadata: Set<String>? = null,
) {
    /**
     * Create a CaptureOptions with all of the default capturing behaviour (capture everything).
     */
    constructor() :
        // we specify one arg to avoid repeating all of the defaults here
        this(breadcrumbs = true)

    companion object {
        const val CAPTURE_STACKTRACE = 1
        const val CAPTURE_BREADCRUMBS = 2
        const val CAPTURE_FEATURE_FLAGS = 4
        const val CAPTURE_THREADS = 8
        const val CAPTURE_USER = 16

        /**
         * A convenience method to capture only selected event fields using a bit-mask of field
         * names (a mask of [CAPTURE_STACKTRACE], [CAPTURE_BREADCRUMBS], etc.).
         *
         * @param fields a bit mask of the fields to capture
         * @param metadata the metadata tabs to capture (or `null` to capture all)
         */
        @JvmStatic
        @JvmOverloads
        fun captureOnly(fields: Int, metadata: Set<String>? = null): ErrorCaptureOptions {
            return ErrorCaptureOptions(
                stacktrace = (fields and CAPTURE_STACKTRACE) != 0,
                breadcrumbs = (fields and CAPTURE_BREADCRUMBS) != 0,
                featureFlags = (fields and CAPTURE_FEATURE_FLAGS) != 0,
                threads = (fields and CAPTURE_THREADS) != 0,
                user = (fields and CAPTURE_USER) != 0,
                metadata = metadata
            )
        }

        /**
         * Return CaptureOptions that will not capture any optional fields.
         */
        @JvmStatic
        fun captureNothing(): ErrorCaptureOptions {
            return captureOnly(0, emptySet())
        }
    }
}
