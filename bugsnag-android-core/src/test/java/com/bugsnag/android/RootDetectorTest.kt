package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files

@RunWith(MockitoJUnitRunner::class)
class RootDetectorTest {

    private val rootDetector = RootDetector()

    @Mock
    lateinit var processBuilder: ProcessBuilder

    @Mock
    lateinit var process: Process

    @Before
    fun setUp() {
        `when`(processBuilder.start()).thenReturn(process)
    }

    /**
     * IOExceptions thrown when starting the process are handled appropriately
     */
    @Test
    fun checkSuProcessStartException() {
        `when`(processBuilder.start()).thenThrow(IOException())
        assertFalse(rootDetector.checkSuExists(processBuilder))
    }

    /**
     * The method returns false if 'which su' returns an empty string
     */
    @Test
    fun checkSuNotFound() {
        val emptyStream = ByteArrayInputStream("".toByteArray())
        `when`(process.inputStream).thenReturn(emptyStream)
        assertFalse(rootDetector.checkSuExists(processBuilder))
        verify(processBuilder, times(1)).command(listOf("which", "su"))
        verify(process, times(1)).destroy()
    }

    /**
     * The method returns true if 'which su' returns a non-empty string
     */
    @Test
    fun checkSuFound() {
        val resultStream = ByteArrayInputStream("/system/bin/su".toByteArray())
        `when`(process.inputStream).thenReturn(resultStream)
        assertTrue(rootDetector.checkSuExists(processBuilder))
    }

    /**
     * Verifies that 'test-keys' triggers root detection.
     */
    @Test
    fun checkBuildTagsRooted() {
        val info = DeviceBuildInfo(null, null, null, null, null, null, "test-keys", null, null)
        assertTrue(RootDetector(info).checkBuildTags())
    }

    /**
     * Verifies that 'release-keys' does not trigger root detection
     */
    @Test
    fun checkBuildTagsNotRooted() {
        val info = DeviceBuildInfo(null, null, null, null, null, null, "release-keys", null, null)
        assertFalse(RootDetector(info).checkBuildTags())
    }

    /**
     * Verifies that a non-existent file does not trigger root detection
     */
    @Test
    fun checkRootBinaryRooted() {
        assertFalse(RootDetector(rootBinaryLocations = listOf("/foo")).checkRootBinaries())
    }

    /**
     * Verifies that an existing root binary triggers root detection
     */
    @Test
    fun checkRootBinaryNotRooted() {
        val tmpFile = Files.createTempFile("evilrootbinary", ".apk")
        val path = tmpFile.toFile().absolutePath
        assertTrue(RootDetector(rootBinaryLocations = listOf(path)).checkRootBinaries())
    }
}
