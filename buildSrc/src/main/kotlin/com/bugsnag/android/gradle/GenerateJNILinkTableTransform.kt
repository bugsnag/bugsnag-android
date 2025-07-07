package com.bugsnag.android.gradle

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class GenerateJNILinkTableTransform(
    api: Int,
    classVisitor: ClassVisitor?,
    val outputDirectory: Directory
) : ClassVisitor(api, classVisitor) {

    private var className: String? = null

    private var nativeMethods: MutableList<JNIMethod> = mutableListOf()

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name?.substringAfterLast('/')
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitEnd() {
        // write the JNI link table for the class to header file
        className?.let { className ->
            if (nativeMethods.isNotEmpty()) {
                generateLinkHeader(className, nativeMethods)
            }
        }

        super.visitEnd()
    }

    private fun generateLinkHeader(className: String, methods: List<JNIMethod>) {
        val headerFile = outputDirectory.file("${className}_JNI.h").asFile
        headerFile.printWriter().use { writer ->
            writer.println("// JNI Link Table for $className")
            writer.println("#include <jni.h>")
            writer.println("#ifndef ${className}_JNI_H")
            writer.println("#define ${className}_JNI_H")
            writer.println()

            writer.println("#define ${className}_JNI_METHODS_COUNT ${methods.size}")

            writer.println("static const JNINativeMethod ${className}_JNIMethods[] = {");
            for (i in methods.indices) {
                val method = methods[i]

                writer.print("    {\"")
                writer.print(method.name)
                writer.print("\", \"")
                writer.print(method.descriptor)
                writer.print("\", &JNI_")
                writer.print(className)
                writer.print('_')
                writer.print(method.name)
                writer.print("}")

                if (i < methods.size - 1) {
                    writer.println(",")
                } else {
                    writer.println()
                }
            }
            writer.println("};");

            writer.println()
            writer.println("#endif // ${className}_JNI_H")
        }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (access and Opcodes.ACC_NATIVE != 0 && name != null && descriptor != null) {
            nativeMethods.add(JNIMethod(name, descriptor))
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    private data class JNIMethod(
        val name: String,
        val descriptor: String,
    )
}

abstract class GenerateJNILinkTableTransformFactory :
    AsmClassVisitorFactory<GenerateJNILinkTableParameters> {
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return GenerateJNILinkTableTransform(
            Opcodes.ASM7,
            nextClassVisitor,
            parameters.get().outputDirectory.get()
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val className = classData.className
        val packagePrefix = parameters.get().packagePrefix.get()

        // Check if the class is in the specified package
        return className.startsWith(packagePrefix)
    }
}

interface GenerateJNILinkTableParameters : InstrumentationParameters {
    @get:Input
    val packagePrefix: Property<String>

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty
}
