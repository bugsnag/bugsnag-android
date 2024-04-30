plugins {
    id("bugsnag-build-plugin")
    id("com.android.library")
}

bugsnagBuildOptions {
    usesNdk = true
    publishesPrefab = "bugsnag-ndk"
}

dependencies {
    api(project(":bugsnag-android-core"))
}

apply(from = "../gradle/kotlin.gradle")

afterEvaluate {
    tasks.create("prefabReleasePackage") {
        doLast {
            project.fileTree("build/intermediates/prefab_package/") {
                include("**/abi.json")
            }.forEach { file ->
                file.writeText(file.readText().replace("c++_static", "none"))
            }
        }
    }
}
