package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.ErrorStore.ERROR_REPORT_COMPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SuperCaliFragilisticExpiAlidociousBeanFactoryException: RuntimeException()

class ErrorFilenameTest {

    private lateinit var errorStore: ErrorStore
    private val config = Configuration("api-key")

    /**
     * Generates a client and ensures that its errorStore has 0 files persisted
     *
     * @throws Exception if initialisation failed
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        errorStore = ErrorStore(config, context, null)
    }

    @Test
    fun testCalculateFilenameUnhandled() {
        val err = generateError(true, Severity.ERROR, RuntimeException())
        val filename = errorStore.calculateFilenameForError(err)
        assertEquals("e-u-java.lang.RuntimeException", filename)
    }

    @Test
    fun testCalculateFilenameHandled() {
        val err = generateError(false, Severity.INFO, IllegalStateException("Whoops"))
        val filename = errorStore.calculateFilenameForError(err)
        assertEquals("i-h-java.lang.IllegalStateException", filename)
    }

    @Test
    fun testCalculateTruncatedFilename() {
        val err = generateError(false, Severity.INFO,
            SuperCaliFragilisticExpiAlidociousBeanFactoryException())
        val filename = errorStore.calculateFilenameForError(err)
        assertEquals("i-h-com.bugsnag.android.SuperCaliFragilistic", filename)
    }

    @Test
    fun testErrorFromInvalidFilename() {
        val invalids = arrayOf(
            null, "", "test.txt", "i-h.foo",
            "1504255147933_683c6b92-b325-4987-80ad-77086509ca1e.json"
        )
        invalids.forEach { assertNull(errorStore.generateErrorFromFilename(it)) }
    }

    @Test
    fun testUnhandledErrorFromFilename() {
        val filename = "1504255147933_e-u-java.lang.RuntimeException_" +
            "683c6b92-b325-4987-80ad-77086509ca1e.json"
        val err = errorStore.generateErrorFromFilename(filename)
        assertNotNull(err)
        assertTrue(err.handledState.isUnhandled)
        assertEquals(Severity.ERROR, err.severity)
        assertEquals("java.lang.RuntimeException", err.exceptionName)
    }

    @Test
    fun testHandledErrorFromFilename() {
        val filename = "1504500000000_i-h-java.lang.IllegalStateException_" +
            "683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json"
        val err = errorStore.generateErrorFromFilename(filename)
        assertNotNull(err)
        assertFalse(err.handledState.isUnhandled)
        assertEquals(Severity.INFO, err.severity)
        assertEquals("java.lang.IllegalStateException", err.exceptionName)
    }

    @Test
    fun testErrorWithoutClassFromFilename() {
        val filename = "1504500000000_i-h-_" +
            "683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json"
        val err = errorStore.generateErrorFromFilename(filename)
        assertNotNull(err)
        assertFalse(err.handledState.isUnhandled)
        assertEquals(Severity.INFO, err.severity)
        assertEquals("", err.exceptionName)
    }

    @Test
    fun testIsLaunchCrashReport() {
        val valid =
            arrayOf("1504255147933_e-u-java.lang.RuntimeException_30b7e350-dcd1-4032-969e-98d30be62bbc_startupcrash.json")
        val invalid = arrayOf(
            "",
            ".json",
            "abcdeAO.json",
            "!@£)(%)(",
            "1504255147933.txt",
            "1504255147933.json"
        )

        for (s in valid) {
            assertTrue(errorStore.isLaunchCrashReport(File(s)))
        }
        for (s in invalid) {
            assertFalse(errorStore.isLaunchCrashReport(File(s)))
        }
    }

    @Test
    fun testComparator() {
        val first = "1504255147933_e-u-java.lang.RuntimeException_" +
            "683c6b92-b325-4987-80ad-77086509ca1e.json"
        val second = "1505000000000_i-h-Exception_683c6b92-b325-4987-80ad-77086509ca1e.json"
        val startup = "1504500000000_w-h-java.lang.IllegalStateException_683c6b92-b325-" +
            "4987-80ad-77086509ca1e_startupcrash.json"

        // handle defaults
        assertEquals(0, ERROR_REPORT_COMPARATOR.compare(null, null).toLong())
        assertEquals(-1, ERROR_REPORT_COMPARATOR.compare(File(""), null).toLong())
        assertEquals(1, ERROR_REPORT_COMPARATOR.compare(null, File("")).toLong())

        // same value should always be 0
        assertEquals(0, ERROR_REPORT_COMPARATOR.compare(File(first), File(first)).toLong())
        assertEquals(0, ERROR_REPORT_COMPARATOR.compare(File(startup), File(startup)).toLong())

        // first is before second
        assertTrue(ERROR_REPORT_COMPARATOR.compare(File(first), File(second)) < 0)
        assertTrue(ERROR_REPORT_COMPARATOR.compare(File(second), File(first)) > 0)

        // startup is handled correctly
        assertTrue(ERROR_REPORT_COMPARATOR.compare(File(first), File(startup)) < 0)
        assertTrue(ERROR_REPORT_COMPARATOR.compare(File(second), File(startup)) > 0)
    }

    private fun generateError(unhandled: Boolean, severity: Severity, exc: Throwable): Error {
        val currentThread = Thread.currentThread()
        val sessionTracker = BugsnagTestUtils.generateSessionTracker()

        val handledState = when {
            unhandled -> HandledState.REASON_UNHANDLED_EXCEPTION
            else -> HandledState.REASON_HANDLED_EXCEPTION
        }
        return Error.Builder(config, exc, sessionTracker, currentThread, unhandled)
            .severityReasonType(handledState)
            .severity(severity).build()
    }
}
