plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compatibility)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.licenseCheck)
    checkstyle
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "com.bugsnag.android.anr"

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndkVersion = libs.versions.android.ndk.get()

        consumerProguardFiles("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild.cmake.arguments += listOf(
            "-DANDROID_CPP_FEATURES=exceptions",
            "-DANDROID_STL=c++_static"
        )

        configureAbis(ndk.abiFilters)
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

    externalNativeBuild.cmake.path = project.file("CMakeLists.txt")
    externalNativeBuild.cmake.version = libs.versions.cmake.get()
}

dependencies {
    api(libs.bundles.common.api)
    testImplementation(libs.bundles.test.jvm)
    androidTestImplementation(libs.bundles.test.android)
    add("api", project(":bugsnag-android-core"))
}

apply(from = rootProject.file("gradle/detekt.gradle"))
apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))

configureCheckstyle()
