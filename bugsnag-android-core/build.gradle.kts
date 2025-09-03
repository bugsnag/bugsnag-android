import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaTask

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
    namespace = "com.bugsnag.android.core"

    configureRelease()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndkVersion = libs.versions.android.ndk.get()

        consumerProguardFiles("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild.cmake.arguments += BugsnagDefaults.cmakeArguments

        configureAbis(ndk.abiFilters)
    }

    lint {
        lintConfig = file("lint.xml")

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

    kotlinOptions {
        jvmTarget = Versions.java.toString()
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        named("main") {
            java.srcDirs("dsl-json/library/src/main/java")
        }

        named("test") {
            java.srcDirs(
                "dsl-json/library/src/test/java",
                "src/sharedTest/java"
            )
        }
        named("androidTest") {
            java.srcDirs(
                "src/sharedTest/java"
            )
        }
    }

    externalNativeBuild.cmake.path = project.file("CMakeLists.txt")
    externalNativeBuild.cmake.version = libs.versions.cmake.get()
}

dependencies {
    api(libs.bundles.common.api)

    testImplementation(libs.bundles.test.jvm)
    androidTestImplementation(libs.bundles.test.android)
}

tasks.getByName<DokkaTask>("dokkaHtml") {
    dokkaSourceSets {
        named("main") {
            noAndroidSdkLink.set(false)
            perPackageOption {
                matchingRegex.set("com\\.bugsnag\\.android\\..*")
                suppress.set(true)
            }
        }
    }
}

plugins.withId("org.jetbrains.kotlinx.binary-compatibility-validator") {
    project.extensions.getByType(ApiValidationExtension::class.java).ignoredPackages.add("com.bugsnag.android.repackaged.dslplatform.json")
}

apply(from = rootProject.file("gradle/detekt.gradle"))
apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))

configureCheckstyle()
