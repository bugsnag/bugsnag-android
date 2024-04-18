plugins {
    id("bugsnag-build-plugin")
}

bugsnagBuildOptions {
    usesNdk = true
}

apply(plugin = "com.android.library")

dependencies {
    add("api", project(":bugsnag-android-core"))
}
apply(from = "../gradle/kotlin.gradle")
