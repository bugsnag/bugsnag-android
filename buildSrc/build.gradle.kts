plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("bugsnag-build-plugin") {
            id = "bugsnag-build-plugin"
            implementationClass = "com.bugsnag.android.BugsnagBuildPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation("com.android.tools.build:gradle:7.0.1")
}
