package com.bugsnag.android.ndk

import com.bugsnag.android.internal.journal.JournaledDocument
import com.bugsnag.android.internal.journal.JsonHelper
import org.junit.Assert
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets

class NativeCrashTimeJournalTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }

        @Suppress("UNCHECKED_CAST")
        private fun loadJournalDocument(baseDocumentPath: File): Map<String, Any> {
            val document = JournaledDocument.loadDocumentContents(baseDocumentPath)
            return BugsnagTestUtils.normalized(document) as Map<String, Any>
        }

        @Suppress("UNCHECKED_CAST")
        private fun jsonToMap(json: String): Map<String, Any> {
            val document = JsonHelper.deserialize(json.toByteArray(StandardCharsets.UTF_8))
            return BugsnagTestUtils.normalized(document) as Map<String, Any>
        }

        @Suppress("UNCHECKED_CAST")
        private fun getExpected(expectedKey: String): Map<String, Any> {
            return allExpected[expectedKey] as Map<String, Any>
        }

        private val allExpected = jsonToMap(loadJson("native_crashtime_journal_test.json"))

        private const val redactMarker = "<redact-me>"

        @Suppress("UNCHECKED_CAST")
        private fun performRedactions(redactions: Map<String, Any>, map: Map<String, Any>): Map<String, Any> {
            return map.mapValues {
                val value = it.value
                when (value) {
                    is String -> if (redactMarker.equals(redactions[it.key])) redactMarker else value
                    is Map<*, *> -> performRedactions(
                        redactions[it.key] as Map<String, Any>,
                        map[it.key] as Map<String, Any>
                    )
                    else -> value
                }
            }
        }

        private fun runJournalTest(
            initialDocument: Map<String, Any>,
            expectedKey: String,
            testCode: (journalPath: String) -> Int
        ) {
            val standardType = "Bugsnag state"
            val standardVersion = 1
            val bufferSize = 100L

            val folder = TemporaryFolder()
            folder.create()
            val baseDocumentPath = folder.newFile("bugsnag-journal")
            val document = JournaledDocument(
                baseDocumentPath,
                standardType,
                standardVersion,
                bufferSize,
                bufferSize,
                initialDocument
            )
            document.close()
            verifyNativeRun(testCode(baseDocumentPath.absolutePath + ".journal.crashtime"))
            val actual = loadJournalDocument(baseDocumentPath)
            folder.delete()

            val expected = getExpected(expectedKey)
            val actualRedacted = performRedactions(expected, actual)
            Assert.assertEquals(getExpected(expectedKey), actualRedacted)
        }
    }

    // bsg_ctj_set_api_key
    private external fun nativeSetApiKey(ctjPath: String, value: String): Int

    @Test
    fun testSetApiKey() {
        runJournalTest(mapOf(), "testSetApiKey") {
            nativeSetApiKey(it, "my-key")
        }
    }

    // bsg_ctj_set_event_context
    private external fun nativeSetContext(ctjPath: String, value: String): Int

    @Test
    fun testSetContext() {
        runJournalTest(mapOf(), "testSetContext") {
            nativeSetContext(it, "my-context")
        }
    }

    // bsg_ctj_set_event_user
    private external fun nativeSetUser(ctjPath: String, id: String, email: String, name: String): Int

    @Test
    fun testSetUser() {
        runJournalTest(mapOf(), "testSetUser") {
            nativeSetUser(it, "my-id", "my-user@nowhere.com", "my-name")
        }
    }

    // bsg_ctj_set_app_binary_arch
    private external fun nativeSetBinaryArch(ctjPath: String, value: String): Int

    @Test
    fun testSetBinaryArch() {
        runJournalTest(mapOf(), "testSetBinaryArch") {
            nativeSetBinaryArch(it, "my-arch")
        }
    }

    // bsg_ctj_set_app_build_uuid
    private external fun nativeSetBuildUuid(ctjPath: String, value: String): Int

    @Test
    fun testSetBuildUuid() {
        runJournalTest(mapOf(), "testSetBuildUuid") {
            nativeSetBuildUuid(it, "123e4567-e89b-12d3-a456-426614174000")
        }
    }

    // bsg_ctj_set_app_id
    private external fun nativeSetAppId(ctjPath: String, value: String): Int

    @Test
    fun testSetAppId() {
        runJournalTest(mapOf(), "testSetAppId") {
            nativeSetAppId(it, "my-id")
        }
    }

    // bsg_ctj_set_app_release_stage
    private external fun nativeSetAppReleaseStage(ctjPath: String, value: String): Int

    @Test
    fun testSetAppReleaseStage() {
        runJournalTest(mapOf(), "testSetAppReleaseStage") {
            nativeSetAppReleaseStage(it, "my-stage")
        }
    }

    // bsg_ctj_set_app_type
    private external fun nativeSetAppType(ctjPath: String, value: String): Int

    @Test
    fun testSetAppType() {
        runJournalTest(mapOf(), "testSetAppType") {
            nativeSetAppType(it, "my-type")
        }
    }

    // bsg_ctj_set_app_version
    private external fun nativeSetAppVersion(ctjPath: String, value: String): Int

    @Test
    fun testSetAppVersion() {
        runJournalTest(mapOf(), "testSetAppVersion") {
            nativeSetAppVersion(it, "my-version")
        }
    }

    // bsg_ctj_set_app_version_code
    private external fun nativeSetAppVersionCode(ctjPath: String, value: Int): Int

    @Test
    fun testSetAppVersionCode() {
        runJournalTest(mapOf(), "testSetAppVersionCode") {
            nativeSetAppVersionCode(it, 100)
        }
    }

    // bsg_ctj_set_app_duration
    private external fun nativeSetAppDuration(ctjPath: String, value: Int): Int

    @Test
    fun testSetAppDuration() {
        runJournalTest(mapOf(), "testSetAppDuration") {
            nativeSetAppDuration(it, 100)
        }
    }

    // bsg_ctj_set_app_duration_in_foreground
    private external fun nativeSetAppDurationInForeground(ctjPath: String, value: Int): Int

    @Test
    fun testSetAppDurationInForeground() {
        runJournalTest(mapOf(), "testSetAppDurationInForeground") {
            nativeSetAppDurationInForeground(it, 100)
        }
    }

    // bsg_ctj_set_app_in_foreground
    private external fun nativeSetAppInForeground(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetAppInForeground() {
        runJournalTest(mapOf(), "testSetAppInForeground") {
            nativeSetAppInForeground(it, true)
        }
    }

    // bsg_ctj_set_app_is_launching
    private external fun nativeSetAppIsLaunching(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetAppIsLaunching() {
        runJournalTest(mapOf(), "testSetAppIsLaunching") {
            nativeSetAppIsLaunching(it, true)
        }
    }

    // bsg_ctj_set_device_jailbroken
    private external fun nativeSetJailbroken(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetJailbroken() {
        runJournalTest(mapOf(), "testSetJailbroken") {
            nativeSetJailbroken(it, false)
        }
    }

    // bsg_ctj_set_device_id
    private external fun nativeSetDeviceId(ctjPath: String, value: String): Int

    @Test
    fun testSetDeviceId() {
        runJournalTest(mapOf(), "testSetDeviceId") {
            nativeSetDeviceId(it, "my-id")
        }
    }

    // bsg_ctj_set_device_locale
    private external fun nativeSetLocale(ctjPath: String, value: String): Int

    @Test
    fun testSetLocale() {
        runJournalTest(mapOf(), "testSetLocale") {
            nativeSetLocale(it, "my-locale")
        }
    }

    // bsg_ctj_set_device_manufacturer
    private external fun nativeSetManufacturer(ctjPath: String, value: String): Int

    @Test
    fun testSetManufacturer() {
        runJournalTest(mapOf(), "testSetManufacturer") {
            nativeSetManufacturer(it, "my-manufacturer")
        }
    }

    // bsg_ctj_set_device_model
    private external fun nativeSetDeviceModel(ctjPath: String, value: String): Int

    @Test
    fun testSetDeviceModel() {
        runJournalTest(mapOf(), "testSetDeviceModel") {
            nativeSetDeviceModel(it, "my-model")
        }
    }

    // bsg_ctj_set_device_os_version
    private external fun nativeSetOsVersion(ctjPath: String, value: String): Int

    @Test
    fun testSetOsVersion() {
        runJournalTest(mapOf(), "testSetOsVersion") {
            nativeSetOsVersion(it, "my-version")
        }
    }

    // bsg_ctj_set_device_total_memory
    private external fun nativeSetTotalMemory(ctjPath: String, value: Long): Int

    @Test
    fun testSetTotalMemory() {
        runJournalTest(mapOf(), "testSetTotalMemory") {
            nativeSetTotalMemory(it, 1000000000)
        }
    }

    // bsg_ctj_set_device_orientation
    private external fun nativeSetOrientation(ctjPath: String, value: String): Int

    @Test
    fun testSetOrientation() {
        runJournalTest(mapOf(), "testSetOrientation") {
            nativeSetOrientation(it, "my-orientation")
        }
    }

    // bsg_ctj_set_device_time_seconds
    private external fun nativeSetDeviceTime(ctjPath: String, value: Long): Int

    @Test
    fun testSetDeviceTime() {
        runJournalTest(mapOf(), "testSetDeviceTime") {
            nativeSetDeviceTime(it, 1000000000)
        }
    }

    // bsg_ctj_set_device_os_name
    private external fun nativeSetOsName(ctjPath: String, value: String): Int

    @Test
    fun testSetOsName() {
        runJournalTest(mapOf(), "testSetOsName") {
            nativeSetOsName(it, "my-name")
        }
    }

    // bsg_ctj_set_error_class
    private external fun nativeSetErrorClass(ctjPath: String, value: String): Int

    @Test
    fun testSetErrorClass() {
        runJournalTest(mapOf(), "testSetErrorClass") {
            nativeSetErrorClass(it, "my-class")
        }
    }

    // bsg_ctj_set_error_message
    private external fun nativeSetErrorMessage(ctjPath: String, value: String): Int

    @Test
    fun testSetErrorMessage() {
        runJournalTest(mapOf(), "testSetErrorMessage") {
            nativeSetErrorMessage(it, "my-message")
        }
    }

    // bsg_ctj_set_error_type
    private external fun nativeSetErrorType(ctjPath: String, value: String): Int

    @Test
    fun testSetErrorType() {
        runJournalTest(mapOf(), "testSetErrorType") {
            nativeSetErrorType(it, "my-type")
        }
    }

    // bsg_ctj_set_event_severity
    private external fun nativeSetEventSeverity(ctjPath: String, value: Int): Int

    @Test
    fun testSetEventSeverity() {
        runJournalTest(mapOf(), "testSetEventSeverity") {
            nativeSetEventSeverity(it, 1)
        }
    }

    // bsg_ctj_set_event_unhandled
    private external fun nativeSetEventUnhandled(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetEventUnhandled() {
        runJournalTest(mapOf(), "testSetEventUnhandled") {
            nativeSetEventUnhandled(it, true)
        }
    }

    // bsg_ctj_set_event_grouping_hash
    private external fun nativeSetGroupingHash(ctjPath: String, value: String): Int

    @Test
    fun testSetGroupingHash() {
        runJournalTest(mapOf(), "testSetGroupingHash") {
            nativeSetGroupingHash(it, "my-hash")
        }
    }

    // bsg_ctj_set_metadata_double
    private external fun nativeAddMetadataDouble(ctjPath: String, section: String, name: String, value: Double): Int

    @Test
    fun testAddMetadataDouble() {
        runJournalTest(mapOf(), "testAddMetadataDouble") {
            nativeAddMetadataDouble(it, "my-section", "my-name", 1.5)
        }
        runJournalTest(mapOf(), "testAddMetadataDigitsDouble") {
            nativeAddMetadataDouble(it, "-1", "0\\.+", 1.5)
        }
    }

    // bsg_ctj_set_metadata_bool
    private external fun nativeAddMetadataBool(ctjPath: String, section: String, name: String, value: Boolean): Int

    @Test
    fun testAddMetadataBool() {
        runJournalTest(mapOf(), "testAddMetadataBool") {
            nativeAddMetadataBool(it, "my-section", "my-name", true)
        }
        runJournalTest(mapOf(), "testAddMetadataDigitsBool") {
            nativeAddMetadataBool(it, "1000", "3456\\.+", false)
        }
    }

    // bsg_ctj_set_metadata_string
    private external fun nativeAddMetadataString(ctjPath: String, section: String, name: String, value: String): Int

    @Test
    fun testAddMetadataString() {
        runJournalTest(mapOf(), "testAddMetadataString") {
            nativeAddMetadataString(it, "my-section", "my-name", "my-value")
        }
        runJournalTest(mapOf(), "testAddMetadataDigitsString") {
            nativeAddMetadataString(it, "9", "9\\.+", "my-value")
        }
    }

    // bsg_ctj_clear_metadata
    private external fun nativeClearMetadata(ctjPath: String, section: String, name: String): Int

    @Test
    fun testClearMetadata() {
        runJournalTest(getExpected("testClearMetadataInitial"), "testClearMetadataFinal") {
            nativeClearMetadata(it, "my-section", "my-name")
        }
    }

    // bsg_ctj_clear_metadata_section
    private external fun nativeClearMetadataSection(ctjPath: String, section: String): Int

    @Test
    fun testClearMetadataSection() {
        runJournalTest(getExpected("testClearMetadataSectionInitial"), "testClearMetadataSectionFinal") {
            nativeClearMetadataSection(it, "my-section")
        }
    }

    // bsg_ctj_store_event
// TODO PLAT-7589
//    private external fun nativeStoreEvent(ctjPath: String): Int
//
//    @Test
//    fun testStoreEvent() {
//        runJournalTest(mapOf(), "testStoreEvent") {
//            nativeStoreEvent(it)
//        }
//    }
}
