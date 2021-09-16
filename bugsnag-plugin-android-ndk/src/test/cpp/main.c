#include <android/log.h>

#define GREATEST_FPRINTF(ignore, fmt, ...) __android_log_print(ANDROID_LOG_INFO, "BugsnagNDKTest", fmt, ##__VA_ARGS__)

#include <greatest/greatest.h>
#include <jni.h>

#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>
#include "test_serializer.h"

SUITE(suite_string_utils);
SUITE(suite_json_serialization);
SUITE(suite_breadcrumbs);
SUITE(suite_event_mutators);
SUITE(suite_event_app_mutators);
SUITE(suite_event_device_mutators);
SUITE(suite_struct_to_file);
SUITE(suite_struct_migration);

GREATEST_MAIN_DEFS();

/**
 * Runs a test suite using greatest.
 *
 * @param a pointer to the test_suite function
 * @return the exit code of the test suite
 */
int run_test_suite(void (*test_suite)(void)) {
    int argc = 0;
    char *argv[] = {};
    GREATEST_MAIN_BEGIN();
    RUN_SUITE(test_suite);
    GREATEST_MAIN_END();
}

/**
 * Runs a test using greatest.
 *
 * @param a pointer to the test_suite function
 * @return the exit code of the test suite
 */
int run_test(enum greatest_test_res (*test)(void)) {
    int argc = 0;
    char *argv[] = {};
    GREATEST_MAIN_BEGIN();
    RUN_TEST(test);
    GREATEST_MAIN_END();
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeStringTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_string_utils);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeJsonSerializeTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_json_serialization);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeStructToFileTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_struct_to_file);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeStructMigrationTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_struct_migration);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeBreadcrumbTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_breadcrumbs);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeEventMutatorsTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_event_mutators);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeEventAppMutatorsTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_event_app_mutators);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeEventDeviceMutatorsTest_run(
    JNIEnv *_env, jobject _this) {
    return run_test_suite(suite_event_device_mutators);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_UserSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadUserTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_user(event->user, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_AppSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadAppTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_app(event->app, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_AppMetadataSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadAppMetadataTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_app_metadata(event->app, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_DeviceSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadDeviceTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_device(event->device, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

TEST test_custom_meta_data_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bugsnag_metadata *meta_data = test_case->data_ptr;
    bsg_serialize_custom_metadata(*meta_data, event);
    free(meta_data);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_CustomMetadataSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadCustomMetadataTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_custom_metadata(event->metadata, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_ContextSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadContextTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_context(event, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_SeverityReasonSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadSeverityReasonTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_severity_reason(event, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_SessionSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadSessionTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bsg_serialize_session(event, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL
Java_com_bugsnag_android_ndk_BreadcrumbStateSerializationTest_run(JNIEnv *env,
                                                                  jobject thiz) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  loadBreadcrumbsTestCase(event);
  JSON_Value *eventVal = json_value_init_array();
  JSON_Array *eventAry = json_value_get_array(eventVal);
  bsg_serialize_breadcrumbs(event, eventAry);
  char *string = json_serialize_to_string(eventVal);
  return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_StackframeSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_stackframe *frame = loadStackframeTestCase();
    JSON_Value *event_val = json_value_init_array();
    JSON_Array *event_obj = json_value_get_array(event_val);
    bsg_serialize_stackframe(frame, false, event_obj);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jstring JNICALL Java_com_bugsnag_android_ndk_ExceptionSerializationTest_run(
        JNIEnv *env, jobject _this) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadExceptionTestCase(event);
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *exception = json_value_get_object(event_val);
    JSON_Value *stack_val = json_value_init_array();
    JSON_Array *stacktrace = json_value_get_array(stack_val);
    json_object_set_value(exception, "stacktrace", stack_val);
    bsg_serialize_error(event->error, exception, stacktrace);
    char *string = json_serialize_to_string(event_val);
    return (*env)->NewStringUTF(env, string);

}

JNIEXPORT jstring JNICALL
Java_com_bugsnag_android_ndk_ThreadSerializationTest_run(JNIEnv *env, jobject thiz) {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    loadThreadTestCase(event);
    JSON_Value *threads_val = json_value_init_array();
    JSON_Array *threads_array = json_value_get_array(threads_val);
    bsg_serialize_threads(event, threads_array);
    char *string = json_serialize_to_string(threads_val);
    return (*env)->NewStringUTF(env, string);
}