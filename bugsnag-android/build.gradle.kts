plugins {
    id("bugsnag-build-plugin")
}

bugsnagBuildOptions {
    compilesCode = false
}

apply(plugin = "com.android.library")

dependencies {
    add("api", project(":bugsnag-android-core"))
    add("api", project(":bugsnag-plugin-android-anr"))
    add("api", project(":bugsnag-plugin-android-ndk"))
}
