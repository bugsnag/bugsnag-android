plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        register("bugsnag-ndk-table-plugin") {
            id = "bugsnag-ndk-table-plugin"
            implementationClass = "com.bugsnag.android.gradle.JNILinkTablePlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    implementation("com.android.tools.build:gradle-api:7.0.4")
}
