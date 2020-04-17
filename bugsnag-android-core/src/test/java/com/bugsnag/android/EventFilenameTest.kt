package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.EventStore.EVENT_COMPARATOR
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
class EventFilenameTest {

    @Mock
    lateinit var context: Context

    private lateinit var eventStore: EventStore

    private val config = BugsnagTestUtils.generateImmutableConfig()

    /**
     * Generates a client and ensures that its eventStore has 0 files persisted
     *
     * @throws Exception if initialisation failed
     */
    @Before
    fun setUp() {
        eventStore = EventStore(
            config,
            context,
            NoopLogger,
            Notifier(),
            null
        )
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
            assertTrue(eventStore.isLaunchCrashReport(File(s)))
        }
        for (s in invalid) {
            assertFalse(eventStore.isLaunchCrashReport(File(s)))
        }
    }

    @Test
    fun testComparator() {
        val first = "1504255147933_683c6b92-b325-4987-80ad-77086509ca1e.json"
        val second = "1505000000000_683c6b92-b325-4987-80ad-77086509ca1e.json"
        val startup = "1504500000000_683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json"

        // handle defaults
        assertEquals(0, EVENT_COMPARATOR.compare(null, null).toLong())
        assertEquals(-1, EVENT_COMPARATOR.compare(File(""), null).toLong())
        assertEquals(1, EVENT_COMPARATOR.compare(null, File("")).toLong())

        // same value should always be 0
        assertEquals(0, EVENT_COMPARATOR.compare(File(first), File(first)).toLong())
        assertEquals(0, EVENT_COMPARATOR.compare(File(startup), File(startup)).toLong())

        // first is before second
        assertTrue(EVENT_COMPARATOR.compare(File(first), File(second)) < 0)
        assertTrue(EVENT_COMPARATOR.compare(File(second), File(first)) > 0)

        // startup is handled correctly
        assertTrue(EVENT_COMPARATOR.compare(File(first), File(startup)) < 0)
        assertTrue(EVENT_COMPARATOR.compare(File(second), File(startup)) > 0)
    }
}
