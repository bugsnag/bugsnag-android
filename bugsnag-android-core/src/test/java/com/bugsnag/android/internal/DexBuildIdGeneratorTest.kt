package com.bugsnag.android.internal

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.util.UUID

internal class DexBuildIdGeneratorTest {
    @Test
    fun extractDexBuildId() {
        val signature = DexBuildIdGenerator.extractDexSignature(
            javaClass.getResourceAsStream("classes.dex")!!.readBytes()
        )

        assertEquals("84c5b09bfec95798281eedd4ebb857ef4a05a56b", signature?.toHexString())
    }

    @Test
    fun extractApkBuildId() {
        val apkFile = File.createTempFile("empty-app-debug", ".apk")
        apkFile.outputStream().buffered().use { output ->
            javaClass.getResourceAsStream("empty-app-debug.apk")!!.copyTo(output)
        }

        val appInfo = ApplicationInfo()
        appInfo.sourceDir = apkFile.absolutePath

        val signature = DexBuildIdGenerator.generateBuildId(appInfo)
        assertEquals("b334dca1d76690eb9684f816e8aa83e842a6bd63", signature)
    }

    @Test
    fun invalidDexHeader() {
        val dexFile = DexBuildIdGenerator::class.java
            .getResourceAsStream("classes.dex")!!
            .readBytes()

        // Change the MagicNumber - we expect this to be considered invalid
        dexFile[1] = 0

        val signature = DexBuildIdGenerator.extractDexSignature(dexFile)
        assertNull(signature)
    }

    @Test
    fun invalidApkFile() {
        val appInfo = ApplicationInfo()
        appInfo.sourceDir = "/there/is/no/such/file/${UUID.randomUUID()}"

        val signature = DexBuildIdGenerator.generateBuildId(appInfo)
        assertNull(signature)
    }
}
