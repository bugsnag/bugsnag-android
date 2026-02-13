plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("androidx.benchmark") version "1.4.1"
}

android {
    compileSdk = 36
    namespace = "com.bugsnag.android.benchmark"

    testOptions.targetSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"

        // suppress warnings that the CPU clock is not locked, and allow running on an emulator.
        // it's not possible to lock the CPU clock without rooting a device, which isn't possible
        // on our CI setup currently.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,LOW_BATTERY,UNLOCKED"
    }

    testBuildType = "release"
    buildTypes {
        debug {
            // Since debuggable can't be modified by gradle for library modules,
            // it must be done in a manifest - see src/androidTest/AndroidManifest.xml
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "benchmark-proguard-rules.pro"
            )
        }
        release {
            isDefault = true
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation(libs.junit)
    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.4.1")

    // Add your dependencies here. Note that you cannot benchmark code
    // in an app module this way - you will need to move any code you
    // want to benchmark to a library module:
    // https://developer.android.com/studio/projects/android-library#Convert
    implementation(project(":bugsnag-android"))
}
