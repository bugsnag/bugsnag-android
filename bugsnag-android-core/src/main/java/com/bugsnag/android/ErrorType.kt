package com.bugsnag.android

/**
 * Represents the type of error captured
 */
enum class ErrorType(internal val desc: String) {

    /**
     * An error captured from Android's JVM layer
     */
    ANDROID("android"),

    /**
     * An error captured from JavaScript
     */
    BROWSER_JS("browserjs"),

    /**
     * An error captured from Android's C layer
     */
    C("c")
}
