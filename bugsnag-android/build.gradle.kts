plugins {
    load(Versions.Plugins.AGP)
    load(Versions.Plugins.licenseCheck)
}

android {
    compileSdk = Versions.Android.Build.compileSdkVersion
    namespace = "com.bugsnag.android"

    defaultConfig {
        minSdk = Versions.Android.Build.minSdkVersion
        ndkVersion = Versions.Android.Build.ndk
    }
}

dependencies {
    add("api", project(":bugsnag-android-core"))
    add("api", project(":bugsnag-plugin-android-anr"))
    add("api", project(":bugsnag-plugin-android-ndk"))
}

apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))
