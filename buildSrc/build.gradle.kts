plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        register("bugsnag-jni-link-table-plugin") {
            id = "bugsnag-jni-link-table-plugin"
            implementationClass = "com.bugsnag.android.gradle.JNILinkTablePlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.android.gradle.api)
}
