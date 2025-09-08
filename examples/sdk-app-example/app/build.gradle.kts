import com.bugsnag.gradle.dsl.release
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.bugsnag.gradle)
}

android {
    namespace = "com.example.bugsnag.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bugsnag.android"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("config") {
            keyAlias = "password"
            keyPassword = "password"
            storeFile = file("../fakekeys.jks")
            storePassword = "password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs["config"]
        }
    }

    externalNativeBuild.cmake .path = file("CMakeLists.txt")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildFeatures.prefab = true
        packagingOptions.jniLibs.pickFirsts += mutableSetOf("**/libbugsnag-ndk.so")
    }
}

bugsnag {
    variants {
        release {
            autoUploadBundle = true
        }
    }
}

dependencies {
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(kotlin("stdlib"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.bugsnag.android)
    implementation(libs.google.android.material)
    implementation(libs.bugsnag.okhttp)
    implementation(libs.squareup.okhttp3)
    implementation(platform(libs.androidx.compose.bom))
}