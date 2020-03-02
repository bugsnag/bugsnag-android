package com.bugsnag.android

/**
 * Holds information about a state change event which is serialized by the BugsnagReactNative
 * class and emitted to the JS layer.
 */
class MessageEvent(

    /**
     * The type of the event. E.g. `UpdateContext`
     */
    val type: String,

    /**
     * The data which underwent a state change. For instance, if a user called
     * `Bugsnag.setContext()`, the new context value would be present in this field.
     */
    val data: Any?
)
