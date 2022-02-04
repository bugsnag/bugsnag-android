// Copied from jni_cache.c in bugsnag-plugin-android-ndk (see PLAT-5794).
// Please keep logic code in sync.

#include "anr_jni_cache.h"
#include "anr_safejni.h"

#include <android/log.h>
#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagNDK", fmt, ##__VA_ARGS__)
#endif

static bsg_anr_jni_cache_t jni_cache;
bsg_anr_jni_cache_t *bsg_anr_jni_cache = &jni_cache;

// All classes must be cached as global refs
#define CACHE_CLASS(CLASS, PATH)                                               \
  do {                                                                         \
    jclass cls = bsg_anr_safe_find_class(env, PATH);                           \
    if (cls == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI class ref " #CLASS                \
                  " (%s) is NULL",                                             \
                  PATH);                                                       \
      goto failed;                                                             \
    }                                                                          \
    bsg_anr_jni_cache->CLASS = (*env)->NewGlobalRef(env, cls);                 \
  } while (0)

// Methods are IDs, which remain valid as long as their class ref remains valid.
#define CACHE_METHOD(CLASS, METHOD, NAME, PARAMS)                              \
  do {                                                                         \
    jmethodID mtd = bsg_anr_safe_get_method_id(env, bsg_anr_jni_cache->CLASS,  \
                                               NAME, PARAMS);                  \
    if (mtd == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI method ref " #CLASS "." #METHOD   \
                  " (%s%s) is NULL",                                           \
                  NAME, PARAMS);                                               \
      goto failed;                                                             \
    }                                                                          \
    bsg_anr_jni_cache->METHOD = mtd;                                           \
  } while (0)

#define CACHE_STATIC_METHOD(CLASS, METHOD, NAME, PARAMS)                       \
  do {                                                                         \
    jmethodID mtd = bsg_anr_safe_get_static_method_id(                         \
        env, bsg_anr_jni_cache->CLASS, NAME, PARAMS);                          \
    if (mtd == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI method ref " #CLASS "." #METHOD   \
                  " (%s%s) is NULL",                                           \
                  NAME, PARAMS);                                               \
      goto failed;                                                             \
    }                                                                          \
    bsg_anr_jni_cache->METHOD = mtd;                                           \
  } while (0)

#define CACHE_STATIC_FIELD(CLASS, FIELD, FIELD_NAME, TYPE)                     \
  do {                                                                         \
    jfieldID field = bsg_anr_safe_get_static_field_id(                         \
        env, bsg_anr_jni_cache->CLASS, FIELD_NAME, TYPE);                      \
    if (field == NULL) {                                                       \
      BUGSNAG_LOG("JNI Cache Init Error: JNI static field ref " #CLASS         \
                  "." #FIELD_NAME " (%s) is NULL",                             \
                  TYPE);                                                       \
      goto failed;                                                             \
    }                                                                          \
    jobject obj = bsg_anr_safe_get_static_object_field(                        \
        env, bsg_anr_jni_cache->CLASS, field);                                 \
    bsg_anr_jni_cache->FIELD = (*env)->NewGlobalRef(env, obj);                 \
  } while (0)

bool bsg_anr_jni_cache_init(JNIEnv *env) {
  if (bsg_anr_jni_cache->initialized) {
    return true;
  }

  (*env)->GetJavaVM(env, &bsg_anr_jni_cache->jvm);
  if (bsg_anr_jni_cache->jvm == NULL) {
    BUGSNAG_LOG("JNI Cache Init Error: Could not get global JavaVM");
    goto failed;
  }

  CACHE_CLASS(LinkedList, "java/util/LinkedList");
  CACHE_METHOD(LinkedList, LinkedList_init, "<init>", "()V");
  CACHE_METHOD(LinkedList, LinkedList_add, "add", "(Ljava/lang/Object;)Z");

  CACHE_CLASS(Integer, "java/lang/Integer");
  CACHE_METHOD(Integer, Integer_init, "<init>", "(I)V");

  CACHE_CLASS(Long, "java/lang/Long");
  CACHE_METHOD(Long, Long_init, "<init>", "(J)V");

  CACHE_CLASS(AnrPlugin, "com/bugsnag/android/AnrPlugin");
  CACHE_METHOD(AnrPlugin, AnrPlugin_notifyAnrDetected, "notifyAnrDetected",
               "(Ljava/util/List;)V");

  CACHE_CLASS(ErrorType, "com/bugsnag/android/ErrorType");
  CACHE_STATIC_FIELD(ErrorType, ErrorType_C, "C",
                     "Lcom/bugsnag/android/ErrorType;");

  CACHE_CLASS(NativeStackFrame, "com/bugsnag/android/NativeStackframe");
  CACHE_METHOD(
      NativeStackFrame, NativeStackFrame_init, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Number;Ljava/lang/"
      "Long;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Boolean;Lcom/bugsnag/"
      "android/ErrorType;)V");

  bsg_anr_jni_cache->initialized = true;
  return true;

failed:
  return false;
}
