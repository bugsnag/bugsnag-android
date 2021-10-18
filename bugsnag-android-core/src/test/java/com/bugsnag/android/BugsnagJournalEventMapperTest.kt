package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BugsnagJournalEventMapperTest {

    private lateinit var journalMap: Map<String, Any?>
    private lateinit var minimalJournalMap: Map<String, Any?>
    private lateinit var fooSection: Map<String, Any?>
    private lateinit var appSection: Map<String, Any?>

    @Before
    fun setUp() {
        fooSection = mapOf(
            "map" to mapOf(
                "a" to "z"
            ),
            "array" to listOf(
                "1", "2", "3"
            )
        )
        appSection = mapOf(
            "processName" to "com.example.bugsnag.android",
            "memoryLimit" to 40265318449,
            "lowMemory" to false
        )
        val breadcrumbs = listOf(
            mapOf(
                "metaData" to emptyMap<String, Any?>(),
                "name" to "Bugsnag loaded",
                "type" to "state",
                "timestamp" to "2021-09-28T10:31:09.092Z"
            ),
            mapOf(
                "metaData" to mapOf(
                    "hasConnection" to true,
                    "networkState" to "wifi"
                ),
                "name" to "Connectivity changed",
                "type" to "request",
                "timestamp" to "2021-09-28T10:31:10.856Z"
            )
        )
        val app = mapOf(
            "duration" to 28,
            "durationInForeground" to 5,
            "inForeground" to true,
            "isLaunching" to false,
            "releaseStage" to "development",
            "binaryArch" to "x86",
            "id" to "com.example.foo",
            "type" to "android",
            "version" to "1.0",
            "versionCode" to 1,
            "buildUUID" to "123",
            "codeBundleId" to "456"
        )
        val device = mapOf(
            "orientation" to "portrait",
            "jailbroken" to true,
            "locale" to "en_US",
            "osName" to "android",
            "manufacturer" to "Google",
            "cpuAbi" to listOf("x86"),
            "osVersion" to "8.0.0",
            "model" to "Android SDK built for x86",
            "id" to "8b105fd3-88bc-4a31-8982-b725d1162d86",
            "time" to "2011-05-29T05:20:51Z",
            "runtimeVersions" to mapOf(
                "osBuild" to "sdk_gphone_x86-userdebug 8.0.0 OSR1.180418.026 6741039 dev-keys",
                "androidApiLevel" to 26
            )
        )

        val stacktrace = listOf(
            mapOf(
                "frameAddress" to "0x82545948",
                "symbolAddress" to "0x82545000",
                "loadAddress" to "0x90000000",
                "lineNumber" to BigDecimal.valueOf(273),
                "isPC" to true,
                "file" to "/data/app/com.example.bugsnag.android-EPFji4GE4IHgwGM2GoOXvQ==/lib/x86/libentrypoint.so",
                "method" to "crash_write_read_only"
            ),
            mapOf(
                "frameAddress" to "0x900040923",
                "symbolAddress" to "0x900010000",
                "loadAddress" to "0x00001000",
                "lineNumber" to BigDecimal.valueOf(509),
                "file" to "/data/app/com.example.bugsnag.android-EPFji4GE4IHgwGM2GoOXvQ==/oat/x86/base.odex",
                "method" to "Java_com_example_bugsnag_android_BaseCrashyActivity_crashFromCXX"
            )
        )
        val exception = mapOf(
            "errorClass" to "SIGSEGV",
            "message" to "Segmentation violation (invalid memory reference)",
            "type" to "c",
            "stacktrace" to stacktrace
        )
        val severityReason = mapOf(
            "type" to "signal",
            "unhandledOverridden" to false,
            "attributes" to mapOf(
                "signalType" to "SIGSEGV"
            )
        )

        // threads
        val threads = listOf(
            mapOf(
                "id" to BigDecimal.valueOf(29695),
                "name" to "ConnectivityThr",
                "state" to "running",
                "type" to "c"

            ),
            mapOf(
                "id" to BigDecimal.valueOf(29698),
                "name" to "Binder:29227_3",
                "state" to "sleeping",
                "type" to "c"
            )
        )
        minimalJournalMap = mapOf(
            "apiKey" to "my-api-key",
            "user" to mapOf(
                "id" to "123",
                "name" to "Boog Snoog",
                "email" to "hello@example.com"
            ),
            "metaData" to mapOf(
                "app" to appSection,
                "foo" to fooSection
            ),
            "breadcrumbs" to breadcrumbs,
            "app" to app,
            "device" to device,
            "projectPackages" to listOf("com.example.bar"),
            "exceptions" to listOf(exception),
            "unhandled" to true,
            "severity" to "error",
            "severityReason" to severityReason,
            "threads" to threads
        )
        journalMap = minimalJournalMap + mapOf(
            "context" to "ExampleActivity",
            "groupingHash" to "hash-123",
            "session" to mapOf(
                "startedAt" to "2021-09-28T10:31:09.620Z",
                "id" to "b4a03b1b-e0dc-4bed-81e8-cb9e9f2ed825",
                "events" to mapOf(
                    "unhandled" to 1,
                    "handled" to 2
                )
            )
        )
    }

    @Test
    fun emptyMapNullEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        assertNull(mapper.convertToEvent(emptyMap()))
    }

    /**
     * Validates that an event that omits non-mandatory fields such as
     * context/session is deserialized.
     */
    @Test
    fun populatedMinimalEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        val event = mapper.convertToEvent(minimalJournalMap)
        checkNotNull(event)
        validateMandatoryEventFields(event)

        // context/session
        assertNull(event.context)
        assertNull(event.session)
        assertNull(event.groupingHash)
    }

    /**
     * Validates that an event containing non-mandatory fields such as
     * context/session is deserialized.
     */
    @Test
    fun populatedEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        val event = mapper.convertToEvent(journalMap)
        checkNotNull(event)
        validateMandatoryEventFields(event)

        // context
        assertEquals("ExampleActivity", event.context)
        assertEquals("hash-123", event.groupingHash)

        // session
        val session = checkNotNull(event.session)
        assertEquals("b4a03b1b-e0dc-4bed-81e8-cb9e9f2ed825", session.id)
        assertEquals(DateUtils.fromIso8601("2021-09-28T10:31:09.620Z"), session.startedAt)
        assertEquals(1, session.unhandledCount)
        assertEquals(2, session.handledCount)
    }

    private fun validateMandatoryEventFields(event: EventInternal) {
        // user
        val user = event.getUser()
        assertNotNull(user)
        assertEquals("123", user.id)
        assertEquals("Boog Snoog", user.name)
        assertEquals("hello@example.com", user.email)

        // metadata
        assertEquals(appSection, event.getMetadata("app"))
        assertEquals(fooSection, event.getMetadata("foo"))

        // breadcrumbs
        assertEquals(2, event.breadcrumbs.size)

        with(event.breadcrumbs[0]) {
            assertEquals("Bugsnag loaded", message)
            assertEquals(BreadcrumbType.STATE, type)
            assertEquals(DateUtils.fromIso8601("2021-09-28T10:31:09.092Z"), timestamp)
            assertEquals(emptyMap<String, Any?>(), metadata)
        }

        with(event.breadcrumbs[1]) {
            assertEquals("Connectivity changed", message)
            assertEquals(BreadcrumbType.REQUEST, type)
            assertEquals(DateUtils.fromIso8601("2021-09-28T10:31:10.856Z"), timestamp)
            val expectedMetadata = mapOf(
                "hasConnection" to true,
                "networkState" to "wifi"
            )
            assertEquals(expectedMetadata, metadata)
        }

        // app
        val app = checkNotNull(event.app)
        assertEquals("x86", app.binaryArch)
        assertEquals("com.example.foo", app.id)
        assertEquals("development", app.releaseStage)
        assertEquals("1.0", app.version)
        assertEquals("456", app.codeBundleId)
        assertEquals("123", app.buildUuid)
        assertEquals("android", app.type)
        assertEquals(1, app.versionCode)
        assertEquals(28, app.duration)
        assertEquals(5, app.durationInForeground)
        assertTrue(app.inForeground as Boolean)
        assertFalse(app.isLaunching as Boolean)

        // device
        val device = checkNotNull(event.device)
        assertEquals("portrait", device.orientation)
        assertTrue(device.jailbroken as Boolean)
        assertEquals(
            "sdk_gphone_x86-userdebug 8.0.0 OSR1.180418.026 6741039 dev-keys",
            device.runtimeVersions?.get("osBuild") as String
        )
        assertEquals(26, device.runtimeVersions?.get("androidApiLevel") as Int)
        assertEquals("en_US", device.locale)
        assertEquals("android", device.osName)
        assertEquals("Google", device.manufacturer)
        assertArrayEquals(arrayOf("x86"), device.cpuAbi)
        assertEquals("8.0.0", device.osVersion)
        assertEquals("Android SDK built for x86", device.model)
        assertEquals("8b105fd3-88bc-4a31-8982-b725d1162d86", device.id)

        // projectPackages
        assertEquals(listOf("com.example.bar"), event.projectPackages)

        // exception
        val err = event.errors.single()
        assertEquals("SIGSEGV", err.errorClass)
        assertEquals("Segmentation violation (invalid memory reference)", err.errorMessage)
        assertEquals(ErrorType.C, err.type)

        val trace = err.stacktrace
        assertEquals(2, trace.size)

        // first stackframe
        val firstFrame = trace[0].toJournalSection()
        assertEquals(273L, firstFrame["lineNumber"])
        assertEquals(0x82545948, firstFrame["frameAddress"])
        assertEquals(0x82545000, firstFrame["symbolAddress"])
        assertEquals(0x90000000, firstFrame["loadAddress"])
        assertTrue(firstFrame["isPC"] as Boolean)
        assertEquals(
            "/data/app/com.example.bugsnag.android-" +
                "EPFji4GE4IHgwGM2GoOXvQ==/lib/x86/libentrypoint.so",
            firstFrame["file"]
        )
        assertEquals("crash_write_read_only", firstFrame["method"])

        // second stackframe
        val secondFrame = trace[1].toJournalSection()
        assertEquals(509L, secondFrame["lineNumber"])
        assertEquals(0x900040923, secondFrame["frameAddress"])
        assertEquals(0x900010000, secondFrame["symbolAddress"])
        assertEquals(0x00001000L, secondFrame["loadAddress"])
        assertNull(secondFrame["isPC"])
        assertEquals(
            "/data/app/com.example.bugsnag.android-" +
                "EPFji4GE4IHgwGM2GoOXvQ==/oat/x86/base.odex",
            secondFrame["file"]
        )
        assertEquals(
            "Java_com_example_bugsnag_android_BaseCrashyActivity_crashFromCXX",
            secondFrame["method"]
        )

        // severity/handledness
        assertEquals(Severity.ERROR, event.severity)

        with(event.severityReason) {
            assertTrue(unhandled)
            assertFalse(unhandledOverridden)
            assertTrue(isOriginalUnhandled)
            assertEquals(Severity.ERROR, currentSeverity)
            assertEquals("signal", severityReasonType)
            assertEquals("SIGSEGV", attributeValue)
        }

        // threads
        assertEquals(2, event.threads.size)
        with(event.threads[0]) {
            assertEquals(29695L, id)
            assertEquals("ConnectivityThr", name)
            assertEquals("running", impl.state)
            assertEquals(ThreadType.C, type)
        }
        with(event.threads[1]) {
            assertEquals(29698, id)
            assertEquals("Binder:29227_3", name)
            assertEquals("sleeping", impl.state)
            assertEquals(ThreadType.C, type)
        }
    }
}
