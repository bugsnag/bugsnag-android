import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("bugsnag-build-plugin")
    id("com.android.library")
}

bugsnagBuildOptions {
    usesNdk = true
}

apply(plugin = "org.jetbrains.dokka")

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

// pick up dsl-json by adding to the default sourcesets
android {
    sourceSets {
        named("main") {
            java.srcDirs("dsl-json/library/src/main/java")
        }
        named("test") {
            java.srcDirs("dsl-json/library/src/test/java")
        }
    }
}

apiValidation.ignoredPackages.add("com.bugsnag.android.repackaged.dslplatform.json")
