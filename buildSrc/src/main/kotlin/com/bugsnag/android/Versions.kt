package com.bugsnag.android

import org.gradle.api.JavaVersion

/**
 * Controls the versions of plugins, dependencies, and build targets used by the SDK.
 */
object Versions {
    // Note minSdkVersion must be >=21 for 64 bit architectures
    val minSdkVersion = 14
    val compileSdkVersion = 34
    val ndk = "23.1.7779620"
    val java = JavaVersion.VERSION_1_7
    val kotlin = "1.4.32"

    // plugins
    val androidGradlePlugin = "7.0.4"
    val detektPlugin = "1.18.1"
    val ktlintPlugin = "10.2.0"
    val dokkaPlugin = "1.5.0"
    val benchmarkPlugin = "1.0.0"
    val protobufPlugin = "0.9.4"

    // dependencies
    val supportLib = "1.1.0"
    val supportTestLib = "1.2.0"
    val espressoTestLib = "3.1.0"
    val junitTestLib = "4.12"
    val mockitoTestLib = "2.28.2"
}