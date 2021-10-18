package com.bugsnag.android.ndk;

import static com.bugsnag.android.ndk.VerifyUtilsKt.verifyNativeRun;

import com.bugsnag.android.internal.journal.JournaledDocument;
import com.bugsnag.android.internal.journal.JsonHelper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NativeCrashTimeJournalTest {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("bugsnag-ndk-test");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(String json) {
        Map<String, Object> document =
                JsonHelper.Companion.deserialize(json.getBytes(StandardCharsets.UTF_8));
        return (Map<String, Object>) BugsnagTestUtils.normalized(document);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDocument(File baseDocumentPath) {
        Map<? super String, ?> document =
                JournaledDocument.Companion.loadDocumentContents(baseDocumentPath);
        return (Map<String, Object>) BugsnagTestUtils.normalized(document);
    }

    private final String standardType = "Bugsnag state";
    private final int standardVersion = 1;

    private void openAndCloseDocument(File baseDocumentPath, Map<String, Object> initialDocument) {
        long bufferSize = 100L;
        JournaledDocument document = new JournaledDocument(
                baseDocumentPath,
                standardType,
                standardVersion,
                bufferSize,
                bufferSize,
                initialDocument
        );
        document.close();
    }

    private void runTest(NativeTestCodeRunner runner, Map<String, Object> initialDocument,
                         String expectedStructure) throws Exception {
        if (initialDocument == null) {
            initialDocument = new HashMap<>();
        }
        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        File baseFile = folder.newFile("bugsnag-journal");
        openAndCloseDocument(baseFile, initialDocument);
        verifyNativeRun(runner.run(baseFile.getAbsolutePath() + ".journal.crashtime"));
        Map<? super String, ?> actual = loadDocument(baseFile);
        folder.delete();
        Map<? super String, ?> expected = jsonToMap(expectedStructure);

        Assert.assertEquals(expected, actual);
    }

    // bsg_ctj_set_api_key

    public native int nativeSetApiKey(String ctjPath, String value);

    @Test
    public void testSetApiKey() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetApiKey(ctjPath, "my-key");
            }
        };

        String expectedStructure = "{"
                + "    \"apiKey\": \"my-key\""
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_context

    public native int nativeSetContext(String ctjPath, String value);

    @Test
    public void testSetContext() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetContext(ctjPath, "my-context");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"context\": \"my-context\""
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_user

    public native int runNativeSetUser(String ctjPath, String id, String email, String name);

    @Test
    public void testSetUser() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return runNativeSetUser(ctjPath, "my-id", "my-user@nowhere.com", "my-name");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"user\": {"
                + "                \"name\": \"my-name\","
                + "                \"id\": \"my-id\","
                + "                \"email\": \"my-user@nowhere.com\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_binary_arch

    public native int nativeSetBinaryArch(String ctjPath, String value);

    @Test
    public void testSetBinaryArch() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetBinaryArch(ctjPath, "my-arch");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"binaryArch\": \"my-arch\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_build_uuid

    public native int nativeSetBuildUuid(String ctjPath, String value);

    @Test
    public void testSetBuildUuid() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetBuildUuid(ctjPath, "123e4567-e89b-12d3-a456-426614174000");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"buildUUID\": \"123e4567-e89b-12d3-a456-426614174000\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_id

    public native int nativeSetAppId(String ctjPath, String value);

    @Test
    public void testSetAppId() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppId(ctjPath, "my-id");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"id\": \"my-id\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_release_stage

    public native int nativeSetAppReleaseStage(String ctjPath, String value);

    @Test
    public void testSetAppReleaseStage() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppReleaseStage(ctjPath, "my-release-stage");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"releaseStage\": \"my-release-stage\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_type

    public native int nativeSetAppType(String ctjPath, String value);

    @Test
    public void testSetAppType() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppType(ctjPath, "my-type");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"type\": \"my-type\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_version

    public native int nativeSetAppVersion(String ctjPath, String value);

    @Test
    public void testSetAppVersion() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppVersion(ctjPath, "my-version");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"version\": \"my-version\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_version_code

    public native int nativeSetAppVersionCode(String ctjPath, int value);

    @Test
    public void testSetAppVersionCode() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppVersionCode(ctjPath, 100);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"versionCode\": 1E+2"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_duration

    public native int nativeSetAppDuration(String ctjPath, int value);

    @Test
    public void testSetAppDuration() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppDuration(ctjPath, 100);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"duration\": 1E+2"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_duration_in_foreground

    public native int nativeSetAppDurationInForeground(String ctjPath, int value);

    @Test
    public void testSetAppDurationInForeground() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppDurationInForeground(ctjPath, 100);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"durationInForeground\": 1E+2"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_in_foreground

    public native int nativeSetAppInForeground(String ctjPath, boolean value);

    @Test
    public void testSetAppInForeground() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppInForeground(ctjPath, true);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"inForeground\": true"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_app_is_launching

    public native int nativeSetAppIsLaunching(String ctjPath, boolean value);

    @Test
    public void testSetAppIsLaunching() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetAppIsLaunching(ctjPath, true);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"app\": {"
                + "                \"isLaunching\": true"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_jailbroken

    public native int nativeSetJailbroken(String ctjPath, boolean value);

    @Test
    public void testSetJailbroken() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetJailbroken(ctjPath, false);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"jailbroken\": false"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_device_id

    public native int nativeSetDeviceId(String ctjPath, String value);

    @Test
    public void testSetDeviceId() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetDeviceId(ctjPath, "my-id");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"id\": \"my-id\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_locale

    public native int nativeSetLocale(String ctjPath, String value);

    @Test
    public void testSetLocale() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetLocale(ctjPath, "my-locale");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"locale\": \"my-locale\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_manufacturer

    public native int nativeSetManufacturer(String ctjPath, String value);

    @Test
    public void testSetManufacturer() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetManufacturer(ctjPath, "my-manufacturer");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"manufacturer\": \"my-manufacturer\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_device_model

    public native int nativeSetDeviceModel(String ctjPath, String value);

    @Test
    public void testSetDeviceModel() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetDeviceModel(ctjPath, "my-model");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"model\": \"my-model\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_os_version

    public native int nativeSetOsVersion(String ctjPath, String value);

    @Test
    public void testSetOsVersion() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetOsVersion(ctjPath, "my-version");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"osVersion\": \"my-version\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_total_memory

    public native int nativeSetTotalMemory(String ctjPath, long value);

    @Test
    public void testSetTotalMemory() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetTotalMemory(ctjPath, 1000000000);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"totalMemory\": 1000000000"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_orientation

    public native int nativeSetOrientation(String ctjPath, String value);

    @Test
    public void testSetOrientation() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetOrientation(ctjPath, "my-orientation");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"orientation\": \"my-orientation\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_device_time

    public native int nativeSetDeviceTime(String ctjPath, long value);

    @Test
    public void testSetDeviceTime() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetDeviceTime(ctjPath, 1000000000);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"timeUnixTimestamp\": 1000000000"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_os_name

    public native int nativeSetOsName(String ctjPath, String value);

    @Test
    public void testSetOsName() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetOsName(ctjPath, "my-name");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"device\": {"
                + "                \"osName\": \"my-name\""
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_error_class

    public native int nativeSetErrorClass(String ctjPath, String value);

    @Test
    public void testSetErrorClass() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetErrorClass(ctjPath, "my-class");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"exceptions\": ["
                + "                {"
                + "                    \"errorClass\": \"my-class\""
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_error_message

    public native int nativeSetErrorMessage(String ctjPath, String value);

    @Test
    public void testSetErrorMessage() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetErrorMessage(ctjPath, "my-message");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"exceptions\": ["
                + "                {"
                + "                    \"message\": \"my-message\""
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_error_type

    public native int nativeSetErrorType(String ctjPath, String value);

    @Test
    public void testSetErrorType() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetErrorType(ctjPath, "my-type");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"exceptions\": ["
                + "                {"
                + "                    \"type\": \"my-type\""
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_event_severity

    public native int nativeSetEventSeverity(String ctjPath, long value);

    @Test
    public void testSetEventSeverity() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetEventSeverity(ctjPath, 1);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"severity\": \"warning\""
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_event_unhandled

    public native int nativeSetEventUnhandled(String ctjPath, boolean value);

    @Test
    public void testSetEventUnhandled() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetEventUnhandled(ctjPath, true);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"unhandled\": true"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_set_grouping_hash

    public native int nativeSetGroupingHash(String ctjPath, String value);

    @Test
    public void testSetGroupingHash() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeSetGroupingHash(ctjPath, "my-hash");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"groupingHash\": \"my-hash\""
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_add_metadata_double

    public native int nativeAddMetadataDouble(String ctjPath, String section, String name,
                                              double value);

    @Test
    public void testAddMetadataDouble() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeAddMetadataDouble(ctjPath, "my-section", "my-name", 1.5);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"metaData\": {"
                + "                \"my-section\": {"
                + "                    \"my-name\": 1.5"
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_add_metadata_string

    public native int nativeAddMetadataString(String ctjPath, String section, String name,
                                              String value);

    @Test
    public void testAddMetadataString() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeAddMetadataString(ctjPath, "my-section", "my-name", "my-value");
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"metaData\": {"
                + "                \"my-section\": {"
                + "                    \"my-name\": \"my-value\""
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_add_metadata_bool

    public native int nativeAddMetadataBool(String ctjPath, String section, String name,
                                            boolean value);

    @Test
    public void testAddMetadataBool() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeAddMetadataBool(ctjPath, "my-section", "my-name", true);
            }
        };

        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"metaData\": {"
                + "                \"my-section\": {"
                + "                    \"my-name\": true"
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, null, expectedStructure);
    }

    // bsg_ctj_clear_metadata

    public native int nativeClearMetadata(String ctjPath, String section, String name);

    @Test
    public void testClearMetadata() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeClearMetadata(ctjPath, "my-section", "my-name");
            }
        };

        String initialDocument = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"metaData\": {"
                + "                \"my-section\": {"
                + "                    \"my-name\": true,"
                + "                    \"my-other-name\": 100"
                + "                },"
                + "                \"my-other-section\": {"
                + "                    \"a\": true,"
                + "                    \"b\": 100"
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"metaData\": {"
                + "                \"my-section\": {"
                + "                    \"my-other-name\": 100"
                + "                },"
                + "                \"my-other-section\": {"
                + "                    \"a\": true,"
                + "                    \"b\": 100"
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, jsonToMap(initialDocument), expectedStructure);
    }

    // bsg_ctj_clear_metadata_section

    public native int nativeClearMetadataSection(String ctjPath, String section);

    @Test
    public void testClearMetadataSection() throws Exception {
        NativeTestCodeRunner runner = new NativeTestCodeRunner() {
            @Override
            public int run(String ctjPath) {
                return nativeClearMetadataSection(ctjPath, "my-section");
            }
        };

        String initialDocument = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"metaData\": {"
                + "                \"my-section\": {"
                + "                    \"my-name\": true,"
                + "                    \"my-other-name\": 100"
                + "                },"
                + "                \"my-other-section\": {"
                + "                    \"a\": true,"
                + "                    \"b\": 100"
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        String expectedStructure = "{"
                + "    \"events\": ["
                + "        {"
                + "            \"metaData\": {"
                + "                \"my-other-section\": {"
                + "                    \"a\": true,"
                + "                    \"b\": 100"
                + "                }"
                + "            }"
                + "        }"
                + "    ]"
                + "}";
        runTest(runner, jsonToMap(initialDocument), expectedStructure);
    }
}
