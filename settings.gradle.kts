pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.5"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

include(
    ":bugsnag-android",
    ":bugsnag-android-core",
    ":bugsnag-plugin-android-anr",
    ":bugsnag-plugin-android-exitinfo",
    ":bugsnag-plugin-android-ndk",
    ":bugsnag-plugin-react-native",
    ":bugsnag-plugin-android-okhttp",
    ":bugsnag-benchmarks"
)
