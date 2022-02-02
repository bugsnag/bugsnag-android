//
// Created by Karl Stenerud on 03.01.22.
//

#include "jni_cache.h"
#include "safejni.h"

#include <android/log.h>
#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagNDK", fmt, ##__VA_ARGS__)
#endif

static bsg_jni_cache_t jni_cache;
bsg_jni_cache_t *bsg_jni_cache = &jni_cache;

// All classes must be cached as global refs
#define CACHE_CLASS(CLASS, PATH)                                               \
  do {                                                                         \
    jclass cls = bsg_safe_find_class(env, PATH);                               \
    if (cls == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI class ref " #CLASS                \
                  " (%s) is NULL",                                             \
                  PATH);                                                       \
      goto failed;                                                             \
    }                                                                          \
    bsg_jni_cache->CLASS = (*env)->NewGlobalRef(env, cls);                     \
  } while (0)

// Methods are IDs, which remain valid as long as their class ref remains valid.
#define CACHE_METHOD(CLASS, METHOD, NAME, PARAMS)                              \
  do {                                                                         \
    jmethodID mtd =                                                            \
        bsg_safe_get_method_id(env, bsg_jni_cache->CLASS, NAME, PARAMS);       \
    if (mtd == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI method ref " #CLASS "." #METHOD   \
                  " (%s%s) is NULL",                                           \
                  NAME, PARAMS);                                               \
      goto failed;                                                             \
    }                                                                          \
    bsg_jni_cache->METHOD = mtd;                                               \
  } while (0)

#define CACHE_STATIC_METHOD(CLASS, METHOD, NAME, PARAMS)                       \
  do {                                                                         \
    jmethodID mtd = bsg_safe_get_static_method_id(env, bsg_jni_cache->CLASS,   \
                                                  NAME, PARAMS);               \
    if (mtd == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI method ref " #CLASS "." #METHOD   \
                  " (%s%s) is NULL",                                           \
                  NAME, PARAMS);                                               \
      goto failed;                                                             \
    }                                                                          \
    bsg_jni_cache->METHOD = mtd;                                               \
  } while (0)

bool bsg_jni_cache_init(JNIEnv *env) {
  if (bsg_jni_cache->initialized) {
    return true;
  }

  (*env)->GetJavaVM(env, &bsg_jni_cache->jvm);
  if (bsg_jni_cache->jvm == NULL) {
    BUGSNAG_LOG("JNI Cache Init Error: Could not get global JavaVM");
    goto failed;
  }

  CACHE_CLASS(boolean, "java/lang/Boolean");
  CACHE_METHOD(boolean, boolean_bool_value, "booleanValue", "()Z");

  CACHE_CLASS(float_class, "java/lang/Float");
  CACHE_METHOD(float_class, float_float_value, "floatValue", "()F");

  CACHE_CLASS(number, "java/lang/Number");
  CACHE_METHOD(number, number_double_value, "doubleValue", "()D");

  CACHE_CLASS(string, "java/lang/String");

  CACHE_CLASS(arraylist, "java/util/ArrayList");
  CACHE_METHOD(arraylist, arraylist_init_with_obj, "<init>",
               "(Ljava/util/Collection;)V");
  CACHE_METHOD(arraylist, arraylist_get, "get", "(I)Ljava/lang/Object;");

  CACHE_CLASS(map, "java/util/Map");
  CACHE_METHOD(map, map_key_set, "keySet", "()Ljava/util/Set;");
  CACHE_METHOD(map, map_size, "size", "()I");
  CACHE_METHOD(map, map_get, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");

  CACHE_CLASS(hash_map, "java/util/HashMap");
  CACHE_METHOD(hash_map, hash_map_key_set, "keySet", "()Ljava/util/Set;");
  CACHE_METHOD(hash_map, hash_map_size, "size", "()I");
  CACHE_METHOD(hash_map, hash_map_get, "get",
               "(Ljava/lang/Object;)Ljava/lang/Object;");

  CACHE_CLASS(native_interface, "com/bugsnag/android/NativeInterface");
  CACHE_STATIC_METHOD(native_interface, ni_get_app, "getApp",
                      "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(native_interface, ni_get_device, "getDevice",
                      "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(native_interface, ni_get_user, "getUser",
                      "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(native_interface, ni_set_user, "setUser", "([B[B[B)V");
  CACHE_STATIC_METHOD(native_interface, ni_get_metadata, "getMetadata",
                      "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(native_interface, ni_get_context, "getContext",
                      "()Ljava/lang/String;");
  CACHE_STATIC_METHOD(
      native_interface, ni_notify, "notify",
      "([B[BLcom/bugsnag/android/Severity;[Ljava/lang/StackTraceElement;)V");
  CACHE_STATIC_METHOD(native_interface, ni_deliver_report, "deliverReport",
                      "([B[BLjava/lang/String;Z)V");
  CACHE_STATIC_METHOD(native_interface, ni_leave_breadcrumb, "leaveBreadcrumb",
                      "([BLcom/bugsnag/android/BreadcrumbType;)V");

  CACHE_CLASS(stack_trace_element, "java/lang/StackTraceElement");
  CACHE_METHOD(stack_trace_element, ste_constructor, "<init>",
               "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");

  CACHE_CLASS(severity, "com/bugsnag/android/Severity");

  CACHE_CLASS(breadcrumb_type, "com/bugsnag/android/BreadcrumbType");

  bsg_jni_cache->initialized = true;
  return true;

failed:
  return false;
}
