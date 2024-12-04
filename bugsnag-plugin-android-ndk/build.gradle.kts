plugins {
    loadDefaultPlugins()
}

android {
    compileSdk = Versions.Android.Build.compileSdkVersion
    namespace = "com.bugsnag.android.ndk"

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
        named("test") {
            java.srcDir(SHARED_TEST_SRC_DIR)
        }
    }

    buildFeatures.prefabPublishing = true
    prefab.create("bugsnag-ndk") {
        headers = "src/main/jni/include"
    }

    externalNativeBuild.cmake.path = project.file("CMakeLists.txt")
    externalNativeBuild.cmake.version = Versions.Android.Build.cmakeVersion
}

dependencies {
    addCommonModuleDependencies()
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

apply(from = rootProject.file("gradle/detekt.gradle"))
apply(from = rootProject.file("gradle/license-check.gradle"))
apply(from = rootProject.file("gradle/release.gradle"))

configureCheckstyle()
