// Copied from jni_cache.h in bugsnag-plugin-android-ndk (see PLAT-5794).
// Please keep logic code in sync.

#ifndef BUGSNAG_ANDROID_ANR_JNI_CACHE_H
#define BUGSNAG_ANDROID_ANR_JNI_CACHE_H

#include <jni.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  JavaVM *jvm;

  jclass Integer;
  jmethodID Integer_init;

  jclass Long;
  jmethodID Long_init;

  jclass LinkedList;
  jmethodID LinkedList_init;
  jmethodID LinkedList_add;

  jclass AnrPlugin;
  jmethodID AnrPlugin_notifyAnrDetected;

  jclass ErrorType;
  jobject ErrorType_C;

  jclass NativeStackFrame;
  jmethodID NativeStackFrame_init;

  bool initialized;
} bsg_anr_jni_cache_t;

extern bsg_anr_jni_cache_t *bsg_anr_jni_cache;

/**
 * Populate all references in the JNI cache.
 *
 * @param env The JNI env
 * @return false if an error occurs, in which case the cache is unusable.
 */
bool bsg_anr_jni_cache_init(JNIEnv *env);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_ANR_JNI_CACHE_H
