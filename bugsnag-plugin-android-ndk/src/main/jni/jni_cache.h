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
  jclass hash_map;
  jclass map;
  jclass arraylist;
  jclass integer;
  jclass boolean;
  jclass metadata;
  jclass native_interface;
  jclass long_class;
  jclass float_class;
  jclass number;
  jclass string;
  jclass stack_trace_element;
  jclass severity;
  jclass breadcrumb_type;
  jmethodID integer_int_value;
  jmethodID long_long_value;
  jmethodID float_float_value;
  jmethodID boolean_bool_value;
  jmethodID number_double_value;
  jmethodID hash_map_get;
  jmethodID hash_map_size;
  jmethodID hash_map_key_set;
  jmethodID map_get;
  jmethodID map_size;
  jmethodID map_key_set;
  jmethodID arraylist_init_with_obj;
  jmethodID arraylist_get;
  jmethodID ni_get_app;
  jmethodID ni_get_device;
  jmethodID ni_get_user;
  jmethodID ni_set_user;
  jmethodID ni_get_metadata;
  jmethodID ni_get_context;
  jmethodID ni_notify;
  jmethodID ni_leave_breadcrumb;
  jmethodID ni_deliver_report;
  jmethodID ste_constructor;
} bsg_jni_cache;

// Always check for null before using this!
extern bsg_jni_cache *bsg_global_jni_cache;

/**
 * Creates a cache of JNI methods/classes that are commonly used in
 * bsg_global_jni_cache.
 *
 * Class and method objects can be kept safely since they aren't moved or
 * removed from the JVM.
 *
 * @param env The JNI env
 * @return false if an error occurs, in which case the cache is unusable.
 */
bool bsg_jni_cache_init(JNIEnv *env);

/**
 * Refresh the JNI cache to get the class references that got auto-released.
 * This MUST be called on every Java-to-native call!
 *
 * @param env The JNI env
 * @return false if an error occurs, in which case the cache is unusable.
 */
bool bsg_jni_cache_refresh(JNIEnv *env);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_JNI_CACHE_H
