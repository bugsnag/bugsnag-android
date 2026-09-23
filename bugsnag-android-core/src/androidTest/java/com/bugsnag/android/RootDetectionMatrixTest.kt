package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates that the old `which su` approach and the new `File.exists()` approach stay aligned
 * for the root-detection scenarios we care about.
 *
 * These tests intentionally compare the changed Java-level su detection logic against a local
 * simulation of the old PATH-based implementation, then exercise the unchanged build-tag/build-
 * props checks that still determine the overall root result.
 */
internal class RootDetectionMatrixTest {

    private val safeBuildProps = "# clean build props\nro.secure=[1]\nro.debuggable=[0]"

    @Test
    fun stockUnrootedDevice_matchesOldAndNew() = assertScenario(
        name = "stock unrooted device",
        buildTags = "release-keys",
        pathEntries = emptyList(),
        rootPaths = emptyList(),
        expectedRooted = false
    )

    @Test
    fun magiskRootedDefaultHide_matchesOldAndNew() = assertScenario(
        name = "magisk rooted (default hide)",
        buildTags = "release-keys",
        pathEntries = listOf(createExecutableSuInTempDir()),
        rootPaths = listOf(tempRootBinaryPath("magisk")),
        expectedRooted = true
    )

    @Test
    fun magiskRootedDenyListActive_acceptsFalse() = assertScenario(
        name = "magisk rooted (deny list active)",
        buildTags = "release-keys",
        pathEntries = emptyList(),
        rootPaths = emptyList(),
        expectedRooted = false
    )

    @Test
    fun superSuRooted_matchesOldAndNew() = assertScenario(
        name = "SuperSU rooted",
        buildTags = "release-keys",
        pathEntries = listOf(createExecutableSuInTempDir()),
        rootPaths = listOf(tempRootBinaryPath("supersu")),
        expectedRooted = true
    )

    @Test
    fun emulatorDefault_matchesOldAndNew() = assertScenario(
        name = "emulator default",
        buildTags = "test-keys",
        pathEntries = emptyList(),
        rootPaths = emptyList(),
        expectedRooted = true
    )

    @Test
    fun emulatorGooglePlayImage_matchesOldAndNew() = assertScenario(
        name = "emulator Google Play image",
        buildTags = "release-keys",
        pathEntries = emptyList(),
        rootPaths = emptyList(),
        expectedRooted = false
    )

    @Test
    fun pathOnlySuOutsideAllowList_isDocumentedEdgeCase() {
        val rogueSu = createExecutableSuInTempDir(dirName = "rogue-su")
        assertTrue(oldWhichSu(listOf(rogueSu)))
        assertFalse(
            RootDetector(
                rootBinaryLocations = listOf("/non/standard/path/su"),
                buildProps = tempBuildProps(safeBuildProps),
                deviceBuildInfo = DeviceBuildInfo(null, null, null, null, null, null, "release-keys", null, null),
                logger = NoopLogger
            ).checkRootBinaries()
        )
    }

    private fun assertScenario(
        name: String,
        buildTags: String,
        pathEntries: List<File>,
        rootPaths: List<String>,
        expectedRooted: Boolean,
    ) {
        val oldSuCheck = oldWhichSu(pathEntries)
        val detector = RootDetector(
            deviceBuildInfo = DeviceBuildInfo(null, null, null, null, null, null, buildTags, null, null),
            rootBinaryLocations = rootPaths,
            buildProps = tempBuildProps(safeBuildProps),
            logger = NoopLogger
        )

        val newSuCheck = detector.checkRootBinaries()
        val overallResult = detector.checkBuildTags() || detector.checkBuildProps() || newSuCheck
        val oldOverallResult = oldSuCheck || detector.checkBuildTags() || detector.checkBuildProps()

        assertEquals("$name: PATH-based and exists-based checks should match", oldSuCheck, newSuCheck)
        assertEquals("$name: overall root result should match the matrix expectation", expectedRooted, overallResult)
        assertEquals("$name: old and new overall root results should stay aligned", oldOverallResult, overallResult)
    }

    private fun oldWhichSu(pathEntries: List<File>): Boolean {
        return pathEntries.any { dir -> File(dir, "su").exists() }
    }

    private fun createExecutableSuInTempDir(dirName: String = "su-bin"): File {
        val dir = createTempDirectory(dirName)
        val su = File(dir, "su")
        su.writeText("#!/bin/sh\necho su\n")
        su.setExecutable(true, false)
        su.deleteOnExit()
        dir.deleteOnExit()
        return dir
    }

    private fun tempRootBinaryPath(dirName: String): String {
        val dir = createTempDirectory(dirName)
        val file = File(dir, "su")
        file.writeText("su")
        file.deleteOnExit()
        dir.deleteOnExit()
        return file.absolutePath
    }

    private fun tempBuildProps(contents: String): File {
        val file = File.createTempFile("build-props", ".prop")
        file.writeText(contents)
        file.deleteOnExit()
        return file
    }

    private fun createTempDirectory(prefix: String): File {
        val baseDir = File(System.getProperty("java.io.tmpdir") ?: "/data/local/tmp")
        var attempt = 0
        while (true) {
            val dir = File(baseDir, "$prefix-${System.nanoTime()}-$attempt")
            if (dir.mkdir()) {
                return dir
            }
            attempt++
        }
    }
}
