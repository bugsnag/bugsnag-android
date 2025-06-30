package com.bugsnag.android.gradle

import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

/**
 * Generates C header files for use with [RegisterNatives](docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html#RegisterNatives)
 * for the Bugsnag Android SDK. This plugin ensures that the Java/Kotlin classes with native methods
 * have corresponding C functions as part of the build process. When the headers are included
 * in the C or C++ code, any missing implementations will cause a build error.
 */
class JNILinkTablePlugin : Plugin<Project> {
    /*
     * This plugin generates JNI headers for the Bugsnag Android SDK by registering an
     * identity ASM transformer to AGP. The transformer processes the classes, but does not
     * modify them. Instead it just captures the native method signatures to generate the headers.
     */

    override fun apply(project: Project) {
        // Create a directory for generated headers
        val generatedHeadersDir = project.layout.buildDirectory.dir("generated/jni-headers")

        // Get the Android components extension
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        // Configure each variant
        androidComponents.onVariants { variant ->
            project.configureVariant(variant, generatedHeadersDir)
        }
    }

    private fun Project.configureVariant(
        variant: Variant,
        generatedHeadersDir: Provider<Directory>
    ) {
        val externalNativeBuild = variant.externalNativeBuild ?: return

        // configure the variant to generate the headers from the class files
        variant.transformClassesWith(
            GenerateJNILinkTableTransformFactory::class.java,
            InstrumentationScope.PROJECT
        ) { parameters ->
            parameters.packagePrefix.set("com.bugsnag.android")
            parameters.outputDirectory.set(generatedHeadersDir)
        }

        fixTaskOrdering(variant)

        // Get the physical directory
        val headersDir = generatedHeadersDir.get().asFile

        // Ensure the directory exists
        headersDir.mkdirs()

        // Configure CMake to include our generated headers directory
        externalNativeBuild.cFlags.apply {
            // Add the include directory to the CMake arguments
            add("-I${headersDir.absolutePath}")
        }
    }

    private fun Project.fixTaskOrdering(variant: Variant) {
        val variantName = variant.name.capitalize()
        val transformTaskName = "transform${variantName}ClassesWithAsm"
        // Ensure the CMake task runs after the classes are transformed
        tasks.whenTaskAdded {
            when (name) {
                "buildCMakeDebug", "buildCMakeRelWithDebInfo" -> {
                    dependsOn(transformTaskName)
                }
            }
        }
    }
}