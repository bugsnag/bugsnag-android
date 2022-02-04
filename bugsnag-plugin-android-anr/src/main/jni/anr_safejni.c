// Copied from safejni.c in bugsnag-plugin-android-ndk (see PLAT-5794).
// Please keep logic code in sync.

#include "anr_safejni.h"
#include <stdbool.h>
#include <string.h>
#include <utils/string.h>

#include <android/log.h>
#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagNDK", fmt, ##__VA_ARGS__)
#endif

bool bsg_anr_check_and_clear_exc(JNIEnv *env) {
  if (env == NULL) {
    return false;
  }
  if ((*env)->ExceptionCheck(env)) {
    BUGSNAG_LOG("BUG: JNI Native->Java call threw an exception:");

    // Print a trace to stderr so that we can debug it
    (*env)->ExceptionDescribe(env);

    // Trigger more accurate dalvik trace (this will also crash the app).

    // Code review check: THIS MUST BE COMMENTED OUT IN CHECKED IN CODE!
    //(*env)->FindClass(env, NULL);

    // Clear the exception so that we don't crash.
    (*env)->ExceptionClear(env);
    return true;
  }
  return false;
}

jclass bsg_anr_safe_find_class(JNIEnv *env, const char *clz_name) {
  if (env == NULL || clz_name == NULL) {
    return NULL;
  }
  jclass clz = (*env)->FindClass(env, clz_name);
  bsg_anr_check_and_clear_exc(env);
  return clz;
}

jmethodID bsg_anr_safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                     const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetMethodID(env, clz, name, sig);
  bsg_anr_check_and_clear_exc(env);
  return methodId;
}

jmethodID bsg_anr_safe_get_static_method_id(JNIEnv *env, jclass clz,
                                            const char *name, const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetStaticMethodID(env, clz, name, sig);
  bsg_anr_check_and_clear_exc(env);
  return methodId;
}

jstring bsg_anr_safe_new_string_utf(JNIEnv *env, const char *str) {
  if (env == NULL || str == NULL) {
    return NULL;
  }
  jstring jstr = (*env)->NewStringUTF(env, str);
  bsg_anr_check_and_clear_exc(env);
  return jstr;
}

jfieldID bsg_anr_safe_get_static_field_id(JNIEnv *env, jclass clz,
                                          const char *name, const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jfieldID field_id = (*env)->GetStaticFieldID(env, clz, name, sig);
  bsg_anr_check_and_clear_exc(env);
  return field_id;
}

jobject bsg_anr_safe_get_static_object_field(JNIEnv *env, jclass clz,
                                             jfieldID field) {
  if (env == NULL || clz == NULL || field == NULL) {
    return NULL;
  }
  jobject obj = (*env)->GetStaticObjectField(env, clz, field);
  bsg_anr_check_and_clear_exc(env);
  return obj;
}

jobject bsg_anr_safe_new_object(JNIEnv *env, jclass clz, jmethodID method,
                                ...) {
  if (env == NULL || clz == NULL || method == NULL) {
    return NULL;
  }
  va_list args;
  va_start(args, method);
  jobject obj = (*env)->NewObjectV(env, clz, method, args);
  va_end(args);
  bsg_anr_check_and_clear_exc(env);
  return obj;
}

void bsg_anr_safe_delete_local_ref(JNIEnv *env, jobject obj) {
  if (env == NULL || obj == NULL) {
    return;
  }
  (*env)->DeleteLocalRef(env, obj);
}
