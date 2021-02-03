package com.bugsnag.android

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import java.io.File

/**
 * A plugin which shares build logic between subprojects. This is the recommended way of
 * sharing logic in Gradle builds as it cuts down on repetition, and avoids the pitfalls
 * of the `allProjects {}` DSL.
 *
 * The Android Library plugin must be applied after this plugin.
 *
 * https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html
 */
class BugsnagBuildPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val bugsnag = project.extensions.create(
            "bugsnagBuildOptions",
            BugsnagBuildPluginExtension::class.java,
            project.objects
        )

        // configure AGP with the information it needs to build the project
        project.pluginManager.withPlugin("com.android.library") {
            configureProject(project, bugsnag)
        }
    }

    private fun configureProject(
        project: Project,
        bugsnag: BugsnagBuildPluginExtension
    ) {
        // load 3rd party gradle plugins
        project.applyPlugins(bugsnag)

        val android = project.extensions.getByType(BaseExtension::class.java)
        android.apply {
            configureDefaults()
            configureAndroidLint(project)
            configureTests()
            configureAndroidPackagingOptions()

            if (bugsnag.usesNdk) {
                configureNdk(project)
            }
        }

        // add 3rd party dependencies to the project
        project.addExternalDependencies()

        // apply legacy groovy scripts to configure 3rd party plugins
        project.apply(from = project.file("../gradle/release.gradle"))
        project.apply(from = project.file("../gradle/license-check.gradle"))

        if (bugsnag.compilesCode) {
            project.apply(from = project.file("../gradle/detekt.gradle"))
            project.apply(from = project.file("../gradle/checkstyle.gradle"))
        }
    }

    /**
     * Configures the Android NDK, if it is enabled
     */
    private fun BaseExtension.configureNdk(project: Project) {
        defaultConfig {
            externalNativeBuild.cmake {
                arguments("-DANDROID_CPP_FEATURES=exceptions", "-DANDROID_STL=c++_static")

                val override: String? = project.findProperty("ABI_FILTERS") as String?
                val abis = override?.split(",") ?: mutableSetOf(
                    "arm64-v8a",
                    "armeabi-v7a",
                    "x86",
                    "x86_64"
                )
                ndk.setAbiFilters(abis)
            }
        }
        externalNativeBuild.cmake.path = project.file("CMakeLists.txt")
    }

    /**
     * Configures the compile and package options for the Android artefacts
     */
    private fun BaseExtension.configureAndroidPackagingOptions() {
        buildFeatures.apply {
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
        packagingOptions {
            pickFirst("**/*.so")
        }
    }

    /**
     * Configures the Android Lint static analysis tool
     */
    private fun BaseExtension.configureAndroidLint(project: Project) {
        lintOptions {
            isAbortOnError = true
            isWarningsAsErrors = true
            isCheckAllWarnings = true
            baseline(File(project.projectDir, "lint-baseline.xml"))
            disable("GradleDependency", "NewerVersionAvailable")
        }
    }

    /**
     * Configures options for how the unit test target behaves.
     */
    private fun BaseExtension.configureTests() {
        testOptions {
            unitTests {
                isReturnDefaultValues = true
            }
        }
    }

    /**
     * Adds external libraries to the dependencies for each project, such as the Kotlin stdlib.
     */
    private fun Project.addExternalDependencies() {
        project.dependencies {
            // needs to be kept as 'compile' for license checking to work
            // as otherwise the downloadLicenses task misses these deps
            add("compile", "androidx.annotation:annotation:${Versions.supportLib}")
            add("compile", "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")

            add("testImplementation", "junit:junit:${Versions.junitTestLib}")
            add("testImplementation", "org.mockito:mockito-core:${Versions.mockitoTestLib}")

            add(
                "androidTestImplementation",
                "org.mockito:mockito-android:${Versions.mockitoTestLib}"
            )
            add("androidTestImplementation", "androidx.test:core:${Versions.supportTestLib}")
            add("androidTestImplementation", "androidx.test:runner:${Versions.supportTestLib}")
            add("androidTestImplementation", "androidx.test:rules:${Versions.supportTestLib}")
            add(
                "androidTestImplementation",
                "androidx.test.espresso:espresso-core:${Versions.espressoTestLib}"
            )
        }
    }

    /**
     * Configures Android project defaults such as minSdkVersion.
     */
    private fun BaseExtension.configureDefaults() {
        defaultConfig {
            setCompileSdkVersion(Versions.compileSdkVersion)
            minSdkVersion(Versions.minSdkVersion)
            ndkVersion = Versions.ndk
            consumerProguardFiles("proguard-rules.pro")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    /**
     * Applies gradle plugins to the given project which are always required (e.g.
     * kotlin-android).
     */
    private fun Project.applyPlugins(bugsnag: BugsnagBuildPluginExtension) {
        val plugins = project.plugins
        plugins.apply("com.github.hierynomus.license")

        if (bugsnag.compilesCode) {
            plugins.apply("kotlin-android")
            plugins.apply("io.gitlab.arturbosch.detekt")
            plugins.apply("org.jlleitschuh.gradle.ktlint")
        }
    }
}