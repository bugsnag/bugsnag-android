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

    private val file = this.javaClass.getResourceAsStream("/tombstone_01.pb")

    @Test
    fun parseValidInputStream() {
        `when`(exitInfo.traceInputStream).thenReturn(file)
        val threads = mutableListOf<Thread>()
        TombstoneParser(logger).parse(exitInfo){
            threads.add(it)
        }

        assertEquals("30640", threads.first().id )
        assertEquals("30639", threads.last().id )
        assertEquals(4, threads.first().stacktrace.size)
        assertEquals("__rt_sigtimedwait", threads.first().stacktrace.first().method )
        assertEquals("/apex/com.android.runtime/lib64/bionic/libc.so", threads.first().stacktrace.first().file )
        assertEquals(667096L, threads.first().stacktrace.first().lineNumber )
        assertEquals(8L, threads.first().stacktrace.first().symbolAddress )
        assertEquals(0L, threads.first().stacktrace.first().loadAddress )
        assertEquals("01331f74b0bb2cb958bdc15282b8ec7b", threads.first().stacktrace.first().codeIdentifier )
    }

    @Test
    fun parseNullInputStream() {
        `when`(exitInfo.traceInputStream).thenReturn(null)
        val threads = mutableListOf<Thread>()
        TombstoneParser(logger).parse(exitInfo){
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
        TombstoneParser(logger).parse(exitInfo){
            threads.add(it)
        }

        verify(exitInfo, times(1)).traceInputStream
        assertEquals(0, threads.size)
    }
}
