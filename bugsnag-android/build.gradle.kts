plugins {
    id("bugsnag-build-plugin")
    id("com.android.library")
}

bugsnagBuildOptions {
    compilesCode = false
}

dependencies {
    add("api", project(":bugsnag-android-core"))
    add("api", project(":bugsnag-plugin-android-anr"))
    add("api", project(":bugsnag-plugin-android-ndk"))
}
