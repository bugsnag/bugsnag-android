import org.gradle.api.Project
import org.gradle.plugin.use.PluginDependenciesSpec

const val SHARED_TEST_SRC_DIR = "../bugsnag-android-core/src/sharedTest/java"

object BugsnagDefaults {
    val cmakeArguments
        get() = listOf(
            "-DANDROID_CPP_FEATURES=exceptions",
            "-DANDROID_STL=c++_static"
        )
}

fun Project.configureAbis(abiFilters: MutableSet<String>) {
    val override: String? = project.findProperty("ABI_FILTERS") as String?
    val abis = override?.split(",") ?: mutableSetOf(
        "arm64-v8a",
        "armeabi-v7a",
        "x86",
        "x86_64"
    )

    abiFilters.clear()
    abiFilters.addAll(abis)
}

fun PluginDependenciesSpec.loadDefaultPlugins() {
    load(Versions.Plugins.AGP)
    load(Versions.Plugins.kotlin)
    load(Versions.Plugins.ktlint)
    load(Versions.Plugins.dokka)
    load(Versions.Plugins.detekt)
    load(Versions.Plugins.licenseCheck)
    load(Versions.Plugins.binaryCompatibilityCheck)
    load(Versions.Plugins.checkstyle)
}
