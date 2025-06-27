package com.bugsnag.android.gradle

import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

class JNILinkTablePlugin : Plugin<Project> {
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