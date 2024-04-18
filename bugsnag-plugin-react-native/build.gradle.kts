plugins {
    id("bugsnag-build-plugin")
}

apply(plugin = "com.android.library")

dependencies {
    add("api", project(":bugsnag-android-core"))
}
