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
            "buildUuid" to "123",
            "codeBundleId" to "456"
        )
        val device = mapOf(
            "orientation" to "portrait",
            "jailbroken" to true,
            "locale" to "en_US",
            "osName" to "android",
            "manufacturer" to "Google",
            "cpuAbi" to arrayOf("x86"),
            "osVersion" to "8.0.0",
            "model" to "Android SDK built for x86",
            "id" to "8b105fd3-88bc-4a31-8982-b725d1162d86",
            "runtimeVersions" to mapOf(
                "osBuild" to "sdk_gphone_x86-userdebug 8.0.0 OSR1.180418.026 6741039 dev-keys",
                "androidApiLevel" to 26
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
            "projectPackages" to listOf("com.example.bar")
        )
        journalMap = minimalJournalMap + mapOf(
            "context" to "ExampleActivity",
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
    }
}
