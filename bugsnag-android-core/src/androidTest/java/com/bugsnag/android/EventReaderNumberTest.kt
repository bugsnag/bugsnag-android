package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.convert
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.streamableToJson
import com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class EventReaderNumberTest {

    private var event: Event? = null

    @Before
    fun setUp() {
        val classLoader = EventReaderNumberTest::class.java.classLoader
        val input = classLoader!!.getResourceAsStream("stackframe_numbers.json")
        val fixtureFile = File.createTempFile("event", ".json")
        val output = FileOutputStream(fixtureFile)

        output.use {
            val buffer = ByteArray(1024)
            var read = input.read(buffer)

            while (read != -1) {
                it.write(buffer, 0, read)
                read = input.read(buffer)
            }
            it.flush()
        }
        val config = generateConfiguration()
        val immutableConfig = convert(config)
        event = EventReader.readEvent(immutableConfig, config, fixtureFile)
    }

    @Test
    fun readErrorExceptionStacktrace() {
        val event = streamableToJsonArray(event!!.exceptions).getJSONObject(0)
        val stacktrace = event.getJSONArray("stacktrace")
        assertEquals(3, stacktrace.length().toLong())

        val frame0 = stacktrace.getJSONObject(0)
        assertEquals(2241790.1, frame0.getDouble("lineNumber"), 0.01)

        val frame1 = stacktrace.getJSONObject(1)
        assertEquals(150000000000, frame1.getLong("lineNumber"))

        val frame2 = stacktrace.getJSONObject(2)
        assertEquals(761, frame2.getInt("lineNumber").toLong())
    }

    @Test
    fun readErrorThreadStacktrace() {
        val thread = streamableToJson(event).getJSONArray("threads").getJSONObject(0)
        assertEquals(11236722452451234, thread.getLong("id"))

        val trace = thread.getJSONArray("stacktrace")
        assertEquals(160923409125093, trace.getJSONObject(0).getLong("lineNumber"))
        assertEquals(1566.5, trace.getJSONObject(1).getDouble("lineNumber"), 0.1)
    }
}
