plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.licenseCheck)
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "com.bugsnag.android"

    configureRelease()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndkVersion = libs.versions.android.ndk.get()
    }
}

dependencies {
    add("api", project(":bugsnag-android-core"))
    add("api", project(":bugsnag-plugin-android-anr"))
    add("api", project(":bugsnag-plugin-android-ndk"))
}

apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))
