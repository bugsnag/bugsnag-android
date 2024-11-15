plugins {
    loadDefaultPlugins()
    load(Versions.Plugins.protobuf)
}

android {
    compileSdk = Versions.Android.Build.compileSdkVersion
    namespace = "com.bugsnag.android.exitinfo"

    defaultConfig {
        minSdk = Versions.Android.Build.minSdkVersion
        ndkVersion = Versions.Android.Build.ndk

        consumerProguardFiles("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        isAbortOnError = true
        isWarningsAsErrors = true
        isCheckAllWarnings = true
        baseline(File(project.projectDir, "lint-baseline.xml"))
        disable("GradleDependency", "NewerVersionAvailable")
    }

    buildFeatures {
        aidl = false
        renderScript = false
        shaders = false
        resValues = false
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = Versions.java
        targetCompatibility = Versions.java
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        named("test") {
            java.srcDir(SHARED_TEST_SRC_DIR)
        }
    }

    libraryVariants.configureEach {
        processJavaResourcesProvider {
            exclude("**/*.proto")
        }
    }
}

dependencies {
    addCommonModuleDependencies()
    api(project(":bugsnag-android-core"))
    implementation("com.google.protobuf:protobuf-javalite:3.24.2")
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

apply(from = rootProject.file("gradle/detekt.gradle"))
apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))

configureCheckstyle()
