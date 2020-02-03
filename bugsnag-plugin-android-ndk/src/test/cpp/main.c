#include <android/log.h>

#define GREATEST_FPRINTF(ignore, fmt, ...) __android_log_print(ANDROID_LOG_INFO, "BugsnagNDKTest", fmt, ##__VA_ARGS__)

#include <greatest/greatest.h>
#include <jni.h>

#include <utils/serializer.h>
#include <stdlib.h>
#include <utils/migrate.h>
#include "test_serializer.h"

SUITE(string_utils);
SUITE(serialize_utils);
SUITE(breadcrumbs);
SUITE(event_mutators);

GREATEST_MAIN_DEFS();

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCXXTest_run(
    JNIEnv *_env, jobject _this) {
    int argc = 0;
    char *argv[] = {};
    GREATEST_MAIN_BEGIN();
    RUN_SUITE(string_utils);
    RUN_SUITE(serialize_utils);
    RUN_SUITE(breadcrumbs);
    RUN_SUITE(event_mutators);
    GREATEST_MAIN_END();
}

TEST test_user_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bsg_user_t *user = test_case->data_ptr;
    bsg_serialize_user(*user, event);
    free(user);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_UserSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadUserTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_user_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_app_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bsg_app_info *app = test_case->data_ptr;
    bsg_serialize_app(*app, event);
    free(app);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_AppSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadAppTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_app_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_app_meta_data_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bsg_app_info *app = test_case->data_ptr;
    bsg_serialize_app_metadata(*app, event);
    free(app);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_AppMetadataSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadAppMetadataTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_app_meta_data_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_device_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bsg_device_info *device = test_case->data_ptr;
    bsg_serialize_device(*device, event);
    free(device);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_DeviceSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadDeviceTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_device_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_device_meta_data_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bsg_device_info *device = test_case->data_ptr;
    bsg_serialize_device_metadata(*device, event);
    free(device);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_DeviceMetadataSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadDeviceMetadataTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_device_meta_data_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_custom_meta_data_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bugsnag_metadata *meta_data = test_case->data_ptr;
    bsg_serialize_custom_metadata(*meta_data, event);
    free(meta_data);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_CustomMetadataSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadCustomMetadataTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_custom_meta_data_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_context_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bugsnag_event *event = test_case->data_ptr;
    bsg_serialize_context(event, event_obj);
    free(event);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_ContextSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadContextTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_context_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_handled_state_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bugsnag_event *event = test_case->data_ptr;
    bsg_serialize_handled_state(event, event_obj);
    free(event);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_HandledStateSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadHandledStateTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_handled_state_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_session_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event_obj = json_value_get_object(event_val);
    bugsnag_event *event = test_case->data_ptr;
    bsg_serialize_session(event, event_obj);
    free(event);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_SessionSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadSessionTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_session_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_breadcrumbs_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_array();
    JSON_Array *event_ary = json_value_get_array(event_val);
    bugsnag_event *event = test_case->data_ptr;
    bsg_serialize_breadcrumbs(event, event_ary);
    free(event);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_BreadcrumbStateSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadBreadcrumbsTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_breadcrumbs_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_stackframe_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_array();
    JSON_Array *event = json_value_get_array(event_val);
    bsg_stackframe_t *frame = test_case->data_ptr;
    bsg_serialize_stackframe(frame, event);
    free(frame);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_StackframeSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadStackframeTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_stackframe_serialization, test_case);
    GREATEST_MAIN_END();
}

TEST test_exception_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *exception = json_value_get_object(event_val);
    JSON_Value *stack_val = json_value_init_array();
    JSON_Array *stacktrace = json_value_get_array(stack_val);
    json_object_set_value(exception, "stacktrace", stack_val);

    bsg_error *exc = test_case->data_ptr;
    bsg_serialize_error(*exc, exception, stacktrace);
    free(exc);
    return validate_serialized_json(test_case, event_val);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_ExceptionSerializationTest_run(
        JNIEnv *_env, jobject _this, jint num, jstring expected_json) {
    int argc = 0;
    char *argv[] = {};
    test_case *test_case = malloc(sizeof(test_case));
    test_case->data_ptr = loadExceptionTestCase(num);

    char *str = (char *) (*_env)->GetStringUTFChars(_env, expected_json, 0);
    test_case->expected_json = str;
    GREATEST_MAIN_BEGIN();
    RUN_TEST1(test_exception_serialization, test_case);
    GREATEST_MAIN_END();
}
