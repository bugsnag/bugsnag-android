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
    implementation("com.android.tools.build:gradle-api:7.0.4")
}
