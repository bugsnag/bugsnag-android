plugins {
    id("bugsnag-build-plugin")
    id("com.android.library")
}

dependencies {
    add("api", project(":bugsnag-android-core"))
}
