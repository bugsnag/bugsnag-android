//
// Created by Karl Stenerud on 15.10.21.
//

#include <greatest/greatest.h>
#include <bugsnag_ndk.h>
#include "crashtime_journal.h"
#include "crashtime_journal_primitives.h"
#include "test_helpers.h"
#include "safejni.h"

void bsg_ctj_test_reset();

TEST init_journal_test(JNIEnv *_env, jstring _path) {
    const char *path = bsg_safe_get_string_utf_chars(_env, _path);
    if (path == NULL) {
        FAILm("Error retrieving path string");
    }
    bsg_ctj_test_reset();
    ASSERT_EQ(true, bsg_ctj_init(path));
    return GREATEST_TEST_RES_PASS;
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetApiKey(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_api_key(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetContext(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_event_context(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetUser(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath,
        jstring _id, jstring _email, jstring _name) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *id = bsg_safe_get_string_utf_chars(_env, _id);
    const char *email = bsg_safe_get_string_utf_chars(_env, _email);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    ASSERT_EQ(true, bsg_ctj_set_event_user(id, email, name));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetBinaryArch(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_app_binary_arch(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetBuildUuid(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_app_build_uuid(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppId(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_app_id(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppReleaseStage(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_app_release_stage(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppType(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_app_type(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppVersion(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_app_version(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppVersionCode(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jint _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_app_version_code(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppDuration(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jint _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_app_duration(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppDurationInForeground(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jint _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_app_duration_in_foreground(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppInForeground(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jboolean _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_app_in_foreground(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetAppIsLaunching(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jboolean _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_app_is_launching(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetJailbroken(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jboolean _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_device_jailbroken(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetDeviceId(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_device_id(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetLocale(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_device_locale(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetManufacturer(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_device_manufacturer(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetDeviceModel(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_device_model(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetOsVersion(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_device_os_version(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetTotalMemory(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jlong _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_device_total_memory(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetOrientation(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_device_orientation(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetDeviceTime(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jlong _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_device_time_seconds(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetOsName(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_device_os_name(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetErrorClass(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_error_class(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetErrorMessage(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_error_message(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetErrorType(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_error_type(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetEventSeverity(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jlong _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_event_severity(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetEventUnhandled(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jboolean _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_event_unhandled(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetGroupingHash(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_event_grouping_hash(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeAddMetadataDouble(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section, jstring _name, jdouble value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    ASSERT_EQ(true, bsg_ctj_set_metadata_double(section, name, value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeAddMetadataString(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section, jstring _name, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_metadata_string(section, name, value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeAddMetadataBool(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section, jstring _name, jboolean value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    ASSERT_EQ(true, bsg_ctj_set_metadata_bool(section, name, value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeClearMetadata(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section, jstring _name) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    ASSERT_EQ(true, bsg_ctj_clear_metadata(section, name));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeClearMetadataSection(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    ASSERT_EQ(true, bsg_ctj_clear_metadata_section(section));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeStoreEvent(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    bugsnag_event event;
    event.severity = BSG_SEVERITY_ERR;
    event.unhandled = true;
    event.unhandled_events = 1;
    event.handled_events = 5;
    strcpy(event.session_id, "123");
    strcpy(event.session_start, "2018-08-07T10:16:34.564Z");
    event.device.time = 1500000000;

    // error
    bsg_error *error = &event.error;
    strcpy(error->errorMessage, "test message");
    strcpy(error->errorClass, "SIGSEGV");
    strcpy(error->type, "c");
    error->frame_count = 2;

    for (int i = 0; i < error->frame_count; i++) {
        bugsnag_stackframe *frame = &error->stacktrace[i];
        frame->frame_address = 0x0000 + i;
        frame->load_address = 0x1000 + i;
        frame->symbol_address = 0x2000 + i;
        frame->line_number = 100 + i;
        sprintf(frame->filename, "file_%d.c", i);
        sprintf(frame->method, "method_%d", i);
    }

    // threads
    event.thread_count = 2;
    strcpy(event.threads[0].name, "ConnectivityThr");
    event.threads[0].id = 29695;
    strcpy(event.threads[0].state, "running");
    strcpy(event.threads[1].name, "Binder:29227_3");
    event.threads[1].id = 29698;
    strcpy(event.threads[1].state, "sleeping");
    ASSERT_EQ(true, bsg_ctj_store_event(&event));
    PASS();
}
