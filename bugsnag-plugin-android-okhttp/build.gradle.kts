plugins {
    id("bugsnag-build-plugin")
    id("com.android.library")
}

dependencies {
    add("api", project(":bugsnag-android-core"))

    add("compileOnly", "com.squareup.okhttp3:okhttp:4.9.1") {
        exclude(group = "org.jetbrains.kotlin")
    }

    add("testImplementation", "com.squareup.okhttp3:mockwebserver:4.9.1") {
        exclude(group = "org.jetbrains.kotlin")
    }
}
