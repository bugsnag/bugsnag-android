package com.bugsnag.android

import com.bugsnag.android.EventStore.Companion.EVENT_COMPARATOR
import com.bugsnag.android.FileStore.Delegate
import com.bugsnag.android.internal.BackgroundTaskService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

internal class EventFilenameTest {

    lateinit var event: Event

    private val config = BugsnagTestUtils.generateImmutableConfig()

    /**
     * Generates a client and ensures that its eventStore has 0 files persisted
     *
     * @throws Exception if initialisation failed
     */
    @Before
    fun setUp() {
        event = BugsnagTestUtils.generateEvent()
        event.apiKey = "0000111122223333aaaabbbbcccc9999"
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
            val eventInfo = EventFilenameInfo.fromFile(File(s), config)
            assertTrue(eventInfo.isLaunchCrashReport())
        }
        for (s in invalid) {
            val eventInfo = EventFilenameInfo.fromFile(File(s), config)
            assertFalse(eventInfo.isLaunchCrashReport())
        }
    }

    @Test
    fun testFindLaunchCrashReportInvalid() {
        val eventStore = EventStore(
            BugsnagTestUtils.generateImmutableConfig(),
            NoopLogger,
            Notifier(),
            BackgroundTaskService(),
            object : Delegate {
                override fun onErrorIOFailure(
                    exception: Exception?,
                    errorFile: File?,
                    context: String?
                ) {
                }
            },
            CallbackState()
        )

        // no files
        assertNull(eventStore.findLaunchCrashReport(emptyList()))

        // regular crash reports
        val jvmCrashReport = File("1504255147933_683c6b92-b325-4987-80ad-77086509ca1e.json")
        assertNull(eventStore.findLaunchCrashReport(listOf(jvmCrashReport)))
        val ndkCrashReport =
            File("1504255147933_0000111122223333aaaabbbbcccc9999_c_my-uuid-123_not-jvm.json")
        assertNull(eventStore.findLaunchCrashReport(listOf(ndkCrashReport)))
    }

    @Test
    fun testFindSingleLaunchCrashReport() {
        val eventStore = EventStore(
            BugsnagTestUtils.generateImmutableConfig(),
            NoopLogger,
            Notifier(),
            BackgroundTaskService(),
            object : Delegate {
                override fun onErrorIOFailure(
                    exception: Exception?,
                    errorFile: File?,
                    context: String?
                ) {
                }
            },
            CallbackState()
        )

        // startup crashes
        val expected = File("1504255147933_30b7e350-dcd1-4032-969e-98d30be62bbc_startupcrash.json")
        assertEquals(expected, eventStore.findLaunchCrashReport(listOf(expected)))
    }

    @Test
    fun testFindMultipleLaunchCrashReport() {
        val eventStore = EventStore(
            BugsnagTestUtils.generateImmutableConfig(),
            NoopLogger,
            Notifier(),
            BackgroundTaskService(),
            object : Delegate {
                override fun onErrorIOFailure(
                    exception: Exception?,
                    errorFile: File?,
                    context: String?
                ) {
                }
            },
            CallbackState()
        )

        // if multiple crashes exist, pick the most recent one
        val expected = File("1664219155431_042c6195-a32c-2f84-11ae-77086509ca1e_startupcrash.json")
        assertEquals(
            expected,
            eventStore.findLaunchCrashReport(
                listOf(
                    File("1504255147933_30b7e350-dcd1-4032-969e-98d30be62bbc_startupcrash.json"),
                    expected,
                    File("1404205127135_683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json")
                )
            )
        )
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

    @Test
    fun regularJvmEventName() {
        val filename = EventFilenameInfo.fromEvent(
            event,
            "my-uuid-123",
            null,
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_android_my-uuid-123_.json",
            filename
        )
    }

    /**
     * Simulates a crash 1s after launch which is considered a startup crash
     */
    @Test
    fun startupCrashJvmEventName() {
        event.app.isLaunching = true

        val filename = EventFilenameInfo.fromEvent(
            event,
            "my-uuid-123",
            null,
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_" +
                "android_my-uuid-123_startupcrash.json",
            filename
        )
    }

    /**
     * Simulates a crash 10s after launch which is not considered a startup crash
     */
    @Test
    fun nonStartupCrashCrashJvmEventName() {
        event.app.isLaunching = false
        val filename = EventFilenameInfo.fromEvent(
            event,
            "my-uuid-123",
            null,
            1504255147933,
            config
        ).encode()

        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_android_my-uuid-123_.json",
            filename
        )
    }

    @Test
    fun ndkEventName() {
        val filename = EventFilenameInfo.fromEvent(
            "{}",
            "my-uuid-123",
            "0000111122223333aaaabbbbcccc9999",
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_c_my-uuid-123_.json",
            filename
        )
    }

    @Test
    fun ndkEventNameNoApiKey() {
        val filename = EventFilenameInfo.fromEvent(
            "{}",
            "my-uuid-123",
            "",
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_5d1ec5bd39a74caa1267142706a7fb21_c_my-uuid-123_.json",
            filename
        )
    }

    @Test
    fun apiKeyFromEmptyFilename() {
        val file = File("")
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals(config.apiKey, eventInfo.apiKey)
        assertEquals("", eventInfo.uuid)
        assertEquals("", eventInfo.suffix)
        assertEquals(-1, eventInfo.timestamp)
        assertEquals(emptySet<ErrorType>(), eventInfo.errorTypes)
    }

    /**
     * Should default to config value as no api key is present
     */
    @Test
    fun apiKeyFromLegacyFilename() {
        val file = File("1504500000000_683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json")
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals(config.apiKey, eventInfo.apiKey)
        assertEquals("startupcrash", eventInfo.suffix)
    }

    @Test
    fun apiKeyFromNewFilename() {
        val file = File(
            "1504255147933_ffff111122948633aaaabbbbcccc9999" +
                "_683c6b92-b325-4987-80ad-77086509ca1e.json"
        )
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals("ffff111122948633aaaabbbbcccc9999", eventInfo.apiKey)
    }

    @Test
    fun apiKeyFromLegacyNdkFilename() {
        val file = File("1603191800142_7e1041e0-7f37-4cfb-9d29-0aa6930bbb72not-jvm.json")
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals(config.apiKey, eventInfo.apiKey)
    }

    @Test
    fun apiKeyFromNdkFilename() {
        val file = File(
            "1603191800142_5d1ec8bd39a74caa1267142706a7fb20_" +
                "7e1041e0-7f37-4cfb-9d29-0aa6930bbb72not-jvm.json"
        )
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals("5d1ec8bd39a74caa1267142706a7fb20", eventInfo.apiKey)
    }
}
