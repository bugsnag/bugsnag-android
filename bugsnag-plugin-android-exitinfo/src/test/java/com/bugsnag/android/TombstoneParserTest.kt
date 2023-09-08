package com.bugsnag.android

import android.app.ApplicationExitInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class TombstoneParserTest {
    @Mock
    lateinit var logger: Logger

    @Mock
    lateinit var exitInfo: ApplicationExitInfo

    @Test
    fun parseValidInputStream() {
        val file = this.javaClass.getResourceAsStream("/tombstone_01.pb")
        `when`(exitInfo.traceInputStream).thenReturn(file)
        val threads = mutableListOf<Thread>()
        TombstoneParser(logger).parse(exitInfo) {
            threads.add(it)
        }

        assertEquals("30640", threads.first().id)
        assertEquals("30639", threads.last().id)

        val firstStackFrame = threads.first().stacktrace
        assertEquals(4, firstStackFrame.size)
        assertEquals("__rt_sigtimedwait", firstStackFrame.first().method)
        assertEquals("/apex/com.android.runtime/lib64/bionic/libc.so", firstStackFrame.first().file)
        assertEquals(667096L, firstStackFrame.first().lineNumber)
        assertEquals(8L, firstStackFrame.first().symbolAddress)
        assertEquals(0L, firstStackFrame.first().loadAddress)
        assertEquals("01331f74b0bb2cb958bdc15282b8ec7b", firstStackFrame.first().codeIdentifier)
    }

    @Test
    fun parseNullInputStream() {
        `when`(exitInfo.traceInputStream).thenReturn(null)
        val threads = mutableListOf<Thread>()
        TombstoneParser(logger).parse(exitInfo) {
            threads.add(it)
        }
        verify(exitInfo, times(1)).traceInputStream
        assertEquals(0, threads.size)
    }

    @Test
    fun parseInvalidInputStream() {
        val junkData = ByteArray(128) { it.toByte() }
        `when`(exitInfo.traceInputStream).thenReturn(junkData.inputStream())
        val threads = mutableListOf<Thread>()
        TombstoneParser(logger).parse(exitInfo) {
            threads.add(it)
        }

        verify(exitInfo, times(1)).traceInputStream
        assertEquals(0, threads.size)
    }
}
