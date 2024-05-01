plugins {
    id("bugsnag-build-plugin")
}

bugsnagBuildOptions {
    usesNdk = true
    publishesPrefab = "bugsnag-ndk"
}

apply(plugin = "com.android.library")

dependencies {
    add("api", project(":bugsnag-android-core"))
}

afterEvaluate {
    tasks.getByName("prefabReleasePackage") {
        doLast {
            project.fileTree("build/intermediates/prefab_package/") {
                include("**/abi.json")
            }.forEach { file ->
                file.writeText(file.readText().replace("c++_static", "none"))
            }
        }
    }
}
