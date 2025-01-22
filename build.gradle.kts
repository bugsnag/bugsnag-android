import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    load(Versions.Plugins.AGP) apply false
    load(Versions.Plugins.kotlin) apply false
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
            apiVersion = Versions.kotlinLang
            languageVersion = Versions.kotlinLang
            freeCompilerArgs += listOf(
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Xno-param-assertions"
            )
        }
    }
}
