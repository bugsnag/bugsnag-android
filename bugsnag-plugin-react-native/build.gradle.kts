plugins {
    loadDefaultPlugins()
}

android {
    compileSdk = Versions.Android.Build.compileSdkVersion
    namespace = "com.bugsnag.android.reactnative"

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
}

dependencies {
    addCommonModuleDependencies()
    add("api", project(":bugsnag-android-core"))
}

apply(from = rootProject.file("gradle/detekt.gradle"))
apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))

configureCheckstyle()
