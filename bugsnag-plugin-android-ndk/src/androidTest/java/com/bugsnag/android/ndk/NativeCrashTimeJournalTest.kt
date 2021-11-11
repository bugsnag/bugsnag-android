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
        private fun getExpected(expectedKey: String): MutableMap<String, Any> {
            return allExpected[expectedKey] as MutableMap<String, Any>
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
            initialDocument: MutableMap<String, Any>,
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
        runJournalTest(mutableMapOf(), "testSetApiKey") {
            nativeSetApiKey(it, "my-key")
        }
    }

    // bsg_ctj_set_event_context
    private external fun nativeSetContext(ctjPath: String, value: String): Int

    @Test
    fun testSetContext() {
        runJournalTest(mutableMapOf(), "testSetContext") {
            nativeSetContext(it, "my-context")
        }
    }

    // bsg_ctj_set_event_user
    private external fun nativeSetUser(ctjPath: String, id: String, email: String, name: String): Int

    @Test
    fun testSetUser() {
        runJournalTest(mutableMapOf(), "testSetUser") {
            nativeSetUser(it, "my-id", "my-user@nowhere.com", "my-name")
        }
    }

    // bsg_ctj_set_app_binary_arch
    private external fun nativeSetBinaryArch(ctjPath: String, value: String): Int

    @Test
    fun testSetBinaryArch() {
        runJournalTest(mutableMapOf(), "testSetBinaryArch") {
            nativeSetBinaryArch(it, "my-arch")
        }
    }

    // bsg_ctj_set_app_build_uuid
    private external fun nativeSetBuildUuid(ctjPath: String, value: String): Int

    @Test
    fun testSetBuildUuid() {
        runJournalTest(mutableMapOf(), "testSetBuildUuid") {
            nativeSetBuildUuid(it, "123e4567-e89b-12d3-a456-426614174000")
        }
    }

    // bsg_ctj_set_app_id
    private external fun nativeSetAppId(ctjPath: String, value: String): Int

    @Test
    fun testSetAppId() {
        runJournalTest(mutableMapOf(), "testSetAppId") {
            nativeSetAppId(it, "my-id")
        }
    }

    // bsg_ctj_set_app_release_stage
    private external fun nativeSetAppReleaseStage(ctjPath: String, value: String): Int

    @Test
    fun testSetAppReleaseStage() {
        runJournalTest(mutableMapOf(), "testSetAppReleaseStage") {
            nativeSetAppReleaseStage(it, "my-stage")
        }
    }

    // bsg_ctj_set_app_type
    private external fun nativeSetAppType(ctjPath: String, value: String): Int

    @Test
    fun testSetAppType() {
        runJournalTest(mutableMapOf(), "testSetAppType") {
            nativeSetAppType(it, "my-type")
        }
    }

    // bsg_ctj_set_app_version
    private external fun nativeSetAppVersion(ctjPath: String, value: String): Int

    @Test
    fun testSetAppVersion() {
        runJournalTest(mutableMapOf(), "testSetAppVersion") {
            nativeSetAppVersion(it, "my-version")
        }
    }

    // bsg_ctj_set_app_version_code
    private external fun nativeSetAppVersionCode(ctjPath: String, value: Int): Int

    @Test
    fun testSetAppVersionCode() {
        runJournalTest(mutableMapOf(), "testSetAppVersionCode") {
            nativeSetAppVersionCode(it, 100)
        }
    }

    // bsg_ctj_set_app_duration
    private external fun nativeSetAppDuration(ctjPath: String, value: Int): Int

    @Test
    fun testSetAppDuration() {
        runJournalTest(mutableMapOf(), "testSetAppDuration") {
            nativeSetAppDuration(it, 100)
        }
    }

    // bsg_ctj_set_app_duration_in_foreground
    private external fun nativeSetAppDurationInForeground(ctjPath: String, value: Int): Int

    @Test
    fun testSetAppDurationInForeground() {
        runJournalTest(mutableMapOf(), "testSetAppDurationInForeground") {
            nativeSetAppDurationInForeground(it, 100)
        }
    }

    // bsg_ctj_set_app_in_foreground
    private external fun nativeSetAppInForeground(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetAppInForeground() {
        runJournalTest(mutableMapOf(), "testSetAppInForeground") {
            nativeSetAppInForeground(it, true)
        }
    }

    // bsg_ctj_set_app_is_launching
    private external fun nativeSetAppIsLaunching(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetAppIsLaunching() {
        runJournalTest(mutableMapOf(), "testSetAppIsLaunching") {
            nativeSetAppIsLaunching(it, true)
        }
    }

    // bsg_ctj_set_device_jailbroken
    private external fun nativeSetJailbroken(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetJailbroken() {
        runJournalTest(mutableMapOf(), "testSetJailbroken") {
            nativeSetJailbroken(it, false)
        }
    }

    // bsg_ctj_set_device_id
    private external fun nativeSetDeviceId(ctjPath: String, value: String): Int

    @Test
    fun testSetDeviceId() {
        runJournalTest(mutableMapOf(), "testSetDeviceId") {
            nativeSetDeviceId(it, "my-id")
        }
    }

    // bsg_ctj_set_device_locale
    private external fun nativeSetLocale(ctjPath: String, value: String): Int

    @Test
    fun testSetLocale() {
        runJournalTest(mutableMapOf(), "testSetLocale") {
            nativeSetLocale(it, "my-locale")
        }
    }

    // bsg_ctj_set_device_manufacturer
    private external fun nativeSetManufacturer(ctjPath: String, value: String): Int

    @Test
    fun testSetManufacturer() {
        runJournalTest(mutableMapOf(), "testSetManufacturer") {
            nativeSetManufacturer(it, "my-manufacturer")
        }
    }

    // bsg_ctj_set_device_model
    private external fun nativeSetDeviceModel(ctjPath: String, value: String): Int

    @Test
    fun testSetDeviceModel() {
        runJournalTest(mutableMapOf(), "testSetDeviceModel") {
            nativeSetDeviceModel(it, "my-model")
        }
    }

    // bsg_ctj_set_device_os_version
    private external fun nativeSetOsVersion(ctjPath: String, value: String): Int

    @Test
    fun testSetOsVersion() {
        runJournalTest(mutableMapOf(), "testSetOsVersion") {
            nativeSetOsVersion(it, "my-version")
        }
    }

    // bsg_ctj_set_device_total_memory
    private external fun nativeSetTotalMemory(ctjPath: String, value: Long): Int

    @Test
    fun testSetTotalMemory() {
        runJournalTest(mutableMapOf(), "testSetTotalMemory") {
            nativeSetTotalMemory(it, 1000000000)
        }
    }

    // bsg_ctj_set_device_orientation
    private external fun nativeSetOrientation(ctjPath: String, value: String): Int

    @Test
    fun testSetOrientation() {
        runJournalTest(mutableMapOf(), "testSetOrientation") {
            nativeSetOrientation(it, "my-orientation")
        }
    }

    // bsg_ctj_set_device_time_seconds
    private external fun nativeSetDeviceTime(ctjPath: String, value: Long): Int

    @Test
    fun testSetDeviceTime() {
        runJournalTest(mutableMapOf(), "testSetDeviceTime") {
            nativeSetDeviceTime(it, 1000000000)
        }
    }

    // bsg_ctj_set_device_os_name
    private external fun nativeSetOsName(ctjPath: String, value: String): Int

    @Test
    fun testSetOsName() {
        runJournalTest(mutableMapOf(), "testSetOsName") {
            nativeSetOsName(it, "my-name")
        }
    }

    // bsg_ctj_set_error_class
    private external fun nativeSetErrorClass(ctjPath: String, value: String): Int

    @Test
    fun testSetErrorClass() {
        runJournalTest(mutableMapOf(), "testSetErrorClass") {
            nativeSetErrorClass(it, "my-class")
        }
    }

    // bsg_ctj_set_error_message
    private external fun nativeSetErrorMessage(ctjPath: String, value: String): Int

    @Test
    fun testSetErrorMessage() {
        runJournalTest(mutableMapOf(), "testSetErrorMessage") {
            nativeSetErrorMessage(it, "my-message")
        }
    }

    // bsg_ctj_set_error_type
    private external fun nativeSetErrorType(ctjPath: String, value: String): Int

    @Test
    fun testSetErrorType() {
        runJournalTest(mutableMapOf(), "testSetErrorType") {
            nativeSetErrorType(it, "my-type")
        }
    }

    // bsg_ctj_set_event_severity
    private external fun nativeSetEventSeverity(ctjPath: String, value: Int): Int

    @Test
    fun testSetEventSeverity() {
        runJournalTest(mutableMapOf(), "testSetEventSeverity") {
            nativeSetEventSeverity(it, 1)
        }
    }

    // bsg_ctj_set_event_unhandled
    private external fun nativeSetEventUnhandled(ctjPath: String, value: Boolean): Int

    @Test
    fun testSetEventUnhandled() {
        runJournalTest(mutableMapOf(), "testSetEventUnhandled") {
            nativeSetEventUnhandled(it, true)
        }
    }

    // bsg_ctj_set_event_grouping_hash
    private external fun nativeSetGroupingHash(ctjPath: String, value: String): Int

    @Test
    fun testSetGroupingHash() {
        runJournalTest(mutableMapOf(), "testSetGroupingHash") {
            nativeSetGroupingHash(it, "my-hash")
        }
    }

    // bsg_ctj_set_metadata_double
    private external fun nativeAddMetadataDouble(ctjPath: String, section: String, name: String, value: Double): Int

    @Test
    fun testAddMetadataDouble() {
        runJournalTest(mutableMapOf(), "testAddMetadataDouble") {
            nativeAddMetadataDouble(it, "my-section", "my-name", 1.5)
        }
        runJournalTest(mutableMapOf(), "testAddMetadataDigitsDouble") {
            nativeAddMetadataDouble(it, "-1", "0\\.+", 1.5)
        }
    }

    // bsg_ctj_set_metadata_bool
    private external fun nativeAddMetadataBool(ctjPath: String, section: String, name: String, value: Boolean): Int

    @Test
    fun testAddMetadataBool() {
        runJournalTest(mutableMapOf(), "testAddMetadataBool") {
            nativeAddMetadataBool(it, "my-section", "my-name", true)
        }
        runJournalTest(mutableMapOf(), "testAddMetadataDigitsBool") {
            nativeAddMetadataBool(it, "1000", "3456\\.+", false)
        }
    }

    // bsg_ctj_set_metadata_string
    private external fun nativeAddMetadataString(ctjPath: String, section: String, name: String, value: String): Int

    @Test
    fun testAddMetadataString() {
        runJournalTest(mutableMapOf(), "testAddMetadataString") {
            nativeAddMetadataString(it, "my-section", "my-name", "my-value")
        }
        runJournalTest(mutableMapOf(), "testAddMetadataDigitsString") {
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

    // bsg_ctj_set_app_in_foreground
    private external fun nativeIncrementUnhandled(ctjPath: String): Int

    @Test
    fun testIncrementUnhandled() {
        runJournalTest(mutableMapOf(), "testIncrementUnhandled") {
            nativeIncrementUnhandled(it)
        }
    }

    // bsg_ctj_store_event
// TODO PLAT-7589
//    private external fun nativeStoreEvent(ctjPath: String): Int
//
//    @Test
//    fun testStoreEvent() {
//        runJournalTest(mutableMapOf(), "testStoreEvent") {
//            nativeStoreEvent(it)
//        }
//    }
}
