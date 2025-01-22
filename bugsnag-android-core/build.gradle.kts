import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    loadDefaultPlugins()
}

android {
    compileSdk = Versions.Android.Build.compileSdkVersion
    namespace = "com.bugsnag.android.core"

    defaultConfig {
        minSdk = Versions.Android.Build.minSdkVersion
        ndkVersion = Versions.Android.Build.ndk

        consumerProguardFiles("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild.cmake.arguments += BugsnagDefaults.cmakeArguments

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
    externalNativeBuild.cmake.version = Versions.Android.Build.cmakeVersion
}

dependencies {
    addCommonModuleDependencies()
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
