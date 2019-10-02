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

GREATEST_MAIN_DEFS();

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeCXXTest_run(
    JNIEnv *_env, jobject _this) {
    int argc = 0;
    char *argv[] = {};
    GREATEST_MAIN_BEGIN();
    RUN_SUITE(string_utils);
    RUN_SUITE(serialize_utils);
    RUN_SUITE(breadcrumbs);
    GREATEST_MAIN_END();
}

TEST test_user_serialization(test_case *test_case) {
    JSON_Value *event_val = json_value_init_object();
    JSON_Object *event = json_value_get_object(event_val);
    bsg_user *user = test_case->data_ptr;
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