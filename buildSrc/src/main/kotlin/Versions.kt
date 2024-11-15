import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * Controls the versions of plugins, dependencies, and build targets used by the SDK.
 */
object Versions {
    val java = JavaVersion.VERSION_1_8
    val kotlin = "1.6.0"
    val kotlinLang = "1.5"

    object Plugins {
        val AGP = ModuleWithVersion("com.android.library", "7.0.4")
        val kotlin = ModuleWithVersion("org.jetbrains.kotlin.android", Versions.kotlin)
        val detekt = ModuleWithVersion("io.gitlab.arturbosch.detekt", "1.23.1")
        val ktlint = ModuleWithVersion("org.jlleitschuh.gradle.ktlint", "10.2.0")
        val dokka = ModuleWithVersion("org.jetbrains.dokka", "1.9.20")
        val binaryCompatibilityCheck = ModuleWithVersion(
            "org.jetbrains.kotlinx.binary-compatibility-validator", "0.13.1")
        val licenseCheck = ModuleWithVersion("com.github.hierynomus.license", "0.16.1")

        val protobuf = ModuleWithVersion("com.google.protobuf", "0.9.4")

        val checkstyle = ModuleWithVersion("checkstyle")
    }

    object Android {
        // dependencies
        val supportLib = "1.1.0"
        val supportTestLib = "1.2.0"
        val espressoTestLib = "3.1.0"
        val junitTestLib = "4.12"
        val mockitoTestLib = "2.28.2"

        object Build {
            // Note minSdkVersion must be >=21 for 64 bit architectures
            val minSdkVersion = 14
            val compileSdkVersion = 34

            val ndk = "23.1.7779620"
            val cmakeVersion = "3.22.1"
        }
    }

    data class ModuleWithVersion(
        val id: String,
        val version: String? = null,
    )
}

fun PluginDependenciesSpec.load(module: Versions.ModuleWithVersion): PluginDependencySpec {
    var plugin = id(module.id)
    if (module.version != null) {
        plugin = plugin.version(module.version)
    }

    return plugin
}

fun DependencyHandler.addCommonModuleDependencies() {
    // needs to be kept as 'compile' for license checking to work
    // as otherwise the downloadLicenses task misses these deps
    add("api", "androidx.annotation:annotation:${Versions.Android.supportLib}")
    add("api", "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")

    add("testImplementation", "junit:junit:${Versions.Android.junitTestLib}")
    add("testImplementation", "org.mockito:mockito-core:${Versions.Android.mockitoTestLib}")
    add("testImplementation", "org.mockito:mockito-inline:${Versions.Android.mockitoTestLib}")
    add("testImplementation", "androidx.test:core:${Versions.Android.supportTestLib}")

    add(
        "androidTestImplementation",
        "org.mockito:mockito-android:${Versions.Android.mockitoTestLib}"
    )
    add("androidTestImplementation", "androidx.test:core:${Versions.Android.supportTestLib}")
    add("androidTestImplementation", "androidx.test:runner:${Versions.Android.supportTestLib}")
    add("androidTestImplementation", "androidx.test:rules:${Versions.Android.supportTestLib}")
    add(
        "androidTestImplementation",
        "androidx.test.espresso:espresso-core:${Versions.Android.espressoTestLib}"
    )
}
