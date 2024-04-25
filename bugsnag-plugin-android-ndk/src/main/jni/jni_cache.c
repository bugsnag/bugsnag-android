//
// Created by Karl Stenerud on 03.01.22.
//

#include "jni_cache.h"
#include "safejni.h"
#include "utils/logger.h"
#include <pthread.h>

#define JNI_VERSION JNI_VERSION_1_6

static bsg_jni_cache_t jni_cache;
bsg_jni_cache_t *const bsg_jni_cache = &jni_cache;

static pthread_key_t jni_cleanup_key;

static void detach_java_env(void *env) {
  if (bsg_jni_cache->initialized && env != NULL) {
    (*bsg_jni_cache->jvm)->DetachCurrentThread(bsg_jni_cache->jvm);
  }
}

JNIEnv *bsg_jni_cache_get_env() {
  if (!bsg_jni_cache->initialized) {
    return NULL;
  }

  JNIEnv *env = NULL;
  switch ((*bsg_jni_cache->jvm)
              ->GetEnv(bsg_jni_cache->jvm, (void **)&env, JNI_VERSION)) {
  case JNI_OK:
    return env;
  case JNI_EDETACHED:
    if ((*bsg_jni_cache->jvm)
            ->AttachCurrentThread(bsg_jni_cache->jvm, &env, NULL) != JNI_OK) {
      BUGSNAG_LOG("Could not attach thread to JVM");
      return NULL;
    }
    if (env == NULL) {
      BUGSNAG_LOG("AttachCurrentThread filled a NULL JNIEnv");
      return NULL;
    }

    // attach a destructor to detach the env before the thread terminates
    pthread_setspecific(jni_cleanup_key, env);

    return env;
  default:
    BUGSNAG_LOG("Could not get JNIEnv");
    return NULL;
  }
}

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

#define CACHE_ENUM_CONSTANT(ENUM, CLASS, NAME)                                 \
  do {                                                                         \
    jclass cls = bsg_safe_find_class(env, CLASS);                              \
    if (cls == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI enum class " CLASS " is NULL");   \
      goto failed;                                                             \
    }                                                                          \
    jfieldID fld =                                                             \
        bsg_safe_get_static_field_id(env, cls, NAME, "L" CLASS ";");           \
    if (fld == NULL) {                                                         \
      BUGSNAG_LOG("JNI Cache Init Error: JNI enum const " CLASS "." NAME       \
                  "  is NULL");                                                \
      goto failed;                                                             \
    }                                                                          \
    jobject value = bsg_safe_get_static_object_field(env, cls, fld);           \
    if (value == NULL) {                                                       \
      BUGSNAG_LOG("JNI Cache Init Error: JNI enum value " CLASS "." NAME       \
                  " is NULL");                                                 \
      goto failed;                                                             \
    }                                                                          \
    bsg_jni_cache->ENUM = (*env)->NewGlobalRef(env, value);                    \
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

  CACHE_CLASS(Boolean, "java/lang/Boolean");
  CACHE_METHOD(Boolean, Boolean_constructor, "<init>", "(Z)V");
  CACHE_METHOD(Boolean, Boolean_booleanValue, "booleanValue", "()Z");

  CACHE_CLASS(Float, "java/lang/Float");
  CACHE_METHOD(Float, Float_floatValue, "floatValue", "()F");

  CACHE_CLASS(number, "java/lang/Number");
  CACHE_METHOD(number, number_double_value, "doubleValue", "()D");

  CACHE_CLASS(Int, "java/lang/Integer");
  CACHE_METHOD(Int, Int_constructor, "<init>", "(I)V");
  CACHE_METHOD(Int, Int_intValue, "intValue", "()I");

  CACHE_CLASS(Long, "java/lang/Long");
  CACHE_STATIC_METHOD(Long, Long_valueOf, "valueOf", "(J)Ljava/lang/Long;");
  CACHE_METHOD(Long, Long_constructor, "<init>", "(J)V");

  CACHE_CLASS(String, "java/lang/String");

  CACHE_CLASS(ArrayList, "java/util/ArrayList");
  CACHE_METHOD(ArrayList, ArrayList_constructor_default, "<init>", "()V");
  CACHE_METHOD(ArrayList, ArrayList_constructor_collection, "<init>",
               "(Ljava/util/Collection;)V");
  CACHE_METHOD(ArrayList, ArrayList_get, "get", "(I)Ljava/lang/Object;");
  CACHE_METHOD(ArrayList, ArrayList_add, "add", "(Ljava/lang/Object;)Z");

  CACHE_CLASS(Set, "java/util/Set");
  CACHE_METHOD(Set, Set_iterator, "iterator", "()Ljava/util/Iterator;");

  CACHE_CLASS(Iterator, "java/util/Iterator");
  CACHE_METHOD(Iterator, Iterator_hasNext, "hasNext", "()Z");
  CACHE_METHOD(Iterator, Iterator_next, "next", "()Ljava/lang/Object;");

  CACHE_CLASS(Map, "java/util/Map");
  CACHE_METHOD(Map, Map_keySet, "keySet", "()Ljava/util/Set;");
  CACHE_METHOD(Map, Map_size, "size", "()I");
  CACHE_METHOD(Map, Map_get, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  CACHE_METHOD(Map, Map_put, "put",
               "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
  CACHE_METHOD(Map, Map_entrySet, "entrySet", "()Ljava/util/Set;");

  CACHE_CLASS(MapEntry, "java/util/Map$Entry");
  CACHE_METHOD(MapEntry, MapEntry_getKey, "getKey", "()Ljava/lang/Object;");
  CACHE_METHOD(MapEntry, MapEntry_getValue, "getValue", "()Ljava/lang/Object;");

  CACHE_CLASS(HashMap, "java/util/HashMap");
  CACHE_METHOD(HashMap, HashMap_constructor, "<init>", "()V");
  CACHE_METHOD(HashMap, HashMap_keySet, "keySet", "()Ljava/util/Set;");
  CACHE_METHOD(HashMap, HashMap_size, "size", "()I");
  CACHE_METHOD(HashMap, HashMap_get, "get",
               "(Ljava/lang/Object;)Ljava/lang/Object;");
  CACHE_METHOD(HashMap, HashMap_put, "put",
               "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

  CACHE_CLASS(NativeInterface, "com/bugsnag/android/NativeInterface");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_getApp, "getApp",
                      "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_getDevice, "getDevice",
                      "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_getUser, "getUser",
                      "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_setUser, "setUser",
                      "([B[B[B)V");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_getMetadata,
                      "getMetadata", "()Ljava/util/Map;");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_getContext, "getContext",
                      "()Ljava/lang/String;");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_notify, "notify",
                      "([B[BLcom/bugsnag/android/Severity;[Lcom/bugsnag/"
                      "android/NativeStackframe;)V");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_isDiscardErrorClass,
                      "isDiscardErrorClass", "(Ljava/lang/String;)Z");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_deliverReport,
                      "deliverReport", "([B[B[BLjava/lang/String;Z)V");
  CACHE_STATIC_METHOD(NativeInterface, NativeInterface_leaveBreadcrumb,
                      "leaveBreadcrumb",
                      "([BLcom/bugsnag/android/BreadcrumbType;)V");

  CACHE_CLASS(NativeStackframe, "com/bugsnag/android/NativeStackframe");
  CACHE_METHOD(
      NativeStackframe, NativeStackframe_constructor, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Number;"
      "Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Boolean;"
      "Lcom/bugsnag/android/ErrorType;Ljava/lang/String;)V");

  CACHE_CLASS(Severity, "com/bugsnag/android/Severity");

  CACHE_CLASS(BreadcrumbType, "com/bugsnag/android/BreadcrumbType");

  CACHE_CLASS(OpaqueValue, "com/bugsnag/android/ndk/OpaqueValue");
  CACHE_METHOD(OpaqueValue, OpaqueValue_getJson, "getJson",
               "()Ljava/lang/String;");
  CACHE_STATIC_METHOD(OpaqueValue, OpaqueValue_makeSafe, "makeSafe",
                      "(Ljava/lang/Object;)Ljava/lang/Object;");

  CACHE_ENUM_CONSTANT(ErrorType_C, "com/bugsnag/android/ErrorType", "C");

  pthread_key_create(&jni_cleanup_key, detach_java_env);

  bsg_jni_cache->initialized = true;
  return true;

failed:
  return false;
}
