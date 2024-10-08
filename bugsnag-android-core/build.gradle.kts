import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("bugsnag-build-plugin")
}

bugsnagBuildOptions {
    usesNdk = true

    // pick up dsl-json by adding to the default sourcesets
    android {
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
    }
}

apply(plugin = "com.android.library")
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

plugins.withId("org.jetbrains.kotlinx.binary-compatibility-validator") {
    project.extensions.getByType(ApiValidationExtension::class.java).ignoredPackages.add("com.bugsnag.android.repackaged.dslplatform.json")
}
