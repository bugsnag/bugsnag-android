import org.gradle.api.Project
import com.android.build.api.dsl.LibraryExtension

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

fun LibraryExtension.configureRelease() {
    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}
