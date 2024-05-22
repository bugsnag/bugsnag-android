#include <stdlib.h>

#include <android/log.h>
#include <jni.h>

#define GREATEST_FPRINTF(ignore, fmt, ...)                                     \
  __android_log_print(ANDROID_LOG_INFO, "BugsnagNDKTest", fmt, ##__VA_ARGS__)

#include <greatest/greatest.h>
#include <parson/parson.h>

#include "test_bsg_event.h"
#include "test_serializer.h"

SUITE(suite_string_utils);
SUITE(suite_json_serialization);
SUITE(suite_breadcrumbs);
SUITE(suite_event_mutators);
SUITE(suite_event_app_mutators);
SUITE(suite_event_device_mutators);
SUITE(suite_struct_to_file);
SUITE(suite_feature_flags);

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

JNIEXPORT int JNICALL
Java_com_bugsnag_android_ndk_NativeStringTest_run(JNIEnv *_env, jobject _this) {
  return run_test_suite(suite_string_utils);
}

extern bool bsg_event_write(bsg_environment *env);

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeJsonSerializeTest_run(
    JNIEnv *_env, jobject _this, jstring _dir) {

  const char *dir = (*_env)->GetStringUTFChars(_env, _dir, NULL);
  if (dir == NULL) {
    return 0;
  }

  BUGSNAG_LOG("Writing event file to %s", dir);
  bsg_environment env;
  bugsnag_event *event = init_event();
  memcpy(&env.next_event, event, sizeof(bugsnag_event));

  env.event_path = strdup(dir);
  strcpy(env.event_uuid, "test-uuid");

  bsg_event_write(&env);

  free(event);

  (*_env)->ReleaseStringUTFChars(_env, _dir, dir);

  return 0;
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeBreadcrumbTest_run(
    JNIEnv *_env, jobject _this) {
  return run_test_suite(suite_breadcrumbs);
}

JNIEXPORT int JNICALL Java_com_bugsnag_android_ndk_NativeEventMutatorsTest_run(
    JNIEnv *_env, jobject _this) {
  return run_test_suite(suite_event_mutators);
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_ndk_NativeEventAppMutatorsTest_run(JNIEnv *_env,
                                                            jobject _this) {
  return run_test_suite(suite_event_app_mutators);
}

JNIEXPORT int JNICALL
Java_com_bugsnag_android_ndk_NativeEventDeviceMutatorsTest_run(JNIEnv *_env,
                                                               jobject _this) {
  return run_test_suite(suite_event_device_mutators);
}

JNIEXPORT jint JNICALL Java_com_bugsnag_android_ndk_NativeFeatureFlagsTest_run(
    JNIEnv *env, jobject thiz) {
  return run_test_suite(suite_feature_flags);
}
