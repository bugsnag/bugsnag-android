plugins {
    id("bugsnag-build-plugin")
    id("com.android.library")
}

bugsnagBuildOptions {
    usesNdk = true
}

dependencies {
    add("api", project(":bugsnag-android-core"))
}
