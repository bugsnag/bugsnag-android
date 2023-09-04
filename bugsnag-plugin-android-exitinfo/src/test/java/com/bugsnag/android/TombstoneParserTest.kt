package com.bugsnag.android

import android.app.ApplicationExitInfo
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
        TombstoneParser(logger).parse(exitInfo)
    }

    @Test
    fun parseNullInputStream() {
        `when`(exitInfo.traceInputStream).thenReturn(null)
        TombstoneParser(logger).parse(exitInfo)
        verify(exitInfo, times(1)).traceInputStream
    }

    @Test
    fun parseInvalidInputStream() {
        val junkData = ByteArray(128) { it.toByte() }
        `when`(exitInfo.traceInputStream).thenReturn(junkData.inputStream())
        TombstoneParser(logger).parse(exitInfo)
        verify(exitInfo, times(1)).traceInputStream
    }
}
