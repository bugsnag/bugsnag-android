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
}
