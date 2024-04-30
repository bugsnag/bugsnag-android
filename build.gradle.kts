buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.1")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.9.0")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:10.2.0")
        classpath("androidx.benchmark:benchmark-gradle-plugin:1.1.1")
    }
}

plugins {
    id("com.github.hierynomus.license") version "0.16.1"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.1" apply false
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