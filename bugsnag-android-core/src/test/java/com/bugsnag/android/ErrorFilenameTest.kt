package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.ErrorStore.ERROR_REPORT_COMPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class ErrorFilenameTest {

    @Mock
    lateinit var context: Context

    private lateinit var errorStore: ErrorStore
    private val config = Configuration("api-key")

    /**
     * Generates a client and ensures that its errorStore has 0 files persisted
     *
     * @throws Exception if initialisation failed
     */
    @Before
    fun setUp() {
        errorStore = ErrorStore(config, null)
    }

    @Test
    fun testIsLaunchCrashReport() {
        val valid =
            arrayOf("1504255147933_30b7e350-dcd1-4032-969e-98d30be62bbc_startupcrash.json")
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
        val first = "1504255147933_683c6b92-b325-4987-80ad-77086509ca1e.json"
        val second = "1505000000000_683c6b92-b325-4987-80ad-77086509ca1e.json"
        val startup = "1504500000000_683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json"

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
}
