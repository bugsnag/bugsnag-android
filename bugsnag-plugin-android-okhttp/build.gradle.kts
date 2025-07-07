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
    namespace = "com.bugsnag.android.okhttp"

    configureRelease()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndkVersion = libs.versions.android.ndk.get()

        consumerProguardFiles("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkAllWarnings = true
        baseline = File(project.projectDir, "lint-baseline.xml")
        disable += setOf("GradleDependency", "NewerVersionAvailable")
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
    api(libs.bundles.common.api)
    add("api", project(":bugsnag-android-core"))

    add("compileOnly", "com.squareup.okhttp3:okhttp:4.9.1") {
        exclude(group = "org.jetbrains.kotlin")
    }

    add("testImplementation", "com.squareup.okhttp3:mockwebserver:4.9.1") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation(libs.bundles.test.jvm)
    androidTestImplementation(libs.bundles.test.android)
}

apply(from = rootProject.file("gradle/detekt.gradle"))
apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))

configureCheckstyle()
