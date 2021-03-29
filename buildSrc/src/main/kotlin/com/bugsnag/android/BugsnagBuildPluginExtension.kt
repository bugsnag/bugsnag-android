package com.bugsnag.android

import org.gradle.api.model.ObjectFactory

/**
 * Controls how the Bugsnag Build Plugin should be applied to a given module. This interface
 * allows for unwanted behaviour to be switched off - e.g., modules which don't use the NDK
 * can disable it.
 */
open class BugsnagBuildPluginExtension(@Suppress("UNUSED_PARAMETER") objects: ObjectFactory) {

    /**
     * Whether this project compiles code or not. If this is set to false then unnecessary
     * plugins are not applied, which speeds up the build. By default this is enabled.
     */
    open var compilesCode: Boolean = true

    /**
     * Whether the project uses the Android NDK or not. By default this is disabled.
     */
    open var usesNdk: Boolean = false

}
