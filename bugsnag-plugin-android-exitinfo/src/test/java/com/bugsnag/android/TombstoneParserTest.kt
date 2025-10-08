package com.bugsnag.android

import com.google.protobuf.InvalidProtocolBufferException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class TombstoneParserTest {
    @Mock
    lateinit var logger: Logger

    @Test
    fun parseValidInputStream() {
        val file = this.javaClass.getResourceAsStream("/tombstone_01.pb")!!

        val threads = mutableListOf<Thread>()
        val fileDescriptors = ArrayList<Map<String, Any>>()
        val logList = mutableListOf<String>()
        var abortMessage: String? = null

        TombstoneParser(logger, listOpenFds = true, includeLogcat = true).parse(
            file,
            threadConsumer = { thread ->
                threads.add(thread)
            },
            fileDescriptorConsumer = { fd, path, owner ->
                fileDescriptors.add(
                    mapOf(
                        "fd" to fd,
                        "path" to path,
                        "owner" to owner,
                    )
                )
            },
            logcatConsumer = { logMessage ->
                logList.add(0, logMessage)
            },
            { abortMessage = it }
        )

        assertEquals("30640", threads.first().id)
        assertEquals("30639", threads.last().id)

        assertEquals(
            "assertion 'init_status == std::future_status::ready' failed - " +
                "Can't start stack, last instance: starting Controller",
            abortMessage
        )

        val firstStackFrame = threads.first().stacktrace
        assertEquals(4, firstStackFrame.size)
        assertEquals("__rt_sigtimedwait", firstStackFrame.first().method)
        assertEquals("/apex/com.android.runtime/lib64/bionic/libc.so", firstStackFrame.first().file)
        assertEquals(667096L, firstStackFrame.first().lineNumber)
        assertEquals(8L, firstStackFrame.first().symbolAddress)
        assertEquals(0L, firstStackFrame.first().loadAddress)
        assertEquals("01331f74b0bb2cb958bdc15282b8ec7b", firstStackFrame.first().codeIdentifier)

        assertEquals(145, fileDescriptors.size)
        val firstFileDescriptor = fileDescriptors.first()
        assertEquals(0, firstFileDescriptor["fd"])
        assertEquals("/dev/null", firstFileDescriptor["path"])
        assertEquals("", firstFileDescriptor["owner"])

        assertEquals(1, logList.size)
    }

    @Test(expected = InvalidProtocolBufferException::class)
    fun parseInvalidInputStream() {
        val junkData = ByteArray(128) { it.toByte() }

        TombstoneParser(logger, listOpenFds = true, includeLogcat = true).parse(
            tombstoneInputStream = junkData.inputStream(),
            threadConsumer = { _ -> fail("threads should not be parsed") },
            fileDescriptorConsumer = { _, _, _ -> fail("fds should not be parsed") },
            logcatConsumer = { _ -> fail("logcat should not be parsed") },
            { _ -> fail("abortMessage should not be parsed") }
        )
    }
}
