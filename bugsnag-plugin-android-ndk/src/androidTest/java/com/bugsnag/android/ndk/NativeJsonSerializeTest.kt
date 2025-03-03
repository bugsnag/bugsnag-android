package com.bugsnag.android.ndk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class NativeJsonSerializeTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    private val path = File(System.getProperty("java.io.tmpdir"), this::class.simpleName!!)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    @Before
    fun setupTmpdir() {
        path.mkdirs()
    }

    @After
    fun deleteTmpdir() {
        path.deleteRecursively()
    }

    external fun run(outputDir: String, timestamp: Long): Int

    @Test
    fun testPassesNativeSuiteEpoch() {
        verifyNativeRun(run(path.absolutePath, 7609))
        val jsonFile = path.listFiles()!!.maxByOrNull { it.lastModified() }!!
        val expected = loadJson("event_serialization.json")
        assertEquals(expected, jsonFile.readText())
    }

    @Test
    fun testRegression2024() {
        val timestamp = GregorianCalendar(2024, 11, 30, 16, 0, 0).timeInMillis
        val datestamp = dateFormat.format(Date(timestamp))

        verifyNativeRun(run(path.absolutePath, timestamp / 1000L))
        val jsonFile = path.listFiles()!!.maxByOrNull { it.lastModified() }!!
        val expected = loadJson("event_serialization.json")
            .replace("\"1970-01-01T02:06:49Z\"", "\"${datestamp}\"")
        assertEquals(expected, jsonFile.readText())
    }

    @Test
    fun testPassesNativeSuite2024() {
        val timestamp = GregorianCalendar(2024, 0, 1).timeInMillis
        val datestamp = dateFormat.format(Date(timestamp))

        verifyNativeRun(run(path.absolutePath, timestamp / 1000L))
        val jsonFile = path.listFiles()!!.maxByOrNull { it.lastModified() }!!
        val expected = loadJson("event_serialization.json")
            .replace("\"1970-01-01T02:06:49Z\"", "\"${datestamp}\"")
        assertEquals(expected, jsonFile.readText())
    }

    @Test
    fun testPassesNativeSuite2025() {
        val timestamp = GregorianCalendar(2025, 1, 1).timeInMillis
        val datestamp = dateFormat.format(Date(timestamp))

        verifyNativeRun(run(path.absolutePath, timestamp / 1000L))
        val jsonFile = path.listFiles()!!.maxByOrNull { it.lastModified() }!!
        val expected = loadJson("event_serialization.json")
            .replace("\"1970-01-01T02:06:49Z\"", "\"${datestamp}\"")
        assertEquals(expected, jsonFile.readText())
    }

    @Test
    fun testPassesNativeSuiteToday() {
        val now = System.currentTimeMillis()
        val datestamp = dateFormat.format(Date(now))

        verifyNativeRun(run(path.absolutePath, now / 1000L))
        val jsonFile = path.listFiles()!!.maxByOrNull { it.lastModified() }!!
        val expected = loadJson("event_serialization.json")
            .replace("\"1970-01-01T02:06:49Z\"", "\"${datestamp}\"")
        assertEquals(expected, jsonFile.readText())
    }

    @Test
    @Ignore("useful when working on the date formatting code")
    fun testDecadesOfDates() {
        val calendar = Calendar.getInstance().apply { add(Calendar.YEAR, -10) }
        val end = Calendar.getInstance().apply { add(Calendar.YEAR, 10) }

        while (calendar < end) {
            val instant = calendar.timeInMillis
            val datestamp = dateFormat.format(Date(instant))

            verifyNativeRun(run(path.absolutePath, instant / 1000L))
            val jsonFile = path.listFiles()!!.maxByOrNull { it.lastModified() }!!
            val expected = loadJson("event_serialization.json")
                .replace("\"1970-01-01T02:06:49Z\"", "\"${datestamp}\"")
            assertEquals(expected, jsonFile.readText())

            // move the date along 6 hours at a time
            calendar.add(Calendar.HOUR, 6)

            jsonFile.delete()
        }
    }
}
