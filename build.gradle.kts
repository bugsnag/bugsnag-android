import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compatibility) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.ktlint) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    gradle.projectsEvaluated {
        tasks.withType<JavaCompile> {
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
    }
}

subprojects {
    tasks.withType(KotlinCompile::class.java).configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            apiVersion = libs.versions.kotlin.lang.get()
            languageVersion = libs.versions.kotlin.lang.get()
            freeCompilerArgs += listOf(
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Xno-param-assertions"
            )
        }
    }
}
