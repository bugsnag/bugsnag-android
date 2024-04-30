plugins {
    id("bugsnag-build-plugin")
    id("com.android.library")
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    api(project(":bugsnag-android-core"))
    implementation("com.google.protobuf:protobuf-javalite:3.24.2")
}

android.libraryVariants.configureEach {
    processJavaResourcesProvider {
        exclude("**/*.proto")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.2"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

apiValidation.ignoredPackages += "com.bugsnag.android.repackaged.server.os"
