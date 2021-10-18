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
    ASSERT_EQ(true, bsg_ctj_set_context(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_runNativeSetUser(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath,
        jstring _id, jstring _email, jstring _name) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *id = bsg_safe_get_string_utf_chars(_env, _id);
    const char *email = bsg_safe_get_string_utf_chars(_env, _email);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    ASSERT_EQ(true, bsg_ctj_set_user(id, email, name));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetBinaryArch(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_binary_arch(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetBuildUuid(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_build_uuid(value));
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
    ASSERT_EQ(true, bsg_ctj_set_jailbroken(_value));
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
    ASSERT_EQ(true, bsg_ctj_set_locale(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetManufacturer(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_manufacturer(value));
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
    ASSERT_EQ(true, bsg_ctj_set_os_version(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetTotalMemory(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jlong _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_total_memory(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetOrientation(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_orientation(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetDeviceTime(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jlong _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    ASSERT_EQ(true, bsg_ctj_set_device_time(_value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeSetOsName(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_set_os_name(value));
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
    ASSERT_EQ(true, bsg_ctj_set_grouping_hash(value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeAddMetadataDouble(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section, jstring _name, jdouble value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    ASSERT_EQ(true, bsg_ctj_add_metadata_double(section, name, value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeAddMetadataString(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section, jstring _name, jstring _value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    const char *value = bsg_safe_get_string_utf_chars(_env, _value);
    ASSERT_EQ(true, bsg_ctj_add_metadata_string(section, name, value));
    PASS();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCrashTimeJournalTest_nativeAddMetadataBool(
        JNIEnv *_env, jobject _this, jstring _crashtimeJournalPath, jstring _section, jstring _name, jboolean value) {
    STOP_ON_FAIL(init_journal_test(_env, _crashtimeJournalPath));
    const char *section = bsg_safe_get_string_utf_chars(_env, _section);
    const char *name = bsg_safe_get_string_utf_chars(_env, _name);
    ASSERT_EQ(true, bsg_ctj_add_metadata_bool(section, name, value));
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
