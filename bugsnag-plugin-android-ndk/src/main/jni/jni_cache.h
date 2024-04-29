//
// Created by Karl Stenerud on 03.01.22.
//

#ifndef BUGSNAG_ANDROID_JNI_CACHE_H
#define BUGSNAG_ANDROID_JNI_CACHE_H

#include <jni.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  bool initialized;

  JavaVM *jvm;

  jclass Boolean;
  jmethodID Boolean_constructor;
  jmethodID Boolean_booleanValue;

  jclass Int;
  jmethodID Int_constructor;
  jmethodID Int_intValue;

  jclass Long;
  jmethodID Long_constructor;
  jmethodID Long_valueOf;

  jclass Float;
  jmethodID Float_floatValue;

  jclass number;
  jmethodID number_double_value;

  jclass String;

  jclass Set;
  jmethodID Set_iterator;

  jclass Iterator;
  jmethodID Iterator_hasNext;
  jmethodID Iterator_next;

  jclass Map;
  jmethodID Map_get;
  jmethodID Map_put;
  jmethodID Map_size;
  jmethodID Map_keySet;
  jmethodID Map_entrySet;

  jclass MapEntry;
  jmethodID MapEntry_getKey;
  jmethodID MapEntry_getValue;

  jclass HashMap;
  jmethodID HashMap_constructor;
  jmethodID HashMap_get;
  jmethodID HashMap_put;
  jmethodID HashMap_size;
  jmethodID HashMap_keySet;

  jclass ArrayList;
  jmethodID ArrayList_constructor_default;
  jmethodID ArrayList_constructor_collection;
  jmethodID ArrayList_get;
  jmethodID ArrayList_add;

  jclass NativeInterface;
  jmethodID NativeInterface_getApp;
  jmethodID NativeInterface_getDevice;
  jmethodID NativeInterface_getUser;
  jmethodID NativeInterface_setUser;
  jmethodID NativeInterface_getMetadata;
  jmethodID NativeInterface_getContext;
  jmethodID NativeInterface_notify;
  jmethodID NativeInterface_leaveBreadcrumb;
  jmethodID NativeInterface_isDiscardErrorClass;
  jmethodID NativeInterface_deliverReport;

  jclass NativeStackframe;
  jmethodID NativeStackframe_constructor;

  jclass Severity;

  jclass BreadcrumbType;

  jclass OpaqueValue;
  jmethodID OpaqueValue_getJson;
  jmethodID OpaqueValue_makeSafe;

  jobject ErrorType_C;
} bsg_jni_cache_t;

extern bsg_jni_cache_t *const bsg_jni_cache;

/**
 * Populate all references in the JNI cache.
 *
 * @param env The JNI env
 * @return false if an error occurs, in which case the cache is unusable.
 */
bool bsg_jni_cache_init(JNIEnv *env);

/**
 * Get the current JNI environment, attaching if necessary.
 * The environment will be detached automatically on thread termination.
 * @return The current JNI environment, or NULL in case of JNI error (which will
 * be logged).
 */
JNIEnv *bsg_jni_cache_get_env();

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_JNI_CACHE_H
